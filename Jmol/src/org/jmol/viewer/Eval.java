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

import org.jmol.g3d.Font3D;
import org.jmol.smiles.InvalidSmilesException;
import org.openscience.jmol.app.SelectWithinDialog;

import java.io.*;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;

class Context {
  String filename;
  String script;
  short[] linenumbers;
  short[] lineIndices;
  Token[][] aatoken;
  int pc;
}

class Eval implements Runnable {

  Compiler compiler;

  final static int scriptLevelMax = 10;
  int scriptLevel;

  Context[] stack = new Context[scriptLevelMax];

  String filename;
  String script;
  short[] linenumbers;
  short[] lineIndices;
  Token[][] aatoken;
  int pc; // program counter

  long timeBeginExecution;
  long timeEndExecution;
  boolean error;
  String errorMessage;

  Token[] statement;
  int statementLength;
  Viewer viewer;
  Thread myThread;
  boolean terminationNotification;
  boolean interruptExecution;
  boolean tQuiet;

  final static boolean logMessages = false;

  Eval(Viewer viewer) {
    compiler = new Compiler();
    this.viewer = viewer;
    clearDefinitionsAndLoadPredefined();
  }

  synchronized void start() {
    if (myThread == null) {
      myThread = new Thread(this);
      interruptExecution = false;
      myThread.start();
    }
  }

  synchronized void haltExecution() {
    if (myThread != null) {
      interruptExecution = true;
      myThread.interrupt();
    }
  }

  boolean isScriptExecuting() {
    return myThread != null;
  }

  synchronized void clearMyThread() {
    myThread = null;
  }

  boolean isActive() {
    return myThread != null;
  }

  boolean hasTerminationNotification() {
    return terminationNotification;
  }

  void resetTerminationNotification() {
    terminationNotification = false;
  }

  boolean hadRuntimeError() {
    return error;
  }

  String getErrorMessage() {
    return errorMessage;
  }

  int getExecutionWalltime() {
    return (int)(timeEndExecution - timeBeginExecution);
  }

  boolean loadScript(String filename, String script) {
    this.filename = filename;
    this.script = script;
    if (! compiler.compile(filename, script)) {
      error = true;
      errorMessage = compiler.getErrorMessage();
      viewer.scriptStatus(errorMessage);
      return false;
    }
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    linenumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    return true;
  }

  void clearState(boolean tQuiet) {
    for (int i = scriptLevelMax; --i >= 0; )
      stack[i] = null;
    scriptLevel = 0;
    error = false;
    errorMessage = null;
    terminationNotification = false;
    interruptExecution = false;
    this.tQuiet = tQuiet;
  }

  boolean loadScriptString(String script, boolean tQuiet) {
    clearState(tQuiet);
    return loadScript(null, script);
  }

  boolean loadScriptFile(String filename, boolean tQuiet) {
    clearState(tQuiet);
    return loadScriptFileInternal(filename);
  }

  boolean loadScriptFileInternal(String filename) {
    Object t = viewer.getInputStreamOrErrorMessageFromName(filename);
    if (! (t instanceof InputStream))
      return loadError((String) t);
    BufferedReader reader =
      new BufferedReader(new InputStreamReader((InputStream) t));
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
	try{reader.close();}catch(IOException ioe){}
      return ioError(filename);
    }
    try{reader.close();}catch(IOException ioe){}
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
    StringBuffer str=new StringBuffer();
    str.append("Eval\n pc:");
    str.append(pc); str.append("\n");
    str.append(aatoken.length); str.append(" statements\n");
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
    variables.clear();

