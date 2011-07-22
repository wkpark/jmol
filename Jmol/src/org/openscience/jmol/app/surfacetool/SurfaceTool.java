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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import java.lang.reflect.Array;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.swing.JOptionPane;

import org.jmol.api.JmolViewer;
import org.jmol.export.history.HistoryFile;
import org.jmol.i18n.GT;
import org.jmol.script.Token;
import org.jmol.shape.Shape;
import org.jmol.shapesurface.Isosurface;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;
import org.jmol.util.*;

/**
 * 
 */
public class SurfaceTool {

  private SurfaceToolGUI GUI;
  boolean useGUI;
  protected JmolViewer viewer;
  private Point3f negCorner;
  private Point3f posCorner;
  private Point3f center;
  private Vector3f boxVec;
  //surface specific parameters
  //TODO may want to combine the following into a single object
  private String[] surfaceIDs = new String[0];
  private String[] surfaceCmds = new String[0];//initially set to basic creation.
  private boolean[] surfaceVisible = new boolean[0];
  private boolean[] filled = new boolean[0];
  private boolean[] meshon = new boolean[0];
  //TODO fill and mesh color, translucency, frontonly, lighting

  public final static int RADIANS = 1;
  public final static int DEGREES = 2;
  private int angleUnits = RADIANS;

  public SurfaceTool(JmolViewer viewer, HistoryFile hfile, String winName,
      boolean useGUI) {
    this.viewer = viewer;
    //initialize to match the boundbox
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
    initSlice();
    if (useGUI) {
      GUI = new SurfaceToolGUI(viewer, hfile, winName, this);
    } else {
      GUI = null;
      useGUI = false;
    }
    this.useGUI = useGUI;
  }

  public void toFrontOrGotFocus() {
    updateSurfaceInfo();
    chooseBestBoundBox();
    setSurfaceToolParam();
  }

  private void chooseBestBoundBox() {
    //need to set the boundbox to the smallest one that surrounds all the
    //objects that could be sliced.
    //select all atoms and molecules to start as first guess.  Want initialization
    //added to the script so do with call to script
    viewer.script("boundbox {*};");
    this.center = viewer.getBoundBoxCenter();
    this.boxVec = viewer.getBoundBoxCornerVector();
    this.negCorner = Slice.vectoPoint(Slice.vecAdd(Slice.pointtoVec(center),
        Slice.vecScale(-1, boxVec)));
    this.posCorner = Slice.vectoPoint(Slice.vecAdd(Slice.pointtoVec(center),
        boxVec));
    //now iterate through all the shapes and get their XYZmin and XYZmax.  Expand
    //Boundbox used by SurfaceTool to encompass these.
    Shape[] shapes = ((Viewer) viewer).getShapeManager().getShapes();
    if (shapes == null)
      return;
    for (int i = 0; i < surfaceIDs.length; ++i) {
      Object[] data = new Object[3];
      data[0] = surfaceIDs[i];
      boolean success = (shapes[JmolConstants.SHAPE_ISOSURFACE].getProperty(
          "getBoundingBox", data));
      Point3f[] minMax = (Point3f[]) data[2];
      if (minMax != null) {
        if (minMax[0].x < negCorner.x) {
          negCorner.x = minMax[0].x;
        }
        if (minMax[0].y < negCorner.y) {
          negCorner.y = minMax[0].y;
        }
        if (minMax[0].z < negCorner.z) {
          negCorner.z = minMax[0].z;
        }
        if (minMax[1].x > posCorner.x) {
          posCorner.x = minMax[1].x;
        }
        if (minMax[1].y > posCorner.y) {
          posCorner.y = minMax[1].y;
        }
        if (minMax[1].z > posCorner.z) {
          posCorner.z = minMax[1].z;
        }
      }
      //check all the draw objects
    }
    //update center and boxVec
    center.x = (posCorner.x + negCorner.x) / 2;
    center.y = (posCorner.y + negCorner.y) / 2;
    center.z = (posCorner.z + negCorner.z) / 2;
    boxVec = Slice.vecAdd(Slice.pointtoVec(posCorner),
        Slice.vecScale(-1, Slice.pointtoVec(center)));
  }

