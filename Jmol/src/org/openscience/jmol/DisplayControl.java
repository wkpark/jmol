/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;
import org.openscience.jmol.render.AtomColors;

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Component;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;

final public class DisplayControl {

  public static DisplayControl control;

  public DisplayControl() {
    control = this;
  }

  public static final int NOLABELS =  0;
  public static final int SYMBOLS =   1;
  public static final int TYPES =     2;
  public static final int NUMBERS =   3;
  public static final int QUICKDRAW = 0;
  public static final int SHADING =   1;
  public static final int WIREFRAME = 2;
  public static final int LINE =      3;
  public static final int ATOMTYPE =   0;
  public static final int ATOMCHARGE = 1;

  
  // while these variables are public, they should be considered *read-only*
  // to write these variables you *must* use the appropriate set function
  // they are currently used by Atom and AtomShape for transforms & rendering
  public boolean mouseDragged = false;
  public int xTranslation;
  public int yTranslation;
  public int cameraZ = 750;
  public final Matrix4d matrixTransform = new Matrix4d();
  private final Point3d point3dScreenTemp = new Point3d();

  private Component panel;

  private int minScreenDimension;
  private Dimension dimCurrent;
  private double scalePixelsPerAngstrom;
  private double scaleDefaultPixelsPerAngstrom;
  private boolean zoomEnabled = true;
  private int zoomPercent = 100;
  private int zoomPercentSetting = 100;
  private double cameraDepth = 3;
  public final Matrix4d matrixRotate = new Matrix4d();
  private final Matrix4d matrixTemp = new Matrix4d();
  private final Vector3d vectorTemp = new Vector3d();
  private boolean perspectiveDepth = false;
  private boolean structuralChange = false;

  public void setAwtComponent(Component component) {
    this.panel = component;
  }

  public Component getAwtComponent() {
    return panel;
  }

  public int modeLabel = NOLABELS;
  public void setModeLabel(int mode) {
    if (modeLabel != mode) {
      modeLabel = mode;
      recalc();
    }
  }

  public int modeAtomDraw = QUICKDRAW;
  public void setModeAtomDraw(int mode) {
    if (modeAtomDraw != mode) {
      modeAtomDraw = mode;
      recalc();
    }
  }

  AtomColorer colorProfile = AtomColors.getInstance();
  public int atomColorProfile = ATOMTYPE;
  public void setAtomColorProfile(int mode) {
    if (atomColorProfile != mode) {
      atomColorProfile = mode;
      if (mode == ATOMTYPE)
        colorProfile = AtomColors.getInstance();
      else
        colorProfile = new PartialAtomicChargeColors();
      recalc();
    }
  }

  public int getAtomColorProfile() {
    return atomColorProfile;
  }

  public int modeBondDraw = QUICKDRAW;
  public void setModeBondDraw(int mode) {
    if (modeBondDraw != mode) {
      modeBondDraw = mode;
      recalc();
    }
  }

  public int percentAngstromBond = 10;
  public void setPercentAngstromBond(int percentAngstromBond) {
    this.percentAngstromBond = percentAngstromBond;
    recalc();
  }

  public Color colorOutline = Color.black;
  public void setColorOutline(Color c) {
    colorOutline = c;
    recalc();
  }

  private Color colorSelection = Color.orange;
  private Color colorSelectionTransparent;
  public void setColorSelection(Color c) {
    if (colorSelection == null || !colorSelection.equals(c)) {
      colorSelection = c;
      colorSelectionTransparent = null;
      recalc();
    }
  }
  public Color getColorSelection() {
    if (colorSelectionTransparent == null) {
      colorSelectionTransparent = 
        useGraphics2D ? getColorTransparent(colorSelection) : colorSelection;
    }
    return colorSelectionTransparent;
  }

  public Color colorRubberband = Color.pink;

  public Color colorText = Color.black;
  public void setColorText(Color c) {
    colorText = c;
    recalc();
  }

  public Color colorDistance = Color.black;
  public void setColorDistance(Color c) {
    colorDistance = c;
    recalc();
  }

  public Color colorAngle = Color.black;
  public void setColorAngle(Color c) {
    colorAngle = c;
    recalc();
  }

