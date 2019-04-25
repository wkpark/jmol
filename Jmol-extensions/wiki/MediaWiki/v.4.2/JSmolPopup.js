// Jaime Prilusky version 2.02 2014
var JSmolCloneData = {};

function popupJSmol(JSmolObject,script,wname) {
  var t = JSmolObject._jmolType; 
  if(!wname) { wname = 'JSmolPopup'; }
  if ( /_Canvas2D/.test(t) ) { t = 'HTML5'; }
  else if ( /_Canvas3D/.test(t) ) { t = 'WebGL'; }
  else if ( /_Applet/.test(t) ) { t = 'Java'; }
  else { t = 'HTML5'; } // null; }
  JSmolCloneData.type = t; JSmolCloneData.platformSpeed = 10;
  try { JSmolCloneData.platformSpeed = Jmol.evaluate(JSmolObject, 'platformSpeed + 0'); }
  catch(err) {}
  if (/^MSG /.test(script)) { 
    script = script.replace("MSG ","");
    JSmolCloneData.loadFile = "set echo off; set echo loading 50% 50%; set echo loading center; color echo [xff0000]; echo " + script + "; refresh;"; 
  } else {
    JSmolCloneData.loadFile = 'load ' + script;
  }
  JSmolCloneData.name = wname;
  w = window.open('/wiki/extensions/jsmol/wiki/JSmolSingPopup.htm',wname,'resizable, width=500, height=500');
  w.focus();
}

function cloneJSmol(JSmolObject,wname) {
  var t = JSmolObject._jmolType; 
  if(!wname) { wname = 'JSmolPopup'; }
  if ( /_Canvas2D/.test(t) ) { t = 'HTML5'; }
  else if ( /_Canvas3D/.test(t) ) { t = 'WebGL'; }
  else if ( /_Applet/.test(t) ) { t = 'Java'; }
  else { t = null; }
  JSmolCloneData.type = t;
  JSmolCloneData.name = wname;
  JSmolCloneData.platformSpeed = Jmol.evaluate(JSmolObject, 'platformSpeed + 0');
  JSmolCloneData.state = Jmol.getPropertyAsString(JSmolObject, 'stateInfo');
  w = window.open('/wiki/extensions/jsmol/wiki/JSmolPopup.htm',wname,'resizable, width=500, height=500');
  w.focus();
}

function setupCheckboxShiftClick () {
  return;
  /* prevents the function in wikibits.js to allow checkbox  - Angel Herraez */
}

