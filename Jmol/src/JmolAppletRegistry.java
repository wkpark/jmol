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
import java.applet.Applet;
import java.util.Hashtable;
import java.util.Enumeration;

import netscape.javascript.JSObject;

public class JmolAppletRegistry {

  String name;
  boolean mayScript;
  Applet applet;
  String strJavaVendor, strJavaVersion, strOSName;
  boolean ns4;
  JSObject jsoWindow;
  JSObject jsoTop;

  public JmolAppletRegistry(String name, boolean mayScript, Applet applet) {
    if (name == null || name.length() == 0)
      name = null;
    this.name = name;
    this.mayScript = mayScript;
    this.applet = applet;
    strJavaVendor = System.getProperty("java.vendor");
    strJavaVersion = System.getProperty("java.version");
    strOSName = System.getProperty("os.name");
    if (mayScript) {
      jsoWindow = JSObject.getWindow(applet);
      System.out.println("JmolAppletRegistry: jsoWindow=" + jsoWindow);
    }
    ns4 = (strJavaVendor.startsWith("Netscape") &
           strJavaVersion.startsWith("1.1"));
    if (! ns4)
      checkIn(name, applet);
    else if (mayScript)
      checkInJavascript(name, applet);
    else
      System.out.println("WARNING!! mayscript not specified");
  }

  public void scriptButton(String targetName, String script, String buttonCallback) {
    if (targetName == null || targetName.length() == 0) {
      System.out.println("no targetName specified");
      return;
    }
    if (! ns4) {
      Object target = htRegistry.get(targetName);
      if (target == null) {
        System.out.println("target " + targetName + " not found");
        return;
      }
      if (! (target instanceof JmolApplet)) {
        System.out.println("target " + targetName + " is not a JmolApplet");
        return;
      }
      JmolApplet targetJmolApplet = (JmolApplet)target;
      targetJmolApplet.scriptButton(jsoWindow, name, script, buttonCallback);
    } else {
      if (mayScript) 
        jsoTop.call("runJmolAppletScript",
                    new Object[] { targetName, jsoWindow, name,
                                   script, buttonCallback });
      else
        System.out.println("WARNING!! mayscript not specified");
    }
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
  
  String functionRunJmolAppletScript=
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
    "}";

  void checkInJavascript(String name, Applet applet) {
    if (name != null && jsoWindow != null) {
      jsoTop = (JSObject)jsoWindow.getMember("top");
      Object t;
      t = jsoTop.eval("top.runJmolAppletScript == undefined");
      if (((Boolean)t).booleanValue())
        jsoTop.eval(functionRunJmolAppletScript);
    }
  }
}
