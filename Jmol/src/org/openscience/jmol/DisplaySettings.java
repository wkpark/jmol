
/*
 * Copyright 2002 The Jmol Development Team
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

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Stores the display style of graphical elements.
 *
 * @author Bradley A. Smith (bradley@baysmith.com).
 */
public class DisplaySettings implements Cloneable {

  /**
   * Display no labels.
   */
  public static final int NOLABELS = 0;

  /**
   * Display atomic symbols.
   */
  public static final int SYMBOLS = 1;

  /**
   * Display atom types.
   */
  public static final int TYPES = 2;

  /**
   * Display atom numbers.
   */
  public static final int NUMBERS = 3;

  /**
   * Draw atoms as filled circles and bonds as filled rectangles.
   */
  public static final int QUICKDRAW = 0;

  /**
   * Draw atoms as lighted spheres and bonds as lighted cylinders.
   */
  public static final int SHADING = 1;

  /**
   * Draw atoms as transparent circles and bonds as transparent rectangles.
   */
  public static final int WIREFRAME = 2;

  /**
   * Draw bonds as lines; invalid for atoms.
   */
  public static final int LINE = 3;

  /**
   * Color atoms by type; invalid for bonds.
   */
  public static final int ATOMTYPE = 0;

  /**
   * Color atoms by charge; invalid for bonds.
   */
  public static final int ATOMCHARGE = 1;

  /**
   * Whether to draw anti-aliased lines.
   */
  private boolean antiAliased = false;

