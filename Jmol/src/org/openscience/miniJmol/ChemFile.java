
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

import java.util.Vector;

public class ChemFile {

	/**
																																	 * Frames contained by this file.
																																	 *
																																	 * @shapeType AggregationLink
																																	 * @associates <b>ChemFrame</b>
																																	 */
	private Vector frames = new Vector(1);
	private boolean bondsEnabled = true;
	private Vector propertyList = new Vector();

	/**
	 * Very simple class that should be subclassed for each different
	 * kind of file that can be read by Jmol.
	 */
	public ChemFile() {
	}

	public ChemFile(boolean bondsEnabled) {
		this.bondsEnabled = bondsEnabled;
	}

	public boolean getBondsEnabled() {
		return bondsEnabled;
	}

	/**
	 * returns a ChemFrame from a sequence of ChemFrames that make up
	 * this ChemFile
	 *
	 * @see ChemFrame
	 * @param whichframe which frame to return
	 */
	public ChemFrame getFrame(int whichframe) {
		if (whichframe < frames.size()) {
			return (ChemFrame) frames.elementAt(whichframe);
		}
		return null;
	}

	/**
	 * Adds a frame to this file.
																																	 *
																																	 * @param frame the frame to be added
	 */
	public void addFrame(ChemFrame frame) {
		frames.addElement(frame);
	}

	/**
	 * Returns the number of frames in this file.
	 */
	public int getNumberFrames() {
		return frames.size();
	}

	/**
	 * Returns a list of descriptions for physical properties
	 * contained by this file.
																																	 */
	public Vector getPropertyList() {
		return propertyList;
	}

	/**
																																	 * Adds a property description to the property list.
																																	 *
																																	 * @param prop the property description
																																	 */
	public void addProperty(String prop) {
		if (propertyList.indexOf(prop) < 0) {
			propertyList.addElement(prop);
		}
	}
}

