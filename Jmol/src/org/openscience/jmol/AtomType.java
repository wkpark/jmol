/*
 * @(#)AtomType.java    1.0 98/08/27
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
/*
 * inspired in part by James Gosling's XYZApp applet
 */

package org.openscience.jmol;

import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.*;
import java.util.Vector;
import javax.swing.*;

public class AtomType {

    private static JPanel jpanel;
    /* 
       Place the light source for shaded atoms to the upper right of the
       atoms and out of the plane:
    */
    private static float lightsource[] = { 1.0f, -1.0f, 2.0f};

    private static float screenScale;
    private static int zOffset;
    private static float depthFactor = 0.33f;
    /**
     * QUICKDRAW mode draws a filled circle
     */
    public static final int QUICKDRAW = 0;
    /**
     * SHADING mode draws a lighted sphere
     */
    public static final int SHADING = 1;
    /**
     * WIREFRAME mode draws a transparent circle
     */
    public static final int WIREFRAME = 2;
    private static int DrawMode = QUICKDRAW;
    public static final int NOLABELS = 0;
    public static final int SYMBOLS = 1;
    public static final int TYPES = 2;
    public static final int NUMBERS = 3;
    private static int LabelMode = NOLABELS;
    private static String PropsMode = "";
    private static boolean drawText = false;
    private static Color outlineColor = Color.black;
    private static Color pickedColor  = Color.orange;
    private static Color textColor = Color.black;
    private static double sphereFactor = 0.2;  /* static vars, once for each 
                                                  class, not once per instance
                                                  of class */
    private String name, root;
    private int AtomicNumber;
    private double mass;
    private double vdWRadius;
    private double covalentRadius;    
    private int Rl;
    private int Gl;
    private int Bl;
    private Image ball;
    private Color col;
    
    /**
     * Sets the JPanel where all atoms will be drawn
     *
     * @param jp the JPanel
     */
    public static void setJPanel(JPanel jp) {
        jpanel = jp;
    }
    
    /**
     * Sets the scale at which the molecules will be drawn
     *
     * @param ss the screen scale
     */
    public static void setScreenScale(float ss) {
        screenScale = ss;
    }

    public static void setZoffset(int z) {
        zOffset = z;
    }

    public static void setDepthFactor(float df) {
        depthFactor = df;
    }

    public static void setSphereFactor(double sf) {
        sphereFactor = sf;
    }    

    public static double getSphereFactor() {
        return sphereFactor;
    }

    public static void setQuickDraw() {
        DrawMode = QUICKDRAW;
    }

    public static void setShading() {
        DrawMode = SHADING;
    }

    public static void setWireFrame() {
        DrawMode = WIREFRAME;
    }

    public static void setRenderMode(int i) {
        DrawMode = i;
    }

    public static void setLabelMode(int i) {
        LabelMode = i;
    }

    /**
     * sets which physical property we want to interrogate on-screen:
     *
     * @param s The string descriptor of the physical property.
     */
    public static void setPropsMode(String s) {
        PropsMode = s;
    }

    public static void setOutlineColor(Color c) {
        outlineColor = c;
    }

    public static void setPickedColor(Color c) {
        pickedColor = c;
    }

    public static void setTextColor(Color c) {
        textColor = c;
    }

    public static int getRenderMode() {
        return DrawMode;
    }

    public static int getLabelMode() {
        return LabelMode;
    }

    /*
     * returns the current value of the physical property drawing mode
     */
    public static String getPropsMode() {
        return PropsMode;
    }

    /**
     * Constructor
     *
     * @param name the name of this atom type (e.g. CA for alpha carbon)
     * @param root the root of this atom type (e.g. C for alpha carbon)
     * @param AtomicNumber the atomic number (usually the number of protons of the root)
     * @param mass the atomic mass 
     * @param vdWRadius the van der Waals radius (helps determine drawing size)
     * @param covalentRadius the covalent radius (helps determine bonding)
     * @param Rl red component for drawing colored atoms
     * @param Gl green component for drawing colored atoms
     * @param Bl blue component for drawing colored atoms
     */
    public AtomType(String name, String root, int AtomicNumber, 
             double mass, double vdWRadius, double covalentRadius, 
             int Rl, int Gl, int Bl) {
        this.name = name;
        this.root = root;
        this.AtomicNumber = AtomicNumber;
        this.mass = mass;
        this.vdWRadius = vdWRadius;
        this.covalentRadius = covalentRadius;
        this.Rl = Rl;
        this.Gl = Gl;
        this.Bl = Bl;
        this.col = new Color(Rl, Gl, Bl);
    }        

