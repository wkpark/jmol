/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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

package org.openscience.jmol.script;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

class Compiler {

  Eval eval;
  Token[] atokenCommand;

  String script;
  int cchScript;
  short lineCurrent;

  int ichToken;
  int cchToken;
  String strToken;

  int ichCurrentCommand;

  short[] linenumbers;
  short[] lineIndices;

  boolean error;
  String errorMessage;
  
  final boolean logMessages = false;

  Compiler(Eval eval) {
    this.eval = eval;
  }

  /*
    mth - 2003 01 05
    initial implementation used java.util.regex.*
    second round used hand-rolled tokenizing to support old browser jvms

    the grammar of rasmol scripts is a little messed-up, so this structure
    was the easiest thing for me to come up with that worked

  final static Pattern patternLeadingWhiteSpace =
    Pattern.compile("[\\s&&[^\\r\\n]]+");
  final static Pattern patternComment =
    Pattern.compile("#[^\\r\\n]*");
  final static Pattern patternEndOfStatement =
    Pattern.compile(";");
  final static Pattern patternEndOfLine =
    Pattern.compile("\\r?\\n|\\r|$", Pattern.MULTILINE);
  final static Pattern patternDecimal =
    Pattern.compile("-?\\d+\\.(\\d*)?|-?\\.\\d+");
  final static Pattern patternPositiveInteger =
    Pattern.compile("\\d+");
  final static Pattern patternNegativeInteger =
    Pattern.compile("-\\d+");
  final static Pattern patternString =
    Pattern.compile("([\"'`])(.*?)\\1");
  final static Pattern patternSpecialString =
    Pattern.compile("[^\\r\\n]+");
  final static Pattern patternLookup =
    Pattern.compile("\\(|\\)|," +
                    "|<=|<|>=|>|==|=|!=|<>|/=" +
                    "|&|\\||!" +
                    "|\\*" +                      // select *
                    "|-" +                        // range
                    "|\\[|\\]" +                  // color [##,##,##]
                    "|\\+" +                      // bond
                    "|\\?" +                      // help command
                    "|[a-zA-Z_][a-zA-Z_0-9]*"
                    );

  boolean lookingAt(Pattern pattern, String description) {
    Matcher m = pattern.matcher(script.subSequence(ichToken, cchScript));
    boolean lookingAt = m.lookingAt();
    if (lookingAt) {
      strToken = m.group();
      cchToken = m.end();
    } else {
      cchToken = 0;
    }
    return lookingAt;
  }
  */

  boolean lookingAtLeadingWhitespace() {
    if (logMessages)
      System.out.println("lookingAtLeadingWhitespace");
    int ichT = ichToken;
    char ch;
    while (ichT < cchScript &&
           ((ch = script.charAt(ichT)) == ' ' || ch == '\t'))
      ++ichT;
    cchToken = ichT - ichToken;
    if (logMessages)
      System.out.println("leadingWhitespace cchScript=" + cchScript +
                         " cchToken=" + cchToken);
    return cchToken > 0;
  }

  boolean lookingAtComment() {
    if (logMessages)
      System.out.println("lookingAtComment ichToken=" + ichToken +
                         " cchToken=" + cchToken);
    if (ichToken == cchScript || script.charAt(ichToken) != '#')
      return false;
    int ichT = ichToken + 1;
    char ch;
    while (ichT < cchScript && (ch = script.charAt(ichT)) != '\r' && ch != 'n')
      ++ichT;
    cchToken = ichT - ichToken;
    return true;
  }

