/*	Support file for building jmol_colors.html color tables
	Do not translate anything in this file
	Angel Herráez, 29 july 2005
*/

function sectionMenu()
{
	document.write('<form style="background-color:#F7D0B2; padding:5px; text-align:center;">Sections in this document:')
	var anclas = ["..choose..", "Atoms", "Secondary", "Residues", "Chains", "Charge","Red_white_blue_gradient","Blue_red_rainbow_gradient","Isosurfaces"]
	var tx = "<select onChange='location.hash=this.options[selectedIndex].value; this.selectedIndex=0'>"
	for (var i=0;i<anclas.length;i++)
	{	tx += "<option value='" + anclas[i] + "'>" + anclas[i] + "</option>"
	}
	tx += "</select>"
	document.write(tx)
	document.write('</form>')
}

function populatePeriodicTable()
{	// populate periodic table with useful parameters:
	var perTableRows = document.getElementById("perTable").rows
	for (var f=0;f<perTableRows.length;f++)	//every row
	{	var x=perTableRows[f].cells
		for (c=0;c<x.length;c++)	//every cell
		{	for (j=1;j<elemParams.length;j++)
			{	if ( x[c].innerHTML == elemParams[j][0] ) 
				{	x[c].bgColor = '#' + elemParams[j][1] 
					x[c].title = j + ' ' + elemParams[j][0] + '  ' + hexColorToDecColor(elemParams[j][1]) + '  ' + elemParams[j][1] 
					x[c].setAttribute('onClick','location.hash = "#color_' + elemParams[j][0] + '"; window.scrollBy(0,-15)')
				}
			}
		}
	}
}


function buildPeriodicTable()
{	
	document.writeln('<div align="center">')
	var tx = '' 
	+ '	<table border="0" cellspacing="0" cellpadding="3" id="perTable">'
	+ '	  <tr><td>H</td><td colspan="16"></td><td>He</td></tr>'
	+ '	  <tr><td>Li</td><td>Be</td><td colspan="10"></td><td>B</td><td>C</td><td>N</td><td>O</td><td>F</td><td>Ne</td></tr>'
	+ '	  <tr><td>Na</td><td>Mg</td><td colspan="10"></td><td>Al</td><td>Si</td><td>P</td><td>S</td><td>Cl</td><td>Ar</td></tr>'
	+ '	  <tr><td>K</td><td>Ca</td><td>Sc</td><td>Ti</td><td>V</td><td>Cr</td><td>Mn</td><td>Fe</td><td>Co</td><td>Ni</td><td>Cu</td><td>Zn</td>'
	+ '		  <td>Ga</td><td>Ge</td><td>As</td><td>Se</td><td>Br</td><td>Kr</td></tr>'
	+ '	  <tr><td>Rb</td><td>Sr</td><td>Y</td><td>Zr</td><td>Nb</td><td>Mo</td><td>Tc</td><td>Ru</td><td>Rh</td><td>Pd</td><td>Ag</td><td>Cd</td>'
	+ '		  <td>In</td><td>Sn</td><td>Sb</td><td>Te</td><td>I</td><td>Xe</td></tr>'
	+ '	  <tr><td>Cs</td><td>Ba</td><td>L*</td><td>Hf</td><td>Ta</td><td>W</td><td>Re</td><td>Os</td><td>Ir</td><td>Pt</td><td>Au</td><td>Hg</td>'
	+ '		  <td>Tl</td><td>Pb</td><td>Bi</td><td>Po</td><td>At</td><td>Rn</td></tr>'
	+ '	  <tr><td>Fr</td><td>Ra</td><td>A*</td><td>Rf</td><td>Db</td><td>Sg</td><td>Bh</td><td>Hs</td><td>Mt</td>'
	+ '		  <td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>'
	+ '		<tr><td colspan="18">&nbsp;</td></tr>'
	+ '	  <tr><td>&nbsp;</td><td>(L:)</td><td>La</td><td>Ce</td><td>Pr</td><td>Nd</td><td>Pm</td><td>Sm</td><td>Eu</td><td>Gd</td>'
	+ '		  <td>Tb</td><td>Dy</td><td>Ho</td><td>Er</td><td>Tm</td><td>Yb</td><td>Lu</td><td>&nbsp;</td></tr>'
	+ '	  <tr><td>&nbsp;</td><td>(A:)</td><td>Ac</td><td>Th</td><td>Pa</td><td>U</td> <td>Np</td><td>Pu</td><td>Am</td><td>Cm</td>'
	+ '		  <td>Bk</td><td>Cf</td><td>Es</td><td>Fm</td><td>Md</td><td>No</td><td>Lr</td><td>&nbsp;</td></tr>'
	+ '	</table>'
	document.writeln(tx)
	populatePeriodicTable()
	document.writeln('</div>')
	document.close()
}

