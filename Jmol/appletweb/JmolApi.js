// JmolApi.js -- Jmol user functions

if(typeof(ChemDoodle)=="undefined") ChemDoodle = null;

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
	
		var applet;  // return value
	
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
		
			applet = new Jmol._Applet(id, Info, null);
			model = null;
			
		} else if (!Info.useJmolOnly && !Info.useImageOnly && ChemDoodle) {	
		
			// ChemDoodle: first try with WebGL unless that doesn't work or we have indicated NOWEBGL
			if (Info.useWebGlIfAvailable && ChemDoodle.featureDetection.supports_webgl()) {
				applet = new Jmol._Canvas3D(id, Info, null);
			} else {
				applet = {}
			}
			if (applet.gl) {
				//applet.specs.set3DRepresentation('Stick');
				applet.specs.set3DRepresentation('Ball and Stick');
				applet.specs.backgroundColor = 'black';
			} else {
				applet = new Jmol._Canvas(id, Info);
				applet.specs.bonds_useJMOLColors = true;
				applet.specs.bonds_width_2D = 3;
				applet.specs.atoms_display = false;
				applet.specs.backgroundColor = 'black';
				applet.specs.bonds_clearOverlaps_2D = true;
			}

		} else {
		
			applet = new Jmol._Image(id, Info, null);
			
		}
		model && applet._search(model);
		return applet;
	}

	Jmol.script = function(applet, script) {
		return applet._script(script);
	}

	Jmol.search = function(applet, query, script) {
		return applet._search(query, script);
	}

	Jmol.showInfo = function(applet, tf) {
		return applet._showInfo(tf);
	}
	
	Jmol.loadFile = function(applet, fileName, params){
		return applet._loadFile(fileName, params);
	}

	Jmol.say = function(msg) {
		alert(msg);
	}
		

})(Jmol);
