
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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import org.openscience.jmol.FortranFormat;

/**
 * PDB files contain a single ChemFrame object.  Only the END, ATOM and
 * HETATM command strings are processed for now, and the ATOM and HETATM
 * entries are only used for coordinate information.
 */
public class PDBReader implements ChemFileReader {

	/**
	 * Creates a PDB file reader.
	 *
	 * @param input source of PDB data
	 */
	public PDBReader(Reader input) {
		this.input = new BufferedReader(input, 1024);
	}

	/**
	 * Read the PDB data.
	 */
	public ChemFile read(StatusDisplay putStatus, boolean bondsEnabled)
			throws IOException {
		ChemFile file = new ChemFile(bondsEnabled);
		file.addFrame(readFrame(putStatus, bondsEnabled));
		return file;
	}

	/**
	 * Parses the PDB file into a ChemFrame.
	 */
	public ChemFrame readFrame(StatusDisplay putStatus, boolean bondsEnabled)
			throws IOException {

		ChemFrame cf = new ChemFrame(bondsEnabled);

		String s = null;
		StringBuffer stat = new StringBuffer();
		int statpos = 0;

		stat.append("Reading File: ");
		String baseStat = stat.toString();
		putStatus.setStatusMessage(stat.toString());

		while (true) {
			try {
				s = input.readLine();
			} catch (IOException ioe) {
				break;
			}
			if (s == null) {
				break;
			}

			String command = null;

			try {
				command = new String(s.substring(0, 6).trim());
			} catch (StringIndexOutOfBoundsException sioobe) {
				break;
			}

			if (command.equalsIgnoreCase("ATOM")
					|| command.equalsIgnoreCase("HETATM")) {

				String atype = new String(s.substring(13, 14).trim());
				String sx = new String(s.substring(29, 38).trim());
				String sy = new String(s.substring(38, 46).trim());
				String sz = new String(s.substring(46, 54).trim());

				double x = FortranFormat.atof(sx);
				double y = FortranFormat.atof(sy);
				double z = FortranFormat.atof(sz);
				cf.addAtom(atype, (float) x, (float) y, (float) z);
			}

			if (command.equalsIgnoreCase("END")) {
				return cf;
			}
			if (statpos > 10) {
				stat.setLength(0);
				stat.append(baseStat);
				statpos = 0;
			} else {
				stat.append(".");
			}
			putStatus.setStatusMessage(stat.toString());
		}

		return cf;
	}

	/**
	 * The source for PDB data.
	 */
	private BufferedReader input;
}
