/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-05 12:22:08 -0600 (Sun, 05 Mar 2006) $
 * $Revision: 4545 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.scriptext;

import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.script.JmolScriptExtension;
import org.jmol.script.SV;
import org.jmol.script.ScriptEvaluator;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.shape.MeshCollection;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;
import org.jmol.util.P3;
import org.jmol.util.P4;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public class ScriptExt implements JmolScriptExtension {
  private Viewer viewer;
  private ScriptEvaluator eval;
  private ShapeManager sm;
  private boolean chk;
  private String fullCommand;
  private String thisCommand;
  private T[] st;

  final static int ERROR_invalidArgument = 22;

  public ScriptExt() {
    // used by Reflection
  }

  public JmolScriptExtension init(Object se) {
    eval = (ScriptEvaluator) se;
    viewer = eval.viewer;
    sm = eval.sm;
    return this;
  }

  public boolean dispatch(int iShape, boolean b, T[] st) throws ScriptException {
    chk = eval.chk;
    fullCommand = eval.fullCommand;
    thisCommand = eval.thisCommand;
    this.st = st;
    switch (iShape) {
    case JC.SHAPE_CGO:
      return cgo();
    case JC.SHAPE_CONTACT:
      return contact();
    case JC.SHAPE_DIPOLES:
      return dipole();
    case JC.SHAPE_DRAW:
      return draw();
    case JC.SHAPE_ISOSURFACE:
    case JC.SHAPE_PLOT3D:
    case JC.SHAPE_PMESH:
      return isosurface(iShape);
    case JC.SHAPE_LCAOCARTOON:
      return lcaoCartoon();
    case JC.SHAPE_MO:
      return mo(b);
    case JC.SHAPE_POLYHEDRA:
      return polyhedra();
    case JC.SHAPE_STRUTS:
      return struts();
      
    }
    return false;
  }

  private BS atomExpressionAt(int i) throws ScriptException {
    return eval.atomExpressionAt(i);
  }

  private void error(int err) throws ScriptException {
    eval.error(err);
  }

  private void invArg() throws ScriptException {
    error(ScriptEvaluator.ERROR_invalidArgument);
  }

  private void invPO() throws ScriptException {
    error(ScriptEvaluator.ERROR_invalidParameterOrder);
  }
  
  private Object getShapeProperty(int shapeType, String propertyName) {
    return eval.getShapeProperty(shapeType, propertyName);
  }

  private String parameterAsString(int i) throws ScriptException {
    return eval.parameterAsString(i);
  }

  private P3 centerParameter(int i) throws ScriptException {
    return eval.centerParameter(i);
  }

  private float floatParameter(int i) throws ScriptException {
    return eval.floatParameter(i);
  }

  private P3 getPoint3f(int i, boolean allowFractional)
  throws ScriptException {
    return eval.getPoint3f(i, allowFractional);
  }

  private P4 getPoint4f(int i) throws ScriptException {
    return eval.getPoint4f(i);
  }

  private int intParameter(int index) throws ScriptException {
    return eval.intParameter(index);
  }

  private boolean isFloatParameter(int index) {
    return eval.isFloatParameter(index);
  }

  private String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
    return eval.setShapeId(iShape, i, idSeen);
  }

  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    eval.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  private String stringParameter(int index) throws ScriptException {
    return  eval.stringParameter(index);  
  }
  
  private int tokAt(int i) {
    return eval.tokAt(i);
  }

  private boolean cgo() throws ScriptException {
    sm.loadShape(JC.SHAPE_CGO);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CGO))
      return false;
    int iptDisplayProperty = 0;
    String thisId = initIsosurface(JC.SHAPE_CGO);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CGO, "ID") == null);
    boolean isInitialized = false;
    JmolList<Object> data = null;
    float translucentLevel = Float.MAX_VALUE;
    eval.colorArgb[0] = Integer.MIN_VALUE;
    int intScale = 0;
    for (int i = eval.iToken; i < eval.slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (eval.getToken(i).tok) {
      case T.varray:
      case T.leftsquare:
      case T.spacebeforesquare:
        if (data != null || isWild)
          invArg();
        data = eval.listParameter(i, 2, Integer.MAX_VALUE);
        i = eval.iToken;
        continue;
      case T.scale:
        if (++i >= eval.slen)
          error(ScriptEvaluator.ERROR_numberExpected);
        switch (eval.getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ScriptEvaluator.ERROR_numberExpected);
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        translucentLevel = eval.getColorTrans(i, false);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.id:
        thisId = setShapeId(JC.SHAPE_CGO, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CGO, "ID") == null);
        i = eval.iToken;
        break;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CGO, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_CGO, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (data != null && !isInitialized) {
        propertyName = "points";
        propertyValue = Integer.valueOf(intScale);
        isInitialized = true;
        intScale = 0;
      }
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_CGO, propertyName, propertyValue);
    }
    eval.finalizeObject(JC.SHAPE_CGO, eval.colorArgb[0], translucentLevel, intScale,
        data != null, data, iptDisplayProperty, null);
    return true;
  }

  private boolean contact() throws ScriptException {
    sm.loadShape(JC.SHAPE_CONTACT);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CONTACT))
      return false;
    int iptDisplayProperty = 0;
    eval.iToken = 1;
    String thisId = initIsosurface(JC.SHAPE_CONTACT);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
    BS bsA = null;
    BS bsB = null;
    BS bs = null;
    RadiusData rd = null;
    float[] params = null;
    boolean colorDensity = false;
    SB sbCommand = new SB();
    int minSet = Integer.MAX_VALUE;
    int displayType = T.plane;
    int contactType = T.nada;
    float distance = Float.NaN;
    float saProbeRadius = Float.NaN;
    boolean localOnly = true;
    Boolean intramolecular = null;
    Object userSlabObject = null;
    int colorpt = 0;
    boolean colorByType = false;
    int tok;
    boolean okNoAtoms = (eval.iToken > 1);
    for (int i = eval.iToken; i < eval.slen; ++i) {
      switch (tok = eval.getToken(i).tok) {
      // these first do not need atoms defined
      default:
        okNoAtoms = true;
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, 0, eval.theTok)) {
          if (eval.theTok != T.times && !T.tokAttr(eval.theTok, T.identifier))
            invArg();
          thisId = setShapeId(JC.SHAPE_CONTACT, i, idSeen);
          i = eval.iToken;
          break;
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      case T.id:
        okNoAtoms = true;
        setShapeId(JC.SHAPE_CONTACT, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
        i = eval.iToken;
        break;
      case T.color:
        switch (tokAt(i + 1)) {
        case T.density:
          tok = T.nada;
          colorDensity = true;
          sbCommand.append(" color density");
          i++;
          break;
        case T.type:
          tok = T.nada;
          colorByType = true;
          sbCommand.append(" color type");
          i++;
          break;
        }
        if (tok == T.nada)
          break;
        //$FALL-THROUGH$ to translucent
      case T.translucent:
      case T.opaque:
        okNoAtoms = true;
        if (colorpt == 0)
          colorpt = i;
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, i, eval.theTok);
        i = eval.iToken;
        break;
      case T.slab:
        okNoAtoms = true;
        userSlabObject = getCapSlabObject(i, false);
        setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
        i = eval.iToken;
        break;

      // now after this you need atoms

      case T.density:
        colorDensity = true;
        sbCommand.append(" density");
        if (isFloatParameter(i + 1)) {
          if (params == null)
            params = new float[1];
          params[0] = -Math.abs(floatParameter(++i));
          sbCommand.append(" " + -params[0]);
        }
        break;
      case T.resolution:
        float resolution = floatParameter(++i);
        if (resolution > 0) {
          sbCommand.append(" resolution ").appendF(resolution);
          setShapeProperty(JC.SHAPE_CONTACT, "resolution", Float
              .valueOf(resolution));
        }
        break;
      case T.within:
      case T.distance:
        distance = floatParameter(++i);
        sbCommand.append(" within ").appendF(distance);
        break;
      case T.plus:
      case T.integer:
      case T.decimal:
        rd = eval.encodeRadiusParameter(i, false, false);
        sbCommand.append(" ").appendO(rd);
        i = eval.iToken;
        break;
      case T.intermolecular:
      case T.intramolecular:
        intramolecular = (tok == T.intramolecular ? Boolean.TRUE
            : Boolean.FALSE);
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.minset:
        minSet = intParameter(++i);
        break;
      case T.hbond:
      case T.clash:
      case T.vanderwaals:
        contactType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.sasurface:
        if (isFloatParameter(i + 1))
          saProbeRadius = floatParameter(++i);
        //$FALL-THROUGH$
      case T.cap:
      case T.nci:
      case T.surface:
        localOnly = false;
        //$FALL-THROUGH$
      case T.trim:
      case T.full:
      case T.plane:
      case T.connect:
        displayType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        if (tok == T.sasurface)
          sbCommand.append(" ").appendF(saProbeRadius);
        break;
      case T.parameters:
        params = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (isWild || bsB != null)
          invArg();
        bs = BSUtil.copy(atomExpressionAt(i));
        i = eval.iToken;
        if (bsA == null)
          bsA = bs;
        else
          bsB = bs;
        sbCommand.append(" ").append(Escape.eBS(bs));
        break;
      }
      idSeen = (eval.theTok != T.delete);
    }
    if (!okNoAtoms && bsA == null)
      error(ScriptEvaluator.ERROR_endOfStatementUnexpected);
    if (chk)
      return false;

    if (bsA != null) {
      // bond mode, intramolec set here
      if (contactType == T.vanderwaals && rd == null)
        rd = new RadiusData(null, 0, EnumType.OFFSET, EnumVdw.AUTO);
      RadiusData rd1 = (rd == null ? new RadiusData(null, 0.26f,
          EnumType.OFFSET, EnumVdw.AUTO) : rd);
      if (displayType == T.nci && bsB == null && intramolecular != null
          && intramolecular.booleanValue())
        bsB = bsA;
      else
        bsB = eval.setContactBitSets(bsA, bsB, localOnly, distance, rd1, true);
      switch (displayType) {
      case T.cap:
      case T.sasurface:
        BS bsSolvent = eval.lookupIdentifierValue("solvent");
        bsA.andNot(bsSolvent);
        bsB.andNot(bsSolvent);
        bsB.andNot(bsA);
        break;
      case T.surface:
        bsB.andNot(bsA);
        break;
      case T.nci:
        if (minSet == Integer.MAX_VALUE)
          minSet = 100;
        setShapeProperty(JC.SHAPE_CONTACT, "minset", Integer.valueOf(minSet));
        sbCommand.append(" minSet ").appendI(minSet);
        if (params == null)
          params = new float[] { 0.5f, 2 };
      }

      if (intramolecular != null) {
        params = (params == null ? new float[2] : ArrayUtil.ensureLengthA(
            params, 2));
        params[1] = (intramolecular.booleanValue() ? 1 : 2);
      }

      if (params != null)
        sbCommand.append(" parameters ").append(Escape.eAF(params));

      // now adjust for type -- HBOND or HYDROPHOBIC or MISC
      // these are just "standard shortcuts" they are not necessary at all
      setShapeProperty(JC.SHAPE_CONTACT, "set", new Object[] {
          Integer.valueOf(contactType), Integer.valueOf(displayType),
          Boolean.valueOf(colorDensity), Boolean.valueOf(colorByType), bsA,
          bsB, rd, Float.valueOf(saProbeRadius), params, sbCommand.toString() });
      if (colorpt > 0)
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, colorpt, 0);
    }
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, iptDisplayProperty, 0))
        invArg();
    }
    if (userSlabObject != null && bsA != null)
      setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
    if (bsA != null && (displayType == T.nci || localOnly)) {
      Object volume = getShapeProperty(JC.SHAPE_CONTACT, "volume");
      if (Escape.isAD(volume)) {
        double[] vs = (double[]) volume;
        double v = 0;
        for (int i = 0; i < vs.length; i++)
          v += Math.abs(vs[i]);
        volume = Float.valueOf((float) v);
      }
      int nsets = ((Integer) getShapeProperty(JC.SHAPE_CONTACT, "nSets"))
          .intValue();

      if (colorDensity || displayType != T.trim) {
        eval.showString((nsets == 0 ? "" : nsets + " contacts with ")
            + "net volume " + volume + " A^3");
      }
    }
    return true;
  }

  private boolean dipole() throws ScriptException {
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    sm.loadShape(JC.SHAPE_DIPOLES);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_DIPOLES))
      return false;
    setShapeProperty(JC.SHAPE_DIPOLES, "init", null);
    if (eval.slen == 1) {
      setShapeProperty(JC.SHAPE_DIPOLES, "thisID", null);
      return false;
    }
    for (int i = 1; i < eval.slen; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (eval.getToken(i).tok) {
      case T.on:
        propertyName = "on";
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.integer:
      case T.decimal:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(i));
        break;
      case T.bitset:
        propertyName = "atomBitset";
        //$FALL-THROUGH$
      case T.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        iHaveAtoms = true;
        break;
      case T.leftbrace:
      case T.point3f:
        // {X, Y, Z}
        P3 pt = getPoint3f(i, true);
        i = eval.iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case T.bonds:
        propertyName = "bonds";
        break;
      case T.calculate:
        propertyName = "calculate";
        break;
      case T.id:
        setShapeId(JC.SHAPE_DIPOLES, ++i, idSeen);
        i = eval.iToken;
        break;
      case T.cross:
        propertyName = "cross";
        propertyValue = Boolean.TRUE;
        break;
      case T.nocross:
        propertyName = "cross";
        propertyValue = Boolean.FALSE;
        break;
      case T.offset:
        float v = floatParameter(++i);
        if (eval.theTok == T.integer) {
          propertyName = "offsetPercent";
          propertyValue = Integer.valueOf((int) v);
        } else {
          propertyName = "offset";
          propertyValue = Float.valueOf(v);
        }
        break;
      case T.offsetside:
        propertyName = "offsetSide";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;

      case T.val:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.width:
        propertyName = "width";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          setShapeId(JC.SHAPE_DIPOLES, i, idSeen);
          i = eval.iToken;
          break;
        }
        invArg();
      }
      idSeen = (eval.theTok != T.delete && eval.theTok != T.calculate);
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DIPOLES, propertyName, propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JC.SHAPE_DIPOLES, "set", null);
    return true;
  }

  private boolean draw() throws ScriptException {
    sm.loadShape(JC.SHAPE_DRAW);
    switch (tokAt(1)) {
    case T.list:
      if (listIsosurface(JC.SHAPE_DRAW))
        return false;
      break;
    case T.pointgroup:
      eval.pointGroup();
      return false;
    case T.helix:
    case T.quaternion:
    case T.ramachandran:
      plot(st);
      return false;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isIntersect = false;
    boolean isFrame = false;
    P4 plane;
    int tokIntersect = 0;
    float translucentLevel = Float.MAX_VALUE;
    eval.colorArgb[0] = Integer.MIN_VALUE;
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    P3 center = null;
    String thisId = initIsosurface(JC.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
    int[] connections = null;
    int iConnect = 0;
    for (int i = eval.iToken; i < eval.slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (eval.getToken(i).tok) {
      case T.unitcell:
      case T.boundbox:
        if (chk)
          break;
        JmolList<Object> vp = viewer.getPlaneIntersection(eval.theTok, null,
            intScale / 100f, 0);
        intScale = 0;
        propertyName = "polygon";
        propertyValue = vp;
        havePoints = true;
        break;
      case T.connect:
        connections = new int[4];
        iConnect = 4;
        float[] farray = eval.floatParameterSet(++i, 4, 4);
        i = eval.iToken;
        for (int j = 0; j < 4; j++)
          connections[j] = (int) farray[j];
        havePoints = true;
        break;
      case T.bonds:
      case T.atoms:
        if (connections == null
            || iConnect > (eval.theTok == T.bondcount ? 2 : 3)) {
          iConnect = 0;
          connections = new int[] { -1, -1, -1, -1 };
        }
        connections[iConnect++] = atomExpressionAt(++i).nextSetBit(0);
        i = eval.iToken;
        connections[iConnect++] = (eval.theTok == T.bonds ? atomExpressionAt(
            ++i).nextSetBit(0) : -1);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.slab:
        switch (eval.getToken(++i).tok) {
        case T.dollarsign:
          propertyName = "slab";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.intersection:
        switch (eval.getToken(++i).tok) {
        case T.unitcell:
        case T.boundbox:
          tokIntersect = eval.theTok;
          isIntersect = true;
          continue;
        case T.dollarsign:
          propertyName = "intersect";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          isIntersect = true;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.polygon:
        propertyName = "polygon";
        havePoints = true;
        JmolList<Object> v = new JmolList<Object>();
        int nVertices = 0;
        int nTriangles = 0;
        P3[] points = null;
        JmolList<SV> vpolygons = null;
        if (eval.isArrayParameter(++i)) {
          points = eval.getPointArray(i, -1);
          nVertices = points.length;
        } else {
          nVertices = Math.max(0, intParameter(i));
          points = new P3[nVertices];
          for (int j = 0; j < nVertices; j++)
            points[j] = centerParameter(++eval.iToken);
        }
        switch (eval.getToken(++eval.iToken).tok) {
        case T.matrix3f:
        case T.matrix4f:
          SV sv = SV.newScriptVariableToken(eval.theToken);
          sv.toArray();
          vpolygons = sv.getList();
          nTriangles = vpolygons.size();
          break;
        case T.varray:
          vpolygons = ((SV) eval.theToken).getList();
          nTriangles = vpolygons.size();
          break;
        default:
          nTriangles = Math.max(0, intParameter(eval.iToken));
        }
        int[][] polygons = ArrayUtil.newInt2(nTriangles);
        for (int j = 0; j < nTriangles; j++) {
          float[] f = (vpolygons == null ? eval.floatParameterSet(++eval.iToken, 3,
              4) : SV.flistValue(vpolygons.get(j), 0));
          if (f.length < 3 || f.length > 4)
            invArg();
          polygons[j] = new int[] { (int) f[0], (int) f[1], (int) f[2],
              (f.length == 3 ? 7 : (int) f[3]) };
        }
        if (nVertices > 0) {
          v.addLast(points);
          v.addLast(polygons);
        } else {
          v = null;
        }
        propertyValue = v;
        i = eval.iToken;
        break;
      case T.symop:
        String xyz = null;
        int iSym = 0;
        plane = null;
        P3 target = null;
        switch (tokAt(++i)) {
        case T.string:
          xyz = stringParameter(i);
          break;
        case T.matrix4f:
          xyz = SV.sValue(eval.getToken(i));
          break;
        case T.integer:
        default:
          if (!eval.isCenterParameter(i))
            iSym = intParameter(i++);
          if (eval.isCenterParameter(i))
            center = centerParameter(i);
          if (eval.isCenterParameter(eval.iToken + 1))
            target = centerParameter(++eval.iToken);
          if (chk)
            return false;
          i = eval.iToken;
        }
        BS bsAtoms = null;
        if (center == null && i + 1 < eval.slen) {
          center = centerParameter(++i);
          // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
          // so we also check here for the atom set to get the right model
          bsAtoms = (tokAt(i) == T.bitset || tokAt(i) == T.expressionBegin ? atomExpressionAt(i)
              : null);
          i = eval.iToken + 1;
        }
        eval.checkLast(eval.iToken);
        if (!chk)
          eval.runScript((String) viewer.getSymmetryInfo(bsAtoms, xyz, iSym, center,
              target, thisId, T.draw));
        return false;
      case T.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case T.leftbrace:
      case T.point4f:
      case T.point3f:
        // {X, Y, Z}
        if (eval.theTok == T.point4f || !eval.isPoint3f(i)) {
          propertyValue = getPoint4f(i);
          if (isFrame) {
            eval.checkLast(eval.iToken);
            if (!chk)
              eval.runScript((Quaternion.newP4((P4) propertyValue)).draw(
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new P3() : center), intScale / 100f));
            return false;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = eval.iToken;
        havePoints = true;
        break;
      case T.hkl:
      case T.plane:
        if (!havePoints && !isIntersect && tokIntersect == 0
            && eval.theTok != T.hkl) {
          propertyName = "plane";
          break;
        }
        if (eval.theTok == T.plane) {
          plane = eval.planeParameter(++i);
        } else {
          plane = eval.hklParameter(++i);
        }
        i = eval.iToken;
        if (tokIntersect != 0) {
          if (chk)
            break;
          JmolList<Object> vpc = viewer.getPlaneIntersection(tokIntersect,
              plane, intScale / 100f, 0);
          intScale = 0;
          propertyName = "polygon";
          propertyValue = vpc;
        } else {
          propertyValue = plane;
          propertyName = "planedef";
        }
        havePoints = true;
        break;
      case T.linedata:
        propertyName = "lineData";
        propertyValue = eval.floatParameterSet(++i, 0, Integer.MAX_VALUE);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "atomSet";
        propertyValue = atomExpressionAt(i);
        if (isFrame)
          center = centerParameter(i);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.varray:
        propertyName = "modelBasedPoints";
        propertyValue = SV.listValue(eval.theToken);
        havePoints = true;
        break;
      case T.spacebeforesquare:
      case T.comma:
        break;
      case T.leftsquare:
        // [x y] or [x y %]
        propertyValue = eval.xypParameter(i);
        if (propertyValue != null) {
          i = eval.iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          invArg();
        isSavedState = true;
        break;
      case T.rightsquare:
        if (!isSavedState)
          invArg();
        isSavedState = false;
        break;
      case T.reverse:
        propertyName = "reverse";
        break;
      case T.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case T.vector:
        propertyName = "vector";
        break;
      case T.length:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "length";
        break;
      case T.decimal:
        // $drawObject
        propertyValue = Float.valueOf(floatParameter(i));
        propertyName = "length";
        break;
      case T.modelindex:
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = Integer.valueOf(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case T.scale:
        if (++i >= eval.slen)
          error(ScriptEvaluator.ERROR_numberExpected);
        switch (eval.getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ScriptEvaluator.ERROR_numberExpected);
        break;
      case T.id:
        thisId = setShapeId(JC.SHAPE_DRAW, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
        i = eval.iToken;
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case T.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.offset:
        P3 pt = getPoint3f(++i, true);
        i = eval.iToken;
        propertyName = "offset";
        propertyValue = pt;
        break;
      case T.crossed:
        propertyName = "crossed";
        break;
      case T.width:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "width";
        swidth = propertyName + " " + propertyValue;
        break;
      case T.line:
        propertyName = "line";
        propertyValue = Boolean.TRUE;
        break;
      case T.curve:
        propertyName = "curve";
        break;
      case T.arc:
        propertyName = "arc";
        break;
      case T.arrow:
        propertyName = "arrow";
        break;
      case T.circle:
        propertyName = "circle";
        break;
      case T.cylinder:
        propertyName = "cylinder";
        break;
      case T.vertices:
        propertyName = "vertices";
        break;
      case T.nohead:
        propertyName = "nohead";
        break;
      case T.barb:
        propertyName = "isbarb";
        break;
      case T.rotate45:
        propertyName = "rotate45";
        break;
      case T.perpendicular:
        propertyName = "perp";
        break;
      case T.radius:
      case T.diameter:
        boolean isRadius = (eval.theTok == T.radius);
        float f = floatParameter(++i);
        if (isRadius)
          f *= 2;
        propertyValue = Float.valueOf(f);
        propertyName = (isRadius || tokAt(i) == T.decimal ? "width"
            : "diameter");
        swidth = propertyName
            + (tokAt(i) == T.decimal ? " " + f : " " + ((int) f));
        break;
      case T.dollarsign:
        // $drawObject[m]
        if ((tokAt(i + 2) == T.leftsquare || isFrame)) {
          P3 pto = center = centerParameter(i);
          i = eval.iToken;
          propertyName = "coord";
          propertyValue = pto;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = eval.objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        idSeen = true;
        translucentLevel = eval.getColorTrans(i, false);
        i = eval.iToken;
        continue;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_DRAW, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_DRAW, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JC.SHAPE_DRAW, "points", Integer.valueOf(intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        invArg();
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DRAW, propertyName, propertyValue);
    }
    eval.finalizeObject(JC.SHAPE_DRAW, eval.colorArgb[0], translucentLevel, intScale,
        havePoints, connections, iptDisplayProperty, null);
    return true;
  }

  private boolean isosurface(int iShape) throws ScriptException {
    // also called by lcaoCartoon
    sm.loadShape(iShape);
    if (tokAt(1) == T.list && listIsosurface(iShape))
      return false;
    int iptDisplayProperty = 0;
    boolean isIsosurface = (iShape == JC.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JC.SHAPE_PMESH);
    boolean isPlot3d = (iShape == JC.SHAPE_PLOT3D);
    boolean isLcaoCartoon = (iShape == JC.SHAPE_LCAOCARTOON);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean isMapped = false;
    boolean isBicolor = false;
    boolean isPhased = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isCavity = false;
    boolean haveRadius = false;
    boolean toCache = false;
    boolean isFxy = false;
    boolean haveSlab = false;
    boolean haveIntersection = false;
    float[] data = null;
    String cmd = null;
    int thisSetNumber = Integer.MIN_VALUE;
    int nFiles = 0;
    int nX, nY, nZ, ptX, ptY;
    float sigma = Float.NaN;
    float cutoff = Float.NaN;
    int ptWithin = 0;
    Boolean smoothing = null;
    int smoothingPower = Integer.MAX_VALUE;
    BS bs = null;
    BS bsSelect = null;
    BS bsIgnore = null;
    SB sbCommand = new SB();
    P3 pt;
    P4 plane = null;
    P3 lattice = null;
    P3[] pts;
    String str = null;
    int modelIndex = (chk ? 0 : Integer.MIN_VALUE);
    eval.setCursorWait(true);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && getShapeProperty(iShape, "ID") == null);
    boolean isColorSchemeTranslucent = false;
    boolean isInline = false;
    Object onlyOneModel = null;
    String translucency = null;
    String colorScheme = null;
    String mepOrMlp = null;
    short[] discreteColixes = null;
    JmolList<Object[]> propertyList = new JmolList<Object[]>();
    boolean defaultMesh = false;
    if (isPmesh || isPlot3d)
      addShapeProperty(propertyList, "fileType", "Pmesh");

    for (int i = eval.iToken; i < eval.slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      eval.getToken(i);
      if (eval.theTok == T.identifier)
        str = parameterAsString(i);
      switch (eval.theTok) {
      // settings only
      case T.isosurfacepropertysmoothing:
        smoothing = (eval.getToken(++i).tok == T.on ? Boolean.TRUE
            : eval.theTok == T.off ? Boolean.FALSE : null);
        if (smoothing == null)
          invArg();
        continue;
      case T.isosurfacepropertysmoothingpower:
        smoothingPower = intParameter(++i);
        continue;
        // offset, rotate, and scale3d don't need to be saved in sbCommand
        // because they are display properties
      case T.move: // Jmol 13.0.RC2 -- required for state saving after coordinate-based translate/rotate
        propertyName = "moveIsosurface";
        if (tokAt(++i) != T.matrix4f)
          invArg();
        propertyValue = eval.getToken(i++).value;
        break;
      case T.offset:
        propertyName = "offset";
        propertyValue = centerParameter(++i);
        i = eval.iToken;
        break;
      case T.rotate:
        propertyName = "rotate";
        propertyValue = (tokAt(eval.iToken = ++i) == T.none ? null
            : getPoint4f(i));
        i = eval.iToken;
        break;
      case T.scale3d:
        propertyName = "scale3d";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.period:
        sbCommand.append(" periodic");
        propertyName = "periodic";
        break;
      case T.origin:
      case T.step:
      case T.point:
        propertyName = eval.theToken.value.toString();
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyValue = centerParameter(++i);
        sbCommand.append(" ").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.boundbox:
        if (fullCommand.indexOf("# BBOX=") >= 0) {
          String[] bbox = TextFormat.split(Parser.getQuotedAttribute(
              fullCommand, "# BBOX"), ',');
          pts = new P3[] { (P3) Escape.uP(bbox[0]), (P3) Escape.uP(bbox[1]) };
        } else if (eval.isCenterParameter(i + 1)) {
          pts = new P3[] { getPoint3f(i + 1, true),
              getPoint3f(eval.iToken + 1, true) };
          i = eval.iToken;
        } else {
          pts = viewer.getBoundBoxVertices();
        }
        sbCommand.append(" boundBox " + Escape.eP(pts[0]) + " "
            + Escape.eP(pts[pts.length - 1]));
        propertyName = "boundingBox";
        propertyValue = pts;
        break;
      case T.pmesh:
        isPmesh = true;
        sbCommand.append(" pmesh");
        propertyName = "fileType";
        propertyValue = "Pmesh";
        break;
      case T.intersection:
        // isosurface intersection {A} {B} VDW....
        // isosurface intersection {A} {B} function "a-b" VDW....
        bsSelect = atomExpressionAt(++i);
        if (chk) {
          bs = new BS();
        } else if (tokAt(eval.iToken + 1) == T.expressionBegin
            || tokAt(eval.iToken + 1) == T.bitset) {
          bs = atomExpressionAt(++eval.iToken);
          bs.and(viewer.getAtomsWithinRadius(5.0f, bsSelect, false, null));
        } else {
          // default is "within(5.0, selected) and not within(molecule,selected)"
          bs = viewer.getAtomsWithinRadius(5.0f, bsSelect, true, null);
          bs.andNot(viewer.getAtomBits(T.molecule, bsSelect));
        }
        bs.andNot(bsSelect);
        sbCommand.append(" intersection ").append(Escape.eBS(bsSelect)).append(
            " ").append(Escape.eBS(bs));
        i = eval.iToken;
        if (tokAt(i + 1) == T.function) {
          i++;
          String f = (String) eval.getToken(++i).value;
          sbCommand.append(" function ").append(Escape.eS(f));
          if (!chk)
            addShapeProperty(propertyList, "func", (f.equals("a+b")
                || f.equals("a-b") ? f : createFunction("__iso__", "a,b", f)));
        } else {
          haveIntersection = true;
        }
        propertyName = "intersection";
        propertyValue = new BS[] { bsSelect, bs };
        break;
      case T.display:
      case T.within:
        boolean isDisplay = (eval.theTok == T.display);
        if (isDisplay) {
          sbCommand.append(" display");
          iptDisplayProperty = i;
          int tok = tokAt(i + 1);
          if (tok == T.nada)
            continue;
          i++;
          addShapeProperty(propertyList, "token", Integer.valueOf(T.on));
          if (tok == T.bitset || tok == T.all) {
            propertyName = "bsDisplay";
            if (tok == T.all) {
              sbCommand.append(" all");
            } else {
              propertyValue = st[i].value;
              sbCommand.append(" ").append(Escape.eBS((BS) propertyValue));
            }
            eval.checkLast(i);
            break;
          } else if (tok != T.within) {
            eval.iToken = i;
            invArg();
          }
        } else {
          ptWithin = i;
        }
        float distance;
        P3 ptc = null;
        bs = null;
        boolean havePt = false;
        if (tokAt(i + 1) == T.expressionBegin) {
          // within ( x.x , .... )
          distance = floatParameter(i + 3);
          if (eval.isPoint3f(i + 4)) {
            ptc = centerParameter(i + 4);
            havePt = true;
            eval.iToken = eval.iToken + 2;
          } else if (eval.isPoint3f(i + 5)) {
            ptc = centerParameter(i + 5);
            havePt = true;
            eval.iToken = eval.iToken + 2;
          } else {
            bs = eval.atomExpression(st, i + 5, eval.slen, true, false, false, true);
            if (bs == null)
              invArg();
          }
        } else {
          distance = floatParameter(++i);
          ptc = centerParameter(++i);
        }
        if (isDisplay)
          eval.checkLast(eval.iToken);
        i = eval.iToken;
        if (fullCommand.indexOf("# WITHIN=") >= 0)
          bs = Escape.uB(Parser.getQuotedAttribute(fullCommand, "# WITHIN"));
        else if (!havePt)
          bs = (eval.expressionResult instanceof BS ? (BS) eval.expressionResult : null);
        if (!chk) {
          if (bs != null && modelIndex >= 0) {
            bs.and(viewer.getModelUndeletedAtomsBitSet(modelIndex));
          }
          if (ptc == null)
            ptc = viewer.getAtomSetCenter(bs);

          getWithinDistanceVector(propertyList, distance, ptc, bs, isDisplay);
          sbCommand.append(" within ").appendF(distance).append(" ").append(
              bs == null ? Escape.eP(ptc) : Escape.eBS(bs));
        }
        continue;
      case T.parameters:
        propertyName = "parameters";
        // if > 1 parameter, then first is assumed to be the cutoff. 
        float[] fparams = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        propertyValue = fparams;
        sbCommand.append(" parameters ").append(Escape.eAF(fparams));
        break;
      case T.property:
      case T.variable:
        onlyOneModel = eval.theToken.value;
        boolean isVariable = (eval.theTok == T.variable);
        int tokProperty = tokAt(i + 1);
        if (mepOrMlp == null) { // not mlp or mep
          if (!surfaceObjectSeen && !isMapped && !planeSeen) {
            addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
            //if (surfaceObjectSeen)
            sbCommand.append(" vdw");
            surfaceObjectSeen = true;
          }
          propertyName = "property";
          if (smoothing == null) {
            boolean allowSmoothing = true;
            switch (tokProperty) {
            case T.atomindex:
            case T.atomno:
            case T.elemno:
            case T.color:
            case T.resno:
              allowSmoothing = false;
              break;
            }
            smoothing = (allowSmoothing
                && viewer.getIsosurfacePropertySmoothing(false) == 1 ? Boolean.TRUE
                : Boolean.FALSE);
          }
          addShapeProperty(propertyList, "propertySmoothing", smoothing);
          sbCommand.append(" isosurfacePropertySmoothing " + smoothing);
          if (smoothing == Boolean.TRUE) {
            if (smoothingPower == Integer.MAX_VALUE)
              smoothingPower = viewer.getIsosurfacePropertySmoothing(true);
            addShapeProperty(propertyList, "propertySmoothingPower", Integer
                .valueOf(smoothingPower));
            sbCommand.append(" isosurfacePropertySmoothingPower "
                + smoothingPower);
          }
          if (viewer.global.rangeSelected)
            addShapeProperty(propertyList, "rangeSelected", Boolean.TRUE);
        } else {
          propertyName = mepOrMlp;
        }
        str = parameterAsString(i);
        //        if (surfaceObjectSeen)
        sbCommand.append(" ").append(str);

        if (str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (chk)
            continue;
          data = viewer.getDataFloat(str);
          if (data == null)
            invArg();
          addShapeProperty(propertyList, propertyName, data);
          continue;
        }

        int atomCount = viewer.getAtomCount();
        data = new float[atomCount];

        if (isVariable) {
          String vname = parameterAsString(++i);
          if (vname.length() == 0) {
            data = eval.floatParameterSet(i, atomCount, atomCount);
          } else {
            data = new float[atomCount];
            if (!chk)
              Parser.parseStringInfestedFloatArray(""
                  + eval.getParameter(vname, T.string), null, data);
          }
          if (!chk/* && (surfaceObjectSeen)*/)
            sbCommand.append(" \"\" ").append(Escape.eAF(data));
        } else {
          eval.getToken(++i);
          if (!chk) {
            sbCommand.append(" " + eval.theToken.value);
            Atom[] atoms = viewer.modelSet.atoms;
            viewer.autoCalculate(tokProperty);
            if (tokProperty != T.color)
              for (int iAtom = atomCount; --iAtom >= 0;)
                data[iAtom] = Atom.atomPropertyFloat(viewer, atoms[iAtom],
                    tokProperty);
          }
          if (tokProperty == T.color)
            colorScheme = "inherit";
          if (tokAt(i + 1) == T.within) {
            float d = floatParameter(i = i + 2);
            sbCommand.append(" within " + d);
            addShapeProperty(propertyList, "propertyDistanceMax", Float
                .valueOf(d));
          }
        }
        propertyValue = data;
        break;
      case T.modelindex:
      case T.model:
        if (surfaceObjectSeen)
          invArg();
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i)
            : eval.modelNumberParameter(++i));
        sbCommand.append(" modelIndex " + modelIndex);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.select:
        // in general, viewer.getCurrentSelection() is used, but we may
        // override that here. But we have to be careful that
        // we PREPEND the selection to the command if no surface object
        // has been seen yet, and APPEND it if it has.
        propertyName = "select";
        BS bs1 = atomExpressionAt(++i);
        propertyValue = bs1;
        i = eval.iToken;
        boolean isOnly = (tokAt(i + 1) == T.only);
        if (isOnly) {
          i++;
          BS bs2 = BSUtil.copy(bs1);
          BSUtil.invertInPlace(bs2, viewer.getAtomCount());
          addShapeProperty(propertyList, "ignore", bs2);
          sbCommand.append(" ignore ").append(Escape.eBS(bs2));
        }
        if (surfaceObjectSeen || isMapped) {
          sbCommand.append(" select " + Escape.eBS(bs1));
        } else {
          bsSelect = (BS) propertyValue;
          if (modelIndex < 0 && bsSelect.nextSetBit(0) >= 0)
            modelIndex = viewer.getAtomModelIndex(bsSelect.nextSetBit(0));
        }
        break;
      case T.set:
        thisSetNumber = intParameter(++i);
        break;
      case T.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        sbCommand.append(" center " + Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.sign:
      case T.color:
        int color;
        idSeen = true;
        boolean isSign = (eval.theTok == T.sign);
        if (isSign) {
          sbCommand.append(" sign");
          addShapeProperty(propertyList, "sign", Boolean.TRUE);
        } else {
          if (tokAt(i + 1) == T.density) {
            i++;
            propertyName = "colorDensity";
            sbCommand.append(" color density");
            if (isFloatParameter(i + 1)) {
              float ptSize = floatParameter(++i);
              sbCommand.append(" " + ptSize);
              propertyValue = Float.valueOf(ptSize);
            }
            break;
          }
          /*
           * "color" now is just used as an equivalent to "sign" and as an
           * introduction to "absolute" any other use is superfluous; it has
           * been replaced with MAP for indicating "use the current surface"
           * because the term COLOR is too general.
           */

          if (eval.getToken(i + 1).tok == T.string) {
            colorScheme = parameterAsString(++i);
            if (colorScheme.indexOf(" ") > 0) {
              discreteColixes = C.getColixArray(colorScheme);
              if (discreteColixes == null)
                error(ScriptEvaluator.ERROR_badRGBColor);
            }
          } else if (eval.theTok == T.mesh) {
            i++;
            sbCommand.append(" color mesh");
            color = eval.getArgbParam(++i);
            addShapeProperty(propertyList, "meshcolor", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            i = eval.iToken;
            continue;
          }
          if ((eval.theTok = tokAt(i + 1)) == T.translucent
              || eval.theTok == T.opaque) {
            sbCommand.append(" color");
            translucency = setColorOptions(sbCommand, i + 1,
                JC.SHAPE_ISOSURFACE, -2);
            i = eval.iToken;
            continue;
          }
          switch (tokAt(i + 1)) {
          case T.absolute:
          case T.range:
            eval.getToken(++i);
            sbCommand.append(" color range");
            addShapeProperty(propertyList, "rangeAll", null);
            if (tokAt(i + 1) == T.all) {
              i++;
              sbCommand.append(" all");
              continue;
            }
            float min = floatParameter(++i);
            float max = floatParameter(++i);
            addShapeProperty(propertyList, "red", Float.valueOf(min));
            addShapeProperty(propertyList, "blue", Float.valueOf(max));
            sbCommand.append(" ").appendF(min).append(" ").appendF(max);
            continue;
          }
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(i + 1);
            if (tokAt(i + 2) == T.to) {
              colorScheme = eval.getColorRange(i + 1);
              i = eval.iToken;
              break;
            }
          }
          sbCommand.append(" color");
        }
        if (eval.isColorParam(i + 1)) {
          color = eval.getArgbParam(++i);
          sbCommand.append(" ").append(Escape.escapeColor(color));
          i = eval.iToken;
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
          idSeen = true;
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(++i);
            i = eval.iToken;
            addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            isBicolor = true;
          } else if (isSign) {
            invPO();
          }
        } else if (!isSign && discreteColixes == null) {
          invPO();
        }
        continue;
      case T.cache:
        if (!isIsosurface)
          invArg();
        toCache = !chk;
        continue;
      case T.file:
        if (tokAt(i + 1) != T.string)
          invPO();
        continue;
      case T.ionic:
      case T.vanderwaals:
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        RadiusData rd = eval.encodeRadiusParameter(i, false, true);
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(rd);
        if (Float.isNaN(rd.value))
          rd.value = 100;
        propertyValue = rd;
        propertyName = "radius";
        haveRadius = true;
        if (isMapped)
          surfaceObjectSeen = false;
        i = eval.iToken;
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.planeParameter(++i);
        i = eval.iToken;
        //if (surfaceObjectSeen)
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" scale ").appendO(propertyValue);
        break;
      case T.all:
        if (idSeen)
          invArg();
        propertyName = "thisID";
        break;
      case T.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // NOT OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        //        ignoreError = true;
        //      try {
        propertyValue = getPoint4f(i);
        propertyName = "ellipsoid";
        i = eval.iToken;
        sbCommand.append(" ellipsoid ").append(Escape.eP4((P4) propertyValue));
        break;
      //        } catch (Exception e) {
      //        }
      //        try {
      //          propertyName = "ellipsoid";
      //          propertyValue = eval.floatParameterSet(i, 6, 6);
      //          i = eval.iToken;
      //          sbCommand.append(" ellipsoid ").append(
      //              Escape.eAF((float[]) propertyValue));
      //          break;
      //        } catch (Exception e) {
      //        }
      //        ignoreError = false;
      //        bs = atomExpressionAt(i);
      //        sbCommand.append(" ellipsoid ").append(Escape.eBS(bs));
      //        int iAtom = bs.nextSetBit(0);
      //        if (iAtom < 0)
      //          return;
      //        Atom[] atoms = viewer.modelSet.atoms;
      //        Tensor[] tensors = atoms[iAtom].getTensors();
      //        if (tensors == null || tensors.length < 1 || tensors[0] == null
      //            || (propertyValue = viewer.getQuadricForTensor(tensors[0], null)) == null)
      //          return;
      //        i = eval.iToken;
      //        propertyName = "ellipsoid";
      //        if (!chk)
      //          addShapeProperty(propertyList, "center", viewer.getAtomPoint3f(iAtom));
      //        break;
      case T.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.hklParameter(++i);
        i = eval.iToken;
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        addShapeProperty(propertyList, "lcaoType", lcaoType);
        sbCommand.append(" lcaocartoon ").append(Escape.eS(lcaoType));
        switch (eval.getToken(++i).tok) {
        case T.bitset:
        case T.expressionBegin:
          // automatically selects just the model of the first atom in the set.
          propertyName = "lcaoCartoon";
          bs = atomExpressionAt(i);
          i = eval.iToken;
          if (chk)
            continue;
          int atomIndex = bs.nextSetBit(0);
          if (atomIndex < 0)
            error(ScriptEvaluator.ERROR_expressionExpected);
          sbCommand.append(" ({").appendI(atomIndex).append("})");
          modelIndex = viewer.getAtomModelIndex(atomIndex);
          addShapeProperty(propertyList, "modelIndex", Integer
              .valueOf(modelIndex));
          V3[] axes = { new V3(), new V3(),
              V3.newV(viewer.getAtomPoint3f(atomIndex)), new V3() };
          if (!lcaoType.equalsIgnoreCase("s")
              && viewer.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                  lcaoType) == null)
            return false;
          propertyValue = axes;
          break;
        default:
          error(ScriptEvaluator.ERROR_expressionExpected);
        }
        break;
      case T.mo:
        // mo 1-based-index
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        boolean isNegOffset = (tokAt(i + 1) == T.minus);
        if (isNegOffset)
          i++;
        float[] linearCombination = null;
        switch (tokAt(++i)) {
        case T.nada:
          error(ScriptEvaluator.ERROR_badArgumentCount);
          break;
        case T.density:
          sbCommand.append("mo [1] squared ");
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          linearCombination = new float[] { 1 };
          offset = moNumber = 0;
          i++;
          break;
        case T.homo:
        case T.lumo:
          offset = moOffset(i);
          moNumber = 0;
          i = eval.iToken;
          //if (surfaceObjectSeen) {
          sbCommand.append(" mo " + (isNegOffset ? "-" : "") + "HOMO ");
          if (offset > 0)
            sbCommand.append("+");
          if (offset != 0)
            sbCommand.appendI(offset);
          //}
          break;
        case T.integer:
          moNumber = intParameter(i);
          //if (surfaceObjectSeen)
          sbCommand.append(" mo ").appendI(moNumber);
          break;
        default:
          if (eval.isArrayParameter(i)) {
            linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
            i = eval.iToken;
          }
        }
        boolean squared = (tokAt(i + 1) == T.squared);
        if (squared) {
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          sbCommand.append(" squared");
          if (linearCombination == null)
            linearCombination = new float[0];
        } else if (tokAt(i + 1) == T.point) {
          ++i;
          int monteCarloCount = intParameter(++i);
          int seed = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          addShapeProperty(propertyList, "monteCarloCount", Integer
              .valueOf(monteCarloCount));
          addShapeProperty(propertyList, "randomSeed", Integer.valueOf(seed));
          sbCommand.append(" points ").appendI(monteCarloCount).appendC(' ')
              .appendI(seed);
        }
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.nci:
        propertyName = "nci";
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        int tok = tokAt(i + 1);
        boolean isPromolecular = (tok != T.file && tok != T.string && tok != T.mrc);
        propertyValue = Boolean.valueOf(isPromolecular);
        if (isPromolecular)
          surfaceObjectSeen = true;
        break;
      case T.mep:
      case T.mlp:
        boolean isMep = (eval.theTok == T.mep);
        propertyName = (isMep ? "mep" : "mlp");
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        String fname = null;
        int calcType = -1;
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == T.integer) {
          calcType = intParameter(++i);
          sbCommand.append(" " + calcType);
          addShapeProperty(propertyList, "mepCalcType", Integer
              .valueOf(calcType));
        }
        if (tokAt(i + 1) == T.string) {
          fname = stringParameter(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" /*file*/" + Escape.eS(fname));
        } else if (tokAt(i + 1) == T.property) {
          mepOrMlp = propertyName;
          continue;
        }
        if (!chk)
          try {
            data = (fname == null && isMep ? viewer.getPartialCharges()
                : viewer.getAtomicPotentials(isMep, bsSelect, bsIgnore, fname));
          } catch (Exception e) {
            // ignore
          }
        if (!chk && data == null)
          error(ScriptEvaluator.ERROR_noPartialCharges);
        propertyValue = data;
        break;
      case T.volume:
        doCalcVolume = !chk;
        sbCommand.append(" volume");
        break;
      case T.id:
        setShapeId(iShape, ++i, idSeen);
        isWild = (getShapeProperty(iShape, "ID") == null);
        i = eval.iToken;
        break;
      case T.colorscheme:
        // either order NOT OK -- documented for TRANSLUCENT "rwb"
        if (tokAt(i + 1) == T.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        colorScheme = parameterAsString(++i).toLowerCase();
        if (colorScheme.equals("sets")) {
          sbCommand.append(" colorScheme \"sets\"");
        } else if (eval.isColorParam(i)) {
          colorScheme = eval.getColorRange(i);
          i = eval.iToken;
        }
        break;
      case T.addhydrogens:
        propertyName = "addHydrogens";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" addHydrogens");
        break;
      case T.angstroms:
        propertyName = "angstroms";
        sbCommand.append(" angstroms");
        break;
      case T.anisotropy:
        propertyName = "anisotropy";
        propertyValue = getPoint3f(++i, false);
        sbCommand.append(" anisotropy").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.area:
        doCalcArea = !chk;
        sbCommand.append(" area");
        break;
      case T.atomicorbital:
      case T.orbital:
        surfaceObjectSeen = true;
        if (isBicolor && !isPhased) {
          sbCommand.append(" phase \"_orb\"");
          addShapeProperty(propertyList, "phase", "_orb");
        }
        float[] nlmZprs = new float[7];
        nlmZprs[0] = intParameter(++i);
        nlmZprs[1] = intParameter(++i);
        nlmZprs[2] = intParameter(++i);
        nlmZprs[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
        //if (surfaceObjectSeen)
        sbCommand.append(" atomicOrbital ").appendI((int) nlmZprs[0]).append(
            " ").appendI((int) nlmZprs[1]).append(" ")
            .appendI((int) nlmZprs[2]).append(" ").appendF(nlmZprs[3]);
        if (tokAt(i + 1) == T.point) {
          i += 2;
          nlmZprs[4] = intParameter(i);
          nlmZprs[5] = (tokAt(i + 1) == T.decimal ? floatParameter(++i) : 0);
          nlmZprs[6] = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          //if (surfaceObjectSeen)
          sbCommand.append(" points ").appendI((int) nlmZprs[4]).appendC(' ')
              .appendF(nlmZprs[5]).appendC(' ').appendI((int) nlmZprs[6]);
        }
        propertyName = "hydrogenOrbital";
        propertyValue = nlmZprs;
        break;
      case T.binary:
        sbCommand.append(" binary");
        // for PMESH, specifically
        // ignore for now
        continue;
      case T.blockdata:
        sbCommand.append(" blockData");
        propertyName = "blockData";
        propertyValue = Boolean.TRUE;
        break;
      case T.cap:
      case T.slab:
        haveSlab = true;
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.cavity:
        if (!isIsosurface)
          invArg();
        isCavity = true;
        if (chk)
          continue;
        float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 1.2f);
        float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 10f);
        if (envelopeRadius > 10f)
          eval.integerOutOfRange(0, 10);
        sbCommand.append(" cavity ").appendF(cavityRadius).append(" ").appendF(
            envelopeRadius);
        addShapeProperty(propertyList, "envelopeRadius", Float
            .valueOf(envelopeRadius));
        addShapeProperty(propertyList, "cavityRadius", Float
            .valueOf(cavityRadius));
        propertyName = "cavity";
        break;
      case T.contour:
      case T.contours:
        propertyName = "contour";
        sbCommand.append(" contour");
        switch (tokAt(i + 1)) {
        case T.discrete:
          propertyValue = eval.floatParameterSet(i + 2, 1, Integer.MAX_VALUE);
          sbCommand.append(" discrete ").append(
              Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
          break;
        case T.increment:
          pt = getPoint3f(i + 2, false);
          if (pt.z <= 0 || pt.y < pt.x)
            invArg(); // from to step
          if (pt.z == (int) pt.z && pt.z > (pt.y - pt.x))
            pt.z = (pt.y - pt.x) / pt.z;
          propertyValue = pt;
          i = eval.iToken;
          sbCommand.append(" increment ").append(Escape.eP(pt));
          break;
        default:
          propertyValue = Integer
              .valueOf(tokAt(i + 1) == T.integer ? intParameter(++i) : 0);
          sbCommand.append(" ").appendO(propertyValue);
        }
        break;
      case T.decimal:
      case T.integer:
      case T.plus:
      case T.cutoff:
        sbCommand.append(" cutoff ");
        if (eval.theTok == T.cutoff)
          i++;
        if (tokAt(i) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(cutoff = floatParameter(++i));
          sbCommand.append("+").appendO(propertyValue);
        } else if (isFloatParameter(i)) {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(cutoff = floatParameter(i));
          sbCommand.appendO(propertyValue);
        } else {
          propertyName = "cutoffRange";
          propertyValue = eval.floatParameterSet(i, 2, 2);
          addShapeProperty(propertyList, "cutoff", Float.valueOf(0));
          sbCommand.append(Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
        }
        break;
      case T.downsample:
        propertyName = "downsample";
        propertyValue = Integer.valueOf(intParameter(++i));
        //if (surfaceObjectSeen)
        sbCommand.append(" downsample ").appendO(propertyValue);
        break;
      case T.eccentricity:
        propertyName = "eccentricity";
        propertyValue = getPoint4f(++i);
        //if (surfaceObjectSeen)
        sbCommand.append(" eccentricity ").append(
            Escape.eP4((P4) propertyValue));
        i = eval.iToken;
        break;
      case T.ed:
        sbCommand.append(" ed");
        // electron density - never documented
        setMoData(propertyList, -1, null, 0, false, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.debug:
      case T.nodebug:
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "debug";
        propertyValue = (eval.theTok == T.debug ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.fixed:
        sbCommand.append(" fixed");
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.fullplane:
        sbCommand.append(" fullPlane");
        propertyName = "fullPlane";
        propertyValue = Boolean.TRUE;
        break;
      case T.functionxy:
      case T.functionxyz:
        // isosurface functionXY "functionName"|"data2d_xxxxx"
        // isosurface functionXYZ "functionName"|"data3d_xxxxx"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        // or
        // isosurface origin.. step... count... functionXY[Z] = "x + y + z"
        boolean isFxyz = (eval.theTok == T.functionxyz);
        propertyName = "" + eval.theToken.value;
        JmolList<Object> vxy = new JmolList<Object>();
        propertyValue = vxy;
        isFxy = surfaceObjectSeen = true;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").append(propertyName);
        String name = parameterAsString(++i);
        if (name.equals("=")) {
          //if (surfaceObjectSeen)
          sbCommand.append(" =");
          name = parameterAsString(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" ").append(Escape.eS(name));
          vxy.addLast(name);
          if (!chk)
            addShapeProperty(propertyList, "func", createFunction("__iso__",
                "x,y,z", name));
          //surfaceObjectSeen = true;
          break;
        }
        // override of function or data name when saved as a state
        String dName = Parser.getQuotedAttribute(fullCommand, "# DATA"
            + (isFxy ? "2" : ""));
        if (dName == null)
          dName = "inline";
        else
          name = dName;
        boolean isXYZ = (name.indexOf("data2d_") == 0);
        boolean isXYZV = (name.indexOf("data3d_") == 0);
        isInline = name.equals("inline");
        //if (!surfaceObjectSeen)
        sbCommand.append(" inline");
        vxy.addLast(name); // (0) = name
        P3 pt3 = getPoint3f(++i, false);
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP(pt3));
        vxy.addLast(pt3); // (1) = {origin}
        P4 pt4;
        ptX = ++eval.iToken;
        vxy.addLast(pt4 = getPoint4f(ptX)); // (2) = {ni ix iy iz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nX = (int) pt4.x;
        ptY = ++eval.iToken;
        vxy.addLast(pt4 = getPoint4f(ptY)); // (3) = {nj jx jy jz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nY = (int) pt4.x;
        vxy.addLast(pt4 = getPoint4f(++eval.iToken)); // (4) = {nk kx ky kz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nZ = (int) pt4.x;

        if (nX == 0 || nY == 0 || nZ == 0)
          invArg();
        if (!chk) {
          float[][] fdata = null;
          float[][][] xyzdata = null;
          if (isFxyz) {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              nZ = Math.abs(nZ);
              xyzdata = floatArraySetXYZ(++eval.iToken, nX, nY, nZ);
            } else if (isXYZV) {
              xyzdata = viewer.getDataFloat3D(name);
            } else {
              xyzdata = viewer.functionXYZ(name, nX, nY, nZ);
            }
            nX = Math.abs(nX);
            nY = Math.abs(nY);
            nZ = Math.abs(nZ);
            if (xyzdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptEvaluator.ERROR_what, "xyzdata is null.");
            }
            if (xyzdata.length != nX || xyzdata[0].length != nY
                || xyzdata[0][0].length != nZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptEvaluator.ERROR_what, "xyzdata[" + xyzdata.length + "]["
                  + xyzdata[0].length + "][" + xyzdata[0][0].length
                  + "] is not of size [" + nX + "][" + nY + "][" + nZ + "]");
            }
            vxy.addLast(xyzdata); // (5) = float[][][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(xyzdata));
          } else {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              fdata = floatArraySet(++eval.iToken, nX, nY);
            } else if (isXYZ) {
              fdata = viewer.getDataFloat2D(name);
              nX = (fdata == null ? 0 : fdata.length);
              nY = 3;
            } else {
              fdata = viewer.functionXY(name, nX, nY);
              nX = Math.abs(nX);
              nY = Math.abs(nY);
            }
            if (fdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptEvaluator.ERROR_what, "fdata is null.");
            }
            if (fdata.length != nX && !isXYZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptEvaluator.ERROR_what, "fdata length is not correct: "
                  + fdata.length + " " + nX + ".");
            }
            for (int j = 0; j < nX; j++) {
              if (fdata[j] == null) {
                eval.iToken = ptY;
                eval.errorStr(ScriptEvaluator.ERROR_what, "fdata[" + j + "] is null.");
              }
              if (fdata[j].length != nY) {
                eval.iToken = ptY;
                eval.errorStr(ScriptEvaluator.ERROR_what, "fdata[" + j
                    + "] is not the right length: " + fdata[j].length + " "
                    + nY + ".");
              }
            }
            vxy.addLast(fdata); // (5) = float[][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(fdata));
          }
        }
        i = eval.iToken;
        break;
      case T.gridpoints:
        propertyName = "gridPoints";
        sbCommand.append(" gridPoints");
        break;
      case T.ignore:
        propertyName = "ignore";
        propertyValue = bsIgnore = atomExpressionAt(++i);
        sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
        i = eval.iToken;
        break;
      case T.insideout:
        propertyName = "insideOut";
        sbCommand.append(" insideout");
        break;
      case T.internal:
      case T.interior:
      case T.pocket:
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "pocket";
        propertyValue = (eval.theTok == T.pocket ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.lobe:
        // lobe {eccentricity}
        propertyName = "lobe";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lobe ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.lonepair:
      case T.lp:
        // lp {eccentricity}
        propertyName = "lp";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lp ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.mapProperty:
        if (isMapped || eval.slen == i + 1)
          invArg();
        isMapped = true;
        if ((isCavity || haveRadius || haveIntersection) && !surfaceObjectSeen) {
          surfaceObjectSeen = true;
          addShapeProperty(propertyList, "bsSolvent",
              (haveRadius || haveIntersection ? new BS()
                  : eval.lookupIdentifierValue("solvent")));
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        if (sbCommand.length() == 0) {
          plane = (P4) getShapeProperty(JC.SHAPE_ISOSURFACE, "plane");
          if (plane == null) {
            if (getShapeProperty(JC.SHAPE_ISOSURFACE, "contours") != null) {
              addShapeProperty(propertyList, "nocontour", null);
            }
          } else {
            addShapeProperty(propertyList, "plane", plane);
            sbCommand.append("plane ").append(Escape.eP4(plane));
            planeSeen = true;
            plane = null;
          }
        } else if (!surfaceObjectSeen && !planeSeen) {
          invArg();
        }
        sbCommand.append("; isosurface map");
        addShapeProperty(propertyList, "map", (surfaceObjectSeen ? Boolean.TRUE
            : Boolean.FALSE));
        break;
      case T.maxset:
        propertyName = "maxset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" maxSet ").appendO(propertyValue);
        break;
      case T.minset:
        propertyName = "minset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" minSet ").appendO(propertyValue);
        break;
      case T.radical:
        // rad {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "rad";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" radical ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        sbCommand.append(" modelBased");
        break;
      case T.molecular:
      case T.sasurface:
      case T.solvent:
        onlyOneModel = eval.theToken.value;
        float radius;
        if (eval.theTok == T.molecular) {
          propertyName = "molecular";
          sbCommand.append(" molecular");
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : 1.4f);
        } else {
          addShapeProperty(propertyList, "bsSolvent",
              eval.lookupIdentifierValue("solvent"));
          propertyName = (eval.theTok == T.sasurface ? "sasurface" : "solvent");
          sbCommand.append(" ").appendO(eval.theToken.value);
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer
              .getFloat(T.solventproberadius));
        }
        sbCommand.append(" ").appendF(radius);
        propertyValue = Float.valueOf(radius);
        if (tokAt(i + 1) == T.full) {
          addShapeProperty(propertyList, "doFullMolecular", null);
          //if (!surfaceObjectSeen)
          sbCommand.append(" full");
          i++;
        }
        surfaceObjectSeen = true;
        break;
      case T.mrc:
        addShapeProperty(propertyList, "fileType", "Mrc");
        sbCommand.append(" mrc");
        continue;
      case T.object:
      case T.obj:
        addShapeProperty(propertyList, "fileType", "Obj");
        sbCommand.append(" obj");
        continue;
      case T.msms:
        addShapeProperty(propertyList, "fileType", "Msms");
        sbCommand.append(" msms");
        continue;
      case T.phase:
        if (surfaceObjectSeen)
          invArg();
        propertyName = "phase";
        isPhased = true;
        propertyValue = (tokAt(i + 1) == T.string ? stringParameter(++i)
            : "_orb");
        sbCommand.append(" phase ").append(Escape.eS((String) propertyValue));
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" resolution ").appendO(propertyValue);
        break;
      case T.reversecolor:
        propertyName = "reverseColor";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" reversecolor");
        break;
      case T.sigma:
        propertyName = "sigma";
        propertyValue = Float.valueOf(sigma = floatParameter(++i));
        sbCommand.append(" sigma ").appendO(propertyValue);
        break;
      case T.geosurface:
        // geosurface [radius]
        propertyName = "geodesic";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" geosurface ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.sphere:
        // sphere [radius]
        propertyName = "sphere";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" sphere ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" squared");
        break;
      case T.inline:
        propertyName = (!surfaceObjectSeen && !planeSeen && !isMapped ? "readFile"
            : "mapColor");
        str = stringParameter(++i);
        if (str == null)
          invArg();
        // inline PMESH data
        if (isPmesh)
          str = TextFormat.replaceAllCharacter(str, "{,}|", ' ');
        if (eval.logMessages)
          Logger.debug("pmesh inline data:\n" + str);
        propertyValue = (chk ? null : str);
        addShapeProperty(propertyList, "fileName", "");
        sbCommand.append(" INLINE ").append(Escape.eS(str));
        surfaceObjectSeen = true;
        break;
      case T.string:
        boolean firstPass = (!surfaceObjectSeen && !planeSeen);
        propertyName = (firstPass && !isMapped ? "readFile" : "mapColor");
        String filename = parameterAsString(i);
        /*
         * A file name, optionally followed by a calculation type and/or an integer file index.
         * Or =xxxx, an EDM from Uppsala Electron Density Server
         * If the model auxiliary info has "jmolSufaceInfo", we use that.
         */
        if (filename.startsWith("=") && filename.length() > 1) {
          String[] info = (String[]) viewer.setLoadFormat(filename, '_', false);
          filename = info[0];
          String strCutoff = (!firstPass || !Float.isNaN(cutoff) ? null
              : info[1]);
          if (strCutoff != null && !chk) {
            cutoff = SV.fValue(SV.getVariable(viewer
                .evaluateExpression(strCutoff)));
            if (cutoff > 0) {
              if (!Float.isNaN(sigma)) {
                cutoff *= sigma;
                sigma = Float.NaN;
                addShapeProperty(propertyList, "sigma", Float.valueOf(sigma));
              }
              addShapeProperty(propertyList, "cutoff", Float.valueOf(cutoff));
              sbCommand.append(" cutoff ").appendF(cutoff);
            }
          }
          if (ptWithin == 0) {
            onlyOneModel = "=xxxx";
            if (modelIndex < 0)
              modelIndex = viewer.getCurrentModelIndex();
            bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
            getWithinDistanceVector(propertyList, 2.0f, null, bs, false);
            sbCommand.append(" within 2.0 ").append(Escape.eBS(bs));
          }
          if (firstPass)
            defaultMesh = true;
        }

        if (firstPass && viewer.getParameter("_fileType").equals("Pdb")
            && Float.isNaN(sigma) && Float.isNaN(cutoff)) {
          // negative sigma just indicates that 
          addShapeProperty(propertyList, "sigma", Float.valueOf(-1));
          sbCommand.append(" sigma -1.0");
        }
        if (filename.length() == 0) {
          if (modelIndex < 0)
            modelIndex = viewer.getCurrentModelIndex();
          filename = eval.getFullPathName();
          propertyValue = viewer.getModelAuxiliaryInfoValue(modelIndex,
              "jmolSurfaceInfo");
        }
        int fileIndex = -1;
        if (propertyValue == null && tokAt(i + 1) == T.integer)
          addShapeProperty(propertyList, "fileIndex", Integer
              .valueOf(fileIndex = intParameter(++i)));
        String stype = (tokAt(i + 1) == T.string ? stringParameter(++i) : null);
        // done reading parameters
        surfaceObjectSeen = true;
        if (chk) {
          break;
        }
        String[] fullPathNameOrError;
        String localName = null;
        if (propertyValue == null) {
          if (fullCommand.indexOf("# FILE" + nFiles + "=") >= 0) {
            // old way, abandoned
            filename = Parser
                .getQuotedAttribute(fullCommand, "# FILE" + nFiles);
            if (tokAt(i + 1) == T.as)
              i += 2; // skip that
          } else if (tokAt(i + 1) == T.as) {
            localName = viewer.getFilePath(
                stringParameter(eval.iToken = (i = i + 2)), false);
            fullPathNameOrError = viewer.getFullPathNameOrError(localName);
            localName = fullPathNameOrError[0];
            if (viewer.getPathForAllFiles() != "") {
              // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
              filename = localName;
              localName = null;
            } else {
              addShapeProperty(propertyList, "localName", localName);
              viewer.setPrivateKeyForShape(iShape); // for the "AS" keyword to work
            }
          }
        }
        // just checking here, and getting the full path name
        if (!filename.startsWith("cache://") && stype == null) {
          fullPathNameOrError = viewer.getFullPathNameOrError(filename);
          filename = fullPathNameOrError[0];
          if (fullPathNameOrError[1] != null)
            eval.errorStr(ScriptEvaluator.ERROR_fileNotFoundException, filename + ":"
                + fullPathNameOrError[1]);
        }
        Logger.info("reading isosurface data from " + filename);

        if (stype != null) {
          propertyValue = viewer.cacheGet(filename);
          addShapeProperty(propertyList, "calculationType", stype);
        }
        if (propertyValue == null) {
          addShapeProperty(propertyList, "fileName", filename);
          if (localName != null)
            filename = localName;
          if (fileIndex >= 0)
            sbCommand.append(" ").appendI(fileIndex);
        }
        sbCommand.append(" /*file*/").append(Escape.eS(filename));
        if (stype != null)
          sbCommand.append(" ").append(Escape.eS(stype));
        break;
      case T.connect:
        propertyName = "connections";
        switch (tokAt(++i)) {
        case T.bitset:
        case T.expressionBegin:
          propertyValue = new int[] { atomExpressionAt(i).nextSetBit(0) };
          break;
        default:
          propertyValue = new int[] { (int) eval.floatParameterSet(i, 1, 1)[0] };
          break;
        }
        i = eval.iToken;
        break;
      case T.atomindex:
        propertyName = "atomIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.link:
        propertyName = "link";
        sbCommand.append(" link");
        break;
      case T.lattice:
        if (iShape != JC.SHAPE_ISOSURFACE)
          invArg();
        pt = getPoint3f(eval.iToken + 1, false);
        i = eval.iToken;
        if (pt.x <= 0 || pt.y <= 0 || pt.z <= 0)
          break;
        pt.x = (int) pt.x;
        pt.y = (int) pt.y;
        pt.z = (int) pt.z;
        sbCommand.append(" lattice ").append(Escape.eP(pt));
        if (isMapped) {
          propertyName = "mapLattice";
          propertyValue = pt;
        } else {
          lattice = pt;
        }
        break;
      default:
        if (eval.theTok == T.identifier) {
          propertyName = "thisID";
          propertyValue = str;
        }
        /* I have no idea why this is here....
        if (planeSeen && !surfaceObjectSeen) {
          addShapeProperty(propertyList, "nomap", Float.valueOf(0));
          surfaceObjectSeen = true;
        }
        */
        if (!eval.setMeshDisplayProperty(iShape, 0, eval.theTok)) {
          if (T.tokAttr(eval.theTok, T.identifier) && !idSeen) {
            setShapeId(iShape, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.slen - 1;
        break;
      }
      idSeen = (eval.theTok != T.delete);
      if (isWild && surfaceObjectSeen)
        invArg();
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
    }

    // OK, now send them all

    if (!chk) {
      if ((isCavity || haveRadius) && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent", (haveRadius ? new BS()
            : eval.lookupIdentifierValue("solvent")));
        addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
      }
      if (planeSeen && !surfaceObjectSeen && !isMapped) {
        // !isMapped added 6/14/2012 12.3.30
        // because it was preventing planes from being mapped properly
        addShapeProperty(propertyList, "nomap", Float.valueOf(0));
        surfaceObjectSeen = true;
      }
      if (thisSetNumber >= -1)
        addShapeProperty(propertyList, "getSurfaceSets", Integer
            .valueOf(thisSetNumber - 1));
      if (discreteColixes != null) {
        addShapeProperty(propertyList, "colorDiscrete", discreteColixes);
      } else if ("sets".equals(colorScheme)) {
        addShapeProperty(propertyList, "setColorScheme", null);
      } else if (colorScheme != null) {
        ColorEncoder ce = viewer.getColorEncoder(colorScheme);
        if (ce != null) {
          ce.isTranslucent = isColorSchemeTranslucent;
          ce.hi = Float.MAX_VALUE;
          addShapeProperty(propertyList, "remapColor", ce);
        }
      }
      if (surfaceObjectSeen && !isLcaoCartoon && sbCommand.indexOf(";") != 0) {
        propertyList.add(0, new Object[] { "newObject", null });
        boolean needSelect = (bsSelect == null);
        if (needSelect)
          bsSelect = BSUtil.copy(viewer.getSelectionSet(false));
        if (modelIndex < 0)
          modelIndex = viewer.getCurrentModelIndex();
        bsSelect.and(viewer.getModelUndeletedAtomsBitSet(modelIndex));
        if (onlyOneModel != null) {
          BS bsModels = viewer.getModelBitSet(bsSelect, false);
          if (bsModels.cardinality() != 1)
            eval.errorStr(ScriptEvaluator.ERROR_multipleModelsDisplayedNotOK, "ISOSURFACE "
                + onlyOneModel);
          if (needSelect) {
            propertyList.add(0, new Object[] { "select", bsSelect });
            if (sbCommand.indexOf("; isosurface map") == 0) {
              sbCommand = new SB().append("; isosurface map select ").append(
                  Escape.eBS(bsSelect)).append(sbCommand.substring(16));
            }
          }
        }
      }
      if (haveIntersection && !haveSlab) {
        if (!surfaceObjectSeen)
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        if (!isMapped) {
          addShapeProperty(propertyList, "map", Boolean.TRUE);
          addShapeProperty(propertyList, "select", bs);
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        addShapeProperty(propertyList, "slab", getCapSlabObject(-100, false));
      }

      boolean timeMsg = (surfaceObjectSeen && viewer.getBoolean(T.showtiming));
      if (timeMsg)
        Logger.startTimer("isosurface");
      setShapeProperty(iShape, "setProperties", propertyList);
      if (timeMsg)
        eval.showString(Logger.getTimerMsg("isosurface", 0));
      if (defaultMesh) {
        setShapeProperty(iShape, "token", Integer.valueOf(T.mesh));
        setShapeProperty(iShape, "token", Integer.valueOf(T.nofill));
        setShapeProperty(iShape, "token", Integer.valueOf(T.frontonly));
        sbCommand.append(" mesh nofill frontOnly");
      }
    }
    if (lattice != null) // before MAP, this is a display option
      setShapeProperty(JC.SHAPE_ISOSURFACE, "lattice", lattice);
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(iShape, iptDisplayProperty, 0))
        invArg();
    }
    if (chk)
      return false;
    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = getShapeProperty(iShape, "area");
      if (area instanceof Float)
        viewer.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        viewer.setUserVariable("isosurfaceArea", SV
            .getVariableAD((double[]) area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        viewer.setFloatProperty("isosurfaceVolume", ((Float) volume)
            .floatValue());
      else
        viewer.setUserVariable("isosurfaceVolume", SV
            .getVariableAD((double[]) volume));
    }
    if (!isLcaoCartoon) {
      String s = null;
      if (isMapped && !surfaceObjectSeen) {
        setShapeProperty(iShape, "finalize", sbCommand.toString());
      } else if (surfaceObjectSeen) {
        cmd = sbCommand.toString();
        setShapeProperty(iShape, "finalize",
            (cmd.indexOf("; isosurface map") == 0 ? "" : " select "
                + Escape.eBS(bsSelect) + " ")
                + cmd);
        s = (String) getShapeProperty(iShape, "ID");
        if (s != null && !eval.tQuiet) {
          cutoff = ((Float) getShapeProperty(iShape, "cutoff")).floatValue();
          if (Float.isNaN(cutoff) && !Float.isNaN(sigma)) {
            Logger.error("sigma not supported");
          }
          s += " created";
          if (isIsosurface)
            s += " with cutoff=" + cutoff;
          float[] minMax = (float[]) getShapeProperty(iShape, "minMaxInfo");
          if (minMax[0] != Float.MAX_VALUE)
            s += " min=" + minMax[0] + " max=" + minMax[1];
          s += "; " + JC.shapeClassBases[iShape].toLowerCase() + " count: "
              + getShapeProperty(iShape, "count");
          s += eval.getIsosurfaceDataRange(iShape, "\n");
        }
      }
      String sarea, svol;
      if (doCalcArea || doCalcVolume) {
        sarea = (doCalcArea ? "isosurfaceArea = "
            + (area instanceof Float ? "" + area : Escape.eAD((double[]) area))
            : null);
        svol = (doCalcVolume ? "isosurfaceVolume = "
            + (volume instanceof Float ? "" + volume : Escape
                .eAD((double[]) volume)) : null);
        if (s == null) {
          if (doCalcArea)
            eval.showString(sarea);
          if (doCalcVolume)
            eval.showString(svol);
        } else {
          if (doCalcArea)
            s += "\n" + sarea;
          if (doCalcVolume)
            s += "\n" + svol;
        }
      }
      if (s != null)
        eval.showString(s);
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
    if (toCache)
      setShapeProperty(iShape, "cache", null);
    return true;
  }

  private boolean lcaoCartoon() throws ScriptException {
    sm.loadShape(JC.SHAPE_LCAOCARTOON);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_LCAOCARTOON))
      return false;
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "init", fullCommand);
    if (eval.slen == 1) {
      setShapeProperty(JC.SHAPE_LCAOCARTOON, "lcaoID", null);
      return false;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < eval.slen; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (eval.getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        if (tokAt(i + 1) == T.off)
          eval.iToken = i + 1;
        propertyValue = getCapSlabObject(i, true);
        i = eval.iToken;
        break;
      case T.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JC.SHAPE_LCAOCARTOON);
        return false;
      case T.rotate:
        float degx = 0;
        float degy = 0;
        float degz = 0;
        switch (eval.getToken(++i).tok) {
        case T.x:
          degx = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.y:
          degy = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.z:
          degz = floatParameter(++i) * JC.radiansPerDegree;
          break;
        default:
          invArg();
        }
        propertyName = "rotationAxis";
        propertyValue = V3.new3(degx, degy, degz);
        break;
      case T.on:
      case T.display:
      case T.displayed:
        propertyName = "on";
        break;
      case T.off:
      case T.hide:
      case T.hidden:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "select";
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        break;
      case T.color:
        translucency = setColorOptions(null, i + 1, JC.SHAPE_LCAOCARTOON, -2);
        if (translucency != null)
          setShapeProperty(JC.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.translucent:
      case T.opaque:
        eval.setMeshDisplayProperty(JC.SHAPE_LCAOCARTOON, i, eval.theTok);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.spacefill:
      case T.string:
        propertyValue = parameterAsString(i).toLowerCase();
        if (propertyValue.equals("spacefill"))
          propertyValue = "cpk";
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.select:
        if (tokAt(i + 1) == T.bitset || tokAt(i + 1) == T.expressionBegin) {
          propertyName = "select";
          propertyValue = atomExpressionAt(i + 1);
          i = eval.iToken;
        } else {
          propertyName = "selectType";
          propertyValue = parameterAsString(++i);
          if (propertyValue.equals("spacefill"))
            propertyValue = "cpk";
        }
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.lonepair:
      case T.lp:
        propertyName = "lonePair";
        break;
      case T.radical:
      case T.rad:
        propertyName = "radical";
        break;
      case T.molecular:
        propertyName = "molecular";
        break;
      case T.create:
        propertyValue = parameterAsString(++i);
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.id:
        propertyValue = eval.getShapeNameParameter(++i);
        i = eval.iToken;
        if (idSeen)
          invArg();
        propertyName = "lcaoID";
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          if (eval.theTok != T.times)
            propertyValue = parameterAsString(i);
          if (idSeen)
            invArg();
          propertyName = "lcaoID";
          break;
        }
        break;
      }
      if (eval.theTok != T.delete)
        idSeen = true;
      if (propertyName == null)
        invArg();
      setShapeProperty(JC.SHAPE_LCAOCARTOON, propertyName, propertyValue);
    }
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "clear", null);
    return true;
  }

  private Object getCapSlabObject(int i, boolean isLcaoCartoon)
      throws ScriptException {
    if (i < 0) {
      // standard range -100 to 0
      return MeshSurface.getSlabWithinRange(i, 0);
    }
    Object data = null;
    int tok0 = tokAt(i);
    boolean isSlab = (tok0 == T.slab);
    int tok = tokAt(i + 1);
    P4 plane = null;
    P3[] pts = null;
    float d, d2;
    BS bs = null;
    Short slabColix = null;
    Integer slabMeshType = null;
    if (tok == T.translucent) {
      float slabTranslucency = (isFloatParameter(++i + 1) ? floatParameter(++i)
          : 0.5f);
      if (eval.isColorParam(i + 1)) {
        slabColix = Short.valueOf(C.getColixTranslucent3(C.getColix(eval
            .getArgbParam(i + 1)), slabTranslucency != 0, slabTranslucency));
        i = eval.iToken;
      } else {
        slabColix = Short.valueOf(C.getColixTranslucent3(C.INHERIT_COLOR,
            slabTranslucency != 0, slabTranslucency));
      }
      switch (tok = tokAt(i + 1)) {
      case T.mesh:
      case T.fill:
        slabMeshType = Integer.valueOf(tok);
        tok = tokAt(++i + 1);
        break;
      default:
        slabMeshType = Integer.valueOf(T.fill);
        break;
      }
    }
    //TODO: check for compatibility with LCAOCARTOONS
    switch (tok) {
    case T.bitset:
    case T.expressionBegin:
      data = atomExpressionAt(i + 1);
      tok = T.decimal;
      eval.iToken++;
      break;
    case T.off:
      eval.iToken = i + 1;
      return Integer.valueOf(Integer.MIN_VALUE);
    case T.none:
      eval.iToken = i + 1;
      break;
    case T.dollarsign:
      // do we need distance here? "-" here?
      i++;
      data = new Object[] { Float.valueOf(1), parameterAsString(++i) };
      tok = T.mesh;
      break;
    case T.within:
      // isosurface SLAB WITHIN RANGE f1 f2
      i++;
      if (tokAt(++i) == T.range) {
        d = floatParameter(++i);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
      } else if (isFloatParameter(i)) {
        // isosurface SLAB WITHIN distance {atomExpression}|[point array]
        d = floatParameter(i);
        if (eval.isCenterParameter(++i)) {
          P3 pt = centerParameter(i);
          if (chk || !(eval.expressionResult instanceof BS)) {
            pts = new P3[] { pt };
          } else {
            Atom[] atoms = viewer.modelSet.atoms;
            bs = (BS) eval.expressionResult;
            pts = new P3[bs.cardinality()];
            for (int k = 0, j = bs.nextSetBit(0); j >= 0; j = bs
                .nextSetBit(j + 1), k++)
              pts[k] = atoms[j];
          }
        } else {
          pts = eval.getPointArray(i, -1);
        }
        if (pts.length == 0) {
          eval.iToken = i;
          invArg();
        }
        data = new Object[] { Float.valueOf(d), pts, bs };
      } else {
        data = eval.getPointArray(i, 4);
        tok = T.boundbox;
      }
      break;
    case T.boundbox:
      eval.iToken = i + 1;
      data = BoxInfo.getCriticalPoints(viewer.getBoundBoxVertices(), null);
      break;
    //case Token.slicebox:
    // data = BoxInfo.getCriticalPoints(((JmolViewer)(viewer)).slicer.getSliceVert(), null);
    //eval.iToken = i + 1;
    //break;  
    case T.brillouin:
    case T.unitcell:
      eval.iToken = i + 1;
      SymmetryInterface unitCell = viewer.getCurrentUnitCell();
      if (unitCell == null) {
        if (tok == T.unitcell)
          invArg();
      } else {
        pts = BoxInfo.getCriticalPoints(unitCell.getUnitCellVertices(),
            unitCell.getCartesianOffset());
        int iType = (int) unitCell
            .getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
        V3 v1 = null;
        V3 v2 = null;
        switch (iType) {
        case 3:
          break;
        case 1: // polymer
          v2 = V3.newVsub(pts[2], pts[0]);
          v2.scale(1000f);
          //$FALL-THROUGH$
        case 2: // slab
          // "a b c" is really "z y x"
          v1 = V3.newVsub(pts[1], pts[0]);
          v1.scale(1000f);
          pts[0].sub(v1);
          pts[1].scale(2000f);
          if (iType == 1) {
            pts[0].sub(v2);
            pts[2].scale(2000f);
          }
          break;
        }
        data = pts;
      }
      break;
    default:
      // isosurface SLAB n
      // isosurface SLAB -100. 0.  as "within range" 
      if (!isLcaoCartoon && isSlab && isFloatParameter(i + 1)) {
        d = floatParameter(++i);
        if (!isFloatParameter(i + 1))
          return Integer.valueOf((int) d);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
        break;
      }
      // isosurface SLAB [plane]
      plane = eval.planeParameter(++i);
      float off = (isFloatParameter(eval.iToken + 1) ? floatParameter(++eval.iToken)
          : Float.NaN);
      if (!Float.isNaN(off))
        plane.w -= off;
      data = plane;
      tok = T.plane;
    }
    Object colorData = (slabMeshType == null ? null : new Object[] {
        slabMeshType, slabColix });
    return MeshSurface.getSlabObject(tok, data, !isSlab, colorData);
  }

  private boolean mo(boolean isInitOnly) throws ScriptException {
    int offset = Integer.MAX_VALUE;
    boolean isNegOffset = false;
    BS bsModels = viewer.getVisibleFramesBitSet();
    JmolList<Object[]> propertyList = new JmolList<Object[]>();
    int i0 = 1;
    if (tokAt(1) == T.model || tokAt(1) == T.frame) {
      i0 = eval.modelNumberParameter(2);
      if (i0 < 0)
        invArg();
      bsModels.clearAll();
      bsModels.set(i0);
      i0 = 3;
    }
    for (int iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels
        .nextSetBit(iModel + 1)) {
      sm.loadShape(JC.SHAPE_MO);
      int i = i0;
      if (tokAt(i) == T.list && listIsosurface(JC.SHAPE_MO))
        return true;
      setShapeProperty(JC.SHAPE_MO, "init", Integer.valueOf(iModel));
      String title = null;
      int moNumber = ((Integer) getShapeProperty(JC.SHAPE_MO, "moNumber"))
          .intValue();
      float[] linearCombination = (float[]) getShapeProperty(JC.SHAPE_MO,
          "moLinearCombination");
      if (isInitOnly)
        return true;// (moNumber != 0);
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String propertyName = null;
      Object propertyValue = null;

      switch (eval.getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.density:
        propertyName = "squareLinear";
        propertyValue = Boolean.TRUE;
        linearCombination = new float[] { 1 };
        offset = moNumber = 0;
        break;
      case T.integer:
        moNumber = intParameter(i);
        linearCombination = moCombo(propertyList);
        if (linearCombination == null && moNumber < 0)
          linearCombination = new float[] { -100, -moNumber };
        break;
      case T.minus:
        switch (tokAt(++i)) {
        case T.homo:
        case T.lumo:
          break;
        default:
          invArg();
        }
        isNegOffset = true;
        //$FALL-THROUGH$
      case T.homo:
      case T.lumo:
        if ((offset = moOffset(i)) == Integer.MAX_VALUE)
          invArg();
        moNumber = 0;
        linearCombination = moCombo(propertyList);
        break;
      case T.next:
        moNumber = T.next;
        linearCombination = moCombo(propertyList);
        break;
      case T.prev:
        moNumber = T.prev;
        linearCombination = moCombo(propertyList);
        break;
      case T.color:
        setColorOptions(null, i + 1, JC.SHAPE_MO, 2);
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = eval.planeParameter(i + 1);
        break;
      case T.point:
        addShapeProperty(propertyList, "randomSeed",
            tokAt(i + 2) == T.integer ? Integer.valueOf(intParameter(i + 2))
                : null);
        propertyName = "monteCarloCount";
        propertyValue = Integer.valueOf(intParameter(i + 1));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.cutoff:
        if (tokAt(i + 1) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(floatParameter(i + 2));
        } else {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(floatParameter(i + 1));
        }
        break;
      case T.debug:
        propertyName = "debug";
        break;
      case T.noplane:
        propertyName = "plane";
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      case T.titleformat:
        if (i + 1 < eval.slen && tokAt(i + 1) == T.string) {
          propertyName = "titleFormat";
          propertyValue = parameterAsString(i + 1);
        }
        break;
      case T.identifier:
        invArg();
        break;
      default:
        if (eval.isArrayParameter(i)) {
          linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
          if (tokAt(eval.iToken + 1) == T.squared) {
            addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
            eval.iToken++;
          }
          break;
        }
        int ipt = eval.iToken;
        if (!eval.setMeshDisplayProperty(JC.SHAPE_MO, 0, eval.theTok))
          invArg();
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
        eval.setMeshDisplayProperty(JC.SHAPE_MO, ipt, tokAt(ipt));
        return true;
      }
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
      if (moNumber != Integer.MAX_VALUE || linearCombination != null) {
        if (tokAt(eval.iToken + 1) == T.string)
          title = parameterAsString(++eval.iToken);
        eval.setCursorWait(true);
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, iModel, title);
        addShapeProperty(propertyList, "finalize", null);
      }
      if (propertyList.size() > 0)
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
      propertyList.clear();
    }
    return true;
  }

  private float[] moCombo(JmolList<Object[]> propertyList) {
    if (tokAt(eval.iToken + 1) != T.squared)
      return null;
    addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    eval.iToken++;
    return new float[0];
  }

  private int moOffset(int index) throws ScriptException {
    boolean isHomo = (eval.getToken(index).tok == T.homo);
    int offset = (isHomo ? 0 : 1);
    int tok = tokAt(++index);
    if (tok == T.integer && intParameter(index) < 0)
      offset += intParameter(index);
    else if (tok == T.plus)
      offset += intParameter(++index);
    else if (tok == T.minus)
      offset -= intParameter(++index);
    return offset;
  }

  @SuppressWarnings("unchecked")
  private void setMoData(JmolList<Object[]> propertyList, int moNumber,
                         float[] lc, int offset, boolean isNegOffset,
                         int modelIndex, String title) throws ScriptException {
    if (chk)
      return;
    if (modelIndex < 0) {
      modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        eval.errorStr(ScriptEvaluator.ERROR_multipleModelsDisplayedNotOK, "MO isosurfaces");
    }
    Map moData = (Map) viewer.getModelAuxiliaryInfoValue(modelIndex, "moData");
    JmolList<Map<String, Object>> mos = null;
    Map<String, Object> mo;
    Float f;
    int nOrb = 0;
    if (lc == null || lc.length < 2) {
      if (lc != null && lc.length == 1)
        offset = 0;
      if (moData == null)
        error(ScriptEvaluator.ERROR_moModelError);
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue()
          : 0);
      int lastMoCount = (moData.containsKey("lastMoCount") ? ((Integer) moData
          .get("lastMoCount")).intValue() : 1);
      if (moNumber == T.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == T.next)
        moNumber = lastMoNumber + lastMoCount;
      mos = (JmolList<Map<String, Object>>) (moData.get("mos"));
      nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ScriptEvaluator.ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ScriptEvaluator.ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
        } else {
          moNumber = -1;
          for (int i = 0; i < nOrb; i++) {
            mo = mos.get(i);
            if ((f = (Float) mo.get("occupancy")) != null) {
              if (f.floatValue() < 0.5f) {
                // go for LUMO = first unoccupied
                moNumber = i;
                break;
              }
              continue;
            } else if ((f = (Float) mo.get("energy")) != null) {
              if (f.floatValue() > 0) {
                // go for LUMO = first positive
                moNumber = i;
                break;
              }
              continue;
            }
            break;
          }
          if (moNumber < 0)
            error(ScriptEvaluator.ERROR_moOccupancy);
          moNumber += offset;
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        eval.errorStr(ScriptEvaluator.ERROR_moIndex, "" + nOrb);
    }
    moNumber = Math.abs(moNumber);
    moData.put("lastMoNumber", Integer.valueOf(moNumber));
    moData.put("lastMoCount", Integer.valueOf(1));
    if (isNegOffset && lc == null)
      lc = new float[] { -100, moNumber };
    if (lc != null && lc.length < 2) {
      mo = mos.get(moNumber - 1);
      if ((f = (Float) mo.get("energy")) == null) {
        lc = new float[] { 100, moNumber };
      } else {

        // constuct set of equivalent energies and square this

        float energy = f.floatValue();
        BS bs = BS.newN(nOrb);
        int n = 0;
        boolean isAllElectrons = (lc.length == 1 && lc[0] == 1);
        for (int i = 0; i < nOrb; i++) {
          if ((f = (Float) mos.get(i).get("energy")) == null)
            continue;
          float e = f.floatValue();
          if (isAllElectrons ? e <= energy : e == energy) {
            bs.set(i + 1);
            n += 2;
          }
        }
        lc = new float[n];
        for (int i = 0, pt = 0; i < n; i += 2) {
          lc[i] = 1;
          lc[i + 1] = (pt = bs.nextSetBit(pt + 1));
        }
        moData.put("lastMoNumber", Integer.valueOf(bs.nextSetBit(0)));
        moData.put("lastMoCount", Integer.valueOf(n / 2));
      }
      addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    }
    addShapeProperty(propertyList, "moData", moData);
    if (title != null)
      addShapeProperty(propertyList, "title", title);
    addShapeProperty(propertyList, "molecularOrbital", lc != null ? lc
        : Integer.valueOf(Math.abs(moNumber)));
    addShapeProperty(propertyList, "clear", null);
  }

  public String plot(T[] args) throws ScriptException {
    // also used for draw [quaternion, helix, ramachandran] 
    // and write quaternion, ramachandran, plot, ....
    // and plot property propertyX, propertyY, propertyZ //
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      eval.errorStr(ScriptEvaluator.ERROR_multipleModelsDisplayedNotOK, "plot");
    modelIndex = viewer.getJmolDataSourceFrame(modelIndex);
    int pt = args.length - 1;
    boolean isReturnOnly = (args != st);
    T[] statementSave = st;
    if (isReturnOnly)
      st = args;
    int tokCmd = (isReturnOnly ? T.show : args[0].tok);
    int pt0 = (isReturnOnly || tokCmd == T.quaternion
        || tokCmd == T.ramachandran ? 0 : 1);
    String filename = null;
    boolean makeNewFrame = true;
    boolean isDraw = false;
    switch (tokCmd) {
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      break;
    case T.draw:
      makeNewFrame = false;
      isDraw = true;
      break;
    case T.show:
      makeNewFrame = false;
      break;
    case T.write:
      makeNewFrame = false;
      if (ScriptEvaluator.tokAtArray(pt, args) == T.string) {
        filename = stringParameter(pt--);
      } else if (ScriptEvaluator.tokAtArray(pt - 1, args) == T.per) {
        filename = parameterAsString(pt - 2) + "." + parameterAsString(pt);
        pt -= 3;
      } else {
        st = statementSave;
        eval.iToken = st.length;
        error(ScriptEvaluator.ERROR_endOfStatementUnexpected);
      }
      break;
    }
    String qFrame = "";
    Object[] parameters = null;
    String stateScript = "";
    boolean isQuaternion = false;
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    int propertyX = 0, propertyY = 0, propertyZ = 0;
    BS bs = BSUtil.copy(viewer.getSelectionSet(false));
    String preSelected = "; select " + Escape.eBS(bs) + ";\n ";
    String type = eval.optParameterAsString(pt).toLowerCase();
    P3 minXYZ = null;
    P3 maxXYZ = null;
    int tok = ScriptEvaluator.tokAtArray(pt0, args);
    if (tok == T.string)
      tok = T.getTokFromName((String) args[pt0].value);
    switch (tok) {
    default:
      eval.iToken = 1;
      invArg();
      break;
    case T.data:
      eval.iToken = 1;
      type = "data";
      preSelected = "";
      break;
    case T.property:
      eval.iToken = pt0 + 1;
      if (!T.tokAttr(propertyX = tokAt(eval.iToken++), T.atomproperty)
          || !T.tokAttr(propertyY = tokAt(eval.iToken++), T.atomproperty))
        invArg();
      if (T.tokAttr(propertyZ = tokAt(eval.iToken), T.atomproperty))
        eval.iToken++;
      else
        propertyZ = 0;
      if (tokAt(eval.iToken) == T.min) {
        minXYZ = getPoint3f(++eval.iToken, false);
        eval.iToken++;
      }
      if (tokAt(eval.iToken) == T.max) {
        maxXYZ = getPoint3f(++eval.iToken, false);
        eval.iToken++;
      }
      type = "property " + T.nameOf(propertyX) + " "
          + T.nameOf(propertyY)
          + (propertyZ == 0 ? "" : " " + T.nameOf(propertyZ));
      if (bs.nextSetBit(0) < 0)
        bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
      stateScript = "select " + Escape.eBS(bs) + ";\n ";
      break;
    case T.ramachandran:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = eval.optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (tokCmd == T.draw ? " draw" : "");
      break;
    case T.quaternion:
    case T.helix:
      qFrame = " \"" + viewer.getQuaternionFrame() + "\"";
      stateScript = "set quaternionFrame" + qFrame + ";\n  ";
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = eval.optParameterAsString(--pt).toLowerCase();
      }
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : eval.optParameterAsString(pt)) + "w")
          .substring(0, 1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!Parser.isOneOf(type, ";w;x;y;z;r;a;")) // a absolute; r relative
        eval.evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    st = statementSave;
    if (chk) // just in case we later add parameter options to this
      return "";

    // if not just drawing check to see if there is already a plot of this type

    if (makeNewFrame) {
      stateScript += "plot " + type;
      int ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, stateScript);
      if (ptDataFrame > 0 && tokCmd != T.write && tokCmd != T.show) {
        // no -- this is that way we switch frames. viewer.deleteAtoms(viewer.getModelUndeletedAtomsBitSet(ptDataFrame), true);
        // data frame can't be 0.
        viewer.setCurrentModelIndexClear(ptDataFrame, true);
        // BitSet bs2 = viewer.getModelAtomBitSet(ptDataFrame);
        // bs2.and(bs);
        // need to be able to set data directly as well.
        // viewer.display(BitSetUtil.setAll(viewer.getAtomCount()), bs2, tQuiet);
        return "";
      }
    }

    // prepare data for property plotting

    float[] dataX = null, dataY = null, dataZ = null;
    P3 factors = P3.new3(1, 1, 1);
    if (tok == T.property) {
      dataX = eval.getBitsetPropertyFloat(bs, propertyX | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.x), (maxXYZ == null ? Float.NaN
              : maxXYZ.x));
      dataY = eval.getBitsetPropertyFloat(bs, propertyY | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.y), (maxXYZ == null ? Float.NaN
              : maxXYZ.y));
      if (propertyZ != 0)
        dataZ = eval.getBitsetPropertyFloat(bs, propertyZ | T.selectedfloat,
            (minXYZ == null ? Float.NaN : minXYZ.z),
            (maxXYZ == null ? Float.NaN : maxXYZ.z));
      if (minXYZ == null)
        minXYZ = P3.new3(getMinMax(dataX, false, propertyX), getMinMax(
            dataY, false, propertyY), getMinMax(dataZ, false, propertyZ));
      if (maxXYZ == null)
        maxXYZ = P3.new3(getMinMax(dataX, true, propertyX), getMinMax(
            dataY, true, propertyY), getMinMax(dataZ, true, propertyZ));
      Logger.info("plot min/max: " + minXYZ + " " + maxXYZ);
      P3 center = P3.newP(maxXYZ);
      center.add(minXYZ);
      center.scale(0.5f);
      factors.setT(maxXYZ);
      factors.sub(minXYZ);
      factors.set(factors.x / 200, factors.y / 200, factors.z / 200);
      if (T.tokAttr(propertyX, T.intproperty)) {
        factors.x = 1;
        center.x = 0;
      } else if (factors.x > 0.1 && factors.x <= 10) {
        factors.x = 1;
      }
      if (T.tokAttr(propertyY, T.intproperty)) {
        factors.y = 1;
        center.y = 0;
      } else if (factors.y > 0.1 && factors.y <= 10) {
        factors.y = 1;
      }
      if (T.tokAttr(propertyZ, T.intproperty)) {
        factors.z = 1;
        center.z = 0;
      } else if (factors.z > 0.1 && factors.z <= 10) {
        factors.z = 1;
      }
      if (propertyZ == 0)
        center.z = minXYZ.z = maxXYZ.z = factors.z = 0;
      for (int i = 0; i < dataX.length; i++)
        dataX[i] = (dataX[i] - center.x) / factors.x;
      for (int i = 0; i < dataY.length; i++)
        dataY[i] = (dataY[i] - center.y) / factors.y;
      if (propertyZ != 0)
        for (int i = 0; i < dataZ.length; i++)
          dataZ[i] = (dataZ[i] - center.z) / factors.z;
      parameters = new Object[] { bs, dataX, dataY, dataZ, minXYZ, maxXYZ,
          factors, center };
    }

    // all set...

    if (tokCmd == T.write)
      return viewer.streamFileData(filename, "PLOT", type, modelIndex,
          parameters);
    
    String data = (type.equals("data") ? "1 0 H 0 0 0 # Jmol PDB-encoded data" : viewer.getPdbData(modelIndex, type, parameters));
    
    if (tokCmd == T.show)
      return data;

    if (Logger.debugging)
      Logger.debug(data);

    if (tokCmd == T.draw) {
      eval.runScript(data);
      return "";
    }

    // create the new model

    String[] savedFileInfo = viewer.getFileInfo();
    boolean oldAppendNew = viewer.getBoolean(T.appendnew);
    viewer.setAppendNew(true);
    boolean isOK = (data != null && viewer.openStringInlineParamsAppend(data, null, true) == null);
    viewer.setAppendNew(oldAppendNew);
    viewer.setFileInfo(savedFileInfo);
    if (!isOK)
      return "";
    int modelCount = viewer.getModelCount();
    viewer.setJmolDataFrame(stateScript, modelIndex, modelCount - 1);
    if (tok != T.property)
      stateScript += ";\n" + preSelected;
    StateScript ss = viewer.addStateScript(stateScript, true, false);

    // get post-processing script

    float radius = 150;
    String script;
    switch (tok) {
    default:
      script = "frame 0.0; frame last; reset;select visible;wireframe only;";
      radius = 10;
      break;
    case T.property:
      viewer.setFrameTitle(modelCount - 1, type + " plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      float f = 3;
      script = "frame 0.0; frame last; reset;" + "select visible; spacefill "
          + f + "; wireframe 0;" + "draw plotAxisX" + modelCount
          + " {100 -100 -100} {-100 -100 -100} \"" + T.nameOf(propertyX)
          + "\";" + "draw plotAxisY" + modelCount
          + " {-100 100 -100} {-100 -100 -100} \"" + T.nameOf(propertyY)
          + "\";";
      if (propertyZ != 0)
        script += "draw plotAxisZ" + modelCount
            + " {-100 -100 100} {-100 -100 -100} \"" + T.nameOf(propertyZ)
            + "\";";
      break;
    case T.ramachandran:
      viewer.setFrameTitle(modelCount - 1, "ramachandran plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {100 0 0} {-100 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 100 0} {0 -100 0} \"psi\";";
      break;
    case T.quaternion:
    case T.helix:
      viewer.setFrameTitle(modelCount - 1, type.replace('w', ' ') + qFrame
          + " for model " + viewer.getModelNumberDotted(modelIndex));
      String color = (C
          .getHexCode(viewer.getColixBackgroundContrast()));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; spacefill 3.0; "
          + "isosurface quatSphere" + modelCount + " color " + color
          + " sphere 100.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {100 0 0} {-100 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 100 0} {0 -100 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 100} {0 0 -100} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02;";
      break;
    }

    // run the post-processing script and set rotation radius and display frame title
    eval.runScript(script + preSelected);
    ss.setModelIndex(viewer.getCurrentModelIndex());
    viewer.setRotationRadius(radius, true);
    sm.loadShape(JC.SHAPE_ECHO);
    eval.showString("frame " + viewer.getModelNumberDotted(modelCount - 1)
        + (type.length() > 0 ? " created: " + type + (isQuaternion ? qFrame : "") : ""));
    return "";
  }

  private static float getMinMax(float[] data, boolean isMax, int tok) {
    if (data == null)
      return 0;
    switch (tok) {
    case T.omega:
    case T.phi:
    case T.psi:
      return (isMax ? 180 : -180);
    case T.eta:
    case T.theta:
      return (isMax ? 360 : 0);
    case T.straightness:
      return (isMax ? 1 : -1);
    }
    float fmax = (isMax ? -1E10f : 1E10f);
    for (int i = data.length; --i >= 0;) {
      float f = data[i];
      if (Float.isNaN(f))
        continue;
      if (isMax == (f > fmax))
        fmax = f;
    }
    return fmax;
  }

  private boolean polyhedra() throws ScriptException {
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int lighting = 0;
    int nAtomSets = 0;
    sm.loadShape(JC.SHAPE_POLYHEDRA);
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.TRUE);
    String setPropertyName = "centers";
    String decimalPropertyName = "radius_";
    float translucentLevel = Float.MAX_VALUE;
    eval.colorArgb[0] = Integer.MIN_VALUE;
    for (int i = 1; i < eval.slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (eval.getToken(i).tok) {
      case T.delete:
      case T.on:
      case T.off:
        if (i + 1 != eval.slen || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && "to".equals(setPropertyName))
          error(ScriptEvaluator.ERROR_incompatibleArguments);
        propertyName = (eval.theTok == T.off ? "off" : eval.theTok == T.on ? "on"
            : "delete");
        onOffDelete = true;
        break;
      case T.opEQ:
      case T.comma:
        continue;
      case T.bonds:
        if (nAtomSets > 0)
          invPO();
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case T.radius:
        decimalPropertyName = "radius";
        continue;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          invPO();
        if (eval.theTok == T.integer) {
          if (decimalPropertyName == "radius_") {
            propertyName = "nVertices";
            propertyValue = Integer.valueOf(intParameter(i));
            needsGenerating = true;
            break;
          }
        }
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = Float.valueOf(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (typeSeen)
          invPO();
        if (++nAtomSets > 2)
          error(ScriptEvaluator.ERROR_badArgumentCount);
        if ("to".equals(setPropertyName))
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = atomExpressionAt(i);
        i =eval.iToken;
        break;
      case T.to:
        if (nAtomSets > 1)
          invPO();
        if (tokAt(i + 1) == T.bitset 
            || tokAt(i + 1) == T.expressionBegin && !needsGenerating) {
          propertyName = "toBitSet";
          propertyValue = atomExpressionAt(++i);
          i = eval.iToken;
          needsGenerating = true;
          break;
        } else if (!needsGenerating) {
          error(ScriptEvaluator.ERROR_insufficientArguments);
        }
        setPropertyName = "to";
        continue;
      case T.facecenteroffset:
        if (!needsGenerating)
          error(ScriptEvaluator.ERROR_insufficientArguments);
        decimalPropertyName = "faceCenterOffset";
        isDesignParameter = true;
        continue;
      case T.distancefactor:
        if (nAtomSets == 0)
          error(ScriptEvaluator.ERROR_insufficientArguments);
        decimalPropertyName = "distanceFactor";
        isDesignParameter = true;
        continue;
      case T.color:
      case T.translucent:
      case T.opaque:
        translucentLevel = eval.getColorTrans(i, true);
        i = eval.iToken;
        continue;
      case T.collapsed:
      case T.flat:
        propertyName = "collapsed";
        propertyValue = (eval.theTok == T.collapsed ? Boolean.TRUE
            : Boolean.FALSE);
        if (typeSeen)
          error(ScriptEvaluator.ERROR_incompatibleArguments);
        typeSeen = true;
        break;
      case T.noedges:
      case T.edges:
      case T.frontedges:
        if (edgeParameterSeen)
          error(ScriptEvaluator.ERROR_incompatibleArguments);
        propertyName = parameterAsString(i);
        edgeParameterSeen = true;
        break;
      case T.fullylit:
        lighting = eval.theTok;
        continue;
      default:
        if (eval.isColorParam(i)) {
          eval.colorArgb[0] = eval.getArgbParam(i);
          i = eval.iToken;
          continue;
        }
        invArg();
      }
      setShapeProperty(JC.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return false;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen && lighting == 0)
      error(ScriptEvaluator.ERROR_insufficientArguments);
    if (needsGenerating)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", null);
    if (eval.colorArgb[0] != Integer.MIN_VALUE)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "colorThis", Integer
          .valueOf(eval.colorArgb[0]));
    if (translucentLevel != Float.MAX_VALUE)
      eval.setShapeTranslucency(JC.SHAPE_POLYHEDRA, "", "translucentThis",
          translucentLevel, null);
    if (lighting != 0)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "token", Integer.valueOf(lighting));
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.FALSE);
    return true;
  }

  private boolean struts() throws ScriptException {
    boolean defOn = (tokAt(1) == T.only || tokAt(1) == T.on || eval.slen == 1);
    int mad = eval.getMadParameter();
    if (defOn)
      mad = Math.round (viewer.getFloat(T.strutdefaultradius) * 2000f);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_STRUT));
    eval.setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
    return true;
  }

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    setShapeProperty(iShape, "init", fullCommand);
    eval.iToken = 0;
    int tok1 = tokAt(1);
    int tok2 = tokAt(2);
    if (tok1 == T.delete || tok2 == T.delete && tokAt(++eval.iToken) == T.all) {
      setShapeProperty(iShape, "delete", null);
      eval.iToken += 2;
      if (eval.slen > eval.iToken) {
        setShapeProperty(iShape, "init", fullCommand);
        setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      }
      return null;
    }
    eval.iToken = 1;
    if (!eval.setMeshDisplayProperty(iShape, 0, tok1)) {
      setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      if (iShape != JC.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
      if (tok1 != T.id
          && (tok2 == T.times || tok1 == T.times
              && eval.setMeshDisplayProperty(iShape, 0, tok2))) {
        String id = setShapeId(iShape, 1, false);
        eval.iToken++;
        return id;
      }
    }
    return null;
  }

  private void getWithinDistanceVector(JmolList<Object[]> propertyList,
                                       float distance, P3 ptc, BS bs,
                                       boolean isShow) {
    JmolList<P3> v = new JmolList<P3>();
    P3[] pts = new P3[2];
    if (bs == null) {
      P3 pt1 = P3.new3(distance, distance, distance);
      P3 pt0 = P3.newP(ptc);
      pt0.sub(pt1);
      pt1.add(ptc);
      pts[0] = pt0;
      pts[1] = pt1;
      v.addLast(ptc);
    } else {
      BoxInfo bbox = viewer.getBoxInfo(bs, -Math.abs(distance));
      pts[0] = bbox.getBboxVertices()[0];
      pts[1] = bbox.getBboxVertices()[7];
      if (bs.cardinality() == 1)
        v.addLast(viewer.getAtomPoint3f(bs.nextSetBit(0)));
    }
    if (v.size() == 1 && !isShow) {
      addShapeProperty(propertyList, "withinDistance", Float.valueOf(distance));
      addShapeProperty(propertyList, "withinPoint", v.get(0));
    }
    addShapeProperty(propertyList, (isShow ? "displayWithin" : "withinPoints"),
        new Object[] { Float.valueOf(distance), pts, bs, v });
  }

  private String setColorOptions(SB sb, int index, int iShape, int nAllowed)
      throws ScriptException {
    eval.getToken(index);
    String translucency = "opaque";
    if (eval.theTok == T.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        eval.setShapeTranslucency(iShape, null, "translucent", value, null);
        if (sb != null) {
          sb.append(" translucent");
          if (value != Float.MAX_VALUE)
            sb.append(" ").appendF(value);
        }
      } else {
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
      }
    } else if (eval.theTok == T.opaque) {
      if (nAllowed >= 0)
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
    } else {
      eval.iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (eval.isColorParam(eval.iToken + 1)) {
        int color = eval.getArgbParam(++eval.iToken);
        setShapeProperty(iShape, "colorRGB", Integer.valueOf(color));
        sb.append(" ").append(Escape.escapeColor(color));
      } else if (eval.iToken < index) {
        invArg();
      } else {
        break;
      }
    }
    return translucency;
  }

  private void addShapeProperty(JmolList<Object[]> propertyList, String key,
                                Object value) {
    if (chk)
      return;
    propertyList.addLast(new Object[] { key, value });
  }

  /**
   * for the ISOSURFACE command
   * 
   * @param fname
   * @param xyz
   * @param ret
   * @return [ ScriptFunction, Params ]
   */
  private Object[] createFunction(String fname, String xyz, String ret) {
    ScriptEvaluator e = (new ScriptEvaluator());
    e.setViewer(viewer);
    try {
      e.compileScript(null, "function " + fname + "(" + xyz + ") { return "
          + ret + "}", false);
      JmolList<SV> params = new JmolList<SV>();
      for (int i = 0; i < xyz.length(); i += 2)
        params.addLast(SV.newVariable(T.decimal, Float.valueOf(0f)).setName(
            xyz.substring(i, i + 1)));
      return new Object[] { e.aatoken[0][1].value, params };
    } catch (Exception ex) {
      return null;
    }
  }

  private float[][] floatArraySet(int i, int nX, int nY) throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare)
      invArg();
    float[][] fparams = ArrayUtil.newFloat2(nX);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = eval.getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        i++;
        float[] f = new float[nY];
        fparams[n++] = f;
        for (int j = 0; j < nY; j++) {
          f[j] = floatParameter(i++);
          if (tokAt(i) == T.comma)
            i++;
        }
        if (tokAt(i++) != T.rightsquare)
          invArg();
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  private float[][][] floatArraySetXYZ(int i, int nX, int nY, int nZ)
      throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare || nX <= 0)
      invArg();
    float[][][] fparams = ArrayUtil.newFloat3(nX, -1);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = eval.getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        fparams[n++] = floatArraySet(i, nY, nZ);
        i = ++eval.iToken;
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    eval.checkLength23();
    if (!chk)
      eval.showString((String) getShapeProperty(iShape, "list"
          + (tokAt(2) == T.nada ? "" : " " + eval.getToken(2).value)));
    return true;
  }

}
