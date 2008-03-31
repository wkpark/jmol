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

package org.jmol.api;

import java.util.Hashtable;

public interface JmolStatusListener {
/*
 * These methods specifically provide notification from 
 * Viewer.StatusManager to the two main classes, applet or app.
 * so that they can handle them slightly differently. This might be
 * a callback for the applet, for example, but not for the app.
 * ALL viewer-type processing, including status queue writing
 * has been done PRIOR to these functions being called.   Bob Hanson
 * 
 */

  public void setCallbackFunction(String callbackType, String callbackFunction);
  
  public void notifyCallback(int type, Object[] data);

  public boolean notifyEnabled(int callback_pick);

  public String eval(String strEval);
  
  public float[][] functionXY(String functionName, int x, int y);
  
  public void createImage(String file, Object type_or_text_or_bytes, int quality);

  public Hashtable getRegistryInfo();

  public void handlePopupMenu(int x, int y);

  public void showConsole(boolean showConsole);
  
  public void showUrl(String url);
}
