/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
  ColorManager colorManager;
  TransformManager transformManager;
  SelectionManager selectionManager;

  public DisplayControl() {
    control = this;
    colorManager = new ColorManager(this);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager();
  }

  public final static int NOLABELS =  0;
  public final static int SYMBOLS =   1;
  public final static int TYPES =     2;
  public final static int NUMBERS =   3;
  public final static int QUICKDRAW = 0;
  public final static int SHADING =   1;
  public final static int WIREFRAME = 2;
  public final static int LINE =      3;
  public final static int ATOMTYPE =   0;
  public final static int ATOMCHARGE = 1;

  // while these variables are public, they should be considered *read-only*
  // to write these variables you *must* use the appropriate set function
  // they are currently used by Atom and AtomShape for transforms & rendering
  public boolean inMotion = false;

  private Component panel;

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
  public int getPercentAngstromBond() {
    return percentAngstromBond;
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

  public boolean showMeasurements = false;
  public void setShowMeasurements(boolean showMeasurements) {
    if (this.showMeasurements != showMeasurements) {
      this.showMeasurements = showMeasurements;
      structuralChange = true;
      recalc();
    }
  }
  public boolean getShowMeasurements() {
    return showMeasurements;
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
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

  public int getPercentVdwAtom() {
    return percentVdwAtom;
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

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    clearSelection();
    transformManager.homePosition();
    recalc();
  }

  public void maybeEnableAntialiasing(Graphics g) {
    if (useGraphics2D && wantsAntialias) {
      Graphics2D g2d = (Graphics2D) g;
      if (wantsAntialiasAlways || !inMotion) {
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
    // FIXME -- I think I need to disable repaints during this process
    this.chemfile = chemfile;
    haveFile = true;
    nframes = chemfile.getNumberOfFrames();
    this.chemframe = chemfile.getFrame(0);
    Measurement.setChemFrame(chemframe);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    homePosition();
    // don't know if I need this firm recalc here or not
    recalcFirmly();
  }

  public boolean haveFile() {
    return haveFile;
  }

  public ChemFrame getFrame() {
    return chemframe;
  }

  public double getRotationRadius() {
    return chemframe.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return chemframe.getRotationCenter();
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


  public boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
    recalc();
  }

  public boolean getWireframeRotation() {
    return wireframeRotation;
  }

  public void setInMotion(boolean inMotion) {
    if (wireframeRotation && this.inMotion != inMotion)
      setFastRendering(inMotion);
    if (this.inMotion && !inMotion) {
      if ((useGraphics2D && wantsAntialias && !wantsAntialiasAlways) ||
          (modeBondDraw == SHADING))
        recalc();
    }
    this.inMotion = inMotion;
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

  public void setCenter(Point3d center) {
    chemframe.setRotationCenter(center);
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

  private void recalcFirmly() {
    panel.repaint();
  }

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
    synchronized(monitorRepaint) {
      panel.repaint();
      try {
        monitorRepaint.wait();
      } catch (InterruptedException e) {
      }
    }
  }

  public void notifyRepainted() {
    synchronized(monitorRepaint) {
      repaintPending = false;
      monitorRepaint.notify();
    }
  }
  
  public void setCenterAsSelected() {
    int numberOfAtoms = numberOfAtoms();
    int countSelected = 0;
    Point3d  center = new Point3d(); // defaults to 0,00,
    BitSet bsSelection = getSelectionSet();
    for (int i = 0; i < numberOfAtoms; ++i) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(((org.openscience.jmol.Atom)chemframe.getAtomAt(i)).getPosition());
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    chemframe.setRotationCenter(center);
    clearSelection();
    scaleFitToScreen();
    recalc();
  }

  public boolean hasStructuralChange() {
    return structuralChange;
  }

  public void defineMeasure(int atom1, int atom2) {
    mlist.addDistance(atom1, atom2);
    recalc();
  }

  public void defineMeasure(int atom1, int atom2, int atom3) {
    mlist.addAngle(atom1, atom2, atom3);
    recalc();
  }

  public void defineMeasure(int atom1, int atom2, int atom3, int atom4) {
    mlist.addDihedral(atom1, atom2, atom3, atom4);
    recalc();
  }

  public void resetStructuralChange() {
    structuralChange = false;
  }

  public final Hashtable imageCache = new Hashtable();
  private void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  // FIXME NEEDSWORK -- bond binding stuff
  private double bondFudge = 1.12f;
  private boolean autoBond = true;

  public void rebond() {
    if (chemframe != null) {
      try {
        chemframe.rebond();
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
  private double arrowHeadSize = 10.0f;
  private double arrowHeadRadius = 1.0f;
  private double arrowLengthScale = 1.0f;

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

  URL appletDocumentBase = null;
  public void setAppletDocumentBase(URL base) {
    appletDocumentBase = base;
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:"};

  public URL getURLFromName(String name) {
    URL url = null;
    int i;
    for (i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i]))
        break;
    }
    try {
      if (i < urlPrefixes.length)
        url = new URL(name);
      else if (appletDocumentBase != null)
        url = new URL(appletDocumentBase, name);
      else
        url = new URL("file", null, name);
    } catch (MalformedURLException e) {
    }
    return url;
  }

  public InputStream getInputStreamFromName(String name) {
    URL url = getURLFromName(name);
    if (url != null) {
      try {
        return url.openStream();
      } catch (IOException e) {
      }
    }
    return null;
  }

  public String openFile(String name) {
    InputStream istream = getInputStreamFromName(name);
    if (istream == null)
        return "error opening url/filename " + name;
    try {
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

  /****************************************************************
   delegated to TransformManager
  ****************************************************************/

  public void rotateXYBy(int xDelta, int yDelta) {
    transformManager.rotateXYBy(xDelta, yDelta);
    recalc();
  }

  public void rotateZBy(int zDelta) {
    transformManager.rotateZBy(zDelta);
    recalc();
  }

  public void rotateFront() {
    transformManager.rotateFront();
    recalc();
  }

  public void rotateToX(double angleRadians) {
    transformManager.rotateToX(angleRadians);
    recalc();
  }
  public void rotateToY(double angleRadians) {
    transformManager.rotateToY(angleRadians);
    recalc();
  }
  public void rotateToZ(double angleRadians) {
    transformManager.rotateToZ(angleRadians);
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
    transformManager.rotateByX(angleRadians);
    recalc();
  }
  public void rotateByY(double angleRadians) {
    transformManager.rotateByY(angleRadians);
    recalc();
  }
  public void rotateByZ(double angleRadians) {
    transformManager.rotateByZ(angleRadians);
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
    transformManager.rotate(axisAngle);
    recalc();
  }

  public void translateXYBy(int xDelta, int yDelta) {
    transformManager.translateXYBy(xDelta, yDelta);
    recalc();
  }

  public void translateToXPercent(int percent) {
    transformManager.translateToXPercent(percent);
    recalc();
  }

  public void translateToYPercent(int percent) {
    transformManager.translateToYPercent(percent);
    recalc();
  }

  public void translateToZPercent(int percent) {
    transformManager.translateToZPercent(percent);
    recalc();
  }

  public int getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  public int getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  public int getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
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

  public void zoomBy(int pixels) {
    transformManager.zoomBy(pixels);
    recalc();
  }

  public int getZoomPercent() {
    return transformManager.zoomPercent;
  }

  public int getZoomPercentSetting() {
    return transformManager.zoomPercentSetting;
  }

  public void zoomToPercent(int percent) {
    transformManager.zoomToPercent(percent);
    recalc();
  }

  public void zoomByPercent(int percent) {
    transformManager.zoomByPercent(percent);
    recalc();
  }

  public void setZoomEnabled(boolean zoomEnabled) {
    transformManager.setZoomEnabled(zoomEnabled);
    recalc();
  }

  public boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  public int getSlabPercentSetting() {
    return transformManager.slabPercentSetting;
  }

  public void slabBy(int pixels) {
    transformManager.slabBy(pixels);
    recalc();
  }

  public void slabToPercent(int percentSlab) {
    transformManager.slabToPercent(percentSlab);
    recalc();
  }

  public void setSlabEnabled(boolean slabEnabled) {
    transformManager.setSlabEnabled(slabEnabled);
    recalc();
  }

  public void setModeSlab(int modeSlab) {
    transformManager.setModeSlab(modeSlab);
    recalc();
  }

  public int getModeSlab() {
    return transformManager.modeSlab;
  }

  public int getSlabValue() {
    return transformManager.slabValue;
  }

  public Matrix4d getPovRotateMatrix() {
    return transformManager.getPovRotateMatrix();
  }

  public Matrix4d getPovTranslateMatrix() {
    return transformManager.getPovTranslateMatrix();
  }

  public void calcViewTransformMatrix() {
    transformManager.calcViewTransformMatrix();
  }

  public void transformPoint(Point3d pointAngstroms, Point3d pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public Point3d transformPoint(Point3d pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public int screenAtomDiameter(int z, Atom atom) {
    return transformManager.screenAtomDiameter(z, atom, percentVdwAtom);
  }

  public int screenBondWidth(int z) {
    return transformManager.screenBondWidth(z, percentAngstromBond);
  }

  public double scaleToScreen(int z, double sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  public void setScreenDimension(Dimension dimCurrent) {
    transformManager.setScreenDimension(dimCurrent);
  }

  public Dimension getScreenDimension() {
    return transformManager.dimCurrent;
  }

  public void scaleFitToScreen() {
    transformManager.scaleFitToScreen();
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    transformManager.setPerspectiveDepth(perspectiveDepth);
  }

  public boolean getPerspectiveDepth() {
    return transformManager.perspectiveDepth;
  }

  public void setCameraDepth(double depth) {
    transformManager.setCameraDepth(depth);
  }

  public double getCameraDepth() {
    return transformManager.cameraDepth;
  }

  public int getCameraZ() {
    return transformManager.cameraZ;
  }

  /****************************************************************
   delegated to ColorManager
  ****************************************************************/

  public void setModeAtomColorProfile(int mode) {
    colorManager.setModeAtomColorProfile(mode);
    recalc();
  }

  public int getModeAtomColorProfile() {
    return colorManager.modeAtomColorProfile;
  }

  public void setColorOutline(Color c) {
    colorManager.setColorOutline(c);
    recalc();
  }

  public Color getColorOutline() {
    return colorManager.colorOutline;
  }

  public void setColorSelection(Color c) {
    colorManager.setColorSelection(c);
    recalc();
  }

  public Color getColorSelection() {
    return colorManager.getColorSelection();
  }

  public Color getColorRubberband() {
    return colorManager.colorRubberband;
  }

  public void setColorText(Color c) {
    colorManager.setColorText(c);
    recalc();
  }
  public Color getColorText() {
    return colorManager.colorText;
  }

  public void setColorDistance(Color c) {
    colorManager.setColorDistance(c);
    recalc();
  }

  public Color getColorDistance() {
    return colorManager.colorDistance;
  }

  public void setColorAngle(Color c) {
    colorManager.setColorAngle(c);
    recalc();
  }

  public Color getColorAngle() {
    return colorManager.colorAngle;
  }

  public void setColorDihedral(Color c) {
    colorManager.setColorDihedral(c);
    recalc();
  }
  public Color getColorDihedral() {
    return colorManager.colorDihedral;
  }

  public void setColorVector(Color c) {
    colorManager.setColorVector(c);
    recalc();
  }

  public Color getColorVector() {
    return colorManager.colorVector;
  }

  public void setColorBackground(Color bg) {
    colorManager.setColorBackground(bg);
    recalc();
  }

  public Color getColorBackground() {
    return colorManager.colorBackground;
  }
  
  public void setColorBackground(String colorName) {
    colorManager.setColorBackground(colorName);
  }

  public void setColorForeground(String colorName) {
    colorManager.setColorForeground(colorName);
  }

  public Color getColorFromHexString(String colorName) {
    return colorManager.getColorFromHexString(colorName);
  }
  public Color getColorAtom(Atom atom) {
    return colorManager.getColorAtom(atom);
  }

  public Color getColorAtomOutline(Color color) {
    return colorManager.getColorAtomOutline(color);
  }

  public Color getDarker(Color color) {
    return colorManager.getDarker(color);
  }

  public void setModeTransparentColors(boolean modeTransparentColors) {
    colorManager.setModeTransparentColors(modeTransparentColors);
  }

  public Color getColorTransparent(Color color) {
    return colorManager.getColorTransparent(color);
  }

  /****************************************************************
   delegated to SelectionManager
  ****************************************************************/

  public void addSelection(Atom atom) {
    selectionManager.addSelection(atom);
    recalc();
  }

  public void removeSelection(Atom atom) {
    selectionManager.removeSelection(atom);
    recalc();
  }

  public void toggleSelection(Atom atom) {
    selectionManager.toggleSelection(atom);
    recalc();
  }

  public void addSelection(Atom[] atoms) {
    selectionManager.addSelection(atoms);
    recalc();
  }

  public void removeSelection(Atom[] atoms) {
    selectionManager.removeSelection(atoms);
    recalc();
  }

  public void clearSelection() {
    selectionManager.clearSelection();
    recalc();
  }

  public int countSelection() {
    return selectionManager.countSelection();
  }

  public boolean isSelected(Atom atom) {
    return selectionManager.isSelected(atom);
  }

  public void setSelectionSet(BitSet set) {
    selectionManager.setSelectionSet(set);
    recalc();
  }

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }
}
