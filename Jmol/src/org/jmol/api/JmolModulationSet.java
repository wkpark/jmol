package org.jmol.api;

import javajs.util.V3;

public interface JmolModulationSet {

  boolean isEnabled();

  String getState();

  int setModT(boolean isOn, int t);

  V3 getPrevSetting();

}
