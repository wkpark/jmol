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

import org.jmol.g3d.Graphics3D;
import org.jmol.g3d.Font3D;
import org.jmol.util.CommandHistory;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.util.Parser;

import java.io.*;
import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import org.jmol.i18n.*;

class Context {
  String filename;
  String script;
  short[] linenumbers;
  int[] lineIndices;
  Token[][] aatoken;
  Token[] statement;
  int statementLength;
  int pc;
  int pcEnd = Integer.MAX_VALUE;
  int lineEnd = Integer.MAX_VALUE;
  int iToken;
  int ifs[];
}

class BondSet extends BitSet {
  int[] associatedAtoms;

  BondSet() {
  }

  BondSet(BitSet bs) {
    for (int i = bs.size(); --i >= 0;)
      if (bs.get(i))
        set(i);
  }

  BondSet(BitSet bs, int[] atoms) {
    this(bs);
    associatedAtoms = atoms;
  }
}

class Eval { //implements Runnable {
  final static int scriptLevelMax = 10;
  final static int MAX_IF_DEPTH = 10; //should be plenty

  Compiler compiler;
  int scriptLevel;
  int scriptReportingLevel;
  Context[] stack = new Context[scriptLevelMax];
  String filename;
  String script;
  String thisCommand;
  short[] linenumbers;
  int[] lineIndices;
  Token[][] aatoken;
  int pc; // program counter
  int lineEnd;
  int pcEnd;
  long timeBeginExecution;
  long timeEndExecution;
  boolean error;
  String errorMessage;
  Token[] statement;
  int statementLength;
  Viewer viewer;
  BitSet bsSubset;
  int iToken;
  int[] ifs;
  boolean isSyntaxCheck, isScriptCheck;

  //Thread myThread;

  boolean tQuiet;
  boolean logMessages = false;
  boolean debugScript = false;
  boolean fileOpenCheck = true;

  Eval(Viewer viewer) {
    compiler = new Compiler(viewer);
    this.viewer = viewer;
    clearDefinitionsAndLoadPredefined();
  }

  final static String EXPRESSION_KEY = "e_x_p_r_e_s_s_i_o_n";

  /**
   * a general-use method to evaluate a "SET" type expression. 
   * @param viewer
   * @param expr
   * @return an object of one of the following types:
   *   Boolean, Integer, Float, String, Point3f, BitSet 
   */

