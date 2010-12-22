package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * Problems identified (Bob Hanson) --
 * 
 *   -- Are these "lattice vectors" for the primitive unit cell? I think so.
 *      Then, if that's the case, why do we not see a list of symmetry-generated atoms?
 * 
 *   -- Frequency data number of atoms does not correspond to initial atom count.
 *      It looks like there is a missing report of symmetry-generated atoms. 
 *      
 * see https://projects.ivec.org/gulp/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * 
 * 
 * @version 1.0
 */

public class GulpReader extends AtomSetCollectionReader {

  private String spaceGroup;
  private boolean isSlab;
  private boolean isPolymer;
  private boolean isMolecular;
  private boolean isPrimitive;
  private String sep = "-------";

  @Override
  protected void initializeReader() throws Exception {
    setFractionalCoordinates(readDimensionality());
    isPrimitive = !checkFilter("CONV");
  }

  private boolean bTest;
  
  @Override
  protected boolean checkLine() throws Exception {
    System.out.println(line);
    if (line.contains("Space group ")) {
      readSpaceGroup();
      return true;
    } 
    
    if (isSlab ? line.contains("Surface cell parameters")
        : isPrimitive ? line.contains("Cartesian lattice vectors")
        : line.contains("Cell parameters (Angstroms/Degrees)")) {
      readCellParameters();
      return true;
    } 
    
    if (line.contains("Fractional coordinates of asymmetric unit :")
        || line.contains("Final asymmetric unit coordinates")
        || (bTest = line.contains("Final fractional coordinates "))
        || line
            .contains(" Mixed fractional/Cartesian coordinates of surface :")
        || line.contains("Cartesian coordinates of cluster ")
        || line.contains(" Final cartesian coordinates of atoms :")
        && isMolecular) {
      if (doGetModel(++modelNumber))
        readAtomicPos(!bTest);
      return true;
    } 
    // past this point, we must already have coordinates defined
    if (!doProcessLines)
      return true;
    if (line.contains("Final cell parameters and derivatives")) {
      // this line comes AFTER atom positions
      readFinalCell();
      return true;
    }    
    if (line.contains("Monopole - monopole (total)")) {
      readEnergy();
      return true;
    }
    
    //if (line.contains(" Phonon Calculation : ")) {
    // readFrequency();

    return true;
  }

  private boolean readDimensionality() throws Exception {
    discardLinesUntilContains("Dimensionality");
    String[] tokens = getTokens();
    switch (parseInt(tokens[2])) {
    case 0:
      isMolecular = true;
      isPrimitive = false;
      return false;
    case 1:
      isPolymer = true;
      isPrimitive = false;
      break;
    case 2:
      isSlab = true;
      isPrimitive = false;
      break;
    }
    return true;
  }
  
  private void readSpaceGroup() throws Exception {
    spaceGroup = (isPrimitive ? "P1" 
        : line.substring(line.indexOf(":") + 1).trim());
  }

  private float a, b, c, alpha, beta, gamma;
  private float[] primitiveData;
  private static String[] tags = AtomSetCollection.notionalUnitcellTags;

  private static int parameterIndex(String key) {
    for (int i = tags.length; --i >= 0;)
      if (tags[i].equals(key))
        return i;
    return -1;
  }

  private void setParameter(String key, float value) {
    switch (parameterIndex(key)) {
    case 0:
      a = value;
      break;
    case 1:
      b = value;
      break;
    case 2:
      c = value;
      break;
    case 3:
      alpha = value;
      break;
    case 4:
      beta = value;
      break;
    case 5:
      gamma = value;
      break;
    }
  }

  private void newAtomSet(boolean doSetUnitCell) {
    atomSetCollection.newAtomSet();
    if (doSetUnitCell)
      setUnitCell();
  }

