  /* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;

import java.util.Hashtable;
import java.util.BitSet;
import java.util.Enumeration;

class StateManager {
  GlobalSettings globalSettings;
  Viewer viewer;
  Hashtable saved = new Hashtable();
  String lastOrientation = "";
  String lastConnections = "";
  String lastSelected = "";
  
  StateManager(Viewer viewer) {
    globalSettings = new GlobalSettings();
    this.viewer = viewer;
  }

  String listSavedStates() {
    String names = "";
    Enumeration e = saved.keys();
    while (e.hasMoreElements())
      names += "\n" + e.nextElement();
    return names;
  }
  
  void saveSelection(String saveName, BitSet bsSelected) {
    saveName = lastSelected = "Selected_" + saveName;
    BitSet bs = (BitSet)bsSelected.clone();
    saved.put(saveName, bs);
  }
  
  boolean restoreSelection(String saveName) {
    String name = (saveName.length() > 0 ? "Selected_" + saveName
        : lastSelected);
    BitSet bsSelected = (BitSet) saved.get(name);
    if (bsSelected == null) {
      viewer.select(new BitSet(), false);
      return false;
    }
    viewer.select(bsSelected, false);
    return true;
  }
   
  void saveOrientation(String saveName) {
    Orientation o = new Orientation();  
    o.saveName = lastOrientation = "Orientation_" + saveName;
    saved.put(o.saveName, o);
  }
  
  boolean restoreOrientation(String saveName, float timeSeconds) {
    String name = (saveName.length() > 0 ? "Orientation_" + saveName : lastOrientation);
    Orientation o = (Orientation) saved.get(name);
    if (o == null)
      return false;
    o.restore(timeSeconds);
//    Logger.info(listSavedStates());
    return true;
  }
   
  class Orientation {

    String saveName;

    Matrix3f rotationMatrix = new Matrix3f();
    float xTrans, yTrans;
    float zoom, rotationRadius;
    Point3f center = new Point3f();
    boolean windowCenteredFlag;
    
    Orientation () {
      viewer.getRotation(rotationMatrix);
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
      zoom = viewer.getZoomPercentFloat();
      center.set(viewer.getRotationCenter());
      windowCenteredFlag = viewer.isWindowCentered();
      rotationRadius = viewer.getRotationRadius();
    }
        
    void restore(float timeSeconds) {
      viewer.setWindowCentered(windowCenteredFlag);
      viewer.moveTo(timeSeconds, rotationMatrix, center, zoom, xTrans, yTrans, rotationRadius);
    }
  }

  void saveBonds(String saveName) {
    Connections b = new Connections();  
    b.saveName = lastConnections = "Bonds_" + saveName;
    saved.put(b.saveName, b);
  }
  
  boolean restoreBonds(String saveName) {
    String name = (saveName.length() > 0 ? "Bonds_" + saveName : lastConnections);
    Connections c = (Connections) saved.get(name);
    if (c == null)
      return false;
    c.restore();
//    Logger.info(listSavedStates());
    return true;
  }
   
  class Connections {

    String saveName;
    int bondCount;
    Connection[] connections;
    
    Connections() {
      Frame frame = viewer.getFrame();
      if (frame == null)
        return;
      bondCount = frame.bondCount;
      connections = new Connection[bondCount + 1];
      Bond[] bonds = frame.bonds;
      for (int i = bondCount; --i >= 0;) {
        Bond b = bonds[i];
        connections[i] = new Connection(b.atom1.atomIndex, b.atom2.atomIndex,
            b.mad, b.colix, b.order, b.shapeVisibilityFlags);
      }
    }
    
    class Connection {
      int atomIndex1;
      int atomIndex2;
      short mad;
      short colix;
      short order;
      int shapeVisibilityFlags;
      
      Connection(int atom1, int atom2, short mad, short colix, short order,
          int shapeVisibilityFlags) {
        atomIndex1 = atom1;
        atomIndex2 = atom2;
        this.mad = mad;
        this.colix = colix;
        this.order = order;
        this.shapeVisibilityFlags = shapeVisibilityFlags;
      }
    }

    void restore() {
      Frame frame = viewer.getFrame();
      if (frame == null)
        return;
      frame.deleteAllBonds();
      for (int i = bondCount; --i >= 0;) {
        Connection c = connections[i];
        if (c.atomIndex1 >= frame.atomCount || c.atomIndex2 >= frame.atomCount)
          continue;        
        Bond b = frame.bondAtoms(frame.atoms[c.atomIndex1], frame.atoms[c.atomIndex2], c.order, c.mad);
        b.colix = c.colix;
        b.shapeVisibilityFlags = c.shapeVisibilityFlags;
      }
    }
  }
  
  class GlobalSettings {

    /*
     *  Mostly these are just saved and restored directly from Viewer.
     *  They are collected here for reference and to ensure that no 
     *  methods are written that bypass viewer's get/set methods.
     *  
     *  Because these are not Frame variables, they should persist past
     *  a new file loading. There is some question in my mind whether all
     *  should be in this category.
     *  
     */

    GlobalSettings() {
      //
    }

    //file loading
    
    boolean forceAutoBond;
    
    char inlineNewlineChar = '|';
    String defaultLoadScript = "";
    Point3f ptDefaultLattice = new Point3f();
    
    void setDefaultLattice(Point3f ptLattice) {
      ptDefaultLattice.set(ptLattice);
    }
    int[] getDefaultLattice() {
      int[] A = new int[4];
      A[1] = (int) ptDefaultLattice.x;
      A[2] = (int) ptDefaultLattice.y;
      A[3] = (int) ptDefaultLattice.z;
      return A;
    }
    
    //centering and perspective
    
    boolean allowCameraMoveFlag = true;
    boolean adjustCameraFlag    = true;

    //solvent
   
    boolean solventOn = false;
    float solventProbeRadius = 1.2f;
    
    //measurements
    
    boolean measureAllModels;

    //rendering
    
    boolean enableFullSceneAntialiasing = false;
    boolean greyscaleRendering          = false;
    boolean zoomLarge                   = true; //false would be like Chime
    boolean labelsGroupFlag             = false;
    boolean labelsFrontFlag             = false;
    boolean labelPointerBackground      = true;
    boolean labelPointerBox             = true;
    boolean labelPointerNoBox           = false;
    boolean dotsSelectedOnlyFlag        = false;
    boolean dotSurfaceFlag              = true;
    boolean displayCellParameters       = true;
    boolean showHiddenSelectionHalos    = false;
    boolean showMeasurements            = true;
    boolean frankOn                     = false;

    int axesMode = JmolConstants.AXES_MODE_BOUNDBOX;

    //atoms and bonds
    
    boolean bondSelectionModeOr  = false;
    boolean showMultipleBonds    = true;
    boolean showHydrogens        = true;
    boolean ssbondsBackbone      = false;
    boolean hbondsBackbone       = false;
    boolean hbondsSolid          = false;

    byte modeMultipleBond = JmolConstants.MULTIBOND_NOTSMALL;

    //secondary structure + Rasmol
    
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting   = true;
    boolean cartoonRocketFlag     = false;
    boolean ribbonBorder          = false;
    boolean chainCaseSensitive    = false;
    boolean rangeSelected         = false;
    boolean traceAlpha            = false;
    boolean highResolutionFlag    = false;

    //misc
    
    int ribbonAspectRatio   = 16;
    int hermiteLevel = 4;
    int pickingSpinRate      = 10;
    boolean hideNameInPopup  = false;
    boolean disablePopupMenu = false;
    String helpPath          = null;

    
    //testing
    
    boolean debugScript = false;
    boolean testFlag1   = false;
    boolean testFlag2   = false;
    boolean testFlag3   = false;
    boolean testFlag4   = false;
  }
  
  
}
