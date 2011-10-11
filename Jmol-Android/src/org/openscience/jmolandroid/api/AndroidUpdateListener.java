package org.openscience.jmolandroid.api;

import org.jmol.api.JmolViewer;

import android.app.Dialog;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceView;

public class AndroidUpdateListener {

	private JmolViewer viewer;
	private SurfaceView imageView;
	private byte counter;
	private Dialog dialog;
	
	public AndroidUpdateListener() {
	}
		
  public void set(JmolViewer viewer, SurfaceView imageView) {
    this.imageView = imageView;
    this.viewer = viewer;
  }
  
	public void repaint() {
		updateCanvas();
		
    	if (dialog != null && --counter <= 0) {
    		dialog.dismiss();
    		dialog = null;
    	}
	}

	public void updateCanvas() {
		long start = System.currentTimeMillis();
		
		synchronized (imageView) {
		  Canvas canvas = null;
			try {
				canvas = imageView.getHolder().lockCanvas();
				
				if (canvas != null)
					viewer.renderScreenImage(canvas, null, viewer.getScreenWidth(), viewer.getScreenHeight());
				else
					Log.w("AMOL", "Unable to lock the canvas");    					
			} finally {
				if (canvas != null)
					imageView.getHolder().unlockCanvasAndPost(canvas);
				
				canvas = null;
				
				Log.d("AMOL", "Image updated in " + (System.currentTimeMillis() - start));    					
			}
		}
	}
	
	public void manageDialog(Dialog dialog, byte value) {
		this.dialog = dialog;
		counter = value;
	}
	
	public boolean isShowingDialog() {
		return dialog != null;
	}

}
