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
import org.jmol.util.TextFormat;
import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;
import org.jmol.modelset.Group;
import org.jmol.modelset.Bond.BondSet;

import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;

import javax.vecmath.Point3f;

class Compiler {

  Hashtable localFunctions = new Hashtable();
  static Hashtable globalFunctions = new Hashtable();
  boolean isFunction(String name) {
    return (name.indexOf("_") == 0 ? localFunctions : globalFunctions).containsKey(name);
  }
  
  private Function thisFunction;
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
  private boolean isCheckOnly;
  private Hashtable contextVariables;
  
  private boolean logMessages = false;

  /*
   * Flow Contexts in Jmol 11.3.23+
   * 
   * As of Jmol 11.3.23, the Jmol scripting language includes a variety of 
   * standard flow control structures:
   * 
   * script filename.spt  # the principal file-based context
   * 
   * function xxx(a,b,c)/end function
   * 
   * if/elseif/else/endif
   * for/break/continue/end for
   * while/break/continue/end while
   * 
   * This is being handled by creating a "flow context" for each of these elements. 
   * There can be only one currently active flow context. 
   * 
   * The primary context is the script file. Variables declared using the "var"
   * keyword are isolated to that script file. 
   * 
   * Function contexts may only be created within the .spt file context. Note that
   * functions are saved in a STATIC Hashtable, htFunctions, so these definitions
   * apply to all applets present from a given server and for an entire user session.
   * There is no particular reason they HAVE to be static, however.
   * 
   * Other contexts may be nested to potentially any depth, as in all programming
   * languages.
   * 
   * The syntactic model is related most closely to JavaScript. But there is no
   * exact model, because we are already useing { } for too many things (select
   * criteria, points, planes, and bitsets). So this syntax uses no braces. 
   * 
   * Instead, the syntax uses the Visual Basic-like "end" syntax. So we have;
   * 
   * if (x)
   *  [do this]
   * else if (y)
   *  [do this]
   * else
   *  [do this]
   * end if
   * 
   * The keywords "elseif" and "endif" are synonymous with "else if" and "end if",
   * just to make that a non-issue. Documentation will refer to "else if" and "end if"
   * so as to be fully consistent with "end for", "end while", and "end function".
   * 
   * TOKENIZATION OF FLOW CONTROL
   * ----------------------------
   * 
   * The .tok field of the command token maintains a variety of attributes for all
   * commands. Two new attributes include 
   * 
   *   noeval        function/end function are never passed to Eval
   *   flowcommand   function/if/else/elseif/endif/for/while/break/continue/end
   *                   indicates special intValue is in effect and that parameter
   *                   number checking is done separately. 
   * 
   * Tokens in general have three fields: int tok, int intValue, and Object value.  
   * The system implemented in jmol 11.3.23 involves coopting the intValue field of
   * the first token of each statement -- the command token. This formerly static field is 
   * generally used to indicate the number of allowed parameters, but that's not
   * necessary for this small set of special commands. Because we are using it dynamically,
   * all flowcommand tokens are copies, not the originals. 
   * 
   * The commands if/elseif/else/endif are implemented as a singly-linked list. 
   * Each intValue field points to the next in the series. In the case of elseif and else, 
   * if this pointer is negative, it indicates that a previous block has been 
   * executed, and this block should be skipped.   
   *  
   * The commands for/end for and while/end while implement intValue as circularly-linked
   * lists. The for/while statement intValue field points to its end statement, and the 
   * end statement points to its corresponding for/while statement. 
   * 
   * In addition, break and continue point to their corresponding for or while 
   * statement so that the end statement pointer can be retrieved (in the case of break)
   * or used for direction (continue). 
   * 
   * If a number is added after break or continue, it indicates the number of levels 
   * of for/while to skip. Thus, "break 1" breaks to one level above the current context.  
   * 
   */
  
  private class FlowContext {
    Token token;
    int pt0;
    Function function;
    FlowContext parent;
    
    FlowContext(Token token, int pt0, FlowContext parent) {
      this.token = token;
      this.pt0 = pt0;
      this.parent = parent;
    }
    
    void setFunction(Function function) {
      this.function = function;
    }

    void setFunction(String script, int ichCurrentCommand, 
                     int pt, short[] lineNumbers,
                     int[] lineIndices, Vector lltoken) {
      int cmdpt0 = function.cmdpt0;
      int chpt0 = function.chpt0;
      int nCommands = pt - cmdpt0;
      function.script = script.substring(chpt0, ichCurrentCommand);
      Token[][] aatoken = function.aatoken = new Token[nCommands][];
      function.lineIndices = new int[nCommands];
      function.lineNumbers = new short[nCommands];
      short line0 = (short) (lineNumbers[cmdpt0] - 1);
      for (int i = 0; i < nCommands; i++) {
        function.lineNumbers[i] = (short) (lineNumbers[cmdpt0 + i] - line0);
        function.lineIndices[i] = lineIndices[cmdpt0 + i] - chpt0;
        aatoken[i] = (Token[]) lltoken.get(cmdpt0 + i);
        if (aatoken[i].length > 0) {
          Token tokenCommand = aatoken[i][0];
          if (Compiler.tokAttr(tokenCommand.tok, Token.flowCommand))
            tokenCommand.intValue -= (tokenCommand.intValue < 0 ? -cmdpt0 : cmdpt0);
        }
      }
      for (int i = pt; --i >= cmdpt0;) {
        lltoken.remove(i);
        lineIndices[i] = 0;
      }
    }
  }
  
  private FlowContext flowContext;
  private int nSemiSkip = 0;
  
  Compiler(Viewer viewer) {
    this.viewer = viewer;
  }