    /**
     * Constructor
     *
     * @param Rl red component for drawing colored atoms
     * @param Gl green component for drawing colored atoms
     * @param Bl blue component for drawing colored atoms
     */
    public AtomType(int Rl, int Gl, int Bl) {
        this.Rl = Rl;
        this.Gl = Gl;
        this.Bl = Bl;
        this.col = new Color(Rl, Gl, Bl);
    }

    private final int blend(int fg, int bg, float fgfactor) {
        return (int) (bg + (fg - bg) * fgfactor);
    }
    
    private void SphereSetup() {        
        float v1[] = new float[3];
        float v2[] = new float[3];
        byte b = 40;
        int i = 2*b + 1;
        int j = -1;
        // Create our own version of an IndexColorModel:
        int model[] = new int[i*i];
        // Normalize the lightsource vector:
        lightsource = normalize(lightsource);
        for (int k1 = -b; k1 <= b; k1++) {
            for (int k2 = -b; k2 <= b; k2++) {
                j++;
                v1[0] = k2;
                v1[1] = k1;
                float len1 = (float) Math.sqrt(k2*k2 + k1*k1);
                if (len1 <=b) {
                    int R2 = 0;
                    int G2 = 0;
                    int B2 = 0;
                    v1[2] = (float)b * (float)Math.cos(Math.asin(len1/b));
                    v1 = normalize(v1);
                    float len2 = (float)Math.abs((double)(v1[0]*lightsource[0] 
                                                          +v1[1]*lightsource[1]
                                                          +v1[2]*lightsource[2]
                                                          ));
                    if (len2 < 0.995f) {
                        R2 = (int)((float)Rl*len2);
                        G2 = (int)((float)Gl*len2);
                        B2 = (int)((float)Bl*len2);
                    } else {
                        v2[0] = lightsource[0]+0.0f;
                        v2[1] = lightsource[1]+0.0f;
                        v2[2] = lightsource[2]+1.0f;
                        v2 = normalize(v2);
                        float len3 = v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
                        float len4 = 8.0f * len3*len3 - 7.0f;
                        float len5 = 100.0f * len4;
                        len5 = Math.max(len5, 0.0f);                        
                        R2 = (int)((float)(Rl * 155) * len2 + 100.0 + len5);
                        G2 = (int)((float)(Gl * 155) * len2 + 100.0 + len5);
                        B2 = (int)((float)(Bl * 155) * len2 + 100.0 + len5);
                        R2 = Math.min(R2, 255);
                        G2 = Math.min(G2, 255);
                        B2 = Math.min(B2, 255);
                    }
                    // Bitwise masking to make model:
                    model[j] = -16777216 | R2 << 16 | G2 << 8 | B2;
                } else 
                    model[j] = 0;
            }            
        }
        ball = jpanel.createImage(new MemoryImageSource(i, i, model, 0, i));
    }
    
