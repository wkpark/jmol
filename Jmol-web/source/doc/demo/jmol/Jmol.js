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

function jmolDebugAlert(enableAlerts) {
  _jmol.debugAlert = (enableAlerts == undefined || enableAlerts)
}

function jmolSetCodebase(codebase) {
  _jmol.codebase = codebase ? codebase : ".";
  if (_jmol.debugAlert)
    alert("jmolCodebase=" + _jmol.codebase);
}

function jmolSetAppletColor(bgcolor, boxfgcolor, progresscolor, boxbgcolor) {
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

function jmolApplet(size, modelFilename, script, nameSuffix) {
  _jmolApplet(size, modelFilename, null, script, nameSuffix);
}

function jmolButton(script, label) {
  var scriptIndex = _jmolAddScript(script);
  if (! label)
    label = script.substring(0, 32);
  var t = "<input type='button' value='" + label +
          "' onClick='_jmolClick(" + scriptIndex + _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          _jmol.buttonCssText + "/>";
  document.open();
  document.write(t);
  document.close();
}

function jmolCheckbox(scriptWhenChecked, scriptWhenUnchecked,
                      labelHtml, isChecked) {
  if (scriptWhenChecked == undefined || scriptWhenChecked == null ||
      scriptWhenUnchecked == undefined || scriptWhenUnchecked == null) {
    alert("jmolCheckbox requires two scripts");
    return;
  }
  if (labelHtml == undefined || labelHtml == null) {
    alert("jmolCheckbox requires a label");
    return;
  }
  var indexChecked = _jmolAddScript(scriptWhenChecked);
  var indexUnchecked = _jmolAddScript(scriptWhenUnchecked);
  var t = "<input type='checkbox' onClick='_jmolCbClick(this," +
          indexChecked + "," + indexUnchecked + "," + _jmol.targetSuffix +
          ")' onMouseover='_jmolCbOver(this," + indexChecked + "," +
          indexUnchecked +
          ");return true' onMouseout='_jmolMouseOut()' " +
	  (isChecked ? "checked " : "") + _jmol.checkboxCssText + "/>" +
          labelHtml;
  document.open();
  document.write(t);
  document.close();
}

function jmolRadioGroup(arrayOfRadioButtons, separatorHtml) {
  var type = typeof arrayOfRadioButtons;
  if (type == "object" && type != null) {
    if (separatorHtml == undefined || separatorHtml == null)
      separatorHtml = "&nbsp; ";
    jmolStartRadioGroup();
    var length = arrayOfRadioButtons.length;
    var i;
    for (i = 0; i < length; ++i) {
      var radio = arrayOfRadioButtons[i];
      type = typeof radio;
      if (type == "object") {
        jmolRadio(radio[0], radio[1], radio[2], separatorHtml);
      } else {
        jmolRadio(radio, null, null, separatorHtml);
      }
    }
  }
}

function jmolLink(script, text) {
  var scriptIndex = _jmolAddScript(script);
  var t = "<a href='javascript:_jmolClick(" + scriptIndex +
          _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          _jmol.linkCssText + ">" + text + "</a>";
  document.open();
  document.write(t);
  document.close();
}

function jmolHtml(html) {
  document.open();
  document.write(html);
  document.close();
}

function jmolBr() {
  document.open();
  document.write("<br />");
  document.close();
}

////////////////////////////////////////////////////////////////
// advanced scripting functions
////////////////////////////////////////////////////////////////

function jmolAppletInline(size, inlineModel, script, nameSuffix) {
  _jmolApplet(size, null, _jmolConvertInline(inlineModel), script, nameSuffix);
}

function jmolSetTarget(targetSuffix) {
  _jmol.targetSuffix = targetSuffix;
  _jmol.targetText = targetSuffix ? ",\"" + targetSuffix + "\"" : "";
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

function jmolStartRadioGroup() {
  ++_jmol.radioGroupCount;
}

function jmolRadio(script, labelHtml, isChecked, separatorHtml) {
  if (!script)
    return;
  if (labelHtml == undefined || labelHtml == null)
    labelHtml = script.substring(0, 32);
  if (! separatorHtml)
    separatorHtml = "";
  var scriptIndex = _jmolAddScript(script);
  var t = "<input name='" + "jmolGroup" + _jmol.radioGroupCount +
          "' type='radio' onClick='_jmolClick(" + scriptIndex +
          _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
	  (isChecked ? "checked " : "") + _jmol.radioCssText + "/>" +
          labelHtml + separatorHtml;
  document.open();
  document.write(t);
  document.close();
}

////////////////////////////////////////////////////////////////
// Cascading Style Sheet Class support
////////////////////////////////////////////////////////////////
function jmolSetAppletCssClass(appletCssClass) {
  _jmol.appletCssClass = appletCssClass;
  _jmol.appletCssText =
    appletCssClass ? "class='" + appletCssClass + "' " : "";
}

function jmolSetButtonCssClass(buttonCssClass) {
  _jmol.buttonCssClass = buttonCssClass;
  _jmol.buttonCssText =
    buttonCssClass ? "class='" + buttonCssClass + "' " : "";
}

function jmolSetCheckboxCssClass(checkboxCssClass) {
  _jmol.checkboxCssClass = checkboxCssClass;
  _jmol.checkboxCssText =
    checkboxCssClass ? "class='" + checkboxCssClass + "' " : "";
}

function jmolSetRadioCssClass(radioCssClass) {
  _jmol.radioCssClass = radioCssClass;
  _jmol.radioCssText = radioCssClass ? "class='" + radioCssClass + "' " : "";
}

function jmolSetLinkCssClass(linkCssClass) {
  _jmol.linkCssClass = linkCssClass;
  _jmol.linkCssText = linkCssClass ? "class='" + linkCssClass + "' " : "";
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

radioGroupCount: 0,

appletCssClass: null,
appletCssText: "",
buttonCssClass: null,
buttonCssText: "",
checkboxCssClass: null,
checkboxCssText: "",
radioCssClass: null,
radioCssText: "",
linkCssClass: null,
linkCssText: "",

targetSuffix: 0,
targetText: "",
scripts: []
};

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
        "' " + appletCssClass +
        " code='JmolApplet' archive='JmolApplet.jar'\n" +
        " codebase='" + codebase + "'\n" +
        " width='" + sz[0] + "' height='" + sz[1] +
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

function _jmolConvertInline(model) {
  var inlineModel = model.replace(/\r|\n|\r\n/g, "|");
  if (_jmol.debugAlert)
    alert("inline model:\n" + inlineModel);
  return inlineModel;
}

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
