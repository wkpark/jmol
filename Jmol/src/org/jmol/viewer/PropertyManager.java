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

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;

class PropertyManager {

  Viewer viewer;

  PropertyManager(Viewer viewer) {
    this.viewer = viewer;
  }

  public String getStringProperty(String infoType) {
    return (String)getProperty(infoType);
  }
  
  public String getJSONProperty(String infoType) {
    return  "{" + toJSON(infoType, getProperty(infoType)) + "}";
  }
  
  public String getStringProperty(String infoType, String paramInfo) {
    return (String)getProperty(infoType);
  }
  
  public String getJSONProperty(String infoType, String paramInfo) {
    return "{" + toJSON(infoType, getProperty(infoType,paramInfo)) + "}";
  }
  
  public Object getProperty(String infoType) {
    if(infoType.equalsIgnoreCase("fileContents"))
      return viewer.getCurrentFileAsString();
    if(infoType.equalsIgnoreCase("fileHeader"))
      return viewer.getFileHeader();      
    if(infoType.equalsIgnoreCase("fileName"))
      return viewer.fileManager.getFullPathName();      
    if(infoType.equalsIgnoreCase("orientationInfo"))
      return viewer.transformManager.getOrientationInfo();       
    if(infoType.equalsIgnoreCase("modelInfo"))
      return getModelInfoObject();      
    if(infoType.equalsIgnoreCase("transformInfo"))
      return viewer.transformManager.getMatrixRotate();      
    if(infoType.equalsIgnoreCase("centerInfo"))
      return viewer.getCenter();      
    if(infoType.equalsIgnoreCase("boundboxInfo"))
      return viewer.modelManager.getBoundBoxInfo();
    if(infoType.equalsIgnoreCase("zoomInfo")) {
      if (viewer.getZoomEnabled()) 
        return (new Integer(viewer.getZoomPercentSetting()));
      return "off";
    }
    return "getProperty ERROR\n\nOptions include\n"
    + "\n getProperty(\"fileName\")"
    + "\n getProperty(\"fileHeader\")"
    + "\n getProperty(\"fileContents\")"
    + "\n\n getProperty(\"modelInfo\")"
    + "\n\n getProperty(\"boundboxInfo\")"
    + "\n getProperty(\"centerInfo\")"
    + "\n getProperty(\"orientationInfo\")"
    + "\n getProperty(\"transformInfo\")"
    + "\n getProperty(\"zoomInfo\")"
    + "";
  }

  public Object getProperty(String infoType, String paramInfo) {

    //System.out.println("viewer.getProperty(\"" + infoType+"\", \"" + paramInfo + "\")");
    
    if(infoType.equalsIgnoreCase("fileContents")) {
      if(paramInfo.length() > 0)
        return viewer.getFileAsString(paramInfo);
    }

    if(infoType.equalsIgnoreCase("atomList")) {
      if(paramInfo.length() > 0){
        return viewer.selectionManager.getAtomBitSetVector(paramInfo);
      }
    }    
    if(infoType.equalsIgnoreCase("atomInfo")) {
      if(paramInfo.length() > 0)
        return getAtomBitSetDetail(paramInfo);
    }
    if(infoType.equalsIgnoreCase("bondInfo")) {
      if(paramInfo.length() > 0)
        return getBondDetail(paramInfo);
    }
    if(infoType.equalsIgnoreCase("extractModel")) {
      if(paramInfo.length() > 0)
        return getModelExtract(paramInfo);
    }

    if(infoType.equalsIgnoreCase("callbackStatus")) 
       return viewer.statusManager.getStatusChanged(paramInfo);
    return "getProperty ERROR\n\nOptions include "
    + "\n getProperty(\"fileContents\",\"<pathname>\")"
    + "\n getProperty(\"atomList\",\"<atom selection>\")"
    + "\n getProperty(\"atomInfo\",\"<atom selection>\")"
    + "\n getProperty(\"bondInfo\",\"<atom selection>\")"
    + "\n getProperty(\"extractModel\",\"<atom selection>\")"
    + "\n getProperty(\"callbackStatus\",\"CallbackNameList\")"
    + "";
  }
  Hashtable getModelInfoObject() {
    Hashtable info = new Hashtable();
    int modelCount = viewer.getModelCount();
    info.put("modelCount",new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", 
        new Boolean(viewer.modelSetHasVibrationVectors()));
    //Properties props = viewer.getModelSetProperties();
    //str = str.concat(listPropertiesJSON(props));
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt",new Integer(i));
      model.put("num",new Integer(viewer.getModelNumber(i)));
      model.put("name",viewer.getModelName(i));
      model.put("vibrationVectors", new Boolean(viewer.modelHasVibrationVectors(i)));
      models.add(model);
    }
    info.put("models",models);
    return info;
  }

