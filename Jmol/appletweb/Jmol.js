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

// for documentation see www.jmol.org/jslibrary

////////////////////////////////////////////////////////////////
// Basic Scripting infrastruture
////////////////////////////////////////////////////////////////

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

////////////////////////////////////////////////////////////////
// Basic controls
////////////////////////////////////////////////////////////////

function jmolButton(script, label) {
  var scriptIndex = _jmolAddScript(script);
  if (label == undefined || label == null)
    label = script.substring(0, 32);
  var t = "<input type='button' value='" + label +
          "' onClick='_jmolClick(" + scriptIndex + _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          _jmol.buttonCssText + "/>";
  if (_jmol.debugAlert)
    alert(t);
  document.write(t);
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
          indexChecked + "," + indexUnchecked + _jmol.targetText +
          ")' onMouseover='_jmolCbOver(this," + indexChecked + "," +
          indexUnchecked +
          ");return true' onMouseout='_jmolMouseOut()' " +
	  (isChecked ? "checked " : "") + _jmol.checkboxCssText + "/>" +
          labelHtml;
  if (_jmol.debugAlert)
    alert(t);
  document.write(t);
}

function jmolRadioGroup(arrayOfRadioButtons, separatorHtml) {
  var type = typeof arrayOfRadioButtons;
  if (type != "object" || type == null || ! arrayOfRadioButtons.length) {
    alert("invalid arrayOfRadioButtons");
    return;
  }
  if (separatorHtml == undefined || separatorHtml == null)
    separatorHtml = "&nbsp; ";
  jmolStartNewRadioGroup();
  var length = arrayOfRadioButtons.length;
  var t = "";
  for (var i = 0; i < length; ++i) {
    var radio = arrayOfRadioButtons[i];
    type = typeof radio;
    if (type == "object") {
      t += _jmolRadio(radio[0], radio[1], radio[2], separatorHtml);
    } else {
      t += _jmolRadio(radio, null, null, separatorHtml);
    }
  }
  if (_jmol.debugAlert)
    alert(t);
  document.write(t);
}

function jmolLink(script, text) {
  var scriptIndex = _jmolAddScript(script);
  var t = "<a href='javascript:_jmolClick(" + scriptIndex +
          _jmol.targetText +
          ")' onMouseover='_jmolMouseOver(" + scriptIndex +
          ");return true' onMouseout='_jmolMouseOut()' " +
          _jmol.linkCssText + ">" + text + "</a>";
  if (_jmol.debugAlert)
    alert(t);
  document.write(t);
}

function jmolMenu(arrayOfMenuItems, size) {
  var type = typeof arrayOfMenuItems;
  if (type == "object" && type != null && arrayOfMenuItems.length) {
    var length = arrayOfMenuItems.length;
    if (typeof size != "number" || size == 1)
      size = null;
    else if (size < 0)
      size = length;
    var sizeText = size ? " size='" + size + "' " : "";
    var t = "<select name='" + _jmol.menuGroupCount++ +
            "' onChange='_jmolMenuSelected(this" +
            _jmol.targetText + ")'" +
            sizeText + _jmol.menuCssText + ">";
    for (var i = 0; i < length; ++i) {
      var menuItem = arrayOfMenuItems[i];
      type = typeof menuItem;
      var script, text;
      var isSelected = undefined;
      if (type == "object" && menuItem != null) {
        script = menuItem[0];
        text = menuItem[1];
        isSelected = menuItem[2];
      } else {
        script = text = menuItem;
      }
      if (text == undefined || text == null)
        text = script;
      var scriptIndex = _jmolAddScript(script);
      var selectedText = isSelected ? "' selected>" : "'>";
      t += "<option value='" + scriptIndex + selectedText + text + "</option>";
    }
    t += "</select>";
    if (_jmol.debugAlert)
      alert(t);
    document.write(t);
  }
}

function jmolHtml(html) {
  document.write(html);
}

function jmolBr() {
  document.write("<br />");
}

////////////////////////////////////////////////////////////////
// advanced scripting functions
////////////////////////////////////////////////////////////////

function jmolDebugAlert(enableAlerts) {
  _jmol.debugAlert = (enableAlerts == undefined || enableAlerts)
}

function jmolAppletInline(size, inlineModel, script, nameSuffix) {
  _jmolApplet(size, null, _jmolConvertInline(inlineModel), script, nameSuffix);
}

function jmolSetTarget(targetSuffix) {
  _jmol.targetSuffix = targetSuffix;
  _jmol.targetText = targetSuffix ? ",\"" + targetSuffix + "\"" : "";
}

function jmolScript(script, targetSuffix) {
  if (script) {
    var target = "jmol-" + (targetSuffix ? targetSuffix : "0");
    var applet = _jmolFindApplet(target);
    if (applet)
      return applet.script(script);
    else
      alert("could not find applet " + target);
  }
}

function jmolLoadInline(model, targetSuffix) {
  if (model) {
    var target = "jmol-" + (targetSuffix ? targetSuffix : "0");
    var applet = _jmolFindApplet(target);
    if (applet)
      return applet.loadInline(model);
    else
      alert("could not find applet " + target);
  }
}

