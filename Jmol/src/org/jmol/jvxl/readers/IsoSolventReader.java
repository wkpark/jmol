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

import java.util.ArrayList;
import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

//import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
//import org.jmol.util.TextFormat;

import org.jmol.api.AtomIndexIterator;
import org.jmol.jvxl.data.MeshData;

class IsoSolventReader extends AtomDataReader {

  IsoSolventReader(SurfaceGenerator sg) {
    super(sg);
  }

  ///// solvent-accessible, solvent-excluded surface //////

  /*
   * The surface fragment idea:
   * 
   * isosurface solvent|sasurface both work on the SELECTED atoms, thus
   * allowing for a subset of the molecule to be involved. But in that
   * case we don't want to be creating a surface that goes right through
   * another atom. Rather, what we want (probably) is just the portion
   * of the OVERALL surface that involves these atoms. 
   * 
   * The addition of Mesh.voxelValue[] means that we can specify any 
   * voxel we want to NOT be excluded (NaN). Here we first exclude any 
   * voxel that would have been INSIDE a nearby atom. This will take care
   * of any portion of the vanderwaals surface that would be there. Then
   * we exclude any special-case voxel that is between two nearby atoms. 
   *  
   *  Bob Hanson 13 Jul 2006
   *  
   * isosurface SOLVENT 1.4 FULL
   * 
   * The FULL option, introduced in Jmol 12.1.24 (11/26/2010):
   * 
   * Prior to Jmol 12.1.24, all isosurface SOLVENT/MOLECULAR calculations
   * only checked for pairs of atoms in calculating troughs. This was
   * sufficient for most work, but a full analysis of molecular surfaces
   * requires that the "ball" rolling around a pair of atoms 
   * may hit a third atom. If this is the case, then the surface area will
   * be somewhat less, and the valleys produced will be shallower.
   * The full analysis option checks for all possible triples and can 
   * take significant calculation time. The results are rewarding, and 
   * the saving of the JVXL equivalent is recommended.
   * 
   * I don't know how PyMOL does it, but its "show surface" command is
   * very fast, though not as exact. Most probably it is a massaged dot surface, similar
   * to spacefill but with a rough additional "surface smoothing" applied.
   * That surface seems to be somewhat smaller than Jmol's, perhaps due to the use of 
   * smaller radii for the atoms, but I am not sure. If it is a modified dot 
   * surface, that would explain its speed. Certainly an interesting idea. 
   * It might be generated in Jmol using a variation on the geosurface idea. 
   * Start with surface dots (very fast) and then adjust them to their proper
   * surface location. Hidden dots could then be removed. I wonder....
   *  
   *  Bob Hanson, 11/26/2010
   *  
   */

  private float cavityRadius;
  private float envelopeRadius;

  private boolean doCalculateTroughs;
  private boolean isCavity, isPocket;
  private float solventRadius;
  
  @Override
  protected void setup() {
    super.setup();
    cavityRadius = params.cavityRadius;
    envelopeRadius = params.envelopeRadius;
    solventRadius = params.solventRadius;
    point = params.point;

    isCavity = (params.isCavity && meshDataServer != null); // Jvxl cannot do this calculation on its own.
    isPocket = (params.pocket != null && meshDataServer != null);

    doCalculateTroughs = (atomDataServer != null && !isCavity // Jvxl needs an atom iterator to do this.
        && solventRadius > 0 && (dataType == Parameters.SURFACE_SOLVENT || dataType == Parameters.SURFACE_MOLECULAR));
    doUseIterator = doCalculateTroughs;
    getAtoms(Float.NaN, false, true);
    if (isCavity || isPocket)
      meshData.dots = meshDataServer.calculateGeodesicSurface(bsMySelected,
          envelopeRadius);

    setHeader("solvent/molecular surface", params.calculationType);
    setRangesAndAddAtoms(params.solvent_ptsPerAngstrom, params.solvent_gridMax,
        params.thePlane != null ? Integer.MAX_VALUE : Math.min(firstNearbyAtom,
            100));
  }

