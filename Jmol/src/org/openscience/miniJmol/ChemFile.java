/*
 * @(#)ChemFile.java    1.0 99/01/19
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
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

import java.io.*;
import java.util.Vector;

public class ChemFile {
    
    /**
	 * Frames contained by this file.
	 *
	 * @shapeType AggregationLink
	 * @associates <b>ChemFrame</b>
	 */
    private Vector frames;
    private Vector propertyList = new Vector();
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

