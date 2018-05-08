package com.knightowlgames.proxypal.image;

import com.knightowlgames.proxypal.HttpManager;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImageManipulator {

    @RequestMapping(value = "/getImage/{cardName}/{setId}", method = RequestMethod.GET)
    public ResponseEntity<String> getImage(@PathVariable("cardName") String cardName,
                                           @PathVariable("setId") String setID) throws IOException
    {
        OkHttpClient client = HttpManager.getHttpClient();
        String url = "https://cdn1.mtggoldfish.com/images/gf/"
                + cardName.replace("+", "%2B")
                + "%2B%255B" + setID.toUpperCase() + "%255D.jpg";
        System.out.println(url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        Path path = Paths.get("./images/" + setID.toUpperCase() + "/");
        //if directory exists?
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                //fail to create directory
                e.printStackTrace();
            }
        }
        Files.copy(response.body().byteStream(), Paths.get("./images/" + setID.toUpperCase() + "/" + cardName + ".jpg"));
        return new ResponseEntity<>("./images/" + setID.toUpperCase() + "/" + cardName + ".jpg", HttpStatus.OK);
    }

}
