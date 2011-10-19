package org.openscience.jmolandroid.api;

import org.jmol.api.JmolViewer;
import org.openscience.jmolandroid.JmolActivity;

import android.app.Dialog;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceView;

public class AndroidUpdateListener {

  private JmolViewer viewer;
  private byte counter;
  private Dialog dialog;
  private JmolActivity ja;

  public AndroidUpdateListener(JmolActivity ja) {
    this.ja = ja;
  }

  void setViewer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void setScreenDimension() {
    int width = ja.getImageView().getWidth();
    int height = ja.getImageView().getHeight();
    if (viewer.getScreenWidth() != width || viewer.getScreenHeight() != height)
      viewer.setScreenDimension(width, height);
  }

  public void repaint() {
    // from Viewer
    if (dialog != null && --counter <= 0) {
      dialog.dismiss();
      dialog = null;
    }
    repaintEvent();
  }

  public void repaintEvent() {
    // from onResume
    ja.getImageView().post(new Runnable() {
      @Override
      public void run() {
        updateCanvas();
      }
    });
  }

  private boolean paused;
  private boolean updating;
  
  protected void updateCanvas() {
    Log.w("Jmol","updateCanvas paused/updating " + paused + " " + updating);
    if (paused || updating)
      return;
    SurfaceView imageView = ja.getImageView();
    long start = System.currentTimeMillis();

    synchronized (imageView) {
      updating = true;
      Canvas canvas = null;
      try {
        canvas = imageView.getHolder().lockCanvas();
        canvas.getHeight(); // simple test for canvas not null
      } catch (Exception e) {
        Log.w("Jmol", "Unable to lock the canvas\n");
        //e.printStackTrace();
      }
      if (canvas != null) {
        // at least for now we want to see errors traced to their Jmol methods, not trapped here
        viewer.renderScreenImage(canvas, null, canvas.getWidth(), canvas
            .getHeight());
        imageView.getHolder().unlockCanvasAndPost(canvas);
      }
      updating = false;
    }
    Log.d("Jmol", "Image updated in " + (System.currentTimeMillis() - start)
        + " ms");
    Log.d("Jmol", "Zoom % " + viewer.getZoomPercentFloat());
  }

  public void setPaused(boolean TF) {
    paused = TF;    
  }

  public void manageDialog(Dialog dialog, byte value) {
    this.dialog = dialog;
    counter = value;
  }

  public boolean isShowingDialog() {
    return dialog != null;
  }

}
