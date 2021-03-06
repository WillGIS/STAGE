/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.stage.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import eu.hydrologis.stage.libs.log.StageLogger;
import eu.hydrologis.stage.libs.utils.StageUtils;
import eu.hydrologis.stage.libs.utilsrap.LoginDialog;

/**
 * The main entry point for STAGE.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class StageEntryPoint extends AbstractEntryPoint {

    @Override
    protected void createContents( final Composite parent ) {
        // Login screen
        if (!LoginDialog.checkUserLogin(getShell())) {
            Label errorLabel = new Label(parent, SWT.NONE);
            errorLabel.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
            errorLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
            String errorMessage = "No permission to proceed.<br/>Please check your password or contact your administrator.";
            errorLabel.setText("<span style='font:bold 24px Arial;'>" + errorMessage + "</span>");
            return;
        }

        // ApplicationContext applicationContext = RWT.getApplicationContext();

        Composite mainComposite = new Composite(parent, SWT.None);
        GridLayout mainLayout = new GridLayout(1, true);
        mainComposite.setLayout(mainLayout);
        mainLayout.marginBottom = 30;
        mainLayout.marginTop = 30;

        mainComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        Label titleLabel = new Label(mainComposite, SWT.NONE);
        titleLabel.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        titleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        String titleMessage = "Welcome to S.T.A.G.E.";
        titleLabel.setText("<span style='font:bold 24px Arial;'>" + titleMessage + "</span>");

        Group appsGroup = new Group(mainComposite, SWT.NONE);
        appsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        GridLayout appsLayout = new GridLayout(1, true);
        appsGroup.setLayout(appsLayout);
        appsLayout.marginBottom = 30;
        appsLayout.marginTop = 30;
        appsLayout.marginLeft = 80;
        appsLayout.marginRight = 80;
        appsGroup.setText("Available modules");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("eu.hydrologis.stage.entrypoint");

        List<String[] > apps = new ArrayList<>();
        if (point != null) {
            IExtension[] extensions = point.getExtensions();
            TreeMap<String, IConfigurationElement> elementsToKeep = new TreeMap<>();
            for( IExtension extension : extensions ) {
                IConfigurationElement[] configurationElements = extension.getConfigurationElements();
                for( IConfigurationElement element : configurationElements ) {
                    try {
                        String title = element.getAttribute("title");
                        elementsToKeep.put(title, element);
                    } catch (Exception e) {
                        StageLogger.logError(this, e);
                    }
                }
            }
            for( Entry<String, IConfigurationElement> entry : elementsToKeep.entrySet() ) {
                String title = entry.getKey();
                IConfigurationElement element = entry.getValue();
                String path = element.getAttribute("path");
                if (title != null && path != null && title.length() > 0 && path.length() > 0) {
                    apps.add(new String[]{title, path});
                }
            }
        }

        for( String[] app : apps ) {
            final Button appButton = new Button(appsGroup, SWT.PUSH);
            appButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            appButton.setText(app[0]);
            appButton.setData(app[1]);
            appButton.addSelectionListener(new SelectionAdapter(){
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    StageUtils.openUrl(appButton.getData().toString(), false);
                }
            });

        }

        Label footerLabel = new Label(mainComposite, SWT.NONE);
        footerLabel.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        footerLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        String footerMessage = "S.T.A.G.E. is brought to you by <a href=\"http://www.hydrologis.com\">HydroloGIS S.r.l.</a>";
        footerLabel.setText("<span style='font:italic 10px Arial;'>" + footerMessage + "</span>");

    }

}
