// JmolCD.js -- Jmol ChemDoodle extension   author: Bob Hanson, hansonr@stolaf.edu  4/16/2012

// requires ChemDoodleWeb.js and ChemDoodleWeb-libs.js
// prior to JmolCD.js

// allows Jmol applets to be created on a page with more flexibility and extendability
// using much of the infrastructure of ChemDoodle.

// allows Jmol-like objects to be displayed on Java-challenged (iPad/iPhone)
// or applet-challenged (Android/iPhone) platforms, with automatic switching to 
// whatever is appropriate. You can specify "ChemDoodle-only", "Jmol-only", "Image-only"
// or some combination of those -- and of course, you are free to rewrite the logic below! 

// allows ChemDoodle-like 2D and 3D canvases that can load files via a privately hosted 
// server that delivers raw data files rather than specialized JSON mol data.
// access to iChemLabs server is not required for simple file-reading operations and 
// database access. Database and image services are provided by a server-side PHP program
// running JmolData.jar with flags -iR. 

// In this case, the NCI and RCSB databases are accessed via a St. Olaf College server, 
// but for your installation, you should consider putting JmolData.jar and jmolcd.php 
// on your own server. Nothing more than these two files is needed on the server.

Jmol = (function() {
	return {
		INFO: ChemDoodle.iChemLabs.INFO,
		SERVER_URL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
		nciPostLoadScript: ";n = ({molecule=1}.length < {molecule=2}.length ? 2 : 1); select molecule=n;display selected;center selected;",
		asynchronous: !0,
		VERSION: 'Jmol 12.3.22/ChemDoodle 4.6.2',
		getVersion: function(){return this.VERSION},
	};
})();

Jmol.getMolGrabber = function(Info) {

	var molgrabber;  // return value

	/* a general function that will switch to the desired rendering option
		involving Jmol or ChemDoodle.
	
	for example: 
	
		jmol_isReady = function(app,isReady) {
		if (!isReady) return;
		molgrabber.setSearchTerm(Info.defaultModel);
	}		

	var Info = {
		id: "molgrabber1",
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
	
		molgrabber = Jmol.getMolGrabber(Info)

	*/

	Info.serverURL && (Jmol.SERVER_URL = Info.serverURL);
	
	if (_jmol && !Info.useChemDoodleOnly && !Info.useImageOnly && navigator.javaEnabled()) {
	
		Info.jmolJarFile || (Info.jmolJarFile = (Info.jmolIsSigned ? "JmolAppletSigned0.jar" : "JmolApplet0.jar")); 
		Info.jmolJarPath || (Info.jmolJarPath = "."); 
		 
	// Jmol applet, signed or unsigned
	
		molgrabber = new Jmol.MolGrabberJmol(Info.id, Info.width, Info.height, Info.jmolJarPath, 
			Info.jmolJarFile, Info.jmolIsSigned, Info.jmolReadyFunctionName,  
			(Info.debug ? "<br />(Java found: using Jmol " + (Info.jmolIsSigned ? "signed, no server)" : "unsigned+server)") : null),
			Info.addSelectionOptions);
			
	} else if (!Info.useJmolOnly && !Info.useImageOnly) {	
	
		// ChemDoodle: first try with WebGL unless that doesn't work or we have indicated NOWEBGL
		if (Info.useWebGlIfAvailable && ChemDoodle.featureDetection.supports_webgl()) {
			molgrabber = new Jmol.MolGrabberCanvas3D(Info.id, Info.width, Info.height,
				(Info.debug ? "<br />(WebGL found: Jmol.MolGrabberCanvas3D)" : null), Info.addSelectionOptions);
		} else {
			molgrabber = {}
		}
		if (molgrabber.gl) {
			//molgrabber.specs.set3DRepresentation('Stick');
			molgrabber.specs.set3DRepresentation('Ball and Stick');
			molgrabber.specs.backgroundColor = 'black';
		} else {
			molgrabber = new Jmol.MolGrabberCanvas(Info.id+"_2D", Info.width, Info.height,
				(Info.debug ? "<br />(No WebGL: Jmol.MolGrabberCanvas)" : null),
				Info.addSelectionOptions);
				molgrabber.specs.bonds_useJMOLColors = true;
			molgrabber.specs.bonds_width_2D = 3;
			molgrabber.specs.atoms_display = false;
			molgrabber.specs.backgroundColor = 'black';
			molgrabber.specs.bonds_clearOverlaps_2D = true;
		}
		Info.defaultModel && molgrabber.setSearchTerm(Info.defaultModel);	

	} else {
	
		// just load the image
		
		molgrabber = new Jmol.MolGrabberImage(Info.id, Info.width, Info.height,
			(Info.debug ? "<br />(Just creating an image)" : null),
			Info.addSelectionOptions);
		Info.defaultModel && molgrabber.setSearchTerm(Info.defaultModel);    
	}
	return molgrabber;
}

