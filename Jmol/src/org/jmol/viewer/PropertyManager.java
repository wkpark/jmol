/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.viewer;

import javajs.J2SIgnoreImport;

import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.V3;
import javajs.util.XmlUtil;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolPropertyManager;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.JmolBioModel;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.viewer.binding.Binding;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of properties
 * with the getProperty() method, or its specifically cast forms
 * getPropertyString() or getPropertyJSON().
 * 
 * It is instantiated by reflection
 * 
 */


@J2SIgnoreImport({ javajs.util.XmlUtil.class })
public class PropertyManager implements JmolPropertyManager {

  public PropertyManager() {
    // required for reflection
  }

  Viewer vwr;
  private Map<String, Integer> map = new Hashtable<String, Integer>();

  @Override
  public void setViewer(Viewer vwr) {
    this.vwr = vwr;
    for (int i = 0, p = 0; i < propertyTypes.length; i += 3, p++)
      if (propertyTypes[i].length() > 0)
        map.put(propertyTypes[i].toLowerCase(), Integer.valueOf(p));
  }

  @Override
  public int getPropertyNumber(String infoType) {
    Integer n = map.get(infoType == null ? "" : infoType.toLowerCase());
    return (n == null ? -1 : n.intValue());
  }

  @Override
  public String getDefaultPropertyParam(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 2]);
  }

  @Override
  public boolean checkPropertyParameter(String name) {
    int propID = getPropertyNumber(name);
    String type = getParamType(propID);
    return (type.length() > 0 && type != atomExpression);
  }

  private final static String atomExpression = "<atom selection>";

  private final static String[] propertyTypes = {
    "appletInfo"      , "", "",
    "fileName"        , "", "",
    "fileHeader"      , "", "",
    "fileContents"    , "<pathname>", "",
    "fileContents"    , "", "",
    "animationInfo"   , "", "",
    "modelInfo"       , atomExpression, "{*}",
    //"X -vibrationInfo", "", "",  //not implemented -- see modelInfo
    "ligandInfo"      , atomExpression, "{*}",
    "shapeInfo"       , "", "",
    "measurementInfo" , "", "",
    
    "centerInfo"      , "", "",
    "orientationInfo" , "", "",
    "transformInfo"   , "", "",
    ""                , "", "", //was prior to 14.3.17 "atomList"        , atomExpression, "(visible)",
    "atomInfo"        , atomExpression, "(visible)",
    
    "bondInfo"        , atomExpression, "(visible)",
    "chainInfo"       , atomExpression, "(visible)",
    "polymerInfo"     , atomExpression, "(visible)",
    "moleculeInfo"    , atomExpression, "(visible)",
    "stateInfo"       , "<state type>", "all",
    
    "extractModel"    , atomExpression, "(visible)",
    "jmolStatus"      , "statusNameList", "",
    "jmolViewer"      , "", "",
    "messageQueue"    , "", "",
    "auxiliaryInfo"   , atomExpression, "{*}",
    
    "boundBoxInfo"    , "", "",  
    "dataInfo"        , "<data type>", "types",
    "image"           , "<width=www,height=hhh>", "",
    "evaluate"        , "<expression>", "",
    "menu"            , "<type>", "current",
    "minimizationInfo", "", "",
    "pointGroupInfo"  , atomExpression, "(visible)",
    "fileInfo"        , "<type>", "",
    "errorMessage"    , "", "",
    "mouseInfo"       , "", "",
    "isosurfaceInfo"  , "", "",
    "isosurfaceData"  , "", "",
    "consoleText"     , "", "",
    "JSpecView"       , "<key>", "",
    "scriptQueueInfo" , "", "",
    "nmrInfo" , "<elementSymbol> or 'all' or 'shifts'", "all",
    "variableInfo","<name>","all",
    "domainInfo"  , atomExpression, "{visible}",
    "validationInfo"  , atomExpression, "{visible}",
    "service"    , "<hashTable>", "",

  };

  private final static int PROP_APPLET_INFO = 0;
  private final static int PROP_FILENAME = 1;
  private final static int PROP_FILEHEADER = 2;
  private final static int PROP_FILECONTENTS_PATH = 3;
  private final static int PROP_FILECONTENTS = 4;

  private final static int PROP_ANIMATION_INFO = 5;
  private final static int PROP_MODEL_INFO = 6;
  //private final static int PROP_VIBRATION_INFO = 7; //not implemented -- see auxiliaryInfo
  private final static int PROP_LIGAND_INFO = 7;
  private final static int PROP_SHAPE_INFO = 8;
  private final static int PROP_MEASUREMENT_INFO = 9;

  private final static int PROP_CENTER_INFO = 10;
  private final static int PROP_ORIENTATION_INFO = 11;
  private final static int PROP_TRANSFORM_INFO = 12;
  private final static int PROP_ATOM_INFO = 13;

  private final static int PROP_BOND_INFO = 15;
  private final static int PROP_CHAIN_INFO = 16;
  private final static int PROP_POLYMER_INFO = 17;
  private final static int PROP_MOLECULE_INFO = 18;
  private final static int PROP_STATE_INFO = 19;

  private final static int PROP_EXTRACT_MODEL = 20;
  private final static int PROP_JMOL_STATUS = 21;
  private final static int PROP_JMOL_VIEWER = 22;
  private final static int PROP_MESSAGE_QUEUE = 23;
  private final static int PROP_AUXILIARY_INFO = 24;

  private final static int PROP_BOUNDBOX_INFO = 25;
  private final static int PROP_DATA_INFO = 26;
  private final static int PROP_IMAGE = 27;
  private final static int PROP_EVALUATE = 28;
  private final static int PROP_MENU = 29;
  private final static int PROP_MINIMIZATION_INFO = 30;
  private final static int PROP_POINTGROUP_INFO = 31;
  private final static int PROP_FILE_INFO = 32;
  private final static int PROP_ERROR_MESSAGE = 33;
  private final static int PROP_MOUSE_INFO = 34;
  private final static int PROP_ISOSURFACE_INFO = 35;
  private final static int PROP_ISOSURFACE_DATA = 36;
  private final static int PROP_CONSOLE_TEXT = 37;
  private final static int PROP_JSPECVIEW = 38;
  private final static int PROP_SCRIPT_QUEUE_INFO = 39;
  private final static int PROP_NMR_INFO = 40;
  private final static int PROP_VAR_INFO = 41;
  private final static int PROP_DOM_INFO = 42;
  private final static int PROP_VAL_INFO = 43;
  private final static int PROP_SERVICE = 44;
  private final static int PROP_COUNT = 45;

  //// static methods used by Eval and Viewer ////

  @Override
  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    if (propertyTypes.length != PROP_COUNT * 3)
      Logger.warn("propertyTypes is not the right length: "
          + propertyTypes.length + " != " + PROP_COUNT * 3);
    Object info;
    if (infoType.indexOf(".") >= 0 || infoType.indexOf("[") >= 0) {
      SV[] args = getArguments(infoType);
      info = extractProperty(
          getPropertyAsObject(args[0].asString(), paramInfo, null), args, 1,
          null, false);
    } else {
      info = getPropertyAsObject(infoType, paramInfo, returnType);
    }
    if (returnType == null)
      return info;
    boolean requestedReadable = returnType.equalsIgnoreCase("readable");
    if (requestedReadable)
      returnType = (isReadableAsString(infoType) ? "String" : "JSON");
    if (returnType.equalsIgnoreCase("String"))
      return (info == null ? "" : info.toString());
    if (requestedReadable)
      return Escape.toReadable(infoType, info);
    else if (returnType.equalsIgnoreCase("JSON"))
      return "{" + PT.toJSON(infoType, info) + "}";
    return info;
  }

  private SV[] getArguments(String propertyName) {
    String lc = propertyName.toLowerCase();
    int pt = -1;
    if (propertyName.indexOf('"') >= 0 || propertyName.indexOf('\'') >= 0)
      propertyName = fixSelectQuotes(propertyName);
    while ((pt = lc.indexOf("[select ", ++pt)) >= 0) {
      int pt2 = lc.indexOf(" where ", pt);
      int pt3 = lc.lastIndexOf("]");
      if (pt2 < 0 || pt2 > pt3)
        continue;
      propertyName = propertyName.substring(0, pt + 1) 
          + propertyName.substring(pt + 1, pt3).replace('.', '\1').replace('[', '\2').replace(']', '\3')
          + propertyName.substring(pt3);
    }
    propertyName = PT.rep(PT.rep(propertyName.replace(']', '\0').replace('[', '\0'), "..", "\4")
        .replace('.', '\0').replace('\1', '.').replace('\2', '[').replace('\3', ']'),"\4","..");
    propertyName = PT.rep(propertyName, "\0\0", "\0");
    String[] names = PT.split(PT.trim(propertyName, "\0"), "\0");
    SV[] args = new SV[names.length];
    for (int i = 0, n; i < names.length; i++)
      args[i] = (names[i].startsWith("'") || names[i].startsWith("\"") 
          ? SV.newS(PT.trim(names[i], "'\""))
              : (n = PT.parseInt(names[i])) == Integer.MIN_VALUE ? 
                  SV.newS(names[i]) : SV.newI(n));
    return args;
  }

  private String fixSelectQuotes(String propertyName) {
    char[] a = propertyName.toCharArray();
    boolean inQuotes = false;
    boolean inQuotes1 = false;
    boolean inQuotes2 = false;
    for (int i = a.length; --i >= 0;) {
      switch (a[i]) {
      case '\'':
        if (!inQuotes2)
          inQuotes = inQuotes1 = !inQuotes;
        break;
      case '"':
        if (!inQuotes1)
          inQuotes = inQuotes2 = !inQuotes;
        break;
      case '.':
        if (inQuotes)
          a[i] = '\1';
        break;
      case '[':
        if (inQuotes)
          a[i] = '\2';
        break;
      case ']':
        if (inQuotes)
          a[i] = '\3';
        break;
        
      }
    }
    propertyName = new String(a);
    return propertyName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object extractProperty(Object prop, Object args, int ptr,
                                Lst<Object> v2, boolean isCompiled) {
    if (ptr < 0) {
      args = getArguments((String) args);
      ptr = 0;
    }
    if (ptr >= ((SV[]) args).length)
      return prop;
    if (!isCompiled)
      args = compileSelect((SV[]) args);
    int pt;
    SV arg = ((SV[]) args)[ptr++];
    Object property = SV.oValue(prop);
    switch (arg.tok) {
    case T.integer:
      pt = arg.intValue - 1; //one-based, as for array selectors
      if (property instanceof Lst<?>) {
        Lst<Object> v = (Lst<Object>) property;
        if (pt < 0)
          pt += v.size();
        return (pt >= 0 && pt < v.size() ? extractProperty(v.get(pt), args,
            ptr, null, true) : "");
      }
      if (property instanceof M3) {
        M3 m = (M3) property;
        float[][] f = new float[][] { new float[] { m.m00, m.m01, m.m02 },
            new float[] { m.m10, m.m11, m.m12 },
            new float[] { m.m20, m.m21, m.m22 } };
        if (pt < 0)
          pt += 3;
        if (pt >= 0 && pt < 3)
          return extractProperty(f, args, --ptr, null, true);
        return "";
      }
      if (property instanceof M4) {
        M4 m = (M4) property;
        float[][] f = new float[][] {
            new float[] { m.m00, m.m01, m.m02, m.m03 },
            new float[] { m.m10, m.m11, m.m12, m.m13 },
            new float[] { m.m20, m.m21, m.m22, m.m23 },
            new float[] { m.m30, m.m31, m.m32, m.m33 } };
        if (pt < 0)
          pt += 4;
        if (pt >= 0 && pt < 4)
          return extractProperty(f, args, --ptr, null, true);
        return "";
      }
      if (AU.isAI(property)) {
        int[] ilist = (int[]) property;
        if (pt < 0)
          pt += ilist.length;
        if (pt >= 0 && pt < ilist.length)
          return Integer.valueOf(ilist[pt]);
        return "";
      }
      if (AU.isAD(property)) {
        double[] dlist = (double[]) property;
        if (pt < 0)
          pt += dlist.length;
        if (pt >= 0 && pt < dlist.length)
          return Double.valueOf(dlist[pt]);
        return "";
      }
      if (AU.isAF(property)) {
        float[] flist = (float[]) property;
        if (pt < 0)
          pt += flist.length;
        if (pt >= 0 && pt < flist.length)
          return Float.valueOf(flist[pt]);
        return "";
      }
      if (AU.isAII(property)) {
        int[][] iilist = (int[][]) property;
        if (pt < 0)
          pt += iilist.length;
        if (pt >= 0 && pt < iilist.length)
          return extractProperty(iilist[pt], args, ptr, null, true);
        return "";
      }
      if (AU.isAFF(property)) {
        float[][] fflist = (float[][]) property;
        if (pt < 0)
          pt += fflist.length;
        if (pt >= 0 && pt < fflist.length)
          return extractProperty(fflist[pt], args, ptr, null, true);
        return "";
      }
      if (AU.isAS(property)) {
        String[] slist = (String[]) property;
        if (pt < 0)
          pt += slist.length;
        if (pt >= 0 && pt < slist.length)
          return slist[pt];
        return "";
      }
      if (property instanceof Object[]) {
        Object[] olist = (Object[]) property;
        if (pt < 0)
          pt += olist.length;
        if (pt >= 0 && pt < olist.length)
          return olist[pt];
        return "";
      }
      break;
    case T.select:
    case T.string:
      if (property instanceof Map<?, ?>) {
        Map<String, ?> h = (Map<String, ?>) property;
        String key;
        boolean asMap = false;
        boolean asArray = false;
        if (arg.tok == T.select) {
          key = arg.myName;
          if (key.contains("**")) {
            asArray = key.endsWith(";");
            if (asArray)
              key = key.substring(0, key.length() - 1);
            boolean isAll = key.equals("**");
            key = PT.rep(key, "**", "*");
            String newKey = "";
            for (Entry<?, ?> e : h.entrySet()) {
              String k = (String) e.getKey();
              if (!isAll && !PT.isLike(k, key))
                continue;
              Object o = e.getValue();
              if ((o instanceof SV)) {
                o = ((SV) o).getMap();
              } else if (!(o instanceof Map<?, ?>)) {
                o = null;
              }
              if (o != null
                  && vwr.checkSelect((Map<String, SV>) o, (T[]) arg.value))
                newKey += "," + k;
            }
            if (newKey.length() == 0)
              return "";
            key = newKey.substring(1);
            asMap = !asArray;
          } else if (!vwr.checkSelect((Map<String, SV>) property,
              (T[]) arg.value))
            return "";
        } else {
          key = arg.asString();
          if (key.equalsIgnoreCase("keys")) {
            Lst<Object> keys = new Lst<Object>();
            for (String k : h.keySet())
              keys.addLast(k);
            return extractProperty(keys, args, ptr, null, true);
          }
        }
        boolean isWild = (asArray || key.startsWith("*") || key.endsWith("*") 
            || key.indexOf(",") >= 0 || key.indexOf(";") >= 0);
        if (isWild) {
          if (v2 == null)
            v2 = new Lst<Object>();
          if (!asArray && key.length() == 1) {
            if (ptr == ((SV[]) args).length) {
              v2.addLast(property);
              return v2;
            }
            return extractProperty(property, args, ptr, v2, true);
          }
        }
        if (key.contains("**"))
          key = PT.rep(key, "**", "*") + ",";
        if (asMap || asArray || key.contains(",")) {
          Map<String, Object> mapNew = new Hashtable<String, Object>();
          String[] tokens = PT.split(key, ",");
          for (int i = tokens.length; --i >= 0;)
            getMapSubset(h, tokens[i], mapNew);
          if (asMap)
            return mapNew;
          if (ptr == ((SV[]) args).length) {
            v2.addLast(mapNew);
            return v2;
          }
          return extractProperty(mapNew, args, ptr, v2, true);
        }
        key = checkMap(h, key, isWild, v2, args, ptr);
        return (key != null && !isWild ? extractProperty(h.get(key), args, ptr,
            null, true) : isWild ? v2 : "");
      }
      if (property instanceof Lst<?>) {
        // drill down into vectors for this key
        Lst<Object> v = (Lst<Object>) property;
        if (v2 == null)
          v2 = new Lst<Object>();
        ptr--;
        for (pt = 0; pt < v.size(); pt++) {
          Object o = v.get(pt);
          if (o instanceof Map<?, ?> || o instanceof Lst<?>
              || (o instanceof SV)
              && (((SV) o).getMap() != null || ((SV) o).getList() != null))
            extractProperty(o, args, ptr, v2, true);
        }
        return v2;
      }
      break;
    }
    return prop;
  }

  private static void getMapSubset(Map<String, ?> h, String key,
                                   Map<String, Object> h2) {
    if (key.startsWith("\"") || key.startsWith("'"))
      key = PT.trim(key,  "\"'");
    Object val = h.get(key);
    if (val != null) {
      h2.put(key, val);
      return;
    }
    for (Entry<String, ?> e : h.entrySet()) {
      String k = e.getKey();
      if (PT.isLike(k, key))
        h2.put(k, e.getValue());
    }
  }

  private Object compileSelect(SV[] args) {
    SV[] argsNew = null;
    for (int i = args.length; --i >= 0;) {
      if (args[i].tok == T.string) {
        String key = (String) args[i].value;
        // SELECT nt* WHERE name!=WC
        // SELECT a,b,c WHERE x.in(...)
        
        if (key.toUpperCase().startsWith("SELECT ")) {
          if (argsNew == null)
            argsNew = (SV[]) AU.arrayCopyObject(args, args.length);
          key = key.substring(6).trim();
          if (key.toUpperCase().startsWith("WHERE "))
            key = "* " + key;
          int pt = key.toUpperCase().indexOf(" WHERE ");
          if (pt < 0) {
            argsNew[i].value = key;
          } else {
            argsNew[i] = SV.newV(T.select, vwr.compileExpr(key.substring(pt + 6)
                .trim()));
            argsNew[i].myName = key.substring(0, pt).trim();
          }
        }
      }
    }
    return (argsNew == null ? args : argsNew);
  }

  private String checkMap(Map<String, ?> h, String key, boolean isWild,
                          Lst<Object> v2, Object args, int ptr) {
    boolean isOK = (v2 == null && h.containsKey(key));
    if (!isOK) {
      boolean hasSemi = key.contains(";");
      if (hasSemi)
        key = ";" + key + ";";
      String lckey = (isWild ? key.toLowerCase() : null);
      for (String k : h.keySet()) {
        if (hasSemi ? key.contains(";" + k + ";") 
            : k.equalsIgnoreCase(key) || lckey != null
            && PT.isLike(k.toLowerCase(), lckey)) {
          if (v2 == null)
            return k;
          v2.addLast(extractProperty(h.get(k), args, ptr, null, true));
          if (!isWild && !hasSemi)
            return null;
        }
      }
    }
    return (isOK ? key : null);
  }

  private static String getPropertyName(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3]);
  }

  private static String getParamType(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 1]);
  }

  private final static String[] readableTypes = { "", "stateinfo",
      "extractmodel", "filecontents", "fileheader", "image", "menu",
      "minimizationInfo" };

  private static boolean isReadableAsString(String infoType) {
    for (int i = readableTypes.length; --i >= 0;)
      if (infoType.equalsIgnoreCase(readableTypes[i]))
        return true;
    return false;
  }

  private Object getPropertyAsObject(String infoType, Object paramInfo,
                                     String returnType) {
    //Logger.debug("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    if (infoType.equals("tokenList")) {
      return T.getTokensLike((String) paramInfo);
    }
    int id = getPropertyNumber(infoType);
    boolean iHaveParameter = (paramInfo != null && paramInfo.toString()
        .length() > 0);
    Object myParam = (iHaveParameter ? paramInfo : getDefaultPropertyParam(id));
    //myParam may now be a bitset
    switch (id) {
    case PROP_APPLET_INFO:
      return getAppletInfo();
    case PROP_ANIMATION_INFO:
      return getAnimationInfo();
    case PROP_ATOM_INFO:
      return getAllAtomInfo(vwr.getAtomBitSet(myParam));
    case PROP_AUXILIARY_INFO:
      return vwr.getAuxiliaryInfoForAtoms(myParam);
    case PROP_BOND_INFO:
      return getAllBondInfo(myParam);
    case PROP_BOUNDBOX_INFO:
      return getBoundBoxInfo();
    case PROP_CENTER_INFO:
      return vwr.tm.fixedRotationCenter;
    case PROP_CHAIN_INFO:
      return getAllChainInfo(vwr.getAtomBitSet(myParam));
    case PROP_CONSOLE_TEXT:
      return vwr.getProperty("DATA_API", "consoleText", null);
    case PROP_DATA_INFO:
      return vwr.getDataObj(myParam.toString(), null, JmolDataManager.DATA_TYPE_UNKNOWN);
    case PROP_ERROR_MESSAGE:
      return vwr.getErrorMessageUn();
    case PROP_EVALUATE:
      return vwr.evaluateExpression(myParam.toString());
    case PROP_EXTRACT_MODEL:
      return vwr.getModelExtract(myParam, true, false, "MOL");
    case PROP_FILE_INFO:
      return getFileInfo(vwr.getFileData(), myParam.toString());
    case PROP_FILENAME:
      return vwr.fm.getFullPathName(false);
    case PROP_FILEHEADER:
      return vwr.getFileHeader();
    case PROP_FILECONTENTS:
    case PROP_FILECONTENTS_PATH:
      return (iHaveParameter ? vwr.getFileAsString3(myParam.toString(), true, null)
          : vwr.getCurrentFileAsString("prop"));
    case PROP_IMAGE:
      String params = myParam.toString().toLowerCase();
      return getImage(params,
          params.indexOf("g64") < 0 && params.indexOf("base64") < 0
              && (returnType == null || returnType.equalsIgnoreCase("java")));
    case PROP_ISOSURFACE_INFO:
      return vwr.getShapeProperty(JC.SHAPE_ISOSURFACE, "getInfo");
    case PROP_ISOSURFACE_DATA:
      return vwr.getShapeProperty(JC.SHAPE_ISOSURFACE, "getData");
    case PROP_NMR_INFO:
      return vwr.getNMRCalculation().getInfo(myParam.toString());
    case PROP_VAR_INFO:
      return getVariables(myParam.toString());
    case PROP_JMOL_STATUS:
      return vwr.getStatusChanged(myParam.toString());
    case PROP_JMOL_VIEWER:
      return vwr;
    case PROP_JSPECVIEW:
      return vwr.getJspecViewProperties(myParam);
    case PROP_LIGAND_INFO:
      return getLigandInfo(vwr.getAtomBitSet(myParam));
    case PROP_MEASUREMENT_INFO:
      return getMeasurementInfo();
    case PROP_MENU:
      return vwr.getMenu(myParam.toString());
    case PROP_MESSAGE_QUEUE:
      return vwr.sm.messageQueue;
    case PROP_MINIMIZATION_INFO:
      return vwr.getMinimizationInfo();
    case PROP_MODEL_INFO:
      return getModelInfo(vwr.getAtomBitSet(myParam));
    case PROP_MOLECULE_INFO:
      return getMoleculeInfo(vwr.getAtomBitSet(myParam));
    case PROP_MOUSE_INFO:
      return getMouseInfo();
    case PROP_ORIENTATION_INFO:
      return vwr.tm.getOrientationInfo();
    case PROP_POINTGROUP_INFO:
      return vwr.ms.getPointGroupInfo(vwr.getAtomBitSet(myParam));
    case PROP_POLYMER_INFO:
      return getAllPolymerInfo(vwr.getAtomBitSet(myParam));
    case PROP_SCRIPT_QUEUE_INFO:
      return vwr.getScriptQueueInfo();
    case PROP_SHAPE_INFO:
      return getShapeInfo();
    case PROP_STATE_INFO:
      return vwr.getStateInfo3(myParam.toString(), 0, 0);
    case PROP_TRANSFORM_INFO:
      return M3.newM3(vwr.tm.matrixRotate);
    case PROP_DOM_INFO:
      return getAnnotationInfo(myParam, T.domains);
    case PROP_VAL_INFO:
      return getAnnotationInfo(myParam, T.validation);
    case PROP_SERVICE:
      myParam = SV.oValue(myParam);
      @SuppressWarnings("unchecked")
      Map<String, Object> info = (myParam instanceof Map<?,?> ? (Map<String, Object>) myParam : null);
      return (info == null ? null : vwr.sm.processService(info));
    }
    String[] data = new String[PROP_COUNT];
    for (int i = 0; i < PROP_COUNT; i++) {
      String paramType = getParamType(i);
      String paramDefault = getDefaultPropertyParam(i);
      String name = getPropertyName(i);
      data[i] = (name.charAt(0) == 'X' ? "" : name
          + (paramType != "" ? " "
              + getParamType(i)
              + (paramDefault != "" ? " #default: "
                  + getDefaultPropertyParam(i) : "") : ""));
    }
    Arrays.sort(data);
    SB info = new SB();
    info.append("getProperty ERROR\n").append(infoType)
        .append("?\nOptions include:\n");
    for (int i = 0; i < PROP_COUNT; i++)
      if (data[i].length() > 0)
        info.append("\n getProperty ").append(data[i]);
    return info.toString();
  }

  private Object getImage(String params, boolean asBytes) {
    int height = -1,
    width = -1;
    int pt;
    if ((pt = params.indexOf("height=")) >= 0)
      height = PT.parseInt(params.substring(pt + 7));
    if ((pt = params.indexOf("width=")) >= 0)
      width = PT.parseInt(params.substring(pt + 6));
    if (width < 0 && height < 0)
      height = width = -1;
    else if (width < 0)
      width = height;
    else
      height = width;
    String type = "JPG";
    if (params.indexOf("type=") >= 0)
      type = PT.getTokens(PT.replaceWithCharacter(params.substring(params.indexOf("type=") + 5), ";,", ' '))[0];
    String[] errMsg = new String[1];
    byte[] bytes = vwr.getImageAsBytes(type.toUpperCase(), width,  height, -1, errMsg);
    return (errMsg[0] != null ? errMsg[0] : asBytes ? new BArray(bytes) : Base64
        .getBase64(bytes).toString());
  }

  private Object getVariables(String name) {
    return (name.toLowerCase().equals("all") ? vwr.g.getAllVariables()
        : vwr.evaluateExpressionAsVariable(name));
  }

  static Object getFileInfo(Object objHeader, String type) {
    Map<String, String> ht = new Hashtable<String, String>();
    if (objHeader == null)
      return ht;
    boolean haveType = (type != null && type.length() > 0);
    if (objHeader instanceof Map) {
      return (haveType ? ((Map<?, ?>) objHeader).get(type) : objHeader);
    }
    String[] lines = PT.split((String) objHeader, "\n");
    // this is meant to be for PDB files only
    if (lines.length == 0
        || lines[0].length() < 6
        || lines[0].charAt(6) != ' '
        || !lines[0].substring(0, 6).equals(
            lines[0].substring(0, 6).toUpperCase())) {
      ht.put("fileHeader", (String) objHeader);
      return ht;
    }
    String keyLast = "";
    SB sb = new SB();
    if (haveType)
      type = type.toUpperCase();
    String key = "";
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.length() < 12)
        continue;
      key = line.substring(0, 6).trim();
      String cont = line.substring(7, 10).trim();
      if (key.equals("REMARK")) {
        key += cont;
      }
      if (!key.equals(keyLast)) {
        if (haveType && keyLast.equals(type))
          return sb.toString();
        if (!haveType) {
          ht.put(keyLast, sb.toString());
          sb = new SB();
        }
        keyLast = key;
      }
      if (!haveType || key.equals(type))
        sb.append(line).appendC('\n');
    }
    if (!haveType) {
      ht.put(keyLast, sb.toString());
    }
    if (haveType)
      return (key.equals(type) ? sb.toString() : "");
    return ht;
  }

  /// info ///

  public Lst<Map<String, Object>> getMoleculeInfo(Object atomExpression) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    JmolMolecule[] molecules = vwr.ms.getMolecules();
    Lst<Map<String, Object>> V = new  Lst<Map<String, Object>>();
    BS bsTemp = new BS();
    for (int i = 0; i < molecules.length; i++) {
      bsTemp = BSUtil.copy(bsAtoms);
      JmolMolecule m = molecules[i];
      bsTemp.and(m.atomList);
      if (bsTemp.length() > 0) {
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("mf", m.getMolecularFormula(false, null, false)); // sets ac and nElements
        info.put("number", Integer.valueOf(m.moleculeIndex + 1)); //for now
        info.put("modelNumber", vwr.ms.getModelNumberDotted(m.modelIndex));
        info.put("numberInModel", Integer.valueOf(m.indexInModel + 1));
        info.put("nAtoms", Integer.valueOf(m.ac));
        info.put("nElements", Integer.valueOf(m.nElements));
        V.addLast(info);
      }
    }
    return V;
  }

  @Override
  public Map<String, Object> getModelInfo(Object atomExpression) {

    BS bsModels = vwr.ms.getModelBS(vwr
        .getAtomBitSet(atomExpression), false);

    ModelSet m = vwr.ms;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("modelSetName", m.modelSetName);
    info.put("modelIndex", Integer.valueOf(vwr.am.cmi));
    info.put("modelCount", Integer.valueOf(m.mc));
    info.put("isTainted", Boolean.valueOf(m.tainted != null));
    info.put("canSkipLoad", Boolean.valueOf(m.canSkipLoad));
    info.put("modelSetHasVibrationVectors", Boolean.valueOf(m
        .modelSetHasVibrationVectors()));
    if (m.modelSetProperties != null) {
      info.put("modelSetProperties", m.modelSetProperties);
    }
    info.put("modelCountSelected", Integer.valueOf(BSUtil
        .cardinalityOf(bsModels)));
    info.put("modelsSelected", bsModels);
    Lst<Map<String, Object>> vModels = new  Lst<Map<String, Object>>();
    m.getMolecules();

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      Map<String, Object> model = new Hashtable<String, Object>();
      model.put("_ipt", Integer.valueOf(i));
      model.put("num", Integer.valueOf(m.getModelNumber(i)));
      model.put("file_model", m.getModelNumberDotted(i));
      model.put("name", m.getModelName(i));
      String s = m.getModelTitle(i);
      if (s != null)
        model.put("title", s);
      s = m.getModelFileName(i);
      if (s != null)
        model.put("file", s);
      s = (String) m.getInfo(i, "modelID");
      if (s != null)
        model.put("id", s);
      model.put("vibrationVectors", Boolean.valueOf(vwr.modelHasVibrationVectors(i)));
      Model mi = m.am[i];
      model.put("atomCount", Integer.valueOf(mi.act));
      model.put("bondCount", Integer.valueOf(mi.getBondCount()));
      model.put("groupCount", Integer.valueOf(mi.getGroupCount()));
      model.put("moleculeCount", Integer.valueOf(mi.moleculeCount));
      if (mi.isBioModel)
        model.put("polymerCount", Integer.valueOf(((JmolBioModel) mi).getBioPolymerCount()));
      model.put("chainCount", Integer.valueOf(m.getChainCountInModelWater(i, true)));
      if (mi.properties != null) {
        model.put("modelProperties", mi.properties);
      }
      Float energy = (Float) m.getInfo(i, "Energy");
      if (energy != null) {
        model.put("energy", energy);
      }
      model.put("atomCount", Integer.valueOf(mi.act));
      vModels.addLast(model);
    }
    info.put("models", vModels);
    return info;
  }

  @Override
  public Map<String, Object> getLigandInfo(Object atomExpression) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    BS bsSolvent = vwr.getAtomBitSet("solvent");
    Map<String, Object> info = new Hashtable<String, Object>();
    Lst<Map<String, Object>> ligands = new  Lst<Map<String, Object>>();
    info.put("ligands", ligands);
    ModelSet ms = vwr.ms;
    BS bsExclude = BSUtil.copyInvert(bsAtoms, ms.ac);
    bsExclude.or(bsSolvent);
    Atom[] atoms = ms.at;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      if (atoms[i].group.isProtein() || atoms[i].group.isDna() || atoms[i].group.isRna())
        atoms[i].group.setAtomBitsAndClear(bsExclude, bsAtoms);
    BS[] bsModelAtoms = new BS[ms.mc];
    for (int i = ms.mc; --i >= 0;) {
      bsModelAtoms[i] = vwr.getModelUndeletedAtomsBitSet(i);
      bsModelAtoms[i].andNot(bsExclude);
    }
    JmolMolecule[] molList = JmolMolecule.getMolecules(atoms, bsModelAtoms,
        null, bsExclude);
    for (int i = 0; i < molList.length; i++) {
      BS bs = molList[i].atomList;
      Map<String, Object> ligand = new Hashtable<String, Object>();
      ligands.addLast(ligand);
      ligand.put("atoms", Escape.eBS(bs));
      String names = "";
      String sep = "";
      Group lastGroup = null;
      int iChainLast = 0;
      String sChainLast = null;
      String reslist = "";
      String model = "";
      int resnolast = Integer.MAX_VALUE;
      int resnofirst = Integer.MAX_VALUE;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom atom = atoms[j];
        if (lastGroup == atom.group)
          continue;
        lastGroup = atom.group;
        int resno = atom.getResno();
        int chain = atom.getChainID();
        if (resnolast != resno - 1) {
          if (reslist.length() != 0 && resnolast != resnofirst)
            reslist += "-" + resnolast;
          chain = -1;
          resnofirst = resno;
        }
        model = "/" + ms.getModelNumberDotted(atom.mi);
        if (iChainLast != 0 && chain != iChainLast)
          reslist += ":" + sChainLast + model;
        if (chain == -1)
          reslist += " " + resno;
        resnolast = resno;
        iChainLast = atom.getChainID();
        sChainLast = atom.getChainIDStr();
        names += sep + atom.getGroup3(false);
        sep = "-";
      }
      reslist += (resnofirst == resnolast ? "" : "-" + resnolast)
          + (iChainLast == 0 ? "" : ":" + sChainLast) + model;
      ligand.put("groupNames", names);
      ligand.put("residueList", reslist.substring(1));
    }
    return info;
  }

  /**
   * use lower case to indicate coord data only (xyz, xyzrn, xyzvib, pdb. use
   * USER: or PROPERTY_xxxx for a property
   * 
   * all other types return full data
   */
  @Override
  public String getAtomData(String atomExpression, String type, boolean allTrajectories) {
    // lower case is only used in Jmol script function print data({*}, "datatype")
    if (!atomExpression.startsWith("{"))
      atomExpression = "{" + atomExpression + "}";
    boolean isUser = type.toLowerCase().startsWith("user:");
    boolean isProp = type.toLowerCase().startsWith("property_");
    if (allTrajectories && !isUser && !isProp)
      type = type.toUpperCase();
    String exp = (isProp ? "%{" + type + "}"
        : isUser ? type.substring(5)
            : type.equals("xyzrn") ? "%-2e %8.3x %8.3y %8.3z %4.2[vdw] 1 [%n]%r.%a#%i"
                : type.equals("xyzvib") ? "%-2e %10.5x %10.5y %10.5z %10.5vx %10.5vy %10.5vz"
                    : type.equals("pdb") ? "{selected and not hetero}.label(\"ATOM  %5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
                        + "+{selected and hetero}.label(\"HETATM%5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
                        : type.equals("xyz") ? "%-2e %10.5x %10.5y %10.5z"
                            : type.equals("cfi") ? "print 'from Jmol" + Viewer.getJmolVersion() + "\n'+{selected}.count + ' ' + {selected}.bonds.count + '\n'   + {selected}.format('%10.0[atomno] %10.0[elemno] %10.4[xyz]')  + {selected}.bonds.format('%i1 %i2') + '\n' + {selected}.bonds.format('%ORDER')"
                            : null);
    if (exp == null)
      return getModelExtract(vwr.getAtomBitSet(atomExpression), false, false,
          type.toUpperCase(), allTrajectories);
    if (exp.startsWith("print"))
      return vwr.runScript(exp);
    if (exp.indexOf("label") < 0)
      exp = atomExpression + ".label(\"" + exp + "\").lines";
    else if (!atomExpression.equals("selected"))
      exp = PT.rep(exp, "selected", atomExpression.substring(1, atomExpression.length() - 1));
    return (String) vwr.evaluateExpression(exp);
  }


  /**
   * 
   * V3000, SDF, JSON, CD, XYZ, XYZVIB, XYZRN, CML, PDB, PQR
   * 
   */
  @Override
  public String getModelExtract(BS bs, boolean doTransform, boolean isModelKit,
                                String type, boolean allTrajectories) {
    
    if (type.equalsIgnoreCase("CML"))
      return getModelCml(bs, Integer.MAX_VALUE, true, doTransform, allTrajectories);
    
    if (type.equals("PDB") || type.equals("PQR"))
      return getPdbAtomData(bs, null, type.equals("PQR"), doTransform, allTrajectories);
    
    boolean asV3000 = type.equalsIgnoreCase("V3000");
    boolean asSDF = type.equalsIgnoreCase("SDF");
    boolean asXYZVIB = (!doTransform && type.equalsIgnoreCase("XYZVIB"));
    boolean asXYZRN = type.equalsIgnoreCase("XYZRN");
    boolean isXYZ = type.toUpperCase().startsWith("XYZ");
    boolean asJSON = type.equalsIgnoreCase("JSON") || type.equalsIgnoreCase("CD");
    SB mol = new SB();
    ModelSet ms = vwr.ms;
    if (!isXYZ && !asJSON) {
      mol.append(isModelKit ? "Jmol Model Kit" : vwr.fm.getFullPathName(false)
          .replace('\\', '/'));
      String version = Viewer.getJmolVersion();
      mol.append("\n__Jmol-").append(version.substring(0, 2));
      int cMM, cDD, cYYYY, cHH, cmm;
      /**
       * @j2sNative
       * 
       *            var c = new Date(); cMM = c.getMonth(); cDD = c.getDate();
       *            cYYYY = c.getFullYear(); cHH = c.getHours(); cmm =
       *            c.getMinutes();
       */
      {
        Calendar c = Calendar.getInstance();
        cMM = c.get(Calendar.MONTH);
        cDD = c.get(Calendar.DAY_OF_MONTH);
        cYYYY = c.get(Calendar.YEAR);
        cHH = c.get(Calendar.HOUR_OF_DAY);
        cmm = c.get(Calendar.MINUTE);
      }
      PT.rightJustify(mol, "_00", "" + (1 + cMM));
      PT.rightJustify(mol, "00", "" + cDD);
      mol.append(("" + cYYYY).substring(2, 4));
      PT.rightJustify(mol, "00", "" + cHH);
      PT.rightJustify(mol, "00", "" + cmm);
      mol.append("3D 1   1.00000     0.00000     0");
      //       This line has the format:
      //  IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
      //  A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6->
      mol.append("\nJmol version ").append(Viewer.getJmolVersion())
          .append(" EXTRACT: ").append(Escape.eBS(bs)).append("\n");
    }
    BS bsAtoms = BSUtil.copy(bs);
    Atom[] atoms = ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (doTransform && atoms[i].isDeleted())
        bsAtoms.clear(i);
    BS bsBonds = getCovalentBondsForAtoms(ms.bo, ms.bondCount, bsAtoms);
    if (!asXYZVIB && bsAtoms.cardinality() == 0)
      return "";
    boolean isOK = true;
    BS bsModels = vwr.ms.getModelBS(bsAtoms, true);
    if (ms.trajectory != null && !allTrajectories)
      ms.trajectory.selectDisplayed(bsModels);
    Quat q = (doTransform ? vwr.tm.getRotationQ() : null);
    if (asSDF) {
      String header = mol.toString();
      mol = new SB();
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        mol.append(header);
        BS bsTemp = BSUtil.copy(bsAtoms);
        bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
        bsBonds = getCovalentBondsForAtoms(ms.bo, ms.bondCount, bsTemp);
        if (!(isOK = addMolFile(i, mol, bsTemp, bsBonds, false, false, q)))
          break;
        mol.append("$$$$\n");
      }
    } else if (isXYZ) {
      LabelToken[] tokensXYZ = LabelToken.compile(vwr,
          (asXYZRN ? "%-2e _XYZ_ %4.2[vdw] 1 [%n]%r.%a#%i\n" : "%-2e _XYZ_\n"),
          '\0', null);
      LabelToken[] tokensVib = (asXYZVIB ? LabelToken.compile(vwr,
          "%-2e _XYZ_ %12.5vx %12.5vy %12.5vz\n", '\0', null) : null);
      P3 ptTemp = new P3();
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        BS bsTemp = BSUtil.copy(bsAtoms);
        bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
        if (bsTemp.cardinality() == 0)
          continue;
        mol.appendI(bsTemp.cardinality()).appendC('\n');
        Properties props = ms.am[i].properties;
        mol.append("Model[" + (i + 1) + "]: ");
        if (ms.frameTitles[i] != null && ms.frameTitles[i].length() > 0) {
          mol.append(ms.frameTitles[i].replace('\n', ' '));
        } else if (props == null) {
          mol.append("Jmol " + Viewer.getJmolVersion());
        } else {
          SB sb = new SB();
          Enumeration<?> e = props.propertyNames();
          String path = null;
          while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            if (propertyName.equals(".PATH"))
              path = props.getProperty(propertyName);
            else
              sb.append(";").append(propertyName).append("=")
                  .append(props.getProperty(propertyName));
          }
          if (path != null)
            sb.append(";PATH=").append(path);
          path = sb.substring(sb.length() > 0 ? 1 : 0);
          mol.append(path.replace('\n', ' '));
        }
        mol.appendC('\n');
        Object[] o = new Object[] { ptTemp };
        for (int j = bsTemp.nextSetBit(0); j >= 0; j = bsTemp.nextSetBit(j + 1)) {
          String s = LabelToken.formatLabelAtomArray(vwr, atoms[j], (asXYZVIB
              && ms.getVibration(j, false) != null ? tokensVib : tokensXYZ),
              '\0', null, ptTemp);
          getPointTransf(i, ms, atoms[j], q, ptTemp);
          s = PT.rep(s, "_XYZ_", PT.sprintf("%12.5p %12.5p %12.5p", "p", o));
          mol.append(s);
        }
      }
    } else {
      isOK = addMolFile(-1, mol, bsAtoms, bsBonds, asV3000, asJSON, q);
    }
    return (isOK ? mol.toString()
        : "ERROR: Too many atoms or bonds -- use V3000 format.");
  }

  private boolean addMolFile(int iModel, SB mol, BS bsAtoms, BS bsBonds,
                             boolean asV3000, boolean asJSON, Quat q) {
    int nAtoms = bsAtoms.cardinality();
    int nBonds = bsBonds.cardinality();
    if (!asV3000 && !asJSON && (nAtoms > 999 || nBonds > 999))
      return false;
    ModelSet ms = vwr.ms;
    int[] atomMap = new int[ms.ac];
    P3 pTemp = new P3();
    if (asV3000) {
      mol.append("  0  0  0  0  0  0            999 V3000");
    } else if (asJSON) {
       mol.append("{\"mol\":{\"createdBy\":\"Jmol "+ Viewer.getJmolVersion() + "\",\"a\":[");
    } else {
      PT.rightJustify(mol, "   ", "" + nAtoms);
      PT.rightJustify(mol, "   ", "" + nBonds);
      mol.append("  0  0  0  0              1 V2000");
    }
    if (!asJSON)
      mol.append("\n");
    if (asV3000) {
      mol.append("M  V30 BEGIN CTAB\nM  V30 COUNTS ").appendI(nAtoms)
          .append(" ").appendI(nBonds).append(" 0 0 0\n").append(
              "M  V30 BEGIN ATOM\n");
    }
    for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
        .nextSetBit(i + 1))
      getAtomRecordMOL(iModel, ms, mol, atomMap[i] = ++n, ms.at[i], q, pTemp, asV3000,
          asJSON);
    if (asV3000) {
      mol.append("M  V30 END ATOM\nM  V30 BEGIN BOND\n");
    } else if (asJSON) {
      mol.append("],\"b\":[");
    }
    for (int i = bsBonds.nextSetBit(0), n = 0; i >= 0; i = bsBonds
        .nextSetBit(i + 1))
      getBondRecordMOL(mol, ++n, ms.bo[i], atomMap, asV3000, asJSON);
    // 21 21 0 0 0
    if (asV3000) {
      mol.append("M  V30 END BOND\nM  V30 END CTAB\n");
    }
    if (asJSON)
      mol.append("]}}");
    else {
      mol.append("M  END\n");
    }
    if (!asJSON && !asV3000) {
      float[] pc = ms.getPartialCharges();
      if (pc != null) {
        mol.append("> <JMOL_PARTIAL_CHARGES>\n").appendI(nAtoms)
            .appendC('\n');
        for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
            .nextSetBit(i + 1))
          mol.appendI(++n).append(" ").appendF(pc[i]).appendC('\n');
      }
    }
    return true;
  }

  private static BS getCovalentBondsForAtoms(Bond[] bonds, int bondCount, BS bsAtoms) {
    BS bsBonds = new BS();
    for (int i = 0; i < bondCount; i++) {
      Bond bond = bonds[i];
      if (bsAtoms.get(bond.atom1.i) && bsAtoms.get(bond.atom2.i)
          && bond.isCovalent())
        bsBonds.set(i);
    }
    return bsBonds;
  }

  /*
  L-Alanine
  GSMACCS-II07189510252D 1 0.00366 0.00000 0
  Figure 1, J. Chem. Inf. Comput. Sci., Vol 32, No. 3., 1992
  0 0 0 0 0 999 V3000
  M  V30 BEGIN CTAB
  M  V30 COUNTS 6 5 0 0 1
  M  V30 BEGIN ATOM
  M  V30 1 C -0.6622 0.5342 0 0 CFG=2
  M  V30 2 C 0.6622 -0.3 0 0
  M  V30 3 C -0.7207 2.0817 0 0 MASS=13
  M  V30 4 N -1.8622 -0.3695 0 0 CHG=1
  M  V30 5 O 0.622 -1.8037 0 0
  M  V30 6 O 1.9464 0.4244 0 0 CHG=-1
  M  V30 END ATOM
  M  V30 BEGIN BOND
  M  V30 1 1 1 2
  M  V30 2 1 1 3 CFG=1
  M  V30 3 1 1 4
  M  V30 4 2 2 5
  M  V30 5 1 2 6
  M  V30 END BOND
  M  V30 END CTAB
  M  END
   */

  private void getAtomRecordMOL(int iModel, ModelSet ms, SB mol, int n, Atom a, Quat q,
                                P3 pTemp, boolean asV3000, boolean asJSON) {
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    getPointTransf(iModel, ms, a, q, pTemp);
    int elemNo = a.getElementNumber();
    String sym = (a.isDeleted() ? "Xx" : Elements
        .elementSymbolFromNumber(elemNo));
    int iso = a.getIsotopeNumber();
    int charge = a.getFormalCharge();
    Object [] o = new Object[] { pTemp };
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").append(sym)
            .append(PT.sprintf(" %12.5p %12.5p %12.5p 0", "p", o));
      if (charge != 0)
        mol.append(" CHG=").appendI(charge);
      if (iso != 0)
        mol.append(" MASS=").appendI(iso);
      mol.append("\n");
    } else if (asJSON) {
      if (n != 1)
        mol.append(",");
      mol.append("{");
      if (a.getElementNumber() != 6)
        mol.append("\"l\":\"").append(a.getElementSymbol()).append("\",");
      if (charge != 0)
        mol.append("\"c\":").appendI(charge).append(",");
      if (iso != 0 && iso != Elements.getNaturalIsotope(elemNo))
        mol.append("\"m\":").appendI(iso).append(",");
      mol.append("\"x\":").appendF(a.x).append(",\"y\":").appendF(a.y)
          .append(",\"z\":").appendF(a.z).append("}");
    } else {
      mol.append(PT.sprintf("%10.5p%10.5p%10.5p", "p", o));
      mol.append(" ").append(sym);
      if (sym.length() == 1)
        mol.append(" ");
      if (iso > 0)
        iso -= Elements.getNaturalIsotope(a.getElementNumber());
      mol.append(" ");
      PT.rightJustify(mol, "  ", "" + iso);
      PT.rightJustify(mol, "   ", "" + (charge == 0 ? 0 : 4 - charge));
      mol.append("  0  0  0  0\n");
    }
  }

  /**
   * pick up the appropriate value for this atom
   * @param i 
   * 
   * @param ms
   * @param a
   * @param q
   * @param pTemp
   */
  private void getPointTransf(int i, ModelSet ms, Atom a, Quat q, P3 pTemp) {
    if (ms.isTrajectory(i >= 0 ? i : a.mi))
      ms.trajectory.getFractional(a, pTemp);
    else
      pTemp.setT(a);
    if (q != null)
      q.transform2(pTemp, pTemp);
  }

  private void getBondRecordMOL(SB mol, int n, Bond b, int[] atomMap,
                                boolean asV3000, boolean asJSON) {
    //  1  2  1  0
    int a1 = atomMap[b.atom1.i];
    int a2 = atomMap[b.atom2.i];
    int order = b.getValence();
    if (order > 3)
      order = 1;
    switch (b.order & ~Edge.BOND_NEW) {
    case Edge.BOND_AROMATIC:
      order = (asJSON ? -3 : 4);
      break;
    case Edge.BOND_PARTIAL12:
      order = (asJSON ? -3 : 5);
      break;
    case Edge.BOND_AROMATIC_SINGLE:
      order = (asJSON ? 1: 6);
      break;
    case Edge.BOND_AROMATIC_DOUBLE:
      order = (asJSON ? 2: 7);
      break;
    case Edge.BOND_PARTIAL01:
      order = (asJSON ? -1: 8);
      break;
    }
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").appendI(order).append(" ")
          .appendI(a1).append(" ").appendI(a2).appendC('\n');
    } else if (asJSON) {
      if (n != 1)
        mol.append(",");
      mol.append("{\"b\":").appendI(a1 - 1).append(",\"e\":").appendI(a2 - 1);
      if (order != 1) {
        mol.append(",\"o\":");
        if (order < 0) {
          mol.appendF(-order / 2f);
        } else {
          mol.appendI(order);   
        }
      }
      mol.append("}");
    } else {
      PT.rightJustify(mol, "   ", "" + a1);
      PT.rightJustify(mol, "   ", "" + a2);
      mol.append("  ").appendI(order).append("  0  0  0\n");
    }
  }

  @Override
  public String getChimeInfo(int tok, BS bs) {
    switch (tok) {
    case T.info:
      break;
    case T.basepair:
      return getBasePairInfo(bs);
    default:
      return getChimeInfoA(vwr.ms.at, tok, bs);
    }
    SB sb = new SB();
    vwr.getChimeMessenger().getAllChimeInfo(sb);
    return sb.appendC('\n').toString().substring(1);
  }

  private String getChimeInfoA(Atom[] atoms, int tok, BS bs) {
    SB info = new SB();
    info.append("\n");
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        switch (tok) {
        default:
          return "";
        case T.selected:
          s = a.getInfo();
          break;
        case T.atoms:
          s = "" + a.getAtomNumber();
          break;
        case T.group:
          s = a.getGroup3(false);
          break;
        case T.chain:
        case T.residue:
        case T.sequence:
        case T.group1:
          int id = a.getChainID();
          s = (id == 0 ? " " : a.getChainIDStr());
          if (id >= 300)
            s = PT.esc(s);
          switch (tok) {
          case T.residue:
            s = "[" + a.getGroup3(false) + "]" + a.group.getSeqcodeString() + ":" + s;
            break;
          case T.sequence:
          case T.group1:
            if (a.mi != modelLast) {
              info.appendC('\n');
              n = 0;
              modelLast = a.mi;
              info.append("Model " + a.getModelNumber());
              glast = null;
              clast = null;
            }
            if (a.group.chain != clast) {
              info.appendC('\n');
              n = 0;
              clast = a.group.chain;
              info.append("Chain " + s + ":\n");
              glast = null;
            }
            Group g = a.group;
            if (g != glast) {
              glast = g;
              if (tok == T.group1) {
                info.append(a.getGroup1('?'));
              } else {
                if ((n++) % 5 == 0 && n > 1)
                  info.appendC('\n');
                PT.leftJustify(info, "          ", "[" + a.getGroup3(false)
                    + "]" + a.getResno() + " ");
              }
            }
            continue;
          }
          break;
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).appendC('\n');
      }
    if (tok == T.sequence)
      info.appendC('\n');
    return info.toString().substring(1);
  }

  @Override
  public String getModelFileInfo(BS frames) {
    ModelSet ms = vwr.ms;
    SB sb = new SB();
    for (int i = 0; i < ms.mc; ++i) {
      if (frames != null && !frames.get(i))
        continue;
      String s = "[\"" + ms.getModelNumberDotted(i) + "\"] = ";
      sb.append("\n\nfile").append(s).append(PT.esc(ms.getModelFileName(i)));
      String id = (String) ms.getInfo(i, "modelID");
      if (id != null)
        sb.append("\nid").append(s).append(PT.esc(id));
      sb.append("\ntitle").append(s).append(PT.esc(ms.getModelTitle(i)));
      sb.append("\nname").append(s).append(PT.esc(ms.getModelName(i)));
      sb.append("\ntype").append(s).append(PT.esc(ms.getModelFileType(i)));
    }
    return sb.toString();
  }

  public Lst<Map<String, Object>> getAllAtomInfo(BS bs) {
    Lst<Map<String, Object>> V = new  Lst<Map<String, Object>>();
    P3 ptTemp = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      V.addLast(getAtomInfoLong(i, ptTemp));
    }
    return V;
  }

  private Map<String, Object> getAtomInfoLong(int i, P3 ptTemp) {
    ModelSet ms = vwr.ms;
    Atom atom = ms.at[i];
    Map<String, Object> info = new Hashtable<String, Object>();
    ms.getAtomIdentityInfo(i, info, ptTemp);
    info.put("element", ms.getElementName(i));
    info.put("elemno", Integer.valueOf(ms.at[i].getElementNumber()));
    info.put("x", Float.valueOf(atom.x));
    info.put("y", Float.valueOf(atom.y));
    info.put("z", Float.valueOf(atom.z));
    info.put("coord", P3.newP(atom));
    if (ms.vibrations != null && ms.vibrations[i] != null)
      ms.vibrations[i].getInfo(info);
    info.put("bondCount", Integer.valueOf(atom.getCovalentBondCount()));
    info.put("radius", Float.valueOf((float) (atom.getRasMolRadius() / 120.0)));
    info.put("model", atom.getModelNumberForLabel());
    String shape = atom.atomPropertyString(vwr, T.shape);
    if (shape != null)
      info.put("shape", shape);
    info.put("visible", Boolean.valueOf(atom.checkVisible()));
    info.put("clickabilityFlags", Integer.valueOf(atom.clickabilityFlags));
    info.put("visibilityFlags", Integer.valueOf(atom.shapeVisibilityFlags));
    info.put("spacefill", Float.valueOf(atom.getRadius()));
    String strColor = Escape.escapeColor(vwr
        .gdata.getColorArgbOrGray(atom.colixAtom));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(atom.colixAtom));
    boolean isTranslucent = C.isColixTranslucent(atom.colixAtom);
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    info.put("formalCharge", Integer.valueOf(atom.getFormalCharge()));
    info.put("partialCharge", Float.valueOf(atom.getPartialCharge()));
    float d = atom.getSurfaceDistance100() / 100f;
    if (d >= 0)
      info.put("surfaceDistance", Float.valueOf(d));
    if (ms.am[atom.mi].isBioModel) {
      info.put("resname", atom.getGroup3(false));
      char insCode = atom.group.getInsertionCode();
      int seqNum = atom.getResno();
      if (seqNum > 0)
        info.put("resno", Integer.valueOf(seqNum));
      if (insCode != 0)
        info.put("insertionCode", "" + insCode);
      info.put("name", ms.at[i].getAtomName());
      info.put("chain", atom.getChainIDStr());
      info.put("atomID", Integer.valueOf(atom.atomID));
      info.put("groupID", Integer.valueOf(atom.group.groupID));
      if (atom.altloc != '\0')
        info.put("altLocation", "" + atom.altloc);
      info.put("structure", Integer.valueOf(atom.group.getProteinStructureType()
          .getId()));
      info.put("polymerLength", Integer.valueOf(atom.group.getBioPolymerLength()));
      info.put("occupancy", Integer.valueOf(atom.getOccupancy100()));
      int temp = atom.getBfactor100();
      info.put("temp", Integer.valueOf(temp / 100));
    }
    return info;
  }

  public Lst<Map<String, Object>> getAllBondInfo(Object bsOrArray) {
    Lst<Map<String, Object>> v = new Lst<Map<String, Object>>();
    ModelSet ms = vwr.ms;
    int bondCount = ms.bondCount;
    Bond[] bonds = ms.bo;
    BS bs1;
    if (bsOrArray instanceof String) {
      bsOrArray = vwr.getAtomBitSet(bsOrArray);
    }
    P3 ptTemp = new P3();
    if (bsOrArray instanceof BS[]) {
      bs1 = ((BS[]) bsOrArray)[0];
      BS bs2 = ((BS[]) bsOrArray)[1];
      for (int i = 0; i < bondCount; i++) {
        int ia = bonds[i].atom1.i;
        int ib = bonds[i].atom2.i;
        if (bs1.get(ia) && bs2.get(ib) || bs2.get(ia) && bs1.get(ib))
          v.addLast(getBondInfo(i, ptTemp));
      }
    } else if (bsOrArray instanceof BondSet) {
      bs1 = (BS) bsOrArray;
      for (int i = bs1.nextSetBit(0); i >= 0 && i < bondCount; i = bs1
          .nextSetBit(i + 1))
        v.addLast(getBondInfo(i, ptTemp));
    } else if (bsOrArray instanceof BS) {
      bs1 = (BS) bsOrArray;
      int thisAtom = (bs1.cardinality() == 1 ? bs1.nextSetBit(0) : -1);
      for (int i = 0; i < bondCount; i++) {
        if (thisAtom >= 0 ? (bonds[i].atom1.i == thisAtom || bonds[i].atom2.i == thisAtom)
            : bs1.get(bonds[i].atom1.i) && bs1.get(bonds[i].atom2.i))
          v.addLast(getBondInfo(i, ptTemp));
      }
    }
    return v;
  }

  private Map<String, Object> getBondInfo(int i, P3 ptTemp) {
    Bond bond = vwr.ms.bo[i];
    Atom atom1 = bond.atom1;
    Atom atom2 = bond.atom2;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("_bpt", Integer.valueOf(i));
    Map<String, Object> infoA = new Hashtable<String, Object>();
    vwr.ms.getAtomIdentityInfo(atom1.i, infoA, ptTemp);
    Map<String, Object> infoB = new Hashtable<String, Object>();
    vwr.ms.getAtomIdentityInfo(atom2.i, infoB, ptTemp);
    info.put("atom1", infoA);
    info.put("atom2", infoB);
    info.put("order", Float.valueOf(PT.fVal(Edge
        .getBondOrderNumberFromOrder(bond.order))));
    info.put("type", Edge.getBondOrderNameFromOrder(bond.order));
    info.put("radius", Float.valueOf((float) (bond.mad / 2000.)));
    info.put("length_Ang", Float.valueOf(atom1.distance(atom2)));
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = Escape.escapeColor(vwr.gdata.getColorArgbOrGray(bond.colix));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(bond.colix));
    if (C.isColixTranslucent(bond.colix))
      info.put("translucent", Boolean.TRUE);
    return info;
  }

  public Map<String, Lst<Map<String, Object>>> getAllChainInfo(BS bs) {
    Map<String, Lst<Map<String, Object>>> finalInfo = new Hashtable<String, Lst<Map<String, Object>>>();
    Lst<Map<String, Object>> modelVector = new  Lst<Map<String, Object>>();
    int modelCount = vwr.ms.mc;
    for (int i = 0; i < modelCount; ++i) {
      Map<String, Object> modelInfo = new Hashtable<String, Object>();
      Lst<Map<String, Lst<Map<String, Object>>>> info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex", Integer.valueOf(i));
        modelInfo.put("chains", info);
        modelVector.addLast(modelInfo);
      }
    }
    finalInfo.put("models", modelVector);
    return finalInfo;
  }

  private Lst<Map<String, Lst<Map<String, Object>>>> getChainInfo(
                                                                    int modelIndex,
                                                                    BS bs) {
    Model model = vwr.ms.am[modelIndex];
    int nChains = model.getChainCount(true);
    Lst<Map<String, Lst<Map<String, Object>>>> infoChains = new  Lst<Map<String, Lst<Map<String, Object>>>>();
    P3 ptTemp = new P3();
        for (int i = 0; i < nChains; i++) {
      Chain chain = model.getChainAt(i);
      Lst<Map<String, Object>> infoChain = new  Lst<Map<String, Object>>();
      int nGroups = chain.groupCount;
      Map<String, Lst<Map<String, Object>>> arrayName = new Hashtable<String, Lst<Map<String, Object>>>();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.groups[igroup];
        if (bs.get(group.firstAtomIndex))
          infoChain.addLast(group.getGroupInfo(igroup, ptTemp));
      }
      if (!infoChain.isEmpty()) {
        arrayName.put("residues", infoChain);
        infoChains.addLast(arrayName);
      }
    }
    return infoChains;
  }

  private Map<String, Lst<Map<String, Object>>> getAllPolymerInfo(BS bs) {
    Map<String, Lst<Map<String, Object>>> info = new Hashtable<String, Lst<Map<String, Object>>>();
    if (vwr.ms.bioModelset != null)
      vwr.ms.bioModelset.getAllPolymerInfo(bs, info);
    return info;
  }

  private String getBasePairInfo(BS bs) {
    SB info = new SB();
    Lst<Bond> vHBonds = new  Lst<Bond>();
    vwr.ms.calcRasmolHydrogenBonds(bs, bs, vHBonds, true, 1, false, null);
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      getAtomResidueInfo(info, b.atom1);
      info.append(" - ");
      getAtomResidueInfo(info, b.atom2);
      info.append("\n");
    }
    return info.toString();
  }

  private static void getAtomResidueInfo(SB info, Atom atom) {
    info.append("[").append(atom.getGroup3(false)).append("]").append(
        atom.group.getSeqcodeString()).append(":");
    int id = atom.getChainID();
    info.append(id == 0 ? " " : atom.getChainIDStr());
  }

  private Map<String, Object> getAppletInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("htmlName", vwr.htmlName);
    info.put("syncId", vwr.syncId);
    info.put("fullName", vwr.fullName);
    info.put("codeBase", "" + Viewer.appletCodeBase);
    if (vwr.isApplet) {
      info.put("documentBase", Viewer.appletDocumentBase);
      info.put("registry", vwr.sm.getRegistryInfo());
    }
    info.put("version", JC.version);
    info.put("date", JC.date);
    info.put("javaVendor", Viewer.strJavaVendor);
    info.put("javaVersion", Viewer.strJavaVersion
        + (!vwr.isJS ? "" : vwr.isWebGL ? "(WebGL)" : "(HTML5)"));
    info.put("operatingSystem", Viewer.strOSName);
    return info;
  }

  private Map<String, Object> getAnimationInfo() {
    AnimationManager am = vwr.am;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("firstModelIndex", Integer.valueOf(am.firstFrameIndex));
    info.put("lastModelIndex", Integer.valueOf(am.lastFrameIndex));
    info.put("animationDirection", Integer.valueOf(am.animationDirection));
    info.put("currentDirection", Integer.valueOf(am.currentDirection));
    info.put("displayModelIndex", Integer.valueOf(am.cmi));
    if (am.animationFrames != null) {
      info.put("isMovie", Boolean.TRUE);
      info.put("frames", Escape.eAI(am.animationFrames));
      info.put("currentAnimationFrame", Integer.valueOf(am.caf));
    }
    info.put("displayModelNumber", vwr.getModelNumberDotted(am.cmi));
    info.put("displayModelName", (am.cmi >= 0 ? vwr.getModelName(am.cmi) : ""));
    info.put("animationFps", Integer.valueOf(am.animationFps));
    info.put("animationReplayMode", T.nameOf(am.animationReplayMode));
    info.put("firstFrameDelay", Float.valueOf(am.firstFrameDelay));
    info.put("lastFrameDelay", Float.valueOf(am.lastFrameDelay));
    info.put("animationOn", Boolean.valueOf(am.animationOn));
    info.put("animationPaused", Boolean.valueOf(am.animationPaused));
    return info;
  }

  private Map<String, Object> getBoundBoxInfo() {
    P3[] pts = vwr.ms.getBoxInfo(null, 1).getBoundBoxPoints(true);
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("center", P3.newP(pts[0]));
    info.put("vector", V3.newV(pts[1]));
    info.put("corner0", P3.newP(pts[2]));
    info.put("corner1", P3.newP(pts[3]));
    return info;
  }

  private Map<String, Object> getShapeInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    SB commands = new SB();
    Shape[] shapes = vwr.shm.shapes;
    if (shapes != null)
      for (int i = 0; i < JC.SHAPE_MAX; ++i) {
        Shape shape = shapes[i];
        if (shape != null) {
          String shapeType = JC.shapeClassBases[i];
          Lst<Map<String, Object>> shapeDetail = shape.getShapeDetail();
          if (shapeDetail != null)
            info.put(shapeType, shapeDetail);
        }
      }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }

  private SV getAnnotationInfo(Object atomExpression, int type) {
    BS bsAtoms = vwr.getAtomBitSet(atomExpression);
    int iModel = vwr.ms.getModelBS(bsAtoms, false).nextSetBit(0);
    if (iModel < 0)
      return null;
    Map<String, Object> modelinfo = vwr.ms.getModelAuxiliaryInfo(iModel);
    SV objAnn = (SV) modelinfo.get(type == T.domains ? "domains"
        : "validation");
    if (objAnn == null || objAnn.tok != T.hash)
      return null;
    vwr.getAnnotationParser(false).initializeAnnotation(objAnn, type, iModel);
    return objAnn.mapGet("_list");    
  }

  @SuppressWarnings("unchecked")
  private Lst<Map<String, Object>> getMeasurementInfo() {
    return (Lst<Map<String, Object>>) vwr.getShapeProperty(JC.SHAPE_MEASURES,
        "info");
  }


  private Object getMouseInfo() {
    if (!vwr.haveDisplay)
      return null;
    Map<String, Object> info = new Hashtable<String, Object>();
    Lst<Object> list = new Lst<Object>();
    ActionManager am = vwr.acm;
    for (Object obj : am.b.getBindings().values()) {
      if (obj instanceof Boolean)
        continue;
      if (AU.isAI(obj)) {
        int[] binding = (int[]) obj;
        obj = new String[] { Binding.getMouseActionName(binding[0], false),
            ActionManager.getActionName(binding[1]) };
      }
      list.addLast(obj);
    }
    info.put("bindings", list);
    info.put("bindingName", am.b.name);
    info.put("actionNames", ActionManager.actionNames);
    info.put("actionInfo", ActionManager.actionInfo);
    info.put("bindingInfo", PT.split(am.getBindingInfo(null), "\n"));
    return info;
  }

  /**
   * PDB or PQR only
   * 
   * @param bs
   *        selected atoms
   * @param out
   *        StringXBuilder or BufferedWriter
   * @return PDB file data string
   */
  @Override
  public String getPdbAtomData(BS bs, OC out, boolean isPQR, boolean doTransform, boolean allTrajectories) {
    if (vwr.ms.ac == 0 || bs.nextSetBit(0) < 0)
      return "";
    if (out == null) {
      out = vwr.getOutputChannel(null, null);
    } else {
      isPQR |= (out.getType().indexOf("PQR") >= 0);
      doTransform |= (out.getType().indexOf("-coord true") >= 0);
    }
    Atom[] atoms = vwr.ms.at;
    Model[] models = vwr.ms.am;
    int iModel = atoms[bs.nextSetBit(0)].mi;
    int iModelLast = -1;
    boolean isBiomodel = false;
    String occTemp = "%6.2Q%6.2b          ";
    if (isPQR) {
      occTemp = "%8.4P%7.4V       ";
      float charge = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        charge += atoms[i].getPartialCharge();
      out.append(
          "REMARK   1 PQR file generated by Jmol " + Viewer.getJmolVersion())
          .append("\nREMARK   1 " + "created " + (new Date()))
          .append("\nREMARK   1 Forcefield Used: unknown\nREMARK   1")
          .append("\nREMARK   5")
          .append(
              "\nREMARK   6 Total charge on this protein: " + charge
                  + " e\nREMARK   6\n");
    }
    
    
    int lastAtomIndex = bs.length() - 1;
    boolean showModels = (iModel != atoms[lastAtomIndex].mi);
    Lst<String> lines = new Lst<String>();
    boolean isMultipleBondPDB = models[iModel].isPdbWithMultipleBonds;
    LabelToken[] tokens;
    P3 ptTemp = new P3();
    Object[] o = new Object[] { ptTemp };
    Quat q = (doTransform ? vwr.tm.getRotationQ() : null);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      isBiomodel = models[a.mi].isBioModel;
      if (showModels && a.mi != iModelLast) {
        if (iModelLast != -1)
          out.append("ENDMDL\n");
        iModelLast = a.mi;
        out.append("MODEL     " + (iModelLast + 1) + "\n");
        lines = new Lst<String>();
      }
      String sa = a.getAtomName();
      boolean leftJustify = (a.getElementSymbol().length() == 2
          || sa.length() >= 4 || PT.isDigit(sa.charAt(0)));
      boolean isHetero = a.isHetero();
      if (!isBiomodel)
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "HETATM%5.-5i %-4.4a%1AUNK %1c   1%1E   _XYZ_" + occTemp,
            '\0', null) : LabelToken
            .compile(vwr,
                "HETATM%5.-5i  %-3.3a%1AUNK %1c   1%1E   _XYZ_"
                    + occTemp, '\0', null)

        );
      else if (isHetero)
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "HETATM%5.-5i %-4.4a%1A%3.3n %1c%4.-4R%1E   _XYZ_"
                + occTemp, '\0', null) : LabelToken.compile(vwr,
            "HETATM%5.-5i  %-3.3a%1A%3.3n %1c%4.-4R%1E   _XYZ_"
                + occTemp, '\0', null));
      else
        tokens = (leftJustify ? LabelToken.compile(vwr,
            "ATOM  %5.-5i %-4.4a%1A%3.3n %1c%4.-4R%1E   _XYZ_"
                + occTemp, '\0', null) : LabelToken.compile(vwr,
            "ATOM  %5.-5i  %-3.3a%1A%3.3n %1c%4.-4R%1E   _XYZ_"
                + occTemp, '\0', null));
      String XX = a.getElementSymbolIso(false).toUpperCase();
      XX = pdbKey(a.group.getBioPolymerIndexInModel())
          + pdbKey(a.group.groupIndex)
          + LabelToken.formatLabelAtomArray(vwr, a, tokens, '\0', null, ptTemp)
          + (XX.length() == 1 ? " " + XX : XX.substring(0, 2)) + "  ";
      getPointTransf(-1, vwr.ms, a, q, ptTemp);
      String xyz = PT.sprintf("%8.3p%8.3p%8.3p", "p", o);
      if (xyz.length() > 24)
        xyz = PT.sprintf("%8.2p%8.2p%8.2p", "p", o);
      XX = PT.rep(XX, "_XYZ_", xyz);
      lines.addLast(XX);
    }
    Map<Integer, Integer> map = new Hashtable<Integer, Integer>();
    fixPDBFormat(lines, map, out);
    if (showModels) {
      out.append("ENDMDL\n");
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        boolean isHetero = (!isBiomodel || a.isHetero());
        boolean isCysS = !isHetero && (a.getElementNumber() == 16);
        if (isHetero || isMultipleBondPDB || isCysS) {
          Bond[] bonds = a.bonds;
          if (bonds != null)
            for (int j = 0; j < bonds.length; j++) {
              int iThis = a.getAtomNumber();
              Atom a2 = bonds[j].getOtherAtom(a);
              if (!bs.get(a2.i))
                continue;
              int n = bonds[j].getCovalentOrder();
              if (n == 1
                  && (isMultipleBondPDB && !isHetero && !isCysS || isCysS
                      && a2.getElementNumber() != 16))
                continue;
              int iOther = a2.getAtomNumber();
              switch (n) {
              case 2:
              case 3:
                if (iOther < iThis)
                  continue; // only one entry in this case -- pseudo-PDB style
                //$FALL-THROUGH$
              case 1:
                out.append("CONECT").append(
                    PT.formatStringS("%5s", "s", "" + map.get(Integer.valueOf(iThis))));
                String s = PT.formatStringS("%5s", "s", "" + map.get(Integer.valueOf(iOther)));
                for (int k = 0; k < n; k++)
                  out.append(s);
                out.append("\n");
                break;
              }
            }
        }
      }
    }
    return out.toString();
  }

  private String pdbKey(int np) {
    String xp = (np < 0 ? "!999" : "   " + np); 
    return xp.substring(xp.length() - 4);
  }

  /**
   * must re-order by resno and then renumber atoms
   * and add TER records based on BioPolymers
   * 
   * note: 3hbt has a break between residues 39 and 51 with
   *       no TER record, but Jmol will put that in.
   * 
   * @param lines
   * @param map
   * @param out 
   */
  private void fixPDBFormat(Lst<String> lines, Map<Integer, Integer> map, OC out) {
    lines.addLast("!999!999XXXXXX99999999999999999999!99!");
    String[] alines = new String[lines.size()]; 
    lines.toArray(alines);
    Arrays.sort(alines);
    lines.clear();
    for (int i = 0, n = alines.length; i < n; i++) {
      lines.addLast(alines[i]);
    }
    String lastPoly = null;
    String lastLine = null;
    int n = lines.size();
    for (int i = 0; i < n; i++) {
      String s = lines.get(i);
      String poly = s.substring(0, 4);
      s = s.substring(8);
      boolean isTerm = false;
      boolean isLast = (s.indexOf("!99!") >= 0);
      if (!poly.equals(lastPoly) || isLast) {
        if (lastPoly != null && !lastPoly.equals("!999")) {
          isTerm = true;
          //TER     458      ASN A  78C                                                      
          s = "TER   " + lastLine.substring(6, 11) + "      "
              + lastLine.substring(17, 27);
          lines.add(i, poly + "!!!!" + s);
          n++;
        }
        lastPoly = poly;
      }
      if (isLast && !isTerm)
        break;
      lastLine = s;
      if (map != null && !isTerm)
        map.put(Integer.valueOf(PT.parseInt(s.substring(6, 11))), Integer.valueOf(i + 1));
      String si = "     " + (i + 1);
      out.append(s.substring(0, 6)).append(si.substring(si.length() - 5))
          .append(s.substring(11)).append("\n");
    }
  }

