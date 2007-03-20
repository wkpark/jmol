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

import org.jmol.g3d.Graphics3D;
import org.jmol.g3d.Geodesic3D;

import java.util.BitSet;
import java.util.Hashtable;

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
 * dots/geosurface +1.2
 * 
 * to provide the solvent-accessible surface.
 * 
 * A better rendering of the solvent accessible surface is given using
 * 
 * isosurface sasurface 1.2  
 * 
 * A discussion of molecular/solvent-accessible surfaces can be found at
 * http://www.netsci.org/Science/Compchem/feature14e.html
 * 
 * In March 2007, Bob refactored all Geodesic business that was here 
 * into the static class g3d.Geodesic3D, made GeoSurface an extension of Dots,
 * and generally similified the code. 
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
 * See g3d.Geodesic3D for more discussion of the implementation. <p>
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
 * torus (doughnut). The portion of the torus between the two points of
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

class Dots extends AtomShape {

  final static float SURFACE_DISTANCE_FOR_CALCULATION = 10f;

  BitSet bsOn = new BitSet();
  BitSet bsIgnore, bsSelected;
  
  static int MAX_LEVEL = 3;
  
  short mad = 0;
  short lastMad = 0;
  float lastSolventRadius = 0;
  float maxRadius = 0;
  float scale = 1f;
  float setRadius = Float.MAX_VALUE;
  float addRadius = Float.MAX_VALUE;
  int dotsConvexMax; // the Max == the highest atomIndex with dots + 1
  int[][] dotsConvexMaps;
  final int nArcPoints = 9;  
  
  int geodesicCount;
  int[] geodesicMap;
  int[] mapT;
  final static int[] mapNull = new int[0];
  final static int DOTS_MODE_DOTS = 0;
  final static int DOTS_MODE_SURFACE = 1;
  final static int DOTS_MODE_CALCONLY = 2;
  
  int argb;

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
  final Point3f pointT = new Point3f();
  boolean TIMINGS = true;
  
  long timeBeginExecution;
  long timeEndExecution;
  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  void initShape() {

    //these next two are for the geodesic fragment at a distance

    geodesicCount = Geodesic3D.vertexVectors.length;
    geodesicMap = allocateBitmap(geodesicCount);
    mapT = allocateBitmap(geodesicCount);
    translucentAllowed = false; //except for geosurface
    super.initShape();
  }

