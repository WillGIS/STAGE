package eu.hydrologis.stage.treeslicesviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptLoader;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.hydrologis.stage.libs.utils.FileUtilities;

public class TreeSlicerDialog extends Dialog {

    public static final String SESSION_USER_KEY = "SESSION_USER";

    private static final long serialVersionUID = 1L;

    private String htmlUrl;

    private File treeSlicesFolder;

    // private static final String CANCEL = "Cancel";

    public TreeSlicerDialog( Shell parent, File treeSlicesFolder ) {
        super(parent);
        this.treeSlicesFolder = treeSlicesFolder;
        JsResources.ensureJavaScriptResources();
        htmlUrl = JsResources.ensureHtmlResources();
        // htmlUrl = JsResources.registerIfMissing("trees_info.html");
    }

    @Override
    protected void configureShell( Shell shell ) {
        super.configureShell(shell);
        // if (title != null) {
        shell.setText("PLOTS FOLDER: " + treeSlicesFolder.getName());
        // }

        shell.setMaximized(true);
    }

    @SuppressWarnings("serial")
    @Override
    protected Control createDialogArea( Composite parent ) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));

        try {
            final Browser treeSlicerBrowser = new Browser(composite, SWT.NONE);
            treeSlicerBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            // treeSlicerBrowser.setUrl(RWT.getRequest().getContextPath() + "trees_info.html");
            treeSlicerBrowser.setUrl(htmlUrl);
            treeSlicerBrowser.addProgressListener(new ProgressListener(){
                public void completed( ProgressEvent event ) {
                    new BrowserFunction(treeSlicerBrowser, "getPlotFiles"){
                        @Override
                        public Object function( Object[] arguments ) {
                            File[] plotFiles = treeSlicesFolder.listFiles(new FilenameFilter(){
                                @Override
                                public boolean accept( File dir, String name ) {
                                    return name.endsWith(".json");
                                }
                            });
                            Arrays.sort(plotFiles);
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            for( File plotFile : plotFiles ) {
                                String name = FileUtilities.getNameWithoutExtention(plotFile);
                                sb.append("{\"name\":\"").append(name).append("\"},");
                            }
                            sb.append("{\"name\":\" - \"}]");
                            return sb.toString();
                        }
                    };
                    new BrowserFunction(treeSlicerBrowser, "getPlotData"){
                        @Override
                        public Object function( Object[] arguments ) {
                            File plotFile = new File(treeSlicesFolder, arguments[0] + ".json");
                            if (plotFile.exists()) {
                                try {
                                    String fileJson = FileUtilities.readFile(plotFile);
                                    return fileJson;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            throw new RuntimeException("Plot file does not exist: " + plotFile.getName());
                        }
                    };
                    treeSlicerBrowser.evaluate("loadScript();");
                }
                public void changed( ProgressEvent event ) {
                }
            });

            // String treeHtml = getTreeHtml();
            // treeSlicerBrowser.setText(treeHtml);

            // new GetSelectedPlotFilesFunction(treeSlicerBrowser);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return composite;
    }
    public static String getTreeHtml() throws Exception {
        InputStream inputStream = JsResources.class.getClassLoader().getResourceAsStream("js/trees_info.html");

        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while( (line = reader.readLine()) != null ) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent ) {
        // createButton(parent, IDialogConstants.CANCEL_ID, CANCEL, false);
    }

    @Override
    protected void buttonPressed( int buttonId ) {
        super.buttonPressed(buttonId);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1000, 800);
    }

    @Override
    public boolean close() {
        return super.close();
    }

}