function buildElementTable()
{
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th>&nbsp;</th><th>&nbsp;</th><th colspan='3'>Jmol</th><th width='10'>&nbsp;</th><th colspan='2'>Rasmol</th><th width='10'>&nbsp;</th><th colspan='2'>Rasmol<br>&#8220;CPKnew&#8221;</th>")
	document.writeln("</tr>")
	for (i=1;i<elemParams.length;i++)
	{	document.write("<tr><td><a name='color_" + elemParams[i][0] + "'></a>" + i + "</td><td>" + elemParams[i][0] + "</td><td>")
		document.write("<tt>" + hexColorToDecColor(elemParams[i][1]) + "</tt></td><td>")
		document.write(elemParams[i][1] + "</td><td bgcolor='#" + elemParams[i][1] + "'>&nbsp;&nbsp;</td>")
		document.write("<td>&nbsp;</td><td>" + elemParams[i][2] + "</td><td bgcolor='#" + elemParams[i][2] + "'>&nbsp;&nbsp;</td>")
		document.write("<td>&nbsp;</td><td>")
		if ( elemCPKnew[i] ) document.write(elemCPKnew[i] + "</td><td bgcolor='#" + elemCPKnew[i] + "'>&nbsp;&nbsp;</td>")
		else document.write("&nbsp;</td><td>&nbsp;</td>")
		document.writeln("</tr>")
	}
	document.writeln("</table>")
	document.close()
}

function buildStructTable(titleProt, lbProt, asRasmol, titleNuc, lbNuc, asDrums)
{
	document.writeln("<table border='0' cellpadding='0'>")
	document.writeln("<tr valign='top'>")
	document.writeln("<td>")
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th colspan='3'><h3>" + titleProt + "</h3></th></tr>")
	buildStructProtTable(lbProt)
	document.writeln("</table>")
    document.writeln(asRasmol + "</td>")
    document.writeln("<td width='100'>&nbsp;</td>");
    document.writeln("<td>");
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th colspan='3'><h3>" + titleNuc + "</h3></th></tr>")
	buildStructNucTable(lbNuc)
	document.writeln("</table>" + asDrums + "</td>")
	document.writeln("</tr>")
	document.writeln("</table>")
}

