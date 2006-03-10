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
import org.jmol.g3d.Graphics3D;

import java.util.Vector;

class Compiler {

  String filename;
  String script;

  short[] lineNumbers;
  short[] lineIndices;
  Token[][] aatokenCompiled;

  boolean error;
  String errorMessage;
  String errorLine;
  boolean preDefining;
  
  static final boolean logMessages = false;

  private void log(String message) {
    if (logMessages)
      System.out.println(message);
  }

  boolean compile(String filename, String script) {
    this.filename = filename;
    this.script = script;
    lineNumbers = lineIndices = null;
    aatokenCompiled = null;
    errorMessage = errorLine = null;
    preDefining = (filename == "#predefine");
    if (compile0())
      return true;
    int icharEnd;
    if ((icharEnd = script.indexOf('\r', ichCurrentCommand)) == -1 &&
        (icharEnd = script.indexOf('\n', ichCurrentCommand)) == -1)
      icharEnd = script.length();
    errorLine = script.substring(ichCurrentCommand, icharEnd);
    return false;
  }

  short[] getLineNumbers() {
    return lineNumbers;
  }

  short[] getLineIndices() {
    return lineIndices;
  }

  Token[][] getAatokenCompiled() {
    return aatokenCompiled;
  }

  String getErrorMessage() {
    String strError = errorMessage;
    strError += " : " + errorLine + "\n";
    if (filename != null)
      strError += filename;
    strError += " line#" + lineCurrent;
    return strError;
  }

  int cchScript;
  short lineCurrent;

  int ichToken;
  int cchToken;
  Token[] atokenCommand;

  int ichCurrentCommand;

