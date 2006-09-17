//examples.js by Bob Hanson hansonr@stolaf.edu 6:17 AM 6/14/2004
isxhtmltest=0/1
isinitialized=0
MAXMSG=100000
msglog=""

JMOLDOCS="http://www.stolaf.edu/people/hansonr/jmol/docs"
force_useHtml4Object=0
force_useIEObject=0

TitleInfo=new Array()

function fixlinks(){
 if(!document.getElementsByTagName) return;
 var anchors = document.getElementsByTagName("a")
 for(var i=0; i<anchors.length; i++){
	var anchor = anchors[i]
	var h=anchor.getAttribute("href")
	if(h && anchor.getAttribute("rel"))anchor.target = anchor.getAttribute("rel")
	if(h && h.indexOf("personal_disclaimer")>=0)anchor.innerHTML=""
 }
}

function idof(s){
	s=s.replace(/\(\#\)/,"")
	return unescape(s).replace(/(\.|\(|\))/g,"").replace(/\s+/g,"")
}


function fixabbrevs(s){
 s=s.replace(/JMOLDOCS/g,JMOLDOCS)
 return s
}

usejmoljs=1

//nstart=(location.search+"?").split("?")[1]
td2width=250
ncolsfortextarea=48
ntd=2
thecaption=""
showappcode=false&&true
docbase="./index.htm"
codebase="."
archive="JmolApplet.jar"
messagecallback="showmsg"
thiscommand=""
animcallback="showmsg"
pickcallback="showmsg"
loadstructcallback="showmsg"
echoformat="font echo 14"
echoformat2="font echo 16"
title="example form"
Scripts=new Array("reset")
height=400
width=400
ref=""
remark=""
FTREF250="<a class=\"ftnote\" href=\"javascript:showref(250)\"><sup>*</sup></a>"
woptions="menubar=yes,scrollbars,alwaysRaised,width=700,height=600,left=50"
loadscript=";"

function showref(n){
 if(n==250)alert("Integer distances in Jmol indicate Rasmol units (0.004 Angstrom), now deprecated.")
}


function dowritenew(s){
 var sm=""+Math.random()
 sm=sm.substring(2,10)
 var newwin=open("","jmol_"+sm,woptions)
 newwin.document.write(s)
 newwin.document.close()
}

function getapplet(name, model, codebase, height, width, script, msgcallback,animcallback,pickcallback,loadstructcallback) {

  if (!isinitialized)jmolInitialize(".")
  if(force_useHtml4Object)_jmol.useHtml4Object=1
  if(force_useIEObject)_jmol.useIEObject=1
  isinitialized = 1
  jmolSetDocument(0)
  if (model)script = "load " + model + ";" + script
  var s = jmolApplet([width,height], script)
  var sext = ""
  if (msgcallback) {
    sext = "\n<param name='MessageCallback' value='" + msgcallback + "' />"
    s = s.replace(/\<param/, sext + "\n<param")
  }
  if (animcallback) {
    sext = "\n<param name='AnimFrameCallback' value='" + animcallback + "' />"
    s = s.replace(/\<param/, sext + "\n<param")
  }
  if (pickcallback) {
    sext = "\n<param name='PickCallback' value='" + pickcallback + "' />"
    s = s.replace(/\<param/, sext + "\n<param")
  }
  if (loadstructcallback) {
    sext = "\n<param name='LoadStructCallback' value='" + loadstructcallback + "' />"
    s = s.replace(/\<param/, sext + "\n<param")
  }
  return s
}

function getinfo(){
 var script=getscriptlink(0,false)
 var s="Click on a link below to see what it does. You can also type a command in the box below the model to see its effect. <a target=_blank href=JMOLDOCS>[documentation]</a> "
 s=fixabbrevs(s)
 if(model)s+=" The model used in this case is "+(model.length==8 && model.indexOf(".pdb")==4?"<a rel=\"_blank\" href=\"http://pdbbeta.rcsb.org/pdb/explore.do?structureId="+model.substring(0,4)+"\">["+model.substring(0,4)+"]</a>":model)+"."
 return "<p>"+s+"</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p>"
}

function getremark(){
 return (remark?"<blockquote><p>"+remark+"</p></blockquote>":"")
}

function getscriptlink(i,isul){
 if(!Scripts[i])return ""
 var S=fixabbrevs(Scripts[i]).split(" ~~ ")
 var s=(S.length>1?(isul?"</ul>":"")+"<table><tbody><tr>":"")
 //2> means colspan=2
 for(var j=0;j<S.length;j++){
	if(S.length>1)s+="\n<td"+(td2width && S.length<=ntd?" width='"+td2width+"'":"")+" valign='top'"+(isNaN(parseInt(S[j]))?">":" colspan='"+parseInt(S[j])+"'")+(isul?"<ul>":"")
	s+=(isul?"\n<li>":"")
	+(S[j].indexOf("<span")>=0||S[j].indexOf("<a href")>=0?S[j]:"<a href=\"javascript:showscript("+i+","+j+")\">"
		+(S[j].indexOf("load")==0?"<font color=red>":"")
		+(S[j].indexOf("###")==0?"<font color=black class=header>":S[j].indexOf("#")==0?"<font color=black>":"")
		+S[j].replace(/\</g,isxhtmltest?"&amp;lt;":"&lt;")
		+(S[j].indexOf("#")==0||S[j].indexOf("load")>=0?"</font>":"")
		+"</a>")
	+(isul?"</li>":"")
	if(S.length>1)s+=(isul?"\n</ul>":"")+"\n</td>"
 }
 if(S.length>1)s+="\n</tr></tbody></table>"+(isul?"\n<ul>":"")
 return s
}

function getscripts(){
 var s=""
 var isul=true
 for (var i=1;i<Scripts.length;i++){
	if(Scripts[i].charAt(0)==" "){
		s+="</ul><p>"+Scripts[i]+"</p><ul>"
	}else if(Scripts[i].charAt(0)=="*"){
		if(Scripts[i]=="*NOUL")isul=false
		if(Scripts[i]=="*UL")isul=true
	}else{
		s+=getscriptlink(i,isul)
	}
 }
 s=s.replace(/\<\/tbody\>\<\/table\>\<\/ul\>\<ul\>\<table\>\<tbody\>/g,"")
 if(s)s+="</ul>"
 return s.substring(5,s.length)
}

function gettitleinfo(){
 var s=""
 var l=window.location+""
 for(var t in TitleInfo){
	s+="\n<option "+(l.indexOf(t+".htm")>=0?"selected=\"selected\"":"")+" value=\""+t+"\">"+TitleInfo[t]+"</option>"
 }
 s="<select onchange=\"location=this.value+'.htm'\">"+s+"</select>"
 return s
}

function showfunction(i){
 if(!i)return
 document.getElementById("msg").value=window[i].toString()
}

function getfunctions(){
 return ""

 if((navigator.appName+navigator.appVersion).indexOf("afari")>=0)return ""
 var S=new Array()
 for(var i in window){
	if(typeof(window[i])=="function" && window[i].toString().indexOf("native")<0)S[S.length]="<option value=\""+i+"\">"+i+"()</option>"
 }
 if(!S)return ""
 return "functions on this page:<select name=\"myfunctions\" onchange=\"showfunction(this.value)\"><option value=\"0\"></option>"+S.sort().join("")+"</select>"
}

function gettitle(){
 return ""
 return s
}

function showcmd(){
 if(document.getElementById("msg").value.length>MAXMSG)document.getElementById("msg").value=document.getElementById("msg").value.substring(0,MAXMSG/2)
 showscript(-1)
}

function showmsg(n,objwhat,moreinfo){

 var what=objwhat+(moreinfo?" :: "+moreinfo:"")
 msglog+="\n"+what
 var s=document.getElementById("msg").value
 if(s.length>MAXMSG) s=s.substring(0,MAXMSG/2)
 document.getElementById("msg").value=n+": "+what+"\n"+s
 if(what.indexOf("executing script")>=0)return
}

function showfile(s){
 if(s.length<100)return
 thiscommand=""
 var s="<pre>"+s.replace(/\</g,"&lt;")+"</pre>"
 dowritenew(s)
}

function showmsgbox(){
 var s=document.getElementById("msg").value+""
 var S=s.split("\n").reverse()
 s=S.join("\n")
 s="<pre>"+s.replace(/\</g,"&lt;")+"</pre>"
 dowritenew(s)
}

function showthefile(){
 thiscommand="show file"
 jmolScript(thiscommand)
}

function showpage(){
 var s=getpage()
 document.write(s)
}

function winHeight(){
  var myWidth = 0, myHeight = 500;
  if( typeof( window.innerWidth ) == 'number' ) {
    //Non-IE
    myWidth = window.innerWidth;
    myHeight = window.innerHeight;
  } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
    //IE 6+ in 'standards compliant mode'
    myWidth = document.documentElement.clientWidth;
    myHeight = document.documentElement.clientHeight;
  } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
    //IE 4 compatible
    myWidth = document.body.clientWidth;
    myHeight = document.body.clientHeight;
  }
  return myHeight
}

