/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.applet;
import org.jmol.api.JmolAppletInterface;
import org.jmol.appletwrapper.AppletWrapper;

import java.applet.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import netscape.javascript.JSObject;
import org.jmol.util.Logger;
public class JmolAppletRegistry {

  public static Hashtable htRegistry = new Hashtable();

  String name;
  boolean mayScript;
  Applet applet;
  AppletContext appletContext;

  public JmolAppletRegistry(String name, boolean mayScript, Applet applet) {
    if (name == null || name.length() == 0)
      name = null;
    this.name = name;
    this.mayScript = mayScript;
    this.applet = applet;
    this.appletContext = applet.getAppletContext();
    checkIn(name, applet);
  }

  public synchronized Enumeration applets() {
    return htRegistry.elements();
  }

  synchronized static void checkIn(String name, Applet applet) {
    cleanRegistry();
    Logger.info("AppletRegistry.checkIn(" + name + ")");
    if (name != null)
      htRegistry.put(name, applet);
    if (Logger.debugging) {
      Enumeration keys = htRegistry.keys();
      while (keys.hasMoreElements()) {
        String theApplet = (String) keys.nextElement();
        Logger.debug(theApplet + " "+ htRegistry.get(theApplet));
      }
    }
  }

  synchronized static void checkOut(String name) {
   htRegistry.remove(name);
   //System.out.println("\napplet registry checkout: " + name);
  }
  
  synchronized static void cleanRegistry() {
    Enumeration keys = htRegistry.keys();
    AppletWrapper app = null;
    boolean closed = true;
    while (keys.hasMoreElements()) {
      String theApplet = (String) keys.nextElement();
      try {
        app = (AppletWrapper) (htRegistry.get(theApplet));
        JSObject theWindow = JSObject.getWindow(app);
        //System.out.print("checking " + app + " window : ");
        closed = ((Boolean)theWindow.getMember("closed")).booleanValue();
        //System.out.println(closed);
        if (closed || theWindow.hashCode() == 0) {
          //error trap
        }
        if (Logger.debugging)
          Logger.debug("Preserving registered applet " + theApplet + " window: " + theWindow.hashCode());
      } catch (Exception e) {
        closed = true;
      }
      if (closed){
        if (Logger.debugging)
          Logger.debug("Dereferencing closed window applet " + theApplet);
        htRegistry.remove(theApplet);
        app.destroy();
      }
    }
  }
  
  JSObject getJsoWindow() {
    JSObject jsoWindow = null;
    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(applet);
      } catch (Exception e) {
        Logger.error("exception trying to get jsoWindow");
      }
    } else {
      Logger.warn("mayScript not specified for:" + name);
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
        Logger.error("exception trying to get window.top");
      }
    }
    return jsoTop;
  }
  
  public void script(String targetName, String script) {
    scriptCallback(targetName, script, null);
  }

  public void scriptCallback(String targetName, String script,
                             String callbackJavaScript) {
    if (targetName == null || targetName.length() == 0) {
      Logger.error("no targetName specified");
      return;
    }
    if (tryDirect(targetName, script, callbackJavaScript))
      return;
    /*
    if (tryJavaScript(targetName, script, callbackJavaScript))
      return;
    */
    Logger.error("unable to find target:" + targetName);
  }

  synchronized public static Vector findApplets(String appletName, String mySyncId,
                            String excludeName) {
    if (appletName != null && appletName.indexOf("[") < 0)
      appletName += "[" + mySyncId + "]";
    Vector apps = new Vector();
    if (appletName != null && htRegistry.containsKey(appletName)) {
      apps.addElement(appletName);
      return apps;
    }
    Enumeration keys = htRegistry.keys();
    while (keys.hasMoreElements()) {
      String theApplet = (String) keys.nextElement();
      if (excludeName != null && theApplet.equals(excludeName))
        continue;
      if (appletName == null && theApplet.indexOf("[" + mySyncId + "]") > 0
          || theApplet.equals(appletName))
        apps.addElement(theApplet);
    }
    return apps;
  }
  
  synchronized private boolean tryDirect(String targetName, String script,
                            String callbackJavaScript) {
    Logger.debug("tryDirect trying appletContext");
    Object target = appletContext.getApplet(targetName);
    if (target == null) {
      Logger.debug("... trying registry");
      Vector apps = findApplets(targetName, null, null);
      if (apps.size() > 0)
        target = htRegistry.get(apps.elementAt(0));
    }
    if (target == null) {
      Logger.error("tryDirect failed to find applet:" + targetName);
      return false;
    }
    if (! (target instanceof JmolAppletInterface)) {
      Logger.error("target " + targetName + " is not a JmolApplet");
      return true;
    }
    JmolAppletInterface targetJmolApplet = (JmolAppletInterface)target;
    targetJmolApplet.scriptButton((callbackJavaScript == null
                                   ? null : getJsoWindow()),
                                  name, script, callbackJavaScript);
    return true;
  }

  /*
  private boolean tryJavaScript(String targetName, String script,
                                   String callbackJavaScript) {
    if (mayScript) {
      JSObject jsoTop = getJsoTop();
      if (jsoTop != null) {
        try {
          jsoTop.eval(functionRunJmolAppletScript);
          jsoTop.call("runJmolAppletScript",
                      new Object[] { targetName, getJsoWindow(), name,
                                     script, callbackJavaScript });
          return true;
        } catch (Exception e) {
          Logger.error("exception calling JavaScript");
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
