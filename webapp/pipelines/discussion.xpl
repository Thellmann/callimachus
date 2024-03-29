<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:calli ="http://callimachusproject.org/rdf/2009/framework#"
    xmlns:l="http://xproc.org/library">
    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="target" required="true"  />
    <p:import href="page-layout-html.xpl" />

    <p:load>
        <p:with-option 
            name="href" 
            select="concat('../queries/discussion.rq?results&amp;target=', encode-for-uri($target))"/>
    </p:load>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/discussion.xhtml" />
        </p:input>
    </p:xslt>
    <calli:page-layout-html query="discussion">
        <p:with-option name="target" select="$target"  />
    </calli:page-layout-html>
</p:pipeline>
