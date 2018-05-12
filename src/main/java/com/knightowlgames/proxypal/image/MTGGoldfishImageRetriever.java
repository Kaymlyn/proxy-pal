package com.knightowlgames.proxypal.image;

import com.knightowlgames.proxypal.HttpManager;
import com.knightowlgames.proxypal.datatype.MagicCard;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mtggoldfish/")
public class MTGGoldfishImageRetriever {

    ImageManipulator manipulator = new ImageManipulator();

    @RequestMapping(value = "/getImageFile/{cardName}/{setId}", method = RequestMethod.GET)
    public ResponseEntity<String> getImage(@PathVariable("cardName") String cardName,
                                           @PathVariable("setId") String setId) throws IOException
    {
        downloadImage(cardName,setId);
        return new ResponseEntity<>("<a href='./images/" + setId.toUpperCase() + "/" + cardName + ".jpg'>" + cardName + "</a>", HttpStatus.OK);
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
                try {
                    if(!downloadImage(cardName, setId))
                    {
                        if (!cardName.contains(" ")) {
                            singleWordFailures.add(cardName);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Unable to get " + cardName);
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
            singleWordFailures.forEach(second -> {
                try {
                    downloadImage(first + "--" + second, setId);
                }
                catch (IOException e)
                {
                    //ignore
                }
            })
        );

        return new ResponseEntity<>(setList.stream()
                .map(s ->  s.replace(" ", "+"))
                .collect(Collectors.joining("\n")), HttpStatus.OK);
    }

    private Set<String> downloadSetList(String setName) throws IOException
    {
        Set<String> setList = new HashSet<>();
        for(int i = 0; i < 4; i++)
        {
            System.out.println("http://gatherer.wizards.com/Pages/Search/Default.aspx?page=" + i + "&output=checklist&set=[%22" + setName.replace("+","%20")+ "%22]");
            Document doc = Jsoup.connect("http://gatherer.wizards.com/Pages/Search/Default.aspx?page=" + i + "&output=checklist&set=[%22" + setName.replace("+","%20")+ "%22]").get();
            Elements cardNames = doc.body().getElementsByClass("nameLink");
            setList.addAll(cardNames.eachText());
        }

        return setList;
    }

    private boolean downloadImage(String cardName, String setId) throws IOException
    {
        OkHttpClient client = HttpManager.getHttpClient();
        String url = "https://cdn1.mtggoldfish.com/images/gf/"
                + cardName
                    .replace("+", "%2B")
                    .replace(" ", "%2B")
                    .replace("--", "%2B%252F%252F%2B")
                    .replace("'", "%2527")
                + "%2B%255B" + setId.toUpperCase() + "%255D.jpg";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if(response.code()== 403)
        {
            response.body().close();
            return false;
        }
        Path path = Paths.get("./images/" + setId.toUpperCase() + "/");
        //if directory exists?
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        String imgPath = ".images/" + setId.toUpperCase() + "/" + cardName.replace(" ", "+") + ".jpg";
        try {
            if(Files.exists(Paths.get(imgPath)))
            {
                System.out.println("File Exists: " + cardName.replace(" ", "+") + ".jpg");
            }
            else {
                System.out.println(url);
                Files.copy(response.body().byteStream(), Paths.get(imgPath));
            }
        }
        catch (FileAlreadyExistsException e)
        {
            System.out.println("File Exists: " + cardName.replace(" ", "+") + ".jpg");
        }
        catch (SocketTimeoutException e)
        {
            if(Files.exists(Paths.get(imgPath)))
            {
                Files.delete(Paths.get(imgPath));

            }
        }
        finally {
            response.body().close();
        }
        return true;
    }

    @RequestMapping("/getDeck/{id}")
    public ResponseEntity<byte[]> getNetDeckNoFiles(@PathVariable("id") Integer id,
                                                    @RequestParam(value = "grayscale", required = false, defaultValue = "false") Boolean grayscale,
                                                    @RequestParam(value = "highContrast", required = false, defaultValue = "false") Boolean contrast,
                                                    @RequestParam(value = "acrossQty", required = false, defaultValue = "3") Integer acrossQty,
                                                    @RequestParam(value = "downQty", required = false, defaultValue = "3") Integer downQty,
                                                    @RequestParam(value = "withLands", required = false, defaultValue = "false") Boolean withLands
    ) throws IOException
    {
        System.out.println("https://www.mtggoldfish.com/deck/" + id + "#paper");
        Document doc = Jsoup.connect("https://www.mtggoldfish.com/deck/" + id + "#paper").get();

        Map<MagicCard, BufferedImage> deckImages = new HashMap<>();

        Elements rows = doc.select("#tab-paper .deck-view-deck-table tbody tr");
        rows.forEach(tr -> {
            if(!tr.select("td").hasClass("deck-header")) {
                MagicCard card = new MagicCard();
                Integer qty = Integer.parseInt(tr.select(".deck-col-qty").text());
                String cardLink = tr.select(".deck-col-card a").attr("data-full-image");
                String cardName = cardLink.substring(cardLink.indexOf("gf/") + 3, cardLink.indexOf("%2B%255B"))
                        .replace("%2B%252F%252F%2B", "--")
                        .replace("%2527", "'")
                        .replace("%252C", ",")
                        .replace("%253E", "")
                        .replace("%253C", "")
                        .replace("%2B", " ");
                card.setName(cardName);
                card.setUsed(qty);
                card.setOwned(0);
                System.out.println("getting card: " + card.getName());
                try
                {
                    if(withLands || !card.getName().matches("(Plains|Island|Swamp|Mountain|Forest) [0-9]*"))
                    {
                        deckImages.put(card, downloadImageFromLink(cardLink));
                    }
                }
                catch (IOException e)
                {
                    //ignore
                }

                if (tr.select(".deck-col-card a").hasAttr("data-full-image1")) {
                    card = new MagicCard();
                    cardLink = tr.select(".deck-col-card a").attr("data-full-image1");
                    cardName = cardLink.substring(cardLink.indexOf("gf/") + 3, cardLink.indexOf("%2B%255B"))
                            .replace("%2B%252F%252F%2B", "--")
                            .replace("%2527", "'")
                            .replace("%252C", ",")
                            .replace("%253E", "")
                            .replace("%253C", "")
                            .replace("%2B", " ");
                    card.setName(cardName);
                    card.setUsed(qty);
                    card.setOwned(0);
                    System.out.println("getting card: " + card.getName());
                    try
                    {
                        deckImages.put(card, downloadImageFromLink(cardLink));
                    }
                    catch (IOException e)
                    {
                        //ignore
                    }
                }
            }
        });

        List<BufferedImage> images = manipulator.imageStitcher(deckImages, acrossQty, downQty, grayscale, contrast);

        PDDocument document = new PDDocument();
        images.forEach(image -> {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            try {
                PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(imageObject, 0, 0);
                contentStream.close();
            }
            catch (IOException e) {
                //ignore
            }
            document.addPage(page);
        });

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.save(byteArrayOutputStream);
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        String filename = id.toString() + ".pdf";
        headers.setContentDispositionFormData(filename, filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers ,HttpStatus.OK);
    }

    private BufferedImage downloadImageFromLink(String link) throws IOException
    {
        return ImageIO.read(new URL(link));
    }

    private void downloadImageFromLink(String link, String deckName) throws IOException
    {
        OkHttpClient client = HttpManager.getHttpClient();
        String cardName = link.substring(link.indexOf("gf/") + 3, link.indexOf("%2B%255B"))
                .replace("%2B%252F%252F%2B", "--")
                .replace("%2527", "'")
                .replace("%252C", ",")
                .replace("%253E", "")
                .replace("%253C", "")
                .replace("%2B", "+") + ".jpg";

        Request request = new Request.Builder()
                .url(link)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        Path path = Paths.get("images/" + deckName + "/");

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String imgPath = "images/" + deckName + "/" + cardName;
        try {
            if(Files.exists(Paths.get(imgPath)))
            {
                System.out.println("File Exists: " + cardName);
            }
            else {
                System.out.println(Paths.get(imgPath).toAbsolutePath());
                Files.copy(response.body().byteStream(), Paths.get(imgPath).toAbsolutePath());
            }
        }
        catch (FileAlreadyExistsException e)
        {
            System.out.println("File Exists: " + cardName.replace(" ", "+") + ".jpg");
        }
        catch (SocketTimeoutException e)
        {
            if(Files.exists(Paths.get(imgPath)))
            {
                Files.delete(Paths.get(imgPath));

            }
        }
        finally {
            response.body().close();
        }
    }
}
