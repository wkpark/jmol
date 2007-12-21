/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.g3d.Graphics3D;
import org.jmol.g3d.Font3D;
import org.jmol.shape.Text;
import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.CommandHistory;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.TextFormat;
import org.jmol.util.Parser;

import org.jmol.modelset.Bond.BondSet;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import org.jmol.i18n.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BoxInfo;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelCollection;
import org.jmol.modelset.ModelSet;

class Eval { //implements Runnable {

  private static class Context {
    String filename;
    String functionName;
    String script;
    short[] lineNumbers;
    int[] lineIndices;
    Token[][] aatoken;
    Token[] statement;
    int statementLength;
    int pc;
    int pcEnd = Integer.MAX_VALUE;
    int lineEnd = Integer.MAX_VALUE;
    int iToken;
    StringBuffer outputBuffer;
    Hashtable contextVariables;

    Context() {
      //
    }

  }

  private final static int scriptLevelMax = 10;

  private Compiler compiler;
  private int scriptLevel;
  private int scriptReportingLevel = 0;
  private Context[] stack = new Context[scriptLevelMax];
  private String filename;
  private String functionName;
  private String script;
  private Hashtable contextVariables;

  String getScript() {
    return script;
  }

  private String thisCommand;
  private short[] lineNumbers;
  private int[] lineIndices;
  private Token[][] aatoken;
  private int pc; // program counter
  private int lineEnd;
  private int pcEnd;
  private long timeBeginExecution;
  private long timeEndExecution;
  private boolean error;
  private String errorMessage;
  private Token[] statement;
  private int statementLength;
  private BitSet bsSubset;
  private boolean isScriptCheck, historyDisabled;

  //Thread myThread;

  private boolean tQuiet;
  private boolean debugScript = false;
  private boolean fileOpenCheck = true;

  boolean logMessages;
  boolean isSyntaxCheck;
  Viewer viewer;
  int iToken;

  private StringBuffer outputBuffer;

  Eval(Viewer viewer) {
    compiler = viewer.getCompiler();
    this.viewer = viewer;
    clearDefinitionsAndLoadPredefined();
  }

  private Object getParameter(String var, boolean asToken) {
    Token token = getContextVariableAsToken(var);
    return (token == null ? viewer.getParameter(var) : asToken ? token : Token
        .oValue(token));
  }

  private Object getNumericParameter(String var) {
    Token token = getContextVariableAsToken(var);
    if (token == null) {
      Object val = viewer.getParameter(var);
      if (!(val instanceof String))
        return val;
      token = new Token(Token.string, val);
    }
    return Token.nValue(token);
  }

  private Token getContextVariableAsToken(String var) {
    if (var.equals("expressionBegin"))
      return null;
    if (contextVariables != null && contextVariables.containsKey(var))
      return (Token) contextVariables.get(var);
    for (int i = scriptLevel; --i >= 0;)
      if (stack[i].contextVariables != null
          && stack[i].contextVariables.containsKey(var))
        return (Token) stack[i].contextVariables.get(var);
    return null;
  }

  private String getParameterEscaped(String var) {
    Token token = getContextVariableAsToken(var);
    return (token == null ? "" + viewer.getParameterEscaped(var) : Escape
        .escape(token.value));
  }

  private final static String EXPRESSION_KEY = "e_x_p_r_e_s_s_i_o_n";

  /**
   * a general-use method to evaluate a "SET" type expression. 
   * @param viewer
   * @param expr
   * @return an object of one of the following types:
   *   Boolean, Integer, Float, String, Point3f, BitSet 
   */

  static Object evaluateExpression(Viewer viewer, String expr) {
    // Text.formatText for MESSAGE and ECHO
    Eval e = new Eval(viewer);
    try {
      if (e.loadScript(null, EXPRESSION_KEY + " = " + expr, false)) {
        e.setStatement(0);
        return e.parameterExpression(2, 0, "", false);
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return "ERROR";
  }

  static BitSet getAtomBitSet(Eval e, Viewer viewer, Object atomExpression) {
    if (atomExpression instanceof BitSet)
      return (BitSet) atomExpression;
    if (e == null)
      e = new Eval(viewer);
    BitSet bs = new BitSet();
    try {
      e.pushContext(null);
      String scr = "select (" + atomExpression + ")";
      scr = TextFormat.replaceAllCharacters(scr, "\n\r", "),(");
      scr = TextFormat.simpleReplace(scr, "()", "(none)");
      if (e.loadScript(null, scr, false)) {
        e.statement = e.aatoken[0];
        bs = e.expression(e.statement, 1, 0, false, false, true);
      }
      e.popContext();
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
    }
    return bs;
  }

  static Vector getAtomBitSetVector(Eval e, Viewer viewer, Object atomExpression) {
    Vector V = new Vector();
    BitSet bs = getAtomBitSet(e, viewer, atomExpression);
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i))
        V.addElement(new Integer(i));
    return V;
  }

  void haltExecution() {
    resumePausedExecution();
    interruptExecution = Boolean.TRUE;
  }

  boolean isScriptExecuting() {
    return isExecuting && !interruptExecution.booleanValue();
  }

  //FindBugs suggest these should not be static -- sounds right to me;
  // otherwise "halt" would halt script execution on ALL open applets -- not
  // the desired idea here, I think. In addition, I think it would then 
  // operate from any instance of eval. 

  private Boolean interruptExecution = Boolean.FALSE;
  private Boolean executionPaused = Boolean.FALSE;
  private boolean isExecuting = false;

  private Thread currentThread = null;

  void runEval(boolean checkScriptOnly, boolean openFiles,
               boolean historyDisabled) {
    // only one reference now -- in Viewer
    //refresh();
    boolean tempOpen = fileOpenCheck;
    fileOpenCheck = openFiles;
    viewer.pushHoldRepaint();
    interruptExecution = Boolean.FALSE;
    executionPaused = Boolean.FALSE;
    isExecuting = true;
    currentThread = Thread.currentThread();
    isSyntaxCheck = isScriptCheck = checkScriptOnly;
    timeBeginExecution = System.currentTimeMillis();
    this.historyDisabled = historyDisabled;
    try {
      instructionDispatchLoop(false);
      String script = viewer.getInterruptScript();
      if (script != "")
        runScript(script, null);
    } catch (ScriptException e) {
      error = true;
      setErrorMessage(e.toString());
      scriptStatus(errorMessage);
    }
    timeEndExecution = System.currentTimeMillis();
    fileOpenCheck = tempOpen;
    if (errorMessage == null && interruptExecution.booleanValue())
      errorMessage = "execution interrupted";
    else if (!tQuiet && !isSyntaxCheck)
      viewer.scriptStatus("Script completed");
    isExecuting = isSyntaxCheck = isScriptCheck = historyDisabled = false;
    viewer.setTainted(true);
    viewer.popHoldRepaint();

  }

  String getErrorMessage() {
    return errorMessage;
  }

  private void setErrorMessage(String err) {
    if (errorMessage == null) //there could be a compiler error from a script command
      errorMessage = GT._("script ERROR: ");
    errorMessage += err;
  }

  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  private void runScript(String script) throws ScriptException {
    runScript(script, null);
  }

  void runScript(String script, StringBuffer outputBuffer)
      throws ScriptException {
    //a = script("xxxx")
    pushContext(null);
    if (outputBuffer != null)
      this.outputBuffer = outputBuffer;
    if (loadScript(null, script, false))
      instructionDispatchLoop(false);
    popContext();
  }

  private void pushContext(Function function) throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      evalError(GT._("too many script levels"));
    Context context = new Context();
    context.filename = filename;
    context.functionName = functionName;
    context.script = script;
    context.lineNumbers = lineNumbers;
    context.lineIndices = lineIndices;
    context.aatoken = aatoken;
    context.statement = statement;
    context.statementLength = statementLength;
    context.pc = pc;
    context.lineEnd = lineEnd;
    context.pcEnd = pcEnd;
    context.iToken = iToken;
    context.outputBuffer = outputBuffer;
    context.contextVariables = contextVariables;
    stack[scriptLevel++] = context;
    if (isScriptCheck)
      Logger.info("-->>-------------".substring(0, scriptLevel + 5) + filename);
  }

  private void popContext() {
    if (isScriptCheck)
      Logger.info("--<<-------------".substring(0, scriptLevel + 5) + filename);
    if (scriptLevel == 0)
      return;
    Context context = stack[--scriptLevel];
    stack[scriptLevel] = null;
    filename = context.filename;
    functionName = context.functionName;
    script = context.script;
    lineNumbers = context.lineNumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    statement = context.statement;
    statementLength = context.statementLength;
    pc = context.pc;
    lineEnd = context.lineEnd;
    pcEnd = context.pcEnd;
    iToken = context.iToken;
    outputBuffer = context.outputBuffer;
    contextVariables = context.contextVariables;
  }

  private boolean loadScript(String filename, String script,
                             boolean debugCompiler) {
    //use runScript, not loadScript from within Eval
    this.filename = filename;
    if (!compiler.compile(filename, script, false, false, debugCompiler, false)) {
      error = true;
      errorMessage = compiler.getErrorMessage();
      return false;
    }
    this.script = compiler.getScript();
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    lineNumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    contextVariables = compiler.getContextVariables();
    return true;
  }

  private Function getFunction(String name, int tok) {
    if (name == null)
      return null;
    Function function = (Function) (name.indexOf("_") == 0 ? compiler.localFunctions
        : Compiler.globalFunctions).get(name);
    return (function == null || function.aatoken == null ? null : function);
  }

  private boolean loadFunction(String name, Vector params, int tok) {
    Function function = getFunction(name, tok);
    if (function == null)
      return false;
    aatoken = function.aatoken;
    lineNumbers = function.lineNumbers;
    lineIndices = function.lineIndices;
    script = function.script;
    pc = 0;
    if (function.names != null) {
      contextVariables = new Hashtable();
      function.setVariables(contextVariables, params);
    }
    functionName = name;
    return true;
  }

  Token getFunctionReturn(String name, Vector params) throws ScriptException {
    pushContext(null);
    loadFunction(name, params, Token.function);
    instructionDispatchLoop(false);
    Token token = getContextVariableAsToken("_retval");
    popContext();
    return token;
  }

  Object checkScriptSilent(String script) {
    if (!compiler.compile(null, script, false, true, false, true))
      return compiler.getErrorMessage();
    isSyntaxCheck = true;
    isScriptCheck = false;
    errorMessage = null;
    this.script = compiler.getScript();
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    lineNumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    contextVariables = compiler.getContextVariables();
    try {
      instructionDispatchLoop(false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
    }
    isSyntaxCheck = false;
    if (errorMessage != null)
      return errorMessage;
    Vector info = new Vector();
    info.addElement(compiler.getScript());
    info.addElement(compiler.getAatokenCompiled());
    info.addElement(compiler.getLineNumbers());
    info.addElement(compiler.getLineIndices());
    return info;
  }

  private void clearState(boolean tQuiet) {
    for (int i = scriptLevelMax; --i >= 0;)
      stack[i] = null;
    scriptLevel = 0;
    error = false;
    errorMessage = null;
    this.tQuiet = tQuiet;
  }

  boolean loadScriptString(String script, boolean tQuiet) {
    //from Viewer.evalStringWaitStatus()
    clearState(tQuiet);
    return loadScript(null, script, debugScript);
  }

  boolean loadScriptFile(String filename, boolean tQuiet) {
    //from viewer
    clearState(tQuiet);
    return loadScriptFileInternal(filename);
  }

  private boolean loadScriptFileInternal(String filename) {
    //from "script" command, with push/pop surrounding or viewer
    if (filename.toLowerCase().indexOf("javascript:") == 0)
      return loadScript(filename, viewer.eval(filename.substring(11)),
          debugScript);
    Object t = viewer.getBufferedReaderOrErrorMessageFromName(filename, null);
    if (!(t instanceof BufferedReader))
      return loadError((String) t);
    BufferedReader reader = (BufferedReader) t;
    StringBuffer script = new StringBuffer();
    try {
      while (true) {
        String command = reader.readLine();
        if (command == null)
          break;
        script.append(command);
        script.append("\n");
      }
    } catch (IOException e) {
      try {
        reader.close();
        reader = null;
      } catch (IOException ioe) {
      }
      return ioError(filename);
    }
    try {
      reader.close();
      reader = null;
    } catch (IOException ioe) {
    }
    return loadScript(filename, script.toString(), debugScript);
  }

  private boolean loadError(String msg) {
    error = true;
    errorMessage = msg;
    return false;
  }

  private boolean ioError(String filename) {
    return loadError("io error reading:" + filename);
  }

  public String toString() {
    StringBuffer str = new StringBuffer();
    str.append("Eval\n pc:");
    str.append(pc);
    str.append("\n");
    str.append(aatoken.length);
    str.append(" statements\n");
    for (int i = 0; i < aatoken.length; ++i) {
      str.append("----\n");
      Token[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str.append(atoken[j]);
        str.append('\n');
      }
      str.append('\n');
    }
    str.append("END\n");
    return str.toString();
  }

  private void clearPredefined(String[] list) {
    int cPredef = list.length;
    for (int iPredef = 0; iPredef < cPredef; iPredef++)
      predefine(list[iPredef]);
  }

  void clearDefinitionsAndLoadPredefined() {
    //executed each time a file is loaded; like clear() for the managers
    variables.clear();
    bsSubset = null;
    viewer.setSelectionSubset(null);
    if (viewer.getModelSet() == null || viewer.getAtomCount() == 0)
      return;
    clearPredefined(JmolConstants.predefinedStatic);
    clearPredefined(JmolConstants.predefinedVariable);
    // Now, define all the elements as predefined sets
    // hydrogen is handled specially, so don't define it

    int firstIsotope = JmolConstants.firstIsotope;
    // name ==> e_=n for all standard elements
    for (int i = JmolConstants.elementNumberMax; --i > 1;) {
      String definition = "@" + JmolConstants.elementNameFromNumber(i) + " _e="
          + i;
      predefine(definition);
    }
    // _Xx ==> name for of all elements, isotope-blind
    for (int i = JmolConstants.elementNumberMax; --i >= 1;) {
      String definition = "@_" + JmolConstants.elementSymbolFromNumber(i) + " "
          + JmolConstants.elementNameFromNumber(i);
      predefine(definition);
    }
    // name ==> _e=nn for each alternative element
    for (int i = firstIsotope; --i >= 0;) {
      String definition = "@" + JmolConstants.altElementNameFromIndex(i)
          + " _e=" + JmolConstants.altElementNumberFromIndex(i);
      predefine(definition);
    }
    // these variables _e, _x can't be more than two characters
    // name ==> _isotope=iinn for each isotope
    // _T ==> _isotope=iinn for each isotope 
    // _3H ==> _isotope=iinn for each isotope 
    for (int i = JmolConstants.altElementMax; --i >= firstIsotope;) {
      String def = " element=" + JmolConstants.altElementNumberFromIndex(i);
      String definition = "@_" + JmolConstants.altElementSymbolFromIndex(i);
      predefine(definition + def);
      definition = "@_" + JmolConstants.altIsotopeSymbolFromIndex(i);
      predefine(definition + def);
      definition = "@" + JmolConstants.altElementNameFromIndex(i);
      if (definition.length() > 1)
        predefine(definition + def);
    }
  }

  private void predefine(String script) {
    if (compiler.compile("#predefine", script, true, false, false, false)) {
      Token[][] aatoken = compiler.getAatokenCompiled();
      if (aatoken.length != 1) {
        viewer
            .scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
                + script);
        return;
      }
      Token[] statement = aatoken[0];
      if (statement.length > 2) {
        int tok = statement[iToken = 1].tok;
        if (tok == Token.identifier
            || Compiler.tokAttr(tok, Token.predefinedset)) {
          String variable = (String) statement[1].value;
          variables.put(variable, statement);
        } else {
          viewer
              .scriptStatus("JmolConstants.java ERROR: invalid variable name:"
                  + script);
        }
      } else {
        viewer
            .scriptStatus("JmolConstants.java ERROR: bad predefinition length:"
                + script);
      }
    } else {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefined set compile error:"
              + script + "\ncompile error:" + compiler.getErrorMessage());
    }
  }

  /* ****************************************************************************
   * ==============================================================
   * syntax check traps
   * ==============================================================
   */

  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    if (!isSyntaxCheck)
      viewer.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  private void setShapeSize(int shapeType, int size) {
    if (!isSyntaxCheck)
      viewer.setShapeSize(shapeType, size);
  }

  private void setBooleanProperty(String key, boolean value) {
    if (!isSyntaxCheck)
      viewer.setBooleanProperty(key, value);
  }

  private void setIntProperty(String key, int value) {
    if (!isSyntaxCheck)
      viewer.setIntProperty(key, value);
  }

  private void setFloatProperty(String key, float value) {
    if (!isSyntaxCheck)
      viewer.setFloatProperty(key, value);
  }

  private void setStringProperty(String key, String value) {
    if (!isSyntaxCheck || key.equalsIgnoreCase("defaultdirectory"))
      viewer.setStringProperty(key, value);
  }

  /* ****************************************************************************
   * ==============================================================
   * command dispatch
   * ==============================================================
   */

  void pauseExecution() {
    if (isSyntaxCheck)
      return;
    delay(100);
    viewer.popHoldRepaint();
    executionPaused = Boolean.TRUE;
  }

  boolean isExecutionPaused() {
    return executionPaused.booleanValue();
  }

  void resumePausedExecution() {
    executionPaused = Boolean.FALSE;
  }

  private boolean checkContinue() {
    if (!interruptExecution.booleanValue()) {
      if (!executionPaused.booleanValue())
        return true;
      if (Logger.debugging) {
        Logger.debug("script execution paused at this command: " + thisCommand);
      }
      try {
        while (executionPaused.booleanValue()) {
          Thread.sleep(100);
          String script = viewer.getInterruptScript();
          if (script != "") {
            resumePausedExecution();
            error = false;
            pc--; // in case there is an error, we point to the PAUSE command
            try {
              runScript(script);
            } catch (Exception e) {
              error = true;
              errorMessage = e.toString();
            }
            pc++;
            if (error)
              scriptStatus(errorMessage);
            pauseExecution();
          }
        }
      } catch (Exception e) {

      }
      Logger.debug("script execution resumed");
    }
    //once more to trap quit during pause
    return !interruptExecution.booleanValue();
  }

  private int commandHistoryLevelMax = 0;

  private void setStatement(int pc) throws ScriptException {
    statement = aatoken[pc];
    statementLength = statement.length;
    Token[] fixed;
    int i;
    int tok;
    for (i = 1; i < statementLength; i++)
      if (statement[i].tok == Token.define)
        break;
    if (i == statementLength)
      return;
    fixed = new Token[statementLength];
    fixed[0] = statement[0];
    boolean isExpression = false;
    int j = 1;
    for (i = 1; i < statementLength; i++) {
      switch (tok = statement[i].tok) {
      case Token.define:
        Object v;
        //Object var_set;
        String s;
        String var = parameterAsString(++i);
        boolean isClauseDefine = (tokAt(i) == Token.expressionBegin);
        if (isClauseDefine) {
          Vector val = (Vector) parameterExpression(++i, 0, "_var", true);
          if (val.size() == 0)
            invalidArgument();
          i = iToken;
          Token t = (Token) val.elementAt(0);
          v = (t.tok == Token.list ? t : Token.oValue(t));
        } else {
          v = getParameter(var, false);
        }
        if (v instanceof Token) {
          fixed[j] = (Token) v;
          if (isExpression && fixed[j].tok == Token.list)
            fixed[j] = new Token(Token.bitset, getAtomBitSet(this, viewer,
                Token.sValue(fixed[j])));
        } else if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          //          if (isExpression && !isClauseDefine 
          //            && (var_set = getParameter(var + "_set", false)) != null)
          //        fixed[j] = new Token(Token.define, "" + var_set);
          //    else
          fixed[j] = new Token(Token.integer, ((Integer) v).intValue(), v);

        } else if (v instanceof Float) {
          fixed[j] = new Token(Token.decimal, Compiler.modelValue("" + v), v);
        } else if (v instanceof String) {
          v = getStringObjectAsToken((String) v, null);
          if (v instanceof Token) {
            fixed[j] = (Token) v;
          } else {
            // identifiers cannot have periods; file names can, though
            s = (String) v;
            tok = (isExpression ? Token.bitset
                : isClauseDefine ? Token.string : s.indexOf(".") >= 0
                    || s.indexOf("=") >= 0 || s.indexOf("[") >= 0
                    || s.indexOf("{") >= 0 ? Token.string : Token.identifier);
            if (isExpression)
              fixed[j] = new Token(Token.bitset, getAtomBitSet(this, viewer, s));
            else
              fixed[j] = new Token(tok, s);
          }
        } else if (v instanceof BitSet) {
          fixed[j] = new Token(Token.bitset, v);
        } else if (v instanceof Point3f) {
          fixed[j] = new Token(Token.point3f, v);
        } else if (v instanceof Point4f) {
          fixed[j] = new Token(Token.point4f, v);
        } else {
          Point3f center = getDrawObjectCenter(var);
          if (center == null)
            invalidArgument();
          fixed[j] = new Token(Token.point3f, center);
        }
        if (j == 1 && statement[0].tok == Token.set
            && fixed[j].tok != Token.identifier)
          invalidArgument();
        break;
      case Token.expressionBegin:
      case Token.expressionEnd:
        //@ in expression will be taken as SELECT
        isExpression = (tok == Token.expressionBegin);
        fixed[j] = statement[i];
        break;
      default:
        fixed[j] = statement[i];
      }

      j++;
    }
    statement = fixed;
    statementLength = j;
  }

  private Object getStringObjectAsToken(String s, String key) {
    if (s == null || s.length() == 0)
      return s;
    Object v = Token.unescapePointOrBitsetAsToken(s);
    if (v instanceof String && key != null)
      return viewer.getListVariable(key, v);
    return v;
  }

  boolean isForCheck = false;

  private void instructionDispatchLoop(boolean doList) throws ScriptException {
    long timeBegin = 0;
    isForCheck = false;
    debugScript = (!isSyntaxCheck && viewer.getDebugScript());
    logMessages = (debugScript && Logger.debugging);
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      viewer.scriptStatus(toString());
    }
    if (!historyDisabled && !isSyntaxCheck
        && scriptLevel <= commandHistoryLevelMax)
      viewer.addCommand(script);
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (!checkContinue())
        break;
      Token token = aatoken[pc][0];
      setStatement(pc);
      if (lineNumbers[pc] > lineEnd)
        break;
      thisCommand = getCommand();
      iToken = 0;
      String script = viewer.getInterruptScript();
      if (script != "")
        runScript(script);
      if (doList || !isSyntaxCheck) {
        int milliSecDelay = viewer.getScriptDelay();
        if (doList || milliSecDelay > 0 && scriptLevel > 0) {
          if (milliSecDelay > 0)
            delay((long) milliSecDelay);
          viewer.scriptEcho("$[" + scriptLevel + "." + lineNumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (isSyntaxCheck) {
        if (isScriptCheck)
          Logger.info(thisCommand);
        if (statementLength == 1 && statement[0].tok != Token.function)
          //            && !Compiler.tokAttr(token.tok, Token.unimplemented))
          continue;
      } else {
        if (debugScript)
          logDebugScript(0);
        if (logMessages)
          Logger.debug(token.toString());
      }
      switch (token.tok) {

      case Token.elseif:
      case Token.ifcmd:
      case Token.whilecmd:
      case Token.forcmd:
      case Token.endifcmd:
      case Token.elsecmd:
      case Token.end:
      case Token.breakcmd:
      case Token.continuecmd:
        flowControl(token.tok);
        break;
      case Token.backbone:
        proteinShape(JmolConstants.SHAPE_BACKBONE);
        break;
      case Token.background:
        background(1);
        break;
      case Token.center:
        center(1);
        break;
      case Token.color:
        color();
        break;
      case Token.data:
        data();
        break;
      case Token.define:
        define();
        break;
      case Token.echo:
        echo(1);
        break;
      case Token.message:
        message();
        break;
      case Token.resume:
        // just needed for script checking
        break;
      case Token.exit: // flush the queue and...
        if (!isSyntaxCheck && pc > 0)
          viewer.clearScriptQueue();
      case Token.quit: // quit this only if it isn't the first command
        if (!isSyntaxCheck)
          interruptExecution = ((pc > 0 || !viewer.usingScriptQueue()) ? Boolean.TRUE
              : Boolean.FALSE);
        break;
      case Token.label:
        label(1);
        break;
      case Token.hover:
        hover();
        break;
      case Token.load:
        load();
        break;
      case Token.monitor:
        monitor();
        break;
      case Token.refresh:
        refresh();
        break;
      case Token.initialize:
        initialize();
        break;
      case Token.reset:
        reset();
        break;
      case Token.rotate:
        rotate(false, false);
        break;
      case Token.script:
      case Token.javascript:
        script(token.tok == Token.javascript);
        break;
      case Token.function:
        function(token.tok);
        break;
      case Token.sync:
        sync();
        break;
      case Token.history:
        history(1);
        break;
      case Token.select:
        select();
        break;
      case Token.translate:
        translate();
        break;
      case Token.invertSelected:
        invertSelected();
        break;
      case Token.rotateSelected:
        rotate(false, true);
        break;
      case Token.translateSelected:
        translateSelected();
        break;
      case Token.zap:
        zap();
        break;
      case Token.zoom:
        zoom(false);
        break;
      case Token.zoomTo:
        zoom(true);
        break;
      case Token.delay:
        delay();
        break;
      case Token.loop:
        delay();
        if (!isSyntaxCheck)
          pc = -1;
        break;
      case Token.gotocmd:
        gotocmd();
        break;
      case Token.move:
        move();
        break;
      case Token.display:
        display(true);
        break;
      case Token.hide:
        display(false);
        break;
      case Token.restrict:
        restrict();
        break;
      case Token.subset:
        subset();
        break;
      case Token.selectionHalo:
        selectionHalo(1);
        break;
      case Token.set:
        set();
        break;
      case Token.slab:
        slab(false);
        break;
      case Token.depth:
        slab(true);
        break;
      case Token.star:
        star();
        break;
      case Token.structure:
        structure();
        break;
      case Token.halo:
        halo();
        break;
      case Token.spacefill:
        spacefill();
        break;
      case Token.wireframe:
        wireframe();
        break;
      case Token.vector:
        vector();
        break;
      case Token.dipole:
        dipole();
        break;
      case Token.animation:
        animation();
        break;
      case Token.vibration:
        vibration();
        break;
      case Token.calculate:
        calculate();
        break;
      case Token.dots:
        dots(1, JmolConstants.SHAPE_DOTS);
        break;
      case Token.strands:
        proteinShape(JmolConstants.SHAPE_STRANDS);
        break;
      case Token.meshRibbon:
        proteinShape(JmolConstants.SHAPE_MESHRIBBON);
        break;
      case Token.ribbon:
        proteinShape(JmolConstants.SHAPE_RIBBONS);
        break;
      case Token.trace:
        proteinShape(JmolConstants.SHAPE_TRACE);
        break;
      case Token.cartoon:
        proteinShape(JmolConstants.SHAPE_CARTOON);
        break;
      case Token.rocket:
        proteinShape(JmolConstants.SHAPE_ROCKETS);
        break;
      case Token.spin:
        rotate(true, false);
        break;
      case Token.ssbond:
        ssbond();
        break;
      case Token.hbond:
        hbond(true);
        break;
      case Token.show:
        show();
        break;
      case Token.file:
        file();
        break;
      case Token.frame:
      case Token.model:
        frame(1);
        break;
      case Token.font:
        font(-1, 0);
        break;
      case Token.moveto:
        moveto();
        break;
      case Token.navigate:
        navigate();
        break;
      case Token.bondorder:
        bondorder();
        break;
      case Token.console:
        console();
        break;
      case Token.pmesh:
        pmesh();
        break;
      case Token.draw:
        draw();
        break;
      case Token.polyhedra:
        polyhedra();
        break;
      case Token.geosurface:
        dots(1, JmolConstants.SHAPE_GEOSURFACE);
        break;
      case Token.centerAt:
        centerAt();
        break;
      case Token.isosurface:
        isosurface(JmolConstants.SHAPE_ISOSURFACE);
        break;
      case Token.lcaocartoon:
        lcaoCartoon();
        break;
      case Token.mo:
        mo(false);
        break;
      case Token.stereo:
        stereo();
        break;
      case Token.connect:
        connect();
        break;
      case Token.getproperty:
        getProperty();
        break;
      case Token.configuration:
        configuration();
        break;
      case Token.axes:
        axes(1);
        break;
      case Token.boundbox:
        boundbox(1);
        break;
      case Token.unitcell:
        unitcell(1);
        break;
      case Token.frank:
        frank(1);
        break;
      case Token.help:
        help();
        break;
      case Token.save:
        save();
        break;
      case Token.restore:
        restore();
        break;
      case Token.ramachandran:
        dataFrame(JmolConstants.JMOL_DATA_RAMACHANDRAN);
        break;
      case Token.quaternion:
        dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
        break;
      case Token.write:
        write();
        break;
      case Token.print:
        print();
        break;
      case Token.returncmd:
        returnCmd();
        break;
      case Token.pause: //resume is done differently
        pause();
        break;
      default:
        unrecognizedCommand();
      }
      if (!isSyntaxCheck)
        viewer.setCursor(Viewer.CURSOR_DEFAULT);
    }
  }

  private void flowControl(int tok) throws ScriptException {
    int pt = statement[0].intValue;
    boolean isDone = (pt < 0 && !isSyntaxCheck);
    boolean isOK = true;
    int ptNext = 0;
    switch (tok) {
    case Token.ifcmd:
    case Token.elseif:
      isOK = (!isDone && ifCmd());
      if (isSyntaxCheck)
        break;
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      ptNext = (isDone || isOK ? -ptNext : ptNext);
      aatoken[Math.abs(pt)][0].intValue = ptNext;
      break;
    case Token.elsecmd:
      checkStatementLength(1);
      if (pt < 0 && !isSyntaxCheck)
        pc = -pt - 1;
      break;
    case Token.endifcmd:
      checkStatementLength(1);
      break;
    case Token.end: //if, for, while
      checkLength2();
      isForCheck = (tokAt(1) == Token.forcmd);
      isOK = (tokAt(1) == Token.ifcmd);
      break;
    case Token.whilecmd:
      isForCheck = false;
      if (!ifCmd() && !isSyntaxCheck)
        pc = pt;
      break;
    case Token.breakcmd:
      if (!isSyntaxCheck)
        pc = aatoken[pt][0].intValue;
      if (statementLength > 1) {
        checkLength2();
        intParameter(1);
      }
      break;
    case Token.continuecmd:
      isForCheck = true;
      if (!isSyntaxCheck)
        pc = pt - 1;
      if (statementLength > 1) {
        checkLength2();
        intParameter(1);
      }
      break;
    case Token.forcmd:
      // for (i = 1; i < 3; i = i + 1);
      // for (var i = 1; i < 3; i = i + 1);
      // for (;;;);
      int[] pts = new int[2];
      int j = 0;
      for (int i = 1; i < statementLength && j < 2; i++)
        if (tokAt(i) == Token.semicolon)
          pts[j++] = i;
      if (isForCheck) {
        j = pts[1] + 1;
        isForCheck = false;
      } else {
        j = 2;
        if (tokAt(j) == Token.var)
          j++;
      }
      if (tokAt(j) == Token.identifier) {
        String key = parameterAsString(j);
        if (getToken(++j).tok != Token.opEQ)
          invalidArgument();
        setVariable(++j, statementLength - 1, key, false);
      }
      isOK = ((Boolean) parameterExpression(pts[0] + 1, pts[1], null, false))
          .booleanValue();
      pt++;
      break;
    }
    if (!isOK && !isSyntaxCheck)
      pc = Math.abs(pt) - 1;
  }

  private boolean ifCmd() throws ScriptException {
    return ((Boolean) parameterExpression(1, 0, null, false)).booleanValue();
  }

  private int getLinenumber() {
    return lineNumbers[pc];
  }

  private String getCommand() {
    int ichBegin = lineIndices[pc];
    int ichEnd = (pc + 1 == lineIndices.length || lineIndices[pc + 1] == 0 ? script
        .length()
        : lineIndices[pc + 1]);

    String s = "";
    try {
      s = script.substring(ichBegin, ichEnd);
      int i;
      if ((i = s.indexOf("\n")) >= 0)
        s = s.substring(0, i);
      if ((i = s.indexOf("\r")) >= 0)
        s = s.substring(0, i);
      if (!s.endsWith(";"))
        s += ";";
    } catch (Exception e) {
      Logger.error("darn problem in Eval getCommand: ichBegin=" + ichBegin
          + " ichEnd=" + ichEnd + " len = " + script.length() + " script = "
          + script + "\n" + e);
    }
    return s;
  }

  private final StringBuffer strbufLog = new StringBuffer(80);

  private void logDebugScript(int ifLevel) {
    strbufLog.setLength(0);
    if (logMessages) {
      Logger.debug(statement[0].toString());
      for (int i = 1; i < statementLength; ++i)
        Logger.debug(statement[i].toString());
    }
    iToken = -2;
    String s = (ifLevel > 0 ? "                          ".substring(0,
        ifLevel * 2) : "");
    strbufLog.append(s).append(statementAsString());
    viewer.scriptStatus(strbufLog.toString());
  }

  /* ****************************************************************************
   * ==============================================================
   * expression processing
   * ==============================================================
   */

  private Token[] tempStatement;
  private boolean isBondSet;
  private Object expressionResult;

  private BitSet expression(int index) throws ScriptException {
    if (!checkToken(index))
      badArgumentCount();
    return expression(statement, index, 0, true, false, true);
  }

  private BitSet expression(Token[] code, int pcStart, int pcStop,
                            boolean allowRefresh, boolean allowUnderflow,
                            boolean bsRequired) throws ScriptException {
    //note that this is general -- NOT just statement[]
    //errors reported would improperly access statement/line context
    //there should be no errors anyway, because this is for 
    //predefined variables, but it is conceivable that one could
    //have a problem. 

    isBondSet = false;
    if (code != statement) {
      tempStatement = statement;
      statement = code;
    }
    Rpn rpn = new Rpn(64, false, false);
    Object val;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    int atomCount = viewer.getAtomCount();
    if (ignoreSubset)
      pcStart = -pcStart;
    if (pcStop == 0)
      pcStop = pcStart + 1;
    //    if (logMessages)
    //    viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      Token instruction = code[pc];
      if (instruction == null)
        break;
      Object value = instruction.value;
      //if (logMessages)
      //viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.expressionBegin:
        pcStart = pc;
        pcStop = code.length;
        break;
      case Token.expressionEnd:
        break expression_loop;
      case Token.leftbrace:
        if (isPoint3f(pc)) {
          Point3f pt = getPoint3f(pc, true);
          if (pt != null) {
            rpn.addX(pt);
            pc = iToken;
            break;
          }
        }
        break; //ignore otherwise
      case Token.rightbrace:
        break;
      case Token.leftsquare:
        isInMath = true;
        rpn.addOp(instruction);
        break;
      case Token.rightsquare:
        isInMath = false;
        rpn.addOp(instruction);
        break;
      case Token.identifier:
        val = getParameter((String) value, true);
        if (val instanceof String)
          val = getStringObjectAsToken((String) val, null);
        if (val instanceof String)
          val = lookupIdentifierValue((String) value);
        rpn.addX(val);
        break;
      case Token.define:
        rpn.addX(getAtomBitSet(this, viewer, (String) value));
        break;
      case Token.plane:
        rpn.addX(instruction);
        rpn.addX(new Token(Token.point4f, planeParameter(pc + 2)));
        pc = iToken;
        break;
      case Token.coord:
        rpn.addX(instruction);
        rpn.addX(getPoint3f(pc + 2, true));
        pc = iToken;
        break;
      case Token.string:
        rpn.addX(instruction);
        if (((String) value).equals("hkl")) {
          rpn.addX(new Token(Token.point4f, hklParameter(pc + 2)));
          pc = iToken;
        }
        break;
      case Token.within:
      case Token.substructure:
      case Token.connected:
      case Token.comma:
        rpn.addOp(instruction);
        break;
      case Token.all:
        rpn.addX(viewer.getModelAtomBitSet(-1, true));
        break;
      case Token.none:
        rpn.addX(new BitSet());
        break;
      case Token.on:
      case Token.off:
        rpn.addX(instruction.tok == Token.on);
        break;
      case Token.selected:
        rpn.addX(BitSetUtil.copy(viewer.getSelectionSet()));
        break;
      case Token.subset:
        rpn.addX(bsSubset == null ? viewer.getModelAtomBitSet(-1, true)
            : BitSetUtil.copy(bsSubset));
        break;
      case Token.hidden:
        rpn.addX(BitSetUtil.copy(viewer.getHiddenSet()));
        break;
      case Token.displayed:
        rpn.addX(BitSetUtil.copyInvert(viewer.getHiddenSet(), atomCount));
        break;
      case Token.visible:
        if (!isSyntaxCheck && !refreshed)
          viewer.setModelVisibility();
        refreshed = true;
        rpn.addX(viewer.getVisibleSet());
        break;
      case Token.clickable:
        // a bit different, because it requires knowing what got slabbed
        if (!isSyntaxCheck && allowRefresh)
          refresh();
        rpn.addX(viewer.getClickableSet());
        break;
      case Token.specialposition:
      case Token.symmetry:
      case Token.unitcell:
      case Token.hetero:
      case Token.hydrogen:
      case Token.protein:
      case Token.nucleic:
      case Token.dna:
      case Token.rna:
      case Token.carbohydrate:
      case Token.purine:
      case Token.pyrimidine:
      case Token.isaromatic:
        rpn.addX(getAtomBits(instruction.tok));
        break;
      case Token.spec_atom:
      case Token.spec_name_pattern:
      case Token.spec_alternate:
        rpn.addX(viewer.getAtomBits(instruction.tok, (String) value));
        break;
      case Token.spec_model:
        // from select */1002 or */1000002 or */1.2
        // */1002 is equivalent to 1.2 when more than one file is present
      case Token.spec_model2:
        // from just using the number 1.2
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE) {
          // from select */n 
          iModel = ((Integer) value).intValue();
          if (!viewer.haveFileSet()) {
            rpn.addX(getAtomBits(Token.spec_model, iModel));
            break;
          }
          if (iModel < 1000)
            iModel = iModel * 1000000;
          else
            iModel = (iModel / 1000) * 1000000 + iModel % 1000;
        }
        rpn.addX(bitSetForModelFileNumber(iModel));
        break;
      case Token.spec_resid:
      case Token.spec_chain:
        rpn.addX(getAtomBits(instruction.tok, instruction.intValue));
        break;
      case Token.spec_seqcode:
        if (isInMath) {
          rpn.addX(instruction.intValue);
          break;
        }
        rpn.addX(getAtomBits(instruction.tok, getSeqCode(instruction)));
        break;
      case Token.spec_seqcode_range:
        if (isInMath) {
          rpn.addX(instruction.intValue);
          rpn.addX(Token.tokenMinus);
          rpn.addX(code[++pc].intValue);
          break;
        }
        rpn.addX(getAtomBits(instruction.tok, new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]) }));
        break;
      case Token.cell:
        Point3f pt = (Point3f) value;
        rpn.addX(getAtomBits(Token.cell, new int[] { (int) (pt.x * 1000),
            (int) (pt.y * 1000), (int) (pt.z * 1000) }));
        break;
      case Token.thismodel:
        rpn
            .addX(viewer
                .getModelAtomBitSet(viewer.getCurrentModelIndex(), true));
        break;
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.sidechain:
      case Token.surface:
        rpn.addX(lookupIdentifierValue((String) value));
        break;
      case Token.opLT:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opEQ:
      case Token.opNE:
        val = code[++pc].value;
        int tokOperator = instruction.tok;
        int tokWhat = instruction.intValue;
        String property = (tokWhat == Token.property ? (String) val : null);
        if (property != null)
          val = code[++pc].value;
        if (isSyntaxCheck) {
          rpn.addX(new BitSet());
          break;
        }
        boolean isModel = (tokWhat == Token.model);
        boolean isRadius = (tokWhat == Token.radius);
        int tokValue = code[pc].tok;
        comparisonValue = code[pc].intValue;
        float comparisonFloat = Float.NaN;
        if (val instanceof String) {
          if (tokValue == Token.identifier)
            val = getNumericParameter((String) val);
          if (val instanceof String)
            val = Token.nValue(code[pc]);
          if (val instanceof Integer)
            comparisonFloat = comparisonValue = ((Integer) val).intValue();
          else if (val instanceof Float && isModel)
            comparisonValue = ModelCollection
                .modelFileNumberFromFloat(((Float) val).floatValue());
        }
        if (val instanceof Integer || tokValue == Token.integer) {
          comparisonValue *= (Compiler
              .tokAttr(tokWhat, Token.atompropertyfloat) ? 100 : 1);
          comparisonFloat = comparisonValue;
          if (isModel && viewer.haveFileSet()) {
            if (comparisonValue >= 1000 && comparisonValue < 1000000) {
              comparisonValue = (comparisonValue / 1000) * 1000000
                  + comparisonValue % 1000;
              tokWhat = -Token.model;
            }
          }
        } else if (val instanceof Float) {
          if (isModel) {
            tokWhat = -Token.model;
          } else {
            comparisonFloat = ((Float) val).floatValue();
            comparisonValue = (int) (comparisonFloat * (isRadius ? 250f : 100f));
          }
        } else {
          iToken++;
          invalidArgument();
        }
        if (isModel && comparisonValue >= 1000000
            && comparisonValue % 1000000 == 0) {
          comparisonValue /= 1000000;
          tokWhat = Token.file;
          isModel = false;
        }
        if (tokWhat == -Token.model && tokOperator == Token.opEQ) {
          rpn.addX(bitSetForModelFileNumber(comparisonValue));
          break;
        }
        if (((String) value).indexOf("-") >= 0) {
          if (!Float.isNaN(comparisonFloat))
            comparisonFloat = -comparisonFloat;
          comparisonValue = -comparisonValue;
        }
        float[] data = (tokWhat == Token.property ? viewer
            .getDataFloat(property) : null);
        rpn.addX(comparatorInstruction(instruction, tokWhat, data, tokOperator,
            comparisonValue, comparisonFloat));
        break;
      case Token.bitset:
      case Token.decimal:
      case Token.point3f:
      case Token.point4f:
        rpn.addX(value);
        break;
      case Token.integer:
        rpn.addX(instruction.intValue);
        break;
      default:
        if (Compiler.tokAttr(instruction.tok, Token.mathop))
          rpn.addOp(instruction);
        else
          unrecognizedExpression();
      }
    }
    expressionResult = rpn.getResult(allowUnderflow, null);
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!isSyntaxCheck)
        rpn.dumpStacks();
      endOfStatementUnexpected();
    }
    expressionResult = ((Token) expressionResult).value;
    if (bsRequired && expressionResult instanceof String) {
      // allow for select @{x} where x is a string that can evaluate to a bitset
      expressionResult = getAtomBitSet(this, viewer, (String) expressionResult);
    }
    if (!bsRequired && !(expressionResult instanceof BitSet))
      return null; // because result is in expressionResult in that case
    BitSet bs = (expressionResult instanceof BitSet ? (BitSet) expressionResult
        : new BitSet());
    isBondSet = (expressionResult instanceof BondSet);
    if (!ignoreSubset && bsSubset != null && !isBondSet)
      bs.and(bsSubset);
    if (tempStatement != null) {
      statement = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  private static int getSeqCode(Token instruction) {
    return (instruction.intValue != Integer.MAX_VALUE ? Group.getSeqcode(
        instruction.intValue, ' ') : ((Integer) instruction.value).intValue());
  }

  private BitSet lookupIdentifierValue(String identifier)
      throws ScriptException {
    // all variables and possible residue names for PDB
    // or atom names for non-pdb atoms are processed here.

    // priority is given to a defined variable.

    BitSet bs = lookupValue(identifier, false);
    if (bs != null)
      return BitSetUtil.copy(bs);

    // next we look for names of groups (PDB) or atoms (non-PDB)
    bs = getAtomBits(Token.identifier, identifier);
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet getAtomBits(int tokType) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(tokType);
  }

  private BitSet getAtomBits(int tokType, String specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(tokType, specInfo);
  }

  private BitSet getAtomBits(int tokType, int specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(tokType, specInfo);
  }

  private BitSet getAtomBits(int tokType, int[] specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(tokType, specInfo);
  }

  private BitSet lookupValue(String variable, boolean plurals)
      throws ScriptException {
    if (isSyntaxCheck) {
      return new BitSet();
    }
    //if (logMessages)
    //viewer.scriptStatus("lookupValue(" + variable + ")");
    Object value = variables.get(variable);
    boolean isDynamic = false;
    if (value == null) {
      value = variables.get("!" + variable);
      isDynamic = (value != null);
    }
    if (value != null) {
      if (value instanceof Token[]) {
        pushContext(null);
        value = expression((Token[]) value, -2, 0, true, false, true);
        popContext();
        if (!isDynamic)
          variables.put(variable, value);
      }
      return (BitSet) value;
    }
    if (plurals)
      return null;
    int len = variable.length();
    if (len < 5) // iron is the shortest
      return null;
    if (variable.charAt(len - 1) != 's')
      return null;
    if (variable.endsWith("ies"))
      variable = variable.substring(0, len - 3) + 'y';
    else
      variable = variable.substring(0, len - 1);
    return lookupValue(variable, true);
  }

  private BitSet comparatorInstruction(Token token, int tokWhat, float[] data,
                                       int tokOperator, int comparisonValue,
                                       float comparisonFloat)
      throws ScriptException {
    BitSet bs = new BitSet();
    int propertyValue = Integer.MAX_VALUE;
    BitSet propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.getModelSet();
    Atom[] atoms = modelSet.atoms;
    int imax = 0;
    int imin = 0;
    int iModel = -1;
    int[] cellRange = null;
    int nOps = 0;
    float propertyFloat = 0;
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyValue = (int) atomProperty(atom, tokWhat, true);
        if (propertyValue == Integer.MAX_VALUE)
          continue;
        break;
      case Token.property:
        if (data == null || data.length <= i)
          continue;
        propertyFloat = data[i];
        switch (tokOperator) {
        case Token.opLT:
          match = propertyFloat < comparisonFloat;
          break;
        case Token.opLE:
          match = propertyFloat <= comparisonFloat;
          break;
        case Token.opGE:
          match = propertyFloat >= comparisonFloat;
          break;
        case Token.opGT:
          match = propertyFloat > comparisonFloat;
          break;
        case Token.opEQ:
          match = propertyFloat == comparisonFloat;
          break;
        case Token.opNE:
          match = propertyFloat != comparisonFloat;
          break;
        }
        if (match)
          bs.set(i);
        continue;
      case Token.symop:
        propertyBitSet = atom.getAtomSymmetry();
        if (bitsetBaseValue >= 200) {
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555)
           * for this the comparision is only with the
           * translational component; the symop itself must match
           * thus: 
           * select symop!=1655 selects all symop=1 and translation !=655
           * select symop>=2555 selects all symop=2 and translation >555
           * symop >=200 indicates any symop in the specified translation
           *  (a few space groups have > 100 operations)  
           * 
           * Note that when normalization is not done, symop=1555 may not be in the 
           * base unit cell. Everything is relative to wherever the base atoms ended up,
           * usually in 555, but not necessarily.
           * 
           * The reason this is tied together an atom may have one translation
           * for one symop and another for a different one.
           * 
           * Bob Hanson - 10/2006
           */
          comparisonValue = bitsetBaseValue % 1000;
          if (atom.getModelIndex() != iModel) {
            iModel = atom.getModelIndex();
            cellRange = modelSet.getModelCellRange(iModel);
            nOps = modelSet.getModelSymmetryCount(iModel);
          }
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0) {
            match = true;
          } else if (nOps == 0 || symop >= 0 && !(match = propertyBitSet.get(symop))) {
            continue;
          }
          bitsetComparator = Token.none;
          if (symop < 0) 
            propertyValue = atom.getCellTranslation(comparisonValue, cellRange, nOps);
          else
            propertyValue = atom.getSymmetryTranslation(symop, cellRange, nOps);
        }
        break;
      }
      // note that a symop property can be both LE and GT !
      if (propertyBitSet != null) {
        switch (bitsetComparator) {
        case Token.opLT:
          imax = comparisonValue - 1;
          imin = 0;
          break;
        case Token.opLE:
          imax = comparisonValue;
          imin = 0;
          break;
        case Token.opGE:
          imax = propertyBitSet.size();
          imin = comparisonValue - 1;
          break;
        case Token.opGT:
          imax = propertyBitSet.size();
          imin = comparisonValue;
          break;
        case Token.opEQ:
          imax = comparisonValue;
          imin = comparisonValue - 1;
          break;
        case Token.opNE:
          match = !propertyBitSet.get(comparisonValue);
          break;
        }
        if (imin < 0)
          imin = 0;
        if (imax > propertyBitSet.size())
          imax = propertyBitSet.size();
        for (int iBit = imin; iBit < imax; iBit++) {
          if (propertyBitSet.get(iBit)) {
            match = true;
            break;
          }
        }
        if (!match || propertyValue == Integer.MAX_VALUE)
          tokOperator = Token.none;

      }
      switch (tokOperator) {
      case Token.opLT:
        match = propertyValue < comparisonValue;
        break;
      case Token.opLE:
        match = propertyValue <= comparisonValue;
        break;
      case Token.opGE:
        match = propertyValue >= comparisonValue;
        break;
      case Token.opGT:
        match = propertyValue > comparisonValue;
        break;
      case Token.opEQ:
        match = propertyValue == comparisonValue;
        break;
      case Token.opNE:
        match = propertyValue != comparisonValue;
        break;
      }
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private float atomProperty(Atom atom, int tokWhat, boolean asInt)
      throws ScriptException {
    float propertyValue = 0;
    switch (tokWhat) {
    case Token.atomno:
      return atom.getAtomNumber();
    case Token.atomIndex:
      return atom.getAtomIndex();
    case Token.elemno:
      return atom.getElementNumber();
    case Token.element:
      return atom.getAtomicAndIsotopeNumber();
    case Token.formalCharge:
      return atom.getFormalCharge();
    case Token.partialCharge:
      propertyValue = atom.getPartialCharge();
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.site:
      return atom.getAtomSite();
    case Token.molecule:
      return atom.getMoleculeNumber();
    case Token.temperature: // 0 - 9999
      propertyValue = atom.getBfactor100();
      return (propertyValue < 0 ? Integer.MAX_VALUE : asInt ? propertyValue
          : propertyValue / 100f);
    case Token.surfacedistance:
      viewer.getSurfaceDistanceMax();
      propertyValue = atom.getSurfaceDistance100();
      return (asInt ? propertyValue : propertyValue / 100f);
    case Token.occupancy:
      return atom.getOccupancy();
    case Token.polymerLength:
      return atom.getPolymerLength();
    case Token.resno:
      return atom.getResno();
    case Token.groupID:
      propertyValue = atom.getGroupID();
      return (propertyValue < 0 ? Integer.MAX_VALUE : propertyValue);
    case Token.atomID:
      return atom.getSpecialAtomID();
    case Token.structure:
      return atom.getProteinStructureType();
    case Token.radius:
      return atom.getRasMolRadius();
    case Token.vanderwaals:
      return (asInt ? 100 : 1) * atom.getVanderwaalsRadiusFloat();
    case Token.psi:
      propertyValue = atom.getGroupPsi();
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.phi:
      propertyValue = atom.getGroupPhi();
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.bondcount:
      return atom.getCovalentBondCount();
    case Token.valence:
      return atom.getValence();
    case Token.file:
      return atom.getModelFileIndex() + 1;
    case Token.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return atom.getModelNumber();
    case -Token.model:
      //float is handled differently
      return atom.getModelFileNumber();
    case Token.atomX:
      propertyValue = atom.x;
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.atomY:
      propertyValue = atom.y;
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.atomZ:
      propertyValue = atom.z;
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.fracX:
      propertyValue = atom.getFractionalCoord('X');
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.fracY:
      propertyValue = atom.getFractionalCoord('Y');
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.fracZ:
      propertyValue = atom.getFractionalCoord('Z');
      return asInt ? propertyValue * 100 : propertyValue;
    default:
      unrecognizedAtomProperty(Token.nameOf(tokWhat));
    }
    return 0;
  }

  /* ****************************************************************************
   * ==============================================================
   * checks and parameter retrieval
   * ==============================================================
   */

  private void checkStatementLength(int length) throws ScriptException {
    iToken = statementLength;
    if (statementLength != length)
      badArgumentCount();
  }

  private void checkLength34() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 3 || statementLength > 4)
      badArgumentCount();
  }

  private int checkLength23() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 2 || statementLength > 3)
      badArgumentCount();
    return statementLength;
  }

  private void checkLength2() throws ScriptException {
    checkStatementLength(2);
  }

  private void checkLength3() throws ScriptException {
    checkStatementLength(3);
  }

  private void checkLength4() throws ScriptException {
    checkStatementLength(4);
  }

  private int modelNumberParameter(Token token) {
    int iFrame = 0;
    boolean useModelNumber = false;
    switch (token.tok) {
    case Token.integer:
      useModelNumber = true;
    //fall through
    case Token.decimal:
      iFrame = token.intValue; //decimal Token intValue is model/frame number encoded
      break;
    default:
      return -1;
    }
    return viewer.getModelNumberIndex(iFrame, useModelNumber, true);
  }

  private String optParameterAsString(int i) throws ScriptException {
    if (i >= statementLength)
      return "";
    return parameterAsString(i);
  }

  private String parameterAsString(int i) throws ScriptException {
    getToken(i);
    if (theToken == null)
      endOfStatementUnexpected();
    return (theTok == Token.integer ? "" + theToken.intValue : ""
        + theToken.value);
  }

  private int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.integer)
        return theToken.intValue;
    return integerExpected();
  }

  private boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case Token.integer:
    case Token.decimal:
      return true;
    }
    return false;
  }

  private float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      getToken(index);
      switch (theTok) {
      case Token.spec_seqcode:
      case Token.integer:
        return theToken.intValue;
      case Token.decimal:
        return ((Float) theToken.value).floatValue();
      }
    }
    return numberExpected();
  }

  private int floatParameterSet(int i, float[] fparams) throws ScriptException {
    if (tokAt(i) == Token.leftbrace)
      i++;
    for (int j = 0; j < fparams.length; j++)
      fparams[j] = floatParameter(i++);
    if (tokAt(i) == Token.rightbrace)
      i++;
    return i;
  }

  private String stringParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.string)
        return (String) theToken.value;
    return stringExpected();
  }

  private String objectNameParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.identifier)
        return parameterAsString(index);
    return objectNameExpected();
  }

  /**
   * Based on the form of the parameters, returns and encoded radius
   * as follows:
   * 
   * script   meaning   range       encoded     
   * 
   * +1.2     offset    [0 - 10]        x        
   * -1.2     offset       0)           x         
   *  1.2     absolute  (0 - 10]      x + 10    
   * -30%     70%      (-100 - 0)     x + 200
   * +30%     130%        (0          x + 200
   *  80%     percent     (0          x + 100
   * 
   *  in each case, numbers can be integer or float
   * 
   * @param index
   * @param defaultValue  a default value or Float.NaN
   * @return one of the above possibilities
   * @throws ScriptException
   */
  private float radiusParameter(int index, float defaultValue)
      throws ScriptException {
    if (!checkToken(index)) {
      if (Float.isNaN(defaultValue))
        numberExpected();
      return defaultValue;
    }
    getToken(index);
    float v = Float.NaN;
    boolean isOffset = (theTok == Token.plus);
    if (isOffset)
      index++;
    boolean isPercent = (tokAt(index + 1) == Token.percent);
    switch (tokAt(index)) {
    case Token.integer:
      v = intParameter(index);
    case Token.decimal:
      if (Float.isNaN(v))
        v = floatParameter(index);
      if (v < 0)
        isOffset = true;
      break;
    default:
      v = defaultValue;
      index--;
    }
    iToken = index + (isPercent ? 1 : 0);
    if (Float.isNaN(v))
      numberExpected();
    if (v == 0)
      return 0;
    if (isPercent) {
      if (v <= -100)
        invalidArgument();
      v += (isOffset ? 200 : 100);
    } else if (isOffset) {
    } else {
      if (v < 0 || v > 10)
        numberOutOfRange(0f, 10f);
      v += 10;
    }
    return v;
  }

  private int setShapeByNameParameter(int index) throws ScriptException {
    String objectName = objectNameParameter(index);
    int shapeType = viewer.getShapeIdFromObjectName(objectName);
    if (!isSyntaxCheck && shapeType < 0)
      objectNameExpected();
    setShapeProperty(shapeType, "thisID", objectName);
    return shapeType;
  }

  private boolean booleanParameter(int i) throws ScriptException {
    if (statementLength == i)
      return true;
    checkStatementLength(i + 1);
    switch (getToken(i).tok) {
    case Token.on:
      return true;
    case Token.off:
      return false;
    default:
      booleanExpected();
    }
    return false;
  }

  private Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case Token.bitset:
    case Token.expressionBegin:
      BitSet bs = expression(statement, i, 0, true, false, false);
      if (bs != null)
        return viewer.getAtomSetCenter(bs);
      if (expressionResult instanceof Point3f)
        return (Point3f) expressionResult;
      invalidArgument();
    case Token.leftbrace:
    case Token.point3f:
      return getPoint3f(i, true);
    }
    invalidArgument();
    //impossible return
    return null;
  }

  private boolean isCenterParameter(int i) {
    int tok = tokAt(i);
    return (tok == Token.dollarsign || tok == Token.leftbrace
        || tok == Token.expressionBegin || tok == Token.point3f || tok == Token.bitset);
  }

  private Point3f centerParameter(int i) throws ScriptException {
    Point3f center = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case Token.dollarsign:
        String id = objectNameParameter(++i);
        // allow for $pt2.3 -- specific vertex
        if (tokAt(i + 1) == Token.leftsquare) {
          id += "." + intParameter(i + 2);
          if (getToken(i + 3).tok != Token.rightsquare)
            invalidArgument();
        }
        if (isSyntaxCheck)
          return new Point3f();
        if ((center = getDrawObjectCenter(id)) == null)
          drawObjectNotDefined(id);
        break;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
        center = atomCenterOrCoordinateParameter(i);
        break;
      }
    }
    if (center == null)
      coordinateOrNameOrExpressionRequired();
    return center;
  }

  private Point4f planeParameter(int i) throws ScriptException {
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    if (i < statementLength)
      switch (getToken(i).tok) {
      case Token.point4f:
        return (Point4f) theToken.value;
      case Token.dollarsign:
        String id = objectNameParameter(++i);
        if (isSyntaxCheck)
          return new Point4f();
        int shapeType = viewer.getShapeIdFromObjectName(id);
        switch (shapeType) {
        case JmolConstants.SHAPE_DRAW:
          setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", id);
          Point3f[] points = (Point3f[]) viewer.getShapeProperty(
              JmolConstants.SHAPE_DRAW, "vertices");
          if (points == null || points.length < 3)
            break;
          Vector3f pv = new Vector3f();
          float w = Graphics3D.getNormalThroughPoints(points[0], points[1],
              points[2], pv, vAB, vAC);
          return new Point4f(pv.x, pv.y, pv.z, w);
        case JmolConstants.SHAPE_ISOSURFACE:
          setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID", id);
          Point4f plane = (Point4f) viewer.getShapeProperty(
              JmolConstants.SHAPE_ISOSURFACE, "plane");
          if (plane != null)
            return plane;
        }
        break;
      case Token.identifier:
      case Token.string:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("xy"))
          return new Point4f(0, 0, 1, 0);
        if (str.equalsIgnoreCase("xz"))
          return new Point4f(0, 1, 0, 0);
        if (str.equalsIgnoreCase("yz"))
          return new Point4f(1, 0, 0, 0);
        iToken += 2;
        if (str.equalsIgnoreCase("x")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("x=?");
          return new Point4f(1, 0, 0, -floatParameter(i));
        }

        if (str.equalsIgnoreCase("y")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("y=?");
          return new Point4f(0, 1, 0, -floatParameter(i));
        }
        if (str.equalsIgnoreCase("z")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("z=?");
          return new Point4f(0, 0, 1, -floatParameter(i));
        }
        break;
      case Token.leftbrace:
        if (!isPoint3f(i))
          return getPoint4f(i);
      //fall through
      case Token.bitset:
      case Token.expressionBegin:
        Point3f pt1 = atomCenterOrCoordinateParameter(i);
        if (getToken(++iToken).tok == Token.comma)
          ++iToken;
        Point3f pt2 = atomCenterOrCoordinateParameter(iToken);
        if (getToken(++iToken).tok == Token.comma)
          ++iToken;
        Point3f pt3 = atomCenterOrCoordinateParameter(iToken);
        i = iToken;
        Vector3f plane = new Vector3f();
        float w = Graphics3D.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB,
            vAC);
        Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
        if (!isSyntaxCheck && Logger.debugging) {
          Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: " + p);
        }
        return p;
      }
    evalError(GT
        ._(
            "plane expected -- either three points or atom expressions or {0} or {1} or {2}",
            new Object[] { "{a b c d}",
                "\"xy\" \"xz\" \"yz\" \"x=...\" \"y=...\" \"z=...\"", "$xxxxx" }));
    //impossible return
    return null;
  }

  private Point4f hklParameter(int i) throws ScriptException {
    Point3f offset = viewer.getCurrentUnitCellOffset();
    if (offset == null)
      if (isSyntaxCheck)
        offset = new Point3f();
      else
        evalError(GT._("No unit cell"));
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    Point3f pt = (Point3f) getPointOrPlane(i, false, true, false, true, 3, 3);
    Point3f pt1 = new Point3f(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
    Point3f pt2 = new Point3f(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
    Point3f pt3 = new Point3f(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
    //trick for 001 010 100 is to define the other points on other edges

    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      evalError(GT._("Miller indices cannot all be zero."));
    } else if (pt.x == 0 && pt.y == 0) {
      pt1.set(1, 0, pt3.z);
      pt2.set(0, 1, pt3.z);
    } else if (pt.y == 0 && pt.z == 0) {
      pt2.set(pt1.x, 0, 1);
      pt3.set(pt1.x, 1, 0);
    } else if (pt.z == 0 && pt.x == 0) {
      pt3.set(0, pt2.y, 1);
      pt1.set(1, pt2.y, 0);
    } else if (pt.x == 0) {
      pt1.set(1, pt2.y, 0);
    } else if (pt.y == 0) {
      pt2.set(0, 1, pt3.z);
    } else if (pt.z == 0) {
      pt3.set(pt1.x, 0, 1);
    }
    viewer.toCartesian(pt1);
    viewer.toCartesian(pt2);
    viewer.toCartesian(pt3);
    pt1.add(offset);
    pt2.add(offset);
    pt3.add(offset);
    Vector3f plane = new Vector3f();
    float w = Graphics3D.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
    Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
    if (!isSyntaxCheck && Logger.debugging)
      Logger.info("defined plane: " + p);
    return p;
  }

  private short getMadParameter() throws ScriptException {
    // wireframe, ssbond, hbond
    short mad = 1;
    switch (getToken(1).tok) {
    case Token.on:
      break;
    case Token.off:
      mad = 0;
      break;
    case Token.integer:
      mad = getMadInteger(intParameter(1));
      break;
    case Token.decimal:
      mad = getMadFloat(floatParameter(1));
      break;
    default:
      booleanOrNumberExpected();
    }
    return mad;
  }

  private short getMadInteger(int radiusRasMol) throws ScriptException {
    //interesting question here about negatives... what if?
    if (radiusRasMol < 0 || radiusRasMol > 750)
      numberOutOfRange(0, 750);
    return (short) (radiusRasMol * 4 * 2);
  }

  private short getMadFloat(float angstroms) throws ScriptException {
    if (angstroms < 0 || angstroms > 3)
      numberOutOfRange(0f, 3f);
    return (short) (angstroms * 1000 * 2);
  }

  private short getSetAxesTypeMad(int index) throws ScriptException {
    if (index == statementLength)
      return 1;
    checkStatementLength(index + 1);
    switch (getToken(index).tok) {
    case Token.on:
      return 1;
    case Token.off:
      return 0;
    case Token.dotted:
      return -1;
    case Token.integer:
      int diameterPixels = intParameter(index);
      if (diameterPixels < -1 || diameterPixels >= 20)
        numberOutOfRange(-1, 19);
      return (short) diameterPixels;
    case Token.decimal:
      float angstroms = floatParameter(index);
      if (angstroms < 0 || angstroms >= 2)
        numberOutOfRange(0.01f, 1.99f);
      return (short) (angstroms * 1000 * 2);
    }
    booleanOrNumberExpected("DOTTED");
    return 0;
  }

  private boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == Token.colorRGB || tok == Token.leftsquare
        || tok == Token.point3f || isPoint3f(i) || (tok == Token.string || tok == Token.identifier)
        && Graphics3D.getArgbFromString((String) statement[i].value) != 0);
  }

  private int getArgbParam(int index) throws ScriptException {
    return getArgbParam(index, false);
  }

  private int getArgbParamLast(int index, boolean allowNone)
      throws ScriptException {
    int icolor = getArgbParam(index, allowNone);
    checkStatementLength(iToken + 1);
    return icolor;
  }

  private int getArgbParam(int index, boolean allowNone) throws ScriptException {
    Point3f pt = null;
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      case Token.identifier:
      case Token.string:
        return Graphics3D.getArgbFromString(parameterAsString(index));
      case Token.leftsquare:
        return getColorTriad(++index);
      case Token.colorRGB:
        return theToken.intValue;
      case Token.point3f:
        pt = (Point3f) theToken.value;
        break;
      case Token.leftbrace:
        pt = getPoint3f(index, false);
        break;
      case Token.none:
        if (allowNone)
          return 0;
      }
    }
    if (pt == null)
      colorExpected();
    return colorPtToInt(pt);
  }

  static int colorPtToInt(Point3f pt) {
    return 0xFF000000 | (((int) pt.x) & 0xFF) << 16
        | (((int) pt.y) & 0xFF) << 8 | (((int) pt.z) & 0xFF);
  }

  private int getArgbOrPaletteParam(int index) throws ScriptException {
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      case Token.leftsquare:
      case Token.colorRGB:
        return getArgbParam(index);
      case Token.rasmol:
        return Token.rasmol;
      case Token.none:
      case Token.jmol:
        return Token.jmol;
      }
    }
    evalError(GT._("a color or palette name (Jmol, Rasmol) is required"));
    //unattainable
    return 0;
  }

  private int getColorTriad(int i) throws ScriptException {
    int[] colors = new int[3];
    int n = 0;
    String hex = "";
    getToken(i);
    Point3f pt = null;
    out: switch (theTok) {
    case Token.integer:
    case Token.spec_seqcode:
      for (; i < statementLength; i++) {
        getToken(i);
        switch (theTok) {
        case Token.comma:
          continue;
        case Token.identifier:
          if (n != 1 || colors[0] != 0)
            badRGBColor();
          hex = "0" + parameterAsString(i);
          break out;
        case Token.integer:
          if (n > 2)
            badRGBColor();
          colors[n++] = theToken.intValue;
          continue;
        case Token.spec_seqcode:
          if (n > 2)
            badRGBColor();
          colors[n++] = ((Integer) theToken.value).intValue() % 256;
          continue;
        case Token.rightsquare:
          if (n == 3)
            return colorPtToInt(new Point3f(colors[0], colors[1], colors[2]));
        default:
          badRGBColor();
        }
      }
      badRGBColor();
    case Token.point3f:
      pt = (Point3f) theToken.value;
      break;
    case Token.identifier:
      hex = parameterAsString(i);
      break;
    default:
      badRGBColor();
    }
    if (getToken(++i).tok != Token.rightsquare)
      badRGBColor();
    if (pt != null)
      return colorPtToInt(pt);
    if ((n = Graphics3D.getArgbFromString("[" + hex + "]")) == 0)
      badRGBColor();
    return n;
  }

  private boolean coordinatesAreFractional;

  private boolean isPoint3f(int i) {
    ignoreError = true;
    boolean isOK = true;
    int t = iToken;
    try {
      getPoint3f(i, true);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    iToken = t;
    return isOK;
  }

  private Point3f getPoint3f(int i, boolean allowFractional)
      throws ScriptException {
    return (Point3f) getPointOrPlane(i, false, allowFractional, true, false, 3,
        3);
  }

  private Point4f getPoint4f(int i) throws ScriptException {
    return (Point4f) getPointOrPlane(i, false, false, false, false, 4, 4);
  }

  private Object getPointOrPlane(int index, boolean integerOnly,
                                 boolean allowFractional, boolean doConvert,
                                 boolean implicitFractional, int minDim,
                                 int maxDim) throws ScriptException {
    // { x y z } or {a/b c/d e/f} are encoded now as seqcodes and model numbers
    // so we decode them here. It's a bit of a pain, but it isn't too bad.
    float[] coord = new float[6];
    int n = 0;
    coordinatesAreFractional = implicitFractional;
    if (tokAt(index) == Token.point3f) {
      if (minDim <= 3 && maxDim >= 3)
        return (Point3f) getToken(index).value;
      invalidArgument();
    }
    if (tokAt(index) == Token.point4f) {
      if (minDim <= 4 && maxDim >= 4)
        return (Point4f) getToken(index).value;
      invalidArgument();
    }
    int multiplier = 1;
    out: for (int i = index; i < statement.length; i++) {
      switch (getToken(i).tok) {
      case Token.leftbrace:
      case Token.comma:
      // case Token.opOr:
      case Token.opAnd:
        break;
      case Token.rightbrace:
        break out;
      case Token.minus:
        multiplier = -1;
        break;
      case Token.spec_seqcode_range:
        if (n == 6)
          invalidArgument();
        coord[n++] = theToken.intValue;
        multiplier = -1;
        break;
      case Token.integer:
      case Token.spec_seqcode:
        if (n == 6)
          invalidArgument();
        coord[n++] = theToken.intValue * multiplier;
        multiplier = 1;
        break;
      case Token.divide:
        getToken(++i);
      case Token.spec_model: // after a slash
        n--;
        if (n < 0 || integerOnly)
          invalidArgument();
        if (theToken.value instanceof Integer || theTok == Token.integer)
          coord[n++] /= (theToken.intValue == Integer.MAX_VALUE ? ((Integer) theToken.value)
              .intValue()
              : theToken.intValue);
        else
          coord[n++] /= ((Float) theToken.value).floatValue();
        coordinatesAreFractional = true;
        break;
      case Token.decimal:
      case Token.spec_model2:
        if (integerOnly)
          invalidArgument();
        if (n == 6)
          invalidArgument();
        coord[n++] = ((Float) theToken.value).floatValue();
        break;
      default:
        invalidArgument();
      }
    }
    if (n < minDim || n > maxDim)
      invalidArgument();
    if (n == 3) {
      Point3f pt = new Point3f(coord[0], coord[1], coord[2]);
      if (coordinatesAreFractional && doConvert && !isSyntaxCheck)
        viewer.toCartesian(pt);
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how to convert?)
        invalidArgument();
      Point4f plane = new Point4f(coord[0], coord[1], coord[2], coord[3]);
      return plane;
    }
    return coord;
  }

  private int theTok;
  private Token theToken;

  private Token getToken(int i) throws ScriptException {
    if (!checkToken(i))
      endOfStatementUnexpected();
    theToken = statement[i];
    theTok = theToken.tok;
    return theToken;
  }

  private int tokAt(int i) {
    return (i < statementLength ? statement[i].tok : Token.nada);
  }

  private boolean checkToken(int i) {
    return (iToken = i) < statementLength;
  }

  /* ****************************************************************************
   * ==============================================================
   * command implementations
   * ==============================================================
   */

  private void help() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String what = (statementLength == 1 ? "" : parameterAsString(1));
    Token t = Token.getTokenFromName(what);
    if (t != null && (t.tok & Token.command) != 0)
      what = "?command=" + what;
    viewer.getHelp(what);
  }

  private void move() throws ScriptException {
    if (statementLength > 11)
      badArgumentCount();
    //rotx roty rotz zoom transx transy transz slab seconds fps
    Vector3f dRot = new Vector3f(floatParameter(1), floatParameter(2),
        floatParameter(3));
    float dZoom = floatParameter(4);
    Vector3f dTrans = new Vector3f(intParameter(5), intParameter(6),
        intParameter(7));
    float dSlab = floatParameter(8);
    float floatSecondsTotal = floatParameter(9);
    int fps = (statementLength == 11 ? intParameter(10) : 30);
    if (isSyntaxCheck)
      return;
    refresh();
    viewer.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  private void moveto() throws ScriptException {
    //moveto time
    //moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius (navCenter) xNav yNav navDepth    
    //moveto [time] { x y z deg} 0 xTrans yTrans (rotCenter) [zoom factor] (navCenter) xNav yNav navDepth    
    //moveto [time] { x y z deg} (rotCenter) [zoom factor] (navCenter) xNav yNav navDepth
    //where zoom factor is z [[+|-|*|/] n] including 0
    //moveto [time] front|back|left|right|top|bottom
    if (statementLength == 2 && isFloatParameter(1)) {
      float f = floatParameter(1);
      if (isSyntaxCheck)
        return;
      if (f > 0)
        refresh();
      viewer.moveTo(f, null, new Point3f(0, 0, 1), 0, 100, 0, 0, 0, null,
          Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    Point3f pt = new Point3f();
    Point3f center = null;
    int i = 1;
    float floatSecondsTotal = (isFloatParameter(i) ? floatParameter(i++) : 2.0f);
    if (floatSecondsTotal < 0)
      invalidArgument();
    float zoom = Float.NaN;
    float xTrans = 0;
    float yTrans = 0;
    float degrees = 90;
    switch (getToken(i).tok) {
    case Token.point3f:
    case Token.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isPoint3f(i)) {
        pt = getPoint3f(i, true);
        i = iToken + 1;
        degrees = floatParameter(i++);
      } else {
        Point4f pt4 = getPoint4f(i);
        i = iToken + 1;
        pt.set(pt4.x, pt4.y, pt4.z);
        degrees = pt4.w;
      }
      break;
    case Token.front:
      pt.set(1, 0, 0);
      degrees = 0f;
      i++;
      break;
    case Token.back:
      pt.set(0, 1, 0);
      degrees = 180f;
      i++;
      break;
    case Token.left:
      pt.set(0, 1, 0);
      i++;
      break;
    case Token.right:
      pt.set(0, -1, 0);
      i++;
      checkStatementLength(i);
      break;
    case Token.top:
      pt.set(1, 0, 0);
      i++;
      checkStatementLength(i);
      break;
    case Token.bottom:
      pt.set(-1, 0, 0);
      i++;
      checkStatementLength(i);
      break;
    default:
      //X Y Z deg
      pt = new Point3f(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }

    boolean isChange = !viewer.isInPosition(pt, degrees);
    //zoom xTrans yTrans (center) rotationRadius 
    float zoom0 = viewer.getZoomPercentFloat();
    if (i != statementLength && !isCenterParameter(i)) {
      zoom = floatParameter(i++);
    }
    if (i != statementLength && !isCenterParameter(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
      if (!isChange && Math.abs(xTrans - viewer.getTranslationXPercent()) >= 1)
        isChange = true;
      if (!isChange && Math.abs(yTrans - viewer.getTranslationYPercent()) >= 1)
        isChange = true;
    }
    float rotationRadius = Float.NaN;
    if (i != statementLength) {
      int ptCenter = i;
      center = centerParameter(i);
      if (!isChange && center.distance(viewer.getRotationCenter()) >= 0.1)
        isChange = true;
      i = iToken + 1;
      if (isFloatParameter(i))
        rotationRadius = floatParameter(i++);
      float radius = viewer.getRotationRadius();
      if (!isCenterParameter(i)) {
        if ((rotationRadius == 0 || Float.isNaN(rotationRadius))
            && (zoom == 0 || Float.isNaN(zoom))) {
          //alternative (atom expression) 0 zoomFactor 
          float factor = Math.abs(getZoomFactor(i, ptCenter, radius, zoom0));
          i = iToken + 1;
          if (Float.isNaN(factor))
            invalidArgument();
          zoom = factor;
        } else {
          if (!isChange
              && Math.abs(rotationRadius - viewer.getRotationRadius()) >= 0.1)
            isChange = true;
        }
      }
    }
    if (zoom == 0 || Float.isNaN(zoom))
      zoom = 100;
    if (Float.isNaN(rotationRadius))
        rotationRadius = 0;

    if (!isChange && Math.abs(zoom - zoom0) >= 1)
      isChange = true;
    // (navCenter) xNav yNav navDepth 

    Point3f navCenter = null;
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;

    if (i != statementLength) {
      navCenter = centerParameter(i);
      i = iToken + 1;
      if (i != statementLength) {
        xNav = floatParameter(i++);
        yNav = floatParameter(i++);
      }
      if (i != statementLength)
        navDepth = floatParameter(i++);
    }

    if (i != statementLength)
      badArgumentCount();

    if (isSyntaxCheck)
      return;
    if (!isChange)
      floatSecondsTotal = 0;
    if (floatSecondsTotal > 0)
      refresh();
    viewer.moveTo(floatSecondsTotal, center, pt, degrees, zoom, xTrans, yTrans,
        rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  private void navigate() throws ScriptException {
    /*
     navigation on/off
     navigation depth p # would be as a depth value, like slab, in percent, but could be negative
     navigation nSec translate X Y  # could be percentages
     navigation nSec translate $object # could be a draw object
     navigation nSec translate (atom selection) #average of values
     navigation nSec center {x y z}
     navigation nSec center $object
     navigation nSec center (atom selection)
     navigation nSec path $object 
     navigation nSec path {x y z theta} {x y z theta}{x y z theta}{x y z theta}...
     navigation nSec trace (atom selection) 
     */
    if (statementLength == 1) {
      setBooleanProperty("navigationMode", true);
      return;
    }
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    Point3f pt;
    if (statementLength == 2) {
      switch (getToken(1).tok) {
      case Token.on:
      case Token.off:
        setBooleanProperty("navigationMode", theTok == Token.on);
      default:
        invalidArgument();
      }
      return;
    }
    if (!viewer.getNavigationMode())
      setBooleanProperty("navigationMode", true);
    for (int i = 1; i < statementLength; i++) {
      float timeSec = (isFloatParameter(i) ? floatParameter(i++) : 2f);
      if (timeSec < 0)
        invalidArgument();
      if (!isSyntaxCheck && timeSec > 0)
        refresh();
      switch (getToken(i).tok) {
      case Token.depth:
        float depth = floatParameter(++i);
        if (!isSyntaxCheck)
          viewer.setNavigationDepthPercent(timeSec, depth);
        continue;
      case Token.center:
        pt = centerParameter(++i);
        i = iToken;
        if (!isSyntaxCheck)
          viewer.navigate(timeSec, pt);
        continue;
      case Token.rotate:
        switch (getToken(++i).tok) {
        case Token.identifier:
          String str = parameterAsString(i++);
          if (str.equalsIgnoreCase("x")) {
            rotAxis.set(1, 0, 0);
            break;
          }
          if (str.equalsIgnoreCase("y")) {
            rotAxis.set(0, 1, 0);
            break;
          }
          if (str.equalsIgnoreCase("z")) {
            rotAxis.set(0, 0, 1);
            break;
          }
          invalidArgument(); // for now
        case Token.point3f:
        case Token.leftbrace:
          rotAxis.set(getPoint3f(i, true));
          i = iToken + 1;
          break;
        }
        float degrees = floatParameter(i);
        if (!isSyntaxCheck)
          viewer.navigate(timeSec, rotAxis, degrees);
        continue;
      case Token.translate:
        float x = Float.NaN;
        float y = Float.NaN;
        if (isFloatParameter(++i)) {
          x = floatParameter(i);
          y = floatParameter(++i);
        } else if (getToken(i).tok == Token.identifier) {
          String str = parameterAsString(i);
          if (str.equalsIgnoreCase("x"))
            x = floatParameter(++i);
          else if (str.equalsIgnoreCase("y"))
            y = floatParameter(++i);
          else
            invalidArgument();
        } else {
          pt = centerParameter(i);
          i = iToken;
          if (!isSyntaxCheck)
            viewer.navTranslate(timeSec, pt);
          continue;
        }
        if (!isSyntaxCheck)
          viewer.navTranslatePercent(timeSec, x, y);
        continue;
      case Token.divide:
        continue;
      case Token.trace:
        Point3f[][] pathGuide;
        Vector vp = new Vector();
        BitSet bs = expression(++i);
        i = iToken;
        if (isSyntaxCheck)
          return;
        viewer.getPolymerPointsAndVectors(bs, vp);
        int n;
        if ((n = vp.size()) > 0) {
          pathGuide = new Point3f[n][];
          for (int j = 0; j < n; j++) {
            pathGuide[j] = (Point3f[]) vp.get(j);
          }
          viewer.navigate(timeSec, pathGuide);
          continue;
        }
        break;
      case Token.identifier:
        Point3f[] path;
        float[] theta = null; //orientation; null for now
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("path")) {
          if (getToken(i + 1).tok == Token.dollarsign) {
            i++;
            //navigate timeSeconds path $id indexStart indexEnd
            String pathID = objectNameParameter(++i);
            if (isSyntaxCheck)
              return;
            setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", pathID);
            path = (Point3f[]) viewer.getShapeProperty(
                JmolConstants.SHAPE_DRAW, "vertices");
            refresh();
            if (path == null)
              invalidArgument();
            int indexStart = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
                : 0);
            int indexEnd = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
                : Integer.MAX_VALUE);
            if (!isSyntaxCheck)
              viewer.navigate(timeSec, path, theta, indexStart, indexEnd);
            continue;
          }
          Vector v = new Vector();
          while (isCenterParameter(i + 1)) {
            v.addElement(centerParameter(++i));
            i = iToken;
          }
          if (v.size() > 0) {
            path = new Point3f[v.size()];
            for (int j = 0; j < v.size(); j++) {
              path[j] = (Point3f) v.get(j);
            }
            if (!isSyntaxCheck)
              viewer.navigate(timeSec, path, theta, 0, Integer.MAX_VALUE);
            continue;
          }
          //possibility here of multiple coord4s?
        }
      //fall through;
      default:
        invalidArgument();
      }
    }
  }

  private void bondorder() throws ScriptException {
    short order = 0;
    switch (getToken(1).tok) {
    case Token.integer:
    case Token.decimal:
      if ((order = JmolConstants.getBondOrderFromFloat(floatParameter(1))) == JmolConstants.BOND_ORDER_NULL)
        invalidArgument();
      break;
    default:
      if ((order = JmolConstants.getBondOrderFromString(parameterAsString(1))) == JmolConstants.BOND_ORDER_NULL)
        invalidArgument();
      // generic partial can be indicated by "partial n.m"
      if (order == JmolConstants.BOND_PARTIAL01 && tokAt(2) == Token.decimal) {
        order = JmolConstants
            .getPartialBondOrderFromInteger(statement[2].intValue);
      }
    }
    setShapeProperty(JmolConstants.SHAPE_STICKS, "bondOrder", new Short(order));
  }

  private void console() throws ScriptException {
    switch (getToken(1).tok) {
    case Token.off:
      if (!isSyntaxCheck)
        viewer.showConsole(false);
      break;
    case Token.on:
      if (isSyntaxCheck)
        break;
      viewer.showConsole(true);
      viewer.clearConsole();
      break;
    default:
      evalError("console ON|OFF");
    }
  }

  private void centerAt() throws ScriptException {
    String relativeTo = null;
    switch (getToken(1).tok) {
    case Token.absolute:
      relativeTo = "absolute";
      break;
    case Token.average:
      relativeTo = "average";
      break;
    case Token.boundbox:
      relativeTo = "boundbox";
      break;
    default:
      invalidArgument();
    }
    Point3f pt = new Point3f(0, 0, 0);
    if (statementLength == 5) {
      // centerAt xxx x y z
      pt.x = floatParameter(2);
      pt.y = floatParameter(3);
      pt.z = floatParameter(4);
    } else if (isCenterParameter(2)) {
      pt = centerParameter(2);
      checkStatementLength(iToken + 1);
    } else {
      checkLength2();
    }
    if (!isSyntaxCheck)
      viewer.setCenterAt(relativeTo, pt);
  }

  private void stereo() throws ScriptException {
    int stereoMode = JmolConstants.STEREO_DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6 
    // stereo redgreen 5

    float degrees = -5;
    boolean degreesSeen = false;
    int[] colors = new int[2];
    int colorpt = 0;
    String state = "";
    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (colorpt > 1)
          badArgumentCount();
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt++] = getArgbParam(i);
        i = iToken;
        if (colorpt == 1)
          colors[colorpt] = ~colors[0];
        state += " " + Escape.escapeColor(colors[colorpt - 1]);
        continue;
      }
      switch (getToken(i).tok) {
      case Token.on:
        checkLength2();
        iToken = 1;
        state = " on";
        break;
      case Token.off:
        checkLength2();
        iToken = 1;
        stereoMode = JmolConstants.STEREO_NONE;
        state = " off";
        break;
      case Token.integer:
      case Token.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        state += " " + degrees;
        break;
      case Token.identifier:
        String id = parameterAsString(i);
        state += " " + id;
        if (!degreesSeen)
          degrees = 3;
        if (id.equalsIgnoreCase("redblue")) {
          stereoMode = JmolConstants.STEREO_REDBLUE;
          break;
        }
        if (id.equalsIgnoreCase("redcyan")) {
          stereoMode = JmolConstants.STEREO_REDCYAN;
          break;
        }
        if (id.equalsIgnoreCase("redgreen")) {
          stereoMode = JmolConstants.STEREO_REDGREEN;
          break;
        }
      // fall into
      default:
        invalidArgument();
      }
    }
    setFloatProperty("stereoDegrees", degrees);
    checkStatementLength(iToken + 1);
    if (isSyntaxCheck)
      return;
    if (colorpt > 0) {
      viewer.setStereoMode(colors, state);
    } else {
      viewer.setStereoMode(stereoMode, state);
    }
  }

  private void connect() throws ScriptException {

    final float[] distances = new float[2];
    BitSet[] atomSets = new BitSet[2];
    atomSets[0] = atomSets[1] = viewer.getSelectionSet();
    float radius = Float.NaN;
    int color = Integer.MIN_VALUE;
    int distanceCount = 0;
    int atomSetCount = 0;
    short bondOrder = JmolConstants.BOND_ORDER_NULL;
    short bo;
    int operation = JmolConstants.CONNECT_MODIFY_OR_CREATE;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    String translucency = null;
    float translucentLevel = Float.MAX_VALUE;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BitSet bsBonds = new BitSet();
    boolean isBonds = false;
    int expression2 = 0;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] 
     *             [<=1 bond type] [<=1 operation]
     * 
     */

    if (statementLength == 1) {
      viewer.rebond();
      return;
    }

    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        color = getArgbParam(i);
        i = iToken;
        isColorOrRadius = true;
        continue;
      }
      switch (getToken(i).tok) {
      case Token.on:
      case Token.off:
        checkLength2();
        if (!isSyntaxCheck)
          viewer.rebond();
        return;
      case Token.integer:
      case Token.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            invalidParameterOrder();
          bo = JmolConstants.getBondOrderFromFloat(floatParameter(i));
          if (bo == JmolConstants.BOND_ORDER_NULL)
            invalidArgument();
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          badArgumentCount();
        distances[distanceCount++] = floatParameter(i);
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (++nAtomSets > 2 || isBonds && nAtomSets > 1)
          badArgumentCount();
        if (haveType || isColorOrRadius)
          invalidParameterOrder();
        atomSets[atomSetCount++] = expression(i);
        if (atomSetCount == 2) {
          int pt = iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == Token.identifier
                && parameterAsString(j).equals("_1")) {
              expression2 = i;
              break;
            }
          iToken = pt;
        }
        isBonds = isBondSet;
        i = iToken; // the for loop will increment i
        break;
      case Token.identifier:
      case Token.hbond:
        String cmd = parameterAsString(i);
        if (cmd.equalsIgnoreCase("pdb")) {
          boolean isAuto = (optParameterAsString(2).equalsIgnoreCase("auto"));
          if (isAuto)
            checkLength3();
          else
            checkLength2();
          if (isSyntaxCheck)
            return;
          viewer.setPdbConectBonding(0, 0, isAuto);
          return;
        }
        if ((bo = JmolConstants.getBondOrderFromString(cmd)) == JmolConstants.BOND_ORDER_NULL) {
          // must be an operation and must be last argument
          haveOperation = true;
          if (++i != statementLength)
            invalidParameterOrder();
          if ((operation = JmolConstants.connectOperationFromString(cmd)) < 0)
            invalidArgument();
          if (operation == JmolConstants.CONNECT_AUTO_BOND
              && !(bondOrder == JmolConstants.BOND_ORDER_NULL
                  || bondOrder == JmolConstants.BOND_H_REGULAR || bondOrder == JmolConstants.BOND_AROMATIC))
            invalidArgument();
          break;
        }
        // must be bond type
        if (haveType)
          incompatibleArguments();
        haveType = true;
        if (bo == JmolConstants.BOND_PARTIAL01) {
          switch (tokAt(i + 1)) {
          case Token.decimal:
            bo = JmolConstants
                .getPartialBondOrderFromInteger(statement[++i].intValue);
            break;
          case Token.integer:
            bo = (short) intParameter(++i);
            break;
          }
        }
        bondOrder = bo;
        break;
      case Token.translucent:
      case Token.opaque:
        if (translucency != null)
          invalidArgument();
        isColorOrRadius = true;
        translucency = parameterAsString(i);
        if (theTok == Token.translucent && isFloatParameter(i + 1))
          translucentLevel = floatParameter(++i);
        break;
      case Token.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case Token.none:
      case Token.delete:
        if (++i != statementLength)
          invalidParameterOrder();
        operation = JmolConstants.CONNECT_DELETE_BONDS;
        if (isColorOrRadius)
          invalidArgument();
        isDelete = true;
        break;
      default:
        invalidArgument();
      }
    }
    if (isSyntaxCheck)
      return;
    if (distanceCount < 2) {
      if (distanceCount == 0)
        distances[0] = JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE;
      distances[1] = distances[0];
      distances[0] = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (translucency != null || !Float.isNaN(radius)
        || color != Integer.MIN_VALUE) {
      if (!haveType)
        bondOrder = JmolConstants.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = JmolConstants.CONNECT_MODIFY_ONLY;
    }
    int nNew = 0;
    int nModified = 0;
    int[] result;
    if (expression2 > 0) {
      BitSet bs = new BitSet();
      variables.put("_1", bs);
      for (int atom1 = atomSets[0].size(); atom1 >= 0; atom1--)
        if (atomSets[0].get(atom1)) {
          bs.set(atom1);
          result = viewer.makeConnections(distances[0], distances[1],
              bondOrder, operation, bs, expression(expression2), bsBonds,
              isBonds);
          nNew += result[0];
          nModified += result[1];
          bs.clear(atom1);
        }
    } else {
      result = viewer.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds);
      nNew += result[0];
      nModified += result[1];
    }
    if (isDelete) {
      if (!(tQuiet || scriptLevel > scriptReportingLevel))
        scriptStatus(GT._("{0} connections deleted", nModified));
      return;
    }
    if (isColorOrRadius) {
      viewer.selectBonds(bsBonds);
      if (!Float.isNaN(radius))
        viewer.setShapeSize(JmolConstants.SHAPE_STICKS, (int) (radius * 2000),
            bsBonds);
      if (color != Integer.MIN_VALUE)
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "color",
            new Integer(color), bsBonds);
      if (translucency != null) {
        if (translucentLevel == Float.MAX_VALUE)
          translucentLevel = viewer.getDefaultTranslucent();
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "translucentLevel",
            new Float(translucentLevel));
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "translucency",
            translucency, bsBonds);
      }
    }
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatus(GT._("{0} new bonds; {1} modified", new Object[] {
          new Integer(nNew), new Integer(nModified) }));
  }

  private void getProperty() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String param = optParameterAsString(2);
    int tok = tokAt(2);
    BitSet bs = (tok == Token.expressionBegin || tok == Token.bitset ? expression(2)
        : null);
    int propertyID = PropertyManager.getPropertyNumber(property);
    if (property.length() > 0 && propertyID < 0) {
      property = ""; // produces a list from Property Manager
      param = "";
    } else if (propertyID >= 0 && statementLength < 3) {
      param = PropertyManager.getDefaultParam(propertyID);
      if (param.equals("(visible)")) {
        viewer.setModelVisibility();
        bs = viewer.getVisibleSet();
      }
    }
    retValue = (String) viewer.getProperty("readable", property,
        (bs == null ? (Object) param : (Object) bs));
    showString(retValue);
  }

  private void background(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (isColorParam(i) || theTok == Token.none) {
      argb = getArgbParamLast(i, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    }
    int iShape = getShapeType(theTok);
    colorShape(iShape, i + 1, true);
  }

  private void center(int i) throws ScriptException {
    // from center (atom) or from zoomTo under conditions of not windowCentered()
    if (statementLength == 1) {
      viewer.setNewRotationCenter((Point3f) null);
      return;
    }
    Point3f center = centerParameter(i);
    if (center == null)
      invalidArgument();
    if (!isSyntaxCheck)
      viewer.setNewRotationCenter(center);
  }

  private void color() throws ScriptException {
    int argb;
    if (isColorParam(1)) {
      colorObject(Token.atoms, 1);
      return;
    }
    switch (getToken(1).tok) {
    case Token.dollarsign:
      colorNamedObject(2);
      return;
    case Token.none:
    case Token.spacefill:
    case Token.amino:
    case Token.chain:
    case Token.group:
    case Token.shapely:
    case Token.structure:
    case Token.temperature:
    case Token.fixedtemp:
    case Token.formalCharge:
    case Token.partialCharge:
    case Token.surfacedistance:
    case Token.vanderwaals:
    case Token.monomer:
    case Token.molecule:
    case Token.altloc:
    case Token.insertion:
    case Token.translucent:
    case Token.opaque:
    case Token.jmol:
    case Token.rasmol:
    case Token.user:
    case Token.property:
      colorObject(Token.atoms, 1);
      return;
    case Token.string:
      String strColor = stringParameter(1);
      setStringProperty("propertyColorSchemeOverLoad", strColor);
      if (tokAt(2) == Token.range || tokAt(2) == Token.absolute) {
        float min = floatParameter(3);
        float max = floatParameter(4);
        if (!isSyntaxCheck)
          viewer.setCurrentColorRange(min, max);
      }
      return;
    case Token.range:
    case Token.absolute:
      checkLength4();
      float min = floatParameter(2);
      float max = floatParameter(3);
      if (!isSyntaxCheck)
        viewer.setCurrentColorRange(min, max);
      return;
    case Token.background:
      argb = getArgbParamLast(2, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    case Token.bitset:
    case Token.expressionBegin:
      colorObject(Token.atoms, -1);
      return;
    case Token.rubberband:
      argb = getArgbParamLast(2, false);
      if (!isSyntaxCheck)
        viewer.setRubberbandArgb(argb);
      return;
    case Token.selectionHalo:
      int i = 2;
      if (tokAt(2) == Token.opaque)
        i++;
      argb = getArgbParamLast(i, true);
      if (isSyntaxCheck)
        return;
      viewer.loadShape(JmolConstants.SHAPE_HALOS);
      setShapeProperty(JmolConstants.SHAPE_HALOS, "argbSelection", new Integer(
          argb));
      return;
    case Token.axes:
    case Token.boundbox:
    case Token.unitcell:
    case Token.identifier:
    case Token.hydrogen:
      //color element
      String str = parameterAsString(1);
      argb = getArgbOrPaletteParam(2);
      checkStatementLength(iToken + 1);
      if (str.equalsIgnoreCase("axes")) {
        setStringProperty("axesColor", Escape.escapeColor(argb));
        return;
      } else if (StateManager.getObjectIdFromName(str) >= 0) {
        if (!isSyntaxCheck)
          viewer.setObjectArgb(str, argb);
        return;
      }
      if (changeElementColor(str, argb))
        return;
      invalidArgument();
    default:
      colorObject(theTok, 2);
    }
  }

  private boolean changeElementColor(String str, int argb) {
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase(JmolConstants.elementNameFromNumber(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = JmolConstants.altElementMax; --i >= 0;) {
      if (str.equalsIgnoreCase(JmolConstants.altElementNameFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
    }
    if (str.charAt(0) != '_')
      return false;
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase("_" + JmolConstants.elementSymbolFromNumber(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = JmolConstants.altElementMax; --i >= JmolConstants.firstIsotope;) {
      if (str
          .equalsIgnoreCase("_" + JmolConstants.altElementSymbolFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
      if (str
          .equalsIgnoreCase("_" + JmolConstants.altIsotopeSymbolFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
    }
    return false;
  }

  private void colorNamedObject(int index) throws ScriptException {
    // color $ whatever green
    int shapeType = setShapeByNameParameter(index);
    colorShape(shapeType, index + 1, false);
  }

  private void colorObject(int tokObject, int index) throws ScriptException {
    colorShape(getShapeType(tokObject), index, false);
  }

  private void colorShape(int shapeType, int index, boolean isBackground)
      throws ScriptException {
    String translucency = null;
    Object colorvalue = null;
    BitSet bs = null;
    String prefix = "";
    int typeMask = 0;
    float translucentLevel = Float.MAX_VALUE;
    if (index < 0) {
      bs = expression(-index);
      index = iToken + 1;
      if (isBondSet)
        shapeType = JmolConstants.SHAPE_STICKS;
    }
    if (isBackground)
      getToken(index);
    else if ((isBackground = (getToken(index).tok == Token.background)) == true)
      getToken(++index);
    if (isBackground)
      prefix = "bg";
    if (!isSyntaxCheck && shapeType == JmolConstants.SHAPE_MO && !mo(true))
      return;
    if (theTok == Token.translucent || theTok == Token.opaque) {
      translucency = parameterAsString(index++);
      if (theTok == Token.translucent && isFloatParameter(index))
        translucentLevel = floatParameter(index++);
    }
    if (index < statementLength && tokAt(index) != Token.on
        && tokAt(index) != Token.off) {
      int tok = getToken(index).tok;
      if (isColorParam(index)) {
        int argb = getArgbParam(index, false);
        colorvalue = (argb == 0 ? null : new Integer(argb));
        if (translucency == null && tokAt(index = iToken + 1) != Token.nada) {
          getToken(index);
          if (translucency == null
              && (theTok == Token.translucent || theTok == Token.opaque)) {
            translucency = parameterAsString(index);
            if (theTok == Token.translucent && isFloatParameter(index + 1))
              translucentLevel = floatParameter(++index);
          }
          //checkStatementLength(index + 1);
          //iToken = index;
        }
      } else if (shapeType == JmolConstants.SHAPE_LCAOCARTOON) {
        iToken--; //back up one
      } else {
        // must not be a color, but rather a color SCHEME
        // this could be a problem for properties, which can't be
        // checked later -- they must be turned into a color NOW.

        // "cpk" value would be "spacefill"
        String name = parameterAsString(index).toLowerCase();
        boolean isByElement = (name.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0);
        boolean isColorIndex = (isByElement || name
            .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
        byte pid = (isColorIndex || shapeType == JmolConstants.SHAPE_ISOSURFACE ? JmolConstants.PALETTE_PROPERTY
            : tok == Token.spacefill ? JmolConstants.PALETTE_CPK
                : JmolConstants.getPaletteID(name));
        // color atoms "cpkScheme"
        if (pid == JmolConstants.PALETTE_UNKNOWN
            || pid == JmolConstants.PALETTE_TYPE
            && shapeType != JmolConstants.SHAPE_HSTICKS)
          invalidArgument();
        Object data = null;
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          if (isColorIndex) {
            if (!isSyntaxCheck) {
              data = getBitsetProperty(null, (isByElement ? Token.elemno
                  : Token.groupID)
                  | Token.minmaxmask, null, null, null, null, false);
            }
          } else {
            if (!isColorIndex && shapeType != JmolConstants.SHAPE_ISOSURFACE)
              index++;
            if (name.equals("property")
                && Compiler.tokAttr(getToken(index).tok, Token.atomproperty)) {
              if (!isSyntaxCheck) {
                data = getBitsetProperty(null, getToken(index++).tok
                    | Token.minmaxmask, null, null, null, null, false);
                if (!(data instanceof float[]))
                  invalidArgument();
              }
            }
          }
        } else if (pid == JmolConstants.PALETTE_VARIABLE) {
          index++;
          name = parameterAsString(index++);
          data = new float[viewer.getAtomCount()];
          Parser.parseFloatArray("" + getParameter(name, false), null,
              (float[]) data);
          pid = JmolConstants.PALETTE_PROPERTY;
        }
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          String scheme = (tokAt(index) == Token.string ? parameterAsString(
              index++).toLowerCase() : null);
          if (scheme != null) {
            setStringProperty("propertyColorScheme", scheme);
            isColorIndex = (scheme.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0 || scheme
                .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
          }
          float min = 0;
          float max = Float.MAX_VALUE;
          if (!isColorIndex
              && (tokAt(index) == Token.absolute || tokAt(index) == Token.range)) {
            min = floatParameter(index + 1);
            max = floatParameter(index + 2);
            index += 3;
            if (min == max && shapeType == JmolConstants.SHAPE_ISOSURFACE) {
              float[] range = (float[]) viewer.getShapeProperty(shapeType,
                  "dataRange");
              if (range != null) {
                min = range[0];
                max = range[1];
              }
            } else if (min == max)
              max = Float.MAX_VALUE;
          }
          if (!isSyntaxCheck) {
            if (shapeType != JmolConstants.SHAPE_ISOSURFACE
                && max != -Float.MAX_VALUE) {
              if (data == null)
                viewer.setCurrentColorRange(name);
              else
                viewer.setCurrentColorRange((float[]) data, null);
            }
            if (max != Float.MAX_VALUE)
              viewer.setCurrentColorRange(min, max);
          }
          if (shapeType == JmolConstants.SHAPE_ISOSURFACE)
            prefix = "remap";
        } else {
          index++;
        }
        colorvalue = new Byte((byte) pid);
        checkStatementLength(index);
      }
      if (isSyntaxCheck || shapeType < 0)
        return;

      //ok, the following five options require precalculation.
      //the state must not save them as paletteIDs, only as pure
      //color values. 
      switch (tok) {
      case Token.surfacedistance:
        viewer.getSurfaceDistanceMax();
        break;
      case Token.temperature:
        if (viewer.isRangeSelected())
          viewer.clearBfactorRange();
        break;
      case Token.group:
        viewer.calcSelectedGroupsCount();
        break;
      case Token.monomer:
        viewer.calcSelectedMonomersCount();
        break;
      case Token.molecule:
        viewer.calcSelectedMoleculesCount();
        break;
      }
      typeMask = (shapeType == JmolConstants.SHAPE_HSTICKS ? JmolConstants.BOND_HYDROGEN_MASK
          : shapeType == JmolConstants.SHAPE_SSSTICKS ? JmolConstants.BOND_SULFUR_MASK
              : shapeType == JmolConstants.SHAPE_STICKS ? JmolConstants.BOND_COVALENT_MASK
                  : 0);
      if (typeMask == 0) {
        viewer.loadShape(shapeType);
        if (bs != null)
          viewer.setShapeProperty(shapeType, prefix + "color", colorvalue, bs);
        else
          viewer.setShapeProperty(shapeType, prefix + "color", colorvalue);
      } else {
        if (bs != null)
          viewer.selectBonds(bs);
        setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
            typeMask));
        setShapeProperty(JmolConstants.SHAPE_STICKS, prefix + "color",
            colorvalue);
      }
    }
    if (translucency != null)
      setShapeTranslucency(shapeType, prefix, translucency, translucentLevel);
    if (typeMask != 0)
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
          JmolConstants.BOND_COVALENT_MASK));
  }

  private void setShapeTranslucency(int shapeType, String prefix,
                                    String translucency, float translucentLevel) {
    if (translucentLevel == Float.MAX_VALUE)
      translucentLevel = viewer.getDefaultTranslucent();
    setShapeProperty(shapeType, "translucentLevel", new Float(translucentLevel));
    if (prefix != null)
      setShapeProperty(shapeType, prefix + "translucency", translucency);
  }

  private Hashtable variables = new Hashtable();
  private Object[] data;

  private void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (iToken = statementLength) {
    case 5:
      //parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = parameterAsString(2);
    //fall through
    case 4:
    case 2:
      dataLabel = parameterAsString(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!isSyntaxCheck)
          viewer.setData(null, null, 0, 0, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = "" + getParameter(dataLabel.substring(i + 1), false);
        dataLabel = dataLabel.substring(0, i).trim();
      } else if (dataString == null && (i = dataLabel.indexOf(" ")) >= 0) {
        dataString = dataLabel.substring(i + 1).trim();
        dataLabel = dataLabel.substring(0, i).trim();
        isOneValue = true;
      }
      break;
    default:
      badArgumentCount();
    }
    String dataType = dataLabel + " ";
    dataType = dataType.substring(0, dataType.indexOf(" "));
    data = new Object[3];
    data[0] = dataLabel;
    data[1] = dataString;
    boolean isModel = dataType.equalsIgnoreCase("model");
    boolean isAppend = dataType.equalsIgnoreCase("append");
    if (!isSyntaxCheck || isScriptCheck && (isModel || isAppend)
        && fileOpenCheck) {
      if (dataType.toLowerCase().indexOf("property_") == 0) {
        BitSet bs = viewer.getSelectionSet();
        int dataField = isOneValue ? Integer.MIN_VALUE : ((Integer) viewer
            .getParameter("propertyDataField")).intValue();
        int matchField = isOneValue ? 0 : ((Integer) viewer
            .getParameter("propertyAtomNumberField")).intValue();
        int atomCount = viewer.getAtomCount();
        int[] atomMap = null;
        BitSet bsAtoms = new BitSet(atomCount);
        if (matchField > 0) {
          atomMap = new int[atomCount + 1];
          for (int j = 0; j <= atomCount; j++)
            atomMap[j] = -1;
          for (int j = 0; j < atomCount; j++) {
            if (!bs.get(j))
              continue;
            int atomNo = viewer.getAtomNumber(j);
            if (atomNo > atomCount + 1 || atomNo < 0 || bsAtoms.get(atomNo))
              continue;
            bsAtoms.set(atomNo);
            atomMap[atomNo] = j;
          }
          data[2] = atomMap;
        } else {
          data[2] = bs;
        }
        viewer.setData(dataType, data, atomCount, matchField, dataField);
      } else {
        viewer.setData(dataType, data, 0, 0, 0);
      }
    }
    if ((isModel || isAppend) && dataString == null)
      invalidArgument();
    if ((isModel || isAppend)
        && (!isSyntaxCheck || isScriptCheck && fileOpenCheck)) {
      // only if first character is "|" do we consider "|" to be new line
      char newLine = viewer.getInlineChar();
      if (dataString.length() > 0 && dataString.charAt(0) != newLine)
        newLine = '\0';
      viewer.loadInline(dataString, newLine, isAppend);
    }
    if (Parser
        .isOneOf(
            dataType.toLowerCase(),
            "coord;formalcharge;occupany;partialcharge;temperature;valency;vibrationvector;"))
      viewer.loadData(dataType, dataString);
  }

  private void define() throws ScriptException {
    // note that the standard definition depends upon the 
    // current state. Once defined, a variable is the set
    // of atoms that matches the definition at that time. 
    // adding DYMAMIC_ to the beginning of the definition 
    // allows one to create definitions that are recalculated
    // whenever they are used. When used, "DYNAMIC_" is dropped
    // so, for example: 
    //   define DYNAMIC_what selected and visible
    // and then 
    //   select what
    // will return different things at different times depending
    // upon what is selected and what is visible
    // but 
    //   define what selected and visible
    // will evaluate the moment it is defined and then represent
    // that set of atoms forever.

    String variable = (String) getToken(1).value;
    BitSet bs = expression(2);
    if (isSyntaxCheck)
      return;
    boolean isDynamic = (variable.indexOf("dynamic_") == 0);
    if (isDynamic) {
      Token[] code = new Token[statementLength];
      for (int i = statementLength; --i >= 0;)
        code[i] = statement[i];
      variables.put("!" + variable.substring(8), code);
      viewer.addStateScript(thisCommand, false);
    } else {
      assignBitsetVariable(variable, bs);
    }
  }

  private void echo(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String text = optParameterAsString(index);
    if (viewer.getEchoStateActive())
      setShapeProperty(JmolConstants.SHAPE_ECHO, "text", text);
    if (viewer.getRefreshing())
      showString(viewer.formatText(text));
  }

  private void message() throws ScriptException {
    String text = parameterAsString(1);
    if (isSyntaxCheck)
      return;
    String s = viewer.formatText(text);
    if (outputBuffer == null)
      Logger.warn(s);
    if (!s.startsWith("_"))
      scriptStatus(s);
  }

  private void scriptStatus(String s) {
    if (outputBuffer != null) {
      outputBuffer.append(s).append('\n');
      return;
    }
    viewer.scriptStatus(s);
  }

  private void pause() throws ScriptException {
    if (isSyntaxCheck)
      return;
    pauseExecution();
    String msg = optParameterAsString(1);
    if (msg.length() == 0)
      msg = ": RESUME to continue.";
    else
      msg = ": " + viewer.formatText(msg);
    viewer.scriptStatus("script execution paused" + msg);
  }

  private void label(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(index++);
    if (strLabel.equalsIgnoreCase("on")) {
      strLabel = viewer.getStandardLabelFormat();
    } else if (strLabel.equalsIgnoreCase("off")) {
      strLabel = null;
    } 
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setLabel(strLabel);
  }

  private void hover() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(1);
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "label", strLabel);
  }

  private void load() throws ScriptException {
    boolean isAppend = false;
    int modelCount = viewer.getModelCount()
        - (viewer.getFileName().equals("zapped") ? 1 : 0);
    boolean appendNew = viewer.getAppendNew();
    StringBuffer loadScript = new StringBuffer("load");
    int[] params = new int[4];
    int nFiles = 1;
    Point3f unitCells = viewer.getDefaultLattice();
    Hashtable htParams = new Hashtable();
    if (viewer.getApplySymmetryToBonds())
      htParams.put("applySymmetryToBonds", Boolean.TRUE);
    htParams.put("params", params);
    //params[0] will be a designated model number or -1 for a trajectory
    params[1] = (int) unitCells.x;
    params[2] = (int) unitCells.y;
    params[3] = (int) unitCells.z;
    int i = 1;
    // ignore optional file format
    //    String filename = "";
    String modelName = "fileset";
    if (statementLength == 1) {
      i = 0;
    } else {
      if (tokAt(1) == Token.identifier
          || parameterAsString(1).equals("fileset")) {
        // 
        modelName = parameterAsString(1);
        if (modelName.equals("menu")) {
          checkLength3();
          if (!isSyntaxCheck)
            viewer.setMenu(parameterAsString(2), true);
          return;
        }
        i = 2;
        loadScript.append(" " + modelName);
        isAppend = (modelName.equalsIgnoreCase("append"));
        if (modelName.equalsIgnoreCase("trajectory"))
          params[0] = -1;
      }
      if (getToken(i).tok != Token.string)
        filenameExpected();
    }
    String filename;
    // long timeBegin = System.currentTimeMillis();
    if (statementLength == i + 1) {
      if (i == 0 || (filename = parameterAsString(i)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap();
        return;
      }
      if (filename.equals("string[]"))
        return;
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck) {
        viewer.openFile(filename, htParams, null, isAppend);
        loadScript.append(" ");
        if (!filename.equals("string") && !filename.equals("string[]"))
          loadScript.append("/*file*/");
        loadScript.append(
            Escape.escape(modelName = (String) htParams.get("fullPathName")));
      }
    } else if (getToken(i + 1).tok == Token.leftbrace
        || theTok == Token.point3f || theTok == Token.integer
        || theTok == Token.identifier) {
      if ((filename = parameterAsString(i++)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap();
        return;
      }
      if (filename.equals("string[]"))
        return;
      int tok;
      String sOptions = "";
      if ((tok = tokAt(i)) == Token.identifier
          && parameterAsString(i).equalsIgnoreCase("manifest")) {
        String manifest = stringParameter(++i);
        htParams.put("manifest", manifest);
        sOptions += " MANIFEST " + Escape.escape(manifest);
        tok = tokAt(++i);
      }
      if (tok == Token.integer) {
        params[0] = intParameter(i);
        sOptions += " " + params[0];
        tok = tokAt(++i);
      }
      if (tok == Token.leftbrace || tok == Token.point3f) {
        unitCells = getPoint3f(i, false);
        i = iToken + 1;
        params[1] = (int) unitCells.x;
        params[2] = (int) unitCells.y;
        params[3] = (int) unitCells.z;
        sOptions += " " + Escape.escape(unitCells);
        int iGroup = -1;
        int[] p;
        float distance = 0;
        /*
         * # Jmol 11.3.9 introduces the capability of visualizing the close contacts 
         * around a crystalline protein (or any other cyrstal structure) that are to 
         * atoms that are in proteins in adjacent unit cells or adjacent to the 
         * protein itself. The option RANGE x, where x is a distance in angstroms, 
         * placed right after the braces containing the set of unit cells to load 
         * does this. The distance, if a positive number, is the maximum distance 
         * away from the closest atom in the {1 1 1} set. If the distance x is a 
         * negative number, then -x is the maximum distance from the {not symmetry} set. 
         * The difference is that in the first case the primary unit cell (555) is 
         * first filled as usual, using symmetry operators, and close contacts to 
         * this set are found. In the second case, only the file-based atoms (
         * Jones-Faithful operator x,y,z) are initially included, then close 
         * contacts to that set are found. Depending upon the application, one or the 
         * other of these options may be desirable.
         */
        if (tokAt(i) == Token.range) {
          i++;
          distance = floatParameter(i++);
          sOptions += " range " + distance;
        }
        htParams.put("symmetryRange", new Float(distance));
        if (tokAt(i) == Token.spacegroup) {
          ++i;
          String spacegroup = TextFormat.simpleReplace(parameterAsString(i++),
              "''", "\"");
          sOptions += " spacegroup " + Escape.escape(spacegroup);
          if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
            iGroup = -999;
          } else {
            if (spacegroup.indexOf(",") >= 0) //Jones Faithful
              if ((unitCells.x < 9 && unitCells.y < 9 && unitCells.z == 0))
                spacegroup += "#doNormalize=0";
            iGroup = SpaceGroup.determineSpaceGroupIndex(spacegroup);
            if (iGroup == -1)
              evalError(GT._("space group {0} was not found.", spacegroup));
          }
          p = new int[5];
          for (int j = 0; j < 4; j++)
            p[j] = params[j];
          p[4] = iGroup;
          params = p;
          htParams.put("params", params);
        }
        if (tokAt(i) == Token.unitcell) {
          ++i;
          p = new int[11];
          for (int j = 0; j < params.length; j++)
            p[j] = params[j];
          p[4] = iGroup;
          float[] fparams = new float[6];
          i = floatParameterSet(i, fparams);
          sOptions += " unitcell {";
          for (int j = 0; j < 6; j++) {
            p[5 + j] = (int) (fparams[j] * 10000f);
            sOptions += (j == 0 ? "" : " ") + p[5 + j];
          }
          sOptions += "}";
          params = p;
          htParams.put("params", params);
        }
      }
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck) {
        viewer.openFile(filename, htParams, null, isAppend);
        loadScript.append(" ");
        if (!filename.equals("string") && !filename.equals("string[]"))
          loadScript.append("/*file*/");
        loadScript.append(
            Escape.escape(modelName = (String) htParams.get("fullPathName")));
        loadScript.append(sOptions);
      }
    } else {
      if (i != 2) {
        modelName = parameterAsString(i++);
        loadScript.append(" ").append(Escape.escape(modelName));
      }
      String[] filenames = new String[statementLength - i];
      int ipt = 0;
      while (i < statementLength) {
        filename = parameterAsString(i);
        filenames[ipt++] = filename;
        i++;
      }
      nFiles = filenames.length;
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck) {
        viewer.openFiles(modelName, filenames, null, isAppend);
        for (i = 0; i < nFiles; i++)
          loadScript.append(" /*file*/").append(Escape.escape(filenames[i]));
      }
    }
    if (isSyntaxCheck && !(isScriptCheck && fileOpenCheck))
      return;
    viewer.addLoadScript(loadScript.toString());
    String errMsg = viewer.getOpenFileError(isAppend);
    // int millis = (int)(System.currentTimeMillis() - timeBegin);
    // Logger.debug("!!!!!!!!! took " + millis + " ms");
    if (errMsg != null && !isScriptCheck)
      evalError(errMsg);

    if (isAppend && (appendNew || nFiles > 1)) {
      viewer.setAnimationRange(-1, -1);
      viewer.setCurrentModelIndex(modelCount);
    }
    if (logMessages)
      scriptStatus("Successfully loaded:" + modelName);
    String defaultScript = viewer.getDefaultLoadScript();
    String msg = "";
    if (defaultScript.length() > 0)
      msg += "\nUsing defaultLoadScript: " + defaultScript;
    String script = viewer.getModelSetProperty("jmolscript");
    if (script != null && viewer.getAllowEmbeddedScripts()) {
      msg += "\nAdding embedded #jmolscript: " + script;
      defaultScript += ";" + script;
      defaultScript = "allowEmbeddedScripts = false;" + defaultScript
          + ";allowEmbeddedScripts = true;";
    }
    if (msg.length() > 0)
      Logger.info(msg);
    if (defaultScript.length() > 0 && !isScriptCheck) //NOT checking embedded scripts here
      runScript(defaultScript);
  }

  private String getFullPathName() throws ScriptException {
    String filename = (!isSyntaxCheck || isScriptCheck && fileOpenCheck ? viewer
        .getFullPathName()
        : "test.xyz");
    if (filename == null)
      invalidArgument();
    return filename;
  }

  private void dataFrame(int datatype) throws ScriptException {
    String type = "";
    boolean isQuaternion = false;
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
      type = "ramachandran";
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      type = (statementLength == 1 ? "w" : optParameterAsString(1));
      boolean isDerivative = (optParameterAsString(statementLength - 1)
          .indexOf("deriv") == 0);
      if (isDerivative && statementLength == 2)
        type = "w";
      if (!Parser.isOneOf(type, "w;x;y;z"))
        evalError("QUATERNION [w,x,y,z] [derivative]");
      type = "quaternion " + type + (isDerivative ? " derivative" : "");
      isQuaternion = true;
      break;
    }
    if (isSyntaxCheck) //just in case we later add parameter options to this
      return;
    //for now, just one frame visible
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      multipleModelsNotOK(type);
    modelIndex = viewer.getJmolDataSourceFrame(modelIndex);
    int ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, type);
    if (isQuaternion && ptDataFrame < 0 && statementLength == 1)
      ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, "quaternion");
    if (ptDataFrame > 0) {
      // data frame can't be 0.
      viewer.setCurrentModelIndex(ptDataFrame, true);
      //      BitSet bs2 = viewer.getModelAtomBitSet(ptDataFrame);
      //    bs2.and(bs);
      //need to be able to set data directly as well.
      //  viewer.display(BitSetUtil.setAll(viewer.getAtomCount()), bs2, tQuiet);
      return;
    }
    String[] savedFileInfo = viewer.getFileInfo();
    boolean oldAppendNew = viewer.getAppendNew();
    viewer.setAppendNew(true);
    String data = viewer.getPdbData(modelIndex, type);
    boolean isOK = (data != null && viewer.loadInline(data, '\n', true));
    viewer.setAppendNew(oldAppendNew);
    viewer.setFileInfo(savedFileInfo);
    if (!isOK)
      return;
    int modelCount = viewer.getModelCount();
    viewer.setJmolDataFrame(type, modelIndex, modelCount - 1);
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
    default:
      viewer.setFrameTitle(modelCount - 1, "ramachandran plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      runScript("frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0; set rotationRadius 260;"
          + "draw ramaAxisX" + modelCount + " {200 0 0} {-200 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 200 0} {0 -200 0} \"psi\";");
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      viewer.setFrameTitle(modelCount - 1, type + " for model "
          + viewer.getModelNumberDotted(modelIndex));
      runScript("frame 0.0; frame last; reset; set rotationRadius 12;"
          + "select visible; trace 0.1; wireframe 0; color trace structure;"
          + "isosurface quatSphere" + modelCount
          + " resolution 1.0 sphere 10.0 mesh nofill translucent 0.8;set rotationRadius 12");
      break;
    }
    viewer.loadShape(JmolConstants.SHAPE_ECHO);
    viewer.addStateScript("frame " + viewer.getModelNumberDotted(modelIndex)
        + "; " + type + ";", false);
    showString("frame " + viewer.getModelNumberDotted(modelCount - 1) + " created: "
        + type);
  }

  //measure() see monitor()

  private void monitor() throws ScriptException {
    int[] countPlusIndexes = new int[5];
    float[] rangeMinMax = new float[2];
    if (statementLength == 1) {
      viewer.hideMeasurements(false);
      return;
    }
    switch (statementLength) {
    case 2:
      switch (getToken(1).tok) {
      case Token.on:
        if (!isSyntaxCheck)
          viewer.hideMeasurements(false);
        return;
      case Token.off:
        if (!isSyntaxCheck)
          viewer.hideMeasurements(true);
        return;
      case Token.delete:
        if (!isSyntaxCheck)
          viewer.clearAllMeasurements();
        return;
      case Token.string:
        if (!isSyntaxCheck)
          viewer.setMeasurementFormats(stringParameter(1));
        return;
      }
      keywordExpected("ON, OFF, DELETE");
    case 3: //measure delete N
      if (getToken(1).tok == Token.delete) {
        if (getToken(2).tok == Token.all) {
          if (!isSyntaxCheck)
            viewer.clearAllMeasurements();
        } else {
          int i = intParameter(2) - 1;
          if (!isSyntaxCheck)
            viewer.deleteMeasurement(i);
        }
        return;
      }
    }
    int nAtoms = 0;
    int expressionCount = 0;
    int atomIndex = -1;
    int atomNumber = 0;
    int ptFloat = -1;
    rangeMinMax[0] = Float.MAX_VALUE;
    rangeMinMax[1] = Float.MAX_VALUE;
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isExpression = false;
    boolean isDelete = false;
    boolean isRange = true;
    boolean isON = false;
    boolean isOFF = false;
    String strFormat = null;
    Vector monitorExpressions = new Vector();

    BitSet bs = new BitSet();

    for (int i = 1; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      case Token.on:
        if (isON || isOFF || isDelete)
          invalidArgument();
        isON = true;
        continue;
      case Token.off:
        if (isON || isOFF || isDelete)
          invalidArgument();
        isOFF = true;
        continue;
      case Token.delete:
        if (isON || isOFF || isDelete)
          invalidArgument();
        isDelete = true;
        continue;
      case Token.range:
        isRange = true; //unnecessary
        atomIndex = -1;
        isAll = true;
        continue;
      case Token.identifier:
        if (parameterAsString(i).equalsIgnoreCase("ALLCONNECTED"))
          isAllConnected = true;
        else
          keywordExpected("ALL, ALLCONNECTED, DELETE");
      // fall through
      case Token.all:
        atomIndex = -1;
        isAll = true;
        continue;
      case Token.string:
        //measures "%a1 %a2 %v %u"
        strFormat = stringParameter(i);
        continue;
      case Token.decimal:
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = floatParameter(i);
        continue;
      case Token.integer:
        isRange = true; // irrelevant if just four integers
        atomNumber = intParameter(i);
        atomIndex = viewer.getAtomIndexFromAtomNumber(atomNumber);
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = atomNumber;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        isExpression = true;
        bs = expression(i);
        i = iToken;
        atomIndex = BitSetUtil.firstSetBit(bs);
        break;
      default:
        expressionOrIntegerExpected();
      }
      //only here for point definition
      if (atomIndex == -1 || isAll && (bs == null || bs.size() == 0)) {
        if (!isSyntaxCheck) {
          if (isExpression)
            return; //there's just nothing to measure
          evalError(GT._("bad atom number"));
        }
      }
      if (isAll) {
        if (++expressionCount > 4)
          badArgumentCount();
        monitorExpressions.addElement(bs);
        nAtoms = expressionCount;
      } else {
        if (++nAtoms > 4)
          badArgumentCount();
        countPlusIndexes[nAtoms] = atomIndex;
      }
    }
    countPlusIndexes[0] = nAtoms;
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isAll) {
      if (!isExpression)
        expressionExpected();
      if (isRange && rangeMinMax[1] < rangeMinMax[0]) {
        rangeMinMax[1] = rangeMinMax[0];
        rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE
            : -200F);
      }
      if (!isSyntaxCheck)
        viewer.defineMeasurement(monitorExpressions, rangeMinMax, isDelete,
            isAllConnected, isON || isOFF, isOFF, strFormat);
      return;
    }
    if (isSyntaxCheck)
      return;
    if (isDelete)
      viewer.deleteMeasurement(countPlusIndexes);
    else if (isON)
      viewer.showMeasurement(countPlusIndexes, true);
    else if (isOFF)
      viewer.showMeasurement(countPlusIndexes, false);
    else
      viewer.toggleMeasurement(countPlusIndexes, strFormat);
  }

  private void refresh() {
    if (isSyntaxCheck)
      return;
    viewer.setTainted(true);
    viewer.requestRepaintAndWait();
  }

  private void reset() throws ScriptException {
    if (isSyntaxCheck)
      return;
    if (statementLength == 1) {
      viewer.reset();
      return;
    }
    // possibly "all"
    if (tokAt(1) == Token.function) {
      Compiler.globalFunctions.clear();
      compiler.localFunctions.clear();
      return;
    }
    String var = parameterAsString(1);
    if (var.charAt(0) == '_')
      invalidArgument();
    if (var.equalsIgnoreCase("aromatic")) {
      viewer.resetAromatic();
    } else {
      viewer.unsetProperty(var);
      //viewer.removeUserVariable(var + "_set");
    }
  }

  private void initialize() {
    viewer.initialize();
  }

  private void restrict() throws ScriptException {
    select();
    if (isSyntaxCheck)
      return;
    BitSet bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
    viewer.invertSelection();
    if (bsSubset != null) {
      bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
      bsSelected.and(bsSubset);
      viewer.setSelectionSet(bsSelected);
      BitSetUtil.invertInPlace(bsSelected, viewer.getAtomCount());
      bsSelected.and(bsSubset);
    }
    boolean bondmode = viewer.getBondSelectionModeOr();
    setBooleanProperty("bondModeOr", true);
    setShapeSize(JmolConstants.SHAPE_STICKS, 0);

    // also need to turn off backbones, ribbons, strands, cartoons
    for (int shapeType = JmolConstants.SHAPE_MAX_SIZE_ZERO_ON_RESTRICT; --shapeType >= 0;)
      if (shapeType != JmolConstants.SHAPE_MEASURES)
        setShapeSize(shapeType, 0);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "delete", null);
    viewer.setLabel(null);

    setBooleanProperty("bondModeOr", bondmode);
    viewer.setSelectionSet(bsSelected);
  }

  private void rotate(boolean isSpin, boolean isSelected)
      throws ScriptException {

    /*
     * The Chime spin method:
     * 
     * set spin x 10;set spin y 30; set spin z 10;
     * spin | spin ON
     * spin OFF
     * 
     * Jmol does these "first x, then y, then z"
     * I don't know what Chime does.
     * 
     * spin and rotate are now consolidated here.
     * 
     * far simpler is
     * 
     *  spin x 10
     *  spin y 10
     *  
     *  these are pure x or y spins or
     *  
     *  spin axisangle {1 1 0} 10
     *  
     *  this is the same as the old "spin x 10; spin y 10" -- or is it?
     *  anyway, it's better!
     *  
     *  note that there are many defaults
     *  
     *  spin     # defaults to spin y 10
     *  spin 10  # defaults to spin y 10
     *  spin x   # defaults to spin x 10
     *  
     *  and several new options
     *  
     *  spin -x
     *  spin axisangle {1 1 0} 10
     *  spin 10 (atomno=1)(atomno=2)
     *  spin 20 {0 0 0} {1 1 1}
     *  
     *  spin MOLECULAR {0 0 0} 20
     *  
     *  The MOLECULAR keyword indicates that spins or rotations are to be
     *  carried out in the internal molecular coordinate frame, not the
     *  fixed room frame. 
     *  
     *  In the case of rotateSelected, all rotations are internal
     *  and the absense of the MOLECULAR keyword indicates to 
     *  rotate about the geometric center of the molecule, not {0 0 0}
     *  
     *  Fractional coordinates may be indicated:
     *   
     *  spin 20 {0 0 0/} {1 1 1/}
     *  
     *  In association with this, TransformManager and associated functions
     *  are TOTALLY REWRITTEN and consolideated. It is VERY clean now - just
     *  two methods here -- one fixed and one molecular, two in Viewer, and 
     *  two in TransformManager. All the centering stuff has been carefully
     *  inspected are reorganized as well. 
     *  
     *  Bob Hanson 5/21/06
     *
     *
     */

    if (statementLength == 2)
      switch (getToken(1).tok) {
      case Token.on:
        if (!isSyntaxCheck)
          viewer.setSpinOn(true);
        return;
      case Token.off:
        if (!isSyntaxCheck)
          viewer.setSpinOn(false);
        return;
      }

    float degrees = Float.MIN_VALUE;
    int nPoints = 0;
    float endDegrees = Float.MAX_VALUE;
    boolean isAxisAngle = false;
    boolean isInternal = false;
    Point3f[] points = new Point3f[3];
    Point3f rotCenter = null;
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    String axisID;
    int direction = 1;
    boolean axesOrientationRasmol = viewer.getAxesOrientationRasmol();

    for (int i = 0; i < 3; ++i)
      points[i] = new Point3f(0, 0, 0);
    for (int i = 1; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      case Token.spin:
        isSpin = true;
        break;
      case Token.minus:
        direction = -1;
        break;
      case Token.axisangle:
        isAxisAngle = true;
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("x")) {
          rotAxis.set(direction, 0, 0);
          break;
        }
        if (str.equalsIgnoreCase("y")) {
          if (axesOrientationRasmol)
            direction = -direction;
          rotAxis.set(0, direction, 0);
          break;
        }
        if (str.equalsIgnoreCase("z")) {
          rotAxis.set(0, 0, direction);
          break;
        }
        if (str.equalsIgnoreCase("internal")
            || str.equalsIgnoreCase("molecular")) {
          isInternal = true;
          break;
        }
        invalidArgument();
      case Token.leftbrace:
      case Token.point3f:
        // {X, Y, Z}
        Point3f pt = getPoint3f(i, true);
        i = iToken;
        if (isAxisAngle) {
          if (axesOrientationRasmol)
            pt.y = -pt.y;
          rotAxis.set(pt);
          isAxisAngle = false;
        } else {
          points[nPoints++].set(pt);
        }
        break;
      case Token.dollarsign:
        // $drawObject[n]
        if (tokAt(i + 2) == Token.leftsquare) {
          Point3f pt1 = centerParameter(i);
          i = iToken;
          if (isAxisAngle) {
            if (axesOrientationRasmol)
              pt1.y = -pt1.y;
            rotAxis.set(pt1);
            isAxisAngle = false;
          } else {
            points[nPoints++].set(pt1);
          }
          break;
        }
        // $drawObject
        isInternal = true;
        axisID = objectNameParameter(++i);
        if (isSyntaxCheck) {
          rotCenter = new Point3f();
          rotAxis = new Vector3f();
        } else {
          //I was going to make this dependent upon type, but
          //upon reflection, I think it is correct.
          //bh
          rotCenter = getDrawObjectCenter(axisID);
          rotAxis = getDrawObjectAxis(axisID);
          if (rotCenter == null)
            drawObjectNotDefined(axisID);
        }
        points[nPoints++].set(rotCenter);
        break;
      case Token.comma:
        break;
      case Token.integer:
      case Token.decimal:
        //spin: degrees per second followed by final value
        //rotate: end degrees followed by degrees per second
        //rotate is a full replacement for spin
        //spin is DEPRECATED

        if (degrees == Float.MIN_VALUE)
          degrees = floatParameter(i);
        else {
          endDegrees = degrees;
          degrees = floatParameter(i);
          isSpin = true;
        }
        break;
      case Token.bitset:
      case Token.expressionBegin:
        BitSet bs = expression(i);
        i = iToken;
        rotCenter = viewer.getAtomSetCenter(bs);
        points[nPoints++].set(rotCenter);
        break;
      default:
        invalidArgument();
      }
      if (nPoints >= 3) //only 2 allowed for rotation -- for now
        evalError(GT._("too many rotation points were specified"));
    }
    if (nPoints < 2 && !isInternal) {
      // simple, standard fixed-frame rotation
      // rotate x  10
      // rotate axisangle {0 1 0} 10

      if (nPoints == 1)
        rotCenter = new Point3f(points[0]);

      // point-centered rotation, but not internal -- "frieda"
      // rotate x 10 (atoms)
      // rotate x 10 $object
      // rotate x 10 
      if (degrees == Float.MIN_VALUE)
        degrees = 10;
      if (isSyntaxCheck)
        return;
      viewer.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees, endDegrees,
          isSpin, isSelected);
      return;
    }
    if (nPoints < 2 && !isSyntaxCheck) {
      // rotate MOLECULAR
      // rotate MOLECULAR (atom1)
      // rotate MOLECULAR x 10 (atom1)
      // rotate axisangle MOLECULAR (atom1)
      points[1].set(points[0]);
      points[1].sub(rotAxis);
    } else {
      // rotate 10 (atom1) (atom2)
      // rotate 10 {x y z} {x y z}
      // rotate 10 (atom1) {x y z}
    }

    if (!isSyntaxCheck && points[0].distance(points[1]) == 0)
      evalError(GT._("rotation points cannot be identical"));
    if (degrees == Float.MIN_VALUE)
      degrees = 10;
    if (isSyntaxCheck)
      return;
    viewer.rotateAboutPointsInternal(points[0], points[1], degrees, endDegrees,
        isSpin, isSelected);
  }

  private Point3f getDrawObjectCenter(String axisID) {
    return (Point3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getSpinCenter:" + axisID);
  }

  private Vector3f getDrawObjectAxis(String axisID) {
    return (Vector3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getSpinAxis:" + axisID);
  }

  private void script(boolean isJavaScript) throws ScriptException {
    if (isJavaScript) {
      if (!isSyntaxCheck)
        viewer.eval(parameterAsString(1));
      return;
    }
    if (getToken(1).tok != Token.string)
      filenameExpected();
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
    String filename = parameterAsString(1);
    String theScript = null;
    if (filename.equalsIgnoreCase("inline")) {
      theScript = parameterExpression(2, 0, "_script", false).toString();
      i = iToken + 1;
    }
    boolean loadCheck = true;
    boolean isCheck = false;
    String option = optParameterAsString(i);
    if (option.equalsIgnoreCase("check")) {
      isCheck = true;
      option = optParameterAsString(++i);
    }
    if (option.equalsIgnoreCase("noload")) {
      loadCheck = false;
      option = optParameterAsString(++i);
    }
    if (option.equalsIgnoreCase("line") || option.equalsIgnoreCase("lines")) {
      i++;
      lineEnd = lineNumber = Math.max(intParameter(i++), 0);
      if (checkToken(i))
        if (getToken(i++).tok == Token.minus)
          lineEnd = (checkToken(i) ? intParameter(i++) : 0);
        else
          invalidArgument();
    } else if (option.equalsIgnoreCase("command")
        || option.equalsIgnoreCase("commands")) {
      i++;
      pc = Math.max(intParameter(i++) - 1, 0);
      pcEnd = pc + 1;
      if (checkToken(i))
        if (getToken(i++).tok == Token.minus)
          pcEnd = (checkToken(i) ? intParameter(i++) : 0);
        else
          invalidArgument();
    }
    checkStatementLength(i);
    if (isSyntaxCheck && !isScriptCheck)
      return;
    if (isScriptCheck)
      isCheck = true;
    boolean wasSyntaxCheck = isSyntaxCheck;
    boolean wasScriptCheck = isScriptCheck;
    if (isCheck)
      isSyntaxCheck = isScriptCheck = true;
    pushContext(null);
    boolean isOK;
    if (theScript != null)
      isOK = loadScript(null, theScript, false);
    else
      isOK = loadScriptFileInternal(filename);
    if (isOK) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < lineNumbers.length && lineNumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = fileOpenCheck;
      fileOpenCheck = fileOpenCheck && loadCheck;
      instructionDispatchLoop(isCheck);
      fileOpenCheck = saveLoadCheck;
      popContext();
    } else {
      Logger.error(GT._("script ERROR: ") + errorMessage);
      popContext();
      if (wasScriptCheck) {
        error = false;
        errorMessage = null;
      } else {
        evalError(null);
      }
    }

    isSyntaxCheck = wasSyntaxCheck;
    isScriptCheck = wasScriptCheck;
  }

  private void function(int tokType) throws ScriptException {
    if (isSyntaxCheck && !isScriptCheck)
      return;
    String name = (String) getToken(0).value;
    if (getFunction(name, tokType) == null)
      evalError(GT._("command expected"));
    Vector params = (statementLength == 1 ? null
        : (Vector) parameterExpression(1, 0, null, true));
    if (isSyntaxCheck)
      return;
    pushContext(null);
    loadFunction(name, params, tokType);
    instructionDispatchLoop(false);
    popContext();
  }

  private void sync() throws ScriptException {
    //new 11.3.9
    String text = "";
    String applet = "";
    switch (statementLength) {
    case 1:
      applet = "*";
      text = "ON";
      break;
    case 2:
      applet = parameterAsString(1);
      if (applet.indexOf("jmolApplet") == 0 || Parser.isOneOf(applet, "*;.;^")) {
        text = "ON";
        if (!isSyntaxCheck)
          viewer.syncScript(text, applet);
        applet = ".";
        break;
      }
      text = applet;
      applet = "*";
      break;
    case 3:
      applet = parameterAsString(1);
      text = parameterAsString(2);
      break;
    }
    if (isSyntaxCheck)
      return; 
    viewer.syncScript(text, applet);
  }

  private void history(int pt) throws ScriptException {
    //history or set history
    if (statementLength == 1) {
      //show it
      showString(viewer.getSetHistory(Integer.MAX_VALUE));
      return;
    }
    if (pt == 2) {
      // set history n; n' = -2 - n; if n=0, then set history OFF
      checkLength3();
      int n = intParameter(2);
      if (n < 0)
        invalidArgument();
      if (!isSyntaxCheck)
        viewer.getSetHistory(n == 0 ? 0 : -2 - n);
      return;
    }
    checkLength2();
    switch (getToken(1).tok) {
    // pt = 1  history     ON/OFF/CLEAR
    case Token.on:
    case Token.clear:
      if (!isSyntaxCheck)
        viewer.getSetHistory(Integer.MIN_VALUE);
      return;
    case Token.off:
      if (!isSyntaxCheck)
        viewer.getSetHistory(0);
      break;
    default:
      keywordExpected("ON, OFF, CLEAR");
    }
  }

  private void display(boolean isDisplay) throws ScriptException {
    BitSet bs = (statementLength == 1 ? null : expression(1));
    if (isSyntaxCheck)
      return;
    if (isDisplay)
      viewer.display(viewer.getModelAtomBitSet(-1, false), bs, tQuiet);
    else
      viewer.hide(bs, tQuiet);
  }

  private void select() throws ScriptException {
    // NOTE this is called by restrict()
    //select beginexpr bonds ( {...} ) endexpr
    if (statementLength == 1) {
      viewer.select(null, tQuiet || scriptLevel > scriptReportingLevel);
      return;
    }
    if (tokAt(2) == Token.bitset && getToken(2).value instanceof BondSet
        || getToken(2).tok == Token.bonds && getToken(3).tok == Token.bitset) {
      if (statementLength == iToken + 2) {
        if (!isSyntaxCheck)
          viewer.selectBonds((BitSet) theToken.value);
        return;
      }
      invalidArgument();
    }
    if (getToken(2).tok == Token.monitor) {
      if (statementLength == 5 && getToken(3).tok == Token.bitset) {
        if (!isSyntaxCheck)
          setShapeProperty(JmolConstants.SHAPE_MEASURES, "select",
              theToken.value);
        return;
      }
      invalidArgument();
    }
    BitSet bs = expression(1);
    if (isSyntaxCheck)
      return;
    if (isBondSet) {
      viewer.selectBonds(bs);
    } else {
      viewer.select(bs, tQuiet || scriptLevel > scriptReportingLevel);
    }
  }

  private void subset() throws ScriptException {
    bsSubset = (statementLength == 1 ? null : expression(-1));
    if (isSyntaxCheck)
      return;
    viewer.setSelectionSubset(bsSubset);
    //I guess we do not want to select, because that could 
    //throw off picking in a strange way
    // viewer.select(bsSubset, false);
  }

  private void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    Point3f pt = null;
    Point4f plane = null;
    if (statementLength == 1) {
      if (isSyntaxCheck)
        return;
      BitSet bs = viewer.getSelectionSet();
      pt = viewer.getAtomSetCenter(bs);
      viewer.invertSelected(pt, bs);
      return;
    }
    String type = parameterAsString(1);

    if (type.equalsIgnoreCase("point")) {
      pt = centerParameter(2);
    } else if (type.equalsIgnoreCase("plane")) {
      plane = planeParameter(2);
    } else if (type.equalsIgnoreCase("hkl")) {
      plane = hklParameter(2);
    }
    checkStatementLength(iToken + 1);
    if (plane == null && pt == null)
      invalidArgument();
    if (isSyntaxCheck)
      return;
    viewer.invertSelected(pt, plane);
  }

  private void translateSelected() throws ScriptException {
    // translateSelected {x y z}
    Point3f pt = getPoint3f(1, true);
    if (!isSyntaxCheck)
      viewer.setAtomCoordRelative(pt);
  }

  private void translate() throws ScriptException {
    float percent = floatParameter(2);
    if (percent > 100 || percent < -100)
      numberOutOfRange(-100, 100);
    if (getToken(1).tok == Token.identifier) {
      String str = parameterAsString(1);
      if (str.equalsIgnoreCase("x")) {
        if (!isSyntaxCheck)
          viewer.translateToXPercent(percent);
        return;
      }
      if (str.equalsIgnoreCase("y")) {
        if (!isSyntaxCheck)
          viewer.translateToYPercent(percent);
        return;
      }
      if (str.equalsIgnoreCase("z")) {
        if (!isSyntaxCheck)
          viewer.translateToZPercent(percent);
        return;
      }
    }
    axisExpected();
  }

  private void zap() {
    viewer.zap(true);
    refresh();
  }

  private void zoom(boolean isZoomTo) throws ScriptException {
    if (!isZoomTo) {
      //zoom
      //zoom on|off
      int tok = (statementLength > 1 ? getToken(1).tok : Token.on);
      switch (tok) {
      case Token.on:
      case Token.off:
        if (statementLength > 2)
          badArgumentCount();
        if (!isSyntaxCheck)
          setBooleanProperty("zoomEnabled", tok == Token.on);
        return;
      }
    }
    float zoom = viewer.getZoomPercentFloat();
    float radius = viewer.getRotationRadius();
    Point3f center = null;
    Point3f currentCenter = viewer.getRotationCenter();
    int i = 1;
    //zoomTo time-sec 
    float time = (isZoomTo ? (isFloatParameter(i) ? floatParameter(i++) : 2f)
        : 0f);
    if (time < 0)
      invalidArgument();
    //zoom {x y z} or (atomno=3)
    int ptCenter = 0;
    if (isCenterParameter(i)) {
      ptCenter = i;
      center = centerParameter(i);
      i = iToken + 1;
    }

    boolean isSameAtom = (center != null && currentCenter.distance(center) < 0.1);
    //zoom/zoomTo percent|-factor|+factor|*factor|/factor | 0
    float factor = getZoomFactor(i, ptCenter, radius, zoom);

    if (factor < 0) {
      factor = -factor;
      if (isZoomTo) {
        // no factor -- check for no center (zoom out) or same center (zoom in)
        if (statementLength == 1 || isSameAtom)
          factor *= 2;
        else if (center == null)
          factor /= 2;
      }
    }
    float xTrans = 0;
    float yTrans = 0;
    float max = viewer.getMaxZoomPercent();
    if (factor < 5 || factor > max)
      numberOutOfRange(5, max);
    if (!viewer.isWindowCentered()) {
      // do a smooth zoom only if not windowCentered
      if (center != null) {
        BitSet bs = expression(ptCenter);
        if (!isSyntaxCheck)
          viewer.setCenterBitSet(bs, false);
      }
      center = viewer.getRotationCenter();
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
    }
    if (isSyntaxCheck)
      return;
    if (isSameAtom && Math.abs(zoom - factor) < 1)
      time = 0;
    viewer.moveTo(time, center, new Point3f(0, 0, 0), Float.NaN, factor,
        xTrans, yTrans, radius, null, Float.NaN, Float.NaN, Float.NaN);
  }

  private float getZoomFactor(int i, int ptCenter, float radius, float factor0)
      throws ScriptException {
    BitSet bs = null;
    float factor = (isFloatParameter(i) ? floatParameter(i) : Float.NaN);
    if (factor == 0) {
      switch (statement[ptCenter].tok) {
      case Token.bitset:
      case Token.expressionBegin:
        bs = expression(statement, ptCenter, 0, true, false, false);
      }
      if (bs == null)
        invalidArgument();
      float r = viewer.calcRotationRadius(bs);
      factor0 = radius / r * 100;
      factor = Float.NaN;
      i++;
    }
    if (factor < 0) {
      factor += factor0;
    } else if (Float.isNaN(factor)) {
      factor = factor0;
      if (isFloatParameter(i + 1)) {
        float value = floatParameter(i + 1);
        switch (getToken(i++).tok) {
        case Token.divide:
          factor /= value;
          break;
        case Token.times:
          factor *= value;
          break;
        case Token.plus:
          factor += value;
          break;
        default:
          invalidArgument();
        }
      } else {
        if (bs == null)
          factor = -factor;
        --i;
      }
    }
    iToken = i;
    return factor;
  }

  private void gotocmd() throws ScriptException {
    String strTo = null;
    strTo = parameterAsString(1);
    int pcTo = -1;
    for (int i = 0; i < aatoken.length; i++) {
      Token[] tokens = aatoken[i];
      if (tokens[0].tok == Token.message)
        if (tokens[1].value.toString().equalsIgnoreCase(strTo)) {
          pcTo = i;
          break;
        }
    }
    if (pcTo < 0)
      invalidArgument();
    if (!isSyntaxCheck)
      pc = pcTo - 1; // ... resetting the program counter
  }

  private void delay() throws ScriptException {
    long millis = 0;
    switch (getToken(1).tok) {
    case Token.on: // this is auto-provided as a default
      millis = 1;
      break;
    case Token.integer:
      millis = intParameter(1) * 1000;
      break;
    case Token.decimal:
      millis = (long) (floatParameter(1) * 1000);
      break;
    default:
      numberExpected();
    }
    if (!isSyntaxCheck)
      delay(millis);
  }

  private void delay(long millis) {
    long timeBegin = System.currentTimeMillis();
    refresh();
    millis -= System.currentTimeMillis() - timeBegin;
    int seconds = (int) millis / 1000;
    millis -= seconds * 1000;
    if (millis <= 0)
      millis = 1;
    while (seconds >= 0 && millis > 0 && !interruptExecution.booleanValue()
        && currentThread == Thread.currentThread()) {
      viewer.popHoldRepaint();
      try {
        Thread.sleep((seconds--) > 0 ? 1000 : millis);
      } catch (InterruptedException e) {
      }
      viewer.pushHoldRepaint();
    }
  }

  private void slab(boolean isDepth) throws ScriptException {
    boolean TF = false;
    Point4f plane = null;
    String str;
    if (isCenterParameter(1) || tokAt(1) == Token.point4f)
      plane = planeParameter(1);
    else
      switch (getToken(1).tok) {
      case Token.integer:
        checkLength2();
        int percent = intParameter(1);
        if (!isSyntaxCheck)
          if (isDepth)
            viewer.depthToPercent(percent);
          else
            viewer.slabToPercent(percent);
        return;
      case Token.on:
        checkLength2();
        TF = true;
      // fall through
      case Token.off:
        checkLength2();
        setBooleanProperty("slabEnabled", TF);
        return;
      case Token.reset:
        checkLength2();
        if (isSyntaxCheck)
          return;
        viewer.slabReset();
        setBooleanProperty("slabEnabled", true);
        return;
      case Token.set:
        checkLength2();
        if (isSyntaxCheck)
          return;
        viewer.setSlabDepthInternal(isDepth);
        setBooleanProperty("slabEnabled", true);
        return;
      case Token.minus:
        str = parameterAsString(2);
        if (str.equalsIgnoreCase("hkl"))
          plane = hklParameter(3);
        else if (str.equalsIgnoreCase("plane"))
          plane = planeParameter(3);
        if (plane == null)
          invalidArgument();
        plane.scale(-1);
        break;
      case Token.plane:
        switch (getToken(2).tok) {
        case Token.none:
          break;
        default:
          plane = planeParameter(2);
        }
        break;
      case Token.identifier:
        str = parameterAsString(1);
        if (str.equalsIgnoreCase("hkl")) {
          plane = (getToken(2).tok == Token.none ? null : hklParameter(2));
          break;
        }
        if (str.equalsIgnoreCase("reference")) {
          //only in 11.2; deprecated
          return;
        }
      default:
        invalidArgument();
      }
    if (!isSyntaxCheck)
      viewer.slabInternal(plane, isDepth);
  }

  private int getDiameterTok() throws ScriptException {
    if (statementLength == 1)
      return Token.on;
    int tok = getToken(1).tok;
    switch (statementLength) {
    case 2:
      break;
    case 3:
      if (tok == Token.integer && getToken(2).tok == Token.percent
          || tok == Token.plus && isFloatParameter(2))
        break;
      invalidArgument();
    }
    return tok;
  }

  private void star() throws ScriptException {
    short mad = 0; // means back to selection business
    switch (getDiameterTok()) {
    case Token.on:
    case Token.vanderwaals:
      mad = -100; // cpk with no args goes to 100%
      break;
    case Token.off:
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1);
      if (statementLength == 2) {
        if (radiusRasMol >= 750 || radiusRasMol < -100)
          numberOutOfRange(-100, 749);
        mad = (short) radiusRasMol;
        if (radiusRasMol > 0)
          mad *= 4 * 2;
      } else {
        if (radiusRasMol < 0 || radiusRasMol > 100)
          numberOutOfRange(0, 100);
        mad = (short) -radiusRasMol; // use a negative number to specify %vdw
      }
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short) (angstroms * 1000 * 2);
      break;
    case Token.temperature:
      mad = -1000;
      break;
    case Token.ionic:
      mad = -1001;
      break;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(JmolConstants.SHAPE_STARS, mad);
  }

  private void structure() throws ScriptException {
    String type = parameterAsString(1).toLowerCase();
    byte iType = 0;
    BitSet bs = null;
    if (type.equals("helix"))
      iType = JmolConstants.PROTEIN_STRUCTURE_HELIX;
    else if (type.equals("sheet"))
      iType = JmolConstants.PROTEIN_STRUCTURE_SHEET;
    else if (type.equals("turn"))
      iType = JmolConstants.PROTEIN_STRUCTURE_TURN;
    else if (type.equals("none"))
      iType = JmolConstants.PROTEIN_STRUCTURE_NONE;
    else
      invalidArgument();
    switch (tokAt(2)) {
    case Token.bitset:
    case Token.expressionBegin:
      bs = expression(2);
      checkStatementLength(iToken + 1);
      break;
    default:
      checkLength2();
    }
    if (isSyntaxCheck)
      return;
    clearPredefined(JmolConstants.predefinedVariable);
    viewer.setProteinType(iType, bs);
  }

  private void halo() throws ScriptException {
    short mad = 0;
    switch (getDiameterTok()) {
    case Token.on:
      mad = -20; // on goes to 25%
      break;
    case Token.vanderwaals:
      mad = -100; // cpk with no args goes to 100%
      break;
    case Token.off:
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1);
      if (statementLength == 2) {
        if (radiusRasMol >= 750 || radiusRasMol < -100)
          numberOutOfRange(-100, 749);
        mad = (short) radiusRasMol;
        if (radiusRasMol > 0)
          mad *= 4 * 2;
      } else {
        if (radiusRasMol < 0 || radiusRasMol > 100)
          numberOutOfRange(0, 100);
        mad = (short) -radiusRasMol; // use a negative number to specify %vdw
      }
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short) (angstroms * 1000 * 2);
      break;
    case Token.temperature:
      mad = -1000;
      break;
    case Token.ionic:
      mad = -1001;
      break;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(JmolConstants.SHAPE_HALOS, mad);
  }

  /// aka cpk
  private void spacefill() throws ScriptException {
    short mad = 0;
    boolean isSolventAccessibleSurface = false;
    int i = 1;
    switch (getDiameterTok()) {
    case Token.on:
    case Token.vanderwaals:
      mad = -100; // cpk with no args goes to 100%
      break;
    case Token.off:
      break;
    case Token.plus:
      isSolventAccessibleSurface = true;
      i++;
    case Token.decimal:
      float angstroms = floatParameter(i);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short) (angstroms * 1000 * 2);
      if (isSolventAccessibleSurface)
        mad += 10000;
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1);
      if (statementLength == 2) {
        if (radiusRasMol >= 750 || radiusRasMol < -200)
          numberOutOfRange(-200, 749);
        mad = (short) radiusRasMol;
        if (radiusRasMol > 0)
          mad *= 4 * 2;
      } else {
        if (radiusRasMol < 0 || radiusRasMol > 200)
          numberOutOfRange(0, 200);
        mad = (short) -radiusRasMol; // use a negative number to specify %vdw
      }
      break;
    case Token.temperature:
      mad = -1000;
      break;
    case Token.ionic:
      mad = -1001;
      break;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(JmolConstants.SHAPE_BALLS, mad);
  }

  private void wireframe() throws ScriptException {
    short mad = getMadParameter();
    if (isSyntaxCheck)
      return;
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
    viewer.setShapeSize(JmolConstants.SHAPE_STICKS, mad, viewer
        .getSelectionSet());
  }

  private void ssbond() throws ScriptException {
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_SULFUR_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter());
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void hbond(boolean isCommand) throws ScriptException {
    if (statementLength == 2 && getToken(1).tok == Token.calculate) {
      if (isSyntaxCheck)
        return;
      int n = viewer.autoHbond(null);
      scriptStatus(GT._("{0} hydrogen bonds", n));
      return;
    }
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter());
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void configuration() throws ScriptException {
    if (!isSyntaxCheck && viewer.getDisplayModelIndex() <= -2)
      evalError(GT._("{0} not allowed with background model displayed",
          "\"CONFIGURATION\""));
    BitSet bsConfigurations;
    if (statementLength == 1) {
      bsConfigurations = viewer.setConformation();
      viewer.addStateScript("select " + Escape.escape(viewer.getSelectionSet())
          + "; configuration;", true);
    } else {
      checkLength2();
      if (isSyntaxCheck)
        return;
      int n = intParameter(1);
      bsConfigurations = viewer.setConformation(n - 1);
      viewer.addStateScript("configuration " + n + ";", true);
      //System.out.println(n + " " + bsConfigurations);
    }
    if (isSyntaxCheck)
      return;
    boolean addHbonds = viewer.hasCalculatedHBonds(bsConfigurations);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    viewer.setShapeSize(JmolConstants.SHAPE_STICKS, 0, bsConfigurations);
    if (addHbonds)
      viewer.autoHbond(bsConfigurations, bsConfigurations, null);
    viewer.select(bsConfigurations, tQuiet);
  }

  private void vector() throws ScriptException {
    short mad = 1;
    switch (iToken = statementLength) {
    case 1:
      break;
    case 2:
      switch (getToken(1).tok) {
      case Token.on:
        break;
      case Token.off:
        mad = 0;
        break;
      case Token.integer:
        int diameterPixels = intParameter(1);
        if (diameterPixels < 0 || diameterPixels >= 20)
          numberOutOfRange(0, 19);
        mad = (short) diameterPixels;
        break;
      case Token.decimal:
        float angstroms = floatParameter(1);
        if (angstroms > 3)
          numberOutOfRange(0f, 3f);
        mad = (short) (angstroms * 1000 * 2);
        break;
      default:
        booleanOrNumberExpected();
      }
      break;
    case 3:
      if (parameterAsString(1).equalsIgnoreCase("scale")) {
        float scale = floatParameter(2);
        if (scale < -10 || scale > 10)
          numberOutOfRange(-10f, 10f);
        setFloatProperty("vectorScale", scale);
        return;
      }
      invalidArgument();
    }
    setShapeSize(JmolConstants.SHAPE_VECTORS, mad);
  }

  private void dipole() throws ScriptException {
    //dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    viewer.loadShape(JmolConstants.SHAPE_DIPOLES);
    if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_DIPOLES))
      return;
    setShapeProperty(JmolConstants.SHAPE_DIPOLES, "init", null);
    if (statementLength == 1) {
      setShapeProperty(JmolConstants.SHAPE_DIPOLES, "thisID", null);
      return;
    }
    for (int i = 1; i < statementLength; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (getToken(i).tok) {
      case Token.on:
        propertyName = "on";
        break;
      case Token.off:
        propertyName = "off";
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      case Token.integer:
      case Token.decimal:
        propertyName = "value";
        propertyValue = new Float(floatParameter(i));
        break;
      case Token.bitset:
        propertyName = "atomBitset";
      // fall through
      case Token.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = expression(i);
        i = iToken;
        iHaveAtoms = true;
        break;
      case Token.leftbrace:
      case Token.point3f:
        // {X, Y, Z}
        Point3f pt = getPoint3f(i, true);
        i = iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case Token.bonds:
        propertyName = "bonds";
        break;
      case Token.calculate:
        propertyName = "calculate";
        break;
      case Token.identifier:
        String cmd = parameterAsString(i);
        if (cmd.equalsIgnoreCase("cross")) {
          propertyName = "cross";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (cmd.equalsIgnoreCase("noCross")) {
          propertyName = "cross";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (cmd.equalsIgnoreCase("offset")) {
          float v = floatParameter(++i);
          if (theTok == Token.integer) {
            propertyName = "offsetPercent";
            propertyValue = new Integer((int) v);
          } else {
            propertyName = "offset";
            propertyValue = new Float(v);
          }
          break;
        }
        if (cmd.equalsIgnoreCase("value")) {
          propertyName = "value";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("offsetSide")) {
          propertyName = "offsetSide";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("width")) {
          propertyName = "width";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (idSeen)
          invalidArgument();
        propertyName = "thisID"; // might be "molecular"
        propertyValue = cmd.toLowerCase();
        break;
      default:
        invalidArgument();
      }
      idSeen = (theTok != Token.delete && theTok != Token.calculate);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DIPOLES, propertyName,
            propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JmolConstants.SHAPE_DIPOLES, "set", null);
  }

  private void animationMode() throws ScriptException {
    float startDelay = 1, endDelay = 1;
    if (statementLength > 5)
      badArgumentCount();
    int animationMode = RepaintManager.ANIMATION_ONCE;
    switch (getToken(2).tok) {
    case Token.loop:
      animationMode = RepaintManager.ANIMATION_LOOP;
      break;
    case Token.identifier:
      String cmd = parameterAsString(2);
      if (cmd.equalsIgnoreCase("once")) {
        startDelay = endDelay = 0;
        break;
      }
      if (cmd.equalsIgnoreCase("palindrome")) {
        animationMode = RepaintManager.ANIMATION_PALINDROME;
        break;
      }
      invalidArgument();
    }
    if (statementLength >= 4) {
      startDelay = endDelay = floatParameter(3);
      if (statementLength == 5)
        endDelay = floatParameter(4);
    }
    if (!isSyntaxCheck)
      viewer.setAnimationReplayMode(animationMode, startDelay, endDelay);
  }

  private void vibration() throws ScriptException {
    float period = 0;
    switch (getToken(1).tok) {
    case Token.on:
      checkLength2();
      period = viewer.getVibrationPeriod();
      break;
    case Token.off:
      checkLength2();
      period = 0;
      break;
    case Token.integer:
    case Token.decimal:
      checkLength2();
      period = floatParameter(1);
      break;
    case Token.identifier:
      String cmd = optParameterAsString(1);
      if (cmd.equalsIgnoreCase("scale")) {
        float scale = floatParameter(2);
        if (scale < -10 || scale > 10)
          numberOutOfRange(-10f, 10f);
        setFloatProperty("vibrationScale", scale);
        return;
      } else if (cmd.equalsIgnoreCase("period")) {
        period = floatParameter(2);
        setFloatProperty("vibrationPeriod", period);
        return;
      } else {
        invalidArgument();
      }
    default:
      period = -1;
    }
    if (period < 0)
      invalidArgument();
    if (isSyntaxCheck)
      return;
    if (period == 0) {
      viewer.setVibrationOff();
      return;
    }
    viewer.setVibrationPeriod(-period);
  }

  private void animationDirection() throws ScriptException {
    checkStatementLength(4);
    boolean negative = false;
    getToken(2);
    if (theTok == Token.minus)
      negative = true;
    else if (theTok != Token.plus)
      invalidArgument();
    int direction = intParameter(3);
    if (direction != 1)
      numberMustBe(-1, 1);
    if (negative)
      direction = -direction;
    if (!isSyntaxCheck)
      viewer.setAnimationDirection(direction);
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    if ((iToken = statementLength) >= 2) {
      clearPredefined(JmolConstants.predefinedVariable);
      switch (getToken(1).tok) {
      case Token.surface:
        isSurface = true;
        //deprecated
        //fall through
      case Token.surfacedistance:
        /* preferred:
         * 
         * calculate surfaceDistance FROM {...}
         * calculate surfaceDistance WITHIN {...}
         * 
         */
        String type = optParameterAsString(2);
        boolean isFrom = false;
        if (type.equalsIgnoreCase("within")) {
        } else if (type.equalsIgnoreCase("from")) {
          isFrom = true;
        } else if (type.length() > 0) {
          isFrom = true;
          iToken--;
        } else if (!isSurface) {
          isFrom = true;          
        }
        BitSet bsSelected = (iToken + 1 < statementLength ? expression(++iToken)
            : viewer.getSelectionSet());
        checkStatementLength(++iToken);
        if (isSyntaxCheck)
          return;
        viewer.calculateSurface(bsSelected, (isFrom ? Float.MAX_VALUE : -1));
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("AROMATIC")) {
          checkLength2();
          if (!isSyntaxCheck)
            viewer.assignAromaticBonds();
          return;
        }
        break;
      case Token.hbond:
        checkLength2();
        if (isSyntaxCheck)
          return;
        viewer.autoHbond(null);
        return;
      case Token.structure:
        BitSet bs = (statementLength == 2 ? null : expression(2));
        if (isSyntaxCheck)
          return;
        if (bs == null)
          bs = viewer.getModelAtomBitSet(-1, false);
          viewer.calculateStructures(bs);
        viewer.addStateScript(thisCommand, false);
        return;
      }
    }
    evalError(GT._("Calculate what?")
        + "aromatic? hbonds? polymers? structure? surfaceDistance FROM? surfaceDistance WITHIN?");
  }

  private void dots(int ipt, int iShape) throws ScriptException {
    if (!isSyntaxCheck)
      viewer.loadShape(iShape);
    setShapeProperty(iShape, "init", null);
    if (statementLength == ipt) {
      setShapeSize(iShape, 1);
      return;
    }
    short mad = 0;
    float radius;
    switch (getToken(ipt).tok) {
    case Token.on:
    case Token.vanderwaals:
      mad = 1;
      break;
    case Token.ionic:
      mad = -1;
      break;
    case Token.off:
      break;
    case Token.plus:
      radius = floatParameter(++ipt);
      if (radius < 0f || radius > 10f)
        numberOutOfRange(0f, 2f);
      mad = (short) (radius == 0f ? 0 : radius * 1000f + 11002);
      break;
    case Token.decimal:
      radius = floatParameter(ipt);
      if (radius < 0f || radius > 10f)
        numberOutOfRange(0f, 10f);
      mad = (short) (radius == 0f ? 0 : radius * 1000f + 1002);
      break;
    case Token.integer:
      int dotsParam = intParameter(ipt);
      if (statementLength > ipt + 1 && statement[ipt + 1].tok == Token.radius) {
        setShapeProperty(iShape, "atom", new Integer(dotsParam));
        ipt++;
        setShapeProperty(iShape, "radius", new Float(floatParameter(++ipt)));
        if (statementLength > ipt + 1 && statement[++ipt].tok == Token.color)
          setShapeProperty(iShape, "colorRGB", new Integer(getArgbParam(++ipt)));
        if (getToken(ipt).tok != Token.bitset)
          invalidArgument();
        setShapeProperty(iShape, "dots", statement[ipt].value);
        return;
      }

      if (dotsParam < 0 || dotsParam > 1000)
        numberOutOfRange(0, 1000);
      mad = (short) (dotsParam == 0 ? 0 : dotsParam + 1);
      break;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(iShape, mad);
  }

  private void proteinShape(int shapeType) throws ScriptException {
    short mad = 0;
    //token has ondefault1
    switch (getToken(1).tok) {
    case Token.on:
      mad = -1; // means take default
      break;
    case Token.off:
      break;
    case Token.structure:
      mad = -2;
      break;
    case Token.temperature:
    // MTH 2004 03 15
    // Let temperature return the mean positional displacement
    // see what people think
    // mad = -3;
    // break;
    case Token.displacement:
      mad = -4;
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1);
      if (radiusRasMol >= 500)
        numberOutOfRange(0, 499);
      mad = (short) (radiusRasMol * 4 * 2);
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms > 4)
        numberOutOfRange(0f, 4f);
      mad = (short) (angstroms * 1000 * 2);
      break;
    case Token.bitset:
      if (!isSyntaxCheck)
        viewer.loadShape(shapeType);
      setShapeProperty(shapeType, "bitset", theToken.value);
      return;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(shapeType, mad);
  }

  private void animation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case Token.on:
      animate = true;
    //fall through
    case Token.off:
      if (!isSyntaxCheck)
        viewer.setAnimationOn(animate);
      break;
    case Token.frame:
      frame(2);
      break;
    case Token.mode:
      animationMode();
      break;
    case Token.direction:
      animationDirection();
      break;
    case Token.identifier:
      String str = parameterAsString(1);
      if (str.equalsIgnoreCase("fps")) {
        checkLength3();
        setIntProperty("animationFps", intParameter(2));
        break;
      }
    default:
      frameControl(1, true);
    }
  }

  private void file() throws ScriptException {
    int file = intParameter(1);
    if (isSyntaxCheck)
      return;
    int modelIndex = viewer.getModelNumberIndex(file * 1000000 + 1, false, false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = viewer.getModelNumberIndex((file + 1) * 1000000 + 1, false, false);
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelCount();
      modelIndex2--;
    }
    viewer.setAnimationOn(false);
    viewer.setAnimationDirection(1);
    viewer.setAnimationRange(modelIndex, modelIndex2);
    viewer.setCurrentModelIndex(-1);
  }

  private void frame(int offset) throws ScriptException {
    boolean useModelNumber = true;
    // for now -- as before -- remove to implement
    // frame/model difference
    if (statementLength == 1 && offset == 1) {
      int modelIndex = viewer.getCurrentModelIndex();
      if (!isSyntaxCheck && modelIndex >= 0
          && (modelIndex = viewer.getJmolDataSourceFrame(modelIndex)) >= 0)
        viewer.setCurrentModelIndex(modelIndex, true);
      return;
    }
    if (statementLength == 3 && parameterAsString(1).equalsIgnoreCase("Title")) {
      if (!isSyntaxCheck)
        viewer.setFrameTitle(parameterAsString(2));
      return;
    }

    if (getToken(offset).tok == Token.minus) {
      ++offset;
      checkStatementLength(offset + 1);
      if (getToken(offset).tok != Token.integer || intParameter(offset) != 1)
        invalidArgument();
      if (!isSyntaxCheck)
        viewer.setAnimationPrevious();
      return;
    }
    boolean isPlay = false;
    boolean isRange = false;
    boolean isAll = false;
    boolean isHyphen = false;
    int[] frameList = new int[] { -1, -1 };
    int nFrames = 0;

    for (int i = offset; i < statementLength; i++) {
      switch (getToken(i).tok) {
      case Token.all:
      case Token.times:
        checkStatementLength(offset + (isRange ? 2 : 1));
        isAll = true;
        break;
      case Token.minus: //ignore
        if (nFrames != 1)
          invalidArgument();
        isHyphen = true;
        break;
      case Token.none:
        checkStatementLength(offset + 1);
        break;
      case Token.decimal:
        useModelNumber = false;
        if (floatParameter(i) < 0)
          isHyphen = true;
      //fall through
      case Token.integer:
        if (nFrames == 2)
          invalidArgument();
        int iFrame = statement[i].intValue;
        if (iFrame >= 1000 && iFrame < 1000000 && viewer.haveFileSet())
          iFrame = (iFrame / 1000) * 1000000 + (iFrame % 1000); //initial way
        if (!useModelNumber && iFrame == 0)
          isAll = true; //   0.0 means ALL; 0 means "all in this range
        if (iFrame >= 1000000)
          useModelNumber = false;
        frameList[nFrames++] = iFrame;
        break;
      case Token.play:
        isPlay = true;
        break;
      case Token.range:
        isRange = true;
        break;
      default:
        checkStatementLength(offset + 1);
        frameControl(i, false);
        return;
      }
    }
    boolean haveFileSet = viewer.haveFileSet();
    if (isRange && nFrames == 0)
      isAll = true;
    if (isSyntaxCheck)
      return;
    if (isAll) {
      viewer.setAnimationOn(false);
      viewer.setAnimationRange(-1, -1);
      if (!isRange) {
        viewer.setCurrentModelIndex(-1);
      }
      return;
    }
    if (nFrames == 2 && !isRange)
      isHyphen = true;
    if (haveFileSet)
      useModelNumber = false;
    else if (useModelNumber)
      for (int i = 0; i < nFrames; i++)
        if (frameList[i] >= 0)
          frameList[i] %= 1000000;
    int modelIndex = viewer.getModelNumberIndex(frameList[0], useModelNumber, false);
    int modelIndex2 = -1;
    if (haveFileSet && nFrames == 1 && modelIndex < 0 && frameList[0] != 0) {
      // may have frame 2.0 or frame 2 meaning the range of models in file 2
      if (frameList[0] < 1000000)
        frameList[0] *= 1000000;
      if (frameList[0] % 1000000 == 0) {
        frameList[0]++;
        modelIndex = viewer.getModelNumberIndex(frameList[0], false, false);
        if (modelIndex >= 0) {
          modelIndex2 = viewer.getModelNumberIndex(frameList[0] + 1000000,
              false, false);
          if (modelIndex2 < 0)
            modelIndex2 = viewer.getModelCount();
          modelIndex2--;
          if (isRange)
            nFrames = 2;
          else if (!isHyphen && modelIndex2 != modelIndex)
            isHyphen = true;
          isRange = isRange || modelIndex == modelIndex2;//(isRange || !isHyphen && modelIndex2 != modelIndex);
        }
      } else {
        //must have been a bad frame number. Just return.
        return;
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0)
      viewer.setCurrentModelIndex(modelIndex, false);
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelNumberIndex(frameList[1], useModelNumber, false);
      viewer.setAnimationOn(false);
      viewer.setAnimationDirection(1);
      viewer.setAnimationRange(modelIndex, modelIndex2);
      viewer.setCurrentModelIndex(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0, false);
    }
    if (isPlay)
      viewer.resumeAnimation();
  }

  BitSet bitSetForModelFileNumber(int m) {
    // where */1.0 or */1.1 or just 1.1 is processed
    BitSet bs = new BitSet();
    if (isSyntaxCheck)
      return bs;
    int modelCount = viewer.getModelCount();
    boolean haveFileSet = viewer.haveFileSet();
    if (m < 1000000 && haveFileSet)
      m *= 1000000;
    int pt = m % 1000000;
    if (pt == 0) {
      int model1 = viewer.getModelNumberIndex(m + 1, false, false);
      if (model1 < 0)
        return bs;
      int model2 = (m == 0 ? modelCount : viewer.getModelNumberIndex(
          m + 1000001, false, false));
      if (model1 < 0)
        model1 = 0;
      if (model2 < 0)
        model2 = modelCount;
      if (viewer.isTrajectory(model1))
        model2 = model1 + 1;
      for (int j = model1; j < model2; j++)
        bs.or(viewer.getModelAtomBitSet(j, false));
    } else {
      int modelIndex = viewer.getModelNumberIndex(m, false, true);
      if (modelIndex >= 0)
        bs.or(viewer.getModelAtomBitSet(modelIndex, false));
    }
    return bs;
  }

  private void frameControl(int i, boolean isSubCmd) throws ScriptException {
    checkStatementLength(i + 1);
    int tok = getToken(i).tok;
    if (isSyntaxCheck)
      switch (tok) {
      case Token.playrev:
      case Token.play:
      case Token.resume:
      case Token.pause:
      case Token.next:
      case Token.prev:
      case Token.rewind:
      case Token.last:
        return;
      }
    else
      switch (tok) {
      case Token.playrev:
        viewer.reverseAnimation();
      //fall through
      case Token.play:
      case Token.resume:
        viewer.resumeAnimation();
        return;
      case Token.pause:
        viewer.pauseAnimation();
        return;
      case Token.next:
        viewer.setAnimationNext();
        return;
      case Token.prev:
        viewer.setAnimationPrevious();
        return;
      case Token.rewind:
        viewer.rewindAnimation();
        return;
      case Token.last:
        viewer.setAnimationLast();
        return;
      }
    evalError(GT._("invalid {0} control keyword", "frame"));
  }

  private int getShapeType(int tok) throws ScriptException {
    int iShape = JmolConstants.shapeTokenIndex(tok);
    if (iShape < 0)
      unrecognizedObject();
    return iShape;
  }

  private void font(int shapeType, int fontsize) throws ScriptException {
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    int sizeAdjust = 0;
    switch (iToken = statementLength) {
    case 5:
      if (getToken(4).tok != Token.identifier)
        invalidArgument();
      fontstyle = parameterAsString(4);
    //fall through
    case 4:
      if (getToken(3).tok != Token.identifier)
        invalidArgument();
      fontface = parameterAsString(3);
      if (getToken(2).tok != Token.integer)
        integerExpected();
      fontsize = intParameter(2);
      shapeType = getShapeType(getToken(1).tok);
      break;
    case 3:
      if (getToken(2).tok != Token.integer)
        integerExpected();
      if (shapeType == -1) {
        shapeType = getShapeType(getToken(1).tok);
        fontsize = intParameter(2);
      } else {//labels --- old set fontsize N
        fontsize += (sizeAdjust = 5);
      }
      break;
    case 2:
    default:
      if (shapeType == JmolConstants.SHAPE_LABELS) {
        //set fontsize
        fontsize = JmolConstants.LABEL_DEFAULT_FONTSIZE;
        break;
      }
      badArgumentCount();
    }
    if (shapeType == JmolConstants.SHAPE_LABELS) {        
      if (fontsize < JmolConstants.LABEL_MINIMUM_FONTSIZE
          || fontsize > JmolConstants.LABEL_MAXIMUM_FONTSIZE)
        numberOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE - sizeAdjust,
            JmolConstants.LABEL_MINIMUM_FONTSIZE - sizeAdjust);
    }
    Font3D font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
    viewer.loadShape(shapeType);
    setShapeProperty(shapeType, "font", font3d);
  }

  /* ****************************************************************************
   * ============================================================== 
   * SET implementations
   * ==============================================================
   */

  private void set() throws ScriptException {
    String key;
    if (statementLength == 1) {
      showString(viewer.getAllSettings(null));
      return;
    }
    boolean isJmolSet = (parameterAsString(0).equals("set"));
    if (isJmolSet && statementLength == 2 && (key = parameterAsString(1)).indexOf("?") >= 0) {
      showString(viewer.getAllSettings(key.substring(0, key.indexOf("?"))));
      return;
    }
    boolean showing = (!isSyntaxCheck && !tQuiet
        && scriptLevel <= scriptReportingLevel && !((String) statement[0].value)
        .equals("var"));
    int val = Integer.MAX_VALUE;
    int n = 0;
    switch (getToken(1).tok) {
    
    // THESE ARE DEPRECATED AND HAVE THEIR OWN COMMAND
    
    case Token.axes:
      axes(2);
      return;
    case Token.background:
      background(2);
      return;
    case Token.boundbox:
      boundbox(2);
      return;
    case Token.frank:
      frank(2);
      return;
    case Token.history:
      history(2);
      return;
    case Token.label:
      label(2);
      return;
    case Token.unitcell:
      unitcell(2);
      return;
    case Token.display://deprecated
    case Token.selectionHalo:
      selectionHalo(2);
      return;

      // THESE HAVE MULTIPLE CONTEXTS AND 
      // SO DO NOT ALLOW CALCULATIONS xxx = a + b...
      
    case Token.bondmode:
      setBondmode();
      return;
    case Token.echo:
      setEcho();
      return;
    case Token.fontsize:
      checkLength23();
      font(JmolConstants.SHAPE_LABELS, statementLength == 2 ? 0 : intParameter(2));
      return;
    case Token.hbond:
      setHbond();
      return;
    case Token.monitor:
      setMonitor();
      return;
    case Token.property: // considered reserved
      key = parameterAsString(1).toLowerCase();
      if (key.startsWith("property_")) {
      } else {
        setProperty();
        return;
      }
      break;
    case Token.picking:
      setPicking();
      return;
    case Token.pickingStyle:
      setPickingStyle();
      return;      
      
    // deprecated to other parameters   
    case Token.spin:
      checkLength4();
      setSpin(parameterAsString(2), (int) floatParameter(3));
      return;
    case Token.ssbond: //ssBondsBackbone
      setSsbond();
      return;

      // THESE NEXT DO ALLOW CALCULATIONS xxx = a + b...
      
    case Token.scale3d:
      setFloatProperty("scaleAngstromsPerInch", floatSetting(2));
      return;
    case Token.formalCharge:
      n = intSetting(2);
      if (!isSyntaxCheck)
        viewer.setFormalCharges(n);
      return;
    case Token.specular:
      if (statementLength == 2 || statement[2].tok != Token.integer) {
        key = "specular";
        break;
      }
    //fall through
    case Token.specpercent:
      key = "specularPercent";
      break;
    case Token.ambient:
      key = "ambientPercent";
      break;
    case Token.diffuse:
      key = "diffusePercent";
      break;
    case Token.specpower:
      val = intSetting(2);
      if (val >= 0) {
        key = "specularPower";
        break;
      }
      if (val < -10 || val > -1)
        numberOutOfRange(-10, -1);
      val = -val;
      key = "specularExponent";
      break;
    case Token.specexponent:
      key = "specularExponent";
      break;
    case Token.bonds:
      key = "showMultipleBonds";
      break;
    case Token.strands:
      key = "strandCount";  
      break;
    case Token.hetero:
      key = "selectHetero";
      break;
    case Token.hydrogen:
      key = "selectHydrogen";
      break;
    case Token.radius:
      key = "solventProbeRadius";
      break;
    case Token.solvent:
      key = "solventProbe";
      break;
    case Token.color:
    case Token.defaultColors:
      key = "defaultColorScheme";
      break;
    default:
      key = parameterAsString(1);
      if (key.charAt(0) == '_') //these cannot be set by user
        invalidArgument();

      //these next are not reported and do not allow calculation xxxx = a + b

      if (key.equalsIgnoreCase("toggleLabel")) {
        if (setLabel("toggle"))
          return;
      }
      if (key.toLowerCase().indexOf("label") == 0
          && Parser.isOneOf(key.toLowerCase(),"labelfront;labelgroup;labelatom;labeloffset;labelpointer;labelalignment;labeltoggle")) {
        if (setLabel(key.substring(5)))
          return;
      }
      if (key.equalsIgnoreCase("userColorScheme")) {
        setUserColors();
        return;
      }
      if (key.equalsIgnoreCase("defaultLattice")) {
        Point3f pt;
        Vector v = (Vector) parameterExpression(2, 0, "XXX", true);
        if (v == null || v.size() == 0)
          invalidArgument();
        Token token = (Token) v.elementAt(0);
        if (token.tok == Token.point3f)
          pt = (Point3f) token.value;
        else {
          int ijk = Token.iValue(token);
          if (ijk < 555)
            pt = new Point3f();
          else
            pt = UnitCell.ijkToPoint3f(ijk + 111);
        }
        if (!isSyntaxCheck)
          viewer.setDefaultLattice(pt);
        return;
      }
      
      // THESE CAN BE PART OF CALCULATIONS
      
      
      if (key.equalsIgnoreCase("defaultDrawArrowScale")) {
        setFloatProperty(key, floatSetting(2));
        return;
      }
      if (key.equalsIgnoreCase("logLevel")) {
        // set logLevel n
        // we have 5 levels 0 - 4 debug -- error
        // n = 0 -- no messages -- turn all off
        // n = 1 add level 4, error
        // n = 2 add level 3, warn
        // etc.
        int ilevel = intSetting(2);
        if (isSyntaxCheck)
          return;
        setIntProperty("logLevel", ilevel);
        return;
      }
      if (key.equalsIgnoreCase("backgroundModel")) {
        String modelDotted = stringSetting(2, false);
        int modelNumber;
        boolean useModelNumber = false;
        if (modelDotted.indexOf(".") < 0) {
          modelNumber = Parser.parseInt(modelDotted);
          useModelNumber = true;
        } else {
          modelNumber = Compiler.modelValue(modelDotted);
        }
        if (isSyntaxCheck)
          return;
        int modelIndex = viewer.getModelNumberIndex(modelNumber, useModelNumber, true);
        viewer.setBackgroundModelIndex(modelIndex);
        return;
      }
      if (key.equalsIgnoreCase("language")) {
        //language can be used without quotes in a SET context
        //set language en
        String lang = stringSetting(2, isJmolSet);
        setStringProperty(key, lang);
        return;
      }
      if (key.equalsIgnoreCase("trajectory") || key.equalsIgnoreCase("trajectories")) {
        Token token = tokenSetting(2); //if an expression, we are done
        if (token.tok == Token.decimal) //if a number, we just set its trajectory
          viewer.getModelNumberIndex(token.intValue, false, true);
        return;
      }
      // deprecated:

      if (key.equalsIgnoreCase("showSelections")) {
        key = "selectionHalos";
        break;
      }
      if (key.equalsIgnoreCase("measurementNumbers")) {
        key = "measurementLabels";
        break;
      }
    }

    if (getContextVariableAsToken(key) != null || !setParameter(key, val, isJmolSet, showing)) {
      int tok2 = (tokAt(1) == Token.expressionBegin ? 0 : tokAt(2));
      setVariable((tok2 == Token.opEQ ? 3 : 2), 0, key, showing);
      if (!isJmolSet)
        return;
    }
    if (showing)
      viewer.showParameter(key, true, 80);
  }

  private int intSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, 0, "XXX", true);
    if (v == null || v.size() == 0)
      invalidArgument();
    return Token.iValue((Token) v.elementAt(0));
  }

  private float floatSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, 0, "XXX", true);
    if (v == null || v.size() == 0)
      invalidArgument();
    return Token.fValue((Token) v.elementAt(0));
  }

  private String stringSetting(int pt, boolean isJmolSet) throws ScriptException {
    if (isJmolSet && statementLength == pt + 1)
        return parameterAsString(pt);
    Vector v = (Vector) parameterExpression(pt, 0, "XXX", true);
    if (v == null || v.size() == 0)
      invalidArgument();
    return Token.sValue((Token) v.elementAt(0));
  }

  private Token tokenSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, 0, "XXX", true);
    if (v == null || v.size() == 0)
      invalidArgument();
    return (Token) v.elementAt(0);
  }

  private void setVariable(int pt, int ptMax, String key, boolean showing)
      throws ScriptException {
    BitSet bs = null;
    String propertyName = "";
    if (tokAt(pt - 1) == Token.expressionBegin) {
      bs = expression(pt - 1);
      pt = iToken + 1;
    }
    int tokProperty = Token.nada;
    if (tokAt(pt) == Token.dot) {
      Token token = getBitsetPropertySelector(pt++, true);
      if (tokAt(++pt) != Token.opEQ)
        invalidArgument();
      pt++;
      tokProperty = token.intValue;
      propertyName = (String) token.value;
    } else if (bs != null)
      invalidArgument();
    String str;
    Token t = getContextVariableAsToken(key);
    boolean asVector = (t != null || tokProperty != Token.nada);
    Object v = parameterExpression(pt, ptMax, key, asVector);
    if (isSyntaxCheck || v == null)
      return;
    Token tv = (asVector ? (Token) ((Vector) v).get(0) : null);
    if (tokProperty != Token.nada) {
      if (bs == null) {
        if (t == null) {
          if (!((v = viewer.getParameter(key)) instanceof String))
            invalidArgument();
          v = getStringObjectAsToken((String) v, null);
          if (!(v instanceof Token))
            invalidArgument();
          t = (Token) v;
        }
        if (!(t.value instanceof BitSet))
          invalidArgument();
        bs = (BitSet) t.value;
      }
      if (propertyName.startsWith("property_")) {
        viewer.setData(propertyName, new Object[] { propertyName,
            Token.sValue(tv), bs }, viewer.getAtomCount(), 0,
            tv.tok == Token.list ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        return;
      }
      setBitsetProperty(bs, tokProperty, Token.iValue(tv), Token.fValue(tv), tv);
      return;
    }

    if (t != null) {
      t.value = tv.value;
      t.intValue = tv.intValue;
      t.tok = tv.tok;
    }
    if (key.startsWith("property_")) {
      int n = viewer.getAtomCount();
      viewer.setData(key,
          new Object[] { key, "" + v, viewer.getSelectionSet() }, n, 0,
          Integer.MIN_VALUE);
      return;
    }
    if (v == null)
      return;
    //viewer.removeUserVariable(key + "_set");
    if (v instanceof Boolean) {
      setBooleanProperty(key, ((Boolean) v).booleanValue());
    } else if (v instanceof Integer) {
      setIntProperty(key, ((Integer) v).intValue());
    } else if (v instanceof Float) {
      setFloatProperty(key, ((Float) v).floatValue());
    } else if (v instanceof String) {
      setStringProperty(key, (String) v);
    } else if (v instanceof BondSet) {
      //setIntProperty(key, BitSetUtil.cardinalityOf((BitSet) v));
      //setStringProperty(key + "_set", Escape.escape((BitSet) v, false));
      setStringProperty(key, Escape.escape((BitSet) v, false));
      //if (showing)
      //viewer.showParameter(key + "_set", true, 80);
    } else if (v instanceof BitSet) {
      //setIntProperty(key, BitSetUtil.cardinalityOf((BitSet) v));
      //setStringProperty(key + "_set", Escape.escape((BitSet) v));
      setStringProperty(key, Escape.escape((BitSet) v));
      //if (showing)
      //viewer.showParameter(key + "_set", true, 80);
    } else if (v instanceof Point3f) {
      //drawPoint(key, (Point3f) v, false);
      str = Escape.escape((Point3f) v);
      setStringProperty(key, str);
      //if (showing)
      //showString("to visualize, use DRAW @" + key);
    } else if (v instanceof Point4f) {
      //drawPlane(key, (Point4f) v, false);
      str = Escape.escape((Point4f) v);
      setStringProperty(key, str);
      //if (showing)
      //showString("to visualize, use ISOSURFACE PLANE @" + key);
    }
  }

  private boolean setParameter(String key, int intVal, boolean isJmolSet, boolean showing)
      throws ScriptException {
    String lcKey = key.toLowerCase();
    if (key.equalsIgnoreCase("scriptReportingLevel")) { //11.1.13
      intVal = intSetting(2);
      if (!isSyntaxCheck) {
        scriptReportingLevel = intVal;
        setIntProperty(key, intVal);
      }
      return true;
    }
    if (key.equalsIgnoreCase("historyLevel")) {
      intVal = intSetting(2);
      if (!isSyntaxCheck) {
        commandHistoryLevelMax = intVal;
        setIntProperty(key, intVal);
      }
      return true;
    }
    if (key.equalsIgnoreCase("dipoleScale")) {
      float scale = floatSetting(2);
      if (scale < -10 || scale > 10)
        numberOutOfRange(-10f, 10f);
      setFloatProperty("dipoleScale", scale);
      return true;
    }
    if (key.equalsIgnoreCase("axesScale")) {
      float scale = floatSetting(2);
      setFloatProperty("axesScale", scale);
      return true;
    }
    if (key.equalsIgnoreCase("measurementUnits")) {
      setMeasurementUnits(stringSetting(2, isJmolSet));
      return true;
    }
    if (Parser.isOneOf(lcKey, "defaults;defaultcolorscheme")) {
      String val;
      if ((theTok = tokAt(2)) == Token.jmol || theTok == Token.rasmol) {
        val = parameterAsString(2).toLowerCase();
        checkLength3();
      } else {
        val = stringSetting(2, false).toLowerCase();
      }
      if (!val.equals("jmol") && !val.equals("rasmol"))
        invalidArgument();
      setStringProperty((key.equalsIgnoreCase("defaults") ? key : "defaultColorScheme"), val);
      return true;
    }
    if (Parser.isOneOf(lcKey, "strandcount;strandcountformeshribbon;strandcountforstrands")) {
      intVal = intSetting(2);
      if (intVal < 0 || intVal > 20)
        numberOutOfRange(0, 20);
      setIntProperty(key, intVal);
      return true;
    }
    if (Parser.isOneOf(lcKey, "specularpercent;ambientpercent;diffusepercent;specularPower")) {
      if (intVal == Integer.MAX_VALUE)
        intVal = intSetting(2);
      if (intVal < 0 || intVal > 100)
        numberOutOfRange(0, 100);
      setIntProperty(key, intVal);
      return true;
    }
    if (key.equalsIgnoreCase("specularExponent")) {
      if (intVal == Integer.MAX_VALUE)
        intVal = intSetting(2);
      if (intVal > 10 || intVal < 1)
        numberOutOfRange(1, 10);
      setIntProperty(key, intVal);
      return true;
    }
    boolean isJmolParameter = viewer.isJmolVariable(key);
    if (isJmolSet && !isJmolParameter) {
      iToken = 1;
      unrecognizedParameter("SET", key);
    }
    switch (statementLength) {
    case 2:
      setBooleanProperty(key, true);
      return true;
    case 3:
      if (intVal != Integer.MAX_VALUE) {
        setIntProperty(key, intVal);
        return true;
      }
      getToken(2);
      if (theTok == Token.none) {
        if (!isSyntaxCheck)
          viewer.removeUserVariable(key);
      } else if (isJmolSet && theTok == Token.identifier) {
//        setStringProperty(key, (String) theToken.value);
      } else {
        return false;
      }
      return true;
    default:
      //if (isJmolSet)
        //invalidArgument();
    }
    return false;
  }

  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean asVector) throws ScriptException {
    Object v;
    boolean isSetCmd = (key != null && key.length() > 0);
    Rpn rpn = new Rpn(64, isSetCmd && tokAt(pt) == Token.leftsquare, asVector);
    if (ptMax < pt)
      ptMax = statementLength;
    out: for (int i = pt; i < ptMax; i++) {
      v = null;
      switch (getToken(i).tok) {
      case Token.semicolon: //for (i = 1; i < 3; i=i+1)
        break out;
      case Token.on:
      case Token.off:
      case Token.decimal:
      case Token.string:
      case Token.point3f:
      case Token.point4f:
      case Token.bitset:
        rpn.addX(theToken);
        break;
      case Token.spec_seqcode:
      case Token.integer:
        rpn.addX(Token.intToken(theToken.intValue));
        break;
      case Token.dollarsign:
        rpn.addX(new Token(Token.point3f, centerParameter(i)));
        i = iToken;
        break;
      case Token.leftbrace:
        v = getPointOrPlane(i, false, true, true, false, 3, 4);
        i = iToken;
        break;
      case Token.expressionBegin:
        v = expression(statement, i, 0, true, true, true);
        i = iToken;
        break;
      case Token.expressionEnd:
        i++;
        break out;
      case Token.rightbrace:
        invalidArgument();
      case Token.comma: //ignore commas
        if (!rpn.addOp(theToken))
          invalidArgument();
        break;
      case Token.dot:
        Token token = getBitsetPropertySelector(i, false);
        //check for added min/max modifier
        if (tokAt(iToken + 1) == Token.dot) {
          if (tokAt(iToken + 2) == Token.all) {
            token.intValue |= Token.minmaxmask;
            getToken(iToken + 2);
          }
          if (Compiler.tokAttrOr(tokAt(iToken + 2), Token.min, Token.max))
            token.intValue |= getToken(iToken + 2).tok;
        }
        if (!rpn.addOp(token))
          invalidArgument();
        i = iToken;
        break;
      default:
        if (theTok == Token.identifier
            && compiler.isFunction((String) theToken.value)) {
          if (!rpn.addOp(new Token(Token.function, theToken.value))) {
            //iToken--;
            invalidArgument();
          }
        } else if (Compiler.tokAttr(theTok, Token.mathop)
            || Compiler.tokAttr(theTok, Token.mathfunc)) {
          if (!rpn.addOp(theToken)) {
            //iToken--;
            invalidArgument();
          }
        } else {
          String name = parameterAsString(i);
          if (isSyntaxCheck) {
            v = name;
          } else {
            //            if (tokAt(i + 1) == Token.leftsquare || tokAt(i + 1) == Token.dot) {
            //            Object o = getParameter(name, true);
            //Object o = getParameter(name+"_set", true);
            //              if (o instanceof String && ((String) o).indexOf("({") == 0)
            //              name += "_set";            
            //        }
            v = getParameter(name, true);
            if (v instanceof String)
              v = getStringObjectAsToken((String) v, name);
          }
          break;

        }
      }
      if (v != null)
        if (v instanceof Boolean) {
          rpn.addX(((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          rpn.addX(Token.intToken(((Integer) v).intValue()));
        } else if (v instanceof Float) {
          rpn.addX(new Token(Token.decimal, v));
        } else if (v instanceof String) {
          rpn.addX(new Token(Token.string, v));
        } else if (v instanceof Point3f) {
          rpn.addX(new Token(Token.point3f, v));
        } else if (v instanceof Point4f) {
          rpn.addX(new Token(Token.point4f, v));
        } else if (v instanceof BitSet) {
          rpn.addX(new Token(Token.bitset, v));
        } else if (v instanceof Token) {
          rpn.addX((Token) v);
        } else {
          invalidArgument();
        }
    }
    Token result = rpn.getResult(false, key);
    if (result == null) {
      if (!isSyntaxCheck)
        rpn.dumpStacks();
      endOfStatementUnexpected();
    }
    if (result.tok == Token.vector)
      return result.value;
    if (key == null)
      return Boolean.valueOf(Token.bValue(result));
    if (key.length() == 0) {
      if (result.tok == Token.string)
        result.intValue = Integer.MAX_VALUE;
      return Token.sValue(result);
    }
    if (result.tok == Token.string && result.intValue != Integer.MAX_VALUE) {
      if (!isSyntaxCheck && !insertArrayValue(key, result))
        invalidArgument();
      return (String) null;
    }
    switch (result.tok) {
    case Token.on:
    case Token.off:
      return Boolean.valueOf(result == Token.tokenOn);
    case Token.integer:
      return new Integer(result.intValue);
    case Token.bitset:
    case Token.decimal:
    case Token.string:
    case Token.point3f:
    default:
      return result.value;
    }
  }

  private boolean insertArrayValue(String key, Token result) {
    int selector = result.intValue;
    if (selector == Integer.MAX_VALUE)
      return false;
    result.intValue = Integer.MAX_VALUE;
    String s = Token.sValue(result);
    Object v = getParameter(key, false);
    if (!(v instanceof String))
      return false;
    v = getStringObjectAsToken((String) v, key);
    if (v instanceof Token) {
      Token token = (Token) v;
      if (token.tok != Token.list)
        return false;
      String[] array = (String[]) token.value;
      if (selector <= 0)
        selector = array.length + selector;
      if (--selector < 0)
        selector = 0;
      if (array.length > selector) {
        array[selector] = s;
        viewer.setListVariable(key, token);
        return true;
      }
      String[] arrayNew = ArrayUtil.ensureLength(array, selector + 1);
      for (int i = array.length; i <= selector; i++)
        arrayNew[i] = "";
      arrayNew[selector] = s;
      token.value = arrayNew;
      viewer.setListVariable(key, token);
      return true;
    } else if (v instanceof String) {
      String str = (String) v;
      int pt = str.length();
      if (selector <= 0)
        selector = pt + selector;
      if (--selector < 0)
        selector = 0;
      while (selector >= str.length())
        str += " ";
      str = str.substring(0, selector) + s + str.substring(selector + 1);
      setStringProperty(key, str);
      return true;
    }
    return false;
  }

  private void assignBitsetVariable(String variable, BitSet bs) {
    variables.put(variable, bs);
    setStringProperty("@" + variable, Escape.escape(bs));
  }

  String getBitsetIdent(BitSet bs, String label, Object tokenValue,
                        boolean useAtomMap) {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    if (isAtoms && label == null)
      label = viewer.getStandardLabelFormat();
    int pt = (label == null ? -1 : label.indexOf("%"));
    if (bs == null || isSyntaxCheck || isAtoms && pt < 0)
      return (label == null ? "" : label);
    StringBuffer s = new StringBuffer();
    int len = bs.size();
    ModelSet modelSet = viewer.getModelSet();
    int n = 0;
    int[] indices = (isAtoms || !useAtomMap ? null : ((BondSet) tokenValue)
        .getAssociatedAtoms());
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = viewer.getAtomIndices(bs);
    int nProp = 0;
    String[] props = null;
    float[][] propArray = null;
    while (pt >= 0 && (pt = label.indexOf("{", pt + 1)) > 0) {
      int pt2 = label.indexOf("}", pt);
      if (pt2 > 0) {
        if (nProp == 0) {
          for (int j = pt; j < label.length(); j++)
            if (label.charAt(j) == '{')
              nProp++;
          props = new String[nProp];
          propArray = new float[nProp][];
          nProp = 0;
        }
        String name = label.substring(pt + 1, pt2);
        float[] f = viewer.getDataFloat(name);
        if (f != null) {
          propArray[nProp] = f;
          props[nProp++] = '{' + name + '}';
        }
      }
      pt = pt2;

    }
    for (int j = 0; j < len; j++)
      if (bs.get(j)) {
        String str = label;
        if (isAtoms) {
          if (str == null) {
            str = modelSet.getAtomAt(j).getInfo();
          } else {
            str = modelSet.getAtomAt(j).formatLabel(str, '\0', indices);
            for (int k = 0; k < nProp; k++)
              if (j < propArray[k].length)
                str = TextFormat.formatString(str, props[k], propArray[k][j]);
          }
        } else {
          Bond bond = modelSet.getBondAt(j);
          if (str == null)
            str = bond.getIdentity();
          else {
            str = bond.formatLabel(str, indices);
            int ia1 = bond.getAtomIndex1();
            int ia2 = bond.getAtomIndex2();
            for (int k = 0; k < nProp; k++)
              if (ia1 < propArray[k].length)
                str = TextFormat.formatString(str, props[k] + "1",
                    propArray[k][ia1]);
            for (int k = 0; k < nProp; k++)
              if (ia2 < propArray[k].length)
                str = TextFormat.formatString(str, props[k] + "2",
                    propArray[k][ia2]);
          }
        }
        str = TextFormat.formatString(str, "#", ++n);
        if (n > 1)
          s.append("\n");
        s.append(str);
      }
    return s.toString();
  }

  private Token getBitsetPropertySelector(int i, boolean mustBeSettable)
      throws ScriptException {
    int tok = getToken(++i).tok;
    String s = parameterAsString(i).toLowerCase();
    switch (tok) {
    default:
      if (Compiler.tokAttrOr(tok, Token.atomproperty, Token.mathproperty))
        break;
      invalidArgument();
    case Token.property:
      break;
    case Token.identifier:
      if (s.equals("x"))
        tok = Token.atomX;
      else if (s.equals("y"))
        tok = Token.atomY;
      else if (s.equals("z"))
        tok = Token.atomZ;
      else
        invalidArgument();
      break;
    }
    if (mustBeSettable && !Compiler.tokAttr(tok, Token.settable))
      invalidArgument();
    return new Token(Token.propselector, tok, s);
  }

  protected Object getBitsetProperty(BitSet bs, int tok, Point3f ptRef,
                                     Point4f planeRef, Object tokenValue,
                                     Object opValue, boolean useAtomMap)
      throws ScriptException {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    boolean isMin = Compiler.tokAttr(tok, Token.min);
    boolean isMax = Compiler.tokAttr(tok, Token.max);
    boolean isAll = Compiler.tokAttr(tok, Token.minmaxmask);
    if (isAll)
      isMin = isMax = false;
    tok &= ~Token.minmaxmask;
    float[] list = null;
    BitSet bsNew = null;

    if (tok == Token.atoms)
      bsNew = (!isAtoms && !isSyntaxCheck ? viewer.getAtomsWithin(Token.bonds,
          bs) : bs);
    if (tok == Token.bonds)
      bsNew = (isAtoms && !isSyntaxCheck ? viewer.getBondsForSelectedAtoms(bs)
          : bs);
    if (bsNew != null) {
      if (!isMax && !isMin || isSyntaxCheck)
        return bsNew;
      int n = bsNew.size();
      int i = 0;
      if (isMin) {
        for (i = -1; ++i < n;)
          if (bsNew.get(i))
            break;
      } else if (isMax) {
        for (i = n; --i >= 0;)
          if (bsNew.get(i))
            break;
      }
      bsNew.clear();
      if (i >= 0 && i < n)
        bsNew.set(i);
      return bsNew;
    }

    if (tok == Token.ident)
      return (isMin || isMax ? "" : getBitsetIdent(bs, null, tokenValue,
          useAtomMap));

    int n = 0;
    int ivAvg = 0, ivMax = Integer.MIN_VALUE, ivMin = Integer.MAX_VALUE;
    float fvAvg = 0, fvMax = -Float.MAX_VALUE, fvMin = Float.MAX_VALUE;
    Point3f pt = new Point3f();
    if (tok == Token.distance && ptRef == null && planeRef == null)
      return pt;

    boolean isInt = true;
    Point3f ptT = (tok == Token.color ? new Point3f() : null);
    ModelSet modelSet = viewer.getModelSet();
    float[] data = (tok == Token.property ? viewer
        .getDataFloat((String) opValue) : null);
    int count = 0;
    if (isAtoms) {
      count = (isSyntaxCheck ? 0 : viewer.getAtomCount());
      if (isAll)
        list = new float[count];
      for (int i = 0; i < count; i++)
        if (bs == null || bs.get(i)) {
          n++;
          Atom atom = modelSet.getAtomAt(i);
          if (isInt) {
            int iv = 0;
            switch (tok) {
            case Token.atomno:
              iv = atom.getAtomNumber();
              break;
            case Token.atomIndex:
              iv = i;
              break;
            case Token.elemno:
              iv = atom.getElementNumber();
              break;
            case Token.element:
              iv = atom.getAtomicAndIsotopeNumber();
              break;
            case Token.formalCharge:
              iv = atom.getFormalCharge();
              break;
            case Token.site:
              iv = atom.getAtomSite();
              break;
            case Token.symop:
              // a little weird
              BitSet bsSym = atom.getAtomSymmetry();
              int len = bsSym.size();
              int p = 0;
              int ivvMin = Integer.MAX_VALUE;
              int ivvMax = Integer.MIN_VALUE;
              for (int k = 0; k < len; k++)
                if (bsSym.get(k)) {
                  iv += k + 1;
                  ivvMin = Math.min(ivvMin, k + 1);
                  ivvMax = Math.max(ivvMax, k + 1);
                  p++;
                }
              if (isMin)
                iv = ivvMin;
              else if (isMax)
                iv = ivvMax;
              n += p - 1;
              break;
            case Token.molecule:
              iv = atom.getMoleculeNumber();
              break;
            case Token.occupancy:
              iv = atom.getOccupancy();
              break;
            case Token.polymerLength:
              iv = atom.getPolymerLength();
              break;
            case Token.resno:
              iv = atom.getResno();
              break;
            case Token.groupID:
              iv = atom.getGroupID();
              break;
            case Token.atomID:
              iv = atom.getSpecialAtomID();
              break;
            case Token.structure:
              iv = atom.getProteinStructureType();
              break;
            case Token.bondcount:
              iv = atom.getCovalentBondCount();
              break;
            case Token.valence:
              iv = atom.getValence();
              break;
            case Token.file:
              iv = atom.getModelFileIndex() + 1;
              break;
            case Token.model:
              iv = atom.getModelNumber();
              break;
            default:
              isInt = false;
              break;
            }
            if (isInt) {
              if (isAll)
                list[i] = iv;
              else if (isMin)
                ivMin = Math.min(ivMin, iv);
              else if (isMax)
                ivMax = Math.max(ivMax, iv);
              else
                ivAvg += iv;
              continue;
            }
          }

          //floats 

          float fv = Float.MAX_VALUE;

          switch (tok) {
          case Token.property:
            fv = (data == null ? 0 : data[i]);
            break;
          case Token.atomX:
            fv = atom.x;
            break;
          case Token.atomY:
            fv = atom.y;
            break;
          case Token.atomZ:
            fv = atom.z;
            break;
          case Token.fracX:
            fv = atom.getFractionalCoord('X');
            break;
          case Token.fracY:
            fv = atom.getFractionalCoord('Y');
            break;
          case Token.fracZ:
            fv = atom.getFractionalCoord('Z');
            break;
          case Token.vibX:
            fv = atom.getVibrationCoord('x');
            break;
          case Token.vibY:
            fv = atom.getVibrationCoord('y');
            break;
          case Token.vibZ:
            fv = atom.getVibrationCoord('z');
            break;
          case Token.distance:
            if (planeRef != null)
              fv = Graphics3D.distanceToPlane(planeRef, atom);
            else
              fv = atom.distance(ptRef);
            break;
          case Token.radius:
            fv = atom.getRadius();
            break;
          case Token.vanderwaals:
            fv = atom.getVanderwaalsRadiusFloat();
            break;
          case Token.partialCharge:
            fv = atom.getPartialCharge();
            break;
          case Token.phi:
            fv = atom.getGroupPhi();
            break;
          case Token.psi:
            fv = atom.getGroupPsi();
            break;
          case Token.surfacedistance:
            viewer.getSurfaceDistanceMax();
            fv = atom.getSurfaceDistance100() / 100f;
            break;
          case Token.temperature: // 0 - 9999
            fv = atom.getBfactor100() / 100f;
            break;
          case Token.xyz:
            pt.add(atom);
            break;
          case Token.fracXyz:
            pt.add(atom.getFractionalCoord());
            break;
          case Token.vibXyz:
            pt.add(new Point3f(atom.getVibrationVector()));
            break;
          case Token.color:
            pt.add(Graphics3D.colorPointFromInt(viewer.getColixArgb(atom
                .getColix()), ptT));
            break;
          default:
            unrecognizedAtomProperty(Token.nameOf(tok));
          }

          if (fv != Float.MAX_VALUE) {
            if (isAll)
              list[i] = fv;
            else if (isMin)
              fvMin = Math.min(fvMin, fv);
            else if (isMax)
              fvMax = Math.max(fvMax, fv);
            else
              fvAvg += fv;
          }
        }
    } else {
      count = viewer.getBondCount();
      if (isAll)
        list = new float[count];
      for (int i = 0; i < count; i++)
        if (bs == null || bs.get(i)) {
          n++;
          Bond bond = modelSet.getBondAt(i);
          switch (tok) {
          case Token.length:
            float fv = bond.getAtom1().distance(bond.getAtom2());
            fvMin = Math.min(fvMin, fv);
            fvMax = Math.max(fvMax, fv);
            fvAvg += fv;
            if (isAll)
              list[i] = fv;
            break;
          case Token.xyz:
            pt.add(bond.getAtom1());
            pt.add(bond.getAtom2());
            n++;
            break;
          case Token.color:
            pt.add(Graphics3D.colorPointFromInt(viewer.getColixArgb(bond
                .getColix()), ptT));
            break;
          default:
            unrecognizedBondProperty(Token.nameOf(tok));
          }
          isInt = false;
        }
    }
    if (tok == Token.xyz || tok == Token.color)
      return (n == 0 ? pt : new Point3f(pt.x / n, pt.y / n, pt.z / n));
    if (n == 0)
      return new Float(Float.NaN);

    if (isMin) {
      n = 1;
      ivAvg = ivMin;
      fvAvg = fvMin;
    } else if (isMax) {
      n = 1;
      ivAvg = ivMax;
      fvAvg = fvMax;
    } else if (isAll) {
      float[] list1 = new float[n];
      for (int i = 0, j = 0; i < count; i++)
        if (bs == null || bs.get(i))
          list1[j++] = list[i];
      if (opValue == null) //not operating -- I don't think this is possible, because opValue is not ever null when minmax is set
        return list1;
      return Escape.escape(list1);
    }
    if (isInt && (ivAvg / n) * n == ivAvg)
      return new Integer(ivAvg / n);
    return new Float((isInt ? ivAvg * 1f : fvAvg) / n);
  }

  private void setBitsetProperty(BitSet bs, int tok, int iValue, float fValue,
                                 Token tokenValue) throws ScriptException {
    if (isSyntaxCheck || BitSetUtil.cardinalityOf(bs) == 0)
      return;
    String[] list;
    int nValues;
    switch (tok) {
    case Token.xyz:
    case Token.fracXyz:
    case Token.vibXyz:
      if (tokenValue.tok == Token.point3f) {
        viewer.setAtomCoord(bs, tok, tokenValue.value);
      } else if (tokenValue.tok == Token.list) {
        list = (String[]) tokenValue.value;
        if ((nValues = list.length) == 0)
          return;
        Point3f[] values = new Point3f[nValues];
        for (int i = nValues; --i >= 0;) {
          Object o = Escape.unescapePoint(list[i]);
          if (!(o instanceof Point3f))
            unrecognizedParameter("ARRAY", list[i]);
          values[i] = (Point3f) o;
        }
        viewer.setAtomCoord(bs, tok, values);
      }
      break;
    case Token.color:
      if (tokenValue.tok == Token.point3f)
        iValue = colorPtToInt((Point3f) tokenValue.value);
      else if (tokenValue.tok == Token.list) {
        list = (String[]) tokenValue.value;
        if ((nValues = list.length) == 0)
          return;
        int[] values = new int[nValues];
        for (int i = nValues; --i >= 0;) {
          Object pt = Escape.unescapePoint(list[i]);
          if (pt instanceof Point3f)
            values[i] = colorPtToInt((Point3f) pt);
          else
            values[i] = Graphics3D.getArgbFromString(list[i]);
          if (values[i] == 0
              && (values[i] = Parser.parseInt(list[i])) == Integer.MIN_VALUE)
            unrecognizedParameter("ARRAY", list[i]);
        }
        viewer.setShapeProperty(JmolConstants.SHAPE_BALLS, "colorValues",
            values, bs);
        break;
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_BALLS, "color",
          tokenValue.tok == Token.string ? tokenValue.value : new Integer(
              iValue), bs);
      break;
    default:
      float[] fvalues = null;
      if (tokenValue.tok == Token.list || tokenValue.tok == Token.string) {
        list = (tokenValue.tok == Token.list ? (String[]) tokenValue.value
            : Parser.getTokens(Token.sValue(tokenValue)));
        if ((nValues = list.length) == 0)
          return;
        fvalues = new float[nValues];
        for (int i = nValues; --i >= 0;)
          fvalues[i] = Parser.parseFloat(list[i]);
      }
      viewer.setAtomProperty(bs, tok, iValue, fValue, fvalues);
    }
  }

  private void axes(int index) throws ScriptException {
    if (statementLength == 1) {
      setShapeSize(JmolConstants.SHAPE_AXES, 1);
      return;
    }
    String type = optParameterAsString(index).toLowerCase();
    if (statementLength == index + 1
        && Parser.isOneOf(type, "window;unitcell;molecular")) {
      setBooleanProperty("axes" + type, true);
      return;
    }
    // axes = scale x.xxx
    if (statementLength == index + 2 && type.equals("scale")) {
      setFloatProperty("axesScale", floatParameter(++index));
      return;
    }
    short mad = getSetAxesTypeMad(index);
    if (!isSyntaxCheck)
      viewer.setObjectMad(JmolConstants.SHAPE_AXES, "axes", mad);
  }

  private void boundbox(int index) throws ScriptException {
    boolean byCorner = false;
    if (tokAt(index) == Token.identifier)
      byCorner = (parameterAsString(index).equalsIgnoreCase("corners"));
    if (byCorner)
      index++;
    if (isCenterParameter(index)) {
      expressionResult = null;
      Point3f pt1 = centerParameter(index);
      index = iToken + 1;
      if (byCorner || isCenterParameter(index)) {
        //boundbox CORNERS {expressionOrPoint1} {expressionOrPoint2}
        //boundbox {expressionOrPoint1} {vector}
        Point3f pt2 = (byCorner ? centerParameter(index) : getPoint3f(index,
            true));
        index = iToken + 1;
        if (!isSyntaxCheck)
          viewer.setBoundBox(pt1, pt2, byCorner);
      } else if (expressionResult != null && expressionResult instanceof BitSet) {
        //boundbox {expression}
        if (!isSyntaxCheck)
          viewer.calcBoundBoxDimensions((BitSet) expressionResult);
      } else {
        invalidArgument();
      }
      if (index == statementLength)
        return;
    }
    short mad = getSetAxesTypeMad(index);
    if (!isSyntaxCheck)
      viewer.setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", mad);
  }

  private void unitcell(int index) throws ScriptException {
    if (statementLength == 1) {
      if (!isSyntaxCheck)
        viewer.setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitcell", (short) 1);
      return;
    }
    if (statementLength == index + 1) {
      if (getToken(index).tok == Token.integer && intParameter(index) >= 111) {
        if (!isSyntaxCheck)
          viewer.setCurrentUnitCellOffset(intParameter(index));
      } else {
        short mad = getSetAxesTypeMad(index);
        if (!isSyntaxCheck)
          viewer.setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitCell", mad);
      }
      return;
    }
    // .xyz here?
    Point3f pt = (Point3f) getPointOrPlane(2, false, true, false, true, 3, 3);
    if (!isSyntaxCheck)
      viewer.setCurrentUnitCellOffset(pt);
  }

  private void frank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  private void setUserColors() throws ScriptException {
    Vector v = new Vector();
    for (int i = 2; i < statementLength; i++) {
      int argb = getArgbParam(i);
      v.addElement(new Integer(argb));
      i = iToken;
    }
    if (isSyntaxCheck)
      return;
    int n = v.size();
    int[] scale = new int[n];
    for (int i = n; --i >= 0;)
      scale[i] = ((Integer) v.elementAt(i)).intValue();
    Viewer.setUserScale(scale);
  }

  private void setBondmode() throws ScriptException {
    checkLength3();
    boolean bondmodeOr = false;
    switch (getToken(2).tok) {
    case Token.opAnd:
      break;
    case Token.opOr:
      bondmodeOr = true;
      break;
    default:
      invalidArgument();
    }
    setBooleanProperty("bondModeOr", bondmodeOr);
  }

  private void selectionHalo(int pt) throws ScriptException {
    if (pt == statementLength) {
      setBooleanProperty("selectionHalos", true);
      return;
    }
    if (pt + 1 < statementLength)
      checkLength3();
    boolean showHalo = false;
    switch (getToken(pt).tok) {
    case Token.on:
    case Token.selected:
      showHalo = true;
    case Token.off:
    case Token.none:
    case Token.normal:
      setBooleanProperty("selectionHalos", showHalo);
      break;
    default:
      invalidArgument();
    }
  }

  private void setEcho() throws ScriptException {
    String propertyName = "target";
    Object propertyValue = null;
    boolean echoShapeActive = true;
    //set echo xxx
    switch (getToken(2).tok) {
    case Token.off:
      checkLength3();
      echoShapeActive = false;
      propertyName = "allOff";
      break;
    case Token.none:
      echoShapeActive = false;
    //fall through
    case Token.all:
      checkLength3();
    //fall through
    case Token.left:
    case Token.right:
    case Token.top:
    case Token.bottom:
    case Token.center:
    case Token.identifier:
      propertyValue = parameterAsString(2);
      break;
    case Token.model:
      int modelIndex = modelNumberParameter(statement[3]);
      if (isSyntaxCheck)
        return;
      if (modelIndex >= viewer.getModelCount())
        invalidArgument();
      propertyName = "model";
      propertyValue = new Integer(modelIndex);
      break;
    case Token.string:
      echo(2);
      return;
    default:
      invalidArgument();
    }
    if (!isSyntaxCheck) {
      viewer.setEchoStateActive(echoShapeActive);
      viewer.loadShape(JmolConstants.SHAPE_ECHO);
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
    }
    if (statementLength == 3)
      return;
    propertyName = "align";
    // set echo name xxx
    if (statementLength == 4) {
      if (isCenterParameter(3)) {
        setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", centerParameter(3));
        return;
      }
      switch (getToken(3).tok) {
      case Token.off:
        propertyName = "off";
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(statement[4]);
        if (isSyntaxCheck)
          return;
        if (modelIndex >= viewer.getModelCount())
          invalidArgument();
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.left:
      case Token.right:
      case Token.top:
      case Token.bottom:
      case Token.center:
      case Token.identifier: //middle
        propertyValue = parameterAsString(3);
        break;
      default:
        invalidArgument();
      }
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
      return;
    }
    //set echo name script "some script"
    //set echo name model x.y
    if (statementLength == 5) {
      switch (tokAt(3)) {
      case Token.script:
        propertyName = "script";
        propertyValue = parameterAsString(4);
        setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
        return;
      case Token.model:
        int modelIndex = modelNumberParameter(statement[4]);
        if (!isSyntaxCheck && modelIndex >= viewer.getModelCount())
          invalidArgument();
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
        return;
      }
    }
    //set echo name x-pos y-pos
    getToken(4);
    int i = 3;
    //set echo name {x y z}
    if (isCenterParameter(i)) {
      setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", centerParameter(i));
      return;
    }
    int pos = intParameter(i++);
    String type;
    propertyValue = new Integer(pos);
    if (tokAt(i) == Token.percent) {
      type = "%xpos";
      i++;
    } else {
      type = "xpos";
    }
    setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
    pos = intParameter(i++);
    propertyValue = new Integer(pos);
    if (tokAt(i) == Token.percent) {
      type = "%ypos";
      i++;
    } else {
      type = "ypos";
    }
    setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
  }

  private boolean setLabel(String str) throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    Object propertyValue = null;
    while (true) {
      if (str.equals("offset")) {
        int xOffset = intParameter(2);
        int yOffset = intParameter(3);
        if (xOffset > 100 || yOffset > 100 || xOffset < -100 || yOffset < -100)
          numberOutOfRange(-100, 100);
        propertyValue = new Integer(((xOffset & 0xFF) << 8) | (yOffset & 0xFF));
        break;
      }
      if (str.equals("alignment")) {
        switch (getToken(2).tok) {
        case Token.left:
        case Token.right:
        case Token.center:
          str = "align";
          propertyValue = theToken.value;
          break;
        default:
          invalidArgument();
        }
        break;
      }
      if (str.equals("pointer")) {
        int flags = Text.POINTER_NONE;
        switch (getToken(2).tok) {
        case Token.off:
        case Token.none:
          break;
        case Token.background:
          flags |= Text.POINTER_BACKGROUND;
        case Token.on:
          flags |= Text.POINTER_ON;
          break;
        default:
          invalidArgument();
        }
        propertyValue = new Integer(flags);
        break;
      }
      if (str.equals("toggle")) {
        iToken = 1;
        BitSet bs = (statementLength == 2 ? null : expression(2));
        checkStatementLength(iToken + 1);
        if (!isSyntaxCheck)
          viewer.togglePickingLabel(bs);
        return true;
      }
      iToken = 1;
      boolean TF = (statementLength == 2 || getToken(2).tok == Token.on);
      if (str.equals("front") || str.equals("group")) {
        if (!TF && tokAt(2) != Token.off)
          invalidArgument();
        if (!TF)
          str = "front";
        propertyValue = (TF ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
      if (str.equals("atom")) {
        if (!TF && tokAt(2) != Token.off)
          invalidArgument();
        str = "front";
        propertyValue = (TF ? Boolean.FALSE : Boolean.TRUE);
        break;
      }
      return false;
    }
    BitSet bs = (iToken + 1 < statementLength ? expression(++iToken) : null);
    checkStatementLength(iToken + 1);
    if (isSyntaxCheck)
      return true;
    if (bs != null)
      viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue, bs);
    else      setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue);
    return true;
  }

  private void setMonitor() throws ScriptException {
    //on off here incompatible with "monitor on/off" so this is just a SET option.
    boolean showMeasurementNumbers = false;
    checkLength3();
    switch (tokAt(2)) {
    case Token.on:
      showMeasurementNumbers = true;
    case Token.off:
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
          showMeasurementNumbers ? Boolean.TRUE : Boolean.FALSE);
      return;
    case Token.identifier:
      setMeasurementUnits(parameterAsString(2));
      return;
    }
    setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad(2));
  }

  private void setMeasurementUnits(String units) throws ScriptException {
    if (!StateManager.isMeasurementUnit(units))
      unrecognizedParameter("set measurementUnits ", units);
    if (!isSyntaxCheck)
      viewer.setMeasureDistanceUnits(units);
  }
  
  private void setProperty() throws ScriptException {
    //what possible good is this? 
    //set property foo bar  is identical to
    //set foo bar
    checkLength4();
    if (getToken(2).tok != Token.identifier)
      propertyNameExpected();
    String propertyName = parameterAsString(2);
    switch (getToken(3).tok) {
    case Token.on:
      setBooleanProperty(propertyName, true);
      break;
    case Token.off:
      setBooleanProperty(propertyName, false);
      break;
    case Token.integer:
      setIntProperty(propertyName, intParameter(3));
      break;
    case Token.decimal:
      setFloatProperty(propertyName, floatParameter(3));
      break;
    case Token.string:
      setStringProperty(propertyName, stringParameter(3));
      break;
    default:
      unrecognizedParameter("SET " + propertyName.toUpperCase(),
          parameterAsString(3));
    }
  }

  private void setSpin(String key, int value) throws ScriptException {
    key = key.toLowerCase();
    if (Parser.isOneOf(key, "x;y;z;fps")) {
      if (!isSyntaxCheck)
        viewer.setSpin(key, value);
      return;
    }
    unrecognizedParameter("set SPIN ", parameterAsString(2));
  }

  private void setSsbond() throws ScriptException {
    checkLength3();
    boolean ssbondsBackbone = false;
    //viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch (tokAt(2)) {
    case Token.backbone:
      ssbondsBackbone = true;
      break;
    case Token.sidechain:
      break;
    default:
      invalidArgument();
    }
    setBooleanProperty("ssbondsBackbone", ssbondsBackbone);
  }

  private void setHbond() throws ScriptException {
    checkLength3();
    boolean bool = false;
    switch (tokAt(2)) {
    case Token.backbone:
      bool = true;
    // fall into
    case Token.sidechain:
      setBooleanProperty("hbondsBackbone", bool);
      break;
    case Token.solid:
      bool = true;
    // falll into
    case Token.dotted:
      setBooleanProperty("hbondsSolid", bool);
      break;
    default:
      invalidArgument();
    }
  }

  private void setPicking() throws ScriptException {
    if (statementLength == 2) {
      setStringProperty("picking", "ident");
      return;
    }
    if (statementLength > 4 || tokAt(2) == Token.string) {
        setStringProperty("picking", stringSetting(2, false));
        return;
    }
    int i = 2;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.select:
    case Token.monitor:
    case Token.spin:
      checkLength34();
      if (statementLength == 4) {
        type = parameterAsString(2).toUpperCase();
        if (type.equals("SPIN"))
          setIntProperty("pickingSpinRate", intParameter(3));
        else
          i = 3;
      }
      break;
    default:
      checkLength3();
    }
    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case Token.on:
    case Token.normal:
      str = "ident";
      break;
    case Token.none:
      str = "off";
      break;
    case Token.select:
      str = "atom";
      break;
    case Token.bonds: //not implemented
      str = "bond";
      break;
    }
    if (JmolConstants.getPickingMode(str) < 0)
      unrecognizedParameter("SET PICKING " + type, str);
    setStringProperty("picking", str);
  }

  private void setPickingStyle() throws ScriptException {
    if (statementLength > 4 || tokAt(2) == Token.string) {
      setStringProperty("pickingStyle", stringSetting(2, false));
      return;
    }
    int i = 2;
    boolean isMeasure = false;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.monitor:
      isMeasure = true;
      type = "MEASURE";
      //fall through
    case Token.select:
      checkLength34();
      if (statementLength == 4)
        i = 3;
      break;
    default:
      checkLength3();
    }
    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case Token.none:
    case Token.off:
      str = (isMeasure ? "measureoff" : "toggle");
      break;
    case Token.on:
      if (isMeasure)
        str = "measure";
      break;
    }
    if (JmolConstants.getPickingStyle(str) < 0)
      unrecognizedParameter("SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  /* ****************************************************************************
   * ==============================================================
   * SAVE/RESTORE 
   * ==============================================================
   */

  private void save() throws ScriptException {
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      switch (tokAt(1)) {
      case Token.orientation:
        if (!isSyntaxCheck)
          viewer.saveOrientation(saveName);
        return;
      case Token.bonds:
        if (!isSyntaxCheck)
          viewer.saveBonds(saveName);
        return;
      case Token.state:
        if (!isSyntaxCheck)
          viewer.saveState(saveName);
        return;
      case Token.structure:
        if (!isSyntaxCheck)
          viewer.saveStructure(saveName);
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.saveSelection(saveName);
          return;
        }
      }
    }
    evalError(GT._("save what?")
        + " bonds? orientation? selection? state? structure?");
  }

  private void restore() throws ScriptException {
    //restore orientation name time
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      if (getToken(1).tok != Token.orientation)
        checkLength23();
      switch (getToken(1).tok) {
      case Token.orientation:
        float timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
        if (timeSeconds < 0)
          invalidArgument();
        if (!isSyntaxCheck)
          viewer.restoreOrientation(saveName, timeSeconds);
        return;
      case Token.bonds:
        if (!isSyntaxCheck)
          viewer.restoreBonds(saveName);
        return;
      case Token.state:
        if (isSyntaxCheck)
          return;
        String state = viewer.getSavedState(saveName);
        if (state == null)
          invalidArgument();
        //state = TextFormat.simpleReplace(state, "\\\"", "\"");
        runScript(state);
        return;
      case Token.structure:
        if (isSyntaxCheck)
          return;
        String shape = viewer.getSavedStructure(saveName);
        if (shape == null)
          invalidArgument();
        runScript(shape);
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.restoreSelection(saveName);
          return;
        }
      }
    }
    evalError(GT._("restore what?")
        + " bonds? orientation? selection? state? structure?");
  }

  private void write() throws ScriptException {
    int pt = 1;
    boolean isApplet = viewer.isApplet();
    String driverList = viewer.getExportDriverList();
    int tok = (statementLength == 1 ? Token.clipboard : tokAt(pt));
    int len = 0;
    int width = -1;
    int height = -1;
    String type = "SPT";
    String data = "";
    String type2 = "";
    String fileName = null;
    boolean isCoord = false;
    boolean isShow = false;
    boolean isExport = false;
    int quality = Integer.MIN_VALUE;
    switch (tok) {
    case Token.quaternion:
      pt++;
      type2 = optParameterAsString(pt).toLowerCase();
      if (Parser.isOneOf(type2, "w;x;y;z"))
        pt++;
      else
        type2 = "w";
      type2 = "quaternion " + type2;
      type = optParameterAsString(pt);
      if (type.indexOf("deriv") == 0) {
        type2 += " derivative";
        pt++;
      }
      type = "QUAT";
      break;
    case Token.ramachandran:
      type = "RAMA";
      type2 = "ramachandran";
      pt++;
      break;
    case Token.function:
      type = "FUNCS";
      pt++;
      break;
    case Token.coord:
    case Token.data:
      type = optParameterAsString(pt + 1).toLowerCase();
      type = "data";
      isCoord = true;
      pt++;
      break;
    case Token.state:
    case Token.script:
      pt++;
      break;
    case Token.mo:
      type = "MO";
      pt++;
      break;
    case Token.isosurface:
      type = "ISO";
      pt++;
      break;
    case Token.history:
      type = "HIS";
      pt++;
      break;
    case Token.var:
      pt += 2;
      type = "VAR";
      break;
    case Token.file:
      type = "FILE";
      pt++;
      break;
    case Token.identifier:
      type = parameterAsString(1).toLowerCase();
      if (type.equals("image")) {
        pt++;
        if (tokAt(pt) == Token.integer) {
          width = intParameter(pt++);
          height = intParameter(pt++);
        }
      } else if (Parser.isOneOf(type, driverList.toLowerCase())) {
        pt++;
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        isExport = true;
        fileName = "Jmol." + type;
        if (tokAt(pt) == Token.integer) {
          width = intParameter(pt++);
          height = intParameter(pt++);
        }
      } else {
        type = "(image)";
      }
      break;
    }
    String val = optParameterAsString(pt);
    if (val.equalsIgnoreCase("clipboard")) {
      if (isSyntaxCheck)
        return;
      //      if (isApplet)
      //      evalError(GT._("The {0} command is not available for the applet.",
      //        "WRITE CLIPBOARD"));
    } else if (Parser.isOneOf(val.toLowerCase(), "png;jpg;jpeg;jpg64")
        && tokAt(pt + 1) == Token.integer) {
      quality = intParameter(++pt);
    } else if (Parser.isOneOf(val.toLowerCase(), "xyz;mol;pdb")) {
      type = val.toUpperCase();
      if (pt + 1 == statementLength)
        pt++;
    }

    //write [image|history|state] clipboard

    //write [optional image|history|state] [JPG quality|JPEG  quality|JPG64 quality|PNG|PPM|SPT] "filename"
    //write script "filename"
    //write isosurface t.jvxl 

    if (type.equals("(image)")
        && Parser.isOneOf(val.toUpperCase(), "JPG;JPG64;JPEG;JPEG64;PNG;PPM")) {
      type = val.toUpperCase();
      pt++;
    }

    if (pt + 2 == statementLength) {
      data = parameterAsString(++pt);
      if (data.charAt(0) != '.')
        type = val.toUpperCase();
    }
    switch (tokAt(pt)) {
    case Token.nada:
      isShow = true;
      break;
    case Token.identifier:
    case Token.string:
      fileName = parameterAsString(pt);
      if (pt == statementLength - 3 && tokAt(pt + 1) == Token.dot) {
        //write filename.xxx  gets separated as filename .spt
        //write isosurface filename.xxx also 
        fileName += "." + parameterAsString(pt + 2);
      }
      if (type != "VAR" && pt == 1)
        type = "image";
      else if (fileName.charAt(0) == '.' && (pt == 2 || pt == 3)) {
        fileName = parameterAsString(pt - 1) + fileName;
        if (type != "VAR" && pt == 2)
          type = "image";
      }
      if (fileName.equalsIgnoreCase("clipboard"))
        fileName = null;
      break;
    case Token.clipboard:
      break;
    default:
      invalidArgument();
    }
    if (type.equals("image")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "JPG";
    }
    if (type.equals("data")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "XYZ";
    }
    boolean isImage = Parser.isOneOf(type, "JPEG;JPG64;JPG;PPM;PNG");
    if (isImage && (isApplet || isShow))
      type = "JPG64";
    if (!isImage
        && !isExport
        && !Parser.isOneOf(type,
            "SPT;HIS;MO;ISO;VAR;FILE;XYZ;MOL;PDB;QUAT;RAMA;FUNCS;"))
      evalError(GT
          ._(
              "write what? {0} or {1} \"filename\"",
              new Object[] {
                  "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|ISOSURFACE|MO|QUATERNION [w,x,y,z] [derivative]"
                      + "|RAMACHANDRAN|STATE|VAR x  CLIPBOARD",
                  "JPG|JPG64|PNG|PPM|SPT|JVXL|XYZ|MOL|PDB|"
                      + driverList.toUpperCase().replace(';', '|') }));
    if (isSyntaxCheck)
      return;
    data = type.intern();
    Object bytes = null;
    if (isExport) {
      //POV-Ray uses a BufferedWriter instead of a StringBuffer.
      boolean isPovRay = type.equals("Povray");
      data = viewer.generateOutput(data, isPovRay ? fileName : null, width,
          height);
      if (isPovRay) {
        data = TextFormat.simpleReplace(data, "%FILETYPE%", "N");
        data = TextFormat.simpleReplace(data, "%OUTPUTFILENAME%", fileName
            + ".png");
        viewer.createImage(fileName + ".ini", data, Integer.MIN_VALUE, 0, 0);
        scriptStatus("Created " + fileName + ".ini:\n\n" + data);
        return;
      }
    } else if (data == "PDB" || data == "XYZ" || data == "MOL") {
      data = viewer.getData("selected", data);
    } else if (data == "QUAT" || data == "RAMA") {
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        multipleModelsNotOK("write " + type2);
      data = viewer.getPdbData(modelIndex, type2);
    } else if (data == "FUNCS") {
      data = getFunctionCalls("");
    } else if (data == "FILE") {
      if (isShow)
        data = viewer.getCurrentFileAsString();
      else
        bytes = viewer.getCurrentFileAsBytes();
      quality = Integer.MIN_VALUE;
    } else if (data == "VAR") {
      data = "" + getParameter(parameterAsString(2), false);
    } else if (data == "SPT") {
      if (isCoord) {
        BitSet tainted = viewer.getTaintedAtoms(AtomCollection.TAINT_COORD);
        viewer.setAtomCoordRelative(new Point3f(0, 0, 0));
        data = (String) viewer.getProperty("string", "stateInfo", null);
        viewer.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
      } else {
        data = (String) viewer.getProperty("string", "stateInfo", null);
      }
    } else if (data == "HIS") {
      data = viewer.getSetHistory(Integer.MAX_VALUE);
    } else if (data == "MO") {
      data = getMoJvxl(Integer.MAX_VALUE);
    } else if (data == "ISO") {
      if ((data = getIsosurfaceJvxl()) == null)
        evalError(GT._("No data available"));
    } else {
      len = -1;
      if (data == "PNG") {
        if (quality == Integer.MIN_VALUE)
          quality = 2;
        else if (quality < 0 || quality > 9)
          quality = 0;
      } else if (quality <= 0)
        quality = 75; //JPG
    }
    if (data == null)
      data = "";
    if (len == 0)
      len = (bytes == null ? data.length() : bytes instanceof String 
          ? ((String)bytes).length() : ((byte[]) bytes).length);
    if (isImage) {
      refresh();
      if (width < 0)
        width = viewer.getScreenWidth();
      if (height < 0)
        height = viewer.getScreenHeight();
    }
    if (isShow) {
      showString(data);
    } else if (bytes != null && bytes instanceof String) {
      scriptStatus((String) bytes);
    } else {
      if (bytes == null)
        bytes = data;
      viewer.createImage(fileName, bytes, quality, width, height);
      scriptStatus("type=" + type + "; file="
          + (fileName == null ? "CLIPBOARD" : fileName)
          + (len >= 0 ? "; length=" + len : "")
          + (isImage ? "; width=" + width + "; height=" + height : "")
          + (quality >= 0 ? "; quality=" + quality : ""));
    }
  }

  private void print() throws ScriptException {
    if (statementLength == 1)
      badArgumentCount();
    String s = (String) parameterExpression(1, 0, "", false);
    if (!isSyntaxCheck)
      showString(s);
  }

  private void returnCmd() throws ScriptException {
    Token t = getContextVariableAsToken("_retval");
    if (t == null) {
      if (!isSyntaxCheck)
        interruptExecution = Boolean.TRUE;
      return;
    }
    Object v = (statementLength == 1 ? null : parameterExpression(1, 0, null, true));
    if (isSyntaxCheck)
      return;
    Token tv = (v == null ? Token.intToken(0) : (Token) ((Vector) v).get(0));
    t.value = tv.value;
    t.intValue = tv.intValue;
    t.tok = tv.tok;
    pcEnd = pc;
  }

  /* ****************************************************************************
   * ==============================================================
   * SHOW 
   * ==============================================================
   */

  private void show() throws ScriptException {
    String value = null;
    String str = parameterAsString(1);
    String msg = null;
    int len = 2;
    if (statementLength == 2 && str.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(str.substring(0, str.indexOf("?"))));
      return;
    }
    switch (getToken(1).tok) {
    case Token.function:
      checkLength23();
      if (!isSyntaxCheck)
        showString(getFunctionCalls(optParameterAsString(2)));
      return;
    case Token.set:
      checkLength2();
      if (!isSyntaxCheck)
        showString(viewer.getAllSettings(null));
      return;
    case Token.url:
      // in a new window
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          viewer.showUrl(getFullPathName());
        return;
      }
      String fileName = parameterAsString(2);
      if (!isSyntaxCheck)
        viewer.showUrl(fileName);
      return;
    case Token.color:
    case Token.defaultColors:
      str = "defaultColorScheme";
      break;
    case Token.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case Token.quaternion:
    case Token.ramachandran:
      if (isSyntaxCheck)
        return;
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        multipleModelsNotOK("show " + theToken.value);
      msg = viewer.getPdbData(modelIndex,
          theTok == Token.quaternion ? "quaternion w" : "ramachandran");
      break;
    case Token.identifier:
      if (str.equalsIgnoreCase("historyLevel")) {
        value = "" + commandHistoryLevelMax;
      } else if (str.equalsIgnoreCase("defaultLattice")) {
        value = Escape.escape(viewer.getDefaultLattice());
      } else if (str.equalsIgnoreCase("logLevel")) {
        value = "" + Viewer.getLogLevel();
      } else if (str.equalsIgnoreCase("fileHeader")) {
        if (!isSyntaxCheck)
          msg = viewer.getPDBHeader();
      } else if (str.equalsIgnoreCase("debugScript")) {
        value = "" + viewer.getDebugScript();
      } else if (str.equalsIgnoreCase("colorScheme")) {
        String name = optParameterAsString(2);
        if (name.length() > 0)
          len = 3;
        if (!isSyntaxCheck)
          value = viewer.getColorSchemeList(name, true);
      } else if (str.equalsIgnoreCase("menu")) {
        if (!isSyntaxCheck)
          value = viewer.getMenu();
      } else if (str.equalsIgnoreCase("strandCount")) {
        msg = "set strandCountForStrands " + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS) 
          + "; set strandCountForMeshRibbon " + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      } else if (str.equalsIgnoreCase("trajectory") || str.equalsIgnoreCase("trajectories")) {
        msg = viewer.getSelectedTrajectories();
      }
      break;
    case Token.axes:
      switch (viewer.getAxesMode()) {
      case JmolConstants.AXES_MODE_UNITCELL:
        msg = "set axesUnitcell";
        break;
      case JmolConstants.AXES_MODE_BOUNDBOX:
        msg = "set axesWindow";
        break;
      default:
        msg = "set axesMolecular";
      }
      break;
    case Token.bondmode:
      msg = "set bondMode " + (viewer.getBondSelectionModeOr() ? "OR" : "AND");
      break;
    case Token.strands:
      msg = "set strandCountForStrands " + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS) 
        + "; set strandCountForMeshRibbon " + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      break;
    case Token.hbond:
      msg = "set hbondsBackbone " + viewer.getHbondsBackbone()
          + ";set hbondsSolid " + viewer.getHbondsSolid();
      break;
    case Token.spin:
      msg = viewer.getSpinState();
      break;
    case Token.ssbond:
      msg = "set ssbondsBackbone " + viewer.getSsbondsBackbone();
      break;
    case Token.display://deprecated
    case Token.selectionHalo:
      msg = "selectionHalos " + (viewer.getSelectionHaloEnabled() ? "ON" : "OFF");
      break;
    case Token.hetero:
      msg = "set selectHetero " + viewer.getRasmolHeteroSetting();
      break;
    case Token.hydrogen:
      msg = "set selectHydrogens " + viewer.getRasmolHydrogenSetting();
      break;
    case Token.ambient:
    case Token.diffuse:
    case Token.specular:
    case Token.specpower:
    case Token.specexponent:
      msg = viewer.getSpecularState();
      break;
    case Token.save:
      if (!isSyntaxCheck)
        msg = viewer.listSavedStates();
      break;
    case Token.unitcell:
      if (!isSyntaxCheck)
        msg = viewer.getUnitCellInfoText();
      break;
    case Token.state:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getStateInfo();
        break;
      }
      String name = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedState(name);
      break;
    case Token.structure:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getStructureState();
        break;
      }
      String shape = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedStructure(shape);
      break;
    case Token.data:
      String type = ((len = statementLength) == 3 ? parameterAsString(2) : null);
      if (!isSyntaxCheck) {
        Object[] data = (type == null ? this.data : viewer.getData(type));
        msg = (data == null ? "no data" : "data \""
            + data[0]
            + "\"\n"
            + (data[1] instanceof float[] ? Escape.escape((float[]) data[1])
                : "" + data[1]));
      }
      break;
    case Token.spacegroup:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getSpaceGroupInfoText(null);
        break;
      }
      String sg = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSpaceGroupInfoText(TextFormat.simpleReplace(sg, "''",
            "\""));
      break;
    case Token.dollarsign:
      len = 3;
      int shapeType = setShapeByNameParameter(2);
      if (!isSyntaxCheck) {
        if (shapeType == JmolConstants.SHAPE_ISOSURFACE)
          msg = getIsosurfaceJvxl();
        else
          msg = (String) viewer.getShapeProperty(shapeType, "command");
      }
      break;
    case Token.boundbox:
      if (!isSyntaxCheck) {
        msg = viewer.getBoundBoxCommand(true);
      }
      break;
    case Token.center:
      if (!isSyntaxCheck)
        msg = "center " + Escape.escape(viewer.getRotationCenter());
      break;
    case Token.draw:
      if (!isSyntaxCheck)
        msg = (String) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
            "command");
      break;
    case Token.file:
      // as as string
      if (statementLength == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getCurrentFileAsString();
        break;
      }
      checkLength3();
      value = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getFileAsString(value);
      break;
    case Token.frame:
      if (tokAt(2) == Token.all && (len = 3) > 0)
        msg = viewer.getModelFileInfoAll();
      else
        msg = viewer.getModelFileInfo();
      break;
    case Token.history:
      int n = ((len = statementLength) == 2 ? Integer.MAX_VALUE
          : intParameter(2));
      if (n < 1)
        invalidArgument();
      if (!isSyntaxCheck) {
        viewer.removeCommand();
        msg = viewer.getSetHistory(n);
      }
      break;
    case Token.isosurface:
      if (!isSyntaxCheck)
        msg = (String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
            "jvxlFileData");
      break;
    case Token.mo:
      int ptMO = ((len = statementLength) == 2 ? Integer.MAX_VALUE
          : intParameter(2));
      if (!isSyntaxCheck)
        msg = getMoJvxl(ptMO);
      break;
    case Token.model:
      if (!isSyntaxCheck)
        msg = viewer.getModelInfoAsString();
      break;
    case Token.monitor:
      if (!isSyntaxCheck)
        msg = viewer.getMeasurementInfoAsString();
      break;
    case Token.orientation:
      if (!isSyntaxCheck)
        msg = viewer.getOrientationText();
      break;
    case Token.pdbheader:
      if (!isSyntaxCheck)
        msg = viewer.getPDBHeader();
      break;
    case Token.symmetry:
      if (!isSyntaxCheck)
        msg = viewer.getSymmetryInfoAsString();
      break;
    case Token.transform:
      if (!isSyntaxCheck)
        msg = "transform:\n" + viewer.getTransformText();
      break;
    case Token.zoom:
      msg = "zoom "
          + (viewer.getZoomEnabled() ? ("" + viewer.getZoomPercentFloat())
              : "off");
      break;
    case Token.frank:
      msg = (viewer.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case Token.radius:
      str = "solventProbeRadius";
      break;
    // not implemented
    case Token.translation:
    case Token.rotation:
      unrecognizedShowParameter("show ORIENTATION");
    case Token.chain:
    case Token.group:
    case Token.sequence:
    case Token.residue:
      unrecognizedShowParameter("getProperty CHAININFO (atom expression)");
    case Token.selected:
      unrecognizedShowParameter("getProperty ATOMINFO (selected)");
    case Token.atoms:
      unrecognizedShowParameter("getProperty ATOMINFO (atom expression)");
    case Token.echo:
    case Token.fontsize:
    case Token.property: // huh? why?
    case Token.bonds:
    case Token.help:
    case Token.solvent:
      value = "?";
      break;
    }
    checkStatementLength(len);
    if (isSyntaxCheck)
      return;
    if (msg != null)
      showString(msg);
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null)
      showString(str + " = " + getParameterEscaped(str));
  }

  private void showString(String str) {
    if (isSyntaxCheck)
      return;
    if (outputBuffer != null)
      outputBuffer.append(str).append('\n');
    else
      viewer.showString(str);
  }

  private String getFunctionCalls(String selectedFunction) {
    StringBuffer s = new StringBuffer();
    int pt = selectedFunction.indexOf("*");
    boolean isGeneric = (pt >= 0);
    boolean isLocal = (selectedFunction.indexOf("_") == 0);
    if (isGeneric)
      selectedFunction = selectedFunction.substring(0, pt);
    selectedFunction = selectedFunction.toLowerCase();
    Hashtable ht = (isLocal ? compiler.localFunctions
        : Compiler.globalFunctions);
    String[] names = new String[ht.size()];
    Enumeration e = ht.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (selectedFunction.length() == 0
          || name.equalsIgnoreCase(selectedFunction) || isGeneric
          && name.toLowerCase().indexOf(selectedFunction) == 0)
        names[n++] = name;
    }
    Arrays.sort(names, 0, n);
    for (int i = 0; i < n; i++)
      s.append(((Function) ht.get(names[i])).toString());
    return s.toString();
  }

  private String getIsosurfaceJvxl() {
    if (isSyntaxCheck)
      return "";
    return (String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
        "jvxlFileData");
  }

  private String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    viewer.loadShape(JmolConstants.SHAPE_MO);
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      multipleModelsNotOK("MO isosurfaces");
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "moData");
    if (moData == null)
      evalError(GT._("no MO basis/coefficient data available for this frame"));
    setShapeProperty(JmolConstants.SHAPE_MO, "moData", moData);
    return (String) viewer.getShapeProperty(JmolConstants.SHAPE_MO, "showMO",
        ptMO);
  }

  /* ****************************************************************************
   * ============================================================== 
   * MESH implementations
   * ==============================================================
   */

  private void pmesh() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_PMESH);
    if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_PMESH))
      return;
    Object t;
    boolean idSeen = false;
    String translucency = null;
    initIsosurface(JmolConstants.SHAPE_PMESH);
    for (int i = iToken; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("FIXED")) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("MODELBASED")) {
          propertyName = "fixed";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (idSeen)
          invalidArgument();
        propertyName = "thisID";
        propertyValue = str;
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(statement[++i]);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.color:
        translucency = setColorOptions(i + 1, JmolConstants.SHAPE_PMESH, -1);
        i = iToken;
        idSeen = true;
        continue;
      case Token.string:
        String filename = stringParameter(i);
        propertyName = "bufferedReader";
        if (filename.equalsIgnoreCase("inline")) {
          if (i + 1 < statementLength && tokAt(i + 1) == Token.string) {
            String data = parameterAsString(++i);
            if (data.indexOf("|") < 0 && data.indexOf("\n") < 0) {
              // space separates -- so set isOnePerLine
              data = data.replace(' ', '\n');
              propertyName = "bufferedReaderOnePerLine";
            }
            data = data.replace('{', ' ').replace(',', ' ').replace('}', ' ')
                .replace('|', '\n');
            data = TextFormat.simpleReplace(data, "\n\n", "\n");
            if (logMessages)
              Logger.debug("pmesh inline data:\n" + data);
            t = viewer.getBufferedReaderForString(data);
          } else {
            stringOrIdentifierExpected();
            break;
          }
        } else {
          if (isSyntaxCheck)
            return;
          if (thisCommand.indexOf("# FILE0=") >= 0)
            filename = extractCommandOption("FILE0"); 
          String[] fullPathNameReturn = new String[1];
          t = viewer.getBufferedReaderOrErrorMessageFromName(filename, fullPathNameReturn);
          if (t instanceof String)
            fileNotFoundException(filename + ":" + t);
          setShapeProperty(JmolConstants.SHAPE_PMESH, "commandOption",
              "FILE0=" + Escape.escape(fullPathNameReturn[0]));
          Logger.info("reading pmesh data from " + fullPathNameReturn[0]);
        }
        propertyValue = t;
        break;
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_PMESH, i, theTok))
          invalidArgument();
        i = iToken;
      }
      idSeen = (theTok != Token.delete);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_PMESH, propertyName, propertyValue);
    }
    if (!isSyntaxCheck) {
      String pmeshError = (String) viewer.getShapeProperty(
          JmolConstants.SHAPE_PMESH, "pmeshError");
      if (pmeshError != null)
        evalError(pmeshError);
    }
    if (translucency != null)
      setShapeProperty(JmolConstants.SHAPE_PMESH, "translucency", translucency);
  }
  
  private String extractCommandOption(String name) {
    int i = thisCommand.indexOf(name + "=");
    return (i < 0 ? name : Parser.getNextQuotedString(thisCommand, i));
  }

  private void draw() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_DRAW);
    if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_DRAW))
      return;
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isTranslucent = false;
    float translucentLevel = Float.MAX_VALUE;
    int colorArgb = Integer.MIN_VALUE;
    int intScale = 0;
    boolean idSeen = false;
    int iptDisplayProperty = 0;
    initIsosurface(JmolConstants.SHAPE_DRAW);
    for (int i = iToken; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case Token.length:
        propertyValue = new Float(floatParameter(++i));
        propertyName = "length";
        break;
      case Token.decimal:
        // $drawObject
        propertyValue = new Float(floatParameter(i));
        propertyName = "length";
        break;
      case Token.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = new Integer(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case Token.rightsquare:
      case Token.leftsquare:
        if ((isSavedState = !isSavedState) == (theTok == Token.rightsquare))
          invalidArgument();
        break;
      case Token.plane:
        propertyName = "plane";
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("FIXED")) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("MODELBASED")) {
          propertyName = "fixed";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("CROSSED")) {
          propertyName = "crossed";
          break;
        }
        if (str.equalsIgnoreCase("CURVE")) {
          propertyName = "curve";
          break;
        }
        if (str.equalsIgnoreCase("ARROW")) {
          propertyName = "arrow";
          break;
        }
        if (str.equalsIgnoreCase("CIRCLE")) { //circle <center>; not yet implemented
          propertyName = "circle";
          break;
        }
        if (str.equalsIgnoreCase("VERTICES")) {
          propertyName = "vertices";
          break;
        }
        if (str.equalsIgnoreCase("REVERSE")) {
          propertyName = "reverse";
          break;
        }
        if (str.equalsIgnoreCase("ROTATE45")) {
          propertyName = "rotate45";
          break;
        }
        if (str.equalsIgnoreCase("PERP")
            || str.equalsIgnoreCase("PERPENDICULAR")) {
          propertyName = "perp";
          break;
        }
        if (str.equalsIgnoreCase("OFFSET")) {
          Point3f pt = getPoint3f(++i, true);
          i = iToken;
          propertyName = "offset";
          propertyValue = pt;
          break;
        }
        if (str.equalsIgnoreCase("SCALE")) {
          if (++i >= statementLength)
            numberExpected();
          switch (getToken(i).tok) {
          case Token.integer:
            intScale = intParameter(i);
            continue;
          case Token.decimal:
            intScale = (int) (floatParameter(i) * 100);
            continue;
          default:
            numberExpected();
          }
        }
        if (str.equalsIgnoreCase("DIAMETER")) { //pixels
          propertyValue = new Float(floatParameter(++i));
          propertyName = (tokAt(i) == Token.decimal ? "width" : "diameter");
          break;
        }
        if (str.equalsIgnoreCase("WIDTH")) { //angstroms
          propertyValue = new Float(floatParameter(++i));
          propertyName = "width";
          break;
        }
        if (idSeen)
          invalidArgument();
        propertyName = "thisID";
        propertyValue = str;
        break;
      case Token.dollarsign:
        // $drawObject[m]
        if (tokAt(i + 2) == Token.leftsquare) {
          Point3f pt = centerParameter(i);
          i = iToken;
          propertyName = "coord";
          propertyValue = pt;
          havePoints = true;
          break;
        }
        // $drawObject

        propertyValue = objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case Token.color:
        i++;
        //fall through
      case Token.translucent:
      case Token.opaque:
        isTranslucent = false;
        boolean isColor = false;
        if (tokAt(i) == Token.translucent) {
          isTranslucent = true;
          if (isFloatParameter(++i))
            translucentLevel = floatParameter(i++);
          isColor = true;
        } else if (tokAt(i) == Token.opaque) {
          ++i;
          isColor = true;
        }
        if (isColorParam(i)) {
          colorArgb = getArgbParam(i);
          i = iToken;
          isColor = true;
        }
        if (!isColor)
          invalidArgument();
        idSeen = true;
        continue;
      case Token.leftbrace:
      case Token.point3f:
        // {X, Y, Z}
        Point3f pt = getPoint3f(i, true);
        i = iToken;
        propertyName = "coord";
        propertyValue = pt;
        havePoints = true;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        propertyName = "atomSet";
        propertyValue = expression(i);
        i = iToken;
        havePoints = true;
        break;
      case Token.list:
        propertyName = "modelBasedPoints";
        propertyValue = theToken.value;
        havePoints = true;
        break;
      default:
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, 0, theTok))
          invalidArgument();
        continue;
      }
      idSeen = (theTok != Token.delete);
      if (havePoints && !isInitialized) {
        setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(
            intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DRAW, propertyName, propertyValue);
    }
    if (havePoints) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
    }
    if (colorArgb != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_DRAW, "color", new Integer(
          colorArgb));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_DRAW, "", "translucent",
          translucentLevel);
    if (intScale != 0) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "scale", new Integer(intScale));
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, iptDisplayProperty, 
          getToken(iptDisplayProperty).tok))
        invalidArgument();
    }
  }

  /*
   private void drawPoint(String key, Point3f pt, boolean isOn) throws ScriptException {
   viewer.loadShape(JmolConstants.SHAPE_DRAW);
   setShapeProperty(JmolConstants.SHAPE_DRAW, "init", null);
   setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", key);
   setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(0));
   setShapeProperty(JmolConstants.SHAPE_DRAW, "coord", pt);
   setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
   if (!isOn)
   setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, -1, Token.off);
   }

   private void drawPlane(String key, Point4f plane, boolean isOn) throws ScriptException {
   viewer.loadShape(JmolConstants.SHAPE_ISOSURFACE);
   setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "init", null);
   setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID", key);
   setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "plane", plane);
   setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "nomap", new Float(0));
   if (!isOn)
   setMeshDisplayProperty(JmolConstants.SHAPE_ISOSURFACE, -1, Token.off);
   }
   */

  private void polyhedra() throws ScriptException {
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets] 
     *   [optional type and/or edge] [optional design parameters]
     *   
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     * 
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int nAtomSets = 0;
    viewer.loadShape(JmolConstants.SHAPE_POLYHEDRA);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "init", null);
    String setPropertyName = "centers";
    String decimalPropertyName = "radius_";
    boolean isTranslucent = false;
    float translucentLevel = Float.MAX_VALUE;
    int color = Integer.MIN_VALUE;
    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        color = getArgbParam(i);
        i = iToken;
        continue;
      }
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.opEQ:
      case Token.comma:
        continue;
      case Token.bonds:
        if (nAtomSets > 0)
          invalidParameterOrder();
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case Token.radius:
        decimalPropertyName = "radius";
        continue;
        
      case Token.color:
        i++;
        //fall through
      case Token.translucent:
      case Token.opaque:
        isTranslucent = false;
        boolean isColor = false;
        if (tokAt(i) == Token.translucent) {
          isTranslucent = true;
          if (isFloatParameter(++i))
            translucentLevel = floatParameter(i++);
          isColor = true;
        } else if (tokAt(i) == Token.opaque) {
          ++i;
          isColor = true;
        }
        if (isColorParam(i)) {
          color = getArgbParam(i);
          i = iToken;
          isColor = true;
        }
        if (!isColor)
          invalidArgument();
        continue;
      case Token.identifier:
        String str = parameterAsString(i);
        if ("collapsed".equalsIgnoreCase(str)) {
          propertyName = "collapsed";
          propertyValue = Boolean.TRUE;
          if (typeSeen)
            incompatibleArguments();
          typeSeen = true;
          break;
        }
        if ("flat".equalsIgnoreCase(str)) {
          propertyName = "collapsed";
          propertyValue = Boolean.FALSE;
          if (typeSeen)
            incompatibleArguments();
          typeSeen = true;
          break;
        }
        if ("edges".equalsIgnoreCase(str) || "noedges".equalsIgnoreCase(str)
            || "frontedges".equalsIgnoreCase(str)) {
          if (edgeParameterSeen)
            incompatibleArguments();
          propertyName = str;
          edgeParameterSeen = true;
          break;
        }
        if (!needsGenerating)
          insufficientArguments();
        if ("to".equalsIgnoreCase(str)) {
          if (nAtomSets > 1)
            invalidParameterOrder();
          if (getToken(i + 1).tok == Token.bitset) {
            propertyName = "toBitSet";
            propertyValue = getToken(++i).value;
            needsGenerating = true;
            break;
          }
          setPropertyName = "to";
          continue;
        }
        if ("faceCenterOffset".equalsIgnoreCase(str)) {
          decimalPropertyName = "faceCenterOffset";
          isDesignParameter = true;
          continue;
        }
        if ("distanceFactor".equalsIgnoreCase(str)) {
          decimalPropertyName = "distanceFactor";
          isDesignParameter = true;
          continue;
        }
        invalidArgument();
      case Token.integer:
        if (nAtomSets > 0 && !isDesignParameter)
          invalidParameterOrder();
        // no reason not to allow integers when explicit
        if (decimalPropertyName == "radius_") {
          propertyName = "nVertices";
          propertyValue = new Integer(intParameter(i));
          needsGenerating = true;
          break;
        }
      case Token.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          invalidParameterOrder();
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = new Float(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case Token.delete:
      case Token.on:
      case Token.off:
        if (i + 1 != statementLength || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && setPropertyName == "to")
          incompatibleArguments();
        propertyName = parameterAsString(i);
        onOffDelete = true;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (typeSeen)
          invalidParameterOrder();
        if (++nAtomSets > 2)
          badArgumentCount();
        if (setPropertyName == "to")
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = expression(i);
        i = iToken;
        break;
      default:
        invalidArgument();
      }
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen)
      insufficientArguments();
    if (needsGenerating)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "generate", null);
    if (color != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "colorThis", new Integer(
          color));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_POLYHEDRA, "", "translucent",
          translucentLevel);
  }

  private void lcaoCartoon() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LCAOCARTOON);
    if (tokAt(1) == Token.list
        && listIsosurface(JmolConstants.SHAPE_LCAOCARTOON))
      return;
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "init", null);
    if (statementLength == 1) {
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "lcaoID", null);
      return;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < statementLength; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.center:
        //serialized lcaoCartoon in isosurface format
        isosurface(JmolConstants.SHAPE_LCAOCARTOON);
        return;
      case Token.rotate:
        Vector3f rotAxis = new Vector3f();
        switch (getToken(++i).tok) {
        case Token.identifier:
          String str = parameterAsString(i);
          float radians = floatParameter(++i)
              * TransformManager.radiansPerDegree;
          if (str.equalsIgnoreCase("x")) {
            rotAxis.set(radians, 0, 0);
            break;
          }
          if (str.equalsIgnoreCase("y")) {
            rotAxis.set(0, radians, 0);
            break;
          }
          if (str.equalsIgnoreCase("z")) {
            rotAxis.set(0, 0, radians);
            break;
          }
          invalidArgument();
        default:
          invalidArgument();
        }
        propertyName = "rotationAxis";
        propertyValue = rotAxis;
        break;
      case Token.on:
        propertyName = "on";
        break;
      case Token.off:
        propertyName = "off";
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      case Token.integer:
      case Token.decimal:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.bitset:
      case Token.expressionBegin:
        propertyName = "select";
        propertyValue = expression(i);
        i = iToken;
        break;
      case Token.color:
        translucency = setColorOptions(i + 1, JmolConstants.SHAPE_LCAOCARTOON,
            -2);
        if (translucency != null)
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = iToken;
        idSeen = true;
        continue;
      case Token.translucent:
      case Token.opaque:
        setMeshDisplayProperty(JmolConstants.SHAPE_LCAOCARTOON, i, theTok);
        i = iToken;
        idSeen = true;
        continue;
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "create";
        if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case Token.select:
        if (tokAt(i + 1) == Token.bitset
            || tokAt(i + 1) == Token.expressionBegin) {
          propertyName = "select";
          propertyValue = expression(i + 1);
          i = iToken;
        } else {
          propertyName = "selectType";
          propertyValue = parameterAsString(++i);
        }
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("SCALE")) {
          propertyName = "scale";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("MOLECULAR")) {
          propertyName = "molecular";
          break;
        }
        if (str.equalsIgnoreCase("CREATE")) {
          propertyValue = parameterAsString(++i);
          propertyName = "create";
          if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
            i++;
            propertyName = "molecular";
          }
          break;
        }
        propertyValue = str;
      //fall through for identifiers
      case Token.all:
        if (idSeen)
          invalidArgument();
        propertyName = "lcaoID";
        break;
      }
      if (theTok != Token.delete)
        idSeen = true;
      if (propertyName == null)
        invalidArgument();
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, propertyName,
          propertyValue);
    }
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "clear", null);
  }

  private int lastMoNumber = 0;

  private boolean mo(boolean isInitOnly) throws ScriptException {
    int modelIndex = viewer.getDisplayModelIndex();
    int offset = Integer.MAX_VALUE;
    if (!isSyntaxCheck && modelIndex < 0)
      multipleModelsNotOK("MO isosurfaces");
    viewer.loadShape(JmolConstants.SHAPE_MO);
    if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_MO))
      return true;
    setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
    String title = null;
    int moNumber = ((Integer) viewer.getShapeProperty(JmolConstants.SHAPE_MO,
        "moNumber")).intValue();
    if (isInitOnly)
      return true;//(moNumber != 0);
    if (moNumber == 0 && !isSyntaxCheck) {
      lastMoNumber = 0;
      moNumber = Integer.MAX_VALUE;
    }
    String str;
    String propertyName = null;
    Object propertyValue = null;
    switch (getToken(1).tok) {
    case Token.integer:
      moNumber = intParameter(1);
      break;
    case Token.next:
      moNumber = lastMoNumber + 1;
      break;
    case Token.prev:
      moNumber = lastMoNumber - 1;
      break;
    case Token.color:
      setColorOptions(2, JmolConstants.SHAPE_MO, 2);
      break;
    case Token.plane:
      // plane {X, Y, Z, W}
      propertyName = "plane";
      propertyValue = planeParameter(2);
      break;
    case Token.identifier:
      str = parameterAsString(1);
      if ((offset = moOffset(1)) != Integer.MAX_VALUE) {
        moNumber = 0;
        break;
      }
      if (str.equalsIgnoreCase("CUTOFF")) {
        if (tokAt(2) == Token.plus) {
          propertyName = "cutoffPositive";
          propertyValue = new Float(floatParameter(3));
        } else {
          propertyName = "cutoff";
          propertyValue = new Float(floatParameter(2));
        }
        break;
      }
      if (str.equalsIgnoreCase("RESOLUTION")
          || str.equalsIgnoreCase("POINTSPERANGSTROM")) {
        propertyName = "resolution";
        propertyValue = new Float(floatParameter(2));
        break;
      }
      if (str.equalsIgnoreCase("SCALE")) {
        propertyName = "scale";
        propertyValue = new Float(floatParameter(2));
        break;
      }
      if (str.equalsIgnoreCase("SQUARED")) {
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      }
      if (str.equalsIgnoreCase("TITLEFORMAT")) {
        if (2 < statementLength && tokAt(2) == Token.string) {
          propertyName = "titleFormat";
          propertyValue = parameterAsString(2);
        }
        break;
      }
      if (str.equalsIgnoreCase("DEBUG")) {
        propertyName = "debug";
        break;
      }
      if (str.equalsIgnoreCase("noplane")) {
        propertyName = "plane";
        break;
      }
      invalidArgument();
    default:
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, 1, theTok))
        invalidArgument();
      return true;
    }
    if (propertyName != null)
      setShapeProperty(JmolConstants.SHAPE_MO, propertyName, propertyValue);
    if (moNumber != Integer.MAX_VALUE) {
      if (tokAt(2) == Token.string)
        title = parameterAsString(2);
      if (!isSyntaxCheck)
        viewer.setCursor(Viewer.CURSOR_WAIT);
      setMoData(JmolConstants.SHAPE_MO, moNumber, offset, modelIndex, title);
    }
    return true;
  }

  private String setColorOptions(int index, int iShape, int nAllowed)
      throws ScriptException {
    getToken(index);
    String translucency = "opaque";
    if (theTok == Token.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        setShapeTranslucency(iShape, null, "translucent", value);
      } else {
        setMeshDisplayProperty(iShape, index, theTok);
      }
    } else if (theTok == Token.opaque) {
      if (nAllowed >= 0)
        setMeshDisplayProperty(iShape, index, theTok);
    } else {
      iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (isColorParam(iToken + 1)) {
        setShapeProperty(iShape, "colorRGB",
            new Integer(getArgbParam(++iToken)));
      } else if (iToken < index) {
        invalidArgument();
      } else {
        break;
      }
    }
    return translucency;
  }

  private int moOffset(int index) throws ScriptException {
    String str = parameterAsString(index++);
    boolean isHomo = false;
    int offset = Integer.MAX_VALUE;
    if ((isHomo = str.equalsIgnoreCase("HOMO")) || str.equalsIgnoreCase("LUMO")) {
      offset = (isHomo ? 0 : 1);
      if (tokAt(index) == Token.integer && intParameter(index) < 0)
        offset += intParameter(index);
      else if (tokAt(index) == Token.plus)
        offset += intParameter(index + 1);
      else if (tokAt(index) == Token.minus)
        offset -= intParameter(index + 1);
    }
    return offset;
  }

  private void setMoData(int shape, int moNumber, int offset, int modelIndex,
                         String title) throws ScriptException {
    if (isSyntaxCheck)
      return;
    if (modelIndex == 0)
      modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      multipleModelsNotOK("MO isosurfaces");
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "jmolSurfaceInfo");
    if (moData != null && ((String) moData.get("surfaceDataType")).equals("mo")) {
      //viewer.loadShape(shape);
      //setShapeProperty(shape, "init", new Integer(modelIndex));
    } else {
      moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "moData");
      if (moData == null)
        evalError(GT._("no MO basis/coefficient data available for this frame"));
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        evalError(GT._("no MO coefficient data available"));
      if (nOrb == 1 && moNumber > 1)
        evalError(GT._("Only one molecular orbital is available in this file"));
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          lastMoNumber = moNumber = ((Integer) moData.get("HOMO")).intValue()
              + offset;
        } else {
          for (int i = 0; i < nOrb; i++) {
            Hashtable mo = (Hashtable) mos.get(i);
            if (!mo.containsKey("occupancy"))
              evalError(GT._("no MO occupancy data available"));
            if (((Float) mo.get("occupancy")).floatValue() == 0) {
              lastMoNumber = moNumber = i + offset;
              break;
            }
          }
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        evalError(GT._("An MO index from 1 to {0} is required", nOrb));
    }
    lastMoNumber = moNumber;
    setShapeProperty(shape, "moData", moData);
    if (title != null)
      setShapeProperty(shape, "title", title);
    setShapeProperty(shape, "molecularOrbital", new Integer(moNumber));
    setShapeProperty(shape, "clear", null);
  }

  private void initIsosurface(int iShape) throws ScriptException {

    //handle isosurface/mo/pmesh delete and id delete here

    setShapeProperty(iShape, "init", thisCommand);
    iToken = 0;
    if (tokAt(1) == Token.delete || tokAt(2) == Token.delete
        && tokAt(++iToken) == Token.all) {
      setShapeProperty(iShape, "delete", null);
      iToken += 2;
      if (statementLength > iToken) {
        setShapeProperty(iShape, "init", thisCommand);
        setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      }
      return;
    }
    iToken = 1;
    if (!setMeshDisplayProperty(iShape, 0, tokAt(1))) {
      setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      if (iShape != JmolConstants.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
    }
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    if (getToken(1).value instanceof String[]) // not just the word "list"
      return false;
    checkLength2();
    if (!isSyntaxCheck)
      showString((String) viewer.getShapeProperty(iShape, "list"));
    return true;
  }

  private void isosurface(int iShape) throws ScriptException {
    viewer.loadShape(iShape);
    if (tokAt(1) == Token.list && listIsosurface(iShape))
      return;
    int colorRangeStage = 0;
    int signPt = 0;
    boolean isIsosurface = (iShape == JmolConstants.SHAPE_ISOSURFACE);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean isCavity = false;
    float[] nlmZ = new float[5];
    float[] data = null;
    int nFiles = 0;
    String str;
    int modelIndex = (isSyntaxCheck ? 0 : viewer.getDisplayModelIndex());
    if (!isSyntaxCheck)
      viewer.setCursor(Viewer.CURSOR_WAIT);
    boolean idSeen = false;
    String translucency = null;
    initIsosurface(iShape);
    for (int i = iToken; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (i != signPt)
          invalidParameterOrder();
        setShapeProperty(iShape, "colorRGB", new Integer(getArgbParam(i)));
        i = iToken;
        signPt = i + 1;
        idSeen = true;
        continue;
      }
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.within:
        float distance = floatParameter(++i);
        propertyValue = centerParameter(++i);
        i = iToken;
        propertyName = "withinPoint";
        setShapeProperty(iShape, "withinDistance", new Float(distance));
        break;
      case Token.property:
        setShapeProperty(iShape, "propertySmoothing", viewer
            .getIsosurfacePropertySmoothing() ? Boolean.TRUE : Boolean.FALSE);
        propertyName = "property";
        str = parameterAsString(i);
        if (!isCavity && str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (isSyntaxCheck)
            continue;
          data = viewer.getDataFloat(str);
          if (data == null)
            invalidArgument();
          propertyValue = data;
          break;
        }
        int atomCount = viewer.getAtomCount();
        int tokProperty = getToken(++i).tok;
        data = (isCavity ? new float[0] : new float[atomCount]);
        if (isCavity)//not implemented: && tokProperty != Token.surfacedistance)
          invalidArgument();
        if (!isSyntaxCheck && !isCavity) {
          Atom[] atoms = viewer.getModelSet().atoms;
          if (tokProperty == Token.surfacedistance)
            viewer.getSurfaceDistanceMax();
          for (int iAtom = atomCount; --iAtom >= 0;) {
            data[iAtom] = atomProperty(atoms[iAtom], tokProperty, false);
          }
        }
        propertyValue = data;
        break;
      case Token.model:
        if (surfaceObjectSeen)
          invalidArgument();
        modelIndex = modelNumberParameter(statement[++i]);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.select:
        propertyName = "select";
        propertyValue = expression(++i);
        i = iToken;
        break;
      case Token.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        i = iToken;
        break;
      case Token.color:
        /* 
         * "color" now is just used as an equivalent to "sign" 
         * and as an introduction to "absolute"
         * any other use is superfluous; it has been replaced with
         * MAP for indicating "use the current surface"
         * because the term COLOR is too general. 
         *  
         */
        colorRangeStage = 0;
        if (getToken(i + 1).tok == Token.string) {
          setShapeProperty(iShape, "setColorScheme", parameterAsString(++i));
        }
        if ((theTok = tokAt(i + 1)) == Token.translucent
            || tokAt(i + 1) == Token.opaque) {
          translucency = setColorOptions(i + 1, JmolConstants.SHAPE_ISOSURFACE,
              -2);
          i = iToken;
        }
        switch (tokAt(i + 1)) {
        case Token.absolute:
        case Token.range:
          getToken(++i);
          colorRangeStage = 1;
          propertyName = "rangeAll";
          if (tokAt(i + 1) == Token.all)
            getToken(++i);
          break;
        default:
          signPt = i + 1;
          continue;
        }
        break;
      case Token.file:
        continue;
      case Token.plus:
        if (colorRangeStage == 0) {
          propertyName = "cutoffPositive";
          propertyValue = new Float(floatParameter(++i));
        }
        break;
      case Token.decimal:
      case Token.integer:
        // default is "cutoff"
        propertyName = (colorRangeStage == 1 ? "red"
            : colorRangeStage == 2 ? "blue" : "cutoff");
        propertyValue = new Float(floatParameter(i));
        if (colorRangeStage > 0)
          ++colorRangeStage;
        break;
      case Token.ionic:
        propertyName = "ionicRadius";
        propertyValue = new Float(radiusParameter(++i, 0));
        i = iToken;
        break;
      case Token.vanderwaals:
        propertyName = "vdwRadius";
        propertyValue = new Float(radiusParameter(++i, 0));
        i = iToken;
        break;
      case Token.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = planeParameter(++i);
        i = iToken;
        break;
      case Token.identifier:
        str = parameterAsString(i);
        if (str.equalsIgnoreCase("REMAPPABLE")) { // testing only
          propertyName = "remappable";
          break;
        }
        if (str.equalsIgnoreCase("SQUARED")) {
          propertyName = "squareData";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("CAP")) {
          propertyName = "cappingPlane";
          propertyValue = planeParameter(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("IGNORE")) {
          propertyName = "ignore";
          propertyValue = expression(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("CUTOFF")) {
          if (++i < statementLength && getToken(i).tok == Token.plus) {
            propertyName = "cutoffPositive";
            propertyValue = new Float(floatParameter(++i));
          } else {
            propertyName = "cutoff";
            propertyValue = new Float(floatParameter(i));
          }
          break;
        }
        if (str.equalsIgnoreCase("CAVITY")) {
          if (!isIsosurface)
            invalidArgument();
          isCavity = true;
          if (isSyntaxCheck)
            continue;
          float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
              : 1.2f);
          float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
              : 10f);
          if (envelopeRadius > 10f)
            numberOutOfRange(0, 10);
          setShapeProperty(iShape, "envelopeRadius", new Float(envelopeRadius));
          setShapeProperty(iShape, "cavityRadius", new Float(cavityRadius));
          propertyName = "cavity";
          break;
        }
        if (str.equalsIgnoreCase("POCKET")) {
          propertyName = "pocket";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("INTERIOR")) {
          propertyName = "pocket";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("SCALE")) {
          propertyName = "scale";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("MINSET")) {
          propertyName = "minset";
          propertyValue = new Integer(intParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("ANGSTROMS")) {
          propertyName = "angstroms";
          break;
        }
        if (str.equalsIgnoreCase("RESOLUTION")
            || str.equalsIgnoreCase("POINTSPERANGSTROM")) {
          propertyName = "resolution";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("ANISOTROPY")) {
          propertyName = "anisotropy";
          propertyValue = getPoint3f(++i, false);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("ECCENTRICITY")) {
          propertyName = "eccentricity";
          propertyValue = getPoint4f(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("FIXED")) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("BLOCKDATA")) {
          propertyName = "blockData";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("MODELBASED")) {
          propertyName = "fixed";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("SIGN")) {
          signPt = i + 1;
          propertyName = "sign";
          propertyValue = Boolean.TRUE;
          colorRangeStage = 1;
          break;
        }
        if (str.equalsIgnoreCase("REVERSECOLOR")) {
          propertyName = "reverseColor";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("ADDHYDROGENS")) {
          propertyName = "addHydrogens";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("COLORSCHEME")) {
          propertyName = "setColorScheme";
          propertyValue = parameterAsString(++i);
          break;
        }
        if (str.equalsIgnoreCase("DEBUG") || str.equalsIgnoreCase("NODEBUG")) {
          propertyName = "debug";
          propertyValue = (str.equalsIgnoreCase("DEBUG") ? Boolean.TRUE
              : Boolean.FALSE);
          break;
        }
        if (str.equalsIgnoreCase("GRIDPOINTS")) {
          propertyName = "gridPoints";
          break;
        }
        if (str.equalsIgnoreCase("CONTOUR")) {
          propertyName = "contour";
          propertyValue = new Integer(
              tokAt(i + 1) == Token.integer ? intParameter(++i) : 0);
          break;
        }
        if (str.equalsIgnoreCase("PHASE")) {
          propertyName = "phase";
          propertyValue = (tokAt(i + 1) == Token.string ? stringParameter(++i)
              : "_orb");
          break;
        }
        if (str.equalsIgnoreCase("INSIDEOUT")) {
          propertyName = "insideOut";
          break;
        }
        // surface objects
        if (str.equalsIgnoreCase("MAP")) { // "use current"
          surfaceObjectSeen = !isCavity;
          propertyName = "map";
          break;
        }
        if (str.equalsIgnoreCase("hkl")) {
          // miller indices hkl 
          planeSeen = true;
          propertyName = "plane";
          propertyValue = hklParameter(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("sphere")) {
          //sphere [radius] 
          surfaceObjectSeen = true;
          propertyName = "sphere";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("ellipsoid")) {
          //ellipsoid {xc yc zc f} where a = b and f = a/c 
          surfaceObjectSeen = true;
          propertyName = "ellipsoid";
          propertyValue = getPoint4f(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("lobe")) {
          //lobe {eccentricity} 
          surfaceObjectSeen = true;
          propertyName = "lobe";
          propertyValue = getPoint4f(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("AtomicOrbital")
            || str.equalsIgnoreCase("orbital")) {
          surfaceObjectSeen = true;
          nlmZ[0] = intParameter(++i);
          nlmZ[1] = intParameter(++i);
          nlmZ[2] = intParameter(++i);
          nlmZ[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
          propertyName = "hydrogenOrbital";
          propertyValue = nlmZ;
          break;
        }
        if (str.equalsIgnoreCase("functionXY")) {
          surfaceObjectSeen = true;
          Vector v = new Vector();
          if (getToken(++i).tok != Token.string)
            invalidArgument();
          String fName;
          v.addElement(fName = parameterAsString(i++)); //(0) = name
          v.addElement(getPoint3f(i, false)); //(1) = {origin}
          Point4f pt;
          int nX, nY;
          v.addElement(pt = getPoint4f(++iToken)); //(2) = {ni ix iy iz}
          nX = (int) pt.x;
          v.addElement(pt = getPoint4f(++iToken)); //(3) = {nj jx jy jz}
          nY = (int) pt.x;
          v.addElement(getPoint4f(++iToken)); //(4) = {nk kx ky kz}
          if (nX == 0 || nY == 0)
            invalidArgument();
          if (!isSyntaxCheck)
            v.addElement(viewer.functionXY(fName, nX, nY)); //(5) = float[][] data
          i = iToken;
          propertyName = "functionXY";
          propertyValue = v;
          break;
        }
        if (str.equalsIgnoreCase("molecular")) {
          surfaceObjectSeen = true;
          propertyName = "molecular";
          propertyValue = new Float(1.4);
          break;
        }
        if (str.equalsIgnoreCase("VARIABLE")) {
          propertyName = "property";
          data = new float[viewer.getAtomCount()];
          if (!isSyntaxCheck) {
            Parser.parseFloatArray(""
                + getParameter(parameterAsString(++i), false), null, data);
          }
          propertyValue = data;
          break;
        }
        propertyValue = str;
      //fall through for identifiers
      case Token.all:
        if (idSeen)
          invalidArgument();
        propertyName = "thisID";
        break;
      case Token.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        setShapeProperty(iShape, "lcaoType", lcaoType);
        switch (getToken(++i).tok) {
        case Token.bitset:
        case Token.expressionBegin:
          propertyName = "lcaoCartoon";
          BitSet bs = expression(i);
          i = iToken;
          int atomIndex = BitSetUtil.firstSetBit(bs);
          modelIndex = 0;
          Point3f pt;
          if (atomIndex < 0) {
            if (!isSyntaxCheck)
              expressionExpected();
            pt = new Point3f();
          } else {
            modelIndex = viewer.getAtomModelIndex(atomIndex);
            pt = viewer.getAtomPoint3f(atomIndex);
          }
          setShapeProperty(iShape, "modelIndex", new Integer(modelIndex));
          Vector3f[] axes = { new Vector3f(), new Vector3f(), new Vector3f(pt) };
          if (!isSyntaxCheck)
            viewer.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                lcaoType, false);
          propertyValue = axes;
          break;
        default:
          expressionExpected();
        }
        break;
      case Token.mo:
        //mo 1-based-index 
        if (++i == statementLength)
          badArgumentCount();
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        if (tokAt(i) == Token.integer) {
          moNumber = intParameter(i);
        } else if ((offset = moOffset(i)) != Integer.MAX_VALUE) {
          moNumber = 0;
          i = iToken;
        }
        setMoData(iShape, moNumber, offset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case Token.mep:
        float[] partialCharges = null;
        try {
          partialCharges = viewer.getPartialCharges();
        } catch (Exception e) {
        }
        if (!isSyntaxCheck && partialCharges == null)
          evalError(GT
              ._("No partial charges were read from the file; Jmol needs these to render the MEP data."));
        surfaceObjectSeen = true;
        propertyName = "mep";
        propertyValue = partialCharges;
        break;
      case Token.sasurface:
      case Token.solvent:
        surfaceObjectSeen = true;
        setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
        propertyName = (theTok == Token.sasurface ? "sasurface" : "solvent");
        float radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer
            .getSolventProbeRadius());
        propertyValue = new Float(radius);
        break;
      case Token.string:
        propertyName = surfaceObjectSeen || planeSeen ? "mapColor" : "readFile";
        /*
         * a file name, optionally followed by an integer file index.
         * OR empty. In that case, if the model auxiliary info has the
         * data stored in it, we use that. There are two possible structures:
         * 
         * jmolSurfaceInfo
         * jmolMappedDataInfo 
         * 
         * Both can be present, but if jmolMappedDataInfo is missing,
         * then jmolSurfaceInfo is used by default.
         * 
         */
        String filename = parameterAsString(i);
        if (filename.length() == 0) {
          if (surfaceObjectSeen || planeSeen)
            propertyValue = viewer.getModelAuxiliaryInfo(modelIndex,
                "jmolMappedDataInfo");
          if (propertyValue == null)
            propertyValue = viewer.getModelAuxiliaryInfo(modelIndex,
                "jmolSurfaceInfo");
          surfaceObjectSeen = true;
          if (propertyValue != null)
            break;
          filename = getFullPathName();
        }
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == Token.integer)
          setShapeProperty(iShape, "fileIndex", new Integer(intParameter(++i)));
        if (thisCommand.indexOf("# FILE" + nFiles + "=") >= 0)
          filename = extractCommandOption("FILE" + nFiles);        
        String[] fullPathNameReturn = new String[1];
        Object t = (isSyntaxCheck ? null : viewer
            .getBufferedReaderOrErrorMessageFromName(filename, fullPathNameReturn));
        if (t instanceof String)
          fileNotFoundException(filename + ":" + t);
        if (!isSyntaxCheck)
          Logger.info("reading isosurface data from " + fullPathNameReturn[0]);
        setShapeProperty(iShape, "commandOption",
            "FILE" + (nFiles++) + "=" + Escape.escape(fullPathNameReturn[0]));
        propertyValue = t;
        break;
      default:
        if (planeSeen && !surfaceObjectSeen) {
          setShapeProperty(iShape, "nomap", new Float(0));
          surfaceObjectSeen = true;
        }
        if (!setMeshDisplayProperty(iShape, i, theTok))
          invalidArgument();
        i = iToken;
      }
      idSeen = (theTok != Token.delete);
      if (propertyName == "property" && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
        setShapeProperty(iShape, "sasurface", new Float(0));
      }
      if (propertyName != null)
        setShapeProperty(iShape, propertyName, propertyValue);
    }
    if (isCavity && !surfaceObjectSeen) {
      surfaceObjectSeen = true;
      setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
      setShapeProperty(iShape, "sasurface", new Float(0));
    }

    if (planeSeen && !surfaceObjectSeen) {
      setShapeProperty(iShape, "nomap", new Float(0));
      surfaceObjectSeen = true;
    }

    if (surfaceObjectSeen && isIsosurface && !isSyntaxCheck) {
      String s = (String) viewer.getShapeProperty(iShape, "ID");
      float[] dataRange = (float[]) viewer.getShapeProperty(iShape, "dataRange");
      Integer n = (Integer) viewer.getShapeProperty(iShape, "count");
      if (s != null) {
        s += " created; number of isosurfaces = " + n;
        if (dataRange != null && dataRange[0] != dataRange[1])
          s += "\ncolor range " + dataRange[2] + " " + dataRange[3] + "; mapped data range " + dataRange[0] + " to " + dataRange[1];
        showString(s);
      }
      setShapeProperty(iShape, "finalize", null);
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
  }

  private boolean setMeshDisplayProperty(int shape, int i, int tok)
      throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    boolean checkOnly = (i == 0);
    //these properties are all processed in MeshCollection.java

    switch (tok) {
    case Token.nada:
    case Token.on:
    case Token.off:
    case Token.delete:
      if (iToken == 1)
        setShapeProperty(shape, "thisID", (String) null);
      if (tok == Token.nada)
        return (iToken == 1);
      if (!checkOnly)
        setShapeProperty(shape, parameterAsString(iToken), null);
      return true;
    case Token.dots:
      propertyValue = Boolean.TRUE;
    //fall through
    case Token.nodots:
      propertyName = "dots";
      break;
    case Token.mesh:
      propertyValue = Boolean.TRUE;
    //fall through
    case Token.nomesh:
      propertyName = "mesh";
      break;
    case Token.fill:
      propertyValue = Boolean.TRUE;
    //fall through
    case Token.nofill:
      propertyName = "fill";
      break;
    case Token.triangles:
      propertyValue = Boolean.TRUE;
    //fall through
    case Token.notriangles:
      propertyName = "triangles";
      break;
    case Token.frontonly:
      propertyValue = Boolean.TRUE;
    //fall through
    case Token.notfrontonly:
      propertyName = "frontOnly";
      break;
    case Token.frontlit:
      propertyName = "lighting";
      propertyValue = new Integer(JmolConstants.FRONTLIT);
      break;
    case Token.backlit:
      propertyName = "lighting";
      propertyValue = new Integer(JmolConstants.BACKLIT);
      break;
    case Token.fullylit:
      propertyName = "lighting";
      propertyValue = new Integer(JmolConstants.FULLYLIT);
      break;
    case Token.opaque:
    case Token.translucent:
      if (checkOnly)
        return true;
      colorShape(shape, iToken, false);
      return true;
    }
    if (propertyName == null)
      return false;
    if (checkOnly)
      return true;
    setShapeProperty(shape, propertyName, propertyValue);
    if ((tok = tokAt(iToken + 1)) != Token.nada) {
      if (!setMeshDisplayProperty(shape, ++iToken, tok))
        --iToken;
    }
    return true;
  }

  ////// script exceptions ///////

  private boolean ignoreError;

  void evalError(String message) throws ScriptException {
    if (ignoreError)
      throw new NullPointerException();
    if (!isSyntaxCheck) {
      String s = viewer.removeCommand();
      viewer.addCommand(s + CommandHistory.ERROR_FLAG);
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
      viewer.setRefreshing(true);
    }
    throw new ScriptException(message);
  }

  //  private void evalWarning(String message) {
  //    new ScriptException(message);
  //  }

  private void multipleModelsNotOK(String what) throws ScriptException {
    evalError(GT._("{0} require that only one model be displayed", what));
  }

  private void unrecognizedCommand() throws ScriptException {
    evalError(GT._("unrecognized command") + ": " + statement[0].value);
  }

  private void unrecognizedAtomProperty(String prop) throws ScriptException {
    evalError(GT._("unrecognized atom property") + ": " + prop);
  }

  private void unrecognizedBondProperty(String prop) throws ScriptException {
    evalError(GT._("unrecognized bond property") + ": " + prop);
  }

  private void filenameExpected() throws ScriptException {
    evalError(GT._("filename expected"));
  }

  private void booleanExpected() throws ScriptException {
    evalError(GT._("boolean expected"));
  }

  private void booleanOrNumberExpected() throws ScriptException {
    evalError(GT._("boolean or number expected"));
  }

  private void booleanOrNumberExpected(String orWhat) throws ScriptException {
    evalError(GT._("boolean, number, or {0} expected", "\"" + orWhat + "\""));
  }

  private void expressionOrIntegerExpected() throws ScriptException {
    evalError(GT._("(atom expression) or integer expected"));
  }

  private void expressionExpected() throws ScriptException {
    evalError(GT._("valid (atom expression) expected"));
  }

  private void badRGBColor() throws ScriptException {
    evalError(GT._("bad [R,G,B] color"));
  }

  private int integerExpected() throws ScriptException {
    evalError(GT._("integer expected"));
    return 0;
  }

  private float numberExpected() throws ScriptException {
    evalError(GT._("number expected"));
    return 0;
  }

  private String stringExpected() throws ScriptException {
    evalError(GT._("quoted string expected"));
    return "";
  }

  private void stringOrIdentifierExpected() throws ScriptException {
    evalError(GT._("quoted string or identifier expected"));
  }

  private void propertyNameExpected() throws ScriptException {
    evalError(GT._("property name expected"));
  }

  private void axisExpected() throws ScriptException {
    evalError(GT._("x y z axis expected"));
  }

  private void colorExpected() throws ScriptException {
    evalError(GT._("color expected"));
  }

  private void unrecognizedObject() throws ScriptException {
    evalError(GT._("unrecognized object"));
  }

  private void unrecognizedExpression() throws ScriptException {
    evalError(GT._("runtime unrecognized expression"));
  }

  void endOfStatementUnexpected() throws ScriptException {
    evalError(GT._("unexpected end of script command"));
  }

  private void badArgumentCount() throws ScriptException {
    evalError(GT._("bad argument count"));
  }

  void invalidArgument() throws ScriptException {
    evalError(GT._("invalid argument"));
  }

  void unrecognizedParameter(String kind, String param) throws ScriptException {
    evalError(GT._("unrecognized {0} parameter", kind) + ": " + param);
  }

  private void unrecognizedShowParameter(String use) throws ScriptException {
    evalError(GT._("unrecognized SHOW parameter --  use {0}", use));
  }

  private void numberOutOfRange(int min, int max) throws ScriptException {
    evalError(GT._("integer out of range ({0} - {1})", new Object[] {
        new Integer(min), new Integer(max) }));
  }

  private void numberOutOfRange(float min, float max) throws ScriptException {
    evalError(GT._("decimal number out of range ({0} - {1})", new Object[] {
        new Float(min), new Float(max) }));
  }

  private void numberMustBe(int a, int b) throws ScriptException {
    evalError(GT._("number must be ({0} or {1})", new Object[] {
        new Integer(a), new Integer(b) }));
  }

  private void fileNotFoundException(String filename) throws ScriptException {
    evalError(GT._("file not found") + ": " + filename);
  }

  private void drawObjectNotDefined(String drawID) throws ScriptException {
    evalError(GT._("draw object not defined") + ": " + drawID);
  }

  String objectNameExpected() throws ScriptException {
    evalError(GT._("object name expected after '$'"));
    return "";
  }

  private void coordinateOrNameOrExpressionRequired() throws ScriptException {
    evalError(GT._(" {x y z} or $name or (atom expression) required"));
  }

  private void keywordExpected(String what) throws ScriptException {
    evalError(GT._("keyword expected") + ": " + what);
  }

  private void invalidParameterOrder() throws ScriptException {
    evalError(GT._("invalid parameter order"));
  }

  private void incompatibleArguments() throws ScriptException {
    evalError(GT._("incompatible arguments"));
  }

  private void insufficientArguments() throws ScriptException {
    evalError(GT._("insufficient arguments"));
  }

  private String statementAsString() {
    StringBuffer sb = new StringBuffer();
    int tok = statement[0].tok;
    boolean addParens = (Compiler.tokAttr(tok, Token.embeddedExpression));
    boolean useBraces = (Compiler.tokAttr(tok, Token.implicitExpression));
    boolean inBrace = false;
    boolean inClauseDefine = false;
    boolean setEquals = (tok == Token.set && ((String) statement[0].value) == "");
    for (int i = 0; i < statementLength; ++i) {
      if (iToken == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.append(' ');
      if (i == 2 && setEquals) {
        setEquals = false;
        sb.append("= ");
      }
      Token token = statement[i];
      if (iToken == i && token.tok != Token.expressionEnd)
        sb.append(">> ");
      switch (token.tok) {
      case Token.expressionBegin:
        if (useBraces)
          sb.append("{");
        else if (addParens)
          sb.append("(");
        continue;
      case Token.expressionEnd:
        if (inClauseDefine && i == statementLength - 1)
          useBraces = false;
        if (useBraces)
          sb.append("}");
        else if (addParens)
          sb.append(")");
        continue;
      case Token.leftsquare:
      case Token.rightsquare:
        break;
      case Token.leftbrace:
      case Token.rightbrace:
        inBrace = (token.tok == Token.leftbrace);
        break;
      case Token.define:
        if (i > 0 && ((String) token.value).equals("define")) {
          sb.append("@");
          if (tokAt(i + 1) == Token.expressionBegin) {
            if (!useBraces)
              inClauseDefine = true;
            useBraces = true;
          }
          continue;
        }
        break;
      case Token.on:
        sb.append("true");
        continue;
      case Token.off:
        sb.append("false");
        continue;
      case Token.select:
        break;
      case Token.integer:
        sb.append(token.intValue);
        continue;
      case Token.point3f:
      case Token.point4f:
      case Token.bitset:
        sb.append(Token.sValue(token));
        continue;
      case Token.spec_seqcode_range:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        token = statement[++i];
        sb.append(' ');
        //        if (token.intValue == Integer.MAX_VALUE)
        sb.append(inBrace ? "-" : "- ");
      //fall through
      case Token.spec_seqcode:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        continue;
      case Token.spec_chain:
        sb.append("*:");
        sb.append((char) token.intValue);
        continue;
      case Token.spec_alternate:
        sb.append("*%");
        if (token.value != null)
          sb.append(token.value.toString());
        continue;
      case Token.spec_model:
        sb.append("*/");
      //fall through
      case Token.spec_model2:
      case Token.decimal:
        if (token.intValue < Integer.MAX_VALUE) {
          sb.append(Escape.escapeModelFileNumber(token.intValue));
        } else {
          sb.append("" + token.value);
        }
        continue;
      case Token.spec_resid:
        sb.append('[');
        sb.append(Group.getGroup3((short) token.intValue));
        sb.append(']');
        continue;
      case Token.spec_name_pattern:
        sb.append('[');
        sb.append(token.value);
        sb.append(']');
        continue;
      case Token.spec_atom:
        sb.append("*.");
        break;
      case Token.cell:
        if (token.value instanceof Point3f) {
          Point3f pt = (Point3f) token.value;
          sb.append("cell={").append(pt.x).append(" ").append(pt.y).append(" ")
              .append(pt.z).append("}");
          continue;
        }
        break;
      case Token.string:
        sb.append("\"").append(token.value).append("\"");
        continue;
      case Token.opEQ:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opLT:
      case Token.opNE:
        //not quite right -- for "inmath"
        if (token.intValue == Token.property) {
          sb.append((String) statement[++i].value).append(" ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(Token.nameOf(token.intValue)).append(" ");
        break;
      case Token.identifier:
        break;
      default:
        if (!logMessages)
          break;
        sb.append(token.toString());
        continue;
      }
      if (token.value != null)
        // value SHOULD NEVER BE NULL, BUT JUST IN CASE...
        sb.append(token.value.toString());
    }
    if (iToken >= statementLength - 1)
      sb.append(" <<");
    return sb.toString();
  }

  String contextTrace() {
    StringBuffer sb = new StringBuffer();
    for (;;) {
      String s = (functionName == null ? "" : " function " + functionName)
          + " file " + filename;
      sb
          .append(setErrorLineMessage(s, getLinenumber(), pc,
              statementAsString()));
      if (scriptLevel > 0)
        popContext();
      else
        break;
    }
    return sb.toString();
  }

  static String setErrorLineMessage(String filename, int lineCurrent,
                                    int pcCurrent, String lineInfo) {
    String err = "\n----";
    if (filename != null)
      err += "line " + lineCurrent + " command " + (pcCurrent + 1) + " of"
          + filename + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  class ScriptException extends Exception {

    private String message;

    ScriptException(String message) {
      this.message = (message == null ? "" : message) + contextTrace();

      if (!isSyntaxCheck)
        Logger.error("eval ERROR: " + toString());
    }

    public String toString() {
      return message;
    }
  }

  /// Reverse Polish Notation Engine for IF, SET, and %{...} -- Bob Hanson 2/16/2007
  /// Just a simple RPN processor that can handle 
  /// boolean, int, float, String, Point3f, and BitSet

  class Rpn {

    private Token[] oStack;
    private Token[] xStack;
    private int oPt = -1;
    private int xPt = -1;
    private int maxLevel;
    private int parenCount;
    private int squareCount;
    private int braceCount;
    private boolean wasX;
    private boolean isAssignment;
    private boolean asVector;

    Rpn(int maxLevel, boolean isAssignment, boolean asVector) {
      this.isAssignment = isAssignment;
      this.maxLevel = maxLevel;
      this.asVector = asVector;
      oStack = new Token[maxLevel];
      xStack = new Token[maxLevel];
      if (logMessages)
        Logger.info("initialize RPN on " + getScript());
    }

    Token getResult(boolean allowUnderflow, String key) throws ScriptException {
      boolean isOK = true;
      Token x = null;
      int selector = Integer.MAX_VALUE;
      while (isOK && oPt >= 0)
        isOK = operate();
      if (isOK && isAssignment && xPt == 2 && xStack[1].tok == Token.leftsquare) {
        x = xStack[2];
        selector = Token.iValue(xStack[0]);
        xPt = 0;
      } else if (isOK && asVector) {
        Vector result = new Vector();
        for (int i = 0; i <= xPt; i++)
          result.addElement(Token.selectItem(xStack[i]));
        return new Token(Token.vector, result);
      }
      if (isOK && xPt == 0) {
        if (x == null)
          x = xStack[0];
        if (x.tok == Token.bitset || x.tok == Token.list
            || x.tok == Token.string)
          x = xStack[0] = Token.selectItem(x);
        if (selector == Integer.MAX_VALUE && key != null && key.length() > 0
            && !isSyntaxCheck)
          viewer.setListVariable(key, x.tok == Token.list ? x : null);
        if (selector != Integer.MAX_VALUE || x.tok == Token.list)
          x = new Token(Token.string, selector, Token.sValue(x));
        return x;
      }
      if (!allowUnderflow && (xPt >= 0 || oPt >= 0)) {
        iToken--;
        invalidArgument();
      }
      return null;
    }

    boolean addX(Token x) throws ScriptException {
      if (xPt + 1 == maxLevel)
        stackOverflow();
      if (wasX && x.tok == Token.integer && x.intValue < 0) {
        addOp(Token.tokenMinus);
        xStack[++xPt] = Token.intToken(-x.intValue);
      } else if (wasX && x.tok == Token.decimal
          && ((Float) x.value).floatValue() < 0) {
        addOp(Token.tokenMinus);
        xStack[++xPt] = new Token(Token.decimal, new Float(-Token.fValue(x)));
      } else {
        xStack[++xPt] = x;
      }
      if (logMessages)
        Logger.info("addX token " + x);
      return wasX = true;
    }

    boolean addX(Object x) throws ScriptException {
      if (x instanceof Integer)
        return addX(((Integer) x).intValue());
      if (x instanceof Float)
        return addX(((Float) x).floatValue());
      if (x instanceof String)
        return addX((String) x);
      if (x instanceof Point3f)
        return addX((Point3f) x);
      if (x instanceof Point4f)
        return addX((Point4f) x);
      if (x instanceof BitSet)
        return addX((BitSet) x);
      if (x instanceof Token)
        return addX((Token) x);
      return false;
    }

    boolean addX(boolean x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = (x ? Token.tokenOn : Token.tokenOff);
      return true;
    }

    boolean addX(int x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = Token.intToken(x);
      return wasX = true;
    }

    boolean addX(float x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.decimal, new Float(x));
      return wasX = true;
    }

    boolean addX(String x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.string, x);
      return wasX = true;
    }

    boolean addX(String[] x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.list, x);
      return wasX = true;
    }

    boolean addX(Point3f x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.point3f, x);
      return wasX = true;
    }

    boolean addX(Point4f x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.point4f, x);
      return wasX = true;
    }

    boolean addX(BitSet x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.bitset, x);
      return wasX = true;
    }

    boolean isOpFunc(Token op) {
      return (Compiler.tokAttr(op.tok, Token.mathfunc) || op.tok == Token.propselector
          && Compiler.tokAttr(op.intValue, Token.mathfunc));
    }

    boolean addOp(Token op) throws ScriptException {

      // Do we have the appropriate context for this operator?

      if (logMessages) {
        dumpStacks();
        Logger.info("\naddOp: " + op);
      }
      Token newOp = null;
      int tok;
      boolean isLeftOp = false;
      boolean isDotSelector = (op.tok == Token.propselector);

      if (isDotSelector && !wasX)
        return false;

      boolean isMathFunc = isOpFunc(op);

      // the word "plane" can also appear alone, not as a function
      if (oPt >= 1 && op.tok != Token.leftparen
          && Compiler.tokAttr(oStack[oPt].tok, Token.specialstring))
        oPt--;

      // math functions as arguments appear without a prefixing operator
      boolean isArgument = (oPt >= 1 && oStack[oPt].tok == Token.leftparen);

      switch (op.tok) {
      case Token.comma:
        if (!wasX)
          return false;
        break;
      case Token.min:
      case Token.max:
      case Token.minmaxmask:
        tok = oPt < 0 ? Token.nada : oStack[oPt].tok;
        if (!wasX
            || !(tok == Token.propselector || tok == Token.bonds || tok == Token.atoms))
          return false;
        oStack[oPt].intValue |= op.tok;
        return true;
      case Token.leftsquare: // {....}[n][m]
        isLeftOp = true;
        break;
      case Token.minus:
        if (wasX)
          break;
        addX(0);
        op = new Token(Token.unaryMinus, "-");
        break;
      case Token.rightparen: //  () without argument allowed only for math funcs
        if (!wasX && oPt >= 1 && oStack[oPt].tok == Token.leftparen
            && !isOpFunc(oStack[oPt - 1]))
          return false;
        break;
      case Token.opNot:
      case Token.leftparen:
        isLeftOp = true;
      default:
        if (isMathFunc) {
          if (!isDotSelector && wasX && !isArgument)
            return false;
          newOp = op;
          isLeftOp = true;
          break;
        }
        if (wasX == isLeftOp)
          return false;
        break;
      }

      //do we need to operate?

      while (oPt >= 0
          && (!(isLeftOp || op.tok == Token.leftsquare) || (op.tok == Token.propselector || op.tok == Token.leftsquare)
              && oStack[oPt].tok == Token.propselector)
          && Token.prec(oStack[oPt].tok) >= Token.prec(op.tok)) {

        if (logMessages) {
          dumpStacks();
          Logger.info("\noperating, oPt=" + oPt + " isLeftOp=" + isLeftOp
              + " oStack[oPt]=" + Token.nameOf(oStack[oPt].tok) + "/"
              + Token.prec(oStack[oPt].tok) + " op=" + Token.nameOf(op.tok)
              + "/" + Token.prec(op.tok));
        }
        // ) and ] must wait until matching ( or [ is found
        if (op.tok == Token.rightparen && oStack[oPt].tok == Token.leftparen) { 
          // (x[2]) finalizes the selection
          xStack[xPt] = Token.selectItem(xStack[xPt]);
          break;
        }

        if (op.tok == Token.rightsquare && oStack[oPt].tok == Token.leftsquare) {
          if (xPt == 0 && isAssignment) {
            addX(Token.tokenArraySelector);
            break;
          }
          if (!doBitsetSelect())
            return false;
          break;
        }

        // if not, it's time to operate

        if (!operate())
          return false;

      }

      // now add a marker on the xStack if necessary

      if (newOp != null)
        addX(newOp);

      // fix up counts and operand flag
      // right ) and ] are not added to the stack

      switch (op.tok) {

      case Token.comma:
        wasX = false;
        return true;
      case Token.leftparen:
        parenCount++;
        wasX = false;
        break;
      case Token.leftsquare:
        squareCount++;
        wasX = false;
        break;
      case Token.rightparen:
        wasX = true;
        oPt--;
        if (parenCount-- <= 0)
          return false;
        if (oPt < 0)
          return true;
        return (isOpFunc(oStack[oPt]) ? evaluateFunction() : true);
      case Token.rightsquare:
        wasX = true;
        oPt--;
        return (squareCount-- > 0);
      case Token.propselector:
        wasX = !Compiler.tokAttr(op.intValue, Token.mathfunc);
        break;
      case Token.leftbrace:
        braceCount++;
        wasX = false;
        break;
      case Token.rightbrace:
        if (braceCount-- <= 0)
          return false;
      default:
        wasX = false;
      }

      //add the operator if possible

      if (++oPt == maxLevel)
        stackOverflow();
      oStack[oPt] = op;
      return true;
    }

    private boolean doBitsetSelect() {
      if (xPt < 0 || xPt == 0 && !isAssignment) {
        return false;
      }
      int i = Token.iValue(xStack[xPt--]);
      Token token = xStack[xPt];
      switch (token.tok) {
      default:
        token = new Token(Token.string, Token.sValue(token));
      //fall through
      case Token.bitset:
      case Token.list:
      case Token.string:
        xStack[xPt] = Token.selectItem(token, i);
      }
      return true;
    }

    void dumpStacks() {
      Logger.info("RPN stacks: for " + getScript());
      for (int i = 0; i <= xPt; i++)
        Logger.info("x[" + i + "]: " + xStack[i]);
      Logger.info("\n");
      for (int i = 0; i <= oPt; i++)
        Logger.info("o[" + i + "]: " + oStack[i] + " prec="
            + Token.prec(oStack[i].tok));
    }

    Token getX() throws ScriptException {
      if (xPt < 0)
        endOfStatementUnexpected();
      return xStack[xPt--];
    }

    private boolean evaluateFunction() throws ScriptException {

      Token op = oStack[oPt--];
      int tok = (op.tok == Token.propselector ? op.intValue : op.tok);
      // for .xxx or .xxx() functions
      // we store the token inthe intValue field of the propselector token
      int nParamMax = Token.prec(tok); // note - this is NINE for dot-operators
      int nParam = 0;
      int pt = xPt;
      while (pt >= 0 && xStack[pt--] != op)
        nParam++;
      if (nParamMax > 0 && nParam > nParamMax)
        return false;
      Token[] args = new Token[nParam];
      for (int i = nParam; --i >= 0;)
        args[i] = getX();
      xPt--;
      switch (tok) {
      case Token.function:
        return evaluateUserFunction((String) op.value, args);
      case Token.find:
        return evaluateFind(args);
      case Token.replace:
        return evaluateReplace(args);
      case Token.array:
        return evaluateArray(args);
      case Token.split:
      case Token.join:
      case Token.trim:
        return evaluateString(op.intValue, args);
      case Token.add:
      case Token.sub:
      case Token.mul:
      case Token.div:
        return evaluateList(op.intValue, args);
      case Token.label:
        return evaluateLabel(args);
      case Token.data:
        return evaluateData(args);
      case Token.load:
        return evaluateLoad(args);
      case Token.script:
      case Token.javascript:
        return evaluateScript(args, tok == Token.javascript);
      case Token.within:
        return evaluateWithin(args);
      case Token.getproperty:
        return evaluateGetProperty(args);
      case Token.point:
        return evaluatePoint(args);
      case Token.plane:
        return evaluatePlane(args);
      case Token.distance:
        if (op.tok == Token.propselector)
          return evaluateDistance(args);
      //fall through
      case Token.angle:
        return evaluateMeasure(args, op.tok == Token.angle);
      case Token.connected:
        return evaluateConnected(args);
      case Token.substructure:
        return evaluateSubstructure(args);
      }
      return false;
    }

    private boolean evaluateDistance(Token[] args) throws ScriptException {
      Token x1 = getX();
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX(1f);
      Token x2 = args[0];
      Point3f pt2 = ptValue(x2);
      Point4f plane2 = planeValue(x2);
      if (x1.tok == Token.bitset)
        return addX(getBitsetProperty(Token.bsSelect(x1), Token.distance, pt2,
            plane2, x1.value, null, false));
      Point3f pt1 = ptValue(x1);
      Point4f plane1 = planeValue(x1);
      if (plane1 == null)
        return addX(plane2 == null ? pt2.distance(pt1) : Graphics3D
            .distanceToPlane(plane2, pt1));
      return addX(Graphics3D.distanceToPlane(plane1, pt2));
    }

    private boolean evaluateMeasure(Token[] args, boolean isAngle)
        throws ScriptException {
      int nPoints = args.length;
      if (nPoints < (isAngle ? 3 : 2) || nPoints > (isAngle ? 4 : 2))
        return false;
      if (isSyntaxCheck)
        return addX(1f);

      Point3f[] pts = new Point3f[nPoints];
      for (int i = 0; i < nPoints; i++)
        pts[i] = ptValue(args[i]);
      switch (nPoints) {
      case 2:
        return addX(pts[0].distance(pts[1]));
      case 3:
        return addX(Measure.computeAngle(pts[0], pts[1], pts[2], true));
      case 4:
        return addX(Measure
            .computeTorsion(pts[0], pts[1], pts[2], pts[3], true));
      }
      return false;
    }

    private boolean evaluateUserFunction(String name, Token[] args)
        throws ScriptException {
      if (isSyntaxCheck)
        return addX((int) 1);
      Vector params = new Vector();
      for (int i = 0; i < args.length; i++)
        params.addElement(args[i]);
      Token token = getFunctionReturn(name, params);
      wasX = false;
      return (token == null ? false : addX(token));
    }

    private boolean evaluateFind(Token[] args) throws ScriptException {
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX((int) 1);
      Token x1 = getX();
      String sFind = Token.sValue(args[0]);
      switch (x1.tok) {
      default:
        return addX(Token.sValue(x1).indexOf(sFind) + 1);
      case Token.list:
        int n = 0;
        String[] list = (String[]) x1.value;
        int ipt = -1;
        for (int i = 0; i < list.length; i++)
          if (list[i].indexOf(sFind) >= 0) {
            n++;
            ipt = i;
          }
        if (n == 1)
          return addX(list[ipt]);
        String[] listNew = new String[n];
        if (n > 0)
          for (int i = list.length; --i >= 0;)
            if (list[i].indexOf(sFind) >= 0)
              listNew[--n] = list[i];
        return addX(listNew);
      }
    }

    private boolean evaluateGetProperty(Token[] args) throws ScriptException {
      if (isSyntaxCheck)
        return addX("");
      int pt = 0;
      String propertyName = (args.length > pt ? Token.sValue(args[pt++]).toLowerCase() : "");
      Object propertyValue = (args.length > pt && args[pt].tok == Token.bitset ? 
          (Object) Token.bsSelect(args[pt++]) 
          : args.length > pt && args[pt].tok == Token.string &&
            PropertyManager.acceptsStringParameter(propertyName) ?
              args[pt++].value : (Object) "");
      if (propertyName.equalsIgnoreCase("fileContents") && args.length > 2) {
        String s = Token.sValue(args[1]);
        for (int i = 2; i < args.length; i++)
          s += "|" + Token.sValue(args[i]);
        propertyValue = s;
        pt = args.length;
      }
      Object property = viewer.getProperty(null, propertyName, propertyValue);
      property = PropertyManager.extractProperty(property, args, pt);
      if (property instanceof String)
        return addX(property);
      if (property instanceof Integer)
        return addX(property);
      if (property instanceof Float)
        return addX(property);
      if (property instanceof Point3f)
        return addX(property);
      if (property instanceof Vector3f)
        return addX(new Point3f((Vector3f)property));
      if (property instanceof Vector) {
        Vector v = (Vector) property;
        int len = v.size();
        String[] list = new String[len];
        for (int i = 0; i < len; i++) {
          Object o = v.elementAt(i);
          if (o instanceof String)
            list[i] = (String) o;
          else
            list[i] = Escape.toReadable(o);
        }
        return addX(list);
      }
      return addX(Escape.toReadable(property));
    }
    
    private boolean evaluatePoint(Token[] args) throws ScriptException {
      if (args.length != 1 && args.length != 3)
        return false;
      if (isSyntaxCheck)
        return addX(new Point3f(0, 0, 0));

      switch (args.length) {
      case 1:
        Object pt = Escape.unescapePoint(Token.sValue(args[0]));
        if (pt instanceof Point3f)
          return addX((Point3f) pt);
        return addX("" + pt);
      case 3:
        float x = Token.fValue(args[0]);
        float y = Token.fValue(args[1]);
        float z = Token.fValue(args[2]);
        return addX(new Point3f(x, y, z));
      }
      return false;
    }

    private boolean evaluatePlane(Token[] args) throws ScriptException {
      if (args.length != 1 && args.length != 3 && args.length != 4)
        return false;
      if (isSyntaxCheck)
        return addX(new Point4f(0, 0, 1, 0));

      switch (args.length) {
      case 1:
        Object pt = Escape.unescapePoint(Token.sValue(args[0]));
        if (pt instanceof Point4f)
          return addX((Point4f) pt);
        return addX("" + pt);
      case 3:
      case 4:
        switch (args[0].tok) {
        case Token.bitset:
        case Token.point3f:
          Point3f pt1 = ptValue(args[0]);
          Point3f pt2 = ptValue(args[1]);
          Point3f pt3 = ptValue(args[2]);
          Vector3f vAB = new Vector3f();
          Vector3f vAC = new Vector3f();
          Vector3f norm = new Vector3f();
          float nd = Graphics3D.getDirectedNormalThroughPoints(pt1, pt2, pt3,
              (args.length == 4 ? ptValue(args[3]) : null), norm, vAB, vAC);
          //System.out.println("evaluatePlane: " + new Point4f(norm.x, norm.y, norm.z, nd));
          return addX(new Point4f(norm.x, norm.y, norm.z, nd));
        default:
          float x = Token.fValue(args[0]);
          float y = Token.fValue(args[1]);
          float z = Token.fValue(args[2]);
          float w = Token.fValue(args[3]);
          return addX(new Point4f(x, y, z, w));
        }
      }
      return false;
    }

    private boolean evaluateReplace(Token[] args) throws ScriptException {
      if (args.length != 2)
        return false;
      Token x = getX();
      if (isSyntaxCheck)
        return addX("");
      String sFind = Token.sValue(args[0]);
      String sReplace = Token.sValue(args[1]);
      String s = (x.tok == Token.list ? null : Token.sValue(x));
      if (s != null)
        return addX(TextFormat.simpleReplace(s, sFind, sReplace));
      String[] list = (String[]) x.value;
      for (int i = list.length; --i >= 0;)
        list[i] = TextFormat.simpleReplace(list[i], sFind, sReplace);
      return addX(list);
    }

    private boolean evaluateString(int tok, Token[] args)
        throws ScriptException {
      if (args.length > 1)
        return false;
      Token x = getX();
      if (isSyntaxCheck)
        return addX(Token.sValue(x));
      String s = (tok == Token.split && x.tok == Token.bitset
          || tok == Token.trim && x.tok == Token.list ? null : Token.sValue(x));
      String sArg = (args.length == 1 ? Token.sValue(args[0])
          : tok == Token.trim ? "" : "\n");
      switch (tok) {
      case Token.split:
        if (x.tok == Token.bitset) {
          BitSet bsSelected = Token.bsSelect(x);
          sArg = "\n";
          int modelCount = viewer.getModelCount();
          s = "";
          for (int i = 0; i < modelCount; i++) {
            s += (i == 0 ? "" : "\n");
            BitSet bs = viewer.getModelAtomBitSet(i, true);
            bs.and(bsSelected);
            s += Escape.escape(bs);
          }
        }
        return addX(TextFormat.split(s, sArg));
      case Token.join:
        if (s.length() > 0 && s.charAt(s.length() - 1) == '\n')
          s = s.substring(0, s.length() - 1);
        return addX(TextFormat.simpleReplace(s, "\n", sArg));
      case Token.trim:
        if (s != null)
          return addX(TextFormat.trim(s, sArg));
        String[] list = (String[]) x.value;
        for (int i = list.length; --i >= 0;)
          list[i] = TextFormat.trim(list[i], sArg);
        return addX(list);
      }
      return addX("");
    }

    private boolean evaluateList(int tok, Token[] args) throws ScriptException {
      if (args.length != 1)
        return false;
      Token x1 = getX();
      Token x2 = args[0];
      if (x1.tok != Token.list && x1.tok != Token.string)
        return false;
      if (isSyntaxCheck)
        return addX("");

      boolean isScalar = (x2.tok != Token.list && Token.sValue(x2)
          .indexOf("\n") < 0);

      String sValue = (isScalar ? Token.sValue(x2) : "");

      float factor = (sValue.indexOf("{") >= 0 ? Float.NaN : isScalar ? Token
          .fValue(x2) : 0);

      String[] sList1 = (x1.value instanceof String ? TextFormat.split(
          (String) x1.value, "\n") : (String[]) x1.value);

      String[] sList2 = (isScalar ? null
          : x2.value instanceof String ? TextFormat.split((String) x2.value,
              "\n") : (String[]) x2.value);

      int len = (isScalar ? sList1.length : Math.min(sList1.length,
          sList2.length));

      String[] sList3 = new String[len];

      float[] list1 = new float[sList1.length];
      Parser.parseFloatArray(sList1, list1);

      float[] list2 = new float[(isScalar ? sList1.length : sList2.length)];
      if (isScalar)
        for (int i = len; --i >= 0;)
          list2[i] = factor;
      else
        Parser.parseFloatArray(sList2, list2);

      Token token = null;
      switch (tok) {
      case Token.add:
        token = Token.tokenPlus;
        break;
      case Token.sub:
        token = Token.tokenMinus;
        break;
      case Token.mul:
        token = Token.tokenTimes;
        break;
      case Token.div:
        token = Token.tokenDivide;
        break;
      }

      for (int i = 0; i < len; i++) {
        if (Float.isNaN(list1[i]))
          addX(Token.unescapePointOrBitsetAsToken(sList1[i]));
        else
          addX(list1[i]);
        if (!Float.isNaN(list2[i]))
          addX(list2[i]);
        else if (isScalar)
          addX(Token.unescapePointOrBitsetAsToken(sValue));
        else
          addX(Token.unescapePointOrBitsetAsToken(sList2[i]));
        if (!addOp(token) || !operate())
          return false;
        sList3[i] = Token.sValue(xStack[xPt--]);
      }
      /*
       switch (tok) {
       case Token.add:
       if (Float.isNaN(factor)) {
       for (int i = len; --i >= 0;)
       sList3[i] = sList1[i] + (isScalar ? sValue : sList2[i]);
       return addX(sList3);
       }
       for (int i = len; --i >= 0;)
       list1[i] += list2[i];
       break;
       case Token.sub:
       if (Float.isNaN(factor)) {
       for (int i = len; --i >= 0;)
       sList3[i] = (isScalar ? sValue : sList2[i]) + sList1[i];
       return addX(sList3);
       }
       for (int i = len; --i >= 0;)
       list1[i] -= list2[i];
       break;
       case Token.mul:
       for (int i = len; --i >= 0;)
       list1[i] *= list2[i];
       break;
       case Token.div:
       for (int i = len; --i >= 0;)
       if (list2[i] == 0)
       list1[i] = Float.NaN;
       else
       list1[i] /= list2[i];
       break;
       }
       for (int i = len; --i >= 0;)
       sList3[i] = "" + list1[i];
       */
      return addX(sList3);
    }

    private boolean evaluateArray(Token[] args) throws ScriptException {
      if (isSyntaxCheck)
        return addX("");
      int len = args.length;
      if (len == 0)
        len = 1;
      String[] array = new String[len];
      array[0] = "";
      for (int i = 0; i < args.length; i++)
        array[i] = Token.sValue(args[i]);
      return addX(array);
    }

    private boolean evaluateLoad(Token[] args) throws ScriptException {
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX("");
      return addX(viewer.getFileAsString(Token.sValue(args[0])));
    }

    private boolean evaluateScript(Token[] args, boolean isJavaScript)
        throws ScriptException {
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX("");
      String s = Token.sValue(args[0]);
      if (isJavaScript)
        return addX(viewer.eval(s));
      StringBuffer sb = new StringBuffer();
      runScript(s, sb);
      return addX(sb.toString());
    }

    private boolean evaluateData(Token[] args) throws ScriptException {
      if (args.length == 0 || args.length > 2)
        return false;
      if (isSyntaxCheck)
        return addX("");
      String selected = Token.sValue(args[0]);
      String type = (args.length == 2 ? Token.sValue(args[1]) : "");

      // parallel addition of float property data sets

      if (selected.indexOf("property_") == 0) {
        float[] f1 = viewer.getDataFloat(selected);
        if (f1 == null)
          return addX("");
        float[] f2 = (type.indexOf("property_") == 0 ? viewer
            .getDataFloat(type) : null);
        if (f2 != null)
          for (int i = Math.min(f1.length, f2.length); --i >= 0;)
            f1[i] += f2[i];
        return addX(Escape.escape(f1));
      }

      // some other data type -- just return it

      if (args.length == 1) {
        Object[] data = viewer.getData(selected);
        return addX(data == null ? "" : "" + data[1]);
      }
      // {selected atoms} XYZ, MOL, PDB file format 
      return addX(viewer.getData(selected, type));
    }

    private boolean evaluateLabel(Token[] args) throws ScriptException {
      Token x1 = getX();
      String format = (args.length == 0 ? "%U" : Token.sValue(args[0]));
      if (args.length > 1 || x1.tok != Token.bitset)
        return false;
      if (isSyntaxCheck)
        return addX("");
      return addX(getBitsetIdent(Token.bsSelect(x1), format, x1.value, true));
    }

    private boolean evaluateWithin(Token[] args) throws ScriptException {
      if (args.length < 1)
        return false;
      Object withinSpec = args[0].value;
      int tok = args[0].tok;
      String withinStr = "" + withinSpec;
      BitSet bs = new BitSet();
      float distance = 0;
      boolean isSequence = false;
      boolean isBoundbox = false;
      int i = args.length;
      boolean isWithinModelSet = false;
      boolean isDistance = (tok == Token.decimal || tok == Token.integer);
      if (withinSpec instanceof String) {
        isSequence = !Parser.isOneOf(withinStr,
            "element;site;group;chain;molecule;model;boundbox");
      } else if (isDistance) {
        distance = Token.fValue(args[0]);
        if (i < 2)
          return false;
        if (args[1].tok == Token.on || args[1].tok == Token.off) {
          isWithinModelSet = Token.bValue(args[1]);
          i = 0;
        }
      } else {
        return false;
      }

      if (i == 3) {
        withinStr = Token.sValue(args[1]);
        if (!Parser.isOneOf(withinStr, "on;off;plane;hkl;coord"))
          return false;
        // within (distance, true|false, [point or atom center] 
        // within (distance, plane|hkl,  [plane definition] )
        // within (distance, coord,  [point or atom center] )

      } else if (i == 1) {

        // within (boundbox)

        if (!withinStr.equals("boundbox"))
          return false;
        isBoundbox = true;
      }
      Point3f pt = null;
      Point4f plane = null;
      i = args.length - 1;
      if (args[i].value instanceof Point4f)
        plane = (Point4f) args[i].value;
      else if (args[i].value instanceof Point3f)
        pt = (Point3f) args[i].value;
      
      if (i > 0 && plane == null && pt == null && !(args[i].value instanceof BitSet))
        return false;
      if (isSyntaxCheck)
        return addX(bs);
      if (plane != null)
        return addX(viewer.getAtomsWithin(distance, plane));
      if (pt != null)
        return addX(viewer.getAtomsWithin(distance, pt));
      bs = (isBoundbox ? null : Token.bsSelect(args[i]));
      if (isDistance)
        return addX(viewer.getAtomsWithin(distance, bs, isWithinModelSet));
      if (isSequence)
        return addX(viewer.getAtomsWithin(Token.sequence, withinStr, bs));
      return addX(viewer.getAtomsWithin(Token.getTokenFromName(withinStr).tok,
          bs));
    }

    private boolean evaluateConnected(Token[] args) throws ScriptException {
      /*
       * Two options here:
       * 
       * connected(1, 3, "single", {carbon})
       * 
       * connected(1, 3, "partial 3.1", {carbon})
       * 
       *  means "atoms connected to carbon by from 1 to 3 single bonds"
       * 
       * connected(1.0, 1.5, "single", {carbon}, {oxygen})
       * 
       *  means "single bonds from 1.0 to 1.5 Angstroms between carbon and oxygen"
       * 
       * the first returns an atom bitset; the second returns a bond bitset.
       * 
       */
      float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
      float fmin = 0, fmax = Float.MAX_VALUE;

      short order = JmolConstants.BOND_ORDER_ANY;
      BitSet atoms1 = null;
      BitSet atoms2 = null;
      boolean haveDecimal = false;
      boolean isBonds = false;
      for (int i = 0; i < args.length; i++) {
        Token token = args[i];
        switch (token.tok) {
        case Token.bitset:
          isBonds = (token.value instanceof BondSet);
          if (isBonds && atoms1 != null)
            return false;
          if (atoms1 == null)
            atoms1 = Token.bsSelect(token);
          else if (atoms2 == null)
            atoms2 = Token.bsSelect(token);
          else
            return false;
          break;
        case Token.string:
          order = JmolConstants.getBondOrderFromString(Token.sValue(token));
          if (order == JmolConstants.BOND_ORDER_NULL)
            return false;
          break;
        case Token.decimal:
          haveDecimal = true;
        // fall through
        default:
          int n = Token.iValue(token);
          float f = Token.fValue(token);
          if (max != Integer.MAX_VALUE)
            return false;

          if (min == Integer.MIN_VALUE) {
            min = Math.max(n, 1);
            fmin = f;
          } else {
            max = n;
            fmax = f;
          }
        }
      }
      if (min == Integer.MIN_VALUE) {
        min = 1;
        max = 100;
        fmin = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
        fmax = JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE;
      } else if (max == Integer.MAX_VALUE) {
        max = min;
        fmax = fmin;
        fmin = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
      }
      if (atoms1 == null)
        atoms1 = viewer.getModelAtomBitSet(-1, true);
      if (haveDecimal && atoms2 == null)
        atoms2 = atoms1;
      if (atoms2 != null) {
        BitSet bsBonds = new BitSet();
        if (isSyntaxCheck)
          return addX(new Token(Token.bitset, new BondSet(bsBonds)));
        viewer.makeConnections(fmin, fmax, order,
            JmolConstants.CONNECT_IDENTIFY_ONLY, atoms1, atoms2, bsBonds,
            isBonds);
        return addX(new Token(Token.bitset, new BondSet(bsBonds, viewer
            .getAtomIndices(viewer.getAtomsWithin(Token.bonds, bsBonds)))));
      }
      if (isSyntaxCheck)
        return addX(atoms1);
      return addX(viewer.getAtomsConnected(min, max, order, atoms1));
    }

    private boolean evaluateSubstructure(Token[] args) throws ScriptException {
      if (args.length != 1)
        return false;
      BitSet bs = new BitSet();
      if (isSyntaxCheck)
        return addX(bs);
      String smiles = Token.sValue(args[0]);
      if (smiles.length() == 0)
        return false;

      try {
        SmilesMatcherInterface matcher = (SmilesMatcherInterface) Class
            .forName(JmolConstants.CLASSBASE_OPTIONS + "smiles.PatternMatcher")
            .newInstance();
        matcher.setViewer(viewer);
        bs = matcher.getSubstructureSet(smiles);
      } catch (Exception e) {
        evalError(e.getMessage());
      }
      return addX(bs);
    }

    private boolean operate() throws ScriptException {

      Token op = oStack[oPt--];
      if (oPt < 0 && op.tok == Token.opEQ && isAssignment && xPt == 2) {
        return true;
      }

      Token x2 = getX();
      if (x2 == Token.tokenArraySelector)
        return false;

      //unary:

      if (x2.tok == Token.list)
        x2 = Token.selectItem(x2);

      if (op.tok == Token.opNot)
        return (x2.tok == Token.bitset ? addX(BitSetUtil.copyInvert(Token
            .bsSelect(x2), (x2.value instanceof BondSet ? viewer
            .getBondCount() : viewer.getAtomCount()))) : addX(!Token
            .bValue(x2)));
      int iv = op.intValue & ~Token.minmaxmask;
      if (op.tok == Token.propselector) {
        switch (iv) {
        case Token.size:
          return addX(Token.sizeOf(x2));
        case Token.type:
          return addX(Token.typeOf(x2));
        case Token.lines:
          if (x2.tok != Token.string)
            return false;
          String s = (String) x2.value;
          s = TextFormat.simpleReplace(s, "\n\r", "\n").replace('\r', '\n');
          return addX(TextFormat.split(s, '\n'));
        case Token.color:
          switch (x2.tok) {
          case Token.string:
          case Token.list:
            Point3f pt = new Point3f();
            return addX(Graphics3D.colorPointFromString(Token.sValue(x2), pt));
          case Token.integer:
          case Token.decimal:
            return addX(viewer.getColorPointForPropertyValue(Token.fValue(x2)));
          case Token.point3f:
            return addX(Escape.escapeColor(colorPtToInt((Point3f) x2.value)));
          default:
          //handle bitset later
          }
          break;
        case Token.boundbox:
          return evaluateBoundBox(x2);
        }
        if (x2.tok == Token.string) {
          Object v = Token.unescapePointOrBitsetAsToken(Token.sValue(x2));
          if (!(v instanceof Token))
            return false;
          x2 = (Token) v;
        }
        return evaluatePointOrBitsetOperation(op, x2);
      }

      //binary:
      String s;
      Token x1 = getX();
      if (x1.tok == Token.list)
        x1 = Token.selectItem(x1);
      switch (op.tok) {
      case Token.opAnd:
        if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
          BitSet bs = Token.bsSelect(x1);
          bs.and(Token.bsSelect(x2));
          return addX(bs);
        }
        return addX(Token.bValue(x1) && Token.bValue(x2));
      case Token.opOr:
        if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
          BitSet bs = Token.bsSelect(x1);
          bs.or(Token.bsSelect(x2));
          return addX(bs);
        }
        return addX(Token.bValue(x1) || Token.bValue(x2));
      case Token.opXor:
        if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
          BitSet bs = Token.bsSelect(x1);
          bs.xor(Token.bsSelect(x2));
          return addX(bs);
        }
        boolean a = Token.bValue(x1);
        boolean b = Token.bValue(x2);
        return addX(a && !b || b && !a);
      case Token.opToggle:
        if (x1.tok != Token.bitset || x2.tok != Token.bitset)
          return false;
        return addX(BitSetUtil.toggleInPlace(Token.bsSelect(x1), Token
            .bsSelect(x2), viewer.getAtomCount()));
      case Token.opLE:
        return addX(Token.fValue(x1) <= Token.fValue(x2));
      case Token.opGE:
        return addX(Token.fValue(x1) >= Token.fValue(x2));
      case Token.opGT:
        return addX(Token.fValue(x1) > Token.fValue(x2));
      case Token.opLT:
        return addX(Token.fValue(x1) < Token.fValue(x2));
      case Token.opEQ:
        if (x1.tok == Token.string && x2.tok == Token.string)
          return addX(Token.sValue(x1).equalsIgnoreCase(Token.sValue(x2)));
        return addX(Token.fValue(x1) == Token.fValue(x2));
      case Token.opNE:
        if (x1.tok == Token.string && x2.tok == Token.string)
          return addX(!(Token.sValue(x1).equalsIgnoreCase(Token.sValue(x2))));
        return addX(Token.fValue(x1) != Token.fValue(x2));
      case Token.plus:
        if (x1.tok == Token.list || x2.tok == Token.list)
          return addX(Token.concatList(x1, x2));
        if (x1.tok == Token.string)
          return addX(Token.sValue(x1) + Token.sValue(x2));
        if (x1.tok == Token.string && x2.tok == Token.integer) {
          if ((s = (Token.sValue(x1)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addX(Token.iValue(x1) + x2.intValue);
        }
        if (x1.tok == Token.integer) {
          if (x2.tok == Token.string) {
            if ((s = (Token.sValue(x2)).trim()).indexOf(".") < 0
                && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
              return addX(x1.intValue + Token.iValue(x2));
          } else if (x2.tok != Token.decimal)
            return addX(x1.intValue + Token.iValue(x2));
        }
        if (x1.tok == Token.point3f) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.point3f:
            pt.add((Point3f) x2.value);
            return addX(pt);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x + f, pt.y + f, pt.z + f));
          }
        }
        return addX(Token.fValue(x1) + Token.fValue(x2));
      case Token.minus:
        if (x1.tok == Token.integer) {
          if (x2.tok == Token.string) {
            if ((s = (Token.sValue(x2)).trim()).indexOf(".") < 0
                && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
              return addX(x1.intValue - Token.iValue(x2));
          } else if (x2.tok != Token.decimal)
            return addX(x1.intValue - Token.iValue(x2));
        }
        if (x1.tok == Token.string && x2.tok == Token.integer) {
          if ((s = (Token.sValue(x1)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addX(Token.iValue(x1) - x2.intValue);
        }
        if (x1.tok == Token.integer && x2.tok == Token.integer)
          return addX(x1.intValue - Token.iValue(x2));
        if (x1.tok == Token.point3f) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.point3f:
            pt.sub((Point3f) x2.value);
            return addX(pt);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x - f, pt.y - f, pt.z - f));
          }
        }
        return addX(Token.fValue(x1) - Token.fValue(x2));
      case Token.unaryMinus:
        switch (x2.tok) {
        case Token.integer:
          return addX(-Token.iValue(x2));
        case Token.point3f:
          Point3f pt = new Point3f((Point3f) x2.value);
          pt.scale(-1f);
          return addX(pt);
        case Token.point4f:
          Point4f plane = new Point4f((Point4f) x2.value);
          plane.scale(-1f);
          return addX(plane);
        case Token.bitset:
          return addX(BitSetUtil.copyInvert(Token.bsSelect(x2), 
              (x2.value instanceof BondSet ? viewer.getBondCount() : viewer.getAtomCount())));
        }
        return addX(-Token.fValue(x2));
      case Token.times:
        if (x1.tok == Token.integer && x2.tok != Token.decimal)
          return addX(x1.intValue * Token.iValue(x2));
        if (x1.tok == Token.point3f) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.point3f:
            Point3f pt2 = ((Point3f) x2.value);
            return addX(pt.x * pt2.x + pt.y * pt2.y + pt.z * pt2.z);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x * f, pt.y * f, pt.z * f));
          }
        }
        return addX(Token.fValue(x1) * Token.fValue(x2));
      case Token.percent:
        // more than just modulus

        //  float % n     round to n digits; n = 0 does "nice" rounding
        //  String % -n    trim to width n; left justify
        //  String % n   trim to width n; right justify
        //  Point3f % n   ah... sets to multiple of unit cell!
        //  bitset % n  
        //  Point3f * Point3f  does dot product
        //  Point3f / Point3f  divides by magnitude
        //  float * Point3f gets magnitude

        s = null;
        int n = Token.iValue(x2);
        switch (x1.tok) {
        case Token.on:
        case Token.off:
        case Token.integer:
          if (n == 0)
            return addX((int) 0);
          return addX(Token.iValue(x1) % n);
        case Token.decimal:
          float f = Token.fValue(x1);
          //neg is scientific notation
          if (n == 0)
            return addX((int) (f + 0.5f * (f < 0 ? -1 : 1)));
          s = TextFormat.formatDecimal(f, n);
          return addX(s);
        case Token.string:
          s = (String) x1.value;
          if (n == 0)
            return addX(TextFormat.trim(s, "\n\t "));
          else if (n > 0)
            return addX(TextFormat.format(s, n, n, false, false));
          return addX(TextFormat.format(s, -n, n, true, false));
        case Token.list:
          String[] list = (String[]) x1.value;
          String[] listout = new String[list.length];
          for (int i = 0; i < list.length; i++) {
            if (n == 0)
              listout[i] = list[i].trim();
            else if (n > 0)
              listout[i] = TextFormat.format(list[i], n, n, true, false);
            else
              listout[i] = TextFormat.format(s, -n, n, false, false);
          }
          return addX(listout);
        case Token.point3f:
          Point3f pt = new Point3f((Point3f) x1.value);
          viewer.toUnitCell(pt, new Point3f(n, n, n));
          return addX(pt);
        case Token.bitset:
          return addX(Token.bsSelect(x1, n));
        }
      case Token.divide:
        if (x1.tok == Token.integer && x2.tok == Token.integer
            && x2.intValue != 0)
          return addX(x1.intValue / x2.intValue);
        if (x1.tok == Token.point3f) {
          Point3f pt = new Point3f((Point3f) x1.value);
          float f = Token.fValue(x2);
          if (f == 0)
            return addX(new Point3f(Float.NaN, Float.NaN, Float.NaN));
          return addX(new Point3f(pt.x / f, pt.y / f, pt.z / f));
        }
        float f1 = Token.fValue(x1);
        float f2 = Token.fValue(x2);
        if (f2 == 0)
          return addX(f1 == 0 ? 0f : f1 < 0 ? Float.POSITIVE_INFINITY
              : Float.POSITIVE_INFINITY);
        return addX(f1 / f2);
      }
      return true;
    }

    private boolean evaluateBoundBox(Token x2) throws ScriptException {
      if (x2.tok != Token.bitset)
        return false;
      if (isSyntaxCheck)
        return addX("");
      BoxInfo b = viewer.getBoxInfo(Token.bsSelect(x2));
      Point3f[] pts = b.getBoundBoxPoints();
      return addX(new String[] {Escape.escape(pts[0]), Escape.escape(pts[1]),
          Escape.escape(pts[2]), Escape.escape(pts[3])});
    }
    
    private boolean evaluatePointOrBitsetOperation(Token op, Token x2)
        throws ScriptException {
      switch (x2.tok) {
      case Token.list:
        String[] list = (String[]) x2.value;
        String[] list2 = new String[list.length];
        for (int i = 0; i < list.length; i++) {
          Object v = Token.unescapePointOrBitsetAsToken(list[i]);
          if (!(v instanceof Token)
              || !evaluatePointOrBitsetOperation(op, (Token) v))
            return false;
          list2[i] = Token.sValue(xStack[xPt--]);
        }
        return addX(list2);
      case Token.point3f:
        switch (op.intValue) {
        case Token.atomX:
          return addX(((Point3f) x2.value).x);
        case Token.atomY:
          return addX(((Point3f) x2.value).y);
        case Token.atomZ:
          return addX(((Point3f) x2.value).z);
        case Token.fracX:
        case Token.fracY:
        case Token.fracZ:
          Point3f ptf = new Point3f((Point3f) x2.value);
          viewer.toFractional(ptf);
          return addX(op.intValue == Token.fracX ? ptf.x
              : op.intValue == Token.fracY ? ptf.y : ptf.z);
        }
        break;
      case Token.bitset:
        if (op.intValue == Token.bonds && x2.value instanceof BondSet)
          return addX(x2);
        Object val = getBitsetProperty(Token.bsSelect(x2), op.intValue, null,
            null, x2.value, op.value, false);
        if (op.intValue == Token.bonds)
          return addX(new Token(Token.bitset, new BondSet((BitSet) val, viewer
              .getAtomIndices(Token.bsSelect(x2)))));
        return addX(val);
      }
      return false;
    }

    Point3f ptValue(Token x) throws ScriptException {
      if (isSyntaxCheck)
        return new Point3f();
      switch (x.tok) {
      case Token.point3f:
        return (Point3f) x.value;
      case Token.bitset:
        return (Point3f) getBitsetProperty(Token.bsSelect(x), Token.xyz, null,
            null, x.value, null, false);
      case Token.string:
      case Token.list:
        Object pt = Escape.unescapePoint(Token.sValue(x));
        if (pt instanceof Point3f)
          return (Point3f) pt;
        break;
      }
      float f = Token.fValue(x);
      return new Point3f(f, f, f);
    }

    Point4f planeValue(Token x) {
      if (isSyntaxCheck)
        return new Point4f();
      switch (x.tok) {
      case Token.point4f:
        return (Point4f) x.value;
      case Token.list:
      case Token.string:
        Object pt = Escape.unescapePoint(Token.sValue(x));
        return (pt instanceof Point4f ? (Point4f) pt : null);
      case Token.bitset:
        //ooooh, wouldn't THIS be nice!
        break;
      }
      return null;
    }

    void stackOverflow() throws ScriptException {
      evalError(GT._("too many parentheses"));
    }

  }
}
