/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-24 10:35:00 -0500 (Wed, 24 Jun 2009) $
 * $Revision: 11106 $
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
package org.jmol.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextPane; //import javax.swing.SwingUtilities;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JScrollPane;

import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.ScriptContext;

public final class ScriptEditor extends JDialog implements JmolScriptEditorInterface, ActionListener {

  EditorTextPane editor;
  private JButton closeButton;
  private JButton loadButton;
  private JButton runButton;
  private JButton pauseButton;
  private JButton stepButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton stateButton;
  JmolViewer viewer;

  /*
   * methods sendeditorEcho, sendeditorMessage(strStatus), notifyScriptStart(),
   * notifyScriptTermination() are public in case developers want to use
   * ScriptWindow separate from the Jmol application.
   */

  public ScriptEditor() { 
  }

  private JFrame jf;

  public JmolScriptEditorInterface getScriptEditor(JmolViewer viewer, Object frame) {
    return new ScriptEditor(viewer, jf = new JFrame());
  }
  
  public ScriptEditor(JmolViewer viewer, JFrame frame) {
    super(frame, GT._("Jmol Script Editor"), false);
    this.viewer = viewer;
    layoutWindow(getContentPane());
    setSize(645, 400);
    setLocationRelativeTo(frame);
  }

  void layoutWindow(Container container) {
    editor = new EditorTextPane(this);
    editor.setDragEnabled(true);
    JScrollPane editorPane = new JScrollPane(editor);

    JPanel buttonPanel = new JPanel();

    stateButton = new JButton(GT._("State"));
    stateButton.addActionListener(this);
    buttonPanel.add(stateButton);

    loadButton = new JButton(GT._("Script"));
    loadButton.addActionListener(this);
    buttonPanel.add(loadButton);

    runButton = new JButton(GT._("Run"));
    runButton.addActionListener(this);
    buttonPanel.add(runButton);
    runButton.setEnabled(false);

    pauseButton = new JButton(GT._("Pause"));
    pauseButton.addActionListener(this);
    buttonPanel.add(pauseButton);
    pauseButton.setEnabled(false);

    stepButton = new JButton(GT._("Step"));
    stepButton.addActionListener(this);
    buttonPanel.add(stepButton);
    stepButton.setEnabled(false);

    haltButton = new JButton(GT._("Halt"));
    haltButton.addActionListener(this);
    buttonPanel.add(haltButton);
    haltButton.setEnabled(false);

    clearButton = new JButton(GT._("Clear"));
    clearButton.addActionListener(this);
    buttonPanel.add(clearButton);

    closeButton = new JButton(GT._("Close"));
    closeButton.addActionListener(this);
    buttonPanel.add(closeButton);

    // container.setLayout(new BorderLayout());
    // container.add(editorPane, BorderLayout.CENTER);
    JPanel buttonPanelWrapper = new JPanel();
    buttonPanelWrapper.setLayout(new BorderLayout());
    buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);

