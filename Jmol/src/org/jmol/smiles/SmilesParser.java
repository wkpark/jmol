/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

package org.jmol.smiles;

/**
 * Parses a SMILES String to create a SmilesMolecule.
 * The SMILES specification has been found at http://www.daylight.com/smiles/.
 * 
 * Currently this parser supports only parts of the SMILES specification.
 * 
 * An example on how to use it:
 * <pre>
 * try {
 *   SmilesParser sp = new SmilesParser();
 *   SmilesMolecule sm = sp.parseSmiles("CC(C)C(=O)O");
 *   // Use the resulting molecule 
 * } catch (InvalidSmilesException e) {
 *   // Exception management
 * }
 * </pre>
 * 
 * @see <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>
 */
public class SmilesParser {

  private SmilesBond[] ringBonds;
  
  /**
   * SmilesParser constructor 
   */
  public SmilesParser() {
    ringBonds = null;
  }

  /**
   * Parse a SMILES String
   * 
   * @param smiles SMILES String
   * @return Molecule corresponding to smiles
   * @throws InvalidSmilesException
   */
  public SmilesMolecule parseSmiles(String smiles) throws InvalidSmilesException {
    if (smiles == null) {
      throw new InvalidSmilesException("SMILES expressions must not be null");
    }

    // First pass
    SmilesMolecule molecule = new SmilesMolecule();
    parseSmiles(molecule, smiles, null);
    
    // Implicit hydrogren creation
    for (int i = 0; i< molecule.getAtomsCount(); i++) {
      SmilesAtom atom = molecule.getAtom(i);
      atom.createMissingHydrogen(molecule);
    }

    // Check for rings
    if (ringBonds != null) {
      for (int i = 0; i < ringBonds.length; i++) {
        if (ringBonds[i] != null) {
          throw new InvalidSmilesException("Open ring");
        }
      }
    }

    return molecule;
  }

  /**
   * Parse a part of a SMILES String
   *
   * @param molecule Resulting molecule 
   * @param smiles SMILES String
   * @param currentAtom Current atom
   * @throws InvalidSmilesException
   */
  private void parseSmiles(
      SmilesMolecule molecule,
      String         smiles,
      SmilesAtom     currentAtom) throws InvalidSmilesException {
    if ((smiles == null) || (smiles.length() == 0)) {
      return;
    }

    // Branching
    int index = 0;
    char firstChar = smiles.charAt(index);
    if (firstChar == '(') {
      index++;
      int currentIndex = index;
      int parenthesisCount = 1;
      while ((currentIndex < smiles.length()) &&
             (parenthesisCount > 0)) {
        switch (smiles.charAt(currentIndex)) {
        case '(':
          parenthesisCount++;
          break;
        case ')':
          parenthesisCount--;
          break;
        }
        currentIndex++;
      }
      if (parenthesisCount != 0) {
        throw new InvalidSmilesException("Unbalanced parenthesis");
      }
      String subSmiles = smiles.substring(index, currentIndex - 1);
      parseSmiles(molecule, subSmiles, currentAtom);
      index = currentIndex;
    }

    // Bonds
    firstChar = smiles.charAt(index);
    int bondType = SmilesBond.getBondTypeFromCode(firstChar);
    if (bondType != SmilesBond.TYPE_UNKOWN) {
      if (currentAtom == null) {
        throw new InvalidSmilesException("Bond without a previous atom");
      }
      index++;
    }

    // Atom
    firstChar = smiles.charAt(index);
    if ((firstChar >= '0') && (firstChar <= '9')) {
      // Ring
      String subSmiles = smiles.substring(index, index + 1);
      parseRing(molecule, subSmiles, currentAtom, bondType);
      index++;
    } else if (firstChar == '%') {
      // Ring
      index++;
      if ((smiles.charAt(index) < 0) || (smiles.charAt(index) > 9)) {
        throw new InvalidSmilesException("Ring number must follow the % sign");
      }
      int currentIndex = index;
      while ((currentIndex < smiles.length()) &&
             (smiles.charAt(currentIndex) >= '0') &&
             (smiles.charAt(currentIndex) <= '9')) {
        currentIndex++;
      }
      String subSmiles = smiles.substring(index, currentIndex);
      parseRing(molecule, subSmiles, currentAtom, bondType);
      index = currentIndex;
    } else if (firstChar == '[') {
      // Atom definition
      index++;
      int currentIndex = index;
      while ((currentIndex < smiles.length()) &&
             (smiles.charAt(currentIndex) != ']')) {
        currentIndex++;
      }
      if (currentIndex >= smiles.length()) {
        throw new InvalidSmilesException("Unmatched [");
      }
      String subSmiles = smiles.substring(index, currentIndex);
      currentAtom = parseAtom(molecule, subSmiles, currentAtom, bondType, true);
      index = currentIndex + 1;
    } else if (((firstChar >= 'a') && (firstChar <= 'z')) ||
               ((firstChar >= 'A') && (firstChar <= 'Z')) ||
			   (firstChar == '*')) {
      // Atom definition
      int size = 1;
      if (index + 1 < smiles.length()) {
        char secondChar = smiles.charAt(index + 1);
        if ((firstChar >= 'A') && (firstChar <= 'Z') &&
            (secondChar >= 'a') && (secondChar <= 'z')) {
          size = 2;
        }
      }
      String subSmiles = smiles.substring(index, index + size);
      currentAtom = parseAtom(molecule, subSmiles, currentAtom, bondType, false);
      index += size;
    }

    // Next part of the SMILES String
    if (index < smiles.length()) {
      String subSmiles = smiles.substring(index);
      parseSmiles(molecule, subSmiles, currentAtom);
    }
  }

