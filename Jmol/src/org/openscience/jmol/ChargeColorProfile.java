/*
 * Copyright 2002 The Jmol Development Team
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.awt.Color;

/**
 * Color atoms by charge. Negatively charged atoms are colored red,
 * positively charged atoms are blue, and Neutral atoms are white.
 */
public class ChargeColorProfile implements ColorProfile {

    /**
     * @param   a       Atom for which the color must be returned
     * @return          Color for charged atom
     */
    public Color getColor(Atom a) {
        Charge ct = (Charge) a.getProperty(Charge.DESCRIPTION);
        Color c = Color.white;
        if (ct != null) {
            double charge = ((Double)ct.getProperty()).doubleValue();
            if (charge > 0.0) {
                if (charge < 1.0) {
                    int index = 255 - (int)(charge*255.0);
                    c = new Color(index, index, 255);
                } else {
                    c = Color.blue;
                }
            } else if (charge < 0.0) {
                if (charge > -1.0) {
                    int index = 255 + (int)(charge*255.0);
                    c = new Color(255, index, index);
                } else {
                    c = Color.red;
                }
            }
        }
        return c;
    };

}
