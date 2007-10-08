/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.export;

import java.util.BitSet;

import org.jmol.shapespecial.*;

public class IsosurfaceGenerator extends IsosurfaceRenderer {

  private _Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (_Exporter)exporter;
  }

  protected void renderPoints() {
    int incr = imesh.vertexIncrement;
    for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0 : imesh.firstRealVertex); i < vertexCount; i += incr) {
      if (vertexValues != null && Float.isNaN(vertexValues[i]) || frontOnly
          && transformedVectors[normixes[i]].z < 0)
        continue;
      if (imesh.vertexColixes != null)
      exporter.fillSphereCentered(4, vertices[i], imesh.vertexColixes[i]);
    }
  }

  protected void renderTriangles(boolean fill, boolean iShowTriangles) {
    int[][] polygonIndexes = imesh.polygonIndexes;
    short colix = imesh.colix;
    short[] vertexColixes = imesh.vertexColixes;
    short hideColix = 0;
    BitSet bsFaces = new BitSet();
    try {
      hideColix = vertexColixes[imesh.polygonIndexes[0][0]];
    } catch (Exception e) {
    }
    //System.out.println("Isosurface renderTriangle polygoncount = "
    //  + mesh.polygonCount + " screens: " + screens.length + " normixes: "
    //+ normixes.length);
    // two-sided means like a plane, with no front/back distinction
    for (int i = imesh.polygonCount; --i >= 0;) {
      //if (!mesh.isPolygonDisplayable(i))
      // continue;
      //if (i !=20)
      //continue;
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
      if (frontOnly && transformedVectors[nA].z < 0
          && transformedVectors[nB].z < 0 && transformedVectors[nC].z < 0)
        continue;
      short colixA, colixB, colixC;
      if (vertexColixes != null) {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
        if (isBicolorMap && (colixA != colixB || colixB != colixC))
          continue;
      } else {
        colixA = colixB = colixC = colix;
      }
      if (iHideBackground) {
        if (colixA == hideColix && colixB == hideColix && colixC == hideColix)
          continue;
        if (colixA == hideColix)
          colixA = backgroundColix;
        if (colixB == hideColix)
          colixB = backgroundColix;
        if (colixC == hideColix)
          colixC = backgroundColix;
      }
      bsFaces.set(i);
    }
    exporter.renderIsosurface(imesh.vertices, imesh.colix, imesh.vertexColixes,
        imesh.normixes, imesh.polygonIndexes, bsFaces, imesh.vertexCount,
        imesh.polygonCount);
  }
}
