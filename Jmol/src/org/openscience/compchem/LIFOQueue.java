/* LIFOQueue.java
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

/** A LIFO queue for result structures. The Size is fixed so that one
    can use this to hold the 10 best structures, e. g. */

public class LIFOQueue extends java.util.Vector{
    public int size;
    
    public LIFOQueue(int size){
        super();
        this.size = size;
    }
    
    public void push(Object O){
        insertElementAt(O, 0);
        if (size() > size) setSize(size);
        // System.out.println("Object pushed. Current size " + size() + "; maxSize = " + size);     
    }
    
    public Object pop(){
        Object O = elementAt(0);
        removeElementAt(0);
        //System.out.println("Object popped. Current size " + size() + "; maxSize = " + size);     
        return O;
    }
}
