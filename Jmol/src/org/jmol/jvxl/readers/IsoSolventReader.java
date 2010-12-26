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

//import java.util.ArrayList;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

//import org.jmol.util.Escape;
//import org.jmol.util.ArrayUtil;
import org.jmol.util.Logger;
import org.jmol.util.Measure;

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
  protected float solventRadius;
  private boolean havePlane;

  @Override
  protected void setup() {
    super.setup();
    cavityRadius = params.cavityRadius;
    envelopeRadius = params.envelopeRadius;
    solventRadius = params.solventRadius;
    point = params.point;

    isCavity = (params.isCavity && meshDataServer != null); // Jvxl cannot do this calculation on its own.
    isPocket = (params.pocket != null && meshDataServer != null);
    havePlane = (params.thePlane != null);

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
    if (bsNearby != null)
      bsMySelected.or(bsNearby);

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
    if (isCavity && dataType != Parameters.SURFACE_NOMAP
        && dataType != Parameters.SURFACE_PROPERTY) {
      generateSolventCube(true);
      generateSolventCavity();
      generateSolventCube(false);
    } else if (params.doFullMolecular) {
      generateSolventCubeMsMs();
    } else {
      generateSolventCube(true);
    }
    if (params.cappingObject instanceof Point4f) { // had a check for mapping here, turning this off
      //Logger.info("capping isosurface using " + params.cappingPlane);
      volumeData.capData((Point4f) params.cappingObject, params.cutoff);
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
  private AtomIndexIterator iter;

  void generateSolventCubeMsMs() {
    volumeData.getYzCount();
    resetVoxelData(Float.MAX_VALUE);
    if (dataType == Parameters.SURFACE_NOMAP)
      return;
    iter = atomDataServer.getSelectedAtomIterator(bsMySelected, true, false); // was TRUE TRUE
    Logger.startTimer();
    getReducedEdges();
    getReducedFaces();
    validateFaces();
    markFaceVoxels(true);
    markToroidVoxels();
    vEdges = null;
    // second pass picks up singularities
    markFaceVoxels(false);
    vFaces = null;
    markSphereVoxels(false);
    markSphereVoxels(true);
    iter.release();
    iter = null;
    unsetVoxelData();
    Logger.checkTimer("solvent surface time");
  }

  private void setVoxel(int i, int j, int k, float value) {
    //if (i == 15 && j == 10 && k == 23) {
    //  dumpPoint(ptXyzTemp);
     // System.out.println("value="  + value);
    //}
    voxelData[i][j][k] = value;
  }

  private BitSet[] bsLocale;
  private List<Edge> vEdges;

  protected int nTest;

  private void getReducedEdges() {
    int atomCount = myAtomCount;
    bsLocale = new BitSet[atomCount];
    htEdges = new Hashtable<String, Edge>();
    vEdges = new ArrayList<Edge>();
    for (int iatomA = 0; iatomA < atomCount; iatomA++)
      bsLocale[iatomA] = new BitSet();
    getMaxRadius();
    float dist2 = solventRadius + maxRadius;
    for (int iatomA = 0; iatomA < atomCount; iatomA++) {
      Point3f ptA = atomXyz[iatomA];
      float rA = atomRadius[iatomA] + solventRadius;
      atomDataServer.setIteratorForAtom(iter, atomIndex[iatomA], rA + dist2);
      while (iter.hasNext()) {
        int iB = iter.next();
        int iatomB = myIndex[iB];
        Point3f ptB = atomXyz[iatomB];
        float rB = atomRadius[iatomB] + solventRadius;
        float dAB = ptA.distance(ptB);
        if (dAB >= rA + rB)
          continue;
        Edge edge = new Edge(iatomA, iatomB);
        vEdges.add(edge);
        bsLocale[iatomA].set(iatomB);
        bsLocale[iatomB].set(iatomA);
        htEdges.put(edge.toString(), edge);
      }
    }
    Logger.info(vEdges.size() + " reduced edges");
  }

  private class Edge {
    int ia, ib;
    //List<Face>edgeFaces = new ArrayList<Face>();
    int nFaces;
    int nInvalid;

    Edge(int ia, int ib) {
      this.ia = Math.min(ia, ib);
      this.ib = Math.max(ia, ib);
    }

    void addFace(Face f) {
      //edgeFaces.add(f);
      nFaces++;
    }

    int getType() {
      return (nFaces > 0 ? nFaces : nInvalid > 0 ? -nInvalid : 0);
    }

    /*
    void setPointP() {
      dPS = getPointP(ia, ib);
      pPlane = plane;
      ptP = new Point3f(p);
      isSingular = (dPS < solventRadius);
    }
    */

    /*
    void dump() {
      System.out.println("draw e" + (nTest) + " @{point"
          + new Point3f(atomXyz[ia]) + "} @{point" + new Point3f(atomXyz[ib])
          + "} color green # " + getType());
    }
    */

    @Override
    public String toString() {
      return ia + "_" + ib;
    }

  }

  private class Face {
    int ia, ib, ic;
    boolean isValid;
    boolean isSingular;
    Point3f pS; // solvent position
    Edge[] edges = new Edge[3];
    public float sortAngle;
    
    Face(int ia, int ib, int ic, Edge edgeAB, Point3f pS, boolean isSingular) {
      this.ia = ia;
      this.ib = ib;
      this.ic = ic;
      this.pS = new Point3f(pS);
      this.isSingular = isSingular;
      edges[0] = edgeAB;
    }

    void setEdges() {
      if (edges[1] != null)
        return;
      edges[1] = findEdge(ib, ic);
      edges[2] = findEdge(ic, ia);
    }

    /*
    public void dump() {
      setEdges();
      System.out.println("/" + * "+faceIndex+ " *" + "/draw fp" + (nTest++) + " @{point"
          + new Point3f(atomXyz[ia]) + "}" + " @{point"
          + new Point3f(atomXyz[ib]) + "}" + " @{point"
          + new Point3f(atomXyz[ic]) + "}" + "# " + ia + " " + ib + " " + ic
          + " " + edges[0] + " " + edges[1] + " " + edges[2] + " " + pS + " " + atomXyz[ia] + " " + atomXyz[ib] + " "  + atomXyz[ic]);
      if (isValid)
        System.out.println("draw pp" + (++nTest) + " width 3.0 @{point" + pS
            + "};");
    }
    */
/*
    int otherPoint(int i, int j) {
      return (i == ia ? (j == ib ? ic : ib) 
          : i == ib ? (j == ia ? ic : ia)
          : j == ia ? ib : ia);
    }
*/
  }

  List<Face> vFaces;
  Map<String, Edge> htEdges;
  BitSet validSpheres;

  private void getReducedFaces() {
    BitSet bs = new BitSet();
    vFaces = new ArrayList<Face>();
    validSpheres = new BitSet();
    nTest = 0;
    for (int i = vEdges.size(); --i >= 0;) {
      Edge edge = vEdges.get(i);
      int ia = edge.ia;
      int ib = edge.ib;
      bs.clear();
      bs.or(bsLocale[ia]);
      bs.and(bsLocale[ib]);
      for (int ic = bs.nextSetBit(ib + 1); ic >= 0; ic = bs.nextSetBit(ic + 1)) {
        if (getSolventPoints(ia, ib, ic)) {
          vFaces.add(new Face(ia, ib, ic, edge, ptS1, dPX < solventRadius));
          vFaces.add(new Face(ib, ia, ic, edge, ptS2, dPX < solventRadius));
        }
      }
    }
    Logger.info(vFaces.size() + " reduced (double-sided) faces");
  }

  protected Edge findEdge(int i, int j) {
    return htEdges.get(i < j ? i + "_" + j : j + "_" + i);
  }

  private float dPX;
  protected double dPS;
  
  private boolean getSolventPoints(int ia, int ib, int ic) {
    /*
     * 
     * A----------p-----B
     *           /|\
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
     * 
     */

    dPS = getPointP(ia, ib);

    //    * 2) project C onto plane as ptT

    Point3f ptC = atomXyz[ic];
    float rCS = atomRadius[ic] + solventRadius;
    float dCT = Measure.distanceToPlane(plane, ptC);
    if (Math.abs(dCT) >= rCS)
      return false;
    double dST = Math.sqrt(rCS * rCS - dCT * dCT);
    ptTemp.scaleAdd(-dCT, vTemp, ptC);
    double dpT = p.distance(ptTemp);
    float dsp2 = (float) (dPS * dPS);
    double cosTheta = (dsp2 + dpT * dpT - dST * dST) / (2 * dPS * dpT);
    //    * 3) calculate two possible pS1 and pS2 in this plane

    if (Math.abs(cosTheta) >= 1) {
      return false;
    }

    Vector3f vXS = vTemp2;
    vXS.set(ptTemp);
    vXS.sub(p);
    vXS.normalize();
    dPX = (float) (dPS * cosTheta);
    ptTemp.scaleAdd(dPX, vXS, p);
    vXS.cross(vTemp, vXS);
    vXS.normalize();
    vXS.scale((float) (Math.sqrt(1 - cosTheta * cosTheta) * dPS));
    ptS1.set(ptTemp);
    ptS1.add(vXS);
    ptS2.set(ptTemp);
    ptS2.sub(vXS);
    return true;
  }

  private void validateFaces() {
    float dist2 = solventRadius + maxRadius;
    int n = 0;
    int nSingular = 0;
    for (int i = vFaces.size(); --i >= 0;) {
      Face f = vFaces.get(i);
      atomDataServer.setIteratorForPoint(iter, modelIndex, f.pS, dist2);
      f.isValid = true;
      while (iter.hasNext()) {
        int ia = iter.next();
        int iatom = myIndex[ia];
        if (iatom == f.ia || iatom == f.ib || iatom == f.ic)
          continue;
        float d = atomData.atomXyz[ia].distance(f.pS);
        if (d < atomData.atomRadius[ia] + solventRadius) {
          f.isValid = false;
          break;
        }
      }
      f.setEdges();
      if (f.isValid) {
        if (f.isSingular)
          nSingular++;
        n++;
        for (int k = 0; k < 3; k++) {
          f.edges[k].addFace(f);
          validSpheres.set(f.edges[k].ia);
          validSpheres.set(f.edges[k].ib);
          //f.edges[k].setPointP();            
        }
      } else {
        for (int k = 0; k < 3; k++)
          f.edges[k].nInvalid++;
      }
    }
    Logger.info(n + " validated reduced faces; " + nSingular + " singular");
    int[] nFaces = new int[11];
    for (int ei = vEdges.size(); --ei >= 0;) {
      int type = vEdges.get(ei).getType();
      if (type < 10)
        nFaces[type < 0 ? 0 : type + 1]++;
    }
    for (int i = 0; i < 10; i++)
      System.out.print((i-1) + ": " + nFaces[i] + "\t");
    System.out.println("edge types");
  }

  private Point3f ptY0 = new Point3f();
  private Point3f ptZ0 = new Point3f();
  private Point3i pt0 = new Point3i();
  private Point3i pt1 = new Point3i();
  
  private void markFaceVoxels(boolean fullyEnclosed) {
    for (int fi = vFaces.size(); --fi >= 0;) {
      Face f = vFaces.get(fi);
      if (!f.isValid)
        continue;
      setGridLimitsForAtom(f.pS, solventRadius + volumeData.minGrid, pt0, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      Point3f ptA = atomXyz[f.ia];
      Point3f ptB = atomXyz[f.ib];
      Point3f ptC = atomXyz[f.ic];
      Point3f ptS = f.pS;
      /*
      dumpLine(ptA, ptB);
      dumpLine(ptB, ptC);
      dumpLine(ptC, ptS);
      dumpLine(ptS, ptA);
      dumpLine(ptS, ptB);
      dumpLine(ptS, ptC);
      */
      for (int i = pt0.x; i < pt1.x; i++) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++, ptXyzTemp
              .add(volumetricVectors[2])) {
            if (fullyEnclosed || voxelData[i][j][k] > 0) {
              if (Measure.isInTetrahedron(ptXyzTemp, ptA, ptB, ptC, ptS, plane,
                  vTemp, vTemp2, vTemp3, fullyEnclosed)) {
                float value = solventRadius - ptXyzTemp.distance(ptS);
                if (value < voxelData[i][j][k])
                  setVoxel(i, j, k, value);
              }
            }
          }
          ptXyzTemp.set(ptZ0);
          ptXyzTemp.add(volumetricVectors[1]);
        }
        ptXyzTemp.set(ptY0);
        ptXyzTemp.add(volumetricVectors[0]);
      }
    }
  }

  //private static Point3f ptRef = new Point3f((float) Math.PI, (float) Math.E, (float) Math.tan(Math.E));

  private void markToroidVoxels() {
    Point3i ptA0 = new Point3i();
    Point3i ptB0 = new Point3i();
    Point3i ptA1 = new Point3i();
    Point3i ptB1 = new Point3i();
    for (int ei = vEdges.size(); --ei >= 0;) {
      Edge edge = vEdges.get(ei);
      if (edge.getType() < 0)
        continue;
      int ia = edge.ia;
      int ib = edge.ib;
      if (edge.getType() == 0) {
        validSpheres.set(ia);
        validSpheres.set(ib);
      }
      Point3f ptA = atomXyz[ia];
      Point3f ptB = atomXyz[ib];
      float rAS = atomRadius[ia] + solventRadius;
      float rBS = atomRadius[ib] + solventRadius;
      float dAB = ptB.distance(ptA);
      /*
      int f0 = 0;
      if (edge.nFaces > 0) {
        for (int fi = edge.nFaces; --fi >= 0;) {
          Face f = edge.faces[fi];
          Point3f ptC = atomXyz[f.otherPoint(ia, ib)];
          f.sortAngle = Measure.computeTorsion(ptC, ptA, ptB, edge.faces[fi].pS, true);
          f.edgeReversed = f.sortAngle < 0;//!edge.faces[fi].isEdgeInOrder(ia, ib);
          f.sortAngle += Measure.computeTorsion(ptC, ptA, ptB, ptRef, true);
        }
        Arrays.sort(edge.faces, new angleSort());
        if (edge.faces[0].edgeReversed) {
          f0 = 1;
          edge.faces[edge.nFaces] = edge.faces[0];
          edge.faces[0].sortAngle += 360;
        }
      }
      */
      setGridLimitsForAtom(ptA, atomRadius[ia] + solventRadius, ptA0, ptA1);
      setGridLimitsForAtom(ptB, atomRadius[ib] + solventRadius, ptB0, ptB1);
      mergeLimits(ptA0, ptB0, pt0, null);
      mergeLimits(ptA1, ptB1, null, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      for (int i = pt0.x; i < pt1.x; i++) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++, ptXyzTemp
              .add(volumetricVectors[2])) {
            //16 4 8
            //System.out.println("draw pb" + (nTest++) + " @{point"
              //  + ptXyzTemp + "} \""+i + " " + j + " " + k +"\"");
            float dVS = checkSpecialVoxel(ptA, rAS, ptB, rBS, dAB, ptXyzTemp);
            if (Float.isNaN(dVS))
              continue;
            float value = solventRadius - dVS;
            /*
            if (edge.nFaces > 0) {
              float angle = Measure.computeTorsion(ptXyzTemp, ptA, ptB, ptRef,
                  true);              
              boolean isOK = false;
              for (int fi = f0; fi < edge.nFaces + f0; fi += 2) {
                if (true || angle > edge.faces[fi].sortAngle && angle < edge.faces[fi+1].sortAngle
                    || angle + 360 > edge.faces[fi].sortAngle && angle + 360 < edge.faces[fi+1].sortAngle) {
                  isOK = true;
               //   if (nTest == 1320)
                 //   System.out.println("hmm");
                  if (false) {
                  System.out.println("#" + ei + " " + fi + " a=" + angle + " a[fi]=" + edge.faces[fi].sortAngle + " a[fi++]=" + edge.faces[fi+1].sortAngle);
                  System.out.println("#" + angle + " " + edge.faces[fi].sortAngle);
                  edge.faces[fi].dump();
                  edge.faces[fi+1].dump();
                  edge.dump();
                  dumpPoint(ptRef, "pb");
                  dumpPoint(ptXyzTemp,"pt");
                  dumpLine(ptA, edge.faces[fi].pS, "pas");
                  dumpLine(ptB, edge.faces[fi].pS, "pbs");
                  break;
                }
              }
              if (!isOK)
                continue;
                //dVS = solventRadius + 0.1f;
            }
            if (nTest < 1000) {
              edge.setPointP();
              vTemp.set(ptXyzTemp);
              vTemp.sub(p);
              vTemp.normalize();
              vTemp.scale(-value);
              vTemp.add(ptXyzTemp);
              System.out.println("draw vvs" + (nTest++) + " @{point"
                  + new Point3f(ptXyzTemp) + "}" + " @{point" + vTemp
                  + "}");
            }
            */
            if (value < voxelData[i][j][k])
              setVoxel(i, j, k, value);
          }

          ptXyzTemp.set(ptZ0);
          ptXyzTemp.add(volumetricVectors[1]);
        }
        ptXyzTemp.set(ptY0);
        ptXyzTemp.add(volumetricVectors[0]);
      }
    }
  }

  protected class angleSort implements Comparator<Face> {

    public int compare(Face f1, Face f2) {
      return (f1 == null || f2 == null ? 0 : f1.sortAngle < f2.sortAngle ? -1 : f1.sortAngle > f2.sortAngle ? 1 : 0);
    }    
  }


  private void markSphereVoxels(boolean asValid) {
    for (int iAtom = 0; iAtom < myAtomCount; iAtom++) {
      boolean isNearby = (iAtom >= firstNearbyAtom);
      Point3f ptA = atomXyz[iAtom];
      float rA = atomRadius[iAtom];
      setGridLimitsForAtom(ptA, rA, pt0, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      for (int i = pt0.x; i < pt1.x; i++) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++, ptXyzTemp
              .add(volumetricVectors[2])) {
            float v = ptXyzTemp.distance(ptA) - rA;
            //if (iAtom==1)
              //System.out.println("draw pb" + (nTest++) + " @{point"
                //+ ptXyzTemp + "} \""+i + " " + j + " " + k + " " + " " + v + " " + voxelData[i][j][k] + " \"#");
            if (!asValid && v > 0)
              continue;
            if (v < voxelData[i][j][k])
              setVoxel(i, j, k, (isNearby ? Float.NaN : v));
          }
          ptXyzTemp.set(ptZ0);
          ptXyzTemp.add(volumetricVectors[1]);
        }
        ptXyzTemp.set(ptY0);
        ptXyzTemp.add(volumetricVectors[0]);
      }
    }
  }

  void resetVoxelData(float value) {
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          voxelData[x][y][z] = value;
  }

  void unsetVoxelData() {
    if (!havePlane) {
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] == Float.MAX_VALUE)
              voxelData[x][y][z] = Float.NaN;
    } else { //solvent planes just focus on negative values
      for (int x = 0; x < nPointsX; ++x)
        for (int y = 0; y < nPointsY; ++y)
          for (int z = 0; z < nPointsZ; ++z)
            if (voxelData[x][y][z] < 0.001f) {
              // Float.NaN will also match ">=" this way
            } else {
              voxelData[x][y][z] = 0.001f;
            }
    }
  }

  private float maxRadius;

  void getMaxRadius() {
    maxRadius = 0;
    for (int iAtom = 0; iAtom < myAtomCount; iAtom++) {
      float rA = atomRadius[iAtom];
      if (rA > maxRadius)
        maxRadius = rA;
    }
  }

  protected Vector3f vTemp = new Vector3f();
  protected Point4f plane = new Point4f();

  void generateSolventCube(boolean isFirstPass) {
    float distance = params.distance;
    float rA, rB;
    Point3f ptA;
    Point3f ptY0 = new Point3f(), ptZ0 = new Point3f();
    Point3i pt0 = new Point3i(), pt1 = new Point3i();
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
    AtomIndexIterator iter = (doCalculateTroughs ? 
        atomDataServer.getSelectedAtomIterator(bsMySelected, true, false) : null);
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
            if (v < voxelData[i][j][k]) {
              voxelData[i][j][k] = (isNearby || isWithin
                  && ptXyzTemp.distance(point) > distance ? Float.NaN : v);
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
    if (isCavity && isFirstPass)
      return;
    if (doCalculateTroughs) {
      Point3i ptA0 = new Point3i();
      Point3i ptB0 = new Point3i();
      Point3i ptA1 = new Point3i();
      Point3i ptB1 = new Point3i();
      for (int iAtom = 0; iAtom < firstNearbyAtom - 1; iAtom++)
        if (atomNo[iAtom] > 0) {
          ptA = atomXyz[iAtom];
          rA = atomRadius[iAtom] + solventRadius;
          int iatomA = atomIndex[iAtom];
          if (isWithin && ptA.distance(point) > distance + rA + 0.5)
            continue;
          setGridLimitsForAtom(ptA, rA - solventRadius, ptA0, ptA1);
          atomDataServer.setIteratorForAtom(iter, iatomA, rA + solventRadius + maxRadius);
          //true ==> only atom index > this atom accepted
          while (iter.hasNext()) {
            int iatomB = iter.next();
            Point3f ptB = atomXyz[myIndex[iatomB]];
            rB = atomData.atomRadius[iatomB] + solventRadius;
            if (isWithin && ptB.distance(point) > distance + rB + 0.5)
              continue;
            if (params.thePlane != null
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
    }
    if (params.thePlane == null) {
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
  }

  private static void mergeLimits(Point3i ptA, Point3i ptB, Point3i pt0,
                                  Point3i pt1) {
    if (pt0 != null) {
      pt0.x = Math.min(ptA.x, ptB.x);
      pt0.y = Math.min(ptA.y, ptB.y);
      pt0.z = Math.min(ptA.z, ptB.z);
    }
    if (pt1 != null) {
      pt1.x = Math.max(ptA.x, ptB.x);
      pt1.y = Math.max(ptA.y, ptB.y);
      pt1.z = Math.max(ptA.z, ptB.z);
    }
  }

  protected Point3f ptTemp2 = new Point3f();
  private Point3f ptS1 = new Point3f();
  private Point3f ptS2 = new Point3f();
  protected Vector3f vTemp2 = new Vector3f();
  private Vector3f vTemp3 = new Vector3f();

  private void setGridLimitsForAtom(Point3f ptA, float rA, Point3i pt0,
                                    Point3i pt1) {
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
      cosAngleBAS = -cosAngleBAS;
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

  protected double cosAngleBAS;
  private double angleBAS;

  private float solventDistance(float rAS, float rBS, float dAB, float dAV,
                                float dBV) {
    double dAV2 = dAV * dAV;
    double rAS2 = rAS * rAS;
    double dAB2 = dAB * dAB;
    double angleVAB = Math.acos((dAV2 + dAB2 - dBV * dBV) / (2 * dAV * dAB));
    angleBAS = Math.acos(cosAngleBAS = (dAB2 + rAS2 - rBS * rBS)
        / (2 * dAB * rAS));
    float dVS = (float) Math.sqrt(rAS2 + dAV2 - 2 * rAS * dAV
        * Math.cos(angleBAS - angleVAB));
    return dVS;
  }

  protected double getPointP(int ia, int ib) {
    Point3f ptA = atomXyz[ia];
    Point3f ptB = atomXyz[ib];
    float rAS = atomRadius[ia] + solventRadius;
    float rBS = atomRadius[ib] + solventRadius;
    vTemp.set(ptB);
    vTemp.sub(ptA);
    float dAB = vTemp.length();
    vTemp.normalize();
    double rAS2 = rAS * rAS;
    double dAB2 = dAB * dAB;
    angleBAS = Math.acos(cosAngleBAS = (dAB2 + rAS2 - rBS * rBS)
        / (2 * dAB * rAS));
    p.scaleAdd((float) (cosAngleBAS * rAS), vTemp, ptA);
    Measure.getPlaneThroughPoint(p, vTemp, plane);
    return Math.sin(angleBAS) * rAS;
  }

  float checkSpecialVoxel2(Point3f ptA, float rAS, Point3f ptB, float rBS,
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
      return (voxelIsInTrough(dVS, rBS * rBS, rAS, dAB, dBV) ? dVS : Float.NaN);
    }
    // not within solvent sphere of A or B
    return Float.NaN;
  }

  /*
  private void dumpLine(Point3f pt1, Point3f pt2, String label) {
    System.out.println("draw " + label + (nTest++) + " @{point" + new Point3f(pt1) + "} @{point" + new Point3f(pt2) + "}");
  }
  private void dumpPoint(Point3f pt, String label) {
    System.out.println("draw " + label + (nTest++) + " @{point" + new Point3f(pt) + "}");
  }
  */
}
