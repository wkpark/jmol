package org.jmol.api;

import org.jmol.atomdata.RadiusData;
import org.jmol.util.BS;
import org.jmol.util.P3;
import org.jmol.viewer.Viewer;

public interface JmolEnvCalc {

  JmolEnvCalc set(Viewer viewer, int atomCount, short[] mads);

  P3[] getPoints();

  BS getBsSurfaceClone();

  void calculate(RadiusData rd, float maxRadius, BS bsSelected,
                 BS bsIgnore, boolean disregardNeighbors,
                 boolean onlySelectedDots, boolean isSurface,
                 boolean multiModel);
}
