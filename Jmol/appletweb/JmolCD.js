// JmolCD.js -- Jmol ChemDoodle extension	 author: Bob Hanson, hansonr@stolaf.edu	4/16/2012

// This library requires
// 
//	JmolCore.js
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
			Jmol._getGrabberOptions(this, caption);
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
			Jmol._getGrabberOptions(this, caption);
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
			return (name.substring(1,2) == "=" ? "LCIF" : "PDB");
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
		case "LCIF":
			molecule = Jmol._cdReadLigandCIF(data);
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

	Jmol._cdReadLigandCIF = function(data) {
	// strictly a hack
    var molecule = new ChemDoodle.structures.Molecule();
		if (data == null || data.length == 0)
			return molecule;
		var pt = data.indexOf("_chem_comp.id");
		if (pt < 0)
			return molecule;
		var ID = jQuery.trim(data.substring(pt+13, data.indexOf("\n", pt)));
		var lines = data.split('\n' + ID);
		var isAtoms = true;
		var atoms = {};
		var aFields = [0,2,11,12,13];
		var bFields = [0,1,2];
		for (var i = 1, n = lines.length; i < n; i++) {
			var line = lines[i];
			if (isAtoms) {
				var List = Jmol._cdGetList(line, aFields);
				var sym = List[1];
				if (sym.length == 2)
				  sym = sym.charAt(0) + sym.charAt(1).toLowerCase();
				molecule.atoms.push(atoms[List[0]] = new ChemDoodle.structures.Atom(sym, parseFloat(List[2]), parseFloat(List[3]), parseFloat(List[4])));
				if (line.indexOf("comp_bond") >= 0)
					isAtoms = false;
			} else {
				var List = Jmol._cdGetList(line, bFields);
				var bondOrder = 1;
				switch(List[2]) {
				case "DOUB":
					bondOrder = 2;
					break;
				case "TRIP":
					bondOrder = 3;
					break;
				}
				molecule.bonds.push(new ChemDoodle.structures.Bond(atoms[List[0]], atoms[List[1]], bondOrder));
				if (line.indexOf("comp_desc") >= 0)
					break;
			}
		}
		return molecule;
	}
	Jmol._cdGetList = function(line, fields) {
		var data = [];
		for (var i = 0, isSpace = true, pt = 0, pt1 = -1, n = line.length, 
		pf = 0, nf = fields.length, af = fields[pf]; i < n && pf < nf; i++) {
			if (line.charAt(i) === ' ') {
				if (!isSpace && pt1 == fields[pf]) {
					pf++;
					data.push(line.substring(pt, i));
				}
				isSpace = true;
			} else if (isSpace) {
				pt = i;
				pt1++;
				isSpace = false;
			}
		}
   	return data;
	}
})(Jmol);
