package org.jmol.adapter.readers.xtal;


/**
 * http://cms.mpi.univie.ac.at/vasp/
 * 
 * @author Pieremanuele Canepa, MIT, 
 *         Department of Material Sciences and Engineering
 *         
 * 
 * @version 1.0
 */

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.TextFormat;

public class AbinitReader extends AtomSetCollectionReader {
  
  private float[] cellLattice;
  
  
  @Override
  protected void initializeReader() {
    setSpaceGroupName("P1");
    doApplySymmetry = true;
    // inputOnly = checkFilter("INPUT");
  }
  
  
  
  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains("Real(R)+Recip(G)")) {
      readIntiallattice();
    } 
    return true;
  }
  
  
  private void readIntiallattice() throws Exception {
  /*
      Real(R)+Recip(G) space primitive vectors, cartesian coordinates (Bohr,Bohr^-1):
      R(1)= 25.9374361  0.0000000  0.0000000  G(1)=  0.0385543  0.0222593  0.0000000
      R(2)=-12.9687180 22.4624785  0.0000000  G(2)=  0.0000000  0.0445187  0.0000000
      R(3)=  0.0000000  0.0000000 16.0314917  G(3)=  0.0000000  0.0000000  0.0623772
      Unit cell volume ucvol=  9.3402532E+03 bohr^3
      */
    
    cellLattice = new float[9];
    String data = "";
    int counter = 0;
    while (readLine() != null && line.indexOf("Unit cell volume") < 0){
      data += line;
      data = TextFormat.simpleReplace(data, "=", "= ");
      String[] tokens = getTokensStr(data);
      cellLattice[counter++] = parseFloatStr(tokens[1]) * ANGSTROMS_PER_BOHR;
      cellLattice[counter++] = parseFloatStr(tokens[2]) * ANGSTROMS_PER_BOHR;
      cellLattice[counter++] = parseFloatStr(tokens[3]) * ANGSTROMS_PER_BOHR;
      counter ++;
    }
    setSymmetry();
  }
  
  
  private void setSymmetry() throws Exception {
    applySymmetryAndSetTrajectory();
    setSpaceGroupName("P1");
    setFractionalCoordinates(false);
  }
  
  
  

}
