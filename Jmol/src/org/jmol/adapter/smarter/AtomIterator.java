/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-08-09 08:31:30 -0500 (Thu, 09 Aug 2012) $
 * $Revision: 17434 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.V3;


/* **************************************************************
 * the frame iterators
 * **************************************************************/
class AtomIterator implements JmolAdapterAtomIterator {
	private int iatom;
	private Atom atom;
	private int atomCount;
	private Atom[] atoms;
	private BS bsAtoms;

	AtomIterator(AtomSetCollection atomSetCollection) {
		atomCount = atomSetCollection.getAtomCount();
		atoms = atomSetCollection.getAtoms();
		bsAtoms = atomSetCollection.bsAtoms;
		iatom = 0;
	}

	public boolean hasNext() {
		if (iatom == atomCount)
			return false;
		while ((atom = atoms[iatom++]) == null
				|| (bsAtoms != null && !bsAtoms.get(atom.index)))
			if (iatom == atomCount)
				return false;
		atoms[iatom - 1] = null; // single pass
		return true;
	}

	public int getAtomSetIndex() {
		return atom.atomSetIndex;
	}

	
	public BS getAtomSymmetry() {
		return atom.bsSymmetry;
	}

	
	public int getAtomSite() {
		return atom.atomSite + 1;
	}

	
	public Object getUniqueID() {
		return Integer.valueOf(atom.index);
	}

	
	public int getElementNumber() {
		return (atom.elementNumber > 0 ? atom.elementNumber : JmolAdapter
				.getElementNumber(atom.getElementSymbol()));
	}

	
	public String getAtomName() {
		return atom.atomName;
	}

	
	public int getFormalCharge() {
		return atom.formalCharge;
	}

	
	public float getPartialCharge() {
		return atom.partialCharge;
	}

	
	public JmolList<Tensor> getTensors() {
		return atom.tensors;
	}

	
	public float getRadius() {
		return atom.radius;
	}
	
	public V3 getVib() {
	  return (atom.vib == null || Float.isNaN(atom.vib.z) ? null : atom.vib);
	}

	
	public float getBfactor() {
		return Float.isNaN(atom.bfactor) && atom.anisoBorU != null ? atom.anisoBorU[7] * 100f
				: atom.bfactor;
	}

	
	public int getOccupancy() {
		return (int) (atom.foccupancy * 100);
	}

	
	public boolean getIsHetero() {
		return atom.isHetero;
	}

	
	public int getAtomSerial() {
		return atom.atomSerial;
	}

	
	public int getChainID() {
		return atom.chainID;
	}

	
	public char getAlternateLocationID() {
		return JmolAdapter.canonizeAlternateLocationID(atom.alternateLocationID);
	}

	
	public String getGroup3() {
		return atom.group3;
	}

	
	public int getSequenceNumber() {
		return atom.sequenceNumber;
	}

	
	public char getInsertionCode() {
		return JmolAdapter.canonizeInsertionCode(atom.insertionCode);
	}

	
	public P3 getXYZ() {
		return atom;
	}

}
