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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;

import java.util.BitSet;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

abstract class MeshCollection extends Shape {

  // Draw, Isosurface(LcaoCartoon MolecularOrbital), Pmesh
  
  int meshCount;
  Mesh[] meshes = new Mesh[4];
  Mesh currentMesh;
  int modelCount;
  boolean isFixed;  
  String script;
  int nUnnamed;
  short colix;
  String myType;

  private Mesh setMesh(String thisID) {
    if (thisID == null) {
      currentMesh = null;
      return null;
    }
    int meshIndex = getIndexFromName(thisID);
    if (meshIndex >= 0) {
      currentMesh = meshes[meshIndex];
    } else {
      allocMesh(thisID);
    }
    return currentMesh;
  }

  void allocMesh(String thisID) {
    meshes = (Mesh[])ArrayUtil.ensureLength(meshes, meshCount + 1);
    currentMesh = meshes[meshCount++] = new Mesh(viewer, thisID, g3d, colix);
  }

  void initShape() {
    colix = Graphics3D.ORANGE;
    modelCount = viewer.getModelCount();
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("MeshCollection.setProperty(" + propertyName + "," + value + ")");
    /*
    Logger.debug("meshCount=" + meshCount +
                       " currentMesh=" + currentMesh);
    for (int i = 0; i < meshCount; ++i) {
      Mesh mesh = meshes[i];
      Logger.debug("i=" + i +
                         " mesh.thisID=" + mesh.thisID +
                         " mesh.visible=" + mesh.visible +
                         " mesh.translucent=" + mesh.translucent +
                         " mesh.colix=" + mesh.meshColix);
    }
    */
    
    if ("thisID" == propertyName) {
      setMesh((String)value);
      return;
    }
    
    if ("reset" == propertyName) {
      String thisID = (String) value;
      if (setMesh(thisID) == null)
        return;
      deleteMesh();
      setMesh(thisID);
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

    if ("background" == propertyName) {
      boolean doHide = ! ((Boolean) value).booleanValue();
      if (currentMesh != null)
        currentMesh.hideBackground = doHide;
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].hideBackground = doHide;
      }
      return;
    }
    if ("title" == propertyName) {
      if (currentMesh != null) {
        currentMesh.title = (String[]) value;
      }
      return;
    }
    
    if ("color" == propertyName) {
      if (value instanceof String && ((String)value).equals("sets")) {
        currentMesh.allocVertexColixes();
        for (int i = 0; i < currentMesh.surfaceSet.length; i++)
          for (int j = 0; j < currentMesh.vertexCount; j++)
          if (currentMesh.surfaceSet[i].get(j))
            currentMesh.vertexColixes[j] = Graphics3D.getColix(Graphics3D.colorNames[i]);
        return;
      }
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
      boolean isTranslucent = (((String)value).equals("translucent"));
      if (currentMesh != null)
        currentMesh.setTranslucent(isTranslucent);
      else {
        for (int i = meshCount; --i >= 0; )
          meshes[i].setTranslucent(isTranslucent);
      }
      return;
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
      deleteMesh();
      return;
    }
    
  }

  private void deleteMesh() {
    if (currentMesh != null) {
      int iCurrent;
      for (iCurrent = meshCount; meshes[--iCurrent] != currentMesh; )
        {}
      deleteMesh(iCurrent);
      currentMesh = null; 
    } else {
      for (int i = meshCount; --i >= 0; )
        meshes[i] = null;
      meshCount = 0;
    }
  }

  void deleteMesh(int i) {
    for (int j = i + 1; j < meshCount; ++j)
      meshes[j - 1] = meshes[j];
    meshes[--meshCount] = null;
  }
  
  int getIndexFromName(String thisID) {
    for (int i = meshCount; --i >= 0; ) {
      if (meshes[i] != null && thisID.equals(meshes[i].thisID))
        return i;
    }
    return -1; 
  }
  
  void setID(Mesh mesh, String id) {
    if (mesh == null)
      return;
    mesh.thisID = id;
  }
  
  void setModelIndex(int atomIndex) {
    if (currentMesh == null)
      return;
    currentMesh.visible = true; 
    if (modelCount < 2)
      isFixed = true;
    if ((currentMesh.atomIndex = atomIndex) >= 0)
      currentMesh.modelIndex = (atomIndex < 0 ? -1 : viewer
          .getAtomModelIndex(atomIndex));
    else if (isFixed)
      currentMesh.modelIndex = -1;
    else
      currentMesh.modelIndex = viewer.getCurrentModelIndex();
    if (currentMesh.thisID == null)
      currentMesh.thisID = myType + (++nUnnamed);
    currentMesh.scriptCommand = script;
  }

  String getShapeState() {
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < meshCount; i++) {
      String cmd = meshes[i].scriptCommand;
      if (cmd == null)
        continue;
      Mesh mesh = meshes[i];
      if (mesh.modelIndex > 0 && modelCount > 1)
        appendCmd(s, "frame " + viewer.getModelNumber(mesh.modelIndex));
      s.append(cmd + "\n");
      if (cmd.charAt(0) != '#') {
        s.append(getMeshState(mesh, myType));
        if (mesh.vertexColixes == null)
          appendCmd(s, getColorCommand("$" + mesh.thisID, mesh.colix));
      }
    }
    return s.toString();
  }

  String getMeshState(Mesh mesh, String type) {
    StringBuffer s = new StringBuffer();
    if (mesh == null)
      return "";
    if (mesh.showPoints)
      appendCmd(s, type + " dots");
    if (mesh.drawTriangles)
      appendCmd(s, type + " mesh");
    if (!mesh.fillTriangles)
      appendCmd(s, type + " nofill");
    if (!mesh.visible)
      appendCmd(s, type + " off");
    return s.toString();
  }
  
  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    for (int i = meshCount; --i >= 0;) {
      Mesh mesh = meshes[i];
      mesh.visibilityFlags = (mesh.visible && mesh.isValid
          && (mesh.modelIndex < 0 || bs.get(mesh.modelIndex)
          && (mesh.atomIndex < 0 || !frame.bsHidden.get(mesh.atomIndex))
          ) ? myVisibilityFlag
          : 0);
    }
  }
}

 