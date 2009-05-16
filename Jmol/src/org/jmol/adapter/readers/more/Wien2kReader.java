/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
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
import org.jmol.util.TextFormat;

import java.io.BufferedReader;

/**
 * A reader for Wein2k DFT files.  
 * 
 * Bob Hanson hansonr@stolaf.edu 5/14/2009
 *   
 */

public class Wien2kReader extends AtomSetCollectionReader {

  private boolean isrhombohedral;
  private char latticeCode;
  
  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("wien2k");
    try {
      setFractionalCoordinates(true);
      atomSetCollection.setCollectionName(readLine());
      readUnitCell();
      readAtoms();
      readSymmetry();
      applySymmetryAndSetTrajectory();
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  private void readUnitCell() throws Exception {    
    readLine();
    isrhombohedral = ((latticeCode = line.charAt(0)) == 'R');
    //todo: need code here for CXZ, CXY, etc. They are NOT the standard "C" SHELX
    if (latticeCode == 'F' || line.startsWith("CXY"))
      atomSetCollection.setLatticeParameter(latticeCode);
    if (line.length() > 32) {
      String name = line.substring(32).trim();
      if (name.indexOf(" ") >= 0)
        name = name.substring(name.indexOf(" ") + 1);
      if (name.indexOf("_") >= 0)
        name = name.substring(name.indexOf("_") + 1);
      setSpaceGroupName(name);
    }
    float factor = (readLine().toLowerCase().indexOf("ang") >= 0 ? 1f : ANGSTROMS_PER_BOHR);
    readLine();
    float a = parseFloat(line.substring(0,10)) * factor;
    float b = parseFloat(line.substring(10,20)) * factor;
    float c = parseFloat(line.substring(20,30)) * factor;
    int l = line.length();
    float alpha = (l >= 40 ? parseFloat(line.substring(30,40)) : 0);
    float beta = (l >= 50 ? parseFloat(line.substring(40,50)) : 0);
    float gamma = (l >= 60 ? parseFloat(line.substring(50,60)) : 0);
    if (isrhombohedral) {
      float ar = (float) Math.sqrt(a * a /3 + c * c / 9) ;
      alpha = beta = gamma = (float) (Math.acos( (2*c * c  - 3 * a * a) 
          / (2 * c * c + 6 * a * a)) * 180f / Math.PI);
      a = b = c = ar;
    }
    if (Float.isNaN(alpha) || alpha == 0)
      alpha = 90;
    if (Float.isNaN(beta) || beta == 0)
      beta = 90;
    if (Float.isNaN(gamma) || gamma == 0)
      gamma = 90; 
    setUnitCell(a, b, c, alpha, beta, gamma);  
  }
  
  private void readAtoms() throws Exception {

    // format (4X,I4,4X,F10.8,3X,F10.8,3X,F10.8)
    
    readLine();
    while (line != null && line.indexOf("ATOM") == 0) {
      float a = parseFloat(line.substring(12,22));
      float b = parseFloat(line.substring(25,35));
      float c = parseFloat(line.substring(38,48));
      if (false && isrhombohedral) {
        float ar = a;
        float br = b;
        float cr = c;
        a = ar * 2 / 3 - br * 1 / 3 - cr * 1 / 3;
        b = ar * 1 / 3 + br * 1 / 3 - cr * 2 / 3;
        c = ar * 1 / 3 + br * 1 / 3 + cr * 1 / 3;        
      }
      readLine();
      if (line.indexOf("MULT=") == 10)
        for (int i = parseInt(line.substring(15,18)); --i >= 0; ) 
          readLine();
      // format (A10,5X,I5,5X,F10.8,5X,F10.5,5X,F5.2)
      
      Atom atom = atomSetCollection.addNewAtom();
      String atomName = line.substring(0, 10);
      String sym = atomName.substring(0,2).trim();
      if (sym.length() == 2 && Character.isDigit(sym.charAt(1)))
        sym = sym.substring(0, 1);
      atom.elementSymbol = sym;
      atom.atomName = TextFormat.simpleReplace(atomName, " ", "");
      setAtomCoord(atom, a, b, c);
      while (readLine() != null && line.indexOf("ATOM") < 0 && line.indexOf("SYMMETRY") < 0) {
      }      
    }
    // return with "SYMMETRY" line in buffer
  }
  
  private void readSymmetry() throws Exception {
    if (line.indexOf("SYMMETRY") < 0)
      return;
    int n = parseInt(line.substring(0, 4).trim());
    for (int i = n; --i >= 0;) {
      String xyz = getJones() + "," + getJones() + "," + getJones();
      setSymmetryOperator(xyz);
      readLine();
    }   
  }
  
  private final String cxyz = " x y z";
  private String getJones() throws Exception {
    readLine();
    String xyz = "";
    // 1 0 0 0.0000000
    float trans = parseFloat(line.substring(6));
    for (int i = 0; i < 6; i++) {
      if (line.charAt(i) == '-')
        xyz += "-";
      if (line.charAt(++i) == '1') {
        xyz += cxyz.charAt(i);
        if (trans > 0)
          xyz += "+";
        if (trans != 0)
          xyz += trans;
      }
    }
    return xyz;
  }
}
