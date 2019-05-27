package org.jmol.api;



import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public interface JmolNavigatorInterface extends Runnable {

  void set(TransformManager transformManager, Viewer vwr);

  void zoomByFactor(float factor, int x, int y);

  void calcNavigationPoint();

  void setNavigationOffsetRelative();//boolean navigatingSurface);

  void navigateKey(int keyCode, int modifiers);

  void navigateList(JmolScriptEvaluator eval, Lst<Object[]> list);

  void navigateAxis(V3 rotAxis, float degrees);

  void setNavigationDepthPercent(float percent);

  String getNavigationState();

  void navTranslatePercentOrTo(float seconds, float x, float y);

  void interrupt();


}
