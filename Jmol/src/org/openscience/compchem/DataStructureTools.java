/* DataStructureTools.java
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

public class DataStructureTools{
 
    public static int INFINITY = 1000000;
 
    /** Analyses a set of Nodes that has been changed or recently loaded
        and  returns a molecular formula  */
    public static String analyseSetOfNodes(Node[] setOfNodes){
        String symbol, mf = "";
        String[] symbols;
        int[] elementCount;
        int HCount = 0;
        int numberOfElements = 0;
        boolean done;
        symbols = new String[setOfNodes.length];
        elementCount = new int[setOfNodes.length];
        for (int f = 0; f < setOfNodes.length; f++){
            symbol = setOfNodes[f].symbol; 
            HCount += setOfNodes[f].HCount;  
            done = false;
            for (int g = 0; g < numberOfElements ; g++){
                if (symbols[g].equals(symbol)){
                    elementCount[g]++;
                    done = true;
                    break; 
                } 
            }
            if (!done){
                symbols[numberOfElements] = symbol;
                elementCount[numberOfElements]++;
                numberOfElements++;
            } 
        }
        for (int g = 0; g < numberOfElements ; g++){
            mf += symbols[g];
            if (elementCount[g] > 1) mf += new Integer(elementCount[g]).toString();
            if (g == 0 && HCount > 0){
                mf += "H";
                if (HCount > 1) mf += new Integer(HCount).toString();
            } 

        }
        return mf;
    }
 
    /** Lists the connection table extracted from the node and bond list of
     * this set of nodes to the System console */
    public static void reportConnectionTable(Node[] setOfNodes){
      
        int contab[][] = createConnectionTable(setOfNodes);
        String line;
        for (int f = 0; f < contab.length; f++){
            line  = setOfNodes[f].symbol + ": ";
            for (int g = 0; g < contab.length; g++){
                line += contab[f][g] + " ";
            }
            System.out.println(line);
        }

    }

    /** Lists a 2D array of int values to the System console */
    public static void printInt2D(int[][] contab){
        String line;
        for (int f = 0; f < contab.length; f++){
            line  = "";
            for (int g = 0; g < contab.length; g++){
                line += contab[f][g] + " ";
            }
            System.out.println(line);
        }
    }
 
    /** Constructs the connection table from the node and bond list of
     * this set of nodes */
    public static int[][] createConnectionTable(Node[] setOfNodes){
        int partner;
        int contab[][] = new int[setOfNodes.length][setOfNodes.length];
        for (int f = 0; f < setOfNodes.length; f++){
            for (int g = 0; g < setOfNodes.length; g++){
                contab[f][g]=0;
            }
        }
        for (int f = 0; f < setOfNodes.length; f++){
            for (int g = 0; g < setOfNodes[f].degree; g++){
                partner = setOfNodes[f].nodeTable[g];
                contab[f][partner] = setOfNodes[f].bondTable[g];
                contab[partner][f] = setOfNodes[f].bondTable[g];
            }
        }
        return contab;
    }
 
    /** Returns an integer matrix containing the shortest paths of all pairs 
        of nodes in setOfNodes */
    public static int[][] getAllPairsShortestPaths(Node[] setOfNodes){
        int[][] contab = createConnectionTable(setOfNodes);
        int[][] allPairsOfShortestPaths = getAllPairsShortestPaths(contab);
        return allPairsOfShortestPaths;
    }
 
    /** Returns an integer matrix containing the shortest paths of all pairs 
        of nodes in setOfNodes */
    public static int[][] getAllPairsShortestPaths(int[][] ct){
        int N = ct.length;
        int[][] contab = new int[N][N];
        for (int i=0; i<N; i++){
            for (int j=0; j<N; j++){
                contab[i][j]=ct[i][j];
            }
        }
        synchronizeConnectionTable(contab);
        int[][] edges = computeEdges(contab);
        int[][][] pathLengths = initPaths(edges);
        int[][] allPairsOfShortestPaths = new int[N][N];
        for (int k=1; k<=N; k++){
            for (int i=0; i<N; i++){
                for (int j=0; j<N; j++){
                    pathLengths[k][i][j] = Math.min(pathLengths[k-1][i][j], pathLengths[k-1][i][k-1] + pathLengths[k-1][k-1][j]);
                }
            }
        }
        for (int i=0; i<N; i++){
            for (int j=0; j<N; j++){
                if (pathLengths[N][i][j] < INFINITY) allPairsOfShortestPaths[i][j] = pathLengths[N][i][j];
                else allPairsOfShortestPaths[i][j] = 0;
            }
        }
        return allPairsOfShortestPaths;
    }

    /** Helper method for getAllPairsShortestPaths. Initializes */
    private static int[][][] initPaths(int[][] edges){
        int N = edges.length;
        int [][][] pathLengths = new int[N + 1][N][N];  
        for (int i=0; i<N; i++){
            for (int j=0; j<N; j++){
                pathLengths[0][i][j] = edges[i][j];
            }
        }
        return pathLengths;
    }
 
    /** Synchronizes the lower half of a connection table according to
        the values in the upper half*/
    public static void synchronizeConnectionTable(int[][] contab){
        int N = contab.length;
        for (int i=0; i < N; i++){
            for (int j=i + 1; j < N; j++){
                contab[j][i] = contab[i][j];
            }
        }
    }
   
    /** returns a duplicate of a symmetrical 2D array of integers. One
        could probably use System.arraycopy in the inner loop */
    public static int[][] cloneInt2D(int[][] int2D){
        int N = int2D.length;
        int[][] newInt2D = new int[N][N];
        for (int i=0; i < N; i++){
            for (int j=0; j < N; j++){
                newInt2D[i][j] = int2D[i][j];
            }
        }
        return newInt2D;
    }

    /** Remove the bond order values from the connection table and
     * make the weight of all edges equal to one */
    public static int[][] computeEdges(int[][] contab){
        int N = contab.length;
        int [][] edges = new int[N][N];
        for (int i=0; i < N; i++){
            for (int j=0; j < N; j++){
                if (contab[i][j] > 0){
                    edges[i][j] = 1;
                }
                else{
                    edges[i][j] = INFINITY;
                }
            }
        }
        return edges;
    }
 
    public static Node[] cloneAtomSet(Node[] thisSet){
        Node[] newAtomSet = new Node[thisSet.length];
        for (int f = 0; f < thisSet.length; f++){
            newAtomSet[f] = (Node)thisSet[f].clone(); 
        }
        return newAtomSet;   
    }
 
    /** Copies the information stored in the connection table back
        into the nodeTable and bondTable arrays of the set of Nodes */
    public static synchronized void synchronizeBonds(Node[] as, int[][] int2D){
        if (int2D.length != as.length){
            System.err.println("Error in DataStructureTools->synchronizeBonds: incompatible array sizes!");
            return;
        }
        for (int f = 0; f < as.length; f++){
            as[f].degree = 0;
        }  
        for (int f = 0; f < as.length - 1; f++){
            for (int g = f + 1; g < as.length; g++){
                if (int2D[f][g] > 0){
                    as[f].nodeTable[as[f].degree] = g;
                    as[f].bondTable[as[f].degree] = int2D[f][g];
                    as[g].nodeTable[as[g].degree] = f;
                    as[g].bondTable[as[g].degree] = int2D[f][g];          
                    as[f].degree ++;
                    as[g].degree ++;
                }
            }   
        }  
    }

    public static void removeIsomorphism(Vector structures){
        boolean removed = false;
        Node[] n1;
        Node[] n2;
        IsomorphismTester isoTest;
        int s = 0;
        do
            {
                removed = false;
                s = structures.size();
                try
                    {
                        n1 = (Node[])structures.elementAt(--s);
                    }
                catch(Exception e)
                    {
                        removed = true;
                    }
            }while(removed);
        structures.setSize(s + 1);
        int i = 0, j = 0;
        do
            {  
                removed = false;
                isoTest = new IsomorphismTester();
                n1 = (Node[])structures.elementAt(i);
                s = structures.size();
                j = 0;
                do{ 
                    if (i != j){
                        if (isoTest.isIsomorph(n1, (Node[])structures.elementAt(j))){
                            structures.removeElementAt(j);
                            removed = true;
                        }
                    }
                    j++;
                }while(structures.size() > 0 && j < structures.size());
                i++;
            }while(structures.size() > 0 && i < structures.size());
    }
}
