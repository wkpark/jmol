
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
import java.util.StringTokenizer;


/**
 * A reader for PDB (Protein Data Bank) files.
 *
 * <p>PDB files are a very widely used method of communicating
 * structural information about biomolecules.  The column position of a
 * field within a given line governs how that field is interpreted.
 *
 * <p>Only the END, ATOM and HETATM command strings are processed for
 * now, and the ATOM and HETATM entries are used only for coordinate
 * information.  We would, of course, gladly accept code donations
 * that parse more of the detailed information contained within PDB
 * files.
 *
 * <p>A full specification of the PDB format is available at:
 *    http://www.rcsb.org/pdb/docs/format/pdbguide2.2/guide2.2_frame.html
 *
 * <p>PDB files also contain only a single frame.
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Brookhaven National Labs or the Research Collaboratory
 * for Structural Bioinformatics.  If you have problems, please
 * contact the author of this code, not the operators of the Protein
 * Data Bank.
 *
 * @author J. Daniel Gezelter (gezelter.1@nd.edu)
 * @version 1.0
 */
public class PDBReader implements ChemFileReader {

  /**
   * Create an PDB file reader.
   *
   * @param input source of PDB data
   */
  public PDBReader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Read the PDB output.
   *
   * @return a ChemFile with the coordinates, charges, vectors, etc.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException, Exception {

    ChemFile file = new ChemFile();
    ChemFrame frame = new ChemFrame();
    StringTokenizer st;

    String line = input.readLine();
    while (input.ready() && (line != null)) {
      st = new StringTokenizer(line, "\t ,;");

      String command;

      try {
        command = new String(line.substring(0, 6).trim());
      } catch (StringIndexOutOfBoundsException sioobe) {
        break;
      }

      if (command.equalsIgnoreCase("ATOM")
              || command.equalsIgnoreCase("HETATM")) {

        String atype = new String(line.substring(13, 14).trim());
        String sx = new String(line.substring(29, 38).trim());
        String sy = new String(line.substring(38, 46).trim());
        String sz = new String(line.substring(46, 54).trim());

        double x = FortranFormat.atof(sx);
        double y = FortranFormat.atof(sy);
        double z = FortranFormat.atof(sz);
        frame.addVert(atype, (float) x, (float) y, (float) z);
      }

      if (command.equalsIgnoreCase("END")) {
        file.frames.addElement(frame);
        return file;
      }

      line = input.readLine();
    }

    // No END marker, so just wrap things up as if we had seen one:
    file.frames.addElement(frame);
    return file;
  }

  private BufferedReader input;
}
