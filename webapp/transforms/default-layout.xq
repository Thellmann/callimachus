xquery version "1.0" encoding "utf-8";

import module namespace calli = "http://callimachusproject.org/rdf/2009/framework#" at "/callimachus/layout-functions.xq";

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
<head>
    <meta name="viewport" content="width=device-width,height=device-height,initial-scale=1.0,target-densityDpi=device-dpi"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <link rel="icon" href="/favicon.ico" />
    {calli:styles-href(<link rel="stylesheet" />)}
    {calli:scripts-src(<script type="text/javascript" />)}
    <!--[if lt IE 9]>
        <script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
    {calli:head-nodes()}
</head>
<body>
    {calli:body-attributes()}
    <div class="until-navbar-large">
        <nav class="navbar navbar-default navbar-static-top hidden-iframe">
            <header class="container">
                <div class="pull-left">
                    {calli:home-href(<a class="navbar-brand">Callimachus</a>)}
                </div>
                {calli:login-href(<a class="btn btn-default navbar-btn pull-right hidden-login">Sign in</a>)}
                <div class="btn-group pull-right hidden-logout">
                    <button type="button" class="btn btn-default navbar-btn dropdown-toggle" data-toggle="dropdown">
                        <span class="sr-only">Main menu</span>
                        <span class="glyphicon glyphicon-align-justify"></span>
                    </button>
                    <ul class="dropdown-menu" role="menu">
                        <li>{calli:folder-href(<a>Home folder</a>)}</li>
                        <li>{calli:changes-href(<a>Recent changes</a>)}</li>
                        <li class="divider"></li>
                        <li>{calli:callimachus-about-href(<a>About Callimachus</a>)}</li>
                        <li>{calli:callimachus-getting-started-href(<a>Getting started</a>)}</li>
                        <li>{calli:callimachus-feedback-href(<a>Send feedback</a>)}</li>
                        <li class="divider"></li>
                        {calli:head-links(<li><a /></li>,<li class="divider" />)}
                        <li>{calli:whatlinkshere-href(<a>What links here</a>)}</li>
                        <li>{calli:relatedchanges-href(<a>Related changes</a>)}</li>
                        <li>{calli:permissions-href(<a>Permissions</a>)}</li>
                        <li>{calli:introspect-href(<a>Introspect resource</a>)}</li>
                        <li><a href="javascript:print()">Print this page</a></li>
                        <li class="divider"></li>
                        <li>{calli:profile-href(<a>Account</a>)}</li>
                        <li>{calli:logout-href(<a>Sign out</a>)}</li>
                    </ul>
                </div>
                <div class="col-xs-4 pull-right hidden-logout">{calli:lookup-form('Lookup...')}</div>
            </header>
        </nav>
        <div class="container">
            <ol class="breadcrumb navbar-left hidden-iframe">{calli:breadcrumb-links(<li><a/></li>, <li class="active"/>)}</ol>
            {calli:activate-nav(<nav class="nav-tabs clearfix hidden-logout hidden-iframe">
                <ul class="nav nav-tabs navbar-right" style="border-bottom:none">
                    <li>{calli:view-href(<a tabindex="1" onclick="location.replace(href);return false">View</a>)}</li>
                    <li>{calli:edit-href(<a tabindex="2" onclick="location.replace(href);return false">Edit</a>)}</li>
                    <li>{calli:discussion-href(<a tabindex="3" onclick="location.replace(href);return false">Discussion</a>)}</li>
                    <li>{calli:describe-href(<a tabindex="4" onclick="location.replace(href);return false">Describe</a>)}</li>
                    <li>{calli:history-href(<a tabindex="5" onclick="location.replace(href);return false">History</a>)}</li>
                </ul>
            </nav>)}
            <div class="clearfix" />
            <div class="tab-content">
                {calli:body-hgroup()}
                {calli:body-sidebar(<div class="sidebar" />)}
                {calli:error-alert(<div class="alert alert-danger alert-dismissable alert-block">
                    <button type="button" class="close" data-dismiss="alert">×</button>
                    <h4>Oops!</h4>
                </div>)}
                {calli:body-nodes()}
            </div>
        </div>
    </div>

    <div class="navbar navbar-large navbar-relative-bottom hidden-iframe">
        <footer class="navbar-inner">
            <div class="container">
                {calli:generator-p(<p class="navbar-right navbar-text" />)}
                {calli:lastmod-time(<p class="navbar-text">This resource was last modified at <time class="datetime-local"/></p>)}
            </div>
        </footer>
    </div>
</body>
</html>
