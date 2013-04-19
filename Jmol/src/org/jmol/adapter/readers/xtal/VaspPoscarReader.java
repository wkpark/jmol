package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

/**
 * http://cms.mpi.univie.ac.at/vasp/
 * 
 * @author Pieremanuele Canepa, Wake Forest University, Department of Physics
 *         Winston Salem, NC 27106, canepap@wfu.edu
 * 
 * @version 1.0
 */

public class VaspPoscarReader extends AtomSetCollectionReader {

  private float scaleFac;
  private String[] atomLab;
  private int nAtom;

  @Override
  protected void initializeReader() throws Exception {
    setSpaceGroupName("P1");
    setFractionalCoordinates(true);
    readCellandCoordinates();
    continuing = false;
  }

  private void readCellandCoordinates() throws Exception {
    readTitle();
    readScfactor();
    readUnitCellVectors();
    readElementstring();
    readTypeCoord();
    readCoordinate();
  }

  private void readTitle() throws Exception {
    // Read job title
    atomSetCollection.setAtomSetName(readLine().trim());
  }

  private void readScfactor() throws Exception {
    // Read the scale factor 
    scaleFac = parseFloatStr(readLine().trim());
  }

  private float[] unitCellData = new float[9];

  private void readUnitCellVectors() throws Exception {
    // Read Unit Cell
    setSymmetry();
    fillFloatArray(null, 0, unitCellData);

    if (scaleFac != 1.00) {
      for (int i = 0; i < unitCellData.length; i++) {
        unitCellData[i] = unitCellData[i] * scaleFac;
      }
    }
    setUnitCell();
  }

  private void setUnitCell() {
    addPrimitiveLatticeVector(0, unitCellData, 0);
    addPrimitiveLatticeVector(1, unitCellData, 3);
    addPrimitiveLatticeVector(2, unitCellData, 6);
  }

  private void setSymmetry() throws Exception {
    applySymmetryAndSetTrajectory();
    setSpaceGroupName("P1");
  }

  private void readElementstring() throws Exception {
    readLine();
    // Read Type of atoms and number of atoms
    //   H    C    O    Be   C    H
    String elementLabel[] = getTokensStr(readLine());
    //   6    24    18     6     6    24
    String atomWeight[] = getTokensStr(line);
    for (int i = 0; i < atomWeight.length; i++) {
      nAtom += Integer.parseInt(atomWeight[i]);
    }

    atomLab = new String[nAtom];
    int counter = 0;
    for (int i = 0; i < elementLabel.length; i++) {
      int noSpecies = Integer.parseInt(atomWeight[i]);
      for (int j = 0; j < noSpecies; j++) {
        atomLab[counter] = elementLabel[i];
        counter++;
      }
    }
  }

  private void readTypeCoord() throws Exception {
    //Read type of coordinates
    // If Selective is there skip a line 
    readLine();
    if (line.toLowerCase().contains("selective"))
      readLine();
    if (line.toLowerCase().contains("cartesian")) {
      setFractionalCoordinates(false);
    } else if (line.toLowerCase().contains("direct")) {
      setFractionalCoordinates(true);
    }
  }

  private void readCoordinate() throws Exception {
    // Read coordinates 
    readLine();
    int counter = 0;
    while (counter < nAtom && readLine() != null) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      atom.atomName = atomLab[counter];
      float x = parseFloatStr(tokens[0]);
      float y = parseFloatStr(tokens[1]);
      float z = parseFloatStr(tokens[2]);
      setAtomCoordXYZ(atom, x, y, z);
      counter++;
    }
    atomSetCollection.setAtomSetName("Initial Coordinates");
  }

}