Jmol.getRawDataFromDatabase = function(database,query,fsuccess,ferror){
	var script = (database == "$" ? Jmol.nciPostLoadScript : "");
	this.contactServer(
		"getRawDataFromDatabase",
		{database:database,dimension:3,query:query,postLoadScript:script},
		function(data){
			fsuccess(data)
		},
		ferror,
		!0
	)
}

Jmol.loadFileData = function(fileName, fReturn){
	this.inRelay?alert("Already connecting to the server, please wait for the first request to finish."):
	(Jmol.inRelay=!0,
		jQuery.ajax({
			dataType:"text",
			type:"POST",
			url:fileName,
			success:function(a){
				Jmol.inRelay=!1;
				fReturn(a);
			},
			error:function(){Jmol.inRelay=!1;alert("Server connectivity failed. Please try again or update this browser to the latest version. (XHR2 not supported)");	null!=c&&c()},
			async:Jmol.asynchronous
		})
	)
}

Jmol.contactServer = function(cmd,content,fSuccess,fError,isRaw){
	
	// allows for reading of raw data rather than JSON
	
	this.inRelay?alert("Already connecting to the server, please wait for the first request to finish."):
	(Jmol.inRelay=!0,
		jQuery.ajax({
			dataType:"text",
			type:"POST",
			data:JSON.stringify({
				call:cmd,
				content:content,
				info:Jmol.INFO
			}),
			url:this.SERVER_URL,
			success:function(a){
				Jmol.inRelay=!1;
				if(isRaw) {
					fSuccess(a);
					return;
				}
				o=JSON.parse(a);
				o.message&&alert(o.message);
//o.content = '{"mol":{"scale":[20,-20,20],"a":[{"x":78.474,"y":18.444,"z":3.67},{"x":64.958,"y":64.212006,"z":7.6419997},{"x":103.462,"y":26.569998,"z":-4.9280005},{"x":89.946,"y":72.338,"z":-0.956},{"x":109.19601,"y":53.517998,"z":-7.2460003},{"x":31.98,"y":28.406,"z":19.326},{"x":-84.273994,"y":-16.376001,"z":51.858},{"x":-115.051994,"y":-3.2080002,"z":14.070001},{"l":"H","x":-18.426,"y":13.716001,"z":17.006},{"x":59.22,"y":37.264,"z":9.952001},{"l":"O","x":-99.78,"y":-50.052002,"z":-24.666002},{"l":"O","x":-25.512001,"y":-33.281998,"z":-38.72},{"l":"O","x":22.08,"y":29.476002,"z":-26.811998},{"l":"O","x":-92.08,"y":-69.404,"z":14.316},{"x":-88.61001,"y":-49.4,"z":-3.246},{"x":13.762001,"y":25.081999,"z":-4.454},{"x":-70.782,"y":-26.126001,"z":3.75},{"x":-29.484001,"y":14.0,"z":-23.994},{"x":-37.694,"y":-14.437999,"z":-29.506},{"l":"H","x":-103.7,"y":-83.898,"z":8.932},{"l":"N","x":-11.774,"y":17.23,"z":-0.8759999},{"x":-59.156,"y":16.960001,"z":-17.647999},{"x":-85.96,"y":-6.886,"z":22.816},{"l":"S","x":-66.38,"y":23.898,"z":17.618},{"l":"N","x":-63.18,"y":-11.977999,"z":-20.772},{"l":"H","x":-52.845997,"y":-33.494,"z":13.710001},{"l":"H","x":-70.414,"y":29.387997,"z":-31.578003},{"l":"H","x":-93.138,"y":-36.222,"z":53.544},{"l":"H","x":-95.102005,"y":-2.47,"z":64.688},{"l":"H","x":-63.384,"y":-17.2,"z":58.034},{"l":"H","x":-115.588,"y":5.1380005,"z":-6.062},{"l":"H","x":-125.116005,"y":10.374001,"z":27.836},{"l":"H","x":-125.17799,"y":-22.512,"z":14.205999},{"l":"H","x":-22.886,"y":25.046001,"z":-41.592003},{"l":"H","x":23.692,"y":43.415993,"z":32.788},{"l":"H","x":33.742,"y":9.392,"z":29.842},{"l":"H","x":74.024,"y":-2.606,"z":5.568},{"l":"H","x":49.914,"y":78.914,"z":12.550001},{"l":"H","x":118.50201,"y":11.865999,"z":-9.842},{"l":"H","x":94.43,"y":93.39,"z":-2.7519999},{"l":"H","x":128.714,"y":59.865997,"z":-13.978}],"b":[{"b":10,"e":14,"o":2},{"b":13,"e":14},{"b":14,"e":16},{"b":16,"e":24},{"b":16,"e":22},{"b":21,"e":24},{"b":18,"e":24},{"b":6,"e":22},{"b":7,"e":22},{"b":22,"e":23},{"b":21,"e":23},{"b":17,"e":21},{"b":17,"e":18},{"b":11,"e":18,"o":2},{"b":17,"e":20},{"b":15,"e":20},{"b":5,"e":15},{"b":12,"e":15,"o":2},{"b":5,"e":9},{"b":0,"e":9,"o":2},{"b":1,"e":9},{"b":0,"e":2},{"b":1,"e":3,"o":2},{"b":2,"e":4,"o":2},{"b":3,"e":4},{"b":13,"e":19},{"b":16,"e":25},{"b":21,"e":26},{"b":6,"e":27},{"b":6,"e":28},{"b":6,"e":29},{"b":7,"e":30},{"b":7,"e":31},{"b":7,"e":32},{"b":17,"e":33},{"b":8,"e":20},{"b":5,"e":34},{"b":5,"e":35},{"b":0,"e":36},{"b":1,"e":37},{"b":2,"e":38},{"b":3,"e":39},{"b":4,"e":40}]}}';
//alert("testing contactServer with mol=" + JSON.stringify(o.content.mol));
//o.content = JSON.parse(o.content);
				null!=d&&o.content&&!o.stop&&d(o.content);
				o.stop&&null!=c&&c()
			},
			error:function(){Jmol.inRelay=!1;alert("Server connectivity failed. Please try again or update this browser to the latest version. (XHR2 not supported)");	null!=fError&&fError()},
			beforeSend:function(a){a.withCredentials=!0},
			async:Jmol.asynchronous
		})
	)
}

