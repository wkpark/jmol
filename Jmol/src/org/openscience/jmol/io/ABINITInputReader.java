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
package org.openscience.jmol.io;

import org.openscience.jmol.ChemFile;
import org.openscience.jmol.CrystalBox;
import org.openscience.jmol.UnitCellBox;
import org.openscience.jmol.FortranFormat;
import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.reflect.Array;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;

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
 * doubles depending of the keyword).
 * Characters following a '#' are ignored.
 * The fisrt line of the file can be considered as a title.
 * This implementaton supports only 1 dataset!!!
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 *<p><p> <p> Create an ABINIT input reader.
 *
 *
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.0 */
public class ABINITInputReader extends ABINITReader {
  
  // ABINIT VARIABLES
  int natom = 1;
  int ntype = 1;
  double acell[] = null;
  double[][] rprim = null;
  double[] angdeg = null;
  String info = "";
  String line;
  

  /**
   *The constructor
   */
  public ABINITInputReader(Reader input) {
    super(input);
  }
  
  /**
   * Read an ABINIT *input* file.
   * @return a <code>ChemFile</code> value
   * @exception IOException if an error occurs
   */
  public ChemFile read() throws IOException {

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
        acell = new double[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitInputToken(false);
          acell[i] = FortranFormat.atof(fieldVal)
	    * ANGSTROMPERBOHR;    //in angstrom
        }
      } else if (fieldVal.equals("rprim")) {
        rprim = new double[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            rprim[i][j] = FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("angdeg")) {
        angdeg = new double[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitInputToken(false);
          angdeg[i] = FortranFormat.atof(fieldVal);
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
    double[][] xangst = null;
    double[][] xred = null;

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
        xangst = (double[][]) Array.newInstance(double.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xangst[i][j] = FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("xcart")) {
        xangst = (double[][]) Array.newInstance(double.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xangst[i][j] = FortranFormat.atof(fieldVal)
	      * ANGSTROMPERBOHR;
          }
        }
      } else if (fieldVal.equals("xred")) {
        xred = (double[][]) Array.newInstance(double.class, dims);
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitInputToken(false);
            xred[i][j] = FortranFormat.atof(fieldVal);
          }
        }
      }

      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitInputToken(true);

    }

    //Set default value if needed
    if (acell == null) {                 //set acell to 1 bohr
      acell = new double[3];
      acell[0] = 1 * ANGSTROMPERBOHR;    //in angstrom    
      acell[1] = 1 * ANGSTROMPERBOHR;
      acell[2] = 1 * ANGSTROMPERBOHR;
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
      rprim = new double[3][3];
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

  }  //end read()


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
    
    if (newLine) { //We ignore the end of the line and go to the following line
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

          //We have a format integer*double
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
  } //end nextAbinitInputToken(boolean newLine)

} //end class ABINITInputReader

