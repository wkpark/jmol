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

import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

import java.util.BitSet;

/* ***************************************************************
 * 
 * 7/17/06 History -- Bob Hanson
 * 
 * Connolly surface rendering was never completed. Miguel got to the point
 * where he identified the three issues -- convex single-atom areas, 
 * two-atom connection "toruses" or "troughs", and three-atom connection "cavities",
 * and he successfully took care of each in its own way. However, he never figured 
 * out how to patch these together effectively, and the surface had triangular 
 * holes.
 *
 * This code was never documented, so users never worked with it.
 * In July of 2006, this code was superceded by the "isosurface solvent" 
 * command, which does this using the marching cubes algorithm to produce 
 * a much cleaner surface. Of course it also takes more time. 
 * 
 * What remains is the van der Waals surface, which can be extended using
 * 
 * geosurface +1.2
 * 
 * to provide the solvent-accessible surface.
 * 
 * A better rendering of the solvent accessible surface is given using
 * 
 * isosurface sasurface  
 * 
 * A discussion of molecular/solvent-accessible surfaces can be found at
 * http://www.netsci.org/Science/Compchem/feature14e.html
 * 
 */

/*
 * Miguel's original comments:
 * 
 *  The Dots and DotsRenderer classes implement vanderWaals and Connolly
 * dot surfaces. <p>
 * The vanderWaals surface is defined by the vanderWaals radius of each
 * atom. The surface of the atom is 'peppered' with dots. Each dot is
 * tested to see if it falls within the vanderWaals radius of any of
 * its neighbors. If so, then the dot is not displayed. <p>
 * See DotsRenderer.Geodesic for more discussion of the implementation. <p>
 * The Connolly surface is defined by rolling a probe sphere over the
 * surface of the molecule. In this way, a smooth surface is generated ...
 * one that does not have crevices between atoms. Three types of shapes
 * are generated: convex, saddle, and concave. <p>
 * The 'probe' is a sphere. A sphere of 1.2 angstroms representing HOH
 * is commonly used. <p>
 * Convex shapes are generated on the exterior surfaces of exposed atoms.
 * They are points on the sphere which are exposed. In these areas of
 * the molecule they look just like the vanderWaals dot surface. <p>
 * The saddles are generated between pairs of atoms. Imagine an O2
 * molecule. The probe sphere is rolled around the two oxygen spheres so
 * that it stays in contact with both spheres. The probe carves out a
 * torus (donut). The portion of the torus between the two points of
 * contact with the oxygen spheres is a saddle. <p>
 * The concave shapes are defined by triples of atoms. Imagine three
 * atom spheres in a close triangle. The probe sphere will sit (nicely)
 * in the little cavity formed by the three spheres. In fact, there are
 * two cavities, one on each side of the triangle. The probe sphere makes
 * one point of contact with each of the three atoms. The shape of the
 * cavity is the spherical triangle on the surface of the probe sphere
 * determined by these three contact points. <p>
 * For each of these three surface shapes, the dots are painted only
 * when the probe sphere does not interfere with any of the neighboring
 * atoms. <p>
 * See the following scripting commands:<br>
 * set solvent on/off (on defaults to 1.2 angstroms) <br>
 * set solvent 1.5 (choose another probe size) <br>
 * dots on/off <br>
 * color dots [color] <br>
 * color dotsConvex [color] <br>
 * color dotsSaddle [color] <br>
 * color dotsConcave [color] <br>
 *
 * The reference article for this implementation is: <br>
 * Analytical Molecular Surface Calculation, Michael L. Connolly,
 * Journal of Applied Crystalography, (1983) 15, 548-558 <p>
 *
 ****************************************************************/

class Dots extends Shape {

  DotsRenderer dotsRenderer;

  BitSet bsOn = new BitSet();
  short mad = 0;
  short lastMad = 0;
  float lastSolventRadius = 0;
  float maxRadius = 0;
  float scale = 1f;
  float setRadius = Float.MAX_VALUE;
  float addRadius = Float.MAX_VALUE;
  short surfaceColix = Graphics3D.GRAY;
  int dotsConvexMax; // the Max == the highest atomIndex with dots + 1
  int[][] dotsConvexMaps;
  final int nArcPoints = 9;  
  
  short[] colixesConvex;
  Vector3f[] geodesicVertices;
  int geodesicCount;
  int[] geodesicMap;
  Vector3f[] geodesicSolventVertices;
  int geodesicSolventCount;
  int[] geodesicSolventMap;
  int[] mapT;
  final static int[] mapNull = new int[0];
  final static int DOTS_MODE_DOTS = 0;
  final static int DOTS_MODE_SURFACE = 1;
  final static int DOTS_MODE_CALCONLY = 2;
  

