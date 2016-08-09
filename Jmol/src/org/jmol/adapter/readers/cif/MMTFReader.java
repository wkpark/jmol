/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BC;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.java.BS;
import org.jmol.script.SV;
import org.jmol.util.Logger;

/**
 * JmolData RCSB MMTF (macromolecular transmission format) file reader
 * 
 * see https://github.com/rcsb/mmtf/blob/master/spec.md
 * 
 * /full/ specification as of 2016.4.18 is implemented,including:
 * 
 * reading atoms, bonds, and DSSR secondary structure
 * 
 *   load =1f88.mmtf 
 *   
 * 
 * reading space groups and unit cells, and using those as per other readers
 * 
 *   load =1crn.mmtf {1 1 1} 
 * 
 * reading bioassemblies (biomolecules) and applying those transformations
 * 
 *   load =1auy.mmtf FILTER "biomolecule 1;*.CA,*.P"
 * 
 * reading biomolecules and lattices, and loading course-grained
 * 
 *   load =1auy.mmtf {2 2 1} filter "biomolecule 1;bychain";spacefill 30.0; color property symop
 * 
 * Note that the filter "DSSP1" indicates that mmtf included DSSP 1.0 calculation should be used. 
 * Otherwise Jmol will use DSSP 2.0 itself.
 * 
 */

public class MMTFReader extends MMCifReader {

  @Override
  protected void addHeader() {
    // no header for this type
  }

  /**
   * standard set up
   * @param fullPath
   * @param htParams
   * @param reader
   */
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    isBinary = true;
    isMMCIF = true;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void processBinaryDocument() throws Exception {
    boolean doDoubleBonds = (!isCourseGrained && !checkFilterKey("NODOUBLE"));
    isDSSP1 = checkFilterKey("DSSP1");
    boolean mmtfImplementsDSSP2 = false; // so far!
    applySymmetryToBonds = true;
    map = (new MessagePackReader(binaryDoc, true)).readMap();
    if (Logger.debugging) {
      for (String s: map.keySet())
        Logger.debug(s);
    }
    asc.setInfo("noAutoBond", Boolean.TRUE);
    Logger.info("MMTF version " + map.get("mmtfVersion"));
    Logger.info("MMTF Producer " + map.get("mmtfProducer"));
    appendLoadNote((String) map.get("title"));
    String id = (String) map.get("structureId");
    fileAtomCount = ((Integer)map.get("numAtoms")).intValue();
    int nBonds = ((Integer)map.get("numBonds")).intValue();
    Logger.info("id atoms bonds " + id + " " + fileAtomCount + " " + nBonds);
    getAtoms(doDoubleBonds);
    if (!isCourseGrained) {
      getBonds(doDoubleBonds);
      if (isDSSP1 || mmtfImplementsDSSP2) 
        getStructure((byte[]) map.get("secStructList"));
    }
    setSymmetry();
    getBioAssembly();
    setModelPDB(true);
    if (Logger.debuggingHigh)
      Logger.info(SV.getVariable(map).asString());
  }

/////////////// MessagePack decoding ///////////////
  
  private Object decode(String key) {
    byte[] b = (byte[]) map.get(key);
    int type = BC.bytesToInt(b, 0, true);
    int n = BC.bytesToInt(b, 4, true);
    int param = BC.bytesToInt(b, 8, true);
    switch (type) {
    case 1:
      return getFloats(b, 4, 1);
    case 2:
    case 3:
    case 4:
      return getInts(b, 1 << (type - 2));
    case 5:
      return rldecode32ToStr(b);
    case 6:
      return rldecode32ToChar(b, n);
    case 7:
      return rldecode32(b, n);
    case 8:
      return rldecode32Delta(b, n);
    case 9:
      return rldecode32f(b, n, param);
    case 10:
      return recursiveIndex16Deltaf(b, n, param);
    case 11:
      return getFloats(b, 2, param);
    }
    return null;
  }

  /**
   * mmtf type 1 and 11
   * 
   * byte[4] to float32
   *  
   * @param b
   * @param n 
   * @param divisor 
   * @return float[]
   */ 
  protected float[] getFloats(byte[] b, int n, float divisor) {
    if (b == null)
      return null;
    int len = (b.length - 12) / n;
    float[] a = new float[len];
    try {
      switch (n) {
      case 2:
        for (int i = 0, j = 12; i < len; i++, j += 2)
          a[i] = BC.bytesToShort(b, j, false) / divisor;
        break;
      case 4:
        for (int i = 0, j = 12; i < len; i++, j += 4)
          a[i] = BC.bytesToFloat(b, j, false);
        break;
      }
    } catch (Exception e) {
    }
    return a;
  }
  
