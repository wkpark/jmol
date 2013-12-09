package org.jmol.api;

import javajs.util.P3;
import javajs.util.V3;

public interface JmolModulationSet {

  boolean isEnabled();

  String getState();

  int setModT(boolean isOn, int t);

  void getModulation(float t, P3 pt);
  
  V3 getPrevSetting();

  Object getModulationData(String type, float t);

}
