package org.jmol.util;

import javajs.util.AU;
import javajs.util.List;

import org.jmol.api.Triangulator;
import org.jmol.java.BS;

import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;

public class TriangleData implements Triangulator {

  /*
   * the triangle data from Marching Cubes
   * 
   * Used for Marching Cubes as well as calculating the intersection of unit cells 
   * and bounding boxes with planes
   * 
   */
  protected static final int[] Pwr2 = new int[] { 1, 2, 4, 8, 16, 32, 64, 128,
    256, 512, 1024, 2048 };


  /*                     Y 
   *                      4 --------4--------- 5                     +z --------4--------- +yz+z                  
   *                     /|                   /|                     /|                   /|
   *                    / |                  / |                    / |                  / |
   *                   /  |                 /  |                   /  |                 /  |
   *                  7   8                5   |                  7   8                5   |
   *                 /    |               /    9                 /    |               /    9
   *                /     |              /     |                /     |              /     |
   *               7 --------6--------- 6      |            +z+1 --------6--------- +yz+z+1|
   *               |      |             |      |               |      |             |      |
   *               |      0 ---------0--|----- 1    X          |      0 ---------0--|----- +yz    X(outer)    
   *               |     /              |     /                |     /              |     /
   *              11    /               10   /                11    /               10   /
   *               |   3                |   1                  |   3                |   1
   *               |  /                 |  /                   |  /                 |  /
   *               | /                  | /                    | /                  | /
   *               3 ---------2-------- 2                     +1 ---------2-------- +yz+1
   *              Z                                           Z (inner)
   * 
   *                                                              streaming data offsets
   *  
   *   
   */

  private final static int[][] fullCubePolygon = new int[][] {
    { 0, 4, 5, 3 }, { 5, 1, 0, 3 }, // back
    { 1, 5, 6, 2 }, { 6, 2, 1, 3 }, 
    { 2, 6, 7, 2 }, { 7, 3, 2, 3 }, // front
    { 3, 7, 4, 2 }, { 4, 0, 3, 2 },
    { 6, 5, 4, 0 }, { 4, 7, 6, 0 }, // top
    { 0, 1, 2, 0 }, { 2, 3, 0, 0 }, // bottom
  };
  
  protected final static P3i[] cubeVertexOffsets = { 
    P3i.new3(0, 0, 0), //0 pt
    P3i.new3(1, 0, 0), //1 pt + yz
    P3i.new3(1, 0, 1), //2 pt + yz + 1
    P3i.new3(0, 0, 1), //3 pt + 1
    P3i.new3(0, 1, 0), //4 pt + z
    P3i.new3(1, 1, 0), //5 pt + yz + z
    P3i.new3(1, 1, 1), //6 pt + yz + z + 1
    P3i.new3(0, 1, 1)  //7 pt + z + 1 
  };

  protected final static byte edgeVertexes[] = { 
    0, 1, 1, 2, 2, 3, 3, 0, 4, 5,
  /*0     1     2     3     4  */
    5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };
  /*5     6     7     8     9     10    11 */
  
  
  
  /* the new triangle table. Fourth number in each ABC set is b3b2b1, where
   * b1 = 1 for AB, b2 = 1 for BC, b3 = 1 for CA lines to be drawn for mesh
   * 
   * So, for example: 
   
   1, 8, 3, 6
   
   * 6 is 110 in binary, so b3 = 1, b2 = 1, b1 = 0.
   * b1 refers to the 18 edge, b2 refers to the 83 edge, 
   * and b3 refers to the 31 edge. The 31 and 83, but not 18 edges 
   * should be drawn for a mesh. On the cube above, you can see
   * that the 18 edges is in the interior of the cube. That's why we
   * don't render it with a mesh.
   
   
   Bob Hanson, 3/29/2007
   
   */

