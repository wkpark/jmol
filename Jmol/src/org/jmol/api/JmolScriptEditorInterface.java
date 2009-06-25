package org.jmol.api;

public interface JmolScriptEditorInterface {

  void setVisible(boolean b);

  void output(String message);

  String getText();

  void dispose();
  
  JmolScriptEditorInterface getScriptEditor(JmolViewer viewer, Object frame);

}