  public Color colorDihedral = Color.black;
  public void setColorDihedral(Color c) {
    colorDihedral = c;
    recalc();
  }

  public boolean showAtoms = true;
  public void setShowAtoms(boolean showAtoms) {
    if (this.showAtoms != showAtoms) {
      this.showAtoms = showAtoms;
      recalc();
    }
  }
  public boolean getShowAtoms() {
    return showAtoms;
  }

  public boolean showBonds = true;
  public void setShowBonds(boolean showBonds) {
    if (this.showBonds != showBonds) {
      this.showBonds = showBonds;
      recalc();
    }
  }
  public boolean getShowBonds() {
    return showBonds;
  }

  public boolean showVectors = false;
  public void setShowVectors(boolean showVectors) {
    if (this.showVectors != showVectors) {
      this.showVectors = showVectors;
      structuralChange = true;
      recalc();
    }
  }
  public boolean getShowVectors() {
    return showVectors;
  }

  public boolean showHydrogens = true;
  public void setShowHydrogens(boolean showHydrogens) {
    if (this.showHydrogens != showHydrogens) {
      this.showHydrogens = showHydrogens;
      recalc();
    }
  }
  public boolean getShowHydrogens() {
    return showHydrogens;
  }

  public boolean showDarkerOutline = false;
  public void setShowDarkerOutline(boolean showDarkerOutline) {
    if (this.showDarkerOutline != showDarkerOutline) {
      this.showDarkerOutline = showDarkerOutline;
      recalc();
    }
  }
  public boolean getShowDarkerOutline() {
    return showDarkerOutline;
  }

  public int percentVdwAtom = 20;
  public void setPercentVdwAtom(int percentVdwAtom) {
    this.percentVdwAtom = percentVdwAtom;
    recalc();
  }

  public boolean fastRendering = false;
  public void setFastRendering(boolean fastRendering) {
    if (this.fastRendering != fastRendering) {
      this.fastRendering = fastRendering;
      recalc();
    }
  }
  public boolean getFastRendering() {
    return fastRendering;
  }

  public String propertyMode = "";
  public void setPropertyMode(String s) {
    propertyMode = s;
    recalc();
  }
  public String getPropertyMode() {
    return propertyMode;
  }

  private final BitSet bsNull = new BitSet();
  private final BitSet bsSelection = new BitSet();

  public void addSelection(Atom atom) {
    bsSelection.set(atom.getAtomNumber());
    recalc();
  }

  public void removeSelection(Atom atom) {
    bsSelection.clear(atom.getAtomNumber());
    recalc();
  }

  public void toggleSelection(Atom atom) {
    int atomNum = atom.getAtomNumber();
    if (bsSelection.get(atomNum))
      bsSelection.clear(atomNum);
    else
      bsSelection.set(atomNum);
    recalc();
  }

