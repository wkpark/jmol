/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

package org.jmol.viewer;

import java.util.BitSet;

import javax.vecmath.Vector3f;

import org.jmol.util.Logger;

class LcaoCartoon extends Isosurface {

  // these are globals, stored here and only passed on when the they are needed. 

  Integer lcaoColorPos = null;
  Integer lcaoColorNeg = null;
  String thisType;
  Float lcaoScale = null;
  int myColorPt;
  String lcaoID;
  BitSet thisSet = null;

  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("\nLcaoCartoon.setProperty " + propertyName + " " + value);

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    if ("init" == propertyName) {
      myColorPt = 0;
      lcaoID = null;
      thisSet = bs;
      // overide bitset selection
      super.setProperty("init", null, null);
      return;
    }

    if ("on" == propertyName) {
      setLcaoOn(true);
      return;
    }

    if ("off" == propertyName) {
      setLcaoOn(false);
      return;
    }

    if ("delete" == propertyName) {
      deleteLcaoCartoon();
      return;
    }

    if ("lcaoID" == propertyName) {
      lcaoID = (String) value;
      return;
    }

    if ("select" == propertyName) {
      thisType = (String) value;
      return;
    }

    if ("scale" == propertyName) {
      lcaoScale = (Float) value;
      return;
    }

    if ("colorRGB" == propertyName) {
      lcaoColorPos = (Integer) value;
      if (myColorPt++ == 0)
        lcaoColorNeg = lcaoColorPos;
      return;

    }

    if ("create" == propertyName) {
      createLcaoCartoon((String) value);
      return;
    }

    super.setProperty(propertyName,value,bs);
  }

  void setLcaoOn(boolean TF) {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        setLcaoOn(i, TF);
  }

  void setLcaoOn(int iAtom, boolean TF) {
    String id = getID(iAtom);
    for (int i = meshCount; --i >=0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        meshes[i].visible = TF;
  }
    
  void deleteLcaoCartoon() {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        deleteLcaoCartoon(i);
  }

  void deleteLcaoCartoon(int iAtom) {
    String id = getID(iAtom);
    for (int i = meshCount; --i >=0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMesh(i);
  }
    
  void createLcaoCartoon(String type) {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        createLcaoCartoon(type, i);
  }

  void createLcaoCartoon(String type, int iAtom) {
    String id = "lcao_" + (iAtom + 1) + "_" + type;
    for (int i = meshCount; --i >=0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMesh(i);
    super.setProperty("init", null, null);
    super.setProperty("thisID", id, null);
    if (lcaoScale != null)
      super.setProperty("scale",lcaoScale,null);
    if (lcaoColorNeg != null) {
      super.setProperty("colorRGB",lcaoColorNeg,null);
      super.setProperty("colorRGB",lcaoColorPos,null);
    }
    super.setProperty("lcaoType",type,null);
    Vector3f[] axes = { new Vector3f(), new Vector3f(),
        new Vector3f(frame.atoms[iAtom])};
    if (type.equalsIgnoreCase("s") || viewer.getPrincipalAxes(iAtom, axes[0], axes[1], type, true))
      super.setProperty("lcaoCartoon",axes,null);
  }
    
  String getID(int i) {
    return (lcaoID != null ? lcaoID : "lcao_" + (i + 1) + "_"
        + (thisType != null ? thisType : ""));
  }
}
