/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-18 01:25:52 -0500 (Wed, 18 Apr 2007) $
 * $Revision: 7435 $
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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.viewer.JmolConstants;

import java.util.BitSet;

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;

public abstract class MeshCollection extends Shape {

  // Draw, Isosurface(LcaoCartoon MolecularOrbital), Pmesh
    
  public int meshCount;
  public Mesh[] meshes = new Mesh[4];
  public Mesh currentMesh;
  public int modelCount;
  public boolean isFixed;  
  public String script;
  public int nUnnamed;
  public short colix;
  public String myType;
  public boolean explicitID;
  protected String previousMeshID;
  protected Mesh linkedMesh;
  protected boolean iHaveModelIndex;
  protected int modelIndex;

  public String[] title;
  protected boolean allowMesh = true;
  
  private Mesh setMesh(String thisID) {
    linkedMesh = null;
    if (thisID == null || thisID.indexOf("*") >= 0) {
      currentMesh = null;
      return null;
    }
    int meshIndex = getIndexFromName(thisID);
    if (meshIndex >= 0) {
      currentMesh = meshes[meshIndex];
      if (thisID.equals(JmolConstants.PREVIOUS_MESH_ID))
        linkedMesh = currentMesh.linkedMesh;
    } else {
      allocMesh(thisID);
    }
    if (currentMesh.thisID == null)
      currentMesh.thisID = myType + (++nUnnamed);
    previousMeshID = currentMesh.thisID;
    return currentMesh;
  }

  public void allocMesh(String thisID) {
    meshes = (Mesh[])ArrayUtil.ensureLength(meshes, meshCount + 1);
    currentMesh = meshes[meshCount++] = new Mesh(thisID, g3d, colix);
    previousMeshID = null;
  }

  public void initShape() {
    super.initShape();
    colix = Graphics3D.ORANGE;
    modelCount = viewer.getModelCount();
  }
  
