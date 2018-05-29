package com.knightowlgames.proxypal.image;

import com.knightowlgames.proxypal.datatype.MagicCard;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
public class ImageManipulator {

    private int margin = 5;
    private float cardScaleFactor;
    private float finalWidth = 180f;

    public ImageManipulator()
    {
        cardScaleFactor = 1;
    }

    public void setCardWidth(Float cardWidth)
    {
        cardScaleFactor = finalWidth/cardWidth;
    }

    public byte[] pdfMaker(List<BufferedImage> images)
    {
        PDDocument document = new PDDocument();

        images.forEach(image -> {
            PDPage page = new PDPage(new PDRectangle((cardScaleFactor)*image.getWidth(), (cardScaleFactor)*image.getHeight()));

            try {
                PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(imageObject, 0, 0,(cardScaleFactor)*image.getWidth(), (cardScaleFactor)*image.getHeight());
                contentStream.close();
            }
            catch (IOException e) {
                //ignore
            }

            document.addPage(page);
        });

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            document.save(byteArrayOutputStream);
            document.close();
        }
        catch (IOException e)
        {
            //ignore
        }

        return byteArrayOutputStream.toByteArray();
    }

    public List<BufferedImage> imageStitcher(Map<MagicCard, BufferedImage> cardImage,
                                             int imagesWide,
                                             int imagesTall,
                                             boolean grayscale,
                                             boolean contrast)
    {
        List<BufferedImage> pages = new ArrayList<>();
        if(imagesWide < 1 || imagesTall < 1)
        {
            return null;
        }

        int imgCount = 0;
        int imgNumber = 0;
        List<MagicCard> cardSet = Arrays.asList(cardImage.keySet().toArray(new MagicCard[0]));
        while(imgNumber < cardSet.size() && cardSet.get(imgNumber).getOwned() >= cardSet.get(imgNumber).getUsed())
        {
            imgNumber ++;
        }
        int imageWidth = cardImage.get(cardSet.get(0)).getWidth();
        int imageHeight = cardImage.get(cardSet.get(0)).getHeight();

        while(imgNumber < cardSet.size())
        {
            BufferedImage compiledImage = new BufferedImage((imagesWide * imageWidth) + (margin * (imagesWide -1)),
                    (imagesTall * imageHeight) + (margin * (imagesTall - 1)),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = compiledImage.createGraphics();
            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, compiledImage.getWidth(), compiledImage.getHeight());
            for (int i = 0; i < imagesTall; i++)
            {
                for (int j = 0; j < imagesWide; j++)
                {
                    if(imgNumber >= cardSet.size())
                    {
                        break;
                    }

                    BufferedImage card = cardImage.get(cardSet.get(imgNumber));
                    compiledImage.createGraphics().drawImage(adjustImage(card, grayscale, contrast), j * (margin + imageWidth), i * (margin + imageHeight), null);

                    imgCount ++;
                    while(imgNumber < cardSet.size() && cardSet.get(imgNumber).getOwned() + imgCount >= cardSet.get(imgNumber).getUsed())
                    {
                        imgNumber ++;
                        imgCount = 0;
                    }

                    if(imgNumber >= cardSet.size())
                    {
                        break;
                    }
                }
                if(imgNumber >= cardSet.size())
                {
                    break;
                }
            }
            pages.add(compiledImage);
        }

        return pages;
    }

    private BufferedImage adjustImage(BufferedImage image, boolean grayscale, boolean contrast) {

        BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D bGr = finalImage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();

        if(grayscale)
        {
            for(int y = 0; y < finalImage.getHeight(); y++) {
                for (int x = 0; x < finalImage.getWidth(); x++) {
                    int p = finalImage.getRGB(x, y);

                    int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;

                    //calculate average
                    int avg = (r + g + b) / 3;

                    if (contrast) {
                        if (avg < 48) {
                            avg = 0;
                        } else if (avg > 207) {
                            avg = 255;
                        }
                    }
                    //replace RGB value with avg
                    p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                    finalImage.setRGB(x, y, p);
                }
            }
        }

        return finalImage;
    }
}

