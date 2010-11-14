<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#">
	<xsl:output method="xml" encoding="UTF-8"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>
					<xsl:value-of select="/sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='label']/*" />
					<xsl:text> Discussion</xsl:text>
				</title>
				<style>
					.thread { padding: 0px; }
					.note { list-style: none; margin-bottom: 1em; padding: 0.5ex; }
					.note div.ui-corner-top { font-size: small; padding: 1ex; height: 16px; line-height: 16px; vertical-align: middle; }
					.note .comment { padding: 0px 1ex; }
					.note .post { padding: 1ex; }
				</style>
			</head>
			<body>
				<h1>
					<xsl:value-of select="/sparql:sparql/sparql:results/sparql:result[1]/sparql:binding[@name='label']/*" />
					<xsl:text> Discussion</xsl:text>
				</h1>
				<p>The purpose of this forum is to provide space for editors to discuss changes, post reminders of editorial work still to be done, or warnings in the event that future changes that might be made. This should not be used by editors for their personal views on a subject.</p>
			<ul class="thread">
				<xsl:apply-templates select="sparql:sparql/sparql:results/sparql:result[sparql:binding/@name='note']" />
				<li class="note authenticated">
					<div class="ui-state-active ui-corner-top" >
						<span class="ui-icon ui-icon-comment" style="float: left"></span>
						<xsl:text>Post a new note</xsl:text>
					</div>
					<div class="post ui-widget-content ui-corner-bottom">
						<form action="?discussion" method="POST">
							<textarea class="auto-expand" name="note"></textarea>
							<div><button type="submit">Send</button><button type="clear">Discard</button></div>
						</form>
					</div>
				</li>
			</ul>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="sparql:result[sparql:binding/@name='note']">
		<li class="note">
			<div class="ui-state-default ui-corner-top" >
				<span class="abbreviated datetime-locale" style="float: right">
					<xsl:value-of select="sparql:binding[@name='modified']/*" />
				</span>
				<span class="ui-icon ui-icon-comment" style="float: left"></span>
				<a href="{sparql:binding[@name='user']/*}">
					<xsl:value-of select="sparql:binding[@name='name']/*" />
				</a>
			</div>
			<div class="comment ui-widget-content ui-corner-bottom">
				<pre class="wiki">
					<xsl:value-of select="sparql:binding[@name='note']/*" />
				</pre>
			</div>
		</li>
	</xsl:template>
</xsl:stylesheet>
