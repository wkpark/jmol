// JmolCore.js -- Jmol core capability

// see JmolApi.js for public user-interface. All these are private functions

// last revision: 4/20/2012

// allows Jmol applets to be created on a page with more flexibility and extendability
// possibly using infrastructure of ChemDoodle.

// This package may be used with or without ChemDoodle.
// If using ChemDoodle, this package requires ChemDoodleWeb-libs.js and ChemDoodleWeb.js prior to JmolCore.js
// If not using ChemDoodle, this package requires jQuery.js (or ChemDoodleWeb-libs.js, which conains jQuery) 

// Allows Jmol-like objects to be displayed on Java-challenged (iPad/iPhone)
// or applet-challenged (Android/iPhone) platforms, with automatic switching to 
// whatever is appropriate. You can specify "ChemDoodle-only", "Jmol-only", "Image-only"
// or some combination of those -- and of course, you are free to rewrite the logic below! 

// Allows ChemDoodle-like 3D and 3D-faked 2D canvases that can load files via a privately hosted 
// server that delivers raw data files rather than specialized JSON mol data.
// Access to iChemLabs server is not required for simple file-reading operations and 
// database access. PubChem and image services are provided by a server-side PHP program
// running JmolData.jar with flags -iR (at St. Olaf College). 
// For your installation, you should consider putting JmolData.jar and jmolcd.php 
// on your own server. Nothing more than these two files is needed on the server.

// The NCI and RCSB databases are accessed via direct AJAX.

if(typeof(ChemDoodle)=="undefined") ChemDoodle = null;

Jmol = (function() {
	return {
		features: {
		  supports_xhr2: function() {return jQuery.support.cors}
		},
		_jmolInfo: {
			userAgent:navigator.userAgent, 
			version: version = 'Jmol 12.3.23' + (ChemDoodle ? "; ChemDoodle " + ChemDoodle.getVersion(): "")
		},
		_serverUrl: "http://chemapps.stolaf.edu/jmol/jmolcd.php",
		_asynchronous: !0,
		db: {
			_databasePrefixes: "$=:",
			_nciLoadScript: ";n = ({molecule=1}.length < {molecule=2}.length ? 2 : 1); select molecule=n;display selected;center selected;",
			_fileLoadScript: ";if (_loadScript = '' && defaultLoadScript == '' && _filetype == 'Pdb') { select protein or nucleic;cartoons Only;color structure; select * };",
			_DirectDatabaseCalls:{
				"$": "http://cactus.nci.nih.gov/chemical/structure/%FILE/file?format=sdf&get3d=True",
				"=": "http://www.rcsb.org/pdb/files/%FILE.pdb",
				"==": "http://www.rcsb.org/pdb/files/ligand/%FILE.cif",
				//":": "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=1983&disopt=3DDisplaySDF",
				"::": "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pccompound&term=%22%FILE%22[completesynonym]"
			},
			_restQueryUrl: "http://www.rcsb.org/pdb/rest/search",
			_restQueryXml: "<orgPdbQuery><queryType>org.pdb.query.simple.AdvancedKeywordQuery</queryType><description>Text Search</description><keywords>QUERY</keywords></orgPdbQuery>",
			_restReportUrl: "http://www.pdb.org/pdb/rest/customReport?pdbids=IDLIST&customReportColumns=structureId,structureTitle"
		}
	}
})();