// changes: MolGrabberCanvas, MolGrabberCanvas3D
//   -- properly scales data using dataMultiplier 1 (3D canvas) or 20 (2d canvas)
//   -- generalized selection/input options
//   -- adds caption
// new: MolGrabberJmol, MolGrabberImage


Jmol.MolGrabberCanvas3D = function(b,c,h,caption,addOptions){
	b&&this.create(b,c,h);
	this.dataMultiplier=1;
	if (addOptions)
		Jmol.getGrabberOptions(this, b,caption); // just for this test page
	return this
}

Jmol.MolGrabberCanvas3D.prototype = new ChemDoodle._Canvas3D;

Jmol.MolGrabberCanvas3D.prototype.setSearchTerm = function(b){
	Jmol.searchQuery(this, b);
}

Jmol.MolGrabberCanvas3D.prototype.search = function(model){
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
}

Jmol.MolGrabberCanvas3D.prototype.searchDatabase = function(model, database){
	this.emptyMessage="Searching...";
	this.molecule=null;
	this.repaint();
	var c = this;
	Jmol.getRawDataFromDatabase(
		database,
		model,
		function(data){Jmol.processFileData(c, data)}
	);
}

Jmol.MolGrabberCanvas3D.prototype.loadFile = function(fileName, params){
	Jmol.loadFile(this, fileName, params);
}

// MolGrabberCanvas changes add a dataMultiplier, subclasses TransformCanvas, modifies display options

