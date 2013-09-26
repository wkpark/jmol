package org.jmol.api;

import org.jmol.util.JmolList;
import org.jmol.util.P4;

public interface Triangulator {

  public JmolList<Object> intersectPlane(P4 plane, JmolList<Object> v, int flags);
}
