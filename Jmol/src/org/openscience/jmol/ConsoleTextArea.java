
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

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class ConsoleTextArea extends JTextArea {
    public ConsoleTextArea(InputStream[] inStreams) {
        for(int i = 0; i < inStreams.length; ++i)
            startConsoleReaderThread(inStreams[i]);
    } // ConsoleTextArea()
    
    
    public ConsoleTextArea() throws IOException {
        final LoopedStreams ls = new LoopedStreams();
        
        // Redirect System.out & System.err.
        
        PrintStream ps = new PrintStream(ls.getOutputStream());
        System.setOut(ps);
        System.setErr(ps);
        
        startConsoleReaderThread(ls.getInputStream());
    } // ConsoleTextArea()
    
    
    private void startConsoleReaderThread(
                                          InputStream inStream) {
        final BufferedReader br =
            new BufferedReader(new InputStreamReader(inStream));
        new Thread(new Runnable() {
                public void run() {
                    StringBuffer sb = new StringBuffer();
                    try {
                        String s;
                        Document doc = getDocument();
                        while((s = br.readLine()) != null) {
                            boolean caretAtEnd = false;
                            caretAtEnd = getCaretPosition() == doc.getLength() ?
                                true : false;
                            sb.setLength(0);
                            append(sb.append(s).append('\n').toString());
                            if(caretAtEnd)
                                setCaretPosition(doc.getLength());
                        }
                    }
                    catch(IOException e) {
                        JOptionPane.showMessageDialog(null,
                                                      "Error reading from BufferedReader: " + e);
                        System.exit(1);
                    }
                }
            }).start();
    } 
}
