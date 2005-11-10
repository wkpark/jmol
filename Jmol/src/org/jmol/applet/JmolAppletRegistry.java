/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.applet;
import org.jmol.api.JmolAppletInterface;

import java.applet.*;
import java.util.Hashtable;
import java.util.Enumeration;

import netscape.javascript.JSObject;

public class JmolAppletRegistry {

  String name;
  boolean mayScript;
  Applet applet;
  AppletContext appletContext;
  String strJavaVendor, strJavaVersion, strOSName;

  public JmolAppletRegistry(String name, boolean mayScript, Applet applet) {
    if (name == null || name.length() == 0)
      name = null;
    this.name = name;
    this.mayScript = mayScript;
    this.applet = applet;
    this.appletContext = applet.getAppletContext();
    strJavaVendor = System.getProperty("java.vendor");
    strJavaVersion = System.getProperty("java.version");
    strOSName = System.getProperty("os.name");
    /*
    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(applet);
        System.out.println("JmolAppletRegistry: jsoWindow=" + jsoWindow);
      } catch (Exception e) {
        System.out.println("exception trying to get jsoWindow");
      }
    }
    */
    checkIn(name, applet);
  }

  public Enumeration applets() {
    return htRegistry.elements();
  }

  private static Hashtable htRegistry = new Hashtable();

  void checkIn(String name, Applet applet) {
    System.out.println("AppletRegistry.checkIn(" + name + ")");
    if (name != null)
      htRegistry.put(name, applet);
  }

  JSObject getJsoWindow() {
    JSObject jsoWindow = null;
    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(applet);
      } catch (Exception e) {
        System.out.println("exception trying to get jsoWindow");
      }
    } else {
      System.out.println("mayScript not specified for:" + name);
    }
    return jsoWindow;
  }

  JSObject getJsoTop() {
    JSObject jsoTop = null;
    JSObject jsoWindow = getJsoWindow();
    if (jsoWindow != null) {
      try {
        jsoTop = (JSObject)jsoWindow.getMember("top");
      } catch (Exception e) {
        System.out.println("exception trying to get window.top");
      }
    }
    return jsoTop;
  }
  
  public void scriptButton(String targetName, String script,
                           String buttonCallback) {
    if (targetName == null || targetName.length() == 0) {
      System.out.println("no targetName specified");
      return;
    }
    if (tryDirect(targetName, script, buttonCallback))
      return;
    /*
    if (tryJavaScript(targetName, script, buttonCallback))
      return;
    */
    System.out.println("unable to find target:" + targetName);
  }

  private boolean tryDirect(String targetName, String script,
                            String buttonCallback) {
    System.out.println("tryDirect trying appletContext");
    Object target = appletContext.getApplet(targetName);
    if (target == null) {
      System.out.println("... trying registry");
      target = htRegistry.get(targetName);
    }
    if (target == null) {
      System.out.println("tryDirect failed to find applet:" + targetName);
      return false;
    }
    if (! (target instanceof JmolAppletInterface)) {
      System.out.println("target " + targetName + " is not a JmolApplet");
      return true;
    }
    JmolAppletInterface targetJmolApplet = (JmolAppletInterface)target;
    targetJmolApplet.scriptButton((buttonCallback == null
                                   ? null : getJsoWindow()),
                                  name, script, buttonCallback);
    return true;
  }

  /*
  private boolean tryJavaScript(String targetName, String script,
                                   String buttonCallback) {
    if (mayScript) {
      JSObject jsoTop = getJsoTop();
      if (jsoTop != null) {
        try {
          jsoTop.eval(functionRunJmolAppletScript);
          jsoTop.call("runJmolAppletScript",
                      new Object[] { targetName, getJsoWindow(), name,
                                     script, buttonCallback });
          return true;
        } catch (Exception e) {
          System.out.println("exception calling JavaScript");
        }
      }
    }
    return false;
  }

  final static String functionRunJmolAppletScript=
    // w = win, n = name, t = target, s = script
    "function runJmolAppletScript(t,w,n,s,b){" +
    " function getApplet(w,t){" +
    "  var a;" +
    "  if(w.document.applets!=undefined){" +
    "   a=w.document.applets[t];" +
    "   if (a!=undefined) return a;" +
    "  }" +
    "  var f=w.frames;" +
    "  if(f!=undefined){" +
    "   for(var i=f.length;--i>=0;){" +
    "     a=getApplet(f[i],t);" +
    "     if(a!=undefined) return a;" +
    "   }" +
    "  }" +
    "  return undefined;" +
    " }" +
    " var a=getApplet(w.top,t);" +
    " if (a==undefined){" +
    "  alert('cannot find JmolApplet:' + t);" +
    "  return;" +
    " }" +
    " a.scriptButton(w,n,s,b);" +
    "}\n";
  */
}
