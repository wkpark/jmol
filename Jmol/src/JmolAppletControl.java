/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import java.applet.*;
import org.openscience.jmol.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.openscience.jmol.viewer.managers.ColorManager;

import netscape.javascript.JSObject;

public class JmolAppletControl extends Applet {

  private final static String[][] parameterInfo = {
    { "foo", "bar,baz,biz",
      "the description" },
  };

  public String getAppletInfo() {
    return "JmolAppletControl ... see jmol.sourceforge.net";
  }

  public String[][] getParameterInfo() {
    return parameterInfo;
  }

  private final static int typeChimePush =   0;
  private final static int typeChimeToggle = 1;
  private final static int typeChimeRadio =  2;
  private final static int typeButton =      3;
  private final static int typeCheckbox =    4;

  // put these in lower case
  private final static String[] typeNames =
  {"chimepush", "chimetoggle", "chimeradio", "button", "checkbox"};

  String myName;
  AppletContext context;
  String targetName;
  String typeName;
  int type;
  int width;
  int height;
  Color colorBackground;
  Color colorForeground;
  String script;
  String label;
  String altScript;
  String buttonCallback;
  JSObject jsoWindow;

  String groupName;
  boolean toggleState;

  Button awtButton;
  Checkbox awtCheckbox;
  Component myControl;

  private String getParam(String paramName) {
    String value = getParameter(paramName);
    if (value != null) {
      value = value.trim();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }
  
  private String getParamLowerCase(String paramName) {
    String value = getParameter(paramName);
    if (value != null) {
      value = value.trim().toLowerCase();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }
  
  public void init() {
    context = getAppletContext();
    myName = getParam("name");
    JmolAppletRegistry.checkIn(myName, this);

    targetName = getParam("target");
    typeName = getParamLowerCase("type");
    for (type = typeNames.length;
         --type >= 0 && ! (typeNames[type].equals(typeName)); )
      {}
    groupName = getParamLowerCase("group");
    String buttonState = getParamLowerCase("state");
    toggleState = (buttonState != null &&
                   (buttonState.equals("on") ||
                    buttonState.equals("true") ||
                    buttonState.equals("pushed") ||
                    buttonState.equals("checked") ||
                    buttonState.equals("1")));
    label = getParameter("label"); // don't trim white space from a label
    script = getParam("script");
    altScript = getParam("altScript");
    try {
      width = Integer.parseInt(getParam("width"));
      height = Integer.parseInt(getParam("height"));
    } catch (NumberFormatException e) {
    }
    String colorName;
    colorName = getParam("bgcolor");
    setBackground(colorName == null
                  ? Color.white
                  : ColorManager.getColorFromString(colorName));
    colorName = getParam("fgcolor");
    setForeground(colorName == null
                  ? Color.black
                  : ColorManager.getColorFromString(colorName));
    buttonCallback = getParam("buttoncallback");
    if (buttonCallback != null)
      jsoWindow = JSObject.getWindow(this);

    setLayout(new GridLayout(1, 1));
    allocateControl();
    logWarnings();
  }

  private void logWarnings() {
    if (targetName == null)
      System.out.println(typeName + " with no target?");
    if (type == -1)
      System.out.println("unrecognized control type:" + typeName);
    if (type == typeChimeRadio && groupName == null)
      System.out.println("chimeRadio with no group name?");
    if (script == null)
      System.out.println("control with no script?");
    if (type == typeChimeToggle && altScript == null)
      System.out.println("chimeToggle with no altScript?");
  }

  private void allocateControl() {
    switch (type) {
    case typeChimePush:
      label = "X";
    case typeButton:
      allocateButton();
      break;
    case typeChimeToggle:
      allocateChimeToggle();
      break;
    case typeChimeRadio:
      allocateChimeRadio();
      break;
    case typeCheckbox:
      allocateCheckbox();
      break;
    }
    if (myControl == null)
      myControl = new Button("?");
    add(myControl);
    validate();
  }

  private void allocateButton() {
    awtButton = new Button(label);
    awtButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          runScript();
        }
      }
                                  );
    toggleState = true;
    myControl = awtButton;
  }

  private void allocateChimeToggle() {
    awtButton = new Button(toggleState ? "X" : "");
    awtButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          toggleState = !toggleState;
          awtButton.setLabel(toggleState ? "X" : "");
          runScript();
        }
      }
                                  );
    myControl = awtButton;
  }

  private void allocateChimeRadio() {
    awtButton = new Button(toggleState ? "X" : "");
    awtButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (! toggleState) {
            notifyRadioPeers();
            toggleState = true;
            awtButton.setLabel("X");
            runScript();
          }
        }
      }
                                  );
    myControl = awtButton;
  }

  private void notifyRadio(String radioGroupName) {
    if ((type != typeChimeRadio && type != typeCheckbox) ||
        radioGroupName == null ||
        ! radioGroupName.equals(groupName))
      return;
    if (toggleState) {
      toggleState = false;
      if (type == typeChimeRadio)
        awtButton.setLabel("");
      else
        awtCheckbox.setState(false);
      runScript();
    }
  }

  private void notifyRadioPeers() {
    for (Enumeration enum = JmolAppletRegistry.applets(); enum.hasMoreElements(); ) {
      Object peer = enum.nextElement();
      if (! (peer instanceof JmolAppletControl))
        continue;
      JmolAppletControl controlPeer = (JmolAppletControl)peer;
      controlPeer.notifyRadio(groupName);
    }
  }

  private void allocateCheckbox() {
    awtCheckbox = new Checkbox(label, toggleState);
    awtCheckbox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          if (toggleState != awtCheckbox.getState()) {
            if (! toggleState && groupName != null)
              notifyRadioPeers();
            toggleState = ! toggleState;
            runScript();
          }
        }
      }
                                  );
    myControl = awtCheckbox;
  }

  private void runScript() {
    String scriptToRun = (toggleState ? script : altScript);
    if (scriptToRun == null)
      return;
    if (targetName == null) {
      System.out.println(typeName + " has no target?");
      return;
    }
    Applet targetApplet = JmolAppletRegistry.lookup(targetName);
    if (targetApplet == null) {
      System.out.println("target " + targetName + " not found");
      return;
    }
    if (! (targetApplet instanceof JmolApplet)) {
      System.out.println("target " + targetName + " is not a JmolApplet");
      return;
    }
    JmolApplet targetJmolApplet = (JmolApplet)targetApplet;
    targetJmolApplet.scriptButton(scriptToRun, jsoWindow, buttonCallback, myName);
  }
}

