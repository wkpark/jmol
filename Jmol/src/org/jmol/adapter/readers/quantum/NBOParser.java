package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.viewer.Viewer;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

public class NBOParser {

  private boolean haveBeta;


  public Lst<Object> getAllStructures(String output) {
    if (output == null)
      return null;
    Lst<Object> list = new Lst<Object>();
    output = PT.rep(output,  "the $CHOOSE", "");
    getStructures(getBlock(output, "$CHOOSE"), "CHOOSE", list);
    getStructures(getBlock(output, "$NRTSTRA"), "NRTSTRA", list);
    getStructures(getBlock(output, "$NRTSTRB"), "NRTSTRB", list);
    return list;
  }
  
  
  private String getBlock(String output, String key) {
    int pt = output.indexOf(key);
    int pt1 = output.indexOf("$END", pt + 1);
    return (pt < 0 || pt1 < 0 ? null : output.substring(pt + key.length(), pt1));
  }


  /**
   * Reads the $NRTSTRA, $NRTSTRB, and $CHOOSE blocks. Creates a Lst of
   * Hashtables
   * 
   * @param output
   *        NBO output block not including $END
   * 
   * @param nrtType
   *        "CHOOSE", "NRTSTRA", "NRTSTRB"
   * @param list
   *        to fill
   * @return number of structures found or -1 for an error
   * 
   */
  public int getStructures(String output, String nrtType, Lst<Object> list) {

    System.out.println(output);
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

    if (output == null)
      return 0;
    int n = 0;
    try {
      boolean ignoreSTR = (output.indexOf("ALPHA") >= 0);
      if (!ignoreSTR && !output.contains("STR"))
        output = "STR " + output + " END";
      nrtType = nrtType.toLowerCase();
      String spin = (nrtType.equals("nrtstrb") ? "beta" : "alpha");
      if (nrtType.equals("choose"))
        nrtType = null;
      Map<String, Object> htData = null;
      String[] tokens = PT.getTokens(output.replace('\r', ' ').replace('\n', ' ').replace('\t', ' '));
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
   * Find the map for a specified structure.
   * 
   * @param structureList  a list of structural information from this class created from an NBO file
   * @param type  nrtstra, nrtstrb, alpha, beta  -- last two are from CHOOSE
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
          && (index < 0 || i == ((Integer) map.get("index")).intValue())) {      
        return map;
      }
    }
    return null;
  }


  /**
   * Starting with a structure map, do what needs to be done to change the
   * current structure to that.
   * 
   * @param sb
   * @param vwr
   * @param map
   * @param addCharge   currently not working
   * 
   * @return a string that can be used to optionally label the atoms
   */
  @SuppressWarnings("unchecked")
  public static String setStructure(SB sb, Viewer vwr, Map<String, Object> map,
                                    boolean addCharge) {
    if (map == null)
      return null;
    Lst<Object> bonds = (Lst<Object>) map.get("bond");
    Lst<Object> lonePairs = (Lst<Object>) map.get("lone");
    Lst<Object> loneValencies = (Lst<Object>) map.get("loneValencies"); // not implemented in GenNBO
    int[] lp = (int[]) map.get("lp");
    int[] lv = (int[]) map.get("lv");
    int[] bondCounts = (int[]) map.get("bondCounts");
    boolean needLP = (lp == null);
    if (needLP) {
      map.put("lp", lp = new int[vwr.ms.ac]);
      map.put("lv", lv = new int[vwr.ms.ac]);
      map.put("bondCounts", bondCounts = new int[vwr.ms.ac]);
    }

    vwr.ms.deleteAllBonds();
    if (needLP && lonePairs != null)
      for (int i = lonePairs.size(); --i >= 0;) {
        int[] na = (int[]) lonePairs.get(i);
        int nlp = na[0];
        int a1 = na[1] - 1;
        lp[a1] = nlp;
      }
    if (bonds != null)
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
            vwr.ms.bsVisible, 0, true, true);
      }
    for (int i = vwr.ms.ac; --i >= 0;) {
      vwr.ms.at[i].setFormalCharge(0);
      vwr.ms.at[i].setValence(bondCounts[i]);
    }
    // It is not entirely possible to determine charge just by how many
    // bonds there are to an atom. But we can come close for most standard
    // structures - NOT CO2(+), though.
    vwr.ms.fixFormalCharges(vwr.getAllAtoms());
    if (sb == null)
      return null;
    sb.append("select visible;label %a;");
    for (int i = vwr.ms.ac; --i >= 0;) {
      if (lp[i] == 0 && lv[i] == 0)
        continue;
      sb.append("select @" + (i + 1) + ";label ");
      if (lp[i] > 0)
        sb.append("<sup>(" + lp[i] + ")</sup>");
      if (lv[i] > 0)
        sb.append("<sub>[" + lv[i] + "]</sub>");
      sb.append("%a");
      if (addCharge) {
        int charge = vwr.ms.at[i].getFormalCharge();
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
