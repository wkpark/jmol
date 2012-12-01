package org.jmol.api;

import java.util.List;

import org.jmol.script.ScriptEvaluator;
import org.jmol.util.Point3f;
import org.jmol.util.Vector3f;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public interface JmolNavigatorInterface extends Runnable {

  void set(TransformManager transformManager, Viewer viewer);

  void navigateTo(float floatSecondsTotal, Vector3f axis, float degrees,
                  Point3f center, float depthPercent, float xTrans, float yTrans);

  void navigate(float seconds, Point3f[][] pathGuide, Point3f[] path,
                float[] theta, int indexStart, int indexEnd);

  void zoomByFactor(float factor, int x, int y);

  void calcNavigationPoint();

  void setNavigationOffsetRelative();//boolean navigatingSurface);

  void navigateKey(int keyCode, int modifiers);

  void navigateList(ScriptEvaluator eval, List<Object[]> list);

  void navigateAxis(Vector3f rotAxis, float degrees);

  void setNavigationDepthPercent(float percent);

  String getNavigationState();

  void navTranslatePercent(float seconds, float x, float y);

  void interrupt();


}
