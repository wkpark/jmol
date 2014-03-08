/* Copyright (c) 2008-2009 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'visible.java' - module to predict colour of visible spectrum
// created July 2008 based on concept published by Darren L. Williams
// in J. Chem. Educ., 84(11), 1873-1877, 2007
//
// Judd-Vos-modified 1931 CIE 2-deg color matching functions (1978)
// The CIE standard observer functions were curve fitted using FitYK
// and the equations used for these calculations. The values obtained
// do not seem to vary appreciably from those published in the JChemEd article

package jspecview.common;

import java.lang.Math;

import jspecview.api.VisibleInterface;

/**
 * Visible class - for prediction of colour from visible spectrum
 * 
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class Visible implements VisibleInterface {

  public Visible() {
  	// for reflection
  }
  
  @Override
	public String getColour(Coordinate xyCoords[], String Yunits) {
    int ind400 = 0, ind437 = 0, ind499 = 0, ind700 = 0;
    for (int i = 0; i < xyCoords.length; i++) {
      if (xyCoords[i].getXVal() < 401) {
        ind400 = i;
      }
      if (xyCoords[i].getXVal() < 438) {
        ind437 = i;
      }
      if (xyCoords[i].getXVal() < 500) {
        ind499 = i;
      }
      if (xyCoords[i].getXVal() < 701) {
        ind700 = i;
      }
    }

    double firstX = xyCoords[0].getXVal();
    double lastX = xyCoords[xyCoords.length - 1].getXVal();

    double matrixx[] = new double[1000], matrixy[] = new double[1000];
    double matrixz[] = new double[1000], matrixcie[] = new double[1000];

    if ((ind700 - ind400) <= 30 || firstX >= 401 || lastX <= 699)
      return null;
    // treat the x-bar and CIE D65 curves in two parts with the changeover at 499 nm
    for (int i = ind400; i < ind437; i++) {
      double x = xyCoords[i].getXVal();
      matrixx[(i - ind400)] = 0.335681 * Math.exp(-0.000998224
          * (Math.pow((x - 441.96), 2)));
      matrixy[(i - ind400)] = 1.01832 * Math.exp(-0.000284660
          * (Math.pow((x - 559.04), 2)));
      matrixz[(i - ind400)] = 1.63045 * Math.exp(-0.001586000
          * (Math.pow((x - 437.406), 2)));
      matrixcie[(i - ind400)] = 115.195 * Math.exp(-8.33988E-05
          * (Math.pow((x - 472.727), 2)));
    }
    for (int i = ind437; i < ind499; i++) { //change over at 437nm for z
      double x = xyCoords[i].getXVal();
      matrixx[(i - ind400)] = 0.335681 * Math.exp(-0.000998224
          * (Math.pow((x - 441.96), 2)));
      matrixy[(i - ind400)] = 1.01832 * Math.exp(-0.00028466
          * (Math.pow((x - 559.04), 2)));
      matrixz[(i - ind400)] = 1.63045 * Math.exp(-0.00043647
          * (Math.pow((x - 437.406), 2)));
      matrixcie[(i - ind400)] = 115.195 * Math.exp(-8.33988E-05
          * (Math.pow((x - 472.727), 2)));
    }
    for (int i = ind499; i < ind700; i++) { //change over at 500nm for x and CIE-D65
      double x = xyCoords[i].getXVal();
      matrixx[(i - ind400)] = 1.05583 * Math.exp(-0.00044156
          * (Math.pow((x - 596.124), 2)));
      matrixy[(i - ind400)] = 1.01832 * Math.exp(-0.00028466
          * (Math.pow((x - 559.04), 2)));
      matrixz[(i - ind400)] = 1.63045 * Math.exp(-0.00043647
          * (Math.pow((x - 437.406), 2)));
      matrixcie[(i - ind400)] = 208.375 - (0.195278 * x);
    }

    double xup = 0, yup = 0, zup = 0, xdwn = 0, ydwn = 0, zdwn = 0;

    if (Yunits.toLowerCase().contains("trans"))
      for (int i = ind400; i < ind700; i++) {
        double y = xyCoords[i].getYVal();
        xup += (y * matrixx[(i - ind400)] * matrixcie[(i - ind400)]);
        xdwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        yup += (y * matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        ydwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        zup += (y * matrixz[(i - ind400)] * matrixcie[(i - ind400)]);
        zdwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
      }
    else
      for (int i = ind400; i <= ind700; i++) {
        double y = -Math.max(xyCoords[i].getYVal(), 0);
        xup += (Math.pow(10, y) * matrixx[(i - ind400)] * matrixcie[(i - ind400)]);
        xdwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        yup += (Math.pow(10, y) * matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        ydwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        zup += (Math.pow(10, y) * matrixz[(i - ind400)] * matrixcie[(i - ind400)]);
        zdwn += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
      }

    double x = xup / xdwn;
    double y = yup / ydwn;
    double z = zup / zdwn;

//            
//            double sumXYZ = x + y + z;
//            double x1 = (x / (sumXYZ));
//            double y1 = (y / (sumXYZ));
//            double z1 = (z / (sumXYZ));
//            //System.out.println("x1 = "+x1+", y1 = "+y1+", z1 = "+z1);

    double matrixRGB[] = new double[] {
        (x * 3.241) + (y * (-1.5374)) + (z * (-0.4986)),
        (x * (-0.9692)) + (y * 1.876) + (z * 0.0416),
        (x * 0.0556) + (y * (-0.204)) + (z * 1.057) };

    for (int i = 0; i < 3; i++)
      matrixRGB[i] = (matrixRGB[i] > 0.00304 ? (1.055 * (Math.pow(matrixRGB[i],
          1 / 2.4))) - 0.055 : 12.92 * matrixRGB[i]);

    int red = fix(matrixRGB[0]);
    int green = fix(matrixRGB[1]);
    int blue = fix(matrixRGB[2]);

//    
//            String redv = "" + ("0123456789ABCDEF".charAt( (red - red % 16) / 16)) +
//                ("0123456789ABCDEF".charAt(red % 16));
//            String greenv = "" + ("0123456789ABCDEF".charAt( (green - green % 16) / 16)) +
//                ("0123456789ABCDEF".charAt(green % 16));
//            String bluev = "" + ("0123456789ABCDEF".charAt( (blue - blue % 16) / 16)) +
//                ("0123456789ABCDEF".charAt(blue % 16));
//            //System.out.println("#"+ redv + greenv + bluev);
//    

    return ("" + red + "," + green + "," + blue);
  }

  private static int fix(double d) {
    return (d <= 0 ? 0 : d >= 1 ? 255 : (int) Math.round(255 * d));
  }

}
