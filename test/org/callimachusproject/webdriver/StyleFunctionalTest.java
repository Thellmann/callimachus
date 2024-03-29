package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class StyleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] style = new String[] {
			"style.css",
			"body {\n" + "background-color:#d0e4fe;\n" + "}\n" + "h1 {\n"
					+ "color:orange;\n" + "text-align:center;\n" + "}\n"
					+ "p {\n" + "font-family:\"Times New Roman\";\n"
					+ "font-size:20px;\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(StyleFunctionalTest.class);
	}

	public StyleFunctionalTest() {
		super();
	}

	public StyleFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateStyle() {
		String name = style[0];
		logger.info("Create style {}", name);
		page.openCurrentFolder().openTextCreate("StyleSheet").clear()
				.type(style[1]).end().saveAs(name);
		logger.info("Delete style {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
