/* Ring.java
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


/** Ring.java A collection of nodes that make up a ring */

public class Ring extends Vector{
    
    /** Vector to store the nodes of this ring.  Each node is bonded
        to its predecessor.  The ring is close by connecting the first
        and the last node */

    public int status = 0; // to hold one of the three stati below
    public final static int PLACEMENT_NONE = 0; // not yet placed
    public final static int PLACEMENT_PRELIM = 1; // just preliminarily placed
    public final static int PLACEMENT_FINAL = 2; // has reached its final place
    public double x; // x coordinate of ring center;
    public double y; // y coordinate of ring center;
    
    /** Creates a new Ring with no nodes stored */ 
    public Ring(){
        super(); 
    }
    
    /** Return a String with a sorted list of nodes numbers */
    public String getSortedString(){
        String s = "";
        int[] rep = new int[size()];
        for (int f = 0; f < size(); f++){
            rep[f] = ((Node)elementAt(f)).number;  
        }  
        quickSort(rep, 0, rep.length - 1);
        for (int f = 0; f < size()-1; f++){
            s += rep[f] + "-";  
        }  
        s += rep[size()-1];
        return s;
    }
    
    /** Returns a String representation of this ring, i.e.  just a
        list of node numbers that make up the ring.  Example:
        2-7-3-9-14 */
    public String getString(){
        String s = "";
        int[] rep = new int[size()];
        for (int f = 0; f < size(); f++){
            rep[f] = ((Node)elementAt(f)).number;  
        }  
        for (int f = 0; f < size()-1; f++){
            s += rep[f] + "-";  
        }  
        s += rep[size()-1];
        return s;
    }
    
    
    
    /* Quick Sort implementation
     */
    private void quickSort(int a[], int left, int right){
        int leftIndex = left;
        int rightIndex = right;
        float partionElement;
        if ( right > left)
            {
                /* Arbitrarily establishing partition element as the
                 * midpoint of the array.  */
                partionElement = a[ ( left + right ) / 2 ];
                // loop through the array until indices cross
                while( leftIndex <= rightIndex )
                    {
                        /* find the first element that is greater than
                         * or equal to the partionElement starting
                         * from the leftIndex.  */
                        while( ( leftIndex < right ) && ( a[leftIndex] < partionElement ) )
                            ++leftIndex;
                        /* find an element that is smaller than or
                         * equal to the partionElement starting from
                         * the rightIndex.  */
                        while( ( rightIndex > left ) &&
                               ( a[rightIndex] > partionElement ) )
                            --rightIndex;
                        // if the indexes have not crossed, swap
                        if( leftIndex <= rightIndex ){
                            swap(a, leftIndex, rightIndex);
                            ++leftIndex;
                            --rightIndex;
                        }
                    }
                /* If the right index has not reached the left side of
                 * array must now sort the left partition.  */
                if( left < rightIndex )
                    quickSort( a, left, rightIndex );
                /* If the left index has not reached the right side of
                 * array must now sort the right partition.  */
                if( leftIndex < right )
                    quickSort( a, leftIndex, right );
            }
    }
    
    private void swap(int a[], int i, int j){
        int T;
        T = a[i];
        a[i] = a[j];
        a[j] = T;
    }
    
    
    /** Returns a given Edge of this ring */ 
    public Edge getEdge(int number){
        int from, to;
        if (number < size() - 1){
            from = ((Node)elementAt(number)).number;
            to =  ((Node)elementAt(number + 1)).number;
            return new Edge(from, to);
        }
        else{
            from = ((Node)elementAt(0)).number;
            to =  ((Node)elementAt(size()-1)).number;
            return new Edge(from, to);
        }
    }
    
