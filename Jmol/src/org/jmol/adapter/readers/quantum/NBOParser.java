package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;

public class NBOParser {

  private boolean haveBeta;


  public Lst<Object> getAllStructures(String output) {
    if (output == null)
      return null;
    Lst<Object> list = new Lst<Object>();
    output = PT.rep(output,  "the $CHOOSE", "");
    getStructures(getBlock(output, "$CHOOSE"), "CHOOSE", list);
    getStructures(getBlock(output, "$NRTSTR"), "NRTSTR", list);
    getStructures(getBlock(output, "$NRTSTRA"), "NRTSTRA", list);
    getStructures(getBlock(output, "$NRTSTRB"), "NRTSTRB", list);
    getStructuresTOPO(getData(output, "TOPO matrix", "* Total *", 1), "TOPOA", list);
    getStructuresTOPO(getData(output, "TOPO matrix", "* Total *", 2), "TOPOB", list);
    return list;
  }
  
  
  //  TOPO matrix for the leading resonance structure:
  //
  //    Atom  1   2   3
  //    ---- --- --- ---
  //  1.  O   2   2   0
  //  2.  C   2   0   2
  //  3.  O   0   2   1
  //
  //        Resonance
  //   RS   Weight(%)                  Added(Removed)
  //---------------------------------------------------------------------------
  //   1*(2)  24.76
  //   2*(2)  24.72   ( O  1),  O  3
  //   3*(2)  24.69    O  1- C  2, ( C  2- O  3), ( O  1),  O  3
  //   4*     24.61   ( O  1- C  2),  C  2- O  3
  //   5       0.23   ( O  1- C  2),  O  1- O  3, ( O  1),  C  2
  //   6       0.20    O  1- C  2,  O  1- C  2, ( C  2- O  3), ( C  2- O  3),
  //                  ( O  1), ( O  1),  O  3,  O  3
  //   7       0.17    O  1- O  3, ( C  2- O  3), ( O  1), ( O  1),  C  2,
  //                   O  3
  //   8       0.16   ( O  1- C  2), ( O  1- C  2),  C  2- O  3,  C  2- O  3,
  //                   O  1, ( O  3)
  //   9       0.12    O  1- O  3, ( C  2- O  3), ( O  1),  C  2
  //  10       0.12   ( O  1- C  2),  C  2- O  3,  O  1, ( O  3)
  //  11-20    0.22
  //---------------------------------------------------------------------------
  //         100.00   * Total *                [* = reference structure]
  //

