/*
  JmolJME.js   Bob Hanson hansonr@stolaf.edu  6/14/2012

  JME 2D option -- use Jmol.getJMEApplet(id, Info, linkedApplet) to access
  
  linkedApplet puts JME into INFO block for that applet; 
	use Jmol.showInfo(jmol,true/false) to show/hide JME
	
	see http://chemapps.stolaf.edu/jmol/jme for files and demo
	
	There is a bug in JME that the first time it loads a model, it centers it, 
	but after that, it fails to center it. Could get around that, perhaps, by
	creating a new JME applet each time.
	
	JME licensing: http://www.molinspiration.com/jme/doc/index.html
	note that required boilerplate: "JME Editor courtesy of Peter Ertl, Novartis"
	
  
  these methods are private to JmolJME.js
  
*/

(function (Jmol, document) {

	Jmol._JMEApplet = function(id, Info, linkedApplet) {
		this._jmolType = "Jmol._JME";
		Jmol._setObject(this, id, Info);
		this._linkedApplet = linkedApplet;
		this._jarFile = Info.jarFile || "JME.jar"; 
		this._jarPath =	Info.jarPath || ".";
		if (Jmol._document) {
			if (this._linkedApplet) {
				this._linkedApplet._infoObject = this;
				this._linkedApplet._info = null;
				var d = Jmol._getElement(this._linkedApplet, "infotablediv");
				d.style.display = "block";
				var d = Jmol._getElement(this._linkedApplet, "infodiv");
				Jmol._document = null;
				d.innerHTML =  this.create();
				Jmol._document = document;
				this._showContainer(false);
			} else {
				this.create();
			}
		}
		return this;
  }
  
  Jmol._JMEApplet.prototype.create = function() {
		var w = (this._linkedApplet ? "2px" : this._containerWidth);
		var h = (this._linkedApplet ? "2px" : this._containerHeight);
		var s = '<applet code="JME.class" id="' + this._id + '_object" name="' + this._id 
			+ '_object" archive="' + this._jarFile + '" codebase="' + this._jarPath + '" width="'+w+'" height="'+h+'">'
			+ '<param name="options" value="autoez" />'	
			+ '</applet>';
		return this._code = Jmol._documentWrite(s);
	}

	Jmol._JMEApplet.prototype._showInfo = function(tf) {
	  // from applet, so here is where we do the SMILES transfer
	  var jme = this._applet = Jmol._getElement(this, "object");
	  var jmol = this._linkedApplet;
	  var jmeSMILES = jme.smiles();
	  var jmolAtoms = jmeSMILES ? Jmol.evaluate(jmol, "{*}.find('SMILES', '" + jmeSMILES + "')") : "({})";
	  var isOK = (jmolAtoms != "({})");
	  if (!isOK) {
		  if (tf) {
		    // toJME
		    this._molData = Jmol.evaluate(jmol, "write('mol')")
		    setTimeout(this._id+"._applet.reset();"+this._id+"._applet.readMolFile("+this._id+"._molData)",100);
		  } else {
		    // toJmol
		    if (jmeSMILES)
			    Jmol.script(jmol, "load \"$" + jmeSMILES + "\"");
		  }
		}
	  this._showContainer(tf);
		this._show(tf);
	}

	Jmol._JMEApplet.prototype._show = function(tf) {
		var w = (!tf ? "2px" : "100%");
		var h = (!tf ? "2px" : "100%");
		Jmol._getElement(this, "object").style.width = w; 
		Jmol._getElement(this, "object").style.height = h; 
		Jmol._getElement(this._linkedApplet, "infoheaderspan").innerHTML = (tf ? this : this._linkedApplet)._infoHeader;
	}

  Jmol._JMEApplet.prototype._showContainer = function(tf) {
		var d = Jmol._getElement(this._linkedApplet, "infoheaderdiv");
		d.style.display = (tf ? "block" : "none");
  	var w = (!tf ? "2px" : this._linkedApplet ? "100%" : this._containerWidth);
		var h = (!tf ? "2px" : this._linkedApplet ? "100%" : this._containerHeight);
		var d = Jmol._getElement(this._linkedApplet, "infotablediv");
		d.style.width = w;
		d.style.height = h;
	}
	
})(Jmol, document);
