
function displayStatus(projectname) {
  window.status = "Project " + projectname;
}

function clearStatus() {
  window.status = " ";
}

function showProjectDescription() {
  window.open("http://vspx27.stanford.edu/cgi-bin/allprojects#" + document.fahForm.infoNumber.value);
}

function hideSolvent() {
  var varScript;
  varScript =
    "restrict (not substructure(\"[O][C](N)N\")) or substructure(\"[O]([C](N)N)[*]\")\n" +
    "restrict (not substructure(\"[N][C]C\")) or substructure(\"[N]([C]C)[*]\")\n" +
    "restrict (not substructure(\"C(Cl)(Cl)Cl\"))\n" +
    "restrict (not substructure(\"CO\"))\n" +
    "restrict (not substructure(\"O\"))\n" +
    "center selected\n" +
    "select none";
  jmolScript(varScript, "Fah");
}

function showXMLFile() {
  window.open("./fah-projects.xml");
}

function createFahPage(select,
                       project, name, credit, atoms, preferred, deadline, frames, code,
                       projectDescription, hideSolvent, availableFiles,
                       findMissing, missingProjects) {
  document.writeln("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
  document.writeln(" <tr>");
  document.writeln("  <td>");
  document.writeln(    select + " : ");
  document.writeln("   <select onchange='showProjectInfo(this.value)'>");
  document.writeln("    <option value=''></option>");
  createAllProjects();
  document.writeln("   </select>");
  document.writeln("   <br/>");
  document.writeln("  </td>");
  document.writeln(" </tr>");
  document.writeln(" <tr>");
  document.writeln("  <td>");
  document.writeln("   <table border='0' cellpadding='0' cellspacing='0' width='100%'>");
  document.writeln("    <tr>");
  document.writeln("     <td>");
  jmolApplet(350, "", "Fah");
  document.writeln("     </td>");
  document.writeln("     <td valign='top'>");
  document.writeln("      <table border='0' cellpadding='0' cellspacing='0' width='100%'>");
  document.writeln("       <tr>");
  document.writeln("        <td align='center' colspan='2'>");
  document.writeln("         <a href='http://folding.stanford.edu'>");
  document.writeln("          <img src='FAHlogoButton.jpg' alt='fah-logo' border='0'/>");
  document.writeln("         </a>");
  document.writeln("        </td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + project + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoNumber' id='infoNumber'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + name + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoName' id='infoName'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + credit + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoCredit' id='infoCredit'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + atoms + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoAtoms' id='infoAtoms'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + preferred + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoPreferred' id='infoPreferred'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + deadline + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoDeadline' id='infoDeadline'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + frames + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoFrames' id='infoFrames'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("       <tr>");
  document.writeln("        <td><label>" + code + " :</label></td>");
  document.writeln("        <td><input type='text' name='infoCode' id='infoCode'");
  document.writeln("                   size='50' readonly='readonly'/></td>");
  document.writeln("       </tr>");
  document.writeln("      </table>");
  document.writeln("     </td>");
  document.writeln("    </tr>");
  document.writeln("   </table>");
  document.writeln("  </td>");
  document.writeln(" </tr>");
  document.writeln(" <tr>");
  document.writeln("  <td align='left'>");
  document.writeln("   <input type='button' value='" + projectDescription + "'");
  document.writeln("          onclick='showProjectDescription()'");
  document.writeln("          onMouseOver='window.status=\"http://vspx27.stanford.edu/cgi-bin/allprojects\"'");
  document.writeln("          onMouseout='window.status=\" \"'/>");
  document.writeln("   <input type='button' value='" + hideSolvent + "'");
  document.writeln("          onclick='hideSolvent()'/>");
  document.writeln("   <input type='button' value='XML'");
  document.writeln("          onclick='showXMLFile()'/>");
  document.writeln("   <small>" + availableFiles + "</small>");
  document.writeln("  </td>");
  document.writeln(" </tr>");
  document.writeln(" <tr>");
  document.writeln("  <td align='center'>");
  document.writeln("   <br/>");
  document.writeln("   <small><i>" + findMissing + "</i></small>");
  document.writeln("  </td>");
  document.writeln(" </tr>");
  document.writeln(" <tr>");
  document.writeln("  <td>");
  document.writeln("   <br/>");
  document.writeln("   <small>");
  document.writeln("    <i><u>" + missingProjects + " :</u></i>");
  createMissingProjects();
  document.writeln("   </small>");
  document.writeln("  </td>");
  document.writeln(" </tr>");
  document.writeln("</table>");
}

function addProject(project, filename, projectname,
                    credit, atoms, preferred, deadline, frames, code) {
  if (projectname != undefined && projectname !== null && filename != undefined && filename != null) {
    var varValue = "" + project + ";" + filename + ";" + projectname + ";";
    if (credit != undefined && credit != null) {
      varValue = varValue + credit;
    }
    varValue = varValue + ";";
    if (atoms != undefined && atoms != null) {
      varValue = varValue + atoms;
    }
    varValue = varValue + ";";
    if (preferred != undefined && preferred != null) {
      varValue = varValue + preferred;
    }
    varValue = varValue + ";";
    if (deadline != undefined && deadline != null) {
      varValue = varValue + deadline;
    }
    varValue = varValue + ";";
    if (frames != undefined && frames != null) {
      varValue = varValue + frames;
    }
    varValue = varValue + ";";
    if (code != undefined && code != null) {
      varValue = varValue + code;
    }
    varOption = "<option value='" + varValue + "'>" + projectname + "</option>";
    document.writeln(varOption);
  }
}

function addMissingProject(name) {
  document.writeln(" " + name + ",");
}

function showProjectInfo(value) {
  var varArray = value.split(";");
  jmolScript("load ../fah/projects/" + varArray[1] + ".xyz.gz", "Fah");
  document.fahForm.infoNumber.value = varArray[0];
  document.fahForm.infoName.value = varArray[2];
  document.fahForm.infoCredit.value = varArray[3];
  document.fahForm.infoAtoms.value = varArray[4];
  document.fahForm.infoPreferred.value = varArray[5];
  document.fahForm.infoDeadline.value = varArray[6];
  document.fahForm.infoFrames.value = varArray[7];
  var code = varArray[8];
  if (code == "A") {
    document.fahForm.infoCode.value = "Amber";
  } else if (code == "DG") {
    document.fahForm.infoCode.value = "Double Gromacs";
  } else if (code == "G") {
    document.fahForm.infoCode.value = "Gromacs";
  } else if (code == "Q") {
    document.fahForm.infoCode.value = "QMD";
  } else if (code == "T") {
    document.fahForm.infoCode.value = "Tinker";
  } else {
    document.fahForm.infoCode.value = code;
  }
}
