package org.jmol.script;

import java.util.Map;

import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.modelset.TickInfo;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Measure;


/**
 * The ScriptParam class holds methods used to parse parameters 
 * in Jmol scripts. 
 *  
 */
abstract public class ScriptParam extends ScriptError {

  
  
  // upward-directed calls

  abstract public BS atomExpressionAt(int i) throws ScriptException;
  abstract public BS atomExpression(T[] code, int pcStart, int pcStop,
                                    boolean allowRefresh, boolean allowUnderflow,
                                    boolean mustBeBitSet, boolean andNotDeleted)
               throws ScriptException;
  abstract protected P3 getObjectCenter(String id, int index, int modelIndex);
  abstract protected P4 getPlaneForObject(String id, V3 vAB, V3 vAC);
  abstract protected Lst<SV> parameterExpressionList(int pt, int ptAtom,
      boolean isArrayItem) throws ScriptException;
  abstract protected void restrictSelected(boolean isBond, boolean doInvert);
  
  public Map<String, SV> contextVariables;
  public ScriptContext thisContext;

  public int iToken;
  public int theTok;
  public T theToken;
  public T[] st;
  public int slen;

  // passed back as globals
  
  protected P3 fractionalPoint;
  protected boolean coordinatesAreFractional;
  public boolean isBondSet;
  public Object expressionResult;


  public T getToken(int i) throws ScriptException {
    if (!checkToken(i))
      error(ERROR_endOfStatementUnexpected);
    theToken = st[i];
    theTok = theToken.tok;
    return theToken;
  }

  public int tokAt(int i) {
    return (i < slen && st[i] != null ? st[i].tok : T.nada);
  }

  protected boolean checkToken(int i) {
    return (iToken = i) < slen;
  }


  @SuppressWarnings("unchecked")
  public Object getParameter(String key, int tokType, boolean nullAsString) {
    Object v = getContextVariableAsVariable(key);
    if (v == null) {
      if (nullAsString)
        v = vwr.getP(key);
      else if ((v = vwr.getPOrNull(key)) == null)
        return null;
    }
    switch (tokType) {
    case T.variable:
      return SV.getVariable(v);
    case T.string:
      if (!(v instanceof Lst<?>))
        break;
      Lst<SV> sv = (Lst<SV>) v;
      SB sb = new SB();
      for (int i = 0; i < sv.size(); i++)
        sb.append(sv.get(i).asString()).appendC('\n');
      return sb.toString();
    }
    return (v instanceof SV ? SV.oValue((SV) v) : v);
  }

  protected String getStringParameter(String var, boolean orReturnName) {
    SV v = getContextVariableAsVariable(var);
    if (v != null)
      return v.asString();
    String val = "" + vwr.getP(var);
    return (val.length() == 0 && orReturnName ? var : val);
  }

  public SV getContextVariableAsVariable(String var) {
    if (var.equals("expressionBegin"))
      return null;
    var = var.toLowerCase();
    return (contextVariables != null && contextVariables.containsKey(var) ? contextVariables
        .get(var) : thisContext == null ? null : thisContext.getVariable(var));
  }
  
  public String paramAsStr(int i) throws ScriptException {
    getToken(i);
    if (theToken == null)
      error(ERROR_endOfStatementUnexpected);
    return SV.sValue(theToken);
  }

  public String stringParameter(int index) throws ScriptException {
    if (!checkToken(index) || getToken(index).tok != T.string)
      error(ERROR_stringExpected);
    return (String) theToken.value;
  }

  public String[] stringParameterSet(int i) throws ScriptException {
    switch (tokAt(i)) {
    case T.string:
      String s = stringParameter(i);
      if (s.startsWith("[\"")) {
        Object o = vwr.evaluateExpression(s);
        if (o instanceof String)
          return PT.split((String) o, "\n");
      }
      return new String[] { s };
    case T.spacebeforesquare:
      i += 2;
      break;
    case T.leftsquare:
      ++i;
      break;
    case T.varray:
      return SV.strListValue(getToken(i));
    default:
      invArg();
    }
    int tok;
    Lst<String> v = new Lst<String>();
    while ((tok = tokAt(i)) != T.rightsquare) {
      switch (tok) {
      case T.comma:
        break;
      case T.string:
        v.addLast(stringParameter(i));
        break;
      default:
      case T.nada:
        invArg();
      }
      i++;
    }
    iToken = i;
    int n = v.size();
    String[] sParams = new String[n];
    for (int j = 0; j < n; j++) {
      sParams[j] = v.get(j);
    }
    return sParams;
  }

