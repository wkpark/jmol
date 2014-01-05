var JSmolCloneData = {};
function cloneJSmol(JSmolObject) {
  var t = JSmolObject._jmolType; 
  JSmolCloneData.type = t;
  JSmolCloneData.state = Jmol.getPropertyAsString(JSmolObject, 'stateInfo');
  window.open('/wiki/extensions/jsmol/wiki/JSmolPopup.htm','JSmolPopup','resizable, width=500, height=500');
}

