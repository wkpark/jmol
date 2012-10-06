package org.jmol.api;



import org.jmol.modelset.Atom;
import org.jmol.util.BitSet;
import org.jmol.util.Point3f;


public interface MepCalculationInterface {

  public abstract void calculate(VolumeDataInterface volumeData, BitSet bsSelected,
                                 Point3f[] atomCoordAngstroms, float[] charges, int calcType);

  public abstract void assignPotentials(Atom[] atoms, float[] potentials, BitSet bsAromatic, BitSet bsCarbonyl, BitSet bsIgnore, String data);

  public abstract float valueFor(float x, float d2, int distanceMode);

}
