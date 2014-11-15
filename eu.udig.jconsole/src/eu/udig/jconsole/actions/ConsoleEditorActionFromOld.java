/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package eu.udig.jconsole.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import eu.udig.jconsole.JConsoleEditorPlugin;
import eu.udig.jconsole.JavaEditor;
import eu.udig.jconsole.JavaFileEditorInput;

/**
 * Action to open an editor
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ConsoleEditorActionFromOld implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow window;

    public void dispose() {
    }

    public void init( IWorkbenchWindow window ) {
        this.window = window;
    }

    public void run( IAction action ) {
        // JConsoleEditorPlugin.getDefault().gatherModules();

        try {

            File lastOpenFolder = JConsoleEditorPlugin.getDefault().getLastOpenFolder();
            FileDialog fileDialog = new FileDialog(window.getShell(), SWT.OPEN | SWT.MULTI);
            fileDialog.setFilterExtensions(new String[]{"*.groovy"});
            fileDialog.setFilterPath(lastOpenFolder.getAbsolutePath());
            String path = fileDialog.open();
            String[] fileNames = fileDialog.getFileNames();
            if (path == null || path.length() < 1) {
                return;
            }
            File f = new File(path);
            if (!f.exists()) {
                return;
            }

            File parentFolder = f.getParentFile();
            JConsoleEditorPlugin.getDefault().setLastOpenFolder(parentFolder.getAbsolutePath());

            for( String fileName : fileNames ) {
                File tmpFile = new File(parentFolder, fileName);
                JavaFileEditorInput jFile = new JavaFileEditorInput(tmpFile);
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(jFile, JavaEditor.ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void selectionChanged( IAction action, ISelection selection ) {
    }

}
