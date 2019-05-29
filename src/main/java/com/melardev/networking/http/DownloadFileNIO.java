package com.melardev.networking.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadFileNIO {

    private static final String IMG_URL = "https://upload.wikimedia.org/wikipedia/en/thumb/3/30/Java_programming_language_logo.svg/419px-Java_programming_language_logo.svg.png";

    public static void main(String[] args) {
        System.out.println(new File(".").getAbsolutePath());
        File f = new File("./downloads");

        if (!f.exists()) {
            if (!f.mkdir()) {
                System.err.println("Could not create folder at " + f.getAbsolutePath());
                return;
            }
        }

        File fout = new File("./downloads/java_logo_nio.png");
        if (fout.exists())
            fout.delete();

        ReadableByteChannel readableByteChannel = null;
        FileOutputStream fileOutputStream = null;
        URL url = null;
        InputStream is = null;
        try {
            fileOutputStream = new FileOutputStream(fout);
            url = new URL(IMG_URL);
            readableByteChannel = Channels.newChannel(url.openStream());
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            System.out.println("File created at " + fout.getAbsolutePath());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {

                if (readableByteChannel != null)
                    readableByteChannel.close();
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
