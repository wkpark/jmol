/*
 * Copyright stuff
 */

var _jmol = {
debugAlert: false,
bgcolor: "black",
progresscolor: "blue",
boxbgcolor: "black",
boxfgcolor: "white",
boxmessage: "Downloading JmolApplet ...",

codebase: ".",
modelbase: "."
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

function jmolApplet(size, model, script, name) {
  var nm = "jmol" + (name ? name : "");
  var sz = _jmolGetAppletSize(size);
  var t;
  t = "<applet name='" + nm + "' id='" + nm + 
      "' code='JmolApplet' archive='JmolApplet.jar'\n" +
      "  codebase=" + _jmol.codebase + "\n" +
      "  width='" + sz[0] + "' height='" + sz[1] +
      "' mayscript='true'>\n" +
      "  <param name='progressbar' value='true' />\n" +
      "  <param name='progresscolor' value='" +
      _jmol.progresscolor + "' />\n" +
      "  <param name='boxmessage' value='" +
      _jmol.boxmessage + "' />\n" +
      "  <param name='boxbgcolor' value='" +
      _jmol.boxbgcolor + "' />\n" +
      "  <param name='boxfgcolor' value='" +
      _jmol.boxfgcolor + "' />\n" +
      "  <param name='bgcolor' value='" + _jmol.bgcolor + "' />\n";

  if (model)
    t += "  <param name='load' value='" +
         _jmol.modelbase + "/" + model + "' />\n";
  if (script)
    t += "  <param name='script' value='" + script + "' />\n";
  t += "</applet>\n";
  if (_jmol.debugAlert)
    alert("jmolApplet(" + size + "," + model + "," + script + "," + name +
          " ->\n" + t);
  document.open(); // NS4 compatibility
  document.write(t);
  document.close(); // NS4 compatibility
}

function jmolScript(script, name) {
  if (script) {
    var nm = "jmol" + (name ? name : "");
    var applet;
    if (document.getElementById)
      applet = document.getElementById(nm);
    else
      applet = document[nm]; // NS4 compatibility
    applet.script(script);
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
