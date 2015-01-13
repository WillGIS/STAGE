/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package eu.hydrologis.rap.log;

/**
 * A simple logger, to be properly implemented.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class StageLogger {

    public static void logInfo( String msg ) {
        System.out.println(msg);
    }

    public static void logDebug( String msg ) {
        System.out.println(msg);
    }

    public static void logError( String msg, Exception e ) {
        System.err.println(msg);
        e.printStackTrace();
    }

}