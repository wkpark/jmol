  <xsl:template match="fah_projects">
    <![CDATA[
      var doc = document;

      function displayStatus(projectname) {
        window.status = "Project " + projectname;
      }

      function clearStatus() {
        window.status = " ";
      }

      function showProjectDescription() {
        window.open("http://vspx27.stanford.edu/cgi-bin/allprojects#" + document.fahForm.infoNumber.value);
      }

      function hideSolvant() {
        var varScript;
        varScript =
          "restrict (not substructure(\"[O][C](N)N\")) or substructure(\"[0]([C](N)N)[*]\")\n" +
          "restrict (not substructure(\"[N][C]C\")) or substructure(\"[N]([C]C)[*]\")\n" +
          "restrict (not substructure(\"C(Cl)(Cl)Cl\"))\n" +
          "restrict (not substructure(\"CO\")) or substructure(\"C(O)[*]\")\n" +
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
        doc.open();
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
          doc.writeln(varInput);
        }
        doc.close();
      }

      doc.writeln("<table width=100% border=0 cellpadding=0><tr>");
    ]]>
    <xsl:for-each select="fah_proj" >
      <xsl:value-of select="." />
      <xsl:if test="@number">
        <xsl:if test="@name">
          <xsl:text>
</xsl:text>
          <xsl:text>addProject('</xsl:text>
          <!-- Project number -->
          <xsl:value-of select="@number" />
          <xsl:text>',</xsl:text>
          <!-- Project file name -->
          <xsl:choose>
            <xsl:when test="@file = 'y'">
              <xsl:text>'p</xsl:text>
              <xsl:value-of select="@number" />
              <xsl:text>'</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>null</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>,'</xsl:text>
          <!-- Project name -->
          <xsl:value-of select="@name" />
          <xsl:text>','</xsl:text>
          <!-- Credit -->
          <xsl:choose>
            <xsl:when test="@credit">
              <xsl:value-of select="@credit" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>','</xsl:text>
          <!-- Atoms -->
          <xsl:choose>
            <xsl:when test="@atoms">
              <xsl:value-of select="@atoms" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>','</xsl:text>
          <!-- Preferred -->
          <xsl:choose>
            <xsl:when test="@preferred">
              <xsl:value-of select="@preferred" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>','</xsl:text>
          <!-- Deadline -->
          <xsl:choose>
            <xsl:when test="@deadline">
              <xsl:value-of select="@deadline" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>','</xsl:text>
          <!-- Frames -->
          <xsl:choose>
            <xsl:when test="@frames">
              <xsl:value-of select="@frames" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>','</xsl:text>
          <!-- Code -->
          <xsl:choose>
            <xsl:when test="@code">
              <xsl:value-of select="@code" />
            </xsl:when>
          </xsl:choose>
          <xsl:text>');</xsl:text>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
    <![CDATA[
      doc.writeln("</tr></table>")
      doc.writeln("<br />");
    ]]>
  </xsl:template>
  
  <xsl:template match="fah_count_projects">
    <xsl:value-of select="count(//fah_projects/fah_proj[@name])"/>
  </xsl:template>
  
  <xsl:template match="fah_count_files">
    <xsl:value-of select="count(//fah_projects/fah_proj[@file])"/>
  </xsl:template>
