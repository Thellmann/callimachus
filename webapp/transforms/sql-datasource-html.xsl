<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    exclude-result-prefixes="xhtml sparql">
    <xsl:output indent="no" method="xml" />
    <xsl:template match="/">
        <xsl:if test="*">
            <html>
                <head>
                    <title>SQL Results</title>
                    <link rel="help" href="../../callimachus-for-web-developers" target="_blank" title="Help" />
                    <style>
                        ul.properties { margin-top: 0px; }
                        li.triple { list-style-type: none }
                        .plain { font-size: large; }
                        .bnode, .plain { font-family: monospace; white-space: pre-wrap; }
                        .typed { color: magenta; }
                        .datatype, .language { color: gray; }
                        .predicate { color: darkgreen; }
                    </style>
                </head>
                <body>
                    <h1>SQL Results</h1>
                    <div>
                        <xsl:apply-templates />
                    </div>
                </body>
            </html>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sparql:sparql">
        <table id="sparql">
            <xsl:apply-templates select="*" />
        </table>
    </xsl:template>
    <xsl:template match="sparql:head">
        <thead id="head">
            <xsl:apply-templates select="sparql:variable" />
        </thead>
    </xsl:template>
    <xsl:template match="sparql:variable">
        <th>
            <xsl:value-of select="@name" />
        </th>
    </xsl:template>
    <xsl:template match="sparql:boolean">
        <tbody id="boolean">
            <tr>
                <td>
                    <xsl:value-of select="text()" />
                </td>
            </tr>
        </tbody>
    </xsl:template>
    <xsl:template match="sparql:results">
        <tbody id="results">
            <xsl:apply-templates select="sparql:result" />
        </tbody>
    </xsl:template>
    <xsl:template match="sparql:result">
        <xsl:variable name="current" select="."/> 
        <tr class="result">
            <xsl:for-each select="../../sparql:head[1]/sparql:variable">
                <xsl:variable name="name" select="@name"/> 
                <td>
                    <xsl:apply-templates select="$current/sparql:binding[@name=$name]" /> 
                </td>
            </xsl:for-each>
        </tr>
    </xsl:template>
    <xsl:template match="sparql:binding">
        <xsl:apply-templates select="*" />
    </xsl:template>
    <xsl:template match="sparql:uri">
        <a href="{text()}" class="uri">
            <xsl:value-of select="text()"/>
        </a>
        <xsl:text> </xsl:text>
    </xsl:template>
    <xsl:template match="sparql:bnode">
        <a class="bnode" resource="_:{text()}" name="{text()}">
            <xsl:text>_:</xsl:text>
            <xsl:value-of select="text()" />
        </a>
    </xsl:template>
    <xsl:template match="sparql:literal">
        <span class="plain literal">
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template match="sparql:literal[@datatype and @datatype!='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
        <span class="typed literal">
            <xsl:attribute name="datatype">
                <xsl:value-of select="@datatype" />
            </xsl:attribute>
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template match="sparql:literal[@xml:lang]">
        <span class="plain literal">
            <xsl:attribute name="xml:lang">
                <xsl:value-of select="@xml:lang" />
            </xsl:attribute>
            <xsl:value-of select="text()" />
        </span>
    </xsl:template>
    <xsl:template
        match="sparql:literal[@datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral']">
        <span class="typed literal" datatype="rdf:XMLLiteral">
            <xsl:value-of disable-output-escaping="yes" select="text()" />
        </span>
    </xsl:template>
</xsl:stylesheet>