function buildStructProtTable(lb)
{
	for (i=0;i<structProtParams.length;i++)
	{	document.write("<tr><td>" + lb[i] + "</td><td>" + structProtParams[i] + "</td><td bgcolor='#" + structProtParams[i] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.close()
}

function buildStructNucTable(lb)
{
	for (i=0;i<structNucParams.length;i++)
	{	document.write("<tr><td>" + lb[i] + "</td><td>" + structNucParams[i] + "</td><td bgcolor='#" + structNucParams[i] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.close()
}

function buildResidueTable(titleProt1a, titleProt1b, titleProt2a, titleProt2b, asRasmol, titleNuc1a, titleNuc1b, asRasmol2)
{
	document.writeln("<table border='0' cellpadding='0'>")
	document.writeln("<tr valign='top'>")
	document.writeln("<td>")
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th>&nbsp;</th><th colspan='2'><h3>" + titleProt1a + "<br>")
	document.writeln("&#8220;" + titleProt1b + "&#8221;</h3></th><th width='15'>&nbsp;</th>")
	document.writeln("<th colspan='2'><h3>" + titleProt2a + "<br>")
	document.writeln("&#8220;" + titleProt2b + "&#8221;</h3></th></tr>")
	buildAaTable()
	document.writeln("</table>")
	document.writeln(asRasmol)
	document.writeln("</td>")
	document.writeln("<td width='100'>&nbsp;</td>")
	document.writeln("<td>");
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th>&nbsp;</th><th colspan='2'><h3>" + titleNuc1a + "<br>")
	document.writeln("&#8220;" + titleNuc1b + "&#8221;</h3></th></tr>")
	buildNtTable()
	document.writeln("</table>")
	document.writeln(asRasmol2 + "</td>")
	document.writeln("</tr>")
	document.writeln("</table>")
}

function buildAaTable()
{
	for (i=0;i<aaParams.length;i++)
	{	document.write("<tr><td>" + aaParams[i][0] + "</td><td>" + aaParams[i][1] + "</td><td bgcolor='#" + aaParams[i][1] + "'>&nbsp;&nbsp;</td>")
		document.writeln("<td>&nbsp;</td><td>" + aaParams[i][2] + "</td><td bgcolor='#" + aaParams[i][2] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.close()
}

function buildNtTable()
{
	for (i=0;i<ntParams.length;i++)
	{	document.writeln("<tr><td>" + ntParams[i][0] + "</td><td>" + ntParams[i][1] + "</td><td bgcolor='#" + ntParams[i][1] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.close()
}

function buildChainTable(titleChain1a, titleChain1b)
{
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th>" + titleChain1a + "<br>" + titleChain1b + "</th>")
	document.writeln("<th colspan='2'>ATOM</th><th width='10'>&nbsp;</th><th colspan='2'>HETATM</th></tr>")
	for (i=0;i<chainParams.length;i++)
	{	document.write("<tr><td>" + chainParams[i][0] + "</td><td>" + chainParams[i][1] + "</td>")
		document.write("<td bgcolor='#" + chainParams[i][1] + "'>&nbsp;&nbsp;</td>")
		document.write("<td>&nbsp;</td><td>" + chainParams[i][2] + "</td>")
		document.writeln("<td bgcolor='#" + chainParams[i][2] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.writeln("</table>")
	document.close()
}

function buildChargeTable(titleCharge)
{
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><th>" + titleCharge + "</th><th colspan='2'>&nbsp;</th></tr>")
	for (i=0;i<chargeParams.length;i++)
	{	document.write("<tr><td>" + chargeParams[i][0] + "</td><td>" + chargeParams[i][1] + "</td>")
		document.writeln("<td bgcolor='#" + chargeParams[i][1] + "'>&nbsp;&nbsp;</td></tr>")
	}
	document.writeln("</table>")
	document.close()
}

function buildRwbTable(t1,t2)
{
	var hhh = rwbParams.length/2
	var hh = parseInt(hhh)
	hhh = hh + (hhh - hh)*2		// +1 (odd) or +0 (even)
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><td colspan='" + hhh + "' style='text-align:left;'>" + t1 + "</td>")
	document.writeln("<td colspan='" + hh + "' style='text-align:right;'>" + t2 + "</td></tr>")
	document.write("<tr>")
	for (i=0;i<rwbParams.length;i++)
	{	document.write("<td bgcolor='#" + rwbParams[i] + "'>&nbsp;&nbsp;</td>")
	}
	document.writeln("</tr>")
	document.writeln("</table>")
	document.close()
}

function buildRainbowTable(t1,t2)
{
	var hhh = rainbowParams.length/2
	var hh = parseInt(hhh)
	hhh = hh + (hhh - hh)*2		// +1 (odd) or +0 (even)
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	document.writeln("<tr><td colspan='" + hhh + "' style='text-align:left;'>" + t1 + "</td>")
	document.writeln("<td colspan='" + hh + "' style='text-align:right;'>" + t2 + "</td></tr>")
	document.write("<tr>")
	for (i=0;i<rainbowParams.length;i++)
	{	document.write("<td bgcolor='#" + rainbowParams[i] + "'>&nbsp;&nbsp;</td>")
	}
	document.writeln("</tr>")
	document.writeln("</table>")
	document.close()
}

function buildIsoSurfaceTables(titlePos, titleNeg)
{
	document.writeln("<table border='0' cellpadding='0'>")
	document.writeln("<tr>")
	document.writeln("<td><h3>" + titlePos + "</h3>")
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	buildIsosurfaceTable(1)
	document.writeln("</table>")
	document.writeln("</td>")
	document.writeln("<td width='100'>&nbsp;</td>")
	document.writeln("<td><h3>" + titleNeg + "</h3>")
	document.writeln("<table border='0' cellspacing='0' cellpadding='4' class='colorTable'>")
	buildIsosurfaceTable(-1)
	document.writeln("</table>")
	document.writeln("</td>")
	document.writeln("</tr>")
	document.writeln("</table>")
}

function buildIsosurfaceTable(t)
{
	if (t==1)		{ k1=0; k2=isoSurfParams.length/2 }						// isosurface positive
	else if (t==-1)	{ k1=isoSurfParams.length/2; k2=isoSurfParams.length }	// isosurface negative
	else return
	document.write("<tr>")
	for (var i=k1; i<k2; i++)
	{	document.write("<th bgcolor='#" + isoSurfParams[i] + "'>&nbsp;&nbsp;</th>")
	}
	document.writeln("</tr>")
	document.close()
}
