/* Jmol Simple JavaScript Color Picker
 by Jonathan Gutow
V1.1
June 14, 2010

requires
   Jmol.js

Usage
Where ever you want a popup color picker box include a script like

<script type="text/javascript">
var scriptStr2 = 'select carbon; color atom $COLOR$;';
JmolColorPickerBox("colorBox1", "rgb(100,100,100)", scriptStr2, "0");
</script>

The only function that will not change name or syntax is JmolColorPickerBox(scriptStr, rgb, boxIdStr,  appletId).

USE OTHER FUNCTIONS IN THE JAVASCRIPT LIBRARY AT YOUR OWN RISK.
All parameters are strings although appletId could potentially be a number, but it is used to make a string.
  scriptStr should contain $COLOR$ where you wish the color string to be passed to Jmol in the script you provide.
  rgb is the browser standard 0-255 red-green-blue values specified as an array [red, green, blue] default = [127,127,127] a dark grey.
  boxIdStr should be a string that is unique to the web document, if not provided it will be set to colorBoxJ, J=0, 1, 2... in the order created.
  appletId is the standard Jmol id of applet you want the colorpicker to send the script to.  Default = "0".


*/

//globals and their defaults

var JmolColorPickerStatus = {
    lastPicked: '', //last picked color...not used at present
    funcName: '', //where to pass to next after pickedColor()
    passThrough: '' //name of the global variable or structure containing information to be passed
    }

var JmolColorPickerBoxes=new Array();//array of boxInfo

function boxInfo(boxID, appletID, scriptStr){//used when using a predefined colorPickerBox
    this.boxID=boxID;
    this.appletID=appletID; //applet ID
    this.scriptStr=scriptStr; //script with $COLOR$ where the color should be placed.
    }

function changeClass(someObj,someClassName) {
    someObj.setAttribute("class",someClassName);
    someObj.setAttribute("className",someClassName);  // this is for IE
}

//Jmol set up functions to allow local testing easily
function addJavaScript(path, file) {
 document.write("<"+"script src=\"" + path + "/" + file + "\" type=\"text/javascript\"><" + "/script>"); 
}


//Build the ColorPicker Div.

// detect if browser supports data:URI   (IE6 & IE7 do not)
    var dataURIsupported = true;
    var testImg64 = new Image();
    testImg64.onload = testImg64.onerror = function() {
        if(this.width != 1 || this.height != 1) { dataURIsupported = false; }
    }
    testImg64.src = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";

