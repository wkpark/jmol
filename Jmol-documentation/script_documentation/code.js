//Jmol Documentation JavaScript 11.0
//BH 8:26 PM 9/28/2005 HTML 4.0 STRICT compatibility; fix for popup under Opera--NO! Breaks Mac!
//BH 2:45 PM 12/2/2005 added ?docbook 3:19 PM 12/09/2005
//BH 8:14 AM 1/30/2006 minor improvements
//BH 6:11 PM 2/21/2006 edited for script() command possibly returning a string
//BH 2:47 PM 4/11/2006 added TEXT option

startmessage ="See an error? Something missing? Please <a href=\"mailto:hansonr@stolaf.edu?subject=Jmol applet documentation\">let us know</a>. For a wide variety of interactive examples, see <a href=examples-11/new.htm>new.htm</a>."
defaultversion = "11.0" //could be 10.2
versionlist = ",10.2,11.0,"
exampledir = "examples/" //will be ignored if the example has a / in the name
datadir = "examples/"
jmoljs = "jmol-11.js"
popupscript = "popupscript.js"
//popup-example display did not work with the multi-file archive path


isxhtmltest=0

/* startup URL options:

database example:

 ?example=.slab

example, with added commands after ~

 ?example=.slab~stereo redcyan

example, with a different model

 ?example=.slab&model=1blu.pdb

example, with added commands and different model

 ?example=.slab~stereo redcyan&model=1blu.pdb

just a model

 ?model=1blu.pdb

just a model with added commands

 ?example=~stereo redcyan&model=1blu.pdb

*/


//	jmolInitialize(".")
	jmolSetDocument(false)
//	_jmol.archivePath="JmolApplet.jar"

Cmds=new Array()
Cmdlist=new Array()
CmdExamples=new Array()
Defs=new Array()
Toks=new Array()
Xrefs=new Array()
Examples=new Array()
IndexKeyList=new Array()
nindexkey=0

theindex=""
thiscommand=""
thistoken=""
ncolumns=5
ntest=10
thesearch=""
docsearch=unescape(document.location.search)
specifiedversion = "10.2" //could be 10.x
if(document.location.href.indexOf("#")<0){
 thesearch=(docsearch+"search=").split("search=")[1].split("&")[0]
 thesearch=(thesearch?unescape(thesearch):"")
}
docbase=(document.location.href.split("?")[0]).split("#")[0]
dowritexml=(docsearch.indexOf("xml")>=0)
dowritedocbook=(docsearch.indexOf("docbook")>=0)
doshowunimplemented=(docsearch.indexOf("unimplemented")>=0)
xrefbase=(dowritexml||dowritedocbook?"":docbase)
dousejmoljs=(docsearch.indexOf("nojmoljs")<0)

useobject=(docsearch.indexOf("useobject")>=0)

theexample=(docsearch+"example=").split("example=")[1].split("&")[0]
themodel=(docsearch+"model=").split("model=")[1].split("&")[0]

specifiedversion=(docsearch+"ver="+defaultversion).split("ver=")[1].split("&")[0]

icandoxrefincommand=0

function fixDocbook(s){
	var S=new Array()
	//remove all a tags
	if(s.indexOf("<a")>=0){
		S=s.replace(/\<\/a\>/g,"<a>").split("<a")
		s=S[0]
		//whatever <a ....>text<a>  
		for(var i=1;i<S.length;i++)s+=S[i].substring(S[i].indexOf(">")+1,S[i].length)
	}
	//remove all img tags
	var i=s.indexOf("<img")
	var j=0
	while(i>=0){
		j=s.indexOf(i,">")
		if(j<0){
			j=s.length
			alert("missing end of <img tag in "+s)
		}
		s=s.substring(0,i)+s.substring(j+1,s.length)
		i=s.indexOf("<img")
	}
	//correct lists
	if(s.indexOf("l>")>=0){
		s=s.replace(/\<ul\>/g,"\n<itemizedlist mark=\"bullet\">")
		s=s.replace(/\<\/ul\>/g,"\n</itemizedlist>\n")
		s=s.replace(/\<ol\>/g,"\n<orderedlist enumeration=\"arabic\">")
		s=s.replace(/\<\/ol\>/g,"</orderedlist>\n")
		s=s.replace(/\<li\>/g,"\n<listitem><para>")
		s=s.replace(/\<\/li\>/g,"</para></listitem>")

	}
	if(s.indexOf("table")>=0){
		s=s.replace(/\<table \>/g,"\n<informaltable>")
		s=s.replace(/\<\/table \>/g,"\n</informaltable>\n")

/*
		s=s.replace(/\<tr \>/g,"\n<row>")
		s=s.replace(/\<\/tr \>/g,"</row>")
		s=s.replace(/\<td \>/g,"<entry>")
		s=s.replace(/\<\/td \>/g,"</entry>")
*/

	}
	s=s.replace(/\<pre\>/g,"<programlisting>").replace(/\<\/pre\>/g,"</programlisting>")
	s=s.replace(/\<p\>/g,"<para>").replace(/\<\/p\>/g,"</para>")
	return s
}



function thesep(ikey){
	return (ikey?"<tr><td colspan=\"12\" class=\"sep1\"><br /><br /><a href=\"?ver="+specifiedversion+"#top\"><img class=\"nf\" src=\"img/u.gif\" border=\"0\" />top</a> <a href=\"javascript:setsearch()\"><img class=\"nf\" src=\"img/q.gif\" border=\"0\" />search</a> <a href=\"?ver="+specifiedversion+"#k"+ikey+"\"><img class=\"nf\" src=\"img/i.gif\" border=\"0\" />index</a></td></tr>":"")
	+"<tr><td class=\"sep\" colspan=\"12\">&nbsp;</td></tr>"
}



HeadList=new Array()

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

function doinit(){
 fixlinks()
 if(theexample||themodel)showModel(theexample,themodel)
}

