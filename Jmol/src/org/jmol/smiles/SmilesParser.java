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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.smiles;

/**
 * Parses a SMILES String to create a <code>SmilesMolecule</code>.
 * The SMILES specification has been found at the
 * <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * An other explanation can be found in the
 * <a href="http://www.daylight.com/dayhtml/doc/theory/index.html">Daylight Theory Manual</a>. <br>
 * 
 * Currently this parser supports only parts of the SMILES specification. <br>
 * 
 * An example on how to use it:
 * <pre><code>
 * try {
 *   SmilesParser sp = new SmilesParser();
 *   SmilesMolecule sm = sp.parseSmiles("CC(C)C(=O)O");
 *   // Use the resulting molecule 
 * } catch (InvalidSmilesException e) {
 *   // Exception management
 * }
 * </code></pre>
 * 
 * @see <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>
 */
public class SmilesParser {

  protected boolean isSmarts;
  protected SmilesBond[] ringBonds;
  

  public SmilesParser(boolean isSmarts) {
    this.isSmarts = isSmarts;    
  }
  
  public static SmilesSearch getMolecule(boolean isSmarts, String pattern) throws InvalidSmilesException {
    return (new SmilesParser(isSmarts)).parse(pattern);
  }

  /**
   * Parses a SMILES String
   * 
   * @param pattern SMILES String
   * @return Molecule corresponding to <code>pattern</code>
   * @throws InvalidSmilesException
   */
   SmilesSearch parse(String pattern) throws InvalidSmilesException {
    if (pattern == null)
      throw new InvalidSmilesException("SMILES expressions must not be null");
    // First pass
    SmilesSearch molecule = new SmilesSearch();
    molecule.isSmarts = isSmarts;
    parseSmiles(molecule, pattern, null);

    if (!isSmarts)
      for (int i = molecule.patternAtomCount; --i >= 0; )
        if (!molecule.getAtom(i).setHydrogenCount(molecule))
          throw new InvalidSmilesException("unbracketed atoms must be one of: B C N O P S F Cl Br I");
            
    // Check for rings
    if (ringBonds != null)
      for (int i = 0; i < ringBonds.length; i++)
        if (ringBonds[i] != null)
          throw new InvalidSmilesException("Open ring");

    fixChirality(molecule);

    return molecule;
  }

  private void fixChirality(SmilesSearch molecule) throws InvalidSmilesException {
    for (int i = molecule.patternAtomCount; --i >= 0; ) {
      SmilesAtom sAtom = molecule.getAtom(i);
      int chiralClass = sAtom.getChiralClass();
      int nBonds = sAtom.getHydrogenCount();
      if (nBonds < 0 || nBonds == Integer.MAX_VALUE)
        nBonds = 0;
      nBonds += sAtom.getBondsCount();
      switch (chiralClass) {
      case SmilesAtom.CHIRALITY_DEFAULT:
        switch (nBonds) {
        case 2:
        case 4:
        case 5:
        case 6:
          chiralClass = nBonds;
          break;
        }
        break;
      case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
        if (nBonds != 4)
          chiralClass = SmilesAtom.CHIRALITY_DEFAULT;
        break;        
      case SmilesAtom.CHIRALITY_ALLENE:
      case SmilesAtom.CHIRALITY_OCTAHEDRAL:
      case SmilesAtom.CHIRALITY_TETRAHEDRAL:
      case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
        if (nBonds != chiralClass)
          chiralClass = SmilesAtom.CHIRALITY_DEFAULT;
        break;        
      }
      if (chiralClass == SmilesAtom.CHIRALITY_DEFAULT)
        throw new InvalidSmilesException("Incorrect number of bonds for chirality descriptor");
      sAtom.setChiralClass(chiralClass);
    }
  }