  public void setSurfaceToolParam() {
    //TODO should get stored parameters from History file upon initialization
    // probably belongs in another routine called only on start up.
    switch (angleUnits) {
    case RADIANS:
      angleXYMax = (float) Math.PI;
      anglefromZMax = (float) Math.PI;
      break;
    case DEGREES:
      angleXYMax = 180;
      anglefromZMax = 180;
      break;
    }
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
    positionMax = positionMin + thicknessMax;
    position = positionMin + delta;
  }

  private void updateSurfaceInfo() {
    Shape[] shapes = ((Viewer) viewer).getShapeManager().getShapes();
    if (shapes != null) {
      //check all the isosurfaces (may have to do pmesh separately)
      if (shapes[JmolConstants.SHAPE_ISOSURFACE] != null) {
        //get list of surfaces by name        
        String surfaceList = (String) ((Viewer) viewer).getShapeManager()
            .getShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "list",
                Integer.MIN_VALUE);
        String[] surfaces = surfaceList.split("\n");
        String[] tempIDs = new String[surfaces.length];
        String[] tempCmds = new String[surfaces.length];
        for (int i = 0; i < surfaces.length; ++i) {
          int start = surfaces[i].indexOf("id:") + 3;
          int end = surfaces[i].indexOf(";", start);
          tempIDs[i] = surfaces[i].substring(start, (end));
          start = surfaces[i].indexOf("title:") + 6;
          end = surfaces[i].indexOf(";", start);
          tempCmds[i] = surfaces[i].substring(start, (end));
        }
        surfaceIDs = tempIDs;
        surfaceCmds = tempCmds;
      }
    }
  }

  public void setAngleUnits(int units) {
    this.angleUnits = units;
  }

  public Point3f getNegCorner() {
    return negCorner;
  }

  public Point3f getPosCorner() {
    return posCorner;
  }

  /* Slicer section Begins
   * 
   */
  private float angleXY;
  private float angleXYMax;
  private float anglefromZ;
  private float anglefromZMax;
  private float positionMin;
  private float position;
  private float positionMax;
  private float thickness;
  private float thicknessMax;
  private Slice slice = new Slice();

  private boolean lefton = false;
  private boolean righton = false;
  private boolean ghoston = false;
  private boolean useMolecular = false;
  private boolean usePercent = false;

  private void initSlice() {
    //set to middle and full width
    this.angleXY = 0;
    this.anglefromZ = (float) (Math.PI / 2);
    this.position = 0;
    this.thickness = negCorner.distance(posCorner);
    this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
        boxVec, useMolecular);
  }

  public void showSliceBoundaryPlanes() {
    this.lefton = true;
    this.righton = true;
    drawSlicePlane(Token.left, true);
    drawSlicePlane(Token.right, true);
  }

  public void hideSliceBoundaryPlanes() {
    this.lefton = false;
    this.righton = false;
    drawSlicePlane(Token.left, false);
    drawSlicePlane(Token.right, false);
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
  public void setSlice(float angleXY, float anglefromZ, float position,
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
    this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
        boxVec, useMolecular);
  }

  /**
   * 
   * @param angle
   *        (float) angle from X-axis of projection on XY plane in radians.
   */
  public void setSliceAngleXY(float angle) {
    if (this.angleXY != angle) {
      this.angleXY = angle;
      this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  public float getSliceAngleXY() {
    return (this.angleXY);
  }

  /**
   * 
   * @param angle
   *        (float) angle of vector from Z axis in radians.
   */
  public void setSliceAnglefromZ(float angle) {
    if (this.anglefromZ != angle) {
      this.anglefromZ = angle;
      this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  public float getAnglefromZ() {
    return (this.anglefromZ);
  }

  /**
   * 
   * @param where
   *        (float) position of slice center along direction vector.
   */
  public void setSlicePosition(float where) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT._("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (this.position != where) {
      this.position = where;
      this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  public float getSlicePosition() {
    return (this.position);
  }

  /**
   * 
   * @param width
   *        (float) thickness of slice.
   */
  public void setSliceThickness(float width) {
    if (usePercent) {//convert to absolute units
      //TODO
      JOptionPane.showMessageDialog(null,
          GT._("Percentage scaling not implemented yet!"), "Warning",
          javax.swing.JOptionPane.WARNING_MESSAGE);
    }
    if (this.thickness != width) {
      this.thickness = width;
      this.slice.setSlice(angleXY, anglefromZ, position, thickness, center,
          boxVec, useMolecular);
    }
  }

  public float getSliceThickness() {
    return (this.thickness);
  }

  public void updateSlices() {
    for (int i = 0; i < surfaceIDs.length; i++) {
      //TODO only operate on selected surfaces.
      if (!surfaceIDs[i].equalsIgnoreCase("_slicerleft")
          && !surfaceIDs[i].equalsIgnoreCase("_slicerright"))
        sliceObject(surfaceIDs[i]);
    }
  }

  public void sliceObject(String objectName) {
    int objectID = ((Viewer) viewer).getShapeManager()
        .getShapeIdFromObjectName(objectName);
    if (objectID == JmolConstants.SHAPE_ISOSURFACE) {//valid shape
      String ghostStr = "";
      if (ghoston)
        ghostStr = "translucent 0.8 mesh ";
      //      String cmd = "isosurface " + objectName + " off;";
      String cmd = "isosurface " + objectName + " slab none";
      cmd += " slab " + ghostStr;
      cmd += "-{" + slice.leftPlane.x + " " + slice.leftPlane.y + " "
          + slice.leftPlane.z + " " + slice.leftPlane.w + "}";
      cmd += " slab " + ghostStr;
      cmd += "{" + slice.rightPlane.x + " " + slice.rightPlane.y + " "
          + slice.rightPlane.z + " " + slice.rightPlane.w + "};";
      //      cmd += " isosurface " + objectName + " on;";
      //planes on or off as appropriate
      drawSlicePlane(Token.left, lefton);
      drawSlicePlane(Token.right, righton);

      viewer.evalStringQuiet(cmd);
    }//TODO shouldn't fail silently as it does now.
    return;
  }

  public void drawSlicePlane(int side, boolean on) {
    switch (side) {
    case Token.left:
      if (on) {
        String planeStr = "{" + slice.leftPlane.x + " " + slice.leftPlane.y
            + " " + slice.leftPlane.z + " " + slice.leftPlane.w + "}";
        //        viewer.evalStringQuiet("draw _slicerLeft plane " + planeStr
        //            + " color translucent 200 magenta;");
        viewer.evalStringQuiet("isosurface _slicerLeft plane " + planeStr
            + "translucent 0.7 magenta;");
        lefton = true;
      } else {
        viewer.evalStringQuiet("isosurface _slicerLeft off");
        lefton = false;
      }
      break;
    case Token.right:
      if (on) {
        String planeStr = "{" + slice.rightPlane.x + " " + slice.rightPlane.y
            + " " + slice.rightPlane.z + " " + slice.rightPlane.w + "}";
        //      viewer.evalStringQuiet("draw _slicerLeft plane " + planeStr
        //          + " color translucent 200 magenta;");
        viewer.evalStringQuiet("isosurface _slicerRight plane " + planeStr
            + "translucent 0.7 cyan;");
        righton = true;
      } else {
        viewer.evalStringQuiet("isosurface _slicerRight off");
        righton = false;
      }
      break;
    }
  }

  /**
   * @return (int) possible values: SurfaceTool.RADIANS, SurfaceTool.DEGREES.
   */
  public int getAngleUnits() {
    return angleUnits;
  }

  /**
   * @return (boolean) true = ghost showing; false = ghost hiding.
   */
  public boolean getGhoston() {
    return ghoston;
  }

  /**
   * @param b
   *        (boolean) true for ghost on.
   */
  public void setGhostOn(boolean b) {
    this.ghoston = b;
  }

  /**
   * @return (boolean) true = using molecular coordinates; false = using
   *         boundbox coordinates.
   */
  public boolean getUseMolecular() {
    return useMolecular;
  }

  public void setUseMolecular(boolean on) {
    this.useMolecular = on;
  }

  public SurfaceToolGUI getGUI() {
    return GUI;
  }

  public float getPositionMin() {
    return positionMin;
  }

  public float getThicknessMax() {
    return thicknessMax;
  }

  public Point3f getCenter() {
    return center;
  }

  public Vector3f getBoxVec() {
    return boxVec;
  }

  public Point4f getSliceMiddle() {
    return slice.getMiddle();
  }
  /* Slicer section End
  * 
  */

}
