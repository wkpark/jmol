package org.jmol.api;

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.jvxl.data.VolumeData;

public interface MepCalculationInterface {

  public abstract void calculate(VolumeData volumeData, BitSet bsSelected,
                                 Point3f[] atomCoordAngstroms, float[] charges);

}
