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

public class Eval {

  Token[][] aatoken;
  int iStatement;
  Token[] statement;
  DisplayControl control;
  String filename;
  final static int scriptLevelMax = 10;
  int scriptLevel;

  public Eval(DisplayControl control) {
    this.control = control;
  }

  void load(String filename, String script) throws ScriptException {
    this.filename = filename;
    iStatement = 0;
    aatoken = Token.tokenize(script);
  }

  void executeString(String script) throws ScriptException {
    load(null, script);
    run();
  }

  public void executeFile(String filename) throws ScriptException {
    executeFile(filename, 0);
  }

  void executeFile(String filename, int scriptLevel) throws ScriptException {
    this.scriptLevel = scriptLevel;
    if (scriptLevel == scriptLevelMax)
      throw new ScriptException("too many script levels:" + filename);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new ScriptException("file not found:" + filename);
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
      throw new ScriptException("io error reading file:" + filename);
    }
    System.out.println("script:" + script + ":");
    load(filename, script);
    run();
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

  void run() throws ScriptException {
    System.out.println("running!");
    System.out.println(toString());
    while (iStatement < aatoken.length) {
      statement = aatoken[iStatement++];
      Token token = statement[0];
      switch (token.tok) {
      case Token.background:
        background();
        break;
      case Token.define:
        define();
        break;
      case Token.echo:
        echo();
        break;
      case Token.load:
        load();
        break;
      case Token.refresh:
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
      case Token.backbone:
      case Token.bond:
      case Token.cartoon:
      case Token.center:
      case Token.clipboard:
      case Token.color:
      case Token.connect:
      case Token.cpk:
      case Token.dots:
      case Token.exit:
      case Token.hbonds:
      case Token.help:
      case Token.label:
      case Token.molecule:
      case Token.monitor:
      case Token.pause:
      case Token.print:
      case Token.quit:
      case Token.renumber:
      case Token.reset:
      case Token.restrict:
      case Token.ribbons:
      case Token.save:
      case Token.set:
      case Token.show:
      case Token.slab:
      case Token.source:
      case Token.spacefill:
      case Token.ssbonds:
      case Token.star:
      case Token.stereo:
      case Token.strands:
      case Token.structure:
      case Token.trace:
      case Token.translate:
      case Token.unbond:
      case Token.wireframe:
      case Token.write:
      case Token.zap:
      case Token.zoom:
        System.out.println("Script command not implemented:" + token.value);
        break;
      default:
        System.out.println("Eval error - not a command " + token.value);
        return;
      }
    }
    System.out.println("done!");
  }

  void FilenameExpected() throws ScriptException {
    throw new ScriptException("filename expected");
  }

  void IntegerExpected() throws ScriptException {
    throw new ScriptException("integer expected");
  }

  void AxisExpected() throws ScriptException {
    throw new ScriptException("x y z axis expected");
  }

  void ColorExpected() throws ScriptException {
    throw new ScriptException("color expected");
  }

  void UnrecognizedExpression() throws ScriptException {
    throw new ScriptException("runtime unrecognized expression");
  }

  void UndefinedVariable() throws ScriptException {
    throw new ScriptException("variable undefined");
  }

  void BadArgumentCount() throws ScriptException {
    throw new ScriptException("bad argument count");
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
      throw new ScriptException("atom expression compiler error" +
                                " - stack over/underflow");
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
      Atom atom = frame.getAtomAt(i);
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

  Color colorsRasmol[] = {
    /* black      */ Color.black,
    /* blue       */ Color.blue,
    /* bluetint   */ new Color( 58 << 16 | 144 << 8 | 255),
    /* brown      */ new Color(175 << 16 | 117 << 8 |  89),
    /* cyan       */ Color.cyan,
    /* gold       */ new Color(255 << 16 | 156 << 8 |   0),
    /* grey       */ Color.gray,
    /* green      */ Color.green,
    /* greenblue  */ new Color(0x002E8B57),
    /* greentint  */ new Color(152 << 16 | 214 << 8 | 179),
    /* hotpink    */ new Color(255 << 16 |   0 << 8 | 101),
    /* magenta    */ Color.magenta,
    /* orange     */ Color.orange,
    /* pink       */ Color.pink,
    /* pinktint   */ new Color(255 << 16 | 171 << 8 | 187),
    /* purple     */ new Color(0xA020F0),
    /* red        */ Color.red,
    /* redorange  */ new Color(0x00FF4500),
    /* seagreen   */ new Color(  0 << 16 | 250 << 8 | 109),
    /* skyblue    */ new Color( 58 << 16 | 144 << 8 | 255),
    /* violet     */ new Color(0x00EE82EE),
    /* white      */ Color.white,
    /* yellow     */ Color.yellow,
    /* yellowtint */ new Color(246 << 16 | 246 << 8 | 117)
  };

  void background() throws ScriptException {
    if (statement.length != 2)
      BadArgumentCount();
    if ((statement[1].tok & Token.colorparam) == 0)
      ColorExpected();
    control.setBackgroundColor(colorsRasmol[statement[1].intValue]);
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
    if (statement.length != 2)
      BadArgumentCount();
    if (statement[1].tok != Token.string)
      FilenameExpected();
    String filename = (String)statement[1].value;
    if (!control.openFile(filename)) {
      // FIXME -- should I throw an exception here? what does rasmol do?
    }
  }

  void refresh() throws ScriptException {
    if (statement.length != 1)
      BadArgumentCount();
    control.refresh();
  }

  void rotate() throws ScriptException {
    if (statement.length != 3)
      BadArgumentCount();
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
    if (statement.length != 2)
      BadArgumentCount();
    if (statement[1].tok != Token.string)
      FilenameExpected();
    String filename = (String)statement[1].value;
    Eval eval = new Eval(control);
    eval.executeFile(filename, scriptLevel+1);
  }

  void select() throws ScriptException {
    if (statement.length == 1) {
      // FIXME -- what is behavior when there are no arguments to select
    } else {
      control.setSelectionSet(expression(statement, 1));
    }
  }
}
