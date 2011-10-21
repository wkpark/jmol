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
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;

public class JmolActivity extends Activity implements JmolStatusListener {

  /*
   * General signal flow -- two cases:
   * 
   * I. System calls onDraw(Canvas) for whatever reason.
   *        Jmol draws onto it
   * 
   * II. Jmol gets a user event or script completion or DELAY command
   *     and needs an update
   *        Jmol triggers imageView.postInvalidate()
   *        System calls onDraw(Canvas)
   * 
   * 
   */
  
  
  private final static String TEST_SCRIPT = ";load http://chemapps.stolaf.edu/jmol/docs/examples-12/data/caffeine.xyz;";
  //labels on; background labels white;spacefill on";

  private final static String STARTUP_SCRIPT = "set allowGestures TRUE;unbind ALL _slidezoom;"
      + TEST_SCRIPT;

  public static float SCALE_FACTOR;

  class JmolImageView extends SurfaceView {

    public JmolImageView(Context context) {
      super(context);
      // TODO Auto-generated constructor stub
      Log.w("Jmol", "JmolImageView " + this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      //super.onDraw(canvas);
      Log.w("Jmol", "JmolActivity onDraw");
      viewer.renderScreenImage(canvas, null, canvas.getWidth(), canvas.getHeight());
    }
  }

  
  private JmolViewer viewer;
  private JmolImageView imageView;
  private AndroidUpdateListener updateListener;
  private boolean opening;
  private ScaleGestureDetector scaleDetector;

  public SurfaceView getImageView() {
    return imageView;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(imageView = new JmolImageView(getBaseContext()));
    imageView.setWillNotDraw(false);
    viewer = null;
    SCALE_FACTOR = getResources().getDisplayMetrics().density + 0.5f;

    scaleDetector = new ScaleGestureDetector(this, new PinchZoomScaleListener());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    viewer.releaseScreenImage();

    viewer = null;
    imageView = null;
    updateListener = null;

    System.exit(0);
  };

  @Override
  protected void onPause() {
    super.onPause();
    updateListener.setPaused(true);    
  }
  
  @Override
  protected void onResume() {
    super.onResume();

    Log.w("Jmol", "onResume..." + viewer);
    // am I correct that imageView is null if and only if viewer is null?
    // otherwise we could have a different imageView in updateListener than here

//    if (imageView == null) {
//      imageView = (SurfaceView) findViewById(R.id.imageMolecule);
//    }

    if (viewer == null) {
      updateListener = new AndroidUpdateListener(this);
      // bit of a chicken and an egg here, but 
      // we pass the updateListener to viewer, where it will be called
      // the "display" and then Platform will get a call asking for an update.
      // not sure about the rest of it!
      viewer = JmolViewer.allocateViewer(updateListener,
          new SmarterJmolAdapter(), null, null, null,
          "platform=org.openscience.jmolandroid.api.Platform", this);
      viewer.script(STARTUP_SCRIPT);
      return;
    }

    Log.w("Jmol", "onResume... viewer=" + viewer + " opening=" + opening);
    updateListener.setPaused(false);    
    if (viewer.getAtomCount() > 0) {// && !updateListener.isShowingDialog()) {
      imageView.invalidate();
    } else {
      //        	setTitle(R.string.app_name);
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
    Log.w("Jmol", "onResume... done");

  };

  // TODO Auto-generated method stub

  @Override
  public void onBackPressed() {
    finish();
  };

  public void dismissDialog() {
    // TODO Auto-generated method stub
    if (pd == null)
      return;
    pd.dismiss();
    pd = null;
  }

  public void repaint() {
    Log.w("Jmol", "JmolActivity repaint " + imageView);
    dismissDialog();
    if (imageView != null)
      imageView.postInvalidate();
  }

  @Override
  public boolean onTouchEvent(final MotionEvent event) {
    final int index = event.findPointerIndex(0);
    if (index < 0 || updateListener == null)
      return true;
    Log.w("Jmol","onTouchEvent " + index + " " + event);
    int e = Integer.MIN_VALUE;
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      e = Event.MOUSE_DOWN;
      break;
    case MotionEvent.ACTION_MOVE:
      e = Event.MOUSE_DRAG;
      break;
    case MotionEvent.ACTION_UP:
      e = Event.MOUSE_UP;
      break;
    }
    
   scaleDetector.onTouchEvent(event);
    
    if (e != Integer.MIN_VALUE)
      updateListener.mouseEvent(e, (int) event.getX(index),
          (int) event.getY(index), Event.MOUSE_LEFT, event.getEventTime());
    return true;
  };

  private class PinchZoomScaleListener extends
      ScaleGestureDetector.SimpleOnScaleGestureListener {
    
    @Override
    public boolean onScale(final ScaleGestureDetector detector) {
      Log.w("JMOL", "old zoom=" + viewer.getZoomPercentFloat());

      float zoomFactor = detector.getScaleFactor()
          / (viewer.getZoomPercentFloat() / 100.0f);
      viewer.syncScript("Mouse: zoomByFactor " + zoomFactor, "~", 0);
      Log.w("JMOL", "changing zoom factor=" + zoomFactor);

      return true;
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    Log.w("Jmol", "onWindowFocusChanged " + hasFocus);
    if (!hasFocus)
      return;
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

      Intent intent = new Intent(getBaseContext(), FileDialog.class);
      intent.putExtra(FileDialog.START_PATH, path.getAbsolutePath());
      startActivityForResult(intent, REQUEST_OPEN);
      break;
    case R.id.download:
      Intent dnIntent = new Intent(getBaseContext(),
          PDBSearchActivity.class);
      startActivityForResult(dnIntent, REQUEST_OPEN);
      break;
    case R.id.cpkspacefill:
      viewer.script("spacefill only;color cpk");
      break;
    case R.id.ballandstick:
      viewer.script("wireframe -0.15;spacefill 23%;color cpk");
      break;
    case R.id.sticks:
      viewer.script("wireframe -0.3;color cpk");
      break;
    case R.id.wireframe:
      viewer.script("wireframe only;color cpk");
      break;
    case R.id.cartoon:
      viewer.script("cartoons only;color cartoons structure");
      break;
    case R.id.trace:
      viewer.script("trace only;color trace structure");
      break;
    default:
      return super.onOptionsItemSelected(item);
    }

    return true;
  }

  private ProgressDialog pd;
  public synchronized void onActivityResult(final int requestCode,
                                            int resultCode, final Intent data) {
    if (resultCode == Activity.RESULT_OK) {

      if (requestCode == REQUEST_OPEN) {
        
        pd = ProgressDialog.show(this, "",
            "Opening file...", true);
        
        //updateListener.manageDialog(ProgressDialog.show(this, "",
         //   "Opening file...", true), (byte) 2);
        // this is the best way to go -- allows for scripts, surfaces, and models
        viewer.openFileAsynchronously(data
            .getStringExtra(FileDialog.RESULT_PATH));
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
