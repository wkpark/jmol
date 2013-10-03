package org.jmol.api;

public interface EventManager {

  void keyPressed(int keyCode, int modifiers);

  void keyReleased(int keyCode);

  void mouseEnterExit(long time, int x, int y, boolean isExit);
  
  void mouseAction(int mode, long time, int x, int y, int count,
                   int buttonMods);

}
