/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.pymol;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmol.adapter.readers.cifpdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.api.JmolDocument;
import org.jmol.util.BoxInfo;
//import org.jmol.util.Escape;
import org.jmol.util.Point3f;
import org.jmol.util.StringXBuilder;

/**
 * experimental PyMOL PSE (binary Python serialization) file
 * 
 * bigendian file.
 * 
 */

public class PyMOLReader extends PdbReader {

  final private static byte APPEND = 97; /* a */
  final private static byte APPENDS = 101; /* e */
  final private static byte BINFLOAT = 71; /* G */
  final private static byte BININT = 74; /* J */
  final private static byte BININT1 = 75; /* K */
  final private static byte BININT2 = 77; /* M */
  final private static byte BINPUT = 113; /* q */
  final private static byte BINSTRING = 84; /* T */
  final private static byte BINUNICODE = 87; /* X */
  final private static byte BUILD = 98; /* b */
  final private static byte EMPTY_DICT = 125; /* } */
  final private static byte EMPTY_LIST = 93; /* ] */
  final private static byte GLOBAL = 99; /* c */
  final private static byte LONG_BINPUT = 114; /* r */
  final private static byte MARK = 40; /* ( */
  final private static byte NONE = 78; /* N */
  final private static byte OBJ = 111; /* o */
  final private static byte SETITEM = 115; /* s */
  final private static byte SETITEMS = 117; /* u */
  final private static byte SHORT_BINSTRING = 85; /* U */
  final private static byte STOP = 46; /* . */
  
//  final private static byte BINGET = 104; /* h */
//  final private static byte BINPERSID = 81; /* Q */
//  final private static byte DICT = 100; /* d */
//  final private static byte DUP = 50; /* 2 */
//  final private static byte EMPTY_TUPLE = 41; /* ) */
//  final private static byte FLOAT = 70; /* F */
//  final private static byte GET = 103; /* g */
//  final private static byte INST = 105; /* i */
//  final private static byte INT = 73; /* I */
//  final private static byte LIST = 108; /* l */
//  final private static byte LONG = 76; /* L */
//  final private static byte LONG_BINGET = 106; /* j */
//  final private static byte PERSID = 80; /* P */
//  final private static byte POP = 48; /* 0 */
//  final private static byte POP_MARK = 49; /* 1 */
//  final private static byte PUT = 112; /* p */
//  final private static byte REDUCE = 82; /* R */
//  final private static byte STRING = 83; /* S */
//  final private static byte TUPLE = 116; /* t */
//  final private static byte UNICODE = 86; /* V */
  
  
  //private static final int MARK_BONDS = 40;
  //private static final int MARK_COORDS = 39;
  //private static final int MARK_ATOMS = 41;
  private static final int SET_FOV = 152;
  private JmolDocument binaryDoc;

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    super.initializeReader();
  }
  
//  private Map<Object, Object> temp  = new Hashtable<Object, Object>();

  private List<Object> list = new ArrayList<Object>();
  private List<Integer> marks = new ArrayList<Integer>();
  private List<Object> build = new ArrayList<Object>();
  private int nextMark;
