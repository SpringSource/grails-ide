/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swtbot.swt.finder.utils.internal.Assert;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Andrew Eisenberg
 */
public class GrailsTestsActivator implements BundleActivator {
    
    public static final String PLUGIN_ID = "org.grails.ide.eclipse.test"; //$NON-NLS-1$
    private static boolean isJointGrailsTest;
       
    public static boolean isGrails200OrLater() {
        return GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_)>=0;
    }

    protected static List<String> findFiles(File file, final boolean recursive) {
        final List<String> files = new ArrayList<String>();
        file.listFiles(new FileFilter() {
            public boolean accept(File file) {
                // ignore the xalan and itext jars since they contain chained libraries in the MANIFEST.MF that don't exist
                // JDT wants to add these non-existant dependencies to the classpath and this causes errors later.
                if (file.getName().endsWith(".jar") && !file.getPath().contains("xalan") && !file.getPath().contains("itext")) {
                    files.add(file.getAbsolutePath());
                }
                if (recursive && file.isDirectory()) {
                    file.listFiles(this);
                }
                return false;
            }
        });
        
        return files;
    }
    
    /**
     * Gets a resource from the "resources" folder or from an arbitrary location in the file system.
     * Use an absolute path to get a file from the file system or a relative path to get a file
     * from the resources folder
     * @param name resource name
     * @return a file for the resource if it exists or null
     * @throws IOException 
     */
    public static File getResource(String path) throws IOException {
        String pluginFileName;
        if (new Path(path).isAbsolute()) {
            pluginFileName = path;
        } else {
            URL pluginURL = FileLocator.resolve(Platform.getBundle(PLUGIN_ID).getEntry("/resources/" + path));
            pluginFileName = pluginURL.getFile();
        }
        return new File(pluginFileName);
    }

    public static File getGrailsDSLD() throws IOException {
        Bundle bundle = Platform.getBundle(GrailsCoreActivator.GRAILS_RESOURCES_PLUGIN_ID);
        Assert.isNotNull(bundle, "Bundle not found: "+GrailsCoreActivator.GRAILS_RESOURCES_PLUGIN_ID);
		URL entry = FileLocator.resolve(bundle.getEntry("dsl-support"));
        return new File(entry.getFile());
    }
    
    
    public void start(BundleContext context) throws Exception {
    }


    public void stop(BundleContext context) throws Exception {
    }

    public static boolean isJointGrailsTest() {
        return isJointGrailsTest;
    }
    
    public static void setJointGrailsTest(boolean isJointGrailsTest) {
        GrailsTestsActivator.isJointGrailsTest = isJointGrailsTest;
    }
}
