
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

  // This variable is used to parse the input file
  private StringTokenizer st;
  private String fieldVal;
  private int repVal = 0;

  private BufferedReader inputBuffer;

  /**
   * Creates a new <code>ABINITReader</code> instance.
   *
   * @param input a <code>Reader</code> value
   */
  public ABINITReader(Reader input) {
    super(input);
    this.inputBuffer = (BufferedReader) input;
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

    inputBuffer.mark(1024);
    for (int i = 0; i < 3; i++) {
      if (inputBuffer.ready()) {
        line = inputBuffer.readLine();
        if ((line.indexOf("ABINIT") >= 0) && (line.indexOf("Version") >= 0)) {

          //This is an output file
          inputBuffer.reset();
          System.out.println("We have an abinit *output* file");
          crystalFile = readAbinitOutput();
          return crystalFile;
        }
      }
    }

    //We don't have an output file so we have an input file
    inputBuffer.reset();
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
    float acell[] = null;
    float[][] rprim = null;
    float[] angdeg = null;
    String info = "";
    String line;

    info = inputBuffer.readLine();
    System.out.println(info);



    //Check if this is a multidataset file. Multidataset is not yet supported
    inputBuffer.mark(1024 * 1024);

    nextAbinitInputToken(true);

    while (fieldVal != null) {
      if (fieldVal.equals("ndtset")) {
        nextAbinitInputToken(false);
        if (FortranFormat.atof(fieldVal) > 1) {
          System.out.println("ABINITReader: multidataset not supported");
          return crystalFile;
        }
      }
      nextAbinitInputToken(false);
    }
    inputBuffer.reset();

    //First pass through the file (get variables of known dimension)
    inputBuffer.mark(1024 * 1024);
    nextAbinitInputToken(true);

    while (fieldVal != null) {
      if (fieldVal.equals("acell")) {
        acell = new float[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitInputToken(false);
          acell[i] = (float) FortranFormat.atof(fieldVal)
              * (float) angstromPerBohr;    //in angstrom
        }
      } else if (fieldVal.equals("rprim")) {
        rprim = new float[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            rprim[i][j] = (float) FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("angdeg")) {
        angdeg = new float[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitInputToken(false);
          angdeg[i] = (float) FortranFormat.atof(fieldVal);
        }
      } else if (fieldVal.equals("ntype")) {
        nextAbinitInputToken(false);
        ntype = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("natom")) {
        nextAbinitInputToken(false);
        natom = Integer.parseInt(fieldVal);
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitInputToken(true);
    }



    //Initialize dynamic variables
    int[] zatnum = (int[]) Array.newInstance(int.class, ntype);
    int[] type = (int[]) Array.newInstance(int.class, natom);


    int[] dims = {
      natom, 3
    };
    float[][] xangst = null;
    float[][] xred = null;

    //Second pass through the file
    inputBuffer.reset();
    nextAbinitInputToken(true);

    while (fieldVal != null) {
      if (fieldVal.equals("zatnum")) {
        for (int i = 0; i < ntype; i++) {
          nextAbinitInputToken(false);
          zatnum[i] = Integer.parseInt(fieldVal);
        }
      } else if (fieldVal.equals("type")) {
        for (int i = 0; i < natom; i++) {
          nextAbinitInputToken(false);
          type[i] = Integer.parseInt(fieldVal);
        }
      } else if (fieldVal.equals("xangst")) {
        xangst = (float[][]) Array.newInstance(float.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xangst[i][j] = (float) FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("xcart")) {
        xangst = (float[][]) Array.newInstance(float.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xangst[i][j] = (float) FortranFormat.atof(fieldVal)
                * angstromPerBohr;
          }
        }
      } else if (fieldVal.equals("xred")) {
        xred = (float[][]) Array.newInstance(float.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xred[i][j] = (float) FortranFormat.atof(fieldVal);
          }
        }
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitInputToken(true);

    }

    //Set default value if needed
    if (acell == null) {                 //set acell to 1 bohr
      acell = new float[3];
      acell[0] = 1 * angstromPerBohr;    //in angstrom    
      acell[1] = 1 * angstromPerBohr;
      acell[2] = 1 * angstromPerBohr;
    }


    //set the atom type
    int[] atomType = new int[natom];
    for (int i = 0; i < natom; i++) {
      atomType[i] = zatnum[type[i] - 1];
    }

    //Store unit cell info
    UnitCellBox unitCellBox = new UnitCellBox();
    if (rprim != null) {
      if (xangst != null) {
        unitCellBox = new UnitCellBox(rprim, acell, true, atomType, xangst);
      } else if (xred != null) {
        unitCellBox = new UnitCellBox(rprim, acell, false, atomType, xred);
      }

    } else if (angdeg != null) {
      if (xangst != null) {
        unitCellBox = new UnitCellBox(acell, angdeg, true, atomType, xangst);
      } else if (xred != null) {
        unitCellBox = new UnitCellBox(acell, angdeg, false, atomType, xred);
      }


    } else if ((rprim == null) && (angdeg == null)) {
      rprim = new float[3][3];
      rprim[0][0] = 1;
      rprim[0][1] = 0;
      rprim[0][2] = 0;
      rprim[1][0] = 0;
      rprim[1][1] = 1;
      rprim[1][2] = 0;
      rprim[2][0] = 0;
      rprim[2][1] = 0;
      rprim[2][2] = 1;
      if (xangst != null) {
        unitCellBox = new UnitCellBox(rprim, acell, true, atomType, xangst);
      } else if (xred != null) {
        unitCellBox = new UnitCellBox(rprim, acell, false, atomType, xred);
      }
    }
    unitCellBox.setInfo(info);
    crystalFile.setUnitCellBox(unitCellBox);
    crystalFile.setCrystalBox(new CrystalBox());    //use defaults value
    crystalFile.generateCrystalFrame();

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
    String line;
    int count = 0;


    //We assume we have only 1 dataset.

    /*
     * The reading of the output file is essentially the same as the
     * reading of an output file except that the output file can
     * contain several frames and there is no need to consider
     * to parse things such as "integer*float".
     */

    //First pass through the file (get variables of known dimension)

    //Skip the beginning of the files and go to "-outvars:"
    nextAbinitToken(true);
    while (!fieldVal.equals("-outvars:")) {
      nextAbinitToken(true);
    }


    inputBuffer.mark(1024 * 1024);

    // Read the value of preprocessed input variables
    while (fieldVal != null) {
      if (fieldVal.equals("acell")) {
        acell = new float[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitToken(false);
          acell[i] = (float) FortranFormat.atof(fieldVal)
              * (float) angstromPerBohr;    //in angstrom
        }
        count++;                            //We found acell
      } else if (fieldVal.equals("rprim")) {
        rprim = new float[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            rprim[i][j] = (float) FortranFormat.atof(fieldVal);
          }
        }
        count++;                            //We found rprim
      } else if (fieldVal.equals("ntype")) {
        nextAbinitToken(false);
        ntype = Integer.parseInt(fieldVal);
        count++;                            //We found ntype
      } else if (fieldVal.equals("natom")) {
        nextAbinitToken(false);
        natom = Integer.parseInt(fieldVal);
        count++;                            //We found natom
      }

      //No need to countinue, we have found all of the searched keyword
      if (count == 4) {
        break;
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitToken(true);

    }                                       //end while


    //Initialize dynamic variables
    int[] zatnum = (int[]) Array.newInstance(int.class, ntype);
    int[] type = (int[]) Array.newInstance(int.class, natom);
    int[] dims = {
      natom, 3
    };
    float[][] xangst = (float[][]) Array.newInstance(float.class, dims);
    float[][] xred = (float[][]) Array.newInstance(float.class, dims);

    //Second pass through the file
    inputBuffer.reset();
    nextAbinitToken(true);

    count = 0;
    while (fieldVal != null) {
      if (fieldVal.equals("zatnum")) {
        for (int i = 0; i < ntype; i++) {
          nextAbinitToken(false);
          zatnum[i] = (int) FortranFormat.atof(fieldVal);
        }
        count++;
      } else if (fieldVal.equals("type")) {
        for (int i = 0; i < natom; i++) {
          nextAbinitToken(false);
          type[i] = Integer.parseInt(fieldVal);
        }
        count++;
      }


      //No need to countinue, we have found all of the searched keyword
      if (count == 2) {
        break;
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitToken(true);

    }    //end while



    String frameSep = "BROYDEN";

    //We start reading the frames
    while (fieldVal != null) {
      if (fieldVal.equals(frameSep)) {
        info = fieldVal;
        nextAbinitToken(false);    //"STEP"
        info = info + " " + fieldVal;
        nextAbinitToken(false);    //"NUMBER"
        info = info + " " + fieldVal;
        nextAbinitToken(false);    // NUMBER
        info = info + " " + fieldVal;
        System.out.println(info);
      } else if (fieldVal.equals("acell=")) {
        acell = new float[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitToken(false);
          acell[i] = (float) FortranFormat.atof(fieldVal) * angstromPerBohr;    //in angstrom
        }
      } else if (fieldVal.equals("rprim=")) {
        rprim = new float[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            rprim[i][j] = (float) FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("Cartesian")
          && nextAbinitToken(false).equals("coordinates")) {



        nextAbinitToken(false);    //read "(bohr)"
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            xangst[i][j] = (float) FortranFormat.atof(fieldVal)
                * angstromPerBohr;
          }
        }

        //Set the atom types
        int[] atomType = new int[natom];
        for (int i = 0; i < natom; i++) {
          atomType[i] = zatnum[type[i] - 1];
        }

        //Store unit cell info
        UnitCellBox unitCellBox = new UnitCellBox(rprim, acell, true,
                                    atomType, xangst);
        unitCellBox.setInfo(info);
        crystalFile.setUnitCellBox(unitCellBox);

        //use defaults value
        crystalFile.setCrystalBox(new CrystalBox());
        crystalFile.generateCrystalFrame();


      }                            //end if "Cartesian coordinates"


      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitToken(true);
    }                              //end while

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
  public String nextAbinitToken(boolean newLine) throws IOException {

    String line;

    if (newLine) {    //We ignore the end of the line and go to the following line
      if (inputBuffer.ready()) {
        line = inputBuffer.readLine();
        st = new StringTokenizer(line, " \t");
      }
    }

    while (!st.hasMoreTokens() && inputBuffer.ready()) {
      line = inputBuffer.readLine();
      st = new StringTokenizer(line, " \t");
    }
    if (st.hasMoreTokens()) {
      fieldVal = st.nextToken();
      if (fieldVal.startsWith("#")) {
        nextAbinitToken(true);
      }
    } else {
      fieldVal = null;
    }
    return this.fieldVal;
  }


  /**
   * Put the next abinit token in <code>fieldVal</code>.
   * If <code>newLine</code> is <code>true</code>, the end of the line
   * is skiped. <br>
   * The first invocation of this method should be done with
   * <code>newLine</code> is <code>true</code>.
   *
   */
  public void nextAbinitInputToken(boolean newLine) throws IOException {

    String line;

    if (newLine) {    //We ignore the end of the line and go to the following line
      repVal = 0;
      if (inputBuffer.ready()) {
        line = inputBuffer.readLine();
        st = new StringTokenizer(line, " \t");
      }
    }


    if (repVal != 0) {
      repVal--;
    } else {
      while (!st.hasMoreTokens() && inputBuffer.ready()) {
        line = inputBuffer.readLine();
        st = new StringTokenizer(line, " \t");
      }
      if (st.hasMoreTokens()) {
        fieldVal = st.nextToken();
        int index = fieldVal.indexOf("*");
        if (index >= 0) {

          //We have a format integer*float
          repVal = Integer.parseInt(fieldVal.substring(0, index));
          fieldVal = fieldVal.substring(index + 1);
          repVal--;
        }
        if (fieldVal.startsWith("#")) {    //We have a comment
          nextAbinitInputToken(true);      //Skip the end of the line
        }
      } else {
        fieldVal = null;
      }
    }
  }

}

