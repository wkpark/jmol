/*
 * Copyright 2001 The Jmol Development Team
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
		if (!settings.getFastRendering() || atomReferences == null) {
			if (atomReferences == null || atomReferences.length != frame.getNumberAtoms()) {
				atomReferences = new AtomReference[frame.getNumberAtoms()];
				for (int i=0; i < atomReferences.length; ++i) {
					atomReferences[i] = new AtomReference();
				}
			}
			for (int i=0; i < frame.getNumberAtoms(); ++i) {
				atomReferences[i].index = i;
				atomReferences[i].z = frame.getAtoms()[i].getScreenPosition().z;
			}
			
			if (frame.getNumberAtoms() > 1) {
				sorter.sort(atomReferences);
			}
		}
		for (int i = 0; i < frame.getNumberAtoms(); i++) {
			int j = atomReferences[i].index;
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

	/**
	 * Renderer for atoms.
	 */
	private AtomRenderer atomRenderer = new AtomRenderer();

	/**
	 * Renderer for bonds.
	 */
	private BondRenderer bondRenderer = new BondRenderer();

	class AtomReference {
		int index;
		float z;
	}

	AtomReference[] atomReferences;

	HeapSorter sorter = new HeapSorter( new HeapSorter.Comparator() {
		public int compare(Object atom1, Object atom2) {
			AtomReference a1 = (AtomReference) atom1;
			AtomReference a2 = (AtomReference) atom2;
			if (a1.z < a2.z) {
				return -1;
			} else if (a1.z > a2.z) {
				return 1;
			}
			return 0;
		}
	});

}