  private void getStructuresTOPO(String data, String nrtType, Lst<Object> list) {
    if (data == null || data.length() == 0)
      return;
    String[] parts = PT.split(data, "Resonance");
    if (parts.length < 2)
      return;
    int pt = parts[0].lastIndexOf(".");
    int nAtoms = PT.parseInt(parts[0].substring(pt - 3, pt));
    if (nAtoms < 0)
      return;
    // decode the top table
    String[] tokens = PT.getTokens(PT.rep(PT.rep(parts[0], ".", ".1"), "Atom",
        "-1"));
    float[] raw = new float[tokens.length];
    int n = PT.parseFloatArrayInfested(tokens, raw);
    int[][] table = new int[nAtoms][nAtoms];
    int atom1 = -1, atom2 = 0, atom0 = 0;
    for (int i = 0; i < n; i++) {
      float f = raw[i];
      if (f < 0) {
        // start of table
        atom1 = -1;
        continue;
      }
      if (f % 1 == 0) {
        if (atom1 == -1) {
          // first atom in header
          atom0 = (int) (f);
          atom1 = -2;
        }
        // value or header
        if (atom1 < 0)
          continue;
        table[atom1][atom2++] = (int) f;
      } else {
        // new row
        atom1 = (int) (f - 1);
        atom2 = atom0 - 1;
      }
    }
    //    Resonance
    //    RS   Weight(%)                  Added(Removed)
    //---------------------------------------------------------------------------
    //    1*     16.80
    //    2*     16.80   ( C  2- C  3),  C  2- C 10,  C  3- C  4, ( C  4- C  7),
    //                    C  7- C  8, ( C  8- C 10), ( C 12- C 13),  C 12- C 14,
    //                    C 13- C 15, ( C 14- C 16), ( C 15- C 21),  C 16- C 21

    int[][] matrix = null;
    // turn this listing into a numeric array. decimal points indicate new atoms

    String s = parts[1].replace('-', ' ');
    s = PT.rep(s, ".", ".1");
    s = PT.rep(s, "(", " -1 ");
    s = PT.rep(s, ")", " -2 ");
    s = PT.rep(s, ",", " -3 ");
    s = PT.rep(s, "\n", " -4 ");
    tokens = PT.getTokens(s);
    raw = new float[tokens.length];
    n = PT.parseFloatArrayInfested(tokens, raw);
    Map<String, Object> htData = null;
    int dir = 1;
    atom1 = atom2 = -1;
    for (int i = 5, index = 0; i < n; i++) {
      float f = raw[i];
      float remain = f % 1;
      if (remain == 0) {
        int v = (int) f;
        switch (v) {
        case -1: // (
          dir = -1;
          atom1 = atom2 = -1;
          continue;
        case -2: // )
          break;
        case -3: // ,
          if (atom1 < 0)
            continue;
          break;
        case -4: // EOL
          // skip next number if the one after that is a fraction. 
          if (raw[i + 2] % 1 != 0)
            i++;
          else if (raw[i + 3] % 1 != 0) // last line may have "34-50 4.33"
            i += 2;
          if (atom1 < 0)
            continue;
          break;
        default:
          if (atom1 < 0) {
            atom1 = atom2 = v - 1;
          } else {
            atom2 = v - 1;
          }
          continue;
        }
        matrix[atom1][atom2] += dir;
        atom1 = atom2 = -1;
        dir = 1;
      } else {
        if (htData == null)
          matrix = table;
        System.out.println("NBOParser matrix " + nrtType + " " + index);
        for (int j = 0; j < nAtoms; j++)
          System.out.println(PT.toJSON(null, matrix[j]));
        System.out.println("-------------------");
        
        if (raw[i + 2] == -4) // blank line (all dashes)
          break;
        list.addLast(htData = new Hashtable<String, Object>());
        s = "" + ((int) f * 100 + (int) ((remain - 0.0999999) * 1000));
        int len = s.length();
        s = (len == 2 ? "0" : "") + s.substring(0, len - 2) + "."
            + s.substring(len - 2);
        htData.put("weight", s);
        htData.put("index", Integer.valueOf(index++));
        htData.put("type", nrtType.toLowerCase());
        htData.put("spin", nrtType.indexOf("B") >= 0 ? "beta" : "alpha");
        matrix = new int[nAtoms][nAtoms];
        htData.put("matrix", matrix);
        for (int j = 0; j < nAtoms; j++)
          for (int k = 0; k < nAtoms; k++)
            matrix[j][k] = table[j][k];
      }
    }
  }


  private String getBlock(String output, String key) {
    int pt = output.indexOf(key);
    int pt1 = output.indexOf("$END", pt + 1);
    return (pt < 0 || pt1 < 0 ? null : output.substring(pt + key.length(), pt1));
  }

  private String getData(String output, String start, String end, int n) {
    int pt = 0, pt1 = 0;
    for (int i = 0; i < n; i++) {
      pt = output.indexOf(start, pt1 + 1);
      pt1 = output.indexOf(end, pt + 1);
    }
    return (pt < 0 || pt1 < 0 ? null : output.substring(pt, pt1));
  }


