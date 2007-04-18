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
   * surfacePoint must be filled with the coordinated of the surface point.
   * In the case of a Jvxl file, the fractional distance is simply 
   * read from the file and not calculated from the surface data, because the
   * surface data values do not exist any longer.  
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
   * @param surfacePoint    intersection point
   * @param isContourType
   * @return                 new vertex index or Integer.MAX_VALUE
   */
  public abstract int getSurfacePointIndex(float cutoff,
                                           boolean isCutoffAbsolute, int x,
                                           int y, int z, Point3i offset,
                                           float valueA, float valueB,
                                           Point3f pointA, Vector3f edgeVector,
                                           Point3f surfacePoint,
                                           boolean isContourType);

  /**
   * addContourVertex is used by the Marching Squares algorithm to
   * uniquely identify a new vertex when an edge is crossed in the 2D plane
   *  
   * @param vertexXYZ
   * @param value
   * @return             new vertex index
   * 
   */
  public abstract int addContourVertex(Point3f vertexXYZ, float value);

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
