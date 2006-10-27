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

package org.jmol.appletwrapper;

import java.awt.*;
import netscape.javascript.JSObject;

public interface WrappedApplet {
  public void setAppletWrapper(AppletWrapper appletWrapper);
  public void init();
  public String getAppletInfo();
  public void update(Graphics g);
  public void paint(Graphics g);
  public boolean handleEvent(Event e);

  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback);
  public void script(String script);
  public String scriptCheck(String script);
  public String scriptWait(String script);
  public void loadInline(String strModel);
  public void loadInline(String[] strModels);
  public void loadInline(String strModel, String script);
  public void loadInline(String[] strModels, String script);
  public void loadNodeId(String nodeId);
  public void loadDOMNode(JSObject DOMNode);
  public Object getProperty(String infoType);
  public Object getProperty(String infoType, String paramInfo);
  public String getPropertyAsString(String infoType);
  public String getPropertyAsString(String infoType, String paramInfo);
  public String getPropertyAsJSON(String infoType);
  public String getPropertyAsJSON(String infoType, String paramInfo);

}
