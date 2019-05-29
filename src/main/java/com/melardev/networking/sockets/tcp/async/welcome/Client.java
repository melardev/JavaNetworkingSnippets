package com.melardev.networking.sockets.tcp.async.welcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    public static void main(String[] args) {

        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            InetSocketAddress addr = new InetSocketAddress("localhost", 3002);
            socketChannel.connect(addr);

            ByteBuffer receivingBuffer = ByteBuffer.allocate(1024);

            while (socketChannel.read(receivingBuffer) != -1) {
                receivingBuffer.flip();

                // please not .position() at this point will always be 0, because of flip().
                // Read NIO basics if you don't know why or what does it means limit position

                System.out.println("[Client] Received: "
                        + new String(receivingBuffer.array(), receivingBuffer.position(), receivingBuffer.limit()));

                receivingBuffer.clear();
            }


        } catch (IOException ioe) {
            System.err.println("I/O error: " + ioe.getMessage());
        } finally {

            try {
                if (socketChannel != null) socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("[Client] Exiting Client");
        }
    }
}

