/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;

class Trace extends Mps {

  Mps.Mpspolymer allocateMpspolymer(Polymer polymer) {
    return new Tchain(polymer);
  }

  class Tchain extends Mps.Mpspolymer {
    Tchain(Polymer polymer) {
      super(polymer, 600, 1500, 500, 1500);
    }
    
    // Bob Hanson 11/03/2006 - first attempt at mesh rendering of 
    // secondary structure.
    // mesh creation occurs at rendering time, because we don't
    // know what all the options are, and they aren't important,
    // until it gets rendered, if ever
 
    boolean isNucleic;
    Point3f[] controlHermites;
    
    void createMeshes(Point3f[] controlPoints) {
      isNucleic = polymer instanceof NucleicPolymer;
      meshes = new Mesh[monomerCount];
      meshReady = new boolean[monomerCount];
      for (int i = 0; i < monomerCount; i++)
         createMesh(i, controlPoints);
    }
    
    final Vector3f Z = new Vector3f(0.1345f,0.5426f,0.3675f); //random reference
    final Vector3f wing = new Vector3f();
    final Vector3f wing0 = new Vector3f();
    final AxisAngle4f aa = new AxisAngle4f();
    final Point3f pt = new Point3f();
    final Point3f pt0 = new Point3f();
    final Matrix3f mat = new Matrix3f();
    
    void createMesh(int i, Point3f[] controlPoints) {
      int nHermites = 9; // ???
      int nPer = 16;
      Mesh mesh = meshes[i] = new Mesh(viewer, "trace_" + i, g3d, colixes[i]);
      int iPrev = Math.max(i - 1, 0);
      int iNext = Math.min(i + 1, monomerCount);
      int iNext2 = Math.min(i + 2, monomerCount);
      float radius1 = this.mads[i] / 1000f;
      float radius2 = this.mads[iNext] / 1000f;
      float dr = (radius2 - radius1)/(nHermites-1);
      //System.out.println("radius"+radius1 + " " + radius2 + " " + dr);

      controlHermites = new Point3f[nHermites];
      Graphics3D.getHermiteList(isNucleic ? 4 : 7, controlPoints[iPrev],
          controlPoints[i], controlPoints[iNext], controlPoints[iNext2],
          controlHermites);
      Vector3f norm = new Vector3f();
      int nPoints = 0;
      norm.sub(controlHermites[1], controlHermites[0]);
      wing0.cross(norm, Z);
      wing0.cross(norm,wing0);
      for (int p = 0; p < nHermites - 1; p++) {
        norm.sub(controlHermites[p + 1], controlHermites[p]);
        wing.cross(norm, wing0);
        wing.normalize();
        wing.scale(radius1 + dr*p);
        aa.set(norm, (float) (2 * Math.PI / nPer));
        mat.set(aa);
        pt0.set(controlHermites[p]);
        Point3f pt2 = new Point3f(pt0);
        pt2.add(wing);
        //if (i==20)
          //System.out.print("draw line_"+p+" "+" {"+pt0.x+" "+pt0.y+" "+pt0.z+"} {"+pt2.x+" "+pt2.y+" "+pt2.z+"};");
        for (int k = 0; k < nPer; k++) {
          pt.set(pt0);
          mat.transform(wing);
          pt.add(wing);
          mesh.addVertexCopy(pt);
          //if (i == 20)
            //System.out.print("draw pt"+p+"_"+k+" {"+pt.x+" "+pt.y+" "+pt.z+"};");
        }
       
        if (p > 0) {
          for (int k = 0; k < nPer; k++) {
            mesh.addQuad(nPoints - nPer + k, 
                nPoints - nPer + ((k + 1) % nPer), 
                nPoints + ((k + 1) % nPer), nPoints + k);
          }
        }
        nPoints += nPer;
      }
      mesh.initialize();
      mesh.colix = colixes[i];
      meshReady[i] = true;
      mesh.visibilityFlags = 1;
    }
  }
}