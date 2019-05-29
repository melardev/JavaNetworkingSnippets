package com.melardev.networking.sockets.tcp.async.select_basic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientNIO {

    public void startClient()

            throws IOException, InterruptedException {


        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8090);

        SocketChannel client = SocketChannel.open(hostAddress);


        System.out.println("Client... started");


        String threadName = Thread.currentThread().getName();


        // Send messages to server

        String[] messages = new String[]

                {threadName + ": test1", threadName + ": test2", threadName + ": test3"};


        for (int i = 0; i < messages.length; i++) {
            byte[] message = messages[i].getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(message);
            client.write(buffer);
            System.out.println(messages[i]);
            buffer.clear();
            Thread.sleep(5000);
        }
        client.close();
    }
}
