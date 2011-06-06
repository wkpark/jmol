package org.jmol.api;

import java.util.BitSet;

import java.util.List;

import javax.vecmath.Point3f;


public interface MOCalculationInterface {

  public abstract boolean setupCalculation(VolumeDataInterface volumeData, BitSet bsSelected,
                                 String calculationType,
                                 Point3f[] atomCoordAngstroms,
                                 int firstAtomOffset, List<int[]> shells,
                                 float[][] gaussians, int[][] dfCoefMaps,
                                 Object slaters,
                                 float[] moCoefficients, 
                                 float[] linearCombination, float[][] coefs,
                                 float[] nuclearCharges, boolean doNormalize, Point3f[] points, float[] parameters);
  
  public abstract void createCube();
  public abstract void process(Point3f pt);
  public abstract void calculateElectronDensity(float[] nuclearCharges);
}
