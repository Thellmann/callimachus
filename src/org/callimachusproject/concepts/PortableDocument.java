package org.callimachusproject.concepts;

import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.annotations.Iri;

/** PDF file */
@Iri("http://callimachusproject.org/rdf/2009/framework#PortableDocument")
public interface PortableDocument {
	public static final String HAS_PAGE = "http://callimachusproject.org/rdf/2009/framework#hasPage";

	/** Page of the document */
	@Iri(HAS_PAGE)
	Set<PortableDocumentPage> getCalliHasPage();
	/** Page of the document */
	@Iri(HAS_PAGE)
	void setCalliHasPage(Set<? extends PortableDocumentPage> calliHasPage);

	/** The depiction property is a relationship between a thing and an Image that depicts it */
	@Iri("http://xmlns.com/foaf/0.1/depiction")
	Object getFoafDepiction();
	/** The depiction property is a relationship between a thing and an Image that depicts it */
	@Iri("http://xmlns.com/foaf/0.1/depiction")
	void setFoafDepiction(Object foafDepiction);

	/** A name given to the resource. */
	@Iri("http://purl.org/dc/terms/title")
	CharSequence getDctermsTitle();
	/** A name given to the resource. */
	@Iri("http://purl.org/dc/terms/title")
	void setDctermsTitle(CharSequence dctermsTitle);

	/** Date of creation of the resource. */
	@Iri("http://purl.org/dc/terms/created")
	XMLGregorianCalendar getDctermsCreated();
	/** Date of creation of the resource. */
	@Iri("http://purl.org/dc/terms/created")
	void setDctermsCreated(XMLGregorianCalendar dctermsCreated);

}
