/* MFAnalyser.java
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

/** MFAnalyser.java
 * 
 * Analyses a molecular formula given in String format and builds set
 * of org.openscience.compchem.Node's reflecting the elements as given
 * by the molecular formula.  
 */
public class MFAnalyser{
 
    public String MF;
    public Node[] setOfNodes;
 
    /** Construct an instance of MFAnalyser, initialized with a molecular 
     * formula string. The string is immediatly analysed and a set of Nodes
     * is built based on this analysis
     */
    public MFAnalyser(String MF){
        this.MF = MF;
        this.setOfNodes = analyseMF(MF);
    } 

    /** returns the complete set of Nodes, as implied by the molecular
     * formula, inlcuding all the hydrogens.
     */
    public Node[] getSetOfNodes(){
        return setOfNodes;  
    }
  
    /** Returns a set of nodes excluding all the hydrogens*/
    public Node[] getSetOfHeavyNodes(){
        Node[] son = new Node[setOfNodes.length];
        Node[] rson;
        int counter = 0;
        for (int f = 0; f < setOfNodes.length; f++){
            if (!setOfNodes[f].symbol.equals("H")) son[counter++] = setOfNodes[f]; 
        } 
        rson = new Node[counter];
        System.arraycopy(son, 0, rson, 0, counter);
        return rson; 
    } 
 
    /** Method that actually does the work of analysing the molecular
        formula */
    private Node[] analyseMF(String MF){
        Vector vnodes  = new Vector();
        Node[] nodes;
        char ThisChar; /* Buffer for */
        String RecentElementSymbol = new String();
        String RecentElementCountString = new String("0"); /* String
                                                              to be
                                                              converted
                                                              to an
                                                              integer */
        int RecentElementCount;
        
        if (MF.length() == 0)
        return null;

        for (int f = 0; f < MF.length(); f ++){
            ThisChar = MF.charAt(f);
            if (f < MF.length()){
                if (ThisChar >= 'A' && ThisChar <= 'Z'){ /* New
                                                            Element
                                                            begins */
                    RecentElementSymbol = java.lang.String.valueOf(ThisChar);
                    RecentElementCountString = "0";
                }
                if (ThisChar >= 'a' && ThisChar<= 'z'){ /* Two-letter
                                                           Element
                                                           continued */
                    RecentElementSymbol += ThisChar;                
                }
                if (ThisChar >= '0' && ThisChar<= '9'){ /* Two-letter
                                                           Element
                                                           continued */
                    RecentElementCountString += ThisChar;                   
                }
            }
            if (f == MF.length() - 1 || (MF.charAt(f + 1) >= 'A' && MF.charAt(f + 1 ) <= 'Z')){
                /* Here an element symbol as well as its number should
                   have been read completely */
                Integer RecentElementCountInteger = new Integer(RecentElementCountString);
                RecentElementCount = RecentElementCountInteger.intValue();
                if (RecentElementCount == 0){
                    RecentElementCount = 1;
                }
                for (int g = 0; g < RecentElementCount; g++){
                    vnodes.addElement(new Node(RecentElementSymbol));
                }
            }
        }
        nodes = new Node[vnodes.size()];
        vnodes.copyInto(nodes);
        return nodes;
    }
}