  /**
   * Reads the $NRTSTR $NRTSTRA, $NRTSTRB, and $CHOOSE blocks. Creates a Lst of
   * Hashtables
   * 
   * @param data
   *        NBO output block not including $END
   * 
   * @param nrtType
   *        "CHOOSE", "NRTSTRA", "NRTSTRB"
   * @param list
   *        to fill
   * @return number of structures found or -1 for an error
   * 
   */
  public int getStructures(String data, String nrtType, Lst<Object> list) {

    //    $NRTSTRA
    //    STR        ! Wgt = 49.51%
    //      LONE 1 2 3 2 END
    //      BOND D 1 2 D 2 3 END
    //    END
    //    STR        ! Wgt = 25.63%
    //      LONE 1 3 3 1 END
    //      BOND S 1 2 T 2 3 END
    //    END
    //    STR        ! Wgt = 24.45%
    //      LONE 1 1 3 3 END
    //      BOND T 1 2 S 2 3 END
    //    END
    //  $END

    //    $CHOOSE
    //    ALPHA
    //     LONE 1 1 3 3 END
    //     BOND T 1 2 S 2 3 END
    //    END
    //    BETA
    //     LONE 1 1 3 2 END
    //     BOND D 1 2 S 2 3 END
    //       3C S 1 2 3 END
    //    END
    //   $END

    //  $CHOOSE
    //    BOND D 1 2 S 1 6 S 1 7 S 2 3 S 2 8 D 3 4 S 3 9 S 4 5 S 4 10 D 5 6 S 5 11
    //         S 6 12 END
    //  $END

    if (data == null || data.length() == 0)
      return 0;
    int n = 0;
    try {
      boolean ignoreSTR = (data.indexOf("ALPHA") >= 0);
      if (!ignoreSTR && !data.contains("STR"))
        data = "STR " + data + " END";
      nrtType = nrtType.toLowerCase();
      String spin = (nrtType.equals("nrtstrb") ? "beta" : "alpha");
      if (nrtType.equals("choose"))
        nrtType = null;
      Map<String, Object> htData = null;
      String[] tokens = PT.getTokens(data.replace('\r', ' ').replace('\n', ' ').replace('\t', ' '));
      String lastType = "";
      int index = 0;
      for (int i = 0, nt = tokens.length; i < nt; i++) {
        String tok = tokens[i];
        //       0         1         2         3         4
        //       01234567890123456789012345678901234567890
        switch ("STR  =    ALPHABETA LONE BOND 3C".indexOf(tok)) {
        case 0:
          if (ignoreSTR)
            continue;
          tok = spin;
          //$FALL-THROUGH$
        case 10:
        case 15:
          list.addLast(htData = new Hashtable<String, Object>());
          if (!lastType.equals(tok)) {
            lastType = tok;
            index = 0;
          }
          htData.put("index", Integer.valueOf(index++));
          htData.put("spin", spin = tok.toLowerCase());
          if (spin.equals("beta"))
            haveBeta = true;
          htData.put("type", nrtType == null ? spin : nrtType);
          n++;
          break;
        case 5:
          htData.put("weight", tokens[++i]);
          break;
        case 20: // LONE
          Lst<int[]> lone = new Lst<int[]>();
          htData.put("lone", lone);
          while (!(tok = tokens[++i]).equals("END")) {
            int at1 = Integer.parseInt(tok);
            int nlp = Integer.parseInt(tokens[++i]);
            lone.addLast(new int[] { nlp, at1 });
          }
          break;
        case 25: // BOND
          Lst<int[]> bonds = new Lst<int[]>();
          htData.put("bond", bonds);
          while (!(tok = tokens[++i]).equals("END")) {
            int order = "DTQ".indexOf(tok.charAt(0)) + 2;
            int at1 = Integer.parseInt(tokens[++i]);
            int at2 = Integer.parseInt(tokens[++i]);
            bonds.addLast(new int[] { order, at1, at2 });
          }
          break;
        case 30: // 3C
          Lst<int[]> threeCenter = new Lst<int[]>();
          htData.put("3c", threeCenter);
          while (!(tok = tokens[++i]).equals("END")) {
            int order = "DTQ".indexOf(tok.charAt(0)) + 2;
            int at1 = Integer.parseInt(tokens[++i]);
            int at2 = Integer.parseInt(tokens[++i]);
            int at3 = Integer.parseInt(tokens[++i]);
            threeCenter.addLast(new int[] { order, at1, at2, at3 });
          }
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      list.clear();
      return -1;
    }
    return n;
  }


  /**
   * 
   * Find the map for a specified structure, producing a structure that can be used to generate lone pairs and bonds for a Lewis structure 
   * 
   * @param structureList  a list of structural information from this class created from an NBO file
   * @param type  topoa, topob, nrtstra, nrtstrb, alpha, beta  -- last two are from CHOOSE
   * @param index  0-based index for this type
   * @return Hashtable or null
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getStructureMap(Lst<Object> structureList,
                                                    String type, int index) {
    if (type == null || structureList == null)
      return null;
    type = type.toLowerCase();
    String spin = (type.indexOf("b") < 0 ? "alpha" : "beta");
    for (int i = 0; i < structureList.size(); i++) {
      Map<String, Object> map = (Map<String, Object>) structureList.get(i);
      if (spin.equals(map.get("spin")) && type.equals(map.get("type"))
          && (index < 0 || index == ((Integer) map.get("index")).intValue())) {      
        return map;
      }
    }
    return null;
  }


  /**
   * Starting with a structure map, do what needs to be done to change the
   * current Jmol structure to that in terms of bonding and formal charge.
   * 
   * @param sb
   * @param vwr
   * @param map
   * @param addFormalCharge  true for closed shell only for now
   * 
   * @return a string that can be used to optionally label the atoms
   */
  @SuppressWarnings("unchecked")
  public static String setJmolLewisStructure(SB sb, Viewer vwr, Map<String, Object> map,
                                    boolean addFormalCharge) {
    if (map == null)
      return null;
    Lst<Object> bonds = (Lst<Object>) map.get("bond");
    Lst<Object> lonePairs = (Lst<Object>) map.get("lone");
    Lst<Object> loneValencies = (Lst<Object>) map.get("loneValencies"); // not implemented in GenNBO
    int[][] matrix = (int[][]) map.get("matrix");
    int[] lp = (int[]) map.get("lp");
    int[] lv = (int[]) map.get("lv");
    int[] bondCounts = (int[]) map.get("bondCounts");
    boolean needLP = (lp == null);
    int atomCount = (matrix == null ? vwr.ms.ac : matrix[1].length);
    if (needLP) {
      map.put("lp", lp = new int[atomCount]);
      map.put("lv", lv = new int[atomCount]);
      map.put("bondCounts", bondCounts = new int[atomCount]);
    }

    vwr.ms.deleteAllBonds();
    if (needLP) {
      if (lonePairs != null) {
        for (int i = lonePairs.size(); --i >= 0;) {
          int[] na = (int[]) lonePairs.get(i);
          int nlp = na[0];
          int a1 = na[1] - 1;
          lp[a1] = nlp;
        }
      } else if (matrix != null) {
        for (int i = atomCount; --i >= 0;) {
          lp[i] = matrix[i][i];
        }
      }

    }
    BS bsVis = vwr.ms.bsVisible;
    if (bonds != null) {
      for (int i = bonds.size(); --i >= 0;) {
        int[] oab = (int[]) bonds.get(i);
        int a1 = oab[1] - 1;
        int a2 = oab[2] - 1;
        int order = oab[0];
        if (needLP) {
          bondCounts[a1] += order;
          bondCounts[a2] += order;
        }
        int mad = (order > 3 ? 100 : order > 2 ? 150 : 200);
        vwr.ms.bondAtoms(vwr.ms.at[a1], vwr.ms.at[a2], order, (short) mad,
            bsVis, 0, true, true);
      }
    } else if (matrix != null) {
      for (int i = 0; i < atomCount - 1; i++) {
        int[] m = matrix[i];
        for (int j = i + 1; j < atomCount; j++) {
          int order = m[j];
          if (order == 0)
            continue;
          int mad = (order > 3 ? 100 : order > 2 ? 150 : 200);
          vwr.ms.bondAtoms(vwr.ms.at[i], vwr.ms.at[j], order, (short) mad,
              bsVis, 0, true, true);
          if (needLP) {
            bondCounts[i] += order;
            bondCounts[j] += order;
          }
        }
      }
    }
    for (int i = atomCount; --i >= 0;) {
      // It is not entirely possible to determine charge just by how many
      // bonds there are to an atom. But we can come close for most standard
      // structures - NOT CO2(+), though.
      Atom a = vwr.ms.at[i];
      a.setValence(bondCounts[i]);
      a.setFormalCharge(0);
      int nH = vwr.ms.getMissingHydrogenCount(a, true);
      if (a.getElementNumber() == 6 && nH == 1) { 
        // for carbon, we need to adjust for lone pairs.
        // sp2 C+ will be "missing one H", but effectively we want to consider it 
        // "one H too many", referencing to carbene (CH2) instead of methane (CH4).
        // thus setting its charge to 1+, not 1-
        if (bondCounts[i] == 3 && lp[i] == 0 || bondCounts[i] == 2)
          nH -= 2; 
      }
      a.setFormalCharge(-nH);
    }
    if (sb == null)
      return null;
    sb.append("select visible;label %a;");
    for (int i = atomCount; --i >= 0;) {
      int charge = (addFormalCharge ? vwr.ms.at[i].getFormalCharge() : 0);
      if (lp[i] == 0 && lv[i] == 0 && charge == 0)
        continue;
      sb.append("select @" + (i + 1) + ";label ");
      if (lp[i] > 0)
        sb.append("<sup>(" + lp[i] + ")</sup>");
      if (lv[i] > 0)
        sb.append("<sub>[" + lv[i] + "]</sub>");
      sb.append("%a");
      if (addFormalCharge) {
        if (charge != 0)
          sb.append("<sup>" + Math.abs(charge)
              + (charge > 0 ? "+" : charge < 0 ? "-" : "") + "</sup>");
      }
      sb.append(";");
    }
    return sb.toString();
  }


  public boolean isOpenShell() {
    return haveBeta;
  }


}