  /* //out
   * 
  int cavityCount;
  Cavity[] cavities;
  int torusCount;
  Torus[] tori;
  Hashtable htTori;
  */
  
  BitSet bsSurface; 
  boolean calcDistanceOnly;
  boolean useVanderwaalsRadius;
  boolean disregardNeighbors = false;
  boolean onlySelectedDots = false;
  int indexI, indexJ, indexK;
  Atom atomI, atomJ, atomK;
  Point3f centerI, centerJ, centerK;
  float radiusI, radiusJ, radiusK;
  float radiusP, diameterP;
  float radiiIP2, radiiJP2, radiiKP2;
  float distanceIJ2;
  /*  //out/
   
  Torus torusIJ, torusIK;
  final Point3f baseIJK = new Point3f();
  final Point3f probeIJK = new Point3f();
  float heightIJK;

  boolean useBobsAlgorithm = true;
  */
  
  final Point3f pointT = new Point3f();
  //final Point3f pointT1 = new Point3f();
  boolean TIMINGS = true;
  
  long timeBeginExecution;
  long timeEndExecution;
  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  void initShape() {
    Logger.debug("init dots");
    dotsRenderer = (DotsRenderer)frame.getRenderer(JmolConstants.SHAPE_DOTS);
    geodesicVertices = dotsRenderer.geodesic.vertices;
    geodesicCount = geodesicVertices.length;
    geodesicMap = allocateBitmap(geodesicCount);
    geodesicSolventVertices = dotsRenderer.geodesicSolvent.vertices;
    geodesicSolventCount = geodesicSolventVertices.length;
    geodesicSolventMap = allocateBitmap(geodesicSolventCount);
    mapT = allocateBitmap(geodesicSolventCount);
  }

  boolean isSurface;
  boolean isCalcOnly;
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("Dots.setProperty: " + propertyName + " " + value);

    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;