  private void setUnitCell() {
    setSpaceGroupName(spaceGroup);
    if (a == 0 && primitiveData == null)
      return;
    if (a == 0) {
      addPrimitiveLatticeVector(0, primitiveData, 0);
      addPrimitiveLatticeVector(1, primitiveData, 3);
      addPrimitiveLatticeVector(2, primitiveData, 6);
    } else {
      if (isSlab) {
        c = -1;
        if (beta == 0)
          beta = gamma = 90;
      } else if (isPolymer) {
        b = c = -1;
        alpha = beta = gamma = 90;
      }
      setUnitCell(a, b, c, alpha, beta, gamma);
    }
  }

  /*

  Cartesian lattice vectors (Angstroms) :

       10.944693    0.000000    0.000000
        3.123705    4.493221    0.000000
        3.123705    1.632784    4.186054

  Cell parameters (Angstroms/Degrees):

  a =      10.9447    alpha =  55.1928
  b =       5.4723    beta  =  55.1928
  c =       5.4723    gamma =  55.1928

   */
  
  private void readCellParameters() throws Exception {
    discardLines(1);
    if (isPrimitive) {
      primitiveData = new float[9];
      fillFloatArray(primitiveData, null, 0);
      a = 0;
      return;
    }
    while (readLine() != null && line.contains("="))  {
      String[] tokens = getTokens();
      if (tokens.length >1 && tokens[1].equals("="))
        setParameter(tokens[0], parseFloat(tokens[2]));
      if (tokens.length > 5 && tokens[4].equals("="))
          setParameter(tokens[3], parseFloat(tokens[5]));
    }
  }

  /*
    Final cell parameters and derivatives :

      --------------------------------------------------------------------------------
             a            5.153230 Angstrom     dE/de1(xx)     0.000090 eV/strain
             b            5.153230 Angstrom     dE/de2(yy)     0.000000 eV/strain
             c            5.153230 Angstrom     dE/de3(zz)     0.000078 eV/strain
             alpha       55.766721 Degrees      dE/de4(yz)     0.000000 eV/strain
             beta        55.766721 Degrees      dE/de5(xz)     0.000000 eV/strain
             gamma       55.766721 Degrees      dE/de6(xy)     0.000000 eV/strain
      --------------------------------------------------------------------------------


   */

  private void readFinalCell() throws Exception {
    discardLinesUntilContains(sep);
    String tokens[];
    while (readLine() != null && (tokens = getTokens()).length >= 2)
      setParameter(tokens[0], parseFloat(tokens[1]));
    setUnitCell();
    applySymmetryAndSetTrajectory();
  }

  /*  Fractional coordinates of asymmetric unit :

    --------------------------------------------------------------------------------
       No.  Atomic       x           y          z         Charge      Occupancy
            Label      (Frac)      (Frac)     (Frac)        (e)         (Frac)  
    --------------------------------------------------------------------------------
          1 Ba    c    0.572390    0.144780    0.144780      0.1690    1.000000    
          2 Ba    c    0.677610 *  0.355219 *  0.355219 *    0.1690    1.000000    
          3 Fe    c    0.822391 *  0.644781 *  0.644781 *    1.9710    1.000000    
          4 Fe    c    0.927610 *  0.855219 *  0.855219 *    1.9710    1.000000    
          5 Fe    c    0.072390 *  0.144780 *  0.144780 *    1.9710    1.000000    
          6 Fe    c    0.177610 *  0.355219 *  0.355219 *    1.9710    1.000000    
          7 Fe    c    0.322391 *  0.644781 *  0.644781 *    1.9710    1.000000    
          8 Fe    c    0.427610 *  0.855219 *  0.855219 *    1.9710    1.000000    
          9 O     c    0.778081 *  0.943838 *  0.250000 *    0.5130    1.000000  
   */

