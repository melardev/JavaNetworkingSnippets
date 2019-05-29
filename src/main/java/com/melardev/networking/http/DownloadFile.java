package com.melardev.networking.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadFile {

    private static final String IMG_URL = "https://upload.wikimedia.org/wikipedia/en/thumb/3/30/Java_programming_language_logo.svg/419px-Java_programming_language_logo.svg.png";

    public static void main(String[] args) {
        System.out.println(new File(".").getAbsolutePath());
        File f = new File("./downloads");
        if (!f.exists()) {
            if (!f.mkdir()) {
                System.err.println("Could not create path at " + f.getAbsolutePath());
                return;
            }

        }
        File fileOutput = new File("./java_logo_io.png");
        if (fileOutput.exists())
            fileOutput.delete();

        FileOutputStream fileOutputStream = null;
        URL url = null;
        InputStream is = null;
        try {
            fileOutputStream = new FileOutputStream(fileOutput);
            url = new URL(IMG_URL);
            is = url.openStream();
            fileOutputStream.write(is.readAllBytes());

            // readAllBytes is available since Java 9
            // Before that, what we ere doing was:
            /*
            byte[] bytes = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(bytes)) != -1)
                fos.write(bytes, 0, bytesRead);
                */


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();

                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