  //////////// meshData extensions ////////////

  @Override
  public void selectPocket(boolean doExclude) {
    if (meshDataServer == null)
      return; //can't do this without help!
    meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    //mark VERTICES for proximity to surface
    Point3f[] v = meshData.vertices;
    int nVertices = meshData.vertexCount;
    float[] vv = meshData.vertexValues;
    Point3f[] dots = meshData.dots;
    int nDots = dots.length;
    for (int i = 0; i < nVertices; i++) {
      for (int j = 0; j < nDots; j++) {
        if (dots[j].distance(v[i]) < envelopeRadius) {
          vv[i] = Float.NaN;
          continue;
        }
      }
    }
    meshData.getSurfaceSet();
    int nSets = meshData.nSets;
    BitSet pocketSet = new BitSet(nSets);
    BitSet ss;
    for (int i = 0; i < nSets; i++)
      if ((ss = meshData.surfaceSet[i]) != null)
        for (int j = ss.nextSetBit(0); j >= 0; j = ss.nextSetBit(j + 1))
          if (Float.isNaN(meshData.vertexValues[j])) {
            pocketSet.set(i);
            //System.out.println("pocket " + i + " " + j + " " + surfaceSet[i]);
            break;
          }
    //now clear all vertices that match the pocket toggle
    //"POCKET"   --> pocket TRUE means "show just the pockets"
    //"INTERIOR" --> pocket FALSE means "show everything that is not a pocket"
    for (int i = 0; i < nSets; i++)
      if (meshData.surfaceSet[i] != null && pocketSet.get(i) == doExclude)
        meshData.invalidateSurfaceSet(i);
    updateSurfaceData();
    if (!doExclude)
      meshData.surfaceSet = null;
    meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
    meshData = new MeshData();
  }
  
  
  /////////////// calculation methods //////////////
  
  
  @Override
  protected void generateCube() {
    /*
     * 
     * Jmol cavity rendering. Tim Driscoll suggested "filling a 
     * protein with foam. Here you go...
     * 
     * 1) Use a dot-surface extended x.xx Angstroms to define the 
     *    outer envelope of the protein.
     * 2) Identify all voxel points outside the protein surface (v > 0) 
     *    but inside the envelope (nearest distance to a dot > x.xx).
     * 3) First pass -- create the protein surface.
     * 4) Replace solvent atom set with "foam" ball of the right radius
     *    at the voxel vertex points.
     * 5) Run through a second time using these "atoms" to generate 
     *    the surface around the foam spheres. 
     *    
     *    Bob Hanson 3/19/07
     * 
     */

    volumeData.voxelData = voxelData = new float[nPointsX][nPointsY][nPointsZ];
    if (isCavity && params.theProperty != null) {
      /*
       * couldn't get this to work -- we only have half of the points
       for (int x = 0, i = 0, ipt = 0; x < nPointsX; ++x)
       for (int y = 0; y < nPointsY; ++y)
       for (int z = 0; z < nPointsZ; ++z)
       voxelData[x][y][z] = (
       bs.get(i++) ? 
       surface_data[ipt++] 
       : Float.NaN);
       mappedDataMin = 0;
       mappedDataMax = 0;
       for (int i = 0; i < nAtoms; i++)
       if (surface_data[i] > mappedDataMax)
       mappedDataMax = surface_data[i];      
       */
      return;
    }
    generateSolventCube(true);
    if (isCavity && dataType != Parameters.SURFACE_NOMAP
        && dataType != Parameters.SURFACE_PROPERTY) {
      generateSolventCavity();
      generateSolventCube(false);
    }
    if (params.cappingObject instanceof Point4f) { // had a check for mapping here, turning this off
      //Logger.info("capping isosurface using " + params.cappingPlane);
      volumeData.capData((Point4f)params.cappingObject, params.cutoff);
      params.cappingObject = null;
    }
  }