Jmol.MolGrabberCanvas = function(b,c,h,caption, addOptions){
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

Jmol.MolGrabberCanvas.prototype = new ChemDoodle.TransformCanvas;

Jmol.MolGrabberCanvas.prototype.setSearchTerm = function(b){
	Jmol.searchQuery(this, b);
}

Jmol.MolGrabberCanvas.prototype.search = Jmol.MolGrabberCanvas3D.prototype.search;

Jmol.MolGrabberCanvas.prototype.searchDatabase = function(model, database){
	this.emptyMessage="Searching...";
	this.molecule=null;
	this.repaint();
	var c=this;
	Jmol.getRawDataFromDatabase(
		database,
		model,
		function(data){Jmol.processFileData(c, data)}
	);
}

Jmol.MolGrabberCanvas.prototype.loadFile = function(fileName, params){
	Jmol.loadFile(this, fileName, params);
}

// MolGrabberJmol -- an alternative to _Canvas3D
//                -- loads the Jmol applet instead of ChemDoodle

Jmol.MolGrabberJmol = function(id,c,h, jmolDirectory, appJar, jmolIsSigned, readyFunctionName, caption, addOptions){
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

Jmol.MolGrabberJmol.prototype.setSearchTerm = function(b){
	Jmol.searchQuery(this, b);
}

Jmol.MolGrabberJmol.prototype.loadMolecule = function(mol) {
	_jmolFindApplet("jmolApplet" + this.id).script('load DATA "model"\n' + mol + '\nEND "model" ' + this.loadParams);
}

Jmol.MolGrabberJmol.prototype.script = function(script) {
	_jmolFindApplet("jmolApplet" + this.id).script(script);
}	

Jmol.MolGrabberJmol.prototype.loadFile = function(fileName, params){
	this.loadParams = (params ? params : "");
	this.thisJmolModel = "" + Math.random();
	var c=this;
	Jmol.loadFileData(
		fileName,
			function(data){c.loadMolecule(data)}
	);
}

Jmol.MolGrabberJmol.prototype.search = Jmol.MolGrabberCanvas3D.prototype.search;

Jmol.MolGrabberJmol.prototype.searchDatabase = function(model, database){
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

// MolGrabberImage -- another alternative to _Canvas

Jmol.MolGrabberImage = function(id,width,height, caption, addOptions){
	this.id = id;
	this.imgWidth = width;
	this.imgHeight = height;
	var img = '<img id="'+id+'_image" width="' + width + '" height="' + height + '" src=""/>';
	document.write(img);
	if (addOptions)
		Jmol.getGrabberOptions(this, id, caption);
	return this;
}

Jmol.MolGrabberImage.prototype.setSearchTerm = function(b){
	Jmol.searchQuery(this, b);
}

Jmol.MolGrabberImage.prototype.loadFile = function(fileName, params){
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
	document.getElementById(this.id + "_image").src = src;
}

Jmol.MolGrabberImage.prototype.search = Jmol.MolGrabberCanvas3D.prototype.search;

Jmol.MolGrabberImage.prototype.searchDatabase = function(model, database){
	var src = Jmol.SERVER_URL 
		+ "?call=getImageFromDatabase"
		+ "&query=" + model;
		+ "&width=" + this.imgWidth
		+ "&height=" + this.imgHeight
		+ "&database=" + database;
	document.getElementById(this.id + "_image").src = src;
}

// Jmol core functionality
	
Jmol.getGrabberOptions = function(canvas, label, note) {

	// for now, only NCI
	// Bob Hanson, hansonr@stolaf.edu 4/14/2012
	
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

Jmol.loadFile = function(molGrabber, fileName){
	molGrabber.emptyMessage="Searching...";
	molGrabber.molecule=null;
	molGrabber.repaint();
	molGrabber.jmolFileType = Jmol.getFileType(fileName);
	Jmol.loadFileData(
		fileName,
		function(data){Jmol.processFileData(molGrabber, data)}
	);
}
	
Jmol.getFileType = function(name) {
	// just the extension, which must be PDB, XYZ..., CIF, or MOL
	name = name.split('.').pop().toUpperCase();
	return name.substring(0, Math.min(name.length, 3));
}

Jmol.searchQuery = function(molgrabber, query) {
		jQuery("#"+molgrabber.id+"_query").val(query);
		molgrabber.search(query.replace(/\"/g, ""));
}

Jmol.processFileData = function(molGrabber, data) {
	var factor = molGrabber.dataMultiplier;
	data = Jmol.cleanFileData(data);
	var molecule;
	switch(molGrabber.jmolFileType) {
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
	molGrabber.loadMolecule(Jmol.scaleMolecule(molecule, factor));
}	

Jmol.cleanFileData = function(data) {
	if (data.indexOf("\r") >= 0 && data.indexOf("\n") >= 0) {
		return data.replace(/\r\n/g,"\n");
	}
	if (data.indexOf("\r") >= 0) {
		return data.replace(/\r/g,"\n");
	}
	return data;
}
	
Jmol.scaleMolecule = function(molecule, multiplier) {
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
