/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.protein.ProteinProp;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

/****************************************************************
 * The Dots and DotsRenderer classes implement vanderWaals and Connolly
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

public class Dots {

  JmolViewer viewer;
  JmolFrame frame;
  DotsRenderer dotsRenderer;

  int dotsConvexCount;
  int[][] dotsConvexMaps;

    int cavityCount;
    Cavity[] cavities;
  int torusCount;
  Torus[] tori;
  Hashtable htTori = new Hashtable();

  Dots(JmolViewer viewer, JmolFrame frame, DotsRenderer dotsRenderer) {
    this.viewer = viewer;
    this.frame = frame;
    this.dotsRenderer = dotsRenderer;
  }

  public void setDotsOn(boolean dotsOn, BitSet bsSelected) {
    int atomShapeCount = frame.atomShapeCount;
    if (dotsOn) {
      AtomShape[] atomShapes = frame.atomShapes;
      if (dotsConvexMaps == null)
        dotsConvexMaps = new int[atomShapeCount][];
      else if (dotsConvexMaps.length < atomShapeCount) {
        int[][] t = new int[atomShapeCount][];
        System.arraycopy(dotsConvexMaps, 0, t, 0,
                         dotsConvexMaps.length);
        dotsConvexMaps = t;
      }
      for (int i = atomShapeCount; --i >= 0; )
        if (bsSelected.get(i) && dotsConvexMaps[i] == null)
          dotsConvexMaps[i] = calcConvexMap(atomShapes[i]);
      calcTori();
      calcCavities();
    } else {
      for (int i = atomShapeCount; --i >= 0; )
        if (bsSelected.get(i))
          dotsConvexMaps[i] = null;
      tori = null;
      cavities = null;
    }
    int iLast = dotsConvexMaps.length;
    while (--iLast > 0 && dotsConvexMaps[iLast] == null)
      {}
    dotsConvexCount = iLast + 1;
  }

  int[] calcConvexMap(AtomShape atom) {
    float vdwRadius = atom.getVanderwaalsRadius();
    float probeRadius = viewer.getSolventProbeRadius();
    getNeighbors(atom, vdwRadius, probeRadius);
    calcConvexBits(atom.getPoint3f(), vdwRadius, probeRadius);
    int indexLast;
    for (indexLast = bitmap.length;
         --indexLast >= 0 && bitmap[indexLast] == 0; )
      {}
    if (indexLast == -1)
      return null;
    int count = indexLast + 1;
    int[] visibilityMap = new int[indexLast + 1];
    System.arraycopy(bitmap, 0, visibilityMap, 0, count);
    return visibilityMap;
  }

  int[] bitmap;
  Point3f pointT = new Point3f();

  void calcConvexBits(Point3f myCenter, float vdwRadius, float probeRadius) {
    Vector3f[] vertices = dotsRenderer.geodesic.vertices;
    int dotCount = vertices.length;
    if (bitmap == null)
      bitmap = allocateBitmap(dotCount);
    setAllBits(bitmap, dotCount);
    if (neighborCount == 0)
      return;
    int iNeighborLast = 0;
    float fullRadius = vdwRadius + probeRadius;
    for (int iDot = dotCount; --iDot >= 0; ) {
      pointT.set(vertices[iDot]);
      pointT.scaleAdd(fullRadius, myCenter);
      int iStart = iNeighborLast;
      do {
        if (pointT.distanceSquared(neighborCenters[iNeighborLast]) <
            neighborPlusProbeRadii2[iNeighborLast]) {
          clearBit(bitmap, iDot);
          break;
        }
        iNeighborLast = (iNeighborLast + 1) % neighborCount;
      } while (iNeighborLast != iStart);
    }
  }

  // I have no idea what this number should be
  int neighborCount;
  AtomShape[] neighbors = new AtomShape[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  
  void getNeighbors(AtomShape atom, float vdwRadius, float probeRadius) {
    AtomShapeIterator iter =
      atom.frame.getWithinIterator(atom, vdwRadius + probeRadius*2 +
                                   atom.frame.getMaxVanderwaalsRadius());
    neighborCount = 0;
    while (iter.hasNext()) {
      AtomShape neighbor = iter.next();
      if (neighbor != atom) {
        if (neighborCount == neighbors.length) {
          AtomShape[] neighborsNew = new AtomShape[2 * neighborCount];
          System.arraycopy(neighbors, 0, neighborsNew, 0, neighborCount);
          neighbors = neighborsNew;
          Point3f[] centersNew = new Point3f[2 * neighborCount];
          System.arraycopy(neighborCenters, 0, centersNew, 0, neighborCount);
          neighborCenters = centersNew;
          float[] radiiNew = new float[2 * neighborCount];
          System.arraycopy(neighborPlusProbeRadii2, 0, radiiNew, 0,
			   neighborCount);
          neighborPlusProbeRadii2 = radiiNew;
        }
        neighbors[neighborCount] = neighbor;
        neighborCenters[neighborCount] = neighbor.point3f;
        float combinedRadius = (neighbor.getVanderwaalsRadius() + probeRadius);
        neighborPlusProbeRadii2[neighborCount] = combinedRadius*combinedRadius;
        ++neighborCount;
      }
    }
    /*
      System.out.println("neighborsFound=" + neighborCount);
      System.out.println("myVdwRadius=" + myVdwRadius +
      " maxVdwRadius=" + maxVdwRadius +
      " distMax=" + (myVdwRadius + maxVdwRadius));
      Point3f me = atom.getPoint3f();
      for (int i = 0; i < neighborCount; ++i) {
      System.out.println(" dist=" +
      me.distance(neighbors[i].getPoint3f()));
      }
    */
  }

    void calcTori() {
	float probeRadius = viewer.getSolventProbeRadius();
	if (probeRadius == 0)
	    return;
	float probeDiameter = 2 * probeRadius;
	tori = new Torus[32];
	torusCount = 0;
	AtomShape[] atomShapes = frame.atomShapes;
	for (int i = frame.atomShapeCount; --i >= 0; ) {
	    AtomShape atomI = atomShapes[i];
	    float vdwRadiusI = atomI.getVanderwaalsRadius();
	    getNeighbors(atomI, vdwRadiusI, probeRadius);
	    for (int iJ = neighborCount; --iJ >= 0; ) {
		AtomShape atomJ = neighbors[iJ];
		int j = atomJ.getAtomIndex();
		if (i >= j) {
		    continue;
		}
		Torus torusIJ = getTorus(atomI, atomJ);
		if (torusIJ == null)
		    continue;
		calcTorusProbeMap(torusIJ);
		if (torusIJ.probeMap == null)
		    continue;
		if (torusCount == tori.length) {
		    Torus[] t = new Torus[torusCount * 2];
		    System.arraycopy(tori, 0, t, 0, torusCount);
		    tori = t;
		}
		tori[torusCount++] = torusIJ;
	    }
	}
    }

    final static int torusProbePositionCount = 64;
    Matrix3f matrixT = new Matrix3f();
    AxisAngle4f aaT = new AxisAngle4f();
    final int[] probeMapT = allocateBitmap(torusProbePositionCount);

    void calcTorusProbeMap(Torus torus) {
	int[] probeMap = probeMapT;
	setAllBits(probeMap, torusProbePositionCount);
	float stepAngle = 2 * (float)Math.PI / torusProbePositionCount;
	aaT.set(torus.axisVector, 0);
	int iNeighborLast = 0;
	for (int a = torusProbePositionCount; --a >= 0; ) {
	    aaT.angle = a * stepAngle;
	    matrixT.set(aaT);
	    matrixT.transform(torus.radialVector, pointT);
	    pointT.add(torus.center);

	    int iStart = iNeighborLast;
	    do {
		if (neighbors[iNeighborLast].getAtomIndex() != torus.j) {
		    if (pointT.distanceSquared(neighborCenters[iNeighborLast]) <
			neighborPlusProbeRadii2[iNeighborLast]) {
			clearBit(probeMap, a);
			break;
		    }
		}
		iNeighborLast = (iNeighborLast + 1) % neighborCount;
	    } while (iNeighborLast != iStart);
	}
	int indexLast;
	for (indexLast = probeMap.length;
	     --indexLast >= 0 && probeMap[indexLast] == 0; )
	    {}
	if (indexLast == -1) {
	    torus.probeMap = null;
	} else {
	    ++indexLast;
	    torus.probeMap = new int[indexLast];
	    while (--indexLast >= 0)
		torus.probeMap[indexLast] = probeMap[indexLast];
	}
    }

    Vector3f vectorT = new Vector3f();

  class Torus {
    int i, j;
    Point3f center;
    float radius;
      Vector3f axisVector;
      Vector3f radialVector;
      Vector3f tangentVector;
      Vector3f outerRadial;
      float outerAngle;
      int[] probeMap;

    Torus(AtomShape atomI, AtomShape atomJ,
	  Point3f center, float radius, Vector3f axisVector) {
      this.i = atomI.getAtomIndex();
      this.j = atomJ.getAtomIndex();
      this.center = center;
      this.radius = radius;
      this.axisVector = axisVector;
      float probeRadius = viewer.getSolventProbeRadius();

      axisVector.normalize();
      axisVector.scale(probeRadius);

      if (axisVector.x == 0)
	  radialVector = new Vector3f(radius, 0, 0);
      else if (axisVector.y == 0)
	  radialVector = new Vector3f(0, radius, 0);
      else if (axisVector.z == 0)
	  radialVector = new Vector3f(0, 0, radius);
      else {
	  radialVector = new Vector3f(-axisVector.y, axisVector.x, 0);
	  radialVector.normalize();
	  radialVector.scale(radius);
      }

      tangentVector = new Vector3f();
      tangentVector.cross(radialVector, axisVector);
      tangentVector.normalize();

      pointT.set(center);
      pointT.add(radialVector);

      outerRadial = new Vector3f(atomI.point3f);
      outerRadial.sub(pointT);
      outerRadial.normalize();
      outerRadial.scale(probeRadius);

      vectorT.set(atomJ.point3f);
      vectorT.sub(pointT);
      outerAngle = vectorT.angle(outerRadial);
    }
  }

  final static Boolean boxedFalse = new Boolean(false);

  Torus getTorus(AtomShape atomI, AtomShape atomJ) {
    int i = atomI.getAtomIndex();
    int j = atomJ.getAtomIndex();
    if (i >= j)
      throw new NullPointerException();
    Long key = new Long(((long)i << 32) + j);
    Object value = htTori.get(key);
    if (value != null) {
      if (value instanceof Torus) {
        Torus torus = (Torus)value;
        return torus;
      }
      return null;
    }
    float radius = calcTorusRadius(atomI, atomJ);
    if (radius == 0) {
      htTori.put(key, boxedFalse);
      return null;
    }
    Point3f center = calcTorusCenter(atomI, atomJ);
    Vector3f axisVector = new Vector3f(atomI.point3f);
    axisVector.sub(center);
    Torus torus = new Torus(atomI, atomJ, center, radius, axisVector);
    htTori.put(key, torus);
    return torus;
  }

  Point3f calcTorusCenter(AtomShape atomI, AtomShape atomJ) {
    float rI = atomI.getVanderwaalsRadius();
    float rJ = atomJ.getVanderwaalsRadius();
    float rP = viewer.getSolventProbeRadius();
    float rIrP2 = rI + rP;
    rIrP2 *= rIrP2;
    float rJrP2 = rJ + rP;
    rJrP2 *= rJrP2;
    float distIJ2 = atomI.point3f.distance(atomJ.point3f);
    distIJ2 *= distIJ2;
    float t = (rIrP2 - rJrP2) / distIJ2;

    Point3f torusCenter = new Point3f(atomJ.point3f);
    torusCenter.sub(atomI.point3f);
    torusCenter.scale(t);
    torusCenter.add(atomI.point3f);
    torusCenter.add(atomJ.point3f);
    torusCenter.scale(0.5f);
    /*
    System.out.println("calcTorusCenter i=" + atomI.point3f.x + "," +
                       atomI.point3f.y + "," + atomI.point3f.z + "  j=" +
                       atomJ.point3f.x + "," + atomJ.point3f.y + "," +
                       atomJ.point3f.z + "  center=" +
                       torusCenter.x + "," + torusCenter.y + "," +
                       torusCenter.z);
    */
    return torusCenter;
  }

  float calcTorusRadius(AtomShape atomI, AtomShape atomJ) {
    float rI = atomI.getVanderwaalsRadius();
    float rJ = atomJ.getVanderwaalsRadius();
    float rP = viewer.getSolventProbeRadius();
    float distIJ = atomI.point3f.distance(atomJ.point3f);
    float distIJ2 = distIJ*distIJ;
    float t1 = rI + rJ + 2*rP;
    float t2 = t1*t1 - distIJ2;
    float diff = rI - rJ;
    float t3 = distIJ2 - diff*diff;
    if (t2 <= 0 || t3 <= 0 || distIJ == 0)
      return 0;
    return (float)(0.5 * Math.sqrt(t2) * Math.sqrt(t3) / distIJ);
  }


    void calcCavities() {
	System.out.println("calcCavities()");
	float probeRadius = viewer.getSolventProbeRadius();
	if (probeRadius == 0)
	    return;
	float probeDiameter = 2 * probeRadius;
	cavities = new Cavity[32];
	cavityCount = 0;
	AtomShape[] atomShapes = frame.atomShapes;
	for (int i = frame.atomShapeCount; --i >= 0; ) {
	    AtomShape atomI = atomShapes[i];
	    float vdwRadiusI = atomI.getVanderwaalsRadius();
	    getNeighbors(atomI, vdwRadiusI, probeRadius);
	    for (int iJ = neighborCount; --iJ >= 0; ) {
		AtomShape atomJ = neighbors[iJ];
		int j = atomJ.getAtomIndex();
		if (i >= j)
		    continue;
		float vdwRadiusJ = atomJ.getVanderwaalsRadius();
		float distIJ = atomI.point3f.distance(atomJ.point3f);
		if (distIJ >= (vdwRadiusI + probeDiameter + vdwRadiusJ))
		    continue;
		for (int iK = iJ; --iK >= 0; ) {
		    AtomShape atomK = neighbors[iK];
		    if (j >= atomK.getAtomIndex())
			continue;
		    float vdwRadiusK = atomK.getVanderwaalsRadius();
		    float distIK = atomI.point3f.distance(atomK.point3f);
		    if (distIK >= (vdwRadiusI + probeDiameter + vdwRadiusK))
			continue;
		    float distJK = atomJ.point3f.distance(atomK.point3f);
		    if (distJK >= (vdwRadiusJ + probeDiameter + vdwRadiusK))
			continue;
		    if (cavityCount == cavities.length) {
			Cavity[] t = new Cavity[2 * cavityCount];
			System.arraycopy(cavities, 0, t, 0, cavityCount);
			cavities = t;
		    }
		    cavities[cavityCount++] = new Cavity(atomI, atomJ, atomK);
		}
	    }
	}
	System.out.println("cavityCount=" + cavityCount);
    }

    Vector3f uIJ = new Vector3f();
    Vector3f uIK = new Vector3f();
    Vector3f uIJK = new Vector3f();
    Vector3f uTB = new Vector3f();

    class Cavity {
	AtomShape atomI, atomJ, atomK;
	Torus torusIJ, torusIK, torusJK;
	Point3f baseIJK;
	float heightIJK;
	Point3f pointAbove;
	Point3f pointBelow;
	

	Cavity(AtomShape atomI, AtomShape atomJ, AtomShape atomK) {
	    this.atomI = atomI; this.atomJ = atomJ; this.atomK = atomK;
	    torusIJ = getTorus(atomI, atomJ);
	    torusIK = getTorus(atomI, atomK);
	    torusJK = getTorus(atomJ, atomK);
	    calcBase();
	    calcHeight();
	    if (heightIJK > 0) {
		pointAbove = new Point3f(uIJK);
		pointAbove.scaleAdd(heightIJK, baseIJK);
		pointBelow = new Point3f(uIJK);
		pointBelow.scaleAdd(-heightIJK, baseIJK);
		System.out.println("pointAbove=" + pointAbove +
				   " pointBelow=" + pointBelow);
	    }
	}

	void calcBase() {
	    uIJ.sub(atomJ.point3f, atomI.point3f);
	    uIJ.normalize();

	    uIK.sub(atomK.point3f, atomI.point3f);
	    uIK.normalize();

	    float angleJIK = uIJ.angle(uIK);
	    
	    uIJK.cross(uIJ, uIK);
	    uIJK.normalize();

	    uTB.cross(uIJK, uIJ);

	    vectorT.sub(torusIK.center, torusIJ.center);
	    baseIJK = new Point3f(uTB);
	    baseIJK.scaleAdd(uIK.dot(vectorT)/(float)Math.sin(angleJIK),
			     torusIJ.center);
	    System.out.println("baseIJK=" + baseIJK);

	}

	void calcHeight() {
	    float rI = atomI.getVanderwaalsRadius();
	    float rP = viewer.getSolventProbeRadius();
	    float hypotenuse = rI + rP;
	    float hypotenuse2 = hypotenuse*hypotenuse;
	    vectorT.sub(baseIJK, atomI.point3f);
	    float baseLength2 = vectorT.lengthSquared();
	    float height2 = hypotenuse2 - baseLength2;
	    heightIJK = height2 <= 0 ? 0 : (float)Math.sqrt(height2);
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
}
