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
import org.openscience.jmol.SelectionSet;
import java.io.*;
import java.awt.Color;

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
      case Token.define:
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
    throw new ScriptException("unrecognized expression");
  }

  void BadArgumentCount() throws ScriptException {
    throw new ScriptException("bad argument count");
  }

  SelectionSet expression(int iStart) throws ScriptException {
    // FIXME -- this is a mega-kludge - need a real expression evaluator
    int lastInt = 0;
    boolean rangeSelection = false;
    SelectionSet set = new SelectionSet();
    for (int i = iStart; i < statement.length; ++i) {
      System.out.println("expression token:" + statement[i]);
      switch (statement[i].tok) {
      case Token.all:
        for (int j = 0, num = control.numberOfAtoms(); j < num; ++j)
          set.addSelection(j);
        break;
      case Token.none:
        set.clearSelection();
        break;
      case Token.integer:
        int thisInt = statement[i].intValue;
        if (rangeSelection) {
          for (int j = lastInt + 1; j <= thisInt - 1; ++j)
            set.addSelection(j);
          rangeSelection = false;
        }
        set.addSelection(thisInt);
        lastInt = thisInt;
        break;
      case Token.opOr:
        break;
      case Token.hyphen:
        rangeSelection = true;
        break;
      default:
        UnrecognizedExpression();
      }
    }
    return set;
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
      control.setSelectionSet(expression(1));
    }
  }
}