//  private List<Object> thisList;
//  private List<Object> coords;
//  private List<Object> bonds;
  private Point3f xyzMin = Point3f.new3(1e6f, 1e6f, 1e6f);
  private Point3f xyzMax = Point3f.new3(-1e6f, -1e6f, -1e6f);
  private int nModels;
  private float thisBfactor;
  private float thisOcc;

  @SuppressWarnings("unchecked")
  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    this.binaryDoc = doc;
    String s, module, name;
    byte b;
    int i, mark;
    double d;
    Object o;
    byte[] a;
    Map<String, Object> map;
    List<Object> l;
    boolean going = true;

    while (going) {
      b = binaryDoc.readByte();
      switch (b) {
      case EMPTY_DICT: //}
        push(new Hashtable<String, Object>());
        //System.out.println("emptyDict at " + list.size());
        break;
      case APPEND:
        o = pop();
        ((List<Object>) peek()).add(o);
        //System.out.println("append to " + list.size());
        break;
      case APPENDS:
        l = getObjects(getMark());
        ((List<Object>) peek()).addAll(l);
        //System.out.println("appends " + l.size() + " to " + list.size() + " nextMark = " + nextMark);
//        switch (nextMark) {
//        case MARK_ATOMS:
//          addAtom(l);
//          break;
//        case MARK_COORDS:
//          if (coords == null)
//            coords = ((List<Object>) peek());
//          break;
//        case MARK_BONDS:
//          if (bonds == null && l.size() == 7)
//            bonds = ((List<Object>) list.get(nextMark - 1));
//          break;
//        }
        break;
      case BINFLOAT:
        d = binaryDoc.readDouble();
        push(Double.valueOf(d));
        break;
      case BININT:
        i = binaryDoc.readInt();
        push(Integer.valueOf(i));
        break;
      case BININT1:
        i = binaryDoc.readByte() & 0xff;
        push(Integer.valueOf(i));
        break;
      case BININT2:
        i = (binaryDoc.readByte() & 0xff | ((binaryDoc.readByte() & 0xff) << 8)) & 0xffff;
        push(Integer.valueOf(i));
        break;
      case BINPUT:
        i = binaryDoc.readByte();
        //unnec? temp.put(Integer.valueOf(i), peek());
        break;
      case LONG_BINPUT:
        i = binaryDoc.readInt();
        //unnec? temp.put(Integer.valueOf(i), peek());
        break;
      case SHORT_BINSTRING:
        i = binaryDoc.readByte();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a);
        push(s);
        //if (s.length() > 0)
          //System.out.println(list.size() + " = " + s);
        break;
      case BINSTRING:
        i = binaryDoc.readInt();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a);
        push(s);
        break;
      case BINUNICODE:
        i = binaryDoc.readInt();
        a = new byte[i];
        binaryDoc.readByteArray(a, 0, i);
        s = new String(a, "UTF-8");
        push(s);
        break;
      case EMPTY_LIST:
        push(new ArrayList<Object>());
        break;
      case GLOBAL:
        module = readString();
        name = readString();
        push(new String[] { "global", module, name });
        break;
      case BUILD:
        o = pop();
        build.add(o);
        //System.out.println("build");
        break;
      case MARK:
        i = list.size();
        //System.out.println("mark " + i);
        marks.add(Integer.valueOf(i));
        break;
      case NONE:
        push(null);
        break;
      case OBJ:
        //System.out.println("OBJ");
        push(getObjects(getMark()));
        break;
      case SETITEM:
        o = pop();
        s = (String) pop();
        //System.out.println("setItem ." + s + " to " + list.size());
        ((Map<String, Object>) peek()).put(s, o);
        break;
      case SETITEMS:
        mark = getMark();
        l = getObjects(mark);
        map = (Map<String, Object>) peek();
        //System.out.println("setItems to " + list.size());
        for (i = l.size(); --i >= 0;) {
          o = l.get(i);
          s = (String) l.get(--i);
        //  System.out.println(" " + (mark + i) + " ." + s);
          map.put(s, o);
        }
        break;
      case STOP:
        going = false;
        break;
      default:
        
        // not used?
        System.out.println("PyMOL reader error: " + b + " " + binaryDoc.getPosition());

