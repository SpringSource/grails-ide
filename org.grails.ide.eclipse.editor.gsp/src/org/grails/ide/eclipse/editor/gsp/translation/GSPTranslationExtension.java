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
package org.grails.ide.eclipse.editor.gsp.translation;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.IDocument;

import org.grails.ide.eclipse.editor.gsp.translation.internal.GSPTranslator;

/**
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 8, 2009
 */
public class GSPTranslationExtension extends GSPTranslation {

	public GSPTranslationExtension(IDocument jspDocument,
            IDocument javaDocument, IJavaProject javaProj,
            GSPTranslator translator) {
        super(jspDocument, javaDocument, javaProj, translator);
    }
    
}