/*  String listPropertiesJSON(Properties props) {
    String str = "";
    String sep = ",";
    if (props == null) {
      return "";
    }
    return str;
  }
  
 */
 
  String packageJSON (String infoType, String info) {
    if (infoType == null) return info;
    return "\"" + infoType + "\": " + info;
  }
  
  String fixString(String s) {
   return simpleReplace(s,"\"","\\\"");  
  }
  
  String toJSON (String infoType, Object info){
    String str = "";
    String sep = "";
    if (info instanceof String) 
      return packageJSON (infoType, "\"" + fixString((String)info) + "\"");
    if (info instanceof Vector) {
      str = "[";
      int imax = ((Vector)info).size();
      for (int i = 0; i < imax; i++) {
        str = str + sep + toJSON(null, ((Vector)info).get(i));
        sep = ",";
      }
      str = str + "]";
      return packageJSON (infoType, str);
    }
    if (info instanceof Matrix3f) {
      str = "[";
      str = str + "[" + ((Matrix3f)info).m00 + ","  + ((Matrix3f)info).m01 + ","  + ((Matrix3f)info).m02 + "]";
      str = str + ",[" + ((Matrix3f)info).m10 + ","  + ((Matrix3f)info).m11 + ","  + ((Matrix3f)info).m12 + "]";
      str = str + ",[" + ((Matrix3f)info).m20 + ","  + ((Matrix3f)info).m21 + ","  + ((Matrix3f)info).m22 + "]";
      str = str + "]";
      return packageJSON (infoType, str);
    }
    if (info instanceof Point3f) {
      str = str + "[" + ((Point3f)info).x + ","  + ((Point3f)info).y + ","  + ((Point3f)info).z + "]";
      return packageJSON (infoType, str);
    }
    if (info instanceof Hashtable) {
      str = "{";
      Enumeration e = ((Hashtable)info).keys();
      while (e.hasMoreElements()) {
        String key = (String)e.nextElement();
        str = str + sep + packageJSON(key, toJSON(null, ((Hashtable)info).get(key)));
        sep = ",";
      }         
      str = str + "}";
      return packageJSON (infoType, str);
    }
    return packageJSON (infoType, info.toString());
  }
  
  Vector getAtomBitSetDetail(String atomExpression) {
    BitSet bs = viewer.getAtomBitSet(atomExpression);
    return getAtomInfoFromBitSet(bs,("pdb" == viewer.getModelSetTypeName()));
  }
  
  Vector getBondDetail(String atomExpression) {
    BitSet bs = viewer.getAtomBitSet(atomExpression);
    return getBondInfoFromBitSet(bs);
  }

  Vector getAtomInfoFromBitSet(BitSet bs, boolean isPDB) {
    Vector V = new Vector();
    int atomCount = viewer.modelManager.getAtomCount();
    for (int i = 0; i < atomCount; i++) 
      if (bs.get(i)) 
        V.add(getAtomInfo(i, isPDB));
    return V;
  }
          
  Hashtable getAtomInfo(int i, boolean isPDB) {
    Atom atom = viewer.modelManager.frame.getAtomAt(i);
    Hashtable info = new Hashtable();
    info.put("_ipt", new Integer(i));
    info.put("atomno", new Integer(atom.getAtomNumber()));
    info.put("sym", viewer.modelManager.getElementSymbol(i));
    info.put("elemno", new Integer(atom.getElementNumber()));
    info.put("x", new Float(viewer.modelManager.getAtomX(i)));
    info.put("y", new Float(viewer.modelManager.getAtomY(i)));
    info.put("z", new Float(viewer.modelManager.getAtomZ(i)));
    info.put("model", new Integer(atom.getModelTagNumber()));
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius()/120)));
    info.put("info", viewer.modelManager.getAtomInfo(i));
    if (isPDB) {
      info.put("resname", atom.getGroup3());
      info.put("resno", atom.getSeqcodeString());
      char chainID = atom.getChainID();
      info.put("name", viewer.modelManager.getAtomName(i));
      info.put("chain", (chainID == '\0' ? "" : "" + chainID ));
      info.put("atomID", new Integer(atom.getSpecialAtomID()));
      info.put("groupID", new Integer(atom.getGroupID()));
      info.put("structure", new Integer(atom.getProteinStructureType()));
      info.put("polymerLength", new Integer(atom.getPolymerLength()));
      info.put("occupancy", new Integer(atom.getOccupancy()));
      int temp = atom.getBfactor100();
      info.put("temp", new Integer((temp<0 ? 0 : temp/100)));
    }
    return info;
  }  

  Vector getBondInfoFromBitSet(BitSet bs) {
    Frame frame = viewer.modelManager.frame;
    Vector V = new Vector();
    int bondCount = viewer.getBondCount();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(viewer.modelManager.frame.getBondAt(i).getAtom1().atomIndex) && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) 
        V.add(getBondInfo(i));
    return V;
  }

  Hashtable getBondInfo(int i) {
    Hashtable info = new Hashtable();
    info.put("_bpt", new Integer(i));
    info.put("_apt1", new Integer(viewer.modelManager.getBondAtom1(i).atomIndex));
    info.put("_apt2", new Integer(viewer.modelManager.getBondAtom2(i).atomIndex));
    info.put("order", new Integer(viewer.modelManager.getBondOrder(i)));
    return info;
  }  
  
  String getModelExtract(String atomExpression) {
    BitSet bs = viewer.selectionManager.getAtomBitSet(atomExpression);
    return viewer.fileManager.getFullPathName() 
        + "\nEXTRACT: " + bs + "\nJmol\n"
        + viewer.modelManager.getModelExtractFromBitSet(bs);
  }

  String listProperties(Properties props) {
    String str = "";
    if (props == null) {
      str = str.concat("\nProperties: null");
    } else {
      Enumeration e = props.propertyNames();
      str = str.concat("\nProperties:");
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        str = str.concat("\n " + propertyName + "=" +
                   props.getProperty(propertyName));
      }
    }
    return str;
  }
  
  final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
    "MODEL ", "SCALE",  "HETATM", "SEQRES",
    "DBREF ", };

  String getPDBHeader() {
    if ("pdb" != viewer.getModelSetTypeName()) {
      return "!Not a pdb file!";
    }
    String modelFile = viewer.getCurrentFileAsString();
    int ichMin = modelFile.length();
    for (int i = pdbRecords.length; --i >= 0; ) {
      int ichFound = -1;
      String strRecord = pdbRecords[i];
      if (modelFile.startsWith(strRecord))
        ichFound = 0;
      else {
        String strSearch = "\n" + strRecord;
        ichFound = modelFile.indexOf(strSearch);
        if (ichFound >= 0)
          ++ichFound;
      }
      if (ichFound >= 0 && ichFound < ichMin)
        ichMin = ichFound;
    }
    return modelFile.substring(0, ichMin);
  }

  String simpleReplace(String str, String strFrom, String strTo) {
     String sout = "";
     int ipt;
     int ipt0 = 0;
     int lfrom = strFrom.length();
     if (lfrom == 0) return str;
     
     while ((ipt = str.indexOf(strFrom, ipt0)) >=0) {
       sout = str.substring(ipt0,ipt) + strTo;
       ipt0 = ipt + lfrom;
     }
     sout = sout + str.substring(ipt0,str.length());
     return sout;
  }

}