    /**
     * Draws the atom on a particular graphics context
     *
     * @param gc the Graphics context
     * @param x x position of the atom in screen space
     * @param y y position of the atom in screen space
     * @param z z position (helps in perspective and depth cueing)
     * @param n atom location in the input file
     * @param props Vector containing the physical properties associated with this atom
     * @param picked whether or not the atom has been selected and gets a "halo"
     *
     */
    public void paint(Graphics gc, int x, int y, int z, int n, 
                      Vector props, boolean picked) {
        int diameter = (int) (2.0f*getCircleRadius(z));
        int radius = diameter >> 1;

        if (picked) {
            int halo = radius + 5;
            int halo2 = 2 * halo;
            gc.setColor(pickedColor);
            gc.fillOval(x - halo, y - halo, halo2, halo2);
        }
        switch( DrawMode ) {
        case WIREFRAME:
            gc.setColor(col);
            gc.drawOval(x - radius, y - radius, diameter, diameter);
            break;
        case SHADING:
            Image ba = ball;
            if (ba == null) {
                SphereSetup();
                ba = ball;
            }
            Image i = ba;
            gc.drawImage(i, x - radius, y - radius, diameter, diameter, 
                         jpanel);
            break;
        default:
            gc.setColor(col);
            gc.fillOval(x - radius, y - radius, diameter, diameter);
            gc.setColor(outlineColor);
            gc.drawOval(x - radius, y - radius, diameter, diameter);
            break;
        }

        if (LabelMode != NOLABELS) {
            int j = 0;
            String s;
            Font font = new Font("Helvetica", Font.PLAIN, radius);
            gc.setFont(font);
            FontMetrics fontMetrics = gc.getFontMetrics(font);
            int k = fontMetrics.getAscent();
            gc.setColor(textColor);
                
            switch( LabelMode ) {
            case SYMBOLS:
                j = fontMetrics.stringWidth(root);
                gc.drawString(root, x-j/2, y+k/2); 
                break;
            case TYPES:
                j = fontMetrics.stringWidth(name);
                gc.drawString(name, x-j/2, y+k/2);
                break;
            case NUMBERS:
                s = new Integer(n).toString();
                j = fontMetrics.stringWidth(s);
                gc.drawString(s, x-j/2, y+k/2);
                break;
            default:
                break;
            }
        }

        if (!PropsMode.equals("")) {
            // check to make sure this atom has this property:
            for (int i = 0; i < props.size(); i++) {
                PhysicalProperty p = (PhysicalProperty)props.elementAt(i);
                if (p.getDescriptor().equals(PropsMode)) {
                    // OK, we had this property.  Let's draw the value on 
                    // screen:
                    Font font = new Font("Helvetica", Font.PLAIN, radius/2);
                    gc.setFont(font);
                    gc.setColor(textColor);
                    String s = p.stringValue();
                    if (s.length() > 5) 
                        s = s.substring(0,5);
                    int k = 2 + (int) (radius/1.4142136f);
                    gc.drawString(s, x+k, y-k);
                }
            }
        }
        
    }
    
    /**
     * returns the name of this AtomType
     */
    public String getName() {
        return name;
    }

    /**
     * returns the root of this AtomType
     */
    public String getRoot() {
        return root;
    }
    
    /**
     * returns the Atomic Number of this AtomType
     */
    public int getAtomicNumber() {
        return AtomicNumber;
    }

    /**
     * returns the mass of this AtomType
     */
    public double getMass() {
        return mass;
    }

    /**
     * returns the covalent radius of this AtomType
     */
    public double getCovalentRadius() {
        return covalentRadius;
    }
    
    /**
     * returns the Van derWaals radius of this AtomType
     */
    public double getvdWRadius() {
        return vdWRadius;
    }

    /**
     * returns the color of this AtomType
     */
    public Color getColor() {
        return col;
    }

    /**
     * sets the name of this AtomType
     *
     * @param n the Name
     */
    public void setName(String n) {
        this.name = n;
    }

    /**
     * sets the root of this AtomType
     *
     * @param r the root
     */
    public void setRoot(String r) {
        this.root = r;
    }
    
    /**
     * sets the Atomic Number of this AtomType
     *
     * @param an the AtomicNumber
     */
    public void setAtomicNumber(int an) {
        this.AtomicNumber = an;
    }

    /**
     * sets the mass of this AtomType
     *
     * @param m the mass
     */
    public void setMass(double m) {
        this.mass = m;
    }

    /**
     * sets the covalent radius of this AtomType
     *
     * @param cr the covalent radius
     */
    public void setCovalentRadius(double cr) {
        this.covalentRadius = cr;
    }
    
    /**
     * sets the Van derWaals radius of this AtomType
     *
     * @param vr the Van derWaals Radius
     */
    public void setvdWRadius(double vr) {
        this.vdWRadius = vr;
    }

    /**
     * sets the color of this AtomType
     *
     * @param c the Color
     */
    public void setColor(Color c) {
        this.col = c;
    }


    /**
     * returns the on-screen radius of this Atom
     *
     * @param z z position in screen space
     */ 
    public float getCircleRadius(int z) {
        double raw = vdWRadius*sphereFactor;
        float depth = (float)(z - zOffset) / (2.0f*zOffset);
        float tmp = screenScale * ((float)raw + depthFactor*depth);
        return tmp < 0.0f ? 1.0f : tmp;
    }

    private float[] normalize(float v[]) {
        float len = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        float v2[] = new float[3];
        if (len == 0.0F) {
            v2[0] = 0.0F;
            v2[1] = 0.0F;
            v2[2] = 0.0F;
        } else {
            v2[0] = v[0] / len;
            v2[1] = v[1] / len;
            v2[2] = v[2] / len;
        }
        return v2;
    }

}
