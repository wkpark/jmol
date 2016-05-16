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


import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;

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
   * For a match to a SMILES string, as in "CCC".find("SMARTS", "[Ch2]")
   * Jmol will return a match (bug fixed 5/2016).
   * 
   * For a match to a 3D model, [h2] refers to all atoms such 
   * that the "calculate hydrogens" command would add two hydrogens,
   * and [*h] refers to cases where Jmol would add at least one hydrogen.
   * 
   * The calculation is not perfect, and is best for proteins.
   * It is jmol.org.modelset.Atom.getImplicitHydrogenCount(), which calls
   * org.jmol.modelset.ModelSet.getMissingHydrogenCount(this, false)
   * 
   * 
   */
  
  
  private Map<Integer, SmilesBond> connections = new Hashtable<Integer, SmilesBond>();
  private Map<String, SmilesMeasure> htMeasures = new Hashtable<String, SmilesMeasure>();

  private int flags;

  private boolean isSmarts;
  private boolean isBioSequence;
  private char bioType = '\0';
   
  private int braceCount;
  private int branchLevel;
  private int componentCount;
  private int componentParenCount;

  private boolean ignoreStereochemistry;
  private boolean bondDirectionPaired = true;
  private boolean isTarget;
  
  public static SmilesSearch getMolecule(String pattern, boolean isSmarts, boolean isTarget)
      throws InvalidSmilesException {
    return (new SmilesParser(isSmarts, isTarget)).parse(pattern);
  }

  SmilesParser(boolean isSmarts, boolean isTarget) {
    this.isSmarts = isSmarts;
    this.isTarget = isTarget;
  }