function setsearch(what){
 if(!what)what=prompt("Search for? (just a simple word or phrase here--no logic)",thesearch)
 if(!what)return
 var s=docbase+"?search="+escape(what)+"&ver="+specifiedversion+"&"
 document.location.href=s
}

function checkfortable(sinfo,isital){
 //pretty low-budget:    [||x|y|z||a|b|`width=nn>c||etc|etc|etc||]
 if(sinfo.indexOf("[||")<0)return sinfo
 sinfo=sinfo.replace(/\[\|\|/g,(isital?"</i>":"")+"<br /></p><table cellpadding=\"2\" cellspacing=\"2\" border=\"1\" ><tr ><td valign=top >").replace(/\|\|\]/g,"</td ></tr ></table ><p>"+(isital?"<i>":""))
 sinfo=sinfo.replace(/\|\|/g,"</td ></tr ><tr ><td valign=top >")
 sinfo=sinfo.replace(/\|/g,"</td ><td >")
 sinfo=sinfo.replace(/\>\`/g," ")
 return sinfo
}

function newCmd(command,examples,xref,description,nparams,param1,param2,param3,param4){
 var n=0
 // *v+10.2 means added in 10.2 -- highlight if specifiedversion
 // *v-10.2 means removed in 10.2

 if (xref.substring(0,2)=="*v" && xref.indexOf("*v-"+specifiedversion)>=0)return
 var notimplemented=(xref=="x")
 description=description.replace(/\<ul\>/,"</i></p><ul>").replace(/\<\/ul\>/,"</ul><p><i>")
	.replace(/\<ol\>/,"</i></p><ol>").replace(/\<\/ol\>/,"</ol><p><i>")
	.replace(/\<pre\>/,"</i></p><pre>").replace(/\<\/pre\>/,"</pre><p><i>")
 description=description.replace(/\{\{/g,"@0@").replace(/\}\}/g,"@1@")
 var S=description.split("CHIME NOTE:")
 var descr=checkfortable(marksearch(S[0],1),1)
 var chimenote=(S[1]?checkfortable(marksearch(S[1],1),0):0)
 if(!doshowunimplemented && (notimplemented||command.indexOf("unimplemented")>=0))return
 if(command){
	Cmdlist[Cmdlist.length]=command
	Cmds[command]=new Array()
	Cmds[command].isimplemented=!notimplemented
	Cmds[command].description=descr
	Cmds[command].chimenote=chimenote
	Cmds[command].enabled=foundsearch(description)
	Cmds[command].version=""
	Cmds[command].isnew=false
	Cmds[command].xrefs=""
	Cmds[command].examples=examples.replace(/\~/,"")
	Cmds[command].list=new Array()
	description=""
	descr=""
 }else{
	command=thiscommand
 }
 if(nparams == "TEXT"){
	Cmds[command].description+="<br /><br /><b>"+param1+"</b> "+descr
	return	
 }
 thiscommand=command
 if(examples)addCmdExample(command,examples.replace(/\~/,""))

 var S=xref.split(";")
 for (var i=0;i<S.length;i++){
	xref=S[i]
	if(xref.indexOf("*v+")==0){
		Cmds[command].isnew=(xref.indexOf("*v+"+specifiedversion)==0)
		Cmds[command].version=xref.substring(3,xref.length)
	}else if(xref && xref.length > 0 && xref!="x" && xref.indexOf("*v-")<0){
		if(!Xrefs[xref])Xrefs[xref]=""
		Xrefs[xref]+=","+command
		if(Cmds[command].xrefs.indexOf(xref)<0)Cmds[command].xrefs+=","+xref
	}
 }

 n=Cmds[command].list.length
 Cmds[command].name=command
 Cmds[command].list[n]=new Array()
 var C=Cmds[command].list[n]
 if(examples.charAt(0)!="~")C.examples=examples
 C.xref=xref
 C.isimplemented=!notimplemented
 C.description=descr
 C.chimenote=chimenote
 C.enabled=foundsearch(description)
 C.Nparams=nparams.split("|")
 C.Param=new Array()
 C.Param[1]=param1
 C.Param[2]=param2
 C.Param[3]=param3
 if(C.Nparams[0]!="0" && !C.Param[1] && n){
	C.Param[1]=Cmds[command].list[n-1].Param[1]
 }
 if(param4){
	S=param4.split(";+")
	for(var i=0;i<S.length;i++)C.Param[i+4]=S[i]
 }
 C.lastparam=0
 for(var i=1;i<C.Param.length;i++){
	if(C.Param[i])C.lastparam=i
 } 
}

function newDef(jsToken,typelist,label,description){
 Defs[jsToken]=new Array()
 Defs[jsToken].typelist=typelist.split(",").join(", ").replace(/  /g," ").replace(/\</g,"&lt;")
 Defs[jsToken].label=(label.indexOf("-")>=0||label.indexOf(" ")>=0&&label.indexOf('"')<0?"["+label+"]":label).replace(/\"/g,"")
 Defs[jsToken].description=marksearch(description,1)
 Defs[jsToken].enabled=foundsearch(description)
}

function newExam(name,script,model,html){
 var E = Examples[name]=new Array()
 E.script=script
 if(model=="_blank")
   E.target = model
 else
   E.model=(model=="1"?"1crn.pdb":model)
 Examples[name].html=html
 if(Cmds[name]){
	Cmds[name].examples=name
	addCmdExample(name,name)
 }
}

function addCmdExample(cmd,name){
	CmdExamples[cmd]=name
}

function newToken(text,isnull,description){
 if(isnull){
 }else{
 	thistoken="."+text
	Toks[thistoken]=new Array()
	Toks[thistoken].Namelist=new Array()
 }
 Toks[thistoken].Namelist[Toks[thistoken].Namelist.length]=text
}


function writetoks(){
 var s=""
 for(var i in Toks){
   s+=gettokhtml(Toks[i])
 }
 document.write(s)
}

function writecmds(){
 Cmdlist = Cmdlist.sort()
 var shead=""
 var skey=""
 var s=(dowritedocbook?"":dowritexml?"<cmdlist>":"<table width=\"700\">")
 for(var i=0;i<Cmdlist.length;i++){
   s+=getcmdhtml(Cmds[Cmdlist[i]])
 }
 if(!dowritedocbook)s+="</table xml=/cmdlist=xml>"
 var T=new Array()
 var nrows=0
 if(HeadList){
	HeadList=HeadList.sort()
	if(dowritedocbook){
		for(var i=0;i<HeadList.length;i++)T[T.length]="<td><xref linkend=\""+idof(HeadList[i])+"\"/></td>"
		shead="\n<informaltable>"
	}else{
		for(var i=0;i<HeadList.length;i++)T[T.length]="<td xml=headlistdata=xml>"
			+"<a style=\"text-decoration:none\" href=\"?ver="+specifiedversion+"#"+idof(HeadList[i])+"\"><img class=\"nf\" height=\"10\" width=\"10\" border=\"0\" src="+(Cmds[HeadList[i]].ihavewin?"\"img/ex.jpg\" title=\"includes example page\"":"\"img/ex.gif\" title=\"\"")+" />&nbsp;"
			+"<span"+(Cmds[HeadList[i]].isnew?" class=new":"")+">"
			+fixhtml(keyof(HeadList[i],0))
			+(Cmds[HeadList[i]].isnew?"&nbsp;*":"")
			+"</span></a>"
			+"</td xml=/headlistdata=xml>"

		shead=(dowritexml?"<headlist>":"<table width=\"800\">")
	}
	nrows=Math.floor((T.length+ncolumns-1)/ncolumns)
	if(dowritexml)nrows=1
	for(var i=0;i<nrows;i++){
		if(!dowritexml)shead+="\n<tr>"
		for(var j=i;j<T.length;j+=nrows)shead+=T[j]
		if(!dowritexml)shead+="</tr>"
	}
	if(!dowritedocbook && !dowritexml)shead+="<tr ><td colspan=6 ><span class=new>&nbsp;<br /><br />* indicates  new or modified in version "+specifiedversion+"</span> </td ></tr ><tr ><td >&nbsp;</td ></tr >"+thesep()
	shead+=(dowritedocbook?"\n</informaltable>":"</table xml=/headlist=xml>")
	s=shead+s
 }
 s = s.replace(/\@0\@/g, "{").replace(/\@1\@/g, "}")
 s = s.replace(/\@TILDE/g, "~")
 if(dowritedocbook){
	s=fixDocbook(s)
	s+="\n<section id=\"index\">\n<para>INDEX</para>\n"+theindex+"\n</section>"
	s=s.replace(/\<blockquote\>/g," ")
	s=s.replace(/\<\/blockquote\>/g," ")
	s=s.replace(/\&nbsp;/g," ")
	s=s.replace(/\&/g,"&amp;")
	s=s.replace(/\<i\>/g,"")
	s=s.replace(/\<\/i\>/g,"")
	s=s.replace(/\<b\>/g,"")
	s=s.replace(/\<\/b\>/g,"")
	s=s.replace(/\<br \/\>\<br \/\>/g,"</para><para>")
	s=s.replace(/\<br \/\>/g,"</para><para>")
	s=s.replace(/\</g,"&lt;")
	s=s.replace(/\n+/g,"\n")
	s=s.replace(/\n/g,"<br />")
	s=s.replace(/\\>/g,"<para>").replace(/\<\/p\>/g,"</para>")
	s=s.replace(/\'\'/g,'"')

 }else if(dowritexml){
	s+="<indexlist>"+theindex+"</indexlist>"
	s=s.replace(/ valign\=\"top\"/g,"")
	s=s.replace(/\<table\>/g," ")
	s=s.replace(/\<td\>/g," ")
	s=s.replace(/\<tr\>/g," ")
	s=s.replace(/\<blockquote\>/g," ")
	s=s.replace(/\<\/table\>/g," ")
	s=s.replace(/\<\/td\>/g," ")
	s=s.replace(/\<\/tr\>/g," ")
	s=s.replace(/\<\/blockquote\>/g," ")
	s=s.replace(/p xml\=/g,"")
	s=s.replace(/td xml\=/g,"")
	s=s.replace(/tr xml\=/g,"")
	s=s.replace(/table xml\=/g,"")
	s=s.replace(/i xml\=/g,"")
	s=s.replace(/b xml\=/g,"")
	s=s.replace(/\<\/\//g,"</")
	s=s.replace(/\<\=xml\>/g,"")
	s=s.replace(/\<\/\=xml\>/g,"")
	s=s.replace(/\&nbsp;/g," ")
	s=s.replace(/\&/g,"&amp;")
	s=s.replace(/\</g,"&lt;")
	s=s.replace(/\&lt\;cmd/g,"<br />&lt;cmd")
	s=s.replace(/\&lt\;\/cmd/g,"<br />&lt;/cmd")
	s=s.replace(/\&lt\;def/g,"<br />&lt;def")
	s=s.replace(/\&lt\;see/g,"<br />&lt;see")
	s=s.replace(/\&lt\;head/g,"<br />&lt;head")
	s=s.replace(/\&lt\;\/headlist\=/,"<br />&lt;/headlist\=")
	s=s.replace(/\&lt\;index/g,"<br />&lt;index")
	s=s.replace(/\&lt\;\/indexl/g,"<br />&lt;/indexl")
	s=s.replace(/\&lt\;jmol/g,"<br /><br />&lt;jmol")
	s=s.replace(/\&lt\;\/jmol/g,"<br />&lt;/jmol")
	s=s.replace(/\=xml/g,"")

 }else{
	s=s.replace(/xml\=\S*?\=xml/g,"")
	s+="<hr />"+theindex+"<hr />"
	s+="<p>last updated: "+lastupdate+"</p>"
 }
 docwrite(s)
}


function writedefs(){
 var s=""
 for(var i in Defs){
   s+=getdefhtml(Defs[i])
 }
 docwrite(s)
}

function sorton0(A,B){
	return(A[0]>B[0]?1:A[0]<B[0]?-1:0)
}

function getExamplesHTML(){
 var cmd=""
 var s="<select onchange=eval(value)><option value=\"0\">Examples</option>"
 var S=new Array()
 var List=new Array()
 var j=0
 var name=""
 for(var i in CmdExamples){
	List=CmdExamples[i].split(",")
	for (var l=0;l<List.length;l++){
		name=List[l]
		if(Examples[name]){
			if(Examples[name].model){
				S[j++]=[i.replace(/\./,"")+" ("+Examples[name].model.split(";")[0]+")",name]
			} else if(Examples[name].target){
				S[j++]=[i.replace(/\./,"")+" (new window)",name]
			}
			Cmds[i].ihavewin=(Examples[name].model||Examples[name].html)
		}
	}
 }
 S=S.sort(sorton0)
 for(var i=0;i<S.length;i++)s+="<option value=\"showModel('"+S[i][1]+"')\">"+S[i][0]+"</option>"
 s+="</select>"

 s+="<select onchange=eval(value)>"
 var List=versionlist.split(",")
 var j=0
 var name=""
 for (var l=1;l<List.length-1;l++)s+="<option "+(List[l]==specifiedversion?" selected=\"1\" ":"")+"value=\"showVersion('"+List[l]+"')\">version "+List[l]+"</option>"
 s+="</select>"
 return s 
}

function showVersion(v){
 var s=docbase+"?ver="+v
 document.location.href=s
}

function writeheader(){
 if(dowritexml||dowritedocbook)return
 var s="<h2><a id=\"top\">&nbsp;</a><a rel=\"_blank\" href=\"http://www.stolaf.edu/academics/chemapps/jmol\">Jmol</a> interactive scripting documentation</h2>"

 if(thesearch){
	s+="<p><br /><br /></p><h3>Search results for "+marksearch(thesearch)+"</h3><p><br /><br /><a href=\"javascript:setsearch()\"><img class=\"nf\" src=\"img/q.gif\" border=\"0\" />Search again</a> "
	 +"<a href=\""+docbase+"\"><img class=\"nf\" src=\"img/u.gif\" border=\"0\" />View Full Database</a> <a href=\"?ver="+specifiedversion+"#index\"><img class=\"nf\" src=\"img/i.gif\" border=\"0\" />Index</a><br /><br /></p>"
	theindex="<h3><a id=\"index\">Index</a> <a href=\""+xrefbase+"?ver="+specifiedversion+"#index\">(full)</a></h3>"
 }else{
	theindex="<h3><a id=\"index\">Index</a></h3>"
	s+="<form action=\"\"><table width=\"700\"><tr><td>"+startmessage+"<br /><br />"
	s+="<table width=\"750\"><tr><td><a href=\"javascript:setsearch()\"><img class=\"nf\" src=\"img/q.gif\" border=\"0\" />Search the Database</a> &nbsp; &nbsp; &nbsp;<a class=\"chimenote\" href=\"javascript:setsearch('chime note')\"><img  class=\"nf\" src=\"img/c.gif\" border=\"0\" />Chime Notes</a> &nbsp; &nbsp; &nbsp; <a href=\"?ver="+specifiedversion+"#index\"><img class=\"nf\" src=\"img/i.gif\" border=\"0\" />Index</a>&nbsp;&nbsp; &nbsp; &nbsp;"+getExamplesHTML()+"</td><td><a href=\"javascript:alert('These images mark places in the documentation where you \\ncan click on a link to pull up a working example in a new window.')\"><img src=\"img/ex.jpg\" border=\"0\" title=\"look for this icon throughout this document to pop up specific examples.\" /></td></tr></table></td></tr>"+thesep()+"</table></form>"
 } 
 docwrite(s)
}

function writeall(){
 document.write("<p>")
 if(dowritedocbook)document.write("<p>&lt;!-- Last Updated "+lastupdate+" --></p>&lt;!-- Automatically created from documentation using http://www.stolaf.edu/people/hansonr/jmol/docs/?docbook --></p>")
 if(dowritexml)document.write("<p>&lt;document lastupdate=\""+lastupdate+"\"></p>")
 writeheader()
 writecmds()
 writedefs()
 writetoks()
 if(dowritedocbook){
 }else if(dowritexml){
	document.write("<p>&lt;/document></p>")
 }else{
	document.write("<p><a href=\"index.htm?xml\">xml</a> <a href=\"index.htm?docbook\">docbook</a></p>")
 }
}
 
function getdefhtml(D){
 var s=""
 return s
}

function gettokhtml(T){
 var s=""
 return s
}

function keyof(sname,isfull){
 var C=Cmds[sname]
 var subset=sname.split(" (")[1]
 var s=sname.split(" (")[0]
 subset=(subset?" ("+subset:"")
 if(Toks[s])s=Toks[s].Namelist.join(" or ")
 if(!isfull && subset)s=s.split(" ")[0]
 s=s.replace(/\./,"")+subset
 var isunimplemented=(!C.isimplemented || s.indexOf("not implemented")>0)
 if(!isunimplemented)return s
 if(dowritexml||dowritedocbook)return ""
 return "<fC=\"#C0C0C0\">"+s+"</f>"
}

function getcmdhtml(C){
 var sp=""
 var st=""
 var theseexamples=""
 var sdefault=""
 var sname=C.name.split(" (")[0]
 var LineList=new Array()
 var deflist="" 
 var newdefs=""
 var definfo=""
 var cmdoption=""
 var sline=""
 var skey=""
 var ikey=0
 var sindextemp=""
 var ihavedesc=0
 var isunimplemented=(!C.isimplemented || C.name.indexOf("implemented")>0)
 var shead=""
 var s=keyof(C.name,0)
 var cmdoptionreal=""
 var spreal=""
 if(isunimplemented && (dowritexml||dowritedocbook))return ""
 ikey=getIndexKey(s)
 var keyname=s
 shead="<a id=>"+marksearch(s)+"</a><a id=\"k"+ikey+"\">&nbsp;</a>"
 if(dowritedocbook){
	sindextemp="\n<para><xref linkend=\""+idof(s)+"\"/></para>\n"
	shead="\n<section xreflabel=\""+s+"\" id=><title>"+marksearch(s)
		+(C.version!="" && C.isnew?" (v. "+C.version+")":"")
		+"</title>"
 }else if(dowritexml){
	sindextemp="<b xml=indextermmain=xml><br /><a href=\"?ver="+specifiedversion+"#k"+ikey+"\">"+s+"</a><a id=\"k"+(ikey+1)+"\">&nbsp;</a><br /></b xml=/indextermmain=xml>\n"
	shead="<jmolcmd><cmdname>"+shead+"</cmdname>"
 }else{
	sindextemp="<b><br /><a href=\"?ver="+specifiedversion+"#"+idof(s)+"\">"+s+"</a><a id=\"k"+(ikey+1)+"\">&nbsp;</a><br /></b>\n"
	shead="<h3>"+shead
		+(C.version!="" && C.isnew?" <br /><span class=new>(v. "+C.version+")</span>":"")
		+"</h3>"
	shead="<tr><td colspan=\"5\">"+shead+(C.isimplemented?"":"<p><i>not implemented</i></p>")+"</td></tr>"
 }
 ikey=(nindexkey++)
 s=""
 var includethis=false
 var searchinfo=C.name

 if(!C.isimplemented){
	s=shead
	if(!foundsearch(keyof(C.name,1)+" not implemented"))s=""
 }else{
	if(dowritedocbook){
		if(!isunimplemented)shead+="\n<para>"+(C.description?C.description:"description will appear here")+"</para>"
	}else{
		if(!isunimplemented)shead+="<tr><td><i xml=cmddescription=xml>"+(C.description?C.description:"description will appear here")+"<br /><br /></i xml=/cmddescription=xml></td></tr>"
	}
	LineList[0]=""
	for(var i=0;i<C.list.length;i++){
		sline=""
		definfo=""
		newdefs=""
		if(!C.list[i].isimplemented){
			if(!dowritexml && !dowritedocbook){
				sline+="<tr><td xml=cmdlisti=xml valign=\"top\"><p>"+marksearch(sname.replace(/\./,""))
				sline+=" "+marksearch(C.list[i].Param[1].replace(/\./,""))+" <i>--not implemented</i></p></td xml=/cmdlisti=xml></tr>"
			}
		}else if(C.list[i].Nparams.length==1 && C.list[i].Nparams[0]!="0"){
			cmdoption=sname.replace(/\./,"")
			cmdoptionreal=cmdoption.split(" ")[0]
			for(var p=1;p<=C.list[i].lastparam;p++){
				sp=C.list[i].Param[p]
				sdefault=""
				if(sp.indexOf("{")>=0){
				  if (sp.indexOf("{{")>=0) {
					sp=sp.replace(/\{\{/g,"{").replace(/\}\}/g,"}")
				  }else{
					sdefault="{default: "+sp.substring(sp.indexOf("{")+1,sp.length)
					sp=sp.substring(0,sp.indexOf("{"))
				  }
				}
				if(Defs[sp]){
					ihavedesc=(Defs[sp].label.charAt(0)=="["&&Defs[sp].description)
					if(ihavedesc&&deflist.indexOf(sp)<0&&newdefs.indexOf(sp)<0){
						newdefs+="|"+sp
						definfo+=defhtml(sp)
					}
					sp=Defs[sp].label
					spreal=sp
					if(ihavedesc)sp=(dowritedocbook?"<xref linkend=''d"+getIndexKey(sp+ikey)+"''/>":"<a href='?ver="+specifiedversion+"#d"+getIndexKey(sp+ikey)+"'>"+sp+"</a>")
				}else{
					sp=sp.replace(/\./,"")
					spreal=sp
				}
				sp+=sdefault
				spreal+=sdefault
				sp=sp.replace(/\[\]/g,"").replace(/\"/g,"")
				spreal=spreal.replace(/\[\]/g,"").replace(/\"/g,"")
				sp=cleanconstants(sp)
				spreal=cleanconstants(spreal)
				cmdoption+=" "+sp
				cmdoptionreal+=" "+spreal
			}

			skey=(C.list[i].Nparams[0].charAt(0)=="*"?C.list[i].Nparams[0]:"")+cmdoption.replace(/ /g,"~")
			if(dowritedocbook){
				sline+="\n<varlistentry><term><command id=\"k"+getIndexKey(skey)+"\" xreflabel=\""+cmdoptionreal+"\">"+marksearch(icandoxrefincommand?cmdoption:cmdoptionreal)+"</command></term>"
			}else{
				if(dowritexml)sline+="<cmdexample>"
				sline+="<tr><td xml=cmdoption=xml valign=\"top\"><a id=\"k"+getIndexKey(skey)+"\">&nbsp;</a>"+marksearch(cmdoption)+"</td xml=/cmdoption=xml></tr>"
			}

			if(C.list[i].description){
				if(dowritedocbook){
					sline+="\n<listitem><para>"+C.list[i].description+"</para></listitem>"
				}else{
					sline+="<tr><td><blockquote><p xml=cmdlistidescription=xml><i xml==xml>"+C.list[i].description+"</i xml==xml></p xml=/cmdlistidescription=xml></blockquote></td></tr>"
				}
			}
			if(C.list[i].examples){
				sline+=getexamples(C.list[i].examples)
			}
			if(dowritedocbook){
				if(!C.list[i].description && !C.list[i].examples)sline+="<listitem><para> </para></listitem>"
				sline+="</varlistentry>"
			}
			if(dowritexml)sline+="</cmdexample>"

		}
		if(foundsearch(shead+sline+definfo+" "+cmdoption+" "+(C.chimenote?"chime note: "+C.chimenote:"")+getexamples(C.examples))){
			LineList[LineList.length]=skey+"|:|"+sline
			deflist+=newdefs
			includethis=true
		}

	}

	if(includethis){
		s=shead
		LineList=LineList.sort()

		st=""

		for(var i=0;i<LineList.length;i++)if(LineList[i]){
			skey=getIndexKey(LineList[i].split("|:|")[0])
			if(skey)sindextemp+=indexkeytag(skey,LineList[i].split("|:|")[0])+"\n"
		}

		for(var i=0;i<LineList.length;i++)if(LineList[i]){
			st+=LineList[i].split("|:|")[1]+"\n"
		}

		if(st.length){
			if(dowritedocbook){
				s+="\n<variablelist><title>Syntax</title>"+st+"\n</variablelist>"
			}else if(dowritexml){
				s+="<cmdexamples>"+st+"</cmdexamples>"
			}else{
				s+=st
			}
		}
		if(deflist.length){
			if(dowritedocbook){
				s+="\n<variablelist><title>Definitions</title>"
			}else{
				if(!dowritexml)s+="<tr><td><p><br /><i>where</i><br /></p>"
				s+="<table xml=cmddefinitions=xml>"
			}
			LineList=deflist.substring(1,deflist.length).split("|")
			for(var i=0;i<LineList.length;i++){
				sp=Defs[LineList[i]].label
				if(dowritedocbook){
					sp="\n<varlistentry><term><option id=\"d"+getIndexKey(sp+ikey)+"\" xreflabel=\""+sp+"\">"+marksearch(sp)+"</option></term><listitem><para>is "+defhtml(LineList[i])+"</para></listitem></varlistentry>"
				}else{
					sp="<tr xml=cmddef=xml><td valign=\"top\">&nbsp;&nbsp;<b xml=defkey=xml><a id=\"d"+getIndexKey(sp+ikey)+"\">"+marksearch(sp)+"</a></b xml=/defkey=xml></td><td xml=defdata=xml>is "+defhtml(LineList[i])+"</td xml=/defdata=xml></tr xml=/cmddef=xml>"
				}
				s+=cleanconstants(sp).replace(/ \./g," ").replace(/\>is is /,">is ")
			}
			if(dowritedocbook){
				s+="\n</variablelist>"
			}else{
				s+="</table xml=/cmddefinitions=xml>"
				if(!dowritexml)s+="</td></tr>"
			}
		}
		if(!isunimplemented)s+=getlinkhtml(C)
	}
 }
 if(s){
	if(!dowritexml && !dowritedocbook)s+=thesep(ikey)
	HeadList[HeadList.length]=C.name
	if(dowritedocbook){
		theindex+=sindextemp
	}else{
		theindex+="<p xml=indexlistset=xml>"+sindextemp+"</p xml=/indexlistset=xml>"
	}
 }
 s=fixhtml(s.replace(/ id\=\>/," id=\""+idof(C.name)+"\">"))
 if(dowritexml)s+="</jmolcmd>"
 if(dowritedocbook)s+="\n</section>"
 return s
}

function indexkeytag(skey,sinfo){
	var S=sinfo.replace(/\~/g," ").replace(/^\*\d*/,"").split("<")
	if(!S[0])return ""
	if(dowritedocbook){
		S[0]="<xref linkend=\"k"+skey+"\"/>"
		var s=S.join(" <")	
		return "\n<para>"+s+"</para>"
	}
	S[0]="<a href=\"?ver="+specifiedversion+"#k"+skey+"\">"+S[0]+"</a>"
	var s=S.join("<")+"<br />"
	if(dowritexml)s="<indexterm>"+s+"</indexterm>"
	return s
}

function getIndexKey(s){
 if(!IndexKeyList[s])IndexKeyList[s]=(nindexkey++)
 return IndexKeyList[s]
}

function getexamples(slist,iusenoheader){
 var S=slist.split(",")
 var s=""
 for (var i=0;i<S.length;i++)s+=getexample(S[i],iusenoheader)
 return s
}

function getexample(swhat,iusenoheader){
 var S=new Array()
 var s=""
 if(!Examples[swhat])return ""
 if(dowritedocbook){
	s+="\n<listitem>"+(iusenoheader?"":"<para>Examples:</para>")
 }else{
	if(!dowritexml)s+="<tr><td>"+(Examples[swhat].model?"<img border=\"0\" src=\"img/ex.jpg\" title=\"pop up example\" /></a>":"")
	+"<br /><i class=\"example\">Examples:</i> "+(Examples[swhat].model?"<a href=\"javascript:showModel('"+swhat+"')\">in new window using "+Examples[swhat].model.split(";")[0]:"")+"</td></tr>"
 }
 if(!Examples[swhat]){
	return "<tr><td><p>Examples["+swhat+"] has not been defined in exam.js</p></td></tr>"
 }
 var scr=Examples[swhat].script
 if(scr){
	if(scr.indexOf("||")<0)scr=scr.replace(/\;/g,"||")
	if(dowritedocbook){
		scr="\n<programlisting>"+scr.replace(/\</g,"&lt;").replace(/\|\|/g,"\n")+"\n</programlisting>"
	}else if(dowritexml){
		scr="<cmdscriptlist>\n<cmdscript>"+scr.replace(/\</g,"&lt;").replace(/\|\|/g,"</cmdscript>\n<cmdscript>")+"</cmdscript>\n</cmdscriptlist>"
	}else{
		scr="<tr><td><table cellpadding=\"10\"><tr><td class=\"example\">"+marksearch(scr.replace(/\</g,"&lt;").replace(/\|\|/g,"<br />"))+"</td></tr></table></td></tr>"
	}
	s+=scr
 }
 if(Examples[swhat].html && !dowritedocbook){
	s+=(dowritexml?"<cmdhtml>":"<tr><td><img border=\"0\" src=\"img/ex.jpg\" title=\"example page\" /></a><br />&nbsp;&nbsp;&nbsp;See ")
	S=Examples[swhat].html.split(",")
	for(var i=0;i<S.length;i++){
		s+="<a rel=\"_blank\" href=\""+(S[i].indexOf("/")>=0?"":exampledir) + S[i]+"\">"+S[i]+"</a>&nbsp;"
	}
	s+=(dowritexml?"</cmdhtml>":"</td></tr>")
 }
 if(dowritedocbook)s+="\n</listitem>"
 return s
}

/*

Does not work on some Macs

function showModel(swhat_ext,smodel){

 swhat_ext=swhat_ext.replace(/\|/,"~")+"~"
 var swhat=swhat_ext.split("~")[0]
 var ext=swhat_ext.split("~")[1]
 if(ext)ext=";"+ext
 var E=Examples[swhat]
 if (E.target){
   open((E.html.indexOf("/")>=0?"":exampledir) + E.html, E.target)
   return
 }
 smodel=(smodel?smodel:E&&E.model?E.model:"1crn.pdb")
 var scrdef="set defaultDirectory \""+datadir+"\";load "+smodel+"; "

 var useGetElement = (_jmol.useHtml4Object || _jmol.useIEObject)
 var shead='<html>\n<head>\n<title>'+swhat+" "+smodel+'</title>\n'
	+'\n<script type="text/javascript" src="'+popupscript+'"></script>'
	+'\n<script type="text/javascript">\ndefaultloadscript=\''+scrdef+'\';\nthismodel="'+datadir + smodel.split(";")[0]+'"\nuseGetElement='+useGetElement+'\n</'+'script>\n</head>'
 	+'\n<body>\n<p>'+swhat.replace(/\./,"")+" "+smodel+'\n</p><form name="info" id="info" action="">'

 var S=(E?E.script.split("||"):[""])
 var s=""
 var sapp=""
 var sdata=""
 var stext=""
 var scr = ""
 if(S.length>1){
	sdata+="<ul>"
	for(var i=0;i<S.length;i++)sdata+="\n<li>"+(S[i].charAt(0)=="#"?S[i]:"<a href=\"javascript:void(jmolScript(unescape('"+escape(S[i])+"')))\">"+S[i]+"</a>")+"</li>"
	sdata+="</ul>"
 }else{
	scr+=S[0]
 }
 scr+=ext
 sdata+='\n<p><br /><textarea name="cmds" id="cmds" rows="'+(S.length>1?5:15)+'" cols="45">\n'+scr.replace(/\;/g,";\n")+'\n</textarea><br /><input type="button" value="Execute" onclick="jmolScriptInfo()" />\n</p>'

 s=shead+'\n<table border="1"><tr><td>\n'
	
 if(dousejmoljs){

 //can't write while writing! IE needs defer=defer. But better solution is just to get code and write it.
	if(useobject && !_jmol.useIEObject)_jmol.useHtml4Object=1 //force object not applet
	sapp=jmolApplet(400,scrdef+scr,"X")
	stext+='\n<script type="text/javascript" src="'+jmoljs+'"></script>'
	+'\n<script type="text/javascript">'
	+'\nuseGetElement='+useGetElement+';'
	+'\n//jmolInitialize(".");'
	+'\njmolApplet(400, defaultloadscript+"'+scr+'","X");'
	+'\n</script>'
 }else{

	stext=sapp="\n<applet name='jmolAppletX' id='jmolAppletX' code='JmolApplet'\n archive='JmolApplet.jar' codebase='.' width='400' height='400' mayscript='true'>"
	+"\n<param name='progressbar' value='true' />"
	+"\n<param name='progresscolor' value='blue' />"
	+"\n<param name='boxmessage' value='Downloading JmolApplet ...' />"
	+"\n<param name='boxbgcolor' value='black' />"
	+"\n<param name='boxfgcolor' value='white' />"
	+"\n<param name='script' value='"+scrdef+scr+"' />"
	+"\n</applet>"
 }
 s+=sapp
 s+="<br><a href=\"javascript:getModel('X')\">load a different PDB file or one of my own molecules</a>"
 s+=" <a href=\"javascript:loadModel('X')\">(reload)</a>"
 s+=" <a href=\"javascript:showModelText('X')\">(text)</a>"
 s+='\n</td><td>\n'
	+sdata
	+'\n</td></tr></table>\n</form>\n<p>'
	+jmolversion+(dousejmoljs?"":" not")+" using <a id=\"blank_link\" href=\"Jmol-11.js\">Jmol-11.js</a>"+(sapp.indexOf("<object")>=0?" object applet":"")
  	+"\n</p><pre>"+(shead+stext+"\n</form>\n</body>\n</html>").replace(/\</g,"&lt;")+"</pre>"
	+'\n</body>'
	+'\n</html>'
	var opt="menubar,scrollbars,width=900,height=600,left=100,top=30"
	var sm=""+Math.random()
	sm=sm.substring(3,10)
	var w=open("","DT_"+sm,opt) //blank? for what?
	w.document.open()
	w.document.write(s)
	w.document.close()
}

*/

function showModel(swhat_ext,smodel){

 swhat_ext=swhat_ext.replace(/\|/,"~")+"~"
 var swhat=swhat_ext.split("~")[0]
 var ext=swhat_ext.split("~")[1]
 if(ext)ext=";"+ext
 var E=Examples[swhat]
 if (E.target){
   open((E.html.indexOf("/")>=0?"":exampledir) + E.html, E.target)
   return
 }
 smodel=(smodel?smodel:E&&E.model?E.model:"1crn.pdb")
 var scrdef="set loadStructCallback \"fileLoadedCallback\"; set defaultDirectory \""+datadir+"\";load "+smodel+"; "
 var useGetElement = (_jmol.useHtml4Object || _jmol.useIEObject)
 var theTitle = swhat.replace(/\./,"") + ": " + smodel
 var useList = (E.script.indexOf("||") > 0)
 var theList = (E.script && useList ? E.script : "")
 var nRows = (theList.split("||").length>1?5:15)
 var theText = (E.script && !useList ? E.script.replace(/\;/g,";\n") : "")
 var theScript = (theText != "" ? E.script : "")
 var js = 'version="'+jmolversion+'";defaultloadscript=\''+scrdef+'\';thismodel="'+datadir + smodel.split(";")[0]+'";useGetElement='+useGetElement
 var fields = "&js="+escape(js)
	+"&title="+escape(theTitle)
	+"&list="+escape(theList)
	+"&text="+escape(theText)
	+"&script="+escape(theScript)
 var opt="menubar,scrollbars,width=900,height=600,left=100,top=30"
 var sm=""+Math.random()
 sm=sm.substring(3,10)
 open("popupTemplate.htm?"+fields,"DT_"+sm,opt)
}


function getchimenote(swhat){
 var s=""
 if(!dowritexml && !dowritedocbook)s+="<tr><td class=\"chimenote\"><p><br /><i>Chime Note:</i></p></td></tr>"
 if(dowritedocbook){
	swhat="<para>"+swhat+"</para>"
 }else if(dowritexml){
	swhat="<cmdchimenote>"+swhat+"</cmdchimenote>"
 }else{
	swhat="<tr><td class=\"chimenote\"><table cellpadding=\"10\"><tr><td class=\"chimenote\"><p>"+swhat+"</p></td></tr></table></td></tr>"
 }
 return s+swhat
}


function getlinkhtml(C){
 if(!C.examples && !C.xrefs && !C.chimenote)return ""
 var slist=""
 var xref=""
 var sout=""
 if(C.examples){
	if(dowritedocbook)sout+="\n<variablelist><title>Examples</title>\n<varlistentry>"
	sout+=getexamples(C.examples,1)
	if(dowritedocbook)sout+="\n</varlistentry>\n</variablelist>"

 }
 var s=""
 var S=new Array()
 var L=new Array()
 if(C.chimenote){
	sout+=getchimenote(C.chimenote)
 }
 if(!C.xrefs)return sout
 L=C.xrefs.split(",")
 for(var i=1;i<L.length;i++)slist+=","+Xrefs[L[i]]
 S=slist.split(",").sort()
 slist=""
 for(var i=1;i<S.length;i++){
	if(S[i]!=C.name && slist.indexOf(S[i])<0){
		if(dowritexml)s+="<seealso>"
		if(dowritedocbook){
			s+="<xref linkend=\""+idof(S[i])+"\"/> "
		}else{
			s+="<a class=\"xref\" href=\""+xrefbase+"?ver="+specifiedversion+"#"+idof(S[i])+"\">"+S[i].replace(/\./,"")+"</a>  "
		}
		if(dowritexml)s+="</seealso>"
		slist+=S[i]
	}
 }
 if(!s)return sout
 if(dowritedocbook)return "<para>See also:"+s+"</para>"
 return sout+(dowritexml?"<cmdxref>":"<tr><td><p><br /><i>See also:</i></p></td></tr><tr><td colspan=\"5\"><p>")+s+(dowritexml?"</cmdxref>":"</p></td></tr>")
}

function fixhtml(s){
 return s.replace(/\<fC\=/g,"<font color=").replace(/\<\/f\>/,"</font>").replace(/\'\'/g,'"')
}

function marksearch(s,isdescr){
 if(!s)return s
 r=(thesearch?new RegExp("("+thesearch+")","gi"):"")
 if(!isdescr||s.indexOf("{")<0)return (r?s.replace(r,"<span class=\"found\">$1</span>"):s)
 // see {.set mode~set mode} etc. 
 //  0     1         2        3
 var S=s.replace(/(\}|\~)/g,"{").split("{")
 s=""
 var st=""
 for(var i=0;i<S.length;i+=3){
	s+=(r?S[i].replace(r,"<font color=red>$1</font>"):S[i])
	if(i+2<S.length){
		st=S[i+2]
		if(st=="")st=S[i+1].replace(/(\#|\.)/g,"")
		if(dowritedocbook){
			s+=unescape(st)  //bold here?
		}else{
			s+="<a class=\"xref\" href=\""+(S[i+1].indexOf("#")!=0?S[i+1]+"\" rel=\"_blank"
				:xrefbase+"?ver="+specifiedversion+idof(S[i+1]))+"\">"+unescape(st)+"</a>"
		}
	}
 }
 return s
}

function foundsearch(s){
 return (thesearch==""||s.toLowerCase().indexOf(thesearch.toLowerCase())>=0)
}

function getdeftypelist(sdef){
 var s=Defs[sdef].typelist
 if(s.length<3||Defs[sdef].description.length<5)return " "+s
 if(s.indexOf("._")<0)return " -- "+s
 var S=s.split(", ")
 while(s.indexOf("._")>=0){
	for(var i=0;i<S.length;i++){
		if(Defs[S[i]])S[i]=Defs[S[i]].typelist
		if(S[i].indexOf("._")>=0){
			alert("Definition of "+S[i]+" not found in definitions of " + sdef)
			S[i]=S[i].replace(/\.\_/g,"?_")
		}
	}
	s=S.join(", ")
 }
 Defs[sdef].typelist=s
 return " -- "+s
}


function defhtml(s){
 return Defs[s].description+marksearch(getdeftypelist(s))
}

function cleanconstants(sp){
 var r=""
 if(sp.indexOf("JmolConstants.")){
	for(var i in JmolConstants){
		r=new RegExp("JmolConstants\\."+i,"g")
		sp=sp.replace(r,JmolConstants[i])
	}
 }	
 return sp
}

function docwrite(s){
	if(isxhtmltest)s=s.replace(/\&lt/g,"&amp;lt").replace(/\<\//g,"&lt;/").replace(/\</g,"<br />&lt;")
	document.write(s)
}

function idof(s){
	s=s.replace(/\(\#\)/,"")
	return unescape(s).replace(/(\.|\(|\))/g,"").replace(/\s+/g,"")
}