  boolean lookingAtEndOfLine() {
    if (logMessages)
      System.out.println("lookingAtEndOfLine");
    if (ichToken == cchScript)
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

  boolean lookingAtEndOfStatement() {
    if (ichToken == cchScript || script.charAt(ichToken) != ';')
      return false;
    cchToken = 1;
    return true;
  }

  boolean lookingAtString() {
    if (ichToken == cchScript)
      return false;
    char chFirst = script.charAt(ichToken);
    if (chFirst != '"' && chFirst != '\'')
      return false;
    int ichT = ichToken + 1;
    while (ichT < cchScript && script.charAt(ichT++) != chFirst)
      ;
    cchToken = ichT - ichToken;
    return true;
  }

  boolean lookingAtSpecialString() {
    int ichT = ichToken;
    char ch;
    while (ichT < cchScript &&
           (ch = script.charAt(ichT)) != '\r' && ch != '\n')
      ++ichT;
    cchToken = ichT - ichToken;
    if (logMessages)
      System.out.println("lookingAtSpecialString cchToken=" + cchToken);
    return cchToken > 0;
  }

  // FIXME -- confirm that we don't need to support negative decimals
  boolean lookingAtPositiveDecimal() {
    int ichT = ichToken;
    char ch = 'X';
    while (ichT < cchScript && (ch = script.charAt(ichT)) >= '0' && ch <= '9')
      ++ichT;
    if (ichT == cchScript || ch != '.')
      return false;
    ++ichT;
    while (ichT < cchScript && (ch = script.charAt(ichT)) >= '0' && ch <= '9')
      ++ichT;
    cchToken = ichT - ichToken;
    return cchToken > 1; // decimal point plust at least one digit
  }

  boolean lookingAtPositiveInteger() {
    int ichT = ichToken;
    char ch;
    while (ichT < cchScript && (ch = script.charAt(ichT)) >= '0' && ch <= '9')
      ++ichT;
    cchToken = ichT - ichToken;
    return cchToken > 0;
  }

  boolean lookingAtNegativeInteger() {
    if (ichToken == cchScript)
      return false;
    if (script.charAt(ichToken) != '-')
      return false;
    int ichT = ichToken + 1;
    char ch;
    while (ichT < cchScript && (ch = script.charAt(ichT)) >= '0' && ch <= '9')
      ++ichT;
    cchToken = ichT - ichToken;
    return cchToken > 1; // minus sign plus at least 1 digit
  }

  boolean lookingAtLookupToken() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    char ch;
    switch (ch = script.charAt(ichT++)) {
    case '(':
    case ')':
    case ',':
    case '&':
    case '|':
    case '*':
    case '-':
    case '[':
    case ']':
    case '+':
    case '?':
    case ':':
    case '@':
      break;
    case '<':
    case '=':
    case '>':
      if (ichT < cchScript &&
          ((ch = script.charAt(ichT)) == '<' || ch == '=' || ch == '>'))
        ++ichT;
      break;
    case '/':
    case '!':
      if (ichT < cchScript && script.charAt(ichT) == '=')
        ++ichT;
      break;
    default:
      if ((ch < 'a' || ch > 'z') && (ch < 'A' && ch > 'Z') && ch != '_')
        return false;
      while (ichT < cchScript &&
             (((ch = script.charAt(ichT)) >= 'a' && ch <= 'z') ||
              (ch >= 'A' && ch <= 'Z') ||
              (ch >= '0' && ch <= '9') || ch == '_'))
        ++ichT;
      break;
    }
    cchToken = ichT - ichToken;
    return true;
  }

  public void compile() throws ScriptException {
    if (compile1())
      return;

    int icharEnd;
    if ((icharEnd = script.indexOf('\r', ichCurrentCommand)) == -1 &&
        (icharEnd = script.indexOf('\n', ichCurrentCommand)) == -1)
      icharEnd = script.length();
    String strLine = script.substring(ichCurrentCommand, icharEnd);
    throw new ScriptException(errorMessage, strLine,
                              eval.filename, lineCurrent);
  }
  
