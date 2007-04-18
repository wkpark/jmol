package org.openscience.jvxl.calc;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

public interface MarchingReader {

  /**
   * getSurfacePointIndex is used by the Marching Cubes algorithm and 
   * must return a unique integer identifier for
   * a vertex created by the Marching Cube algorithm when it finds an 
   * edge. If a vertex is discarded, then Integer.MAX_VALUE should be returned.
   * 
   * the 3D coordinate of the point can be calculated using 
   * 
   * surfacePoint.scaleAdd(fraction, edgeVector, pointA);
   * 
   * where fraction is generally calculated as:
   * 
   *  fraction = (cutoff - valueA) / (valueB - valueA);
   *  if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
   *  fraction = (-cutoff - valueA) / (valueB - valueA);
   *  
   *  This method is also used by MarchingCubes to deliver the appropriate
   *  oblique planar coordinate to MarchingSquares for later contouring.
   * 
   * @param cutoff
   * @param isCutoffAbsolute
   * @param x
   * @param y
   * @param z
   * @param offset
   * @param valueA
   * @param valueB
   * @param pointA
   * @param edgeVector      vector from A to B
   * @param isContourType
   * @return                 new vertex index or Integer.MAX_VALUE
   */
  public abstract int getSurfacePointIndex(float cutoff,
                                           boolean isCutoffAbsolute, int x,
                                           int y, int z, Point3i offset,
                                           float valueA, float valueB,
                                           Point3f pointA, Vector3f edgeVector,
                                           boolean isContourType);

  /**
   * addVertexCopy is used by the Marching Squares algorithm to
   * uniquely identify a new vertex when an edge is crossed in the 2D plane.
   * 
   * The implementing method should COPY the Point3f using Point3f.set(). 
   *  
   * @param vertexXYZ
   * @param value
   * @return             new vertex index
   * 
   */
  public abstract int addVertexCopy(Point3f vertexXYZ, float value);

  /**
   * addTriangleCheck adds a triangle along with a 3-bit check indicating
   * which edges to draw in mesh mode: 1 (iA-iB) + 2 (iB-iC) + 4 (iC-iA) 
   * 
   * @param iA
   * @param iB
   * @param iC
   * @param check
   * @param isAbsolute
   */
  public abstract void addTriangleCheck(int iA, int iB, int iC, int check,
                                        boolean isAbsolute);
}
