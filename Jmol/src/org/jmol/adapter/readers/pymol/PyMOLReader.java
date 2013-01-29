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
import org.jmol.util.Escape;
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
  final private static byte BINGET = 104; /* h */
  final private static byte BININT = 74; /* J */
  final private static byte BININT1 = 75; /* K */
  final private static byte BININT2 = 77; /* M */
  final private static byte BINPERSID = 81; /* Q */
  final private static byte BINPUT = 113; /* q */
  final private static byte BINSTRING = 84; /* T */
  final private static byte BINUNICODE = 87; /* X */
  final private static byte BUILD = 98; /* b */
  final private static byte DICT = 100; /* d */
  final private static byte DUP = 50; /* 2 */
  final private static byte EMPTY_DICT = 125; /* } */
  final private static byte EMPTY_LIST = 93; /* ] */
  final private static byte EMPTY_TUPLE = 41; /* ) */
  final private static byte FLOAT = 70; /* F */
  final private static byte GET = 103; /* g */
  final private static byte GLOBAL = 99; /* c */
  final private static byte INST = 105; /* i */
  final private static byte INT = 73; /* I */
  final private static byte LIST = 108; /* l */
  final private static byte LONG = 76; /* L */
  final private static byte LONG_BINGET = 106; /* j */
  final private static byte LONG_BINPUT = 114; /* r */
  final private static byte MARK = 40; /* ( */
  final private static byte NONE = 78; /* N */
  final private static byte OBJ = 111; /* o */
  final private static byte PERSID = 80; /* P */
  final private static byte POP = 48; /* 0 */
  final private static byte POP_MARK = 49; /* 1 */
  final private static byte PUT = 112; /* p */
  final private static byte REDUCE = 82; /* R */
  final private static byte SETITEM = 115; /* s */
  final private static byte SETITEMS = 117; /* u */
  final private static byte SHORT_BINSTRING = 85; /* U */
  final private static byte STOP = 46; /* . */
  final private static byte STRING = 83; /* S */
  final private static byte TUPLE = 116; /* t */
  final private static byte UNICODE = 86; /* V */
  
  
  private static final int MARK_BONDS = 40;
  private static final int MARK_COORDS = 39;
  private static final int MARK_ATOMS = 41;
  private static final int SET_FOV = 152;
  private JmolDocument binaryDoc;

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    super.initializeReader();
  }
  
  private Map<Object, Object> temp  = new Hashtable<Object, Object>();

  private List<Object> list = new ArrayList<Object>();
  private List<Integer> marks = new ArrayList<Integer>();
  private List<Object> build = new ArrayList<Object>();
  private int nextMark;
  private List<Object> thisList;
  private List<Object> coords;
  private List<Object> bonds;
  private Point3f xyzMin = Point3f.new3(1e6f, 1e6f, 1e6f);
  private Point3f xyzMax = Point3f.new3(-1e6f, -1e6f, -1e6f);

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
        System.out.println("appends " + l.size() + " to " + list.size() + " nextMark = " + nextMark);
        switch (nextMark) {
        case MARK_ATOMS:
          addAtom(l);
          break;
        case MARK_COORDS:
          if (coords == null)
            coords = ((List<Object>) peek());
          break;
        case MARK_BONDS:
          if (bonds == null && l.size() == 7)
            bonds = ((List<Object>) list.get(nextMark - 1));
          break;
        }
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
        System.out.println("setItem ." + s + " to " + list.size());
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
          System.out.println(" " + (mark + i) + " ." + s);
          map.put(s, o);
        }
        break;
      case STOP:
        going = false;
        break;
      default:
        
        // not used?
        System.out.println("PyMOL reader: " + b + " " + binaryDoc.getPosition());

        switch (b) {
        case BINGET:
          i = binaryDoc.readByte();
          push(temp.remove(Integer.valueOf(i)));
          break;
        case BINPERSID:
          s = (String) pop();
          push(new Object[] { "persid", s }); // for now
          break;
        case DICT:
          map = new Hashtable<String, Object>();
          mark = getMark();
          for (i = list.size(); i >= mark;) {
            o = list.remove(--i);
            s = (String) list.remove(--i);
            map.put(s, o);
          }
          push(map);
          break;
        case DUP:
          push(peek());
          break;
        case EMPTY_TUPLE:
          push(new Point3f());
          break;
        case FLOAT:
          s = readString();
          push(Double.valueOf(s));
          break;
        case GET:
          s = readString();
          o = temp.remove(s);
          push(o);
          break;
        case INST:
          l = getObjects(getMark());
          module = readString();
          name = readString();
          push(new Object[] { "inst", module, name, l });
          break;
        case INT:
          s = readString();
          try {
            push(Integer.valueOf(Integer.parseInt(s)));
          } catch (Exception e) {
            System.out.println("INT too large: " + s + " @ " + binaryDoc.getPosition());
            push(Integer.valueOf(Integer.MAX_VALUE));
          }
          break;
        case LIST:
          push(getObjects(getMark()));
          break;
        case LONG:
          i = (int) binaryDoc.readLong();
          push(Long.valueOf(i));
          break;
        case LONG_BINGET:
          i = binaryDoc.readInt();
          push(temp.remove(Integer.valueOf(i)));
          break;
        case PERSID:
          s = readString();
          push(new Object[] { "persid", s });
          break;
        case POP:
          pop();
          break;
        case POP_MARK:
          getObjects(getMark());
          break;
        case PUT:
          s = readString();
          temp.put(s, peek());
          break;
        case REDUCE:
          push(new Object[] { "reduce", pop(), pop() });
          break;
        case STRING:
          s = readString();
          push(Escape.unescapeUnicode(s));
          break;
        case TUPLE:
          l = getObjects(getMark());
          Point3f pt = new Point3f();
          pt.x = ((Double) l.get(0)).floatValue();
          pt.y = ((Double) l.get(1)).floatValue();
          pt.z = ((Double) l.get(2)).floatValue();
          break;
        case UNICODE:
          a = readLineBytes();
          s = new String(a, "UTF-8");
          push(s);
          break;
        }
      }
    }
    process((Map<String, Object>) list.remove(0));
  }

