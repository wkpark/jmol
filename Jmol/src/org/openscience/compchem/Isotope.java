/* Isotope.java
 *
 * Copyright (C) 1997, 1998  Dr. Christoph Steinbeck 
 * 
 * Contact: steinbeck@ice.mpg.de
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.  All I ask is that
 * proper credit is given for my work, which includes - but is not
 * limited to - adding the above copyright notice to the beginning of
 * your source code files, and to any copyright notice that you may
 * distribute with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *  
 */

package org.openscience.compchem;


/** Isotope.java
 * Used to store data of a particular isotope
 */
 
public class Isotope extends Object implements Cloneable{
 
    public String symbol= "";
    public int atomicMass = -1 ;
    public double exactMass = (double)-1;
    public float naturalAbundance = (float)-1;
 
    public Object clone(){
        Isotope i = new Isotope();
        i.symbol = this.symbol;
        i.atomicMass = this.atomicMass;
        i.exactMass = this.exactMass;
        i.naturalAbundance = this.naturalAbundance;
        return i;
    }
 
    public String toString(){
        String s = "[" + atomicMass + "]";
        s += symbol + ": exact mass = " + exactMass;
        s += "; relative natural abundance = " + naturalAbundance;
        return s; 
    }
 
}
