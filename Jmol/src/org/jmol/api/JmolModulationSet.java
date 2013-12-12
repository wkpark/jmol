package org.jmol.api;

import javajs.util.T3;

public interface JmolModulationSet {

  Object getModulation(String type, T3 t456);

  String getState();

  boolean isEnabled();

  void setModTQ(T3 a, boolean isOn, T3 qtOffset, boolean isQ, float scale, SymmetryInterface uc);

  float getScale();

  void addTo(T3 a, float scale);

}