//        switch (b) {
//        case BINGET:
//          i = binaryDoc.readByte();
//          push(temp.remove(Integer.valueOf(i)));
//          break;
//        case BINPERSID:
//          s = (String) pop();
//          push(new Object[] { "persid", s }); // for now
//          break;
//        case DICT:
//          map = new Hashtable<String, Object>();
//          mark = getMark();
//          for (i = list.size(); i >= mark;) {
//            o = list.remove(--i);
//            s = (String) list.remove(--i);
//            map.put(s, o);
//          }
//          push(map);
//          break;
//        case DUP:
//          push(peek());
//          break;
//        case EMPTY_TUPLE:
//          push(new Point3f());
//          break;
//        case FLOAT:
//          s = readString();
//          push(Double.valueOf(s));
//          break;
//        case GET:
//          s = readString();
//          o = temp.remove(s);
//          push(o);
//          break;
//        case INST:
//          l = getObjects(getMark());
//          module = readString();
//          name = readString();
//          push(new Object[] { "inst", module, name, l });
//          break;
//        case INT:
//          s = readString();
//          try {
//            push(Integer.valueOf(Integer.parseInt(s)));
//          } catch (Exception e) {
//            System.out.println("INT too large: " + s + " @ " + binaryDoc.getPosition());
//            push(Integer.valueOf(Integer.MAX_VALUE));
//          }
//          break;
//        case LIST:
//          push(getObjects(getMark()));
//          break;
//        case LONG:
//          i = (int) binaryDoc.readLong();
//          push(Long.valueOf(i));
//          break;
//        case LONG_BINGET:
//          i = binaryDoc.readInt();
//          push(temp.remove(Integer.valueOf(i)));
//          break;
//        case PERSID:
//          s = readString();
//          push(new Object[] { "persid", s });
//          break;
//        case POP:
//          pop();
//          break;
//        case POP_MARK:
//          getObjects(getMark());
//          break;
//        case PUT:
//          s = readString();
//          temp.put(s, peek());
//          break;
//        case REDUCE:
//          push(new Object[] { "reduce", pop(), pop() });
//          break;
//        case STRING:
//          s = readString();
//          push(Escape.unescapeUnicode(s));
//          break;
//        case TUPLE:
//          l = getObjects(getMark());
//          Point3f pt = new Point3f();
//          pt.x = ((Double) l.get(0)).floatValue();
//          pt.y = ((Double) l.get(1)).floatValue();
//          pt.z = ((Double) l.get(2)).floatValue();
//          break;
//        case UNICODE:
//          a = readLineBytes();
//          s = new String(a, "UTF-8");
//          push(s);
//          break;
//        }
      }
    }
    process((Map<String, Object>) list.remove(0));
  }

  private static String getString(List<Object> list, int i) {
    String s = (String) list.get(i);
    return (s.length() == 0 ? " " : s);
  }

  private static int getInt(List<Object> list, int i) {
    return ((Integer) list.get(i)).intValue();
  }

  private static float getFloat(List<Object> list, int i) {
    return ((Double) list.get(i)).floatValue();
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getList(List<Object> list, int i) {
    if (list == null || list.size() <= i)
      return null;
    Object o = list.get(i);
    return (o instanceof List<?> ? (List<Object>)o : null);
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getMapList(Map<String, Object> map, String key) {
    return (List<Object>) map.get(key);
  }

  private void process(Map<String, Object> map) {
    List<Object> view = getMapList(map, "view");
    List<Object> settings = getMapList(map, "settings");
    List<Object> names = getMapList(map, "names");
    for (int i = 1; i < names.size(); i++) { 
      processBranch(getList(names, i));
//      List<Object> item = getList(names, i);
//      if (item == null)
//        continue;
//      System.out.println(i + " " + item.get(0) + " "
//          + getList(getList(item, 5), 0));
    }
//    System.out.println("--");
    setView(view, settings);
  }

  private final static int BRANCH_MOLECULE = 1;
  private final static int BRANCH_MAPSURFACE = 2;
  private final static int BRANCH_MAPMESH = 3;
  private final static int BRANCH_MEASURE = 4;
  private final static int BRANCH_CGO = 6; // compiled graphics object
  private final static int BRANCH_SURFACE = 7;
  private final static int BRANCH_GROUP = 12;

//  #cf. \pymol\layer1\PyMOLObject.h
//  #define cObjectCallback     5
//  #define cObjectGadget       8
//  #define cObjectCalculator   9
//  #define cObjectSlice        10
//  #define cObjectAlignment    11

  
  private void processBranch(List<Object> branch) {
    String name = (String) branch.get(0);
    if (name.indexOf("_") == 0 || getInt(branch, 1) != 0) // otherwise, it's just a selection
      return;
    boolean isHidden = (getInt(branch, 2) != 1);
    System.out.println("Branch " + name + " isHidden = " + isHidden);
    int type = getInt(branch, 4);
    List<Object> deepBranch = getList(branch, 5);
    switch (type) {
    case BRANCH_MOLECULE:
      processModels(deepBranch);
      break;
    case BRANCH_MAPSURFACE:
      break;
    case BRANCH_MAPMESH:
      break;
    case BRANCH_MEASURE:
      break;
    case BRANCH_CGO:
      break;
    case BRANCH_SURFACE:
      break;
    case BRANCH_GROUP:
      break;
    }
  }

  
  private int atomCount;
  private int[] atomMap;
  private void processModels(List<Object> deepBranch) {
    processCryst(getList(deepBranch, 10));
    atomCount = atomSetCollection.getAtomCount();
    atomMap = new int[getInt(deepBranch, 3)];
    List<Object> coordBranches = getList(deepBranch, 4);
    List<Object> bonds = getList(deepBranch, 6);
    List<Object> pymolAtoms = getList(deepBranch, 7);
    System.out.println("PyMOL model count = " + coordBranches.size());
    int lastEnd = -1;
    for (int i = 0; i < coordBranches.size(); i++) {

      List<Object> coordBranch = getList(coordBranches, i);
      int thisCount = getInt(coordBranch, 0);
      int thisEnd = getInt(coordBranch, 1);
      if (thisEnd != lastEnd) {
        model(++nModels);
        lastEnd = thisEnd;
      }
      //if (nModels < 9)
        //continue;
      List<Object> coords = getList(coordBranch, 2);
      List<Object> pointers = getList(coordBranch, 3);
      int n = pointers.size();
      for (int j = 0; j < n; j++) {
        int pt = getInt(pointers, j);
        atomMap[pt] = atomCount++;
        addAtom(pymolAtoms, pt, coords);
      }
    }
    processBonds(bonds);
  }

//chain     => 1,     # chain ID
//ac        => 2,     # alternate conformation indicator
//segid     => 4,     # segment ID
//          
//resix     => 0,     # without insertion code, unlike "resno"
//resno     => 3,     # using this and not the sequence number (resix) to deal with boundary case of insertion code... untested    
//residue   => 5,     # 3-letter identifier
//          
//atomno    => 22,    # original PDB atom number
//atom      => 6,     # (e.g. CB, NZ)
//symbol    => 7,     # (e.g. C, N)
//type      => 13,    # internal index number of "atom name"
//mol2      => 8,     # MOL2 atom type (i.e. N.am)
//charge    => 18,    # atom charge
//
//bf        => 14,    # temperature factor
//occ       => 15,    # occupany
//vdw       => 16,    # van der Waals radius
//          
//color     => 21,    # color code index
//label     => 9,     # label text
//          
//reps      => 20,    # representation flags 
//ss        => 10,    # s.s. assignment, S/H/L/""
//
//cartoon   => 23,    # cartoon type modifier
//
//### UNMAPPED: 11, 12, 17, 19

private void addAtom(List<Object> pymolAtoms, int pt, List<Object> coords) {
  List<Object> a = getList(pymolAtoms, pt);
  int seqNo = getInt(a, 0);
  String chainID = getString(a, 1);
  String altLoc = getString(a, 2);
  String insCode = " "; //?    
  String group3 = getString(a, 5);
  if (group3.length() > 3)
    group3 = group3.substring(0, 3);
  if (group3.equals(" "))
    group3 = "UNK";
  String name = getString(a, 6);
  String sym = getString(a, 7);
  if (sym.equals(" "))
    sym = getString(a, 7);
  List<Object> list2 = getList(a, 20);
  boolean isHetero = (getInt(list2, 0) == 1);
  Atom atom = processAtom(name, altLoc.charAt(0), 
      group3, chainID.charAt(0), seqNo, insCode.charAt(0),
     isHetero, sym);
  if (!filterPDBAtom(atom, fileAtomIndex++))
    return;
  int serNo = getInt(a, 22);
  int cpt = getInt(a, 36) * 3;
  float x = getFloat(coords, cpt);
  float y = getFloat(coords, ++cpt);
  float z = getFloat(coords, ++cpt);
  BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
  //System.out.println(chainID +  " " + fileAtomIndex + " " + serNo + " " + x  + " " + y + " " + z);
  int charge = 0; //?
  thisBfactor = getFloat(a, 14);
  thisOcc = getFloat(a, 15);
  processAtom2(atom, serNo, x, y, z, charge);
}
 
@Override
protected void setAdditionalAtomParameters(Atom atom) {
  atom.bfactor = thisBfactor;
  atom.occupancy = (int) (thisOcc * 100);
}


  private void processBonds(List<Object> bonds) {
    for (int i = 0; i < bonds.size(); i++) {
      List<Object> b = getList(bonds, i);
      int order = getInt(b, 2);
      if (order < 1 || order > 3)
        order = 1;
      // TODO: hydrogen bonds?
      int ia = atomMap[getInt(b, 0)];
      int ib = atomMap[getInt(b, 1)];
      if (ia == 2198 && ib==2157)
      System.out.println("bond " + i + " " + getInt(b, 0) + " " + getInt(b, 1) + " / " + ia + " " + ib);
      atomSetCollection.addBond(new Bond(ia, ib, order));
    }
  }

  private void processCryst(List<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    List<Object> l = getList(cryst, 0);
    List<Object> a = getList(cryst, 1);
    setUnitCell(getFloat(l, 0), getFloat(l, 1), getFloat(l, 2),
        getFloat(a, 0), getFloat(a, 1), getFloat(a, 2));
    setSpaceGroupName(getString(cryst, 2));
  }

  private void setView(List<Object> view, List<Object> settings) {
    float fov = getFloat(getList(settings, SET_FOV), 2);
    StringXBuilder sb = new StringXBuilder();
    float d = Math.abs(xyzMax.x-xyzMin.x);
    d = Math.max(d, Math.abs(xyzMax.y-xyzMin.y));
    d = Math.max(d, Math.abs(xyzMax.z-xyzMin.z));
    d /= 2;
    //float fov = getFloat(24);
    float f = Math.abs(getFloat(view, 18) - getFloat(view, 21));
    System.out.println("fov, range: " + fov + " " + getFloat(view, 18) + " " + getFloat(view, 21) + " " + f);
    sb.append("{ p2j_ar = (_width+0.0) / (_height+0.0); p2j_fov = ").appendF(fov)
      .append(";if( p2j_ar < 1) { p2j_fov*=p2j_ar};")
      .append("zoom @{(").appendF(d).append(" / (").appendF(f)
      .append(" * ( sin(p2j_fov/2.0) / cos(p2j_fov/2.0) ) ) * 100 )} };");

    
    sb.append("center {")
    .appendF(getFloat(view, 19)).append(" ")
    .appendF(getFloat(view, 20)).append(" ")
    .appendF(getFloat(view, 21)).append("};");
    sb.append("rotate @{quaternion({") // only the first two rows are needed
      .appendF(getFloat(view, 0)).append(" ")
      .appendF(getFloat(view, 1)).append(" ")
      .appendF(getFloat(view, 2)).append("}{")
      .appendF(getFloat(view, 4)).append(" ")
      .appendF(getFloat(view, 5)).append(" ")
      .appendF(getFloat(view, 6)).append("})};");
    sb.append("translate X ").appendF(getFloat(view, 16)).append(" angstroms;");
    sb.append("translate Y ").appendF(-getFloat(view, 17)).append(" angstroms;");


    
    
    addJmolScript(sb.toString());
    
  }

  private List<Object> getObjects(int mark) {
    int n = list.size() - mark;
    List<Object> args = new ArrayList<Object>();
    for (int j = 0; j < n; j++)
      args.add(null);
    for (int j = n, i = list.size(); --i >= mark;)
      args.set(--j, list.remove(i));
    return args;
  }
  
//  private byte[] readLineBytes() throws Exception {
//    String s = readString();
//    return s.getBytes();
//  }

  private String readString() throws Exception {
    StringXBuilder sb = new StringXBuilder();
    while(true) {
      byte b = binaryDoc.readByte();
      if (b == 0xA)
        break;
      sb.appendC((char)b);
    }
    return sb.toString();
  }
  
  private int getMark() {
    int mark = marks.remove(marks.size() - 1).intValue();
    nextMark = (mark == 1 ? 0 : marks.get(marks.size() - 1).intValue());
    return mark;
  }

  private void push(Object o) {
    list.add(o);
  }
  
  private Object peek() {
    return list.get(list.size() - 1);
  }

  private Object pop() {
    return list.remove(list.size() - 1);
  }
}
