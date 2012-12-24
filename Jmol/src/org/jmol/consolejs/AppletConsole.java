/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-07-21 10:12:08 -0500 (Sat, 21 Jul 2012) $
 * $Revision: 17376 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, www.jmol.org
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
package org.jmol.consolejs;

import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.console.GenericConsole;

public class AppletConsole extends GenericConsole {

  public AppletConsole() {
  }

  Object jsConsole;

  public void start(JmolViewer viewer) {
    this.viewer = viewer;
    setLabels();
    displayConsole(); // will call layoutWindow
  }

  @Override
  protected void layoutWindow(String enabledButtons) {
    /**
     * TODO: all the buttons are set up; implement the window now
     * also set up this.input, this.output
     * it can stay hidden at this point
     * 
     * @j2sNative
     * 
     *            jsConsole = Jmol.Console.createConsole(this); 
     *            this.label1 = jsConsole.label1;
     *            this.setTitle();
     */
  }

  @Override
  protected void setTitle() {
    /**
     * @j2sNative
     * 
     * if (jsConsole)
     *   jsConsole.setTitle(this.getLabel("title"));
     * 
     */
  }

  @Override
  public void setVisible(boolean visible) {
    /**
     * @j2sNative
     * 
     *            jsConsole.setVisible(visible);
     * 
     */
    {
    }
  }

  public void actionPerformed(Object source) {
    // TODO -- button press -- tie this in to JavaScript so that it can be called
    doAction(source);
  }

  public void processComponentKeyEvent(int kcode, int kid, boolean isControlDown) {
    // TODO -- key pressed -- tie this in to JavaScript so that it can be called
    processKey(kcode, kid, isControlDown);
  }

  @Override
  protected JmolAbstractButton setButton(String text) {
    /**
     * @j2sNative
     * 
     *            return console.setButton(text);
     */
    {
      return null;
    }
  }

  @Override
  public void dispose() {
    setVisible(false);
  }

  @Override
  protected boolean isMenuItem(Object source) {
    //ignore
    return false;
  }

  @Override
  public JmolScriptEditorInterface getScriptEditor() {
    //ignore
    return null;
  }

  @Override
  protected String nextFileName(String stub, int nTab) {
    //ignore
    return null;
  }

}
