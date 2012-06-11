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

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectAttachementsCache;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectAttachementsCache.Dependency;
import org.grails.ide.eclipse.core.launch.GrailsCommandLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests for (portions of) the {@link GrailsCommandUtils} class.
 * These are the JOINT tests, the ones that run on the joint STS-grails build 
 * @author Kris De Volder
 * @created 2010-08-04
 */
public class JointGrailsCommandTest extends AbstractCommandTest {

	public static boolean heartbeat = false;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GroovyCompilerVersionCheck.testMode();
		//Ensure prior test projects don't exist
		StsTestUtil.deleteAllProjects();
		//clearGrailsState(); //Workaround for http://jira.grails.org/browse/GRAILS-7655 (ivy cache corruption)
	}
	
	/**
	 * Attempt to distill down to smaller test the reason for test failures where it seems
	 * "grails compile --non-interactive" is being interpreted as a request to call "grails interactive"
	 * instead. This happens only when we call it from STS, not commandline.
	 */
	public void testRefreshDependencyFileCommand() throws Exception {
//		GrailsCommandLaunchConfigurationDelegate.DEBUG = true;
		try {
			GrailsVersion version = GrailsVersion.MOST_RECENT;
			ensureDefaultGrailsVersion(version);
            project = ensureProject(TEST_PROJECT_NAME);

			GrailsCommand cmd = GrailsCommandFactory.refreshDependencyFile(project);
			System.out.println(cmd.synchExec());
		} finally {
			GrailsCommandLaunchConfigurationDelegate.DEBUG = false;
		}
	}
	
	/**
	 * A test running the Grails command used for downloading
	 * @throws Exception
	 */
	public void testDownloadSourcesWithFunnyChars() throws Exception {
		if (GrailsVersion.V_2_1_.compareTo(GrailsVersion.MOST_RECENT) > 0) {
			//This is known to be broken in Grails 2.0.3:
			//See http://jira.grails.org/browse/GRAILS-8955
			return; 
		}
		//Note to Andrew: I put it back in Joint tests because in afterthought I realized that 
		// it actually does a useful compatibility check between STS and
		// Grails w.r.t. how spaces are being escaped on the commandline. If Grails & STS disagree on this, then windows users
		// with folders like "Documents and Settings" will have issues.
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		doTestDownloadSources(File.createTempFile("a name with spaces", "xml"), version);
		doTestDownloadSources(File.createTempFile("with \\ and space", "xml"), version);
	}

    /**
     * A test running the Grails command used for downloading
     * @throws Exception
     */
    public void testDownloadSources() throws Exception {
        if (GrailsVersion.V_2_1_.compareTo(GrailsVersion.MOST_RECENT) > 0) {
			//This is known to be broken in Grails 2.0.3:
			//See http://jira.grails.org/browse/GRAILS-8955
			return; 
		}
		GrailsVersion version = GrailsVersion.MOST_RECENT;
		doTestDownloadSources(File.createTempFile("dependencies", "xml"), version);
    }

    private void doTestDownloadSources(File target, GrailsVersion version) throws Exception {
        if (version.compareTo(GrailsVersion.V_2_0_0) >= 0) {
            ensureDefaultGrailsVersion(version);
            final String projectName = TEST_PROJECT_NAME;
            project = ensureProject(projectName);
            GrailsCommand cmd = GrailsCommand.forTest(project, "refresh-dependencies");
            cmd.addArgument("--include-source");
            // cmd.addArgument("--include-javadoc");
            cmd.addArgument(target.getAbsolutePath());

            ILaunchResult result = cmd.synchExec();
            assertTrue(result.isOK());

            Map<String, Dependency> data = PerProjectAttachementsCache.parseData(target);
            assertTrue(data != null && !data.isEmpty());
        }
    }

    
    /**
     * Quick test of the method that retrieves the spring-loaded jar File from
     * a Grails install.
     */
    public void testSpringLoadedJar() throws Exception {
        GrailsVersion version = GrailsVersion.MOST_RECENT;
        ensureDefaultGrailsVersion(version);
        if (version.compareTo(GrailsVersion.V_2_0_0)>=0) {
            //Only applicable from Grails 1.4 and up.
            IGrailsInstall install = GrailsVersion.MOST_RECENT.getInstall();
            File jar = install.getSpringLoadedJar();
            assertNotNull(jar);
            assertTrue(jar.exists());
            assertTrue(jar.isAbsolute());
            assertTrue(jar.getName().endsWith(".jar"));
            assertTrue(jar.getName().startsWith("springloaded-core-"));
        }
    }
    /**
     * Relates to https://issuetracker.springsource.com/browse/STS-1874. Example code from
     * https://issuetracker.springsource.com/browse/STS-1880.
     */
    public void testAgentBasedReloading() throws Exception {
        GrailsVersion version = GrailsVersion.MOST_RECENT;
        if (version.compareTo(GrailsVersion.V_2_0_0)>=0) {
            ensureDefaultGrailsVersion(version);
            final String projectName = TEST_PROJECT_NAME;
            project = ensureProject(projectName);
            createResource(project, "grails-app/controllers/ReloadableController.groovy", 
                    "class ReloadableController\n" + 
                            "{\n" + 
                            "   def index = { render \"hello world\" }\n" + 
                    "}");
            StsTestUtil.assertNoErrors(project); // Forces build and checks for compile errors in project.

            final int port = StsTestUtil.findFreeSocketPort();
            ILaunchConfigurationWorkingCopy launchConf = (ILaunchConfigurationWorkingCopy) GrailsLaunchConfigurationDelegate.getLaunchConfiguration(project, "-Dserver.port="+port+" run-app", false);
            
            ILaunch launch = launchConf.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
            dumpOutput(launch);
            final URL url = new URL("http://localhost:"+port+"/"+projectName+"/reloadable/index");
            try {
                new ACondition("hello world") {
                    public boolean test() throws Exception {
                        String page = getPageContent(url);
                        assertEquals("hello world\n", page);
                        return true;
                    }
                }.waitFor(120000);

                createResource(project, "grails-app/controllers/ReloadableController.groovy", 
                        "class ReloadableController\n" + 
                                "{\n" + 
                                "   def index = { render \"goodbye world\" }\n" + 
                        "}");

                new ACondition("goodbye world") {
                    public boolean test() throws Exception {
                        String page = getPageContent(url);
                        assertEquals("goodbye world\n", page);
                        return true;
                    }
                }.waitFor(30000); // updating contents with SpringLoaded should be faster?
            } finally {
                launch.terminate();
            }
        } else {
            System.out.println("Skipping this test");
        }
    }
    
    /**
     * Execute a command that is not associated with any project (yet).
     */
    public void testCreateApp() throws Exception {
        ensureProject(TEST_PROJECT_NAME);
    }

    /**
     * Tests creation of a plugin project
     */
    public void testCreatePlugin() throws Exception {
        ensureProject(TEST_PROJECT_NAME+"-plugin", true);
    }
 }