  /*

  Cartesian coordinates of cluster :

  --------------------------------------------------------------------------------
   No.  Atomic       x           y          z         Charge      Occupancy
        Label      (Angs)      (Angs)     (Angs)        (e)         (Frac)  
  --------------------------------------------------------------------------------
      1 C1    c     -2.3420 *    1.0960 *   -0.0010 *   -0.1819    1.000000    
      2 C1    c     -1.0490 *    1.5890 *    0.0010 *   -0.2684    1.000000    
      3 C2    c      0.0730      0.7330     -0.0010      0.0661    1.000000    
      4 C3    c     -0.1850 *   -0.6550 *   -0.0030 *    0.1492    1.000000    
      5 C1    c     -1.4790 *   -1.1720 *   -0.0050 *   -0.1988    1.000000    
      6 C4    c     -2.5770 *   -0.3000 *   -0.0040 *    0.2336    1.000000    
      7 H1    c     -3.1860 *    1.7820 *   -0.0070 *    0.1768    1.000000    
      8 H1    c     -0.8980 *    2.6650 *    0.0030 *    0.1892    1.000000    
      9 C5    c      1.4540 *    1.1720 *    0.0010 *    0.1307    1.000000    
     10 H1    c     -1.6110 *   -2.2500 *   -0.0110 *    0.1768    1.000000    
     11 C7    c      2.1730 *   -1.1940 *    0.0010 *    0.7946    1.000000    
     12 C6    c      2.4410 *    0.2330 *    0.0020 *   -0.4195    1.000000    
     13 H1    c      3.4910 *    0.5060 *    0.0030 *    0.1718    1.000000    
     14 O1    c      0.8280 *   -1.5660 *   -0.0020 *   -0.3694    1.000000    
     15 O2    c      2.9950 *   -2.0810 *    0.0040 *   -0.6874    1.000000    
     16 C8    c      1.7830 *    2.6420 *    0.0020 *   -0.2362    1.000000    
     17 H2    c      1.3640 *    3.1400 *   -0.8810 *    0.0960    1.000000    
     18 H2    c      1.3630 *    3.1390 *    0.8850 *    0.0960    1.000000    
     19 H2    c      2.8640 *    2.7990 *    0.0020 *    0.0960    1.000000    
     20 N1    c     -3.8700 *   -0.7920 *   -0.0560 *   -0.8553    1.000000    
     21 H3    c     -4.0140 *   -1.7570 *    0.2000 *    0.4202    1.000000    
     22 H3    c     -4.6160 *   -0.1740 *    0.2260 *    0.4202    1.000000    
  --------------------------------------------------------------------------------

   */

  private void readAtomicPos(boolean finalizeSymmetry) throws Exception {
    newAtomSet(finalizeSymmetry);
    discardLinesUntilContains(sep);
    discardLinesUntilContains(sep);
    while (readLine() != null && line.indexOf(sep) < 0) {
      line = line.replace('*', ' ');
      String[] tokens = getTokens();
      if (!tokens[2].equals("c"))
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = tokens[1];
      setAtomCoord(atom, parseFloat(tokens[3]), parseFloat(tokens[4]),
          parseFloat(tokens[5]));
    }
    if (finalizeSymmetry)
      applySymmetryAndSetTrajectory();
  }

  /*  
  --------------------------------------------------------------------------------
  Total lattice energy       =        -386.17106576 eV
  --------------------------------------------------------------------------------
  Total lattice energy       =          -37259.6047 kJ/(mole unit cells)
  Total lattice energy       =           -8905.2592 kcal/(mole unit cells)
  --------------------------------------------------------------------------------


   **** Optimisation achieved ****


  Final energy =    -557.53367977 eV
  Final Gnorm  =       0.00035566

   */

  private Double totEnergy;
  private String energyUnits;

  private void readEnergy() throws Exception {
    // question: Why read monopole-monopole energy as "totEnergy"?
    // note that in some cases this is in Kcal/mol
    if (line.indexOf("=") < 0)
      discardLinesUntilContains("=");
    String[] tokens = getTokens(line.substring(line.indexOf("=")));
    totEnergy = Double.valueOf(Double.parseDouble(tokens[1]));
    energyUnits = tokens[2];
    discardLinesUntilContains(sep);
    setEnergy();    
  }

