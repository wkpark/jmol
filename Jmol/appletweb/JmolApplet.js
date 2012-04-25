// JmolApplet.js -- Jmol._Applet and Jmol._Image

(function (Jmol) {


  /*  AngelH, mar2007:
    By (re)setting these variables in the webpage before calling jmolApplet(),
    a custom message can be provided (e.g. localized for user's language) when no Java is installed.
  */
		Jmol._noJavaMsg == undefined && (Jmol._noJavaMsg =
        "You do not have Java applets enabled in your web browser, or your browser is blocking this applet.<br />\
        Check the warning message from your browser and/or enable Java applets in<br />\
        your web browser preferences, or install the Java Runtime Environment from <a href='http://www.java.com'>www.java.com</a>");
		Jmol._noJavaMsg2 == undefined && (Jmol._noJavaMsg2 =
        "You do not have the<br />\
        Java Runtime Environment<br />\
        installed for applet support.<br />\
        Visit <a href='http://www.java.com'>www.java.com</a>");

	Jmol._setCommonMethods = function(proto) {
		proto._showInfo = Jmol._Applet.prototype._showInfo;	
		proto._search = Jmol._Applet.prototype._search;
	}

	// _Applet -- the main, full-featured, object
	
	Jmol._Applet = function(id, Info, caption, checkOnly){
		this._jmolType = "Jmol._Applet" + (Info.jmolIsSigned ? " (signed)" : "");
		if (checkOnly)
			return this;
		Jmol._targetId = this._id = id;
		this._width = Info.width;
		this._height = Info.height;
		this._jmolIsSigned = Info.jmolIsSigned;
		this._dataMultiplier=1;
		this._hasOptions = Info.addSelectionOptions;
		this._info = JSON.stringify(this);
		this._infoHeader = this._jmolType + ' "' + this._id + '"'
		this._defaultModel = Info.defaultModel;
		this._readyFunction = Info.jmolReadyFunction;
		this._readyScript = (Info.script ? Info.script : "");
		this._ready = false; 
		this._applet = null;
		this._jmolJarFile = Info.jmolJarFile || (Info.jmolIsSigned ? "JmolAppletSigned0.jar" : "JmolApplet0.jar"); 
		this._jmolJarPath =	Info.jmolJarPath || "."; 
		this._memoryLimit = Info.memoryLimit || 512;
		this._canScript = function(script) {return true;};
		
		/*
		 * private variables
		 */
		var that = this;
		
		/*
		 * private methods
		 */
		var getJarFilename=function(fileNameOrFlag){
			that._jmolJarFile =
	    	(typeof(fileNameOrFlag) == "string"  ? fileNameOrFlag : (fileNameOrFlag ?  "JmolAppletSigned" : "JmolApplet") + "0.jar");
		}
		var setCodebase=function(codebase) {
 			that._jmolJarPath = codebase ? codebase : ".";
		}
		
		/*
		 * privileged methods
		 */
		this._initialize = function(codebaseDirectory, fileNameOrUseSignedApplet) {
			if(this._jmolJarFile) {
				var f = this._jmolJarFile;
				if(f.indexOf("/") >= 0) {
					alert("This web page URL is requesting that the applet used be " + f + ". This is a possible security risk, particularly if the applet is signed, because signed applets can read and write files on your local machine or network.");
					var ok = prompt("Do you want to use applet " + f + "? ", "yes or no")
					if(ok == "yes") {
						codebaseDirectory = f.substring(0, f.lastIndexOf("/"));
						fileNameOrUseSignedApplet = f.substring(f.lastIndexOf("/") + 1);
					} else {
						getJarFilename(fileNameOrUseSignedApplet);
						alert("The web page URL was ignored. Continuing using " + this._jmolJarFile + ' in directory "' + codebaseDirectory + '"');
					}
				} else {
					fileNameOrUseSignedApplet = f;
				}
			}
			setCodebase(codebaseDirectory);
			getJarFilename(fileNameOrUseSignedApplet);
			Jmol.controls == undefined || Jmol.controls._onloadResetForms();		
		}
		
		this._create(id,Info);
		  	
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, caption);
		return this;
		
	}

	Jmol._Applet.prototype._create = function(id, Info){
		/*
		 * private variables
		 */
		var _windowsClassId = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
		var _windowsCabUrl = "http://java.sun.com/update/1.6.0/jinstall-6u22-windows-i586.cab";

		/*
		 * private methods
		 */
		function sterilizeScript(script) {
			script = script.replace(/'/g, "&#39;");
			if (Jmol._debugAlert)
				alert("script:\n" + script);
			return script;
		}
		function sterilizeInline(model) {
			model = model.replace(/\r|\n|\r\n/g, (model.indexOf("|") >= 0 ? "\\/n" : "|")).replace(/'/g, "&#39;");
			if(Jmol._debugAlert)
				alert("inline model:\n" + model);
			return model;
		}

		function writeParams() {
 			var t = "";
 			console.log(params);
 			for (var i in params)
				if(params[i]!="")
		 			t+="  <param name='"+i+"' value='"+params[i]+"' />\n";
 			return t;
		}
		
		function getParameters(Info){
			var availableValues = ['progressbar','progresscolor','boxbgcolor','boxfgcolor','boxmessage',
										'messagecallback','pickcallback','animframecallback','appletreadycallback','atommovedcallback',
										'echocallback','evalcallback','hovercallback','language','loadstructcallback','measurecallback',
										'minimizationcallback','resizecallback','scriptcallback','statusform','statustext','statustextarea',
										'synccallback'];
				for (var i in Info)
					if(availableValues[i.toLowerCase()])
						params[i] = Info [i];
		}
		
		/*
		 * Applet creation
		 */
		this._initialize(Info.jmolJarPath, Info.jmolJarFile);
		//var suffix = id.replace(/^jmolApplet/,"");
		var params = {
			syncId: ("" + Math.random()).substring(3),
			progressbar: "true",
			progresscolor: "blue",
			boxbgcolor: Info.color || "black",
			boxfgcolor: "white",
			boxmessage: "Downloading JmolApplet ..."
		};
		params.appletReadyCallback = this._id + ".readyCallback";
		
		//var sz = j._applet.getSize();
		var widthAndHeight = " width='" + this._width + "' height='" + this._height + "' ";
		var tHeader, tFooter;
		getParameters(Info);
			
		if (Jmol.featureDetection.useIEObject || Jmol.featureDetection.useHtml4Object) {
			params.archive = this._jmolJarFile;
			params.mayscript = 'true';
			params.codebase = this._jmolJarPath;
			params.code = 'JmolApplet.class';
			tHeader =
				"<object name='" + this._id +
				"' id='" + this._id + "' " + "\n" +
				widthAndHeight + "\n";
			tFooter = "</object>";
		}
		//if (java_arguments) //PP java_arguments is always set
		params.java_arguments = "-Xmx" + Math.round(Info.memoryLimit || this._memoryLimit) + "m";
		if (Jmol.featureDetection.useIEObject) { // use MSFT IE6 object tag with .cab file reference
			tHeader += " classid='" + _windowsClassId + "'\n" + " codebase='" + _windowsCabUrl + "'\n>\n";
		} else if (Jmol.featureDetection.useHtml4Object) { // use HTML4 object tag
			tHeader += " type='application/x-java-applet'\n>\n";
				/*	" classid='java:JmolApplet'\n" +	AH removed this
					Chromium Issue 62076:	 Java Applets using an <object> with a classid paramater don't load.
					http://code.google.com/p/chromium/issues/detail?id=62076
					They say this is the correct behavior according to the spec, and there's no indication at this point
					that WebKit will be changing the handling, so eventually Safari will acquire this behavior too.
					Removing the classid parameter seems to be well tolerated by all browsers (even IE!).
				*/ 
		} else { // use applet tag
			tHeader =
				"<applet name='" + this._id +
				"' id='" + this._id + "' \n" +
				widthAndHeight + "\n" +
				" code='JmolApplet'" +
				" archive='" + this._jmolJarFile + "' codebase='" + this._jmolJarPath + "'\n" +
				" mayscript='true'>\n";
			tFooter = "</applet>";
		}
		var visitJava;
		if (Jmol.featureDetection.useIEObject || Jmol.featureDetection.useHtml4Object) {
			var szX = "width:" + this._width;
			if ( szX.indexOf("%")==-1 ) 
				szX+="px";
			var szY = "height:" + this._height;
			if ( szY.indexOf("%")==-1 )
				szY+="px";
			visitJava = "<p style='background-color:yellow; color:black; " + szX + ";" + szY + ";" +
					// why doesn't this vertical-align work?
				"text-align:center;vertical-align:middle;'>\n" +
				Jmol._noJavaMsg + "</p>";
		} else {
			visitJava = "<table bgcolor='yellow'><tr>" +
				"<td align='center' valign='middle' " + widthAndHeight + "><font color='black'>\n" +
				Jmol._noJavaMsg2 + "</font></td></tr></table>";
		}
		params.loadInline = (Info.inlineModel ? sterilizeInline(Info.inlineModel) : "");
		//params.script = (Info.script ? sterilizeScript(Info.script) : "");
		var t = tHeader + writeParams() + visitJava + tFooter;
		if (Jmol._debugAlert)
			alert(t);
		Jmol._getWrapper(this, true);
		document.write(t);
		Jmol._getWrapper(this, false);
	}
	
	Jmol._Applet.prototype.readyCallback = function(id, fullid, isReady, applet) {
		if (!isReady)
			return; // ignore -- page is closing
		this._ready = true;
		var script = this._readyScript;
		this._applet = applet;		
		if (this._defaultModel)
			this._search(this._defaultModel, (script ? ";" + script : ""));
		else if (script)
			this._script(script);
		this._readyFunction && this._readyFunction(this);
	}
	
	Jmol._Applet.prototype._showInfo = function(tf) {
		if ((!this._isInfoVisible) == (!tf))
			return;
		this._isInfoVisible = tf;
		Jmol._getElement(this, "infotablediv").style.display = (tf ? "block" : "none");
		Jmol._getElement(this, "appletdiv").style.height = (tf ? 1 : this._height) + "px";
		Jmol._getElement(this, "appletdiv").style.width = (tf ? 1 : this._width) + "px";
		if (!tf)//&& Jmol._isMsieRenderBug -- occurring also on Mac systems)
			alert("returning to applet...");
		this._show(!tf);
		if (tf) {
			Jmol._getElement(this, "infoheaderdiv").innerHTML = this._infoHeader;
			Jmol._getElement(this, "infodiv").innerHTML = this._info;
		}
	}

	Jmol._Applet.prototype._search = function(query, script){
		this._showInfo(false);
		Jmol._setQueryTerm(this, query);
		query || (query = Jmol._getElement(this, "query").value);
		query && (query = query.replace(/\"/g, ""));
		var database;
		if (Jmol._isDatabaseCall(query)) {
			database = query.substring(0, 1);
			query = query.substring(1);
		} else {
			database = (this._hasOptions ? Jmol._getElement(this, "select").value : "$");
		}
		if (database == "=" && query.length == 3)
			query = "=" + query; // this is a ligand			
		var dm = database + query;
		if (!query || dm.indexOf("?") < 0 && dm == this._thisJmolModel)
			return;
		this._thisJmolModel = dm;
		if (database == "$" || database == ":")
			this._jmolFileType = "MOL";
		else if (database == "=")
			this._jmolFileType = "PDB";
		this._searchDatabase(query, database, script);
	}
	
	Jmol._Applet.prototype._loadModel = function(mol, params) {
		var script = 'load DATA "model"\n' + mol + '\nEND "model" ' + params;
		this._applet.script(script);
	}
	
	Jmol._Applet.prototype._show = function(tf) {
		var w = (tf ? this._width : 1) + "px";
		var h = (tf ? this._height : 1) + "px";
			document.getElementById(this._id).style.width = w; 
			document.getElementById(this._id).style.height = h; 
	}
	
	Jmol._Applet.prototype._script = function(script) {
		if (!this._ready) {
			this._readyScript || (this._readyScript = ";");
			this._readyScript += ";" + script;
			return; 
		}
		this._applet.script(script);
	}	
	
	Jmol._Applet.prototype._loadFile = function(fileName, params){
		this._showInfo(false);
		params || (params = "");
		this._thisJmolModel = "" + Math.random();
		if (this._jmolIsSigned) {
			this._script("load \"" + fileName + "\"" + params);
			return;
		}
		var c = this;
		Jmol._loadFileData(this, fileName, function(data){c._loadModel(data, params)});
	}
	
	Jmol._Applet.prototype._searchDatabase = function(query, database, script){
		this._showInfo(false);
		if (query.indexOf("?") >= 0) {
			Jmol._getInfoFromDatabase(this, database, query.split("?")[0]);
			return;
		}
		script || (script = Jmol._getScriptForDatabase(database));
		var dm = database + query;
		if (Jmol.db._DirectDatabaseCalls[database]) {
			this._loadFile(dm, script);
			return;
		}
		if (this._jmolIsSigned) {
			this._script("zap;set echo middle center;echo Retrieving data...;refresh;load \"" + dm + "\";" + script);
		} else {
			// need to do the postLoad here as well
			var c=this;
			Jmol._getRawDataFromServer(
				database,
				query,
				function(data){c._loadModel(data, ";" + script)}
			);
		}
	}
	

	// _Image -- an alternative to _Applet
	
	Jmol._Image = function(id, Info, caption, checkOnly){
		this._jmolType = "image";
		if (checkOnly)
			return this;
		Jmol._targetId = this._id = id;
		this._width = Info.width;
		this._height = Info.height;
		this._hasOptions = Info.addSelectionOptions;
		this._info = JSON.stringify(this);
		this._infoHeader = this._jmolType + ' "' + this._id + '"'
		Jmol._getWrapper(this, true);
		var s = '<img id="'+id+'_image" width="' + Info.width + '" height="' + Info.height + '" src=""/>';
		document.write(s);
		Jmol._getWrapper(this, false);
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, caption);
		this._canScript = function(script) {return (script.indexOf("#alt:LOAD") >= 0);};
		return this;
	}

	Jmol._setCommonMethods(Jmol._Image.prototype);

	Jmol._Image.prototype._script = function(script) {
	} // not implemented
	
	Jmol._Image.prototype._show = function(tf) {
		Jmol._getElement(this, "appletdiv").style.display = (tf ? "block" : "none");
	}
		
	Jmol._Image.prototype._loadFile = function(fileName, params){
		this._showInfo(false);
		this._thisJmolModel = "" + Math.random();
		params = (params ? params : "");
		var database = "";
		if (Jmol._isDatabaseCall(fileName)) {
			database = fileName.substring(0, 1); 
			fileName = Jmol._getDirectDatabaseCall(fileName, false);
		} else if (fileName.indexOf("://") < 0) {
			var ref = document.location.href
			var pt = ref.lastIndexOf("/");
			fileName = ref.substring(0, pt + 1) + fileName;
		}
		
		var src = Jmol._serverUrl 
				+ "?call=getImageForFileLoad"
				+ "&file=" + escape(fileName)
				+ "&width=" + this._width
				+ "&height=" + this._height
				+ "&params=" + encodeURIComponent(params + ";frank off;");
		Jmol._getElement(this, "image").src = src;
	}

	Jmol._Image.prototype._searchDatabase = function(query, database, script){
		if (query.indexOf("?") == query.length - 1)
			return;
		this._showInfo(false);
		script || (script = Jmol._getScriptForDatabase(database));
		var src = Jmol._serverUrl 
			+ "?call=getImageFromDatabase"
			+ "&database=" + database
			+ "&query=" + query
			+ "&width=" + this._width
			+ "&height=" + this._height
			+ "&script=" + encodeURIComponent(script + ";frank off;");
		Jmol._getElement(this, "image").src = src;
	}

})(Jmol);
