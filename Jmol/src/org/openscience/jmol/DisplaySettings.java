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
	public static void setLabelMode(int i) {
		if (i > NUMBERS) {
			return;
		} 
		labelMode = i;
	} 

	/**
	 * Gets the display style for labels.
	 */
	public static int getLabelMode() {
		return labelMode;
	} 

	/**
	 * Sets the display style for drawing atoms.
	 */
	public static void setAtomDrawMode(int i) {
		if (i >= LINE) {
			return;
		} 
		atomDrawMode = i;
	} 

	/**
	 * Gets the display style for drawing atoms.
	 */
	public static int getAtomDrawMode() {
		return atomDrawMode;
	} 

	/**
	 * Sets the display style for drawing bonds.
	 */
	public static void setBondDrawMode(int i) {
		if (i > LINE) {
			return;
		} 
		bondDrawMode = i;
	} 

	/**
	 * Gets the display style for drawing bonds.
	 */
	public static int getBondDrawMode() {
		return bondDrawMode;
	} 

	/**
	 * Sets the color used for outlining atoms and bonds in QUICKDRAW
	 * and WIREFRAME drawing modes.
	 */
	public static void setOutlineColor(Color c) {
		outlineColor = c;
	} 

	/**
	 * Gets the color used for outlining atoms and bonds in QUICKDRAW
	 * and WIREFRAME drawing modes.
	 */
	public static Color getOutlineColor() {
		return outlineColor;
	} 

	/**
	 * Sets the color used for highlighting selected atoms.
	 */
	public static void setPickedColor(Color c) {
		pickedColor = c;
	} 

	/**
	 * Gets the color used for highlighting selected atoms.
	 */
	public static Color getPickedColor() {
		return pickedColor;
	} 

	/**
	 * Sets the color used for drawing text.
	 */
	public static void setTextColor(Color c) {
		textColor = c;
	} 

	/**
	 * Gets the color used for drawing text.
	 */
	public static Color getTextColor() {
		return textColor;
	} 

	/**
	 * Sets physical property to be displayed.
	 * 
	 * @param s The string descriptor of the physical property.
	 */
	public static void setPropertyMode(String s) {
		propertyMode = s;
	} 

	/*
	 * Returns the descriptor of the physical property currently displayed.
	 */
	public static String getPropertyMode() {
		return propertyMode;
	} 

	/**
	 * Sets the color of the line drawn for distance measurements.
	 * @param c the color
	 */
	public static void setDistanceColor(Color c) {
		distanceColor = c;
	} 

	/**
	 * Gets the color of the line drawn for distance measurements.
	 */
	public static Color getDistanceColor() {
		return distanceColor;
	} 

	/**
	 * Sets the color of the line drawn for angle measurements.
	 * @param c the color
	 */
	public static void setAngleColor(Color c) {
		angleColor = c;
	} 

	/**
	 * Gets the color of the line drawn for angle measurements.
	 */
	public static Color getAngleColor() {
		return angleColor;
	} 

	/**
	 * Sets the color of the line drawn for dihedral measurements.
	 * @param c the color
	 */
	public static void setDihedralColor(Color c) {
		dihedralColor = c;
	} 

	/**
	 * Gets the color of the line drawn for dihedral measurements.
	 */
	public static Color getDihedralColor() {
		return dihedralColor;
	} 

	/**
	 * Display style for labels.
	 */
	private static int labelMode = NOLABELS;

	/**
	 * Display style for drawing atoms.
	 */
	private static int atomDrawMode = QUICKDRAW;

	/**
	 * Display style for drawing bonds.
	 */
	private static int bondDrawMode = QUICKDRAW;

	/**
	 * Color used for outlining atoms and bonds in QUICKDRAW and
	 * WIREFRAME drawing modes.
	 */
	private static Color outlineColor = Color.black;

	/**
	 * Descriptor of the physical property to be displayed.
	 */
	private static String propertyMode = "";

	/**
	 * Color used for highlighting selected atoms.
	 */
	private static Color pickedColor = Color.orange;

	/**
	 * Color used for drawing text.
	 */
	private static Color textColor = Color.black;

	/**
	 * Color of the line drawn for distance measurements.
	 */
	private static Color distanceColor = Color.black;

	/**
	 * Color of the line drawn for angle measurements.
	 */
	private static Color angleColor = Color.black;

	/**
	 * Color of the line drawn for dihedral measurements.
	 */
	private static Color dihedralColor = Color.black;
}
