/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
import org.openscience.jmol.Atom;
import freeware.PrintfFormat;
import javax.vecmath.Point3d;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves molecules in a rudimentary PDB format.
 */
public class PdbSaver extends FileSaver {

  /**
   * Creates a PDB file saver.
   *
   * @param cf the ChemFile to dump.
   * @param out the stream to write the file to.
   */
  public PdbSaver(ChemFile cf, OutputStream out) throws IOException {
    super(cf, out);
  }

  public void writeFileStart(ChemFile cf, BufferedWriter w)
      throws IOException {
    atomNumber = 1;

    // No preamble for PDB files
  }

  public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException {

    // No postamble for PDB files
  }

  /**
   * The number of the next atom to be written. Used to give each
   * atom a unique serial number.
   */
  int atomNumber = 1;

  /**
   * Writes a single frame in PDB format to the Writer.
   *
   * @param cf the ChemFrame to write
   * @param w the Writer to write it to
   */
  public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {

    int na = 0;
    String info = "";
    String st = "";
    String tab = "\t";
    boolean writecharge = false;
    boolean writevect = false;

    String hetatmRecordName = "HETATM";
    String terRecordName = "TER";
    PrintfFormat serialFormat = new PrintfFormat("%5d");
    PrintfFormat atomNameFormat = new PrintfFormat("%-4s");
    PrintfFormat positionFormat = new PrintfFormat("%8.3f");

    // Loop through the atoms and write them out:
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < cf.getNumberOfAtoms(); i++) {
      buffer.setLength(0);
      buffer.append(hetatmRecordName);
      buffer.append(serialFormat.sprintf(atomNumber));
      buffer.append(' ');
      Atom atom = cf.getAtomAt(i);
      buffer.append(atomNameFormat.sprintf(atom.getID()));
      buffer.append(" MOL          ");
      Point3d position = atom.getPosition();
      buffer.append(positionFormat.sprintf(position.x));
      buffer.append(positionFormat.sprintf(position.y));
      buffer.append(positionFormat.sprintf(position.z));

      w.write(buffer.toString(), 0, buffer.length());
      w.newLine();
      ++atomNumber;
    }
    w.write(terRecordName, 0, terRecordName.length());
    w.newLine();
  }
}