//
//  /**
//   * PDB line sorter 
//   * @param s1 
//   * @param s2 
//   * @return -1, 0, or 1
//   */
//  public int compare(String s1, String s2) {
//    int atA = PT.parseInt(s1.substring(10, 16));
//    int atB = PT.parseInt(s2.substring(10, 16));
//    int resA = PT.parseInt(s1.substring(26, 30));
//    int resB = PT.parseInt(s2.substring(26, 30));
//    return (resA < resB ? -1 : resA > resB ? 1 : atA < atB ? -1
//        : atA > atB ? 1 : 0);
//  }

  /* **********************
   * 
   * Jmol Data Frame methods
   * 
   *****************************/

  @Override
  @SuppressWarnings("static-access")
  public String getPdbData(int modelIndex, String type, BS bsSelected,
                           Object[] parameters, OC out, boolean addStructure) {
    if (vwr.ms.isJmolDataFrameForModel(modelIndex))
      modelIndex = vwr.ms.getJmolDataSourceFrame(modelIndex);
    if (modelIndex < 0)
      return "";
    Model model = vwr.ms.am[modelIndex];
    boolean isPDB = model.isBioModel;
    if (parameters == null && !isPDB)
      return null;
    if (out == null)
      out = vwr.getOutputChannel(null, null);
    SB pdbCONECT = new SB();
    boolean isDraw = (type.indexOf("draw") >= 0);
    BS bsAtoms = null;
    BS bsWritten = new BS();
    char ctype = '\0';
    LabelToken[] tokens = vwr.ms.getLabeler().compile(vwr,
        "ATOM  %-6i%4a%1A%3.-3n %1c%4R%1E   ", '\0', null);
    if (parameters == null) {
      ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type
          .charAt(11) : 'R');
      ((JmolBioModel) model).getPdbData(type, ctype, isDraw, bsSelected, out, tokens, pdbCONECT,
          bsWritten);
      bsAtoms = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    } else {
      // plot property x y z....
      bsAtoms = (BS) parameters[0];
      float[] dataX = (float[]) parameters[1];
      float[] dataY = (float[]) parameters[2];
      float[] dataZ = (float[]) parameters[3];
      boolean haveY = (dataY != null);
      boolean haveZ = (dataZ != null);
      P3 minXYZ = (P3) parameters[4];
      P3 maxXYZ = (P3) parameters[5];
      P3 factors = (P3) parameters[6];
      P3 center = (P3) parameters[7];
      String format = (String) parameters[8];
      String[] properties = (String[]) parameters[9];
      boolean isPDBFormat = (factors != null && format == null);
      Atom[] atoms = vwr.ms.at;
      if (isPDBFormat) {
        out.append("REMARK   6 Jmol PDB-encoded data: ").append(type)
            .append("; ").append(Viewer.getJmolVersion()).append("; ").append(vwr.apiPlatform.getDateFormat(null)).append("\n");
        out.append("REMARK   6 Jmol data").append(" min = ")
            .append(Escape.eP(minXYZ)).append(" max = ")
            .append(Escape.eP(maxXYZ)).append(" unScaledXyz = xyz * ")
            .append(Escape.eP(factors)).append(" + ").append(Escape.eP(center))
            .append(";\n");
        String atomNames = null;
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          String name = "" + atoms[i].getAtomName();
          if (atomNames != null || name.length() > 4) {
            if (atomNames == null) {
              atomNames = "";
              i = -1;
              continue;
            }
            atomNames += " " + name;
          }
        }
        if (atomNames != null)
          out.append("REMARK   6 Jmol atom names").append(atomNames).append("\n");
        String resNames = null;
        for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
            .nextSetBit(i + 1)) {
          String name = "" + atoms[i].getGroup3(true);
          if (resNames != null || name.length() > 3) {
            if (resNames == null) {
              resNames = "";
              i = -1;
              continue;
            }
            resNames += " " + name;
          }
        }
        if (resNames != null)
          out.append("REMARK   6 Jmol residue names").append(resNames).append("\n");
        for (int i = 0; i < properties.length; i++)
          if (properties[i] != null)
            out.append("REMARK   6 Jmol property ").append(properties[i]).append(";\n");
      }
      String strExtra = "";
      Atom atomLast = null;
      P3 ptTemp = new P3();
      if (!isPDBFormat) {
        if (format == null)
          format = "%-5i %-10s %-13.5f "
              + (haveZ ? "%-13.5f %-13.5f" : haveY ? "%-13.5f" : "");
        format += "\n";

      }
      for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), n++) {
        float x = dataX[n];
        float y = (haveY ? dataY[n] : 0f);
        float z = (haveZ ? dataZ[n] : 0f);
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
          continue;
        Atom a = atoms[i];
        if (isPDBFormat) {
          out.append(LabelToken.formatLabelAtomArray(vwr, a, tokens, '\0',
              null, ptTemp));
          if (isPDB)
            bsWritten.set(i);
          out.append(PT.sprintf(
              "%-8.2f%-8.2f%-10.2f    %6.3f          %2s    %s\n", "ssF",
              new Object[] { a.getElementSymbolIso(false).toUpperCase(),
                  strExtra, new float[] { x, y, z, 0f } }));
          if (atomLast != null
              && atomLast.group.getBioPolymerIndexInModel() == a.group
                  .getBioPolymerIndexInModel())
            pdbCONECT.append("CONECT")
                .append(PT.formatStringI("%5i", "i", atomLast.getAtomNumber()))
                .append(PT.formatStringI("%5i", "i", a.getAtomNumber()))
                .appendC('\n');
        } else if (haveZ) {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x, y, z } }));
        } else if (haveY) {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x, y } }));
        } else {
          out.append(PT.sprintf(
              format,
              "isF",
              new Object[] { Integer.valueOf(a.getAtomNumber()),
                  a.getAtomName(), new float[] { x } }));
        }
        atomLast = a;
      }
    }
    out.append(pdbCONECT.toString());
    if (isDraw)
      return out.toString();
    bsSelected.and(bsAtoms);
    if (isPDB && addStructure)
      out.append("\n\n"
          + vwr.ms.getProteinStructureState(bsWritten, ctype == 'R' ? T.ramachandran : T.pdb));
    return out.toString();
  }

  /*
   * <molecule title="acetic_acid.mol"
   * xmlns="http://www.xml-cml.org/schema/cml2/core"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.xml-cml.org/schema/cml2/core cmlAll.xsd">
   * <atomArray> <atom id="a1" elementType="C" x3="0.1853" y3="0.0096"
   * z3="0.4587"/> <atom id="a2" elementType="O" x3="0.6324" y3="1.0432"
   * z3="0.8951"/> <atom id="a3" elementType="C" x3="-1.0665" y3="-0.1512"
   * z3="-0.3758"/> <atom id="a4" elementType="O" x3="0.7893" y3="-1.1734"
   * z3="0.6766" formalCharge="-1"/> <atom id="a5" elementType="H" x3="-1.7704"
   * y3="-0.8676" z3="0.1055"/> <atom id="a6" elementType="H" x3="-0.8068"
   * y3="-0.5215" z3="-1.3935"/> <atom id="a7" elementType="H" x3="-1.5889"
   * y3="0.8259" z3="-0.4854"/> </atomArray> <bondArray> <bond atomRefs2="a1 a2"
   * order="partial12"/> <bond atomRefs2="a1 a3" order="S"/> <bond
   * atomRefs2="a1 a4" order="partial12"/> <bond atomRefs2="a3 a5" order="S"/>
   * <bond atomRefs2="a3 a6" order="S"/> <bond atomRefs2="a3 a7" order="S"/>
   * </bondArray> </molecule>
   */
  @Override
  public String getModelCml(BS bs, int atomsMax, boolean addBonds, boolean doTransform, boolean allTrajectories) {
    // not allowing full trajectory business here. 
    SB sb = new SB();
    int nAtoms = BSUtil.cardinalityOf(bs);
    if (nAtoms == 0)
      return "";
    // creating an instance prevents pre-loading by JavaScript
    Interface.getInterface("javajs.util.XmlUtil", vwr, "file");
    XmlUtil.openTag(sb, "molecule");
    XmlUtil.openTag(sb, "atomArray");
    BS bsAtoms = new BS();
    Atom[] atoms = vwr.ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (--atomsMax < 0)
        break;
      Atom atom = atoms[i];
      String name = atom.getAtomName();
      PT.rep(name, "\"", "''");
      bsAtoms.set(atom.i);
      XmlUtil.appendTag(sb, "atom/", new String[] { "id",
          "a" + (atom.i + 1), "title", atom.getAtomName(), "elementType",
          atom.getElementSymbol(), "x3", "" + atom.x, "y3", "" + atom.y, "z3",
          "" + atom.z });
    }
    XmlUtil.closeTag(sb, "atomArray");
    if (addBonds) {
      XmlUtil.openTag(sb, "bondArray");
      int bondCount = vwr.ms.bondCount;
      Bond[] bonds = vwr.ms.bo;
      for (int i = 0; i < bondCount; i++) {
        Bond bond = bonds[i];
        Atom a1 = bond.atom1;
        Atom a2 = bond.atom2;
        if (!bsAtoms.get(a1.i) || !bsAtoms.get(a2.i))
          continue;
        String order = Edge.getCmlBondOrder(bond.order);
        if (order == null)
          continue;
        XmlUtil.appendTag(sb, "bond/", new String[] { "atomRefs2",
            "a" + (bond.atom1.i + 1) + " a" + (bond.atom2.i + 1),
            "order", order, });
      }
      XmlUtil.closeTag(sb, "bondArray");
    }
    XmlUtil.closeTag(sb, "molecule");
    return sb.toString();
  }


}
