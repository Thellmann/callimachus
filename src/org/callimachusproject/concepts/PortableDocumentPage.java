package org.callimachusproject.concepts;

import java.math.BigInteger;
import java.util.Set;

import org.openrdf.annotations.Iri;

/** Single page in a PDF file */
@Iri("http://callimachusproject.org/rdf/2009/framework#PortableDocumentPage")
public interface PortableDocumentPage {
	/** Page position within the document (first page is page 1) */
	@Iri("http://callimachusproject.org/rdf/2009/framework#pageNumber")
	BigInteger getCalliPageNumber();
	/** Page position within the document (first page is page 1) */
	@Iri("http://callimachusproject.org/rdf/2009/framework#pageNumber")
	void setCalliPageNumber(BigInteger calliPageNumber);

	/** The depiction property is a relationship between a thing and an Image that depicts it */
	@Iri("http://xmlns.com/foaf/0.1/depiction")
	Object getFoafDepiction();
	/** The depiction property is a relationship between a thing and an Image that depicts it */
	@Iri("http://xmlns.com/foaf/0.1/depiction")
	void setFoafDepiction(Object foafDepiction);

	@Iri("http://www.w3.org/2000/01/rdf-schema#label")
	Set<CharSequence> getRdfsLabel();
	@Iri("http://www.w3.org/2000/01/rdf-schema#label")
	void setRdfsLabel(Set<CharSequence> rdfsLabel);

	/** A related resource that is referenced, cited, or otherwise pointed to by the described resource. */
	@Iri("http://purl.org/dc/terms/references")
	Set<Object> getDctermsReferences();
	/** A related resource that is referenced, cited, or otherwise pointed to by the described resource. */
	@Iri("http://purl.org/dc/terms/references")
	void setDctermsReferences(Set<?> dctermsReferences);

}
