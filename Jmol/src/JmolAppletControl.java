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
import java.awt.TextArea;
import java.awt.ScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Button;
import netscape.javascript.*;
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


  JSObject win;
  String myName;
  AppletContext context;
  String targetName;
  TextArea text;
  ScrollPane scrollPane;

  Button kickButton;
  Button alertButton;
  Button callButton;

  public void init() {
    win = JSObject.getWindow(this);
    myName = getParameter("name");
    setName(myName);
    targetName = getParameter("target");
    context = getAppletContext();
    //    add(new JmolButton(), "center");

    kickButton = new Button("kick" + targetName);
    kickButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          text.append("kickButton pressed\n");
          kickTarget();
        }
      }
                                 );
    add(kickButton);

    alertButton = new Button("alert");
    alertButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          alertJavaScript();
        }
      }
                                 );
    add(alertButton);

    callButton = new Button("call");
    callButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          callJavaScript();
        }
      }
                                 );
    add(callButton);

    scrollPane = new ScrollPane();
    text = new TextArea();
    scrollPane.add(text);
    add(scrollPane);
    validate();
  }

  void kickTarget() {
    Applet target = context.getApplet(targetName);
    if (target == null)
      return;
    if (target instanceof JmolAppletControl) {
      JmolAppletControl controlsTarget = (JmolAppletControl)target;
      controlsTarget.takeThat();
    }
  }

  String message="Hello Universe! ";
  int i = 0;

  public void takeThat() {
    text.append("Ouch!\n");
  }

  void alertJavaScript() {
    text.append("" + win.eval("alert('alert from " + myName + "!')"));
  }

  void callJavaScript() {
    text.append("" + win.call("listener", new String[] { myName }));
  }

  /*
  public void paint(Graphics g) {
    g.drawString(message + i++, 20, 30);
    g.drawString("win=" + win, 20, 45);
    g.drawString("targetName=" + targetName, 20, 60);
    int y = 75;
    for (Enumeration enum = context.getApplets(); enum.hasMoreElements(); ) {
      Applet applet = (Applet)enum.nextElement();
      String appletName = applet.getName();
      Class appletClass = applet.getClass();
      g.drawString("I see " + appletName + ", a " + appletClass,
                   20, y);
      y += 15;
      if (! myName.equals(appletName) &&
          appletClass == JmolAppletControl.class) {
        g.drawString("I will kick him!", 20, y);
        y += 15;
      }
    }
  }
  */

  public void setMessage(String message) {
    this.message = message;
    text.append("I am " + myName + "\n");
  }
}

