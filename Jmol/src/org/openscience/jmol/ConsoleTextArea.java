/*
 * Original code by Tony LaPaso
 *
 * Added to Jmol 11/04/99 by Charles Fulton
 *
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
