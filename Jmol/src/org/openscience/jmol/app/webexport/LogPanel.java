/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import java.awt.*;
import javax.swing.*;

class LogPanel {

  private static JTextArea logArea;
  private static boolean resetFlag;

  static void log(String message) {
    if (resetFlag)
      logArea.setText("");
    resetFlag = (message.length() == 0);
    logArea.append(message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  static String getText() {
    return logArea.getText();
  }

  JComponent getPanel() {
    //Now layout the LogPanel.  It will be added to the tabs in the main class.

    //Create the log first, because the action listeners
    //need to refer to it.
    logArea = new JTextArea(20, 20);
    logArea.setMargin(new Insets(5, 5, 5, 5));
    logArea.setEditable(false);
    JScrollPane logScrollPane = new JScrollPane(logArea);

    //Create a label for the log
    JLabel logLabel = new JLabel("Log and Error Messages:");
    //put in its own panel so that it will be centered
    JPanel logLabelPanel = new JPanel();
    logLabelPanel.add(logLabel);

    //Create a panel of the log and its label
    JPanel logPanel = new JPanel();
    logPanel.setLayout(new BorderLayout());
    logPanel.add(logLabelPanel, BorderLayout.PAGE_START);
    logPanel.add(logScrollPane, BorderLayout.PAGE_END);

    return (logPanel);
  }
}
