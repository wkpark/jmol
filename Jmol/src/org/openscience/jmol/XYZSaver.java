
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

import java.util.Vector;
import java.io.*;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class XYZSaver extends FileSaver {

	/**
	 * Constructor.
	 * @param cf the ChemFile to dump.
	 * @param out the stream to write the XYZ file to.
	 */
	public XYZSaver(ChemFile cf, OutputStream out) throws IOException {
		super(cf, out);
	}

	public void writeFileStart(ChemFile cf, BufferedWriter w)
			throws IOException {

		// No preamble for XYZ Files
	}

	public void writeFileEnd(ChemFile cf, BufferedWriter w)
			throws IOException {

		// No postamble for XYZ Files
	}

	/**
	 * writes a single frame in XYZ format to the Writer.
	 * @param cf the ChemFrame to write
	 * @param w the Writer to write it to
	 */
	public void writeFrame(ChemFrame cf, BufferedWriter w)
			throws IOException {

		int na = 0;
		String info = "";
		String st = "";
		String tab = "\t";
		boolean writecharge = false;
		boolean writevect = false;

		try {

			String s1 = new Integer(cf.getNvert()).toString() + "\n";
			w.write(s1, 0, s1.length());

			String s2 = cf.getInfo();
			if (s2 == null) {
				w.newLine();
			} else {
				w.write(s2 + "\n", 0, s2.length() + 1);
			}

			Vector fp = cf.getAtomProps();

			// Create some dummy properties:
			Charge c = new Charge(0.0);
			double[] vect = new double[3];
			vect[0] = 0.0;
			vect[1] = 0.0;
			vect[2] = 0.0;
			VProperty vp = new VProperty(vect);

			// test if we have charges or vectors in this frame:
			for (int i = 0; i < fp.size(); i++) {
				String prop = (String) fp.elementAt(i);
				if (prop.equals(c.getDescriptor())) {
					writecharge = true;
				}
				if (prop.equals(vp.getDescriptor())) {
					writevect = true;
				}
			}

			// Loop through the atoms and write them out:

			for (int i = 0; i < cf.getNvert(); i++) {

				AtomType a = cf.getAtomAt(i);
				st = a.getBaseAtomType().getName();

				double[] pos = cf.getVertCoords(i);
				st = st + tab + new Double(pos[0]).toString() + tab
						+ new Double(pos[1]).toString() + tab
							+ new Double(pos[2]).toString();

				Vector props = cf.getVertProps(i);

				if (writecharge) {
					for (int j = 0; j < props.size(); j++) {
						PhysicalProperty p =
							(PhysicalProperty) props.elementAt(j);
						String desc = p.getDescriptor();
						if (desc.equals(c.getDescriptor())) {
							Charge ct = (Charge) p;
							st = st + tab + ct.stringValue();
						}
					}
				}

				if (writevect) {
					for (int j = 0; j < props.size(); j++) {
						PhysicalProperty p =
							(PhysicalProperty) props.elementAt(j);
						String desc = p.getDescriptor();
						if (desc.equals(vp.getDescriptor())) {
							VProperty vt = (VProperty) p;
							double[] vtmp;
							vtmp = vt.getVector();
							st = st + tab + new Double(vtmp[0]).toString()
									+ tab + new Double(vtmp[1]).toString()
										+ tab + new Double(vtmp[2])
											.toString();
						}
					}
				}
				st = st + "\n";
				w.write(st, 0, st.length());

			}

		} catch (IOException e) {
			throw e;
		}

	}
}


