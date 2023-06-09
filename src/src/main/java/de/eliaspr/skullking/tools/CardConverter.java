package de.eliaspr.skullking.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CardConverter {

    public static void run() {
        File input = new File("Z:\\SkullKing Karten\\BearbeitetTransparent");
        File output = new File("src/main/resources/de/eliaspr/skullking/res/cards/");
        if (!output.exists()) {
            output.mkdirs();
        } else {
            for (File f : output.listFiles()) {
                f.delete();
            }
        }

        int threads = 10;
        ArrayList<ArrayList<File>> files = new ArrayList<ArrayList<File>>();
        int i;
        for (i = 0; i < threads; i++) {
            files.add(new ArrayList<>());
        }
        i = 0;
        for (File img : input.listFiles()) {
            if (img.getName().toLowerCase().endsWith("png")) {
                files.get(i++).add(img);
                if (i >= threads) {
                    i = 0;
                }
            }
        }

        for (i = 0; i < threads; i++) {
            final ArrayList<File> list = files.get(i);
            new Thread(() -> {
                for (File img : list) {
                    System.out.println(img.getName());
                    BufferedImage bufImg = null;
                    try {
                        bufImg = ImageIO.read(img);
                        BufferedImage destImg = new BufferedImage(280, (int) (280f * 88f / 56f), BufferedImage.TYPE_INT_ARGB); // card size: 88x56mm
                        destImg.getGraphics().drawImage(bufImg, 0, 0, destImg.getWidth(), destImg.getHeight(), null);
                        File out = new File(output, img.getName().toLowerCase());
                        ImageIO.write(destImg, "PNG", out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "th" + i).start();
        }
    }

}
