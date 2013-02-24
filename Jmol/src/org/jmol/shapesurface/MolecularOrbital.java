/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

package org.jmol.shapesurface;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.Point4f;
import org.jmol.util.SB;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.constant.EnumQuantumShell;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.readers.Parameters;

public class MolecularOrbital extends Isosurface {

  @Override
  public void initShape() {
    super.initShape();
    myType = "mo";
    setPropI("thisID", "mo", null);
  }

  // these are globals, stored here and only passed on when the they are needed. 

  private String moTranslucency;
  private Float moTranslucentLevel;
  private Point4f moPlane;
  private Float moCutoff;
  private Float moResolution;
  private Float moScale;
  private Integer moColorPos;
  private Integer moColorNeg;
  private Integer moMonteCarloCount;
  private boolean moIsPositiveOnly;
  private Boolean moSquareData;
  private Boolean moSquareLinear;
  private Integer moRandomSeed;
  private int moFill = T.nofill;
  private int moMesh = T.mesh;
  private int moDots = T.nodots;
  private int moFrontOnly = T.frontonly;
  private String moTitleFormat;
  private boolean moDebug;
  private int myColorPt;
  private String strID;
  private int moNumber;
  private float[] moLinearCombination;
  private Map<String, Map<String, Object>> htModels;
  private Map<String, Object> thisModel;

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    if ("init" == propertyName) {
      myColorPt = 0;
      moDebug = false;
      int modelIndex = ((Integer) value).intValue();
      strID = getId(modelIndex);
      //Logger.info("MO init " + strID);
      // overide bitset selection
      setPropI("init", null, null);
      setPropI("modelIndex", Integer.valueOf(modelIndex), null);
      if (htModels == null)
        htModels = new Hashtable<String, Map<String, Object>>();
      if (!htModels.containsKey(strID))
        htModels.put(strID, new Hashtable<String, Object>());
      thisModel = htModels.get(strID);
      moNumber = (!thisModel.containsKey("moNumber") ? 0 : ((Integer) thisModel
          .get("moNumber")).intValue());
      moLinearCombination = (float[]) thisModel.get("moLinearCombination");
      moSquareData = moSquareLinear = null;
      return;
    }

    if ("slab" == propertyName) {
      if (value instanceof Integer) {
        thisModel.put("slabValue", value);
      } else {
        Object[] slabInfo = (Object[]) value;
        int tok = ((Integer) slabInfo[0]).intValue();
        moSlab = (List<Object>) thisModel.get("slab");
        if (moSlab == null)
          thisModel.put("slab", moSlab = new ArrayList<Object>());
        if (tok == T.none) {
          moSlab = null;
          thisModel.remove("slab");
          return;
        }
        moSlab.add(value);
      }
      return;
    }

    if ("cutoff" == propertyName) {
      thisModel.put("moCutoff", value);
      thisModel.put("moIsPositiveOnly", Boolean.FALSE);
      return;
    }

    if ("scale" == propertyName) {
      thisModel.put("moScale", value);
      return;
    }


    if ("squareData" == propertyName) {
      thisModel.put("moSquareData", Boolean.TRUE);
      moSquareData = Boolean.TRUE;
      return;
    }

    if ("squareLinear" == propertyName) {
      thisModel.put("moSquareLinear", Boolean.TRUE);
      moSquareLinear = Boolean.TRUE;
      return;
    }
    
    if ("cutoffPositive" == propertyName) {
      thisModel.put("moCutoff", value);
      thisModel.put("moIsPositiveOnly", Boolean.TRUE);
      return;
    }

    if ("resolution" == propertyName) {
      thisModel.put("moResolution", value);
      return;
    }

    if ("titleFormat" == propertyName) {
      moTitleFormat = (String) value;
      return;
    }

    if ("color" == propertyName) {
      if (!(value instanceof Integer))
        return;
      thisModel.remove("moTranslucency");
      setPropI("color", value, bs);
      propertyName = "colorRGB";
      myColorPt = 0;
      //$FALL-THROUGH$
    }

    if ("colorRGB" == propertyName) {
      moColorPos = (Integer) value;
      if (myColorPt++ == 0)
        moColorNeg = moColorPos;
      thisModel.put("moColorNeg", moColorNeg);
      thisModel.put("moColorPos", moColorPos);
      return;
    }

    if ("plane" == propertyName) {
      if (value == null)
        thisModel.remove("moPlane");
      else
        thisModel.put("moPlane", value);
      return;
    }

    if ("monteCarloCount" == propertyName) {
      thisModel.put("monteCarloCount", value);
      return;
    }