    if ("init" == propertyName) {
      int mode = ((Integer) value).intValue();
      isSurface = (mode == DOTS_MODE_SURFACE);
      isCalcOnly = (mode == DOTS_MODE_CALCONLY);
      return;
    }
    if ("color" == propertyName) {
      setProperty("colorConvex", value, bs);
      return;
    }
    if ("colorSurface" == propertyName) {
      surfaceColix = Graphics3D.getColix(value);
      return;
    }
    if ("translucencySurface" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      surfaceColix = Graphics3D.setTranslucent(surfaceColix, isTranslucent);
      return;
    }
    if ("colorConvex" == propertyName) {
      if (colixesConvex == null)
        return;
      Logger.debug("Dots.setProperty('colorConvex')");
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i))
          colixesConvex[i] = ((colix != Graphics3D.UNRECOGNIZED) ? colix
              : viewer.getColixAtomPalette(atoms[i], (String) value));
      return;
    }
  }

  void setSize(int size, BitSet bsSelected) {
    // miguel 9 Sep 2005
    // this is a hack ...
    // if mad == 0 then turn it off
    //    1           van der Waals (dots) or +1.2, calconly)
    //   -1           ionic/covalent
    // 2 - 1001       (mad-1)/100 * van der Waals
    // 1002 - 11002    (mad - 1002)/1000 set radius 0.0 to 10.0 angstroms

    Logger.debug("Dots.setSize " + size);

/*    if (size == Integer.MIN_VALUE) {
      size = 1;
      calcDistanceOnly = true;
    }
*/

    bsSurface = new BitSet();
    boolean isVisible = true;

    short mad = (short) size;
    if (mad == 1 && isCalcOnly)
      mad = 1200 + 11002;
    if (mad < 0) { // ionic
      useVanderwaalsRadius = false;
      addRadius = Float.MAX_VALUE;
      setRadius = Float.MAX_VALUE;
      scale = 1;
    } else if (mad == 0) {
      isSurface = false;
      isCalcOnly = false;
      isVisible = false;
    } else if (mad == 1) {
      useVanderwaalsRadius = true;
      addRadius = Float.MAX_VALUE;
      setRadius = Float.MAX_VALUE;
      scale = 1;
    } else if (mad <= 1001) {
      useVanderwaalsRadius = true;
      addRadius = Float.MAX_VALUE;
      setRadius = Float.MAX_VALUE;
      scale = (mad - 1) / 100f;
    } else if (mad <= 11002) {
      useVanderwaalsRadius = false;
      addRadius = Float.MAX_VALUE;
      setRadius = (mad - 1002) / 1000f;
      scale = 1;
    } else if (mad <= 13002) {
      useVanderwaalsRadius = true;
      addRadius = (mad - 11002) / 1000f;
      setRadius = Float.MAX_VALUE;
      scale = 1;
    }
    maxRadius = frame.getMaxVanderwaalsRadius();
    float solventRadius = viewer.getCurrentSolventProbeRadius();
    if (addRadius == Float.MAX_VALUE)
      addRadius = (solventRadius != 0 ? solventRadius : 0);
//    if (showSurface) //done here -- rest is for renderer
  //    return;

    timeBeginExecution = System.currentTimeMillis();

    int atomCount = frame.atomCount;

    // combine current and selected set
    boolean newSet = (lastSolventRadius != addRadius || mad != 0
        && mad != lastMad || dotsConvexMax == 0);

    // for an solvent-accessible surface there is no torus/cavity issue. 
    // we just increment the atom radius and set the probe radius = 0;

    if (isVisible) {
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i) && !bsOn.get(i)) {
          bsOn.set(i);
          newSet = true;
        }
    } else {
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          bsOn.set(i, false);
    }
    
    if (!isCalcOnly)
      for (int i = atomCount; --i >= 0;)
        frame.atoms[i].setShapeVisibility(myVisibilityFlag, bsOn.get(i));
    if (newSet) {
      dotsConvexMax = 0;
      dotsConvexMaps = null;
      radiusP = 0;
      diameterP = 2 * radiusP;
      lastSolventRadius = addRadius;
    }
    // always delete old surfaces for selected atoms

    if (isVisible && dotsConvexMaps != null) {
      for (int i = atomCount; --i >= 0;)
        if (bsOn.get(i)) {
          dotsConvexMaps[i] = null;
        }
    }
    // now, calculate surface for selected atoms
    if (isVisible) {
      lastMad = mad;
      if (dotsConvexMaps == null) {
        dotsConvexMaps = new int[atomCount][];
        colixesConvex = new short[atomCount];
      }
      disregardNeighbors = (viewer.getDotSurfaceFlag() == false);
      onlySelectedDots = (viewer.getDotsSelectedOnlyFlag() == true);
      for (int i = atomCount; --i >= 0;)
        if (bsOn.get(i)) {
          //if (calcDistanceOnly)
            //setDistance(i, -1);
          setAtomI(i);
          getNeighbors();
          calcConvexMap();
          
          //if (calcDistanceOnly)
            //checkSurfaceAtom();
        }
    }
    if (dotsConvexMaps == null)
      dotsConvexMax = 0;
    else {
      // update this count to speed up dotsRenderer
      int i;
      for (i = atomCount; --i >= 0 && dotsConvexMaps[i] == null;) {
      }
      dotsConvexMax = i + 1;
    }
    
    frame.setSurfaceAtoms(bsSurface, bsOn);
    if (isCalcOnly) {
      dotsConvexMax = 0;
      dotsConvexMaps = null;
    }


/*    if (calcDistanceOnly) {
      Point3f vertex = new Point3f();
      for (int iAtom1 = dotsConvexMax; --iAtom1 >= 0;) {
        setAtomI(iAtom1);
        float combinedRadii = radiusI + radiusP;
        int[] map = dotsConvexMaps[iAtom1];
        for (int iPt = 12; --iPt >= 0;) {
          if (getBit(map, iPt)) {
            vertex.set(geodesicVertices[iPt]);
            vertex.scaleAdd(combinedRadii, centerI);
            for (int iAtom2 = 0; iAtom2 < atomCount; iAtom2++)
              if (!bsSurface.get(iAtom2)) {
                float d = frame.atoms[iAtom2].distance(vertex);
                setDistance(iAtom2, d);
              }
          }
        }
      }
    }
*/
    timeEndExecution = System.currentTimeMillis();
    Logger.debug("dots generation time = " + getExecutionWalltime());
  }

  void setDistance(int iAtom, float distance) {
    if (distance < 0) {
      frame.bfactor100s[iAtom] = 30000;
    } else {
      frame.bfactor100s[iAtom] = (short) (int) Math.min(
          frame.bfactor100s[iAtom], distance * 10000);
    }
  }

