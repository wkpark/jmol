/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

function jmolDebugAlert(enabled) {
  _jmol.debugAlert = (enabled == undefined || enabled)
}

function jmolSetColor(bgcolor, boxfgcolor, progresscolor, boxbgcolor) {
  _jmol.bgcolor = bgcolor;
  _jmol.boxbgcolor = boxbgcolor ? boxbgcolor : bgcolor;
  if (boxfgcolor)
    _jmol.boxfgcolor = boxfgcolor
  else if (_jmol.boxbgcolor == "white" || _jmol.boxbgcolor == "#FFFFFF")
    _jmol.boxfgcolor = "black";
  else
    _jmol.boxfgcolor = "white";
  if (_jmol.debugAlert)
    alert(" bgcolor=" + _jmol.bgcolor +
          " boxbgcolor=" + _jmol.boxbgcolor +
          " boxfgcolor=" + _jmol.boxfgcolor +
          " progresscolor=" + _jmol.progresscolor);

}

function jmolSetCodebase(codebase) {
  _jmol.codebase = codebase ? codebase : ".";
  if (_jmol.debugAlert)
    alert("jmolCodebase=" + _jmol.codebas);
}

function _jmolApplet(size, modelFilename, inlineModel, script, nameSuffix) {
  with (_jmol) {
    if (! nameSuffix)
      nameSuffix = appletCount;
    ++appletCount;
    if (! script)
      script = "select *";
    var sz = _jmolGetAppletSize(size);
    var t;
    t = "<applet name='jmol" + nameSuffix + "' id='jmol" + nameSuffix + 
        "' code='JmolApplet' archive='JmolApplet.jar'\n" +
        "  codebase=" + codebase + "\n" +
        "  width='" + sz[0] + "' height='" + sz[1] +
        "' mayscript='true'>\n" +
        "  <param name='progressbar' value='true' />\n" +
        "  <param name='progresscolor' value='" +
        progresscolor + "' />\n" +
        "  <param name='boxmessage' value='" +
        boxmessage + "' />\n" +
        "  <param name='boxbgcolor' value='" +
        boxbgcolor + "' />\n" +
        "  <param name='boxfgcolor' value='" +
        boxfgcolor + "' />\n" +
        "  <param name='bgcolor' value='" + bgcolor + "' />\n";

    if (modelFilename)
      t += "  <param name='load' value='" +
           modelbase + "/" + modelFilename + "' />\n";
    else if (inlineModel)
      t += "  <param name='inline' value='" + inlineModel + "' />\n";
    if (script)
      t += "  <param name='script' value='" + script + "' />\n";
    t += "</applet>\n";
    jmolSetTarget(nameSuffix);
    if (debugAlert)
      alert("jmolApplet(" + size + "," + modelFilename + "," +
            script + "," + name + " ->\n" + t);
    document.open(); // NS4 compatibility
    document.write(t);
    document.close(); // NS4 compatibility
  }
}

function jmolApplet(size, modelFilename, script, nameSuffix) {
  _jmolApplet(size, modelFilename, null, script, nameSuffix);
}

function jmolAppletInline(size, inlineModel, script, nameSuffix) {
  _jmolApplet(size, null, _jmolConvertInline(inlineModel), script, nameSuffix);
}

function _jmolConvertInline(model) {
  return model.replace(/\r|\n|\r\n/g, "|");
}

function jmolScript(script, targetSuffix) {
  if (script) {
    var target = "jmol" + (targetSuffix ? targetSuffix : "0");
    if (document.getElementById)
      document.getElementById(target).script(script);
    else
      document[target].script(script); // NS4 compatibility
  }
}

function jmolLoadInline(model, targetSuffix) {
  if (model) {
    var target = "jmol" + (targetSuffix ? targetSuffix : "0");
    var applet;
    if (document.getElementById)
      applet = document.getElementById(target);
    else
      applet = document[target]; // NS4 compatibility
    applet.loadInline(model);
  }
}

