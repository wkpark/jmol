/* IsomorphismTester.java
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


public class IsomorphismTester{

    int[] baseTable;  
    Node[] baseNodes;

    public IsomorphismTester(){
    }
 
   
    public IsomorphismTester(Node[] atomSet){
        this.baseNodes = atomSet;
        this.baseTable = computeMorganMatrix(baseNodes);
    }
    
    public int[] computeMorganMatrix(Node[] atomSet){
        int [] morganMatrix, tempMorganMatrix;
        int N = atomSet.length;
        morganMatrix = new int[N];
        tempMorganMatrix = new int[N];
        for (int f = 0; f < N; f++){
            morganMatrix[f] =  atomSet[f].getBondCount();
            tempMorganMatrix[f] =  atomSet[f].getBondCount();
        }
        for (int i = 0; i < N; i++){
            for (int f = 0; f < N; f++){
                for (int g = 0; g < atomSet[f].degree; g ++){
                    morganMatrix[f] += tempMorganMatrix[atomSet[f].nodeTable[g]]; 
                }
            }
            System.arraycopy(morganMatrix, 0, tempMorganMatrix, 0, N);
        }
        return morganMatrix; 
    }
 
    public boolean isIsomorph(Node[] atomSet1, Node[] atomSet2){
        boolean found;
        int[]  morganMatrix1 = computeMorganMatrix(atomSet1);
        int[]  morganMatrix2 = computeMorganMatrix(atomSet2);
        for (int f = 0; f < morganMatrix1.length; f++){
            found = false;
            for (int g = 0; g < morganMatrix2.length; g++){
                if (morganMatrix1[f] == morganMatrix2[g]){
                    if(!(atomSet1[f].symbol.equals(atomSet2[g].symbol) && atomSet1[f].HCount == atomSet2[g].HCount)) return false;
                    found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public boolean isIsomorph(Node[] atomSet2){
        boolean found;
        int[]  morganMatrix2 = computeMorganMatrix(atomSet2);
        for (int f = 0; f < baseTable.length; f++){
            found = false;
            for (int g = 0; g < morganMatrix2.length; g++){
                if (baseTable[f] == morganMatrix2[g]){
                    if(!(baseNodes[f].symbol.equals(atomSet2[g].symbol) && baseNodes[f].HCount == atomSet2[g].HCount)) return false;
                    found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }


}
