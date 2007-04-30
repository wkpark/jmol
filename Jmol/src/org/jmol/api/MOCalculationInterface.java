package org.jmol.api;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;

import org.jmol.jvxl.data.VolumeData;

public interface MOCalculationInterface {

  public abstract void calculate(VolumeData volumeData, BitSet bsSelected,
                                 String calculationType,
                                 Point3f[] atomCoordAngstroms,
                                 int firstAtomOffset, Vector shells,
                                 float[][] gaussians, Hashtable aoOrdersDF,
                                 int[][] slaterInfo, float[][] slaterData,
                                 float[] moCoefficients);

}
