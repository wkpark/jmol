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
  private V3 nq; // wave vector
  private int n; // power
  private char axis;

  
  public V3 getWaveVector() {
    return nq;
  }
  
  /**
   * Each atomic modulation involves a fractional coordinate wave vector q, a
   * Fourier power n, a modulation axis (x, y, or, z), and specified 
   * coefficients for cos and sin.
   * 
   * 
   * @param nq
   * @param n 
   * @param axis
   * @param coefs
   */
  public Modulation(P3 nq, int n, char axis, P3 coefs) {
    this.nq = V3.newV(nq);
    //this.q.scale(1f/n); leave this nq.
    this.n = n;
    this.axis = axis;
    this.ccos = coefs.x;
    this.csin = coefs.y;
  }
  
  /**
   * Starting with fractional coordinates, determine the overall modulation displacement.
   * 
   * @param mods     a given atom's modulations
   * @param r        fractional xyz
   * @param epsilon  as in x4´ = epsilon x4 + delta  
   * @param delta    as in x4´ = epsilon x4 + delta
   * @param rot      symmetry rotation to be applied 
   * @param d        displacement return
   */
  public static void modulateAtom(JmolList<Modulation> mods, Tuple3f r, float epsilon, float delta, Matrix3f rot, V3 d) {    
    d.set(0, 0, 0);
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(r, epsilon, delta, d);
    rot.transform(d);    
  }

  /**
   * 
   * In general, we have:
   *  
   *   u_axis(x) = sum[A1 cos(theta) + B1 sin(theta)]
   * 
   * where axis is x, y, or z, and theta = 2n pi x
   * 
   * However, for symmetry-related atoms, we need to do a 4D transformation, 
   * not just a 3D one. We need to operate on x first BEFORE applying u(x):
   * 
   *   u(x4') = Ru(x4)
   * 
   * where we need to express x4 in terms of a "transformed" x4', because 
   * x4' is for our rotated point.
   * 
   *   x4' = epsilon x4 + delta
   * 
   * where epsilon = +/-1, so
   * 
   *   x4 = epsilon (x4' - delta)
   * 
   * More generally, we might have something like:
   * 
   *   x4' = x5 + 1/2; x5' = x4 - 1/2
   *   
   * Will have to work on that later!
   * 
   * 
   * @param r 
   * @param epsilon 
   * @param delta 
   * @param vecMod 
   * 
   */
  private void apply(Tuple3f r, float epsilon, float delta, V3 vecMod) {
    
    // TODO: must be adapted for d > 1 modulation
    
    double theta = TWOPI * (epsilon * (nq.dot(r) + n * delta));
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
    //System.out.println("MOD q=" + nq + " r=" + r + " axis=" + axis + " theta=" + theta + " ccos=" + ccos + " csin=" + csin + " delta=" + delta + " v=" + v);
  }

}
