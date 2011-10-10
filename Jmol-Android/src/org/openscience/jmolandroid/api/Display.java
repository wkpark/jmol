package org.openscience.jmolandroid.api;

import org.jmol.api.JmolViewer;

/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to org.jmol.awt
 * 
 */

class Display {

  static boolean hasFocus(Object display) {
    return true;
  }

  static void requestFocusInWindow(Object display) {
    return;
  }

  static void repaint(Object display) {
    ((AndroidUpdateListener) display).repaint();
  }

  /**
   * legacy apps will use this
   * 
   * @param viewer
   * @param g
   * @param size
   */
  static void renderScreenImage(JmolViewer viewer, Object g, Object size) {
    // ignored
  }

  static void setTransparentCursor(Object display) {
    // ignored
  }

  static void setCursor(int c, Object display) {
    // ignored
  }


}
