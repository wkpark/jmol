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
		jmolInitialize(Info.jmolJarPath, Info.jmolJarFile);
		Info.jmolReadyFunctionName && jmolSetParameter("appletReadyCallback", Info.jmolReadyFunctionName);
		var script = "";
		Jmol._getWrapper(this, true);
		jmolApplet(["100%","100%"], script, suffix);
		Jmol._getWrapper(this, false);  	
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, id, caption);
		return this;
	}

	Jmol._Applet.prototype._showInfo = function(tf) {
		if ((!this._isInfoVisible) == (!tf))
			return;
		this._isInfoVisible = tf;
		document.getElementById(this._id + "_infotablediv").style.display = (tf ? "block" : "none");
		document.getElementById(this._id + "_appletdiv").style.height = (tf ? 1 : this._height);
		document.getElementById(this._id + "_appletdiv").style.width = (tf ? 1 : this._width);
		this._show(!tf);
		document.getElementById(this._id + "_infoheaderdiv").innerHTML = this._infoHeader;
		document.getElementById(this._id + "_infodiv").innerHTML = this._info;
	}

	Jmol._Applet.prototype._search = function(query, script){
		this._showInfo(false);
		Jmol._setQueryTerm(this, query);
		query || (query = jQuery("#"+this._id+"_query").val());
		query && (query = query.replace(/\"/g, ""));
		var database;
		if (Jmol._isDatabaseCall(query)) {
			database = query.substring(0, 1);
			query = query.substring(1);
		} else {
			database = (this._hasOptions ? jQuery("#"+this._id+"_select").val() : "$");
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
		_jmolFindApplet(this._jmolId).script(script);
	}
	
	Jmol._Applet.prototype._show = function(tf) {
		_jmolFindApplet(this._jmolId).resize(tf ? this._width : 1, tf ? this._height : 1);
	}
	
	Jmol._Applet.prototype._script = function(script) {
		_jmolFindApplet(this._jmolId).script(script);
	}	
	
	Jmol._Applet.prototype._loadFile = function(fileName, params){
		this._showInfo(false);
		params || (params = "");
		this._thisJmolModel = "" + Math.random();
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
		if (Jmol.db._DirectDatabaseCalls[database]) {
			this._loadFile(database + query, script);
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
			Jmol._getGrabberOptions(this, id, caption);
		return this;
	}

	Jmol._setCommonMethods(Jmol._Image.prototype);

	Jmol._Image.prototype._script = function(script) {} // not implemented
	
	Jmol._Image.prototype._show = function(tf) {
		document.getElementById(this._id + "_appletdiv").style.display = (tf ? "block" : "none");
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
				+ "&params=" + escape(params) + ";frank off;";
			alert(src)
		document.getElementById(this._id + "_image").src = src;
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
			+ "&script=;frank off;" + script;
		document.getElementById(this._id + "_image").src = src;
	}

})(Jmol);
