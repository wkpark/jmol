/*
 * AtomRenderer.java
 * 
 * Copyright (C) 1999  Bradley A. Smith
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.miniJmol;

import org.openscience.jmol.PhysicalProperty;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.vecmath.Point3f;

/**
 * Drawing methods for atoms.
 */
public class AtomRenderer {

    /**
     * Sets the Canvas where all atoms will be drawn.
     *
     * @param c the Canvas
     */
    public static void setCanvas(Canvas c) {
        canvas = c;
    }
    
    /**
     * Creates an AtomRenderer with default parameters.
     */
    public AtomRenderer() {
    }

    /**
     * Draws an atom on a particular graphics context.
     *
     * @param gc the Graphics context
	 * @param atom the atom to be drawn
     * @param picked whether or not the atom has been selected and gets a "halo"
	 * @param settings the display settings
     */
    public void paint(Graphics gc, Atom atom, boolean picked, DisplaySettings settings) {
		int x = (int)atom.getScreenPosition().x;
		int y = (int)atom.getScreenPosition().y;
		int z = (int)atom.getScreenPosition().z;
        int diameter = (int) (2.0f*settings.getCircleRadius(z, atom.getType().getVdwRadius()));
        int radius = diameter >> 1;

        if (picked) {
            int halo = radius + 5;
            int halo2 = 2 * halo;
            gc.setColor(settings.getPickedColor());
            gc.fillOval(x - halo, y - halo, halo2, halo2);
        }
        switch( settings.getAtomDrawMode() ) {
        case DisplaySettings.WIREFRAME:
            gc.setColor(atom.getType().getColor());
            gc.drawOval(x - radius, y - radius, diameter, diameter);
            break;
        case DisplaySettings.SHADING:
			Image shadedImage;
			if (ballImages.containsKey(atom.getType().getColor())) {
				shadedImage = (Image)ballImages.get(atom.getType().getColor());
			} else {
                shadedImage = SphereSetup(atom.getType().getColor(), settings);
				ballImages.put(atom.getType().getColor(), shadedImage);
            }
            gc.drawImage(shadedImage, x - radius, y - radius, diameter,
						 diameter, canvas);
            break;
        default:
            gc.setColor(atom.getType().getColor());
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
                j = fontMetrics.stringWidth(atom.getType().getRoot());
                gc.drawString(atom.getType().getRoot(), x-j/2, y+k/2); 
                break;
            case DisplaySettings.TYPES:
                j = fontMetrics.stringWidth(atom.getType().getName());
                gc.drawString(atom.getType().getName(), x-j/2, y+k/2);
                break;
            case DisplaySettings.NUMBERS:
                s = Integer.toString(atom.getAtomNumber()+1);
                j = fontMetrics.stringWidth(s);
                gc.drawString(s, x-j/2, y+k/2);
                break;
            default:
                break;
            }
        }

        if (!settings.getPropertyMode().equals("")) {
            // check to make sure this atom has this property:
			Enumeration propIter = atom.getProperties().elements();
			while (propIter.hasMoreElements()) {
                PhysicalProperty p = (PhysicalProperty)propIter.nextElement();
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

	/**
	 * Creates a shaded atom image.
	 *
	 * @param ballColor color for the sphere.
	 * @param settings the display settings used for the light source.
	 * @returns an image of a shaded sphere.
	 */    
    private static Image SphereSetup(Color ballColor, DisplaySettings settings) {
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
        return canvas.createImage(new MemoryImageSource(i, i, model, 0, i));
    }

    /**
	 * Returns a normalized vector for the float[3] given.
	 */
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

    /**
     * The Canvas where all atoms will be drawn. This reference will be used for creating
	 * and displaying the images for shaded atom spheres.
     */
    private static Canvas canvas;

	/**
	 * Pool of atom images for shaded renderings.
	 */
    private static Hashtable ballImages = new Hashtable();
    
}
