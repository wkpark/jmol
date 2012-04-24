// JmolApplet.js -- Jmol._Applet and Jmol._Image

(function (Jmol) {

	Jmol._setCommonMethods = function(proto) {
		proto._showInfo = Jmol._Applet.prototype._showInfo;	
		proto._search = Jmol._Applet.prototype._search;
	}

	// _Applet -- the main, full-featured, object
	
	Jmol._Applet = function(id, Info, caption){
		this._jmolType = "Jmol._Applet" + (Info.jmolIsSigned ? " (signed)" : "");
		this._id = id;
		var suffix = id.replace(/^jmolApplet/,"");
		this._jmolId = "jmolApplet" + suffix;
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
		jmolInitialize(Info.jmolJarPath, Info.jmolJarFile);
		jmolSetParameter("appletReadyCallback", this._id + ".readyCallback");
		var script = "";
		Jmol._getWrapper(this, true);
		jmolApplet([Info.width,Info.height], script, suffix);
		Jmol._getWrapper(this, false);  	
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, caption);
		return this;
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
			document.getElementById(this._jmolId).style.width = w; 
			document.getElementById(this._jmolId).style.height = h; 
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
	
	Jmol._Image = function(id, Info, caption){
		this._jmolType = "image";
		this._id = id;
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
		return this;
	}

	Jmol._setCommonMethods(Jmol._Image.prototype);

	Jmol._Image.prototype._script = function(script) {} // not implemented
	
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