    /** Sorts the atoms of this ring such that the two atoms with the
        lowest indices are at the beginning of the list. Of course the
        order is preserved.  */
    public void sort(){
        // choose the number of an arbitrary atom 
        int lowestIndex = ((Node)elementAt(0)).number;
        int lowest = 0;
        int size = size();
        for (int f = 1; f < size; f++){
            if (((Node)elementAt(f)).number < lowestIndex){
                lowestIndex = ((Node)elementAt(f)).number;
                lowest = f;
            }
        }  
        if (lowest == size -1){
            shiftLeft(1);
            lowest --;
        }
        else if (lowest == 0){
            shiftRight(1);
            lowest ++;
        }
        /* if the number of the (lowest + 1)-st is smaller than the
           one of the (lowest - 1)-st then the order is correct and we
           just have to left-shift lowest times.  */
        if (((Node)elementAt(lowest + 1)).number < ((Node)elementAt(lowest - 1)).number){
            shiftLeft(lowest);
        }
        /* else if the number of the (lowest + 1)-st is greater than
           the one of the (lowest - 1)-st then we have to shift to the
           right (size()-1-lowest) times until the node with the
           smallest order is at the end of the list and then invert
           the list */
        else{
            shiftRight(size()-1-lowest);
            invert(); 
        }
    }
    
    /** Sorts the ring so that node with 'nodeNumber' is first */
    public void sort(int nodeNumber){
        int actNumber = ((Node)elementAt(0)).number;
        while(actNumber != nodeNumber){
            shiftLeft(1);
            actNumber = ((Node)elementAt(0)).number;
            
        }
    }
    
    /** shifts the node list of this ring to the left */
    public void shiftLeft(int times){
        for (int f = 0; f < times; f++){
            Node n = (Node)elementAt(0);
            removeElementAt(0);
            addElement(n);
        }
    }
    
    /** shifts the node list of this ring to the right */
    public void shiftRight(int times){
        for (int f = 0; f < times; f++){
            Node n = (Node)elementAt(size()-1);
            removeElementAt(size()-1);
            insertElementAt(n, 0);
        }
    }
    
    /** Inverts the order of Nodes in this ring */
    public void invert(){
        int oldsize = size();
        for(int f = 0; f < oldsize; f++){
            insertElementAt((Node)elementAt(size() - 1 -f), f);
        }
        setSize(oldsize);  
    }
    
    /** returns the sum of the ringCounter values of all the nodes in
        this ring */
    public int getComplexity(Node[] atomSet){
        int compl = 0;
        for (int f = 0; f < size(); f++){
            compl += atomSet[((Node)elementAt(f)).number].ringCounter;  
        } 
        return compl;
        
    }
    
    /** Checks if all Nodes of this ring have not yet been finally
        positioned and returns 'true' if so. */
    public boolean notAllPositioned(Node[] atomSet){
        int test;
        for (int f = 0; f < size(); f++){
            //test = ((Node)elementAt(f)).number;
            if (atomSet[((Node)elementAt(f)).number].status < 1) return true;  
        } 
        return false;
    }
    
    /** returns a vector listing the numbers of the nodes involved in
        this ring */
    public Vector getNodeNumberList(){
        Vector vec = new Vector();
        for (int f = 0; f < size(); f++){
            vec.addElement(new Integer(((Node)elementAt(f)).number));
        } 
        return vec;
    }
    
    /** Returns the radius of this ring assuming that is has been
        drawn as a regular polygon. The radius is defined as the
        distance between the ring center and one of the nodes. */
    public double getRadius(double bondLength){
        double radius = Math.sqrt(Math.pow(bondLength, (double)2)/(2 - (2 * Math.cos(getSectorAngle()))));
        return radius;
    }
    
    /** Returns the angle that is made of by a 'node', the ring center
        and a direct neighbor of 'node'. */
    public double getSectorAngle(){
        double gamma = 360/size(); 
        gamma = Math.PI/180*gamma;
        return gamma;
    }
    
    /** Returns the number (the location in 'atomSet') of the node
        which is positioned at postition 'thisNode' in this Vector */
    public int getNodeNumber(int thisNode){
        return ((Node)elementAt(thisNode)).number; 
    }
}
