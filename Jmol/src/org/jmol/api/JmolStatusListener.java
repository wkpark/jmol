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

public interface JmolStatusListener {
  public void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile,
                               String errorMessage);

  public void setStatusMessage(String statusMessage);
  public void setStatusMessage(String statusMessage, String additionalInfo);
  
  public void scriptEcho(String strEcho);

  public void scriptStatus(String strStatus);

  public void notifyScriptTermination(String statusMessage, int msWalltime);

  public void handlePopupMenu(int x, int y);

  public void notifyMeasurementsChanged();

  public void notifyFrameChanged(int frameNo);

  public void notifyAtomPicked(int atomIndex, String strInfo);

  public void showUrl(String url);

  public void showConsole(boolean showConsole);

}
