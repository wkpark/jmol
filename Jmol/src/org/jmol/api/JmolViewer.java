/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

import org.jmol.viewer.Viewer;

/**
 * This is the high-level API for the JmolViewer for simple access.
 * <p>
 * We will implement a low-level API at some point
 **/

abstract public class JmolViewer extends JmolSimpleViewer {

  static public JmolViewer allocateViewer(Component awtComponent,
                                          JmolAdapter jmolAdapter) {
    return Viewer.allocateViewer(awtComponent, jmolAdapter);
  }

  abstract public void setJmolStatusListener(JmolStatusListener jmolStatusListener);

  abstract public void setAppletContext(String htmlName, URL documentBase, URL codeBase,
                               String appletProxy);

  abstract public void haltScriptExecution();

  abstract public boolean isJvm12orGreater();
  abstract public String getOperatingSystemName();
  abstract public String getJavaVersion();
  abstract public String getJavaVendor();

  abstract public boolean haveFrame();

  abstract public void pushHoldRepaint();
  abstract public void popHoldRepaint();

  abstract public void setJmolDefaults();
  abstract public void setRasmolDefaults();
  abstract public void setDebugScript(boolean debugScript);

  abstract public void setFrankOn(boolean frankOn);

  // change this to width, height
  abstract public void setScreenDimension(Dimension dim);
  abstract public int getScreenWidth();
  abstract public int getScreenHeight();

  abstract public Image getScreenImage();
  abstract public void releaseScreenImage();

  abstract public boolean handleOldJvm10Event(Event e);

  abstract public int getMotionEventNumber();

  abstract public void openReader(String fullPathName, String name, Reader reader);
  abstract public void openClientFile(String fullPathName, String fileName,
                             Object clientFile);

  abstract public void showUrl(String urlString);

  abstract public void deleteMeasurement(int i);
  abstract public void clearMeasurements();
  abstract public int getMeasurementCount();
  abstract public String getMeasurementStringValue(int i);
  abstract public int[] getMeasurementCountPlusIndices(int i);

  abstract public Component getAwtComponent();

  abstract public BitSet getElementsPresentBitSet();

  abstract public int getAnimationFps();
  abstract public void setAnimationFps(int framesPerSecond);

  abstract public String script(String script);
  abstract public String scriptWait(String script);
  abstract public void loadInline(String strModel);

  abstract public String evalStringQuiet(String script);
  abstract public boolean isScriptExecuting();

  abstract public void setVectorScale(float vectorScaleValue);
  abstract public void setVibrationScale(float vibrationScaleValue);
  abstract public void setVibrationPeriod(float vibrationPeriod);

  abstract public String getModelSetName();
  abstract public String getModelSetFileName();
  abstract public String getModelSetPathName();
  abstract public Properties getModelSetProperties();
  abstract public int getModelNumber(int atomSetIndex);
  abstract public String getModelName(int atomSetIndex);
  abstract public Properties getModelProperties(int atomSetIndex);
  abstract public String getModelProperty(int atomSetIndex, String propertyName);
  abstract public boolean modelHasVibrationVectors(int atomSetIndex);

  abstract public int getModelCount();
  abstract public int getDisplayModelIndex();
  abstract public int getAtomCount();
  abstract public int getBondCount();
  abstract public int getGroupCount();
  abstract public int getChainCount();
  abstract public int getPolymerCount();
  abstract public int getPolymerCountInModel(int modelIndex);

  abstract public void setModeMouse(int modeMouse);
  abstract public void setSelectionHaloEnabled(boolean haloEnabled);

  abstract public void setShowHydrogens(boolean showHydrogens);
  abstract public void setShowMeasurements(boolean showMeasurements);

  abstract public void selectAll();
  abstract public void clearSelection();

  abstract public void addSelectionListener(JmolSelectionListener listener);
  abstract public void removeSelectionListener(JmolSelectionListener listener);
  abstract public void setSelectionSet(BitSet newSelection);
//BH 2/2006  abstract public BitSet getSelectionSet();

  // get rid of this!
  abstract public void setModeAtomColorProfile(String mode);

  abstract public void homePosition();
  abstract public void rotateFront();
  abstract public void rotateToX(int degrees);
  abstract public void rotateToY(int degrees);

  abstract public void rotateToX(float radians);
  abstract public void rotateToY(float radians);
  abstract public void rotateToZ(float radians);

  abstract public void setCenterSelected();

  abstract public BitSet getGroupsPresentBitSet();

  //deprecated
  abstract public void setWireframeRotation(boolean wireframeRotation);
  abstract public void setPerspectiveDepth(boolean perspectiveDepth);

  abstract public boolean getPerspectiveDepth();
  abstract public boolean getWireframeRotation();
  abstract public boolean getShowHydrogens();
  abstract public boolean getShowMeasurements();

  abstract public void setShowAxes(boolean showAxes);
  abstract public boolean getShowAxes();
  abstract public void setShowBbcage(boolean showBbcage);
  abstract public boolean getShowBbcage();

  abstract public int getAtomNumber(int atomIndex);
  abstract public String getAtomName(int atomIndex);

  abstract public float getRotationRadius();

  abstract public int getZoomPercent();
  abstract public Matrix4f getUnscaledTransformMatrix();

  abstract public Color getColorBackground();
  abstract public void setColorBackground(Color colorBackground);
  abstract public void setColorBackground(String colorName);

  abstract public float getAtomRadius(int atomIndex);
  abstract public Point3f getAtomPoint3f(int atomIndex);
  abstract public Color getAtomColor(int atomIndex);
  abstract public int getAtomModelIndex(int atomIndex);

  abstract public float getBondRadius(int bondIndex);
  abstract public Point3f getBondPoint3f1(int bondIndex);
  abstract public Point3f getBondPoint3f2(int bondIndex);
  abstract public Color getBondColor1(int bondIndex);
  abstract public Color getBondColor2(int bondIndex);
  abstract public short getBondOrder(int bondIndex);
  abstract public int getBondModelIndex(int bondIndex);

  abstract public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex);
  
  abstract public boolean getAxesOrientationRasmol();
  abstract public void setAxesOrientationRasmol(boolean axesMessedUp);
  abstract public int getPercentVdwAtom();
  abstract public void setPercentVdwAtom(int percentVdwAtom);

  abstract public boolean getAutoBond();
  abstract public void setAutoBond(boolean autoBond);

  // EVIL!
  abstract public short getMadBond();
  abstract public void setMarBond(short marBond);

  abstract public float getBondTolerance();
  abstract public void setBondTolerance(float bondTolerance);

  abstract public void rebond();

  abstract public float getMinBondDistance();
  abstract public void setMinBondDistance(float minBondDistance);

  abstract public void setColorSelection(Color colorSelection);
  abstract public Color getColorLabel();
  abstract public void setColorLabel(Color colorBond);
  abstract public Color getColorBond();
  abstract public void setColorBond(Color colorBond);
  abstract public Color getColorVector();
  abstract public void setColorVector(Color colorVector);
  abstract public Color getColorMeasurement();
  abstract public void setColorMeasurement(Color colorMeasurement);

  abstract public void refresh(int isOrientationChange, String strWhy);

  abstract public boolean getBooleanProperty(String propertyName);
  abstract public void setBooleanProperty(String propertyName, boolean value);

  abstract public boolean showModelSetDownload();
  
  abstract public void repaintView();

  abstract public Object getProperty(String returnType, String infoType, String paramInfo);
  
}

