/*
 * @(#)ChemFrame.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import java.awt.Graphics;
import java.util.*;

public class ChemFrame {
    private static float bondFudge       = 1.12f;
//      private static float ScreenScale;
    private static boolean AutoBond      = true;
    private static Matrix3D mat;
    /* 
       pickedAtoms and napicked are static because
       they deal with deformations or measurements that will persist
       across frames.  
    */
    private static boolean[] pickedAtoms;
    private static int napicked;
    // Added by T.GREY for quick drawing on atom movement
    private static boolean doingMoveDraw = false;

    // This stuff can vary for each frame in the dynamics:

    String info;       // The title or info string for this frame.
    float[] vert;      // atom vertices in real space
    float[] vect;      // vert + dx  for vectors in real space
    int[] tvert;       // atom positions transformed to screen space
    int[] tvect;       // vector ends transformed to screen space
    AtomType[] atoms;  // array of atom types
    Bond[] bonds;      // array of bonds
    Vector[] aProps;   // array of Vector of atom properties
    Vector atomProps;  // Vector of all the atom properties present in this frame
    Vector frameProps; // Vector of all the frame properties present in this frame
    boolean hasFrameProperties = false;
    boolean hasAtomProperties = false;
    boolean hasVectors = false;
    int[] ZsortMap;
    int nvert = 0;
    int nbonds, maxbonds, maxvert;
    int maxbpa = 20;     // maximum number of bonds per atom
    int[] nBpA;          // number of bonds per atom
    int[][] inBonds;     // atom i's membership in it's jth bond points 
                         // to which bond?
    boolean[] bondDrawn;
    int[] bondEnd1;
    int[] bondEnd2;
    Vector dlist, alist, dhlist; // distance, angle, dihedral lists
    
    float xmin, xmax, ymin, ymax, zmin, zmax;
    /**@shapeType AggregationLink
    @associates <b>Vibration</b>*/
    static {
        mat = new Matrix3D();
        mat.xrot(0);
        mat.yrot(0);
    }

    /**
     * returns the number of atoms that are currently in the "selected"
     * list for future operations
     */
    public static int getNpicked() {
        return napicked;
    }    

    // Added by T.GREY for quick drawing on atom movement
    /**
     * Sets whether we are in ultra-hasty on the move drawing mode
     * @PARAM movingOn If true then turns on quick mode, if false then turns it off (if its on)
     */
    public void setMovingDrawMode(boolean movingOn){
        doingMoveDraw = movingOn;
    }
    
    // Added by T.GREY for quick drawing on atom movement
    /**
     * Gets whether we are in ultra-hasty on the move drawing mode
     */
    public boolean getMovingDrawMode(){
        return doingMoveDraw;
    }