  public static Object evaluateExpression(Viewer viewer, String expr) {
    // Text.formatText for MESSAGE and ECHO
    Eval e = new Eval(viewer);
    try {
      if (e.loadScript(null, EXPRESSION_KEY + " = " + expr)) {
        e.statement = e.aatoken[0];
        e.statementLength = e.statement.length;
        return e.parameterExpression(2, "");
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
      e.pushContext();
      if (e.loadScript(null, "select (" + atomExpression + ")")) {
        e.statement = e.aatoken[0];
        bs = e.expression(e.statement, 1, false, false, true);
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
        V.add(new Integer(i));
    return V;
  }

  void haltExecution() {
    resumePausedExecution();
    interruptExecution = Boolean.TRUE;
  }

  boolean isScriptExecuting() {
    return isExecuting && !interruptExecution.booleanValue();
  }

  static Boolean interruptExecution = Boolean.FALSE;
  static Boolean executionPaused = Boolean.FALSE;
  boolean isExecuting = false;

  Thread currentThread = null;

  public void runEval(boolean checkScriptOnly) {
    // only one reference now -- in Viewer
    //refresh();
    viewer.pushHoldRepaint();
    interruptExecution = Boolean.FALSE;
    executionPaused = Boolean.FALSE;
    isExecuting = true;
    currentThread = Thread.currentThread();
    isSyntaxCheck = isScriptCheck = checkScriptOnly;
    timeBeginExecution = System.currentTimeMillis();
    try {
      instructionDispatchLoop(false);
    } catch (ScriptException e) {
      error = true;
      setErrorMessage(e.toString());
      viewer.scriptStatus(errorMessage);
    }
    timeEndExecution = System.currentTimeMillis();

    if (errorMessage == null && interruptExecution.booleanValue())
      errorMessage = "execution interrupted";
    else if (!tQuiet && !isSyntaxCheck)
      viewer.scriptStatus("Script completed");
    isExecuting = isSyntaxCheck = isScriptCheck = false;
    viewer.setTainted(true);
    viewer.popHoldRepaint();

  }

  boolean hadRuntimeError() {
    return error;
  }

  String getErrorMessage() {
    return errorMessage;
  }

  void setErrorMessage(String err) {
    if (errorMessage == null) //there could be a compiler error from a script command
      errorMessage = "script ERROR: ";
    errorMessage += err;
  }

  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  private void runScript(String script) throws ScriptException {
    //load, restore
    pushContext();
    if (loadScript(null, script))
      instructionDispatchLoop(false);
    popContext();
  }

  void pushContext() throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      evalError(GT._("too many script levels"));
    Context context = new Context();
    context.filename = filename;
    context.script = script;
    context.linenumbers = linenumbers;
    context.lineIndices = lineIndices;
    context.aatoken = aatoken;
    context.statement = statement;
    context.statementLength = statementLength;
    context.pc = pc;
    context.lineEnd = lineEnd;
    context.pcEnd = pcEnd;
    context.iToken = iToken;
    context.ifs = ifs;
    stack[scriptLevel++] = context;
    if (isScriptCheck)
      Logger.info("-->>-------------".substring(0, scriptLevel + 5) + filename);
  }

  void popContext() {
    if (isScriptCheck)
      Logger.info("--<<-------------".substring(0, scriptLevel + 5) + filename);
    if (scriptLevel == 0)
      return;
    Context context = stack[--scriptLevel];
    stack[scriptLevel] = null;
    filename = context.filename;
    script = context.script;
    linenumbers = context.linenumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    statement = context.statement;
    statementLength = context.statementLength;
    pc = context.pc;
    lineEnd = context.lineEnd;
    pcEnd = context.pcEnd;
    iToken = context.iToken;
    ifs = context.ifs;
  }

  boolean loadScript(String filename, String script) {
    //use runScript, not loadScript from within Eval
    this.filename = filename;
    if (!compiler.compile(filename, script, false, false)) {
      error = true;
      errorMessage = compiler.getErrorMessage();
      return false;
    }
    this.script = compiler.getScript();
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    linenumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    return true;
  }

  Object checkScriptSilent(String script) {
    if (!compiler.compile(null, script, false, true))
      return compiler.getErrorMessage();
    isSyntaxCheck = true;
    isScriptCheck = false;
    errorMessage = null;
    this.script = compiler.getScript();
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    linenumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    try {
      instructionDispatchLoop(false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
    }
    isSyntaxCheck = false;
    if (errorMessage != null)
      return errorMessage;
    Vector info = new Vector();
    info.add(compiler.getScript());
    info.add(compiler.getAatokenCompiled());
    info.add(compiler.getLineNumbers());
    info.add(compiler.getLineIndices());
    return info;
  }

  void clearState(boolean tQuiet) {
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
    return loadScript(null, script);
  }

  boolean loadScriptFile(String filename, boolean tQuiet) {
    //from viewer
    clearState(tQuiet);
    return loadScriptFileInternal(filename);
  }

  private boolean loadScriptFileInternal(String filename) {
    //from "script" command, with push/pop surrounding or viewer
    if (filename.toLowerCase().indexOf("javascript:") == 0)
      return loadScript(filename, viewer.eval(filename.substring(11)));
    Object t = viewer.getInputStreamOrErrorMessageFromName(filename);
    if (!(t instanceof InputStream))
      return loadError((String) t);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        (InputStream) t));
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
    return loadScript(filename, script.toString());
  }

  boolean loadError(String msg) {
    error = true;
    errorMessage = msg;
    return false;
  }

  boolean fileNotFound(String filename) {
    return loadError("file not found:" + filename);
  }

  boolean ioError(String filename) {
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

  void clearDefinitionsAndLoadPredefined() {
    //executed each time a file is loaded; like clear() for the managers
    variables.clear();
    bsSubset = null;
    viewer.setSelectionSubset(null);
    if (viewer.getFrame() == null || viewer.getAtomCount() == 0)
      return;

    int cPredef = JmolConstants.predefinedSets.length;
    for (int iPredef = 0; iPredef < cPredef; iPredef++)
      predefine(JmolConstants.predefinedSets[iPredef]);
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

  void predefine(String script) {
    if (compiler.compile("#predefine", script, true, false)) {
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

  void setShapeProperty(int shapeType, String propertyName, Object propertyValue) {
    if (!isSyntaxCheck)
      viewer.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  void setShapeSize(int shapeType, int size) {
    if (!isSyntaxCheck)
      viewer.setShapeSize(shapeType, size);
  }

  void setBooleanProperty(String key, boolean value) {
    if (!isSyntaxCheck)
      viewer.setBooleanProperty(key, value);
  }

  void setIntProperty(String key, int value) {
    if (!isSyntaxCheck)
      viewer.setIntProperty(key, value);
  }

  void setFloatProperty(String key, float value) {
    if (!isSyntaxCheck)
      viewer.setFloatProperty(key, value);
  }

  void setStringProperty(String key, String value) {
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

  boolean checkContinue() {
    if (!interruptExecution.booleanValue()) {
      if (!executionPaused.booleanValue())
        return true;
      Logger.debug("script execution paused at this command: " + thisCommand);
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
              viewer.scriptStatus(errorMessage);
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

  int commandHistoryLevelMax = 0;

  void fixVariables() throws ScriptException {
    Token[] fixed;
    int i;
    for (i = 1; i < statementLength; i++)
      if (statement[i].tok == Token.define)
        break;
    if (i == statementLength)
      return;
    fixed = new Token[statementLength];
    fixed[0] = statement[0];
    int j = 1;
    for (i = 1; i < statementLength; i++) {
      if (statement[i].tok == Token.define) {
        String var = parameterAsString(++i);
        Object v = viewer.getParameter(var);
        if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          fixed[j] = new Token(Token.integer, ((Integer) v).intValue(), v);
        } else if (v instanceof Float) {
          fixed[j] = new Token(Token.decimal, Compiler.modelValue("" + v), v);
        } else if (v instanceof String && ((String) v).length() > 0) {
          v = getStringObjectAsToken((String) v);
          if (v instanceof Token)
            fixed[j] = (Token) v;
          else
            fixed[j] = new Token(Token.identifier, (String) v);
        } else {
          Point3f center = getDrawObjectCenter(var);
          if (center == null)
            invalidArgument();
          fixed[j] = new Token(Token.point3f, center);
        }
        if (j == 1 && statement[0].tok == Token.set
            && fixed[j].tok != Token.identifier)
          invalidArgument();
      } else {
        fixed[j] = statement[i];
      }
      j++;
    }
    statement = fixed;
    statementLength = j;
  }

  Object getStringObjectAsToken(String s) {
    Object v = s;
    if (s == null || s.length() == 0)
      return s;
    if (s.charAt(0) == '{')
      v = StateManager.unescapePoint(s);
    else if (s.indexOf("({") == 0)
      v = StateManager.unescapeBitset(s);
    else if (s.indexOf("[{") == 0)
      return new Token(Token.bitset,
          new BondSet(StateManager.unescapeBitset(s)));
    if (v instanceof Point3f)
      return new Token(Token.point3f, v);
    else if (v instanceof Point4f)
      return new Token(Token.point4f, v);
    else if (v instanceof BitSet)
      return new Token(Token.bitset, v);
    return v;
  }

  void instructionDispatchLoop(boolean doList) throws ScriptException {
    long timeBegin = 0;
    int ifLevel = 0;
    ifs = new int[MAX_IF_DEPTH + 1];
    ifs[0] = 0;
    debugScript = (!isSyntaxCheck && viewer.getDebugScript());
    logMessages = (debugScript && Logger.isActiveLevel(Logger.LEVEL_DEBUG));
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      viewer.scriptStatus(toString());
    }
    if (!isSyntaxCheck && scriptLevel <= commandHistoryLevelMax)
      viewer.addCommand(script);
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (!checkContinue())
        break;
      Token token = aatoken[pc][0];
      statement = aatoken[pc];
      statementLength = statement.length;
      fixVariables();
      if (linenumbers[pc] > lineEnd)
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
          viewer.scriptEcho("$[" + scriptLevel + "." + linenumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (isSyntaxCheck) {
        if (isScriptCheck)
          Logger.info(thisCommand);
        if (statementLength == 1)
          //            && !Compiler.tokAttr(token.tok, Token.unimplemented))
          continue;
      } else {
        if (debugScript)
          logDebugScript();
        if (logMessages)
          Logger.debug(token.toString());
        if (ifLevel > 0 && ifs[ifLevel] < 0 && token.tok != Token.endifcmd
            && token.tok != Token.ifcmd && token.tok != Token.elsecmd)
          continue;
      }
      switch (token.tok) {
      case Token.ifcmd:
        for (int i = 1; i <= ifLevel; i++)
          if (ifs[ifLevel] == pc || ifs[ifLevel] == -1 - pc) {
            ifLevel = i - 1;
            break;
          }
        if (++ifLevel == MAX_IF_DEPTH)
          evalError(GT._("Too many nested {0} commands", "IF"));
        ifs[ifLevel] = (ifs[ifLevel - 1] >= 0 && ifCmd() ? pc : -1 - pc);
        break;
      case Token.elsecmd:
        if (ifLevel < 1)
          evalError(GT._("Invalid {0} command", "ELSE"));
        if (!isSyntaxCheck)
          ifs[ifLevel] = -1 - ifs[ifLevel];
        break;
      case Token.endifcmd:
        if (--ifLevel < 0)
          evalError(GT._("Invalid {0} command", "ENDIF"));
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
        script();
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
        display();
        break;
      case Token.hide:
        hide();
        break;
      case Token.restrict:
        restrict();
        break;
      case Token.subset:
        subset();
        break;
      case Token.selectionHalo:
        setSelectionHalo(1);
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
        dots(1, Dots.DOTS_MODE_DOTS);
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
        font();
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
        dots(1, Dots.DOTS_MODE_SURFACE);
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
        mo();
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
        setAxes(1);
        break;
      case Token.boundbox:
        setBoundbox(1);
        break;
      case Token.unitcell:
        setUnitcell(1);
        break;
      case Token.frank:
        setFrank(1);
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
      case Token.write:
        write();
        break;
      case Token.pause: //resume is done differently
        pause();
        break;
      default:
        unrecognizedCommand();
      }
    }
  }

  boolean ifCmd() throws ScriptException {
    return ((Boolean) parameterExpression(1, null)).booleanValue();
  }

  int getLinenumber() {
    return linenumbers[pc];
  }

  String getCommand() {
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

  final StringBuffer strbufLog = new StringBuffer(80);

  void logDebugScript() {
    strbufLog.setLength(0);
    if (logMessages) {
      Logger.debug(statement[0].toString());
      for (int i = 1; i < statementLength; ++i)
        Logger.debug(statement[i].toString());
    }
    iToken = -2;
    strbufLog.append(statementAsString());
    viewer.scriptStatus(strbufLog.toString());
  }

  /* ****************************************************************************
   * ==============================================================
   * expression processing
   * ==============================================================
   */

  Token[] tempStatement;
  boolean isBondSet;
  Token expressionResult;

  BitSet expression(int index) throws ScriptException {
    if (!checkToken(index))
      badArgumentCount();
    return expression(statement, index, true, false, true);
  }

  BitSet expression(Token[] code, int pcStart, boolean allowRefresh,
                    boolean allowUnderflow, boolean bsRequired)
      throws ScriptException {
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
    Rpn rpn = new Rpn(10);
    Object val;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    if (ignoreSubset)
      pcStart = -pcStart;
    int pcStop = pcStart + 1;
    //    if (logMessages)
    //    viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      Token instruction = code[pc];
      Object value = code[pc].value;
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
        val = viewer.getParameter((String) value);
        if (val instanceof String)
          val = getStringObjectAsToken("" + val);
        if (val instanceof String)
          val = lookupIdentifierValue((String) value);
        rpn.addX(val);
        break;
      case Token.string:
        val = (String) value;
        rpn.addX(instruction);
        if (val.equals("plane")) {
          rpn.addX(new Token(Token.point4f, planeParameter(pc + 2)));
          pc = iToken;
          break;
        } else if (val.equals("hkl")) {
          rpn.addX(new Token(Token.point4f, hklParameter(pc + 2)));
          pc = iToken;
          break;
        } else if (val.equals("coord")) {
          rpn.addX(getPoint3f(pc + 2, true));
          pc = iToken;
          break;
        }
        break;
      case Token.within:
      case Token.substructure:
      case Token.connected:
      case Token.comma:
        rpn.addOp(instruction);
        break;
      case Token.all:
        rpn.addX(bsAll());
        break;
      case Token.none:
        rpn.addX(new BitSet());
        break;
      case Token.selected:
        rpn.addX(copyBitSet(viewer.getSelectionSet()));
        break;
      case Token.subset:
        rpn.addX(copyBitSet(bsSubset == null ? bsAll() : bsSubset));
        break;
      case Token.hidden:
        rpn.addX(copyBitSet(viewer.getHiddenSet()));
        break;
      case Token.displayed:
        rpn.addX(invertBitSet(viewer.getHiddenSet()));
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
        rpn.addX(getAtomBits((String) value));
        break;
      case Token.spec_atom:
        rpn.addX(viewer.getAtomBits("SpecAtom", (String) value));
        break;
      case Token.spec_name_pattern:
        rpn.addX(viewer.getAtomBits("SpecName", (String) value));
        break;
      case Token.spec_alternate:
        rpn.addX(getAtomBits("SpecAlternate", (String) value));
        break;
      case Token.spec_model:
      //1002 is equivalent to 1.2 when more than one file is present
      case Token.spec_model2:
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE) {
          iModel = ((Integer) value).intValue();
          if (!viewer.haveFileSet()) {
            rpn.addX(getAtomBits("SpecModel", iModel));
            break;
          }
          if (iModel < 1000)
            iModel = iModel * 1000000;
          else
            iModel = (iModel / 1000) * 1000000 + iModel % 1000;
        }
        rpn.addX(bitSetForModelNumberSet(new int[] { iModel }, 1));
        break;
      case Token.spec_resid:
        rpn.addX(getAtomBits("SpecResid", instruction.intValue));
        break;
      case Token.spec_chain:
        rpn.addX(getAtomBits("SpecChain", instruction.intValue));
        break;
      case Token.spec_seqcode:
        if (isInMath) {
          rpn.addX(instruction.intValue);
          break;
        }
        rpn.addX(getAtomBits("SpecSeqcode", getSeqCode(instruction)));
        break;
      case Token.spec_seqcode_range:
        if (isInMath) {
          rpn.addX(instruction.intValue);
          rpn.addX(Token.tokenMinus);
          rpn.addX(code[++pc].intValue);
          break;
        }
        rpn.addX(getAtomBits("SpecSeqcodeRange", new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]) }));
        break;
      case Token.cell:
        Point3f pt = (Point3f) value;
        rpn.addX(getAtomBits("Cell", new int[] { (int) (pt.x * 1000),
            (int) (pt.y * 1000), (int) (pt.z * 1000) }));
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
        String property = (tokWhat == Token.property ? (String)val : null);
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
        boolean isIdentifier = (tokValue == Token.identifier);
        float comparisonFloat = Float.NaN;
        if (isIdentifier) {
          val = viewer.getParameter((String) val);
          if (val instanceof Integer)
            comparisonFloat = comparisonValue = ((Integer) val).intValue();
          else if (val instanceof Float && isModel)
            comparisonValue = Mmset.modelFileNumberFromFloat(((Float) val)
                .floatValue());
        }
        if (val instanceof Integer || tokValue == Token.integer) {
          comparisonValue *= (Compiler
              .tokAttr(tokWhat, Token.atompropertyfloat) ? 100 : 1);
          comparisonFloat = comparisonValue;
          if (isModel) {
            if (comparisonValue > 1000 && comparisonValue < 1000000
                && viewer.haveFileSet()) {
              comparisonValue = (comparisonValue / 1000) * 1000000
                  + comparisonValue % 1000;
            }
          }
        } else if (val instanceof Float) {
          if (isModel) {
            tokWhat = -tokWhat;
          } else {
            comparisonFloat =((Float) val).floatValue() ;
            comparisonValue = (int) (comparisonFloat * (isRadius ? 250f
                : 100f));
          }
        } else
          invalidArgument();
        if (((String) value).indexOf("-") >= 0)
          comparisonValue = -comparisonValue;
        float[] data = (tokWhat == Token.property ? Viewer.getDataFloat(property) : null);
        rpn.addX(comparatorInstruction(instruction, tokWhat, data, tokOperator,
            comparisonValue, comparisonFloat));
        break;
      case Token.bitset:
      case Token.decimal:
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
    expressionResult = rpn.getResult(allowUnderflow);
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!isSyntaxCheck)
        rpn.dumpStacks();
      endOfStatementUnexpected();
    }
    if (!bsRequired && !(expressionResult.value instanceof BitSet))
      return null;
    BitSet bs = (expressionResult.value instanceof BitSet ? (BitSet) expressionResult.value
        : new BitSet());
    isBondSet = (expressionResult.value instanceof BondSet);
    if (!ignoreSubset && bsSubset != null && !isBondSet)
      bs.and(bsSubset);
    if (tempStatement != null) {
      statement = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  static int getSeqCode(Token instruction) {
    return (instruction.intValue != Integer.MAX_VALUE ? Group.getSeqcode(Math
        .abs(instruction.intValue), ' ') : ((Integer) instruction.value)
        .intValue());
  }

  BitSet toggle(BitSet A, BitSet B) {
    for (int i = viewer.getAtomCount(); --i >= 0;) {
      if (!B.get(i))
        continue;
      if (A.get(i)) { //both set --> clear A
        A.clear(i);
      } else {
        A.or(B); //A is not set --> return all on
        break;
      }
    }
    return A;
  }

  BitSet notSet(BitSet bs) {
    for (int i = viewer.getAtomCount(); --i >= 0;) {
      if (bs.get(i))
        bs.clear(i);
      else
        bs.set(i);
    }
    return bs;
  }

  BitSet lookupIdentifierValue(String identifier) throws ScriptException {
    // all variables and possible residue names for PDB
    // or atom names for non-pdb atoms are processed here.

    // priority is given to a defined variable.

    BitSet bs = lookupValue(identifier, false);
    if (bs != null)
      return copyBitSet(bs);

    // next we look for names of groups (PDB) or atoms (non-PDB)
    bs = getAtomBits("IdentifierOrNull", identifier);
    return (bs == null ? new BitSet() : bs);
  }

  BitSet getAtomBits(String setType) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(setType);
  }

  BitSet getAtomBits(String setType, String specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(setType, specInfo);
  }

  BitSet getAtomBits(String setType, int specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(setType, specInfo);
  }

  BitSet getAtomBits(String setType, int[] specInfo) {
    if (isSyntaxCheck)
      return new BitSet();
    return viewer.getAtomBits(setType, specInfo);
  }

  BitSet lookupValue(String variable, boolean plurals) throws ScriptException {
    if (isSyntaxCheck)
      return new BitSet();
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
        pushContext();
        value = expression((Token[]) value, -2, true, false, true);
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

  BitSet comparatorInstruction(Token token, int tokWhat, float[] data, int tokOperator,
                               int comparisonValue, float comparisonFloat) throws ScriptException {
    BitSet bs = new BitSet();
    int propertyValue = Integer.MAX_VALUE;
    BitSet propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    int imax = 0;
    int imin = 0;
    float propertyFloat = 0;
    Frame frame = viewer.getFrame();
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = frame.getAtomAt(i);
      switch (tokWhat) {
      default:
        propertyValue = (int) atomProperty(frame, atom, tokWhat, true);
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
        if (bitsetBaseValue >= 1000) {
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555)
           * for this the comparision is only with the
           * translational component; the symop itself must match
           * thus: 
           * select symop!=1655 selects all symop=1 and translation !=655
           * select symo >=2555 selects all symop=2 and translation >555
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
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0 || !(match = propertyBitSet.get(symop)))
            continue;
          bitsetComparator = Token.none;
          propertyValue = atom.getSymmetryTranslation(symop);
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

  float atomProperty(Frame frame, Atom atom, int tokWhat, boolean asInt)
      throws ScriptException {
    float propertyValue = 0;
    switch (tokWhat) {
    case Token.atomno:
      return atom.getAtomNumber();
    case Token.atomIndex:
      return atom.atomIndex;
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
      return (propertyValue < 0 ? Integer.MAX_VALUE : asInt ? propertyValue : propertyValue / 100f);
    case Token.surfacedistance:
      if (frame.getSurfaceDistanceMax() == 0)
        dots(statementLength, Dots.DOTS_MODE_CALCONLY);
      propertyValue = atom.getSurfaceDistance100();
      return (propertyValue < 0 ? Integer.MAX_VALUE : asInt ? propertyValue : propertyValue / 100f);
    case Token.occupancy:
      return atom.getOccupancy();
    case Token.polymerLength:
      return atom.getPolymerLength();
    case Token.resno:
      propertyValue = atom.getResno();
      return (propertyValue == -1 ? Integer.MAX_VALUE : propertyValue);
    case Token.groupID:
      propertyValue = atom.getGroupID();
      return (propertyValue < 0 ? Integer.MAX_VALUE : propertyValue);
    case Token.atomID:
      return atom.getSpecialAtomID();
    case Token.structure:
      return atom.getProteinStructureType();
    case Token.radius:
      return atom.getRasMolRadius();
    case Token.psi:
      propertyValue = atom.getGroupPsi();
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.phi:
      propertyValue = atom.getGroupPhi();
      return asInt ? propertyValue * 100 : propertyValue;
    case Token.bondcount:
      return atom.getCovalentBondCount();
    case Token.file:
      return atom.getModelFileIndex() + 1;
    case Token.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return atom.getModelNumber() % 1000000;
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

  void checkStatementLength(int length) throws ScriptException {
    iToken = statementLength;
    if (statementLength != length)
      badArgumentCount();
  }

  void checkLength34() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 3 || statementLength > 4)
      badArgumentCount();
  }

  int checkLength23() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 2 || statementLength > 3)
      badArgumentCount();
    return statementLength;
  }

  void checkLength2() throws ScriptException {
    checkStatementLength(2);
  }

  void checkLength3() throws ScriptException {
    checkStatementLength(3);
  }

  void checkLength4() throws ScriptException {
    checkStatementLength(4);
  }

  String optParameterAsString(int i) throws ScriptException {
    if (i >= statementLength)
      return "";
    return parameterAsString(i);
  }

  String parameterAsString(int i) throws ScriptException {
    getToken(i);
    if (theToken == null)
      endOfStatementUnexpected();
    return (theTok == Token.integer ? "" + theToken.intValue : ""
        + theToken.value);
  }

  int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.integer)
        return theToken.intValue;
    return integerExpected();
  }

  boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case Token.integer:
    case Token.decimal:
      return true;
    }
    return false;
  }

  float floatParameter(int index) throws ScriptException {
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

  int floatParameterSet(int i, float[] fparams) throws ScriptException {
    if (tokAt(i) == Token.leftbrace)
      i++;
    for (int j = 0; j < fparams.length; j++)
      fparams[j] = floatParameter(i++);
    if (tokAt(i) == Token.rightbrace)
      i++;
    return i;
  }

  String stringParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.string)
        return (String) theToken.value;
    return stringExpected();
  }

  String objectNameParameter(int index) throws ScriptException {
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
  float radiusParameter(int index, float defaultValue) throws ScriptException {
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

  int setShapeByNameParameter(int index) throws ScriptException {
    String objectName = objectNameParameter(index);
    int shapeType = viewer.getShapeIdFromObjectName(objectName);
    if (!isSyntaxCheck && shapeType < 0)
      objectNameExpected();
    setShapeProperty(shapeType, "thisID", objectName);
    return shapeType;
  }

  float getRasmolAngstroms(int i) throws ScriptException {
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case Token.integer:
        return intParameter(i) / 250f;
      case Token.decimal:
        return floatParameter(i);
      }
    }
    return numberExpected();
  }

  boolean booleanParameter(int i) throws ScriptException {
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

  boolean isAtomCenterOrCoordinateNext(int i) {
    int tok = tokAt(i);
    return (tok == Token.leftbrace || tok == Token.expressionBegin
        || tok == Token.point3f || tok == Token.bitset);
  }

  Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case Token.bitset:
    case Token.expressionBegin:
      BitSet bs = expression(statement, i, true, false, false);
      if (bs != null)
        return viewer.getAtomSetCenter(bs);
      if (expressionResult.value instanceof Point3f)
        return (Point3f) expressionResult.value;
      invalidArgument();
    case Token.leftbrace:
    case Token.point3f:
      return getPoint3f(i, true);
    }
    invalidArgument();
    //impossible return
    return null;
  }

  boolean isCenterParameter(int i) {
    switch (tokAt(i)) {
    case Token.dollarsign:
    case Token.bitset:
    case Token.expressionBegin:
    case Token.leftbrace:
    case Token.point3f:
      return true;
    }
    return false;
  }

  Point3f centerParameter(int i) throws ScriptException {
    Point3f center = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case Token.dollarsign:
        String id = objectNameParameter(++i);
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

  Point4f planeParameter(int i) throws ScriptException {
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
        Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: " + p);
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

  Point4f hklParameter(int i) throws ScriptException {
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
    Logger.info("defined plane: " + p);
    return p;
  }

  short getMadParameter() throws ScriptException {
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

  short getMadInteger(int radiusRasMol) throws ScriptException {
    //interesting question here about negatives... what if?
    if (radiusRasMol < 0 || radiusRasMol > 750)
      numberOutOfRange(0, 750);
    return (short) (radiusRasMol * 4 * 2);
  }

  short getMadFloat(float angstroms) throws ScriptException {
    if (angstroms < 0 || angstroms > 3)
      numberOutOfRange(0f, 3f);
    return (short) (angstroms * 1000 * 2);
  }

  short getSetAxesTypeMad(int index) throws ScriptException {
    checkStatementLength(index + 1);
    short mad = 0;
    switch (getToken(index).tok) {
    case Token.on:
      mad = 1;
    case Token.off:
      break;
    case Token.integer:
      int diameterPixels = intParameter(index);
      if (diameterPixels < -1 || diameterPixels >= 20)
        numberOutOfRange(-1, 19);
      mad = (short) diameterPixels;
      break;
    case Token.decimal:
      float angstroms = floatParameter(index);
      if (angstroms < 0 || angstroms >= 2)
        numberOutOfRange(0.01f, 1.99f);
      mad = (short) (angstroms * 1000 * 2);
      break;
    case Token.dotted:
      mad = -1;
      break;
    default:
      booleanOrNumberExpected("DOTTED");
    }
    return mad;
  }

  static BitSet copyBitSet(BitSet bitSet) {
    BitSet copy = new BitSet();
    copy.or(bitSet);
    return copy;
  }

  private BitSet invertBitSet(BitSet bitSet) {
    BitSet copy = bsAll();
    copy.andNot(bitSet);
    return copy;
  }

  boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == Token.colorRGB || tok == Token.leftsquare);
  }

  int getArgbParam(int index) throws ScriptException {
    return getArgbParam(index, false);
  }

  int getArgbParamLast(int index, boolean allowNone) throws ScriptException {
    int icolor = getArgbParam(index, allowNone);
    checkStatementLength(iToken + 1);
    return icolor;
  }

  int getArgbParam(int index, boolean allowNone) throws ScriptException {
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      case Token.string:
        return Graphics3D.getArgbFromString(parameterAsString(index));
      case Token.leftsquare:
        return getColorTriad(++index);
      case Token.colorRGB:
        return theToken.intValue;
      case Token.none:
        if (allowNone)
          return 0;
      }
    }
    colorExpected();
    //impossible return
    return 0;
  }

  int getArgbOrPaletteParam(int index) throws ScriptException {
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

  int getColorTriad(int i) throws ScriptException {
    int[] colors = new int[3];
    int n = 0;
    getToken(i);
    switch (theTok) {
    case Token.integer:
      for (; i < statementLength; i++) {
        getToken(i);
        switch (theTok) {
        case Token.comma:
          continue;
        case Token.integer:
          if (n > 2)
            badRGBColor();
          colors[n++] = theToken.intValue;
          continue;
        case Token.spec_seqcode:
          if (n > 2)
            badRGBColor();
          colors[n++] = ((Integer) theToken.value).intValue();
          continue;
        case Token.rightsquare:
          if (n == 3)
            return 0xFF000000 | colors[0] << 16 | colors[1] << 8 | colors[2];
        default:
          badRGBColor();
        }
      }
      badRGBColor();
    case Token.point3f:
      Point3f pt = (Point3f) theToken.value;
      if (getToken(++i).tok != Token.rightsquare)
        badRGBColor();
      return 0xFF000000 | (((int) pt.x) << 16) | (((int) pt.y) << 8)
          | ((int) pt.z);
    case Token.identifier:
      String hex = parameterAsString(i);
      if (getToken(++i).tok == Token.rightsquare && hex.length() == 7
          && hex.charAt(0) == 'x')
        try {
          return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
        } catch (NumberFormatException e) {
          badRGBColor();
        }
    }
    badRGBColor();
    // impossible return
    return 0;
  }

  boolean coordinatesAreFractional;

  boolean isPoint3f(int i) {
    ignoreError = true;
    boolean isOK = true;
    try {
      getPoint3f(i, true);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    return isOK;
  }

  Point3f getPoint3f(int i, boolean allowFractional) throws ScriptException {
    return (Point3f) getPointOrPlane(i, false, allowFractional, true, false, 3,
        3);
  }

  Point4f getPoint4f(int i) throws ScriptException {
    return (Point4f) getPointOrPlane(i, false, false, false, false, 4, 4);
  }

  Object getPointOrPlane(int index, boolean integerOnly,
                         boolean allowFractional, boolean doConvert,
                         boolean implicitFractional, int minDim, int maxDim)
      throws ScriptException {
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
    out: for (int i = index; i < statement.length; i++) {
      switch (getToken(i).tok) {
      case Token.leftbrace:
      case Token.comma:
      // case Token.opOr:
      case Token.opAnd:
        break;
      case Token.rightbrace:
        break out;
      case Token.integer:
      case Token.spec_seqcode_range:
      case Token.spec_seqcode:
        if (n == 6)
          invalidArgument();
        coord[n++] = theToken.intValue;
        break;
      case Token.slash:
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
      //System.out.println("getPointOrPlane:" + pt);
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how to convert?)
        invalidArgument();
      Point4f plane = new Point4f(coord[0], coord[1], coord[2], coord[3]);
      //System.out.println("getPointOrPlane:" + plane);
      return plane;
    }
    return coord;
  }

  int theTok;
  Token theToken;

  Token getToken(int i) throws ScriptException {
    if (!checkToken(i))
      endOfStatementUnexpected();
    theToken = statement[i];
    theTok = theToken.tok;
    return theToken;
  }

  int tokAt(int i) {
    return (i < statementLength ? statement[i].tok : Token.nada);
  }

  boolean checkToken(int i) {
    return (iToken = i) < statementLength;
  }

  /* ****************************************************************************
   * ==============================================================
   * command implementations
   * ==============================================================
   */

  void help() throws ScriptException {
    if (!viewer.isApplet())
      return;
    String what = (statementLength == 1 ? "" : parameterAsString(1));
    if (!isSyntaxCheck)
      viewer.getHelp(what);
  }

  void moveto() throws ScriptException {
    //moveto time
    //moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius (navCenter) xNav yNav navDepth    
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
    float zoom = 100;
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
      break;
    case Token.top:
      pt.set(1, 0, 0);
      i++;
      break;
    case Token.bottom:
      pt.set(-1, 0, 0);
      i++;
      break;
    default:
      //X Y Z deg
      pt = new Point3f(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }
    //zoom xTrans yTrans (center) rotationRadius 
    if (i != statementLength && !isAtomCenterOrCoordinateNext(i))
      zoom = floatParameter(i++);
    if (i != statementLength && !isAtomCenterOrCoordinateNext(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
    }
    float rotationRadius = 0;
    if (i != statementLength) {
      center = atomCenterOrCoordinateParameter(i);
      i = iToken + 1;
      if (i != statementLength && !isAtomCenterOrCoordinateNext(i))
        rotationRadius = floatParameter(i++);
    }
    // (navCenter) xNav yNav navDepth 

    Point3f navCenter = null;
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;

    if (i != statementLength) {
      navCenter = atomCenterOrCoordinateParameter(i);
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

    if (!isSyntaxCheck) {
      if (floatSecondsTotal > 0)
        refresh();
      viewer.moveTo(floatSecondsTotal, center, pt, degrees, zoom, xTrans,
          yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    }
  }

  void navigate() throws ScriptException {
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
      case Token.slash:
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
            v.add(centerParameter(++i));
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

  void bondorder() throws ScriptException {
    short order = 0;
    switch (getToken(1).tok) {
    case Token.integer:
      order = (short) intParameter(1);
      if (order < 0 || order > 3)
        invalidArgument();
      break;
    case Token.hbond:
      order = JmolConstants.BOND_H_REGULAR;
      break;
    case Token.decimal:
      float f = floatParameter(1);
      if (f == (short) f) {
        order = (short) f;
        if (order < 0 || order > 3)
          invalidArgument();
      } else if (f == 0.5f)
        order = JmolConstants.BOND_H_REGULAR;
      else if (f == 1.5f)
        order = JmolConstants.BOND_AROMATIC;
      else
        invalidArgument();
      break;
    case Token.identifier:
      order = JmolConstants.getBondOrderFromString(parameterAsString(1));
      if (order >= 1)
        break;
    // fall into
    default:
      invalidArgument();
    }
    if (!isSyntaxCheck)
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "bondOrder",
          new Short(order), viewer.getSelectedAtomsOrBonds());
  }

  void console() throws ScriptException {
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

  void centerAt() throws ScriptException {
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

  void stereo() throws ScriptException {
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
      case Token.leftsquare:
      case Token.colorRGB:
        if (colorpt > 1)
          badArgumentCount();
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt++] = getArgbParam(i);
        i = iToken;
        if (colorpt == 1)
          colors[colorpt] = ~colors[0];
        state += " " + StateManager.escapeColor(colors[colorpt - 1]);
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

  void connect() throws ScriptException {

    final float[] distances = new float[2];
    BitSet[] atomSets = new BitSet[2];
    atomSets[0] = atomSets[1] = viewer.getSelectionSet();
    float radius = Float.NaN;
    int color = Integer.MIN_VALUE;
    int distanceCount = 0;
    int atomSetCount = 0;
    short bondOrder = JmolConstants.BOND_ORDER_NULL;
    int operation = JmolConstants.CONNECT_MODIFY_OR_CREATE;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    boolean isTranslucentOrOpaque = false;
    String translucency = null;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BitSet bsBonds = new BitSet();
    boolean isBonds = false;
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
      switch_tag: switch (getToken(i).tok) {
      case Token.on:
      case Token.off:
        checkLength2();
        if (!isSyntaxCheck)
          viewer.rebond();
        return;
      case Token.integer:
      case Token.decimal:
        if (++nDistances > 2)
          badArgumentCount();
        if (nAtomSets > 0 || haveType || isColorOrRadius)
          invalidParameterOrder();
        distances[distanceCount++] = floatParameter(i);
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (++nAtomSets > 2 || isBonds && nAtomSets > 1)
          badArgumentCount();
        if (haveType || isColorOrRadius)
          invalidParameterOrder();
        atomSets[atomSetCount++] = expression(i);
        isBonds = isBondSet;
        i = iToken; // the for loop will increment i
        break;
      case Token.identifier:
      case Token.hbond:
        String cmd = parameterAsString(i);
        for (int j = JmolConstants.bondOrderNames.length; --j >= 0;) {
          if (cmd.equalsIgnoreCase(JmolConstants.bondOrderNames[j])) {
            if (haveType)
              incompatibleArguments();
            cmd = JmolConstants.bondOrderNames[j];
            bondOrder = JmolConstants.getBondOrderFromString(cmd);
            haveType = true;
            break switch_tag;
          }
        }
        if (++i != statementLength)
          invalidParameterOrder();
        if ((operation = JmolConstants.connectOperationFromString(cmd)) < 0)
          invalidArgument();
        haveOperation = true;
        break;
      case Token.translucent:
      case Token.opaque:
        if (isTranslucentOrOpaque)
          invalidArgument();
        isColorOrRadius = isTranslucentOrOpaque = true;
        translucency = parameterAsString(i);
        break;
      case Token.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case Token.leftsquare:
      case Token.colorRGB:
        color = getArgbParam(i);
        i = iToken;
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
    if (isTranslucentOrOpaque || !Float.isNaN(radius)
        || color != Integer.MIN_VALUE) {
      if (!haveType)
        bondOrder = JmolConstants.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = JmolConstants.CONNECT_MODIFY_ONLY;
    }
    int n = viewer.makeConnections(distances[0], distances[1], bondOrder,
        operation, atomSets[0], atomSets[1], bsBonds, isBonds);
    if (isDelete) {
      if (!(tQuiet || scriptLevel > scriptReportingLevel))
        viewer.scriptStatus(GT._("{0} connections deleted", n));
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
      if (isTranslucentOrOpaque)
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "translucency",
            translucency, bsBonds);
      viewer.selectBonds(null);
    }
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      viewer.scriptStatus(GT._("{0} connections modified or created", n));
  }

  void getProperty() throws ScriptException {
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

  void background(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (isColorParam(i) || theTok == Token.none) {
      argb = getArgbParamLast(i, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    }
    int iShape = getShapeType(theTok);
    argb = getArgbParamLast(i + 1, true);
    if (!isSyntaxCheck)
      viewer.setShapePropertyArgb(iShape, "bgcolor", argb);
  }

  void center(int i) throws ScriptException {
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

  void color() throws ScriptException {
    int argb;
    switch (getToken(1).tok) {
    case Token.dollarsign:
      colorNamedObject(2);
      return;
    case Token.colorRGB:
    case Token.leftsquare:
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
      colorObject(Token.atom, 1);
      return;
    case Token.background:
      argb = getArgbParamLast(2, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    case Token.bitset:
    case Token.expressionBegin:
      colorObject(Token.atom, -1);
      return;
    case Token.rubberband:
      argb = getArgbParamLast(2, false);
      if (!isSyntaxCheck)
        viewer.setRubberbandArgb(argb);
      return;
    case Token.selectionHalo:
      argb = getArgbParamLast(2, true);
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
       setStringProperty("axesColor", StateManager.escapeColor(argb));
       return;
     }else if (StateManager.getObjectIdFromName(str) >= 0) {
        if (!isSyntaxCheck)
          viewer.setObjectArgb(str, argb);
        return;
      }
      for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
        if (str.equalsIgnoreCase(JmolConstants.elementNameFromNumber(i))) {
          if (!isSyntaxCheck)
            viewer.setElementArgb(i, argb);
          return;
        }
      }
      for (int i = JmolConstants.altElementMax; --i >= 0;) {
        if (str.equalsIgnoreCase(JmolConstants.altElementNameFromIndex(i))) {
          if (!isSyntaxCheck)
            viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
                argb);
          return;
        }
      }
      if (str.charAt(0) == '_') {
        for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
          if (str.equalsIgnoreCase("_"
              + JmolConstants.elementSymbolFromNumber(i))) {
            if (!isSyntaxCheck)
              viewer.setElementArgb(i, argb);
            return;
          }
        }
        for (int i = JmolConstants.altElementMax; --i >= JmolConstants.firstIsotope;) {
          if (str.equalsIgnoreCase("_"
              + JmolConstants.altElementSymbolFromIndex(i))) {
            if (!isSyntaxCheck)
              viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
                  argb);
            return;
          }
          if (str.equalsIgnoreCase("_"
              + JmolConstants.altIsotopeSymbolFromIndex(i))) {
            if (!isSyntaxCheck)
              viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
                  argb);
            return;
          }
        }
      }
      invalidArgument();
    default:
      colorObject(theTok, 2);
    }
  }

  void colorNamedObject(int index) throws ScriptException {
    // color $ whatever green
    int shapeType = setShapeByNameParameter(index);
    colorShape(shapeType, index + 1);
  }

  void colorObject(int tokObject, int index) throws ScriptException {
    colorShape(getShapeType(tokObject), index);
  }

  void colorShape(int shapeType, int index) throws ScriptException {
    String translucentOrOpaque = null;
    Object colorvalue = null;
    String colorOrBgcolor = "color";
    BitSet bs = null;
    if (index < 0) {
      bs = expression(-index);
      index = iToken + 1;
      if (!isSyntaxCheck) {
        if (isBondSet) {
          viewer.selectBonds(bs);
          shapeType = JmolConstants.SHAPE_STICKS;
        } else {
          viewer.select(bs, true);
        }
      }
    }
    if (getToken(index).tok == Token.background) {
      colorOrBgcolor = "bgcolor";
      getToken(++index);
    }
    if (theTok == Token.translucent || theTok == Token.opaque)
      translucentOrOpaque = parameterAsString(index++);
    String modifier = "";
    if (shapeType < 0) {
      //geosurface
      shapeType = -shapeType;
      modifier = "Surface";
    }
    if (index < statementLength) {
      int tok = getToken(index).tok;
      if (isColorParam(index)) {
        int argb = getArgbParam(index, false);
        colorvalue = (argb == 0 ? null : new Integer(argb));
        if (tokAt(index = iToken + 1) != Token.nada) {
          getToken(index);
          if (translucentOrOpaque == null
              && (theTok == Token.translucent || theTok == Token.opaque))
            translucentOrOpaque = parameterAsString(index);
          checkStatementLength(index + 1);
        }
      } else {
        // must not be a color, but rather a color SCHEME
        // this could be a problem for properties, which can't be
        // checked later -- they must be turned into a color NOW.

        // "cpk" value would be "spacefill"

        String name = parameterAsString(index).toLowerCase();
        byte pid = (tok == Token.spacefill ? JmolConstants.PALETTE_CPK
            : JmolConstants.getPaletteID(name));
        if (pid == JmolConstants.PALETTE_UNKNOWN
            || pid == JmolConstants.PALETTE_TYPE
            && shapeType != JmolConstants.SHAPE_HSTICKS)
          invalidArgument();
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          if (name.equals("property")
              && Compiler.tokAttr(getToken(++index).tok, Token.atomproperty)) {
            if (!isSyntaxCheck) {
              Object data = getBitsetProperty(null,
                  getToken(index).tok | Token.minmaxmask, null, null, null,
                  null, false);
              if (data instanceof float[])
                viewer.setCurrentColorRange((float[])data, null);
              else
                invalidArgument();
            }
          } else if (!isSyntaxCheck) {
            viewer.setCurrentColorRange(name);
          }
        }
        if (pid == JmolConstants.PALETTE_VARIABLE) {
          name = parameterAsString(++index);
          float[] data = new float[viewer.getAtomCount()];
          Parser.parseFloatArray("" + viewer.getParameter(name), null, data);
          viewer.setCurrentColorRange(data, null);
          pid = JmolConstants.PALETTE_PROPERTY;
        }
        colorvalue = new Byte((byte) pid);
        checkStatementLength(index + 1);
      }
      if (isSyntaxCheck)
        return;

      //ok, the following five options require precalculation.
      //the state must not save them as paletteIDs, only as pure
      //color values. 

      switch (tok) {
      case Token.surfacedistance:
        if (viewer.getFrame().getSurfaceDistanceMax() == 0)
          dots(statementLength, Dots.DOTS_MODE_CALCONLY);
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
      viewer.loadShape(shapeType);
      if (shapeType == JmolConstants.SHAPE_STICKS)
        viewer.setShapeProperty(shapeType, colorOrBgcolor + modifier,
            colorvalue, viewer.getSelectedAtomsOrBonds());
      else
        setShapeProperty(shapeType, colorOrBgcolor + modifier, colorvalue);
    }
    if (translucentOrOpaque != null)
      setShapeProperty(shapeType, "translucency" + modifier,
          translucentOrOpaque);
  }

  Hashtable variables = new Hashtable();
  Object[] data;

  void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
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
          Viewer.setData(null, null, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = "" + viewer.getParameter(dataLabel.substring(i + 1));
        dataLabel = dataLabel.substring(0, i).trim();
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
    if (!isSyntaxCheck || isScriptCheck && isModel && fileOpenCheck) {
      if (dataType.toLowerCase().indexOf("property_") == 0) {
        data[2] = viewer.getSelectedAtomsOrBonds();
        Viewer.setData(dataType, data, viewer.getAtomCount());
      } else {
        Viewer.setData(dataType, data, 0);
      }
    }
    if (isModel && (!isSyntaxCheck || isScriptCheck && fileOpenCheck)) {
      // only if first character is "|" do we consider "|" to be new line
      char newLine = viewer.getInlineChar();
      if (dataString.length() > 0 && dataString.charAt(0) != newLine)
        newLine = '\0';
      viewer.loadInline(dataString, newLine);
    }
    if (dataType.equalsIgnoreCase("coord")) {
      viewer.loadCoordinates(dataString);
    }
  }

  void define() throws ScriptException {
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
      viewer.addStateScript(thisCommand);
    } else {
      assignBitsetVariable(variable, bs);
    }
  }

  void echo(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String text = optParameterAsString(index);
    if (viewer.getEchoStateActive())
      setShapeProperty(JmolConstants.SHAPE_ECHO, "text", text);
    showString(viewer.formatText(text));
  }

  void message() throws ScriptException {
    String text = parameterAsString(1);
    if (isSyntaxCheck)
      return;
    String s = viewer.formatText(text);
    Logger.warn(s);
    if (!s.startsWith("_"))
      viewer.scriptStatus(s);
  }

  void pause() throws ScriptException {
    pauseExecution();
    String msg = optParameterAsString(1);
    if (msg.length() == 0)
      msg = ": RESUME to continue.";
    else
      msg = ": " + viewer.formatText(msg);
    viewer.scriptStatus("script execution paused" + msg);
  }

  void label(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(index);
    if (strLabel.equalsIgnoreCase("on")) {
      strLabel = viewer.getStandardLabelFormat();
    } else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setLabel(strLabel);
  }

  void hover() throws ScriptException {
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

  void load() throws ScriptException {
    StringBuffer loadScript = new StringBuffer("load");
    int[] params = new int[4];
    Point3f unitCells = viewer.getDefaultLattice();
    params[1] = (int) unitCells.x;
    params[2] = (int) unitCells.y;
    params[3] = (int) unitCells.z;
    int i = 1;
    // ignore optional file format
    String filename = "fileset";
    if (statementLength == 1) {
      i = 0;
    } else {
      if (getToken(1).tok == Token.identifier)
        i = 2;
      if (getToken(i).tok != Token.string)
        filenameExpected();
    }
    // long timeBegin = System.currentTimeMillis();
    if (statementLength == i + 1) {
      if (i == 0 || (filename = parameterAsString(i)).length() == 0)
        filename = viewer.getFullPathName();
      loadScript.append(" " + StateManager.escape(filename) + ";");
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck)
        viewer.openFile(filename, params, loadScript.toString());
    } else if (getToken(i + 1).tok == Token.leftbrace
        || theTok == Token.integer) {
      if ((filename = parameterAsString(i++)).length() == 0)
        filename = viewer.getFullPathName();
      loadScript.append(" " + StateManager.escape(filename));
      if (getToken(i).tok == Token.integer) {
        params[0] = intParameter(i++);
        loadScript.append(" " + params[0]);
      }
      int tok = tokAt(i);
      if (tok == Token.leftbrace || tok == Token.point3f) {
        unitCells = getPoint3f(i, false);
        i = iToken + 1;
        params[1] = (int) unitCells.x;
        params[2] = (int) unitCells.y;
        params[3] = (int) unitCells.z;
        loadScript.append(" " + StateManager.escape(unitCells));
        int iGroup = -1;
        int[] p;
        if (tokAt(i) == Token.spacegroup) {
          ++i;
          String spacegroup = TextFormat.simpleReplace(parameterAsString(i++),
              "''", "\"");
          loadScript.append(" " + StateManager.escape(spacegroup));
          if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
            iGroup = -999;
          } else {
            if (spacegroup.indexOf(",") >= 0) //Jones Faithful
              if ((unitCells.x < 9 && unitCells.y < 9 && unitCells.z == 0))
                spacegroup += "#doNormalize=0";
            iGroup = viewer.getSpaceGroupIndexFromName(spacegroup);
            if (iGroup == -1)
              evalError(GT._("space group {0} was not found.", spacegroup));
          }
          p = new int[5];
          for (int j = 0; j < 4; j++)
            p[j] = params[j];
          p[4] = iGroup;
          params = p;
        }
        if (tokAt(i) == Token.unitcell) {
          ++i;
          p = new int[11];
          for (int j = 0; j < params.length; j++)
            p[j] = params[j];
          p[4] = iGroup;
          float[] fparams = new float[6];
          i = floatParameterSet(i, fparams);
          loadScript.append(" {");
          for (int j = 0; j < 6; j++) {
            p[5 + j] = (int) (fparams[j] * 10000f);
            loadScript.append((j == 0 ? "" : " ") + p[5 + j]);
          }
          loadScript.append("}");
          params = p;
        }
      }
      loadScript.append(";");
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck)
        viewer.openFile(filename, params, loadScript.toString());
    } else {
      String modelName = parameterAsString(i);
      i++;
      loadScript.append(" " + StateManager.escape(modelName));
      String[] filenames = new String[statementLength - i];
      while (i < statementLength) {
        modelName = parameterAsString(i);
        filenames[filenames.length - statementLength + i] = modelName;
        loadScript.append(" " + StateManager.escape(modelName));
        i++;
      }
      loadScript.append(";");
      if (!isSyntaxCheck || isScriptCheck && fileOpenCheck)
        viewer.openFiles(modelName, filenames, loadScript.toString());
    }
    if (isSyntaxCheck && !(isScriptCheck && fileOpenCheck))
      return;
    String errMsg = viewer.getOpenFileError();
    // int millis = (int)(System.currentTimeMillis() - timeBegin);
    // Logger.debug("!!!!!!!!! took " + millis + " ms");
    if (errMsg != null && !isScriptCheck)
      evalError(errMsg);
    if (logMessages)
      viewer.scriptStatus("Successfully loaded:" + filename);
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

  //measure() see monitor()

  void monitor() throws ScriptException {
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
      default:
        keywordExpected("ON, OFF, DELETE");
      }
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
        atomIndex = viewer.firstAtomOf(bs);
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
        monitorExpressions.add(bs);
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

  void refresh() {
    if (isSyntaxCheck)
      return;
    viewer.setTainted(true);
    viewer.requestRepaintAndWait();
  }

  void reset() throws ScriptException {
    if (isSyntaxCheck)
      return;
    if (statementLength == 1) {
      viewer.reset();
      return;
    }
    String var = parameterAsString(1);
    if (var.charAt(0) == '_')
      invalidArgument();
    viewer.unsetProperty(var);
  }

  void initialize() {
    viewer.initialize();
  }

  void restrict() throws ScriptException {
    select();
    if (isSyntaxCheck)
      return;
    BitSet bsSelected = copyBitSet(viewer.getSelectionSet());
    viewer.invertSelection();
    if (bsSubset != null) {
      BitSet bs = copyBitSet(viewer.getSelectionSet());
      bs.and(bsSubset);
      viewer.setSelectionSet(bs);
    }
    boolean bondmode = viewer.getBondSelectionModeOr();
    setBooleanProperty("bondModeOr", true);
    setShapeSize(JmolConstants.SHAPE_STICKS, 0);

    // also need to turn off backbones, ribbons, strands, cartoons
    for (int shapeType = JmolConstants.SHAPE_MIN_SELECTION_INDEPENDENT; --shapeType >= 0;)
      if (shapeType != JmolConstants.SHAPE_MEASURES)
        setShapeSize(shapeType, 0);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "delete", null);
    viewer.setLabel(null);

    setBooleanProperty("bondModeOr", bondmode);
    viewer.setSelectionSet(bsSelected);
  }

  void rotate(boolean isSpin, boolean isSelected) throws ScriptException {

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
     *  fixed room frame. Fractional coordinates may be indicated:
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
      case Token.hyphen:
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
        // $drawObject
        isInternal = true;
        axisID = objectNameParameter(++i);
        if (isSyntaxCheck) {
          rotCenter = new Point3f();
          rotAxis = new Vector3f();
        } else {
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

  Point3f getDrawObjectCenter(String axisID) {
    return (Point3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getSpinCenter:" + axisID);
  }

  Vector3f getDrawObjectAxis(String axisID) {
    return (Vector3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getSpinAxis:" + axisID);
  }

  void script() throws ScriptException {
    // token allows for only 1 or 2 parameters
    if (getToken(1).tok != Token.string)
      filenameExpected();
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
    String filename = parameterAsString(1);
    String theScript = (filename.equalsIgnoreCase("inline") ? parameterAsString(i++)
        : null);
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
        if (getToken(i++).tok == Token.hyphen)
          lineEnd = (checkToken(i) ? intParameter(i++) : 0);
        else
          invalidArgument();
    } else if (option.equalsIgnoreCase("command")
        || option.equalsIgnoreCase("commands")) {
      i++;
      pc = Math.max(intParameter(i++) - 1, 0);
      pcEnd = pc + 1;
      if (checkToken(i))
        if (getToken(i++).tok == Token.hyphen)
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
    pushContext();
    boolean isOK;
    if (theScript != null)
      isOK = loadScript(null, theScript);
    else
      isOK = loadScriptFileInternal(filename);
    if (isOK) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < linenumbers.length && linenumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = fileOpenCheck;
      fileOpenCheck = fileOpenCheck && loadCheck;
      instructionDispatchLoop(isCheck);
      fileOpenCheck = saveLoadCheck;
      popContext();
    } else {
      Logger.error("script ERROR: " + errorMessage);
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

  void history(int pt) throws ScriptException {
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

  void hide() throws ScriptException {
    if (statementLength == 1) {
      viewer.hide(null, tQuiet);
      return;
    }
    BitSet bs = expression(1);
    if (!isSyntaxCheck)
      viewer.hide(bs, tQuiet);
  }

  void display() throws ScriptException {
    if (statementLength == 1) {
      viewer.display(bsAll(), null, tQuiet);
      return;
    }
    BitSet bs = expression(1);
    if (!isSyntaxCheck)
      viewer.display(bsAll(), bs, tQuiet);
  }

  BitSet bsAll() {
    int atomCount = viewer.getAtomCount();
    BitSet bs = new BitSet(atomCount);
    for (int i = atomCount; --i >= 0;)
      bs.set(i);
    return bs;
  }

  void select() throws ScriptException {
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

  void subset() throws ScriptException {
    bsSubset = (statementLength == 1 ? null : expression(-1));
    if (isSyntaxCheck)
      return;
    viewer.setSelectionSubset(bsSubset);
    //I guess we do not want to select, because that could 
    //throw off picking in a strange way
    // viewer.select(bsSubset, false);
  }

  void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    Point3f pt = null;
    Point4f plane = null;
    if (statementLength == 1) {
      if (isSyntaxCheck)
        return;
      BitSet bs = viewer.getSelectedAtomsOrBonds();
      pt = viewer.getAtomSetCenter(bs);
      viewer.invertSelected(pt, bs);
      return;
    }
    String type = parameterAsString(1);

    if (type.equalsIgnoreCase("point")) {
      pt = atomCenterOrCoordinateParameter(2);
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

  void translateSelected() throws ScriptException {
    // translateSelected {x y z}
    Point3f pt = getPoint3f(1, true);
    if (!isSyntaxCheck)
      viewer.setAtomCoordRelative(pt);
  }

  void translate() throws ScriptException {
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

  void zap() {
    viewer.zap(true);
    refresh();
  }

  void zoom(boolean isZoomTo) throws ScriptException {
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
    if (isAtomCenterOrCoordinateNext(i)) {
      ptCenter = i;
      center = atomCenterOrCoordinateParameter(i);
      i = iToken + 1;
    }

    boolean isSameAtom = (center != null && currentCenter.distance(center) < 0.1);

    //zoom/zoomTo percent|-factor|+factor|*factor|/factor 
    float factor = (isFloatParameter(i) ? floatParameter(i++) : 0f);
    if (factor < 0)
      factor += zoom;
    if (factor == 0) {
      factor = zoom;
      if (isFloatParameter(i + 1)) {
        float value = floatParameter(i + 1);
        switch (getToken(i).tok) {
        case Token.slash:
          factor /= value;
          break;
        case Token.asterisk:
          factor *= value;
          break;
        case Token.plus:
          factor += value;
          break;
        default:
          invalidArgument();
        }
      } else if (isZoomTo) {
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
    if (!isSyntaxCheck)
      viewer.moveTo(time, center, new Point3f(0, 0, 0), Float.NaN, factor,
          xTrans, yTrans, radius, null, Float.NaN, Float.NaN, Float.NaN);
  }

  void gotocmd() throws ScriptException {
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

  void delay() throws ScriptException {
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

  void move() throws ScriptException {
    if (statementLength > 11)
      badArgumentCount();
    //rotx roty rotz zoom transx transy transz slab seconds fps
    Vector3f dRot = new Vector3f(floatParameter(1), floatParameter(2),
        floatParameter(3));
    int dZoom = intParameter(4);
    Vector3f dTrans = new Vector3f(intParameter(5), intParameter(6),
        intParameter(7));
    int dSlab = intParameter(8);
    float floatSecondsTotal = floatParameter(9);
    int fps = (statementLength == 11 ? intParameter(10) : 30);
    if (isSyntaxCheck)
      return;
    refresh();
    viewer.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  void slab(boolean isDepth) throws ScriptException {
    boolean TF = false;
    Point4f plane = null;
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
    case Token.identifier:
      if (parameterAsString(1).equalsIgnoreCase("plane")) {
        switch (getToken(2).tok) {
        case Token.none:
          break;
        default:
          plane = planeParameter(2);
        }
        if (!isSyntaxCheck)
          viewer.slabInternal(plane, isDepth);
        return;
      }
      if (parameterAsString(1).equalsIgnoreCase("hkl")) {
        plane = (getToken(2).tok == Token.none ? null : hklParameter(2));
        if (!isSyntaxCheck)
          viewer.slabInternal(plane, isDepth);
        return;
      }
      if (parameterAsString(1).equalsIgnoreCase("reference")) {
        Point3f pt = centerParameter(2);
        if (!isSyntaxCheck)
          viewer.slabInternalReference(pt);
        return;
      }
    default:
      invalidArgument();
    }
  }

  int diameterToken() throws ScriptException {
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

  void star() throws ScriptException {
    short mad = 0; // means back to selection business
    switch (diameterToken()) {
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

  void halo() throws ScriptException {
    short mad = 0;
    switch (diameterToken()) {
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
  void spacefill() throws ScriptException {
    short mad = 0;
    boolean isSolventAccessibleSurface = false;
    int i = 1;
    switch (diameterToken()) {
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

  void wireframe() throws ScriptException {
    short mad = getMadParameter();
    if (!isSyntaxCheck)
      viewer.setShapeSize(JmolConstants.SHAPE_STICKS, mad, viewer
          .getSelectedAtomsOrBonds());
  }

  void ssbond() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    setShapeSize(JmolConstants.SHAPE_SSSTICKS, getMadParameter());
  }

  void hbond(boolean isCommand) throws ScriptException {
    if (statementLength == 2 && getToken(1).tok == Token.calculate) {
      if (isSyntaxCheck)
        return;
      int n = viewer.autoHbond(null);
      viewer.scriptStatus(GT._("{0} hydrogen bonds", n));
      return;
    }
    setShapeSize(JmolConstants.SHAPE_HSTICKS, getMadParameter());
  }

  void configuration() throws ScriptException {
    if (!isSyntaxCheck && viewer.getDisplayModelIndex() <= -2)
      evalError(GT._("{0} not allowed with background model displayed",
          "\"CONFIGURATION\""));
    BitSet bsConfigurations;
    if (statementLength == 1) {
      bsConfigurations = viewer.setConformation();
      viewer.addStateScript("select "
          + StateManager.escape(viewer.getSelectionSet()));
      viewer.addStateScript("configuration;");
    } else {
      checkLength2();
      if (isSyntaxCheck)
        return;
      int n = intParameter(1);
      bsConfigurations = viewer.setConformation(n - 1);
      viewer.addStateScript("configuration " + n + ";");
    }
    boolean addHbonds = viewer.hbondsAreVisible();
    viewer.setShapeSize(JmolConstants.SHAPE_HSTICKS, 0, bsConfigurations);
    if (addHbonds)
      viewer.autoHbond(bsConfigurations, bsConfigurations, null);
    viewer.select(bsConfigurations, tQuiet);
  }

  void vector() throws ScriptException {
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

  void dipole() throws ScriptException {
    //dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    viewer.loadShape(JmolConstants.SHAPE_DIPOLES);
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
        propertyName = "dipoleValue";
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
        continue; // ignored
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
            propertyName = "dipoleOffsetPercent";
            propertyValue = new Integer((int) v);
          } else {
            propertyName = "dipoleOffset";
            propertyValue = new Float(v);
          }
          break;
        }
        if (cmd.equalsIgnoreCase("value")) {
          propertyName = "dipoleValue";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("offsetSide")) {
          propertyName = "offsetSide";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("width")) {
          propertyName = "dipoleWidth";
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
      idSeen = (theTok != Token.delete);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DIPOLES, propertyName,
            propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JmolConstants.SHAPE_DIPOLES, "set", null);
  }

  void animationMode() throws ScriptException {
    float startDelay = 1, endDelay = 1;
    if (statementLength > 5)
      badArgumentCount();
    int animationMode = 0;
    switch (getToken(2).tok) {
    case Token.loop:
      ++animationMode;
      break;
    case Token.identifier:
      String cmd = parameterAsString(2);
      if (cmd.equalsIgnoreCase("once")) {
        startDelay = endDelay = 0;
        break;
      }
      if (cmd.equalsIgnoreCase("palindrome")) {
        animationMode = 2;
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

  void vibration() throws ScriptException {
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

  void animationDirection() throws ScriptException {
    checkStatementLength(4);
    boolean negative = false;
    getToken(2);
    if (theTok == Token.hyphen)
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

  void calculate() throws ScriptException {
    if ((iToken = statementLength) >= 2) {
      switch (getToken(1).tok) {
      case Token.surface:
        dots(2, Dots.DOTS_MODE_CALCONLY);
        if (!isSyntaxCheck)
          viewer.addStateScript(thisCommand);
        return;
      case Token.hbond:
        checkLength2();
        if (isSyntaxCheck)
          return;
        viewer.autoHbond(null);
        return;
      case Token.structure:
        checkLength2();
        if (!isSyntaxCheck)
          viewer.calculateStructures();
        return;
      }
    }
    evalError(GT._("Calculate what?") + "hbonds?  surface? structure?");
  }

  void dots(int ipt, int dotsMode) throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_DOTS);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "init", new Integer(dotsMode));
    if (statementLength == ipt) {
      setShapeSize(JmolConstants.SHAPE_DOTS, 1);
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
        setShapeProperty(JmolConstants.SHAPE_DOTS, "atom", new Integer(
            dotsParam));
        ipt++;
        setShapeProperty(JmolConstants.SHAPE_DOTS, "radius", new Float(
            floatParameter(++ipt)));
        if (statementLength > ipt + 1 && statement[++ipt].tok == Token.color)
          setShapeProperty(JmolConstants.SHAPE_DOTS, "colorRGB", new Integer(
              getArgbParam(++ipt)));
        if (getToken(ipt).tok != Token.bitset)
          invalidArgument();
        setShapeProperty(JmolConstants.SHAPE_DOTS, "dots", statement[ipt].value);
        return;
      }

      if (dotsParam < 0 || dotsParam > 1000)
        numberOutOfRange(0, 1000);
      mad = (short) (dotsParam == 0 ? 0 : dotsParam + 1);
      break;
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(JmolConstants.SHAPE_DOTS, mad);
  }

  void proteinShape(int shapeType) throws ScriptException {
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
    default:
      booleanOrNumberExpected();
    }
    setShapeSize(shapeType, mad);
  }

  void animation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case Token.on:
      animate = true;
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

  void file() throws ScriptException {
    int file = intParameter(1);
    if (isSyntaxCheck)
      return;
    int modelIndex = viewer.getModelNumberIndex(file * 1000000 + 1, false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = viewer.getModelNumberIndex((file + 1) * 1000000 + 1, false);
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelCount();
      modelIndex2--;
    }
    viewer.setAnimationOn(false);
    viewer.setAnimationDirection(1);
    viewer.setAnimationRange(modelIndex, modelIndex2);
    viewer.setCurrentModelIndex(-1);
  }

  void frame(int offset) throws ScriptException {
    boolean useModelNumber = true;
    // for now -- as before -- remove to implement
    // frame/model difference

    if (getToken(offset).tok == Token.hyphen) {
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
      case Token.asterisk:
        checkStatementLength(offset + (isRange ? 2 : 1));
        isAll = true;
        break;
      case Token.hyphen: //ignore
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
          isAll = true; // 0.0 means ALL; 0 means "all in this range
        if (iFrame > 1000000)
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
    if (isSyntaxCheck)
      return;
    if (isRange && nFrames == 0)
      isAll = true;
    if (isAll) {
      viewer.setAnimationOn(false);
      viewer.setAnimationRange(-1, -1);
      if (!isRange)
        viewer.setCurrentModelIndex(-1);
      return;
    }
    if (nFrames == 2 && !isRange)
      isHyphen = true;
    if (haveFileSet)
      useModelNumber = false;
    else
      for (int i = 0; i < nFrames; i++)
        if (frameList[i] >= 0)
          frameList[i] %= 1000000;
    int modelIndex = viewer.getModelNumberIndex(frameList[0], useModelNumber);
    int modelIndex2 = -1;
    if (haveFileSet && nFrames == 1 && modelIndex < 0 && frameList[0] != 0) {
      // may have frame 2.0 or frame 2 meaning the range of models in file 2
      if (frameList[0] < 1000000)
        frameList[0] *= 1000000;
      if (frameList[0] % 1000000 == 0) {
        frameList[0]++;
        modelIndex = viewer.getModelNumberIndex(frameList[0], false);
        if (modelIndex >= 0) {
          modelIndex2 = viewer.getModelNumberIndex(frameList[0] + 1000000,
              false);
          if (modelIndex2 < 0)
            modelIndex2 = viewer.getModelCount();
          modelIndex2--;
          if (isRange)
            nFrames = 2;
          else if (!isHyphen && modelIndex2 != modelIndex)
            isHyphen = true;
          isRange = (!isHyphen && modelIndex2 != modelIndex);
        }
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0) {
      viewer.setCurrentModelIndex(modelIndex);
    }
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelNumberIndex(frameList[1], useModelNumber);
      viewer.setAnimationOn(false);
      viewer.setAnimationDirection(1);
      viewer.setAnimationRange(modelIndex, modelIndex2);
      viewer.setCurrentModelIndex(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0);
    }
    if (isPlay)
      viewer.resumeAnimation();
  }

  BitSet bitSetForModelNumberSet(int[] frameList, int nFrames) {
    BitSet bs = new BitSet();
    if (isSyntaxCheck)
      return bs;
    int modelCount = viewer.getModelCount();
    boolean haveFileSet = viewer.haveFileSet();
    for (int i = 0; i < nFrames; i++) {
      int m = frameList[i];
      if (m < 1000000 && haveFileSet)
        m *= 1000000;
      int pt = m % 1000000;
      if (pt == 0) {
        int model1 = viewer.getModelNumberIndex(m + 1, false);
        int model2 = viewer.getModelNumberIndex(m + 1000001, false);
        if (model1 < 0)
          model1 = 0;
        if (model2 < 0)
          model2 = modelCount;
        for (int j = model1; j < model2; j++)
          bs.or(viewer.getModelAtomBitSet(j));
      } else {
        bs.or(viewer.getModelAtomBitSet(viewer.getModelNumberIndex(m, false)));
      }
    }
    return bs;
  }

  void frameControl(int i, boolean isSubCmd) throws ScriptException {
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

  int getShapeType(int tok) throws ScriptException {
    if (tok == Token.geosurface)
      return -JmolConstants.shapeTokenIndex(Token.dots);
    int iShape = JmolConstants.shapeTokenIndex(tok);
    if (iShape < 0)
      unrecognizedObject();
    return iShape;
  }

  void font() throws ScriptException {
    int shapeType = 0;
    int fontsize = 0;
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    switch (iToken = statementLength) {
    case 5:
      if (getToken(4).tok != Token.identifier)
        invalidArgument();
      fontstyle = parameterAsString(4);
    case 4:
      if (getToken(3).tok != Token.identifier)
        invalidArgument();
      fontface = parameterAsString(3);
    case 3:
      if (getToken(2).tok != Token.integer)
        integerExpected();
      fontsize = intParameter(2);
      shapeType = getShapeType(getToken(1).tok);
      break;
    default:
      badArgumentCount();
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

  void set() throws ScriptException {
    if (statementLength == 1) {
      showString(viewer.getAllSettings(60));
      return;
    }
    int val = Integer.MAX_VALUE;
    int n = 0;
    String key = null;
    switch (getToken(1).tok) {
    case Token.axes:
      setAxes(2);
      return;
    case Token.background:
      background(2);
      return;
    case Token.bondmode:
      setBondmode();
      return;
    case Token.label:
      label(2);
      return;
    case Token.boundbox:
      setBoundbox(2);
      return;
    case Token.color:
    case Token.defaultColors:
      setDefaultColors();
      return;
    case Token.display://deprecated
    case Token.selectionHalo:
      setSelectionHalo(2);
      return;
    case Token.echo:
      setEcho();
      return;
    case Token.fontsize:
      setFontsize();
      return;
    case Token.hbond:
      setHbond();
      return;
    case Token.history:
      history(2);
      return;
    case Token.monitor:
      setMonitor(2);
      return;
    case Token.property: // huh? why?
      setProperty();
      return;
    case Token.scale3d:
      setScale3d();
      return;
    case Token.spin:
      checkLength4();
      setSpin(parameterAsString(2), (int) floatParameter(3));
      return;
    case Token.frank:
      setFrank(2);
      return;
    case Token.ssbond:
      setSsbond();
      return;
    case Token.unitcell:
      setUnitcell(2);
      return;
    case Token.picking:
      setPicking();
      return;
    case Token.pickingStyle:
      setPickingStyle();
      return;
    case Token.formalCharge:
      n = intParameter(2);
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
    case Token.ambient:
      if (key == null)
        key = "ambientPercent";
    case Token.diffuse:
      if (key == null)
        key = "diffusePercent";
      val = intParameter(2);
      if (val > 100 || val < 0)
        numberOutOfRange(0, 100);
      break;
    case Token.specpower:
      val = intParameter(2);
      if (val > 100)
        numberOutOfRange(0, 100);
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
      val = intParameter(2);
      if (val > 10 || val < 1)
        numberOutOfRange(1, 10);
      key = "specularExponent";
      break;
    case Token.bonds:
      key = "showMultipleBonds";
      break;
    case Token.strands:
      checkLength3();
      int strandCount = intParameter(2);
      if (strandCount < 0 || strandCount > 20)
        numberOutOfRange(0, 20);
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
    default:
      key = parameterAsString(1);
      if (key.charAt(0) == '_') //these cannot be set by user
        invalidArgument();

      //these next are not reported

      if (key.toLowerCase().indexOf("label") == 0) {
        setLabel(key.substring(5));
        return;
      }
      if (key.equalsIgnoreCase("defaultColorScheme")) {
        setDefaultColors();
        return;
      }
      if (key.equalsIgnoreCase("measurementNumbers")
          || key.equalsIgnoreCase("measurementLabels")) {
        setMonitor(2);
        return;
      }
      if (key.equalsIgnoreCase("defaultLattice")) {
        Point3f pt;
        if (getToken(2).tok == Token.integer) {
          int i = intParameter(2);
          pt = new Point3f(i, i, i);
        } else {
          pt = (Point3f) getPointOrPlane(2, false, false, false, false, 3, 3);
        }
        if (!isSyntaxCheck)
          viewer.setDefaultLattice(pt);
        return;
      }
      if (key.equalsIgnoreCase("toggleLabel")) { //from PickingManager
        BitSet bs = expression(2);
        if (!isSyntaxCheck)
          viewer.togglePickingLabel(bs);
        return;
      }
      if (key.equalsIgnoreCase("logLevel")) {
        // set logLevel n
        // we have 5 levels 0 - 4 debug -- error
        // n = 0 -- no messages -- turn all off
        // n = 1 add level 4, error
        // n = 2 add level 3, warn
        // etc.
        checkLength3();
        int ilevel = intParameter(2);
        if (isSyntaxCheck)
          return;
        Logger.setLogLevel(ilevel);
        Logger.info("logging level set to " + ilevel);
        return;
      }
      if (key.equalsIgnoreCase("backgroundModel")) {
        checkLength3();
        if (!isSyntaxCheck)
          viewer.setBackgroundModel(statement[2].intValue);
        return;
      }
      if (Compiler.isOneOf(key.toLowerCase(), "spinx;spiny;spinz;spinfps")) {
        checkLength3();
        setSpin(key.substring(4), (int) floatParameter(2));
        return;
      }

      // deprecated:

      if (key.equalsIgnoreCase("showSelections")) {
        key = "selectionHalos";
      }

    }

    String str = "";
    boolean showing = (!isSyntaxCheck && scriptLevel <= scriptReportingLevel);

    if (setParameter(key, val)) {
      if (isSyntaxCheck)
        return;
    } else {
      Object v = parameterExpression((getToken(2).tok == Token.opEQ ? 3 : 2),
          key);
      if (isSyntaxCheck)
        return;
      if (v instanceof Boolean) {
        setBooleanProperty(key, ((Boolean) v).booleanValue());
      } else if (v instanceof Integer) {
        setIntProperty(key, ((Integer) v).intValue());
      } else if (v instanceof Float) {
        setFloatProperty(key, ((Float) v).floatValue());
      } else if (v instanceof String) {
        setStringProperty(key, (String) v);
      } else if (v instanceof BondSet) {
        setIntProperty(key, Viewer.cardinalityOf((BitSet) v));
        setStringProperty(key + "_set", StateManager.escape((BitSet) v, false));
        if (showing)
          viewer.showParameter(key + "_set", true, 80);
      } else if (v instanceof BitSet) {
        setIntProperty(key, Viewer.cardinalityOf((BitSet) v));
        setStringProperty(key + "_set", StateManager.escape((BitSet) v));
        if (showing)
          viewer.showParameter(key + "_set", true, 80);
      } else if (v instanceof Point3f) {
        //drawPoint(key, (Point3f) v, false);
        str = StateManager.escape((Point3f) v);
        setStringProperty(key, str);
        //if (showing)
        //showString("to visualize, use DRAW @" + key);
      } else if (v instanceof Point4f) {
        //drawPlane(key, (Point4f) v, false);
        str = StateManager.escape((Point4f) v);
        setStringProperty(key, str);
        //if (showing)
        //showString("to visualize, use ISOSURFACE PLANE @" + key);
      }
    }
    if (showing)
      viewer.showParameter(key, true, 80);
  }

  boolean setParameter(String key, int intVal) throws ScriptException {
    if (key.equalsIgnoreCase("scriptReportingLevel")) { //11.1.13
      checkLength3();
      int iLevel = intParameter(2);
      if (!isSyntaxCheck) {
        scriptReportingLevel = iLevel;
        setIntProperty(key, iLevel);
      }
    } else if (key.equalsIgnoreCase("defaults")) {
      checkLength3();
      String val = parameterAsString(2).toLowerCase();
      if (!val.equals("jmol") && !val.equals("rasmol"))
        invalidArgument();
      setStringProperty("defaults", val);
    } else if (key.equalsIgnoreCase("historyLevel")) {
      checkLength3();
      int iLevel = intParameter(2);
      if (!isSyntaxCheck) {
        commandHistoryLevelMax = iLevel;
        setIntProperty(key, iLevel);
      }
    } else if (key.equalsIgnoreCase("dipoleScale")) {
      checkLength3();
      float scale = floatParameter(2);
      if (scale < -10 || scale > 10)
        numberOutOfRange(-10f, 10f);
      setFloatProperty("dipoleScale", scale);
    } else if (statementLength == 2) {
      if (!isSyntaxCheck)
        setBooleanProperty(key, true);
    } else if (statementLength == 3) {
      if (intVal != Integer.MAX_VALUE) {
        setIntProperty(key, intVal);
        return true;
      }
      getToken(2);
      if (theTok == Token.none) {
        if (!isSyntaxCheck)
          viewer.unsetProperty(key);
      } else if (theTok == Token.on || theTok == Token.off) {
        if (!isSyntaxCheck)
          setBooleanProperty(key, theTok == Token.on);
      } else {
        return false;
      }
    } else {
      return false;
    }
    return true;

  }

  Object parameterExpression(int pt, String key) throws ScriptException {
    Object v;
    Rpn rpn = new Rpn(16);
    for (int i = pt; i < statementLength; i++) {
      v = null;
      switch (getToken(i).tok) {
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
        rpn.addX(new Token(Token.integer, theToken.intValue));
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
        v = expression(statement, i, true, true, false);
        i = iToken;
        break;
      case Token.rightbrace:
        invalidArgument();
      case Token.comma: //ignore commas
        break;
      case Token.dot:
        Token token = getBitsetPropertySelector(i);
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
        if (Compiler.tokAttr(theTok, Token.mathop)
            || Compiler.tokAttr(theTok, Token.mathfunc)) {
          if (!rpn.addOp(theToken)) {
            iToken--;
            invalidArgument();
          }
        } else {
          String name = parameterAsString(i);
          if (isSyntaxCheck) {
            v = name;
          } else {
            v = viewer.getParameter(name);
            if (v instanceof String)
              v = getStringObjectAsToken((String) v);
          }
          break;

        }
      }
      if (v != null)
        if (v instanceof Boolean) {
          rpn.addX(((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          rpn.addX(new Token(Token.integer, ((Integer) v).intValue()));
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
    Token result = rpn.getResult(false);
    if (result == null) {
      if (!isSyntaxCheck)
        rpn.dumpStacks();
      endOfStatementUnexpected();
    }

    if (key == null)
      return new Boolean(Token.bValue(result));
    switch (result.tok) {
    case Token.on:
    case Token.off:
      return new Boolean(result == Token.tokenOn);
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

  void assignBitsetVariable(String variable, BitSet bs) {
    variables.put(variable, bs);
    setStringProperty("@" + variable, StateManager.escape(bs));
  }

  int[] getAtomIndices(BitSet bs) {
    int len = bs.size();
    int n = 0;
    int atomCount = viewer.getFrame().getAtomCount();
    int[] indices = new int[atomCount];
    for (int j = 0; j < len; j++)
      if (bs.get(j))
        indices[j] = ++n;
    return indices;
  }

  BitSet getAtomBitsetFromBonds(BitSet bsBonds) {
    BitSet bsAtoms = new BitSet();
    int bondCount = viewer.getBondCount();
    Bond[] bonds = viewer.getFrame().bonds;
    for (int i = bondCount; --i >= 0;) {
      if (!bsBonds.get(i))
        continue;
      bsAtoms.set(bonds[i].atom1.atomIndex);
      bsAtoms.set(bonds[i].atom2.atomIndex);
    }
    return bsAtoms;
  }

  String getBitsetIdent(BitSet bs, String label, Object tokenValue,
                        boolean useAtomMap) {
    int pt = (label == null ? -1 : label.indexOf("%"));
    if (bs == null || isSyntaxCheck || pt < 0)
      return (label == null ? "" : label);
    StringBuffer s = new StringBuffer();
    int len = bs.size();
    Frame frame = viewer.getFrame();
    int n = 0;
    boolean isAtoms = !(tokenValue instanceof BondSet);
    int[] indices = (isAtoms || !useAtomMap ? null
        : ((BondSet) tokenValue).associatedAtoms);
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = getAtomIndices(bs);
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
        float[] f = Viewer.getDataFloat(name);
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
            str = frame.getAtomAt(j).getIdentity();
          } else {
            str = frame.getAtomAt(j).formatLabel(str, '\0', indices);
            for (int k = 0; k < nProp; k++)
              if (j < propArray[k].length)
                str = TextFormat.formatString(str, props[k], propArray[k][j]);
          }
        } else {
          Bond bond = frame.getBondAt(j); 
          if (str == null)
            str = bond.getIdentity();
          else {
            str = bond.formatLabel(str, indices);
            int ia1 = bond.atom1.atomIndex;
            int ia2 = bond.atom2.atomIndex;
            for (int k = 0; k < nProp; k++)
              if (ia1 < propArray[k].length)
                str = TextFormat.formatString(str, props[k]+"1", propArray[k][ia1]);
            for (int k = 0; k < nProp; k++)
              if (ia2 < propArray[k].length)
                str = TextFormat.formatString(str, props[k]+"2", propArray[k][ia2]);
          }
        }
        str = TextFormat.formatString(str, "#", ++n);
        if (n > 1)
          s.append("\n");
        s.append(str);
      }
    return s.toString();
  }

  Token getBitsetPropertySelector(int i) throws ScriptException {
    int tok = getToken(++i).tok;
    String s = parameterAsString(i).toLowerCase();
    switch (tok) {
    default:
      if (Compiler.tokAttrOr(tok, Token.atomproperty, Token.mathproperty))
        break;
      invalidArgument();
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
    return new Token(Token.propselector, tok, s);
  }

  Object getBitsetProperty(BitSet bs, int tok, Point3f ptRef, Point4f planeRef,
                           Object tokenValue, Object opValue, boolean useAtomMap)
      throws ScriptException {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    boolean isMin = Compiler.tokAttr(tok, Token.min);
    boolean isMax = Compiler.tokAttr(tok, Token.max);
    boolean isAll = Compiler.tokAttr(tok, Token.minmaxmask);
    tok &= ~Token.minmaxmask;
    float[] list = null;
    BitSet bsNew = null;
    
    if (tok == Token.atom)
      bsNew = (!isAtoms && !isSyntaxCheck ? getAtomBitsetFromBonds(bs) : bs);
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
    Frame frame = viewer.getFrame();
    float[]data = (tok == Token.property ? Viewer.getDataFloat((String)opValue) : null); 
    
    if (isAtoms) {
      int atomCount = (isSyntaxCheck ? 0 : viewer.getAtomCount());
      if (isAll)
        list = new float[atomCount];
      for (int i = 0; i < atomCount; i++)
        if (bs == null || bs.get(i)) {
          n++;
          Atom atom = frame.getAtomAt(i);
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
              iv = atom.getResno() + 1;
              break;
            case Token.groupID:
              iv = atom.getGroupID() + 1;
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
            case Token.file:
              iv = atom.getModelFileIndex() + 1;
              break;
            case Token.model:
              iv = atom.getModelNumber() % 1000000;
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
          case Token.distance:
            if (planeRef != null)
              fv = Graphics3D.distanceToPlane(planeRef, atom);
            else
              fv = atom.distance(ptRef);
            break;
          case Token.radius:
            fv = atom.getRadius();
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
            if (frame.getSurfaceDistanceMax() == 0)
              dots(statementLength, Dots.DOTS_MODE_CALCONLY);
            fv = atom.getSurfaceDistance100() / 100f;
            break;
          case Token.temperature: // 0 - 9999
            fv = atom.getBfactor100() / 100f;
            break;
          case Token.xyz:
            pt.add(atom);
            break;
          case Token.color:
            pt.add(Graphics3D.colorPointFromInt(viewer
                .getColixArgb(atom.colixAtom), ptT));
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
      int bondCount = viewer.getBondCount();
      if (isAll)
        list = new float[bondCount];
      for (int i = 0; i < bondCount; i++)
        if (bs == null || bs.get(i)) {
          n++;
          Bond bond = frame.getBondAt(i);
          switch (tok) {
          case Token.length:
            float fv = bond.atom1.distance(bond.atom2);
            fvMin = Math.min(fvMin, fv);
            fvMax = Math.max(fvMax, fv);
            fvAvg += fv;
            if (isAll)
              list[i] = fv;
            break;
          case Token.xyz:
            pt.add(bond.atom1);
            pt.add(bond.atom2);
            n++;
            break;
          case Token.color:
            pt.add(Graphics3D.colorPointFromInt(
                viewer.getColixArgb(bond.colix), ptT));
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
    }
    if (isAll && opValue == null) //not operating
      return list;
    if (isAll)
      return StateManager.escape(list);
    if (isInt && (ivAvg / n) * n == ivAvg)
      return new Integer(ivAvg / n);
    return new Float((isInt ? ivAvg * 1f : fvAvg) / n);
  }

  void setAxes(int index) throws ScriptException {
    if (statementLength == 1) {
      setShapeSize(JmolConstants.SHAPE_AXES, 1);
      return;
    }
    String type = optParameterAsString(index).toLowerCase();
    if (statementLength == index + 1
        && Compiler.isOneOf(type, "window;unitcell;molecular")) {
      viewer.setAxesMode("axes" + type, true);
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

  void setBoundbox(int index) throws ScriptException {
    short mad = getSetAxesTypeMad(index);
    if (!isSyntaxCheck)
      viewer.setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", mad);
  }

  void setUnitcell(int index) throws ScriptException {
    if (statementLength == 1) {
      if (!isSyntaxCheck)
        viewer.setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitcell", (short)1);
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

  void setFrank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  void setDefaultColors() throws ScriptException {
    checkLength3();
    String type = parameterAsString(2);
    if (!type.equalsIgnoreCase("rasmol") && !type.equalsIgnoreCase("jmol"))
      invalidArgument();
    setStringProperty("defaultColorScheme", type);
  }

  void setBondmode() throws ScriptException {
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

  void setSelectionHalo(int pt) throws ScriptException {
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

  void setEcho() throws ScriptException {
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
      checkLength3();
      echoShapeActive = false;
    case Token.all:
      checkLength3();
    case Token.left:
    case Token.right:
    case Token.top:
    case Token.bottom:
    case Token.center:
    case Token.identifier:
      propertyValue = statement[2].value;
      break;
    case Token.string:
      echo(2);
      return;
    default:
      invalidArgument();
    }
    if (!isSyntaxCheck)
      viewer.setEchoStateActive(echoShapeActive);
    viewer.loadShape(JmolConstants.SHAPE_ECHO);
    setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
    if (statementLength == 3)
      return;
    propertyName = "align";
    // set echo name xxx
    if (statementLength == 4) {
      switch (getToken(3).tok) {
      case Token.off:
        propertyName = "off";
        break;
      case Token.left:
      case Token.right:
      case Token.top:
      case Token.bottom:
      case Token.center:
      case Token.identifier: //middle
        propertyValue = statement[3].value;
        break;
      default:
        invalidArgument();
      }
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
      return;
    }
    //set echo name x-pos y-pos
    getToken(4);
    int i = 3;
    //set echo name {x y z}
    if (isAtomCenterOrCoordinateNext(i)) {
      setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz",
          atomCenterOrCoordinateParameter(i));
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

  void setFontsize() throws ScriptException {
    int rasmolSize = 8;
    if (statementLength == 3) {
      rasmolSize = intParameter(2);
      // this is a kludge/hack to be somewhat compatible with RasMol
      rasmolSize += 5;

      if (rasmolSize < JmolConstants.LABEL_MINIMUM_FONTSIZE
          || rasmolSize > JmolConstants.LABEL_MAXIMUM_FONTSIZE)
        numberOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE,
            JmolConstants.LABEL_MINIMUM_FONTSIZE);
    }
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "fontsize", new Integer(
        rasmolSize));
  }

  void setLabel(String str) throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    if (str.equals("offset")) {
      checkLength4();
      int xOffset = intParameter(2);
      int yOffset = intParameter(3);
      if (xOffset > 100 || yOffset > 100 || xOffset < -100 || yOffset < -100)
        numberOutOfRange(-100, 100);
      int offset = ((xOffset & 0xFF) << 8) | (yOffset & 0xFF);
      setShapeProperty(JmolConstants.SHAPE_LABELS, "offset",
          new Integer(offset));
      return;
    }
    if (str.equals("alignment")) {
      checkLength3();
      switch (statement[2].tok) {
      case Token.left:
      case Token.right:
      case Token.center:
        setShapeProperty(JmolConstants.SHAPE_LABELS, "align",
            statement[2].value);
        return;
      }
      invalidArgument();
    }
    if (str.equals("pointer")) {
      checkLength3();
      int flags = Text.POINTER_NONE;
      switch (statement[2].tok) {
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
      setShapeProperty(JmolConstants.SHAPE_LABELS, "pointer",
          new Integer(flags));
      return;
    }
    checkLength2();
    if (str.equals("atom")) {
      setShapeProperty(JmolConstants.SHAPE_LABELS, "front", Boolean.FALSE);
      return;
    }
    if (str.equals("front")) {
      viewer
          .setShapeProperty(JmolConstants.SHAPE_LABELS, "front", Boolean.TRUE);
      return;
    }
    if (str.equals("group")) {
      viewer
          .setShapeProperty(JmolConstants.SHAPE_LABELS, "group", Boolean.TRUE);
      return;
    }
    invalidArgument();
  }

  void setMonitor(int index) throws ScriptException {
    //on off here incompatible with "monitor on/off" so this is just a SET option.
    //index will be 2 here.
    boolean showMeasurementNumbers = false;
    checkLength3();
    switch (tokAt(index)) {
    case Token.on:
      showMeasurementNumbers = true;
    case Token.off:
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
          showMeasurementNumbers ? Boolean.TRUE : Boolean.FALSE);
      return;
    case Token.identifier:
      String units = parameterAsString(index);
      if (!StateManager.isMeasurementUnit(units))
        unrecognizedParameter("MEASURE ", units);
      if (!isSyntaxCheck)
        viewer.setMeasureDistanceUnits(units);
      return;
    }
    setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad(index));
  }

  void setProperty() throws ScriptException {
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

  void setSpin(String key, int value) throws ScriptException {
    if (Compiler.isOneOf(key, "x;y;z;fps")) {
      if (isSyntaxCheck)
        return;
      switch ("x;y;z".indexOf(key)) {
      case 0:
        viewer.setSpinX(value);
        return;
      case 2:
        viewer.setSpinY(value);
        return;
      case 4:
        viewer.setSpinZ(value);
        return;
      default:
        viewer.setSpinFps(value);
        return;
      }
    }
    unrecognizedParameter("SPIN =", parameterAsString(2));
  }

  void setSsbond() throws ScriptException {
    checkLength3();
    boolean ssbondsBackbone = false;
    viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch (statement[2].tok) {
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

  void setHbond() throws ScriptException {
    checkLength3();
    boolean bool = false;
    switch (statement[2].tok) {
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

  void setScale3d() throws ScriptException {
    checkLength3();
    switch (statement[2].tok) {
    case Token.decimal:
    case Token.integer:
      break;
    default:
      numberExpected();
    }
    setFloatProperty("scaleAngstromsPerInch", floatParameter(2));
  }

  void setPicking() throws ScriptException {
    if (statementLength == 2) {
      setStringProperty("picking", "ident");
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
    if (JmolConstants.GetPickingMode(str) < 0)
      unrecognizedParameter("SET PICKING " + type, str);
    setStringProperty("picking", str);
  }

  void setPickingStyle() throws ScriptException {
    int i = 2;
    boolean isMeasure = false;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.monitor:
      isMeasure = true;
      type = "MEASURE";
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
    if (JmolConstants.GetPickingStyle(str) < 0)
      unrecognizedParameter("SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  /* ****************************************************************************
   * ==============================================================
   * SAVE/RESTORE 
   * ==============================================================
   */

  void save() throws ScriptException {
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      switch (statement[1].tok) {
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
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.saveSelection(saveName);
          return;
        }
      }
    }
    evalError(GT._("save what?") + " bonds? orientation? selection? state?");
  }

  void restore() throws ScriptException {
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
        runScript(state);
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.restoreSelection(saveName);
          return;
        }
      }
    }
    evalError(GT._("restore what?") + " bonds? orientation? selection?");
  }

  void write() throws ScriptException {
    boolean isApplet = viewer.isApplet();
    int tok = (statementLength == 1 ? Token.clipboard : statement[1].tok);
    int pt = 1;
    int len = 0;
    String type = "SPT";
    String data = "";
    boolean isCoord = false;
    switch (tok) {
    case Token.coord:
    case Token.data:
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
    case Token.identifier:
      type = parameterAsString(1).toLowerCase();
      if (type.equals("image")) {
        pt++;
      } else if (type.equals("var")) {
        pt += 2;
        type = "VAR";
      } else {
        type = "image";
      }
      break;
    }
    if (pt >= statementLength)
      badArgumentCount();

    tok = statement[pt].tok;
    String val = parameterAsString(pt);

    //write [image|history|state] clipboard

    if (val.equalsIgnoreCase("clipboard")) {
      if (isSyntaxCheck)
        return;
      if (isApplet)
        evalError(GT._("The {0} command is not available for the applet.",
            "WRITE CLIPBOARD"));
    }

    //write [optional image|history|state] [JPG|JPG64|PNG|PPM|SPT] "filename"
    //write script "filename"
    //write isosurface t.jvxl 

    if (pt + 2 == statementLength) {
      data = parameterAsString(++pt);
      if (data.charAt(0) != '.')
        type = val.toUpperCase();
    }
    if (pt + 1 != statementLength)
      badArgumentCount();
    String fileName = null;
    switch (getToken(pt).tok) {
    case Token.identifier:
    case Token.string:
      fileName = parameterAsString(pt);
      //write filename.xxx  gets separated as filename .spt
      //write isosurface filename.xxx also 
      if (fileName.charAt(0) == '.' && (pt == 2 || pt == 3)) {
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
    boolean isImage = Compiler.isOneOf(type, "JPEG;JPG64;JPG;PPM;PNG");
    if (!isImage && !Compiler.isOneOf(type, "SPT;HIS;MO;ISO;VAR;XYZ;MOL;PDB"))
      evalError(GT._("write what? {0} or {1} \"filename\"", new Object[] {
          "STATE|HISTORY|IMAGE|ISOSURFACE|MO CLIPBOARD|VAR x|DATA",
          "JPG|JPG64|PNG|PPM|SPT|JVXL|XYZ|MOL|PDB" }));
    if (isSyntaxCheck)
      return;
    if (isImage && isApplet)
      evalError(GT._("The {0} command is not available for the applet.",
          "WRITE image"));
    int quality = Integer.MIN_VALUE;
    data = type.intern();
    if (data == "PDB" || data == "XYZ" || data == "MOL") {
      data = viewer.getData("selected", data);
    } else if (data == "VAR") {
      data = "" + viewer.getParameter(parameterAsString(2));
    } else if (data == "SPT") {
      if (isCoord) {
        BitSet tainted = viewer.getTaintedAtoms();
        viewer.setAtomCoordRelative(new Point3f(0, 0, 0));
        data = (String) viewer.getProperty("string", "stateInfo", null);
        viewer.setTaintedAtoms(tainted);
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
      quality = 100;
    }
    if (len == 0)
      len = data.length();
    viewer.createImage(fileName, data, quality);
    viewer.scriptStatus("type=" + type + "; file="
        + (fileName == null ? "CLIPBOARD" : fileName)
        + (len >= 0 ? "; length=" + len : ""));
  }

  /* ****************************************************************************
   * ==============================================================
   * SHOW 
   * ==============================================================
   */

  void show() throws ScriptException {
    String value = null;
    String str = parameterAsString(1);
    String msg = null;
    int len = 2;
    switch (getToken(1).tok) {
    case Token.set:
      checkLength2();
      if (!isSyntaxCheck)
        showString(viewer.getAllSettings(60));
      return;
    case Token.url:
      // in a new window
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          viewer.showUrl(viewer.getFullPathName());
        return;
      }
      String fileName = parameterAsString(2);
      if (!isSyntaxCheck)
        viewer.showUrl(fileName);
      return;
    case Token.defaultColors:
      str = "defaultColorScheme";
      break;
    case Token.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case Token.identifier:
      if (str.equalsIgnoreCase("historyLevel")) {
        value = "" + commandHistoryLevelMax;
      } else if (str.equalsIgnoreCase("defaultLattice")) {
        value = StateManager.escape(viewer.getDefaultLattice());
      } else if (str.equalsIgnoreCase("logLevel")) {
        value = "" + Viewer.getLogLevel();
      } else if (str.equalsIgnoreCase("fileHeader")) {
        if (!isSyntaxCheck)
          msg = viewer.getPDBHeader();
      } else if (str.equalsIgnoreCase("debugScript")) {
        value = "" + viewer.getDebugScript();
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
      viewer.loadShape(JmolConstants.SHAPE_STRANDS);
      msg = "set strands "
          + viewer.getShapeProperty(JmolConstants.SHAPE_STRANDS, "strandCount");
      break;
    case Token.hbond:
      msg = "hbondsBackbone = " + viewer.getHbondsBackbone()
          + ";hbondsSolid = " + viewer.getHbondsSolid();
      break;
    case Token.spin:
      msg = viewer.getSpinState();
      break;
    case Token.ssbond:
      msg = "ssbondsBackbone = " + viewer.getSsbondsBackbone();
      break;
    case Token.display://deprecated
    case Token.selectionHalo:
      msg = "selectionHalos = " + viewer.getSelectionHaloEnabled();
      break;
    case Token.hetero:
      msg = "defaultSelectHetero = " + viewer.getRasmolHeteroSetting();
      break;
    case Token.hydrogen:
      msg = "defaultSelectHydrogens = " + viewer.getRasmolHydrogenSetting();
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
    case Token.data:
      String type = ((len = statementLength) == 3 ? parameterAsString(2) : null);
      if (!isSyntaxCheck) {
        Object[] data = (type == null ? this.data : Viewer.getData(type));
        msg = (data == null ? "no data" : "data \""
            + data[0]
            + "\"\n"
            + (data[1] instanceof float[] ? StateManager
                .escape((float[]) data[1]) : "" + data[1]));
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
      if (!isSyntaxCheck)
        msg = "boundbox " + viewer.getBoundBoxCenter() + " "
            + viewer.getBoundBoxCornerVector();
      break;
    case Token.center:
      if (!isSyntaxCheck)
        msg = "center " + StateManager.escape(viewer.getRotationCenter());
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
    case Token.atom:
      unrecognizedShowParameter("getProperty ATOMINFO (atom expression)");
    case Token.echo:
    case Token.fontsize:
    case Token.property: // huh? why?
    case Token.bonds:
    case Token.frank:
    case Token.help:
    case Token.radius:
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
      showString(str + " = " + viewer.getParameterEscaped(str));
  }

  void showString(String str) {
    if (isSyntaxCheck)
      return;
    viewer.showString(str);
  }

  String getIsosurfaceJvxl() {
    if (isSyntaxCheck)
      return "";
    return (String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
        "jvxlFileData");
  }

  String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    viewer.loadShape(JmolConstants.SHAPE_MO);
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      evalError(GT._("MO isosurfaces require that only one model be displayed"));
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

  void pmesh() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_PMESH);
    setShapeProperty(JmolConstants.SHAPE_PMESH, "init", thisCommand);
    Object t;
    boolean idSeen = false;
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.identifier:
        propertyValue = theToken.value;
        String str = ((String) propertyValue);
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
        break;
      case Token.string:
        String filename = stringParameter(i);
        propertyName = "bufferedReader";
        if (filename.equalsIgnoreCase("inline")) {
          if (i + 1 < statementLength && statement[i + 1].tok == Token.string) {
            String data = (String) statement[++i].value;
            if (data.indexOf("|") < 0 && data.indexOf("\n") < 0) {
              // space separates -- so set isOnePerLine
              data = TextFormat.simpleReplace(data, " ", "\n");
              propertyName = "bufferedReaderOnePerLine";
            }
            data = TextFormat.simpleReplace(data, "{", " ");
            data = TextFormat.simpleReplace(data, ",", " ");
            data = TextFormat.simpleReplace(data, "}", " ");
            data = TextFormat.simpleReplace(data, "|", "\n");
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
          t = viewer.getUnzippedBufferedReaderOrErrorMessageFromName(filename);
          if (t instanceof String)
            fileNotFoundException(filename + ":" + t);
        }
        propertyValue = t;
        break;
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_PMESH, theTok))
          invalidArgument();
      }
      idSeen = (theTok != Token.delete);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_PMESH, propertyName, propertyValue);
    }
  }

  void draw() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_DRAW);
    setShapeProperty(JmolConstants.SHAPE_DRAW, "init", null);
    boolean havePoints = false;
    boolean idSeen = false;
    boolean isInitialized = false;
    boolean isTranslucent = false;
    int colorArgb = Integer.MIN_VALUE;
    int intScale = 0;
    for (int i = 1; i < statementLength; ++i) {
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
      case Token.identifier:
        propertyValue = theToken.value;
        String str = (String) propertyValue;
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
        if (str.equalsIgnoreCase("PLANE")) {
          propertyName = "plane";
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
        if (str.equalsIgnoreCase("DIAMETER")) {
          propertyValue = new Float(floatParameter(++i));
          propertyName = "diameter";
          break;
        }
        if (idSeen)
          invalidArgument();
        propertyName = "thisID";
        break;
      case Token.dollarsign:
        // $drawObject
        propertyValue = objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case Token.color:
        isTranslucent = false;
        if (tokAt(++i) == Token.translucent) {
          isTranslucent = true;
          i++;
        }
        if (isColorParam(i)) {
          colorArgb = getArgbParam(i);
          i = iToken;
          setShapeProperty(JmolConstants.SHAPE_DRAW, "colorRGB", new Integer(
              colorArgb));
          continue;
        } else if (isTranslucent) {
          continue;
        }
        invalidArgument();
      case Token.decimal:
        // $drawObject
        propertyValue = new Float(floatParameter(i));
        propertyName = "length";
        break;
      case Token.integer:
        intScale = intParameter(i);
        break;
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
      case Token.translucent:
        isTranslucent = true;
        break;
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, theTok))
          invalidArgument();
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
    if (colorArgb != 0)
      setShapeProperty(JmolConstants.SHAPE_DRAW, "colorRGB", new Integer(
          colorArgb));
    if (isTranslucent)
      setShapeProperty(JmolConstants.SHAPE_DRAW, "translucency", "translucent");
    if (intScale != 0) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "scale", new Integer(intScale));
    }
  }

  void drawPoint(String key, Point3f pt, boolean isOn) {
    viewer.loadShape(JmolConstants.SHAPE_DRAW);
    setShapeProperty(JmolConstants.SHAPE_DRAW, "init", null);
    setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", key);
    setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(0));
    setShapeProperty(JmolConstants.SHAPE_DRAW, "coord", pt);
    setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
    if (!isOn)
      setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, Token.off);
  }

  void drawPlane(String key, Point4f plane, boolean isOn) {
    viewer.loadShape(JmolConstants.SHAPE_ISOSURFACE);
    setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "init", null);
    setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID", key);
    setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "plane", plane);
    setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "nomap", new Float(0));
    if (!isOn)
      setMeshDisplayProperty(JmolConstants.SHAPE_ISOSURFACE, Token.off);
  }

  void polyhedra() throws ScriptException {
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
    String translucency = "";
    int color = Integer.MIN_VALUE;
    for (int i = 1; i < statementLength; ++i) {
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
      case Token.translucent:
        translucency = "translucent";
        continue;
      case Token.opaque:
        translucency = "opaque";
        continue;
      case Token.colorRGB:
      case Token.leftsquare:
        color = getArgbParam(i);
        i = iToken;
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
    if (translucency.length() > 0)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "translucencyThis",
          translucency);
  }

  void lcaoCartoon() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LCAOCARTOON);
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "init", null);
    if (statementLength == 1) {
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "lcaoID", null);
      return;
    }
    for (int i = 1; i < statementLength; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.center:
        //serialized lcaoCartoon in isosurface format
        isosurface(JmolConstants.SHAPE_LCAOCARTOON);
        return;
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
        if (isColorParam(++i)) {
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB",
              new Integer(getArgbParam(i)));
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB",
              new Integer(getArgbParam(isColorParam(i + 1) ? i + 1 : i)));
          i = iToken;
          continue;
        }
        invalidArgument();
      case Token.string:
        propertyName = "create";
        propertyValue = stringParameter(i);
        if (i + 1 < statementLength
            && statement[i + 1].tok == Token.identifier
            && ((String) (statement[i + 1].value))
                .equalsIgnoreCase("molecular")) {
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "molecular", null);
        }
        break;
      case Token.select:
        propertyName = "selectType";
        propertyValue = parameterAsString(++i);
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("SCALE")) {
          propertyName = "scale";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("CREATE")) {
          propertyName = "create";
          propertyValue = parameterAsString(++i);
          break;
        }
        propertyValue = str;
      case Token.all:
        propertyName = "lcaoID";
        break;
      }
      if (propertyName == null)
        invalidArgument();
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, propertyName,
          propertyValue);
    }
  }

  int lastMoNumber = 0;

  void mo() throws ScriptException {
    int modelIndex = viewer.getDisplayModelIndex();
    if (!isSyntaxCheck && modelIndex < 0)
      evalError(GT._("MO isosurfaces require that only one model be displayed"));
    viewer.loadShape(JmolConstants.SHAPE_MO);
    setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
    Integer index = null;
    String title = null;
    try {
      index = (Integer) viewer.getShapeProperty(JmolConstants.SHAPE_MO,
          "moNumber");
    } catch (Exception e) {
      // could just be the string "no current mesh"
    }
    int moNumber = (index == null ? Integer.MAX_VALUE : index.intValue());
    if (moNumber == Integer.MAX_VALUE)
      lastMoNumber = 0;
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
      //mo color color1 color2
      if (tokAt(2) == Token.colorRGB || tokAt(2) == Token.leftsquare) {
        setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(
            getArgbParam(2)));
        if (tokAt(++iToken) == Token.colorRGB
            || tokAt(iToken) == Token.leftsquare)
          setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(
              getArgbParam(iToken)));
        break;
      }
      invalidArgument();
    case Token.identifier:
      str = parameterAsString(1);
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
      if (str.equalsIgnoreCase("RESOLUTION")) {
        propertyName = "resolution";
        propertyValue = new Float(floatParameter(2));
        break;
      }
      if (str.equalsIgnoreCase("SCALE")) {
        propertyName = "scale";
        propertyValue = new Float(floatParameter(2));
        break;
      }
      if (str.equalsIgnoreCase("TITLEFORMAT")) {
        if (2 < statementLength && statement[2].tok == Token.string) {
          propertyName = "titleFormat";
          propertyValue = parameterAsString(2);
        }
        break;
      }
      if (str.equalsIgnoreCase("DEBUG")) {
        propertyName = "debug";
        break;
      }
      if (str.equalsIgnoreCase("plane")) {
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = planeParameter(2);
        break;
      }
      if (str.equalsIgnoreCase("noplane")) {
        propertyName = "plane";
        propertyValue = null;
        break;
      }
      invalidArgument();
    default:
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, theTok))
        invalidArgument();
      return;
    }
    if (propertyName != null)
      setShapeProperty(JmolConstants.SHAPE_MO, propertyName, propertyValue);
    if (moNumber != Integer.MAX_VALUE) {
      if (tokAt(2) == Token.string)
        title = parameterAsString(2);
      setMoData(JmolConstants.SHAPE_MO, moNumber, title);
    }
  }

  void setMoData(int shape, int moNumber, String title) throws ScriptException {
    if (isSyntaxCheck)
      return;
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      evalError(GT._("MO isosurfaces require that only one model be displayed"));
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "moData");
    Hashtable surfaceInfo = (Hashtable) viewer.getModelAuxiliaryInfo(
        modelIndex, "jmolSurfaceInfo");
    if (surfaceInfo != null
        && ((String) surfaceInfo.get("surfaceDataType")).equals("mo")) {
      viewer.loadShape(JmolConstants.SHAPE_ISOSURFACE);
      setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "init", null);
      setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "sign", Boolean.TRUE);
      setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "getSurface",
          surfaceInfo);

      return;
    }
    if (moData == null)
      evalError(GT._("no MO basis/coefficient data available for this frame"));
    Vector mos = (Vector) (moData.get("mos"));
    int nOrb = (mos == null ? 0 : mos.size());
    if (nOrb == 0)
      evalError(GT._("no MO coefficient data available"));
    if (nOrb == 1 && moNumber > 1)
      evalError(GT._("Only one molecular orbital is available in this file"));
    if (moNumber < 1 || moNumber > nOrb)
      evalError(GT._("An MO index from 1 to {0} is required", nOrb));
    lastMoNumber = moNumber;
    setShapeProperty(shape, "moData", moData);
    if (title != null)
      setShapeProperty(shape, "title", title);
    setShapeProperty(shape, "molecularOrbital", new Integer(moNumber));
  }

  void isosurface(int iShape) throws ScriptException {
    viewer.loadShape(iShape);
    setShapeProperty(iShape, "init", isScriptCheck ? "" : thisCommand);
    setShapeProperty(iShape, "title", new String[] { thisCommand });
    int colorRangeStage = 0;
    int signPt = 0;
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean idSeen = false;
    float[] nlmZ = new float[5];
    float[] data = null;
    String str;
    int modelIndex = (isSyntaxCheck ? 0 : viewer.getDisplayModelIndex());
    if (modelIndex < 0)
      evalError(GT._(
          "the {0} command requires that only one model be displayed",
          "ISOSURFACE"));
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.property:
        propertyName = "property";
        str = parameterAsString(i);
        if (str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (isSyntaxCheck)
            continue;
          data = Viewer.getDataFloat(str);
          if (data == null)
            invalidArgument();
          propertyValue = data;
          break;
        }
        int atomCount = viewer.getAtomCount();
        int tokProperty = getToken(++i).tok;
        data = new float[atomCount];
        if (!isSyntaxCheck) {
          Frame frame = viewer.getFrame();
          Atom[] atoms = frame.atoms;
          for (int iAtom = 0; iAtom < atomCount; iAtom++)
            data[iAtom] = atomProperty(frame, atoms[iAtom], tokProperty, false);
        }
        propertyValue = data;
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
        switch (getToken(i + 1).tok) {
        case Token.absolute:
          ++i;
          colorRangeStage = 1;
          continue;
        case Token.colorRGB:
        case Token.leftsquare:
          signPt = i + 1;
          continue;
        default:
          //ignore
          continue;
        }
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
      case Token.leftsquare:
      case Token.colorRGB:
        if (i != signPt)
          invalidParameterOrder();
        propertyName = "colorRGB";
        propertyValue = new Integer(getArgbParam(i));
        i = iToken;
        signPt = i + 1;
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
      case Token.identifier:
        str = parameterAsString(i);
        if (str.equalsIgnoreCase("REMAPPABLE")) { // testing only
          propertyName = "remappable";
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
        if (str.equalsIgnoreCase("SCALE")) {
          propertyName = "scale";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("ANGSTROMS")) {
          propertyName = "angstroms";
          break;
        }
        if (str.equalsIgnoreCase("RESOLUTION")) {
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
        if (str.equalsIgnoreCase("INSIDEOUT")) { // no longer of use?
          propertyName = "insideOut";
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
        // surface objects
        if (str.equalsIgnoreCase("MAP")) { // "use current"
          surfaceObjectSeen = true;
          propertyName = "map";
          break;
        }
        if (str.equalsIgnoreCase("plane")) {
          // plane {X, Y, Z, W}
          planeSeen = true;
          propertyName = "plane";
          propertyValue = planeParameter(++i);
          i = iToken;
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
          v.add(statement[i++].value);
          v.add(getPoint3f(i, false));
          v.add(getPoint4f(++iToken));
          v.add(getPoint4f(++iToken));
          v.add(getPoint4f(++iToken));
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
                + viewer.getParameter(parameterAsString(++i)), null, data);
          }
          propertyValue = data;
          break;
        }
        propertyValue = theToken.value;
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
        switch (statement[++i].tok) {
        case Token.bitset:
        case Token.expressionBegin:
          propertyName = "lcaoCartoon";
          BitSet bs = expression(i);
          i = iToken;
          int atomIndex = viewer.firstAtomOf(bs);
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
            viewer.getPrincipalAxes(atomIndex, axes[0], axes[1], lcaoType,
                false);
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
        int moNumber = intParameter(i);
        setMoData(iShape, moNumber, null);
        surfaceObjectSeen = true;
        continue;
      case Token.mep:
        float[] partialCharges = null;
        try {
          partialCharges = viewer.getFrame().partialCharges;
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
        propertyName = surfaceObjectSeen || planeSeen ? "mapColor"
            : "getSurface";
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
        String filename = (String) theToken.value;
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
          filename = viewer.getFullPathName();
        }
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == Token.integer)
          setShapeProperty(iShape, "fileIndex", new Integer(intParameter(++i)));
        Object t = (isSyntaxCheck ? null : viewer
            .getUnzippedBufferedReaderOrErrorMessageFromName(filename));
        if (t instanceof String)
          fileNotFoundException(filename + ":" + t);
        if (!isSyntaxCheck)
          Logger.info("reading isosurface data from " + filename);
        propertyValue = t;
        break;
      default:
        if (!setMeshDisplayProperty(iShape, theTok))
          invalidArgument();
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
    if (planeSeen && !surfaceObjectSeen) {
      setShapeProperty(iShape, "nomap", new Float(0));
      surfaceObjectSeen = true;
    }
    if (surfaceObjectSeen && iShape == JmolConstants.SHAPE_ISOSURFACE && !isSyntaxCheck) {
      String id = (String) viewer.getShapeProperty(iShape, "ID");
      Integer n = (Integer) viewer.getShapeProperty(iShape, "count");
      if (id != null)
        showString(id + " created; number of isosurfaces = " + n);
    }
  }

  boolean setMeshDisplayProperty(int shape, int tok) {
    String propertyName = null;
    Object propertyValue = null;
    switch (tok) {
    case Token.on:
      propertyName = "on";
      break;
    case Token.off:
      propertyName = "off";
      break;
    case Token.delete:
      propertyName = "delete";
      break;
    case Token.dots:
      propertyValue = Boolean.TRUE;
    case Token.nodots:
      propertyName = "dots";
      break;
    case Token.mesh:
      propertyValue = Boolean.TRUE;
    case Token.nomesh:
      propertyName = "mesh";
      break;
    case Token.fill:
      propertyValue = Boolean.TRUE;
    case Token.nofill:
      propertyName = "fill";
      break;
    case Token.translucent:
      propertyName = "translucency";
      propertyValue = "translucent";
      break;
    case Token.opaque:
      propertyName = "translucency";
      propertyValue = "opaque";
      break;
    }
    if (propertyName == null)
      return false;
    setShapeProperty(shape, propertyName, propertyValue);
    return true;
  }

  ////// script exceptions ///////

  boolean ignoreError;

  void evalError(String message) throws ScriptException {
    if (ignoreError)
      throw new NullPointerException();
    if (!isSyntaxCheck) {
      String s = viewer.removeCommand();
      viewer.addCommand(s + CommandHistory.ERROR_FLAG);
    }
    throw new ScriptException(message);
  }

  //  private void evalWarning(String message) {
  //    new ScriptException(message);
  //  }

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

  String statementAsString() {
    StringBuffer sb = new StringBuffer();
    int tok = statement[0].tok;
    boolean addParens = (Compiler.tokAttr(tok, Token.embeddedExpression));
    boolean useBraces = (tok == Token.ifcmd || tok == Token.set);
    for (int i = 0; i < statementLength; ++i) {
      if (iToken == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.append(' ');
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
        if (useBraces)
          sb.append("}");
        else if (addParens)
          sb.append(")");
        continue;
      case Token.leftsquare:
      case Token.rightsquare:
        break;
      case Token.define:
        if (i > 0) {
          sb.append("@");
          continue;
        }
      case Token.on:
        sb.append("true");
        continue;
      case Token.off:
        sb.append("false");
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
          sb.append("" + token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        token = statement[++i];
        sb.append(' ');
        if (token.intValue == Integer.MAX_VALUE)
          sb.append("- ");
      //fall through
      case Token.spec_seqcode:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append("" + token.intValue);
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
          sb.append("" + token.value);
        continue;
      case Token.spec_model:
        sb.append("*/");
      //fall through
      case Token.spec_model2:
      case Token.decimal:
        if (token.intValue < Integer.MAX_VALUE) {
          int iv = token.intValue;
          sb.append("" + (iv / 1000000));
          sb.append(".");
          sb.append("" + (iv % 1000000));
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
          sb.append("cell={" + pt.x + " " + pt.y + " " + pt.z + "}");
          continue;
        }
        break;
      case Token.string:
        sb.append("\"" + token.value + "\"");
        continue;
      case Token.opEQ:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opLT:
      case Token.opNE:
        //not quite right -- for "inmath"
        if (token.intValue == Token.property) {
          sb.append((String)statement[++i].value + " ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(Token.nameOf(token.intValue) + " ");
        break;
      case Token.identifier:
        break;
      default:
        if (!logMessages)
          break;
        sb.append(token.toString());
        continue;
      }
      sb.append("" + token.value);
    }
    if (iToken >= statementLength - 1)
      sb.append(" <<");
    return sb.toString();
  }

  String contextTrace() {
    StringBuffer sb = new StringBuffer();
    for (;;) {
      sb.append(setErrorLineMessage(filename, getLinenumber(), pc,
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
      err += "line " + lineCurrent + " command " + (pcCurrent + 1)
          + " of file " + filename + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  class ScriptException extends Exception {

    String message;

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

    Token[] oStack;
    Token[] xStack;
    int oPt = -1;
    int xPt = -1;
    int maxLevel;
    int parenCount;
    int squareCount;
    int braceCount;
    boolean wasX = false;

    Rpn(int maxLevel) {
      this.maxLevel = maxLevel;
      oStack = new Token[maxLevel];
      xStack = new Token[maxLevel];
      if (logMessages)
        Logger.info("initialize RPN on " + script);
    }

    Token getResult(boolean allowUnderflow) throws ScriptException {
      boolean isOK = true;
      while (isOK && oPt >= 0)
        isOK = operate(Token.nada);
      if (isOK && xPt == 0) {
        Token x = xStack[0];
        if (x.tok == Token.bitset || x.tok == Token.list
            || x.tok == Token.string)
          x = xStack[0] = Token.selectItem(x, Integer.MIN_VALUE);
        if (x.tok == Token.list)
          x = new Token(Token.string, Token.sValue(x));
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
        xStack[++xPt] = new Token(Token.integer, -x.intValue);
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
      xStack[xPt] = new Token(Token.integer, x, new Integer(x));
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
        Logger.info("\naddOp: " + op);
        dumpStacks();
      }
      Token newOp = null;
      int tok;
      boolean isLeftOp = false;
      boolean isDotSelector = (op.tok == Token.propselector);
      boolean isMathFunc = isOpFunc(op);
      if (isDotSelector && !wasX)
        return false;

      switch (op.tok) {
      case Token.comma:
        if (!wasX)
          return false;
        wasX = false;
        return true;
      case Token.min:
      case Token.max:
      case Token.minmaxmask:
        tok = oPt < 0 ? Token.nada : oStack[oPt].tok;
        if (!wasX
            || !(tok == Token.propselector || tok == Token.bonds || tok == Token.atom))
          return false;
        oStack[oPt].intValue |= op.tok;
        return true;
      case Token.leftsquare: // two contexts: [x x].distance or {....}[n]
        isLeftOp = !wasX;
        if (isLeftOp)
          op = newOp = new Token(Token.leftsquare, 0);
        break;
      case Token.hyphen:
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
          if (!isDotSelector && wasX)
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
          && Token.prec(oStack[oPt]) >= Token.prec(op)) {

        if (logMessages) {
          dumpStacks();
          Logger.info("\noperating, oPt=" + oPt + " isLeftOp=" + isLeftOp
              + " oStack[oPt]=" + Token.nameOf(oStack[oPt].tok) + "/"
              + Token.prec(oStack[oPt]) + " op=" + Token.nameOf(op.tok) + "/"
              + Token.prec(op));
        }
        // ) and ] must wait until matching ( or [ is found
        if (op.tok == Token.rightparen && oStack[oPt].tok == Token.leftparen)
          break;

        if (op.tok == Token.rightsquare && oStack[oPt].tok == Token.leftsquare) {
          if (oStack[oPt].intValue == 0) {
            if (xPt >= 0 && xStack[xPt].tok == Token.string) {
              if (!concatList())
                return false;
            }
          } else if (!doBitsetSelect()) {
            return false;
          }
          break;
        }

        // if not, it's time to operate

        if (!operate(op.tok))
          return false;

      }

      // now add a marker on the xStack if necessary

      if (newOp != null)
        addX(newOp);

      // fix up counts and operand flag
      // right ) and ] are not added to the stack

      switch (op.tok) {
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

    private boolean concatList() throws ScriptException {
      int nPoints = 0;
      int pt = xPt;
      while (xStack[pt--].tok != Token.leftsquare)
        nPoints++;
      String[] list = new String[nPoints];
      for (int i = 0; i < nPoints; i++)
        list[i] = Token.sValue(xStack[pt + i + 2]);
      xPt = pt;
      addX(list);
      return true;

    }

    private boolean doBitsetSelect() {
      if (xPt < 1)
        return false;
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
      Logger.info("RPN stacks: for " + script);
      for (int i = 0; i <= xPt; i++)
        Logger.info("x[" + i + "]: " + xStack[i]);
      for (int i = 0; i <= oPt; i++)
        Logger.info("o[" + i + "]: " + oStack[i] + " prec="
            + Token.prec(oStack[i]));
    }

    Token getX() throws ScriptException {
      if (xPt < 0)
        endOfStatementUnexpected();
      return xStack[xPt--];
    }

    boolean evaluateFunction() throws ScriptException {

      Token op = oStack[oPt--];
      int tok = (op.tok == Token.propselector ? op.intValue : op.tok);
      int nParamMax = Token.prec(op); // note - this is NINE for dot-operators
      int nParam = 0;
      int pt = xPt;
      while (xStack[pt--] != op)
        nParam++;
      if (nParam > nParamMax)
        return false;
      Token[] args = new Token[nParam];
      for (int i = nParam; --i >= 0;)
        args[i] = getX();
      xPt--;
      switch (tok) {
      case Token.find:
        return evaluateFind(args);
      case Token.replace:
        return evaluateReplace(args);
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
      case Token.within:
        return evaluateWithin(args);
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

    boolean evaluateDistance(Token[] args) throws ScriptException {
      Token x1 = getX();
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX(1f);
      Token x2 = args[0];
      Point3f pt = ptValue(x2);
      Point4f plane = planeValue(x2);
      if (x1.tok == Token.bitset)
        return addX(getBitsetProperty(Token.bsSelect(x1), Token.distance, pt,
            plane, x1.value, null, false));
      else if (x1.tok == Token.point3f)
        return addX(plane == null ? pt.distance(ptValue(x1)) : Graphics3D
            .distanceToPlane(plane, ptValue(x1)));
      return false;
    }

    boolean evaluateMeasure(Token[] args, boolean isAngle)
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
        return addX(Measurement.computeAngle(pts[0], pts[1], pts[2], true));
      case 4:
        return addX(Measurement.computeTorsion(pts[0], pts[1], pts[2], pts[3],
            true));
      }
      return false;
    }

    boolean evaluateFind(Token[] args) throws ScriptException {
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

    boolean evaluateReplace(Token[] args) throws ScriptException {
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

    boolean evaluateString(int tok, Token[] args) throws ScriptException {
      if (args.length > 1)
        return false;
      Token x = getX();
      String s = (tok == Token.trim && x.tok == Token.list ? null : Token
          .sValue(x));
      if (isSyntaxCheck)
        return addX(s);
      String sArg = (args.length == 1 ? Token.sValue(args[0])
          : tok == Token.trim ? "" : "\n");
      switch (tok) {
      case Token.split:
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

    boolean evaluateList(int tok, Token[] args) throws ScriptException {
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

      float factor = (isScalar ? Token.fValue(x2) : 0);

      String[] sList1 = (x1.value instanceof String ? TextFormat.split(
          (String) x1.value, "\n") : (String[]) x1.value);
      float[] list1 = new float[sList1.length];
      Parser.parseFloatArray(sList1, list1);

      String[] sList2 = (isScalar ? null
          : x2.value instanceof String ? TextFormat.split((String) x2.value,
              "\n") : (String[]) x2.value);
      float[] list2 = new float[(isScalar ? sList1.length : sList2.length)];
      int len = Math.min(list1.length, list2.length);
      if (isScalar)
        for (int i = len; --i >= 0;)
          list2[i] = factor;
      else
        Parser.parseFloatArray(sList2, list2);
      switch (tok) {
      case Token.add:
        for (int i = len; --i >= 0;)
          list1[i] += list2[i];
        break;
      case Token.sub:
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
        sList1[i] = "" + list1[i];
      return addX(sList1);
    }

    boolean evaluateLoad(Token[] args) throws ScriptException {
      //System.out.println("eval load");
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX("");
      return addX(viewer.getFileAsString(Token.sValue(args[0])));
    }

    boolean evaluateData(Token[] args) throws ScriptException {
      //System.out.println("eval data");
      if (args.length == 0 || args.length > 2)
        return false;
      if (isSyntaxCheck)
        return addX("");
      String selected = Token.sValue(args[0]);
      String type = (args.length == 2 ? Token.sValue(args[1]) : "");

      // parallel addition of float property data sets

      if (selected.indexOf("property_") == 0) {
        float[] f1 = Viewer.getDataFloat(selected);
        if (f1 == null)
          return addX("");
        float[] f2 = (type.indexOf("property_") == 0 ? Viewer
            .getDataFloat(type) : null);
        if (f2 != null)
          for (int i = Math.min(f1.length, f2.length); --i >= 0;)
            f1[i] += f2[i];
        return addX(StateManager.escape(f1));
      }

      // some other data type -- just return it

      if (args.length == 1) {
        Object[] data = Viewer.getData(selected);
        return addX(data == null ? "" : "" + data[1]);
      }
      // {selected atoms} XYZ, MOL, PDB file format 
      return addX(viewer.getData(selected, type));
    }

    boolean evaluateLabel(Token[] args) throws ScriptException {
      //System.out.println("eval label");
      Token x1 = getX();
      if (args.length != 1)
        return false;
      if (isSyntaxCheck)
        return addX("");
      Token x2 = args[0];
      if (x1.tok != Token.bitset || x2.tok != Token.string)
        return false;
      return addX(getBitsetIdent(Token.bsSelect(x1), Token.sValue(x2),
          x1.value, true));
    }

    boolean evaluateWithin(Token[] args) throws ScriptException {
      // within ( distance, expression)
      // within ( group, etc., expression)
      // within ( plane or hkl or coord  atomcenter atomcenter atomcenter )
      //System.out.println("eval within");
      if (args.length < 1)
        return false;
      Object withinSpec = args[0].value;
      String withinStr = "" + withinSpec;
      BitSet bs = new BitSet();
      float distance = 0;
      boolean isSequence = false;
      if (withinSpec instanceof String)
        isSequence = !Compiler.isOneOf(withinStr,
            "element;site;group;chain;molecule;model");
      else if (withinSpec instanceof Float)
        distance = Token.fValue(args[0]);
      else
        return false;
      if (args.length == 3) {
        withinStr = Token.sValue(args[1]);
        if (!Compiler.isOneOf(withinStr, "plane;hkl;coord"))
          return false;
      }
      if (isSyntaxCheck)
        return addX(bs);
      Point3f pt = null;
      Point4f plane = null;
      int i = args.length - 1;
      if (args[i].value instanceof Point4f)
        plane = (Point4f) args[i].value;
      if (args[i].value instanceof Point3f)
        pt = (Point3f) args[i].value;
      if (plane == null && pt == null && !(args[i].value instanceof BitSet))
        return false;
      if (isSyntaxCheck)
        return addX(bs);
      if (plane != null)
        return addX(viewer.getAtomsWithin(distance, plane));
      if (pt != null)
        return addX(viewer.getAtomsWithin(distance, pt));
      bs = Token.bsSelect(args[i]);
      if (withinSpec instanceof Float)
        return addX(viewer.getAtomsWithin(distance, bs));
      if (isSequence)
        return addX(viewer.getAtomsWithin("sequence", withinStr, bs));
      return addX(viewer.getAtomsWithin(withinStr, bs));
    }

    boolean evaluateConnected(Token[] args) throws ScriptException {
      /*
       * Two options here:
       * 
       * connected(1, 3, "single", {carbon})
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
        atoms1 = bsAll();
      if (haveDecimal && atoms2 == null)
        atoms2 = atoms1;
      if (atoms2 != null) {
        BitSet bsBonds = new BitSet();
        if (isSyntaxCheck)
          return addX(new Token(Token.bitset, new BondSet(bsBonds)));
        viewer.makeConnections(fmin, fmax, order,
            JmolConstants.CONNECT_IDENTIFY_ONLY, atoms1, atoms2, bsBonds,
            isBonds);
        return addX(new Token(Token.bitset, new BondSet(bsBonds,
            getAtomIndices(getAtomBitsetFromBonds(bsBonds)))));
      }
      if (isSyntaxCheck)
        return addX(atoms1);
      return addX(viewer.getAtomsConnected(min, max, order, atoms1));
    }

    boolean evaluateSubstructure(Token[] args) throws ScriptException {
      //System.out.println("eval subs");
      if (args.length != 1)
        return false;
      BitSet bs = new BitSet();
      if (isSyntaxCheck)
        return addX(bs);
      String smiles = Token.sValue(args[0]);
      if (smiles.length() == 0)
        return false;
      PatternMatcher matcher = new PatternMatcher(viewer);
      try {
        bs = matcher.getSubstructureSet(smiles);
      } catch (Exception e) {
        evalError(e.getMessage());
      }
      return addX(bs);
    }

    boolean operate(int thisOp) throws ScriptException {

      Token op = oStack[oPt--];
      Token x2 = getX();

      //unary:

      if (op.tok == Token.opNot)
        return (x2.tok == Token.bitset ? addX(notSet(Token.bsSelect(x2)))
            : addX(!Token.bValue(x2)));

      int iv = op.intValue & ~Token.minmaxmask;
      if (op.tok == Token.propselector) {
        if (iv == Token.size) {
          return addX(Token.sizeOf(x2));
        }
        if (iv == Token.lines) {
          if (x2.tok != Token.string)
            return false;
          String s = (String) x2.value;
          s = TextFormat.simpleReplace(s, "\n\r", "\n");
          s = TextFormat.simpleReplace(s, "\r", "\n");
          return addX(Text.split(s, '\n'));
        }
        if (iv == Token.color && x2.tok == Token.string) {
          Point3f pt = new Point3f();
          return addX(Graphics3D.colorPointFromString(Token.sValue(x2), pt));
        }
        if (x2.tok != Token.bitset)
          return false;
        if (op.intValue == Token.bonds && x2.value instanceof BondSet)
          return addX(x2);
        Object val = getBitsetProperty(Token.bsSelect(x2), op.intValue, null,
            null, x2.value, op.value, false);
        if (op.intValue == Token.bonds)
          return addX(new Token(Token.bitset, new BondSet((BitSet) val,
              getAtomIndices(Token.bsSelect(x2)))));
        return addX(val);
      }

      //binary:
      Token x1 = getX();
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
        return addX(Token.bValue(x1) || Token.bValue(x2));
      case Token.opToggle:
        if (x1.tok != Token.bitset || x2.tok != Token.bitset)
          return false;
        return addX(toggle(Token.bsSelect(x1), Token.bsSelect(x2)));
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
        if (x1.tok == Token.list) {
          if (x2.tok == Token.list || x2.tok == Token.string)
            return addX(Token.concatList(x1, x2, true, x2.tok == Token.list));
        }
        if (x1.tok == Token.string || x1.tok == Token.list) {
          if (x2.tok == Token.list)
            return addX(Token.concatList(x1, x2, false, true));
          return addX(Token.sValue(x1) + Token.sValue(x2));
        }
        if (x1.tok == Token.integer && x2.tok != Token.decimal)
          return addX(x1.intValue + Token.iValue(x2));
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
      case Token.hyphen:
        if (x1.tok == Token.integer)
          return addX(x1.intValue - Token.iValue(x2));
      //fall through
      case Token.unaryMinus:
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
      case Token.asterisk:
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
        //  String % n    trim to width n; left justify
        //  String % -n   trim to width n; right justify
        //  Point3f % n   ah... sets to multiple of unit cell!
        //  bitset % n  
        //  Point3f * Point3f  does dot product
        //  Point3f / Point3f  divides by magnitude
        //  float * Point3f gets magnitude

        String s = null;
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
            return addX(TextFormat.trim(s,"\n\t "));
          else if (n > 0)
            return addX(TextFormat.format(s, n, n, true, false));
          return addX(TextFormat.format(s, -n, n, false, false));
        case Token.list:
          String[] list = (String[]) x1.value;
          for (int i = 0; i < list.length; i++) {
            if (n == 0)
              list[i] = list[i].trim();
            else if (n > 0)
              list[i] = TextFormat.format(list[i], n, n, true, false);
            else
              list[i] = TextFormat.format(s, -n, n, false, false);
          }
          return addX(list);
        case Token.point3f:
          Point3f pt = new Point3f((Point3f) x1.value);
          viewer.toUnitCell(pt, new Point3f(n, n, n));
          return addX(pt);
        case Token.bitset:
          return addX(Token.bsSelect(x1, n));
        }
      case Token.slash:
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

    Point3f ptValue(Token x) throws ScriptException {
      if (isSyntaxCheck)
        return new Point3f();
      switch (x.tok) {
      case Token.point3f:
        return (Point3f) x.value;
      case Token.bitset:
        return (Point3f) getBitsetProperty(Token.bsSelect(x), Token.xyz, null,
            null, x.value, null, false);
      default:
        float f = Token.fValue(x);
        return new Point3f(f, f, f);
      }
    }

    Point4f planeValue(Token x) {
      if (isSyntaxCheck)
        return new Point4f();
      switch (x.tok) {
      case Token.point4f:
        return (Point4f) x.value;
      case Token.bitset:
      //ooooh, wouldn't THIS be nice!
      default:
        return null;
      }
    }

    void stackOverflow() throws ScriptException {
      evalError(GT._("too many parentheses"));
    }

  }
}
