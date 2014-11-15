/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package eu.udig.omsbox.view.actions;

import java.io.File;
import java.util.Date;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import eu.udig.omsbox.utils.FileUtilities;
import eu.udig.omsbox.utils.OmsBoxConstants;
import eu.udig.omsbox.view.OmsBoxView;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OmsScriptGenerationAction implements IViewActionDelegate {

    private IViewPart view;

    public void init( IViewPart view ) {
        this.view = view;
    }

    public void run( IAction action ) {
        if (view instanceof OmsBoxView) {
            OmsBoxView omsView = (OmsBoxView) view;
            try {
                String script = omsView.generateScriptForSelectedModule();
                if (script == null) {
                    return;
                }
                Program program = Program.findProgram(".txt");
                if (program != null) {
                    File tempFile = File.createTempFile("omsbox_", ".oms");
                    if (tempFile == null || !tempFile.exists() || tempFile.getAbsolutePath() == null) {
                        // try with user's home folder
                        String ts = OmsBoxConstants.dateTimeFormatterYYYYMMDDHHMMSS.format(new Date());
                        String userHomePath = System.getProperty("user.home"); //$NON-NLS-1$

                        File userHomeFile = new File(userHomePath);
                        if (!userHomeFile.exists()) {
                            String message = "Unable to create the oms script both in the temp folder and user home. Check your permissions.";
                            MessageDialog.openError(omsView.getViewSite().getShell(), "ERROR", message);
                            return;
                        }
                        tempFile = new File(userHomeFile, "omsbox_" + ts + ".oms");
                    }
                    FileUtilities.writeFile(script,tempFile);

                    program.execute(tempFile.getAbsolutePath());

                    // cleanup when leaving uDig
                    // tempFile.deleteOnExit();
                } else {
                    // make it the good old way prompting
                    FileDialog fileDialog = new FileDialog(view.getSite().getShell(), SWT.SAVE);
                    String path = fileDialog.open();
                    if (path == null || path.length() < 1) {
                        return;
                    }
                    FileUtilities.writeFile(script, new File(path));
                }

            } catch (Exception e) {
                e.printStackTrace();
                String message = "An error ocurred while generating the script.";
                MessageDialog.openError(omsView.getViewSite().getShell(), "ERROR", message);
            }

        }
    }

    public void selectionChanged( IAction action, ISelection selection ) {
    }

}
