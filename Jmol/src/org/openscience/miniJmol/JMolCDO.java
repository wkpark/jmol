
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

import org.openscience.cdopi.ANIMATIONCDO;
import java.util.Vector;
import org.openscience.jmol.FortranFormat;

public final class JMolCDO extends ANIMATIONCDO {

	private Vector allFrames;
	private ChemFrame currentFrame;
	private int frameNo;

	private String atomType;
	private String atomX;
	private String atomY;
	private String atomZ;

	public JMolCDO() {
		allFrames = new Vector();
		currentFrame = new ChemFrame();
		frameNo = 0;
	}

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startObject(String type) {

		if (type.equals("Atom")) {
			this.startAtom();
		} else if (type.equals("Molecule")) {
			this.startMolecule();
		} else if (type.equals("Fragment")) {
			this.startFragment();
		} else if (type.equals("Bond")) {
			this.startBond();
		} else if (type.equals("Animation")) {
			this.startAnimation();
		} else if (type.equals("Frame")) {
			this.startFrame();
		} else {
			System.err.println(
					"DEBUG: unknown CDO Object Type at StartObject -> "
						+ type);
		}
	}

	public void endObject(String type) {

		if (type.equals("Atom")) {
			this.endAtom();
		} else if (type.equals("Molecule")) {
			this.endMolecule();
		} else if (type.equals("Fragment")) {
			this.endFragment();
		} else if (type.equals("Bond")) {
			this.endBond();
		} else if (type.equals("Animation")) {
			this.endAnimation();
		} else if (type.equals("Frame")) {
			this.endFrame();
		} else {
			System.err.println(
					"DEBUG: unknown CDO Object Type at EndObject -> " + type);
		}
	}

	public void setObjectProperty(String type, String proptype,
			String propvalue) {

		if (type.equals("Atom")) {
			this.setAtomProperty(proptype, propvalue);
		} else if (type.equals("Molecule")) {
			this.setMoleculeProperty(proptype, propvalue);
		} else if (type.equals("Fragment")) {
			this.setFragmentProperty(proptype, propvalue);
		} else if (type.equals("Bond")) {
			this.setBondProperty(proptype, propvalue);
		} else if (type.equals("Animation")) {
			this.setAnimationProperty(proptype, propvalue);
		} else if (type.equals("Frame")) {
			this.setFrameProperty(proptype, propvalue);
		} else {
			System.err.println(
					"DEBUG: unknown CDO Object Type at SetObjectProperty -> "
						+ type);
		}
	}

	public void startAnimation() {
		System.out.println("startAnimation");
	}

	public void endAnimation() {
	}

	public void startFrame() {
		System.out.println("startFrame");
		frameNo++;
		currentFrame = new ChemFrame();
	}

	public void endFrame() {
		System.out.println("endFrame");
		allFrames.addElement(currentFrame);
	}

	public void setFrameProperty(String type, String value) {
		System.out.println("setFrameProperty: " + type + "=" + value);
		if (type.equals("title")) {
			currentFrame.setInfo(value);
		}
	}

	public void startMolecule() {
	}

	public void endMolecule() {
	}

	public void startFragment() {
	}

	public void endFragment() {
	}

	public void startAtom() {

		System.out.println("startAtom");
		atomType = "";
		atomX = "";
		atomY = "";
		atomZ = "";
	}

	public void endAtom() {

		System.out.println("endAtom: " + atomType + " " + atomX + " " + atomY
				+ " " + atomZ);
		double x = FortranFormat.atof(atomX.trim());
		double y = FortranFormat.atof(atomY.trim());
		double z = FortranFormat.atof(atomZ.trim());
		try {
			currentFrame.addAtom(atomType.trim(), (float) x, (float) y,
					(float) z);
		} catch (Exception e) {
			System.out.println("JMolCDO error while adding atom: " + e);
		}
	}

	public void setAtomProperty(String type, String value) {

		System.out.println("setAtomProp: " + type + "=" + value);
		if (type.equals("type")) {
			atomType = value;
		}
		if (type.equals("x3")) {
			atomX = value;
		}
		if (type.equals("y3")) {
			atomY = value;
		}
		if (type.equals("z3")) {
			atomZ = value;
		}
	}

	public Vector returnChemFrames() {
		return allFrames;
	}

	public void setDocumentProperty(String type, String value) {
	}

	public void setMoleculeProperty(String type, String value) {
	}

	public void setFragmentProperty(String type, String value) {
	}

	public void setAnimationProperty(String type, String value) {
	}

	public void setBondProperty(String type, String value) {
	}

	public void startBond() {
	}

	public void endBond() {
	}
}