function jmolSetTarget(targetSuffix) {
  _jmol.targetSuffix = targetSuffix;
  _jmol.targetText = targetSuffix ? ",\"" + targetSuffix + "\"" : "";
}

function jmolButton(script, label, cssClass) {
  var scriptIndex = _jmolAddScript(script);
  var cssText = cssClass ? "class='" + cssClass + "' " : "";
  if (! label)
    label = script.substring(0, 32);
  var t = "<input type='button' value='" + label +
          "' onClick='_jmolClick(" + scriptIndex + _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          cssText + "/>";
  document.open();
  document.write(t);
  document.close();
}

function jmolCheckbox(scriptWhenChecked, scriptWhenUnchecked,
                      isChecked, cssClass) {
  if (! scriptWhenChecked && scriptWhenUnchecked) {
    alert("jmolCheckbox requires two scripts");
    return;
  }
  var indexChecked = _jmolAddScript(scriptWhenChecked);
  var indexUnchecked = _jmolAddScript(scriptWhenUnchecked);
  var cssText = cssClass ? "class='" + cssClass + "' " : "";
  var t = "<input type='checkbox' onClick='_jmolCbClick(this," +
          indexChecked + "," + indexUnchecked + "," + _jmol.targetSuffix +
          ")' onMouseover='_jmolCbOver(this," + indexChecked + "," + indexUnchecked +
          ");return true' onMouseout='_jmolMouseOut()' " +
	  (isChecked ? "checked " : "") + cssText + "/>";
  document.open();
  document.write(t);
  document.close();
}

function jmolRadio(groupName, script, isChecked, cssClass) {
  if (!groupName || !script)
    return;
  var scriptIndex = _jmolAddScript(script);
  var cssText = cssClass ? "class='" + cssClass + "' " : "";
  var t = "<input name='" + groupName +
          "' type='radio' onClick='_jmolClick(" + scriptIndex + _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
	  (isChecked ? "checked " : "") + cssText + "/>";
  document.open();
  document.write(t);
  document.close();
}

function jmolLink(text, script, cssClass) {
  var scriptIndex = _jmolAddScript(script);
  var cssText = cssClass ? "class='" + cssClass + "' " : "";
  var t = "<a href='javascript:_jmolClick(" + scriptIndex + _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          cssText + ">" + text + "</a>";
  document.open();
  document.write(t);
  document.close();
}

////////////////////////////////////////////////////////////////
// functions for INTERNAL USE ONLY which are subject to change
// use at your own risk ... you have been WARNED!
////////////////////////////////////////////////////////////////

var _jmol = {
debugAlert: false,
bgcolor: "black",
progresscolor: "blue",
boxbgcolor: "black",
boxfgcolor: "white",
boxmessage: "Downloading JmolApplet ...",

codebase: ".",
modelbase: ".",

appletCount: 0,

targetSuffix: 0,
targetText: "",
scripts: []
};

function _jmolGetAppletSize(size) {
  var width, height;
  var type = typeof size;
  if (type == "number")
    width = height = size;
  else if (type == "object" && size != null) {
    width = size[0]; height = size[1];
  }
  if (! (width >= 25 && width <= 2000))
    width = 300;
  if (! (height >= 25 && height <= 2000))
    height = 300;
  return [width, height];
}

function _jmolAddScript(script) {
  var index = _jmol.scripts.length;
  _jmol.scripts[index] = script;
  return index;
}

function _jmolClick(scriptIndex, targetSuffix) {
  jmolScript(_jmol.scripts[scriptIndex], targetSuffix);
}

function _jmolCbClick(ckbox, whenChecked, whenUnchecked, targetSuffix) {
  jmolScript(_jmol.scripts[ckbox.checked ? whenChecked : whenUnchecked],
             targetSuffix);
}

function _jmolCbOver(ckbox, whenChecked, whenUnchecked) {
  window.status = _jmol.scripts[ckbox.checked ? whenUnchecked : whenChecked];
}

function _jmolMouseOver(scriptIndex) {
  window.status = _jmol.scripts[scriptIndex];
}

function _jmolMouseOut() {
  window.status = " ";
  return true;
}
