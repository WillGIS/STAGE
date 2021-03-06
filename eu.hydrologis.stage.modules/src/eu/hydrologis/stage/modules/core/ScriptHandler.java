/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.stage.modules.core;

import java.util.HashMap;
import java.util.List;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.rap.rwt.service.UISession;
import org.eclipse.rap.rwt.widgets.DialogCallback;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.hydrologis.stage.libs.utilsrap.MessageDialogUtil;
import eu.hydrologis.stage.libs.workspace.StageWorkspace;
import eu.hydrologis.stage.modules.SpatialToolboxSessionPluginSingleton;
import eu.hydrologis.stage.modules.utils.SpatialToolboxConstants;
import eu.hydrologis.stage.modules.utils.SpatialToolboxUtils;
import eu.hydrologis.stage.modules.widgets.ModuleGui;
import eu.hydrologis.stage.modules.widgets.ModuleGuiElement;

/**
 * Handler for script generation from gui and script execution.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class ScriptHandler {
    private static final String ORG_JGRASSTOOLS_MODULES = "org.jgrasstools.modules";

    private static final String QUOTE = "'";

    /**
     * Map of variables bound to their {@link ModuleDescription}.
     */
    private HashMap<ModuleDescription, String> variableNamesMap = new HashMap<ModuleDescription, String>();

    private ModuleDescription mainModuleDescription;

    /**
     * Generates the script from the supplied gui.
     * 
     * @param moduleGui
     *            the gui object for which to generate the script.
     * @return the oms3 script.
     * @throws Exception
     */
    public String genereateScript( ModuleGui moduleGui, Shell shell ) throws Exception {
        variableNamesMap.clear();

        mainModuleDescription = moduleGui.getModuleDescription();
        // input
        List<ModuleGuiElement> modulesInputGuiList = moduleGui.getModulesInputGuiList();
        // output
        List<ModuleGuiElement> modulesOutputGuiList = moduleGui.getModulesOuputGuiList();

        StringBuilder sb = new StringBuilder();
        for( ModuleGuiElement mgElem : modulesInputGuiList ) {
            String res = mgElem.validateContent();
            if (res != null) {
                sb.append(res);
            }
        }
        for( ModuleGuiElement mgElem : modulesOutputGuiList ) {
            String res = mgElem.validateContent();
            if (res != null) {
                sb.append(res);
            }
        }

        if (sb.length() > 0) {
            String title = "Warning";
            String message = "The following problems were reported\n" + sb.toString();
            DialogCallback callback = new DialogCallback(){
                public void dialogClosed( int returnCode ) {
                    // showResult( "Result: " + returnCode );
                }
            };
            MessageDialogUtil.openWarning(shell, title, message, callback);

            return null;
        }

        String loggerLevelGui = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedLogLevel();
        String loggerLevelOms = SpatialToolboxConstants.LOGLEVELS_MAP.get(loggerLevelGui);
        StringBuilder scriptSb = new StringBuilder();
        scriptSb.append("import " + ORG_JGRASSTOOLS_MODULES + ".*\n\n");

        /*
         * first get all the components
         */
        StringBuilder componentsSb = new StringBuilder();
        componentsSb.append(module2ComponenDescription(mainModuleDescription));
        for( ModuleGuiElement inElement : modulesInputGuiList ) {
            if (!inElement.hasData()) {
                continue;
            }
            FieldData fieldData = inElement.getFieldData();
            if (fieldData.otherModule != null) {
                String componentDescription = module2ComponenDescription(fieldData.otherModule);
                componentsSb.append(componentDescription);
                throw new RuntimeException();
            }
        }
        for( ModuleGuiElement outElement : modulesOutputGuiList ) {
            if (!outElement.hasData()) {
                continue;
            }
            FieldData fieldData = outElement.getFieldData();
            if (fieldData.otherModule != null) {
                String componentDescription = module2ComponenDescription(fieldData.otherModule);
                componentsSb.append(componentDescription);
                throw new RuntimeException();
            }
        }
        scriptSb.append(componentsSb.toString());

        /*
         * then gather all the input component's fields
         */
        StringBuilder parametersSb = new StringBuilder();
        for( ModuleGuiElement inElement : modulesInputGuiList ) {
            if (!inElement.hasData()) {
                continue;
            }
            FieldData fieldData = inElement.getFieldData();
            field2ParameterDescription(fieldData, mainModuleDescription, parametersSb);
        }
        for( ModuleGuiElement outElement : modulesOutputGuiList ) {
            if (!outElement.hasData()) {
                continue;
            }
            FieldData fieldData = outElement.getFieldData();
            field2ParameterDescription(fieldData, mainModuleDescription, parametersSb);
        }
        scriptSb.append(parametersSb.toString());

        String varName = variableNamesMap.get(mainModuleDescription);
        scriptSb.append(varName).append(".process();\n");

        dumpSimpleOutputs(scriptSb, mainModuleDescription);

        return scriptSb.toString();
    }

    /**
     * Runs a script.
     * 
     * @param scriptId
     *            the unique id that will be used to identify the process of the
     *            run script.
     * @param script
     *            the script.
     * @param dataFolder 
     */
    public void runModule( final String scriptId, String script, final org.eclipse.swt.widgets.List logList,
            final String dataFolder ) {
        try {
            final Display display = Display.getCurrent();
            String sessionId = RWT.getUISession().getId();
            final ServerPushSession logPushSession = new ServerPushSession();
            StageScriptExecutor executor = new StageScriptExecutor();
            executor.addProcessListener(new IProcessListener(){
                public void onProcessStopped() {
                    UISession uiSession = RWT.getUISession(display);
                    uiSession.exec(new Runnable(){
                        public void run() {
                            SpatialToolboxSessionPluginSingleton.getInstance().cleanProcess(scriptId);
                            // loadOutputMaps();
                            logPushSession.stop();

                        }
                    });
                }

                @Override
                public void onMessage( final String message, boolean isError ) {
                    display.asyncExec(new Runnable(){
                        public void run() {

                            String _message = message
                                    .replaceFirst(dataFolder, StageWorkspace.STAGE_DATA_FOLDER_SUBSTITUTION_NAME);

                            String[] ignore = {"Call MapContent dispose()",//
                                    "no gdaljni",//
                                    "MediaLibLoad",//
                                    "MediaLibAcc",//
                                    "load mediaLib",//
                                    "Failed to load the GDAL",//
                            };

                            for( String ignoreStr : ignore ) {
                                if (message.contains(ignoreStr))
                                    return;
                            }
                            logList.add(_message, 0);
                            // logList.select(logList.getItemCount() - 1);
                            // logList.showSelection();
                            // int itemCount = logList.getItemCount();
                            // logList.setTopIndex(itemCount - 1);
                        }
                    });
                }

            });
            String loggerLevelGui = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedLogLevel();
            if (loggerLevelGui == null)
                loggerLevelGui = SpatialToolboxConstants.LOGLEVEL_GUI_OFF;
            String ramLevel = String.valueOf(SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedHeap());
            String encoding = SpatialToolboxSessionPluginSingleton.getInstance().retrieveSavedEncoding();
            Process process = executor.exec(sessionId, script, loggerLevelGui, ramLevel, encoding);
            logPushSession.start();

            SpatialToolboxSessionPluginSingleton.getInstance().addProcess(process, scriptId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private void loadOutputMaps() {
    // List<FieldData> outputsList = mainModuleDescription.getOutputsList();
    // List<FieldData> InputsList = mainModuleDescription.getInputsList();
    // for( FieldData fieldData : outputsList ) {
    // if (fieldData.fieldType.equals(GridCoverage2D.class.getCanonicalName())
    // ||
    // fieldData.fieldType.equals(SimpleFeatureCollection.class.getCanonicalName()))
    // {
    // if (fieldData.otherModule != null) {
    // List<FieldData> inputList2 = fieldData.otherModule.getInputsList();
    // for( FieldData fieldData2 : inputList2 ) {
    // String filePath = fieldData2.fieldValue;
    // File file = new File(filePath);
    // try {
    // if (file.exists())
    // loadFileInMap(file);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // }
    // }
    // for( FieldData fieldData : InputsList ) {
    // if (fieldData.guiHints == null)
    // continue;
    // if (fieldData.fieldType.equals(String.class.getCanonicalName())
    // && fieldData.guiHints.contains(OmsBoxConstants.FILEOUT_UI_HINT) &&
    // fieldData.fieldValue !=
    // null
    // && fieldData.fieldValue.length() > 0) {
    // String filePath = fieldData.fieldValue;
    // File file = new File(filePath);
    // try {
    // if (file.exists())
    // loadFileInMap(file);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // }
    // private void loadFileInMap( File file ) throws Exception {
    // URL fileUrl = file.toURI().toURL();
    // CatalogPlugin cp = CatalogPlugin.getDefault();
    // IServiceFactory sf = cp.getServiceFactory();
    // List<IService> services = sf.createService(fileUrl);
    // List<IGeoResource> resources = new ArrayList<IGeoResource>();
    // for( IService service : services ) {
    // List< ? extends IGeoResource> geoResource = service.resources(new
    // NullProgressMonitor());
    // if (geoResource != null) {
    // if (service instanceof JGrassService) {
    // for( IGeoResource iGeoResource : geoResource ) {
    // URL identifier = iGeoResource.getIdentifier();
    // File newFile = URLUtils.urlToFile(identifier);
    //
    // if (file.getName().equals(newFile.getName())) {
    // File parentFile = file.getParentFile();
    // File newParentFile = newFile.getParentFile();
    // if (parentFile != null && newParentFile != null
    // && parentFile.getName().equals(newParentFile.getName())) {
    // parentFile = parentFile.getParentFile();
    // newParentFile = newParentFile.getParentFile();
    // if (parentFile != null && newParentFile != null) {
    // if (parentFile.getName().equals(newParentFile.getName())) {
    // resources.add(iGeoResource);
    // }
    // } else {
    // resources.add(iGeoResource);
    // }
    // }
    // }
    // }
    // } else {
    // for( IGeoResource iGeoResource : geoResource ) {
    // resources.add(iGeoResource);
    // }
    // }
    // }
    // }
    // if (resources.size() == 0) {
    // return;
    // }
    //
    // IMap map = ApplicationGIS.getActiveMap();
    //
    // LayerFactory layerFactory = map.getLayerFactory();
    // for( IGeoResource resource : resources ) {
    // if (resource instanceof JGrassMapGeoResource) {
    // JGrassMapGeoResource grassMGR = (JGrassMapGeoResource) resource;
    // File locationFile = grassMGR.getLocationFile();
    // File mapsetFile = grassMGR.getMapsetFile();
    // File mapFile = grassMGR.getMapFile();
    // IGeoResource addedMapToCatalog =
    // JGrassCatalogUtilities.addMapToCatalog(locationFile.getAbsolutePath(),
    // mapsetFile.getName(), mapFile.getName(),
    // JGrassConstants.GRASSBINARYRASTERMAP);
    // int index = map.getMapLayers().size();
    // ApplicationGIS.addLayersToMap(map, Arrays.asList(addedMapToCatalog),
    // index);
    // } else {
    // Layer layer = layerFactory.createLayer(resource);
    // AddLayerCommand cmd = new AddLayerCommand(layer);
    // map.sendCommandASync(cmd);
    // }
    // }
    // }

    /**
     * Adds to the script a tail part to dump the outputs that are not connected
     * to any module.
     * 
     * <p>
     * These outputs are for example single double values or arrays and matrixes
     * of numbers.
     * 
     * @param scriptSb
     *            the script {@link StringBuilder} to which to add the dump
     *            commands.
     * @param mainModuleDescription
     *            the main {@link ModuleDescription module}.
     */
    private void dumpSimpleOutputs( StringBuilder scriptSb, ModuleDescription mainModuleDescription ) {
        scriptSb.append("println \"\"\n");
        scriptSb.append("println \"\"\n");

        // make print whatever is simple output
        String mainVarName = variableNamesMap.get(mainModuleDescription);
        List<FieldData> outputsList = mainModuleDescription.getOutputsList();
        for( FieldData fieldData : outputsList ) {
            if (fieldData.isSimpleType()) {
                String varPlusField = mainVarName + "." + fieldData.fieldName;
                // String ifString = "if( " + varPlusField + " != null )\n";
                // scriptSb.append(ifString);
                scriptSb.append("println \"");
                String fieldDescription = fieldData.fieldDescription.trim();
                if (fieldDescription.endsWith(".")) {
                    fieldDescription = fieldDescription.substring(0, fieldDescription.length() - 1);
                }
                scriptSb.append(fieldDescription);
                scriptSb.append(" = \" + ");
                scriptSb.append(varPlusField);
                scriptSb.append("\n");
            }
        }

        // in case make print double[] and double[][] outputs
        scriptSb.append("println \"\"\n\n");
        for( FieldData fieldData : outputsList ) {
            String varPlusField = mainVarName + "." + fieldData.fieldName;
            if (fieldData.isSimpleArrayType()) {
                if (fieldData.fieldType.equals(double[][].class.getCanonicalName())
                        || fieldData.fieldType.equals(float[][].class.getCanonicalName())
                        || fieldData.fieldType.equals(int[][].class.getCanonicalName())) {

                    String ifString = "if( " + varPlusField + " != null ) {\n";
                    scriptSb.append(ifString);
                    String typeStr = null;
                    if (fieldData.fieldType.equals(double[][].class.getCanonicalName())) {
                        typeStr = "double[][]";
                    } else if (fieldData.fieldType.equals(float[][].class.getCanonicalName())) {
                        typeStr = "float[][]";
                    } else if (fieldData.fieldType.equals(int[][].class.getCanonicalName())) {
                        typeStr = "int[][]";
                    }

                    scriptSb.append("println \"");
                    scriptSb.append(fieldData.fieldDescription);
                    scriptSb.append("\"\n");
                    scriptSb.append("println \"-----------------------------------\"\n");
                    scriptSb.append(typeStr);
                    scriptSb.append(" matrix = ");
                    scriptSb.append(varPlusField);
                    scriptSb.append("\n");

                    scriptSb.append("for( int i = 0; i < matrix.length; i++ ) {\n");
                    scriptSb.append("for( int j = 0; j < matrix[0].length; j++ ) {\n");
                    scriptSb.append("print matrix[i][j] + \" \";\n");
                    scriptSb.append("}\n");
                    scriptSb.append("println \" \";\n");
                    scriptSb.append("}\n");
                    scriptSb.append("}\n");
                    scriptSb.append("\n");
                } else if (fieldData.fieldType.equals(double[].class.getCanonicalName())
                        || fieldData.fieldType.equals(float[].class.getCanonicalName())
                        || fieldData.fieldType.equals(int[].class.getCanonicalName())) {

                    String ifString = "if( " + varPlusField + " != null ) {\n";
                    scriptSb.append(ifString);

                    String typeStr = null;
                    if (fieldData.fieldType.equals(double[].class.getCanonicalName())) {
                        typeStr = "double[]";
                    } else if (fieldData.fieldType.equals(float[].class.getCanonicalName())) {
                        typeStr = "float[]";
                    } else if (fieldData.fieldType.equals(int[].class.getCanonicalName())) {
                        typeStr = "int[]";
                    }
                    scriptSb.append("println \"");
                    scriptSb.append(fieldData.fieldDescription);
                    scriptSb.append("\"\n");
                    scriptSb.append("println \"-----------------------------------\"\n");
                    scriptSb.append(typeStr);
                    scriptSb.append(" array = ");
                    scriptSb.append(mainVarName);
                    scriptSb.append(".");
                    scriptSb.append(fieldData.fieldName);
                    scriptSb.append("\n");

                    scriptSb.append("for( int i = 0; i < array.length; i++ ) {\n");
                    scriptSb.append("println array[i] + \" \";\n");
                    scriptSb.append("}\n");
                    scriptSb.append("}\n");
                    scriptSb.append("\n");
                }
                scriptSb.append("println \" \"\n\n");
            }
        }

    }

    /**
     * Converts a module to its oms3 script description as needed in the modules
     * part.
     * 
     * @param moduleDescription
     *            the main {@link ModuleDescription module}.
     * @return the string describing the module in oms3 syntax.
     */
    private String module2ComponenDescription( ModuleDescription moduleDescription ) {
        StringBuilder sb = new StringBuilder();
        String varName = moduleDescription.getScriptName();

        String simpleClassName = moduleDescription.getName();
        String completeClassName = moduleDescription.getClassName();

        String remainingString = completeClassName.replaceAll(ORG_JGRASSTOOLS_MODULES, "");
        remainingString = remainingString.replaceAll(simpleClassName, "");
        remainingString = remainingString.replace('.', ' ');
        remainingString = remainingString.trim();
        if (remainingString.length() != 0) {
            // add the package also
            simpleClassName = completeClassName;
        }

        variableNamesMap.put(moduleDescription, varName);
        sb.append(simpleClassName).append(" ");
        sb.append(varName);
        sb.append(" = new ");
        sb.append(simpleClassName);
        sb.append("();");
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Converts module fields to their oms3 script description as needed in the
     * parameters part.
     * 
     * @param field
     * @param mainModuleDescription
     * @param sb
     */
    private void field2ParameterDescription( FieldData field, ModuleDescription mainModuleDescription, StringBuilder sb ) {
        if (field.otherModule == null && field.fieldValue != null && field.fieldValue.length() > 0) {
            boolean isString = field.fieldType.endsWith("String");
            String TMPQUOTE = QUOTE;
            if (SpatialToolboxUtils.isFieldExceptional(field)) {
                TMPQUOTE = "";
            }
            if (field.guiHints != null && field.guiHints.contains(SpatialToolboxConstants.MULTILINE_UI_HINT)) {
                TMPQUOTE = "\"\"\"";
            }
            sb.append(variableNamesMap.get(mainModuleDescription));
            sb.append(".");
            sb.append(field.fieldName);
            sb.append(" = ");
            String fieldValue = field.fieldValue;
            if (isString)
                sb.append(TMPQUOTE);
            sb.append(fieldValue);
            if (isString)
                sb.append(TMPQUOTE);
            sb.append("\n");
        } else if (field.otherModule != null) {
            ModuleDescription otherModule = field.otherModule;
            List<FieldData> inputsList = otherModule.getInputsList();
            for( FieldData fieldData : inputsList ) {
                if (fieldData.isSimpleType()) {
                    field2ParameterDescription(fieldData, otherModule, sb);
                } else if (SpatialToolboxUtils.isFieldExceptional(fieldData)) {
                    field2ParameterDescription(fieldData, otherModule, sb);
                }
            }
            List<FieldData> outputList = otherModule.getOutputsList();
            for( FieldData fieldData : outputList ) {
                if (fieldData.isSimpleType())
                    field2ParameterDescription(fieldData, otherModule, sb);
            }
            throw new RuntimeException();
        }
    }

    /**
     * Connects input modules in OMS3 script syntax.
     * 
     * @param mainModule
     * @param inData
     * @param sb
     */
    private void connectInputModules( ModuleDescription mainModule, FieldData inData, StringBuilder sb ) {
        if (inData.otherModule == null) {
            return;
        }
        ModuleDescription otherModule = inData.otherModule;
        sb.append("\t");
        sb.append(QUOTE);
        sb.append(variableNamesMap.get(otherModule));
        sb.append(".");
        sb.append(inData.otherFieldName);
        sb.append(QUOTE);
        sb.append("\t\t");
        sb.append(QUOTE);
        sb.append(variableNamesMap.get(mainModule));
        sb.append(".");
        sb.append(inData.fieldName);
        sb.append(QUOTE);
        sb.append("\n");
    }

    /**
     * Connects output modules in OMS3 script syntax.
     * 
     * @param mainModule
     * @param outData
     * @param sb
     */
    private void connectOutputModules( ModuleDescription mainModule, FieldData outData, StringBuilder sb ) {
        if (outData.otherModule == null) {
            return;
        }
        ModuleDescription otherModule = outData.otherModule;
        sb.append("\t");
        sb.append(QUOTE);
        sb.append(variableNamesMap.get(mainModule));
        sb.append(".");
        sb.append(outData.fieldName);
        sb.append(QUOTE);
        sb.append("\t\t");
        sb.append(QUOTE);
        sb.append(variableNamesMap.get(otherModule));
        sb.append(".");
        sb.append(outData.otherFieldName);
        sb.append(QUOTE);
        sb.append("\n");
    }
}