  /**
   * mmtf types 2-4
   * 
   * Decode a byte array into a byte, short, or int array.
   *   
   * @param b
   * @param nbytes  1 (byte), 2 (int16), or 4 (int32)
   * @return int array
   */
  protected int[] getInts(byte[] b, int nbytes) {
    if (b == null)
      return null;
    int len = (b.length - 12) / nbytes;
    int[] a = new int[len];
    switch (nbytes) {
    case 1:
      for (int i = 0, j = 12; i < len; i++, j++)
        a[i] = b[j];
      break;
    case 2:
      for (int i = 0, j = 12; i < len; i++, j += 2)
        a[i] = BC.bytesToShort(b, j, true);
      break;
    case 4:
      for (int i = 0, j = 12; i < len; i++, j += 4)
        a[i] = BC.bytesToInt(b, j, true);
      break;
    }
    return a;
  }
  
  /**
   * mmtf type 5 
   * 
   * Decode each four bytes as a 1- to 4-character string label
   * where a 0 byte indicates end-of-string.
   *  
   * @param b a byte array
   * @return String[]
   */
  protected String[] rldecode32ToStr(byte[] b) {
    String[] id = new String[(b.length - 12) / 4];
    out: for (int i = 0, len = id.length, pt = 12; i < len; i++) {
      SB sb = new SB();
      for (int j = 0; j < 4; j++) {
        switch (b[pt]) {
        case 0:
          id[i] = sb.toString();
          pt += 4 - j;
          continue out;
        default:
          sb.appendC((char) b[pt++]);
          if (j == 3)
            id[i] = sb.toString();
          continue;
        }
      }        
    }    
    return id;
  }

  /**
   * mmtf type 6 
   * 
   * Decode an array of int32 using run-length decoding to one
   * char per int.
   * 
   * @param b
   * @param n
   * @return array of integers
   */
  protected char[] rldecode32ToChar(byte[] b, int n) {
    if (b == null)
      return null;
    char[] ret = new char[n];
    for (int i = 0, pt = 2; i < n;) {
      char val = (char) b[(++pt) << 2];
      for (int j = BC.bytesToInt(b, (++pt) << 2, true); --j >= 0;)
         ret[i++] = val;
    }
    return ret;
  }

  /**
   * mmtf type 7 
   * 
   * Decode an array of int32 using run-length decoding.
   * 
   * @param b
   * @param n
   * @return array of integers
   */
  protected int[] rldecode32(byte[] b, int n) {
    if (b == null)
      return null;
    int[] ret = new int[n];
    for (int i = 0, pt = 2; i < n;) {
      int val = BC.bytesToInt(b, (++pt) << 2, true);
      for (int j = BC.bytesToInt(b, (++pt) << 2, true); --j >= 0;)
         ret[i++] = val;
    }
    return ret;
  }

  /**
   * mmtf type 8
   * 
   * Decode an array of int32 using run-length decoding
   * of a difference array.
   * 
   * @param b
   * @param n
   * @return array of integers
   */
  protected int[] rldecode32Delta(byte[] b, int n) {
    if (b == null)
      return null;
    int[] ret = new int[n];
    for (int i = 0, pt = 3, val = 0; i < n;) {
      int diff = BC.bytesToInt(b, (pt++) << 2, true);
      for (int j = BC.bytesToInt(b, (pt++) << 2, true); --j >= 0;)
        ret[i++] = (val = val + diff);
    }
    return ret;
  }
  
  /**
   * mmtf type 9 
   * 
   * Decode an array of int32 using run-length decoding.
   * 
   * @param b
   * @param n
   * @param divisor 
   * @return array of floats
   */
  protected float[] rldecode32f(byte[] b, int n,  float divisor) {
    if (b == null)
      return null;
    float[] ret = new float[n];
    for (int i = 0, pt = 2; i < n;) {
      int val = BC.bytesToInt(b, (++pt) << 2, true);
      for (int j = BC.bytesToInt(b, (++pt) << 2, true); --j >= 0;)
         ret[i++] = val / divisor;
    }
    return ret;
  }