function jmolStartNewRadioGroup() {
  ++_jmol.radioGroupCount;
}

function jmolRadio(script, labelHtml, isChecked, separatorHtml) {
  var t = _jmolRadio(script, labelHtml, isChecked, separatorHtml);
  if (_jmol.debugAlert)
    alert(t);
  document.write(t);
}

////////////////////////////////////////////////////////////////
// Cascading Style Sheet Class support
////////////////////////////////////////////////////////////////
function jmolSetAppletCssClass(appletCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.appletCssClass = appletCssClass;
    _jmol.appletCssText = appletCssClass ? "class='" + appletCssClass + "' " : "";
  }
}

function jmolSetButtonCssClass(buttonCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.buttonCssClass = buttonCssClass;
    _jmol.buttonCssText = buttonCssClass ? "class='" + buttonCssClass + "' " : "";
  }
}

function jmolSetCheckboxCssClass(checkboxCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.checkboxCssClass = checkboxCssClass;
    _jmol.checkboxCssText = checkboxCssClass ? "class='" + checkboxCssClass + "' " : "";
  }
}

function jmolSetRadioCssClass(radioCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.radioCssClass = radioCssClass;
    _jmol.radioCssText = radioCssClass ? "class='" + radioCssClass + "' " : "";
  }
}

function jmolSetLinkCssClass(linkCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.linkCssClass = linkCssClass;
    _jmol.linkCssText = linkCssClass ? "class='" + linkCssClass + "' " : "";
  }
}

function jmolSetMenuCssClass(menuCssClass) {
  if (_jmol.modernBrowser) {
    _jmol.menuCssClass = menuCssClass;
    _jmol.menuCssText = menuCssClass ? "class='" + menuCssClass + "' " : "";
  }
}

////////////////////////////////////////////////////////////////
// functions for INTERNAL USE ONLY which are subject to change
// use at your own risk ... you have been WARNED!
////////////////////////////////////////////////////////////////

var _jmol = {
modernBrowser: !!document.getElementById,

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
menuGroupCount: 0,

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
menuCssClass: null,
menuCssText: "",

targetSuffix: 0,
targetText: "",
scripts: [""]
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
    t = "<applet name='jmol-" + nameSuffix + "' id='jmol-" + nameSuffix +
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
    if (_jmol.debugAlert)
      alert(t);
    document.write(t);
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

function _jmolRadio(script, labelHtml, isChecked, separatorHtml) {
  if (!script)
    return "";
  if (labelHtml == undefined || labelHtml == null)
    labelHtml = script.substring(0, 32);
  if (! separatorHtml)
    separatorHtml = "";
  var scriptIndex = _jmolAddScript(script);
  return "<input name='" + "jmolGroup" + _jmol.radioGroupCount +
         "' type='radio' onClick='_jmolClick(" + scriptIndex +
         _jmol.targetText +
         ")' onMouseover='_jmolMouseOver(" + scriptIndex +
         ");return true' onMouseout='_jmolMouseOut()' " +
	 (isChecked ? "checked " : "") + _jmol.radioCssText + "/>" +
         labelHtml + separatorHtml;
}

function _jmolFindApplet(target) {
  // first look for the target in the current window
  var applet = _jmolSearchFrames(window, target);
  if (applet == undefined)
    applet = _jmolSearchFrames(top, target); // look starting in top frame
  return applet;
}

function _jmolSearchFrames(win, target) {
  var applet;
  var frames = win.frames;
  if (frames && frames.length) { // look in all the frames below this window
    for (var i = 0; i < frames.length; ++i) {
      applet = _jmolSearchFrames(frames[i++], target);
      if (applet)
        break;
    }
  } else { // look for the applet in this window
    var doc = win.document;
    if (doc.getElementById) // this is the DOM way to do it
      applet = doc.getElementById(target);
    else if (doc.applets) // NS4 compatibility
      applet = doc.applets[target];
    else // this also works on NS4 & may help with some other old browsers
      applet = doc[target];
  }
  return applet;
}

function _jmolAddScript(script) {
  if (! script)
    return 0;
  var index = _jmol.scripts.length;
  _jmol.scripts[index] = script;
  return index;
}

function _jmolClick(scriptIndex, targetSuffix) {
  jmolScript(_jmol.scripts[scriptIndex], targetSuffix);
}

function _jmolMenuSelected(menuObject, targetSuffix) {
  var scriptIndex = menuObject.value;
  if (scriptIndex != undefined) {
    jmolScript(_jmol.scripts[scriptIndex], targetSuffix);
    return;
  }
  var length = menuObject.length;
  if (typeof length == "number") {
    for (var i = 0; i < length; ++i) {
      if (menuObject[i].selected) {
        _jmolClick(menuObject[i].value, targetSuffix);
	return;
      }
    }
  }
  _jmolOldBrowser();
}

function _jmolCbClick(ckbox, whenChecked, whenUnchecked, targetSuffix) {
  _jmolClick(ckbox.checked ? whenChecked : whenUnchecked, targetSuffix);
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

function _jmolOldBrowser() {
  alert("Your outdated web browser does not support this operation");
}
