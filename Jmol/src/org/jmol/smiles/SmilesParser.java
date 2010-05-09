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

import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JmolConstants;

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

  /*
   * 3D-SMARTS -- Bob Hanson hansonr@stolaf.edu 5/10/2010
   * 
   * An adaptation of SMARTS for 3D molecular atom selection.
   * 
   * Comparision to Daylight SMARTS:
   * 
   * -- defines "aromatic" unambiguously and strictly geometrically as follows:
   * 
   *     -- an "aromatic atom" is defined to be any atom within an aromatic ring
   *        (no restriction to element)
   *     -- "aromatic ring" is defined as a ring of 3-7 atoms for which
   *          (a) the normals to all combinations of three ring atoms as well as 
   *          (b) one normal for each subtituent defined for the plane formed
   *              by that substituent, its connecting ring atom, and one other ring atom
   *         are all parallel or antiparallel to each other.
   * 
   * -- nesting ("recursive" SMARTS") implemented: using [C&$(C[$(aaaO);$(aaC)])]
   * 
   * -- all atom logic implemented: [X,!X,X&X,X&X&X;X&X] etc.
   * 
   * -- "&" is completely optional: [13CH2] same as [13&C&H2]
   * 
   * -- allows any order of primitives; "H1" interpreted as "one H atom"
   *       [H2C13] same as [13CH2]
   * 
   * -- bracketed H atoms -- as in [CH2] -- are not selected
   * -- unbracketed H atoms -- as in HCCC -- are selected
   * 
   * -- adds ambiguous isotope: [C13?] -- "C13 or C with undesignated isotope"
   * 
   * -- does NOT implement "zero-level parentheses", since the match is 
   *    always only within a given model (even though one might use . for a not-connected indicator)
   * 
   * -- does NOT implement "?" in atom stereochemistry ("chirality") because 
   *    3D structures are always defined stereochemically.
   * 
   * -- does NOT implement "?" for bond stereochemistry, as 3D structures
   *    always defined stereochemically
   *    
   *   [smarts] == [node][connections] 
   *   [branch] == "(" [smarts] ")" 
   *   [connections] == [connection] | NULL }
   *   [connection] == { [branch] | [bond] [node] } [connections]
   *   [node] == { [atomExpression] | [atomExpression] [ringPoint] }
   *   [ringPoint] == { "%" [digits] | [digit] }
   *      # note: all ringPoints must have a second matching ringPoint 
   *   [bond] == { "-" | "=" | "#" | "." | "/" | "\\" | ":" | "~" | "@" | NULL
   *
   * 
   *   [atomExpression] = { [unbracketedAtomType] | "[" [bracketedExpression] "]" }
   *   
   *   [unbracketedAtomType] == [atomType] 
   *                                 & ! { "Na" | "Ca" | "Ba" | "Pa" | "Sc" | "Ac" }
   *      # note: These elements Xy are instead interpreted as "X" "y", a single-letter
   *      #       element followed by an aromatic atom
   *        
   *   [atomType] == { [validElementSymbol] | "A" | [aromaticType] | "*" }
   *   [validElementSymbol] == (see org.jmol.viewer.JmolConstants.elementSymbols; including Xx)
   *   [aromaticType] == "a" | [validElementSymbol].toLowerCase() 
   *                                 & ! { "na" | "ca" | "ba" | "pa" | "sc" | "ac" }
   *       
   *   [bracketedExpression] == { [orSet] | [orSet] ";" [andSet] } 
   *   
   *   [orSet] == { [andSet] | [andSet] "," [andSet] }
   *   [andSet] == { [primitiveDescriptor] 
   *                              | [primitiveDescriptor] [primitiveDescriptor] 
   *                              | [primitiveDescriptor] "&" [andSet] }
   *                              
   *   [primitiveDescriptor] == { "!" [primitive] | [primitive] }
   *   [primitive] == { [isotope] | [atomType] | [charge] | [stereochemistry]
   *                              | [A_Prop] | [D_Prop] | [H_Prop] | [h_Prop] 
   *                              | [R_Prop] | [r_Prop] | [v_Prop] | [X_Prop]
   *                              | [x_Prop] | [nestedExpression] }
   *   [isotope] == [digits] | [digits] "?"
   *       # note -- isotope mass may come before or after element symbol, 
   *       #         EXCEPT "H1" which must be parsed as "an atom with a single H"
   *   [charge] == { "-" [digits] | "+" [digits] | [plusSet] | [minusSet] }
   *   [plusSet] == { "+" | "+" [plusSet] }
   *   [minusSet] == { "-" | "-" [minusSet] }
   *   [stereochemistry] == { "@"           # anticlockwise
   *                              | "@@"    # clockwise
   *                              | "@" [stereochemistryDescriptor] 
   *                              | "@@" [stereochemistryDescriptor] }
   *   [stereochemistryDescriptor] == [stereoClass] [stereoOrder]
   *   [stereoClass] == { "AL" | "TH" | "SP" | "TP" | "OH" }
   *   [stereoOrder] == [digits]
   *       # note -- "?" here (unspecified) is not relevant in 3D-SMARTS 
   *   
   *   [A_Prop] == "#" [digits]           # elemental atomic number
   *   [D_Prop] == "D" [digits]           # degree -- total number of connections excluding hmod
   *   [H_Prop] == { "H" [digits] | "H" } # exact hydrogen count 
   *   [h_Prop] == "h" [digits]           # implicit hydrogens -- at least this number
   *   [R_Prop] == "R" [digits]           # SSSR ring membership; e.g. "R2" indicates "in two rings"; "R0" indicates "not in any ring" 
   *   [r_Prop] == "r" [digits]           # ring size in smallest SSSR ring of size [digits]
   *   [v_Prop] == "v" [digits]           # valence -- total bond order (counting double as 2, e.g.)
   *   [X_Prop] == "X" [digits]           # connectivity -- total number of connections (includes hmod)
   *   [x_Prop] == "x" [digits]           # ring connectivity -- total ring connections ?
   *   
   *   [nestedExpression] == "$(" + [atomExpression] + ")"
   * 
   *   [digits] = { [digit] | [digit] [digits] }
   *   [digit] = { "0" | "1" | "2" | "3" | "4" | "5" | "6" | 7" | "8" | "9" }
   *
   *
   *
   * Bob Hanson, Jmol 12.0.RC10, 5/9/2010
   * 
   */
  
  private boolean isSmarts;
  private SmilesBond[] ringBonds;
  

  public static SmilesSearch getMolecule(boolean isSmarts, String pattern) throws InvalidSmilesException {
    return (new SmilesParser(isSmarts)).parse(pattern);
  }

  private SmilesParser(boolean isSmarts) {
    this.isSmarts = isSmarts;    
  }
  
  /**
   * Parses a SMILES String
   * 
   * @param pattern SMILES String
   * @return Molecule corresponding to <code>pattern</code>
   * @throws InvalidSmilesException
   */
   SmilesSearch parse(String pattern) throws InvalidSmilesException {
    if (Logger.debugging)
      Logger.debug("Smiles Parser: " + pattern);
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
  private void parseSmiles(SmilesSearch molecule, String pattern,
                           SmilesAtom currentAtom)
      throws InvalidSmilesException {

    if (pattern == null || pattern.length() == 0)
      return;

    if (pattern.indexOf("$(") >= 0)
      pattern = parseNested(molecule, pattern);

    int[] ret = new int[1];
    int index = 0;
    int len = pattern.length();
    char ch = pattern.charAt(0);

    // Branch

    if (ch == '(') {
      if (currentAtom == null)
        throw new InvalidSmilesException("No previous atom for branch");
      int pt = index++;
      int parenthesisCount = 1;
      while (++pt < len && parenthesisCount > 0) {
        switch (pattern.charAt(pt)) {
        case '(':
          parenthesisCount++;
          break;
        case ')':
          parenthesisCount--;
          break;
        }
      }
      if (parenthesisCount != 0)
        throw new InvalidSmilesException("Unbalanced parenthesis");
      String subSmiles = pattern.substring(index, pt - 1);
      parseSmiles(molecule, subSmiles, currentAtom);
      index = pt;
      if (index >= len) {
        if (isSmarts)
          return;
        throw new InvalidSmilesException("Pattern must not end with ')'");
      }
    }

    // Bond

    ch = pattern.charAt(index);
    int bondType = SmilesBond.getBondTypeFromCode(ch);
    if (bondType != SmilesBond.TYPE_UNKNOWN) {
      if (!isSmarts && "-=#:".indexOf(ch) < 0)
        throw new InvalidSmilesException("SMARTS bond type " + ch
            + " not allowed in SMILES");
      if (currentAtom == null && bondType != SmilesBond.TYPE_NONE)
        throw new InvalidSmilesException("Bond without a previous atom");
      switch (bondType) {
      case SmilesBond.TYPE_DIRECTIONAL_1:
      case SmilesBond.TYPE_DIRECTIONAL_2:
        molecule.haveBondStereochemistry = true;
        break;
      case SmilesBond.TYPE_ANY: // ~
        bondType = SmilesBond.TYPE_UNKNOWN;
        break;
      }
      index++;
    }

    // Atom or ring

    ch = pattern.charAt(index);
    if (Character.isDigit(ch)) {
      // [ringPoint]
      parseRing(molecule, ch - '0', currentAtom, bondType);
      index++;
    } else if (ch == '%') {
      // [ringPoint]
      index = getDigits(pattern, index + 1, ret);
      if (ret[0] < 1)
        throw new InvalidSmilesException(
            "Ring number > 0 must follow the % sign");
      parseRing(molecule, ret[0], currentAtom, bondType);
    } else if (ch == '[') {
      // [bracketedExpression]
      index++;
      int currentIndex = index;
      while ((currentIndex < len) && (pattern.charAt(currentIndex) != ']')) {
        currentIndex++;
      }
      if (currentIndex >= len) {
        throw new InvalidSmilesException("Unmatched [");
      }
      String subSmiles = pattern.substring(index, currentIndex);
      currentAtom = parseAtom(molecule, null, subSmiles, currentAtom, bondType,
          true, false);
      index = currentIndex + 1;
    } else if (ch == '*' || Character.isLetter(ch)) {
      // [atomType]
      char ch2 = (isSmarts && Character.isUpperCase(ch) ? getChar(pattern,
          index + 1) : '\0');
      if (!Character.isLowerCase(ch2)
          || JmolConstants.elementNumberFromSymbol(pattern.substring(index,
              index + 2)) == 0)
        ch2 = '\0';
      // guess at some ambiguous SMARTS strings:
      if (ch2 != '\0'
          && "NA CA BA PA SC AC".indexOf(pattern.substring(index, index + 2)) >= 0) {
        Logger.error("Note: " + ch + ch2 + " NOT interpreted as an element");
        ch2 = '\0';
      }
      int size = (Character.isUpperCase(ch) && Character.isLowerCase(ch2) ? 2
          : 1);
      String subSmiles = pattern.substring(index, index + size);
      currentAtom = parseAtom(molecule, null, subSmiles, currentAtom, bondType,
          false, false);
      index += size;
    }

    if (index == 0)
      throw new InvalidSmilesException("Unexpected character: "
          + pattern.charAt(0));
    
    // [connections]
    
    if (index < len)
      parseSmiles(molecule, pattern.substring(index), currentAtom);
  }

  private String parseNested(SmilesSearch molecule, String pattern) throws InvalidSmilesException {
    int index;
    while ((index = pattern.lastIndexOf("$(")) >= 0) {
      int pCount = 0;
      for (int pt = index; pt < pattern.length(); pt++) {
        switch(pattern.charAt(pt)) {
        case '(':
          pCount++;
          break;
        case ')':
          pCount--;
          if (pCount == 0) {
            int i = molecule.addNested(pattern.substring(index + 2, pt));
            pattern = pattern.substring(0, index) + "_" + i + pattern.substring(pt + 1);
          }
          break;
        }
      }
      if (pCount != 0)
        throw new InvalidSmilesException("unmatched () in $(...)");
    }
    return pattern;
  }

  /**
   * Parses an atom definition
   * 
   * @param molecule
   *          Resulting molecule
   * @param atomSet
   * @param pattern
   *          SMILES String
   * @param currentAtom
   *          Current atom
   * @param bondType
   *          Bond type
   * @param isBracketed
   *          Indicates if is a isBracketed definition (between [])
   * @param isPrimitive
   *          TODO
   * @return New atom
   * @throws InvalidSmilesException
   */
  private SmilesAtom parseAtom(SmilesSearch molecule, SmilesAtom atomSet,
                               String pattern, SmilesAtom currentAtom,
                               int bondType, boolean isBracketed,
                               boolean isPrimitive)
      throws InvalidSmilesException {
    if (pattern == null || pattern.length() == 0)
      throw new InvalidSmilesException("Empty atom definition");

    SmilesAtom newAtom = (atomSet == null ? molecule.addAtom()
        : isPrimitive ? atomSet.addPrimitive() : atomSet.addAtomOr());

    int index = 0;
    int pt;
    String props = "";
    pt = pattern.indexOf(";");
    boolean haveOr = (pattern.indexOf(",") >= 0);
    if (pt >= 0) {
      if (!isSmarts)
        throw new InvalidSmilesException(
            "[;] notation only valid with SMARTS, not SMILES");
      props = "&" + pattern.substring(pt + 1);
      pattern = pattern.substring(0, pt);
      if (!haveOr) {
        pattern += props;
        props = "";
      }      
    }
    if (haveOr) {
      if (!isSmarts)
        throw new InvalidSmilesException(
            "[,] notation only valid with SMARTS, not SMILES");
      pattern += ",";
      while ((pt = pattern.indexOf(",", index)) >= 0 && pt < pattern.length()) {
        parseAtom(molecule, newAtom, pattern.substring(index, pt) + props,
            null, 0, true, false);
        index = pt + 1;
      }
    } else if (pattern.indexOf("&") >= 0) {
      // process primitive
      if (!isSmarts)
        throw new InvalidSmilesException(
            "[&] notation only valid with SMARTS, not SMILES");
      pattern += "&";
      while ((pt = pattern.indexOf("&", index)) >= 0 && pt < pattern.length()) {
        parseAtom(molecule, newAtom, pattern.substring(index, pt) + props, null, 0,
            true, true);
        index = pt + 1;
      }
    } else {
      int[] ret = new int[1];
      int len = pattern.length();
      char ch = pattern.charAt(0);

      boolean isNot = false;
      if (isSmarts && pattern.charAt(0) == '!') {
        index++;
        newAtom.not = isNot = true;
      }

      int hydrogenCount = Integer.MIN_VALUE;

      while (index < len) {
        ch = (pattern.charAt(index));
        char nextChar = getChar(pattern, index + 1);
        boolean haveSymbol = ((isPrimitive ? atomSet : newAtom).atomicNumber >= 0);
        if (Character.isDigit(ch)) {
          index = getDigits(pattern, index, ret);
          int mass = ret[0];
          if (mass == Integer.MIN_VALUE)
            throw new InvalidSmilesException("Non numeric atomic mass");
          if (getChar(pattern, index) == '?') {
            index++;
            mass = -mass; // or undefined
          }
          newAtom.setAtomicMass(mass);
        } else if ("-+@".indexOf(ch) >= 0 
            || ch == 'H' && (Character.isDigit(nextChar) 
                || haveSymbol && !isNot) 
            || "_DhRrvXx".indexOf(ch) >= 0 && Character.isDigit(nextChar)) {
          switch (ch) {
          default:
            throw new InvalidSmilesException("Invalid SMARTS primitive: " + pattern.substring(index));
          case '_':  // $(...) nesting
            index = getDigits(pattern, index + 1, ret);
            newAtom.iNested = ret[0];
            break;
          case 'D':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setDegree(ret[0]);
            break;
          case 'H':
            index = getDigits(pattern, index + 1, ret);
            hydrogenCount = (ret[0] == Integer.MIN_VALUE ? 1 : ret[0]);
            break;
          case 'h':
            index = getDigits(pattern, index + 1, ret);
            hydrogenCount = (ret[0] == Integer.MIN_VALUE ? -1 : -ret[0]);
            break;
          case 'R':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setRingMembership(ret[0]);
            molecule.needRingData = true;
            break;
          case 'r':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setRingSize(ret[0]);
            molecule.needRingData = true;
            if (ret[0] > molecule.ringDataMax)
              molecule.ringDataMax = ret[0] + 1;
            break;
          case 'v':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setValence(ret[0]);
            break;
          case 'X':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setConnectivity(ret[0]);
            break;
          case 'x':
            index = getDigits(pattern, index + 1, ret);
            newAtom.setRingConnectivity(ret[0]);
            molecule.needRingData = true;
            break;
          case '-':
          case '+':
            index = checkCharge(pattern, index, newAtom);
            break;
          case '@':
            molecule.haveAtomStereochemistry = true;
            index = checkChirality(pattern, index,
                molecule.atoms[newAtom.index]);
            break;
          }
        } else {
          // Symbol
          int size = (Character.isUpperCase(ch)
              && Character.isLowerCase(nextChar) ? 2 : 1);
          if (size == 2) {
            // must check for Ar2, which must be A&r2
            nextChar = getChar(pattern, index + 2);
            if (Character.isDigit(nextChar))
              size = 1;
          }
          String symbol = (ch + pattern.substring(index + 1, index + size));
          if (!newAtom.setSymbol(symbol))
            throw new InvalidSmilesException("Invalid atom symbol");
          if (isPrimitive)
            atomSet.atomicNumber = newAtom.atomicNumber; 
          // indicates we have already assigned an atom number
          index += size;
        }
        isNot = false;
      }
      if (hydrogenCount == Integer.MIN_VALUE && isBracketed)
        hydrogenCount = Integer.MAX_VALUE;
      newAtom.setHydrogenCount(hydrogenCount);
      // for stereochemistry only:
      molecule.atoms[newAtom.index].setHydrogenCount(hydrogenCount);
    }

    // Final check

    if (currentAtom != null && bondType != SmilesBond.TYPE_NONE) {
      if (bondType == SmilesBond.TYPE_UNKNOWN)
        bondType = (isSmarts ? SmilesBond.TYPE_ANY 
            : SmilesBond.TYPE_SINGLE);
      molecule.createBond(currentAtom, newAtom, bondType);
    }
    if (Logger.debugging)
      Logger.debug("new atom: " + newAtom);
    return newAtom;
  }

  private static char getChar(String pattern, int i) {
    return (i < pattern.length() ? pattern.charAt(i) : '\0');
  }

  private static int getDigits(String pattern, int index, int[] ret) {
    int pt = index;
    int len = pattern.length();
    while (pt < len && Character.isDigit(pattern.charAt(pt)))
      pt++;
    ret[0] = Parser.parseInt(pattern.substring(index, pt));
    return pt;
  }



  /**
   * Parses a ring definition
   * 
   * @param molecule
   *          Resulting molecule
   * @param ringNum
   * @param currentAtom
   *          Current atom
   * @param bondType
   *          Bond type
   * @throws InvalidSmilesException
   */
  private void parseRing(SmilesSearch molecule, int ringNum,
                           SmilesAtom currentAtom, int bondType)
      throws InvalidSmilesException {

    // Ring management

    if (ringBonds == null)
      ringBonds = new SmilesBond[10];
    if (ringNum >= ringBonds.length) {
      SmilesBond[] tmp = new SmilesBond[ringBonds.length * 2];
      System.arraycopy(ringBonds, 0, tmp, 0, ringBonds.length);
      ringBonds = tmp;
    }

    SmilesBond b = ringBonds[ringNum];
    if (b == null) {
      ringBonds[ringNum] = molecule.createBond(currentAtom, null, bondType);
      return;
    }
    if (bondType == SmilesBond.TYPE_UNKNOWN) {
      if ((bondType = b.getBondType()) == SmilesBond.TYPE_UNKNOWN)
        bondType = (isSmarts ? SmilesBond.TYPE_ANY : SmilesBond.TYPE_SINGLE);
    } else if (b.getBondType() != SmilesBond.TYPE_UNKNOWN
        && b.getBondType() != bondType) {
      throw new InvalidSmilesException("Incoherent bond type for ring");
    }
    b.setBondType(bondType);
    b.setAtom2(currentAtom);
    currentAtom.addBond(b);
    ringBonds[ringNum] = null;
  }

  private int checkCharge(String pattern, int index, SmilesAtom newAtom) throws InvalidSmilesException {
    // Charge
    int len = pattern.length();
    char ch = pattern.charAt(index);
    int count = 1;
    ++index;
    if (index < len) {
      char nextChar = pattern.charAt(index);
      if (Character.isDigit(nextChar)) {
        int[] ret = new int[1];
        index = getDigits(pattern, index, ret);
        count = ret[0];
        if (count == Integer.MIN_VALUE)
          throw new InvalidSmilesException("Non numeric charge");
      } else {
        while (index < len
            && pattern.charAt(index) == ch) {
          index++;
          count++;
        }
      }
    }
    newAtom.setCharge(ch == '+' ? count : -count);
    return index;
  }

  private int checkChirality(String pattern, int index, SmilesAtom newAtom)
      throws InvalidSmilesException {
    int stereoClass = 0;
    int order = Integer.MIN_VALUE;
    int len = pattern.length();
    int ch;
    stereoClass = SmilesAtom.CHIRALITY_DEFAULT;
    order = 1;
    if (++index < len) {
      switch (ch = pattern.charAt(index)) {
      case '@':
        order = 2;
        index++;
        break;
      case 'H':
        break;
      case 'A':
      case 'O':
      case 'S':
      case 'T':
        stereoClass = (index + 1 < len ? SmilesAtom.getChiralityClass(pattern
            .substring(index, index + 2)) : -1);
        index += 2;
        break;
      default:
        order = (Character.isDigit(ch) ? 1 : -1);
      }
      int pt = index;
      if (order == 1) {
        while (pt < len && Character.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            order = Integer.parseInt(pattern.substring(index, pt));
          } catch (NumberFormatException e) {
            order = -1;
          }
          index = pt;
        }
      }
      if (order < 1 || stereoClass < 0)
        throw new InvalidSmilesException("Invalid stereochemistry descriptor");
    }
    newAtom.setChiralClass(stereoClass);
    newAtom.setChiralOrder(order);
    if (getChar(pattern, index) == '?') {
      Logger.error("Ignoring '?' in stereochemistry");
      index++;
    }
    return index;
  }

  private void fixChirality(SmilesSearch molecule) throws InvalidSmilesException {
    for (int i = molecule.patternAtomCount; --i >= 0; ) {
      SmilesAtom sAtom = molecule.getAtom(i);
      int stereoClass = sAtom.getChiralClass();
      int nBonds = sAtom.getHydrogenCount();
      if (nBonds < 0 || nBonds == Integer.MAX_VALUE)
        nBonds = 0;
      nBonds += sAtom.getBondsCount();
      switch (stereoClass) {
      case SmilesAtom.CHIRALITY_DEFAULT:
        switch (nBonds) {
        case 2:
        case 4:
        case 5:
        case 6:
          stereoClass = nBonds;
          break;
        }
        break;
      case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
        if (nBonds != 4)
          stereoClass = SmilesAtom.CHIRALITY_DEFAULT;
        break;        
      case SmilesAtom.CHIRALITY_ALLENE:
      case SmilesAtom.CHIRALITY_OCTAHEDRAL:
      case SmilesAtom.CHIRALITY_TETRAHEDRAL:
      case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
        if (nBonds != stereoClass)
          stereoClass = SmilesAtom.CHIRALITY_DEFAULT;
        break;        
      }
      if (stereoClass == SmilesAtom.CHIRALITY_DEFAULT)
        throw new InvalidSmilesException("Incorrect number of bonds for stereochemistry descriptor");
      sAtom.setChiralClass(stereoClass);
    }
  }


}
