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

import org.jmol.util.BitSet;
import org.jmol.util.Colix;
import org.jmol.util.JmolFont;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.Point3f;
import org.jmol.util.Point3i;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumPalette;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;


import java.util.List;
import java.util.Map;

/**
 * Shape now encompasses:
 * 
 * AtomShape (abstract)
 *     |
 *   Balls, Dots, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors
 *           |
 *         GeoSurface  
 * 
 * BioShapeCollection (abstract)
 *     |
 *   Backbone, Cartoon, MeshRibbon, Ribbons, Rockets, Strands, Trace
 * 
 * Dipoles
 * 
 * FontLineShape (abstract)
 *     |
 *   Axes, Bbcage, Frank, Uccage
 * 
 * Measures
 * 
 * MeshCollection (abstract)
 *   |       |
 * Draw   Isosurface
 *           |
 *        LcaoCartoon, MolecularOrbital, Pmesh
 * 
 * Sticks
 *     |
 *    Hsticks, Sssticks
 * 
 * TextShape (abstract)
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
  
  public static final float RADIUS_MAX = 4;
  public Viewer viewer; //public for now for Backbone
  public ModelSet modelSet;
  public GData gdata;
  public int shapeID;
  public int myVisibilityFlag;
  protected float translucentLevel;
  protected boolean translucentAllowed = true;
  public boolean isBioShape;
  
  public Viewer getViewer() {
    return viewer;
  }
  
  final public void initializeShape(Viewer viewer, GData g3d, ModelSet modelSet,
                               int shapeID) {
    this.viewer = viewer;
    this.gdata = g3d;
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

  /**
   * 
   * @param shape
   */
  public void merge(Shape shape) {
    // shape-dependent Jmol 12.0.RC6
  }
  
  public void setShapeSizeRD(int size, RadiusData rd, BitSet bsSelected) {
    if (rd == null)
      setSize(size, bsSelected);
    else
      setSizeRD(rd, bsSelected);
  }

  /**
   * 
   * @param size
   * @param bsSelected
   */
  protected void setSize(int size, BitSet bsSelected) {
    // not for atoms except to turn off -- size = 0
  }

  /**
   * 
   * @param rd
   * @param bsSelected
   */
  protected void setSizeRD(RadiusData rd, BitSet bsSelected) {
    // balls, dots, other atomshapes
  }

  /**
   * specifically from modelSet.setShapeProperty, mostly from ScriptEvaluation,
   * but not always  -- definitely not from "super.setProperty"
   * 
   * @param propertyName
   * @param value
   * @param bsSelected
   */
  public void setShapeProperty(String propertyName, Object value,
                               BitSet bsSelected) {
      setProperty(propertyName, value, bsSelected == null ? 
          viewer.getSelectionSet(false) : bsSelected);
  }

  /**
   * may come from any source -- executed AFTER a shape's own setProperty method
   * 
   * @param propertyName
   * @param value
   * @param bsSelected
   */
  @SuppressWarnings("unchecked")
  public void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if (propertyName == "setProperties") {
      if (bsSelected == null)
        bsSelected = viewer.getSelectionSet(false);
      List<Object[]> propertyList = (List<Object[]>) value;
      while (propertyList.size() > 0) {
        Object[] data = propertyList.remove(0);
        setShapeProperty(((String) data[0]).intern(), data[1], bsSelected);
      }
      return;
    }
    if (propertyName == "translucentLevel") {
      translucentLevel = ((Float) value).floatValue();
      return;
    }

    if (propertyName == "refreshTrajectories") {
      return;
    }

    Logger.warn("unassigned " + JmolConstants.shapeClassBases[shapeID] + " + shape setProperty:" + propertyName + ":" + value);
  }

  /**
   * 
   * @param property
   * @param data
   * @return true if serviced
   */
  public boolean getPropertyData(String property, Object[] data) {
    return false;
  }

  /**
   * 
   * @param property
   * @param index
   * @return true if serviced
   */
  public Object getProperty(String property, int index) {
    return null;
  }

  /**
   * 
   * @param thisID
   * @return index
   */
  public int getIndexFromName(String thisID) {
    return -1;
  }

  /**
   * 
   * @param x
   * @param y
   * @return T/F
   */
  public boolean wasClicked(int x, int y) {
    return false;
  }

  /**
   * 
   * @param xMouse
   * @param yMouse
   * @param closest
   * @param bsNot
   */
  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest, BitSet bsNot) {
  }

  /**
   * 
   * @param pointMin
   * @param pointMax
   */
  public void checkBoundsMinMax(Point3f pointMin, Point3f pointMax) {
  }

  public void setModelClickability() {
  }

  /**
   * 
   * @param x
   * @param y
   * @param modifiers
   * @param bsVisible
   * @return Hashtable containing information about pt clicked
   */
  public Map<String, Object> checkObjectClicked(int x, int y, int modifiers, BitSet bsVisible) {
    return null;
  }

  /**
   * 
   * @param x
   * @param y
   * @param bsVisible
   * @return T/F
   */
  public boolean checkObjectHovered(int x, int y, BitSet bsVisible) {
    return false;
  }

  /**
   * 
   * @param prevX
   * @param prevY
   * @param x
   * @param y
   * @param modifiers
   * @param bsVisible
   * @return T/F
   */
  public boolean checkObjectDragged(int prevX, int prevY, int x, int y,
                             int modifiers, BitSet bsVisible) {
    return false;
  }

  protected int coordinateInRange(int x, int y, Point3f vertex, int dmin2, Point3i ptXY) {
    viewer.transformPtScr(vertex, ptXY);
    int d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
  
  public short getColixI(short colix, byte paletteID, int atomIndex) {
    return getColixA(colix, paletteID, modelSet.atoms[atomIndex]);
  }

  protected short getColixA(short colix, byte paletteID, Atom atom) {
    return (colix == Colix.USE_PALETTE ? viewer.getColixAtomPalette(atom,
        paletteID) : colix);
  }

  protected short getColixB(short colix, int pid, Bond bond) {
    return (colix == Colix.USE_PALETTE ? viewer.getColixBondPalette(bond,
        pid) : colix);
  }

  public List<Map<String, Object>> getShapeDetail() {
    return null;
  }

  public String getShapeState() {
    return null;
  }

  /**
   * 
   * @param bs
   */
  public void setVisibilityFlags(BitSet bs) {
  }

  static public void setStateInfo(Map<String, BitSet> ht,
                                  int i, String key) {
    setStateInfo(ht, i, i, key);
  }

  static public void setStateInfo(Map<String, BitSet> ht,
                                  int i1, int i2, String key) {
    StateManager.setStateInfo(ht, i1, i2, key);
  }

  static public String getShapeCommands(Map<String, BitSet> htDefine,
                                        Map<String, BitSet> htMore) {
    return StateManager.getCommands(htDefine, htMore, "select");
  }

  static public String getShapeCommandsSel(Map<String, BitSet> htDefine,
                                        Map<String, BitSet> htMore,
                                        String selectCmd) {
    return StateManager.getCommands(htDefine, htMore, selectCmd);
  }

  static public void appendCmd(StringXBuilder s, String cmd) {
    StateManager.appendCmd(s, cmd);
  }

  static public String getFontCommand(String type, JmolFont font) {
    if (font == null)
      return "";
    return "font " + type + " " + font.fontSizeNominal + " " + font.fontFace + " "
        + font.fontStyle;
  }

  public String getColorCommandUnk(String type, short colix) {
    return getColorCommand(type, EnumPalette.UNKNOWN.id, colix);
  }

  public String getColorCommand(String type, byte pid, short colix) {
    if (pid == EnumPalette.UNKNOWN.id && colix == Colix.INHERIT_ALL)
      return "";
    return "color " + type + " " + encodeTransColor(pid, colix, translucentAllowed);
  }

  private static String encodeTransColor(byte pid, short colix,
                                  boolean translucentAllowed) {
    if (pid == EnumPalette.UNKNOWN.id && colix == Colix.INHERIT_ALL)
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
        + (pid != EnumPalette.UNKNOWN.id 
        && !EnumPalette.isPaletteVariable(pid) 
        ? EnumPalette.getPaletteName(pid) : encodeColor(colix));
  }

  protected static String encodeColor(short colix) {
    // used also by labels for background state (no translucent issues there?)
    return (Colix.isColixColorInherited(colix) ? "none" : Colix
        .getHexCode(colix));
  }

  protected static String getTranslucentLabel(short colix) {
    return (Colix.isColixTranslucent(colix) ? "translucent "
        + Colix.getColixTranslucencyFractional(colix): "opaque");
  }

  public static short getColix(short[] colixes, int i, Atom atom) {
    return Colix.getColixInherited(
        (colixes == null || i >= colixes.length ? Colix.INHERIT_ALL
            : colixes[i]), atom.getColix());
  }

  /**
   * 
   * @param atomIndex
   * @return size
   */
  public int getSize(int atomIndex) {
    return 0;
  }

  /**
   * 
   * @param group
   * @return size
   */
  public int getSize(Group group) {
    return 0;
  }

}
