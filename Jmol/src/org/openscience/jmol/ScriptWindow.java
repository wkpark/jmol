/*
 * @(#)ScriptWindow.java    1.0 2000/12/03
 *
 * Copyright (c) 2000 Egon L. Willighagen All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol;

import javax.swing.*;
import java.awt.event.*;

public class ScriptWindow extends JDialog implements java.awt.event.WindowListener, java.awt.event.ActionListener{

    private JTextArea output;
    private JTextField input;
    private JButton close;

    private Jmol window;

    public ScriptWindow(Jmol boss){
        super(boss.frame,"Rasmol Scripts",true);
        window = boss;
        getContentPane().setLayout(new java.awt.BorderLayout());
        output = new JTextArea(20,30);
        output.setEditable(false);
        output.append("> ");
	JScrollPane scrollPane = new JScrollPane(output);
        getContentPane().add("North",scrollPane);
        input = new JTextField();
        input.addActionListener(new ActionListener() {

            private RasMolScriptHandler scripthandler = new RasMolScriptHandler(window, output);

            public void actionPerformed(ActionEvent e) {
                String command = input.getText();
                output.append(command);
                output.append("\n");
                input.setText(null);
                
                // execute script
		try { 
		    scripthandler.handle(command);
		} catch (RasMolScriptException rasmolerror) {
                    output.append(rasmolerror.getMessage());
                    output.append("\n");
		}

                // return prompt
                output.append("> ");
            }});
        getContentPane().add("Center",input);
        close = new JButton("Close");
        close.addActionListener(this);
        getContentPane().add("South",close);
        setLocationRelativeTo(boss);
        pack();
    }

    public void windowClosing(java.awt.event.WindowEvent e){
	hide();
    }

    public void actionPerformed(java.awt.event.ActionEvent e){
	hide();
    }
    
    public void windowClosed(java.awt.event.WindowEvent e) {}
    public void windowOpened(java.awt.event.WindowEvent e) {}
    public void windowIconified(java.awt.event.WindowEvent e) {}
    public void windowDeiconified(java.awt.event.WindowEvent e) {}
    public void windowActivated(java.awt.event.WindowEvent e) {}
    public void windowDeactivated(java.awt.event.WindowEvent e) {}
}