    JSplitPane spane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPane,
        buttonPanelWrapper);
    editorPane.setMinimumSize(new Dimension(300, 300));
    editorPane.setPreferredSize(new Dimension(5000, 5000));
    buttonPanelWrapper.setMinimumSize(new Dimension(60, 60));
    buttonPanelWrapper.setMaximumSize(new Dimension(1000, 60));
    buttonPanelWrapper.setPreferredSize(new Dimension(60, 60));
    spane.setDividerSize(0);
    spane.setResizeWeight(0.95);
    container.add(spane);
    // container.setLayout(new BorderLayout());
    // container.add(editorPane,BorderLayout.CENTER);
    // container.add(buttonPanelWrapper,BorderLayout.SOUTH);

  }

  public void notifyScriptStart() {
    runButton.setEnabled(true);
    haltButton.setEnabled(true);
    pauseButton.setEnabled(true);
  }

  public void notifyScriptTermination() {
    runButton.setEnabled(true);
    pauseButton.setEnabled(false);
    haltButton.setEnabled(false);
  }

  private ScriptContext context;
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == closeButton) {
      hide();
    } else if (source == loadButton) {
      context = (ScriptContext) viewer.getProperty(null, "scriptContext", null);
      editor.clearContent(context.script);
    } else if (source == runButton) {
    } else if (source == pauseButton) {
    } else if (source == stepButton) {
    } else if (source == clearButton) {
      editor.clearContent();
    } else if (source == stateButton) {
      editor.clearContent(viewer.getStateInfo());
    } else if (source == haltButton) {
      viewer.haltScriptExecution();
      editor.grabFocus(); // always grab the focus (e.g., after clear)
    }
  }

  class EditorTextPane extends JTextPane {

    EditorDocument editorDoc;
    //JmolViewer viewer;

    boolean checking = false;

    EditorTextPane(ScriptEditor scriptEditor) {
      super(new EditorDocument());
      editorDoc = (EditorDocument) getDocument();
      editorDoc.setEditorTextPane(this);
      //this.viewer = scriptEditor.viewer;
    }

    public void clearContent() {
      clearContent(null);
    }

    public void clearContent(String text) {
      editorDoc.clearContent();
      if (text != null)
        editorDoc.outputEcho(text);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
     */

    /**
     * Custom key event processing for command 0 implementation.
     * 
     * Captures key up and key down strokes to call command history and
     * redefines the same events with control down to allow caret vertical
     * shift.
     * 
     * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
     */
    protected void processKeyEvent(KeyEvent ke) {
      // Id Control key is down, captures events does command
      // history recall and inhibits caret vertical shift.

      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      if (kcode == KeyEvent.VK_UP && kid == KeyEvent.KEY_PRESSED
          && !ke.isControlDown()) {
      } else if (kcode == KeyEvent.VK_DOWN && kid == KeyEvent.KEY_PRESSED
          && !ke.isControlDown()) {
      } else if ((kcode == KeyEvent.VK_DOWN || kcode == KeyEvent.VK_UP)
          && kid == KeyEvent.KEY_PRESSED && ke.isControlDown()) {
      } else {
        // Standard processing for other events.
        super.processKeyEvent(ke);
        // check command for compiler-identifyable syntax issues
        // this may have to be taken out if people start complaining
        // that only some of the commands are being checked
        // that is -- that the script itself is not being fully checked

        // not perfect -- help here?
        if (kid == KeyEvent.KEY_RELEASED
            && ke.getModifiers() < 2
            && (kcode > KeyEvent.VK_DOWN && kcode < 400 || kcode == KeyEvent.VK_BACK_SPACE)) {

        }
      }
    }

  }

  class EditorDocument extends DefaultStyledDocument {

    EditorTextPane EditorTextPane;

    SimpleAttributeSet attError;
    SimpleAttributeSet attEcho;
    SimpleAttributeSet attPrompt;
    SimpleAttributeSet attUserInput;
    SimpleAttributeSet attStatus;

    EditorDocument() {
      super();

      attError = new SimpleAttributeSet();
      StyleConstants.setForeground(attError, Color.red);

      attPrompt = new SimpleAttributeSet();
      StyleConstants.setForeground(attPrompt, Color.magenta);

      attUserInput = new SimpleAttributeSet();
      StyleConstants.setForeground(attUserInput, Color.black);

      attEcho = new SimpleAttributeSet();
      StyleConstants.setForeground(attEcho, Color.blue);
      StyleConstants.setBold(attEcho, true);

      attStatus = new SimpleAttributeSet();
      StyleConstants.setForeground(attStatus, Color.black);
      StyleConstants.setItalic(attStatus, true);
    }

    void setEditorTextPane(EditorTextPane EditorTextPane) {
      this.EditorTextPane = EditorTextPane;
    }


    /**
     * Removes all content of the script window, and add a new prompt.
     */
    void clearContent() {
      try {
        super.remove(0, getLength());
      } catch (BadLocationException exception) {
        Logger.error("Could not clear script window content", exception);
      }
    }

    void outputEcho(String text) {
      clearContent();
      try {
        super.insertString(0, text, attEcho);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public Object getMyMenuBar() {
    return null;
  }

  public String getText() {
    return editor.getText();
  }

  public void output(String message) {
    editor.clearContent(message);
  }

  public void dispose() {
    if(jf != null)
      jf.dispose();
  }


}
