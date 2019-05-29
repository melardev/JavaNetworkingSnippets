package com.melardev.networking.sockets.udp.sync.basic;

public class Launcher {

    public static void main(String[] args) {
        try {
            new Thread(() -> EchoServer.main(null)).start();

            Thread.sleep(2000);

            new Thread(() -> {
                EchoClient.main(null);
            }).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
