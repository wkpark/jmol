/*
 * @(#)WhatsNewDialog.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

public class WhatsNewDialog extends JDialog implements HyperlinkListener
{
    private static JmolResourceHandler jrh;
    JEditorPane html;

    static {
        jrh = new JmolResourceHandler("WhatsNew");
    }
    
    public WhatsNewDialog(JFrame fr) {
        
        super(fr, "What's New in Jmol", true);

        try {
			URL changeLogURL = ClassLoader.getSystemResource(jrh.getString("changeLogURL"));
			if (changeLogURL != null) {
				html = new JEditorPane(changeLogURL);
			} else {
				html = new JEditorPane("text/plain", "Unable to find url '" + jrh.getString("changeLogURL") + "'.");
			}
            html.setEditable(false);
            html.addHyperlinkListener(this);
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
            JScrollPane scroller = new JScrollPane() {
                public Dimension getPreferredSize() {
                    return new Dimension(500,400);
                }
                public float getAlignmentX() {
                    return LEFT_ALIGNMENT;
                }
            };
            scroller.getViewport().add(html);

        JPanel htmlWrapper = new JPanel(new BorderLayout());
        htmlWrapper.setAlignmentX(LEFT_ALIGNMENT);
        htmlWrapper.add(scroller, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
        JButton ok = new JButton(jrh.getString("okLabel"));
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OKPressed();
            }});
        buttonPanel.add( ok );
        getRootPane().setDefaultButton(ok);
        
        JPanel container = new JPanel();
        container.setLayout( new BorderLayout() );
        
        container.add(htmlWrapper, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(container);
        pack();
        centerDialog();
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            linkActivated(e.getURL());
        }
    }

    /**
     * Follows the reference in an
     * link.  The given url is the requested reference.
     * By default this calls <a href="#setPage">setPage</a>,
     * and if an exception is thrown the original previous
     * document is restored and a beep sounded.  If an 
     * attempt was made to follow a link, but it represented
     * a malformed url, this method will be called with a
     * null argument.
     *
     * @param u the URL to follow
     */
    protected void linkActivated(URL u) {
        Cursor c = html.getCursor();
        Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        html.setCursor(waitCursor);
        SwingUtilities.invokeLater(new PageLoader(u, c));
    }
    
    /**
     * temporary class that loads synchronously (although later than
     * the request so that a cursor change can be done).  
     */
    class PageLoader implements Runnable {
        
        PageLoader(URL u, Cursor c) {
            url = u;
            cursor = c;
        }
        
        public void run() {
            if (url == null) {
                // restore the original cursor
                html.setCursor(cursor);
                
                // remove this hack when automatic validation is
                // activated.
                Container parent = html.getParent();
                parent.repaint();
            } else {
                Document doc = html.getDocument();
                try {
                    html.setPage(url);
                } catch (IOException ioe) {
                    html.setDocument(doc);
                    getToolkit().beep();
                } finally {
                    // schedule the cursor to revert after the paint
                    // has happended.
                    url = null;
                    SwingUtilities.invokeLater(this);
                }
            }
        }
        
        URL url;
        Cursor cursor;
    }
    

    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height/2;
        screenSize.width = screenSize.width/2;
        size.height = size.height/2;
        size.width = size.width/2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x,y);
    }

    public void OKPressed() {
        this.setVisible(false);
    }

}
