
/*
 * @(#)XYZReader.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
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
import java.util.Enumeration;
import java.util.StringTokenizer;
import org.openscience.jmol.FortranFormat;

/**
 * XYZ files may contain multiple ChemFrame objects, and may have charges
 * and vector information contained along with atom types and coordinates.
 * XYZ files <em>must</em> have a number of atoms at the beginning of each
 * line then another line (which may be blank) to identify each frame.
 */
public class XYZReader implements ChemFileReader {

	/**
	 * Creates an XYZ file reader.
	 *
	 * @param input source of XYZ data
	 */
	public XYZReader(Reader input) {
		this.input = new BufferedReader(input, 1024);
	}

	/**
	 * Read the XYZ file.
	 */
	public ChemFile read(StatusDisplay putStatus, boolean bondsEnabled)
			throws IOException {

		int fr = 0;
		ChemFile file = new ChemFile(bondsEnabled);

		while (true) {
			fr++;
			ChemFrame cf = readFrame(putStatus, fr, bondsEnabled);
			if (cf == null) {
				break;
			}
			file.addFrame(cf);
			Enumeration propIter = cf.getFrameProps().elements();
			while (propIter.hasMoreElements()) {
				file.addProperty((String) propIter.nextElement());
			}
		}
		return file;
	}

	/**
	 * Parses the next section of the XYZ file into a ChemFrame.
	 */
	public ChemFrame readFrame(
			StatusDisplay putStatus, int frameNum, boolean bondsEnabled)
				throws IOException {

		int na = 0;
		String info = "";
		StringBuffer stat = new StringBuffer();
		String statBase = null;

		String l = input.readLine();
		if (l == null) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(l, "\t ,;");
		String sn = st.nextToken();
		na = Integer.parseInt(sn);
		info = input.readLine();
		if (putStatus != null) {
			stat.append("Reading Frame ");
			stat.append(frameNum);
			stat.append(": ");
			statBase = stat.toString();
			stat.append("0 %");
			putStatus.setStatusMessage(stat.toString());
		}

		// OK, we got enough to start building a ChemFrame:
		ChemFrame cf = new ChemFrame(na, bondsEnabled);
		cf.setInfo(info);

		String s;		// temporary variable used to store data as we read it

		for (int i = 0; i < na; i++) {
			s = input.readLine();
			if (s == null) {
				break;
			}
			if (!s.startsWith("#")) {
				double x = 0.0f, y = 0.0f, z = 0.0f, c = 0.0f;
				double vect[] = new double[3];
				st = new StringTokenizer(s, "\t ,;");
				boolean readcharge = false;
				boolean readvect = false;
				int nt = st.countTokens();
				switch (nt) {
				case 1 :
				case 2 :
				case 3 :
					throw new IOException(
							"XYZFile.readFrame(): Not enough fields on line.");

				case 5 :	// atype, x, y, z, charge                    
					readcharge = true;
					break;

				case 7 :	// atype, x, y, z, vx, vy, vz
					readvect = true;
					break;

				case 8 :	// atype, x, y, z, charge, vx, vy, vz
					readcharge = true;
					readvect = true;
					break;

				default :		// 4, 6, or > 8  fields, just read atype, x, y, z
					break;
				}

				String aname = st.nextToken();
				String sx = st.nextToken();
				String sy = st.nextToken();
				String sz = st.nextToken();
				x = FortranFormat.atof(sx);
				y = FortranFormat.atof(sy);
				z = FortranFormat.atof(sz);

				Vector props = new Vector();
				if (readcharge) {
					String sc = st.nextToken();
					c = FortranFormat.atof(sc);
				}

				if (readvect) {
					String svx = st.nextToken();
					String svy = st.nextToken();
					String svz = st.nextToken();
					vect[0] = FortranFormat.atof(svx);
					vect[1] = FortranFormat.atof(svy);
					vect[2] = FortranFormat.atof(svz);
				}

				if (readcharge || readvect) {
					cf.addAtom(aname, (float) x, (float) y, (float) z, props);
				} else {
					cf.addAtom(aname, (float) x, (float) y, (float) z);
				}
			}
			if (putStatus != null) {
				stat.setLength(0);
				stat.append(statBase);
				if (na > 1) {
					stat.append((int) (100 * i / (na - 1)));
				} else {
					stat.append(100);
				}
				stat.append(" %");
				putStatus.setStatusMessage(stat.toString());
			}
		}
		return cf;
	}

	private BufferedReader input;
}
