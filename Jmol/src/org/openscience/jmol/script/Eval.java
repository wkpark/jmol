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

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.Atom;
import java.io.*;
import java.awt.Color;
import java.util.BitSet;
import java.util.Hashtable;

public class Eval implements Runnable {

  Compiler compiler;

  final static int scriptLevelMax = 10;
  int scriptLevel;
  String filename;
  String script;
  short[] linenumbers;
  short[] lineIndices;
  Token[][] aatoken;

  boolean error;
  String errorMessage;

  int pc; // program counter
  Token[] statement;
  DisplayControl control;
  Thread myThread;
  boolean haltExecution;

  public Eval(DisplayControl control) {
    compiler = new Compiler(this);
    this.control = control;
  }

  boolean load(String filename, String script, int scriptLevel) {
    this.filename = filename;
    this.script = script;
    this.scriptLevel = scriptLevel;
    if (scriptLevel == scriptLevelMax)
      return TooManyScriptLevels(filename);
    pc = 0;
    try {
      compiler.compile();
      return true;
    } catch (ScriptException e) {
      error = true;
      errorMessage = "" + e;
      System.out.println(errorMessage);
      return false;
    }
  }

  public boolean loadString(String script) {
    return load(null, script, 0);
  }

  public boolean loadFile(String filename) {
    return loadFile(filename, 0);
  }