//  void reset() {
//    braceCount = 0;
//    branchLevel = 0;
//  }

  
  /**
   * Parses a SMILES String
   * 
   * @param pattern
   *        SMILES String
   * @return Molecule corresponding <ctode>pattern</code>
   * @throws InvalidSmilesException
   */
  SmilesSearch parse(String pattern) throws InvalidSmilesException {
    // if (Logger.debugging)
    // Logger.debug("Smiles Parser: " + pattern);
    if (pattern == null)
      throw new InvalidSmilesException("expression must not be null");
    SmilesSearch search = new SmilesSearch();
    if (pattern.indexOf("$(select") >= 0) // must do this before cleaning
      pattern = parseNested(search, pattern, "select");
    int[] ret = new int[1];
    pattern = extractFlags(pattern, ret);
    flags = ret[0];
    ignoreStereochemistry = ((flags & JC.SMILES_IGNORE_STEREOCHEMISTRY) == JC.SMILES_IGNORE_STEREOCHEMISTRY);
    search.setFlags(flags);
    if (pattern.indexOf("$") >= 0)
      pattern = parseVariables(pattern);
    if (isSmarts && pattern.indexOf("[$") >= 0)
      pattern = parseVariableLength(pattern);
    if (pattern.indexOf("||") >= 0) {
      String[] patterns = PT.split(pattern, "||");
      String toDo = "";
      search.subSearches = new SmilesSearch[patterns.length];
      for (int i = 0; i < patterns.length; i++) {
        String key = "|" + patterns[i] + "|";
        if (toDo.indexOf(key) < 0) {
          search.subSearches[i] = getSearch(search, patterns[i], flags);
          toDo += key;
        }
      }
      //if (Logger.debugging)
      //Logger.info(toDo);
    } else {
      search = getSearch(search, pattern, flags);
    }
    return search;
  }

  private String parseVariableLength(String pattern)
      throws InvalidSmilesException {
    SB sout = new SB();
    // fix internal ||
    int len = pattern.length() - 1;
    int nParen = 0;
    boolean haveInternalOr = false;
    for (int i = 0; i < len; i++) {
      switch (pattern.charAt(i)) {
      case '(':
        nParen++;
        break;
      case ')':
        nParen--;
        break;
      case '|':
        if (nParen > 0) {
          haveInternalOr = true;
          if (pattern.charAt(i + 1) == '|') {
            pattern = pattern.substring(0, i) + pattern.substring(i + 1);
            len--;
          }
        }
        break;
      }
    }
    if (pattern.indexOf("||") >= 0) {
      String[] patterns = PT.split(pattern, "||");
      for (int i = 0; i < patterns.length; i++)
        sout.append("||").append(parseVariableLength(patterns[i]));
    } else {
      int pt = -1;
      int[] ret = new int[1];
      // [$n(...)] or [$m-n(...)]
      boolean isOK = true;
      String bracketed = null;
      while ((pt = pattern.indexOf("[$", pt + 1)) >= 0) {
        int pt0 = pt;
        int min = Integer.MIN_VALUE;
        int max = Integer.MIN_VALUE;
        pt = getDigits(pattern, pt + 2, ret);
        min = ret[0];
        if (min != Integer.MIN_VALUE) {
          if (getChar(pattern, pt) == '-') {
            pt = getDigits(pattern, pt + 1, ret);
            max = ret[0];
          }
        } 
        if (getChar(pattern, pt) != '(')
          continue;
        bracketed = getSubPattern(pattern, pt0, '[');
        if (!bracketed.endsWith(")"))
          continue;
        int pt1 = pt0 + bracketed.length() + 2;
        String repeat = getSubPattern(pattern, pt, '(');
        int pt2 = pt;
        bracketed = getSubPattern(pattern, pt, '[');
        pt += 1 + repeat.length();
        if (repeat.indexOf(':') >= 0 && repeat.indexOf('|') < 0) {
          // must enclose first ":" in ()
          // what is this??
          int parenCount = 0;
          int n = repeat.length();
          int ptColon = -1;
          for (int i = 0; i < n; i++) {
            switch (repeat.charAt(i)) {
            case '[':
            case '(':
              parenCount++;
              break;
            case ')':
            case ']':
              parenCount--;
              break;
            case '.':
              if (ptColon >= 0 && parenCount == 0)
                n = i;
              break;
            case ':':
              if (ptColon < 0 && parenCount == 0)
                ptColon = i;
              break;
            }
          }
          if (ptColon > 0)
            repeat = repeat.substring(0, ptColon) + "("
                + repeat.substring(ptColon, n) + ")" + repeat.substring(n);
        }
        if (min == Integer.MIN_VALUE) {
          int ptOr = repeat.indexOf("|");
          if (ptOr >= 0)
            return parseVariableLength(pattern.substring(0, pt0) + "[$1" + pattern.substring(pt2, pt2 + ptOr + 1) + ")]"
                + pattern.substring(pt1) + "||"
                + pattern.substring(0, pt0) + "[$1(" + pattern.substring(pt2 + ptOr + 2)
                + pattern.substring(pt1));
          continue;
        }
        if (max == Integer.MIN_VALUE)
          max = min;
        if (repeat.indexOf("|") >= 0)
          repeat = "[$(" + repeat + ")]";
        for (int i = min; i <= max; i++) {
          SB sb = new SB();
          sb.append("||").append(pattern.substring(0, pt0));
          for (int j = 0; j < i; j++)
            sb.append(repeat);
          sb.append(pattern.substring(pt1));
          sout.appendSB(sb);
        }
      }
      if (!isOK)
        throw new InvalidSmilesException("bad variable expression: "
            + bracketed);
    }
    return (haveInternalOr ? parseVariableLength(sout.substring(2)) : sout.length() < 2 ? pattern : sout.substring(2));
  }

  SmilesSearch getSearch(SmilesSearch parent, String pattern, int flags)
      throws InvalidSmilesException {
    // First pass
    htMeasures = new Hashtable<String, SmilesMeasure>();
    SmilesSearch molecule = new SmilesSearch();
    molecule.setTop(parent);
    molecule.isSmarts = isSmarts;
    molecule.pattern = pattern;
    molecule.setFlags(flags);
    if (pattern.indexOf("$(") >= 0)
      pattern = parseNested(molecule, pattern, "");
    parseSmiles(molecule, pattern, null, false);

    // Check for braces

    if (braceCount != 0)
      throw new InvalidSmilesException("unmatched '{'");
    
    if (!bondDirectionPaired)
      throw new InvalidSmilesException("unmatched '/'");

    if (!connections.isEmpty())
      throw new InvalidSmilesException("Open ring");

    // finalize atoms

    molecule.set();

    // set the searches now that we know what's a bioAtom and what's not

    if (isSmarts)
      for (int i = molecule.ac; --i >= 0;) {
        SmilesAtom atom = molecule.patternAtoms[i];
        checkNested(molecule, atom, flags);
        for (int k = 0; k < atom.nAtomsOr; k++)
          checkNested(molecule, atom.atomsOr[k], flags);
        for (int k = 0; k < atom.nPrimitives; k++)
          checkNested(molecule, atom.primitives[k], flags);
      }
    if (!isSmarts && !isBioSequence)
      molecule.elementCounts[1] = molecule.getMissingHydrogenCount();

 // problem here is that we need to first create a topomap of the structure
 // sometimes, and that has to be done BEFORE fixChirality, perhaps.
 // this is only a problem when there is an H attached directly to a 
 // see-saw, trigonal bipyramidal, or octahedral center.
    if (!ignoreStereochemistry && !isTarget)
     fixChirality(molecule);

    return molecule;
  }
  
  private void checkNested(SmilesSearch molecule, SmilesAtom atom, int flags) 
  throws InvalidSmilesException {
    if (atom.iNested > 0) {
      Object o = molecule.getNested(atom.iNested);
      if (o instanceof String) {
        String s = (String) o;
        if (s.startsWith("select"))
          return;
        if (s.charAt(0) != '~' && atom.bioType != '\0')
          s = "~" + atom.bioType + "~" + s;
        SmilesSearch search = getSearch(molecule, s, flags);
        if (search.ac > 0 && search.patternAtoms[0].selected)
          atom.selected = true;
        molecule.setNested(atom.iNested, search);
      }
    }
  }

  private void fixChirality(SmilesSearch molecule)
      throws InvalidSmilesException {
    for (int i = molecule.ac; --i >= 0;) {
      SmilesAtom sAtom = molecule.patternAtoms[i];
      if (sAtom.stereo != null)
        sAtom.stereo.fixStereo(sAtom);
    }
  }

  /**
   * Parses a part of a SMILES String
   * 
   * @param molecule
   *        Resulting molecule
   * @param pattern
   *        SMILES String
   * @param currentAtom
   *        Current atom
   * @param isBranchAtom
   *        If we are starting a new branch
   * @throws InvalidSmilesException
   */
  private void parseSmiles(SmilesSearch molecule, String pattern,
                           SmilesAtom currentAtom, boolean isBranchAtom)
      throws InvalidSmilesException {
    int[] ret = new int[1];
    int pt = 0;
    char ch;
    SmilesBond bond = null;
    boolean wasMeasure = false;
    boolean wasBranch = false;
    loop: while (pattern != null && pattern.length() != 0) {
      int index = 0;
      if (currentAtom == null) {
        index = checkBioType(pattern, 0);
        if (index == pattern.length())
          pattern += "*";
        if (isBioSequence)
          molecule.needAromatic = molecule.top.needAromatic = false;
      }
      ch = getChar(pattern, index);
      boolean haveOpen = checkBrace(molecule, ch, '{');
      if (haveOpen)
        ch = getChar(pattern, ++index);
      if (ch == '(') {

        // measure, biosequence, branch, or component

        String subString = getSubPattern(pattern, index, '(');
        boolean isMeasure = (getChar(pattern, index + 1) == '.');
        if (currentAtom == null) {
          if (isMeasure || !isSmarts)
            throw new InvalidSmilesException("No previous atom for measure");

          // component (....)          
          
          molecule.haveComponents = true;
          do {
            componentCount++;
            componentParenCount++;
            ch = getChar(pattern = pattern.substring(1), 0);
          } while (ch == '(');          
          if (!haveOpen && (haveOpen = checkBrace(molecule, ch, '{')) == true)
            ch = getChar(pattern = pattern.substring(1), 0);
        } else {
          // measure, biosequence, or branch
          wasMeasure = wasBranch = false;
          if (subString.startsWith(".")) {
            parseMeasure(molecule, subString.substring(1), currentAtom);
            wasMeasure = true;
          } else if (subString.length() == 0 && isBioSequence) {
            // () can mean NOT crosslinked
            currentAtom.notCrossLinked = true;
          } else {
            branchLevel++;
            parseSmiles(molecule, subString, currentAtom, true);
            wasBranch = true;
            branchLevel--;
          }
          index = subString.length() + 2;
          ch = getChar(pattern, index);
          if (ch == '}' && checkBrace(molecule, ch, '}'))
            index++;
          ch = '\0';
          // skip next section
        }
      }

      if (ch != '\0') {
        // bond-atom-bond-atom...
        pt = index;
        out: while (ch != '\0') {
          switch (SmilesBond.isBondType(ch, isSmarts, isBioSequence)) {
          case 1:
            break;
          case 0:
            break out;
          case -1:
            // ^... or ^^...
            // look for nn- or just -
            if (!((PT.isDigit(getChar(pattern, ++index)) && index++ > 0 ? PT
                .isDigit(getChar(pattern, index++)) : true) && (ch = getChar(
                pattern, index)) == '-'))
              throw new InvalidSmilesException(
                  "malformed atropisomerism bond ^nn-  or ^^nn-");
            continue;
          }
          ch = getChar(pattern, ++index);
        }
        ch = getChar(pattern, index);
        if (ch == ')') {
          switch (ch = getChar(pattern, ++index)) {
          case '\0':
          case ')':
          case '.':
            pattern = pattern.substring(index);
            componentParenCount--;
            if (componentParenCount >= 0)
              continue loop;
          }
          throw new InvalidSmilesException(
              "invalid continuation after component grouping (SMARTS).(SMARTS)");
        }
        bond = parseBond(molecule, null, pattern.substring(pt, index), null,
            currentAtom, false, isBranchAtom, index - pt, ret);

        if (haveOpen && bond.order != SmilesBond.TYPE_UNKNOWN)
          ch = getChar(pattern, index = pt);
        if (checkBrace(molecule, ch, '{'))
          ch = getChar(pattern, ++index);
        switch (ch) {
        case '~':
          if (bond.order == SmilesBond.TYPE_NONE) {
            index = checkBioType(pattern, index);
            if (index == pattern.length())
              pattern += "*";
          }
          break;
        case '(':
          do {
            componentCount++;
            componentParenCount++;
            ch = getChar(pattern, ++index);
          } while (ch == '(');
          break;
        case '\0':
          if (bond.order == SmilesBond.TYPE_NONE)
            return;
        }
        boolean isConnect = (PT.isDigit(ch) || ch == '%');
        boolean isAtom = (!isConnect && (ch == '_' || ch == '[' || ch == '*' || PT
            .isLetter(ch)));
        if (isConnect) {
          if (wasMeasure || wasBranch)
            throw new InvalidSmilesException(
                "connection number must immediately follow its connecting atom");
          index = getRingNumber(pattern, index, ch, ret);
          int ringNumber = ret[0];
          parseConnection(molecule, ringNumber, currentAtom, bond);
          bond = null;
        } else if (isAtom) {
          wasMeasure = wasBranch = false;
          switch (ch) {
          case '[':
          case '_':
            String subPattern = getSubPattern(pattern, index, ch);
            index += subPattern.length() + (ch == '[' ? 2 : 0);
            if (isBioSequence && ch == '[' && subPattern.indexOf(".") < 0
                && subPattern.indexOf("_") < 0)
              subPattern += ".0";
            currentAtom = parseAtom(molecule, null, subPattern, currentAtom,
                bond, ch == '[', false, isBranchAtom);
            currentAtom.hasSubpattern = true;
            if (bond.order != SmilesBond.TYPE_UNKNOWN
                && bond.order != SmilesBond.TYPE_NONE)
              setBondAtom(bond, null, currentAtom, molecule);
            bond = null;
            break;
          default:
            // [atomType]
            char ch2 = (!isBioSequence && PT.isUpperCase(ch) ? getChar(pattern,
                index + 1) : '\0');
            if (ch != 'X' || ch2 != 'x')
              if (!PT.isLowerCase(ch2)
                  || Elements.elementNumberFromSymbol(
                      pattern.substring(index, index + 2), true) == 0)
                ch2 = '\0';
            // guess at some ambiguous SEARCH strings:
            if (ch2 != '\0'
                && "NA CA BA PA SC AC".indexOf(pattern.substring(index,
                    index + 2)) >= 0) {
              //System.out.println("Note: " + ch + ch2 + " NOT interpreted as an element");
              ch2 = '\0';
            }
            int size = (PT.isUpperCase(ch) && PT.isLowerCase(ch2) ? 2 : 1);
            currentAtom = parseAtom(molecule, null,
                pattern.substring(index, index + size), currentAtom, bond,
                false, false, isBranchAtom);
            bond = null;
            index += size;
          }

        } else {
          throw new InvalidSmilesException("Unexpected character: "
              + getChar(pattern, index));
        }

        ch = getChar(pattern, index);
        if (ch == '}' && checkBrace(molecule, ch, '}'))
          index++;

      }
      // [connections]

      pattern = pattern.substring(index);
      isBranchAtom = false;
    }
  }

  /**
   * set the bond and look for a=a, setting AROMATIC_DOUBLE automatically if found
   * 
   * @param bond
   * @param a1
   * @param a2
   * @param molecule
   */
  private void setBondAtom(SmilesBond bond, SmilesAtom a1, SmilesAtom a2,
                           SmilesSearch molecule) {
    bond.set2a(a1, a2);
    if (molecule != null && bond.order == Edge.BOND_COVALENT_DOUBLE
        && bond.atom1 != null && bond.atom2 != null && bond.atom1.isAromatic
        && bond.atom2.isAromatic
        && ((flags & JC.SMILES_AROMATIC_DOUBLE) == 0))
      molecule.setFlags(flags = (flags | JC.SMILES_AROMATIC_DOUBLE));
  }

  static int getRingNumber(String pattern, int index, char ch, int[] ret) throws InvalidSmilesException {
    int ringNumber;
    switch (ch) {
    case '%':
      // [ringPoint]
      if (getChar(pattern, index + 1) == '(') { // %(nnn)
        String subPattern = getSubPattern(pattern, index + 1, '(');
        getDigits(subPattern, 0, ret);
        index += subPattern.length() + 3;
        if (ret[0] < 0)
          throw new InvalidSmilesException("Invalid number designation: "
              + subPattern);
      } else {
        if (index + 3 <= pattern.length())
          index = getDigits(pattern.substring(0, index + 3), index + 1,
              ret);
        if (ret[0] < 10)
          throw new InvalidSmilesException(
              "Two digits must follow the % sign");
      }
      ringNumber = ret[0];
      break;
    default:
      ringNumber = ch - '0';
      index++;
    }
    ret[0] = ringNumber;
    return index;
  }

  private int checkBioType(String pattern, int index) {
    isBioSequence = (pattern.charAt(index) == '~');
    if (isBioSequence) {
      index++;
      bioType = '*';
      char ch = getChar(pattern, 2);
      if (ch == '~'
          && ((ch = pattern.charAt(1)) == '*' || PT.isLowerCase(ch))) {
        bioType = ch;
        index = 3;
      }
    }
    return index;
  }

  private void parseMeasure(SmilesSearch molecule, String strMeasure,
                            SmilesAtom currentAtom)
      throws InvalidSmilesException {
    // parsing of C(.d:1.5,1.6)C
    // or C(.d1:1.5-1.6)C(.d1)
    // or C(.d1:!1.5-1.6)C(.d1)
    int pt = strMeasure.indexOf(":");
    String id = (pt < 0 ? strMeasure : strMeasure.substring(0, pt));
    while (pt != 0) { // no real repeat here -- just an enclosure for break
      int len = id.length();
      if (len == 1)
        id += "0";
      SmilesMeasure m = htMeasures.get(id);
      if ((m == null) == (pt < 0) || len == 0)
        break;
      try {
        if (pt > 0) {
          int type = (SmilesMeasure.TYPES.indexOf(id.charAt(0)));
          if (type < 2)
            break;
          int[] ret = new int[1];
          getDigits(id, 1, ret);
          int index = ret[0];
          strMeasure = strMeasure.substring(pt + 1);
          boolean isNot = strMeasure.startsWith("!");
          if (isNot)
            strMeasure = strMeasure.substring(1);          
          boolean isNegative = (strMeasure.startsWith("-"));
          if (isNegative)
            strMeasure = strMeasure.substring(1);
          strMeasure = PT.rep(strMeasure, "-", ",");
          strMeasure = PT.rep(strMeasure, ",,", ",-");
          if (isNegative)
            strMeasure = "-" + strMeasure;
          String[] tokens = PT.split(strMeasure, ",");
          if(tokens.length % 2 == 1 || isNot && tokens.length != 2)
            break;
          float[] vals = new float[tokens.length];
          int i = tokens.length;
          for (; --i >= 0;)
             if (Float.isNaN(vals[i] = PT.fVal(tokens[i])))
               break;
          if (i >= 0)
            break;
          m = new SmilesMeasure(molecule, index, type, isNot, vals);
          molecule.measures.addLast(m);
          if (index > 0)
            htMeasures.put(id, m);
          else if (index == 0 && Logger.debugging)
            Logger.debug("measure created: " + m);
        } else {
          if (!m.addPoint(currentAtom.index))
            break;
          if (m.nPoints == m.type) {
            htMeasures.remove(id);
            if (Logger.debugging)
              Logger.debug("measure created: " + m);
          }
          return;
        }
        if (!m.addPoint(currentAtom.index))
          break;
      } catch (NumberFormatException e) {
        break;
      }
      return;
    }
    throw new InvalidSmilesException("invalid measure: " + strMeasure);
  }

  private boolean checkBrace(SmilesSearch molecule, char ch, char type)
      throws InvalidSmilesException {
    switch (ch) {
    case '{':
      if (ch != type)
        break;
      // 3D SEARCH {C}CCC{C}C subset selection
      braceCount++;
      molecule.top.haveSelected = true;
      return true;
    case '}':
      if (ch != type)
        break;
      if (braceCount > 0) {
        braceCount--;
        return true;
      }
      break;
    default:
      return false;
    }
    throw new InvalidSmilesException("Unmatched '}'");
  }

  private String parseNested(SmilesSearch molecule, String pattern,
                             String prefix) throws InvalidSmilesException {
    int index;
    prefix = "$(" + prefix;
    while ((index = pattern.lastIndexOf(prefix)) >= 0) {
      String s = getSubPattern(pattern, index + 1, '(');
      int pt = index + s.length() + 3;
      String ext = pattern.substring(pt);
      pattern = pattern.substring(0, index);
      // [$(aaN)$(aaa[CH3])] --> [$(aaN)&$(aaa[CH3])]
      // [$(aaN)c$(aaa[CH3])] --> [$(aaN)&$([c])&$(aaa)]
      // [$(aaN)!$(aaa[CH3])] --> [$(aaN)&!$(aaa[CH3])]
      // [$(aaN)c,C] --> [$(aaN)&c,C)]
      String pre = "";
      if (prefix.length() == 2 && index > 1 && !pattern.endsWith("!")) {
        pt = pattern.length();
        if (pt > 0 && ",;&".indexOf(pattern.substring(pt - 1)) >= 0) {
          pre = pattern.substring(pt - 1, pt) + pre;
          pattern = pattern.substring(pt - 1);
        } else if (pt > 1) {
          pre = "&" + pre;           
        }
      }
      if (ext.length() > 1 & ",;&".indexOf(ext.charAt(0)) <0)
        ext = "&" + ext;
      pattern = pattern + pre + "_" + molecule.addNested(s) + "_" + ext;
    }
    return pattern;
  }

  /**
   * variables can be defined, as in 
   * 
   * select within(SMARTS,'$R1="[CH3, NH2]";$R2="[$([$R1]),OH]"; {a}[$R2]')
   * 
   * select within(SMARTS,'$R1="[CH3,NH2]";$R2="[OH]";  {a}[$([$R1]),$([$R2])]')
   * 
   * "select aromatic atoms bearing CH3, NH2, or OH"
   * 
   * 
   * @param pattern
   * @return substituted pattern
   * @throws InvalidSmilesException
   */
  private String parseVariables(String pattern) throws InvalidSmilesException {
    Lst<String> keys = new  Lst<String>();
    Lst<String> values = new  Lst<String>();
    int index;
    int ipt = 0;
    int iptLast = -1;
    if (Logger.debugging)
      Logger.info(pattern);
    while ((index = pattern.indexOf("$", ipt)) >= 0) {
      if (getChar(pattern, index + 1) == '(') // was ipt???
        break;
      ipt = skipTo(pattern, index, '=');
      if (ipt <= index + 1 || getChar(pattern, ipt + 1) != '\"')
        break;
      String key = pattern.substring(index, ipt);
      if (key.lastIndexOf('$') > 0 || key.indexOf(']') > 0)
        throw new InvalidSmilesException("Invalid variable name: " + key);
      String s = getSubPattern(pattern, ipt + 1, '\"');
      keys.addLast("[" + key + "]");
      values.addLast(s);
      ipt += s.length() + 2;
      ipt = skipTo(pattern, ipt, ';');
      iptLast = ++ipt;
    }
    if (iptLast < 0)
      return pattern;
    pattern = pattern.substring(iptLast);
    for (int i = keys.size(); --i >= 0;) {
      String k = keys.get(i);
      String v = values.get(i);
      if (!v.equals(k))
        pattern = PT.rep(pattern, k, v);
    }
    if (Logger.debugging)
      Logger.info(pattern);
    return pattern;
  }

  /**
   * Parses an atom definition
   * 
   * @param molecule
   *        Resulting molecule
   * @param atomSet
   * @param pattern
   *        SMILES String
   * @param currentAtom
   *        Current atom
   * @param bond
   * @param isBracketed
   *        Indicates if is a isBracketed definition (between [])
   * @param isPrimitive
   * @param isBranchAtom
   * @return New atom
   * @throws InvalidSmilesException
   */
  private SmilesAtom parseAtom(SmilesSearch molecule, SmilesAtom atomSet,
                               String pattern, SmilesAtom currentAtom,
                               SmilesBond bond, boolean isBracketed,
                               boolean isPrimitive, boolean isBranchAtom)
      throws InvalidSmilesException {
    if (pattern == null || pattern.length() == 0)
      throw new InvalidSmilesException("Empty atom definition");

    SmilesAtom newAtom = new SmilesAtom();
    if (componentParenCount > 0)
      newAtom.component = componentCount;
    if (atomSet == null)
      molecule.appendAtom(newAtom);
    boolean isNewAtom = true;
    if (!checkLogic(molecule, pattern, newAtom, null, currentAtom, isPrimitive,
        isBranchAtom, null)) {
      int[] ret = new int[1];

      if (isBioSequence && pattern.length() == 1)
        pattern += ".0";
      char ch = pattern.charAt(0);

      int index = 0;
      boolean isNot = false;
      if (isSmarts && ch == '!') {
        ch = getChar(pattern, ++index);
        if (ch == '\0')
          throw new InvalidSmilesException("invalid '!'");
        newAtom.not = isNot = true;
      }
      int biopt = pattern.indexOf('.');
      if (biopt >= 0) {
        newAtom.isBioResidue = true;
        // res#nn.name
        String resOrName = pattern.substring(index, biopt);
        pattern = pattern.substring(biopt + 1).toUpperCase();
        // res#nn
        if ((biopt = resOrName.indexOf("#")) >= 0) {
          getDigits(resOrName, biopt + 1, ret);
          resOrName = resOrName.substring(0, biopt);
          newAtom.residueNumber = ret[0];
        }
        if (resOrName.length() == 0)
          resOrName = "*";
        if (resOrName.length() > 1)
          newAtom.residueName = resOrName.toUpperCase();
        else if (!resOrName.equals("*"))
          newAtom.residueChar = resOrName;
        // *.name#n
        resOrName = pattern;
        if ((biopt = resOrName.indexOf("#")) >= 0) {
          getDigits(resOrName, biopt + 1, ret);
          newAtom.elementNumber = ret[0]; // this can be important for unusual groups
          resOrName = resOrName.substring(0, biopt);
        }
        if (resOrName.length() == 0)
          resOrName = "*";
        else if (resOrName.equals("0"))
          resOrName = "\0"; // lead atom
        if (resOrName.equals("*"))
          newAtom.isBioAtomWild = true;
        else
          newAtom.setAtomName(resOrName);
        ch = '\0';
      }
      newAtom.setBioAtom(bioType);
      int hydrogenCount = Integer.MIN_VALUE;
      while (ch != '\0' && isNewAtom) {
        newAtom.setAtomName(isBioSequence ? "\0" : "");
        if (PT.isDigit(ch)) {
          index = getDigits(pattern, index, ret);
          int mass = ret[0];
          if (mass == Integer.MIN_VALUE)
            throw new InvalidSmilesException("Non numeric atomic mass");
          if (getChar(pattern, index) == '?') {
            index++;
            mass = -mass; // or undefined
          }
          if (newAtom.elementDefined)
            throw new InvalidSmilesException(
                "atom mass must precede atom symbol or be separated from it with \";\"");
          newAtom.setAtomicMass(mass);
        } else {
          switch (ch) {
          case '"':
            String type = PT.getQuotedStringAt(pattern, index);
            index += type.length() + 2;
            newAtom.setAtomType(type);
            break;
          case '_': // $(...) nesting
            index = getDigits(pattern, index + 1, ret) + 1;
            if (ret[0] == Integer.MIN_VALUE)
              throw new InvalidSmilesException("Invalid SEARCH primitive: "
                  + pattern.substring(index));
            newAtom.iNested = ret[0];
            if (isBioSequence && isBracketed) {
              if (index != pattern.length())
                throw new InvalidSmilesException("invalid characters: "
                    + pattern.substring(index));
            }
            break;
          case '=':
            index = getDigits(pattern, index + 1, ret);
            newAtom.jmolIndex = ret[0];
            break;
          case '#':
            boolean isAtomNo = (pattern.charAt(index + 1) == '-');
            index = getDigits(pattern, index + (isAtomNo ? 2 : 1), ret);
            if (isAtomNo)
              newAtom.atomNumber = ret[0];
            else
              newAtom.elementNumber = ret[0];
            break;
          // does not work
          //          case '(':
          //            // JmolSMARTS, JmolSMILES reference to atom
          //            String name = getSubPattern(pattern, index, '(');
          //            index += 2 + name.length();
          //            newAtom = checkReference(newAtom, name, ret);
          //            isNewAtom = (ret[0] == 1); // we are done here
          //            if (!isNewAtom) {
          //              if (isNot)
          //                index = 0; // triggers an error
          //              isNot = true; // flags that this must be the end
          //            }
          //            break;
          case '-':
          case '+':
            index = checkCharge(pattern, index, newAtom);
            break;
          case '@':
            if (molecule.stereo == null)
              molecule.stereo = SmilesStereo.newStereo(null);
            index = SmilesStereo.checkChirality(pattern, index,
                molecule.patternAtoms[newAtom.index]);
            break;
          case ':': //openSmiles application-dependent atom class
            // must be non-negative
            index = getDigits(pattern, ++index, ret);
            if (ret[0] == Integer.MIN_VALUE)
              throw new InvalidSmilesException("Invalid atom class");
            newAtom.atomClass = ret[0];
            break;
          default:
            // SMARTS has ambiguities in terms of chaining without &.
            // H alone is "one H atom"
            // "!H" is "not hydrogen" but "!H2" is "not two attached hydrogens"
            // [Rh] could be rhodium or "Any ring atom with at least one implicit H atom"
            // [Ar] could be argon or "Any aliphatic ring atom"
            // however it has been pointed out by a reviewer that [Cr3] must be 
            // interpreted as chromium-3.
            // There is no ambiguity, though, with [Rh3] or [Ar6] because
            // in those cases the number forces the single-character property
            // "h3" or "r6", thus forcing "R" or "A"
            // also, Jmol has Xx
            // We manage this by simply asserting that if & is not used and
            // the first two-letters of the expression could be interpreted
            // as a symbol, AND the next character is not a digit, then it WILL be interpreted as a symbol
            char nextChar = getChar(pattern, index + 1);
            // 14.4.5: removed to require [C;r3] not [Cr3] for C in a 3-membered ring
            //                        && (!isBracketed || !PT.isDigit(getChar(pattern, index + 2))) 
            int len = index + (PT.isLowerCase(nextChar) ? 2 : 1);
            String sym2 = pattern.substring(index + 1, len);
            String symbol = Character.toUpperCase(ch) + sym2;
            boolean mustBeSymbol = true;
            boolean checkForPrimitive = (isBracketed && PT.isLetter(ch));
            if (checkForPrimitive) {
              if (!isNot && (isPrimitive ? atomSet : newAtom).hasSymbol) {
                // if we already have a symbol, and we aren't negating,
                // then this MUST be a property
                // because you can't have [C&O], but you can have [R&!O&!C]
                // We also allow [C&!O], though that's not particularly useful.
                mustBeSymbol = false;
              } else if (ch == 'H') {
                // "H" by itself is hydrogen, not H1
                mustBeSymbol = (pattern.length() == 1 || !PT.isDigit(nextChar));
              } else if (PT.isDigit(nextChar)) {
                mustBeSymbol = false;
              } else if (!symbol.equals("A") && !symbol.equals("Xx")) {
                // check for two-character symbol, then one-character symbol
                mustBeSymbol = ((ch == 'h' ? len == 2 : true) && Elements
                    .elementNumberFromSymbol(symbol, true) > 0);
                if (!mustBeSymbol && len == 2) {
                  sym2 = "";
                  symbol = symbol.substring(0, 1);
                  mustBeSymbol = (Elements
                      .elementNumberFromSymbol(symbol, true) > 0);
                }
              }
            }
            if (mustBeSymbol) {
              if (!isBracketed && !isSmarts && !isBioSequence
                  && !SmilesAtom.allowSmilesUnbracketed(symbol)
                  || !newAtom.setSymbol(symbol = ch + sym2))
                throw new InvalidSmilesException("Invalid atom symbol: "
                    + symbol);
              if (isPrimitive)
                atomSet.hasSymbol = true;
              // indicates we have already assigned an atom number
              //if (!symbol.equals("*"))
              //molecule.parent.needAromatic = true;
              index += symbol.length();
            } else {
              index = getDigits(pattern, index + 1, ret);
              int val = ret[0];
              switch (ch) {
              default:
                throw new InvalidSmilesException("Invalid SEARCH primitive: "
                    + pattern.substring(index));
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
                // default >= 1
                newAtom.setImplicitHydrogenCount(val == Integer.MIN_VALUE ? -1
                    : val);
                break;
              case 'R':
                if (val == Integer.MIN_VALUE)
                  val = -1; // R --> !R0; !R --> R0
                newAtom.setRingMembership(val);
                molecule.top.needRingData = true;
                break;
              case 'r':
                if (val == Integer.MIN_VALUE) {
                  val = -1; // r --> !R0; !r --> R0
                  newAtom.setRingMembership(val);
                } else {
                  // 500 --> aromatic 5-membered ring
                  // 600 --> aromatic 6-membered ring
                  newAtom.setRingSize(val);
                  switch (val) {
                  case 500:
                    val = 5;
                    break;
                  case 600:
                    val = 6;
                    break;
                  }
                  if (val > molecule.ringDataMax)
                    molecule.ringDataMax = val;
                }
                molecule.top.needRingData = true;
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
                molecule.top.needRingData = true;
                break;
              }
            }
          }
        }
        ch = getChar(pattern, index);
        if (isNot && ch != '\0')
          throw new InvalidSmilesException(
              "'!' may only involve one primitive.");
      }
      if (hydrogenCount == Integer.MIN_VALUE && isBracketed)
        hydrogenCount = Integer.MIN_VALUE + 1;
      newAtom.setExplicitHydrogenCount(hydrogenCount);
      // for stereochemistry only:
      molecule.patternAtoms[newAtom.index]
          .setExplicitHydrogenCount(hydrogenCount);
    }
    if (braceCount > 0)
      newAtom.selected = true;
    if (isNewAtom) {
      if (atomSet != null) {
        if (isPrimitive)
          atomSet.appendPrimitive(newAtom);
        else
          atomSet.appendAtomOr(newAtom);
      }
    }

    // Final check

    if (currentAtom != null && bond.order == SmilesBond.TYPE_NONE) {
      newAtom.notBondedIndex = currentAtom.index;
    }
    if (currentAtom != null && bond.order != SmilesBond.TYPE_NONE) {
      if (bond.order == SmilesBond.TYPE_UNKNOWN)
        bond.order = (isBioSequence && isBranchAtom ? SmilesBond.TYPE_BIO_CROSSLINK
            : isSmarts || currentAtom.isAromatic && newAtom.isAromatic ? SmilesBond.TYPE_ANY
                : Edge.BOND_COVALENT_SINGLE);
      if (!isBracketed)
        setBondAtom(bond, null, newAtom, molecule);
      if (branchLevel == 0
          && (bond.order == SmilesBond.TYPE_AROMATIC || bond.order == SmilesBond.TYPE_BIO_CROSSLINK))
        branchLevel++;
    }
    // if (Logger.debugging)
    // Logger.debug("new atom: " + newAtom);
    if (branchLevel == 0)
      molecule.lastChainAtom = newAtom;
    return newAtom;
  }

