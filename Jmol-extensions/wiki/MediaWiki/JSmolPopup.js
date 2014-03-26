var JSmolCloneData = {};
function cloneJSmol(JSmolObject) {
  var t = JSmolObject._jmolType; 
  if ( /_Canvas2D/.test(t) ) { t = 'HTML5'; }
  else if ( /_Canvas3D/.test(t) ) { t = 'WebGL'; }
  else if ( /_Applet/.test(t) ) { t = 'Java'; }
  else { t = null; }
  JSmolCloneData.type = t;
  JSmolCloneData.platformSpeed = Jmol.evaluate(JSmolObject, 'platformSpeed + 0');
  JSmolCloneData.state = Jmol.getPropertyAsString(JSmolObject, 'stateInfo');
  w = window.open('/wiki/extensions/jsmol/wiki/JSmolPopup.htm','JSmolPopup','resizable, width=500, height=500');
  w.focus();
}

function setupCheckboxShiftClick () {
  return;
  /* overrides the homonymous function in wikibits.js fixing Jmol checkbox - Angel Herraez */
}
