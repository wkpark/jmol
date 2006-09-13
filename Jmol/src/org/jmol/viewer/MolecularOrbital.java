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
import java.util.Vector;

import javax.vecmath.Point4f;

import org.jmol.util.Logger;

class MolecularOrbital extends Isosurface {

  void initShape() {
    super.setProperty("thisID", "mo", null);
    return;
  }

  // these are globals, stored here and only passed on when the they are needed. 

  String moTranslucency = null;
  Point4f moPlane = null;
  Float moCutoff = new Float(Isosurface.defaultQMOrbitalCutoff);
  Float moResolution = null;
  Float moScale = null;
  Integer moColorPos = null;
  Integer moColorNeg = null;
  Float moRed = null;
  Float moBlue = null;
  String moTitleFormat = null;
  boolean moDebug;
  boolean moIsPositiveOnly = false;
  int myColorPt;
  String strID;

  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("MolecularOrbital.setProperty " + propertyName + " " + value);

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    if ("init" == propertyName) {
      myColorPt = 0;
      moDebug = false;
      strID = (String) value;
      // overide bitset selection
      super.setProperty("init", null, null);
      return;
    }

    if ("cutoff" == propertyName) {
      moCutoff = (Float) value;
      moIsPositiveOnly = false;
      return;
    }

    if ("scale" == propertyName) {
      moScale = (Float) value;  // box only
      return;
    }

    if ("cutoffPositive" == propertyName) {
      moCutoff = (Float) value;
      moIsPositiveOnly = true;
      return;
    }

    if ("resolution" == propertyName) {
      moResolution = (Float) value;
      return;
    }
    
    if ("titleFormat" == propertyName) {
      moTitleFormat = (String) value;
      return;
    }

    if ("colorRGB" == propertyName) {
      moColorPos = (Integer) value;
      if (myColorPt++ == 0)
        moColorNeg = moColorPos;
      return;
    }

    if ("plane" == propertyName) {
      moPlane = (Point4f) value;
      return;
    }

    if ("molecularOrbital" == propertyName) {
      setOrbital(((Integer) value).intValue());
      return;
    }

    if ("translucency" == propertyName) {
      moTranslucency = (String) value;
    }

    super.setProperty(propertyName, value, bs);

  }
  
  Object getProperty(String propertyName, int param) {
    if (propertyName == "showMO") {
      StringBuffer str = new StringBuffer();
      String infoType = "jvxlFileData";
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        return "";
      int thisMO = param;
      int currentMO = qm_moNumber;
      if (currentMO == 0)
        thisMO = 0;
      int nTotal = (thisMO > 0 ? 1 : nOrb);
      for (int i = 1; i <= nOrb; i++)
        if (thisMO == 0 || thisMO == i || thisMO == Integer.MAX_VALUE
            && i == currentMO) {
          super.setProperty("init", "mo_show", null);
          setOrbital(i);
          str.append(super.getProperty(infoType, nTotal));
          infoType = "jvxlSurfaceData";
          super.setProperty("delete", "mo_show", null);
        }
      return "" + str;
    }
    if (propertyName == "moNumber") {
      return (qm_moNumber == 0 ? null : new Integer(qm_moNumber));
    }
    return null;
  }
  
  void setOrbital(int moNumber) {
    super.setProperty("reset", strID, null);
    if (moDebug)
      super.setProperty("debug", Boolean.TRUE, null);
    if (moScale != null)
      super.setProperty("scale", moScale, null);
    if (moResolution != null)
      super.setProperty("resolution", moResolution, null);
    if (moPlane != null) {
      super.setProperty("plane", moPlane, null);
      if (moCutoff != null) {
        super.setProperty("red", new Float(-moCutoff.floatValue()), null);
        super.setProperty("blue", moCutoff, null);
      }
    } else {
      if (moCutoff != null)
        super.setProperty((moIsPositiveOnly ? "cutoffPositive" : "cutoff"),
            moCutoff, null);
      if (moColorNeg != null)
        super.setProperty("colorRGB", moColorNeg, null);
      if (moColorPos != null)
        super.setProperty("colorRGB", moColorPos, null);
    }
    super.setProperty("title", moTitleFormat, null);
    super.setProperty("molecularOrbital", new Integer(moNumber), null);
    if (moTranslucency != null)
      super.setProperty("translucency", moTranslucency, null);
    return;
  }
}
