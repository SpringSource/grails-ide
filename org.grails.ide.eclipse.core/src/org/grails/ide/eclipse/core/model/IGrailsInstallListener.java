/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.model;

import java.util.Set;

/**
 * @author Christian Dupuis
 * @author Kris De Volder
 */
public interface IGrailsInstallListener {

	void defaultInstallChanged(IGrailsInstall oldDefault, IGrailsInstall newDefault);
	void installsChanged(Set<IGrailsInstall> newInstalls);

}
