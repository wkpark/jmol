public class ChemModel {
    
    private static AtomTypeTable atomTypeTable;
    private static float bondFudge       = 1.12f;
    private static boolean AutoBond      = true;
    private static boolean pickedAtoms[]; // These are static because 
    private static boolean pickedBonds[]; // the picked list should persist
    private static boolean pickedBends[]; // across frames.
    private static boolean pickedTorsions[];
    private static int napicked;
    private static int nbpicked;
    private static int nepicked;
    private static int ntpicked;

    // This stuff can vary for each frame in the dynamics:

    String info;       // The title or info string for this frame.
    float vert[];      // atom vertices in real space
    float vect[];      // vert + dx  for vectors in real space
    AtomType atoms[];  // array of atom types
    Bond bonds[];      // array of bonds
    Vector[] aProps;   // array of Vector of atom properties
    Vector frameProps; // Vector of all the properties present in this frame
    boolean hasProperties = false;
    boolean hasVectors = false;
    int nbonds, maxbonds, maxvert;
    int maxbpa = 10;     // maximum number of bonds per atom
    int nBpA[];          // number of bonds per atom
    int inBonds[][];     // atom i's membership in it's jth bond points 
                         // to which bond?
    int bondEnd1[];
    int bondEnd2[];

    float xmin, xmax, ymin, ymax, zmin, zmax;

    /**
     * returns the number of atoms that are currently in the "selected"
     * list for future operations
     */
    public static int getNpicked() {
        return napicked;
    }    
    static void setAtomTypeTable(AtomTypeTable att) {
        atomTypeTable = att;
    }
    static void setBondFudge(float bf) {
        bondFudge = bf;
    }
    static float getBondFudge() {
        return bondFudge;
    }
    static void setAutoBond(boolean ab) {
        AutoBond = ab;
    }
    static boolean getAutoBond() {
        return AutoBond;
    }

    /**
     * Constructor for a ChemFrame with a known number of atoms.
     *
     * @param na the number of atoms in the frame
     */
    public ChemFrame(int na) {
        this.maxvert = na;
        frameProps = new Vector();
        vert = new float[na * 3];
        vect = new float[na * 3];
        atoms = new AtomType[na];
        aProps = new Vector[na];
        pickedAtoms = new boolean[na];
        nBpA = new int[na];
        inBonds = new int[na][maxbpa];
        for (int i = 0; i < na; i++) {
            vert[3*i] = 0.0f;
            vert[3*i+1] = 0.0f;
            vert[3*i+2] = 0.0f;
            vect[3*i] = 0.0f;
            vect[3*i+1] = 0.0f;
            vect[3*i+2] = 0.0f;
            pickedAtoms[i] = false;
        }
    }

    /**
     * Constructor for a ChemFrame with an unknown number of atoms.
     *
     */
    public ChemFrame() {
        this.maxvert = 100;
        frameProps = new Vector();
        vert = new float[100 * 3];
        vect = new float[100 * 3];
        atoms = new AtomType[100];
        aProps = new Vector[100];
        pickedAtoms = new boolean[100];
        nBpA = new int[100];
        inBonds = new int[100][maxbpa];
        for (int i = 0; i < 100; i++) {
            vert[3*i] = 0.0f;
            vert[3*i+1] = 0.0f;
            vert[3*i+2] = 0.0f;
            vect[3*i] = 0.0f;
            vect[3*i+1] = 0.0f;
            vect[3*i+2] = 0.0f;
            pickedAtoms[i] = false;
        }
    }

    /** 
     * returns a Vector containing the list of PhysicalProperty descriptors
     * present in this frame
     */
    public Vector getFrameProps() {
        return frameProps;
    }

    /**
     * Sets the information label for the frame
     *
     * @param info the information label for this frame
     */            
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Returns this frame's information label
     */            
    public String getInfo() {
        return info;
    }

    /**
     * Adds an atom to the frame and finds all bonds between the 
     * new atom and pre-existing atoms in the frame
     *
     * @param name the name of the extended atom type for the new atom
     * @param x the x coordinate of the new atom
     * @param y the y coordinate of the new atom
     * @param z the z coordinate of the new atom
     */                
    public int addVert(String name, float x, float y, 
                       float z) throws Exception {
        int i = nvert;
        if (i >= maxvert) {
            System.out.println("Increasing vector size!");
            maxvert *= 2;
            
            float nv[] = new float[maxvert * 3];
            System.arraycopy(vert, 0, nv, 0, vert.length);
            vert = nv;
            
            AtomType nat[] = new AtomType[maxvert];
            System.arraycopy(atoms, 0, nat, 0, atoms.length);
            atoms = nat;
            
            Vector nap[] = new Vector[maxvert];
            System.arraycopy(aProps, 0, nap, 0, aProps.length);
            aProps = nap;

            float nve[] = new float[maxvert * 3];
            System.arraycopy(vect, 0, nve, 0, vect.length);
            vect = nve;

            boolean np[] = new boolean[maxvert];
            System.arraycopy(pickedAtoms, 0, np, 0, pickedAtoms.length);
            pickedAtoms = np;

            int nbpa[] = new int[maxvert];
            System.arraycopy(nBpA, 0, nbpa, 0, nBpA.length);
            nBpA = nbpa;

            int inb2[][] = new int[maxvert][maxbpa];
            System.arraycopy(inBonds, 0, inb2, 0, inBonds.length);
            inBonds = inb2;
        }

        AtomType a = atomTypeTable.get(name);
        atoms[i] = a;        
        nBpA[i] = 0;
        aProps[i] = new Vector();
                      
        for (int j = 0; j < i ; j++ ) {
            float d2 = 0.0f;
            float dx = vert[3*j] - x;
            float dy = vert[3*j+1] - y;
            float dz = vert[3*j+2] - z;
            d2 += dx*dx + dy*dy + dz*dz;
            AtomType b = atoms[j];
            float dr = bondFudge*((float) a.getCovalentRadius() + 
                                  (float) b.getCovalentRadius());
            float dr2 = dr*dr;
            
            if (d2 <= dr2) {
                // We found a bond
                int k = nbonds;
                if (k >= maxbonds)
                    if (bonds == null) {
                        maxbonds = 100;
                        bonds = new Bond[maxbonds];
                        bondDrawn = new boolean[maxbonds];
                        bondEnd1 = new int[maxbonds];
                        bondEnd2 = new int[maxbonds];
                        pickedBonds = new boolean[maxbonds];
                    } else {
                        maxbonds *= 2;
                        Bond nb[] = new Bond[maxbonds];
                        System.arraycopy(bonds, 0, nb, 0, bonds.length);
                        bonds = nb;
                        boolean bd[] = new boolean[maxbonds];
                        System.arraycopy(bondDrawn, 0, bd, 0, 
                                         bondDrawn.length);
                        boolean bp[] = new boolean[maxbonds];
                        System.arraycopy(pickedBonds, 0, bp, 0, 
                                         pickedBonds.length);
                        bondDrawn = bd;
                        int be1[] = new int[maxbonds];
                        System.arraycopy(bondEnd1, 0, be1, 0, bondEnd1.length);
                        bondEnd1 = be1;
                        int be2[] = new int[maxbonds];
                        System.arraycopy(bondEnd2, 0, be2, 0, bondEnd2.length);
                        bondEnd2 = be2;
                    }
                Bond bond = new Bond(a, b);
                bonds[k] = bond;
                bondEnd1[k] = i;
                bondEnd2[k] = j;
                
                int na = nBpA[i] + 1;
                int nb = nBpA[j] + 1;

                if (na >= maxbonds) throw new JmolException("ChemFrame.rebond",
                                                            "max bonds per atom exceeded");
                if (nb >= maxbonds) throw new JmolException("ChemFrame.rebond",
                                                            "max bonds per atom exceeded");
                
                inBonds[i][na-1] = k;
                inBonds[j][nb-1] = k;                
                nBpA[j] = nb;
                nBpA[i] = na;
                
                nbonds++;
            }
        }
        
        i *= 3;
        vert[i] = x;
        vert[i + 1] = y;
        vert[i + 2] = z;
        
        return nvert++;
    }
    
    /**
     * Adds an atom to the frame and finds all bonds between the 
     * new atom and pre-existing atoms in the frame 
     *
     * @param name the name of the extended atom type for the new atom
     * @param x the x coordinate of the new atom
     * @param y the y coordinate of the new atom
     * @param z the z coordinate of the new atom  
     * @param props a Vector containing the properties of this atom
     */                
    public int addPropertiedVert(String name, float x, float y, float z, 
                                 Vector props) throws Exception {

        hasProperties = true;
        int i = addVert(name, x, y, z);
        aProps[i] = props;

        for (int j = 0; j < props.size(); j++) {
            PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
            String desc = p.getDescriptor();
            if (desc.equals("Vector")) {
                hasVectors = true;
                VProperty vp = (VProperty) p;
                double[] vtmp = new double[3];
                vtmp = vp.getVector();
                int k = i*3;
                vect[k] = (float)(x + vtmp[0]);
                vect[k+1] = (float)(y + vtmp[1]);
                vect[k+2] = (float)(z + vtmp[2]);
            }
            
            // Update the frameProps if we found a new property
            if (frameProps.indexOf(desc) < 0) {
                frameProps.addElement(desc);
            }
            
        }
        return i;
    }


    /** 
     * Add all atoms in this frame to the list of picked atoms
     */
    public void selectAll() {
        if (nvert <= 0)
            return;
        napicked = 0;
        for (int i = 0; i < nvert; i++) {
                pickedAtoms[i] = true; 
                napicked++;
        }
    }

    /** 
     * Remove all atoms in this frame from the list of picked atoms
     */
    public void deselectAll() {
        if (nvert <= 0)
            return;
        for (int i = 0; i < nvert; i++) {
                pickedAtoms[i] = false; 
        }
        napicked=0;
    }
        
    /** 
     * Clear out the list of picked atoms, find the nearest atom to a
     * set of screen coordinates and add this new atom to the picked
     * list.
     *
     * @param x the screen x coordinate of the selection point
     * @param y the screen y coordinate of the selection point
     */
    public void selectAtom(int x, int y) {
        int smallest = getNearestAtom(x, y);
        if (pickedAtoms[smallest]) {
            pickedAtoms[smallest] = false; 
        } else {
            pickedAtoms[smallest] = true; 
        }
        for (int i = 0; i < nvert; i++) {
            if (i != smallest) pickedAtoms[i] = false;
        }
        napicked = 1;
    }

    /** 
     * Find the nearest atom to a set of screen coordinates and add
     * this new atom to the picked list.
     *
     * @param x the screen x coordinate of the selection point
     * @param y the screen y coordinate of the selection point 
     */
    public void shiftSelectAtom(int x, int y) {
        int smallest = getNearestAtom(x, y);
        if (pickedAtoms[smallest]) {
            pickedAtoms[smallest] = false; 
        } else {
            pickedAtoms[smallest] = true; 
        }
        napicked++;
    }

    /** 
     * Clear out the list of picked atoms, find all atoms within
     * designated region and add these atoms to the picked list.
     *
     * @param x1 the x coordinate of point 1 of the region's bounding rectangle
     * @param y1 the y coordinate of point 1 of the region's bounding rectangle
     * @param x2 the x coordinate of point 2 of the region's bounding rectangle
     * @param y2 the y coordinate of point 2 of the region's bounding rectangle
     */
    public void selectRegion(int x1, int y1, int x2, int y2) {
        if (nvert <= 0)
            return;
        transform();
        int v[] = tvert;
        napicked=0;
        for (int i = 0; i < nvert; i++) {
            if (isAtomInRegion(i, x1, y1, x2, y2)) {
                pickedAtoms[i] = true; 
                napicked++;
            } else {
                pickedAtoms[i] = false;
            }
        }
    }

    /** 
     * Find all atoms within designated region and add these atoms to
     * the picked list.
     *
     * @param x1 the x coordinate of point 1 of the region's bounding rectangle
     * @param y1 the y coordinate of point 1 of the region's bounding rectangle
     * @param x2 the x coordinate of point 2 of the region's bounding rectangle
     * @param y2 the y coordinate of point 2 of the region's bounding rectangle
     */
    public void shiftSelectRegion(int x1, int y1, int x2, int y2) {
        if (nvert <= 0)
            return;
        transform();
        int v[] = tvert;
        for (int i = 0; i < nvert; i++) {
            if (isAtomInRegion(i, x1, y1, x2, y2)) {
                pickedAtoms[i] = true; 
                napicked++;
            }
        }
    }
                
    private boolean isAtomInRegion(int n, int x1, int y1, int x2, int y2) {
        int x = tvert[3*n];
        int y = tvert[3*n+1];
        if (x > x1 && x < x2) {
            if (y > y1 && y < y2) {
                return true;
            }
        }
        return false;
    }
       
    private int getNearestAtom(int x, int y) {
        if (nvert <= 0)
            return -1;
        transform();
        int v[] = tvert;
        int dx, dy, dr2;
        int smallest = -1;
        int smallr2 = Integer.MAX_VALUE;
        for (int i = 0; i < nvert; i++) {
            dx = v[3*i]-x;
            dy = v[3*i+1]-y;
            dr2 = dx*dx + dy*dy;
            if (dr2 < smallr2) {
                smallest = i;
                smallr2 = dr2;
            }
        }
        if (smallest >= 0) {
            return smallest;
        }
        return -1;
    }
    
    /** 
     * Find the bounding box of this model 
     */
    public void findBB() {
        if (nvert <= 0)
            return;
        float v[] = vert;
        float xmin = v[0], xmax = xmin;
        float ymin = v[1], ymax = ymin;
        float zmin = v[2], zmax = zmin;
        for (int i = nvert * 3; (i -= 3) > 0;) {
            float x = v[i];
            if (x < xmin)
                xmin = x;
            if (x > xmax)
                xmax = x;
            float y = v[i + 1];
            if (y < ymin)
                ymin = y;
            if (y > ymax)
                ymax = y;
            float z = v[i + 2];
            if (z < zmin)
                zmin = z;
            if (z > zmax)
                zmax = z;
        }
        this.xmax = xmax;
        this.xmin = xmin;
        this.ymax = ymax;
        this.ymin = ymin;
        this.zmax = zmax;
        this.zmin = zmin;
    }

    /** 
     * Walk through this frame and find all bonds again.
     */
    public void rebond() throws Exception {
        // zero out the currently existing bonds:
        nbonds = 0;        
        for (int i = 0; i < nvert; i++) {
            nBpA[i] = 0;
        }
        // do a n*(n-1) scan to get new bonds:
        for (int i = 0; i < nvert-1; i++) {
            AtomType a = atoms[i];
            float ax = vert[3*i];
            float ay = vert[3*i+1];
            float az = vert[3*i+2];
            for (int j = i; j < nvert ; j++ ) {
                float d2 = 0.0f;
                float dx = vert[3*j]   - ax;
                float dy = vert[3*j+1] - ay;
                float dz = vert[3*j+2] - az;
                d2 += dx*dx + dy*dy + dz*dz;
                AtomType b = atoms[j];
                float dr = bondFudge*((float) a.getCovalentRadius() + 
                                  (float) b.getCovalentRadius());
                float dr2 = dr*dr;
                
                if (d2 <= dr2) {
                    // We found a bond
                    int k = nbonds;
                    if (k >= maxbonds)
                        if (bonds == null) {
                            maxbonds = 100;
                            bonds = new Bond[maxbonds];
                            bondDrawn = new boolean[maxbonds];
                            bondEnd1 = new int[maxbonds];
                            bondEnd2 = new int[maxbonds];
                        } else {
                            maxbonds *= 2;
                            Bond nb[] = new Bond[maxbonds];
                            System.arraycopy(bonds, 0, nb, 0, bonds.length);
                            bonds = nb;
                            boolean bd[] = new boolean[maxbonds];
                            System.arraycopy(bondDrawn, 0, bd, 0, bondDrawn.length);
                            bondDrawn = bd;
                            int be1[] = new int[maxbonds];
                            System.arraycopy(bondEnd1, 0, be1, 0, bondEnd1.length);
                            bondEnd1 = be1;
                            int be2[] = new int[maxbonds];
                            System.arraycopy(bondEnd2, 0, be2, 0, bondEnd2.length);
                            bondEnd2 = be2;
                        }
                    Bond bt = new Bond(a, b);
                    bonds[k] = bt;
                    bondEnd1[k] = i;
                    bondEnd2[k] = j;
                
                    int na = nBpA[i] + 1;
                    int nb = nBpA[j] + 1;

                    if (na >= maxbonds) throw new JmolException("ChemFrame.rebond", "max bonds per atom exceeded");
                    if (nb >= maxbonds) throw new JmolException("ChemFrame.rebond", "max bonds per atom exceeded");
                    
                    inBonds[i][na-1] = k;
                    inBonds[j][nb-1] = k;                
                    nBpA[j] = nb;
                    nBpA[i] = na;
                    
                    nbonds++;
                }
            }            
        }
    }
}


        
        
