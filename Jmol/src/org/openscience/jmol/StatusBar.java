/*
 * @(#)StatusBar.java    2.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter & Chrisotpher Steinbeck 
 *    All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
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