  boolean compile(String filename, String script, boolean isPredefining,
                  boolean isSilent, boolean debugScript, boolean isCheckOnly) {
    this.isCheckOnly = isCheckOnly;
    this.filename = filename;
    this.isSilent = isSilent;
    this.script = cleanScriptComments(script);
    this.contextVariables = null;
    logMessages = (!isSilent && !isPredefining && debugScript);
    preDefining = (filename == "#predefine");
    return (compile0() || handleError());
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

  Hashtable getContextVariables() {
    return contextVariables;
  }
  
  private void addContextVariable(String ident) {
    if (thisFunction == null) {
      if (contextVariables == null)
        contextVariables = new Hashtable();
      contextVariables.put(ident, new Token(Token.string, ""));
    } else {
      thisFunction.addVariable(ident, false);
    }
  }

  Token[][] getAatokenCompiled() {
    return aatokenCompiled;
  }

  static boolean tokAttr(int a, int b) {
    return (a & b) == b;
  }
  
  static boolean tokAttrOr(int a, int b1, int b2) {
    return (a & b1) == b1 || (a & b2) == b2;
  }
  
 
  static int modelValue(String strDecimal) {
    //this will overflow, but it doesn't matter -- it's only for file.model
    //2147483647 is maxvalue, so this allows loading
    //simultaneously up to 2147 files. Yeah, sure!
    int pt = strDecimal.indexOf(".");
    if (pt < 1 || strDecimal.charAt(0) == '-')
      return Integer.MAX_VALUE;
    int i = 0;
    int j = 0;
    if (pt > 0 && (i = Integer.parseInt(strDecimal.substring(0, pt))) < 0)
      i = -i;
    if (pt < strDecimal.length() - 1)
      try {
         j = Integer.parseInt(strDecimal.substring(pt + 1));
      } catch(NumberFormatException e) {
        // not a problem
      }
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
    int pt, pt1, pt2;
    while ((pt = script.indexOf("/**")) >= 0) {
      pt1 = script.indexOf("**/", pt + 3);
      if (pt1 < 0)
        break;
      String scrap = script.substring(pt, pt1);
      if ((pt2 = scrap.indexOf(JmolConstants.EMBEDDED_SCRIPT_TAG)) >= 0)
        return cleanScriptComments(scrap.substring(pt2 + 30));
      script = script.substring(0, pt) + script.substring(pt1 + 3);
    }
    while ((pt = script.indexOf("/*")) >= 0) {
      pt1 = script.indexOf("*/", pt + 2);
      if (pt1 < 0)
        break;
      String scrap = script.substring(pt + 2, pt1);
      if ((pt2 = scrap.indexOf(JmolConstants.EMBEDDED_SCRIPT_TAG)) >= 0)
        return scrap.substring(pt2 + 30);
      script = script.substring(0, pt) + script.substring(pt1 + 2);
    }
    return script;
  }
  
  private int cchScript;
  private short lineCurrent;

  private int ichToken;
  private int cchToken;
  private int iCommand;

  private int ichCurrentCommand;
  private boolean isNewSet;
  private boolean isSetBrace;
  private int ptNewSetModifier;
  private boolean iHaveQuotedString = false;
  
  private Vector ltoken;
  private Token lastToken;
  private void addTokenToPrefix(Token token) {
    ltoken.addElement(token);
    lastToken = token;
  }

  private boolean compile0() {
    lineNumbers = null;
    lineIndices = null;
    aatokenCompiled = null;
    errorMessage = errorLine = null;
    flowContext = null;
    nSemiSkip = 0;
    cchScript = script.length();
    ichToken = 0;
    ichCurrentCommand = 0;
    lineCurrent = 1;
    iCommand = 0;
    lastToken = Token.tokenOff;
    int lnLength = 8;
    int braceCount = 0;
    int parenCount = 0;
    int bracketCount = 0;
    lineNumbers = new short[lnLength];
    lineIndices = new int[lnLength];
    isNewSet = isSetBrace = false;
    ptNewSetModifier = 1;
    isShowScriptOutput = false;
    Vector lltoken = new Vector();
    ltoken = new Vector();
    int tokCommand = Token.nada;
    String comment = null;
    boolean endOfLine = false;
    for (; true; ichToken += cchToken) {
      int nTokens = ltoken.size();
      if (nTokens == 0 && thisFunction != null && thisFunction.chpt0 == 0)
        thisFunction.chpt0 = ichToken;
      if (lookingAtLeadingWhitespace())
        continue;
      if (lookingAtComment()) {
        if (nTokens > 0)
          continue;
        comment = script.substring(ichToken, ichToken + cchToken);
        int nChar = cchToken;
        ichCurrentCommand = ichToken;
        ichToken += cchToken;
        if ((endOfLine = lookingAtEndOfLine()) || lookingAtEndOfStatement())
          cchToken += nChar;
        ichToken = ichCurrentCommand;
      } else {
        endOfLine = lookingAtEndOfLine();
      }
      if (comment != null || endOfLine || lookingAtEndOfStatement()) {
        if (nTokens > 0 || comment != null) {
          if (nTokens == 0)
            ichCurrentCommand = ichToken;
          iCommand = lltoken.size();
          if (thisFunction != null && thisFunction.cmdpt0 < 0) {
            thisFunction.cmdpt0 = iCommand;
          }
          if (!compileCommand())
            return false;
          if (!tokAttr(tokCommand, Token.noeval)
              || atokenInfix[0].intValue <= 0) {
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
            lltoken.addElement(atokenInfix);
          }
          ltoken.setSize(0);
          nTokens = 0;
          tokCommand = Token.nada;
          tokenCommand = null;
          iHaveQuotedString = false;
          if (comment != null) {
            comment = null;
          }
        }
        if (ichToken < cchScript) {
          if (endOfLine)
            ++lineCurrent;
          if (isShowScriptOutput)
            ichCurrentCommand = ichToken + cchToken;
          continue;
        }
        break;
      }
      char ch;
      if (nTokens == 0) {
        isNewSet = isSetBrace = false;
        ptNewSetModifier = 1;
        nSemiSkip = 0;
        braceCount = 0;
        bracketCount = 0;
      } else {
        if (nTokens == ptNewSetModifier) {
          ch = script.charAt(ichToken);
          if (tokCommand == Token.set || tokAttr(tokCommand, Token.setparam)) {
            if (tokAttr(tokCommand, Token.setparam) && ch == '='
                || (isNewSet || isSetBrace)
                && (ch == '=' || ch == '[' || ch == '.')) {
              tokenCommand = (ch == '=' ? Token.tokenSet
                  : ch == '[' ? Token.tokenSetArray : Token.tokenSetProperty);
              tokCommand = Token.set;
              ltoken.insertElementAt(tokenCommand, 0);
              cchToken = 1;
              if (ch == '[')
                addTokenToPrefix(new Token(Token.leftsquare, "["));
              if (ch == '.')
                addTokenToPrefix(new Token(Token.dot, "."));
              continue;
            }
          }
        }
        if (lookingAtString()) {
          if (cchToken < 0)
            return error(ERROR_endOfCommandUnexpected);
          String str = ((tokCommand == Token.load || tokCommand == Token.script)
              && !iHaveQuotedString ? script.substring(ichToken + 1, ichToken
              + cchToken - 1) : getUnescapedStringLiteral());
          addTokenToPrefix(new Token(Token.string, str));
          iHaveQuotedString = true;
          if (tokCommand == Token.data && str.indexOf("@") < 0 && !getData(str))
            return error(ERROR_missingEnd, "data");
          continue;
        }
        if (tokCommand == Token.sync && nTokens == 1 && charToken()) {
          String ident = script.substring(ichToken, ichToken + cchToken);
          addTokenToPrefix(new Token(Token.identifier, ident));
          continue;
        }
        if (tokCommand == Token.load) {
          if (lookingAtLoadFormat()) {
            String strFormat = script.substring(ichToken, ichToken + cchToken);
            strFormat = strFormat.toLowerCase();
            if (strFormat.equals("append") || strFormat.equals("files")
                || strFormat.equals("menu") || strFormat.equals("trajectory"))
              addTokenToPrefix(new Token(Token.identifier, strFormat));
            else if (strFormat.indexOf("=") == 0) {
              addTokenToPrefix(new Token(Token.string, strFormat));
            }
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
        if (tokAttr(tokCommand, Token.specialstring)
            && !(tokCommand == Token.script && iHaveQuotedString)
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
          ch = script.charAt(ichToken);
          try {
            int seqNum = (ch == '*' || ch == '^' ? Integer.MAX_VALUE : Integer
                .parseInt(script.substring(ichToken, ichToken + cchToken - 2)));
            char insertionCode = script.charAt(ichToken + cchToken - 1);
            if (insertionCode == '^')
              insertionCode = ' ';
            int seqcode = Group.getSeqcode(seqNum, insertionCode);
            addTokenToPrefix(new Token(Token.seqcode, seqcode, "seqcode"));
            continue;
          } catch (NumberFormatException nfe) {
            return error(ERROR_invalidExpressionToken, "" + ch);
          }
        }
        if (lookingAtInteger(tokAttr(tokCommand, Token.negnums))) {
          String intString = script.substring(ichToken, ichToken + cchToken);
          int val = Integer.parseInt(intString);
          if (tokCommand == Token.breakcmd || tokCommand == Token.continuecmd) {
            if (nTokens != 1)
              return error(ERROR_badArgumentCount);
            FlowContext f = getBreakableContext(val = Math.abs(val));
            if (f == null)
              return error(ERROR_badContext, (String) tokenCommand.value);
            ((Token) ltoken.get(0)).intValue = f.pt0; //copy
          }
          addTokenToPrefix(new Token(Token.integer, val, intString));
          continue;
        }
        if (lastToken.tok == Token.select || !tokenAttr(lastToken, Token.mathfunc)) {
          // don't want to mess up x.distance({1 2 3})
          // if you want to use a bitset there, you must use 
          // bitsets properly: x.distance( ({1 2 3}) )
          boolean isBond = (script.charAt(ichToken) == '[');
          BitSet bs = lookingAtBitset();
          if (bs != null) {
            if (isBond)
              addTokenToPrefix(new Token(Token.bitset, new BondSet(bs)));
            // occasionally BondSet appears unknown in Eclipse even though it is defined
            // in Eval.java -- doesn't seem to matter.
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
          if ((token = Token.getTokenFromName(ident)) == null
              && (token = Token.getTokenFromName(ident.toLowerCase())) != null)
            token = new Token(token.tok, token.intValue, ident);
        } else {
          ident = ident.toLowerCase();
          token = Token.getTokenFromName(ident);
        }
        if (token == null) {
          if (ident.indexOf("property_") == 0)
            token = new Token(Token.property, ident.toLowerCase());
          else
            token = new Token(Token.identifier, ident);
        }
        int tok = token.tok;
        if (tok == Token.leftparen || tok == Token.leftbrace) {
          // the select() function uses dual semicolon notation
          // but we must differentiate from isosurface select(...) and set picking select
          if (nTokens > 1 && (lastToken.tok == Token.select))
              nSemiSkip += 2;
          parenCount++;
        }
        if (tok == Token.rightparen || tok == Token.rightbrace) {
          if (parenCount == 0)
            return error(ERROR_leftParenthesisExpected);
          parenCount--;
          // we need to remove the semiskip if parentheses or braces have been closed. 11.5.46
          if (parenCount == 0)
            nSemiSkip = 0;
        }
        switch (tokCommand) {
        // special cases
        case Token.nada:
          lastToken = Token.tokenOff;
          ichCurrentCommand = ichToken;
          tokenCommand = token;
          tokCommand = tok;
          if (tokAttr(tokCommand, Token.flowCommand)) {
            int pt = lltoken.size();
            boolean isEnd = false;
            boolean isNew = true;
            switch (tok) {
            case Token.end:
              if (flowContext == null)
                return error(ERROR_badContext, "end");
              isEnd = true;
              if (flowContext.token.tok != Token.function)
                token = new Token(tok, -flowContext.pt0, token.value); //copy
              break;
            case Token.ifcmd:
            case Token.forcmd:
            case Token.whilecmd:
              break;
            case Token.endifcmd:
              isEnd = true;
              if (flowContext == null || flowContext.token.tok != Token.ifcmd
                  && flowContext.token.tok != Token.elsecmd
                  && flowContext.token.tok != Token.elseif)
                return error(ERROR_badContext, "endif");
              break;
            case Token.elsecmd:
              if (flowContext == null || flowContext.token.tok != Token.ifcmd
                  && flowContext.token.tok != Token.elseif)
                return error(ERROR_badContext, "else");
              flowContext.token.intValue = flowContext.pt0 = pt;
              break;
            case Token.breakcmd:
            case Token.continuecmd:
              isNew = false;
              FlowContext f = getBreakableContext(0);
              if (f == null)
                return error(ERROR_badContext, (String) token.value);
              token = new Token(tok, f.pt0, token.value); //copy
              break;
            case Token.elseif:
              if (flowContext == null || flowContext.token.tok != Token.ifcmd
                  && flowContext.token.tok != Token.elseif
                  && flowContext.token.tok != Token.elsecmd)
                return error(ERROR_badContext, "elseif");
              flowContext.token.intValue = flowContext.pt0 = pt;
              break;
            case Token.function:
              if (flowContext != null)
                return error(ERROR_badContext, "function");
              break;
            }
            if (isEnd) {
              flowContext.token.intValue = pt;
              if (tok == Token.endifcmd)
                flowContext = flowContext.parent;
            } else if (isNew) {
              token = new Token(tok, token.value); //copy
              if (tok == Token.elsecmd || tok == Token.elseif)
                flowContext.token = token;
              else
                flowContext = new FlowContext(token, pt, flowContext);
            }
            break;
          }
          if (tokAttr(tokCommand, Token.command))
            break;
          if (!tokAttr(tok, Token.identifier) && !tokAttr(tok, Token.setparam))
            return commandExpected();
          tokCommand = Token.set;
          isSetBrace = (tok == Token.leftbrace);
          isNewSet = !isSetBrace;
          braceCount = (isSetBrace ? 1 : 0);
          bracketCount = 0;
          ptNewSetModifier = (isNewSet ? 1 : Integer.MAX_VALUE);
          break;
        case Token.function:
          if (tokenCommand.intValue == 0) {
            if (nTokens != 1)
              break;
            // user has given macro command
            tokenCommand.value = ident;
            continue;
          }
          if (nTokens == 1) {
            flowContext.setFunction(thisFunction = new Function(ident));
            break;
          }
          if (nTokens == 2) {
            if (tok != Token.leftparen)
              return error(ERROR_leftParenthesisExpected);
            break;
          }
          if (nTokens == 3 && tok == Token.rightparen)
            break;
          if (nTokens % 2 == 0) {
            if (tok != Token.comma && tok != Token.rightparen)
              return error(ERROR_commaOrCloseExpected);
            break;
          }
          thisFunction.addVariable(ident, true);
          break;
        case Token.elsecmd:
          if (nTokens != 1 || tok != Token.ifcmd)
            return error(ERROR_badArgumentCount);
          ltoken.removeElementAt(0);
          ltoken.addElement(token = new Token(Token.elseif, "elseif"));
          flowContext.token = token;
          tokCommand = Token.elseif;
          continue;
        case Token.var:
          if (nTokens != 1)
            break;
          addContextVariable(ident);
          ltoken.removeElementAt(0);
          ltoken.addElement(Token.tokenSetVar);
          tokCommand = Token.set;
          break;
        case Token.end:
          if (nTokens != 1)
            return error(ERROR_badArgumentCount);
          if (flowContext == null || flowContext.token.tok != tok)
            if (tok != Token.ifcmd || flowContext.token.tok != Token.elsecmd
                && flowContext.token.tok != Token.elseif)
              return error(ERROR_badContext, "end " + ident);
          switch (tok) {
          case Token.ifcmd:
          case Token.forcmd:
          case Token.whilecmd:
            break;
          case Token.function:
            if (!isCheckOnly)
              (thisFunction.name.indexOf("_") == 0 ? localFunctions
                  : globalFunctions).put(thisFunction.name, thisFunction);
            flowContext.setFunction(script, ichCurrentCommand, lltoken.size(),
                lineNumbers, lineIndices, lltoken);
            thisFunction = null;
            tokenCommand.intValue = Integer.MAX_VALUE; // don't include this one
            flowContext = flowContext.parent;
            continue;
          default:
            return error(ERROR_unrecognizedToken, "end " + ident);
          }
          flowContext = flowContext.parent;
          break;
        case Token.forcmd:
          if (nTokens == 1) {
            if (tok != Token.leftparen)
              return error(ERROR_unrecognizedToken, ident);
            nSemiSkip += 2;
          } else if (nTokens == 3 && ((Token) ltoken.get(2)).tok == Token.var)
            addContextVariable(ident);
          break;
        case Token.set:
          if (tok == Token.leftbrace)
            braceCount++;
          else if (tok == Token.rightbrace) {
            braceCount--;
            if (isSetBrace && braceCount == 0
                && ptNewSetModifier == Integer.MAX_VALUE)
              ptNewSetModifier = nTokens + 1;
          }
          if (nTokens == ptNewSetModifier) { // 1 when { is not present
            boolean isSetArray = false;
            if (tok == Token.opEQ || tok == Token.leftsquare) {
              // x =   or   x[n] = 
              token = (Token) ltoken.get(0);
              ltoken.removeElementAt(0);
              isSetArray = (tok == Token.leftsquare);
              ltoken.addElement(isSetArray ? Token.tokenSetArray
                  : Token.tokenSet);
              tok = token.tok;
              tokCommand = Token.set;
            }
            if (tok == Token.leftparen) {
              // mysub(xxx,xxx,xxx)
              token = (Token) ltoken.get(0);
              ltoken.removeElementAt(0);
              tokenCommand = new Token(Token.function, 0, token.value);
              ltoken.add(0, tokenCommand);
              tokCommand = Token.function;
              token = Token.tokenLeftParen;
              tok = Token.leftparen;
              break;
            }
            if (tok != Token.identifier && (!tokAttr(tok, Token.setparam)))
              return isNewSet ? commandExpected() : error(
                  ERROR_unrecognizedParameter, "SET", ": " + ident);
            if (isSetArray) {
              addTokenToPrefix(token);
              token = Token.tokenArraySelector;
              tok = Token.leftsquare;
            }
          }
          break;
        case Token.define:
          if (nTokens == 1) {
            // we are looking at the variable name
            if (tok != Token.identifier) {
              if (preDefining) {
                if (!tokAttr(tok, Token.predefinedset))
                  return error("ERROR IN Token.java or JmolConstants.java -- the following term was used in JmolConstants.java but not listed as predefinedset in Token.java: "
                      + ident);
              } else if (tokAttr(tok, Token.predefinedset)) {
                Logger
                    .warn("WARNING: predefined term '"
                        + ident
                        + "' has been redefined by the user until the next file load.");
              } else if (!isCheckOnly && ident.length() > 1) {
                Logger
                    .warn("WARNING: redefining "
                        + ident
                        + "; was "
                        + token
                        + "not all commands may continue to be functional for the life of the applet!");
                tok = token.tok = Token.identifier;
                Token.addToken(ident, token);
              }
            }
          } else if (nTokens == 2 && tok == Token.opEQ) {
            // we are looking at @x =.... just insert a SET command
            // and ignore the =. It's the same as set @x ... 
            ltoken.insertElementAt(Token.tokenSet, 0);
            continue;
          } else {
            // we are looking at the expression
            if (tok != Token.identifier && tok != Token.set
                && !(tokAttr(tok, Token.expression)))
              return error(ERROR_invalidExpressionToken, ident);
          }
          break;
        case Token.center:
          if (tok != Token.identifier && tok != Token.dollarsign
              && !tokAttr(tok, Token.expression))
            return error(ERROR_invalidExpressionToken, ident);
          break;
        case Token.restrict:
        case Token.select:
        case Token.display:
        case Token.hide:
        case Token.delete:
          if (tok == Token.leftsquare)
            bracketCount++;
          if (tok == Token.rightsquare)
            bracketCount--;
          //ensure anything goes for inside brackets 
          if (tok != Token.identifier && !tokAttr(tok, Token.expression)
              && tok != Token.min && tok != Token.max && bracketCount == 0)
            return error(ERROR_invalidExpressionToken, ident);
          break;
        }
        addTokenToPrefix(token);
        continue;
      }
      if (nTokens == 0 || (isNewSet || isSetBrace)
          && nTokens == ptNewSetModifier)
        return commandExpected();
      return error(ERROR_unrecognizedToken, script.substring(ichToken,
          ichToken + 1));
    }
    aatokenCompiled = new Token[lltoken.size()][];
    lltoken.copyInto(aatokenCompiled);
    if (flowContext != null)
      return error(ERROR_missingEnd, Token.nameOf(flowContext.token.tok));
    return true;
  }

  private FlowContext getBreakableContext(int nLevelsUp) {
    FlowContext f = flowContext;
    while (f != null && (f.token.tok != Token.forcmd
      && f.token.tok != Token.whilecmd || nLevelsUp-- > 0))
      f = f.parent;
    return f;
  }
  
  private boolean getData(String key) {
    ichToken += key.length() + 2;
    if (script.length() > ichToken && script.charAt(ichToken) == '\r')
      ichToken++;
    if (script.length() > ichToken && script.charAt(ichToken) == '\n')
      ichToken++;
    int i = script.indexOf("\"" + key + "\"", ichToken) - 4;
    if (i < 0 || !script.substring(i, i + 4).equalsIgnoreCase("END "))
      return false;
    String str = script.substring(ichToken, i);
    addTokenToPrefix(new Token(Token.data, str));
    addTokenToPrefix(new Token(Token.identifier, "end"));
    addTokenToPrefix(new Token(Token.string, key));
    cchToken = i - ichToken + key.length() + 6;
    return true;
  }

  private static boolean isSpaceOrTab(char ch) {
    return ch == ' ' || ch == '\t';
  }

  private boolean eol(char ch) {
    return (ch == '\r' || ch == '\n' || ch == ';' && nSemiSkip <= 0);  
  }
  
  private boolean lookingAtLeadingWhitespace() {
    int ichT = ichToken;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    if (isLineContinuation(ichT))
      ichT += 2;
    cchToken = ichT - ichToken;
    return cchToken > 0;
  }

  private boolean isLineContinuation(int ichT) {
    char ch;
    return (ichT < cchScript - 1 
        && script.charAt(ichT) == '\\'
       && ((ch = script.charAt(ichT + 1)) == '\r' 
         || ch == '\n'));
  }

  private boolean isShowCommand;
  
  private boolean lookingAtComment() {
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
    if (ichToken == ichCurrentCommand && ichToken < cchScript 
        && script.charAt(ichToken) == '$') {
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

    for (; ichEnd < cchScript && !eol(ch = script.charAt(ichEnd)); ichEnd++) {
      if (isLineContinuation(ichEnd)) {
        ichEnd++;
        continue;
      }
      if (ch == '#' && ichFirstSharp == -1)
        ichFirstSharp = ichEnd;
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
    if (ichToken == cchScript 
        || !(script.charAt(ichToken) == ';' && nSemiSkip-- <= 0))
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
    return sb.toString();
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

  static String[] loadFormats = { "append", "files", "trajectory", "menu",
    /*ancient:*/ "alchemy", "mol2", "mopac", "nmrpdb", "charmm", "xyz", "mdl", "pdb" };

  private boolean lookingAtLoadFormat() {
    int ichT;
    String match = script
        .substring(ichToken, Math.min(cchScript, ichToken + 10)).toLowerCase();
    for (int i = loadFormats.length; --i >= 0;) {
      String strFormat = loadFormats[i];
      int cchFormat = strFormat.length();
      if (match.indexOf(strFormat) == 0 && (ichT = ichToken + cchFormat) < cchScript && isSpaceOrTab(script.charAt(ichT))) {
        cchToken = cchFormat;
        return true;
      }
    }
    return false;
  }

  private boolean lookingAtSpecialString() {
    int ichT = ichToken;
    while (ichT < cchScript && !eol(script.charAt(ichT)))
      ++ichT;
    if (ichT > ichToken && script.charAt(ichToken) == '@'
        && (ichT <= ichToken + 1 || script.charAt(ichToken + 1) != '{'))
      return false;
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
    if (ichToken + 4 > cchScript 
        || script.charAt(ichToken + 1) != '{'
      ||(script.charAt(ichToken) != '(' 
        && script.charAt(ichToken) != '['))
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
    case ';':
    case '@':
    case '.':
    case '%':
    case '[':
    case ']':
    case '\\':  // leftdivide
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
    case '\'':
    case '?': // include question marks in identifier for atom expressions
      while (ichT < cchScript
          && (Character.isLetterOrDigit(ch = script.charAt(ichT)) 
              || ch == '_' || ch == '?' || ch == '~' || ch == '\'')
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

  private boolean charToken() {
    char ch;
    if (ichToken == cchScript || (ch = script.charAt(ichToken)) == '"' || ch == '@')
      return false;
    int ichT = ichToken;
    while (ichT < cchScript && !isSpaceOrTab(ch = script.charAt(ichT)) && ch != '#' && !eol(ch))
        ++ichT;
    cchToken = ichT - ichToken;
    return true;
  }

  boolean isImplicitExpression;
  boolean isSetOrDefine;
  Token tokenCommand;
  int tokCommand;

  Vector ltokenPostfix = null;
  Token[] atokenInfix;
  int itokenInfix;
  boolean isEmbeddedExpression;
  boolean isCommaAsOrAllowed;
  
  private boolean compileCommand() {
    if (ltoken.size() == 0) {
      // comment
      atokenInfix = new Token[0];
      ltoken.copyInto(atokenInfix);
      return true;
    }
    tokenCommand = (Token) ltoken.firstElement();
    tokCommand = tokenCommand.tok;
    isImplicitExpression = tokAttr(tokCommand, Token.implicitExpression);
    isSetOrDefine = (tokCommand == Token.set || tokCommand == Token.define);
    isCommaAsOrAllowed = tokAttr(tokCommand, Token.expressionCommand);
    int size = ltoken.size();
    if (size == 1 && !tokAttr(tokCommand, Token.flowCommand) 
        && tokenCommand.intValue != Integer.MAX_VALUE
        && tokAttr(tokenCommand.intValue, Token.onDefault1))
      addTokenToPrefix(Token.tokenOn);
    atokenInfix = new Token[ltoken.size()];
    ltoken.copyInto(atokenInfix);
    if (logMessages) {
      for (int i = 0; i < atokenInfix.length; i++)
        Logger.debug(i + ": " + atokenInfix[i]);
    }

    //compile color parameters

    if (tokAttr(tokCommand, Token.colorparam) && !compileColorParam())
      return false;

    //compile expressions

    isEmbeddedExpression = tokCommand != Token.function && (!tokAttrOr(tokCommand, Token.expressionCommand, Token.specialstring));
    boolean checkExpression = isEmbeddedExpression || (tokAttr(tokCommand, Token.expressionCommand));

      // $ at beginning disallow expression checking for center command
    if (tokCommand == Token.center && tokAt(1) == Token.dollarsign)
      checkExpression = false;

    if (checkExpression && !compileExpression())
      return false;

    //check statement length

    size = atokenInfix.length;

    int nDefined = 0;
    for (int i = 1; i < size; i++) {
      if (tokAt(i) == Token.define)
        nDefined++;
    }

    size -= nDefined;
    if (isNewSet) {
      if (size == 1) {
        atokenInfix[0] = new Token(Token.function, 0, atokenInfix[0].value);
        isNewSet = false;
      }
    }

    if ((isNewSet || isSetBrace) && size < ptNewSetModifier + 2)
      return commandExpected();
    if (isSetOrDefine || tokAttrOr(tokenCommand.tok, Token.noeval, Token.flowCommand)) //intValue is NOT of this nature
      return true;
    int allowedLen = (tokenCommand.intValue & 0x0F) + 1;
    if (!tokAttr(tokenCommand.intValue, Token.varArgCount)) {
      if (size > allowedLen)
        return error(ERROR_badArgumentCount);
      if (size < allowedLen)
        return error(ERROR_endOfCommandUnexpected);
    } else if (allowedLen > 1 && size > allowedLen) {
      // max2, max3, max4, etc.
      return error(ERROR_badArgumentCount);
    }
    return true;
  }

  private boolean compileExpression() {
    int firstToken = (isSetOrDefine && !isSetBrace ? 2 : 1);
    ltokenPostfix = new Vector();
    itokenInfix = 0;
    Token tokenBegin = null;
    for (int i = 0; i < firstToken && addNextToken(); i++) {
    }
    while (moreTokens()) {
      if (isEmbeddedExpression) {
        while (!isExpressionNext() && addNextToken()) {
        }
        if (!moreTokens())
          break;
      }
      if (lastToken.tok == Token.define) {
        if (!clauseDefine())
          return false;
        continue;
      }
      if (!isImplicitExpression)
        addTokenToPostfix(tokenBegin = new Token(Token.expressionBegin, "implicitExpressionBegin"));
      if (!clauseOr(isCommaAsOrAllowed || !isImplicitExpression
          && tokPeek(Token.leftparen)))
        return false;
      if (!isImplicitExpression
          && !(isEmbeddedExpression && lastToken == Token.tokenCoordinateEnd)) {
        addTokenToPostfix(Token.tokenExpressionEnd);
      }
      if (moreTokens()) {
        if (tokCommand != Token.select && !isEmbeddedExpression)
          return error(ERROR_endOfExpressionExpected);
        if (tokCommand == Token.select) {
          // advanced select, with two expressions, the first
          // being an atom expression; the second being a property selector expression
          tokenBegin.intValue = 0;
          tokCommand = Token.nada;
          isEmbeddedExpression = true;
          isImplicitExpression = true;
          isCommaAsOrAllowed = false;
        }
      }
    }
    atokenInfix = new Token[ltokenPostfix.size()];
    ltokenPostfix.copyInto(atokenInfix);
    return true;
  }

  private boolean isExpressionNext() {
    return tokPeek(Token.leftbrace) || !isImplicitExpression && tokPeek(Token.leftparen);
  }

  private static boolean tokenAttr(Token token, int tok) {
    return token != null && (token.tok & tok) == tok;
  }
  
  private boolean moreTokens() {
    return (itokenInfix < atokenInfix.length);
  }
  
  private int tokAt(int i) {
    return (i < atokenInfix.length ? atokenInfix[i].tok : Token.nada);
  }
  
  private int tokPeek() {
    if (itokenInfix >= atokenInfix.length)
      return Token.nada;
    return atokenInfix[itokenInfix].tok;
  }

  private boolean tokPeek(int tok) {
    if (itokenInfix >= atokenInfix.length)
      return false;
    return (atokenInfix[itokenInfix].tok == tok);
  }

  private int intPeek() {
    if (itokenInfix >= atokenInfix.length)
      return Integer.MAX_VALUE;
    return atokenInfix[itokenInfix].intValue;    
  }
  
  private Object valuePeek() {
    if (moreTokens())
      return atokenInfix[itokenInfix].value;
    return "";
  }
 
  /**
   * increments the pointer; does NOT set theToken or theValue
   * @return the next token
   */
  private Token tokenNext() {
    if (itokenInfix >= atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix++];
  }
  
  private boolean tokenNext(int tok) {
    Token token = tokenNext();
    return (token != null && token.tok == tok);
  }

  private boolean returnToken() {
    itokenInfix--;
    return false;
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

  private boolean addTokenToPostfix(int tok, Object value) {
    return addTokenToPostfix(new Token(tok, value));
  }

  private boolean addTokenToPostfix(int tok, int intValue, Object value) {
    return addTokenToPostfix(new Token(tok, intValue, value));
  }

  private boolean addTokenToPostfix(Token token) {
    if (token == null)
      return false;
    if (logMessages)
      log("addTokenToPostfix" + token);
    ltokenPostfix.addElement(token);
    lastToken = token;
    return true;
  }

  private boolean addNextToken() {
    return addTokenToPostfix(tokenNext());
  }
  
  private boolean addNextTokenIf(int tok) {
    return (tokPeek(tok) && addNextToken());
  }
  
  private boolean addSubstituteTokenIf(int tok, Token token) {
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
        addSubstituteTokenIf(Token.comma, Token.tokenOr);
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
    case Token.minus: // selecting a negative residue spec
    case Token.seqcode:
    case Token.times:
    case Token.leftsquare:
    case Token.identifier:
    case Token.colon:
    case Token.percent:
      if (clauseResidueSpec())
        return true;
      //fall through for identifier specifically
    default:
      if (tokAttrOr(tok, Token.property,  Token.atomproperty))
        return clauseComparator();
      if (!tokAttr(tok, Token.predefinedset))
        break;
    // fall into the code and below and just add the token
    case Token.all:
    case Token.none:
    case Token.isaromatic:
    case Token.bitset:
      return addNextToken();
    case Token.integer:
      if (clauseResidueSpec())
        return true;
    case Token.divide:
      addNextToken();
      return true;
    case Token.string:
      haveString = true;
      return addNextToken();
    case Token.define:
      addNextToken();
      switch (tokPeek()) {
      case Token.nada:
        break;
      default:
        return clauseDefine();
      }
      //fall through
      case Token.nada:
        return error(ERROR_endOfCommandUnexpected);
    case Token.bonds:
    case Token.monitor:
      addNextToken();
      if (tokPeek(Token.bitset))
        addNextToken();
      else if (tokPeek(Token.define)) {
        addNextToken();
        return clauseDefine();
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
      return addTokenToPostfix(Token.spec_model2, getToken().intValue, theValue);
    case Token.leftparen:
      addNextToken();
      if (!clauseOr(true))
        return false;
      if (!addNextTokenIf(Token.rightparen))
        return error(ERROR_rightParenthesisExpected);
      return checkForItemSelector();
    case Token.leftbrace:
      return checkForCoordinate(isImplicitExpression);
    }
    return error(ERROR_unrecognizedExpressionToken, "" + valuePeek());
  }

  private boolean checkForCoordinate(boolean isImplicitExpression) {
    /*
     * A bit tricky here: we have three contexts for braces -- 
     * 
     * 1) expressionCommands SELECT, RESTRICT, DEFINE, 
     *    DISPLAY, HIDE, CENTER, and SUBSET
     * 
     * 2) embeddedExpression commands such as DRAW and ISOSURFACE
     * 
     * 3) IF and SET
     * 
     * Then, within these, we have the possibility that we are 
     * looking at a coordinate {0 0 0} (with or without commas, and
     * possibly fractional, {1/2 1/2 1}, and possibly a plane Point4f
     * definition, {a b c d}) or an expression. 
     * 
     * We assume an expression initially and then adjust accordingly
     * if it turns out this is a coordinate. 
     * 
     * Note that due to tha nuances of how expressions such as (1-4) are
     * reported as special codes, Eval must still intepret these
     * carefully. This could be corrected for here, I think.
     * 
     */
    boolean isCoordinate = false;
    int pt = ltokenPostfix.size();
    if (isImplicitExpression) {
      addTokenToPostfix(Token.tokenExpressionBegin);
      tokenNext();
    }else if (isEmbeddedExpression) {
      tokenNext();
      pt--;
    } else {
      addNextToken();
    }
    if (!clauseOr(false))
      return false;
    int n = 1;
    while (!tokPeek(Token.rightbrace)) {
        boolean haveComma = addNextTokenIf(Token.comma);
        if (!clauseOr(false))
          return (haveComma || n < 3? false : error(ERROR_rightBraceExpected));
        n++;
    }
    isCoordinate = (n >= 2); // could be {1 -2 3}
    if (isCoordinate && (isImplicitExpression || isEmbeddedExpression)) {
      ltokenPostfix.set(pt, Token.tokenCoordinateBegin);
      addTokenToPostfix(Token.tokenCoordinateEnd);
      tokenNext();
    } else if (isImplicitExpression) {
      addTokenToPostfix(Token.tokenExpressionEnd);
      tokenNext();
    } else if (isEmbeddedExpression)
      tokenNext();
    else
      addNextToken();
    return checkForItemSelector();
  }
  
  private boolean checkForItemSelector() {
    // {x[1]}  @{x}[1][3]  (atomno=3)[2][5]
    for (int i = 0; i < 2; i++) {
      if (!addNextTokenIf(Token.leftsquare))
        break;
      if (!clauseItemSelector())
        return false;
      if (!addNextTokenIf(Token.rightsquare))
        return error(ERROR_rightBracketExpected);
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
    float distance = Float.MAX_VALUE;
    String key = null;
    boolean allowComma = true;
    switch (theToken.tok) {
    case Token.minus:
      if (getToken() == null)
        return false;
      if (theToken.tok != Token.integer)
        return error(ERROR_numberExpected);
      distance = -theToken.intValue;
      break;
    case Token.integer:
    case Token.decimal:
      distance = floatValue();
      break;
    case Token.branch:
      allowComma = false;
      //fall through
    case Token.boundbox:
    case Token.chain:
    case Token.coord:
    case Token.element:
    case Token.group:
    case Token.model:
    case Token.molecule:
    case Token.plane:
    case Token.site:
    case Token.string:
    case Token.structure:
      key = (String) theValue;
      break;
    case Token.identifier:
      key = ((String) theValue).toLowerCase();
      break;
    default:
      return error(ERROR_unrecognizedParameter,"WITHIN", ": " + theToken.value);
    }
    if (key == null)
      addTokenToPostfix(Token.decimal, new Float(distance));
    else
      addTokenToPostfix(Token.string, key);

    while (true) {
      if (!addNextTokenIf(Token.comma))
        break;
      int tok = tokPeek();
      if (distance != Float.MAX_VALUE && (tok == Token.on || tok == Token.off)) {
        addTokenToPostfix(getToken());
        if (!addNextTokenIf(Token.comma))
          break;
        tok = tokPeek();
      }
      boolean isCoordOrPlane = false;
       if (key == null) {
        if (tok == Token.identifier) {
          //distance was specified, but to what?
          getToken();
          key = ((String) theValue).toLowerCase();
          if (key.equals("hkl")) {
            isCoordOrPlane = true;
            addTokenToPostfix(Token.string, key);
          } else {
            returnToken();
          }
        } else if (tok == Token.coord || tok == Token.plane) {
          isCoordOrPlane = true;
          addNextToken();
        } else if (tok == Token.leftbrace) {
          returnToken();
          isCoordOrPlane = true;
          addTokenToPostfix(Token
              .getTokenFromName(distance == Float.MAX_VALUE ? "plane" : "coord"));
        }
        addNextTokenIf(Token.comma);
      }
      tok = tokPeek();
      if (isCoordOrPlane) {
        while (!tokPeek(Token.rightparen)) {
          switch (tokPeek()) {
          case Token.nada:
            return error(ERROR_endOfCommandUnexpected);
          case Token.leftparen:
            addTokenToPostfix(Token.tokenExpressionBegin);
            addNextToken();
            if (!clauseOr(false))
              return error(ERROR_unrecognizedParameter,"WITHIN", ": ?");
            if (!addNextTokenIf(Token.rightparen))
              return error(ERROR_commaOrCloseExpected);
            addTokenToPostfix(Token.tokenExpressionEnd);
            break;
          case Token.define:
            addTokenToPostfix(getToken());
            if (!clauseDefine())
              return false;
            break;
          default:
            addTokenToPostfix(getToken());
          }
        }
      } else if (!clauseOr(allowComma)) {// *expression*
        return error(ERROR_endOfCommandUnexpected);
      }
    }
    if (!addNextTokenIf(Token.rightparen))
      return error(ERROR_rightParenthesisExpected);
    return true;
  }

  private boolean clauseConnected() {
    // connected (1,3, single, .....)
    if (!addNextTokenIf(Token.leftparen)) {
      addTokenToPostfix(Token.tokenLeftParen);
      addTokenToPostfix(Token.tokenRightParen);
      return true;
    }
    while (true) {
      if (addNextTokenIf(Token.integer))
        if (!addNextTokenIf(Token.comma))
          break;
      if (addNextTokenIf(Token.integer))
        if (!addNextTokenIf(Token.comma))
          break;
      if (addNextTokenIf(Token.decimal))
        if (!addNextTokenIf(Token.comma))
          break;
      if (addNextTokenIf(Token.decimal))
        if (!addNextTokenIf(Token.comma))
          break;
      if (tokPeek() == Token.identifier || tokPeek() == Token.hbond) {
        String strOrder = (String) getToken().value;
        int intType = JmolConstants.getBondOrderFromString(strOrder);
        if (intType == JmolConstants.BOND_ORDER_NULL) {
          returnToken();
        } else {
          addTokenToPostfix(Token.string, strOrder);
          if (!addNextTokenIf(Token.comma))
            break;
        }
      }
      if (addNextTokenIf(Token.rightparen))
        return true;
      if (!clauseOr(tokPeek(Token.leftparen))) // *expression*
        return false;
      if (addNextTokenIf(Token.rightparen))
        return true;
      if (!addNextTokenIf(Token.comma))
        return false;
      if (!clauseOr(tokPeek(Token.leftparen))) // *expression*
        return false;

      break;
    }
    if (!addNextTokenIf(Token.rightparen))
      return error(ERROR_rightParenthesisExpected);
    return true;
  }

  private boolean clauseSubstructure() {
    if (!addNextTokenIf(Token.leftparen))
      return false;
    if (!addNextTokenIf(Token.string))
      return error(ERROR_stringExpected);
    if (!addNextTokenIf(Token.rightparen))
      return error(ERROR_rightParenthesisExpected);
    return true;
  }

  private boolean clauseItemSelector() {
    int tok;
    while ((tok = tokPeek()) != Token.nada && tok != Token.rightsquare) {
      if (!clauseOr(false))
        return false;
      returnToken();
      if (tokPeek() != Token.times)
        tokenNext();
      tok = tokPeek();
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
      return error(ERROR_comparisonOperatorExpected);
    if (getToken() == null)
      return error(ERROR_unrecognizedExpressionToken, "" + valuePeek());
    boolean isNegative = (isToken(Token.minus));
    if (isNegative && getToken() == null)
      return error(ERROR_numberExpected);
    switch (theToken.tok) {
    case Token.integer:
    case Token.decimal:
    case Token.identifier:
    case Token.string:
    case Token.leftbrace:
    case Token.define:
      break;
    default:
      return error(ERROR_numberOrVariableNameExpected);
    }
    addTokenToPostfix(tokenComparator.tok, tokenAtomProperty.tok,
        tokenComparator.value + (isNegative ? " -" : ""));
    if (tokenAtomProperty.tok == Token.property)
      addTokenToPostfix(tokenAtomProperty);
    if (isToken(Token.leftbrace)) {
      returnToken();
      return clausePrimitive();
    }
    addTokenToPostfix(theToken);
    if (theToken.tok == Token.define)
      return clauseDefine();
    return true;
  }

  private boolean clauseCell() {
    Point3f cell = new Point3f();
    tokenNext(); // CELL
    if (!tokenNext(Token.opEQ)) // =
      return error(ERROR_equalSignExpected);
    if (getToken() == null)
      return error(ERROR_coordinateExpected);
    // 555 = {1 1 1}
    //Token coord = tokenNext(); // 555 == {1 1 1}
    if (isToken(Token.integer)) {
      int nnn = theToken.intValue;
      cell.x = nnn / 100 - 4;
      cell.y = (nnn % 100) / 10 - 4;
      cell.z = (nnn % 10) - 4;
      return addTokenToPostfix(Token.cell, cell);
    }
    if (!isToken(Token.leftbrace) || !getNumericalToken())
      return error(ERROR_coordinateExpected); // i
    cell.x = floatValue();
    if (tokPeek(Token.comma)) // ,
      tokenNext();
    if (!getNumericalToken())
      return error(ERROR_coordinateExpected); // j
    cell.y = floatValue();
    if (tokPeek(Token.comma)) // ,
      tokenNext();
    if (!getNumericalToken() || !tokenNext(Token.rightbrace))
      return error(ERROR_coordinateExpected); // k
    cell.z = floatValue();
    return addTokenToPostfix(Token.cell, cell);
  }

  private boolean clauseDefine() {
    // we allow @x[1], which compiles as {@x}[1], not @{x[1]}
    // otherwise [1] gets read as a general atom name selector
    if (!addSubstituteTokenIf(Token.leftbrace, Token.tokenExpressionBegin))
      return addNextToken() && checkForItemSelector();
    while (moreTokens() && !tokPeek(Token.rightbrace)) {
      if (tokPeek(Token.leftbrace)) {
        if (!checkForCoordinate(true))
          return false;
      } else {
        addNextToken();
      }
    }
    return addSubstituteTokenIf(Token.rightbrace, Token.tokenExpressionEnd)
        && checkForItemSelector();
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
    if (tok == Token.times || tok == Token.leftsquare
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
    if (tok == Token.times || tok == Token.minus || tok == Token.integer
        || tok == Token.seqcode) {
      wasInteger = (tok == Token.integer);
      if (!clauseResNumSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == Token.colon || tok == Token.times || tok == Token.identifier
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
    if (tok == Token.colon || tok == Token.divide) {
      if (!clauseModelSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (!specSeen)
      return error(ERROR_residueSpecificationExpected);
    if (!residueSpecCodeGenerated) {
      // nobody generated any code, so everybody was a * (or equivalent)
      addTokenToPostfix(Token.tokenAll);
    }
    return true;
  }

  private boolean clauseResNameSpec() {
    getToken();
    if (isToken(Token.times) || isToken(Token.nada))
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
        return error(ERROR_residueSpecificationExpected);
      strSpec = strSpec.toUpperCase();
      return generateResidueSpecCode(new Token(Token.spec_name_pattern, strSpec));
    }

    // no [ ]:

    if (!isToken(Token.identifier))
      return error(ERROR_identifierOrResidueSpecificationExpected);
    //check for a * in the next token, which
    //would indicate this must be a name with wildcard

    if (tokPeek(Token.times)) {
      String res = theValue + "*";
      getToken();
      return generateResidueSpecCode(new Token(Token.identifier, res));
    }
    return generateResidueSpecCode(theToken);
  }

  private boolean clauseResNumSpec() {
    log("clauseResNumSpec()");
    if (tokPeek(Token.times))
      return (getToken() != null);
    return clauseSequenceRange();
  }

  private boolean clauseSequenceRange() {
    Token seqToken = getSequenceCode(false);
    if (seqToken == null)
      return false;
    int tok = tokPeek();
    if (tok == Token.minus || tok == Token.integer && intPeek() < 0) {
      if (tok == Token.minus) {
        tokenNext();
      } else if (tokPeek() == Token.integer && intPeek() < 0) {
         // hyphen masquerading as neg int
          int i = -intPeek();
          tokenNext().intValue = i;
          returnToken();
      }
      seqToken.tok = Token.spec_seqcode_range;
      generateResidueSpecCode(seqToken);
      seqToken = getSequenceCode(true);
      return addTokenToPostfix(seqToken);
    }
    return generateResidueSpecCode(seqToken);
  }

  private Token getSequenceCode(boolean isSecond) {
    // problem is that some commands, like zoomTo allow negative numbers,
    // while other, like center, do not.
    // 
    // (25 [-] 35)     ==> 25 - 35
    // (25 -35)        ==> 25 - 35
    
    // (25 [-] [-] 35) ==> 25 - -35
    // (25 [-] -35)    ==> 25 - -35
    
    // ([-] 25 [-] 35) ==> -25 - 35
    // (-25 -35)       ==> -25 - 35
    
    boolean negative = false;
    int seqcode = Integer.MAX_VALUE;
    int seqvalue = Integer.MAX_VALUE;
    int tokPeek = tokPeek();
    if (tokPeek == Token.minus) {
      tokenNext();
      tokPeek = tokPeek();
      negative = true;
    }
    if (tokPeek == Token.seqcode)
      seqcode = tokenNext().intValue * (negative ? -1 : 1);
    else if (tokPeek == Token.integer)
      seqvalue = tokenNext().intValue * (negative ? -1 : 1);
    else if (!isSecond){
      if (negative)
        returnToken();
      return null;
    }
    return new Token(Token.spec_seqcode, seqvalue, new Integer(seqcode));
  }

  private boolean clauseChainSpec(int tok) {
    if (tok == Token.colon) {
      tokenNext();
      tok = tokPeek();
      if (isSpecTerminator(tok))
        return generateResidueSpecCode(new Token(Token.spec_chain, '\0',
            "spec_chain"));
    }
    if (tok == Token.times)
      return (getToken() != null);
    char chain;
    switch (tok) {
    case Token.integer:
      getToken();
      int val = theToken.intValue;
      if (val < 0 || val > 9)
        return error(ERROR_invalidChainSpecification);
      chain = (char) ('0' + val);
      break;
    case Token.identifier:
      String strChain = (String) getToken().value;
      if (strChain.length() != 1)
        return error(ERROR_invalidChainSpecification);
      chain = strChain.charAt(0);
      if (chain == '?')
        return true;
      break;
    default:
      return error(ERROR_invalidChainSpecification);
    }
    return generateResidueSpecCode(new Token(Token.spec_chain, chain,
        "spec_chain"));
  }

  private boolean isSpecTerminator(int tok) {
    switch (tok) {
    case Token.nada:
    case Token.divide:
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
    case Token.times:
    case Token.string:
    case Token.integer:
    case Token.identifier:
      break;
    default:
      return error(ERROR_invalidModelSpecification);
    }
    //Logger.debug("alternate specification seen:" + alternate);
    return generateResidueSpecCode(new Token(Token.spec_alternate, alternate));
  }

  private boolean clauseModelSpec() {
    getToken();
    if (tokPeek(Token.times)) {
      getToken();
      return true;
    }
    switch (tokPeek()) {
    case Token.integer:
      return generateResidueSpecCode(new Token(Token.spec_model, new Integer(
          getToken().intValue)));
    case Token.decimal:
            return generateResidueSpecCode(new Token(Token.spec_model,
          getToken().intValue, theValue));
    case Token.comma:
    case Token.rightbrace:
    case Token.nada:
      return generateResidueSpecCode(new Token(Token.spec_model, new Integer(1)));
    }
    return error(ERROR_invalidModelSpecification);
  }

  private boolean clauseAtomSpec() {
    if (!tokenNext(Token.dot))
      return error(ERROR_invalidAtomSpecification);
    if (getToken() == null)
      return true;
    String atomSpec = "";
    if (isToken(Token.integer)) {
      atomSpec += "" + theToken.intValue;
      if (getToken() == null)
        return error(ERROR_invalidAtomSpecification);
    }
    switch (theToken.tok) {
    case Token.times:
      return true;
    case Token.identifier:
      break;
    default:
      return error(ERROR_invalidAtomSpecification);
    }
    atomSpec += theValue;
    if (tokPeek(Token.times)) {
      tokenNext();
      // this one is a '*' as a prime, not a wildcard
      atomSpec += "*";
    }
    return generateResidueSpecCode(new Token(Token.spec_atom, atomSpec));
  }

  private boolean compileColorParam() {
    for (int i = 1; i < atokenInfix.length; ++i) {
      theToken = atokenInfix[i];
      //Logger.debug(token + " atokenInfix: " + atokenInfix.length);
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

  private final static int ERROR_badArgumentCount  = 0;
  private final static int ERROR_badContext  = 1;
  private final static int ERROR_commandExpected = 2;
  private final static int ERROR_commaOrCloseExpected  = 3;
  private final static int ERROR_comparisonOperatorExpected  = 4;
  private final static int ERROR_coordinateExpected  = 5;
  private final static int ERROR_endOfCommandUnexpected  = 6;
  private final static int ERROR_endOfExpressionExpected  = 7;
  private final static int ERROR_equalSignExpected  = 8;
  private final static int ERROR_identifierOrResidueSpecificationExpected  = 9;
  private final static int ERROR_invalidAtomSpecification  = 10;
  private final static int ERROR_invalidChainSpecification  = 11;
  private final static int ERROR_invalidExpressionToken  = 12;
  private final static int ERROR_invalidModelSpecification  = 13;
  private final static int ERROR_leftParenthesisExpected  = 14;
  private final static int ERROR_missingEnd  = 15;
  private final static int ERROR_numberExpected  = 16;
  private final static int ERROR_numberOrVariableNameExpected  = 17;
  private final static int ERROR_residueSpecificationExpected  = 18;
  private final static int ERROR_rightBraceExpected  = 19;
  private final static int ERROR_rightBracketExpected  = 20;
  private final static int ERROR_rightParenthesisExpected  = 21;
  private final static int ERROR_stringExpected  = 22;
  private final static int ERROR_unrecognizedExpressionToken  = 23;
  private final static int ERROR_unrecognizedParameter  = 24;
  private final static int ERROR_unrecognizedToken  = 25;


  private final static String[] errors = {
    GT._("bad argument count"), // 0
    GT._("invalid context for {0}"), // 1
    GT._("command expected"), // 2
    GT._("comma or right parenthesis expected"), // 3
    GT._("comparison operator expected"), // 4
    GT._("{ number number number } expected"), // 5
    GT._("unexpected end of script command"), // 6
    GT._("end of expression expected"), // 7
    GT._("equal sign expected"), // 8
    GT._("identifier or residue specification expected"), // 9
    GT._("invalid atom specification"), // 10
    GT._("invalid chain specification"), // 11
    GT._("invalid expression token: {0}"), // 12
    GT._("invalid model specification"), // 13
    GT._("left parenthesis expected"), // 14
    GT._("missing END for {0}"), // 15
    GT._("number expected"), // 16
    GT._("number or variable name expected"), // 17
    GT._("residue specification (ALA, AL?, A*) expected"), // 18
    GT._("right brace expected"), // 19
    GT._("right bracket expected"), // 20
    GT._("right parenthesis expected"), // 21
    GT._("quoted string expected"), // 22
    GT._("unrecognized expression token: {0}"), // 23
    GT._("unrecognized {0} parameter"), // 24
    GT._("unrecognized token: {0}"), // 25
  };

  private boolean commandExpected() {
    ichToken = ichCurrentCommand;
    return error(errors[ERROR_commandExpected]);
  }

  private boolean error(int error) {
    return error(error, null, null);
  }

  private boolean error(int error, String value) {
    return error(error, value, null);
  }
  
  private boolean error(int error, String value, String more) {
    String strError = errors[error];
    if (value != null)
      strError = TextFormat.simpleReplace(strError, "{0}", value);
    if (more != null)
      strError += more;
    return error(strError);
  }
  
  private boolean error(String errorMessage) {
    this.errorMessage = errorMessage;
    return false;
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
        ichToken - ichCurrentCommand)
        + " >>>> " + errorLine.substring(ichToken - ichCurrentCommand) : errorLine)
        + " <<<<";
    errorMessage = "script compiler ERROR: " + errorMessage
         + Eval.setErrorLineMessage(filename, lineCurrent, iCommand, lineInfo);
    if (!isSilent) {
      viewer.addCommand(errorLine + CommandHistory.ERROR_FLAG);
      Logger.error(errorMessage);
    }
    return false;
  }
 
}
