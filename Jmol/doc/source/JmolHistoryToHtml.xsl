<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" indent="yes"/>

<xsl:template match="/" >
<html>
<head>
<title>Jmol History</title>
</head>
<body bgcolor="#ffffff">
<center>
<img src="../images/Jmol_logo.jpg" alt="Jmol logo" />
</center>

<h2>List of changes to Jmol:</h2>

<p>Contributors:</p>
<blockquote><xsl:text>
</xsl:text>
<xsl:for-each select="history/contributors/contributor" >
<xsl:value-of select="@id" /> = <xsl:value-of select="name" />
(<a href="mailto:{email}"><xsl:value-of select="email" /></a>)<br/><xsl:text>
</xsl:text>
</xsl:for-each>
</blockquote>

<xsl:for-each select="history/changes" >
<h3><xsl:value-of select="@version" /></h3>
<ul>
<xsl:for-each select="change" >
<li>
<xsl:value-of select="." /><xsl:if test="@contributor"> (<xsl:value-of select="@contributor" />)</xsl:if>
</li>
</xsl:for-each>
</ul>
</xsl:for-each>

</body>
</html>

</xsl:template>

</xsl:stylesheet>
