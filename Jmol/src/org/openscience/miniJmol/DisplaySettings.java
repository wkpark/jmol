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
package org.openscience.miniJmol;

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
	 * Sets the bond width.
	 * @param width  the bond width
	 */
	public void setBondWidth(float width) {
		bondWidth = width;
	} 

	/**
	 * Gets the bond width.
	 */
	public float getBondWidth() {
		return bondWidth;
	} 

	/**
	 * Sets the bond screen scale.
	 * @param scale  the screen scale
	 */
	public void setBondScreenScale(float scale) {
		bondScreenScale = scale;
	} 

	/**
	 * Gets the bond screen scale.
	 */
	public float getBondScreenScale() {
		return bondScreenScale;
	} 

	/**
	 * Sets flag for whether to draw bonds to atom centers.
	 */
    public void setDrawBondsToAtomCenters(boolean on) {
        drawBondsToAtomCenters = on;
    }

	/**
	 * Gets flag for whether to draw bonds to atom centers.
	 */
    public boolean getDrawBondsToAtomCenters() {
        return drawBondsToAtomCenters;
    }

	/**
	 * Gets the light source vector.
	 */
    public float[] getLightSourceVector() {
        return lightSource;
    }

	/**
	 * Sets the scale at which atoms will be drawn.
	 * @param scale  the screen scale
	 */
	public void setAtomScreenScale(float scale) {
		atomScreenScale = scale;
	} 

	/**
	 * Gets the scale at which atoms will be drawn.
	 */
	public float getAtomScreenScale() {
		return atomScreenScale;
	} 

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
     * Returns the on-screen radius of an atom with the given radius.
     *
     * @param z z position in screen space
     */ 
    public float getCircleRadius(int z, double radius) {
        double raw = radius*atomSphereFactor;
        float depth = (float)(z - atomZOffset) / (2.0f*atomZOffset);
        float tmp = atomScreenScale * ((float)raw + atomDepthFactor*depth);
        return tmp < 0.0f ? 1.0f : tmp;
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

	/**
	 * Bond screen scale.
	 */
	private float bondScreenScale;

	/**
	 * Bond width.
	 */
	private float bondWidth = 0.1f;

	/**
	 * Flag for whether to draw bonds to the center of atoms.
	 */
	private boolean drawBondsToAtomCenters = false;

    /**
	 * Place the light source for shaded atoms to the upper right of
	 * the atoms and out of the plane.
	 */
    private float[] lightSource = { 1.0f, -1.0f, 2.0f};

	/**
	 * The scale at which atoms will be drawn.
	 */
    private float atomScreenScale = 1.0f;

	/**
	 * Atom z offset.
	 */
    private int atomZOffset = 1;

	/**
	 * Atom depth factor.
	 */
    private float atomDepthFactor = 0.33f;

	/**
	 * Atom sphere factor.
	 */
    private double atomSphereFactor = 0.2;

	/**
	 * Flag for fast rendering.
	 * Added by T.GREY for quick drawing on atom movement.
	 */
    private boolean doFastRendering = false;

}
