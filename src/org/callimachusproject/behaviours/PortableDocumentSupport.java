package org.callimachusproject.behaviours;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.util.PDFTextStripper;
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.concepts.PortableDocument;
import org.callimachusproject.concepts.PortableDocumentPage;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.exceptions.BadGateway;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
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

	private final Logger logger = LoggerFactory
			.getLogger(PortableDocumentSupport.class);

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
		storeMetadata(null);
		this.touchRevision();
	}

	@Sparql("portableDocumentOutline.rq")
	public abstract GraphQueryResult GetOutline();

	public String PostOutline(GraphQueryResult result) throws IOException,
			OpenRDFException, COSVisitorException {
		PDDocumentLoader loader = new PDDocumentLoader(this.getHttpClient());
		try {
			String base = this.getResource().stringValue();
			PDDocument doc = loader.loadDocument(this.openInputStream(), base);
			List<PageSource> outline = asOutlineList(result, loader.getLoadedPageIri());
			Map<PDPage, String> pageIDMap = getPageIDMap(doc);
			addPages(doc, loader, outline);
			List<PDPage> allPages = doc.getDocumentCatalog().getAllPages();
			pageIDMap.keySet().retainAll(allPages);
			String[] quotedFrom = new String[outline.size()];
			assert outline.size() == allPages.size();
			for (int i = 0, n = outline.size(); i < n; i++) {
				PDPage page = allPages.get(i);
				String from = quotedFrom[i] = outline.get(i).getQuotedFrom();
				if (!pageIDMap.containsKey(page) && from != null && from.lastIndexOf('#') > 0) {
					String id = from.substring(from.lastIndexOf('#') + 1);
					if (id.indexOf('=') < 0 && id.indexOf('&') < 0) {
						pageIDMap.put(page, id);
					}
				}
			}
			setPageIDs(doc, pageIDMap);
			OutputStream out = this.openOutputStream();
			try {
				doc.save(out);
			} finally {
				out.close();
			}
			storeMetadata(quotedFrom);
			this.touchRevision();
			return base + "?view";
		} finally {
			loader.close();
		}
	}

	private void addPages(PDDocument doc, PDDocumentLoader loader,
			List<PageSource> outline) throws IOException {
		List<PDPage> allPages = doc.getDocumentCatalog().getAllPages();
		List<PDPage> currently = new ArrayList<PDPage>(allPages);
		for (int i = 0, n = outline.size(); i < n; i++) {
			PageSource source = outline.get(i);
			PDPage page = loader.loadPage(source.getCopyFrom());
			if (currently.size() > i && !currently.get(i).equals(page)) {
				for (int j = currently.size() - 1; j >= i; j--) {
					doc.removePage(currently.get(j));
				}
				currently.clear();
			}
			if (currently.size() <= i) {
				doc.addPage(page);
			}
		}
		for (int j = currently.size() - 1; j >= outline.size(); j--) {
			doc.removePage(currently.get(j));
		}
	}

	private void setPageIDs(PDDocument doc, Map<PDPage, String> pageIDMap)
			throws IOException {
		Map<String, COSObjectable> map = new HashMap<>();
		for (Map.Entry<PDPage, String> e : pageIDMap.entrySet()) {
			PDPage page = e.getKey();
			String id = e.getValue();
			if (!map.containsKey(id)) {
				PDPageFitDestination dest = new PDPageFitDestination();
				dest.setPage(page);
				map.put(id, dest);
			}
		}
		PDDocumentNameDictionary names = doc.getDocumentCatalog().getNames();
		if (names == null) {
			names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
			doc.getDocumentCatalog().setNames(names);
		}
		PDDestinationNameTreeNode dests = names.getDests();
		if (dests == null) {
			dests = new PDDestinationNameTreeNode(names.getCOSDictionary());
			names.setDests(dests);
		}
		dests.setNames(map);
	}

	private List<PageSource> asOutlineList(GraphQueryResult result, Set<String> loaded)
			throws QueryEvaluationException {
		Map<String, String> derived = new TreeMap<>();
		Map<Integer, String> map = new TreeMap<>();
		Set<String> included = new HashSet<>();
		while (result.hasNext()) {
			Statement st = result.next();
			Resource subj = st.getSubject();
			String pred = st.getPredicate().stringValue();
			Value obj = st.getObject();
			if (this.getResource().equals(subj)
					&& PortableDocument.HAS_PAGE.equals(pred)) {
				included.add(obj.stringValue());
			} else if (PortableDocumentPage.WAS_QUOTED_FROM.equals(pred)) {
				derived.put(subj.stringValue(), obj.stringValue());
			} else if (obj instanceof Literal
					&& PortableDocumentPage.PAGE_NUMBER.equals(pred)) {
				int num = ((Literal) st.getObject()).intValue();
				map.put(num - 1, subj.stringValue());
			}
		}
		List<String> values = new ArrayList<>(map.values());
		values.retainAll(included);
		List<PageSource> outline = new ArrayList<>(values.size());
		for (String value : values) {
			if (!loaded.contains(value) && derived.containsKey(value)) {
				outline.add(new PageSource(derived.get(value)));
			} else {
				outline.add(new PageSource(value, derived.get(value)));
			}
		}
		return outline;
	}

	private void storeMetadata(String[] quotedFrom) throws IOException, RepositoryException {
		String base = this.getResource().stringValue();
		InputStream in = this.openInputStream();
		PDDocument doc = PDDocument.load(in);
		try {
			PDDocumentInformation info = doc.getDocumentInformation();
			storeDocumentInformation(info);
			Map<String, PDPage> pages = getCanonicalPageMap(doc, base);
			TermFactory tf = TermFactory.newInstance(base);
			PDDocumentCatalog cat = doc.getDocumentCatalog();
			if (cat.getURI() != null && cat.getURI().getBase() != null) {
				tf = TermFactory
						.newInstance(tf.resolve(cat.getURI().getBase()));
			}
			storePageMetadata(pages, quotedFrom, tf);
		} finally {
			doc.close();
			in.close();
		}
	}

	private static Map<String, PDPage> getCanonicalPageMap(PDDocument doc,
			String systemId) throws IOException {
		String[] ids = getPageIDs(doc);
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		List<PDPage> allPages = cat.getAllPages();
		assert ids.length == allPages.size();
		Map<String, PDPage> pages = new LinkedHashMap<>();
		for (int i = 0; i < ids.length; i++) {
			pages.put(systemId + "#" + ids[i], allPages.get(i));
		}
		return pages;
	}

	private static Map<PDPage, String> getPageIDMap(PDDocument doc) throws IOException {
		String[] ids = getPageIDs(doc);
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		List<PDPage> allPages = cat.getAllPages();
		assert ids.length == allPages.size();
		Map<PDPage, String> pages = new LinkedHashMap<>();
		for (int i = 0; i < ids.length; i++) {
			if (!ids[i].startsWith("page=")) {
				pages.put(allPages.get(i), ids[i]);
			}
		}
		return pages;
	}

	private static String[] getPageIDs(PDDocument doc) throws IOException {
		String[] ids = new String[doc.getNumberOfPages()];
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		if (cat.getNames() != null) {
			PDDestinationNameTreeNode dests = cat.getNames().getDests();
			if (dests != null) {
				readDestinations(dests, ids);
			}
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == null)
				continue;
			boolean duplicate = false;
			for (int j = i + 1; j < ids.length; j++) {
				if (ids[i].equals(ids[j])) {
					ids[j] = null;
					duplicate = true;
				}
			}
			if (duplicate || ids[i].indexOf('=') >= 0) {
				ids[i] = null;
			}
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == null) {
				ids[i] = "page=" + (1 + i);
			}
		}
		return ids;
	}

	private static void readDestinations(PDNameTreeNode dests, String[] ids)
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
			try {
				return (PDPage) cat.getAllPages().get(num - 1);
			} catch (IndexOutOfBoundsException e) {
				throw new BadRequest(e);
			}
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

	private void storeDocumentInformation(PDDocumentInformation info)
			throws IOException {
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

	private void storePageMetadata(Map<String, PDPage> pages, String[] quotedFrom, TermFactory tf)
			throws RepositoryException, IOException {
		String base = this.getResource().stringValue();
		ObjectConnection con = this.getObjectConnection();
		ObjectFactory of = con.getObjectFactory();
		for (PortableDocumentPage page : this.getCalliHasPage()) {
			page.getRdfsLabel().clear();
			page.setCalliPageNumber(null);
			page.setProvWasQuotedFrom(null);
			page.setFoafDepiction(null);
			page.getDctermsReferences().clear();
		}
		this.getCalliHasPage().clear();
		int num = 0;
		for (String iri : pages.keySet()) {
			String id = iri.substring(iri.indexOf('#') + 1);
			String thumbnail = base + "?thumbnail=" + PercentCodec.encode(id);
			PortableDocumentPage page = con.addDesignation(
					of.createObject(iri), PortableDocumentPage.class);
			page.setCalliPageNumber(BigInteger.valueOf(++num));
			if (quotedFrom != null && quotedFrom[num - 1] != null) {
				page.setProvWasQuotedFrom(of.createObject(quotedFrom[num - 1]));
			}
			RDFObject depiction = of.createObject(thumbnail);
			page.setFoafDepiction(depiction);
			if (num == 1) {
				this.setFoafDepiction(depiction);
			}
			this.getCalliHasPage().add(page);
			Set<Object> references = page.getDctermsReferences();
			List<PDAnnotation> annotations = pages.get(iri).getAnnotations();
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

	private static class PageSource {
		private final String copyFrom;
		private final String quotedFrom;

		public PageSource(String copyFrom) {
			this(copyFrom, copyFrom);
		}

		public PageSource(String copyFrom, String quotedFrom) {
			this.copyFrom = copyFrom;
			this.quotedFrom = quotedFrom;
		}

		public String getCopyFrom() {
			return copyFrom;
		}

		public String getQuotedFrom() {
			return quotedFrom;
		}
	}

	private static class PDDocumentLoader {
		final Collection<Closeable> toBeClosed = new LinkedList<>();
		final Map<String, PDDocument> documents = new HashMap<>();
		final Map<String, PDPage> pages = new HashMap<>();
		final HttpUriClient client;

		public PDDocumentLoader(HttpUriClient client) {
			this.client = client;
		}

		public PDDocument loadDocument(InputStream in, String systemId)
				throws IOException {
			PDDocument doc;
			if (in == null) {
				doc = new PDDocument();
			} else {
				toBeClosed.add(in);
				doc = PDDocument.load(in);
			}
			documents.put(systemId, doc);
			pages.putAll(getPageIriMap(doc, systemId));
			return doc;
		}

		public PDPage loadPage(String inserting) throws ResponseException,
				IOException {
			if (!pages.containsKey(inserting)) {
				String url = inserting.substring(0, inserting.indexOf('#'));
				if (documents.containsKey(url))
					throw new BadRequest("Missing document page: " + inserting);
				InputStream entity = client.getEntity(url, "application/pdf")
						.getContent();
				if (entity == null)
					throw new BadGateway("Missing entity on " + inserting);
				toBeClosed.add(entity);
				PDDocument loaded = PDDocument.load(entity);
				documents.put(url, loaded);
				pages.putAll(getPageIriMap(loaded, url));
				if (!pages.containsKey(inserting))
					throw new BadRequest("Missing document page: " + inserting);
			}
			return pages.get(inserting);
		}

		public Set<String> getLoadedPageIri() {
			return pages.keySet();
		}

		public void close() throws IOException {
			try {
				for (PDDocument doc : documents.values()) {
					doc.close();
				}
			} finally {
				for (Closeable closeable : toBeClosed) {
					closeable.close();
				}
			}
		}

		private Map<String, PDPage> getPageIriMap(PDDocument doc,
				String systemId) throws IOException {
			Map<String, PDPage> pages = getCanonicalPageMap(doc, systemId);
			int num = 0;
			for (String iri : new ArrayList<>(pages.keySet())) {
				String base = iri.substring(0, iri.indexOf('#'));
				pages.put(base + "#page=" + (++num), pages.get(iri));
			}
			return pages;
		}
	}
}
