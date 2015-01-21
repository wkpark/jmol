/* 

Jmol.js   (JSmol version)
author: Bob Hanson hansonr@stolaf.edu 5/24/2013 12:06:25 PM

Script replacement for Jmol.js that uses JSmol instead.
Can be used to turn most legacy Jmol.js-based sites to JSmol.
Presumes prior loading of JSmol.min.js

1) rename your current Jmol.js file Jmol_old.js in case you want to undo this
2) replace that file with this one

3) If you use JmolInitialize, put the Jar files from the java/ directory here 
   in that designated directory and make a new subdirectory there called j2s.
   Then put all j2s/* files in that j2s subdirectory.
      
    If you don't use JmolIinitialize, all the jar files should go into a subdirectory named "java" 
    in the same directory as your web page, and all the JavaScript should go into a subdirectory "j2s"
    also in the same directory as your web page, just like it is here.
      
3) copy all j2s/* files into a directory on your site if you want to use HTML5 (defaults to ./j2s)
4) copy all java/* files into a directory on your site if you want to use Java (defaults to ./java)


5)  


*/

Jmol.Info = {
			disableJ2SLoadMonitor: false,
			disableInitialConsole: true
}

var defaultdir = "."
var defaultjar = "JmolApplet.jar"


var undefined; // for IE 5 ... wherein undefined is undefined

////////////////////////////////////////////////////////////////
// Basic Scripting infrastruture
////////////////////////////////////////////////////////////////

var _jmol = {
  appletCount: 0,
  applets: {},
  allowedJmolSize: [25, 2048, 300],   // min, max, default (pixels)
  codebase: "java",
  targetSuffix: 0,
  target: "jmolApplet0",
  buttonCount: 0,
  checkboxCount: 0,
  linkCount: 0,
  cmdCount: 0,
  menuCount: 0,
  radioCount: 0,
  radioGroupCount: 0,
  initialized: false,
  initChecked: false,
  archivePath: null, // JmolApplet0.jar OR JmolAppletSigned0.jar
}


function jmolInitialize(codebaseDirectory, fileNameOrUseSignedApplet) {
  if (_jmol.initialized)
    return;
  _jmol.initialized = true;
  if(_jmol.jmoljar) {
    var f = _jmol.jmoljar;
    if (f.indexOf("/") >= 0) {
      alert ("This web page URL is requesting that the applet used be " + f + ". This is a possible security risk, particularly if the applet is signed, because signed applets can read and write files on your local machine or network.")
      var ok = prompt("Do you want to use applet " + f + "? ","yes or no")
      if (ok == "yes") {
        codebaseDirectory = f.substring(0, f.lastIndexOf("/"));
        fileNameOrUseSignedApplet = f.substring(f.lastIndexOf("/") + 1);
      } else {
  _jmolGetJarFilename(fileNameOrUseSignedApplet);
        alert("The web page URL was ignored. Continuing using " + _jmol.archivePath + ' in directory "' + codebaseDirectory + '"');
      }
    } else {
      fileNameOrUseSignedApplet = f;
    }
  }
  _jmol.codebase = codebaseDirectory;
  _jmolGetJarFilename(fileNameOrUseSignedApplet);
}

function _jmolApplet(size, inlineModel, script, nameSuffix) {
    nameSuffix == undefined && (nameSuffix = _jmol.appletCount);
    var id = "jmolApplet" + nameSuffix;
    jmolSetTarget(nameSuffix);
    ++_jmol.appletCount;
    script || (script = "select *");
    if (_jmol.initialized) {
      Info.jarPath || (Info.jarPath = _jmol.codebase);
      Info.jarFile || (Info.jarFile = _jmol.archivePath);
      Info.j2sPath || (Info.j2sPath = Info.jarPath + "/j2s");    
    } else {
     // we just assume they are in ./java and ./j2s
    }
    var sz = _jmolGetAppletSize(size);
    Info || (Info = Jmol.Info);
    Info.width || (Info.width = sz[0]);
    Info.height || (Info.height = sz[1]);  
    Info.script || (Info.script = script);
    Info.isSigned == undefined && (Info.isSigned = (Info.jarFile.indexOf("Signed") >= 0));
    for (var i in _jmol.params)
      if(_jmol.params[i]!="")
        Info[i] || (Info[i] = _jmol.params[i]);
    return _jmol.applets[id] = Jmol.getApplet(id, Info)
}

function jmolSetParameter(key,value) {
  (Info || Jmol.Info)[key] = value;
}