(function (Jmol) {

	// Jmol core functionality

	Jmol._getGrabberOptions = function(applet, label, note) {
	
		// feel free to adjust this look to anything you want
		
		var c=[];
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
				applet._search()
			}
		);
		jQuery("#"+label+"_query").keypress(
			function(a){
				13==a.which&&applet._search()
			}
		);
		if (applet.repaint) {
			applet.emptyMessage="Enter search term below",
			applet.repaint()
		};
	}
	
	Jmol._getWrapper = function(applet, isHeader) {
		var s = (isHeader ?
			"<div id=ID_appletinfotablediv style=width:Wpx;height:Hpx>\
			<table><tr><td></td><td><div id=ID_infotablediv style=width:Wpx;height:Hpx;display:none>\
			<table><tr height=20><td style=background:yellow><span id=ID_infoheaderdiv></span></td>\
			<td width=10><a href=javascript:Jmol.showInfo(ID,false)>[x]</a></td></tr><tr><td colspan=2>\
			<div id=ID_infodiv style=overflow:scroll;width:Wpx;height:" + (applet._height - 15) + "px></div></td></tr></table></div></td></tr>\
			<tr><td><div id=ID_appletdiv style=width:Wpx;height:Hpx>"
			:"</div></td><td></td></tr></table></div>"
		).replace(/H/g, applet._height).replace(/W/g, applet._width).replace(/ID/g, applet._id);
		document.write(s);
	}

	Jmol._getScriptForDatabase = function(database) {
		return (database == "$" ? Jmol.db._nciLoadScript : Jmol.db._fileLoadScript);
	}
	
   //   <dataset><record><structureId>1BLU</structureId><structureTitle>STRUCTURE OF THE 2[4FE-4S] FERREDOXIN FROM CHROMATIUM VINOSUM</structureTitle></record><record><structureId>3EUN</structureId><structureTitle>Crystal structure of the 2[4Fe-4S] C57A ferredoxin variant from allochromatium vinosum</structureTitle></record></dataset>
      
	Jmol._setInfo = function(applet, database, data) {
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
					info.push("<tr><td valign=top><a href=\"javascript:Jmol.search(" + applet._id + ",'=" + S[i].substring(0, 4) + "')\">" + S[i].substring(0, 4) + "</a></td>");
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
		applet._infoHeader = header;
		applet._info = info.join("");
		applet._showInfo(true);
	}
	
	Jmol._loadSuccess = function(a, fSuccess) {
		Jmol._ajaxActive=!1;
		fSuccess(a);
	}

	Jmol._loadError = function(fError){
		Jmol._ajaxActive=!1;
		Jmol.say("Error connecting to server.");	
		null!=fError&&fError()
	}
	
	Jmol._isDatabaseCall = function(query) {
		return (Jmol.db._databasePrefixes.indexOf(query.substring(0, 1)) >= 0);
	}
	
	Jmol._getDirectDatabaseCall = function(query, checkXhr2) {
		if (checkXhr2 && !Jmol.features.supports_xhr2())
			return query;
		var pt = 2;
		var call = Jmol.db._DirectDatabaseCalls[query.substring(0,pt)];
		if (!call)
			call = Jmol.db._DirectDatabaseCalls[query.substring(0,--pt)];
		query = (call ? call.replace(/\%FILE/, query.substring(pt)) : query);
		return query;
	}
	
	Jmol._getRawDataFromServer = function(database,query,fSuccess,fError){
		Jmol._contactServer(
			"getRawDataFromDatabase",
			{database:database,query:query,script:Jmol._getScriptForDatabase(database)},
			fSuccess, fError
		);
	}
	
	Jmol._getInfoFromDatabase = function(applet, database, query){
		if (database == "====") {
			if (Jmol._checkActive())
				return;
			var data = Jmol.db._restQueryXml.replace(/QUERY/,query);
			var info = {
				dataType: "text",
				type: "POST",
				contentType:"application/x-www-form-urlencoded",
				url: Jmol.db._restQueryUrl,
				data: encodeURIComponent(data) + "&req=browser",
				success: function(data) {Jmol._ajaxActive=!1;Jmol._extractInfoFromRCSB(applet, database, query, data)},
				error: function() {Jmol._loadError(null)},
				async: Jmol._asynchronous
			}
			jQuery.ajax(info);
			return;
		}		
		Jmol._contactServer(
			"getInfoFromDatabase",
			{database:database,query:query},
			function(data) {Jmol._setInfo(applet, database, data) }
		);
	}
	
	Jmol._extractInfoFromRCSB = function(applet, database, query, output) {
		var n = output.length/5;
		if (n == 0)
			return;	
		if (query.length == 4 && n != 1) {
			var QQQQ = query.toUpperCase();
			var pt = output.indexOf(QQQQ);
			if (pt > 0 && "123456789".indexOf(QQQQ.substring(0, 1)) >= 0)
				output = QQQQ + "," + output.substring(0, pt) + output.substring(pt + 5);
			if (n > 50)
				output = output.substring(0, 250);
			output = output.replace(/\n/g,",");
			var url = Jmol._restReportUrl.replace(/IDLIST/,output);
			Jmol._loadFileData(applet, url, function(data) {Jmol._setInfo(applet, database, data) });		
		}
	}

	Jmol._checkActive = function() {
		if (Jmol._ajaxActive) {
			Jmol.say("Already connecting to the server - please wait for the first request to finish.");
			return true;
		}		
		Jmol._ajaxActive=!0;
		return false;
	}
	
	Jmol._loadFileData = function(applet, fileName, fSuccess, fError){
		if (Jmol._isDatabaseCall(fileName)) {
			Jmol._setQueryTerm(applet, fileName);
			fileName = Jmol._getDirectDatabaseCall(fileName, true);
			if (Jmol._isDatabaseCall(fileName)) {
				// xhr2 not supported (MSIE)
				fileName = Jmol._getDirectDatabaseCall(fileName, false);
				Jmol._getRawDataFromServer("_",fileName,fSuccess,fError);		
				return;
			}
		}	
		if (!Jmol._checkActive())
			jQuery.ajax({
				dataType: "text",
				url: fileName,
				success: function(a) {Jmol._loadSuccess(a, fSuccess)},
				error: function() {Jmol._loadError(fError)},
				async: Jmol._asynchronous
			});
	}
	
	Jmol._contactServer = function(cmd,content,fSuccess,fError){
		if (!Jmol._checkActive())
			jQuery.ajax({
				dataType: "text",
				type: "POST",
				data: JSON.stringify({
					call: cmd,
					content: content,
					info: Jmol._jmolInfo
				}),
				url: Jmol._serverUrl,
				success: function(a) {Jmol._loadSuccess(a, fSuccess)},
				error:function() { Jmol._loadError(fError) },
				async:Jmol._asynchronous
			});
	}

	Jmol._setQueryTerm = function(applet, query) {
		if (!query || !applet._hasOptions)
			return;
		if (Jmol._isDatabaseCall(query)) {
			var database = query.substring(0, 1);
			query = query.substring(1);
			if (database == "=" && query.length == 4 && query.substring(0, 1) == "=")
				query = query.substring(1);
			var d = document.getElementById(applet._id + "_select");
			for (var i = 0; i < d.options.length; i++)
				if (d[i].value == database)
					d[i].selected = true;
		}
		jQuery("#"+applet._id+"_query").val(query);
	}
	

})(Jmol);