    if ("randomSeed" == propertyName) {
      if (value == null)
        thisModel.remove("randomSeed");
      else
        thisModel.put("randomSeed", value);
      return;
    }

    if ("molecularOrbital" == propertyName) {
      if (value instanceof Integer) {
        moNumber = ((Integer) value).intValue();
        thisModel.put("moNumber", value);
        thisModel.remove("moLinearCombination");
        moLinearCombination = null;
      } else {
        moNumber = 0;
        moLinearCombination = (float[]) value;
        thisModel.put("moNumber", Integer.valueOf(0));
        thisModel.put("moLinearCombination", moLinearCombination);
      }
      if (moSquareData == Boolean.TRUE)
        thisModel.put("moSquareData", Boolean.TRUE);
      else
        thisModel.remove("moSquareData");
      if (moSquareLinear == Boolean.TRUE)
        thisModel.put("moSquareLinear", Boolean.TRUE);
      else
        thisModel.remove("moSquareLinear");
      setOrbital(moNumber, moLinearCombination);
      return;
    }

    if ("translucentLevel" == propertyName) {
      if (thisModel == null) {
        if (currentMesh == null)
          return;
        thisModel = htModels.get(currentMesh.thisID);
      }
      thisModel.put("moTranslucentLevel", value);
      //pass through
    }

    if ("delete" == propertyName) {
      htModels.remove(strID);
      moNumber = 0;
      moLinearCombination = null;
      //pass through
    }

    if ("token" == propertyName) {
      int tok = ((Integer) value).intValue();
      switch (tok) {
      case T.dots:
      case T.nodots:
        moDots = tok;
        break;
      case T.fill:
      case T.nofill:
        moFill = tok;
        break;
      case T.mesh:
      case T.nomesh:
        moMesh = tok;
        break;
      case T.frontonly:
      case T.notfrontonly:
        moFrontOnly = tok;
        break;
      }
      // pass through  
    }

