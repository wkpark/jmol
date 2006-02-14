  /* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

/**
 * This class only exists so that people can declare
 * JmolApplet in applet tags without having to give a full package
 * specification
 *
 * see org.jmol.applet.Jmol
 *
 */

import org.jmol.api.JmolAppletInterface;
import netscape.javascript.JSObject;

public class JmolApplet
  extends org.jmol.appletwrapper.AppletWrapper
  implements JmolAppletInterface {
  
  public JmolApplet() {
    super("org.jmol.applet.Jmol",
          "jmol75x29x8.gif",
          "Loading Jmol applet ...",
          3, preloadClasses);
  }

  private final static String[] preloadClasses = {
    "javax.vecmath.Point3f+",
    "org.jmol.g3d.Graphics3D",
    "org.jmol.adapter.smarter.SmarterJmolAdapter",
    "org.jmol.popup.JmolPopup",

    "javax.vecmath.Vector3f+",
    ".Matrix3f+", ".Point3i+",

    "org.jmol.g3d.Sphere3D",
    ".Line3D", ".Cylinder3D", ".Colix", ".Shade3D",

    "org.jmol.adapter.smarter.Atom",
    ".Bond", ".AtomSetCollection", ".AtomSetCollectionReader",
    ".Resolver",

  };

  public String getPropertyAsString(String infoType) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getPropertyAsString(infoType));
  }

  public String getPropertyAsString(String infoType, String paramInfo) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getPropertyAsString(infoType, paramInfo));
  }

  public String getPropertyAsJSON(String infoType) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getPropertyAsJSON(infoType));
  }

  public String getPropertyAsJSON(String infoType, String paramInfo) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getPropertyAsJSON(infoType, paramInfo));
  }

  public Object getProperty(String infoType) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getProperty(infoType));
  }

  public Object getProperty(String infoType, String paramInfo) {
    return (wrappedApplet == null ? null : ((JmolAppletInterface)wrappedApplet).getProperty(infoType, paramInfo));
  }

  public String script(String script) {
    if (wrappedApplet != null)
      return ((JmolAppletInterface)wrappedApplet).script(script);
    return null;
  }
  
  public String scriptWait(String script) {
    if (wrappedApplet != null)
      return ((JmolAppletInterface)wrappedApplet).scriptWait(script);
    return null;
  }
  
  public void loadInline(String strModel) {
    if (wrappedApplet != null)
      ((JmolAppletInterface)wrappedApplet).loadInline(strModel);
  }

  public void loadInline(String strModel, String script) {
    if (wrappedApplet != null)
      ((JmolAppletInterface)wrappedApplet).loadInline(strModel, script);
  }

  public void loadNodeId(String nodeId) {
    if (wrappedApplet != null)
      ((JmolAppletInterface)wrappedApplet).loadNodeId(nodeId);
  }

  public void loadDOMNode(JSObject DOMNode) {
    if (wrappedApplet != null)
      ((JmolAppletInterface)wrappedApplet).loadDOMNode(DOMNode);
  }

  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    if (wrappedApplet != null)
      ((JmolAppletInterface)wrappedApplet).scriptButton(buttonWindow,
                                                        buttonName,
                                                        script,
                                                        buttonCallback);
  }
}
