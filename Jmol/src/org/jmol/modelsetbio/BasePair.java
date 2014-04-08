package org.jmol.modelsetbio;

import java.util.Map;

public class BasePair {
  Map<String, Object> info;
  public NucleicMonomer g1;
  public NucleicMonomer g2;
  
  public BasePair(Map<String, Object> info, NucleicMonomer g1, NucleicMonomer g2) {
    this.info = info;
    this.g1 = g1;
    g1.addBasePair(this);
    this.g2 = g2;
    g2.addBasePair(this);
  }

  public int getPartnerAtom(NucleicMonomer g) {
    return (g == g1 ? g2 : g1).getLeadAtom().i;
  }

}