  protected final static byte[][] triangleTable2 = { null, { 0, 8, 3, 7 },
      { 0, 1, 9, 7 }, { 1, 8, 3, 6, 9, 8, 1, 5 }, { 1, 2, 10, 7 },
      { 0, 8, 3, 7, 1, 2, 10, 7 }, { 9, 2, 10, 6, 0, 2, 9, 5 },
      { 2, 8, 3, 6, 2, 10, 8, 1, 10, 9, 8, 3 }, { 3, 11, 2, 7 },
      { 0, 11, 2, 6, 8, 11, 0, 5 }, { 1, 9, 0, 7, 2, 3, 11, 7 },
      { 1, 11, 2, 6, 1, 9, 11, 1, 9, 8, 11, 3 }, { 3, 10, 1, 6, 11, 10, 3, 5 },
      { 0, 10, 1, 6, 0, 8, 10, 1, 8, 11, 10, 3 },
      { 3, 9, 0, 6, 3, 11, 9, 1, 11, 10, 9, 3 }, { 9, 8, 10, 5, 10, 8, 11, 6 },
      { 4, 7, 8, 7 }, { 4, 3, 0, 6, 7, 3, 4, 5 }, { 0, 1, 9, 7, 8, 4, 7, 7 },
      { 4, 1, 9, 6, 4, 7, 1, 1, 7, 3, 1, 3 }, { 1, 2, 10, 7, 8, 4, 7, 7 },
      { 3, 4, 7, 6, 3, 0, 4, 3, 1, 2, 10, 7 },
      { 9, 2, 10, 6, 9, 0, 2, 3, 8, 4, 7, 7 },
      { 2, 10, 9, 3, 2, 9, 7, 0, 2, 7, 3, 6, 7, 9, 4, 6 },
      { 8, 4, 7, 7, 3, 11, 2, 7 }, { 11, 4, 7, 6, 11, 2, 4, 1, 2, 0, 4, 3 },
      { 9, 0, 1, 7, 8, 4, 7, 7, 2, 3, 11, 7 },
      { 4, 7, 11, 3, 9, 4, 11, 1, 9, 11, 2, 2, 9, 2, 1, 6 },
      { 3, 10, 1, 6, 3, 11, 10, 3, 7, 8, 4, 7 },
      { 1, 11, 10, 6, 1, 4, 11, 0, 1, 0, 4, 3, 7, 11, 4, 5 },
      { 4, 7, 8, 7, 9, 0, 11, 1, 9, 11, 10, 6, 11, 0, 3, 6 },
      { 4, 7, 11, 3, 4, 11, 9, 4, 9, 11, 10, 6 }, { 9, 5, 4, 7 },
      { 9, 5, 4, 7, 0, 8, 3, 7 }, { 0, 5, 4, 6, 1, 5, 0, 5 },
      { 8, 5, 4, 6, 8, 3, 5, 1, 3, 1, 5, 3 }, { 1, 2, 10, 7, 9, 5, 4, 7 },
      { 3, 0, 8, 7, 1, 2, 10, 7, 4, 9, 5, 7 },
      { 5, 2, 10, 6, 5, 4, 2, 1, 4, 0, 2, 3 },
      { 2, 10, 5, 3, 3, 2, 5, 1, 3, 5, 4, 2, 3, 4, 8, 6 },
      { 9, 5, 4, 7, 2, 3, 11, 7 }, { 0, 11, 2, 6, 0, 8, 11, 3, 4, 9, 5, 7 },
      { 0, 5, 4, 6, 0, 1, 5, 3, 2, 3, 11, 7 },
      { 2, 1, 5, 3, 2, 5, 8, 0, 2, 8, 11, 6, 4, 8, 5, 5 },
      { 10, 3, 11, 6, 10, 1, 3, 3, 9, 5, 4, 7 },
      { 4, 9, 5, 7, 0, 8, 1, 5, 8, 10, 1, 2, 8, 11, 10, 3 },
      { 5, 4, 0, 3, 5, 0, 11, 0, 5, 11, 10, 6, 11, 0, 3, 6 },
      { 5, 4, 8, 3, 5, 8, 10, 4, 10, 8, 11, 6 }, { 9, 7, 8, 6, 5, 7, 9, 5 },
      { 9, 3, 0, 6, 9, 5, 3, 1, 5, 7, 3, 3 },
      { 0, 7, 8, 6, 0, 1, 7, 1, 1, 5, 7, 3 }, { 1, 5, 3, 5, 3, 5, 7, 6 },
      { 9, 7, 8, 6, 9, 5, 7, 3, 10, 1, 2, 7 },
      { 10, 1, 2, 7, 9, 5, 0, 5, 5, 3, 0, 2, 5, 7, 3, 3 },
      { 8, 0, 2, 3, 8, 2, 5, 0, 8, 5, 7, 6, 10, 5, 2, 5 },
      { 2, 10, 5, 3, 2, 5, 3, 4, 3, 5, 7, 6 },
      { 7, 9, 5, 6, 7, 8, 9, 3, 3, 11, 2, 7 },
      { 9, 5, 7, 3, 9, 7, 2, 0, 9, 2, 0, 6, 2, 7, 11, 6 },
      { 2, 3, 11, 7, 0, 1, 8, 5, 1, 7, 8, 2, 1, 5, 7, 3 },
      { 11, 2, 1, 3, 11, 1, 7, 4, 7, 1, 5, 6 },
      { 9, 5, 8, 5, 8, 5, 7, 6, 10, 1, 3, 3, 10, 3, 11, 6 },
      { 5, 7, 0, 1, 5, 0, 9, 6, 7, 11, 0, 1, 1, 0, 10, 5, 11, 10, 0, 1 },
      { 11, 10, 0, 1, 11, 0, 3, 6, 10, 5, 0, 1, 8, 0, 7, 5, 5, 7, 0, 1 },
      { 11, 10, 5, 3, 7, 11, 5, 5 }, { 10, 6, 5, 7 },
      { 0, 8, 3, 7, 5, 10, 6, 7 }, { 9, 0, 1, 7, 5, 10, 6, 7 },
      { 1, 8, 3, 6, 1, 9, 8, 3, 5, 10, 6, 7 }, { 1, 6, 5, 6, 2, 6, 1, 5 },
      { 1, 6, 5, 6, 1, 2, 6, 3, 3, 0, 8, 7 },
      { 9, 6, 5, 6, 9, 0, 6, 1, 0, 2, 6, 3 },
      { 5, 9, 8, 3, 5, 8, 2, 0, 5, 2, 6, 6, 3, 2, 8, 5 },
      { 2, 3, 11, 7, 10, 6, 5, 7 }, { 11, 0, 8, 6, 11, 2, 0, 3, 10, 6, 5, 7 },
      { 0, 1, 9, 7, 2, 3, 11, 7, 5, 10, 6, 7 },
      { 5, 10, 6, 7, 1, 9, 2, 5, 9, 11, 2, 2, 9, 8, 11, 3 },
      { 6, 3, 11, 6, 6, 5, 3, 1, 5, 1, 3, 3 },
      { 0, 8, 11, 3, 0, 11, 5, 0, 0, 5, 1, 6, 5, 11, 6, 6 },
      { 3, 11, 6, 3, 0, 3, 6, 1, 0, 6, 5, 2, 0, 5, 9, 6 },
      { 6, 5, 9, 3, 6, 9, 11, 4, 11, 9, 8, 6 }, { 5, 10, 6, 7, 4, 7, 8, 7 },
      { 4, 3, 0, 6, 4, 7, 3, 3, 6, 5, 10, 7 },
      { 1, 9, 0, 7, 5, 10, 6, 7, 8, 4, 7, 7 },
      { 10, 6, 5, 7, 1, 9, 7, 1, 1, 7, 3, 6, 7, 9, 4, 6 },
      { 6, 1, 2, 6, 6, 5, 1, 3, 4, 7, 8, 7 },
      { 1, 2, 5, 5, 5, 2, 6, 6, 3, 0, 4, 3, 3, 4, 7, 6 },
      { 8, 4, 7, 7, 9, 0, 5, 5, 0, 6, 5, 2, 0, 2, 6, 3 },
      { 7, 3, 9, 1, 7, 9, 4, 6, 3, 2, 9, 1, 5, 9, 6, 5, 2, 6, 9, 1 },
      { 3, 11, 2, 7, 7, 8, 4, 7, 10, 6, 5, 7 },
      { 5, 10, 6, 7, 4, 7, 2, 1, 4, 2, 0, 6, 2, 7, 11, 6 },
      { 0, 1, 9, 7, 4, 7, 8, 7, 2, 3, 11, 7, 5, 10, 6, 7 },
      { 9, 2, 1, 6, 9, 11, 2, 2, 9, 4, 11, 1, 7, 11, 4, 5, 5, 10, 6, 7 },
      { 8, 4, 7, 7, 3, 11, 5, 1, 3, 5, 1, 6, 5, 11, 6, 6 },
      { 5, 1, 11, 1, 5, 11, 6, 6, 1, 0, 11, 1, 7, 11, 4, 5, 0, 4, 11, 1 },
      { 0, 5, 9, 6, 0, 6, 5, 2, 0, 3, 6, 1, 11, 6, 3, 5, 8, 4, 7, 7 },
      { 6, 5, 9, 3, 6, 9, 11, 4, 4, 7, 9, 5, 7, 11, 9, 1 },
      { 10, 4, 9, 6, 6, 4, 10, 5 }, { 4, 10, 6, 6, 4, 9, 10, 3, 0, 8, 3, 7 },
      { 10, 0, 1, 6, 10, 6, 0, 1, 6, 4, 0, 3 },
      { 8, 3, 1, 3, 8, 1, 6, 0, 8, 6, 4, 6, 6, 1, 10, 6 },
      { 1, 4, 9, 6, 1, 2, 4, 1, 2, 6, 4, 3 },
      { 3, 0, 8, 7, 1, 2, 9, 5, 2, 4, 9, 2, 2, 6, 4, 3 },
      { 0, 2, 4, 5, 4, 2, 6, 6 }, { 8, 3, 2, 3, 8, 2, 4, 4, 4, 2, 6, 6 },
      { 10, 4, 9, 6, 10, 6, 4, 3, 11, 2, 3, 7 },
      { 0, 8, 2, 5, 2, 8, 11, 6, 4, 9, 10, 3, 4, 10, 6, 6 },
      { 3, 11, 2, 7, 0, 1, 6, 1, 0, 6, 4, 6, 6, 1, 10, 6 },
      { 6, 4, 1, 1, 6, 1, 10, 6, 4, 8, 1, 1, 2, 1, 11, 5, 8, 11, 1, 1 },
      { 9, 6, 4, 6, 9, 3, 6, 0, 9, 1, 3, 3, 11, 6, 3, 5 },
      { 8, 11, 1, 1, 8, 1, 0, 6, 11, 6, 1, 1, 9, 1, 4, 5, 6, 4, 1, 1 },
      { 3, 11, 6, 3, 3, 6, 0, 4, 0, 6, 4, 6 }, { 6, 4, 8, 3, 11, 6, 8, 5 },
      { 7, 10, 6, 6, 7, 8, 10, 1, 8, 9, 10, 3 },
      { 0, 7, 3, 6, 0, 10, 7, 0, 0, 9, 10, 3, 6, 7, 10, 5 },
      { 10, 6, 7, 3, 1, 10, 7, 1, 1, 7, 8, 2, 1, 8, 0, 6 },
      { 10, 6, 7, 3, 10, 7, 1, 4, 1, 7, 3, 6 },
      { 1, 2, 6, 3, 1, 6, 8, 0, 1, 8, 9, 6, 8, 6, 7, 6 },
      { 2, 6, 9, 1, 2, 9, 1, 6, 6, 7, 9, 1, 0, 9, 3, 5, 7, 3, 9, 1 },
      { 7, 8, 0, 3, 7, 0, 6, 4, 6, 0, 2, 6 }, { 7, 3, 2, 3, 6, 7, 2, 5 },
      { 2, 3, 11, 7, 10, 6, 8, 1, 10, 8, 9, 6, 8, 6, 7, 6 },
      { 2, 0, 7, 1, 2, 7, 11, 6, 0, 9, 7, 1, 6, 7, 10, 5, 9, 10, 7, 1 },
      { 1, 8, 0, 6, 1, 7, 8, 2, 1, 10, 7, 1, 6, 7, 10, 5, 2, 3, 11, 7 },
      { 11, 2, 1, 3, 11, 1, 7, 4, 10, 6, 1, 5, 6, 7, 1, 1 },
      { 8, 9, 6, 1, 8, 6, 7, 6, 9, 1, 6, 1, 11, 6, 3, 5, 1, 3, 6, 1 },
      { 0, 9, 1, 7, 11, 6, 7, 7 },
      { 7, 8, 0, 3, 7, 0, 6, 4, 3, 11, 0, 5, 11, 6, 0, 1 }, { 7, 11, 6, 7 },
      { 7, 6, 11, 7 }, { 3, 0, 8, 7, 11, 7, 6, 7 },
      { 0, 1, 9, 7, 11, 7, 6, 7 }, { 8, 1, 9, 6, 8, 3, 1, 3, 11, 7, 6, 7 },
      { 10, 1, 2, 7, 6, 11, 7, 7 }, { 1, 2, 10, 7, 3, 0, 8, 7, 6, 11, 7, 7 },
      { 2, 9, 0, 6, 2, 10, 9, 3, 6, 11, 7, 7 },
      { 6, 11, 7, 7, 2, 10, 3, 5, 10, 8, 3, 2, 10, 9, 8, 3 },
      { 7, 2, 3, 6, 6, 2, 7, 5 }, { 7, 0, 8, 6, 7, 6, 0, 1, 6, 2, 0, 3 },
      { 2, 7, 6, 6, 2, 3, 7, 3, 0, 1, 9, 7 },
      { 1, 6, 2, 6, 1, 8, 6, 0, 1, 9, 8, 3, 8, 7, 6, 3 },
      { 10, 7, 6, 6, 10, 1, 7, 1, 1, 3, 7, 3 },
      { 10, 7, 6, 6, 1, 7, 10, 4, 1, 8, 7, 2, 1, 0, 8, 3 },
      { 0, 3, 7, 3, 0, 7, 10, 0, 0, 10, 9, 6, 6, 10, 7, 5 },
      { 7, 6, 10, 3, 7, 10, 8, 4, 8, 10, 9, 6 }, { 6, 8, 4, 6, 11, 8, 6, 5 },
      { 3, 6, 11, 6, 3, 0, 6, 1, 0, 4, 6, 3 },
      { 8, 6, 11, 6, 8, 4, 6, 3, 9, 0, 1, 7 },
      { 9, 4, 6, 3, 9, 6, 3, 0, 9, 3, 1, 6, 11, 3, 6, 5 },
      { 6, 8, 4, 6, 6, 11, 8, 3, 2, 10, 1, 7 },
      { 1, 2, 10, 7, 3, 0, 11, 5, 0, 6, 11, 2, 0, 4, 6, 3 },
      { 4, 11, 8, 6, 4, 6, 11, 3, 0, 2, 9, 5, 2, 10, 9, 3 },
      { 10, 9, 3, 1, 10, 3, 2, 6, 9, 4, 3, 1, 11, 3, 6, 5, 4, 6, 3, 1 },
      { 8, 2, 3, 6, 8, 4, 2, 1, 4, 6, 2, 3 }, { 0, 4, 2, 5, 4, 6, 2, 3 },
      { 1, 9, 0, 7, 2, 3, 4, 1, 2, 4, 6, 6, 4, 3, 8, 6 },
      { 1, 9, 4, 3, 1, 4, 2, 4, 2, 4, 6, 6 },
      { 8, 1, 3, 6, 8, 6, 1, 0, 8, 4, 6, 3, 6, 10, 1, 3 },
      { 10, 1, 0, 3, 10, 0, 6, 4, 6, 0, 4, 6 },
      { 4, 6, 3, 1, 4, 3, 8, 6, 6, 10, 3, 1, 0, 3, 9, 5, 10, 9, 3, 1 },
      { 10, 9, 4, 3, 6, 10, 4, 5 }, { 4, 9, 5, 7, 7, 6, 11, 7 },
      { 0, 8, 3, 7, 4, 9, 5, 7, 11, 7, 6, 7 },
      { 5, 0, 1, 6, 5, 4, 0, 3, 7, 6, 11, 7 },
      { 11, 7, 6, 7, 8, 3, 4, 5, 3, 5, 4, 2, 3, 1, 5, 3 },
      { 9, 5, 4, 7, 10, 1, 2, 7, 7, 6, 11, 7 },
      { 6, 11, 7, 7, 1, 2, 10, 7, 0, 8, 3, 7, 4, 9, 5, 7 },
      { 7, 6, 11, 7, 5, 4, 10, 5, 4, 2, 10, 2, 4, 0, 2, 3 },
      { 3, 4, 8, 6, 3, 5, 4, 2, 3, 2, 5, 1, 10, 5, 2, 5, 11, 7, 6, 7 },
      { 7, 2, 3, 6, 7, 6, 2, 3, 5, 4, 9, 7 },
      { 9, 5, 4, 7, 0, 8, 6, 1, 0, 6, 2, 6, 6, 8, 7, 6 },
      { 3, 6, 2, 6, 3, 7, 6, 3, 1, 5, 0, 5, 5, 4, 0, 3 },
      { 6, 2, 8, 1, 6, 8, 7, 6, 2, 1, 8, 1, 4, 8, 5, 5, 1, 5, 8, 1 },
      { 9, 5, 4, 7, 10, 1, 6, 5, 1, 7, 6, 2, 1, 3, 7, 3 },
      { 1, 6, 10, 6, 1, 7, 6, 2, 1, 0, 7, 1, 8, 7, 0, 5, 9, 5, 4, 7 },
      { 4, 0, 10, 1, 4, 10, 5, 6, 0, 3, 10, 1, 6, 10, 7, 5, 3, 7, 10, 1 },
      { 7, 6, 10, 3, 7, 10, 8, 4, 5, 4, 10, 5, 4, 8, 10, 1 },
      { 6, 9, 5, 6, 6, 11, 9, 1, 11, 8, 9, 3 },
      { 3, 6, 11, 6, 0, 6, 3, 4, 0, 5, 6, 2, 0, 9, 5, 3 },
      { 0, 11, 8, 6, 0, 5, 11, 0, 0, 1, 5, 3, 5, 6, 11, 3 },
      { 6, 11, 3, 3, 6, 3, 5, 4, 5, 3, 1, 6 },
      { 1, 2, 10, 7, 9, 5, 11, 1, 9, 11, 8, 6, 11, 5, 6, 6 },
      { 0, 11, 3, 6, 0, 6, 11, 2, 0, 9, 6, 1, 5, 6, 9, 5, 1, 2, 10, 7 },
      { 11, 8, 5, 1, 11, 5, 6, 6, 8, 0, 5, 1, 10, 5, 2, 5, 0, 2, 5, 1 },
      { 6, 11, 3, 3, 6, 3, 5, 4, 2, 10, 3, 5, 10, 5, 3, 1 },
      { 5, 8, 9, 6, 5, 2, 8, 0, 5, 6, 2, 3, 3, 8, 2, 5 },
      { 9, 5, 6, 3, 9, 6, 0, 4, 0, 6, 2, 6 },
      { 1, 5, 8, 1, 1, 8, 0, 6, 5, 6, 8, 1, 3, 8, 2, 5, 6, 2, 8, 1 },
      { 1, 5, 6, 3, 2, 1, 6, 5 },
      { 1, 3, 6, 1, 1, 6, 10, 6, 3, 8, 6, 1, 5, 6, 9, 5, 8, 9, 6, 1 },
      { 10, 1, 0, 3, 10, 0, 6, 4, 9, 5, 0, 5, 5, 6, 0, 1 },
      { 0, 3, 8, 7, 5, 6, 10, 7 }, { 10, 5, 6, 7 },
      { 11, 5, 10, 6, 7, 5, 11, 5 }, { 11, 5, 10, 6, 11, 7, 5, 3, 8, 3, 0, 7 },
      { 5, 11, 7, 6, 5, 10, 11, 3, 1, 9, 0, 7 },
      { 10, 7, 5, 6, 10, 11, 7, 3, 9, 8, 1, 5, 8, 3, 1, 3 },
      { 11, 1, 2, 6, 11, 7, 1, 1, 7, 5, 1, 3 },
      { 0, 8, 3, 7, 1, 2, 7, 1, 1, 7, 5, 6, 7, 2, 11, 6 },
      { 9, 7, 5, 6, 9, 2, 7, 0, 9, 0, 2, 3, 2, 11, 7, 3 },
      { 7, 5, 2, 1, 7, 2, 11, 6, 5, 9, 2, 1, 3, 2, 8, 5, 9, 8, 2, 1 },
      { 2, 5, 10, 6, 2, 3, 5, 1, 3, 7, 5, 3 },
      { 8, 2, 0, 6, 8, 5, 2, 0, 8, 7, 5, 3, 10, 2, 5, 5 },
      { 9, 0, 1, 7, 5, 10, 3, 1, 5, 3, 7, 6, 3, 10, 2, 6 },
      { 9, 8, 2, 1, 9, 2, 1, 6, 8, 7, 2, 1, 10, 2, 5, 5, 7, 5, 2, 1 },
      { 1, 3, 5, 5, 3, 7, 5, 3 }, { 0, 8, 7, 3, 0, 7, 1, 4, 1, 7, 5, 6 },
      { 9, 0, 3, 3, 9, 3, 5, 4, 5, 3, 7, 6 }, { 9, 8, 7, 3, 5, 9, 7, 5 },
      { 5, 8, 4, 6, 5, 10, 8, 1, 10, 11, 8, 3 },
      { 5, 0, 4, 6, 5, 11, 0, 0, 5, 10, 11, 3, 11, 3, 0, 3 },
      { 0, 1, 9, 7, 8, 4, 10, 1, 8, 10, 11, 6, 10, 4, 5, 6 },
      { 10, 11, 4, 1, 10, 4, 5, 6, 11, 3, 4, 1, 9, 4, 1, 5, 3, 1, 4, 1 },
      { 2, 5, 1, 6, 2, 8, 5, 0, 2, 11, 8, 3, 4, 5, 8, 5 },
      { 0, 4, 11, 1, 0, 11, 3, 6, 4, 5, 11, 1, 2, 11, 1, 5, 5, 1, 11, 1 },
      { 0, 2, 5, 1, 0, 5, 9, 6, 2, 11, 5, 1, 4, 5, 8, 5, 11, 8, 5, 1 },
      { 9, 4, 5, 7, 2, 11, 3, 7 },
      { 2, 5, 10, 6, 3, 5, 2, 4, 3, 4, 5, 2, 3, 8, 4, 3 },
      { 5, 10, 2, 3, 5, 2, 4, 4, 4, 2, 0, 6 },
      { 3, 10, 2, 6, 3, 5, 10, 2, 3, 8, 5, 1, 4, 5, 8, 5, 0, 1, 9, 7 },
      { 5, 10, 2, 3, 5, 2, 4, 4, 1, 9, 2, 5, 9, 4, 2, 1 },
      { 8, 4, 5, 3, 8, 5, 3, 4, 3, 5, 1, 6 }, { 0, 4, 5, 3, 1, 0, 5, 5 },
      { 8, 4, 5, 3, 8, 5, 3, 4, 9, 0, 5, 5, 0, 3, 5, 1 }, { 9, 4, 5, 7 },
      { 4, 11, 7, 6, 4, 9, 11, 1, 9, 10, 11, 3 },
      { 0, 8, 3, 7, 4, 9, 7, 5, 9, 11, 7, 2, 9, 10, 11, 3 },
      { 1, 10, 11, 3, 1, 11, 4, 0, 1, 4, 0, 6, 7, 4, 11, 5 },
      { 3, 1, 4, 1, 3, 4, 8, 6, 1, 10, 4, 1, 7, 4, 11, 5, 10, 11, 4, 1 },
      { 4, 11, 7, 6, 9, 11, 4, 4, 9, 2, 11, 2, 9, 1, 2, 3 },
      { 9, 7, 4, 6, 9, 11, 7, 2, 9, 1, 11, 1, 2, 11, 1, 5, 0, 8, 3, 7 },
      { 11, 7, 4, 3, 11, 4, 2, 4, 2, 4, 0, 6 },
      { 11, 7, 4, 3, 11, 4, 2, 4, 8, 3, 4, 5, 3, 2, 4, 1 },
      { 2, 9, 10, 6, 2, 7, 9, 0, 2, 3, 7, 3, 7, 4, 9, 3 },
      { 9, 10, 7, 1, 9, 7, 4, 6, 10, 2, 7, 1, 8, 7, 0, 5, 2, 0, 7, 1 },
      { 3, 7, 10, 1, 3, 10, 2, 6, 7, 4, 10, 1, 1, 10, 0, 5, 4, 0, 10, 1 },
      { 1, 10, 2, 7, 8, 7, 4, 7 }, { 4, 9, 1, 3, 4, 1, 7, 4, 7, 1, 3, 6 },
      { 4, 9, 1, 3, 4, 1, 7, 4, 0, 8, 1, 5, 8, 7, 1, 1 },
      { 4, 0, 3, 3, 7, 4, 3, 5 }, { 4, 8, 7, 7 },
      { 9, 10, 8, 5, 10, 11, 8, 3 }, { 3, 0, 9, 3, 3, 9, 11, 4, 11, 9, 10, 6 },
      { 0, 1, 10, 3, 0, 10, 8, 4, 8, 10, 11, 6 },
      { 3, 1, 10, 3, 11, 3, 10, 5 }, { 1, 2, 11, 3, 1, 11, 9, 4, 9, 11, 8, 6 },
      { 3, 0, 9, 3, 3, 9, 11, 4, 1, 2, 9, 5, 2, 11, 9, 1 },
      { 0, 2, 11, 3, 8, 0, 11, 5 }, { 3, 2, 11, 7 },
      { 2, 3, 8, 3, 2, 8, 10, 4, 10, 8, 9, 6 }, { 9, 10, 2, 3, 0, 9, 2, 5 },
      { 2, 3, 8, 3, 2, 8, 10, 4, 0, 1, 8, 5, 1, 10, 8, 1 }, { 1, 10, 2, 7 },
      { 1, 3, 8, 3, 9, 1, 8, 5 }, { 0, 9, 1, 7 }, { 0, 3, 8, 7 }, null };

