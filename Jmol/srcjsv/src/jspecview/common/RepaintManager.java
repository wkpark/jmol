package jspecview.common;

import javax.swing.JPanel;

public class RepaintManager {

	public RepaintManager(ScriptInterface si) {
		this.si = si;
	}
  /////////// thread management ///////////
  
//  private int holdRepaint = 0;
  boolean repaintPending;
	private ScriptInterface si;

//  public void pushHoldRepaint() {
//    ++holdRepaint;
//  }
//
//  public void popHoldRepaint(boolean andRepaint) {
//    --holdRepaint;
//    if (holdRepaint <= 0) {
//      holdRepaint = 0;
//      if (andRepaint) {
//        repaintPending = true;
//        ((JPanel) si.getSelectedPanel()).repaint();
//      }
//    }
//  }

//  private boolean repaintFailed;
  
  private int n;
  public boolean refresh() {
  	n++;
    if (repaintPending) {
    	System.out.println("Repaint " + n + " skipped");
 //   	repaintFailed = true;
      return false;
    }
    repaintPending = true;
//    repaintFailed = false;
//    if (holdRepaint == 0) {
    	((JPanel) si.getSelectedPanel()).repaint();//repaint(100, 100, 1, 1);
//    }
    return true;
  }

  synchronized public void repaintDone() {
    repaintPending = false;
 //   if (repaintFailed) {
    	//refresh();
 //   } else {
      notify(); // to cancel any wait in requestRepaintAndWait()
//    }
  }

//  synchronized public void requestRepaintAndWait() {
//  	((JPanel) si.getSelectedPanel()).repaint();
//    try {
//     wait(1000);  // more than a second probably means we are locked up here
//      if (repaintPending) {
//        repaintDone();
//      }
//    } catch (InterruptedException e) {
//    }
//  }

}
