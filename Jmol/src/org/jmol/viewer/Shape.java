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

package org.jmol.viewer;

import org.jmol.util.Logger;
import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;

/**
 * After code reorganization of 11/2006 (BH) Shape now encompasses:
 * 
 * AtomShape
 *   Balls, Dots, Halos, Labels, Polyhedra, Stars, Vectors
 * 
 * Dipoles, Measures
 * 
 * FontLineShape
 *   Axes, Bbcage, Frank, Uccage
 * 
 * MeshCollection
 *    Draw, Isosurface, Pmesh
 *               |______________ LcaoCartoon, MolecularOrbital
 * 
 * Mps
 *   Backbone, Cartoon, MeshRibbon, Ribbons, Rockets, Strands, Trace
 * 
 * Sticks
 *    Hsticks, Sssticks
 * 
 * TextShape
 *    Echo, Hover
 *    
 */
abstract class Shape {
  
  Viewer viewer;
  Frame frame;
  Graphics3D g3d;
  int shapeID;
  int myVisibilityFlag;

  final void setViewerG3dFrame(Viewer viewer, Graphics3D g3d, Frame frame, int shapeID) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.frame = frame;
    this.shapeID = shapeID;
    this.myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(shapeID);
    initShape();
  }

  void initShape() {
  }

  void setSize(int size, BitSet bsSelected) {
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    Logger.warn("unassigned shape setProperty:" + propertyName + ":" + value);
  }

  Object getProperty(String property, int index) {
    return null;
  }

  boolean wasClicked(int x, int y) {
    return false;
  }

  void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
  }

  void checkBoundsMinMax(Point3f pointMin, Point3f pointMax) {
  }

  void setModelClickability() {
  }

  void checkObjectClicked(int x, int y, int modifiers) {
  }

  void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY, int modifiers) {
  }

  short setColix(short colix, int paletteID, int atomIndex) {
    return setColix(colix, paletteID, frame.getAtomAt(atomIndex));
  }
  
  short setColix(short colix, int paletteID, Atom atom) {
    return (colix == Graphics3D.USE_PALETTE ? viewer.getColixAtomPalette(atom,
        paletteID) : colix);
  }

  Vector getShapeDetail() {
    return null;
  }
  
  String getShapeState() {
    return null;
  }
  
  void setVisibilityFlags(BitSet bs) {
  }
  
  static void setStateInfo(Hashtable ht, int i, String key) {
    setStateInfo(ht, i, i, key);
  }

  static void setStateInfo(Hashtable ht, int i1, int i2, String key) {
    StateManager.setStateInfo(ht, i1, i2, key);
  }

  static String getShapeCommands(Hashtable htDefine, Hashtable htMore, int atomCount) {
    return StateManager.getCommands(htDefine, htMore, atomCount);
  }

  static String getShapeCommands(Hashtable htDefine, Hashtable htMore, int count, String selectCmd) {
    return StateManager.getCommands(htDefine, htMore, count, selectCmd);
  }
  
  static void appendCmd(StringBuffer s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append(cmd + ";\n");
  }
  
  static String getFontCommand(String type, Font3D font) {
    if (font == null)
      return "";
    return "font " + type + " " + font.fontSize + " "
        + font.fontFace + " " + font.fontStyle;
  }
  
  String getColorCommand(String type, short colix) {
    return getColorCommand(type, (short)-1, colix);
  }

  String getColorCommand(String type, short pid, short colix) {
    if (pid < 0 && colix == 0)
      return "";
    return "color " + type + " " + encodeTransColor(pid, colix);
  }

  String encodeTransColor(short colix) {
    return encodeTransColor((short)-1, colix);
  }

  String encodeTransColor(short pid, short colix) {
    if (pid < 0 && colix == 0)
      return "";
    String s = "";
    if (pid >= 0) {
      if (Graphics3D.isColixTranslucent(colix))
        s += "translucent ";
      s += JmolConstants.getPaletteName(pid);
    } else {
      s += encodeColor(colix);
    }
    return s;
  }
  
  String encodeColor(short colix) {
    String color = g3d.getHexColorFromIndex(colix);
    return (Graphics3D.isColixTranslucent(colix) ? "translucent": "")
    + (color == null ? " none" : " [x" + color + "]"); 
  }
  
}
