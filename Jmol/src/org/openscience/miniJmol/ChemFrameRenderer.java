/*
 * Copyright 2001 The Jmol Development Team
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

import java.awt.Graphics;
import org.openscience.jmol.DisplaySettings;
import java.util.Enumeration;

/**
 *  Drawing methods for ChemFrame.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ChemFrameRenderer {

	/**
	 * Paint this model to a graphics context.  It uses the matrix
	 * associated with this model to map from model space to screen
	 * space.
	 *
	 * @param g the Graphics context on which to paint
     * @param frame the ChemFrame to render
	 * @param settings the display settings
	 */
	public void paint(Graphics g, ChemFrame frame, DisplaySettings settings) {

		if ((frame.getAtoms() == null) || (frame.getNumberAtoms() <= 0)) {
			return;
		}
		frame.transform();
        if (bufferedAtomZs == null || bufferedAtomZs.length != frame.getNumberAtoms()) {
            bufferedAtomZs = new float[frame.getNumberAtoms()];
        }
        for (int i=0; i < frame.getNumberAtoms(); ++i) {
            bufferedAtomZs[i] = frame.getAtoms()[i].getScreenPosition().z;
        }

		if (!settings.getFastRendering() || zSortedAtomIndicies == null) {
			sortAtoms(frame);
		}
		for (int i = 0; i < frame.getNumberAtoms(); i++) {
			int j = zSortedAtomIndicies[i];
			if (settings.getShowBonds()) {
				Enumeration bondIter = frame.getAtoms()[j].getBondedAtoms();
				while (bondIter.hasMoreElements()) {
                    Atom otherAtom = (Atom)bondIter.nextElement();
                    if (otherAtom.getScreenPosition().z < frame.getAtoms()[j].getScreenPosition().z) {
						bondRenderer.paint(g, frame.getAtoms()[j],
								otherAtom, settings);
                    }
				}
			}
			if (settings.getShowAtoms() && !settings.getFastRendering()) {
				atomRenderer.paint(g, frame.getAtoms()[j], frame.getPickedAtoms()[j], settings,
						false);
			} else if (settings.getFastRendering()) {
				atomRenderer.paint(g, frame.getAtoms()[j], frame.getPickedAtoms()[j], settings,
						true);
			}
			if (settings.getShowBonds()) {
				Enumeration bondIter = frame.getAtoms()[j].getBondedAtoms();
				while (bondIter.hasMoreElements()) {
                    Atom otherAtom = (Atom)bondIter.nextElement();
                    if (otherAtom.getScreenPosition().z >= frame.getAtoms()[j].getScreenPosition().z) {
						bondRenderer.paint(g, frame.getAtoms()[j],
								otherAtom, settings);
                    }
				}
			}

		}
	}

	private void sortAtoms(ChemFrame frame) {

		if (frame.getNumberAtoms() == 0) {
			return;
		}
        int numberAtoms = frame.getNumberAtoms();
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

	/**
	 * Renderer for atoms.
	 */
	private AtomRenderer atomRenderer = new AtomRenderer();

	/**
	 * Renderer for bonds.
	 */
	private BondRenderer bondRenderer = new BondRenderer();

	/**
	 * Array of atom indicies sorted by Z coordinate.
	 */
	private int[] zSortedAtomIndicies;

	/**
	 * Temporary array used in the bubble sort.
	 * Kept as object variables to prevent allocating and freeing
	 * memory for each call to the sort routine.
	 */
	private int[] zSortedAtomIndicies2;

	/**
	 * Temporary array used in the bubble sort.
	 * Kept as object variables to prevent allocating and freeing
	 * memory for each call to the sort routine.
	 */
	private int[] zBubbles;

	/**
	 * Z coordinates of the atoms, buffered for quick access.
	 */
	private float[] bufferedAtomZs;
}