  private void setEnergy() {
    atomSetCollection.setAtomSetEnergy("" + totEnergy, totEnergy.floatValue());
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", totEnergy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", totEnergy);
    atomSetCollection.setAtomSetName("E = " + totEnergy + " " + energyUnits);
  }

  /*  
  private void setAtomSetInfo(){

  }*/

  /*  
  Frequency       0.0000    0.0000    0.0000  133.9963  134.1490  227.2719
  IR Intensity    0.0000    0.0000    0.0000    0.0030    0.0030    0.0000
     in X         0.0000    0.0000    0.0000    0.0008    0.0023    0.0000
     in Y         0.0000    0.0000    0.0000    0.0023    0.0008    0.0000
     in Z         0.0000    0.0000    0.0000    0.0000    0.0000    0.0000
  Raman Intsty    0.0000    0.0000    0.0000    0.0000    0.0000    0.3276

      1 x       0.390326 -0.056775 -0.015611 -0.039949  0.069193 -0.305899
      1 y       0.054908  0.388754 -0.040954  0.091620  0.052898  0.000000
      1 z      -0.021265 -0.038324 -0.392302 -0.013870 -0.008008  0.000000
      2 x       0.390326 -0.056775 -0.015611  0.099320 -0.011213  0.152950
      2 y       0.054908  0.388754 -0.040954  0.011214 -0.086372 -0.264916
      2 z      -0.021265 -0.038324 -0.392302  0.013870 -0.008007  0.000000
      3 x       0.390326 -0.056775 -0.015611 -0.039948 -0.091621  0.152949
      3 y       0.054908  0.388754 -0.040954 -0.069194  0.052897  0.264916
      3 z      -0.021265 -0.038324 -0.392302  0.000000  0.016016  0.000000
      4 x       0.294586 -0.042849 -0.011782 -0.120330  0.243402 -0.030861
      4 y       0.041440  0.293399 -0.030909  0.086109  0.358453  0.294237
      4 z      -0.016049 -0.028924 -0.296077  0.125378  0.314751  0.179865
      5 x       0.294586 -0.042849 -0.011782  0.381440  0.203588 -0.239386
      5 y       0.041440  0.293399 -0.030909  0.046295 -0.143317 -0.173845
      5 z      -0.016049 -0.028924 -0.296077 -0.335271 -0.048796  0.179865
      6 x       0.294586 -0.042849 -0.011782  0.096075 -0.211051  0.270247
      6 y       0.041440  0.293399 -0.030909 -0.368344  0.142048 -0.120392
      6 z      -0.016049 -0.028924 -0.296077  0.209894 -0.265956  0.179865
      7 x       0.294586 -0.042849 -0.011782 -0.150630  0.225910 -0.030862
      7 y       0.041440  0.293399 -0.030909  0.353487 -0.104649 -0.294238
      7 z      -0.016049 -0.028924 -0.296077  0.335275 -0.048791 -0.179865
      8 x       0.294586 -0.042849 -0.011782 -0.367031 -0.228549 -0.239387
      8 y       0.041440  0.293399 -0.030909 -0.100971  0.111751  0.173846
      8 z      -0.016049 -0.028924 -0.296077 -0.209891 -0.265961 -0.179865
      9 x       0.294586 -0.042849 -0.011782  0.134742 -0.188728  0.270249
      9 y       0.041440  0.293399 -0.030909 -0.061150 -0.390021  0.120392
      9 z      -0.016049 -0.028924 -0.296077 -0.125383  0.314752 -0.179865
   */
  /*
  private void readFrequency() throws Exception {
    discardLines(1);
    readLine();

    //for the  time being it reads only phonons at the gamma point k (0, 0, 0)
    if (line.contains("gamma")) {
      discardLines(9);
      if (line.contains("Frequency"))
        ;
    } else {
      return;
    }
  }
   */
}
