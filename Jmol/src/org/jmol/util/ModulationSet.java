package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

/**
 * A class to group a set of modulations for an atom as a "vibration"
 * Extends V3 so that it will be a displacement, and its value will be an occupancy
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/9/2013
 * 
 */

public class ModulationSet extends Vibration {

  public JmolList<Modulation> mods;
  public float[] epsilon;
  public float[] delta;
  public int[] t = new int[3];
  
  public P3[] q123;
  public double[] qlen;
  private int modDim;
  public P3 r;
  public Matrix3f rot;
  public float vdisp = Float.NaN;
  public float vocc = Float.NaN;
  public float vocc0 = Float.NaN;
  public Map<String, Float> htValues;
  public boolean enabled = false;
  public String id;

  public V3 prevSetting;

  public ModulationSet(String id, JmolList<Modulation> list, int modDim, P3[] q123, double[] qlen) {
    this.id = id;
    mods = list;
    this.modDim = modDim;
    this.q123 = q123;
    this.qlen = qlen;
  }

  /**
   * Determine the overall modulation.
   * 
   * For symmetry-related atoms, we need to do a 4D transformation, not just a
   * 3D one. We need to operate on x first BEFORE applying u(x):
   * 
   * u(x4') = Ru(x4)
   * 
   * where we need to express x4 in terms of a "transformed" x4', because x4' is
   * for our rotated point.
   * 
   * x4' = epsilon x4 + delta
   * 
   * where epsilon = +/-1, so
   * 
   * x4 = (x4' - delta) / epsilon = epsilon (x4' - delta)
   * 
   * More generally, we might have something like:
   * 
   * x4' = x5 + 1/2; x5' = x4 - 1/2
   * 
   * so in those cases, we have to use the proper q.
   * 
   * 
   */
  public void calculate() {
    // Q: What about x4,x5 exchange?
    set(0, 0, 0);
    htValues = null;
    vdisp = vocc = Float.NaN;
    P3[] aq = new P3[modDim];
    
    for (int iq = 0; iq < modDim; iq++) {
      int eps = (int) epsilon[iq];
      if (eps == 0)
        continue;
      int qpt = (Math.abs(eps) - 1);      
      aq[iq] = q123[qpt];
    }
    P3 q = new P3();
    int jmax = 1;
      for (int j = 0;j < modDim; j++) {
        // testing hypothesis that either one should work...
        // checks out for (0,1) (1,0), but not (1,1)
        if (j == jmax)continue;
      
      for (int i = mods.size(); --i >= 0;) {

        int iq = mods.get(i).getQ(aq, q);
        int eps = (int) epsilon[iq];
        eps = (eps > 0 ? 1 : -1);
        //int qpt = (Math.abs(eps) - 1);


        double x4 = eps * (q.dot(r) - delta[iq] + qlen[iq] * t[iq]);
        System.out.println("MODSET q=" + q + " r=" + r
            + " epsilon=" + eps + " delta=" + delta[iq] + " t=" + t[iq]
            + " x4=" + x4);

        // here we are using the original specification for the function.
        // F2 = "(0)q1 + (1)q2"
        // even though we have switched q1 and q2
        mods.get(i).apply(i, this, x4);
      }
    }
    rot.transform(this);
  }

// http://nanocrystallography.research.pdx.edu/static/mcodcif/4/33/14/4331458.cif
  
//  _space_group_ssg_name_WJJ        P-42_1m(a,a,0)00s(-a,a,0)000
//  loop_
//  _space_group_symop_ssg_id
//  _space_group_symop_ssg_operation_algebraic
//  1 x1,x2,x3,x4,x5
//  2 -x1,-x2,x3,-x4,-x5
//  3 x2,-x1,-x3,x5,-x4
//  4 -x2,x1,-x3,-x5,x4
//  5 -x1+1/2,x2+1/2,-x3,x5+1/2,x4+1/2
//  6 x1+1/2,-x2+1/2,-x3,-x5+1/2,-x4+1/2
//  7 -x2+1/2,-x1+1/2,x3,-x4+1/2,x5+1/2
//  8 x2+1/2,x1+1/2,x3,x4+1/2,-x5+1/2

//  _cell_wave_vector_seq_id
//  _cell_wave_vector_x
//  _cell_wave_vector_y
//  _cell_wave_vector_z
//  1 0.216050 0.216050 0.000000
//  2 -0.216050 0.216050 0.000000

//  loop_
//  _atom_site_Fourier_wave_vector_seq_id
//  _jana_atom_site_fourier_wave_vector_q1_coeff
//  _jana_atom_site_fourier_wave_vector_q2_coeff
//  1 1 0
//  2 0 1
//  3 1 1
//  4 -1 1
//  5 2 0
//  6 0 2

  public void addUTens(String utens, float v, int n) {
    if (htValues == null)
      htValues = new Hashtable<String, Float>();
    Float f = htValues.get(utens);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET " + id + " n=" + n + " utens=" + utens + " f=" + f + " v="+ v);
    if(f != null)
      v += f.floatValue();
    htValues.put(utens, Float.valueOf(v));

  }

  
  /**
   * Set modulation "t" value, which sets which unit cell in sequence we are looking at.
   * 
   * @param isOn
   * @param t
   * @return 0 (no change), 1 (disabled), 2 (enabled), 3 (new t), 4 (same t)
   * 
   */
  public int setModT(boolean isOn, int t) {
    if (t == Integer.MAX_VALUE) {
      if (enabled == isOn)
        return 0;
      enabled = isOn;
      scale(-1);
      return (enabled ? 2 : 1);
    }
    if (t == this.t[0])
      return 4;
    if (prevSetting == null)
      prevSetting = new V3(); 
    prevSetting.setT(this);
    this.t[0] = t;
    calculate();
    enabled = false;
    return 3;
  }

}