  boolean compile1() {
    script = eval.script;
    cchScript = script.length();
    ichToken = 0;
    lineCurrent = 1;
    int lnLength = 8;
    linenumbers = new short[lnLength];
    lineIndices = new short[lnLength];
    error = false;

    Vector lltoken = new Vector();
    Vector ltoken = new Vector();
    Token tokenCommand = null;
    int tokCommand = Token.nada;

    for ( ; true; ichToken += cchToken) {
      if (lookingAtLeadingWhitespace())
        continue;
      if (lookingAtComment())
        continue;
      boolean endOfLine = lookingAtEndOfLine();
      if (endOfLine || lookingAtEndOfStatement()) {
        if (tokCommand != Token.nada) {
          if (! compileCommand(ltoken))
            return false;
          lltoken.addElement(atokenCommand);
          int iCommand = lltoken.size();
          if (iCommand == lnLength) {
            short[] lnT = new short[lnLength * 2];
            System.arraycopy(linenumbers, 0, lnT, 0, lnLength);
            linenumbers = lnT;
            lnT = new short[lnLength * 2];
            System.arraycopy(lineIndices, 0, lnT, 0, lnLength);
            lineIndices = lnT;
            lnLength *= 2;
          }
          linenumbers[iCommand] = lineCurrent;
          lineIndices[iCommand] = (short) ichCurrentCommand;
          ltoken.setSize(0);
          tokCommand = Token.nada;
        }
        if (ichToken < cchScript) {
          if (endOfLine)
            ++lineCurrent;
          continue;
        }
        break;
      }
      if (tokCommand != Token.nada) {
        if (lookingAtString()) {
          String str = script.substring(ichToken+1, ichToken+cchToken-1);
          ltoken.addElement(new Token(Token.string, str));
          continue;
        }
        if ((tokCommand & Token.specialstring) != 0 &&
            lookingAtSpecialString()) {
          String str = script.substring(ichToken, ichToken + cchToken);
          ltoken.addElement(new Token(Token.string, str));
          continue;
        }
        if (lookingAtPositiveDecimal()) {
          double value =
            Float.parseFloat(script.substring(ichToken, ichToken + cchToken));
          ltoken.addElement(new Token(Token.decimal, new Double(value)));
          continue;
        }
        if (lookingAtPositiveInteger() || 
            ((tokCommand & Token.negativeints) != 0 &&
             lookingAtNegativeInteger())) {
          int val = Integer.parseInt(script.substring(ichToken,
                                                      ichToken + cchToken));
          ltoken.addElement(new Token(Token.integer, val, null));
          continue;
        }
      }
      if (lookingAtLookupToken()) {
        String ident = script.substring(ichToken, ichToken + cchToken);
        ident = ident.toLowerCase();
        Token token = (Token) Token.map.get(ident);
        if (token == null)
          token = new Token(Token.identifier, ident);
        switch (tokCommand) {
        case Token.nada:
          ichCurrentCommand = ichToken;
          tokenCommand = token;
          tokCommand = token.tok;
          if ((tokCommand & Token.command) == 0)
            return commandExpected();
          break;
        case Token.set:
          if (ltoken.size() == 1) {
            if ((token.tok & Token.setspecial) != 0) {
              tokenCommand = token;
              tokCommand = token.tok;
              ltoken.removeAllElements();
              break;
            }
            if ((token.tok & Token.setparam) == 0)
              return cannotSet(ident);
          }
          break;
        case Token.show:
          if ((token.tok & Token.showparam) == 0)
            return cannotShow(ident);
          break;
        case Token.define:
          if (ltoken.size() == 1) {
            // we are looking at the variable name
            if (token.tok != Token.identifier &&
                (token.tok & Token.predefinedset) != Token.predefinedset)
              return invalidExpressionToken(ident);
          } else {
            // we are looking at the expression
            if (token.tok != Token.identifier &&
                (token.tok & Token.expression) == 0)
              return invalidExpressionToken(ident);
          }
          break;
        case Token.center:
        case Token.restrict:
        case Token.select:
          if ((token.tok != Token.identifier) &&
              (token.tok & Token.expression) == 0)
            return invalidExpressionToken(ident);
          break;
        }
        ltoken.addElement(token);
        continue;
      }
      if (ltoken.size() == 0)
        return commandExpected();
      return unrecognizedToken();
    }
    eval.aatoken = new Token[lltoken.size()][];
    lltoken.copyInto(eval.aatoken);
    eval.linenumbers = linenumbers;
    eval.lineIndices = lineIndices;
    return true;
  }

  private boolean commandExpected() {
    return compileError("command expected");
  }
  private boolean cannotSet(String ident) {
    return compileError("cannot SET:" + ident);
  }
  private boolean cannotShow(String ident) {
    return compileError("cannot SHOW:" + ident);
  }
  private boolean invalidExpressionToken(String ident) {
    return compileError("invalid expression token:" + ident);
  }
  private boolean unrecognizedToken() {
    return compileError("unrecognized token");
  }
  private boolean badArgumentCount() {
    return compileError("bad argument count");
  }
  private boolean endOfExpressionExpected() {
    return compileError("end of expression expected");
  }
  private boolean leftParenthesisExpected() {
    return compileError("left parenthesis expected");
  }
  private boolean rightParenthesisExpected() {
    return compileError("right parenthesis expected");
  }
  private boolean commaExpected() {
    return compileError("comma expected");
  }
  private boolean unrecognizedExpressionToken() {
    return compileError("unrecognized expression token");
  }
  private boolean integerExpectedAfterHyphen() {
    return compileError("integer expected after hyphen");
  }
  private boolean comparisonOperatorExpected() {
    return compileError("comparison operator expected");
  }
  private boolean integerExpected() {
    return compileError("integer expected");
  }
  private boolean numberExpected() {
    return compileError("number expected");
  }
  private boolean badRGBColor() {
    return compileError("bad [R,G,B] color");
  }
  private boolean residueIdentifierExpected() {
    return compileError("residue identifier expected");
  }
  private boolean resnoSpecificationExpected() {
    return compileError("residue number specification expected");
  }
  private boolean invalidChainSpecification() {
    return compileError("invalid chain specification");
  }
  private boolean invalidAtomSpecification() {
    return compileError("invalid atom specification");
  }

