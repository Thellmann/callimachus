package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.Test;

public class ArticleIntegrationTest extends TemporaryServerIntegrationTestCase {
	private static final String DOCBOOK = "<article version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
			"<title>LS command</title> \n " +
			"<para>This command is a synonym for command. \n" +
			"</para> \n </article>";
	private static final String XHTML = "<html  xmlns='http://www.w3.org/1999/xhtml'> \n" +
			"<head><title>LS command</title></head> \n " +
			"<body><h1>LS command</h1>" +
			"<p>This command is a synonym for command. \n" +
			"</p> </body> </html>";
	private static final String XHTML_H2 = "<html xmlns='http://www.w3.org/1999/xhtml'>\n"
			+ "<head>\n"
			+ "    <title></title>\n"
			+ "</head>\n"
			+ "<body>\n"
			+ "    <h1>heading</h1>\n"
			+ "    \n"
			+ "    <h2>Subheading</h2>\n"
			+ "    \n"
			+ "    <h2>subheading2</h2>\n"
			+ "    \n"
			+ "</body>\n"
			+ "</html>";

	@Test
	public void testDocbookCreate() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/Article") + "&location=test-article.docbook");
		WebResource article = create.create("application/docbook+xml", DOCBOOK.getBytes());
		WebResource edit = article.link("edit-media", "application/docbook+xml");
		edit.get("application/docbook+xml");
		edit.put("application/docbook+xml", DOCBOOK.getBytes());
		edit.delete();
	}

	@Test
	public void testXhtmlCreate() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/Article") + "&location=test-article.docbook");
		WebResource article = create.create("application/xhtml+xml", XHTML.getBytes());
		WebResource edit = article.link("edit-media", "application/xhtml+xml");
		edit.get("application/xhtml+xml");
		edit.put("application/xhtml+xml", XHTML.getBytes());
		edit.delete();
	}

	@Test
	public void testXhtmlHeadingCreate() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/Article") + "&location=test-article.docbook");
		WebResource article = create.create("application/xhtml+xml", XHTML.getBytes());
		WebResource edit = article.link("edit-media", "application/xhtml+xml");
		edit.get("application/xhtml+xml");
		edit.put("application/xhtml+xml", XHTML_H2.getBytes());
		edit.delete();
	}

}
