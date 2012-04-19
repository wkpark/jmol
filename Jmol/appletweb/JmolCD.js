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
	var	version = 'Jmol 12.3.23' + (ChemDoodle ? "; ChemDoodle " + ChemDoodle.getVersion(): "");
	return {
		INFO: {userAgent:navigator.userAgent, version: version},
		SERVER_URL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
		nciLoadScript: ";n = ({molecule=1}.length < {molecule=2}.length ? 2 : 1); select molecule=n;display selected;center selected;",
		fileLoadScript: ";if (_loadScript = '' && defaultLoadScript == '' && _filetype == 'Pdb') { select protein or nucleic;cartoons Only;color structure; select * };",
		asynchronous: !0,
		getVersion: function(){return version},
	}
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
				applet = new Jmol.Canvas(Info.id, Info.width, Info.height,
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

	// Jmol core functionality
		
	Jmol.getWrapper = function(applet, isHeader) {
		var s = (isHeader ?
			"<div id=ID_appletinfotablediv style=width:Wpx;height:Hpx>\
			<table><tr><td></td><td><div id=ID_infotablediv style=width:Wpx;height:Hpx;display:none>\
			<table><tr height=20><td style=background:yellow><span id=ID_infoheaderdiv></span></td>\
			<td width=10><a href=javascript:Jmol.showInfo(ID,false)>[x]</a></td></tr><tr><td colspan=2>\
			<div id=ID_infodiv style=overflow:scroll;width:Wpx;height:" + (applet.height - 15) + "px></div></td></tr></table></div></td></tr>\
			<tr><td><div id=ID_appletdiv style=width:Wpx;height:Hpx>"
			:"</div></td><td></td></tr></table></div>"
		).replace(/H/g, applet.height).replace(/W/g, applet.width).replace(/ID/g, applet.id);
		document.write(s);
	}

	Jmol.showInfo = function(applet, tf) {
		document.getElementById(applet.id + "_infotablediv").style.display = (tf ? "block" : "none");
		document.getElementById(applet.id + "_appletdiv").style.height = (tf ? 1 : applet.height);
		document.getElementById(applet.id + "_appletdiv").style.width = (tf ? 1 : applet.width);
		applet.show(!tf);
		document.getElementById(applet.id + "_infoheaderdiv").innerHTML = applet.infoHeader;
		document.getElementById(applet.id + "_infodiv").innerHTML = applet.info;
	}

	Jmol.searchQuery = function(applet, query) {
			if (applet.hasOptions)
				jQuery("#"+applet.id+"_query").val(query);
			applet.search(query.replace(/\"/g, ""));
	}
	
	Jmol.getScriptForModel = function(database, model) {
		return (database == "$" ? Jmol.nciLoadScript : Jmol.fileLoadScript);
	}
	
	Jmol.getRawDataFromDatabase = function(database,query,fSuccess,fError){
		var c=this;
		this.contactServer(
			"getRawDataFromDatabase",
			{database:database,dimension:3,query:query,script:Jmol.getScriptForModel(database)},
			fSuccess, fError
		)
	}
	
	Jmol.getInfoFromDatabase = function(applet, query, database){
		var c=this;
		this.contactServer(
			"getInfoFromDatabase",
			{database:database,query:query},
			function(data) { Jmol.setInfo(database, applet, data) }
		)
	}
	
	/*
      <dataset><record><structureId>1BLU</structureId><structureTitle>STRUCTURE OF THE 2[4FE-4S] FERREDOXIN FROM CHROMATIUM VINOSUM</structureTitle></record><record><structureId>3EUN</structureId><structureTitle>Crystal structure of the 2[4Fe-4S] C57A ferredoxin variant from allochromatium vinosum</structureTitle></record></dataset>
      
	*/
	Jmol.setInfo = function(database, applet, data) {
		var info = [];
		var header = "";
		if (data.indexOf("ERROR") == 0)
			header = data;
		else
			switch (database) {
			case "=":
				var S = data.split("<structureId>");
				var info = ["<table>"];
				for (var i = 1; i < S.length; i++) {
					info.push("<tr><td valign=top><a href=\"javascript:Jmol.showInfo(" + applet.id + ",false);" + applet.id + ".setSearchTerm('=" + S[i].substring(0, 4) + "')\">" + S[i].substring(0, 4) + "</a></td>");
					info.push("<td>" + S[i].split("Title>")[1].split("</")[0] + "</td></tr>");
				}
				info.push("</table>");
				header = (S.length - 1) + " matches";
				break;			
			case "$": // NCI
			case ":": // pubChem
			break;
			default:
				return;
		}
		applet.infoHeader = header;
		applet.info = info.join("");
		Jmol.showInfo(applet, true);
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
	
	Jmol.Applet = function(id, width, height, jmolDirectory, appJar, jmolIsSigned, readyFunctionName, caption, addOptions){
		this.jmolType = "Jmol.Applet" + (jmolIsSigned ? " (signed)" : "");
		this.id = id;
		this.width = width;
		this.height = height;
		this.jmolIsSigned = jmolIsSigned;
		this.dataMultiplier=1;
		this.hasOptions = addOptions;
		this.info = JSON.stringify(this);
		this.infoHeader = this.jmolType + ' "' + this.id + '"'
		jmolInitialize(jmolDirectory, appJar);
		readyFunctionName && jmolSetParameter("appletReadyCallback", readyFunctionName);
		var script = "";
		
		Jmol.getWrapper(this, true);
		jmolApplet(["100%","100%"],script, id);
		Jmol.getWrapper(this, false);  	
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption);
		return this;
	}
	
	Jmol.Applet.prototype.setSearchTerm = function(b){
		this.script("zap;set echo middle center;echo Retrieving data...");
		Jmol.searchQuery(this, b);
	}
	
	Jmol.Applet.prototype.loadMolecule = function(mol) {
		_jmolFindApplet("jmolApplet" + this.id).script('load DATA "model"\n' + mol + '\nEND "model" ' + this.loadParams
				+ Jmol.fileLoadScript);
	}
	
	Jmol.Applet.prototype.show = function(tf) {
		_jmolFindApplet("jmolApplet" + this.id).resize(tf ? this.width : 1, tf ? this.height : 1);
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
		database = (this.hasOptions ? jQuery("#"+this.id+"_select").val() : "$");
		if (model.indexOf("=") == 0 || model.indexOf("$") == 0 || model.indexOf(":") == 0) {
			database = model.substring(0, 1);
			model = model.substring(1);
		}
		var dm = database + model;
		if (!model || dm.indexOf("?") < 0 && dm == this.thisJmolModel)
			return;
		this.thisJmolModel = dm;
		if (database == "$" || database == ":")
			this.jmolFileType = "MOL";
		else if (database == "=")
			this.jmolFileType = "PDB";
		this.searchDatabase(model, database);
	}
			
	Jmol.Applet.prototype.searchDatabase = function(model, database){
		if (model.indexOf("?") >= 0) {
		  Jmol.getInfoFromDatabase(this, model.split("?")[0], database);
		  return;
		}
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
		this.jmolType = "image";
		this.id = id;
		this.width = width;
		this.height = height;
		this.hasOptions = addOptions;
		this.info = JSON.stringify(this);
		this.infoHeader = this.jmolType + ' "' + this.id + '"'
		var img = '<img id="'+id+'_image" width="' + width + '" height="' + height + '" src=""/>';
		Jmol.getWrapper(this, true);
		document.write(img);
		Jmol.getWrapper(this, false);
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption);
		return this;
	}
	
	Jmol.Image.prototype.show = function(tf) {
		document.getElementById(this.id + "_appletdiv").style.display = (tf ? "block" : "none");
	}
	
	Jmol.Image.prototype.search = Jmol.Applet.prototype.search;
	Jmol.Image.prototype.setSearchTerm = Jmol.Applet.prototype.setSearchTerm;
	
	Jmol.Image.prototype.loadFile = function(fileName, params){
		this.thisJmolModel = "" + Math.random();
		params = (params ? params : "");
		if (fileName.indexOf("://") < 0 && fileName.indexOf("=") != 0 && fileName.indexOf("$") != 0 && fileName.indexOf(":") != 0) {
			var ref = document.location.href
			var pt = ref.lastIndexOf("/");
			fileName = ref.substring(0, pt + 1) + fileName;
		}
		var src = Jmol.Jmol.SERVER_URL 
				+ "?call=getImageForFileLoad"
				+ "&file=" + fileName
				+ "&width=" + this.width
				+ "&height=" + this.height
				+ "&params=" + escape(params);
			+ "&script=frank off;" + Jmol.getScriptForModel(database, model);
		Jmol.getWrapper(this, true);
		document.getElementById(this.id + "_image").src = src;
		Jmol.getWrapper(this, false);
	}

	Jmol.Image.prototype.searchDatabase = function(model, database){
		var src = Jmol.SERVER_URL 
			+ "?call=getImageFromDatabase"
			+ "&query=" + model;
			+ "&width=" + this.width
			+ "&height=" + this.height
			+ "&database=" + database
			+ "&script=frank off;" + Jmol.getScriptForModel(database, model);
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
		c.push('<option value=":">PubChem(small molecules)</option>');
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
	
	if (!ChemDoodle) 
		return;

	// Note: all of the rest of this can be removed if you have no interest in using ChemDoodle
 		
	// changes: MolGrabberCanvas, MolGrabberCanvas3D
	//   -- properly scales data using dataMultiplier 1 (3D canvas) or 20 (2d canvas)
	//   -- generalized selection/input options
	//   -- adds caption
	// new: Applet, Image
	
	
	Jmol.Canvas3D = function(id,width,height,caption,addOptions){
		this.jmolType = "Jmol.Canvas3D";
		this.id = id;
		this.width = width;
		this.height = height;
		this.hasOptions = addOptions;
		this.dataMultiplier=1;
		this.info = JSON.stringify(this);
		this.infoHeader = this.jmolType + ' "' + this.id + '"'
		Jmol.getWrapper(this, true);
		this.create(id,width,height);
		Jmol.getWrapper(this, false);
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption); // just for this test page
		return this
	}
	
	// MolGrabberCanvas changes add a dataMultiplier, subclasses TransformCanvas, modifies display options
	
	Jmol.Canvas = function(id, width, height, caption, addOptions){
		this.jmolType = "Jmol.Canvas";
		this.id = id;
		this.width = width;
		this.height = height;
		this.hasOptions = addOptions;
		this.info = JSON.stringify(this);
		this.infoHeader = this.jmolType + ' "' + this.id + '"'
		Jmol.getWrapper(this, true);
		this.create(id,width,height);
		Jmol.getWrapper(this, false);
		this.lastPoint=null;
		this.rotate3D=true;
		this.rotationMultMod=1.3;
		this.lastPinchScale=1;
		this.lastGestureRotate=0;
		this.dataMultiplier=20;
		if (addOptions)
			Jmol.getGrabberOptions(this, id, caption);
		return this;
	}

  var cdSetPrototype = function(proto) {
  
		proto.show = function(tf) {
			document.getElementById(this.id + "_appletdiv").style.display = (tf ? "block" : "none");
		}
	
		proto.setSearchTerm = function(b){
			Jmol.searchQuery(this, b);
		}

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
		}
		
		proto.loadFile = function(fileName, params){
			this.thisJmolModel = "" + Math.random();
			this.emptyMessage="Searching...";
			this.molecule=null;
			this.repaint();
			this.jmolFileType = Jmol.cdGetFileType(fileName);
			var cdcanvas = this;
			Jmol.loadFileData(
				fileName,
				function(data){Jmol.cdProcessFileData(cdcanvas, data)}
			);
		}

		return proto;		
	}

	Jmol.Canvas3D.prototype = cdSetPrototype(new ChemDoodle._Canvas3D);
	Jmol.Canvas.prototype = cdSetPrototype(new ChemDoodle.TransformCanvas);
			
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
