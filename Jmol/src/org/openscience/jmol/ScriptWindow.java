/*
 * @(#)ScriptWindow.java    1.0 2000/12/03
 *
 * Copyright (c) 2000 Egon L. Willighagen All Rights Reserved.
 *
 * GPL 2.0 license.
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
        output = new JTextArea(20,60);
        output.setEditable(false);
        output.append("> ");
	JScrollPane scrollPane = new JScrollPane(output);
        getContentPane().add("North",scrollPane);
        input = new JTextField();
        input.addActionListener(new ActionListener() {

            private RasMolScriptHandler scripthandler = new RasMolScriptHandler(window);

            public void actionPerformed(ActionEvent e) {
                String command = input.getText();
                output.append(command);
                output.append("\n");
                input.setText(null);
                
                // execute script
		try { 
		    scripthandler.handle(command);
		} catch (RasMolScriptException rasmolerror) {
		    output.append("Error:\n");
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
