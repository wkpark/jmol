
/*
 * Copyright 2002 The Jmol Development Team
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
import org.openscience.jmol.FortranFormat;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;


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
 * @author Bradley A. Smith (bradley@baysmith.com)
 * @author Egon Willighagen (egonw@sci.kun.nl)
 */
public class PDBReader extends DefaultChemFileReader {

  /**
   * Creates a PDB file reader.
   *
   * @param input source of PDB data
   */
  public PDBReader(Reader input) {
    super(input);
  }

  /**
   * Read the PDB data.
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = new ChemFrame();
    ChemFrame.setAutoBond(false);
    boolean bondsCleared = false;
    StringTokenizer st;

    String line = input.readLine();
    while (input.ready() && (line != null)) {
      st = new StringTokenizer(line, "\t ,;");

      String command;

      try {
        command = line.substring(0, 6).trim();
      } catch (StringIndexOutOfBoundsException sioobe) {
        break;
      }

      if (command.equalsIgnoreCase("ATOM")
          || command.equalsIgnoreCase("HETATM")) {

        // First two columns of atom name are the element.
        String atype = "";
        if (Character.isDigit(line.charAt(12))) {
          atype = line.substring(13, 14);
        } else {
          atype = line.substring(12, 14).trim();
        }
        
        String sx = line.substring(30, 38).trim();
        String sy = line.substring(38, 46).trim();
        String sz = line.substring(46, 54).trim();

        double x = FortranFormat.atof(sx);
        double y = FortranFormat.atof(sy);
        double z = FortranFormat.atof(sz);
        frame.addAtom(atype, (float) x, (float) y, (float) z);
/*
 * The following code for processing CONECT records has several defects.
 * 1. It uses a StringTokenizer to parse the CONECT record. This is
 *   incorrect because PDB records are fixed field widths. There may
 *   be no whitespace between fields.
 * 2. It assumes that the atom serial numbers are equal to the atom
 *   index - 1. Atom serial numbers are not necessarily sequential, and
 *   do not necessarily start with 1.
 *
 * Once the defects have been corrected, the functionality will be
 * reintroduced.
 *
      } else if (command.equalsIgnoreCase("CONECT")) {

        // read connectivity information
        if (!bondsCleared) {
          frame.clearBonds();
          bondsCleared = true;
        }

        String atom = line.substring(6, 11).trim();
        String connectedAtoms = line.substring(12).trim();
        StringTokenizer linet = new StringTokenizer(connectedAtoms);

        // Though not mentioned in PDB's specifications, adhere to Rasmol's
        // custom to define double/triple bonds by mentioning bonds with
        // double bonded atoms twice or with triple bonded atoms three times.
        //
        // E.g.: CONECT  3 4 4
        // would be a double bond between atoms 3 and 4
        //
        Hashtable tmp = new Hashtable(10);
        while (linet.hasMoreTokens()) {
          String connectedAtom = linet.nextToken();
          Integer neighbour = null;
          try {
            neighbour = new Integer(connectedAtom);
          } catch (NumberFormatException ex) {
            // If a token is not an integer, skip remaining tokens.
            break;
          }
          if (tmp.containsKey(neighbour)) {
            tmp.put(neighbour,
                new Integer(((Integer) tmp.get(neighbour)).intValue() + 1));
          } else {
            tmp.put(neighbour, new Integer(1));
          }
        }
        Enumeration neighbours = tmp.keys();
        int atomi = Integer.parseInt(atom);
        while (neighbours.hasMoreElements()) {
          Integer neighbour = (Integer) neighbours.nextElement();
          int atomj = neighbour.intValue();
          int bondorder = ((Integer) tmp.get(neighbour)).intValue();
          frame.addBond(atomi - 1, atomj - 1, bondorder);
        }
*/
      } else if (command.equalsIgnoreCase("MODEL")) {
        frame.setInfo(line.trim());
      } else if (command.equalsIgnoreCase("ENDMDL")) {
        ChemFrame.setAutoBond(true);
        frame.rebond();
        file.addFrame(frame);
        fireFrameRead();
        
        frame = new ChemFrame();
        ChemFrame.setAutoBond(false);
        
      } else if (command.equalsIgnoreCase("END")) {
        ChemFrame.setAutoBond(true);
        if (frame.getNumberOfAtoms() > 0) {
          frame.rebond();
          file.addFrame(frame);
          fireFrameRead();
        }
        return file;
      }

      line = input.readLine();
    }

    // No END marker, so just wrap things up as if we had seen one:
    ChemFrame.setAutoBond(true);
    if (frame.getNumberOfAtoms() > 0) {
      frame.rebond();
      file.addFrame(frame);
      fireFrameRead();
    }
    return file;
  }
}
