/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
package org.openscience.jmol.util;

import java.util.StringTokenizer;
import java.util.Vector;   //VEC
import java.lang.reflect.Array;
import javax.swing.JTextField; //TXF
import javax.swing.JOptionPane;

public class FieldReader {
  
  /**
   * Read a vector of text fields of the form "double, double, double".
   */
  static public double[][] readField3(Vector jTextField)
    throws NumberFormatException {
    
    StringTokenizer st;
    String sn;
    double matrix[][] = new double[jTextField.size()][3] ;
    
    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
			       .getText(), ",");
      for (int j = 0; j < 3; j++) {
	if (st.hasMoreTokens()) {
	  sn = st.nextToken();
	  matrix[i][j] = Double.parseDouble(sn);
	}
      }
    }
    return matrix;
  }    //end readField3(...)

  /**
   * Read a text fields of the form "double, double, double".
   */
  static public double[] readField3(JTextField jTextField)
    throws NumberFormatException {

    StringTokenizer st;
    String sn;
    double matrix[] = new double[3] ;

    
    st = new StringTokenizer(jTextField.getText(), ",");
    for (int j = 0; j < 3; j++) {
      if (st.hasMoreTokens()) {
	sn = st.nextToken();
	  matrix[j] = Double.parseDouble(sn);
      }
    }
    
    return matrix;
  }    //end readField3(...)

  /**
   * Read a vector of text fields of the form "double".
   */
  static public double[] readField1(Vector jTextField)
    throws NumberFormatException {
      
    StringTokenizer st;
    String sn;
    int dim = jTextField.size();
    double vect[] = (double[]) Array.newInstance(double.class, dim);

    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
			       .getText(), ",");

      if (st.hasMoreTokens()) {
	sn = st.nextToken();
	  vect[i] = Double.parseDouble(sn);
      }

    }
    return vect;
  }    //end readField1(...)

  /**
   * Read a  text fields of the form "double".
   */
  static public double readField1(JTextField jTextField)
    throws NumberFormatException{
      
    double value=0;
    StringTokenizer st;
    String sn;
    
    st = new StringTokenizer(jTextField.getText(), ",");
    
    if (st.hasMoreTokens()) {
      sn = st.nextToken();
      value = Double.parseDouble(sn);
    }
    return value;
  }    //end readField1(...)


  static protected void errorDialog(String s) {
    
    JOptionPane.showMessageDialog
      (null, s, "alert", JOptionPane.ERROR_MESSAGE);
    
  }    //end errorDialog
  
  
}
