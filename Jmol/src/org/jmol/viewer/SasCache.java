/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import javax.vecmath.*;
import org.jmol.util.Bmp;

class SasCache {

  final Viewer viewer;

  SasCache(Viewer viewer, int atomCacheSize, int geodesicVertexCount) {
    this.viewer = viewer;
    allocAtomCache(atomCacheSize, geodesicVertexCount);
    // now that I am rendering in a different way, it is not clear
    // to me that this needs to be anything other than 1 ...
    // ... I think I only use 1 torus at a time
    allocTorusCache(2, MAX_TORUS_POINTS);
  }

  void clear() {
    clearAtomCache();
    clearTorusCache();
  }

  ////////////////////////////////////////////////////////////////
  // atom cache support
  ////////////////////////////////////////////////////////////////
  
  int atomCacheSize;
  int atomScreensLength;
  Point3i[][] atomCacheScreens;
  int[] atomCacheAtomIndexes;
  int atomCacheLruClock;
  int[] atomCacheLrus;
  Vector3f[] atomCacheTransformedGeodesicVectors;

  void allocAtomCache(int atomCacheSize, int geodesicVertexCount) {
    this.atomCacheSize = atomCacheSize;
    atomCacheScreens = new Point3i[atomCacheSize][];
    atomCacheAtomIndexes = new int[atomCacheSize];
    atomCacheLrus = new int[atomCacheSize];
    this.atomScreensLength = geodesicVertexCount;
    for (int i = atomCacheSize; --i >= 0; ) {
      atomCacheScreens[i] = allocScreens(atomScreensLength);
      atomCacheAtomIndexes[i] = -1;
      atomCacheLrus[i] = -1;
    }
    atomCacheLruClock = 0;
    atomCacheTransformedGeodesicVectors =
      viewer.g3d.getTransformedVertexVectors();
  }

  void clearAtomCache() {
    for (int i = atomCacheSize; --i >= 0; )
      atomCacheAtomIndexes[i] = atomCacheLrus[i] = -1;
  }

  void free() {
    for (int i = atomCacheSize; --i >= 0; ) {
      atomCacheScreens[i] = null;
      atomCacheAtomIndexes[i] = -1;
      atomCacheLrus[i] = -1;
    }
  }
  
  Point3i[] lookupAtomScreens(Atom atom, int[] vertexMap) {
    int atomIndex = atom.atomIndex;
    for (int i = atomCacheSize; --i >= 0; ) {
      if (atomCacheAtomIndexes[i] == atomIndex) {
        atomCacheLrus[i] = atomCacheLruClock++;
        return atomCacheScreens[i];
      }
    }
    int iOldest = 0;
    int lruOldest = atomCacheLrus[0];
    for (int i = atomCacheSize; --i > 0; ) { // only > 0
      if (atomCacheLrus[i] < lruOldest) {
        lruOldest = atomCacheLrus[i];
        iOldest = i;
      }
    }
    Point3i[] screens = atomCacheScreens[iOldest];
    calcAtomScreens(atom, vertexMap, screens);
    atomCacheAtomIndexes[iOldest] = atomIndex;
    atomCacheLrus[iOldest] = atomCacheLruClock++;
    return screens;
  }

  void touchAtomScreens(Point3i[] screens) {
    for (int i = atomCacheSize; --i >= 0; ) {
      if (screens == atomCacheScreens[i]) {
        atomCacheLrus[i] = atomCacheLruClock++;
        return;
      }
    }
    throw new NullPointerException();
  }

  void flushAtomScreens(Atom atom) {
    int atomIndex = atom.atomIndex;
    for (int i = atomCacheSize; --i >= 0; ) {
      if (atomCacheAtomIndexes[i] == atomIndex) {
        atomCacheAtomIndexes[i] = -1;
        atomCacheLrus[i] = -1;
        break;
      }
    }
  }

  void calcAtomScreens(Atom atom, int[] vertexMap, Point3i[] screens) {
    float radius = atom.getVanderwaalsRadiusFloat();
    int atomX = atom.getScreenX();
    int atomY = atom.getScreenY();
    int atomZ = atom.getScreenZ();
    float scaledRadius = viewer.scaleToScreen(atomZ, radius);
    for (int v = -1; (v = Bmp.nextSetBit(vertexMap, v + 1)) >= 0; ) {
      Vector3f tv = atomCacheTransformedGeodesicVectors[v];
      Point3i screen = screens[v];
      screen.x = atomX + (int)(scaledRadius * tv.x);
      screen.y = atomY - (int)(scaledRadius * tv.y); // y inverted on screen!
      screen.z = atomZ - (int)(scaledRadius * tv.z); // smaller z comes to me
    }
  }

  ////////////////////////////////////////////////////////////////
  // Torus cache support
  ////////////////////////////////////////////////////////////////

  int torusCacheSize;
  int torusScreensLength;
  Point3i[][] torusCacheScreens;
  Sasurface1.Torus[] torusCacheToruses;
  int torusCacheLruClock;
  int[] torusCacheLrus;
  Vector3f[] torusCacheTransformedGeodesicVectors;

  static final int MAX_TORUS_POINTS = Sasurface.MAX_TORUS_POINTS;

  Point3f[] torusPointsT;

  void allocTorusCache(int torusCacheSize, int torusScreensLength) {
    this.torusCacheSize = torusCacheSize;
    this.torusScreensLength = torusScreensLength;
    torusCacheScreens = new Point3i[torusCacheSize][];
    torusCacheToruses = new Sasurface1.Torus[torusCacheSize];
    torusCacheLrus = new int[torusCacheSize];
    for (int i = torusCacheSize; --i >= 0; ) {
      torusCacheScreens[i] = allocScreens(torusScreensLength);
      torusCacheToruses[i] = null;
      torusCacheLrus[i] = -1;
    }
    torusCacheLruClock = 0;
    torusPointsT = allocPoints(MAX_TORUS_POINTS);
  }

  void clearTorusCache() {
    for (int i = torusCacheSize; --i >= 0; ) {
      torusCacheToruses[i] = null;
      torusCacheLrus[i] = -1;
    }
  }

  Point3i[] lookupTorusScreens(Sasurface1.Torus torus) {
    for (int i = torusCacheSize; --i >= 0; ) {
      if (torusCacheToruses[i] == torus) {
        torusCacheLrus[i] = torusCacheLruClock++;
        return torusCacheScreens[i];
      }
    }
    int iOldest = 0;
    int lruOldest = torusCacheLrus[0];
    for (int i = torusCacheSize; --i > 0; ) { // only > 0
      if (torusCacheLrus[i] < lruOldest) {
        lruOldest = torusCacheLrus[i];
        iOldest = i;
      }
    }
    Point3i[] screens = torusCacheScreens[iOldest];
    torus.calcPoints(torusPointsT);
    torus.calcScreens(torusPointsT, screens);
    torusCacheToruses[iOldest] = torus;
    torusCacheLrus[iOldest] = torusCacheLruClock++;
    return screens;
  }

  Point3f[] allocPoints(int count) {
    Point3f[] points = new Point3f[count];
    for (int i = count; --i >= 0; )
      points[i] = new Point3f();
    return points;
  }

  Point3i[] allocScreens(int count) {
    Point3i[] screens = new Point3i[count];
    for (int i = count; --i >= 0; )
      screens[i] = new Point3i();
    return screens;
  }
}
