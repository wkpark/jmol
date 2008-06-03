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
import java.awt.Image;
import java.net.URL;
import java.util.BitSet;
import java.util.Properties;
import java.util.Hashtable;
import java.io.Reader;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.jmol.viewer.Viewer;

/**
 * This is the high-level API for the JmolViewer for simple access.
 * <p>
 * We will implement a low-level API at some point
 * 
 *
 **/

abstract public class JmolViewer extends JmolSimpleViewer {

  /**
   *  This is the main access point for creating an application
   *  or applet viewer. After allocation it is MANDATORY that one of 
   *  the next commands is either 
   *  
   *    viewer.evalString("ZAP");
   *    
   *    or at least:
   *    
   *    viewer.setAppletContext("",null,null,"")
   *    
   *    One or the other of these is necessary to establish the 
   *    first modelset, which might be required by one or more
   *    later evaluated commands or file loadings.
   *    
   * 
   * @param awtComponent
   * @param jmolAdapter
   * @return              a JmolViewer object
   */
  static public JmolViewer allocateViewer(Component awtComponent,
                                          JmolAdapter jmolAdapter) {
    
    return Viewer.allocateViewer(awtComponent, jmolAdapter);
  }

  static public String getJmolVersion() {
    return Viewer.getJmolVersion();
  }

  static public boolean checkOption(JmolViewer viewer, String option) {
    Object testFlag = viewer.getParameter(option);
    return (testFlag instanceof Boolean && ((Boolean) testFlag).booleanValue()
        || testFlag instanceof Integer && ((Integer) testFlag).intValue() != 0);
  }

  // for POV-Ray -- returns the INI file
  
  abstract public String generateOutput(String type, String fileName, int width, int height); 

  abstract public void setJmolStatusListener(JmolStatusListener jmolStatusListener);

  abstract public void setAppletContext(String htmlName, URL documentBase, URL codeBase,
                               String appletProxy);

  abstract public boolean checkHalt(String strCommand);
  abstract public void haltScriptExecution();

  abstract public boolean isJvm12orGreater();
  abstract public String getOperatingSystemName();
  abstract public String getJavaVersion();
  abstract public String getJavaVendor();

  abstract public boolean haveFrame();

  abstract public void pushHoldRepaint();
  abstract public void popHoldRepaint();

  // for example: getData("selected","XYZ");
  abstract public String getData(String atomExpression, String type);


  // change this to width, height
  abstract public void setScreenDimension(Dimension dim);
  abstract public int getScreenWidth();
  abstract public int getScreenHeight();

  abstract public Image getScreenImage();
  abstract public void releaseScreenImage();
  
  abstract public void createImage(String file, Object type_or_text_or_bytes, int quality,
                                   int width, int height);

  abstract public boolean handleOldJvm10Event(Event e);

  abstract public int getMotionEventNumber();

  abstract public void openReader(String fullPathName, String name, Reader reader);
  abstract public void openClientFile(String fullPathName, String fileName,
                             Object clientFile);

  abstract public void showUrl(String urlString);


  abstract public int getMeasurementCount();
  abstract public String getMeasurementStringValue(int i);
  abstract public int[] getMeasurementCountPlusIndices(int i);

  abstract public Component getAwtComponent();

  abstract public BitSet getElementsPresentBitSet(int modelIndex);

  abstract public int getAnimationFps();

  abstract public String script(String script);
  abstract public String scriptCheck(String script);
  abstract public String scriptWait(String script);
  abstract public Object scriptWaitStatus(String script, String statusList);
  abstract public void loadInline(String strModel);
  abstract public void loadInline(String strModel, boolean isMerge);
  abstract public void loadInline(String strModel, char newLine);
  abstract public void loadInline(String[] arrayModels);
  abstract public void loadInline(String[] arrayModels, boolean isMerge);

  abstract public String evalStringQuiet(String script);
  abstract public boolean isScriptExecuting();

  abstract public String getModelSetName();
  abstract public String getModelSetFileName();
  abstract public String getModelSetPathName();
  abstract public String getFileAsString(String filename);
  abstract public Properties getModelSetProperties();
  abstract public Hashtable getModelSetAuxiliaryInfo();
  abstract public int getModelNumber(int modelIndex);
  abstract public String getModelName(int modelIndex);
  abstract public String getModelNumberDotted(int modelIndex);
  abstract public Properties getModelProperties(int modelIndex);
  abstract public String getModelProperty(int modelIndex, String propertyName);
  abstract public Hashtable getModelAuxiliaryInfo(int modelIndex);
  abstract public Object getModelAuxiliaryInfo(int modelIndex, String keyName);
  abstract public boolean modelHasVibrationVectors(int modelIndex);

  abstract public int getModelCount();
  abstract public int getDisplayModelIndex(); // can return -2 - modelIndex if a background model is displayed
  abstract public int getAtomCount();
  abstract public int getBondCount(); // NOT THE REAL BOND COUNT -- just an array maximum
  abstract public int getGroupCount();
  abstract public int getChainCount();
  abstract public int getPolymerCount();
  abstract public int getAtomCountInModel(int modelIndex);
  abstract public int getBondCountInModel(int modelIndex);  // use -1 here for "all"
  abstract public int getGroupCountInModel(int modelIndex);
  abstract public int getChainCountInModel(int modelIindex);
  abstract public int getPolymerCountInModel(int modelIndex);
  abstract public int getSelectionCount();

