/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-04-13 22:22:44 -0500 (Tue, 13 Apr 2010) $
 * $Revision: 12851 $
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


import java.applet.Applet;
import java.awt.Event;
import java.awt.Graphics;

import org.jmol.smiles.SmilesMatcher;

public class JmolSmilesApplet extends Applet {

  public JmolSmilesApplet() {
    System.out.println("JmolSmilesApplet constructor");
  }

  public void init() {
    System.out.println("JmolSmilesApplet init");
  }

  public int find(String pattern, String smiles, boolean isSearch, boolean isAll) {
    System.out.println("find " + pattern + " in " + smiles + " isSearch? " + isSearch + "; isAll? " + isAll);
    int ret = -1;
    try {
      System.out.println("getting smiles matcher");
      SmilesMatcher sm = new SmilesMatcher();
      System.out.println(sm);
      ret = sm.find(pattern, smiles, isSearch, isAll);
      System.out.println("ret" + ret);
    } catch (Exception e) {
      e.printStackTrace();
    } catch (Error er) {
      er.printStackTrace();
    }
    return ret;
  }

  public String getAppletInfo() {
    return "JmolSmilesApplet";
  }
  
  public void update(Graphics g) {}
  public void paint(Graphics g) {}
  public boolean handleEvent(Event e) {
    return false;
  }
  
  public void destroy() {
    System.out.println("JmolSmilesApplet destroy");
  }

}
