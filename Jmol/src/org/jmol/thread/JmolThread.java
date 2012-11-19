package org.jmol.thread;

import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptEvaluator;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

abstract public class JmolThread extends Thread {
  
  public String name = "JmolThread";
  
  private static int threadIndex;
  
  protected static final int INIT = -1;
  protected static final int MAIN = 0;
  protected static final int FINISH = -2;
  protected static final int CHECK1 = 1;
  protected static final int CHECK2 = 2;
  protected static final int CHECK3 = 3;
  
  protected Viewer viewer;
  protected ScriptEvaluator eval;
  protected ScriptContext sc;

  protected boolean hoverEnabled;

  protected long startTime;
  protected long targetTime;
  protected long lastRepaintTime;
  protected long currentTime;
  protected int sleepTime;
  
  protected boolean isJS;
  protected boolean interrupted = false;
  protected boolean isReset;
  protected boolean continuing = true;

 
  public JmolThread(Viewer viewer, String name) {
    super(name);
    this.viewer = viewer;
    hoverEnabled = viewer.isHoverEnabled();
    isJS = viewer.isSingleThreaded();
    this.name = name + "_" + (++threadIndex);    
  }
  abstract protected void run1(int mode) throws InterruptedException;
  abstract protected boolean checkContinue();

  /**
   * JavaScript only --
   * -- scriptDelay, moveTo, spin
   * -- save context for restoration later
   * -- move program counter forward one command
   * 
   * @param eval
   */
  public void setEval(ScriptEvaluator eval) {
    this.eval = eval;
    sc = null;
    if (eval == null || !isJS)
      return;
    eval.scriptLevel--;
    eval.pushContext2(null);
    sc = eval.thisContext;
    sc.pc++;
  }

  protected void resumeEval() {
    if (eval == null || !isJS)
      return;
    eval.resumeEval(sc,false);
    eval = null;
    sc = null;
  }
  
  protected void restartHover() {
    if (hoverEnabled)
      viewer.startHoverWatcher(true);
  }

  @Override
  public synchronized void start() {
    if (isJS) {
      Logger.info("starting " + name);
      run();
    } else {
      super.start();
    }
  }

  @Override
  public void interrupt() {
    interrupted = true;
    restartHover();
    if (!isJS)
      super.interrupt();
  }
  
  protected boolean checkInterrupted() {
    /**
     * @j2sNative
     * 
     * return this.interrupted;
     */
    {
      return super.isInterrupted();
    }
  }
  
  public void reset() {
    isReset = true;
    interrupt();
  }

  @Override
  public void run() {
    startTime = System.currentTimeMillis();
    try {
      run1(INIT);
      while (checkContinue()) {
        run1(MAIN);
        if (isJS)
          return;
      }
    } catch (InterruptedException e) {
      if (Logger.debugging)
        oops(e);
    } catch (Exception e) {
      oops(e);
    }
    try {
      run1(FINISH);
    } catch (Exception e) {
      Logger.info(name + " exception in FINISH " + e);
    }
  }
  
  protected void oops(Exception e) {
    System.out.println(name + " exception " + e);
  }

  /**
   * 
   * @param millis  
   * @param runPtr
   * @return true if we can continue on with this thread (Java, not JavaScript)
   * @throws InterruptedException 
   *  
   */
  protected boolean runSleep(int millis, int runPtr) throws InterruptedException {
   /**
    * @j2sNative
    * 
    * var me = this;
    * setTimeout(function(){me.run1(runPtr)}, Math.max(millis, 0));
    * return false;
    *  
    */
    {
      if (millis > 0)
        Thread.sleep(millis);
      return true;
    }
  }
  
  public void discontinue() {
    continuing = false;
  }

}
