package org.jmol.util;

import javajs.util.List;
import javajs.util.P4;

public interface Triangulator {

  public List<Object> intersectPlane(P4 plane, List<Object> v, int flags);
}
