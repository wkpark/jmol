/*
 * @(#)RasMolScriptHandler.java    1.0 2000/11/30
 *
 * Copyright (c) 2000  Egon L. Willighagen All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 **/
package org.openscience.jmol;

import java.util.StringTokenizer;
import java.util.Enumeration;
import java.io.File;
import java.awt.Color;
import javax.swing.JTextArea;

/**
 *  Suggested by Henry Rzepa. This handler will deal with both
 *  typed commando's and command line scripts:
 *
 *     jmol -script <rasmol.script>
 *
**/
class RasMolScriptHandler {

    private Jmol program;
    private JTextArea output;

    RasMolScriptHandler (Jmol program) {
        this.program = program;
	this.output = null;
    }

    RasMolScriptHandler (Jmol program, JTextArea output) {
        this.program = program;
	this.output = output;
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
			file = param;
                        param = "CML";
		    }
                    program.openFile(new File(file), param);
   	        } else {
		    throw new RasMolScriptException("Error: omitted parameter.");
		}
            } else if (word.equals("colour") || word.equals("color")) {
                if (st.hasMoreElements()) {
                    String param = (String)st.nextElement();
                    String color;
                    if (st.hasMoreElements()) {
			color = (String)st.nextElement();
		    } else {
			color = param;
                        param = "atom";
		    }
                    setColor(param, color);
   	        } else {
		    throw new RasMolScriptException("Error: omitted colour.");
		}
	    } else if (word.equals("echo")) {
                while (st.hasMoreElements()) {
                    print((String)st.nextElement() + " ");
		    println();
		}
	    } else if (word.equals("set")) {
                if (st.hasMoreElements()) {
		    String param = (String)st.nextElement();
                    if (st.hasMoreElements()) {
			String value = (String)st.nextElement();
                        setParam(param, value);
		    } else {
			throw new RasMolScriptException("Error: omitted value.");
		    }
   	        } else {
		    throw new RasMolScriptException("Error: omitted parameter.");
		}
	    } else if (word.equals("refresh")) {
                program.display.repaint();
	    } else if (word.equals("background")) {
                if (st.hasMoreElements()) {
		    setBackgroundColor((String)st.nextElement());
   	        } else {
		    throw new RasMolScriptException("Error: omitted color.");
		}
	    } else if (word.equals("select")) {
                if (st.hasMoreElements()) {
		    select((String)st.nextElement());
   	        } else {
		    throw new RasMolScriptException("Error: omitted expression.");
		}
	    } else if (word.equals("list")) {
                if (st.hasMoreElements()) {
		    list((String)st.nextElement());
   	        } else {
		    throw new RasMolScriptException("Error: omitted object type.");
		}
	    } else if (word.equals("exit")) {
                if (output != null) {
		    // script command is run from script window
		    program.scriptWindow.hide();
		} else {
		    // script is run from command line
		}
	    } else if (word.equals("clear")) {
                if (output != null) {
		    // script command is run from script window
		    output.setText();
		} else {
		    // script is run from command line
		}
	    } else {
		throw new RasMolScriptException("Unrecognized command: " + command);
	    }
	}
    }
 
    private void setParam(String param, String value) throws RasMolScriptException {
	if (param.equals("shadows")) {
            boolean val = checkBoolean(value);
            if (val) {
		// turn shading on
                program.settings.setAtomDrawMode(DisplaySettings.SHADING);
                program.settings.setBondDrawMode(DisplaySettings.SHADING);
	    } else {
		// turn shading off
                program.settings.setAtomDrawMode(DisplaySettings.QUICKDRAW);
                program.settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
	    }
	} else {
	    throw new RasMolScriptException("Unrecognized parameter: " + param);
	}
    }

    private void list(String objectType) throws RasMolScriptException {
        if (objectType.equals("atoms")) {
	    for (int i=0; i<program.display.md.atoms.length; i++) {
                StringBuffer sb = new StringBuffer();
                sb.append("  ");
		sb.append(i + 1);
		sb.append(" ");
		sb.append(program.display.md.atoms[i].getBaseAtomType().getRoot());
		println(sb.toString());
	    }
	} else {
	    throw new RasMolScriptException("Unrecognized object type: " + objectType);
	}
    }

    private void select(String value) throws RasMolScriptException {
        if (value.equals("all") || value.equals("*")) {
            program.display.md.selectAll();            
	} else if (value.equals("none")) {
            program.display.md.deselectAll();
        } else if (value.indexOf(',') != -1) {
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreElements()) {
                String subexpr = (String)st.nextElement();
		try {
                    int atom = Integer.parseInt(subexpr);
                    program.display.md.selectAtomByNumber(atom);
		} catch (NumberFormatException e) {
		    throw new RasMolScriptException("Error: invalid expression: " + subexpr);
		}
	    }        
	} else {
	    try {
		int atom = Integer.parseInt(value);
		program.display.md.selectAtomByNumber(atom);
	    } catch (NumberFormatException e) {
		throw new RasMolScriptException("Error: invalid expression: " + value);
	    }
	}
    }

    private Color getColor(String value) throws RasMolScriptException {
        if (value.equals("red")) {
            return Color.red;
	} else if (value.equals("white")) {
            return Color.white;
	} else if (value.equals("black")) {
            return Color.black;
	} else if (value.equals("grey")) {
            return Color.gray;
	} else if (value.equals("blue")) {
            return Color.blue;
	} else if (value.equals("green")) {
            return Color.green;
	} else if (value.equals("cyan")) {
            return Color.cyan;
	} else if (value.equals("magenta")) {
            return Color.magenta;
	} else if (value.equals("orange")) {
            return Color.orange;
	} else if (value.equals("pink")) {
            return Color.pink;
	} else if (value.equals("yellow")) {
            return Color.yellow;
	} else {
	    throw new RasMolScriptException("Unknown color: " + value);
	} 
    } 

    private void setColor(String object, String value) throws RasMolScriptException {
        if (object.equals("atom")) {
	    // give selected atoms new colour
	    Enumeration selectedAtoms = program.display.md.getSelectedAtoms().elements();
	    while (selectedAtoms.hasMoreElements()) {
                int atom = ((Integer)selectedAtoms.nextElement()).intValue();
                println("atom " + atom + " selected.");
		program.display.md.atoms[atom-1].getBaseAtomType().setColor(this.getColor(value));
	    }
        } else {
            throw new RasMolScriptException("Error: unknown object: " + object);
        }
    }

    private void setBackgroundColor(String value) throws RasMolScriptException {
        program.display.setBackgroundColor(this.getColor(value));
    }

    private boolean checkBoolean(String value) throws RasMolScriptException {
        if (value.equals("true")) {
	    return true;
	} else if (value.equals("false")) {
	    return false;
	} else {
            throw new RasMolScriptException("Unrecognized boolean value: " + value);
	}
    }

    private void print(String s) {
	if (output != null) {
	    output.append(s);
	} else {
	    System.out.print(s);
	}
    }

    private void println(String s) {
        this.print(s);
        this.println();
    }

    private void println() {
        this.print("\n");
    }
}


