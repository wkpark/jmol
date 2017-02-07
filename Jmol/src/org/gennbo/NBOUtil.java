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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;


import javajs.util.SB;


public class NBOUtil {

  protected static void postAddGlobalC(SB sb, String label, String val) {
    sb.append("GLOBAL C_").append(label).append(" ").append(val).append(sep);  
   }

  protected static void postAddGlobalI(SB sb, String label, int offset, JComboBox<String> cb) {
    sb.append("GLOBAL I_").append(label).append(" ").appendI(cb == null ? offset : cb.getSelectedIndex() + offset).append(sep);  
   }

  protected static void postAddGlobalT(SB sb, String key, JTextField t) {
    sb.append("GLOBAL ").append(key).append(" ").append(t.getText()).append(sep);
  }

  protected static void postAddGlobal(SB sb, String key, String val) {
    sb.append("GLOBAL ").append(key).append(" ").append(val).append(sep);
  }

  protected static SB postAddCmd(SB sb, String cmd) {
    return sb.append("CMD ").append(cmd).append(sep);
  }

  /**
   * Creates the title blocks with background color for headers.
   * 
   * @param title
   *        - title for the section
   * @param rightSideComponent
   *        help button, for example
   * @return Box formatted title box
   */
  protected static Box createTitleBox(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(NBOConfig.titleColor);
    label.setForeground(Color.white);
    label.setFont(NBOConfig.titleFont);
    label.setOpaque(true);
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.add(label, BorderLayout.WEST);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box2.setMaximumSize(new Dimension(360, 25));
      box.add(box2);
    } else
      box.add(label);
    box.setAlignmentX(0.0f);
  
    return box;
  }

  /**
   * create a bordered box, either vertical or horizontal
   * 
   * @param isVertical
   * @return a box
   */
  protected static Box createBorderBox(boolean isVertical) {
    Box box = isVertical ? Box.createVerticalBox() : Box.createHorizontalBox();
    box.setAlignmentX(0.0f);
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    return box;
  }

  protected static final String sep = System.getProperty("line.separator");
  
///**
//* Centers the dialog on the screen.
//* 
//* @param d
//*/
//protected void centerDialog(JDialog d) {
// int x = getWidth() / 2 - d.getWidth() / 2 + this.getX();
// int y = getHeight() / 2 - d.getHeight() / 2;
// d.setLocation(x, y);
//}
//

}

class StyledComboBoxUI extends MetalComboBoxUI {

  protected int height;
  protected int width;

  protected StyledComboBoxUI(int h, int w) {
    super();
    height = h;
    width = w;
  }

  @Override
  protected ComboPopup createPopup() {
    BasicComboPopup popup = new BasicComboPopup(comboBox) {
      @Override
      protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        return super.computePopupBounds(px, py, Math.max(width, pw), height);
      }
    };
    popup.getAccessibleContext().setAccessibleParent(comboBox);
    return popup;
  }
}