 public void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.debugging) {
      Logger.debug("MeshCollection.setProperty(" + propertyName + "," + value
          + ")");
    }
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

    if ("init" == propertyName) {
      title = null;
      return;
    }
    
    if ("link" == propertyName) {
      if (meshCount >= 2 && currentMesh != null)
        currentMesh.linkedMesh = meshes[meshCount - 2];
      return;
    }
    
    if ("commandOption" == propertyName) {
      String s = "# " + (String) value;
      if (script.indexOf(s) < 0)
        script += " " + s;
      return;
    }

    if ("thisID" == propertyName) {
      String id = (String) value;
      setMesh(id);
      explicitID = (id != null && !id.equals(JmolConstants.PREVIOUS_MESH_ID));
      if (explicitID)
        previousMeshID = id;
      return;
    }

    if ("title" == propertyName) {
      if (value == null) {
        title = null;
      } else if (value instanceof String[]) {
        title = (String[]) value;
      } else {
        int nLine = 1;
        String lines = (String) value;
        for (int i = lines.length(); --i >= 0;)
          if (lines.charAt(i) == '|')
            nLine++;
        title = new String[nLine];
        nLine = 0;
        int i0 = -1;
        for (int i = 0; i < lines.length(); i++)
          if (lines.charAt(i) == '|') {
            title[nLine++] = lines.substring(i0 + 1, i);
            i0 = i;
          }
        title[nLine] = lines.substring(i0 + 1);
      }
      return;
    }

    if ("delete" == propertyName) {
      deleteMesh();
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

    boolean isOn; 
    if ((isOn = ("on" == propertyName)) || "off" == propertyName) {
      if (currentMesh != null) {
        currentMesh.visible = isOn;
      } else {
        int i = 0;
        String key = (explicitID && previousMeshID != null
            && (i = previousMeshID.indexOf("*")) >= 0 ? 
                previousMeshID.substring(0, i).toLowerCase() : null);
        if (key == null || key.length() == 0) {
          for (i = meshCount; --i >= 0;)
            meshes[i].visible = isOn;
        } else {
          for (i = 0; i < meshCount; i++) {
            if (meshes[i].thisID.toLowerCase().indexOf(key) == 0)
              meshes[i].visible = isOn;
          }
        }
      }
      return;
    }
    
    if ("color" == propertyName) {
      if (value == null)
        return;
      colix = Graphics3D.getColix(value);
      if (currentMesh != null) {
        currentMesh.colix = colix;
        if (linkedMesh != null)
          linkedMesh.colix = colix;         
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].colix = colix;
      }
      return;
    }   

    if ("translucency" == propertyName) {
      boolean isTranslucent = (((String) value).equals("translucent"));
      if (currentMesh != null) {
        currentMesh.setTranslucent(isTranslucent, translucentLevel);
        if (linkedMesh != null)
          linkedMesh.setTranslucent(isTranslucent, translucentLevel);         
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].setTranslucent(isTranslucent, translucentLevel);
      }
      return;
    }
    boolean test;
    if ((test = ("nodots" == propertyName)) || "dots" == propertyName) {
      boolean showDots = (!test && value == Boolean.TRUE);
      if (currentMesh != null) {
        currentMesh.showPoints = showDots;
        if (linkedMesh != null)
          linkedMesh.showPoints = showDots;
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].showPoints = showDots;
      }
      return;
    }

    if ((test = ("nomesh" == propertyName)) || "mesh" == propertyName) {
      boolean showMesh = (!test && value == Boolean.TRUE);
      if (currentMesh != null) {
        currentMesh.drawTriangles = showMesh;
        if (linkedMesh != null)
          linkedMesh.drawTriangles = showMesh;
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].drawTriangles = showMesh;
      }
      return;
    }

    if ((test = ("nofill" == propertyName)) || "fill" == propertyName) {
      boolean showFill = (!test && value == Boolean.TRUE);
      if (currentMesh != null) {
        currentMesh.fillTriangles = showFill;
        if (linkedMesh != null)
          linkedMesh.fillTriangles = showFill;
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].fillTriangles = showFill;
      }
      return;
    }

    if ("triangles" == propertyName) {
      boolean showTriangles = (value == Boolean.TRUE);
      if (currentMesh != null) {
        currentMesh.showTriangles = showTriangles;
        if (linkedMesh != null)
          linkedMesh.showTriangles = showTriangles;
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].showTriangles = showTriangles;
      }
      return;
    }

    if ("frontOnly" == propertyName) {
      boolean frontOnly = (value == Boolean.TRUE);
      if (currentMesh != null) {
        currentMesh.frontOnly = frontOnly;
        if (linkedMesh != null)
          linkedMesh.frontOnly = frontOnly;
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].frontOnly = frontOnly;
      }
      return;
    }

    if ((test = ("lighting" == propertyName))
        || Parser.isOneOf(propertyName, "backlit;frontlit;fulllit")) {
      int lighting = (test ? ((Integer) value).intValue()
          : "frontlit" == propertyName ? JmolConstants.FRONTLIT
              : "backlit" == propertyName ? JmolConstants.BACKLIT : JmolConstants.FULLYLIT);
      if (currentMesh != null) {
        currentMesh.setLighting(lighting);
        if (linkedMesh != null)
          linkedMesh.setLighting(lighting);         
      } else {
        for (int i = meshCount; --i >= 0;)
          meshes[i].setLighting(lighting);
      }
      return;
    }
    
    super.setProperty(propertyName, value, bs);
  }

 public Object getProperty(String property, int index) {
    if (property == "count") {
      int n = 0;
      for (int i = 0; i < meshCount; i++)
        if (meshes[i] != null && meshes[i].vertexCount > 0)
          n++;
      return new Integer(n);
    }
    if (property == "ID")
      return (currentMesh == null ? (String) null : currentMesh.thisID);
    if (property == "list") {
      StringBuffer sb = new StringBuffer();
      int k = 0;
      for (int i = 0; i < meshCount; i++) {
        if (meshes[i] == null || meshes[i].vertexCount == 0)
          continue;
        Mesh m = meshes[i];
        sb.append((++k)).append(" id:" + m.thisID).append(
            "; model:" + viewer.getModelNumberDotted(m.modelIndex)).append(
            "; vertices:" + m.vertexCount).append(
            "; polygons:" + m.polygonCount).append("; visible:" + m.visible);
        if (m.title != null) {
          String s = "";
          for (int j = 0; j < m.title.length; j++)
            s += (j == 0 ? "; title:" : " | ") + m.title[j];
          if (s.length() > 100)
            s = s.substring(0, 100) + "...";
          sb.append(s);
        }
        sb.append('\n');
      }
      return sb.toString();
    }
    return null;
  }

  private void deleteMesh() {
    int i = 0;
    if (explicitID && currentMesh != null) {
      for (i = meshCount; meshes[--i] != currentMesh;) {
      }
      deleteMesh(i);
    } else {
      String key = (explicitID && previousMeshID != null
          && (i = previousMeshID.indexOf("*")) >= 0 ? 
              previousMeshID.substring(0, i).toLowerCase() : null);
      if (key == null || key.length() == 0) {
        for (i = meshCount; --i >= 0; )
          meshes[i] = null;
        meshCount = 0;
        nUnnamed = 0;
      } else {
        for (i = meshCount; --i >= 0; ) {
          if (meshes[i].thisID.toLowerCase().indexOf(key) == 0)
            deleteMesh(i);
        }
      }
    }
    currentMesh = null;
  }

  public void deleteMesh(int i) {
    for (int j = i + 1; j < meshCount; ++j)
      meshes[j - 1] = meshes[j];
    meshes[--meshCount] = null;
  }
  
  public int getIndexFromName(String thisID) {
    if (JmolConstants.PREVIOUS_MESH_ID.equals(thisID))
      return (previousMeshID == null 
          ? meshCount - 1 : getIndexFromName(previousMeshID));
    for (int i = meshCount; --i >= 0; ) {
      if (meshes[i] != null && thisID.equals(meshes[i].thisID))
        return i;
    }
    return -1; 
  }
  
  public void setModelIndex(int atomIndex, int modelIndex) {
    if (currentMesh == null)
      return;
    currentMesh.visible = true; 
    if ((currentMesh.atomIndex = atomIndex) >= 0)
      currentMesh.modelIndex = viewer.getAtomModelIndex(atomIndex);
    else if (isFixed)
      currentMesh.modelIndex = -1;
    else if (modelIndex >= 0)
      currentMesh.modelIndex = modelIndex;
    else
      currentMesh.modelIndex = viewer.getCurrentModelIndex();
    currentMesh.scriptCommand = script;
  }

 public String getShapeState() {
    StringBuffer s = new StringBuffer("\n");
    for (int i = 0; i < meshCount; i++) {
      Mesh mesh = meshes[i];
      String cmd = mesh.scriptCommand;
      if (cmd == null)
        continue;
      int pt = cmd.indexOf(";#");
      //not perfect -- user may have that in a title, I suppose...
      if (pt >= 0)
          cmd = cmd.substring(0, pt + 1);
      if (mesh.bitsets != null)  {
        cmd += "# "
            + (mesh.bitsets[0] == null ? "({null})" : Escape.escape(mesh.bitsets[0]))
            + " " + (mesh.bitsets[1] == null ? "({null})" : Escape.escape(mesh.bitsets[1]))
            + (mesh.bitsets[2] == null ? "" : "/" + Escape.escape(mesh.bitsets[2]));
      }
      if (mesh.modelIndex >= 0)
        cmd += "# MODEL({" + mesh.modelIndex + "})";
      if (mesh.linkedMesh != null)
        cmd += " LINK";
      if (mesh.data1 != null) {
        String name = ((String) mesh.data1.elementAt(0)).toLowerCase();
        if (name.indexOf("data2d_") != 0)
          name = "data2d_" + name;
        name = TextFormat.simpleReplace(name, "_xyz", "_");
        cmd = Escape.encapsulateData(name, mesh.data1.elementAt(5)) 
            + "  " + cmd + "# DATA=\"" + name + "\"";
      }
      if (mesh.data2 != null) {
        String name = ((String) mesh.data2.elementAt(0)).toLowerCase();
        if (name.indexOf("data2d_") != 0)
          name = "data2d_" + name;
        name = TextFormat.simpleReplace(name, "_xyz", "_");
        cmd = Escape.encapsulateData(name, mesh.data2.elementAt(5)) 
            + "  " + cmd + "# DATA2=\"" + name + "\"";
      }
      
      if (mesh.modelIndex >= 0 && modelCount > 1)
        appendCmd(s, "frame " + viewer.getModelNumberDotted(mesh.modelIndex));
      appendCmd(s, cmd);
      if (cmd.charAt(0) != '#') {
        if (allowMesh)
        appendCmd(s, mesh.getState(myType));
        if (mesh.isColorSolid)
          appendCmd(s, getColorCommand("$" + mesh.thisID, mesh.colix));
        else if (mesh.colorCommand != null)
          appendCmd(s, mesh.colorCommand);
      }
    }
    return s.toString();
  }
  
 public void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    for (int i = meshCount; --i >= 0;) {
      Mesh mesh = meshes[i];
      mesh.visibilityFlags = (mesh.visible && mesh.isValid
          && (mesh.modelIndex < 0 || bs.get(mesh.modelIndex)
          && (mesh.atomIndex < 0 || !modelSet.isAtomHidden(mesh.atomIndex))
          ) ? myVisibilityFlag
          : 0);
    }
  }
 
  protected void getModelIndex(String script) {
    //pmesh and isosurface state
    int i;
    iHaveModelIndex = false;
    modelIndex = -1;
    if (script == null || (i = script.indexOf("MODEL({")) < 0)
      return;
    int j = script.indexOf("})", i);
    if (j < 0)
      return;
    BitSet bs = Escape.unescapeBitset(script.substring(i + 3, j + 1));
    modelIndex = (bs == null ? -1 : BitSetUtil.firstSetBit(bs));
    iHaveModelIndex = (modelIndex >= 0);
  }
}

 