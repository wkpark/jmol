// Jaime Prilusky version 2.02 2014
// Angel Herráez version 5.0, 2019
var JSmolCloneData = {};

function popupJSmol(dat) {
	var wname,spt,wW,wH,wparm,wn;
	JSmolCloneData = {};
  JSmolCloneData.type = (dat.type ? dat.type.trim() : 'HTML5'); 
	JSmolCloneData.platformSpeed = (dat.pspeed ? dat.pspeed : 8);
  JSmolCloneData.tit = (dat.tit ? dat.tit.trim() : '');
  JSmolCloneData.cap = (dat.cap ? dat.cap.trim() : '');
  wname = (dat.wname ? dat.wname.trim() : 'JSmolPopup');
  spt = (dat.spt ? dat.spt.trim() : '');	
  if (/^MSG /.test(spt)) { 
    spt = spt.replace("MSG ","");
    JSmolCloneData.loadFile = "set echo off; set echo loading 50% 50%; set echo loading center; color echo [xff0000]; echo " + spt + "; refresh;"; 
  } else {
    JSmolCloneData.loadFile = 'load "' + spt + '";';
  }
	wW = 500; wH = 500;
	wparm = 'resizable, width=' + wW + ', height=' + wH;
	if (dat.x) {
		if (dat.x + wW > window.innerWidth) { dat.x = window.innerWidth - wW - 5; }
		wparm += ',left=' + dat.x; 
	}
	if (dat.y) { 
		if (dat.y + wH > window.innerHeight) { dat.y = window.innerHeight - wH - 5; }
		wparm += ',top=' + dat.y; 
	}
	
	wn = 'popWin_'+wname;
	closePopupIfOpen(wn); // closing the window first allows to reposition the new instance with same name
	window[wn] = window.open(extensionDir + 'JSmolPopup.htm',wname,wparm); 
	//creates a global variable with the name 'popWin_XXX', so it can be polled next time
}

function cloneJSmol(JSmolObject,wname) {
	var t,w,wW,wH,wparm;
  t = JSmolObject._jmolType; 
  if (!wname) { wname = 'JSmolPopup'; }
  if ( /_Canvas2D/.test(t) ) { t = 'HTML5'; }
  else if ( /_Canvas3D/.test(t) ) { t = 'WebGL'; }
  else if ( /_Applet/.test(t) ) { t = 'Java'; }
  else { t = null; }
	JSmolCloneData = {};
  JSmolCloneData.type = t;
  JSmolCloneData.name = wname;
  JSmolCloneData.platformSpeed = Jmol.evaluate(JSmolObject, 'platformSpeed + 0');
  JSmolCloneData.state = Jmol.getPropertyAsString(JSmolObject, 'stateInfo');
	wW = 500; wH = 500;
	wparm = 'resizable, width=' + wW + ', height=' + wH;
	w = window.open(extensionDir + 'JSmolPopup.htm',wname,wparm);
  w.focus();
}

function closePopupIfOpen(popupName){
  if ( typeof(window[popupName]) != 'undefined' && !window[popupName].closed ) {
    window[popupName].close();
  }
}
