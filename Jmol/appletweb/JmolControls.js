(function(Jmol) {

	var c = Jmol.controls = {

		_hasResetForms: false,	
		_scripts: [""],
		_checkboxMasters: {},
		_checkboxItems: {},
	
		_buttonCount: 0,
		_checkboxCount: 0,
		_radioGroupCount: 0,
		_radioCount: 0,
		_linkCount: 0,
		_cmdCount: 0,
		_menuCount: 0,
		
		_previousOnloadHandler: null,	
		_control: null,
		_element: null,
		
		_appletCssClass: null,
		_appletCssText: "",
		_buttonCssClass: null,
		_buttonCssText: "",
		_checkboxCssClass: null,
		_checkboxCssText: "",
		_radioCssClass: null,
		_radioCssText: "",
		_linkCssClass: null,
		_linkCssText: "",
		_menuCssClass: null,
		_menuCssText: ""
	};

	c._addScript = function(appId,script) {
		if (!script)
			return 0;
		var index = c._scripts.length;
		c._scripts[index] = [appId, script];
		return index;
	}
	
	c._getIdForControl = function(appletOrId, script) {
		return (typeof appletOrId == "string" ? appletOrId 
		  : !script || appletOrId._canScript(script) ? appletOrId._id
			: null);
	}
		
	c._getRadio = function(appletOrId, script, labelHtml, isChecked, separatorHtml, groupName, id, title) {
		var appId = c._getIdForControl(appletOrId, script);
		if (appId == null)
			return null;
		++c._radioCount;
		groupName != undefined && groupName != null || (groupName = "jmolRadioGroup" + (c._radioGroupCount - 1));
		if (!script)
			return "";
		labelHtml != undefined && labelHtml != null || (labelHtml = script.substring(0, 32));
		separatorHtml || (separatorHtml = "");
		var scriptIndex = c._addScript(script);
		var eospan = "</span>";
		var t = "<span id=\"span_"+id+"\""+(title ? " title=\"" + title + "\"":"")+"><input name='"
		+ groupName + "' id='"+id+"' type='radio' onclick='Jmol.controls._click(this," +
					 scriptIndex + "," + appId + ");return true;' onmouseover='Jmol.controls._mouseOver(" +
					 scriptIndex + ");return true;' onmouseout='Jmol.controls._mouseOut()' " +
		 (isChecked ? "checked='true' " : "") + c._radioCssText + " />";
		if (labelHtml.toLowerCase().indexOf("<td>")>=0) {
			t += eospan;
			eospan = "";
		}
		t += "<label for=\"" + id + "\">" + labelHtml + "</label>" +eospan + separatorHtml;
		return t;
	}
	
/////////// events //////////

	c._scriptExecute = function(element, scriptInfo) {
		var applet = Jmol._applets[scriptInfo[0]];
		var script = scriptInfo[1];
		if (typeof(script) == "object")
			script[0](element, script, applet);
		else
			Jmol.script(applet, script);
	}
	
	c._commandKeyPress = function(e, id, appId) {
		var keycode = (e == 13 ? 13 : window.event ? window.event.keyCode : e ? e.which : 0);
		if (keycode == 13) {
			var inputBox = document.getElementById(id)
			Jmol.controls._scriptExecute(inputBox, [appId, inputBox.value]);
		}
	}
	
	c._click = function(elementClicked, scriptIndex) {
		Jmol.controls._element = elementClicked;
		Jmol.controls._scriptExecute(elementClicked, Jmol.controls._scripts[scriptIndex]);
	}
	
	c._menuSelected = function(menuObject, appId) {
		var scriptIndex = menuObject.value;
		if (scriptIndex != undefined) {
			Jmol.controls._scriptExecute(menuObject, Jmol.controls._scripts[scriptIndex]);
			return;
		}
		var len = menuObject.length;
		if (typeof len == "number")
			for (var i = 0; i < len; ++i)
				if (menuObject[i].selected) {
					Jmol.controls._click(menuObject[i], menuObject[i].value, appId);
					return;
				}
		alert("?Que? menu selected bug #8734");
	}
		
	c._cbNotifyMaster = function(m){
		//called when a group item is checked
		var allOn = true;
		var allOff = true;
		for (var chkBox in m.chkGroup){
			if(m.chkGroup[chkBox].checked)
				allOff = false;
			else
				allOn = false;
		}
		if (allOn)m.chkMaster.checked = true;
		if (allOff)m.chkMaster.checked = false;
		if ((allOn || allOff) && Jmol.controls._checkboxItems[m.chkMaster.id])
			Jmol.controls._cbNotifyMaster(Jmol.controls._checkboxItems[m.chkMaster.id])
	}
	
	c._cbNotifyGroup = function(m, isOn){
		//called when a master item is checked
		for (var chkBox in m.chkGroup){
			var item = m.chkGroup[chkBox]
			item.checked = isOn;
			if (Jmol.controls._checkboxMasters[item.id])
				Jmol.controls._cbNotifyGroup(Jmol.controls._checkboxMasters[item.id], isOn)
		}
	}
	
	c._cbSetCheckboxGroup = function(chkMaster, chkbox){
		var id = chkMaster;
		if(typeof(id)=="number")id = "jmolCheckbox" + id;
		chkMaster = document.getElementById(id);
		if (!chkMaster)alert("jmolSetCheckboxGroup: master checkbox not found: " + id);
		var m = Jmol.controls._checkboxMasters[id] = {};
		m.chkMaster = chkMaster;
		m.chkGroup = {};
		for (var i = 1; i < arguments.length; i++){
			var id = arguments[i];
			if(typeof(id)=="number")id = "jmolCheckbox" + id;
			checkboxItem = document.getElementById(id);
			if (!checkboxItem)alert("jmolSetCheckboxGroup: group checkbox not found: " + id);
			m.chkGroup[id] = checkboxItem;
			Jmol.controls._checkboxItems[id] = m;
		}
	}
	
	c._cbClick = function(ckbox, whenChecked, whenUnchecked, applet) {
		var c = Jmol.controls;
		c._control = ckbox;
		c._click(ckbox, ckbox.checked ? whenChecked : whenUnchecked, applet);
		if(c._checkboxMasters[ckbox.id])
			c._notifyGroup(c._checkboxMasters[ckbox.id], ckbox.checked)
		if(c._checkboxItems[ckbox.id])
			c._notifyMaster(c._checkboxItems[ckbox.id])
	}
	
	c._cbOver = function(ckbox, whenChecked, whenUnchecked) {
		window.status = Jmol.controls._scripts[ckbox.checked ? whenUnchecked : whenChecked];
	}
	
	c._mouseOver = function(scriptIndex) {
		window.status = c._scripts[scriptIndex];
	}
	
	c._mouseOut = function() {
		window.status = " ";
		return true;
	}

	c._onloadResetForms = function() {
		var c = Jmol.controls;
		// must be evaluated ONLY once -- is this compatible with jQuery?
		if (c._hasResetForms)
			return;
		c._hasResetForms = true;
		c._previousOnloadHandler = window.onload;
		window.onload = function() {
			var c = Jmol.controls;
			if (c._buttonCount+c._checkboxCount+c._menuCount+c._radioCount+c._radioGroupCount > 0) {
				var forms = document.forms;
				for (var i = forms.length; --i >= 0; )
					forms[i].reset();
			}
			if (c._previousOnloadHandler)
				c._previousOnloadHandler();
		}
	}


})(Jmol);
