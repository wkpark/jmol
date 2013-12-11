package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;

import javajs.util.List;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

/**
 * A class to group a set of modulations for an atom as a "vibration"
 * Extends V3 so that it will be a displacement, and its value will be an occupancy
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/9/2013
 * 
 */

public class ModulationSet extends Vibration implements JmolModulationSet {

  public float vOcc = Float.NaN;
  public Map<String, Float> htUij;
  public float vOcc0;

  String id;
  V3 x456;
  
  private List<Modulation> mods;
  private M3 gammaE;
  private P3 qtOffset = new P3();
  private int modDim;
  private boolean enabled;
  private P3 r0;
  private ModulationSet modTemp;
  private int iop;
  private M3 gammaIinv;
  private M4 q123w;
  private V3 sI;
  private float scale = 1;
 
  @Override
  public float getScale() {
    return scale;
  }
  
  @Override
  public boolean isEnabled() {
    return enabled;
  }


  public ModulationSet() {
    
  }
  
  /**
   * A collection of modulations for a specific atom. 
   * 
   * We treat the set of modulation vectors q1,q2,q3,... as
   * a matrix Q with row 1 = q1, row 2 = q2, etc. Then we
   * have Qr = [q1.r, q2.r, q3.r,...]. 
   * 
   * Similarly, we express the x1' - xn' aspects of the operators
   * as the matrix Gamma_I (epsilons) and s_I (shifts). However, 
   * since we are only considering up to n = 3, we can express these
   * together as a 4x4 matrix just for storage. 
   * 
   * Then for X defined as [x4,x5,x6...] (column vector, really)
   * we have:
   * 
   * X' = Gamma_I * X + s_I
   *
   * and
   * 
   * X = Gamma_I^-1(X' - S_I)
   * 
   * not figured out for composite structures
   * 
   * @param id 
   * @param r 
   * @param modDim 
   * @param mods 
   * @param gammaE 
   * @param gammaIS 
   * @param q123w 
   * @param iop 
   * @return  this
   * 
   * 
   */

  public ModulationSet set(
  String id, P3 r, int modDim,
  List<Modulation> mods, M3 gammaE, 
                       M4 gammaIS, M4 q123w, int iop) {
    this.id = id;
    this.modDim = modDim;
    this.mods = mods;
    this.gammaE = gammaE;
    this.iop = iop;
    
    // set up x456
    
    gammaIinv = new M3();
    gammaIS.getRotationScale(gammaIinv);
    sI = new V3();
    gammaIS.get(sI);
    gammaIinv.invert();
       
    this.q123w = q123w;

    r0 = P3.newP(r);    
    x456 = V3.newV(r0);
    q123w.transform(x456);
    x456.sub(sI);
    gammaIinv.transform(x456);
    if (Logger.debuggingHigh)
      Logger.debug
      //Logger.info
      ("MODSET create r=" + Escape.eP(r)
        + " si=" + Escape.eP(sI) + " ginv=" + gammaIinv.toString().replace('\n',' ') + " x4=" + x456.x);
    return this;
  }

  private P3 tinv = new P3();
  private SymmetryInterface unitCell;
  
  public synchronized ModulationSet calculate(T3 fracT, boolean isQ) {
    x = y = z = 0;
    htUij = null;
    vOcc = Float.NaN;
    tinv.set(0, 0, 0);
    if (isQ && qtOffset != null) {
      tinv.setT(qtOffset);     
      q123w.transform(tinv);
    }
    if (fracT != null)
      tinv.add(fracT);
    gammaIinv.transform(tinv);
    tinv.add(x456);
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(this, tinv);
    gammaE.transform(this);
    return this;
  }
  
  public void addUTens(String utens, float v) {
    if (htUij == null)
      htUij = new Hashtable<String, Float>();
    Float f = htUij.get(utens);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET " + id + " utens=" + utens + " f=" + f + " v="+ v);
    if(f != null)
      v += f.floatValue();
    htUij.put(utens, Float.valueOf(v));

  }

  
  /**
   * Set modulation "t" value, which sets which unit cell in sequence we are
   * looking at; d=1 only.
   * 
   * @param isOn
   * @param qtOffset
   * @param isQ
   * @param scale
   * 
   */
  @Override
  public void setModTQ(boolean isOn, T3 qtOffset, boolean isQ, float scale, SymmetryInterface uc) {
    this.scale = scale;
    this.enabled = isOn;
    unitCell = uc;
    if (qtOffset == null)
      return;
    if (isQ) {
      this.qtOffset.setT(qtOffset);
      qtOffset = null;
    }
    calculate(qtOffset, isQ);
  }

  @Override
  public void addTo(T3 a, float scale) {
    ptTemp.setT(this);
    ptTemp.scale(this.scale * scale);
    unitCell.toCartesian(ptTemp, true);
    a.add(ptTemp);
  }
    
  @Override
  public String getState() {
    return "modulation " + (!enabled ? "OFF" : qtOffset == null ? "ON" : Escape.eP(qtOffset));
  }

  @Override
  public Object getModulation(String type, T3 t456) {
    getModTemp();
    if (type.equals("D")) {
      return P3.newP(t456 == null ? r0 : modTemp.calculate(t456, false));
    }
    return null;
  }

  P3 ptTemp = new P3();
  
  @Override
  public void setTempPoint(T3 a, T3 t456, float vibScale, float scale) {
    if (!enabled)
      return;
    getModTemp();
    addTo(a, -1);
    modTemp.calculate(t456, false).addTo(a, scale);
  }
    
  private void getModTemp() {
    if (modTemp != null)
      return;
    modTemp = new ModulationSet();
    modTemp.id = id;
    modTemp.x456 = x456;
    modTemp.mods = mods;
    modTemp.gammaE = gammaE;
    modTemp.modDim = modDim;
    modTemp.gammaIinv = gammaIinv;
    modTemp.q123w = q123w;
    modTemp.r0 = r0;
    modTemp.unitCell = unitCell;
  }

  @Override
  public void getInfo(Map<String, Object> info) {
    Hashtable<String, Object> modInfo = new Hashtable<String, Object>();
    modInfo.put("id", id);
    modInfo.put("r0", r0);
    modInfo.put("x456", x456);
    modInfo.put("modDim", Integer.valueOf(modDim));
    modInfo.put("gammaE", gammaE);
    modInfo.put("gammaIinv", gammaIinv);
    modInfo.put("sI", sI);
    modInfo.put("q123w", q123w);
    modInfo.put("symop", Integer.valueOf(iop + 1));

    List<Hashtable<String, Object>> mInfo = new List<Hashtable<String, Object>>();
    for (int i = 0; i < mods.size(); i++)
      mInfo.addLast(mods.get(i).getInfo());
    modInfo.put("mods", mInfo);
    info.put("modulation", modInfo);
  }


}
