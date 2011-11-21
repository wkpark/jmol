/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
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

package org.jmol.adapter.readers.simple;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.adapter.smarter.*;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Quaternion;

public class ZMatrixReader extends AtomSetCollectionReader {
  /*
   * a simple Z-matrix reader
   * 
   * Can be invoked using ZMATRIX::   or with file starting with #ZMATRIX
   * # are comments; can include jmolscript: xxxx
   * 
   * a positive dihedral is defined as being
   * 
   *              (back)
   *        +120 /
   *  (front)---O
   *  
   *  Any invalid element symbol such as X or XX indicates a dummy
   *  atom that will not be included in the model but is needed
   *  to create the structure
   * 
   * Bob Hanson hansonr@stolaf.edu 11/19/2011
   */

  /*

#ZMATRIX -- methane
C
H   1 1.089000     
H   1 1.089000  2  109.4710      
H   1 1.089000  2  109.4710  3  120.0000   
H   1 1.089000  2  109.4710  3 -120.0000

or, to add bond orders, just add them as one more integer on the line:

#ZMATRIX -- CO2 
C
O   1 1.3000                 2     
O   1 1.3000    2  180       2      

any position number may be replaced by a unique atom name, with number:

#ZMATRIX -- CO2
C1
O1   C1 1.3000                2     
O2   C1 1.3000    O1  180     2      

ignored dummy atoms are any atoms starting with "X" and a number,
allowing for positioning:

#ZMATRIX -- CO2
X1
X2   X1 1.0
C1   X1 1.0       X2 90
O1   C1 1.3000    X2 90   X1 0  2     
O2   C1 1.3000    O1 180  X2 0  2      

negative distance indicates that the second angle is a normal angle, not a dihedral

#ZMATRIX -- NH3 (using simple angles only)
N1 
H1 N1 1.0
H2 N1 1.0 H1 107  
H3 N1 -1.0 H1 107 H2 107

negative distance and one negative angle reverses the chirality

#ZMATRIX -- NH3 (using simple angles only; reversed chirality)
N1 
H1 N1 1.0
H2 N1 1.0 H1 107  
H3 N1 -1.0 H1 -107 H2 107

symbolics may be used -- they may be listed first or last

#ZMATRIX

dist 1.0
angle 107

N1 
H1 N1 dist
H2 N1 dist H1 angle 
H3 N1 -dist H1 angle H2 angle

If #ZMATRIX is not the start of the file, MOPAC style is checked.
MOPAC will have the third line blank. Atom names are not allowed,
and isotopes are in the form of "C13". The first two lines will be 
considered to be comments and ignored:

 AM1
Ethane

C
C     1     r21
H     2     r32       1     a321
H     2     r32       1     a321      3  d4213
H     2     r32       1     a321      3 -d4213
H     1     r32       2     a321      3   60.
H     1     r32       2     a321      3  180.
H     1     r32       2     a321      3  d300

r21        1.5
r32        1.1
a321     109.5
d4213    120.0
d300     300.0


   */

  private int atomCount;
  private List<Atom> vAtoms = new ArrayList<Atom>();
  private Map<String, Integer> atomMap = new Hashtable<String, Integer>();
  private String[] tokens;
  private boolean isJmolZformat;
  private List<String[]> lineBuffer = new ArrayList<String[]>();
  private Map<String, Float> symbolicMap = new Hashtable<String, Float>();
  private boolean isMopac;
  
  @Override
  protected boolean checkLine() throws Exception {
    // easiest just to grab all lines that are comments or symbolic first, then do the processing of atoms.
    if (line.startsWith("#")) {
      isJmolZformat = true;
      checkLineForScript();
      return true;
    }
    tokens = getTokens(line);
    if ((isJmolZformat || lineBuffer.size() > 2)
        && tokens.length == 2) {
      getSymbolic();
      return true;
    }
    lineBuffer.add(tokens);
    return true;
  }

  @Override
  protected void finalizeReader() throws Exception {
    // Mopac must have third line blank and not have # as first character -- at least that's what we are using
    isMopac = (lineBuffer.size() > 3 && lineBuffer.get(2).length == 0);
    for (int i = (isMopac ? 3 : 0); i < lineBuffer.size(); i++)
      if ((tokens = lineBuffer.get(i)).length > 0)
        getAtom();
    super.finalizeReader();
  }

  private void getSymbolic() {
    if (symbolicMap.containsKey(tokens[0]))
      return;
    float f = parseFloat(tokens[1]);
    symbolicMap.put(tokens[0], Float.valueOf(f));
    Logger.info("symbolic " + tokens[0] + " = " + f);
  }