  public String objectNameParameter(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_objectNameExpected);
    return paramAsStr(index);
  }

  protected P3 atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case T.bitset:
    case T.expressionBegin:
      BS bs = atomExpression(st, i, 0, true, false, false, true);
      if (bs != null && bs.cardinality() == 1)
        return vwr.getAtomPoint3f(bs.nextSetBit(0));
      if (bs != null)
        return vwr.ms.getAtomSetCenter(bs);
      if (expressionResult instanceof P3)
        return (P3) expressionResult;
      invArg();
      break;
    case T.leftbrace:
    case T.point3f:
      return getPoint3f(i, true);
    }
    invArg();
    // impossible return
    return null;
  }

  public boolean isCenterParameter(int i) {
    int tok = tokAt(i);
    return (tok == T.dollarsign || tok == T.leftbrace
        || tok == T.expressionBegin || tok == T.point3f || tok == T.bitset);
  }

  public P3 centerParameter(int i) throws ScriptException {
    return centerParameterForModel(i, Integer.MIN_VALUE);
  }

  protected P3 centerParameterForModel(int i, int modelIndex)
      throws ScriptException {
    P3 center = null;
    expressionResult = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case T.dollarsign:
        String id = objectNameParameter(++i);
        int index = Integer.MIN_VALUE;
        // allow for $pt2.3 -- specific vertex
        if (tokAt(i + 1) == T.leftsquare) {
          index = parameterExpressionList(-i - 1, -1, true).get(0).asInt();
          if (getToken(--iToken).tok != T.rightsquare)
            invArg();
        }
        if (chk)
          return new P3();
        if (tokAt(i + 1) == T.per
            && (tokAt(i + 2) == T.length || tokAt(i + 2) == T.size)) {
          index = Integer.MAX_VALUE;
          iToken = i + 2;
        }
        if ((center = getObjectCenter(id, index, modelIndex)) == null)
          errorStr(ERROR_drawObjectNotDefined, id);
        break;
      case T.bitset:
      case T.expressionBegin:
      case T.leftbrace:
      case T.point3f:
        center = atomCenterOrCoordinateParameter(i);
        break;
      }
    }
    if (center == null)
      error(ERROR_coordinateOrNameOrExpressionRequired);
    return center;
  }

  public P4 planeParameter(int i) throws ScriptException {
    V3 vAB = new V3();
    V3 vAC = new V3();
    P4 plane = null;
    boolean isNegated = (tokAt(i) == T.minus);
    if (isNegated)
      i++;
    if (i < slen)
      switch (getToken(i).tok) {
      case T.point4f:
        plane = P4.newPt((P4) theToken.value);
        break;
      case T.dollarsign:
        String id = objectNameParameter(++i);
        if (chk)
          return new P4();
        plane = getPlaneForObject(id, vAB, vAC);
        break;
      case T.x:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("x=?", null);
        plane = P4.new4(1, 0, 0, -floatParameter(i));
        break;
      case T.y:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("y=?", null);
        plane = P4.new4(0, 1, 0, -floatParameter(i));
        break;
      case T.z:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("z=?", null);
        plane = P4.new4(0, 0, 1, -floatParameter(i));
        break;
      case T.identifier:
      case T.string:
        String str = paramAsStr(i);
        if (str.equalsIgnoreCase("xy"))
          return P4.new4(0, 0, 1, 0);
        if (str.equalsIgnoreCase("xz"))
          return P4.new4(0, 1, 0, 0);
        if (str.equalsIgnoreCase("yz"))
          return P4.new4(1, 0, 0, 0);
        iToken += 2;
        break;
      case T.leftbrace:
        if (!isPoint3f(i)) {
          plane = getPoint4f(i);
          break;
        }
        //$FALL-THROUGH$
      case T.bitset:
      case T.expressionBegin:
        P3 pt1 = atomCenterOrCoordinateParameter(i);
        if (getToken(++iToken).tok == T.comma)
          ++iToken;
        P3 pt2 = atomCenterOrCoordinateParameter(iToken);
        if (getToken(++iToken).tok == T.comma)
          ++iToken;
        P3 pt3 = atomCenterOrCoordinateParameter(iToken);
        i = iToken;
        V3 norm = new V3();
        float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, norm, vAB, vAC);
        plane = new P4();
        plane.set4(norm.x, norm.y, norm.z, w);
        if (!chk && Logger.debugging)
          Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: "
              + plane);
        break;
      }
    if (plane == null)
      errorMore(ERROR_planeExpected, "{a b c d}",
          "\"xy\" \"xz\" \"yz\" \"x=...\" \"y=...\" \"z=...\"", "$xxxxx");
    if (isNegated) {
      plane.scale4(-1);
    }
    return plane;
  }

  public P4 hklParameter(int i) throws ScriptException {
    if (!chk && vwr.getCurrentUnitCell() == null)
      error(ERROR_noUnitCell);
    P3 pt = (P3) getPointOrPlane(i, false, true, false, true, 3, 3);
    P4 p = getHklPlane(pt);
    if (p == null)
      error(ERROR_badMillerIndices);
    if (!chk && Logger.debugging)
      Logger.debug("defined plane: " + p);
    return p;
  }

  public P4 getHklPlane(P3 pt) {
    V3 vAB = new V3();
    V3 vAC = new V3();
    P3 pt1 = P3.new3(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
    P3 pt2 = P3.new3(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
    P3 pt3 = P3.new3(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
    // trick for 001 010 100 is to define the other points on other edges
    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      return null;
    } else if (pt.x == 0 && pt.y == 0) {
      pt1.set(1, 0, pt3.z);
      pt2.set(0, 1, pt3.z);
    } else if (pt.y == 0 && pt.z == 0) {
      pt2.set(pt1.x, 0, 1);
      pt3.set(pt1.x, 1, 0);
    } else if (pt.z == 0 && pt.x == 0) {
      pt3.set(0, pt2.y, 1);
      pt1.set(1, pt2.y, 0);
    } else if (pt.x == 0) {
      pt1.set(1, pt2.y, 0);
    } else if (pt.y == 0) {
      pt2.set(0, 1, pt3.z);
    } else if (pt.z == 0) {
      pt3.set(pt1.x, 0, 1);
    }
    // base this one on the currently defined unit cell
    vwr.toCartesian(pt1, false);
    vwr.toCartesian(pt2, false);
    vwr.toCartesian(pt3, false);
    V3 plane = new V3();
    float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
    P4 pt4 = new P4();
    pt4.set4(plane.x, plane.y, plane.z, w);
    return pt4;
  }

  protected Object getPointOrPlane(int index, boolean integerOnly,
                                 boolean allowFractional, boolean doConvert,
                                 boolean implicitFractional, int minDim,
                                 int maxDim) throws ScriptException {
    // { x y z } or {a/b c/d e/f} are encoded now as seqcodes and model numbers
    // so we decode them here. It's a bit of a pain, but it isn't too bad.
    float[] coord = new float[6];
    int n = 0;
    coordinatesAreFractional = implicitFractional;
    if (tokAt(index) == T.point3f) {
      if (minDim <= 3 && maxDim >= 3)
        return /*Point3f*/getToken(index).value;
      invArg();
    }
    if (tokAt(index) == T.point4f) {
      if (minDim <= 4 && maxDim >= 4)
        return /*Point4f*/getToken(index).value;
      invArg();
    }
    int multiplier = 1;
    out: for (int i = index; i < st.length; i++) {
      switch (getToken(i).tok) {
      case T.leftbrace:
      case T.comma:
      case T.opAnd:
      case T.opAND:
        break;
      case T.rightbrace:
        break out;
      case T.minus:
        multiplier = -1;
        break;
      case T.spec_seqcode_range:
        if (n == 6)
          invArg();
        coord[n++] = theToken.intValue;
        multiplier = -1;
        break;
      case T.integer:
      case T.spec_seqcode:
        if (n == 6)
          invArg();
        coord[n++] = theToken.intValue * multiplier;
        multiplier = 1;
        break;
      case T.divide:
      case T.spec_model: // after a slash
        if (!allowFractional)
          invArg();
        if (theTok == T.divide)
          getToken(++i);
        n--;
        if (n < 0 || integerOnly)
          invArg();
        if (theToken.value instanceof Integer || theTok == T.integer) {
          coord[n++] /= (theToken.intValue == Integer.MAX_VALUE ? ((Integer) theToken.value)
              .intValue() : theToken.intValue);
        } else if (theToken.value instanceof Float) {
          coord[n++] /= ((Float) theToken.value).floatValue();
        }
        coordinatesAreFractional = true;
        break;
      case T.spec_chain: //? 
      case T.misc: // NaN
        coord[n++] = Float.NaN;
        break;
      case T.decimal:
      case T.spec_model2:
        if (integerOnly)
          invArg();
        if (n == 6)
          invArg();
        coord[n++] = ((Float) theToken.value).floatValue();
        break;
      default:
        invArg();
      }
    }
    if (n < minDim || n > maxDim)
      invArg();
    if (n == 3) {
      P3 pt = P3.new3(coord[0], coord[1], coord[2]);
      if (coordinatesAreFractional && doConvert) {
        fractionalPoint = P3.newP(pt);
        if (!chk)
          vwr.toCartesian(pt, false);//!vwr.getBoolean(T.fractionalrelative));
      }
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how
        // to convert?)
        invArg();
      P4 plane = P4.new4(coord[0], coord[1], coord[2], coord[3]);
      return plane;
    }
    return coord;
  }

  public boolean isPoint3f(int i) {
    // first check for simple possibilities:
    boolean isOK;
    if ((isOK = (tokAt(i) == T.point3f)) || tokAt(i) == T.point4f
        || isFloatParameter(i + 1) && isFloatParameter(i + 2)
        && isFloatParameter(i + 3) && isFloatParameter(i + 4))
      return isOK;
    ignoreError = true;
    int t = iToken;
    isOK = true;
    try {
      getPoint3f(i, true);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    iToken = t;
    return isOK;
  }

  public P3 getPoint3f(int i, boolean allowFractional) throws ScriptException {
    return (P3) getPointOrPlane(i, false, allowFractional, true, false, 3, 3);
  }

  public P4 getPoint4f(int i) throws ScriptException {
    return (P4) getPointOrPlane(i, false, false, false, false, 4, 4);
  }

  public P3 xypParameter(int index) throws ScriptException {
    // [x y] or [x,y] refers to an xy point on the screen
    //     return a Point3f with z = Float.MAX_VALUE
    // [x y %] or [x,y %] refers to an xy point on the screen
    // as a percent
    //     return a Point3f with z = -Float.MAX_VALUE

    int tok = tokAt(index);
    if (tok == T.spacebeforesquare)
      tok = tokAt(++index);
    if (tok != T.leftsquare || !isFloatParameter(++index))
      return null;
    P3 pt = new P3();
    pt.x = floatParameter(index);
    if (tokAt(++index) == T.comma)
      index++;
    if (!isFloatParameter(index))
      return null;
    pt.y = floatParameter(index);
    boolean isPercent = (tokAt(++index) == T.percent);
    if (isPercent)
      ++index;
    if (tokAt(index) != T.rightsquare)
      return null;
    iToken = index;
    pt.z = (isPercent ? -1 : 1) * Float.MAX_VALUE;
    return pt;
  }

  public String optParameterAsString(int i) throws ScriptException {
    if (i >= slen)
      return "";
    return paramAsStr(i);
  }

  public int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == T.integer)
        return theToken.intValue;
    error(ERROR_integerExpected);
    return 0;
  }

  public boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case T.integer:
    case T.decimal:
      return true;
    }
    return false;
  }

  public float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      getToken(index);
      switch (theTok) {
      case T.spec_seqcode_range:
        return -theToken.intValue;
      case T.spec_seqcode:
      case T.integer:
        return theToken.intValue;
      case T.spec_model2:
      case T.decimal:
        return ((Float) theToken.value).floatValue();
      }
    }
    error(ERROR_numberExpected);
    return 0;
  }
  
  public P3[] getPointArray(int i, int nPoints) throws ScriptException {
    P3[] points = (nPoints < 0 ? null : new P3[nPoints]);
    Lst<P3> vp = (nPoints < 0 ? new Lst<P3>() : null);
    int tok = (i < 0 ? T.varray : getToken(i++).tok);
    switch (tok) {
    case T.varray:
      Lst<SV> v = ((SV) theToken).getList();  
      if (nPoints >= 0 && v.size() != nPoints)
        invArg();
      nPoints = v.size();
      if (points == null)
        points = new P3[nPoints];
      for (int j = 0; j < nPoints; j++)
        if ((points[j] = SV.ptValue(v.get(j))) == null)
          invArg();
      return points;
    case T.spacebeforesquare:
      tok = tokAt(i++);
      break;
    }
    if (tok != T.leftsquare)
      invArg();
    int n = 0;
    while (tok != T.rightsquare && tok != T.nada) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.nada:
      case T.rightsquare:
        break;
      case T.comma:
        i++;
        break;
      default:
        if (nPoints >= 0 && n == nPoints) {
          tok = T.nada;
          break;
        }
        P3 pt = getPoint3f(i, true);
        if (points == null)
          vp.addLast(pt);
        else
          points[n] = pt;
        n++;
        i = iToken + 1;
      }
    }
    if (tok != T.rightsquare)
      invArg();
    if (points == null)
      points = vp.toArray(new P3[vp.size()]);
    if (nPoints > 0 && points[nPoints -  1] == null)
      invArg();
    return points;
  }

  public Lst<Object> listParameter(int i, int nMin, int nMax)
      throws ScriptException {
    Lst<Object> v = new Lst<Object>();
    P3 pt;
    int tok = tokAt(i);
    if (tok == T.spacebeforesquare)
      tok = tokAt(++i);
    boolean haveBrace = (tok == T.leftbrace);
    boolean haveSquare = (tok == T.leftsquare);
    if (haveBrace || haveSquare)
      i++;
    int n = 0;
    while (n < nMax) {
      tok = tokAt(i);
      if (haveBrace && tok == T.rightbrace || haveSquare
          && tok == T.rightsquare)
        break;
      switch (tok) {
      case T.comma:
      case T.leftbrace:
      case T.rightbrace:
        break;
      case T.string:
        break;
      case T.point3f:
        pt = getPoint3f(i, false);
        v.addLast(Float.valueOf(pt.x));
        v.addLast(Float.valueOf(pt.y));
        v.addLast(Float.valueOf(pt.z));
        n += 3;
        break;
      case T.point4f:
        P4 pt4 = getPoint4f(i);
        v.addLast(Float.valueOf(pt4.x));
        v.addLast(Float.valueOf(pt4.y));
        v.addLast(Float.valueOf(pt4.z));
        v.addLast(Float.valueOf(pt4.w));
        n += 4;
        break;
      default:
        v.addLast(Float.valueOf(floatParameter(i)));
        n++;
        if (n == nMax && haveSquare && tokAt(i + 1) == T.rightbrace)
          i++;
      }
      i++;
    }
    if (haveBrace && tokAt(i++) != T.rightbrace || haveSquare
        && tokAt(i++) != T.rightsquare || n < nMin || n > nMax)
      invArg();
    iToken = i - 1;
    return v;
  }

  /**
   * process a general string or set of parameters as an array of floats,
   * allowing for relatively free form input
   * 
   * @param i
   * @param nMin
   * @param nMax
   * @return array of floats
   * @throws ScriptException
   */
  public float[] floatParameterSet(int i, int nMin, int nMax)
      throws ScriptException {
    Lst<Object> v = null;
    float[] fparams = null;
    int n = 0;
    String s = null;
    iToken = i;
    switch (tokAt(i)) {
    case T.string:
      s = SV.sValue(st[i]);
      s = PT.replaceWithCharacter(s, "{},[]\"'", ' ');
      fparams = PT.parseFloatArray(s);
      n = fparams.length;
      break;
    case T.varray:
      fparams = SV.flistValue(st[i], 0);
      n = fparams.length;
      break;
    default:
      v = listParameter(i, nMin, nMax);
      n = v.size();
    }
    if (n < nMin || n > nMax)
      invArg();
    if (fparams == null) {
      fparams = new float[n];
      for (int j = 0; j < n; j++)
        fparams[j] = ((Float) v.get(j)).floatValue();
    }
    return fparams;
  }

  public boolean isArrayParameter(int i) {
    switch (tokAt(i)) {
    case T.varray:
    case T.matrix3f:
    case T.matrix4f:
    case T.spacebeforesquare:
    case T.leftsquare:
      return true;
    }
    return false;
  }

  public Quat getQuaternionParameter(int i) throws ScriptException {
    switch (tokAt(i)) {
    case T.varray:
      Lst<SV> sv = ((SV) getToken(i)).getList();
      P4 p4 = null;
      if (sv.size() == 0 || (p4 = SV.pt4Value(sv.get(0))) == null)
        invArg();
      return Quat.newP4(p4);
    case T.best:
      return (chk ? null : Quat.newP4((P4) Escape.uP(vwr
          .getOrientationText(T.best, null))));
    default:
      return Quat.newP4(getPoint4f(i));
    }
  }

  /*
   * ****************************************************************************
   * ============================================================== checks and
   * parameter retrieval
   * ==============================================================
   */

  public int checkLast(int i) throws ScriptException {
    return checkLength(i + 1) - 1;
  }

  public int checkLength(int length) throws ScriptException {
    if (length >= 0)
      return checkLengthErrorPt(length, 0);
    // max
    if (slen > -length) {
      iToken = -length;
      bad();
    }
    return slen;
  }

  protected int checkLengthErrorPt(int length, int errorPt)
      throws ScriptException {
    if (slen != length) {
      iToken = errorPt > 0 ? errorPt : slen;
      if (errorPt > 0)
        invArg();
      else
        bad();
    }
    return slen;
  }

  public int checkLength23() throws ScriptException {
    iToken = slen;
    if (slen != 2 && slen != 3)
      bad();
    return slen;
  }

  protected int checkLength34() throws ScriptException {
    iToken = slen;
    if (slen != 3 && slen != 4)
      bad();
    return slen;
  }

  public int modelNumberParameter(int index) throws ScriptException {
    int iFrame = 0;
    boolean useModelNumber = false;
    switch (tokAt(index)) {
    case T.integer:
      useModelNumber = true;
      //$FALL-THROUGH$
    case T.decimal:
      iFrame = getToken(index).intValue; // decimal Token intValue is
      // model/frame number encoded
      break;
    case T.string:
      iFrame = getFloatEncodedInt(stringParameter(index));
      break;
    default:
      invArg();
    }
    return vwr.ms.getModelNumberIndex(iFrame, useModelNumber, true);
  }

  public int getMadParameter() throws ScriptException {
    // wireframe, ssbond, hbond, struts
    int mad = 1;
    switch (getToken(1).tok) {
    case T.only:
      restrictSelected(false, false);
      break;
    case T.on:
      break;
    case T.off:
      mad = 0;
      break;
    case T.integer:
      int radiusRasMol = intParameterRange(1, 0, 750);
      mad = radiusRasMol * 4 * 2;
      break;
    case T.decimal:
      float f = floatParameterRange(1, -3, 3);
      mad = (Float.isNaN(f) ? Integer.MAX_VALUE : (int) Math.floor(f * 1000 * 2));
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    return mad;
  }

  public int intParameterRange(int i, int min, int max) throws ScriptException {
    int val = intParameter(i);
    if (val < min || val > max) {
      integerOutOfRange(min, max);
      return Integer.MAX_VALUE;
    }
    return val;
  }

  protected float floatParameterRange(int i, float min, float max)
      throws ScriptException {
    float val = floatParameter(i);
    if (val < min || val > max) {
      numberOutOfRange(min, max);
      return Float.NaN;
    }
    return val;
  }

  public Lst<P3> getPointVector(T t, int i) throws ScriptException {
    switch (t.tok) {
    case T.bitset:
      return vwr.ms.getAtomPointVector((BS) t.value);
    case T.varray:
      Lst<P3> data = new Lst<P3>();
      P3 pt;
      Lst<SV> pts = ((SV) t).getList();
      for (int j = 0; j < pts.size(); j++)
        if ((pt = SV.ptValue(pts.get(j))) != null)
          data.addLast(pt);
        else
          return null;
      return data;
    }
    if (i > 0)
      return vwr.ms.getAtomPointVector(atomExpressionAt(i));
    return null;
  }

  /**
   * Encodes a string such as "2.10" as an integer instead of a float so as to
   * distinguish "2.1" from "2.10" used for model numbers and partial bond
   * orders. 2147483647 is maxvalue, so this allows loading simultaneously up to
   * 2147 files, each with 999999 models (or trajectories)
   * 
   * @param strDecimal
   * @return float encoded as an integer
   */
  static int getFloatEncodedInt(String strDecimal) {
    int pt = strDecimal.indexOf(".");
    if (pt < 1 || strDecimal.charAt(0) == '-' || strDecimal.endsWith(".")
        || strDecimal.contains(".0"))
      return Integer.MAX_VALUE;
    int i = 0;
    int j = 0;
    if (pt > 0) {
      try {
        i = Integer.parseInt(strDecimal.substring(0, pt));
        if (i < 0)
          i = -i;
      } catch (NumberFormatException e) {
        i = -1;
      }
    }
    if (pt < strDecimal.length() - 1)
      try {
        j = Integer.parseInt(strDecimal.substring(pt + 1));
      } catch (NumberFormatException e) {
        // not a problem
      }
    i = i * 1000000 + j;
    return (i < 0 ? Integer.MAX_VALUE : i);
  }

  /**
   * reads standard n.m float-as-integer n*1000000 + m and returns (n % 6) << 5
   * + (m % 0x1F)
   * 
   * @param bondOrderInteger
   * @return Bond order partial mask
   */
  public static int getPartialBondOrderFromFloatEncodedInt(int bondOrderInteger) {
    return (((bondOrderInteger / 1000000) % 6) << 5)
        + ((bondOrderInteger % 1000000) & 0x1F);
  }

  public static int getBondOrderFromString(String s) {
    return (s.indexOf(' ') < 0 ? Edge.getBondOrderFromString(s)
        : s.toLowerCase().indexOf("partial ") == 0 ? getPartialBondOrderFromString(s
            .substring(8).trim()) : Edge.BOND_ORDER_NULL);
  }

  private static int getPartialBondOrderFromString(String s) {
    return getPartialBondOrderFromFloatEncodedInt(getFloatEncodedInt(s));
  }

  public boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == T.navy || tok == T.spacebeforesquare || tok == T.leftsquare
        || tok == T.varray || tok == T.point3f || isPoint3f(i) || (tok == T.string || T
        .tokAttr(tok, T.identifier))
        && CU.getArgbFromString((String) st[i].value) != 0);
  }

  public int getArgbParam(int index) throws ScriptException {
    return getArgbParamOrNone(index, false);
  }

  protected int getArgbParamLast(int index, boolean allowNone)
      throws ScriptException {
    int icolor = getArgbParamOrNone(index, allowNone);
    checkLast(iToken);
    return icolor;
  }

  protected int getArgbParamOrNone(int index, boolean allowNone)
      throws ScriptException {
    P3 pt = null;
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      default:
        if (!T.tokAttr(theTok, T.identifier))
          break;
        //$FALL-THROUGH$
      case T.navy:
      case T.string:
        return CU.getArgbFromString(paramAsStr(index));
      case T.spacebeforesquare:
        return getColorTriad(index + 2);
      case T.leftsquare:
        return getColorTriad(++index);
      case T.varray:
        float[] rgb = SV.flistValue(theToken, 3);
        if (rgb != null && rgb.length != 3)
          pt = P3.new3(rgb[0], rgb[1], rgb[2]);
        break;
      case T.point3f:
        pt = (P3) theToken.value;
        break;
      case T.leftbrace:
        pt = getPoint3f(index, false);
        break;
      case T.none:
        if (allowNone)
          return 0;
      }
    }
    if (pt == null)
      error(ERROR_colorExpected);
    return CU.colorPtToFFRGB(pt);
  }

  private int getColorTriad(int i) throws ScriptException {
    float[] colors = new float[3];
    int n = 0;
    String hex = "";
    getToken(i);
    P3 pt = null;
    float val = 0;
    out: switch (theTok) {
    case T.integer:
    case T.spec_seqcode:
    case T.decimal:
      for (; i < slen; i++) {
        switch (getToken(i).tok) {
        case T.comma:
          continue;
        case T.identifier:
          if (n != 1 || colors[0] != 0)
            error(ERROR_badRGBColor);
          hex = "0" + paramAsStr(i);
          break out;
        case T.decimal:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = floatParameter(i);
          break;
        case T.integer:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = theToken.intValue;
          break;
        case T.spec_seqcode:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = ((Integer) theToken.value).intValue() % 256;
          break;
        case T.rightsquare:
          if (n != 3)
            error(ERROR_badRGBColor);
          --i;
          pt = P3.new3(colors[0], colors[1], colors[2]);
          break out;
        default:
          error(ERROR_badRGBColor);
        }
        colors[n++] = val;
      }
      error(ERROR_badRGBColor);
      break;
    case T.point3f:
      pt = (P3) theToken.value;
      break;
    case T.identifier:
      hex = paramAsStr(i);
      break;
    default:
      error(ERROR_badRGBColor);
    }
    if (getToken(++i).tok != T.rightsquare)
      error(ERROR_badRGBColor);
    if (pt != null)
      return CU.colorPtToFFRGB(pt);
    if ((n = CU.getArgbFromString("[" + hex + "]")) == 0)
      error(ERROR_badRGBColor);
    return n;
  }

  /**
   * 
   * @param index
   * @param allowUnitCell
   *        IGNORED
   * @param allowScale
   * @param allowFirst
   * @return TickInfo
   * @throws ScriptException
   */
  public TickInfo tickParamAsStr(int index, boolean allowUnitCell,
                             boolean allowScale, boolean allowFirst)
      throws ScriptException {
    iToken = index - 1;
    if (tokAt(index) != T.ticks)
      return null;
    TickInfo tickInfo;
    String str = " ";
    switch (tokAt(index + 1)) {
    case T.x:
    case T.y:
    case T.z:
      str = paramAsStr(++index).toLowerCase();
      break;
    case T.identifier:
      invArg();
    }
    if (tokAt(++index) == T.none) {
      tickInfo = new TickInfo(null);
      tickInfo.type = str;
      iToken = index;
      return tickInfo;
    }
    tickInfo = new TickInfo((P3) getPointOrPlane(index, false, true, false,
        false, 3, 3));
    if (coordinatesAreFractional || tokAt(iToken + 1) == T.unitcell) {
      tickInfo.scale = P3.new3(Float.NaN, Float.NaN, Float.NaN);
      allowScale = false;
    }
    if (tokAt(iToken + 1) == T.unitcell)
      iToken++;
    tickInfo.type = str;
    if (tokAt(iToken + 1) == T.format)
      tickInfo.tickLabelFormats = stringParameterSet(iToken + 2);
    if (!allowScale)
      return tickInfo;
    if (tokAt(iToken + 1) == T.scale) {
      if (isFloatParameter(iToken + 2)) {
        float f = floatParameter(iToken + 2);
        tickInfo.scale = P3.new3(f, f, f);
      } else {
        tickInfo.scale = getPoint3f(iToken + 2, true);
      }
    }
    if (allowFirst)
      if (tokAt(iToken + 1) == T.first)
        tickInfo.first = floatParameter(iToken + 2);
    // POINT {x,y,z} reference point not implemented
    //if (tokAt(iToken + 1) == Token.point)
    // tickInfo.reference = centerParameter(iToken + 2);
    return tickInfo;
  }

  ////////////////////  setting global parameters ////////////////////////

  public void setBooleanProperty(String key, boolean value) {
    if (!chk)
      vwr.setBooleanProperty(key, value);
  }

  protected boolean setIntProperty(String key, int value) {
    if (!chk)
      vwr.setIntProperty(key, value);
    return true;
  }

  protected boolean setFloatProperty(String key, float value) {
    if (!chk)
      vwr.setFloatProperty(key, value);
    return true;
  }

  protected void setStringProperty(String key, String value) {
    if (!chk)
      vwr.setStringProperty(key, value);
  }



}
