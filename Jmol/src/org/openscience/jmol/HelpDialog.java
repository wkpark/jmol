
/*
 * Copyright 2002 The Jmol Development Team
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


import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JEditorPane;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import java.awt.Container;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.io.IOException;

public class HelpDialog extends JDialog implements HyperlinkListener {

  JEditorPane html;

  public HelpDialog(JFrame fr) {

    super(fr, "Jmol Help", true);

    try {
      URL helpURL =
        this.getClass().getClassLoader()
          .getResource(JmolResourceHandler.getInstance()
            .getString("Help.helpURL"));
      if (helpURL != null) {
        html = new JEditorPane(helpURL);
      } else {
        html = new JEditorPane("text/plain",
            "Unable to find url '"
              + JmolResourceHandler.getInstance().getString("Help.helpURL")
                + "'.");
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
        return new Dimension(300, 300);
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
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton ok =
      new JButton(JmolResourceHandler.getInstance()
        .getString("Help.okLabel"));
    ok.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        OKPressed();
      }
    });
    buttonPanel.add(ok);
    getRootPane().setDefaultButton(ok);

    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

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
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void OKPressed() {
    this.setVisible(false);
  }

}