  private void getAtom() throws Exception {
    float f;
    Atom atom = new Atom();
    String element = tokens[0];
    int i = element.length();
    while (--i >= 0 && Character.isDigit(element.charAt(i))) {
      //continue;
    }
    if (++i == 0)
      throw new Exception("Bad Z-matrix atom name");
    if (i == element.length()) {
      // no number -- append atomCount
      atom.atomName = element + (atomCount + 1);
    } else {
      // has a number -- pull out element
      atom.atomName = element;
      element = element.substring(0, i);
    }
    if (isMopac && i != tokens[0].length())      // C13 == 13C
      element = tokens[0].substring(i) + element;
    setElementAndIsotope(atom, element);
    
    int ia = getAtomIndex(1);
    int bondOrder = 1;
    switch (tokens.length) {
    case 1:
      if (atomCount != 0)
        atom = null;
      else
        atom.set(0, 0, 0);
      bondOrder = 0;
      break;
    case 4:
      bondOrder = (int) getValue(3);
      // fall through
    case 3:
      // tokens[1] is ignored
      if (atomCount != 1) {
        atom = null;
        break;
      }
      f = getValue(2);
      if (Float.isNaN(f))
        return;
      atom.set(f, 0, 0);
      ia = 0;
      break;
    case 6:
    case 8:
      bondOrder = (int) getValue(tokens.length - 1);
      // fall through
    case 5:
    case 7:
      if (tokens.length < 7 && atomCount != 2) {
        atom = null;
        break;
      }
      float d = getValue(2);
      if (Float.isNaN(d))
        return;
      float theta1 = getValue(4);
      if (Float.isNaN(theta1))
        return;
      float theta2 = (tokens.length < 7 ? Float.MAX_VALUE : getValue(6));
      if (Float.isNaN(theta2))
        return;
      int ib = getAtomIndex(3);
      int ic = (tokens.length < 7 ? -1 : getAtomIndex(5));
      atom = setAtom(atom, ia, ib, ic, d, theta1, theta2);
      break;
    default:
      atom = null;
    }
    if (atom == null)
      throw new Exception("bad Z-Matrix line");
    vAtoms.add(atom);
    if (!isMopac)
      atomMap.put(atom.atomName, Integer.valueOf(atomCount));
    atomCount++;
    if (element.startsWith("X") && JmolAdapter.getElementNumber(element) < 1) {
      Logger.info("#dummy atom ignored: atom " + atomCount + " - "
          + atom.atomName);
    } else {
      atomSetCollection.addAtom(atom);
      Logger.info(atom.atomName + " " + atom.x + " " + atom.y + " " + atom.z);
      if (bondOrder > 0)
        atomSetCollection.addBond(new Bond(atom.atomIndex,
            vAtoms.get(ia).atomIndex, bondOrder));
    }
  }

  private float getValue(int i) {
    String key = tokens[i];
    float f = parseFloat(key);
    if (Float.isNaN(f)) {
      boolean isNeg = key.startsWith("-");
      Float F = symbolicMap.get(isNeg ? key.substring(1) : key);
      if (F != null && !Float.isNaN(f = F.floatValue()) && isNeg)
        f = -f;
    }
    return f;
  }

  private int getAtomIndex(int i) {
    if (i >= tokens.length)
      return -1;
    int ia = parseInt(tokens[i]);
    if (tokens[i].length() != ("" + ia).length()) // check for clean integer, not "13C1"
      ia = atomMap.get(tokens[i]).intValue();
    else
      ia--;    
    return ia;
  }

  private final Point3f pt0 = new Point3f();
  private final Vector3f v1 = new Vector3f();
  private final Vector3f v2 = new Vector3f();
  private final Point4f plane1 = new Point4f();
  private final Point4f plane2 = new Point4f();
  
  private Atom setAtom(Atom atom, int ia, int ib, int ic, float d,
                       float theta1, float theta2) {
    if (Float.isNaN(theta1) || Float.isNaN(theta2))
      return null;
    pt0.set(vAtoms.get(ia));
    v1.sub(vAtoms.get(ib), pt0);
    v1.normalize();
    if (theta2 == Float.MAX_VALUE) {
      // just the first angle being set
      v2.set(0, 0, 1);
      (new Quaternion(v2, theta1)).transform(v1, v2);
    } else if (d >= 0) {
      // theta2 is a dihedral angle
      // just do two quaternion rotations
      v2.sub(vAtoms.get(ic), pt0);
      v2.cross(v1, v2);
      (new Quaternion(v2, theta1)).transform(v1, v2);
      (new Quaternion(v1, -theta2)).transform(v2, v2);
    } else {
      // d < 0
      // theta1 and theta2 are simple angles atom-ia-ib and atom-ia-ic 
      // get vector that is intersection of two planes and go from there
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ib, ic, -d, theta1, 0),
          v1, plane1);
      Measure.getPlaneThroughPoint(setAtom(atom, ia, ic, ib, -d, theta2, 0),
          v1, plane2);
      List<Object> list = Measure.getIntersection(plane1, plane2);
      if (list.size() == 0)
        return null;
      pt0.set((Point3f) list.get(0));
      d = (float) Math.sqrt(d * d - pt0.distanceSquared(vAtoms.get(ia)))
          * Math.signum(theta1) * Math.signum(theta2);
      v2.set((Vector3f) list.get(1));
    }
    atom.scaleAdd(d, v2, pt0);
    return atom;
  }
}