function getpage(){

 var s=gettitle()+getremark()+getinfo()

 s+='\n<div id="aframe" style = "position:absolute;top:50px;left:10px;width:530px;overflow:auto;height:'+(winHeight()-75)+'px">'
+'<table id="atable"><tbody><tr><td>'
+getscripts()
+'\n</td></tr></tbody></table></div>'
+'\n<div id="bframe" style = "position:absolute;top:50px;left:550px;height:500px;width:450px;overflow:auto;height:'+(winHeight()-75)+'px">'
+'\n<span id="jmolApplet">'
+getapplet("jmol",model,codebase,height,width,loadscript+";"+Scripts[0],messagecallback,animcallback,pickcallback,loadstructcallback)
+'\n</span>'
+'\n<br />'+thecaption
+'\n<form action="javascript:showcmd()"><p>'
+'\ncmd: <input id="cmd" type="text" size="50" value="" />'
+'\n<a href="javascript:showmsgbox()">popup</a>'
 +'\n<br /><textarea id="msg" cols="'+ncolsfortextarea+'" rows="6" wrap="off">'
+'\n</textarea>'
+'\n<br />'+getfunctions()
+'\n</p></form>'
+'\n</div>'
 if(isxhtmltest)s=s.replace(/\</g,"<br />&lt;")
 return s
}



function usercallback(s){}

function showscript(i,j,script){
	if(!j)j=0
	var s=(script?script:i>=0?Scripts[i].split(" ~~ ")[j]:document.getElementById("cmd").value)
	showmsg("user",s+"\n")
	thiscommand=s
	usercallback(s)
	var S=(s+"#").split("#")
	document.getElementById("cmd").value=s=S[0]
	jmolScript(s)
}


