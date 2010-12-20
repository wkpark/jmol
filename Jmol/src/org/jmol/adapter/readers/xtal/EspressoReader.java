package org.jmol.adapter.readers.xtal;

/**
 * Quantum Espresso
 * http://www.quantum-espresso.org and http://qe-forge.org/frs/?group_id=10
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

import javax.vecmath.Vector3f;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

public class EspressoReader extends AtomSetCollectionReader {

  private Vector3f[] abc = new Vector3f[3];
  private boolean endOptimization = false;
  
  @Override
  protected void initializeReader() {
    setSpaceGroupName("P1");
    //This is correct only for the first set of coordinates
    setFractionalCoordinates(false);
    // inputOnly = checkFilter("INPUT");
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains("lattice parameter (a_0)")) {
      readAparam();
    } else if (line.contains("crystal axes:")) {
      readIntialCellParam();
    } else if (line.contains("Cartesian axes")) {
      readInitialAtomic();
    } else if (line.contains("!    total energy")) {
      readEnergy();
    } else if (line.contains("CELL_PARAMETERS")) {
      readCellParam();
    } else if (line.contains("ATOMIC_POSITIONS")) {
      readAtomic();
    } else if(line.contains("End") && line.contains(" Geometry Optimization")){
      endOptimization = true;
    }
    return true;
  }


  private float aPar;

  private void readAparam() throws Exception {
    String[] tokens = getTokens(line.substring(line.indexOf("=") + 1));
    aPar = parseFloat(tokens[0]) * ANGSTROMS_PER_BOHR;
  }

  /*
  crystal axes: (cart. coord. in units of a_0)
            a(1) = (  1.000000  0.000000  0.000000 )  
            a(2) = ( -0.500000  0.866025  0.000000 )  
            a(3) = (  0.000000  0.000000  0.744955 )  

  reciprocal axes: (cart. coord. in units 2 pi/a_0)
            b(1) = (  1.000000  0.577350  0.000000 )  
            b(2) = (  0.000000  1.154701  0.000000 )  
            b(3) = (  0.000000  0.000000  1.342362 )  

   */

  private float a, b, c, alpha, beta, gamma;

  private void readIntialCellParam() throws Exception {
    readLine();
    String[] tokens = getTokens();
    float x, y, z;
    for (int n = 0; n < 3; n++) {
      if (tokens.length >= 3) {
        x = parseFloat(tokens[3]) * aPar;
        y = parseFloat(tokens[4]) * aPar;
        z = parseFloat(tokens[5]) * aPar;
        abc[n] = new Vector3f(x, y, z);
      }
      tokens = getTokens(readLine());
    }
    a = abc[0].length();
    b = abc[1].length();
    c = abc[2].length();
    alpha = (float) Math.toDegrees(abc[1].angle(abc[2]));
    beta = (float) Math.toDegrees(abc[2].angle(abc[0]));
    gamma = (float) Math.toDegrees(abc[0].angle(abc[1]));
    setCell();
  }
  
  
/*  
  CELL_PARAMETERS (alat= 17.62853047)
  1.019135101   0.000000000   0.000000000
 -0.509567550   0.882596887   0.000000000
  0.000000000   0.000000000   0.737221415

*/  private void readCellParam() throws Exception {
    aPar = parseFloat(line.substring(line.indexOf("=") + 1, line
       .lastIndexOf(")") - 1))
        * ANGSTROMS_PER_BOHR;
    String[] tokens = getTokens();
    float x, y, z;
    for (int n = 0; n < 3; n++) {
      if (tokens.length >= 3) {
        x = parseFloat(tokens[0]) * aPar;
        y = parseFloat(tokens[1]) * aPar;
        z = parseFloat(tokens[2]) * aPar;
        abc[n] = new Vector3f(x, y, z);
      }
      tokens = getTokens(readLine());
    }
    a = abc[0].length();
    b = abc[1].length();
    c = abc[2].length();
    alpha = (float) Math.toDegrees(abc[1].angle(abc[2]));
    beta = (float) Math.toDegrees(abc[2].angle(abc[0]));
    gamma = (float) Math.toDegrees(abc[0].angle(abc[1]));
    
    
    applySymmetryAndSetTrajectory();
    setSpaceGroupName("P1");  
    setFractionalCoordinates(true);
    setCell();
  }

  private void setCell() throws Exception {
    setUnitCell(a, b, c, alpha, beta, gamma);
    applySymmetryAndSetTrajectory();
  }

  /*
  Cartesian axes

    site n.     atom                  positions (a_0 units)
        1           Ca  tau(  1) = (   0.5000000  -0.2886751  -0.0018296  )
        2           Ca  tau(  2) = (  -0.5000000   0.2886751   0.3706481  )
        3           Ca  tau(  3) = (  -0.5000000   0.2886751   0.0001849  )
        4           Ca  tau(  4) = (   0.5000000  -0.2886751  -0.3722928  )
        5           Ca  tau(  5) = (   0.2493986  -0.0086855   0.1856962  )
        6           Ca  tau(  6) = (  -0.2493986   0.0086855  -0.1867815  )
        7           Ca  tau(  7) = (  -0.1171775   0.2203283   0.1856962  )
        8           Ca  tau(  8) = (  -0.1322212  -0.2116428   0.1856962  )
        9           Ca  tau(  9) = (   0.1322212   0.2116428  -0.1867815  )
       10           Ca  tau( 10) = (   0.1171775  -0.2203283  -0.1867815  )
   */
  private void readInitialAtomic() throws Exception {
    discardLines(2);
    while (readLine() != null && line.length() > 1) {
      String[] tokens = getTokens();
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = tokens[1];
      // here the cordinates are a_lat there fore expressed on base of cube of side a 
      float x = parseFloat(tokens[6])* aPar;
      float y = parseFloat(tokens[7])* aPar;
      float z = parseFloat(tokens[8])* aPar;
      setAtomCoord(atom, x, y, z);
      
    }
  }

  private void readAtomic() throws Exception {
    
/*    
    This is to check for 
    O       -0.088707198  -0.347657305   0.434774168
    O       -0.258950107   0.088707198   0.434774168
    O        0.000000000   0.000000000  -0.214003341
    O        0.000000000   0.000000000   0.286225136
    H        0.000000000   0.000000000  -0.071496337
    H        0.000000000   0.000000000   0.428733409
    End final coordinates
    */
    
    int condition = 1;
    if(endOptimization){
      condition = 22;
    }
    while (readLine() != null && line.length() > condition){
      String[] tokens = getTokens();
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = tokens[0];
      //Here the coordinates are fractional
      float x = parseFloat(tokens[1]);
      float y = parseFloat(tokens[2]);
      float z = parseFloat(tokens[3]);
      setAtomCoord(atom, x, y, z);
    }
  }

  //!    total energy              =   -1668.20791579 Ry
  private Double totEnergy;
  private void readEnergy() throws Exception {
    String[] tokens = getTokens(line.substring(line.indexOf("=") + 1));
    totEnergy = Double.valueOf(Double.parseDouble(tokens[0]));
    atomSetCollection.newAtomSet();
    setEnergy();
  }

  private void setEnergy() {
    atomSetCollection.setAtomSetEnergy("" + totEnergy, totEnergy.floatValue());
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", totEnergy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", totEnergy);
    atomSetCollection.setAtomSetName("E = " + totEnergy + " Ry");
  }
}
