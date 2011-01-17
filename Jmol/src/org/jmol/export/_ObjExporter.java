package org.jmol.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.export.MeshData.Data;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.util.Escape;
import org.jmol.util.Quaternion;
import org.jmol.viewer.Viewer;

/**
 * Class to export Wavefront OBJ files.  The format is described at<br><br>
 * <a href="http://en.wikipedia.org/wiki/Wavefront_.obj_file">
 * http://en.wikipedia.org/wiki/Wavefront_.obj_file</a><br>
 * and<br>
 * <a href="http://www.martinreddy.net/gfx/3d/OBJ.spec">
 * http://www.martinreddy.net/gfx/3d/OBJ.spec</a><br><br>
 * Two files are produced: the object in the .obj file and the materials in the
 * .mtl file.  Both should be kept in the same directory.<br><br>
 * The exporter has been tested for ball and stick models, but not for:
 * <ul>
 * <li>outputFace (not implemented)</li>
 * <li>outputCone</li>
 * <li>outputTextPixel</li>
 * <li>outputTriangle</li>
 * <li>outputSurface (not implemented)</li>
 * </ul>
 * 
 * @author ken@kenevans.net
 * 
 */
public class _ObjExporter extends __CartesianExporter {
  private static final boolean debug = false;
  /** BufferedWriter for the .mtl file. */
  private BufferedWriter mtlbw;
  /** FileOutputStream for the .mtl file. */
  private FileOutputStream mtlos;
  /** File for the .mtl file. */
  File mtlFile;
  /** Bytes written to the .mtl file. */
  private int nMtlBytes;
  /** HashSet for textures. */
  Set<Short> textures = new HashSet<Short>();

  /** Number for the next mesh of this type. */
  private int sphereNum = 1;
  /** Number for the next mesh of this type. */
  private int cylinderNum = 1;
  /** Number for the next mesh of this type. */
  private int ellipseNum = 1;
  /** Number for the next mesh of this type. */
  private int circleNum = 1;
  /** Number for the next mesh of this type. */
  private int ellipsoidNum = 1;
  /** Number for the next mesh of this type. */
  private int coneNum = 1;
  /** Number for the next mesh of this type. */
  private int triangleNum = 1;

  /** Wavefront OBJ refers to vertices and normals by their location in the
   *  file.  This keeps track of where the latest set starts. */
  private int currentVertexOrigin = 1;
  /** Wavefront OBJ refers to vertices and normals by their location in the
   *  file.  This keeps track of where the latest set starts.  */
  private int currentNormalOrigin = 1;

  public _ObjExporter() {
    debugPrint("_WavefrontObjExporter CTOR");
    commentChar = "# ";
  }

  /**
   * Debug print utility.  Only prints if debug is true.
   * @param string
   */
  protected void debugPrint(final String string) {
    if (debug) {
      System.out.println(string);
    }
  }