  /**
   * 
   * mmtf type 10 (first part)
   * 
   * Decode an array of int16 using run-length decoding of a difference array.
   * 
   * @param b
   * @param n
   * @param divisor 
   * @return array of integers
   */
  protected float[] recursiveIndex16Deltaf(byte[] b, int n, float divisor) {
    if (b == null)
      return null;
    float[] ret = new float[n];
    for (int i = 0, pt = 6, val = 0, buf = 0; i < n;) {
      int diff = BC.bytesToShort(b, (pt++) << 1, true);
      if (diff == 32767 || diff == -32768) {
        buf += diff;
      } else {
        ret[i++] = (val = val + diff + buf) / divisor;
        buf = 0;
      }
    }
    return ret;
  }

//  /**
//   * Decode an array of int16 using run-length decoding
//   * of a difference array.
//   * 
//   * @param b
//   * @param n
//   * @param i0 
//   * @return array of integers
//   */
//  protected int[] rldecode16Delta(byte[] b, int n, int i0) {
//    if (b == null)
//      return null;
//    int[] ret = new int[n];
//    for (int i = 0, pt = i0 / 2, val = 0; i < n;) {
//      int diff = BC.bytesToShort(b, (pt++) << 1, true);
//      for (int j = BC.bytesToShort(b, (pt++) << 1, true); --j >= 0;)
//        ret[i++] = (val = val + diff);
//    }
//    return ret;
//  }

  
//  /**
//   * Do a split delta to a float[] array
//   * @param xyz label "x", "y", "z", or "bFactor"
//   * @param factor for dividing in the end -- 1000f or 100f 
//   * @return float[]
//   * 
//   */ 
//  protected float[] getFloatsSplit(String xyz, float factor) {
//    byte[] big = (byte[]) map.get(xyz + "Big");
//    return (big == null ? null : splitDelta(big,
//        (byte[]) map.get(xyz + "Small"), fileAtomCount, factor));
//  }

//  /**
//   * Do a split delta to a float[] array
//   * 
//   * @param big
//   *        [n m n m n m...] where n is a "big delta" and m is a number of
//   *        "small deltas
//   * @param small
//   *        array containing the small deltas
//   * @param n
//   *        the size of the final array
//   * @param factor
//   *        to divide the final result by -- 1000f or 100f here
//   * @return float[]
//   */
//  protected float[] splitDelta(byte[] big, byte[] small, int n, float factor) {
//    float[] ret = new float[n];
//    for (int i = 0, smallpt = 0, val = 0, datapt = 0, len = big.length >> 2; i < len; i++) {
//      ret[datapt++] = (val = val + BC.bytesToInt(big, i << 2, true)) / factor;
//      if (++i < len)
//        for (int j = BC.bytesToInt(big, i << 2, true); --j >= 0; smallpt++)
//          ret[datapt++] = (val = val + BC.bytesToShort(small, smallpt << 1, true))
//              / factor;
//    }
//    return ret;
//  }
//

//////////////////////////////// MMTF-Specific /////////////////////////  
  
  private Map<String, Object> map; // input JSON-like map from MessagePack binary file  
  private int fileAtomCount;
  private int opCount = 0;
  private int[] groupModels;
  
  private String[] labelAsymList; // created in getAtoms; used in getBioAssembly
  private Atom[] atomMap; // necessary because some atoms may be deleted. 
    // TODO  - also consider mapping group indices