  boolean loadFile(String filename, int scriptLevel) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      return FileNotFound(filename);
    }
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
    return load(filename, script, scriptLevel);
  }

  boolean LoadError(String msg) {
    error = true;
    errorMessage = msg;
    return false;
  }

  boolean TooManyScriptLevels(String filename) {
    return LoadError("too many script levels:" + filename);
  }

  boolean FileNotFound(String filename) {
    return LoadError("file not found:" + filename);
  }

  boolean IOError(String filename) {
    return LoadError("io error reading:" + filename);
  }

  public void start() {
    if (myThread == null) {
      haltExecution = false;
      myThread = new Thread(this);
      myThread.start();
    }
  }

  public void haltExecution() {
    if (myThread != null)
      haltExecution = true;
  }

  public String toString() {
    String str;
    str = "Eval\n";
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

  public void run() {
    try {
      // FIXME -- confirm repaint behavior during script execution
      control.setHoldRepaint(true);
      instructionDispatchLoop();
    } catch (ScriptException e) {
      System.out.println("" + e);
    }
    myThread = null;
    control.setHoldRepaint(false);
  }

  public void instructionDispatchLoop() throws ScriptException {
    while (!haltExecution && pc < aatoken.length) {
      statement = aatoken[pc++];
      Token token = statement[0];
      switch (token.tok) {
      case Token.background:
        background();
        break;
      case Token.center:
        center();
        break;
      case Token.define:
        define();
        break;
      case Token.echo:
        echo();
        break;
      case Token.exit:
      case Token.quit: // in rasmol quit actually exits the program
        haltExecution = true;
        break;
      case Token.load:
        load();
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
        // not implemented
      case Token.backbone:
      case Token.bond:
      case Token.cartoon:
      case Token.clipboard:
      case Token.color:
      case Token.connect:
      case Token.dots:
      case Token.hbonds:
      case Token.help:
      case Token.label:
      case Token.molecule:
      case Token.monitor:
      case Token.pause:
      case Token.print:
      case Token.renumber:
      case Token.restrict:
      case Token.ribbons:
      case Token.save:
      case Token.set:
      case Token.show:
      case Token.slab:
      case Token.spacefill:
      case Token.ssbonds:
      case Token.star:
      case Token.stereo:
      case Token.strands:
      case Token.structure:
      case Token.trace:
      case Token.unbond:
      case Token.wireframe:
      case Token.write:
      case Token.zap:
        // chime extended commands
      case Token.view:
      case Token.spin:
      case Token.list:
      case Token.display3d:
        System.out.println("Script command not implemented:" + token.value);
        break;
      default:
        UnrecognizedCommand(token);
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

  void EvalError(String message) throws ScriptException {
    throw new ScriptException(message, getLine(), filename, getLinenumber());
  }

  void UnrecognizedCommand(Token token) throws ScriptException {
    EvalError("unrecognized command:" + token.value);
  }

  void FilenameExpected() throws ScriptException {
    EvalError("filename expected");
  }

  void BooleanExpected() throws ScriptException {
    EvalError("boolean expected");
  }

  void IntegerExpected() throws ScriptException {
    EvalError("integer expected");
  }

  void NumberExpected() throws ScriptException {
    EvalError("number expected");
  }

  void AxisExpected() throws ScriptException {
    EvalError("x y z axis expected");
  }

  void ColorExpected() throws ScriptException {
    EvalError("color expected");
  }

  void UnrecognizedExpression() throws ScriptException {
    EvalError("runtime unrecognized expression");
  }

  void UndefinedVariable() throws ScriptException {
    EvalError("variable undefined");
  }

  void BadArgumentCount() throws ScriptException {
    EvalError("bad argument count");
  }

  void OutOfRange() throws ScriptException {
    EvalError("out of range");
  }

  void ErrorLoadingScript(String msg) throws ScriptException {
    EvalError("error loading script -> " + msg);
  }

  BitSet expression(Token[] code, int pcStart) throws ScriptException {
    int numberOfAtoms = control.numberOfAtoms();
    BitSet bs;
    BitSet[] stack = new BitSet[10];
    int sp = 0;
    for (int pc = pcStart; pc < code.length; ++pc) {
      Token instruction = code[pc];
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
        int thisInt = instruction.intValue;
        bs = new BitSet();
        if (thisInt >= 0 && thisInt < numberOfAtoms)
          bs.set(thisInt);
        stack[sp++] = bs;
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
        for (int i = 0; i < numberOfAtoms; ++i) {
          if (bs.get(i))
            bs.clear(i);
          else
            bs.set(i);
        }
        break;
      case Token.hyphen:
        int min = instruction.intValue;
        int last = ((Integer)instruction.value).intValue();
        int max = last + 1;
        if (max > numberOfAtoms)
          max = numberOfAtoms;
        bs = stack[sp++] = new BitSet(max);
        for (int i = min; i < max; ++i)
          bs.set(i);
        break;
      case Token.identifier:
        String variable = (String)instruction.value;
        Token[] definition = (Token[])variables.get(variable);
        if (definition == null)
          UndefinedVariable();
        stack[sp++] = expression(definition, 2);
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
        UnrecognizedExpression();
      }
    }
    if (sp != 1)
      EvalError("atom expression compiler error - stack over/underflow");
    return stack[0];
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
      Atom atom = (org.openscience.jmol.Atom)frame.getAtomAt(i);
      switch (property) {
      case Token.atomno:
        propertyValue = i;
        break;
      case Token.elemno:
        propertyValue = atom.getAtomicNumber();
        break;
      case Token.temperature:
      case Token.resno:
        // FIXME -- what is resno?
        propertyValue = -1;
        break;
      case Token.radius:
        // FIXME -- confirm that RasMol units are 250 per angstrom
        propertyValue = (int)(atom.getVdwRadius() * 250);
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

  void background() throws ScriptException {
    if ((statement[1].tok & Token.colorparam) == 0)
      ColorExpected();
    control.setColorBackground(new Color(statement[1].intValue));
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

  Hashtable variables = new Hashtable();
  void define() throws ScriptException {
    String variable = (String)statement[1].value;
    variables.put(variable, statement);
  }

  void echo() throws ScriptException {
    if (statement.length == 2 && statement[1].tok == Token.string)
      control.scriptEcho((String)statement[1].value);
    else
      control.scriptEcho("");
  }

  void load() throws ScriptException {
    if (statement[1].tok != Token.string)
      FilenameExpected();
    String filename = (String)statement[1].value;
    if (!control.openFile(filename)) {
      // FIXME -- should I throw an exception here? what does rasmol do?
    }
  }

  void refresh() throws ScriptException {
    control.requestRepaintAndWait();
  }

  void reset() throws ScriptException {
    control.zoomToPercent(100);
    control.setZoomEnabled(true);
    control.rotateFront();
    control.setCenter(null);
    control.translateToXPercent(0);
    control.translateToYPercent(0);
    control.translateToZPercent(0);
  }

  void rotate() throws ScriptException {
    if (statement[2].tok != Token.integer)
      IntegerExpected();
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
      AxisExpected();
    }
  }

  void script() throws ScriptException {
    if (statement[1].tok != Token.string)
      FilenameExpected();
    String filename = (String)statement[1].value;
    Eval eval = new Eval(control);
    if (eval.loadFile(filename, scriptLevel+1))
      eval.run();
    else
      ErrorLoadingScript(eval.errorMessage);
  }

  void select() throws ScriptException {
    if (statement.length == 1) {
      // FIXME -- what is behavior when there are no arguments to select
    } else {
      control.setSelectionSet(expression(statement, 1));
    }
  }

  void translate() throws ScriptException {
    if (statement[2].tok != Token.integer)
      IntegerExpected();
    int percent = statement[2].intValue;
    if (percent > 100 || percent < -100)
      OutOfRange();
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
      AxisExpected();
    }
  }

  void zoom() throws ScriptException {
    if (statement[1].tok == Token.integer) {
      int percent = statement[1].intValue;
      if (percent < 10 || percent > 500)
        OutOfRange();
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
      BooleanExpected();
    }
  }

  void delay() throws ScriptException {
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
      IntegerExpected();
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  void move() throws ScriptException {
    if (statement.length < 10 || statement.length > 12)
      BadArgumentCount();
    for (int i = 1; i < statement.length; ++i)
      if (statement[i].tok != Token.integer)
        IntegerExpected();
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

    int zoom = control.getZoomPercent();
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
    if (totalSteps == 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    for (int i = 1; i <= totalSteps; ++i) {
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
            return;
          }
        }
      }
    }
  }
}