  // Abstract methods

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputFace(int[], int[], int)
   */
  @Override
  protected void outputFace(int[] is, int[] coordMap, int faceVertexMax) {
    debugPrint("outputFace");
    // TODO
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCircle(javax.vecmath.Point3f, javax.vecmath.Point3f, float, short, boolean)
   */
  @Override
  protected void outputCircle(Point3f pt1, Point3f pt2, float radius,
                              short colix, boolean doFill) {
    debugPrint("outputCircle");

    if (doFill) {
      outputCircle1(pt1, pt2, colix, radius);
      return;
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCone(javax.vecmath.Point3f, javax.vecmath.Point3f, float, short)
   */
  @Override
  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
                            short colix) {
    debugPrint("outputCone");

    outputCone1(ptBase, ptTip, radius, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCylinder(javax.vecmath.Point3f, javax.vecmath.Point3f, javax.vecmath.Point3f, short, byte, float, javax.vecmath.Point3f, javax.vecmath.Point3f)
   */
  @Override
  protected boolean outputCylinder(Point3f ptCenter, Point3f pt1, Point3f pt2,
                                   short colix, byte endcaps, float radius,
                                   Point3f ptX, Point3f ptY) {
    // Ignore ptX and pyY as they are passed null from __CartesianExporter.draw
    if (debug) {
      debugPrint("outputCylinder: colix="
          + String.format("%04x", new Short(colix)));
      debugPrint("  ptCenter=" + ptCenter);
      debugPrint("  pt1=" + pt1);
      debugPrint("  endcaps=" + endcaps + " NONE=" + Graphics3D.ENDCAPS_NONE
          + " FLAT=" + Graphics3D.ENDCAPS_FLAT + " SPHERICAL="
          + Graphics3D.ENDCAPS_SPHERICAL);
      debugPrint("  radius=" + radius);
      debugPrint("  pt2=" + pt2);
      debugPrint("  ptX=" + ptX);
      debugPrint("  ptY=" + ptY);
    }

    if (ptX != null) {
      if (endcaps == Graphics3D.ENDCAPS_FLAT) {
        outputEllipse1(ptCenter, pt1, ptX, ptY, colix);
        tempP3.set(ptCenter);
        tempP3.sub(ptX);
        tempP3.add(ptCenter);
        outputEllipse1(ptCenter, pt2, tempP3, ptY, colix);
      }

    } else if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, radius * 1.01f, colix);
      outputSphere(pt2, radius * 1.01f, colix);
    } else if (endcaps == Graphics3D.ENDCAPS_FLAT) {
      outputCircle1(pt1, pt2, colix, radius);
      outputCircle1(pt2, pt1, colix, radius);
    }
    outputCylinder1(ptCenter, pt1, pt2, colix, endcaps, radius, ptX, ptY);

    return true;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputEllipsoid(javax.vecmath.Point3f, javax.vecmath.Point3f[], short)
   */
  @Override
  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    if (debug) {
      debugPrint("outputEllipsoid: colix="
          + String.format("%04x", new Short(colix)));
      debugPrint("  center=" + center);
      debugPrint("  points[0]=" + points[0]);
      debugPrint("  points[1]=" + points[1]);
      debugPrint("  points[2]=" + points[2]);
    }
    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3])
        .toAxisAngle4f();
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    outputEllipsoid1(center, sx, sy, sz, a, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputSphere(javax.vecmath.Point3f, float, short)
   */
  @Override
  protected void outputSphere(Point3f center, float radius, short colix) {
    // Note center is called ptAtom2 in the _CartesianExporter superclass
    // Note radius is called f in the _CartesianExporter superclass
    // Atom extends Point3fi extends Point3f, so this may be passed an Atom
    if (debug) {
      debugPrint("outputSphere: colix="
          + String.format("%04x", new Short(colix)));
      debugPrint("  center.getClass().getName()=" + center.getClass().getName());
      debugPrint("  center=" + center);
      debugPrint("  center.x=" + center.x);
      debugPrint("  center.y=" + center.y);
      debugPrint("  center.z=" + center.z);
      debugPrint("  radius=" + radius);
    }
    // Treat as a special case of ellipsoid
    outputEllipsoid1(center, radius, radius, radius, null, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputTextPixel(javax.vecmath.Point3f, int)
   */
  @Override
  protected void outputTextPixel(Point3f pt, int argb) {
    debugPrint("outputTextPixel");

    short colix = Graphics3D.getColix(argb);
    outputSphere(pt, 0.02f, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputTriangle(javax.vecmath.Point3f, javax.vecmath.Point3f, javax.vecmath.Point3f, short)
   */
  @Override
  protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3,
                                short colix) {
    debugPrint("outputTriangle");

    outputTriangle1(pt1, pt2, pt3, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#outputHeader()
   */
  @Override
  protected void outputHeader() {
    debugPrint("outputHeader");
    output("# Created by Jmol " + Viewer.getJmolVersion() + "\n");
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#output(javax.vecmath.Tuple3f)
   */
  @Override
  protected void output(Tuple3f pt) {
    debugPrint("output");
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#outputSurface(javax.vecmath.Point3f[], javax.vecmath.Vector3f[], short[], int[][], short[], int, int, int, java.util.BitSet, int, short, java.util.List, java.util.Map, javax.vecmath.Point3f)
   */
  @Override
  protected void outputSurface(Point3f[] vertices, Vector3f[] normals,
                               short[] colixes, int[][] indices,
                               short[] polygonColixes, int nVertices,
                               int nPolygons, int nFaces, BitSet bsFaces,
                               int faceVertexMax, short colix,
                               List<Short> colorList,
                               Map<String, String> htColixes, Point3f offset) {
    debugPrint("outputSurface");
    // TODO
  }

  // Non-abstract overrides from _Exporter

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#initializeOutput(org.jmol.viewer.Viewer, org.jmol.g3d.Graphics3D, java.lang.Object)
   */
  @Override
  boolean initializeOutput(Viewer viewer, Graphics3D g3d, Object output) {
    debugPrint("initializeOutput: + output");
    // Call the super method
    boolean retVal = super.initializeOutput(viewer, g3d, output);
    if (!retVal) {
      debugPrint("End initializeOutput (error in super):");
      return false;
    }

    // Open stream and writer for the .mtl file
    try {
      int dot = fileName.lastIndexOf(".");
      if (dot < 0) {
        debugPrint("End initializeOutput (Error creating .mtl file):");
        return false;
      }
      String mtlFileName = fileName.substring(0, dot) + ".mtl";
      mtlFile = new File(mtlFileName);
      System.out.println("_WavefrontObjExporter writing to "
          + mtlFile.getAbsolutePath());
      mtlos = new FileOutputStream(mtlFile);
      mtlbw = new BufferedWriter(new OutputStreamWriter(mtlos));
    } catch (FileNotFoundException ex) {
      debugPrint("End initializeOutput (" + ex.getMessage() + "):");
      return false;
    }
    outputMtl("# Created by Jmol " + Viewer.getJmolVersion() + "\n");
    output("\nmtllib " + mtlFile.getName() + "\n");
    debugPrint("End initializeOutput:");
    return true;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#finalizeOutput()
   */
  @Override
  // TODO should be protected in __Exporter
  String finalizeOutput() {
    debugPrint("finalizeOutput");
    String retVal = super.finalizeOutput();

    // Close the writer and stream
    try {
      mtlbw.flush();
      mtlbw.close();
      mtlos = null;
    } catch (IOException ex) {
      ex.printStackTrace();
      if (retVal.startsWith("OK")) {
        return "ERROR EXPORTING MTL FILE";
      }
      return retVal + " and ERROR EXPORTING MTL FILE";
    }

    retVal += ", " + nMtlBytes + " " + mtlFile.getPath();
    debugPrint(retVal);
    debugPrint("End finalizeOutput:");
    return retVal;
  }

  // Added methods

  /**
   * Write to the .mtl file and keep track of the bytes written.
   * 
   * @param data
   */
  private void outputMtl(String data) {
    nMtlBytes += data.length();
    try {
      mtlbw.write(data);
    } catch (IOException ex) {
      // TODO Ignore for now
    }
  }

  /**
   * Returns the name to be used for the texture associated with the given
   * colix. Jmol reading of the file without additional resources requires a color name here
   * in the form:  kRRGGBB
   * 
   * @param colix The value of colix.
   * @return The name for the structure.
   */
  private String getTextureName(short colix) {
    return "k" + Escape.getHexColorFromRGB(g3d.getColorArgbOrGray(colix));
  }

  /**
   * Local implementation of outputCircle.
   * 
   * @param ptCenter
   * @param ptPerp
   * @param colix
   * @param radius
   */
  private void outputCircle1(Point3f ptCenter, Point3f ptPerp, short colix,
                             float radius) {
    Data data = MeshData.getCircleData();
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name = "Circle" + circleNum++;
    matrix.set(getRotationMatrix(ptCenter, ptPerp, radius));
    matrix.m03 = ptCenter.x;
    matrix.m13 = ptCenter.y;
    matrix.m23 = ptCenter.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, colix);
  }

  /**
   * Local implementation of outputCone.
   * 
   * @param ptBase
   * @param ptTip
   * @param radius
   * @param colix
   */
  private void outputCone1(Point3f ptBase, Point3f ptTip, float radius,
                           short colix) {
    Data data = MeshData.getConeData();
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name = "Cone" + coneNum++;
    matrix.set(getRotationMatrix(ptBase, ptTip, radius));
    matrix.m03 = ptBase.x;
    matrix.m13 = ptBase.y;
    matrix.m23 = ptBase.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, colix);
  }

  /**
   * Local implementation of outputEllipse.
   * 
   * @param ptCenter
   * @param ptZ
   * @param ptX
   * @param ptY
   * @param colix
   * @return Always returns true.
   */
  private boolean outputEllipse1(Point3f ptCenter, Point3f ptZ, Point3f ptX,
                                 Point3f ptY, short colix) {
    Data data = MeshData.getCircleData();
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name = "Ellipse" + ellipseNum++;
    matrix.set(getRotationMatrix(ptCenter, ptZ, 1, ptX, ptY));
    matrix.m03 = ptZ.x;
    matrix.m13 = ptZ.y;
    matrix.m23 = ptZ.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, colix);
    return true;
  }

  /**
   * Local implementation of outputEllipsoid.
   * 
   * @param center
   * @param rx
   * @param ry
   * @param rz
   * @param a
   * @param colix
   */
  private void outputEllipsoid1(Point3f center, float rx, float ry, float rz,
                                AxisAngle4f a, short colix) {
    Data data = MeshData.getSphereData();
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name;
    if (center instanceof Atom) {
      Atom atom = (Atom) center;
      name = atom.getAtomName().replaceAll("\\s", "") + "_Atom";
    } else if (rx == ry && rx == rz) {
      // Is a sphere
      name = "Sphere" + sphereNum++;
    } else {
      name = "Ellipsoid" + ellipsoidNum++;
    }
    if (a != null) {
      Matrix3f mq = new Matrix3f();
      Matrix3f m = new Matrix3f();
      m.m00 = rx;
      m.m11 = ry;
      m.m22 = rz;
      mq.set(a);
      mq.mul(m);
      matrix.set(mq);
    } else {
      matrix.setIdentity();
      matrix.m00 = rx;
      matrix.m11 = ry;
      matrix.m22 = rz;
    }
    matrix.m03 = center.x;
    matrix.m13 = center.y;
    matrix.m23 = center.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, colix);
  }

  /**
   * Local implementation of outputCylinder.
   * 
   * @param ptCenter
   * @param pt1
   * @param pt2
   * @param colix
   * @param endcaps
   * @param radius
   * @param ptX
   * @param ptY
   */
  private void outputCylinder1(Point3f ptCenter, Point3f pt1, Point3f pt2,
                               short colix, byte endcaps, float radius,
                               Point3f ptX, Point3f ptY) {
    Data data = MeshData.getCylinderData(false);
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name = "Cylinder" + cylinderNum++;
    int n = (ptX != null && endcaps == Graphics3D.ENDCAPS_NONE ? 2 : 1);
    for (int i = 0; i < n; i++) {
      if (ptX == null)
        matrix.set(getRotationMatrix(pt1, pt2, radius));
      else
        matrix.set(getRotationMatrix(ptCenter, pt2, radius, ptX, ptY));
      matrix.m03 = pt1.x;
      matrix.m13 = pt1.y;
      matrix.m23 = pt1.z;
      matrix.m33 = 1;
    }
    addMesh(name, data, matrix, colix);
  }

  /**
   * Local implementation of outputCylinder.
   * 
   * @param pt1 Vertex 1.
   * @param pt2 Vertex 2.
   * @param pt3 Vertex 3.
   * @param colix The colix.
   */
  private void outputTriangle1(Point3f pt1, Point3f pt2, Point3f pt3,
                               short colix) {
    Data data = MeshData.getTriangleData(pt1, pt2, pt3);
    Matrix4f matrix = new Matrix4f();
    addTexture(colix);
    String name = "Triangle" + triangleNum++;
    matrix.setIdentity();
    addMesh(name, data, matrix, colix);
  }

  /**
   * Adds a texture to the .mtl file if it is a new texture.  Some of the
   * parameter choices are arbitrarily chosen.  The .mtl file can be easily
   * edited if it is desired to change things.
   * 
   * @param colix
   */
  private void addTexture(short colix) {
    Short scolix = new Short(colix);
    if (textures.contains(scolix)) {
      return;
    }
    textures.add(scolix);
    StringBuffer sb = new StringBuffer();
    sb.append("\nnewmtl " + getTextureName(colix) + "\n");
    // Highlight exponent (0-1000) High is a tight, concentrated highlight
    sb.append(" Ns 163\n");
    // Opacity (Sometimes d is used, sometimes Tr)
    //    sb.append(" d " + opacityFractionalFromColix(colix) + "\n");
    sb.append(" Tr " + opacityFractionalFromColix(colix) + "\n");
    // Index of refraction (.0001-10) 1.0 passes through
    sb.append(" Ni 0.001\n");
    // Illumination model (2 = highlight on)
    sb.append(" illum 2\n");
    // Ambient
    //    sb.append(" Ka " + rgbFractionalFromColix(colix, ' ') + "\n");
    sb.append(" Ka 0.20 0.20 0.20\n");
    // Diffuse
    sb.append(" Kd " + rgbFractionalFromColix(colix, ' ') + "\n");
    // Specular
    sb.append(" Ks 0.25 0.25 0.25\n");

    outputMtl(sb.toString());
  }

  /**
   * Adds a new mesh using the given data (faces, vertices, and normals) and
   * colix after transforming it via the given affine transform matrix.
   * 
   * @param name
   * @param data
   * @param matrix
   * @param colix
   */
  private void addMesh(String name, Data data, Matrix4f matrix, short colix) {
    // Note: No texture coordinates (vt) are used
    // The group (g) is probably not needed, but makes the file easier to read
    // Vertices and normals are numbered sequentially throughout the OBJ file
    //   (Why the currentVertexOrigin, etc. are needed)
    // currentNormalOrigin is the same as currentVertexOrigin since the
    //   normals and vertices are in 1-1 correspondence for our meshes
    output("\n" + "g " + name + "\n");
    output("usemtl " + getTextureName(colix) + "\n");

    int nVertices = data.getVertexes().length;
    int nNormals = data.getNormals().length;
    int nFaces = data.getFaces().length;

    Point3f p0;
    Vector3f v0;
    output("# Number of vertices: " + nVertices + "\n");
    for (Tuple3f vertex : data.getVertexes()) {
      p0 = new Point3f(vertex);
      matrix.transform(p0);
      output("v " + p0.x + " " + p0.y + " " + p0.z + "\n");
    }
    output("# Number of normals: " + nNormals + "\n");
    for (Tuple3f normal : data.getNormals()) {
      v0 = new Vector3f(normal);
      matrix.transform(v0);
      output("vn " + v0.x + " " + v0.y + " " + v0.z + "\n");
    }
    output("# Number of faces: " + nFaces + "\n");
    for (int[] face : data.getFaces()) {
      output("f");
      for (int i = 0; i < face.length; i++) {
        output(" " + (face[i] + currentVertexOrigin) + "//"
            + (face[i] + currentNormalOrigin));
      }
      output("\n");
    }
    currentVertexOrigin += nVertices;
    currentNormalOrigin += nNormals;
  }

}
