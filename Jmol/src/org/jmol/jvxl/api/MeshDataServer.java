package org.jmol.jvxl.api;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.jvxl.data.MeshData;
import java.util.BitSet;

public interface MeshDataServer extends VertexDataServer {
  
  /*
   * An interface for interacting with 
   * the MarchingCubes and MarchingSquares classes 
   * as well as the SurfaceGenerator/VoxelReader classes
   * during and after surface generation
   * 
   * Isosurface is an example.
   * 
   * VoxelReader accepts vertexes from MarchingCubes/MarchingSquares
   * and then either consumes them or passes them on to Isosurface.
   * 
   * In addition, MeshData information is passed back and forth
   * via this mechanism.
   * 
   * This is crude. I would like to do it better.
   * 
   * Bob Hanson 20 Apr 2007
   * 
   */
  
  
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
   * @param vertexA [0:7]
   * @param vertexB [0:7]
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
                                           int vertexA, int vertexB, 
                                           float valueA, float valueB,
                                           Point3f pointA, Vector3f edgeVector,
                                           boolean isContourType);

  /**
   * addVertexCopy is used by the Marching Squares algorithm to
   * uniquely identify a new vertex when an edge is crossed in the 2D plane.
   * The implementing method should COPY the Point3f using Point3f.set(). 
   * 
   * The data consumer can use the association key to group this vertex with others
   * near the same gridpoint.
   *  
   * @param vertexXYZ
   * @param value
   * @param assocVertex       unique association vertex or -1
   * @return                  new vertex index
   * 
   */
  public abstract int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex);

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

  public abstract void invalidateTriangles();
  public abstract void fillMeshData(MeshData meshData, int mode);
  public abstract void notifySurfaceGenerationCompleted();
  public abstract void notifySurfaceMappingCompleted();
  public abstract Point3f[] calculateGeodesicSurface(BitSet bsSelected, float envelopeRadius);  
}
