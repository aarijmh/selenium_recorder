package io.auton8.recorder.runner;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
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

import io.auton8.recorder.model.Command;
import io.auton8.recorder.model.Test;
import io.auton8.recorder.model.TestSuite;

public class DataDrivenRunner {

	static WebDriver driver;
	static WebDriverWait wait;

	public static Map<String, String> readMapping(String filePath) throws InvalidFormatException, IOException {
		Map<String, String> mapping = new HashMap<String, String>();

		try (Workbook workbook = new XSSFWorkbook(new File(filePath))) {
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				if (row.getCell(0) != null) {
					mapping.put(row.getCell(0).getStringCellValue(), row.getCell(1).getStringCellValue());
				}
			}
		}
		return mapping;
	}

	public static Map<Integer, Map<String, String>> readDataWithMapping(String filePath) throws InvalidFormatException, IOException {
		Map<Integer, Map<String, String>> mapping = new HashMap<>();

		Map<String, Integer> headerMapping = new LinkedHashMap<>();

		try (Workbook workbook = new XSSFWorkbook(new File(filePath))) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);

			int count = 0;
			for (Cell cell : headerRow) {
				headerMapping.put(cell.getStringCellValue(), count++);
			}

			int rowCount = 0;

			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				for (String key : headerMapping.keySet()) {
					Map<String, String> valueList = null;
					if (!mapping.containsKey(rowCount)) {
						valueList = new HashMap<>();
						mapping.put(rowCount, valueList);
					} else {
						valueList = mapping.get(rowCount);
					}
					Cell cell = row.getCell(headerMapping.get(key));
					if (cell != null) {
						if (cell.getCellType().equals(CellType.NUMERIC)) {
							valueList.put(key, String.valueOf((int) cell.getNumericCellValue()));
						} else if (cell.getCellType().equals(CellType.STRING)) {
							valueList.put(key, String.valueOf(cell.getStringCellValue()));
						} else if (cell.getCellType().equals(CellType.BLANK)) {
							valueList.put(key, null);
						} else {
							System.out.println();
						}
					} else {
						valueList.put(key, null);
					}
				}
				rowCount++;
			}

		}
		return mapping;
	}

	public static void runTestCaseWithData(String testCasePath, Map<String, String> mapping, Map<String, String> data, String logOutFile, String outputFolder, Integer runNumber, DataOutputStream idDot) throws IOException {
		Gson gson = new Gson();

		TestSuite testSuite = gson.fromJson(new FileReader(new File(testCasePath)), TestSuite.class);
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
		File file = new File(logOutFile);

		FileOutputStream outputStream = new FileOutputStream(file);

		DataOutputStream dot = new DataOutputStream(outputStream);
		String runId = java.util.UUID.randomUUID().toString();
		File runFolder = new File(outputFolder + File.separator + runNumber);
		runFolder.mkdirs();

		try {
			// Navigate to Url

			for (Test test : testSuite.getTests()) {
				count = 1;
				for (Command command : test.getCommands()) {
					if (command.getCommand().startsWith("//"))
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
								By eleBy = HelperFunctions.getBy(entry.getKey(), entry.getValue(), command.getTargetMap().get(entry.getKey()));
								if (eleBy != null) {
									foo = wait.until(ExpectedConditions.visibilityOfElementLocated(eleBy));
								}
								found = true;
							} catch (TimeoutException ne) {
								ne.printStackTrace();
								dot.writeBytes(ne.toString() + "\n");
							} catch (ElementClickInterceptedException eci) {
								eci.printStackTrace();
								dot.writeBytes(eci.toString() + "\n");
							}
							if (found)
								break;
						}
						if (!found)
							break;
						if (foo != null) {
							if (command.getCommand().equalsIgnoreCase("click")) {
								if (command.getId().equals("afac828b-557e-483f-94bf-d3a9920fc260")) {
									System.out.println();
								}
								if (mapping.containsKey(command.getId())) {
									if (command.getTargetMap().containsKey("xpath:innerText")) {
										String value = data.get(mapping.get(command.getId()));
										if (value != null) {
											String innerText = command.getTargetMap().get("xpath:innerText").replace("xpath=", "");
											String newValue = innerText.substring(0, innerText.indexOf("'")) + "'" + value + innerText.substring(innerText.lastIndexOf("'"));
											By by = By.xpath(newValue);
											foo = driver.findElement(by);
										}
									}
								}
								try {
									foo.click();
								} catch (ElementClickInterceptedException eci) {

									executor.executeScript("arguments[0].click();", foo);
								} catch (StaleElementReferenceException e) {
									// retrieving the name input field again
									wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//mat-option[contains(.,'Apartment')]"))).click();
									// now nameHtmlElement is no longer stale
								}

							} else if (command.getCommand().equalsIgnoreCase("type")) {
								if (mapping.containsKey(command.getId())) {
									String value = data.get(mapping.get(command.getId()));
									foo.sendKeys(value == null ? "" : value);
								} else
									foo.sendKeys(command.getValue());
							} else if (command.getCommand().equalsIgnoreCase("mouseOver")) {
								Actions action = new Actions(driver);
								action.moveToElement(foo).perform();
							} else if (command.getCommand().startsWith("wait")) {
								Thread.sleep(Duration.ofSeconds(Long.valueOf(command.getValue())));
								System.out.println("Waiting done");
							} else if (command.getCommand().startsWith("copy")) {
								dot.writeBytes("COPIED--" + foo.getText() + "," + foo.getAttribute("value") + "," + foo.getTagName() + "\n");
								idDot.writeBytes(runId + "," + foo.getAttribute("value") + "\n");
							}
							TakesScreenshot scrShot = ((TakesScreenshot) driver);
							// Call getScreenshotAs method to create image file
							File SrcFile = scrShot.getScreenshotAs(OutputType.FILE);
							// Move image file to new destination
							File DestFile = new File(runFolder.getAbsolutePath() + File.separator + command.getId() + ".png");
							// Copy file at destination
							FileUtils.copyFile(SrcFile, DestFile);
						}

					}
					dot.writeBytes(count + " " + command + "\n");
					System.out.println(count + " " + command);
					count++;
				}
				break;
			}

		} catch (Exception e) {
			dot.writeBytes(count + "\n");
			System.out.println(count);
			e.printStackTrace();
			dot.writeBytes(e.toString() + "\n");
		} finally {
			System.out.println("Loaded");
			driver.quit();
			dot.close();
			//idDot.close();
		}

	}

	public static void main(String[] args) throws InvalidFormatException, IOException {
		Map<String, String> mapping = readMapping("C:\\Users\\Lenovo\\Downloads\\auton8\\Sanity Test T24\\Ind Customer Mapping.xlsx");
		Map<Integer, Map<String, String>> dataMapping = readDataWithMapping("C:\\Users\\Lenovo\\Downloads\\auton8\\Sanity Test T24\\TC_005_SampleDataDrive_Auton8.xlsx");

		String runId = java.util.UUID.randomUUID().toString();
		String outputFolder = "C:\\Users\\Lenovo\\Downloads\\auton8\\Sanity Test T24\\"+runId;
		File file = new File(outputFolder);
		file.mkdirs();
		DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(outputFolder+File.separator+"ids.txt")));
		for (Integer key : dataMapping.keySet()) {
			long startTime = System.currentTimeMillis();
			runTestCaseWithData("C:\\Users\\Lenovo\\Downloads\\auton8\\Sanity Test T24\\FINAL Individual Customer -- COMPLETE v3.side", 
					mapping, 
					dataMapping.get(key), 
					outputFolder+File.separator+"log.txt",
					outputFolder+File.separator+"screenshots\\",
					key,
					outputStream);
			long endTime = System.currentTimeMillis();
			System.out.println(key+" =  "+(endTime-startTime));
		}
		

	}

}
