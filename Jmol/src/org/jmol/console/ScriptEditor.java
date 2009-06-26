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
import java.awt.Rectangle;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextPane; //import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;
import javax.swing.JScrollPane;

import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.ScriptContext;

public final class ScriptEditor extends JDialog implements JmolScriptEditorInterface, ActionListener {

  EditorTextPane editor;
  private JButton closeButton;
  private JButton loadButton;
  private JButton topButton;
  private JButton runButton;
  private JButton pauseButton;
  private JButton stepButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton resumeButton;
  private JButton stateButton;
  private JButton consoleButton;
  JmolViewer viewer;

  /*
   * methods sendeditorEcho, sendeditorMessage(strStatus), notifyScriptStart(),
   * notifyScriptTermination() are public in case developers want to use
   * ScriptWindow separate from the Jmol application.
   */

  public ScriptEditor() { 
  }

  private JmolConsole jmolConsole;

  public JmolScriptEditorInterface getScriptEditor(JmolViewer viewer, Object frame, Object jmolConsole) {
    return new ScriptEditor(viewer, null, (JmolConsole) jmolConsole);
  }
  
  protected String title;
  protected  String parsedData = "";
  protected ScriptContext parsedContext;
  
  protected SimpleAttributeSet attHighlight;
  protected SimpleAttributeSet attEcho;
  protected SimpleAttributeSet attError;

  public ScriptEditor(JmolViewer viewer, JFrame frame, JmolConsole jmolConsole) {
    super(frame, null, false);
    setAttributes();
    setTitle(title = GT._("Jmol Script Editor"));
    this.viewer = viewer;
    this.jmolConsole = jmolConsole;
    layoutWindow(getContentPane());
    setSize(745, 400);
    if (frame != null)
      setLocationRelativeTo(frame);
  }

