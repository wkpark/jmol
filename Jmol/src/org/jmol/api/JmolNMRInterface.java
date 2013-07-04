package org.jmol.api;

public interface JmolNMRInterface {

  /**
   * Get magnetogyricRatio (gamma/10^7 rad s^-1 T^-1) and quadrupoleMoment (Q/fm^2)
   * for a given isotope or for the default isotope of an element.
   * 
   * @param isoSym may be an element symbol (H, F) or an isotope_symbol (1H, 19F)
   * @return  [g, Q]
   */
  public float[] getIsotopeData(String isoSym);

}


