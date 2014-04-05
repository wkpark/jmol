/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dssr;

import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericLineReader;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.api.JmolDSSRParser;
import org.jmol.util.Logger;

public class DSSRParser implements JmolDSSRParser {

  private GenericLineReader reader;
  private String line;
  private Map<String, Object> dssr;

  public DSSRParser() {
    // for reflection
  }
  
  @Override
  public void process(Map<String, Object> info, GenericLineReader reader) throws Exception {
    info.put("dssr", dssr = new Hashtable<String, Object>());
    this.reader = reader;
    while (rd() != null) {
      if (line.startsWith("List of")) {
        Logger.info(line);
        int n = PT.parseInt(line.substring(8));
        if (n < 0)
          continue;
        line = PT.rep(PT.trim(line,  "s"), " interaction", "");
        int pt = "pair elix lice stem plet tack loop ulge otif pper turn tion ment".indexOf(line.trim().substring(line.length() - 4));
                //0    5    10        20        30        40        50        60
        switch(pt) {
        case 0:
          readPairs(n);
          break;
        default:
          Logger.info("DSSRParser ignored: " + line);
          break;
        case 5:
        case 10:
          getList(n, "helices", "  helix#");
          break;
        case 15:
          getList(n, "stems", "  stem#");
          break;
        case 20:
          readNTList("multiplets", n);
          break;
        case 25:
          readStacks(n);
          break;
        case 30:
          readLoops(n); // hairpin, internal, or kissing
          break;
        case 35:
          readBulges(n);
          break;          
        case 40:
          readMotifs(n);
          break;          
        case 45:
          readNTList("riboseZippers", n);
          break;          
        case 50:
          readTurns(n);
          break;          
        case 55:
          readJunctions(n);
          break;
        case 60:
          readNTList("singleStrandedSegments", n);
          break;
        }        
      }
    }
  }

  private Lst<Map<String, Object>> addList(String name) {    
    Lst<Map<String, Object>> map = new Lst<Map<String, Object>>();
    if (name != null)
      dssr.put(name, map);
    return map;
  }

  /**
   * List of 40 coaxial stacks
   * 
   * 1 Helix#1 contains 4 stems: [#1,#2,#3,#4]
   * 
   * 2 Helix#4 contains 4 stems: [#6,#7,#9,#13]
   * 
   * 3 Helix#10 contains 3 stems: [#16,#17,#18]
   * 
   * 4 Helix#13 contains 2 stems: [#21,#22]
   * 
   * @param n
   * @throws Exception 
   */
  private void readStacks(int n) throws Exception {
    readInfo("coaxialStacks", n);
  }

  /**
   * Default for no real processing -- just the lines
   * 
   * @param key
   * @param n
   * @return  list of information
   * @throws Exception
   */
  private Lst<String> readInfo(String key, int n) throws Exception {
    Lst<String> list = new Lst<String>();
    if (key != null)
      dssr.put(key, list);
    for (int i = 0; i < n; i++)
      list.addLast(rd());
    return list;
  }

  /*
****************************************************************************
Note: for the various types of loops listed below, numbers within the first
      set of brackets are the number of loop nts, and numbers in the second
      set of brackets are the identities of the stems (positive number) or
      lone WC/wobble pairs (negative numbers) to which they are linked.

****************************************************************************
List of 68 hairpin loops
   1 hairpin loop: nts=10; [8]; linked by [#7]
     nts=10 UGCCAAGCUG 0.U55,0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63,0.G64
       nts=8 GCCAAGCU 0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63


****************************************************************************
List of 67 internal loops
   1 symmetric internal loop: nts=14; [5,5]; linked by [#1,#-1]
     nts=14 GUGGAUUAUGAAAU 0.G21,0.U22,0.G23,0.G24,0.A25,0.U26,0.U27,0.A516,0.U517,0.G518,0.A519,0.A520,0.A521,0.U522
       nts=5 UGGAU 0.U22,0.G23,0.G24,0.A25,0.U26
       nts=5 UGAAA 0.U517,0.G518,0.A519,0.A520,0.A521
   2 asymmetric internal loop: nts=7; [1,2]; linked by [#4,#-2]
     nts=7 GCGCAAC 0.G39,0.C40,0.G41,0.C440,0.A441,0.A442,0.C443
       nts=1 C 0.C40
       nts=2 AA 0.A441,0.A442
   3 symmetric internal loop: nts=8; [2,2]; linked by [#8,#9]
     nts=8 CAAGCACG 0.C58,0.A59,0.A60,0.G61,0.C85,0.A86,0.C87,0.G88
       nts=2 AA 0.A59,0.A60
       nts=2 AC 0.A86,0.C87


List of 1 kissing loop interaction
   1 stem #29 between hairpin loops #12 and #50


   */
  private void readLoops(int n) throws Exception {
    if (line.indexOf("internal") >= 0) {
      readSets("internalLoops", n, 2, 4);
    } else if (line.indexOf("hairpin") >= 0) {
      readSets("hairpinLoops", n, 1, 3);
    } else if (line.indexOf("kissing") >= 0) {
      readInfo("kissingLoops", n);
    }
  }

