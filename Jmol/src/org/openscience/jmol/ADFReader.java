
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
import java.util.Hashtable;

/**
 * A reader for ADF output.
 * Amsterdam Density Functional (ADF) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all ADF output. If you have problems,
 * please contact the author of this code, not the developers
 * of ADF.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class ADFReader implements ChemFileReader {

  /**
   * Create an ADF output reader.
   *
   * @param input source of ADF data
   */
  public ADFReader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Read the ADF output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException, Exception {

    ChemFile file = new ChemFile();
    ChemFrame frame = null;
    String line = input.readLine();

    // Find first set of coordinates
    while (input.ready() && (line != null)) {
      if (line.indexOf("Coordinates (Cartesian)") >= 0) {
        frame = new ChemFrame();
        readCoordinates(frame);
        break;
      }
      line = input.readLine();
    }
    if (frame != null) {
      line = input.readLine();
      while (input.ready() && (line != null)) {
        if (line.indexOf("Coordinates (Cartesian)") >= 0) {

          // Found set of coordinates
          // Add current frame to file and create a new one.
          file.frames.addElement(frame);
          frame = new ChemFrame();
          readCoordinates(frame);
        } else if (line.indexOf("Energy:") >= 0) {

          // Found an energy
          frame.setInfo(line.trim());
        } else if (line.indexOf("Vibrations") >= 0) {

          // Found a set of vibrations
          readFrequencies(frame);
        }
        line = input.readLine();
      }

      // Add current frame to file
      file.frames.addElement(frame);
    }
    return file;
  }

  /**
   * Reads a set of coordinates into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readCoordinates(ChemFrame frame)
          throws IOException, Exception {

    String line;
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    while (input.ready()) {
      line = input.readLine();
      if ((line == null) || (line.indexOf("-----") > 0)) {
        break;
      }
      int atomicNumber;
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);
      token.nextToken();

      // ignore first token
      if (token.nextToken() == StreamTokenizer.TT_WORD) {
        atomicNumber = atomLabelToAtomNumber(token.sval);
        if (atomicNumber == 0) {

          // skip dummy atoms
          continue;
        }
      } else {
        throw new IOException("Error reading coordinates");
      }

      // Ignore coordinates in bohr
      token.nextToken();
      token.nextToken();
      token.nextToken();

      // Read coordinates in angstroms
      double x;
      double y;
      double z;
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        x = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        y = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        z = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      frame.addVert(atomicNumber, (float) x, (float) y, (float) z);
    }
  }

  /**
   * Reads a set of vibrations into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readFrequencies(ChemFrame frame)
          throws IOException, Exception {

    String line;
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    while (line.indexOf('.') > 0) {
      Vector currentVibs = new Vector();
      StringReader freqValRead = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(freqValRead);
      while (token.nextToken() != StreamTokenizer.TT_EOF) {
        Vibration freq = new Vibration(Double.toString(token.nval));
        currentVibs.addElement(freq);
      }
      line = input.readLine();
      for (int i = 0; i < frame.getNvert(); ++i) {
        line = input.readLine();
        StringReader vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);
        token.nextToken();

        // ignore first token
        if (token.nextToken() == StreamTokenizer.TT_WORD) {
          int atomicNumber = atomLabelToAtomNumber(token.sval);
          if (atomicNumber == 0) {

            // skip dummy atoms
            --i;
            continue;
          }
        } else {
          throw new IOException("Error reading frequencies");
        }
        for (int j = 0; j < currentVibs.size(); ++j) {
          double[] v = new double[3];
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[0] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[1] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[2] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          ((Vibration) currentVibs.elementAt(j)).addAtomVector(v);
        }
      }
      for (int i = 0; i < currentVibs.size(); ++i) {
        frame.addVibration((Vibration) currentVibs.elementAt(i));
      }
      line = input.readLine();
      line = input.readLine();
      line = input.readLine();
    }
  }

  /**
   * Maps atomic symbol to atomic number.
   *
   * @return  atomic number
   */
  private static int atomLabelToAtomNumber(String label) {

    int number = 0;
    if (atomicSymbols.containsKey(label)) {
      number = ((Integer) atomicSymbols.get(label)).intValue();
    }
    return number;
  }

  /**
   * The source for ADF data.
   */
  private BufferedReader input;

  /**
   * Table for mapping atomic symbols to atomic numbers.
   * Keys are atomic symbols represented by String.
   * Values are atomic numbers represented by Integer.
   */
  private static Hashtable atomicSymbols = new Hashtable();

  /**
   * Initialize atomic symbol to atomic number map.
   */
  static {
    atomicSymbols.put(new String("XX"), new Integer(0));
    atomicSymbols.put("H", new Integer(1));
    atomicSymbols.put("He", new Integer(2));
    atomicSymbols.put("Li", new Integer(3));
    atomicSymbols.put("Be", new Integer(4));
    atomicSymbols.put("B", new Integer(5));
    atomicSymbols.put("C", new Integer(6));
    atomicSymbols.put("N", new Integer(7));
    atomicSymbols.put("O", new Integer(8));
    atomicSymbols.put("F", new Integer(9));
    atomicSymbols.put("Ne", new Integer(10));
    atomicSymbols.put("Na", new Integer(11));
    atomicSymbols.put("Mg", new Integer(12));
    atomicSymbols.put("Al", new Integer(13));
    atomicSymbols.put("Si", new Integer(14));
    atomicSymbols.put("P", new Integer(15));
    atomicSymbols.put("S", new Integer(16));
    atomicSymbols.put("Cl", new Integer(17));
    atomicSymbols.put("Ar", new Integer(18));
    atomicSymbols.put("K", new Integer(19));
    atomicSymbols.put("Ca", new Integer(20));
    atomicSymbols.put("Sc", new Integer(21));
    atomicSymbols.put("Ti", new Integer(22));
    atomicSymbols.put("V", new Integer(23));
    atomicSymbols.put("Cr", new Integer(24));
    atomicSymbols.put("Mn", new Integer(25));
    atomicSymbols.put("Fe", new Integer(26));
    atomicSymbols.put("Co", new Integer(27));
    atomicSymbols.put("Ni", new Integer(28));
    atomicSymbols.put("Cu", new Integer(29));
    atomicSymbols.put("Zn", new Integer(30));
    atomicSymbols.put("Ga", new Integer(31));
    atomicSymbols.put("Ge", new Integer(32));
    atomicSymbols.put("As", new Integer(33));
    atomicSymbols.put("Se", new Integer(34));
    atomicSymbols.put("Br", new Integer(35));
    atomicSymbols.put("Kr", new Integer(36));
  }
}