//  private Map<String, SmilesAtom> atomRefs;
  
//  /**
//   * allow for [(...)] to indicate a specific pattern atom
//   * 
//   * @param newAtom
//   * @param name
//   * @param ret
//   *        set [0] to 1 for new atom; 0 otherwise
//   * @return new or old atom
//   */
//  private SmilesAtom checkReference(SmilesAtom newAtom, String name, int[] ret) {
//    if (atomRefs == null)
//      atomRefs = new Hashtable<String, SmilesAtom>();
//    SmilesAtom ref = atomRefs.get(name);
//    if (ref == null) {
//      // this is a new atom
//      atomRefs.put(newAtom.referance = name, ref = newAtom);
//      if (!newAtom.hasSymbol) {
//        if (name.length() > 0) {
//          String s = null;
//          if (name.length() >= 2
//              && (Elements.elementNumberFromSymbol(s = name.substring(0, 2),
//                  true) > 0 || Elements.elementNumberFromSymbol(
//                  s = name.substring(0, 1), true) > 0)) {
//            newAtom.setSymbol(s);
//          }
//        }
//      }
//      ret[0] = 1;
//    } else {
//      ret[0] = 0;
//    }
//    return ref;
//  }

  /**
   * Parses a ring definition
   * 
   * @param molecule
   *        Resulting molecule
   * @param ringNum
   * @param currentAtom
   *        Current atom
   * @param bond
   * @throws InvalidSmilesException
   */
  private void parseConnection(SmilesSearch molecule, int ringNum,
                         SmilesAtom currentAtom, SmilesBond bond) throws InvalidSmilesException {

    // Ring management
      Integer r = Integer.valueOf(ringNum);
      SmilesBond bond0 = connections.get(r);
      if (bond0 == null) {
        connections.put(r, bond);
        molecule.top.ringCount++;
        return;
      }
      connections.remove(r);
    // wnen the bond type is unknown, we go with:
    // (1) the other end, if defined there
    // (2) or if SMARTS, "ANY"
    // (3) or if both atoms are aromatic, "ANY"
    // (4) or "SINGLE"
    // we must check for C1......C/1....
    // in which case the "/" is referring to "second to first" not "first to second"
    switch (bond.order) {
    case SmilesBond.TYPE_UNKNOWN:
      bond.order = (bond0.order != SmilesBond.TYPE_UNKNOWN ? bond0.order
          : isSmarts || currentAtom.isAromatic && bond0.atom1.isAromatic ? SmilesBond.TYPE_ANY
              : Edge.BOND_COVALENT_SINGLE);
      break;
    case Edge.BOND_STEREO_NEAR:
      bond.order = Edge.BOND_STEREO_FAR;
      break;
    case Edge.BOND_STEREO_FAR:
      bond.order = Edge.BOND_STEREO_NEAR;
      break;
    }
    if (bond0.order != SmilesBond.TYPE_UNKNOWN && bond0.order != bond.order 
        || currentAtom == bond0.atom1
        || bond0.atom1.getBondTo(currentAtom) != null)
      throw new InvalidSmilesException("Bad connection type or atom");
    bond0.set(bond);
    currentAtom.bondCount--;
    bond0.setAtom2(currentAtom, molecule);
  }

  private int checkCharge(String pattern, int index, SmilesAtom newAtom)
      throws InvalidSmilesException {
    // Charge
    int len = pattern.length();
    char ch = pattern.charAt(index);
    int count = 1;
    ++index;
    if (index < len) {
      char nextChar = pattern.charAt(index);
      if (PT.isDigit(nextChar)) {
        int[] ret = new int[1];
        index = getDigits(pattern, index, ret);
        count = ret[0];
        if (count == Integer.MIN_VALUE)
          throw new InvalidSmilesException("Non numeric charge");
      } else {
        while (index < len && pattern.charAt(index) == ch) {
          index++;
          count++;
        }
      }
    }
    newAtom.setCharge(ch == '+' ? count : -count);
    return index;
  }

  private SmilesBond parseBond(SmilesSearch molecule, SmilesBond bondSet,
                               String pattern, SmilesBond bond,
                               SmilesAtom currentAtom, boolean isPrimitive,
                               boolean isBranchAtom, int len, int[] ret)
      throws InvalidSmilesException {

    // pattern length will be 1 or 2 only, x or !x

    char ch;
    if (len > 0) {
      switch (ch = pattern.charAt(0)) {
      case '>':
        if (!pattern.equals(">>")) {  // reaction  separator as "."
          len  = -1;
          break;
        }
        //$FALL-THROUGH$
      case '.':
        if (bond == null && bondSet == null) {
          isBioSequence = (getChar(pattern, 1) == '~');
          return new SmilesBond(null, null, SmilesBond.TYPE_NONE, false);
        }
        len = -1;
        break;
      case '+':
        if (bondSet != null)
          len = -1;
        break;
      }
    } else {
      ch = '\0';
    }

    SmilesBond newBond = (bondSet == null ? (bond == null ? new SmilesBond(
        currentAtom,
        null,
        (isBioSequence && currentAtom != null ? (isBranchAtom ? SmilesBond.TYPE_BIO_CROSSLINK
            : SmilesBond.TYPE_BIO_SEQUENCE)
            : SmilesBond.TYPE_UNKNOWN), false)
        : bond)
        : isPrimitive ? bondSet.addPrimitive() : bondSet.addBondOr());

//    still having some problems here. 
    if (len > 0
        && !checkLogic(molecule, pattern, null, newBond, currentAtom,
            isPrimitive, false, ret)) {
      boolean isBondNot = (ch == '!');
      if (isBondNot) {
        ch = getChar(pattern, 1);
        if (ch == '\0' || ch == '!')
          throw new InvalidSmilesException("invalid '!'");
      }
      int bondType = SmilesBond.getBondTypeFromCode(ch);
      if (bondType == SmilesBond.TYPE_RING)
        molecule.top.needRingMemberships = true;
      if (currentAtom == null && bondType != SmilesBond.TYPE_NONE)
        throw new InvalidSmilesException("Bond without a previous atom");
      switch (bondType) {
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        // looking here for ^nn- or ^^nn-
        if ((len = pattern.length()) < (isBondNot ? 3 : 2) || pattern.charAt(len - 1) != '-') {
          len = 0;
        } else {
          if (len == (isBondNot ? 3 : 2)) {
            newBond.setAtropType(22);
          } else {
            getDigits(pattern, (isBondNot ? 2 : 1), ret);
            newBond.setAtropType(ret[0]);
          }
        }
        molecule.haveBondStereochemistry = true;
        break;
      case Edge.BOND_STEREO_NEAR:
      case Edge.BOND_STEREO_FAR:
        bondDirectionPaired = !bondDirectionPaired;
        molecule.haveBondStereochemistry = true;
        break;
      case SmilesBond.TYPE_AROMATIC:
        //if (!isBioSequence)
        //molecule.parent.needAromatic = true;
        break;
      case Edge.BOND_COVALENT_DOUBLE:
        molecule.top.nDouble++;
        //$FALL-THROUGH$
      case Edge.BOND_COVALENT_SINGLE:
        if (currentAtom.isAromatic)
          molecule.top.needRingData = true;
        break;
      }
      newBond.set2(bondType, isBondNot);
      // in the case of a bioSequence, we also mark the 
      // parent bond, especially if NOT
      if (isBioSequence && bondSet != null)
        bondSet.set2(bondType, isBondNot);
    }
    if (len == -1)
      throw new InvalidSmilesException("invalid bond:" + ch);
    return newBond;
  }

  private boolean checkLogic(SmilesSearch molecule, String pattern,
                             SmilesAtom atom, SmilesBond bond,
                             SmilesAtom currentAtom, boolean isPrimitive,
                             boolean isBranchAtom, int[] ret)
      throws InvalidSmilesException {
    int pt = pattern.lastIndexOf("!");
    while (pt > 0) {
      if (",;&!".indexOf(pattern.charAt(pt - 1)) < 0)
        pattern = pattern.substring(0, pt) + "&" + pattern.substring(pt);
      pt = pattern.lastIndexOf("!", pt - 1);
    }
    pt = pattern.indexOf(',');
    int len = pattern.length();
    out: while (true) {
      boolean haveOr = (pt > 0);
      if (haveOr && !isSmarts || pt == 0)
        break;
      String props = "";
      pt = pattern.indexOf(';');
      if (pt >= 0) {
        if (!isSmarts || pt == 0)
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
            throw new InvalidSmilesException("missing "
                + (bond == null ? "atom" : "bond") + " token");
          if (bond == null)
            parseAtom(molecule, atom, s, null, null, true, false, isBranchAtom);
          else
            parseBond(molecule, bond, s, null, currentAtom, false, false,
                s.length(), ret);
          index = pt + 1;
        }
      } else if ((pt = pattern.indexOf('&')) >= 0 || bond != null && len > 1
          && !isPrimitive) {
        // process primitive
        if (pt == 0 || bond == null && !isSmarts)
          break;
        if (bond != null && pt < 0) {
          // bonds are simpler, because they have only one character
          if (len > 1) {
            SB sNew = new SB();
            for (int i = 0; i < len;) {
              char ch = pattern.charAt(i++);
              sNew.appendC(ch);
              switch (ch) {
              case '!':
                if (!isSmarts)
                  break out;
                continue;
              case '^':
              case '`':
                while ((ch = pattern.charAt(i++)) != '-' && ch != '\0'){
                  sNew.appendC(ch);
                }
                sNew.appendC('-');
                break;
              }
              if (i < len) {
                if (!isSmarts)
                  break out;
                sNew.appendC('&');
              }
            }
            pattern = sNew.toString();
            len = pattern.length();
          }
        }
        pattern += "&";
        while ((pt = pattern.indexOf('&', index)) > 0 && pt <= len) {
          String s = pattern.substring(index, pt) + props;
          if (bond == null)
            parseAtom(molecule, atom, s, null, null, true, true, isBranchAtom);
          else
            parseBond(molecule, isSmarts ? bond : null, s, isSmarts ? null : bond, currentAtom, true, false,
                s.length(), ret);
          index = pt + 1;
        }
      } else {
        return false;
      }
      return true;
    }
    char ch = pattern.charAt(pt);
    throw new InvalidSmilesException((isSmarts ? "invalid placement for '" + ch
        + "'" : "[" + ch + "] notation only valid with SMARTS, not SMILES,")
        + " in " + pattern);
  }

  static String getSubPattern(String pattern, int index, char ch)
      throws InvalidSmilesException {
    char ch2;
    int margin = 1;
    switch (ch) {
    case '[':
      ch2 = ']';
      break;
    case '"':
    case '%':
    case '/':
      ch2 = ch;
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

  static char getChar(String pattern, int i) {
    return (i < pattern.length() ? pattern.charAt(i) : '\0');
  }

  /**
   * 
   * @param pattern
   * @param index
   * @param ret
   * @return pointer to the character AFTER the digits
   */
  static int getDigits(String pattern, int index, int[] ret) {
    int pt = index;
    int len = pattern.length();
    while (pt < len && PT.isDigit(pattern.charAt(pt)))
      pt++;
    if (pt > index)
      try {
        ret[0] = Integer.parseInt(pattern.substring(index, pt));
        return pt;
      } catch (NumberFormatException e) {
      }
    ret[0] = Integer.MIN_VALUE;
    return pt;
  }

  private static int skipTo(String pattern, int index, char ch0) {
    int pt = index;
    char ch;
    while ((ch = getChar(pattern, ++pt)) != ch0 && ch != '\0') {
    }
    return (ch == '\0' ? -1 : pt);
  }

  /**
   * 
   * @param pattern
   * @return  comments and white space removed, also ^^ to '
   */
  static String cleanPattern(String pattern) {
    pattern = PT.replaceAllCharacters(pattern, " \t\n\r", "");
    pattern = PT.rep(pattern, "^^", "`"); // atropisomer "Sa" 
    int i = 0;
    int i2 = 0;
    while ((i = pattern.indexOf("//*")) >= 0
        && (i2 = pattern.indexOf("*//")) >= i)
      pattern = pattern.substring(0, i) + pattern.substring(i2 + 3);
    pattern = PT.rep(pattern, "//", "");
    return pattern;
  }

  static String extractFlags(String pattern, int[] ret) throws InvalidSmilesException {
    pattern = cleanPattern(pattern);
    int flags = 0;
    while (pattern.startsWith("/")) {
      String strFlags = getSubPattern(pattern, 0, '/').toUpperCase();
      pattern = pattern.substring(strFlags.length() + 2);
      flags = SmilesSearch.addFlags(flags,  strFlags);
    }
    ret[0] = flags;
    return pattern;
  }

  static int getFlags(String pattern) throws InvalidSmilesException {
    int[] ret = new int[1];
    extractFlags(pattern, ret);
    return ret[0];
  }


}
