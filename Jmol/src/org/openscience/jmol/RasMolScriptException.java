/*
 * @(#)RasMolScriptException.java    1.0 98/08/27
 *
 * Copyright (c) 2000  Egon L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.jmol;

public class RasMolScriptException extends JmolException {

    public RasMolScriptException( String message ) {
        super("RasMolScriptHandler.handle()", message);
    }

}
