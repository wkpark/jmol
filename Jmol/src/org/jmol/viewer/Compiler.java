/* $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.viewer;

import org.jmol.util.Logger;
import org.jmol.util.CommandHistory;
import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;

import java.util.Vector;
import java.util.BitSet;

import javax.vecmath.Point3f;

class Compiler {

  private Viewer viewer;
  private String filename;
  private String script;

  private short[] lineNumbers;
  private int[] lineIndices;
  private Token[][] aatokenCompiled;

  private String errorMessage;
  private String errorLine;
  private boolean preDefining;
  private boolean isSilent;
  private boolean isShowScriptOutput;

  private boolean logMessages = false;

  Compiler(Viewer viewer) {
    this.viewer = viewer;
  }

  boolean compile(String filename, String script, boolean isPredefining,
                  boolean isSilent) {
    this.filename = filename;
    this.isSilent = isSilent;
    this.script = cleanScriptComments(script);
    logMessages = (!isSilent && !isPredefining && Logger
        .isActiveLevel(Logger.LEVEL_DEBUG));
    lineNumbers = null;
    lineIndices = null;
    aatokenCompiled = null;
    errorMessage = errorLine = null;
    preDefining = (filename == "#predefine");
    return (compile0() ? true : handleError());
  }

  String getScript() {
    return script;
  }
  
  short[] getLineNumbers() {
    return lineNumbers;
  }

  int[] getLineIndices() {
    return lineIndices;
  }

  Token[][] getAatokenCompiled() {
    return aatokenCompiled;
  }

  static boolean tokAttr(int a, int b) {
    return (a & b) == b;
  }
  
  static int modelValue(String strDecimal) {
    //this will overflow, but it doesn't matter -- it's only for file.model
    //2147483647 is maxvalue, so this allows loading
    //simultaneously up to 2147 files. Yeah, sure!
    int pt = strDecimal.indexOf(".");
    if (pt < 0)
      return 0;
    int i = 0;
    int j = 0;
    if (pt > 0 && (i = Integer.parseInt(strDecimal.substring(0, pt))) < 0)
      i = -i;
    if (pt < strDecimal.length() - 1)
      j = Integer.parseInt(strDecimal.substring(pt + 1));
    return i * 1000000 + j;
  }
  
  private void log(String message) {
    if (logMessages)
      Logger.debug(message);
  }

 /**
   * allows for two kinds of comments. 
   * 
   * 1) /** .... ** /  (closing involved two asterisks and slash together, but that can't be shown here.
   * 2) /* ..... * /  (same deal here).
   * 
   * The reason is that /* ... * / will appear as standard in MOVETO command
   * but we still might want to escape it, so around that you can have /** .... ** /
   * 
   * @param script
   * @return cleaned script
   */
  private static String cleanScriptComments(String script) {
    int pt, pt1;
    while ((pt = script.indexOf("/**")) >= 0) {
      pt1 = script.indexOf("**/", pt + 3);
      if (pt1 < 0)
        break;
      script = script.substring(0, pt) + script.substring(pt1 + 3);
    }
    while ((pt = script.indexOf("/*")) >= 0) {
      pt1 = script.indexOf("*/", pt + 2);
      if (pt1 < 0)
        break;
      script = script.substring(0, pt) + script.substring(pt1 + 2);
    }
    return script;
  }
  
  private int cchScript;
  private short lineCurrent;

  private int ichToken;
  private int cchToken;
  private int iCommand;
  private Token[] atokenCommand;

  private int ichCurrentCommand;
  private boolean isNewSet;

  private boolean iHaveQuotedString = false;
  
  private Vector ltoken;
  private Token lastPrefixToken;
  private void addTokenToPrefix(Token token) {
    ltoken.addElement(token);
    lastPrefixToken = token;
  }
  
  private boolean compile0() {
    cchScript = script.length();
    ichToken = 0;
    lineCurrent = 1;
    iCommand = 0;
    int lnLength = 8;
    lineNumbers = new short[lnLength];
    lineIndices = new int[lnLength];
    isNewSet = false;
    isShowScriptOutput = false;

    Vector lltoken = new Vector();
    ltoken = new Vector();
    int tokCommand = Token.nada;
    isNewSet = false;
    for (int nTokens = 0; true; ichToken += cchToken) {
      if (lookingAtLeadingWhitespace())
        continue;
      nTokens++;
      if (tokCommand == Token.nada)
        ichCurrentCommand = ichToken;
      if (lookingAtComment())
        continue;
      boolean endOfLine = lookingAtEndOfLine();
      if (endOfLine || lookingAtEndOfStatement()) {
        if (tokCommand != Token.nada) {
          if (!compileCommand())
            return false;
          iCommand = lltoken.size();
          if (iCommand == lnLength) {
            short[] lnT = new short[lnLength * 2];
            System.arraycopy(lineNumbers, 0, lnT, 0, lnLength);
            lineNumbers = lnT;
            int[] lnI = new int[lnLength * 2];
            System.arraycopy(lineIndices, 0, lnI, 0, lnLength);
            lineIndices = lnI;
            lnLength *= 2;
          }
          lineNumbers[iCommand] = lineCurrent;
          lineIndices[iCommand] = ichCurrentCommand;
          lltoken.addElement(atokenCommand);
          ltoken.setSize(0);
          tokCommand = Token.nada;
          iHaveQuotedString = false;
          nTokens = 0;
        }
        if (ichToken < cchScript) {
          if (endOfLine)
            ++lineCurrent;
          continue;
        }
        break;
      }
      if (tokCommand == Token.nada) {
        isNewSet = false;
      } else {
        if (lookingAtString()) {
          if (cchToken < 0)
            return endOfCommandUnexpected();
          String str = ((tokCommand == Token.load || tokCommand == Token.script)
              && !iHaveQuotedString ? script.substring(ichToken + 1, ichToken
              + cchToken - 1) : getUnescapedStringLiteral());
          addTokenToPrefix(new Token(Token.string, str));
          iHaveQuotedString = true;
          if (tokCommand == Token.data && str.indexOf("@") < 0)
            getData(str);
          continue;
        }
        if (tokCommand == Token.load) {
          if (lookingAtLoadFormat()) {
            //String strFormat = script.substring(ichToken, ichToken + cchToken);
            //strFormat = strFormat.toLowerCase();
            //addTokenToPrefix(new Token(Token.identifier, strFormat));
            continue;
          }
          if (!iHaveQuotedString && lookingAtSpecialString()) {
            String str = script.substring(ichToken, ichToken + cchToken).trim();
            int pt = str.indexOf(" ");
            if (pt > 0) {
              cchToken = pt;
              str = str.substring(0, pt);
            }
            addTokenToPrefix(new Token(Token.string, str));
            iHaveQuotedString = true;
            continue;
          }
        }
        if (tokCommand == Token.script) {
          if (!iHaveQuotedString && lookingAtSpecialString()) {
            String str = script.substring(ichToken, ichToken + cchToken).trim();
            int pt = str.indexOf(" ");
            if (pt > 0) {
              cchToken = pt;
              str = str.substring(0, pt);
            }
            addTokenToPrefix(new Token(Token.string, str));
            iHaveQuotedString = true;
            continue;
          }
        }
        if (tokCommand == Token.write) {
          int pt = cchToken;
          //write image spt filename
          //write script filename
          //write spt filename
          //write jpg filename
          //write filename

          if (nTokens > 2 && !iHaveQuotedString && lookingAtSpecialString()) {
            String str = script.substring(ichToken, ichToken + cchToken).trim();
            if (str.indexOf(" ") < 0) {
              addTokenToPrefix(new Token(Token.string, str));
              iHaveQuotedString = true;
              continue;
            }
            cchToken = pt;
          }
        }
        if (!(tokCommand == Token.script && iHaveQuotedString)
            && tokAttr(tokCommand, Token.specialstring)
            && lookingAtSpecialString()) {
          String str = script.substring(ichToken, ichToken + cchToken);
          addTokenToPrefix(new Token(Token.string, str));
          continue;
        }
        float value;
        if (!Float.isNaN(value = lookingAtExponential())) {
          addTokenToPrefix(new Token(Token.decimal, new Float(value)));
          continue;
        }
        if (lookingAtDecimal()) {
          value =
          // can't use parseFloat with jvm 1.1
          // Float.parseFloat(script.substring(ichToken, ichToken + cchToken));
          Float.valueOf(script.substring(ichToken, ichToken + cchToken))
              .floatValue();
          int intValue = (modelValue(script.substring(ichToken, ichToken
              + cchToken)));
          ltoken
              .addElement(new Token(Token.decimal, intValue, new Float(value)));
          continue;
        }
        if (lookingAtSeqcode()) {
          char ch = script.charAt(ichToken);
          int seqNum = (ch == '*' || ch == '^' ? 0 : Integer.parseInt(script
              .substring(ichToken, ichToken + cchToken - 2)));
          char insertionCode = script.charAt(ichToken + cchToken - 1);
          if (insertionCode == '^')
            insertionCode = ' ';
          int seqcode = Group.getSeqcode(seqNum, insertionCode);
          addTokenToPrefix(new Token(Token.seqcode, seqcode, "seqcode"));
          continue;
        }
        if (lookingAtInteger(tokAttr(tokCommand, Token.negnums))) {
          String intString = script.substring(ichToken, ichToken + cchToken);
          int val = Integer.parseInt(intString);
          addTokenToPrefix(new Token(Token.integer, val, intString));
          continue;
        }
        if (!tokenAttr(lastPrefixToken, Token.mathfunc)) {
          // don't want to mess up x.distance({1 2 3})
          // if you want to use a bitset there, you must use 
          // bitsets properly: x.distance( ({1 2 3}) )
          boolean isBond = (script.charAt(ichToken) == '[');
          BitSet bs = lookingAtBitset();
          if (bs != null) {
            if (isBond)
              addTokenToPrefix(new Token(Token.bitset, new BondSet(bs)));
            else
            addTokenToPrefix(new Token(Token.bitset, bs));
            continue;
          }
        }
      }
      if (lookingAtLookupToken()) {
        String ident = script.substring(ichToken, ichToken + cchToken);
        Token token;
        // hack to support case sensitive alternate locations and chains
        // if an identifier is a single character long, then
        // allocate a new Token with the original character preserved
        if (ident.length() == 1) {
          if ((token = (Token) Token.map.get(ident)) == null) {
            String lowerCaseIdent = ident.toLowerCase();
            if ((token = (Token) Token.map.get(lowerCaseIdent)) != null)
              token = new Token(token.tok, token.intValue, ident);
          }
        } else {
          ident = ident.toLowerCase();
          token = (Token) Token.map.get(ident);
        }
        if (token == null)
          token = new Token(Token.identifier, ident);
        int tok = token.tok;
        switch (tokCommand) {
        // special cases
        case Token.nada:
          ichCurrentCommand = ichToken;
          tokCommand = tok;
          if (tokAttr(tokCommand, Token.command))
            break;
          if (!tokAttr(tok, Token.identifier))
            return commandExpected();
          tokCommand = Token.set;
          isNewSet = true;
          break;
        case Token.set:
          if (ltoken.size() == 1) {
            if (tok == Token.opEQ) {
              token = (Token) ltoken.get(0);
              ltoken.remove(0);
              ltoken.add(new Token(Token.set, Token.varArgCount, "set"));
              tok = token.tok;
              tokCommand = Token.set;
            }
            if (tok != Token.identifier && !tokAttr(tok, Token.setparam))
              return isNewSet ? commandExpected() : unrecognizedParameter(
                  "SET", ident);
          }
          break;
        case Token.define:
          if (ltoken.size() == 1) {
            // we are looking at the variable name

            if (!preDefining && tok != Token.identifier) {
              if (!tokAttr(tok, Token.predefinedset)) {
                Logger.warn("WARNING: redefining " + ident + "; was " + token);
                tok = token.tok = Token.identifier;
                Token.map.put(ident, token);
                Logger
                    .warn("WARNING: not all commands may continue to be functional for the life of the applet!");
              } else {
                Logger
                    .warn("WARNING: predefined term '"
                        + ident
                        + "' has been redefined by the user until the next file load.");
              }
            }

            if (tok != Token.identifier && !tokAttr(tok, Token.predefinedset))
              return invalidExpressionToken(ident);
          } else {
            // we are looking at the expression
            if (tok != Token.identifier && tok != Token.set
                && !(tokAttrOr(tok, Token.expression, Token.predefinedset)))
              return invalidExpressionToken(ident);
          }
          break;
        case Token.center:
          if (tok != Token.identifier && tok != Token.dollarsign
              && !tokAttr(tok, Token.expression))
            return invalidExpressionToken(ident);
          break;
        case Token.restrict:
        case Token.select:
        case Token.display:
        case Token.hide:
          if (tok != Token.identifier && !tokAttr(tok, Token.expression))
            return invalidExpressionToken(ident);
          break;
        }
        addTokenToPrefix(token);
        continue;
      }
      if (ltoken.size() == 0 || isNewSet && ltoken.size() == 1)
        return commandExpected();
      return unrecognizedToken(script);
    }
    aatokenCompiled = new Token[lltoken.size()][];
    lltoken.copyInto(aatokenCompiled);
    return true;
  }

  private void getData(String key) {
    ichToken += key.length() + 2;
    if (script.length() > ichToken && script.charAt(ichToken) == '\r')
      ichToken++;
    if (script.length() > ichToken && script.charAt(ichToken) == '\n')
      ichToken++;
    int i = script.indexOf("end \"" + key + "\"", ichToken);
    if (i < 0)
      i = script.length();
    String str = script.substring(ichToken, i);
    addTokenToPrefix(new Token(Token.data, str));
    cchToken = i - ichToken + 6 + key.length();
  }

  private static boolean isSpaceOrTab(char ch) {
    return ch == ' ' || ch == '\t';
  }

  private static boolean eol(char ch) {
    return (ch == ';' || ch == '\r' || ch == '\n');  
  }
  
  private boolean lookingAtLeadingWhitespace() {
    //log("lookingAtLeadingWhitespace");
    int ichT = ichToken;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    cchToken = ichT - ichToken;
    //log("leadingWhitespace cchScript=" + cchScript + " cchToken=" + cchToken);
    return cchToken > 0;
  }

  private boolean isShowCommand;
  
  private boolean lookingAtComment() {
    //log ("lookingAtComment ichToken=" + ichToken + " cchToken=" + cchToken);
    // first, find the end of the statement and scan for # (sharp) signs
    char ch = 'X';
    int ichEnd = ichToken;
    int ichFirstSharp = -1;

    /*
     * New in Jmol 11.1.9: we allow for output from the
     * set showScript command to be used as input. These lines
     * start with $ and have a [...] phrase after them. 
     * Their presence switches us to this new mode where we
     * use those statements as our commands and any line WITHOUT
     * those as comments. 
     */
    if (ichToken == ichCurrentCommand && ichToken < cchScript && script.charAt(ichToken) == '$') {
      isShowScriptOutput = true;
      while (ch != ']' && ichEnd < cchScript
          && !eol(ch = script.charAt(ichEnd)))
        ++ichEnd;
      cchToken = ichEnd - ichToken;
      isShowCommand = true;
      return true;
    } else if (isShowScriptOutput) {
      if (!isShowCommand)
        ichFirstSharp = ichToken;
      if (ichToken >= cchScript || eol(script.charAt(ichToken))) {
        isShowCommand = false;
        return false;
      }
    }

    for (; ichEnd < cchScript && !eol(ch = script.charAt(ichEnd)); ichEnd++)
      if (ch == '#' && ichFirstSharp == -1)
        ichFirstSharp = ichEnd;
    if (ichFirstSharp == -1) // there were no sharps found
      return false;

    /****************************************************************
     * check for #jc comment
     * if it occurs anywhere in the statement, then the statement is
     * not executed.
     * This allows statements which are executed in RasMol but are
     * comments in Jmol
     ****************************************************************/

    if (cchScript - ichFirstSharp >= 3
        && script.charAt(ichFirstSharp + 1) == 'j'
        && script.charAt(ichFirstSharp + 2) == 'c') {
      // statement contains a #jc before then end ... strip it all
      cchToken = ichEnd - ichToken;
      return true;
    }

    // if the sharp was not the first character then it isn't a comment
    if (ichFirstSharp != ichToken)
      return false;

    /****************************************************************
     * check for leading #jx <space> or <tab>
     * if you see it, then only strip those 4 characters
     * if they put in #jx <newline> then they are not going to
     * execute anything, and the regular code will take care of it
     ****************************************************************/
    if (cchScript > ichToken + 3 && script.charAt(ichToken + 1) == 'j'
        && script.charAt(ichToken + 2) == 'x'
        && isSpaceOrTab(script.charAt(ichToken + 3))) {
      cchToken = 4; // #jx[\s\t]
      return true;
    }

    // first character was a sharp, but was not #jx ... strip it all
    cchToken = ichEnd - ichToken;
    return true;
  }

  private boolean lookingAtEndOfLine() {
    //log("lookingAtEndOfLine");
    if (ichToken >= cchScript)
      return true;
    int ichT = ichToken;
    char ch = script.charAt(ichT);
    if (ch == '\r') {
      ++ichT;
      if (ichT < cchScript && script.charAt(ichT) == '\n')
        ++ichT;
    } else if (ch == '\n') {
      ++ichT;
    } else {
      return false;
    }
    cchToken = ichT - ichToken;
    return true;
  }

  private boolean lookingAtEndOfStatement() {
    if (ichToken == cchScript || script.charAt(ichToken) != ';')
      return false;
    cchToken = 1;
    return true;
  }

  private boolean lookingAtString() {
    if (ichToken == cchScript)
      return false;
    if (script.charAt(ichToken) != '"')
      return false;
    // remove support for single quote
    // in order to use it in atom expressions
    //    char chFirst = script.charAt(ichToken);
    //    if (chFirst != '"' && chFirst != '\'')
    //      return false;
    int ichT = ichToken;
    //    while (ichT < cchScript && script.charAt(ichT++) != chFirst)
    char ch;
    boolean previousCharBackslash = false;
    while (++ichT < cchScript) {
      ch = script.charAt(ichT);
      if (ch == '"' && !previousCharBackslash)
        break;
      previousCharBackslash = ch == '\\' ? !previousCharBackslash : false;
    }
    if (ichT == cchScript)
      cchToken = -1;
    else
      cchToken = ++ichT - ichToken;
    return true;
  }

  String getUnescapedStringLiteral() {
    if (cchToken < 2)
      return "";
    StringBuffer sb = new StringBuffer(cchToken - 2);
    int ichMax = ichToken + cchToken - 1;
    int ich = ichToken + 1;
    while (ich < ichMax) {
      char ch = script.charAt(ich++);
      if (ch == '\\' && ich < ichMax) {
        ch = script.charAt(ich++);
        switch (ch) {
        case 'b':
          ch = '\b';
          break;
        case 'n':
          ch = '\n';
          break;
        case 't':
          ch = '\t';
          break;
        case 'r':
          ch = '\r';
        // fall into
        case '"':
        case '\\':
        case '\'':
          break;
        case 'x':
        case 'u':
          int digitCount = ch == 'x' ? 2 : 4;
          if (ich < ichMax) {
            int unicode = 0;
            for (int k = digitCount; --k >= 0 && ich < ichMax;) {
              char chT = script.charAt(ich);
              int hexit = getHexitValue(chT);
              if (hexit < 0)
                break;
              unicode <<= 4;
              unicode += hexit;
              ++ich;
            }
            ch = (char) unicode;
          }
        }
      }
      sb.append(ch);
    }
    return "" + sb;
  }

  static int getHexitValue(char ch) {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return 10 + ch - 'a';
    else if (ch >= 'A' && ch <= 'F')
      return 10 + ch - 'A';
    else
      return -1;
  }

  // note that these formats include a space character
  String[] loadFormats = { "alchemy ", "mol2 ", "mopac ", "nmrpdb ", "charmm ",
      "xyz ", "mdl ", "pdb " };

  private boolean lookingAtLoadFormat() {
    for (int i = loadFormats.length; --i >= 0;) {
      String strFormat = loadFormats[i];
      int cchFormat = strFormat.length();
      if (script.regionMatches(true, ichToken, strFormat, 0, cchFormat)) {
        cchToken = cchFormat - 1; // subtract off the space character
        return true;
      }
    }
    return false;
  }

  private boolean lookingAtSpecialString() {
    int ichT = ichToken;
    while (ichT < cchScript && !eol(script.charAt(ichT)))
      ++ichT;
    cchToken = ichT - ichToken;
    log("lookingAtSpecialString cchToken=" + cchToken);
    return cchToken > 0;
  }

  private float lookingAtExponential() {
    if (ichToken == cchScript)
      return Float.NaN; //end
    int ichT = ichToken;
    boolean isNegative = (script.charAt(ichT) == '-');
    if (isNegative)
      ++ichT;
    int pt0 = ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    if (ichT < cchScript && ch == '.')
      ++ichT;
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    if (ichT == cchScript || !digitSeen)
      return Float.NaN; //integer
    int ptE = ichT;
    int factor = 1;
    int exp = 0;
    boolean isExponential = (ch == 'E' || ch == 'e');
    if (!isExponential || ++ichT == cchScript)
      return Float.NaN;
    ch = script.charAt(ichT);
    // I THOUGHT we only should allow "E+" or "E-" here, not "2E1" because
    // "2E1" might be a PDB het group by that name. BUT it turns out that
    // any HET group starting with a number is unacceptable and must
    // be given as [nXm], in brackets.

    if (ch == '-' || ch == '+') {
      ichT++;
      factor = (ch == '-' ? -1 : 1);
    }
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ichT++;
      exp = (exp * 10 + ch - '0');
    }
    if (exp == 0)
      return Float.NaN;
    cchToken = ichT - ichToken;
    double value = Float.valueOf(script.substring(pt0, ptE)).doubleValue();
    value *= (isNegative ? -1 : 1) * Math.pow(10, factor * exp);
    return (float) value;
  }

  private boolean lookingAtDecimal() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (script.charAt(ichT) == '-')
      ++ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT++)))
      digitSeen = true;
    if (ch != '.')
      return false;
    // only here if  "dddd."

    // to support 1.ca, let's check the character after the dot
    // to determine if it is an alpha
    char ch1;
    if (ichT < cchScript && !eol(ch1 = script.charAt(ichT))) {
      if (Character.isLetter(ch1) || ch1 == '?')
        return false;
      //well, guess what? we also have to look for 86.1Na, so...
      //watch out for moveto..... 56.;refresh...
      if (ichT + 1 < cchScript
          && (Character.isLetter(ch1 = script.charAt(ichT + 1)) || ch1 == '?'))
        return false;
    }
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    cchToken = ichT - ichToken;
    return digitSeen;
  }

  private boolean lookingAtSeqcode() {
    int ichT = ichToken;
    char ch = ' ';
    if (ichT + 1 < cchScript && script.charAt(ichT) == '*'
        && script.charAt(ichT + 1) == '^') {
      ch = '^';
      ++ichT;
    } else {
      while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT)))
        ++ichT;
    }
    if (ch != '^')
      return false;
    ichT++;
    if (ichT == cchScript)
      ch = ' ';
    else
      ch = script.charAt(ichT++);
    if (ch != ' ' && ch != '*' && ch != '?' && !Character.isLetter(ch))
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  private boolean lookingAtInteger(boolean allowNegative) {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (allowNegative && script.charAt(ichToken) == '-')
      ++ichT;
    int ichBeginDigits = ichT;
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT)))
      ++ichT;
    if (ichBeginDigits == ichT)
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  BitSet lookingAtBitset() {
    // ({n n:m n}) or ({null})
    // [{n:m}] is a BOND bitset
    // EXCEPT if the previous token was a function:
    // {carbon}.distance({3 3 3})
    // Yes, I wish I had used {{...}}, but this will work. 
    
    if (script.indexOf("({null})", ichToken) == ichToken) {
      cchToken = 8;
      return new BitSet();
    }
    if (ichToken + 4 > cchScript || script.charAt(ichToken + 1) != '{'
      ||(script.charAt(ichToken) != '(' && script.charAt(ichToken) != '['))
      return null;
    int ichT = ichToken + 2;
    char chEnd = (script.charAt(ichToken) == '(' ? ')' : ']');
    char ch = ' ';
    while (ichT < cchScript && (ch = script.charAt(ichT)) != '}'
        && (Character.isDigit(ch) || isSpaceOrTab(ch) || ch == ':'))
      ichT++;
    if (ch != '}' || ichT + 1 == cchScript
        || script.charAt(ichT + 1) != chEnd)
      return null;
    int iprev = -1;
    int ipt = 0;
    BitSet bs = new BitSet();
    for (int ich = ichToken+ 2; ich < ichT;ich = ipt) {
      while (isSpaceOrTab(ch = script.charAt(ich)))
        ich++;
      ipt = ich;
      while (Character.isDigit(ch = script.charAt(ipt)))
        ipt++;
      if (ipt == ich) // possibly :m instead of n:m
        return null;
      int val = Integer.parseInt(script.substring(ich, ipt));
      if (ch == ':') {
        iprev = val;
        ipt++;
      } else {
        if (iprev >= 0) {
          if (iprev > val)
            return null;
          for (int i = iprev; i <= val; i++)
            bs.set(i);
        } else {
          bs.set(val);
        }
        iprev = -1;
      }
    }
    if (iprev >= 0)
      return null;
    cchToken = ichT + 2 - ichToken;
    return bs;
  }
  
  private boolean lookingAtLookupToken() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    char ch;
    switch (ch = script.charAt(ichT++)) {
    case '(':
    case ')':
    case ',':
    case '*':
    case '-':
    case '{':
    case '}':
    case '$':
    case '+':
    case ':':
    case '@':
    case '.':
    case '%':
    case '[':
    case ']':
      break;
    case '&':
    case '|':
      if (ichT < cchScript && script.charAt(ichT) == ch)
        ++ichT;
      break;
    case '<':
    case '=':
    case '>':
      if (ichT < cchScript
          && ((ch = script.charAt(ichT)) == '<' || ch == '=' || ch == '>'))
        ++ichT;
      break;
    case '/':
    case '!':
      if (ichT < cchScript && script.charAt(ichT) == '=')
        ++ichT;
      break;
    default:
      if (!Character.isLetter(ch))
        return false;
    //fall through
    case '~':
    case '_':
    case '?': // include question marks in identifier for atom expressions
      while (ichT < cchScript
          && (Character.isLetterOrDigit(ch = script.charAt(ichT)) || ch == '_'
              || ch == '?' || ch == '~')
          ||
          // hack for insertion codes embedded in an atom expression :-(
          // select c3^a
          (ch == '^' && ichT > ichToken && Character.isDigit(script
              .charAt(ichT - 1))))
        ++ichT;
      break;
    }
    cchToken = ichT - ichToken;
    return true;
  }

  boolean isSetOrIf;

  private boolean compileCommand() {
    Token tokenCommand = (Token) ltoken.firstElement();
    int tokCommand = tokenCommand.tok;
    int size = ltoken.size();
    if (size == 1 && tokenCommand.intValue != Integer.MAX_VALUE && tokAttr(tokenCommand.intValue, Token.onDefault1))
      addTokenToPrefix(Token.tokenOn);
    atokenCommand = new Token[ltoken.size()];
    ltoken.copyInto(atokenCommand);
    if (logMessages) {
      for (int i = 0; i < atokenCommand.length; i++)
        Logger.debug(i + ": " + atokenCommand[i]);
    }
    

    //compile color parameters

    if (tokAttr(tokCommand, Token.colorparam) && !compileColorParam())
      return false;

    //compile expressions

    isSetOrIf =  (tokCommand == Token.set || tokCommand == Token.ifcmd);
    boolean checkExpression = (tokAttrOr(tokCommand,
        Token.expressionCommand, Token.embeddedExpression));
    if (!tokAttr(tokCommand, Token.coordOrSet)) {
      // $ or { at beginning disallow expression checking for center command
      int firstTok = (size == 1 ? Token.nada : atokenCommand[1].tok);
      if (tokCommand == Token.center && (firstTok == Token.leftbrace || firstTok == Token.dollarsign))
        checkExpression = false;
    }
    if (checkExpression && !compileExpression())
      return false;

    //check statement length

    size = atokenCommand.length;

    int allowedLen = (tokenCommand.intValue & 0x0F) + 1;
    if (!tokAttr(tokenCommand.intValue, Token.varArgCount)) {
      if (size > allowedLen)
        return badArgumentCount();
      if (size < allowedLen)
        return endOfCommandUnexpected();
    } else if (allowedLen > 1 && size > allowedLen) {
      // max2, max3, max4, etc.
      return badArgumentCount();
    }
    if (isNewSet && size < 3)
      return commandExpected();
    return true;
  }

  Vector ltokenPostfix = null;
  Token[] atokenInfix;
  int itokenInfix;
  boolean isCoordinate;
  
  private boolean compileExpression() {
    int tokCommand = atokenCommand[0].tok;
    boolean isMultipleOK = (tokAttr(tokCommand, Token.embeddedExpression));
    itokenInfix = 1;
    if (tokCommand == Token.define || tokCommand == Token.set)
      itokenInfix = 2;
    if (tokCommand == Token.set && itokenInfix >= atokenCommand.length)
      return true;
    ltokenPostfix = new Vector();
    atokenInfix = atokenCommand;
    for (int i = 0; i < itokenInfix; ++i)
      addTokenToPostfix(atokenCommand[i]);
    while (itokenInfix > 0 && itokenInfix < atokenCommand.length) {
      if (isMultipleOK)
        while (itokenInfix < atokenCommand.length
            && atokenCommand[itokenInfix].tok != (isSetOrIf ? Token.leftbrace
                : Token.leftparen))
          addTokenToPostfix(atokenCommand[itokenInfix++]);
      if (itokenInfix == atokenCommand.length)
        break;
      int pt = ltokenPostfix.size();
      addTokenToPostfix(Token.tokenExpressionBegin);
      isCoordinate = false;
      if (!clauseOr(!isSetOrIf))
        return false;
      if (isCoordinate && isSetOrIf) {
        ltokenPostfix.set(pt, Token.tokenCoordinateBegin);
        addTokenToPostfix(Token.tokenCoordinateEnd);
      } else {
        addTokenToPostfix(Token.tokenExpressionEnd);
      }
      if (itokenInfix != atokenInfix.length && !isMultipleOK)
        return endOfExpressionExpected();
    }
    atokenCommand = new Token[ltokenPostfix.size()];
    ltokenPostfix.copyInto(atokenCommand);
    return true;
  }

  private static boolean tokAttrOr(int a, int b1, int b2) {
    return (a & b1) == b1 || (a & b2) == b2;
  }
  
  private static boolean tokenAttr(Token token, int tok) {
    return token != null && (token.tok & tok) == tok;
  }
  
  private int tokPeek() {
    if (itokenInfix == atokenInfix.length)
      return Token.nada;
    return atokenInfix[itokenInfix].tok;
  }

  private boolean tokPeek(int tok) {
    if (itokenInfix == atokenInfix.length)
      return false;
    return (atokenInfix[itokenInfix].tok == tok);
  }

  private int intPeek() {
    if (itokenInfix == atokenInfix.length)
      return Integer.MAX_VALUE;
    return atokenInfix[itokenInfix].intValue;    
  }
  
  /**
   * increments the pointer; does NOT set theToken or theValue
   * @return the next token
   */
  private Token tokenNext() {
    if (itokenInfix == atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix++];
  }
  
  private boolean tokenNext(int tok) {
    Token token = tokenNext();
    return (token != null && token.tok == tok);
  }

  private void returnToken() {
    itokenInfix--;
  }

  Token theToken;
  Object theValue;
  
  /**
   * gets the next token and sets global theToken and theValue
   * @return the next token
   */
  private Token getToken() {
    theValue = ((theToken = tokenNext()) == null ? null : theToken.value);
    return theToken;
  }
  
  private boolean isToken(int tok) {
    return theToken != null && theToken.tok == tok;
  }
  
  private boolean getNumericalToken() {
    return (getToken() != null 
        && (isToken(Token.integer) || isToken(Token.decimal)));
  }
  
  private float floatValue() {
    switch (theToken.tok) {
    case Token.integer:
      return theToken.intValue;
    case Token.decimal:
      return ((Float) theValue).floatValue();
    }
    return 0;
  }

  private boolean addTokenToPostfix(Token token) {
    if (token == null)
      return false;
    if (logMessages)
      log("addTokenToPostfix" + token);
    ltokenPostfix.addElement(token);
    return true;
  }

  private boolean addNextToken() {
    return addTokenToPostfix(tokenNext());
  }
  
  private boolean addNextTokenIf(int tok) {
    return (tokPeek(tok) && addNextToken());
  }
  
  private boolean addNextTokenIf(int tok, Token token) {
    if (!tokPeek(tok))
      return false;
    itokenInfix++;
    return addTokenToPostfix(token);
  }
  
  boolean haveString;
  
  private boolean clauseOr(boolean allowComma) {
    haveString = false;
    if (!clauseAnd())
      return false;
    //for simplicity, giving XOR (toggle) same precedence as OR
    //OrNot: First OR, but if that makes no change, then NOT (special toggle)
    int tok;
    while ((tok = tokPeek())== Token.opOr || tok == Token.opXor
        || tok==Token.opToggle|| allowComma && tok == Token.comma) {
      if (tok == Token.comma && !haveString)
        addNextTokenIf(Token.comma, Token.tokenOr);
      else
        addNextToken();
      if (!clauseAnd())
        return false;
    }
    return true;
  }

  private boolean clauseAnd() {
    if (!clauseNot())
      return false;
    while (tokPeek(Token.opAnd)) {
      addNextToken();
      if (!clauseNot())
        return false;
    }
    return true;
  }

  // for RPN processor, not reversed
  private boolean clauseNot() {
    if (tokPeek(Token.opNot)) {
      addNextToken();
      return clauseNot();
    }
    return clausePrimitive();
  }

  private boolean clausePrimitive() {
    int tok = tokPeek();
    switch (tok) {
    default:
      if (tokAttr(tok, Token.atomproperty))
        return clauseComparator();
      if (!tokAttr(tok, Token.predefinedset))
        break;
    // fall into the code and below and just add the token
    case Token.all:
    case Token.none:
    case Token.bitset:
      return addNextToken();
    case Token.string:
      haveString = true;
      return addNextToken();
    case Token.nada:
      return endOfCommandUnexpected();
    case Token.define:
      addNextToken();
      return addNextToken();
    case Token.bonds:
    case Token.monitor:
      addNextToken();
      if (tokPeek(Token.bitset))
        addNextToken();
      else if (tokPeek(Token.define)) {
        addNextToken();
        addNextToken();
      }
      return true;
    case Token.cell:
      return clauseCell();
    case Token.within:
      addNextToken();
      return clauseWithin();
    case Token.connected:
      addNextToken();
      return clauseConnected();
    case Token.substructure:
      addNextToken();
      return clauseSubstructure();
    case Token.decimal:
      return addTokenToPostfix(new Token(Token.spec_model2,
          getToken().intValue, theValue));
    case Token.hyphen: // selecting a negative residue spec
    case Token.integer:
    case Token.seqcode:
    case Token.asterisk:
    case Token.leftsquare:
    case Token.identifier:
    case Token.colon:
    case Token.percent:
      if (clauseResidueSpec())
        return true;
    case Token.slash:
      if (clauseResidueSpec())
        return true;
    case Token.leftparen:
      addNextToken();
      if (!clauseOr(true))
        return false;
      if (!addNextTokenIf(Token.rightparen))
        return rightParenthesisExpected();
      return checkForMath();
    case Token.leftbrace:
      isCoordinate = false;
      if (isSetOrIf)
        tokenNext();
      else
        addNextToken();
      if (!clauseOr(false))
        return false;
      if (tokPeek() != Token.rightbrace) {
        addNextTokenIf(Token.comma);
        if (!clauseOr(false))
          return false;
        addNextTokenIf(Token.comma);
        if (!clauseOr(false))
          return false;
        addNextTokenIf(Token.comma);
        clauseOr(false);
        if (tokPeek() != Token.rightbrace)
          return rightBraceExpected();
        isCoordinate = true;
      }
      if (isSetOrIf)
        tokenNext();
      else
        addNextToken();
      return checkForMath();
    }
    return unrecognizedExpressionToken();
  }

  private boolean checkForMath() {
    for (int i = 0; i < 2; i++) {
      if (!addNextTokenIf(Token.leftsquare))
        break;
      if (!clauseMath())
        return false;
      if (!addNextTokenIf(Token.rightsquare))
        return rightBracketExpected();
    }
    return true;
  }
  
  // within ( plane, ....)
  // within ( distance, plane, planeExpression)
  // within ( hkl, ....
  // within ( distance, orClause)
  // within ( group, ....)

  private boolean clauseWithin() {
    if (!addNextTokenIf(Token.leftparen))
      return false;
    if (getToken() == null)
      return false;
    float distance = 0;
    String key = null;
    switch (theToken.tok) {
    case Token.hyphen:
      if (getToken() == null)
        return false;
      if (theToken.tok != Token.integer)
        return numberExpected();
      distance = -theToken.intValue;
      break;
    case Token.integer:
    case Token.decimal:
      distance = floatValue();
      break;
    case Token.group:
    case Token.chain:
    case Token.molecule:
    case Token.model:
    case Token.site:
    case Token.coord:
    case Token.element:
    case Token.string:
      key = (String) theValue;
      break;
    case Token.identifier:
      key = ((String) theValue).toLowerCase();
      break;
    default:
      return unrecognizedParameter("WITHIN", "" + theToken.value);
    }
    if (key == null)
      addTokenToPostfix(new Token(Token.decimal, new Float(distance)));
    else
      addTokenToPostfix(new Token(Token.string, key));

    while (true) {
      if (!addNextTokenIf(Token.comma))
        break;
      int tok = tokPeek();
      boolean isCoordOrPlane = false;
      if (key == null && tok == Token.identifier || tok == Token.coord) {
        //distance was specified, but to what?
        getToken();
        key = ((String) theValue).toLowerCase();
        if (";plane;hkl;coord;".indexOf(";" + key + ";") >= 0) {
          addTokenToPostfix(new Token(Token.string, key));
        } else {
          returnToken();
          if (tok == Token.leftbrace) {
            isCoordOrPlane = true;
            addTokenToPostfix(new Token(Token.string, "coord"));
          }
        }
      }
      if (";plane;hkl;coord;".indexOf(";" + key + ";") >= 0)
        isCoordOrPlane = true;
      addNextTokenIf(Token.comma);
      if (isCoordOrPlane) {
        while (!tokPeek(Token.rightparen)) {
          switch (tokPeek()) {
          case Token.nada:
            return endOfCommandUnexpected();
          case Token.leftparen:
            addTokenToPostfix(Token.tokenExpressionBegin);
            addNextToken();
            if (!clauseOr(false))
              return unrecognizedParameter("WITHIN", "?");
            if (!addNextTokenIf(Token.rightparen))
              return commaOrCloseExpected();
            addTokenToPostfix(Token.tokenExpressionEnd);
            break;
          default:
            addTokenToPostfix(getToken());
          }
        }
      } else if (!clauseOr(true)) {// *expression*
        return endOfCommandUnexpected();
      }
    }
    if (!addNextTokenIf(Token.rightparen))
      return rightParenthesisExpected();
    return true;
  }

  private boolean clauseConnected() {
    // connected (1,3, single, .....)
    if (!addNextTokenIf(Token.leftparen)) {
      addTokenToPostfix(new Token(Token.leftparen));
      addTokenToPostfix(new Token(Token.rightparen));
      return true;
    }
    while (true) {
      if (addNextTokenIf(Token.integer))
        if (!addNextTokenIf(Token.comma))
          break;
      if (addNextTokenIf(Token.integer))
        if (!addNextTokenIf(Token.comma))
          break;
      if (tokPeek() == Token.identifier) {
        String strOrder = (String) getToken().value;
        int intType = JmolConstants.getBondOrderFromString(strOrder);
        if (intType == JmolConstants.BOND_ORDER_NULL) {
          returnToken();
        } else {
          addTokenToPostfix(new Token(Token.string, strOrder));
          if (!addNextTokenIf(Token.comma))
            break;
        }
      }
      if (addNextTokenIf(Token.rightparen))
        return true;
      if (!clauseOr(true)) // *expression*
        return false;
      break;
    }
    if (!addNextTokenIf(Token.rightparen))
      return rightParenthesisExpected();
    return true;
  }

  private boolean clauseSubstructure() {
    if (!addNextTokenIf(Token.leftparen))
      return false;
    if (!addNextTokenIf(Token.string))
      return stringExpected();
    if (!addNextTokenIf(Token.rightparen))
      return rightParenthesisExpected();
    return true;
  }

  private boolean clauseMath() {
    int tok;
    while ((tok = tokPeek()) != Token.nada && tok != Token.rightsquare) {
      if (!clauseOr(false))
        return false;
      returnToken();
      if (tokPeek() != Token.asterisk)
        tokenNext();
      tok = tokPeek();
      //System.out.println ("clausemath" + Integer.toHexString(tok) + " " + Integer.toHexString(Token.mathop));
      if (tok == Token.rightsquare || !tokAttr(tok, Token.mathop))
        break;
      if (tok != Token.leftparen)
        addNextToken();
    }
    return true;
  }
  
  private boolean clauseComparator() {
    Token tokenAtomProperty = tokenNext();
    Token tokenComparator = tokenNext();
    if (!tokenAttr(tokenComparator, Token.comparator))
      return comparisonOperatorExpected();
    if (getToken() == null)
      return unrecognizedExpressionToken();
    boolean isNegative = (isToken(Token.hyphen));
    if (isNegative && getToken() == null)
      return numberExpected();
    switch (theToken.tok) {
    case Token.integer:
    case Token.decimal:
    case Token.identifier:
    case Token.string:
    case Token.leftbrace:
    case Token.define:
      break;
    default:
      return numberOrVariableNameExpected();
    }
    addTokenToPostfix(new Token(tokenComparator.tok, tokenAtomProperty.tok,
        tokenComparator.value + (isNegative ? " -" : "")));
    if (isToken(Token.leftbrace)) {
      returnToken();
      return clausePrimitive();
    }
    addTokenToPostfix(theToken);
    if (theToken.tok == Token.define)
      addTokenToPostfix(getToken());
    return true;
  }

  private boolean clauseCell() {
    Point3f cell = new Point3f();
    tokenNext(); // CELL
    if (!tokenNext(Token.opEQ)) // =
      return equalSignExpected();
    if (getToken() == null)
      return coordinateExpected();
    // 555 = {1 1 1}
    //Token coord = tokenNext(); // 555 == {1 1 1}
    if (isToken(Token.integer)) {
      int nnn = theToken.intValue;
      cell.x = nnn / 100 - 4;
      cell.y = (nnn % 100) / 10 - 4;
      cell.z = (nnn % 10) - 4;
      return addTokenToPostfix(new Token(Token.cell, cell));
    }
    if (!isToken(Token.leftbrace) || !getNumericalToken())
      return coordinateExpected(); // i
    cell.x = floatValue();
    if (tokPeek(Token.comma)) // ,
      tokenNext();
    if (!getNumericalToken())
      return coordinateExpected(); // j
    cell.y = floatValue();
    if (tokPeek(Token.comma)) // ,
      tokenNext();
    if (!getNumericalToken() || !tokenNext(Token.rightbrace))
      return coordinateExpected(); // k
    cell.z = floatValue();
    return addTokenToPostfix(new Token(Token.cell, cell));
  }

  private boolean residueSpecCodeGenerated;

  private boolean generateResidueSpecCode(Token token) {
    if (residueSpecCodeGenerated)
      addTokenToPostfix(Token.tokenAnd);
    addTokenToPostfix(token);
    residueSpecCodeGenerated = true;
    return true;
  }

  private boolean clauseResidueSpec() {
    boolean specSeen = false;
    residueSpecCodeGenerated = false;
    int tok = tokPeek();
    if (tok == Token.asterisk || tok == Token.leftsquare
        || tok == Token.identifier) {

      //note: there are many groups that could
      //in principle be escaped here, for example:
      //"AND" "SET" and others
      //rather than do this, just have people
      //use [AND] [SET], which is no problem.

      if (!clauseResNameSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    boolean wasInteger = false;
    if (tok == Token.asterisk || tok == Token.hyphen || tok == Token.integer
        || tok == Token.seqcode) {
      wasInteger = (tok == Token.integer);
      if (!clauseResNumSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.colon || tok == Token.asterisk || tok == Token.identifier
        || tok == Token.integer && !wasInteger) {
      if (!clauseChainSpec(tok))
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.dot) {
      if (!clauseAtomSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.percent) {
      if (!clauseAlternateSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.colon || tok == Token.slash) {
      if (!clauseModelSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (!specSeen)
      return residueSpecificationExpected();
    if (!residueSpecCodeGenerated) {
      // nobody generated any code, so everybody was a * (or equivalent)
      addTokenToPostfix(Token.tokenAll);
    }
    return true;
  }

  private boolean clauseResNameSpec() {
    getToken();
    if (isToken(Token.asterisk) || isToken(Token.nada))
      return (!isToken(Token.nada));
    if (isToken(Token.leftsquare)) {
      String strSpec = "";
      while (getToken() != null && !isToken(Token.rightsquare))
        strSpec += theValue;
      if (!isToken(Token.rightsquare))
        return false;
      if (strSpec == "")
        return true;
      int pt;
      if (strSpec.length() > 0 && (pt = strSpec.indexOf("*")) >= 0
          && pt != strSpec.length() - 1)
        return residueSpecificationExpected();
      strSpec = strSpec.toUpperCase();
      return generateResidueSpecCode(new Token(Token.spec_name_pattern, strSpec));
    }

    // no [ ]:

    if (!isToken(Token.identifier))
      return identifierOrResidueSpecificationExpected();

    //check for a * in the next token, which
    //would indicate this must be a name with wildcard

    if (tokPeek(Token.asterisk)) {
      String res = theValue + "*";
      getToken();
      return generateResidueSpecCode(new Token(Token.identifier, res));
    }
    return generateResidueSpecCode(theToken);
  }

  private boolean clauseResNumSpec() {
    log("clauseResNumSpec()");
    if (tokPeek(Token.asterisk))
      return (getToken() != null);
    return clauseSequenceRange();
  }

  private boolean clauseSequenceRange() {
    if (!clauseSequenceCode())
      return false;
    int tok = tokPeek();
    if (tok == Token.hyphen || tok == Token.integer && intPeek() < 0) {
      // must allow for a negative number here when this is an embedded expression
      // in a command that allows for negInts. 
      if (tok == Token.hyphen)
        tokenNext();
      else
        returnToken();
      int seqcodeA = seqcode;
      if (!clauseSequenceCode())
        seqcode = Integer.MAX_VALUE;
      return generateResidueSpecCode(new Token(Token.spec_seqcode_range,
          seqcodeA, new Integer(seqcode)));
    }
    return generateResidueSpecCode(new Token(Token.spec_seqcode, seqcode,
        new Integer(seqvalue)));
  }

  int seqcode;
  int seqvalue;
  
  private boolean clauseSequenceCode() {
    boolean negative = false;
    int tokPeek = tokPeek();
    if (tokPeek == Token.hyphen) {
      tokenNext();
      negative = true;
      tokPeek = tokPeek();
    }
    if (tokPeek == Token.seqcode)
      seqcode = tokenNext().intValue;
    else if (tokPeek == Token.integer) {
      seqvalue = tokenNext().intValue;
      seqcode = Group.getSeqcode(Math.abs(seqvalue), ' ');
    } else
      return false;
    if (negative)
      seqcode = -seqcode;
    return true;
  }

  private boolean clauseChainSpec(int tok) {
    if (tok == Token.colon) {
      tokenNext();
      tok = tokPeek();
      if (isSpecTerminator(tok))
        return generateResidueSpecCode(new Token(Token.spec_chain, '\0',
            "spec_chain"));
    }
    if (tok == Token.asterisk)
      return (getToken() != null);
    char chain;
    switch (tok) {
    case Token.integer:
      getToken();
      int val = theToken.intValue;
      if (val < 0 || val > 9)
        return invalidChainSpecification();
      chain = (char) ('0' + val);
      break;
    case Token.identifier:
      String strChain = (String) getToken().value;
      if (strChain.length() != 1)
        return invalidChainSpecification();
      chain = strChain.charAt(0);
      if (chain == '?')
        return true;
      break;
    default:
      return invalidChainSpecification();
    }
    return generateResidueSpecCode(new Token(Token.spec_chain, chain,
        "spec_chain"));
  }

  private boolean isSpecTerminator(int tok) {
    switch (tok) {
    case Token.nada:
    case Token.slash:
    case Token.opAnd:
    case Token.opOr:
    case Token.opNot:
    case Token.comma:
    case Token.percent:
    case Token.rightparen:
      return true;
    }
    return false;
  }

  private boolean clauseAlternateSpec() {
    tokenNext();
    int tok = tokPeek();
    if (isSpecTerminator(tok))
      return generateResidueSpecCode(new Token(Token.spec_alternate, null));
    String alternate = (String) getToken().value;
    switch (theToken.tok) {
    case Token.asterisk:
    case Token.string:
    case Token.integer:
    case Token.identifier:
      break;
    default:
      return invalidModelSpecification();
    }
    //Logger.debug("alternate specification seen:" + alternate);
    return generateResidueSpecCode(new Token(Token.spec_alternate, alternate));
  }

  private boolean clauseModelSpec() {
    getToken();
    if (isToken(Token.colon) || isToken(Token.slash))
      getToken();
    if (isToken(Token.asterisk))
      return true;
    if (isToken(Token.nada) || theToken == null)
      return invalidModelSpecification();
    switch (theToken.tok) {
    case Token.decimal:
      return generateResidueSpecCode(new Token(Token.spec_model,
          theToken.intValue, null));
    case Token.integer:
      break;
    default:
      return invalidModelSpecification();
    }
    //integer implies could be model number
    return generateResidueSpecCode(new Token(Token.spec_model, new Integer(
        theToken.intValue)));
  }

  private boolean clauseAtomSpec() {
    if (!tokenNext(Token.dot))
      return invalidAtomSpecification();
    if (getToken() == null)
      return true;
    String atomSpec = "";
    if (isToken(Token.integer)) {
      atomSpec += "" + theToken.intValue;
      if (getToken() == null)
        return invalidAtomSpecification();
    }
    switch (theToken.tok) {
    case Token.asterisk:
      return true;
    case Token.identifier:
      break;
    default:
      return invalidAtomSpecification();
    }
    atomSpec += theValue;
    if (tokPeek(Token.asterisk)) {
      tokenNext();
      // this one is a '*' as a prime, not a wildcard
      atomSpec += "*";
    }
    return generateResidueSpecCode(new Token(Token.spec_atom, atomSpec));
  }

  private boolean compileColorParam() {
    for (int i = 1; i < atokenCommand.length; ++i) {
      theToken = atokenCommand[i];
      //Logger.debug(token + " atokenCommand: " + atokenCommand.length);
      if (isToken(Token.dollarsign)) {
        i++; // skip identifier
      } else if (isToken(Token.identifier)) {
        String id = (String) theToken.value;
        int argb = Graphics3D.getArgbFromString(id);
        if (argb != 0) {
          theToken.tok = Token.colorRGB;
          theToken.intValue = argb;
        }
      }
    }
    return true;
  }
  
  /// error handling
  
  private boolean commandExpected() {
    return compileError(GT._("command expected"));
  }

  private boolean invalidExpressionToken(String ident) {
    return compileError(GT._("invalid expression token: {0}", ident));
  }

  private boolean unrecognizedToken(String ident) {
    return compileError(GT._("unrecognized token: {0}", ident));
  }

  private boolean endOfCommandUnexpected() {
    return compileError(GT._("unexpected end of script command"));
  }

  private boolean badArgumentCount() {
    return compileError(GT._("bad argument count"));
  }

  private boolean endOfExpressionExpected() {
    return compileError(GT._("end of expression expected"));
  }

  private boolean rightParenthesisExpected() {
    return compileError(GT._("right parenthesis expected"));
  }

  private boolean rightBraceExpected() {
    return compileError(GT._("right brace expected"));
  }

  private boolean rightBracketExpected() {
    return compileError(GT._("right bracket expected"));
  }

  private boolean coordinateExpected() {
    return compileError(GT._("{ number number number } expected"));
  }

  private boolean unrecognizedExpressionToken() {
    String value = (itokenInfix == atokenInfix.length ? ""
        : atokenInfix[itokenInfix].value.toString());
    return compileError(GT._("unrecognized expression token: {0}", "" + value));
  }

  private boolean comparisonOperatorExpected() {
    return compileError(GT._("comparison operator expected"));
  }

  private boolean equalSignExpected() {
    return compileError(GT._("equal sign expected"));
  }

  private boolean numberExpected() {
    return compileError(GT._("number expected"));
  }

  private boolean numberOrVariableNameExpected() {
    return compileError(GT._("number or variable name expected"));
  }

  private boolean unrecognizedParameter(String kind, String param) {
    return compileError(GT._("unrecognized {0} parameter", kind) + ": " + param);
  }
  
  private boolean identifierOrResidueSpecificationExpected() {
    return compileError(GT._("identifier or residue specification expected"));
  }

  private boolean residueSpecificationExpected() {
    return compileError(GT._("residue specification (ALA, AL?, A*) expected"));
  }

  private boolean invalidChainSpecification() {
    return compileError(GT._("invalid chain specification"));
  }

  private boolean invalidModelSpecification() {
    return compileError(GT._("invalid model specification"));
  }

  private boolean invalidAtomSpecification() {
    return compileError(GT._("invalid atom specification"));
  }

  private boolean compileError(String errorMessage) {
    this.errorMessage = errorMessage;
    return false;
  }

  private boolean stringExpected() {
    return compileError(GT._("quoted string expected"));
  }

  private boolean commaOrCloseExpected() {
    return compileError(GT._("comma or right parenthesis expected"));
  }

  String getErrorMessage() {
    return errorMessage;
  }
  
  private boolean handleError() {
    int icharEnd;
    if ((icharEnd = script.indexOf('\r', ichCurrentCommand)) == -1
        && (icharEnd = script.indexOf('\n', ichCurrentCommand)) == -1)
      icharEnd = script.length();
    errorLine = script.substring(ichCurrentCommand, icharEnd);
    String lineInfo = (ichToken < errorLine.length() ? errorLine.substring(0,
        ichToken)
        + " >>>> " + errorLine.substring(ichToken) : errorLine)
        + " <<<<";
    errorMessage = "script compiler ERROR: " + errorMessage
         + Eval.setErrorLineMessage(filename, lineCurrent, iCommand, lineInfo);
    if (!isSilent) {
      viewer.addCommand(errorLine + CommandHistory.ERROR_FLAG);
      Logger.error(errorMessage);
    }
    return false;
  }
  
  /*
  mth -- I think I am going to be sick
  the grammer is not context-free
  what does the string cys120 mean?
  if you have previously defined a variable, as in
  define cys120 carbon
  then when you use cys120 it refers to the previous definition.
  however, if cys120 was *not* previously defined, then it refers to
  the residue of type cys at number 120.
  what a disaster.
  
  rmh -- Note that these syntax rules have not been recently updated.
  Newer features such as if/else/endif, aa = xxx variation of set aa xxx,
  using file.model instead of * /model, being able to express bitsets within
  expressions, new CONNECTED syntax, among other things are not represented here.
  
  rmh 2/21/07 -- Up until this time, the compiler output was in Reverse Polish
  Notation (RPN). I've added an RPN processor to Eval now, so this is no longer
  necessary. The RPN processor allows processing of math separately from 
  expressions, but it can (and now does) also process natural-language expressions.
  
  The main advantage to this is that the order of tokens out is the same as the 
  order of all tokens in. This allows for a much more user-friendly syntax for
  debugging, primarily. In addition, if we wanted, we could now include
  math in expressions. For example, comparator values would not need to be 
  constants.  
  
  

  expression       :: = clauseOr

  clauseOr         ::= clauseAnd {OR|XOR|OrNot clauseAnd}*

  clauseAnd        ::= clauseNot {AND clauseNot}*

  clauseNot        ::= NOT clauseNot | clausePrimitive

  clausePrimitive  ::= clauseComparator |
  clauseCell |       // RMH 6/06
  WITHIN clauseFunction |
  CONNECTED clauseFunction |  // RMH 3/06
  SUBSTRUCTURE clauseFunction |  // RMH 3/06
  clauseResidueSpec |
  none | all |
  ( clauseOr )

  clauseComparator ::= atomproperty comparatorop integer

  clauseDistance   ::= integer | decimal

  clauseResidueSpec::= { clauseResNameSpec }
  { clauseResNumSpec }
  { clauseChainSpec }
  { clauseAtomSpec }
  { clauseAlternateSpec }
  { clauseModelSpec }

  clauseResNameSpec::= * | [ resNamePattern ] | resNamePattern

  // question marks are part of identifiers
  // they get split up and dealt with as wildcards at runtime
  // and the integers which are residue number chains get bundled
  // in with the identifier and also split out at runtime
  // iff a variable of that name does not exist

  resNamePattern   ::= up to 3 alphanumeric chars with * and ?

  clauseResNumSpec ::= * | clauseSequenceRange

  clauseSequenceRange ::= clauseSequenceCode { - clauseSequenceCode }

  clauseSequenceCode ::= seqcode | {-} integer

  clauseChainSpec  ::= {:} * | identifier | integer

  clauseAtomSpec   ::= . * | . identifier {*}
  // note that in atom spec context a * is *not* a wildcard
  // rather, it denotes a 'prime'

  clauseAlternateSpec ::= {%} identifier | integer

  clauseModelSpec  ::= {:|/} * | integer | decimal

  */


}
