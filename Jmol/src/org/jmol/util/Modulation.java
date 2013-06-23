package org.jmol.util;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Modulation extends V3 {

  private double ccos; 
  private double csin;
  private int f; // usually f[0] = 1, f[1] = 2, etc. 
  private V3 wv; // wave vectors
  private double twoPIx4;

  public static float getPointAndOffset(Modulation[] mods, P3 pt, double t, double scale) {
    // f(x4) = x1 + A1 cos(2pi * x4 * t) + B1 sin(2pi * x4 * t)
    //            + A2 cos(2pi * x4 * t * 2) + B2 sin(2pi * x4 * t * 2);
    //            + A3 cos(2pi * x4 * t * 3) + B3 sin(2pi * x4 * t * 3);
    // however, more generally, we allow any number of independent wave
    // vectors with any number of orders.
    // x' = x1 + sum_i[ ccos[i] cos(2pi * x4[i] * f[i] * t)
    //                    +csin[i] sin(2pi * x4[i] * f[i] * t) ] * wv[i] * scale
    P3 pt0 = P3.newP(pt);
    for (int i = mods.length; --i >= 0;) {
      mods[i].addTo(pt, t, scale);
    }
    return pt.distance(pt0);
  }

  private void addTo(P3 pt, double t, double scale) {
    double theta = t * twoPIx4 * f;
    double v = 0;
    if (ccos != 0)
      v += ccos * Math.cos(theta);
    if (csin != 0)
      v += csin * Math.sin(theta);
    pt.scaleAdd2((float) (v * scale), wv, pt);
  }

}
