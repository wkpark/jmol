// JmolCD.js -- Jmol ChemDoodle extension   author: Bob Hanson, hansonr@stolaf.edu  4/16/2012

// This library requires
// 
//  JmolCore.js
//	gl-matrix-min.js 
//	jQuery.min.js
//	mousewheel.js 
//	ChemDoodleWeb.js
//
// prior to JmolCD.js

if(typeof(ChemDoodle)=="undefined") ChemDoodle = null;


(function (Jmol) {

	if (!ChemDoodle) 
		return;
		
	Jmol._getCanvas = function(id, Info) {
		// overrides the function in JmolCore.js
		// ChemDoodle: first try with WebGL unless that doesn't work or we have indicated NOWEBGL
		var applet = (Info.useWebGlIfAvailable && ChemDoodle.featureDetection.supports_webgl() 
			? new Jmol._Canvas3D(id, Info, null) : null);
		if (applet && applet.gl) {
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
		return applet;
	}

	Jmol._Canvas3D = function(id, Info, caption){
		this._jmolType = "Jmol._Canvas3D";
		this._id = id;
		this._width = Info.width;
		this._height = Info.height;
		this._hasOptions = Info.addSelectionOptions;
		this._dataMultiplier=1;
		this._info = JSON.stringify(this);
		this._infoHeader = this._jmolType + ' "' + this._id + '"'
		Jmol._getWrapper(this, true);
		this.create(id,Info.width,Info.height);
		Jmol._getWrapper(this, false);
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, id, caption);
		return this;
	}
	
	Jmol._Canvas = function(id, Info, caption){
		this._jmolType = "Jmol._Canvas";
		this._id = id;
		this._width = Info.width;
		this._height = Info.height;
		this._hasOptions = Info.addSelectionOptions;
		this._info = JSON.stringify(this);
		this._infoHeader = this._jmolType + ' "' + this._id + '"'
		Jmol._getWrapper(this, true);
		this.create(id, Info.width, Info.height);
		Jmol._getWrapper(this, false);
		this._dataMultiplier=20;
		this.lastPoint=null;
		this.rotate3D=true;
		this.rotationMultMod=1.3;
		this.lastPinchScale=1;
		this.lastGestureRotate=0;
		if (Info.addSelectionOptions)
			Jmol._getGrabberOptions(this, id, caption);
		return this;
	}

  var _cdSetPrototype = function(proto) {
  
  	Jmol._setCommonMethods(proto);

		proto._script = function(script) {} // not implemented
	
		proto._searchDatabase = function(query, database, script){
			this._showInfo(false);
			if (query.indexOf("?") >= 0) {
			  Jmol._getInfoFromDatabase(this, database, query.split("?")[0]);
			  return;
			}
			if (Jmol.db._DirectDatabaseCalls[database]) {
				this._loadFile(database + query);
				return;
			}
			this.emptyMessage="Searching...";
			this.molecule=null;
			this.repaint();
			var c = this;
			Jmol._getRawDataFromServer(
				database,
				query,
				function(data){Jmol._cdProcessFileData(c, data)}
			);
		}
		
		proto._show = function(tf) {
			document.getElementById(this._id + "_appletdiv").style.display = (tf ? "block" : "none");
		}
	
		proto._loadFile = function(fileName){
			this._showInfo(false);
			this._thisJmolModel = "" + Math.random();
			this.emptyMessage="Retrieving data...";
			this.molecule=null;
			this.repaint();
			this._jmolFileType = Jmol._cdGetFileType(fileName);
			var cdcanvas = this;
			Jmol._loadFileData(this, fileName, function(data){Jmol._cdProcessFileData(cdcanvas, data)});
		}

		return proto;		
	}

	Jmol._Canvas3D.prototype = _cdSetPrototype(new ChemDoodle._Canvas3D);
	Jmol._Canvas.prototype = _cdSetPrototype(new ChemDoodle.TransformCanvas);

	Jmol._cdGetFileType = function(name) {
		var database = name.substring(0, 1);
		if (database == "$" || database == ":")
			return "MOL";
		if (database == "=")
			return (name.substring(1,2) == "=" ? "CIF_noUnitCell" : "PDB");
		// just the extension, which must be PDB, XYZ..., CIF, or MOL
		name = name.split('.').pop().toUpperCase();
		return name.substring(0, Math.min(name.length, 3));
	}
	
	Jmol._cdProcessFileData = function(cdcanvas, data) {
		var factor = cdcanvas._dataMultiplier;
		data = Jmol._cdCleanFileData(data);
		var molecule;
		switch(cdcanvas._jmolFileType) {
		case "PDB":
		case "PQR":
			molecule = ChemDoodle.readPDB(data, 1);
			// note: default factor for readPDB is 1
			break;
		case "XYZ":
			molecule = ChemDoodle.readXYZ(data, 1);
			// 1 here is just in case
			break;
		case "CIF_noUnitCell":
			molecule = ChemDoodle.readCIF(data, 0, 0, 0, 1);
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
		cdcanvas.loadMolecule(Jmol._cdScaleMolecule(molecule, factor));
	}	
	
	Jmol._cdCleanFileData = function(data) {
		if (data.indexOf("\r") >= 0 && data.indexOf("\n") >= 0) {
			return data.replace(/\r\n/g,"\n");
		}
		if (data.indexOf("\r") >= 0) {
			return data.replace(/\r/g,"\n");
		}
		return data;
	}
		
	Jmol._cdScaleMolecule = function(molecule, multiplier) {
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
