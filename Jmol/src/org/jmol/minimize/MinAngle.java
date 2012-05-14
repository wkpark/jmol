package org.jmol.minimize;

public class MinAngle extends MinObject {
  public int sbType;
  public Integer sbKey;
  MinAngle(int[] data) {
    this.data = data; //  ia, ib, ic, iab, ibc
  }  
}
