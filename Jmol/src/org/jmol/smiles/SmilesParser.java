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

import java.util.Vector;

import org.jmol.util.Elements;
import org.jmol.util.TextFormat;

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
   * see package.html for details
   *
   * Bob Hanson, Jmol 12.0.RC10, 5/8/2010
   * 
   */

  /*
   * implicit hydrogen calculation
   * 
   * For a match to a SMILES string, as in "CCC".find("[Ch2]")
   * Jmol will return no match, because "CCC" refers to a structure
   * with a specific set of H atoms. Just because the H atoms are 
   * "implicit" in "CCC" is irrelevant.
   * 
   * For a match to a 3D model, [*h2] refers to all atoms such 
   * that the "calculate hydrogens" command would add two hydrogens,
   * and [*h] refers to cases where Jmol would add at least one hydrogen.
   * 
   * Jmol calculates the number of implicit hydrogens as follows:
   * 
   *  int targetValence = getTargetValence();
   *  int charge = getFormalCharge();
   *  if (charge != 0)
        targetValence += (targetValence == 4 ? -Math.abs(charge) : charge);
   * int n = targetValence - getValence();
   * nImplicitHydrogens = (n < 0 ? 0 : n);
   * 
   * Where getTargetValence() returns:
   *     switch (getElementNumber()) {
   *     case 6: //C
   *     case 14: //Si      
   *       return 4;
   *     case 5:  // B
   *     case 7:  // N
   *     case 15: // P
   *       return 3;
   *     case 8: //O
   *     case 16: //S
   *       return 2;
   *     default:
   *       return -1;
   *     }
   *     
   * Thus the implicit hydrogen count is:
   * 
   * a) 0 for all atoms other than {B,C,N,O,P,Si,S}
   * b) 0 for BR3
   * c) 0 for NR3, 1 for NR2, 2 for NR
   * d) 0 for RN=R, 1 for R=N, 0 for R=NR2(+), 0 for R2N(-)
   * e) 0 for CR4, 1 for CR3, 2 for CR2, 3 for CR
   * f) 0 for CR3(+), 0 for CR3(-)
   * 
   */
  private boolean isSearch;
  private SmilesBond[] ringBonds;
  private int braceCount;
  


  public static SmilesSearch getMolecule(String pattern, boolean isSearch) throws InvalidSmilesException {
    return (new SmilesParser(isSearch)).parse(pattern);
  }

  private SmilesParser(boolean isSearch) {
    this.isSearch = isSearch;    
  }
  
  /**
   * Parses a SMILES String
   * 
   * @param pattern
   *          SMILES String
   * @return Molecule corresponding to <code>pattern</code>
   * @throws InvalidSmilesException
   */
  SmilesSearch parse(String pattern) throws InvalidSmilesException {
    // if (Logger.debugging)
    // Logger.debug("Smiles Parser: " + pattern);
    if (pattern == null)
      throw new InvalidSmilesException("SMILES expressions must not be null");
    // First pass
    SmilesSearch molecule = new SmilesSearch();
    molecule.isSearch = isSearch;
    molecule.pattern = pattern;
    parseSmiles(molecule, pattern, null);
    
    // Check for braces
    
    if (braceCount != 0)
      throw new InvalidSmilesException("unmatched '{'");

    // Check for rings
    
    if (ringBonds != null)
      for (int i = 0; i < ringBonds.length; i++)
        if (ringBonds[i] != null)
          throw new InvalidSmilesException("Open ring");

    // finalize atoms
    
    molecule.setAtomArray();

    for (int i = molecule.atomCount; --i >= 0;) {
      molecule.getAtom(i).setBondArray();
      if (!isSearch && !molecule.getAtom(i).setHydrogenCount(molecule))
        throw new InvalidSmilesException(
            "unbracketed atoms must be one of: B C N O P S F Cl Br I");
    }

    if (!isSearch)
      molecule.elementCounts[1] = molecule.getMissingHydrogenCount();
    fixChirality(molecule);

    return molecule;
  }

   private void fixChirality(SmilesSearch molecule) throws InvalidSmilesException {
     for (int i = molecule.atomCount; --i >= 0; ) {
       SmilesAtom sAtom = molecule.getAtom(i);
       int stereoClass = sAtom.getChiralClass();
       int nBonds = sAtom.missingHydrogenCount;
       if (nBonds < 0)
         nBonds = 0;
       nBonds += sAtom.getCovalentBondCount();
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

    if (pattern == null)
      return;

    pattern = pattern.replaceAll("\\s","");

    if (pattern.indexOf("$") >= 0)
      pattern = parseVariables(pattern);
    if (pattern.indexOf("$(") >= 0)
      pattern = parseNested(molecule, pattern);

    if (pattern.length() == 0)
      return;

    int[] ret = new int[1];
    int index = 0;
    int pt = 0;
    int len = pattern.length();
    char ch = pattern.charAt(0);
    boolean haveOpen = checkBrace(molecule, ch, '{');
    if (haveOpen)
      ch = getChar(pattern, ++index);

    // Branch -- note that bonds come AFTER '(', not before. 

    if (ch == '(') {
      if (currentAtom == null)
        throw new InvalidSmilesException("No previous atom for branch");
      pt = index++;
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
      ch = getChar(pattern, pt);
      if (ch == '}' && checkBrace(molecule, ch, '}'))
          pt++;
      if (pt < len) {
        parseSmiles(molecule, pattern.substring(pt), currentAtom);
        return;
      }
      if (!isSearch)
        throw new InvalidSmilesException("Pattern must not end with ')'");
      return;
    }
    
    pt = index;
    while (SmilesBond.isBondType(ch, isSearch))
      ch = getChar(pattern, ++index);

    SmilesBond bond = parseBond(molecule, null, pattern.substring(pt, index), null, currentAtom, false);

    if (haveOpen && bond.bondType != SmilesBond.TYPE_UNKNOWN)
      index = pt;
    ch = getChar(pattern, index);
    if (checkBrace(molecule, ch, '{'))
      ch = getChar(pattern, ++index);

    boolean isRing = (Character.isDigit(ch) || ch == '%');
    boolean isAtom = (!isRing && (ch == '_' || ch == '[' || ch == '*' || Character
        .isLetter(ch)));
    if (isRing) {
      int ringNumber;
      switch (ch) {
      case '%':
        // [ringPoint]
        if (index + 3 <= pattern.length())
          index = getDigits(pattern.substring(0, index + 3), index + 1, ret);
        if ((ringNumber = ret[0]) < 10)
          throw new InvalidSmilesException(
              "Two digits must follow the % sign");
        break;
      default:
        ringNumber = ch - '0';
        index++;
      }
      parseRing(molecule, ringNumber, currentAtom, bond);
    } else if (isAtom) {
      switch (ch) {
      case '[':
      case '_':
        String subPattern = getSubPattern(pattern, index, ch);
        currentAtom = parseAtom(molecule, null, subPattern, currentAtom,
           bond, true, false);
        if (bond.bondType != SmilesBond.TYPE_UNKNOWN && bond.bondType != SmilesBond.TYPE_NONE)
          bond.set(null, currentAtom);
        index += subPattern.length() + (ch == '[' ? 2 : 0);
        break;
      default:
        // [atomType]
        char ch2 = (Character.isUpperCase(ch) ? getChar(pattern,
            index + 1) : '\0');
        if (ch != 'X' || ch2 != 'x')
          if (!Character.isLowerCase(ch2)
              || Elements.elementNumberFromSymbol(pattern.substring(index,
                  index + 2), true) == 0)
            ch2 = '\0';
        // guess at some ambiguous SEARCH strings:
        if (ch2 != '\0'
            && "NA CA BA PA SC AC".indexOf(pattern.substring(index, index + 2)) >= 0) {
          //System.out.println("Note: " + ch + ch2 + " NOT interpreted as an element");
          ch2 = '\0';
        }
        int size = (Character.isUpperCase(ch) && Character.isLowerCase(ch2) ? 2
            : 1);
        currentAtom = parseAtom(molecule, null, pattern.substring(index, index
            + size), currentAtom, bond, false, false);
        index += size;
      }
    } else {
      throw new InvalidSmilesException("Unexpected character: "
          + pattern.charAt(index));
    }

    ch = getChar(pattern, index);
    if (ch == '}' && checkBrace(molecule, ch, '}'))
      index++;

    // [connections]

    parseSmiles(molecule, pattern.substring(index), currentAtom);
  }

  private boolean checkBrace(SmilesSearch molecule, char ch, char type)
      throws InvalidSmilesException {
    switch (ch) {
    case '{':
      if (ch != type)
        break;
      // 3D SEARCH {C}CCC{C}C subset selection
      braceCount++;
      molecule.haveSelected = true;
      return true;
    case '}':
      if (ch != type)
        break;
      if (braceCount > 0) {
        braceCount--;
        return true;
      }
    default:
      return false;
    }
    throw new InvalidSmilesException("Unmatched '}'");
  }

  private String parseNested(SmilesSearch molecule, String pattern) throws InvalidSmilesException {
    int index;
    while ((index = pattern.lastIndexOf("$(")) >= 0) {
      String s = getSubPattern(pattern, index + 1, '(');
      int i = molecule.addNested(s);
      int pt = index + s.length() + 3;
      s = "_" + i + "_";
      pattern = pattern.substring(0, index) + s + pattern.substring(pt);
    }
    return pattern;
  }

  private String parseVariables(String pattern) throws InvalidSmilesException {
    Vector keys = new Vector();
    Vector values = new Vector();
    int index;
    int ipt = 0;
    int iptLast = -1;
    while ((index = pattern.indexOf("$", ipt)) >= 0) {
      if (getChar(pattern, ipt + 1) == '(')
        break;
      ipt = skipTo(pattern, index, '=');
      if (ipt <= index + 1 || getChar(pattern, ipt + 1) != '\"')
        break;
      String key = pattern.substring(index, ipt);
      if (key.lastIndexOf('$') > 0 || key.indexOf(']') > 0)
        throw new InvalidSmilesException("Invalid variable name: " + key);
      String s = getSubPattern(pattern, ipt + 1, '\"');
      keys.add("[" + key + "]");
      values.add(s);
      ipt += s.length() + 2;      
      ipt = skipTo(pattern, ipt, ';');
      iptLast = ++ipt;
    }
    if (iptLast < 0)
      return pattern;
    return TextFormat.replaceStrings(pattern.substring(iptLast), keys, values);
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
   * @param bond 
   * @param isBracketed
   *          Indicates if is a isBracketed definition (between [])
   * @param isPrimitive
   * @return New atom
   * @throws InvalidSmilesException
   */
  private SmilesAtom parseAtom(SmilesSearch molecule, SmilesAtom atomSet,
                               String pattern, SmilesAtom currentAtom,
                               SmilesBond bond,
                               boolean isBracketed, boolean isPrimitive)
      throws InvalidSmilesException {
    if (pattern == null || pattern.length() == 0)
      throw new InvalidSmilesException("Empty atom definition");

    SmilesAtom newAtom = (atomSet == null ? molecule.addAtom()
        : isPrimitive ? atomSet.addPrimitive() : atomSet.addAtomOr());

    if (braceCount > 0)
      newAtom.selected = true;
    
    if (!checkLogic(molecule, pattern, newAtom, null, currentAtom, isPrimitive)) {

      int[] ret = new int[1];

      char ch = pattern.charAt(0);

      int index = 0;
      boolean isNot = false;
      if (isSearch && ch == '!') {
        ch = getChar(pattern, ++index);
        if (ch == '\0')
          throw new InvalidSmilesException("invalid '!'");
        newAtom.not = isNot = true;
      }

      int hydrogenCount = Integer.MIN_VALUE;

      while (ch != '\0') {
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
        } else {
          switch (ch) {
          case '_': // $(...) nesting
            index = getDigits(pattern, index + 1, ret) + 1;
            if (ret[0] == Integer.MIN_VALUE)
              throw new InvalidSmilesException("Invalid SEARCH primitive: "
                  + pattern.substring(index));
            newAtom.iNested = ret[0];
            break;
          case '-':
          case '+':
            index = checkCharge(pattern, index, newAtom);
            break;
          case '@':
            molecule.haveAtomStereochemistry = true;
            index = checkChirality(pattern, index,
                molecule.patternAtoms[newAtom.index]);
            break;
          default:
            // SMARTS has ambiguities in terms of chaining without &.
            // H alone is "one H atom"
            // "!H" is "not hydrogen" but "!H2" is "not two attached hydrogens"
            // [Rh] could be rhodium or "Any ring atom with at least one implicit H atom"
            // [Ar] could be argon or "Any aliphatic ring atom"
            // There is no ambiguity, though, with [Rh3] or [Ar6] because
            // in those cases the number forces the single-character property
            // "h3" or "r6", thus forcing "R" or "A"
            // also, Jmol has Xx
            // We manage this by simply asserting that if & is not used and
            // the first two-letters of the expression could be interpreted
            // as a symbol, AND the next character is not a digit, then it WILL be interpreted as a symbol
            char nextChar = getChar(pattern, index + 1);
            String sym2 = pattern.substring(index + 1, index
                + (Character.isLowerCase(nextChar) 
                    && (!isBracketed || !Character.isDigit(getChar(pattern, index + 2))) 
                    ? 2 : 1));
            String symbol = Character.toUpperCase(ch) + sym2;
            boolean mustBeSymbol = true;
            boolean checkForPrimitive = (isBracketed && (ch == '=' || Character.isLetter(ch)));
            if (checkForPrimitive) {
              if (!isNot && (isPrimitive ? atomSet : newAtom).hasSymbol) {
                // if we already have a symbol, and we aren't negating,
                // then this MUST be a property
                // because you can't have [C&O], but you can have [R&!O&!C]
                // We also allow [C&!O], though that's not particularly useful.
                mustBeSymbol = false;
              } else if (ch == 'H') {
                // only H if not H<n> or H1? 
                // 
                mustBeSymbol = !Character.isDigit(nextChar) || getChar(pattern, index + 2) == '?';
              } else if ("=DdhRrvXx".indexOf(ch) >= 0
                  && Character.isDigit(nextChar)) {
                // not a symbol if any of these are followed by a number 
                mustBeSymbol = false;
              } else if (!symbol.equals("A") && !symbol.equals("Xx")) {
                // check for two-character symbol, then one-character symbol
                mustBeSymbol = (Elements.elementNumberFromSymbol(symbol, true) > 0);
                if (!mustBeSymbol && sym2 != "") {
                  sym2 = "";
                  symbol = symbol.substring(0, 1);
                  mustBeSymbol = (Elements
                      .elementNumberFromSymbol(symbol, true) > 0);
                }
              }
            }
            if (mustBeSymbol) {
              if (!isBracketed && !isSearch
                  && !SmilesAtom.allowSmilesUnbracketed(symbol)
                  || !newAtom.setSymbol(symbol = ch + sym2))
                throw new InvalidSmilesException("Invalid atom symbol: "
                    + symbol);
              if (isPrimitive)
                atomSet.hasSymbol = true;
              // indicates we have already assigned an atom number
              index += symbol.length();
            } else {
              index = getDigits(pattern, index + 1, ret);
              int val = ret[0];
              switch (ch) {
              default:
                throw new InvalidSmilesException("Invalid SEARCH primitive: "
                    + pattern.substring(index));
              case '=':
                newAtom.jmolIndex = val;
                break;
              case 'D':
                // default is 1
                newAtom.setDegree(val == Integer.MIN_VALUE ? 1 : val);
                break;
              case 'd':
                // default is 1
                newAtom
                    .setNonhydrogenDegree(val == Integer.MIN_VALUE ? 1 : val);
                break;
              case 'H':
                // default is 1
                hydrogenCount = (val == Integer.MIN_VALUE ? 1 : val);
                break;
              case 'h':
                // default > 1
                newAtom.setImplicitHydrogenCount(val == Integer.MIN_VALUE ? -1
                    : val);
                break;
              case 'R':
                if (val == Integer.MIN_VALUE)
                  val = -1; // R --> !R0; !R --> R0
                newAtom.setRingMembership(val);
                molecule.needRingData = true;
                break;
              case 'r':
                if (val == Integer.MIN_VALUE) {
                  val = -1; // r --> !R0; !r --> R0
                  newAtom.setRingMembership(val);
                } else {
                  newAtom.setRingSize(val);
                  if (val > molecule.ringDataMax)
                    molecule.ringDataMax = val;
                }
                molecule.needRingData = true;
                break;
              case 'v':
                // default 1
                newAtom.setValence(val == Integer.MIN_VALUE ? 1 : val);
                break;
              case 'X':
                // default 1
                newAtom.setConnectivity(val == Integer.MIN_VALUE ? 1 : val);
                break;
              case 'x':
                // default > 0
                newAtom
                    .setRingConnectivity(val == Integer.MIN_VALUE ? -1 : val);
                molecule.needRingData = true;
                break;
              }
            }
          }
        }
        ch = getChar(pattern, index);
        if (isNot && ch != '\0')
          throw new InvalidSmilesException("'!' may only involve one primitive.");
      }
      if (hydrogenCount == Integer.MIN_VALUE && isBracketed)
        hydrogenCount = Integer.MIN_VALUE + 1;
      newAtom.setExplicitHydrogenCount(hydrogenCount);
      // for stereochemistry only:
      molecule.patternAtoms[newAtom.index].setExplicitHydrogenCount(hydrogenCount);
    }

    // Final check

    if (currentAtom != null && bond.bondType != SmilesBond.TYPE_NONE) {
      if (bond.bondType == SmilesBond.TYPE_UNKNOWN)
        bond.bondType = (isSearch || currentAtom.isAromatic()
            && newAtom.isAromatic() ? SmilesBond.TYPE_ANY
            : SmilesBond.TYPE_SINGLE);
      if (!isBracketed)
        bond.set(null, newAtom);
    }
    // if (Logger.debugging)
    // Logger.debug("new atom: " + newAtom);
    return newAtom;
  }


  /**
   * Parses a ring definition
   * 
   * @param molecule
   *          Resulting molecule
   * @param ringNum
   * @param currentAtom
   *          Current atom
   * @param bond 
   * @throws InvalidSmilesException
   */
  private void parseRing(SmilesSearch molecule, int ringNum,
                         SmilesAtom currentAtom, SmilesBond bond)
      throws InvalidSmilesException {

    // Ring management

    if (ringBonds == null)
      ringBonds = new SmilesBond[10];
    if (ringNum >= ringBonds.length) {
      SmilesBond[] tmp = new SmilesBond[ringBonds.length * 2];
      System.arraycopy(ringBonds, 0, tmp, 0, ringBonds.length);
      ringBonds = tmp;
    }

    SmilesBond bond0 = ringBonds[ringNum];
    if (bond0 == null) {
      ringBonds[ringNum] = bond;
      return;
    }
    ringBonds[ringNum] = null;
    if (bond.bondType == SmilesBond.TYPE_UNKNOWN) {
      if (bond0.bondType == SmilesBond.TYPE_UNKNOWN)
        bond.bondType = (isSearch || currentAtom.isAromatic()
            && bond0.getAtom1().isAromatic() ? SmilesBond.TYPE_ANY
            : SmilesBond.TYPE_SINGLE);
      else
        bond.bondType = bond0.bondType;
    } else if (bond0.bondType != SmilesBond.TYPE_UNKNOWN
        && bond0.bondType != bond.bondType) {
      throw new InvalidSmilesException("Incoherent bond type for ring");
    }
    bond0.set(bond);
    currentAtom.bondCount--;
    bond0.setAtom2(currentAtom);
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
      System.out.println("Ignoring '?' in stereochemistry");
      index++;
    }
    return index;
  }

  private SmilesBond parseBond(SmilesSearch molecule, SmilesBond bondSet,
                               String pattern, SmilesBond bond,
                               SmilesAtom currentAtom, boolean isPrimitive)
      throws InvalidSmilesException {

    // pattern length will be 1 or 2 only, x or !x

    char ch = getChar(pattern, 0);
    if (ch == '.') {
      if (bond != null || bondSet != null)
        throw new InvalidSmilesException("invalid '.'");
      return new SmilesBond(SmilesBond.TYPE_NONE, false);      
    }
    SmilesBond newBond = (bondSet == null ? (bond == null ? new SmilesBond(
        currentAtom, null, SmilesBond.TYPE_UNKNOWN, false) : bond)
        : isPrimitive ? bondSet.addPrimitive() : bondSet.addBondOr());

    if (ch != '\0'
        && !checkLogic(molecule, pattern, null, newBond, currentAtom, isPrimitive)) {
      boolean isBondNot = (ch == '!');
      if (isBondNot) {
        ch = getChar(pattern, 1);
        if (ch == '\0' || ch == '!')
          throw new InvalidSmilesException("invalid '!'");
      }
      int bondType = SmilesBond.getBondTypeFromCode(ch);
      if (bondType == SmilesBond.TYPE_RING)
        molecule.needRingMemberships = true;
      if (currentAtom == null && bondType != SmilesBond.TYPE_NONE)
        throw new InvalidSmilesException("Bond without a previous atom");
      switch (bondType) {
      case SmilesBond.TYPE_DIRECTIONAL_1:
      case SmilesBond.TYPE_DIRECTIONAL_2:
        molecule.haveBondStereochemistry = true;
        break;
      }
      newBond.set(bondType, isBondNot);
    }
    return newBond;
  }

  private boolean checkLogic(SmilesSearch molecule, String pattern,
                             SmilesAtom atom, SmilesBond bond,
                             SmilesAtom currentAtom, boolean isPrimitive)
      throws InvalidSmilesException {
    int pt = pattern.indexOf(',');
    int len = pattern.length();
    while (true) {
      boolean haveOr = (pt > 0);
      if (haveOr && !isSearch || pt == 0)
        break;
      String props = "";
      pt = pattern.indexOf(';');
      if (pt >= 0) {
        if (!isSearch || pt == 0)
          break;
        props = "&" + pattern.substring(pt + 1);
        pattern = pattern.substring(0, pt);
        if (!haveOr) {
          pattern += props;
          props = "";
        }
      }
      int index = 0;
      if (haveOr) {
        pattern += ",";
        while ((pt = pattern.indexOf(',', index)) > 0 && pt <= len) {
          String s = pattern.substring(index, pt) + props;
          if (s.length() == 0)
            throw new InvalidSmilesException("missing " + (bond == null ? "atom" : "bond") + " token");
          if (bond == null)
            parseAtom(molecule, atom, s, null, null, true, false);
          else
            parseBond(molecule, bond, s, null, currentAtom, false);
          index = pt + 1;
        }
      } else if ((pt = pattern.indexOf('&')) >= 0 || bond != null && len > 1
          && !isPrimitive) {
        // process primitive
        if (!isSearch || pt == 0)
          break;
        if (bond != null && pt < 0) {
          // bonds are simpler, because they have only one character
          if (len > 1) {
            StringBuffer sNew = new StringBuffer();
            for (int i = 0; i < len;) {
              char ch = pattern.charAt(i++);
              sNew.append(ch);
              if (ch != '!' && i < len)
                sNew.append('&');
            }
            pattern = sNew.toString();
            len = pattern.length();
          }
        }
        pattern += "&";
        while ((pt = pattern.indexOf('&', index)) > 0 && pt <= len) {
          String s = pattern.substring(index, pt) + props;
          if (bond == null)
            parseAtom(molecule, atom, s, null, null, true, true);
          else
            parseBond(molecule, bond, s, null, currentAtom, true);
          index = pt + 1;
        }
      } else {
        return false;
      }
      return true;
    }
    char ch = pattern.charAt(pt);
    throw new InvalidSmilesException(isSearch ? "invalid placement for '" + ch
        + "'" : "[" + ch + "] notation only valid with SEARCH, not SMILES");
  }

  private static String getSubPattern(String pattern, int index, char ch) throws InvalidSmilesException {
    char ch2;
    int margin = 1;
    switch (ch) {
    case '[':
      ch2 = ']';
      break;
    case '"':
      ch2 = '"';
      break;
    case '(':
      ch2 = ')';
      break;
    default:
      ch2 = ch;
      margin = 0;
    }
    int len = pattern.length();
    int pCount = 1;
    for (int pt = index + 1; pt < len; pt++) {
      char ch1 = pattern.charAt(pt);
      if (ch1 == ch2) {
        pCount--;
        if (pCount == 0)
          return pattern.substring(index + margin, pt + 1 - margin);
      } else if (ch1 == ch) {
        pCount++;
      }
    }
    throw new InvalidSmilesException("Unmatched " + ch);
  }

  private static char getChar(String pattern, int i) {
    return (i < pattern.length() ? pattern.charAt(i) : '\0');
  }

  private static int getDigits(String pattern, int index, int[] ret) {
    int pt = index;
    int len = pattern.length();
    while (pt < len && Character.isDigit(pattern.charAt(pt)))
      pt++;
    try {
      ret[0] = Integer.parseInt(pattern.substring(index, pt));
    } catch (NumberFormatException e) {
      ret[0] = Integer.MIN_VALUE;
    }
    return pt;
  }

  private static int skipTo(String pattern, int index, char ch0) {
    int pt = index;
    char ch;
    while ((ch = getChar(pattern, ++pt)) != ch0 && ch != '\0'){}
    return (ch == '\0' ? -1 : pt);
  }


}
