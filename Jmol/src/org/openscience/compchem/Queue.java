/* Queue.java
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

import java.util.Vector;

/** Implementation of a Queue (FIFO stack)  */
public class Queue extends Vector{

    /** Constructs an empty Queue */
    public Queue(){
        super();  
    } 
    
    /** Places an Object into the queue */
    public void push(Object o){
        addElement(o);
    }
    
    /** Returns an Object from the queue */
    public Object pop(){
        Object o = elementAt(0);
        removeElementAt(0); 
        return o;
    }
}
