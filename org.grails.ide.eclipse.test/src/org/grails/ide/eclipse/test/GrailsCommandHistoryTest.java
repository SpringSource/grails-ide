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
import java.util.Arrays;

import junit.framework.TestCase;

import org.codehaus.groovy.eclipse.core.model.GroovyRuntime;
import org.codehaus.groovy.eclipse.test.TestProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.springsource.ide.eclipse.commons.core.Entry;
import org.springsource.ide.eclipse.commons.internal.core.commandhistory.CommandHistory;



/**
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @created 2010-06-17
 */
public class GrailsCommandHistoryTest extends TestCase {

	private File saveFile = null;

	private CommandHistory newGrailsCommandHistory() {
		return new CommandHistory("TEST", GrailsNature.NATURE_ID);
	}
	
	public void testNewHistory() throws Exception {
		CommandHistory hist = newGrailsCommandHistory();
		assertTrue(hist.isEmpty());
	}

	public void testAddElements() {
		CommandHistory hist = newGrailsCommandHistory();
		assertFalse(hist.isDirty());
		
		hist.add(new Entry("run-app foo", "kris"));
		assertTrue(hist.isDirty());
		assertFalse(hist.isEmpty());
		assertEquals(new Entry("run-app foo", "kris"), hist.getLast());
		
		hist.add(new Entry("run-app bar", "nieraj"));
		assertTrue(hist.isDirty());
		assertFalse(hist.isEmpty());
		assertEquals(new Entry("run-app bar", "nieraj"), hist.getLast());
	}
	
	public void testToArray() throws Exception {
		Entry[] entries = new Entry[] {
				new Entry("run-app foo", "kris"),
				new Entry("run-app bar", "nieraj"),
				new Entry("blah blah", "ka.da.na.za"),
		};
		
		CommandHistory hist = newGrailsCommandHistory();
		assertTrue(hist.isEmpty());
		
		for (int i = entries.length-1; i >= 0; i--) {
			hist.add(entries[i]);
		}
		
		assertTrue(Arrays.equals(entries, hist.toArray()));
	}

	public void testSaveLoad() throws Exception {
		saveFile = new File("saveit.gch");
		
		CommandHistory hist = newGrailsCommandHistory();
		
		hist.add(new Entry("run-app foo", "kris"));
		assertFalse(hist.isEmpty());
		assertEquals(new Entry("run-app foo", "kris"), hist.getLast());
		
		hist.add(new Entry("run-app bar", "nieraj"));
		assertFalse(hist.isEmpty());
		assertEquals(new Entry("run-app bar", "nieraj"), hist.getLast());
		assertEquals(2, hist.size());
		assertTrue(hist.isDirty());
		
		hist.save(saveFile);
		assertFalse(hist.isDirty());
		
		hist = newGrailsCommandHistory();
		assertTrue(hist.isEmpty());
		
		hist.load(saveFile);
		assertFalse(hist.isDirty());
		assertEquals(2, hist.size());
		
		assertIterator(hist, 
				new Entry("run-app bar", "nieraj"),
				new Entry("run-app foo", "kris"));
		
	}
	
	public void testLimit() throws Exception {
		CommandHistory hist = newGrailsCommandHistory();
		assertTrue(hist.getMaxSize()>10); // set to a sensible value initially?
		
		Entry[] entries = new Entry[] {
			new Entry("a","pa"),
			new Entry("b","pb"),
			new Entry("c","pc"),
			new Entry("d","pd")
		};
		for (int i = 0; i < entries.length; i++) {
			hist.add(entries[i]);
		}

		assertEquals(entries.length, hist.size()); 
		
		hist.setMaxSize(2);
		assertEquals(2, hist.getMaxSize());
		assertEquals(2, hist.size());
		
		assertIterator(hist,
				new Entry("d","pd"),
				new Entry("c","pc")
		);
		
		saveFile  = new File("testSave.gch");
		hist.save(saveFile);
		
		hist = newGrailsCommandHistory();
		assertTrue(hist.isEmpty());       // This really is a new instance
		assertTrue(hist.getMaxSize()>10); // set to a sensible value initially?
		
		hist.load(saveFile);
		assertEquals(2, hist.getMaxSize());
		assertIterator(hist,
				new Entry("d","pd"),
				new Entry("c","pc")
		);
	}
	
	public void testValidEntries() throws Exception {
		CommandHistory hist = newGrailsCommandHistory();
		Entry[] entries = new Entry[] {
			new Entry("a","pa"),
			new Entry("b","pb"),
			new Entry("c","pc"),
			new Entry("d","pd")
		};
		for (int i = 0; i < entries.length; i++) {
			hist.add(entries[i]);
		}
		assertEquals(entries.length, hist.size());

		TestProject pa = new TestProject("pa");
		TestProject pc = new TestProject("pc");
		
		assertIterator(hist.validEntries() /*none*/);
		
		addGrailsNature(pa);
		assertIterator(hist.validEntries(), 
				new Entry("a","pa")
		);
		
		addGrailsNature(pc);
		assertIterator(hist.validEntries(), 
				new Entry("c","pc"),
				new Entry("a","pa")
		);
		
		TestProject pd = new TestProject("pd");
		addGrailsNature(pd);
		assertIterator(hist.validEntries(), 
				new Entry("d","pd"),
				new Entry("c","pc"),
				new Entry("a","pa")
		);
		
		TestProject pb = new TestProject("pb");
		addGrailsNature(pb);
		assertIterator(hist.validEntries(), 
				new Entry("d","pd"),
				new Entry("c","pc"),
				new Entry("b","pb"),
				new Entry("a","pa")
		);
		
		assertIterator(hist.getRecentValid(2),
				new Entry("d","pd"),
				new Entry("c","pc")
		);
	}

	/**
	 * Turn project into a Grails project.
	 */
	private void addGrailsNature(TestProject testProject) throws CoreException {
        GroovyRuntime.addGroovyRuntime(testProject.getProject());
        testProject.addNature(GrailsNature.NATURE_ID);
	}

	private void assertIterator(Iterable<Entry> hist, Entry... expected) {
		int i = 0;
		for (Entry entry : hist) {
			assertEquals(expected[i], entry);
			i++;
		}
		assertEquals("Too few elements in iterator", expected.length, i);
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		if (saveFile!=null && saveFile.exists()) {
			saveFile.delete();
		}
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : projects) {
			if (p.exists())
				p.delete(true, true, new NullProgressMonitor());
		}
	}
}
