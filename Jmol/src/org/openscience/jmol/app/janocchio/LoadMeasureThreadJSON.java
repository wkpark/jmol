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

import javax.swing.JCheckBoxMenuItem;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoadMeasureThreadJSON extends Thread {

  NMR_JmolPanel nmr;
  JSONObject data;

  public LoadMeasureThreadJSON(NMR_JmolPanel nmr, JSONObject data) {
    this.nmr = nmr;
    this.data = data;
  }

  @Override
  public void run() {
    try {

      //String jre = System.getProperty("java.version");
      String command = new String();
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

      if (data.has("Labels")) {
        JSONArray labels = data.getJSONArray("Labels");
        // labels

        for (int i = 0; i < labels.length(); i++) {
          JSONObject label = labels.getJSONObject(i);

          int j = label.getInt("index");
          String com = null;
          loop = true;

          String l = (String) label.get("label");
          com = nmr.labelSetter.setLabel(j - 1, l);

          command = command + ";" + com;
        }

        String[] labelArray = nmr.labelSetter.getLabelArray();
        nmr.noeTable.setLabelArray(labelArray);
        nmr.coupleTable.setLabelArray(labelArray);
      }

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

      if (data.has("NOEs")) {
        JSONArray noes = data.getJSONArray("NOEs");
        // Noes

        for (int i = 0; i < noes.length(); i++) {
          JSONObject noe = noes.getJSONObject(i);
          int ia = noe.getInt("a");
          int ib = noe.getInt("b");
          int pa = ia - subindex;
          int pb = ib - subindex;

          command = command + ";measure " + pa + " " + pb;
          if (noe.has("exp")) {
            String exp = (String) noe.get("exp");
            if (exp != null) {
              nmr.noeTable.setExpNoe(exp, ia - 1, ib - 1);
            }
          }
          if (noe.has("expd")) {
            String exp = (String) noe.get("expd");
            if (exp != null) {
              nmr.noeTable.setExpDist(exp, ia - 1, ib - 1);
            }
          }
        }
      }

      if (data.has("Couples")) {
        JSONArray couples = data.getJSONArray("Couples");
        // Couples

        for (int i = 0; i < couples.length(); i++) {
          JSONObject couple = couples.getJSONObject(i);
          int ia = couple.getInt("a");
          int ib = couple.getInt("b");
          int ic = couple.getInt("c");
          int id = couple.getInt("d");
          int pa = ia - subindex;
          int pb = ib - subindex;
          int pc = ic - subindex;
          int pd = id - subindex;

          command = command + ";measure " + pa + " " + pb + " " + pc + " " + pd;
          if (couple.has("exp")) {
            String exp = (String) couple.get("exp");
            if (exp != null) {

              nmr.coupleTable.setExpCouple(exp, ia - 1, id - 1);
            }
          }
        }
      }

      if (data.has("RefNOE")) {
        int[] noeNPrefIndices = new int[2];

        JSONObject refNOE = data.getJSONObject("RefNOE");
        noeNPrefIndices[0] = refNOE.getInt("a");
        noeNPrefIndices[1] = refNOE.getInt("b");
        nmr.noeTable.setNoeNPrefIndices(noeNPrefIndices);
      }

      if (data.has("ExpRefNOEValue")) {
        double expRefValue = data.getDouble("ExpRefNOEValue");
        nmr.noeTable.setNoeExprefValue(expRefValue);
      }

      if (data.has("CorrelationTime")) {
        double dval = data.getDouble("CorrelationTime");
        nmr.noeTable.setCorrelationTime(dval);
        nmr.noeTable.noeParameterSelectionPanel.getTauField().setText(
            String.valueOf(dval));
      }

      if (data.has("MixingTime")) {
        double dval = data.getDouble("MixingTime");
        nmr.noeTable.setMixingTime(dval);
        nmr.noeTable.noeParameterSelectionPanel.gettMixField().setText(
            String.valueOf(dval));
      }

      if (data.has("NMRfreq")) {
        double dval = data.getDouble("NMRfreq");
        nmr.noeTable.setNMRfreq(dval);
        nmr.noeTable.noeParameterSelectionPanel.getFreqField().setText(
            String.valueOf(dval));
      }

      if (data.has("RhoStar")) {
        double dval = data.getDouble("RhoStar");
        nmr.noeTable.setRhoStar(dval);
        nmr.noeTable.noeParameterSelectionPanel.getRhoStarField().setText(
            String.valueOf(dval));
      }

      if (data.has("NoeYellowValue")) {
        double dval = data.getDouble("NoeYellowValue");
        nmr.noeTable.setYellowValue(dval);
        nmr.noeTable.noeColourSelectionPanel.getYellowField().setText(
            String.valueOf(dval));
      }

      if (data.has("NoeRedValue")) {
        double dval = data.getDouble("NoeRedValue");
        nmr.noeTable.setRedValue(dval);
        nmr.noeTable.noeColourSelectionPanel.getRedField().setText(
            String.valueOf(dval));
      }

      if (data.has("CoupleYellowValue")) {
        double dval = data.getDouble("CoupleYellowValue");
        nmr.coupleTable.setYellowValue(dval);
        nmr.coupleTable.coupleColourSelectionPanel.getYellowField().setText(
            String.valueOf(dval));
      }

      if (data.has("CoupleRedValue")) {
        double dval = data.getDouble("CoupleRedValue");
        nmr.coupleTable.setRedValue(dval);
        nmr.coupleTable.coupleColourSelectionPanel.getRedField().setText(
            String.valueOf(dval));
      }

      nmr.noeTable.addMolCDK();
      nmr.noeTable.updateTables();
      nmr.coupleTable.addMolCDK();
      nmr.coupleTable.updateTables();
      nmr.vwr.scriptWait(command);

      if (data.has("NamfisPopulation")) {
        JSONArray populations = data.getJSONArray("NamfisPopulation");
        if (populations.length() > 0) {
          int nmodel = ((NMR_Viewer) nmr.vwr).getModelCount();
          double[] population = new double[nmodel + 1];
          for (int i = 0; i <= nmodel; i++) {
            population[i] = 0.0;
          }
          for (int i = 0; i < populations.length(); i++) {
            JSONObject p = populations.getJSONObject(i);
            int index = p.getInt("index");
            double pop = p.getDouble("p");
            population[index] = pop;
          }

          nmr.populationDisplay.addPopulation(population);

          JCheckBoxMenuItem mi = (JCheckBoxMenuItem) nmr
              .getMenuItem("NMR.populationDisplayCheck");
          mi.setSelected(true);
        }
      }

    } catch (Exception ie) {
      // Logger.debug("execution command interrupted!"+ie);
    }
  }
}
