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
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Button;
import java.util.Enumeration;

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

  // put these in lower case
  private final static String[] typeNames =
  {"chimepush", "chimetoggle", "chimeradio"};

  String myName;
  AppletContext context;
  String targetName;
  String typeName;
  int type;
  int width;
  int height;
  String script;
  String altScript;
  //  JmolApplet jmolApplet;

  String groupName;
  boolean toggleState;

  Button awtButton;
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
    script = getParam("script");
    altScript = getParam("altScript");
    try {
      width = Integer.parseInt(getParam("width"));
      height = Integer.parseInt(getParam("height"));
    } catch (NumberFormatException e) {
    }
    setLayout(new GridLayout(1, 1));
    allocateControl();
  }

  int clickCount;

  private void allocateControl() {
    switch (type) {
    case typeChimePush:
      allocateChimePush();
      break;
    case typeChimeToggle:
      allocateChimeToggle();
      break;
    case typeChimeRadio:
      allocateChimeRadio();
      break;
    }
    if (myControl == null)
      myControl = new Button("?");
    add(myControl);
    validate();
  }

  private void allocateChimePush() {
    awtButton = new Button("X");
    awtButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          context.showStatus("click " + (++clickCount));
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
    if (type != typeChimeRadio ||
        radioGroupName == null ||
        ! radioGroupName.equals(groupName))
      return;
    if (toggleState) {
      toggleState = false;
      awtButton.setLabel("");
      runScript();
    }
  }

  private void notifyRadioPeers() {
    System.out.println("notifyRadioPeers()");
    for (Enumeration enum = context.getApplets(); enum.hasMoreElements(); ) {
      Object peer = enum.nextElement();
      if (! (peer instanceof JmolAppletControl))
        continue;
      JmolAppletControl controlPeer = (JmolAppletControl)peer;
      controlPeer.notifyRadio(groupName);
    }
  }

  private void runScript() {
    String scriptToRun = (toggleState ? script : altScript);
    if (scriptToRun == null)
      return;
    System.out.println("runScript:" + scriptToRun);
  }
}

