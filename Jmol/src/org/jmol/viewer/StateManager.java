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
import java.util.Enumeration;

class StateManager {
  GlobalSettings globalSettings;
  Viewer viewer;
  Hashtable saved = new Hashtable();
  String lastOrientation = "";
  String lastConnections = "";
  
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
    int xTrans, yTrans, zoom;
    Point3f center = new Point3f();
    boolean windowCenteredFlag;
    
    Orientation () {
      viewer.getRotation(rotationMatrix);
      xTrans = (int) viewer.getTranslationXPercent();
      yTrans = (int) viewer.getTranslationYPercent();
      zoom = viewer.getZoomPercent();
      center.set(viewer.getCenter());
      windowCenteredFlag = viewer.isWindowCentered();
    }
    
    void setRotationMatrix(Matrix3f mat) {
      rotationMatrix.set(mat);
    }
    
    void setTranslation(int x, int y) {
      xTrans = x;
      yTrans = y;
    }
    
    void setZoom(int zoom) {
      this.zoom = zoom;
    }
    
    void setCenter(Point3f pt) {
      center.set(pt);
    }
    
    void setWindowCentered(boolean TF) {
      windowCenteredFlag = TF;
    }
    
    void restore(float timeSeconds) {
      viewer.moveTo(timeSeconds, rotationMatrix, center, zoom, xTrans, yTrans);
      viewer.translateToXPercent(xTrans);
      viewer.translateToYPercent(yTrans);
      viewer.setWindowCentered(windowCenteredFlag);
      //viewer.setRotationCenterNoScale(center);
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
        Bond b = frame.bondAtoms(frame.atoms[c.atomIndex1], frame.atoms[c.atomIndex2], c.order);
        b.mad = c.mad;
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
    boolean adjustCameraFlag = true;

    //solvent
   
    boolean solventOn;
    float solventProbeRadius = 1.2f;
    
    //measurements
    
    boolean measureAllModels;

    //rendering
    
    boolean enableFullSceneAntialiasing;
    boolean greyscaleRendering;
    boolean labelsGroupFlag;
    boolean labelsFrontFlag;
    boolean dotsSelectedOnlyFlag;
    boolean dotSurfaceFlag = true;
    boolean displayCellParameters = true;
    int axesMode = JmolConstants.AXES_MODE_BOUNDBOX;

    //atoms and bonds
    
    boolean bondSelectionModeOr;

    //secondary structure + Rasmol
    
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting = true;
    boolean cartoonRocketFlag;
    boolean ribbonBorder;
    boolean chainCaseSensitive;
    boolean rangeSelected;

    //misc
    
    int pickingSpinRate = 10;
    boolean hideNameInPopup;
    boolean disablePopupMenu;
    String helpPath;
    
    //testing
    
    boolean debugScript;
    boolean testFlag1;
    boolean testFlag2;
    boolean testFlag3;
    boolean testFlag4;
  }
}
