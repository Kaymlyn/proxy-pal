package com.knightowlgames.proxypal;

import com.knightowlgames.proxypal.datatype.MagicCard;
import com.knightowlgames.proxypal.image.ImageManipulator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mtggoldfish/")
public class MTGGoldfishImageRetriever {

	private ImageManipulator manipulator;

	private Float cardWidth = 223f;

	@Autowired
	public MTGGoldfishImageRetriever(ImageManipulator manipulator) {
		this.manipulator = manipulator;
		this.manipulator.setCardWidth(cardWidth);
		this.manipulator.setMargin(2);
	}

	@RequestMapping(value = "/getImageFile/{cardName}/{setId}", method = RequestMethod.GET)
	public ResponseEntity<String> getImage(@PathVariable("cardName") String cardName,
                                           @PathVariable("setId") String setId)
			{
				if (downloadImageToFile(cardName, setId))
					{
						return new ResponseEntity<>("<a href='./images/" + setId.toUpperCase() + "/" + cardName + ".jpg'>" + cardName + "</a>", HttpStatus.OK);
					}
				else
					{
						return new ResponseEntity<>("Can't find: " + cardName + "<br />", HttpStatus.NOT_FOUND);
					}
			}

	@RequestMapping(value = "/getImageFiles/{setName}/{setId}", method = RequestMethod.GET)
	public ResponseEntity<String> getBulkImages(@PathVariable("setName") String setName,
                                                @PathVariable("setId") String setId)
			{
				Set<String> setList;
				Set<String> singleWordFailures = new HashSet<>();
				try {
					setList = downloadSetList(setName);
					System.out.println(setList.toString());
					setList.forEach(cardName -> {
						if (!downloadImageToFile(cardName, setId))
							{
								if (!cardName.contains(" ")) {
									singleWordFailures.add(cardName);
								}
							}
					});
				}
				catch (IOException e)
						{
							System.out.println("Unable to get set list for " + setName);
							e.printStackTrace();
							setList = new HashSet<>();
						}

				singleWordFailures.forEach(first ->
						singleWordFailures.forEach(second ->  downloadImageToFile(first + "--" + second, setId))
				);

				return new ResponseEntity<>(setList.stream()
						.map(s ->  s.replace(" ", "+"))
						.collect(Collectors.joining("\n")), HttpStatus.OK);
			}

	private Set<String> downloadSetList(String setName) throws IOException
			{
				Set<String> setList = new HashSet<>();
				for (int i = 0; i < 4; i++)
					{
						System.out.println("http://gatherer.wizards.com/Pages/Search/Default.aspx?page=" + i + "&output=checklist&set=[%22" + setName.replace("+", "%20") + "%22]");
						Document doc = Jsoup.connect("http://gatherer.wizards.com/Pages/Search/Default.aspx?page=" + i + "&output=checklist&set=[%22" + setName.replace("+", "%20") + "%22]").get();
						Elements cardNames = doc.body().getElementsByClass("nameLink");
						setList.addAll(cardNames.eachText());
					}

				return setList;
			}


