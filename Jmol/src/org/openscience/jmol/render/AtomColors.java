/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 1997-2002  The Chemistry Development Kit (CDK) project
 *
 * Contact: steinbeck@ice.mpg.de, gezelter@maul.chem.nd.edu, egonw@sci.kun.nl
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openscience.jmol.render;

import java.awt.Color;

import java.util.Hashtable;

import org.openscience.cdk.Atom;
import org.openscience.cdk.renderer.color.AtomColorer;

import org.openscience.jmol.AtomType;
import org.openscience.jmol.BaseAtomType;


/**
 * <p>Singleton class defing the colors of the different atom types.
 * When initialized for the first time, all atoms have the color
 * dark grey. An instance is created with:
 * <pre>
 *   AtomColors colorer = AtomColors.getInstance();
 * </pre>
 */
public class AtomColors implements AtomColorer {

    private static AtomColors ac = null;

    private AtomColors() {}

    public static AtomColors getInstance() {
        if (ac == null) {
            ac = new AtomColors();
        }
        return ac;
    }

    /**
     * Returns the color for a certain atom type
     */
    public Color getAtomColor(Atom a) {
        Object o = a.getProperty("org.openscience.jmol.color");
        if (o == null) {
            // no color set. return white
            return Color.white;
        } else if (o instanceof Color) {
            return (Color)o;
        } else {
            // no color set. return white
            return Color.white;
        }
    }
}
