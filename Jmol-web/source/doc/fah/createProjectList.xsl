<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="text" indent="no"/>
  
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="fah_projects">
    <xsl:text><![CDATA[function createAllProjects() {]]>
</xsl:text>
    <xsl:text><![CDATA[document.writeln("<table width='100%' border=0 cellpadding=0><tr>");]]>
</xsl:text>
    <xsl:for-each select="fah_proj" >
      <xsl:value-of select="." />
      <xsl:if test="@number">
        <xsl:if test="@name">
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
          <xsl:text>');
</xsl:text>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
    <xsl:text><![CDATA[document.writeln("</tr></table>");]]>
</xsl:text>
    <xsl:text><![CDATA[document.writeln("<br/>");]]>
</xsl:text>
    <xsl:text><![CDATA[}]]>
</xsl:text>
  </xsl:template>

</xsl:stylesheet>
