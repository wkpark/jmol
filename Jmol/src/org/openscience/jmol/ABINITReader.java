
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

import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.reflect.Array;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;

/**
 * Abinit summary (www.abinit.org):
 *   ABINIT is a package whose main program allows one to find the total
 *   energy, charge density and electronic structure of systems made of
 *   electrons and nuclei (molecules and periodic solids) within
 *   Density Functional Theory (DFT), using pseudopotentials and a
 *   planewave basis. ABINIT also includes options to optimize the
 *   geometry according to the DFT forces and stresses, or to perform
 *   molecular dynamics simulation using these forces, or to generate
 *   dynamical matrices, Born effective charges, and dielectric tensors.
 *
 *
 * <p> An abinit input file is composed of many keywords arranged
 * in a non-specific
 * order. Each keyword is followed by one or more numbers (integers or
 * floats depending of the keyword).
 * Characters following a '#' are ignored.
 * The fisrt line of the file can be considered as a title.
 * This implementaton supports only 1 dataset!!!
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 *<p><p> <p> Create an ABINIT input and output reader.
 *
 *
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.3 */
public class ABINITReader extends DefaultChemFileReader {

  // Factor conversion bohr to angstrom
  private static final float angstromPerBohr = 0.529177249f;

  // This variable will be used to parse the input file
  private StringTokenizer st;

  /**
   * Creates a new <code>ABINITReader</code> instance.
   *
   * @param input a <code>Reader</code> value
   */
  public ABINITReader(Reader input) {
    super(input);
  }


  /**
   * Read an ABINIT file. Automagically determines if it is
   * an abinit input or output file
   *
   * @return a ChemFile with the coordinates
   * @exception IOException if an error occurs
   */
  public ChemFile read() throws IOException {

    CrystalFile crystalFile = new CrystalFile();

    String line;



    /**
     * Check if it is an ABINIT *input* file or an abinit *output* file.
     * Actually we check if it is an abinit output file. If not, this is
     * an abinit input file
     */

    /**
     * The words "Version" and "ABINIT" must be found on the
     * same line and within the first three lines.
     */

    input.mark(1024);
    for (int i = 0; i < 3; i++) {
      if (input.ready()) {
        line = input.readLine();
        if ((line.indexOf("ABINIT") >= 0) && (line.indexOf("Version") >= 0)) {

          //This is an output file
          input.reset();
          System.out.println("We have an abinit *output* file");
          crystalFile = readAbinitOutput();
          return crystalFile;
        }
      }
    }

    //We don't we an output file so we have an input file
    input.reset();
    System.out.println("We have an abinit *input* file");
    crystalFile = readAbinitInput();
    return crystalFile;
  }


  /**
   * Read an ABINIT *input* file.
   * @return a <code>ChemFile</code> value
   * @exception IOException if an error occurs
   */
  public CrystalFile readAbinitInput() throws IOException {

    CrystalFile crystalFile = new CrystalFile();
    int natom = 1;
    int ntype = 1;
    float acell[] = new float[3];
    float[][] rprim = new float[3][3];
    String info = "";
    String sn;
    String line;

    info = input.readLine();
    System.out.println(info);



    //Check if this is a multidataset file. Multidataset is not yet supported
    input.mark(1024 * 1024);
    line = input.readLine();
    st = new StringTokenizer(line, " \t");
    sn = nextAbinitToken(input);

    while (sn != null) {
      if (sn.equals("ndtset")) {
        sn = nextAbinitToken(input);
        if (FortranFormat.atof(sn) > 1) {
          System.out.println("ABINITReader: multidataset not supported");
          return crystalFile;
        }
      }
      sn = nextAbinitToken(input);
    }
    input.reset();

    //First pass through the file (get variables of known dimension)
    input.mark(1024 * 1024);
    line = input.readLine();
    st = new StringTokenizer(line, " \t");
    sn = nextAbinitToken(input);

    while (sn != null) {
      if (sn.equals("acell")) {
        for (int i = 0; i < 3; i++) {

          sn = nextAbinitToken(input);
          int index = sn.indexOf("*");
          if (index >= 0)    //Test if number format of type i*f
          {
            int times = Integer.parseInt(sn.substring(0, index));
            double value = FortranFormat.atof(sn.substring(index + 1));
            for (int j = i; j < i + times; j++) {
              acell[j] = (float) value;
            }
            i = i + times - 1;
          } else {
            acell[i] = (float) FortranFormat.atof(sn);
          }
        }
      } else if (sn.equals("rprim")) {
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            rprim[i][j] = (float) FortranFormat.atof(sn);

          }
        }
      } else if (sn.equals("ntype")) {
        sn = nextAbinitToken(input);
        ntype = Integer.parseInt(sn);
      } else if (sn.equals("natom")) {
        sn = nextAbinitToken(input);
        natom = Integer.parseInt(sn);
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      if (input.ready()) {
        line = input.readLine();
        st = new StringTokenizer(line, " \t");
        sn = nextAbinitToken(input);
      } else {
        sn = null;
      }
    }



