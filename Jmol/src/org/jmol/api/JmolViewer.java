/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
package org.jmol.api;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Color;
import java.awt.Image;
import java.net.URL;
import java.util.BitSet;
import java.util.Properties;
import java.io.Reader;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

/**
 * This is the high-level API for the JmolViewer for simple access.
 * <p>
 * We will implement a low-level API at some point
 **/

public interface JmolViewer extends JmolSimpleViewer {

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener);

  public void setAppletContext(URL documentBase, URL codeBase,
                               String appletProxy);

  public void haltScriptExecution();

  public boolean isJvm12orGreater();
  public String getOperatingSystemName();
  public String getJavaVersion();

  public boolean haveFrame();

  public void pushHoldRepaint();
  public void popHoldRepaint();

  public void setJmolDefaults();
  public void setRasmolDefaults();
  public void setDebugScript(boolean debugScript);

  public void setFrankOn(boolean frankOn);

  // change this to width, height
  public void setScreenDimension(Dimension dim);
  public int getScreenWidth();
  public int getScreenHeight();

  public Image getScreenImage();
  public void releaseScreenImage();


  public void notifyRepainted();

  public boolean handleOldJvm10Event(Event e);

  public int getMotionEventNumber();

  public void openReader(String fullPathName, String name, Reader reader);
  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile);

  public void showUrl(String urlString);

  public void deleteMeasurement(int i);
  public void clearMeasurements();
  public int getMeasurementCount();
  public String getMeasurementStringValue(int i);
  public int[] getMeasurementCountPlusIndices(int i);

  public Component getAwtComponent();

  public BitSet getElementsPresentBitSet();

  public int getAnimationFps();
  public void setAnimationFps(int framesPerSecond);

  public String evalStringQuiet(String script);

  public void setVectorScale(float vectorScaleValue);
  public void setVibrationScale(float vibrationScaleValue);
  public void setVibrationPeriod(float vibrationPeriod);

  public String getModelSetName();
  public String getModelSetFileName();
  public String getModelSetPathName();
  public Properties getModelSetProperties();
  public int getModelNumber(int atomSetIndex);
  public String getModelName(int atomSetIndex);
  public Properties getModelProperties(int atomSetIndex);
  public String getModelProperty(int atomSetIndex, String propertyName);
  public boolean modelHasVibrationVectors(int atomSetIndex);

  public int getModelCount();
  public int getAtomCount();
  public int getBondCount();
  public int getGroupCount();
  public int getChainCount();
  public int getPolymerCount();

  public void setModeMouse(int modeMouse);
  public void setSelectionHaloEnabled(boolean haloEnabled);

  public void setShowHydrogens(boolean showHydrogens);
  public void setShowMeasurements(boolean showMeasurements);

  public void selectAll();
  public void clearSelection();

  // get rid of this!
  public void setModeAtomColorProfile(byte mode);

  public void homePosition();
  public void rotateFront();
  public void rotateToX(int degrees);
  public void rotateToY(int degrees);

  public void rotateToX(float radians);
  public void rotateToY(float radians);
  public void rotateToZ(float radians);

  public void setCenterSelected();

  public BitSet getGroupsPresentBitSet();

  //deprecated
  public void setWireframeRotation(boolean wireframeRotation);
  public void setPerspectiveDepth(boolean perspectiveDepth);

  public boolean getPerspectiveDepth();
  public boolean getWireframeRotation();
  public boolean getShowHydrogens();
  public boolean getShowMeasurements();

  public void setShowAxes(boolean showAxes);
  public boolean getShowAxes();
  public void setShowBbcage(boolean showBbcage);
  public boolean getShowBbcage();

  public int getAtomNumber(int atomIndex);
  public String getAtomName(int atomIndex);

  public float getRotationRadius();

  public int getZoomPercent();
  public Matrix4f getUnscaledTransformMatrix();

  public Color getColorBackground();
  public void setColorBackground(Color colorBackground);
  public void setColorBackground(String colorName);

  public float getAtomRadius(int atomIndex);
  public Point3f getAtomPoint3f(int atomIndex);
  public Color getAtomColor(int atomIndex);

  public float getBondRadius(int bondIndex);

  public Point3f getBondPoint3f1(int bondIndex);
  public Point3f getBondPoint3f2(int bondIndex);
  public Color getBondColor1(int bondIndex);
  public Color getBondColor2(int bondIndex);
  public short getBondOrder(int bondIndex);

  public boolean getAxesOrientationRasmol();
  public void setAxesOrientationRasmol(boolean axesMessedUp);
  public int getPercentVdwAtom();
  public void setPercentVdwAtom(int percentVdwAtom);

  public boolean getAutoBond();
  public void setAutoBond(boolean autoBond);

  // EVIL!
  public short getMadBond();
  public void setMarBond(short marBond);

  public float getBondTolerance();
  public void setBondTolerance(float bondTolerance);

  public void rebond();

  public float getMinBondDistance();
  public void setMinBondDistance(float minBondDistance);

  public void setColorSelection(Color colorSelection);
  public Color getColorLabel();
  public void setColorLabel(Color colorBond);
  public Color getColorBond();
  public void setColorBond(Color colorBond);
  public Color getColorVector();
  public void setColorVector(Color colorVector);
  public Color getColorMeasurement();
  public void setColorMeasurement(Color colorMeasurement);

  public void refresh();

  public boolean getBooleanProperty(String propertyName);
  public void setBooleanProperty(String propertyName, boolean value);

  public boolean showModelSetDownload();
}
