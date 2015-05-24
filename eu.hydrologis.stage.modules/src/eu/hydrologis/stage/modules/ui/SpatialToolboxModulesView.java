/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.stage.modules.ui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.hydrologis.stage.libs.utils.ImageCache;
import eu.hydrologis.stage.libs.utilsrap.MessageDialogUtil;
import eu.hydrologis.stage.libs.workspace.StageWorkspace;
import eu.hydrologis.stage.libs.workspace.User;
import eu.hydrologis.stage.modules.SpatialToolboxSessionPluginSingleton;
import eu.hydrologis.stage.modules.core.ModuleDescription;
import eu.hydrologis.stage.modules.core.ModuleDescription.Status;
import eu.hydrologis.stage.modules.core.ScriptHandler;
import eu.hydrologis.stage.modules.core.StageModulesManager;
import eu.hydrologis.stage.modules.utils.SpatialToolboxConstants;
import eu.hydrologis.stage.modules.utils.ViewerFolder;
import eu.hydrologis.stage.modules.utils.ViewerModule;
import eu.hydrologis.stage.modules.widgets.ModuleGui;

/**
 * The stage modules view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialToolboxModulesView {

    private static final String PARAMETERS = "Parameters";
    private static final String EXECUTION_TOOLS = "Execution";
    private static final String CHARACTER_ENCODING = "Encoding";
    private static final String DEBUG_INFO = "Debug info";
    private static final String MEMORY_MB = "Memory [MB]";
    private static final String PROCESS_SETTINGS = "Process settings";
    private static final String LOAD_EXPERIMENTAL_MODULES = "Load experimental modules";
    private static final String NO_MODULE_SELECTED = "No module selected";
    private static final String MODULES = "Modules";

    private Composite modulesGuiComposite;
    private StackLayout modulesGuiStackLayout;
    private TreeViewer modulesViewer;

    private ModuleDescription currentSelectedModule;
    private ModuleGui currentSelectedModuleGui;
    private String filterTextString;

    private Display display;
    private org.eclipse.swt.widgets.List logList;

    public void createStageModulesTab( Display display, Composite parent, CTabItem stageTab ) throws IOException {
        this.display = display;
        SashForm mainComposite = new SashForm(parent, SWT.HORIZONTAL);
        mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite leftComposite = new Composite(mainComposite, SWT.None);
        GridLayout leftLayout = new GridLayout(1, true);
        leftLayout.marginWidth = 0;
        leftLayout.marginHeight = 0;
        leftComposite.setLayout(leftLayout);
        GridData leftGD = new GridData(GridData.FILL, GridData.FILL, true, true);
        leftComposite.setLayoutData(leftGD);

        Group modulesListGroup = new Group(leftComposite, SWT.NONE);
        modulesListGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        modulesListGroup.setLayout(new GridLayout(2, false));
        modulesListGroup.setText(MODULES);

        addTextFilter(modulesListGroup);
        modulesViewer = createTreeViewer(modulesListGroup);
        List<ViewerFolder> viewerFolders = new ArrayList<ViewerFolder>();
        modulesViewer.setInput(viewerFolders);
        addFilterButtons(modulesListGroup);

        addQuickSettings(leftComposite);

        Group parametersTabsGroup = new Group(mainComposite, SWT.NONE);
        GridData modulesGuiCompositeGD = new GridData(SWT.FILL, SWT.FILL, true, true);
        modulesGuiCompositeGD.horizontalSpan = 2;
        modulesGuiCompositeGD.verticalSpan = 2;
        parametersTabsGroup.setLayoutData(modulesGuiCompositeGD);
        parametersTabsGroup.setLayout(new GridLayout(1, false));
        parametersTabsGroup.setText(PARAMETERS);

        modulesGuiComposite = new Composite(parametersTabsGroup, SWT.BORDER);
        modulesGuiStackLayout = new StackLayout();
        modulesGuiStackLayout.marginWidth = 15;
        modulesGuiStackLayout.marginHeight = 15;
        modulesGuiComposite.setLayout(modulesGuiStackLayout);
        modulesGuiComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label noModuleLabel = getNoModuleLabel();
        modulesGuiStackLayout.topControl = noModuleLabel;

        addRunTools(mainComposite);

        mainComposite.setWeights(new int[]{1, 3, 2});
        try {
            relayout(false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        stageTab.setControl(mainComposite);
    }

    private Label getNoModuleLabel() {
        Label noModuleLabel = new Label(modulesGuiComposite, SWT.NONE);
        noModuleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        noModuleLabel.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        noModuleLabel.setText("<span style='font:bold 26px Arial;'>" + NO_MODULE_SELECTED + "</span>");
        return noModuleLabel;
    }

    private void addTextFilter( Composite modulesComposite ) {

        Label filterLabel = new Label(modulesComposite, SWT.NONE);
        filterLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        filterLabel.setText("Filter");

        final Text filterText = new Text(modulesComposite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
        filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        filterText.addModifyListener(new ModifyListener(){
            private static final long serialVersionUID = 1L;

            public void modifyText( ModifyEvent event ) {
                filterTextString = filterText.getText();
                try {
                    if (filterTextString == null || filterTextString.length() == 0) {
                        relayout(false, null);
                    } else {
                        relayout(true, filterTextString);
                    }
                } catch (InvocationTargetException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private TreeViewer createTreeViewer( Composite modulesComposite ) {

        // FIXME
        // PatternFilter patternFilter = new PatternFilter();
        // final FilteredTree filter = new FilteredTree(modulesComposite,
        // SWT.SINGLE | SWT.BORDER, patternFilter, true);
        // final TreeViewer modulesViewer = filter.getViewer();
        final TreeViewer modulesViewer = new TreeViewer(modulesComposite);

        Control control = modulesViewer.getControl();
        GridData controlGD = new GridData(SWT.FILL, SWT.FILL, true, true);
        controlGD.horizontalSpan = 2;
        control.setLayoutData(controlGD);
        modulesViewer.setContentProvider(new ITreeContentProvider(){
            private static final long serialVersionUID = 1L;

            public Object[] getElements( Object inputElement ) {
                return getChildren(inputElement);
            }

            public Object[] getChildren( Object parentElement ) {
                if (parentElement instanceof List< ? >) {
                    List< ? > list = (List< ? >) parentElement;
                    Object[] array = list.toArray();
                    return array;
                }
                if (parentElement instanceof ViewerFolder) {
                    ViewerFolder folder = (ViewerFolder) parentElement;
                    List<ViewerFolder> subFolders = folder.getSubFolders();
                    List<ViewerModule> modules = folder.getModules();
                    List<Object> allObjs = new ArrayList<Object>();
                    allObjs.addAll(subFolders);
                    allObjs.addAll(modules);

                    return allObjs.toArray();
                }
                return new Object[0];
            }

            public Object getParent( Object element ) {
                if (element instanceof ViewerFolder) {
                    ViewerFolder folder = (ViewerFolder) element;
                    return folder.getParentFolder();
                }
                if (element instanceof ViewerModule) {
                    ViewerModule module = (ViewerModule) element;
                    return module.getParentFolder();
                }
                return null;
            }

            public boolean hasChildren( Object element ) {
                return getChildren(element).length > 0;
            }

            public void dispose() {
            }

            public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            }

        });

        modulesViewer.setLabelProvider(new LabelProvider(){
            private static final long serialVersionUID = 1L;

            public Image getImage( Object element ) {
                if (element instanceof ViewerFolder) {
                    return ImageCache.getInstance().getImage(display, ImageCache.CATEGORY);
                }
                if (element instanceof ViewerModule) {
                    ModuleDescription md = ((ViewerModule) element).getModuleDescription();
                    Status status = md.getStatus();
                    if (status == Status.experimental) {
                        return ImageCache.getInstance().getImage(display, ImageCache.MODULEEXP);
                    } else {
                        return ImageCache.getInstance().getImage(display, ImageCache.MODULE);
                    }
                }
                return null;
            }

            public String getText( Object element ) {
                if (element instanceof ViewerFolder) {
                    ViewerFolder categoryFolder = (ViewerFolder) element;
                    String name = categoryFolder.getName();
                    name = name.replaceFirst("/", "");
                    return name;
                }
                if (element instanceof ViewerModule) {
                    ModuleDescription module = ((ViewerModule) element).getModuleDescription();
                    return module.getName().replaceAll("\\_\\_", ".");
                }
                return ""; //$NON-NLS-1$
            }
        });

        modulesViewer.addSelectionChangedListener(new ISelectionChangedListener(){

            public void selectionChanged( SelectionChangedEvent event ) {
                if (!(event.getSelection() instanceof IStructuredSelection)) {
                    return;
                }
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();

                Object selectedItem = sel.getFirstElement();
                if (selectedItem == null) {
                    // unselected, show empty panel
                    putUnselected();
                    return;
                }

                if (selectedItem instanceof ViewerModule) {
                    currentSelectedModule = ((ViewerModule) selectedItem).getModuleDescription();
                    currentSelectedModuleGui = new ModuleGui(currentSelectedModule);

                    Control control = currentSelectedModuleGui.makeGui(modulesGuiComposite, false);

                    // Label dummyLabel = new Label(modulesGuiComposite,
                    // SWT.NONE);
                    // dummyLabel.setLayoutData(new
                    // GridData(SWT.BEGINNING, SWT.CENTER, false,
                    // false));
                    // dummyLabel.setText(currentSelectedModule.toString());

                    modulesGuiStackLayout.topControl = control;
                    modulesGuiComposite.layout(true);
                } else {
                    currentSelectedModuleGui = null;
                    putUnselected();
                }
            }

        });

        return modulesViewer;
    }

    private void addFilterButtons( Composite modulesComposite ) {
        Button filterActive = new Button(modulesComposite, SWT.CHECK);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        filterActive.setLayoutData(gd);
        filterActive.setText(LOAD_EXPERIMENTAL_MODULES);
        final ExperimentalFilter activeFilter = new ExperimentalFilter();
        // modulesViewer.addFilter(activeFilter);
        filterActive.setSelection(true);
        filterActive.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            public void widgetSelected( SelectionEvent event ) {
                if (((Button) event.widget).getSelection())
                    modulesViewer.removeFilter(activeFilter);
                else
                    modulesViewer.addFilter(activeFilter);
            }
        });
    }

    private void addQuickSettings( Composite modulesListComposite ) throws IOException {
        Group quickSettingsGroup = new Group(modulesListComposite, SWT.NONE);
        quickSettingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        quickSettingsGroup.setLayout(new GridLayout(2, false));
        quickSettingsGroup.setText(PROCESS_SETTINGS);

        Label heapLabel = new Label(quickSettingsGroup, SWT.NONE);
        heapLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        heapLabel.setText(MEMORY_MB);
        final Combo heapCombo = new Combo(quickSettingsGroup, SWT.DROP_DOWN);
        heapCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        heapCombo.setItems(SpatialToolboxConstants.HEAPLEVELS);
        int savedHeapLevel = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedHeap();
        for( int i = 0; i < SpatialToolboxConstants.HEAPLEVELS.length; i++ ) {
            if (SpatialToolboxConstants.HEAPLEVELS[i].equals(String.valueOf(savedHeapLevel))) {
                heapCombo.select(i);
                break;
            }
        }
        heapCombo.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            public void widgetSelected( SelectionEvent e ) {
                String item = heapCombo.getText();
                try {
                    SpatialToolboxSessionPluginSingleton.getInstance().saveHeap(Integer.parseInt(item));
                } catch (NumberFormatException | IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        heapCombo.addModifyListener(new ModifyListener(){
            private static final long serialVersionUID = 1L;

            public void modifyText( ModifyEvent e ) {
                String item = heapCombo.getText();
                try {
                    Integer.parseInt(item);
                } catch (Exception ex) {
                    return;
                }
                if (item.length() > 0) {
                    try {
                        SpatialToolboxSessionPluginSingleton.getInstance().saveHeap(Integer.parseInt(item));
                    } catch (NumberFormatException | IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });

        Label logLabel = new Label(quickSettingsGroup, SWT.NONE);
        logLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        logLabel.setText(DEBUG_INFO);
        final Combo logCombo = new Combo(quickSettingsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        logCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        logCombo.setItems(SpatialToolboxConstants.LOGLEVELS_GUI);
        String savedLogLevel = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedLogLevel();
        for( int i = 0; i < SpatialToolboxConstants.LOGLEVELS_GUI.length; i++ ) {
            if (SpatialToolboxConstants.LOGLEVELS_GUI[i].equals(savedLogLevel)) {
                logCombo.select(i);
                break;
            }
        }
        logCombo.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            public void widgetSelected( SelectionEvent e ) {
                String item = logCombo.getText();
                try {
                    SpatialToolboxSessionPluginSingleton.getInstance().saveLogLevel(item);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        Label chartsetLabel = new Label(quickSettingsGroup, SWT.NONE);
        chartsetLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        chartsetLabel.setText(CHARACTER_ENCODING);

        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
        ArrayList<String> charsetList = new ArrayList<String>();
        charsetList.add("");
        for( Entry<String, Charset> charset : charsetMap.entrySet() ) {
            charsetList.add(charset.getKey());
        }
        String[] charsetArray = charsetList.toArray(new String[0]);
        final Combo encodingCombo = new Combo(quickSettingsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        encodingCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        encodingCombo.setItems(charsetArray);
        String savedEncoding = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedEncoding();
        for( int i = 0; i < charsetArray.length; i++ ) {
            if (charsetArray[i].equals(savedEncoding)) {
                encodingCombo.select(i);
                break;
            }
        }
        encodingCombo.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            public void widgetSelected( SelectionEvent e ) {
                String item = encodingCombo.getText();
                try {
                    SpatialToolboxSessionPluginSingleton.getInstance().saveEncoding(item);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

    }

    private void addRunTools( Composite modulesListComposite ) throws IOException {
        Group runToolsGroup = new Group(modulesListComposite, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 2;
        gridData.verticalSpan = 2;
        runToolsGroup.setLayoutData(gridData);
        GridLayout gridLayout = new GridLayout(2, false);
        runToolsGroup.setLayout(gridLayout);
        runToolsGroup.setText(EXECUTION_TOOLS);

        Button runModuleButton = new Button(runToolsGroup, SWT.PUSH);
        runModuleButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
        // runModuleButton.setText("Run module");
        runModuleButton.setToolTipText("Run module");
        runModuleButton.setImage(ImageCache.getInstance().getImage(display, ImageCache.RUN));
        runModuleButton.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            @Override
            public void widgetSelected( SelectionEvent e ) {
                try {
                    runSelectedModule();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        logList = new org.eclipse.swt.widgets.List(runToolsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        logList.setLayoutData(new GridData(GridData.FILL_BOTH));
        logList.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
    }

    private static class ExperimentalFilter extends ViewerFilter {
        private static final long serialVersionUID = 1L;

        public boolean select( Viewer arg0, Object arg1, Object arg2 ) {
            if (arg2 instanceof ViewerFolder) {
                return true;
            }
            if (arg2 instanceof ViewerModule) {
                ModuleDescription md = ((ViewerModule) arg2).getModuleDescription();
                if (md.getStatus() == ModuleDescription.Status.experimental) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Resfresh the viewer.
     * 
     * @param filterText
     * 
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    public void relayout( final boolean expandAll, final String filterText ) throws InvocationTargetException,
            InterruptedException {
        TreeMap<String, List<ModuleDescription>> availableModules = StageModulesManager.getInstance().browseModules(false);
        final List<ViewerFolder> viewerFolders = ViewerFolder.hashmap2ViewerFolders(availableModules, filterText);

        Display.getDefault().syncExec(new Runnable(){
            public void run() {
                modulesViewer.setInput(viewerFolders);
                if (expandAll)
                    modulesViewer.expandAll();
            }
        });
    }

    /**
     * Put the properties view to an label that defines no selection.
     */
    public void putUnselected() {
        Label noModuleLabel = getNoModuleLabel();
        modulesGuiStackLayout.topControl = noModuleLabel;
        modulesGuiComposite.layout(true);
    }

    /**
     * Runs the module currently selected in the gui.
     * 
     * @throws Exception
     */
    public void runSelectedModule() throws Exception {
        ScriptHandler scriptHandler = new ScriptHandler();
        if (currentSelectedModuleGui == null) {
            MessageDialogUtil.openWarning(modulesGuiComposite.getShell(), "WARNING", NO_MODULE_SELECTED, null);
            return;
        }
        String script = scriptHandler.genereateScript(currentSelectedModuleGui, display.getActiveShell());
        if (script == null) {
            return;
        }

        String scriptID = currentSelectedModuleGui.getModuleDescription().getName() + " "
                + SpatialToolboxConstants.dateTimeFormatterYYYYMMDDHHMMSS.format(new Date());
        logList.removeAll();

        String currentUserName = User.getCurrentUserName();
        String dataFolder = StageWorkspace.getInstance().getDataFolder(currentUserName).getAbsolutePath();
        dataFolder = StageWorkspace.makeSafe(dataFolder);
        scriptHandler.runModule(scriptID, script, logList, dataFolder);
    }

    /**
     * Generates the script for the module currently selected in the gui.
     * 
     * @return the generated script.
     * @throws Exception
     */
    public String generateScriptForSelectedModule() throws Exception {
        ScriptHandler handler = new ScriptHandler();
        if (currentSelectedModuleGui == null) {
            return null;
        }
        String script = handler.genereateScript(currentSelectedModuleGui, display.getActiveShell());
        return script;
    }

    public ModuleDescription getCurrentSelectedModule() {
        return currentSelectedModule;
    }
}