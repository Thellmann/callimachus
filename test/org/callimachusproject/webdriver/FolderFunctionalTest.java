package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;

public class FolderFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(FolderFunctionalTest.class);
	}

	public FolderFunctionalTest() {
		super();
	}

	public FolderFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testHomeFolderView() {
		page.openHomeFolder().assertLayout();
	}

	public void testCreateFolder() {
		String folderName = "Bob's%20R&D+Folder";
		logger.info("Create folder {}", folderName);
		page.openCurrentFolder().openFolderCreate().with(folderName).create()
				.waitUntilFolderOpen(folderName);
		logger.info("Delete folder {}", folderName);
		page.openCurrentFolder().waitUntilFolderOpen(folderName).openEdit()
				.waitUntilTitle(folderName).delete();
	}

}