 private void generateSolventCavity() {
    //we have a ring of dots around the model.
    //1) identify which voxelData points are > 0 and within this volume
    //2) turn these voxel points into atoms with given radii
    //3) rerun the calculation to mark a solvent around these!
    BitSet bs = new BitSet(nPointsX * nPointsY * nPointsZ);
    int i = 0;
    int nDots = meshData.dots.length;
    int n = 0;
    //surface_data = new float[1000];

    // for (int j = 0; j < nDots; j++) {
    //   System.out.println("draw pt"+j+" {"+meshData.dots[j].x + " " +meshData.dots[j].y + " " +meshData.dots[j].z + "} " );
    // }
    /*
     Point3f pt = new Point3f(15.3375f, 1.0224999f, 5.1125f);
     for (int x = 0; x < nPointsX; ++x)
     for (int y = 0; y < nPointsY; ++y) {
     out: for (int z = 0; z < nPointsZ; ++z, ++i) {
     float d = voxelData[x][y][z]; 
     volumeData.voxelPtToXYZ(x, y, z, ptXyzTemp);
     if (d < Float.MAX_VALUE && d >= cavityRadius)
     if (ptXyzTemp.distance(pt) < 3f)
     System.out.println("draw pt"+(n++)+" {"+ptXyzTemp.x + " " +ptXyzTemp.y + " " +ptXyzTemp.z + "} # " +x + " " + y + " " + z + " " + voxelData[x][y][z]);
     if (false && d < Float.MAX_VALUE &&
     d >= cavityRadius) {
     volumeData.voxelPtToXYZ(x, y, z, ptXyzTemp);
     if (ptXyzTemp.x > 0 && ptXyzTemp.y > 0 && ptXyzTemp.z > 0)
     System.out.println("draw pt"+(n++)+" {"+ptXyzTemp.x + " " +ptXyzTemp.y + " " +ptXyzTemp.z + "} " );
     }
     }
     }
     n = 0;
     i = 0;
     */
    float d;
    float r2 = envelopeRadius;// - cavityRadius;

    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y) {
        out: for (int z = 0; z < nPointsZ; ++z, ++i)
          if ((d = voxelData[x][y][z]) < Float.MAX_VALUE && d >= cavityRadius) {
            volumeData.voxelPtToXYZ(x, y, z, ptXyzTemp);
            for (int j = 0; j < nDots; j++) {
              if (meshData.dots[j].distance(ptXyzTemp) < r2)
                continue out;
            }
            bs.set(i);
            n++;
          }
      }
    Logger.info("cavities include " + n + " voxel points");
    atomRadius = new float[n];
    atomXyz = new Point3f[n];
    for (int x = 0, ipt = 0, apt = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          if (bs.get(ipt++)) {
            volumeData.voxelPtToXYZ(x, y, z, (atomXyz[apt] = new Point3f()));
            atomRadius[apt++] = voxelData[x][y][z];
          }
    myAtomCount = firstNearbyAtom = n;
  }

  final Point3f ptXyzTemp = new Point3f();
  private int n1 = 0;
  private StringBuffer sbTest = new StringBuffer();


