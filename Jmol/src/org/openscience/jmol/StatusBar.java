
/*
 * Copyright 2001 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.awt.*;
import javax.swing.*;

class StatusBar extends JPanel {

    JLabel status[];

    public StatusBar() {
        status = new JLabel[3];
        setLayout(new GridLayout(1, 3));
        setPreferredSize(new Dimension(640, 30));
        status[0] = new JLabel();
        status[0].setPreferredSize(new Dimension(100, 100));
        status[0].setBorder(BorderFactory.createBevelBorder(1));
        status[0].setHorizontalAlignment(0);
        status[1] = new JLabel();
        status[1].setPreferredSize(new Dimension(100, 100));
        status[1].setBorder(BorderFactory.createBevelBorder(1));
        status[1].setHorizontalAlignment(0);
        status[2] = new JLabel();
        status[2].setPreferredSize(new Dimension(100, 100));
        status[2].setBorder(BorderFactory.createBevelBorder(1));
        status[2].setHorizontalAlignment(0);
        add(status[0]);
        add(status[1]);
        add(status[2]);
    }
    
    public void setStatus(int label, String text) {
        status[label - 1].setText(text);
    }
}
