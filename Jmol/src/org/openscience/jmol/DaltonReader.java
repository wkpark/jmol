/*
 * Copyright (C) 2001  The Jmol Development Team
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
package org.openscience.jmol;

import java.io.*;
import java.util.*;

/**
 *  Reader for output from the Dalton electronic structure program.
 *
 *  <p>
 *   T. Helgaker, H.J.Aa. Jensen, P. Joergensen, J. Olsen,
 *   K. Ruud,H. Aagren, K.L. Bak, V. Bakken, O. Christiansen,P. Dahle,
 *   E.K. Dalskov, T. Enevoldsen, B. Fernandez,H. Heiberg, H. Hettema,
 *   D. Jonsson, S. Kirpekar, R. Kobayashi, H. Koch, K.V. Mikkelsen,
 *   P. Norman, M.J. Packer, T.A. Ruden, T. Saue, S.P.A. Sauer,
 *   K.O. Sylvester-Hvid, P.R. Taylor, and O. Vahtras: "Dalton release
 *   1.1 (2000), an electronic structure program"
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
class DaltonReader implements ChemFileReader {

	static final double angstromPerBohr = 0.529177249;
	
	BufferedReader input;
	Hashtable atomTypeMap = new Hashtable();

	public DaltonReader(Reader input) {
		this.input = new BufferedReader(input);
	}
	
	public ChemFile read() throws Exception {
		ChemFile file = new ChemFile();
		ChemFrame frame = null;
		
		// Find energy
		String line;
		while (input.ready()) {
			line = input.readLine();
			if (line.trim().startsWith("Total energy")) {
				frame.setInfo(line.trim());
			} else if (line.trim().startsWith("Atoms and basis sets")) {
				readAtomTypes();
			} else if (line.trim().startsWith("Molecular geometry")) {
				if (frame != null) {
					file.frames.addElement(frame);
				}
				frame = new ChemFrame();
				readCoordinates(frame);
			} else if (line.trim().startsWith("Normal Coordinates")) {
				readFrequencies(frame);
				break;
			}
		}
		// Add current frame to file
		file.frames.addElement(frame);
		
		return file;
	}
	

	void readAtomTypes() throws Exception {
		atomTypeMap.clear();
		
		String line;
		for (int i=0; i < 9; ++i) {
			line = input.readLine();
		}
		while (input.ready()) {
			line = input.readLine().trim();
			if (line.startsWith("-----")) {
				break;
			}
			StringTokenizer tokenizer = new StringTokenizer(line);
			if (tokenizer.countTokens() < 3) {
				throw new Exception("Error reading atom types");
			}
			String label = tokenizer.nextToken();
			tokenizer.nextToken();
			Integer atomicNumber = new Integer(tokenizer.nextToken());
			atomTypeMap.put(label, atomicNumber);
		}
	}
	
	/**
	 * Reads a set of coordinates into ChemFrame.
	 *
	 * @param frame  the destination ChemFrame
	 * @exception IOException  if an I/O error occurs
	 */
	void readCoordinates(ChemFrame mol) throws IOException, Exception {
		String line;
		line = input.readLine();
		line = input.readLine();
		while (input.ready()) {
			line = input.readLine().trim();
			if (line.length() == 0) {
				break;
			}
			int atomicNumber;
			double x;
			double y;
			double z;
			StringTokenizer tokenizer = new StringTokenizer(line);
			if (tokenizer.countTokens() < 3) {
				throw new Exception("Error reading coordinates");
			}

			String label = tokenizer.nextToken();
			if (atomTypeMap.containsKey(label)) {
				atomicNumber = ((Integer)atomTypeMap.get(label)).intValue();
			} else {
				// Unrecognized atom type
				atomicNumber = -1;
			}
			x = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;
			y = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;
			z = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;

			mol.addVert(atomicNumber, (float)x, (float)y, (float)z);
		}
	}
	
	/**
	 * Reads a set of vibrations into ChemFrame.
	 *
	 * @param frame  the destination ChemFrame
	 * @exception IOException  if an I/O error occurs
	 */
	void readFrequencies(ChemFrame mol) throws Exception {
		String line;
		line = input.readLine();
		line = input.readLine();
		line = input.readLine();

		while (input.ready()) {
			String headerLine = input.readLine().trim();
			line = input.readLine().trim();
			if (!line.startsWith("-----")) {
				break;
			}

			Vector currentFreqs = new Vector();
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			int numberOfCurrentFrequencies = tokenizer.countTokens()/2;
			for (int i=0; i < numberOfCurrentFrequencies; ++i) {
				tokenizer.nextToken();
				String value = tokenizer.nextToken();
				Vibration freq = new Vibration(value);
				currentFreqs.addElement(freq);
			}
			Object[] currentVectors = new Object[currentFreqs.size()];
			
			line = input.readLine();

			for (int i=0; i < mol.getNvert(); ++i) {
				line = input.readLine();
				tokenizer = new StringTokenizer(line);
				tokenizer.nextToken();  // ignore first token
				tokenizer.nextToken();  // ignore second token
				for (int j=0; j < currentFreqs.size(); ++j) {
					currentVectors[j] = new double[3];
					((double[])currentVectors[j])[0] = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;
				}

				line = input.readLine();
				tokenizer = new StringTokenizer(line);
				tokenizer.nextToken();  // ignore first token
				tokenizer.nextToken();  // ignore second token
				for (int j=0; j < currentFreqs.size(); ++j) {
					((double[])currentVectors[j])[1] = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;
				}

				line = input.readLine();
				tokenizer = new StringTokenizer(line);
				tokenizer.nextToken();  // ignore first token
				tokenizer.nextToken();  // ignore second token
				for (int j=0; j < currentFreqs.size(); ++j) {
					((double[])currentVectors[j])[2] = (new Double(tokenizer.nextToken())).doubleValue() * angstromPerBohr;
					((Vibration)currentFreqs.elementAt(j)).addAtomVector((double[])currentVectors[j]);
				}
				// Skip blank line between each atom
				line = input.readLine();
			}
			for (int i=0; i < currentFreqs.size(); ++i) {
				mol.addVibration((Vibration)currentFreqs.elementAt(i));
			}

			// Skip blank line between frequency sets
			line = input.readLine();
		}
	}
}

