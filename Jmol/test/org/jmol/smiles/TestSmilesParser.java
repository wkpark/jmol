/*
 * JUnit TestCase for the SmilesParser
 */

package org.jmol.smiles;

import junit.framework.TestCase;

public class TestSmilesParser extends TestCase {

  public TestSmilesParser(String arg0) {
    super(arg0);
  }

  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 1 of Smiles tutorial
   */
  public void testChapter1_01() {    // Test [H+]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomH = molecule.createAtom();
    atomH.setCharge(1);
    atomH.setSymbol("H");
    checkMolecule("[H+]", molecule);
  }
  public void testChapter1_02() {    // Test C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    checkMolecule("C", molecule);
  }
  public void testChapter1_03() {    // Test O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomO);
    checkMolecule("O", molecule);
  }
  public void testChapter1_04() {    // Test [OH3+]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setCharge(1);
    atomO.setSymbol("O");
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomO);
    checkMolecule("[OH3+]", molecule);
  }
  public void testChapter1_05() {    // Test [2H]O[2H]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomH1 = molecule.createAtom();
    atomH1.setAtomicMass(2);
    atomH1.setSymbol("H");
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    molecule.createBond(atomH1, atomO, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomH2 = molecule.createAtom();
    atomH2.setAtomicMass(2);
    atomH2.setSymbol("H");
    molecule.createBond(atomO, atomH2, SmilesBond.TYPE_SINGLE);
    checkMolecule("[2H]O[2H]", molecule);
  }
  public void testChapter1_06() {    // Test [Au]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomAu = molecule.createAtom();
    atomAu.setSymbol("Au");
    checkMolecule("[Au]", molecule);
  }
  public void testChapter1_07() {    // Test CCO
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    molecule.createBond(atomC2, atomO, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomO);
    checkMolecule("CCO", molecule);
  }
  public void testChapter1_08() {    // Test O=C=O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    molecule.createBond(atomO1, atomC, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC, atomO2, SmilesBond.TYPE_DOUBLE);
    checkMolecule("O=C=O", molecule);
  }
  public void testChapter1_09() {    // Test C#N
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    SmilesAtom atomN = molecule.createAtom();
    atomN.setSymbol("N");
    molecule.createBond(atomC, atomN, SmilesBond.TYPE_TRIPLE);
    addHydrogen(molecule, atomC);
    checkMolecule("C#N", molecule);
  }
  public void testChapter1_10() {    // Test CC(=O)O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomC2, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC2, atomO2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomO2);
    checkMolecule("CC(=O)O", molecule);
  }
  public void testChapter1_11() {    // Test C1CCCCC1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC6);
    checkMolecule("C1CCCCC1", molecule);
  }
  public void testChapter1_12() {    // Test C1CC2CCCCC2CC1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    molecule.createBond(atomC3, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC7 = molecule.createAtom();
    atomC7.setSymbol("C");
    molecule.createBond(atomC6, atomC7, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC8 = molecule.createAtom();
    atomC8.setSymbol("C");
    molecule.createBond(atomC7, atomC8, SmilesBond.TYPE_SINGLE);
    atomC3.getBond(1).setAtom2(atomC8);
    atomC8.addBond(atomC3.getBond(1));
    SmilesAtom atomC9 = molecule.createAtom();
    atomC9.setSymbol("C");
    molecule.createBond(atomC8, atomC9, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC0 = molecule.createAtom();
    atomC0.setSymbol("C");
    molecule.createBond(atomC9, atomC0, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC0);
    atomC0.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC8);
    addHydrogen(molecule, atomC9);
    addHydrogen(molecule, atomC9);
    addHydrogen(molecule, atomC0);
    addHydrogen(molecule, atomC0);
    checkMolecule("C1CC2CCCCC2CC1", molecule);
  }
  public void testChapter1_13() {    // Test c1ccccc1
    // Not implemented
  }
  public void testChapter1_14() {    // Test [Na+].[O-]c1ccccc1
    // Not implemented
  }
  public void testChapter1_15() {    // Test C/C=C/C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_DIRECTIONAL_1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    checkMolecule("C/C=C/C", molecule);
  }
  public void testChapter1_16() {    // Test N[C@@H](C)C(=O)O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomN = molecule.createAtom();
    atomN.setSymbol("N");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setChiralClass("");
    atomC1.setChiralOrder(2);
    atomC1.setSymbol("C");
    molecule.createBond(atomN, atomC1, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC1, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomC3, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC3, atomO2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomN);
    addHydrogen(molecule, atomN);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomO2);
    checkMolecule("N[C@@H](C)C(=O)O", molecule);
  }
  public void testChapter1_17() {    // Test O[C@H]1CCCC[C@H]1O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setChiralClass("");
    atomC1.setChiralOrder(1);
    atomC1.setSymbol("C");
    molecule.createBond(atomO1, atomC1, SmilesBond.TYPE_SINGLE);
    molecule.createBond(atomC1, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setChiralClass("");
    atomC6.setChiralOrder(1);
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(1).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(1));
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC6, atomO2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomO1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomO2);
    checkMolecule("O[C@H]1CCCC[C@H]1O", molecule);
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 2 of Smiles tutorial
   */
  public void testChapter2_01() {    // Test [S]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomS = molecule.createAtom();
    atomS.setSymbol("S");
    checkMolecule("[S]", molecule);
  }
  public void testChapter2_02() {    // Test [Au]
    testChapter1_06();
  }
  public void testChapter2_03() {    // Test C
    testChapter1_02();
  }
  public void testChapter2_04() {    // Test P
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomP = molecule.createAtom();
    atomP.setSymbol("P");
    addHydrogen(molecule, atomP);
    addHydrogen(molecule, atomP);
    addHydrogen(molecule, atomP);
    checkMolecule("P", molecule);
  }
  public void testChapter2_05() {    // Test S
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomS = molecule.createAtom();
    atomS.setSymbol("S");
    addHydrogen(molecule, atomS);
    addHydrogen(molecule, atomS);
    checkMolecule("S", molecule);
  }
  public void testChapter2_06() {    // Test Cl
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    addHydrogen(molecule, atomCl);
    checkMolecule("Cl", molecule);
  }
  public void testChapter2_07() {    // Test [OH-]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setCharge(-1);
    atomO.setSymbol("O");
    addHydrogen(molecule, atomO);
    checkMolecule("[OH-]", molecule);
  }
  public void testChapter2_08() {    // Test [OH-1]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setCharge(-1);
    atomO.setSymbol("O");
    addHydrogen(molecule, atomO);
    checkMolecule("[OH-1]", molecule);
  }
  public void testChapter2_09() {    // Test [Fe+2]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomFe = molecule.createAtom();
    atomFe.setCharge(2);
    atomFe.setSymbol("Fe");
    checkMolecule("[Fe+2]", molecule);
  }
  public void testChapter2_10() {    // Test [Fe++]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomFe = molecule.createAtom();
    atomFe.setCharge(2);
    atomFe.setSymbol("Fe");
    checkMolecule("[Fe++]", molecule);
  }
  public void testChapter2_11() {    // Test [235U]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomU = molecule.createAtom();
    atomU.setAtomicMass(235);
    atomU.setSymbol("U");
    checkMolecule("[235U]", molecule);
  }
  public void testChapter2_12() {    // Test [*+2]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atom = molecule.createAtom();
    atom.setCharge(2);
    atom.setSymbol("*");
    checkMolecule("[*+2]", molecule);
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 3 of Smiles tutorial
   */
  public void testChapter3_01() {    // Test CC
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    checkMolecule("CC", molecule);
  }
  public void testChapter3_02() {    // Test C-C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    checkMolecule("C-C", molecule);
  }
  public void testChapter3_03() {    // Test [CH3]-[CH3]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    checkMolecule("[CH3]-[CH3]", molecule);
  }
  public void testChapter3_04() {    // Test C=O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    molecule.createBond(atomC, atomO, SmilesBond.TYPE_DOUBLE);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    checkMolecule("C=O", molecule);
  }
  public void testChapter3_05() {    // Test C#N
    testChapter1_09();
  }
  public void testChapter3_06() {    // Test C=C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    checkMolecule("C=C", molecule);
  }
  public void testChapter3_07() {    // Test cc
    // Not implemented
  }
  public void testChapter3_08() {    // Test C=CC=C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_DOUBLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    checkMolecule("C=CC=C", molecule);
  }
  public void testChapter3_09() {    // Test cccc
    // Not implemented
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 4 of Smiles tutorial
   */
  public void testChapter4_01() {    // Test CC(C)C(=O)O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC2, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomC4, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC4, atomO2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomO2);
    checkMolecule("CC(C)C(=O)O", molecule);
  }
  public void testChapter4_02() {    // Test FC(F)F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    molecule.createBond(atomF1, atomC, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC, atomF2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF3 = molecule.createAtom();
    atomF3.setSymbol("F");
    molecule.createBond(atomC, atomF3, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC);
    checkMolecule("FC(F)F", molecule);
  }
  public void testChapter4_03() {    // Test C(F)(F)F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    molecule.createBond(atomC, atomF1, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC, atomF2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF3 = molecule.createAtom();
    atomF3.setSymbol("F");
    molecule.createBond(atomC, atomF3, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC);
    checkMolecule("C(F)(F)F", molecule);
  }
  public void testChapter4_04() {    // Test O=Cl(=O)(=O)[O-]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomO1, atomCl, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomCl, atomO2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO3 = molecule.createAtom();
    atomO3.setSymbol("O");
    molecule.createBond(atomCl, atomO3, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO4 = molecule.createAtom();
    atomO4.setCharge(-1);
    atomO4.setSymbol("O");
    molecule.createBond(atomCl, atomO4, SmilesBond.TYPE_SINGLE);
    checkMolecule("O=Cl(=O)(=O)[O-]", molecule);
  }
  public void testChapter4_05() {    // Test Cl(=O)(=O)(=O)[O-]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomCl, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomCl, atomO2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO3 = molecule.createAtom();
    atomO3.setSymbol("O");
    molecule.createBond(atomCl, atomO3, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO4 = molecule.createAtom();
    atomO4.setCharge(-1);
    atomO4.setSymbol("O");
    molecule.createBond(atomCl, atomO4, SmilesBond.TYPE_SINGLE);
    checkMolecule("Cl(=O)(=O)(=O)[O-]", molecule);
  }
  public void testChapter4_06() {    // Test CCCC(C(=O)O)CCC
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomC5, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC5, atomO2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC4, atomC6, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC7 = molecule.createAtom();
    atomC7.setSymbol("C");
    molecule.createBond(atomC6, atomC7, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC8 = molecule.createAtom();
    atomC8.setSymbol("C");
    molecule.createBond(atomC7, atomC8, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomO2);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC8);
    addHydrogen(molecule, atomC8);
    addHydrogen(molecule, atomC8);
    checkMolecule("CCCC(C(=O)O)CCC", molecule);
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 5 of Smiles tutorial
   */
  public void testChapter5_01() {    // Test C1CCCCC1
    testChapter1_11();
  }
  public void testChapter5_02() {    // Test C1=CCCCC1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC6);
    checkMolecule("C1=CCCCC1", molecule);
  }
  public void testChapter5_03() {    // Test C=1CCCCC1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    checkMolecule("C=1CCCCC1", molecule);
  }
  public void testChapter5_04() {    // Test C1CCCCC=1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    checkMolecule("C1CCCCC=1", molecule);
  }
  public void testChapter5_05() {    // Test C=1CCCCC=1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC6);
    atomC6.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    checkMolecule("C=1CCCCC=1", molecule);
  }
  public void testChapter5_06() {    // Test c12c(cccc1)cccc2
    // Not implemented
  }
  public void testChapter5_07() {    // Test c1cc2ccccc2cc1
    // Not implemented
  }
  public void testChapter5_08() {    // Test c1ccccc1c2ccccc2
    // Not implemented
  }
  public void testChapter5_09() {    // Test c1ccccc1c1ccccc1
    // Not implemented
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 6 of Smiles tutorial
   */
  public void testChapter6_01() {    // Test [Na+].[Cl-]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomNa = molecule.createAtom();
    atomNa.setCharge(1);
    atomNa.setSymbol("Na");
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setCharge(-1);
    atomCl.setSymbol("Cl");
    checkMolecule("[Na+].[Cl-]", molecule);
  }
  public void testChapter6_02() {    // Test [Na+].[O-]c1ccccc1
    // Not implemented
  }
  public void testChapter6_03() {    // Test c1cc([O-].[Na+])ccc1
    // Not implemented
  }
  public void testChapter6_04() {    // Test C1.O2.C12
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    molecule.createBond(atomO, atomC2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    checkMolecule("C1.O2.C12", molecule);
  }
  public void testChapter6_05() {    // Test CCO
    testChapter1_07();
  }
  
  /*
   * Test methods for 'org.jmol.smiles.SmilesParser'
   * Using examples from Chapter 7 of Smiles tutorial
   */
  public void testChapter7_01() {    // Test C
    testChapter1_02();
  }
  public void testChapter7_02() {    // Test [C]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    checkMolecule("[C]", molecule);
  }
  public void testChapter7_03() {    // Test [12C]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setAtomicMass(12);
    atomC.setSymbol("C");
    checkMolecule("[12C]", molecule);
  }
  public void testChapter7_04() {    // Test [13C]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setAtomicMass(13);
    atomC.setSymbol("C");
    checkMolecule("[13C]", molecule);
  }
  public void testChapter7_05() {    // Test [13CH4]
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC = molecule.createAtom();
    atomC.setAtomicMass(13);
    atomC.setSymbol("C");
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomC);
    checkMolecule("[13CH4]", molecule);
  }
  public void testChapter7_06() {    // Test F/C=C/F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF1, atomC1, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC2, atomF2, SmilesBond.TYPE_DIRECTIONAL_1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    checkMolecule("F/C=C/F", molecule);
  }
  public void testChapter7_07() {    // Test F\C=C\F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF1, atomC1, SmilesBond.TYPE_DIRECTIONAL_2);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC2, atomF2, SmilesBond.TYPE_DIRECTIONAL_2);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    checkMolecule("F\\C=C\\F", molecule);
  }
  public void testChapter7_08() {    // Test F/C=C\F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF1, atomC1, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC2, atomF2, SmilesBond.TYPE_DIRECTIONAL_2);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    checkMolecule("F/C=C\\F", molecule);
  }
  public void testChapter7_09() {    // Test F\C=C/F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF1 = molecule.createAtom();
    atomF1.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF1, atomC1, SmilesBond.TYPE_DIRECTIONAL_2);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomF2 = molecule.createAtom();
    atomF2.setSymbol("F");
    molecule.createBond(atomC2, atomF2, SmilesBond.TYPE_DIRECTIONAL_1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    checkMolecule("F\\C=C/F", molecule);
  }
  public void testChapter7_10() {    // Test F/C=C/C=C/C
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF, atomC1, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_DIRECTIONAL_1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    checkMolecule("F/C=C/C=C/C", molecule);
  }
  public void testChapter7_11() {    // Test F/C=C/C=CC
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomF, atomC1, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_DIRECTIONAL_1);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    checkMolecule("F/C=C/C=CC", molecule);
  }
  public void testChapter7_12() {    // Test N[C@@H](C)C(=O)O
    testChapter1_16();
  }
  public void testChapter7_13() {    // Test N[C@H](C)C(=O)O
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomN = molecule.createAtom();
    atomN.setSymbol("N");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setChiralClass("");
    atomC1.setChiralOrder(1);
    atomC1.setSymbol("C");
    molecule.createBond(atomN, atomC1, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC1, atomC3, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomO1 = molecule.createAtom();
    atomO1.setSymbol("O");
    molecule.createBond(atomC3, atomO1, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomO2 = molecule.createAtom();
    atomO2.setSymbol("O");
    molecule.createBond(atomC3, atomO2, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomN);
    addHydrogen(molecule, atomN);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomO2);
    checkMolecule("N[C@H](C)C(=O)O", molecule);
  }
  public void testChapter7_14() {    // Test O[C@H]1CCCC[C@H]1O
    testChapter1_17();
  }
  public void testChapter7_15() {    // Test C1C[C@H]2CCCC[C@H]2CC1
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomC1, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setChiralClass("");
    atomC3.setChiralOrder(1);
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_SINGLE);
    molecule.createBond(atomC3, null, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC5 = molecule.createAtom();
    atomC5.setSymbol("C");
    molecule.createBond(atomC4, atomC5, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC6 = molecule.createAtom();
    atomC6.setSymbol("C");
    molecule.createBond(atomC5, atomC6, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC7 = molecule.createAtom();
    atomC7.setSymbol("C");
    molecule.createBond(atomC6, atomC7, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC8 = molecule.createAtom();
    atomC8.setChiralClass("");
    atomC8.setChiralOrder(1);
    atomC8.setSymbol("C");
    molecule.createBond(atomC7, atomC8, SmilesBond.TYPE_SINGLE);
    atomC3.getBond(1).setAtom2(atomC8);
    atomC8.addBond(atomC3.getBond(1));
    SmilesAtom atomC9 = molecule.createAtom();
    atomC9.setSymbol("C");
    molecule.createBond(atomC8, atomC9, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC0 = molecule.createAtom();
    atomC0.setSymbol("C");
    molecule.createBond(atomC9, atomC0, SmilesBond.TYPE_SINGLE);
    atomC1.getBond(0).setAtom2(atomC0);
    atomC0.addBond(atomC1.getBond(0));
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC1);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC2);
    addHydrogen(molecule, atomC3);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC5);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC6);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC7);
    addHydrogen(molecule, atomC8);
    addHydrogen(molecule, atomC9);
    addHydrogen(molecule, atomC9);
    addHydrogen(molecule, atomC0);
    addHydrogen(molecule, atomC0);
    checkMolecule("C1C[C@H]2CCCC[C@H]2CC1", molecule);
  }
  public void testChapter7_16() {    // Test OC(Cl)=[C@]=C(C)F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomO, atomC1, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomC1, atomCl, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setChiralClass("");
    atomC2.setChiralOrder(1);
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    molecule.createBond(atomC3, atomF, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    checkMolecule("OC(Cl)=[C@]=C(C)F", molecule);
  }
  public void testChapter7_17() {    // Test OC(Cl)=[C@AL1]=C(C)F
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    SmilesAtom atomC1 = molecule.createAtom();
    atomC1.setSymbol("C");
    molecule.createBond(atomO, atomC1, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomC1, atomCl, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomC2 = molecule.createAtom();
    atomC2.setChiralClass("AL");
    atomC2.setChiralOrder(1);
    atomC2.setSymbol("C");
    molecule.createBond(atomC1, atomC2, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC3 = molecule.createAtom();
    atomC3.setSymbol("C");
    molecule.createBond(atomC2, atomC3, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomC4 = molecule.createAtom();
    atomC4.setSymbol("C");
    molecule.createBond(atomC3, atomC4, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    molecule.createBond(atomC3, atomF, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomO);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    addHydrogen(molecule, atomC4);
    checkMolecule("OC(Cl)=[C@AL1]=C(C)F", molecule);
  }
  public void testChapter7_18() {    // Test F[Po@SP1](Cl)(Br)I
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    SmilesAtom atomPo = molecule.createAtom();
    atomPo.setChiralClass("SP");
    atomPo.setChiralOrder(1);
    atomPo.setSymbol("Po");
    molecule.createBond(atomF, atomPo, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomPo, atomCl, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomBr = molecule.createAtom();
    atomBr.setSymbol("Br");
    molecule.createBond(atomPo, atomBr, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomI = molecule.createAtom();
    atomI.setSymbol("I");
    molecule.createBond(atomPo, atomI, SmilesBond.TYPE_SINGLE);
    checkMolecule("F[Po@SP1](Cl)(Br)I", molecule);
  }
  public void testChapter7_19() {    // Test O=C[As@](F)(Cl)(Br)S
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    molecule.createBond(atomO, atomC, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomAs = molecule.createAtom();
    atomAs.setChiralClass("");
    atomAs.setChiralOrder(1);
    atomAs.setSymbol("As");
    molecule.createBond(atomC, atomAs, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    molecule.createBond(atomAs, atomF, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomAs, atomCl, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomBr = molecule.createAtom();
    atomBr.setSymbol("Br");
    molecule.createBond(atomAs, atomBr, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomS = molecule.createAtom();
    atomS.setSymbol("S");
    molecule.createBond(atomAs, atomS, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomS);
    checkMolecule("O=C[As@](F)(Cl)(Br)S", molecule);
  }
  public void testChapter7_20() {    // Test O=C[Co@](F)(Cl)(Br)(I)S
    SmilesMolecule molecule = new SmilesMolecule();
    SmilesAtom atomO = molecule.createAtom();
    atomO.setSymbol("O");
    SmilesAtom atomC = molecule.createAtom();
    atomC.setSymbol("C");
    molecule.createBond(atomO, atomC, SmilesBond.TYPE_DOUBLE);
    SmilesAtom atomCo = molecule.createAtom();
    atomCo.setChiralClass("");
    atomCo.setChiralOrder(1);
    atomCo.setSymbol("Co");
    molecule.createBond(atomC, atomCo, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomF = molecule.createAtom();
    atomF.setSymbol("F");
    molecule.createBond(atomCo, atomF, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomCl = molecule.createAtom();
    atomCl.setSymbol("Cl");
    molecule.createBond(atomCo, atomCl, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomBr = molecule.createAtom();
    atomBr.setSymbol("Br");
    molecule.createBond(atomCo, atomBr, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomI = molecule.createAtom();
    atomI.setSymbol("I");
    molecule.createBond(atomCo, atomI, SmilesBond.TYPE_SINGLE);
    SmilesAtom atomS = molecule.createAtom();
    atomS.setSymbol("S");
    molecule.createBond(atomCo, atomS, SmilesBond.TYPE_SINGLE);
    addHydrogen(molecule, atomC);
    addHydrogen(molecule, atomS);
    checkMolecule("O=C[Co@](F)(Cl)(Br)(I)S", molecule);
  }
  
  /**
   * Check that the SMILES parsing is correct
   * 
   * @param smiles SMILES string
   * @param expected SMILES molecule
   */
  private static void checkMolecule(String smiles, SmilesMolecule expected) {
    try {
      SmilesParser parser = new SmilesParser();
      SmilesMolecule molecule = parser.parseSmiles(smiles);
      assertTrue(areMoleculeEquals(molecule, expected));
    } catch (InvalidSmilesException e) {
      fail("InvalidSmilesException: " + e.getMessage());
    }
  }
  
  /**
   * Adds an hydrogen
   * 
   * @param molecule Molecule in which the hydrogen is added
   * @param bonded Other atom to bond to
   */
  private void addHydrogen(SmilesMolecule molecule, SmilesAtom bonded) {
    SmilesAtom atomH = molecule.createAtom();
    atomH.setSymbol("H");
    if (bonded != null) {
      molecule.createBond(bonded, atomH, SmilesBond.TYPE_SINGLE);
    }
  }
  
  /**
   * Compares two SmilesMolecule
   * 
   * @param molecule1 Molecule 1
   * @param molecule2 Molecule 2
   * @return true if they are equal
   */
  private static boolean areMoleculeEquals(
          SmilesMolecule molecule1,
          SmilesMolecule molecule2) {
    if ((molecule1 == null) || (molecule2 == null)) {
      System.out.println("Molecule null");
      return false;
    }
    if (molecule1.getAtomsCount() != molecule2.getAtomsCount()) {
      System.out.println(
          "Atoms count (" +
          molecule1.getAtomsCount() + "," +
          molecule2.getAtomsCount() + ")");
      return false;
    }
    for (int i = 0; i < molecule1.getAtomsCount(); i++) {
      SmilesAtom atom1 = molecule1.getAtom(i);
      SmilesAtom atom2 = molecule2.getAtom(i);
      if ((atom1 == null) || (atom2 == null)) {
        System.out.println("Atom " + i + " null");
        return false;
      }
      if (atom1.getAtomicMass() != atom2.getAtomicMass()) {
        System.out.println(
            "Atom " + i + " atomic mass (" +
            atom1.getAtomicMass() + "," +
            atom2.getAtomicMass() + ")");
        return false;
      }
      if (atom1.getBondsCount() != atom2.getBondsCount()) {
        System.out.println(
            "Atom " + i + " bonds count (" +
            atom1.getBondsCount() + "," +
            atom2.getBondsCount() + ")");
        return false;
      }
      for (int j = 0; j < atom1.getBondsCount(); j++) {
        SmilesBond bond1 = atom1.getBond(j);
        SmilesBond bond2 = atom2.getBond(j);
        if ((bond1 == null) || (bond2 == null)) {
          System.out.println(
              "Atom " + i + ", bond " + j + " null (" +
              bond1 + "," + bond2 + ")");
          return false;
        }
        if (bond1.getBondType() != bond2.getBondType()) {
          System.out.println(
              "Atom " + i + ", bond " + j + " bond type (" +
              bond1.getBondType() + "," +
              bond2.getBondType() + ")");
          return false;
        }
        if (bond1.getAtom1().getNumber() != bond2.getAtom1().getNumber()) {
          System.out.println(
              "Atom " + i + ", bond " + j + " atom1 number (" +
              bond1.getAtom1().getNumber() + "," +
              bond2.getAtom1().getNumber() + ")");
          return false;
        }
        if (bond1.getAtom2().getNumber() != bond2.getAtom2().getNumber()) {
          System.out.println(
              "Atom " + i + ", bond " + j + " atom2 number (" +
              bond1.getAtom2().getNumber() + "," +
              bond2.getAtom2().getNumber() + ")");
          return false;
        }
      }
      if (atom1.getCharge() != atom2.getCharge()) {
        System.out.println(
            "Atom " + i + " charge (" +
            atom1.getCharge() + "," +
            atom2.getCharge() + ")");
        return false;
      }
      if (atom1.getChiralClass() == null) {
        if (atom2.getChiralClass() != null) {
          System.out.println(
              "Atom " + i + " chiral class (" +
              atom1.getChiralClass() + "," +
              atom2.getChiralClass() + ")");
          return false;
        }
      } else if (!atom1.getChiralClass().equals(atom2.getChiralClass())) {
        System.out.println(
            "Atom " + i + " chiral class (" +
            atom1.getChiralClass() + "," +
            atom2.getChiralClass() + ")");
        return false;
      }
      if (atom1.getChiralOrder() != atom2.getChiralOrder()) {
        System.out.println(
            "Atom " + i + " chiral order (" +
            atom1.getChiralOrder() + "," +
            atom2.getChiralOrder() + ")");
        return false;
      }
      if (!atom1.getSymbol().equals(atom2.getSymbol())) {
        System.out.println(
            "Atom " + i + " symbol (" +
            atom1.getSymbol() + "," +
            atom2.getSymbol() + ")");
        return false;
      }
    }
    return true;
  }
  
}
