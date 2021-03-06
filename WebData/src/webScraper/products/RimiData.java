package webScraper.products;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for collecting product info from app.rimi.lv.
 * 
 * @author Lauris Lazda
 * @author Jānis Lazda
 */

// Commented code is left there so it is easier to change from gathering
// sub-category names to gather only category names if needed.
public class RimiData {

	// TODO: check if server returns answer, if not handle exception and delay
	// search for ~5 minutes.

	// private List<String> urlList;
	private Map<String, String> urlList;
	// private List<String> urlSecondaryList;
	private Map<String, String> urlSecondaryList;
	private List<WebProduct> products;
	private WebProduct webProduct;
	// private Map<String, String> categories;
	private List<String> categories;

	/**
	 * Constructor of RimiData. Inicializes Lists and HashMaps and calls
	 * collectURLs() and collectData()
	 */
	public RimiData() {
		// urlList = new ArrayList<>();
		urlList = new HashMap<>();
		// urlSecondaryList = new ArrayList<>();
		urlSecondaryList = new HashMap<>();
		products = new ArrayList<>();
		// categories = new HashMap<>();
		categories = new ArrayList<>();

		try {
			collectURLs();
			collectData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method which reads page count for current category and calls readPage()
	 * passing categoryName and url
	 */
	private void collectData() throws Exception {
		URL url;
		String inputLine;
		int pagesCount = 0;
		Pattern pageCount = Pattern.compile("(?<=<b>1 / )(\\d{1,2})(?=</b>)");
		Matcher matcher;
		BufferedReader in;
		String categoryName;
		boolean isContent = false;

		// for (String string : urlList) {
		for (String string : urlList.keySet()) {
			categoryName = urlList.get(string);
			boolean hasCategories = true;
			url = new URL(string);
			in = new BufferedReader(new InputStreamReader(url.openStream(),
					"UTF8"));
			while ((inputLine = in.readLine()) != null) {
				if ((inputLine.replaceAll("\\s", ""))
						.equals("<divclass=\"category-itemsjs-cat-items\">")) {
					inputLine = in.readLine();
					matcher = pageCount.matcher(inputLine);
					matcher.find();
					pagesCount = Integer.parseInt(matcher.group());
					hasCategories = false;
					break;
				}
			}
			in.close();

			if (hasCategories) {
				String homeURL = "https://app.rimi.lv/products";
				url = new URL(string);
				Pattern subCategoryIdPattern = Pattern
						.compile("(?<=<a href=\"/products/)(.+?)(?=\" data-category-id)");
				in = new BufferedReader(new InputStreamReader(url.openStream(),
						"UTF8"));

				while ((inputLine = in.readLine()) != null) {
					if (isContent) {
						if ((inputLine.replaceAll("\\s", "")).equals("</div>")) {
							break;
						}
						matcher = subCategoryIdPattern.matcher(inputLine);
						if (matcher.find()) {
							urlSecondaryList.put(
									homeURL + "/" + matcher.group(),
									categoryName);
						}
					}
					if ((inputLine.replaceAll("\\s", ""))
							.equals("<divclass=\"collapsable-groupm-bottom01hidden-xs\">")) {
						isContent = true;
					}
				}
				in.close();
			} else {
				for (int i = 1; i <= pagesCount; i++) {
					if (pagesCount == 1) {
						readPage(url, categoryName);
					} else {
						url = new URL(string + "/page/" + i);
						readPage(url, categoryName);
					}
				}
			}
		}
		// for (String subUrl : urlSecondaryList) {
		for (String subUrl : urlSecondaryList.keySet()) {
			categoryName = urlSecondaryList.get(subUrl);
			url = new URL(subUrl);
			pagesCount = 0;
			in = new BufferedReader(new InputStreamReader(url.openStream(),
					"UTF8"));
			while ((inputLine = in.readLine()) != null) {
				if ((inputLine.replaceAll("\\s", ""))
						.equals("<divclass=\"category-itemsjs-cat-items\">")) {
					inputLine = in.readLine();
					matcher = pageCount.matcher(inputLine);
					matcher.find();
					pagesCount = Integer.parseInt(matcher.group());
					break;
				}
			}
			in.close();
			for (int i = 1; i <= pagesCount; i++) {
				if (pagesCount == 1) {
					readPage(url, categoryName);
				} else {
					url = new URL(subUrl + "/page/" + i);
					readPage(url, categoryName);
				}
			}
		}
	}

	/**
	 * Method which reads passed webpages URL and collects products from each
	 * passed webpage storing them in webProduct object
	 * 
	 * @param url
	 * @param categoryName
	 */
	private void readPage(URL url, String categoryName) throws Exception {
		String product = "";
		double price = 0;
		long barcode = 0;

		BufferedReader in = new BufferedReader(new InputStreamReader(
				url.openStream(), "UTF8"));
		String inputLine;
		Pattern namePattern = Pattern.compile("(?<=name\">)(.+?)(?=</div>)");
		Pattern wholePricePattern = Pattern
				.compile("(?<=whole-number \">)(\\d{1,3})(?=</span>)");
		Pattern decimalPricePattern = Pattern
				.compile("(?<=decimal\">)(\\d{2})(?=</span>)");
		Pattern barcodePattern = Pattern
				.compile("(?<=data-ean=\")(\\d+)(?=\" data-item_id=)");
		Matcher matcher;

		while ((inputLine = in.readLine()) != null) {
			boolean isTable = false;
			boolean isContent = false;
			boolean priceBubble = false;
			boolean barcodeFound = false;
			String wholePrice;
			String decimalPrice;
			String allPrice;
			if ((inputLine.replaceAll("\\s", ""))
					.equals("<divclass=\"products-shelfjs-products-shelf\">")) {
				while ((inputLine = in.readLine()) != null) {
					if (isContent) {
						if ((inputLine.replaceAll("\\s", "")).equals("</ul>")) {
							// Breaks the inner loop when content ends
							break;
						}
						if (!barcodeFound) {
							matcher = barcodePattern.matcher(inputLine);
							if (matcher.find()) {
								barcode = Long.parseLong(matcher.group());
								barcodeFound = true;
							}
						}
						if ((inputLine.replaceAll("\\s", ""))
								.equals("<divclass=\"inforelativeclear\">")) {
							inputLine = in.readLine();
							matcher = namePattern.matcher(inputLine);
							matcher.find();
							product = matcher.group();
						}
						if ((inputLine.replaceAll("\\s", ""))
								.equals("<divclass=\"price-bubble\">")) {
							priceBubble = true;
						}
						if ((inputLine.replaceAll("\\s", ""))
								.equals("<divclass=\"price\">")) {
							if (priceBubble) {
								priceBubble = false;
								continue;
							}
							inputLine = in.readLine();
							matcher = wholePricePattern.matcher(inputLine);
							matcher.find();
							wholePrice = matcher.group();
							inputLine = in.readLine();
							matcher = decimalPricePattern.matcher(inputLine);
							matcher.find();
							decimalPrice = matcher.group();
							allPrice = wholePrice + "." + decimalPrice;
							price = Double.parseDouble(allPrice);
							webProduct = new WebProduct(categoryName, product,
									price, barcode);
							products.add(webProduct);
							System.out.println(webProduct.toString()); // test
							barcodeFound = false;
						}
					}
					if (isTable) {
						if ((inputLine.replaceAll("\\s", ""))
								.equals("<liclass=\"relativeitemeffectfade-shadowjs-shelf-itemshelf-item\"data-ads=\"true\"")
								|| (inputLine.replaceAll("\\s", ""))
										.equals("<liclass=\"relativeitemeffectfade-shadowjs-shelf-itemshelf-item\"")) {
							isContent = true;
						}
					}
					if ((inputLine.replaceAll("\\s", ""))
							.equals("<ulclass=\"shelfjs-shelfclearclearfix\">")) {
						isTable = true;
					}
				}
				// When nessecary info is collected stops outer loop
				break;
			}
		}
		in.close();
	}

	/**
	 * Method which collects category IDs and stores them in categories list
	 * then going through the list populates the urlList with category URLs
	 */
	private void collectURLs() throws Exception {
		boolean isContent = false;
		String homeURL = "https://app.rimi.lv/products";
		URL url = new URL(homeURL + "/selection");
		String inputLine;
		Pattern categoryIdPattern = Pattern
				.compile("(?<=<a href=\"/products/)(.+?)(?=\" data-category-id)");
		Pattern categoryNamePattern = Pattern
				.compile("(?<=<span>)(.+?)(?=</span>)");
		Matcher matcher;
		BufferedReader in = new BufferedReader(new InputStreamReader(
				url.openStream(), "UTF8"));

		while ((inputLine = in.readLine()) != null) {
			if (isContent) {
				if ((inputLine.replaceAll("\\s", "")).equals("</div>")) {
					break;
				}
				matcher = categoryIdPattern.matcher(inputLine);
				if (matcher.find()) {
					categories.add(matcher.group());
					// String categoryId = matcher.group();
					// matcher = categoryNamePattern.matcher(inputLine);
					// matcher.find();
					// String categoryName = matcher.group();
					// categories.put(categoryId, categoryName);
				}
			}
			if ((inputLine.replaceAll("\\s", ""))
					.equals("<divclass=\"collapsable-groupm-bottom01hidden-xs\">")) {
				isContent = true;
			}
		}
		in.close();
		// for (String string : categories.keySet()) {
		for (String string : categories) {
			isContent = false;
			url = new URL(homeURL + "/" + string);
			// Pattern subCategoryPattern =
			// Pattern.compile("(?<=data-category-id=\")(.+?)(?=\" class)");
			in = new BufferedReader(new InputStreamReader(url.openStream(),
					"UTF8"));

			while ((inputLine = in.readLine()) != null) {
				if (isContent) {
					if ((inputLine.replaceAll("\\s", "")).equals("</div>")) {
						break;
					}
					// matcher = subCategoryPattern.matcher(inputLine);
					matcher = categoryIdPattern.matcher(inputLine);
					if (matcher.find()) {
						// urlList.add(homeURL + "/" + matcher.group());
						String categoryId = matcher.group();
						inputLine = in.readLine();
						matcher = categoryNamePattern.matcher(inputLine);
						matcher.find();
						String categoryName = matcher.group();
						urlList.put(homeURL + "/" + categoryId, categoryName);
					}
				}
				if ((inputLine.replaceAll("\\s", ""))
						.equals("<divclass=\"collapsable-groupm-bottom01hidden-xs\">")) {
					isContent = true;
				}
			}
			in.close();
		}
	}

	/**
	 * Method to pass collected data
	 * 
	 * @return list of WebProducts
	 */
	public List<WebProduct> getProducts() {
		return products;
	}
}
