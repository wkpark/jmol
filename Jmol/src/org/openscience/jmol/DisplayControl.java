/*
 * Copyright 2002 The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.render.AtomShape;

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.util.Hashtable;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

final public class DisplayControl {

  public static final int NOLABELS = DisplaySettings.NOLABELS;
  public static final int SYMBOLS = DisplaySettings.SYMBOLS;
  public static final int TYPES = DisplaySettings.TYPES;
  public static final int NUMBERS = DisplaySettings.NUMBERS;
  public static final int QUICKDRAW = DisplaySettings.QUICKDRAW;
  public static final int SHADING = DisplaySettings.SHADING;
  public static final int WIREFRAME = DisplaySettings.WIREFRAME;
  public static final int LINE = DisplaySettings.LINE;
  public static final int ATOMTYPE = DisplaySettings.ATOMTYPE;
  public static final int ATOMCHARGE = DisplaySettings.ATOMCHARGE;

  
  // while these variables are public, they should be considered *read-only*
  // to write these variables you *must* use the appropriate set function
  // they are currently used by Atom and AtomShape for transforms & rendering
  public boolean mouseDragged = false;
  public int xTranslation;
  public int yTranslation;
  public int cameraZ = 750;
  public final Matrix4d matrixTransform = new Matrix4d();

  private DisplayPanel panel;
  private DisplaySettings settings;

  private int minScreenDimension;
  private Dimension dimCurrent;
  private double scalePixelsPerAngstrom;
  private double scaleDefaultPixelsPerAngstrom;
  private double zoomScale;
  private double cameraDepth = 3;
  private final Matrix4d matrixRotate = new Matrix4d();
  private final Matrix4d matrixTemp = new Matrix4d();
  private final Vector3d vectorTemp = new Vector3d();
  private boolean perspectiveDepth = true;
  private boolean structuralChange = false;

  DisplayControl() {
    this.settings = new DisplaySettings();
  }

  public void setDisplayPanel(DisplayPanel panel) {
    this.panel = panel;
  }

  public DisplayPanel getDisplayPanel() {
    return panel;
  }

  public DisplaySettings getSettings() {
    return settings;
  }
  
  public void setLabelMode(int mode) {
    settings.setLabelMode(mode);
    recalc();
  }

  public int getLabelMode() {
    return settings.getLabelMode();
  }

  public void setAtomDrawMode(int mode) {
    settings.setAtomDrawMode(mode);
    recalc();
  }

  public int getAtomDrawMode() {
    return settings.getAtomDrawMode();
  }

  public void setAtomColorProfile(int mode) {
    settings.setAtomColorProfile(mode);
    recalc();
  }

  public int getAtomColorProfile() {
    return settings.getAtomColorProfile();
  }

  public void setBondDrawMode(int mode) {
    settings.setBondDrawMode(mode);
    recalc();
  }

  public int getBondDrawMode() {
    return settings.getBondDrawMode();
  }

  public void setBondWidth(double width) {
    settings.setBondWidth(width);
    recalc();
  }

  public double getBondWidth() {
    return settings.getBondWidth();
  }

  public void setOutlineColor(Color c) {
    settings.setOutlineColor(c);
    recalc();
  }

  public Color getOutlineColor() {
    return settings.getOutlineColor();
  }

  public void setPickedColor(Color c) {
    settings.setPickedColor(c);
    recalc();
  }

  private Color colorOpaquePicked = null;
  private Color colorTransparentPicked = null;
  public Color getPickedColor() {
    Color pickedCurrent = settings.getPickedColor();
    if (colorTransparentPicked == null ||
        colorOpaquePicked != pickedCurrent) {
      colorTransparentPicked = colorOpaquePicked = pickedCurrent;
      if (useGraphics2D) {
        int rgba = (pickedCurrent.getRGB() & 0x00FFFFFF) | 0x80000000;
        colorTransparentPicked = new Color(rgba, true);
      }
    }
    return colorTransparentPicked;
  }

  public void setTextColor(Color c) {
    settings.setTextColor(c);
    recalc();
  }

  public Color getTextColor() {
    return settings.getTextColor();
  }

  public void setPropertyMode(String s) {
    settings.setPropertyMode(s);
    recalc();
  }

  public String getPropertyMode() {
    return settings.getPropertyMode();
  }

  public void setDistanceColor(Color c) {
    settings.setDistanceColor(c);
    recalc();
  }

  public Color getDistanceColor() {
    return settings.getDistanceColor();
  }

  public void setAngleColor(Color c) {
    settings.setAngleColor(c);
    recalc();
  }

  public Color getAngleColor() {
    return settings.getAngleColor();
  }

  public void setDihedralColor(Color c) {
    settings.setDihedralColor(c);
    recalc();
  }

  public Color getDihedralColor() {
    return settings.getDihedralColor();
  }

  public void setShowAtoms(boolean showAtoms) {
    settings.setShowAtoms(showAtoms);
    recalc();
  }

  public boolean getShowAtoms() {
    return settings.getShowAtoms();
  }

  public void setShowBonds(boolean showBonds) {
    settings.setShowBonds(showBonds);
    recalc();
  }

  public boolean getShowBonds() {
    return settings.getShowBonds();
  }

  public void setShowVectors(boolean showVectors) {
    settings.setShowVectors(showVectors);
    recalc();
  }

  public boolean getShowVectors() {
    return settings.getShowVectors();
  }

  public void setShowHydrogens(boolean showHydrogens) {
    settings.setShowHydrogens(showHydrogens);
    recalc();
  }

  public boolean getShowHydrogens() {
    return settings.getShowHydrogens();
  }

  public void setShowDarkerOutline(boolean showDarkerOutline) {
    settings.setShowDarkerOutline(showDarkerOutline);
    recalc();
  }

  public boolean getShowDarkerOutline() {
    return settings.getShowDarkerOutline();
  }

  public void setAtomSphereFactor(double f) {
    settings.setAtomSphereFactor(f);
    recalc();
  }

  public double getAtomSphereFactor() {
    return settings.getAtomSphereFactor();
  }

  public void setAntiAliased(boolean antiAlias) {
    settings.setAntiAliased(antiAlias);
    recalc();
  }

  public void setFastRendering(boolean b) {
    settings.setFastRendering(b);
    recalc();
  }

  public boolean getFastRendering() {
    return settings.getFastRendering();
  }

  private final SelectionSet setPicked = new SelectionSet();

  public void addSelection(Atom atom) {
    setPicked.addSelection(atom.getAtomNumber());
    recalc();
  }

  public void removeSelection(Atom atom) {
    setPicked.removeSelection(atom.getAtomNumber());
    recalc();
  }

  public void toggleSelection(Atom atom) {
    int atomNum = atom.getAtomNumber();
    if (setPicked.isSelected(atomNum))
      setPicked.removeSelection(atomNum);
    else
      setPicked.addSelection(atomNum);
    recalc();
  }

  public void addSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      setPicked.addSelection(atoms[i].getAtomNumber());
    recalc();
  }

  public void removeSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      setPicked.removeSelection(atoms[i].getAtomNumber());
    recalc();
  }

  public int[] getSelection() {
    return setPicked.getSelection();
  }

  public void clearSelection() {
    setPicked.clearSelection();
    recalc();
  }

  public int countSelection() {
    return setPicked.countSelection();
  }

  public boolean isSelected(Atom atom) {
    return setPicked.isSelected(atom.getAtomNumber());
  }

  public double getScalePixelsPerAngstrom() {
    return scalePixelsPerAngstrom;
  }

  public void translateBy(int xDelta, int yDelta) {
    xTranslation += xDelta;
    yTranslation += yDelta;
  }

  public void rotateBy(int xDelta, int yDelta) {
    // what fraction of PI radians do you want to rotate?
    // the full screen width corresponds to a PI (180 degree) rotation
    // if you grab an atom near the outside edge of the molecule,
    // you can essentially "pull it" across the screen and it will
    // track with the mouse cursor

    // the accelerator is just a slop factor ... it felt a litte slow to me
    double rotateAccelerator = 1.1f;

    // a change in the x coordinate generates a rotation about the y axis
    double ytheta = Math.PI * xDelta / minScreenDimension / zoomScale;
    rotateByY(ytheta * rotateAccelerator);
    double xtheta = Math.PI * yDelta / minScreenDimension / zoomScale;
    rotateByX(xtheta * rotateAccelerator);
    recalc();
  }

  public void multiplyZoomScale(double scale) {
    zoomScale *= scale;
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom * zoomScale;
    recalc();
  }

  public double getZoomScale() {
    return zoomScale;
  }

  public Matrix4d getPovRotateMatrix() {
    return new Matrix4d(matrixRotate);
  }

  public Matrix4d getPovTranslateMatrix() {
    Matrix4d matrixPovTranslate = new Matrix4d();
    matrixPovTranslate.setIdentity();
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x = (xTranslation - dimCurrent.width/2) / scalePixelsPerAngstrom;
    vectorTemp.y = -(yTranslation - dimCurrent.height/2)
      / scalePixelsPerAngstrom; // invert y axis
    vectorTemp.z = 0;
    matrixPovTranslate.set(vectorTemp);
    return matrixPovTranslate;
  }

  public void calcViewTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixTransform.setIdentity();
    // first, translate the coordinates back to the center
    vectorTemp.set(getFrame().getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixTransform.mul(matrixRotate);
    matrixTransform.mul(matrixRotate, matrixTransform);
    // now shift so that all z coordinates are <= 0
    // this is important for scaling
    vectorTemp.x = 0;
    vectorTemp.y = 0;
    vectorTemp.z = getFrame().getRotationRadius();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);
    // now scale to screen coordinates
    matrixTemp.set(scalePixelsPerAngstrom);
    matrixTemp.m11=-scalePixelsPerAngstrom; // invert y dimension
    matrixTransform.mul(matrixTemp, matrixTransform);
    // note that the image is still centered at 0, 0
    // translations come later (to deal with perspective)
    // and all z coordinates are <= 0
  }

  public void transformPoint(Point3d pointAngstroms, Point3d pointScreen) {
    matrixTransform.transform(pointAngstroms, pointScreen);
    if (perspectiveDepth) {
      int depth = cameraZ - (int)pointScreen.z;
      pointScreen.x = (((int)pointScreen.x * cameraZ) / depth) + xTranslation;
      pointScreen.y = (((int)pointScreen.y * cameraZ) / depth) + yTranslation;
    } else {
      pointScreen.x += xTranslation;
      pointScreen.y += yTranslation;
    }
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    this.perspectiveDepth = perspectiveDepth;
    scaleFitToScreen();
    recalc();
  }

  public boolean isPerspectiveDepth() {
    return perspectiveDepth;
  }

  public void setCameraDepth(double depth) {
    cameraDepth = depth;
  }

  public double getCameraDepth() {
    return cameraDepth;
  }

  public int getCameraZ() {
    return cameraZ;
  }

  public void homePosition() {
    matrixRotate.setIdentity();         // no rotations
    scaleFitToScreen();
    recalc();
  }

  public void setScreenDimension(Dimension dimCurrent) {
    this.dimCurrent = dimCurrent;
  }

  public Dimension getScreenDimension() {
    return dimCurrent;
  }

  // don't do recalc here
  public void scaleFitToScreen(Dimension dimCurrent) {
    setScreenDimension(dimCurrent);
    scaleFitToScreen();
  }
  public void scaleFitToScreen() {
    // translate to the middle of the screen
    xTranslation = dimCurrent.width / 2;
    yTranslation = dimCurrent.height / 2;
    // find smaller screen dimension
    minScreenDimension = dimCurrent.width;
    if (dimCurrent.height < minScreenDimension)
      minScreenDimension = dimCurrent.height;
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (minScreenDimension > 2)
      minScreenDimension -= 2;
    scalePixelsPerAngstrom =
      minScreenDimension / 2 / getFrame().getRotationRadius();
    if (perspectiveDepth) {
      double scaleFactor = (cameraZ + minScreenDimension / 2) / (double)cameraZ;
      scaleFactor += .02f; // don't know why I need this, but seems I do -- mth
      scalePixelsPerAngstrom *= scaleFactor;
    }
    // these are important!
    scaleDefaultPixelsPerAngstrom = scalePixelsPerAngstrom;
    zoomScale = 1;
    cameraZ = (int)cameraDepth * minScreenDimension;
  }

  public void maybeEnableAntialiasing(Graphics g) {
    if (useGraphics2D && wantsAntialias &&
        (wantsAntialiasAlways || !mouseDragged)) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
    }
  }

  private BasicStroke dottedStroke = null;
  public void maybeDottedStroke(Graphics g) {
    if (useGraphics2D) {
      if (dottedStroke == null) {
        dottedStroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND, 0,
                                       new float[] {3, 3}, 0);
      }
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(dottedStroke);
    }
  }

  private boolean haveFile = false;
  private ChemFile chemfile;
  private ChemFrame chemframe;
  private int nframes = 0;
  private MeasurementList mlist = null;

  public void setChemFile(ChemFile chemfile) {
    this.chemfile = chemfile;
    haveFile = true;
    nframes = chemfile.getNumberOfFrames();
    this.chemframe = chemfile.getFrame(0);
    Measurement.setChemFrame(chemframe);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    homePosition();
  }

  public boolean haveFile() {
    return haveFile;
  }

  public ChemFrame getFrame() {
    return chemframe;
  }

  public void setFrame(int fr) {
    if (haveFile && fr >= 0 && fr < nframes) {
        setFrame(chemfile.getFrame(fr));
        recalc();
    }
  }

  public void setFrame(ChemFrame frame) {
    chemframe = frame;
    Measurement.setChemFrame(frame);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    recalc();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    MeasurementList source = (MeasurementList) mle.getSource();
    mlist = source;
    chemframe.updateMlists(mlist.getDistanceList(),
                           mlist.getAngleList(),
                           mlist.getDihedralList());
  }

  private Color backgroundColor = null;
  public void setBackgroundColor(Color bg) {
    if (bg == null)
      backgroundColor = Color.getColor("backgroundColor");
    else
      backgroundColor = bg;
    recalc();
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  private boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
    recalc();
  }

  public void setMouseDragged(boolean mouseDragged) {
    if (wireframeRotation && this.mouseDragged != mouseDragged)
      settings.setFastRendering(mouseDragged);
    if (this.mouseDragged && !mouseDragged)
      recalc();
    this.mouseDragged = mouseDragged;
  }

  public boolean jvm12orGreater = false;
  public boolean useGraphics2D = false;
  public boolean wantsGraphics2D = true;
  public boolean wantsAntialias = true;
  public boolean wantsAntialiasAlways = false;
  public void setJvm12orGreater(boolean jvm12orGreater) {
    this.jvm12orGreater = jvm12orGreater;
    useGraphics2D = jvm12orGreater && wantsGraphics2D;
  }

  public void setWantsGraphics2D(boolean wantsGraphics2D) {
    if (this.wantsGraphics2D != wantsGraphics2D) {
      this.wantsGraphics2D = wantsGraphics2D;
      useGraphics2D = jvm12orGreater && wantsGraphics2D;
      flushCachedImages();
      recalc();
    }
  }

  public void setWantsAntialias(boolean wantsAntialias) {
    if (this.wantsAntialias != wantsAntialias) {
      this.wantsAntialias = wantsAntialias;
      recalc();
    }
  }

  public void setWantsAntialiasAlways(boolean wantsAntialiasAlways) {
    this.wantsAntialiasAlways = wantsAntialiasAlways;
    // no need to recalc in this state since we aren't doing anything
  }

  public Image takeSnapshot() {
    return panel.takeSnapshot();
  }

  private void recalc() {
    panel.repaint();
  }

  public void rotateFront() {
    matrixRotate.setIdentity();
    recalc();
  }

  public void rotateToX(double angleRadians) {
    matrixRotate.rotX(angleRadians);
    recalc();
  }
  public void rotateToY(double angleRadians) {
    matrixRotate.rotY(angleRadians);
    recalc();
  }
  public void rotateToZ(double angleRadians) {
    matrixRotate.rotZ(angleRadians);
    recalc();
  }

  public void rotateToX(int angleDegrees) {
    rotateToX(Math.toRadians(angleDegrees));
  }
  public void rotateToY(int angleDegrees) {
    rotateToY(Math.toRadians(angleDegrees));
  }
  public void rotateToZ(int angleDegrees) {
    rotateToZ(Math.toRadians(angleDegrees));
  }

  public void rotateByX(double angleRadians) {
    matrixTemp.rotX(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByY(double angleRadians) {
    matrixTemp.rotY(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByZ(double angleRadians) {
    matrixTemp.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByX(int angleDegrees) {
    rotateByX(Math.toRadians(angleDegrees));
  }
  public void rotateByY(int angleDegrees) {
    rotateByY(Math.toRadians(angleDegrees));
  }
  public void rotateByZ(int angleDegrees) {
    rotateByZ(Math.toRadians(angleDegrees));
  }
  public void rotate(AxisAngle4d axisAngle) {
    matrixTemp.setIdentity();
    matrixTemp.setRotation(axisAngle);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void setCenter(Point3d center) {
    getFrame().setRotationCenter(center);
  }

  public void refresh() {
  // this is here temporarily while I figure out what to do in scripting
    recalc();
  }
  

  public void setCenterAsSelected() {
    int[] picked = setPicked.getSelection();
    Point3d center = null;
    if (picked.length > 0) {
      // just take the average of all the points
      center = new Point3d(); // defaults to 0,0,0
      for (int i = 0; i < picked.length; ++i)
        center.add(new Point3d(getFrame().getAtomAt(picked[i]).getPosition()));
      center.scale(1.0f / picked.length); // just divide by the quantity
    }
    getFrame().setRotationCenter(center);
    clearSelection();
    scaleFitToScreen();
    recalc();
  }

  public int getScreenDiameter(int z, double vdwRadius) {
    if (z > 0)
      System.out.println("--?QUE? no way that z > 0--");
    int d = (int)(2 * vdwRadius *
                  scalePixelsPerAngstrom * getAtomSphereFactor());
    if (perspectiveDepth)
      d = (d * cameraZ) / (cameraZ - z);
    return d;
  }

  public double scaleToScreen(int z, double sizeAngstroms) {
    // all z's are <= 0
    // so the more negative z is, the smaller the screen scale
    double pixelSize = sizeAngstroms * scalePixelsPerAngstrom;
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraZ) / (cameraZ - z);
    return pixelSize;
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
  }

  public boolean hasStructuralChange() {
    return structuralChange;
  }

  public void resetStructuralChange() {
    structuralChange = false;
  }

  public final Hashtable imageCache = new Hashtable();
  private void flushCachedImages() {
    imageCache.clear();
    colorTransparentPicked = null;
  }

  // FIXME NEEDSWORK -- bond binding stuff
  private double bondFudge = 1.12f;
  private boolean autoBond = true;

  public void rebond() {
    if (getFrame() != null) {
      try {
        getFrame().rebond();
      } catch (Exception e){
      }
    }
  }

  public void setBondFudge(double bf) {
    bondFudge = bf;
    recalc();
  }

  public double getBondFudge() {
    return bondFudge;
  }

  public void setAutoBond(boolean ab) {
    autoBond = ab;
    recalc();
  }

  public boolean getAutoBond() {
    return autoBond;
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  private Color vectorColor = Color.black;
  private double arrowHeadSize = 10.0f;
  private double arrowHeadRadius = 1.0f;
  private double arrowLengthScale = 1.0f;

  public void setVectorColor(Color c) {
    vectorColor = c;
  }

  public Color getVectorColor() {
    return vectorColor;
  }

  public void setArrowHeadSize(double ls) {
    arrowHeadSize = 10.0f * ls;
  }

  public double getArrowHeadSize() {
    return arrowHeadSize / 10.0f;
  }

  // mth dec 2003
  // for some reason, internal to ArrowLine the raw arrowHeadSize was
  // used, but externally it is multiplied/divided by 10
  // will figure it out and fix it later
  public double getArrowHeadSize10() {
    return arrowHeadSize;
  }

  public void setArrowLengthScale(double ls) {
    arrowLengthScale = ls;
  }

  public double getArrowLengthScale() {
    return arrowLengthScale;
  }

  public void setArrowHeadRadius(double rs) {
    arrowHeadRadius = rs;
  }

  public double getArrowHeadRadius() {
    return arrowHeadRadius;
  }


}
