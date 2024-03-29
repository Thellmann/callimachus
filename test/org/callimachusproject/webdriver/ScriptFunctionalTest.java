package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class ScriptFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] script = new String[] {
			"script.js",
			"function factorial(n) {\n" + "    if (n === 0) {\n"
					+ "        return 1;\n" + "    }\n"
					+ "    return n * factorial(n - 1);\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ScriptFunctionalTest.class);
	}

	public ScriptFunctionalTest() {
		super();
	}

	public ScriptFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateScript() {
		String name = script[0];
		logger.info("Create script {}", name);
		page.openCurrentFolder().openTextCreate("Script").clear()
				.type(script[1]).end().saveAs(name);
		logger.info("Delete script {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
