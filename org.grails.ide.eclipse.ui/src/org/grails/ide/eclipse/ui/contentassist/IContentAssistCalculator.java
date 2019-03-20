//COPIED from spring-ide org.springframework.ide.eclipse.beans.ui.editor.contentassist.IContentAssistCalculator
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
package org.grails.ide.eclipse.ui.contentassist;

/**
 * Implementations of this interface are fine-grained objects responsible for calculating content
 * assist proposals.
 * <p>
 * Normally a content assist processor would extend the
 * {@link NamespaceContentAssistProcessorSupport} class and register these
 * {@link IContentAssistCalculator} instances in the
 * {@link NamespaceContentAssistProcessorSupport#init()} method.
 * @author Christian Dupuis
 * @since 2.0.2
 */
public interface IContentAssistCalculator {

	/**
	 * Calculate content assist proposals under the given context.
	 * @param context the current context of the content assist proposal request
	 * @param recorder the recorder to record calculated proposals
	 */
	void computeProposals(IContentAssistContext context, IContentAssistProposalRecorder recorder);

}
