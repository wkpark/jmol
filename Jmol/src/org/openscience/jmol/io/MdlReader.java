/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.io;

import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.cdk.Atom;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads MDL CTfile format files.
 * For information about the format, see http://www.mdli.com/.
 *
 * <h3>Current limitations</h3>
 * <ul>
 *   <li>Only molfiles can be read. Not SDfiles, RXNfiles, RDfiles, etc.
 *   <li>All information except the molecule name, atom types, coordinates, and
 *     bonds (not bond  orders) is ignored.
 * <ul>
 *
 * @author Jochen Junker
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class MdlReader extends DefaultChemFileReader {

  /**
   * Creates an MDL file reader.
   *
   * @param input source of data
   */
  public MdlReader(Reader input) {
    super(input);
  }

  /**
   * Read the MDL file.
   *
   * @return a ChemFile with the coordinates
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = readFrame();
    file.addFrame(frame);
    return file;
  }

  /**
   * Parses the MOL file into a ChemFrame.
   */
  public ChemFrame readFrame() throws IOException {

    ChemFrame frame = new ChemFrame(bondsEnabled);

    // Read the molecule name
    String line = input.readLine();
    if (line != null) {
      frame.setInfo(line.trim());
    }

    // Ignore the next two lines.
    line = input.readLine();
    line = input.readLine();

    // Read counts line
    line = input.readLine();
    if (line == null) {
      return null;
    }

    int numberOfAtoms = Integer.parseInt(line.substring(0, 3).trim());
    int numberOfBonds = Integer.parseInt(line.substring(3, 6).trim());

    // Read atoms
    for (int i = 0; i < numberOfAtoms; i++) {
      line = input.readLine();
      if (line == null) {
        break;
      }

      double x = Double.valueOf(line.substring(0, 10).trim()).doubleValue();
      double y = Double.valueOf(line.substring(10, 20).trim()).doubleValue();
      double z = Double.valueOf(line.substring(20, 30).trim()).doubleValue();
      String atomSymbol = line.substring(31, 34).trim();

      Atom atom = new Atom(atomSymbol);
      atom.setX3D(x);
      atom.setY3D(y);
      atom.setZ3D(z);
      frame.addAtom(atom);
    }

    // Read bonds
    frame.clearBonds();
    for (int i = 0; i < numberOfBonds; i++) {
      line = input.readLine();
      if (line == null) {
        break;
      }
      int atom0 = Integer.parseInt(line.substring(0, 3).trim());
      int atom1 = Integer.parseInt(line.substring(3, 6).trim());
      int bondOrder = Integer.parseInt(line.substring(6, 9).trim());
      frame.addBond(atom0 - 1, atom1 - 1, bondOrder);
    }

    fireFrameRead();
    return frame;
  }
}
