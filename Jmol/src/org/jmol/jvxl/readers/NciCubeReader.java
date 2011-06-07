/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.io.BufferedReader;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.Interface;
import org.jmol.api.MOCalculationInterface;

class NciCubeReader extends CubeReader {

  /* reads an electron density cube file and does a 
   * discrete calculation for reduced electron density and
   * Hessian processing.
   * 
   * Bob Hanson hansonr@stolaf.edu  6/7/2011
   *  
   */
  NciCubeReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    hasColorData = true;
  }
  
  private MOCalculationInterface q;
  
  private float[][] yzPlanesRaw;
  private int nPlanesRaw;
  
  @Override
  public void getPlane(int x) {
    if (nPlanesRaw == 0) {
      initPlanes();
      q = (MOCalculationInterface) Interface
          .getOptionInterface("quantum.NciCalculation");
      q.setupCalculation(volumeData, null, null, null, -1, null, null, null,
          null, null, null, null, null, false, null, params.parameters);
      q.setPlanes(yzPlanesRaw = new float[4][yzCount]);
      getPlane(yzPlanesRaw[0]);
      getPlane(yzPlanesRaw[1]);
      nPlanesRaw = 2;
      for (int i = 0; i < yzCount; i++)
        yzPlanes[0][i] = 100;
      return;
    }
    if (nPlanesRaw == 4) {
      float[] plane = yzPlanesRaw[0];
      yzPlanesRaw[0] = yzPlanesRaw[1];
      yzPlanesRaw[1] = yzPlanesRaw[2];
      yzPlanesRaw[2] = yzPlanesRaw[3];
      yzPlanesRaw[3] = plane;
      getPlane(yzPlanesRaw[3]);
    } else {
      getPlane(yzPlanesRaw[nPlanesRaw++]);
    }
    q.calcPlane(yzPlanes[x % 2]);
  }
  
  @Override
  protected float getSurfacePointAndFraction(float cutoff,
                                             boolean isCutoffAbsolute,
                                             float valueA, float valueB,
                                             Point3f pointA,
                                             Vector3f edgeVector, int x, int y,
                                             int z, int vA, int vB,
                                             float[] fReturn, Point3f ptReturn) {
      
      float zero = super.getSurfacePointAndFraction(cutoff, isCutoffAbsolute, valueA,
          valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
      vA = marchingCubes.getLinearOffset(x, y, z, vA);
      vB = marchingCubes.getLinearOffset(x, y, z, vB);
      return (q == null || Float.isNaN(zero) ? zero : q.process(vA, vB, fReturn[0]));
  }

}


