/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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
package org.gennbo;

import java.util.Map;

import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.JmolPlugin;

public class NBOPlugin implements JmolPlugin {

  protected NBODialog nboDialog;
  protected Viewer vwr;
  
  public final static String version = "0.1.2";

  @Override
  public void start(JFrame frame, Viewer vwr, Map<String, Object> jmolOptions) {
    this.vwr = vwr;
    nboDialog = new NBODialog(this, frame, vwr, jmolOptions);
    System.out.println("NBO Plugin started.");    
  }

  @Override
  public String getName() {
    return "NBO";
  }
  
  @Override
  public String getVersion() {
    return version;
  }
 
  @Override
  public void setVisible(boolean b) {
    if (nboDialog == null)
      return;
    nboDialog.setVisible(b);
  }

  @Override
  public void destroy() {
    if (nboDialog == null)
      return;
    nboDialog.close();
    nboDialog = null;
  }

  @Override
  public void notifyCallback(CBK type, Object[] data) {
    if (nboDialog == null)
      return;
    nboDialog.notifyCallback(type, data);
  }

}
