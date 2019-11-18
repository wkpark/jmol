/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

import javax.swing.*;

import java.io.BufferedReader;

public class LoadMeasureThread extends Thread {

  NMR_JmolPanel nmr;
  BufferedReader inp;
  int subindex;

  public LoadMeasureThread(NMR_JmolPanel nmr, BufferedReader inp) {
    this.nmr = nmr;
    this.inp = inp;
    int minindex = nmr.getMinindex();
    if (minindex == 0) {
      this.subindex = 1;
    } else {
      this.subindex = 0;
    }
  }

  @Override
  public void run() {
    //setPriority(Thread.MIN_PRIORITY);
    try {
      //sleep(1000);

      String command = new String();
      String line;
      boolean loop = true;
      // This exception handling loop seems to have desired 
      // effect of holding this thread until the molecule is
      // loaded and the labels can be set.
      while (loop) {
        try {
          nmr.labelSetter.setLabel(1, "test");
          nmr.labelSetter.setLabel(1, null);
          loop = false;
        } catch (Exception e) {
          loop = true;
        }
      }

      //labels
      while ((line = inp.readLine()).trim().length() != 0) {
        String[] l = line.split("\\s+");
        int i = (new Integer(l[0])).intValue();
        String com = null;
        com = nmr.labelSetter.setLabel(i - 1, l[1]);
        command = command + ";" + com;
      }
      String[] labelArray = nmr.labelSetter.getLabelArray();
      nmr.noeTable.setLabelArray(labelArray);
      nmr.coupleTable.setLabelArray(labelArray);

      // The atom indexing in Jmol appears to start from 0 or 1
      // depending on JVM.
      // This is the work around 
      int minindex = nmr.getMinindex();
      int subindex;
      if (minindex == 0) {
        subindex = 1;
      } else {
        subindex = 0;
      }

      // Noes
      while ((line = inp.readLine()).trim().length() != 0) {
        String[] l = line.split("\\s+");
        int ia = (new Integer(l[0])).intValue();
        int ib = (new Integer(l[1])).intValue();
        int pa = ia - subindex;
        int pb = ib - subindex;
        command = command + ";measure " + pa + " " + pb;
        if (l[2] != null) {
          if (!l[2].equals("null")) {
            nmr.noeTable.setExpNoe(l[2], ia - 1, ib - 1);
          }
        }
      }

      //Couples
      while ((line = inp.readLine()) != null) {
        if (line.trim().length() == 0)
          break;
        String[] l = line.split("\\s+");
        int ia = (new Integer(l[0])).intValue();
        int ib = (new Integer(l[1])).intValue();
        int ic = (new Integer(l[2])).intValue();
        int id = (new Integer(l[3])).intValue();
        int pa = ia - subindex;
        int pb = ib - subindex;
        int pc = ic - subindex;
        int pd = id - subindex;
        command = command + ";measure " + pa + " " + pb + " " + pc + " " + pd;
        if (l[4] != null) {
          if (!l[4].equals("null")) {
            nmr.coupleTable.setExpCouple(l[4], ia - 1, id - 1);
          }
        }
      }

      nmr.noeTable.updateTables();
      nmr.coupleTable.updateTables();
      nmr.vwr.script(command);

    } catch (Exception ie) {
      //Logger.debug("execution command interrupted!"+ie);
    }
  }
}