  private boolean compileError(String errorMessage) {
    System.out.println("compileError(" + errorMessage + ")");
    error = true;
    this.errorMessage = errorMessage;
    return false;
  }

  private boolean compileCommand(Vector ltoken) {
    Token tokenCommand = (Token)ltoken.firstElement();
    int tokCommand = tokenCommand.tok;
    if ((tokenCommand.intValue & Token.onDefault1) != 0 && ltoken.size() == 1)
      ltoken.addElement(Token.tokenOn);
    if (tokCommand == Token.set) {
      int size = ltoken.size();
      if (size < 2)
        return badArgumentCount();
      if (size == 2 &&
          (((Token)ltoken.elementAt(1)).tok & Token.setDefaultOn) != 0)
        ltoken.addElement(Token.tokenOn);
    }
    atokenCommand = new Token[ltoken.size()];
    ltoken.copyInto(atokenCommand);
    if ((tokCommand & Token.expression) != 0 && !compileExpression())
      return false;
    if ((tokCommand & Token.colorparam) != 0 && !compileColorParam())
      return false;
    if ((tokenCommand.intValue & Token.varArgCount) == 0 &&
        (tokenCommand.intValue & 7) + 1 != atokenCommand.length)
      return badArgumentCount();
    return true;
  }

  /*
    expression       :: = clauseOr

    clauseOr         ::= clauseAnd {OR clauseAnd}*

    clauseAnd        ::= clauseNot {AND clauseNot}*

    clauseNot        ::= NOT clauseNot | clausePrimitive

    clausePrimitive  ::= clauseInteger |
                         clauseComparator |
                         clauseWithin |
                         clauseWildcard |
                         none |
                         ( clauseOr )

    clauseInteger    ::= integer | integer - integer

    clauseComparator ::= atomproperty comparatorop integer

    clauseWithin     ::= WITHIN ( clauseDistance , expression )

    clauseDistance   ::= integer | decimal

    clauseWildcard   ::= clauseResSpec {clauseAtomSpec}

    clauseResSpec    ::= clauseResName {clauseResNo} {clauseChain}

    clauseResName    ::= [ clauseResName0 ] | clauseResName0

    clauseResName0   ::= * | {?}* identifier {?}*

    clauseResNo      ::= * | {-} integer

    clauseChain      ::= * | {:} integer | identifier

    clauseAtomSpec   ::= {identifier | ?}+
  */

  private boolean compileExpression() {
    int i = 1;
    if (atokenCommand[0].tok == Token.define)
      i = 2;
    if (i >= atokenCommand.length)
      return true;
    return compileExpression(i);
  }

  Vector ltokenPostfix = null;
  Token[] atokenInfix;
  int itokenInfix;
                  
  public boolean compileExpression(int itoken) {
    ltokenPostfix = new Vector();
    for (int i = 0; i < itoken; ++i)
      ltokenPostfix.addElement(atokenCommand[i]);
    atokenInfix = atokenCommand;
    itokenInfix = itoken;
    if (! clauseOr())
      return false;
    if (itokenInfix != atokenInfix.length)
      return endOfExpressionExpected();
    atokenCommand = new Token[ltokenPostfix.size()];
    ltokenPostfix.copyInto(atokenCommand);
    return true;
  }

  Token tokenNext() {
    if (itokenInfix == atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix++];
  }

  int tokPeek() {
    if (itokenInfix == atokenInfix.length)
      return 0;
    return atokenInfix[itokenInfix].tok;
  }

  boolean clauseOr() {
    if (! clauseAnd())
      return false;
    while (tokPeek() == Token.opOr) {
      Token tokenOr = tokenNext();
      if (! clauseAnd())
        return false;
      ltokenPostfix.addElement(tokenOr);
    }
    return true;
  }

  boolean clauseAnd() {
    if (! clauseNot())
      return false;
    while (tokPeek() == Token.opAnd) {
      Token tokenAnd = tokenNext();
      if (! clauseNot())
        return false;
      ltokenPostfix.addElement(tokenAnd);
    }
    return true;
  }

