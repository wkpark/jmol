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

import org.jmol.util.Logger;
import org.jmol.g3d.Graphics3D;
import org.jmol.g3d.Font3D;
import org.jmol.smiles.InvalidSmilesException;
import org.jmol.util.CommandHistory;
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

  BitSet getAtomBitSet(String atomExpression) throws ScriptException {
    //SelectionManager 
    BitSet bs = new BitSet();
    pushContext();
    if (loadScript(null, "select (" + atomExpression + ")")) {
      statement = aatoken[0];
      bs = expression(statement, 1, false, false);
    }
    popContext();
    return bs;
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
    this.script = compiler.script;
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
    this.script = compiler.script;
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
    info.add(compiler.script);
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
      str.append(" |");
      Token[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str.append(' ');
        str.append(atoken[j]);
      }
      str.append("\n");
    }
    str.append("END\n");
    return str.toString();
  }

  void clearDefinitionsAndLoadPredefined() {
    //executed each time a file is loaded; like clear() for the managers
    variables.clear();
    bsSubset = null;
    viewer.setSelectionSubset(null);

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
        } else if (v instanceof String && ((String)v).length() > 0) {
          fixed[j] = new Token(Token.identifier, v);
        } else  {
          Point3f center = viewer.getDrawObjectCenter(var);
          if (center == null)
            invalidArgument();
          fixed[j] = new Token(Token.xyz,center);
        }
      } else {
        fixed[j] = statement[i];
      }
      j++;
    }
    statement = fixed;
    statementLength = j;
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
        if (statementLength == 1
            && !Compiler.tokAttr(token.tok, Token.unimplemented))
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
        echo();
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
        label();
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
        if (Compiler.tokAttr(token.tok, Token.unimplemented)) {
          String s = GT._("command ignored (not implemented): {0}", token.value
              .toString());
          if (isSyntaxCheck)
            evalError(s);
          evalWarning(s);
          break;
        }
        unrecognizedCommand();
      }
    }
  }

  boolean ifCmd() throws ScriptException {
    return ((Boolean)parameterExpression(1, true)).booleanValue();
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

  boolean isExpressionBitSet;
  Token[] tempStatement;

  BitSet expression(int index) throws ScriptException {
    if (!checkToken(index))
      badArgumentCount();
    return expression(statement, index, true, false);
  }

  BitSet expression(Token[] code, int pcStart, boolean allowRefresh,
                    boolean allowUnderflow) throws ScriptException {
    //note that this is general -- NOT just statement[]
    //errors reported would improperly access statement/line context
    //there should be no errors anyway, because this is for 
    //predefined variables, but it is conceivable that one could
    //have a problem. 

    if (code != statement) {
      tempStatement = statement;
      statement = code;
    }
    isExpressionBitSet = false;
    BitSet bs;
    BitSet[] stack = new BitSet[10];
    int sp = 0;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    if (ignoreSubset)
      pcStart = -pcStart;
    if (logMessages)
      viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart;; ++pc) {
      iToken = pc;
      Token instruction = code[pc];
      if (logMessages)
        viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.expressionBegin:
        break;
      case Token.expressionEnd:
      case Token.rightbrace:
        break expression_loop;
      case Token.all:
        bs = stack[sp++] = bsAll();
        break;
      case Token.none:
        stack[sp++] = new BitSet();
        break;
      case Token.opOr:
        bs = stack[--sp];
        stack[sp - 1].or(bs);
        break;
      case Token.opXor:
        bs = stack[--sp];
        stack[sp - 1].xor(bs);
        break;
      case Token.opAnd:
        bs = stack[--sp];
        stack[sp - 1].and(bs);
        break;
      case Token.opNot:
        bs = stack[sp - 1];
        notSet(bs);
        break;
      case Token.opToggle:
        bs = stack[--sp];
        toggle(stack[sp - 1], bs);
        break;
      case Token.within:
        float distance;
        Object withinSpec = instruction.value;
        if (withinSpec instanceof String) {
          if (withinSpec.equals("plane")) {
            distance = floatParameter(++pc);
            Point4f thisPlane = planeParameter(++pc);
            pc = iToken + 1;
            stack[sp++] = viewer.getAtomsWithin(distance, thisPlane);
            break;
          } else if (withinSpec.equals("hkl")) {
            distance = floatParameter(++pc);
            Point4f thisPlane = hklParameter(++pc);
            pc = iToken + 1;
            stack[sp++] = viewer.getAtomsWithin(distance, thisPlane);
            break;
          } else if (withinSpec.equals("coord")) {
            distance = floatParameter(++pc);
            Point3f thisCoordinate = getCoordinate(++pc, true);
            pc = iToken + 1;
            stack[sp++] = viewer.getAtomsWithin(distance, thisCoordinate);
            break;
          }
        }
        bs = stack[sp - 1];
        stack[sp - 1] = within(instruction, bs);
        break;
      case Token.connected:
        if (((Integer) instruction.value).intValue() == Integer.MAX_VALUE)
          break;
        bs = stack[sp - 1];
        stack[sp - 1] = connected(instruction, bs);
        break;
      case Token.substructure:
        stack[sp++] = getSubstructureSet((String) instruction.value);
        break;
      case Token.selected:
        stack[sp++] = copyBitSet(viewer.getSelectionSet());
        break;
      case Token.subset:
        stack[sp++] = copyBitSet(bsSubset == null ? bsAll() : bsSubset);
        break;
      case Token.hidden:
        stack[sp++] = copyBitSet(viewer.getHiddenSet());
        break;
      case Token.displayed:
        stack[sp++] = invertBitSet(viewer.getHiddenSet());
        break;
      case Token.visible:
        if (!isSyntaxCheck && !refreshed)
          viewer.setModelVisibility();
        refreshed = true;
        stack[sp++] = viewer.getVisibleSet();
        break;
      case Token.clickable:
        // a bit different, because it requires knowing what got slabbed
        if (!isSyntaxCheck && allowRefresh)
          refresh();
        stack[sp++] = viewer.getClickableSet();
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
        stack[sp++] = getAtomBits((String) instruction.value);
        break;
      case Token.spec_atom:
        stack[sp++] = viewer
            .getAtomBits("SpecAtom", (String) instruction.value);
        break;
      case Token.spec_name_pattern:
        stack[sp++] = viewer
            .getAtomBits("SpecName", (String) instruction.value);
        break;
      case Token.bitset:
        stack[sp++] = (BitSet) instruction.value;
        isExpressionBitSet = true;
        break;
      case Token.spec_alternate:
        stack[sp++] = getAtomBits("SpecAlternate", (String) instruction.value);
        break;
      case Token.spec_model:
      //1002 is equivalent to 1.2 when more than one file is present
      case Token.spec_model2:
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE) {
          iModel = ((Integer) instruction.value).intValue();
          if (!viewer.haveFileSet()) {
            stack[sp++] = getAtomBits("SpecModel", iModel);
            break;
          }
          if (iModel < 1000)
            iModel = iModel * 1000000;
          else
            iModel = (iModel / 1000) * 1000000 + iModel % 1000;
        }
        stack[sp++] = bitSetForModelNumberSet(new int[] { iModel }, 1);
        break;
      case Token.spec_resid:
        stack[sp++] = getAtomBits("SpecResid", instruction.intValue);
        break;
      case Token.spec_seqcode:
        stack[sp++] = getAtomBits("SpecSeqcode", instruction.intValue);
        break;
      case Token.spec_chain:
        stack[sp++] = getAtomBits("SpecChain", instruction.intValue);
        break;
      case Token.spec_seqcode_range:
        int seqcodeA = instruction.intValue;
        int seqcodeB = ((Integer) instruction.value).intValue();
        stack[sp++] = getAtomBits("SpecSeqcodeRange", new int[] { seqcodeA,
            seqcodeB });
        break;
      case Token.cell:
        Point3f pt = (Point3f) instruction.value;
        stack[sp++] = getAtomBits("Cell", new int[] { (int) (pt.x * 1000),
            (int) (pt.y * 1000), (int) (pt.z * 1000) });
        break;
      case Token.identifier:
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.sidechain:
      case Token.surface:
        stack[sp++] = lookupIdentifierValue((String) instruction.value);
        break;
      case Token.opLT:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opEQ:
      case Token.opNE:
        bs = stack[sp++] = new BitSet();
        Object val = code[++pc].value;
        if (isSyntaxCheck)
          break;
        int tokOperator = instruction.tok;
        int tokWhat = instruction.intValue;
        boolean isModel = (tokWhat == Token.model);
        boolean isRadius = (tokWhat == Token.radius);
        int tokValue = code[pc].tok;
        comparisonValue = code[pc].intValue;
        boolean isIdentifier = (tokValue == Token.identifier);
        if (isIdentifier) {
          val = viewer.getParameter((String) val);
          if (val instanceof Integer)
            comparisonValue = ((Integer) val).intValue();
          else if (val instanceof Float && isModel)
            comparisonValue = Mmset.modelFileNumberFromFloat(((Float) val)
                .floatValue());
        }
        if (val instanceof Integer || tokValue == Token.integer) {
          comparisonValue *= (Compiler
              .tokAttr(tokWhat, Token.atompropertyfloat) ? 100 : 1);
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
            comparisonValue = (int) (((Float) val).floatValue() * (isRadius ? 250f
                : 100f));
          }
        } else
          invalidArgument();
        if (((String) instruction.value).indexOf("-") >= 0)
          comparisonValue = -comparisonValue;
        comparatorInstruction(tokWhat, tokOperator, comparisonValue, bs);
        break;
      default:
        unrecognizedExpression();
      }
    }
    if (sp != 1) {
      if (allowUnderflow)
        return null;
      evalError(GT._("atom expression compiler error - stack over/underflow"));
    }
    if (!ignoreSubset && bsSubset != null)
      stack[0].and(bsSubset);
    if (tempStatement != null) {
      statement = tempStatement;
      tempStatement = null;
    }
    return stack[0];
  }

  void toggle(BitSet A, BitSet B) {
    for (int i = viewer.getAtomCount(); --i >= 0;) {
      if (!B.get(i))
        continue;
      if (A.get(i)) { //both set --> clear A
        A.clear(i);
      } else {
        A.or(B); //A is not set --> return all on
        return;
      }
    }
  }

  void notSet(BitSet bs) {
    for (int i = viewer.getAtomCount(); --i >= 0;) {
      if (bs.get(i))
        bs.clear(i);
      else
        bs.set(i);
    }
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
    if (logMessages)
      viewer.scriptStatus("lookupValue(" + variable + ")");
    Object value = variables.get(variable);
    boolean isDynamic = false;
    if (value == null) {
      value = variables.get("!" + variable);
      isDynamic = (value != null);
    }
    if (value != null) {
      if (value instanceof Token[]) {
        pushContext();
        value = expression((Token[]) value, -2, true, false);
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

  void comparatorInstruction(int tokWhat, int tokOperator, int comparisonValue,
                             BitSet bs) throws ScriptException {
    int propertyValue = Integer.MAX_VALUE;//Float.NaN;
    BitSet propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    int imax = 0;
    int imin = 0;
    Frame frame = viewer.getFrame();
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = frame.getAtomAt(i);
      switch (tokWhat) {
      case Token.atomno:
        propertyValue = atom.getAtomNumber();
        break;
      case Token.atomIndex:
        propertyValue = i;
        break;
      case Token.elemno:
        propertyValue = atom.getElementNumber();
        break;
      case Token.element:
        propertyValue = atom.getAtomicAndIsotopeNumber();
        break;
      case Token.formalCharge:
        propertyValue = atom.getFormalCharge();
        break;
      case Token.partialCharge:
        propertyValue = (int) (atom.getPartialCharge() * 100);
        break;
      case Token.site:
        propertyValue = atom.getAtomSite();
        break;
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
      case Token.molecule:
        propertyValue = atom.getMoleculeNumber();
        break;
      case Token.temperature: // 0 - 9999
        propertyValue = atom.getBfactor100();
        if (propertyValue < 0)
          continue;
        break;
      case Token.surfacedistance:
        if (frame.getSurfaceDistanceMax() == 0)
          dots(statementLength, Dots.DOTS_MODE_CALCONLY);
        propertyValue = atom.getSurfaceDistance100();
        if (propertyValue < 0)
          continue;
        break;
      case Token.occupancy:
        propertyValue = atom.getOccupancy();
        break;
      case Token.polymerLength:
        propertyValue = atom.getPolymerLength();
        break;
      case Token.resno:
        propertyValue = atom.getResno();
        if (propertyValue == -1)
          continue;
        break;
      case Token._groupID:
        propertyValue = atom.getGroupID();
        if (propertyValue < 0)
          continue;
        break;
      case Token._atomID:
        propertyValue = atom.getSpecialAtomID();
        break;
      case Token._structure:
        propertyValue = atom.getProteinStructureType();
        break;
      case Token.radius:
        propertyValue = atom.getRasMolRadius();
        break;
      case Token.psi:
        propertyValue = (int) (atom.getGroupPsi() * 100f);
        break;
      case Token.phi:
        propertyValue = (int) (atom.getGroupPhi() * 100f);
        break;
      case Token._bondedcount:
        propertyValue = atom.getCovalentBondCount();
        break;
      case Token.file:
        propertyValue = atom.getModelFileIndex() + 1;
        break;
      case Token.model:
        //integer model number -- could be PDB/sequential adapter number
        //or it could be a sequential model in file number when multiple files
        propertyValue = atom.getModelNumber() % 1000000;
        break;
      case -Token.model:
        //float is handled differently
        propertyValue = atom.getModelFileNumber();
        break;
      case Token.atomX:
        propertyValue = (int) (atom.x * 100);
        break;
      case Token.atomY:
        propertyValue = (int) (atom.y * 100);
        break;
      case Token.atomZ:
        propertyValue = (int) (atom.z * 100);
        break;
      default:
        unrecognizedAtomProperty(tokWhat);
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
  }

  BitSet within(Token instruction, BitSet bs) throws ScriptException {
    Object withinSpec = instruction.value;
    if (withinSpec instanceof Float)
      return viewer.getAtomsWithin(((Float) withinSpec).floatValue(), bs);
    String withinStr = "" + withinSpec;
    if (withinSpec instanceof String) {
      if (withinStr.equals("element") || withinStr.equals("site")
          || withinStr.equals("group") || withinStr.equals("chain")
          || withinStr.equals("molecule") || withinStr.equals("model"))
        return viewer.getAtomsWithin(withinStr, bs);
      return viewer.getAtomsWithin("sequence", withinStr, bs);
    }
    unrecognizedParameter("WITHIN", withinStr);
    return null; //can't get here
  }

  BitSet connected(Token instruction, BitSet bs) {
    if (isSyntaxCheck)
      return new BitSet();
    int intType = instruction.intValue;
    int max = ((Integer) instruction.value).intValue();
    int min = max >> 8;
    max = max & 0xFF;
    return viewer.getAtomsConnected(min, max, intType, bs);
  }

  BitSet getSubstructureSet(String smiles) throws ScriptException {
    if (isSyntaxCheck)
      return new BitSet();
    PatternMatcher matcher = new PatternMatcher(viewer);
    try {
      return matcher.getSubstructureSet(smiles);
    } catch (InvalidSmilesException e) {
      evalError(e.getMessage());
    }
    return null;
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
    Token token = getToken(i);
    if (token == null)
      endOfStatementUnexpected();
    return (token.tok == Token.integer ? "" + token.intValue : "" + token.value);
  }

  int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.integer)
        return statement[index].intValue;
    return integerExpected();
  }

  boolean isFloatParameter(int index) {
    if (index >= statementLength)
      return false;
    switch (statement[index].tok) {
    case Token.integer:
    case Token.decimal:
      return true;
    }
    return false;
  }

  float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      Token token = getToken(index);
      switch (token.tok) {
      case Token.integer:
        return token.intValue;
      case Token.decimal:
        return ((Float) token.value).floatValue();
      }
    }
    return numberExpected();
  }

  int floatParameterSet(int i, float[] fparams) throws ScriptException {
    if (i < statementLength && statement[i].tok == Token.leftbrace)
      i++;
    for (int j = 0; j < fparams.length; j++)
      fparams[j] = floatParameter(i++);
    if (i < statementLength && statement[i].tok != Token.rightbrace)
      i++;
    return i;
  }

  String stringParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.string)
        return (String) statement[index].value;
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
    int tok = getToken(index).tok;
    float v = Float.NaN;
    boolean isOffset = (tok == Token.plus);
    if (isOffset)
      index++;
    boolean isPercent = (index + 1 < statementLength && statement[index + 1].tok == Token.percent);
    tok = (index < statementLength ? statement[index].tok : 0);
    switch (tok) {
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
    return (i != statementLength && (statement[i].tok == Token.leftbrace
        || statement[i].tok == Token.expressionBegin || statement[i].tok == Token.xyz));
  }

  Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case Token.expressionBegin:
      return viewer.getAtomSetCenter(expression(++i));
    case Token.leftbrace:
    case Token.xyz:
      return getCoordinate(i, true);
    }
    invalidArgument();
    //impossible return
    return null;
  }

  boolean isCenterParameter(int i) {
    if (i >= statementLength)
      return false;
    switch (statement[i].tok) {
    case Token.dollarsign:
    case Token.expressionBegin:
    case Token.leftbrace:
    case Token.xyz:
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
        center = viewer.getDrawObjectCenter(id);
        if (center == null)
          if (isSyntaxCheck)
            center = new Point3f();
          else
            drawObjectNotDefined(id);
        break;
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.xyz:
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
      switch (statement[i].tok) {
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
        if (!isCoordinate3(i))
          return getPoint4f(i);
      //fall through
      case Token.expressionBegin:
        Point3f pt1 = atomCenterOrCoordinateParameter(i);
        Point3f pt2 = atomCenterOrCoordinateParameter(++iToken);
        Point3f pt3 = atomCenterOrCoordinateParameter(++iToken);
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
    Point3f pt = getCoordinate(i, true, false, true);
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

  int getArgbParam(int index) throws ScriptException {
    if (checkToken(index)) {
      Token token = getToken(index);
      if (token.tok == Token.colorRGB)
        return token.intValue;
    }
    colorExpected();
    //impossible return
    return 0;
  }

  int getArgbOrNoneParam(int index) throws ScriptException {
    if (checkToken(index)) {
      Token token = getToken(index);
      if (token.tok == Token.colorRGB)
        return token.intValue;
      if (token.tok == Token.none)
        return 0;
    }
    colorExpected();
    //impossible return
    return 0;
  }

  int getArgbOrPaletteParam(int index) throws ScriptException {
    if (checkToken(index)) {
      Token token = getToken(index);
      switch (token.tok) {
      case Token.colorRGB:
        return token.intValue;
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

  boolean coordinatesAreFractional;

  boolean isCoordinate3(int i) {
    ignoreError = true;
    boolean isOK = true;
    try {
      getCoordinate(i, true, true, false);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    return isOK;
  }

  Point3f getCoordinate(int i, boolean allowFractional) throws ScriptException {
    return getCoordinate(i, allowFractional, true, false);
  }

  Point3f getCoordinate(int i, boolean allowFractional, boolean doConvert,
                        boolean implicitFractional) throws ScriptException {
    // syntax:  {1/2, 1/2, 1/3} or {0.5/, 0.5, 0.5}
    // ONE fractional sign anywhere is enough to make ALL fractional;
    // denominator of 1 can be implied;
    // commas not necessary if denominator is present or not a fraction
    coordinatesAreFractional = implicitFractional;
    if (!checkToken(i))
      coordinateExpected();
    Token token;
    if ((token = getToken(i)).tok == Token.xyz) {
      return (Point3f) token.value;  
    }
    i++;
    if (token.tok != Token.leftbrace)
      coordinateExpected();
    Point3f pt = new Point3f();
    out: for (int j = i; j + 1 < statementLength; j++) {
      switch (statement[j].tok) {
      case Token.slash:
        coordinatesAreFractional = true;
      case Token.rightbrace:
        break out;
      }
    }
    if (coordinatesAreFractional && !allowFractional)
      evalError(GT._("fractional coordinates are not allowed in this context"));
    pt.x = coordinateValue(i);
    pt.y = coordinateValue(++iToken);
    pt.z = coordinateValue(++iToken);
    if (getToken(++iToken).tok != Token.rightbrace)
      coordinateExpected();
    if (coordinatesAreFractional && doConvert)
      viewer.toCartesian(pt);
    return pt;
  }

  Point4f getPoint4f(int i) throws ScriptException {
    coordinatesAreFractional = false;
    if (!checkToken(i) || getToken(i++).tok != Token.leftbrace)
      coordinateExpected();
    Point4f pt = new Point4f();
    out: for (int j = i; j + 1 < statementLength; j++) {
      switch (statement[j].tok) {
      case Token.slash:
        coordinatesAreFractional = true;
      case Token.rightbrace:
        break out;
      }
    }
    if (coordinatesAreFractional)
      evalError(GT._("fractional coordinates are not allowed in this context"));
    pt.x = coordinateValue(i);
    pt.y = coordinateValue(++iToken);
    pt.z = coordinateValue(++iToken);
    pt.w = coordinateValue(++iToken);
    if (getToken(++iToken).tok != Token.rightbrace)
      coordinateExpected();
    return pt;
  }

  float coordinateValue(int i) throws ScriptException {
    // includes support for fractional coordinates
    float val = floatParameter(i++);
    Token token = getToken(i);
    if (token.tok == Token.slash) {
      token = getToken(++i);
      if (token.tok == Token.integer || token.tok == Token.decimal) {
        val /= floatParameter(i++);
        token = getToken(i);
      }
    }
    iToken = (token.tok == Token.opOr ? i : i - 1);
    return val;
  }

  int theTok;
  
  Token getToken(int i) throws ScriptException {
    if (!checkToken(i))
      endOfStatementUnexpected();
    theTok = statement[i].tok;
    return statement[i];
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
    String what = (statementLength == 1 ? "" : stringParameter(1));
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
    case Token.xyz:
    case Token.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isCoordinate3(i)) {
        pt = getCoordinate(i, true);
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
    int tok;
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    Point3f pt;
    if (statementLength == 2) {
      switch (tok = getToken(1).tok) {
      case Token.on:
      case Token.off:
        setBooleanProperty("navigationMode", tok == Token.on);
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
          String str = (String) statement[i++].value;
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
        case Token.xyz:
        case Token.leftbrace:
          rotAxis.set(getCoordinate(i, true));
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
          if ((tok = getToken(i + 1).tok) == Token.dollarsign) {
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
        state = " on";
        break;
      case Token.off:
        checkLength2();
        stereoMode = JmolConstants.STEREO_NONE;
        state = " off";
        break;
      case Token.colorRGB:
        if (colorpt > 1)
          badArgumentCount();
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt++] = getArgbParam(i);
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
    int operation = JmolConstants.MODIFY_OR_CREATE;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    boolean isTranslucentOrOpaque = false;
    String translucency = null;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BitSet bsBonds = new BitSet();
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
      case Token.expressionBegin:
        if (++nAtomSets > 2)
          badArgumentCount();
        if (haveType || isColorOrRadius)
          invalidParameterOrder();
        atomSets[atomSetCount++] = expression(i);
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
      case Token.colorRGB:
        color = getArgbParam(i);
        isColorOrRadius = true;
        break;
      case Token.none:
      case Token.delete:
        if (++i != statementLength)
          invalidParameterOrder();
        operation = JmolConstants.connectOperationFromString("delete");
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
        operation = JmolConstants.MODIFY_ONLY;
    }
    int n = viewer.makeConnections(distances[0], distances[1], bondOrder,
        operation, atomSets[0], atomSets[1], bsBonds);
    if (isDelete) {
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
    viewer.scriptStatus(GT._("{0} connections modified or created", n));
  }

  void getProperty() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String param = optParameterAsString(2);
    BitSet bs = (statementLength > 2
        && getToken(2).tok == Token.expressionBegin ? expression(2) : null);
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
    int tok = getToken(i).tok;
    int argb;
    if (tok == Token.colorRGB || tok == Token.none) {
      argb = getArgbOrNoneParam(i);
      if (!isSyntaxCheck)
        viewer.setBackgroundArgb(argb);
      return;
    }
    argb = getArgbOrNoneParam(i + 1);
    int iShape = getShapeType(tok);
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
    int tok = getToken(1).tok;
    switch (tok) {
    case Token.dollarsign:
      colorNamedObject(2);
      return;
    case Token.colorRGB:
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
    case Token.user:
    case Token.monomer:
    case Token.molecule:
    case Token.altloc:
    case Token.insertion:
    case Token.translucent:
    case Token.opaque:
      colorObject(Token.atom, 1);
      return;
    case Token.jmol:
    case Token.rasmol:
      colorObject(Token.atom, 1);
      return;
    case Token.rubberband:
      argb = getArgbParam(2);
      if (!isSyntaxCheck)
        viewer.setRubberbandArgb(argb);
      return;
    case Token.background:
      argb = getArgbOrNoneParam(2);
      if (!isSyntaxCheck)
        viewer.setBackgroundArgb(argb);
      return;
    case Token.selectionHalo:
      argb = getArgbOrNoneParam(2);
      if (isSyntaxCheck)
        return;
      viewer.loadShape(JmolConstants.SHAPE_HALOS);
      setShapeProperty(JmolConstants.SHAPE_HALOS, "argbSelection", new Integer(
          argb));
      return;
    case Token.identifier:
    case Token.hydrogen:
      //color element
      argb = getArgbOrPaletteParam(2);
      String str = parameterAsString(1);
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
    case Token.bond:
      tok = Token.bonds; // special hack for bond/bonds confusion
    // fall through
    default:
      colorObject(tok, 2);
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
    int tok = getToken(index).tok;
    if (tok == Token.background) {
      colorOrBgcolor = "bgcolor";
      tok = getToken(++index).tok;
    }
    if (tok == Token.translucent || tok == Token.opaque)
      translucentOrOpaque = parameterAsString(index++);
    String modifier = "";
    if (shapeType < 0) {
      //geosurface
      shapeType = -shapeType;
      modifier = "Surface";
    }
    if (index < statementLength) {
      if ((tok = getToken(index).tok) == Token.colorRGB) {
        int argb = getArgbParam(index);
        colorvalue = (argb == 0 ? null : new Integer(argb));
      } else {
        // "cpk" value would be "spacefill"
        byte pid = (tok == Token.spacefill ? JmolConstants.PALETTE_CPK
            : JmolConstants.getPaletteID(parameterAsString(index)));
        if (pid == JmolConstants.PALETTE_UNKNOWN
            || pid == JmolConstants.PALETTE_TYPE
            && shapeType != JmolConstants.SHAPE_HSTICKS)
          invalidArgument();
        colorvalue = new Byte((byte) pid);
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
  String[] dataLabelString;

  void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
    switch (iToken = statementLength) {
    case 3:
      dataString = parameterAsString(2);
    //fall through
    case 2:
      dataLabel = parameterAsString(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!isSyntaxCheck)
          Viewer.setData(null, null);
        return;
      }
      if (statementLength > 2)
        break;
    default:
      badArgumentCount();
    }
    String dataType = dataLabel + " ";
    dataType = dataType.substring(0, dataType.indexOf(" "));
    dataLabelString = new String[2];
    dataLabelString[0] = dataLabel;
    dataLabelString[1] = dataString;
    boolean isModel = dataType.equalsIgnoreCase("model");
    if (!isSyntaxCheck || isScriptCheck && isModel && fileOpenCheck)
      Viewer.setData(dataType, dataLabelString);
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
      variables.put(variable, bs);
      setStringProperty("@" + variable, StateManager.escape(bs));
    }
  }

  void echo() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String text = optParameterAsString(1);
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

  void label() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(1);
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
    } else if (statement[i + 1].tok == Token.leftbrace
        || statement[i + 1].tok == Token.integer) {
      if ((filename = parameterAsString(i++)).length() == 0)
        filename = viewer.getFullPathName();
      loadScript.append(" " + StateManager.escape(filename));
      if (getToken(i).tok == Token.integer) {
        params[0] = intParameter(i++);
        loadScript.append(" " + params[0]);
      }
      if (i < statementLength 
          && (getToken(i).tok == Token.leftbrace || theTok == Token.xyz)) {
        unitCells = getCoordinate(i, false);
        i = iToken + 1;
        params[1] = (int) unitCells.x;
        params[2] = (int) unitCells.y;
        params[3] = (int) unitCells.z;
        loadScript.append(" " + StateManager.escape(unitCells));
        int iGroup = -1;
        int[] p;
        if (i < statementLength && getToken(i).tok == Token.spacegroup) {
          ++i;
          String spacegroup = Viewer.simpleReplace(parameterAsString(i++),
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
        if (i < statementLength && getToken(i).tok == Token.unitcell) {
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
        if (statement[2].tok == Token.all) {
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
    countPlusIndexes[0] = 0;
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
      } else {
        if (++countPlusIndexes[0] > 4)
          badArgumentCount();
        countPlusIndexes[countPlusIndexes[0]] = atomIndex;
      }
    }
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

  void reset() {
    viewer.reset();
  }

  void initialize() {
    viewer.initialize();
    zap();
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
      switch (statement[1].tok) {
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
      case Token.xyz:
        // {X, Y, Z}
        Point3f pt = getCoordinate(i, true);
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
        rotCenter = viewer.getDrawObjectCenter(axisID);
        rotAxis = viewer.getDrawObjectAxis(axisID);
        if (rotCenter == null)
          if (isSyntaxCheck)
            rotCenter = new Point3f();
          else
            drawObjectNotDefined(axisID);
        points[nPoints++].set(rotCenter);
        break;
      case Token.opOr:
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
      case Token.expressionBegin:
        BitSet bs = expression(++i);
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

  void script() throws ScriptException {
    // token allows for only 1 or 2 parameters
    if (getToken(1).tok != Token.string)
      filenameExpected();
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
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
    String filename = stringParameter(1);
    if (loadScriptFileInternal(filename)) {
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
    switch (statement[1].tok) {
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
      viewer.hide(bs, tQuiet || isExpressionBitSet);
  }

  void display() throws ScriptException {
    if (statementLength == 1) {
      viewer.display(bsAll(), null, tQuiet);
      return;
    }
    BitSet bs = expression(1);
    if (!isSyntaxCheck)
      viewer.display(bsAll(), bs, tQuiet || isExpressionBitSet);
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
    if (statementLength == 5 && statement[2].tok == Token.bonds
        && statement[3].tok == Token.bitset) {
      if (!isSyntaxCheck)
        viewer.selectBonds((BitSet) statement[3].value);
      return;
    }

    if (statementLength == 5 && statement[2].tok == Token.monitor
        && statement[3].tok == Token.bitset) {
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "select",
          statement[3].value);
      return;
    }
    if (statementLength == 1) {
      viewer.select(null, tQuiet || scriptLevel > scriptReportingLevel);
      return;
    }
    BitSet bs = expression(1);
    if (!isSyntaxCheck)
      viewer.select(bs, tQuiet || isExpressionBitSet
          || scriptLevel > scriptReportingLevel);
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
    Point3f pt = getCoordinate(1, true);
    if (!isSyntaxCheck)
      viewer.setAtomCoordRelative(pt);
  }

  void translate() throws ScriptException {
    float percent = floatParameter(2);
    if (percent > 100 || percent < -100)
      numberOutOfRange(-100, 100);
    if (statement[1].tok == Token.identifier) {
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
    switch (statementLength) {
    case 2:
      break;
    case 3:
      int tok = getToken(1).tok;
      if (tok == Token.integer && getToken(2).tok == Token.percent
          || tok == Token.plus && isFloatParameter(2))
        break;
      invalidArgument();
    }
    return getToken(1).tok;
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
    if (statementLength == 2 && statement[1].tok == Token.calculate) {
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
      if (getToken(1).tok == Token.identifier
          && parameterAsString(1).equalsIgnoreCase("scale")) {
        float scale = floatParameter(2);
        if (scale < -10 || scale > 10)
          numberOutOfRange(-10f, 10f);
        viewer.setVectorScale(scale);
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
      case Token.expressionBegin:
        propertyName = (statement[i + 1].tok == Token.bitset ? "atomBitset"
            : iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = expression(i);
        i = iToken;
        iHaveAtoms = true;
        break;
      case Token.leftbrace:
      case Token.xyz:
        // {X, Y, Z}
        Point3f pt = getCoordinate(i, true);
        i = iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case Token.bond:
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
          if (statement[i].tok == Token.integer) {
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
        idSeen = true;
        propertyName = "thisID"; // might be "molecular"
        propertyValue = cmd.toLowerCase();
        break;
      default:
        invalidArgument();
      }
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
      period = viewer.getDefaultVibrationPeriod();
      break;
    case Token.off:
      checkLength2();
      period = 0;
      break;
    case Token.integer:
      checkLength2();
      period = intParameter(1);
      break;
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
        setFloatProperty("vibrationPeriod", -period);
        return;
      } else {
        invalidArgument();
      }
    default:
      period = -1;
    }
    if (period < 0)
      invalidArgument();
    setFloatProperty("vibrationPeriod", period);
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
        if (getToken(++ipt).tok != Token.bitset)
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
        if (!haveFileSet && m > 1000000)
          m = pt;
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
      showString(viewer.getAllSettings());
      return;
    }
    int val = 0;
    int n = 0;
    String key;
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
    case Token.strands:
      setStrands();
      return;
    case Token.spin:
      setSpin();
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
        setBooleanProperty("specular", booleanParameter(2));
        return;
      }
    //fall through
    case Token.ambient:
    case Token.diffuse:
    case Token.specpercent:
      checkLength3();
      val = intParameter(2);
      if (val > 100 || val < 0)
        numberOutOfRange(0, 100);
      setIntProperty(parameterAsString(1), val);
      return;
    case Token.specpower:
      checkLength3();
      val = intParameter(2);
      if (val > 100)
        numberOutOfRange(0, 100);
      if (val >= 0) {
        setIntProperty((String) "specularPower", val);
        return;
      }
      if (val < -10 || val > -1)
        numberOutOfRange(-10, -1);
      setIntProperty("specularExponent", -val);
      return;
    case Token.specexponent:
      checkLength3();
      if (val == 0)
        val = intParameter(2);
      if (val > 10 || val < 1)
        numberOutOfRange(1, 10);
      setIntProperty("specularExponent", val);
      return;
    // not implemented
    case Token.backfade:
    case Token.cartoon:
    case Token.hourglass:
    case Token.kinemage:
    case Token.menus:
    case Token.mouse:
    case Token.shadow:
    case Token.slabmode:
    case Token.transparent:
    case Token.vectps:
    case Token.write:
    // fall through to identifier

    case Token.bonds:
    case Token.frank:
    case Token.help:
    case Token.hetero:
    case Token.hydrogen:
    case Token.identifier:
    case Token.radius:
    case Token.solvent:
      key = parameterAsString(1);
      if (key.charAt(0) == '_') //these cannot be set by user
        invalidArgument();

      //these next are not reported

      if (key.toLowerCase().indexOf("label") == 0) {
        setLabel(key.substring(5));
        return;
      } else if (key.equalsIgnoreCase("defaultColorScheme")) {
        setDefaultColors();
        return;
      } else if (key.equalsIgnoreCase("measurementNumbers")) {
        setMonitor(2);
        return;
      } else if (key.equalsIgnoreCase("defaultLattice")) {
        Point3f pt;
        if (getToken(2).tok == Token.integer) {
          int i = intParameter(2);
          pt = new Point3f(i, i, i);
        } else {
          pt = getSetCoordinate(2, false, false);
        }
        if (!isSyntaxCheck)
          viewer.setDefaultLattice(pt);
        return;
      } else if (key.equalsIgnoreCase("toggleLabel")) { //from PickingManager
        BitSet bs = expression(2);
        if (!isSyntaxCheck)
          viewer.togglePickingLabel(bs);
        return;
      } else if (key.equalsIgnoreCase("logLevel")) {
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
        Viewer.setLogLevel(ilevel);
        Logger.info("logging level set to " + ilevel);
        return;
      } else if (key.equalsIgnoreCase("backgroundModel")) {
        checkLength3();
        if (!isSyntaxCheck)
          viewer.setBackgroundModel(statement[2].intValue);
        return;
      }
      if (setParameter(key)) {
        if (isSyntaxCheck)
          return;
      } else {
        Object v = parameterExpression(2, false);
        if (isSyntaxCheck)
          return;
        if (v instanceof Boolean) {
          setBooleanProperty(key, ((Boolean)v).booleanValue());
        } else if (v instanceof Integer) {
          setIntProperty(key, ((Integer)v).intValue());
        } else if (v instanceof Float) {
          setFloatProperty(key, ((Float)v).floatValue());
        } else if (v instanceof String) {
          setStringProperty(key, (String)v);
        } else if (v instanceof Point3f) {
          drawPoint(key, (Point3f)v, false);
          showString("draw " + key + " " + StateManager.escape((Point3f)v)
              + "; draw off");
          return;
        }
      }
      if (!isSyntaxCheck && scriptLevel <= scriptReportingLevel)
        showString(key + " = " + viewer.getParameterEscaped(key));
    }
  }

  boolean setParameter(String key) throws ScriptException {
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

  Object parameterExpression(int pt, boolean TFonly) throws ScriptException {
    Object v;
    Rpn rpn = new Rpn(16);
    for (int i = pt; i < statementLength; i++) {
      v = null;
      Token token = getToken(i);
      switch (token.tok) {
      case Token.on:
      case Token.off:
      case Token.integer:
      case Token.decimal:
      case Token.string:
        rpn.addX(token);
        break;
      case Token.identifier:
        if (isSyntaxCheck)
          v = Boolean.TRUE;
        else
          v = viewer.getParameter(parameterAsString(i));
        break;
      case Token.expressionBegin:
        v = getExpressionValue(i);
        i = iToken;
        break;
      default:
        if (!Compiler.tokAttr(token.tok, Token.mathop))
          invalidArgument();
        if (!rpn.addOp(token))
          invalidArgument();
      }
      if (v != null)
        if (v instanceof Boolean) {
          rpn.addX(((Boolean) v).booleanValue());
        } else if (v instanceof Integer) {
          rpn.addX(new Token(Token.integer, ((Integer) v).intValue()));
        } else if (v instanceof Float) {
          rpn.addX(new Token(Token.decimal, v));
        } else if (v instanceof String) {
          rpn.addX(new Token(Token.string, v));
        } else if (v instanceof Point3f) {
          rpn.addX(new Token(Token.xyz, v));
        } else {
          invalidArgument();
        }
    }
    Token result = rpn.getResult();
    if (result == null)
      endOfStatementUnexpected();
    if (TFonly)
      return new Boolean(Token.bValue(result));
    switch (result.tok) {
    case Token.on:
    case Token.off:
      return new Boolean(result == Token.tokenOn);
    case Token.integer:
      return new Integer(result.intValue);
    case Token.decimal:
    case Token.string:
    case Token.xyz:
    default:
      return result.value;
    }
  }

  void getBitsetItem(int i, BitSet bs) throws ScriptException {
    if (bs == null || i >= statementLength)
      return;
    if (statement[i].tok == Token.expressionEnd) {
      if (i + 2 < statementLength
          && statement[i + 1].tok == Token.expressionBegin)
        i += 2;
    }
    if (statement[i].tok != Token.identifier)
      return;
    String s = parameterAsString(i);
    if (s.charAt(0) != '_') {
      iToken--;
      return;
    }
    int pt;
    try {
      pt = Integer.parseInt(s.substring(1));
    } catch (Exception e) {
      return;
    }
    int len = bs.size();
    int n = 0;
    for (int j = 0; j < len; j++)
      if (bs.get(j) && ++n != pt)
        bs.clear(j);
  }

  String getBitsetIdent(BitSet bs) {
    if (bs == null)
      return "";
    StringBuffer s = new StringBuffer();
    int len = bs.size();
    int n = 0;
    Frame frame = viewer.getFrame();
    for (int j = 0; j < len; j++)
      if (bs.get(j)) {
        if (n++ > 0)
          s.append(", ");
        s.append(frame.getAtomAt(j).getIdentity());
      }
    return s.toString();
  }

  Object getExpressionValue(int i) throws ScriptException {
    Point3f ptRef = null;
    Point3f pt = null;
    BitSet bs = expression(statement,
        getToken(i).tok == Token.leftbrace ? i + 1 : i, true, true);
    if (bs == null)
      pt = getSetCoordinate(i, false, true);
    else
      getBitsetItem(iToken + 1, bs);
    i = iToken + 1;
    if (i >= statementLength || statement[i].tok != Token.dot) {
      if (pt != null)
        return pt;
      return new Integer(viewer.cardinalityOf(bs));
    }
    int tok = getToken(++i).tok;
    if (tok == Token.identifier) {
      String s = parameterAsString(i).toLowerCase();
      if (s.equals("x"))
        tok = Token.atomX;
      else if (s.equals("y"))
        tok = Token.atomY;
      else if (s.equals("z"))
        tok = Token.atomZ;
      else if (s.equals("bondcount"))
        tok = Token._bondedcount;
      else if (s.equals("xyz"))
        tok = Token.xyz;
      else
        invalidArgument();
    } else if (tok == Token.distance) {
      BitSet bs2 = expression(statement,
          getToken(++i).tok == Token.leftbrace ? i + 1 : i, false, true);
      getBitsetItem(iToken + 1, bs2);
      ptRef = (bs2 == null ? getSetCoordinate(i, false, true) : viewer
          .getAtomSetCenter(bs2));
    } else if (tok == Token.ident) {
      if (bs == null)
        invalidArgument();
      return getBitsetIdent(bs);
    }
    if (pt != null)
      return new Float(pt.distance(ptRef));
    return averageValue(bs, tok, ptRef);
  }

  Object averageValue(BitSet bs, int tok, Point3f ptRef) throws ScriptException {
    int iv = 0;
    float fv = 0;
    int n = 0;
    Point3f pt = new Point3f();
    boolean isInt = true;
    int atomCount = viewer.getAtomCount();
    Frame frame = viewer.getFrame();
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        n++;
        Atom atom = frame.getAtomAt(i);
        switch (tok) {
        case Token.atomno:
          iv += atom.getAtomNumber();
          continue;
        case Token.atomIndex:
          iv += i;
          continue;
        case Token.elemno:
          iv += atom.getElementNumber();
          continue;
        case Token.element:
          iv += atom.getAtomicAndIsotopeNumber();
          continue;
        case Token.formalCharge:
          iv += atom.getFormalCharge();
          continue;
        case Token.site:
          iv += atom.getAtomSite();
          continue;
        case Token.symop:
          // a little weird
          BitSet bsSym = atom.getAtomSymmetry();
          int len = bsSym.size();
          int p = 0;
          for (int k = 0; k < len; k++)
            if (bsSym.get(k)) {
              iv += k + 1;
              p++;
            }
          n += p - 1;
          continue;
        case Token.molecule:
          iv += atom.getMoleculeNumber();
          continue;
        case Token.occupancy:
          iv += atom.getOccupancy();
          continue;
        case Token.polymerLength:
          iv += atom.getPolymerLength();
          continue;
        case Token.resno:
          iv += atom.getResno() + 1;
          continue;
        case Token._groupID:
          iv += atom.getGroupID() + 1;
          continue;
        case Token._atomID:
          iv += atom.getSpecialAtomID();
          continue;
        case Token._structure:
          iv += atom.getProteinStructureType();
          continue;
        case Token._bondedcount:
          iv += atom.getCovalentBondCount();
          continue;
        case Token.file:
          iv += atom.getModelFileIndex() + 1;
          continue;
        case Token.model:
          iv += atom.getModelNumber() % 1000000;
          continue;
        case Token.partialCharge:
          fv += atom.getPartialCharge();
          break;
        case Token.temperature: // 0 - 9999
          fv += atom.getBfactor100() / 100f;
          break;
        case Token.surfacedistance:
          if (frame.getSurfaceDistanceMax() == 0)
            dots(statementLength, Dots.DOTS_MODE_CALCONLY);
          fv += atom.getSurfaceDistance100() / 100f;
          break;
        case Token.radius:
          fv += atom.getRadius();
          break;
        case Token.psi:
          fv += atom.getGroupPsi();
          break;
        case Token.phi:
          fv += atom.getGroupPhi();
          break;
        case Token.atomX:
          fv += atom.x;
          break;
        case Token.atomY:
          fv += atom.y;
          break;
        case Token.atomZ:
          fv += atom.z;
          break;
        case Token.distance:
          fv += atom.distance(ptRef);
          break;
        case Token.xyz:
          pt.add(atom);
          break;
        default:
          unrecognizedAtomProperty(tok);
        }
        isInt = false;
      }
    if (tok == Token.xyz)
      return (n == 0 ? pt : new Point3f(pt.x / n, pt.y / n, pt.z / n));
    if (n == 0)
      return new Float(Float.NaN);
    if (isInt && (iv / n) * n == iv)
      return new Integer(iv / n);
    return new Float((isInt ? iv * 1f : fv) / n);
  }

  void setAxes(int index) throws ScriptException {
    if (statementLength == 1) {
      setShapeSize(JmolConstants.SHAPE_AXES, 1);
      return;
    }
    // set axes scale x.xxx
    if (statementLength == index + 2
        && statement[index].tok == Token.identifier
        && ((String) statement[index].value).equalsIgnoreCase("scale")) {
      setShapeProperty(JmolConstants.SHAPE_AXES, "scale", new Float(
          floatParameter(index + 1)));
      return;
    }
    setShapeSize(JmolConstants.SHAPE_AXES, getSetAxesTypeMad(index));
  }

  void setBoundbox(int index) throws ScriptException {
    setShapeSize(JmolConstants.SHAPE_BBCAGE, getSetAxesTypeMad(index));
  }

  void setUnitcell(int index) throws ScriptException {
    if (statementLength == 1) {
      setShapeSize(JmolConstants.SHAPE_UCCAGE, 1);
      return;
    }
    if (statementLength == index + 1) {
      if (getToken(index).tok == Token.integer && intParameter(index) >= 111) {
        if (!isSyntaxCheck)
          viewer.setCurrentUnitCellOffset(intParameter(index));
      } else {
        setShapeSize(JmolConstants.SHAPE_UCCAGE, getSetAxesTypeMad(index));
      }
      return;
    }
    // .xyz here?
    Point3f pt = (index == 1 ? getCoordinate(1, true, false, true)
        : getSetCoordinate(2, false, false));
    if (!isSyntaxCheck)
      viewer.setCurrentUnitCellOffset(pt);
  }

  Point3f getSetCoordinate(int index, boolean integerOnly, boolean doConvert)
      throws ScriptException {
    // { x y z } or {a/b c/d e/f} are encoded in SET commands as seqcodes and model numbers
    // so we decode them here. It's a bit of a pain, but it isn't too bad.
    float[] coord = new float[3];
    int n = -1;
    coordinatesAreFractional = false;
    out: for (int i = index; i < statement.length; i++) {
      switch (getToken(i).tok) {
      case Token.expressionEnd:
        break out;
      case Token.spec_seqcode:
        if (++n > 2)
          invalidArgument();
        coord[n] = ((Integer) statement[i].value).intValue();
        break;
      case Token.spec_model:
        if (integerOnly)
          invalidArgument();
        coord[n] /= ((Integer) statement[i].value).intValue();
        coordinatesAreFractional = true;
        break;
      case Token.spec_model2:
        if (integerOnly)
          invalidArgument();
        if (++n > 2)
          invalidArgument();
        coord[n] = ((Float) statement[i].value).floatValue();
        break;
      }
    }
    if (n != 2)
      invalidArgument();
    Point3f pt = new Point3f(coord[0], coord[1], coord[2]);
    if (coordinatesAreFractional && doConvert && !isSyntaxCheck) {
      viewer.toCartesian(pt);
    }
    return pt;
  }

  void setFrank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  void setDefaultColors() throws ScriptException {
    checkLength3();
    String type = "" + statement[2].value;
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
    if (i < statementLength && statement[i].tok == Token.percent) {
      type = "%xpos";
      i++;
    } else {
      type = "xpos";
    }
    setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
    pos = intParameter(i++);
    propertyValue = new Integer(pos);
    if (i < statementLength && statement[i].tok == Token.percent) {
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
    switch (statement[index].tok) {
    case Token.on:
      showMeasurementNumbers = true;
    case Token.off:
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
          showMeasurementNumbers ? Boolean.TRUE : Boolean.FALSE);
      return;
    case Token.identifier:
      String units = (String) statement[index].value;
      if (!StateManager.isMeasurementUnit(units))
        unrecognizedParameter("SET MEASUREMENTS", units);
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

  void setStrands() throws ScriptException {
    int strandCount = 5;
    if (statementLength == 3)
      if ((strandCount = intParameter(2)) < 0 || strandCount > 20)
        numberOutOfRange(0, 20);
    setShapeProperty(JmolConstants.SHAPE_STRANDS, "strandCount", new Integer(
        strandCount));
  }

  void setSpin() throws ScriptException {
    checkLength4();
    int value = (int) floatParameter(3);
    if (getToken(2).tok == Token.identifier) {
      String str = parameterAsString(2).toLowerCase();
      int pt = "x y z f fp fps".indexOf(str);
      //        0 2 4      11
      if (pt >= 0)
        switch (pt) {
        case 0:
          viewer.setSpinX(value);
          return;
        case 2:
          viewer.setSpinY(value);
          return;
        case 4:
          viewer.setSpinZ(value);
          return;
        case 11:
          viewer.setSpinFps(value);
          return;
        }
    }
    unrecognizedParameter("SET SPIN", parameterAsString(2));
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
    String type = "SPT";
    boolean isCoord = false;
    switch (tok) {
    case Token.coord:
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
      if (type.equals("image"))
        pt++;
      else
        type = "image";
      break;
    }
    if (pt == statementLength)
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
      viewer.createImage(null, type, 100);
      return;
    }

    //write [optional image|history|state] [JPG|JPG64|PNG|PPM|SPT] "filename"
    //write script "filename"

    if (pt + 2 == statementLength) {
      type = val.toUpperCase();
      pt++;
    }
    if (pt + 1 != statementLength)
      badArgumentCount();
    String fileName = null;
    switch (getToken(pt).tok) {
    case Token.identifier:
    case Token.string:
      fileName = parameterAsString(pt);
      //write filename.xxx  gets separated as filename .spt
      if (fileName.charAt(0) == '.' && pt == 2) {
        fileName = parameterAsString(1) + fileName;
        type = "image";
      }
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
    boolean isImage = (";JPEG;JPG64;JPG;PPM;PNG;".indexOf(";" + type + ";") >= 0);
    if (!isImage && ";SPT;HIS;MO;ISO;".indexOf(";" + type + ";") < 0)
      evalError(GT._("write what? {0} or {1} \"filename\"", new Object[] {
          "STATE|HISTORY|IMAGE|ISOSURFACE|MO CLIPBOARD",
          "JPG|JPG64|PNG|PPM|SPT|JVXL" }));
    if (isSyntaxCheck)
      return;
    if (isImage && isApplet)
      evalError(GT._("The {0} command is not available for the applet.",
          "WRITE image"));
    int quality = Integer.MIN_VALUE;
    String data = type.intern();
    if (data == "SPT")
      if (isCoord) {
        BitSet tainted = viewer.getTaintedAtoms();
        viewer.setAtomCoordRelative(new Point3f(0, 0, 0));
        data = (String) viewer.getProperty("string", "stateInfo", null);
        viewer.setTaintedAtoms(tainted);
      } else {
        data = (String) viewer.getProperty("string", "stateInfo", null);
      }
    else if (data == "HIS")
      data = viewer.getSetHistory(Integer.MAX_VALUE);
    else if (data == "MO")
      data = getMoJvxl(Integer.MAX_VALUE);
    else if (data == "ISO") {
      if ((data = getIsosurfaceJvxl()) == null)
        evalError(GT._("No data available"));
    } else
      quality = 100;
    viewer.createImage(fileName, data, quality);
    viewer.scriptStatus("type=" + type + "; file=" + fileName);
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
        showString(viewer.getAllSettings());
      return;
    case Token.url:
      // in a new window
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          viewer.showUrl(viewer.getFullPathName());
        return;
      }
      String fileName = stringParameter(2);
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
      msg = "selectionHalosEnabled = " + viewer.getSelectionHaloEnabled();
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
      String type = ((len = statementLength) == 3 ? stringParameter(2) : null);
      if (!isSyntaxCheck) {
        String[] data = (type == null ? dataLabelString : viewer.getData(type));
        msg = (data == null ? "no data" : "data \"" + data[0] + "\"\n"
            + data[1]);
      }
      break;
    case Token.spacegroup:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getSpaceGroupInfoText(null);
        break;
      }
      String sg = stringParameter(2);
      if (!isSyntaxCheck)
        msg = viewer
            .getSpaceGroupInfoText(Viewer.simpleReplace(sg, "''", "\""));
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
      value = stringParameter(2);
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
    Logger.warn(str);
    viewer.scriptEcho(str);
  }

  String getIsosurfaceJvxl() {
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
      int tok = getToken(i).tok;
      switch (tok) {
      case Token.identifier:
        propertyValue = statement[i].value;
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
        idSeen = true;
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
              data = Viewer.simpleReplace(data, " ", "\n");
              propertyName = "bufferedReaderOnePerLine";
            }
            data = Viewer.simpleReplace(data, "{", " ");
            data = Viewer.simpleReplace(data, ",", " ");
            data = Viewer.simpleReplace(data, "}", " ");
            data = Viewer.simpleReplace(data, "|", "\n");
            data = Viewer.simpleReplace(data, "\n\n", "\n");
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
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_PMESH, tok))
          invalidArgument();
        continue;
      }
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
      int tok = getToken(i).tok;
      switch (tok) {
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case Token.identifier:
        propertyValue = statement[i].value;
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
          Point3f pt = getCoordinate(++i, true);
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
        if (str.equalsIgnoreCase("LENGTH")) {
          propertyValue = new Float(floatParameter(++i));
          propertyName = "length";
          break;
        }
        if (idSeen)
          invalidArgument();
        idSeen = true;
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
        if (++i < statementLength && statement[i].tok == Token.translucent) {
          isTranslucent = true;
          i++;
        }
        if (i < statementLength && statement[i].tok == Token.colorRGB) {
          colorArgb = getArgbParam(i);
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
      case Token.xyz:
        // {X, Y, Z}
        Point3f pt = getCoordinate(i, true);
        i = iToken;
        propertyName = "coord";
        propertyValue = pt;
        havePoints = true;
        break;
      case Token.expressionBegin:
        propertyName = "atomSet";
        propertyValue = expression(++i);
        i = iToken;
        havePoints = true;
        break;
      case Token.translucent:
        isTranslucent = true;
        break;
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, tok))
          invalidArgument();
      }
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
      case Token.opOr:
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
        color = getArgbParam(i);
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
          if (statementLength > i + 2 && statement[i + 2].tok == Token.bitset) {
            propertyName = "toBitSet";
            propertyValue = statement[i + 2].value;
            i += 3;
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
      case Token.expressionBegin:
        if (typeSeen)
          invalidParameterOrder();
        if (++nAtomSets > 2)
          badArgumentCount();
        if (setPropertyName == "to")
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = expression(++i);
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
      case Token.expressionBegin:
        propertyName = "select";
        propertyValue = expression(++i);
        i = iToken;
        break;
      case Token.color:
        if (++i < statementLength && statement[i].tok == Token.colorRGB) {
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB",
              new Integer(getArgbParam(i)));
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB",
              new Integer(getArgbParam(i + 1 < statementLength
                  && statement[i + 1].tok == Token.colorRGB ? ++i : i)));
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
        propertyValue = stringParameter(++i);
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
          propertyValue = stringParameter(++i);
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
    int tok = getToken(1).tok;
    switch (tok) {
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
      if (2 < statementLength && statement[2].tok == Token.colorRGB) {
        setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(
            getArgbParam(2)));
        if (3 < statementLength && statement[3].tok == Token.colorRGB)
          setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(
              getArgbParam(3)));
        break;
      }
      invalidArgument();
    case Token.identifier:
      str = parameterAsString(1);
      if (str.equalsIgnoreCase("CUTOFF")) {
        if (2 < statementLength && statement[2].tok == Token.plus) {
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
          propertyValue = stringParameter(2);
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
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, tok))
        invalidArgument();
      return;
    }
    if (propertyName != null)
      setShapeProperty(JmolConstants.SHAPE_MO, propertyName, propertyValue);
    if (moNumber != Integer.MAX_VALUE) {
      if (2 < statementLength && statement[2].tok == Token.string)
        title = stringParameter(2);
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
    String str;
    int modelIndex = (isSyntaxCheck ? 0 : viewer.getDisplayModelIndex());
    if (modelIndex < 0)
      evalError(GT._(
          "the {0} command requires that only one model be displayed",
          "ISOSURFACE"));
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      Token token = getToken(i);
      switch (token.tok) {
      case Token.select:
        propertyName = "select";
        ++i;
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
      case Token.colorRGB:
        if (i != signPt && i != signPt + 1)
          invalidParameterOrder();
        propertyName = "colorRGB";
        propertyValue = new Integer(getArgbParam(i));
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
          propertyValue = getCoordinate(++i, false);
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
          propertyValue = new Integer(i + 1 < statementLength
              && statement[i + 1].tok == Token.integer ? intParameter(++i) : 0);
          break;
        }
        if (str.equalsIgnoreCase("PHASE")) {
          propertyName = "phase";
          propertyValue = (i + 1 < statementLength
              && statement[i + 1].tok == Token.string ? stringParameter(++i)
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
          v.add(getCoordinate(i, false));
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
        propertyValue = token.value;
      //fall through for identifiers
      case Token.all:
        if (idSeen)
          invalidArgument();
        propertyName = "thisID";
        idSeen = true;
        break;
      case Token.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        setShapeProperty(iShape, "lcaoType", lcaoType);
        switch (statement[++i].tok) {
        case Token.expressionBegin:
          propertyName = "lcaoCartoon";
          BitSet bs = expression(++i);
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
        propertyName = (token.tok == Token.sasurface ? "sasurface" : "solvent");
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
        String filename = (String) token.value;
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
        if (i + 1 < statementLength && statement[i + 1].tok == Token.integer)
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
        if (!setMeshDisplayProperty(iShape, token.tok))
          invalidArgument();
      }
      if (propertyName != null)
        setShapeProperty(iShape, propertyName, propertyValue);
    }
    if (planeSeen && !surfaceObjectSeen) {
      setShapeProperty(iShape, "nomap", new Float(0));
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

  void evalWarning(String message) {
    new ScriptException(message);
  }

  void unrecognizedCommand() throws ScriptException {
    evalError(GT._("unrecognized command") + ": " + statement[0].value);
  }

  void unrecognizedAtomProperty(int propnum) throws ScriptException {
    evalError(GT._("unrecognized atom property") + ": " + propnum);
  }

  void filenameExpected() throws ScriptException {
    evalError(GT._("filename expected"));
  }

  void booleanExpected() throws ScriptException {
    evalError(GT._("boolean expected"));
  }

  void booleanOrPercentExpected() throws ScriptException {
    evalError(GT._("boolean or percent expected"));
  }

  void booleanOrNumberExpected() throws ScriptException {
    evalError(GT._("boolean or number expected"));
  }

  void booleanOrNumberExpected(String orWhat) throws ScriptException {
    evalError(GT._("boolean, number, or {0} expected", "\"" + orWhat + "\""));
  }

  void expressionOrDecimalExpected() throws ScriptException {
    evalError(GT._("(atom expression) or decimal number expected"));
  }

  void expressionOrIntegerExpected() throws ScriptException {
    evalError(GT._("(atom expression) or integer expected"));
  }

  void expressionExpected() throws ScriptException {
    evalError(GT._("valid (atom expression) expected"));
  }

  int integerExpected() throws ScriptException {
    evalError(GT._("integer expected"));
    return 0;
  }

  float numberExpected() throws ScriptException {
    evalError(GT._("number expected"));
    return 0;
  }

  String stringExpected() throws ScriptException {
    evalError(GT._("quoted string expected"));
    return "";
  }

  void stringOrIdentifierExpected() throws ScriptException {
    evalError(GT._("quoted string or identifier expected"));
  }

  void propertyNameExpected() throws ScriptException {
    evalError(GT._("property name expected"));
  }

  void axisExpected() throws ScriptException {
    evalError(GT._("x y z axis expected"));
  }

  void colorExpected() throws ScriptException {
    evalError(GT._("color expected"));
  }

  void unrecognizedObject() throws ScriptException {
    evalError(GT._("unrecognized object"));
  }

  void unrecognizedExpression() throws ScriptException {
    evalError(GT._("runtime unrecognized expression"));
  }

  void undefinedVariable(String varName) throws ScriptException {
    evalError(GT._("variable undefined") + ": " + varName);
  }

  void endOfStatementUnexpected() throws ScriptException {
    evalError(GT._("unexpected end of script command"));
  }

  void badArgumentCount() throws ScriptException {
    evalError(GT._("bad argument count"));
  }

  void invalidArgument() throws ScriptException {
    evalError(GT._("invalid argument"));
  }

  void unrecognizedParameter(String kind, String param) throws ScriptException {
    evalError(GT._("unrecognized {0} parameter", kind) + ": " + param);
  }

  void unrecognizedShowParameter(String use) throws ScriptException {
    evalError(GT._("unrecognized SHOW parameter --  use {0}", use));
  }

  void numberOutOfRange() throws ScriptException {
    evalError(GT._("number out of range"));
  }

  void numberOutOfRange(int min, int max) throws ScriptException {
    evalError(GT._("integer out of range ({0} - {1})", new Object[] {
        new Integer(min), new Integer(max) }));
  }

  void numberOutOfRange(float min, float max) throws ScriptException {
    evalError(GT._("decimal number out of range ({0} - {1})", new Object[] {
        new Float(min), new Float(max) }));
  }

  void numberMustBe(int a, int b) throws ScriptException {
    evalError(GT._("number must be ({0} or {1})", new Object[] {
        new Integer(a), new Integer(b) }));
  }

  void fileNotFoundException(String filename) throws ScriptException {
    evalError(GT._("file not found") + ": " + filename);
  }

  void drawObjectNotDefined(String drawID) throws ScriptException {
    evalError(GT._("draw object not defined") + ": " + drawID);
  }

  String objectNameExpected() throws ScriptException {
    evalError(GT._("object name expected after '$'"));
    return "";
  }

  void coordinateExpected() throws ScriptException {
    evalError(GT._("{ number number number } expected"));
  }

  void coordinateOrNameOrExpressionRequired() throws ScriptException {
    evalError(GT._(" {x y z} or $name or (atom expression) required"));
  }

  void keywordExpected(String what) throws ScriptException {
    evalError(GT._("keyword expected") + ": " + what);
  }

  void invalidParameterOrder() throws ScriptException {
    evalError(GT._("invalid parameter order"));
  }

  void incompatibleArguments() throws ScriptException {
    evalError(GT._("incompatible arguments"));
  }

  void insufficientArguments() throws ScriptException {
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
      case Token.on:
        sb.append("true");
        continue;
      case Token.off:
        sb.append("false");
        break;
      case Token.integer:
        sb.append(token.intValue);
        continue;
      case Token.spec_seqcode:
        sb.append(Group.getSeqcodeString(token.intValue));
        continue;
      case Token.spec_chain:
        sb.append(':');
        sb.append((char) token.intValue);
        continue;
      case Token.spec_alternate:
        sb.append("%");
        sb.append("" + token.value);
        break;
      case Token.spec_model:
        sb.append("/");
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
      case Token.bitset:
        sb.append(StateManager.escape((BitSet) token.value));
        continue;
      case Token.spec_atom:
        sb.append('.');
        break;
      case Token.spec_seqcode_range:
        sb.append(Group.getSeqcodeString(token.intValue));
        sb.append('-');
        sb.append(Group.getSeqcodeString(((Integer) token.value).intValue()));
        break;
      case Token.within:
        sb.append("within ");
        break;
      case Token.connected:
        sb.append("connected ");
        break;
      case Token.substructure:
        sb.append("substructure ");
        break;
      case Token.cell:
        Point3f pt = (Point3f) token.value;
        sb.append("cell={" + pt.x + " " + pt.y + " " + pt.z + "}");
        continue;
      case Token.string:
        sb.append("\"" + token.value + "\"");
        continue;
      case Token.opEQ:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opLT:
      case Token.opNE:
        sb.append(Token.getAtomPropertyName(token.intValue));
        sb.append(" ");
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

  static Object evaluateExpression(Viewer viewer, String expr) {
    Eval e = new Eval(viewer);
    try {
      if (e.loadScript(null, "x = " + expr)) {
        e.statement = e.aatoken[0];
        e.statementLength = e.statement.length;
        return e.parameterExpression(2, false);
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return "ERROR";
  }

  static BitSet getAtomBitSet(Viewer viewer, Object atomExpression) {
    if (atomExpression instanceof BitSet)
      return (BitSet) atomExpression;
    Eval e = new Eval(viewer);
    BitSet bs = new BitSet();
    try {
      bs = e.getAtomBitSet(atomExpression.toString());
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
      return bs;
    }
    return bs;
  }

  static Vector getAtomBitSetVector(Viewer viewer, Object atomExpression) {
    Vector V = new Vector();
    BitSet bs = getAtomBitSet(viewer, atomExpression);
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i))
        V.add(new Integer(i));
    return V;
  }

  /// Reverse Polish Notation Engine for IF, SET, and %{...} -- Bob Hanson 2/16/2007
  /// Just a simple RPN processor that can handle boolean, int, float, String, and Point3f

  class Rpn {

    Token[] oStack;
    Token[] xStack;
    int oPt = -1;
    int xPt = -1;
    int maxLevel;
    int parenCount;
    boolean isUnary = true;

    Rpn(int maxLevel) {
      this.maxLevel = maxLevel;
      oStack = new Token[maxLevel];
      xStack = new Token[maxLevel];
      if (logMessages)
        Logger.info("initialize RPN on " + script);
    }

    Token getResult() throws ScriptException {
      while (oPt >= 0)
        if (!operate(Token.nada))
          return null;
      if (xPt != 0)
        return null;
      return xStack[0];
    }

    boolean addX(Token x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      if (!isUnary && x.tok == Token.integer && x.intValue < 0) {
        addOp(Token.tokenMinus);
        xStack[xPt] = new Token(Token.integer, -x.intValue);
      } else if (!isUnary && x.tok == Token.decimal
          && ((Float) x.value).floatValue() < 0) {
        addOp(Token.tokenMinus);
        xStack[xPt] = new Token(Token.decimal, new Float(-((Float) x.value)
            .floatValue()));
      } else {
        xStack[xPt] = x;
      }
      switch (x.tok) {
      case Token.integer:
      case Token.decimal:
      case Token.on:
      case Token.off:
      case Token.string:
        isUnary = false;
        break;
      default:
        isUnary = true;
      }
      return true;
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
      return true;
    }

    boolean addX(float x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.decimal, new Float(x));
      return true;
    }

    boolean addX(String x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.string, x);
      return true;
    }

    boolean addX(Point3f x) throws ScriptException {
      if (++xPt == maxLevel)
        stackOverflow();
      xStack[xPt] = new Token(Token.xyz, x);
      return true;
    }

    boolean addOp(Token op) throws ScriptException {
      if (op.tok == Token.rightparen) {
        if (oPt >= 0 && oStack[oPt].tok == Token.leftparen) {
          oPt--;
          return true;
        }
        if (parenCount == 0)
          return false;
      }
      while (oPt >= 0 && op.tok != Token.leftparen && op.tok != Token.opNot
          && Token.prec(oStack[oPt]) >= Token.prec(op)) {
        if (!operate(op.tok))
          return false;
      }

      switch (op.tok) {
      case Token.rightparen:
        isUnary = false;
        break;
      default:
        isUnary = true;
      }
      if (op.tok == Token.rightparen)
        return true;
      if (++oPt == maxLevel)
        stackOverflow();
      oStack[oPt] = op;
      if (op.tok == Token.leftparen)
        parenCount++;

      return true;
    }

    void dumpStacks() {
      Logger.info("RPN stacks: ");
      for (int i = 0; i <= xPt; i++)
        Logger.info("x[" + i + "]: " + xStack[i]);
      for (int i = 0; i <= oPt; i++)
        Logger.info("o[" + i + "]: " + oStack[i] + " prec="
            + Token.prec(oStack[i]));
    }

    boolean operate(int thisOp) throws ScriptException {

      if (logMessages)
        dumpStacks();

      Token op = oStack[oPt--];
      if (op.tok == Token.leftparen)
        return (parenCount-- > 0 && thisOp == Token.rightparen);

      if (xPt < 0)
        stackUnderflow();
      Token x2 = xStack[xPt--];
      checkOK(x2.tok, op.tok, 2);
      if (op.tok == Token.opNot)
        return addX(!Token.bValue(x2));

      if (xPt < 0)
        stackUnderflow();
      Token x1 = xStack[xPt--];
      checkOK(x1.tok, op.tok, 1);

      switch (op.tok) {
      case Token.opAnd:
        return addX(Token.bValue(x1) && Token.bValue(x2));
      case Token.opOr:
        return addX(Token.bValue(x1) || Token.bValue(x2));
      case Token.opLE:
        return addX(Token.fValue(x1) <= Token.fValue(x2));
      case Token.opGE:
        return addX(Token.fValue(x1) >= Token.fValue(x2));
      case Token.opGT:
        return addX(Token.fValue(x1) > Token.fValue(x2));
      case Token.opLT:
        return addX(Token.fValue(x1) < Token.fValue(x2));
      case Token.opEQ:
        return addX(Token.fValue(x1) == Token.fValue(x2));
      case Token.opNE:
        return addX(Token.fValue(x1) != Token.fValue(x2));
      case Token.plus:
        if (x1.tok == Token.string && x2.tok == Token.string)
          return addX("" + x1.value + x2.value);
        if (x1.tok == Token.integer && x2.tok == Token.integer)
          return addX(x1.intValue + x2.intValue);
        if (x1.tok == Token.xyz) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.xyz:
            pt.add((Point3f) x2.value);
            return addX(pt);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x + f, pt.y + f, pt.z + f));
          }
        }
        return addX(Token.fValue(x1) + Token.fValue(x2));
      case Token.hyphen:
        if (x1.tok == Token.integer && x2.tok == Token.integer)
          return addX(x1.intValue - x2.intValue);
        if (x1.tok == Token.xyz) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.xyz:
            pt.sub((Point3f) x2.value);
            return addX(pt);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x - f, pt.y - f, pt.z - f));
          }
        }
        return addX(Token.fValue(x1) - Token.fValue(x2));
      case Token.asterisk:
        if (x1.tok == Token.integer && x2.tok == Token.integer)
          return addX(x1.intValue * x2.intValue);
        if (x1.tok == Token.xyz) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.xyz:
            Point3f pt2 = ((Point3f) x2.value);
            return addX(pt.x * pt2.x + pt.y * pt2.y + pt.z * pt2.z);
          default:
            float f = Token.fValue(x2);
            return addX(new Point3f(pt.x * f, pt.y * f, pt.z * f));
          }
        }
        return addX(Token.fValue(x1) * Token.fValue(x2));
      case Token.percent:
        return addX(x1.intValue % x2.intValue);
      case Token.slash:
        if (x1.tok == Token.integer && x2.tok == Token.integer
            && x2.intValue != 0 && (x1.intValue / x2.intValue) * x2.intValue == x1.intValue)
          return addX(x1.intValue / x2.intValue);
        if (x1.tok == Token.xyz) {
          Point3f pt = new Point3f((Point3f) x1.value);
          switch (x2.tok) {
          case Token.xyz:
            invalidArgument();
          default:
            float f = Token.fValue(x2);
            if (f == 0)
              return addX(new Point3f(Float.NaN, Float.NaN, Float.NaN));
            return addX(new Point3f(pt.x / f, pt.y / f, pt.z / f));
          }
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

    boolean checkOK(int x, int op, int side) throws ScriptException {
      if (op == Token.percent && x != Token.integer)
        invalidArgument();
      return true;
    }

    void stackOverflow() throws ScriptException {
      evalError(GT._("too many parentheses"));
    }

    void stackUnderflow() throws ScriptException {
      numberExpected();
    }
  }

}
