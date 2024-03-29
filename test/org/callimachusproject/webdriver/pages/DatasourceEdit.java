package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DatasourceEdit extends CalliPage {

	public DatasourceEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		browser.click(By.id("delete-datasource"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

}