    int cPredef = JmolConstants.predefinedSets.length;
    for (int iPredef = 0; iPredef < cPredef; iPredef++)
      predefine(JmolConstants.predefinedSets[iPredef]);
    // Now, define all the elements as predefined sets
    // hydrogen is handled specially, so don't define it
    for (int i = JmolConstants.elementNames.length; --i > 1; ) {
      String definition = "@" + JmolConstants.elementNames[i] + " _e=" + i;
      predefine(definition);
    }
    for (int j = JmolConstants.alternateElementNumbers.length; --j >= 0; ) {
      String definition =
        "@" + JmolConstants.alternateElementNames[j] +
        " _e=" + JmolConstants.alternateElementNumbers[j];
      predefine(definition);
    }
  }

  void predefineElements() {
    // the name 'hydrogen' handled specially
    for (int i = JmolConstants.elementNames.length; --i > 1; ) {
      String definition = "@" + JmolConstants.elementNames[i] + " _e=" + i;
      if (! compiler.compile("#element", definition)) {
        System.out.println("element definition error:" + definition);
        continue;
      }
      Token [][] aatoken = compiler.getAatokenCompiled();
      Token[] statement = aatoken[0];
      //int tok = statement[1].tok;
      String variable = (String)statement[1].value;
      variables.put(variable, statement);
    }
  }

  void predefine(String script) {
    if (compiler.compile("#predefine", script)) {
      Token [][] aatoken = compiler.getAatokenCompiled();
      if (aatoken.length != 1) {
        viewer.scriptStatus("predefinition does not have exactly 1 command:"
                            + script);
        return;
      }
      Token[] statement = aatoken[0];
      if (statement.length > 2) {
        int tok = statement[1].tok;
        if (tok == Token.identifier ||
            (tok & Token.predefinedset) == Token.predefinedset) {
          String variable = (String)statement[1].value;
          variables.put(variable, statement);
        } else {
          viewer.scriptStatus("invalid variable name:" + script);
        }
      } else {
        viewer.scriptStatus("bad predefinition length:" + script);
      }
    } else {
      viewer.scriptStatus("predefined set compile error:" + script +
                          "\ncompile error:" + compiler.getErrorMessage());
    }
  }

  public void run() {
    // this refresh is here to ensure that the screen has been painted ...
    // since it could be a problem when an applet is loaded with a script
    // ready to run. 
    refresh();
    timeBeginExecution = System.currentTimeMillis();
    viewer.pushHoldRepaint();
    try {
      instructionDispatchLoop();
    } catch (ScriptException e) {
      error = true;
      errorMessage = "" + e;
      viewer.scriptStatus(errorMessage);
    }
    timeEndExecution = System.currentTimeMillis();
    if (errorMessage == null && interruptExecution)
      errorMessage = "execution interrupted";
    if (errorMessage != null)
      viewer.scriptStatus(errorMessage);
    else if (! tQuiet)
      viewer.scriptStatus("Script completed");
    clearMyThread();
    terminationNotification = true;
    viewer.popHoldRepaint();
  }

  void instructionDispatchLoop() throws ScriptException {
    long timeBegin = 0;
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      viewer.scriptStatus(toString());
    }
    while (!interruptExecution && pc < aatoken.length) {
      statement = aatoken[pc++];
      statementLength = statement.length;
      if (viewer.getDebugScript())
        logDebugScript();
      Token token = statement[0];
      switch (token.tok) {
      case Token.backbone:
        proteinShape(JmolConstants.SHAPE_BACKBONE);
        break;
      case Token.background:
        background();
        break;
      case Token.center:
        center();
        break;
      case Token.color:
        color();
        break;
      case Token.define:
        define();
        break;
      case Token.echo:
        echo();
        break;
      case Token.exit:
      case Token.quit: // in rasmol quit actually exits the program
        return;
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
      case Token.reset:
        reset();
        break;
      case Token.rotate:
        rotate();
        break;
      case Token.script:
        script();
        break;
      case Token.select:
        select();
        break;
      case Token.translate:
        translate();
        break;
      case Token.zap:
        zap();
        break;
      case Token.zoom:
        zoom();
        break;
      case Token.delay:
        delay();
        break;
      case Token.loop:
        delay(); // a loop is just a delay followed by ...
        pc = 0;  // ... resetting the program counter
        break;
      case Token.move:
        move();
        break;
      case Token.restrict:
        restrict();
        break;
      case Token.set:
        set();
        break;
      case Token.slab:
        slab();
        break;
      case Token.depth:
        depth();
        break;
      case Token.star:
        star();
        break;
      case Token.cpk:
        cpk();
        break;
      case Token.wireframe:
        wireframe();
        break;
      case Token.vector:
        vector();
        break;
      case Token.animation:
        animation();
        break;
      case Token.vibration:
        vibration();
        break;
      case Token.dots:
        dots();
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
      case Token.prueba:
        proteinShape(JmolConstants.SHAPE_PRUEBA);
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
        spin();
        break;
      case Token.ssbond:
        ssbond();
        break;
      case Token.hbond:
        hbond();
        break;
      case Token.show:
        show();
        break;
      case Token.frame:
      case Token.model:
        frame();
        break;
      case Token.font:
        font();
        break;
      case Token.moveto:
        moveto();
        break;
      case Token.console:
        console();
        break;
      case Token.pmesh:
        pmesh();
        break;
      case Token.polyhedra:
        polyhedra();
        break;
      case Token.sasurface:
        sasurface();
        break;
      case Token.centerAt:
        centerAt();
        break;
      case Token.isosurface:
        isosurface();
        break;
      case Token.stereo:
        stereo();
        break;
      case Token.connect:
        connect();
        break;

        // phdana pfaat ->
      case Token.selectionListeners:
          selectionListeners();
          break;

      case Token.selectWithinDialog:
          selectWithinDialog();
          break;
      // phdana pfaat <-

      // not implemented
      case Token.bond:
      case Token.clipboard:
      case Token.help:
      case Token.molecule:
      case Token.pause:
      case Token.print:
      case Token.renumber:
      case Token.save:
      case Token.structure:
      case Token.unbond:
      case Token.write:
        // chime extended commands
      case Token.view:
      case Token.list:
      case Token.display3d:
        viewer.scriptStatus("Script command not implemented:" + token.value);
        break;
      default:
        unrecognizedCommand(token);
        return;
      }
    }
  }

  int getLinenumber() {
    return linenumbers[pc];
  }

  String getLine() {
    int ichBegin = lineIndices[pc];
    int ichEnd;
    if ((ichEnd = script.indexOf('\r', ichBegin)) == -1 &&
        (ichEnd = script.indexOf('\n', ichBegin)) == -1)
      ichEnd = script.length();
    return script.substring(ichBegin, ichEnd);
  }
  
  final StringBuffer strbufLog = new StringBuffer(80);
  void logDebugScript() {
    strbufLog.setLength(0);
    strbufLog.append(statement[0].value.toString());
    for (int i = 1; i < statementLength; ++i) {
      strbufLog.append(' ');
      Token token = statement[i];
      switch (token.tok) {
      case Token.integer:
        strbufLog.append(token.intValue);
        continue;
      case Token.spec_seqcode:
        strbufLog.append(Group.getSeqcodeString(token.intValue));
        continue;
      case Token.spec_chain:
        strbufLog.append(':');
        strbufLog.append((char)token.intValue);
        continue;
      case Token.spec_alternate:
        strbufLog.append("%");
        strbufLog.append("" + token.value);
        break;
      case Token.spec_model:
        strbufLog.append("/");
        strbufLog.append("" + token.value);
        break;
      case Token.spec_resid:
        strbufLog.append('[');
        strbufLog.append(Group.getGroup3((short)token.intValue));
        strbufLog.append(']');
        continue;
      case Token.spec_name_pattern:
        strbufLog.append('[');
        strbufLog.append(token.value);
        strbufLog.append(']');
        continue;
      case Token.spec_atom:
        strbufLog.append('.');
        break;
      case Token.spec_seqcode_range:
        strbufLog.append(Group.getSeqcodeString(token.intValue));
        strbufLog.append('-');
        strbufLog.append(Group.getSeqcodeString(((Integer)token.value).intValue()));
        break;
      case Token.within:
        strbufLog.append("within ");
        break;
      case Token.connected:
        strbufLog.append("connected ");
        break;
      case Token.substructure:
        strbufLog.append("substructure ");
        break;
      case Token.opEQ:
      case Token.opNE:
      case Token.opGT:
      case Token.opGE:
      case Token.opLT:
      case Token.opLE:
        strbufLog.append(Token.atomPropertyNames[token.intValue & 0x0F]);
        strbufLog.append(Token.comparatorNames[token.tok & 0x0F]);
        break;
      }
      strbufLog.append("" + token.value);
    }
    viewer.scriptStatus(strbufLog.toString());
  }

  void evalError(String message) throws ScriptException {
    throw new ScriptException(message, getLine(), filename, getLinenumber());
  }

  void unrecognizedCommand(Token token) throws ScriptException {
    evalError("unrecognized command:" + token.value);
  }

  void unrecognizedAtomProperty(int propnum) throws ScriptException {
    evalError("unrecognized atom property:" + propnum);
  }

  void filenameExpected() throws ScriptException {
    evalError("filename expected");
  }

  void booleanExpected() throws ScriptException {
    evalError("boolean expected");
  }

  void booleanOrPercentExpected() throws ScriptException {
    evalError("boolean or percent expected");
  }

  void booleanOrNumberExpected() throws ScriptException {
    evalError("boolean or number expected");
  }

  void integerExpected() throws ScriptException {
    evalError("integer expected");
  }

  void numberExpected() throws ScriptException {
    evalError("number expected");
  }

  void propertyNameExpected() throws ScriptException {
    evalError("property name expected");
  }

  void axisExpected() throws ScriptException {
    evalError("x y z axis expected");
  }

  void colorExpected() throws ScriptException {
    evalError("color expected");
  }

  void keywordExpected() throws ScriptException {
    evalError("keyword expected");
  }

  void unrecognizedColorObject() throws ScriptException {
    evalError("unrecognized color object");
  }

  void unrecognizedExpression() throws ScriptException {
    evalError("runtime unrecognized expression");
  }

  void undefinedVariable() throws ScriptException {
    evalError("variable undefined");
  }

  void badArgumentCount() throws ScriptException {
    evalError("bad argument count");
  }

  void invalidArgument() throws ScriptException {
    evalError("invalid argument");
  }

  void incompatibleArguments() throws ScriptException {
    evalError("incompatible arguments");
  }

  void insufficientArguments() throws ScriptException {
    evalError("insufficient arguments");
  }

  void unrecognizedSetParameter() throws ScriptException {
    evalError("unrecognized SET parameter");
  }

  void unrecognizedSubcommand() throws ScriptException {
    evalError("unrecognized subcommand");
  }

  void invalidParameterOrder() throws ScriptException {
    evalError("invalid parameter order");
  }

  void subcommandExpected() throws ScriptException {
    evalError("subcommand expected");
  }

  void setspecialShouldNotBeHere() throws ScriptException {
    evalError("interpreter error - setspecial should not be here");
  }

  void numberOutOfRange() throws ScriptException {
    evalError("number out of range");
  }

  void numberOutOfRange(int min, int max) throws ScriptException {
    evalError("integer out of range (" + min + " - " + max + ")");
  }

  void numberOutOfRange(float min, float max) throws ScriptException {
    evalError("decimal number out of range (" + min + " - " + max + ")");
  }

  void numberMustBe(int a, int b) throws ScriptException {
    evalError("number must be (" + a + " or " + b + ")");
  }

  void badAtomNumber() throws ScriptException {
    evalError("bad atom number");
  }

  void errorLoadingScript(String msg) throws ScriptException {
    evalError("error loading script -> " + msg);
  }

  void fileNotFoundException(String filename) throws ScriptException {
    evalError("file not found : " + filename);
  }

  void notImplemented(int itoken) {
    notImplemented(statement[itoken]);
  }

  void notImplemented(Token token) {
    viewer.scriptStatus("" + token.value +
                       " not implemented in command:" + statement[0].value);
  }

  // gets a boolean value from the 2nd parameter to the command
  // as in set foo <boolean>

  void checkStatementLength(int length) throws ScriptException {
    if (statementLength != length)
      badArgumentCount();
  }

  void checkLength34() throws ScriptException {
    if (statementLength < 3 || statementLength > 4)
      badArgumentCount();
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
  
  int intParameter(int index) throws ScriptException {
    if (statement[index].tok != Token.integer)
      integerExpected();
    return statement[index].intValue;
  }

  float floatParameter(int index) throws ScriptException {
    if (index >= statementLength)
      badArgumentCount();
    float floatValue = 0;
    switch (statement[index].tok) {
    case Token.integer:
      floatValue = statement[index].intValue;
      break;
    case Token.decimal:
      floatValue = ((Float)statement[index].value).floatValue();
      break;
    default:
      numberExpected();
    }
    return floatValue;
  }

  float getSetAngstroms() throws ScriptException {
    checkLength3();
    Token token = statement[2];
    switch (token.tok) {
    case Token.integer:
      return token.intValue / 250f;
    case Token.decimal:
      return ((Float)token.value).floatValue();
    default:
      numberExpected();
    }
    return -1;
  }

  boolean getSetBoolean() throws ScriptException {
    checkLength3();
    switch (statement[2].tok) {
    case Token.on:
      return true;
    case Token.off:
      return false;
    default:
      booleanExpected();
    }
    return false;
  }

  short getSetAxesTypeMad() throws ScriptException {
    checkLength3();
    int tok = statement[2].tok;
    short mad = 0;
    switch (tok) {
    case Token.on:
      mad = 1;
    case Token.off:
      break;
    case Token.integer:
      int diameterPixels = statement[2].intValue;
      if (diameterPixels < 0 || diameterPixels >= 20)
        numberOutOfRange(0, 19);
      mad = (short)diameterPixels;
      break;
    case Token.decimal:
      float angstroms = floatParameter(2);
      if (angstroms < 0 || angstroms >= 2)
        numberOutOfRange(0f, 1.99999f);
      mad = (short)(angstroms * 1000 * 2);
      break;
    case Token.dotted:
      mad = -1;
      break;
    default:
      booleanOrNumberExpected();
    }
    return mad;
  }

  int getSetInteger() throws ScriptException {
    checkLength3();
    return intParameter(2);
  }

  BitSet copyBitSet(BitSet bitSet) {
    BitSet copy = new BitSet();
    copy.or(bitSet);
    return copy;
  }

  int pcLastExpressionInstruction;
  BitSet expression(Token[] code, int pcStart) throws ScriptException {
    int numberOfAtoms = viewer.getAtomCount();
    BitSet bs;
    BitSet[] stack = new BitSet[10];
    int sp = 0;
    if (logMessages)
      viewer.scriptStatus("start to evaluate expression");
    expression_loop:
    for (int pc = pcStart; ; ++pc) {
      Token instruction = code[pc];
      if (logMessages)
        viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.expressionBegin:
        break; 
      case Token.expressionEnd:
        pcLastExpressionInstruction = pc;
          break expression_loop;
      case Token.all:
        bs = stack[sp++] = new BitSet(numberOfAtoms);
        for (int i = numberOfAtoms; --i >= 0; )
          bs.set(i);
        break;
      case Token.none:
        stack[sp++] = new BitSet();
        break;
      case Token.opOr:
        bs = stack[--sp];
        stack[sp-1].or(bs);
        break;
      case Token.opAnd:
        bs = stack[--sp];
        stack[sp-1].and(bs);
        break;
      case Token.opNot:
        bs = stack[sp - 1];
        notSet(bs);
        break;
      case Token.within:
        bs = stack[sp - 1];
        stack[sp - 1] = new BitSet();
        withinInstruction(instruction, bs, stack[sp - 1]);
        break;
      case Token.connected:
        bs = stack[sp - 1];
        stack[sp - 1] = connected(instruction, bs);
        break;
      case Token.substructure:
        stack[sp++] = getSubstructureSet((String)instruction.value);
        break;
      case Token.selected:
        stack[sp++] = copyBitSet(viewer.getSelectionSet());
        break;
      case Token.hetero:
        stack[sp++] = getHeteroSet();
        break;
      case Token.hydrogen:
        stack[sp++] = getHydrogenSet();
        break;
      case Token.spec_name_pattern:
        stack[sp++] = getSpecName((String)instruction.value);
        break;
      case Token.spec_resid:
        stack[sp++] = getSpecResid(instruction.intValue);
        break;
      case Token.spec_seqcode:
        stack[sp++] = getSpecSeqcode(instruction.intValue);
        break;
      case Token.spec_seqcode_range:
        int seqcodeA = instruction.intValue;
        int seqcodeB = ((Integer)instruction.value).intValue();
        stack[sp++] = getSpecSeqcodeRange(seqcodeA, seqcodeB);
        break;
      case Token.spec_chain:
        stack[sp++] = getSpecChain((char)instruction.intValue);
        break;
      case Token.spec_atom:
        stack[sp++] = getSpecAtom((String)instruction.value);
        break;
      case Token.spec_alternate:
        stack[sp++] = getSpecAlternate((String)instruction.value);
        break;
      case Token.spec_model:
        stack[sp++] = getSpecModel((String)instruction.value);
        break;
      case Token.protein:
        stack[sp++] = getProteinSet();
        break;
      case Token.nucleic:
        stack[sp++] = getNucleicSet();
        break;
      case Token.dna:
        stack[sp++] = getDnaSet();
        break;
      case Token.rna:
        stack[sp++] = getRnaSet();
        break;
      case Token.purine:
        stack[sp++] = getPurineSet();
        break;
      case Token.pyrimidine:
        stack[sp++] = getPyrimidineSet();
        break;
      case Token.y:
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.identifier:
      case Token.sidechain:
      case Token.surface:
        stack[sp++] = lookupIdentifierValue((String)instruction.value);
        break;
      case Token.opLT:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opEQ:
      case Token.opNE:
        bs = stack[sp++] = new BitSet();
        comparatorInstruction(instruction, bs);
        break;
      default:
        unrecognizedExpression();
      }
    }
    if (sp != 1)
      evalError("atom expression compiler error - stack over/underflow");
    return stack[0];
  }

  BitSet lookupIdentifierValue(String identifier) throws ScriptException {
    // identifiers must be handled as a hack
    // the expression 'select c1a' might be [c]1:a or might be a 'define c1a ...'
    BitSet bsDefinedSet = lookupValue(identifier, false);
    if (bsDefinedSet != null)
      return copyBitSet(bsDefinedSet); // identifier had been previously defined
    //    System.out.println("undefined & trying specname with:" + identifier);
    // determine number of leading alpha characters
    int alphaLen = 0;
    int len = identifier.length();
    while (alphaLen < len && Compiler.isAlphabetic(identifier.charAt(alphaLen)))
      ++alphaLen;
    if (alphaLen > 3)
      undefinedVariable();
    String potentialGroupName = identifier.substring(0, alphaLen);
    //    System.out.println("potentialGroupName=" + potentialGroupName);
    //          undefinedVariable();
    BitSet bsName = lookupPotentialGroupName(potentialGroupName);
    if (bsName == null)
      undefinedVariable();
    if (alphaLen == len)
      return bsName;
    //
    // look for a sequence code
    // for now, only support a sequence number
    //
    int seqcodeEnd = alphaLen;
    while (seqcodeEnd < len && Compiler.isDigit(identifier.charAt(seqcodeEnd)))
      ++seqcodeEnd;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(alphaLen, seqcodeEnd));
    } catch (NumberFormatException nfe) {
      evalError("identifier parser error #373");
    }
    char insertionCode = ' ';
    if (seqcodeEnd < len && identifier.charAt(seqcodeEnd) == '^') {
      ++seqcodeEnd;
      if (seqcodeEnd == len)
        evalError("invalid insertion code");
      insertionCode = identifier.charAt(seqcodeEnd++);
    }
    //    System.out.println("sequence number=" + seqNumber +
    //                       " insertionCode=" + insertionCode);
    int seqcode = Group.getSeqcode(seqNumber, insertionCode);
    //    System.out.println("seqcode=" + seqcode);
    BitSet bsSequence = getSpecSeqcode(seqcode);
    BitSet bsNameSequence = bsName;
    bsNameSequence.and(bsSequence);
    if (seqcodeEnd == len)
      return bsNameSequence;
    //
    // look for a chain spec ... also alpha & part of an identifier ... :-(
    //
    char chainID = identifier.charAt(seqcodeEnd);
    if (++seqcodeEnd != len)
      undefinedVariable();
    //    System.out.println("chainID=" + chainID);
    BitSet bsChain = getSpecChain(chainID);
    BitSet bsNameSequenceChain = bsNameSequence;
    bsNameSequenceChain.and(bsChain);
    return bsNameSequenceChain;
  }

  BitSet lookupPotentialGroupName(String potentialGroupName) {
    BitSet bsResult = null;
    //    System.out.println("lookupPotentialGroupName:" + potentialGroupName);
    Frame frame = viewer.getFrame();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.isGroup3(potentialGroupName)) {
        if (bsResult == null)
          bsResult = new BitSet(i + 1);
        bsResult.set(i);
      }
    }
    return bsResult;
  }

  void notSet(BitSet bs) {
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      if (bs.get(i))
        bs.clear(i);
      else
        bs.set(i);
    }
  }

  BitSet getHeteroSet() {
    Frame frame = viewer.getFrame();
    BitSet bsHetero = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isHetero())
        bsHetero.set(i);
    return bsHetero;
  }

  BitSet getHydrogenSet() {
    if (logMessages)
      viewer.scriptStatus("getHydrogenSet()");
    Frame frame = viewer.getFrame();
    BitSet bsHydrogen = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.getElementNumber() == 1)
        bsHydrogen.set(i);
    }
    return bsHydrogen;
  }

  BitSet getProteinSet() {
    Frame frame = viewer.getFrame();
    BitSet bsProtein = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isProtein())
        bsProtein.set(i);
    return bsProtein;
  }

  BitSet getNucleicSet() {
    Frame frame = viewer.getFrame();
    BitSet bsNucleic = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isNucleic())
        bsNucleic.set(i);
    return bsNucleic;
  }

  BitSet getDnaSet() {
    Frame frame = viewer.getFrame();
    BitSet bsDna = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isDna())
        bsDna.set(i);
    return bsDna;
  }

  BitSet getRnaSet() {
    Frame frame = viewer.getFrame();
    BitSet bsRna = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isRna())
        bsRna.set(i);
    return bsRna;
  }

  BitSet getPurineSet() {
    Frame frame = viewer.getFrame();
    BitSet bsPurine = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isPurine())
        bsPurine.set(i);
    return bsPurine;
  }

  BitSet getPyrimidineSet() {
    Frame frame = viewer.getFrame();
    BitSet bsPyrimidine = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).isPyrimidine())
        bsPyrimidine.set(i);
    return bsPyrimidine;
  }

  /*
  BitSet getResidueSet(String strResidue) {
    Frame frame = viewer.getFrame();
    BitSet bsResidue = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      PdbAtom pdbatom = frame.getAtomAt(i).getPdbAtom();
      if (pdbatom != null && pdbatom.isResidue(strResidue))
        bsResidue.set(i);
    }
    return bsResidue;
  }
  */

  BitSet getSpecName(String resNameSpec) {
    BitSet bsRes = new BitSet();
    //    System.out.println("getSpecName:" + resNameSpec);
    Frame frame = viewer.getFrame();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.isGroup3Match(resNameSpec))
        bsRes.set(i);
    }
    return bsRes;
  }

  BitSet getSpecResid(int resid) {
    BitSet bsRes = new BitSet();
    Frame frame = viewer.getFrame();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.getGroupID() == resid)
        bsRes.set(i);
    }
    return bsRes;
  }

  BitSet getSpecSeqcode(int seqcode) {
    Frame frame = viewer.getFrame();
    BitSet bsResno = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (seqcode == atom.getSeqcode())
        bsResno.set(i);
    }
    return bsResno;
  }

  BitSet getSpecSeqcodeRange(int seqcodeA, int seqcodeB) {
    Frame frame = viewer.getFrame();
    BitSet bsResidue = new BitSet();
    frame.selectSeqcodeRange(seqcodeA, seqcodeB, bsResidue);
    return bsResidue;
  }

  BitSet getSpecChain(char chain) {
    boolean caseSensitive = viewer.getChainCaseSensitive();
    if (! caseSensitive)
      chain = Character.toUpperCase(chain);
    Frame frame = viewer.getFrame();
    BitSet bsChain = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      char ch = frame.getAtomAt(i).getChainID();
      if (! caseSensitive)
        ch = Character.toUpperCase(ch);
      if (chain == ch)
        bsChain.set(i);
    }
    return bsChain;
  }

  BitSet getSpecAtom(String atomSpec) {
    Frame frame = viewer.getFrame();
    BitSet bsAtom = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.isAtomNameMatch(atomSpec))
        bsAtom.set(i);
    }
    return bsAtom;
  }

  BitSet getResidueWildcard(String strWildcard) {
    Frame frame = viewer.getFrame();
    BitSet bsResidue = new BitSet();
    //    System.out.println("getResidueWildcard:" + strWildcard);
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.isGroup3Match(strWildcard))
        bsResidue.set(i);
    }
    return bsResidue;
  }

  BitSet lookupValue(String variable, boolean plurals) throws ScriptException {
    if (logMessages)
      viewer.scriptStatus("lookupValue(" + variable + ")");
    Object value = variables.get(variable);
    if (value != null) {
      if (value instanceof Token[]) {
        value = expression((Token[])value, 2);
        variables.put(variable, value);
      }
      return (BitSet)value;
    }
    if (plurals)
      return null;
    int len = variable.length();
    if (len < 5) // iron is the shortest
      return null;
    if (variable.charAt(len - 1) != 's')
      return null;
    if (variable.endsWith("ies"))
      variable = variable.substring(0, len-3) + 'y';
    else
      variable = variable.substring(0, len-1);
    return lookupValue(variable, true);
  }

  void selectModelIndexAtoms(int modelIndex, BitSet bsResult) {
    Frame frame = viewer.getFrame();
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (frame.getAtomAt(i).getModelIndex() == modelIndex)
        bsResult.set(i);
  }

  BitSet getSpecAlternate(String alternateSpec) {
    System.out.println("getSpecAlternate(" + alternateSpec + ")");
    Frame frame = viewer.getFrame();
    BitSet bs = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      Atom atom = frame.getAtomAt(i);
      if (atom.isAlternateLocationMatch(alternateSpec))
        bs.set(i);
    }
    return bs;
  }

  BitSet getSpecModel(String modelTag) {
    int modelNumber = -1;
    try {
      modelNumber = Integer.parseInt(modelTag);
    } catch (NumberFormatException nfe) {
    }
    
    BitSet bsModel = new BitSet(viewer.getAtomCount());
    selectModelIndexAtoms(viewer.getModelNumberIndex(modelNumber), bsModel);
    return bsModel;
  }

  void comparatorInstruction(Token instruction, BitSet bs)
    throws ScriptException {
    int comparator = instruction.tok;
    int property = instruction.intValue;
    float propertyValue = 0; // just for temperature
    int comparisonValue = ((Integer)instruction.value).intValue();
    int numberOfAtoms = viewer.getAtomCount();
    Frame frame = viewer.getFrame();
    for (int i = 0; i < numberOfAtoms; ++i) {
      Atom atom = frame.getAtomAt(i);
      switch (property) {
      case Token.atomno:
        propertyValue = atom.getAtomNumber();
        break;
      case Token.elemno:
        propertyValue = atom.getElementNumber();
        break;
      case Token.temperature:
        propertyValue = atom.getBfactor100();
        if (propertyValue < 0)
          continue;
        propertyValue /= 100;
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
        if (propertyValue < 0)
          continue;
        break;
      case Token._structure:
        propertyValue = getProteinStructureType(atom);
        if (propertyValue == -1)
          continue;
        break;
      case Token.radius:
        propertyValue = atom.getRasMolRadius();
        break;
      case Token.bondcount:
        propertyValue = atom.getCovalentBondCount();
        break;
      case Token.hbondcount:
        propertyValue = atom.getHbondCount();
        break;
      case Token.model:
        propertyValue = atom.getModelTagNumber();
        break;
      default:
        unrecognizedAtomProperty(property);
      }
      boolean match = false;
      switch (comparator) {
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


  void withinInstruction(Token instruction, BitSet bs, BitSet bsResult)
    throws ScriptException {
    Object withinSpec = instruction.value;
    if (withinSpec instanceof Float) {
      withinDistance(((Float)withinSpec).floatValue(), bs, bsResult);
      return;
    }
    if (withinSpec instanceof String) {
      String withinStr = (String)withinSpec;
      if (withinStr.equals("group")) {
        withinGroup(bs, bsResult);
        return;
      }
      if (withinStr.equals("chain")) {
        withinChain(bs, bsResult);
        return;
      }
      if (withinStr.equals("model")) {
        withinModel(bs, bsResult);
        return;
      }
    }
    evalError("Unrecognized within parameter:" + withinSpec);
  }

  void withinDistance(float distance, BitSet bs, BitSet bsResult) {
    Frame frame = viewer.getFrame();
    for (int i = frame.getAtomCount(); --i >= 0; ) {
      if (bs.get(i)) {
        Atom atom = frame.getAtomAt(i);
        AtomIterator iterWithin =
          frame.getWithinAnyModelIterator(atom, distance);
        while (iterWithin.hasNext())
          bsResult.set(iterWithin.next().getAtomIndex());
      }
    }
  }
  
  void withinGroup(BitSet bs, BitSet bsResult) {
    //    System.out.println("withinGroup");
    Frame frame = viewer.getFrame();
    Group groupLast = null;
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      if (! bs.get(i))
        continue;
      Atom atom = frame.getAtomAt(i);
      Group group = atom.getGroup();
      if (group != groupLast) {
        group.selectAtoms(bsResult);
        groupLast = group;
      }
    }
  }

  void withinChain(BitSet bs, BitSet bsResult) {
    Frame frame = viewer.getFrame();
    Chain chainLast = null;
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      if (! bs.get(i))
        continue;
      Atom atom = frame.getAtomAt(i);
      Chain chain = atom.getChain();
      if (chain != chainLast) {
        chain.selectAtoms(bsResult);
        chainLast = chain;
      }
    }
  }

  void withinModel(BitSet bs, BitSet bsResult) {
    Frame frame = viewer.getFrame();
    int modelIndexLast = -1;
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      if (bs.get(i)) {
        int modelIndex = frame.getAtomAt(i).getModelIndex();
        if (modelIndex != modelIndexLast) {
          selectModelIndexAtoms(modelIndex, bsResult);
          modelIndexLast = modelIndex;
        }
      }
    }
  }

  BitSet connected(Token instruction, BitSet bs) {
    int min = instruction.intValue;
    int max = ((Integer) instruction.value).intValue();
    System.out.println("connected(" + min + "," + max + "," + bs + ")");
    Frame frame = viewer.getFrame();
    BitSet bsResult = new BitSet();
    for (int i = viewer.getAtomCount(); --i >= 0; ) {
      int connectedCount = frame.getAtomAt(i).getConnectedCount(bs);
      if (connectedCount >= min && connectedCount <= max)
        bsResult.set(i);
    }
    return bsResult;
  }

  BitSet getSubstructureSet(String smiles) throws ScriptException {
    PatternMatcher matcher = new PatternMatcher(viewer);
    try {
      return matcher.getSubstructureSet(smiles);
    } catch (InvalidSmilesException e) {
      evalError(e.getMessage());
    }
    return null;
  }

  int getProteinStructureType(Atom atom) {
    return atom.getProteinStructureType();
  }


  int getArgbParam(int itoken) throws ScriptException {
    if (itoken >= statementLength)
      colorExpected();
    if (statement[itoken].tok != Token.colorRGB)
      colorExpected();
    return statement[itoken].intValue;
  }

  int getArgbOrNoneParam(int itoken) throws ScriptException {
    if (itoken >= statementLength)
      colorExpected();
    if (statement[itoken].tok == Token.colorRGB)
      return statement[itoken].intValue;
    if (statement[itoken].tok != Token.none)
      colorExpected();
    return 0;
   }

  void background() throws ScriptException {
    if (statementLength < 2 || statementLength > 3)
      badArgumentCount();
    int tok = statement[1].tok;
    if (tok == Token.colorRGB || tok == Token.none)
      viewer.setBackgroundArgb(getArgbOrNoneParam(1));
    else
      viewer.setShapePropertyArgb(getShapeType(tok), "bgcolor",
                                  getArgbOrNoneParam(2));
  }

  // mth - 2003 01
  // the doc for RasMol says that they use the center of gravity
  // this is currently only using the geometric center
  // but someplace in the rasmol doc it makes reference to the geometric
  // center as the default for rotations. who knows. 
  void center() throws ScriptException {
    viewer.setCenterBitSet(statementLength == 1
                           ? null
                           : expression(statement, 1));
  }

  void color() throws ScriptException {
    if (statementLength > 5 || statementLength < 2)
      badArgumentCount();
    int tok = statement[1].tok;
    outer:
    switch (tok) {
    case Token.colorRGB:
    case Token.none:
    case Token.cpk:
    case Token.amino:
    case Token.chain:
    case Token.group:
    case Token.shapely:
    case Token.structure:
    case Token.temperature:
    case Token.fixedtemp:
    case Token.formalCharge:
    case Token.partialCharge:
    case Token.user:
    case Token.monomer:
    case Token.translucent:
    case Token.opaque:
      colorObject(Token.atom, 1);
      break;
    case Token.rubberband:
      viewer.setRubberbandArgb(getArgbParam(2));
      break;
    case Token.background:
      viewer.setBackgroundArgb(getArgbOrNoneParam(2));
      break;
    case Token.identifier:
    case Token.hydrogen:
      String str = (String)statement[1].value;
      int argb = getArgbOrNoneParam(2);
      if (str.equalsIgnoreCase("dotsConvex")) {
        viewer.setShapePropertyArgb(JmolConstants.SHAPE_DOTS,
                                    "colorConvex", argb);
        return;
      }
      if (str.equalsIgnoreCase("dotsConcave")) {
        viewer.setShapePropertyArgb(JmolConstants.SHAPE_DOTS,
                                    "colorConcave", argb);
        return;
      }
      if (str.equalsIgnoreCase("dotsSaddle")) {
        viewer.setShapePropertyArgb(JmolConstants.SHAPE_DOTS,
                                    "colorSaddle", argb);
        return;
      }
      if (str.equalsIgnoreCase("selectionHalo")) {
        viewer.setSelectionArgb(argb);
        return;
      }
      for (int i = JmolConstants.elementNames.length; --i >= 0; ) {
        if (str.equalsIgnoreCase(JmolConstants.elementNames[i])) {
          viewer.setElementArgb(i, getArgbParam(2));
          return;
        }
      }
      for (int i = JmolConstants.alternateElementNames.length; --i >= 0; ) {
        if (str.equalsIgnoreCase(JmolConstants.alternateElementNames[i])) {
          viewer.setElementArgb(JmolConstants.alternateElementNumbers[i],
                                getArgbParam(2));
          return;
        }
      }
      invalidArgument();
      
    default:
      if (tok == Token.bond) // special hack for bond/bonds confusion
        tok = Token.bonds;
      for (int i = 0; i < shapeToks.length; ++i)
        if (tok == shapeToks[i]) {
          colorObject(tok, 2);
          break outer;
        }
      invalidArgument();
    }
  }

  void colorObject(int tokObject, int itoken) throws ScriptException {
    if (itoken >= statementLength)
      badArgumentCount();
    String translucentOrOpaque = null;
    Object colorvalue = null;
    int shapeType = getShapeType(tokObject);
    String colorOrBgcolor = "color";
    int tok = statement[itoken].tok;
    if (tok == Token.background) {
      colorOrBgcolor = "bgcolor";
      ++itoken;
      tok = statement[itoken].tok;
    }
    if (tok == Token.translucent || tok == Token.opaque) {
      translucentOrOpaque = (String)(statement[itoken].value);
      ++itoken;
    }
    if (itoken < statementLength) {
      colorvalue = statement[itoken].value;
      tok = statement[itoken].tok;
      switch (tok) {
      case Token.none:
      case Token.cpk:
      case Token.formalCharge:
      case Token.partialCharge:
      case Token.structure:
      case Token.amino:
      case Token.shapely:
      case Token.chain:
      case Token.type:
      case Token.temperature:
      case Token.fixedtemp:
        break;
      case Token.group:
        viewer.calcSelectedGroupsCount();
        break;
      case Token.monomer:
        viewer.calcSelectedMonomersCount();
        break;
      case Token.user:
        notImplemented(itoken);
        return;
      case Token.colorRGB:
        int argb = getArgbParam(itoken);
        colorvalue = argb == 0 ? null : new Integer(argb);
        break;
      default:
        invalidArgument();
      }
      viewer.loadShape(shapeType);
      viewer.setShapeProperty(shapeType, colorOrBgcolor, colorvalue);
    }
    if (translucentOrOpaque != null)
      viewer.setShapeProperty(shapeType, "translucency", translucentOrOpaque);
  }

  Hashtable variables = new Hashtable();
  void define() throws ScriptException {
    String variable = (String)statement[1].value;
    variables.put(variable, (expression(statement, 2)));
  }

  void predefine(Token[] statement) {
    String variable = (String)statement[1].value;
    variables.put(variable, statement);
  }

  boolean echoShapeActive = false;

  void echo() {
    String text = "";
    if (statementLength == 2 && statement[1].tok == Token.string)
      text = (String)statement[1].value;
    if (echoShapeActive)
      viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, "echo", text);
    viewer.scriptEcho(text);
  }

  void label() {
    String strLabel = (String)statement[1].value;
    if (strLabel.equalsIgnoreCase("on")) {
      // from the RasMol 2.6b2 manual: RasMol uses the label
      // "%n%r:%c.%a" if the molecule contains more than one chain:
      // "%e%i" if the molecule has only a single residue (a small molecule) and
      // "%n%r.%a" otherwise.
      if (viewer.getModelCount() > 1)
        strLabel = "[%n]%r:%c.%a/%M";
      else if (viewer.getChainCount() > 1)
        strLabel = "[%n]%r:%c.%a";
      else if (viewer.getGroupCount() <= 1)
        strLabel = "%e%i";
      else
        strLabel = "[%n]%r.%a";
    } else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setLabel(strLabel);
  }

  void hover() {
    String strLabel = (String)statement[1].value;
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.loadShape(JmolConstants.SHAPE_HOVER);
    viewer.setShapeProperty(JmolConstants.SHAPE_HOVER, "label", strLabel);
  }

  void load() throws ScriptException {
    int i = 1;
    // ignore optional file format
    if (statement[i].tok == Token.identifier)
      ++i;
    if (statement[i].tok != Token.string)
      filenameExpected();
    //long timeBegin = System.currentTimeMillis();
    if (statementLength == i + 1) {
      String filename = (String)statement[i].value;
      viewer.openFile(filename);
    } else {
      String modelName = (String)statement[i].value;
      i++;
      String[] filenames = new String[statementLength - i];
      while (i < statementLength) {
        filenames[filenames.length - statementLength + i] = (String)statement[i].value;
        i++;
      }
      viewer.openFiles(modelName, filenames);
    }
    String errMsg = viewer.getOpenFileError();
    //int millis = (int)(System.currentTimeMillis() - timeBegin);
    //    System.out.println("!!!!!!!!! took " + millis + " ms");
    if (errMsg != null)
      evalError(errMsg);
    if (logMessages)
      viewer.scriptStatus("Successfully loaded:" + filename);
  }

  int[] monitorArgs = new int[5];

  void monitor() throws ScriptException {
    if (statementLength == 1) {
      viewer.setShowMeasurements(true);
      return;
    }
    if (statementLength == 2) {
      if (statement[1].tok == Token.on)
        viewer.setShowMeasurements(true);
      else if (statement[1].tok == Token.off)
        viewer.clearMeasurements();
      else
        booleanExpected();
      return;
    }
    if (statementLength < 3 || statementLength > 5)
      badArgumentCount();
    for (int i = 1; i < statementLength; ++i) {
      if (statement[i].tok != Token.integer)
        integerExpected();
    }
    int argCount = monitorArgs[0] = statementLength - 1;
    //int numAtoms = viewer.getAtomCount();
    for (int i = 0; i < argCount; ++i) {
      Token token = statement[i + 1];
      if (token.tok != Token.integer)
        integerExpected();
      int atomNumber = token.intValue;
      int atomIndex = viewer.getAtomIndexFromAtomNumber(atomNumber);
      if (atomIndex == -1)
        badAtomNumber();
      monitorArgs[i + 1] = atomIndex;
    }
    viewer.toggleMeasurement(monitorArgs);
  }

  void refresh() {
    viewer.requestRepaintAndWait();
  }

  void reset() {
    viewer.homePosition();
  }

  void restrict() throws ScriptException {
    select();
    viewer.invertSelection();
    boolean bondmode = viewer.getBondSelectionModeOr();
    viewer.setBondSelectionModeOr(true);
    viewer.setShapeSize(JmolConstants.SHAPE_STICKS, 0);

    // also need to turn off backbones, ribbons, strands, cartoons
    for (int shapeType = JmolConstants.SHAPE_MIN_SELECTION_INDEPENDENT;
         --shapeType >= 0; )
      viewer.setShapeSize(shapeType, 0);

    viewer.setLabel(null);

    viewer.setBondSelectionModeOr(bondmode);
    viewer.invertSelection();
  }

  void rotate() throws ScriptException {
    if (statement.length > 3 &&
        statement[1].tok == Token.axisangle) {
      checkStatementLength(6);
      viewer.rotateAxisAngle(floatParameter(2),
                             floatParameter(3),
                             floatParameter(4),
                             floatParameter(5));
      return;
    }
    checkLength3();
    float degrees = floatParameter(2);
    switch (statement[1].tok) {
    case Token.x:
      viewer.rotateXDegrees(degrees);
      break;
    case Token.y:
      viewer.rotateYDegrees(degrees);
      break;
    case Token.z:
      viewer.rotateZDegreesScript(degrees);
      break;
    default:
      axisExpected();
    }
  }

  void pushContext() throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      evalError("too many script levels");
    Context context = new Context();
    context.filename = filename;
    context.script = script;
    context.linenumbers = linenumbers;
    context.lineIndices = lineIndices;
    context.aatoken = aatoken;
    context.pc = pc;
    stack[scriptLevel++] = context;
  }

  void popContext() throws ScriptException {
    if (scriptLevel == 0)
      evalError("RasMol virtual machine error - stack underflow");
    Context context = stack[--scriptLevel];
    stack[scriptLevel] = null;
    filename = context.filename;
    script = context.script;
    linenumbers = context.linenumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    pc = context.pc;
  }
  void script() throws ScriptException {
    // token allows for only 1 parameter
    pushContext();
    String filename = (String)statement[1].value;
    if (! loadScriptFileInternal(filename))
      errorLoadingScript(errorMessage);
    instructionDispatchLoop();
    popContext();
  }

  void select() throws ScriptException {
    // NOTE this is called by restrict()
    if (statementLength == 1) {
      viewer.selectAll();
      if (!viewer.getRasmolHydrogenSetting())
        viewer.excludeSelectionSet(getHydrogenSet());
      if (!viewer.getRasmolHeteroSetting())
        viewer.excludeSelectionSet(getHeteroSet());
    } else {
      viewer.setSelectionSet(expression(statement, 1));
    }
    viewer.scriptStatus("" + viewer.getSelectionCount() + " atoms selected");
  }

  void translate() throws ScriptException {
    if (statementLength <3)
      badArgumentCount();
    if (statement[2].tok != Token.integer)
      integerExpected();
    int percent = statement[2].intValue;
    if (percent > 100 || percent < -100)
      numberOutOfRange(-100, 100);
    switch (statement[1].tok) {
    case Token.x:
      viewer.translateToXPercent(percent);
      break;
    case Token.y:
      viewer.translateToYPercent(percent);
      break;
    case Token.z:
      viewer.translateToZPercent(percent);
      break;
    default:
      axisExpected();
    }
  }

  void zap() {
    viewer.clear();
  }

  void zoom() throws ScriptException {
    // token has ondefault1
    if (statement[1].tok == Token.integer) {
      int percent = statement[1].intValue;
      if (percent < 5 || percent > Viewer.MAXIMUM_ZOOM_PERCENTAGE)
        numberOutOfRange(5, Viewer.MAXIMUM_ZOOM_PERCENTAGE);
      viewer.zoomToPercent(percent);
      return;
    }
    switch (statement[1].tok) {
    case Token.on:
      viewer.setZoomEnabled(true);
      break;
    case Token.off:
      viewer.setZoomEnabled(false);
      break;
    default:
      booleanOrPercentExpected();
    }
  }

  void delay() throws ScriptException {
    long timeBegin = System.currentTimeMillis();
    long millis = 0;
    //token has ondefault1
    Token token = statement[1];
    switch (token.tok) {
    case Token.integer:
    case Token.on: // this is auto-provided as a default
      millis = token.intValue * 1000;
      break;
    case Token.decimal:
      millis = (long)(((Float)token.value).floatValue() * 1000);
      break;
    default:
      numberExpected(); 
    }
    viewer.requestRepaintAndWait();
    millis -= System.currentTimeMillis() - timeBegin;
    if (millis > 0) {
      viewer.popHoldRepaint();
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
      }
      viewer.pushHoldRepaint();
    }
  }

  void move() throws ScriptException {
    if (statementLength < 10 || statementLength > 12)
      badArgumentCount();
    float dRotX = floatParameter(1);
    float dRotY = floatParameter(2);
    float dRotZ = floatParameter(3);
    int dZoom = intParameter(4);
    int dTransX = intParameter(5);
    int dTransY = intParameter(6);
    int dTransZ = intParameter(7);
    int dSlab = intParameter(8);
    float floatSecondsTotal = floatParameter(9);
    int fps = 30/*, maxAccel = 5*/;
    if (statementLength > 10) {
      fps = statement[10].intValue;
      if (statementLength > 11) {
        //maxAccel = statement[11].intValue;
      }
    }

    int zoom = viewer.getZoomPercent();
    int slab = viewer.getSlabPercentSetting();
    float transX = viewer.getTranslationXPercent();
    float transY = viewer.getTranslationYPercent();
    float transZ = viewer.getTranslationZPercent();

    long timeBegin = System.currentTimeMillis();
    int timePerStep = 1000 / fps;
    int totalSteps = (int)(fps * floatSecondsTotal);
    float radiansPerDegreePerStep = (float)Math.PI / 180 / totalSteps;
    float radiansXStep = radiansPerDegreePerStep * dRotX;
    float radiansYStep = radiansPerDegreePerStep * dRotY;
    float radiansZStep = radiansPerDegreePerStep * dRotZ;
    viewer.setInMotion(true);
    if (totalSteps == 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    for (int i = 1; i <= totalSteps && !interruptExecution; ++i) {
      if (dRotX != 0)
        viewer.rotateXRadians(radiansXStep);
      if (dRotY != 0)
        viewer.rotateYRadians(radiansYStep);
      if (dRotZ != 0)
        viewer.rotateZRadians(radiansZStep);
      if (dZoom != 0)
        viewer.zoomToPercent(zoom + dZoom * i / totalSteps);
      if (dTransX != 0)
        viewer.translateToXPercent(transX + dTransX * i / totalSteps);
      if (dTransY != 0)
        viewer.translateToYPercent(transY + dTransY * i / totalSteps);
      if (dTransZ != 0)
        viewer.translateToZPercent(transZ + dTransZ * i / totalSteps);
      if (dSlab != 0)
        viewer.slabToPercent(slab + dSlab * i / totalSteps);
      int timeSpent = (int)(System.currentTimeMillis() - timeBegin);
      int timeAllowed = i * timePerStep;
      if (timeSpent < timeAllowed) {
        viewer.requestRepaintAndWait();
        timeSpent = (int)(System.currentTimeMillis() - timeBegin);
        int timeToSleep = timeAllowed - timeSpent;
        if (timeToSleep > 0) {
          try {
            Thread.sleep(timeToSleep);
          } catch (InterruptedException e) {
          }
        }
      }
    }
    viewer.setInMotion(false);
  }

  void slab() throws ScriptException {
    //token has ondefault1
    if (statement[1].tok == Token.integer) {
      int percent = statement[1].intValue;
      if (percent < 0 || percent > 100)
        numberOutOfRange(0, 100);
      viewer.slabToPercent(percent);
      return;
    }
    switch (statement[1].tok) {
    case Token.on:
      viewer.setSlabEnabled(true);
      break;
    case Token.off:
      viewer.setSlabEnabled(false);
      break;
    default:
      booleanOrPercentExpected();
    }
  }

  void depth() throws ScriptException {
    viewer.depthToPercent(intParameter(1));
  }

  void star() throws ScriptException {
    short mad = 0;
    int tok = Token.on;
    if (statementLength > 1) {
      tok = statement[1].tok;
      if (! ((statementLength == 2) ||
             (statementLength == 3 &&
              tok == Token.integer &&
              statement[2].tok == Token.percent))) {
        badArgumentCount();
      }
    }
    switch (tok) {
    case Token.on:
    case Token.vanderwaals:
      mad = -100; // cpk with no args goes to 100%
      break;
    case Token.off:
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      if (statementLength == 2) {
        if (radiusRasMol >= 750 || radiusRasMol < -100)
          numberOutOfRange(-100, 749);
        mad = (short)radiusRasMol;
        if (radiusRasMol > 0)
          mad *= 4 * 2;
      } else {
        if (radiusRasMol < 0 || radiusRasMol > 100)
          numberOutOfRange(0, 100);
        mad = (short)-radiusRasMol; // use a negative number to specify %vdw
      }
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short)(angstroms * 1000 * 2);
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
    viewer.setShapeSize(JmolConstants.SHAPE_STARS, mad);
  }

  void cpk() throws ScriptException {
    short mad = 0;
    int tok = Token.on;
    if (statementLength > 1) {
      tok = statement[1].tok;
      if (! ((statementLength == 2) ||
             (statementLength == 3 &&
              tok == Token.integer &&
              statement[2].tok == Token.percent))) {
        badArgumentCount();
      }
    }
    switch (tok) {
    case Token.on:
    case Token.vanderwaals:
      mad = -100; // cpk with no args goes to 100%
      break;
    case Token.off:
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      if (statementLength == 2) {
        if (radiusRasMol >= 750 || radiusRasMol < -200)
          numberOutOfRange(-200, 749);
        mad = (short)radiusRasMol;
        if (radiusRasMol > 0)
          mad *= 4 * 2;
      } else {
        if (radiusRasMol < 0 || radiusRasMol > 200)
          numberOutOfRange(0, 200);
        mad = (short)-radiusRasMol; // use a negative number to specify %vdw
      }
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short)(angstroms * 1000 * 2);
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
    viewer.setShapeSize(JmolConstants.SHAPE_BALLS, mad);
  }

  short getMadParameter() throws ScriptException {
    //for wireframe, ssbond, hbond
    int tok = statement[1].tok;
    short mad = 1;
    switch (tok) {
    case Token.on:
      break;
    case Token.off:
      mad = 0;
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      if (radiusRasMol < 0 || radiusRasMol > 750)
        numberOutOfRange(0, 750);
      mad = (short)(radiusRasMol * 4 * 2);
      break;
    case Token.decimal:
      float angstroms = floatParameter(1);
      if (angstroms < 0 || angstroms > 3)
        numberOutOfRange(0f, 3f);
      mad = (short)(angstroms * 1000 * 2);
      break;
    default:
      booleanOrNumberExpected();
    }
    return mad;
  }

  void wireframe() throws ScriptException {
    viewer.setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter());
  }

  void ssbond() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    viewer.setShapeSize(JmolConstants.SHAPE_SSSTICKS, getMadParameter());
  }

  void hbond() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_HSTICKS);
    if (statementLength == 2 && statement[1].tok == Token.identifier && 
       ((String)statement[1].value).equalsIgnoreCase("calculate")) {
      BitSet bs = viewer.getSelectionSet();
      viewer.getFrame().autoHbond(bs, bs);
      return;
    }
    viewer.setShapeSize(JmolConstants.SHAPE_HSTICKS, getMadParameter());
  }

  void vector() throws ScriptException {
    short mad = 1;
    if (statementLength > 1) {
      switch (statement[1].tok) {
      case Token.on:
        break;
      case Token.off:
        mad = 0;
        break;
      case Token.integer:
        int diameterPixels = statement[1].intValue;
        if (diameterPixels < 0 || diameterPixels >= 20)
          numberOutOfRange(0, 19);
        mad = (short)diameterPixels;
        break;
      case Token.decimal:
        float angstroms = floatParameter(1);
        if (angstroms < 0 || angstroms > 3)
          numberOutOfRange(0f, 3f);
        mad = (short)(angstroms * 1000 * 2);
        break;
      case Token.identifier:
        String cmd = (String)statement[1].value;
        if (! cmd.equalsIgnoreCase("scale"))
          unrecognizedSubcommand();
        vectorScale();
        return;
      default:
        booleanOrNumberExpected();
      }
      checkLength2();
    }
    viewer.setShapeSize(JmolConstants.SHAPE_VECTORS, mad);
  }

  void vectorScale() throws ScriptException {
    checkLength3();
    float scale = floatParameter(2);
    if (scale < -10 || scale > 10)
      numberOutOfRange(-10f, 10f);
    viewer.setVectorScale(scale);
  }

  void animation() throws ScriptException {
    if (statementLength < 2)
      subcommandExpected();
    int tok = statement[1].tok;
    boolean animate = false;
    switch(tok) {
    case Token.on:
      animate = true;
    case Token.off:
      viewer.setAnimationOn(animate);
      break;
    case Token.information:
      showAnimation();
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
      viewer.setAnimationFps(getSetInteger());
      break;
    default:
      unrecognizedSubcommand();
    }
  }

  void animationMode() throws ScriptException {
    float startDelay = 1, endDelay = 1;
    if (statementLength < 3 || statementLength > 5)
      badArgumentCount();
    int animationMode = 0;
    switch (statement[2].tok) {
    case Token.loop:
      ++animationMode;
      break;
    case Token.identifier:
      String cmd = (String)statement[2].value;
      if (cmd.equalsIgnoreCase("once")) {
        startDelay = endDelay = 0;
        break;
      }
      if (cmd.equalsIgnoreCase("palindrome")) {
        animationMode = 2;
        break;
      }
      unrecognizedSubcommand();
    }
    if (statementLength >= 4) {
      startDelay = endDelay = floatParameter(3);
      if (statementLength == 5)
        endDelay = floatParameter(4);
    }
    viewer.setAnimationReplayMode(animationMode, startDelay, endDelay);
  }

  void vibration() throws ScriptException {
    if (statementLength < 2)
      subcommandExpected();
    Token token = statement[1];
    float period = 0;
    switch(token.tok) {
    case Token.off:
    case Token.on:
    case Token.integer:
      period = token.intValue;
      break;
    case Token.decimal:
      period = floatParameter(1);
      break;
    case Token.identifier:
      String cmd = (String)statement[1].value;
      if (cmd.equalsIgnoreCase("scale")) {
        vibrationScale();
        return;
      }
    default:
      unrecognizedSubcommand();
    }
    viewer.setVibrationPeriod(period);
  }

  void vibrationScale() throws ScriptException {
    checkLength3();
    float scale = floatParameter(2);
    if (scale < -10 || scale > 10)
      numberOutOfRange(-10f, 10f);
    viewer.setVibrationScale(scale);
  }

  void animationDirection() throws ScriptException {
    checkStatementLength(4);
    boolean negative = false;
    if (statement[2].tok == Token.hyphen)
      negative = true;
    else if (statement[2].tok != Token.plus)
      invalidArgument();

    if (statement[3].tok != Token.integer)
      invalidArgument();
    int direction = statement[3].intValue;
    if (direction != 1)
      numberMustBe(1, -1);
    if (negative)
      direction = -direction;
    viewer.setAnimationDirection(direction);
  }


  /*
  void animate() throws ScriptException {
    if (statement.length < 2 || statement[1].tok != Token.identifier)
      unrecognizedSubcommand();
    String cmd = (String)statement[1].value;
    if (cmd.equalsIgnoreCase("frame")) {
      if (statement.length != 3 || statement[2].tok != Token.integer)
        integerExpected();
      int frame = statement[2].intValue;
      if (frame < 0 || frame >= viewer.getNumberOfFrames()) 
       numberOutOfRange();
      viewer.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("next")) {
      int frame = viewer.getCurrentFrameNumber() + 1;
      if (frame < viewer.getNumberOfFrames())
        viewer.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("prev")) {
      int frame = viewer.getCurrentFrameNumber() - 1;
      if (frame >= 0)
        viewer.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("nextwrap")) {
      int frame = viewer.getCurrentFrameNumber() + 1;
      if (frame >= viewer.getNumberOfFrames())
        frame = 0;
      viewer.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("prevwrap")) {
      int frame = viewer.getCurrentFrameNumber() - 1;
      if (frame < 0)
        frame = viewer.getNumberOfFrames() - 1;
      viewer.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("play")) {
      animatePlay(true);
    } else if (cmd.equalsIgnoreCase("revplay")) {
      animatePlay(false);
    } else if (cmd.equalsIgnoreCase("rewind")) {
      viewer.setFrame(0);
    } else {
      unrecognizedSubcommand();
    }
  }

  void animatePlay(boolean forward) {
    int nframes = viewer.getNumberOfFrames();
    long timeBegin = System.currentTimeMillis();
    long targetTime = timeBegin;
    int frameTimeMillis = 100;
    int frameBegin, frameEnd, frameDelta;
    if (forward) {
      frameBegin = 0;
      frameEnd = nframes;
      frameDelta = 1;
    } else {
      frameBegin = nframes - 1;
      frameEnd = -1;
      frameDelta = -1;
    }
    viewer.setInMotion(true);
    for (int frame = frameBegin; frame != frameEnd; frame += frameDelta) {
      viewer.setFrame(frame);
      refresh();
      targetTime += frameTimeMillis;
      long sleepTime = targetTime - System.currentTimeMillis();
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }
      }
    }
    viewer.setInMotion(false);
  }
  */

  void dots() throws ScriptException {
    // token has onDefault1
    short mad = 0;
    switch (statement[1].tok) {
    case Token.on:
    case Token.vanderwaals:
      mad = 1;
      break;
    case Token.ionic:
      mad = -1;
      break;
    case Token.off:
      break;
    case Token.integer:
      int dotsParam = statement[1].intValue;
      if (dotsParam < 0 || dotsParam > 1000)
        numberOutOfRange(0, 1000);
      // I don't know what to do with this thing yet
      mad = (short)dotsParam;
      break;
    default:
      booleanOrNumberExpected();
    }
    viewer.setShapeSize(JmolConstants.SHAPE_DOTS, mad);
  }

  void proteinShape(int shapeType) throws ScriptException {
    short mad = 0;
    //token has ondefault1
    int tok = statement[1].tok;
    switch (tok) {
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
      //      mad = -3;
      //      break;
    case Token.displacement:
      mad = -4;
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      //currently not possible to get here with < 0, but that may change
      //this redundancy is safer
      if (radiusRasMol < 0 || radiusRasMol >= 500)
        numberOutOfRange(0, 499);
      mad = (short)(radiusRasMol * 4 * 2);
      break;
    case Token.decimal:
      float angstroms = ((Float)statement[1].value).floatValue();
      if (angstroms < 0 || angstroms > 4)
        numberOutOfRange(0f, 4f);
      mad = (short)(angstroms * 1000 * 2);
      break;
    default:
      booleanOrNumberExpected();
    }
    viewer.setShapeSize(shapeType, mad);
  }

  void spin() throws ScriptException {
    boolean spinOn = false;
    switch (statement[1].tok) {
    case Token.on:
      spinOn = true;
    case Token.off:
      break;
    default:
      booleanExpected();
    }
    viewer.setSpinOn(spinOn);
  }
      
  void frame() throws ScriptException {
    frame(1);
  }

  void frame(int offset) throws ScriptException {
    if (statementLength <= offset)
      badArgumentCount();
    if (statement[offset].tok == Token.hyphen) {
      ++offset;
      checkStatementLength(offset + 1);
      if (statement[offset].tok != Token.integer ||
          statement[offset].intValue != 1)
        invalidArgument();
      viewer.setAnimationPrevious();
      return;
    }
    if (statementLength != offset + 1)
      badArgumentCount();
    int modelNumber = -1;
    switch(statement[offset].tok) {
    case Token.all:
    case Token.asterisk:
      break;
    case Token.none:
      break;
    case Token.integer:
      modelNumber = statement[offset].intValue;
      break;
    case Token.identifier:
      String ident = (String)statement[offset].value;
      if (ident.equalsIgnoreCase("next")) {
        viewer.setAnimationNext();
        return;
      }
      if (ident.equalsIgnoreCase("prev")) {
        viewer.setAnimationPrevious();
        return;
      }
      if (ident.equalsIgnoreCase("play")) {
        viewer.setAnimationOn(true,viewer.getDisplayModelIndex());
        return;
      }
      break;
    default:
      invalidArgument();
    }
    int modelIndex = viewer.getModelNumberIndex(modelNumber);
    viewer.setDisplayModelIndex(modelIndex);
  }

  // note that this array *MUST* be in the same sequence as the
  // SHAPE_* constants in JmolConstants
  
  private final static int[] shapeToks =
  {Token.atom, Token.bonds, Token.hbond, Token.ssbond,
   Token.label, Token.vector,
   Token.monitor, Token.dots, Token.backbone,
   Token.trace, Token.cartoon, Token.strands, Token.meshRibbon, Token.ribbon,
   Token.rocket, Token.star,
   Token.axes, Token.boundbox, Token.unitcell, Token.frank, Token.echo,
   Token.hover, Token.pmesh, Token.polyhedra, Token.sasurface,
   Token.isosurface,
   Token.prueba,
  };

  static {
    if (shapeToks.length != JmolConstants.SHAPE_MAX) {
      System.out.println("shapeToks mismatch");
      throw new NullPointerException();
    }
  }

  int getShapeType(int tok) throws ScriptException {
    for (int i = shapeToks.length; --i >= 0; )
      if (tok == shapeToks[i])
        return i;
    unrecognizedColorObject();
    return -1;
  }

  void font() throws ScriptException {
    int shapeType = 0;
    int fontsize = 0;
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    switch (statementLength) {
    case 5:
      if (statement[4].tok != Token.identifier)
        keywordExpected();
      fontstyle = (String)statement[4].value;
    case 4:
      if (statement[3].tok != Token.identifier)
        keywordExpected();
      fontface = (String)statement[3].value;
    case 3:
      if (statement[2].tok != Token.integer)
        integerExpected();
      fontsize = statement[2].intValue;
      shapeType = getShapeType(statement[1].tok);
      break;
    default:
      badArgumentCount();
    }
    /*
    System.out.println("font <obj> fontsize=" + fontsize);
    System.out.println("fontface=" + fontface + " fontstyle=" + fontstyle);
    */
    Font3D font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
    viewer.setShapeProperty(shapeType, "font", font3d);
  }

  /*==============================================================*
   * SET implementations
   *==============================================================*/

  void set() throws ScriptException {
    switch (statement[1].tok) {
    case Token.axes:
      setAxes();
      break;
    case Token.bondmode:
      setBondmode();
      break;
    case Token.bonds:
      setBonds();
      break;
    case Token.boundbox:
      setBoundbox();
      break;
    case Token.color:
      System.out.println("WARNING! use 'set defaultColors' not 'set color'");
      // fall into
    case Token.defaultColors:
      setDefaultColors();
      break;
    case Token.debugscript:
      setDebugScript();
      break;
    case Token.display:
      setDisplay();
      break;
    case Token.echo:
      setEcho();
      break;
    case Token.fontsize:
      setFontsize();
      break;
    case Token.frank:
      setFrank();
      break;
    case Token.hetero:
      setHetero();
      break;
    case Token.hydrogen:
      setHydrogen();
      break;
    case Token.labeloffset:
      setLabelOffset();
      break;
    case Token.monitor:
      setMonitor();
      break;
    case Token.property:
      setProperty();
      break;
    case Token.solvent:
      setSolvent();
      break;
    case Token.radius:
      setRadius();
      break;
    case Token.strands:
      setStrands();
      break;
    case Token.specular:
      setSpecular();
      break;
    case Token.specpower:
      setSpecPower();
      break;
    case Token.ambient:
      setAmbient();
      break;
    case Token.diffuse:
      setDiffuse();
      break;
    case Token.spin:
      setSpin();
      break;
    case Token.ssbond:
      setSsbond();
      break;
    case Token.hbond:
      setHbond();
      break;
    case Token.scale3d:
      setScale3d();
      break;
    case Token.unitcell:
      setUnitcell();
      break;
    case Token.picking:
      setPicking();
      break;
    case Token.pickingStyle:
        setPickingStyle();
        break;
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
    case Token.formalCharge: // set charge in Chime
      notImplemented(1);
      break;
    case Token.identifier:
      viewer.setBooleanProperty((String)statement[1].value, getSetBoolean());
      break;
    case Token.background:
    case Token.stereo:
      setspecialShouldNotBeHere();
    default:
      unrecognizedSetParameter();
    }
  }

  void setAxes() throws ScriptException {
    viewer.setShapeSize(JmolConstants.SHAPE_AXES,
                        getSetAxesTypeMad());
  }

  void setBoundbox() throws ScriptException {
    viewer.setShapeSize(JmolConstants.SHAPE_BBCAGE,
                        getSetAxesTypeMad());
  }

  void setUnitcell() throws ScriptException {
    viewer.setShapeSize(JmolConstants.SHAPE_UCCAGE,
                        getSetAxesTypeMad());
  }

  void setFrank() throws ScriptException {
    viewer.setShapeSize(JmolConstants.SHAPE_FRANK,
                        getSetAxesTypeMad());
  }

  void setDefaultColors() throws ScriptException {
    checkLength3();
    switch(statement[2].tok) {
    case Token.rasmol:
    case Token.jmol:
      viewer.setDefaultColors((String)statement[2].value);
      break;
    default:
      invalidArgument();
    }
  }

  void setBondmode() throws ScriptException {
    checkLength3();
    boolean bondmodeOr = false;
    switch(statement[2].tok) {
    case Token.opAnd:
      break;
    case Token.opOr:
      bondmodeOr = true;
      break;
    default:
      invalidArgument();
    }
    viewer.setBondSelectionModeOr(bondmodeOr);
  }

  void setBonds() throws ScriptException {
    viewer.setShowMultipleBonds(getSetBoolean());
  }

  void setDisplay() throws ScriptException {
    boolean showHalo = false;
    checkLength3();
    switch (statement[2].tok) {
    case Token.selected:
      showHalo = true;
    case Token.normal:
      viewer.setSelectionHaloEnabled(showHalo);
      break;
    default:
      keywordExpected();
    }
  }

  void setEcho() throws ScriptException {
    String propertyName = "target";
    String propertyValue = null;
    checkLength34();
    echoShapeActive = true;
    switch (statement[2].tok) {
    case Token.off:
      echoShapeActive = false;
      propertyName = "off";
      break;
    case Token.none:
      echoShapeActive = false;
    case Token.identifier:
      propertyValue=(String)statement[2].value;
      break;
    default:
      keywordExpected();
    }
    viewer.loadShape(JmolConstants.SHAPE_ECHO);
    viewer.setShapeProperty(JmolConstants.SHAPE_ECHO,
                            propertyName, propertyValue);
    if (statementLength == 4) {
      int tok = statement[3].tok;
      if (tok != Token.identifier && tok != Token.center)
        keywordExpected();
      viewer.setShapeProperty(JmolConstants.SHAPE_ECHO,
                              "align", (String)statement[3].value);
    }
  }
  
  void setFontsize() throws ScriptException {
    int rasmolSize = 8;
    if (statementLength == 3) {
      rasmolSize=getSetInteger();
      // this is a kludge/hack to be somewhat compatible with RasMol
      rasmolSize += 5;
      
      if (rasmolSize < JmolConstants.LABEL_MINIMUM_FONTSIZE ||
          rasmolSize > JmolConstants.LABEL_MAXIMUM_FONTSIZE)
        numberOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE,
            JmolConstants.LABEL_MINIMUM_FONTSIZE);
    }
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "fontsize",
                            new Integer(rasmolSize));
  }

  void setLabelOffset() throws ScriptException {
    checkLength4();
    int xOffset = intParameter(2);
    int yOffset = intParameter(3);
    int offset = ((xOffset & 0xFF) << 8) | (yOffset & 0xFF);
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "offset",
                            new Integer(offset));
  }

  void setHetero() throws ScriptException {
    viewer.setRasmolHeteroSetting(getSetBoolean());
  }

  void setHydrogen() throws ScriptException {
    viewer.setRasmolHydrogenSetting(getSetBoolean());
  }

  void setMonitor() throws ScriptException {
    boolean showMeasurementNumbers = false;
    checkLength3();
    switch (statement[2].tok) {
    case Token.on:
      showMeasurementNumbers = true;
    case Token.off:
      viewer.setShapeProperty(JmolConstants.SHAPE_MEASURES,
                              "showMeasurementNumbers",
                              showMeasurementNumbers
                              ? Boolean.TRUE
                              : Boolean.FALSE);
      return;
    case Token.identifier:
      if (! viewer.setMeasureDistanceUnits((String)statement[2].value))
        unrecognizedSetParameter();
      return;
    }
    viewer.setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad());
  }

  void setDebugScript() throws ScriptException {
    viewer.setDebugScript(getSetBoolean());
  }

  void setProperty() throws ScriptException {
    checkLength4();
    if (statement[2].tok != Token.identifier)
      propertyNameExpected();
    String propertyName = (String)statement[2].value;
    switch (statement[3].tok) {
    case Token.on:
      viewer.setBooleanProperty(propertyName, true);
      break;
    case Token.off:
      viewer.setBooleanProperty(propertyName, false);
      break;
    case Token.integer:
    case Token.decimal:
    case Token.string:
      notImplemented(3);
    default:
      unrecognizedSetParameter();
    }
  }

  void setSolvent() throws ScriptException {
    viewer.setSolventOn(getSetBoolean());
  }

  void setRadius() throws ScriptException {
    viewer.setSolventProbeRadius(getSetAngstroms());
  }

  void setStrands() throws ScriptException {
    int strandCount = 5;
    if (statementLength == 3) {
      if (statement[2].tok != Token.integer)
        integerExpected();
      strandCount = statement[2].intValue;
      if (strandCount < 0 || strandCount > 20)
        numberOutOfRange(0, 20);
    }
    viewer.setShapeProperty(JmolConstants.SHAPE_STRANDS,
                            "strandCount", new Integer(strandCount));
  }

  void setSpecular() throws ScriptException {
    checkLength3();
    if (statement[2].tok == Token.integer)
      viewer.setSpecularPercent(getSetInteger());
    else
      viewer.setSpecular(getSetBoolean());
  }

  void setSpecPower() throws ScriptException {
    viewer.setSpecularPower(getSetInteger());
  }

  void setAmbient() throws ScriptException {
    viewer.setAmbientPercent(getSetInteger());
  }

  void setDiffuse() throws ScriptException {
    viewer.setDiffusePercent(getSetInteger());
  }

  void setSpin() throws ScriptException {
    checkLength4();
    int value = intParameter(3);
    switch (statement[2].tok) {
    case Token.x:
      viewer.setSpinX(value);
      break;
    case Token.y:
      viewer.setSpinY(value);
      break;
    case Token.z:
      viewer.setSpinZ(value);
      break;
    case Token.fps:
      viewer.setSpinFps(value);
      break;
    default:
      unrecognizedSetParameter();
    }
  }

  void setSsbond() throws ScriptException {
    checkLength3();
    boolean ssbondsBackbone = false;
    viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch(statement[2].tok) {
    case Token.backbone:
      ssbondsBackbone = true;
      break;
    case Token.sidechain:
      break;
    default:
      invalidArgument();
    }
    viewer.setSsbondsBackbone(ssbondsBackbone);
  }

  void setHbond() throws ScriptException {
    checkLength3();
    boolean bool = false;
    switch(statement[2].tok) {
    case Token.backbone:
      bool = true;
      // fall into
    case Token.sidechain:
      viewer.setHbondsBackbone(bool);
      break;
    case Token.solid:
      bool = true;
      // falll into
    case Token.dotted:
      viewer.setHbondsSolid(bool);
      break;
    default:
      invalidArgument();
    }
  }

  void setScale3d() throws ScriptException {
    checkLength3();
    float angstromsPerInch = 0;
    switch (statement[2].tok) {
    case Token.decimal:
      angstromsPerInch = ((Float)statement[2].value).floatValue();
      break;
    case Token.integer:
      angstromsPerInch = statement[2].intValue;
      break;
    default:
      numberExpected();
    }
    viewer.setScaleAngstromsPerInch(angstromsPerInch);
  }

  void setPicking() throws ScriptException {
    int pickingMode = JmolConstants.PICKING_IDENT;
    if (statementLength >= 3) {
      switch (statement[2].tok) {
      case Token.none:
      case Token.off:
        pickingMode = JmolConstants.PICKING_OFF;
        //fall into
      case Token.on:
        break;
      case Token.ident:
        pickingMode = JmolConstants.PICKING_IDENT;
        break;
      case Token.distance:
        pickingMode = JmolConstants.PICKING_DISTANCE;
        break;
      case Token.monitor:
        pickingMode = JmolConstants.PICKING_MONITOR;
        break;
      case Token.angle:
        pickingMode = JmolConstants.PICKING_ANGLE;
        break;
      case Token.torsion:
        pickingMode = JmolConstants.PICKING_TORSION;
        break;
      case Token.label:
        pickingMode = JmolConstants.PICKING_LABEL;
        break;
      case Token.center:
        pickingMode = JmolConstants.PICKING_CENTER;
        break;
      case Token.coord:
        pickingMode = JmolConstants.PICKING_COORD;
        break;
      case Token.bond:
        pickingMode = JmolConstants.PICKING_BOND;
        break;
      case Token.atom:
        pickingMode = JmolConstants.PICKING_SELECT_ATOM;
        break;
      case Token.group:
        pickingMode = JmolConstants.PICKING_SELECT_GROUP;
        break;
      case Token.chain:
        pickingMode = JmolConstants.PICKING_SELECT_CHAIN;
        break;
      case Token.select:
        pickingMode = JmolConstants.PICKING_SELECT_ATOM;
        if (statementLength == 4) {
          switch (statement[3].tok) {
          case Token.chain:
            pickingMode = JmolConstants.PICKING_SELECT_CHAIN;
            // fall into
          case Token.atom:
            break;
          case Token.group:
            pickingMode = JmolConstants.PICKING_SELECT_GROUP;
            break;
          default:
            invalidArgument();
          }
        }
        break;
      default:
        invalidArgument();
      }
    }
    viewer.setPickingMode(pickingMode);
  }

  void setPickingStyle() throws ScriptException {
    int pickingStyle = JmolConstants.PICKINGSTYLE_CHIME;
    if (statementLength >= 3) {
      switch (statement[2].tok) {
      case Token.none:
      case Token.chime:
          pickingStyle = JmolConstants.PICKINGSTYLE_CHIME;
          break;
      case Token.rasmol:
          pickingStyle = JmolConstants.PICKINGSTYLE_RASMOL;
          break;
      case Token.pfaat:
          pickingStyle = JmolConstants.PICKINGSTYLE_PFAAT;
          break;
      default:
        invalidArgument();
      }
    }
    viewer.setPickingStyle(pickingStyle);
  }  

  /*==============================================================*
   * SHOW implementations
   *==============================================================*/

  void show() throws ScriptException {
    switch(statement[1].tok) {
    case Token.pdbheader:
      showPdbHeader();
      break;
    case Token.model:
      showModel();
      break;
    case Token.animation:
      showAnimation();
      break;
    case Token.orientation:
      showOrientation();
      break;
    case Token.transform:
      showTransform();
      break;
    case Token.center:
      showCenter();
      break;
    case Token.file:
      showFile();
      break;
    case Token.boundbox:
      showBoundbox();
      break;
    case Token.zoom:
      showZoom();
      break;

      // not implemented
    case Token.spin:
    case Token.list:
    case Token.mlp:
    case Token.information:
    case Token.phipsi:
    case Token.ramprint:
    case Token.rotation:
    case Token.group:
    case Token.chain:
    case Token.atom:
    case Token.sequence:
    case Token.symmetry:
    case Token.translation:
    case Token.residue:
    case Token.all:
    case Token.selected:
      notImplemented(1);
      break;

    default:
      evalError("unrecognized SHOW parameter");
    }
  }

  void showString(String str) {
    System.out.println("show:" + str);
    viewer.scriptStatus(str);
  }

  final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
                                       "MODEL ", "SCALE",  "HETATM", "SEQRES",
                                       "DBREF ", };

  void showPdbHeader() {
    if ("pdb" != viewer.getModelSetTypeName()) {
      showString("!Not a pdb file!");
      return;
    }
    String modelFile = viewer.getCurrentFileAsString();
    int ichMin = modelFile.length();
    for (int i = pdbRecords.length; --i >= 0; ) {
      int ichFound = -1;
      String strRecord = pdbRecords[i];
      if (modelFile.startsWith(strRecord))
        ichFound = 0;
      else {
        String strSearch = "\n" + strRecord;
        ichFound = modelFile.indexOf(strSearch);
        if (ichFound >= 0)
          ++ichFound;
      }
      if (ichFound >= 0 && ichFound < ichMin)
        ichMin = ichFound;
    }
    showString(modelFile.substring(0, ichMin));
  }

  void showModel() {
    int modelCount = viewer.getModelCount();
    showString("model count = " + modelCount +
               "\nmodelSetHasVibrationVectors:" +
               viewer.modelSetHasVibrationVectors());
    Properties props = viewer.getModelSetProperties();
    printProperties(props);
    for (int i = 0; i < modelCount; ++i) {
      showString("" + i + ":" + viewer.getModelNumber(i) +
                 ":" + viewer.getModelName(i) +
                 "\nmodelHasVibrationVectors:" +
                 viewer.modelHasVibrationVectors(i));
      printProperties(viewer.getModelProperties(i));
    }
  }

  void showFile() throws ScriptException {
    System.out.println("showFile && statementLength=" + statementLength);
    if (statementLength == 2) {
      showString(viewer.getCurrentFileAsString());
      return;
    }
    if (statementLength == 3 && statement[2].tok == Token.string) {
      String fileName = (String)statement[2].value;
      System.out.println("fileName=" + fileName);
      showString(viewer.getFileAsString(fileName));
      return;
    }
    invalidArgument();
  }

  void printProperties(Properties props) {
    if (props == null) {
      showString("Properties: null");
    } else {
      Enumeration e = props.propertyNames();
      showString("Properties:");
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        showString(" " + propertyName + "=" +
                   props.getProperty(propertyName));
      }
    }
    System.out.println("");
  }

  void showAnimation() {
    showString("show animation information goes here");
  }

  void showOrientation() {
    showString(viewer.getOrientationText());
  }

  void showTransform() {
    showString("transform:\n" + viewer.getTransformText());
  }

  void showCenter() {
    showString("center: " + viewer.getCenter());
  }

  void showZoom() {
    showString("zoom " + (viewer.getZoomEnabled()
                           ? ("" + viewer.getZoomPercentSetting())
                           : "off"));
  }

  void showBoundbox() {
    showString("boundbox: " + viewer.getBoundBoxCenter() +
               " " + viewer.getBoundBoxCornerVector());
  }

  AxisAngle4f aaMoveTo;
  AxisAngle4f aaStep;
  AxisAngle4f aaTotal;
  Matrix3f matrixStart;
  Matrix3f matrixInverse;
  Matrix3f matrixStep;
  Matrix3f matrixEnd;

  void moveto() throws ScriptException {
    if (statementLength < 6 || statementLength > 9)
      badArgumentCount();
    float floatSecondsTotal = floatParameter(1);
    float axisX = floatParameter(2);
    float axisY = floatParameter(3);
    float axisZ = floatParameter(4);
    float degrees = floatParameter(5);
    int zoom = statementLength >= 7 ? intParameter(6) : 100;
    int xTrans = statementLength >= 8 ? intParameter(7) : 0;
    int yTrans = statementLength >= 9 ? intParameter(8) : 0;

    if (aaMoveTo == null) {
      aaMoveTo = new AxisAngle4f();
      aaStep = new AxisAngle4f();
      aaTotal = new AxisAngle4f();
      matrixStart = new Matrix3f();
      matrixEnd = new Matrix3f();
      matrixStep = new Matrix3f();
      matrixInverse = new Matrix3f();
    }
    if (degrees < 0.01f && degrees > -0.01f) {
      matrixEnd.setIdentity();
    } else {
      if (axisX == 0 && axisY == 0 && axisZ == 0) {
        // invalid ... no rotation
        int sleepTime = (int)(floatSecondsTotal * 1000) - 30;
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
          }
        }
        return;
      }
      aaMoveTo.set(axisX, axisY, axisZ, degrees * (float)Math.PI / 180);
      matrixEnd.set(aaMoveTo);
    }
    viewer.getRotation(matrixStart);
    matrixInverse.invert(matrixStart);

    matrixStep.mul(matrixEnd, matrixInverse);
    aaTotal.set(matrixStep);

    /*
    System.out.println("\nmatrixStart=\n" + matrixStart +
                       "\nmatrixInverse=\n" + matrixInverse +
                       "\nmatrixStep=\n" + matrixStep +
                       "\naaStep=\n" + aaStep);
    */

    int fps = 30;
    int totalSteps = (int)(floatSecondsTotal * fps);
    if (totalSteps > 1) {
      aaStep.angle /= totalSteps;
      int frameTimeMillis = 1000 / fps;
      long targetTime = System.currentTimeMillis();
      int zoomStart = viewer.getZoomPercent();
      int zoomDelta = zoom - zoomStart;
      float xTransStart = viewer.getTranslationXPercent();
      float xTransDelta = xTrans - xTransStart;
      float yTransStart = viewer.getTranslationYPercent();
      float yTransDelta = yTrans - yTransStart;
      for (int i = 1; i < totalSteps; ++i) {

        viewer.getRotation(matrixStart);
        matrixInverse.invert(matrixStart);
        matrixStep.mul(matrixEnd, matrixInverse);
        aaTotal.set(matrixStep);

        aaStep.set(aaTotal);
        aaStep.angle /= (totalSteps - i + 1);
        if (aaStep.angle == 0)
          matrixStep.setIdentity();
        else
          matrixStep.set(aaStep);
        matrixStep.mul(matrixStart);
        viewer.zoomToPercent(zoomStart + (zoomDelta * i / totalSteps));
        viewer.translateToXPercent(xTransStart + (xTransDelta*i/totalSteps));
        viewer.translateToYPercent(yTransStart + (yTransDelta*i/totalSteps));
        viewer.setRotation(matrixStep);
        targetTime += frameTimeMillis;
        if (System.currentTimeMillis() < targetTime) {
          viewer.requestRepaintAndWait();
          int sleepTime = (int)(targetTime - System.currentTimeMillis());
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
    } else {
      int sleepTime = (int)(floatSecondsTotal * 1000) - 30;
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }
      }
    }
    viewer.zoomToPercent(zoom);
    viewer.translateToXPercent(xTrans);
    viewer.translateToYPercent(yTrans);
    viewer.setRotation(matrixEnd);
  }

  void console() {
    viewer.showConsole(statement[1].tok == Token.on);
  }

  void pmesh() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_PMESH);
    viewer.setShapeProperty(JmolConstants.SHAPE_PMESH, "meshID", null);
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (statement[i].tok) {
      case Token.identifier:
        propertyName = "meshID";
        propertyValue = statement[i].value;
        break;
      case Token.string:
        String filename = (String)statement[i].value;
        Object t =
          viewer.getUnzippedBufferedReaderOrErrorMessageFromName(filename);
        if (t instanceof String)
          fileNotFoundException(filename + ":" + t);
        propertyName = "bufferedreader";
        propertyValue = t;
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
      case Token.on:
      case Token.off:
        propertyName = (String)statement[i].value;
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      default:
        invalidArgument();
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_PMESH,
                              propertyName, propertyValue);
    }
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
    viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "init", null);
    String setPropertyName = "potentialCenterSet";
    String decimalPropertyName = "radius_";
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      Token token = statement[i];
      switch (token.tok) {
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
      case Token.identifier:
        String str = (String)token.value;
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
        if (! needsGenerating)
          insufficientArguments();
        if ("to".equalsIgnoreCase(str)) {
          if (nAtomSets > 1)
            invalidParameterOrder();
          setPropertyName = "potentialVertexSet";
          continue;
        }
        if ("centerAngleMax".equalsIgnoreCase(str)) {
          decimalPropertyName = "centerAngleMax";
          isDesignParameter = true;
          continue;
        }
        if ("faceNormalMax".equalsIgnoreCase(str)) {
          decimalPropertyName = "faceNormalMax";
          isDesignParameter = true;
          continue;
        }
        if ("faceCenterOffset".equalsIgnoreCase(str)) {
          decimalPropertyName = "faceCenterOffset";
          isDesignParameter = true;
          continue;
        }
        unrecognizedSubcommand();
      case Token.integer:
        if (nAtomSets > 0 && ! isDesignParameter)
          invalidParameterOrder();
        // no reason not to allow integers when explicit
        if (decimalPropertyName == "radius_") {
          propertyName = "vertexCount";
          propertyValue = new Integer(token.intValue);
          needsGenerating = true;
          break;
        }
      case Token.decimal:
        if (nAtomSets > 0 && ! isDesignParameter)
          invalidParameterOrder();
        propertyName = (decimalPropertyName == "radius_" ? "radius" :
          decimalPropertyName);
        propertyValue = new Float(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case Token.delete:
      case Token.on:
      case Token.off:
        if (++i != statementLength || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && setPropertyName == "potentialVertexSet")
          incompatibleArguments();
        propertyName = (String)token.value;
        onOffDelete = true;
        break;
      case Token.edges:
      case Token.noedges:
      case Token.frontedges:
        if (edgeParameterSeen)
          incompatibleArguments();
        propertyName = (String)token.value;
        edgeParameterSeen = true;
        break;
      case Token.expressionBegin:
        if (typeSeen)
          invalidParameterOrder();
        if (++nAtomSets > 2)
          badArgumentCount();
        if (setPropertyName == "potentialVertexSet")
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "potentialVertexSet";
        propertyValue = expression(statement, ++i);
        i = pcLastExpressionInstruction; // the for loop will increment i
        break;
      default:
        invalidArgument();
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return;
    }
    if (! needsGenerating && ! typeSeen && ! edgeParameterSeen)
      insufficientArguments();
    if (needsGenerating)
      viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "generate", null);
  }

  void sasurface() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_SASURFACE);
    viewer.setShapeProperty(JmolConstants.SHAPE_SASURFACE, "surfaceID", null);
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (statement[i].tok) {
      case Token.identifier:
        propertyValue = statement[i].value;
        // fall into
      case Token.all:
        propertyName = "surfaceID";
        break;
      case Token.on:
      case Token.off:
        propertyName = (String)statement[i].value;
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      default:
        invalidArgument();
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_SASURFACE,
                              propertyName, propertyValue);
    }
  }

  void centerAt() throws ScriptException {
    if (statementLength != 2 && statementLength != 5)
      badArgumentCount();
    String relativeTo = null;
    switch (statement[1].tok) {
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
      unrecognizedSubcommand();
    }
    float x, y, z;
    if (statementLength == 2) {
      x = y = z = 0;
    } else {
      x = floatParameter(2);
      y = floatParameter(3);
      z = floatParameter(4);
    }
    viewer.setCenter(relativeTo, x, y, z);
  }

  void isosurface() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_ISOSURFACE);
    viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "meshID", null);
    boolean colorSeen = false;
    int colorRangeStage = 0;
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (statement[i].tok) {
      case Token.identifier:
        propertyValue = statement[i].value;
        // fall into
      case Token.all:
        propertyName = "meshID";
        break;
      case Token.string:
        String filename = (String)statement[i].value;
        Object t =
          viewer.getUnzippedBufferedReaderOrErrorMessageFromName(filename);
        if (t instanceof String)
          fileNotFoundException(filename + ":" + t);
        propertyName = colorSeen ? "colorreader" : "bufferedreader";
        propertyValue = t;
        if (colorSeen && colorRangeStage != 0 && colorRangeStage != 3)
          invalidArgument();
        break;
      case Token.decimal:
        if (colorRangeStage == 0) {
          propertyName = "cutoff";
          propertyValue = statement[i].value;
          break;
        }
        // fall into
      case Token.integer:
        if (colorRangeStage == 0 || colorRangeStage >= 3)
          invalidArgument();
        propertyName = colorRangeStage == 1 ? "rangeMin" : "rangeMax";
        propertyValue = new Float(floatParameter(i));
        ++colorRangeStage;
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
      case Token.on:
      case Token.off:
        propertyName = (String)statement[i].value;
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      case Token.color:
        colorSeen = true;
        propertyName = "removeRange";
        break;
      case Token.absolute:
        colorRangeStage = 1;
        break;
      default:
        invalidArgument();
      }
      if (propertyName != null)
        viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
                                propertyName, propertyValue);
    }
  }

  void stereo() throws ScriptException {
    int stereoMode = JmolConstants.STEREO_DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    float degrees = -5;
    boolean degreesSeen = false;
    for (int i = 1; i < statementLength; ++i) {
      switch (statement[i].tok) {
      case Token.on:
        stereoMode = JmolConstants.STEREO_DOUBLE;
        break;
      case Token.off:
        stereoMode = JmolConstants.STEREO_NONE;
        break;
      case Token.integer:
      case Token.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        break;
      case Token.identifier:
        String id = (String)statement[i].value;
        if (! degreesSeen)
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
        booleanOrNumberExpected();
      }
    }
    viewer.setStereoDegrees(degrees);
    viewer.setStereoMode(stereoMode);
  }

  void connect() throws ScriptException {
    boolean haveType = false;
    int nAtomSets = 0;
    int nDistances = 0;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] 
     *             [<=1 bond type] [<=1 operation]
     * 
     */
    
    viewer.setShapeProperty(JmolConstants.SHAPE_STICKS,
                            "resetConnectParameters", null);
    if (statementLength == 1) {
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS,
                              "rasmolCompatibleConnect", null);
      return;
    }
    for (int i = 1; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch_tag:
      switch (statement[i].tok) {
      case Token.on:
      case Token.off:
        if (statementLength != 2) 
          badArgumentCount();
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS,
                                "rasmolCompatibleConnect", null);
        return;
      case Token.integer:
      case Token.decimal:
        if (++nDistances > 2)
          badArgumentCount();
        if (nAtomSets > 0 || haveType)
          invalidParameterOrder();
        propertyName = "connectDistance";
        propertyValue = new Float(floatParameter(i));
        break;
      case Token.expressionBegin:
        if (++nAtomSets > 2)
          badArgumentCount();
        if (haveType)
          invalidParameterOrder();
        propertyName = "connectSet";
        propertyValue = expression(statement, i);
        i = pcLastExpressionInstruction; // the for loop will increment i
        break;
      case Token.identifier:
      case Token.hbond:
        String cmd = (String)statement[i].value;
        for (int j =  JmolConstants.bondOrderNames.length; --j >= 0; ) {
          if (cmd.equalsIgnoreCase(JmolConstants.bondOrderNames[j])) {
            if (haveType)
              incompatibleArguments();
            cmd = JmolConstants.bondOrderNames[j];
            propertyName = "connectBondOrder";
            propertyValue = cmd;
            haveType = true;
            break switch_tag;
          }
        }
        if (++i != statementLength)
          invalidParameterOrder();
        if ("modify".equalsIgnoreCase(cmd))
          propertyValue = "modify";
        else if ("create".equalsIgnoreCase(cmd))
          propertyValue = "create";
        else if ("modifyOrCreate".equalsIgnoreCase(cmd))
          propertyValue = "modifyOrCreate";
        else if ("auto".equalsIgnoreCase(cmd))
          propertyValue = "auto";
        else
          unrecognizedSubcommand();
        propertyName = "connectOperation";
        break;
      case Token.none:
      case Token.delete:
        if (++i != statementLength)
          invalidParameterOrder();
        propertyName = "connectOperation";
        propertyValue = "delete";
        break;
      default:
        invalidArgument();
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS,
                              propertyName, propertyValue);
    }
    viewer.setShapeProperty(JmolConstants.SHAPE_STICKS,
                            "applyConnectParameters", null);
  }

  void selectionListeners() {
    viewer.enableSelectionListeners(statement[1].tok == Token.on);
  }

 void selectWithinDialog() {
   
   String cmd = SelectWithinDialog.showDialog(viewer.getAwtComponent());
   if (cmd != null)
   {
       viewer.scriptStatus("SelectWithinDialog command: "+cmd);
       viewer.evalString(cmd);
   }
}
}
