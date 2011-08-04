/* $RCSfile$
 * $J. Gutow$
 * $July 22, 2011$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.swing.JOptionPane;

import org.jmol.api.JmolViewer;
import org.jmol.export.history.HistoryFile;
import org.jmol.i18n.GT;
import org.jmol.script.Token;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import org.jmol.viewer.JmolConstants;

/**
 * 
 */
public class SurfaceTool {

  private SurfaceToolGUI gui;
  boolean useGUI;
  protected JmolViewer viewer;
  private final Point3f negCorner = new Point3f();
  private final Point3f posCorner = new Point3f();
  private final Point3f center = new Point3f();
  private final Vector3f boxVec = new Vector3f();
  //surface specific parameters
  //TODO may want to combine the following into a single object
  private final List<String> surfaceIDs = new ArrayList<String>();
  private final List<Integer> surfaceKind = new ArrayList<Integer>();
  //private String[] surfaceCmds = new String[0];//initially set to basic creation.
  //private boolean[] surfaceVisible = new boolean[0];
  //private boolean[] filled = new boolean[0];
  //private boolean[] meshon = new boolean[0];
  //TODO fill and mesh color, translucency, frontonly, lighting

  final static int DEGREES = 0;
  final static int RADIANS = 1;
  final static int GRADIANS = 2;
  final static int CIRCLE_FRACTION = 3;
  final static int UNITS_PI = 4;
  private int angleUnits = DEGREES;
  //Note order of following list must match above.
  private String [] angleUnitsList = {GT._("Degrees"),GT._("Radians"),GT._("Gradians"),GT._("Circle Fraction"),GT._("Units of Pi")};
                  
  public SurfaceTool(JmolViewer viewer, HistoryFile hfile, String winName,
      boolean useGUI) {
    this.viewer = viewer;
    this.useGUI = useGUI;
    //initialization must occur before a new gui
    //initialize to match the boundbox
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
    initSlice();
    gui = (useGUI ? new SurfaceToolGUI(viewer, hfile, winName, this) : null);
  }

  public void toFront() {
    gui.toFront();
  }

  void toFrontOrGotFocus() {
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
  }

  private void chooseBestBoundBox() {
    //need to set the boundbox to the smallest one that surrounds all the
    //objects that could be sliced.
    //select all atoms and molecules to start as first guess.  Want initialization
    //added to the script so do with call to script
    
    BoxInfo box = new BoxInfo();
    viewer.calcAtomsMinMax(null, box);
    center.set(box.getBoundBoxCenter());
    boxVec.set(box.getBoundBoxCornerVector());
    posCorner.add(center, boxVec);
    negCorner.sub(center, boxVec);
    Shape[] shapes = (Shape[]) viewer.getProperty("DATA_API", "shapeManager",
    "getShapes");
    //now iterate through all the shapes and get their XYZmin and XYZmax.  Expand
    //Boundbox used by SurfaceTool to encompass these.
    box = checkMeshBB(shapes,JmolConstants.SHAPE_ISOSURFACE, box);
    box = checkMeshBB(shapes,JmolConstants.SHAPE_PMESH, box);
    //TODO
//    box = checkMeshBB(shapes,JmolConstants.SHAPE_MO, box);
    if (box!=null){
      center.set(box.getBoundBoxCenter());
      negCorner.sub(center, box.getBoundBoxCornerVector());
      posCorner.add(center, box.getBoundBoxCornerVector());
      boxVec.set(box.getBoundBoxCornerVector());
    }
  }
  
  BoxInfo checkMeshBB(Shape [] shapes, int kind, BoxInfo box){
    MeshCollection mc = (MeshCollection) shapes[kind];
    if (mc == null)
      return box;
    for (int i = 0; i < mc.meshCount; i++) {
      Mesh m = mc.meshes[i];
      if (!m.isValid || m.vertexCount == 0 && m.polygonCount == 0)
        continue;
      if (m.thisID.equalsIgnoreCase("_slicerleft") || m.thisID.equalsIgnoreCase("_slicerright"))
        continue;
      Point3f[] bb = m.getBoundingBox();
      if (bb == null)
        continue;
      box.addBoundBoxPoint(bb[0]);
      box.addBoundBoxPoint(bb[1]);    
    }   
    return box;
  }

