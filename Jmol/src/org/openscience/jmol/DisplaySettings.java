/*
 * DisplaySettings.java
 * 
 * Copyright (C) 1999  Bradley A. Smith
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol;

import java.awt.Color;

/**
 * Stores the display style of graphical elements.
 */
public class DisplaySettings {

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
	 * Display style for drawing bonds.
	 */
	private int bondDrawMode = QUICKDRAW;

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

    private static boolean ShowAtoms     = true;
    private static boolean ShowBonds     = true;
    private static boolean ShowVectors   = false;
    private static boolean ShowHydrogens = true;

    /**
     * Toggles on/off the flag that decides whether atoms are shown
     * when displaying a ChemFrame
     */
    public static void toggleAtoms() {
        ShowAtoms = !ShowAtoms;
    }    

    /**
     * Toggles on/off the flag that decides whether bonds are shown
     * when displaying a ChemFrame
     */
    public void toggleBonds() {
        ShowBonds = !ShowBonds;
    }

    /**
     * Toggles on/off the flag that decides whether vectors are shown
     * when displaying a ChemFrame
     */
    public void toggleVectors() {
        ShowVectors = !ShowVectors;
    }

    /**
     * Toggles on/off the flag that decides whether Hydrogen atoms are
     * shown when displaying a ChemFrame 
     */
    public void toggleHydrogens() {
        ShowHydrogens = !ShowHydrogens;
    }

    /**
     * Set the flag that decides whether atoms are shown
     * when displaying a ChemFrame
     *
     * @param sa the value of the flag
     */
    public void setShowAtoms(boolean sa) {
        ShowAtoms = sa;
    }

    /**
     * Set the flag that decides whether bonds are shown
     * when displaying a ChemFrame
     *
     * @param sb the value of the flag
     */
    public void setShowBonds(boolean sb) {
        ShowBonds = sb;
    }

    /**
     * Set the flag that decides whether vectors are shown
     * when displaying a ChemFrame
     *
     * @param sv the value of the flag
     */
    public void setShowVectors(boolean sv) {
        ShowVectors = sv;
    }

    /**
     * Set the flag that decides whether Hydrogen atoms are shown
     * when displaying a ChemFrame.  Currently non-functional.
     *
     * @param sh the value of the flag
     */
    public void setShowHydrogens(boolean sh) {
        ShowHydrogens = sh;
    }

    public boolean getShowAtoms() {
        return ShowAtoms;
    }
    public boolean getShowBonds() {
        return ShowBonds;
    }
    public boolean getShowVectors() {
        return ShowVectors;
    }
	public boolean getShowHydrogens() {
        return ShowHydrogens;
    }
}
