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

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.AxisAngle4f;

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

  public DisplayPanel panel;
  public DisplaySettings settings;

  private Dimension dimCurrent;
  private int minScreenDimension;

  private float scalePixelsPerAngstrom;
  private float scaleDefaultPixelsPerAngstrom;
  private int xTranslate;
  private int yTranslate;
  private final Matrix4f matrixRotate = new Matrix4f();
  private final Matrix4f matrixViewTransform = new Matrix4f();
  private final Matrix4f matrixTemp = new Matrix4f();
  private final Vector3f vectorTemp = new Vector3f();

  DisplayControl(DisplayPanel panel, DisplaySettings settings) {
    this.panel = panel;
    this.settings = settings;
  }

  public DisplayPanel getPanel() {
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

  public void setBondWidth(float width) {
    settings.setBondWidth(width);
    recalc();
  }

  public float getBondWidth() {
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

  public Color getPickedColor() {
    return settings.getPickedColor();
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

  public void setDrawBondsToAtomCenters(boolean drawToCenters) {
    settings.setDrawBondsToAtomCenters(drawToCenters);
    recalc();
  }

  public boolean getDrawBondsToAtomCenters() {
    return settings.getDrawBondsToAtomCenters();
  }

  public void setAtomSphereFactor(float f) {
    settings.setAtomSphereFactor(f);
    recalc();
  }

  public float getAtomSphereFactor() {
    return (float)settings.getAtomSphereFactor();
  }

  public void setAntiAliased(boolean antiAlias) {
    settings.setAntiAliased(antiAlias);
    recalc();
  }

  public boolean isAntiAliased() {
    return settings.isAntiAliased();
  }

  public void setFastRendering(boolean b) {
    settings.setFastRendering(b);
    recalc();
  }

  public boolean getFastRendering() {
    return settings.getFastRendering();
  }

  public void addPickedAtom(Atom atom) {
    settings.addPickedAtom(atom);
    recalc();
  }

  public void removePickedAtom(Atom atom) {
    settings.removePickedAtom(atom);
    recalc();
  }

  public void addPickedAtoms(Atom[] atoms) {
    settings.addPickedAtoms(atoms);
    recalc();
  }

  public void removePickedAtoms(Atom[] atoms) {
    settings.removePickedAtoms(atoms);
    recalc();
  }

  public IntSet getPickedAtoms() {
    return settings.getPickedAtoms();
  }
  
  public void clearPickedAtoms() {
    settings.clearPickedAtoms();
    recalc();
  }

  public boolean isAtomPicked(Atom atom) {
    return settings.isAtomPicked(atom);
  }

  /*
  public void setScalePixelsPerAngstrom() {
    panel.setScalePixelsPerAngstrom();
  }
  */

  public float getScalePixelsPerAngstrom() {
    return scalePixelsPerAngstrom;
  }

  public void translateBy(int xDelta, int yDelta) {
    xTranslate += xDelta;
    yTranslate += yDelta;
  }

  public void rotateBy(int xDelta, int yDelta) {
    // what fraction of PI radians do you want to rotate?
    // the full screen width corresponds to a PI (180 degree) rotation
    // if you grab an atom near the outside edge of the molecule,
    // you can essentially "pull it" across the screen and it will
    // track with the mouse cursor
    // the current zoom factor should be taken into account here

    // a change in the x coordinate generates a rotation about the y axis
    float ytheta = (float)Math.PI * xDelta / minScreenDimension;
    rotateByY(ytheta);
    float xtheta = (float)Math.PI * yDelta / minScreenDimension;
    rotateByX(xtheta);
    recalc();
  }

  public void multiplyZoomScale(float scale) {
    scalePixelsPerAngstrom *= scale;
    recalc();
  }

  public float getZoomScale() {
    return scalePixelsPerAngstrom / scaleDefaultPixelsPerAngstrom;
  }

  public Matrix4f getPovRotateMatrix() {
    return new Matrix4f(matrixRotate);
  }

  public Matrix4f getPovTranslateMatrix() {
    Matrix4f matrixPovTranslate = new Matrix4f();
    matrixPovTranslate.setIdentity();
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x = (xTranslate - dimCurrent.width/2) / scalePixelsPerAngstrom;
    vectorTemp.y = -(yTranslate - dimCurrent.height/2)
      / scalePixelsPerAngstrom; // invert y axis
    vectorTemp.z = 0;
    matrixPovTranslate.set(vectorTemp);
    return matrixPovTranslate;
  }

  public Matrix4f getViewTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixViewTransform.setIdentity();
    // first, translate the coordinates back to the center
    vectorTemp.set(getFrame().getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixViewTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixViewTransform.mul(matrixRotate);
    matrixViewTransform.mul(matrixRotate, matrixViewTransform);
    // now shift so that all z coordinates are <= 0
    // this is important for scaling
    vectorTemp.x = 0;
    vectorTemp.y = 0;
    vectorTemp.z = getFrame().getRotationRadius();
    matrixTemp.setTranslation(vectorTemp);
    matrixViewTransform.sub(matrixTemp);
    // now scale to screen coordinates
    matrixTemp.set(scalePixelsPerAngstrom);
    matrixTemp.m11=-scalePixelsPerAngstrom; // invert y dimension
    matrixViewTransform.mul(matrixTemp, matrixViewTransform);
    // now translate to the translate coordinates
    matrixTemp.setZero();
    // This z dimension is here because of the approximations used
    // to calculate atom sizes
    vectorTemp.x = xTranslate;
    vectorTemp.y = yTranslate;
    vectorTemp.z = minScreenDimension/2;
    matrixTemp.setTranslation(vectorTemp);
    matrixViewTransform.add(matrixTemp);
    return matrixViewTransform;
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
    xTranslate = dimCurrent.width / 2;
    yTranslate = dimCurrent.height / 2;
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
    scaleDefaultPixelsPerAngstrom = scalePixelsPerAngstrom;
  }

  public void maybeEnableAntialiasing(Graphics g) {
    if (!mouseDragged && antialiasCapable && settings.isAntiAliased()) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
    }
  }
  public void maybeDottedStroke(Graphics g) {
    if (antialiasCapable) {
      Graphics2D g2 = (Graphics2D) g;
      BasicStroke dotted =
        new BasicStroke(1, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 0,
                        new float[] {3, 3}, 0);
      g2.setStroke(dotted);
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

  private boolean mouseDragged = false;
  public void setMouseDragged(boolean mouseDragged) {
    if (wireframeRotation && this.mouseDragged != mouseDragged)
      settings.setFastRendering(mouseDragged);
    if (this.mouseDragged && !mouseDragged)
      recalc();
    this.mouseDragged = mouseDragged;
  }

  public boolean isMouseDragged() {
    return mouseDragged;
  }

  private boolean antialiasCapable = false;
  public void setAntialiasCapable(boolean antialiasCapable) {
    this.antialiasCapable = antialiasCapable;
  }

  public boolean isAntialiasCapable() {
    return antialiasCapable;
  }

  public boolean isAntialiased() {
    return antialiasCapable && settings.isAntiAliased();
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

  public void rotateToX(float angleRadians) {
    matrixRotate.rotX(angleRadians);
    recalc();
  }
  public void rotateToY(float angleRadians) {
    matrixRotate.rotY(angleRadians);
    recalc();
  }
  public void rotateToZ(float angleRadians) {
    matrixRotate.rotZ(angleRadians);
    recalc();
  }

  public void rotateToX(int angleDegrees) {
    rotateToX((float)Math.toRadians(angleDegrees));
  }
  public void rotateToY(int angleDegrees) {
    rotateToY((float)Math.toRadians(angleDegrees));
  }
  public void rotateToZ(int angleDegrees) {
    rotateToZ((float)Math.toRadians(angleDegrees));
  }

  public void rotateByX(float angleRadians) {
    matrixTemp.rotX(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByY(float angleRadians) {
    matrixTemp.rotY(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByZ(float angleRadians) {
    matrixTemp.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void rotateByX(int angleDegrees) {
    rotateByX((float)Math.toRadians(angleDegrees));
  }
  public void rotateByY(int angleDegrees) {
    rotateByY((float)Math.toRadians(angleDegrees));
  }
  public void rotateByZ(int angleDegrees) {
    rotateByZ((float)Math.toRadians(angleDegrees));
  }
  public void rotate(AxisAngle4f axisAngle) {
    matrixTemp.setIdentity();
    matrixTemp.setRotation(axisAngle);
    matrixRotate.mul(matrixTemp, matrixRotate);
    recalc();
  }
  public void setCenter(Point3f center) {
    getFrame().setRotationCenter(center);
  }

  public void setCenterAsSelected() {
    int[] picked = getPickedAtoms().elements();
    Point3f center = null;
    if (picked.length > 0) {
      // just take the average of all the points
      center = new Point3f(); // defaults to 0,0,0
      for (int i = 0; i < picked.length; ++i)
        center.add(getFrame().getAtomAt(picked[i]).getPosition());
      center.scale(1.0f / picked.length); // just divide by the quantity
    }
    getFrame().setRotationCenter(center);
    clearPickedAtoms();
    scaleFitToScreen();
    recalc();
  }

  public final static int cameraZ = 750;
  public int getScreenDiameter(int z, float vdwRadius) {
    // all z's are <= 0
    // so the more negative z is, the smaller the radius
    float d = 2 * vdwRadius * scalePixelsPerAngstrom * getAtomSphereFactor();
    return (int)((d * cameraZ) / (cameraZ - z));
  }

  public float scaleToScreen(int z, float sizeAngstroms) {
    // all z's are <= 0
    // so the more negative z is, the smaller the screen scale
    float pixelSize = sizeAngstroms * scalePixelsPerAngstrom;
    return ((pixelSize * cameraZ) / (cameraZ - z));
  }

  public float[] getLightSource() {
    return settings.getLightSourceVector();
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
  }
}
