/* StandardElements.java
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

import java.util.*;
import java.io.*;

/** StandardsIsotopes.java
 * Used to store data of a particular isotope
 */
 
public class StandardElements{
    private Element[] elements = {
        new Element("H", 1, 1),
        new Element("C", 3, 4),
        new Element("Si", 3, 4),
        new Element("N", 3, 3),
        new Element("P", 3, 3),
        new Element("O", 2, 2),
        new Element("S", 2, 2), 
        new Element("F", 1, 1),
        new Element("Cl", 1, 1),
        new Element("Br", 1, 1),
        new Element("I", 1, 1)};
 
    public int getSize(){
        return elements.length; 
    }
  
    public int getMaxBondOrder(String elementSymbol){
        Element e = getElement(elementSymbol);
        if (e != null) return e.maxBondOrder;
        else return -1;
    }  
  
    public int getMaxBondOrderSum(String elementSymbol){
        Element e = getElement(elementSymbol);
        if (e != null) return e.maxBondOrderSum;
        else return -1;
    }  
  
    public Element elementAt(int pos){
        if (pos >= 0 && pos < elements.length) return (Element)elements[pos].clone();
        return null;
    }

  
    private void enlargeElementArray(){
        int newSize = (int)(elements.length * 1.1);
        Element[] newElements = new Element[newSize];
        System.arraycopy(elements, 0, newElements, 0, elements.length); 
        elements = newElements;
    }
  
    private Element getElement(String elementSymbol){
        for (int f = 0; f < elements.length; f++){
            if (elements[f].symbol.equals(elementSymbol)) return elements[f];
        }
        return null; 
    }
}