  /**
   * Returns whether these settings are equal to another.
   *
   * @param obj the other settings.
   * @return true if the settings are equal.
   */
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DisplaySettings)) {
      return false;
    }
    DisplaySettings otherSettings = (DisplaySettings) obj;
    return labelMode == otherSettings.labelMode
        && atomDrawMode == otherSettings.atomDrawMode
        && atomColorProfile == otherSettings.atomColorProfile
        && bondDrawMode == otherSettings.bondDrawMode
        && bondWidth == otherSettings.bondWidth
        && bondScreenScale == otherSettings.bondScreenScale
        && (outlineColor == otherSettings.outlineColor ||
            (outlineColor != null && outlineColor.equals(otherSettings.outlineColor)))
        && (propertyMode == otherSettings.propertyMode ||
            (propertyMode != null && propertyMode.equals(otherSettings.propertyMode)))
        && (pickedColor == otherSettings.pickedColor ||
            (pickedColor != null && pickedColor.equals(otherSettings.pickedColor)))
        && (textColor == otherSettings.textColor ||
            (textColor != null && textColor.equals(otherSettings.textColor)))
        && (distanceColor == otherSettings.distanceColor ||
            (distanceColor != null && propertyMode.equals(otherSettings.distanceColor)))
        && (angleColor == otherSettings.angleColor ||
            (angleColor != null && propertyMode.equals(otherSettings.angleColor)))
        && (dihedralColor == otherSettings.dihedralColor ||
            (dihedralColor != null && propertyMode.equals(otherSettings.dihedralColor)))
        && showAtoms == otherSettings.showAtoms
        && showBonds == otherSettings.showBonds
        && showVectors == otherSettings.showVectors
        && showHydrogens == otherSettings.showHydrogens
        && showDarkerOutline == otherSettings.showDarkerOutline
        && vectorScreenScale == otherSettings.vectorScreenScale
        && atomScreenScale == otherSettings.atomScreenScale
        && atomZOffset == otherSettings.atomZOffset
        && atomDepthFactor == otherSettings.atomDepthFactor
        && atomSphereFactor == otherSettings.atomSphereFactor
        && doFastRendering == otherSettings.doFastRendering
        && antiAliased == otherSettings.antiAliased;
  }
  
  /**
   * Returns the hash code for this object.
   *
   * @return the hash code for this object.
   */
  public int hashCode() {
    int result = 17;
    result = 37*result + labelMode;
    result = 37*result + atomDrawMode;
    result = 37*result + atomColorProfile;
    result = 37*result + bondDrawMode;
    result = 37*result + Float.floatToIntBits(bondWidth);
    result = 37*result + Float.floatToIntBits(bondScreenScale);
    result = 37*result + (outlineColor == null ? 0 : outlineColor.hashCode());
    result = 37*result + (propertyMode == null ? 0 : propertyMode.hashCode());
    result = 37*result + (pickedColor == null ? 0 : pickedColor.hashCode());
    result = 37*result + (textColor == null ? 0 : textColor.hashCode());
    result = 37*result + (distanceColor == null ? 0 : distanceColor.hashCode());
    result = 37*result + (angleColor == null ? 0 : angleColor.hashCode());
    result = 37*result + (dihedralColor == null ? 0 : dihedralColor.hashCode());
    result = 37*result + (showAtoms ? 0 : 1);
    result = 37*result + (showBonds ? 0 : 1);
    result = 37*result + (showVectors ? 0 : 1);
    result = 37*result + (showHydrogens ? 0 : 1);
    result = 37*result + (showDarkerOutline ? 0 : 1);
    result = 37*result + Float.floatToIntBits(vectorScreenScale);
    result = 37*result + Float.floatToIntBits(atomScreenScale);
    result = 37*result + atomZOffset;
    result = 37*result + Float.floatToIntBits(atomDepthFactor);
    long longValue = Double.doubleToLongBits(atomSphereFactor);
    result = 37*result + (int)(longValue ^ (longValue >>>32));
    result = 37*result + (doFastRendering ? 0 : 1);
    result = 37*result + (antiAliased ? 0 : 1);
    return result;
  }

  /**
   * Sets the display style for labels.
   */
  public void setLabelMode(int i) {
    if (i > NUMBERS) {
      return;
    }
    labelMode = i;
  }

  /**
   * Gets the display style for labels.
   */
  public int getLabelMode() {
    return labelMode;
  }

  /**
   * Sets the display style for drawing atoms.
   */
  public void setAtomDrawMode(int i) {
    if (i >= LINE) {
      return;
    }
    atomDrawMode = i;
  }

  /**
   * Gets the display style for drawing atoms.
   */
  public int getAtomDrawMode() {
    return atomDrawMode;
  }

  /**
   * Sets the display style for drawing atoms.
   */
  public void setAtomColorProfile(int i) {
    if (i > ATOMCHARGE) {
      return;
    }
    atomColorProfile = i;
  }

  /**
   * Gets the display style for drawing atoms.
   */
  public int getAtomColorProfile() {
    return atomColorProfile;
  }

  /**
   * Sets the display style for drawing bonds.
   */
  public void setBondDrawMode(int i) {
    if (i > LINE) {
      return;
    }
    bondDrawMode = i;
  }

  /**
   * Gets the display style for drawing bonds.
   */
  public int getBondDrawMode() {
    return bondDrawMode;
  }

  public float getBondWidth() {
    return bondWidth;
  }

  public void setBondWidth(float width) {
    bondWidth = width;
  }

  public float getBondScreenScale() {
    return bondScreenScale;
  }

  public void setBondScreenScale(float scale) {
    bondScreenScale = scale;
  }

  /**
   * Sets the color used for outlining atoms and bonds in QUICKDRAW
   * and WIREFRAME drawing modes.
   */
  public void setOutlineColor(Color c) {
    outlineColor = c;
  }

  /**
   * Gets the color used for outlining atoms and bonds in QUICKDRAW
   * and WIREFRAME drawing modes.
                                   */
  public Color getOutlineColor() {
    return outlineColor;
  }

  /**
   * Sets the color used for highlighting selected atoms.
   */
  public void setPickedColor(Color c) {
    pickedColor = c;
  }

  /**
   * Gets the color used for highlighting selected atoms.
   */
  public Color getPickedColor() {
    return pickedColor;
  }

  /**
   * Sets the color used for drawing text.
   */
  public void setTextColor(Color c) {
    textColor = c;
  }

  /**
   * Gets the color used for drawing text.
   */
  public Color getTextColor() {
    return textColor;
  }

  /**
   * Sets physical property to be displayed.
   *
   * @param s The string descriptor of the physical property.
   */
  public void setPropertyMode(String s) {
    propertyMode = s;
  }

  /*
   * Returns the descriptor of the physical property currently displayed.
   */
  public String getPropertyMode() {
    return propertyMode;
  }

  /**
   * Sets the color of the line drawn for distance measurements.
   * @param c the color
   */
  public void setDistanceColor(Color c) {
    distanceColor = c;
  }

  /**
   * Gets the color of the line drawn for distance measurements.
   */
  public Color getDistanceColor() {
    return distanceColor;
  }

  /**
   * Sets the color of the line drawn for angle measurements.
   * @param c the color
   */
  public void setAngleColor(Color c) {
    angleColor = c;
  }

  /**
   * Gets the color of the line drawn for angle measurements.
   */
  public Color getAngleColor() {
    return angleColor;
  }

  /**
   * Sets the color of the line drawn for dihedral measurements.
   * @param c the color
   */
  public void setDihedralColor(Color c) {
    dihedralColor = c;
  }

  /**
   * Gets the color of the line drawn for dihedral measurements.
   */
  public Color getDihedralColor() {
    return dihedralColor;
  }

  /**
   * Display style for labels.
   */
  private int labelMode = NOLABELS;

  /**
   * Display style for drawing atoms.
   */
  private int atomDrawMode = QUICKDRAW;

  /**
   * Coloring profile for drawing atoms.
   */
  private int atomColorProfile = ATOMTYPE;

  /**
   * Display style for drawing bonds.
   */
  private int bondDrawMode = QUICKDRAW;

  private float bondWidth = 0.1f;

  private float bondScreenScale;

  /**
   * Color used for outlining atoms and bonds in QUICKDRAW and
   * WIREFRAME drawing modes.
   */
  private Color outlineColor = Color.black;

  /**
   * Descriptor of the physical property to be displayed.
   */
  private String propertyMode = "";

  /**
   * Color used for highlighting selected atoms.
   */
  private Color pickedColor = Color.orange;

  /**
   * Color used for drawing text.
   */
  private Color textColor = Color.black;

  /**
   * Color of the line drawn for distance measurements.
   */
  private Color distanceColor = Color.black;

  /**
   * Color of the line drawn for angle measurements.
   */
  private Color angleColor = Color.black;

  /**
   * Color of the line drawn for dihedral measurements.
   */
  private Color dihedralColor = Color.black;

  private boolean showAtoms = true;
  private boolean showBonds = true;
  private boolean showVectors = false;
  private boolean showHydrogens = true;
  private boolean showDarkerOutline = false;

  /**
   * Toggles on/off the flag that decides whether atoms are shown
   * when displaying a ChemFrame
   */
  public void toggleAtoms() {
    showAtoms = !showAtoms;
  }

  /**
   * Toggles on/off the flag that decides whether bonds are shown
   * when displaying a ChemFrame
   */
  public void toggleBonds() {
    showBonds = !showBonds;
  }

  /**
   * Toggles on/off the flag that decides whether vectors are shown
   * when displaying a ChemFrame
   */
  public void toggleVectors() {
    setShowVectors(!getShowVectors());
  }

  /**
   * Toggles on/off the flag that decides whether Hydrogen atoms are
   * shown when displaying a ChemFrame
   */
  public void toggleHydrogens() {
    showHydrogens = !showHydrogens;
  }

  /**
   * Set the flag that decides whether atoms are shown
   * when displaying a ChemFrame
   *
   * @param sa the value of the flag
   */
  public void setShowAtoms(boolean sa) {
    showAtoms = sa;
  }

  /**
   * Set the flag that decides whether bonds are shown
   * when displaying a ChemFrame
   *
   * @param sb the value of the flag
   */
  public void setShowBonds(boolean sb) {
    showBonds = sb;
  }

  /**
   * Set the flag that decides whether vectors are shown
   * when displaying a ChemFrame
   *
   * @param sv the value of the flag
   */
  public void setShowVectors(boolean sv) {
    showVectors = sv;
  }

  /**
   * Set the flag that decides whether Hydrogen atoms are shown
   * when displaying a ChemFrame.  Currently non-functional.
   *
   * @param sh the value of the flag
   */
  public void setShowHydrogens(boolean sh) {
    showHydrogens = sh;
  }
  public void setShowDarkerOutline(boolean showDarkerOutline) {
    this.showDarkerOutline = showDarkerOutline;
  }

  public boolean getShowAtoms() {
    return showAtoms;
  }

  public boolean getShowBonds() {
    return showBonds;
  }

  public boolean getShowVectors() {
    return showVectors;
  }

  public boolean getShowHydrogens() {
    return showHydrogens;
  }

  public boolean getShowDarkerOutline() {
    return showDarkerOutline;
  }

  /**
   * Returns the on-screen radius of an atom with the given radius.
   *
   * @param z z position in screen space
   */
  public float getCircleRadius(int z, double radius) {

    double raw = radius * atomSphereFactor;
    float depth = (float) (z - atomZOffset) / (2.0f * atomZOffset);
    float tmp = atomScreenScale * ((float) raw + atomDepthFactor * depth);
    if (tmp < 0.0f) {
      tmp = 1.0f;
    }
    return tmp;
  }

  /**
   * Returns the on-screen radius of something at location z which
   * has an approximate size of one angstrom;
   *
   * @param z z position in screen space
   */
  public float getScreenSize(int z) {

    double raw = atomSphereFactor;
    float depth = (float) (z - atomZOffset) / (2.0f * atomZOffset);
    float tmp = atomScreenScale * ((float) raw + atomDepthFactor * depth);
    if (tmp < 0.0f) {
      tmp = 1.0f;
    }
    return tmp;
  }

  /**
   * Gets the light source vector.
   */
  public float[] getLightSourceVector() {
    return lightSource;
  }

  /**
   * Place the light source for shaded atoms to the upper right of
   * the atoms and out of the plane.
   */
  private float[] lightSource = {
    -1.0f, -1.0f, 2.0f
  };

  /**
   * Gets flag for whether to draw bonds to atom centers.
   */
  public boolean getDrawBondsToAtomCenters() {
    return drawBondsToAtomCenters;
  }

  /**
   * Sets flag for whether to draw bonds to atom centers.
   */
  public void setDrawBondsToAtomCenters(boolean on) {
    drawBondsToAtomCenters = on;
  }

  /**
   * Toggles flag for whether to draw bonds to atom centers.
   */
  public void toggleDrawBondsToAtomCenters() {
    drawBondsToAtomCenters = !drawBondsToAtomCenters;
  }

  /**
   * Flag for whether to draw bonds to the center of atoms.
   */
  private boolean drawBondsToAtomCenters = false;

  /**
   * Gets the vector screen scale.
   */
  public float getVectorScreenScale() {
    return vectorScreenScale;
  }

  /**
   * Sets the vector screen scale.
   * @param scale  the screen scale
   */
  public void setVectorScreenScale(float scale) {
    vectorScreenScale = scale;
  }

  /**
   * Vector screen scale.
   */
  private float vectorScreenScale;

  /**
   * Gets the atom screen scale.
   */
  public float getAtomScreenScale() {
    return atomScreenScale;
  }

  /**
   * Sets the atom screen scale.
   * @param scale  the screen scale
   */
  public void setAtomScreenScale(float scale) {
    atomScreenScale = scale;
  }

  /**
   * Atom screen scale.
   */
  private float atomScreenScale;

  /**
   * Sets the atom z offset.
   * @param z  the z offset
   */
  public void setAtomZOffset(int z) {
    atomZOffset = z;
  }

  /**
   * Gets the atom z offset.
   */
  public int getAtomZOffset() {
    return atomZOffset;
  }

  /**
   * Atom z offset.
   */
  private int atomZOffset = 1;

  /**
   * Sets the atom depth factor.
   * @param z  the z offset
   */
  public void setAtomDepthFactor(float f) {
    atomDepthFactor = f;
  }

  /**
   * Gets the atom depth factor.
   */
  public float getAtomDepthFactor() {
    return atomDepthFactor;
  }

  /**
   * Atom depth factor.
   */
  private float atomDepthFactor = 0.33f;

  /**
   * Sets the atom sphere factor.
   * @param z  the z offset
   */
  public void setAtomSphereFactor(double d) {
    atomSphereFactor = d;
  }

  /**
   * Gets the atom sphere factor.
   */
  public double getAtomSphereFactor() {
    return atomSphereFactor;
  }

  /**
   * Atom sphere factor.
   */
  private double atomSphereFactor = 0.2;

  /**
   * Returns whether anti-aliased lines will be drawn.
   */
  public boolean isAntiAliased() {
    return antiAliased;
  }

  /**
   * Sets whether anti-aliased lines will be drawn.
   *
   * @param on if true then anti-aliasing will be turned on.
   */
  public void setAntiAliased(boolean on) {
    antiAliased = on;
  }

  /**
   * Sets the fast rendering flag.
   * @param b  whether to do fast rendering
   */
  public void setFastRendering(boolean b) {
    doFastRendering = b;
  }

  /**
   * Gets the fast rendering flag.
   */
  public boolean getFastRendering() {
    return doFastRendering;
  }

  /**
   * Flag for fast rendering.
   * Added by T.GREY for quick drawing on atom movement.
   */
  private boolean doFastRendering = false;

  /**
   * The set of indicies for selected atoms.
   */
  private IntSet pickedAtoms = new IntSet();

  public IntSet getPickedAtoms() {
    return pickedAtoms;
  }
  
  public void addPickedAtom(Atom atom) {
    int atomNumber = atom.getAtomNumber();
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.add(atomNumber);
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  public void addPickedAtoms(Atom[] atoms) {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    for (int i = 0; i < atoms.length; ++i) {
      int atomNumber = atoms[i].getAtomNumber();
      pickedAtoms.add(atomNumber);
    }
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  public void removePickedAtom(Atom atom) {
    int atomNumber = atom.getAtomNumber();
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.remove(atomNumber);
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  public void removePickedAtoms(Atom[] atoms) {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    for (int i = 0; i < atoms.length; ++i) {
      int atomNumber = atoms[i].getAtomNumber();
      pickedAtoms.remove(atomNumber);
    }
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  public void togglePickedAtom(Atom atom) {
    int atomNumber = atom.getAtomNumber();
    if (pickedAtoms.contains(atomNumber)) {
      removePickedAtom(atom);
    } else {
      addPickedAtom(atom);
    }
  }

  public void togglePickedAtoms(Atom[] atoms) {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    for (int i = 0; i < atoms.length; ++i) {
      int atomNumber = atoms[i].getAtomNumber();
      if (pickedAtoms.contains(atomNumber)) {
        pickedAtoms.remove(atomNumber);
      } else {
        pickedAtoms.add(atomNumber);
      }
    }
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  
  public void clearPickedAtoms() {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.clear();
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }

  public boolean isAtomPicked(Atom atom) {
    return pickedAtoms.contains(atom.getAtomNumber());
  }
  
  public static final String atomPickedProperty = "atomPicked";

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.removePropertyChangeListener(listener);
  }

  private PropertyChangeSupport changeSupport =
    new PropertyChangeSupport(this);

  public DisplaySettings copy() {
    DisplaySettings dsNew = null;
    try {
      dsNew = (DisplaySettings) clone();
    } catch (CloneNotSupportedException e) {}
    return dsNew;
  }
}