/*  void checkSurfaceAtom() {
    for (int i = 0; i < geodesicCount; i++)
      if (getBit(geodesicMap, i)) {
        setDistance(i, getAppropriateRadius(atomI));
        bsSurface.set(indexI);
        break;
      }
  }
*/
  float getAppropriateRadius(Atom atom) {
    float v = addRadius + (setRadius != Float.MAX_VALUE ? setRadius : (useVanderwaalsRadius
            ? atom.getVanderwaalsRadiusFloat()
            : atom.getBondingRadiusFloat()) * scale);
    return v;
  }

  void setAtomI(int indexI) {
    this.indexI = indexI;
    atomI = frame.atoms[indexI];
    centerI = atomI;
    radiusI = getAppropriateRadius(atomI);
    radiiIP2 = radiusI + radiusP;
    radiiIP2 *= radiiIP2;
  }
  
  void setNeighborJ(int indexNeighbor) {
    indexJ = neighborIndices[indexNeighbor];
    atomJ = neighbors[indexNeighbor];
    radiusJ = getAppropriateRadius(atomJ);
    radiiJP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerJ = neighborCenters[indexNeighbor];
    distanceIJ2 = centerI.distanceSquared(centerJ);
  }

  void setNeighborK(int indexNeighbor) {
    indexK = neighborIndices[indexNeighbor];
    centerK = neighborCenters[indexNeighbor];
    atomK = neighbors[indexNeighbor];
    radiusK = getAppropriateRadius(atomK);
    radiiKP2 = neighborPlusProbeRadii2[indexNeighbor];
  }
      
  
  
  void calcConvexMap() {
    calcConvexBits();
    int[] map = mapNull;
    int count = getMapStorageCount(geodesicMap);
    if (count > 0) {
      bsSurface.set(indexI);
      if (isSurface) {
        addIncompleteFaces(geodesicMap, dotsRenderer.geodesic);
        //add a second row as well.
        addIncompleteFaces(geodesicMap, dotsRenderer.geodesic);
      }
      count = getMapStorageCount(geodesicMap);
      map = new int[count];
      System.arraycopy(geodesicMap, 0, map, 0, count);
    }
    dotsConvexMaps[indexI] = map;
  }
  
  int getMapStorageCount(int[] map) {
    int indexLast;
    for (indexLast = map.length; --indexLast >= 0
        && map[indexLast] == 0;) {
    }
    return indexLast + 1;
  }

  void addIncompleteFaces(int[] points, DotsRenderer.Geodesic g) {
    clearBitmap(mapT);
    short[] faces = g.faceIndices;
    int len = faces.length;
    int maxPt = -1;
    for (int f = 0; f < len;) {
      short p1 = faces[f++];
      short p2 = faces[f++];
      short p3 = faces[f++];
      boolean ok1 = getBit(points, p1); 
      boolean ok2 = getBit(points, p2); 
      boolean ok3 = getBit(points, p3);
      if (! (ok1 || ok2 || ok3) || ok1 && ok2 && ok3)
        continue;
      // trick: DO show faces if ANY ONE vertex is missing
      if (!ok1) {
        setBit(mapT, p1);
        if (maxPt < p1)
          maxPt = p1;
      }
      if (!ok2) {
        setBit(mapT, p2);
        if (maxPt < p2)
          maxPt = p2;
      }
      if (!ok3) {
        setBit(mapT, p3);
        if (maxPt < p3)
          maxPt = p3;
      }
    }
    for (int i=0; i <= maxPt; i++) {
      if (getBit(mapT, i))
        setBit(points, i);
    }
  }


  Point3f centerT;
  void calcConvexBits() {
    setAllBits(geodesicMap, geodesicCount);
    float combinedRadii = radiusI + radiusP;
    if (neighborCount == 0)
      return;
    int faceTest;
    int p1, p2, p3;
    short[] faces = dotsRenderer.geodesic.faceIndices;
    int p4 = DotsRenderer.power4[dotsRenderer.geodesic.level - 1];
    boolean ok1, ok2, ok3;
    clearBitmap(mapT);
    Point3f[] vertexTest = dotsRenderer.vertexTest;
    for (int i = 0; i < 12; i++) {
      vertexTest[i].set(geodesicVertices[i]);
      vertexTest[i].scaleAdd(combinedRadii, centerI);      
    }    
    for (int f = 0; f < 20; f++) {
      faceTest = 0;
      p1 = faces[3 * p4 * (4 * f + 0)];
      p2 = faces[3 * p4 * (4 * f + 1)];
      p3 = faces[3 * p4 * (4 * f + 2)];
      for (int j = 0; j < neighborCount; j++) {
        float maxDist = neighborPlusProbeRadii2[j];
        centerT = neighborCenters[j];
        ok1 = vertexTest[p1].distanceSquared(centerT) >= maxDist;
        ok2 = vertexTest[p2].distanceSquared(centerT) >= maxDist;
        ok3 = vertexTest[p3].distanceSquared(centerT) >= maxDist;
        if (!ok1)
          clearBit(geodesicMap, p1);
        if (!ok2)
          clearBit(geodesicMap, p2);
        if (!ok3)
          clearBit(geodesicMap, p3);
        if (!ok1 && !ok2 && !ok3) {
          faceTest = -1;
          break;
        }
      }
      int kFirst = f * 12 * p4;
      int kLast = kFirst + 12 * p4;
      for (int k = kFirst; k < kLast; k++) {
        int vect = faces[k];
        if (getBit(mapT, vect) || ! getBit(geodesicMap, vect))
            continue;
        switch (faceTest) {
        case -1:
          //face full occluded
          clearBit(geodesicMap, vect);
          break;
        case 0:
          //face partially occluded
          for (int j = 0; j < neighborCount; j++) {
            float maxDist = neighborPlusProbeRadii2[j];
            centerT = neighborCenters[j];
            pointT.set(geodesicVertices[vect]);
            pointT.scaleAdd(combinedRadii, centerI);
            if (pointT.distanceSquared(centerT) < maxDist) {
              clearBit(geodesicMap, vect);
            }
          }
          break;
        case 1:
          //face is fully surface
        }
        setBit(mapT, vect);
      }
    }
  }

  int neighborCount;
  Atom[] neighbors = new Atom[16];
  int[] neighborIndices = new int[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  float[] neighborRadii2 = new float[16];
  
  void getNeighbors() {
    /*
    Logger.debug("Dots.getNeighbors radiusI=" + radiusI +
                       " diameterP=" + diameterP +
                       " maxVdw=" + frame.getMaxVanderwaalsRadius());
    */
    neighborCount = 0;
    if (disregardNeighbors)
      return;
    AtomIterator iter =
      frame.getWithinModelIterator(atomI, radiusI + diameterP +
                                   maxRadius);    
    while (iter.hasNext()) {
      Atom neighbor = iter.next();
      if (neighbor == atomI)
        continue;
      // only consider selected neighbors
      if (onlySelectedDots && !bsOn.get(neighbor.atomIndex))
        continue;
      float neighborRadius = getAppropriateRadius(neighbor);
      if (centerI.distance(neighbor) >
          radiusI + radiusP + radiusP + neighborRadius)
        continue;
      if (neighborCount == neighbors.length) {
        neighbors = (Atom[])ArrayUtil.doubleLength(neighbors);
        neighborIndices = ArrayUtil.doubleLength(neighborIndices);
        neighborCenters = (Point3f[])ArrayUtil.doubleLength(neighborCenters);
        neighborPlusProbeRadii2 = ArrayUtil.doubleLength(neighborPlusProbeRadii2);
        neighborRadii2 = ArrayUtil.doubleLength(neighborRadii2);
      }
      neighbors[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor;
      neighborIndices[neighborCount] = neighbor.atomIndex;
      float neighborPlusProbeRadii = neighborRadius + radiusP;
      neighborPlusProbeRadii2[neighborCount] =
        neighborPlusProbeRadii * neighborPlusProbeRadii;
      neighborRadii2[neighborCount] =
        neighborRadius * neighborRadius;
      ++neighborCount;
    }
  }
  
  final static int[] allocateBitmap(int count) {
    return new int[(count + 31) >> 5];
  }

  final static void setBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] |= 1 << (~i & 31);
  }

  final static void clearBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] &= ~(1 << (~i & 31));
  }

  final static boolean getBit(int[] bitmap, int i) {
    return (bitmap[(i >> 5)] << (i & 31)) < 0;
  }

  final static void setAllBits(int[] bitmap, int count) {
    int i = count >> 5;
    if ((count & 31) != 0)
      bitmap[i] = 0x80000000 >> (count - 1);
    while (--i >= 0)
      bitmap[i] = -1;
  }
  
  final static void clearBitmap(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }
  
  String showMap(int[] map) {
    String s = "showMap";
    int n = 0;
    int iDot = map.length << 5;
    while (--iDot >= 0)
      if (getBit(map, iDot)) {
        n++;
        s += " " + iDot;
      }
    s = n + " points:" + s;
    return s;
  }

  void setModelClickability() {
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = frame.atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      atom.clickabilityFlags |= myVisibilityFlag;
    }
  }
}
