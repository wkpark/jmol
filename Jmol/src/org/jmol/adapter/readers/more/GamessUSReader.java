/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.io.BufferedReader;
import java.util.Hashtable;

public class GamessUSReader extends GamessReader {

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("gamess");
    try {
      readLine();
      boolean iHaveAtoms = false;
      while (line != null) {
        if (line.indexOf("COORDINATES (BOHR)") >= 0 || line.indexOf("COORDINATES OF ALL ATOMS ARE (ANGS)") >= 0) {
          if (++modelNumber != desiredModelNumber && desiredModelNumber > 0) {
            if (iHaveAtoms)
              break;
            readLine();
            continue;
          }
          if (line.indexOf("COORDINATES (BOHR)") >= 0)
            readAtomsInBohrCoordinates();
          else
            readAtomsInAngstromCoordinates();
          iHaveAtoms = true;
        } else if (iHaveAtoms && line.indexOf("FREQUENCIES IN CM") >= 0) {
          readFrequencies();
        } else if (iHaveAtoms && line.indexOf("ATOMIC BASIS SET") >= 0) {
          readGaussianBasis("SHELL TYPE", "TOTAL");
          continue;
        } else if (iHaveAtoms && (line.indexOf("  EIGENVECTORS") >= 0  
            || line.indexOf("  MOLECULAR ORBITALS") >= 0)) {
          readMolecularOrbitals();
          continue;
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }
  
  protected void readAtomsInBohrCoordinates() throws Exception {
/*
 ATOM      ATOMIC                      COORDINATES (BOHR)
           CHARGE         X                   Y                   Z
 C           6.0     3.9770911639       -2.7036584676       -0.3453920672

0         1         2         3         4         5         6         7    
01234567890123456789012345678901234567890123456789012345678901234567890123456789

*/    

    readLine(); // discard one line
    String atomName;
    atomSetCollection.newAtomSet();
    int n = 0;
    while (readLine() != null
        && (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 17, 37);
      float y = parseFloat(line, 37, 57);
      float z = parseFloat(line, 57, 77);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName + (++n);
      atom.set(x, y, z);
      atom.scale(ANGSTROMS_PER_BOHR);
      atomNames.addElement(atomName);
    }
  }

  private void readAtomsInAngstromCoordinates() throws Exception {
    readLine(); 
    readLine(); // discard two lines
    String atomName;
    atomSetCollection.newAtomSet();
/*    
       COORDINATES OF ALL ATOMS ARE (ANGS)
   ATOM   CHARGE       X              Y              Z
 ------------------------------------------------------------
 C           6.0   2.1045861621  -1.4307145508  -0.1827736240

0         1         2         3         4         5         6    
0123456789012345678901234567890123456789012345678901234567890

*/
    int n = 0;
    while (readLine() != null
        && (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 16, 31);
      float y = parseFloat(line, 31, 46);
      float z = parseFloat(line, 46, 61);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName + (++n);
      atom.set(x, y, z);
      atomNames.addElement(atomName);
    }
  }
  /*
   * 
   ATOMIC BASIS SET
   ----------------
   THE CONTRACTED PRIMITIVE FUNCTIONS HAVE BEEN UNNORMALIZED
   THE CONTRACTED BASIS FUNCTIONS ARE NOW NORMALIZED TO UNITY

   SHELL TYPE PRIMITIVE    EXPONENT          CONTRACTION COEFFICIENTS

   C         


   1   S    1           172.2560000       .061766907377
   1   S    2            25.9109000       .358794042852
   1   S    3             5.5333500       .700713083689

   2   L    4             3.6649800      -.395895162119       .236459946619
   2   L    5              .7705450      1.215834355681       .860618805716

   OR:

   SHELL TYPE PRIM    EXPONENT          CONTRACTION COEFFICIENTS

   C         

   1   S    1      71.616837    2.707814 (  0.154329) 
   1   S    2      13.045096    2.618880 (  0.535328) 
   1   S    3       3.530512    0.816191 (  0.444635) 

   2   L    4       2.941249   -0.160017 ( -0.099967)     0.856045 (  0.155916) 
   2   L    5       0.683483    0.214036 (  0.399513)     0.538304 (  0.607684) 
   2   L    6       0.222290    0.161536 (  0.700115)     0.085276 (  0.391957) 

   */
  
  protected String fixShellTag(String tag) {
    return tag;
  }

  /*
   ------------------
   MOLECULAR ORBITALS
   ------------------

          ------------
          EIGENVECTORS
          ------------

                      1          2          3          4          5
                  -79.9156   -20.4669   -20.4579   -20.4496   -20.4419
                     A          A          A          A          A   
    1  C  1  S   -0.000003  -0.000029  -0.000004   0.000011   0.000016
    2  C  1  S   -0.000009   0.000140   0.000001   0.000057   0.000065
    3  C  1  X    0.000007  -0.000241  -0.000022  -0.000010  -0.000061
    4  C  1  Y   -0.000008   0.000017  -0.000027  -0.000010   0.000024
    5  C  1  Z    0.000007   0.000313   0.000009  -0.000002  -0.000001
    6  C  1  S    0.000049   0.000875  -0.000164  -0.000521  -0.000440
    7  C  1  X   -0.000066   0.000161   0.000125   0.000034   0.000406
    8  C  1  Y    0.000042   0.000195  -0.000165  -0.000254  -0.000573
    9  C  1  Z    0.000003   0.000045   0.000052   0.000112  -0.000129
   10  C  1 XX   -0.000010   0.000010  -0.000040   0.000019   0.000045
   11  C  1 YY   -0.000010  -0.000031   0.000000  -0.000003   0.000019
...

                      6          7          8          9         10
                  -20.4354   -20.4324   -20.3459   -20.3360   -11.2242
                     A          A          A          A          A   
    1  C  1  S    0.000000  -0.000001   0.000001   0.000000   0.008876
    2  C  1  S   -0.000003   0.000002   0.000003   0.000002   0.000370

...
 TOTAL NUMBER OF BASIS SET SHELLS             =  101

   */

  protected void getMOHeader(String[] tokens, Hashtable[] mos, int nThisLine) throws Exception {
    tokens = getTokens(readLine());
    for (int i = 0; i < nThisLine; i++)
      mos[i].put("energy", new Float(tokens[i]));
    tokens = getTokens(readLine());
    for (int i = 0; i < nThisLine; i++)
      mos[i].put("symmetry", tokens[i]);
  }
}
