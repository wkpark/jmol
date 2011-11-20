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

import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.adapter.smarter.*;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
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

#ZMATRIX
C
H   1 1.089000     
H   1 1.089000  2  109.4710      
H   1 1.089000  2  109.4710  3  120.0000   
H   1 1.089000  2  109.4710  3 -120.0000

or, to add bond orders, just add them as one more integer on the line:

#ZMATRIX
C
O   1 1.3000                 2     
O   1 1.3000    2  180       2      

   */
  private int atomCount;
  private Vector<Atom> vAtoms = new Vector<Atom>();
  
  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith("#")) {
      checkLineForScript();
      return true;
    }
    String[] tokens = getTokens();
    if (tokens.length == 0)
      return true;
    int ia = 0;
    Atom atom = new Atom();
    String element = tokens[0];
    int i = element.length();
    while (--i >= 0 && Character.isDigit(element.charAt(i))) {
      //continue;
    }
    if (i < 0)
      throw new Exception("Bad Z-matrix atom name");
    if (i == element.length() - 1) {
      atom.atomName = element + (atomCount + 1);
    } else {
      atom.atomName = element;
      element = element.substring(0, ++i);
    }
    atom.elementSymbol = element;
    int bondOrder = 1;
    switch (tokens.length) {
    case 2:
      // bond order ignored, if present
      // fall through
    case 1:
      if (atomCount != 0)
        atom = null;
      else
        atom.set(0, 0, 0);
      bondOrder = 0;
      break;
    case 4:
      bondOrder = parseInt(tokens[3]);
      // fall through
    case 3:
      // tokens[1] is ignored
      if (atomCount != 1) {
        atom = null;
        break;
      }
      atom.set(parseFloat(tokens[2]), 0, 0);
      ia = 1;
      break;
    case 6:
    case 8:
      bondOrder = parseInt(tokens[tokens.length - 1]);
      // fall through
    case 5:
    case 7:
      if (tokens.length < 7 && atomCount != 2) {
        atom = null;
        break;
      }
      ia = parseInt(tokens[1]);
      float d = parseFloat(tokens[2]);
      int ib = parseInt(tokens[3]);
      float theta1 = parseFloat(tokens[4]);
      int ic = (tokens.length < 7 ? -1 : parseInt(tokens[5]));
      float theta2 = (tokens.length < 7 ? Float.MAX_VALUE : parseFloat(tokens[6]));
      atom = setAtom(atom, ia, ib, ic, d, theta1, theta2);
      break;
    default:
      atom = null;
    }
    if (atom == null)
      throw new Exception("bad Z-Matrix line");
    atomCount++;
    vAtoms.add(atom);
    if (JmolAdapter.getElementNumber(element) < 1) {
      Logger.info("atom ignored: atom " + atomCount + " - " + atom.atomName);
    } else {
      atomSetCollection.addAtom(atom);
      if (bondOrder > 0)
        atomSetCollection.addBond(new Bond(atom.atomIndex, vAtoms.get(ia - 1).atomIndex, bondOrder));
    }
    return true;
  }

  private Point3f pt0 = new Point3f();
  private Point3f pt1 = new Point3f();
  private Point3f pt2 = new Point3f();
  private Vector3f v1 = new Vector3f();
  private Vector3f v2 = new Vector3f();
  
  private Atom setAtom(Atom atom, int ia, int ib, int ic, float d,
                       float theta1, float theta2) {
    if (Float.isNaN(theta1) || Float.isNaN(theta2) 
        || ia < 1 || ia > atomCount
        || ib < 1 || ib > atomCount)
      return null;
    pt0.set(vAtoms.get(ia - 1));
    pt1.set(vAtoms.get(ib - 1));
    v1.sub(pt1, pt0);
    v1.normalize();
    v1.scale(d);
    Quaternion q;
    if (theta2 == Float.MAX_VALUE) {
      pt2.set(0, 0, 1);
      q = new Quaternion(pt2, theta1);
    } else {
      pt2.set(vAtoms.get(ic - 1));
      v2.sub(pt2, pt1);
      v2.cross(v1, v2);
      q = new Quaternion(v2, theta1);
      v2.set(v1);
      v1 = q.transform(v1);
      q = new Quaternion(v2, -theta2);
    }
    v1 = q.transform(v1);
    pt0.add(v1);
    atom.set(pt0);
    return atom;
  }
}
