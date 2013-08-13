package org.jmol.api;

import org.jmol.modelset.ModelSet;
import org.jmol.util.GData;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolRepaintManager {

  void set(Viewer viewer, ShapeManager shapeManager);

  boolean isRepaintPending();

  void popHoldRepaint(boolean andRepaint, String why);

  boolean repaintIfReady(String why);

  void pushHoldRepaint(String why);

  void repaintDone();

  void requestRepaintAndWait(String why);

  void clear(int iShape);

  void render(GData gdata, ModelSet modelSet, boolean isFirstPass, int[] minMax);

  String renderExport(String type, GData gdata, ModelSet modelSet,
                      String fileName);

}
