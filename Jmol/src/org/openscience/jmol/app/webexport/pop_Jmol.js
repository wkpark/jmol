/*	Allows to open Jmol models on request, in a div
	
	Available from  http://biomodel.uah.es/Jmol/  and  http://wiki.jmol.org/ 
	Author: Angel Herr�ez.  Version 2007.04.23
	
	This template is offered freely for anyone to use or adapt it, 
	according to Creative Commons Attribution-ShareAlike 3.0 License,
	http://creativecommons.org/licenses/by-sa/3.0/

	Modified 2007.07.17  by Jonathan Gutow
	
	Main change is that the JmolSize is specified in the call.
	Image file is forced to fit within the div boundaries by scaling it.
	Image file name is passed explicitely.
	Removed the passing of the molecule name as labels should be set in script.
	
	Modified 2007.08.13 by Bob Hanson
	
	-- integration into Jmol application

*/

function putJmolDiv(molNr, molFileName,imageFileName,appletWidth,appletHeight) {
 //molFileName can be the name of a molecule or script file.
	var tx = '<div id="Jmol' + molNr 
	  + '" class="JmolDiv"'
	  + ' style="width:' + appletWidth + 'px; height:' + appletHeight + 'px;'
	  + ' background-image:URL('+imageFileName+')"'
	  + '>';
	tx += '<br><table cellpadding="10"><tr><td style="background-color:white">';
	tx += 'To get a 3-D model you can manipulate, click ';
	tx += '<a href="javascript:void(popInJmol(' + molNr + ', \'' + molFileName + '\','+ appletWidth + ','+ appletHeight + '))">here</a>.';
	tx += 'Download time may be significant the first time the applet is loaded.</td></tr></table></div>';
	document.writeln(tx);
}

function popInJmol(n,fileName,width,height) {
	document.getElementById("Jmol"+n).innerHTML = jmolApplet([width,height],"script "+fileName+"",n);
}

function addJmolDiv(i,floatdiv,name,width,height,caption,note) {

        var s = "\n<br><div \"style='height:"+(height+100)+"px'\">\n<div class = \""+floatdiv+"\">";

	s += "\n<table style=\"text-align: left; width: "+width+"px;\" border='1' cellpadding='2'";
	s += "\n cellspacing='2'>";
	s += "\n    <tr>";
	s += "\n      <td style=\"vertical-align: top; width: "+width+"px; height: "+height+"px;\">";
	document.write(s);//.replace(/\</g,"&lt;"));

	putJmolDiv(i, name+".scpt",name+".png",width, height);

	s = "\n      </td>";
	s += "\n    </tr>";
	s += "\n    <tr>";
	s += "\n      <td style=\"vertical-align: top;\">"+caption+"<br>";
	s += "\n      </td>";
	s += "\n    </tr>";
	s += "\n</table>";
	s += "\n</div>";
	s += "\n<div>"+note+"</div></div>";
	document.write(s);//.replace(/\</g,"&lt;"));

}

function addAppletButton(i, name, label, info) {
  var s = '\n<table style="text-align: center; width: 100%" border="1" cellpadding="2" cellspacing="2">'
  s += '<tr><td>'
  document.write(s)
  jmolButton('Script '+name+'.scpt', label); 
  var s = '</td></tr></table>\n' + info + "\n</br>";
  document.write(s)
}