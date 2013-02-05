PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX void:<http://rdfs.org/ns/void#>
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX prov:<http://www.w3.org/ns/prov#>
PREFIX audit:<http://www.openrdf.org/rdf/2012/auditing#>

DELETE {
    ?folder calli:describedby ?describedby
} WHERE {
    ?folder a <types/Folder>; calli:describedby ?describedby
    FILTER EXISTS {
        ?folder calli:describedby ?describedby2
        FILTER (?describedby2 < ?describedby)
    }
};

INSERT {
    </> calli:reader </auth/groups/public>
} WHERE {
    FILTER NOT EXISTS {
        </> calli:reader </auth/groups/public>
    }
};

DELETE {
    </layout/template.xsl> ?p ?o
} WHERE {
    </layout/template.xsl> ?p ?o
};

DELETE {
    </> a <http://callimachusproject.org/rdf/2009/framework#Alias>
} WHERE {
    </> a <http://callimachusproject.org/rdf/2009/framework#Alias>, calli:Folder
};

INSERT {
    ?graph a <http://www.openrdf.org/rdf/2012/auditing#ObsoleteBundle>
} WHERE {
    ?graph a <http://www.openrdf.org/rdf/2009/auditing#ObsoleteTransaction>
    FILTER NOT EXISTS { ?graph a <http://www.openrdf.org/rdf/2012/auditing#ObsoleteBundle> }
};