  boolean isSurface = false;
  boolean isCalcOnly;
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Dots.setProperty: " + propertyName + " " + value);
    }

    if ("init" == propertyName) {
      initialize(((Integer) value).intValue());
      return;
    }
    
    if ("translucency" == propertyName) {
      return;  // no translucent dots
    }

    if ("ignore" == propertyName) {
      bsIgnore = (BitSet)value;
      return;
    }

    if ("select" == propertyName) {
      bsSelected = (BitSet)value;
      return;
    }

    // next four are for serialization
    if ("radius" == propertyName) {
      radiusI = ((Float)value).floatValue();
      return;
    }
    if ("colorRGB" == propertyName) {
      argb = ((Integer)value).intValue();
      return;
    }
    if ("atom" == propertyName) {
      indexI = ((Integer)value).intValue();
      dotsConvexMax = Math.max(indexI + 1, dotsConvexMax);
      atoms[indexI].setShapeVisibility(myVisibilityFlag, true);
      return;  
    }
    if ("dots" == propertyName) {
      isActive = true;
      bs = (BitSet)value;
      setAllBits(geodesicMap, geodesicCount);
      for (int iDot = geodesicCount; --iDot >= 0;)
        if (!bs.get(iDot))
          clearBit(geodesicMap, iDot);
      if (dotsConvexMaps == null)
        dotsConvexMaps = new int[atomCount][];
      int[] map = mapNull;
      int count = getMapStorageCount(geodesicMap);
      if (count > 0) {
        map = new int[count];
        System.arraycopy(geodesicMap, 0, map, 0, count);
      }
      dotsConvexMaps[indexI] = map;
      if (mads == null)
        mads = new short[atomCount];
      mads[indexI] = (short) (radiusI * 1000f);
      if (colixes == null) {
        colixes = new short[atomCount];
        paletteIDs = new byte[atomCount];
      }
      colixes[indexI] = Graphics3D.getColix(argb);
      //all done!
      return;
    }    
    super.setProperty(propertyName, value, bs);
  }

  void setSuperProperty(String propertyName, Object value, BitSet bs) {
    super.setProperty(propertyName, value, bs);
  }
  
  float getRadius() {
    return Float.isNaN(setRadius)? 0 : setRadius;
  }
  
  void initialize(int mode) {
    isCalcOnly = (mode == DOTS_MODE_CALCONLY);
    if (isCalcOnly)
      bsOn.clear();
    bsIgnore = null;
    bsSelected = null;
    argb = 0;
    isActive = false;
  }
  
  void setSize(int size, BitSet bsSelected) {
    if (this.bsSelected != null)
      bsSelected = this.bsSelected;
    
    // if mad == 0 then turn it off
    //    1           van der Waals (dots) or +1.2, calconly)
    //   -1           ionic/covalent
    // 2 - 1001       (mad-1)/100 * van der Waals
    // 1002 - 11002    (mad - 1002)/1000 set radius 0.0 to 10.0 angstroms

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Dots.setSize " + size);
    }
    bsSurface = new BitSet();
    boolean isVisible = true;

    isActive = true;
    short mad = (short) size;
    if (mad == 1 && isCalcOnly) {
      addRadius = Float.MAX_VALUE;
      setRadius = SURFACE_DISTANCE_FOR_CALCULATION;
      scale = 1;
    } else if (mad < 0) { // ionic
      useVanderwaalsRadius = false;
      addRadius = Float.MAX_VALUE;
      setRadius = Float.MAX_VALUE;
      scale = 1;
    } else if (mad == 0) {
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
    maxRadius = !useVanderwaalsRadius ?  setRadius : frame.getMaxVanderwaalsRadius();
    float solventRadius = viewer.getCurrentSolventProbeRadius();
    if (addRadius == Float.MAX_VALUE)
      addRadius = (solventRadius != 0 ? solventRadius : 0);

    timeBeginExecution = System.currentTimeMillis();

    // combine current and selected set
    boolean newSet = (lastSolventRadius != addRadius || mad != 0
        && mad != lastMad || dotsConvexMax == 0 || isCalcOnly);

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
      for (int i = atomCount; --i >= 0;) {
          atoms[i].setShapeVisibility(myVisibilityFlag, bsOn.get(i));
      }
    if (newSet) {
      dotsConvexMax = 0;
      dotsConvexMaps = null;
      radiusP = 0;
      mads = null;
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
        colixes = new short[atomCount];
        paletteIDs = new byte[atomCount];
      }
      disregardNeighbors = (!isCalcOnly && viewer.getDotSurfaceFlag() == false);
      onlySelectedDots = (!isCalcOnly && viewer.getDotsSelectedOnlyFlag() == true);
      for (int i = atomCount; --i >= 0;)
        if (bsOn.get(i) && (bsIgnore == null || !bsIgnore.get(i))) {
          setAtomI(i);
          getNeighbors();
          calcConvexMap();
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
    currentPoints = null;
    timeEndExecution = System.currentTimeMillis();
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("dots generation time = " + getExecutionWalltime());
    }
  }

  float getAppropriateRadius(Atom atom) {
    if (mads != null)
      return mads[atom.atomIndex] / 1000f;
    float v = addRadius + (setRadius != Float.MAX_VALUE ? setRadius : (useVanderwaalsRadius
            ? atom.getVanderwaalsRadiusFloat()
            : atom.getBondingRadiusFloat()) * scale);
    return v;
  }

  void setAtomI(int indexI) {
    this.indexI = indexI;
    atomI = atoms[indexI];
    centerI = atomI;
    radiusI = getAppropriateRadius(atomI);
    radiiIP2 = radiusI + radiusP;
    radiiIP2 *= radiiIP2;
  }
  
  void calcConvexMap() {
    calcConvexBits();
    int[] map = mapNull;
    
    int count = getMapStorageCount(geodesicMap);
    if (count > 0) {
      bsSurface.set(indexI);
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

  Point3f centerT;
  
  //level = 3 for both
  final Point3f[] vertexTest = new Point3f[12];
  {
    for(int i = 0; i < 12; i++)
      vertexTest[i] = new Point3f();
  }

  static int[] power4 = {1, 4, 16, 64, 256};
  
  void calcConvexBits() {
    setAllBits(geodesicMap, geodesicCount);
    float combinedRadii = radiusI + radiusP;
    if (neighborCount == 0)
      return;
    int faceTest;
    int p1, p2, p3;
    short[] faces = Geodesic3D.faceVertexesArrays[MAX_LEVEL];
    
    int p4 = power4[MAX_LEVEL - 1];
    boolean ok1, ok2, ok3;
    clearBitmap(mapT);
    for (int i = 0; i < 12; i++) {
      vertexTest[i].set(Geodesic3D.vertexVectors[i]);
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
            pointT.set(Geodesic3D.vertexVectors[vect]);
            pointT.scaleAdd(combinedRadii, centerI);
            if (pointT.distanceSquared(centerT) < maxDist)
              clearBit(geodesicMap, vect);
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
    neighborCount = 0;
    if (disregardNeighbors)
      return;
    AtomIterator iter = frame.getWithinModelIterator(atomI, radiusI + diameterP
        + maxRadius);
    while (iter.hasNext()) {
      Atom neighbor = iter.next();
      if (neighbor == atomI || bsIgnore != null && bsIgnore.get(neighbor.atomIndex))
        continue;
      // only consider selected neighbors
      if (onlySelectedDots && !bsOn.get(neighbor.atomIndex))
        continue;
      float neighborRadius = getAppropriateRadius(neighbor);
      if (centerI.distance(neighbor) > radiusI + radiusP + radiusP
          + neighborRadius)
        continue;
      if (neighborCount == neighbors.length) {
        neighbors = (Atom[]) ArrayUtil.doubleLength(neighbors);
        neighborIndices = ArrayUtil.doubleLength(neighborIndices);
        neighborCenters = (Point3f[]) ArrayUtil.doubleLength(neighborCenters);
        neighborPlusProbeRadii2 = ArrayUtil
            .doubleLength(neighborPlusProbeRadii2);
        neighborRadii2 = ArrayUtil.doubleLength(neighborRadii2);
      }
      neighbors[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor;
      neighborIndices[neighborCount] = neighbor.atomIndex;
      float neighborPlusProbeRadii = neighborRadius + radiusP;
      neighborPlusProbeRadii2[neighborCount] = neighborPlusProbeRadii
          * neighborPlusProbeRadii;
      neighborRadii2[neighborCount] = neighborRadius * neighborRadius;
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
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      atom.clickabilityFlags |= myVisibilityFlag;
    }
  }

  String getShapeState() {
    if (dotsConvexMaps == null || isCalcOnly)
      return "";
    StringBuffer s = new StringBuffer();
    Hashtable temp = new Hashtable();
    int atomCount = viewer.getAtomCount();
    String type = (isSurface ? "geoSurface " : "dots ");
    for (int i = 0; i < atomCount; i++) {
      if (dotsConvexMaps[i] == null)
        continue;
      if (!isSurface && bsColixSet != null && bsColixSet.get(i))
          setStateInfo(temp, i, getColorCommand(type, paletteIDs[i], colixes[i]));
      BitSet bs = new BitSet();
      int[] map = dotsConvexMaps[i];
      int iDot = map.length << 5;
      while (--iDot >= 0)
        if (getBit(map, iDot))
          bs.set(iDot);
      appendCmd(s, type + i + " radius "
          + getAppropriateRadius(atoms[i]) + " "
          + StateManager.escape(bs));
    }
    s.append(getShapeCommands(temp, null, atomCount));
    return s.toString();
  }

  int getPointCount(int[] visibilityMap, int dotCount) {
    if (visibilityMap == null)
      return 0;
    int iDot = visibilityMap.length << 5;
    if (iDot > dotCount)
      iDot = dotCount;
    int n = 0;
    n = 0;
    while (--iDot >= 0)
      if (Dots.getBit(visibilityMap, iDot))
        n++;
    return n;
  }
  
  Point3f[] currentPoints;
  Point3f[] getPoints() {
    if (dotsConvexMaps == null) {
      initialize(DOTS_MODE_CALCONLY);
      setSize(1, bsSelected == null ? viewer.getSelectionSet() : bsSelected);
    }
    if (currentPoints != null)
      return currentPoints;
    int nPoints = 0;
    int dotCount = 42;
    for (int i = dotsConvexMax; --i >= 0;)
      nPoints += getPointCount(dotsConvexMaps[i], dotCount);
    Point3f[] points = new Point3f[nPoints];
    if (nPoints == 0)
      return points;
    nPoints = 0;
    for (int i = dotsConvexMax; --i >= 0;)
      if (dotsConvexMaps[i] != null) {
        Atom atom = atoms[i];
        int iDot = dotsConvexMaps[i].length << 5;
        if (iDot > dotCount)
          iDot = dotCount;
        while (--iDot >= 0)
          if (getBit(dotsConvexMaps[i], iDot)) {
            Point3f pt = new Point3f();
            pt.scaleAdd(setRadius, Geodesic3D.vertexVectors[iDot], atom);
            points[nPoints++] = pt;
          }
      }
    currentPoints = points;
    return points;
  }
}
