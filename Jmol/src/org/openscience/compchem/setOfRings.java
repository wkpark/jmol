/* setOfRings.java
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

/** Implementation of a set of Rings.
    Maintains a Vector "rings" to store "ring" objects
    */
    
public class setOfRings extends Vector{
 
    public setOfRings(){
        super();
    }
    /** Checks - and returns 'true' - if a certain ring is 
        already stored in this setOfRings */
    public boolean ringAlreadyInSet(Ring r){
        Ring ring;
        String s1, s2;
        s1 = r.getSortedString();
        //  System.out.println(s1);
  
        //  System.out.println(this.size());
        for (int f = 0; f < this.size(); f++){
            ring = (Ring)this.elementAt(f);
            s2 = ring.getSortedString();
            if (s1.equals(s2)){
                return true;
            }
        }   
        return false;
    }

    /** Uses the private quicksort method of this class to sort the rings in the
        set by size */
    public void sort(){
        int left = 0;
        int right = size() - 1;
        quickSort(left, right); 
    }  

    /** Sorts the rings by size 
        Quick Sort implementation
    */
    private void quickSort(int left, int right){
        int leftIndex = left;
        int rightIndex = right;
        float partionElement;
        if ( right > left){  
            /* Arbitrarily establishing partition element as the midpoint of
             * the array.
             */
            partionElement = ((Ring)elementAt( ( left + right ) / 2 )).size();
            // loop through the array until indices cross
            while( leftIndex <= rightIndex ){
                /* find the first element that is greater than or equal to
                 * the partionElement starting from the leftIndex.
                 */
                while( ( leftIndex < right ) && (((Ring)elementAt(leftIndex)).size() < partionElement))
                    ++leftIndex;
                /* find an element that is smaller than or equal to
                 * the partionElement starting from the rightIndex.
                 */
                while( ( rightIndex > left ) && (((Ring)elementAt(rightIndex)).size() > partionElement ) )
                    --rightIndex;
                // if the indexes have not crossed, swap
                if( leftIndex <= rightIndex ){
                    swap(leftIndex, rightIndex);
                    ++leftIndex;
                    --rightIndex;
                }
            }
            /* If the right index has not reached the left side of array
             * must now sort the left partition.
             */
            if( left < rightIndex )
                quickSort(left, rightIndex );
            /* If the left index has not reached the right side of array
             * must now sort the right partition.
             */
            if( leftIndex < right )
                quickSort(leftIndex, right );
        }
    }
 

    /** swap for quicksort */
    private void swap(int i, int j){
        Ring r; 
        r = (Ring)elementAt(i);
        setElementAt( elementAt(j), i);
        setElementAt(r, j);
    }
 
    /** Lists the rings in this set to the console */
    public void reportRingList(){
        for (int f = 0; f < size(); f++){
            System.out.println(((Ring)elementAt(f)).getString()); 
        }
    }
 
    /** For each node in each ring of the list, make a list of rings 
        that it is part of. */
  
    public void makeNodeRingLists(Node[] thisAtomSet){
        Ring ring;
        int nodeNo;
        for (int f = 0; f < size(); f++){
            ring = (Ring)elementAt(f); 
            for (int g = 0; g < ring.size(); g++){
                nodeNo = ((Node)ring.elementAt(g)).number;
                thisAtomSet[nodeNo].inRings[thisAtomSet[nodeNo].ringCounter] = f; 
                thisAtomSet[nodeNo].ringCounter ++;   
            }
        } 
    } 
}    
