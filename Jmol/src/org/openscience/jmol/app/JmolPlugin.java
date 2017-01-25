package org.openscience.jmol.app;

import javax.swing.JFrame;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

public interface JmolPlugin { 
  void start(JFrame frame, Viewer viewer);
  void destroy();
  String getVersion();
  String getName();
  void setVisible(boolean b);
  void notifyCallback(CBK type, Object[] data);
}