  /*
List of 35 junctions
   1 3-way junction: nts=12; [0,6,0]; linked by [#-1,#2,#-14]
     nts=12 UGCUGCAAAGCA 0.U27,0.G28,0.C480,0.U481,0.G482,0.C483,0.A484,0.A485,0.A486,0.G487,0.C515,0.A516
       nts=0
       nts=6 UGCAAA 0.U481,0.G482,0.C483,0.A484,0.A485,0.A486
       nts=0
   2 3-way junction: nts=27; [2,15,4]; linked by [#2,#3,#30]
     nts=27 CUCGCGAUAGUGAACAAGUAGCGAACG 0.C29,0.U30,0.C31,0.G32,0.C451,0.G452,0.A453,0.U454,0.A455,0.G456,0.U457,0.G458,0.A459,0.A460,0.C461,0.A462,0.A463,0.G464,0.U465,0.A466,0.G467,0.C474,0.G475,0.A476,0.A477,0.C478,0.G479
       nts=2 UC 0.U30,0.C31
       nts=15 GAUAGUGAACAAGUA 0.G452,0.A453,0.U454,0.A455,0.G456,0.U457,0.G458,0.A459,0.A460,0.C461,0.A462,0.A463,0.G464,0.U465,0.A466
       nts=4 GAAC 0.G475,0.A476,0.A477,0.C478
   3 6-way junction: nts=36; [0,3,9,3,3,6]; linked by [#-2,#5,#-4,#15,#16,#18]
     nts=36 GCGGAACGGAACAGAAAAUGAUGUACAGCUAAACAC 0.G41,0.C42,0.G149,0.G150,0.A151,0.A152,0.C153,0.G184,0.G185,0.A186,0.A187,0.C188,0.A189,0.G190,0.A191,0.A192,0.A193,0.A194,0.U202,0.G203,0.A204,0.U205,0.G206,0.U233,0.A234,0.C235,0.A236,0.G237,0.C433,0.U434,0.A435,0.A436,0.A437,0.C438,0.A439,0.C440
       nts=0
       nts=3 GAA 0.G150,0.A151,0.A152
       nts=9 GAACAGAAA 0.G185,0.A186,0.A187,0.C188,0.A189,0.G190,0.A191,0.A192,0.A193
       nts=3 GAU 0.G203,0.A204,0.U205
       nts=3 ACA 0.A234,0.C235,0.A236
       nts=6 UAAACA 0.U434,0.A435,0.A436,0.A437,0.C438,0.A439
   */
  private void readJunctions(int n) throws Exception {
    readSets("junctions", n, 0, 3);
  }

  /*
****************************************************************************
List of 38 bulges
   1 bulge: nts=5; [0,1]; linked by [#3,#4]
     nts=5 GCGAC 0.G33,0.C34,0.G448,0.A449,0.C450
       nts=0
       nts=1 A 0.A449
   2 bulge: nts=5; [0,1]; linked by [#-4,#14]
     nts=5 CCGAG 0.C153,0.C154,0.G182,0.A183,0.G184
       nts=0
       nts=1 A 0.A183
   */
  private void readBulges(int n) throws Exception {
    readSets("bulges", n, 2, 2);
  }

  private void readSets(String key, int n, int nway, int ptnts) throws Exception {
    Lst<Map<String, Object>>sets = addList(key);
    boolean isJunction = (nway == 0);
    for (int i = 0; i < n; i++) {
      Map<String, Object>set = new Hashtable<String, Object>(); 
      String[] tokens = PT.getTokens(rd());
      set.put("type", tokens[1]);
      if (isJunction)
        nway = PT.parseInt(tokens[1].substring(0, tokens[1].indexOf("-")));
      set.put("count", Integer.valueOf(nway));
      set.put("nts", Integer.valueOf(PT.trim(tokens[ptnts],";").substring(4)));
      set.put("linkedBy", tokens[ptnts + 4]);
      set.put("nts", readNTList(null, nway + 1));
      sets.addLast(set);
    }
  }

