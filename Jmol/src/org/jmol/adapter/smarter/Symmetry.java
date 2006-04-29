/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;
import javax.vecmath.Point4f;
import javax.vecmath.Matrix4f;


// see http://rruff.geo.arizona.edu/AMS/amcsd.php
// Bob Hanson 4/2006

class Symmetry {
  String xyz;
  Matrix4f operation;
  
  Symmetry() { }
  
  boolean setMatrixFromXYZ(String xyz) {
    this.xyz = xyz;
    operation = new Matrix4f();
    float[] temp = new float[16];
    boolean isNegative = false;
    boolean isDenominator = false;
    boolean isDecimal = false;
    char ch;
    int x = 0;
    int y = 0;
    int z = 0;
    float iValue = 0;
    int rowPt = -1;
    float decimalMultiplier = 1f;
    xyz += ",";
    //System.out.println(xyz.length() + " " + xyz);
    for (int i = 0; i < xyz.length(); i++) {
      ch = xyz.charAt(i);
      //System.out.println("char = " + ch + isDecimal);
      switch (ch) {
      case '\'':
      case ' ':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        isDenominator = true;
        continue;
      case 'X':
      case 'x':
        x = (isNegative ? -1 : 1);
        break;
      case 'Y':
      case 'y':
        y = (isNegative ? -1 : 1);
        break;
      case 'Z':
      case 'z':
        z = (isNegative ? -1 : 1);
        break;
      case ',':
        if (++rowPt > 2) {
          System.out.println("Symmetry Operation? " + xyz);
          return false;
        }
        temp[rowPt * 4 + 0] = x;
        temp[rowPt * 4 + 1] = y;
        temp[rowPt * 4 + 2] = z;
        temp[rowPt * 4 + 3] = iValue;
        if (rowPt == 2) {
          operation.set(temp);
          System.out.println("symmetry " + xyz + "\n" + operation);
          rowPt = 0;
          return true;
        }
        x = y = z = 0;
        iValue = 0;
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal)
          continue;
        //allow to pass through
      default:
        //System.out.println(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (isDecimal && ich >= 0 && ich <= 9) {
          decimalMultiplier /= 10f;
          iValue += decimalMultiplier * ich;
          continue;
        }
        if (ich >= 1 && ich <= 9) {
          if (isDenominator) {
            iValue /= ich;
          } else {
            iValue = (isNegative ? -1f : 1f) * ich;
          }
        } else {
          System.out.println("symmetry character?" + ch);
        }
      }
      isDecimal = isNegative = isDenominator = false;
    }
    return false;
  }
  
  void newPoint(Atom atom1, Atom atom2, boolean normalize) {
    Point4f temp = new Point4f(atom1.x, atom1.y, atom1.z, 1);
    operation.transform(temp, temp);
    if (normalize) {
      while (temp.x > 1f)
        temp.x -= 1f;
      while (temp.y > 1f)
        temp.y -= 1f;
      while (temp.z > 1f)
        temp.z -= 1f;
      while (temp.x < 0)
        temp.x += 1f;
      while (temp.y < 0)
        temp.y += 1f;
      while (temp.z < 0)
        temp.z += 1f;
    }
    atom2.x = temp.x;
    atom2.y = temp.y;
    atom2.z = temp.z;
  }
  
  boolean isUnityOperation() {
    Point4f temp = new Point4f(0.12345f, 0.23456f, 0.345678f, 1);
    Point4f temp2 = new Point4f();
    //System.out.println("isUnityOp?" + temp);
    operation.transform(temp, temp2);
    temp2.w += 1.0f;
    return temp.equals(temp2);
    
  }
}
