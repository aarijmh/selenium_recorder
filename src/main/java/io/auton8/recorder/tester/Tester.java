package io.auton8.recorder.tester;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import io.auton8.recorder.model.Command;
import io.auton8.recorder.model.Test;
import io.auton8.recorder.model.TestSuite;

public class Tester {

	static WebDriver driver;
	static WebDriverWait wait;

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

	public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {

		Gson gson = new Gson();

		String folderName = "C:\\Users\\Lenovo\\Downloads\\auton8\\Sanity Test T24\\";
		String fileName = "FINAL Individual Customer -- COMPLETE v2.side";
//		String fileName = "Infinity.side";
		TestSuite testSuite = gson.fromJson(new FileReader(new File(folderName+File.separator+fileName)), TestSuite.class);
		testSuite.updateTestMap();
		FirefoxOptions browserOptions = new FirefoxOptions();
		browserOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
		driver = new FirefoxDriver(browserOptions);
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		
		Map<String, String> preferenceList = new LinkedHashMap<>();

		preferenceList.put("xpath:position", "xpath");
		preferenceList.put("css:finder", "css");
		preferenceList.put("xpath:idRelative", "xpath");
		preferenceList.put("xpath:attributes", "xpath");
		preferenceList.put("id", "id");
		preferenceList.put("name", "name");
		preferenceList.put("xpath:innerText", "xpath");

		Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(20)).pollingEvery(Duration.ofMillis(300)).ignoring(ElementNotInteractableException.class);
		int count = 1;
		FileOutputStream outputStream = new FileOutputStream(new File("d:\\sele.txt"));
		DataOutputStream dot = new DataOutputStream(outputStream);
		try {
			// Navigate to Url

			for (Test test : testSuite.getTests()) {
				count = 1;
				for (Command command : test.getCommands()) {
					if(command.getCommand().startsWith("//"))
						continue;
					if (command.getCommand().equalsIgnoreCase("open")) {
						driver.get(command.getTarget());
					} else if (command.getCommand().equalsIgnoreCase("setWindowSize")) {
						String[] dimension = command.getTarget().split("x");
						int width = Integer.valueOf(dimension[0]);
						int height = Integer.valueOf(dimension[1]);
						Dimension d = new Dimension(width, height);
						driver.manage().window().setSize(d);
					} else if (command.getCommand().equalsIgnoreCase("mouseUpAt") || command.getCommand().equalsIgnoreCase("mouseDownAt")) {
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
					} else if (command.getCommand().equalsIgnoreCase("runScript")) {
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript(command.getTarget());
					} else if (command.getCommand().equalsIgnoreCase("mouseOver") || command.getCommand().equalsIgnoreCase("mouseOut")) {

					} else {
						boolean found = false;
						WebElement foo = null;

						for (Entry<String, String> entry : preferenceList.entrySet()) {
							if (!command.getTargetMap().containsKey(entry.getKey()))
								continue;
							try {
								By eleBy = getBy(entry.getKey(), entry.getValue(), command.getTargetMap().get(entry.getKey()));
								if (eleBy != null) {
									foo = wait.until(ExpectedConditions.visibilityOfElementLocated(eleBy));
								}
								found = true;
							} catch (TimeoutException ne) {
								ne.printStackTrace();
								dot.writeBytes(ne.toString()+"\n");
							} catch (ElementClickInterceptedException eci) {
								eci.printStackTrace();
								dot.writeBytes(eci.toString()+"\n");
							}
							if (found)
								break;
						}
						if (!found)
							break;
						if (foo != null) {
							if (command.getCommand().equalsIgnoreCase("click")) {
								try {
									foo.click();
								} catch (ElementClickInterceptedException eci) {
									
									executor.executeScript("arguments[0].click();", foo);
								}catch(StaleElementReferenceException e) {
						            // retrieving the name input field again
									wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//mat-option[contains(.,'Apartment')]"))).click();
						            // now nameHtmlElement is no longer stale
						        }


							} else if (command.getCommand().equalsIgnoreCase("type")) {
								foo.sendKeys(command.getValue());
							} else if (command.getCommand().equalsIgnoreCase("mouseOver")) {
								Actions action = new Actions(driver);
								action.moveToElement(foo).perform();
							}else if (command.getCommand().startsWith("wait")) {
								Thread.sleep(Duration.ofSeconds(Long.valueOf(command.getValue())));
								System.out.println("Waiting done");
							}else if (command.getCommand().startsWith("copy")) {
								dot.writeBytes("COPIED--"+foo.getText()+","+foo.getAttribute("value")+","+foo.getTagName()+"\n");
								
							}
						}

					}
					dot.writeBytes(count + " " + command+"\n");
					System.out.println(count + " " + command);
					count++;
				}
				break;
			}

		} catch (Exception e) {
			dot.writeBytes(count+"\n");
			System.out.println(count);
			e.printStackTrace();
			dot.writeBytes(e.toString()+"\n");
		} finally {
			System.out.println("Loaded");
			driver.quit();
			dot.close();
		}

	}

}