  /**
   * a generic cell - plane intersector -- used for finding the plane through a
   * 
   * not static so as to allow JavaScript to not load it as core.
   * 
   * unit cell
   * 
   * @param plane
   * @param v 
   * @param flags
   *          0 -- polygon int[]  1 -- edges only 2 -- triangles only 3 -- both
   * @return Vector of Point3f[3] triangles and Point3f[2] edge lines
   */


  public List<Object> intersectPlane(P4 plane, List<Object> v, int flags) {
    if (plane == null) {
      v.addLast(fullCubePolygon);
      return v;
    }
    P3[] vertices = (P3[]) v.get(0);
    if (flags != 0)
      v.clear();
    float[] values = new float[8];
    P3[] edgePoints = new P3[12];
    int insideMask = 0;
    for (int i = 0; i < 8; i++) {
      values[i] = plane.x * vertices[i].x + plane.y * vertices[i].y + plane.z
          * vertices[i].z + plane.w;
      if (values[i] < 0)
        insideMask |= Pwr2[i];
    }
    byte[] triangles = triangleTable2[insideMask];
    if (triangles == null)
      return null;
    for (int i = 0; i < 24; i+=2) {
      int v1 = edgeVertexes[i];
      int v2 = edgeVertexes[i + 1];
      // (P - P1) / (P2 - P1) = (0 - v1) / (v2 - v1)
      // or
      // P = P1 + (P2 - P1) * (0 - v1) / (v2 - v1)
      P3 result = P3.newP(vertices[v2]);
      result.sub(vertices[v1]);
      result.scale(values[v1] / (values[v1] - values[v2]));
      result.add(vertices[v1]);
      edgePoints[i >> 1] = result;
    }
    if (flags == 0) {
      BS bsPoints = new BS();
      v.clear();
      for (int i = 0; i < triangles.length; i++) {
        bsPoints.set(triangles[i]);
        //System.out.print(triangles[i]+" " );
        if (i % 4 == 2)
          i++;
      }
      //System.out.println();
      int nPoints = BSUtil.cardinalityOf(bsPoints);
      P3[] pts = new P3[nPoints];
      v.addLast(pts);
      int[]list = new int[12];
      int ptList = 0;
      for (int i = 0; i < triangles.length; i++) {
        int pt = triangles[i];
        if (bsPoints.get(pt)) {
          bsPoints.clear(pt);
          pts[ptList] = edgePoints[pt];
          list[pt] = (byte) ptList++;
        }          
        if (i % 4 == 2)
          i++;
      }
      
      int[][]polygons = AU.newInt2(triangles.length >> 2);
      v.addLast(polygons);
      for (int i = 0; i < triangles.length; i++)
          polygons[i >> 2] = new int[] { list[triangles[i++]], 
              list[triangles[i++]], list[triangles[i++]], triangles[i] };
      return v;
    }
    for (int i = 0; i < triangles.length; i++) {
      P3 pt1 = edgePoints[triangles[i++]];
      P3 pt2 = edgePoints[triangles[i++]];
      P3 pt3 = edgePoints[triangles[i++]];
      if ((flags & 1) == 1)
        v.addLast(new P3[] { pt1, pt2, pt3 });
      if ((flags & 2) == 2) {
        byte b = triangles[i];
        if ((b & 1) == 1)
          v.addLast(new P3[] { pt1, pt2 });
        if ((b & 2) == 2)
          v.addLast(new P3[] { pt2, pt3 });
        if ((b & 4) == 4)
          v.addLast(new P3[] { pt1, pt3 });
      }
    }
    return v;
  }
}