  void generateSolventCube(boolean isFirstPass) {
    float distance = params.distance;
    float rA, rB;
    Point3f ptA;
    Point3f ptY0 = new Point3f(), ptZ0 = new Point3f();
    Point3i pt0 = new Point3i(), pt1 = new Point3i();
    boolean havePlane = (params.thePlane != null);
    float value = Float.MAX_VALUE;
    if (Logger.debugging)
      Logger.startTimer();
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          voxelData[x][y][z] = value;
    if (dataType == Parameters.SURFACE_NOMAP)
      return;
    int atomCount = myAtomCount;
    float maxRadius = 0;
    float r0 = (isFirstPass && isCavity ? cavityRadius : 0);
    boolean isWithin = (isFirstPass && distance != Float.MAX_VALUE && point != null);
    //true ==> only atom index > this atom accepted
    for (int iAtom = 0; iAtom < atomCount; iAtom++) {
      ptA = atomXyz[iAtom];
      rA = atomRadius[iAtom];
      if (rA > maxRadius)
        maxRadius = rA;
      if (isWithin && ptA.distance(point) > distance + rA + 0.5)
        continue;
      boolean isNearby = (iAtom >= firstNearbyAtom);
      setGridLimitsForAtom(ptA, rA + r0, pt0, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      for (int i = pt0.x; i < pt1.x; i++) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++) {
            float v = ptXyzTemp.distance(ptA) - rA;
            if (v < voxelData[i][j][k])
              voxelData[i][j][k] = (isNearby || isWithin
                  && ptXyzTemp.distance(point) > distance ? Float.NaN : v);
            ptXyzTemp.add(volumetricVectors[2]);
          }
          ptXyzTemp.set(ptZ0);
          ptXyzTemp.add(volumetricVectors[1]);
        }
        ptXyzTemp.set(ptY0);
        ptXyzTemp.add(volumetricVectors[0]);
      }
    }
    if (isCavity && isFirstPass)
      return;
    if (doCalculateTroughs) {
      Point3i ptA0 = new Point3i();
      Point3i ptB0 = new Point3i();
      Point3i ptA1 = new Point3i();
      Point3i ptB1 = new Point3i();
      volumeData.getYzCount();
      ArrayList<float[]> troughList1 = new ArrayList<float[]>();
      float dist2 = 2 * (solventRadius + maxRadius);
      Point3f pt = new Point3f();
      Vector3f vTemp = new Vector3f();
      Vector3f vTemp2 = new Vector3f();
      Point4f plane = new Point4f();
      if (params.doFullMolecular) {
        float v;
        float vmin = -1.5f * volumeData.minGrid;
        AtomIndexIterator iter = atomDataServer.getSelectedAtomIterator(
            bsMySelected, false, false); // was TRUE TRUE
        for (int i = 0; i < volumeData.nPoints; i++) {
          v = volumeData.getVoxelData(i);
          // only select 
          if (!havePlane && v < vmin)
            continue;
          volumeData.getPoint(i, pt);
          if (isWithin && pt.distance(point) > distance)
            continue;
          troughList1.clear();

          atomDataServer.setIteratorForPoint(iter, modelIndex, pt, dist2);
          while (iter.hasNext()) {
            int iatomB = iter.next();
            Point3f ptB = atomXyz[myIndex[iatomB]];
            rB = atomData.atomRadius[iatomB] + solventRadius;
            if (isWithin && ptB.distance(point) > distance + rB + 0.5)
              continue;
            if (havePlane
                && Math.abs(volumeData.distancePointToPlane(ptB)) > 2 * rB)
              continue;
            float d = (float) Math.sqrt(iter.foundDistance2());
            float[] a = new float[] { iatomB, rB, d };
            troughList1.add(a);
          }
          int n = troughList1.size();
          for (int ia = 0; ia < n - 1 && (havePlane || v >= vmin); ia++) {
            float[] af = troughList1.get(ia);
            int iatomA = (int) af[0];
            float rAS = af[1];
            ptA = atomXyz[myIndex[iatomA]];
            for (int ib = ia + 1; ib < n && (havePlane || v >= vmin); ib++) {
              float[] bf = troughList1.get(ib);
              int iatomB = (int) bf[0];
              float rBS = bf[1];
              Point3f ptB = atomXyz[myIndex[iatomB]];
              float dAB = ptA.distance(ptB);
              if (dAB >= rAS + rBS)
                continue;
              float dVS = checkSpecialVoxel(ptA, rAS, ptB, rBS, dAB, pt);
              if (Float.isNaN(dVS))
                continue;
              float v1 = solventRadius - dVS;
              // question here is whether or not there is a third atom to worry about
              //if (v > 0) // only when outside?
              for (int ic = 0; ic < n  && (havePlane || v1 >= vmin); ic++) {
                float[] cf = troughList1.get(ic);
                int iatomC = (int) cf[0];
                if (iatomC == iatomA || iatomC == iatomB)
                  continue;
                float rCS = cf[1];
                Point3f ptC = atomXyz[myIndex[iatomC]];
                if (ptC.distance(ptA) >= rAS + rCS
                    || ptC.distance(ptB) >= rBS + rCS)
                  continue;
                n1++;
                v1 = checkSpecialVoxel(ptA, rAS, ptB, ptC, rCS, pt, plane,
                    vTemp, vTemp2, v1);
              }
              if (v1 < v)
                volumeData.setVoxelData(i, v = v1);
            }

            //                System.out.println(i + " " + iatomA + " " + iatomB + " " + troughList.size());            
          }

        }
        Logger.info("isosurface molecular (pairs and triples) nPoints=" + volumeData.nPoints + " nTests="
            + n1);
        iter.release();
        iter = null;
      } else {
        AtomIndexIterator iter = atomDataServer.getSelectedAtomIterator(
            bsMySelected, true, false); //
        for (int iAtom = 0; iAtom < firstNearbyAtom - 1; iAtom++)
          if (atomNo[iAtom] > 0) {
            ptA = atomXyz[iAtom];
            rA = atomRadius[iAtom] + solventRadius;
            int iatomA = atomIndex[iAtom];
            if (isWithin && ptA.distance(point) > distance + rA + 0.5)
              continue;
            setGridLimitsForAtom(ptA, atomRadius[iAtom], ptA0, ptA1);
            // iterating over all atoms within rA + 2*rSolvent + rMax
            atomDataServer.setIteratorForAtom(iter, iatomA, rA + solventRadius
                + maxRadius);
            while (iter.hasNext()) {
              int iatomB = iter.next();
              Point3f ptB = atomXyz[myIndex[iatomB]];
              rB = atomData.atomRadius[iatomB] + solventRadius;
              if (isWithin && ptB.distance(point) > distance + rB + 0.5)
                continue;
              if (havePlane
                  && Math.abs(volumeData.distancePointToPlane(ptB)) > 2 * rB)
                continue;
              float dAB = ptA.distance(ptB);
              if (dAB >= rA + rB)
                continue;

              // two atoms have been found 
              // now we scan for points near them. 

              //defining pt0 and pt1 very crudely -- this could be refined
              setGridLimitsForAtom(ptB, rB - solventRadius, ptB0, ptB1);
              pt0.x = Math.min(ptA0.x, ptB0.x);
              pt0.y = Math.min(ptA0.y, ptB0.y);
              pt0.z = Math.min(ptA0.z, ptB0.z);
              pt1.x = Math.max(ptA1.x, ptB1.x);
              pt1.y = Math.max(ptA1.y, ptB1.y);
              pt1.z = Math.max(ptA1.z, ptB1.z);
              volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
              for (int i = pt0.x; i < pt1.x; i++) {
                ptY0.set(ptXyzTemp);
                for (int j = pt0.y; j < pt1.y; j++) {
                  ptZ0.set(ptXyzTemp);
                  for (int k = pt0.z; k < pt1.z; k++) {
                    n1++;
                    float dVS = (isWithin
                        && ptXyzTemp.distance(point) > distance ? Float.NaN
                        : checkSpecialVoxel(ptA, rA, ptB, rB, dAB, ptXyzTemp));
                    if (!Float.isNaN(dVS)) {
                      float v = solventRadius - dVS;
                      if (v < voxelData[i][j][k]) {
                        voxelData[i][j][k] = v;
                      }
                    }
                    ptXyzTemp.add(volumetricVectors[2]);
                  }
                  ptXyzTemp.set(ptZ0);
                  ptXyzTemp.add(volumetricVectors[1]);
                }
                ptXyzTemp.set(ptY0);
                ptXyzTemp.add(volumetricVectors[0]);
              }
            }
          }
        iter.release();
        iter = null;
        Logger.info("isosurface molecular (pairs only) nPoints=" + volumeData.nPoints + " nTests="
            + n1);
      }
    }
    if (!havePlane) {
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] == Float.MAX_VALUE)
              voxelData[x][y][z] = Float.NaN;
    } else { //solvent planes just focus on negative values
      value = 0.001f;
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] < value) {
              // Float.NaN will also match ">=" this way
            } else {
              voxelData[x][y][z] = value;
            }
    }
    if (Logger.debugging)
      Logger.checkTimer("solvent surface time");
    if (sbTest.length() > 0)
      Logger.info(sbTest.toString());
  }

  private boolean done;
  private Point3f ptTemp2 = new Point3f();
  private Point3f ptS1 = new Point3f();
  private Point3f ptS2 = new Point3f();
  private Vector3f vTemp2 = new Vector3f();
  private Vector3f vTemp3 = new Vector3f();
  
  private float checkSpecialVoxel(Point3f ptA, float rAS, Point3f ptB, Point3f ptC, float rCS,
                                  Point3f pt, Point4f planeTemp, Vector3f vTemp, Vector3f vXS, 
                                  float v1) {
    /*
     * 
     * A----------p-----B
     *       pt  /|\
     *          / | \ 
     *         /  |  \
     *        S'--X---S  (both in plane perp to vAB through point p)
     *         \  |  / .
     *          \ | /   . rCS
     *           \|/     .
     *            T------C (T is projection of C onto plane perp to vAB)
     *               dCT
     * We want ptS such that 
     *   rAS = rA + rSolvent, 
     *   rBS = rB + rSolvent, and 
     *   rCS = rC + rSolvent
     * 
     * 1) define plane perpendicular to A-B axis and containing ptS
     * 2) project C onto plane as ptT
     * 3) calculate two possible ptS and ptS' in this plane
     *      or return if not applicable
     * 4) find closer ptS to pt
     * 5) return new distance only if pt is within the tetrahedron ABCS
     * 
     * isosurface solvent 1.4 FULL # Jmol 12.1.24  11/26/2010 RMH
     * 
     */
    
    if (done)
      return v1;
    
    //     * 1) define plane perpendicular to A-B axis and containing ptS

    vTemp.set(ptB);
    vTemp.sub(ptA);
    vTemp.normalize();
    
    // note -- depending upon which side of the centerline we are on, 
    //         ptAB may be ptA or ptB.
    
    p.scaleAdd((float) (cosAngleBAS * rS), vTemp, ptAB);
    double dpS = Math.sin(angleBAS) * rS;
    Measure.getPlaneThroughPoint(p, vTemp, planeTemp);
    
    //    * 2) project C onto plane as ptT

    float dCT = Measure.distanceToPlane(planeTemp, ptC);
    if (Math.abs(dCT) > rCS)
      return v1;
    ptTemp.scaleAdd(-dCT, vTemp, ptC);
    double dpT = p.distance(ptTemp);
    double dST = Math.sqrt(rCS * rCS - dCT * dCT);
    float dsp2 = (float) (dpS * dpS);
    double cosTheta = (dsp2 + dpT * dpT - dST * dST) / (2 * dpS * dpT);
    if (cosTheta < 0.01f || cosTheta > 1)
      return v1;
    
    //    * 3) calculate two possible ptS and ptS' in this plane

    vXS.set(pt);
    vXS.sub(p);
    vXS.normalize();
    ptS1.scaleAdd((float)dpS, vXS, p);
    float dpX = (float) (dpS * cosTheta);
    vXS.set(ptTemp);
    vXS.sub(p);
    vXS.normalize();
    ptTemp.scaleAdd(dpX, vXS, p);
    float d = Measure.distanceToPlane(planeTemp, pt);
    ptTemp2.scaleAdd(-d, vTemp, pt);
    vTemp2.set(ptTemp2);
    vTemp2.sub(p);
    if (vTemp2.dot(vXS) < 0)
      return v1;    
    vXS.cross(vTemp, vXS);
    vXS.normalize();
    float dXS = (float) (Math.sqrt(1 - cosTheta * cosTheta) * dpS);
    vXS.scale(dXS);
    ptS1.set(ptTemp);
    ptS1.add(vXS);
    ptS2.set(ptTemp);
    ptS2.sub(vXS);

    //    * 4) find closer ptS to pt

    d = pt.distance(ptS1);
    float d2 = pt.distance(ptS2);
    if (d2 < d) {
      ptS1.set(ptS2);
      d = d2;
    }
    
    //    * 5) return new distance only if pt is within the tetrahedron ABCS

    return (Measure.isInTetrahedron(pt, ptA, ptB, ptC, ptS1, planeTemp, vTemp, vTemp2, vTemp3) ? solventRadius - d : v1);
  }

