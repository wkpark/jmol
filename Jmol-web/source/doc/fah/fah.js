
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

function viewProject(project, filename, projectname, credit, atoms, preferred, deadline, frames, code) {
  jmolScript("load ../fah/projects/" + filename + ".xyz.gz", "Fah");
  document.fahForm.infoNumber.value = project;
  document.fahForm.infoName.value = projectname;
  document.fahForm.infoCredit.value = credit;
  document.fahForm.infoAtoms.value = atoms;
  document.fahForm.infoPreferred.value = preferred;
  document.fahForm.infoDeadline.value = deadline;
  document.fahForm.infoFrames.value = frames;
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

function addProject(project, filename, projectname, credit, atoms, preferred, deadline, frames, code) {
  var varDisabled = "";
  var varOnClick = "";
  var varOnMouseOver = "";
  var varOnMouseOut = "";
  if (filename == undefined || filename == null) {
    varDisabled = "disabled='true'";
  } else {
    varOnClick =
    "onclick='viewProject(" +
    "\"" + project + "\"," +
    "\"" + filename + "\"," +
    "\"" + projectname + "\"," +
    "\"" + credit + "\"," +
    "\"" + atoms + "\"," +
    "\"" + preferred + "\"," +
    "\"" + deadline + "\"," +
    "\"" + frames + "\"," +
    "\"" + code + "\")'";
  }
  if (projectname == undefined || projectname == null) {
  } else {
    varOnMouseOver = "onMouseOver='displayStatus(\"" + projectname + "\");return true'";
    varOnMouseOut = "onMouseout='clearStatus();return true'";
  }
  if (projectname == undefined || projectname == null) {
  } else {
    varInput = "<input" +
               " type='button'" +
               " value='" + project + "'" +
               " " + varDisabled +
               " " + varOnClick +
               " " + varOnMouseOver +
               " " + varOnMouseOut +
               " />";
    document.writeln(varInput);
  }
}

function addRowStanfordLogo() {
  document.writeln("<tr>");
  document.writeln(  "<td align='center' colspan='2'>");
  document.writeln(    "<a href='http://folding.stanford.edu'>");
  document.writeln(      "<img src='FAHlogoButton.jpg' alt='fah-logo' border='0'/>");
  document.writeln(    "</a>");
  document.writeln(  "</td>");
  document.writeln("</tr>");
}

function addRowProjectNumber(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoNumber' id='infoNumber'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectName(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoName' id='infoName'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectCredit(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoCredit' id='infoCredit'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectAtoms(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoAtoms' id='infoAtoms'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectPreferred(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoPreferred' id='infoPreferred'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectDeadline(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoDeadline' id='infoDeadline'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectFrames(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoFrames' id='infoFrames'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowProjectCode(label) {
  document.writeln("<tr>");
  document.writeln(  "<td><label>" + label + ":</label></td>");
  document.writeln(  "<td><input type='text' name='infoCode' id='infoCode'");
  document.writeln(  "  size='50' readonly='readonly'/></td>");
  document.writeln("</tr>");
}

function addRowButtons(projectDescription, hideSolvent, files_projects) {
  document.writeln("<tr>");
  document.writeln(  "<td align='left' colspan='2'>");
  document.writeln(    "<br/>");
  document.writeln(    "<input type='button' value='" + projectDescription + "'");
  document.writeln(    "  onclick='showProjectDescription()'");
  document.writeln(    "  onMouseOver='window.status=\"http://vspx27.stanford.edu/cgi-bin/allprojects\"'");
  document.writeln(    "  onMouseout='window.status=\" \"'/>");
  document.writeln(    "<input type='button' value='" + hideSolvent + "'");
  document.writeln(    "  onclick='hideSolvent()'/>");
  document.writeln(    "<input type='button' value='XML'");
  document.writeln(    "  onclick='showXMLFile()'/>");
  document.writeln(    "<small>");
  document.writeln(      files_projects);
  document.writeln(    "</small>");
  document.writeln(  "</td>");
  document.writeln("</tr>");
}

function addRowCurrentXyz(text) {
  document.writeln("<tr>");
  document.writeln(  "<td align='left' colspan='2'>");
  document.writeln(    "<small><i>");
  document.writeln(      text);
  document.writeln(    "</i></small>");
  document.writeln(  "</td>");
  document.writeln("</tr>");
}