  /*
List of 106 A-minor motifs
   1  type=I A/U-A  0.A48/0.U43,0.A148 WC
        -0.U43  H-bonds[1]: "O2'(hydroxyl)-O2'(hydroxyl)[2.90]"
        +0.A148 H-bonds[1]: "N1-O2'(hydroxyl)[2.74]"
   2  type=I A/G-C  0.A69/0.G54,0.C65 WC
        +0.G54  H-bonds[2]: "N1-O2'(hydroxyl)[2.69],N3-N2(amino)[2.84]"
        -0.C65  H-bonds[2]: "O2'(hydroxyl)-O2'(hydroxyl)[2.62],O2'(hydroxyl)-O2(carbonyl)[2.61]"
   3  type=I A/G-C  0.A98/0.G81,0.C93 WC
        +0.G81  H-bonds[2]: "N1-O2'(hydroxyl)[2.67],N3-N2(amino)[3.12]"
        -0.C93  H-bonds[0]: ""
   4  type=II A/G-C 0.A152/0.G41,0.C440 WC
        +0.G41  H-bonds[0]: ""
        -0.C440 H-bonds[3]: "O2'(hydroxyl)-O3'[3.17],O2'(hydroxyl)-O2'(hydroxyl)[2.73],N3-O2'(hydroxyl)[2.70]"
   */
  private void readMotifs(int n) throws Exception {
    Lst<Map<String, Object>>motifs = addList("aMinorMotifs");
    for (int i = 0; i < n; i++) {
      Map<String, Object>motif = new Hashtable<String, Object>(); 
      String[] tokens = PT.getTokens(rd());
      motif.put("type", tokens[1].substring(tokens[1].indexOf("=") + 1) + " " + tokens[2]);
      motif.put("info", line);
      motif.put("data", readInfo(null, 2));      
      motifs.addLast(motif);
    }
  }
  
  /*
List of 9 (possible) kink turns
   1 Normal k-turn with GA on NC-helix#5; iloop#4
      C#11[0.C93,0.G81 CG] [0.G97,0.A80 GA] NC#10[0.G77,0.C100 GC]
      strand1 nts=15; GGCGAAGAACCAUGG 0.G91,0.G92,0.C93,0.G94,0.A95,0.A96,0.G97,0.A98,0.A99,0.C100,0.C101,0.A102,0.U103,0.G104,0.G105
      strand2 nts=12; CCAUGGGGAGCC 0.C72,0.C73,0.A74,0.U75,0.G76,0.G77,0.G78,0.G79,0.A80,0.G81,0.C82,0.C83
   2 Undecided case with GA on NC-helix#14; iloop#7
      C#22[0.C280,0.G369 CG] [0.A285,0.G367 AG] NC#-9[0.G365,0.C287 GC]
      strand1 nts=13; GCUACCUCUCAUC 0.G275,0.C276,0.U277,0.A278,0.C279,0.C280,0.U281,0.C282,0.U283,0.C284,0.A285,0.U286,0.C287
      strand2 nts=10; GUGCGGUAGU 0.G365,0.U366,0.G367,0.C368,0.G369,0.G370,0.U371,0.A372,0.G373,0.U374
   */
  private void readTurns(int n) throws Exception {    
    Lst<Map<String, Object>>turns = addList("kinkTurns");
    for (int i = 0; i < n; i++) {
      Map<String, Object>turn = new Hashtable<String, Object>(); 
      String[] tokens = PT.getTokens(rd());
      turn.put("type", tokens[1]);
      turn.put("info", line);
      turn.put("details", rd());
      turn.put("nts", readNTList(null, 2));
      turns.addLast(turn);
    }
  }

  /*
List of 12 base pairs
      nt1              nt2             bp  name         Saenger    LW DSSR
   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W

List of 50 lone WC/wobble pairs
  Note: lone WC/wobble pairs are assigned negative indices to differentiate
        them from the stem numbers, which are positive.
        ------------------------------------------------------------------
  -1 0.U27            0.A516           U-A WC           20-XX     cWW cW-W
   */
  private void readPairs(int n) throws Exception {
    boolean isWobble = (line.indexOf("wobble") >= 0);
    Lst<Map<String, Object>>pairs = addList(isWobble ? "wobblePairs" : "basePairs");
    for (int i = (isWobble ? 3 : 1); --i >= 0;)
      rd();
    for (int i = 0; i < n; i++)
      pairs.addLast(getBPData());
 }

