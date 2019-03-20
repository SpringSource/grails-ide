/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.longrunning;

import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;

/**
 * An abstraction that shields grails core plugin from having to depend directly on UI plugins to instantiate
 * an IOConsole view instance (or something equivalent) to show output to the user. 
 * <p>
 * An instance of this class provides some mechanism to create output streams that will present any output sent
 * to them in some view that is accessible to the user.
 * 
 * @author Kris De Volder
 * @since 2.5.3
 */
public abstract class ConsoleProvider {

	/** 
	 * Creates a page in the console view to show output and returns output and input streams to
	 * write to / read from it.
	 */
	public abstract Console getConsole(String command, GrailsCommandExecution grailsCommandExecution);

	/**
	 * If a console provider keeps track of consoles that have been created, this method
	 * can be called to remove all those associated with terminated executions.
	 * <p>
	 * The default implementation provided here does nothing, assuming the provides doesn't
	 * keep a history. Subclass should override if they do maintain a history.
	 */
	public void removeAllTerminated() {
	}

}
