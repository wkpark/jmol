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

scripts: []
};

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

function jmolSetCodeBase(codeBase) {
  _jmol.codebase = codeBase ? codebase : ".";
  if (_jmol.debugAlert)
    alert("jmolCodeBase=" + jmolCodeBase);
}

function jmolApplet(size, modelFilename, script, nameSuffix) {
  with (_jmol) {
    var nm = "jmol" + (nameSuffix ? nameSuffix : appletCount);
    ++appletCount;
    var sz = _jmolGetAppletSize(size);
    var t;
    t = "<applet name='" + nm + "' id='" + nm + 
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
    if (script)
      t += "  <param name='script' value='" + script + "' />\n";
    t += "</applet>\n";
    if (debugAlert)
      alert("jmolApplet(" + size + "," + modelFilename + "," +
            script + "," + name + " ->\n" + t);
    document.open(); // NS4 compatibility
    document.write(t);
    document.close(); // NS4 compatibility
  }
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

function jmolButton(script, label, targetSuffix) {
  var scriptIndex = _jmolAddScript(script);
  var targetText = targetSuffix ? ",\"" + targetSuffix + "\"" : "";
  if (! label)
    label = script.substring(0, 32);
  var t = "<input type='button' value='" + label +
          "' onClick='jmolScript(_jmol.scripts[" +
          scriptIndex + "]" + targetText + ")' />";
  document.open();
  document.write(t);
  document.close();
}

function jmolCheckbox(scriptWhenChecked, scriptWhenUnchecked, targetSuffix) {
  if (! scriptWhenChecked && scriptWhenUnchecked) {
    alert("jmolCheckbox requires two scripts");
    return;
  }
  var scriptChecked = _jmolAddScript(scriptWhenChecked);
  var scriptUnchecked = _jmolAddScript(scriptWhenUnchecked);
  var t = "<input type='checkbox' onClick='_jmolCheckboxEvent(this," +
          scriptChecked + "," + scriptUnchecked + "," + targetSuffix + ")' />";
  document.open();
  document.write(t);
  document.close();
}

function _jmolCheckboxEvent(ckbox, whenChecked, whenUnchecked, targetSuffix) {
  jmolScript(_jmol.scripts[ckbox.checked ? whenChecked : whenUnchecked],
             targetSuffix);
}

