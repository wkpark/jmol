<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" omit-xml-declaration="yes"/>

    <xsl:template match="br">
        <xsl:text>; </xsl:text>
    </xsl:template>
    
    <xsl:template match="jmolcmd">
        <section>
            <title>
                <xsl:value-of select="cmdname"/>
            </title>
            <para>
                <xsl:value-of select="cmddescription"/>
            </para>
            <xsl:apply-templates select="cmdexamples"/>
            <xsl:apply-templates select="cmddefinitions"/>
        </section>
    </xsl:template>
						
    <xsl:template match="cmdexamples">
        <xsl:if test="cmdexample">
            <variablelist>
                <title>Syntax</title>
                <xsl:for-each select="cmdexample">
                    <varlistentry>
                        <term>
                            <command><xsl:value-of select="cmdoption"/></command>
                        </term>
                        <listitem>
                            <para>
                                <xsl:value-of select="cmdlistidescription"/>
                            </para>
                            <xsl:for-each select="cmdscript">
                                <example>
                                  <title><xsl:value-of select="../cmdoption"/></title>
                                  <programlisting>
                                  <xsl:apply-templates select="."/>
                                  </programlisting>
                                </example>
                            </xsl:for-each>
                        </listitem>
                    </varlistentry>
                </xsl:for-each>
            </variablelist>
        </xsl:if>
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


