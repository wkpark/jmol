/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;

import java.util.BitSet;

abstract class MeshCollection extends SelectionIndependentShape {

  int meshCount;
  Mesh[] meshes = new Mesh[4];
  Mesh currentMesh;

  void initShape() {
    colix = Graphics3D.ORANGE;
  }

  void setProperty(String propertyName, Object value, BitSet bs) {

    /*
    System.out.println("setProperty(" + propertyName + "," + value + ")");
    System.out.println("meshCount=" + meshCount +
                       " currentMesh=" + currentMesh);
    for (int i = 0; i < meshCount; ++i) {
      Mesh mesh = meshes[i];
      System.out.println("i=" + i +
                         " mesh.meshID=" + mesh.meshID +
                         " mesh.visible=" + mesh.visible +
                         " mesh.translucent=" + mesh.translucent +
                         " mesh.colix=" + mesh.meshColix);
    }
    */
    if ("meshID" == propertyName) {
      String meshID = (String)value;
      //      System.out.println("meshID=" + meshID);
      if (meshID == null) {
        currentMesh = null;
        return;
      }
      for (int i = meshCount; --i >= 0; ) {
        currentMesh = meshes[i];
        if (meshID.equals(currentMesh.meshID))
          return;
      }
      allocMesh(meshID);
      return;
    }
    if ("on" == propertyName) {
      if (currentMesh != null)
        currentMesh.visible = true;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].visible = true;
      }
      return;
    }
    if ("off" == propertyName) {
      if (currentMesh != null)
        currentMesh.visible = false;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].visible = false;
      }
      return;
    }
    if ("test1" == propertyName) {
      if (currentMesh != null)
        currentMesh.test1();
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].test1();
      }
      return;
    }
    if ("color" == propertyName) {
      if (value != null) {
        colix = Graphics3D.getColix(value);
        if (currentMesh != null) {
          currentMesh.colix = colix;
          currentMesh.vertexColixes = null;
        } else {
          for (int i = meshCount; --i >= 0; ) {
            Mesh mesh = meshes[i];
            mesh.colix = colix;
            mesh.vertexColixes = null;
          }
        }
      }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      if (currentMesh != null)
        currentMesh.colix = 
          Graphics3D.setTranslucent(currentMesh.colix, isTranslucent);
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].colix = 
            Graphics3D.setTranslucent(meshes[i].colix, isTranslucent);
      }
    }
    if ("dots" == propertyName) {
      boolean showDots = value == Boolean.TRUE;
      if (currentMesh != null)
        currentMesh.showPoints = showDots;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].showPoints = showDots;
      }
      return;
    }
    if ("mesh" == propertyName) {
      boolean showMesh = value == Boolean.TRUE;
      if (currentMesh != null)
        currentMesh.drawTriangles = showMesh;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].drawTriangles = showMesh;
      }
      return;
    }
    if ("fill" == propertyName) {
      boolean showFill = value == Boolean.TRUE;
      if (currentMesh != null)
        currentMesh.fillTriangles = showFill;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].fillTriangles = showFill;
      }
      return;
    }
    if ("delete" == propertyName) {
      if (currentMesh != null) {
        int iCurrent;
        for (iCurrent = meshCount; meshes[--iCurrent] != currentMesh; )
          {}
        for (int j = iCurrent + 1; j < meshCount; ++j)
          meshes[j - 1] = meshes[j];
        meshes[--meshCount] = null;
        currentMesh = null;
      } else {
        for (int i = meshCount; --i >= 0; )
          meshes[i] = null;
        meshCount = 0;
      }
      return;
    }
  }

  void allocMesh(String meshID) {
    meshes = (Mesh[])Util.ensureLength(meshes, meshCount + 1);
    currentMesh = meshes[meshCount++] = new Mesh(viewer, meshID, g3d, colix);
  }

  int ichNextParse;
  
  float parseFloat(String str) {
    return parseFloatChecked(str, 0, str.length());
  }

  float parseFloat(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Float.NaN;
    return parseFloatChecked(str, ich, cch);
  }

  float parseFloat(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichStart, ichMax);
  }

  final static float[] decimalScale =
  {0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f};
  final static float[] tensScale =
  {10, 100, 1000, 10000, 100000, 1000000};

  float parseFloatChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    float value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      ++ich;
      negative = true;
    }
    ch = 0;
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      ++ich;
      digitSeen = true;
    }
    if (ch == '.') {
      int iscale = 0;
      while (++ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
        if (iscale < decimalScale.length)
          value += (ch - '0') * decimalScale[iscale];
        ++iscale;
        digitSeen = true;
      }
    }
    if (! digitSeen)
      value = Float.NaN;
    else if (negative)
      value = -value;
    if (ich < ichMax && (ch == 'E' || ch == 'e')) {
      if (++ich >= ichMax)
        return Float.NaN;
      ch = str.charAt(ich);
      if ((ch == '+') && (++ich >= ichMax))
        return Float.NaN;
      int exponent = parseIntChecked(str, ich, ichMax);
      if (exponent == Integer.MIN_VALUE)
        return Float.NaN;
      if (exponent > 0)
        value *= ((exponent < tensScale.length)
                  ? tensScale[exponent - 1]
                  : Math.pow(10, exponent));
      else if (exponent < 0)
        value *= ((-exponent < decimalScale.length)
                  ? decimalScale[-exponent - 1]
                  : Math.pow(10, exponent));
    } else {
      ichNextParse = ich; // the exponent code finds its own ichNextParse
    }
    //    System.out.println("parseFloat(" + str + "," + ichStart + "," +
    //                       ichMax + ") -> " + value);
    return value;
  }
  
  int parseInt(String str) {
    return parseIntChecked(str, 0, str.length());
  }

  int parseInt(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ich, cch);
  }

  int parseInt(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichStart, ichMax);
  }

  int parseIntChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    int value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      negative = true;
      ++ich;
    }
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      digitSeen = true;
      ++ich;
    }
    if (! digitSeen)
      value = Integer.MIN_VALUE;
    else if (negative)
      value = -value;
    //    System.out.println("parseInt(" + str + "," + ichStart + "," +
    //                       ichMax + ") -> " + value);
    ichNextParse = ich;
    return value;
  }
}

