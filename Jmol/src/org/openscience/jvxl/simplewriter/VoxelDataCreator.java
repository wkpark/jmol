package org.openscience.jvxl.simplewriter;

import javax.vecmath.Point3f;

public class VoxelDataCreator {

  VoxelDataCreator() {
  }
  
    /**
     * Developer must customize this method
     * 
     * @param volumeData
     */
    
    void createVoxelData(VolumeData volumeData) {
      float[][][] voxelData = volumeData.getVoxelData();
      int[] counts = volumeData.getVoxelCounts();
      int nX = counts[0];
      int nY = counts[1];
      int nZ = counts[2];
      // whatever method here that is desired;
      Point3f pt = new Point3f();
      for (int x = 0; x < nX; ++x)
        for (int y = 0; y < nY; ++y)
          for (int z = 0; z < nZ; ++z) {
            volumeData.voxelPtToXYZ(x, y, z, pt); 
            // for instance...
            voxelData[x][y][z] = pt.x * pt.x + pt.y * pt.y - pt.z * pt.z;
          }
    }    

}
