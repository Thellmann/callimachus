package docbook;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpClientFactory;
import org.callimachusproject.xproc.Pipeline;
import org.callimachusproject.xproc.PipelineFactory;

public class ArticleRoundTripTest extends TestCase {
	private static final File testDir = new File("test/docbook/tests/");

	private final HttpClient client = HttpClientFactory.getInstance().createHttpClient("http://example.com/");

	public static TestSuite suite() {
		if (testDir.isDirectory()) {
			TestSuite suite = listCases(testDir);
			suite.setName(ArticleRoundTripTest.class.getName());
			return suite;
		} else {
			return new TestSuite(ArticleRoundTripTest.class.getName());
		}
	}

	private static TestSuite listCases(File dir) {
		TestSuite cases = new TestSuite(dir.getName());
		String[] testFiles = dir.list(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return !filename.startsWith(".") && filename.endsWith(".docbook");
			}
		});
		Arrays.sort(testFiles);
		for (String filename : testFiles) {
			File f = new File(dir, filename);
			if (f.isDirectory()) {
				cases.addTest(listCases(f));
			} else {
				String name = f.getAbsolutePath();
				if (f.getPath().startsWith(testDir.getPath() + '/')) {
					name = f.getPath().substring(testDir.getPath().length() + 1);
				}
				cases.addTest(new ArticleRoundTripTest(name));
			}
		}
		return cases;
	}

	public ArticleRoundTripTest() {
		super();
	}

	public ArticleRoundTripTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected void runTest() throws Throwable {
		PipelineFactory pf = PipelineFactory.newInstance();
		String systemId = new File("test/docbook/test-harness.xpl").toURI()
				.toASCIIString();
		Pipeline pipeline = pf.createPipeline(systemId, client);
		String file = testDir.toURI().resolve(getName()).toASCIIString();
		Reader source = new StringReader(
				"<html xmlns='http://www.w3.org/1999/xhtml'><body><details><summary>test</summary><p><a rel='source' href='"
						+ file + "'>test</a></p></details></body></html>");
		String result = pipeline.pipeReader(source, file).asString();
		assertFalse(result, result.contains("ERROR"));
		assertFalse(result, result.contains("FAIL"));
		assertTrue(result, result.contains("PASS"));
	}

}
