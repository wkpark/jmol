// JmolCD.js -- Jmol ChemDoodle extension   author: Bob Hanson, hansonr@stolaf.edu  4/16/2012

// last revision: 4/17/2012

// allows Jmol applets to be created on a page with more flexibility and extendability
// possibly using infrastructure of ChemDoodle.

// this package may be used with or without ChemDoodle
// if not using ChemDoodle, this package requires jQuery.js (or ChemDoodleWeb-libs.js, which conains jQuery) 
// if using ChemDoodle, this package requires ChemDoodleWeb-libs.js and ChemDoodleWeb.js prior to JmolCD.js

// allows Jmol-like objects to be displayed on Java-challenged (iPad/iPhone)
// or applet-challenged (Android/iPhone) platforms, with automatic switching to 
// whatever is appropriate. You can specify "ChemDoodle-only", "Jmol-only", "Image-only"
// or some combination of those -- and of course, you are free to rewrite the logic below! 

// allows ChemDoodle-like 3D and 3D-faked 2D canvases that can load files via a privately hosted 
// server that delivers raw data files rather than specialized JSON mol data.
// access to iChemLabs server is not required for simple file-reading operations and 
// database access. Database and image services are provided by a server-side PHP program
// running JmolData.jar with flags -iR. 
// In this case, the NCI and RCSB databases are accessed via a St. Olaf College server, 
// but for your installation, you should consider putting JmolData.jar and jmolcd.php 
// on your own server. Nothing more than these two files is needed on the server.

if(typeof(ChemDoodle)=="undefined") ChemDoodle = null;

Jmol = (function() {
	var	version = 'Jmol 12.3.22' + (ChemDoodle ? "; ChemDoodle " + ChemDoodle.getVersion(): "");
	return {
		INFO: {userAgent:navigator.userAgent, version: version},
		SERVER_URL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
		nciLoadScript: ";n = ({molecule=1}.length < {molecule=2}.length ? 2 : 1); select molecule=n;display selected;center selected;",
		fileLoadScript: ";if (_loadScript = '' && defaultLoadScript == '' && _filetype == 'Pdb') { select protein or nucleic;cartoons Only;color structure; select * };",
		asynchronous: !0,
		getVersion: function(){return version},
	};
})();

