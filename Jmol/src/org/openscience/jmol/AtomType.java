
/*
 * Copyright 2001 The Jmol Development Team
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.*;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.*;

public class AtomType {
    
    private BaseAtomType baseType;
    private static JPanel jpanel;

    /** color is specified also at atom itself.
     *  if color == null then it uses the color of
     *  the base type
     */
    private Color color = null;

    /**
     * Pool of atom images for shaded renderings.
     */
    private static Hashtable ballImages = new Hashtable();
    
    /**
     * Sets the JPanel where all atoms will be drawn
     *
     * @param jp the JPanel
     */
    public static void setJPanel(JPanel jp) {
        jpanel = jp;
    }
    
    /**
     * Constructor
     *
     * @param name the name of this atom type (e.g. CA for alpha carbon)
     * @param root the root of this atom type (e.g. C for alpha carbon)
     * @param AtomicNumber the atomic number (usually the number of protons of the root)
     * @param mass the atomic mass 
     * @param vdwRadius the van der Waals radius (helps determine drawing size)
     * @param covalentRadius the covalent radius (helps determine bonding)
     * @param Rl red component for drawing colored atoms
     * @param Gl green component for drawing colored atoms
     * @param Bl blue component for drawing colored atoms
     */
    public AtomType(String name, String root, int AtomicNumber, 
             double mass, double vdwRadius, double covalentRadius, 
             int Rl, int Gl, int Bl) {
		baseType = BaseAtomType.get(name, root, AtomicNumber, mass, vdwRadius, covalentRadius, new Color(Rl, Gl, Bl));
    }        

    /**
     * Constructs an AtomType from the another AtomType.
     * @param at atom type 
     */
    public AtomType(AtomType at) {
		baseType = at.baseType;
    }

    /**
     * Constructs an AtomType from the BaseAtomType.
     * @param at base atom type 
     */
    public AtomType(BaseAtomType at) {
		baseType = at;
    }

    private final int blend(int fg, int bg, float fgfactor) {
        return (int) (bg + (fg - bg) * fgfactor);
    }
    
    private static Image SphereSetup(DisplaySettings settings, Color ballColor) {
        float v1[] = new float[3];
        float v2[] = new float[3];
        byte b = 40;
        int i = 2*b + 1;
        int j = -1;
        // Create our own version of an IndexColorModel:
        int model[] = new int[i*i];
        // Normalize the lightsource vector:
        float[] lightsource = normalize(settings.getLightSourceVector());
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
                        R2 = (int)((float)ballColor.getRed()*len2);
                        G2 = (int)((float)ballColor.getGreen()*len2);
                        B2 = (int)((float)ballColor.getBlue()*len2);
                    } else {
                        v2[0] = lightsource[0]+0.0f;
                        v2[1] = lightsource[1]+0.0f;
                        v2[2] = lightsource[2]+1.0f;
                        v2 = normalize(v2);
                        float len3 = v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
                        float len4 = 8.0f * len3*len3 - 7.0f;
                        float len5 = 100.0f * len4;
                        len5 = Math.max(len5, 0.0f);                        
                        R2 = (int)((float)(ballColor.getRed() * 155) * len2 + 100.0 + len5);
                        G2 = (int)((float)(ballColor.getGreen() * 155) * len2 + 100.0 + len5);
                        B2 = (int)((float)(ballColor.getBlue() * 155) * len2 + 100.0 + len5);
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
        return jpanel.createImage(new MemoryImageSource(i, i, model, 0, i));
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
    public void paint(Graphics gc, DisplaySettings settings, 
                      int x, int y, int z, int n, 
                      Vector props, boolean picked) {
        // check wether this atom has an atom specified color
        if (color == null) {
	    color = baseType.getColor();
	}
        int diameter = (int) (2.0f*settings.getCircleRadius(z, baseType.getVdwRadius()));
        int radius = diameter >> 1;

        if (picked) {
            int halo = radius + 5;
            int halo2 = 2 * halo;
            gc.setColor(settings.getPickedColor());
            gc.fillOval(x - halo, y - halo, halo2, halo2);
        }
        switch( settings.getAtomDrawMode() ) {
        case DisplaySettings.WIREFRAME:
            gc.setColor(color);
            gc.drawOval(x - radius, y - radius, diameter, diameter);
            break;
        case DisplaySettings.SHADING:
            Image shadedImage;
            if (ballImages.containsKey(color)) {
                shadedImage = (Image)ballImages.get(color);
            } else {
                shadedImage = SphereSetup(settings, color);
                ballImages.put(color, shadedImage);
            }
            gc.drawImage(shadedImage, x - radius, y - radius, diameter,
                         diameter, jpanel);
            break;
        default:
            gc.setColor(color);
            gc.fillOval(x - radius, y - radius, diameter, diameter);
            gc.setColor(settings.getOutlineColor());
            gc.drawOval(x - radius, y - radius, diameter, diameter);
            break;
        }

        if (settings.getLabelMode() != DisplaySettings.NOLABELS) {
            int j = 0;
            String s;
            Font font = new Font("Helvetica", Font.PLAIN, radius);
            gc.setFont(font);
            FontMetrics fontMetrics = gc.getFontMetrics(font);
            int k = fontMetrics.getAscent();
            gc.setColor(settings.getTextColor());
                
            switch( settings.getLabelMode() ) {
            case DisplaySettings.SYMBOLS:
                j = fontMetrics.stringWidth(baseType.getRoot());
                gc.drawString(baseType.getRoot(), x-j/2, y+k/2); 
                break;
            case DisplaySettings.TYPES:
                j = fontMetrics.stringWidth(baseType.getName());
                gc.drawString(baseType.getName(), x-j/2, y+k/2);
                break;
            case DisplaySettings.NUMBERS:
                s = new Integer(n).toString();
                j = fontMetrics.stringWidth(s);
                gc.drawString(s, x-j/2, y+k/2);
                break;
            default:
                break;
            }
        }

        if (!settings.getPropertyMode().equals("")) {
            // check to make sure this atom has this property:
            for (int i = 0; i < props.size(); i++) {
                PhysicalProperty p = (PhysicalProperty)props.elementAt(i);
                if (p.getDescriptor().equals(settings.getPropertyMode())) {
                    // OK, we had this property.  Let's draw the value on 
                    // screen:
                    Font font = new Font("Helvetica", Font.PLAIN, radius/2);
                    gc.setFont(font);
                    gc.setColor(settings.getTextColor());
                    String s = p.stringValue();
                    if (s.length() > 5) 
                        s = s.substring(0,5);
                    int k = 2 + (int) (radius/1.4142136f);
                    gc.drawString(s, x+k, y-k);
                }
            }
        }
        
    }
    
    private static float[] normalize(float v[]) {
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

    public BaseAtomType getBaseAtomType() {
	return baseType;
    }

    /** 
     * return atom specific color. If not given, return
     * default color
     **/
    public Color getColor() {
        if (color != null) {
	    return this.color;
	} else {
	    return baseType.getColor();
	}
    }

    /** 
     * set atom specific color
     **/
    public void setColor(Color c) {
	this.color = c;
    }
}
