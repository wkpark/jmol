package org.openscience.jmol.app;

import java.util.Scanner;

import org.jmol.api.JmolViewer;
import org.jmol.script.ScriptContext;

public class InputScannerThread extends Thread {
 
  private JmolViewer viewer;
  private Scanner scanner;
  private boolean isSilent;

  InputScannerThread(JmolViewer viewer, boolean isSilent) {
    this.viewer = viewer;
    this.isSilent = isSilent;
    start();
  }

  private StringBuilder buffer = new StringBuilder();
  @Override
  public synchronized void start() {
    scanner = new Scanner(System.in);
    scanner.useDelimiter("\n");
    super.start();
  }

  @Override
  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      say(null);
      while (true) {
        Thread.sleep(1);
        if (scanner.hasNext()) {
          String s = scanner.next();
          s = s.substring(0, s.length() - 1);
          if (s.toLowerCase().equals("exitjmol"))
            System.exit(0);
          if (viewer.checkHalt(s, false)) {
            buffer = new StringBuilder();
            s = "";
          }
          buffer.append(s).append('\n');
          if (!checkCommand() && buffer.length() == 1) {
            say(null);
          }
        }
      }
    } catch (InterruptedException e) {
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void say(String msg) {
    if (msg == null)
      msg = "Enter: \nexitJmol to exit Jmol\nquit to stop processing and re-initialize input\n!exit to stop all script processing\nJmol>";
    if (!isSilent)
      System.out.print(msg);
    System.out.flush();
  }

  private boolean checkCommand() {
    String strCommand = buffer.toString();
    if (strCommand.length() == 1 || strCommand.charAt(0) == '!'
        || viewer.isScriptExecuting()
        || viewer.getBooleanProperty("executionPaused"))
      return false;
    Object ret = viewer.scriptCheck(strCommand);
    if (ret instanceof String) {
      say((String) ret);
      return false;
    }
    if (ret instanceof ScriptContext) {
      ScriptContext c = (ScriptContext) ret;
      if (!c.isComplete) {
        return true;
      }
    }
    buffer = new StringBuilder();
    viewer.script(strCommand);
    return true;
  }
}
