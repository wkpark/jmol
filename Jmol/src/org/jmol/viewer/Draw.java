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

package org.jmol.viewer;


import java.util.BitSet;
import javax.vecmath.Point3f;

class Draw extends MeshCollection {

  Point3f[] ptList = new Point3f[8];
  int[] ptIdentifiers = new int[8];
  BitSet[] ptBitSets = new BitSet[8];
  Point3f xyz = new Point3f();
  int ipt;
  int npoints = -1;
  int nbitsets = 0;
  int ncoord = 0;
  int nidentifiers = 0;
  boolean isFixed = false;
  float newScale;
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    System.out.println("draw "+propertyName+" "+value);

    if ("fixed" == propertyName) {
      isFixed = ((Boolean)value).booleanValue();
      return;
    }
    
    if ("points" == propertyName) {
      ipt = npoints = ncoord = nbitsets = nidentifiers = 0;
      newScale = ((Integer)value).floatValue()/100;
      if (newScale == 0)
        newScale = 1;
      return;
    }
    
    if ("scale" == propertyName) {
      newScale = ((Integer)value).floatValue()/100;
      if (newScale == 0)
        newScale = 0.01f; //very tiny but still sizable;
      if (currentMesh != null) {
        //no points in this script statement
        currentMesh.scaleDrawing(newScale);
        currentMesh.initialize();
      }
      return;
    }
    
    if ("identifier" == propertyName) {
      String meshID = (String)value;
      int meshIndex = getMeshIndex(meshID);
      if (meshIndex >= 0) {
        ptIdentifiers[nidentifiers++] = meshIndex;
        npoints++;
      } else {
        System.out.println("draw identifier " + value + " not found");
      }
      return;
    }
    
    if ("coord" == propertyName) {
      float x = ((Float)value).floatValue();
      if (ipt == 0) {
        xyz.x = x; 
      } else if (ipt == 1) {
        xyz.y = x; 
      } else if (ipt == 2) {
        xyz.z = x; 
        ptList[ncoord++] = new Point3f(xyz);
        npoints++;
        System.out.println(npoints + " " + ptList[ncoord-1]);
      }
      ipt = (ipt + 1) % 3;
      return;
    }
    if ("atomSet" == propertyName) {
      if (((BitSet)value).cardinality() == 0)
        return;
      ptBitSets[nbitsets++] = (BitSet)value;
      npoints++;
      System.out.println(npoints + " " + ptBitSets[nbitsets-1]);
      return;
    } 
    if ("set" == propertyName) {
      isValid = setDrawing();
      if(isValid) {
        currentMesh.scaleDrawing(newScale);
        currentMesh.initialize();
        currentMesh.setAxes();
        currentMesh.visible = true;
      }
      npoints = -1; //for later scaling
      return;
    }
    if ("meshID" == propertyName) {
      npoints = -1;
      isFixed = false;
    }
      
    super.setProperty(propertyName, value, bs);
  }

  boolean setDrawing() {
    if (currentMesh == null)
      allocMesh(null);
    currentMesh.clear("draw");
    if (npoints == 0)
      return false;
    int nPoly = 0;
    int modelCount = viewer.getModelCount();
    if (nbitsets == 0 && nidentifiers == 0 || modelCount < 2) 
      isFixed = true;
    if (isFixed) {
      currentMesh.setPolygonCount(1);
      currentMesh.ptCenters = null;
      currentMesh.visibilityFlags = null;
      nPoly = setVerticesAndPolygons(-1, nPoly);
    } else {
      currentMesh.setPolygonCount(modelCount);
      currentMesh.ptCenters = new Point3f[modelCount];
      currentMesh.visibilityFlags = new int[modelCount];
      for (int iModel = 0; iModel < modelCount; iModel++) {
        int n0 = currentMesh.vertexCount;
        nPoly = setVerticesAndPolygons(iModel, nPoly);
        currentMesh.setCenter(iModel);
      }
    }
    currentMesh.setCenter(-1);
    return true;
  }

  int setVerticesAndPolygons(int iModel, int nPoly) {    
    int nPoints = ncoord;
    // [x,y,z] points are already defined in ptList
    if (iModel < 0) {
      // add in [drawID] references as overall centers
      for (int i = 0; i < nidentifiers; i++)
        ptList[nPoints++] = meshes[ptIdentifiers[i]].ptCenter;
      // add in (atom set) references as overall centers
      for (int i = 0; i < nbitsets; i++)
        ptList[nPoints++] = viewer.getAtomSetCenter(ptBitSets[i]);
    } else {
      // [drawID] references may be fixed or not
      for (int i = 0; i < nidentifiers; i++) {
        if (meshes[ptIdentifiers[i]].ptCenters == null ||
            meshes[ptIdentifiers[i]].ptCenters[iModel] == null) {
          ptList[nPoints++] = meshes[ptIdentifiers[i]].ptCenter;          
        } else {
          ptList[nPoints++] = meshes[ptIdentifiers[i]].ptCenters[iModel];
        }
      }
      // (atom set) references must be filtered for relevant model
      // note that if a model doesn't have a relevant point, one may
      // get a line instead of a plane, a point instead of a line, etc.
      BitSet bsModel = viewer.getModelAtomBitSet(iModel);
      for (int i = 0; i < nbitsets; i++) {
        BitSet bs = (BitSet)ptBitSets[i].clone();
        bs.and(bsModel);
        if (bs.cardinality() > 0) {
          ptList[nPoints++] = viewer.getAtomSetCenter(bs);
        }
      }
    }
    return currentMesh.setPolygon(ptList, nPoints, nPoly);
  }

  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * note that this is NOT done with atoms and bonds, because they have mads.
     * When you say "frame 0" it is just turning on all the mads.
     */
    int modelCount = viewer.getModelCount();
    for (int i = meshCount; --i >= 0; ) {
      if (meshes[i].visibilityFlags == null)
        continue;
      for (int iModel = modelCount; --iModel >= 0; )
        meshes[i].visibilityFlags[iModel] = (bs.get(iModel) ? 1 : 0); 
    }
  } 
}
