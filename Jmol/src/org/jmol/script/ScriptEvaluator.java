/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.script;

import java.awt.Image;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.api.MinimizerInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BoxInfo;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelCollection;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.shape.Object2d;
import org.jmol.shape.Shape;
import org.jmol.util.BitSetUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;

import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;
import org.jmol.util.SurfaceFileTyper;
import org.jmol.util.TextFormat;
import org.jmol.modelset.TickInfo;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.PropertyManager;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;

public class ScriptEvaluator {

  /*
   * The ScriptEvaluator class, the Viewer, the xxxxManagers, the Graphics3D
   * rendering engine, the ModelSet and Shape classes, and the Adapter file
   * reader classes form the core of the Jmol molecular visualization framework.
   * 
   * The ScriptEvaluator has just a few entry points, which you will find
   * immediately following this comment. They include:
   * 
   * public boolean compileScriptString(String script, boolean tQuiet)
   * 
   * public boolean compileScriptFile(String filename, boolean tQuiet)
   * 
   * public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option, boolean
   * isCmdLine_C_Option, boolean historyDisabled, boolean listCommands)
   * 
   * Essentially ANYTHING can be done using these three methods. A variety of
   * other methods are available via Viewer, which is the the true portal to
   * Jmol (via the JmolViewer interface) for application developers who want
   * faster, more direct processing.
   * 
   * A little Jmol history:
   * 
   * General history notes can be found at our ConfChem paper, which can be
   * found at
   * http://chemapps.stolaf.edu/jmol/presentations/confchem2006/jmol-confchem
   * .htm
   * 
   * This ScriptEvaluator class was initially written by Michael (Miguel) Howard
   * as Eval.java as an efficient means of reproducing the RasMol scripting
   * language for Jmol. Key additions there included:
   * 
   * - tokenization of commands via the Compiler class (now ScriptCompiler and
   * ScriptCompilationTokenParser) - ScriptException error handling - a flexible
   * yet structured command parameter syntax - implementations of RasMol
   * secondary structure visualizations - isosurfaces, dots, labels, polyhedra,
   * draw, stars, pmesh, more
   * 
   * Other Miguel contributions include:
   * 
   * - the structural bases of the Adapter, ModelSet, and ModelSetBio classes -
   * creation of Manager classes - absolutely amazing raw pixel bitmap rendering
   * code (org.jmol.g3d) - popup context menu - inline model loading
   * 
   * Bob Hanson (St. Olaf College) found out about Jmol during the spring of
   * 2004. After spending over a year working on developing online interactive
   * documentation, he started actively writing code early in 2006. During the
   * period 2006-2009 Bob completely reworked the script processor (and much of
   * the rest of Jmol) to handle a much broader range of functionality. Notable
   * improvements include:
   * 
   * - display/hide commands - dipole, ellipsoid, geosurface, lcaoCartoon
   * visualizations - quaternion and ramachandran commands - much expanded
   * isosurface / draw commands - configuration, disorder, and biomolecule
   * support - broadly 2D- and 3D-positionable echos - translateSelected and
   * rotateSelected commands - getProperty command, providing access to more
   * file information - data and write commands - writing of high-resolution
   * JPG, PNG, and movie-sequence JPG - generalized export to Maya and PovRay
   * formats
   * 
   * - multiple file loading, including trajectories - minimization using the
   * Universal Force Field (UFF) - atom/model deletion and addition - direct
   * loading of properties such as partial charge or coordinates - several new
   * file readers, including manifested zip file reading - default directory, CD
   * command, and pop-up file open/save dialogs
   * 
   * - "internal" molecular coordinate-based rotations - full support for
   * crystallographic formats, including space groups, symmetry, unit cells, and
   * fractional coordinates - support for point groups and molecular symmetry -
   * navigation mode - antialiasing of display and imaging - save/restore/write
   * exact Jmol state - JVXL file format for compressed rapid generation of
   * isosurfaces
   * 
   * - user-defined variables - addition of a Reverse Polish Notation (RPN)
   * expression processor - extension of the RPN processor to user variables -
   * user-defined functions - flow control commands if/else/endif, for, and
   * while - JavaScript/Java-like brace syntax - key stroke-by-key stroke
   * command syntax checking - integrated help command - user-definable popup
   * menu - language switching
   * 
   * - fully functional signed applet - applet-applet synchronization, including
   * two-applet geoWall stereo rendering - JSON format for property delivery to
   * JavaScript - jmolScriptWait, dual-threaded queued JavaScript scripting
   * interface - extensive callback development - script editor panel (work in
   * progress, June 2009)
   * 
   * Several other people have contributed. Perhaps they will not be too shy to
   * add their claim to victory here. Please add your contributions.
   * 
   * - Jmol application (Egon Willighagen) - smiles support (Nico Vervelle) -
   * readers (Rene Kanter, Egon, several others) - initial VRML export work
   * (Nico Vervelle) - WebExport (Jonathan Gutow) - internationalization (Nico,
   * Egon, Angel Herriez) - Jmol Wiki and user guide book (Angel Herriez)
   * 
   * While this isn't necessarily the best place for such discussion, open
   * source principles require proper credit given to those who have
   * contributed. This core class seems to me a place to acknowledge this core
   * work of the Jmol team.
   * 
   * Bob Hanson, 6/2009 hansonr@stolaf.edu
   */

  public static final String SCRIPT_COMPLETED = "Script completed";

  public ScriptEvaluator(Viewer viewer) {
    this.viewer = viewer;
    compiler = viewer.compiler;
    definedAtomSets = viewer.definedAtomSets;
  }

  // //////////////// primary interfacing methods //////////////////

  /*
   * see Viewer.evalStringWaitStatus for how these are implemented
   */
  public boolean compileScriptString(String script, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = "[script]";
    return compileScript(null, script, debugScript);
  }

  public boolean compileScriptFile(String filename, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = filename;
    return compileScriptFileInternal(filename, null, null, null);
  }

  public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option,
                                     boolean isCmdLine_C_Option,
                                     boolean historyDisabled,
                                     boolean listCommands) {
    boolean tempOpen = this.isCmdLine_C_Option;
    this.isCmdLine_C_Option = isCmdLine_C_Option;
    viewer.pushHoldRepaint("runEval");
    interruptExecution = executionPaused = false;
    executionStepping = false;
    isExecuting = true;
    currentThread = Thread.currentThread();
    isSyntaxCheck = this.isCmdLine_c_or_C_Option = isCmdLine_c_or_C_Option;
    timeBeginExecution = System.currentTimeMillis();
    this.historyDisabled = historyDisabled;
    setErrorMessage(null);
    try {
      try {
        setScriptExtensions();
        instructionDispatchLoop(listCommands);
        String script = viewer.getInterruptScript();
        if (script != "")
          runScript(script, null);
      } catch (Error er) {
        viewer.handleError(er, false);
        setErrorMessage("" + er + " " + viewer.getShapeErrorState());
        errorMessageUntranslated = "" + er;
        scriptStatusOrBuffer(errorMessage);
      }
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
      errorMessageUntranslated = e.getErrorMessageUntranslated();
      scriptStatusOrBuffer(errorMessage);
      viewer.notifyError((errorMessage != null
          && errorMessage.indexOf("java.lang.OutOfMemoryError") >= 0 ? "Error"
          : "ScriptException"), errorMessage, errorMessageUntranslated);
    }
    timeEndExecution = System.currentTimeMillis();
    this.isCmdLine_C_Option = tempOpen;
    if (errorMessage == null && interruptExecution)
      setErrorMessage("execution interrupted");
    else if (!tQuiet && !isSyntaxCheck)
      viewer.scriptStatus(SCRIPT_COMPLETED);
    isExecuting = isSyntaxCheck = isCmdLine_c_or_C_Option = historyDisabled = false;
    viewer.setTainted(true);
    viewer.popHoldRepaint("runEval");
  }

  /**
   * runs a script and sends selected output to a provided StringBuffer
   * 
   * @param script
   * @param outputBuffer
   * @throws ScriptException
   */
  public void runScript(String script, StringBuffer outputBuffer)
      throws ScriptException {
    // a = script("xxxx")
    pushContext(null);
    contextPath += " >> script() ";

    this.outputBuffer = outputBuffer;
    if (compileScript(null, script + JmolConstants.SCRIPT_EDITOR_IGNORE, false))
      instructionDispatchLoop(false);
    popContext(false);
  }

  /**
   * a method for just checking a script
   * 
   * @param script
   * @return a ScriptContext that indicates errors and provides a tokenized
   *         version of the script that has passed all syntax checking, both in
   *         the compiler and the evaluator
   * 
   */
  public ScriptContext checkScriptSilent(String script) {
    ScriptContext sc = compiler.compile(null, script, false, true, false, true);
    if (sc.errorType != null)
      return sc;
    getScriptContext(sc, false, false);
    isSyntaxCheck = true;
    isCmdLine_c_or_C_Option = isCmdLine_C_Option = false;
    pc = 0;
    try {
      instructionDispatchLoop(false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
      sc = getScriptContext();
    }
    isSyntaxCheck = false;
    return sc;
  }

  // //////////////////////// script execution /////////////////////

  private boolean tQuiet;
  protected boolean isSyntaxCheck;
  private boolean isCmdLine_C_Option;
  protected boolean isCmdLine_c_or_C_Option;
  private boolean historyDisabled;
  protected boolean logMessages;
  private boolean debugScript;

  public void setDebugging() {
    debugScript = viewer.getDebugScript();
    logMessages = (debugScript && Logger.debugging);
  }

  private boolean interruptExecution;
  private boolean executionPaused;
  private boolean executionStepping;
  private boolean isExecuting;

  private long timeBeginExecution;
  private long timeEndExecution;

  public int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  public void haltExecution() {
    resumePausedExecution();
    interruptExecution = true;
  }

  public void pauseExecution() {
    if (isSyntaxCheck)
      return;
    delay(-100);
    viewer.popHoldRepaint("pauseExecution");
    executionStepping = false;
    executionPaused = true;
  }

  public void stepPausedExecution() {
    executionStepping = true;
    executionPaused = false;
    // releases a paused thread but
    // sets it to pause for the next command.
  }

  public void resumePausedExecution() {
    executionPaused = false;
    executionStepping = false;
  }

  public boolean isScriptExecuting() {
    return isExecuting && !interruptExecution;
  }

  public boolean isExecutionPaused() {
    return executionPaused;
  }

  public boolean isExecutionStepping() {
    return executionStepping;
  }

  /**
   * when paused, indicates what statement will be next
   * 
   * @return a string indicating the statement
   */
  public String getNextStatement() {
    return (pc < aatoken.length ? setErrorLineMessage(functionName, filename,
        getLinenumber(null), pc, statementAsString(aatoken[pc], -9999)) : "");
  }

  /**
   * used for recall of commands in the application console
   * 
   * @param pc
   * @param allThisLine
   * @param addSemi
   * @return a string representation of the command
   */
  private String getCommand(int pc, boolean allThisLine, boolean addSemi) {
    if (pc >= lineIndices.length)
      return "";
    if (allThisLine) {
      int pt0 = -1;
      int pt1 = script.length();
      for (int i = 0; i < lineNumbers.length; i++)
        if (lineNumbers[i] == lineNumbers[pc]) {
          if (pt0 < 0)
            pt0 = lineIndices[i][0];
          pt1 = lineIndices[i][1];
        } else if (lineNumbers[i] == 0 || lineNumbers[i] > lineNumbers[pc]) {
          break;
        }
      if (pt1 == script.length() - 1 && script.endsWith("}"))
        pt1++;
      return (pt0 == script.length() || pt1 < pt0 ? "" : script.substring(Math
          .max(pt0, 0), Math.min(script.length(), pt1)));
    }
    int ichBegin = lineIndices[pc][0];
    int ichEnd = lineIndices[pc][1];
    // (pc + 1 == lineIndices.length || lineIndices[pc + 1][0] == 0 ? script
    // .length()
    // : lineIndices[pc + 1]);
    String s = "";
    if (ichBegin < 0 || ichEnd <= ichBegin || ichEnd > script.length())
      return "";
    try {
      s = script.substring(ichBegin, ichEnd);
      if (s.indexOf("\\\n") >= 0)
        s = TextFormat.simpleReplace(s, "\\\n", "  ");
      if (s.indexOf("\\\r") >= 0)
        s = TextFormat.simpleReplace(s, "\\\r", "  ");
      // int i;
      // for (i = s.length(); --i >= 0 && !ScriptCompiler.eol(s.charAt(i), 0);
      // ){
      // }
      // s = s.substring(0, i + 1);
      if (s.length() > 0 && !s.endsWith(";")/*
                                             * && !s.endsWith("{") &&
                                             * !s.endsWith("}")
                                             */)
        s += ";";
    } catch (Exception e) {
      Logger.error("darn problem in Eval getCommand: ichBegin=" + ichBegin
          + " ichEnd=" + ichEnd + " len = " + script.length() + "\n" + e);
    }
    return s;
  }

  private void logDebugScript(int ifLevel) {
    if (logMessages) {
      if (statement.length > 0)
        Logger.debug(statement[0].toString());
      for (int i = 1; i < statementLength; ++i)
        Logger.debug(statement[i].toString());
    }
    iToken = -9999;
    if (logMessages) {
      StringBuffer strbufLog = new StringBuffer(80);
      String s = (ifLevel > 0 ? "                          ".substring(0,
          ifLevel * 2) : "");
      strbufLog.append(s).append(statementAsString(statement, iToken));
      viewer.scriptStatus(strbufLog.toString());
    } else {
      String cmd = getCommand(pc, false, false);
      viewer.scriptStatus(cmd);
    }

  }

  // /////////////// string-based evaluation support /////////////////////

  private final static String EXPRESSION_KEY = "e_x_p_r_e_s_s_i_o_n";

  /**
   * a general-use method to evaluate a "SET" type expression.
   * 
   * @param viewer
   * @param expr
   * @return an object of one of the following types: Boolean, Integer, Float,
   *         String, Point3f, BitSet
   */

  public static Object evaluateExpression(Viewer viewer, Object expr) {
    // Text.formatText for MESSAGE and ECHO
    ScriptEvaluator e = new ScriptEvaluator(viewer);
    try {
      if (expr instanceof String) {
        if (e.compileScript(null, EXPRESSION_KEY + " = " + expr, false)) {
          e.contextVariables = viewer.getContextVariables();
          e.setStatement(0);
          return e.parameterExpression(2, 0, "", false);
        }
      } else if (expr instanceof Token[]) {
        e.contextVariables = viewer.getContextVariables();
        return e.expression((Token[]) expr, 0, 0, true, false, true, false);
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return "ERROR";
  }

  ShapeManager shapeManager;
  
  public static boolean evaluateContext(Viewer viewer, ScriptContext context, ShapeManager shapeManager) {
    ScriptEvaluator e = new ScriptEvaluator(viewer);
    e.compiler = new ScriptCompiler(e.compiler); 
    e.shapeManager = shapeManager;
    try {
      e.getScriptContext(context, true, false);
      e.instructionDispatchLoop(false);
    } catch (Exception ex) {
      Logger.error("Error evaluating context");
      ex.printStackTrace();
      return false;
    }
    return true;
  }
  /**
   * a general method to evaluate a string representing an atom set.
   * 
   * @param e
   * @param atomExpression
   * @return is a bitset indicating the selected atoms
   * 
   */
  public static BitSet getAtomBitSet(ScriptEvaluator e, Object atomExpression) {
    if (atomExpression instanceof BitSet)
      return (BitSet) atomExpression;
    BitSet bs = new BitSet();
    try {
      e.pushContext(null);
      String scr = "select (" + atomExpression + ")";
      scr = TextFormat.replaceAllCharacters(scr, "\n\r", "),(");
      scr = TextFormat.simpleReplace(scr, "()", "(none)");
      if (e.compileScript(null, scr, false)) {
        e.statement = e.aatoken[0];
        bs = e.expression(e.statement, 1, 0, false, false, true, true);
      }
      e.popContext(false);
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
    }
    return bs;
  }

  /**
   * just provides a vector list of atoms in a string-based expression
   * 
   * @param e
   * @param atomCount
   * @param atomExpression
   * @return vector list of selected atoms
   */
  public static Vector getAtomBitSetVector(ScriptEvaluator e, int atomCount,
                                           Object atomExpression) {
    Vector V = new Vector();
    BitSet bs = getAtomBitSet(e, atomExpression);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      V.addElement(new Integer(i));
    return V;
  }

  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean asVector) throws ScriptException {
    return parameterExpression(pt, ptMax, key, asVector, -1, false, null, null);
  }

  /**
   * This is the primary driver of the RPN (reverse Polish notation) expression
   * processor. It handles all math outside of a "traditional" Jmol
   * SELECT/RESTRICT context. [Object expression() takes care of that, and also
   * uses the RPN class.]
   * 
   * @param pt
   *          token index in statement start of expression
   * @param ptMax
   *          token index in statement end of expression
   * @param key
   *          variable name for debugging reference only -- null indicates
   *          return Boolean -- "" indicates return String
   * @param asVector
   *          a flag passed on to RPN;
   * @param ptAtom
   *          this is a for() or select() function with a specific atom selected
   * @param isArrayItem
   *          we are storing A[x] = ... so we need to deliver "x" as well
   * @param localVars
   *          see below -- lists all nested for(x, {exp}, select(y, {ex},...))
   *          variables
   * @param localVar
   *          x or y in above for(), select() examples
   * @return either a vector or a value, caller's choice.
   * @throws ScriptException
   *           errors are thrown directly to the Eval error system.
   */
  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean asVector, int ptAtom,
                                     boolean isArrayItem, Hashtable localVars,
                                     String localVar) throws ScriptException {

    /*
     * localVar is a variable designated at the beginning of the select(x,...)
     * or for(x,...) construct that will be implicitly used for properties. So,
     * for example, "atomno" will become "x.atomno". That's all it is for.
     * localVars provides a localized context variable set for a given nested
     * set of for/select.
     * 
     * Note that localVars has nothing to do standard if/for/while flow
     * contexts, just these specialized functions. Any variable defined in for
     * or while is simply added to the context for a given script or function.
     * These assignments are made by the compiler when seeing a VAR keyword.
     */
    Object v, res;
    boolean isImplicitAtomProperty = (localVar != null);
    boolean isOneExpressionOnly = (pt < 0);
    boolean returnBoolean = (key == null);
    boolean returnString = (key != null && key.length() == 0);
    if (isOneExpressionOnly)
      pt = -pt;
    int nParen = 0;
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, isArrayItem,
        asVector, false);
    if (pt == 0 && ptMax == 0) // set command with v[...] = ....
      pt = 2;
    if (ptMax < pt)
      ptMax = statementLength;
    out: for (int i = pt; i < ptMax; i++) {
      v = null;
      int tok = getToken(i).tok;
      if (isImplicitAtomProperty && tokAt(i + 1) != Token.per) {
        ScriptVariable token = (localVars != null
            && localVars.containsKey(theToken.value) ? null
            : getBitsetPropertySelector(i, false));
        if (token != null) {
          rpn.addX((ScriptVariable) localVars.get(localVar));
          if (!rpn.addOp(token)) {
            error(ERROR_invalidArgument);
          }
          if ((token.intValue == Token.function || token.intValue == Token.parallel)
              && tokAt(iToken + 1) != Token.leftparen) {
            rpn.addOp(Token.tokenLeftParen);
            rpn.addOp(Token.tokenRightParen);
          }
          i = iToken;
          continue;
        }
      }
      switch (tok) {
      case Token.ifcmd:
        if (getToken(++i).tok != Token.leftparen)
          error(ERROR_invalidArgument);
        if (localVars == null)
          localVars = new Hashtable();
        res = parameterExpression(++i, -1, null, false, -1, false, localVars,
            localVar);
        boolean TF = ((Boolean) res).booleanValue();
        int iT = iToken;
        if (getToken(iT++).tok != Token.semicolon)
          error(ERROR_invalidArgument);
        parameterExpression(iT, -1, null, false);
        int iF = iToken;
        if (tokAt(iF++) != Token.semicolon)
          error(ERROR_invalidArgument);
        parameterExpression(-iF, -1, null, false, 1, false, localVars, localVar);
        int iEnd = iToken;
        if (tokAt(iEnd) != Token.rightparen)
          error(ERROR_invalidArgument);
        v = parameterExpression(TF ? iT : iF, TF ? iF : iEnd, "XXX", false, 1,
            false, localVars, localVar);
        i = iEnd;
        break;
      case Token.forcmd:
      case Token.select:
        boolean isFunctionOfX = (pt > 0);
        boolean isFor = (isFunctionOfX && tok == Token.forcmd);
        // it is important to distinguish between the select command:
        // select {atomExpression} (mathExpression)
        // and the select(dummy;{atomExpression};mathExpression) function:
        // select {*.ca} (phi < select(y; {*.ca}; y.resno = _x.resno + 1).phi)
        String dummy;
        // for(dummy;...
        // select(dummy;...
        if (isFunctionOfX) {
          if (getToken(++i).tok != Token.leftparen
              || !Token.tokAttr(getToken(++i).tok, Token.identifier))
            error(ERROR_invalidArgument);
          dummy = parameterAsString(i);
          if (getToken(++i).tok != Token.semicolon)
            error(ERROR_invalidArgument);
        } else {
          dummy = "_x";
        }
        // for(dummy;{atom expr};...
        // select(dummy;{atom expr};...
        v = tokenSetting(-(++i)).value;
        if (!(v instanceof BitSet))
          error(ERROR_invalidArgument);
        BitSet bsAtoms = (BitSet) v;
        i = iToken;
        if (isFunctionOfX && getToken(i++).tok != Token.semicolon)
          error(ERROR_invalidArgument);
        // for(dummy;{atom expr};math expr)
        // select(dummy;{atom expr};math expr)
        // bsX is necessary because there are a few operations that still
        // are there for now that require it; could go, though.
        BitSet bsSelect = new BitSet();
        BitSet bsX = new BitSet();
        String[] sout = (isFor ? new String[BitSetUtil.cardinalityOf(bsAtoms)]
            : null);
        ScriptVariable t = null;
        if (localVars == null)
          localVars = new Hashtable();
        bsX.set(0);
        localVars.put(dummy, t = ScriptVariable.getVariableSelected(0, bsX)
            .setName(dummy));
        // one test just to check for errors and get iToken
        int pt2 = -1;
        if (isFunctionOfX) {
          pt2 = i - 1;
          int np = 0;
          int tok2;
          while (np >= 0 && ++pt2 < ptMax) {
            if ((tok2 = tokAt(pt2)) == Token.rightparen)
              np--;
            else if (tok2 == Token.leftparen)
              np++;
          }
        }
        int p = 0;
        int jlast = 0;
        int j = bsAtoms.nextSetBit(0);
        if (j < 0) {
          iToken = pt2 - 1;
        } else if (!isSyntaxCheck) {
          for (; j >= 0; j = bsAtoms.nextSetBit(j + 1)) {
            if (jlast >= 0)
              bsX.clear(jlast);
            jlast = j;
            bsX.set(j);
            t.index = j;
            res = parameterExpression(i, pt2, (isFor ? "XXX" : null), isFor, j,
                false, localVars, isFunctionOfX ? null : dummy);
            if (isFor) {
              if (res == null || ((Vector) res).size() == 0)
                error(ERROR_invalidArgument);
              sout[p++] = ScriptVariable.sValue((ScriptVariable) ((Vector) res)
                  .elementAt(0));
            } else if (((Boolean) res).booleanValue()) {
              bsSelect.set(j);
            }
          }
        }
        if (isFor) {
          v = sout;
        } else if (isFunctionOfX) {
          v = bsSelect;
        } else {
          return bitsetVariableVector(bsSelect);
        }
        i = iToken + 1;
        break;
      case Token.semicolon: // for (i = 1; i < 3; i=i+1)
        break out;
      case Token.decimal:
        rpn.addXNum(ScriptVariable.getVariable(theToken.value));
        break;
      case Token.spec_seqcode:
      case Token.integer:
        rpn.addXNum(ScriptVariable.intVariable(theToken.intValue));
        break;
      // these next are for the within() command
      case Token.plane:
        if (tokAt(iToken + 1) == Token.leftparen) {
          if (!rpn.addOp(theToken, true))
            error(ERROR_invalidArgument);
          break;
        }
        rpn.addX(new ScriptVariable(theToken));
        break;
      case Token.atomname:
      case Token.atomtype:
      case Token.branch:
      case Token.boundbox:
      case Token.chain:
      case Token.coord:
      case Token.element:
      case Token.group:
      case Token.model:
      case Token.molecule:
      case Token.site:
      case Token.structure:
        //
      case Token.on:
      case Token.off:
      case Token.string:
      case Token.point3f:
      case Token.point4f:
      case Token.matrix3f:
      case Token.matrix4f:
      case Token.bitset:
        rpn.addX(new ScriptVariable(theToken));
        break;
      case Token.dollarsign:
        rpn.addX(new ScriptVariable(Token.point3f, centerParameter(i)));
        i = iToken;
        break;
      case Token.leftbrace:
        v = getPointOrPlane(i, false, true, true, false, 3, 4);
        i = iToken;
        break;
      case Token.expressionBegin:
        if (tokAt(i + 1) == Token.expressionEnd) {
          v = new BitSet();
          i++;
          break;
        } else if (tokAt(i + 1) == Token.all && tokAt(i + 2) == Token.expressionEnd) {
          tok = Token.all;
          iToken += 2;
        }
        // fall through
      case Token.all:
        if (tok == Token.all)
          v = viewer.getModelAtomBitSet(-1, true);
        else
          v = expression(statement, i, 0, true, true, true, true);
        i = iToken;
        if (nParen == 0 && isOneExpressionOnly) {
          iToken++;
          return bitsetVariableVector(v);
        }
        break;
      case Token.expressionEnd:
        i++;
        break out;
      case Token.rightbrace:
        error(ERROR_invalidArgument);
        break;
      case Token.comma: // ignore commas
        if (!rpn.addOp(theToken))
          error(ERROR_invalidArgument);
        break;
      case Token.per:
        ScriptVariable token = getBitsetPropertySelector(i + 1, false);
        if (token == null)
          error(ERROR_invalidArgument);
        // check for added min/max modifier
        boolean isUserFunction = (token.intValue == Token.function);
        boolean allowMathFunc = true;
        int tok2 = tokAt(iToken + 2);
        if (tokAt(iToken + 1) == Token.per) {
          switch (tok2) {
          case Token.all:
            tok2 = Token.minmaxmask;
            if (tokAt(iToken + 3) == Token.per
                && tokAt(iToken + 4) == Token.bin)
              tok2 = Token.allfloat;
            // fall through
          case Token.min:
          case Token.max:
          case Token.stddev:
          case Token.sum:
          case Token.sum2:
          case Token.average:
            allowMathFunc = (isUserFunction || tok2 == Token.minmaxmask || tok2 == Token.allfloat);
            token.intValue |= tok2;
            getToken(iToken + 2);
          }
        }
        allowMathFunc &= (tokAt(iToken + 1) == Token.leftparen || isUserFunction);
        if (!rpn.addOp(token, allowMathFunc))
          error(ERROR_invalidArgument);
        i = iToken;
        if (token.intValue == Token.function && tokAt(i + 1) != Token.leftparen) {
          rpn.addOp(Token.tokenLeftParen);
          rpn.addOp(Token.tokenRightParen);
        }
        break;
      default:
        if (Token.tokAttr(theTok, Token.mathop)
            || Token.tokAttr(theTok, Token.mathfunc)) {
          if (!rpn.addOp(theToken)) {
            if (ptAtom >= 0) {
              // this is expected -- the right parenthesis
              break out;
            }
            error(ERROR_invalidArgument);
          }
          if (theTok == Token.leftparen)
            nParen++;
          else if (theTok == Token.rightparen) {
            if (--nParen == 0 && isOneExpressionOnly) {
              iToken++;
              break out;
            }
          }
        } else if (Token.tokAttr(theTok, Token.identifier)
            && viewer.isFunction((String) theToken.value)) {
          if (!rpn.addOp(new ScriptVariable(Token.function, theToken.value))) {
            // iToken--;
            error(ERROR_invalidArgument);
          }
          if (tokAt(i + 1) != Token.leftparen) {
            rpn.addOp(Token.tokenLeftParen);
            rpn.addOp(Token.tokenRightParen);
          }
        } else {
          String name = parameterAsString(i).toLowerCase();
          if (isSyntaxCheck)
            v = name;
          else if ((localVars == null || (v = localVars.get(name)) == null)
              && (v = getContextVariableAsVariable(name)) == null)
            rpn.addX(viewer.getOrSetNewVariable(name, false));
          break;
        }
      }
      if (v != null) {
        if (v instanceof BitSet)
          rpn.addX((BitSet) v);
        else
          rpn.addX(v);
      }
    }
    ScriptVariable result = rpn.getResult(false, key);
    if (result == null) {
      if (!isSyntaxCheck)
        rpn.dumpStacks("null result");
      error(ERROR_endOfStatementUnexpected);
    }
    if (result.tok == Token.vector)
      return result.value;
    if (returnBoolean)
      return Boolean.valueOf(ScriptVariable.bValue(result));
    if (returnString) {
      if (result.tok == Token.string)
        result.intValue = Integer.MAX_VALUE;
      return ScriptVariable.sValue(result);
    }
    switch (result.tok) {
    case Token.on:
    case Token.off:
      return Boolean.valueOf(result.intValue == 1);
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

  Object bitsetVariableVector(Object v) {
    Vector resx = new Vector();
    if (v instanceof BitSet)
      resx.addElement(new ScriptVariable(Token.bitset, v));
    return resx;
  }

  Object getBitsetIdent(BitSet bs, String label, Object tokenValue,
                        boolean useAtomMap, int index, boolean isExplicitlyAll) {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    if (isAtoms) {
      if (label == null)
        label = viewer.getStandardLabelFormat();
      else if (label.length() == 0)
        label = "%[label]";
    }
    int pt = (label == null ? -1 : label.indexOf("%"));
    boolean haveIndex = (index != Integer.MAX_VALUE);
    if (bs == null || isSyntaxCheck || isAtoms && pt < 0) {
      if (label == null)
        label = "";
      return isExplicitlyAll ? new String[] { label } : (Object) label;
    }
    ModelSet modelSet = viewer.getModelSet();
    int n = 0;
    int[] indices = (isAtoms || !useAtomMap ? null : ((BondSet) tokenValue)
        .getAssociatedAtoms());
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = viewer.getAtomIndices(bs);
    boolean asIdentity = (label == null || label.length() == 0);
    Hashtable htValues = (isAtoms || asIdentity ? null : LabelToken
        .getBondLabelValues());
    LabelToken[] tokens = (asIdentity ? null : isAtoms ? LabelToken.compile(
        viewer, label, '\0', null) : LabelToken.compile(viewer, label, '\1',
        htValues));
    int nmax = (haveIndex ? 1 : BitSetUtil.cardinalityOf(bs));
    String[] sout = new String[nmax];
    for (int j = (haveIndex ? index : bs.nextSetBit(0)); j >= 0; j = bs
        .nextSetBit(j + 1)) {
      String str;
      if (isAtoms) {
        if (asIdentity)
          str = modelSet.getAtomAt(j).getInfo();
        else
          str = LabelToken.formatLabel(viewer, modelSet.getAtomAt(j), null,
              tokens, '\0', indices);
      } else {
        Bond bond = modelSet.getBondAt(j);
        if (asIdentity)
          str = bond.getIdentity();
        else
          str = LabelToken.formatLabel(viewer, bond, tokens, htValues, indices);
      }
      str = TextFormat.formatString(str, "#", (n + 1));
      sout[n++] = str;
      if (haveIndex)
        break;
    }
    return nmax == 1 && !isExplicitlyAll ? sout[0] : (Object) sout;
  }

  private ScriptVariable getBitsetPropertySelector(int i, boolean mustBeSettable)
      throws ScriptException {
    int tok = getToken(i).tok;
    String s = null;
    switch (tok) {
    case Token.min:
    case Token.max:
    case Token.average:
    case Token.stddev:
    case Token.sum:
    case Token.sum2:
    case Token.property:
      break;
    default:
      if (Token.tokAttrOr(tok, Token.atomproperty, Token.mathproperty))
        break;
      if (!Token.tokAttr(tok, Token.identifier))
        return null;
      String name = parameterAsString(i);
      if (!mustBeSettable && viewer.isFunction(name)) {
        tok = Token.function;
        break;
      }
      return null;
    }
    if (mustBeSettable && !Token.tokAttr(tok, Token.settable))
      return null;
    if (s == null)
      s = parameterAsString(i).toLowerCase();
    return new ScriptVariable(Token.propselector, tok, s);
  }

  protected Object getBitsetProperty(BitSet bs, int tok, Point3f ptRef,
                                     Point4f planeRef, Object tokenValue,
                                     Object opValue, boolean useAtomMap,
                                     int index, boolean asVector)
      throws ScriptException {

    // index is a special argument set in parameterExpression that
    // indicates we are looking at only one atom within a for(...) loop
    // the bitset cannot be a BondSet in that case

    boolean haveIndex = (index != Integer.MAX_VALUE);

    boolean isAtoms = haveIndex || !(tokenValue instanceof BondSet);
    // check minmax flags:

    int minmaxtype = tok & Token.minmaxmask;
    boolean allFloat = (minmaxtype == Token.allfloat);
    boolean isExplicitlyAll = (minmaxtype == Token.minmaxmask || allFloat);
    tok &= ~Token.minmaxmask;
    if (tok == Token.nada)
      tok = (isAtoms ? Token.atoms : Token.bonds);

    // determine property type:

    boolean isPt = false;
    boolean isInt = false;
    boolean isString = false;
    switch (tok) {
    case Token.xyz:
    case Token.vibxyz:
    case Token.fracxyz:
    case Token.unitxyz:
    case Token.color:
      isPt = true;
      break;
    case Token.function:
    case Token.distance:
      break;
    default:
      isInt = Token.tokAttr(tok, Token.intproperty)
          && !Token.tokAttr(tok, Token.floatproperty);
      // occupancy and radius considered floats here
      isString = !isInt && Token.tokAttr(tok, Token.strproperty);
      // structure considered int; for the name, use .label("%[structure]")
    }

    // preliminarty checks we only want to do once:

    Point3f pt = (isPt || !isAtoms ? new Point3f() : null);
    if (isString || isExplicitlyAll)
      minmaxtype = Token.all;
    Vector vout = (minmaxtype == Token.all ? new Vector() : null);

    BitSet bsNew = null;
    String userFunction = null;
    Vector params = null;
    BitSet bsAtom = null;
    ScriptVariable tokenAtom = null;
    Point3f ptT = null;
    float[] data = null;

    switch (tok) {
    case Token.atoms:
    case Token.bonds:
      if (isSyntaxCheck)
        return bs;
      bsNew = (tok == Token.atoms ? (isAtoms ? bs : viewer.getAtomBits(
          Token.bonds, bs)) : (isAtoms ? (BitSet) new BondSet(viewer
          .getBondsForSelectedAtoms(bs)) : bs));
      int i;
      switch (minmaxtype) {
      case Token.min:
        i = bsNew.nextSetBit(0);
        break;
      case Token.max:
        i = bsNew.length() - 1;
        break;
      case Token.stddev:
      case Token.sum:
      case Token.sum2:
        return new Float(Float.NaN);
      default:
        return bsNew;
      }
      bsNew.clear();
      if (i >= 0)
        bsNew.set(i);
      return bsNew;
    case Token.identify:
      switch (minmaxtype) {
      case 0:
      case Token.all:
        return getBitsetIdent(bs, null, tokenValue, useAtomMap, index,
            isExplicitlyAll);
      }
      return "";
    case Token.function:
      userFunction = (String) ((Object[]) opValue)[0];
      params = (Vector) ((Object[]) opValue)[1];
      bsAtom = new BitSet(viewer.getAtomCount());
      tokenAtom = new ScriptVariable(Token.bitset, bsAtom);
      break;
    case Token.straightness:
    case Token.surfacedistance:
      viewer.autoCalculate(tok);
      break;
    case Token.distance:
      if (ptRef == null && planeRef == null)
        return new Point3f();
      break;
    case Token.color:
      ptT = new Point3f();
      break;
    case Token.property:
      data = viewer.getDataFloat((String) opValue);
      break;
    }

    int n = 0;
    int ivvMinMax = 0;
    int ivMinMax = 0;
    float fvMinMax = 0;
    double sum = 0;
    double sum2 = 0;
    switch (minmaxtype) {
    case Token.min:
      ivMinMax = Integer.MAX_VALUE;
      fvMinMax = Float.MAX_VALUE;
      break;
    case Token.max:
      ivMinMax = Integer.MIN_VALUE;
      fvMinMax = -Float.MAX_VALUE;
      break;
    }
    ModelSet modelSet = viewer.getModelSet();
    int mode = (isPt ? 3 : isString ? 2 : isInt ? 1 : 0);
    if (isAtoms) {
      boolean haveBitSet = (bs != null);
      int iModel = -1;
      int i0, i1;
      if (haveIndex) {
        i0 = index;
        i1 = index + 1;
      } else if (haveBitSet) {
        i0 = bs.nextSetBit(0);
        i1 = Math.min(viewer.getAtomCount(), bs.length());
      } else {
        i0 = 0;
        i1 = viewer.getAtomCount();
      }
      if (isSyntaxCheck)
        i1 = 0;
      for (int i = i0; i >= 0 && i < i1; i = (haveBitSet ? bs.nextSetBit(i + 1)
          : i + 1)) {
        n++;
        Atom atom = modelSet.getAtomAt(i);
        switch (mode) {
        case 0: // float
          float fv = Float.MAX_VALUE;
          switch (tok) {
          case Token.function:
            bsAtom.set(i);
            fv = ScriptVariable.fValue(getFunctionReturn(userFunction, params,
                tokenAtom));
            bsAtom.clear(i);
            break;
          case Token.property:
            fv = (data == null ? 0 : data[i]);
            break;
          case Token.distance:
            if (planeRef != null)
              fv = Measure.distanceToPlane(planeRef, atom);
            else
              fv = atom.distance(ptRef);
            break;
          default:
            fv = Atom.atomPropertyFloat(viewer, atom, tok);
          }
          if (fv == Float.MAX_VALUE || Float.isNaN(fv)
              && minmaxtype != Token.all) {
            n--; // don't count this one
            continue;
          }
          switch (minmaxtype) {
          case Token.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case Token.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case Token.all:
            vout.add(new Float(fv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += ((double) fv) * fv;
            // fall through
          case Token.sum:
          default:
            sum += fv;
          }
          break;
        case 1: // isInt
          int iv = 0;
          switch (tok) {
          case Token.symop:
            // a little weird:
            // First we determine how many operations we have in this model.
            // Then we get the symmetry bitset, which shows the assignments
            // of symmetry for this atom.
            if (atom.getModelIndex() != iModel)
              iModel = atom.getModelIndex();
            BitSet bsSym = atom.getAtomSymmetry();
            int p = 0;
            switch (minmaxtype) {
            case Token.min:
              ivvMinMax = Integer.MAX_VALUE;
              break;
            case Token.max:
              ivvMinMax = Integer.MIN_VALUE;
              break;
            }
            for (int k = bsSym.nextSetBit(0); k >= 0; k = bsSym
                .nextSetBit(k + 1)) {
              iv += k + 1;
              switch (minmaxtype) {
              case Token.min:
                ivvMinMax = Math.min(ivvMinMax, k + 1);
                break;
              case Token.max:
                ivvMinMax = Math.max(ivvMinMax, k + 1);
                break;
              }
              p++;
            }
            switch (minmaxtype) {
            case Token.min:
            case Token.max:
              iv = ivvMinMax;
            }
            n += p - 1;
            break;
          case Token.configuration:
          case Token.cell:
            error(ERROR_unrecognizedAtomProperty, Token.nameOf(tok));
          default:
            iv = Atom.atomPropertyInt(atom, tok);
          }
          switch (minmaxtype) {
          case Token.min:
            if (iv < ivMinMax)
              ivMinMax = iv;
            break;
          case Token.max:
            if (iv > ivMinMax)
              ivMinMax = iv;
            break;
          case Token.all:
            vout.add(new Integer(iv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += ((double) iv) * iv;
            // fall through
          case Token.sum:
          default:
            sum += iv;
          }
          break;
        case 2: // isString
          vout.add(Atom.atomPropertyString(atom, tok));
          break;
        case 3: // isPt
          Tuple3f t = Atom.atomPropertyTuple(atom, tok);
          if (t == null)
            error(ERROR_unrecognizedAtomProperty, Token.nameOf(tok));
          pt.add(t);
          if (minmaxtype == Token.all) {
            vout.add(new Point3f(pt));
            pt.set(0, 0, 0);
          }
          break;
        }
        if (haveIndex)
          break;
      }
    } else { // bonds
      boolean isAll = (bs == null);
      int i0 = (isAll ? 0 : bs.nextSetBit(0));
      int i1 = viewer.getBondCount();
      for (int i = i0; i >= 0 && i < i1; i = (isAll ? i + 1 : bs
          .nextSetBit(i + 1))) {
        n++;
        Bond bond = modelSet.getBondAt(i);
        switch (tok) {
        case Token.length:
          float fv = bond.getAtom1().distance(bond.getAtom2());
          switch (minmaxtype) {
          case Token.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case Token.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case Token.all:
            vout.add(new Float(fv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += (double) fv * fv;
            // fall through
          case Token.sum:
          default:
            sum += fv;
          }
          break;
        case Token.xyz:
          switch (minmaxtype) {
          case Token.all:
            pt.set(bond.getAtom1());
            pt.add(bond.getAtom2());
            pt.scale(0.5f);
            vout.add(new Point3f(pt));
            break;
          default:
            pt.add(bond.getAtom1());
            pt.add(bond.getAtom2());
            n++;
          }
          break;
        case Token.color:
          Graphics3D.colorPointFromInt(viewer.getColorArgbOrGray(bond
              .getColix()), ptT);
          switch (minmaxtype) {
          case Token.all:
            vout.add(new Point3f(ptT));
            break;
          default:
            pt.add(ptT);
          }
          break;
        default:
          error(ERROR_unrecognizedBondProperty, Token.nameOf(tok));
        }
      }
    }
    if (minmaxtype == Token.all) {
      if (asVector)
        return vout;
      int len = vout.size();
      if (isString && !isExplicitlyAll && len == 1)
        return vout.get(0);
      if (tok == Token.sequence) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++)
          sb.append((String) vout.get(i));
        return sb.toString();
      }
      if (allFloat) {
        Float[] fout = new Float[len];
        Point3f zero = (len > 0 && isPt ? new Point3f() : null);
        for (int i = len; --i >= 0;) {
          Object v = vout.get(i);
          switch (mode) {
          case 0:
            fout[i] = (Float) v;
            break;
          case 1:
            fout[i] = new Float(((Integer) v).floatValue());
            break;
          case 2:
            fout[i] = new Float(Parser.parseFloat((String) v));
            break;
          case 3:
            fout[i] = new Float(((Point3f) v).distance(zero));
            break;
          }
        }
        return fout;
      }
      String[] sout = new String[len];
      for (int i = len; --i >= 0;) {
        Object v = vout.get(i);
        if (v instanceof Point3f)
          sout[i] = Escape.escape((Point3f) v);
        else
          sout[i] = "" + vout.get(i);
      }
      return sout;
    }
    if (isPt)
      return (n == 0 ? pt : new Point3f(pt.x / n, pt.y / n, pt.z / n));
    if (n == 0 || n == 1 && minmaxtype == Token.stddev)
      return new Float(Float.NaN);
    if (isInt) {
      switch (minmaxtype) {
      case Token.min:
      case Token.max:
        return new Integer(ivMinMax);
      }
    }
    switch (minmaxtype) {
    case Token.min:
    case Token.max:
      sum = fvMinMax;
      break;
    case Token.sum:
      break;
    case Token.sum2:
      sum = sum2;
      break;
    case Token.stddev:
      // because SUM (x_i - X_av)^2 = SUM(x_i^2) - 2X_av SUM(x_i) + SUM(X_av^2)
      // = SUM(x_i^2) - 2nX_av^2 + nX_av^2
      // = SUM(x_i^2) - nX_av^2
      // = SUM(x_i^2) - [SUM(x_i)]^2 / n
      sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
      break;
    default:
      sum /= n;
      break;
    }
    return new Float(sum);
  }

  private void setBitsetProperty(BitSet bs, int tok, int iValue, float fValue,
                                 Token tokenValue) throws ScriptException {
    if (isSyntaxCheck || BitSetUtil.cardinalityOf(bs) == 0)
      return;
    String[] list = null;
    String sValue = null;
    float[] fvalues = null;
    int nValues;
    switch (tok) {
    case Token.xyz:
    case Token.fracxyz:
    case Token.vibxyz:
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
            error(ERROR_unrecognizedParameter, "ARRAY", list[i]);
          values[i] = (Point3f) o;
        }
        viewer.setAtomCoord(bs, tok, values);
      }
      return;
    case Token.color:
      if (tokenValue.tok == Token.point3f)
        iValue = Graphics3D.colorPtToInt((Point3f) tokenValue.value);
      else if (tokenValue.tok == Token.list) {
        list = (String[]) tokenValue.value;
        if ((nValues = list.length) == 0)
          return;
        int[] values = new int[nValues];
        for (int i = nValues; --i >= 0;) {
          Object pt = Escape.unescapePoint(list[i]);
          if (pt instanceof Point3f)
            values[i] = Graphics3D.colorPtToInt((Point3f) pt);
          else
            values[i] = Graphics3D.getArgbFromString(list[i]);
          if (values[i] == 0
              && (values[i] = Parser.parseInt(list[i])) == Integer.MIN_VALUE)
            error(ERROR_unrecognizedParameter, "ARRAY", list[i]);
        }
        setShapeProperty(JmolConstants.SHAPE_BALLS, "colorValues",
            values, bs);
        return;
      }
      setShapeProperty(JmolConstants.SHAPE_BALLS, "color",
          tokenValue.tok == Token.string ? tokenValue.value : new Integer(
              iValue), bs);
      return;
    case Token.label:
    case Token.format:
      if (tokenValue.tok == Token.list)
        list = (String[]) tokenValue.value;
      else
        sValue = ScriptVariable.sValue(tokenValue);
      viewer.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
      return;
    case Token.element:
    case Token.elemno:
      clearDefinedVariableAtomSets();
      break;
    }
    if (tokenValue.tok == Token.list || tokenValue.tok == Token.string) {
      list = (tokenValue.tok == Token.list ? (String[]) tokenValue.value
          : Parser.getTokens(ScriptVariable.sValue(tokenValue)));
      if ((nValues = list.length) == 0)
        return;
      fvalues = new float[nValues];
      for (int i = nValues; --i >= 0;)
        fvalues[i] = (tok == Token.element ? JmolConstants
            .elementNumberFromSymbol(list[i]) : Parser.parseFloat(list[i]));
      if (tokenValue.tok == Token.string && nValues == 1) {
        fValue = fvalues[0];
        iValue = (int) fValue;
        sValue = list[0];
        list = null;
        fvalues = null;
      }
    }
    viewer.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
  }

  // ///////////////////// general fields //////////////////////

  private final static int scriptLevelMax = 10;

  private Thread currentThread;
  protected Viewer viewer;
  protected ScriptCompiler compiler;
  private Hashtable definedAtomSets;
  private StringBuffer outputBuffer;

  private String contextPath = "";
  private String filename;
  private String functionName;
  private boolean isStateScript;
  private int scriptLevel;
  private int scriptReportingLevel = 0;
  private int commandHistoryLevelMax = 0;

  // created by Compiler:
  private Token[][] aatoken;
  private short[] lineNumbers;
  private int[][] lineIndices;
  private Hashtable contextVariables;

  public Hashtable getContextVariables() {
    return contextVariables;
  }

  private String script;

  public String getScript() {
    return script;
  }

  // specific to current statement
  protected int pc; // program counter
  private String thisCommand;
  private String fullCommand;
  private Token[] statement;
  private int statementLength;
  private int iToken;
  private int lineEnd;
  private int pcEnd;
  private String scriptExtensions;

  // ////////////////////// supporting methods for compilation and loading
  // //////////

  private boolean compileScript(String filename, String strScript,
                                boolean debugCompiler) {
    this.filename = filename;
    strScript = fixScriptPath(strScript, filename);
    getScriptContext(compiler.compile(filename, strScript, false, false,
        debugCompiler, false), false, false);
    isStateScript = (script.indexOf(Viewer.STATE_VERSION_STAMP) >= 0);
    String s = script;
    pc = setScriptExtensions();
    if (!isSyntaxCheck && viewer.isScriptEditorVisible()
        && strScript.indexOf(JmolConstants.SCRIPT_EDITOR_IGNORE) < 0)
      viewer.scriptStatus("");
    script = s;
    return !error;
  }

  private String fixScriptPath(String strScript, String filename) {
    if (filename != null && strScript.indexOf("$SCRIPT_PATH$") >= 0) {
      String path = filename;
      // we first check for paths into ZIP files and adjust accordingly
      int pt = Math.max(filename.lastIndexOf("|"), filename.lastIndexOf("/"));
      path = path.substring(0, pt + 1);
      strScript = TextFormat.simpleReplace(strScript, "$SCRIPT_PATH$/", path);
      // now replace the variable itself
      strScript = TextFormat.simpleReplace(strScript, "$SCRIPT_PATH$", path);
    }
    return strScript;
  }

  private int setScriptExtensions() {
    String extensions = scriptExtensions;
    if (extensions == null)
      return 0;
    int pt = extensions.indexOf("##SCRIPT_STEP");
    if (pt >= 0) {
      executionStepping = true;
    }
    pt = extensions.indexOf("##SCRIPT_START=");
    if (pt < 0)
      return 0;
    pt = Parser.parseInt(extensions.substring(pt + 15));
    if (pt == Integer.MIN_VALUE)
      return 0;
    for (pc = 0; pc < lineIndices.length; pc++) {
      if (lineIndices[pc][0] > pt || lineIndices[pc][1] >= pt)
        break;
    }
    if (pc > 0 && pc < lineIndices.length && lineIndices[pc][0] > pt)
      --pc;
    return pc;
  }

  private void runScript(String script) throws ScriptException {
    if (!viewer.isPreviewOnly())
      runScript(script, outputBuffer);
  }

  private boolean compileScriptFileInternal(String filename, String localPath,
                                            String remotePath, String scriptPath) {
    // from "script" command, with push/pop surrounding or viewer
    if (filename.toLowerCase().indexOf("javascript:") == 0)
      return compileScript(filename, viewer.jsEval(filename.substring(11)),
          debugScript);
    String[] data = new String[2];
    data[0] = filename;
    if (!viewer.getFileAsString(data, Integer.MAX_VALUE, false)) {
      setErrorMessage("io error reading " + data[0] + ": " + data[1]);
      return false;
    }
    this.filename = filename;
    String script = fixScriptPath(data[1], data[0]);
    if (scriptPath == null) {
      scriptPath = viewer.getFullPath(filename);
      scriptPath = scriptPath.substring(0, scriptPath.lastIndexOf("/"));
    }
    script = FileManager.setScriptFileReferences(script, localPath, remotePath,
        scriptPath);
    return compileScript(filename, script, debugScript);
  }

  // ///////////// Jmol parameter / user variable / function support
  // ///////////////

  private Object getParameter(String key, boolean asToken) {
    Object v = getContextVariableAsVariable(key);
    if (v == null)
      v = viewer.getParameter(key);
    if (asToken)
      return (v instanceof ScriptVariable ? (ScriptVariable) v : ScriptVariable
          .getVariable(v));
    return (v instanceof ScriptVariable ? ScriptVariable
        .oValue((ScriptVariable) v) : v);
  }

  private String getParameterEscaped(String var) {
    ScriptVariable v = getContextVariableAsVariable(var);
    return (v == null ? "" + viewer.getParameterEscaped(var) : Escape
        .escape(v.value));
  }

  private String getStringParameter(String var, boolean orReturnName) {
    ScriptVariable v = getContextVariableAsVariable(var);
    if (v != null)
      return ScriptVariable.sValue(v);
    String val = "" + viewer.getParameter(var);
    return (val.length() == 0 && orReturnName ? var : val);
  }

  private Object getNumericParameter(String var) {
    if (var.equalsIgnoreCase("_modelNumber")) {
      int modelIndex = viewer.getCurrentModelIndex();
      return new Integer(modelIndex < 0 ? 0 : viewer
          .getModelFileNumber(modelIndex));
    }
    ScriptVariable v = getContextVariableAsVariable(var);
    if (v == null) {
      Object val = viewer.getParameter(var);
      if (!(val instanceof String))
        return val;
      v = new ScriptVariable(Token.string, val);
    }
    return ScriptVariable.nValue(v);
  }

  private ScriptVariable getContextVariableAsVariable(String var) {
    if (var.equals("expressionBegin"))
      return null;
    var = var.toLowerCase();
    if (contextVariables != null && contextVariables.containsKey(var))
      return (ScriptVariable) contextVariables.get(var);
    ScriptContext context = thisContext;
    while (context != null) {
      if (context.contextVariables != null
          && context.contextVariables.containsKey(var))
        return (ScriptVariable) context.contextVariables.get(var);
      context = context.parentContext;
    }
    return null;
  }

  private Object getStringObjectAsVariable(String s, String key) {
    if (s == null || s.length() == 0)
      return s;
    Object v = ScriptVariable.unescapePointOrBitsetAsVariable(s);
    if (v instanceof String && key != null)
      v = viewer.setUserVariable(key, new ScriptVariable(Token.string,
          (String) v));
    return v;
  }

  private ParallelProcessor parallelProcessor;

  private ScriptVariable runFunction(String name, Vector params, Token tokenAtom, boolean getReturn) throws ScriptException {
    pushContext(null);
    contextPath += " >> function " + name;
    ScriptFunction function = viewer.getFunction(name);
    if (function == null)
      return null;
    functionName = name;
    if (function instanceof ParallelProcessor) {
      parallelProcessor = (ParallelProcessor) function;
      vProcess = null;
    }
    aatoken = function.aatoken;
    lineNumbers = function.lineNumbers;
    lineIndices = function.lineIndices;
    script = function.script;
    pc = 0;
    if (function.names != null) {
      contextVariables = new Hashtable();
      function.setVariables(contextVariables, params);
    }
    if (tokenAtom != null)
      contextVariables.put("_x", tokenAtom);
    instructionDispatchLoop(false);
    if (function.tok == Token.parallel) {
      ((ParallelProcessor) function).runAllProcesses(viewer);      
    }
    ScriptVariable v = (getReturn ? getContextVariableAsVariable("_retval") : null);
    popContext(false);
    return v;
  }

  protected ScriptVariable getFunctionReturn(String name, Vector params,
                                             ScriptVariable tokenAtom)
      throws ScriptException {
        
    return runFunction(name, params, tokenAtom, true);
  }

  private void clearDefinedVariableAtomSets() {
    definedAtomSets.remove("# variable");
  }

  /**
   * support for @xxx or define xxx commands
   * 
   */
  private void defineSets() {
    if (!definedAtomSets.containsKey("# static")) {
      for (int i = 0; i < JmolConstants.predefinedStatic.length; i++)
        defineAtomSet(JmolConstants.predefinedStatic[i]);
      defineAtomSet("# static");
    }
    if (definedAtomSets.containsKey("# variable"))
      return;
    for (int i = 0; i < JmolConstants.predefinedVariable.length; i++)
      defineAtomSet(JmolConstants.predefinedVariable[i]);
    // Now, define all the elements as predefined sets
    // hydrogen is handled specially, so don't define it

    int firstIsotope = JmolConstants.firstIsotope;
    // name ==> e_=n for all standard elements
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      String definition = "@" + JmolConstants.elementNameFromNumber(i) + " _e="
          + i;
      defineAtomSet(definition);
    }
    // _Xx ==> name for of all elements, isotope-blind
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      String definition = "@_" + JmolConstants.elementSymbolFromNumber(i) + " "
          + JmolConstants.elementNameFromNumber(i);
      defineAtomSet(definition);
    }
    // name ==> _e=nn for each alternative element
    for (int i = firstIsotope; --i >= 0;) {
      String definition = "@" + JmolConstants.altElementNameFromIndex(i)
          + " _e=" + JmolConstants.altElementNumberFromIndex(i);
      defineAtomSet(definition);
    }
    // these variables _e, _x can't be more than two characters
    // name ==> _isotope=iinn for each isotope
    // _T ==> _isotope=iinn for each isotope
    // _3H ==> _isotope=iinn for each isotope
    for (int i = JmolConstants.altElementMax; --i >= firstIsotope;) {
      String def = " element=" + JmolConstants.altElementNumberFromIndex(i);
      String definition = "@_" + JmolConstants.altElementSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@_" + JmolConstants.altIsotopeSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@" + JmolConstants.altElementNameFromIndex(i);
      if (definition.length() > 1)
        defineAtomSet(definition + def);
    }
    defineAtomSet("# variable");
  }

  private void defineAtomSet(String script) {
    if (script.indexOf("#") == 0) {
      definedAtomSets.put(script, Boolean.TRUE);
      return;
    }
    ScriptContext sc = compiler.compile("#predefine", script, true, false,
        false, false);
    if (sc.errorType != null) {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefined set compile error:"
              + script + "\ncompile error:" + sc.errorMessageUntranslated);
      return;
    }

    if (sc.aatoken.length != 1) {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
              + script);
      return;
    }
    Token[] statement = sc.aatoken[0];
    if (statement.length <= 2) {
      viewer.scriptStatus("JmolConstants.java ERROR: bad predefinition length:"
          + script);
      return;
    }
    int tok = statement[1].tok;
    if (!Token.tokAttr(tok, Token.identifier)
        && !Token.tokAttr(tok, Token.predefinedset)) {
      viewer.scriptStatus("JmolConstants.java ERROR: invalid variable name:"
          + script);
      return;
    }
    definedAtomSets.put(statement[1].value, statement);
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

  private BitSet lookupValue(String setName, boolean plurals)
      throws ScriptException {
    if (isSyntaxCheck) {
      return new BitSet();
    }
    defineSets();
    Object value = definedAtomSets.get(setName);
    boolean isDynamic = false;
    if (value == null) {
      value = definedAtomSets.get("!" + setName);
      isDynamic = (value != null);
    }
    if (value instanceof BitSet)
      return (BitSet) value;
    if (value instanceof Token[]) {
      pushContext(null);
      BitSet bs = expression((Token[]) value, -2, 0, true, false, true, true);
      popContext(false);
      if (!isDynamic)
        definedAtomSets.put(setName, bs);
      return bs;
    }
    if (plurals)
      return null;
    int len = setName.length();
    if (len < 5) // iron is the shortest
      return null;
    if (setName.charAt(len - 1) != 's')
      return null;
    if (setName.endsWith("ies"))
      setName = setName.substring(0, len - 3) + 'y';
    else
      setName = setName.substring(0, len - 1);
    return lookupValue(setName, true);
  }

  public void deleteAtomsInVariables(BitSet bsDeleted) {
    Enumeration e = definedAtomSets.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Object value = definedAtomSets.get(key);
      if (value instanceof BitSet)
        BitSetUtil.deleteBits((BitSet) value, bsDeleted);
    }
  }
  
  //Hashtable thisCommandVariables;

  /**
   * provides support for @x and @{....} in statements. The compiler passes on
   * these, because they must be integrated with the statement dynamically.
   * 
   * @param pc
   * @return a fixed token set -- with possible overrun of unused null tokens
   * 
   * @throws ScriptException
   */
  private boolean setStatement(int pc) throws ScriptException {
    statement = aatoken[pc];
    statementLength = statement.length;
    //thisCommandVariables = null;
    if (statementLength == 0)
      return true;
    Token[] fixed;
    int i;
    int tok;
    for (i = 1; i < statementLength; i++) {
      if (statement[i] == null) {
        statementLength = i;
        return true;
      }
      if (statement[i].tok == Token.define)
        break;
    }
    if (i == statementLength)// || isScriptCheck)
      return i == statementLength;
    fixed = new Token[statementLength];
    fixed[0] = statement[0];
    boolean isExpression = false;
    int j = 1;
    //thisCommandVariables = new Hashtable(); 
    for (i = 1; i < statementLength; i++) {
      if (statement[i] == null)
        continue;
      switch (tok = getToken(i).tok) {
      case Token.define:
        Object v;
        // Object var_set;
        String s;
        String var = parameterAsString(++i);
        boolean isClauseDefine = (tokAt(i) == Token.expressionBegin);
        if (isClauseDefine) {
          Vector val = (Vector) parameterExpression(++i, 0, "_var", true);
          if (val == null || val.size() == 0)
            error(ERROR_invalidArgument);
          i = iToken;
          ScriptVariable vt = (ScriptVariable) val.elementAt(0);
          v = (vt.tok == Token.list ? vt : ScriptVariable.oValue(vt));
//          thisCommandVariables.put("v", v);
        } else {
          v = getParameter(var, false);
//          thisCommandVariables.put(var, v);
        }
        tok = tokAt(0);
        boolean forceString = (Token.tokAttr(tok, Token.implicitStringCommand)
            || tok == Token.load || tok == Token.script); // for the file names
        if (v instanceof ScriptVariable) {
          fixed[j] = (Token) v;
          if (isExpression && fixed[j].tok == Token.list)
            fixed[j] = new ScriptVariable(Token.bitset, getAtomBitSet(this,
                ScriptVariable.sValue((ScriptVariable) fixed[j])));
        } else if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          // if (isExpression && !isClauseDefine
          // && (var_set = getParameter(var + "_set", false)) != null)
          // fixed[j] = new Token(Token.define, "" + var_set);
          // else
          fixed[j] = new Token(Token.integer, ((Integer) v).intValue(), v);

        } else if (v instanceof Float) {
          fixed[j] = new Token(Token.decimal, JmolConstants.modelValue("" + v),
              v);
        } else if (v instanceof String) {
          v = getStringObjectAsVariable((String) v, null);
          if (v instanceof ScriptVariable) {
            fixed[j] = (Token) v;
          } else {
            s = (String) v;
            if (isExpression) {
              fixed[j] = new Token(Token.bitset, getAtomBitSet(this, s));
            } else {
              // bit of a hack here....
              // identifiers cannot have periods; file names can, though
              // TODO: this is still a hack
              // what we really need to know is what the compiler
              // expects here -- a string or an identifier, because
              // they will be processed differently.
              // a filename with only letters and numbers will be
              // read incorrectly here as an identifier.

              // note that command keywords cannot be implemented as variables
              // because they are not Tfoken.identifiers in the first place.
              // but the identifier tok is important here because just below
              // there is a check for SET parameter name assignments.
              // even those may not work...

              tok = (isClauseDefine || forceString || s.indexOf(".") >= 0
                  || s.indexOf(" ") >= 0 || s.indexOf("=") >= 0
                  || s.indexOf(";") >= 0 || s.indexOf("[") >= 0
                  || s.indexOf("{") >= 0 ? Token.string : Token.identifier);
              fixed[j] = new Token(tok, v);
            }
          }
        } else if (v instanceof BitSet) {
          fixed[j] = new Token(Token.bitset, v);
        } else if (v instanceof Point3f) {
          fixed[j] = new Token(Token.point3f, v);
        } else if (v instanceof Point4f) {
          fixed[j] = new Token(Token.point4f, v);
        } else if (v instanceof Matrix3f) {
          fixed[j] = new Token(Token.matrix3f, v);
        } else if (v instanceof Matrix4f) {
          fixed[j] = new Token(Token.matrix4f, v);
        } else if (v instanceof String[]) {
          String[] sv = (String[]) v;
          if (sv.length > 0 && sv[1].startsWith("{")
              && !(Escape.unescapePoint(sv[1]) instanceof String)) {
            fixed[j] = new Token(Token.list, sv);
          } else {
            fixed[j] = new Token(Token.string, Escape.escape(sv, true));
          }
        } else {
          Point3f center = getObjectCenter(var, Integer.MIN_VALUE,
              Integer.MIN_VALUE);
          if (center == null)
            error(ERROR_invalidArgument);
          fixed[j] = new Token(Token.point3f, center);
        }
        if (j == 1 && statement[0].tok == Token.set
            && fixed[j].tok != Token.identifier)
          error(ERROR_invalidArgument);
        break;
      case Token.expressionBegin:
      case Token.expressionEnd:
        // @ in expression will be taken as SELECT
        isExpression = (tok == Token.expressionBegin);
        fixed[j] = statement[i];
        break;
      default:
        fixed[j] = statement[i];
      }

      j++;
    }
    statement = fixed;
    for (i = j; i < statement.length; i++)
      statement[i] = null;
    statementLength = j;
    
    
    return true;
  }

  // ///////////////// Script context support //////////////////////

  private void clearState(boolean tQuiet) {
    thisContext = null;
    scriptLevel = 0;
    setErrorMessage(null);
    contextPath = "";
    this.tQuiet = tQuiet;
  }

  private void pushContext(ContextToken token) throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      error(ERROR_tooManyScriptLevels);
    scriptLevel++;
    thisContext = getScriptContext();
    if (isCmdLine_c_or_C_Option)
      Logger.info("-->>-------------".substring(0, scriptLevel + 5)
          + scriptLevel + " " + filename);
    if (token != null) {
      contextVariables = token.contextVariables;
    }
  }

  private void popContext(boolean isPushPop) {
    if (isCmdLine_c_or_C_Option)
      Logger.info("--<<-------------".substring(0, scriptLevel + 5)
          + scriptLevel + " " + filename);
    if (scriptLevel == 0 || thisContext == null)
      return;
    scriptLevel--;
    getScriptContext(thisContext, true, isPushPop);
  }

  private ScriptContext thisContext = null;
  public ScriptContext getScriptContext() {
    ScriptContext context = new ScriptContext();
    context.scriptLevel = scriptLevel;
    context.parentContext = thisContext;
    context.contextPath = contextPath;
    context.filename = filename;
    context.parallelProcessor = parallelProcessor;
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
    context.isStateScript = isStateScript;

    context.errorMessage = errorMessage;
    context.errorType = errorType;
    context.iCommandError = iCommandError;
    context.isSyntaxCheck = isSyntaxCheck;
    context.executionStepping = executionStepping;
    context.executionPaused = executionPaused;
    context.scriptExtensions = scriptExtensions;
    return context;
  }

  private void getScriptContext(ScriptContext context, boolean isFull,
                                boolean isPushPop) {

    // just from the compiler:
    if (context == null)
      return;
    script = context.script;
    lineNumbers = context.lineNumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    contextVariables = context.contextVariables;
    scriptExtensions = context.scriptExtensions;
    if (!isFull) {
      error = (context.errorType != null);
      errorMessage = context.errorMessage;
      errorMessageUntranslated = context.errorMessageUntranslated;
      iCommandError = context.iCommandError;
      errorType = context.errorType;
      return;
    }

    contextPath = context.contextPath;
    filename = context.filename;
    parallelProcessor = context.parallelProcessor;
    functionName = context.functionName;
    iToken = context.iToken;
    outputBuffer = context.outputBuffer;
    isStateScript = context.isStateScript;
    thisContext = context.parentContext;

    if (isPushPop) {
      return;
    }
    statement = context.statement;
    statementLength = context.statementLength;
    pc = context.pc;
    lineEnd = context.lineEnd;
    pcEnd = context.pcEnd;
  }

  private String getContext(boolean withVariables) {
    StringBuffer sb = new StringBuffer();
    ScriptContext context = thisContext;
    while (context != null) {
      if (withVariables) {
        if (context.contextVariables != null) {
          sb.append(getScriptID(context));
          sb.append(StateManager.getVariableList(context.contextVariables, 80,
              true));
        }
      } else {
        sb.append(setErrorLineMessage(context.functionName, context.filename,
            getLinenumber(context), context.pc, statementAsString(
                context.statement, -9999)));
      }
      context = context.parentContext;
    }
    if (withVariables) {
      if (contextVariables != null) {
        sb.append(getScriptID(null));
        sb.append(StateManager.getVariableList(contextVariables, 80, true));
      }
    } else {
      sb.append(setErrorLineMessage(functionName, filename,
          getLinenumber(null), pc, statementAsString(statement, -9999)));
    }

    return sb.toString();
  }

  private int getLinenumber(ScriptContext c) {
    return (c == null ? lineNumbers[pc] : c.lineNumbers[c.pc]);
  }

  private String getScriptID(ScriptContext context) {
    String fuName = (context == null ? functionName : "function "
        + context.functionName);
    String fiName = (context == null ? filename : context.filename);
    return "\n# " + fuName + " (file " + fiName + ")\n";
  }

  // /////////////// error message support /////////////////

  private boolean error;
  private String errorMessage;
  protected String errorMessageUntranslated;
  protected String errorType;
  protected int iCommandError;

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorMessageUntranslated() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private void setErrorMessage(String err) {
    errorMessageUntranslated = null;
    if (err == null) {
      error = false;
      errorType = null;
      errorMessage = null;
      iCommandError = -1;
      return;
    }
    error = true;
    if (errorMessage == null) // there could be a compiler error from a script
      // command
      errorMessage = GT._("script ERROR: ");
    errorMessage += err;
  }

  private boolean ignoreError;

  private void planeExpected() throws ScriptException {
    error(ERROR_planeExpected, "{a b c d}",
        "\"xy\" \"xz\" \"yz\" \"x=...\" \"y=...\" \"z=...\"", "$xxxxx");
  }

  private void integerOutOfRange(int min, int max) throws ScriptException {
    error(ERROR_integerOutOfRange, "" + min, "" + max);
  }

  private void numberOutOfRange(float min, float max) throws ScriptException {
    error(ERROR_numberOutOfRange, "" + min, "" + max);
  }

  void error(int iError, int i) throws ScriptException {
    iToken = i;
    error(iError, null, null, null, false);
  }

  void error(int iError) throws ScriptException {
    error(iError, null, null, null, false);
  }

  void error(int iError, String value) throws ScriptException {
    error(iError, value, null, null, false);
  }

  void error(int iError, String value, String more) throws ScriptException {
    error(iError, value, more, null, false);
  }

  void error(int iError, String value, String more, String more2)
      throws ScriptException {
    error(iError, value, more, more2, false);
  }

  private void warning(int iError, String value, String more)
      throws ScriptException {
    error(iError, value, more, null, true);
  }

  void error(int iError, String value, String more, String more2,
             boolean warningOnly) throws ScriptException {
    String strError = ignoreError ? null : errorString(iError, value, more,
        more2, true);
    String strUntranslated = (!ignoreError && GT.getDoTranslate() ? errorString(
        iError, value, more, more2, false)
        : null);
    if (!warningOnly)
      evalError(strError, strUntranslated);
    showString(strError);
  }

  void evalError(String message, String strUntranslated) throws ScriptException {
    if (ignoreError)
      throw new NullPointerException();
    if (!isSyntaxCheck) {
      // String s = viewer.getSetHistory(1);
      // viewer.addCommand(s + CommandHistory.ERROR_FLAG);
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
      viewer.setBooleanProperty("refreshing", true);
    }
    throw new ScriptException(message, strUntranslated);
  }

  final static int ERROR_axisExpected = 0;
  final static int ERROR_backgroundModelError = 1;
  final static int ERROR_badArgumentCount = 2;
  final static int ERROR_badMillerIndices = 3;
  final static int ERROR_badRGBColor = 4;
  final static int ERROR_booleanExpected = 5;
  final static int ERROR_booleanOrNumberExpected = 6;
  final static int ERROR_booleanOrWhateverExpected = 7;
  final static int ERROR_colorExpected = 8;
  final static int ERROR_colorOrPaletteRequired = 9;
  final static int ERROR_commandExpected = 10;
  final static int ERROR_coordinateOrNameOrExpressionRequired = 11;
  final static int ERROR_drawObjectNotDefined = 12;
  final static int ERROR_endOfStatementUnexpected = 13;
  final static int ERROR_expressionExpected = 14;
  final static int ERROR_expressionOrIntegerExpected = 15;
  final static int ERROR_filenameExpected = 16;
  final static int ERROR_fileNotFoundException = 17;
  final static int ERROR_incompatibleArguments = 18;
  final static int ERROR_insufficientArguments = 19;
  final static int ERROR_integerExpected = 20;
  final static int ERROR_integerOutOfRange = 21;
  final static int ERROR_invalidArgument = 22;
  final static int ERROR_invalidParameterOrder = 23;
  final static int ERROR_keywordExpected = 24;
  final static int ERROR_moCoefficients = 25;
  final static int ERROR_moIndex = 26;
  final static int ERROR_moModelError = 27;
  final static int ERROR_moOccupancy = 28;
  final static int ERROR_moOnlyOne = 29;
  final static int ERROR_multipleModelsDisplayedNotOK = 30;
  final static int ERROR_noData = 31;
  final static int ERROR_noPartialCharges = 32;
  final static int ERROR_noUnitCell = 33;
  final static int ERROR_numberExpected = 34;
  final static int ERROR_numberMustBe = 35;
  final static int ERROR_numberOutOfRange = 36;
  final static int ERROR_objectNameExpected = 37;
  final static int ERROR_planeExpected = 38;
  final static int ERROR_propertyNameExpected = 39;
  final static int ERROR_spaceGroupNotFound = 40;
  final static int ERROR_stringExpected = 41;
  final static int ERROR_stringOrIdentifierExpected = 42;
  final static int ERROR_tooManyPoints = 43;
  final static int ERROR_tooManyScriptLevels = 44;
  final static int ERROR_unrecognizedAtomProperty = 45;
  final static int ERROR_unrecognizedBondProperty = 46;
  final static int ERROR_unrecognizedCommand = 47;
  final static int ERROR_unrecognizedExpression = 48;
  final static int ERROR_unrecognizedObject = 49;
  final static int ERROR_unrecognizedParameter = 50;
  final static int ERROR_unrecognizedParameterWarning = 51;
  final static int ERROR_unrecognizedShowParameter = 52;
  final static int ERROR_what = 53;
  final static int ERROR_writeWhat = 54;
  final static int ERROR_multipleModelsNotOK = 55;

  static String errorString(int iError, String value, String more,
                            String more2, boolean translated) {
    boolean doTranslate = false;
    if (!translated && (doTranslate = GT.getDoTranslate()) == true)
      GT.setDoTranslate(false);
    String msg;
    switch (iError) {
    default:
      msg = "Unknown error message number: " + iError;
      break;
    case ERROR_axisExpected:
      msg = GT._("x y z axis expected");
      break;
    case ERROR_backgroundModelError:
      msg = GT._("{0} not allowed with background model displayed");
      break;
    case ERROR_badArgumentCount:
      msg = GT._("bad argument count");
      break;
    case ERROR_badMillerIndices:
      msg = GT._("Miller indices cannot all be zero.");
      break;
    case ERROR_badRGBColor:
      msg = GT._("bad [R,G,B] color");
      break;
    case ERROR_booleanExpected:
      msg = GT._("boolean expected");
      break;
    case ERROR_booleanOrNumberExpected:
      msg = GT._("boolean or number expected");
      break;
    case ERROR_booleanOrWhateverExpected:
      msg = GT._("boolean, number, or {0} expected");
      break;
    case ERROR_colorExpected:
      msg = GT._("color expected");
      break;
    case ERROR_colorOrPaletteRequired:
      msg = GT._("a color or palette name (Jmol, Rasmol) is required");
      break;
    case ERROR_commandExpected:
      msg = GT._("command expected");
      break;
    case ERROR_coordinateOrNameOrExpressionRequired:
      msg = GT._("{x y z} or $name or (atom expression) required");
      break;
    case ERROR_drawObjectNotDefined:
      msg = GT._("draw object not defined");
      break;
    case ERROR_endOfStatementUnexpected:
      msg = GT._("unexpected end of script command");
      break;
    case ERROR_expressionExpected:
      msg = GT._("valid (atom expression) expected");
      break;
    case ERROR_expressionOrIntegerExpected:
      msg = GT._("(atom expression) or integer expected");
      break;
    case ERROR_filenameExpected:
      msg = GT._("filename expected");
      break;
    case ERROR_fileNotFoundException:
      msg = GT._("file not found");
      break;
    case ERROR_incompatibleArguments:
      msg = GT._("incompatible arguments");
      break;
    case ERROR_insufficientArguments:
      msg = GT._("insufficient arguments");
      break;
    case ERROR_integerExpected:
      msg = GT._("integer expected");
      break;
    case ERROR_integerOutOfRange:
      msg = GT._("integer out of range ({0} - {1})");
      break;
    case ERROR_invalidArgument:
      msg = GT._("invalid argument");
      break;
    case ERROR_invalidParameterOrder:
      msg = GT._("invalid parameter order");
      break;
    case ERROR_keywordExpected:
      msg = GT._("keyword expected");
      break;
    case ERROR_moCoefficients:
      msg = GT._("no MO coefficient data available");
      break;
    case ERROR_moIndex:
      msg = GT._("An MO index from 1 to {0} is required");
      break;
    case ERROR_moModelError:
      msg = GT._("no MO basis/coefficient data available for this frame");
      break;
    case ERROR_moOccupancy:
      msg = GT._("no MO occupancy data available");
      break;
    case ERROR_moOnlyOne:
      msg = GT._("Only one molecular orbital is available in this file");
      break;
    case ERROR_multipleModelsDisplayedNotOK:
      msg = GT._("{0} require that only one model be displayed");
      break;
    case ERROR_multipleModelsNotOK:
      msg = GT._("{0} requires that only one model be loaded");
      break;
    case ERROR_noData:
      msg = GT._("No data available");
      break;
    case ERROR_noPartialCharges:
      msg = GT
          ._("No partial charges were read from the file; Jmol needs these to render the MEP data.");
      break;
    case ERROR_noUnitCell:
      msg = GT._("No unit cell");
      break;
    case ERROR_numberExpected:
      msg = GT._("number expected");
      break;
    case ERROR_numberMustBe:
      msg = GT._("number must be ({0} or {1})");
      break;
    case ERROR_numberOutOfRange:
      msg = GT._("decimal number out of range ({0} - {1})");
      break;
    case ERROR_objectNameExpected:
      msg = GT._("object name expected after '$'");
      break;
    case ERROR_planeExpected:
      msg = GT
          ._("plane expected -- either three points or atom expressions or {0} or {1} or {2}");
      break;
    case ERROR_propertyNameExpected:
      msg = GT._("property name expected");
      break;
    case ERROR_spaceGroupNotFound:
      msg = GT._("space group {0} was not found.");
      break;
    case ERROR_stringExpected:
      msg = GT._("quoted string expected");
      break;
    case ERROR_stringOrIdentifierExpected:
      msg = GT._("quoted string or identifier expected");
      break;
    case ERROR_tooManyPoints:
      msg = GT._("too many rotation points were specified");
      break;
    case ERROR_tooManyScriptLevels:
      msg = GT._("too many script levels");
      break;
    case ERROR_unrecognizedAtomProperty:
      msg = GT._("unrecognized atom property");
      break;
    case ERROR_unrecognizedBondProperty:
      msg = GT._("unrecognized bond property");
      break;
    case ERROR_unrecognizedCommand:
      msg = GT._("unrecognized command");
      break;
    case ERROR_unrecognizedExpression:
      msg = GT._("runtime unrecognized expression");
      break;
    case ERROR_unrecognizedObject:
      msg = GT._("unrecognized object");
      break;
    case ERROR_unrecognizedParameter:
      msg = GT._("unrecognized {0} parameter");
      break;
    case ERROR_unrecognizedParameterWarning:
      msg = GT
          ._("unrecognized {0} parameter in Jmol state script (set anyway)");
      break;
    case ERROR_unrecognizedShowParameter:
      msg = GT._("unrecognized SHOW parameter --  use {0}");
      break;
    case ERROR_what:
      msg = "{0}";
      break;
    case ERROR_writeWhat:
      msg = GT._("write what? {0} or {1} \"filename\"");
      break;
    }
    if (msg.indexOf("{0}") < 0) {
      if (value != null)
        msg += ": " + value;
    } else {
      msg = TextFormat.simpleReplace(msg, "{0}", value);
      if (msg.indexOf("{1}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{1}", more);
      else if (more != null)
        msg += ": " + more;
      if (msg.indexOf("{2}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{2}", more);
    }
    if (doTranslate)
      GT.setDoTranslate(true);
    return msg;
  }

  String getExceptionContextTrace() {
    StringBuffer sb = new StringBuffer();
    for (;;) {
      sb.append(setErrorLineMessage(functionName, filename,
          getLinenumber(null), pc, statementAsString(statement, iToken)));
      if (scriptLevel > 0)
        popContext(false);
      else
        break;
    }
    return sb.toString();
  }

  static String setErrorLineMessage(String functionName, String filename,
                                    int lineCurrent, int pcCurrent,
                                    String lineInfo) {
    String err = "\n----";
    if (filename != null || functionName != null)
      err += "line " + lineCurrent + " command " + (pcCurrent + 1) + " of "
          + (functionName == null ? filename : "function " + functionName)
          + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  class ScriptException extends Exception {

    private String message;
    private String untranslated;

    ScriptException(String msg, String untranslated) {
      errorType = message = msg;
      iCommandError = pc;
      this.untranslated = (untranslated == null ? msg : untranslated);
      if (message == null) {
        message = "";
        return;
      }

      String s = getExceptionContextTrace();
      message += s;
      this.untranslated += s;
      if (isSyntaxCheck
          || msg.indexOf("file recognized as a script file:") >= 0)
        return;
      Logger.error("eval ERROR: " + toString());
      if (viewer.autoExit)
        viewer.exitJmol();
    }

    protected String getErrorMessageUntranslated() {
      return untranslated;
    }

    public String toString() {
      return message;
    }
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

  private String statementAsString(Token[] statement, int iTok) {
    if (statement.length == 0)
      return "";
    StringBuffer sb = new StringBuffer();
    int tok = statement[0].tok;
    switch (tok) {
    case Token.nada:
      String s = (String) statement[0].value;
      return (s.startsWith("/") ? "/" : "#") + s;
    case Token.end:
      if (statement.length == 2 && 
          (statement[1].tok == Token.function || statement[1].tok == Token.parallel))
        return ((ScriptFunction) (statement[1].value)).toString();
    }
    boolean useBraces = true;// (!Token.tokAttr(tok,
    // Token.atomExpressionCommand));
    boolean inBrace = false;
    boolean inClauseDefine = false;
    boolean setEquals = (tok == Token.set
        && ((String) statement[0].value) == "" && statement[0].intValue == '=' && tokAt(1) != Token.expressionBegin);
    int len = statement.length;
    for (int i = 0; i < len; ++i) {
      Token token = statement[i];
      if (token == null) {
        len = i;
        break;
      }
      if (iTok == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.append(' ');
      if (i == 2 && setEquals) {
        setEquals = false;
        if (token.tok != Token.opEQ)
          sb.append("= ");
      }
      if (iTok == i && token.tok != Token.expressionEnd)
        sb.append(">> ");
      switch (token.tok) {
      case Token.expressionBegin:
        if (useBraces)
          sb.append("{");
        continue;
      case Token.expressionEnd:
        if (inClauseDefine && i == statementLength - 1)
          useBraces = false;
        if (useBraces)
          sb.append("}");
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
        sb.append(ScriptVariable.sValue(token));
        continue;
      case Token.seqcode:
        sb.append('^');
        continue;
      case Token.spec_seqcode_range:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        token = statement[++i];
        sb.append(' ');
        // if (token.intValue == Integer.MAX_VALUE)
        sb.append(inBrace ? "-" : "- ");
        // fall through
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
        // fall through
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
        // not quite right -- for "inmath"
        if (token.intValue == Token.property) {
          sb.append((String) statement[++i].value).append(" ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(Token.nameOf(token.intValue)).append(" ");
        break;
      default:
        if (Token.tokAttr(token.tok, Token.identifier) || !logMessages)
          break;
        sb.append('\n').append(token.toString()).append('\n');
        continue;
      }
      if (token.value != null)
        // value SHOULD NEVER BE NULL, BUT JUST IN CASE...
        sb.append(token.value.toString());
    }
    if (iTok >= len - 1)
      sb.append(" <<");
    return sb.toString();
  }

  ///////////// shape get/set properties ////////////////

  private Object getShapeProperty(int shapeType, String propertyName) {
    return shapeManager.getShapeProperty(shapeType, propertyName, Integer.MIN_VALUE);
  }

  private boolean getShapeProperty(int shapeType, String propertyName,
                                  Object[] data) {
    return shapeManager.getShapeProperty(shapeType, propertyName, data);
  }
  
  private Object getShapeProperty(int shapeType, String propertyName, int index) {
    return shapeManager.getShapeProperty(shapeType, propertyName, index);
  }

  private void addShapeProperty(Vector propertyList, String key, Object value) {
    if (!isSyntaxCheck)
      propertyList.add(new Object[] { key, value });
  }

  private void loadShape(int iShape) {
      shapeManager.loadShape(iShape);
  }

  private void setObjectMad(int iShape, String name, int mad) {
    if (isSyntaxCheck)
      return;
    viewer.setObjectMad(iShape, name, mad);
  }

  private void setObjectArgb(String str, int argb) {
    if (isSyntaxCheck)
      return;
    viewer.setObjectArgb(str, argb);
  }

  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    if (isSyntaxCheck)
      return;
    shapeManager.setShapeProperty(shapeType, propertyName, propertyValue, null);
  }

  private void setShapeProperty(int iShape, String propertyName,
                                Object propertyValue, BitSet bs) {
    if (isSyntaxCheck)
      return;
    shapeManager.setShapeProperty(iShape, propertyName, propertyValue, bs);
  }

  private void setShapeSize(int shapeType, int size) {
      setShapeSize(shapeType, size, null);
  }

  private void setShapeSize(int shapeType, int size, BitSet bs) {
    // stars, halos, balls only
    if (isSyntaxCheck)
      return;
      shapeManager.setShapeSize(shapeType, size, null, null);
  }

  private void setShapeSize(int shapeType, RadiusData rd) {
    if (isSyntaxCheck)
      return;
      shapeManager.setShapeSize(shapeType, 0, rd, null);
  }

  ////////////////////  setting properties ////////////////////////
  
  private void setBooleanProperty(String key, boolean value) {
    if (!isSyntaxCheck)
      viewer.setBooleanProperty(key, value);
  }

  private boolean setIntProperty(String key, int value) {
    if (!isSyntaxCheck)
      viewer.setIntProperty(key, value);
    return true;
  }

  private boolean setFloatProperty(String key, float value) {
    if (!isSyntaxCheck)
      viewer.setFloatProperty(key, value);
    return true;
  }

  private void setStringProperty(String key, String value) {
    if (!isSyntaxCheck)
      viewer.setStringProperty(key, value);
  }

  //////////////////// showing strings /////////////////
  
  private void showString(String str) {
    showString(str, false);
  }

  private void showString(String str, boolean isPrint) {
    if (isSyntaxCheck)
      return;
    if (outputBuffer != null)
      outputBuffer.append(str).append('\n');
    else
      viewer.showString(str, isPrint);
  }

  private void scriptStatusOrBuffer(String s) {
    if (isSyntaxCheck)
      return;
    if (outputBuffer != null) {
      outputBuffer.append(s).append('\n');
      return;
    }
    viewer.scriptStatus(s);
  }

  
  ///////////////// expression processing ///////////////////

  private Token[] tempStatement;
  private boolean isBondSet;
  private Object expressionResult;

  private BitSet expression(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_badArgumentCount, index);
    return expression(statement, index, 0, true, false, true, true);
  }

  private BitSet expression(Token[] code, int pcStart, int pcStop,
                            boolean allowRefresh, boolean allowUnderflow,
                            boolean mustBeBitSet, boolean andNotDeleted)
      throws ScriptException {
    // note that this is general -- NOT just statement[]
    // errors reported would improperly access statement/line context
    // there should be no errors anyway, because this is for
    // predefined variables, but it is conceivable that one could
    // have a problem.

    isBondSet = false;
    if (code != statement) {
      tempStatement = statement;
      statement = code;
    }
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, false, false, mustBeBitSet);
    Object val;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    int nExpress = 0;
    int atomCount = viewer.getAtomCount();
    if (ignoreSubset)
      pcStart = -pcStart;
    ignoreSubset |= isSyntaxCheck;
    if (pcStop == 0 && code.length > pcStart)
      pcStop = pcStart + 1;
    // if (logMessages)
    // viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      Token instruction = code[pc];
      if (instruction == null)
        break;
      Object value = instruction.value;
      // if (logMessages)
      // viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.expressionBegin:
        pcStart = pc;
        pcStop = code.length;
        nExpress++;
        break;
      case Token.expressionEnd:
        nExpress--;
        if (nExpress > 0)
          continue;
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
        break; // ignore otherwise
      case Token.rightbrace:
        if (pc > 0 && code[pc - 1].tok == Token.leftbrace)
          rpn.addX(new BitSet());
        break;
      case Token.leftsquare:
        isInMath = true;
        rpn.addOp(instruction);
        break;
      case Token.rightsquare:
        isInMath = false;
        rpn.addOp(instruction);
        break;
      case Token.define:
        rpn.addX(getAtomBitSet(this, (String) value));
        break;
      case Token.hkl:
        rpn.addX(new ScriptVariable(instruction));
        rpn.addX(new ScriptVariable(Token.point4f, hklParameter(pc + 2)));
        pc = iToken;
        break;
      case Token.plane:
        rpn.addX(new ScriptVariable(instruction));
        rpn.addX(new ScriptVariable(Token.point4f, planeParameter(pc + 2)));
        pc = iToken;
        break;
      case Token.coord:
        rpn.addX(new ScriptVariable(instruction));
        rpn.addX(getPoint3f(pc + 2, true));
        pc = iToken;
        break;
      case Token.string:
        rpn.addX(new ScriptVariable(instruction));
        // note that the compiler has changed all within() types to strings.
        if (((String) value).equals("hkl")) {
          rpn.addX(new ScriptVariable(Token.point4f, hklParameter(pc + 2)));
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
        rpn.addX(new ScriptVariable(instruction));
        break;
      case Token.selected:
        rpn.addX(BitSetUtil.copy(viewer.getSelectionSet()));
        break;
      case Token.subset:
        BitSet bsSubset = viewer.getSelectionSubset();
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
      case Token.spec_atom:
        int atomID = instruction.intValue;
        if (atomID > 0)
          rpn.addX(compareInt(Token.atomid, null, Token.opEQ, atomID));
        else
          rpn.addX(getAtomBits(instruction.tok, (String) value));
        break;
      case Token.carbohydrate:
      case Token.dna:
      case Token.hetero:
      case Token.isaromatic:
      case Token.nucleic:
      case Token.protein:
      case Token.purine:
      case Token.pyrimidine:
      case Token.rna:
      case Token.spec_name_pattern:
      case Token.spec_alternate:
      case Token.specialposition:
      case Token.symmetry:
      case Token.unitcell:
        rpn.addX(getAtomBits(instruction.tok, (String) value));
        break;
      case Token.spec_model:
        // from select */1002 or */1000002 or */1.2
        // */1002 is equivalent to 1.2 when more than one file is present
      case Token.spec_model2:
        // from just using the number 1.2
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE && value instanceof Integer) {
          // from select */n
          iModel = ((Integer) value).intValue();
          if (!viewer.haveFileSet()) {
            rpn.addX(getAtomBits(Token.spec_model, new Integer(iModel)));
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
        rpn
            .addX(getAtomBits(instruction.tok,
                new Integer(instruction.intValue)));
        break;
      case Token.spec_seqcode:
        if (isInMath)
          rpn.addXNum(ScriptVariable.intVariable(instruction.intValue));
        else
          rpn.addX(getAtomBits(Token.spec_seqcode, new Integer(
              getSeqCode(instruction))));
        break;
      case Token.spec_seqcode_range:
        if (isInMath) {
          rpn.addXNum(ScriptVariable.intVariable(instruction.intValue));
          rpn.addX(Token.tokenMinus);
          rpn.addXNum(ScriptVariable.intVariable(code[++pc].intValue));
          break;
        }
        int chainID = (pc + 3 < code.length && code[pc + 2].tok == Token.opAnd
            && code[pc + 3].tok == Token.spec_chain ? code[pc + 3].intValue
            : '\t');
        rpn.addX(getAtomBits(Token.spec_seqcode_range, new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]), chainID }));
        if (chainID != '\t')
          pc += 2;
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
      case Token.hydrogen:
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.helix:
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
        if (tokWhat == Token.configuration && tokOperator != Token.opEQ)
          error(ERROR_invalidArgument);
        if (isSyntaxCheck) {
          rpn.addX(new BitSet());
          break;
        }
        boolean isModel = (tokWhat == Token.model);
        boolean isIntProperty = Token.tokAttr(tokWhat, Token.intproperty);
        boolean isFloatProperty = Token.tokAttr(tokWhat, Token.floatproperty);
        boolean isIntOrFloat = isIntProperty && isFloatProperty;
        boolean isStringProperty = !isIntProperty
            && Token.tokAttr(tokWhat, Token.strproperty);
        if (tokWhat == Token.element)
          isIntProperty = !(isStringProperty = false);
        int tokValue = code[pc].tok;
        comparisonValue = code[pc].intValue;
        float comparisonFloat = Float.NaN;
        if (val instanceof Point3f) {
          if (tokWhat == Token.color) {
            comparisonValue = Graphics3D.colorPtToInt((Point3f) val);
            tokValue = Token.integer;
            isIntProperty = true;
          }
        } else if (val instanceof String) {
          if (tokWhat == Token.color) {
            comparisonValue = Graphics3D.getArgbFromString((String) val);
            if (comparisonValue == 0
                && Token.tokAttr(tokValue, Token.identifier)) {
              val = getStringParameter((String) val, true);
              if (((String) val).startsWith("{")) {
                val = Escape.unescapePoint((String) val);
                if (val instanceof Point3f)
                  comparisonValue = Graphics3D.colorPtToInt((Point3f) val);
                else
                  comparisonValue = 0;
              } else {
                comparisonValue = Graphics3D.getArgbFromString((String) val);
              }
            }
            tokValue = Token.integer;
            isIntProperty = true;
          } else if (isStringProperty) {
            if (Token.tokAttr(tokValue, Token.identifier))
              val = getStringParameter((String) val, true);
          } else {
            if (Token.tokAttr(tokValue, Token.identifier))
              val = getNumericParameter((String) val);
            if (val instanceof String) {
              if (tokWhat == Token.structure || tokWhat == Token.element)
                isStringProperty = !(isIntProperty = (comparisonValue != Integer.MAX_VALUE));
              else
                val = ScriptVariable.nValue(code[pc]);
            }
            if (val instanceof Integer)
              comparisonFloat = comparisonValue = ((Integer) val).intValue();
            else if (val instanceof Float && isModel)
              comparisonValue = ModelCollection
                  .modelFileNumberFromFloat(((Float) val).floatValue());
          }
        }
        if (isStringProperty && !(val instanceof String)) {
          val = "" + val;
        }
        if (val instanceof Integer || tokValue == Token.integer) {
          if (isModel) {
            if (comparisonValue >= 1000000)
              tokWhat = -Token.model;
          } else if (isIntOrFloat) {
            isFloatProperty = false;
          } else if (isFloatProperty) {
            comparisonFloat = comparisonValue;
          }
        } else if (val instanceof Float) {
          if (isModel) {
            tokWhat = -Token.model;
          } else {
            comparisonFloat = ((Float) val).floatValue();
            if (isIntOrFloat) {
              isIntProperty = false;
            } else if (isIntProperty) {
              comparisonValue = (int) comparisonFloat;
            }
          }
        } else if (!isStringProperty) {
          iToken++;
          error(ERROR_invalidArgument);
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
        if (value != null && ((String) value).indexOf("-") >= 0) {
          if (isIntProperty)
            comparisonValue = -comparisonValue;
          else if (!Float.isNaN(comparisonFloat))
            comparisonFloat = -comparisonFloat;
        }
        float[] data = (tokWhat == Token.property ? viewer
            .getDataFloat(property) : null);
        rpn.addX(isIntProperty ? compareInt(tokWhat, data, tokOperator,
            comparisonValue) : isStringProperty ? compareString(tokWhat,
            tokOperator, (String) val) : compareFloat(tokWhat, data,
            tokOperator, comparisonFloat));
        break;
      case Token.bitset:
      case Token.point3f:
      case Token.point4f:
        rpn.addX(value);
        break;
      case Token.decimal:
      case Token.integer:
        rpn.addXNum(new ScriptVariable(instruction));
        break;
      default:
        if (Token.tokAttr(instruction.tok, Token.mathop)) {
          rpn.addOp(instruction);
          break;
        }
//        if (instruction.tok == Token.identifier) {
          val = getParameter((String) value, false);
          if (val instanceof String)
            val = getStringObjectAsVariable((String) val, null);
          if (val instanceof String || val instanceof String[])
            val = lookupIdentifierValue((String) value);
          rpn.addX(val);
          break;
//        }
   //     error(ERROR_unrecognizedExpression);
      }
    }
    expressionResult = rpn.getResult(allowUnderflow, null);
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!isSyntaxCheck)
        rpn.dumpStacks("after getResult");
      error(ERROR_endOfStatementUnexpected);
    }
    expressionResult = ((ScriptVariable) expressionResult).value;
    if (expressionResult instanceof String
        && (mustBeBitSet || ((String) expressionResult).startsWith("({"))) {
      // allow for select @{x} where x is a string that can evaluate to a bitset
      expressionResult = (isSyntaxCheck ? new BitSet() : getAtomBitSet(this,
          (String) expressionResult));
    }
    if (!mustBeBitSet && !(expressionResult instanceof BitSet))
      return null; // because result is in expressionResult in that case
    BitSet bs = (expressionResult instanceof BitSet ? (BitSet) expressionResult
        : new BitSet());
    isBondSet = (expressionResult instanceof BondSet);
    if (!isBondSet) {
      viewer.excludeAtoms(bs, ignoreSubset);
    }
    if (tempStatement != null) {
      statement = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  private BitSet compareFloat(int tokWhat, float[] data, int tokOperator,
                              float comparisonFloat) {
    BitSet bs = new BitSet();
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.getModelSet();
    Atom[] atoms = modelSet.atoms;
    float propertyFloat = 0;
    viewer.autoCalculate(tokWhat);
    for (int i = atomCount; --i >= 0;) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyFloat = Atom.atomPropertyFloat(viewer, atom, tokWhat);
        break;
      case Token.property:
        if (data == null || data.length <= i)
          continue;
        propertyFloat = data[i];
      }
      match = compareFloat(tokOperator, propertyFloat, comparisonFloat);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private BitSet compareString(int tokWhat, int tokOperator,
                               String comparisonString) throws ScriptException {
    BitSet bs = new BitSet();
    Atom[] atoms = viewer.getModelSet().atoms;
    int atomCount = viewer.getAtomCount();
    boolean isCaseSensitive = (tokWhat == Token.chain && viewer
        .getChainCaseSensitive());
    if (!isCaseSensitive)
      comparisonString = comparisonString.toLowerCase();
    for (int i = atomCount; --i >= 0;) {
      String propertyString = Atom.atomPropertyString(atoms[i], tokWhat);
      if (!isCaseSensitive)
        propertyString = propertyString.toLowerCase();
      if (compareString(tokOperator, propertyString, comparisonString))
        bs.set(i);
    }
    return bs;
  }

  protected BitSet compareInt(int tokWhat, float[] data, int tokOperator,
                              int comparisonValue) {
    int propertyValue = Integer.MAX_VALUE;
    BitSet propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.getModelSet();
    Atom[] atoms = modelSet.atoms;
    int imax = -1;
    int imin = 0;
    int iModel = -1;
    int[] cellRange = null;
    int nOps = 0;
    BitSet bs;
    // preliminary setup
    switch (tokWhat) {
    case Token.symop:
      switch (bitsetComparator) {
      case Token.opGE:
      case Token.opGT:
        imax = Integer.MAX_VALUE;
        break;
      }
      break;
    case Token.atomindex:
      switch (tokOperator) {
      case Token.opLT:
        return BitSetUtil.newBitSet(0, comparisonValue);
      case Token.opLE:
        return BitSetUtil.newBitSet(0, comparisonValue + 1);
      case Token.opGE:
        return BitSetUtil.newBitSet(comparisonValue, atomCount);
      case Token.opGT:
        return BitSetUtil.newBitSet(comparisonValue + 1, atomCount);
      case Token.opEQ:
        return (comparisonValue < atomCount ? BitSetUtil.newBitSet(
            comparisonValue, comparisonValue + 1) : new BitSet());
      case Token.opNE:
      default:
        bs = BitSetUtil.setAll(atomCount);
        if (comparisonValue >= 0)
          bs.clear(comparisonValue);
        return bs;
      }
    }
    bs = new BitSet(atomCount);
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyValue = Atom.atomPropertyInt(atom, tokWhat);
        break;
      case Token.configuration:
        // these are all-inclusive; no need to do a by-atom comparison
        return BitSetUtil.copy(viewer.getConformation(-1, comparisonValue - 1,
            false));
      case Token.symop:
        propertyBitSet = atom.getAtomSymmetry();
        if (propertyBitSet == null)
          continue;
        if (atom.getModelIndex() != iModel) {
          iModel = atom.getModelIndex();
          cellRange = modelSet.getModelCellRange(iModel);
          nOps = modelSet.getModelSymmetryCount(iModel);
        }
        if (bitsetBaseValue >= 200) {
          if (cellRange == null)
            continue;
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555) for
           * this the comparision is only with the translational component; the
           * symop itself must match thus: select symop!=1655 selects all
           * symop=1 and translation !=655 select symop>=2555 selects all
           * symop=2 and translation >555 symop >=200 indicates any symop in the
           * specified translation (a few space groups have > 100 operations)
           * 
           * Note that when normalization is not done, symop=1555 may not be in
           * the base unit cell. Everything is relative to wherever the base
           * atoms ended up, usually in 555, but not necessarily.
           * 
           * The reason this is tied together an atom may have one translation
           * for one symop and another for a different one.
           * 
           * Bob Hanson - 10/2006
           */
          comparisonValue = bitsetBaseValue % 1000;
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0) {
            match = true;
          } else if (nOps == 0 || symop >= 0
              && !(match = propertyBitSet.get(symop))) {
            continue;
          }
          bitsetComparator = Token.none;
          if (symop < 0)
            propertyValue = atom.getCellTranslation(comparisonValue, cellRange,
                nOps);
          else
            propertyValue = atom.getSymmetryTranslation(symop, cellRange, nOps);
        } else if (nOps > 0) {
          if (comparisonValue > nOps) {
            if (bitsetComparator != Token.opLT
                && bitsetComparator != Token.opLE)
              continue;
          }
          if (bitsetComparator == Token.opNE) {
            if (comparisonValue > 0 && comparisonValue <= nOps
                && !propertyBitSet.get(comparisonValue)) {
              bs.set(i);
            }
            continue;
          }
        }
        switch (bitsetComparator) {
        case Token.opLT:
          imax = comparisonValue - 1;
          break;
        case Token.opLE:
          imax = comparisonValue;
          break;
        case Token.opGE:
          imin = comparisonValue - 1;
          break;
        case Token.opGT:
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
        if (imin < imax) {
          int pt = propertyBitSet.nextSetBit(imin);
          if (pt >= 0 && pt < imax)
            match = true;
        }
        // note that a symop property can be both LE and GT !
        if (!match || propertyValue == Integer.MAX_VALUE)
          tokOperator = Token.none;
      }
      if (tokOperator != Token.none)
        match = compareInt(tokOperator, propertyValue, comparisonValue);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private boolean compareString(int tokOperator, String propertyValue,
                                String comparisonValue) throws ScriptException {
    switch (tokOperator) {
    case Token.opEQ:
    case Token.opNE:
      return (TextFormat.isMatch(propertyValue, comparisonValue, true, true) == (tokOperator == Token.opEQ));
    default:
      error(ERROR_invalidArgument);
    }
    return false;
  }

  private static boolean compareInt(int tokOperator, int propertyValue,
                                    int comparisonValue) {
    switch (tokOperator) {
    case Token.opLT:
      return propertyValue < comparisonValue;
    case Token.opLE:
      return propertyValue <= comparisonValue;
    case Token.opGE:
      return propertyValue >= comparisonValue;
    case Token.opGT:
      return propertyValue > comparisonValue;
    case Token.opEQ:
      return propertyValue == comparisonValue;
    case Token.opNE:
      return propertyValue != comparisonValue;
    }
    return false;
  }

  private static boolean compareFloat(int tokOperator, float propertyFloat,
                                      float comparisonFloat) {
    switch (tokOperator) {
    case Token.opLT:
      return propertyFloat < comparisonFloat;
    case Token.opLE:
      return propertyFloat <= comparisonFloat;
    case Token.opGE:
      return propertyFloat >= comparisonFloat;
    case Token.opGT:
      return propertyFloat > comparisonFloat;
    case Token.opEQ:
      return propertyFloat == comparisonFloat;
    case Token.opNE:
      return propertyFloat != comparisonFloat;
    }
    return false;
  }

  private BitSet getAtomBits(int tokType, Object specInfo) {
    return (isSyntaxCheck ? new BitSet() : viewer
        .getAtomBits(tokType, specInfo));
  }

  private static int getSeqCode(Token instruction) {
    return (instruction.intValue != Integer.MAX_VALUE ? Group.getSeqcode(
        instruction.intValue, ' ') : ((Integer) instruction.value).intValue());
  }

  /*
   * ****************************************************************************
   * ============================================================== checks and
   * parameter retrieval
   * ==============================================================
   */

  private int checkLast(int i) throws ScriptException {
    return checkLength(i + 1) - 1;
  }

  private int checkLength(int length) throws ScriptException {
    if (length >= 0)
      return checkLength(length, 0);
    // max
    if (statementLength > -length) {
      iToken = -length;
      error(ERROR_badArgumentCount);
    }
    return statementLength;
  }

  private int checkLength(int length, int errorPt) throws ScriptException {
    if (statementLength != length) {
      iToken = errorPt > 0 ? errorPt : statementLength;
      error(errorPt > 0 ? ERROR_invalidArgument : ERROR_badArgumentCount);
    }
    return statementLength;
  }

  private int checkLength23() throws ScriptException {
    iToken = statementLength;
    if (statementLength != 2 && statementLength != 3)
      error(ERROR_badArgumentCount);
    return statementLength;
  }

  private int checkLength34() throws ScriptException {
    iToken = statementLength;
    if (statementLength != 3 && statementLength != 4)
      error(ERROR_badArgumentCount);
    return statementLength;
  }

  private int theTok;
  private Token theToken;

  private Token getToken(int i) throws ScriptException {
    if (!checkToken(i))
      error(ERROR_endOfStatementUnexpected);
    theToken = statement[i];
    theTok = theToken.tok;
    return theToken;
  }

  private int tokAt(int i) {
    return (i < statementLength  && statement[i] != null ? statement[i].tok : Token.nada);
  }

  private int tokAt(int i, Token[] args) {
    return (i < args.length && args[i] != null ? args[i].tok : Token.nada);
  }

  private Token tokenAt(int i, Token[] args) {
    return (i < args.length ? args[i] : null);
  }

  private boolean checkToken(int i) {
    return (iToken = i) < statementLength;
  }

  private int modelNumberParameter(int index) throws ScriptException {
    int iFrame = 0;
    boolean useModelNumber = false;
    switch (tokAt(index)) {
    case Token.integer:
      useModelNumber = true;
      // fall through
    case Token.decimal:
      iFrame = getToken(index).intValue; // decimal Token intValue is
      // model/frame number encoded
      break;
    case Token.string:
      iFrame = JmolConstants.modelValue(stringParameter(index));
      break;
    default:
      error(ERROR_invalidArgument);
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
      error(ERROR_endOfStatementUnexpected);
    return (theTok == Token.integer ? "" + theToken.intValue : ""
        + theToken.value);
  }

  private int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.integer)
        return theToken.intValue;
    error(ERROR_integerExpected);
    return 0;
  }

  private int intParameter(int i, int min, int max) throws ScriptException {
    int val = intParameter(i);
    if (val < min || val > max)
      integerOutOfRange(min, max);
    return val;
  }

  private boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case Token.integer:
    case Token.decimal:
      return true;
    }
    return false;
  }

  private float floatParameter(int i, float min, float max)
      throws ScriptException {
    float val = floatParameter(i);
    if (val < min || val > max)
      numberOutOfRange(min, max);
    return val;
  }

  private float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      getToken(index);
      switch (theTok) {
      case Token.spec_seqcode_range:
        return -theToken.intValue;
      case Token.spec_seqcode:
      case Token.integer:
        return theToken.intValue;
      case Token.spec_model2:
      case Token.decimal:
        return ((Float) theToken.value).floatValue();
      }
    }
    error(ERROR_numberExpected);
    return 0;
  }

  /**
   * process a general string or set of parameters as an array of floats,
   * allowing for relatively free form input
   * 
   * @param i
   * @param nMin
   * @param nMax
   * @return array of floats
   * @throws ScriptException
   */
  private float[] floatParameterSet(int i, int nMin, int nMax)
      throws ScriptException {
    int tok = tokAt(i);
    boolean haveBrace = (tok == Token.leftbrace);
    boolean haveSquare = (tok == Token.leftsquare);
    float[] fparams = null;
    Vector v = new Vector();
    int n = 0;
    if (haveBrace || haveSquare)
      i++;
    Point3f pt;
    if (tokAt(i) == Token.string) {
      String s = stringParameter(i);
      s = TextFormat.replaceAllCharacters(s, "{},[]\"'", ' ');
      fparams = Parser.parseFloatArray(s);
      n = fparams.length;
    } else {
      while (n < nMax) {
        tok = tokAt(i);
        if (haveBrace && tok == Token.rightbrace || haveSquare
            && tok == Token.rightsquare)
          break;
        switch (tok) {
        case Token.comma:
        case Token.leftbrace:
        case Token.rightbrace:
          break;
        case Token.string:
          break;
        case Token.point3f:
          pt = getPoint3f(i, false);
          v.add(new Float(pt.x));
          v.add(new Float(pt.y));
          v.add(new Float(pt.z));
          n += 3;
          break;
        case Token.point4f:
          Point4f pt4 = getPoint4f(i);
          v.add(new Float(pt4.x));
          v.add(new Float(pt4.y));
          v.add(new Float(pt4.z));
          v.add(new Float(pt4.w));
          n += 4;
          break;
        default:
          v.add(new Float(floatParameter(i)));
          n++;
        }
        i++;
      }
    }
    if (haveBrace && tokAt(i++) != Token.rightbrace || haveSquare
        && tokAt(i++) != Token.rightsquare)
      error(ERROR_invalidArgument);
    iToken = i - 1;
    if (n < nMin || n > nMax)
      error(ERROR_invalidArgument);
    if (fparams == null) {
      fparams = new float[n];
      for (int j = 0; j < n; j++)
        fparams[j] = ((Float) v.get(j)).floatValue();
    }
    return fparams;
  }

  private String stringParameter(int index) throws ScriptException {
    if (!checkToken(index) || getToken(index).tok != Token.string)
      error(ERROR_stringExpected);
    return (String) theToken.value;
  }

  private String[] stringParameterSet(int i) throws ScriptException {
    switch (tokAt(i)) {
    case Token.string:
      String s = stringParameter(i);
      if (s.startsWith("[\"")) {
        Object o = viewer.evaluateExpression(s);
        if (o instanceof String)
          return TextFormat.split((String) o, '\n');
      }
      return new String[] { s };
    case Token.leftsquare:
      ++i;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    int tok;
    Vector v = new Vector();
    while ((tok = tokAt(i)) != Token.rightsquare) {
      switch (tok) {
      case Token.comma:
        break;
      case Token.string:
        v.add(stringParameter(i));
        break;
      default:
      case Token.nada:
        error(ERROR_invalidArgument);
      }
      i++;
    }
    iToken = i;
    int n = v.size();
    String[] sParams = new String[n];
    for (int j = 0; j < n; j++)
      sParams[j] = (String) v.get(j);
    return sParams;
  }

  private String objectNameParameter(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_objectNameExpected);
    return parameterAsString(index);
  }

  private boolean booleanParameter(int i) throws ScriptException {
    if (statementLength == i)
      return true;
    switch (getToken(checkLast(i)).tok) {
    case Token.on:
      return true;
    case Token.off:
      return false;
    default:
      error(ERROR_booleanExpected);
    }
    return false;
  }

  private Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case Token.bitset:
    case Token.expressionBegin:
      BitSet bs = expression(statement, i, 0, true, false, false, true);
      if (bs != null)
        return viewer.getAtomSetCenter(bs);
      if (expressionResult instanceof Point3f)
        return (Point3f) expressionResult;
      error(ERROR_invalidArgument);
      break;
    case Token.leftbrace:
    case Token.point3f:
      return getPoint3f(i, true);
    }
    error(ERROR_invalidArgument);
    // impossible return
    return null;
  }

  private boolean isCenterParameter(int i) {
    int tok = tokAt(i);
    return (tok == Token.dollarsign || tok == Token.leftbrace
        || tok == Token.expressionBegin || tok == Token.point3f || tok == Token.bitset);
  }

  private Point3f centerParameter(int i) throws ScriptException {
    return centerParameter(i, Integer.MIN_VALUE);
  }

  private Point3f centerParameter(int i, int modelIndex) throws ScriptException {
    Point3f center = null;
    expressionResult = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case Token.dollarsign:
        String id = objectNameParameter(++i);
        int index = Integer.MIN_VALUE;
        // allow for $pt2.3 -- specific vertex
        if (tokAt(i + 1) == Token.leftsquare) {
          index = intParameter(i + 2);
          if (getToken(i + 3).tok != Token.rightsquare)
            error(ERROR_invalidArgument);
        }
        if (isSyntaxCheck)
          return new Point3f();
        if ((center = getObjectCenter(id, index, modelIndex)) == null)
          error(ERROR_drawObjectNotDefined, id);
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
      error(ERROR_coordinateOrNameOrExpressionRequired);
    return center;
  }

  private Point4f planeParameter(int i) throws ScriptException {
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    Point4f plane = null;
    boolean isNegated = (tokAt(i) == Token.minus);
    if (isNegated)
      i++;
    if (i < statementLength)
      switch (getToken(i).tok) {
      case Token.point4f:
        plane = (Point4f) theToken.value;
        break;
      case Token.dollarsign:
        String id = objectNameParameter(++i);
        if (isSyntaxCheck)
          return new Point4f();
        int shapeType = shapeManager.getShapeIdFromObjectName(id);
        switch (shapeType) {
        case JmolConstants.SHAPE_DRAW:
          setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", id);
          Point3f[] points = (Point3f[]) getShapeProperty(
              JmolConstants.SHAPE_DRAW, "vertices");
          if (points == null || points.length < 3 || points[0] == null
              || points[1] == null || points[2] == null)
            break;
          plane = Measure.getPlaneThroughPoints(points[0], points[1],
              points[2], new Vector3f(), vAB, vAC);
          break;
        case JmolConstants.SHAPE_ISOSURFACE:
          setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID", id);
          plane = (Point4f) getShapeProperty(
              JmolConstants.SHAPE_ISOSURFACE, "plane");
          break;
        }
        break;
      case Token.x:
        if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
          evalError("x=?", null);
        plane = new Point4f(1, 0, 0, -floatParameter(i));
        break;
      case Token.y:
        if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
          evalError("y=?", null);
        plane = new Point4f(0, 1, 0, -floatParameter(i));
        break;
      case Token.z:
        if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
          evalError("z=?", null);
        plane = new Point4f(0, 0, 1, -floatParameter(i));
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
        break;
      case Token.leftbrace:
        if (!isPoint3f(i)) {
          plane = getPoint4f(i);
          break;
        }
        // fall through
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
        Vector3f norm = new Vector3f();
        float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, norm, vAB, vAC);
        plane = new Point4f(norm.x, norm.y, norm.z, w);
        if (!isSyntaxCheck && Logger.debugging)
          Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: "
              + plane);
        break;
      }
    if (plane == null)
      planeExpected();
    if (isNegated) {
      plane.scale(-1);
    }
    return plane;
  }

  private Point4f hklParameter(int i) throws ScriptException {
    if (!isSyntaxCheck && viewer.getCurrentUnitCell() == null)
      error(ERROR_noUnitCell);
    Point3f pt = (Point3f) getPointOrPlane(i, false, true, false, true, 3, 3);
    Point4f p = getHklPlane(pt);
    if (p == null)
      error(ERROR_badMillerIndices);
    if (!isSyntaxCheck && Logger.debugging)
      Logger.info("defined plane: " + p);
    return p;
  }

  protected Point4f getHklPlane(Point3f pt) {
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    Point3f pt1 = new Point3f(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
    Point3f pt2 = new Point3f(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
    Point3f pt3 = new Point3f(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
    // trick for 001 010 100 is to define the other points on other edges
    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      return null;
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
    Vector3f plane = new Vector3f();
    float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
    return new Point4f(plane.x, plane.y, plane.z, w);
  }

  private int getMadParameter() throws ScriptException {
    // wireframe, ssbond, hbond, struts
    int mad = 1;
    switch (getToken(1).tok) {
    case Token.only:
      restrictSelected(false, false);
      break;
    case Token.on:
      break;
    case Token.off:
      mad = 0;
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1, 0, 750);
      mad = radiusRasMol * 4 * 2;
      break;
    case Token.decimal:
      mad = (int) (floatParameter(1, -3, 3) * 1000 * 2);
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    return mad;
  }

  private int getSetAxesTypeMad(int index) throws ScriptException {
    if (index == statementLength)
      return 1;
    switch (getToken(checkLast(index)).tok) {
    case Token.on:
      return 1;
    case Token.off:
      return 0;
    case Token.dotted:
      return -1;
    case Token.integer:
      return intParameter(index, -1, 19);
    case Token.decimal:
      float angstroms = floatParameter(index, 0, 2);
      return (int) (angstroms * 1000 * 2);
    }
    error(ERROR_booleanOrWhateverExpected, "\"DOTTED\"");
    return 0;
  }

  private boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == Token.leftsquare || tok == Token.point3f || isPoint3f(i) || (tok == Token.string || Token
        .tokAttr(tok, Token.identifier))
        && Graphics3D.getArgbFromString((String) statement[i].value) != 0);
  }

  private int getArgbParam(int index) throws ScriptException {
    return getArgbParam(index, false);
  }

  private int getArgbParamLast(int index, boolean allowNone)
      throws ScriptException {
    int icolor = getArgbParam(index, allowNone);
    checkLast(iToken);
    return icolor;
  }

  private int getArgbParam(int index, boolean allowNone) throws ScriptException {
    Point3f pt = null;
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      default:
        if (theTok != Token.string && !Token.tokAttr(theTok, Token.identifier))
          break;
        return Graphics3D.getArgbFromString(parameterAsString(index));
      case Token.leftsquare:
        return getColorTriad(++index);
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
      error(ERROR_colorExpected);
    return Graphics3D.colorPtToInt(pt);
  }

  private int getColorTriad(int i) throws ScriptException {
    float[] colors = new float[3];
    int n = 0;
    String hex = "";
    getToken(i);
    Point3f pt = null;
    float val = 0;
    out: switch (theTok) {
    case Token.integer:
    case Token.spec_seqcode:
    case Token.decimal:
      for (; i < statementLength; i++) {
        switch (getToken(i).tok) {
        case Token.comma:
          continue;
        case Token.identifier:
          if (n != 1 || colors[0] != 0)
            error(ERROR_badRGBColor);
          hex = "0" + parameterAsString(i);
          break out;
        case Token.decimal:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = floatParameter(i);
          break;
        case Token.integer:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = theToken.intValue;
          break;
        case Token.spec_seqcode:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = ((Integer) theToken.value).intValue() % 256;
          break;
        case Token.rightsquare:
          if (n != 3)
            error(ERROR_badRGBColor);
          --i;
          pt = new Point3f(colors[0], colors[1], colors[2]);
          break out;
        default:
          error(ERROR_badRGBColor);
        }
        colors[n++] = val;
      }
      error(ERROR_badRGBColor);
      break;
    case Token.point3f:
      pt = (Point3f) theToken.value;
      break;
    case Token.identifier:
      hex = parameterAsString(i);
      break;
    default:
      error(ERROR_badRGBColor);
    }
    if (getToken(++i).tok != Token.rightsquare)
      error(ERROR_badRGBColor);
    if (pt != null)
      return Graphics3D.colorPtToInt(pt);
    if ((n = Graphics3D.getArgbFromString("[" + hex + "]")) == 0)
      error(ERROR_badRGBColor);
    return n;
  }

  private boolean coordinatesAreFractional;

  private boolean isPoint3f(int i) {
    // first check for simple possibilities:
    boolean isOK;
    if ((isOK = (tokAt(i) == Token.point3f)) || tokAt(i) == Token.point4f
        || isFloatParameter(i + 1) && isFloatParameter(i + 2)
        && isFloatParameter(i + 3) && isFloatParameter(i + 4))
      return isOK;
    ignoreError = true;
    int t = iToken;
    isOK = true;
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
      error(ERROR_invalidArgument);
    }
    if (tokAt(index) == Token.point4f) {
      if (minDim <= 4 && maxDim >= 4)
        return (Point4f) getToken(index).value;
      error(ERROR_invalidArgument);
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
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue;
        multiplier = -1;
        break;
      case Token.integer:
      case Token.spec_seqcode:
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue * multiplier;
        multiplier = 1;
        break;
      case Token.divide:
        getToken(++i);
      case Token.spec_model: // after a slash
        n--;
        if (n < 0 || integerOnly)
          error(ERROR_invalidArgument);
        if (theToken.value instanceof Integer || theTok == Token.integer) {
          coord[n++] /= (theToken.intValue == Integer.MAX_VALUE ? ((Integer) theToken.value)
              .intValue()
              : theToken.intValue);
        } else if (theToken.value instanceof Float) {
          coord[n++] /= ((Float) theToken.value).floatValue();
        }
        coordinatesAreFractional = true;
        break;
      case Token.decimal:
      case Token.spec_model2:
        if (integerOnly)
          error(ERROR_invalidArgument);
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = ((Float) theToken.value).floatValue();
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (n < minDim || n > maxDim)
      error(ERROR_invalidArgument);
    if (n == 3) {
      Point3f pt = new Point3f(coord[0], coord[1], coord[2]);
      if (coordinatesAreFractional && doConvert && !isSyntaxCheck)
        viewer.toCartesian(pt);
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how
        // to convert?)
        error(ERROR_invalidArgument);
      Point4f plane = new Point4f(coord[0], coord[1], coord[2], coord[3]);
      return plane;
    }
    return coord;
  }

  private Point3f xypParameter(int index) throws ScriptException {
    // [x y] or [x,y] refers to an xy point on the screen
    // just a Point3f with z = Float.MAX_VALUE
    // [x y %] or [x,y %] refers to an xy point on the screen
    // as a percent
    // just a Point3f with z = -Float.MAX_VALUE

    if (tokAt(index) != Token.leftsquare || !isFloatParameter(++index))
      return null;
    Point3f pt = new Point3f();
    pt.x = floatParameter(index);
    if (tokAt(++index) == Token.comma)
      index++;
    if (!isFloatParameter(index))
      return null;
    pt.y = floatParameter(index);
    boolean isPercent = (tokAt(++index) == Token.percent);
    if (isPercent)
      ++index;
    if (tokAt(index) != Token.rightsquare)
      return null;
    iToken = index;
    pt.z = (isPercent ? -1 : 1) * Float.MAX_VALUE;
    return pt;
  }

  private int intSetting(int pt, int val, int min, int max)
      throws ScriptException {
    if (val == Integer.MAX_VALUE)
      val = intSetting(pt);
    if (val != Integer.MIN_VALUE && val < min || val > max)
      integerOutOfRange(min, max);
    return val;
  }

  private int intSetting(int pt) throws ScriptException {
    if (pt == statementLength)
      return Integer.MIN_VALUE;
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.iValue((ScriptVariable) v.elementAt(0));
  }

  private float floatSetting(int pt, float min, float max)
      throws ScriptException {
    if (pt == statementLength)
      return Float.NaN;
    float val = floatSetting(pt);
    if (val < min || val > max)
      numberOutOfRange(min, max);
    return val;
  }

  private float floatSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.fValue((ScriptVariable) v.elementAt(0));
  }

  private String stringSetting(int pt, boolean isJmolSet)
      throws ScriptException {
    if (isJmolSet && statementLength == pt + 1)
      return parameterAsString(pt);
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.sValue((ScriptVariable) v.elementAt(0));
  }

  private ScriptVariable tokenSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return (ScriptVariable) v.elementAt(0);
  }

  /*
   * ****************************************************************
   * =============== command dispatch ===============================
   */

  /**
   * provides support for the script editor
   * 
   * @param i
   * @return true if displayable
   */
  private boolean isCommandDisplayable(int i) {
    if (i >= aatoken.length || i >= pcEnd || aatoken[i] == null)
      return false;
    return (lineIndices[i][1] > lineIndices[i][0]);
  }

  /**
   * checks to see if there is a pause condition, during which commands can
   * still be issued, but with the ! first.
   * 
   * @return false if there was a problem
   */
  private boolean checkContinue() {
    if (interruptExecution)
      return false;

    if (executionStepping && isCommandDisplayable(pc)) {
      viewer.scriptStatus("Next: " + getNextStatement(),
          "stepping -- type RESUME to continue", 0, null);
      executionPaused = true;
    } else if (!executionPaused) {
      return true;
    }

    if (true || Logger.debugging) {
      Logger.info("script execution paused at command " + (pc + 1) + " level "
          + scriptLevel + ": " + thisCommand);
    }

    try {
      while (executionPaused) {
        viewer.popHoldRepaint("pause");
        Thread.sleep(100);
        refresh();
        String script = viewer.getInterruptScript();
        if (script != "") {
          resumePausedExecution();
          setErrorMessage(null);
          pc--; // in case there is an error, we point to the PAUSE command
          try {
            runScript(script);
          } catch (Exception e) {
            setErrorMessage("" + e);
          } catch (Error er) {
            setErrorMessage("" + er);
          }
          if (error) {
            popContext(false);
            scriptStatusOrBuffer(errorMessage);
            setErrorMessage(null);
          }
          pc++;
          pauseExecution();
        }
        viewer.pushHoldRepaint("pause");
      }
      if (!isSyntaxCheck && !interruptExecution && !executionStepping) {
        viewer.scriptStatus("script execution "
            + (error || interruptExecution ? "interrupted" : "resumed"));
      }
    } catch (Exception e) {
      viewer.pushHoldRepaint("pause");
    }
    Logger.debug("script execution resumed");
    // once more to trap quit during pause
    return !error && !interruptExecution;
  }

  /**
   * here we go -- everything else in this class is called by this method or one
   * of its subsidiary methods.
   * 
   * 
   * @param doList
   * @throws ScriptException
   */
  private void instructionDispatchLoop(boolean doList) throws ScriptException {
    long timeBegin = 0;
    vProcess = null;
    boolean isForCheck = false; // indicates the stage of the for command loop
    if (shapeManager == null)
      shapeManager = viewer.getShapeManager();
    debugScript = logMessages = false;
    if (!isSyntaxCheck)
      setDebugging();
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      viewer.scriptStatus(script);
    }
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    String lastCommand = "";
    if (aatoken == null)
      return;
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (!isSyntaxCheck && !checkContinue())
        break;
      if (lineNumbers[pc] > lineEnd)
        break;
      theToken = (aatoken[pc].length == 0 ? null : aatoken[pc][0]);
      // when checking scripts, we can't check statments
      // containing @{...}
      if (!historyDisabled && !isSyntaxCheck
          && scriptLevel <= commandHistoryLevelMax && !tQuiet) {
        String cmdLine = getCommand(pc, true, true);
        if (theToken != null
            && cmdLine.length() > 0
            && !cmdLine.equals(lastCommand)
            && (theToken.tok == Token.function || theToken.tok == Token.parallel 
                || !Token.tokAttr(theToken.tok,
                Token.flowCommand)))
          viewer.addCommand(lastCommand = cmdLine);
      }
      if (!setStatement(pc)) {
        Logger.info(getCommand(pc, true, false)
            + " -- STATEMENT CONTAINING @{} SKIPPED");
        continue;
      }
      thisCommand = getCommand(pc, false, true);
      fullCommand = thisCommand + getNextComment();
      getToken(0);
      iToken = 0;
      String script = viewer.getInterruptScript();
      if (script != "")
        runScript(script);
      if (doList || !isSyntaxCheck) {
        int milliSecDelay = viewer.getScriptDelay();
        if (doList || milliSecDelay > 0 && scriptLevel > 0) {
          if (milliSecDelay > 0)
            delay(-(long) milliSecDelay);
          viewer.scriptEcho("$[" + scriptLevel + "." + lineNumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (vProcess != null && (theTok != Token.end || statementLength < 2 || statement[1].tok != Token.process)) {
        vProcess.add(statement);
        continue;
      }
      if (isSyntaxCheck) {
        if (isCmdLine_c_or_C_Option)
          Logger.info(thisCommand);
        if (statementLength == 1 && statement[0].tok != Token.function 
            && statement[0].tok != Token.parallel)
          continue;
      } else {
        if (debugScript)
          logDebugScript(0);
        if (scriptLevel == 0 && viewer.logCommands())
          viewer.log(thisCommand);
        if (logMessages && theToken != null)
          Logger.debug(theToken.toString());
      }
      if (theToken == null)
        continue;
      if (Token.tokAttr(theToken.tok, Token.shapeCommand))
        processShapeCommand(theToken.tok);
      else
        switch (theToken.tok) {
        case Token.nada:
          break;
        case Token.push:
          pushContext((ContextToken) theToken);
          break;
        case Token.pop:
          popContext(true);
          break;
        case Token.breakcmd:
        case Token.continuecmd:
        case Token.elsecmd:
        case Token.elseif:
        case Token.end:
        case Token.endifcmd:
        case Token.forcmd:
        case Token.gotocmd:
        case Token.ifcmd:
        case Token.loop:
        case Token.process:
        case Token.whilecmd:
          isForCheck = flowControl(theToken.tok, isForCheck);
          break;
        case Token.animation:
          animation();
          break;
        case Token.background:
          background(1);
          break;
        case Token.bind:
          bind();
          break;
        case Token.bondorder:
          bondorder();
          break;
        case Token.calculate:
          calculate();
          break;
        case Token.cd:
          cd();
          break;
        case Token.center:
          center(1);
          break;
        case Token.centerAt:
          centerAt();
          break;
        case Token.color:
          color();
          break;
        case Token.compare:
          compare();
          break;
        case Token.configuration:
          configuration();
          break;
        case Token.connect:
          connect(1);
          break;
        case Token.console:
          console();
          break;
        case Token.data:
          data();
          break;
        case Token.define:
          define();
          break;
        case Token.delay:
          delay();
          break;
        case Token.delete:
          delete();
          break;
        case Token.depth:
          slab(true);
          break;
        case Token.display:
          display(true);
          break;
        case Token.exit: // flush the queue and...
        case Token.quit: // quit this only if it isn't the first command
          if (isSyntaxCheck)
            break;
          if (pc > 0 && theToken.tok == Token.exit)
            viewer.clearScriptQueue();
          interruptExecution = (pc > 0 || !viewer.usingScriptQueue());
          break;
        case Token.exitjmol:
          if (isSyntaxCheck || viewer.isApplet())
            return;
          viewer.exitJmol();
          break;
        case Token.file:
          file();
          break;
        case Token.font:
          font(-1, 0);
          break;
        case Token.frame:
        case Token.model:
          frame(1);
          break;
        case Token.parallel: // not actually found 
        case Token.function:
          function(); // when a function is a command
          break;
        case Token.getproperty:
          getProperty();
          break;
        case Token.help:
          help();
          break;
        case Token.hide:
          display(false);
          break;
        case Token.hbond:
          hbond(true);
          break;
        case Token.history:
          history(1);
          break;
        case Token.hover:
          hover();
          break;
        case Token.initialize:
          viewer.initialize();
          break;
        case Token.invertSelected:
          invertSelected();
          break;
        case Token.javascript:
          script(Token.javascript, null);
          break;
        case Token.load:
          load();
          break;
        case Token.log:
          log();
          break;
        case Token.message:
          message();
          break;
        case Token.minimize:
          minimize();
          break;
        case Token.move:
          move();
          break;
        case Token.moveto:
          moveto();
          break;
        case Token.navigate:
          navigate();
          break;
        case Token.pause: // resume is done differently
          pause();
          break;
        case Token.print:
          print();
          break;
        case Token.quaternion:
          dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
          break;
        case Token.ramachandran:
          dataFrame(JmolConstants.JMOL_DATA_RAMACHANDRAN);
          break;
        case Token.refresh:
          refresh();
          break;
        case Token.reset:
          reset();
          break;
        case Token.restore:
          restore();
          break;
        case Token.restrict:
          restrict();
          break;
        case Token.resume:
          if (!isSyntaxCheck)
            resumePausedExecution();
          break;
        case Token.returncmd:
          returnCmd();
          break;
        case Token.rotate:
          rotate(false, false);
          break;
        case Token.rotateSelected:
          rotate(false, true);
          break;
        case Token.save:
          save();
          break;
        case Token.set:
          set();
          break;
        case Token.script:
          script(Token.script, null);
          break;
        case Token.select:
          select(1);
          break;
        case Token.selectionhalos:
          selectionHalo(1);
          break;
        case Token.show:
          show();
          break;
        case Token.slab:
          slab(false);
          break;
        case Token.spin:
          rotate(true, false);
          break;
        case Token.ssbond:
          ssbond();
          break;
        case Token.struts:
          struts();
          break;
        case Token.step:
          if (pause())
            stepPausedExecution();
          break;
        case Token.stereo:
          stereo();
          break;
        case Token.structure:
          structure();
          break;
        case Token.subset:
          subset();
          break;
        case Token.sync:
          sync();
          break;
        case Token.timeout:
          timeout(1);
          break;
        case Token.translate:
          translate(false);
          break;
        case Token.translateSelected:
          translate(true);
          break;
        case Token.unbind:
          unbind();
          break;
        case Token.vibration:
          vibration();
          break;
        case Token.write:
          write(null);
          break;
        case Token.zap:
          zap(true);
          break;
        case Token.zoom:
          zoom(false);
          break;
        case Token.zoomTo:
          zoom(true);
          break;
        default:
          error(ERROR_unrecognizedCommand);
        }
      if (!isSyntaxCheck && !tQuiet)
        viewer.setCursor(Viewer.CURSOR_DEFAULT);
      // at end because we could use continue to avoid it
      if (executionStepping) {
        executionPaused = (isCommandDisplayable(pc + 1));
      }
    }
  }

  private void processShapeCommand(int tok) throws ScriptException {
    int iShape;
    switch (tok) {
    case Token.axes:
      iShape = JmolConstants.SHAPE_AXES;
      break;
    case Token.backbone:
      iShape = JmolConstants.SHAPE_BACKBONE;
      break;
    case Token.boundbox:
      iShape = JmolConstants.SHAPE_BBCAGE;
      break;
    case Token.cartoon:
      iShape = JmolConstants.SHAPE_CARTOON;
      break;
    case Token.dipole:
      iShape = JmolConstants.SHAPE_DIPOLES;
      break;
    case Token.dots:
      iShape = JmolConstants.SHAPE_DOTS;
      break;
    case Token.draw:
      iShape = JmolConstants.SHAPE_DRAW;
      break;
    case Token.echo:
      iShape = JmolConstants.SHAPE_ECHO;
      break;
    case Token.ellipsoid:
      iShape = JmolConstants.SHAPE_ELLIPSOIDS;
      break;
    case Token.frank:
      iShape = JmolConstants.SHAPE_FRANK;
      break;
    case Token.geosurface:
      iShape = JmolConstants.SHAPE_GEOSURFACE;
      break;
    case Token.halo:
      iShape = JmolConstants.SHAPE_HALOS;
      break;
    case Token.isosurface:
      iShape = JmolConstants.SHAPE_ISOSURFACE;
      break;
    case Token.label:
      iShape = JmolConstants.SHAPE_LABELS;
      break;
    case Token.lcaocartoon:
      iShape = JmolConstants.SHAPE_LCAOCARTOON;
      break;
    case Token.measurements:
    case Token.measure:
      iShape = JmolConstants.SHAPE_MEASURES;
      break;
    case Token.meshRibbon:
      iShape = JmolConstants.SHAPE_MESHRIBBON;
      break;
    case Token.mo:
      iShape = JmolConstants.SHAPE_MO;
      break;
    case Token.plot3d:
      iShape = JmolConstants.SHAPE_PLOT3D;
      break;
    case Token.pmesh:
      iShape = JmolConstants.SHAPE_PMESH;
      break;
    case Token.polyhedra:
      iShape = JmolConstants.SHAPE_POLYHEDRA;
      break;
    case Token.ribbon:
      iShape = JmolConstants.SHAPE_RIBBONS;
      break;
    case Token.rocket:
      iShape = JmolConstants.SHAPE_ROCKETS;
      break;
    case Token.spacefill: // aka cpk
      iShape = JmolConstants.SHAPE_BALLS;
      break;
    case Token.star:
      iShape = JmolConstants.SHAPE_STARS;
      break;
    case Token.strands:
      iShape = JmolConstants.SHAPE_STRANDS;
      break;
    case Token.trace:
      iShape = JmolConstants.SHAPE_TRACE;
      break;
    case Token.unitcell:
      iShape = JmolConstants.SHAPE_UCCAGE;
      break;
    case Token.vector:
      iShape = JmolConstants.SHAPE_VECTORS;
      break;
    case Token.wireframe:
      iShape = JmolConstants.SHAPE_STICKS;
      break;
    default:
      iShape = -1;
    }
    if (iShape < 0)
      error(ERROR_unrecognizedCommand);

    // if (Logger.debugging)
    // setShapeProperty(iShape, "initXml", null);

    switch (tok) {
    case Token.axes:
      axes(1);
      break;
    case Token.backbone:
    case Token.cartoon:
    case Token.meshRibbon:
    case Token.ribbon:
    case Token.rocket:
    case Token.strands:
    case Token.trace:
      proteinShape(iShape);
      break;
    case Token.boundbox:
      boundbox(1);
      break;
    case Token.dipole:
      dipole();
      break;
    case Token.dots:
    case Token.geosurface:
      dots(iShape);
      break;
    case Token.draw:
      draw();
      break;
    case Token.echo:
      echo(1, false);
      break;
    case Token.ellipsoid:
      ellipsoid();
      break;
    case Token.frank:
      frank(1);
      break;
    case Token.halo:
    case Token.spacefill: // aka cpk
    case Token.star:
      setAtomShapeSize(iShape, (tok == Token.halo ? 0.2f : 1f));
      break;
    case Token.isosurface:
    case Token.plot3d:
    case Token.pmesh:
      isosurface(iShape);
      break;
    case Token.label:
      label(1);
      break;
    case Token.lcaocartoon:
      lcaoCartoon();
      break;
    case Token.measurements:
    case Token.measure:
      measure();
      break;
    case Token.mo:
      mo(false);
      break;
    case Token.polyhedra:
      polyhedra();
      break;
    case Token.unitcell:
      unitcell(1);
      break;
    case Token.vector:
      vector();
      break;
    case Token.wireframe:
      wireframe();
      break;
    }
    // if (Logger.debugging)
    // setShapeProperty(iShape, "setXml", null);
  }

  private boolean flowControl(int tok, boolean isForCheck)
      throws ScriptException {
    switch (tok) {
    case Token.gotocmd:
      String strTo = parameterAsString(checkLast(1));
      int pcTo = -1;
      for (int i = 0; i < aatoken.length; i++) {
        Token[] tokens = aatoken[i];
        if (tokens[0].tok == Token.message || tokens[0].tok == Token.nada)
          if (tokens[tokens.length - 1].value.toString()
              .equalsIgnoreCase(strTo)) {
            pcTo = i;
            break;
          }
      }
      if (pcTo < 0)
        error(ERROR_invalidArgument);
      int di = (pcTo < pc ? 1 : -1);
      int nPush = 0;
      for (int i = pcTo; i != pc; i += di) {
        switch (aatoken[i][0].tok) {
        case Token.push:
        case Token.process:
        case Token.forcmd:
        case Token.whilecmd:
          nPush++;
          break;
        case Token.pop:
        case Token.end:
          nPush--;
          break;
        }
      }
      if (nPush != 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        pc = pcTo - 1; // ... resetting the program counter
      return isForCheck;
    case Token.loop:
      // back to the beginning of this script
      delay();
      if (!isSyntaxCheck)
        pc = -1;
      return isForCheck;
    }
    int pt = statement[0].intValue;
    boolean isDone = (pt < 0 && !isSyntaxCheck);
    boolean isOK = true;
    int ptNext = 0;
    switch (tok) {
    case Token.process:
      pushContext((ContextToken) theToken);
      isDone = isOK = true;
      addProcess(pc, pt, true);
      break;
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
      checkLength(1);
      if (pt < 0 && !isSyntaxCheck)
        pc = -pt - 1;
      break;
    case Token.endifcmd:
      checkLength(1);
      break;
    case Token.end: // function, if, for, while
      if (getToken(checkLast(1)).tok == Token.function || theTok == Token.parallel) {
        viewer.addFunction((ScriptFunction) theToken.value);
        return isForCheck;
      }
      if (theTok == Token.process) {
        addProcess(pt, pc, false);
        popContext(true);
      }
      isOK = (theTok == Token.process || theTok == Token.ifcmd);
      isForCheck = (theTok == Token.forcmd || theTok == Token.whilecmd);
      break;
    case Token.whilecmd:
      if (!isForCheck)
        pushContext((ContextToken) theToken);
      isForCheck = false;
      if (!ifCmd() && !isSyntaxCheck) {
        pc = pt;
        popContext(true);
      }
      break;
    case Token.breakcmd:
      if (!isSyntaxCheck) {
        pc = aatoken[pt][0].intValue;
        popContext(true);
      }
      if (statementLength == 1)
        break;
      int n = intParameter(checkLast(1));
      if (isSyntaxCheck)
        break;
      for (int i = 0; i < n; i++)
        popContext(true);
      break;
    case Token.continuecmd:
      isForCheck = true;
      if (!isSyntaxCheck)
        pc = pt - 1;
      if (statementLength > 1)
        intParameter(checkLast(1));
      break;
    case Token.forcmd:
      // for (i = 1; i < 3; i = i + 1);
      // for (var i = 1; i < 3; i = i + 1);
      // for (;;;);
      // for (var x in {...}) { xxxxx }
      int[] pts = new int[2];
      int j = 0;
      BitSet bsIn = null;
      for (int i = 1, nSkip = 0; i < statementLength && j < 2; i++) {
        switch (tokAt(i)) {
        case Token.semicolon:
          if (nSkip > 0)
            nSkip--;
          else
            pts[j++] = i;
          break;
        case Token.in:
          nSkip -= 2;
          bsIn = expression(++i);
          i = iToken;
          break;
        case Token.select:
          nSkip += 2;
          break;
        }

      }
      if (isForCheck) {
        j = pts[1] + 1;
        isForCheck = false;
      } else {
        pushContext((ContextToken) theToken);
        j = 2;
        if (tokAt(j) == Token.var)
          j++;
      }
      String key = parameterAsString(j);
      boolean isMinusMinus = key.equals("--") || key.equals("++");
      if (isMinusMinus) {
        key = parameterAsString(++j);
      }
      if (Token.tokAttr(tokAt(j), Token.misc)
          || getContextVariableAsVariable(key) != null) {
        if (bsIn == null && !isMinusMinus && getToken(++j).tok != Token.opEQ)
          error(ERROR_invalidArgument);
        if (bsIn == null) {
          if (isMinusMinus)
            j -= 2;
          setVariable(++j, statementLength - 1, key, 0);
        } else {
          setVariable(j + 2, statementLength - 1, key + "_set", 0);
          setVariable(j + 2, statementLength - 1, key, 0);
        }
      }
      isOK = ((Boolean) parameterExpression(pts[0] + 1, pts[1], null, false))
          .booleanValue();
      pt++;
      if (!isOK)
        popContext(true);
      break;
    }
    if (!isOK && !isSyntaxCheck)
      pc = Math.abs(pt) - 1;
    return isForCheck;
  }

  private Vector vProcess;
  static int iProcess;
  private void addProcess(int pc, int pt, boolean isStart) {
    if (parallelProcessor == null)
      return;
    if (isStart) {
      vProcess = new Vector();
    } else {
      
      Token[][] statements = new Token[pt][];
      for (int i = 0; i < vProcess.size(); i++)
        statements[i + 1 - pc] = (Token[]) vProcess.get(i);
      ScriptContext context = getScriptContext();
      context.aatoken = statements;
      context.pc = 1 - pc;
      context.pcEnd = pt;
      parallelProcessor.addProcess("p" + (++iProcess), context);
      vProcess = null;
    }
  }

  private boolean ifCmd() throws ScriptException {
    return ((Boolean) parameterExpression(1, 0, null, false)).booleanValue();
  }

  private void returnCmd() throws ScriptException {
    ScriptVariable t = getContextVariableAsVariable("_retval");
    if (t == null) {
      if (!isSyntaxCheck)
        interruptExecution = true;
      return;
    }
    Vector v = (statementLength == 1 ? null : (Vector) parameterExpression(1,
        0, null, true));
    if (isSyntaxCheck)
      return;
    ScriptVariable tv = (v == null || v.size() == 0 ? ScriptVariable
        .intVariable(0) : (ScriptVariable) v.get(0));
    t.value = tv.value;
    t.intValue = tv.intValue;
    t.tok = tv.tok;
    pcEnd = pc;
  }

  private void help() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String what = optParameterAsString(1);
    int pt = 0;
    if (what.toLowerCase().startsWith("mouse") && (pt = what.indexOf(" ")) >= 0
        && pt == what.lastIndexOf(" ")) {
      showString(viewer.getBindingInfo(what.substring(pt + 1)));
      return;
    }
    if (Token.tokAttr(Token.getTokFromName(what), Token.scriptCommand))
      what = "?command=" + what;
    viewer.getHelp(what);
  }

  private void move() throws ScriptException {
    if (statementLength > 11)
      error(ERROR_badArgumentCount);
    // rotx roty rotz zoom transx transy transz slab seconds fps
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
    // moveto time
    // moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} 0 xTrans yTrans (rotCenter) [zoom factor]
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} (rotCenter) [zoom factor] (navCenter) xNav
    // yNav navDepth
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]
    // moveto [time] front|back|left|right|top|bottom
    if (statementLength == 2 && tokAt(1) == Token.stop) {
      if (!isSyntaxCheck)
        viewer.stopMotion();
      return;
    }
    if (statementLength == 2 && isFloatParameter(1)) {
      float f = floatParameter(1);
      if (isSyntaxCheck)
        return;
      if (f > 0)
        refresh();
      viewer.moveTo(f, null, JmolConstants.axisZ, 0, null, 100, 0, 0, 0, null,
          Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    Vector3f axis = new Vector3f(Float.NaN, 0, 0);
    Point3f center = null;
    int i = 1;
    float floatSecondsTotal = (isFloatParameter(i) ? floatParameter(i++) : 2.0f);
    float degrees = 90;
    BitSet bsCenter = null;
    switch (getToken(i).tok) {
    case Token.quaternion:
      Quaternion q;
      boolean isMolecular = false;
      if (tokAt(++i) == Token.molecular) {
        // see comment below
        isMolecular = true;
        i++;
      }
      if (tokAt(i) == Token.bitset || tokAt(i) == Token.expressionBegin) {
        isMolecular = true;
        center = centerParameter(i);
        if (!(expressionResult instanceof BitSet))
          error(ERROR_invalidArgument);
        bsCenter = (BitSet) expressionResult;
        q = (isSyntaxCheck ? new Quaternion() : viewer
            .getAtomQuaternion(bsCenter.nextSetBit(0)));
      } else if (tokAt(i) == Token.list){
        String[] s = (String[])getToken(i).value;
        if (s.length == 0)
          error(ERROR_invalidArgument);
        Object o = Escape.unescapePoint(s[0]);
        if (!(o instanceof Point4f))
          error(ERROR_invalidArgument);
        q = new Quaternion((Point4f)o);
      } else {
        q = new Quaternion(getPoint4f(i));
      }
      i = iToken + 1;
      if (q == null)
        error(ERROR_invalidArgument);
      AxisAngle4f aa = q.toAxisAngle4f();
      axis.set(aa.x, aa.y, aa.z);
      /*
       * The quaternion angle for an atom represents the angle by which the
       * reference frame must be rotated to match the frame defined for the
       * residue.
       * 
       * However, to "moveTo" this frame as the REFERENCE frame, what we have to
       * do is take that quaternion frame and rotate it BACKWARD by that many
       * degrees. Then it will match the reference frame, which is ultimately
       * our window frame.
       * 
       * We only apply this for molecular-type quaternions, because in general
       * the orientation quaternion refers to how the reference plane has been
       * changed (the orientation matrix)
       */
      degrees = (isMolecular ? -1 : 1) * (float) (aa.angle * 180.0 / Math.PI);
      break;
    case Token.point4f:
    case Token.point3f:
    case Token.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isPoint3f(i)) {
        axis.set(getPoint3f(i, true));
        i = iToken + 1;
        degrees = floatParameter(i++);
      } else {
        Point4f pt4 = getPoint4f(i);
        i = iToken + 1;
        axis.set(pt4.x, pt4.y, pt4.z);
        degrees = (pt4.x == 0 && pt4.y == 0 && pt4.z == 0 ? Float.NaN : pt4.w);
      }
      break;
    case Token.front:
      axis.set(1, 0, 0);
      degrees = 0f;
      checkLength(++i);
      break;
    case Token.back:
      axis.set(0, 1, 0);
      degrees = 180f;
      checkLength(++i);
      break;
    case Token.left:
      axis.set(0, 1, 0);
      checkLength(++i);
      break;
    case Token.right:
      axis.set(0, -1, 0);
      checkLength(++i);
      break;
    case Token.top:
      axis.set(1, 0, 0);
      checkLength(++i);
      break;
    case Token.bottom:
      axis.set(-1, 0, 0);
      checkLength(++i);
      break;
    default:
      // X Y Z deg
      axis = new Vector3f(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }
    if (Float.isNaN(axis.x) || Float.isNaN(axis.y) || Float.isNaN(axis.z))
      axis.set(0, 0, 0);
    else if (axis.length() == 0 && degrees == 0)
      degrees = Float.NaN;
    boolean isChange = !viewer.isInPosition(axis, degrees);
    // optional zoom
    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    // optional xTrans yTrans
    float xTrans = 0;
    float yTrans = 0;
    if (isFloatParameter(i) && !isCenterParameter(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
      if (!isChange && Math.abs(xTrans - viewer.getTranslationXPercent()) >= 1)
        isChange = true;
      if (!isChange && Math.abs(yTrans - viewer.getTranslationYPercent()) >= 1)
        isChange = true;
    }
    if (bsCenter == null && i != statementLength) {
      // if any more, required (center)
      center = centerParameter(i);
      if (expressionResult instanceof BitSet)
        bsCenter = (BitSet) expressionResult;
      i = iToken + 1;
    }
    float rotationRadius = Float.NaN;
    float zoom0 = viewer.getZoomSetting();
    if (center != null) {
      if (!isChange && center.distance(viewer.getRotationCenter()) >= 0.1)
        isChange = true;
      // optional {center} rotationRadius
      if (isFloatParameter(i))
        rotationRadius = floatParameter(i++);
      if (!isCenterParameter(i)) {
        if ((rotationRadius == 0 || Float.isNaN(rotationRadius))
            && (zoom == 0 || Float.isNaN(zoom))) {
          // alternative (atom expression) 0 zoomFactor
          float newZoom = Math
              .abs(getZoom(i, bsCenter, (zoom == 0 ? 0 : zoom0)));
          i = iToken + 1;
          zoom = newZoom;
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
      error(ERROR_badArgumentCount);

    if (isSyntaxCheck)
      return;
    if (!isChange)
      floatSecondsTotal = 0;
    if (floatSecondsTotal > 0)
      refresh();
    viewer.moveTo(floatSecondsTotal, center, axis, degrees, null, zoom, xTrans,
        yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  private void navigate() throws ScriptException {
    /*
     * navigation on/off navigation depth p # would be as a depth value, like
     * slab, in percent, but could be negative navigation nSec translate X Y #
     * could be percentages navigation nSec translate $object # could be a draw
     * object navigation nSec translate (atom selection) #average of values
     * navigation nSec center {x y z} navigation nSec center $object navigation
     * nSec center (atom selection) navigation nSec path $object navigation nSec
     * path {x y z theta} {x y z theta}{x y z theta}{x y z theta}... navigation
     * nSec trace (atom selection)
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
        if (isSyntaxCheck)
          return;
        setObjectMad(JmolConstants.SHAPE_AXES, "axes", 1);
        setShapeProperty(JmolConstants.SHAPE_AXES, "position", new Point3f(50,
            50, Float.MAX_VALUE));
        setBooleanProperty("navigationMode", true);
        viewer.setNavOn(theTok == Token.on);
        return;
      case Token.stop:
        if (!isSyntaxCheck)
          viewer.setNavXYZ(0, 0, 0);
        return;
      case Token.point3f:
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (!viewer.getNavigationMode())
      setBooleanProperty("navigationMode", true);
    for (int i = 1; i < statementLength; i++) {
      float timeSec = (isFloatParameter(i) ? floatParameter(i++) : 2f);
      if (timeSec < 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck && timeSec > 0)
        refresh();
      switch (getToken(i).tok) {
      case Token.point3f:
      case Token.leftbrace:
        // navigate {x y z}
        pt = getPoint3f(i, true);
        iToken++;
        if (iToken != statementLength)
          error(ERROR_invalidArgument);
        if (isSyntaxCheck)
          return;
        viewer.setNavXYZ(pt.x, pt.y, pt.z);
        return;
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
        case Token.x:
          rotAxis.set(1, 0, 0);
          break;
        case Token.y:
          rotAxis.set(0, 1, 0);
          break;
        case Token.z:
          rotAxis.set(0, 0, 1);
          break;
        case Token.identifier:
          error(ERROR_invalidArgument); // for now
          break;
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
        } else {
          switch (tokAt(i)) {
          case Token.x:
            x = floatParameter(++i);
            break;
          case Token.y:
            y = floatParameter(++i);
            break;
          default:
            pt = centerParameter(i);
            i = iToken;
            if (!isSyntaxCheck)
              viewer.navTranslate(timeSec, pt);
            continue;
          }
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
      case Token.surface:
        if (i != 1)
          error(ERROR_invalidArgument);
        if (isSyntaxCheck)
          return;
        viewer.navigateSurface(timeSec, optParameterAsString(2));
        continue;
      case Token.path:
        Point3f[] path;
        float[] theta = null; // orientation; null for now
        if (getToken(i + 1).tok == Token.dollarsign) {
          i++;
          // navigate timeSeconds path $id indexStart indexEnd
          String pathID = objectNameParameter(++i);
          if (isSyntaxCheck)
            return;
          setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", pathID);
          path = (Point3f[]) getShapeProperty(JmolConstants.SHAPE_DRAW,
              "vertices");
          refresh();
          if (path == null)
            error(ERROR_invalidArgument);
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
        // fall through
      default:
        error(ERROR_invalidArgument);
      }
    }
  }

  private void bondorder() throws ScriptException {
    checkLength(-3);
    int order = 0;
    switch (getToken(1).tok) {
    case Token.integer:
    case Token.decimal:
      if ((order = JmolConstants.getBondOrderFromFloat(floatParameter(1))) == JmolConstants.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      break;
    default:
      if ((order = JmolConstants.getBondOrderFromString(parameterAsString(1))) == JmolConstants.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      // generic partial can be indicated by "partial n.m"
      if (order == JmolConstants.BOND_PARTIAL01 && tokAt(2) == Token.decimal) {
        order = JmolConstants
            .getPartialBondOrderFromInteger(statement[2].intValue);
      }
    }
    setShapeProperty(JmolConstants.SHAPE_STICKS, "bondOrder",
        new Integer(order));
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
      error(ERROR_invalidArgument);
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
      error(ERROR_invalidArgument);
    }
    Point3f pt = new Point3f(0, 0, 0);
    if (statementLength == 5) {
      // centerAt xxx x y z
      pt.x = floatParameter(2);
      pt.y = floatParameter(3);
      pt.z = floatParameter(4);
    } else if (isCenterParameter(2)) {
      pt = centerParameter(2);
      checkLast(iToken);
    } else {
      checkLength(2);
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

    float degrees = JmolConstants.DEFAULT_STEREO_DEGREES;
    boolean degreesSeen = false;
    int[] colors = null;
    int colorpt = 0;
    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (colorpt > 1)
          error(ERROR_badArgumentCount);
        if (colorpt == 0)
          colors = new int[2];
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt] = getArgbParam(i);
        if (colorpt++ == 0)
          colors[1] = ~colors[0];
        i = iToken;
        continue;
      }
      switch (getToken(i).tok) {
      case Token.on:
        checkLast(iToken = 1);
        iToken = 1;
        break;
      case Token.off:
        checkLast(iToken = 1);
        stereoMode = JmolConstants.STEREO_NONE;
        break;
      case Token.integer:
      case Token.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        break;
      case Token.identifier:
        if (!degreesSeen)
          degrees = 3;
        stereoMode = JmolConstants.getStereoMode(parameterAsString(i));
        if (stereoMode != JmolConstants.STEREO_UNKNOWN)
          break;
        // fall into
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (isSyntaxCheck)
      return;
    viewer.setStereoMode(colors, stereoMode, degrees);
  }

  private void compare() throws ScriptException {
    // compare {model1} {model2} [atoms] {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} orientations
    // compare {model1} {model2} orientations {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} [orientations] [quaternionList1]
    // [quaternionList2]
    boolean isQuaternion = false;
    boolean doRotate = false;
    boolean doTranslate = false;
    boolean doAnimate = false;
    float nSeconds = Float.NaN;
    Quaternion[] data1 = null, data2 = null;
    BitSet bsAtoms1 = null, bsAtoms2 = null;
    Vector vAtomSets = null;
    Vector vQuatSets = null;
    BitSet bsFrom = expression(1);
    BitSet bsTo = expression(++iToken);
    BitSet bsSubset = null;
    for (int i = iToken + 1; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      case Token.decimal:
      case Token.integer:
        nSeconds = Math.abs(floatParameter(i));
        if (nSeconds > 0)
          doAnimate = true;
        break;
      case Token.comma:
        break;
      case Token.subset:
        bsSubset = expression(++i);
        i = iToken;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (vQuatSets != null)
          error(ERROR_invalidArgument);
        bsAtoms1 = expression(iToken);
        int tok = tokAt(iToken + 1);
        bsAtoms2 = (tok == Token.bitset || tok == Token.expressionBegin ? expression(++iToken)
            : BitSetUtil.copy(bsAtoms1));
        bsAtoms1.and(bsFrom);
        bsAtoms2.and(bsTo);
        if (bsSubset != null) {
          bsAtoms1.and(bsSubset);
          bsAtoms2.and(bsSubset);
        }
        if (vAtomSets == null)
          vAtomSets = new Vector();
        vAtomSets.add(new BitSet[] { bsAtoms1, bsAtoms2 });
        i = iToken;
        break;
      case Token.list:
        if (vAtomSets != null)
          error(ERROR_invalidArgument);
        isQuaternion = true;
        data1 = ScriptMathProcessor
            .getQuaternionArray((Object[]) theToken.value);
        getToken(++i);
        data2 = ScriptMathProcessor
            .getQuaternionArray((Object[]) theToken.value);
        if (vQuatSets == null)
          vQuatSets = new Vector();
        vQuatSets.add(new Object[] { data1, data2 });
        break;
      case Token.orientation:
        isQuaternion = true;
        break;
      case Token.point:

      case Token.atoms:
        isQuaternion = false;
        break;
      case Token.rotate:
        doRotate = true;
        break;
      case Token.translate:
        doTranslate = true;
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (isSyntaxCheck)
      return;
    float[] retStddev = new float[2]; // [0] final, [1] initial for atoms
    Quaternion q = null;
    Vector vQ = new Vector();
    Point3f[][] centerAndPoints = null;
    if (isQuaternion) {
      if (vAtomSets == null && vQuatSets == null) {
        vAtomSets = new Vector();
        vAtomSets.add(new BitSet[] { bsFrom, bsTo });
      }
      if (vQuatSets == null) {
        for (int i = 0; i < vAtomSets.size(); i++) {
          BitSet[] bss = (BitSet[]) vAtomSets.get(i);
          data1 = viewer.getAtomGroupQuaternions(bss[0], Integer.MAX_VALUE);
          data2 = viewer.getAtomGroupQuaternions(bss[1], Integer.MAX_VALUE);
          for (int j = 0; j < data1.length && j < data2.length; j++)
            vQ.add(data2[j].div(data1[j]));
        }
      } else {
        for (int j = 0; j < data1.length && j < data2.length; j++)
          vQ.add(data2[j].div(data1[j]));
      }
      retStddev[0] = 0;
      data1 = new Quaternion[vQ.size()];
      for (int i = vQ.size(); --i >= 0;)
        data1[i] = (Quaternion) vQ.get(i);
      q = Quaternion.sphereMean(data1, retStddev, 0.0001f);
      showString("RMSD = " + retStddev[0] + " degrees");
    } else {
      // atoms
      if (bsAtoms1 == null) {
        bsAtoms1 = viewer.getAtomBitSet("spine");
        if (bsAtoms1.nextSetBit(0) < 0) {
          bsAtoms1 = bsFrom;
          bsAtoms2 = bsTo;
        } else {
          bsAtoms2 = BitSetUtil.copy(bsAtoms1);
          bsAtoms1.and(bsFrom);
          bsAtoms2.and(bsTo);
        }
        vAtomSets = new Vector();
        vAtomSets.add(new BitSet[] { bsAtoms1, bsAtoms2 });
      }
      centerAndPoints = viewer.getCenterAndPoints(vAtomSets, true);
      q = ModelSet.calculateQuaternionRotation(centerAndPoints, retStddev);
      float r0 = (Float.isNaN(retStddev[1]) ? Float.NaN
          : (int) (retStddev[0] * 100) / 100f);
      float r1 = (Float.isNaN(retStddev[1]) ? Float.NaN
          : (int) (retStddev[1] * 100) / 100f);
      showString("RMSD " + r0 + " --> " + r1 + " Angstroms");
    }
    Point3f pt1 = new Point3f();
    if (centerAndPoints == null)
      centerAndPoints = viewer.getCenterAndPoints(vAtomSets, true);
    if (Float.isNaN(nSeconds) || nSeconds < 0) {
      nSeconds = 1;
    } else if (!doRotate && !doTranslate) {
      doAnimate = doRotate = doTranslate = true;
    }
    doAnimate = (nSeconds != 0);
    float endDegrees = Float.NaN;
    Vector3f translation = null;
    if (doTranslate) {
      translation = new Vector3f(centerAndPoints[1][0]);
      translation.sub(centerAndPoints[0][0]);
      endDegrees = 0;
    }
    if (doRotate) {
      if (q == null)
        evalError("option not implemented", null);
      pt1.set(centerAndPoints[0][0]);
      pt1.add(q.getNormal());
      endDegrees = q.getTheta();
    }
    if (Float.isNaN(endDegrees) || Float.isNaN(pt1.x))
      return;
    Vector ptsB = null;
    if (doRotate && doTranslate && nSeconds != 0) {
      Vector ptsA = viewer.getAtomPointVector(bsFrom);
      Matrix4f m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
      ptsB = Measure.transformPoints(ptsA, m4, centerAndPoints[0][0]);
    }
    viewer.rotateAboutPointsInternal(centerAndPoints[0][0], pt1, endDegrees
        / nSeconds, endDegrees, doAnimate, bsFrom, translation, ptsB);
  }

  private void connect(int index) throws ScriptException {

    final float[] distances = new float[2];
    BitSet[] atomSets = new BitSet[2];
    atomSets[0] = atomSets[1] = viewer.getSelectionSet();
    float radius = Float.NaN;
    int color = Integer.MIN_VALUE;
    int distanceCount = 0;
    int bondOrder = JmolConstants.BOND_ORDER_NULL;
    int bo;
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
    int ptColor = 0;
    float energy = 0;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1
     * operation]
     */

    if (statementLength == 1) {
      viewer.rebond();
      return;
    }

    for (int i = index; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      case Token.on:
      case Token.off:
        checkLength(2);
        if (!isSyntaxCheck)
          viewer.rebond();
        return;
      case Token.integer:
      case Token.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            error(ERROR_invalidParameterOrder);
          bo = JmolConstants.getBondOrderFromFloat(floatParameter(i));
          if (bo == JmolConstants.BOND_ORDER_NULL)
            error(ERROR_invalidArgument);
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          error(ERROR_badArgumentCount);
        float dist = floatParameter(i);
        if (tokAt(i + 1) == Token.percent) {
          dist = -dist / 100f;
          i++;
        }
        distances[distanceCount++] = dist;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (nAtomSets > 2 || isBonds && nAtomSets > 0)
          error(ERROR_badArgumentCount);
        if (haveType || isColorOrRadius)
          error(ERROR_invalidParameterOrder);
        atomSets[nAtomSets++] = expression(i);
        isBonds = isBondSet;
        if (nAtomSets == 2) {
          int pt = iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == Token.identifier
                && parameterAsString(j).equals("_1")) {
              expression2 = i;
              break;
            }
          iToken = pt;
        }
        i = iToken;
        break;
      case Token.color:
        int tok = tokAt(i + 1);
        if (tok != Token.translucent && tok != Token.opaque)
          ptColor = i + 1;
        continue;
      case Token.translucent:
      case Token.opaque:
        if (translucency != null)
          error(ERROR_invalidArgument);
        isColorOrRadius = true;
        translucency = parameterAsString(i);
        if (theTok == Token.translucent && isFloatParameter(i + 1))
          translucentLevel = getTranslucentLevel(++i);
        ptColor = i + 1;
        break;
      case Token.pdb:
        boolean isAuto = (tokAt(2) == Token.auto);
        checkLength(isAuto ? 3 : 2);
        if (!isSyntaxCheck)
          viewer.setPdbConectBonding(isAuto);
        return;
      case Token.adjust:
      case Token.auto:
      case Token.create:
      case Token.modify:
      case Token.modifyorcreate:
        // must be an operation and must be last argument
        haveOperation = true;
        if (++i != statementLength)
          error(ERROR_invalidParameterOrder);
        operation = theTok;
        if (theTok == Token.auto
            && !(bondOrder == JmolConstants.BOND_ORDER_NULL
                || bondOrder == JmolConstants.BOND_H_REGULAR || bondOrder == JmolConstants.BOND_AROMATIC))
          error(ERROR_invalidArgument);
        break;
      case Token.struts:
        if (!isColorOrRadius) {
          color = 0xFFFFFF;
          translucency = "translucent";
          translucentLevel = 0.5f;
          radius = viewer.getStrutDefaultRadius();
          isColorOrRadius = true;
        }
        if (!haveOperation)
          operation = JmolConstants.CONNECT_MODIFY_OR_CREATE;
        haveOperation = true;
        // fall through
      case Token.identifier:
      case Token.aromatic:
      case Token.hbond:
        if (i > 0) {
          if (ptColor == i)
            break;
          // I know -- should have required the COLOR keyword
          if (isColorParam(i)) {
            ptColor = -i;
            break;
          }
        }
        String cmd = parameterAsString(i);
        if ((bo = JmolConstants.getBondOrderFromString(cmd)) == JmolConstants.BOND_ORDER_NULL) {
          error(ERROR_invalidArgument);
        }
        // must be bond type
        if (haveType)
          error(ERROR_incompatibleArguments);
        haveType = true;
        switch (bo) {
        case JmolConstants.BOND_PARTIAL01:
          switch (tokAt(i + 1)) {
          case Token.decimal:
            bo = JmolConstants
                .getPartialBondOrderFromInteger(statement[++i].intValue);
            break;
          case Token.integer:
            bo = (short) intParameter(++i);
            break;
          }
          break;
        case JmolConstants.BOND_H_REGULAR:
          if (tokAt(i + 1) == Token.integer) {
            bo = (short) (intParameter(++i) << JmolConstants.BOND_HBOND_SHIFT);
            energy = floatParameter(++i);
          }
          break;
        }
        bondOrder = bo;
        break;
      case Token.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case Token.none:
      case Token.delete:
        if (++i != statementLength)
          error(ERROR_invalidParameterOrder);
        operation = JmolConstants.CONNECT_DELETE_BONDS;
        // if (isColorOrRadius) / for struts automatic color
        // error(ERROR_invalidArgument);
        isDelete = true;
        isColorOrRadius = false;
        break;
      default:
        ptColor = i;
        break;
      }
      // now check for color -- -i means we've already checked
      if (i > 0) {
        if (ptColor == -i || ptColor == i && isColorParam(i)) {
          color = getArgbParam(i);
          i = iToken;
          isColorOrRadius = true;
        } else if (ptColor == i) {
          error(ERROR_invalidArgument);
        }
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
      definedAtomSets.put("_1", bs);
      BitSet bs0 = atomSets[0];
      for (int atom1 = bs0.nextSetBit(0); atom1 >= 0; atom1 = bs0
          .nextSetBit(atom1 + 1)) {
        bs.set(atom1);
        result = viewer.makeConnections(distances[0], distances[1], bondOrder,
            operation, bs, expression(expression2), bsBonds, isBonds, 0);
        nNew += Math.abs(result[0]);
        nModified += result[1];
        bs.clear(atom1);
      }
    } else {
      result = viewer.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds, energy);
      nNew += Math.abs(result[0]);
      nModified += result[1];
    }
    if (isDelete) {
      if (!(tQuiet || scriptLevel > scriptReportingLevel))
        scriptStatusOrBuffer(GT._("{0} connections deleted", nModified));
      return;
    }
    if (isColorOrRadius) {
      viewer.selectBonds(bsBonds);
      if (!Float.isNaN(radius))
        setShapeSize(JmolConstants.SHAPE_STICKS, (int) (radius * 2000),
            bsBonds);
      if (color != Integer.MIN_VALUE)
        setShapeProperty(JmolConstants.SHAPE_STICKS, "color",
            new Integer(color), bsBonds);
      if (translucency != null) {
        if (translucentLevel == Float.MAX_VALUE)
          translucentLevel = viewer.getDefaultTranslucent();
        setShapeProperty(JmolConstants.SHAPE_STICKS, "translucentLevel",
            new Float(translucentLevel));
        setShapeProperty(JmolConstants.SHAPE_STICKS, "translucency",
            translucency, bsBonds);
      }
    }
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT._("{0} new bonds; {1} modified", new Object[] {
          new Integer(nNew), new Integer(nModified) }));
  }

  private float getTranslucentLevel(int i) throws ScriptException {
    float f = floatParameter(i);
    return (theTok == Token.integer && f > 0 && f < 9 ? f + 1 : f);
  }

  private void getProperty() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String name = property;
    if (name.indexOf(".") >= 0)
      name = name.substring(0, name.indexOf("."));
    if (name.indexOf("[") >= 0)
      name = name.substring(0, name.indexOf("["));
    int propertyID = PropertyManager.getPropertyNumber(name);
    String param = optParameterAsString(2);
    int tok = tokAt(2);
    BitSet bs = (tok == Token.expressionBegin || tok == Token.bitset ? expression(2)
        : null);
    if (property.length() > 0 && propertyID < 0) {
      // no such property
      property = ""; // produces a list from Property Manager
      param = "";
    } else if (propertyID >= 0 && statementLength < 3) {
      param = PropertyManager.getDefaultParam(propertyID);
      if (param.equals("(visible)")) {
        viewer.setModelVisibility();
        bs = viewer.getVisibleSet();
      }
    } else if (propertyID == PropertyManager.PROP_FILECONTENTS_PATH) {
      for (int i = 3; i < statementLength; i++)
        param += parameterAsString(i);
    }
    retValue = (String) viewer.getProperty("readable", property,
        (bs == null ? (Object) param : (Object) bs));
    showString(retValue);
  }

  private void background(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (theTok == Token.image) {
      // background IMAGE "xxxx.jpg"
      String file = parameterAsString(checkLast(++i));
      if (isSyntaxCheck)
        return;
      Hashtable htParams = new Hashtable();
      Object image = null;
      if (!file.equalsIgnoreCase("none") && file.length() > 0)
        image = viewer.getFileAsImage(file, htParams);
      if (image instanceof String)
        evalError((String) image, null);
      viewer.setBackgroundImage((String) htParams.get("fullPathName"),
          (Image) image);
      return;
    }
    if (isColorParam(i) || theTok == Token.none) {
      argb = getArgbParamLast(i, true);
      if (isSyntaxCheck)
        return;
      setObjectArgb("background", argb);
      viewer.setBackgroundImage(null, null);
      return;
    }
    int iShape = getShapeType(theTok);
    colorShape(iShape, i + 1, true);
  }

  private void center(int i) throws ScriptException {
    // from center (atom) or from zoomTo under conditions of not
    // windowCentered()
    if (statementLength == 1) {
      viewer.setNewRotationCenter(null);
      return;
    }
    Point3f center = centerParameter(i);
    if (center == null)
      error(ERROR_invalidArgument);
    if (!isSyntaxCheck)
      viewer.setNewRotationCenter(center);
  }

  private String setObjectProperty() throws ScriptException {
    String s = "";
    String id = getShapeNameParameter(2);
    Object[] data = new Object[] { id, null };
    if (isSyntaxCheck)
      return "";
    int iTok = iToken;
    int tokCommand = tokAt(0);
    boolean isWild = TextFormat.isWild(id);
    for (int iShape = JmolConstants.SHAPE_DIPOLES;;) {
      if (iShape != JmolConstants.SHAPE_MO
          && getShapeProperty(iShape, "checkID", data)) {
        setShapeProperty(iShape, "thisID", id);
        switch (tokCommand) {
        case Token.delete:
          setShapeProperty(iShape, "delete", null);
          break;
        case Token.hide:
        case Token.display:
          setShapeProperty(iShape, "hidden",
              tokCommand == Token.display ? Boolean.FALSE : Boolean.TRUE);
          break;
        case Token.show:
          if (iShape == JmolConstants.SHAPE_ISOSURFACE && !isWild)
            return getIsosurfaceJvxl(false, JmolConstants.SHAPE_ISOSURFACE);
          else if (iShape == JmolConstants.SHAPE_PMESH && !isWild)
            return getIsosurfaceJvxl(true, JmolConstants.SHAPE_PMESH);
          s += (String) getShapeProperty(iShape, "command") + "\n";
        case Token.color:
          colorShape(iShape, iTok + 1, false);
          break;
        }
        if (!isWild)
          break;
      }
      if (iShape == JmolConstants.SHAPE_DIPOLES)
        iShape = JmolConstants.SHAPE_MAX_HAS_ID;
      if (--iShape < JmolConstants.SHAPE_MIN_HAS_ID)
        break;
    }
    return s;
  }

  private void color() throws ScriptException {
    int i = 1;
    if (isColorParam(1)) {
      theTok = Token.atoms;
    } else {
      int argb = 0;
      i = 2;
      switch (getToken(1).tok) {
      case Token.dollarsign:
        setObjectProperty();
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
      case Token.formalcharge:
      case Token.partialcharge:
      case Token.straightness:
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
      case Token.symop:
      case Token.user:
      case Token.property:
        theTok = Token.atoms;
        i = 1;
        break;
      case Token.string:
        String strColor = stringParameter(1);
        boolean isTranslucent = (tokAt(2) == Token.translucent);
        if (!isSyntaxCheck)
          viewer.setPropertyColorScheme(strColor, isTranslucent, true);
        i = (isTranslucent ? 3 : 2);
        if (tokAt(i) == Token.range || tokAt(i) == Token.absolute) {
          float min = floatParameter(++i);
          float max = floatParameter(++i);
          if (!isSyntaxCheck)
            viewer.setCurrentColorRange(min, max);
        }
        return;
      case Token.range:
      case Token.absolute:
        float min = floatParameter(2);
        float max = floatParameter(checkLast(3));
        if (!isSyntaxCheck)
          viewer.setCurrentColorRange(min, max);
        return;
      case Token.background:
        argb = getArgbParamLast(2, true);
        if (!isSyntaxCheck)
          setObjectArgb("background", argb);
        return;
      case Token.bitset:
      case Token.expressionBegin:
        i = -1;
        theTok = Token.atoms;
        break;
      case Token.rubberband:
        argb = getArgbParamLast(2, false);
        if (!isSyntaxCheck)
          viewer.setRubberbandArgb(argb);
        return;
      case Token.selectionhalos:
        i = 2;
        if (tokAt(2) == Token.opaque)
          i++;
        argb = getArgbParamLast(i, true);
        if (isSyntaxCheck)
          return;
        loadShape(JmolConstants.SHAPE_HALOS);
        setShapeProperty(JmolConstants.SHAPE_HALOS, "argbSelection",
            new Integer(argb));
        return;
      case Token.axes:
      case Token.boundbox:
      case Token.unitcell:
      case Token.identifier:
      case Token.hydrogen:
        // color element
        String str = parameterAsString(1);
        if (checkToken(2)) {
          switch (getToken(2).tok) {
          case Token.rasmol:
            argb = Token.rasmol;
            break;
          case Token.none:
          case Token.jmol:
            argb = Token.jmol;
            break;
          default:
            argb = getArgbParam(2);
          }
        }
        if (argb == 0)
          error(ERROR_colorOrPaletteRequired);
        checkLast(iToken);
        if (str.equalsIgnoreCase("axes")
            || StateManager.getObjectIdFromName(str) >= 0) {
          setObjectArgb(str, argb);
          return;
        }
        if (changeElementColor(str, argb))
          return;
        error(ERROR_invalidArgument);
      case Token.isosurface:
        setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID",
            JmolConstants.PREVIOUS_MESH_ID);
        break;
      }
    }
    colorShape(getShapeType(theTok), i, false);
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

  private void colorShape(int shapeType, int index, boolean isBackground)
      throws ScriptException {
    String translucency = null;
    Object colorvalue = null;
    BitSet bs = null;
    String prefix = "";
    boolean isColor = false;
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
        translucentLevel = getTranslucentLevel(index++);
    }
    int tok = 0;
    if (index < statementLength && tokAt(index) != Token.on
        && tokAt(index) != Token.off) {
      isColor = true;
      tok = getToken(index).tok;
      if (isColorParam(index)) {
        int argb = getArgbParam(index, false);
        colorvalue = (argb == 0 ? null : new Integer(argb));
        if (translucency == null && tokAt(index = iToken + 1) != Token.nada) {
          getToken(index);
          if (translucency == null
              && (theTok == Token.translucent || theTok == Token.opaque)) {
            translucency = parameterAsString(index);
            if (theTok == Token.translucent && isFloatParameter(index + 1))
              translucentLevel = getTranslucentLevel(++index);
          }
          // checkLength(index + 1);
          // iToken = index;
        }
      } else if (shapeType == JmolConstants.SHAPE_LCAOCARTOON) {
        iToken--; // back up one
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
            || (pid == JmolConstants.PALETTE_TYPE || pid == JmolConstants.PALETTE_ENERGY)
            && shapeType != JmolConstants.SHAPE_HSTICKS)
          error(ERROR_invalidArgument);
        Object  data = null;
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          BitSet bsSelected = (viewer.isRangeSelected() ? viewer.getSelectionSet() : null);
          if (isColorIndex) {
            if (!isSyntaxCheck) {
              data = getBitsetProperty(bsSelected, (isByElement ? Token.elemno
                  : Token.groupid)
                  | Token.minmaxmask, null, null, null, null, false,
                  Integer.MAX_VALUE, false);
            }
          } else {
            if (!isColorIndex && shapeType != JmolConstants.SHAPE_ISOSURFACE)
              index++;
            if (name.equals("property")
                && Token.tokAttr((tok = getToken(index).tok),
                    Token.atomproperty)
                && !Token.tokAttr(tok, Token.strproperty)) {
              if (!isSyntaxCheck) {
                data = getBitsetProperty(bsSelected, getToken(index++).tok
                    | Token.minmaxmask, null, null, null, null, false,
                    Integer.MAX_VALUE, false);
              }
            }
          }
          if (data != null && !(data instanceof float[])) {
            if (data instanceof String[]) {
              float[] fdata = new float[((String[]) data).length];
              Parser.parseFloatArray((String[]) data, null, fdata);
              data = fdata;
            } else {
              error(ERROR_invalidArgument);
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
              float[] range = (float[]) getShapeProperty(shapeType,
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
        checkLength(index);
      }
    }
    if (isSyntaxCheck || shapeType < 0)
      return;
    typeMask = (shapeType == JmolConstants.SHAPE_STRUTS ? JmolConstants.BOND_STRUT
        : shapeType == JmolConstants.SHAPE_HSTICKS ? JmolConstants.BOND_HYDROGEN_MASK
            : shapeType == JmolConstants.SHAPE_SSSTICKS ? JmolConstants.BOND_SULFUR_MASK
                : shapeType == JmolConstants.SHAPE_STICKS ? JmolConstants.BOND_COVALENT_MASK
                    : 0);
    if (typeMask == 0) {
      loadShape(shapeType);
      if (shapeType == JmolConstants.SHAPE_LABELS)
        setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
            .getNoneSelected());
    } else {
      if (bs != null) {
        viewer.selectBonds(bs);
        bs = null;
      }
      shapeType = JmolConstants.SHAPE_STICKS;
      setShapeProperty(shapeType, "type", new Integer(typeMask));
    }
    if (isColor) {
      // ok, the following five options require precalculation.
      // the state must not save them as paletteIDs, only as pure
      // color values.
      switch (tok) {
      case Token.surfacedistance:
      case Token.straightness:
        viewer.autoCalculate(tok);
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
      if (bs == null)
        setShapeProperty(shapeType, prefix + "color", colorvalue);
      else
        setShapeProperty(shapeType, prefix + "color", colorvalue, bs);
    }
    if (translucency != null)
      setShapeTranslucency(shapeType, prefix, translucency, translucentLevel,
          bs);
    if (typeMask != 0)
      setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
          JmolConstants.BOND_COVALENT_MASK));
  }

  private void colorShape(int shapeType, int typeMask, int argb,
                          String translucency, float translucentLevel, BitSet bs) {

    if (typeMask != 0) {
      setShapeProperty(shapeType = JmolConstants.SHAPE_STICKS, "type",
          new Integer(typeMask));
    }
    setShapeProperty(shapeType, "color", new Integer(argb), bs);
    if (translucency != null)
      setShapeTranslucency(shapeType, "", translucency, translucentLevel, bs);
    if (typeMask != 0)
      setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
          JmolConstants.BOND_COVALENT_MASK));
  }
  
  private void setShapeTranslucency(int shapeType, String prefix,
                                    String translucency,
                                    float translucentLevel, BitSet bs) {
    if (translucentLevel == Float.MAX_VALUE)
      translucentLevel = viewer.getDefaultTranslucent();
    setShapeProperty(shapeType, "translucentLevel", new Float(translucentLevel));
    if (prefix == null)
      return;
    if (bs == null)
      setShapeProperty(shapeType, prefix + "translucency", translucency);
    else if (!isSyntaxCheck)
      setShapeProperty(shapeType, prefix + "translucency", translucency,
          bs);
  }

  private void cd() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String dir = (statementLength == 1 ? null : parameterAsString(1));
    showString(viewer.cd(dir));
  }

  private Object[] data;

  private void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (iToken = statementLength) {
    case 5:
      // parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = parameterAsString(2);
      // fall through
    case 4:
    case 2:
      dataLabel = parameterAsString(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!isSyntaxCheck)
          viewer.setData(null, null, 0, 0, 0, 0, 0);
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
      error(ERROR_badArgumentCount);
    }
    dataLabel = dataLabel.toLowerCase();
    String dataType = dataLabel + " ";
    boolean isDefault = (dataLabel.indexOf("(default)") >= 0);
    dataType = dataType.substring(0, dataType.indexOf(" "));
    boolean isModel = dataType.equals("model");
    boolean isAppend = dataType.equals("append");
    boolean processModel = ((isModel || isAppend) && (!isSyntaxCheck || isCmdLine_C_Option));
    if ((isModel || isAppend) && dataString == null)
      error(ERROR_invalidArgument);
    int userType = -1;
    if (processModel) {
      // only if first character is "|" do we consider "|" to be new line
      char newLine = viewer.getInlineChar();
      if (dataString.length() > 0 && dataString.charAt(0) != newLine)
        newLine = '\0';
      int modelCount = viewer.getModelCount()
          - (viewer.getFileName().equals("zapped") ? 1 : 0);
      boolean appendNew = viewer.getAppendNew();
      viewer.loadInline(dataString, newLine, isAppend);
      if (isAppend && appendNew) {
        viewer.setAnimationRange(-1, -1);
        viewer.setCurrentModelIndex(modelCount);
      }
    }
    if (isSyntaxCheck && !processModel)
      return;
    data = new Object[3];
    if (dataType.equals("element_vdw")) {
      // vdw for now
      data[0] = dataType;
      data[1] = dataString.replace(';', '\n');
      int n = JmolConstants.elementNumberMax;
      int[] eArray = new int[n + 1];
      for (int ie = 1; ie <= n; ie++)
        eArray[ie] = ie;
      data[2] = eArray;
      viewer.setData("element_vdw", data, n, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data2d_") == 0) {
      // data2d_someName
      data[0] = dataLabel;
      data[1] = Parser.parseFloatArray2d(dataString);
      viewer.setData(dataLabel, data, 0, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data3d_") == 0) {
      // data3d_someName
      data[0] = dataLabel;
      data[1] = Parser.parseFloatArray3d(dataString);
      viewer.setData(dataLabel, data, 0, 0, 0, 0, 0);
      return;
    }
    String[] tokens = Parser.getTokens(dataLabel);
    if (dataType.indexOf("property_") == 0
        && !(tokens.length == 2 && tokens[1].equals("set"))) {
      BitSet bs = viewer.getSelectionSet();
      data[0] = dataType;
      int atomNumberField = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberField")).intValue());
      int atomNumberFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberColumnCount")).intValue());
      int propertyField = (isOneValue ? Integer.MIN_VALUE : ((Integer) viewer
          .getParameter("propertyDataField")).intValue());
      int propertyFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyDataColumnCount")).intValue());
      if (!isOneValue && dataLabel.indexOf(" ") >= 0) {
        if (tokens.length == 3) {
          // DATA "property_whatever [atomField] [propertyField]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          propertyField = Parser.parseInt(tokens[2]);
        }
        if (tokens.length == 5) {
          // DATA
          // "property_whatever [atomField] [atomFieldColumnCount] [propertyField] [propertyDataColumnCount]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          atomNumberFieldColumnCount = Parser.parseInt(tokens[2]);
          propertyField = Parser.parseInt(tokens[3]);
          propertyFieldColumnCount = Parser.parseInt(tokens[4]);
        }
      }
      if (atomNumberField < 0)
        atomNumberField = 0;
      if (propertyField < 0)
        propertyField = 0;
      int atomCount = viewer.getAtomCount();
      int[] atomMap = null;
      BitSet bsTemp = new BitSet(atomCount);
      if (atomNumberField > 0) {
        atomMap = new int[atomCount + 2];
        for (int j = 0; j <= atomCount; j++)
          atomMap[j] = -1;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          int atomNo = viewer.getAtomNumber(j);
          if (atomNo > atomCount + 1 || atomNo < 0 || bsTemp.get(atomNo))
            continue;
          bsTemp.set(atomNo);
          atomMap[atomNo] = j;
        }
        data[2] = atomMap;
      } else {
        data[2] = BitSetUtil.copy(bs);
      }
      data[1] = dataString;
      viewer.setData(dataType, data, atomCount, atomNumberField,
          atomNumberFieldColumnCount, propertyField, propertyFieldColumnCount);
      return;
    }
    userType = AtomCollection.getUserSettableType(dataType);
    if (userType >= 0) {
      // this is a known settable type or "property_xxxx"
      viewer.setAtomData(userType, dataType, dataString, isDefault);
      return;
    }
    // this is just information to be stored.
    data[0] = dataLabel;
    data[1] = dataString;
    viewer.setData(dataType, data, 0, 0, 0, 0, 0);
  }

  private void define() throws ScriptException {
    // note that the standard definition depends upon the
    // current state. Once defined, a setName is the set
    // of atoms that matches the definition at that time.
    // adding DYMAMIC_ to the beginning of the definition
    // allows one to create definitions that are recalculated
    // whenever they are used. When used, "DYNAMIC_" is dropped
    // so, for example:
    // define DYNAMIC_what selected and visible
    // and then
    // select what
    // will return different things at different times depending
    // upon what is selected and what is visible
    // but
    // define what selected and visible
    // will evaluate the moment it is defined and then represent
    // that set of atoms forever.

    String setName = (String) getToken(1).value;
    if (isSyntaxCheck)
      return;
    boolean isSite = setName.startsWith("site_");
    boolean isDynamic = (setName.indexOf("dynamic_") == 0);
    if (isDynamic || isSite) {
      Token[] code = new Token[statementLength];
      for (int i = statementLength; --i >= 0;)
        code[i] = statement[i];
      definedAtomSets
          .put("!" + (isSite ? setName : setName.substring(8)), code);
      if (!isSite)
        viewer.addStateScript(thisCommand, false, true);
    } else {
      BitSet bs = expression(2);
      definedAtomSets.put(setName, bs);
      setStringProperty("@" + setName, Escape.escape(bs));
    }
  }

  private void echo(int index, boolean isImage) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String text = optParameterAsString(index);
    if (viewer.getEchoStateActive()) {
      if (isImage) {
        Hashtable htParams = new Hashtable();
        Object image = viewer.getFileAsImage(text, htParams);
        if (image instanceof String) {
          text = (String) image;
        } else {
          setShapeProperty(JmolConstants.SHAPE_ECHO, "text", htParams
              .get("fullPathName"));
          setShapeProperty(JmolConstants.SHAPE_ECHO, "image", image);
          text = null;
        }
      } else if (text.startsWith("\1")) {
        // no reporting, just screen echo, from mouseManager key press
        text = text.substring(1);
        isImage = true;
      }
      if (text != null)
        setShapeProperty(JmolConstants.SHAPE_ECHO, "text", text);
    }
    if (!isImage && viewer.getRefreshing())
      showString(viewer.formatText(text));
  }

  private void message() throws ScriptException {
    String text = parameterAsString(checkLast(1));
    if (isSyntaxCheck)
      return;
    String s = viewer.formatText(text);
    if (outputBuffer == null)
      viewer.showMessage(s);
    if (!s.startsWith("_"))
      scriptStatusOrBuffer(s);
  }

  private void log() throws ScriptException {
    if (statementLength == 1)
      error(ERROR_badArgumentCount);
    if (isSyntaxCheck)
      return;
    String s = (String) parameterExpression(1, 0, "", false);
    if (tokAt(1) == Token.off)
      setStringProperty("logFile", "");
    else
      viewer.log(s);
  }

  private void print() throws ScriptException {
    if (statementLength == 1)
      error(ERROR_badArgumentCount);
    showString((String) parameterExpression(1, 0, "", false), true);
  }

  private boolean pause() throws ScriptException {
    if (isSyntaxCheck)
      return false;
    String msg = optParameterAsString(1);
    if (!viewer.getBooleanProperty("_useCommandThread")) {
      // showString("Cannot pause thread when _useCommandThread = FALSE: " +
      // msg);
      // return;
    }
    if (viewer.autoExit || !viewer.haveDisplay)
      return false;
    if (scriptLevel == 0 && pc == aatoken.length - 1) {
      viewer.scriptStatus("nothing to pause: " + msg);
      return false;
    }
    msg = (msg.length() == 0 ? ": RESUME to continue." : ": "
        + viewer.formatText(msg));
    pauseExecution();
    viewer.scriptStatus("script execution paused" + msg,
        "script paused for RESUME");
    return true;
  }

  private void label(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    loadShape(JmolConstants.SHAPE_LABELS);
    String strLabel = null;
    switch (getToken(index).tok) {
    case Token.on:
      strLabel = viewer.getStandardLabelFormat();
      break;
    case Token.off:
      break;
    case Token.hide:
    case Token.display:
      setShapeProperty(JmolConstants.SHAPE_LABELS, "display",
          theTok == Token.display ? Boolean.TRUE : Boolean.FALSE);
      return;
    default:
      strLabel = parameterAsString(index);
    }
    shapeManager.setLabel(strLabel, viewer.getSelectionSet());
  }

  private void hover() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(1);
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.setHoverLabel(strLabel);
  }

  private void load() throws ScriptException {
    boolean isAppend = false;
    Vector firstLastSteps = null;
    int modelCount = viewer.getModelCount()
        - (viewer.getFileName().equals("zapped") ? 1 : 0);
    boolean appendNew = viewer.getAppendNew();
    StringBuffer loadScript = new StringBuffer("load");
    int nFiles = 1;
    Hashtable htParams = new Hashtable();
    int i = 1;
    // ignore optional file format
    // String filename = "";
    String modelName = null;
    String filename = null;
    String[] filenames = null;
    String[] tempFileInfo = null;
    boolean isInline = false;
    int tokType = 0;
    int tok;
    boolean doLoadFiles = (!isSyntaxCheck || isCmdLine_C_Option);
    String errMsg = null;
    String sOptions = "";

    // check for special parameters

    if (statementLength == 1) {
      i = 0;
    } else {
      modelName = parameterAsString(i);
      tok = tokAt(i);
      /*
       * I have no idea what this was about. if
       * (modelName.equalsIgnoreCase("data")) { modelName =
       * stringParameter(++i); if (!isSyntaxCheck) htParams.put("parameterData",
       * viewer.getFileAsString(modelName));
       * loadScript.append(" data / *file* /").append(Escape.escape(modelName));
       * tok = tokAt(++i); modelName = parameterAsString(i); }
       */
      if (tok == Token.identifier || modelName.equalsIgnoreCase("fileset")) {
        if (modelName.equals("menu")) {
          String m = parameterAsString(checkLast(2));
          if (!isSyntaxCheck)
            viewer.setMenu(m, true);
          return;
        }
        i++;
        loadScript.append(" " + modelName);
        isAppend = (modelName.equalsIgnoreCase("append"));
        isInline = (modelName.equalsIgnoreCase("inline"));
        tokType = (Parser.isOneOf(modelName.toLowerCase(),
            JmolConstants.LOAD_ATOM_DATA_TYPES) ? Token
            .getTokFromName(modelName.toLowerCase()) : 0);
        if (tokType > 0) {
          // loading just some data here
          // xyz vxyz vibration temperature occupancy partialcharge
          htParams.put("atomDataOnly", Boolean.TRUE);
          htParams.put("modelNumber", new Integer(1));
          if (tokType == Token.vibration)
            tokType = Token.vibxyz;
          tempFileInfo = viewer.getFileInfo();
        }
        if (isAppend
            && ((filename = optParameterAsString(i))
                .equalsIgnoreCase("trajectory")
                || filename.equalsIgnoreCase("models") || filename
                .equalsIgnoreCase("inline"))) {
          modelName = filename;
          loadScript.append(" " + modelName);
          i++;
        }
        if (tokType > 0)
          isAppend = true;
        if (modelName.equalsIgnoreCase("trajectory")
            || modelName.equalsIgnoreCase("models")) {
          if (modelName.equalsIgnoreCase("trajectory"))
            htParams.put("isTrajectory", Boolean.TRUE);
          if (isPoint3f(i)) {
            Point3f pt = getPoint3f(i, false);
            i = iToken + 1;
            // first last stride
            htParams.put("firstLastStep", new int[] { (int) pt.x, (int) pt.y,
                (int) pt.z });
            loadScript.append(" " + Escape.escape(pt));
          } else if (tokAt(i) == Token.bitset) {
            BitSet bsModels = (BitSet) getToken(i++).value;
            htParams.put("bsModels", bsModels);
            loadScript.append(" " + Escape.escape(bsModels));
          } else {
            htParams.put("firstLastStep", new int[] { 0, -1, 1 });
          }
        }
      } else if (modelName.equalsIgnoreCase("inline")) {
      } else {
        modelName = "fileset";
      }
      if (getToken(i).tok != Token.string)
        error(ERROR_filenameExpected);
    }
    // long timeBegin = System.currentTimeMillis();

    // file name is next

    int filePt = i;
    String localName = null;
    if (tokAt(filePt + 1) == Token.as) {
      localName = stringParameter(i + 2);
      i += 2;
    }
    if (statementLength == i + 1) {
      if (i == 0 || (filename = parameterAsString(filePt)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap(false);
        return;
      }
      if (filename.indexOf("[]") >= 0)
        return;
    } else if (getToken(i + 1).tok == Token.leftbrace
        || theTok == Token.point3f || theTok == Token.integer
        || theTok == Token.manifest || theTok == Token.packed
        || theTok == Token.filter && tokAt(i + 3) != Token.coord
        || theTok == Token.identifier && tokAt(i + 3) != Token.coord) {
      if ((filename = parameterAsString(filePt)).length() == 0)
        filename = viewer.getFullPathName();
      if (filePt == i)
        i++;
      if (filename == null) {
        zap(false);
        return;
      }
      if (filename.indexOf("[]") >= 0)
        return;
      if ((tok = tokAt(i)) == Token.manifest) {
        String manifest = stringParameter(++i);
        htParams.put("manifest", manifest);
        sOptions += " MANIFEST " + Escape.escape(manifest);
        tok = tokAt(++i);
      }
      if (tok == Token.integer) {
        int n = intParameter(i);
        sOptions += " " + n;
        if (n < 0)
          htParams.put("vibrationNumber", new Integer(-n));
        else
          htParams.put("modelNumber", new Integer(n));
        tok = tokAt(++i);
      }
      Point3f lattice = null;
      if (tok == Token.leftbrace || tok == Token.point3f) {
        lattice = getPoint3f(i, false);
        i = iToken + 1;
        tok = tokAt(i);
      }
      boolean isPacked = false;
      if (tok == Token.packed) {
        if (lattice == null)
          lattice = new Point3f(555, 555, -1);
        isPacked = true;
        iToken++;
        i++;
      }
      if (lattice != null) {
        i = iToken + 1;
        htParams.put("lattice", lattice);
        sOptions += " {" + (int) lattice.x + " " + (int) lattice.y + " "
            + (int) lattice.z + "}";
        if (isPacked) {
          htParams.put("packed", Boolean.TRUE);
          sOptions += " PACKED";
        }
        float distance = 0;
        /*
         * # Jmol 11.3.9 introduces the capability of visualizing the close
         * contacts around a crystalline protein (or any other cyrstal
         * structure) that are to atoms that are in proteins in adjacent unit
         * cells or adjacent to the protein itself. The option RANGE x, where x
         * is a distance in angstroms, placed right after the braces containing
         * the set of unit cells to load does this. The distance, if a positive
         * number, is the maximum distance away from the closest atom in the {1
         * 1 1} set. If the distance x is a negative number, then -x is the
         * maximum distance from the {not symmetry} set. The difference is that
         * in the first case the primary unit cell (555) is first filled as
         * usual, using symmetry operators, and close contacts to this set are
         * found. In the second case, only the file-based atoms ( Jones-Faithful
         * operator x,y,z) are initially included, then close contacts to that
         * set are found. Depending upon the application, one or the other of
         * these options may be desirable.
         */
        if (tokAt(i) == Token.range) {
          i++;
          distance = floatParameter(i++);
          sOptions += " range " + distance;
        }
        htParams.put("symmetryRange", new Float(distance));
        String spacegroup = null;
        float[] fparams = null;
        int iGroup = Integer.MIN_VALUE;
        if (tokAt(i) == Token.spacegroup) {
          ++i;
          spacegroup = TextFormat.simpleReplace(parameterAsString(i++), "''",
              "\"");
          sOptions += " spacegroup " + Escape.escape(spacegroup);
        }
        if (tokAt(i) == Token.unitcell) {
          ++i;
          fparams = floatParameterSet(i, 6, 6);
          i = iToken;
          sOptions += " unitcell {";
          for (int j = 0; j < 6; j++)
            sOptions += (j == 0 ? "" : " ") + fparams[j];
          sOptions += "}";
          htParams.put("unitcell", fparams);
        }
        if (spacegroup != null) {
          if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
            iGroup = -999;
          } else {
            if (spacegroup.indexOf(",") >= 0) // Jones Faithful
              if ((lattice.x < 9 && lattice.y < 9 && lattice.z == 0))
                spacegroup += "#doNormalize=0";
            iGroup = -2;
            htParams.put("spaceGroupName", spacegroup);
          }
        }
        if (fparams != null && iGroup == Integer.MIN_VALUE)
          iGroup = -1;
        if (iGroup != Integer.MIN_VALUE)
          htParams.put("spaceGroupIndex", new Integer(iGroup));
      }
      if (tokAt(i) == Token.filter) {
        String filter = stringParameter(++i);
        htParams.put("filter", filter);
        sOptions += " FILTER " + Escape.escape(filter);
      }
    } else {
      if (i != 2) {
        modelName = parameterAsString(i++);
        loadScript.append(" ").append(Escape.escape(modelName));
      }
      Point3f pt = null;
      BitSet bs = null;
      Vector fNames = new Vector();
      while (i < statementLength) {
        switch (tokAt(i)) {
        case Token.filter:
          String filter = stringParameter(++i);
          htParams.put("filter", filter);
          loadScript.append(" FILTER ").append(Escape.escape(filter));
          ++i;
          continue;
        case Token.coord:
          htParams.remove("isTrajectory");
          if (firstLastSteps == null) {
            firstLastSteps = new Vector();
            pt = new Point3f(0, -1, 1);
          }
          if (isPoint3f(++i)) {
            pt = getPoint3f(i, false);
            i = iToken + 1;
          } else if (tokAt(i) == Token.bitset) {
            bs = (BitSet) getToken(i).value;
            pt = null;
            i = iToken + 1;
          }
          break;
        case Token.identifier:
          error(ERROR_invalidArgument);
        }
        fNames.add(filename = parameterAsString(i++));
        if (pt != null) {
          firstLastSteps.addElement(new int[] { (int) pt.x, (int) pt.y,
              (int) pt.z });
          loadScript.append(" COORD " + Escape.escape(pt));
        } else if (bs != null) {
          firstLastSteps.addElement(bs);
          loadScript.append(" COORD " + Escape.escape(bs));
        }
        loadScript.append(" /*file*/").append(Escape.escape(filename));
      }
      if (firstLastSteps != null)
        htParams.put("firstLastSteps", firstLastSteps);
      nFiles = fNames.size();
      filenames = new String[nFiles];
      for (int j = 0; j < nFiles; j++)
        filenames[j] = (String) fNames.get(j);
      filename = modelName;
    }
    if (!doLoadFiles)
      return;
    if (filenames == null) {
      // standard file loading here
      if (filename.startsWith("@") && filename.length() > 1) {
        String s = getStringParameter(filename.substring(1), false);
        htParams.put("fileData", s);
        loadScript = new StringBuffer("{\nvar " + filename.substring(1) + " = "
            + Escape.escape(s) + ";\n  " + loadScript + "\n}\n");
      } else if (isInline) {
        htParams.put("fileData", filename);
      }
    }

    // OK, we are ready to load the data and create the model set

    OutputStream os = null;
    if (localName != null) {
      String[] fullPath = new String[] { localName };
      os = viewer.getOutputStream(localName, fullPath);
      if (os == null)
        Logger.error("Could not create output stream for " + fullPath[0]);
      else
        htParams.put("OutputStream", os);
    }
    errMsg = viewer.loadModelFromFile(filename, filenames, isAppend, htParams,
        tokType);
    if (os != null)
      try {
        Logger.info(GT._("file {0} created", localName));
        os.close();
      } catch (IOException e) {
        Logger.error("error closing file " + e.getMessage());
      }
    if (tokType > 0) {
      // we are just loading an atom property
      // reset the file info in FileManager, check for errors, and return
      viewer.setFileInfo(tempFileInfo);
      if (errMsg != null && !isCmdLine_c_or_C_Option)
        evalError(errMsg, null);
      return;
    }
    if (filenames == null) {
      // a single file or string -- complete the loadScript
      loadScript.append(" ");
      if (filename.startsWith("@") || isInline) {
        loadScript.append(Escape.escape(filename));
      } else {
        if (!filename.equals("string") && !filename.equals("string[]"))
          loadScript.append("/*file*/");
        if (localName != null)
          localName = viewer.getFullPath(localName);
        loadScript.append(Escape.escape((localName != null ? localName
            : (modelName = (String) htParams.get("fullPathName")))));
      }
      loadScript.append(sOptions);
    }
    viewer.addLoadScript(loadScript.toString());
    // with "@t" we do not save the load command but instead the data statement
    // but there could state problems here because then we don't have the
    // option to save load options with that... Hmm.
    if (errMsg != null && !isCmdLine_c_or_C_Option) {
      if (statementLength == 2) {
        if (errMsg.indexOf("NOTE: file recognized as a script file:") == 0) {
          viewer.addLoadScript("-");
          script(0, filename);
          return;
        }
        String surfaceType = SurfaceFileTyper.determineSurfaceFileType(viewer
            .getBufferedInputStream(filename));
        if (surfaceType != null) {
          viewer.addLoadScript("-");
          runScript("isosurface " + Escape.escape(filename));
          return;
        }
      }
      evalError(errMsg, null);
    }
    if (isAppend && (appendNew || nFiles > 1)) {
      viewer.setAnimationRange(-1, -1);
      viewer.setCurrentModelIndex(modelCount);
    }
    if (logMessages)
      scriptStatusOrBuffer("Successfully loaded:" + modelName);
    String script = viewer.getDefaultLoadScript();
    String msg = "";
    if (script.length() > 0)
      msg += "\nUsing defaultLoadScript: " + script;
    String embeddedScript = (String) viewer.getModelSetAuxiliaryInfo("jmolscript");
    viewer.getModelSetAuxiliaryInfo().remove("jmolscript");
    if (embeddedScript != null && viewer.getAllowEmbeddedScripts()) {
      msg += "\nAdding embedded #jmolscript: " + embeddedScript;
      script += ";" + embeddedScript;
      viewer.setStringProperty("_loadScript", script);
      script = "allowEmbeddedScripts = false;" + script
          + ";allowEmbeddedScripts = true;";
    }
    if (msg.length() > 0)
      Logger.info(msg);
    String siteScript = (String) viewer.getModelSetAuxiliaryInfo("sitescript");
    viewer.getModelSetAuxiliaryInfo().remove("sitescript");
    if (siteScript != null)
      script = siteScript + ";" + script;
    if (script.length() > 0 && !isCmdLine_c_or_C_Option)
      // NOT checking embedded scripts in some cases
      runScript(script);
  }

  private String getFullPathName() throws ScriptException {
    String filename = (!isSyntaxCheck || isCmdLine_C_Option ? viewer
        .getFullPathName() : "test.xyz");
    if (filename == null)
      error(ERROR_invalidArgument);
    return filename;
  }

  private void dataFrame(int datatype) throws ScriptException {
    boolean isQuaternion = false;
    boolean isDraw = (tokAt(0) == Token.draw);
    int pt0 = (isDraw ? 1 : 0);
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    int pt = statementLength - 1;
    String type = optParameterAsString(pt).toLowerCase();
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (isDraw ? " draw" : "");
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      }
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : optParameterAsString(pt)) + "w")
          .substring(0, 1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!Parser.isOneOf(type, "w;x;y;z;r;a")) // a absolute; r relative
        evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    if (isSyntaxCheck) // just in case we later add parameter options to this
      return;
    // for now, just one frame visible
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      error(ERROR_multipleModelsDisplayedNotOK, type);
    modelIndex = viewer.getJmolDataSourceFrame(modelIndex);
    if (isDraw) {
      runScript(viewer.getPdbData(modelIndex, type));
      return;
    }
    int ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, type);
    if (ptDataFrame > 0) {
      // data frame can't be 0.
      viewer.setCurrentModelIndex(ptDataFrame, true);
      // BitSet bs2 = viewer.getModelAtomBitSet(ptDataFrame);
      // bs2.and(bs);
      // need to be able to set data directly as well.
      // viewer.display(BitSetUtil.setAll(viewer.getAtomCount()), bs2, tQuiet);
      return;
    }
    String[] savedFileInfo = viewer.getFileInfo();
    boolean oldAppendNew = viewer.getAppendNew();
    viewer.setAppendNew(true);
    String data = viewer.getPdbData(modelIndex, type);
    boolean isOK = (data != null && viewer.loadInline(data, true) == null);
    viewer.setAppendNew(oldAppendNew);
    viewer.setFileInfo(savedFileInfo);
    if (!isOK)
      return;
    StateScript ss = viewer.addStateScript(type, true, false);
    int modelCount = viewer.getModelCount();
    viewer.setJmolDataFrame(type, modelIndex, modelCount - 1);
    String script;
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
    default:
      viewer.setFrameTitle(modelCount - 1, type + " plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {200 0 0} {-200 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 200 0} {0 -200 0} \"psi\";"
      // + "draw ramaAxisZ" + modelCount + " {0 0 400} {0 0 0} \"" +
      // (isRamachandranRelative ? "theta" : "omega") +"\";"
      ;
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      viewer.setFrameTitle(modelCount - 1, type + " for model "
          + viewer.getModelNumberDotted(modelIndex));
      String color = (Graphics3D
          .getHexCode(viewer.getColixBackgroundContrast()));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; " + "isosurface quatSphere"
          + modelCount + " resolution 1.0 color " + color
          + " sphere 10.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {10 0 0} {-10 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 10 0} {0 -10 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 10} {0 0 -10} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02";
      break;
    }
    runScript(script);
    ss.setModelIndex(viewer.getCurrentModelIndex());
    viewer.setRotationRadius(isQuaternion ? 12.5f : 260f, true);
    loadShape(JmolConstants.SHAPE_ECHO);
    showString("frame " + viewer.getModelNumberDotted(modelCount - 1)
        + " created: " + type);
  }

  private void measure() throws ScriptException {
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
      error(ERROR_keywordExpected, "ON, OFF, DELETE");
      break;
    case 3: // measure delete N
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
    int modelIndex = -1;
    int atomIndex = -1;
    int ptFloat = -1;
    int[] countPlusIndexes = new int[5];
    float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isNotConnected = false;
    boolean isRange = true;
    int tokAction = Token.opToggle;
    String strFormat = null;
    Vector points = new Vector();
    BitSet bs = new BitSet();
    Object value = null;
    TickInfo tickInfo = null;
    for (int i = 1; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      case Token.identifier:
        error(ERROR_keywordExpected, "ALL, ALLCONNECTED, DELETE");
      default:
        error(ERROR_expressionOrIntegerExpected);
      case Token.opNot:
        if (tokAt(i + 1) != Token.connected)
          error(ERROR_invalidArgument);
        i++;
        isNotConnected = true;
        break;
      case Token.connected:
      case Token.allconnected:
      case Token.all:
        isAllConnected = (theTok == Token.allconnected);
        atomIndex = -1;
        isAll = true;
        if (isAllConnected && isNotConnected)
          error(ERROR_invalidArgument);
        break;
      case Token.decimal:
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = floatParameter(i);
        break;
      case Token.delete:
        if (tokAction != Token.opToggle)
          error(ERROR_invalidArgument);
        tokAction = Token.delete;
        break;
      case Token.integer:
        int iParam = intParameter(i);
        if (isAll) {
          isRange = true; // irrelevant if just four integers
          ptFloat = (ptFloat + 1) % 2;
          rangeMinMax[ptFloat] = iParam;
        } else {
          atomIndex = viewer.getAtomIndexFromAtomNumber(iParam);
          if (!isSyntaxCheck && atomIndex < 0)
            return;
          if (value != null)
            error(ERROR_invalidArgument);
          if ((countPlusIndexes[0] = ++nAtoms) > 4)
            error(ERROR_badArgumentCount);
          countPlusIndexes[nAtoms] = atomIndex;
        }
        break;
      case Token.modelindex:
        modelIndex = intParameter(++i);
        break;
      case Token.off:
        if (tokAction != Token.opToggle)
          error(ERROR_invalidArgument);
        tokAction = Token.off;
        break;
      case Token.on:
        if (tokAction != Token.opToggle)
          error(ERROR_invalidArgument);
        tokAction = Token.on;
        break;
      case Token.range:
        isAll = true;
        isRange = true; // unnecessary
        atomIndex = -1;
        break;
      case Token.string:
        // measures "%a1 %a2 %v %u"
        strFormat = stringParameter(i);
        break;
      case Token.ticks:
        tickInfo = checkTicks(i);
        i = iToken;
        tokAction = Token.define;
        break;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
      case Token.dollarsign:
        if (atomIndex >= 0)
          error(ERROR_invalidArgument);
        expressionResult = Boolean.FALSE;
        value = centerParameter(i);
        if (expressionResult instanceof BitSet) {
          value = bs = (BitSet) expressionResult;
          if (!isSyntaxCheck && bs.length() == 0)
            return;
        }
        if (value instanceof Point3f) {
          Point3fi v = new Point3fi();
          v.set((Point3f) value);
          v.modelIndex = (short) modelIndex;
          value = v;
        }
        if ((nAtoms = ++expressionCount) > 4)
          error(ERROR_badArgumentCount);
        points.addElement(value);
        i = iToken;
        break;
      }
    }
    if (nAtoms < 2 && (tickInfo == null || nAtoms == 1))
      error(ERROR_badArgumentCount);
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isRange && rangeMinMax[1] < rangeMinMax[0]) {
      rangeMinMax[1] = rangeMinMax[0];
      rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE
          : -200F);
    }
    if (isSyntaxCheck)
      return;
    if (value != null || tickInfo != null) {
      if (value == null)
        tickInfo.id = "default";
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "measure",
          new MeasurementData(points, tokAction, rangeMinMax, strFormat, null,
              tickInfo, isAllConnected, isNotConnected, isAll));
      return;
    }
    switch (tokAction) {
    case Token.delete:
      viewer.deleteMeasurement(countPlusIndexes);
      break;
    case Token.on:
      viewer.showMeasurement(countPlusIndexes, true);
      break;
    case Token.off:
      viewer.showMeasurement(countPlusIndexes, false);
      break;
    default:
      viewer.toggleMeasurement(countPlusIndexes, strFormat);
    }
  }

  private void refresh() {
    if (isSyntaxCheck)
      return;
    viewer.setTainted(true);
    viewer.requestRepaintAndWait();
  }

  private void reset() throws ScriptException {
    checkLength(-2);
    if (isSyntaxCheck)
      return;
    if (statementLength == 1) {
      viewer.reset();
      return;
    }
    // possibly "all"
    switch (tokAt(1)) {
    case Token.function:
      viewer.clearFunctions();
      return;
    case Token.vanderwaals:
      viewer.setData("element_vdw", new Object[] { null, "" }, 0, 0, 0, 0, 0);
      return;
    case Token.aromatic:
      viewer.resetAromatic();
      return;
    }
    String var = parameterAsString(1);
    if (var.charAt(0) == '_')
      error(ERROR_invalidArgument);
    viewer.unsetProperty(var);
  }

  private void restrict() throws ScriptException {
    boolean isBond = (tokAt(1) == Token.bonds);
    select(isBond ? 2 : 1);
    if (isSyntaxCheck)
      return;
    restrictSelected(isBond, true);
  }

  private void restrictSelected(boolean isBond, boolean doInvert) {
    BitSet bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
    if (doInvert)
      viewer.invertSelection();
    BitSet bsSubset = viewer.getSelectionSubset();
    if (doInvert && bsSubset != null) {
      bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
      bsSelected.and(bsSubset);
      viewer.setSelectionSet(bsSelected);
      BitSetUtil.invertInPlace(bsSelected, viewer.getAtomCount());
      bsSelected.and(bsSubset);
    }
    BitSetUtil.andNot(bsSelected, viewer.getDeletedAtoms());
    boolean bondmode = viewer.getBondSelectionModeOr();

    if (!isBond)
      setBooleanProperty("bondModeOr", true);
    setShapeSize(JmolConstants.SHAPE_STICKS, 0);
    // wireframe will not operate on STRUTS even though they are
    // a form of bond order (see BondIteratoSelected)
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_STRUT));
    setShapeSize(JmolConstants.SHAPE_STICKS, 0);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
    // also need to turn off backbones, ribbons, strands, cartoons
    for (int shapeType = JmolConstants.SHAPE_MAX_SIZE_ZERO_ON_RESTRICT; --shapeType >= 0;)
      if (shapeType != JmolConstants.SHAPE_MEASURES)
        setShapeSize(shapeType, 0);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "delete", null);
    shapeManager.setLabel(null, viewer.getSelectionSet());

    if (!isBond)
      setBooleanProperty("bondModeOr", bondmode);
    viewer.setSelectionSet(bsSelected);
  }

  private void rotate(boolean isSpin, boolean isSelected)
      throws ScriptException {

    // rotate is a full replacement for spin
    // spin is DEPRECATED

    /*
     * The Chime spin method:
     * 
     * set spin x 10;set spin y 30; set spin z 10; spin | spin ON spin OFF
     * 
     * Jmol does these "first x, then y, then z" I don't know what Chime does.
     * 
     * spin and rotate are now consolidated here.
     * 
     * far simpler is
     * 
     * spin x 10 spin y 10
     * 
     * these are pure x or y spins or
     * 
     * spin axisangle {1 1 0} 10
     * 
     * this is the same as the old "spin x 10; spin y 10" -- or is it? anyway,
     * it's better!
     * 
     * note that there are many defaults
     * 
     * spin # defaults to spin y 10 spin 10 # defaults to spin y 10 spin x #
     * defaults to spin x 10
     * 
     * and several new options
     * 
     * spin -x spin axisangle {1 1 0} 10 spin 10 (atomno=1)(atomno=2) spin 20 {0
     * 0 0} {1 1 1}
     * 
     * spin MOLECULAR {0 0 0} 20
     * 
     * The MOLECULAR keyword indicates that spins or rotations are to be carried
     * out in the internal molecular coordinate frame, not the fixed room frame.
     * 
     * In the case of rotateSelected, all rotations are molecular and the
     * absense of the MOLECULAR keyword indicates to rotate about the geometric
     * center of the molecule, not {0 0 0}
     * 
     * Fractional coordinates may be indicated:
     * 
     * spin 20 {0 0 0/} {1 1 1/}
     * 
     * In association with this, TransformManager and associated functions are
     * TOTALLY REWRITTEN and consolideated. It is VERY clean now - just two
     * methods here -- one fixed and one molecular, two in Viewer, and two in
     * TransformManager. All the centering stuff has been carefully inspected
     * are reorganized as well.
     * 
     * Bob Hanson 5/21/06
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

    BitSet bsAtoms = null;
    float degreesPerSecond = Float.MIN_VALUE;
    int nPoints = 0;
    float endDegrees = Float.MAX_VALUE;
    boolean isMolecular = false;
    boolean haveRotation = false;
    Vector ptsA = null;
    Point3f[] points = new Point3f[2];
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    Vector3f translation = null;
    Matrix4f m4 = null;
    int direction = 1;
    int tok;
    Quaternion q = null;
    boolean helicalPath = false;
    Vector ptsB = null;
    BitSet bsCompare = null;
    Point3f invPoint = null;
    Point4f invPlane = null;
    boolean axesOrientationRasmol = viewer.getAxesOrientationRasmol();
    for (int i = 1; i < statementLength; ++i) {
      switch (tok = getToken(i).tok) {
      case Token.spin:
        isSpin = true;
        continue;
      case Token.internal:
      case Token.molecular:
        isMolecular = true;
        continue;
      case Token.selected:
        isSelected = true;
        break;
      case Token.comma:
        continue;
      case Token.integer:
      case Token.decimal:
        if (endDegrees == Float.MAX_VALUE) {
          endDegrees = floatParameter(i);
        } else {
          degreesPerSecond = floatParameter(i);
          isSpin = (degreesPerSecond != 0);
        }
        continue;
      case Token.minus:
        direction = -1;
        continue;
      case Token.x:
        haveRotation = true;
        rotAxis.set(direction, 0, 0);
        continue;
      case Token.y:
        haveRotation = true;
        rotAxis.set(0, (axesOrientationRasmol && !isMolecular ? -direction
            : direction), 0);
        continue;
      case Token.z:
        haveRotation = true;
        rotAxis.set(0, 0, direction);
        continue;
      case Token.symop:
        int symop = intParameter(++i);
        if (isSyntaxCheck)
          continue;
        Hashtable info = viewer.getSpaceGroupInfo(null);
        Object[] op = (info == null ? null : (Object[]) info.get("operations"));
        if (symop == 0 || op == null || op.length < Math.abs(symop))
          error(ERROR_invalidArgument);
        op = (Object[]) op[Math.abs(symop) - 1];
        translation = (Vector3f) op[5];
        invPoint = (Point3f) op[6];
        points[0] = (Point3f) op[7];
        rotAxis = (Vector3f) op[8];
        endDegrees = ((Integer) op[9]).intValue();
        if (symop < 0) {
          endDegrees = -endDegrees;
          if (translation != null)
            translation.scale(-1);
        }
        if (endDegrees == 0 && points[0] != null) {
          // glide plane
          invPlane = Measure.getPlaneThroughPoint(points[0], rotAxis);
        }
        q = new Quaternion(rotAxis, endDegrees);
        nPoints = (points[0] == null ? 0 : 1);
        isMolecular = true;
        haveRotation = true;
        continue;
      case Token.helix:
        // screw motion, for compare
        helicalPath = true;
        continue;
      case Token.translate:
        translation = new Vector3f(centerParameter(++i));
        isMolecular = isSelected = true;
        break;
      case Token.branch:
        haveRotation = true;
        int iAtom1 = expression(++i).nextSetBit(0);
        int iAtom2 = expression(++iToken).nextSetBit(0);
        if (iAtom1 < 0 || iAtom2 < 0)
          return;
        bsAtoms = viewer.getBranchBitSet(iAtom2, iAtom1);
        isSelected = true;
        isMolecular = true;
        points[0] = viewer.getAtomPoint3f(iAtom1);
        points[1] = viewer.getAtomPoint3f(iAtom2);
        nPoints = 2;
        break;
      case Token.compare:
      case Token.matrix4f:
      case Token.matrix3f:
        haveRotation = true;
        if (tok == Token.compare) {
          bsCompare = expression(++i);
          ptsA = viewer.getAtomPointVector(bsCompare);
          if (ptsA == null)
            error(ERROR_invalidArgument, i);
          i = iToken;
          ptsB = getPointVector(getToken(++i).value, i);
          if (ptsB == null || ptsA.size() != ptsB.size())
            error(ERROR_invalidArgument, i);
          m4 = new Matrix4f();
          points[0] = new Point3f();
          nPoints = 1;
          float stddev = (isSyntaxCheck ? 0 : Measure.getTransformMatrix4(ptsA,
              ptsB, m4, points[0]));
          // if the standard deviation is very small, we leave ptsB
          // because it will be used to set the absolute final positions
          if (stddev > 0.001)
            ptsB = null;
        } else if (tok == Token.matrix4f) {
          m4 = (Matrix4f) theToken.value;
        }
        Matrix3f m3 = new Matrix3f();
        if (m4 != null) {
          translation = new Vector3f();
          m4.get(translation);
          m4.get(m3);
        } else {
          m3 = (Matrix3f) theToken.value;
        }
        q = (isSyntaxCheck ? new Quaternion() : new Quaternion(m3));
        rotAxis.set(q.getNormal());
        endDegrees = q.getTheta();
        isMolecular = true;
        break;
      case Token.quaternion:
      case Token.point4f:
        if (tok == Token.quaternion)
          i++;
        haveRotation = true;
        if (tokAt(i) == Token.list){
          String[] s = (String[])getToken(i).value;
          if (s.length == 0)
            error(ERROR_invalidArgument);
          Object o = Escape.unescapePoint(s[0]);
          if (!(o instanceof Point4f))
            error(ERROR_invalidArgument);
          q = new Quaternion((Point4f)o);
        } else {
          q = new Quaternion(getPoint4f(i));
        }
        rotAxis.set(q.getNormal());
        endDegrees = q.getTheta();
        break;
      case Token.axisangle:
        haveRotation = true;
        if (isPoint3f(++i)) {
          rotAxis.set(centerParameter(i));
          break;
        }
        Point4f p4 = getPoint4f(i);
        rotAxis.set(p4.x, p4.y, p4.z);
        endDegrees = p4.w;
        q = new Quaternion(rotAxis, endDegrees);
        break;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
      case Token.dollarsign:
        if (tok == Token.bitset || tok == Token.expressionBegin) {
          if (translation != null || q != null || nPoints == 2) {
            bsAtoms = expression(i);
            ptsB = null;
            isSelected = true;
            break;
          }
        }
        haveRotation = true;
        if (nPoints == 2)
          nPoints = 0;
        // {X, Y, Z}
        // $drawObject[n]
        Point3f pt1 = centerParameter(i, viewer.getCurrentModelIndex());
        if (!isSyntaxCheck && tok == Token.dollarsign
            && tokAt(i + 2) != Token.leftsquare) {
          // rotation about an axis such as $line1
          isMolecular = true;
          rotAxis = getDrawObjectAxis(objectNameParameter(++i), viewer
              .getCurrentModelIndex());
        }
        points[nPoints++] = pt1;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      i = iToken;
    }
    if (isSyntaxCheck)
      return;
    if (isSelected && bsAtoms == null)
      bsAtoms = viewer.getSelectionSet();
    if (bsCompare != null) {
      isSelected = true;
      if (bsAtoms == null)
        bsAtoms = bsCompare;
    }
    float rate = (degreesPerSecond == Float.MIN_VALUE ? 10
        : degreesPerSecond < 0 ?
        // -n means number of seconds, not degreesPerSecond
        -endDegrees / degreesPerSecond
            : degreesPerSecond);

    if (q != null) {
      if (nPoints == 0)
        points[0] = viewer.getAtomSetCenter(bsAtoms != null ? bsAtoms
            : isSelected ? viewer.getSelectionSet() : viewer
                .getModelAtomBitSet(null));
      if (helicalPath) {
        points[1] = new Point3f(points[0]);
        points[1].add(translation);
        Object[] ret = (Object[]) Measure.computeHelicalAxis(null, Token.array,
            points[0], points[1], q);
        points[0] = (Point3f) ret[0];
        float theta = ((Point3f) ret[3]).x;
        if (theta != 0) {
          translation = (Vector3f) ret[1];
          rotAxis = new Vector3f(translation);
          if (theta < 0)
            rotAxis.scale(-1);
        }
        m4 = null;
      }
      if (isSpin && m4 == null)
        m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
      nPoints = 1;
    }
    if (invPoint != null) {
      viewer.invertAtomCoord(invPoint, bsAtoms);
      if (rotAxis == null)
        return;
    }
    if (invPlane != null) {
      viewer.invertAtomCoord(invPlane, bsAtoms);
    }
    if (nPoints < 2) {
      if (!isMolecular) {
        // fixed-frame rotation
        // rotate x 10 # Chime-like
        // rotate axisangle {0 1 0} 10
        // rotate x 10 (atoms) # point-centered
        // rotate x 10 $object # point-centered
        viewer.rotateAxisAngleAtCenter(points[0], rotAxis, rate, endDegrees,
            isSpin, bsAtoms);
        return;
      }
      if (nPoints == 0)
        points[0] = new Point3f();
      // rotate MOLECULAR
      // rotate MOLECULAR (atom1)
      // rotate MOLECULAR x 10 (atom1)
      // rotate axisangle MOLECULAR (atom1)
      points[1] = new Point3f(points[0]);
      points[1].add(rotAxis);
      nPoints = 2;
    }
    if (nPoints == 0)
      points[0] = new Point3f();
    if (nPoints < 2 || points[0].distance(points[1]) == 0) {
      points[1] = new Point3f(points[0]);
      points[1].y += 1.0;
    }
    if (endDegrees == Float.MAX_VALUE)
      endDegrees = 0;
    if (endDegrees != 0 && translation != null && !haveRotation)
      translation.scale(endDegrees / translation.length());
    if (isSpin && translation != null
        && (endDegrees == 0 || degreesPerSecond == 0)) {
      // need a token rotation
      endDegrees = 0.01f;
      rate = (degreesPerSecond == Float.MIN_VALUE ? 0.01f
          : degreesPerSecond < 0 ?
          // -n means number of seconds, not degreesPerSecond
          -endDegrees / degreesPerSecond
              : degreesPerSecond * 0.01f / translation.length());
      degreesPerSecond = 0.01f;
    }
    if (bsAtoms != null && isSpin && ptsB == null && m4 != null) {
      ptsA = viewer.getAtomPointVector(bsAtoms);
      ptsB = Measure.transformPoints(ptsA, m4, points[0]);
    }
    if (bsAtoms != null && !isSpin && ptsB != null)
      viewer.setAtomCoord(bsAtoms, Token.xyz, ptsB);
    else
      viewer.rotateAboutPointsInternal(points[0], points[1], rate, endDegrees,
          isSpin, bsAtoms, translation, ptsB);
  }

  Vector getPointVector(Object value, int i) throws ScriptException {
    if (value instanceof BitSet)
      return viewer.getAtomPointVector((BitSet) value);
    if (value instanceof String[])
      return Escape.unescapePointVector((String[]) value);
    if (i > 0)
      return viewer.getAtomPointVector(expression(i));
    return null;
  }

  private Point3f getObjectCenter(String axisID, int index, int modelIndex) {
    Object[] data = new Object[] { axisID, new Integer(index),
        new Integer(modelIndex) };
    return (getShapeProperty(JmolConstants.SHAPE_DRAW, "getCenter", data)
        || getShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "getCenter",
            data) ? (Point3f) data[2] : null);
  }

  private Point3f[] getObjectBoundingBox(String id) {
    Object[] data = new Object[] { id, null, null };
    return (getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
        "getBoundingBox", data) ? (Point3f[]) data[2] : null);
  }

  private Vector3f getDrawObjectAxis(String axisID, int index) {
    Object[] data = new Object[] { axisID, new Integer(index), null };
    return (getShapeProperty(JmolConstants.SHAPE_DRAW, "getSpinAxis",
        data) ? (Vector3f) data[2] : null);
  }

  private void script(int tok, String filename) throws ScriptException {
    boolean loadCheck = true;
    boolean isCheck = false;
    boolean doStep = false;
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
    String theScript = parameterAsString(1);
    String localPath = null;
    String remotePath = null;
    String scriptPath = null;
    if (tok == Token.javascript) {
      checkLength(2);
      if (!isSyntaxCheck)
        viewer.jsEval(theScript);
      return;
    }
    if (filename == null) {
      tok = tokAt(1);
      if (tok != Token.string)
        error(ERROR_filenameExpected);
      filename = parameterAsString(1);
      if (filename.equalsIgnoreCase("applet")) {
        // script APPLET x "....."
        String appID = parameterAsString(2);
        theScript = parameterExpression(3, 0, "_script", false).toString();
        checkLast(iToken);
        if (isSyntaxCheck)
          return;
        if (appID.length() == 0 || appID.equals("all"))
          appID = "*";
        if (!appID.equals(".")) {
          viewer.jsEval(appID + "\1" + theScript);
          if (!appID.equals("*"))
            return;
        }
      } else {
        theScript = null;
        tok = tokAt(statementLength - 1);
        doStep = (tok == Token.step);
        if (filename.equalsIgnoreCase("inline")) {
          theScript = parameterExpression(2,
              (doStep ? statementLength - 1 : 0), "_script", false).toString();
          i = iToken + 1;
        }
        while (filename.equalsIgnoreCase("localPath")
            || filename.equalsIgnoreCase("remotePath")
            || filename.equalsIgnoreCase("scriptPath")) {
          if (filename.equalsIgnoreCase("localPath"))
            localPath = parameterAsString(i++);
          else if (filename.equalsIgnoreCase("scriptPath"))
            scriptPath = parameterAsString(i++);
          else
            remotePath = parameterAsString(i++);
          filename = parameterAsString(i++);
        }
        if ((tok = tokAt(i)) == Token.check) {
          isCheck = true;
          tok = tokAt(++i);
        }
        if (tok == Token.noload) {
          loadCheck = false;
          tok = tokAt(++i);
        }
        if (tok == Token.line || tok == Token.lines) {
          i++;
          lineEnd = lineNumber = Math.max(intParameter(i++), 0);
          if (checkToken(i)) {
            if (getToken(i).tok == Token.minus)
              lineEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              lineEnd = -intParameter(i++);
            if (lineEnd <= 0)
              error(ERROR_invalidArgument);
          }
        } else if (tok == Token.command || tok == Token.commands) {
          i++;
          pc = Math.max(intParameter(i++) - 1, 0);
          pcEnd = pc + 1;
          if (checkToken(i)) {
            if (getToken(i).tok == Token.minus)
              pcEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              pcEnd = -intParameter(i++);
            if (pcEnd <= 0)
              error(ERROR_invalidArgument);
          }
        }
        checkLength(doStep ? i + 1 : i);
      }
    }
    
    // processing
    
    if (isSyntaxCheck && !isCmdLine_c_or_C_Option)
      return;
    if (isCmdLine_c_or_C_Option)
      isCheck = true;
    boolean wasSyntaxCheck = isSyntaxCheck;
    boolean wasScriptCheck = isCmdLine_c_or_C_Option;
    if (isCheck)
      isSyntaxCheck = isCmdLine_c_or_C_Option = true;
    pushContext(null);
    contextPath += " >> " + filename;
    if (theScript == null ? compileScriptFileInternal(filename, localPath,
        remotePath, scriptPath) : compileScript(null, theScript, false)) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < lineNumbers.length && lineNumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = isCmdLine_C_Option;
      isCmdLine_C_Option &= loadCheck;
      executionStepping |= doStep;
      instructionDispatchLoop(isCheck);
      if (debugScript && viewer.getMessageStyleChime())
        viewer.scriptStatus("script <exiting>");
      isCmdLine_C_Option = saveLoadCheck;
      popContext(false);
    } else {
      Logger.error(GT._("script ERROR: ") + errorMessage);
      popContext(false);
      if (wasScriptCheck) {
        setErrorMessage(null);
      } else {
        evalError(null, null);
      }
    }

    isSyntaxCheck = wasSyntaxCheck;
    isCmdLine_c_or_C_Option = wasScriptCheck;
  }

  private void function() throws ScriptException {
    if (isSyntaxCheck && !isCmdLine_c_or_C_Option)
      return;
    String name = (String) getToken(0).value;
    if (!viewer.isFunction(name))
      error(ERROR_commandExpected);
    Vector params = (statementLength == 1 || statementLength == 3
        && tokAt(1) == Token.leftparen && tokAt(2) == Token.rightparen ? null
        : (Vector) parameterExpression(1, 0, null, true));
    if (isSyntaxCheck)
      return;
    runFunction(name, params, null, false);
  }

  private void sync() throws ScriptException {
    // new 11.3.9
    checkLength(-3);
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
      text = (tokAt(2) == Token.stereo ? Viewer.SYNC_GRAPHICS_MESSAGE
          : parameterAsString(2));
      break;
    }
    if (isSyntaxCheck)
      return;
    viewer.syncScript(text, applet);
  }

  private void history(int pt) throws ScriptException {
    // history or set history
    if (statementLength == 1) {
      // show it
      showString(viewer.getSetHistory(Integer.MAX_VALUE));
      return;
    }
    if (pt == 2) {
      // set history n; n' = -2 - n; if n=0, then set history OFF
      int n = intParameter(checkLast(2));
      if (n < 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        viewer.getSetHistory(n == 0 ? 0 : -2 - n);
      return;
    }
    switch (getToken(checkLast(1)).tok) {
    // pt = 1 history ON/OFF/CLEAR
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
      error(ERROR_keywordExpected, "ON, OFF, CLEAR");
    }
  }

  private void display(boolean isDisplay) throws ScriptException {
    if (tokAt(1) == Token.dollarsign) {
      setObjectProperty();
      return;
    }
    BitSet bs = (statementLength == 1 ? null : expression(1));
    if (isSyntaxCheck)
      return;
    if (isDisplay)
      viewer.display(bs, tQuiet);
    else
      viewer.hide(bs, tQuiet);
  }

  private void delete() throws ScriptException {
    if (statementLength == 1) {
      zap(true);
      return;
    }
    if (tokAt(1) == Token.dollarsign) {
      setObjectProperty();
      return;
    }
    BitSet bs = expression(statement, 1, 0, true, false, true, false);
    if (isSyntaxCheck)
      return;
    int nDeleted = viewer.deleteAtoms(bs, false);
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT._("{0} atoms deleted", nDeleted));
  }

  private void minimize() throws ScriptException {
    BitSet bsSelected = null;
    int steps = Integer.MAX_VALUE;
    float crit = 0;
    boolean addHydrogen = false;
    boolean isSilent = false;
    MinimizerInterface minimizer = viewer.getMinimizer(false);
    // may be null
    for (int i = 1; i < statementLength; i++)
      switch (getToken(i).tok) {
      case Token.clear:
        checkLength(2);
        if (isSyntaxCheck || minimizer == null)
          return;
        minimizer.setProperty("clear", null);
        return;
      case Token.constraint:
        if (i != 1)
          error(ERROR_invalidArgument);
        int n = 0;
        i++;
        float targetValue = 0;
        int[] aList = new int[5];
        if (tokAt(i) == Token.clear) {
          checkLength(2);
        } else {
          while (n < 4 && !isFloatParameter(i)) {
            aList[++n] = expression(i).nextSetBit(0);
            i = iToken + 1;
          }
          aList[0] = n;
          if (n == 1)
            error(ERROR_invalidArgument);
          targetValue = floatParameter(checkLast(i));
        }
        if (!isSyntaxCheck)
          viewer.getMinimizer(true).setProperty("constraint",
              new Object[] { aList, new int[n], new Float(targetValue) });
        return;
      case Token.silent:
        isSilent = true;
        break;
      case Token.stop:
      case Token.cancel:
        checkLength(2);
        if (isSyntaxCheck || minimizer == null)
          return;
        minimizer.setProperty(parameterAsString(i), null);
        return;
      case Token.fix:
      case Token.fixed:
        if (i != 1)
          error(ERROR_invalidArgument);
        BitSet bsFixed = expression(++i);
        if (bsFixed.nextSetBit(0) < 0)
          bsFixed = null;
        checkLength(iToken + 1, 1);
        if (!isSyntaxCheck)
          viewer.getMinimizer(true).setProperty("fixed", bsFixed);
        return;
      case Token.energy:
        steps = 0;
        continue;
      case Token.addhydrogens:
        addHydrogen = true;
        continue;
      case Token.step:
      case Token.steps:
        steps = intParameter(++i);
        continue;
      case Token.criterion:
        crit = floatParameter(++i);
        continue;
      case Token.select:
        bsSelected = expression(++i);
        i = iToken;
        continue;
      default:
        error(ERROR_invalidArgument);
        break;
      }
    if (!isSyntaxCheck)
      viewer.minimize(steps, crit, bsSelected, addHydrogen, isSilent);
  }

  private void select(int i) throws ScriptException {
    // NOTE this is called by restrict()
    if (statementLength == 1) {
      viewer.select(null, tQuiet || scriptLevel > scriptReportingLevel);
      return;
    }
    if (statementLength == 2 && tokAt(1) == Token.only)
      return; // coming from "cartoon only"
    // select beginexpr none endexpr
    viewer.setNoneSelected(statementLength == 4 && tokAt(2) == Token.none);
    // select beginexpr bonds ( {...} ) endex pr
    if (tokAt(2) == Token.bitset && getToken(2).value instanceof BondSet
        || getToken(2).tok == Token.bonds && getToken(3).tok == Token.bitset) {
      if (statementLength == iToken + 2) {
        if (!isSyntaxCheck)
          viewer.selectBonds((BitSet) theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    if (getToken(2).tok == Token.measure) {
      if (statementLength == 5 && getToken(3).tok == Token.bitset) {
        if (!isSyntaxCheck)
          setShapeProperty(JmolConstants.SHAPE_MEASURES, "select",
              theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    BitSet bs = null;
    if (getToken(1).intValue == 0) {
      Object v = tokenSetting(0).value;
      if (!(v instanceof BitSet))
        error(ERROR_invalidArgument);
      checkLast(iToken);
      bs = (BitSet) v;
    } else {
      bs = expression(i);
    }
    if (isSyntaxCheck)
      return;
    if (isBondSet) {
      viewer.selectBonds(bs);
    } else {
      if (bs.length() > viewer.getAtomCount()) {
        bs = BitSetUtil.copy(bs);
        bs.and(viewer.getModelAtomBitSet(-1, false));
      }
      viewer.select(bs, tQuiet || scriptLevel > scriptReportingLevel);
    }
  }

  private void subset() throws ScriptException {
    BitSet bs = (statementLength == 1 ? null : expression(-1));
    if (isSyntaxCheck)
      return;
    // There might have been a reason to have bsSubset being set BEFORE
    // checking syntax checking, but I can't remember why.
    // will leave it this way for now. Might cause some problems with script
    // checking.
    viewer.setSelectionSubset(bs);
    // I guess we do not want to select, because that could
    // throw off picking in a strange way
    // viewer.select(bsSubset, false);
  }

  private void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    // invertSelected STEREO {sp3Atom} {one or two groups)
    Point3f pt = null;
    Point4f plane = null;
    BitSet bs = null;
    int iAtom = Integer.MIN_VALUE;
    switch (tokAt(1)) {
    case Token.nada:
      if (isSyntaxCheck)
        return;
      bs = viewer.getSelectionSet();
      pt = viewer.getAtomSetCenter(bs);
      viewer.invertAtomCoord(pt, bs);
      return;
    case Token.stereo:
      iAtom = expression(2).nextSetBit(0);
      // and only these:
      bs = expression(iToken + 1);
      break;
    case Token.point:
      pt = centerParameter(2);
      break;
    case Token.plane:
      plane = planeParameter(2);
      break;
    case Token.hkl:
      plane = hklParameter(2);
      break;
    }
    checkLength(iToken + 1, 1);
    if (plane == null && pt == null && iAtom == Integer.MIN_VALUE)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    if (iAtom == -1)
      return;
    viewer.invertSelected(pt, plane, iAtom, bs);
  }

  private void translate(boolean isSelected) throws ScriptException {
    // translate [selected] X|Y|Z x.x [NM|ANGSTROMS]
    // translate [selected] X|Y x.x%
    // translate [selected] X|Y|Z x.x [NM|ANGSTROMS]
    // translate [selected] X|Y x.x%
    // translate {x y z} [{atomExpression}]
    BitSet bs = null;
    int i = 1;
    if (tokAt(1) == Token.selected) {
      isSelected = true;
      i++;
    }
    if (isPoint3f(i)) {
      Point3f pt = getPoint3f(i, true);
      bs = (!isSelected && iToken + 1 < statementLength ? expression(++iToken)
          : null);
      checkLast(iToken);
      if (!isSyntaxCheck)
        viewer.setAtomCoordRelative(pt, bs);
      return;
    }
    int xyz = getToken(i).tok;
    if (xyz != Token.x && xyz != Token.y && xyz != Token.z)
      error(ERROR_axisExpected);
    float amount = floatParameter(++i);
    if (amount == 0)
      return;
    char type;
    switch (tokAt(++i)) {
    case Token.nada:
    case Token.bitset:
    case Token.expressionBegin:
      type = '\0';
      break;
    default:
      type = (optParameterAsString(i).toLowerCase() + '\0').charAt(0);
    }
    iToken = (type == '\0' ? 2 : 3);
    bs = (isSelected ? viewer.getSelectionSet()
        : iToken + 1 < statementLength ? expression(++iToken) : null);
    checkLast(iToken);
    if (!isSyntaxCheck)
      viewer.translate(parameterAsString(1).charAt(0), amount, type, bs);
  }

  private void zap(boolean isZapCommand) throws ScriptException {
    if (statementLength == 1 || !isZapCommand) {
      viewer.zap(true, isZapCommand && !isStateScript);
      refresh();
      return;
    }
    BitSet bs = expression(1);
    if (isSyntaxCheck)
      return;
    int nDeleted = viewer.deleteAtoms(bs, true);
    boolean isQuiet = (tQuiet || scriptLevel > scriptReportingLevel);
    if (!isQuiet)
      scriptStatusOrBuffer(GT._("{0} atoms deleted", nDeleted));
    viewer.select(null, isQuiet);
  }

  private void zoom(boolean isZoomTo) throws ScriptException {
    if (!isZoomTo) {
      // zoom
      // zoom on|off
      int tok = (statementLength > 1 ? getToken(1).tok : Token.on);
      switch (tok) {
      case Token.in:
      case Token.out:
        break;
      case Token.on:
      case Token.off:
        if (statementLength > 2)
          error(ERROR_badArgumentCount);
        if (!isSyntaxCheck)
          setBooleanProperty("zoomEnabled", tok == Token.on);
        return;
      }
    }
    Point3f center = null;
    Point3f currentCenter = viewer.getRotationCenter();
    int i = 1;
    // zoomTo time-sec
    float time = (isZoomTo ? (isFloatParameter(i) ? floatParameter(i++) : 2f)
        : 0f);
    if (time < 0) {
      // zoom -10
      i--;
      time = 0;
    }
    // zoom {x y z} or (atomno=3)
    int ptCenter = 0;
    BitSet bsCenter = null;
    if (isCenterParameter(i)) {
      ptCenter = i;
      center = centerParameter(i);
      if (expressionResult instanceof BitSet)
        bsCenter = (BitSet) expressionResult;
      i = iToken + 1;
    }

    // disabled sameAtom stuff -- just too weird
    boolean isSameAtom = false && (center != null && currentCenter
        .distance(center) < 0.1);
    // zoom/zoomTo [0|n|+n|-n|*n|/n|IN|OUT]
    // zoom/zoomTo percent|-factor|+factor|*factor|/factor | 0
    float zoom = viewer.getZoomSetting();
    float newZoom = getZoom(i, bsCenter, zoom);
    i = iToken + 1;
    float xTrans = Float.NaN;
    float yTrans = Float.NaN;
    if (i != statementLength) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
    }
    if (i != statementLength)
      error(ERROR_invalidArgument);
    if (newZoom < 0) {
      newZoom = -newZoom; // currentFactor
      if (isZoomTo) {
        // no factor -- check for no center (zoom out) or same center (zoom in)
        if (statementLength == 1 || isSameAtom)
          newZoom *= 2;
        else if (center == null)
          newZoom /= 2;
      }
    }
    float max = viewer.getMaxZoomPercent();
    if (newZoom < 5 || newZoom > max)
      numberOutOfRange(5, max);
    if (!viewer.isWindowCentered()) {
      // do a smooth zoom only if not windowCentered
      if (center != null) {
        BitSet bs = expression(ptCenter);
        if (!isSyntaxCheck)
          viewer.setCenterBitSet(bs, false);
      }
      center = viewer.getRotationCenter();
      if (Float.isNaN(xTrans))
        xTrans = viewer.getTranslationXPercent();
      if (Float.isNaN(yTrans))
        yTrans = viewer.getTranslationYPercent();
    }
    if (isSyntaxCheck)
      return;
    if (Float.isNaN(xTrans))
      xTrans = 0;
    if (Float.isNaN(yTrans))
      yTrans = 0;
    if (isSameAtom && Math.abs(zoom - newZoom) < 1)
      time = 0;
    viewer.moveTo(time, center, JmolConstants.center, Float.NaN, null, newZoom,
        xTrans, yTrans, Float.NaN, null, Float.NaN, Float.NaN, Float.NaN);
  }

  private float getZoom(int i, BitSet bs, float currentZoom)
      throws ScriptException {
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]

    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    if (zoom == 0 || currentZoom == 0) {
      // moveTo/zoom/zoomTo {center} 0
      if (bs == null)
        error(ERROR_invalidArgument);
      float r = viewer.calcRotationRadius(bs);
      currentZoom = viewer.getRotationRadius() / r * 100;
      zoom = Float.NaN;
    }
    if (zoom < 0) {
      // moveTo/zoom/zoomTo -factor
      zoom += currentZoom;
    } else if (Float.isNaN(zoom)) {
      // moveTo/zoom/zoomTo [optional {center}] percent|+factor|*factor|/factor
      // moveTo/zoom/zoomTo {center} 0 [optional
      // -factor|+factor|*factor|/factor]
      int tok = tokAt(i);
      switch (tok) {
      case Token.out:
      case Token.in:
        zoom = currentZoom * (tok == Token.out ? 0.5f : 2f);
        i++;
        break;
      case Token.divide:
      case Token.times:
      case Token.plus:
        float value = floatParameter(++i);
        i++;
        switch (tok) {
        case Token.divide:
          zoom = currentZoom / value;
          break;
        case Token.times:
          zoom = currentZoom * value;
          break;
        case Token.plus:
          zoom = currentZoom + value;
          break;
        }
        break;
      default:
        // indicate no factor indicated
        zoom = (bs == null ? -currentZoom : currentZoom);
      }
    }
    iToken = i - 1;
    return zoom;
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
      error(ERROR_numberExpected);
    }
    if (!isSyntaxCheck)
      delay(millis);
  }

  private void delay(long millis) {
    long timeBegin = System.currentTimeMillis();
    refresh();
    int delayMax;
    if (millis < 0)
      millis = -millis;
    else if ((delayMax = viewer.getDelayMaximum()) > 0 && millis > delayMax)
      millis = delayMax;
    millis -= System.currentTimeMillis() - timeBegin;
    int seconds = (int) millis / 1000;
    millis -= seconds * 1000;
    if (millis <= 0)
      millis = 1;
    while (seconds >= 0 && millis > 0 && !interruptExecution
        && currentThread == Thread.currentThread()) {
      viewer.popHoldRepaint("delay");
      try {
        Thread.sleep((seconds--) > 0 ? 1000 : millis);
      } catch (InterruptedException e) {
      }
      viewer.pushHoldRepaint("delay");
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
        int percent = intParameter(checkLast(1));
        if (!isSyntaxCheck)
          if (isDepth)
            viewer.depthToPercent(percent);
          else
            viewer.slabToPercent(percent);
        return;
      case Token.on:
        checkLength(2);
        TF = true;
        // fall through
      case Token.off:
        checkLength(2);
        setBooleanProperty("slabEnabled", TF);
        return;
      case Token.reset:
        checkLength(2);
        if (isSyntaxCheck)
          return;
        viewer.slabReset();
        setBooleanProperty("slabEnabled", true);
        return;
      case Token.set:
        checkLength(2);
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
          error(ERROR_invalidArgument);
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
      case Token.hkl:
        plane = (getToken(2).tok == Token.none ? null : hklParameter(2));
        break;
      case Token.reference:
        // only in 11.2; deprecated
        return;
      default:
        error(ERROR_invalidArgument);
      }
    if (!isSyntaxCheck)
      viewer.slabInternal(plane, isDepth);
  }

  private void ellipsoid() throws ScriptException {
    int mad = 0;
    int i = 1;
    switch (getToken(1).tok) {
    case Token.on:
      mad = 50;
      break;
    case Token.off:
      break;
    case Token.integer:
      mad = intParameter(1);
      break;
    case Token.id:
    case Token.times:
    case Token.identifier:
      loadShape(JmolConstants.SHAPE_ELLIPSOIDS);
      if (theTok == Token.id)
        i++;
      setShapeId(JmolConstants.SHAPE_ELLIPSOIDS, i, false);
      i = iToken;
      while (++i < statementLength) {
        String key = parameterAsString(i);
        Object value = null;
        switch (tokAt(i)) {
        case Token.axes:
          Vector3f[] axes = new Vector3f[3];
          for (int j = 0; j < 3; j++) {
            axes[j] = new Vector3f();
            axes[j].set(centerParameter(++i));
            i = iToken;
          }
          value = axes;
          break;
        case Token.center:
          value = centerParameter(++i);
          i = iToken;
          break;
        case Token.color:
          float translucentLevel = Float.NaN;
          i++;
          if ((theTok = tokAt(i)) == Token.translucent) {
            value = "translucent";
            if (isFloatParameter(++i))
              translucentLevel = getTranslucentLevel(i++);
            else
              translucentLevel = viewer.getDefaultTranslucent();
          } else if (theTok == Token.opaque) {
            value = "opaque";
            i++;
          }
          if (isColorParam(i)) {
            setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, "color",
                new Integer(getArgbParam(i)));
            i = iToken;
          }
          if (value == null)
            continue;
          if (!Float.isNaN(translucentLevel))
            setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS,
                "translucentLevel", new Float(translucentLevel));
          key = "translucency";
          break;
        case Token.delete:
          value = Boolean.TRUE;
          checkLength(3);
          break;
        case Token.modelindex:
          value = new Integer(intParameter(++i));
          break;
        case Token.on:
          value = Boolean.TRUE;
          break;
        case Token.off:
          key = "on";
          value = Boolean.FALSE;
          break;
        case Token.scale:
          value = new Float(floatParameter(++i));
          break;
        }
        if (value == null)
          error(ERROR_invalidArgument);
        setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, key.toLowerCase(),
            value);
      }
      setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, "thisID", null);
      return;
    default:
      error(ERROR_invalidArgument);
    }
    setShapeSize(JmolConstants.SHAPE_ELLIPSOIDS, mad);
  }

  private String getShapeNameParameter(int i) throws ScriptException {
    String id = parameterAsString(i);
    boolean isWild = id.equals("*");
    if (id.length() == 0)
      error(ERROR_invalidArgument);
    if (isWild) {
      switch (tokAt(i + 1)) {
      case Token.nada:
      case Token.on:
      case Token.off:
      case Token.displayed:
      case Token.hidden:
      case Token.color:
      case Token.delete:
        break;
      default:
        id += optParameterAsString(++i);
      }
    }
    if (tokAt(i + 1) == Token.times)
      id += parameterAsString(++i);
    iToken = i;
    return id;
  }

  private String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
    if (idSeen)
      error(ERROR_invalidArgument);
    String name = getShapeNameParameter(i).toLowerCase();
    setShapeProperty(iShape, "thisID", name);
    return name;
  }

  private void setAtomShapeSize(int shape, float scale) throws ScriptException {
    // halo star spacefill
    RadiusData rd = null;
    int tok = tokAt(1);
    boolean isOnly = false;
    switch (tok) {
    case Token.only:
      restrictSelected(false, false);
      break;
    case Token.on:
      break;
    case Token.off:
      scale = 0;
      break;
    case Token.decimal:
      isOnly = (floatParameter(1) < 0);
      // fall through
    case Token.integer:
    default:
      rd = encodeRadiusParameter(1, isOnly);
      if (Float.isNaN(rd.value))
        error(ERROR_invalidArgument);
    }
    if (rd == null)
      rd = new RadiusData(scale, RadiusData.TYPE_FACTOR, JmolConstants.VDW_AUTO);
    if (isOnly)
      restrictSelected(false, false);
    setShapeSize(shape, rd);
  }

  /*
   * Based on the form of the parameters, returns and encoded radius as follows:
   * 
   * script meaning range encoded
   * 
   * +1.2 offset [0 - 10] -1.2 offset 0) 1.2 absolute (0 - 10] -30% 70% (-100 -
   * 0) +30% 130% (0 80% percent (0
   */

  private RadiusData encodeRadiusParameter(int index, boolean isOnly) throws ScriptException {

    float value = Float.NaN;
    int type = RadiusData.TYPE_ABSOLUTE;
    int vdwType = 0;

    int tok = getToken(index).tok;
    switch (tok) {
    case Token.adpmax:
    case Token.adpmin:
    case Token.ionic:
    case Token.temperature:
    case Token.vanderwaals:
      value = 1;
      type = RadiusData.TYPE_FACTOR;
      vdwType = (tok == Token.vanderwaals ? 0 : tok);
      tok = tokAt(++index);
      break;
    }

    switch (tok) {
    case Token.auto:
    case Token.rasmol:
    case Token.babel:
    case Token.babel21:
    case Token.jmol:
      value = 1;
      type = RadiusData.TYPE_FACTOR;
      --index;
      break;
    case Token.plus:
    case Token.decimal:
      if (tok == Token.plus) {
        index++;
        type = RadiusData.TYPE_OFFSET;
      } else {
        type = RadiusData.TYPE_ABSOLUTE;
        vdwType = Integer.MAX_VALUE;
      }
      value = floatParameter(index, (isOnly ? -Atom.RADIUS_MAX : 0), Atom.RADIUS_MAX);
      if (isOnly)
        value = -value;
      break;
    case Token.integer:
      value = intParameter(index);
      if (tokAt(index + 1) == Token.percent) {
        index++;
        type = RadiusData.TYPE_FACTOR;
        if (value < 0 || value > 200)
          integerOutOfRange(0, 200);
        value /= 100;
        break;
      }
      // rasmol 250-scale if positive or percent (again), if negative
      // (deprecated)
      if (value > 749 || value < -200)
        integerOutOfRange(-200, 749);
      if (value > 0) {
        value /= 250;
        type = RadiusData.TYPE_ABSOLUTE;
      } else {
        value /= -100;
        type = RadiusData.TYPE_FACTOR;
      }
      break;
    default:
      if (value == 1)
        index--;
    }
    if (vdwType == 0) {
      iToken = index;
      vdwType = JmolConstants.getVdwType(optParameterAsString(++index));
      if (vdwType < 0) {
        vdwType = JmolConstants.getVdwType("auto");
      } else {
        iToken = index;
      }
    }
    return new RadiusData(value, type, vdwType);
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
      error(ERROR_invalidArgument);
    switch (tokAt(2)) {
    case Token.bitset:
    case Token.expressionBegin:
      bs = expression(2);
      checkLast(iToken);
      break;
    default:
      checkLength(2);
    }
    if (isSyntaxCheck)
      return;
    clearDefinedVariableAtomSets();
    viewer.setProteinType(iType, bs);
  }

  private void wireframe() throws ScriptException {
    int mad = getMadParameter();
    if (isSyntaxCheck)
      return;
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, mad);
  }

  private void ssbond() throws ScriptException {
    int mad = getMadParameter();
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_SULFUR_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, mad);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void struts() throws ScriptException {
    boolean defOn = (tokAt(1) == Token.only || tokAt(1) == Token.on || statementLength == 1);
    int mad = getMadParameter();
    if (defOn)
      mad = (int) (viewer.getStrutDefaultRadius() * 2000f);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_STRUT));
    setShapeSize(JmolConstants.SHAPE_STICKS, mad);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void hbond(boolean isCommand) throws ScriptException {
    if (statementLength == 2 && getToken(1).tok == Token.calculate) {
      if (isSyntaxCheck)
        return;
      int n = viewer.autoHbond(null, null);
      scriptStatusOrBuffer(GT._("{0} hydrogen bonds", Math.abs(n)));
      return;
    }
    if (statementLength == 2 && getToken(1).tok == Token.delete) {
      if (isSyntaxCheck)
        return;
      connect(0);
      return;
    }
    int mad = getMadParameter();
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, mad);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void configuration() throws ScriptException {
    // if (!isSyntaxCheck && viewer.getDisplayModelIndex() <= -2)
    // error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BitSet bsAtoms;
    if (statementLength == 1) {
      bsAtoms = viewer.setConformation();
      viewer.addStateScript("select", null, viewer.getSelectionSet(), null,
          "configuration", true, false);
    } else {
      int n = intParameter(checkLast(1));
      if (isSyntaxCheck)
        return;
      bsAtoms = viewer.getConformation(viewer.getCurrentModelIndex(), n - 1,
          true);
      viewer.addStateScript("configuration " + n + ";", true, false);
    }
    if (isSyntaxCheck)
      return;
    boolean addHbonds = viewer.hasCalculatedHBonds(bsAtoms);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, 0, bsAtoms);
    if (addHbonds)
      viewer.autoHbond(bsAtoms, bsAtoms);
    viewer.select(bsAtoms, tQuiet);
  }

  private void vector() throws ScriptException {
    int type = RadiusData.TYPE_SCREEN;
    float value = 1;
    checkLength(-3);
    switch (iToken = statementLength) {
    case 1:
      break;
    case 2:
      switch (getToken(1).tok) {
      case Token.on:
        break;
      case Token.off:
        value = 0;
        break;
      case Token.integer:
        // diameter Pixels
        type = RadiusData.TYPE_SCREEN;
        value = intParameter(1, 0, 19);
        break;
      case Token.decimal:
        // radius angstroms
        type = RadiusData.TYPE_ABSOLUTE;
        value = floatParameter(1, 0, 3);
        break;
      default:
        error(ERROR_booleanOrNumberExpected);
      }
      break;
    case 3:
      if (tokAt(1) == Token.scale) {
        setFloatProperty("vectorScale", floatParameter(2, -100, 100));
        return;
      }
    }
    setShapeSize(JmolConstants.SHAPE_VECTORS, new RadiusData(value, type, 0));
  }

  private void dipole() throws ScriptException {
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    loadShape(JmolConstants.SHAPE_DIPOLES);
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
      case Token.id:
        setShapeId(JmolConstants.SHAPE_DIPOLES, ++i, idSeen);
        i = iToken;
        break;
      case Token.cross:
        propertyName = "cross";
        propertyValue = Boolean.TRUE;
        break;
      case Token.nocross:
        propertyName = "cross";
        propertyValue = Boolean.FALSE;
        break;
      case Token.offset:
        float v = floatParameter(++i);
        if (theTok == Token.integer) {
          propertyName = "offsetPercent";
          propertyValue = new Integer((int) v);
        } else {
          propertyName = "offset";
          propertyValue = new Float(v);
        }
        break;
      case Token.offsetside:
        propertyName = "offsetSide";
        propertyValue = new Float(floatParameter(++i));
        break;

      case Token.val:
        propertyName = "value";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.width:
        propertyName = "width";
        propertyValue = new Float(floatParameter(++i));
        break;
      default:
        if (theTok == Token.times || Token.tokAttr(theTok, Token.identifier)) {
          setShapeId(JmolConstants.SHAPE_DIPOLES, i, idSeen);
          i = iToken;
          break;
        }
        error(ERROR_invalidArgument);
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
      error(ERROR_badArgumentCount);
    int animationMode = JmolConstants.ANIMATION_ONCE;
    switch (getToken(2).tok) {
    case Token.loop:
      animationMode = JmolConstants.ANIMATION_LOOP;
      break;
    case Token.once:
      startDelay = endDelay = 0;
      break;
    case Token.palindrome:
      animationMode = JmolConstants.ANIMATION_PALINDROME;
      break;
    case Token.identifier:
      error(ERROR_invalidArgument);
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
    checkLength(-3);
    float period = 0;
    switch (getToken(1).tok) {
    case Token.on:
      checkLength(2);
      period = viewer.getVibrationPeriod();
      break;
    case Token.off:
      checkLength(2);
      period = 0;
      break;
    case Token.integer:
    case Token.decimal:
      checkLength(2);
      period = floatParameter(1);
      break;
    case Token.scale:
      setFloatProperty("vibrationScale", floatParameter(2, -10, 10));
      return;
    case Token.period:
      setFloatProperty("vibrationPeriod", floatParameter(2));
      return;
    case Token.identifier:
      error(ERROR_invalidArgument);
    default:
      period = -1;
    }
    if (period < 0)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    if (period == 0) {
      viewer.setVibrationOff();
      return;
    }
    viewer.setVibrationPeriod(-period);
  }

  private void animationDirection() throws ScriptException {
    int i = 2;
    int direction = 0;
    switch (tokAt(i)) {
    case Token.minus:
      direction = -intParameter(++i);
      break;
    case Token.plus:
      direction = intParameter(++i);
      break;
    case Token.integer:
      direction = intParameter(i);
      if (direction > 0)
        direction = 0;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    checkLength(++i);
    if (direction != 1 && direction != -1)
      error(ERROR_numberMustBe, "-1", "1");
    if (!isSyntaxCheck)
      viewer.setAnimationDirection(direction);
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    BitSet bs;
    BitSet bs2;
    int n = Integer.MIN_VALUE;
    if ((iToken = statementLength) >= 2) {
      clearDefinedVariableAtomSets();
      switch (getToken(1).tok) {
      case Token.straightness:
        if (!isSyntaxCheck) {
          viewer.calculateStraightness();
          viewer.addStateScript(thisCommand, false, true);
        }
        return;
      case Token.hydrogen:
        bs = (statementLength == 2 ? null : expression(2));
        checkLast(iToken);
        if (isSyntaxCheck)
          return;
        if (viewer.getModelSet().getModelCount() > 1)
          error(ERROR_multipleModelsNotOK); // for now...
        viewer.addHydrogens(bs);
        return;
      case Token.pointgroup:
        pointGroup();
        return;
      case Token.surface:
        isSurface = true;
        // deprecated
        // fall through
      case Token.surfacedistance:
        /*
         * preferred:
         * 
         * calculate surfaceDistance FROM {...} calculate surfaceDistance WITHIN
         * {...}
         */
        boolean isFrom = false;
        switch (tokAt(2)) {
        case Token.within:
          iToken++;
          break;
        case Token.nada:
          isFrom = !isSurface;
          break;
        case Token.from:
          isFrom = true;
          iToken++;
          break;
        default:
          isFrom = true;
        }
        bs = (iToken + 1 < statementLength ? expression(++iToken) : viewer
            .getSelectionSet());
        checkLength(++iToken);
        if (isSyntaxCheck)
          return;
        viewer.calculateSurface(bs, (isFrom ? Float.MAX_VALUE : -1));
        return;
      case Token.struts:
        bs = (iToken + 1 < statementLength ? expression(++iToken) : null);
        bs2 = (iToken + 1 < statementLength ? expression(++iToken) : null);
        checkLength(++iToken);
        if (isSyntaxCheck)
          return;
        n = viewer.calculateStruts(bs, bs2);
        if (n > 0)
          colorShape(JmolConstants.SHAPE_STRUTS, JmolConstants.BOND_STRUT,
              0x0FFFFFF, "translucent", 0.5f, null);
        showString(GT._("{0} struts added", n));
        return;
      case Token.volume:
        if (!isSyntaxCheck) {
          float val = viewer.getVolume(null, null);
          showString("" + Math.round(val * 10) / 10f + " A^3; "
              + Math.round(val * 6.02) / 10f + " cm^3/mole (VDW "
              + viewer.getDefaultVdwTypeNameOrData(Integer.MIN_VALUE) + ")");
          return;
        }
        break;
      case Token.aromatic:
        checkLength(2);
        if (!isSyntaxCheck)
          viewer.assignAromaticBonds();
        return;
      case Token.identifier:
        checkLength(2);
        break;
      case Token.hbond:
        if (statementLength == 2) {
          if (!isSyntaxCheck) {
            n = viewer.autoHbond(null, null);
            break;
          }
          return;
        }
        BitSet bs1 = expression(2);
        bs2 = expression(iToken + 1);
        if (!isSyntaxCheck) {
          n = viewer.autoHbond(bs1, bs2);
          break;
        }
        return;
      case Token.structure:
        bs = (statementLength == 2 ? null : expression(2));
        if (isSyntaxCheck)
          return;
        if (bs == null)
          bs = viewer.getAtomBitSet(null);
        viewer.calculateStructures(bs);
        viewer.addStateScript(thisCommand, false, true);
        return;
      }
      if (n != Integer.MIN_VALUE) {
        scriptStatusOrBuffer(GT._("{0} hydrogen bonds", Math.abs(n)));
        return;
      }
    }
    error(
        ERROR_what,
        "CALCULATE",
        "aromatic? hbonds? polymers? straightness? structure? strut? surfaceDistance FROM? surfaceDistance WITHIN? volume?");
  }

  private void pointGroup() throws ScriptException {
    switch (tokAt(0)) {
    case Token.calculate:
      if (!isSyntaxCheck)
        showString(viewer.calculatePointGroup());
      return;
    case Token.show:
      if (!isSyntaxCheck)
        showString(viewer.getPointGroupAsString(false, null, 0, 0));
      return;
    }
    // draw pointgroup [C2|C3|Cs|Ci|etc.] [n] [scale x]
    int pt = 2;
    String type = (tokAt(pt) == Token.scale ? "" : optParameterAsString(pt));
    float scale = 1;
    int index = 0;
    if (type.length() > 0) {
      if (isFloatParameter(++pt))
        index = intParameter(pt++);
    }
    if (tokAt(pt) == Token.scale)
      scale = floatParameter(++pt);
    if (!isSyntaxCheck)
      runScript(viewer.getPointGroupAsString(true, type, index, scale));
  }

  private void dots(int iShape) throws ScriptException {
    if (!isSyntaxCheck)
      loadShape(iShape);
    setShapeProperty(iShape, "init", null);
    float value = Float.NaN;
    int type = 0;
    int ipt = 1;
    switch (getToken(1).tok) {
    case Token.only:
      restrictSelected(false, false);
      value = 1;
      type = RadiusData.TYPE_FACTOR;
      break;
    case Token.on:
      value = 1;
      type = RadiusData.TYPE_FACTOR;
      break;
    case Token.off:
      value = 0;
      break;
    case Token.integer:
      int dotsParam = intParameter(ipt++);
      if (tokAt(ipt) == Token.radius) {
        setShapeProperty(iShape, "atom", new Integer(dotsParam));
        setShapeProperty(iShape, "radius", new Float(floatParameter(++ipt)));
        if (tokAt(++ipt) == Token.color) {
          setShapeProperty(iShape, "colorRGB", new Integer(getArgbParam(++ipt)));
          ipt++;
        }
        if (getToken(ipt).tok != Token.bitset)
          error(ERROR_invalidArgument);
        setShapeProperty(iShape, "dots", statement[ipt].value);
        return;
      }
      break;
    }
    RadiusData rd = (Float.isNaN(value) ? encodeRadiusParameter(1, false)
        : new RadiusData(value, type, 0));
    if (Float.isNaN(rd.value))
      error(ERROR_invalidArgument);
    setShapeSize(iShape, rd);
  }

  private void proteinShape(int shapeType) throws ScriptException {
    int mad = 0;
    // token has ondefault1
    switch (getToken(1).tok) {
    case Token.only:
      if (isSyntaxCheck)
        return;
      restrictSelected(false, false);
      mad = -1;
      break;
    case Token.on:
      mad = -1; // means take default
      break;
    case Token.off:
      break;
    case Token.structure:
      mad = -2;
      break;
    case Token.temperature:
    case Token.displacement:
      mad = -4;
      break;
    case Token.integer:
      mad = (intParameter(1, 0, 499) * 8);
      break;
    case Token.decimal:
      mad = (int) (floatParameter(1, -Shape.RADIUS_MAX, Shape.RADIUS_MAX) * 2000);
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    case Token.bitset:
      if (!isSyntaxCheck)
        loadShape(shapeType);
      setShapeProperty(shapeType, "bitset", theToken.value);
      return;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    setShapeSize(shapeType, mad);
  }

  private void animation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case Token.on:
      animate = true;
      // fall through
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
    case Token.fps:
      setIntProperty("animationFps", intParameter(checkLast(2)));
      break;
    default:
      frameControl(1, true);
    }
  }

  private void file() throws ScriptException {
    int file = intParameter(checkLast(1));
    if (isSyntaxCheck)
      return;
    int modelIndex = viewer.getModelNumberIndex(file * 1000000 + 1, false,
        false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = viewer.getModelNumberIndex((file + 1) * 1000000 + 1, false,
          false);
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
      int m;
      if (!isSyntaxCheck && modelIndex >= 0
          && (m = viewer.getJmolDataSourceFrame(modelIndex)) >= 0)
        viewer.setCurrentModelIndex(m == modelIndex ? Integer.MIN_VALUE : m);
      return;
    }
    switch (tokAt(1)) {
    case Token.title:
      if (checkLength23() > 0)
        if (!isSyntaxCheck)
          viewer.setFrameTitle(statementLength == 2 ? "@{_modelName}"
              : parameterAsString(2));
      return;
    case Token.align:
      BitSet bs = (statementLength == 2 || tokAt(2) == Token.none ? null
          : expression(2));
      if (!isSyntaxCheck)
        viewer.setFrameOffsets(bs);
      return;
    }
    if (getToken(offset).tok == Token.minus) {
      ++offset;
      if (getToken(checkLast(offset)).tok != Token.integer
          || intParameter(offset) != 1)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        viewer.setAnimation(Token.prev);
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
        checkLength(offset + (isRange ? 2 : 1));
        isAll = true;
        break;
      case Token.minus: // ignore
        if (nFrames != 1)
          error(ERROR_invalidArgument);
        isHyphen = true;
        break;
      case Token.none:
        checkLength(offset + 1);
        break;
      case Token.decimal:
        useModelNumber = false;
        if (floatParameter(i) < 0)
          isHyphen = true;
        // fall through
      case Token.integer:
      case Token.string:
        if (nFrames == 2)
          error(ERROR_invalidArgument);
        int iFrame = (theTok == Token.string ? JmolConstants.modelValue((String)theToken.value) : theToken.intValue);
        if (iFrame == -1) {
          checkLength(offset + 1);
          if (!isSyntaxCheck)
            viewer.setAnimation(Token.prev);
          return;
        }
        if (iFrame >= 1000 && iFrame < 1000000 && viewer.haveFileSet())
          iFrame = (iFrame / 1000) * 1000000 + (iFrame % 1000); // initial way
        if (!useModelNumber && iFrame == 0)
          isAll = true; // 0.0 means ALL; 0 means "all in this range
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
        frameControl(offset, false);
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
    int modelIndex = viewer.getModelNumberIndex(frameList[0], useModelNumber,
        false);
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
          isRange = isRange || modelIndex == modelIndex2;// (isRange ||
          // !isHyphen &&
          // modelIndex2 !=
          // modelIndex);
        }
      } else {
        // must have been a bad frame number. Just return.
        return;
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0)
      viewer.setCurrentModelIndex(modelIndex, false);
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelNumberIndex(frameList[1], useModelNumber,
            false);
      viewer.setAnimationOn(false);
      viewer.setAnimationDirection(1);
      viewer.setAnimationRange(modelIndex, modelIndex2);
      viewer.setCurrentModelIndex(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0, false);
    }
    if (isPlay)
      viewer.setAnimation(Token.resume);
  }

  BitSet bitSetForModelFileNumber(int m) {
    // where */1.0 or */1.1 or just 1.1 is processed
    BitSet bs = new BitSet(viewer.getAtomCount());
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
    switch (getToken(checkLast(i)).tok) {
    case Token.playrev:
    case Token.play:
    case Token.resume:
    case Token.pause:
    case Token.next:
    case Token.prev:
    case Token.rewind:
    case Token.first:
    case Token.last:
      if (!isSyntaxCheck)
        viewer.setAnimation(theTok);
      return;
    }
    error(ERROR_invalidArgument);
  }

  private int getShapeType(int tok) throws ScriptException {
    int iShape = JmolConstants.shapeTokenIndex(tok);
    if (iShape < 0)
      error(ERROR_unrecognizedObject);
    return iShape;
  }

  private void font(int shapeType, float fontsize) throws ScriptException {
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    int sizeAdjust = 0;
    float scaleAngstromsPerPixel = -1;
    switch (iToken = statementLength) {
    case 6:
      scaleAngstromsPerPixel = floatParameter(5);
      if (scaleAngstromsPerPixel >= 5) // actually a zoom value
        scaleAngstromsPerPixel = viewer.getZoomSetting()
            / scaleAngstromsPerPixel / viewer.getScalePixelsPerAngstrom(false);
      // fall through
    case 5:
      if (getToken(4).tok != Token.identifier)
        error(ERROR_invalidArgument);
      fontstyle = parameterAsString(4);
      // fall through
    case 4:
      if (getToken(3).tok != Token.identifier)
        error(ERROR_invalidArgument);
      fontface = parameterAsString(3);
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      fontsize = floatParameter(2);
      shapeType = getShapeType(getToken(1).tok);
      break;
    case 3:
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      if (shapeType == -1) {
        shapeType = getShapeType(getToken(1).tok);
        fontsize = floatParameter(2);
      } else {// labels --- old set fontsize N
        if (fontsize >= 1)
          fontsize += (sizeAdjust = 5);
      }
      break;
    case 2:
    default:
      if (shapeType == JmolConstants.SHAPE_LABELS) {
        // set fontsize
        fontsize = JmolConstants.LABEL_DEFAULT_FONTSIZE;
        break;
      }
      error(ERROR_badArgumentCount);
    }
    if (shapeType == JmolConstants.SHAPE_LABELS) {
      if (fontsize < 0
          || fontsize >= 1
          && (fontsize < JmolConstants.LABEL_MINIMUM_FONTSIZE || fontsize > JmolConstants.LABEL_MAXIMUM_FONTSIZE))
        integerOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE - sizeAdjust,
            JmolConstants.LABEL_MAXIMUM_FONTSIZE - sizeAdjust);
      setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
          .getNoneSelected());
    }
    if (isSyntaxCheck)
      return;
    Font3D font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
    loadShape(shapeType);
    setShapeProperty(shapeType, "font", font3d);
    if (scaleAngstromsPerPixel >= 0)
      setShapeProperty(shapeType, "scalereference", new Float(
          scaleAngstromsPerPixel));
  }

  private void set() throws ScriptException {
    /*
     * The SET command now allows only the following:
     * 
     * SET SET xxx? SET [valid Jmol Token.setparam keyword] SET labelxxxx SET
     * xxxxCallback
     * 
     * All other variables must be assigned using
     * 
     * x = ....
     * 
     * The processing goes as follows:
     * 
     * check for SET check for SET xx? check for SET xxxx where xxxx is a
     * command --- deprecated (all other settings may alternatively start with x
     * = y) check for SET xxxx where xxxx requires special checking (all other
     * settings may alternatively start with x = (math expression) check for
     * context variables var x = ... check for deprecated SET words such as
     * "radius"
     */
    if (statementLength == 1) {
      showString(viewer.getAllSettings(null));
      return;
    }
    boolean isJmolSet = (parameterAsString(0).equals("set"));
    String key = optParameterAsString(1);
    if (isJmolSet && statementLength == 2 && key.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(key.substring(0, key.indexOf("?"))));
      return;
    }
    int tok = getToken(1).tok;
    int newTok = 0;
    String sval;
    int ival = Integer.MAX_VALUE;

    boolean showing = (!isSyntaxCheck && !tQuiet
        && scriptLevel <= scriptReportingLevel && !((String) statement[0].value)
        .equals("var"));

    // THESE FIRST ARE DEPRECATED AND HAVE THEIR OWN COMMAND
    // anything in this block MUST RETURN

    switch (tok) {
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
    case Token.display:// deprecated
    case Token.selectionhalos:
      selectionHalo(2);
      return;
    case Token.timeout:
      timeout(2);
      return;
    }

    // THESE HAVE MULTIPLE CONTEXTS AND
    // SO DO NOT ALLOW CALCULATIONS xxx = a + b...
    // and are thus "setparam" only

    // anything in this block MUST RETURN

    switch (tok) {
    case Token.axescolor:
      ival = getArgbParam(2);
      if (!isSyntaxCheck)
        setObjectArgb("axes", ival);
      return;
    case Token.bondmode:
      setBondmode();
      return;
    case Token.debug:
      if (isSyntaxCheck)
        return;
      int iLevel = (tokAt(2) == Token.off || tokAt(2) == Token.integer
          && intParameter(2) == 0 ? 4 : 5);
      Logger.setLogLevel(iLevel);
      setIntProperty("logLevel", iLevel);
      if (iLevel == 4) {
        viewer.setDebugScript(false);
        if (showing)
          viewer.showParameter("debugScript", true, 80);
      }
      setDebugging();
      if (showing)
        viewer.showParameter("logLevel", true, 80);
      return;
    case Token.echo:
      setEcho();
      return;
    case Token.fontsize:
      font(JmolConstants.SHAPE_LABELS, checkLength23() == 2 ? 0
          : floatParameter(2));
      return;
    case Token.hbond:
      setHbond();
      return;
    case Token.measure:
    case Token.measurements:
      setMonitor();
      return;
    case Token.ssbond: // ssBondsBackbone
      setSsbond();
      return;
    case Token.togglelabel:
      setLabel("toggle");
      return;
    case Token.usercolorscheme:
      setUserColors();
      return;
    }

    // these next may report a value
    // require special checks
    // math expressions are allowed in most cases.

    boolean justShow = true;

    switch (tok) {
    case Token.axesscale:
      setFloatProperty("axesScale", floatSetting(2, -100, 100));
      break;
    case Token.backgroundmodel:
      if (statementLength > 2) {
        String modelDotted = stringSetting(2, false);
        int modelNumber;
        boolean useModelNumber = false;
        if (modelDotted.indexOf(".") < 0) {
          modelNumber = Parser.parseInt(modelDotted);
          useModelNumber = true;
        } else {
          modelNumber = JmolConstants.modelValue(modelDotted);
        }
        if (isSyntaxCheck)
          return;
        int modelIndex = viewer.getModelNumberIndex(modelNumber,
            useModelNumber, true);
        viewer.setBackgroundModelIndex(modelIndex);
        return;
      }
      break;
    case Token.defaultvdw:
      // allows unquoted string for known vdw type
      if (statementLength > 2) {
        sval = (statementLength == 3
            && JmolConstants.getVdwType(parameterAsString(2)) == JmolConstants.VDW_UNKNOWN ? stringSetting(
            2, false)
            : parameterAsString(2));
        if (JmolConstants.getVdwType(sval) < 0)
          error(ERROR_invalidArgument);
        setStringProperty(key, sval);
      }
      break;
    case Token.defaultlattice:
      if (statementLength > 2) {
        Point3f pt;
        Vector v = (Vector) parameterExpression(2, 0, "XXX", true);
        if (v == null || v.size() == 0)
          error(ERROR_invalidArgument);
        ScriptVariable var = (ScriptVariable) v.elementAt(0);
        if (var.tok == Token.point3f)
          pt = (Point3f) var.value;
        else {
          int ijk = ScriptVariable.iValue(var);
          if (ijk < 555)
            pt = new Point3f();
          else
            pt = viewer.getSymmetry().ijkToPoint3f(ijk + 111);
        }
        if (!isSyntaxCheck)
          viewer.setDefaultLattice(pt);
      }
      break;
    case Token.defaults:
    case Token.defaultcolorscheme:
      // allows unquoted "jmol" or "rasmol"
      if (statementLength > 2) {
        if ((theTok = tokAt(2)) == Token.jmol || theTok == Token.rasmol) {
          sval = parameterAsString(checkLast(2)).toLowerCase();
        } else {
          sval = stringSetting(2, false).toLowerCase();
        }
        if (!sval.equals("jmol") && !sval.equals("rasmol"))
          error(ERROR_invalidArgument);
        setStringProperty(key, sval);
      }
      break;
    case Token.dipolescale:
      setFloatProperty("dipoleScale", floatSetting(2, -10, 10));
      break;
    case Token.formalcharge:
      ival = intSetting(2);
      if (ival == Integer.MIN_VALUE)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        viewer.setFormalCharges(ival);
      return;
    case Token.historylevel:
      // save value locally as well
      ival = intSetting(2);
      if (!isSyntaxCheck) {
        if (ival != Integer.MIN_VALUE)
          commandHistoryLevelMax = ival;
        setIntProperty(key, ival);
      }
      break;
    case Token.language:
      // language can be used without quotes in a SET context
      // set language en
      if (statementLength > 2)
        setStringProperty(key, stringSetting(2, isJmolSet));
      break;
    case Token.measurementunits:
      if (statementLength > 2)
        setMeasurementUnits(stringSetting(2, isJmolSet));
      break;
    case Token.phongexponent:
      setIntProperty(key, intSetting(2, Integer.MAX_VALUE, 0, 1000));
      break;
    case Token.picking:
      if (statementLength > 2) {
        setPicking();
        return;
      }
      break;
    case Token.pickingstyle:
      if (statementLength > 2) {
        setPickingStyle();
        return;
      }
      break;
    case Token.property: // compiler may give different values to this token
      // set property_xxxx will be handled in setVariable
      break;
    case Token.scriptreportinglevel:
      // save value locally as well
      ival = intSetting(2);
      if (!isSyntaxCheck) {
        if (ival != Integer.MIN_VALUE)
          scriptReportingLevel = ival;
        setIntProperty(key, ival);
      }
      break;
    case Token.solventproberadius:
      setFloatProperty(key, floatSetting(2, 0, 10));
      break;
    case Token.specular:
    case Token.specularpercent:
    case Token.ambientpercent:
    case Token.diffusepercent:
      ival = intSetting(2);
      if (tok == Token.specular) {
        if (ival == Integer.MIN_VALUE || ival == 0 || ival == 1) {
          justShow = false;
          break;
        }
        tok = Token.specularpercent;
        key = "specularPercent";
      }
      setIntProperty(key, intSetting(2, ival, 0, 100));
      break;
    case Token.specularpower:
    case Token.specularexponent:
      ival = intSetting(2);
      if (tok == Token.specularpower) {
        if (ival >= 0) {
          justShow = false;
          break;
        }
        tok = Token.specularexponent;
        key = "specularExponent";
        if (ival < -10 || ival > -1)
          integerOutOfRange(-10, -1);
        ival = -ival;
      }
      setIntProperty(key, intSetting(2, ival, 0, 10));
      break;
    case Token.strands:
    case Token.strandcount:
    case Token.strandcountformeshribbon:
    case Token.strandcountforstrands:
      if (tok == Token.strands) {
        tok = Token.strandcount;
        key = "strandCount";
      }
      setIntProperty(key, intSetting(2, Integer.MAX_VALUE, 0, 20));
      break;
    default:
      justShow = false;
    }

    if (justShow && !showing)
      return;

    // var xxxx = xxx can supercede set xxxx

    boolean isContextVariable = (!justShow && !isJmolSet && getContextVariableAsVariable(key) != null);

    if (!justShow && !isContextVariable) {

      // THESE NEXT are deprecated:

      switch (tok) {
      case Token.bonds:
        newTok = Token.showmultiplebonds;
        break;
      case Token.hetero:
        newTok = Token.selecthetero;
        break;
      case Token.hydrogen:
        newTok = Token.selecthydrogen;
        break;
      case Token.measurementnumbers:
        newTok = Token.measurementlabels;
        break;
      case Token.radius:
        newTok = Token.solventproberadius;
        setFloatProperty("solventProbeRadius", floatSetting(2, 0, 10));
        justShow = true;
        break;
      case Token.scale3d:
        newTok = Token.scaleangstromsperinch;
        break;
      case Token.solvent:
        newTok = Token.solventprobe;
        break;
      case Token.color:
        newTok = Token.defaultcolorscheme;
        break;
      case Token.spin:
        sval = parameterAsString(2).toLowerCase();
        switch ("x;y;z;fps".indexOf(sval + ";")) {
        case 0:
          newTok = Token.spinx;
          break;
        case 2:
          newTok = Token.spiny;
          break;
        case 4:
          newTok = Token.spinz;
          break;
        case 6:
          newTok = Token.spinfps;
          break;
        default:
          error(ERROR_unrecognizedParameter, "set SPIN ", sval);
        }
        if (!isSyntaxCheck)
          viewer.setSpin(sval, (int) floatParameter(checkLast(3)));
        justShow = true;
        break;
      }
    }

    if (newTok != 0) {
      key = Token.nameOf(tok = newTok);
    } else if (!justShow && !isContextVariable) {
      // special cases must be checked
      if (key.charAt(0) == '_') // these cannot be set by user
        error(ERROR_invalidArgument);

      // these next are not reported and do not allow calculation xxxx = a + b

      String lckey = key.toLowerCase();
      if (lckey.indexOf("label") == 0
          && Parser
              .isOneOf(key.substring(5).toLowerCase(),
                  "front;group;atom;offset;offsetexact;pointer;alignment;toggle;scalereference")) {
        if (setLabel(key.substring(5)))
          return;
      }
      if (lckey.indexOf("callback") >= 0)
        tok = Token.setparam;
    }

    if (isJmolSet && !Token.tokAttr(tok, Token.setparam)) {
      iToken = 1;
      if (!isStateScript)
        error(ERROR_unrecognizedParameter, "SET", key);
      warning(ERROR_unrecognizedParameterWarning, "SET", key);
    }

    if (!justShow && isJmolSet) {
      // simple cases
      switch (statementLength) {
      case 2:
        // set XXXX;
        // too bad we allow this...
        setBooleanProperty(key, true);
        justShow = true;
        break;
      case 3:
        // set XXXX val;
        // check for int and NONE just in case
        if (ival != Integer.MAX_VALUE) {
          // keep it simple
          setIntProperty(key, ival);
          justShow = true;
        }
        break;
      }
    }

    if (!justShow && !isJmolSet && tokAt(2) == Token.none) {
      if (!isSyntaxCheck)
        viewer.removeUserVariable(key);
      justShow = true;
    }

    if (!justShow) {
      int tok2 = (tokAt(1) == Token.expressionBegin ? 0 : tokAt(2));
      int setType = statement[0].intValue;
      // recasted by compiler:
      // var c.xxx =
      // c.xxx =
      // {...}[n].xxx =
      // not supported:
      // a[...][...].xxx =
      // var a[...][...].xxx =

      int pt = (tok2 == Token.opEQ ? 3
      // set x = ...
          : setType == '=' && !key.equals("return") && tok2 != Token.opEQ ? 0
          // {c}.xxx =
              // {...}.xxx =
              // {{...}[n]}.xxx =
              : 2
      // var a[...].xxx =
      // a[...].xxx =
      // var c = ...
      // var c = [
      // c = [
      // c = ...
      // set x ...
      // a[...] =
      );

      setVariable(pt, 0, key, setType);
      if (!isJmolSet)
        return;
    }
    if (showing)
      viewer.showParameter(key, true, 80);
  }

  private void setBondmode() throws ScriptException {
    boolean bondmodeOr = false;
    switch (getToken(checkLast(2)).tok) {
    case Token.opAnd:
      break;
    case Token.opOr:
      bondmodeOr = true;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("bondModeOr", bondmodeOr);
  }

  private void setEcho() throws ScriptException {
    String propertyName = "target";
    Object propertyValue = null;
    boolean echoShapeActive = true;
    // set echo xxx
    int len = 3;
    switch (getToken(2).tok) {
    case Token.off:
      checkLength(3);
      echoShapeActive = false;
      propertyName = "allOff";
      break;
    case Token.hide:
    case Token.hidden:
      propertyName = "hidden";
      propertyValue = Boolean.TRUE;
      break;
    case Token.on:
    case Token.display:
    case Token.displayed:
      propertyName = "hidden";
      propertyValue = Boolean.FALSE;
      break;
    case Token.none:
      echoShapeActive = false;
      // fall through
    case Token.all:
      checkLength(3);
      // fall through
    case Token.left:
    case Token.right:
    case Token.top:
    case Token.bottom:
    case Token.center:
    case Token.identifier:
      propertyValue = parameterAsString(2);
      break;
    case Token.model:
      int modelIndex = modelNumberParameter(3);
      if (isSyntaxCheck)
        return;
      if (modelIndex >= viewer.getModelCount())
        error(ERROR_invalidArgument);
      propertyName = "model";
      propertyValue = new Integer(modelIndex);
      len = 4;
      break;
    case Token.image:
      // set echo image "..."
      echo(3, true);
      return;
    case Token.depth:
      // set echo depth zzz
      propertyName = "%zpos";
      propertyValue = new Integer((int) floatParameter(3));
      len = 4;
      break;
    case Token.string:
      echo(2, false);
      return;
    default:
      if (!Token.tokAttr(theTok, Token.identifier))
        error(ERROR_invalidArgument);
      propertyValue = parameterAsString(2);
      break;
    }
    if (!isSyntaxCheck) {
      viewer.setEchoStateActive(echoShapeActive);
      loadShape(JmolConstants.SHAPE_ECHO);
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
    }
    if (statementLength == len)
      return;
    propertyName = "align";
    propertyValue = null;
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
      case Token.hidden:
        propertyName = "hidden";
        propertyValue = Boolean.TRUE;
        break;
      case Token.displayed:
      case Token.on:
        propertyName = "hidden";
        propertyValue = Boolean.FALSE;
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(4);
        if (isSyntaxCheck)
          return;
        if (modelIndex >= viewer.getModelCount())
          error(ERROR_invalidArgument);
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.left:
      case Token.right:
      case Token.top:
      case Token.bottom:
      case Token.center:
      case Token.identifier:
        propertyValue = parameterAsString(3);
        break;
      default:
        if (!Token.tokAttr(theTok, Token.identifier))
          error(ERROR_invalidArgument);
        propertyValue = parameterAsString(3);
        break;
      }
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
      return;
    }
    // set echo name script "some script"
    // set echo name model x.y
    // set echo name depth nnnn
    // set echo name image "myimage.jpg"
    if (statementLength == 5) {
      switch (tokAt(3)) {
      case Token.script:
        propertyName = "script";
        propertyValue = parameterAsString(4);
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(4);
        if (!isSyntaxCheck && modelIndex >= viewer.getModelCount())
          error(ERROR_invalidArgument);
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.image:
        // set echo name image "xxx"
        echo(4, true);
        return;
      case Token.depth:
        propertyName = "%zpos";
        propertyValue = new Integer((int) floatParameter(4));
        break;
      }
      if (propertyValue != null) {
        setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
        return;
      }
    }
    // set echo name [x y] or set echo name [x y %]
    // set echo name x-pos y-pos

    getToken(4);
    int i = 3;
    // set echo name {x y z}
    if (isCenterParameter(i)) {
      if (!isSyntaxCheck)
        setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", centerParameter(i));
      return;
    }
    String type = "xypos";
    if ((propertyValue = xypParameter(i)) == null) {
      int pos = intParameter(i++);
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
    }
    setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
  }

  private boolean setLabel(String str) throws ScriptException {
    loadShape(JmolConstants.SHAPE_LABELS);
    Object propertyValue = null;
    setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
        .getNoneSelected());
    while (true) {
      if (str.equals("scalereference")) {
        float scaleAngstromsPerPixel = floatParameter(2);
        if (scaleAngstromsPerPixel >= 5) // actually a zoom value
          scaleAngstromsPerPixel = viewer.getZoomSetting()
              / scaleAngstromsPerPixel
              / viewer.getScalePixelsPerAngstrom(false);
        propertyValue = new Float(scaleAngstromsPerPixel);
        break;
      }
      if (str.equals("offset") || str.equals("offsetexact")) {
        int xOffset = intParameter(2, -127, 127);
        int yOffset = intParameter(3, -127, 127);
        propertyValue = new Integer(Object2d.getOffset(xOffset, yOffset));
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
          error(ERROR_invalidArgument);
        }
        break;
      }
      if (str.equals("pointer")) {
        int flags = Object2d.POINTER_NONE;
        switch (getToken(2).tok) {
        case Token.off:
        case Token.none:
          break;
        case Token.background:
          flags |= Object2d.POINTER_BACKGROUND;
        case Token.on:
          flags |= Object2d.POINTER_ON;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        propertyValue = new Integer(flags);
        break;
      }
      if (str.equals("toggle")) {
        iToken = 1;
        BitSet bs = (statementLength == 2 ? null : expression(2));
        checkLast(iToken);
        if (!isSyntaxCheck)
          viewer.togglePickingLabel(bs);
        return true;
      }
      iToken = 1;
      boolean TF = (statementLength == 2 || getToken(2).tok == Token.on);
      if (str.equals("front") || str.equals("group")) {
        if (!TF && tokAt(2) != Token.off)
          error(ERROR_invalidArgument);
        if (!TF)
          str = "front";
        propertyValue = (TF ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
      if (str.equals("atom")) {
        if (!TF && tokAt(2) != Token.off)
          error(ERROR_invalidArgument);
        str = "front";
        propertyValue = (TF ? Boolean.FALSE : Boolean.TRUE);
        break;
      }
      return false;
    }
    BitSet bs = (iToken + 1 < statementLength ? expression(++iToken) : null);
    checkLast(iToken);
    if (isSyntaxCheck)
      return true;
    if (bs == null)
      setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue);
    else
      setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue,
          bs);
    return true;
  }

  private void setMonitor() throws ScriptException {
    // on off here incompatible with "monitor on/off" so this is just a SET
    // option.
    int tok = tokAt(checkLast(2));
    switch (tok) {
    case Token.on:
    case Token.off:
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
          tok == Token.on ? Boolean.TRUE : Boolean.FALSE);
      return;
    case Token.dotted:
    case Token.integer:
    case Token.decimal:
      setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad(2));
      return;
    }
    setMeasurementUnits(parameterAsString(2));
  }

  private boolean setMeasurementUnits(String units) throws ScriptException {
    if (!StateManager.isMeasurementUnit(units))
      error(ERROR_unrecognizedParameter, "set measurementUnits ", units);
    if (!isSyntaxCheck)
      viewer.setMeasureDistanceUnits(units);
    return true;
  }

  /*
   * private void setProperty() throws ScriptException { // what possible good
   * is this? // set property foo bar is identical to // set foo bar if
   * (getToken(2).tok != Token.identifier) error(ERROR_propertyNameExpected);
   * String propertyName = parameterAsString(2); switch
   * (getToken(checkLast(3)).tok) { case Token.on:
   * setBooleanProperty(propertyName, true); break; case Token.off:
   * setBooleanProperty(propertyName, false); break; case Token.integer:
   * setIntProperty(propertyName, intParameter(3)); break; case Token.decimal:
   * setFloatProperty(propertyName, floatParameter(3)); break; case
   * Token.string: setStringProperty(propertyName, stringParameter(3)); break;
   * default: error(ERROR_unrecognizedParameter, "SET " +
   * propertyName.toUpperCase(), parameterAsString(3)); } }
   */

  private void setSsbond() throws ScriptException {
    boolean ssbondsBackbone = false;
    // loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch (tokAt(checkLast(2))) {
    case Token.backbone:
      ssbondsBackbone = true;
      break;
    case Token.sidechain:
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("ssbondsBackbone", ssbondsBackbone);
  }

  private void setHbond() throws ScriptException {
    boolean bool = false;
    switch (tokAt(checkLast(2))) {
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
      error(ERROR_invalidArgument);
    }
  }

  private void setPicking() throws ScriptException {
    // set picking
    if (statementLength == 2) {
      setStringProperty("picking", "identify");
      return;
    }
    // set picking @{"xxx"} or some large length, ignored
    if (statementLength > 4 || tokAt(2) == Token.string) {
      setStringProperty("picking", stringSetting(2, false));
      return;
    }
    int i = 2;
    // set picking select ATOM|CHAIN|GROUP|MOLECULE|MODEL|SITE
    // set picking measure ANGLE|DISTANCE|TORSION
    // set picking spin fps
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.select:
    case Token.measure:
    case Token.spin:
      if (checkLength34() == 4) {
        type = parameterAsString(2).toUpperCase();
        if (type.equals("SPIN"))
          setIntProperty("pickingSpinRate", intParameter(3));
        else
          i = 3;
      }
    case Token.delete:
      break;
    default:
      checkLength(3);
    }

    // set picking on
    // set picking normal
    // set picking identify
    // set picking off
    // set picking select
    // set picking bonds
    // set picking dragselected

    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case Token.on:
    case Token.normal:
      str = "identify";
      break;
    case Token.none:
      str = "off";
      break;
    case Token.select:
      str = "atom";
      break;
    case Token.label:
      str = "label";
      break;
    case Token.bonds: // not implemented
      str = "bond";
      break;
    case Token.delete:
      checkLength(4);
      if (tokAt(3) != Token.bonds)
        error(ERROR_invalidArgument);
      str = "deleteBond";
      break;
    }
    int mode = JmolConstants.getPickingMode(str);
    if (mode < 0)
      error(ERROR_unrecognizedParameter, "SET PICKING " + type, str);
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
    case Token.measure:
      isMeasure = true;
      type = "MEASURE";
      // fall through
    case Token.select:
      if (checkLength34() == 4)
        i = 3;
      break;
    default:
      checkLength(3);
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
      error(ERROR_unrecognizedParameter, "SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  private void timeout(int index) throws ScriptException {
    // timeout ID "mytimeout" mSec "script"
    // msec < 0 --> repeat indefinitely
    // timeout ID "mytimeout" 1000 // milliseconds
    // timeout ID "mytimeout" 0.1 // seconds
    // timeout ID "mytimeout" OFF
    // timeout OFF
    String name = null;
    String script = null;
    int mSec = 0;
    if (statementLength == index) {
      showString(viewer.showTimeout(null));
      return;
    }
    for (int i = index; i < statementLength; i++)
      switch (getToken(i).tok) {
      case Token.id:
        name = parameterAsString(++i);
        break;
      case Token.off:
        break;
      case Token.integer:
        mSec = intParameter(i);
        break;
      case Token.decimal:
        mSec = (int) (floatParameter(i) * 1000);
        break;
      default:
        if (name == null)
          name = parameterAsString(i);
        else if (script == null)
          script = parameterAsString(i);
        else
          error(ERROR_invalidArgument);
        break;
      }
    if (!isSyntaxCheck)
      viewer.setTimeout(name, mSec, script);
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

  private void setVariable(int pt, int ptMax, String key, int setType)
      throws ScriptException {

    // from SET, we are only here if a Jmol parameter has not been identified
    // from FOR or WHILE, no such check is made

    // if both pt and ptMax are 0, then it indicates that

    BitSet bs = null;
    String propertyName = "";
    int tokProperty = Token.nada;
    boolean isArrayItem = (statement[0].intValue == '[');
    boolean settingProperty = false;
    boolean isExpression = false;
    boolean settingData = (key.startsWith("property_"));
    ScriptVariable t = (settingData ? null : getContextVariableAsVariable(key));
    boolean isUserVariable = (t != null);
    if (pt > 0 && tokAt(pt - 1) == Token.expressionBegin) {
      bs = expression(pt - 1);
      pt = iToken + 1;
      isExpression = true;
    }
    if (tokAt(pt) == Token.per) {
      settingProperty = true;
      ScriptVariable token = getBitsetPropertySelector(++pt, true);
      if (token == null)
        error(ERROR_invalidArgument);
      if (tokAt(++pt) != Token.opEQ)
        error(ERROR_invalidArgument);
      pt++;
      tokProperty = token.intValue;
      propertyName = (String) token.value;
    }
    if (isExpression && !settingProperty)
      error(ERROR_invalidArgument);

    // get value

    Object v = parameterExpression(pt, ptMax, key, true, -1, isArrayItem, null,
        null);
    if (v == null)
      return;
    int nv = ((Vector) v).size();
    if (nv == 0 || !isArrayItem && nv > 1 || isArrayItem && nv != 3)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    ScriptVariable tv = (ScriptVariable) ((Vector) v).get(isArrayItem ? 2 : 0);

    // create user variable if needed for list now, so we can do the copying

    boolean needVariable = (!isUserVariable && !isExpression && !settingData && (isArrayItem
        || settingProperty || !(tv.value instanceof String
        || tv.tok == Token.integer || tv.value instanceof Integer
        || tv.value instanceof Float || tv.value instanceof Boolean)));

    if (needVariable) {
      t = viewer.getOrSetNewVariable(key, true);
      if (t == null) { // can't set a variable _xxxx
        error(ERROR_invalidArgument);
      }
      isUserVariable = true;
    }

    if (isArrayItem) {

      // stack is selector [ VALUE

      int index = ScriptVariable.iValue((ScriptVariable) ((Vector) v).get(0));
      t.setSelectedValue(index, tv);
      return;
    }
    if (settingProperty) {
      if (!isExpression) {
        if (!(t.value instanceof BitSet))
          error(ERROR_invalidArgument);
        bs = (BitSet) t.value;
      }
      if (propertyName.startsWith("property_")) {
        viewer.setData(propertyName, new Object[] { propertyName,
            ScriptVariable.sValue(tv), BitSetUtil.copy(bs) }, viewer
            .getAtomCount(), 0, 0, tv.tok == Token.list ? Integer.MAX_VALUE
            : Integer.MIN_VALUE, 0);
        return;
      }
      setBitsetProperty(bs, tokProperty, ScriptVariable.iValue(tv),
          ScriptVariable.fValue(tv), tv);
      return;
    }

    if (isUserVariable) {
      t.set(tv);
      return;
    }

    v = ScriptVariable.oValue(tv);

    if (key.startsWith("property_")) {
      int n = viewer.getAtomCount();
      if (v instanceof String[])
        v = TextFormat.join((String[]) v, '\n', 0);
      viewer.setData(key, new Object[] { key, "" + v,
          BitSetUtil.copy(viewer.getSelectionSet()) }, n, 0, 0,
          Integer.MIN_VALUE, 0);
      return;
    }
    String str;
    if (v instanceof Boolean) {
      setBooleanProperty(key, ((Boolean) v).booleanValue());
    } else if (v instanceof Integer) {
      setIntProperty(key, ((Integer) v).intValue());
    } else if (v instanceof Float) {
      setFloatProperty(key, ((Float) v).floatValue());
    } else if (v instanceof String) {
      setStringProperty(key, (String) v);
    } else if (v instanceof BondSet) {
      setStringProperty(key, Escape.escape((BitSet) v, false));
    } else if (v instanceof BitSet) {
      setStringProperty(key, Escape.escape((BitSet) v));
    } else if (v instanceof Point3f) {
      str = Escape.escape((Point3f) v);
      setStringProperty(key, str);
    } else if (v instanceof Point4f) {
      str = Escape.escape((Point4f) v);
      setStringProperty(key, str);
    } else {
      Logger.error("ERROR -- return from propertyExpression was " + v);
    }
  }

  private void axes(int index) throws ScriptException {
    // axes (index==1) or set axes (index==2)
    TickInfo tickInfo = checkTicks(index);
    index = iToken + 1;
    int tok = tokAt(index);
    String type = optParameterAsString(index).toLowerCase();
    if (statementLength == index + 1
        && Parser.isOneOf(type, "window;unitcell;molecular")) {
      setBooleanProperty("axes" + type, true);
      return;
    }
    // axes scale x.xxx
    switch (tok) {
    case Token.scale:
      setFloatProperty("axesScale", floatParameter(checkLast(++index)));
      return;
    case Token.label:
      switch (tok = tokAt(index + 1)) {
      case Token.off:
      case Token.on:
        checkLength(index + 2);
        setShapeProperty(JmolConstants.SHAPE_AXES, "labels"
            + (tok == Token.on ? "On" : "Off"), null);
        return;
      }
      checkLength(index + 4);
      // axes labels "X" "Y" "Z"
      setShapeProperty(JmolConstants.SHAPE_AXES, "labels", new String[] {
          parameterAsString(++index), parameterAsString(++index),
          parameterAsString(++index) });
      return;
    }
    // axes position [x y %]
    if (type.equals("position")) {
      Point3f xyp;
      if (tokAt(++index) == Token.off) {
        xyp = new Point3f();
      } else {
        xyp = xypParameter(index);
        if (xyp == null)
          error(ERROR_invalidArgument);
        index = iToken;
      }
      setShapeProperty(JmolConstants.SHAPE_AXES, "position", xyp);
      return;
    }
    int mad = getSetAxesTypeMad(index);
    if (isSyntaxCheck)
      return;
    setObjectMad(JmolConstants.SHAPE_AXES, "axes", mad);
    if (tickInfo != null)
      setShapeProperty(JmolConstants.SHAPE_AXES, "tickInfo", tickInfo);
  }

  private void boundbox(int index) throws ScriptException {
    TickInfo tickInfo = checkTicks(index);
    index = iToken + 1;
    float scale = 1;
    if (tokAt(index) == Token.scale) {
      scale = floatParameter(++index);
      if (!isSyntaxCheck && scale == 0)
        error(ERROR_invalidArgument);
      index++;
      if (index == statementLength) {
        if (!isSyntaxCheck)
          viewer.setBoundBox(null, null, true, scale);
        return;
      }
    }
    boolean byCorner = (tokAt(index) == Token.corners);
    if (byCorner)
      index++;
    if (isCenterParameter(index)) {
      expressionResult = null;
      int index0 = index;
      Point3f pt1 = centerParameter(index);
      index = iToken + 1;
      if (byCorner || isCenterParameter(index)) {
        // boundbox CORNERS {expressionOrPoint1} {expressionOrPoint2}
        // boundbox {expressionOrPoint1} {vector}
        Point3f pt2 = (byCorner ? centerParameter(index) : getPoint3f(index,
            true));
        index = iToken + 1;
        if (!isSyntaxCheck)
          viewer.setBoundBox(pt1, pt2, byCorner, scale);
      } else if (expressionResult != null && expressionResult instanceof BitSet) {
        // boundbox {expression}
        if (!isSyntaxCheck)
          viewer.calcBoundBoxDimensions((BitSet) expressionResult, scale);
      } else if (expressionResult == null && tokAt(index0) == Token.dollarsign) {
        if (isSyntaxCheck)
          return;
        Point3f[] bbox = getObjectBoundingBox(objectNameParameter(++index0));
        if (bbox == null)
          error(ERROR_invalidArgument);
        viewer.setBoundBox(bbox[0], bbox[1], true, scale);
        index = iToken + 1;
      } else {
        error(ERROR_invalidArgument);
      }
      if (index == statementLength)
        return;
    }
    int mad = getSetAxesTypeMad(index);
    if (isSyntaxCheck)
      return;
    if (tickInfo != null)
      setShapeProperty(JmolConstants.SHAPE_BBCAGE, "tickInfo", tickInfo);
    setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", mad);
  }

  private TickInfo checkTicks(int index) throws ScriptException {
    iToken = index - 1;
    if (tokAt(index) != Token.ticks)
      return null;
    TickInfo tickInfo;
    String str = " ";
    switch (tokAt(index + 1)) {
    case Token.x:
    case Token.y:
    case Token.z:
      str = parameterAsString(++index).toLowerCase();
      break;
    case Token.identifier:
      error(ERROR_invalidArgument);
    }
    if (tokAt(++index) == Token.none) {
      tickInfo = new TickInfo(null);
      tickInfo.type = str;
      iToken = index;
      return tickInfo;
    }
    tickInfo = new TickInfo(getPoint3f(index, false));
    tickInfo.type = str;
    if (tokAt(iToken + 1) == Token.format)
      tickInfo.tickLabelFormats = stringParameterSet(iToken + 2);
    if (tokAt(iToken + 1) == Token.scale) {
      if (isFloatParameter(iToken + 2)) {
        float f = floatParameter(iToken + 2);
        tickInfo.scale = new Point3f(f, f, f);
      } else if (tokAt(iToken + 2) == Token.unitcell) {
        tickInfo.scale = new Point3f(1 / viewer
            .getUnitCellInfo(JmolConstants.INFO_A), 1 / viewer
            .getUnitCellInfo(JmolConstants.INFO_B), 1 / viewer
            .getUnitCellInfo(JmolConstants.INFO_C));
        if (Float.isNaN(tickInfo.scale.x))
          tickInfo.scale = null;
        iToken += 2;
      } else {
        tickInfo.scale = getPoint3f(iToken + 2, true);
      }
    }
    if (tokAt(iToken + 1) == Token.first)
      tickInfo.first = floatParameter(iToken + 2);
    if (tokAt(iToken + 1) == Token.point)
      tickInfo.reference = centerParameter(iToken + 2);
    return tickInfo;
  }

  private void unitcell(int index) throws ScriptException {
    int icell = Integer.MAX_VALUE;
    int mad = Integer.MAX_VALUE;
    Point3f pt = null;
    TickInfo tickInfo = checkTicks(index);
    index = iToken;
    if (statementLength == index + 2) {
      if (getToken(index + 1).tok == Token.integer
          && intParameter(index + 1) >= 111)
        icell = intParameter(++index);
    } else if (statementLength > index + 1) {
      pt = (Point3f) getPointOrPlane(++index, false, true, false, true, 3, 3);
      index = iToken;
    }
    mad = getSetAxesTypeMad(++index);
    checkLast(iToken);
    if (isSyntaxCheck)
      return;
    if (icell != Integer.MAX_VALUE)
      viewer.setCurrentUnitCellOffset(icell);
    setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitCell", mad);
    if (pt != null)
      viewer.setCurrentUnitCellOffset(pt);
    if (tickInfo != null)
      setShapeProperty(JmolConstants.SHAPE_UCCAGE, "tickInfo", tickInfo);
  }

  private void frank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  private void selectionHalo(int pt) throws ScriptException {
    boolean showHalo = false;
    switch (pt == statementLength ? Token.on : getToken(pt).tok) {
    case Token.on:
    case Token.selected:
      showHalo = true;
    case Token.off:
    case Token.none:
    case Token.normal:
      setBooleanProperty("selectionHalos", showHalo);
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void save() throws ScriptException {
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      switch (tokAt(1)) {
      case Token.rotation:
        if (!isSyntaxCheck)
          viewer.saveOrientation(saveName);
        return;
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
      case Token.coord:
        if (!isSyntaxCheck)
          viewer.saveCoordinates(saveName, viewer.getSelectionSet());
        return;
      case Token.selection:
        if (!isSyntaxCheck)
          viewer.saveSelection(saveName);
        return;
      }
    }
    error(ERROR_what, "SAVE",
        "bonds? coordinates? orientation? selection? state? structure?");
  }

  private void restore() throws ScriptException {
    // restore orientation name time
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      if (getToken(1).tok != Token.orientation)
        checkLength23();
      float timeSeconds;
      switch (getToken(1).tok) {
      case Token.rotation:
        timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
        if (timeSeconds < 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck)
          viewer.restoreRotation(saveName, timeSeconds);
        return;
      case Token.orientation:
        timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
        if (timeSeconds < 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck)
          viewer.restoreOrientation(saveName, timeSeconds);
        return;
      case Token.bonds:
        if (!isSyntaxCheck)
          viewer.restoreBonds(saveName);
        return;
      case Token.coord:
        if (isSyntaxCheck)
          return;
        String script = viewer.getSavedCoordinates(saveName);
        if (script == null)
          error(ERROR_invalidArgument);
        runScript(script);
        return;
      case Token.state:
        if (isSyntaxCheck)
          return;
        String state = viewer.getSavedState(saveName);
        if (state == null)
          error(ERROR_invalidArgument);
        runScript(state);
        return;
      case Token.structure:
        if (isSyntaxCheck)
          return;
        String shape = viewer.getSavedStructure(saveName);
        if (shape == null)
          error(ERROR_invalidArgument);
        runScript(shape);
        return;
      case Token.selection:
        if (!isSyntaxCheck)
          viewer.restoreSelection(saveName);
        return;
      }
    }
    error(ERROR_what, "RESTORE",
        "bonds? coords? orientation? selection? state? structure?");
  }

  String write(Token[] args) throws ScriptException {
    String[] fullPath = new String[1];
    int pt = 0;
    boolean isApplet = viewer.isApplet();
    boolean isCommand = false;
    boolean isShow = false;
    String driverList = viewer.getExportDriverList();
    if (args == null) {
      args = statement;
      isCommand = true;
      pt++;
    } else {
      isShow = true;
    }
    int argCount = (isCommand ? statementLength : args.length);
    int tok = (isCommand && args.length == 1 ? Token.clipboard
        : tokAt(pt, args));
    int len = 0;
    int width = -1;
    int height = -1;
    String type = "SPT";
    String data = "";
    String type2 = "";
    String fileName = null;
    boolean isCoord = false;
    boolean isExport = false;
    BitSet bsFrames = null;
    String localPath = null;
    String remotePath = null;
    String val = null;
    String msg = null;
    int quality = Integer.MIN_VALUE;
    if (tok == Token.string) {
      Token t = Token.getTokenFromName(ScriptVariable.sValue(args[pt]));
      if (t != null)
        tok = t.tok;
    }
    switch (tok) {
    case Token.pointgroup:
      type = "PGRP";
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (type2.equals("draw"))
        pt++;
      break;
    case Token.quaternion:
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (Parser.isOneOf(type2, "w;x;y;z;a;r"))
        pt++;
      else
        type2 = "w";
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      boolean isDerivative = (type.indexOf("deriv") == 0 || type
          .indexOf("diff") == 0);
      if (isDerivative || type2.equals("a") || type2.equals("r")) {
        type2 += " difference" + (type.indexOf("2") >= 0 ? "2" : "");
        if (isDerivative)
          type = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      if (type.equals("draw")) {
        type2 += " draw";
        pt++;
      }
      type2 = "quaternion " + type2;
      type = "QUAT";
      break;
    case Token.ramachandran:
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (Parser.isOneOf(type2, "r;c;p"))
        pt++;
      else
        type2 = "";
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (type.equals("draw")) {
        type2 += " draw";
        pt++;
      }
      type2 = "ramachandran " + type2;
      type = "RAMA";
      break;
    case Token.function:
      type = "FUNCS";
      pt++;
      break;
    case Token.coord:
    case Token.data:
      type = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      type = "data";
      isCoord = true;
      break;
    case Token.state:
    case Token.script:
      val = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      while (val.equals("localpath") || val.equals("remotepath")) {
        if (val.equals("localpath"))
          localPath = ScriptVariable.sValue(tokenAt(++pt, args));
        else
          remotePath = ScriptVariable.sValue(tokenAt(++pt, args));
        val = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      type = "SPT";
      break;
    case Token.mo:
      type = "MO";
      pt++;
      break;
    case Token.pmesh:
      type = "PMESH";
      pt++;
      break;
    case Token.mesh:
      type = "MESH";
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
    case Token.image:
    case Token.identifier:
    case Token.string:
    case Token.frame:
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (tok == Token.image) {
        pt++;
      } else if (tok == Token.frame) {
        BitSet bsAtoms;
        if (pt + 1 < argCount && args[++pt].tok == Token.expressionBegin
            || args[pt].tok == Token.bitset) {
          bsAtoms = expression(args, pt, 0, true, false, true, true);
          pt = iToken + 1;
        } else {
          bsAtoms = viewer.getModelAtomBitSet(-1, false);
        }
        if (!isSyntaxCheck)
          bsFrames = viewer.getModelBitSet(bsAtoms, true);
      } else if (Parser.isOneOf(type, driverList.toLowerCase())) {
        // povray, maya, vrml, idtf
        pt++;
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        isExport = true;
        fileName = "Jmol." + type;
      } else if (type.equals("menu")) {
        pt++;
        type = "MENU";
      } else if (type.equals("zip")) {
        type = "ZIP";
        pt++;
      } else if (type.equals("zipall")) {
        type = "ZIPALL";
        pt++;
      } else {
        type = "(image)";
      }
      if (tokAt(pt, args) == Token.integer) {
        width = ScriptVariable.iValue(tokenAt(pt++, args));
        height = ScriptVariable.iValue(tokenAt(pt++, args));
      }
      break;
    }
    val = ScriptVariable.sValue(tokenAt(pt, args));
    if (val.equalsIgnoreCase("clipboard")) {
      if (isSyntaxCheck)
        return "";
      // if (isApplet)
      // evalError(GT._("The {0} command is not available for the applet.",
      // "WRITE CLIPBOARD"));
    } else if (Parser.isOneOf(val.toLowerCase(), "png;jpg;jpeg;jpg64;jpeg64")
        && tokAt(pt + 1, args) == Token.integer) {
      quality = ScriptVariable.iValue(tokenAt(++pt, args));
    } else if (Parser.isOneOf(val.toLowerCase(), "xyz;mol;pdb;cml")) {
      type = val.toUpperCase();
      if (pt + 1 == argCount)
        pt++;
    }

    // write [image|history|state] clipboard

    // write [optional image|history|state] [JPG quality|JPEG quality|JPG64
    // quality|PNG|PPM|SPT] "filename"
    // write script "filename"
    // write isosurface t.jvxl

    if (type.equals("(image)")
        && Parser.isOneOf(val.toUpperCase(),
            "GIF;JPG;JPG64;JPEG;JPEG64;PNG;PPM")) {
      type = val.toUpperCase();
      pt++;
    }

    if (pt + 2 == argCount) {
      data = ScriptVariable.sValue(tokenAt(++pt, args));
      if (data.length() > 0 && data.charAt(0) != '.')
        type = val.toUpperCase();
    }
    switch (tokAt(pt, args)) {
    case Token.nada:
    case Token.clipboard:
      break;
    case Token.identifier:
    case Token.string:
      fileName = ScriptVariable.sValue(tokenAt(pt, args));
      if (pt == argCount - 3 && tokAt(pt + 1, args) == Token.per) {
        // write filename.xxx gets separated as filename .spt
        // write isosurface filename.xxx also
        fileName += "." + ScriptVariable.sValue(tokenAt(pt + 2, args));
      }
      if (type != "VAR" && pt == 1)
        type = "image";
      else if (fileName.length() > 0 && fileName.charAt(0) == '.'
          && (pt == 2 || pt == 3)) {
        fileName = ScriptVariable.sValue(tokenAt(pt - 1, args)) + fileName;
        if (type != "VAR" && pt == 2)
          type = "image";
      }
      if (fileName.equalsIgnoreCase("clipboard"))
        fileName = null;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    if (type.equals("image") || type.equals("frame")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "JPG";
      if (type.equals("MNU"))
        type = "MENU";
      else if (type.equals("WRL") || type.equals("VRML")) {
        type = "Vrml";
        isExport = true;
      } else if (type.equals("X3D")) {
        type = "X3d";
        isExport = true;
      } else if (type.equals("IDTF")) {
        type = "Idtf";
        isExport = true;
      } else if (type.equals("MA")) {
        type = "Maya";
        isExport = true;
      } else if (type.equals("JVXL")) {
        type = "ISOX";
      } else if (type.equals("XJVXL")) {
        type = "ISOX";
      } else if (type.equals("MESH")) {
        type = "MESH";
      } else if (type.equals("JMOL")) {
        type = "ZIPALL";
      }
    }
    if (type.equals("data")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "XYZ";
    }
    boolean isImage = Parser.isOneOf(type, "GIF;JPEG64;JPEG;JPG64;JPG;PPM;PNG");
    if (isImage && fileName == null)
      type = "[image to clipboard]";
    else if (isImage && (isApplet && !viewer.isSignedApplet() || isShow))
      type = "JPG64";
    else if (!isImage
        && !isExport
        && !Parser
            .isOneOf(
                type,
                "ZIP;ZIPALL;SPT;HIS;MO;ISO;ISOX;MESH;PMESH;VAR;FILE;CML;XYZ;MENU;MOL;PDB;PGRP;QUAT;RAMA;FUNCS;"))
      error(
          ERROR_writeWhat,
          "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|ISOSURFACE|JMOL|MENU|MO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
              + "|RAMACHANDRAN|SPT|STATE|VAR x|ZIP|ZIPALL  CLIPBOARD",
          "CML|GIF|JPG|JPG64|JVXL|MESH|MOL|PDB|PMESH|PNG|PPM|SPT|XJVXL|XYZ|ZIP"
              + driverList.toUpperCase().replace(';', '|'));
    if (isSyntaxCheck)
      return "";
    data = type.intern();
    Object bytes = null;
    boolean doDefer = false;
    if (isExport) {
      // POV-Ray uses a BufferedWriter instead of a StringBuffer.
      boolean isPovRay = type.equals("Povray");
      data = viewer.generateOutput(data, isPovRay ? fileName : null, width,
          height);
      if (data == null || data.length() == 0)
        return "";
      if (isPovRay) {
        if (!isCommand)
          return data;
        fileName = data.substring(data.indexOf("File created: ") + 14);
        fileName = fileName.substring(0, fileName.indexOf("\n"));
        fileName = fileName.substring(0, fileName.lastIndexOf(" ("));
        msg = viewer.createImage(fileName + ".ini", "ini", data,
            Integer.MIN_VALUE, 0, 0, null, fullPath);
        if (msg != null) {
          if (!msg.startsWith("OK"))
            evalError(msg, null);
          scriptStatusOrBuffer("Created " + fullPath[0] + ":\n\n" + data);
        }
        return "";
      }
    } else if (data == "MENU") {
      data = viewer.getMenu("");
    } else if (data == "PGRP") {
      data = viewer.getPointGroupAsString(type2.equals("draw"), null, 0, 1.0f);
    } else if (data == "PDB") {
      if (isShow) {
        data = viewer.getPdbData(null, null);
      } else {
        doDefer = true;
        /*
         * OutputStream os = viewer.getOutputStream(fileName, fullPath); msg =
         * viewer.getPdbData(null, new BufferedOutputStream(os)); if (msg !=
         * null) msg = "OK " + msg + " " + fullPath[0]; try { os.close(); }
         * catch (IOException e) { // TODO }
         */
      }
    } else if (data == "FILE") {
      if (isShow)
        data = viewer.getCurrentFileAsString();
      else
        doDefer = true;
      if ("?".equals(fileName))
        fileName = "?Jmol." + viewer.getParameter("_fileType");
    } else if (data == "QUAT" || data == "RAMA") {
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        error(ERROR_multipleModelsDisplayedNotOK, "write " + type2);
      data = type2;
      if (isShow)
        data = viewer.getPdbData(modelIndex, type2);
      else
        doDefer = true;
    } else if (data == "XYZ" || data == "MOL" || data == "CML") {
      data = viewer.getData("selected", data);
    } else if (data == "FUNCS") {
      data = viewer.getFunctionCalls(null);
      type = "TXT";
    } else if (data == "VAR") {
      data = ScriptVariable.sValue((ScriptVariable) getParameter(ScriptVariable
          .sValue(tokenAt(isCommand ? 2 : 1, args)), true));
      type = "TXT";
    } else if (data == "SPT") {
      if (isCoord) {
        BitSet tainted = viewer.getTaintedAtoms(AtomCollection.TAINT_COORD);
        viewer.setAtomCoordRelative(new Point3f(0, 0, 0), null);
        data = (String) viewer.getProperty("string", "stateInfo", null);
        viewer.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
      } else {
        data = (String) viewer.getProperty("string", "stateInfo", null);
        if (localPath != null || remotePath != null)
          data = FileManager.setScriptFileReferences(data, localPath,
              remotePath, null);
      }
    } else if (data == "ZIP" || data == "ZIPALL") {
      data = (String) viewer.getProperty("string", "stateInfo", null);
      bytes = viewer.createImage(fileName, type, data, Integer.MIN_VALUE, -1,
          -1);
    } else if (data == "HIS") {
      data = viewer.getSetHistory(Integer.MAX_VALUE);
      type = "SPT";
    } else if (data == "MO") {
      data = getMoJvxl(Integer.MAX_VALUE);
      type = "XJVXL";
    } else if (data == "PMESH") {
      if ((data = getIsosurfaceJvxl(true, JmolConstants.SHAPE_PMESH)) == null)
        error(ERROR_noData);
      type = "XJVXL";
    } else if (data == "ISO" || data == "ISOX" || data == "MESH") {
      if ((data = getIsosurfaceJvxl(data == "MESH",
          JmolConstants.SHAPE_ISOSURFACE)) == null)
        error(ERROR_noData);
      type = (data.indexOf("<?xml") >= 0 ? "XJVXL" : "JVXL");
      if (!isShow)
        showString((String) getShapeProperty(
            JmolConstants.SHAPE_ISOSURFACE, "jvxlFileInfo"));
    } else {
      // image
      len = -1;
      if (quality < 0)
        quality = -1;
    }
    if (data == null && !doDefer)
      data = "";
    if (len == 0 && !doDefer)
      len = (bytes == null ? data.length()
          : bytes instanceof String ? ((String) bytes).length()
              : ((byte[]) bytes).length);
    if (isImage) {
      refresh();
      if (width < 0)
        width = viewer.getScreenWidth();
      if (height < 0)
        height = viewer.getScreenHeight();
    }
    if (!isCommand)
      return data;
    if (isShow) {
      showString(data);
    } else if (bytes != null && bytes instanceof String) {
      // load error here
      scriptStatusOrBuffer((String) bytes);
      return "";
    } else {
      if (bytes == null && (!isImage || fileName != null))
        bytes = data;
      if (doDefer)
        msg = viewer.streamFileData(fileName, type, type2);
      else
        msg = viewer.createImage(fileName, type, bytes, quality, width, height,
            bsFrames, fullPath);
      if (msg != null) {
        if (!msg.startsWith("OK"))
          evalError(msg, null);
        scriptStatusOrBuffer(msg
            + (isImage ? "; width=" + width + "; height=" + height : ""));
      }
    }
    return "";
  }

  private void show() throws ScriptException {
    String value = null;
    String str = parameterAsString(1);
    String msg = null;
    String name = null;
    int len = 2;
    Token token = getToken(1);
    int tok = (token instanceof ScriptVariable ? Token.nada : token.tok);
    if (tok == Token.string) {
      token = Token.getTokenFromName(str);
      if (token != null)
        tok = token.tok;
    }
    if (tok != Token.symop)
      checkLength(-3);
    if (statementLength == 2 && str.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(str.substring(0, str.indexOf("?"))));
      return;
    }
    switch (tok) {
    case Token.nada:
      msg = Escape.escape(((ScriptVariable) theToken).value);
      break;
    case Token.symop:
      if (statementLength > 3) {
        Point3f pt1 = centerParameter(2);
        Point3f pt2 = centerParameter(++iToken);
        if (!isSyntaxCheck)
          msg = viewer.getSymmetryOperation(null, 0, pt1, pt2);
        len = ++iToken;
      } else {
        int iop = (checkLength23() == 2 ? 0 : intParameter(2));
        if (!isSyntaxCheck)
          msg = viewer.getSymmetryOperation(null, iop, null, null);
        len = -3;
      }
      break;
    case Token.vanderwaals:
      if (statementLength == 2) {
        if (!isSyntaxCheck)
          showString(viewer.getDefaultVdwTypeNameOrData(-1));
        return;
      }
      int vdwType = JmolConstants.getVdwType(parameterAsString(2));
      if (vdwType == JmolConstants.VDW_UNKNOWN)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        showString(viewer.getDefaultVdwTypeNameOrData(vdwType));
      return;
    case Token.function:
      checkLength23();
      if (!isSyntaxCheck)
        showString(viewer.getFunctionCalls(optParameterAsString(2)));
      return;
    case Token.set:
      checkLength(2);
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
      name = parameterAsString(2);
      if (!isSyntaxCheck)
        viewer.showUrl(name);
      return;
    case Token.color:
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
        error(ERROR_multipleModelsDisplayedNotOK, "show " + theToken.value);
      msg = viewer.getPdbData(modelIndex,
          theTok == Token.quaternion ? "quaternion w" : "ramachandran");
      break;
    case Token.trace:
      if (!isSyntaxCheck)
        msg = getContext(false);
      break;
    case Token.colorscheme:
      name = optParameterAsString(2);
      if (name.length() > 0)
        len = 3;
      if (!isSyntaxCheck)
        value = viewer.getColorSchemeList(name, true);
      break;
    case Token.variables:
      if (!isSyntaxCheck)
        msg = viewer.getVariableList() + getContext(true);
      break;
    case Token.trajectory:
      if (!isSyntaxCheck)
        msg = viewer.getTrajectoryInfo();
      break;
    case Token.historylevel:
      value = "" + commandHistoryLevelMax;
      break;
    case Token.loglevel:
      value = "" + Viewer.getLogLevel();
      break;
    case Token.debugscript:
      value = "" + viewer.getDebugScript();
      break;
    case Token.strandcount:
      msg = "set strandCountForStrands "
          + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS)
          + "; set strandCountForMeshRibbon "
          + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      break;
    case Token.timeout:
      msg = viewer.showTimeout((len = statementLength) == 2 ? null
          : parameterAsString(2));
      break;
    case Token.defaultlattice:
      value = Escape.escape(viewer.getDefaultLattice());
      break;
    case Token.minimize:
      if (!isSyntaxCheck)
        msg = viewer.getMinimizationInfo();
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
      if (!isSyntaxCheck)
        msg = "set strandCountForStrands "
            + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS)
            + "; set strandCountForMeshRibbon "
            + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      break;
    case Token.hbond:
      msg = "set hbondsBackbone " + viewer.getHbondsBackbone()
          + ";set hbondsSolid " + viewer.getHbondsSolid();
      break;
    case Token.spin:
      if (!isSyntaxCheck)
        msg = viewer.getSpinState();
      break;
    case Token.ssbond:
      msg = "set ssbondsBackbone " + viewer.getSsbondsBackbone();
      break;
    case Token.display:// deprecated
    case Token.selectionhalos:
      msg = "selectionHalos "
          + (viewer.getSelectionHaloEnabled() ? "ON" : "OFF");
      break;
    case Token.hetero:
      msg = "set selectHetero " + viewer.getRasmolSetting(tok);
      break;
    case Token.addhydrogens:
      msg = Escape.escapeArray(viewer.getAdditionalHydrogens(null, true, true, null));
      break;
    case Token.hydrogen:
      msg = "set selectHydrogens " + viewer.getRasmolSetting(tok);
      break;
    case Token.ambientpercent:
    case Token.diffusepercent:
    case Token.specular:
    case Token.specularpower:
    case Token.specularexponent:
      if (!isSyntaxCheck)
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
    case Token.coord:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getCoordinateState(viewer.getSelectionSet());
        break;
      }
      String nameC = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedCoordinates(nameC);
      break;
    case Token.state:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getStateInfo();
        break;
      }
      name = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedState(name);
      break;
    case Token.structure:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getProteinStructureState();
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
            + (data[1] instanceof float[] ? Escape.escape((float[]) data[1],
                true) : data[1] instanceof float[][] ? Escape.escape(
                (float[][]) data[1], false) : "" + data[1]) + "\nend \""
            + data[0] + "\";");
      }
      break;
    case Token.spacegroup:
      Hashtable info = null;
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck) {
          info = viewer.getSpaceGroupInfo(null);
        }
      } else {
        String sg = parameterAsString(2);
        if (!isSyntaxCheck)
          info = viewer.getSpaceGroupInfo(TextFormat.simpleReplace(sg, "''",
              "\""));
      }
      if (info != null)
        msg = "" + info.get("spaceGroupInfo") + info.get("symmetryInfo");
      break;
    case Token.dollarsign:
      len = 3;
      msg = setObjectProperty();
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
        msg = (String) getShapeProperty(JmolConstants.SHAPE_DRAW,
            "command");
      break;
    case Token.file:
      // as as string
      if (statementLength == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getCurrentFileAsString();
        break;
      }
      len = 3;
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
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck) {
        viewer.removeCommand();
        msg = viewer.getSetHistory(n);
      }
      break;
    case Token.isosurface:
      if (!isSyntaxCheck)
        msg = (String) getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
            "jvxlDataXml");
      break;
    case Token.mo:
      if (optParameterAsString(2).equalsIgnoreCase("list")) {
        msg = viewer.getMoInfo(-1);
        len = 3;
      } else {
        int ptMO = ((len = statementLength) == 2 ? Integer.MIN_VALUE
            : intParameter(2));
        if (!isSyntaxCheck)
          msg = getMoJvxl(ptMO);
      }
      break;
    case Token.model:
      if (!isSyntaxCheck)
        msg = viewer.getModelInfoAsString();
      break;
    case Token.measurements:
      if (!isSyntaxCheck)
        msg = viewer.getMeasurementInfoAsString();
      break;
    case Token.translation:
    case Token.rotation:
    case Token.moveto:
      if (!isSyntaxCheck)
        msg = viewer.getOrientationText(tok, null);
      break;
    case Token.orientation:
      len = 2;
      if (statementLength > 3)
        break;
      switch (tok = tokAt(2)) {
      case Token.translation:
      case Token.rotation:
      case Token.moveto:
      case Token.nada:
        if (!isSyntaxCheck)
          msg = viewer.getOrientationText(tok, null);
        break;
      default:
        name = optParameterAsString(2);
        msg = viewer.getOrientationText(0, name);
      }
      len = statementLength;
      break;
    case Token.pdbheader:
      if (!isSyntaxCheck)
        msg = viewer.getPDBHeader();
      break;
    case Token.pointgroup:
      pointGroup();
      return;
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
          + (viewer.getZoomEnabled() ? ("" + viewer.getZoomSetting()) : "off");
      break;
    case Token.frank:
      msg = (viewer.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case Token.radius:
      str = "solventProbeRadius";
      break;
    // Chime related
    case Token.basepair:
    case Token.chain:
    case Token.sequence:
    case Token.residue:
    case Token.selected:
    case Token.group:
    case Token.atoms:
    case Token.info:
    case Token.bonds:
      msg = viewer.getChimeInfo(tok);
      break;
    // not implemented
    case Token.echo:
    case Token.fontsize:
    case Token.property: // huh? why?
    case Token.help:
    case Token.solvent:
      value = "?";
      break;
    case Token.identifier:
      if (str.equalsIgnoreCase("fileHeader")) {
        if (!isSyntaxCheck)
          msg = viewer.getPDBHeader();
      } else if (str.equalsIgnoreCase("menu")) {
        if (!isSyntaxCheck)
          value = viewer.getMenu("");
      } else if (str.equalsIgnoreCase("mouse")) {
        String qualifiers = ((len = statementLength) == 2 ? null
            : parameterAsString(2));
        if (!isSyntaxCheck)
          msg = viewer.getBindingInfo(qualifiers);
      }
      break;
    }
    checkLength(len);
    if (isSyntaxCheck)
      return;
    if (msg != null)
      showString(msg);
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null) {
      if (str.indexOf(" ") >= 0)
        showString(str);
      else
        showString(str + " = " + getParameterEscaped(str));
    }
  }

  private String getIsosurfaceJvxl(boolean asMesh, int iShape) {
    if (isSyntaxCheck)
      return "";
    return (String) getShapeProperty(iShape, asMesh ? "jvxlMeshXml"
        : "jvxlDataXml");
  }

  private String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    loadShape(JmolConstants.SHAPE_MO);
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      error(ERROR_multipleModelsDisplayedNotOK, "MO isosurfaces");
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "moData");
    if (moData == null)
      error(ERROR_moModelError);
    Integer n = (Integer) getShapeProperty(JmolConstants.SHAPE_MO,
        "moNumber");
    if (n == null || ((Integer) n).intValue() == 0) {
      setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
      setShapeProperty(JmolConstants.SHAPE_MO, "moData", moData);
    } else if (ptMO == Integer.MAX_VALUE) {
    }
    return (String) getShapeProperty(JmolConstants.SHAPE_MO, "showMO",
        ptMO);
  }

  private String extractCommandOption(String name) {
    int i = fullCommand.indexOf(name + "=");
    return (i < 0 ? null : Parser.getNextQuotedString(fullCommand, i));
  }

  private void draw() throws ScriptException {
    loadShape(JmolConstants.SHAPE_DRAW);
    switch (tokAt(1)) {
    case Token.list:
      if (listIsosurface(JmolConstants.SHAPE_DRAW))
        return;
      break;
    case Token.pointgroup:
      pointGroup();
      return;
    case Token.quaternion:
      dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
      return;
    case Token.helix:
      dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
      return;
    case Token.ramachandran:
      dataFrame(JmolConstants.JMOL_DATA_RAMACHANDRAN);
      return;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isTranslucent = false;
    boolean isIntersect = false;
    boolean isFrame = false;
    Point4f plane;
    int tokIntersect = 0;
    float translucentLevel = Float.MAX_VALUE;
    int colorArgb = Integer.MIN_VALUE;
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    Point3f center = null;
    String thisId = initIsosurface(JmolConstants.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JmolConstants.SHAPE_DRAW, "ID") == null);
    for (int i = iToken; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.unitcell:
      case Token.boundbox:
        if (isSyntaxCheck)
          break;
        Vector vp = viewer.getPlaneIntersection(theTok, null, intScale / 100f,
            0);
        intScale = 0;
        if (vp == null)
          continue;
        propertyName = "polygon";
        propertyValue = vp;
        havePoints = true;
        break;
      case Token.intersection:
        switch (getToken(++i).tok) {
        case Token.unitcell:
        case Token.boundbox:
          tokIntersect = theTok;
          isIntersect = true;
          continue;
        case Token.dollarsign:
          propertyName = "intersect";
          propertyValue = objectNameParameter(++i);
          i = iToken;
          isIntersect = true;
          havePoints = true;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      case Token.polygon:
        int nVertices = intParameter(++i);
        Point3f[] points = new Point3f[nVertices];
        for (int j = 0; j < nVertices; j++, i = iToken)
          points[j] = getPoint3f(++iToken, true);
        Vector v = new Vector();
        v.add(points);
        int nTriangles = intParameter(++i);
        int[][] polygons = new int[nTriangles][];
        for (int j = 0; j < nTriangles; j++, i = iToken) {
          float[] f = floatParameterSet(++i, 3, 4);
          polygons[j] = new int[] { (int) f[0], (int) f[1], (int) f[2],
              (f.length == 3 ? 7 : (int) f[3]) };
        }
        v.add(polygons);
        propertyName = "polygon";
        propertyValue = v;
        havePoints = true;
        break;
      case Token.symop:
        String xyz = null;
        int iSym = 0;
        plane = null;
        Point3f target = null;
        switch (tokAt(++i)) {
        case Token.string:
          xyz = stringParameter(i);
          break;
        case Token.matrix4f:
          xyz = ScriptVariable.sValue(getToken(i));
          break;
        case Token.integer:
        default:
          if (!isCenterParameter(i))
            iSym = intParameter(i++);
          if (isCenterParameter(i))
            center = centerParameter(i);
          if (isCenterParameter(iToken + 1))
            target = centerParameter(++iToken);
          if (isSyntaxCheck)
            return;
          i = iToken;
        }
        BitSet bsAtoms = null;
        if (center == null && i + 1 < statementLength) {
          center = centerParameter(++i);
          // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
          // so we also check here for the atom set to get the right model
          bsAtoms = (tokAt(i) == Token.bitset
              || tokAt(i) == Token.expressionBegin ? expression(i) : null);
          i = iToken + 1;
        }
        checkLast(iToken);
        if (!isSyntaxCheck)
          runScript((String) viewer.getSymmetryInfo(bsAtoms, xyz, iSym, center,
              target, thisId, Token.draw));
        return;
      case Token.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case Token.leftbrace:
      case Token.point4f:
      case Token.point3f:
        // {X, Y, Z}
        if (theTok == Token.point4f || !isPoint3f(i)) {
          propertyValue = getPoint4f(i);
          if (isFrame) {
            checkLast(iToken);
            if (!isSyntaxCheck)
              runScript((new Quaternion((Point4f) propertyValue)).draw(
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new Point3f() : center), intScale / 100f));
            return;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = iToken;
        havePoints = true;
        break;
      case Token.hkl:
      case Token.plane:
        if (!havePoints && !isIntersect && tokIntersect == 0
            && theTok != Token.hkl) {
          propertyName = "plane";
          break;
        }
        if (theTok == Token.plane) {
          plane = planeParameter(++i);
        } else {
          plane = hklParameter(++i);
        }
        i = iToken;
        if (tokIntersect != 0) {
          if (isSyntaxCheck)
            break;
          Vector vpc = viewer.getPlaneIntersection(tokIntersect, plane,
              intScale / 100f, 0);
          intScale = 0;
          if (vpc == null)
            continue;
          propertyName = "polygon";
          propertyValue = vpc;
        } else {
          propertyValue = plane;
          propertyName = "planedef";
        }
        havePoints = true;
        break;
      case Token.linedata:
        propertyName = "lineData";
        propertyValue = floatParameterSet(++i, 0, Integer.MAX_VALUE);
        i = iToken;
        havePoints = true;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        propertyName = "atomSet";
        propertyValue = expression(i);
        if (isFrame)
          center = centerParameter(i);
        i = iToken;
        havePoints = true;
        break;
      case Token.list:
        propertyName = "modelBasedPoints";
        propertyValue = theToken.value;
        havePoints = true;
        break;
      case Token.comma: // ignore -- necessary between { } and [x y]
        break;
      case Token.leftsquare:
        // [x y] or [x y %]
        propertyValue = xypParameter(i);
        if (propertyValue != null) {
          i = iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = true;
        break;
      case Token.rightsquare:
        if (!isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = false;
        break;
      case Token.reverse:
        propertyName = "reverse";
        break;
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case Token.vector:
        propertyName = "vector";
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
      case Token.modelindex:
        propertyName = "modelIndex";
        propertyValue = new Integer(intParameter(++i));
        break;
      case Token.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = new Integer(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case Token.scale:
        if (++i >= statementLength)
          error(ERROR_numberExpected);
        switch (getToken(i).tok) {
        case Token.integer:
          intScale = intParameter(i);
          continue;
        case Token.decimal:
          intScale = (int) (floatParameter(i) * 100);
          continue;
        }
        error(ERROR_numberExpected);
        break;
      case Token.id:
        thisId = setShapeId(JmolConstants.SHAPE_DRAW, ++i, idSeen);
        isWild = (getShapeProperty(JmolConstants.SHAPE_DRAW, "ID") == null);
        i = iToken;
        break;
      case Token.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case Token.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case Token.offset:
        Point3f pt = getPoint3f(++i, true);
        i = iToken;
        propertyName = "offset";
        propertyValue = pt;
        break;
      case Token.crossed:
        propertyName = "crossed";
        break;
      case Token.width:
        propertyValue = new Float(floatParameter(++i));
        propertyName = "width";
        swidth = (String) propertyName + " " + propertyValue;
        break;
      case Token.line:
        propertyName = "line";
        propertyValue = Boolean.TRUE;
        break;
      case Token.curve:
        propertyName = "curve";
        break;
      case Token.arc:
        propertyName = "arc";
        break;
      case Token.arrow:
        propertyName = "arrow";
        break;
      case Token.circle:
        propertyName = "circle";
        break;
      case Token.cylinder:
        propertyName = "cylinder";
        break;
      case Token.vertices:
        propertyName = "vertices";
        break;
      case Token.nohead:
        propertyName = "nohead";
        break;
      case Token.rotate45:
        propertyName = "rotate45";
        break;
      case Token.perpendicular:
        propertyName = "perp";
        break;
      case Token.diameter:
        float f = floatParameter(++i);
        propertyValue = new Float(f);
        propertyName = (tokAt(i) == Token.decimal ? "width" : "diameter");
        swidth = (String) propertyName
            + (tokAt(i) == Token.decimal ? " " + f : " " + ((int) f));
        break;
      case Token.dollarsign:
        // $drawObject[m]
        if ((tokAt(i + 2) == Token.leftsquare || isFrame)) {
          Point3f pto = center = centerParameter(i);
          i = iToken;
          propertyName = "coord";
          propertyValue = pto;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case Token.color:
      case Token.translucent:
      case Token.opaque:
        if (theTok != Token.color)
          --i;
        if (tokAt(i + 1) == Token.translucent) {
          i++;
          isTranslucent = true;
          if (isFloatParameter(i + 1))
            translucentLevel = getTranslucentLevel(++i);
        } else if (tokAt(i + 1) == Token.opaque) {
          i++;
          isTranslucent = true;
          translucentLevel = 0;
        }
        if (isColorParam(i + 1)) {
          colorArgb = getArgbParam(++i);
          i = iToken;
        } else if (!isTranslucent) {
          error(ERROR_invalidArgument);
        }
        idSeen = true;
        continue;
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, 0, theTok)) {
          if (theTok == Token.times || Token.tokAttr(theTok, Token.identifier)) {
            thisId = setShapeId(JmolConstants.SHAPE_DRAW, i, idSeen);
            i = iToken;
            break;
          }
          error(ERROR_invalidArgument);
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = iToken;
        continue;
      }
      idSeen = (theTok != Token.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(
            intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DRAW, propertyName, propertyValue);
    }
    if (havePoints) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
    }
    if (colorArgb != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_DRAW, "color",
          new Integer(colorArgb));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_DRAW, "", "translucent",
          translucentLevel, null);
    if (intScale != 0) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "scale", new Integer(intScale));
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, iptDisplayProperty,
          getToken(iptDisplayProperty).tok))
        error(ERROR_invalidArgument);
    }
  }

  private void polyhedra() throws ScriptException {
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int nAtomSets = 0;
    loadShape(JmolConstants.SHAPE_POLYHEDRA);
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
      case Token.delete:
      case Token.on:
      case Token.off:
        if (i + 1 != statementLength || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && setPropertyName == "to")
          error(ERROR_incompatibleArguments);
        propertyName = parameterAsString(i);
        onOffDelete = true;
        break;
      case Token.opEQ:
      case Token.comma:
        continue;
      case Token.bonds:
        if (nAtomSets > 0)
          error(ERROR_invalidParameterOrder);
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case Token.radius:
        decimalPropertyName = "radius";
        continue;
      case Token.integer:
      case Token.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          error(ERROR_invalidParameterOrder);
        if (theTok == Token.integer) {
          if (decimalPropertyName == "radius_") {
            propertyName = "nVertices";
            propertyValue = new Integer(intParameter(i));
            needsGenerating = true;
            break;
          }
        }
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = new Float(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (typeSeen)
          error(ERROR_invalidParameterOrder);
        if (++nAtomSets > 2)
          error(ERROR_badArgumentCount);
        if (setPropertyName == "to")
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = expression(i);
        i = iToken;
        break;
      case Token.to:
        if (nAtomSets > 1)
          error(ERROR_invalidParameterOrder);
        if (getToken(i + 1).tok == Token.bitset) {
          propertyName = "toBitSet";
          propertyValue = getToken(++i).value;
          needsGenerating = true;
          break;
        } else if (!needsGenerating) {
          error(ERROR_insufficientArguments);
        }
        setPropertyName = "to";
        continue;
      case Token.facecenteroffset:
        if (!needsGenerating)
          error(ERROR_insufficientArguments);
        decimalPropertyName = "faceCenterOffset";
        isDesignParameter = true;
        continue;
      case Token.distancefactor:
        if (!needsGenerating)
          error(ERROR_insufficientArguments);
        decimalPropertyName = "distanceFactor";
        isDesignParameter = true;
        continue;
      case Token.color:
      case Token.translucent:
      case Token.opaque:
        isTranslucent = false;
        if (theTok != Token.color)
          --i;
        if (tokAt(i + 1) == Token.translucent) {
          i++;
          isTranslucent = true;
          if (isFloatParameter(++i))
            translucentLevel = getTranslucentLevel(i);
        } else if (tokAt(i + 1) == Token.opaque) {
          i++;
          isTranslucent = true;
          translucentLevel = 0;
        }
        if (isColorParam(i + 1)) {
          color = getArgbParam(i);
          i = iToken;
        } else if (!isTranslucent)
          error(ERROR_invalidArgument);
        continue;
      case Token.collapsed:
      case Token.flat:
        propertyName = "collapsed";
        propertyValue = (theTok == Token.collapsed ? Boolean.TRUE
            : Boolean.FALSE);
        if (typeSeen)
          error(ERROR_incompatibleArguments);
        typeSeen = true;
        break;
      case Token.noedges:
      case Token.edges:
      case Token.frontedges:
        if (edgeParameterSeen)
          error(ERROR_incompatibleArguments);
        propertyName = parameterAsString(i);
        edgeParameterSeen = true;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen)
      error(ERROR_insufficientArguments);
    if (needsGenerating)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "generate", null);
    if (color != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "colorThis", new Integer(
          color));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_POLYHEDRA, "", "translucent",
          translucentLevel, null);
  }

  private void lcaoCartoon() throws ScriptException {
    loadShape(JmolConstants.SHAPE_LCAOCARTOON);
    if (tokAt(1) == Token.list
        && listIsosurface(JmolConstants.SHAPE_LCAOCARTOON))
      return;
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "init", fullCommand);
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
      case Token.cap:
      case Token.slab:
        propertyName = (String) theToken.value;
        if (tokAt(i + 1) == Token.off)
          iToken = i + 1;
        else
          propertyValue = getCapSlabObject(i);
        i = iToken;
        break;
      case Token.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JmolConstants.SHAPE_LCAOCARTOON);
        return;
      case Token.rotate:
        float degx = 0;
        float degy = 0;
        float degz = 0;
        switch (getToken(++i).tok) {
        case Token.x:
          degx = floatParameter(++i) * JmolConstants.radiansPerDegree;
          break;
        case Token.y:
          degy = floatParameter(++i) * JmolConstants.radiansPerDegree;
          break;
        case Token.z:
          degz = floatParameter(++i) * JmolConstants.radiansPerDegree;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        propertyName = "rotationAxis";
        propertyValue = new Vector3f(degx, degy, degz);
        break;
      case Token.on:
      case Token.display:
      case Token.displayed:
        propertyName = "on";
        break;
      case Token.off:
      case Token.hide:
      case Token.hidden:
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
      case Token.spacefill:
      case Token.string:
        propertyValue = parameterAsString(i).toLowerCase();
        if (propertyValue.equals("spacefill"))
          propertyValue = "cpk";
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
          if (propertyValue.equals("spacefill"))
            propertyValue = "cpk";
        }
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.lonepair:
      case Token.lp:
        propertyName = "lonePair";
        break;
      case Token.radical:
      case Token.rad:
        propertyName = "radical";
        break;
      case Token.molecular:
        propertyName = "molecular";
        break;
      case Token.create:
        propertyValue = parameterAsString(++i);
        propertyName = "create";
        if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case Token.id:
        propertyValue = getShapeNameParameter(++i);
        i = iToken;
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "lcaoID";
        break;
      default:
        if (theTok == Token.times || Token.tokAttr(theTok, Token.identifier)) {
          if (theTok != Token.times)
            propertyValue = parameterAsString(i);
          if (idSeen)
            error(ERROR_invalidArgument);
          propertyName = "lcaoID";
          break;
        }
        break;
      }
      if (theTok != Token.delete)
        idSeen = true;
      if (propertyName == null)
        error(ERROR_invalidArgument);
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, propertyName,
          propertyValue);
    }
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "clear", null);
  }

  private Object getCapSlabObject(int i) throws ScriptException {
    Object propertyValue = null;
    if (tokAt(i + 1) == Token.boundbox) {
      propertyValue = BoxInfo.getCriticalPoints(viewer.getBoundBoxVertices(),
          null);
      iToken = i + 1;
    } else if (tokAt(i + 1) == Token.unitcell) {
      SymmetryInterface unitCell = viewer.getCurrentUnitCell();
      if (unitCell == null)
        error(ERROR_invalidArgument);
      propertyValue = BoxInfo.getCriticalPoints(unitCell.getUnitCellVertices(),
          unitCell.getCartesianOffset());
      iToken = i + 1;
    } else {
      Point4f plane = planeParameter(++i);
      float off = (isFloatParameter(iToken + 1) ? floatParameter(++iToken)
          : Float.NaN);
      if (!Float.isNaN(off))
        plane.w -= off;
      propertyValue = plane;
    }
    return propertyValue;
  }

  private boolean mo(boolean isInitOnly) throws ScriptException {
    int offset = Integer.MAX_VALUE;
    BitSet bsModels = viewer.getVisibleFramesBitSet();
    Vector propertyList = new Vector();
    int i = 1;
    if (tokAt(1) == Token.model || tokAt(1) == Token.frame) {
      i = modelNumberParameter(2);
      if (i < 0)
        error(ERROR_invalidArgument);
      bsModels.clear();
      bsModels.set(i);
      i = 3;
    }
    for (int iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels.nextSetBit(iModel + 1)) {
      loadShape(JmolConstants.SHAPE_MO);
      if (tokAt(i) == Token.list && listIsosurface(JmolConstants.SHAPE_MO))
        return true;
      setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(iModel));
      String title = null;
      int moNumber = ((Integer) getShapeProperty(JmolConstants.SHAPE_MO,
          "moNumber")).intValue();
      if (isInitOnly)
        return true;// (moNumber != 0);
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.integer:
        moNumber = intParameter(i);
        break;
      case Token.next:
        moNumber = Token.next;
        break;
      case Token.prev:
        moNumber = Token.prev;
        break;
      case Token.color:
        setColorOptions(i + 1, JmolConstants.SHAPE_MO, 2);
        break;
      case Token.plane:
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = planeParameter(i + 1);
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(i + 1));
        break;
      case Token.cutoff:
        if (tokAt(i + 1) == Token.plus) {
          propertyName = "cutoffPositive";
          propertyValue = new Float(floatParameter(i + 2));
        } else {
          propertyName = "cutoff";
          propertyValue = new Float(floatParameter(i + 1));
        }
        break;
      case Token.debug:
        propertyName = "debug";
        break;
      case Token.noplane:
        propertyName = "plane";
        break;
      case Token.pointsperangstrom:
      case Token.resolution:
        propertyName = "resolution";
        propertyValue = new Float(floatParameter(i + 1));
        break;
      case Token.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      case Token.titleformat:
        if (i + 1 < statementLength && tokAt(i + 1) == Token.string) {
          propertyName = "titleFormat";
          propertyValue = parameterAsString(i + 1);
        }
        break;
      case Token.homo:
      case Token.lumo:
        if ((offset = moOffset(i)) == Integer.MAX_VALUE)
          error(ERROR_invalidArgument);
        moNumber = 0;
        break;
      case Token.identifier:
        error(ERROR_invalidArgument);
      default:
        int ipt = iToken;
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, 0, theTok))
          error(ERROR_invalidArgument);
        setShapeProperty(JmolConstants.SHAPE_MO, "setProperties", propertyList);
        setMeshDisplayProperty(JmolConstants.SHAPE_MO, ipt, tokAt(ipt));
        return true;
      }
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
      if (moNumber != Integer.MAX_VALUE) {
        if (tokAt(i + 1) == Token.string)
          title = parameterAsString(i + 1);
        if (!isSyntaxCheck)
          viewer.setCursor(Viewer.CURSOR_WAIT);
        setMoData(propertyList, moNumber, offset, iModel, title);
        addShapeProperty(propertyList, "finalize", null);
        setShapeProperty(JmolConstants.SHAPE_MO, "setProperties", propertyList);
      }

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
        setShapeTranslucency(iShape, null, "translucent", value, null);
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
        error(ERROR_invalidArgument);
      } else {
        break;
      }
    }
    return translucency;
  }

  private int moOffset(int index) throws ScriptException {
    boolean isHomo = (getToken(index).tok == Token.homo);
    int offset = Integer.MAX_VALUE;
    offset = (isHomo ? 0 : 1);
    int tok = tokAt(++index);
    if (tok == Token.integer && intParameter(index) < 0)
      offset += intParameter(index);
    else if (tok == Token.plus)
      offset += intParameter(++index);
    else if (tok == Token.minus)
      offset -= intParameter(++index);
    return offset;
  }

  private void setMoData(Vector propertyList, int moNumber, int offset,
                         int modelIndex, String title) throws ScriptException {
    if (isSyntaxCheck)
      return;
    if (modelIndex < 0) {
      modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        error(ERROR_multipleModelsDisplayedNotOK, "MO isosurfaces");
    }
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "jmolSurfaceInfo");
    int firstMoNumber = moNumber;
    if (moData != null && ((String) moData.get("surfaceDataType")).equals("mo")) {
      // loadShape(shape);
      // setShapeProperty(shape, "init", new Integer(modelIndex));
    } else {
      moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "moData");
      if (moData == null)
        error(ERROR_moModelError);
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue()
          : 0);
      if (moNumber == Token.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == Token.next)
        moNumber = lastMoNumber + 1;
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
        } else {
          for (int i = 0; i < nOrb; i++) {
            Hashtable mo = (Hashtable) mos.get(i);
            if (!mo.containsKey("occupancy"))
              error(ERROR_moOccupancy);
            if (((Float) mo.get("occupancy")).floatValue() == 0) {
              moNumber = i + offset;
              break;
            }
          }
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        error(ERROR_moIndex, "" + nOrb);
    }
    moData.put("lastMoNumber", new Integer(moNumber));
    addShapeProperty(propertyList, "moData", moData);
    if (title != null)
      addShapeProperty(propertyList, "title", title);
    if (firstMoNumber < 0)
      addShapeProperty(propertyList, "charges", viewer.getAtomicCharges());
    addShapeProperty(propertyList, "molecularOrbital", new Integer(
        firstMoNumber < 0 ? -moNumber : moNumber));
    addShapeProperty(propertyList, "clear", null);
  }

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    setShapeProperty(iShape, "init", fullCommand);
    iToken = 0;
    if (tokAt(1) == Token.delete || tokAt(2) == Token.delete
        && tokAt(++iToken) == Token.all) {
      setShapeProperty(iShape, "delete", null);
      iToken += 2;
      if (statementLength > iToken) {
        setShapeProperty(iShape, "init", fullCommand);
        setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      }
      return null;
    }
    iToken = 1;
    if (!setMeshDisplayProperty(iShape, 0, tokAt(1))) {
      setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      if (iShape != JmolConstants.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
      if (tokAt(2) == Token.times && tokAt(1) != Token.id) {
        String id = setShapeId(iShape, 1, false);
        iToken++;
        return id;
      }
    }
    return null;
  }

  private String getNextComment() {

    String nextCommand = getCommand(pc + 1, false, true);
    return (nextCommand.startsWith("#") ? nextCommand : "");
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    if (getToken(1).value instanceof String[]) // not just the word "list"
      return false;
    checkLength(2);
    if (!isSyntaxCheck)
      showString((String) getShapeProperty(iShape, "list"));
    return true;
  }

  private void isosurface(int iShape) throws ScriptException {
    loadShape(iShape);
    if (tokAt(1) == Token.list && listIsosurface(iShape))
      return;
    int colorRangeStage = 0;
    int signPt = 0;
    int iptDisplayProperty = 0;
    boolean isIsosurface = (iShape == JmolConstants.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JmolConstants.SHAPE_PMESH);
    boolean isPlot3d = (iShape == JmolConstants.SHAPE_PLOT3D);

    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isCavity = false;
    boolean haveRadius = false;
    boolean isFxy = false;
    boolean isColorMesh = false;
    float[] nlmZ = new float[5];
    float[] data = null;
    int thisSetNumber = 0;
    int nFiles = 0;
    int nX, nY, nZ, ptX, ptY;
    int ptSigma = 0;
    int ptCutoff = 0;
    int ptWithin = 0;
    BitSet bs;
    Vector v;
    Point3f[] pts;
    String str = null;
    int modelIndex = (isSyntaxCheck ? 0 : viewer.getCurrentModelIndex());
    if (!isSyntaxCheck)
      viewer.setCursor(Viewer.CURSOR_WAIT);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && getShapeProperty(iShape, "ID") == null);
    boolean isColorSchemeTranslucent = false;
    String translucency = null;
    String colorScheme = null;
    short[] discreteColixes = null;
    Vector propertyList = new Vector();
    boolean defaultMesh = false;
    if (isPmesh || isPlot3d)
      addShapeProperty(propertyList, "fileType", "Pmesh");
    for (int i = iToken; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (i != signPt)
          error(ERROR_invalidParameterOrder);
        addShapeProperty(propertyList,
            (isColorMesh ? "colorMesh" : "colorRGB"), new Integer(
                getArgbParam(i)));
        i = iToken;
        signPt = i + 1;
        idSeen = true;
        continue;
      }
      String propertyName = null;
      Object propertyValue = null;
      getToken(i);
      if (theTok == Token.identifier
          && (str = parameterAsString(i)).equalsIgnoreCase("inline"))
        theTok = Token.string;
      switch (theTok) {
      case Token.boundbox:
        if (!isSyntaxCheck) {
          if (fullCommand.indexOf("# BBOX=") >= 0) {
            String[] bbox = TextFormat.split(extractCommandOption("# BBOX"),
                ',');
            pts = new Point3f[] { (Point3f) Escape.unescapePoint(bbox[0]),
                (Point3f) Escape.unescapePoint(bbox[1]) };
          } else {
            pts = viewer.getBoundBoxVertices();
          }
          addShapeProperty(propertyList, "commandOption", "BBOX=\""
              + Escape.escape(pts[0]) + ","
              + Escape.escape(pts[pts.length - 1]) + "\"");
          addShapeProperty(propertyList, "boundingBox", pts);
        }
        continue;
      case Token.pmesh:
        isPmesh = true;
        addShapeProperty(propertyList, "fileType", "Pmesh");
        continue;
      case Token.within:
        ptWithin = i;
        float distance = floatParameter(++i);
        Point3f ptc = centerParameter(++i);
        i = iToken;
        if (fullCommand.indexOf("# WITHIN=") >= 0)
          bs = Escape.unescapeBitset(extractCommandOption("# WITHIN"));
        else
          bs = (expressionResult instanceof BitSet ? (BitSet) expressionResult
              : null);
        getWithinDistanceVector(propertyList, distance, ptc, bs);
        continue;
      case Token.property:
        addShapeProperty(propertyList, "propertySmoothing", viewer
            .getIsosurfacePropertySmoothing() ? Boolean.TRUE : Boolean.FALSE);
        str = parameterAsString(i);
        propertyName = "property";
        if (!isCavity && str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (isSyntaxCheck)
            continue;
          data = viewer.getDataFloat(str);
          if (data == null)
            error(ERROR_invalidArgument);
          propertyValue = data;
          break;
        }
        int tokProperty = getToken(++i).tok;
        int atomCount = viewer.getAtomCount();
        data = (isCavity ? new float[0] : new float[atomCount]);
        if (isCavity)// not implemented: && tokProperty !=
          // Token.surfacedistance)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck && !isCavity) {
          Atom[] atoms = viewer.getModelSet().atoms;
          viewer.autoCalculate(tokProperty);
          for (int iAtom = atomCount; --iAtom >= 0;) {
            data[iAtom] = Atom.atomPropertyFloat(viewer, atoms[iAtom],
                tokProperty);
          }
        }
        if (tokProperty == Token.color)
          colorScheme = "colorRGB";
        propertyValue = data;
        break;
      case Token.model:
        if (surfaceObjectSeen)
          error(ERROR_invalidArgument);
        modelIndex = modelNumberParameter(++i);
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
      case Token.set:
        thisSetNumber = intParameter(++i);
        break;
      case Token.offset:
        propertyName = "offset";
        propertyValue = centerParameter(++i);
        i = iToken;
        break;
      case Token.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        i = iToken;
        break;
      case Token.color:
        if (tokAt(i + 1) == Token.density) {
          i++;
          propertyName = "colorDensity";
          break;
        }
        /*
         * "color" now is just used as an equivalent to "sign" and as an
         * introduction to "absolute" any other use is superfluous; it has been
         * replaced with MAP for indicating "use the current surface" because
         * the term COLOR is too general.
         */

        colorRangeStage = 0;
        if (getToken(i + 1).tok == Token.string) {
          colorScheme = parameterAsString(++i);
          if (colorScheme.indexOf(" ") > 0) {
            discreteColixes = Graphics3D.getColixArray(colorScheme);
            if (discreteColixes == null)
              error(ERROR_badRGBColor);
          }
        } else if (theTok == Token.mesh) {
          isColorMesh = true;
          i++;
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
        else
          ptCutoff = i;
        break;
      case Token.ionic:
      case Token.vanderwaals:
        RadiusData rd = encodeRadiusParameter(i, false);
        if (Float.isNaN(rd.value))
          rd.value = 100;
        propertyValue = rd;
        propertyName = "radius";
        haveRadius = true;
        i = iToken;
        break;
      case Token.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = planeParameter(++i);
        i = iToken;
        break;
      case Token.scale3d:
        propertyName = "scale3d";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.all:
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "thisID";
        break;
      case Token.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        try {
          propertyValue = getPoint4f(i);
          propertyName = "ellipsoid";
          i = iToken;
          break;
        } catch (ScriptException e) {
        }
        try {
          propertyName = "ellipsoid";
          propertyValue = floatParameterSet(i, 6, 6);
          i = iToken;
          break;
        } catch (ScriptException e) {
        }
        bs = expression(i);
        int iAtom = bs.nextSetBit(0);
        Atom[] atoms = viewer.getModelSet().atoms;
        if (iAtom >= 0)
          propertyValue = atoms[iAtom].getEllipsoid();
        if (propertyValue == null)
          return;
        i = iToken;
        propertyName = "ellipsoid";
        if (!isSyntaxCheck)
          addShapeProperty(propertyList, "center", viewer.getAtomPoint3f(iAtom));
        break;
      case Token.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = hklParameter(++i);
        i = iToken;
        break;
      case Token.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        addShapeProperty(propertyList, "lcaoType", lcaoType);
        switch (getToken(++i).tok) {
        case Token.bitset:
        case Token.expressionBegin:
          propertyName = "lcaoCartoon";
          bs = expression(i);
          i = iToken;
          int atomIndex = bs.nextSetBit(0);
          modelIndex = 0;
          Point3f pt;
          if (atomIndex < 0) {
            if (!isSyntaxCheck)
              error(ERROR_expressionExpected);
            pt = new Point3f();
          } else {
            modelIndex = viewer.getAtomModelIndex(atomIndex);
            pt = viewer.getAtomPoint3f(atomIndex);
          }
          addShapeProperty(propertyList, "modelIndex", new Integer(modelIndex));
          Vector3f[] axes = { new Vector3f(), new Vector3f(), new Vector3f(pt),
              new Vector3f() };
          if (!isSyntaxCheck)
            viewer.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                lcaoType, false);
          propertyValue = axes;
          break;
        default:
          error(ERROR_expressionExpected);
        }
        break;
      case Token.mo:
        // mo 1-based-index
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        switch (tokAt(++i)) {
        case Token.nada:
          error(ERROR_badArgumentCount);
        case Token.homo:
        case Token.lumo:
          if ((offset = moOffset(i)) != Integer.MAX_VALUE) {
            moNumber = 0;
            i = iToken;
          }
          break;
        case Token.integer:
          moNumber = intParameter(i);
          break;
        }
        setMoData(propertyList, moNumber, offset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case Token.mep:
        float[] partialCharges = null;
        try {
          partialCharges = viewer.getPartialCharges();
        } catch (Exception e) {
        }
        if (!isSyntaxCheck && partialCharges == null)
          error(ERROR_noPartialCharges);
        surfaceObjectSeen = true;
        propertyName = "mep";
        propertyValue = partialCharges;
        break;
      case Token.sasurface:
      case Token.solvent:
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent",
            lookupIdentifierValue("solvent"));
        propertyName = (theTok == Token.sasurface ? "sasurface" : "solvent");
        float radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer
            .getSolventProbeRadius());
        propertyValue = new Float(radius);
        break;
      case Token.volume:
        doCalcVolume = !isSyntaxCheck;
        break;
      case Token.id:
        setShapeId(iShape, ++i, idSeen);
        isWild = (getShapeProperty(iShape, "ID") == null);
        i = iToken;
        break;
      case Token.colorscheme:
        // either order OK -- documented for "rwb" TRANSLUCENT
        if (tokAt(i + 1) == Token.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        colorScheme = parameterAsString(++i);
        if (tokAt(i + 1) == Token.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        break;
      case Token.addhydrogens:
        propertyName = "addHydrogens";
        propertyValue = Boolean.TRUE;
        break;
      case Token.angstroms:
        propertyName = "angstroms";
        break;
      case Token.anisotropy:
        propertyName = "anisotropy";
        propertyValue = getPoint3f(++i, false);
        i = iToken;
        break;
      case Token.area:
        doCalcArea = !isSyntaxCheck;
        break;
      case Token.atomicorbital:
      case Token.orbital:
        surfaceObjectSeen = true;
        nlmZ[0] = intParameter(++i);
        nlmZ[1] = intParameter(++i);
        nlmZ[2] = intParameter(++i);
        nlmZ[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
        propertyName = "hydrogenOrbital";
        propertyValue = nlmZ;
        break;
      case Token.binary:
        // for PMESH, specifically
        // ignore for now
        continue;
      case Token.blockdata:
        propertyName = "blockData";
        propertyValue = Boolean.TRUE;
        break;
      case Token.cap:
      case Token.slab:
        propertyName = (String) theToken.value;
        propertyValue = getCapSlabObject(i);
        i = iToken;
        break;
      case Token.cavity:
        if (!isIsosurface)
          error(ERROR_invalidArgument);
        isCavity = true;
        if (isSyntaxCheck)
          continue;
        float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 1.2f);
        float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 10f);
        if (envelopeRadius > 10f)
          integerOutOfRange(0, 10);
        addShapeProperty(propertyList, "envelopeRadius", new Float(
            envelopeRadius));
        addShapeProperty(propertyList, "cavityRadius", new Float(cavityRadius));
        propertyName = "cavity";
        break;
      case Token.contour:
      case Token.contours:
        propertyName = "contour";
        switch (tokAt(i + 1)) {
        case Token.discrete:
          propertyValue = floatParameterSet(i + 2, 1, Integer.MAX_VALUE);
          i = iToken;
          break;
        case Token.increment:
          Point3f pt = getPoint3f(i + 2, false);
          if (pt.z <= 0)
            error(ERROR_invalidArgument); // from to step
          propertyValue = pt;
          i = iToken;
          break;
        default:
          propertyValue = new Integer(
              tokAt(i + 1) == Token.integer ? intParameter(++i) : 0);
        }
        break;
      case Token.cutoff:
        if (++i < statementLength && getToken(i).tok == Token.plus) {
          propertyName = "cutoffPositive";
          propertyValue = new Float(floatParameter(++i));
        } else {
          propertyName = "cutoff";
          propertyValue = new Float(floatParameter(i));
        }
        ptCutoff = i;
        break;
      case Token.downsample:
        propertyName = "downsample";
        propertyValue = new Integer(intParameter(++i));
        break;
      case Token.eccentricity:
        propertyName = "eccentricity";
        propertyValue = getPoint4f(++i);
        i = iToken;
        break;
      case Token.ed:
        setMoData(propertyList, -1, 0, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case Token.debug:
      case Token.nodebug:
        propertyName = "debug";
        propertyValue = (theTok == Token.debug ? Boolean.TRUE : Boolean.FALSE);
        break;
      case Token.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case Token.fullplane:
        propertyName = "fullPlane";
        propertyValue = Boolean.TRUE;
        break;
      case Token.functionxy:
        // isosurface functionXY "functionName"|"data2d_xxxxx"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        Vector vxy = new Vector();
        if (getToken(++i).tok != Token.string)
          error(ERROR_what,
              "functionXY must be followed by a function name in quotes.");
        String name = parameterAsString(i++);
        // override of function or data name when saved as a state
        String dName = extractCommandOption("# DATA" + (isFxy ? "2" : ""));
        if (dName != null)
          name = dName;
        boolean isXYZ = (name.indexOf("data2d_") == 0);
        vxy.addElement(name); // (0) = name
        vxy.addElement(getPoint3f(i, false)); // (1) = {origin}
        Point4f pt4;
        ptX = ++iToken;
        vxy.addElement(pt4 = getPoint4f(ptX)); // (2) = {ni ix iy iz}
        nX = (int) pt4.x;
        ptY = ++iToken;
        vxy.addElement(pt4 = getPoint4f(ptY)); // (3) = {nj jx jy jz}
        nY = (int) pt4.x;
        vxy.addElement(getPoint4f(++iToken)); // (4) = {nk kx ky kz}
        if (nX == 0 || nY == 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck) {
          float[][] fdata = (isXYZ ? viewer.getDataFloat2D(name) : viewer
              .functionXY(name, nX, nY));
          if (isXYZ) {
            nX = (fdata == null ? 0 : fdata.length);
            nY = 3;
          } else {
            nX = Math.abs(nX);
            nY = Math.abs(nY);
          }
          if (fdata == null) {
            iToken = ptX;
            error(ERROR_what, "fdata is null.");
          }
          if (fdata.length != nX && !isXYZ) {
            iToken = ptX;
            error(ERROR_what, "fdata length is not correct: " + fdata.length
                + " " + nX + ".");
          }
          for (int j = 0; j < nX; j++) {
            if (fdata[j] == null) {
              iToken = ptY;
              error(ERROR_what, "fdata[" + j + "] is null.");
            }
            if (fdata[j].length != nY) {
              iToken = ptY;
              error(ERROR_what, "fdata[" + j + "] is not the right length: "
                  + fdata[j].length + " " + nY + ".");
            }
          }
          vxy.addElement(fdata); // (5) = float[][] data
        }
        i = iToken;
        propertyName = "functionXY";
        propertyValue = vxy;
        isFxy = surfaceObjectSeen = true;
        break;
      case Token.functionxyz:
        // isosurface functionXYZ "functionName"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        v = new Vector();
        if (getToken(++i).tok != Token.string)
          error(ERROR_what,
              "functionXYZ must be followed by a function name in quotes.");
        String fName = parameterAsString(i++);
        // override of function or data name when saved as a state
        String dataName = extractCommandOption("# DATA" + (isFxy ? "2" : ""));
        if (dataName != null)
          fName = dataName;
        boolean isXYZV = (fName.indexOf("data3d_") == 0);
        if (dataName != null)
          fName = dataName;
        v.addElement(fName); // (0) = name
        v.addElement(getPoint3f(i, false)); // (1) = {origin}
        Point4f pt;
        ptX = ++iToken;
        v.addElement(pt = getPoint4f(ptX)); // (2) = {ni ix iy iz}
        nX = (int) pt.x;
        ptY = ++iToken;
        v.addElement(pt = getPoint4f(ptY)); // (3) = {nj jx jy jz}
        nY = (int) pt.x;
        v.addElement(pt = getPoint4f(++iToken)); // (4) = {nk kx ky kz}
        nZ = (int) pt.x;
        if (nX == 0 || nY == 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck) {
          float[][][] xyzdata = (isXYZV ? viewer.getDataFloat3D(fName) : viewer
              .functionXYZ(fName, nX, nY, nZ));
          nX = Math.abs(nX);
          nY = Math.abs(nY);
          if (xyzdata == null) {
            iToken = ptX;
            error(ERROR_what, "xyzdata is null.");
          }
          if (xyzdata.length != nX || xyzdata[0].length != nY
              || xyzdata[0][0].length != nZ) {
            iToken = ptX;
            error(ERROR_what, "xyzdata[" + xyzdata.length + "]["
                + xyzdata[0].length + "][" + xyzdata[0][0].length
                + "] is not of size [" + nX + "][" + nY + "][" + nZ + "]");
          }
          v.addElement(xyzdata); // (5) = float[][][] data
        }
        i = iToken;
        propertyName = "functionXYZ";
        propertyValue = v;
        isFxy = surfaceObjectSeen = true;
        break;
      case Token.gridpoints:
        propertyName = "gridPoints";
        break;
      case Token.ignore:
        propertyName = "ignore";
        propertyValue = expression(++i);
        i = iToken;
        break;
      case Token.insideout:
        propertyName = "insideOut";
        break;
      case Token.internal:
      case Token.interior:
        propertyName = "pocket";
        propertyValue = Boolean.FALSE;
        break;
      case Token.lobe:
        // lobe {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "lobe";
        propertyValue = getPoint4f(++i);
        i = iToken;
        break;
      case Token.lonepair:
      case Token.lp:
        // lp {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "lp";
        propertyValue = getPoint4f(++i);
        i = iToken;
        break;
      case Token.map:
        if (haveRadius && !surfaceObjectSeen) {
          surfaceObjectSeen = true;
          addShapeProperty(propertyList, "bsSolvent",
              lookupIdentifierValue("solvent"));
          addShapeProperty(propertyList, "sasurface", new Float(0));
        }
        surfaceObjectSeen = !isCavity;
        propertyName = "map";
        break;
      case Token.maxset:
        propertyName = "maxset";
        propertyValue = new Integer(intParameter(++i));
        break;
      case Token.minset:
        propertyName = "minset";
        propertyValue = new Integer(intParameter(++i));
        break;
      case Token.radical:
        // rad {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "rad";
        propertyValue = getPoint4f(++i);
        i = iToken;
        break;
      case Token.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case Token.molecular:
        surfaceObjectSeen = true;
        propertyName = "molecular";
        propertyValue = new Float(1.4);
        break;
      case Token.object:
      case Token.obj:
        addShapeProperty(propertyList, "fileType", "Obj");
        continue;
      case Token.phase:
        if (surfaceObjectSeen)
          error(ERROR_invalidArgument);
        propertyName = "phase";
        propertyValue = (tokAt(i + 1) == Token.string ? stringParameter(++i)
            : "_orb");
        break;
      case Token.pocket:
        propertyName = "pocket";
        propertyValue = Boolean.TRUE;
        break;
      case Token.pointsperangstrom:
      case Token.resolution:
        propertyName = "resolution";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.reversecolor:
        propertyName = "reverseColor";
        propertyValue = Boolean.TRUE;
        break;
      case Token.sigma:
        ptSigma = i;
        propertyName = "sigma";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.sign:
        signPt = i + 1;
        propertyName = "sign";
        propertyValue = Boolean.TRUE;
        colorRangeStage = 1;
        break;
      case Token.sphere:
        // sphere [radius]
        surfaceObjectSeen = true;
        propertyName = "sphere";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      case Token.variable:
        propertyName = "property";
        data = new float[viewer.getAtomCount()];
        if (!isSyntaxCheck) {
          Parser.parseFloatArray(""
              + getParameter(parameterAsString(++i), false), null, data);
        }
        propertyValue = data;
        break;
      case Token.string:
        String filename = parameterAsString(i);
        boolean firstPass = (!surfaceObjectSeen && !planeSeen);
        if (filename.startsWith("+") && filename.length() > 1) {
          // Uppsala Electron Density Server (default, at least)
          String[] info = viewer.getElectronDensityLoadInfo();
          String server = info[0];
          //String option = (!firstPass || ptWithin > 0 ? null : info[1]);
          String strCutoff = (isSyntaxCheck || !firstPass || ptCutoff > 0 ? null : info[2]);
          String f = filename.substring(1);
          filename = TextFormat.simpleReplace(server, "%LCFILE", f.toLowerCase());
          filename = TextFormat.simpleReplace(filename, "%FILE", f);
          if (strCutoff != null) {
            strCutoff = TextFormat.simpleReplace(strCutoff,"%LCFILE", f.toLowerCase());
            strCutoff = TextFormat.simpleReplace(strCutoff,"%FILE", f);
            float cutoff = ScriptVariable.fValue(ScriptVariable.getVariable(viewer.evaluateExpression(strCutoff)));
            if (cutoff > 0) {
              addShapeProperty(propertyList, "cutoff", new Float(cutoff));
              ptCutoff = i;
            }
          }
          if (ptWithin == 0) {
            getWithinDistanceVector(propertyList, 2.0f, null, viewer.getModelAtomBitSet(modelIndex, true));
          }
          defaultMesh = true;
        }
        if (firstPass
            && viewer.getParameter("_fileType").equals("Pdb") && ptSigma == 0
            && ptCutoff == 0) {
          addShapeProperty(propertyList, "sigma", new Float(1));
        }
        propertyName = (firstPass ? "readFile" : "mapColor");
        /*
         * a file name, optionally followed by an integer file index. OR empty.
         * In that case, if the model auxiliary info has the data stored in it,
         * we use that. There are two possible structures:
         * 
         * jmolSurfaceInfo jmolMappedDataInfo
         * 
         * Both can be present, but if jmolMappedDataInfo is missing, then
         * jmolSurfaceInfo is used by default.
         */
        if (filename.equals("TESTDATA") && Viewer.testData != null) {
          propertyValue = Viewer.testData;
          break;
        }
        if (filename.equals("TESTDATA2") && Viewer.testData2 != null) {
          propertyValue = Viewer.testData2;
          break;
        }
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
          addShapeProperty(propertyList, "fileIndex", new Integer(
              intParameter(++i)));
        if (filename.equals("string")) {
          propertyValue = (isSyntaxCheck ? null : viewer.getFileInfo()[3]);
          addShapeProperty(propertyList, "fileName", "");
        } else if (filename.equalsIgnoreCase("INLINE")) {
          // inline PMESH data
          if (tokAt(i + 1) != Token.string)
            error(ERROR_stringExpected);
          // addShapeProperty(propertyList, "fileType", "Pmesh");
          String sdata = parameterAsString(++i);
          if (isPmesh)
            sdata = TextFormat.replaceAllCharacters(sdata, "{,}|", ' ');
          if (logMessages)
            Logger.debug("pmesh inline data:\n" + sdata);
          propertyValue = (isSyntaxCheck ? null : sdata);
          addShapeProperty(propertyList, "fileName", "");
        } else if (!isSyntaxCheck) {
          String[] fullPathNameOrError;
          String localName = null;
          if (fullCommand.indexOf("# FILE" + nFiles + "=") >= 0) {
            filename = extractCommandOption("# FILE" + nFiles);
            if (tokAt(i + 1) == Token.as)
              i += 2; // skip that
          } else if (tokAt(i + 1) == Token.as) {
            localName = viewer.getFullPath(stringParameter(i = i + 2));
            fullPathNameOrError = viewer.getFullPathNameOrError(localName);
            localName = fullPathNameOrError[0];
            addShapeProperty(propertyList, "localName", localName);
          }
          // just checking here, and getting the full path name
          fullPathNameOrError = viewer.getFullPathNameOrError(filename);
          filename = fullPathNameOrError[0];
          if (fullPathNameOrError[1] != null)
            error(ERROR_fileNotFoundException, filename + ":"
                + fullPathNameOrError[1]);
          Logger.info("reading isosurface data from " + filename);
          addShapeProperty(propertyList, "commandOption", "FILE" + (nFiles++)
              + "=" + Escape.escape(localName == null ? filename : localName));
          addShapeProperty(propertyList, "fileName", filename);
          // null value indicates that we need a reader based on the fileName
          propertyValue = null;
        }
        break;
      case Token.identifier:
        if (str.equalsIgnoreCase("LINK")) { // for state of lcaoCartoon
          propertyName = "link";
          break;
        } else if (str.equalsIgnoreCase("REMAPPABLE")) { // testing only
          propertyName = "remappable";
          break;
        } else {
          propertyName = "thisID";
          propertyValue = str;
        }
        // fall through
      default:
        if (planeSeen && !surfaceObjectSeen) {
          addShapeProperty(propertyList, "nomap", new Float(0));
          surfaceObjectSeen = true;
        }
        if (!setMeshDisplayProperty(iShape, 0, theTok)) {
          if (Token.tokAttr(theTok, Token.identifier) && !idSeen) {
            setShapeId(iShape, i, idSeen);
            i = iToken;
            break;
          }
          error(ERROR_invalidArgument);
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = statementLength - 1;
        break;
      }
      idSeen = (theTok != Token.delete);
      if (propertyName == "property" && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent",
            lookupIdentifierValue("solvent"));
        propertyName = "sasurface";
        propertyValue = new Float(0);
      }
      if (isWild && surfaceObjectSeen)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
    }
    if ((isCavity || haveRadius) && !surfaceObjectSeen) {
      surfaceObjectSeen = true;
      addShapeProperty(propertyList, "bsSolvent",
          lookupIdentifierValue("solvent"));
      addShapeProperty(propertyList, "sasurface", new Float(0));
    }

    if (planeSeen && !surfaceObjectSeen) {
      addShapeProperty(propertyList, "nomap", new Float(0));
      surfaceObjectSeen = true;
    }
    if (thisSetNumber > 0)
      addShapeProperty(propertyList, "getSurfaceSets", new Integer(
          thisSetNumber - 1));
    if (discreteColixes != null)
      addShapeProperty(propertyList, "colorDiscrete", discreteColixes);
    else if (colorScheme != null)
      addShapeProperty(propertyList, "setColorScheme",
          new Object[] { colorScheme,
              isColorSchemeTranslucent ? Boolean.TRUE : Boolean.FALSE });

    // TODO: addShapeProperty(propertyList, "variables", thisCommandVariables);

    // OK, now send them all
    setShapeProperty(iShape, "setProperties", propertyList);

    if (defaultMesh) {
      setShapeProperty(iShape, "token", new Integer(Token.mesh));
      setShapeProperty(iShape, "token", new Integer(Token.nofill));
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(iShape, iptDisplayProperty,
          getToken(iptDisplayProperty).tok))
        error(ERROR_invalidArgument);
    }

    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = getShapeProperty(iShape, "area");
      if (area instanceof Float)
        viewer.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        viewer.setUserVariable("isosurfaceArea", ScriptVariable
            .getVariable(area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        viewer.setFloatProperty("isosurfaceVolume", ((Float) volume)
            .floatValue());
      else
        viewer.setUserVariable("isosurfaceVolume", ScriptVariable
            .getVariable(volume));
    }
    if (surfaceObjectSeen && isIsosurface && !isSyntaxCheck) {
      setShapeProperty(iShape, "finalize", null);
      Integer n = (Integer) getShapeProperty(iShape, "count");
      float[] dataRange = (float[]) getShapeProperty(iShape, "dataRange");
      String s = (String) getShapeProperty(iShape, "ID");
      if (s != null) {
        float cutoff = ((Float) getShapeProperty(iShape, "cutoff"))
            .floatValue();
        if (Float.isNaN(cutoff) && ptSigma > 0) {
          Logger.error("sigma not supported");
        }
        s += " created with cutoff=" + cutoff;
        float[] minMax = (float[]) getShapeProperty(iShape, "minMaxInfo");
        if (minMax[0] != Float.MAX_VALUE)
          s += " min=" + minMax[0] + " max=" + minMax[1];
        s += "; number of isosurfaces = " + n;
        if (dataRange != null && dataRange[0] != Float.MAX_VALUE
            && dataRange[0] != dataRange[1])
          s += "\ncolor range " + dataRange[2] + " " + dataRange[3]
              + "; mapped data range " + dataRange[0] + " to " + dataRange[1];
        if (doCalcArea)
          s += "\nisosurfaceArea = " + Escape.escapeArray(area);
        if (doCalcVolume)
          s += "\nisosurfaceVolume = " + Escape.escapeArray(volume);
        showString(s);
      }
    } else if (doCalcArea || doCalcVolume) {
      if (doCalcArea)
        showString("isosurfaceArea = " + Escape.escapeArray(area));
      if (doCalcVolume)
        showString("isosurfaceVolume = " + Escape.escapeArray(volume));
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
  }

  private void getWithinDistanceVector(Vector propertyList, float distance, Point3f ptc, BitSet bs) {
    Vector v = new Vector();
    Point3f[] pts = new Point3f[2];
    if (bs == null) {
      Point3f pt1 = new Point3f(distance, distance, distance);
      Point3f pt0 = new Point3f(ptc);
      pt0.sub(pt1);
      pt1.add(ptc);
      pts[0] = pt0;
      pts[1] = pt1;
      v.add(ptc);
    } else {
      addShapeProperty(propertyList, "commandOption", "WITHIN=\""
          + Escape.escape(bs) + "\"");
      BoxInfo bbox = viewer.getBoxInfo(bs, -distance);
      pts[0] = bbox.getBboxVertices()[0];
      pts[1] = bbox.getBboxVertices()[7];
      if (bs.cardinality() == 1)
        v.add(viewer.getAtomPoint3f(bs.nextSetBit(0)));
    }
    if (v.size() == 1) {
      addShapeProperty(propertyList, "withinDistance", new Float(distance));
      addShapeProperty(propertyList, "withinPoint", (Point3f) v.get(0));
    }
    addShapeProperty(propertyList, "withinPoints",
        new Object[] { new Float(distance), pts, bs, v });
  }

  /**
   * @param shape
   * @param i
   * @param tok
   * @return true if successful
   * @throws ScriptException
   */
  private boolean setMeshDisplayProperty(int shape, int i, int tok)
      throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    boolean checkOnly = (i == 0);
    // these properties are all processed in MeshCollection.java
    if (!checkOnly)
      iToken = i;
    switch (tok) {
    case Token.opaque:
    case Token.translucent:
      if (!checkOnly)
        colorShape(shape, iToken, false);
      return true;
    case Token.nada:
    case Token.delete:
    case Token.on:
    case Token.off:
    case Token.hide:
    case Token.hidden:
    case Token.display:
    case Token.displayed:
      if (iToken == 1)
        setShapeProperty(shape, "thisID", null);
      if (tok == Token.nada)
        return (iToken == 1);
      if (checkOnly)
        return true;
      if (tok == Token.delete) {
        setShapeProperty(shape, "delete", null);
        return true;
      }
      if (tok == Token.hidden || tok == Token.hide)
        tok = Token.off;
      else if (tok == Token.displayed || tok == Token.display)
        tok = Token.on;
      // fall through for on/off
    case Token.frontlit:
    case Token.backlit:
    case Token.fullylit:
    case Token.contourlines:
    case Token.nocontourlines:
    case Token.dots:
    case Token.nodots:
    case Token.mesh:
    case Token.nomesh:
    case Token.fill:
    case Token.nofill:
    case Token.triangles:
    case Token.notriangles:
    case Token.frontonly:
    case Token.notfrontonly:
      propertyName = "token";
      propertyValue = new Integer(tok);
      break;
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

  private void bind() throws ScriptException {
    /*
     * bind "MOUSE-ACTION" actionName bind "MOUSE-ACTION" "script" range
     * [xyrange] [xyrange]
     */
    String mouseAction = stringParameter(1);
    String name = parameterAsString(2);
    Point3f range1 = null;
    Point3f range2 = null;
    if (tokAt(3) == Token.range) {
      range1 = xypParameter(4);
      range2 = xypParameter(++iToken);
      checkLast(iToken);
    } else {
      checkLength(3);
    }
    if (!isSyntaxCheck)
      viewer.bindAction(mouseAction, name, range1, range2);
  }

  private void unbind() throws ScriptException {
    /*
     * unbind "MOUSE-ACTION"|all ["...script..."|actionName|all]
     */
    if (statementLength != 1)
      checkLength23();
    String mouseAction = optParameterAsString(1);
    String name = optParameterAsString(2);
    if (mouseAction.length() == 0 || tokAt(1) == Token.all)
      mouseAction = null;
    if (name.length() == 0 || tokAt(2) == Token.all)
      name = null;
    if (name == null && mouseAction != null
        && ActionManager.getActionFromName(mouseAction) >= 0) {
      name = mouseAction;
      mouseAction = null;
    }
    if (!isSyntaxCheck)
      viewer.unBindAction(mouseAction, name);
  }

}
