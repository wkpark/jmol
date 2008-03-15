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

package org.jmol.shape;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;
import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;

import javax.vecmath.Point3f;
import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;

/**
 * After code reorganization of 11/2006 (BH) Shape now encompasses:
 * 
 * AtomShape
 *     |
 *   Balls, Dots, Halos, Labels, Polyhedra, Stars, Vectors
 *           |
 *         GeoSurface  
 * 
 * Dipoles, Measures
 * 
 * FontLineShape
 *     |
 *   Axes, Bbcage, Frank, Uccage
 * 
 * MeshCollection
 *   |       |
 * Draw    MeshFileCollection  (adds parser code)
 *             |        |
 *            Pmesh   IsosurfaceMeshCollection (adds JVXL code)
 *                      |
 *                  Isosurface
 *                      |________ LcaoCartoon, MolecularOrbital
 * 
 * BioShapeCollection
 *   |
 *   Backbone, Cartoon, MeshRibbon, Ribbons, Rockets, Strands, Trace
 * 
 * Sticks
 *     |
 *    Hsticks, Sssticks
 * 
 * TextShape
 *     |
 *    Echo, Hover
 *    
 */
public abstract class Shape {

  //public Shape () {
  //  System.out.println("Shape " + this + " constructed");
  //}
  
  //public void finalize() {
  //  System.out.println("Shape " + shapeID + " " + this + " finalized");
  //}
  
  public Viewer viewer; //public for now for Backbone
  public ModelSet modelSet;
  protected Graphics3D g3d;
  public int shapeID;
  public int myVisibilityFlag;
  protected float translucentLevel;
  protected boolean translucentAllowed = true;
  public boolean isBioShape;
  
  public Viewer getViewer() {
    return viewer;
  }
  
  final public void initializeShape(Viewer viewer, Graphics3D g3d, ModelSet modelSet,
                               int shapeID) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.shapeID = shapeID;
    this.myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(shapeID);
    setModelSet(modelSet);
    initShape();
    //System.out.println("Shape " + shapeID + " " + this + " initialized");

  }

  public void setModelSet(ModelSet modelSet) {
    this.modelSet = modelSet;
    initModelSet();
  }
  
  protected void initModelSet() {
  }

  public void initShape() {
  }

  public void setSize(int size, BitSet bsSelected) {
  }

  public void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if (propertyName == "translucentLevel") {
      translucentLevel = ((Float) value).floatValue();
      return;
    }

    if (propertyName == "refreshTrajectories") {
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      //Object[] {newModels, atoms, new int[] {iModelDeleted, firstAtomIndex, nAtoms}
      //return;
    }
    
    Logger.warn("unassigned " + JmolConstants.shapeClassBases[shapeID] + " + shape setProperty:" + propertyName + ":" + value);
  }

  public Object getProperty(String property, int index) {
    return null;
  }

  public int getIndexFromName(String thisID) {
    return -1;
  }

  public boolean wasClicked(int x, int y) {
    return false;
  }

  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest) {
  }

  public void checkBoundsMinMax(Point3f pointMin, Point3f pointMax) {
  }

  public void setModelClickability() {
  }

  public boolean checkObjectClicked(int x, int y, int modifiers) {
    return false;
  }

  public boolean checkObjectHovered(int x, int y) {
    return false;
  }

  public boolean checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                             int modifiers) {
    return false;
  }

  public short setColix(short colix, byte paletteID, int atomIndex) {
    return setColix(colix, paletteID, modelSet.getAtomAt(atomIndex));
  }

  protected short setColix(short colix, byte paletteID, Atom atom) {
    return (colix == Graphics3D.USE_PALETTE ? viewer.getColixAtomPalette(atom,
        paletteID) : colix);
  }

  protected void remapColors() {
    
  }
  
  public Vector getShapeDetail() {
    return null;
  }

  public String getShapeState() {
    return null;
  }

  public void setVisibilityFlags(BitSet bs) {
  }

  static public void setStateInfo(Hashtable ht, int i, String key) {
    setStateInfo(ht, i, i, key);
  }

  static public void setStateInfo(Hashtable ht, int i1, int i2, String key) {
    StateManager.setStateInfo(ht, i1, i2, key);
  }

  static public String getShapeCommands(Hashtable htDefine, Hashtable htMore,
                                 int atomCount) {
    return StateManager.getCommands(htDefine, htMore, atomCount);
  }

  static public String getShapeCommands(Hashtable htDefine, Hashtable htMore,
                                 int count, String selectCmd) {
    return StateManager.getCommands(htDefine, htMore, count, selectCmd);
  }

  static public void appendCmd(StringBuffer s, String cmd) {
    StateManager.appendCmd(s, cmd);
  }

  static public String getFontCommand(String type, Font3D font) {
    if (font == null)
      return "";
    return "font " + type + " " + font.fontSizeNominal + " " + font.fontFace + " "
        + font.fontStyle;
  }

  public String getColorCommand(String type, short colix) {
    return getColorCommand(type, JmolConstants.PALETTE_UNKNOWN, colix);
  }

  public String getColorCommand(String type, byte pid, short colix) {
    if (pid == JmolConstants.PALETTE_UNKNOWN && colix == Graphics3D.INHERIT_ALL)
      return "";
    return "color " + type + " " + encodeTransColor(pid, colix, translucentAllowed);
  }

  private String encodeTransColor(byte pid, short colix,
                                  boolean translucentAllowed) {
    if (pid == JmolConstants.PALETTE_UNKNOWN && colix == Graphics3D.INHERIT_ALL)
      return "";
    /* nuance here is that some palettes depend upon a
     * point-in-time calculation that takes into account
     * some aspect of the current state, such as what groups
     * are selected in the case of "color group". So we have
     * to identify these and NOT use them in serialization.
     * Serialization of the palette name is just a convenience
     * anyway. 
     */
    return (translucentAllowed ? getTranslucentLabel(colix) + " " : "")
        + (pid != JmolConstants.PALETTE_UNKNOWN 
        && !JmolConstants.isPaletteVariable(pid) 
        ? JmolConstants.getPaletteName(pid) : encodeColor(colix));
  }

  String encodeColor(short colix) {
    // used also by labels for background state (no translucent issues there?)
    return (Graphics3D.isColixColorInherited(colix) ? "none" : Escape
        .escapeColor(g3d.getColixArgb(colix)));
  }

  private static String getTranslucentLabel(short colix) {
    return (Graphics3D.isColixTranslucent(colix) ? "translucent "
        + Graphics3D.getColixTranslucencyLevel(colix): "opaque");
  }

  public static short getColix(short[] colixes, int i, Atom atom) {
    return Graphics3D.getColixInherited(
        (colixes == null || i >= colixes.length ? Graphics3D.INHERIT_ALL
            : colixes[i]), atom.getColix());
  }

}
