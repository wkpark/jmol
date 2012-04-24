// JmolApi.js -- Jmol user functions

(function (Jmol) {

	Jmol.getVersion = function(){return _version};

	Jmol.getApplet = function(id, Info) {
	
	// note that the variable name the return is assigned to MUST match the first parameter in quotes
	// applet = Jmol.getApplet("applet", Info)

		id || (id = "jmolApplet0");
		Info || (Info = {
			width: 300,
			height: 300,
			debug: false,
			addSelectionOptions: true,
			serverURL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
			defaultModel: "",
			useChemDoodleOnly: false,
			useJmolOnly: true,
			useWebGlIfAvailable: true,
			useImageOnly: false,
			jmolIsSigned: false,
			jmolJarPath: ".",
			jmolJarFile: "JmolApplet0.jar",
			jmolReadyFunctionName: ""
		});	
	
		var applet = null;  // return value
	
		/* a general function that will switch to the desired rendering option
			involving Jmol or ChemDoodle.
		
		for example: 
		
			jmol_isReady = function(app,apptag,isReady) {
			if (!isReady) return;
			applet.setSearchTerm(Info.defaultModel);
		}		
	
	...
	
		[in body script tag]
		
			applet = Jmol.getApplet(Info)
	
		*/
	
		Info.serverURL && (Jmol._serverUrl = Info.serverURL);
		var model = Info.defaultModel;
		
		if (_jmol && !Info.useChemDoodleOnly && !Info.useImageOnly && navigator.javaEnabled()) {
		
			Info.jmolJarFile || (Info.jmolJarFile = (Info.jmolIsSigned ? "JmolAppletSigned0.jar" : "JmolApplet0.jar")); 
			Info.jmolJarPath || (Info.jmolJarPath = "."); 
			 
		// Jmol applet, signed or unsigned
		
			return new Jmol._Applet(id, Info, null);
		} 
		if (!Info.useJmolOnly && !Info.useImageOnly) 
			applet = Jmol._getCanvas(id, Info);	
		if (applet == null)
			applet = new Jmol._Image(id, Info, null);
		model && applet._search(model);
		return applet;
	}

	Jmol.script = function(applet, script) {
		applet._script(script);
	}

	Jmol.search = function(applet, query, script) {
		applet._search(query, script);
	}

	Jmol.showInfo = function(applet, tf) {
		applet._showInfo(tf);
	}
	
	Jmol.loadFile = function(applet, fileName, params){
		applet._loadFile(fileName, params);
	}

	Jmol.say = function(msg) {
		alert(msg);
	}
		

})(Jmol);
