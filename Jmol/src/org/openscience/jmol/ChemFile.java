
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
package org.openscience.jmol;

import java.io.*;
import java.util.Vector;

public class ChemFile {

	/**@shapeType AggregationLink
	@associates <b>ChemFrame</b>*/
	Vector frames;
	int nframes = 0;
	Vector AtomPropertyList = new Vector();
	Vector FramePropertyList = new Vector();

	/**
	 * Very simple class that should be subclassed for each different
	 * kind of file that can be read by Jmol.
	 */
	public ChemFile() {
		frames = new Vector(1);
	}

	/**
	 * returns a ChemFrame from a sequence of ChemFrames that make up
	 * this ChemFile
	 *
	 * @see ChemFrame
	 * @param whichframe which frame to return
	 */
	public ChemFrame getFrame(int whichframe) {
		return (ChemFrame) frames.elementAt(whichframe);
	}

	/**
	 * returns the number of frames in this file
	 */
	public int nFrames() {
		return frames.size();
	}

	/**
	 * returns the vector containing the descriptive list of Physical
	 * properties that this file contains.
	 */
	public Vector getAtomPropertyList() {
		return AtomPropertyList;
	}

	/**
	 * returns the vector containing the descriptive list of Frame
	 * properties that this file contains.
	 */
	public Vector getFramePropertyList() {
		return FramePropertyList;
	}

}

