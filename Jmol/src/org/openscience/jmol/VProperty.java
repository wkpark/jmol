
/*
 * Copyright 2001 The Jmol Development Team
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

import java.util.*;

/**
 * A class to store the VProperty (an arbitrary 3-vector) for an atom
 * (could be velocity, direction in a Normal mode, point dipole,
 * etc.).  
 */
public class VProperty extends PhysicalProperty {
    double[] property = new double[3];

    /**
     * Constructor for VProperty
     * @param v The 3-vector containing the VProperty
     */
    public VProperty(double v[]) {
        super("Vector", v);
        property[0] = v[0];
        property[1] = v[1];
        property[2] = v[2];
    }

    /** 
     * returns the Vector
     */
    public double[] getVector() {
        return property;
    }

}
