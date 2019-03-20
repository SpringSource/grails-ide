/*******************************************************************************
 * Copyright (c) 2012,2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.actions;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;
import org.grails.ide.eclipse.editor.groovy.elements.INavigableGrailsElement;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @author Vasiliy Kizhaev
 */
public class GoToTestClass extends AbstractGotoClass {

    @Override
    protected String errorMessage() {
        return "No Test class found"; //$NON-NLS-1$
    }

    @Override
    protected boolean hasRelated() {
        return GrailsWorkspaceCore.hasRelatedTestClass(unit);
    }

    @Override
    protected IGrailsElement navigateTo(INavigableGrailsElement elt) {
        return elt.getTestClass();
    }
    
    @Override
    public String getCommandName() {
        return "create-unit-test";
    }

}