//      /**
//       * Sets the screen scaling factor for zooming.
//       *
//       * @param ss the screenscale factor
//       */
//      public static void setScreenScale(float ss) {
//          ScreenScale = ss;
//      }

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
    static void matmult(Matrix3D m) {
        mat.mult(m);
    }
    static void matscale(float xs, float ys, float zs) {
        mat.scale(xs, ys, zs);
    }
    static void mattranslate(float xt, float yt, float zt) {
        mat.translate(xt, yt, zt);
    }
    static void matunit() {
        mat.unit();
    }
    
    /**
     * Constructor for a ChemFrame with a known number of atoms.
     *
     * @param na the number of atoms in the frame
     */
    public ChemFrame(int na) {
        this.maxvert = na;
        frameProps = new Vector();
        atomProps = new Vector();
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
        atomProps = new Vector();
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
     * returns a Vector containing the list of Physical Properties
     * associated with this frame
     */
    public Vector getFrameProps() {
        return frameProps;
    }

    /**
     * Adds a PhysicalProperty to this ChemFrame
     *
     * @param property the PhysicalProperty to be added.
     */
    public void addFrameProperty(PhysicalProperty property) {
        String desc = property.getDescriptor();

        // Make sure we don't have an identical property already defined:

        boolean found = false;
        for (Enumeration e = frameProps.elements() ; e.hasMoreElements() ;) {
            PhysicalProperty fp = (PhysicalProperty) (e.nextElement());
            String fpd = fp.getDescriptor();
            if (desc.equals(fpd)) found = true;
        }

        if (!found) frameProps.addElement(property);
    }

    /** 
     * returns a Vector containing the list of PhysicalProperty descriptors
     * present for the atoms in this frame
     */
    public Vector getAtomProps() {
        return atomProps;
    }

    /**
     * Sets the information label for the frame
     *
     * @param info the information label for this frame
     */            
    public void setInfo(String info) {
        this.info = info;
    }

    public void updateMlists(Vector dlist, Vector alist, Vector dhlist) {
        this.dlist = dlist;
        this.alist = alist;
        this.dhlist = dhlist;
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
    public int addVert(String name, float x, float y, float z) throws Exception {
        return addVert(new AtomType(BaseAtomType.get(name)), x, y, z);
    }

    /**
     * Adds an atom to the frame and finds all bonds between the 
     * new atom and pre-existing atoms in the frame
     *
     * @param type atom type for the new atom
     * @param x the x coordinate of the new atom
     * @param y the y coordinate of the new atom
     * @param z the z coordinate of the new atom
     */                
    public int addVert(AtomType type, float x, float y, float z) throws Exception {
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

        atoms[i] = type;
        nBpA[i] = 0;
        aProps[i] = new Vector();
                      
        for (int j = 0; j < i ; j++ ) {
            float d2 = 0.0f;
            float dx = vert[3*j] - x;
            float dy = vert[3*j+1] - y;
            float dz = vert[3*j+2] - z;
            d2 += dx*dx + dy*dy + dz*dz;
            AtomType b = atoms[j];
            float dr = bondFudge*((float) type.getBaseAtomType().getCovalentRadius() + 
                                  (float) b.getBaseAtomType().getCovalentRadius());
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
                        System.arraycopy(bondDrawn, 0, bd, 0, 
                                         bondDrawn.length);
                        bondDrawn = bd;
                        int be1[] = new int[maxbonds];
                        System.arraycopy(bondEnd1, 0, be1, 0, bondEnd1.length);
                        bondEnd1 = be1;
                        int be2[] = new int[maxbonds];
                        System.arraycopy(bondEnd2, 0, be2, 0, bondEnd2.length);
                        bondEnd2 = be2;
                    }
                Bond bond = new Bond(type, b);
                bonds[k] = bond;
                bondEnd1[k] = i;
                bondEnd2[k] = j;
                
                int na = nBpA[i] + 1;
                int nb = nBpA[j] + 1;

                if (na >= maxbpa) throw new JmolException("ChemFrame.rebond",
                                                            "max bonds per atom exceeded");
                if (nb >= maxbpa) throw new JmolException("ChemFrame.rebond",
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
     * @param atomicNumber the atomicNumber of the extended atom type for the new atom
     * @param x the x coordinate of the new atom
     * @param y the y coordinate of the new atom
     * @param z the z coordinate of the new atom
     */                
    public int addVert(int atomicNumber, float x, float y, float z) throws Exception {
        BaseAtomType baseType = BaseAtomType.get(atomicNumber);
        if (baseType == null) {
            return -1;
        }
        return addVert(new AtomType(baseType), x, y, z);
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

        hasAtomProperties = true;
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
            
            // Update the atomProps if we found a new property
            if (atomProps.indexOf(desc) < 0) {
                atomProps.addElement(desc);
            }
            
        }
        return i;
    }

    /**
     * Adds a PhysicalProperty to the atom at the specified index.
     *
     * @param vertexIndex index of the vertex to which property is added.
     * @param property the PhysicalProperty to be added.
     */
    public void addProperty(int vertexIndex, PhysicalProperty property) {
        aProps[vertexIndex].addElement(property);
        String desc = property.getDescriptor();
        if (desc.equals("Vector") && property instanceof VProperty) {
            hasVectors = true;
            VProperty vp = (VProperty) property;
            double[] vtmp = new double[3];
            vtmp = vp.getVector();
            int k = vertexIndex*3;
            vect[k] = (float)(vert[k] + vtmp[0]);
			vect[k+1] = (float)(vert[k+1] + vtmp[1]);
			vect[k+2] = (float)(vert[k+2] + vtmp[2]);
        }
        // Update the atomProps if we found a new property
        if (atomProps.indexOf(desc) < 0) {
            atomProps.addElement(desc);
        }
    }
    
    /** 
     * returns the number of atoms in the ChemFrame
     */
    public int getNvert() {
        return nvert;
    }

    /**
     * returns the AtomType of the i'th atom
     * 
     * @param i the index of the atom
     */
    public AtomType getAtomType(int i) {
        return atoms[i] ;
    }

    /**
     * returns the coordinates of the i'th atom
     * 
     * @param i the index of the atom
     */
    public double[] getVertCoords(int i) {
        int k = i*3;
        double[] coords = {vert[k], vert[k+1], vert[k+2]};
        return coords;
    }

    /**
     * returns the properties of the i'th atom
     * 
     * @param i the index of the atom
     */
    public Vector getVertProps(int i) {
        Vector prps = aProps[i];
        return prps;
    }

    /** 
     * Transform all the points in this model
     */
    public void transform() {
        if (nvert <= 0)
            return;
        if (tvert == null || tvert.length < nvert * 3)
            tvert = new int[nvert * 3];
        mat.transform(vert, tvert, nvert);
        if (hasVectors) {
            if (tvect == null || tvect.length < nvert * 3)
                tvect = new int[nvert * 3];
            mat.transform(vect, tvect, nvert);
        }
    }
    
    /** 
     * Paint this model to a graphics context.  It uses the matrix
     * associated with this model to map from model space to screen
     * space.  
     *
     * @param g the Graphics context to paint to
     */
    public synchronized void paint(Graphics g, DisplaySettings settings) {
        if (vert == null || nvert <= 0)
            return;
        transform();
        int v[] = tvert;
        int zs[] = ZsortMap;
        if (zs == null) {
            ZsortMap = zs = new int[nvert];
            for (int i = nvert; --i >= 0;)
                zs[i] = i * 3;
        }

//Added by T.GREY for quick-draw on move support
        if (!doingMoveDraw){
            
            /*
             * I use a bubble sort since from one iteration to the next, the sort
             * order is pretty stable, so I just use what I had last time as a
             * "guess" of the sorted order.  With luck, this reduces O(N log N)
             * to O(N)
             */
            
            for (int i = nvert - 1; --i >= 0;) {
                boolean flipped = false;
                for (int j = 0; j <= i; j++) {
                    int a = zs[j];
                    int b = zs[j + 1];
                    if (v[a + 2] > v[b + 2]) {
                        zs[j + 1] = a;
                        zs[j] = b;
                        flipped = true;
                    }
                }
                if (!flipped)
                    break;
            }
        }
        int lg = 0;
        int lim = nvert;
        AtomType ls[] = atoms;
        if (lim <= 0 || nvert <= 0)
            return;

        for (int k = 0; k < nbonds; k++) {
            bondDrawn[k] = false;
        }

        for (int i = 0; i < lim; i++) {
            int j = zs[i];
            if (settings.getShowBonds()) {
                int na = nBpA[j/3];
                for (int k = 0; k < na; k++) {
                    int which = inBonds[j/3][k];
                    if (!bondDrawn[which]) {
                        int l;
                        if (bondEnd1[which] == j/3) {
                            l = 3 * bondEnd2[which];
                            bonds[which].paint(g, settings,
                                               v[j], v[j+1], v[j+2],
                                               v[l], v[l+1], v[l+2],
                                               doingMoveDraw);
                        } else {
                            l = 3 * bondEnd1[which];
                            bonds[which].paint(g, settings,
                                               v[l], v[l+1], v[l+2],
                                               v[j], v[j+1], v[j+2],
                                               doingMoveDraw);
                        }
                        
                    }
                }
            }
            //Added by T.GREY for quick-draw on move support
            if (settings.getShowAtoms() && !doingMoveDraw)
                atoms[j/3].paint(g, settings, v[j], v[j + 1], v[j + 2], j/3 + 1, 
                                 aProps[j/3], pickedAtoms[j/3]);
            
            if (settings.getShowVectors() && hasVectors) {
                ArrowLine al = new ArrowLine(g, v[j], v[j+1], 
                                             tvect[j], tvect[j+1], 
                                             false, true,
                                             0, 
                                             3+(int)(tvect[j+2]/settings.getVectorScreenScale()));
            }
            
        }

        if (dlist != null) {
            for (Enumeration e = dlist.elements() ; e.hasMoreElements() ;) {
                Distance d = (Distance) e.nextElement();
                int[] al = d.getAtomList();
                int l = 3*al[0];
                int j = 3*al[1];
                try {
                    d.paint(g, settings, v[l], v[l+1], v[l+2], v[j], v[j+1], v[j+2]);
                } catch (Exception ex) {}
            }
        }
        if (alist != null) {
            for (Enumeration e = alist.elements() ; e.hasMoreElements() ;) {
                Angle an = (Angle) e.nextElement();
                int[] al = an.getAtomList();
                int l = 3*al[0];
                int j = 3*al[1];
                int k = 3*al[2];
                try {
                    an.paint(g, settings, v[l], v[l+1], v[l+2], v[j], v[j+1], v[j+2],
                             v[k], v[k+1], v[k+2]);
                } catch (Exception ex) {}
            }                        
        }
        if (dhlist != null) {
            for (Enumeration e = dhlist.elements() ; e.hasMoreElements() ;) {
                Dihedral dh = (Dihedral) e.nextElement();
                int[] dhl = dh.getAtomList();
                int l = 3*dhl[0];
                int j = 3*dhl[1];
                int k = 3*dhl[2];
                int m = 3*dhl[3];
                try {
                    dh.paint(g, settings, v[l], v[l+1], v[l+2], v[j], v[j+1], v[j+2],
                             v[k], v[k+1], v[k+2], v[m], v[m+1], v[m+2]);
                } catch (Exception ex) {}
            }                        
        }
    }

    /**
     * return a Vector with selected atoms
     */
    public Vector getSelectedAtoms() {
        Vector result = new Vector();
        for (int i = 0; i < nvert; i++) {
            if (pickedAtoms[i]) {
                result.add(new Integer(i+1));
	    };
        }
        return result;
    }

    /** 
     * Add atom by its number
     */
    public void selectAtomByNumber(int atom) {
        if (nvert <= 0 || atom > nvert) return;	
        pickedAtoms[atom-1] = true;
        napicked++;
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

    public int pickMeasuredAtom(int x, int y) {        
        return getNearestAtom(x, y);
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
            napicked = 0;
        } else {
            pickedAtoms[smallest] = true; 
            napicked = 1;
        }
        for (int i = 0; i < nvert; i++) {
            if (i != smallest) pickedAtoms[i] = false;
        }
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
            napicked--;
        } else {
            pickedAtoms[smallest] = true; 
            napicked++;
        }
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
                if (!pickedAtoms[i]) {
                    pickedAtoms[i] = true; 
                    napicked++;
                }
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
                float dr = bondFudge*((float) a.getBaseAtomType().getCovalentRadius() + 
                                  (float) b.getBaseAtomType().getCovalentRadius());
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

  private Vector vibrations = new Vector();
  public void addVibration(Vibration v){
  	vibrations.addElement(v);
  }
  public Vibration getVibration(int index){
  	return (Vibration)vibrations.elementAt(index);
  }
  public int getNumberVibrations(){
  	return vibrations.size();
  }
  public Enumeration getVibrations() {
  	return vibrations.elements();
  }
}