    if ("translucency" == propertyName) {
      if (thisModel == null) {
        if (currentMesh == null)
          return;
        thisModel = htModels.get(currentMesh.thisID);
      }
      thisModel.put("moTranslucency", value);
      //pass through
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Map<String, Map<String, Object>> htModelsNew = new Hashtable<String, Map<String, Object>>();
      for (int i = meshCount; --i >= 0;) {
        if (meshes[i] == null)
          continue;
        if (meshes[i].modelIndex == modelIndex) {
          meshCount--;
          if (meshes[i] == currentMesh) {
            currentMesh = null;
            thisModel = null;
          }
          meshes = (IsosurfaceMesh[]) ArrayUtil.deleteElements(meshes, i, 1);
          continue;
        }
        Map<String, Object> htModel = htModels.get(meshes[i].thisID);
        if (meshes[i].modelIndex > modelIndex) {
          meshes[i].modelIndex--;
          meshes[i].thisID = getId(meshes[i].modelIndex);
        }
        htModelsNew.put(meshes[i].thisID, htModel);
      }
      htModels = htModelsNew;
      return;
    }
    setPropI(propertyName, value, bs);
  }

  private String getId(int modelIndex) {
    return "mo_model" + viewer.getModelNumberDotted(modelIndex);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getProperty(String propertyName, int param) {
    if (propertyName.equals("list")) {
      String s = (String) getPropI("list");
      if (s.length() > 1)
        s += "cutoff = " + getPropI("cutoff") + "\n";
      return viewer.getMoInfo(-1) + "\n" + s;
    }
    if (propertyName == "moNumber")
      return Integer.valueOf(moNumber);
    if (propertyName == "moLinearCombination")
      return moLinearCombination;
    if (propertyName == "showMO") {
      SB str = new SB();
      List<Map<String, Object>> mos = (List<Map<String, Object>>) (sg.getMoData().get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      int thisMO = param;
      int currentMO = moNumber;
      boolean isShowCurrent = (thisMO == Integer.MIN_VALUE);
      if (thisMO == Integer.MAX_VALUE) {
        thisMO = currentMO;
      }
      if (nOrb == 0 || isShowCurrent && currentMO == 0)
        return "";
      boolean doOneMo = (thisMO != 0);
      if (currentMO == 0)
        thisMO = 0;
      boolean haveHeader = false;
      int nTotal = (thisMO > 0 ? 1 : nOrb);
      int i0 = (nTotal == 1 && currentMO > 0 ? currentMO : 1);
      for (int i = i0; i <= nOrb; i++)
        if (thisMO == 0 || thisMO == i || !doOneMo && i == currentMO) {
          if (!doOneMo) {
            Parameters params = sg.getParams();
            setPropI("init", params, null);
            setOrbital(i, null);
          }
          jvxlData.moleculeXml = viewer.getModelCml(viewer.getModelUndeletedAtomsBitSet(thisMesh.modelIndex), 100, true);
          if (!haveHeader) {
            str.append(JvxlCoder.jvxlGetFile(jvxlData, null, null,
                "HEADERONLY", true, nTotal, null, null));
            haveHeader = true;
          }
          str.append(JvxlCoder.jvxlGetFile(jvxlData, null, jvxlData.title,
              null, false, 1, thisMesh.getState("mo"),
              (thisMesh.scriptCommand == null ? "" : thisMesh.scriptCommand)));
          if (!doOneMo)
            setPropI("delete", "mo_show", null);
          if (nTotal == 1)
            break;
        }
      str.append(JvxlCoder.jvxlGetFile(jvxlData, null, null, "TRAILERONLY", true,
          0, null, null));
      return str.toString();
    }
    return getPropI(propertyName);
  }

  @Override
  protected void clearSg() {
    //sg = null; // not Molecular Orbitals
  }

  private List<Object> moSlab;
  private Integer moSlabValue;
  
  @SuppressWarnings("unchecked")
  private boolean getSettings(String strID) {
    thisModel = htModels.get(strID);
    if (thisModel == null || thisModel.get("moNumber") == null)
      return false;
    moTranslucency = (String) thisModel.get("moTranslucency");
    moTranslucentLevel = (Float) thisModel.get("moTranslucentLevel");
    moPlane = (Point4f) thisModel.get("moPlane");
    moCutoff = (Float) thisModel.get("moCutoff");
    if (moCutoff == null)
      moCutoff = (Float) sg.getMoData().get("defaultCutoff");
    if (moCutoff == null) {
      moCutoff = Float.valueOf(Parameters.defaultQMOrbitalCutoff);
    }
    thisModel.put("moCutoff", Float.valueOf(moCutoff.floatValue()));
    moResolution = (Float) thisModel.get("moResolution");
    moScale = (Float) thisModel.get("moScale");
    moColorPos = (Integer) thisModel.get("moColorPos");
    moColorNeg = (Integer) thisModel.get("moColorNeg");
    moSquareData = (Boolean) thisModel.get("moSquareData");
    moSquareLinear = (Boolean) thisModel.get("moSquareLinear");
    moMonteCarloCount = (Integer) thisModel.get("monteCarloCount");
    moRandomSeed = (Integer) thisModel.get("randomSeed");
    moSlabValue = (Integer)  thisModel.get("slabValue");
    moSlab = (List<Object>) thisModel.get("slab");
    if (moRandomSeed == null)
      thisModel.put("randomSeed", moRandomSeed = Integer.valueOf(
          ((int) -System.currentTimeMillis())% 10000));
    moNumber = ((Integer) thisModel.get("moNumber")).intValue();
    moLinearCombination = (float[]) thisModel.get("moLinearCombination");
    Object b = thisModel.get("moIsPositiveOnly");
    moIsPositiveOnly = (b != null && ((Boolean) (b)).booleanValue());
    return true;
  }

  private void setOrbital(int moNumber, float[] linearCombination) {
    setPropI("reset", strID, null);
    if (moDebug)
      setPropI("debug", Boolean.TRUE, null);
    getSettings(strID);
    if (moScale != null)
      setPropI("scale", moScale, null);
    if (moResolution != null)
      setPropI("resolution", moResolution, null);
    if (moPlane != null) {
      setPropI("plane", moPlane, null);
      if (moCutoff != null) {
        setPropI("red", Float.valueOf(-moCutoff.floatValue()), null);
        setPropI("blue", moCutoff, null);
      }
    } else {
      if (moCutoff != null)
        setPropI((moIsPositiveOnly ? "cutoffPositive" : "cutoff"),
            moCutoff, null);
      if (moColorNeg != null)
        setPropI("colorRGB", moColorNeg, null);
      if (moColorPos != null)
        setPropI("colorRGB", moColorPos, null);
      if (moMonteCarloCount != null) {
        setPropI("randomSeed", moRandomSeed, null);
        setPropI("monteCarloCount", moMonteCarloCount, null);
      }
    }
    setPropI("squareData", moSquareData, null);
    setPropI("squareLinear", moSquareLinear, null);
    setPropI("title", moTitleFormat, null);
    setPropI("fileName", viewer.getFileName(), null);
    setPropI("molecularOrbital", linearCombination == null ? Integer
        .valueOf(moNumber) : linearCombination, null);
    if (moPlane != null && moColorNeg != null)
      setPropI("colorRGB", moColorNeg, null);
    if (moPlane != null && moColorPos != null)
      setPropI("colorRGB", moColorPos, null);
    currentMesh.isColorSolid = false;
    if (moSlabValue != null)
      setPropI("slab", moSlabValue, null);
    if (moSlab != null)
      for (int i = 0; i < moSlab.size(); i++)
        setPropI("slab", moSlab.get(i), null);
    if (moTranslucentLevel != null)
      setPropI("translucenctLevel", moTranslucentLevel, null);
    if (moTranslucency != null)
      setPropI("translucency", moTranslucency, null);
    setPropI("token", Integer.valueOf(moFill), null);
    setPropI("token", Integer.valueOf(moMesh), null);
    setPropI("token", Integer.valueOf(moDots), null);
    setPropI("token", Integer.valueOf(moFrontOnly), null);
    
    thisModel.put("mesh", currentMesh);
    return;
  }

  @Override
  public String getShapeState() {
    if (htModels == null)
      return "";
    SB s = new SB();
    int modelCount = viewer.getModelCount();
    for (int i = 0; i < modelCount; i++)
      s.append(getMoState(i));
    //System.out.println("molecular orbital state " + s.length());
    return s.toString();
  }

  private String getMoState(int modelIndex) {
    strID = getId(modelIndex);
    if (!getSettings(strID))
      return "";
    SB s = new SB();
    int modelCount = viewer.getModelCount();
    if (modelCount > 1)
      appendCmd(s, "frame " + viewer.getModelNumberDotted(modelIndex));
    if (moCutoff != null)
      appendCmd(s, "mo cutoff " + (sg.getIsPositiveOnly() ? "+" : "")
          + moCutoff);
    if (moScale != null)
      appendCmd(s, "mo scale " + moScale);
    if (moMonteCarloCount != null)
      appendCmd(s, "mo points " + moMonteCarloCount + " " + moRandomSeed);
    if (moResolution != null)
      appendCmd(s, "mo resolution " + moResolution);
    if (moPlane != null)
      appendCmd(s, "mo plane {" + moPlane.x + " " + moPlane.y + " " + moPlane.z
          + " " + moPlane.w + "}");
    if (moTitleFormat != null)
      appendCmd(s, "mo titleFormat " + Escape.eS(moTitleFormat));
    //the following is a correct object==object test
    if (moColorNeg != null)
      appendCmd(s, "mo color "
          + Escape.escapeColor(moColorNeg.intValue())
          + (moColorNeg.equals(moColorPos) ? "" : " "
              + Escape.escapeColor(moColorPos.intValue())));
    if (moSlab != null) {
      if (thisMesh.slabOptions != null)
        appendCmd(s, thisMesh.slabOptions.toString());
      if (thisMesh.jvxlData.slabValue != Integer.MIN_VALUE)
        appendCmd(s, "mo slab " + thisMesh.jvxlData.slabValue);
    }
    if (moLinearCombination == null) {
      appendCmd(s, "mo " + (moSquareData == Boolean.TRUE ? "squared ": "") + moNumber);
    } else {
      appendCmd(s, "mo " + EnumQuantumShell.getMOString(moLinearCombination) + (moSquareLinear == Boolean.TRUE ? " squared": ""));
    }
    if (moTranslucency != null)
      appendCmd(s, "mo translucent " + moTranslucentLevel);
    appendCmd(s, ((IsosurfaceMesh) thisModel.get("mesh")).getState("mo"));
    return s.toString();
  }
  
  @Override
  public void merge(Shape shape) {
  MolecularOrbital mo = (MolecularOrbital) shape;
  moColorNeg = mo.moColorNeg;
  moColorPos = mo.moColorPos;
  moCutoff = mo.moCutoff;
  moPlane = mo.moPlane;
  moResolution = mo.moResolution;
  moScale = mo.moScale;
  moSlab = mo.moSlab;
  moSlabValue = mo.moSlabValue;
  moTitleFormat = mo.moTitleFormat;
  moTranslucency = mo.moTranslucency;
  if (htModels == null)
    htModels = new Hashtable<String, Map<String, Object>>();
  Map<String, Map<String, Object>> ht = mo.htModels;
  if (ht != null) {
    for (Map.Entry<String, Map<String, Object>> entry : ht.entrySet()) {
      String key = entry.getKey();
      htModels.put(key, entry.getValue());
    }
  }
  super.merge(shape);
}

}
