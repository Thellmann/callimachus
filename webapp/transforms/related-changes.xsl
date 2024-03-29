<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:import href="folder-changes.xsl" />
    <xsl:template match="/">
        <html>
            <head>
                <title>Related Changes</title>
                <link rel="help" href="../../callimachus-for-web-developers#System_menu" target="_blank" title="Help" />
            </head>
            <body>
                <h1>Related Changes</h1>
                <xsl:apply-templates />
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