  /**
   * set up all atoms, including bonding within a group
   * 
   * @param doMulti true to add double bonds
   * 
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void getAtoms(boolean doMulti) throws Exception {
    
    // chains
    int[] chainsPerModel = (int[]) map.get("chainsPerModel");
    int[] groupsPerChain = (int[]) map.get("groupsPerChain"); // note that this is label_asym, not auth_asym
    labelAsymList = (String[]) decode("chainIdList"); // label_asym
    String[] authAsymList = (String[]) decode("chainNameList"); // Auth_asym

    // groups
    int[] groupTypeList = (int[]) decode("groupTypeList");
    int groupCount = groupTypeList.length;
    groupModels = new int[groupCount];
    int[] groupIdList = (int[]) decode("groupIdList");
    Object[] groupList = (Object[]) map.get("groupList");
    char[] insCodes = (char[]) decode("insCodeList");

    //o = map.get("entityList");
    //o = map.get("altLabelList");
    // 1crn: [7, 3, 3, 7, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 6, 6, 6, 7, 7, 2, 2, 2, 2, 2, 2, 2, 2, 1, 7, 3, 3, 7, 1, 1, 1, 7, 7, 7, 4, 4, 4, 7, 7]

    int[] atomId = (int[]) decode("atomIdList");
    boolean haveSerial = (atomId != null);

    char[] altloc = (char[]) decode("altLocList"); // rldecode32
    float[] occ = (float[]) decode("occupancyList");

    float[] x = (float[]) decode("xCoordList");//getFloatsSplit("xCoord", 1000f);
    float[] y = (float[]) decode("yCoordList");
    float[] z = (float[]) decode("zCoordList");
    float[] bf = (float[]) decode("bFactorList");
    int iatom = 0;
    String[] nameList = (useAuthorChainID ? authAsymList : labelAsymList);
    int iModel = -1;
    int iChain = 0;
    int nChain = 0;
    int iGroup = 0;
    int nGroup = 0;
    int chainpt = 0;
    int seqNo = 0;
    String chainID = "";
    String authAsym = "", labelAsym = "";
    char insCode = '\0';
    atomMap = new Atom[fileAtomCount];
    for (int j = 0; j < groupCount; j++) {
      int a0 = iatom;
      if (insCodes != null)
        insCode = insCodes[j];
      seqNo = groupIdList[j];
      if (++iGroup >= nGroup) {
        chainID = nameList[chainpt];
        authAsym = authAsymList[chainpt];
        labelAsym = labelAsymList[chainpt];
        nGroup = groupsPerChain[chainpt++];
        iGroup = 0;
        if (++iChain >= nChain) {
          groupModels[j] = ++iModel;
          nChain = chainsPerModel[iModel];
          iChain = 0;
          setModelPDB(true);
          incrementModel(iModel + 1);
          
          nAtoms0 = asc.ac;
        }
      }
      Map<String, Object> g = (Map<String, Object>) groupList[groupTypeList[j]];
      String group3 = (String) g.get("groupName");
      addHetero(group3, "" + g.get("chemCompType"), true);
      //System.out.println(group3 + " " + g.get("chemCompType"));
      String[] atomNameList = (String[]) g.get("atomNameList");
      String[] elementList = (String[]) g.get("elementList");
      int len = atomNameList.length;
      for (int ia = 0, pt = 0; ia < len; ia++, iatom++) {
        Atom a = new Atom();
        if (insCode != 0)
          a.insertionCode = insCode;        
        setAtomCoordXYZ(a, x[iatom], y[iatom], z[iatom]);
        a.elementSymbol = elementList[pt];
        a.atomName = atomNameList[pt++];
        if (seqNo >= 0)
          a.sequenceNumber = seqNo;
        a.group3 = group3;
        setChainID(a, chainID);
        if (bf != null)
          a.bfactor = bf[iatom];
        if (altloc != null)
          a.altLoc = altloc[iatom];
        if (occ != null)
          a.foccupancy = occ[iatom];
        if (haveSerial)
          a.atomSerial = atomId[iatom];
        if (!filterAtom(a, -1) || !processSubclassAtom(a, labelAsym, authAsym))
          continue;
        if (haveSerial) {
          asc.addAtomWithMappedSerialNumber(a);
        } else {
          asc.addAtom(a);
        }
        atomMap[iatom] = a;
        ++ac;
      }
      if (!isCourseGrained) {
        int[] bo = (int[]) g.get("bondOrderList");
        if (bo != null) {
          int[] bi = (int[]) g.get("bondAtomList");
          for (int bj = 0, pt = 0, nj = bo.length; bj < nj; bj++) {
            Atom a1 = atomMap[bi[pt++] + a0];
            Atom a2 = atomMap[bi[pt++] + a0];
            if (a1 != null && a2 != null)
              addBond(new Bond(a1.index, a2.index, doMulti ? bo[bj] : 1), false);
          }
        }
      }
    }
  }

  private void addBond(Bond bond, boolean isInter) {
    asc.addBond(bond);
    if (Logger.debugging && isInter) {
      Atom a1 =  asc.atoms[bond.atomIndex1];
      Atom a2 =  asc.atoms[bond.atomIndex2];
      Logger.info("bond " +a1.group3 + a1.sequenceNumber + "." + a1.atomName 
          + " " + a2.group3 + a2.sequenceNumber + "." + a2.atomName + " " + bond.order);
    }
  }

  private void getBonds(boolean doMulti) {
    int[] b = (int[]) decode("bondOrderList");
    int[] bi = (int[]) decode("bondAtomList");
    for (int i = 0, pt = 0, n = b.length; i < n; i++) {
      Atom a1 = atomMap[bi[pt++]];
      Atom a2 = atomMap[bi[pt++]];
      if (a1 != null && a2 != null)
        addBond(new Bond(a1.index, a2.index, doMulti ? b[i] : 1), true);
    }
  }

  private void setSymmetry() {
    setSpaceGroupName((String) map.get("spaceGroup"));
    float[] o = (float[]) map.get("unitCell");
    if (o != null)
      for (int i = 0; i < 6; i++)
        setUnitCellItem(i, o[i]);
  }

  @SuppressWarnings("unchecked")
  private void getBioAssembly() {
    Object[] o = (Object[]) map.get("bioAssemblyList");
    if (vBiomolecules == null)
      vBiomolecules = new Lst<Map<String, Object>>();      

    for (int i = o.length; --i >= 0;) {      
      Map<String, Object> info = new Hashtable<String, Object>();
      vBiomolecules.addLast(info);
      int iMolecule = i + 1;
      checkFilterAssembly("" + iMolecule, info);
      info.put("name", "biomolecule " + iMolecule);
      info.put("molecule", Integer.valueOf(iMolecule));
      Lst<String> assemb = new Lst<String>();
      Lst<String> ops = new Lst<String>();
      info.put("biomts", new Lst<M4>());
      info.put("chains", new Lst<String>());
      info.put("assemblies", assemb); 
      info.put("operators", ops);
      // need to add NCS here.
      Map<String, Object> m = (Map<String, Object>) o[i];
      Object[] tlist = (Object[]) m.get("transformList");
      SB chlist = new SB();
      
      for (int j = 0, n = tlist.length; j < n; j++) {

        Map<String, Object> t = (Map<String, Object>) tlist[j];        
        
        // for every transformation...

        chlist.setLength(0);
        
        // for compatibility with the mmCIF reader, we create
        // string lists of the chains in the form $A$B$C...
     
        int[] chainList = (int[]) t.get("chainIndexList");
        for (int k = 0, kn  = chainList.length; k < kn; k++)
          chlist.append("$").append(labelAsymList[chainList[k]]);
        assemb.addLast(chlist.append("$").toString());
        
        // now save the 4x4 matrix transform for this operation
        
        String id = "" + (++opCount);
        addMatrix(id, M4.newA16((float[]) t.get("matrix")), true);
        ops.addLast(id);
      }
    }
  }

  //  Code  Name
  //  0*   pi helix
  //  1   bend   (ignored)
  //  2*   alpha helix
  //  3*   extended (sheet)
  //  4*   3-10 helix
  //  5   bridge (ignored)
  //  6*   turn 
  //  7   coil (ignored)
  //  -1  undefined

  // 1F88: "secStructList": [7713371173311776617777666177444172222222222222222222222222222222166771222222222222222222262222222222617662222222222222222222222222222222222177111777722222222222222222221222261173333666633337717776666222222222226622222222222226611771777
  // DSSP (Jmol):            ...EE....EE....TT.....TTT...GGG..HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH.TT...HHHHHHHHHHHHHHHHHHHTHHHHHHHHHHT..TTHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH..........HHHHHHHHHHHHHHHHHHH.HHHHT...EEEETTTTEEEE......TTTTHHHHHHHHHHHTTHHHHHHHHHHHHHTT........
  
  /**
   * Get and translate the DSSP string from digit format
   *  
   * @param a input data
   */
  private void getStructure(byte[] a) {    
    BS[] bsStructures = new BS[] { new BS(), null, new BS(), new BS(), new BS(), null, new BS() };
    if (Logger.debugging)
      Logger.info(PT.toJSON("secStructList", a));
    int lastGroup = -1;
    for (int i = 0; i < a.length; i++) {
      int type = a[i];
      switch (type) {
      case 0: // PI
      case 2: // alpha
      case 3: // sheet
      case 4: // 3-10
      case 6: // turn
        bsStructures[type].set(i);
        lastGroup = i;
      }
    }
    
    int n = (isDSSP1 ? asc.iSet : groupModels[lastGroup]);
    if (lastGroup >= 0)
      asc.addStructure(new Structure(n, null, null, null, 0, 0, bsStructures));
  }

}

