<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="xsl d xhtml">

<xsl:import href="/callimachus/editor/docbook2xhtml.xsl" />

<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

<xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#">
    <head>
        <title><xsl:value-of select="//d:title[1]" /></title>
        <link rel="edit" href="?edit" />
        <link rel="describedby" href="?describe" />
        <link rel="version-history" href="?history" />
    </head>
    <body>
        <article>
            <xsl:apply-templates select="d:article/*" />
        </article>
    </body>
    </html>
</xsl:template>

</xsl:stylesheet>