function jmolSetTranslation(TF) {
  _jmol.params.doTranslate = ''+TF;
}

function _jmolGetJarFilename(fileNameOrFlag) {
  _jmol.archivePath =
    (typeof(fileNameOrFlag) == "string"  ? fileNameOrFlag : (fileNameOrFlag ?  "JmolAppletSigned" : "JmolApplet") + "0.jar");
}

function jmolSetDocument(doc) {
  _jmol.currentDocument = doc;
}

function jmolSetAppletColor(boxbgcolor, boxfgcolor, progresscolor) {
  (Info || Jmol.Info).color = boxbgcolor ? boxbgcolor : "black";
}

function jmolSetAppletWindow(w) {
  _jmol.appletWindow = w;
}

function jmolApplet(size, script, nameSuffix) {
  return _jmolApplet(size, null, script, nameSuffix);
}

////////////////////////////////////////////////////////////////
// Basic controls
////////////////////////////////////////////////////////////////

// undefined means it wasn't there; null means it was explicitly listed as null (so as to skip it)

function jmolButton(script, label, id, title) {
  return Jmol.jmolButton(_jmol.target, script, label, id, title);
}

function jmolCheckbox(scriptWhenChecked, scriptWhenUnchecked,
                      labelHtml, isChecked, id, title) {
  return Jmol.jmolCheckbox(_jmol.target, scriptWhenChecked, scriptWhenUnchecked,
                      labelHtml, isChecked, id, title)
}

function jmolRadioGroup(arrayOfRadioButtons, separatorHtml, groupName, id, title) {
  return Jmol.jmolRadioGroup(_jmol.target, arrayOfRadioButtons, separatorHtml, groupName, id, title)
}


function jmolRadio(script, labelHtml, isChecked, separatorHtml, groupName, id, title) {
  return  Jmol.jmolRadio(_jmol.target, script, labelHtml, isChecked, separatorHtml, groupName, id, title)
}

function jmolLink(script, label, id, title) {
  return Jmol.jmolLink(_jmol.target, script, label, id, title)
}

function jmolCommandInput(label, size, id, title) {
  return Jmol.jmolCommandInput(_jmol.target, label, size, id, title);
}

function jmolMenu(arrayOfMenuItems, size, id, title) {
  return Jmol.jmolMenu(_jmol.target, arrayOfMenuItems, size, id, title);
}

function jmolHtml(html) {
  return Jmol._documentWrite(html);
}

function jmolBr() {
  return Jmol._documentWrite("<br />");
}

////////////////////////////////////////////////////////////////
// advanced scripting functions
////////////////////////////////////////////////////////////////

function jmolDebugAlert(enableAlerts) {
  // n/a
}

function jmolAppletInline(size, inlineModel, script, nameSuffix) {
 alert("jmolAppletInline not implemented")
}

function jmolSetTarget(targetSuffix) {
  if (targetSuffix)_jmol.targetSuffix = targetSuffix;
  return _jmol.target = "jmolApplet" + _jmol.targetSuffix;
}

function jmolFindTarget(targetSuffix) {
  return _jmol.applets[jmolSetTarget(targetSuffix)];
}

function jmolScript(script, targetSuffix) {
  Jmol.script(jmolFindTarget(targetSuffix), script)
}

function jmolLoadInline(model, targetSuffix) {
  alert("jmolLoadInline not implemented")
}


function jmolLoadInlineScript(model, script, targetSuffix) {
  alert("jmolLoadInlineScript not implemented - use DATA command")
}


function jmolLoadInlineArray(ModelArray, script, targetSuffix) {
  alert("jmolLoadInlineArray not implemented - use DATA command")
}

function jmolAppendInlineArray(ModelArray, script, targetSuffix) {
  alert("jmolAppendInlineArray not implemented - use DATA command")
}

function jmolAppendInlineScript(model, script, targetSuffix) {
  alert("jmolAppendInlineScript not implemented - use DATA command")
}

function jmolCheckBrowser(action, urlOrMessage, nowOrLater) {
  // unnecessary
}

////////////////////////////////////////////////////////////////
// Cascading Style Sheet Class support
////////////////////////////////////////////////////////////////

function jmolSetAppletCssClass(appletCssClass) {
  Jmol.setAppletCss(appletCssClass)
}

function jmolSetButtonCssClass(s) {
  Jmol.setButtonCss(s)
}

