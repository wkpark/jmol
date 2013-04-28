/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 17:19:14 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4529 $
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

package org.jmol.shapecgo;

import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.shapespecial.Draw;
import org.jmol.shapespecial.DrawMesh;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.JmolList;
import org.jmol.util.SB;

public class CGO extends Draw {
  
  CGOMesh[] cmeshes = new CGOMesh[4];
  private CGOMesh cgoMesh;

  public CGO() {
    super();
  }
  
  private void initCGO() {
    // TODO
    
  }

  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = cmeshes = (CGOMesh[]) ArrayUtil.ensureLength(cmeshes,
        meshCount * 2);
    currentMesh = thisMesh = cgoMesh = cmeshes[index] = (m == null ? new CGOMesh(thisID,
        colix, index) : (CGOMesh) m);
    currentMesh.color = color;
    currentMesh.index = index;
    if (thisID != null && thisID != MeshCollection.PREVIOUS_MESH_ID
        && htObjects != null)
      htObjects.put(thisID.toUpperCase(), currentMesh);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      initCGO();
      setPropertySuper("init", value, bs);
      return;
    }
    
    if ("setCGO" == propertyName) {
      JmolList<Object> list = (JmolList<Object>) value;
      setProperty("init", null, null);
      int n = list.size() - 1;
      setProperty("thisID", list.get(n), null);
      propertyName = "set";
      setProperty("set", value, null);
      return;
    }
    
    if ("set" == propertyName) {
      if (cgoMesh == null) {
        allocMesh(null, null);
        cgoMesh.colix = colix;
        cgoMesh.color = color;
      }
      cgoMesh.isValid = setCGO((JmolList<Object>) value);
      if (cgoMesh.isValid) {
        scale(cgoMesh, newScale);
        cgoMesh.initialize(T.fullylit, null, null);
        cgoMesh.title = title;
        cgoMesh.visible = true;
      }
      return;
    }
    
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      //int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
      //int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
      for (int i = meshCount; --i >= 0;) {
        CGOMesh m = cmeshes[i];
        if (m == null)
          continue;
        boolean deleteMesh = (m.modelIndex == modelIndex);
        if (m.modelFlags != null) {
          m.deleteAtoms(modelIndex);
          deleteMesh = (m.modelFlags.length() == 0);
          if (!deleteMesh)
            continue;
        } 
        if (deleteMesh) {
          meshCount--;
          if (meshes[i] == currentMesh)
            currentMesh = cgoMesh = null;
          meshes = cmeshes = (CGOMesh[]) ArrayUtil
              .deleteElements(meshes, i, 1);
        } else if (meshes[i].modelIndex > modelIndex) {
          meshes[i].modelIndex--;
        }
      }
      resetObjects();
      return;
    }

    setPropertySuper(propertyName, value, bs);
  }

  @Override
  protected void setPropertySuper(String propertyName, Object value, BS bs) {
    currentMesh = cgoMesh;
    setPropMC(propertyName, value, bs);
    cgoMesh = (CGOMesh)currentMesh;  
  }

  private boolean setCGO(JmolList<Object> data) {
    if (cgoMesh == null)
      allocMesh(null, null);
    cgoMesh.clear("cgo");
    return cgoMesh.set(data);
  }

  @Override
  protected void scale(Mesh mesh, float newScale) {
    // TODO
    
  }
  
  @Override
  protected String getCommand2(Mesh mesh, int modelIndex) {
    return "";
  }

  @Override
  public String getShapeState() {
    SB s = new SB();
    return s.toString();
  }
  
}
