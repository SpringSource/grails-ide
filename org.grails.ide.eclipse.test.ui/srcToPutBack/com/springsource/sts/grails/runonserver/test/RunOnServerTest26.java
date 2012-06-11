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
package org.grails.ide.eclipse.runonserver.test;

import com.springsource.sts.server.tc.tests.support.TcServerFixture;

/**
 * @author Kris De Volder
 */
public class RunOnServerTest26 extends RunOnServerTestTemplate {

	@Override
	protected TcServerFixture getTcServerFixture() {
		return TcServerFixture.V_2_6;
	}
	
}