  /**
   * Parses an atom definition
   * 
   * @param molecule Resulting molecule 
   * @param smiles SMILES String
   * @param currentAtom Current atom
   * @param bondType Bond type
   * @param complete Indicates if is a complete definition (between [])
   * @return New atom
   * @throws InvalidSmilesException
   */
  private SmilesAtom parseAtom(
        SmilesMolecule molecule,
        String         smiles,
        SmilesAtom     currentAtom,
        int            bondType,
		boolean        complete) throws InvalidSmilesException {
    if ((smiles == null) || (smiles.length() == 0)) {
      throw new InvalidSmilesException("Empty atom definition");
    }

    // Atomic mass
  	int index = 0;
  	char firstChar = smiles.charAt(index);
  	Integer atomicMass = null;
  	if ((firstChar >= '0') && (firstChar <= '9')) {
  	  int currentIndex = index;
  	  while ((currentIndex < smiles.length()) &&
             (smiles.charAt(currentIndex) >= '0') &&
             (smiles.charAt(currentIndex) <= '9')) {
  	    currentIndex++;
  	  }
  	  String sub = smiles.substring(index, currentIndex);
  	  try {
  	    atomicMass = Integer.valueOf(sub);
  	  } catch (NumberFormatException e) {
  	    throw new InvalidSmilesException("Non numeric atomic mass");
  	  }
  	  index = currentIndex;
  	}

  	// Symbol
  	if (index >= smiles.length()) {
  	  throw new InvalidSmilesException("Missing atom symbol");
  	}
  	firstChar = smiles.charAt(index);
  	if (((firstChar < 'a') || (firstChar > 'z')) &&
        ((firstChar < 'A') || (firstChar > 'Z')) &&
        (firstChar != '*')) {
  	  throw new InvalidSmilesException("Unexpected atom symbol");
  	}
    int size = 1;
    if (index + 1 < smiles.length()) {
      char secondChar = smiles.charAt(index + 1);
      if ((firstChar >= 'A') && (firstChar <= 'Z') &&
          (secondChar >= 'a') && (secondChar <= 'z')) {
        size = 2;
      }
    }
    String atomSymbol = smiles.substring(index, index + size);
    index += size;

    // Chirality
    if (index < smiles.length()) {
      //TODO
    }

    // Hydrogen count
    Integer hydrogenCount = null;
    if (index < smiles.length()) {
      firstChar = smiles.charAt(index);
      if (firstChar == 'H') {
        index++;
        int currentIndex = index;
        while ((currentIndex < smiles.length()) &&
               (smiles.charAt(currentIndex) >= '0') &&
               (smiles.charAt(currentIndex) <= '9')) {
          currentIndex++;
        }
        if (currentIndex > index) {
          String sub = smiles.substring(index, currentIndex);
          try {
            hydrogenCount = Integer.valueOf(sub);
          } catch (NumberFormatException e) {
            throw new InvalidSmilesException("Non numeric hydrogen count");
          }
        } else {
          hydrogenCount = new Integer(1);
        }
        index = currentIndex;
      }
    }
    if ((hydrogenCount == null) && (complete)) {
      hydrogenCount = new Integer(0);
    }

    // Charge
    int charge = 0;
    if (index < smiles.length()) {
      firstChar = smiles.charAt(index);
      if ((firstChar == '+') || (firstChar == '-')) {
        int count = 1;
        index++;
        if (index < smiles.length()) {
          char nextChar = smiles.charAt(index);
          if ((nextChar >= '0') && (nextChar <= '9')) {
            int currentIndex = index;
            while ((currentIndex < smiles.length()) &&
                   (smiles.charAt(currentIndex) >= '0') &&
                   (smiles.charAt(currentIndex) <= '9')) {
              currentIndex++;
            }
            String sub = smiles.substring(index, currentIndex);
            try {
              count = Integer.parseInt(sub);
            } catch (NumberFormatException e) {
              throw new InvalidSmilesException("Non numeric charge");
            }
            index = currentIndex;
          } else {
            int currentIndex = index;
            while ((currentIndex < smiles.length()) &&
                   (smiles.charAt(currentIndex) == firstChar)) {
              currentIndex++;
              count++;
            }
            index = currentIndex;
          }
        }
        if (firstChar == '+') {
          charge = count;
        } else {
          charge = -count;
        }
      }
    }

    // Final check
    if (index < smiles.length()) {
      throw new InvalidSmilesException("Unexpected characters after atom definition");
    }

    // Create atom
    if (bondType == SmilesBond.TYPE_UNKOWN) {
      bondType = SmilesBond.TYPE_SINGLE;
    }
    SmilesAtom newAtom = molecule.createAtom();
    if (currentAtom != null) {
      molecule.createBond(currentAtom, newAtom, bondType);
    }
    newAtom.setSymbol(atomSymbol);
    newAtom.setAtomicMass(atomicMass);
    newAtom.setCharge(charge);
    newAtom.setHydrogenCount(hydrogenCount);
    return newAtom;
  }

