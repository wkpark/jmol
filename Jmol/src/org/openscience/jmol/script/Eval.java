/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.Atom;
import org.openscience.jmol.ProteinProp;
import java.io.*;
import java.awt.Color;
import java.util.BitSet;
import java.util.Hashtable;
import javax.vecmath.Point3d;

class Context {
  String filename;
  String script;
  short[] linenumbers;
  short[] lineIndices;
  Token[][] aatoken;
  int pc;
}

public class Eval implements Runnable {

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
  DisplayControl control;
  Thread myThread;
  boolean terminationNotification;
  boolean interruptExecution;

  final static boolean logMessages = false;

  public Eval(DisplayControl control) {
    compiler = new Compiler();
    this.control = control;
    clearDefinitionsAndLoadPredefined();
  }

  public synchronized void start() {
    if (myThread == null) {
      myThread = new Thread(this);
      interruptExecution = false;
      myThread.start();
    }
  }

  public synchronized void haltExecution() {
    if (myThread != null) {
      interruptExecution = true;
      myThread.interrupt();
    }
  }

  synchronized void clearMyThread() {
    myThread = null;
  }

  public boolean isActive() {
    return myThread != null;
  }

  public boolean hasTerminationNotification() {
    return terminationNotification;
  }

  public void resetTerminationNotification() {
    terminationNotification = false;
  }

