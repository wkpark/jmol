package org.jmol.api;

import org.jmol.applet.Jvm12;

public interface JmolAppletConsoleInterface {

  void setVisible(boolean b);

  void output(String message);

  String getText();

  void dispose();

  Object getMyMenuBar();

  void set(JmolViewer viewer, Jvm12 jvm12);

}
