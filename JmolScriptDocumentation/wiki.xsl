<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="jmolcmd">
        <xsl:choose>
            <xsl:when test="contains(normalize-space(cmdname), ' ')">
                <xsl:variable name="commandName" select="substring-before(normalize-space(cmdname), ' ')"/>
                <xsl:call-template name="outputCommand">
                  <xsl:with-param name="commandName" select="$commandName"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="commandName" select="normalize-space(cmdname)"/>
                <xsl:call-template name="outputCommand">
                  <xsl:with-param name="commandName" select="$commandName"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="outputCommand">
        <xsl:param name="commandName"/>
        <xsl:document href="{concat('Script', concat(translate(substring($commandName,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'), substring($commandName,2)))}" method="text">
= <xsl:value-of select="$commandName"/> =

<xsl:value-of select="cmddescription"/>

<!-- xsl:apply-templates select="cmdexamples"/>
<xsl:apply-templates select="cmddefinitions"/ -->
        </xsl:document>
    </xsl:template>        
    
    <xsl:template match="cmdexamples">
        <variablelist>
            <title>Examples</title>
            <xsl:for-each select="cmdexample">
                <varlistentry>
                    <term>
                        <command><xsl:value-of select="cmdoption"/></command>
                    </term>
                    <listitem>
                        <para>
                            <xsl:value-of select="cmdlistidescription"/>
                        </para>
                    </listitem>
                </varlistentry>
            </xsl:for-each>
        </variablelist>
    </xsl:template>
						
    <xsl:template match="cmddefinitions">
        <variablelist>
            <title>Definitions</title>
            <xsl:for-each select="cmddef">
                <varlistentry>
                    <term>
                        <option><xsl:value-of select="defkey"/></option>
                    </term>
                    <listitem>
                        <para>
                            <xsl:value-of select="defdata"/>
                        </para>
                    </listitem>
                </varlistentry>
            </xsl:for-each>
        </variablelist>
    </xsl:template>

</xsl:stylesheet>