  abstract public void addSelectionListener(JmolSelectionListener listener);
  abstract public void removeSelectionListener(JmolSelectionListener listener);
//BH 2/2006  abstract public BitSet getSelectionSet();

  abstract public void homePosition();

  abstract public Hashtable getHeteroList(int modelIndex);


  abstract public boolean getPerspectiveDepth();
  abstract public boolean getShowHydrogens();
  abstract public boolean getShowMeasurements();
  abstract public boolean getShowAxes();
  abstract public boolean getShowBbcage();

  abstract public int getAtomNumber(int atomIndex);
  abstract public String getAtomName(int atomIndex);
  abstract public String getAtomInfo(int atomIndex);

  abstract public float getRotationRadius();

  abstract public int getZoomPercent(); //deprecated
  abstract public float getZoomPercentFloat();
  abstract public Matrix4f getUnscaledTransformMatrix();

  abstract public int getBackgroundArgb();
  
  abstract public float getAtomRadius(int atomIndex);
  abstract public Point3f getAtomPoint3f(int atomIndex);
  abstract public int getAtomArgb(int atomIndex);
  abstract public int getAtomModelIndex(int atomIndex);

  abstract public float getBondRadius(int bondIndex);
  abstract public Point3f getBondPoint3f1(int bondIndex);
  abstract public Point3f getBondPoint3f2(int bondIndex);
  abstract public int getBondArgb1(int bondIndex);
  abstract public int getBondArgb2(int bondIndex);
  abstract public short getBondOrder(int bondIndex);
  abstract public int getBondModelIndex(int bondIndex);

  abstract public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex);
  
  abstract public boolean getAxesOrientationRasmol();
  abstract public int getPercentVdwAtom();

  abstract public boolean getAutoBond();

  abstract public short getMadBond();

  abstract public float getBondTolerance();

  abstract public void rebond();

  abstract public float getMinBondDistance();

  abstract public void refresh(int isOrientationChange, String strWhy);

  abstract public boolean showModelSetDownload();
  
  abstract public void repaintView();

  abstract public boolean getBooleanProperty(String propertyName);
  abstract public boolean getBooleanProperty(String key, boolean doICare);
  abstract public Object getParameter(String name);
  abstract public Object getProperty(String returnType, String infoType, String paramInfo);

  abstract public String getSetHistory(int howFarBack);
  
  abstract public boolean havePartialCharges();

  abstract public boolean isApplet();

  abstract public String getAltLocListInModel(int modelIndex);

  abstract public String getStateInfo();
  
  abstract public void syncScript(String script, String applet);  

  //but NOTE that if you use the following, you are
  //bypassing the script history. If you want a script history, use
  //viewer.script("set " + propertyName + " " + value);
  
  abstract public void setColorBackground(String colorName);
  abstract public void setShowAxes(boolean showAxes);
  abstract public void setShowBbcage(boolean showBbcage);
  abstract public void setJmolDefaults();
  abstract public void setRasmolDefaults();

  abstract public void setBooleanProperty(String propertyName, boolean value);
  abstract public void setIntProperty(String propertyName, int value);
  abstract public void setFloatProperty(String propertyName, float value);
  abstract public void setStringProperty(String propertyName, String value);

  abstract public void setModeMouse(int modeMouse); //only MOUSEMODE_NONE, prior to nulling viewer

  abstract public void setShowHydrogens(boolean showHydrogens);
  abstract public void setShowMeasurements(boolean showMeasurements);
  abstract public void setPerspectiveDepth(boolean perspectiveDepth);
  abstract public void setAutoBond(boolean autoBond);
  abstract public void setMarBond(short marBond);
  abstract public void setBondTolerance(float bondTolerance);
  abstract public void setMinBondDistance(float minBondDistance);
  abstract public void setAxesOrientationRasmol(boolean axesMessedUp);
  abstract public void setPercentVdwAtom(int percentVdwAtom);
  
  //for each of these the script equivalent is shown  
  abstract public void setAnimationFps(int framesPerSecond);
  //viewer.script("animation fps x.x")
  abstract public void setFrankOn(boolean frankOn);
  //viewer.script("frank on")
  abstract public void setDebugScript(boolean debugScript);
  //viewer.script("set logLevel 5/4")
  //viewer.script("set debugScript on/off")
  abstract public void deleteMeasurement(int i);
  //viewer.script("measures delete " + (i + 1));
  abstract public void clearMeasurements();
  //viewer.script("measures delete");
  abstract public void setVectorScale(float vectorScaleValue);
  //viewer.script("vector scale " + vectorScaleValue);
  abstract public void setVibrationScale(float vibrationScaleValue);
  //viewer.script("vibration scale " + vibrationScaleValue);
  abstract public void setVibrationPeriod(float vibrationPeriod);
  //viewer.script("vibration " + vibrationPeriod);
  abstract public void selectAll();
  //viewer.script("select all");
  abstract public void clearSelection();
  //viewer.script("select none");
  //viewer.script("select ({2 3:6})");
  abstract public void setSelectionSet(BitSet newSelection);
  //viewer.script("selectionHalos ON"); //or OFF
  abstract public void setSelectionHalos(boolean haloEnabled);
  //viewer.script("center (selected)");
  abstract public void setCenterSelected(); 

  //not used in Jmol application:
  
  abstract public void rotateFront();
  abstract public void rotateToX(int degrees);
  abstract public void rotateToY(int degrees);
  abstract public void rotateToX(float radians);
  abstract public void rotateToY(float radians);
  abstract public void rotateToZ(float radians);


}

