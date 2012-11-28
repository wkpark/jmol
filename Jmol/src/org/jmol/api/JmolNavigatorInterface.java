package org.jmol.api;

import org.jmol.util.Point3f;
import org.jmol.util.Vector3f;
import org.jmol.viewer.TransformManager11;
import org.jmol.viewer.Viewer;

public interface JmolNavigatorInterface {

  void set(TransformManager11 transformManager11, Viewer viewer);

  void navigateTo(float floatSecondsTotal, Vector3f axis, float degrees,
                  Point3f center, float depthPercent, float xTrans, float yTrans);

  void navigate(float seconds, Point3f[][] pathGuide, Point3f[] path,
                float[] theta, int indexStart, int indexEnd);

  void zoomByFactor(float factor, int x, int y);

  void calcNavigationPoint();

  void setNavigationOffsetRelative(boolean navigatingSurface);

  void navigate(int keyCode, int modifiers);

  void setNavigationDepthPercent(float timeSec, float percent);

  void navTranslatePercent(float seconds, float x, float y);

  String getNavigationState();

}
