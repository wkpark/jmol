package org.jmol.util;

import java.util.Hashtable;

import javajs.util.P3;
import javajs.util.T3;

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

  private P3 qCoefs;
  
  private double a1;
  private double a2;
  private double center;
  private double left, right;

  private char axis;
  private final char type;
  private P3 params;

  private String utens;

  private final static String typeNames = "DF DS OF OC UF";

  public static final char TYPE_DISP_FOURIER = 'f';
  public static final char TYPE_DISP_SAWTOOTH = 's';
  public static final char TYPE_OCC_FOURIER = 'o';
  public static final char TYPE_OCC_CRENEL = 'c';
  public static final char TYPE_U_FOURIER = 'u';

  /**
   * Each atomic modulation involves a fractional coordinate wave vector q, a
   * Fourier power n, a modulation axis (x, y, or, z), and specified parameters
   * that depend upon the type of function.
   * 
   * 
   * @param axis
   * @param type
   * @param params
   * @param utens TODO
   * @param qCoefs
   */
  public Modulation(char axis, char type, P3 params, String utens, P3 qCoefs) {
    if (Logger.debuggingHigh)
      Logger.debug("MOD create " + Escape.eP(qCoefs) + " axis=" + axis + " type=" + type + " params=" + params + " utens=" + utens);
    this.axis = axis;
    this.type = type;
    this.utens = utens;
    this.params = params;
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
      a1 = 2 * params.z / params.y;
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
   * More generally, we have for a given rotation that is characterized by
   * 
   * X {x4 x5 x6 ...}
   * 
   * Gamma_E (R3 rotation)
   * 
   * Gamma_I (X rotation)
   * 
   * S_I (X translation)
   * 
   * We allow here only up to x6, simply because we are using standard R3
   * rotation objects Matrix3f, P3, V3.
   * 
   * We desire:
   * 
   * u'(X') = Gamma_E u(X)
   * 
   * which is defined as [private communication, Vaclav Petricek]:
   * 
   * u'(X') = Gamma_E sum[ U_c cos(2 pi (n m).Gamma_I^-1{X - S_I}) + U_s sin(2
   * pi (n m).Gamma_I^-1{X - S_I}) ]
   * 
   * where
   * 
   * U_c and U_s are coefficients for cos and sin, respectively (will be a1 and
   * a2 here)
   * 
   * (n m) is an array of Fourier number coefficients, such as (1 0), (1 -1), or
   * (0 2)
   * 
   * In Jmol we precalculate Gamma_I^-1(X - S_I) as x456, 
   * but we still have to add in Gamma_I^-1(t). 
   * 
   * @param ms
   * @param x456  -- Vector of x4, x5 and x6
   * 
   * 
   */

  void apply(ModulationSet ms, T3 x456) {
    double x = qCoefs.dot(x456);
    double v = 0;
    //if (type == TYPE_OCC_CRENEL)
    //delta = 0;

    switch (type) {
    case TYPE_DISP_FOURIER:
    case TYPE_OCC_FOURIER:
    case TYPE_U_FOURIER:
      double theta = TWOPI * x;
      if (a1 != 0)
        v += a1 * Math.cos(theta);
      if (a2 != 0)
        v += a2 * Math.sin(theta);
      if (Logger.debuggingHigh)
        Logger.debug("MOD " + ms.id + " " + Escape.eP(qCoefs) + " axis=" + axis
            + " v=" + v + " ccos,csin=" + a1 + "," + a2 + " / theta=" + theta);
      break;
    case TYPE_OCC_CRENEL:

      //  An occupational crenel function along the internal space is
      //  defined as follows:
      //
      //           p(x4)=1   if x4 belongs to the interval [c-w/2,c+w/2]
      //           p(x4)=0   if x4 is outside the interval [c-w/2,c+w/2],

      x -= Math.floor(x);
      ms.vOcc = (range(x) ? 1 : 0);
      ms.vOcc0 = Float.NaN; // absolute
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

      x -= Math.floor(x);
      if (!range(x))
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
        if (x < left && left < center)
          x += 1;
        else if (x > right && right > center)
          x -= 1;
      }
      v = a1 * (x - center);
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
      ms.addUTens(utens, (float) v);
      break;
    default:
      if (Float.isNaN(ms.vOcc))
        ms.vOcc = 0;
      ms.vOcc += (float) v;
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

  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    int t = (0 + type) * 3;
    info.put("type", typeNames.substring(t, t + 2).trim() + axis);
    info.put("params", params);
    info.put("qCoefs", qCoefs);
    if (utens != null)
      info.put("Utens",utens);
    return info;
  }

}