	//add logic to either use caching (downloadImageToFile) or always pull image without caching (downloadImageFromLink)
	@RequestMapping("/getDeck/{id}")
	public ResponseEntity<byte[]> getNetDeckNoFiles(@PathVariable("id") Integer id,
                                                    @RequestParam(value = "grayscale", required = false, defaultValue = "false") Boolean grayscale,
                                                    @RequestParam(value = "highContrast", required = false, defaultValue = "false") Boolean contrast,
                                                    @RequestParam(value = "acrossQty", required = false, defaultValue = "3") Integer acrossQty,
                                                    @RequestParam(value = "downQty", required = false, defaultValue = "3") Integer downQty,
                                                    @RequestParam(value = "withLands", required = false, defaultValue = "false") Boolean withLands,
                                                    @RequestParam(value = "withSideboard", required = false, defaultValue = "false") Boolean withSideboard

    ) throws IOException
			{
				System.out.println("https://www.mtggoldfish.com/deck/" + id + "#paper");
				Document doc = Jsoup.connect("https://www.mtggoldfish.com/deck/" + id + "#paper").get();

				Map<MagicCard, BufferedImage> deckImages = new LinkedHashMap<>();

				boolean atSideboard = false;
				String deckName = doc.select("h2.deck-view-title").text().replace(" ", "+").replace("\"", "").trim();
				Elements rows = doc.select("#tab-paper .deck-view-deck-table tbody tr");
				for (Element row : rows)
					{
						if (!withSideboard && row.select("td").text().contains("Sideboard"))
							{
								atSideboard = true;
							}
						if (!row.select("td").hasClass("deck-header") && !atSideboard) {
							MagicCard card = new MagicCard();
							Integer qty = Integer.parseInt(row.select(".deck-col-qty").text());
							String cardLink = row.select(".deck-col-card a").attr("data-full-image");
							String cardName = deriveCardNameFromLink(cardLink);
							card.setName(cardName);
							card.setUsed(qty);
							card.setOwned(0);
							System.out.println("getting card: " + card.getName());
							try
									{
										if (withLands || !card.getName().matches("(Plains|Island|Swamp|Mountain|Forest) [0-9]*"))
											{
												deckImages.put(card, downloadImageAndCacheToFile(cardLink));
											}
									}
							catch (IOException e)
									{
										//ignore
									}

							if (row.select(".deck-col-card a").hasAttr("data-full-image1")) {
								card = new MagicCard();
								cardLink = row.select(".deck-col-card a").attr("data-full-image1");
								cardName = deriveCardNameFromLink(cardLink);
								card.setName(cardName);
								card.setUsed(qty);
								card.setOwned(0);
								System.out.println("getting card: " + card.getName());
								try
										{
											deckImages.put(card, downloadImageAndCacheToFile(cardLink));
										}
								catch (IOException e)
										{
											//ignore
										}
							}
						}
					}

				List<BufferedImage> images = manipulator.imageStitcher(deckImages, acrossQty, downQty, grayscale, contrast);

				byte[] pdf = manipulator.pdfMaker(images);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.parseMediaType("application/pdf"));
				String filename = deckName + "-" + id.toString() + ".pdf";
				headers.setContentDispositionFormData(filename, filename);
				headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
				return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
			}

	private BufferedImage downloadImageAndCacheToFile(String link) throws IOException
			{
				OkHttpClient client = HttpManager.getHttpClient();
				Request request = new Request.Builder()
						.url(link)
						.get()
						.build();
				Response response = client.newCall(request).execute();
				if (response.code() == 403)
					{
						response.body().close();
						return null;
					}

				String setName = deriveSetNameFromLink(link);
				String cardName = deriveCardNameFromLink(link);

				Path path = Paths.get("./images/" + setName + "/");
				//if directory exists?
				if (!Files.exists(path)) {
					try {
						Files.createDirectories(path);
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}
				String imgPath = "./images/" + setName + "/" + cardName.replace(" ", "+") + ".jpg";
				BufferedImage image = null;
				try {
					if (Files.exists(Paths.get(imgPath)))
						{
							System.out.println("File Exists: " + cardName.replace(" ", "+") + ".jpg");
						}
					else {
						System.out.println(link);
						Files.copy(response.body().byteStream(), Paths.get(imgPath));
					}
					image = ImageIO.read(new File(imgPath));
				}
				catch (FileAlreadyExistsException e)
						{
							System.out.println("File Exists: " + cardName.replace(" ", "+") + ".jpg");
						}
				catch (SocketTimeoutException e)
						{
							if (Files.exists(Paths.get(imgPath)))
								{
									Files.delete(Paths.get(imgPath));

								}
						}
				finally {
					response.body().close();
				}
				return  image;
			}

	private boolean downloadImageToFile(String cardName, String setId)
			{
				String url = "https://cdn1.mtggoldfish.com/images/gf/"
						+ cardName
						.replace("+", "%2B")
						.replace(" ", "%2B")
						.replace("--", "%2B%252F%252F%2B")
						.replace("'", "%2527")
						+ "%2B%255B" + setId.toUpperCase() + "%255D.jpg";
				try
						{
							downloadImageAndCacheToFile(url);
						}
				catch (IOException e)
						{
							return false;
						}
				return true;
			}

	private BufferedImage downloadImageFromLink(String link) throws IOException
			{
				return ImageIO.read(new URL(link));
			}

	private String deriveCardNameFromLink(String link)
			{
				return link.substring(link.indexOf("gf/") + 3, link.indexOf("%2B%255B"))
						.replace("%2B%252F%252F%2B", "--")
						.replace("%2527", "'")
						.replace("%252C", ",")
						.replace("%253E", "")
						.replace("%253C", "")
						.replace("%2B", " ");
			}

	private String deriveSetNameFromLink(String link)
			{
				return link.substring(link.indexOf("%2B%255B") + 8, link.indexOf("%255D"));
			}
}
