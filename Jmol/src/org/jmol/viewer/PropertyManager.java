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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Tuple3f;

import org.jmol.util.Escape;
import org.jmol.util.Logger;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of
 * properties with the getProperty() method, or its specifically cast 
 * forms getPropertyString() or getPropertyJSON().
 *
 */

class PropertyManager {

  private Viewer viewer;

  PropertyManager(Viewer viewer) {
    this.viewer = viewer;
  }


  private final static String[] propertyTypes = {
    "appletInfo"      , "", "",
    "fileName"        , "", "",
    "fileHeader"      , "", "",
    "fileContents"    , "", "",
    "fileContents"    , "<pathname>", "",
  
    "animationInfo"   , "", "",
    "modelInfo"       , "", "",
    "X -vibrationInfo", "", "",  //not implemented -- see modelInfo
    "shapeInfo"       , "", "",
    "measurementInfo" , "", "",
    
    "centerInfo"      , "", "",
    "orientationInfo" , "", "",
    "transformInfo"   , "", "",
    "atomList"        , "<atom selection>", "(visible)",
    "atomInfo"        , "<atom selection>", "(visible)",
    
    "bondInfo"        , "<atom selection>", "(visible)",
    "chainInfo"       , "<atom selection>", "(visible)",
    "polymerInfo"     , "<atom selection>", "(visible)",
    "moleculeInfo"    , "<atom selection>", "(visible)",
    "stateInfo"       , "<state type>", "all",
    
    "extractModel"    , "<atom selection>", "(visible)",
    "jmolStatus"      , "statusNameList", "",
    "jmolViewer"      , "", "",
    "messageQueue"    , "", "",
    "auxiliaryInfo"   , "", "",
    
    "boundBoxInfo"    , "", "",  
    "dataInfo"        , "<data type>", "types",
    "image"           , "", "",
    "evaluate"        , "<expression>", "",
    "menu"            , "", "",
  };

  private final static int PROP_APPLET_INFO = 0;
  private final static int PROP_FILENAME = 1;
  private final static int PROP_FILEHEADER = 2;
  private final static int PROP_FILECONTENTS = 3;
  private final static int PROP_FILECONTENTS_PATH = 4;
  
  private final static int PROP_ANIMATION_INFO = 5;
  private final static int PROP_MODEL_INFO = 6;
  //private final static int PROP_VIBRATION_INFO = 7; //not implemented -- see auxiliaryInfo
  private final static int PROP_SHAPE_INFO = 8;
  private final static int PROP_MEASUREMENT_INFO = 9;
  
  private final static int PROP_CENTER_INFO = 10;
  private final static int PROP_ORIENTATION_INFO = 11;
  private final static int PROP_TRANSFORM_INFO = 12;
  private final static int PROP_ATOM_LIST = 13;
  private final static int PROP_ATOM_INFO = 14;
  
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
  private final static int PROP_COUNT = 30;

  static int getPropertyNumber(String infoType) {
    if (infoType == null)
      return -1;
    for(int i = 0; i < PROP_COUNT; i++)
      if(infoType.equalsIgnoreCase(getPropertyName(i)))
        return i;
    return -1;
  }
  
