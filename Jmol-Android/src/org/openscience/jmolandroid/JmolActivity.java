package org.openscience.jmolandroid;

import java.io.File;
import java.util.Map;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.Event;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.openscience.jmolandroid.api.AndroidUpdateListener;
import org.openscience.jmolandroid.search.Downloader;
import org.openscience.jmolandroid.search.PDBSearchActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class JmolActivity extends Activity implements JmolStatusListener {
	
	private JmolViewer viewer;
	
	private SurfaceView imageView;
  public SurfaceView getImageView() {
    return imageView;
  }

  private AndroidUpdateListener updateListener;
	private boolean opening;
  public static float SCALE_FACTOR;
	
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    viewer = null;
    SCALE_FACTOR = getResources().getDisplayMetrics().density + 0.5f;
  }

    @Override 
    protected void onDestroy() {
    	super.onDestroy();
        
// unnec.    	viewer.zap(true, true, false);
    	viewer.releaseScreenImage();

    	viewer = null;
    	imageView = null;
    	updateListener = null;

    	System.exit(0);
    };
    
  @Override
  protected void onResume() {
    super.onResume();
    
    Log.w("AMOL","onResume...");
    // am I correct that imageView is null if and only if viewer is null?
    // otherwise we could have a different imageView in updateListener than here

    if (imageView == null)
      imageView = (SurfaceView) findViewById(R.id.imageMolecule);

    if (viewer == null) {
      updateListener = new AndroidUpdateListener(this);
      // bit of a chicken and an egg here, but 
      // we pass the updateListener to viewer, where it will be called
      // the "display" and then Platform will get a call asking for an update.
      // not sure about the rest of it!
      viewer = JmolViewer
          .allocateViewer(updateListener, new SmarterJmolAdapter(), null, null, null,
              "platform=org.openscience.jmolandroid.api.Platform", this);
      viewer.script("load http://chemapps.stolaf.edu/jmol/docs/examples-12/data/caffeine.xyz");
    }

    Log.w("AMOL","onResume... viewer=" + viewer);
    Log.w("AMOL","onResume... opening " + opening);
    if (viewer.getAtomCount() > 0
        && !updateListener.isShowingDialog()) {
      imageView.post(new Runnable() {
        @Override
        public void run() {
          updateListener.updateCanvas();
        }
      });
    } else {
      //        	this.setTitle(R.string.app_name);
      if (!opening) {
        imageView.post(new Runnable() {
          @Override
          public void run() {
            openOptionsMenu();
          }
        });
      }
      opening = false;
    }

  };
    
    @Override
    public void onBackPressed() {
    	this.finish();
    };
    
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
      Log.w("AMOL","onTouchEvent " + event);
    	switch (event.getAction()) {
    		case MotionEvent.ACTION_DOWN:
    			new AsyncTask<MotionEvent, Void, Void>(){
					@Override
					protected Void doInBackground(MotionEvent... event) {
			    		viewer.mouseEvent(Event.MOUSE_DOWN, (int)event[0].getX(), (int)event[0].getY(), Event.MOUSE_LEFT, event[0].getDownTime());
			    		return null;
					}}.execute(event);
    	    	break;
    		case MotionEvent.ACTION_UP:
    			new AsyncTask<MotionEvent, Object, Object>(){
					@Override
					protected Void doInBackground(MotionEvent... event) {
			    		viewer.mouseEvent(Event.MOUSE_UP, (int)event[0].getX(), (int)event[0].getY(), Event.MOUSE_LEFT, event[0].getDownTime());
			    		return null;
					}}.execute(event);
	    		break;
    		case MotionEvent.ACTION_MOVE:
    			new AsyncTask<MotionEvent, Object, Object>(){
					@Override
					protected Void doInBackground(MotionEvent... event) {
			    		viewer.mouseEvent(Event.MOUSE_DRAG, (int)event[0].getX(), (int)event[0].getY(), Event.MOUSE_LEFT, event[0].getDownTime());
			    		return null;
					}}.execute(event);
	    		break;
    	}
    	
		return true;
    };
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      Log.w("AMOL","onWindowFocusChanged " + hasFocus);
    	if (!hasFocus) return;
      updateListener.setScreenDimension();
    };
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem styleMenu = menu.findItem(R.id.style);
    	
    	if (styleMenu != null)
    		styleMenu.setEnabled(viewer.getAtomCount() > 0);
    	
    	return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    private static final int REQUEST_OPEN = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.open:
    		File path = Downloader.getAppDir(this);
        	
        	Intent intent = new Intent(this.getBaseContext(), FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, path.getAbsolutePath());
			this.startActivityForResult(intent, REQUEST_OPEN);
            break;
        case R.id.download:
        	Intent dnIntent = new Intent(this.getBaseContext(), PDBSearchActivity.class);
			this.startActivityForResult(dnIntent, REQUEST_OPEN);
        	break;
		case R.id.cpkspacefill:
			viewer.evalString("restrict bonds not selected;select not selected;spacefill 100%;color cpk");
			break;
		case R.id.ballandstick:
			viewer.evalString("restrict bonds not selected;select not selected;spacefill 23%AUTO;wireframe 0.15;color cpk");
			break;
		case R.id.sticks:
			viewer.evalString("restrict bonds not selected;select not selected;wireframe 0.3;color cpk");
			break;
		case R.id.wireframe:
			viewer.evalString("restrict bonds not selected;select not selected;wireframe on;color cpk");
			break;
		case R.id.cartoon:
			viewer.evalString("restrict bonds not selected;select not selected;cartoons on;color structure");
			break;
		case R.id.trace:
			viewer.evalString("restrict bonds not selected;select not selected;trace on;color structure");
			break;
        default:
            return super.onOptionsItemSelected(item);
        }
        
        return true;
    }
    
    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
            if (resultCode == Activity.RESULT_OK) {

                    if (requestCode == REQUEST_OPEN) {
                    	updateListener.manageDialog(ProgressDialog.show(this, "", "Opening file...", true), (byte)2);
                        viewer.openFileAsynchronously(data.getStringExtra(FileDialog.RESULT_PATH));
                        opening = true;
                    }
            }
    }

    @Override
    public String createImage(String fileName, String type, Object textOrBytes,
                              int quality) {
      // ignore
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String eval(String strEval) {
      // ignore
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public float[][] functionXY(String functionName, int x, int y) {
      // ignore
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public float[][][] functionXYZ(String functionName, int nx, int ny, int nz) {
      // ignore
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Map<String, Object> getRegistryInfo() {
      // ignore
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void resizeInnerPanel(String data) {
      // ignore
      // TODO Auto-generated method stub
      
    }

    @Override
    public void showUrl(String url) {
      // ignore
      // TODO Auto-generated method stub      
    }

    @Override
    public void notifyCallback(EnumCallback message, Object[] data) {
      // probably ignore
      // TODO Auto-generated method stub
      
    }

    @Override
    public boolean notifyEnabled(EnumCallback type) {
      // probably ignore
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void setCallbackFunction(String callbackType, String callbackFunction) {
      // ignore -- applet only
      // TODO Auto-generated method stub
      
    }

}