/*
 * @(#)RasMolScriptHandler.java    1.0 2000/11/30
 *
 * Copyright (c) 2000  Egon L. Willighagen All Rights Reserved.
 *
 *  Suggested by Henry Rzepa. This handler will deal with both
 *  typed commando's and command line scripts:
 *
 *     jmol -script <rasmol.script>
 *
 *  This code is GPL.
 * 
 **/
package org.openscience.jmol;

import java.util.StringTokenizer;
import java.io.File;

class RasMolScriptHandler {

    private Jmol program;

    RasMolScriptHandler (Jmol program) {
        this.program = program;
    }

    public void handle(String command) throws RasMolScriptException {
        StringTokenizer st = new StringTokenizer(command);
        if (!st.hasMoreElements()) {
            throw new RasMolScriptException("No command is given.");   
	} else {
            String word = (String)st.nextElement();
            if (word.equals("load")) {
                if (st.hasMoreElements()) {
                    String param = (String)st.nextElement();
                    String file;
                    if (st.hasMoreElements()) {
			file = (String)st.nextElement();
		    } else {
                        param = "xyz";
			file = param;
		    }
                    program.openFile(new File(file), param);
   	        } else {
		    throw new RasMolScriptException("Error: omitted parameter.");
		}
	    } else {
		throw new RasMolScriptException("Unrecognized command: " + command);
	    }
	}
    }
}

