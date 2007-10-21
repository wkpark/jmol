package org.jmol.api;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.viewer.Viewer;
import java.util.BitSet;

public interface JmolExportInterface {

  abstract void setRenderer(JmolRendererInterface jmolRenderer);
  
  // This method is implemented in org.jmol.export._Exporter 
  // when selecting a specific driver:

  abstract boolean initializeOutput(Viewer viewer, Graphics3D g3d, Object output);
  
  abstract String finalizeOutput();

  // The following two methods are provided as a general necessity of many drivers.

  abstract void getHeader();

  abstract void getFooter();

  // These methods are used by specific shape generators, which themselves are 
  // extensions of classes in org.jmol.shape, org.jmol.shapebio, and org.jmol.shapespecial. 
  // More will be added as additional objects are added to be exportable classes.

  abstract void renderAtom(Atom atom, short colix);

  // The following methods are used by a variety of shape generators and 
  // replace methods in org.jmol.g3d. More will be added as needed. 

  abstract void renderIsosurface(Point3f[] vertices, short colix,
                                 short[] colixes, Vector3f[] normals,
                                 int[][] indices, BitSet bsFaces,
                                 int nVertices, int faceVertexMax);
  
  abstract void renderText(Text t);
  
  abstract void drawString(short colix, String str, Font3D font3d, int xBaseline,
                            int yBaseline, int z, int zSlab);
  
  abstract void fillCylinder(Point3f atom1, Point3f atom2, short colix1, short colix2,
                             byte endcaps, int madBond, int bondOrder);

  abstract void fillCylinder(short colix, byte endcaps, int diameter, 
                             Point3f screenA, Point3f screenB);

  abstract void fillScreenedCircleCentered(short colix, int diameter, int x,
                                                    int y, int z);  //halos 

  abstract void drawPixel(short colix, int x, int y, int z); //measures
 
  abstract void drawTextPixel(int argb, int x, int y, int z);

  //rockets and dipoles
  abstract void fillCone(short colix, byte endcap, int diameter, 
                         Point3f screenBase, Point3f screenTip);
  
  //cartoons, rockets:
  abstract void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC);
  
  //rockets:
  abstract void fillSphereCentered(short colix, int diameter, Point3f pt);
  
  abstract void plotText(int x, int y, int z, int argb, int backgroundArgb, 
                         String text, Font3D font3d);

  // NOT IMPLEMENTED, but could be if needed:
  
  //cartoons, meshRibbons:
  abstract void drawHermite(short colix, boolean fill, boolean border, int tension,
                   Point3f s0, Point3f s1, Point3f s2, Point3f s3,
                   Point3f s4, Point3f s5, Point3f s6, Point3f s7,
                   int aspectRatio);
  
  //cartoons, rockets, trace:
  abstract void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3f s0, Point3f s1, Point3f s2, Point3f s3);
  
  //strands:
  abstract void drawHermite(short colix, int tension,
                             Point3f s0, Point3f s1, Point3f s2, Point3f s3);


}