  /**
   * Parses a ring definition
   * 
   * @param molecule Resulting molecule 
   * @param smiles SMILES String
   * @param currentAtom Current atom
   * @param bondType Bond type
   * @throws InvalidSmilesException
   */
  private void parseRing(
        SmilesMolecule molecule,
        String         smiles,
        SmilesAtom     currentAtom,
        int            bondType) throws InvalidSmilesException {
  	// Extracting ring number
    int ringNum = 0;
    try {
      ringNum = Integer.parseInt(smiles);
    } catch (NumberFormatException e) {
      throw new InvalidSmilesException("Non numeric ring identifier");
    }
    
    // Checking rings buffer is big enough
    if (ringBonds == null) {
      ringBonds = new SmilesBond[10];
      for (int i = 0; i < ringBonds.length; i++) {
        ringBonds[i] = null;
      }
    }
    if (ringNum >= ringBonds.length) {
      SmilesBond[] tmp = new SmilesBond[ringNum + 1];
      for (int i = 0; i < ringBonds.length; i++) {
        tmp[i] = ringBonds[i];
      }
      for (int i = ringBonds.length; i < tmp.length; i++) {
        tmp[i] = null;
      }
    }
    
    // Ring management
    if (ringBonds[ringNum] == null) {
      ringBonds[ringNum] = molecule.createBond(currentAtom, null, bondType);
    } else {
      if (bondType == SmilesBond.TYPE_UNKOWN) {
        bondType = ringBonds[ringNum].getBondType();
        if (bondType == SmilesBond.TYPE_UNKOWN) {
          bondType = SmilesBond.TYPE_SINGLE;
        }
      } else {
        if ((ringBonds[ringNum].getBondType() != SmilesBond.TYPE_UNKOWN) &&
            (ringBonds[ringNum].getBondType() != bondType)) {
          throw new InvalidSmilesException("Incoherent bond type for ring");
        }
      }
      ringBonds[ringNum].setBondType(bondType);
      ringBonds[ringNum].setAtom2(currentAtom);
      currentAtom.addBond(ringBonds[ringNum]);
      ringBonds[ringNum] = null;
    }
  }

  /*
  private static void outputMolecule(SmilesMolecule molecule) {
    for (int i = 0; i < molecule.getAtomsCount(); i++) {
      SmilesAtom atom = molecule.getAtom(i);
      System.out.print("Atom (" + i + "): " + atom.getSymbol());
      System.out.print("  :");
      for (int j = 0; j < atom.getBondsCount(); j++) {
        SmilesBond bond = atom.getBond(j);
        System.out.print(
            " " + bond.getAtom1().getNumber() +
            "-" + bond.getAtom2().getNumber() +
            "-" + bond.getBondType());
      }
      System.out.println();
    }
  }

  private static void testMolecule(String smiles) {
    try {
        SmilesParser parser = new SmilesParser();
        SmilesMolecule molecule = parser.parseSmiles(smiles);
        System.out.println("SMILES: " + smiles);
        outputMolecule(molecule);
      } catch (InvalidSmilesException e) {
        System.out.println("Erreur: " + e);
      }
  }

  public static void main(String[] args) {
    testMolecule("[S]");
    testMolecule("[Au]");
    testMolecule("C");
    testMolecule("P");
    testMolecule("S");
    testMolecule("Cl");
    testMolecule("[OH-]");
    testMolecule("[OH-1]");
    testMolecule("[Fe+2]");
    testMolecule("[Fe++]");
    testMolecule("[235U]");
    testMolecule("[*+2]");
    
    testMolecule("CC");
    testMolecule("C-C");
    testMolecule("[CH3]-[CH3]");
    testMolecule("C=O");
    testMolecule("C#N");
    testMolecule("C=C");
    testMolecule("C=CC=C");
    
    testMolecule("CC(C)C(=O)O");
    testMolecule("FC(F)F");
    testMolecule("C(F)(F)F");
    testMolecule("O=Cl(=O)(=O)[O-]");
    testMolecule("CCCC(C(=O)O)CCC");
    
    testMolecule("C1CCCCC1");
    testMolecule("C=1CCCCC1");
  }
  */
}
