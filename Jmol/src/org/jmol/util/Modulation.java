package org.jmol.util;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Modulation extends V3 {

  private final static double TWOPI = 2 * Math.PI; 
  
  private double ccos; 
  private double csin;
  private V3 q; // wave vector
  private char axis;

  /**
   * Each atomic modulation involves a fractional coordinate wave vector q, 
   * a modulation axis (x, y, or, z), and specified coefficients for cos and sin.
   * 
   * @param q
   * @param axis
   * @param coefs
   */
  public Modulation(P3 q, char axis, P3 coefs) {
    this.q = V3.newV(q);
    this.axis = axis;
    this.ccos = coefs.x;
    this.csin = coefs.y;
  }
  
  /**
   * Starting with fractional coordinates, determine the overall modulation vector.
   * 
   * @param pt     fractional xyz
   * @param mods   a given atom's modulations
   * @param vecMod will be filled;
   */
  public static void modulateAtom(P3 pt, JmolList<Modulation> mods, V3 vecMod) {    
    V3 r = V3.newV(pt);
    vecMod.set(0, 0, 0);
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).addTo(r, vecMod);
  }

  /**
   * @param r 
   * @param vecMod 
   * 
   */
  private void addTo(V3 r, V3 vecMod) {
    // pt[axis]' = pt[axis] + A1 cos(2pi * q.r) + B1 sin(2pi * q.r)
    double theta = TWOPI * q.dot(r);
    double v = 0;
    if (ccos != 0)
      v += ccos * Math.cos(theta);
    if (csin != 0)
      v += csin * Math.sin(theta);
    switch (axis) {
    case 'x':
      vecMod.x += v;
      break;
    case 'y':
      vecMod.y += v;
      break;
    case 'z':
      vecMod.z += v;
      break;
    }
   }

}