  void setSurfaceToolParam() {
    //TODO should get stored parameters from History file upon initialization
    // probably belongs in another routine called only on start up.
    thicknessMax = 2 * boxVec.length();
    float delta = position - positionMin;
    if (useMolecular) {
      //set positionMin to minimum of BBoxCornerMin.x .y or .z or if all are 
      //negative -1* distance from origin. PositionMax similarly.
      if (negCorner.x < 0 && negCorner.y < 0 && negCorner.z < 0) {
        positionMin = -1 * negCorner.distance(new Point3f(0, 0, 0));
      } else {
        positionMin = Math.min(negCorner.x, negCorner.y);
        positionMin = Math.min(negCorner.z, positionMin);
      }
    } else {
      positionMin = -1 * (boxVec.length());
    }
    //positionMax = positionMin + thicknessMax;
    position = positionMin + delta;
  }

  private void updateSurfaceInfo() {
    Shape[] shapes = (Shape[]) viewer.getProperty("DATA_API", "shapeManager",
        "getShapes");
    surfaceIDs.clear();
    surfaceKind.clear();
    updateMeshInfo(shapes,JmolConstants.SHAPE_ISOSURFACE);
    updateMeshInfo(shapes,JmolConstants.SHAPE_PMESH);
    //TODO
//    updateMeshInfo(shapes,JmolConstants.SHAPE_MO);
  }

private void updateMeshInfo(Shape[] shapes, int kind){
  if (shapes != null) {
    MeshCollection mc = (MeshCollection) shapes[kind];
    if (mc != null) {
      //check all the meshes
      for (int i = 0; i < mc.meshCount; i++) {
        Mesh m = mc.meshes[i];
          if (!m.isValid || m.vertexCount == 0 && m.polygonCount == 0)
            continue;
          if (m.thisID.equalsIgnoreCase("_slicerleft") || m.thisID.equalsIgnoreCase("_slicerright"))
            continue;
          surfaceIDs.add(m.thisID);
          surfaceKind.add(Integer.valueOf(kind));
      }
    }
  }  
}
//  private MeshCollection getIsosurfaces() {
//    Shape[] shapes = (Shape[]) viewer.getProperty("DATA_API", "shapeManager",
//        "getShapes");
//    return (shapes == null ? null
//        : (MeshCollection) shapes[JmolConstants.SHAPE_ISOSURFACE]);
//  }

  void setAngleUnits(int units) {
    angleUnits = units;
  }

  Point3f getNegCorner() {
    return negCorner;
  }

  Point3f getPosCorner() {
    return posCorner;
  }

  /* Slicer section Begins
   * 
   */
  private float angleXY;
  //private float angleXYMax;
  private float anglefromZ;
  //private float anglefromZMax;
  private float positionMin;
  private float position;
  //private float positionMax;
  private float thickness;
  private float thicknessMax;
  private Slice slice = new Slice();

  private boolean lefton = false;
  private boolean righton = false;
  private boolean ghoston = false;
  private boolean capon = false;
  private boolean useMolecular = false;
  private boolean usePercent = false;

  private void initSlice() {
    //set to middle and full width
    angleXY = 0;
    anglefromZ = (float) (Math.PI / 2);
    position = 0;
    thickness = negCorner.distance(posCorner)/5;
    slice.setSlice(angleXY, anglefromZ, position, thickness, center,
        boxVec, useMolecular);
  }

  void showSliceBoundaryPlanes(boolean onOrOff) {
    lefton = righton = onOrOff;
    StringBuffer cmd = new StringBuffer();
    drawSlicePlane(cmd, Token.left, onOrOff);
    drawSlicePlane(cmd, Token.right, onOrOff);
    viewer.evalStringQuiet(cmd.toString());
  }

  /**
   * Defines a slice within which isosurfaces (and in future? atoms) are
   * displayed.
   * 
   * @param angleXY
   *        (float)angle in radians from X-axis to projection in XY plane
   * @param anglefromZ
   *        (float)angle in radians from z-axis to vector
   * @param position
   *        (float) position along direction vector in absolute units
   * @param thickness
   *        (float) thickness of slice in absolute units
   */
  void setSlice(float angleXY, float anglefromZ, float position,
                       float thickness) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT._("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    this.angleXY = angleXY;
    this.anglefromZ = anglefromZ;
    this.position = position;
    this.thickness = thickness;
    slice.setSlice(angleXY, anglefromZ, position, thickness, center,
        boxVec, useMolecular);
  }

