/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands.test;

import static org.grails.ide.eclipse.commands.GrailsCommandFactory.createDomainClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.DependencyData;
import org.grails.ide.eclipse.core.internal.classpath.DependencyFileFormat;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.launch.SharedLaunchConstants;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;
import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * Tests for the GrailsCommand class.
 * 
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @created 2010-08-04
 */
public class GrailsCommandTest extends AbstractCommandTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GroovyCompilerVersionCheck.testMode();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
//		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
	}
	
	public void testInPlacePluginDependency() throws Exception {
		IProject plugin = ensureProject(this.getClass().getSimpleName()+"-"+"plug-in", true);
		IProject nonPlugin = ensureProject(this.getClass().getSimpleName()+"-"+"NonPlugin");
		IJavaProject jNonPlugin = JavaCore.create(nonPlugin);

		// at first, should not have a dependency and should not be on classpath
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsPluginUtil.addPluginDependency(nonPlugin, plugin);

		// dependency should exist, but not on classpath until refresh
		// dependencies is run
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));

		// now remove dependency
		GrailsPluginUtil.removePluginDependency(nonPlugin, plugin);

		// dependency should not exist, but still on classpath until refresh
		// dependencies is run
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));
		assertFalse("Dependency should not exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));

		// try one more time for good show
		GrailsPluginUtil.addPluginDependency(nonPlugin, plugin);

		// dependency should exist, but not on classpath until refresh
		// dependencies is run
		assertTrue("Dependency should exist",
				GrailsPluginUtil.dependencyExists(nonPlugin, plugin));
		assertFalse("Should not be on classpath",
				isOnClasspath(nonPlugin, plugin));

		GrailsCommandUtils.refreshDependencies(jNonPlugin, false);
		assertTrue("Should be on classpath", isOnClasspath(nonPlugin, plugin));
	}

	/**
	 * Execute a command that runs "inside" a Grails project.
	 */
	public void testCreateDomainClass() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		GrailsCommand cmd = createDomainClass(proj, "gTunes.Song");
		ILaunchResult result = cmd.synchExec();
		System.out.println(result.getOutput());
		GrailsTest.assertRegexp("Created.*Song", result.getOutput());
		proj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		assertResourceExists(TEST_PROJECT_NAME+"/grails-app/domain/gTunes/Song.groovy");
		// assertResourceExists(TEST_PROJECT_NAME+"/test/unit/gTunes/SongTests.groovy");
	}

	public void testBogusCommand() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		GrailsCommand cmd = GrailsCommand.forTest(proj,"create-domain")
				.addArgument("gTunes.Album");
		try {
			cmd.synchExec();
			fail("Should have an exception, but didn't have one");
		} catch (CoreException e) {
			if (e.getStatus() instanceof MultiStatus) {
				MultiStatus m = (MultiStatus) e.getStatus();
				if (m.getChildren().length == 2) {
					assertContains("Script 'CreateDomain' not found",
							m.getChildren()[0].getMessage());
					return;
				}
			}
			fail("Incorrect exception thrown.  Status:\n" + e.getMessage());
		}
	}

	/**
	 * Test the hook-up of DependencyExtractingBuildListener as a
	 * "BuildListener" to a grails process.
	 */
	public void testBuildListener() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
		
		GrailsCommand cmd = GrailsCommand.forTest(proj, "compile");
		File tmpFile = File.createTempFile("testListener", ".log");
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		assertFalse(tmpFile.exists());

		// Typical way to pass info to build listener in external process is by
		// setting system properties
		cmd.setSystemProperty(SharedLaunchConstants.DEPENDENCY_FILE_NAME_PROP,
				tmpFile.toString());
		cmd.attachBuildListener(SharedLaunchConstants.DependencyExtractingBuildListener_CLASS);

		cmd.synchExec();

		checkDependencyFile(tmpFile);
	}

	/**
	 * Repeat the above check, but instead of using the "nuts and bolts" just
	 * use enableRefreshDependencyFile(). This should do the same thing (so the
	 * generated dependency file should pass the same checks).
	 */
	public void testBuildListener2() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
		
		GrailsCommand cmd = GrailsCommand.forTest(proj, "compile");
		cmd.enableRefreshDependencyFile();
		File tmpFile = GrailsClasspathUtils.getDependencyDescriptor(proj);
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		assertFalse(tmpFile.exists());

		cmd.synchExec();

		checkDependencyFile(tmpFile);
	}

	/**
	 * Do a few simple checks to see if the contents of a generated dependency
	 * file looks ok. These checks are by no means comprehensive, but it is
	 * better than nothing.
	 * 
	 * @throws IOException
	 */
	protected void checkDependencyFile(File file) throws IOException {
		// ///////////////////////////////////////
		// Check the generated file...
		assertTrue(file.exists());

		DependencyData depData = DependencyFileFormat.read(file);

		String dotGrailsFolder = new File(GrailsCoreActivator.getDefault().getUserHome() + "/"
				+ ".grails/" + grailsVersion()).getCanonicalPath();

		// Check plugins directory points where it should
		String pluginsDirectory = depData.getPluginsDirectory();
		assertEquals(dotGrailsFolder + "/projects/"+TEST_PROJECT_NAME+"/plugins",
				pluginsDirectory);

		Set<String> sources = depData.getSources();
		for (String string : sources) {
			System.out.println(string);
		}
		String[] expectedPlugins = new String[] { "tomcat", "hibernate" }; // Plugins
																			// installed
																			// by
																			// default
																			// in
																			// new
																			// grails
																			// apps.

		for (String pluginName : expectedPlugins) {
			for (String javaGroovy : new String[] { "java", "groovy" }) {
				String expect = pluginsDirectory + "/" + pluginName + "-"
						+ grailsVersion() + "/src/" + javaGroovy;
				assertTrue("Missing source entry: " + expect,
						sources.contains(expect));
			}
		}

		Set<String> pluginsXmls = depData.getPluginDescriptors();
		// for (String string : pluginsXmls) {
		// System.out.println(string);
		// }
		for (String pluginName : expectedPlugins) {
			String expect = pluginsDirectory + "/" + pluginName + "-"
					+ grailsVersion() + "/plugin.xml";
			assertTrue("Missing plugin.xml file: " + expect,
					pluginsXmls.contains(expect));
		}

		// TODO: KDV: (depend) add some more checks of the contents of the file
		// (i.e. check for a few basic jar dependencies that should always be
		// there.)
	}

	/**
	 * Test to see if after changing application.properties and doing refresh
	 * dependencies, the project source folders look correct.
	 * 
	 * @throws Exception
	 */
	public void testEditedApplicationProperties() throws Exception {
		IProject proj = ensureProject(TEST_PROJECT_NAME);
		IFile eclipsePropFile = proj.getFile("application.properties");
		File propFile = eclipsePropFile.getLocation().toFile();

		// Modify the props file add two plugins
		Properties props = new Properties();
		props.load(new FileInputStream(propFile)); // Need to close this reader?
		props.put("plugins.feeds", "1.5");
		props.put("plugins.acegi", "0.5.3");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		// Refresh dependencies
		GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);

		// Check that the plugins linked source folders are now there.
		assertPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertPluginSourceFolder(proj, "acegi-0.5.3", "src", "groovy");

		// /////////////////////////////////////////////////////////////
		// Now modify the version of the plugins and try this again

		props.put("plugins.feeds", "1.4");
		props.put("plugins.acegi", "0.5.2");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		// Refresh dependencies
		// GrailsClient.DEBUG_PROCESS = true;
		GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);
		// GrailsClient.DEBUG_PROCESS = false;

		// Check that the linked source folders of the replaced version are no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "acegi-0.5.3", "src", "groovy");

		// Check that the linked source folders of the new versions are there.
		assertPluginSourceFolder(proj, "feeds-1.4", "src", "groovy");
		assertPluginSourceFolder(proj, "acegi-0.5.2", "src", "groovy");
		
		// /////////////////////////////////////////////////////////////
		// Now remove the plugins and try this again

		props.remove("plugins.feeds");
		props.remove("plugins.acegi");
		props.store(new FileOutputStream(propFile), "#Grails metadata file");
		eclipsePropFile.refreshLocal(IResource.DEPTH_ZERO,
				new NullProgressMonitor());

		// Refresh dependencies
		GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);

		// Check that the linked source folders of the replaced version are no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "acegi-0.5.3", "src", "groovy");

		// Check that the linked source folders of the new versions are also no
		// longer there.
		assertAbsentPluginSourceFolder(proj, "feeds-1.4", "src", "groovy");
		assertAbsentPluginSourceFolder(proj, "acegi-0.5.2", "src", "groovy");
	}
	
	
	public void testTagLibsFromPlugin() throws Exception {
	    IProject proj = ensureProject(TEST_PROJECT_NAME);
	    ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
	    
	    GrailsCommand cmd = GrailsCommand.forTest(proj, "install-plugin")
	    		.addArgument("feeds")
	    		.addArgument("1.5");
	    cmd.synchExec();
		GrailsCommandUtils.refreshDependencies(JavaCore.create(proj), true);
		
	    assertPluginSourceFolder(proj, "feeds-1.5", "src", "groovy");
	    
	    // now also check to see that the tag is available
	    PerProjectTagProvider provider = GrailsCore.get().connect(proj, PerProjectTagProvider.class);
	    assertNotNull("feeds:meta tag not installed", provider.getDocumentForTagName("feed:meta"));
    }
	
	public void testOutputLimit() throws Exception {
	    IProject proj = ensureProject(TEST_PROJECT_NAME);
	    ensureDefaultGrailsVersion(GrailsVersion.getGrailsVersion(proj));
	    
		GrailsCommand cmd = GrailsCommand.forTest("help");
		ILaunchResult result = cmd.synchExec();
//		String allOutput = result.getOutput();
		
		int orgLimit = GrailsCoreActivator.getDefault().getGrailsCommandOutputLimit();
		try {
			GrailsCoreActivator.getDefault().setGrailsCommandOutputLimit(100);
			result = cmd.synchExec();
			assertEquals(100, result.getOutput().length());
						
		} finally {
			GrailsCoreActivator.getDefault().setGrailsCommandOutputLimit(orgLimit);
		}
	}
	
	// /**
	// * Test grails command to build "exploded" war file inside the target
	// directory (rather than a ".war" archive). To do this
	// * we need to somehow set additional properties in the grails build
	// settings that we do not ordinarily have access to)
	// * from the command line (only can be set in the BuildSettings.groovy
	// config file). So the this test implicitly checks
	// * the mechanism hacked-up here to set properties in BuildSettings)
	// */
	// public void testExplodedWar() throws Exception {
	// IProject proj = ensureProject(TEST_PROJECT_NAME);
	// GrailsCommand cmd = new GrailsCommand(proj, "dev war");
	// cmd.
	// }

	/**
	 * What's the default grails version that we expect new projects to be
	 * created with.
	 */
	private String grailsVersion() {
		return GrailsCoreActivator.getDefault().getInstallManager()
				.getDefaultGrailsInstall().getVersionString();
	}
	
}