  private static String getPropertyName(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3];
  }
  
  private static String getParamType(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3 + 1];
  }
  
  static String getDefaultParam(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3 + 2];
  }
  
  private final static String[] readableTypes = {
    "", "stateinfo", "extractmodel", "filecontents", "fileheader", "image", "menu"};
  
  private boolean isReadableAsString(String infoType) {
    for (int i = readableTypes.length; --i >= 0; )
      if (infoType.equalsIgnoreCase(readableTypes[i]))
          return true;
    return false;
  }

  private boolean requestedReadable = false;
  
  synchronized Object getProperty(String returnType, String infoType, Object paramInfo) {
    if (propertyTypes.length != PROP_COUNT * 3)
      Logger.warn("propertyTypes is not the right length: " + propertyTypes.length + " != " + PROP_COUNT * 3);
    
    Object info = getPropertyAsObject(infoType, paramInfo);
    if (returnType == null)
      return info;
    requestedReadable = returnType.equalsIgnoreCase("readable");
    if (requestedReadable)
      returnType = (isReadableAsString(infoType) ? "String" : "JSON");
    if (returnType.equalsIgnoreCase("String")) return info.toString();
    if (requestedReadable)
      return Escape.toReadable(infoType, info);
    else if (returnType.equalsIgnoreCase("JSON"))
      return "{" + Escape.toJSON(infoType, info) + "}";
    return info;
  }
  
  synchronized private Object getPropertyAsObject(String infoType,
                                                  Object paramInfo) {
    //Logger.debug("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    int id = getPropertyNumber(infoType);
    boolean iHaveParameter = (paramInfo != null && paramInfo.toString()
        .length() > 0);
    Object myParam = (iHaveParameter ? paramInfo : getDefaultParam(id));
    //myParam may now be a bitset
    switch (id) {
    case PROP_APPLET_INFO:
      return viewer.getAppletInfo();
    case PROP_ANIMATION_INFO:
      return viewer.getAnimationInfo();
    case PROP_ATOM_LIST:
      return viewer.getAtomBitSetVector(myParam);
    case PROP_ATOM_INFO:
      return viewer.getAllAtomInfo(myParam);
    case PROP_AUXILIARY_INFO:
      return viewer.getAuxiliaryInfo();
    case PROP_BOND_INFO:
      return viewer.getAllBondInfo(myParam);
    case PROP_BOUNDBOX_INFO:
      return viewer.getBoundBoxInfo();
    case PROP_CENTER_INFO:
      return viewer.getRotationCenter();
    case PROP_CHAIN_INFO:
      return viewer.getAllChainInfo(myParam);
    case PROP_EXTRACT_MODEL:
      return viewer.getModelExtract(myParam);
    case PROP_FILENAME:
      return viewer.getFullPathName();
    case PROP_FILEHEADER:
      return viewer.getFileHeader();
    case PROP_FILECONTENTS:
    case PROP_FILECONTENTS_PATH:
      if (iHaveParameter)
        return viewer.getFileAsString(myParam.toString());
      return viewer.getCurrentFileAsString();
    case PROP_JMOL_STATUS:
      return viewer.getStatusChanged(myParam.toString());
    case PROP_JMOL_VIEWER:
      return viewer.getViewer();
    case PROP_MEASUREMENT_INFO:
      return viewer.getMeasurementInfo();
    case PROP_MENU:
      return viewer.getMenu();
    case PROP_MESSAGE_QUEUE:
      return viewer.getMessageQueue();
    case PROP_MODEL_INFO:
      return viewer.getModelInfo();
    case PROP_MOLECULE_INFO:
      return viewer.getMoleculeInfo(myParam);
    case PROP_ORIENTATION_INFO:
      return viewer.getOrientationInfo();
    case PROP_POLYMER_INFO:
      return viewer.getAllPolymerInfo(myParam);
    case PROP_SHAPE_INFO:
      return viewer.getShapeInfo();
    case PROP_STATE_INFO:
      return viewer.getStateInfo(myParam.toString());
    case PROP_TRANSFORM_INFO:
      return viewer.getMatrixRotate();
    case PROP_DATA_INFO:
      return viewer.getData(myParam.toString());
    case PROP_EVALUATE:
      return Eval.evaluateExpression(viewer, myParam.toString());
    case PROP_IMAGE:
      return viewer.getJpegBase64(100);
    }
    String info = "getProperty ERROR\n" + infoType + "?\nOptions include:\n";
    for (int i = 0; i < PROP_COUNT; i++) {
      String paramType = getParamType(i);
      String paramDefault = getDefaultParam(i);
      String name = getPropertyName(i);
      if (name.charAt(0) != 'X')
        info += "\n getProperty "
            + name
            + (paramType != "" ? " " + paramType
                + (paramDefault != "" ? " #default: " + paramDefault : "") : "");
    }
    return info;
  }

  public static Object extractProperty(Object property, Token[] args, int ptr) {
    if (ptr >= args.length)
      return property;
    int pt;
    Token arg = args[ptr++];
    switch (arg.tok) {
    case Token.integer:
      pt = Token.iValue(arg) - 1;  //one-based, as for array selectors
      if (property instanceof Vector) {
        Vector v = (Vector) property;
        if (pt < 0)
          pt += v.size();
        if (pt >= 0 && pt < v.size())
          return extractProperty(v.elementAt(pt), args, ptr);
        return "";
      }
      if (property instanceof String[]) {
        String[] slist = (String[]) property;
        if (pt < 0)
          pt += slist.length;
        if (pt >= 0 && pt < slist.length)
          return slist[pt];
        return "";
      }
      if (property instanceof Matrix3f) {
        Matrix3f m = (Matrix3f) property;
        float[][] f = new float[][] {
            new float[] {m.m00, m.m01, m.m02}, 
            new float[] {m.m10, m.m11, m.m12}, 
            new float[] {m.m20, m.m21, m.m22}}; 
        if (pt < 0)
          pt += 3;
        if (pt >= 0 && pt < 3)
          return extractProperty(f, args, --ptr);
        return "";
      }
      if (property instanceof float[]) {
        float[] flist = (float[]) property;
        if (pt < 0)
          pt += flist.length;
        if (pt >= 0 && pt < flist.length)
          return new Float(flist[pt]);
        return "";
      }
      if (property instanceof int[]) {
        int[] ilist = (int[]) property;
        if (pt < 0)
          pt += ilist.length;
        if (pt >= 0 && pt < ilist.length)
          return new Integer(ilist[pt]);
        return "";
      }
      if (property instanceof float[][]) {
        float[][] fflist = (float[][]) property;
        if (pt < 0)
          pt += fflist.length;
        if (pt >= 0 && pt < fflist.length)
          return extractProperty(fflist[pt], args, ptr);
        return "";
      }
      if (property instanceof int[][]) {
        int[][] iilist = (int[][]) property;
        if (pt < 0)
          pt += iilist.length;
        if (pt >= 0 && pt < iilist.length)
          return extractProperty(iilist[pt], args, ptr);
        return "";
      }
      break;
    case Token.string:
      String key = Token.sValue(arg);
      if (property instanceof Hashtable) {
        Hashtable h = (Hashtable) property;
        if (key.equalsIgnoreCase("keys")) {
          Vector keys = new Vector();
          Enumeration e = h.keys();
          while (e.hasMoreElements())
            keys.addElement(e.nextElement()); 
          return extractProperty(keys, args, ptr);
        }
        if (!h.containsKey(key)) {
          Enumeration e = h.keys();
          String newKey = "";
          while (e.hasMoreElements())
            if ((newKey = ((String) e.nextElement())).equalsIgnoreCase(key)) {
              key = newKey;
              break;
            }
        }
        if (h.containsKey(key))
          return extractProperty(h.get(key), args, ptr);
        return "";
      }
      break;
    }
    return property;
  }
}
