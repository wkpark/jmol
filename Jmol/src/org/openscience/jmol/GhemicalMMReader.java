
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Reads Ghemical (http://www.uku.fi/~thassine/ghemical/)
 * molecular mechanics (*.mm1gp) files.
 *
 * @author Egon Willighagen (egonw@sci.kun.nl)
 */
public class GhemicalMMReader extends DefaultChemFileReader {

  public GhemicalMMReader(Reader input) {
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
   * Parses the file into a ChemFrame.
   */
  public ChemFrame readFrame() throws IOException {

    int[] atoms = new int[1];
    float[] atomxs = new float[1];
    float[] atomys = new float[1];
    float[] atomzs = new float[1];
    float[] atomcharges = new float[1];

    int[] bondatomid1 = new int[1];
    int[] bondatomid2 = new int[1];
    int[] bondorder = new int[1];

    int numberOfAtoms = 0;
    int numberOfBonds = 0;

    ChemFrame frame = new ChemFrame(bondsEnabled);

    String line = input.readLine();
    while (line != null) {
      StringTokenizer st = new StringTokenizer(line);
      String command = st.nextToken();
      if ("!Header".equals(command)) {
      } else if ("!Info".equals(command)) {
      } else if ("!Atoms".equals(command)) {

        // determine number of atoms to read
        numberOfAtoms = Integer.parseInt(st.nextToken());
        atoms = new int[numberOfAtoms];
        atomxs = new float[numberOfAtoms];
        atomys = new float[numberOfAtoms];
        atomzs = new float[numberOfAtoms];

        for (int i = 0; i < numberOfAtoms; i++) {
          line = input.readLine();
          StringTokenizer atomInfoFields = new StringTokenizer(line);
          int atomID = Integer.parseInt(atomInfoFields.nextToken());
          atoms[atomID] = Integer.parseInt(atomInfoFields.nextToken());
        }
      } else if ("!Bonds".equals(command)) {

        // determine number of bonds to read
        numberOfBonds = Integer.parseInt(st.nextToken());
        bondatomid1 = new int[numberOfAtoms];
        bondatomid2 = new int[numberOfAtoms];
        bondorder = new int[numberOfAtoms];

        for (int i = 0; i < numberOfBonds; i++) {
          line = input.readLine();
          StringTokenizer bondInfoFields = new StringTokenizer(line);
          bondatomid1[i] = Integer.parseInt(bondInfoFields.nextToken());
          bondatomid2[i] = Integer.parseInt(bondInfoFields.nextToken());
          String order = bondInfoFields.nextToken();
          if ("D".equals(order)) {
            bondorder[i] = 2;
          } else if ("S".equals(order)) {
            bondorder[i] = 1;
          } else if ("T".equals(order)) {
            bondorder[i] = 3;
          } else {

            // ignore order, i.e. set to single
            bondorder[i] = 1;
          }
        }
      } else if ("!Coord".equals(command)) {
        for (int i = 0; i < numberOfAtoms; i++) {
          line = input.readLine();
          StringTokenizer atomInfoFields = new StringTokenizer(line);
          int atomID = Integer.parseInt(atomInfoFields.nextToken());
          float x = Float.valueOf(atomInfoFields.nextToken()).floatValue();
          float y = Float.valueOf(atomInfoFields.nextToken()).floatValue();
          float z = Float.valueOf(atomInfoFields.nextToken()).floatValue();
          atomxs[atomID] = x * (float) 10;    // convert to Angstrom
          atomys[atomID] = y * (float) 10;
          atomzs[atomID] = z * (float) 10;
        }
      } else if ("!Charges".equals(command)) {
      } else if ("!End".equals(command)) {

        // Store atoms
        for (int i = 0; i < numberOfAtoms; i++) {
          frame.addAtom(atoms[i], atomxs[i], atomys[i], atomzs[i]);
        }

        // Store bonds
        frame.clearBonds();
        for (int i = 0; i < numberOfBonds; i++) {
          frame.addBond(bondatomid1[i], bondatomid2[i], bondorder[i]);
        }

        fireFrameRead();
        return frame;
      } else {

        // disregard this line
      }

      line = input.readLine();
    }

    // this should not happen, file is lacking !End command
    return null;
  }
}
