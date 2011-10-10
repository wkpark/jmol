package org.openscience.jmolandroid.api;

import org.jmol.api.JmolViewer;

import android.app.Dialog;
import android.util.Log;
import android.view.SurfaceView;

public class AndroidUpdateListener {

	private JmolViewer viewer;
	private AndroidSurface surface;
	private SurfaceView imageView;
	private byte counter;
	private Dialog dialog;
	
	public AndroidUpdateListener(JmolViewer viewer, SurfaceView imageView) {
		this.imageView = imageView;
		this.viewer = viewer;

		this.surface = new AndroidSurface();
	}
		
	@Override
	public void imageUpdated() {
		updateCanvas();
		
    	if (dialog != null && --counter <= 0) {
    		dialog.dismiss();
    		dialog = null;
    	}
	}

	public void updateCanvas() {
		long start = System.currentTimeMillis();
		
		synchronized (imageView) {
			try {
				surface.canvas = imageView.getHolder().lockCanvas();
				
				if (surface.canvas != null)
					viewer.renderScreenImage(surface, null, viewer.getScreenWidth(), viewer.getScreenHeight(), null);
				else
					Log.w("AMOL", "Unable to lock the canvas");    					
			} finally {
				if (surface.canvas != null)
					imageView.getHolder().unlockCanvasAndPost(surface.canvas);
				
				surface.canvas = null;
				
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
