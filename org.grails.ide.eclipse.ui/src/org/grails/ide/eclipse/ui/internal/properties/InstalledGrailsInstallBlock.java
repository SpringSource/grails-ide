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
package org.grails.ide.eclipse.ui.internal.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class InstalledGrailsInstallBlock implements ISelectionProvider {

	private static final int[] defaultColumnWidth = {
		140, 280
	};

	/**
	 * This block's control
	 */
	private Composite fControl;

	/**
	 * VMs being displayed
	 */
	private List<IGrailsInstall> fVMs = new ArrayList<IGrailsInstall>();

	/**
	 * The main list control
	 */
	private CheckboxTableViewer fVMList;

	// Action buttons
	private Button fAddButton;

	private Button fRemoveButton;

	private Button fEditButton;

	// index of column used for sorting
	private int fSortColumn = 0;

	/**
	 * Selection listeners (checked JRE changes)
	 */
	private ListenerList fSelectionListeners = new ListenerList();

	/**
	 * Previous selection
	 */
	private ISelection fPrevSelection = new StructuredSelection();

	private Table fTable;

	/**
	 * Content provider to show a list of JREs
	 */
	class JREsContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return fVMs.toArray();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}
	}

	/**
	 * Label provider for installed JREs table.
	 */
	class VMLabelProvider extends LabelProvider implements ITableLabelProvider {

		private Image errorImg;

		/**
		 * @see ITableLabelProvider#getColumnText(Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof IGrailsInstall) {
				IGrailsInstall vm = (IGrailsInstall) element;
				switch (columnIndex) {
				case 0:
					return vm.getName();
				case 1:
					return vm.getHome();
				}
			}
			return element.toString();
		}

		/**
		 * @see ITableLabelProvider#getColumnImage(Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				IGrailsInstall install = (IGrailsInstall) element;
				if (install.verify().isOK()) {
					return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
				} else {
					return getErrorImage();
				}
			}
			return null;
		}

		private Image getErrorImage() {
			if (errorImg==null) {
				errorImg = GrailsUiActivator.getImageDescriptor("icons/full/obj16/error.gif")
						.createImage(true);
			}
			return errorImg;
		}

		@Override
		public void dispose() {
			super.dispose();
			if (errorImg!=null) {
				errorImg.dispose();
			}
		}
		
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionListeners.add(listener);
	}

	public ISelection getSelection() {
		return new StructuredSelection(fVMList.getCheckedElements());
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionListeners.remove(listener);
	}

	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (!selection.equals(fPrevSelection)) {
				fPrevSelection = selection;
				Object jre = ((IStructuredSelection) selection).getFirstElement();
				if (jre == null) {
					fVMList.setCheckedElements(new Object[0]);
				}
				else {
					fVMList.setCheckedElements(new Object[] { jre });
					fVMList.reveal(jre);
				}
				fireSelectionChanged();
			}
		}
	}

	public void createControl(Composite ancestor) {
		Font font = ancestor.getFont();
		Composite parent = SWTFactory.createComposite(ancestor, font, 2, 1, GridData.FILL_BOTH);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		fControl = parent;

		SWTFactory.createLabel(parent, "Grails Installations:", 2);

		fTable = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		fTable.setFont(font);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);
		
//		GridData gd = new GridData(GridData.FILL_BOTH);
//		gd.heightHint = 250;
//		gd.widthHint = 350;
		GridDataFactory
		.fillDefaults()
		.hint(350, 250)
		.grab(true, true)
		.applyTo(fTable);
		
		TableColumn column = new TableColumn(fTable, SWT.LEFT);
		column.setText("Name");
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sortByName();
			}
		});

		column = new TableColumn(fTable, SWT.LEFT);
		column.setText("Location");
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sortByLocation();
			}
		});

		fVMList = new CheckboxTableViewer(fTable);
		fVMList.setLabelProvider(new VMLabelProvider());
		fVMList.setContentProvider(new JREsContentProvider());
		// by default, sort by name
		sortByName();

		fVMList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {
				enableButtons();
			}
		});

		fVMList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					setCheckedJRE((IGrailsInstall) event.getElement());
				}
				else {
					setCheckedJRE(null);
				}
			}
		});

		fVMList.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				if (!fVMList.getSelection().isEmpty()) {
					editVM();
				}
			}
		});
		fTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					if (fRemoveButton.isEnabled()) {
						removeVMs();
					}
				}
			}
		});

		Composite buttons = SWTFactory.createComposite(parent, font, 1, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);

		fAddButton = SWTFactory.createPushButton(buttons, "Add...", null);
		fAddButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				addVM();
			}
		});

		fEditButton = SWTFactory.createPushButton(buttons, "Edit...", null);
		fEditButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				editVM();
			}
		});

		fRemoveButton = SWTFactory.createPushButton(buttons, "Remove...", null);
		fRemoveButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				removeVMs();
			}
		});

		SWTFactory.createVerticalSpacer(parent, 1);

		fillWithWorkspaceJREs();
		enableButtons();
		fAddButton.setEnabled(JavaRuntime.getVMInstallTypes().length > 0);
	}

	/**
	 * Compares the given name against current names and adds the appropriate numerical suffix to ensure that it is
	 * unique.
	 * @param name the name with which to ensure uniqueness
	 * @return the unique version of the given name
	 * @since 3.2
	 */
	public String generateName(String name, IGrailsInstall install) {
		if (!isDuplicateName(name, install)) {
			return name;
		}

		if (name.matches(".*\\(\\d*\\)")) { //$NON-NLS-1$
			int start = name.lastIndexOf('(');
			int end = name.lastIndexOf(')');
			String stringInt = name.substring(start + 1, end);
			int numericValue = Integer.parseInt(stringInt);
			String newName = name.substring(0, start + 1) + (numericValue + 1) + ")"; //$NON-NLS-1$
			return generateName(newName, install);
		}
		else {
			return generateName(name + " (1)", install); //$NON-NLS-1$
		}
	}

	/**
	 * Fire current selection
	 */
	private void fireSelectionChanged() {
		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		Object[] listeners = fSelectionListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
			listener.selectionChanged(event);
		}
	}

	/**
	 * Sorts by VM name.
	 */
	private void sortByName() {
		fVMList.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if ((e1 instanceof IGrailsInstall) && (e2 instanceof IGrailsInstall)) {
					IGrailsInstall left = (IGrailsInstall) e1;
					IGrailsInstall right = (IGrailsInstall) e2;
					return left.getName().compareToIgnoreCase(right.getName());
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(Object element, String property) {
				return true;
			}
		});
		fSortColumn = 1;
	}

	/**
	 * Sorts by VM location.
	 */
	private void sortByLocation() {
		fVMList.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if ((e1 instanceof IGrailsInstall) && (e2 instanceof IGrailsInstall)) {
					IGrailsInstall left = (IGrailsInstall) e1;
					IGrailsInstall right = (IGrailsInstall) e2;
					return left.getHome().compareToIgnoreCase(right.getHome());
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(Object element, String property) {
				return true;
			}
		});
		fSortColumn = 2;
	}

	/**
	 * Enables the buttons based on selected items counts in the viewer
	 */
	private void enableButtons() {
		IStructuredSelection selection = (IStructuredSelection) fVMList.getSelection();
		int selectionCount = selection.size();
		fEditButton.setEnabled(selectionCount == 1);
		if (selectionCount > 0 && selectionCount < fVMList.getTable().getItemCount()) {
			fRemoveButton.setEnabled(true);
		}
		else {
			fRemoveButton.setEnabled(false);
		}
	}

	/**
	 * Returns this block's control
	 * 
	 * @return control
	 */
	public Control getControl() {
		return fControl;
	}

	/**
	 * Sets the JREs to be displayed in this block
	 * 
	 * @param vms JREs to be displayed
	 */
	protected void setJREs(IGrailsInstall[] vms) {
		fVMs.clear();
		for (int i = 0; i < vms.length; i++) {
			fVMs.add(vms[i]);
		}
		fVMList.setInput(fVMs);
		fVMList.refresh();
		
		for (IGrailsInstall install : vms) {
			if (install.isDefault()) {
				setCheckedJRE(install);
			}
		}
	}

	/**
	 * Returns the JREs currently being displayed in this block
	 * 
	 * @return JREs currently being displayed in this block
	 */
	public IGrailsInstall[] getJREs() {
		return fVMs.toArray(new IGrailsInstall[fVMs.size()]);
	}

	/**
	 * Bring up a wizard that lets the user create a new VM definition.
	 */
	private void addVM() {
		GrailsInstallDialog wizard = new GrailsInstallDialog(getShell(), new DefaultGrailsInstall(null, null, false), this);
		if (wizard.open() == Window.OK) {
			IGrailsInstall result = wizard.getResult();
			if (result != null) {
				fVMs.add(result);
				fVMList.refresh();
				Object[] checkedElements = fVMList.getCheckedElements();
				if (checkedElements==null || checkedElements.length==0) {
					fVMList.setSelection(new StructuredSelection(result));
					setSelection(new StructuredSelection(result));
				}
			}
		}
	}

	public void vmAdded(IGrailsInstall vm) {
		fVMs.add(vm);
		fVMList.refresh();
	}

	public boolean isDuplicateName(String name, IGrailsInstall install) {
		if (install != null) {
			if (install.getName() != null && install.getName().equals(name)) {
				return false;
			}
		}
		for (int i = 0; i < fVMs.size(); i++) {
			IGrailsInstall vm = fVMs.get(i);
			if (vm.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Performs the edit VM action when the Edit... button is pressed
	 */
	private void editVM() {
		IStructuredSelection selection = (IStructuredSelection) fVMList.getSelection();
		IGrailsInstall vm = (IGrailsInstall) selection.getFirstElement();
		if (vm == null) {
			return;
		}
		GrailsInstallDialog wizard = new GrailsInstallDialog(getShell(), vm, this);
		if (wizard.open() == Window.OK) {
			IGrailsInstall result = wizard.getResult();
			if (result != null) {
				// replace with the edited VM
				int index = fVMs.indexOf(vm);
				fVMs.remove(index);
				fVMs.add(index, result);
				fVMList.refresh();
				fVMList.setSelection(new StructuredSelection(result));
				fireSelectionChanged();
			}
		}
	}

	/**
	 * Performs the remove VM(s) action when the Remove... button is pressed
	 */
	@SuppressWarnings("unchecked")
	private void removeVMs() {
		IStructuredSelection selection = (IStructuredSelection) fVMList.getSelection();
		IGrailsInstall[] vms = new IGrailsInstall[selection.size()];
		Iterator<IGrailsInstall> iter = selection.iterator();
		int i = 0;
		while (iter.hasNext()) {
			vms[i] = iter.next();
			i++;
		}
		removeJREs(vms);
	}

	/**
	 * Removes the given VMs from the table.
	 * 
	 * @param vms
	 */
	public void removeJREs(IGrailsInstall[] vms) {
		IStructuredSelection prev = (IStructuredSelection) getSelection();
		for (int i = 0; i < vms.length; i++) {
			fVMs.remove(vms[i]);
		}
		fVMList.refresh();
		IStructuredSelection curr = (IStructuredSelection) getSelection();
		if (!curr.equals(prev)) {
			IGrailsInstall[] installs = getJREs();
			if (curr.size() == 0 && installs.length == 1) {
				// pick a default VM automatically
				setSelection(new StructuredSelection(installs[0]));
			}
		}
		fireSelectionChanged();
	}

	protected Shell getShell() {
		return getControl().getShell();
	}

	/**
	 * Sets the checked JRE, possible <code>null</code>
	 * 
	 * @param vm JRE or <code>null</code>
	 */
	public void setCheckedJRE(IGrailsInstall vm) {
		if (vm == null) {
			setSelection(new StructuredSelection());
		}
		else {
			setSelection(new StructuredSelection(vm));
		}
	}

	/**
	 * Returns the checked JRE or <code>null</code> if none.
	 * 
	 * @return the checked JRE or <code>null</code> if none
	 */
	public IGrailsInstall getCheckedJRE() {
		Object[] objects = fVMList.getCheckedElements();
		if (objects.length == 0) {
			return null;
		}
		return (IGrailsInstall) objects[0];
	}

	/**
	 * Persist table settings into the give dialog store, prefixed with the given key.
	 * 
	 * @param settings dialog store
	 * @param qualifier key qualifier
	 */
	public void saveColumnSettings(IDialogSettings settings, String qualifier) {
		int columnCount = fTable.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			settings.put(qualifier + ".columnWidth" + i, fTable.getColumn(i).getWidth()); //$NON-NLS-1$
		}
		settings.put(qualifier + ".sortColumn", fSortColumn); //$NON-NLS-1$
	}

	/**
	 * Restore table settings from the given dialog store using the given key.
	 * 
	 * @param settings dialog settings store
	 * @param qualifier key to restore settings from
	 */
	public void restoreColumnSettings(IDialogSettings settings, String qualifier) {
		fVMList.getTable().layout(true);
		restoreColumnWidths(settings, qualifier);
		try {
			fSortColumn = settings.getInt(qualifier + ".sortColumn"); //$NON-NLS-1$
		}
		catch (NumberFormatException e) {
			fSortColumn = 1;
		}
		switch (fSortColumn) {
		case 1:
			sortByName();
			break;
		case 2:
			sortByLocation();
			break;
		}
	}

	/**
	 * Restores the column widths from dialog settings
	 * @param settings
	 * @param qualifier
	 */
	private void restoreColumnWidths(IDialogSettings settings, String qualifier) {
		int columnCount = fTable.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			int width = -1;
			try {
				width = settings.getInt(qualifier + ".columnWidth" + i); //$NON-NLS-1$
			}
			catch (NumberFormatException e) {
			}

			if (width > 0) {
				fTable.getColumn(i).setWidth(width);
			} else {
				fTable.getColumn(i).setWidth(defaultColumnWidth[i]);
			}
		}
	}

	/**
	 * Populates the JRE table with existing JREs defined in the workspace.
	 */
	protected void fillWithWorkspaceJREs() {
		Collection<IGrailsInstall> installs = GrailsCoreActivator.getDefault().getInstallManager().getAllInstalls();
		setJREs(installs.toArray(new IGrailsInstall[installs.size()]));
	}

}
