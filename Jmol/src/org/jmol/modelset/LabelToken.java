/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import org.jmol.util.TextFormat;
import org.jmol.viewer.Token;

public class LabelToken {
  
  /*
   * by Bob Hanson, 5/28/2009
   * 
   * a compiler for the atom label business.
   * 
   */

  String text;  
  int tok;
  int pt = -1;
  int pt1 = 0;
  char ch1;
  int width;
  int precision = Integer.MAX_VALUE;
  boolean alignLeft;
  boolean zeroPad;

  // do not change array order without changing string order as well
  final private static String labelTokenParams = "AaBbCcDEefGgIiLlMmNnoPpQqRrSsTtUuVvWXxYyZz%%%gqW";
  final private static int[] labelTokenIds = {
  /* 'A' */Token.altloc,
  /* 'a' */Token.atomName,
  /* 'B' */Token.atomType,
  /* 'b' */Token.temperature,
  /* 'C' */Token.formalCharge,
  /* 'c' */Token.chain,
  /* 'D' */Token.atomIndex,
  /* 'E' */Token.insertion,
  /* 'e' */Token.element,
  /* 'f' */Token.phi,
  /* 'G' */Token.groupindex,
  /* 'g' */'g', //getSelectedGroupIndexWithinChain()
  /* 'I' */Token.radius,
  /* 'i' */Token.atomno,
  /* 'L' */Token.polymerLength,
  /* 'l' */Token.elemno,
  /* 'M' */Token.model,
  /* 'm' */Token.group1,
  /* 'N' */Token.molecule,
  /* 'n' */Token.group,
  /* 'o' */Token.symmetry,
  /* 'P' */Token.partialCharge,
  /* 'p' */Token.psi,
  /* 'Q' */Token.occupancy,
  /* 'q' */'q',  //occupancy * 100
  /* 'R' */Token.resno,
  /* 'r' */Token.sequence,
  /* 'S' */Token.site,
  /* 's' */Token.chain,
  /* 'T' */Token.straightness,
  /* 't' */Token.temperature,
  /* 'U' */Token.identify,
  /* 'u' */Token.surfacedistance,
  /* 'V' */Token.vanderwaals,
  /* 'v' */Token.vibXyz, 
  /* 'W' */'W',   // identifier and XYZ coord
  /* 'X' */Token.fracX, 
  /* 'x' */Token.atomX, 
  /* 'Y' */Token.fracY, 
  /* 'y' */Token.atomY, 
  /* 'Z' */Token.fracZ,
  /* 'z' */Token.atomZ, 

  // not having letter equivalents:
  
           Token.bondcount,
           Token.groupID,
           Token.structure,
           Token.strucno,
           Token.unitX,
           Token.unitY,
           Token.unitZ,
           Token.valence,
           Token.vibX,
           Token.vibY,
           Token.vibZ,
           Token.unitXyz,
           Token.fracXyz,
  };

  private static boolean isLabelPropertyTok(int tok) {
    for (int i = labelTokenIds.length; --i >= 0;)
      if (labelTokenIds[i] == tok)
        return true;
    return false;
  }

  private final static String twoCharLabelTokenParams = "fuv";

  private final static int[] twoCharLabelTokenIds = { Token.fracX, Token.fracY,
      Token.fracZ, Token.unitX, Token.unitY, Token.unitZ, Token.vibX,
      Token.vibY, Token.vibZ, };

  private LabelToken(int pt) {
    this.pt = pt;
  }

  private LabelToken(String text) {
    this.text = text;
  }

  public static LabelToken[] compile(String strFormat, char chAtom) {
    if (strFormat.indexOf("%") < 0)
      return new LabelToken[] { new LabelToken(strFormat) };
    int n = 0;
    int ich = -1;
    int cch = strFormat.length();
    while (++ich < cch && (ich = strFormat.indexOf('%', ich)) >= 0)
      n++;
    LabelToken[] tokens = new LabelToken[n * 2 + 1];
    int ichPercent;
    int i = 0;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0;) {
      if (ich != ichPercent)
        tokens[i++] = new LabelToken(strFormat.substring(ich, ichPercent));
      LabelToken lt = tokens[i++] = new LabelToken(ichPercent);
      ich = setToken(strFormat, lt, cch, chAtom);
    }
    if (ich < cch)
      tokens[i++] = new LabelToken(strFormat.substring(ich));
    return tokens;
  }

  private static int setToken(String strFormat, LabelToken lt, int cch, int chAtom) {
    int ich = lt.pt + 1;
    char ch;
    if (strFormat.charAt(ich) == '-') {
      lt.alignLeft = true;
      ++ich;
    }
    if (strFormat.charAt(ich) == '0') {
      lt.zeroPad = true;
      ++ich;
    }
    while (Character.isDigit(ch = strFormat.charAt(ich))) {
      lt.width = (10 * lt.width) + (ch - '0');
      ++ich;
    }
    lt.precision = Integer.MAX_VALUE;
    if (strFormat.charAt(ich) == '.') {
      ++ich;
      if (Character.isDigit(ch = strFormat.charAt(ich))) {
        lt.precision = ch - '0';
        ++ich;
      }
    }
    switch (ch = strFormat.charAt(ich++)) {
    case '%':
      break;
    case '[':
      int ichClose = strFormat.indexOf(']', ich);
      if (ichClose < ich) {
        ich = cch;
        break;
      }
      String propertyName = strFormat.substring(ich, ichClose);
      Token token = Token.getTokenFromName(propertyName);
      if (token != null && isLabelPropertyTok(token.tok))
        lt.tok = token.tok;
      ich = ichClose + 1;
      break;
    case '{': // client property name
      int ichCloseBracket = strFormat.indexOf('}', ich);
      if (ichCloseBracket < ich) {
        ich = cch;
        break;
      }
      lt.text = strFormat.substring(ich, ichCloseBracket);
      lt.tok = Token.data;
      ich = ichCloseBracket + 1;
      break;
    default:
      int i, i1;
      if (ich < cch 
          && (i = twoCharLabelTokenParams.indexOf(ch)) >= 0
          && (i1 = "xyz".indexOf(strFormat.charAt(ich))) >= 0) {
        lt.tok = twoCharLabelTokenIds[i * 3 + i1];
        ich++;
      } else if ((i = labelTokenParams.indexOf(ch)) >= 0) {
        lt.tok = labelTokenIds[i];
      }
    }
    lt.text = strFormat.substring(lt.pt, ich);
    lt.pt1 = ich;
    if (chAtom != '\0' && ich < cch && Character.isDigit(ch = strFormat.charAt(ich))) {
      ich++;
      lt.ch1 = ch;
      if (ch != chAtom)
        lt.tok = 0;
    }
    return ich;
  }

  public String format(float floatT, String strT) {
    if (!Float.isNaN(floatT))
      return TextFormat.format(floatT, width, precision, alignLeft, zeroPad);
    else if (strT != null)
      return TextFormat.format(strT, width, precision, alignLeft, zeroPad);
    else
      return text;
  }
}