  public boolean hadRuntimeError() {
    return error;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public int getExecutionWalltime() {
    return (int)(timeEndExecution - timeBeginExecution);
  }

  boolean loadScript(String filename, String script) {
    this.filename = filename;
    this.script = script;
    if (! compiler.compile(filename, script)) {
      error = true;
      errorMessage = compiler.getErrorMessage();
      control.scriptStatus(errorMessage);
      return false;
    }
    pc = 0;
    aatoken = compiler.getAatokenCompiled();
    linenumbers = compiler.getLineNumbers();
    lineIndices = compiler.getLineIndices();
    return true;
  }

  void clearState() {
    for (int i = scriptLevelMax; --i >= 0; )
      stack[i] = null;
    scriptLevel = 0;
    error = false;
    errorMessage = null;
    terminationNotification = false;
    interruptExecution = false;
  }

  public boolean loadScriptString(String script) {
    clearState();
    return loadScript(null, script);
  }

  public boolean loadScriptFile(String filename) {
    clearState();
    return loadScriptFile(filename);
  }

  boolean loadScriptFileInternal(String filename) {
    InputStream istream = control.getInputStreamFromName(filename);
    if (istream == null)
      return false;
    BufferedReader reader = null;
    reader = new BufferedReader(new InputStreamReader(istream));

    String script = "";
    try {
      while (true) {
        String command = reader.readLine();
        if (command == null)
          break;
        script += command + "\n";
      }
    } catch (IOException e) {
      return IOError(filename);
    }
    return loadScript(filename, script);
  }

  boolean LoadError(String msg) {
    error = true;
    errorMessage = msg;
    return false;
  }

  boolean FileNotFound(String filename) {
    return LoadError("file not found:" + filename);
  }

  boolean IOError(String filename) {
    return LoadError("io error reading:" + filename);
  }

  public String toString() {
    String str;
    str = "Eval\n";
    str += " pc:" + pc + "\n";
    str += aatoken.length + " statements\n";
    for (int i = 0; i < aatoken.length; ++i) {
      str += " |";
      Token[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str += " " + atoken[j];
      }
      str += "\n";
    }
    str += "END\n";
    return str;
  }

  public void clearDefinitionsAndLoadPredefined() {
    // FIXME mth -- need to call this when a new file is loaded!
    variables.clear();

    int cPredef = Token.predefinitions.length;
    for (int iPredef = 0; iPredef < cPredef; iPredef++) {
      String script = Token.predefinitions[iPredef];
      if (compiler.compile("#predefinitions", script)) {
        Token [][] aatoken = compiler.getAatokenCompiled();
        if (aatoken.length != 1) {
          control.scriptStatus("predefinition does not have exactly 1 command:"
                             + script);
          continue;
        }
        Token[] statement = aatoken[0];
        if (statement.length > 2) {
          int tok = statement[1].tok;
          if (tok == Token.identifier ||
              (tok & Token.predefinedset) == Token.predefinedset) {
            String variable = (String)statement[1].value;
            variables.put(variable, statement);
          } else {
            control.scriptStatus("invalid variable name:" + script);
          }
        } else {
          control.scriptStatus("bad statement length:" + script);
        }
      } else {
        control.scriptStatus("predefined set compile error:" + script +
                           "\ncompile error:" + compiler.getErrorMessage());
      }
    }
  }

  public void run() {
    // this refresh is here to ensure that the screen has been painted ...
    // since it could be a problem when an applet is loaded with a script
    // ready to run. 
    refresh();
    timeBeginExecution = System.currentTimeMillis();
    control.pushHoldRepaint();
    try {
      instructionDispatchLoop();
    } catch (ScriptException e) {
      error = true;
      errorMessage = "" + e;
      control.scriptStatus(errorMessage);
    }
    timeEndExecution = System.currentTimeMillis();
    if (errorMessage == null && interruptExecution)
      errorMessage = "execution interrupted";
    clearMyThread();
    terminationNotification = true;
    control.popHoldRepaint();
  }

  public void instructionDispatchLoop() throws ScriptException {
    long timeBegin = 0;
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      control.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      control.scriptStatus(toString());
    }
    while (!interruptExecution && pc < aatoken.length) {
      statement = aatoken[pc++];
      Token token = statement[0];
      switch (token.tok) {
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
        // chime extended commands
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
      case Token.spacefill:
        spacefill();
        break;
      case Token.wireframe:
        wireframe();
        break;
      case Token.animate:
        animate();
        break;
        // not implemented
      case Token.backbone:
      case Token.bond:
      case Token.cartoon:
      case Token.clipboard:
      case Token.connect:
      case Token.dots:
      case Token.hbonds:
      case Token.help:
      case Token.molecule:
      case Token.pause:
      case Token.print:
      case Token.renumber:
      case Token.ribbons:
      case Token.save:
      case Token.show:
      case Token.ssbonds:
      case Token.star:
      case Token.stereo:
      case Token.strands:
      case Token.structure:
      case Token.trace:
      case Token.unbond:
      case Token.write:
        // chime extended commands
      case Token.view:
      case Token.spin:
      case Token.list:
      case Token.display3d:
        control.scriptStatus("Script command not implemented:" + token.value);
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

  void evalError(String message) throws ScriptException {
    throw new ScriptException(message, getLine(), filename, getLinenumber());
  }

  void unrecognizedCommand(Token token) throws ScriptException {
    evalError("unrecognized command:" + token.value);
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

  void unrecognizedSetParameter() throws ScriptException {
    evalError("unrecognized SET parameter");
  }

  void unrecognizedSubcommand() throws ScriptException {
    evalError("unrecognized subcommand");
  }

  void setspecialShouldNotBeHere() throws ScriptException {
    evalError("interpreter error - setspecial should not be here");
  }

  void numberOutOfRange() throws ScriptException {
    evalError("number out of range");
  }

  void badAtomNumber() throws ScriptException {
    evalError("bad atom number");
  }

  void errorLoadingScript(String msg) throws ScriptException {
    evalError("error loading script -> " + msg);
  }

  void notImplemented(int itoken) {
    notImplemented(statement[itoken]);
  }

  void notImplemented(Token token) {
    control.scriptStatus("" + token.value +
                       " not implemented in command:" + statement[0].value);
  }

  // gets a boolean value from the 2nd parameter to the command
  // as in set foo <boolean>

  boolean getSetBoolean() throws ScriptException {
    if (statement.length != 3)
      badArgumentCount();
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

  BitSet copyBitSet(BitSet bitSet) {
    BitSet copy = new BitSet();
    copy.or(bitSet);
    return copy;
  }

  BitSet expression(Token[] code, int pcStart) throws ScriptException {
    int numberOfAtoms = control.numberOfAtoms();
    BitSet bs;
    BitSet[] stack = new BitSet[10];
    int sp = 0;
    if (logMessages)
      control.scriptStatus("start to evaluate expression");
    for (int pc = pcStart; pc < code.length; ++pc) {
      Token instruction = code[pc];
      if (logMessages)
        control.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.all:
        bs = stack[sp++] = new BitSet(numberOfAtoms);
        for (int i = 0; i < numberOfAtoms; ++i)
          bs.set(i);
        break;
      case Token.none:
        stack[sp++] = new BitSet();
        break;
      case Token.integer:
        stack[sp++] = getResidueSet(instruction.intValue);
        break;
      case Token.hyphen:
        int min = instruction.intValue;
        int last = ((Integer)instruction.value).intValue();
        stack[sp++] = getResidueSet(min, last);
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
      case Token.selected:
        stack[sp++] = copyBitSet(control.getSelectionSet());
        break;
      case Token.hetero:
        stack[sp++] = getHeteroSet();
        break;
      case Token.hydrogen:
        stack[sp++] = getHydrogenSet();
        break;
      case Token.spec_name:
        stack[sp++] = getSpecName((String)instruction.value);
        break;
      case Token.spec_number:
        stack[sp++] = getSpecNumber(instruction.intValue);
        break;
      case Token.spec_chain:
        stack[sp++] = getSpecChain((char)instruction.intValue);
        break;
      case Token.spec_atom:
        stack[sp++] = getSpecAtom((String)instruction.value);
        break;
      case Token.y:
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.identifier:
        String variable = (String)instruction.value;
        BitSet value = lookupValue(variable, false);
        if (value == null)
          undefinedVariable();
        stack[sp++] = copyBitSet(value);
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

  void notSet(BitSet bs) {
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      if (bs.get(i))
        bs.clear(i);
      else
        bs.set(i);
    }
  }

  BitSet getHeteroSet() {
    ChemFrame frame = control.getFrame();
    BitSet bsHetero = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop != null && pprop.isHetero())
        bsHetero.set(i);
    }
    return bsHetero;
  }

  BitSet getHydrogenSet() {
    if (logMessages)
      control.scriptStatus("getHydrogenSet()");
    ChemFrame frame = control.getFrame();
    BitSet bsHydrogen = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      Atom atom = frame.getJmolAtomAt(i);
      if (atom.getAtomicNumber() == 1)
        bsHydrogen.set(i);
    }
    return bsHydrogen;
  }

  BitSet getResidueSet(String strResidue) {
    ChemFrame frame = control.getFrame();
    BitSet bsResidue = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop != null && pprop.isResidue(strResidue))
        bsResidue.set(i);
    }
    return bsResidue;
  }

  BitSet getResidueSet(int resno) {
    ChemFrame frame = control.getFrame();
    BitSet bsResidue = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop != null && pprop.getResno() == resno)
        bsResidue.set(i);
    }
    return bsResidue;
  }

  BitSet getResidueSet(int resnoMin, int resnoLast) {
    ChemFrame frame = control.getFrame();
    BitSet bsResidue = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      int atomResno = pprop.getResno();
      if (atomResno >= resnoMin && atomResno <= resnoLast)
        bsResidue.set(i);
    }
    return bsResidue;
  }

  BitSet getSpecName(String resNameSpec) {
    BitSet bsRes = new BitSet();
    if (resNameSpec.length() != 3) {
      control.scriptStatus("residue name spec != 3 :" + resNameSpec);
      return bsRes;
    }
    ChemFrame frame = control.getFrame();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      if (pprop.isResidueNameMatch(resNameSpec))
        bsRes.set(i);
    }
    return bsRes;
  }

  BitSet getSpecNumber(int number) {
    ChemFrame frame = control.getFrame();
    BitSet bsResno = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      if (number == pprop.getResno())
        bsResno.set(i);
    }
    return bsResno;
  }

  BitSet getSpecChain(char chain) {
    chain = Character.toUpperCase(chain);
    ChemFrame frame = control.getFrame();
    BitSet bsChain = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      if (chain == pprop.getChain())
        bsChain.set(i);
    }
    return bsChain;
  }

  BitSet getSpecAtom(String atomSpec) {
    atomSpec = atomSpec.toUpperCase();
    ChemFrame frame = control.getFrame();
    BitSet bsAtom = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      if (pprop.isAtomNameMatch(atomSpec))
        bsAtom.set(i);
    }
    return bsAtom;
  }

  BitSet getResidueWildcard(String strWildcard) {
    ChemFrame frame = control.getFrame();
    BitSet bsResidue = new BitSet();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      ProteinProp pprop = frame.getJmolAtomAt(i).getProteinProp();
      if (pprop == null)
        continue;
      if (pprop.isResidueNameMatch(strWildcard))
        bsResidue.set(i);
    }
    return bsResidue;
  }

  BitSet lookupValue(String variable, boolean plurals) throws ScriptException {
    if (logMessages)
      control.scriptStatus("lookupValue(" + variable + ")");
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

  void comparatorInstruction(Token instruction, BitSet bs)
    throws ScriptException {
    int comparator = instruction.tok;
    int property = instruction.intValue;
    int propertyValue = 0;
    int comparisonValue = ((Integer)instruction.value).intValue();
    int numberOfAtoms = control.numberOfAtoms();
    ChemFrame frame = control.getFrame();
    for (int i = 0; i < numberOfAtoms; ++i) {
      Atom atom = frame.getJmolAtomAt(i);
      switch (property) {
      case Token.atomno:
        propertyValue = i + 1; // in the user world the atoms start with 1
        break;
      case Token.elemno:
        propertyValue = atom.getAtomicNumber();
        break;
      case Token.temperature:
        propertyValue = getTemperature(atom);
        if (propertyValue == -1)
          return;
        break;
      case Token.resno:
        propertyValue = getResno(atom);
        if (propertyValue == -1)
          continue;
        break;
      case Token._resid:
       propertyValue = getResID(atom);
        if (propertyValue == -1)
          continue;
        break;
      case Token._atomid:
       propertyValue = getAtomID(atom);
        if (propertyValue == -1)
          continue;
        break;
      case Token.radius:
        propertyValue = atom.getAtomShape().getRasMolRadius();
        break;
      case Token._bondedcount:
        propertyValue = atom.getBondedCount();
        break;
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

  void withinInstruction(Token instruction, BitSet bs, BitSet bsResult) {
    double distance = ((Double)instruction.value).doubleValue();
    double distanceSquared = distance*distance;
    Atom[] atoms = control.getFrame().getJmolAtoms();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      if (bs.get(i)) {
        // the atom itself is in the set
        bsResult.set(i);
        continue;
      }
      if (isWithin(distanceSquared, atoms[i].getPoint3D(), bs))
        bsResult.set(i);
    }
  }

  boolean isWithin(double distanceSquared, Point3d point, BitSet bs) {
    Atom[] atoms = control.getFrame().getJmolAtoms();
    for (int i = control.numberOfAtoms(); --i >= 0; ) {
      if (! bs.get(i))
        continue;
      Point3d pointB = atoms[i].getPoint3D();
      double d = point.x - pointB.x;
      double d2 = d*d;
      if (d2 > distanceSquared) continue;
      d = point.y - pointB.y;
      d2 += d*d;
      if (d2 > distanceSquared) continue;
      d = point.z - pointB.z;
      d2 += d*d;
      if (d2 <= distanceSquared)
        return true;
    }
    return false;
  }

  int getResno(Atom atom) {
    ProteinProp pprop = atom.getProteinProp();
    return (pprop == null) ? -1 : pprop.getResno();
  }

  int getTemperature(Atom atom) {
    ProteinProp pprop = atom.getProteinProp();
    return (pprop == null) ? -1 : pprop.getTemperature();
  }

  int getResID(Atom atom) {
    ProteinProp pprop = atom.getProteinProp();
    return (pprop == null) ? -1 : pprop.getResID();
  }

  int getAtomID(Atom atom) {
    ProteinProp pprop = atom.getProteinProp();
    return (pprop == null) ? -1 : pprop.getAtomID();
  }


  Color getColorParam(int itoken) throws ScriptException {
    if (itoken >= statement.length)
      colorExpected();
    if (statement[itoken].tok != Token.colorRGB)
      colorExpected();
    return new Color(statement[itoken].intValue);
  }

  Color getColorOrNoneParam(int itoken) throws ScriptException {
    if (itoken >= statement.length)
      colorExpected();
    if (statement[itoken].tok == Token.colorRGB)
      return new Color(statement[itoken].intValue);
    if (statement[itoken].tok != Token.none)
      colorExpected();
    return null;
  }

  void background() throws ScriptException {
    control.setColorBackground(getColorParam(1));
  }

  // mth - 2003 01
  // the doc for RasMol says that they use the center of gravity
  // this is currently only using the geometric center
  // but someplace in the rasmol doc it makes reference to the geometric
  // center as the default for rotations. who knows. 
  void center() throws ScriptException {
    if (statement.length == 1) {
      control.clearSelection();
    } else {
      control.setSelectionSet(expression(statement, 1));
    }
    control.setCenterAsSelected();
  }

  void color() throws ScriptException {
    if (statement.length > 3 || statement.length < 2)
      badArgumentCount();
    switch (statement[1].tok) {
    case Token.colorRGB:
    case Token.spacefill:
    case Token.amino:
    case Token.chain:
    case Token.group:
    case Token.shapely:
    case Token.structure:
    case Token.temperature:
    case Token.charge:
    case Token.user:
      colorAtom(1);
      break;
    case Token.atom:
      colorAtom(2);
      break;
    case Token.bond:
    case Token.bonds:
      control.setColorBondScript(getColorOrNoneParam(2));
      break;
    case Token.label:
      control.setColorLabel(getColorOrNoneParam(2));
      break;
    case Token.backbone:
    case Token.ribbons:
    case Token.dots:
    case Token.hbonds:
    case Token.ssbonds:
      notImplemented(1);
      break;
    default:
      invalidArgument();
    }
  }

  void colorAtom(int itoken) throws ScriptException {
    byte mode = DisplayControl.ATOMTYPE;
    Color color = null;
    switch (statement[itoken].tok) {
    case Token.spacefill:
      break;
    case Token.charge:
      mode = DisplayControl.ATOMCHARGE;
      break;
    case Token.amino:
    case Token.chain:
    case Token.group:
    case Token.shapely:
    case Token.structure:
    case Token.temperature:
    case Token.user:
      notImplemented(itoken);
      return;
    case Token.colorRGB:
      mode = DisplayControl.COLOR;
      color = getColorParam(itoken);
      break;
    default:
        invalidArgument();
    }
    control.setColorAtomScript(mode, color);
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

  void echo() throws ScriptException {
    if (statement.length == 2 && statement[1].tok == Token.string)
      control.scriptEcho((String)statement[1].value);
    else
      control.scriptEcho("");
  }

  void label() throws ScriptException {
    String strLabel = (String)statement[1].value;
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "<default>";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    control.setLabelScript(strLabel);
  }

  void load() throws ScriptException {
    int i = 1;
    // ignore optional file format
    if (statement[i].tok == Token.identifier)
      ++i;
    if (statement[i].tok != Token.string)
      filenameExpected();
    if (statement.length != i + 1)
      badArgumentCount();
    String filename = (String)statement[i].value;
    String errMsg = control.openFile(filename);
    if (errMsg != null)
      evalError(errMsg);
    if (logMessages)
      control.scriptStatus("Successfully loaded:" + filename);
  }

  void monitor() throws ScriptException {
    if (statement.length == 1) {
      control.setShowMeasurements(true);
      return;
    }
    if (statement.length == 2) {
      if (statement[1].tok == Token.on)
        control.setShowMeasurements(true);
      else if (statement[1].tok == Token.off)
        control.setShowMeasurements(false);
      else
        booleanExpected();
      return;
    }
    if (statement.length < 3 || statement.length > 5)
      badArgumentCount();
    for (int i = 1; i < statement.length; ++i) {
      if (statement[i].tok != Token.integer)
        integerExpected();
    }
    int argCount = statement.length - 1;
    int numAtoms = control.numberOfAtoms();
    int[] args = new int[argCount];
    for (int i = 0; i < argCount; ++i) {
      Token token = statement[i + 1];
      if (token.tok != Token.integer)
        integerExpected();
      int atomIndex = token.intValue - 1; // atoms start at 1
      if (atomIndex < 0 || atomIndex >= numAtoms)
        badAtomNumber();
      args[i] = atomIndex;
    }
    control.defineMeasure(args);
  }

  void refresh() {
    control.requestRepaintAndWait();
  }

  void reset() {
    control.homePosition();
  }

  void restrict() throws ScriptException {
    select();
    control.invertSelection();
    boolean bondmode = control.getBondSelectionModeOr();
    control.setBondSelectionModeOr(true);
    control.setStyleBondScript(DisplayControl.NONE);
    control.setBondSelectionModeOr(bondmode);
    control.setStyleAtomScript(DisplayControl.NONE);
    control.setLabelScript(null);
    // also need to turn off backbones, ribbons, strands, cartoons
    control.invertSelection();
  }

  void rotate() throws ScriptException {
    if (statement[2].tok != Token.integer)
      integerExpected();
    int degrees = statement[2].intValue;
    switch (statement[1].tok) {
    case Token.x:
      control.rotateByX(degrees);
      break;
    case Token.y:
      control.rotateByY(degrees);
      break;
    case Token.z:
      control.rotateByZ(degrees);
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
    if (statement[1].tok != Token.string)
      filenameExpected();
    pushContext();
    String filename = (String)statement[1].value;
    if (! loadScriptFileInternal(filename))
      errorLoadingScript(errorMessage);
    instructionDispatchLoop();
    popContext();
  }

  void select() throws ScriptException {
    // NOTE this is called by restrict()
    if (statement.length == 1) {
      control.selectAll();
      if (!control.getRasmolHydrogenSetting())
        control.excludeSelectionSet(getHydrogenSet());
      if (!control.getRasmolHeteroSetting())
        control.excludeSelectionSet(getHeteroSet());
    } else {
      control.setSelectionSet(expression(statement, 1));
    }
  }

  void translate() throws ScriptException {
    if (statement[2].tok != Token.integer)
      integerExpected();
    int percent = statement[2].intValue;
    if (percent > 100 || percent < -100)
      numberOutOfRange();
    switch (statement[1].tok) {
    case Token.x:
      control.translateToXPercent(percent);
      break;
    case Token.y:
      control.translateToYPercent(percent);
      break;
    case Token.z:
      control.translateToZPercent(percent);
      break;
    default:
      axisExpected();
    }
  }

  void zap() {
    control.clear();
  }

  void zoom() throws ScriptException {
    if (statement[1].tok == Token.integer) {
      int percent = statement[1].intValue;
      if (percent < 10 || percent > 500)
        numberOutOfRange();
      control.zoomToPercent(percent);
      return;
    }
    switch (statement[1].tok) {
    case Token.on:
      control.setZoomEnabled(true);
      break;
    case Token.off:
      control.setZoomEnabled(false);
      break;
    default:
      booleanOrPercentExpected();
    }
  }

  void delay() throws ScriptException {
    long timeBegin = System.currentTimeMillis();
    long millis = 0;
    Token token = statement[1];
    switch (token.tok) {
    case Token.integer:
    case Token.on: // this is auto-provided as a default
      millis = token.intValue * 1000;
      break;
    case Token.decimal:
      millis = (long)(((Double)token.value).doubleValue() * 1000);
      break;
    default:
      numberExpected(); 
    }
    control.requestRepaintAndWait();
    millis -= System.currentTimeMillis() - timeBegin;
    if (millis > 0)
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  void move() throws ScriptException {
    if (statement.length < 10 || statement.length > 12)
      badArgumentCount();
    for (int i = 1; i < statement.length; ++i)
      if (statement[i].tok != Token.integer)
        integerExpected();
    int dRotX = statement[1].intValue;
    int dRotY = statement[2].intValue;
    int dRotZ = statement[3].intValue;
    int dZoom = statement[4].intValue;
    int dTransX = statement[5].intValue;
    int dTransY = statement[6].intValue;
    int dTransZ = statement[7].intValue;
    int dSlab = statement[8].intValue;
    int secondsTotal = statement[9].intValue;
    int fps = 30, maxAccel = 5;
    if (statement.length > 10) {
      fps = statement[10].intValue;
      if (statement.length > 11)
        maxAccel = statement[11].intValue;
    }

    int zoom = control.getZoomPercentSetting();
    int slab = control.getSlabPercentSetting();
    int transX = control.getTranslationXPercent();
    int transY = control.getTranslationYPercent();
    int transZ = control.getTranslationZPercent();

    long timeBegin = System.currentTimeMillis();
    int timePerStep = 1000 / fps;
    int totalSteps = fps * secondsTotal;
    double radiansPerDegreePerStep = Math.PI / 180 / totalSteps;
    double radiansXStep = radiansPerDegreePerStep * dRotX;
    double radiansYStep = radiansPerDegreePerStep * dRotY;
    double radiansZStep = radiansPerDegreePerStep * dRotZ;
    control.setInMotion(true);
    if (totalSteps == 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    for (int i = 1; i <= totalSteps && !interruptExecution; ++i) {
      if (dRotX != 0)
        control.rotateByX(radiansXStep);
      if (dRotY != 0)
        control.rotateByY(radiansYStep);
      if (dRotZ != 0)
        control.rotateByZ(radiansZStep);
      if (dZoom != 0)
        control.zoomToPercent(zoom + dZoom * i / totalSteps);
      if (dTransX != 0)
        control.translateToXPercent(transX + dTransX * i / totalSteps);
      if (dTransY != 0)
        control.translateToYPercent(transY + dTransY * i / totalSteps);
      if (dTransZ != 0)
        control.translateToZPercent(transZ + dTransZ * i / totalSteps);
      if (dSlab != 0)
        control.slabToPercent(slab + dSlab * i / totalSteps);
      int timeSpent = (int)(System.currentTimeMillis() - timeBegin);
      int timeAllowed = i * timePerStep;
      if (timeSpent < timeAllowed) {
        control.requestRepaintAndWait();
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
    control.setInMotion(false);
  }

  void set() throws ScriptException {
    switch(statement[1].tok) {
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
    case Token.display:
      setDisplay();
      break;
    case Token.fontsize:
      setFontsize();
      break;
    case Token.hetero:
      setHetero();
      break;
    case Token.hydrogen:
      setHydrogen();
      break;
    case Token.monitor:
      setMonitor();
      break;
    case Token.property:
      setProperty();
      break;

      /*
    case Token.spacefill:
      setSpacefill();
      break;
    case Token.bond:
      setBond();
      break;
      */
      // not implemented
    case Token.ambient:
    case Token.backfade:
    case Token.cartoon:
    case Token.hbonds:
    case Token.hourglass:
    case Token.kinemage:
    case Token.menus:
    case Token.mouse:
    case Token.picking:
    case Token.radius:
    case Token.shadow:
    case Token.slabmode:
    case Token.solvent:
    case Token.specular:
    case Token.specpower:
    case Token.ssbonds:
    case Token.strands:
    case Token.transparent:
    case Token.unitcell:
    case Token.vectps:
    case Token.write:
      notImplemented(1);
      break;
    case Token.background:
    case Token.stereo:
      setspecialShouldNotBeHere();
    default:
      unrecognizedSetParameter();
    }
  }

  void slab() throws ScriptException {
    if (statement[1].tok == Token.integer) {
      int percent = statement[1].intValue;
      if (percent < 0 || percent > 100)
        numberOutOfRange();
      control.slabToPercent(percent);
      return;
    }
    switch (statement[1].tok) {
    case Token.on:
      control.setSlabEnabled(true);
      break;
    case Token.off:
      control.setSlabEnabled(false);
      break;
    default:
      booleanOrPercentExpected();
    }
  }

  void spacefill() throws ScriptException {
    int tok = statement[1].tok;
    byte style = DisplayControl.SHADING;
    short mar = -999;
    switch (tok) {
    case Token.on:
      mar = -100; // spacefill with no args goes to 100%
      break;
    case Token.off:
      style = DisplayControl.NONE;
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      if (radiusRasMol >= 500 || radiusRasMol < -100)
        numberOutOfRange();
      mar = (short)radiusRasMol;
      if (radiusRasMol > 0)
        mar *= 4;
      break;
    case Token.decimal:
      double angstroms = ((Double)statement[1].value).doubleValue();
      if (angstroms >= 2)
        numberOutOfRange();
      mar = (short)(angstroms * 1000);
      break;
    case Token.wireframe:
      style = DisplayControl.WIREFRAME;
      break;
    case Token.identifier:
      String id = (String)statement[1].value;
      if (id.equalsIgnoreCase("shaded"))
        break;
      if (id.equalsIgnoreCase("invisible")) {
        style = DisplayControl.INVISIBLE;
        break;
      }
      if (id.equalsIgnoreCase("quickdraw")) {
        style = DisplayControl.QUICKDRAW;
        break;
      }
    default:
      booleanOrNumberExpected();
    }
    if (mar == -999)
      control.setStyleAtomScript(style);
    else
      control.setStyleMarAtomScript(style, mar);
  }

  void wireframe() throws ScriptException {
    int tok = statement[1].tok;
    byte style = DisplayControl.WIREFRAME;
    short mar = -1;
    switch (tok) {
    case Token.on:
      break;
    case Token.off:
      style = DisplayControl.NONE;
      break;
    case Token.integer:
      int radiusRasMol = statement[1].intValue;
      if (radiusRasMol >= 500)
        numberOutOfRange();
      mar = (short)(radiusRasMol * 4);
      style = DisplayControl.SHADING;
      break;
    case Token.decimal:
      double angstroms = ((Double)statement[1].value).doubleValue();
      if (angstroms >= 2)
        numberOutOfRange();
      mar = (short)(angstroms * 1000);
      style = DisplayControl.SHADING;
      break;
    case Token.identifier:
      String id = (String)statement[1].value;
      if (id.equalsIgnoreCase("shaded")) {
        control.setStyleBond(DisplayControl.SHADING);
        return;
      }
      if (id.equalsIgnoreCase("box")) {
        control.setStyleBond(DisplayControl.BOX);
        return;
      }
      if (id.equalsIgnoreCase("quickdraw")) {
        control.setStyleBond(DisplayControl.QUICKDRAW);
        return;
      }
    default:
      booleanOrNumberExpected();
    }
    if (mar == -1)
      control.setStyleBondScript(style);
    else
      control.setStyleMarBondScript(style, mar);
  }

  void animate() throws ScriptException {
    if (statement.length < 2 || statement[1].tok != Token.identifier)
      unrecognizedSubcommand();
    String cmd = (String)statement[1].value;
    if (cmd.equalsIgnoreCase("frame")) {
      if (statement.length != 3 || statement[2].tok != Token.integer)
        integerExpected();
      int frame = statement[2].intValue;
      if (frame < 0 || frame >= control.getNumberOfFrames()) 
       numberOutOfRange();
      control.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("next")) {
      int frame = control.getCurrentFrameNumber() + 1;
      if (frame < control.getNumberOfFrames())
        control.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("prev")) {
      int frame = control.getCurrentFrameNumber() - 1;
      if (frame >= 0)
        control.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("nextwrap")) {
      int frame = control.getCurrentFrameNumber() + 1;
      if (frame >= control.getNumberOfFrames())
        frame = 0;
      control.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("prevwrap")) {
      int frame = control.getCurrentFrameNumber() - 1;
      if (frame < 0)
        frame = control.getNumberOfFrames() - 1;
      control.setFrame(frame);
    } else if (cmd.equalsIgnoreCase("play")) {
      animatePlay(true);
    } else if (cmd.equalsIgnoreCase("revplay")) {
      animatePlay(false);
    } else if (cmd.equalsIgnoreCase("rewind")) {
      control.setFrame(0);
    } else {
      unrecognizedSubcommand();
    }
  }

  void animatePlay(boolean forward) {
    int nframes = control.getNumberOfFrames();
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
    control.setInMotion(true);
    for (int frame = frameBegin; frame != frameEnd; frame += frameDelta) {
      control.setFrame(frame);
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
    control.setInMotion(false);
  }

  /****************************************************************
   * SET implementations
   ****************************************************************/

  void setAxes() throws ScriptException {
    if (statement.length != 3)
      badArgumentCount();
    byte modeAxes = DisplayControl.AXES_NONE;
    switch (statement[2].tok) {
    case Token.on:
      modeAxes = DisplayControl.AXES_BBOX;
    case Token.off:
      break;
    default:
      booleanExpected();
    }
    control.setModeAxes(modeAxes);
  }

  void setBondmode() throws ScriptException {
    if (statement.length != 3)
      badArgumentCount();
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
    control.setBondSelectionModeOr(bondmodeOr);
  }

  void setBonds() throws ScriptException {
    control.setShowMultipleBonds(getSetBoolean());
  }

  void setBoundbox() throws ScriptException {
    control.setShowBoundingBox(getSetBoolean());
  }

  void setDisplay() throws ScriptException {
    control.setSelectionHaloEnabled(getSetBoolean());
  }

  void setFontsize() throws ScriptException {
    int fontsize = 8;
    if (statement.length == 3) {
      if (statement[2].tok != Token.integer)
        integerExpected();
      fontsize = statement[2].intValue;
      if (fontsize > 72)
        numberOutOfRange();
    }
    control.setLabelFontSize(fontsize);
  }

  void setHetero() throws ScriptException {
    control.setRasmolHeteroSetting(getSetBoolean());
  }

  void setHydrogen() throws ScriptException {
    control.setRasmolHydrogenSetting(getSetBoolean());
  }

  void setMonitor() throws ScriptException {
    control.setShowMeasurementLabels(getSetBoolean());
  }

  void setProperty() throws ScriptException {
    if (statement.length != 4)
      badArgumentCount();
    if (statement[2].tok != Token.identifier)
      propertyNameExpected();
    String propertyName = (String)statement[2].value;
    switch (statement[3].tok) {
    case Token.on:
      control.setBooleanProperty(propertyName, true);
      break;
    case Token.off:
      control.setBooleanProperty(propertyName, false);
      break;
    case Token.integer:
    case Token.decimal:
    case Token.string:
      notImplemented(3);
    default:
      unrecognizedSetParameter();
    }
  }

  /*
  void setWireframerotation() throws ScriptException {
    control.setWireframeRotation(getSetBoolean());
  }

  void setPerspectivedepth() throws ScriptException {
    control.setPerspectiveDepth(getSetBoolean());
  }

  void setShowHydrogens() throws ScriptException {
    control.setShowHydrogens(getSetBoolean());
  }

  void setShowVectors() throws ScriptException {
    control.setShowVectors(getSetBoolean());
  }

  void setShowMeasurements() throws ScriptException {
    control.setShowMeasurements(getSetBoolean());
  }

  void setShowSelections() throws ScriptException {
    control.setSelectionHaloEnabled(getSetBoolean());
  }

  */

  /*
  void setSpacefill() throws ScriptException {
    byte style = DisplayControl.SHADING;
    if (statement.length == 3) {
      switch (statement[2].tok) {
      case Token.wireframe:
        style = DisplayControl.WIREFRAME;
        break;
      case Token.identifier:
        String str = (String)statement[2].value;
        if (str.equalsIgnoreCase("shaded"))
          break;
        if (str.equals("quickdraw")) {
          style = DisplayControl.QUICKDRAW;
          break;
        }
        if (str.equals("invisible")) {
          style = DisplayControl.INVISIBLE;
          break;
        }
        if (str.equals("none")) {
          style = DisplayControl.NONE;
          break;
        }
      default:
        unrecognizedStyleParameter();
      }
    }
    control.setStyleAtomScript(DisplayControl.SHADING);
  }

  void setBond() throws ScriptException {
    byte style = DisplayControl.SHADING;
    if (statement.length == 3) {
      switch (statement[2].tok) {
      case Token.wireframe:
        style = DisplayControl.WIREFRAME;
        break;
      case Token.identifier:
        String str = (String)statement[2].value;
        if (str.equalsIgnoreCase("shaded"))
          break;
        if (str.equals("quickdraw")) {
          style = DisplayControl.QUICKDRAW;
          break;
        }
        if (str.equals("box")) {
          style = DisplayControl.BOX;
          break;
        }
        if (str.equals("none")) {
          style = DisplayControl.NONE;
          break;
        }
      default:
        unrecognizedStyleParameter();
      }
    }
    control.setStyleBondScript(DisplayControl.SHADING);
  }
  */
}
