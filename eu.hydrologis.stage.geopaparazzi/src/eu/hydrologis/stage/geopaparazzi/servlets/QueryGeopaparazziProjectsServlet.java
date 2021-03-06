/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.stage.geopaparazzi.servlets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.hydrologis.stage.geopaparazzi.geopapbrowser.utils.GeopaparazziWorkspaceUtilities;
import eu.hydrologis.stage.libs.log.StageLogger;
import eu.hydrologis.stage.libs.utils.StageUtils;
import eu.hydrologis.stage.libs.workspace.LoginChecker;

/**
 * The geopaparazzi projects list download servlet.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class QueryGeopaparazziProjectsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    static {
        try {
            // make sure sqlite driver are there
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
        }
    }

    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        resp.setContentType("application/json");

        String authHeader = req.getHeader("Authorization");

        String[] userPwd = StageUtils.getUserPwdWithBasicAuthentication(authHeader);
        if (userPwd == null || !LoginChecker.isLoginOk(userPwd[0], userPwd[1])) {
            throw new ServletException("No permission!");
        }
        
        StageLogger.logDebug(this, "Project query incoming with user: " + userPwd[0]);

        try {
            File[] geopaparazziProjectFiles = GeopaparazziWorkspaceUtilities.getGeopaparazziProjectFiles(userPwd[0]);
            List<HashMap<String, String>> projectMetadataList = GeopaparazziWorkspaceUtilities
                    .readProjectMetadata(geopaparazziProjectFiles);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"projects\": [");

            for( int i = 0; i < projectMetadataList.size(); i++ ) {
                HashMap<String, String> metadataMap = projectMetadataList.get(i);
                long fileSize = geopaparazziProjectFiles[i].length();
                if (i > 0)
                    sb.append(",");
                sb.append("{");
                sb.append("    \"id\": \"" + geopaparazziProjectFiles[i].getName() + "\",");
                sb.append("    \"title\": \"" + metadataMap.get("description") + "\",");
                sb.append("    \"date\": \"" + metadataMap.get("creationts") + "\",");
                sb.append("    \"author\": \"" + metadataMap.get("creationuser") + "\",");
                sb.append("    \"name\": \"" + metadataMap.get("name") + "\",");
                sb.append("    \"size\": \"" + fileSize + "\"");
                sb.append("}");
            }

            sb.append("]");
            sb.append("}");

            ServletOutputStream out = resp.getOutputStream();
            out.print(sb.toString());
            out.flush();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}