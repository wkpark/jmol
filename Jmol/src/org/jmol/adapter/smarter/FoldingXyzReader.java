/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;

/**
 * This reader is for current.xyz files generated by Folding@Home project
 * (see http://folding.stanford.edu)
 * 
 * I have not found a precise description of the file format.
 * I used source code from fpd from Dick Howell to analyze the file format.
 * (see http://boston.quik.com/rph)
 */

class FoldingXyzReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("Folding@Home");

    try {
      String[] elements = reader.readLine().trim().split("[ \t]+");
      if (elements.length > 0) {
      	int modelAtomCount = Integer.parseInt(elements[0]);
      	atomSetCollection.newAtomSet();
      	if (elements.length > 1) {
      		atomSetCollection.setAtomSetName("Protein " + elements[1]);
      	}
      	readAtoms(reader, modelAtomCount);
      }
    } catch (Exception ex) {
      atomSetCollection.errorMessage = "Could not read file:" + ex;
    }
    return atomSetCollection;
  }
	    
  final float[] chargeAndOrVector = new float[4];
  final boolean isNaN[] = new boolean[4];
  
  void readAtoms(BufferedReader reader,
                 int modelAtomCount) throws Exception {
    for (int i = 0; i <= modelAtomCount; ++i) {
      String line = reader.readLine();
      if ((line != null) && (line.length() == 0)) {
      	line = reader.readLine();
      }
      if (line != null) {
	    System.out.println("Line: " + line);
	    Atom atom = atomSetCollection.addNewAtom();
	    parseInt(line);
	    atom.atomName = parseToken(line, ichNextParse);
	    if (atom.atomName != null) {
	    	atom.elementSymbol = atom.atomName.substring(0, 1);
	    }
	    atom.x = parseFloat(line, ichNextParse);
	    atom.y = parseFloat(line, ichNextParse);
	    atom.z = parseFloat(line, ichNextParse);
	    parseInt(line, ichNextParse);
	    int bondNum = Integer.MIN_VALUE;
	    while ((bondNum = parseInt(line, ichNextParse)) > 0) {
	      atomSetCollection.addNewBond(i, bondNum - 1);
	    }
      }
    }
  }
}
