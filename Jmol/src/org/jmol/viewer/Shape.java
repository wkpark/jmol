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

abstract class Shape {

  Viewer viewer;
  Frame frame;
  Graphics3D g3d;
  int shapeID;
  int myVisibilityFlag;
  BitSet bsSizeSet;
  BitSet bsColixSet;
  
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
    Logger.warn("unassigned property:" + propertyName + ":" + value);
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
    BitSet bs;
    if (ht.containsKey(key)) {
      bs = (BitSet) ht.get(key);
    } else {
      bs = new BitSet();
      ht.put(key, bs);
    }
    for (int i = i1; i<= i2; i++)
      bs.set(i);
  }

  static String getShapeCommands(Hashtable htDefine, Hashtable htMore, int atomCount) {
    return StateManager.getCommands(htDefine, htMore, atomCount);
  }
 }