  /**
   * 
   * @param angle
   *        (float) angle from X-axis of projection on XY plane in radians.
   */
  void setSliceAngleXY(float angle) {
    if (angleXY != angle) {
      angleXY = angle;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  float getSliceAngleXY() {
    return (angleXY);
  }

  /**
   * 
   * @param angle
   *        (float) angle of vector from Z axis in radians.
   */
  void setSliceAnglefromZ(float angle) {
    if (anglefromZ != angle) {
      anglefromZ = angle;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  float getAnglefromZ() {
    return (anglefromZ);
  }

  /**
   * 
   * @param where
   *        (float) position of slice center along direction vector.
   */
  void setSlicePosition(float where) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT._("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (position != where) {
      position = where;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  float getSlicePosition() {
    return (position);
  }

  /**
   * 
   * @param width
   *        (float) thickness of slice.
   */
  void setSliceThickness(float width) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT._("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (thickness != width) {
      thickness = width;
      slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  float getSliceThickness() {
    return (thickness);
  }

  void updateSlices() {
    for (int i = 0; i < surfaceIDs.size(); i++)
        sliceObject(surfaceIDs.get(i), surfaceKind.get(i).intValue());
  }

  void sliceObject(String objectName, int kind) {
 //   unnecessary already in ID list, which we carefully update
//    MeshCollection mc = getIsosurfaces(); 
//    int iMesh = mc.getIndexFromName(objectName);
//    if (iMesh < 0 || !mc.meshes[iMesh].isValid) {
      // do something here
//    } else {
      //valid surface because it is in the updated list and we check
    //every time the window gains focus to catch changes.
    String cmdStart ="";
    switch (kind){
    case JmolConstants.SHAPE_ISOSURFACE:
      cmdStart = "isosurface";
      break;
    case JmolConstants.SHAPE_PMESH:
      cmdStart = "pmesh";
      break;
    case JmolConstants.SHAPE_MO:
      cmdStart = "mo";
      break;
    }
      String ghostStr = (ghoston ? "translucent 0.8 mesh " : "");
      //      String cmd = "isosurface " + objectName + " off;";
      StringBuffer cmd = new StringBuffer();
      //planes on or off as appropriate
      drawSlicePlane(cmd, Token.left, lefton);
      drawSlicePlane(cmd, Token.right, righton);
      cmd.append(cmdStart);
      cmd.append(" ID \"").append(objectName).append("\" slab none");
      getSlabOption(cmd, ghostStr + "-", slice.leftPlane);
      getSlabOption(cmd, ghostStr, slice.rightPlane);
      cmd.append(";");
      //      cmd += " isosurface ").append(objectName).append(" on;";

      viewer.evalStringQuiet(cmd.toString());
//    }//TODO shouldn't fail silently as it does now.
    return;
  }
  
  private void getSlabOption(StringBuffer cmd, String prefix, Point4f plane) {
    String slabCapStr = (capon ? " cap ":" slab ");
    cmd.append(slabCapStr).append(prefix).append(Escape.escape(plane));
  }

  private void drawSlicePlane(StringBuffer cmd, int side,
                                     boolean on) {
    String color;
    String name = Token.nameOf(side);
    Point4f plane;
    switch (side) {
    default:
    case Token.left:
      plane = slice.leftPlane;
      color = "magenta";
      break;
    case Token.right:
      plane = slice.rightPlane;
      color = "cyan";
      break;
    }
    cmd.append("isosurface _slicer").append(name);
    if (on) {
      cmd.append(" plane ").append(Escape.escape(plane))
          .append(" translucent 0.7 ").append(color).append(";");
    } else {
      cmd.append(" off;");
    }
  }

  /**
   * @return (int) possible values: SurfaceTool.RADIANS, SurfaceTool.DEGREES.
   */
  int getAngleUnits() {
    return angleUnits;
  }

  /**
   * @return (boolean) true = ghost showing; false = ghost hiding.
   */
  boolean getGhoston() {
    return ghoston;
  }

  /**
   * @param b
   *        (boolean) true for ghost on.
   */
  void setGhostOn(boolean b) {
    ghoston = b;
  }

  /**
   * @return (boolean) true = using molecular coordinates; false = using
   *         boundbox coordinates.
   */
  boolean getUseMolecular() {
    return useMolecular;
  }

  void setUseMolecular(boolean on) {
    useMolecular = on;
  }

  float getPositionMin() {
    return positionMin;
  }

  float getThicknessMax() {
    return thicknessMax;
  }

  Point3f getCenter() {
    return center;
  }

  Vector3f getBoxVec() {
    return boxVec;
  }

  Point4f getSliceMiddle() {
    return slice.getMiddle();
  }
  
  String[] getAngleUnitsList(){
    return angleUnitsList;
  }

  boolean getCapOn() {
    return capon;
  }
  
  void setCapOn(boolean b){
    capon = b;
  }
}
