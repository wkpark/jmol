package org.jmol.api;

import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;
import java.util.BitSet;

public interface JmolExportInterface {

  // This method is implemented in org.jmol.export.Exporter 
  // when selecting a specific driver:

  abstract void initialize(Viewer viewer, Graphics3D g3d, StringBuffer output);

  // The following two methods are provided as a general necessity of many drivers.

  abstract void getHeader();

  abstract void getFooter();

  // These methods are used by specific shape generators, which themselves are 
  // extensions of classes in org.jmol.shape, org.jmol.shapebio, and org.jmol.shapespecial. 
  // More will be added as additional objects are added to be exportable classes.

  abstract void renderAtom(Atom atom, short colix);

  abstract void renderBond(Atom atom1, Atom atom2, short colix1, short colix2,
                           byte endcaps, int madBond);

  // The following methods are used by a variety of shape generators and 
  // replace methods in org.jmol.g3d. More will be added as needed. 

  abstract void fillSphereCentered(int radius, Point3f pt, short colix);

  abstract void renderIsosurface(Point3f[] vertices, short colix,
                                 short[] colixes, short[] normals,
                                 int[][] indices, BitSet bsFaces,
                                 int nVertices, int nPoints);
}
