package org.callimachusproject.behaviours;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.util.PDFTextStripper;
import org.callimachusproject.concepts.PortableDocument;
import org.callimachusproject.concepts.PortableDocumentPage;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PortableDocumentSupport implements PortableDocument,
		CalliObject, FileObject {

	private static final Pattern PAGE_EQ = Pattern.compile("page=(\\d+)");
	private static final Pattern NAMEDDEST_EQ = Pattern
			.compile("nameddest=([^#=\\&]+)");
	private static final DatatypeFactory df;
	static {
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	private final Logger logger = LoggerFactory.getLogger(PortableDocumentSupport.class);

	public BufferedImage GetThumbnail(String pageID) throws IOException {
		InputStream in = this.openInputStream();
		PDDocument doc = PDDocument.load(in);
		try {
			PDPage page = getPDPage(pageID, doc);
			return page.convertToImage(BufferedImage.TYPE_INT_RGB, 48);
		} finally {
			doc.close();
			in.close();
		}
	}

	public Reader GetPageText(String pageID) throws IOException {
		final InputStream in = this.openInputStream();
		final PDDocument doc = PDDocument.load(in);
		try {
			PDFTextStripper stripper = new PDFTextStripper();
			int num = getPageNumber(pageID, doc);
			if (num > 0 && pageID != null && pageID.length() > 0) {
				stripper.setStartPage(num);
				stripper.setEndPage(num);
			}
			StringWriter writer = new StringWriter();
			stripper.writeText(doc, writer);
			return new StringReader(writer.toString());
		} finally {
			doc.close();
			in.close();
		}
	}

	public void PutPdf(InputStream stream) throws IOException,
			RepositoryException {
		OutputStream out = this.openOutputStream();
		try {
			ChannelUtil.transfer(stream, out);
		} finally {
			out.close();
		}
		readMetadata();
		this.touchRevision();
	}

	private void readMetadata() throws IOException, RepositoryException {
		InputStream in = this.openInputStream();
		PDDocument doc = PDDocument.load(in);
		try {
			PDDocumentInformation info = doc.getDocumentInformation();
			storeDocumentInformation(info);
			PDDocumentCatalog cat = doc.getDocumentCatalog();
			storePageMetadata(cat, getPageIDs(doc));
		} finally {
			doc.close();
			in.close();
		}
	}

	private String[] getPageIDs(PDDocument doc) throws IOException {
		String[] ids = new String[doc.getNumberOfPages()];
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		if (cat.getNames() != null) {
			PDDestinationNameTreeNode dests = cat.getNames().getDests();
			if (dests != null) {
				readDestinations(dests, ids);
			}
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == null) {
				ids[i] = "page=" + Integer.toString(1 + i);
			}
		}
		return ids;
	}

	private void readDestinations(PDNameTreeNode dests, String[] ids)
			throws IOException {
		Map<String, COSObjectable> names = dests.getNames();
		if (names != null) {
			for (Map.Entry<String, COSObjectable> e : names.entrySet()) {
				if (e.getValue() instanceof PDPageDestination) {
					PDPageDestination dest = (PDPageDestination) e.getValue();
					int num = dest.findPageNumber();
					if (num > 0) {
						ids[num - 1] = e.getKey();
					}
				}
			}
		}
		List<PDNameTreeNode> kids = dests.getKids();
		if (kids != null && !kids.isEmpty()
				&& Arrays.asList(ids).contains(null)) {
			for (PDNameTreeNode node : kids) {
				readDestinations(node, ids);
			}
		}
	}

	private PDPage getPDPage(String pageID, PDDocument doc) throws IOException {
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		if (cat.getAllPages().isEmpty())
			throw new BadRequest("Document has no pages");
		int num = getPageNumber(pageID, doc);
		if (num > 0) {
			return (PDPage) cat.getAllPages().get(num - 1);
		}
		throw new BadRequest("Unknown page: " + pageID + "\nExpected one of "
				+ Arrays.asList(getPageIDs(doc)));
	}

	private int getPageNumber(String pageID, PDDocument doc) throws IOException {
		if (pageID == null || pageID.length() == 0) {
			return 1;
		}
		Matcher pageNumber = PAGE_EQ.matcher(pageID);
		if (pageNumber.matches()) {
			try {
				return Integer.parseInt(pageNumber.group(1));
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		Matcher namedDest = NAMEDDEST_EQ.matcher(pageID);
		if (namedDest.matches()) {
			pageID = namedDest.group(1);
		}
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		if (cat.getNames() != null) {
			PDDestinationNameTreeNode dests = cat.getNames().getDests();
			if (dests != null) {
				int num = findDestinationPageNumber(pageID, dests);
				if (num > 0) {
					return num;
				}
			}
		}
		PDPageLabels pageLabels = cat.getPageLabels();
		if (pageLabels != null) {
			Map<String, Integer> map = pageLabels.getPageIndicesByLabels();
			Integer index = map.get(pageID);
			if (index != null) {
				return index + 1;
			}
		}
		return -1;
	}

	private int findDestinationPageNumber(String key, PDNameTreeNode dests)
			throws IOException {
		Map<String, COSObjectable> names = dests.getNames();
		if (names != null) {
			COSObjectable value = names.get(key);
			if (value instanceof PDPageDestination) {
				PDPageDestination dest = (PDPageDestination) value;
				int num = dest.findPageNumber();
				if (num > 0) {
					return num;
				}
			}
		}
		List<PDNameTreeNode> kids = dests.getKids();
		if (kids != null && !kids.isEmpty()) {
			for (PDNameTreeNode node : kids) {
				int num = findDestinationPageNumber(key, node);
				if (num > 0)
					return num;
			}
		}
		return -1;
	}

	private void storeDocumentInformation(PDDocumentInformation info) throws IOException {
		this.setDctermsTitle(info.getTitle());
		try {
			Calendar created = info.getCreationDate();
			if (created instanceof GregorianCalendar) {
				GregorianCalendar gcal = (GregorianCalendar) created;
				this.setDctermsCreated(df.newXMLGregorianCalendar(gcal));
			}
		} catch (IOException e) {
			logger.warn(e.toString(), e);
		}
	}

	private void storePageMetadata(PDDocumentCatalog cat, String[] ids) throws RepositoryException, IOException {
		List<PDPage> allPages = cat.getAllPages();
		PDPage[] pages = allPages.toArray(new PDPage[allPages.size()]);
		assert ids.length == pages.length;
		String base = this.getResource().stringValue();
		TermFactory tf = TermFactory.newInstance(base);
		if (cat.getURI() != null && cat.getURI().getBase() != null) {
			tf = TermFactory.newInstance(tf.resolve(cat.getURI().getBase()));
		}
		ObjectConnection con = this.getObjectConnection();
		ObjectFactory of = con.getObjectFactory();
		for (PortableDocumentPage page : this.getCalliHasPage()) {
			page.getRdfsLabel().clear();
			page.setCalliPageNumber(null);
			page.setFoafDepiction(null);
			page.getDctermsReferences().clear();
		}
		this.getCalliHasPage().clear();
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			String iri = base + "#" + id;
			String thumbnail = base + "?thumbnail=" + PercentCodec.encode(id);
			PortableDocumentPage page = con.addDesignation(
					of.createObject(iri), PortableDocumentPage.class);
			page.getRdfsLabel().add(id);
			page.setCalliPageNumber(BigInteger.valueOf(i + 1));
			RDFObject depiction = of.createObject(thumbnail);
			page.setFoafDepiction(depiction);
			if (i == 0) {
				this.setFoafDepiction(depiction);
			}
			this.getCalliHasPage().add(page);
			Set<Object> references = page.getDctermsReferences();
			List<PDAnnotation> annotations = pages[i].getAnnotations();
			if (annotations != null) {
				for (PDAnnotation ann : annotations) {
					if (ann instanceof PDAnnotationLink) {
						PDAction action = ((PDAnnotationLink) ann).getAction();
						if (action instanceof PDActionURI) {
							String uri = ((PDActionURI) action).getURI();
							if (uri != null) {
								String target = tf.resolve(uri);
								references.add(of.createObject(target));
							}
						}
					}
				}
			}
		}
	}
}