  /**
   * Parses a part of a SMILES String
   * 
   * @param molecule
   *          Resulting molecule
   * @param pattern
   *          SMILES String
   * @param currentAtom
   *          Current atom
   * @throws InvalidSmilesException
   */
  protected void parseSmiles(SmilesSearch molecule, String pattern,
                             SmilesAtom currentAtom)
      throws InvalidSmilesException {

    if (pattern == null || pattern.length() == 0)
      return;

    // Branching
    int index = 0;
    int len = pattern.length();
    char ch = pattern.charAt(0);
    if (ch == '(') {
      index++;
      int currentIndex = index;
      int parenthesisCount = 1;
      while ((currentIndex < len) && (parenthesisCount > 0)) {
        switch (pattern.charAt(currentIndex)) {
        case '(':
          parenthesisCount++;
          break;
        case ')':
          parenthesisCount--;
          break;
        }
        currentIndex++;
      }
      if (parenthesisCount != 0)
        throw new InvalidSmilesException("Unbalanced parenthesis");
      String subSmiles = pattern.substring(index, currentIndex - 1);
      parseSmiles(molecule, subSmiles, currentAtom);
      index = currentIndex;
      if (index >= len) {
        if (isSmarts)
          return;
        throw new InvalidSmilesException("Pattern must not end with ')'");
      }
    }

    // Bonds
    ch = pattern.charAt(index);
    int bondType = SmilesBond.getBondTypeFromCode(ch);
    if (bondType != SmilesBond.TYPE_UNKNOWN) {
      if (bondType == SmilesBond.TYPE_DIRECTIONAL_1 || bondType == SmilesBond.TYPE_DIRECTIONAL_2)
        molecule.haveBondStereochemistry = true;
      if (currentAtom == null) {
        throw new InvalidSmilesException("Bond without a previous atom");
      }
      index++;
    }

    // Atom
    ch = pattern.charAt(index);
    if (Character.isDigit(ch)) {
      // Ring
      String subSmiles = pattern.substring(index, index + 1);
      parseRing(molecule, subSmiles, currentAtom, bondType);
      index++;
    } else if (ch == '%') {
      // Ring
      index++;
      if ((pattern.charAt(index) < 0) || (pattern.charAt(index) > 9)) {
        throw new InvalidSmilesException("Ring number must follow the % sign");
      }
      int currentIndex = index;
      while (currentIndex < len
          && Character.isDigit(pattern.charAt(currentIndex)))
        currentIndex++;
      String subSmiles = pattern.substring(index, currentIndex);
      parseRing(molecule, subSmiles, currentAtom, bondType);
      index = currentIndex;
    } else if (ch == '[') {
      // Atom definition
      index++;
      int currentIndex = index;
      while ((currentIndex < len) && (pattern.charAt(currentIndex) != ']')) {
        currentIndex++;
      }
      if (currentIndex >= len) {
        throw new InvalidSmilesException("Unmatched [");
      }
      String subSmiles = pattern.substring(index, currentIndex);
      currentAtom = parseAtom(molecule, subSmiles, currentAtom, bondType, true);
      index = currentIndex + 1;
    } else if (ch == '*' || Character.isLetter(ch)) {
      // Atom definition
      int size = (index + 1 < len
          && Character.isLowerCase(pattern.charAt(index + 1)) ? 2 : 1);
      String subSmiles = pattern.substring(index, index + size);
      currentAtom = parseAtom(molecule, subSmiles, currentAtom, bondType, false);
      index += size;
    }

    // Next part of the SMILES String
    if (index == 0)
      throw new InvalidSmilesException("Unexpected character: "
          + pattern.charAt(0));
    if (index < len)
      parseSmiles(molecule, pattern.substring(index), currentAtom);
  }

  /**
   * Parses an atom definition
   * 
   * @param molecule
   *          Resulting molecule
   * @param pattern
   *          SMILES String
   * @param currentAtom
   *          Current atom
   * @param bondType
   *          Bond type
   * @param complete
   *          Indicates if is a complete definition (between [])
   * @return New atom
   * @throws InvalidSmilesException
   */
  protected SmilesAtom parseAtom(SmilesSearch molecule, String pattern,
                                 SmilesAtom currentAtom, int bondType,
                                 boolean complete)
      throws InvalidSmilesException {
    if ((pattern == null) || (pattern.length() == 0)) {
      throw new InvalidSmilesException("Empty atom definition");
    }
    SmilesAtom newAtom = molecule.createAtom();

    pattern = checkCharge(pattern, newAtom);
    if (pattern.indexOf("@") >= 0) {
      molecule.haveAtomStereochemistry = true;
      pattern = checkChirality(pattern, newAtom);
    }
    int len = pattern.length();
    int index = 0;
    int pt = index;
    char ch = pattern.charAt(0);

    // isotope
    if (Character.isDigit(ch)) {
      while (pt < len && Character.isDigit(pattern.charAt(pt)))
        pt++;
      try {
        newAtom.setAtomicMass(Integer.parseInt(pattern.substring(index, pt)));
      } catch (NumberFormatException e) {
        throw new InvalidSmilesException("Non numeric atomic mass");
      }
      index = pt;
    }
    // Symbol
    if (index >= len)
      throw new InvalidSmilesException("Missing atom symbol");
    ch = pattern.charAt(index);
    if (ch != '*' && !Character.isLetter(ch))
      throw new InvalidSmilesException("Unexpected atom symbol");
    char nextChar = (index + 1 < len ? pattern.charAt(index + 1) : 'Z');
    int size = (Character.isLetter(ch)
        && Character.isLowerCase(nextChar) ? 2 : 1);
    if (size == 2 && nextChar == 'h' && index + 2 < len
        && Character.isDigit(pattern.charAt(index + 2)))
      size = 1;
    if (!newAtom.setSymbol(Character.toUpperCase(ch)
        + pattern.substring(index + 1, index + size)))
      throw new InvalidSmilesException("Invalid atom symbol");
    index += size;
    checkHydrogenCount(complete, pattern, index, newAtom);
    
    // Final check

    if (bondType == SmilesBond.TYPE_UNKNOWN)
      bondType = SmilesBond.TYPE_SINGLE;
    if ((currentAtom != null) && (bondType != SmilesBond.TYPE_NONE))
      molecule.createBond(currentAtom, newAtom, bondType);
    return newAtom;
  }