function makeColorPicker(){
    JmolColorPickerDiv = document.getElementById("JmolColorPickerDiv");
    if(! JmolColorPickerDiv){
        var colorPickerCSS = document.createElement('style');
        colorPickerCSS.type = 'text/css';
        CSSStr ='.JmolColorPicker_vis {border-style:solid;border-width:thin;clear:both;display:block;overflow:visible;position:relative;left:-52px;width:104px;z-index:2;}'
        CSSStr +='.JmolColorPicker_hid {height:0;min-height:0;display:none;overflow:hidden;z-index:0;}';
        if (colorPickerCSS.styleSheet) { // IE
            colorPickerCSS.styleSheet.cssText = CSSStr;
        } else { // W3C
            content = document.createTextNode(CSSStr); 
            colorPickerCSS.appendChild(content);
        }
        document.getElementsByTagName('head')[0].appendChild(colorPickerCSS);
        JmolColorPickerDiv = document.createElement("div");
        JmolColorPickerDiv.setAttribute("id", "JmolColorPickerDiv");
        changeClass(JmolColorPickerDiv,"JmolColorPicker_hid");
        }
   var rgbs=[[255,0,0]
       ,[255,128,0]
       ,[255,255,0]
       ,[128,255,0]
       ,[0,255,0]
       ,[0,255,128]
       ,[0,255,255]
       ,[0,128,255]
       ,[0,0,255]
       ,[128,0,255]
       ,[255,0,255]
       ,[255,0,128]
       ,[255,255,255]
   ];
   var hues=[[190,100],
             [175,95],
             [150,90],
             [135,80],
             [100,68],
             [85,55],
             [70,40],
             [60,30],
             [50,20],
             [35,0]
     ];
    var tempwidth = 8*(rgbs.length);
    var htmlStr = '<div id="JmolColorPickerHover" style="font-size:2px;width:'+tempwidth+'px;text-align:right;background-color:white;cursor:default;">';
    if (dataURIsupported) {
        htmlStr += '<image id="JmolColorPickerCancel" onclick="pickedColor(\'cancel\');" src="data:image/bmp;base64,Qk3CAQAAAAAAADYAAAAoAAAACwAAAAsAAAABABgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAdnZ2j4+PoKCgqampqampoKCgj4+PAAAAAAAAAAAAAAAAAAAAAAAAsbGxwsLCysrKysrKwsLCAAAAAAAAAAAAAAAAZWVlAAAAAAAAAAAA29vb5OTk5OTkAAAAAAAAAAAAj4+PAAAAdnZ2oKCgAAAAAAAAAAAA9PT0AAAAAAAAAAAAwsLCoKCgAAAAfn5+qampysrKAAAAAAAAAAAAAAAAAAAA5OTkysrKqampAAAAfn5+qampysrK5OTkAAAAAAAAAAAA9PT05OTkysrKqampAAAAdnZ2oKCgwsLCAAAAAAAAAAAAAAAAAAAA29vbwsLCoKCgAAAAZWVlj4+PAAAAAAAAAAAA5OTkAAAAAAAAAAAAsbGxj4+PAAAATExMAAAAAAAAAAAAwsLCysrKysrKAAAAAAAAAAAAdnZ2AAAAAAAAAAAAAAAAj4+PoKCgqampqampoKCgAAAAAAAAAAAAAAAAAAAAAAAATExMZWVldnZ2fn5+fn5+dnZ2ZWVlAAAAAAAAAAAA">';
    } else {
        htmlStr += '<span id="JmolColorPickerCancel" onclick="pickedColor(\'cancel\');" style="font-size:10px; padding:0 2px; background-color:#A0A0A0; font-family:Verdana, Arial, Helvetica, sans-serif;">X</span>';
    }
    htmlStr += '</div>';	 
    htmlStr += '<table cellspacing="0" cellpadding="0" border="0" style="font-size:2px; cursor:default;"><tbody>';
    for (j = 0; j < hues.length;j++){
    htmlStr += '<tr>'
    var f = (hues[j][0])/100.0;
       for (k = 0; k < rgbs.length; k++){
       if(rgbs[k][0]==255&&rgbs[k][1]==255&&rgbs[k][2]==255) f =(hues[j][1])/100.0;; 
       r = Math.min(Math.max(Math.round(rgbs[k][0] * f),Math.round(255-rgbs[k][0])*(f-1)^2),255);
       g = Math.min(Math.max(Math.round(rgbs[k][1] * f),Math.round(255-rgbs[k][1])*(f-1)^2),255);
       b = Math.min(Math.max(Math.round(rgbs[k][2] * f),Math.round(255-rgbs[k][2])*(f-1)^2),255);
          htmlStr +='<td style="background-color: rgb(' + r + "," + g + ","+ b + ');">';
          htmlStr +='<div style="width: 8px; height: 8px;" onclick=\'pickedColor("rgb('+r+','+g+','+b+')");\' ';
          htmlStr +='onmouseover=\'hoverColor("rgb('+r+','+g+','+b+')");\'></div>';
          htmlStr +='</td>';
       }//for k
   htmlStr +='</tr>';
   }//for j
   htmlStr += '</tbody></table>'; 
    content = document.createTextNode("loading color picker...");
    JmolColorPickerDiv.appendChild(content);
    JmolColorPickerDiv.innerHTML = htmlStr;
    return(JmolColorPickerDiv);   
}

