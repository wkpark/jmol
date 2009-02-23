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
import org.jmol.viewer.Token;

import java.util.BitSet;

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;
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
  protected boolean allowContourLines;

  public String[] title;
  protected boolean allowMesh = true;
  
  private Mesh setMesh(String thisID) {
    linkedMesh = null;
    if (thisID == null || TextFormat.isWild(thisID)) {
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
      Logger.debug("MeshCollection.setProperty(" 
          + propertyName + "," + (propertyName == "token" ? 
              Token.nameOf(((Integer)value).intValue()): value.toString())
          + ")");
    }

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

    if ("color" == propertyName) {
      if (value == null)
        return;
      colix = Graphics3D.getColix(value);
      setProperty(Token.color, false);
      return;
    }

    if ("translucency" == propertyName) {
      setProperty(Token.translucent, (((String) value).equals("translucent")));
      return;
    }

    if ("hidden" == propertyName) {
      value = new Integer(((Boolean)value).booleanValue() ? Token.off: Token.on);
      propertyName = "token";
      //continue
    }

    if ("token" == propertyName) {
      int tok = ((Integer) value).intValue();
      boolean test = true;
      switch (tok) {
      case Token.on:
      case Token.frontlit:
      case Token.backlit:
      case Token.fullylit:
      case Token.dots:
      case Token.mesh:
      case Token.fill:
      case Token.triangles:
      case Token.frontonly:
        break;
      case Token.off:
        test = false;
        tok = Token.on;
        break;
      case Token.contourlines:
        if (!allowContourLines)
          tok = Token.mesh;
        break;
      case Token.nocontourlines:
        test = false;
        tok = (allowContourLines ? Token.contourlines : Token.mesh);
        break;
      case Token.nodots:
        test = false;
        tok = Token.dots;
        break;
      case Token.nomesh:
        test = false;
        tok = Token.mesh;
        break;
      case Token.nofill:
        test = false;
        tok = Token.fill;
        break;
      case Token.notriangles:
        test = false;
        tok = Token.triangles;
        break;
      case Token.notfrontonly:
        test = false;
        tok = Token.frontonly;
        break;
      default:
        System.out.println("PROBLEM IN MESHCOLLECTION: token? " + Token.nameOf(tok));
      }
      setProperty(tok, test);
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

 private void setProperty(int tokProp, boolean bProp) {
    if (currentMesh != null) {
      switch (tokProp) {
      case Token.on:
        currentMesh.visible = bProp;
        return;
      case Token.color:
        currentMesh.colix = colix;
        if (linkedMesh != null)
          linkedMesh.colix = colix;
        return;
      case Token.translucent:
        currentMesh.setTranslucent(bProp, translucentLevel);
        if (linkedMesh != null)
          linkedMesh.setTranslucent(bProp, translucentLevel);
        return;
      case Token.frontlit:
      case Token.backlit:
      case Token.fullylit:
        currentMesh.setLighting(tokProp);
        if (linkedMesh != null)
          linkedMesh.setLighting(tokProp);
        return;
      case Token.contourlines:
        currentMesh.showContourLines = bProp;
        if (linkedMesh != null)
          linkedMesh.showContourLines = bProp;
        return;
      case Token.dots:
        currentMesh.showPoints = bProp;
        if (linkedMesh != null)
          linkedMesh.showPoints = bProp;
        return;
      case Token.mesh:
        currentMesh.drawTriangles = bProp;
        if (linkedMesh != null)
          linkedMesh.drawTriangles = bProp;
        return;
      case Token.fill:
        currentMesh.fillTriangles = bProp;
        if (linkedMesh != null)
          linkedMesh.fillTriangles = bProp;
        return;
      case Token.triangles:
        currentMesh.showTriangles = bProp;
        if (linkedMesh != null)
          linkedMesh.showTriangles = bProp;
        return;
      }
      return;
    }
    String key = (explicitID && previousMeshID != null
        && TextFormat.isWild(previousMeshID) ? previousMeshID.toUpperCase()
        : null);
    if (key != null && key.length() == 0)
      key = null;
    for (int i = 0; i < meshCount; i++) {
      Mesh m = meshes[i];
      if (key == null
          || TextFormat.isMatch(m.thisID.toUpperCase(), key, true, true))
        switch (tokProp) {
        case Token.on:
          m.visible = bProp;
          break;
        case Token.color:
          m.colix = colix;
          break;
        case Token.translucent:
          m.setTranslucent(bProp, translucentLevel);
          break;
        case Token.frontlit:
        case Token.backlit:
        case Token.fullylit:
          m.setLighting(tokProp);
          break;
        case Token.dots:
          m.showPoints = bProp;
          break;
        case Token.mesh:
          m.drawTriangles = bProp;
          break;
        case Token.fill:
          m.fillTriangles = bProp;
          break;
        case Token.triangles:
          m.showTriangles = bProp;
          break;
        }
    }
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
    if (property == "command") {
      String key = previousMeshID.toUpperCase();
      boolean isWild = TextFormat.isWild(key);
      StringBuffer sb = new StringBuffer();
      for (int i = meshCount; --i >= 0;) {
        String id = meshes[i].thisID.toUpperCase();
        if (id.equals(key) || isWild && TextFormat.isMatch(id, key, true, true))
            getMeshCommand(sb, i);
      }
      return sb.toString();
    }
    if (property.startsWith("checkID:")) {
      // returns FIRST match
      String key = property.substring(8).toUpperCase();
      boolean isWild = TextFormat.isWild(key);
      for (int i = meshCount; --i >= 0;) {
        String id = meshes[i].thisID.toUpperCase();
        if (id.equals(key) || isWild && TextFormat.isMatch(id, key, true, true))
          return id;
      }
    }
    if (property == "vertices")
      return getVertices(currentMesh);
    if (property.startsWith("getCenter:")) {
      int meshIndex = getIndexFromName(property.substring(10));
      Mesh m;
      return (meshIndex < 0 || (m = meshes[meshIndex]).vertices == null
          || m.vertexCount <= index ? null 
          : index >= 0 ? m.vertices[index] : null);

    }
    return null;
  }

 private Object getVertices(Mesh mesh) {
   if (mesh == null)
     return null;
   return mesh.vertices;
 }
 

  private void deleteMesh() {
    int i = 0;
    if (explicitID && currentMesh != null) {
      for (i = meshCount; meshes[--i] != currentMesh;) {
      }
      deleteMesh(i);
    } else {
      String key = (explicitID && previousMeshID != null
          && TextFormat.isWild(previousMeshID) ?  
              previousMeshID.toUpperCase() : null);
      if (key == null || key.length() == 0) {
        for (i = meshCount; --i >= 0; )
          meshes[i] = null;
        meshCount = 0;
        nUnnamed = 0;
      } else {
        for (i = meshCount; --i >= 0; ) {
          if (TextFormat.isMatch(meshes[i].thisID.toUpperCase(), key, true, true))
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
      return (previousMeshID == null ? meshCount - 1
          : getIndexFromName(previousMeshID));
    if (TextFormat.isWild(thisID)) {
      thisID = thisID.toLowerCase();
      for (int i = meshCount; --i >= 0;) {
        if (meshes[i] != null
            && TextFormat.isMatch(meshes[i].thisID, thisID, true, true))
          return i;
      }
    }
    else
      for (int i = meshCount; --i >= 0;) {
        if (meshes[i] != null && thisID.equalsIgnoreCase(meshes[i].thisID))
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
    StringBuffer sb = new StringBuffer("\n");
    for (int i = 0; i < meshCount; i++)
      getMeshCommand(sb, i);
    return sb.toString();
  }

 private void getMeshCommand(StringBuffer sb, int i) {
   Mesh mesh = meshes[i];
   String cmd = mesh.scriptCommand;
   if (cmd == null)
     return;
   cmd = cmd.replace('\t',' ');
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
   if (cmd.toLowerCase().indexOf(" ID ") < 0 && !myType.equals("mo"))
       cmd += "# ID=\"" + mesh.thisID + "\"";
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
     appendCmd(sb, "frame " + viewer.getModelNumberDotted(mesh.modelIndex));
   appendCmd(sb, cmd);
   if (cmd.charAt(0) != '#') {
     if (allowMesh)
     appendCmd(sb, mesh.getState(myType));
     if (mesh.colorCommand != null) {
       if (!mesh.isColorSolid && Graphics3D.isColixTranslucent(mesh.colix))
         appendCmd(sb, getColorCommand(myType, mesh.colix));
       appendCmd(sb, mesh.colorCommand);
     }
     if (mesh.isColorSolid)
       appendCmd(sb, getColorCommand(myType, mesh.colix));
   }
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

 