(function (Jmol) {

	Jmol.getApplet = function(Info) {
	
		var applet;  // return value
	
		/* a general function that will switch to the desired rendering option
			involving Jmol or ChemDoodle.
		
		for example: 
		
			jmol_isReady = function(app,isReady) {
			if (!isReady) return;
			applet.setSearchTerm(Info.defaultModel);
		}		
	
		var Info = {
			id: "applet1",
			width: 300,
			height: 300,
			debug: true
			addSelectionOptions: true,
			serverURL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
			defaultModel: "morphine",
			useChemDoodleOnly: false,
			useJmolOnly: false,
			useWebGlIfAvailable: true,
			useImageOnly: true,
			jmolIsSigned: useSigned,
			jmolJarPath: ".",
			jmolJarFile: (useSigned ? "JmolAppletSigned.jar" : "JmolApplet.jar"),
			jmolReadyFunctionName: "jmol_isReady",
		}
	
	...
	
		[in body script tag]
		
			applet = Jmol.getApplet(Info)
	
		*/
	
		Info.serverURL && (Jmol.SERVER_URL = Info.serverURL);
		
		if (_jmol && !Info.useChemDoodleOnly && !Info.useImageOnly && navigator.javaEnabled()) {
		
			Info.jmolJarFile || (Info.jmolJarFile = (Info.jmolIsSigned ? "JmolAppletSigned0.jar" : "JmolApplet0.jar")); 
			Info.jmolJarPath || (Info.jmolJarPath = "."); 
			 
		// Jmol applet, signed or unsigned
		
			applet = new Jmol.Applet(Info.id, Info.width, Info.height, Info.jmolJarPath, 
				Info.jmolJarFile, Info.jmolIsSigned, Info.jmolReadyFunctionName,  
				(Info.debug ? "<br />(Java found: using Jmol " + (Info.jmolIsSigned ? "signed, no server)" : "unsigned+server)") : null),
				Info.addSelectionOptions);
				
		} else if (!Info.useJmolOnly && !Info.useImageOnly && ChemDoodle) {	
		
			// ChemDoodle: first try with WebGL unless that doesn't work or we have indicated NOWEBGL
			if (Info.useWebGlIfAvailable && ChemDoodle.featureDetection.supports_webgl()) {
				applet = new Jmol.Canvas3D(Info.id, Info.width, Info.height,
					(Info.debug ? "<br />(WebGL found: Jmol.Canvas3D)" : null), Info.addSelectionOptions);
			} else {
				applet = {}
			}
			if (applet.gl) {
				//applet.specs.set3DRepresentation('Stick');
				applet.specs.set3DRepresentation('Ball and Stick');
				applet.specs.backgroundColor = 'black';
			} else {
				applet = new Jmol.Canvas(Info.id+"_2D", Info.width, Info.height,
					(Info.debug ? "<br />(No WebGL: Jmol.Canvas)" : null),
					Info.addSelectionOptions);
					applet.specs.bonds_useJMOLColors = true;
				applet.specs.bonds_width_2D = 3;
				applet.specs.atoms_display = false;
				applet.specs.backgroundColor = 'black';
				applet.specs.bonds_clearOverlaps_2D = true;
			}
			Info.defaultModel && applet.setSearchTerm(Info.defaultModel);	
	
		} else {
		
			// just load the image
			
			applet = new Jmol.Image(Info.id, Info.width, Info.height,
				(Info.debug ? "<br />(Just creating an image)" : null),
				Info.addSelectionOptions);
			Info.defaultModel && applet.setSearchTerm(Info.defaultModel);    
		}
		return applet;
	}

	Jmol.getScript = function(database, model) {
		return (database == "$" ? Jmol.nciLoadScript : Jmol.fileLoadScript);
	}
	
	Jmol.getRawDataFromDatabase = function(database,query,fSuccess,fError){
		var c=this;
		this.contactServer(
			"getRawDataFromDatabase",
			{database:database,dimension:3,query:query,script:Jmol.getScript(database)},
			fSuccess, fError
		)
	}
	
	Jmol.loadSuccess = function(a, fSuccess) {
		Jmol.inRelay=!1;
		fSuccess(a);
	}

	Jmol.loadError = function(fError){
		Jmol.inRelay=!1;
		alert("Could not connect to server. (Note that some browsers cannot read local files this way.)");	
		null!=fError&&fError()
	}
	
	Jmol.loadFileData = function(fileName, fSuccess, fError){
		Jmol.inRelay?alert("Already connecting to the server - please wait for the first request to finish."):
		(Jmol.inRelay=!0,
			jQuery.ajax({
				dataType: "text",
				type: "POST",
				url: fileName,
				success: function(a) {Jmol.loadSuccess(a, fSuccess)},
				error: function() {Jmol.loadError(fError)},
				async: Jmol.asynchronous
			})
		)
	}
	
	Jmol.contactServer = function(cmd,content,fSuccess,fError){
		Jmol.inRelay?alert("Already connecting to the server - please wait for the previous request to finish."):
		(Jmol.inRelay=!0,
			jQuery.ajax({
				dataType: "text",
				type: "POST",
				data: JSON.stringify({
					call: cmd,
					content: content,
					info: Jmol.INFO
				}),
				url: this.SERVER_URL,
				success: function(a) {Jmol.loadSuccess(a, fSuccess)},
				error:function() { Jmol.loadError(fError) },
				async:Jmol.asynchronous
			})
		)
	}
	
	// Applet -- an alternative to _Canvas3D
	//                -- loads the Jmol applet instead of ChemDoodle
	
	Jmol.Applet = function(id,c,h, jmolDirectory, appJar, jmolIsSigned, readyFunctionName, caption, addOptions){
		this.id = id;
		this.jmolIsSigned = jmolIsSigned;
		this.dataMultiplier=1;
		jmolInitialize(jmolDirectory, appJar);
		readyFunctionName && jmolSetParameter("appletReadyCallback", readyFunctionName);
		var script = "";
		jmolApplet([c,h],script, id);  	
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption);
			return this;
	}
	
	Jmol.Applet.prototype.setSearchTerm = function(b){
		Jmol.searchQuery(this, b);
	}
	
	Jmol.Applet.prototype.loadMolecule = function(mol) {
		_jmolFindApplet("jmolApplet" + this.id).script('load DATA "model"\n' + mol + '\nEND "model" ' + this.loadParams
				+ Jmol.fileLoadScript);
	}
	
	Jmol.Applet.prototype.script = function(script) {
		_jmolFindApplet("jmolApplet" + this.id).script(script);
	}	
	
	Jmol.Applet.prototype.loadFile = function(fileName, params){
		this.loadParams = (params ? params : "");
		this.thisJmolModel = "" + Math.random();
		var c = this;
		Jmol.loadFileData(fileName, function(data){c.loadMolecule(data)});
	}
	
	Jmol.Applet.prototype.search = function(model){
		model || (model = jQuery("#"+this.id+"_query").val().replace(/\"/g, ""));
		database = jQuery("#"+this.id+"_select").val();
		if (model.indexOf("=") == 0 || model.indexOf("$") == 0) {
			database = model.substring(0, 1);
			model = model.substring(1);
		}
		var dm = database + model;
		if (!model || model && dm == this.thisJmolModel)
			return;
		this.thisJmolModel = dm;
		if (database == "$")
			this.jmolFileType = "MOL";
		else if (database == "=")
			this.jmolFileType = "PDB";
		this.searchDatabase(model, database);
	};
			
	Jmol.Applet.prototype.searchDatabase = function(model, database){
		if (this.jmolIsSigned) {
			var postLoad = (database == "$" ? Jmol.nciPostLoadScript : "");				
			this.script("load \"" + dm + "\"" + postLoad);
		} else {
			// need to do the postLoad here as well
			var c=this;
			Jmol.getRawDataFromDatabase(
				database,
				model,
				function(data){c.loadMolecule(data)}
			);
		}
	}
	
	// Image -- another alternative to _Canvas
	Jmol.Image = function(id,width,height, caption, addOptions){
		this.id = id;
		this.imgWidth = width;
		this.imgHeight = height;
		var img = '<img id="'+id+'_image" width="' + width + '" height="' + height + '" src=""/>';
		document.write(img);
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption);
		return this;
	}
	
	Jmol.Image.prototype.search = Jmol.Applet.prototype.search;
	Jmol.Image.prototype.setSearchTerm = Jmol.Applet.prototype.setSearchTerm;
	
	Jmol.Image.prototype.loadFile = function(fileName, params){
		params = (params ? params : "");
		if (fileName.indexOf("://") < 0 && fileName.indexOf("=") != 0 && fileName.indexOf("$") != 0) {
			var ref = document.location.href
			var pt = ref.lastIndexOf("/");
			fileName = ref.substring(0, pt + 1) + fileName;
		}
		var src = Jmol.Jmol.SERVER_URL 
				+ "?call=getImageForFileLoad"
				+ "&file=" + fileName
				+ "&width=" + this.imgWidth
				+ "&height=" + this.imgHeight
				+ "&params=" + escape(params);
			+ "&script=" + Jmol.getScriptForModel(database, model);
		document.getElementById(this.id + "_image").src = src;
	}

	Jmol.Image.prototype.searchDatabase = function(model, database){
		var src = Jmol.SERVER_URL 
			+ "?call=getImageFromDatabase"
			+ "&query=" + model;
			+ "&width=" + this.imgWidth
			+ "&height=" + this.imgHeight
			+ "&database=" + database
			+ "&script=" + Jmol.getScriptForModel(database, model);
		document.getElementById(this.id + "_image").src = src;
	}

	// user-adjustable 
		
	Jmol.getGrabberOptions = function(canvas, label, note) {
	
		// for now, only NCI
		// Bob Hanson, hansonr@stolaf.edu 4/14/2012
		// feel free to adjust this look to anything you want
		
		c=[];
		c.push('<br><input type="text" id="');
		c.push(label);
		c.push('_query" size="32" value="" />');
		c.push("<br><nobr>");
		c.push('<select id="');
		c.push(label);
		c.push('_select">');
		c.push('<option value="$" selected>NCI(small molecules)</option>');
		c.push('<option value="=">RCSB(macromolecules)</option>');
		c.push("</select>");
		c.push('<button id="');
		c.push(label);
		c.push('_submit">Search</button>');
		c.push("</nobr>");
		note && c.push(note);
		document.writeln(c.join(""));
		jQuery("#"+label+"_submit").click(
			function(){
				canvas.search()
			}
		);
		jQuery("#"+label+"_query").keypress(
			function(a){
				13==a.which&&canvas.search()
			}
		);
		canvas.repaint && (
			canvas.emptyMessage="Enter search term below",
			canvas.repaint()
		);
	}
	
	// Jmol core functionality
		
	Jmol.searchQuery = function(applet, query) {
			jQuery("#"+applet.id+"_query").val(query);
			applet.search(query.replace(/\"/g, ""));
	}
	
	if (!ChemDoodle) 
		return;

	// Note: all of the rest of this can be removed if you have no interest in using ChemDoodle
 		
	// changes: MolGrabberCanvas, MolGrabberCanvas3D
	//   -- properly scales data using dataMultiplier 1 (3D canvas) or 20 (2d canvas)
	//   -- generalized selection/input options
	//   -- adds caption
	// new: Applet, Image
	
	
	Jmol.Canvas3D = function(b,c,h,caption,addOptions){
		b&&this.create(b,c,h);
		this.dataMultiplier=1;
		if (addOptions)
			Jmol.getGrabberOptions(this, b,caption); // just for this test page
		return this
	}
	
	// MolGrabberCanvas changes add a dataMultiplier, subclasses TransformCanvas, modifies display options
	
	Jmol.Canvas = function(b,c,h,caption, addOptions){
		b&&this.create(b,c,h);
		this.lastPoint=null;
		this.rotate3D=true;
		this.rotationMultMod=1.3;
		this.lastPinchScale=1;
		this.lastGestureRotate=0;
		this.dataMultiplier=20;
		if (addOptions)
			Jmol.getGrabberOptions(this,b,caption);
		return this;
	}

  var cdSetPrototype = function(proto) {
  
		proto.setSearchTerm = function(b){
			Jmol.searchQuery(this, b);
		};

		proto.search = Jmol.Applet.prototype.search;
	
		proto.searchDatabase = function(model, database){
			this.emptyMessage="Searching...";
			this.molecule=null;
			this.repaint();
			var c = this;
			Jmol.getRawDataFromDatabase(
				database,
				model,
				function(data){Jmol.cdProcessFileData(c, data)}
			);
		};
		
		proto.loadFile = function(fileName, params){
			Jmol.cdLoadFile(this, fileName, params);
		};

		return proto;		
	}

	Jmol.Canvas3D.prototype = cdSetPrototype(new ChemDoodle._Canvas3D);
	Jmol.Canvas.prototype = cdSetPrototype(new ChemDoodle.TransformCanvas);
	
	Jmol.cdLoadFile = function(cdcanvas, fileName){
		cdcanvas.emptyMessage="Searching...";
		cdcanvas.molecule=null;
		cdcanvas.repaint();
		cdcanvas.jmolFileType = Jmol.cdGetFileType(fileName);
		Jmol.loadFileData(
			fileName,
			function(data){Jmol.cdProcessFileData(cdcanvas, data)}
		);
	}
		
	Jmol.cdGetFileType = function(name) {
		// just the extension, which must be PDB, XYZ..., CIF, or MOL
		name = name.split('.').pop().toUpperCase();
		return name.substring(0, Math.min(name.length, 3));
	}
	
	Jmol.cdProcessFileData = function(cdcanvas, data) {
		var factor = cdcanvas.dataMultiplier;
		data = Jmol.cdCleanFileData(data);
		var molecule;
		switch(cdcanvas.jmolFileType) {
		case "PDB":
		case "PQR":
			molecule = ChemDoodle.readPDB(data, 1);
			// note: default factor for readPDB is 1
			break;
		case "XYZ":
			molecule = ChemDoodle.readXYZ(data, 1);
			// 1 here is just in case
			break;
		case "CIF":
			molecule = ChemDoodle.readCIF(data, 1, 1, 1, 1);
			// last 1 here is just in case
			break;
		case "MOL":
			molecule = ChemDoodle.readMOL(data, 1);
			// note: default factor for readMOL is 20
			break;
		default:
			return;
		}		
		cdcanvas.loadMolecule(Jmol.cdScaleMolecule(molecule, factor));
	}	
	
	Jmol.cdCleanFileData = function(data) {
		if (data.indexOf("\r") >= 0 && data.indexOf("\n") >= 0) {
			return data.replace(/\r\n/g,"\n");
		}
		if (data.indexOf("\r") >= 0) {
			return data.replace(/\r/g,"\n");
		}
		return data;
	}
		
	Jmol.cdScaleMolecule = function(molecule, multiplier) {
		if (multiplier != 0 && multiplier != 1) {
			var atoms = molecule.atoms;
			for(var i = atoms.length; --i >= 0;){
				var a = atoms[i];
				a.x*=multiplier;
				a.y*=-multiplier;
				a.z*=multiplier;
			}
		}
		return molecule;
	}

})(Jmol);
