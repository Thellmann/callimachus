<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p=     "http://www.w3.org/ns/xproc"
        xmlns:c=     "http://www.w3.org/ns/xproc-step"
        xmlns:l     ="http://xproc.org/library"
        xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" select="resolve-uri('/')" />
    <p:option name="query" select="''" />

    <p:import href="page-layout-html.xpl" />

    <calli:page-layout-html>
        <p:with-option name="target"  select="$target" />
        <p:with-option name="query" select="$query" />
    </calli:page-layout-html>
</p:pipeline>
