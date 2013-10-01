// JmolApi.js -- Jmol user functions  Bob Hanson hansonr@stolaf.edu

// BH 9/3/2013 5:48:03 PM simplification of Jmol.getAppletHTML()
// BH 5/16/2013 9:01:41 AM checkbox group fix
// BH 1/15/2013 10:55:06 AM updated to default to HTML5 not JAVA
 
// along with this file you need at least JmolCore.js and JmolApplet.js. Also, if you want buttons, JmolControls.js
// in that order. Then include JmolApi.js. 

// default settings are below. Generally you would do something like this:

// jmol = "jmol"
// Info = {.....your settings if not default....}
// Jmol.jmolButton(jmol,....)
// jmol = Jmol.getApplet(jmol, Info)
// Jmol.script(jmol,"....")
// Jmol.jmolLink(jmol,....)
// etc. 
// first parameter is always the applet id, either the string "jmol" or the object defined by Jmol.getApplet()
// no need for waiting to start giving script commands. You can also define a callback function as part of Info.

// see JmolCore.js for details

// BH 8/12/2012 5:15:11 PM added Jmol.getAppletHtml()

(function (Jmol) {

	Jmol.getVersion = function(){return Jmol._jmolInfo.version};

	Jmol.getApplet = function(id, Info, checkOnly) {
  	// requires JmolApplet.js and java/JmolApplet*.jar
    /*
		var DefaultInfo = {
			color: "#FFFFFF", // applet object background color, as for older jmolSetBackgroundColor(s)
			width: 300,
			height: 300,
			addSelectionOptions: false,
			serverURL: "http://chemapps.stolaf.edu/jmol/jsmol/jsmol.php",
			defaultModel: "",
			script: null,
			src: null,
			readyFunction: null,
			use: "HTML5",//other options include JAVA, WEBGL, and IMAGE
			jarPath: "java",
			jarFile: "JmolApplet0.jar",
			isSigned: false,
			j2sPath: "j2s",
      coverImage: null,     // URL for image to display
      coverTitle: "",       // tip that is displayed before model starts to load
      coverCommand: "",     // Jmol command executed upon clicking image
      deferApplet: false,   // true == the model should not be loaded until the image is clicked
      deferUncover: false,  // true == the image should remain until command execution is complete 
			disableJ2SLoadMonitor: false,
			disableInitialConsole: false,
			debug: false
		};	 
    
    */
    return Jmol._Applet._getApplet(id, Info, checkOnly);
	}

	Jmol.getJMEApplet = function(id, Info, linkedApplet, checkOnly) {
  	// requires JmolJME.js and jme/JME.jar
    /*
		var DefaultInfo = {
			width: 300,
			height: 300,
			jarPath: "jme",
			jarFile: "JME.jar",
			options: "autoez"
			// see http://www2.chemie.uni-erlangen.de/services/fragment/editor/jme_functions.html
			// rbutton, norbutton - show / hide R button
			// hydrogens, nohydrogens - display / hide hydrogens
			// query, noquery - enable / disable query features
			// autoez, noautoez - automatic generation of SMILES with E,Z stereochemistry
			// nocanonize - SMILES canonicalization and detection of aromaticity supressed
			// nostereo - stereochemistry not considered when creating SMILES
			// reaction, noreaction - enable / disable reaction input
			// multipart - possibility to enter multipart structures
			// number - possibility to number (mark) atoms
			// depict - the applet will appear without editing butons,this is used for structure display only
		};		    
    */
    return Jmol._JMEApplet._getApplet(id, Info, linkedApplet, checkOnly);
  }
  	
	Jmol.getJSVApplet = function(id, Info, checkOnly) {
	  // requires JmolJSV.js and either JSpecViewApplet.jar or JSpecViewAppletSigned.jar  
    /*
  	var DefaultInfo = {
			width: 500,
			height: 300,
			debug: false,
			jarPath: ".",
			jarFile: "JSpecViewApplet.jar",
			isSigned: false,
			initParams: null,
			readyFunction: null,
			script: null
		};
    */
    return Jmol._JSVApplet._getApplet(id, Info, checkOnly);
  }	


////////////////// scripting ///////////////////
  
	Jmol.loadFile = function(applet, fileName, params){
		applet._loadFile(fileName, params);
	}

	Jmol.script = function(applet, script) {
    if (applet._checkDeferred(script)) 
      return;
		applet._script(script);
	}
	
	Jmol.scriptWait = function(applet, script) {
		return applet._scriptWait(script);
	}
	
	Jmol.scriptEcho = function(applet, script) {
		return applet._scriptEcho(script);
	}
	
	Jmol.scriptMessage = function(applet, script) {
		return applet._scriptMessage(script);
	}
	
	Jmol.scriptWaitOutput = function(applet, script) {
		return applet._scriptWait(script);
	}
	
	Jmol.scriptWaitAsArray = function(applet, script) {
		return applet._scriptWaitAsArray(script);
	}
	
	Jmol.search = function(applet, query, script) {
		applet._search(query, script);
	}

////////////////// "get" methods ///////////////////
	
	Jmol.evaluate = function(applet,molecularMath) {
		return applet._evaluate(molecularMath);
	}

  // optional Info here	
  Jmol.getAppletHtml = function(applet, Info) {
    if (Info) {
      Jmol.setDocument(0);
      applet = Jmol.getApplet(applet, Info);
    }  
    return applet._code;
	}
		
	Jmol.getPropertyAsArray = function(applet,sKey,sValue) {
		return applet._getPropertyAsArray(sKey,sValue);
	}

	Jmol.getPropertyAsJavaObject = function(applet,sKey,sValue) {
		return applet._getPropertyAsJavaObject(sKey,sValue);
	}
	
	Jmol.getPropertyAsJSON = function(applet,sKey,sValue) {
		return applet._getPropertyAsJSON(sKey,sValue);
	}

	Jmol.getPropertyAsString = function(applet,sKey,sValue) {
		return applet._getPropertyAsString(sKey,sValue);
	}

	Jmol.getStatus = function(applet,strStatus) {
		return applet._getStatus(strStatus);
	}

	
////////////////// general methods ///////////////////

	Jmol.resizeApplet = function(applet,size) {
		return applet._resizeApplet(size);
	}

	Jmol.restoreOrientation = function(applet,id) {
		return applet._restoreOrientation(id);
	}
	
	Jmol.restoreOrientationDelayed = function(applet,id,delay) {
		return applet._restoreOrientationDelayed(id,delay);
	}
	
	Jmol.saveOrientation = function(applet,id) {
		return applet._saveOrientation(id);
	}
	
	Jmol.say = function(msg) {
		alert(msg);
	}

//////////// console functions /////////////

	Jmol.clearConsole = function(applet) {
		applet._clearConsole();
	}

	Jmol.getInfo = function(applet) {
		return applet._info;
	}

	Jmol.setInfo = function(applet, info, isShown) {
		applet._info = info;
		if (arguments.length > 2)
			applet._showInfo(isShown);
	}

	Jmol.showInfo = function(applet, tf) {
		applet._showInfo(tf);
	}


//////////// controls and HTML /////////////


	Jmol.jmolBr = function() {
		return Jmol._documentWrite("<br />");
	}

	Jmol.jmolButton = function(appletOrId, script, label, id, title) {
		return Jmol.controls._getButton(appletOrId, script, label, id, title);
	}
	
	Jmol.jmolCheckbox = function(appletOrId, scriptWhenChecked, scriptWhenUnchecked,
			labelHtml, isChecked, id, title) {
		return Jmol.controls._getCheckbox(appletOrId, scriptWhenChecked, scriptWhenUnchecked,
			labelHtml, isChecked, id, title);
	}


	Jmol.jmolCommandInput = function(appletOrId, label, size, id, title) {
		return Jmol.controls._getCommandInput(appletOrId, label, size, id, title);
	}
		
	Jmol.jmolHtml = function(html) {
		return Jmol._documentWrite(html);
	}
	
	Jmol.jmolLink = function(appletOrId, script, label, id, title) {
		return Jmol.controls._getLink(appletOrId, script, label, id, title);
	}

	Jmol.jmolMenu = function(appletOrId, arrayOfMenuItems, size, id, title) {
		return Jmol.controls._getMenu(appletOrId, arrayOfMenuItems, size, id, title);
	}

	Jmol.jmolRadio = function(appletOrId, script, labelHtml, isChecked, separatorHtml, groupName, id, title) {
		return Jmol.controls._getRadio(appletOrId, script, labelHtml, isChecked, separatorHtml, groupName, id, title);
	}

	Jmol.jmolRadioGroup = function (appletOrId, arrayOfRadioButtons, separatorHtml, groupName, id, title) {
		return Jmol.controls._getRadioGroup(appletOrId, arrayOfRadioButtons, separatorHtml, groupName, id, title);
	}

	Jmol.setCheckboxGroup = function(chkMaster, chkBoxes) {
    // chkBoxes can be an array or any number of additional string arguments
		Jmol.controls._cbSetCheckboxGroup(chkMaster, chkBoxes, arguments);
	}
	
	Jmol.setDocument = function(doc) {
		
		// If doc is null or 0, Jmol.getApplet() will still return an Object, but the HTML will
		// put in applet._code and not written to the page. This can be nice, because then you 
		// can still refer to the applet, but place it on the page after the controls are made. 
		//
		// This really isn't necessary, though, because there is a simpler way: Just define the 
		// applet variable like this:
		//
		// jmolApplet0 = "jmolApplet0"
		//
		// and then, in the getApplet command, use
		//
		// jmolapplet0 = Jmol.getApplet(jmolApplet0,....)
		// 
		// prior to this, "jmolApplet0" will suffice, and after it, the Object will work as well
		// in any button creation 
		//		 
		//  Bob Hanson 25.04.2012
		
		Jmol._document = doc;
	}

	Jmol.setXHTML = function(id) {
		Jmol._isXHTML = true;
		Jmol._XhtmlElement = null;
		Jmol._XhtmlAppendChild = false;
		if (id){
			Jmol._XhtmlElement = document.getElementById(id);
			Jmol._XhtmlAppendChild = true;
		}
	}

	////////////////////////////////////////////////////////////////
	// Cascading Style Sheet Class support
	////////////////////////////////////////////////////////////////
	
	// BH 4/25 -- added text option. setAppletCss(null, "style=\"xxxx\"")
	// note that since you must add the style keyword, this can be used to add any attribute to these tags, not just css. 
	
	Jmol.setAppletCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._appletCssClass = cssClass);
		Jmol.controls._appletCssText = text ? text + " " : cssClass ? "class=\"" + cssClass + "\" " : "";
	}
	
	Jmol.setButtonCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._buttonCssClass = cssClass);
		Jmol.controls._buttonCssText = text ? text + " " : cssClass ? "class=\"" + cssClass + "\" " : "";
	}
	
	Jmol.setCheckboxCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._checkboxCssClass = cssClass);
		Jmol.controls._checkboxCssText = text ? text + " " : cssClass ? "class=\"" + cssClass + "\" " : "";
	}
	
	Jmol.setRadioCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._radioCssClass = cssClass);
		Jmol.controls._radioCssText = text ? text + " " : cssClass ? "class=\"" + cssClass + "\" " : "";
	}
	
	Jmol.setLinkCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._linkCssClass = cssClass);
		Jmol.controls._linkCssText = text ? text + " " : cssClass ? "class=\"" + cssClass + "\" " : "";
	}
	
	Jmol.setMenuCss = function(cssClass, text) {
		cssClass != null && (Jmol.controls._menuCssClass = cssClass);
		Jmol.controls._menuCssText = text ? text + " ": cssClass ? "class=\"" + cssClass + "\" " : "";
	}

  Jmol.setAppletSync = function(applets, commands, isJmolJSV) {
    Jmol._syncedApplets = applets;   // an array of appletIDs
    Jmol._syncedCommands = commands; // an array of commands; one or more may be null 
    Jmol._syncedReady = {};
    Jmol._isJmolJSVSync = isJmolJSV;
	}
	
	/*
	Jmol._grabberOptions = [
	  ["$", "NCI(small molecules)"],
	  [":", "PubChem(small molecules)"],
	  ["=", "RCSB(macromolecules)"]
	];
	*/
	
	Jmol.setGrabberOptions = function(options) {
	  Jmol._grabberOptions = options;
	}

  Jmol.setAppletHtml = function (applet, divid) {
    if (!applet._code) 
      return;
    Jmol.$html(divid, applet._code);
    if (applet._init && !applet._deferApplet)
      applet._init();
  }

  Jmol.coverApplet = function(applet, doCover) {
    if (applet._cover)
      applet._cover(doCover);
  }
  


})(Jmol);
