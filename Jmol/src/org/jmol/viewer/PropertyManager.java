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
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of
 * properties with the getProperty() method, or its specifically cast 
 * forms getPropertyString() or getPropertyJSON().
 *
 */

class PropertyManager {

  Viewer viewer;

  PropertyManager(Viewer viewer) {
    this.viewer = viewer;
  }
  
  synchronized Object getProperty(String returnType, String infoType, String paramInfo) {
    Object info = getPropertyAsObject(infoType, paramInfo);
    if (returnType == "String") return info.toString();
    if (returnType == "JSON")return "{" + toJSON(infoType, info) + "}";
    return info;
  }
  
  synchronized private Object getPropertyAsObject(String infoType, String paramInfo) {
    //System.out.println("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    boolean iHaveParameter = (paramInfo.length() > 0);
    String myParam = paramInfo;

    // status
    
    if(infoType.equalsIgnoreCase("jmolStatus")) 
      return viewer.getStatusChanged(myParam);

    // general information
    
    if(infoType.equalsIgnoreCase("appletInfo"))
      return viewer.getAppletInfo();

    if(infoType.equalsIgnoreCase("fileName"))
      return viewer.getFullPathName();      
    
    if(infoType.equalsIgnoreCase("fileHeader"))
      return viewer.getFileHeader();      

    if(infoType.equalsIgnoreCase("fileContents")) {
      if(iHaveParameter) return viewer.getFileAsString(myParam);
      return viewer.getCurrentFileAsString();
    }

    if(infoType.equalsIgnoreCase("animationInfo"))
      return viewer.getAnimationInfo();

    if(infoType.equalsIgnoreCase("modelInfo"))
      return viewer.getModelInfo();      

    // orientation stuff
    
    if(infoType.equalsIgnoreCase("boundboxInfo"))
      return viewer.getBoundBoxInfo();

    if(infoType.equalsIgnoreCase("centerInfo"))
      return viewer.getCenter();      

    if(infoType.equalsIgnoreCase("orientationInfo"))
      return viewer.getOrientationInfo();       

    if(infoType.equalsIgnoreCase("transformInfo"))
      return viewer.getMatrixRotate();      

    if(infoType.equalsIgnoreCase("zoomInfo")) {
      if (viewer.getZoomEnabled()) 
        return (new Integer(viewer.getZoomPercentSetting()));
      return "off";
    }

    // atom-bond stuff
    
    if(! iHaveParameter) myParam = "visible";

    if(infoType.equalsIgnoreCase("atomList")) 
      return viewer.getAtomBitSetVector(myParam);
    
    if(infoType.equalsIgnoreCase("atomInfo")) 
      return viewer.getAllAtomInfo(myParam);
    
    if(infoType.equalsIgnoreCase("bondInfo")) 
      return viewer.getAllBondInfo(myParam);
    
    if(infoType.equalsIgnoreCase("polymerInfo"))
      return viewer.getAllPolymerInfo(myParam);      

    if(infoType.equalsIgnoreCase("chainInfo"))
      return viewer.getAllChainInfo(myParam);      

    if(infoType.equalsIgnoreCase("stateInfo")) 
      return viewer.getAllStateInfo(myParam);

    if(infoType.equalsIgnoreCase("extractModel")) 
      return viewer.getModelExtract(myParam);

    // misc
    
    if(infoType.equalsIgnoreCase("shapeInfo"))
      return viewer.getShapeInfo();      

    if(infoType.equalsIgnoreCase("measurementInfo")) 
      return viewer.getMeasurementInfo();

    // public objects
    
    if(infoType.equalsIgnoreCase("jmolViewer"))
      return viewer.getViewer();      

    if(infoType.equalsIgnoreCase("messageQueue"))
      return viewer.getMessageQueue();      

    return "getProperty ERROR\n"+ infoType +"?\nOptions include\n"
    + "\n getProperty(\"appletInfo\")"

    + "\n\n getProperty(\"fileName\")"
    + "\n getProperty(\"fileHeader\")"
    + "\n getProperty(\"fileContents\")"
    + "\n getProperty(\"fileContents\",\"<pathname>\")"

    + "\n\n getProperty(\"animationInfo\")"
    + "\n getProperty(\"modelInfo\")"
    + "\n getProperty(\"shapeInfo\")"
    + "\n getProperty(\"measurementInfo\")"
    
    + "\n\n getProperty(\"boundboxInfo\")"
    + "\n getProperty(\"centerInfo\")"
    + "\n getProperty(\"orientationInfo\")"
    + "\n getProperty(\"transformInfo\")"
    + "\n getProperty(\"zoomInfo\")"

    + "\n\n getProperty(\"atomList\",\"<atom selection>\")"
    + "\n getProperty(\"atomInfo\",\"<atom selection>\")"
    + "\n getProperty(\"bondInfo\",\"<atom selection>\")"
    + "\n getProperty(\"polymerInfo\",\"<atom selection>\")"
    + "\n getProperty(\"chainInfo\",\"<atom selection>\")"
    + "\n getProperty(\"stateInfo\",\"<atom selection>\")"
    + "\n getProperty(\"extractModel\",\"<atom selection>\")"
    
    + "\n\n getProperty(\"jmolStatus\",\"statusNameList\")"
    + "\n getProperty(\"jmolViewer\")"
    + "\n getProperty(\"messageQueue\")"
    + "";
  }
   
  String packageJSON (String infoType, String info) {
    if (infoType == null) return info;
    return "\"" + infoType + "\": " + info;
  }
  
  String fixString(String s) {
   s = viewer.simpleReplace(s,"\"","\\\"");
   s = viewer.simpleReplace(s,"\n"," | ");
   return s;  
  }
  
  String toJSON (String infoType, Object info){

    //System.out.println(infoType+" -- "+info);

    String str = "";
    String sep = "";
    if (info == null || info instanceof String) 
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
    if (info instanceof Vector3f) {
      str = str + "[" + ((Vector3f)info).x + ","  + ((Vector3f)info).y + ","  + ((Vector3f)info).z + "]";
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

}
