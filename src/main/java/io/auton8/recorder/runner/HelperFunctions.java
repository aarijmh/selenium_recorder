package io.auton8.recorder.runner;

import org.openqa.selenium.By;

public class HelperFunctions {
	public static By getBy(String key, String omit, String target) {

		if (key.equals("id")) {
			return By.id(target.replace(omit + "=", ""));
		} else if (key.equals("name")) {
			return By.name(target.replace(omit + "=", ""));
		} else if (key.equals("css:finder")) {
			return By.cssSelector(target.replace(omit + "=", ""));
		} else if (key.equals("xpath:attributes")) {
			return By.xpath(target.replace(omit + "=", ""));
		} else if (key.equals("xpath:position")) {
			return By.xpath(target.replace(omit + "=", ""));
		}
		else if (key.equals("xpath:idRelative")) {
			return By.xpath(target.replace(omit + "=", ""));
		}
		else if (key.equals("xpath:innerText")) {
			return By.xpath(target.replace(omit + "=", ""));
		}
		return null;
	}
}
