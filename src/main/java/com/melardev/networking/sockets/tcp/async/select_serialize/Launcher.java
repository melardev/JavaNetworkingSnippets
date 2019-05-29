package com.melardev.networking.sockets.tcp.async.select_serialize;


public class Launcher {

    public static void main(String[] args) {
        try {
            new Thread(() -> {
                Server.main(null);
            }).start();

            Thread.sleep(2000);

            new Thread(() -> {
                Client.main(null);
            }).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
