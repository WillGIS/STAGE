/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.stage.spatialite;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.las.spatialite.LasCell;
import org.jgrasstools.gears.io.las.spatialite.LasCellsTable;
import org.jgrasstools.gears.io.las.spatialite.LasSource;
import org.jgrasstools.gears.io.las.spatialite.LasSourcesTable;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import eu.hydrologis.stage.libs.log.StageLogger;
import eu.hydrologis.stage.libs.utils.ImageCache;
import eu.hydrologis.stage.libs.utils.StageProgressBar;
import eu.hydrologis.stage.libs.utilsrap.LoginDialog;
import eu.hydrologis.stage.libs.workspace.StageWorkspace;
import eu.hydrologis.stage.libs.workspace.User;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LidarViewerEntryPoint extends AbstractEntryPoint {

    private static final long serialVersionUID = 1L;

    private Display display;

    private SpatialiteDb currentConnectedDatabase;

    private Shell parentShell;

    private StageProgressBar generalProgressBar;

    private Label connectedDbLabel;

    private Browser mapBrowser;

    private Text databaseNameText;

    private String lidarMapUrl;

    private LidarViewerProgressListener lidarViewerProgressListener;

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

        parentShell = parent.getShell();
        display = parent.getDisplay();

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(2, false));

        Composite leftComposite = new Composite(composite, SWT.None);
        GridLayout leftLayout = new GridLayout(3, false);
        leftLayout.marginWidth = 0;
        leftLayout.marginHeight = 0;
        leftComposite.setLayout(leftLayout);
        GridData leftGD = new GridData(GridData.FILL, GridData.FILL, false, true);
        // leftGD.widthHint = 100;
        leftComposite.setLayoutData(leftGD);

        connectedDbLabel = new Label(leftComposite, SWT.NONE);
        connectedDbLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        connectedDbLabel.setText("Database: ");

        databaseNameText = new Text(leftComposite, SWT.BORDER);
        databaseNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        databaseNameText.setText("                          ");
        databaseNameText.setEditable(false);

        Button connectDatabaseButton = new Button(leftComposite, SWT.PUSH);
        // connectDatabaseButton.setText("");
        connectDatabaseButton.setImage(ImageCache.getInstance().getImage(display, ImageCache.CONNECT));
        connectDatabaseButton.setToolTipText("Select database to connect to.");
        connectDatabaseButton.addSelectionListener(new SelectionAdapter(){
            private static final long serialVersionUID = 1L;

            @Override
            public void widgetSelected( SelectionEvent e ) {
                openDatabase(parent.getShell());
            }
        });

        Composite rightComposite = new Composite(composite, SWT.None);
        GridLayout rightLayout = new GridLayout(1, true);
        rightLayout.marginWidth = 0;
        rightLayout.marginHeight = 0;
        rightComposite.setLayout(rightLayout);
        GridData rightGD = new GridData(GridData.FILL, GridData.FILL, true, true);
        rightComposite.setLayoutData(rightGD);

        try {
            mapBrowser = new Browser(rightComposite, SWT.NONE);
            GridData mapBrowserGD = new GridData(SWT.FILL, SWT.FILL, true, true);
            mapBrowser.setLayoutData(mapBrowserGD);

            JsResources.ensureJavaScriptResources();
            lidarMapUrl = JsResources.ensureLidarmapHtmlResource();

            lidarViewerProgressListener = new LidarViewerProgressListener();

        } catch (Exception e) {
            e.printStackTrace();
        }

        GridData progressBarGD = new GridData(SWT.FILL, SWT.FILL, true, false);
        progressBarGD.horizontalSpan = 2;
        generalProgressBar = new StageProgressBar(composite, SWT.HORIZONTAL | SWT.INDETERMINATE, progressBarGD);

    }

    private void openDatabase( final Shell shell ) {
        try {
            closeCurrentDb();
        } catch (Exception e1) {
            StageLogger.logError(this, e1);
        }
        File userFolder = StageWorkspace.getInstance().getDataFolder(User.getCurrentUserName());
        SpatialiteSelectionDialog fileDialog = new SpatialiteSelectionDialog(parentShell, userFolder);
        final int returnCode = fileDialog.open();
        if (returnCode == SWT.OK) {
            final File selectedFile = fileDialog.getSelectedFile();
            if (selectedFile != null) {
                generalProgressBar.setProgressText("Connect to existing database");
                generalProgressBar.start(0);
                new Thread(new Runnable(){
                    public void run() {
                        shell.getDisplay().syncExec(new Runnable(){
                            public void run() {
                                try {

                                    closeCurrentDb();

                                    currentConnectedDatabase = new SpatialiteDb();
                                    currentConnectedDatabase.open(selectedFile.getAbsolutePath());

                                    databaseNameText.setText(selectedFile.getName());

                                    mapBrowser.setUrl(lidarMapUrl);
                                    mapBrowser.addProgressListener(lidarViewerProgressListener);
                                    // DbLevel dbLevel =
                                    // gatherDatabaseLevels(currentConnectedDatabase);
                                } catch (Exception e) {
                                    currentConnectedDatabase = null;
                                    StageLogger.logError(LidarViewerEntryPoint.this, e);
                                } finally {
                                    generalProgressBar.stop();
                                }
                            }
                        });
                    }
                }).start();
            } else {
                generalProgressBar.stop();
            }
        } else {
            generalProgressBar.stop();
        }

    }

    private void closeCurrentDb() throws Exception {
        if (currentConnectedDatabase != null) {
            currentConnectedDatabase.close();
            currentConnectedDatabase = null;
        }
    }

    public void selected( boolean selected ) {
        // /*
        // * execution shortcut
        // */
        // if (selected) {
        // runKeyListener = new Listener(){
        // private static final long serialVersionUID = 1L;
        //
        // public void handleEvent( Event event ) {
        // createNewDatabase(parent.getShell());
        // }
        // };
        // String[] shortcut = new String[]{"ALT+ENTER"};
        // display.setData(RWT.ACTIVE_KEYS, shortcut);
        // display.setData(RWT.CANCEL_KEYS, shortcut);
        // display.addFilter(SWT.KeyDown, runKeyListener);
        // } else {
        // if (runKeyListener != null)
        // display.removeFilter(SWT.KeyDown, runKeyListener);
        // }
    }

    private class LidarViewerProgressListener implements ProgressListener {
        private static final long serialVersionUID = 1L;
        public void completed( ProgressEvent event ) {
            new BrowserFunction(mapBrowser, "getJsonData"){
                @Override
                public Object function( Object[] arguments ) {
                    int type = (int) getDouble(arguments[0]); // 1,
                    double south = getDouble(arguments[1]); // s,
                    double north = getDouble(arguments[2]); // n,
                    double west = getDouble(arguments[3]); // w,
                    double east = getDouble(arguments[4]); // e,
                    int zoom = (int) getDouble(arguments[5]); // zoom

                    StringBuilder sb = new StringBuilder();
                    sb.append("type=").append(type).append("\n");
                    sb.append("south=").append(south).append("\n");
                    sb.append("north=").append(north).append("\n");
                    sb.append("west=").append(west).append("\n");
                    sb.append("east=").append(east).append("\n");
                    sb.append("zoom=").append(zoom).append("\n***************************************");
                    System.out.println(sb.toString());

                    Envelope llEnv = new Envelope(west, east, south, north);

                    try {
                        if (type == 1) {
                            return getSources(llEnv);
                        } else if (type == 2) {
                            return getCells(llEnv);
                        } else if (type == 3) {
                            return getPoints(llEnv);
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    return "";
                }

            };
            mapBrowser.evaluate("loadScript();");
        }
        public void changed( ProgressEvent event ) {
        }
    }

    private Object getSources( Envelope env ) throws NoSuchAuthorityCodeException, FactoryException, Exception, IOException {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632");

        CoordinateReferenceSystem leafletCRS = DefaultGeographicCRS.WGS84;// CRS.decode("EPSG:4326");
        MathTransform ll2CRSTransform = CRS.findMathTransform(leafletCRS, crs);
        MathTransform crs2llTransform = CRS.findMathTransform(crs, leafletCRS);
        Envelope searchEnv = JTS.transform(env, ll2CRSTransform);

        List<LasSource> lasSources = LasSourcesTable.getLasSources(currentConnectedDatabase);

        JSONObject rootObj = new JSONObject();
        JSONArray dataArray = new JSONArray();
        rootObj.put("data", dataArray);

        Envelope bounds = null;
        int index = 0;
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        for( LasSource lasSource : lasSources ) {
            Envelope geomEnvCRS = lasSource.polygon.getEnvelopeInternal();
            if (!geomEnvCRS.intersects(searchEnv)) {
                continue;
            }

            Envelope geomEnvLL = JTS.transform(geomEnvCRS, crs2llTransform);

            if (bounds == null) {
                bounds = new Envelope(geomEnvLL);
            } else {
                bounds.expandToInclude(geomEnvLL);
            }

            minValue = Math.min(minValue, lasSource.maxElev);
            maxValue = Math.max(maxValue, lasSource.maxElev);
            Coordinate[] coordinates = lasSource.polygon.getCoordinates();
            JSONObject dataObj = new JSONObject();
            dataArray.put(index++, dataObj);
            Coordinate newCoord = new Coordinate();
            for( int i = 0; i < coordinates.length; i++ ) {
                JTS.transform(coordinates[i], newCoord, crs2llTransform);

                dataObj.put("x" + (i + 1), newCoord.x);
                dataObj.put("y" + (i + 1), newCoord.y);
            }

            dataObj.put("l", lasSource.name);
            dataObj.put("v", lasSource.maxElev);
        }
        rootObj.put("xmin", bounds.getMinX());
        rootObj.put("xmax", bounds.getMaxX());
        rootObj.put("ymin", bounds.getMinY());
        rootObj.put("ymax", bounds.getMaxY());
        rootObj.put("vmin", minValue);
        rootObj.put("vmax", maxValue);

        return rootObj.toString();
    }

    private Object getCells( Envelope env ) throws NoSuchAuthorityCodeException, FactoryException, TransformException, Exception,
            IOException {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632");

        CoordinateReferenceSystem leafletCRS = DefaultGeographicCRS.WGS84;// CRS.decode("EPSG:4326");
        MathTransform ll2CRSTransform = CRS.findMathTransform(leafletCRS, crs);
        MathTransform crs2llTransform = CRS.findMathTransform(crs, leafletCRS);

        JSONObject rootObj = new JSONObject();
        JSONArray dataArray = new JSONArray();
        rootObj.put("data", dataArray);

        Polygon searchPolygonLL = GeometryUtilities.createPolygonFromEnvelope(env);
        Geometry searchPolygonLLCRS = JTS.transform(searchPolygonLL, ll2CRSTransform);

        long t1 = System.currentTimeMillis();
        List<LasCell> lasCells = LasCellsTable.getLasCells(currentConnectedDatabase, searchPolygonLLCRS, true, false, false,
                false, false);
        int size = lasCells.size();
        long t2 = System.currentTimeMillis();
        System.out.println("time: " + (t2 - t1) + " size = " + size);

        // if (size > 4000) {
        // return getSources(env);
        // }
        if (size == 0) {
            return null;
        }

        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        int index = 0;
        for( LasCell lasCell : lasCells ) {
            Coordinate[] coordinates = lasCell.polygon.getCoordinates();
            JSONObject dataObj = new JSONObject();
            dataArray.put(index++, dataObj);
            Coordinate newCoord = new Coordinate();
            for( int i = 0; i < coordinates.length; i++ ) {
                JTS.transform(coordinates[i], newCoord, crs2llTransform);

                dataObj.put("x" + (i + 1), newCoord.x);
                dataObj.put("y" + (i + 1), newCoord.y);
            }
            minValue = Math.min(minValue, lasCell.avgElev);
            maxValue = Math.max(maxValue, lasCell.avgElev);

            // dataObj.put("l", lasCell.avgElev);
            dataObj.put("v", lasCell.avgElev);
        }
        rootObj.put("vmin", minValue);
        rootObj.put("vmax", maxValue);
        return rootObj.toString();
    }

    private Object getPoints( Envelope env ) throws NoSuchAuthorityCodeException, FactoryException, TransformException,
            Exception, IOException {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632");

        CoordinateReferenceSystem leafletCRS = DefaultGeographicCRS.WGS84;// CRS.decode("EPSG:4326");
        MathTransform ll2CRSTransform = CRS.findMathTransform(leafletCRS, crs);
        MathTransform crs2llTransform = CRS.findMathTransform(crs, leafletCRS);

        JSONObject rootObj = new JSONObject();
        JSONArray dataArray = new JSONArray();
        rootObj.put("data", dataArray);

        Polygon searchPolygonLL = GeometryUtilities.createPolygonFromEnvelope(env);
        Geometry searchPolygonLLCRS = JTS.transform(searchPolygonLL, ll2CRSTransform);

        long t1 = System.currentTimeMillis();
        List<LasCell> lasCells = LasCellsTable.getLasCells(currentConnectedDatabase, searchPolygonLLCRS, true, false, false,
                false, false);
        int size = lasCells.size();
        if (size == 0) {
            return null;
        }

        int count = 0;
        for( LasCell lasCell : lasCells ) {
            count += lasCell.pointsCount;
        }
        long t2 = System.currentTimeMillis();
        System.out.println("time: " + (t2 - t1) + " exploded size = " + count);

        int index = 0;
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        for( LasCell lasCell : lasCells ) {
            double[][] cellPositions = LasCellsTable.getCellPositions(lasCell);
            Coordinate newCoord = new Coordinate();
            for( int i = 0; i < cellPositions.length; i++ ) {
                JSONObject dataObj = new JSONObject();
                dataArray.put(index++, dataObj);
                double[] xyz = cellPositions[i];

                minValue = Math.min(minValue, xyz[2]);
                maxValue = Math.max(maxValue, xyz[2]);
                JTS.transform(new Coordinate(xyz[0], xyz[1]), newCoord, crs2llTransform);

                dataObj.put("x1", newCoord.x);
                dataObj.put("y1", newCoord.y);
                // dataObj.put("l", lasCell.avgElev);
                dataObj.put("v", xyz[2]);
            }

        }
        rootObj.put("vmin", minValue);
        rootObj.put("vmax", maxValue);
        return rootObj.toString();
    }

    private double getDouble( Object object ) {
        if (object instanceof Number) {
            Number num = (Number) object;
            return num.doubleValue();
        }
        throw new RuntimeException("Not a number...");
    }

}
