import java.util.StringTokenizer;

public class SqueezeCube {
  public static void main(String[] argv) {
    System.err.println("Hello world!");
  }

  BufferedReader br;

  String title1;
  String title2;
  int atomCount;
  boolean negativeAtomCount;
  float volumetricOriginX, volumetricOriginY, volumetricOriginZ;
  final int[] voxelCounts = new int[3];
  final float[][] voxelVectors = new float[3][];

  
  SqueezeCube(BufferedReader br) {
    this.br = br;
    readVolumetricHeader();
  }

  void readVolumetricHeader() {
    try {
      readTitleLines();
      readAtomCountAndOrigin();
      readVoxelVectors();
      readAtoms();
      readExtraLine();
    } catch (Exception e) {
      e.printStackTrace();
      throw new NullPointerException();
    }
  }

  void readVolumetricData() {
    try {
      readVoxelData();
    } catch (Exception e) {
      e.printStackTrace();
      throw new NullPointerException();
    }
  }

  void readTitleLines() throws Exception {
    title1 = br.readLine().trim();
    title2 = br.readLine().trim();
  }

  void readAtomCountAndOrigin() throws Exception {
    StringTokenizer st = new StringTokenizer(br.readLine());
    if (st.countTokens != 4) {
      System.err.println("atom count + origin line incorrect");
      throw new IndexOutOfBoundsException();
    }
    atomCount = Integer.parseInt(st.nextToken());
    volumetricOriginX = Float.parseFloat(st.nextToken());
    volumetricOriginY = Float.parseFloat(st.nextToken());
    volumetricOriginZ = Float.parseFloat(st.nextToken());
    if (atomCount < 0) {
      atomCount = -atomCount;
      negativeAtomCount = true;
    }
  }

  void readVoxelVectors() throws Exception {
    for (int i = 0; i < 3; ++i)
      readVoxelVector(br, i);
  }

  void readVoxelVector(int voxelVectorIndex) throws Exception {
    StringTokenizer st = new StringTokenizer(br.readLine());
    if (st.countTokens != 4) {
      System.err.println("voxel vector line incorrect");
      throw new IndexOutOfBoundsException();
    }
    float[] voxelVector = new float[3];
    voxelVectors[voxelVectorIndex] = voxelVector;
    voxelCounts[voxelVectorIndex] = Integer.parseInt(st.nextToken());
    voxelVector[0] = Float.parseFloat(st.nextToken());
    voxelVector[1] = Float.parseFloat(st.nextToken());
    voxelVector[2] = Float.parseFloat(st.nextToken());
  }

  void readAtoms() throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = br.readLine();
      /*
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (byte)parseInt(line);
      atom.partialCharge = parseFloat(line, ichNextParse);
      atom.x = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.y = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.z = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      */
    }
  }

  void readExtraLine() throws Exception {
    if (negativeAtomCount)
      br.readLine();
  }

  void readVoxelData() throws Exception {
    System.out.println("entering readVoxelData");
    String line = "";
    ichNextParse = 0;
    int voxelCountX = voxelCounts[0];
    int voxelCountY = voxelCounts[1];
    int voxelCountZ = voxelCounts[2];
    voxelData = new float[voxelCountX][][];
    for (int x = 0; x < voxelCountX; ++x) {
      float[][] plane = new float[voxelCountY][];
      voxelData[x] = plane;
      for (int y = 0; y < voxelCountY; ++y) {
        float[] strip = new float[voxelCountZ];
        plane[y] = strip;
        for (int z = 0; z < voxelCountZ; ++z) {
          float voxelValue = parseFloat(line, ichNextParse);
          if (Float.isNaN(voxelValue)) {
            line = br.readLine();
            if (line == null || Float.isNaN(voxelValue = parseFloat(line))) {
              System.out.println("end of file in CubeReader?");
              throw new NullPointerException();
            }
          }
          strip[z] = voxelValue;
        }
      }
    }
    System.out.println("Successfully read " + voxelCountX +
                       " x " + voxelCountY +
                       " x " + voxelCountZ + " voxels");
}