  void setAttributes() {
    attHighlight = new SimpleAttributeSet();
    StyleConstants.setBackground(attHighlight, Color.LIGHT_GRAY);
    StyleConstants.setForeground(attHighlight, Color.blue);
    StyleConstants.setBold(attHighlight, true);

    attEcho = new SimpleAttributeSet();
    StyleConstants.setForeground(attEcho, Color.blue);
    StyleConstants.setBold(attEcho, true);

    attError = new SimpleAttributeSet();
    StyleConstants.setForeground(attError, Color.red);
    StyleConstants.setBold(attError, true);

  }
  void layoutWindow(Container container) {
    editor = new EditorTextPane(this);
    editor.setDragEnabled(true);
    JScrollPane editorPane = new JScrollPane(editor);

    JPanel buttonPanel = new JPanel();

    consoleButton = new JButton(GT._("Console"));
    consoleButton.addActionListener(this);
    buttonPanel.add(consoleButton);

    stateButton = new JButton(GT._("State"));
    stateButton.addActionListener(this);
    buttonPanel.add(stateButton);

    loadButton = new JButton(GT._("Script"));
    loadButton.addActionListener(this);
    buttonPanel.add(loadButton);
    
    
    topButton = new JButton(GT._("Check"));
    topButton.addActionListener(this);
    buttonPanel.add(topButton);

    runButton = new JButton(GT._("Run"));
    runButton.addActionListener(this);
    buttonPanel.add(runButton);

    pauseButton = new JButton(GT._("Pause"));
    pauseButton.addActionListener(this);
    buttonPanel.add(pauseButton);
    pauseButton.setEnabled(true);

    stepButton = new JButton(GT._("Step"));
    stepButton.addActionListener(this);
    buttonPanel.add(stepButton);

    resumeButton = new JButton(GT._("Resume"));
    resumeButton.addActionListener(this);
    buttonPanel.add(resumeButton);
    resumeButton.setEnabled(false);

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

  public void setVisible(boolean b) {
    super.setVisible(b);
    viewer.getProperty("DATA_API", "scriptEditorState", b ? Boolean.TRUE : Boolean.FALSE);
    editor.grabFocus();
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
    super.dispose();
  }

  /*
   * (non-Javadoc)
   * @see org.jmol.api.JmolScriptEditorInterface#notifyContext(org.jmol.viewer.ScriptContext, java.lang.Object[])
   * 
   * from org.jmol.viewer.StatusManager:
   * 
      if (isScriptCompletion && viewer.getMessageStyleChime()
          && viewer.getDebugScript()) {
        jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT,
            new Object[] { null, "script <exiting>", statusMessage,
                new Integer(-1), strErrorMessageUntranslated });
        strStatus = "Jmol script completed.";
      }
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT,
          new Object[] { sJmol, strStatus, statusMessage,
              new Integer(isScriptCompletion ? -1 : msWalltime),
              strErrorMessageUntranslated });
   */
  
  public void notifyContext(ScriptContext context, Object[] data) {
    haltButton.setEnabled(context.errorMessage == null);
    pauseButton.setEnabled(context.errorMessage == null);
    resumeButton.setEnabled(false);
    if (context.errorMessage == null)
      setContext(context); 
  }

  String filename;
  
  private synchronized void setContext(ScriptContext scriptContext) {
    if (scriptContext.script.indexOf(JmolConstants.SCRIPT_EDITOR_IGNORE) >= 0)
      return;
    ScriptContext context = parsedContext = scriptContext;
    String s = context.script;
    filename = context.filename;
    if (filename == null && context.functionName != null)
      filename = "function " + context.functionName; 
    editor.clearContent(s);
    boolean isPaused = context.executionPaused || context.executionStepping;
    System.out.println(context.executionPaused + " "+ context.executionStepping);
    resumeButton.setEnabled(isPaused);
    //System.out.println(context.pc + " " + context.executionPaused + " " + context.executionStepping);
    gotoCommand(context.pc + (context.executionPaused ? 0 : 1), isPaused, attHighlight);
  }
  
  private void gotoCommand(int pt, boolean isPaused, SimpleAttributeSet attr) {    
    ScriptContext context = parsedContext;
    try {
      try {
        setVisible(true);
        int pt2;
        int pt1;
        if (pt < 0) {
          pt1 = 0;
          pt2 = editor.getDocument().getLength();
        } else if (context == null || context.aatoken == null) {
          pt1 = pt2 = 0;
        } else if (pt < context.aatoken.length) {
          pt1 = context.lineIndices[pt][0];
          pt2 = context.lineIndices[pt][1];
          //System.out.println ("cursor set to " + pt + " ispaused " + isPaused + " " + context.pc + " " + context.lineIndices.length);
        } else {
          pt1 = pt2 = editor.getDocument().getLength();
        }
        if (isPaused) {
          editor.setCaretPosition(pt1);
          editor.editorDoc.doHighlight(pt1, pt2, attr);
        }
        //editor.grabFocus();
      } catch (Exception e) {
        editor.setCaretPosition(0);
        // do we care?
      }
    } catch (Error er) {
      // no. We don't.
    }    
  }

  public void actionPerformed(ActionEvent e) {
    checkAction(e);
  }
  
  private synchronized void checkAction(ActionEvent e) {
    Object source = e.getSource();
    if (source == consoleButton) {
      jmolConsole.setVisible(true);
      return;
    }
    if (source == closeButton) {
      setVisible(false);
      return;
    }
    if (source == loadButton) {
      setContext((ScriptContext) viewer.getProperty("DATA_API",
          "scriptContext", null));
      return;
    }
    if (source == topButton) {
      gotoTop();
      return;
    }
    if (source == runButton) {
      notifyScriptStart();
      jmolConsole.execute(editor.getText() + "\0##");
      return;
    }
    if (source == pauseButton) {
      jmolConsole.execute("!pause\0##");
      return;
    }
    if (source == resumeButton) {
      jmolConsole.execute("!resume\0##");
      return;
    }
    if (source == stepButton) {
      doStep();
      return;
    }
    if (source == clearButton) {
      editor.clearContent();
      return;
    }
    if (source == stateButton) {
      editor.clearContent(viewer.getStateInfo());
      return;
    }
    if (source == haltButton) {
      viewer.haltScriptExecution();
      return;
    }

  }
 
  private void gotoTop() {
    editor.setCaretPosition(0);
    editor.grabFocus();
    gotoPosition(0, 0);
    parseScript(editor.getText());
  }

  protected void parseScript(String text) {
    if (text == null || text.length() == 0) {
      parsedContext = null;
      parsedData = "";
      setTitle(title);
      return;
    }
    if (text.equals(parsedData) && parsedContext != null)
      return;
    parsedData = text;
    parsedContext = (ScriptContext) viewer.getProperty("DATA_API","scriptCheck", text);
    setTitle(title + (filename == null ? "" : "[" + filename + "]") 
        + " -- " + (parsedContext.aatoken == null ? "" : parsedContext.aatoken.length + " commands ") 
        + (parsedContext.iCommandError < 0 ? "" : " ERROR: " + parsedContext.errorType));
    boolean isError = (parsedContext.iCommandError >= 0);
    gotoCommand(isError ? parsedContext.iCommandError : 0, true, isError ? attError : attHighlight);
  }

  private void doStep() {
    boolean isPaused = viewer.getBooleanProperty("executionPaused");
    jmolConsole.execute(isPaused ? "!step\0##" 
        : editor.getText() + "\0##SCRIPT_STEP\n##SCRIPT_START=" +  editor.getCaretPosition());
  }

  private void gotoPosition(int i, int j) {
    editor.scrollRectToVisible(new Rectangle(i, j));
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
      filename = null;
      clearContent(null);
    }

    public synchronized void clearContent(String text) {
      editorDoc.outputEcho(text);
      parseScript(text);
    }
    
    protected void processKeyEvent(KeyEvent ke)
    {
       // Id Control key is down, captures events does command
       // history recall and inhibits caret vertical shift.

      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      if (kcode == KeyEvent.VK_Z
          && kid == KeyEvent.KEY_PRESSED
          && ke.isControlDown()) {
          editor.editorDoc.undo();
       } else if (kcode == KeyEvent.VK_Z
           && kid == KeyEvent.KEY_PRESSED
           && ke.isShiftDown() && ke.isControlDown()
         || kcode == KeyEvent.VK_Y
         && kid == KeyEvent.KEY_PRESSED
         && ke.isControlDown()) {
           editor.editorDoc.redo();
       } else {
         super.processKeyEvent(ke);
       }
    }
  }

