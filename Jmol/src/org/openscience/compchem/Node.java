/* Node.java
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

import java.io.*;

/** Node.java 
    An atom in the chemical graph represented by number,
    chemical symbol and some other attributes 
*/
 
public class Node implements Serializable{
      
    /** The name of this node. This should be something
        that relates to the element symbol of the node, like C-1*/
    public String name;
    
    public String symbol = "";
    public int number;
    public int status = 0; // to hold one of the three stati below
    public final static int PLACEMENT_NONE = 0; // not yet placed
    public final static int PLACEMENT_PRELIM = 1; // just preliminarily placed
    public final static int PLACEMENT_FINAL = 2; // has reached its final place
    /** x coordinate of this node */
    public double x; 
    /** y coordinate of this node */ 
    public double y;
    /** z coordinate of this node */   
    public double z;
 
    /** list of adjacent nodes */
    public int[] nodeTable = new int[6];
    /** list of the bondorders of the bonds listed in nodeTable */
    public int[] bondTable = new int[6];
    /** degree of this node, i.e. the number of attached nodes */
    public int degree = 0;
    /** source is needed for Figueras' (SSSR) ring perception algorithm
        and denotes the  */
    public int source; 
    /** Possiblity to indicate the number of implicit hydrogen atoms 
        attached to this node. This is used to check is the sum of bond orders
        of all bonds of this node to other heavy atoms plus the sum of implicit
        hydrogens euqals maxBondOrderSum */
    public int HCount = 0;
    /** Maximum allowed sum of bond orders of all bonds of this node. 
        This should for example be set to 4 (four) for carbon atoms */
    public int maxBondOrderSum;
    /** Maximum allow BondOrder for this node. Would e.g. be 2 for oxygens */
    public int maxBondOrder;
    
    /** current sum of bondorders for this node.  */
    public int bondOrderSum;
    
    /** to store the numbers of the rings of which this node is part of.
        BTW, what is the maximum number of Rings that an atom with, say, 
        octahedral configuration can be part of? Ten is certainly too much. */ 
    public int[] inRings = new int[10];
    
    public int ringCounter;
    
    /** Constructs an empty Node */ 
    public Node(){
        bondOrderSum = getBondCount();
    }
    
    /** Constructs a Node with a given element symbol*/ 
    public Node(String s){
        this();
        this.symbol = s;  
    }
    
    /** Constructs a Node with a given element Symbol and node number */
    public Node(String s, int n){
        this();
        this.symbol = s;
        this.number = n;  
    }
    
    /** Constructs a Node with a given element Symbol and node number */
    public Node(String s, double x, double y, double z){
        this();
        this.symbol = s;
        this.x = x;        
        this.y = y;
    }

    
    /** Constructs a Node with a given node number */
    
    public Node(int n){
        this();
        this.number = n; 
    }
    
    /** Returns  */ 
    public Object clone(){
        Node n = new Node();
        n.name = name;
        n.symbol = symbol;
        n.number = number;
        n.status = status;
        n.degree = degree;
        n.source = source;
        n.x = x;
        n.y = y;
        n.z = z;
        n.ringCounter = ringCounter;
        n.HCount = HCount;
        n.maxBondOrderSum = maxBondOrderSum;
        n.maxBondOrder = maxBondOrder;
        n.bondOrderSum = bondOrderSum;
        System.arraycopy(nodeTable, 0, n.nodeTable, 0, 6);
        System.arraycopy(bondTable, 0, n.bondTable, 0, 6);
        System.arraycopy(inRings, 0, n.inRings, 0, 10);
        return n;
    }
    
    /** Prints a list of rings of which this node is part of 
        to the system console */
    public void reportRings(){
        String s = "[ ";
        for (int f = 0; f < ringCounter; f ++){
            s += inRings[f] + " "; 
        }
        s += "]";
        System.out.println(s);
    }
    
    /** Returns the sum of all bond orders of all bonds of this Node */
    public int getBondCount(){
        int count = 0;
        for (int f = 0; f < degree; f++){
            count += bondTable[f]; 
        } 
        return count;
    }
    
    /** maxBondOrderSum has to be set if this method is to be
        used. The value of HCount is set to the difference between
        maxBondOrderSum and the result of getBondCount() 
    */
    public void saturate(){
        HCount = maxBondOrderSum - getBondCount();
    }
    
    public boolean isSaturated(){
        if (getBondCount() + HCount == maxBondOrderSum) return true;
        else return false;
    }
    
    
    public boolean isBondedTo(int nodeNumber){
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == nodeNumber) return true;
        } 
        return false; 
    }
    
    public int getBondOrder(int nodeNumber){
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == nodeNumber) return bondTable[f];
        } 
        return 0; 
    }
    
    public void incrementBondOrder(int nodeNumber)
    {
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == nodeNumber) bondTable[f] += 1;
            return;
        }    
            
    }
    
    public void decrementBondOrder(int nodeNumber) {
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == nodeNumber && bondTable[f] > 0) bondTable[f] -= 1;
            return;
        }    
        
    }
    
    public double getX()
    {
        return x;    
    }
    
    public double getY()
    {
        return y;    
    }
    
    
    
    /** Returns the largest currently formable bondorder for this node  */
    public int getCurrentMaxBondOrder(){
        return Math.min(maxBondOrder, maxBondOrderSum - HCount - bondOrderSum);
    } 
    
    /** Forms a bond between this node and targetNode with order
        'bondOrder'.  No checking is performed of whether this exceeds
        the bondOrderSum limit of this node 
    */
    public void formBond(int targetNode, int bondOrder){
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == targetNode){
                bondTable[f] = bondOrder % 4; 
                bondOrderSum = getBondCount();
                return;
            }
        }
        
        nodeTable[degree] = targetNode;
        bondTable[degree] = bondOrder;
        degree++;
        bondOrderSum += bondOrder;
    }
    
    /** Removes a bond between this node and targetNode. */
    public void removeBond(int targetNode){
        for (int f = 0; f < degree; f++){
            if (nodeTable[f] == targetNode){
                bondOrderSum -= bondTable[f];
                for (int g = f; g < degree - 1; g++){
                    nodeTable[g] = nodeTable[g + 1];
                    bondTable[g] = bondTable[g + 1];     
                }
                degree--;
                break;
            } 
        }
    }
 
    /** Resets the Node to have no bonds with other nodes */
    public void reset(){
        degree = 0;
        bondOrderSum = 0;
    }
    
    
    public String toString(){
        String out;
        out = "Number: " + number;
        out += "; Nodename: " + name;
        out += "; Symbol: " + symbol;
        out += "; Degree: " + degree;
        out += "; HCount: " + HCount + "\n";
        out += "maxBondOrder: " +maxBondOrder ;
        out += "; maxBondOrderSum: " + maxBondOrderSum;
        out += "; bondOrderSum: " + bondOrderSum + "\n";
        out += "x: " + x ;
        out += "; y: " + y;
        out += "; z: " + z + "\n";
        
        out += "NodeTable: ";
        for (int f = 0; f < degree; f++) out += nodeTable[f] + " ";
        out += "\n";
        out += "BondTable: ";
        for (int f = 0; f < degree; f++) out += bondTable[f] + " ";
        return out;   
    }
    
}


 