//  chain     => 1,     # chain ID
//  ac        => 2,     # alternate conformation indicator
//  segid     => 4,     # segment ID
//            
//  resix     => 0,     # without insertion code, unlike "resno"
//  resno     => 3,     # using this and not the sequence number (resix) to deal with boundary case of insertion code... untested    
//  residue   => 5,     # 3-letter identifier
//            
//  atomno    => 22,    # original PDB atom number
//  atom      => 6,     # (e.g. CB, NZ)
//  symbol    => 7,     # (e.g. C, N)
//  type      => 13,    # internal index number of "atom name"
//  mol2      => 8,     # MOL2 atom type (i.e. N.am)
//  charge    => 18,    # atom charge
//
//  bf        => 14,    # temperature factor
//  occ       => 15,    # occupany
//  vdw       => 16,    # van der Waals radius
//            
//  color     => 21,    # color code index
//  label     => 9,     # label text
//            
//  reps      => 20,    # representation flags 
//  ss        => 10,    # s.s. assignment, S/H/L/""
//  
//  cartoon   => 23,    # cartoon type modifier
//
//  ### UNMAPPED: 11, 12, 17, 19

  @SuppressWarnings("unchecked")
  private void addAtom(List<Object> list) {
    if (list.size() != 47) {
      System.out.println(list);
      return;
    }
    thisList = list;
    
    int seqNo = getInt(0);
    String chainID = getString(1);
    String altLoc = getString(2);
    String insCode = " "; //?    
    String group3 = getString(5);
    if (group3.equals(" "))
      return;
    String name = getString(6);
    String sym = getString(7);
    if (sym.equals(" "))
      sym = getString(7);
    List<Object> list2 = (List<Object>) list.get(20);
    boolean isHetero = ((Integer) list2.get(0)).intValue() == 1;
    Atom atom = processAtom(name, altLoc.charAt(0), 
        group3, chainID.charAt(0), seqNo, insCode.charAt(0),
       isHetero, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return;
    int serNo = getInt(22);
    int pt = getInt(36) * 3;
    float x = getCoord(pt);
    float y = getCoord(++pt);
    float z = getCoord(++pt);
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    //System.out.println(chainID +  " " + fileAtomIndex + " " + serNo + " " + x  + " " + y + " " + z);
    int charge = 0; //?
    processAtom2(atom, serNo, x, y, z, charge);
  }
   
  private float getCoord(int pt) {
    return ((Double) coords.get(pt)).floatValue();
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    atom.bfactor = getFloat(14);
    atom.occupancy = (int) (getFloat(15) * 100);
  }

  private String getString(int i) {
    String s = (String) thisList.get(i);
    return (s.length() == 0 ? " " : s);
  }

  private int getInt(int i) {
    return ((Integer) thisList.get(i)).intValue();
  }

  private float getFloat(int i) {
    return ((Double) thisList.get(i)).floatValue();
  }

  @SuppressWarnings("unchecked")
  private void process(Map<String, Object> map) {
    connect();
    List<Object> view = (List<Object>) map.get("view");
    List<Object> settings = (List<Object>) map.get("settings");
    setView(view, settings);
    List<Object> names = (List<Object>) map.get("names");
    for (int i = 0; i < names.size(); i++) {
      List<Object> item = getList(names, i);
      if (item == null)
        continue;
      System.out.println(i + " " + item.get(0) + " "
          + getList(getList(item, 5), 0));
    }
    System.out.println("--");

  }

  @SuppressWarnings("unchecked")
  private void connect() {
    if (bonds == null) {
      System.out.println("bonds not found");
      return;
    }
    for (int i = 0; i < bonds.size(); i++) {
      thisList = (List<Object>) bonds.get(i);
      int order = getInt(2);
      if (order < 1 || order > 3)
        continue; // for now
      order = 1; // lots of issues with, for example, C=O=CH3 in t.pse
      System.out.println(thisList);
      try {
        atomSetCollection.addBond(new Bond(getInt(0), getInt(1), order));
      } catch (Exception e) {
        // ignore - some sort of phantom atom
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setView(List<Object> view, List<Object> settings) {
    thisList = (List<Object>) settings.get(SET_FOV);
    float fov = getFloat(2);

    thisList = view;
    StringXBuilder sb = new StringXBuilder();
    float d = Math.abs(xyzMax.x-xyzMin.x);
    d = Math.max(d, Math.abs(xyzMax.y-xyzMin.y));
    d = Math.max(d, Math.abs(xyzMax.z-xyzMin.z));
    d /= 2;
    //float fov = getFloat(24);
    float f = Math.abs(getFloat(18) - getFloat(21));
    System.out.println("fov, range: " + fov + " " + getFloat(18) + " " + getFloat(21) + " " + f);
    sb.append("{ p2j_ar = (_width+0.0) / (_height+0.0); p2j_fov = ").appendF(fov)
      .append(";if( p2j_ar < 1) { p2j_fov*=p2j_ar};")
      .append("zoom @{(").appendF(d).append(" / (").appendF(f)
      .append(" * ( sin(p2j_fov/2.0) / cos(p2j_fov/2.0) ) ) * 100 )} };");

    
    sb.append("center {")
    .appendF(getFloat(19)).append(" ")
    .appendF(getFloat(20)).append(" ")
    .appendF(getFloat(21)).append("};");
    sb.append("rotate @{quaternion({") // only the first two rows are needed
      .appendF(getFloat(0)).append(" ")
      .appendF(getFloat(1)).append(" ")
      .appendF(getFloat(2)).append("}{")
      .appendF(getFloat(4)).append(" ")
      .appendF(getFloat(5)).append(" ")
      .appendF(getFloat(6)).append("})};");
    sb.append("translate X ").appendF(getFloat(16)).append(" angstroms;");
    sb.append("translate Y ").appendF(-getFloat(17)).append(" angstroms;");


    
    
    addJmolScript(sb.toString());
    
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getList(List<Object> list, int i) {
    if (list == null || list.size() <= i)
      return null;
    Object o = list.get(i);
    return (o instanceof List<?> ? (List<Object>)o : null);
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
  
  private byte[] readLineBytes() throws Exception {
    String s = readString();
    return s.getBytes();
  }

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