  boolean clauseNot() {
    if (tokPeek() == Token.opNot) {
      Token tokenNot = tokenNext();
      if (! clauseNot())
        return false;
      ltokenPostfix.addElement(tokenNot);
      return true;
    }
    return clausePrimitive();
  }

  boolean clausePrimitive() {
    switch (tokPeek()) {
    case Token.integer:
      return clauseInteger();
    case Token.atomno:
    case Token.elemno:
    case Token.resno:
    case Token.radius:
    case Token.temperature:
    case Token._bondedcount:
    case Token._resid:
      return clauseComparator();
    case Token.within:
      return clauseWithin();
    default:
      int tok = tokPeek();
      if ((tok & Token.predefinedset) != Token.predefinedset)
        break;
    case Token.all:
    case Token.none:
      ltokenPostfix.addElement(tokenNext());
      return true;
    case Token.asterisk:
    case Token.questionmark:
    case Token.leftsquare:
    case Token.identifier:
      return clauseWildcard();
    case Token.leftparen:
      tokenNext();
      if (! clauseOr())
          return false;
      if (tokenNext().tok != Token.rightparen)
        return rightParenthesisExpected();
      return true;
    }
    return unrecognizedExpressionToken();
  }

  boolean clauseInteger() {
    Token tokenInt1 = tokenNext();
    if (tokPeek() != Token.hyphen) {
      ltokenPostfix.addElement(tokenInt1);
      return true;
    }
    tokenNext();
    Token tokenInt2 = tokenNext();
    if (tokenInt2.tok != Token.integer)
      return integerExpectedAfterHyphen();
    int min = tokenInt1.intValue;
    int max = tokenInt2.intValue;
    if (max < min) {
      int intT = max; max = min; min = intT;
    }
    ltokenPostfix.addElement(new Token(Token.hyphen, min, new Integer(max)));
    return true;
  }

  boolean clauseComparator() {
    Token tokenAtomProperty = tokenNext();
    Token tokenComparator = tokenNext();
    if ((tokenComparator.tok & Token.comparator) == 0)
      return comparisonOperatorExpected();
    Token tokenValue = tokenNext();
    if (tokenValue.tok != Token.integer)
      return integerExpected();
    int val = tokenValue.intValue;
    // note that a comparator instruction is a complicated instruction
    // int intValue is the tok of the property you are comparing
    // the value against which you are comparing is stored as an Integer
    // in the object value
    ltokenPostfix.addElement(new Token(tokenComparator.tok,
                                       tokenAtomProperty.tok,
                                       new Integer(val)));
    return true;
  }

  boolean clauseWithin() {
    tokenNext();                             // WITHIN
    if (tokenNext().tok != Token.leftparen)  // (
      return leftParenthesisExpected();
    Double distance;
    Token tokenDistance = tokenNext();       // distance
    if (tokenDistance.tok == Token.integer)
      distance = new Double((tokenDistance.intValue * 4) / 1000.0);
    else if (tokenDistance.tok == Token.decimal)
      distance = (Double)tokenDistance.value;
    else
      return numberExpected();
    if (tokenNext().tok != Token.opOr)       // ,
      return commaExpected();
    if (! clauseOr())                        // *expression*
      return false;
    if (tokenNext().tok != Token.rightparen) // )
      return rightParenthesisExpected();
    ltokenPostfix.addElement(new Token(Token.within, distance));
    return true;
  }

  Token tokenResIdent;
  String strNameSpec;
  int resnoSpec;
  char chainSpec;
  String strAtomSpec;

  boolean clauseWildcard() {
    strNameSpec = "";
    resnoSpec = Integer.MAX_VALUE;
    chainSpec = '?';
    strAtomSpec = null;
    tokenResIdent = null;
    if (! clauseResSpec())
      return false;
    if (tokPeek() == Token.dot && !clauseAtomSpec())
      return false;
    if (resnoSpec == Integer.MAX_VALUE &&
        chainSpec=='?' && strAtomSpec == null) {
      Token token;
      if (strNameSpec.equals("*")) {
        System.out.println("* -> all");
        token = Token.tokenAll;
      } else if (tokenResIdent != null &&
                 strNameSpec.equals((String)tokenResIdent.value)) {
        System.out.println("residue name :" + strNameSpec);
        token = tokenResIdent;
      } else {
        System.out.println("residue name wildcard:" + strNameSpec);
        token = new Token(Token.residue_wildcard, strNameSpec);
      }
      ltokenPostfix.addElement(token);
      return true;
    }
    System.out.println("complex atom specification not implemented ..." +
                       " emitting ALL");
    ltokenPostfix.addElement(Token.tokenAll);
    return true;
  }