function jmolSetCheckboxCssClass(s) {
  Jmol.setCheckboxCss(s)
}

function jmolSetRadioCssClass(s) {
  Jmol.setRadioCss(s)
}

function jmolSetLinkCssClass(s) {
  Jmol.setLinkCss(s)
}

function jmolSetMenuCssClass(s) {
  Jmol.setMenuCss(s)
}

function jmolSetMemoryMb(nMb) {
  // n/a
}


function jmolSetCallback(callbackName,funcName) {
  document.title=("jmolSetCallback " + callbackName + "/" + funcName + " must be included in Info definition")
}

function jmolSetSyncId(n) {
  alert("jmolSetSyncId " + n + " must be included in Info definition")
}

function jmolSetLogLevel(n) {
  Jmol.script(_jmol.target, "set loglevel " + n)
}

function _jmolGetAppletSize(size, units) {
  var width, height;
  if ( (typeof size) == "object" && size != null ) {
    width = size[0]; height = size[1];
  } else {
    width = height = size;
  }
  return [_jmolFixDim(width, units), _jmolFixDim(height, units)];
}

function _jmolFixDim(x, units) {
  var sx = "" + x;
  return (sx.length == 0 ? (units ? "" : _jmol.allowedJmolSize[2])
  : sx.indexOf("%") == sx.length-1 ? sx
    : (x = parseFloat(x)) <= 1 && x > 0 ? x * 100 + "%"
    : (isNaN(x = Math.floor(x)) ? _jmol.allowedJmolSize[2]
      : x < _jmol.allowedJmolSize[0] ? _jmol.allowedJmolSize[0]
        : x > _jmol.allowedJmolSize[1] ? _jmol.allowedJmolSize[1]
        : x) + (units ? units : ""));
}

//////////user property/status functions/////////

function jmolGetStatus(strStatus,targetSuffix){
  return Jmol.getStatus(jmolFindTarget(targetSuffix), strStatus)
}

function jmolGetPropertyAsArray(sKey,sValue,targetSuffix) {
  return Jmol.getPropertyAsArray(jmolFindTarget(targetSuffix), sKey, sValue)
}

function jmolGetPropertyAsString(sKey,sValue,targetSuffix) {
  return Jmol.getPropertyAsString(jmolFindTarget(targetSuffix), sKey, sValue)
}

function jmolGetPropertyAsJSON(sKey,sValue,targetSuffix) {
  return Jmol.getPropertyAsJSON(jmolFindTarget(targetSuffix), sKey, sValue)
}

function jmolGetPropertyAsJavaObject(sKey,sValue,targetSuffix) {
  return Jmol.getPropertyAsJavaObject(jmolFindTarget(targetSuffix), sKey, sValue)
}

///////// synchronous scripting ////////

function jmolScriptWait(script, targetSuffix) {
  return Jmol.scriptWait(jmolFindTarget(targetSuffix), script)
}

function jmolScriptWaitOutput(script, targetSuffix) {
  return Jmol.scriptWaitOutput(jmolFindTarget(targetSuffix), script)
}

function jmolEvaluate(molecularMath, targetSuffix) {
  return Jmol.evaluate(jmolFindTarget(targetSuffix), molecularMath)
}

function jmolScriptEcho(script, targetSuffix) {
  return Jmol.scriptEcho(jmolFindTarget(targetSuffix), script)
}


function jmolScriptMessage(script, targetSuffix) {
  return Jmol.scriptMessage(jmolFindTarget(targetSuffix), script)
}


function jmolScriptWaitAsArray(script, targetSuffix) {
  return Jmol.scriptWait(jmolFindTarget(targetSuffix), script)
}



////////////   save/restore orientation   /////////////

function jmolSaveOrientation(id, targetSuffix) {
  return Jmol.saveOrientation(jmolFindTarget(targetSuffix), id)
}

function jmolRestoreOrientation(id, targetSuffix) {
  return Jmol.restoreOrientation(jmolFindTarget(targetSuffix), id)
}

function jmolRestoreOrientationDelayed(id, delay, targetSuffix) {
  return Jmol.restoreOrientationDelayed(jmolFindTarget(targetSuffix), id, delay)
}

function jmolResizeApplet(size, targetSuffix) {
  return Jmol.resizeApplet(jmolFindTarget(targetSuffix), size);
}


////////////  add parameter /////////////

function jmolAppletAddParam(appletCode,name,value){
  alert ("use Info to add a parameter: " + name + "/" + value)
}