  private void checkHydrogenCount(boolean complete, String pattern, int index,
                                  SmilesAtom newAtom)
      throws InvalidSmilesException {
    // Hydrogen count
    int hydrogenCount = Integer.MIN_VALUE;
    int len = pattern.length();
    if (index < len) {
      char ch = pattern.charAt(index);
      if (ch == 'H' || isSmarts && ch == 'h') {
        index++;
        int pt = index;
        while (pt < len && Character.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            hydrogenCount = Integer.parseInt(pattern.substring(index, pt));
          } catch (NumberFormatException e) {
            throw new InvalidSmilesException("Non numeric hydrogen count");
          }
        } else {
          hydrogenCount = 1;
        }
        index = pt;
      }
      if (index < len)
        throw new InvalidSmilesException(
            "Unexpected characters after atom definition: "
                + pattern.substring(index));
      if (ch == 'h') // minimum count
        hydrogenCount = -hydrogenCount;
    }
    if (hydrogenCount == Integer.MIN_VALUE && complete)
      hydrogenCount = Integer.MAX_VALUE;
    newAtom.setHydrogenCount(hydrogenCount);
  }

  private String checkChirality(String pattern, SmilesAtom newAtom)
      throws InvalidSmilesException {
    int chiralClass = 0;
    int chiralOrder = Integer.MIN_VALUE;
    int pt0 = pattern.indexOf('@');
    int len = pattern.length();
    int ch;
    int index = pt0;
    chiralClass = SmilesAtom.CHIRALITY_DEFAULT;
    chiralOrder = 1;
    if (++index < len) {
      switch (ch = pattern.charAt(index)) {
      case '@':
        chiralOrder = 2;
        index++;
        break;
      case 'H':
        break;
      case 'A':
      case 'O':
      case 'S':
      case 'T':
        chiralClass = (index + 1 < len ? SmilesAtom.getChiralityClass(pattern
            .substring(index, index + 2)) : -1);
        index += 2;
        break;
      default:
        chiralOrder = (Character.isDigit(ch) ? 1 : -1);
      }
      int pt = index;
      if (chiralOrder == 1) {
        while (pt < len && Character.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            chiralOrder = Integer.parseInt(pattern.substring(index, pt));
          } catch (NumberFormatException e) {
            chiralOrder = -1;
          }
          index = pt;
        }
      }
      if (chiralOrder < 1 || chiralClass < 0)
        throw new InvalidSmilesException("Invalid chirality descriptor");
    }
    newAtom.setChiralClass(chiralClass);
    newAtom.setChiralOrder(chiralOrder);
    return pattern.substring(0, pt0) + pattern.substring(index);
  }

  private String checkCharge(String pattern, SmilesAtom newAtom) throws InvalidSmilesException {
    // Charge
    int pt = pattern.indexOf("-") + pattern.indexOf("+") + 1;
    if (pt < 0)
      return pattern;
    int len = pattern.length();
    char ch = pattern.charAt(pt);
    int count = 1;
    int index = pt + 1;
    int currentIndex = index;
    if (index < len) {
      char nextChar = pattern.charAt(index);
      if (Character.isDigit(nextChar)) {
        while (currentIndex < len && Character.isDigit(pattern.charAt(currentIndex)))
          currentIndex++;
        try {
          count = Integer.parseInt(pattern.substring(index, currentIndex));
        } catch (NumberFormatException e) {
          throw new InvalidSmilesException("Non numeric charge");
        }
      } else {
        while (currentIndex < len
            && pattern.charAt(currentIndex) == ch) {
          currentIndex++;
          count++;
        }
      }
      index = currentIndex;
    }
    newAtom.setCharge(ch == '+' ? count : -count);
    return pattern.substring(0, pt) + pattern.substring(index, currentIndex);
  }

  /**
   * Parses a ring definition
   * 
   * @param molecule Resulting molecule 
   * @param pattern SMILES String
   * @param currentAtom Current atom
   * @param bondType Bond type
   * @throws InvalidSmilesException
   */
  protected void parseRing(
        SmilesSearch molecule,
        String         pattern,
        SmilesAtom     currentAtom,
        int            bondType) throws InvalidSmilesException {
  	// Extracting ring number
    int ringNum = 0;
    try {
      ringNum = Integer.parseInt(pattern);
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
      if (bondType == SmilesBond.TYPE_UNKNOWN) {
        bondType = ringBonds[ringNum].getBondType();
        if (bondType == SmilesBond.TYPE_UNKNOWN) {
          bondType = SmilesBond.TYPE_SINGLE;
        }
      } else {
        if ((ringBonds[ringNum].getBondType() != SmilesBond.TYPE_UNKNOWN) &&
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

}