  public void addSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      bsSelection.set(atoms[i].getAtomNumber());
    recalc();
  }

  public void removeSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      bsSelection.clear(atoms[i].getAtomNumber());
    recalc();
  }

  public void clearSelection() {
    bsSelection.and(bsNull);
    recalc();
  }

  public int countSelection() {
    int count = 0;
    for (int i = 0, size = bsSelection.size(); i < size; ++i)
      if (bsSelection.get(i))
        ++count;
    return count;
  }

  public boolean isSelected(Atom atom) {
    return bsSelection.get(atom.getAtomNumber());
  }

  public void setSelectionSet(BitSet set) {
    bsSelection.and(bsNull);
    bsSelection.or(set);
    recalc();
  }

  public void translateBy(int xDelta, int yDelta) {
    xTranslation += xDelta;
    yTranslation += yDelta;
    recalc();
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
    double ytheta = Math.PI * xDelta / minScreenDimension;
    rotateByY(ytheta * rotateAccelerator);
    double xtheta = Math.PI * yDelta / minScreenDimension;
    rotateByX(xtheta * rotateAccelerator);
    recalc();
  }

  public void zoomBy(int pixels) {
    int percent = pixels * zoomPercentSetting / minScreenDimension;
    if (percent == 0)
      percent = (pixels < 0) ? -1 : 1;
    zoomByPercent(percent);
  }

  public int getZoomPercent() {
    return zoomPercent;
  }

  public void zoomToPercent(int percent) {
    zoomPercentSetting = percent;
    calcZoom();
  }

  public void zoomByPercent(int percent) {
    zoomPercentSetting += percent;
    calcZoom();
  }

  private void calcZoom() {
    if (zoomPercentSetting < 10)
      zoomPercentSetting = 10;
    if (zoomPercentSetting > 1000)
      zoomPercentSetting = 1000;
    zoomPercent = (zoomEnabled) ? zoomPercentSetting : 100;
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom *
      zoomPercent / 100;
    recalc();
  }

  public void setZoomEnabled(boolean zoomEnabled) {
    if (this.zoomEnabled != zoomEnabled) {
      this.zoomEnabled = zoomEnabled;
      calcZoom();
    }
  }

  public Matrix4d getPovRotateMatrix() {
    return new Matrix4d(matrixRotate);
  }

  public Matrix4d getPovTranslateMatrix() {
    Matrix4d matrixPovTranslate = new Matrix4d();
    matrixPovTranslate.setIdentity();
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x = (xTranslation-dimCurrent.width/2) / scalePixelsPerAngstrom;
    vectorTemp.y = -(yTranslation-dimCurrent.height/2)
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
    pointScreen.set(transformPoint(pointAngstroms));
  }

  public Point3d transformPoint(Point3d pointAngstroms) {
    matrixTransform.transform(pointAngstroms, point3dScreenTemp);
    if (perspectiveDepth) {
      int depth = cameraZ - (int)point3dScreenTemp.z;
      point3dScreenTemp.x =
        (((int)point3dScreenTemp.x * cameraZ) / depth) + xTranslation;
      point3dScreenTemp.y =
        (((int)point3dScreenTemp.y * cameraZ) / depth) + yTranslation;
    } else {
      point3dScreenTemp.x += xTranslation;
      point3dScreenTemp.y += yTranslation;
    }
    return point3dScreenTemp;
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

  public void scaleFitToScreen() {
    if (dimCurrent == null || chemframe == null)  {
      // FIXME -- what is proper startup sequence in this case? 
      return;
    }

    // FIXME perspective view resize - mth dec 2003
    // there is some problem with perspective view with the screen is
    // resized larger. only shows up in perspective view. things are being
    // displayed larger than they should be. that is, rotations can go
    // off the edge of the screen. goes away when home is hit

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
      double scaleFactor = (cameraZ + minScreenDimension/2) / (double)cameraZ;
      scaleFactor += .02f; // don't know why I need this, but seems I do -- mth
      scalePixelsPerAngstrom *= scaleFactor;
    }
    // these are important!
    scaleDefaultPixelsPerAngstrom = scalePixelsPerAngstrom;
    zoomPercentSetting = zoomPercent = 100;
    zoomEnabled = true;
    cameraZ = (int)cameraDepth * minScreenDimension;
  }

  public void maybeEnableAntialiasing(Graphics g) {
    if (useGraphics2D && wantsAntialias) {
      Graphics2D g2d = (Graphics2D) g;
      if (wantsAntialiasAlways || !mouseDragged) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
      } else {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_SPEED);
      }
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
    clearSelection();
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
    }
  }

  public void setFrame(ChemFrame frame) {
    chemframe = frame;
    Measurement.setChemFrame(frame);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    structuralChange = true;
    clearSelection();
    recalc();
    //    System.out.println("scalePixelsPerAngstrom="+scalePixelsPerAngstrom+
    //                       " zoomPercentSetting=" + zoomPercentSetting);
  }

  public int numberOfAtoms() {
    return (chemframe == null) ? 0 : chemframe.getNumberOfAtoms();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    MeasurementList source = (MeasurementList) mle.getSource();
    mlist = source;
    chemframe.updateMlists(mlist.getDistanceList(),
                           mlist.getAngleList(),
                           mlist.getDihedralList());
  }

  public Color colorBackground = Color.white;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    recalc();
  }

  public boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
    recalc();
  }

  public void setMouseDragged(boolean mouseDragged) {
    if (wireframeRotation && this.mouseDragged != mouseDragged)
      setFastRendering(mouseDragged);
    if (this.mouseDragged && !mouseDragged) {
      if ((useGraphics2D && wantsAntialias && !wantsAntialiasAlways) ||
          (modeBondDraw == SHADING))
        recalc();
    }
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
    return null;
    //return panel.takeSnapshot();
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

  private boolean holdRepaint;
  private boolean repaintPending;

  public void setHoldRepaint(boolean holdRepaint) {
    if (this.holdRepaint != holdRepaint) {
      this.holdRepaint = holdRepaint;
      if (!holdRepaint && repaintPending)
        panel.repaint();
    }
  }

  Object monitorRepaint = new Object();

  private void recalc() {
    if (repaintPending)
      return;
    repaintPending = true;
    if (! holdRepaint)
      panel.repaint();
  }

  public void refresh() {
    recalc();
  }

  public void requestRepaintAndWait() {
    panel.repaint();
    synchronized(monitorRepaint) {
      try {
        monitorRepaint.wait();
      } catch (InterruptedException e) {
      }
    }
  }

  public void notifyRepainted() {
    repaintPending = false;
    synchronized(monitorRepaint) {
      monitorRepaint.notify();
    }
  }
  
  public void setCenterAsSelected() {
    int numberOfAtoms = numberOfAtoms();
    int countSelected = 0;
    Point3d  center = new Point3d(); // defaults to 0,00,
    for (int i = 0; i < numberOfAtoms; ++i) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(((org.openscience.jmol.Atom)getFrame().getAtomAt(i)).getPosition());
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    getFrame().setRotationCenter(center);
    clearSelection();
    scaleFitToScreen();
    recalc();
  }

  public int screenAtomDiameter(int z, Atom atom) {
    double vdwRadius = atom.getVdwRadius();
    if (z > 0)
      System.out.println("--?QUE? no way that z > 0--");
    if (vdwRadius <= 0)
      System.out.println("--?QUE? vdwRadius=" + vdwRadius);
    int d = (int)(2 * vdwRadius *
                  scalePixelsPerAngstrom * percentVdwAtom / 100);
    if (perspectiveDepth)
      d = (d * cameraZ) / (cameraZ - z);
    return d;
  }

  public int screenBondWidth(int z) {
    int w = (int)(scalePixelsPerAngstrom * percentAngstromBond / 100);
    if (perspectiveDepth)
      w = (w * cameraZ) / (cameraZ - z);
    return w;
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
    colorSelectionTransparent = null;
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
  public Color colorVector = Color.black;
  private double arrowHeadSize = 10.0f;
  private double arrowHeadRadius = 1.0f;
  private double arrowLengthScale = 1.0f;

  public void setColorVector(Color c) {
    colorVector = c;
    recalc();
  }

  public void setArrowHeadSize(double ls) {
    arrowHeadSize = 10.0f * ls;
    recalc();
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
    recalc();
  }

  public double getArrowLengthScale() {
    return arrowLengthScale;
  }

  public void setArrowHeadRadius(double rs) {
    arrowHeadRadius = rs;
    recalc();
  }

  public double getArrowHeadRadius() {
    return arrowHeadRadius;
  }

  public Color getColorAtom(Atom atom) {
    Color color = colorProfile.getAtomColor((org.openscience.cdk.Atom)atom);
    if (modeTransparentColors)
      color = getColorTransparent(color);
    return color;
  }

  public Color getColorAtomOutline(Color color) {
    Color outline = (showDarkerOutline || modeAtomDraw == SHADING)
      ? getDarker(color) : colorOutline;
    if (modeTransparentColors)
      outline = getColorTransparent(outline);
    return outline;
  }

  private Hashtable htDarker = new Hashtable();
  public Color getDarker(Color color) {
    Color darker = (Color) htDarker.get(color);
    if (darker == null) {
      darker = color.darker();
      htDarker.put(color, darker);
    }
    return darker;
  }

  private boolean modeTransparentColors = false;
  public void setModeTransparentColors(boolean modeTransparentColors) {
    this.modeTransparentColors = modeTransparentColors;
  }

  private final static int transparency = 0x60;
  private Hashtable htTransparent = new Hashtable();
  public Color getColorTransparent(Color color) {
    Color transparent = (Color) htTransparent.get(color);
    if (transparent == null) {
      int argb = (color.getRGB() & 0x00FFFFFF) | (transparency << 24);
      transparent = new Color (argb, true);
      htTransparent.put(color, transparent);
    }
    return transparent;
  }

  URL appletDocumentBase = null;
  public void setAppletDocumentBase(URL base) {
    appletDocumentBase = base;
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:"};

  public String openFile(String filename) {
    try {
      InputStream istream = null;
      int i;
      for (i = 0; i < urlPrefixes.length; ++i) {
        if (filename.startsWith(urlPrefixes[i]))
          break;
      }
      if (i < urlPrefixes.length) {
        URL url = new URL(filename);
        istream = url.openStream();
      } else if (appletDocumentBase != null) {
        URL url = new URL(appletDocumentBase, filename);
        istream = url.openStream();
      } else {
        File file = new File(filename);
        istream = new FileInputStream(file);
      }
      openInputStream(istream);
    } catch (Exception e) {
      return "" + e;
    }
    return null;
  }

  private void openInputStream(InputStream istream) throws JmolException {
    InputStreamReader isr = new InputStreamReader(istream);
    BufferedReader bufreader = new BufferedReader(isr);
    try {
      ChemFileReader reader = null;
      try {
        reader = ReaderFactory.createReader(bufreader);
      } catch (IOException ex) {
        throw new JmolException("readMolecule",
            "Error determining input format: " + ex);
      }
      if (reader == null) {
        throw new JmolException("readMolecule", "Unknown input format");
      }
      ChemFile newChemFile = reader.read();

      if (newChemFile != null) {
        if (newChemFile.getNumberOfFrames() > 0) {
          setChemFile(newChemFile);
        } else {
          throw new JmolException("readMolecule",
              "the input appears to be empty");
        }
      } else {
        throw new JmolException("readMolecule",
            "unknown error reading input");
      }
    } catch (IOException ex) {
      throw new JmolException("readMolecule", "Error reading input: " + ex);
    }
  }


  public void scriptEcho(String str) {
    // FIXME -- if there is a script window it should go there
    // for an applet it needs to go someplace else
    System.out.println(str);
  }

  public void translateToXPercent(int percent) {
    // FIXME -- what is the proper RasMol interpretation of this with zooming?
    xTranslation = (dimCurrent.width/2) + dimCurrent.width * percent / 100;
    recalc();
  }

  public void translateToYPercent(int percent) {
    yTranslation = (dimCurrent.height/2) + dimCurrent.height * percent / 100;
    recalc();
  }

  public void translateToZPercent(int percent) {
    // FIXME who knows what this should be? some type of zoom?
    recalc();
  }

  public int getTranslationXPercent() {
    return (xTranslation - dimCurrent.width/2) * 100 / dimCurrent.width;
  }

  public int getTranslationYPercent() {
    return (yTranslation - dimCurrent.height/2) * 100 / dimCurrent.height;
  }

  public int getTranslationZPercent() {
    return 0;
  }

  public void translateByXPercent(int percent) {
    translateToXPercent(getTranslationXPercent() + percent);
  }

  public void translateByYPercent(int percent) {
    translateToYPercent(getTranslationYPercent() + percent);
  }

  public void translateByZPercent(int percent) {
    translateToZPercent(getTranslationZPercent() + percent);
  }

  public void setColorBackground(String colorName) {
    setColorBackground(getColorFromHexString(colorName));
  }

  public void setColorForeground(String colorName) {
    // what is this supposed to do?
    // setColorForeground(getColorFromHexString(colorName));
  }

  public Color getColorFromHexString(String colourName) {

    if ((colourName == null) || (colourName.length() != 7)) {
      throw new IllegalArgumentException("Colour name: " + colourName
          + " is either null ot not seven chars long");
    }
    java.awt.Color colour = null;
    try {
      String rdColour = "0x" + colourName.substring(1, 3);
      String gnColour = "0x" + colourName.substring(3, 5);
      String blColour = "0x" + colourName.substring(5, 7);
      int red = (Integer.decode(rdColour)).intValue();
      int green = (Integer.decode(gnColour)).intValue();
      int blue = (Integer.decode(blColour)).intValue();
      colour = new java.awt.Color(red, green, blue);
    } catch (NumberFormatException e) {
      System.out.println("MDLView: Error extracting colour, using white");
      colour = Color.white;
    }
    return colour;
  }

}
