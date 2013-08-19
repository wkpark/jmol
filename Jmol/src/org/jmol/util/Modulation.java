package org.jmol.util;

/**
 * A class to allow for more complex vibrations and associated phenomena, such
 * as modulated crystals, including Fourier series, Crenel functions, and
 * sawtooth functions
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/8/2013
 * 
 */

public class Modulation {

  private static final double TWOPI = 2 * Math.PI;

  private int[] qCoefs;
  
  private double a1;
  private double a2;
  private double center;
  private double left, right;

  private int fn; // power
  private char axis;
  private final int type;

  private String utens;

  public static final int TYPE_DISP_FOURIER = 0;
  public static final int TYPE_DISP_SAWTOOTH = 1;
  public static final int TYPE_OCC_FOURIER = 2;
  public static final int TYPE_OCC_CRENEL = 3;
  public static final int TYPE_U_FOURIER = 4;

  /**
   * Each atomic modulation involves a fractional coordinate wave vector q, a
   * Fourier power n, a modulation axis (x, y, or, z), and specified parameters
   * that depend upon the type of function.
   * 
   * 
   * @param axis
   * @param type
   * @param fn
   * @param params
   * @param utens TODO
   * @param qCoefs
   */
  public Modulation(char axis, int type, int fn, P3 params, String utens, int[] qCoefs) {
    if (Logger.debuggingHigh)
      Logger.debug("MOD create " + Escape.eAI(qCoefs) + " axis=" + axis + " type=" + type + " fn=" + fn + " params=" + params + " utens=" + utens);
    this.axis = axis;
    this.type = type;
    this.fn = fn;
    this.utens = utens;
    this.qCoefs = qCoefs;
    switch (type) {
    case TYPE_DISP_FOURIER:
    case TYPE_OCC_FOURIER:
    case TYPE_U_FOURIER:
      a1 = params.x;  // cos
      a2 = params.y;  // sin
      //System.out.println("ccos=" + a1 + " csin=" + a2);
      break;
    case TYPE_DISP_SAWTOOTH:
    case TYPE_OCC_CRENEL:
      center = params.x;
      float width = params.y;
      if (width > 1)
        width = 1; // http://b-incstrdb.ehu.es/incstrdb/CIFFile.php?RefCode=Bi-Sr-Ca-Cu-O_rNdCbetq
      left = center - width / 2;
      right = center + width / 2;
      if (left < 0)
        left += 1;
      if (right > 1)
        right -= 1;
      if (left >= right && left - right < 0.01f)
        left = right + 0.01f;
      a1 = 2 * params.z / width;
      break;
    }
  }

  /**
   * 
   * In general, we have, for Fourier:
   * 
   * u_axis(x) = sum[A1 cos(theta) + B1 sin(theta)]
   * 
   * where axis is x, y, or z, and theta = 2n pi x
   * 
   * @param i
   * @param ms
   * @param x4 
   * 
   */
   void apply(int i, ModulationSet ms, double x4) {

    double v = 0;
    //if (type == TYPE_OCC_CRENEL)
    //delta = 0;
    
    switch (type) {
    case TYPE_DISP_FOURIER:
    case TYPE_OCC_FOURIER:
    case TYPE_U_FOURIER:
      double theta = TWOPI * fn * x4;
      if (a1 != 0)
        v += a1 * Math.cos(theta);
      if (a2 != 0)
        v += a2 * Math.sin(theta);
      if (Logger.debuggingHigh)
        Logger.debug("MOD " +  i + ":" + ms.id + " fn=" + fn + " " +  " axis=" + axis + " v=" + v + " ccos,csin=" + a1 + "," + a2 + " / theta=" + theta);
      break;
    case TYPE_OCC_CRENEL:

      //  An occupational crenel function along the internal space is
      //  defined as follows:
      //
      //           p(x4)=1   if x4 belongs to the interval [c-w/2,c+w/2]
      //           p(x4)=0   if x4 is outside the interval [c-w/2,c+w/2],

      x4 -= Math.floor(x4);
      ms.vocc = (range(x4) ? 1 : 0);
      ms.vocc0 = Float.NaN; // don't add this in
      //System.out.println("MOD " + ms.r + " " +  ms.delta + " " + ms.epsilon + " " + ms.id + " " + ms.v + " l=" + left + " x=" + x4 + " r=" + right);
      return;
    case TYPE_DISP_SAWTOOTH:

      //  _atom_site_displace_special_func_sawtooth_ items are the
      //  adjustable parameters of a sawtooth function. A displacive sawtooth
      //  function along the internal space is defined as follows:
      //
      //    u_x = 2a_x[(x4 − c)/w] 
      //             
      //  for x4 belonging to the interval [c − (w/2), c + (w/2)], where ax,
      //  ay and az are the amplitudes (maximum displacements) along each
      //  crystallographic axis, w is its width, x4 is the internal coordinate
      //  and c is the centre of the function in internal space. ux, uy and
      //  uz must be expressed in relative units.

      // here we have set a1 = 2a_xyz/w 

      x4 -= Math.floor(x4);
      if (!range(x4))
        return;

      // x < L < c
      //
      //           /|
      //          / |
      //         / x------------->
      //         |  |   L     /|
      // --------+--|---|----c-+------
      //         0  R   |   /  1       
      //                |  /         
      //                | /
      //                |/

      // becomes

      //                         /|
      //                        / |
      //                       / x|
      //                L     /   |
      // --------+------|----c-+--|----
      //         0      |   /  1  R     
      //                |  /         
      //                | /
      //                |/

      // x > R > c
      //
      //              /|
      //             / |
      //            /  |
      //           /   |         L
      // --------+c----|---------|---+-------
      //         0     R         |   1
      //        <-----------------x / 
      //                         | /
      //                         |/

      // becomes

      //              /|
      //             / |
      //            /  |
      //     L     /   |
      // ----|---+c----|-------------+--------
      //     |   0     R             1
      //     |x /                  
      //     | /
      //     |/

      if (left > right) {
        if (x4 < left && left < center)
          x4 += 1;
        else if (x4 > right && right > center)
          x4 -= 1;
      }
      v = a1 * (x4 - center);
      break;
    }

    switch (axis) {
    case 'x':
      ms.x += v;
      break;
    case 'y':
      ms.y += v;
      break;
    case 'z':
      ms.z += v;
      break;
    case 'U':
      ms.addUTens(utens, (float) v, fn);
      break;
    default:
      if (Float.isNaN(ms.vocc))
        ms.vocc = 0;
      ms.vocc += (float) v;
    }
  }

  /**
   * Check that left < x4 < right, but allow for folding
   * 
   * @param x4
   * @return true only if x4 is in the (possibly folded) range of left and right
   * 
   */
  private boolean range(double x4) {
    return (left < right ? left <= x4 && x4 <= right : left <= x4
        || x4 <= right);
  }

  /** just guessing here....
   * 
   * @param aq
   * @param q
   * @return pointer to last-used index
   */
  int getQ(P3[] aq, P3 q) {
    q.set(0, 0, 0);
    int eq = 0;
    fn = 0;
    for (int i = 0; i < aq.length; i++) {
      q.scaleAdd2(qCoefs[i], aq[i], q);
      if (qCoefs[i] != 0) {
        eq = i;
        fn+= Math.abs(qCoefs[i]);
      }
    }
    q.scale(1f/fn);
    return eq;
  }
  
}
