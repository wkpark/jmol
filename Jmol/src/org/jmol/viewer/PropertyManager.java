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

    if(infoType.equalsIgnoreCase("fileContents")) {
      if(iHaveParameter) return viewer.getFileAsString(myParam);
      return viewer.getCurrentFileAsString();
    }

    // no second parameter here
    
    if(infoType.equalsIgnoreCase("messageQueue"))
      return viewer.getMessageQueue();      

    if(infoType.equalsIgnoreCase("jmolViewer"))
      return viewer.getViewer();      

    if(infoType.equalsIgnoreCase("fileHeader"))
      return viewer.getFileHeader();      

    if(infoType.equalsIgnoreCase("fileName"))
      return viewer.getFullPathName();      
    
    if(infoType.equalsIgnoreCase("jmolStatus")) 
      return viewer.getStatusChanged(myParam);

    if(infoType.equalsIgnoreCase("orientationInfo"))
      return viewer.getOrientationInfo();       

    if(infoType.equalsIgnoreCase("modelInfo"))
      return viewer.getModelInfoObject();      

    if(infoType.equalsIgnoreCase("transformInfo"))
      return viewer.getMatrixRotate();      

    if(infoType.equalsIgnoreCase("centerInfo"))
      return viewer.getCenter();      

    if(infoType.equalsIgnoreCase("boundboxInfo"))
      return viewer.getBoundBoxInfo();

    if(infoType.equalsIgnoreCase("zoomInfo")) {
      if (viewer.getZoomEnabled()) 
        return (new Integer(viewer.getZoomPercentSetting()));
      return "off";
    }

    if(! iHaveParameter) myParam = "all";

    if(infoType.equalsIgnoreCase("atomList")) 
      return viewer.getAtomBitSetVector(myParam);
    
    if(infoType.equalsIgnoreCase("atomInfo")) 
      return viewer.getAtomBitSetDetail(myParam);
    
    if(infoType.equalsIgnoreCase("bondInfo")) 
      return viewer.getBondDetail(myParam);
    
    if(infoType.equalsIgnoreCase("extractModel")) 
      return viewer.getModelExtract(myParam);
    
    return "getProperty ERROR\n\nOptions include\n"
    + "\n getProperty(\"fileName\")"
    + "\n getProperty(\"fileHeader\")"
    + "\n getProperty(\"fileContents\")"
    + "\n getProperty(\"fileContents\",\"<pathname>\")"
    + "\n\n getProperty(\"modelInfo\")"
    + "\n\n getProperty(\"boundboxInfo\")"
    + "\n getProperty(\"centerInfo\")"
    + "\n getProperty(\"orientationInfo\")"
    + "\n getProperty(\"transformInfo\")"
    + "\n getProperty(\"zoomInfo\")"
    + "\n getProperty(\"atomList\",\"<atom selection>\")"
    + "\n getProperty(\"atomInfo\",\"<atom selection>\")"
    + "\n getProperty(\"bondInfo\",\"<atom selection>\")"
    + "\n getProperty(\"extractModel\",\"<atom selection>\")"
    + "\n getProperty(\"callbackStatus\",\"CallbackNameList\")"
    + "";
  }
   
  String packageJSON (String infoType, String info) {
    if (infoType == null) return info;
    return "\"" + infoType + "\": " + info;
  }
  
  String fixString(String s) {
   return simpleReplace(s,"\"","\\\"");  
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

  String simpleReplace(String str, String strFrom, String strTo) {
    int fromLength = strFrom.length();
    if (str == null || fromLength == 0)
      return str;
    int ipt;
    int ipt0 = 0;
    String sout = "";
    while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
      sout += str.substring(ipt0, ipt) + strTo;
      ipt0 = ipt + fromLength;
    }
    sout += str.substring(ipt0, str.length());
    return sout;
  }


}
