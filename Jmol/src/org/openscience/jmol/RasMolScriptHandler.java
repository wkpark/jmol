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

import java.util.StringTokenizer;

package org.openscience.jmol;

class RasMolScriptHandler {

    private Jmol program;

    RasMolScriptHandler (Jmol program) {
        this.program = program;
    }

    public handle(String command) throws RasMolScriptException {
        StringTokenizer st = new StringTokenizer(command);
        if (!st.hasMoreElements()) {
            throw new RasMolScriptException("No command is given.");   
	} else {
            String command = st.nextElement();
            if (command.equals("load")) {
                if (st.hasMoreElements()) {
                    String param = st.nextElement();
                    String file;
                    if (st.hasMoreElements()) {
			file = st.nextElement();
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

