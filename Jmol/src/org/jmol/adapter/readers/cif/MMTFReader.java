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
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.util.Logger;

/**
 * JmolData RCSB MMTF (macromolecular transmission format) file
 * see https://github.com/rcsb/mmtf/blob/master/spec.md
 * 
 * 
 * 
 * 
 */

public class MMTFReader extends MMCifReader {

  
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    isBinary = true;
    isMMCIF = true;
    setupASCR(fullPath, htParams, reader);
  }

  private Map<String, Object> map;
  private int nBonds;
  private int atomCount;
  
  @Override
  protected void processBinaryDocument() throws Exception {
    boolean doMulti = (!isCourseGrained && !checkFilterKey("NODOUBLE"));
    applySymmetryToBonds = true;
    map = (new MessagePackReader(binaryDoc, true)).readMap();
    Logger.info("MMTF version " + map.get("mmtfVersion"));
    Logger.info("MMTF Producer " + map.get("mmtfProducer"));
    appendLoadNote((String) map.get("title"));
    String id = (String) map.get("pdbId");
    if (id == null)
      id = (String) map.get("structureId"); // 1JGQ
    atomCount = ((Integer)map.get("numAtoms")).intValue();
    nBonds = ((Integer)map.get("numBonds")).intValue();
    Logger.info("id atoms bonds " + id + " " + atomCount + " " + nBonds);
    getAtoms(doMulti);
    if (!isCourseGrained)
      getBonds(doMulti);
    setSymmetry();
    getBioAssembly();
    setModelPDB(true);
    //System.out.println(Escape.e(map));
  }

  private String[] getAsymList(String name) {
    byte[] b = (byte[]) map.get(name);
    String[] id = new String[b.length / 4];
    out: for (int i = 0, len = id.length, pt = 0; i < len; i++) {
      SB sb = new SB();
      for (int j = 0; j < 4; j++) {
        switch (b[pt]) {
        case 0:
          id[i] = sb.toString();
          pt += 4 - j;
          continue out;
        default:
          sb.appendC((char) b[pt++]);
          continue;
        }
      }        
    }
    
    return id;
  }

  private void getBonds(boolean doMulti) {
    byte[] b = (byte[]) map.get("bondOrderList");
    int[] bi = getInts((byte[]) map.get("bondAtomList"), 4);
    for (int i = 0, pt = 0, n = b.length; i < n; i++) {
      int a1 = atomMap[bi[pt++]] - 1;
      int a2 = atomMap[bi[pt++]] - 1;
      if (a1 >= 0 && a2 >= 0)
        asc.addBond(new Bond(a1, a2, doMulti ? b[i] : 1));
    }
  }

  private void setSymmetry() {
    setSpaceGroupName((String) map.get("spaceGroup"));
    float[] o = (float[]) map.get("unitCell");
    if (o != null)
      for (int i = 0; i < 6; i++)
        setUnitCellItem(i, o[i]);
  }

  private int opCount = 0;
  
  @SuppressWarnings("unchecked")
  private void getBioAssembly() {
    Object[] o = (Object[]) map.get("bioAssemblyList");
    for (int i = o.length; --i >= 0;) {
      Map<String, Object> info = new Hashtable<String, Object>();
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
      Map<String, Object> m = (Map<String, Object>) o[i];
      Object[] tlist = (Object[]) m.get("transformList");
      for (int j = 0, n = tlist.length; j < n; j++) {
        SB chlist = new SB();
        Map<String, Object> t = (Map<String, Object>) tlist[j];
        int[] chainList = (int[]) t.get("chainIndexList");
        for (int k = 0, kn  = chainList.length; k < kn; k++)
          chlist.append("$").append(labelAsymList[chainList[k]]);
        String id = "" + (++opCount);
        addBiomt(id, M4.newA16((float[]) t.get("matrix")));
        ops.addLast(id);
        assemb.addLast(chlist.append("$").toString());
      }
      if (vBiomolecules == null)
        vBiomolecules = new Lst<Map<String, Object>>();      
      vBiomolecules.addLast(info);
    }
  }

  private String[] labelAsymList;
  private int[] atomMap;
  
  @SuppressWarnings("unchecked")
  private void getAtoms(boolean doMulti) throws Exception {
    int[] groupTypeList = getInts((byte[]) map.get("groupTypeList"), 4);
    int groupCount = groupTypeList.length;
    labelAsymList = getAsymList("chainIdList"); // label_asym
    String[] authAsymList = getAsymList("chainNameList"); // Auth_asym
    Object[] groupList = (Object[]) map.get("groupList");
    int[] groupsPerChain = (int[]) map.get("groupsPerChain"); // note that this is label_asym, not auth_asym
    int[] chainsPerModel = (int[]) map.get("chainsPerModel");
    Object o = map.get("insCodeList");
    /**
     * @j2sNative
     * 
     *            if (o[0] == null) o = null;
     * 
     */
    {
    }
    int[] insCodes = (o == null || o instanceof Object[] ? null : rldecode32(
        (byte[]) o, groupCount));

    o = map.get("secStructList");
    o = map.get("entityList");
    //o = map.get("altLabelList");
    // 1crn: [7, 3, 3, 7, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 6, 6, 6, 7, 7, 2, 2, 2, 2, 2, 2, 2, 2, 1, 7, 3, 3, 7, 1, 1, 1, 7, 7, 7, 4, 4, 4, 7, 7]

    int[] atomId = rldecode32Delta((byte[]) map.get("atomIdList"), atomCount);
    boolean haveSerial = (atomId != null);

    int[] altloc = rldecode32((byte[]) map.get("altLocList"), atomCount);
    int[] occ = rldecode32((byte[]) map.get("occupancyList"), atomCount);

    int[] seqList = rldecode32Delta((byte[]) map.get("sequenceIndexList"),
        groupCount);

    float[] x = getFloatsSplit("xCoord", 1000f);
    float[] y = getFloatsSplit("yCoord", 1000f);
    float[] z = getFloatsSplit("zCoord", 1000f);
    float[] bf = getFloatsSplit("bFactor", 100f);
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
    int insCode = 0;
    atomMap = new int[atomCount];
    for (int j = 0; j < groupCount; j++) {
      int a0 = iatom;
      if (insCodes != null)
        insCode = insCodes[j];
      seqNo = seqList[j];
      if (++iGroup >= nGroup) {
        chainID = nameList[iChain];
        authAsym = authAsymList[iChain];
        labelAsym = labelAsymList[iChain];
        nGroup = groupsPerChain[chainpt++];
        iGroup = 0;
        if (++iChain >= nChain) {
          iModel++;
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
          a.insertionCode = (char) insCode;
        a.set(x[iatom], y[iatom], z[iatom]);
        a.elementSymbol = elementList[pt];
        a.atomName = atomNameList[pt++];
        if (seqNo >= 0)
          a.sequenceNumber = seqNo;
        a.group3 = group3;
        setChainID(a, chainID);
        if (bf != null)
          a.bfactor = bf[iatom];
        if (altloc != null)
          a.altLoc = (char) altloc[iatom];
        if (occ != null)
          a.foccupancy = occ[iatom] / 100f;
        if (haveSerial)
          a.atomSerial = atomId[iatom];
        if (!filterAtom(a, -1) || !processSubclassAtom(a, labelAsym, authAsym))
          continue;
        if (haveSerial) {
          asc.addAtomWithMappedSerialNumber(a);
        } else {
          asc.addAtom(a);
        }
        // map to [1....n] not [0...n] so that
        atomMap[iatom] = ++ac;
      }
      if (!isCourseGrained) {
        int[] bo = (int[]) g.get("bondOrderList");
        if (bo != null) {
          int[] bi = (int[]) g.get("bondAtomList");
          for (int bj = 0, pt = 0, nj = bo.length; bj < nj; bj++) {
            int a1 = atomMap[bi[pt++] + a0] - 1;
            int a2 = atomMap[bi[pt++] + a0] - 1;
            if (a1 >= 0 && a2 >= 0)
              asc.addBond(new Bond(a1, a2, doMulti ? bo[bj] : 1));
          }
        }
      }
    }
  }

  /**
   * decode an array of int32 using run-length decoding
   * 
   * @param b
   * @param n
   * @return array of integers
   */
  private int[] rldecode32(byte[] b, int n) {
    if (b == null)
      return null;
    int[] ret = new int[n];
    for (int i = 0, pt = -1; i < n;) {
      int val = BC.bytesToInt(b, (++pt) << 2, true);
      for (int j = BC.bytesToInt(b, (++pt) << 2, true); --j >= 0;)
         ret[i++] = val;
    }
    return ret;
  }

  /**
   * decode an array of int32 using run-length decoding
   * of a difference array.
   * 
   * @param b
   * @param n
   * @return array of integers
   */
  private int[] rldecode32Delta(byte[] b, int n) {
    if (b == null)
      return null;
    int[] ret = new int[n];
    for (int i = 0, pt = 0, val = 0; i < n;) {
      int diff = BC.bytesToInt(b, (pt++) << 2, true);
      for (int j = BC.bytesToInt(b, (pt++) << 2, true); --j >= 0;)
         ret[i++] = (val = val + diff);
    }
    return ret;
  }

  private float[] getFloatsSplit(String xyz, float factor) {
    byte[] big = (byte[]) map.get(xyz + "Big");
    if (big == null)
      return null;
    byte[] small = (byte[]) map.get(xyz + "Small");
    return splitDelta(big, small, atomCount, factor);
  }

  private float[] splitDelta(byte[] big, byte[] small, int n, float factor) {
    float[] ret = new float[n];
    for (int i = 0, smallpt = 0, val = 0, datapt = 0, len = big.length >> 2; i < len; i++) {
      ret[datapt++] = (val = val + BC.bytesToInt(big, i << 2, true)) / factor;
      if (++i < len)
        for (int j = BC.bytesToInt(big, i << 2, true); --j >= 0; smallpt++)
          ret[datapt++] = (val = val + BC.bytesToShort(small, smallpt << 1, true))
              / factor;
    }
    return ret;
  }

  /**
   * 
   * @param b
   * @param nbytes  2 (int16) or 4 (int32)
   * @return int array
   */
  private int[] getInts(byte[] b, int nbytes) {
    if (b == null)
      return null;
    int len = b.length / nbytes;
    int[] a = new int[len];
    switch (nbytes) {
    case 2:
      for (int i = 0, j = 0; i < len; i++, j += nbytes)
        a[i] = BC.bytesToShort(b, j, true);
      break;
    case 4:
      for (int i = 0, j = 0; i < len; i++, j += nbytes)
        a[i] = BC.bytesToInt(b, j, true);
      break;
    }
    return a;
  }

  @Override
  protected void addHeader() {
    // no header for this type
  }

}