  class EditorDocument extends DefaultStyledDocument {

    EditorTextPane EditorTextPane;

    EditorDocument() {
      super();
      putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
      addUndoableEditListener(new MyUndoableEditListener());
    }

    void setEditorTextPane(EditorTextPane EditorTextPane) {
      this.EditorTextPane = EditorTextPane;
    }

    void doHighlight(int from, int to, SimpleAttributeSet attr) {
      setCharacterAttributes(0, editor.editorDoc.getLength(), attEcho, true);
      if (from >= to)
        return;
      setCharacterAttributes(from, to - from, attr, true);
      editor.select(from, to);
      editor.setSelectedTextColor(attr == attError ? Color.RED : Color.black);

    }

    protected UndoManager undo = new UndoManager();

    protected class MyUndoableEditListener implements UndoableEditListener {
      public void undoableEditHappened(UndoableEditEvent e) {
        // Remember the edit and update the menus
        undo.addEdit(e.getEdit());
        // undoAction.updateUndoState();
        // redoAction.updateRedoState();
      }
    }  

    protected void undo() {
      try {
        undo.undo();
      } catch (Exception e) {
        //
      }
    }
    
    protected void redo() {
      try {
        undo.redo();
      } catch (Exception e) {
        //
      }
    }
    
    /**
     * Removes all content of the script window, and add a new prompt.
     */
    void clearContent() {
      try {
        super.remove(0, getLength());
      } catch (Exception exception) {
        //Logger.error("Could not clear script window content", exception);
      }
    }

    void outputEcho(String text) {
      clearContent();
      if (text == null)
        return;
      try {
        super.insertString(0, text, attEcho);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }
}