  boolean compile0() {
    cchScript = script.length();
    ichToken = 0;
    lineCurrent = 1;
    int lnLength = 8;
    lineNumbers = new short[lnLength];
    lineIndices = new short[lnLength];
    error = false;

    Vector lltoken = new Vector();
    Vector ltoken = new Vector();
    //Token tokenCommand = null;
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
            System.arraycopy(lineNumbers, 0, lnT, 0, lnLength);
            lineNumbers = lnT;
            lnT = new short[lnLength * 2];
            System.arraycopy(lineIndices, 0, lnT, 0, lnLength);
            lineIndices = lnT;
            lnLength *= 2;
          }
          lineNumbers[iCommand] = lineCurrent;
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
          String str = getUnescapedStringLiteral();
          ltoken.addElement(new Token(Token.string, str));
          continue;
        }
        if (tokCommand == Token.load && lookingAtLoadFormat()) {
          String strFormat = script.substring(ichToken, ichToken + cchToken);
          strFormat = strFormat.toLowerCase();
          ltoken.addElement(new Token(Token.identifier, strFormat));
          continue;
        }
        if ((tokCommand & Token.specialstring) != 0 &&
            lookingAtSpecialString()) {
          String str = script.substring(ichToken, ichToken + cchToken);
          ltoken.addElement(new Token(Token.string, str));
          continue;
        }
        if (lookingAtDecimal((tokCommand & Token.negnums) != 0)) {
          float value =
          // can't use parseFloat with jvm 1.1
          // Float.parseFloat(script.substring(ichToken, ichToken + cchToken));
            Float.valueOf(script.substring(ichToken, ichToken + cchToken))
            .floatValue();
          ltoken.addElement(new Token(Token.decimal, new Float(value)));
          continue;
        }
        if (lookingAtSeqcode()) {
          int seqNum =
            Integer.parseInt(script.substring(ichToken,
                                              ichToken + cchToken - 2));
          char insertionCode = script.charAt(ichToken + cchToken - 1);
          int seqcode = Group.getSeqcode(seqNum, insertionCode);
          ltoken.addElement(new Token(Token.seqcode, seqcode, "seqcode"));
          continue;
        }
        if (lookingAtInteger((tokCommand & Token.negnums) != 0)) {
          String intString = script.substring(ichToken, ichToken + cchToken);
          int val = Integer.parseInt(intString);
          ltoken.addElement(new Token(Token.integer, val, intString));
          continue;
        }
      }
      if (lookingAtLookupToken()) {
        String ident = script.substring(ichToken, ichToken + cchToken);
        Token token;
        // hack to support case sensitive alternate locations and chains
        // if an identifier is a single character long, then
        // allocate a new Token with the original character preserved
        if (ident.length() == 1) {
          token = (Token) Token.map.get(ident);
          if (token == null) {
            String lowerCaseIdent = ident.toLowerCase();
            token = (Token) Token.map.get(lowerCaseIdent);
            if (token != null)
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
        case Token.nada:
          ichCurrentCommand = ichToken;
          //tokenCommand = token;
          tokCommand = tok;
          if ((tokCommand & Token.command) == 0)
            return commandExpected();
          break;
        case Token.set:
          if (ltoken.size() == 1) {
            if ((tok & Token.setspecial) != 0) {
              //tokenCommand = token;
              tokCommand = tok;
              ltoken.removeAllElements();
              break;
            }
            if ((tok & Token.setparam) == 0 &&
                tok != Token.identifier)
              return cannotSet(ident);
          }
          break;
        case Token.show:
          if ((tok & Token.showparam) == 0)
            return cannotShow(ident);
          break;
        case Token.define:
          if (ltoken.size() == 1) {
            // we are looking at the variable name
            
            if (!preDefining &&  tok != Token.identifier) { 
              if ((tok & Token.predefinedset) != Token.predefinedset) {
                System.out.println("WARNING: redefining " + ident + "; was " + token);
                tok = token.tok = Token.identifier;
                Token.map.put(ident, token);
                System.out.println("WARNING: not all commands may continue to be functional for the life of the applet!");
              } else {
                System.out.println("WARNING: predefined term '" + ident + "' has been redefined by the user until the next file load.");
              }
            }

            if (tok != Token.identifier &&
                (tok & Token.predefinedset) != Token.predefinedset)
              return invalidExpressionToken(ident);
          } else {
            // we are looking at the expression
            if (tok != Token.identifier && tok != Token.set &&
                (tok & (Token.expression | Token.predefinedset)) == 0)
              return invalidExpressionToken(ident);
          }
          break;
        case Token.center:
        case Token.restrict:
        case Token.select:
          if (tok != Token.identifier && (tok & Token.expression) == 0)
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
    aatokenCompiled = new Token[lltoken.size()][];
    lltoken.copyInto(aatokenCompiled);
    return true;
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

  private final static boolean isSpaceOrTab(char ch) {
    return ch == ' ' || ch == '\t';
  }

  boolean lookingAtLeadingWhitespace() {
    log("lookingAtLeadingWhitespace");
    int ichT = ichToken;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    cchToken = ichT - ichToken;
    log("leadingWhitespace cchScript=" + cchScript + " cchToken=" + cchToken);
    return cchToken > 0;
  }

  boolean lookingAtComment() {
    log ("lookingAtComment ichToken=" + ichToken + " cchToken=" + cchToken);
    // first, find the end of the statement and scan for # (sharp) signs
    char ch;
    int ichEnd = ichToken;
    int ichFirstSharp = -1;
    while (ichEnd < cchScript &&
           (ch = script.charAt(ichEnd)) != ';' && ch != '\r' && ch != '\n') {
      if (ch == '#' && ichFirstSharp == -1) {
        ichFirstSharp = ichEnd;
        //System.out.println("I see a first sharp @ " + ichFirstSharp);
      }
      ++ichEnd;
    }
    if (ichFirstSharp == -1) // there were no sharps found
      return false;

    /****************************************************************
     * check for #jc comment
     * if it occurs anywhere in the statement, then the statement is
     * not executed.
     * This allows statements which are executed in RasMol but are
     * comments in Jmol
     ****************************************************************/

    /*
    System.out.println("looking for #jc comment");
    System.out.println("count left=" + (cchScript - ichFirstSharp) + '\n' +
                       script.charAt(ichFirstSharp + 1) +
                       script.charAt(ichFirstSharp + 2));
    */
    

    if (cchScript - ichFirstSharp >= 3 &&
        script.charAt(ichFirstSharp + 1) == 'j' &&
        script.charAt(ichFirstSharp + 2) == 'c') {
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
    if (cchScript > ichToken + 3 &&
        script.charAt(ichToken + 1) == 'j' &&
        script.charAt(ichToken + 2) == 'x' &&
        isSpaceOrTab(script.charAt(ichToken + 3))) {
      cchToken = 4; // #jx[\s\t]
      return true;
    }

    // first character was a sharp, but was not #jx ... strip it all
    cchToken = ichEnd - ichToken;
    return true;
  }

  boolean lookingAtEndOfLine() {
    log("lookingAtEndOfLine");
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
    if (script.charAt(ichToken) != '"')
      return false;
    // remove support for single quote
    // in order to use it in atom expressions
    //    char chFirst = script.charAt(ichToken);
    //    if (chFirst != '"' && chFirst != '\'')
    //      return false;
    int ichT = ichToken + 1;
    //    while (ichT < cchScript && script.charAt(ichT++) != chFirst)
    char ch;
    boolean previousCharBackslash = false;
    while (ichT < cchScript) {
      ch = script.charAt(ichT++);
      if (ch == '"' && !previousCharBackslash)
        break;
      previousCharBackslash = ch == '\\' ? !previousCharBackslash : false;
    }
    cchToken = ichT - ichToken;
    return true;
  }

  String getUnescapedStringLiteral() {
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
            for (int k = digitCount; --k >= 0 && ich < ichMax; ) {
              char chT = script.charAt(ich);
              int hexit = getHexitValue(chT);
              if (hexit < 0)
                break;
              unicode <<= 4;
              unicode += hexit;
              ++ich;
            }
            ch = (char)unicode;
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
  String[] loadFormats = { "alchemy ", "mol2 ", "mopac ", "nmrpdb ",
                           "charmm ", "xyz ", "mdl ", "pdb "};

  boolean lookingAtLoadFormat() {
    for (int i = loadFormats.length; --i>=0; ) {
      String strFormat = loadFormats[i];
      int cchFormat = strFormat.length();
      if (script.regionMatches(true, ichToken, strFormat, 0, cchFormat)) {
        cchToken = cchFormat - 1; // subtract off the space character
        return true;
      }
    }
    return false;
  }

  boolean lookingAtSpecialString() {
    int ichT = ichToken;
    char ch;
    while (ichT < cchScript &&
           (ch = script.charAt(ichT)) != ';' && ch != '\r' && ch != '\n')
      ++ichT;
    cchToken = ichT - ichToken;
    log("lookingAtSpecialString cchToken=" + cchToken);
    return cchToken > 0;
  }

  boolean lookingAtDecimal(boolean allowNegative) {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (script.charAt(ichT) == '-')
      ++ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    if (ichT == cchScript || ch != '.')
      return false;
    // to support 1.ca, let's check the character after the dot
    // to determine if it is an alpha
    if (ch == '.' && (ichT + 1 < cchScript) &&
        isAlphabetic(script.charAt(ichT + 1)))
      return false;
    ++ichT;
    while (ichT < cchScript && isDigit(script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    cchToken = ichT - ichToken;
    return digitSeen;
  }

  static boolean isAlphabetic(char ch) {
    return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
  }

  static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  boolean lookingAtSeqcode() {
    int ichT = ichToken;
    char ch = ' ';
    while (ichT < cchScript && isDigit(ch = script.charAt(ichT)))
      ++ichT;
    if (ichT == ichToken || ichT + 2 > cchScript || ch != '^')
      return false;
    ch = script.charAt(++ichT);
    if (! isAlphabetic(ch))
      return false;
    ++ichT;
    cchToken = ichT - ichToken;
    return true;
  }

  boolean lookingAtInteger(boolean allowNegative) {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (allowNegative && script.charAt(ichToken) == '-')
      ++ichT;
    int ichBeginDigits = ichT;
    while (ichT < cchScript && isDigit(script.charAt(ichT)))
      ++ichT;
    if (ichBeginDigits == ichT)
      return false;
    cchToken = ichT - ichToken;
    return true;
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
    case '*':
    case '-':
    case '[':
    case ']':
    case '+':
    case ':':
    case '@':
    case '.':
    case '%':
      break;
    case '&':
    case '|':
      if (ichT < cchScript && script.charAt(ichT) == ch)
        ++ichT;
      break;
    case '<':
    case '=':
    case '>':
      System.out.println("compiler < " + ichT + " " + cchScript + " "+script.substring(0,ichT+1));
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
    case '?': // include question marks in identifier for atom expressions
      while (ichT < cchScript &&
             (isAlphabetic(ch = script.charAt(ichT)) ||
              isDigit(ch) ||
              ch == '_' || ch == '?') ||
             // hack for insertion codes embedded in an atom expression :-(
             // select c3^a
             (ch == '^' && ichT > ichToken && isDigit(script.charAt(ichT - 1)))
             )
        ++ichT;
      break;
    }
    cchToken = ichT - ichToken;
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
  private boolean nonSequentialExpressions() {
    return compileError("embedded expression not followed directly by another embedded expression");
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
  private boolean stringExpected() {
    return compileError("string expected");
  }
  private boolean unrecognizedExpressionToken() {
    return compileError("unrecognized expression token:" + valuePeek());
  }
  /*
  private boolean integerExpectedAfterHyphen() {
    return compileError("integer expected after hyphen");
  }
  */
  private boolean comparisonOperatorExpected() {
    return compileError("comparison operator expected");
  }
  private boolean integerExpected() {
    return compileError("integer expected");
  }
  /*
  private boolean numberExpected() {
    return compileError("number expected");
  }
  */
  private boolean numberOrKeywordExpected() {
    return compileError("number or keyword expected");
  }
  private boolean badRGBColor() {
    return compileError("bad [R,G,B] color");
  }
  private boolean identifierOrResidueSpecificationExpected() {
    return compileError("identifier or residue specification expected");
  }
  private boolean residueSpecificationExpected() {
    return compileError("3 letter residue specification expected");
  }
  /*
  private boolean resnumSpecificationExpected() {
    return compileError("residue number specification expected");
  }
  private boolean invalidResidueNameSpecification(String strResName) {
    return compileError("invalid residue name specification:" + strResName);
  }
  */
  private boolean invalidChainSpecification() {
    return compileError("invalid chain specification");
  }
  private boolean invalidModelSpecification() {
    return compileError("invalid model specification");
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
    //System.out.println(tokenCommand + script);
    int tokCommand = tokenCommand.tok;
    if ((tokenCommand.intValue & Token.onDefault1) == Token.onDefault1 &&
        ltoken.size() == 1)
      ltoken.addElement(Token.tokenOn);
    if (tokCommand == Token.set) {
      int size = ltoken.size();
      if (size < 2)
        return badArgumentCount();
      /*
       * miguel 2005 01 01 - setDefaultOn is not used
      if (size == 2 &&
          (((Token)ltoken.elementAt(1)).tok & Token.setDefaultOn) != 0)
        ltoken.addElement(Token.tokenOn);
      */
    }
    atokenCommand = new Token[ltoken.size()];
    ltoken.copyInto(atokenCommand);
    if ((tokCommand & (Token.expressionCommand|Token.embeddedExpression)) != 0
        && !compileExpression())
      return false;
    if ((tokCommand & Token.colorparam) != 0 && !compileColorParam())
      return false;
    if ((tokenCommand.intValue & Token.varArgCount) == 0 &&
        (tokenCommand.intValue & 0x0F) + 1 != atokenCommand.length)
      return badArgumentCount();
    return true;
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

    expression       :: = clauseOr

    clauseOr         ::= clauseAnd {OR clauseAnd}*

    clauseAnd        ::= clauseNot {AND clauseNot}*

    clauseNot        ::= NOT clauseNot | clausePrimitive

    clausePrimitive  ::= clauseComparator |
                         clauseWithin |
                         clauseResidueSpec |
                         none | all |
                         ( clauseOr )

    clauseComparator ::= atomproperty comparatorop integer

    clauseWithin     ::= WITHIN ( clauseDistance , expression )

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

    clauseModelSpec  ::= {:|/} * | integer

  */

  private boolean compileExpression() {
    int i = 1;
    boolean isMultipleOK = false;
    int tokCommand = atokenCommand[0].tok;
    if (tokCommand == Token.define)
      i = 2;
    else if ((tokCommand & Token.embeddedExpression) != 0) {
      // look for the open parenthesis
      isMultipleOK = true;
      while (i < atokenCommand.length &&
             atokenCommand[i].tok != Token.leftparen)
        ++i;
    }
    if (i >= atokenCommand.length)
      return true;
    int expPtr=i;
    // 0 here means OK; -1 means error; > 0 means pointer to the next expression 
    while (expPtr > 0) {
     // System.out.println("next expression at " + expPtr);
      expPtr = compileExpression(expPtr);
      if (expPtr > 0 && ! isMultipleOK)
        return endOfExpressionExpected();
      if (expPtr <= 0) return (expPtr == 0);
      if (atokenCommand[expPtr].tok != Token.leftparen)
        return nonSequentialExpressions();
    }
    //really cannot get here
    return true;
  }

  Vector ltokenPostfix = null;
  Token[] atokenInfix;
  int itokenInfix;
                  
  boolean addTokenToPostfix(Token token) {
    ltokenPostfix.addElement(token);
    return true;
  }

  int compileExpression(int itoken) {
    int expPtr = 0;
    ltokenPostfix = new Vector();
    for (int i = 0; i < itoken; ++i)
      addTokenToPostfix(atokenCommand[i]);
    atokenInfix = atokenCommand;
    itokenInfix = itoken;

    addTokenToPostfix(Token.tokenExpressionBegin);
    if (! clauseOr())
      return -1;
    addTokenToPostfix(Token.tokenExpressionEnd);
    if (itokenInfix != atokenInfix.length) {
      /*
      System.out.println("itokenInfix=" + itokenInfix + " atokenInfix.length="
                         + atokenInfix.length);
      for (int i = 0; i < atokenInfix.length; ++i) 
        System.out.println("" + i + ":" + atokenInfix[i]);
      */
      //not a problem! 
      expPtr = ltokenPostfix.size();
      for (int i = itokenInfix; i < atokenInfix.length; ++i)
        addTokenToPostfix(atokenCommand[i]);
    }
    atokenCommand = new Token[ltokenPostfix.size()];
    ltokenPostfix.copyInto(atokenCommand);
    return expPtr;
  }

  Token tokenNext() {
    if (itokenInfix == atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix++];
  }

  boolean tokenNext(int tok) {
    Token token = tokenNext();
    return (token != null && token.tok == tok);
  }
  
  Object valuePeek() {
    if (itokenInfix == atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix].value;
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
      addTokenToPostfix(tokenOr);
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
      addTokenToPostfix(tokenAnd);
    }
    return true;
  }

  boolean clauseNot() {
    if (tokPeek() == Token.opNot) {
      Token tokenNot = tokenNext();
      if (! clauseNot())
        return false;
      return addTokenToPostfix(tokenNot);
    }
    return clausePrimitive();
  }

  boolean clausePrimitive() {
    int tok = tokPeek();
    switch (tok) {
    case Token.within:
      return clauseWithin();
    case Token.substructure:
      return clauseSubstructure();
    case Token.hyphen: // selecting a negative residue spec
    case Token.integer:
    case Token.seqcode:
    case Token.asterisk:
    case Token.leftsquare:
    case Token.identifier:
    case Token.x:
    case Token.y:
    case Token.z:
    case Token.colon:
      return clauseResidueSpec();
    default:
      if ((tok & Token.atomproperty) == Token.atomproperty)
        return clauseComparator();
      if ((tok & Token.predefinedset) != Token.predefinedset)
        break;
      // fall into the code and below and just add the token
    case Token.all:
    case Token.none:
      return addTokenToPostfix(tokenNext());
    case Token.leftparen:
      tokenNext();
      if (! clauseOr())
          return false;
      if (! tokenNext(Token.rightparen))
        return rightParenthesisExpected();
      return true;
    }
    return unrecognizedExpressionToken();
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
    return addTokenToPostfix(new Token(tokenComparator.tok,
                                       tokenAtomProperty.tok,
                                       new Integer(val)));
  }

  boolean clauseWithin() {
    tokenNext();                             // WITHIN
    if (! tokenNext(Token.leftparen))  // (
      return leftParenthesisExpected();
    Object distance;
    Token tokenDistance = tokenNext();       // distance
    if (tokenDistance == null)
      return numberOrKeywordExpected();
    switch(tokenDistance.tok) {
    case Token.integer:
      distance = new Float((tokenDistance.intValue * 4) / 1000f);
      break;
    case Token.decimal:
    case Token.group:
    case Token.chain:
    case Token.model:
      distance = tokenDistance.value;
      break;
    default:
      return numberOrKeywordExpected();
    }
    if (! tokenNext(Token.opOr))       // ,
      return commaExpected();
    if (! clauseOr())                        // *expression*
      return false;
    if (! tokenNext(Token.rightparen)) // )T
      return rightParenthesisExpected();
    return addTokenToPostfix(new Token(Token.within, distance));
  }

  boolean clauseSubstructure() {
    tokenNext();                             // substructure
    if (! tokenNext(Token.leftparen))  // (
      return leftParenthesisExpected();
    Token tokenSmiles = tokenNext();         // "smiles"
    if (tokenSmiles == null || tokenSmiles.tok != Token.string)
      return stringExpected();
    if (! tokenNext(Token.rightparen)) // )
      return rightParenthesisExpected();
    return addTokenToPostfix(new Token(Token.substructure, tokenSmiles.value));
  }

  boolean residueSpecCodeGenerated;

  boolean generateResidueSpecCode(Token token) {
    addTokenToPostfix(token);
    if (residueSpecCodeGenerated)
        addTokenToPostfix(Token.tokenAnd);
    residueSpecCodeGenerated = true;
    return true;
  }

  boolean clauseResidueSpec() {
    boolean specSeen = false;
    residueSpecCodeGenerated= false;
    int tok = tokPeek();
    if (tok == Token.asterisk ||
        tok == Token.leftsquare ||
        tok == Token.identifier ||
        tok == Token.set ||
        tok == Token.x ||
        tok == Token.y ||
        tok == Token.z) {
      log("I see a residue name");
      if (! clauseResNameSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.asterisk ||
        tok == Token.hyphen ||
        tok == Token.integer ||
        tok == Token.seqcode) {
      log("I see a residue number");
      if (! clauseResNumSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.colon ||
        tok == Token.asterisk ||
        tok == Token.identifier ||
        tok == Token.x ||
        tok == Token.y ||
        tok == Token.z ||
        tok == Token.integer) {
      if (! clauseChainSpec())
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
      if (! clauseAlternateSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.colon ||
        tok == Token.slash) {
      if (! clauseModelSpec())
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

  boolean clauseResNameSpec() {
    int tokPeek = tokPeek();
    if (tokPeek == Token.asterisk) {
      tokenNext();
      return true;
    }
    Token tokenT = tokenNext();
    if (tokenT == null) 
      return false;
    if (tokenT.tok == Token.leftsquare) {
      log("I see a left square bracket");
      // FIXME mth -- maybe need to deal with asterisks here too
      tokenT = tokenNext(); 
      if (tokenT == null) 
        return false;
      String strSpec = "";
      if (tokenT.tok == Token.plus) {
        strSpec = "+";
        tokenT = tokenNext();
        if (tokenT == null) 
          return false;
      }
      // what a hack :-(
      int tok = tokenT.tok;
      if (tok == Token.integer) {
        strSpec += tokenT.value;
        tokenT = tokenNext();
        if (tokenT == null) 
          return false;
        tok = tokenT.tok;
      }
      if (tok == Token.identifier || tok == Token.set ||
          tok == Token.x || tok == Token.y || tok == Token.z) {
        strSpec += tokenT.value;
        tokenT = tokenNext();
        if (tokenT == null) 
          return false;
        tok = tokenT.tok;
      }
      if (strSpec == "")
        return residueSpecificationExpected();
      strSpec = strSpec.toUpperCase();
      int groupID = Group.lookupGroupID(strSpec);
      if (groupID != -1)
        generateResidueSpecCode(new Token(Token.spec_resid, groupID, strSpec));
      else
        generateResidueSpecCode(new Token(Token.spec_name_pattern, strSpec));
      return tok == Token.rightsquare;
    }
    return processIdentifier(tokenT);
  }

  boolean processIdentifier(Token tokenIdent) {
    // OK, the kludge here is that in the general case, it is not
    // possible to distinguish between an identifier and an atom expression
    if (tokenIdent.tok != Token.identifier)
      return identifierOrResidueSpecificationExpected();
    String strToken = (String)tokenIdent.value;
    log("processing identifier:" + strToken);
    int cchToken = strToken.length();

    // too short to be an atom specification? 
    if (cchToken < 3)
      return generateResidueSpecCode(tokenIdent);
    // has characters where there should be digits?
    // but don't look at last character because it could be chain spec
    for (int i = 3; i < cchToken-1; ++i)
      if (!Character.isDigit(strToken.charAt(i)))
        return generateResidueSpecCode(tokenIdent);
    log("still here looking at:" + strToken);

    // still might be an identifier ... so be careful
    int seqcode = -1;
    char chain = '?';
    if (cchToken > 3) {
      // let's take a look at the last character
      String strResno;
      char chLast = strToken.charAt(cchToken-1);
      log("the last character is:" + chLast);
      if (Character.isDigit(chLast)) {
        strResno = strToken.substring(3);
        log("strResNo=" + strResno);
      } else {
        chain = chLast;
        strResno = strToken.substring(3, cchToken - 1);
      }
      try {
        int sequenceNum = Integer.parseInt(strResno);
        log("I parsed sequenceNum=" + sequenceNum);
        seqcode = Group.getSeqcode(sequenceNum, ' ');
      } catch (NumberFormatException e) {
        return generateResidueSpecCode(tokenIdent);
      }
    }
    String strUpper3 = strToken.substring(0, 3).toUpperCase();
    int groupID;
    if (strUpper3.charAt(0) == '?' ||
        strUpper3.charAt(1) == '?' ||
        strUpper3.charAt(2) == '?') {
      generateResidueSpecCode(new Token(Token.spec_name_pattern, strUpper3));
    } else if ((groupID = Group.lookupGroupID(strUpper3)) != -1) {
      generateResidueSpecCode(new Token(Token.spec_resid, groupID, strUpper3));
    } else {
      return generateResidueSpecCode(tokenIdent);
    }
    log(" I see a residue name:" + strUpper3 +
                       " seqcode=" + seqcode +
                       " chain=" + chain);

    if (seqcode != -1)
      generateResidueSpecCode(new Token(Token.spec_seqcode,
                                        seqcode, "spec_seqcode"));
    if (chain != '?')
      generateResidueSpecCode(new Token(Token.spec_chain, chain, "spec_chain"));
    return true;
  }

  boolean clauseResNumSpec() {
    log("clauseResNumSpec()");
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      return true;
    }
    return clauseSequenceRange();
  }

  boolean clauseSequenceRange() {
    if (! clauseSequenceCode())
      return false;
    if (tokPeek() == Token.hyphen) {
      tokenNext();
      int seqcodeA = seqcode;
      if (! clauseSequenceCode())
        return false;
      return generateResidueSpecCode(new Token(Token.spec_seqcode_range,
                                               seqcodeA,
                                               new Integer(seqcode)));
    }
    return generateResidueSpecCode(new Token(Token.spec_seqcode,
                                             seqcode, "seqcode"));
  }

  int seqcode;

  boolean clauseSequenceCode() {
    boolean negative = false;
    int tokPeek = tokPeek();
    if (tokPeek == Token.hyphen) {
      tokenNext();
      negative = true;
      tokPeek = tokPeek();
    }
    if (tokPeek == Token.seqcode)
      seqcode = tokenNext().intValue;
    else if (tokPeek == Token.integer)
      seqcode = Group.getSeqcode(tokenNext().intValue, ' ');
    else
      return false;
    if (negative)
      seqcode = -seqcode;
    return true;
  }

  boolean clauseChainSpec() {
    if (tokPeek() == Token.colon)
      tokenNext();
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      return true;
    }
    if (tokPeek() == Token.colon) // null chain followed by model spec    
      return true;
    Token tokenChain;
    char chain;
    switch (tokPeek()) {
    case Token.colon:
    case Token.nada:
    case Token.dot:
      chain = '\0';
      break;
    case Token.integer:
      tokenChain = tokenNext();
      if (tokenChain.intValue < 0 || tokenChain.intValue > 9)
        return invalidChainSpecification();
      chain = (char)('0' + tokenChain.intValue);
      break;
    case Token.identifier:
    case Token.x:
    case Token.y:
    case Token.z:
      tokenChain = tokenNext();
      String strChain = (String)tokenChain.value;
      if (strChain.length() != 1)
        return invalidChainSpecification();
      chain = strChain.charAt(0);
      if (chain == '?')
        return true;
      break;
    default:
      return invalidChainSpecification();
    }
    return generateResidueSpecCode(new Token(Token.spec_chain,
                                             chain, "spec_chain"));
  }

  boolean clauseAlternateSpec() {
    int tok = tokPeek();
    if (tok == Token.percent)
      tokenNext();
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      return true;
    }
    Token tokenAlternate = tokenNext();
    switch (tokenAlternate.tok) {
    case Token.string:
    case Token.integer:
    case Token.identifier:
    case Token.x:
    case Token.y:
    case Token.z:
      break;
    default:
      return invalidModelSpecification();
    }
    String alternate = (String)tokenAlternate.value;
    System.out.println("alternate specification seen:" + alternate);
    return generateResidueSpecCode(new Token(Token.spec_alternate, alternate));
  }

  boolean clauseModelSpec() {
    int tok = tokPeek();
    if (tok == Token.colon || tok == Token.slash)
      tokenNext();
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      return true;
    }
    Token tokenModel = tokenNext();
    if (tokenModel == null)
      return invalidModelSpecification();
    switch (tokenModel.tok) {
    case Token.string:
    case Token.integer:
    case Token.identifier:
    case Token.x:
    case Token.y:
    case Token.z:
      break;
    default:
      return invalidModelSpecification();
    }
    return generateResidueSpecCode(new Token(Token.spec_model,
                                             (String)tokenModel.value));
  }

  boolean clauseAtomSpec() {
    if (! tokenNext(Token.dot))
      return invalidAtomSpecification();
    Token tokenAtomSpec = tokenNext();
    if (tokenAtomSpec == null)
      return true;
    switch (tokenAtomSpec.tok) {
    case Token.asterisk:
      return true;
    case Token.x:
    case Token.y:
    case Token.z:
    case Token.identifier:
      break;
    default:
      return invalidAtomSpecification();
    }
    String atomSpec = (String)tokenAtomSpec.value;
    if (tokPeek() == Token.asterisk) {
      tokenNext();
      // this one is a '*' as a prime, not a wildcard
      atomSpec += "*";
    }
    return generateResidueSpecCode(new Token(Token.spec_atom, atomSpec));
  }

  boolean compileColorParam() {
    for (int i = 1; i < atokenCommand.length; ++i) {
      Token token = atokenCommand[i];
      if (token.tok == Token.leftsquare) {
        Token[] atokenNew = new Token[i + 1];
        System.arraycopy(atokenCommand, 0, atokenNew, 0, i);
        if (! compileRGB(atokenCommand, i, atokenNew))
          return false;
        atokenCommand = atokenNew;
        break;
      } else if (token.tok == Token.identifier) {
        String id = (String)token.value;
        int argb = Graphics3D.getArgbFromString(id);
        if (argb != 0) {
          token.tok = Token.colorRGB;
          token.intValue = argb;
        }
      }
    }
    return true;
  }

  boolean compileRGB(Token[] atoken, int i, Token[] atokenNew) {
    if (atoken.length == i + 7 &&
        atoken[i  ].tok == Token.leftsquare &&
        atoken[i+1].tok == Token.integer    &&
        atoken[i+2].tok == Token.opOr       &&
        atoken[i+3].tok == Token.integer    &&
        atoken[i+4].tok == Token.opOr       &&
        atoken[i+5].tok == Token.integer    &&
        atoken[i+6].tok == Token.rightsquare) {
      int rgb = atoken[i+1].intValue << 16 | atoken[i+3].intValue << 8 |
        atoken[i+5].intValue;
      atokenNew[i] = new Token(Token.colorRGB, rgb, "[R,G,B]");
      return true;
    }
    // chime also accepts [xRRGGBB]
    if (atoken.length == i + 3 &&
        atoken[i  ].tok == Token.leftsquare &&
        atoken[i+1].tok == Token.identifier &&
        atoken[i+2].tok == Token.rightsquare) {
      String hex = (String)atoken[i+1].value;
      if (hex.length() == 7 &&
          hex.charAt(0) == 'x') {
        try {
          int rgb = Integer.parseInt(hex.substring(1), 16);
          atokenNew[i] = new Token(Token.colorRGB, rgb, "[xRRGGBB]");
          return true;
        } catch (NumberFormatException e) {
        }
      }
    }
    return badRGBColor();
  }

}
