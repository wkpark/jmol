
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
import java.util.Vector;

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
 */
public class PDBReader implements ChemFileReader {

  /**
   * Creates a PDB file reader.
   *
   * @param input source of PDB data
   */
  public PDBReader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Whether bonds are enabled in the files and frames read.
   */
  private boolean bondsEnabled = true;
  
  /**
   * Sets whether bonds are enabled in the files and frames which are read.
   *
   * @param bondsEnabled if true, enables bonds.
   */
  public void setBondsEnabled(boolean bondsEnabled) {
    this.bondsEnabled = bondsEnabled;
  }
  
  /**
   * Read the PDB data.
   */
  public ChemFile read() throws IOException {
    ChemFile file = new ChemFile(bondsEnabled);
    file.addFrame(readFrame());
    return file;
  }

  /**
   * Parses the PDB file into a ChemFrame.
   */
  public ChemFrame readFrame() throws IOException {

    ChemFrame cf = new ChemFrame(bondsEnabled);

    String s = null;

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

        String atype = new String(s.substring(12, 14).trim());
        String sx = new String(s.substring(30, 38).trim());
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
    }

    fireFrameRead();
    return cf;
  }
  
  /**
   * Holder of reader event listeners.
   */
  private Vector listenerList = new Vector();
  
  /**
   * An event to be sent to listeners. Lazily initialized.
   */
  private ReaderEvent readerEvent = null;
  
  /**
   * Adds a reader listener.
   *
   * @param l the reader listener to add.
   */
  public void addReaderListener(ReaderListener l) {
    listenerList.addElement(l);
  }
  
  /**
   * Removes a reader listener.
   *
   * @param l the reader listener to remove.
   */
  public void removeReaderListener(ReaderListener l) {
    listenerList.remove(l);
  }
  
  /**
   * Sends a frame read event to the reader listeners.
   */
  private void fireFrameRead() {
    for (int i = 0; i < listenerList.size(); ++i) {
      ReaderListener listener = (ReaderListener) listenerList.elementAt(i);
      // Lazily create the event:
      if (readerEvent == null) {
        readerEvent = new ReaderEvent(this);
      }
      listener.frameRead(readerEvent);
    }
  }
 
  /**
   * The source for PDB data.
   */
  private BufferedReader input;
}
