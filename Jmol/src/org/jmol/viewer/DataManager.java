/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-03 20:53:36 -0500 (Wed, 03 Oct 2007) $
 * $Revision: 8351 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;

import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Parser;

/*
 * a class for storing and retrieving user data,
 * including atom-related and color-related data
 * 
 */

class DataManager {

  private Hashtable dataValues = new Hashtable();

  DataManager() {
  }

  void clear() {
    dataValues.clear();
  }
  
  void setData(String type, Object[] data, int atomCount,
                      int matchField, int field) {
    //Eval
    /*
     * data[0] -- label
     * data[1] -- string or float[]
     * data[2] -- selection bitset or int[] atomMap when field > 0
     * 
     * matchField = data must match atomNo in this column, >= 1
     * field = column containing the data, >= 1:
     *   0 ==> values are a simple list; clear the data
     *   Integer.MAX_VALUE ==> values are a simple list; don't clear the data
     *   Integer.MIN_VALUE ==> one SINGLE data value should be used for all selected atoms
     */
    if (data[2] != null && atomCount > 0) {
      boolean createNew = (matchField != 0 
          || field != Integer.MIN_VALUE && field != Integer.MAX_VALUE);
      Object[] oldData = (Object[]) dataValues.get(type);
      float[] f = (oldData == null || createNew ? new float[atomCount] 
          : ArrayUtil.ensureLength(((float[]) oldData[1]), atomCount));
      String stringData = (String) data[1];
      BitSet bs;
      String[] strData = null;
      if (field == Integer.MIN_VALUE &&
          (strData = Parser.getTokens(stringData)).length > 1)
        field = 0;
      if (field == 0 || field == Integer.MAX_VALUE) {
        bs = (BitSet) data[2];
        if (strData == null)
          strData = Parser.getTokens(stringData);
        Parser.parseFloatArray(strData, bs, f);
      } else if (matchField == 0) {
        bs = (BitSet) data[2];
        Parser.parseFloatArrayFromMatchAndField(stringData, bs, 0, null, field, f);
      } else {
        int[] iData = (int[]) data[2]; 
        bs = new BitSet();
        Parser.parseFloatArrayFromMatchAndField(stringData, null, matchField, iData, field, f);
        for (int i = iData.length; --i >= 0; )
          if (iData[i] >= 0)
            bs.set(iData[i]);
      }
      if (oldData != null && oldData[2] instanceof BitSet && !createNew)
        bs.or((BitSet)(oldData[2]));
      data[2] = bs;
      data[1] = f;
    }
    dataValues.put(type, data);
  }

  Object[] getData(String type) {
    if (dataValues == null)
      return null;
    if (type.equalsIgnoreCase("types")) {
      String[] info = new String[2];
      info[0] = "types";
      info[1] = "";
      int n = 0;
      Enumeration e = (dataValues.keys());
      while (e.hasMoreElements())
        info[1] += (n++ == 0 ? "," : "") + e.nextElement();
      return info;
    }
    return (Object[]) dataValues.get(type);
  }

  float[] getDataFloat(String label) {
    if (dataValues == null)
      return null;
    Object[] data = getData(label);
    if (data == null || !(data[1] instanceof float[]))
      return null;
    return (float[]) data[1];
  }

  float getDataFloat(String label, int atomIndex) {
    if (dataValues != null) {
      Object[] data = getData(label);
      if (data != null && data[1] instanceof float[]) {
        float[] f = (float[]) data[1];
        if (atomIndex < f.length)
          return f[atomIndex];
      }
    }
    return Float.NaN;
  }

  void getDataState(StringBuffer s, StringBuffer sfunc, Atom[] atoms,
                    int atomCount, String atomProps) {
    if (dataValues == null)
      return;
    Enumeration e = (dataValues.keys());
    int n = 0;
    if (atomProps != "") {
      n = 1;
      if (sfunc != null)
        s.append("function _setDataState();\n");
      s.append(atomProps);
    }
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (name.indexOf("property_") == 0) {
        if (n == 0 && sfunc != null)
          s.append("function _setDataState();\n");
        n++;
        Object data = ((Object[]) dataValues.get(name))[1];
        if (data instanceof float[]) {
          AtomCollection.getAtomicPropertyState(s, atoms, atomCount,
              AtomCollection.TAINT_MAX, 
              (BitSet) ((Object[]) dataValues.get(name))[2], 
              name, (float[]) data);
          s.append("\n");
        } else {
          s.append("\n  DATA \"").append(name).append("\"");
          s.append(data);
          s.append("  end \"").append(name).append("\";\n");
        }
      }
    }
    if (n == 0 || sfunc == null)
      return;
    sfunc.append("  _setDataState\n");
    s.append("end function;\n\n");
  }
}
