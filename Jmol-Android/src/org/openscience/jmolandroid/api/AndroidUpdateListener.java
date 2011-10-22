package org.openscience.jmolandroid.api;

import org.jmol.api.JmolViewer;
import org.openscience.jmolandroid.JmolActivity;

public class AndroidUpdateListener {

  private JmolViewer viewer;
  private JmolActivity ja;

  public AndroidUpdateListener(JmolActivity ja) {
    this.ja = ja;
  }

  void setViewer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void getScreenDimensions(int[] widthHeight) {
    widthHeight[0] = ja.getImageView().getWidth();
    widthHeight[1] = ja.getImageView().getHeight();
  }
  
  public void setScreenDimension() {
    int width = ja.getImageView().getWidth();
    int height = ja.getImageView().getHeight();
    if (viewer.getScreenWidth() != width || viewer.getScreenHeight() != height)
      viewer.setScreenDimension(width, height);
  }

  public void repaint() {
    // from Viewer
    ja.repaint();
  }

  public void mouseEvent(int id, int x, int y, int modifiers, long when) {
    viewer.mouseEvent(id, x, y, modifiers, when);
  }

}