//  private void addTestString(String strXXX, String s) {
//    if (s != null && sbTest.indexOf(s) >= 0)
//      return;
//    sbTest.append(s == null ? strXXX : TextFormat.simpleReplace(strXXX, "XXX", s));
//  }

  void setGridLimitsForAtom(Point3f ptA, float rA, Point3i pt0, Point3i pt1) {
    int n = 1;
    volumeData.xyzToVoxelPt(ptA.x - rA, ptA.y - rA, ptA.z - rA, pt0);
    pt0.x -= n;
    pt0.y -= n;
    pt0.z -= n;
    if (pt0.x < 0)
      pt0.x = 0;
    if (pt0.y < 0)
      pt0.y = 0;
    if (pt0.z < 0)
      pt0.z = 0;
    volumeData.xyzToVoxelPt(ptA.x + rA, ptA.y + rA, ptA.z + rA, pt1);
    pt1.x += n + 1;
    pt1.y += n + 1;
    pt1.z += n + 1;
    if (pt1.x >= nPointsX)
      pt1.x = nPointsX;
    if (pt1.y >= nPointsY)
      pt1.y = nPointsY;
    if (pt1.z >= nPointsZ)
      pt1.z = nPointsZ;
  }

  final Point3f p = new Point3f();

  float checkSpecialVoxel(Point3f ptA, float rAS, Point3f ptB, float rBS,
                          float dAB, Point3f ptV) {
    /*
     * Checking here for voxels that are in the situation:
     * 
     * A------)(-----S-----)(------B  (not actually linear)
     * |-----rAS-----|-----rBS-----|
     * |-----------dAB-------------|
     *         ptV
     * |--dAV---|---------dBV------|
     *
     * A and B are the two atom centers; S is a hypothetical
     * PROJECTED solvent center based on the position of ptV 
     * in relation to first A, then B.
     * 
     * Where the projected solvent location for one voxel is 
     * within the solvent radius sphere of another, this voxel should
     * be checked in relation to solvent distance, not atom distance.
     * 
     * aa           bb
     *   aaa      bbb
     *      aa  bb
     *         S
     *+++    /  a\    +++
     *   ++ /  | ap ++
     *     +*  V  *aa     x     want V such that angle ASV < angle ASB
     *    /  *****  \
     *   A --+--+----B
     *        b
     * 
     *  ++   the van der Waals radius for each atom.
     *  aa   the extended solvent radius for atom A.
     *  bb   the extended solvent radius for atom B.
     *  p    the projection of voxel V onto aaaaaaa.  
     *  **   the key "trough" location. 
     *  
     *  The objective is to calculate dSV only when V
     *  is within triangle ABS.
     *  
     * Getting dVS:
     * 
     * Known: rAB, rAS, rBS, giving angle BAS (theta)
     * Known: rAB, rAV, rBV, giving angle VAB (alpha)
     * Determined: angle VAS (theta - alpha), and from that, dSV, using
     * the cosine law:
     * 
     *   a^2 + b^2 - 2ab Cos(theta) = c^2.
     * 
     * The trough issue:
     * 
     * Since the voxel might be at point x (above), outside the
     * triangle, we have to test for that. What we will be looking 
     * for in the "trough" will be that angle ASV < angle ASB
     * that is, cosASB < cosASV, for each point p within bbbbb.
     * 
     * If we find the voxel in the "trough", then we set its value to 
     * (solvent radius - dVS).
     * 
     */
    float dAV = ptA.distance(ptV);
    float dBV = ptB.distance(ptV);
    float dVS;
    float f = rAS / dAV;
    if (f > 1) {
      // within solvent sphere of atom A
      // calculate point on solvent sphere aaaa projected through ptV
      p.set(ptA.x + (ptV.x - ptA.x) * f, ptA.y + (ptV.y - ptA.y) * f, ptA.z
          + (ptV.z - ptA.z) * f);
      // If the distance of this point to B is less than the distance
      // of S to B, then we need to check this point
      if (ptB.distance(p) >= rBS)
        return Float.NaN;
      // we are somewhere in the arc SAB, within the solvent sphere of A
      dVS = solventDistance(rAS, rBS, dAB, dAV, dBV);
      ptAB = ptA;
      rS = rBS;
      return (voxelIsInTrough(dVS, rAS * rAS, rBS, dAB, dAV) ? dVS : Float.NaN);
    }
    if ((f = rBS / dBV) > 1) {
      // calculate point on solvent sphere B projected through ptV
      p.set(ptB.x + (ptV.x - ptB.x) * f, ptB.y + (ptV.y - ptB.y) * f, ptB.z
          + (ptV.z - ptB.z) * f);
      if (ptA.distance(p) >= rAS)
        return Float.NaN;
      // we are somewhere in the triangle ASB, within the solvent sphere of B
      dVS = solventDistance(rBS, rAS, dAB, dBV, dAV);
      ptAB = ptB;
      rS = rBS;
      return (voxelIsInTrough(dVS, rBS * rBS, rAS, dAB, dBV) ? dVS : Float.NaN);
    }
    // not within solvent sphere of A or B
    return Float.NaN;
  }

  private static boolean voxelIsInTrough(float dXC, float rAC2, float rBC,
                                         float dAB, float dAX) {
    /*
     *         C
     *        /|\
     *       / | \
     *      /  |  \
     *     /   X   \
     *    /         \
     *   A           B
     * 
     */
    //only calculate what we need -- a factor proportional to cos
    float cosACBf = (rAC2 + rBC * rBC - dAB * dAB) / rBC; //  /2 /rAS);
    float cosACXf = (rAC2 + dXC * dXC - dAX * dAX) / dXC; //  /2 /rAS);
    return (cosACBf < cosACXf);
  }

  private double cosAngleBAS;
  private double angleBAS;
  private double rS;
  private Point3f ptAB;
  
  private float solventDistance(float rAS, float rBS, float dAB, float dAV, float dBV) {
    double dAV2 = dAV * dAV;
    double rAS2 = rAS * rAS;
    double dAB2 = dAB * dAB;
    double angleVAB = Math.acos((dAV2 + dAB2 - dBV * dBV)
        / (2 * dAV * dAB));
    angleBAS = Math.acos(cosAngleBAS = (dAB2 + rAS2 - rBS * rBS)
        / (2 * dAB * rAS));
    float dVS = (float) Math.sqrt(rAS2 + dAV2 - 2 * rAS * dAV
        * Math.cos(angleBAS - angleVAB));
    return dVS;
  }
}
