
/*
 * @(#)ChemFrame.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.
 */

package org.openscience.miniJmol;

import org.openscience.jmol.DisplaySettings;
import org.openscience.jmol.PhysicalProperty;
import org.openscience.jmol.Matrix3D;
import java.awt.Graphics;
import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3f;

public class ChemFrame {

	/**
	 * returns the number of atoms that are currently in the "selected"
	 * list for future operations
	 */
	public int getNpicked() {
		return napicked;
	}

	/**
	 * Toggles on/off the flag that decides whether atoms are shown
	 * when displaying a ChemFrame
	 */
	public void toggleAtoms() {
		showAtoms = !showAtoms;
	}

	/**
	 * Toggles on/off the flag that decides whether Hydrogen atoms are
	 * shown when displaying a ChemFrame
	 */
	public void toggleHydrogens() {
		showHydrogens = !showHydrogens;
	}

	/**
	 * Sets whether atoms are shown when displaying a ChemFrame
	 *
	 * @param value is true if the atoms are to be shown.
	 */
	public void setShowAtoms(boolean value) {
		showAtoms = value;
	}

	/**
	 * Returns whether atoms are shown when displaying a ChemFrame
	 */
	public boolean getShowAtoms() {
		return showAtoms;
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

	public boolean getShowHydrogens() {
		return showHydrogens;
	}

	public void setBondFudge(float bf) {
		bondFudge = bf;
	}

	public float getBondFudge() {
		return bondFudge;
	}

	public void setAutoBond(boolean ab) {
		autoBond = ab;
	}

	public boolean getAutoBond() {
		return autoBond;
	}

	public void setMat(Matrix3D newmat) {
		mat = newmat;
	}

	/**
	 * Constructor for a ChemFrame with a known number of atoms.
	 *
	 * @param na the number of atoms in the frame
	 */
	public ChemFrame(int na, boolean bondsEnabled) {

		mat = new Matrix3D();
		mat.xrot(0);
		mat.yrot(0);
		frameProps = new Vector();
		atoms = new Atom[na];
		bufferedAtomZs = new float[atoms.length];
		pickedAtoms = new boolean[atoms.length];
		for (int i = 0; i < pickedAtoms.length; ++i) {
			pickedAtoms[i] = false;
		}
		this.bondsEnabled = bondsEnabled;
	}

	/**
	 * Constructor for a ChemFrame with an unknown number of atoms.
	 *
	 */
	public ChemFrame(boolean bondsEnabled) {
		this(100, bondsEnabled);
	}

	public ChemFrame() {
		this(true);
	}

	/**
	 * returns a Vector containing the list of PhysicalProperty descriptors
	 * present in this frame
	 */
	public Vector getFrameProps() {
		return frameProps;
	}

	/**
	 * Sets the information label for the frame
	 *
	 * @param info the information label for this frame
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * Returns this frame's information label
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Adds an atom to the frame and finds all bonds between the
	 * new atom and pre-existing atoms in the frame
	 *
	 * @param name the name of the extended atom type for the new atom
	 * @param x the x coordinate of the new atom
	 * @param y the y coordinate of the new atom
	 * @param z the z coordinate of the new atom
	 */
	public void addAtom(String name, float x, float y, float z) {
		addAtom(BaseAtomType.get(name), x, y, z);
	}

	/**
	 * Adds an atom to the frame and finds all bonds between the
	 * new atom and pre-existing atoms in the frame
	 *
	 * @param type atom type for the new atom
	 * @param x the x coordinate of the new atom
	 * @param y the y coordinate of the new atom
	 * @param z the z coordinate of the new atom
	 */
	public void addAtom(BaseAtomType type, float x, float y, float z) {

		if (numberAtoms >= atoms.length) {
			increaseArraySizes(2 * atoms.length);
		}

		atoms[numberAtoms] = new Atom(type, new Point3f(x, y, z),
				numberAtoms);

		if (bondsEnabled) {
			for (int j = 0; j < numberAtoms; j++) {
				if (Atom.closeEnoughToBond(atoms[numberAtoms], atoms[j],
						bondFudge)) {
					atoms[numberAtoms].addBondedAtom(atoms[j]);
					atoms[j].addBondedAtom(atoms[numberAtoms]);
				}
			}
		}
		++numberAtoms;
	}

	/**
	 * Adds an atom to the frame and finds all bonds between the
	 * new atom and pre-existing atoms in the frame
	 *
	 * @param atomicNumber the atomicNumber of the extended atom type for the new atom
	 * @param x the x coordinate of the new atom
	 * @param y the y coordinate of the new atom
	 * @param z the z coordinate of the new atom
	 */
	public void addAtom(int atomicNumber, float x, float y, float z) {

		BaseAtomType baseType = BaseAtomType.get(atomicNumber);
		if (baseType == null) {
			throw new RuntimeException("unable to get atom type");
		}
		addAtom(baseType, x, y, z);
	}

	/**
	 * Adds an atom to the frame and finds all bonds between the
	 * new atom and pre-existing atoms in the frame
	 *
	 * @param name the name of the extended atom type for the new atom
	 * @param x the x coordinate of the new atom
	 * @param y the y coordinate of the new atom
	 * @param z the z coordinate of the new atom
	 * @param props a Vector containing the properties of this atom
	 */
	public void addAtom(String name, float x, float y, float z,
			Vector props) {

		addAtom(name, x, y, z);
		atoms[numberAtoms].setProperties(props);

		for (int j = 0; j < props.size(); j++) {
			PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
			String desc = p.getDescriptor();

			// Update the frameProps if we found a new property
			if (frameProps.indexOf(desc) < 0) {
				frameProps.addElement(desc);
			}

		}
	}


	/**
	 * Returns the number of atoms in the ChemFrame
	 */
	public int getNumberAtoms() {
		return numberAtoms;
	}

	/**
	 * Returns the properties of an atom.
	 *
	 * @param i the index of the atom
	 */
	public Vector getAtomProperties(int i) {
		return atoms[i].getProperties();
	}

	/**
	 * Transform all the points in this model
	 */
	public void transform() {

		if (numberAtoms > 0) {
			for (int i = 0; i < numberAtoms; ++i) {
				bufferedAtomZs[i] = atoms[i].transform(mat);
			}
		}
	}

	/**
	 * Paint this model to a graphics context.  It uses the matrix
	 * associated with this model to map from model space to screen
	 * space.
	 *
	 * @param g the Graphics context to paint to
			 * @param settings the display settings
	 */
	public void paint(Graphics g, DisplaySettings settings) {

		if ((atoms == null) || (numberAtoms <= 0)) {
			return;
		}
		transform();

		//Added by T.GREY for quick-draw on move support
		if (!settings.getFastRendering()) {
			sortAtoms();
		}
		for (int i = 0; i < numberAtoms; i++) {
			int j = zSortedAtomIndicies[i];
			if (settings.getShowBonds()) {
				Enumeration bondIter = atoms[j].getBondedAtoms();
				while (bondIter.hasMoreElements()) {
					bondRenderer.paint(g, atoms[j],
							(Atom) bondIter.nextElement(), settings);
				}
			}

			//Added by T.GREY for quick-draw on move support
			if (showAtoms && !settings.getFastRendering()) {
				atomRenderer.paint(g, atoms[j], pickedAtoms[j], settings,
						false);
			} else if (settings.getFastRendering()) {
				atomRenderer.paint(g, atoms[j], pickedAtoms[j], settings,
						true);
			}
		}
	}

	/**
	 * Add all atoms in this frame to the list of picked atoms
	 */
	public void selectAll() {

		if (numberAtoms <= 0) {
			return;
		}
		napicked = 0;
		for (int i = 0; i < numberAtoms; i++) {
			pickedAtoms[i] = true;
			napicked++;
		}
	}

	/**
	 * Remove all atoms in this frame from the list of picked atoms
	 */
	public void deselectAll() {

		if (numberAtoms <= 0) {
			return;
		}
		for (int i = 0; i < numberAtoms; i++) {
			pickedAtoms[i] = false;
		}
		napicked = 0;
	}

	public int pickMeasuredAtom(int x, int y) {
		return getNearestAtom(x, y);
	}

	/**
	 * Clear out the list of picked atoms, find the nearest atom to a
	 * set of screen coordinates and add this new atom to the picked
	 * list.
	 *
	 * @param x the screen x coordinate of the selection point
	 * @param y the screen y coordinate of the selection point
	 */
	public void selectAtom(int x, int y) {

		int smallest = getNearestAtom(x, y);
		if (pickedAtoms[smallest]) {
			pickedAtoms[smallest] = false;
			napicked = 0;
		} else {
			pickedAtoms[smallest] = true;
			napicked = 1;
		}
		for (int i = 0; i < numberAtoms; i++) {
			if (i != smallest) {
				pickedAtoms[i] = false;
			}
		}
	}

	/**
	 * Find the nearest atom to a set of screen coordinates and add
	 * this new atom to the picked list.
	 *
	 * @param x the screen x coordinate of the selection point
	 * @param y the screen y coordinate of the selection point
	 */
	public void shiftSelectAtom(int x, int y) {

		int smallest = getNearestAtom(x, y);
		if (pickedAtoms[smallest]) {
			pickedAtoms[smallest] = false;
			napicked--;
		} else {
			pickedAtoms[smallest] = true;
			napicked++;
		}
	}

	/**
	 * Clear out the list of picked atoms, find all atoms within
	 * designated region and add these atoms to the picked list.
	 *
	 * @param x1 the x coordinate of point 1 of the region's bounding rectangle
	 * @param y1 the y coordinate of point 1 of the region's bounding rectangle
	 * @param x2 the x coordinate of point 2 of the region's bounding rectangle
	 * @param y2 the y coordinate of point 2 of the region's bounding rectangle
	 */
	public void selectRegion(int x1, int y1, int x2, int y2) {

		if (numberAtoms <= 0) {
			return;
		}
		transform();
		napicked = 0;
		for (int i = 0; i < numberAtoms; i++) {
			if (isAtomInRegion(i, x1, y1, x2, y2)) {
				pickedAtoms[i] = true;
				napicked++;
			} else {
				pickedAtoms[i] = false;
			}
		}
	}

	/**
	 * Find all atoms within designated region and add these atoms to
	 * the picked list.
	 *
	 * @param x1 the x coordinate of point 1 of the region's bounding rectangle
	 * @param y1 the y coordinate of point 1 of the region's bounding rectangle
	 * @param x2 the x coordinate of point 2 of the region's bounding rectangle
	 * @param y2 the y coordinate of point 2 of the region's bounding rectangle
	 */
	public void shiftSelectRegion(int x1, int y1, int x2, int y2) {

		if (numberAtoms <= 0) {
			return;
		}
		transform();
		for (int i = 0; i < numberAtoms; i++) {
			if (isAtomInRegion(i, x1, y1, x2, y2)) {
				if (!pickedAtoms[i]) {
					pickedAtoms[i] = true;
					napicked++;
				}
			}
		}
	}

	private boolean isAtomInRegion(int atomIndex, int x1, int y1, int x2,
			int y2) {

		int x = (int) atoms[atomIndex].getScreenPosition().x;
		int y = (int) atoms[atomIndex].getScreenPosition().y;
		if ((x > x1) && (x < x2)) {
			if ((y > y1) && (y < y2)) {
				return true;
			}
		}
		return false;
	}

	private int getNearestAtom(int x, int y) {

		if (numberAtoms <= 0) {
			return -1;
		}
		transform();
		int smallest = -1;
		int smallr2 = Integer.MAX_VALUE;
		for (int i = 0; i < numberAtoms; i++) {
			int dx = (int) atoms[i].getScreenPosition().x - x;
			int dy = (int) atoms[i].getScreenPosition().y - y;
			int dr2 = dx * dx + dy * dy;
			if (dr2 < smallr2) {
				smallest = i;
				smallr2 = dr2;
			}
		}
		if (smallest >= 0) {
			return smallest;
		}
		return -1;
	}

	/**
	 * Find the bounds of this model.
	 */
	public void findBounds() {

		if ((atoms == null) || (numberAtoms <= 0)) {
			return;
		}
		min = new Point3f(atoms[0].getPosition());
		max = new Point3f(min);
		for (int i = 1; i < numberAtoms; ++i) {
			float x = atoms[i].getPosition().x;
			if (x < min.x) {
				min.x = x;
			}
			if (x > max.x) {
				max.x = x;
			}
			float y = atoms[i].getPosition().y;
			if (y < min.y) {
				min.y = y;
			}
			if (y > max.y) {
				max.y = y;
			}
			float z = atoms[i].getPosition().z;
			if (z < min.z) {
				min.z = z;
			}
			if (z > max.z) {
				max.z = z;
			}
		}
	}

	public Point3f getMinimumBounds() {
		return min;
	}

	public Point3f getMaximumBounds() {
		return max;
	}

	/**
	 * Walk through this frame and find all bonds again.
	 */
	public void rebond() {

		// Clear the currently existing bonds.
		for (int i = 0; i < numberAtoms; i++) {
			atoms[i].clearBondedAtoms();
		}

		// Do a n*(n-1) scan to get new bonds.
		for (int i = 0; i < numberAtoms - 1; i++) {
			for (int j = i; j < numberAtoms; j++) {
				if (Atom.closeEnoughToBond(atoms[i], atoms[j], bondFudge)) {
					atoms[i].addBondedAtom(atoms[j]);
					atoms[j].addBondedAtom(atoms[i]);
				}
			}
		}
	}

	private void increaseArraySizes(int newArraySize) {

		Atom newAtoms[] = new Atom[newArraySize];
		System.arraycopy(atoms, 0, newAtoms, 0, atoms.length);
		atoms = newAtoms;

		float newAtomZs[] = new float[newArraySize];
		System.arraycopy(bufferedAtomZs, 0, newAtomZs, 0,
				bufferedAtomZs.length);
		bufferedAtomZs = newAtomZs;

		boolean np[] = new boolean[newArraySize];
		System.arraycopy(pickedAtoms, 0, np, 0, pickedAtoms.length);
		pickedAtoms = np;
	}

	private void sortAtoms() {

		if (numberAtoms == 0) {
			return;
		}
		if ((zSortedAtomIndicies == null)
				|| (zSortedAtomIndicies.length != numberAtoms)) {
			zSortedAtomIndicies = new int[numberAtoms];
			zSortedAtomIndicies2 = new int[numberAtoms];
			zBubbles = new int[numberAtoms];
			for (int i = 0; i < numberAtoms; ++i) {
				zSortedAtomIndicies[i] = i;
			}
		}
		if (numberAtoms == 1) {
			zSortedAtomIndicies[0] = 0;
			return;
		}

		/*
		 * I use a bubble sort since from one iteration to the next, the sort
		 * order is pretty stable, so I just use what I had last time as a
		 * "guess" of the sorted order.  With luck, this reduces O(N log N)
		 * to O(N)
		 */

		/*
		 * New Mods:
		 * Do single pass of bubble sort, if this fixes order, simply return
		 * While doing bubble sort, form lists of fully order sub-lists so
		 * that a more efficient merge-sort can be used if order is not resolved
		 * by the bubble sort.
		 *
		 */

		// Change to true if one-pass bubble sort doesn't fix the values.
		boolean flipped = false;

		// The number of fully ordered sublists
		int zBubbleCount = 0;

		int preN = zSortedAtomIndicies[0];
		int thisN = zSortedAtomIndicies[1];

		// Store array values in local variables because array lookups are
		// more expensive than local variable references
		float preVal = bufferedAtomZs[preN];
		float thisVal = bufferedAtomZs[thisN];
		float oldVal = 0.0f;
		zBubbles[zBubbleCount++] = 0;
		if (preVal <= thisVal) {
			oldVal = preVal;
			preVal = thisVal;
			preN = thisN;
		} else {
			oldVal = thisVal;
			zSortedAtomIndicies[0] = thisN;
			zSortedAtomIndicies[1] = preN;
		}
		for (int j = 2; j < numberAtoms; j++) {
			thisN = zSortedAtomIndicies[j];
			thisVal = bufferedAtomZs[thisN];
			if (thisVal < preVal) {
				if (thisVal < oldVal) {
					zBubbles[zBubbleCount++] = j - 1;
					flipped = true;
				}
				zSortedAtomIndicies[j - 1] = thisN;
				zSortedAtomIndicies[j] = preN;
				oldVal = thisVal;
			} else {
				oldVal = preVal;
				preVal = thisVal;
				preN = thisN;
			}
		}

		// Now, have done one pass of bubble sort, and have zBubbles ready for doing a
		// merge sort.  If flipped=false, then single bubble sort pass was enough to
		// correct the order, so just exit out.
		if (!flipped) {
			return;
		}

		boolean direct = false;		// which of zSortedAtomIndicies is from and to
		int[] from = zSortedAtomIndicies;
		int[] to = zSortedAtomIndicies2;

		while (zBubbleCount > 1) {

			// Keep merging sorted sub-lists until a single list remains.
			if (direct) {
				from = zSortedAtomIndicies2;
				to = zSortedAtomIndicies;
			} else {
				from = zSortedAtomIndicies;
				to = zSortedAtomIndicies2;
			}
			int fromBubblePos = 0;
			int toBubblePos = 0;
			int toPos = 0;
			while (fromBubblePos < (zBubbleCount - 1)) {

				// While two or more lists to merge, take top two lists, and merge them
				// into a single list.

				int bubble1At = toPos;
				int bubble2At = zBubbles[++fromBubblePos];
				zBubbles[toBubblePos++] = bubble1At;
				int bubble1End = bubble2At;
				++fromBubblePos;
				int bubble2End = 0;
				if (fromBubblePos >= zBubbleCount) {
					bubble2End = numberAtoms;
				} else {
					bubble2End = zBubbles[fromBubblePos];
				}
				preN = from[bubble1At];
				thisN = from[bubble2At];
				preVal = bufferedAtomZs[preN];
				thisVal = bufferedAtomZs[thisN];
				while (true) {

					// This is the loop for the mering process.
					if (preVal < thisVal) {
						to[toPos] = preN;
						++toPos;
						++bubble1At;
						if (bubble1At == bubble1End) {
							while (bubble2At < bubble2End) {
								to[toPos] = from[bubble2At];
								++toPos;
								++bubble2At;
							}
							break;
						} else {
							preN = from[bubble1At];
							preVal = bufferedAtomZs[preN];
						}
					} else {
						to[toPos] = thisN;
						++toPos;
						++bubble2At;
						if (bubble2At == bubble2End) {
							while (bubble1At < bubble1End) {
								to[toPos] = from[bubble1At];
								++toPos;
								++bubble1At;
							}
							break;
						} else {
							thisN = from[bubble2At];
							thisVal = bufferedAtomZs[thisN];
						}
					}
				}
			}

			// At this point, we check for a single unmerged list, and copy it across to
			// the new array.
			if (fromBubblePos < zBubbleCount) {
				zBubbles[toBubblePos] = toPos;
				++toBubblePos;
				while (toPos < numberAtoms) {
					to[toPos] = from[toPos];
					toPos++;
				}
			}
			zBubbleCount = toBubblePos;
			direct = !direct;		// swap from and to lists.
		}
		zSortedAtomIndicies = to;
		zSortedAtomIndicies2 = from;

		// OK, now has been merge sorted.

		// The commented code below is the original bubble sort.

		/*
										for (int i = numberAtoms - 1; --i >= 0;) {
														boolean flipped = false;
														passes++;
														for (int j = 0; j <= i; j++) {
																		int a = zSortedAtomIndicies[j];
																		int b = zSortedAtomIndicies[j+1];
																		if (bufferedAtomZs[a] > bufferedAtomZs[b]) {
																						zSortedAtomIndicies[j+1] = a;
																						zSortedAtomIndicies[j] = b;
																						flipped = true;
																		}
														}
														if (!flipped) {
																		break;
														}
										}
		*/
	}

	private float bondFudge = 1.12f;
	private boolean autoBond = true;
	private boolean showAtoms = true;
	private boolean showHydrogens = true;
	private Matrix3D mat;
	private boolean[] pickedAtoms;
	private int napicked;

	// This stuff can vary for each frame in the dynamics:

	/**
	 * Information string for this frame.
	 */
	String info;

	/**
	 * Array of atoms.
	 */
	private Atom[] atoms;

	/**
	 * Number of atoms in frame.
	 */
	private int numberAtoms = 0;

	/**
	 * Z coordinates of the atoms, buffered for quick access.
	 */
	private float[] bufferedAtomZs;

	/**
	 * Array of atom indicies sorted by Z coordinate.
	 */
	private int[] zSortedAtomIndicies;

	/**
	 * Next two are temporary arrays used in the bubble sort.
	 * Keep them as object variables to prevent allocating and freeing
	 * memory for each call to the sort routine.
	 */
	private int[] zSortedAtomIndicies2;
	private int[] zBubbles;

	/**
	 * Renderer for atoms.
	 */
	private AtomRenderer atomRenderer = new AtomRenderer();

	/**
	 * Renderer for bonds.
	 */
	private BondRenderer bondRenderer = new BondRenderer();

	/**
	 * List of all atom properties contained by atoms in this frame.
	 */
	private Vector frameProps;

	/**
	 * Minimum co-ords of the atoms in this frame
	 */
	private Point3f min;

	/**
	 * Maximum co-ords of the atoms in this frame
	 */
	private Point3f max;

	private boolean bondsEnabled;
}
