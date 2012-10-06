package org.jmol.api;


import org.jmol.util.Point3f;
import org.jmol.util.Point3i;
import org.jmol.util.Point4f;
import org.jmol.util.Vector3f;

public interface VolumeDataInterface {

  public abstract void setVoxelDataAsArray(float[][][] voxelData);

  public abstract float[][][] getVoxelData();

  public abstract int setVoxelCounts(int nPointsX, int nPointsY, int nPointsZ);

  public abstract int[] getVoxelCounts();

  public abstract void setVolumetricVector(int i, float x, float y, float z);

  public abstract float[] getVolumetricVectorLengths();

  public abstract void setVolumetricOrigin(float x, float y, float z);

  public abstract float[] getOriginFloat();

  public abstract void setDataDistanceToPlane(Point4f plane);

  public abstract void setPlaneParameters(Point4f plane);

  public abstract float calcVoxelPlaneDistance(int x, int y, int z);

  public abstract float distancePointToPlane(Point3f pt);

  public abstract void transform(Vector3f v1, Vector3f v2);

  public abstract void voxelPtToXYZ(int x, int y, int z, Point3f pt);

  public abstract void xyzToVoxelPt(float x, float y, float z, Point3i pt3i);

  public abstract float lookupInterpolatedVoxelValue(Point3f point);

  public abstract void filterData(boolean isSquared, float invertCutoff);

  public abstract void capData(Point4f plane, float cutoff);

}