function pickedColor(colorStr){
    changeClass(document.getElementById('JmolColorPickerDiv'), "JmolColorPicker_hid");
    if(colorStr!='cancel'){
        var evalStr = ''+ JmolColorPickerStatus.funcName+'("'+colorStr+'",'+ JmolColorPickerStatus.passThrough+');';
        eval(evalStr);
    }
}

function hoverColor(colorStr){
    document.getElementById("JmolColorPickerHover").style.background = colorStr;
}

function popUpPicker(whereID, funcName, passThrough){
    var pickerDiv = document.getElementById("JmolColorPickerDiv");
    if (!pickerDiv) {//make a new picker
        JmolColorPickerDiv =  makeColorPicker();
        document.body.appendChild(JmolColorPickerDiv);
        pickerDiv = document.getElementById("JmolColorPickerDiv");
        }
    JmolColorPickerStatus.funcName = funcName;
    JmolColorPickerStatus.passThrough = passThrough;
    var where = document.getElementById(whereID);
    where.appendChild(pickerDiv);
    changeClass(pickerDiv,"JmolColorPicker_vis");
}


function JmolColorPickerBox(scriptStr, startColor, boxID, appletID){
    if (!appletID) appletID = "0";
    var boxNum = JmolColorPickerBoxes.length;
    if (!boxID) boxID = 'colorBox'+boxNum;
    if (!startColor) startColor = [127,127,127];
    var presentColor = 'rgb('+startColor[0]+','+startColor[1]+','+startColor[2]+')';
    JmolColorPickerBoxes[boxNum]= new boxInfo(boxID, appletID, scriptStr);  
    var boxDiv = document.createElement("div");
    boxDiv.setAttribute("id",boxID);
    content = document.createTextNode("building color box...");
    boxDiv.appendChild(content);
    boxDiv.style.background=presentColor;
    boxDiv.style.height='14px';
    boxDiv.style.width='28px';
    htmlStr = '<table style="font-size:0px; cursor:default;" cellspacing="0" cellpadding="0" border="1" onclick=\'popUpPicker(';
    htmlStr += '"'+boxID+'","colorBoxUpdate",'+boxNum+');\' ';
    htmlStr += '><tbody>';
    htmlStr += '<tr><td><div style="height: 12px; width: 12px;"></div></td><td>';
    var boxArrowName = 'colorBoxArrow'+boxNum;
    if (dataURIsupported) {
        htmlStr += '<image id="'+ boxArrowName+'" src="data:image/bmp;base64,Qk3mAQAAAAAAADYAAAAoAAAACwAAAAwAAAABABgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIAAAAAAAAAAAAAAAAAAAAAAAAAAAAyMjIyMjIAAAAyMjIyMjIyMjIAAAAAAAAAAAAAAAAAAAAyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIAAAAAAAAAAAAyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAAyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIAAAA">';
    } else {
        htmlStr += '<span id="'+ boxArrowName+'" style="font-size:10px; padding:0 2px; background-color:#A0A0A0; font-family:Verdana, Arial, Helvetica, sans-serif;">V</span>';
    }
    htmlStr += '</td></tr></tbody></table>';
    boxDiv.innerHTML = htmlStr;
    scripts = document.getElementsByTagName("script");
    scriptNode = scripts.item(scripts.length-1);
    parentNode = scriptNode.parentNode;
    parentNode.appendChild(boxDiv);
}


function colorBoxUpdate(pickedColor, boxNum){
    document.getElementById(JmolColorPickerBoxes[boxNum].boxID).style.background = pickedColor;
    changeClass(document.getElementById('JmolColorPickerDiv'), "JmolColorPicker_hid");
    var rgbCodes = pickedColor.replace(/rgb/i,'').replace('(','[').replace(')',']');
    var scriptStr = JmolColorPickerBoxes[boxNum].scriptStr.replace('$COLOR$', rgbCodes);
    jmolScript(scriptStr,JmolColorPickerBoxes[boxNum].appletID);
}