    //Initialize dynamic variables
    int[] zatnum = (int[]) Array.newInstance(int.class, ntype);
    int[] type = (int[]) Array.newInstance(int.class, natom);

    //ChemFrame frame = new ChemFrame(natom);
    int[] dims = {
      natom, 3
    };
    float[][] xangst = (float[][]) Array.newInstance(float.class, dims);

    float[][] xred = (float[][]) Array.newInstance(float.class, dims);

    //Second pass through the file
    input.reset();
    sn = nextAbinitToken(input);

    while (sn != null) {
      if (sn.equals("zatnum")) {
        for (int i = 0; i < ntype; i++) {
          sn = nextAbinitToken(input);
          zatnum[i] = Integer.parseInt(sn);
        }
      } else if (sn.equals("type")) {
        for (int i = 0; i < natom; i++) {
          sn = nextAbinitToken(input);
          int index = sn.indexOf("*");
          if (index >= 0)    //Test if number format of type i*i
          {
            int times = Integer.parseInt(sn.substring(0, index));
            int value = Integer.parseInt(sn.substring(index + 1));
            for (int j = i; j < i + times; j++) {
              type[j] = value;
            }
            i = i + times - 1;
          } else {
            type[i] = Integer.parseInt(sn);
          }
        }
      } else if (sn.equals("xangst")) {
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            xangst[i][j] = (float) FortranFormat.atof(sn);
          }
        }
      } else if (sn.equals("xcart")) {
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            xangst[i][j] = (float) FortranFormat.atof(sn) * angstromPerBohr;
          }
        }
      } else if (sn.equals("xred")) {
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            xred[i][j] = (float) FortranFormat.atof(sn);
          }
          xangst[i][0] =
              (xred[i][0] * rprim[0][0] + xred[i][1] * rprim[1][0] + xred[i][2] * rprim[2][0])
                * acell[0] * angstromPerBohr;
          xangst[i][1] =
              (xred[i][0] * rprim[0][1] + xred[i][1] * rprim[1][1] + xred[i][2] * rprim[2][1])
                * acell[1] * angstromPerBohr;
          xangst[i][2] =
              (xred[i][0] * rprim[0][2] + xred[i][1] * rprim[1][2] + xred[i][2] * rprim[2][2])
                * acell[2] * angstromPerBohr;
        }
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      if (input.ready()) {
        line = input.readLine();
        st = new StringTokenizer(line, " \t");
        sn = nextAbinitToken(input);
      } else {
        sn = null;
      }

    }



    //Convert acell from bohr to angstrom
    acell[0] = acell[0] * (float) angstromPerBohr;
    acell[1] = acell[1] * (float) angstromPerBohr;
    acell[2] = acell[2] * (float) angstromPerBohr;

    //set the atom type
    int[] atomType = new int[natom];
    for (int i = 0; i < natom; i++) {
      atomType[i] = zatnum[type[i] - 1];
    }

    //Store unit cell info
    crystalFile.setUnitCellBox(new UnitCellBox(rprim, acell, true, atomType,
        xangst));
    crystalFile.setCrystalBox(new CrystalBox());    //use defaults value
    crystalFile.generateCrystalFrame();

    //fireFrameRead();
    return crystalFile;

  }


  /**
   * Read an ABINIT *output* file.
   * @return a <code>ChemFile</code> value
   * @exception IOException if an error occurs
   */
  public CrystalFile readAbinitOutput() throws IOException {

    CrystalFile crystalFile = new CrystalFile();
    int natom = 1;
    int ntype = 1;
    float[] acell = new float[3];
    float[][] rprim = new float[3][3];
    String info = "";
    String sn;
    String line;
    int count = 0;


    //We assume we have only 1 dataset.

    /*
     * The reading of the output file is essentially the same as the
     * reading of an output file except that the output file can
     * contain several frames
     */

    //First pass through the file (get variables of known dimension)
    input.mark(1024 * 1024);
    line = input.readLine();
    st = new StringTokenizer(line, " \t");
    sn = nextAbinitToken(input);

    while (sn != null) {
      if (sn.equals("acell")) {
        for (int i = 0; i < 3; i++) {

          sn = nextAbinitToken(input);
          acell[i] = (float) FortranFormat.atof(sn);
        }

        count++;    //We found acell

      } else if (sn.equals("rprim")) {
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            rprim[i][j] = (float) FortranFormat.atof(sn);
          }
        }
        count++;    //We found rprim
      } else if (sn.equals("ntype")) {
        sn = nextAbinitToken(input);
        ntype = Integer.parseInt(sn);
        count++;    //We found ntype
      } else if (sn.equals("natom")) {
        sn = nextAbinitToken(input);
        natom = Integer.parseInt(sn);
        count++;    //We found natom
      }

      //No need to countinue, we have found all of the searched keyword
      if (count == 4) {
        break;
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      if (input.ready()) {
        line = input.readLine();
        st = new StringTokenizer(line, " \t");
        sn = nextAbinitToken(input);
      } else {
        sn = null;
      }

    }


    //Initialize dynamic variables
    int[] zatnum = (int[]) Array.newInstance(int.class, ntype);
    int[] type = (int[]) Array.newInstance(int.class, natom);
    int[] dims = {
      natom, 3
    };
    float[][] xangst = (float[][]) Array.newInstance(float.class, dims);

    float[][] xred = (float[][]) Array.newInstance(float.class, dims);

    //Second pass through the file
    input.reset();
    sn = nextAbinitToken(input);

    count = 0;
    while (sn != null) {
      if (sn.equals("zatnum")) {
        for (int i = 0; i < ntype; i++) {
          sn = nextAbinitToken(input);
          zatnum[i] = (int) FortranFormat.atof(sn);
        }
        count++;
      } else if (sn.equals("type")) {
        for (int i = 0; i < natom; i++) {
          sn = nextAbinitToken(input);
          type[i] = Integer.parseInt(sn);
        }
        count++;
      }


      //No need to countinue, we have found all of the searched keyword
      if (count == 2) {
        break;
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      if (input.ready()) {
        line = input.readLine();
        st = new StringTokenizer(line, " \t");
        sn = nextAbinitToken(input);
      } else {
        sn = null;
      }

    }    //end while


    //Convert acell from bohr to angstrom
    acell[0] = acell[0] * (float) angstromPerBohr;
    acell[1] = acell[1] * (float) angstromPerBohr;
    acell[2] = acell[2] * (float) angstromPerBohr;


    //We start reading the frames
    while (sn != null) {
      if (sn.equals("NUMBER"))                          // Get the step number
      {
        info = nextAbinitToken(input);
      } else if (sn.equals("Cartesian")
          && nextAbinitToken(input).equals("coordinates")) {

        //ChemFrame frame = new ChemFrame(natom);
        //frame.setInfo("Step number: " + info);

        //Go to next line
        if (input.ready()) {
          line = input.readLine();
          st = new StringTokenizer(line, " \t");
        }

        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            sn = nextAbinitToken(input);
            xangst[i][j] = (float) FortranFormat.atof(sn) * angstromPerBohr;
          }


        }


        int[] atomType = new int[natom];
        for (int i = 0; i < natom; i++) {
          atomType[i] = zatnum[type[i] - 1];
        }

        //Store unit cell info
        crystalFile.setUnitCellBox(new UnitCellBox(rprim, acell, true,
            atomType, xangst));
        crystalFile.setCrystalBox(new CrystalBox());    //use defaults value
        crystalFile.generateCrystalFrame();

        //fireFrameRead();
      }


      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      if (input.ready()) {
        line = input.readLine();
        st = new StringTokenizer(line, " \t");
        sn = nextAbinitToken(input);
      } else {
        sn = null;
      }
    }



    return crystalFile;
  }

  /**
   * Find the next token of an abinit file.
   * ABINIT tokens are words separated by space(s). Characters
   * following a "#" are ignored till the end of the line.
   * @param input a <code>BufferedReader</code> value
   * @return a <code>String</code> value
   * @exception IOException if an error occurs
   */
  public String nextAbinitToken(BufferedReader input) throws IOException {

    String line;
    String sn;
    while (!st.hasMoreTokens() && input.ready()) {
      line = input.readLine();
      st = new StringTokenizer(line, " \t");
    }
    if (st.hasMoreTokens()) {
      sn = st.nextToken();
      if (sn.startsWith("#")) {
        line = input.readLine();
        if (input.ready()) {
          st = new StringTokenizer(line, " \t");
          sn = nextAbinitToken(input);
        }
      }
    } else {
      sn = null;
    }

    return sn;
  }

}

