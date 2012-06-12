// JmolApi.js -- Jmol user functions

(function (Jmol) {

	Jmol.getVersion = function(){return _version};

	Jmol._defaultInfo = {
			color: "#FFFFFF", // applet object background color, as for older jmolSetBackgroundColor(s)
			width: 300,
			height: 300,
			addSelectionOptions: false,
			serverURL: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
			defaultModel: "",
			useNoApplet: false,
			useJmolOnly: true,
			useWebGlIfAvailable: true,
			useImageOnly: false,
			isSigned: false,
			jarPath: ".",
			jarFile: "JmolApplet0.jar",
			readyFunction: null,
			script: null,
			src: null,
			debug: false
		};
 
	Jmol.getApplet = function(id, Info, checkOnly) {
	
	// note that the variable name the return is assigned to MUST match the first parameter in quotes
	// applet = Jmol.getApplet("applet", Info)

		checkOnly || (checkOnly = !1);
		id || (id = "jmolApplet0");
		Info || (Info = {});
		for (x in Jmol._defaultInfo)
		  if (typeof Info[x] == "undefined")
		  	Info[x] = Jmol._defaultInfo[x];
		Jmol._debugAlert = Info.debug;	
		Info.serverURL && (Jmol._serverUrl = Info.serverURL);
		var model = (checkOnly ? null : Info.defaultModel);
		var applet = null;

		if (!Info.useNoApplet && !Info.useImageOnly && navigator.javaEnabled()) {
		
			Info.jarFile || (Info.jarFile = (Info.isSigned ? "JmolAppletSigned0.jar" : "JmolApplet0.jar")); 
			Info.jarPath || (Info.jarPath = "."); 
			 
		// Jmol applet, signed or unsigned
		
			applet = new Jmol._Applet(id, Info, null, checkOnly);
		} 
 		if (applet == null) {
			if (!Info.useJmolOnly && !Info.useImageOnly) {
				applet = Jmol._getCanvas(id, Info, checkOnly);
			} 
			if (applet == null)
				applet = new Jmol._Image(id, Info, null, checkOnly);
		}
		// keyed to both its string id and itself
		if (!checkOnly) {
		  Jmol._lastAppletID = id;
			Jmol._applets[id] = Jmol._applets[applet] = applet;
		}	
		return applet;
	}

	Jmol.script = function(applet, script) {	
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
		return applet._scriptWait(script);
	}
	
	Jmol.getStatus = function(applet,strStatus) {
		return applet._getStatus(strStatus);
	}
	
	Jmol.getPropertyAsArray = function(applet,sKey,sValue) {
		return applet._getPropertyAsArray(sKey,sValue);
	}

	Jmol.getPropertyAsString = function(applet,sKey,sValue) {
		return applet._getPropertyAsString(sKey,sValue);
	}

	Jmol.getPropertyAsJSON = function(applet,sKey,sValue) {
		return applet._getPropertyAsJSON(sKey,sValue);
	}

	Jmol.getPropertyAsJavaObject = function(applet,sKey,sValue) {
		return applet._getPropertyAsJavaObject(sKey,sValue);
	}
	
	Jmol.evaluate = function(applet,molecularMath) {
		return applet._evaluate(molecularMath);
	}
	
	Jmol.saveOrientation = function(applet,id) {
		return applet._saveOrientation(id);
	}
	
	Jmol.restoreOrientation = function(applet,id) {
		return applet._restoreOrientation(id);
	}
	
	Jmol.restoreOrientationDelayed = function(applet,id,delay) {
		return applet._restoreOrientationDelayed(id,delay);
	}
	
	Jmol.resizeApplet = function(applet,size) {
		return applet._resizeApplet(size);
	}

	Jmol.search = function(applet, query, script) {
		applet._search(query, script);
	}

	Jmol.loadFile = function(applet, fileName, params){
		applet._loadFile(fileName, params);
	}

	Jmol.say = function(msg) {
		alert(msg);
	}

	Jmol.showInfo = function(applet, tf) {
		applet._showInfo(tf);
	}



//////////// controls and HTML /////////////


	Jmol.jmolBr = function() {
		return Jmol._documentWrite("<br />");
	}

	Jmol.jmolButton = function(appletOrId, script, label, id, title) {
		var appId = Jmol.controls._getIdForControl(appletOrId, script);
		if (appId == null)
			return "";
		var c = Jmol.controls;
		//_jmolInitCheck();
		id != undefined && id != null || (id = "jmolButton" + c._buttonCount);
		label != undefined && label != null || (label = script.substring(0, 32));
		++c._buttonCount;
		var scriptIndex = c._addScript(appId, script);
		var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><input type='button' name='" + id + "' id='" + id +
						"' value='" + label +
						"' onclick='Jmol.controls._click(this," + scriptIndex +
						")' onmouseover='Jmol.controls._mouseOver(" + scriptIndex +
						");return true' onmouseout='Jmol.controls._mouseOut()' " +
						c._buttonCssText + " /></span>";
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}
	
	Jmol.jmolCheckbox = function(appletOrId, scriptWhenChecked, scriptWhenUnchecked,
												labelHtml, isChecked, id, title) {
		var appId = Jmol.controls._getIdForControl(appletOrId, scriptWhenChecked);
		if (appId != null)
			appId = Jmol.controls._getIdForControl(appletOrId, scriptWhenUnchecked);
		if (appId == null)
			return "";

		var c = Jmol.controls;
		//_jmolInitCheck();
		id != undefined && id != null || (id = "jmolCheckbox" + c._checkboxCount);
		++c._checkboxCount;
		if (scriptWhenChecked == undefined || scriptWhenChecked == null ||
				scriptWhenUnchecked == undefined || scriptWhenUnchecked == null) {
			alert("jmolCheckbox requires two scripts");
			return;
		}
		if (labelHtml == undefined || labelHtml == null) {
			alert("jmolCheckbox requires a label");
			return;
		}
		var indexChecked = c._addScript(appId, scriptWhenChecked);
		var indexUnchecked = c._addScript(appId, scriptWhenUnchecked);
		var eospan = "</span>"
		var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><input type='checkbox' name='" + id + "' id='" + id +
						"' onclick='Jmol.controls._cbClick(this," +
						indexChecked + "," + indexUnchecked +
						")' onmouseover='Jmol.controls._cbOver(this," + indexChecked + "," +
						indexUnchecked +
						");return true' onmouseout='Jmol.controls._mouseOut()' " +
			(isChecked ? "checked='true' " : "")+ c._checkboxCssText + " />"
		if (labelHtml.toLowerCase().indexOf("<td>")>=0) {
			t += eospan
			eospan = "";
		}
		t += "<label for=\"" + id + "\">" + labelHtml + "</label>" +eospan;
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}

	Jmol.jmolCommandInput = function(appletOrId, label, size, id, title) {
		var appId = Jmol.controls._getIdForControl(appletOrId, "x");
		if (appId == null)
			return "";
		var c = Jmol.controls;
		//_jmolInitCheck();
		id != undefined && id != null || (id = "jmolCmd" + c._cmdCount);
		label != undefined && label != null || (label = "Execute");
		size != undefined && !isNaN(size) || (size = 60);
		++c._cmdCount;
		var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><input name='" + id + "' id='" + id +
						"' size='"+size+"' onkeypress='Jmol.controls._commandKeyPress(event,\""+id+"\",\"" + appId + "\")'><input type=button value = '"+label+"' onclick='Jmol.controls._commandKeyPress(13,\""+id+"\",\"" + appId + "\")' /></span>";
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}
		
	Jmol.jmolHtml = function(html) {
		return Jmol._documentWrite(html);
	}
	
	Jmol.jmolLink = function(appletOrId, script, label, id, title) {
		var appId = Jmol.controls._getIdForControl(appletOrId, script);
		if (appId == null)
			return "";
		var c = Jmol.controls;
		//_jmolInitCheck();
		id != undefined && id != null || (id = "jmolLink" + c._linkCount);
		label != undefined && label != null || (label = script.substring(0, 32));
		++c._linkCount;
		var scriptIndex = c._addScript(appId, script);
		var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><a name='" + id + "' id='" + id +
						"' href='javascript:Jmol.controls._click(this," + scriptIndex + ");' onmouseover='Jmol.controls._mouseOver(" + scriptIndex +
						");return true;' onmouseout='Jmol.controls._mouseOut()' " +
						c._linkCssText + ">" + label + "</a></span>";
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}
	
	Jmol.jmolMenu = function(appletorId, arrayOfMenuItems, size, id, title) {
		var appId = Jmol.controls._getIdForControl(appletOrId, null);
		var c = Jmol.controls;
		//_jmolInitCheck();
		id != undefined && id != null || (id = "jmolMenu" + c._menuCount);
		++c._menuCount;
		var type = typeof arrayOfMenuItems;
		if (type != null && type == "object" && arrayOfMenuItems.length) {
			var len = arrayOfMenuItems.length;
			if (typeof size != "number" || size == 1)
				size = null;
			else if (size < 0)
				size = len;
			var sizeText = size ? " size='" + size + "' " : "";
			var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><select name='" + id + "' id='" + id +
							"' onChange='Jmol.controls._menuSelected(this,\"" + appId + "\")'" +
							sizeText + c._menuCssText + ">";
			for (var i = 0; i < len; ++i) {
				var menuItem = arrayOfMenuItems[i];
				type = typeof menuItem;
				var script = null;
				var text = null;
				var isSelected = null;
				if (type == "object" && menuItem != null) {
					script = menuItem[0];
					text = menuItem[1];
					isSelected = menuItem[2];
				} else {
					script = text = menuItem;
				}
				appId = Jmol.controls._getIdForControl(appletOrId, script);
				if (appId == null)
					return "";
				text == null && (text = script);
				if (script=="#optgroup") {
					t += "<optgroup label='" + text + "'>";
				} else if (script=="#optgroupEnd") {
					t += "</optgroup>";
				} else {
					var scriptIndex = c._addScript(appId, script);
					var selectedText = isSelected ? "' selected='true'>" : "'>";
					t += "<option value='" + scriptIndex + selectedText + text + "</option>";
				}
			}
			t += "</select></span>";
			if (Jmol._debugAlert)
				alert(t);
			return Jmol._documentWrite(t);
		}
	}
	
	Jmol.jmolRadio = function(appletOrId, script, labelHtml, isChecked, separatorHtml, groupName, id, title) {
		//_jmolInitCheck();
		if (Jmol.controls._radioGroupCount == 0)
			++Jmol.controls._radioGroupCount;
		var t = Jmol.controls._getRadio(appletOrId, script, labelHtml, isChecked, separatorHtml, groupName, (id ? id : groupName + "_" + Jmol._radioCount), title ? title : 0);
		if (t == null)
			return "";
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}
	
	Jmol.jmolRadioGroup = function (appletOrId, arrayOfRadioButtons, separatorHtml, groupName, id, title) {
		/*
	
			array: [radio1,radio2,radio3...]
			where radioN = ["script","label",isSelected,"id","title"]
	
		*/
	
		//_jmolInitCheck();
		var type = typeof arrayOfRadioButtons;
		if (type != "object" || type == null || ! arrayOfRadioButtons.length) {
			alert("invalid arrayOfRadioButtons");
			return;
		}
		var c = Jmol.controls;
		separatorHtml != undefined && separatorHtml != null || (separatorHtml = "&nbsp; ");
		var len = arrayOfRadioButtons.length;
		++c._radioGroupCount;
		groupName || (groupName = "jmolRadioGroup" + (c._radioGroupCount - 1));
		var t = "<span id='"+(id ? id : groupName)+"'>";
		for (var i = 0; i < len; ++i) {
			if (i == len - 1)
				separatorHtml = "";
			var radio = arrayOfRadioButtons[i];
			type = typeof radio;
			var s = null;
			if (type == "object") {
				t += (s = c._getRadio(appletOrId, radio[0], radio[1], radio[2], separatorHtml, groupName, (radio.length > 3 ? radio[3]: (id ? id : groupName)+"_"+i), (radio.length > 4 ? radio[4] : 0), title));
			} else {
				t += (s = c._getRadio(appletOrId, radio, null, null, separatorHtml, groupName, (id ? id : groupName)+"_"+i, title));
			}
			if (s == null)
			  return "";
		}
		t+="</span>"
		if (Jmol._debugAlert)
			alert(t);
		return Jmol._documentWrite(t);
	}
	
	Jmol.setCheckboxGroup = function(chkMaster,chkBox) {
		Jmol.controls._cbSetCheckboxGroup(chkMaster, chkBox);
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
})(Jmol);
