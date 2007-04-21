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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.util.ArrayUtil;
import org.jmol.util.Logger;
import org.jmol.viewer.Atom;
import org.jmol.viewer.AtomIterator;
import org.jmol.viewer.JmolConstants;

class IsoSolventReader extends VolumeDataReader {

  IsoSolventReader(SurfaceGenerator sg) {
    super(sg);
  }

  ///// solvent-accessible, solvent-excluded surface //////

  class Voxel extends Point3i {
    Point3f ptXyz = new Point3f();
    float value;

    void setValue(int x, int y, int z, float value) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.value = value;
      volumeData.voxelPtToXYZ(x, y, z, this.ptXyz);
    }

    void setValue(float value) {
      if (this.value > value)
        this.value = value;
    }
  }

  Voxel solvent_voxel = new Voxel();
  BitSet atomSet = new BitSet();
  BitSet bsSolventSelected;
  float[] solvent_atomRadius;
  Point3f[] solvent_ptAtom;
  Point3f[] solvent_dots;
  int[] solvent_atomNo = null;
  int solvent_nAtoms;
  int solvent_firstNearbyAtom;
  int solvent_modelIndex;
  boolean solvent_quickPlane;

  void setup() {
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
     */
    bsSolventSelected = new BitSet();
    if (params.thePlane != null)
      volumeData.setPlaneParameters(params.thePlane);
    solvent_quickPlane = true;//viewer.getTestFlag1();
    Atom[] atoms = viewer.getFrame().atoms;
    Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE,
        Float.MAX_VALUE);
    Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE,
        -Float.MAX_VALUE);
    solvent_modelIndex = -1;
    int iAtom = 0;
    int nAtoms = viewer.getAtomCount();
    int nSelected = 0;
    params.iUseBitSets = true;
    if (params.bsIgnore == null) {
      params.bsIgnore = new BitSet();
    }
    for (int i = 0; i < nAtoms; i++) {
      if (params.bsSelected.get(i) && (!params.bsIgnore.get(i))) {
        if (solvent_quickPlane
            && params.thePlane != null
            && Math.abs(volumeData.distancePointToPlane(atoms[i])) > 2 * solventWorkingRadius(atoms[i])) {
          continue;
        }
        bsSolventSelected.set(i);
        nSelected++;
      }
    }
    atomSet = new BitSet();
    int firstSet = -1;
    int lastSet = 0;
    for (int i = 0; i < nAtoms; i++)
      if (bsSolventSelected.get(i)) {
        int iModel = atoms[i].getModelIndex();
        if (solvent_modelIndex < 0)
          solvent_modelIndex = iModel;
        if (solvent_modelIndex != iModel) {
          params.bsIgnore.set(i);
          continue;
        }
        ++iAtom;
        atomSet.set(i);
        if (firstSet == -1)
          firstSet = i;
        lastSet = i;
      }
    int nH = 0;
    solvent_atomNo = null;
    if (iAtom > 0) {
      Point3f[] hAtoms = null;
      if (params.addHydrogens) {
        hAtoms = viewer.getAdditionalHydrogens(atomSet);
        nH = hAtoms.length;
      }
      solvent_atomRadius = new float[iAtom + nH];
      solvent_ptAtom = new Point3f[iAtom + nH];
      solvent_atomNo = new int[iAtom + nH];

      float r = solventWorkingRadius(null);
      for (int i = 0; i < nH; i++) {
        solvent_atomNo[i] = -1;
        solvent_atomRadius[i] = r;
        solvent_ptAtom[i] = hAtoms[i];
        if (params.logMessages)
          Logger.debug("draw {" + hAtoms[i].x + " " + hAtoms[i].y + " "
              + hAtoms[i].z + "};");
      }
      iAtom = nH;
      for (int i = firstSet; i <= lastSet; i++) {
        if (!atomSet.get(i))
          continue;
        solvent_atomNo[iAtom] = i;
        solvent_ptAtom[iAtom] = atoms[i];
        solvent_atomRadius[iAtom++] = solventWorkingRadius(atoms[i]);
      }
    }
    solvent_nAtoms = solvent_firstNearbyAtom = iAtom;
    Logger.info(iAtom + " atoms will be used in the surface calculation");

    for (int i = 0; i < solvent_nAtoms; i++) {
      Point3f pt = solvent_ptAtom[i];
      float rA = solvent_atomRadius[i];
      if (pt.x - rA < xyzMin.x)
        xyzMin.x = pt.x - rA;
      if (pt.x + rA > xyzMax.x)
        xyzMax.x = pt.x + rA;
      if (pt.y - rA < xyzMin.y)
        xyzMin.y = pt.y - rA;
      if (pt.y + rA > xyzMax.y)
        xyzMax.y = pt.y + rA;
      if (pt.z - rA < xyzMin.z)
        xyzMin.z = pt.z - rA;
      if (pt.z + rA > xyzMax.z)
        xyzMax.z = pt.z + rA;
    }
    Logger.info("surface range " + xyzMin + " to " + xyzMax);
    // fragment idea

    Point3f pt = new Point3f();

    BitSet bsNearby = new BitSet();
    int nNearby = 0;
    firstSet = -1;
    lastSet = 0;
    for (int i = 0; i < nAtoms; i++) {
      if (atomSet.get(i) || params.bsIgnore.get(i))
        continue;
      float rA = solventWorkingRadius(atoms[i]);
      if (solvent_quickPlane && params.thePlane != null
          && Math.abs(volumeData.distancePointToPlane(atoms[i])) > 2 * rA)
        continue;
      pt = atoms[i];
      if (pt.x + rA > xyzMin.x && pt.x - rA < xyzMax.x && pt.y + rA > xyzMin.y
          && pt.y - rA < xyzMax.y && pt.z + rA > xyzMin.z
          && pt.z - rA < xyzMax.z) {
        if (firstSet == -1)
          firstSet = i;
        lastSet = i;
        bsNearby.set(i);
        nNearby++;
      }
    }

    if (nNearby != 0) {
      solvent_nAtoms += nNearby;
      solvent_atomRadius = (float[]) ArrayUtil.setLength(solvent_atomRadius,
          solvent_nAtoms);
      solvent_ptAtom = (Point3f[]) ArrayUtil.setLength(solvent_ptAtom,
          solvent_nAtoms);

      iAtom = solvent_firstNearbyAtom;
      for (int i = firstSet; i <= lastSet; i++) {
        if (!bsNearby.get(i))
          continue;
        solvent_ptAtom[iAtom] = atoms[i];
        solvent_atomRadius[iAtom++] = solventWorkingRadius(atoms[i]);
      }
    }

    int maxGrid;
    maxGrid = params.solvent_gridMax;
    setVoxelRange(0, xyzMin.x, xyzMax.x, params.solvent_ptsPerAngstrom, maxGrid);
    setVoxelRange(1, xyzMin.y, xyzMax.y, params.solvent_ptsPerAngstrom, maxGrid);
    setVoxelRange(2, xyzMin.z, xyzMax.z, params.solvent_ptsPerAngstrom, maxGrid);
    //precalculateVoxelData = true;
    int nAtomsWritten = Math.min(solvent_firstNearbyAtom, 100);
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer.append("solvent-").append(dataType == Parameters.SURFACE_SASURFACE ? "accesible" : "excluded").
                   append(" surface\nrange ").append(xyzMin).append(" to ").append(xyzMax).append("\n");
    JvxlReader.jvxlCreateHeader(null, null, volumeData, nAtomsWritten, jvxlFileHeaderBuffer);
   
    Atom atom = null;
    for (int i = 0; i < nAtomsWritten; i++) {
      int nZ = (solvent_atomNo[i] < 0 ? 1 : (atom = atoms[solvent_atomNo[i]]).getElementNumber());
      jvxlFileHeaderBuffer.append(nZ+ " " + nZ + ".0 " + atom.x + " "
          + atom.y + " " + atom.z + "\n");
    }
    atomCount = -Integer.MAX_VALUE;
    negativeAtomCount = false;
  }

  float solventWorkingRadius(Atom atom) {
    float r = (params.solventAtomRadiusAbsolute > 0 ? params.solventAtomRadiusAbsolute
        : atom == null ? JmolConstants.vanderwaalsMars[1] / 1000f
            : params.useIonic ? atom.getBondingRadiusFloat() : atom.getVanderwaalsRadiusFloat());
    r *= params.solventAtomRadiusFactor;
    r += params.solventExtendedAtomRadius + params.solventAtomRadiusOffset;
    if (r < 0.1)
      r = 0.1f;
    return r;
  }

  //float[] surface_data;
  BitSet solvent_bs;
  float cavityRadius;
  float envelopeRadius;

  protected void generateCube() {
    /*
     * 
     * Jmol cavity rendering. Tim Driscoll suggested "filling a 
     * protein with foam. Here you go....
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

    if (params.isCavity && params.theProperty != null) {
      /*
       * couldn't get this to work -- we only have half of the points
       for (int x = 0, i = 0, ipt = 0; x < nPointsX; ++x)
       for (int y = 0; y < nPointsY; ++y)
       for (int z = 0; z < nPointsZ; ++z)
       voxelData[x][y][z] = (
       solvent_bs.get(i++) ? 
       surface_data[ipt++] 
       : Float.NaN);
       mappedDataMin = 0;
       mappedDataMax = 0;
       for (int i = 0; i < solvent_nAtoms; i++)
       if (surface_data[i] > mappedDataMax)
       mappedDataMax = surface_data[i];      
       */
      return;
    }
    cavityRadius = params.cavityRadius;
    envelopeRadius = params.envelopeRadius;
    if (params.isCavity)
      solvent_dots = viewer.calculateSurface(params.bsSelected,
          params.bsIgnore, envelopeRadius);
    generateSolventCube(true);
    if (!params.isCavity || dataType == Parameters.SURFACE_NOMAP
        || dataType == Parameters.SURFACE_PROPERTY)
      return;
    //we have a ring of dots around the model.
    //1) identify which voxelData points are > 0 and within this volume
    //2) turn these voxel points into solvent_atoms with given radii
    //3) rerun the calculation to mark a solvent around these!
    solvent_bs = new BitSet(nPointsX * nPointsY * nPointsZ);
    int i = 0;
    int nDots = solvent_dots.length;
    int n = 0;
    //surface_data = new float[1000];
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y) {
        out: for (int z = 0; z < nPointsZ; ++z, ++i)
          if (voxelData[x][y][z] < Float.MAX_VALUE
              && voxelData[x][y][z] >= cavityRadius) {
            volumeData.voxelPtToXYZ(x, y, z, ptXyzTemp);
            //float dMin = Float.MAX_VALUE;
            //float d;
            for (int j = 0; j < nDots; j++) {
              if (solvent_dots[j].distance(ptXyzTemp) < envelopeRadius)
                continue out;
            }
            solvent_bs.set(i);
            n++;
          }
      }
    Logger.info("cavities include " + n + " voxel points");
    //if (n == 0)
    //  return;
    solvent_atomRadius = new float[n];
    solvent_ptAtom = new Point3f[n];
    for (int x = 0, ipt = 0, apt = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          if (solvent_bs.get(ipt++)) {
            volumeData.voxelPtToXYZ(x, y, z,
                (solvent_ptAtom[apt] = new Point3f()));
            solvent_atomRadius[apt++] = voxelData[x][y][z];
          }
    solvent_nAtoms = solvent_firstNearbyAtom = n;
    generateSolventCube(false);
  }

  boolean selectPocket(boolean doExclude) {
    if (solvent_dots == null)
      solvent_dots = viewer.calculateSurface(params.bsSelected,
          params.bsIgnore, envelopeRadius);
    //mark VERTICES for proximity to surface
    Point3f[] v = meshData.vertices;
    int nVertices = meshData.vertexCount;
    float[] vv = meshData.vertexValues;
    int nDots = solvent_dots.length;
    for (int i = 0; i < nVertices; i++) {
      for (int j = 0; j < nDots; j++) {
        if (solvent_dots[j].distance(v[i]) < envelopeRadius) {
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
        for (int j = ss.length(); --j >= 0;)
          if (ss.get(j) && Float.isNaN(meshData.vertexValues[j])) {
            pocketSet.set(i);
            //System.out.println("pocket " + i + " " + j + " " + surfaceSet[i]);
            break;
          }
    //now clear all vertices that match the pocket toggle
    //"POCKET"   --> pocket TRUE means "show just the pockets"
    //"INTERIOR" --> pocket FALSE means "show everything that is not a pocket"
    for (int i = 0; i < nSets; i++)
      if (meshData.surfaceSet[i] != null) {
        if (pocketSet.get(i) == doExclude)
          meshData.invalidateSurfaceSet(i);
      }
    return true;
  }

  final Point3f ptXyzTemp = new Point3f();
  
  void generateSolventCube(boolean isFirstPass) {
    long time = System.currentTimeMillis();
    float distance = params.distance;
    float[] theProperty = params.theProperty;
    float rA, rB;
    Point3f ptA;
    Point3f ptY0 = new Point3f(), ptZ0 = new Point3f();
    Point3i pt0 = new Point3i(), pt1 = new Point3i();
    float maxValue = Float.MAX_VALUE;
    int propMax = 0, solvMax = 0, iPt;
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          voxelData[x][y][z] = maxValue;
    if (dataType == Parameters.SURFACE_NOMAP)
      return;
    float property[][][] = null;
    boolean isProperty = false;
    if (dataType == Parameters.SURFACE_PROPERTY) {
      property = new float[nPointsX][nPointsY][nPointsZ];
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            property[x][y][z] = Float.NaN;
      isProperty = true;
      propMax = theProperty.length;
      solvMax = (isFirstPass ? solvent_atomNo.length : Integer.MAX_VALUE);
    }
    float maxRadius = 0;
    boolean isWithin = (isFirstPass && distance != Float.MAX_VALUE);
    for (int iAtom = 0; iAtom < solvent_nAtoms; iAtom++) {
      ptA = solvent_ptAtom[iAtom];
      rA = solvent_atomRadius[iAtom];
      if (rA > maxRadius)
        maxRadius = rA;
      if (isWithin && ptA.distance(point) > distance + rA + 0.5)
        continue;
      boolean isNearby = (iAtom >= solvent_firstNearbyAtom);
      setGridLimitsForAtom(ptA, rA, pt0, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      for (int i = pt0.x; i < pt1.x; i++) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++) {
            float v = ptXyzTemp.distance(ptA) - rA;
            if (v < voxelData[i][j][k]) {
              voxelData[i][j][k] = (isNearby || isWithin
                  && ptXyzTemp.distance(point) > distance ? Float.NaN : v);
              if (isProperty && iAtom < solvMax
                  && (iPt = solvent_atomNo[iAtom]) >= 0
                  && iPt < propMax) {
                property[i][j][k] = theProperty[iPt];
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
    if (params.isCavity && isFirstPass)
      return;
    float solventRadius = params.solventRadius;
    if (!params.isCavity && solventRadius > 0
        && (dataType == Parameters.SURFACE_SOLVENT || dataType == Parameters.SURFACE_MOLECULAR)) {
      Point3i ptA0 = new Point3i();
      Point3i ptB0 = new Point3i();
      Point3i ptA1 = new Point3i();
      Point3i ptB1 = new Point3i();
      for (int iAtom = 0; iAtom < solvent_firstNearbyAtom - 1; iAtom++)
        if (solvent_ptAtom[iAtom] instanceof Atom) {
          ptA = solvent_ptAtom[iAtom];
          rA = solvent_atomRadius[iAtom] + solventRadius;
          if (isWithin && ptA.distance(point) > distance + rA + 0.5)
            continue;
          setGridLimitsForAtom(ptA, rA - solventRadius, ptA0, ptA1);
          AtomIterator iter = viewer.getWithinModelIterator((Atom) ptA, rA + solventRadius + maxRadius);
          while (iter.hasNext()) {
            Atom ptB = iter.next();
            if (ptB.getAtomIndex() <= ((Atom) ptA).getAtomIndex())
              continue;
            // selected 
            // only consider selected neighbors
            if (!bsSolventSelected.get(ptB.getAtomIndex()))
              continue;
            rB = solventWorkingRadius(ptB) + solventRadius;
            if (isWithin && ptB.distance(point) > distance + rB + 0.5)
              continue;
            if (solvent_quickPlane && params.thePlane != null
                && Math.abs(volumeData.distancePointToPlane(ptB)) > 2 * rB)
              continue;

            float dAB = ptA.distance(ptB);
            if (dAB >= rA + rB)
              continue;
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
                  float dVS = checkSpecialVoxel(ptA, rA, ptB, rB, dAB,
                      ptXyzTemp);
                  if (!Float.isNaN(dVS)) {
                    float v = solventRadius - dVS;
                    if (v < voxelData[i][j][k]) {
                      voxelData[i][j][k] = (isWithin
                          && ptXyzTemp.distance(point) > distance ? Float.NaN
                          : v);
                      if (isProperty && iAtom < solvMax
                          && (iPt = solvent_atomNo[iAtom]) >= 0
                          && iPt < propMax) {
                        property[i][j][k] = theProperty[iPt];
                      }
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
    }

    if (dataType == Parameters.SURFACE_PROPERTY)
      voxelData = property;
    if (params.thePlane == null) {
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] == Float.MAX_VALUE)
              voxelData[x][y][z] = Float.NaN;
    } else {  //solvent planes just focus on negative values
      maxValue = 0.001f;
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] < maxValue) {
              // Float.NaN will also match ">=" this way
            } else {
              voxelData[x][y][z] = maxValue;
            }
    }
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("solvent surface time:"
          + (System.currentTimeMillis() - time));
    }
  }

  void setGridLimitsForAtom(Point3f ptA, float rA, Point3i pt0, Point3i pt1) {
    volumeData.xyzToVoxelPt(ptA.x - rA, ptA.y - rA, ptA.z - rA, pt0);
    pt0.x -= 1;
    pt0.y -= 1;
    pt0.z -= 1;
    if (pt0.x < 0)
      pt0.x = 0;
    if (pt0.y < 0)
      pt0.y = 0;
    if (pt0.z < 0)
      pt0.z = 0;
    volumeData.xyzToVoxelPt(ptA.x + rA, ptA.y + rA, ptA.z + rA, pt1);
    pt1.x += 2;
    pt1.y += 2;
    pt1.z += 2;
    if (pt1.x >= nPointsX)
      pt1.x = nPointsX;
    if (pt1.y >= nPointsY)
      pt1.y = nPointsY;
    if (pt1.z >= nPointsZ)
      pt1.z = nPointsZ;
  }
/*
  float getSolventValue(int x, int y, int z) {

    // old method -- not used 

    solvent_voxel.setValue(x, y, z, Float.MAX_VALUE);
    float rA, rB;
    Point3f ptA, ptB;
    for (int i = 0; i < solvent_nAtoms && solvent_voxel.value >= -0.5; i++) {
      ptA = solvent_ptAtom[i];
      rA = solvent_atomRadius[i];
      float v = solvent_voxel.ptXyz.distance(ptA) - rA;
      if (v < solvent_voxel.value)
        solvent_voxel.setValue(i >= solvent_firstNearbyAtom ? Float.NaN : v);
    }
    if (solventRadius == 0)
      return solvent_voxel.value;
    Point3f ptV = solvent_voxel.ptXyz;
    for (int i = 0; i < solvent_nAtoms - 1 && solvent_voxel.value >= -0.5; i++) {
      ptA = solvent_ptAtom[i];
      rA = solvent_atomRadius[i] + solventRadius;
      for (int j = i + 1; j < solvent_nAtoms && solvent_voxel.value >= -0.5; j++) {
        if (i >= solvent_firstNearbyAtom && j >= solvent_firstNearbyAtom)
          continue;
        ptB = solvent_ptAtom[j];
        rB = solvent_atomRadius[j] + solventRadius;
        float dAB = ptA.distance(ptB);
        if (dAB >= rA + rB)
          continue;
        float dVS = checkSpecialVoxel(ptA, rA, ptB, rB, dAB, ptV);
        if (!Float.isNaN(dVS))
          solvent_voxel.setValue(solventRadius - dVS);
      }
    }
    return solvent_voxel.value;
  }
*/
  final Point3f ptS = new Point3f();

  float checkSpecialVoxel(Point3f ptA, float rAS, Point3f ptB, float rBS,
                          float dAB, Point3f ptV) {
    /*
     * Checking here for voxels that are in the situation:
     * 
     * A------)-- V ---((--))-- S --(------B
     *            |----d--------|     
     *  or
     *
     * B------)-- V ---((--))-- S --(------A
     *            |----d--------|     
     *
     * A and B are the two atom centers; V is the voxel; S is a hypothetical
     * PROJECTED solvent center based on the position of V in relation to 
     * first A, then B; ( and ) are atom radii and (( )) are the overlapping
     * atom+solvent radii.
     * 
     * That is, where the projected solvent location for one voxel is 
     * within the solvent radius sphere of another, this voxel should
     * be checked in relation to solvent distance, not atom distance.
     * 
     * 
     *        S
     *++    /   \    ++
     *  ++ /  |  \ ++
     *    +   V   +      x     want V such that angle ASV < angle ASB
     *   / ******  \
     *  A --+--+----B
     *        b
     * 
     * A, B are atoms; S is solvent center; V is voxel point 
     * objective is to calculate dSV. ++ Here represents the van der Waals 
     * radius for each atom. ***** is the key "trough" location.
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
     * that is, cosASB < cosASV 
     * 
     * If we find the voxel in the "trough", then we set its value to 
     * (solvent radius - dVS).
     * 
     */
    float dAV = ptA.distance(ptV);
    float dBV = ptB.distance(ptV);
    float dVS = Float.NaN;
    float f = rAS / dAV;
    if (f > 1) {
      ptS.set(ptA.x + (ptV.x - ptA.x) * f, ptA.y + (ptV.y - ptA.y) * f, ptA.z
          + (ptV.z - ptA.z) * f);
      if (ptB.distance(ptS) < rBS) {
        dVS = solventDistance(ptV, ptA, ptB, rAS, rBS, dAB, dAV, dBV);
        if (!voxelIsInTrough(dVS, rAS * rAS, rBS, dAB, dAV, dBV))
          return Float.NaN;
      }
      return dVS;
    }
    f = rBS / dBV;
    if (f <= 1)
      return dVS;
    ptS.set(ptB.x + (ptV.x - ptB.x) * f, ptB.y + (ptV.y - ptB.y) * f, ptB.z
        + (ptV.z - ptB.z) * f);
    if (ptA.distance(ptS) < rAS) {
      dVS = solventDistance(ptV, ptB, ptA, rBS, rAS, dAB, dBV, dAV);
      if (!voxelIsInTrough(dVS, rAS * rAS, rBS, dAB, dAV, dBV))
        return Float.NaN;
    }
    return dVS;
  }

  boolean voxelIsInTrough(float dVS, float rAS2, float rBS, float dAB,
                          float dAV, float dBV) {
    //only calculate what we need -- a factor proportional to cos
    float cosASBf = (rAS2 + rBS * rBS - dAB * dAB) / rBS; //  /2 /rAS);
    float cosASVf = (rAS2 + dVS * dVS - dAV * dAV) / dVS; //  /2 /rAS);
    return (cosASBf < cosASVf);
  }

  float solventDistance(Point3f ptV, Point3f ptA, Point3f ptB, float rAS,
                        float rBS, float dAB, float dAV, float dBV) {
    double angleVAB = Math.acos((dAV * dAV + dAB * dAB - dBV * dBV)
        / (2 * dAV * dAB));
    double angleBAS = Math.acos((dAB * dAB + rAS * rAS - rBS * rBS)
        / (2 * dAB * rAS));
    float dVS = (float) Math.sqrt(rAS * rAS + dAV * dAV - 2 * rAS * dAV
        * Math.cos(angleBAS - angleVAB));
    return dVS;
  }

  
}
