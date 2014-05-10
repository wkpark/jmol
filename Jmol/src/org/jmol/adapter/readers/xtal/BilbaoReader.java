/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

/* 
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
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

package org.jmol.adapter.readers.xtal;

import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.ModulationSet;
import org.jmol.util.Vibration;

/**
 * Bilbao Crystallographic Database file reader
 * 
 * see, for example, http://www.cryst.ehu.es/cryst/compstru.html Comparison of
 * Crystal Structures with the same Symmetry
 * 
 * 
 * 
 * filter options include:
 * 
 * HIGH include high-symmetry structure;
 * 
 * preliminary only
 * 
 * @author Bob Hanson
 */

public class BilbaoReader extends AtomSetCollectionReader {

  private boolean getHigh;
  private boolean getSym;

  @Override
  public void initializeReader() throws Exception {
    if (rd().indexOf("<") < 0) {
      readBilbaoFormat(null, false);
      continuing = false;
    }
    getHigh = checkFilterKey("HIGH");
    getSym = true || !checkFilterKey("SYM");

    asc.getXSymmetry().vibScale = 1;
  }

  /*
  15
  13.800 5.691 9.420 90.0 102.3 90.0
  7
  Pb    1   4e    0.0000 0.2910 0.2500
  Pb    2   8f    0.3170 0.3090 0.3520
  P     1   8f    0.5990 0.2410 0.4470
  O     1   8f    0.6430 0.0300 0.3920
  O     2   8f    0.6340 0.4640 0.3740
  O     3   8f    0.6420 0.2800 0.6120
  O     4   8f    0.4910 0.2220 0.4200 
  */
  private void readBilbaoFormat(String title, boolean getDisplacement)
      throws Exception {
    setFractionalCoordinates(true);
    if (!doGetModel(++modelNumber, title))
      return;
    asc.newAtomSet();
    if (title != null) {
      asc.setAtomSetName(title);
      appendLoadNote(title);
    }
    if (line.indexOf("<pre>") >= 0)
      line = line.substring(line.indexOf("<pre>") + 5);
    int intTableNo = parseIntStr(line);
    setSpaceGroupName("" + intTableNo);
    float[] data = new float[6];
    fillFloatArray(null, 0, data);
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, data[i]);
    int i0 = asc.ac;
    int n = parseIntStr(rd());
    for (int i = n; --i >= 0;) {
      String[] tokens = getTokensStr(rd());
      if (!getSym && tokens[1].contains("_"))
        continue;
      addAtomXYZSymName(tokens, 3, tokens[0], tokens[0] + tokens[1]);
    }
    if (getDisplacement) {
      rd();
      /*
      ##disp-par## Rb1x|x0.000000x|x0.000791x|x-0.001494
      ##disp-par## Rb1_2x|x0.000000x|x0.000791x|x0.001494
       */
      for (int i = 0; i < n; i++) {
        String[] tokens = PT.split(rd(), "x|x");
        if (getSym || !tokens[0].contains("_"))
          asc.atoms[i0 + i].vib = V3.new3(parseFloatStr(tokens[1]),
              parseFloatStr(tokens[2]), parseFloatStr(tokens[3]));
      }
    }
    applySymmetryAndSetTrajectory();

    // convert the atom vibs to Cartesian displacements
    if (getDisplacement) {
      for (int i = asc.ac; --i >= i0;) {
        Atom a = asc.atoms[i];
        if (a.vib != null) {
          Vibration v = new Vibration();
          v.setT(a.vib);
          a.vib = v;
          v.modDim = Vibration.TYPE_DISPLACEMENT;
          asc.getSymmetry().toCartesian(v, true);
        }
      }
      appendLoadNote((asc.ac - i0) + " displacements");
    }

  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.contains("High symmetry structure<")) {
      if (getHigh)
        readBilbaoFormat("high symmetry", false);
    } else if (line.contains("Low symmetry structure<")) {
      readBilbaoFormat("low symmetry", false);
    } else if (line.contains("structure in the subgroup basis<")) {
      readBilbaoFormat("high symmetry in the subgroup basis", false);
    } else if (line.contains("Low symmetry structure after the origin shift<")) {
      readBilbaoFormat("low symmetry after origin shift", false);
    } else if (line.contains("<h3>Irrep:")) {
      readIrrep();
    }
    return true;
  }

  /*
  <input type="hidden" name="irrep" value="GM1+::GM">
  <input type="hidden" name="set" value="<i>C</i><i>c</i>">
  <input type="hidden" name="iso" value=" 62 Pnma D2h-16">
  <input type="hidden" name="what" value="virtual">
  <input type="submit" value="Virtual structure">
  with only this symmetry component of the distortion frozen.</form>
  <form action="mcif2vesta/index.php" method=post>
  <input type="hidden" name="BCS" value="009
  15.312000 26.660000 29.121000 90.000000 90.000000 90.000000 
  */

  private void readIrrep() throws Exception {
    String s = getLinesUntil("\"BCS\"");
    int pt = s.indexOf("The amplitude");
    pt = s.indexOf("=", pt);
    String amp = s.substring(pt + 2, s.indexOf(" ", pt + 2));
    String irrep = getAttr(s, "irrep");
    if (irrep.indexOf(":") >= 0)
      irrep = irrep.substring(0, irrep.indexOf(":"));
    //String set = getAttr(s, "set");
    String iso = getAttr(s, "iso");
    //String what = getAttr(s, "what");
    line = line.substring(line.indexOf("value=") + 7);
    readBilbaoFormat(irrep + " " + iso + " (" + amp + " Ang.)",
        true);

  }

  private String getAttr(String s, String key) {
    int pt = s.indexOf("value", s.indexOf("\"" + key + "\""));
    s = PT.getQuotedStringAt(s, pt);
    s = PT.rep(s, "<i>", "");
    s = PT.rep(s, "</i>", "");
    return s.trim();
  }

  private String getLinesUntil(String key) throws Exception {
    SB sb = new SB();
    do {
      sb.append(line);
    } while (!rd().contains(key));
    return sb.toString();
  }
}
