package org.jmol.thread;

public class JmolThread extends Thread {
  
  public String name = "JmolThread";
  protected boolean interrupted = false;
  
  protected void setMyName(String name) {
    this.name = name;
    super.setName(name);
  }

  @Override
  public synchronized void start() {
    super.start();
  }

  @Override
  public void interrupt() {
    interrupted = true;
    super.interrupt();
  }
  

}
