package com.melardev.networking.http;

import java.net.MalformedURLException;
import java.net.URL;

public class ParseUrlDemo {

    public static void main(String[] args) {
        try {
            printUrlInfo(new URL("http://tomcat:tomcat@myhost.com/path/to/file.php?search=parsing urls in java#Begin"));
            printUrlInfo(new URL("http://tomcat:tomcat@myhost.com:8080/path/to/file.php?search=parsing+urls+in+java#Begin"));
            // https://github.com/p4-team/ctf/tree/master/2016-12-27-33c3/web_400_list0r
            printUrlInfo(new URL("http://tomcat:tomcat@myhost.com@anotherdomain/path/to/file.php?search=parsing urls in java#Begin"));

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void printUrlInfo(URL url) {
        System.out.println(url.toString());
        System.out.println("Protocol: " + url.getProtocol());
        System.out.println("UserInfo: " + url.getUserInfo());
        System.out.println("Host: " + url.getHost());
        System.out.println("Port: " + url.getPort());
        System.out.println("Path: " + url.getPath());
        System.out.println("File: " + url.getFile());
        System.out.println("Query: " + url.getQuery());
        System.out.println("Authority: " + url.getAuthority());
        System.out.println("Reference: " + url.getRef());
    }
}