  /*
List of 1 helix
  Note: a helix is defined by base-stacking interactions, regardless of bp
        type and backbone connectivity, and may contain more than one stem.
      helix#number[stems-contained] bps=number-of-base-pairs in the helix
      bp-type: '|' for a canonical WC/wobble pair, '.' otherwise
      helix-form: classification of a dinucleotide step comprising the bp
        above the given designation and the bp that follows it. Types
        include 'A', 'B' or 'Z' for the common A-, B- and Z-form helices,
        '.' for an unclassified step, and 'x' for a step without a
        continuous backbone.
      --------------------------------------------------------------------
  helix#1[1] bps=12
      strand-1 5'-CGCGAATTCGCG-3'
       bp-type    ||||||||||||
      strand-2 3'-GCGCTTAAGCGC-5'
      helix-form  BBBBBBBBBBB
   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W

List of 1 stem
  Note: a stem is defined as a helix consisting of only canonical WC/wobble
        pairs, with a continuous backbone.
      stem#number[#helix-number containing this stem]
      Other terms are defined as in the above Helix section.
      --------------------------------------------------------------------
  stem#1[#1] bps=12
      strand-1 5'-CGCGAATTCGCG-3'
       bp-type    ||||||||||||
      strand-2 3'-GCGCTTAAGCGC-5'
      helix-form  BBBBBBBBBBB
   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W


   */
  
  private Map<String, Object> getBPData() throws Exception {
    Map<String, Object> data = new Hashtable<String, Object>();
    String[] tokens = PT.getTokens(rd());
    data.put("nt1", fix(tokens[1]));
    data.put("nt2", fix(tokens[2]));
    data.put("bp",  tokens[3]);
    int pt = (tokens.length == 8 ? 5 : 4);
    data.put("name", pt == 5 ? tokens[4] : "?");
    data.put("Saenger", tokens[pt++]);
    data.put("LW", tokens[pt++]);
    data.put("DSSR", tokens[pt]);
    return data;
  }

  /*
List of 31 non-loop single-stranded segments
   1 nts=3 UAU 0.U10,0.A11,0.U12
   2 nts=1 A 0.A128

List of 46 ribose zippers
   1 nts=4 UUAG 0.U26,0.U27,0.A1318,0.G1319
   2 nts=4 ACAC 0.A152,0.C153,0.A439,0.C440
   
List of 233 multiplets
  10 nts=3 AAA 0.A59,0.A60,0.A86
  11 nts=3* AGG 0.A80,0.G94,0.G97
   */

  private Lst<Map<String, Object>> readNTList(String type, int n) throws Exception {
    Lst<Map<String, Object>>list = addList(type);
    System.out.println("readNTlist " + type + " " + n);
    for (int i = 0; i < n; i++)
      list.addLast(getNTList());
    return list;
  }

  private void getList(int n, String key, String str) throws Exception {
    Lst<Map<String, Object>>list = addList(key);
    for (int i = 0; i < n; i++) {
      skipTo(str);
      int bps = PT.parseInt(line.substring(line.indexOf("=") + 1));
      Map<String, Object> data = new Hashtable<String, Object>();
      data.put("info", getHeader(4));
      list.addLast(data);
      Lst<Map<String, Object>>pairs = new Lst<Map<String, Object>>();    
      data.put("basePairs", pairs);
      for (int j = 0; j < bps; j++)
        pairs.addLast(getBPData());
    }
  }

  private Map<String, Object> getNTList() throws Exception {
    Map<String, Object> data = new Hashtable<String, Object>();
    //1 nts=0
    //3 nts=4 CGAA 0.C303,0.G304,0.A305,0.A306
    //nts=8 GCCAAGCU 0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63
    String[] tokens = PT.getTokens(rd());
    int pt = (tokens[0].startsWith("nts") ? 1 : 2);
    if (tokens.length > pt) {
      data.put("seq", tokens[pt++]);
      data.put("nt", getNT(tokens[pt]));
    }
    return data;
  }
  
  private Object getNT(String s) {
    String[] tokens = PT.split(s, ",");
    Lst<String> list = new Lst<String>();
    System.out.println("getNT "+ s + " " + tokens.length);
    for (int i = 0; i < tokens.length; i++)
      list.addLast(fix(tokens[i]));
    return list;
  }

  private Object getHeader(int n) throws Exception {
    SB header = new SB();
    header.append(line).append("\n");
    for (int j = 0; j < n; j++)
      header.append(rd()).append("\n");
    return header.toString();
  }

  private void skipTo(String key) throws Exception {
    while (!line.startsWith(key)) {
      rd();
    }
  }

  /**
   * A.T8 --> [T]8:A
   * 
   * @param nt
   * @return Jmol atom residue as [name]resno:chain
   */
  private String fix(String nt) {
    int pt1 = nt.indexOf(".");
    if (pt1 < 0)
      System.out.println(line);
    String chain = nt.substring(0, pt1);
    int pt = nt.length();
    while (Character.isDigit(nt.charAt(--pt))) {
    }
    return "[" + nt.substring(pt1 + 1, pt + 1) + "]" + nt.substring(pt + 1) + ":"
        + chain;
  }

  private String rd() throws Exception {
    return (line = reader.readNextLine());    
  }
}