  boolean clauseResSpec() {
    if (! clauseResName())
      return false;
    int tokPeek = tokPeek();
    if (tokPeek == Token.asterisk ||
        tokPeek == Token.integer ||
        tokPeek == Token.hyphen) {
      if (! clauseResNo())
        return false;
      tokPeek = tokPeek();
    }
    if (tokPeek == Token.colon ||
        tokPeek == Token.identifier ||
        tokPeek == Token.asterisk ||
        tokPeek == Token.questionmark) {
      if (! clauseChain())
        return false;
    }
    return true;
  }

  boolean clauseResName() {
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      strNameSpec = "*";
      return true;
    }
    if (tokPeek() == Token.leftsquare) {
      tokenNext();
      if (! clauseResName0())
        return false;
      if (tokenNext().tok != Token.rightsquare)
        return false;
      return true;
    }
    return clauseResName0();
  }

  boolean clauseResName0() {
    Token token;
    while ((token = tokenNext()).tok == Token.questionmark)
      strNameSpec += "?";
    if (token.tok != Token.identifier)
      return residueIdentifierExpected();
    // FIXME mth -- the token "y" will not work here!
    tokenResIdent = token;
    strNameSpec += token.value;
    while (tokPeek() == Token.questionmark) {
      tokenNext();
      strNameSpec += '?';
    }
    return true;
  }

  boolean clauseResNo() {
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      return true;
    }
    boolean negative = false;
    if (tokPeek() == Token.hyphen) {
      tokenNext();
      negative = true;
    }
    Token tokenResno = tokenNext();
    if (tokenResno.tok != Token.integer)
      return resnoSpecificationExpected();
    resnoSpec = (negative) ? -tokenResno.intValue : tokenResno.intValue;
    return true;
  }

  boolean clauseChain() {
    Token tokenChain = tokenNext();
    if (tokenChain.tok == Token.asterisk ||
        tokenChain.tok == Token.questionmark)
      return true;
    if (tokenChain.tok == Token.colon)
      tokenChain = tokenNext();
    if (tokenChain.tok == Token.integer) {
      if (tokenChain.intValue < 0 || tokenChain.intValue > 9)
        return invalidChainSpecification();
      chainSpec = (char)('0' + tokenChain.intValue);
      return true;
    }
    if (tokenChain.tok == Token.ident) {
      String strChain = (String)tokenChain.value;
      if (strChain.length() != 1)
        return invalidChainSpecification();
      chainSpec = strChain.charAt(0);
    }
    return invalidChainSpecification();
  }

  boolean clauseAtomSpec() {
    if (tokenNext().tok != Token.dot)
      return invalidAtomSpecification();
    Token tokenAtomSpec = tokenNext();
    if (tokenAtomSpec.tok == Token.asterisk)
      return true;
    if (tokenAtomSpec.tok == Token.questionmark ||
        tokenAtomSpec.tok == Token.identifier)
      strAtomSpec += tokenAtomSpec.value;
    else
      return invalidAtomSpecification();
    while (tokPeek() == Token.questionmark ||
           tokPeek() == Token.identifier)
      strAtomSpec += tokenNext().value;
    return true;
  }

  boolean compileColorParam() {
    for (int i = 1; i < atokenCommand.length; ++i) {
      if (atokenCommand[i].tok == Token.leftsquare) {
        Token[] atokenNew = new Token[i + 1];
        System.arraycopy(atokenCommand, 0, atokenNew, 0, i);
        if (! compileRGB(atokenCommand, i, atokenNew))
          return false;
        atokenCommand = atokenNew;
        break;
      }
    }
    return true;
  }

  boolean compileRGB(Token[] atoken, int i, Token[] atokenNew) {
    if (atoken.length != i + 7 ||
        atoken[i  ].tok != Token.leftsquare ||
        atoken[i+1].tok != Token.integer    ||
        atoken[i+2].tok != Token.opOr       ||
        atoken[i+3].tok != Token.integer    ||
        atoken[i+4].tok != Token.opOr       ||
        atoken[i+5].tok != Token.integer    ||
        atoken[i+6].tok != Token.rightsquare)
      return badRGBColor();
    int rgb = atoken[i+1].intValue << 16 | atoken[i+3].intValue << 8 |
      atoken[i+5].intValue;
    atokenNew[i] = new Token(Token.colorRGB, rgb, "[R,G,B]");
    return true;
  }
}
