package com.melardev.networking.sockets.tcp.async.completion_simple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Scanner;

public class AsyncTcpClientCallbacks {

    private static AsynchronousSocketChannel socketChannel;
    static CompletionHandler<Integer, ByteBuffer> readHandler = new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer bytesReaded, ByteBuffer buffer) {
            buffer.flip();
            byte[] receivedBytes = new byte[buffer.limit()];
            // Get into receivedBytes
            buffer.get(receivedBytes);
            String message = new String(receivedBytes);
            System.out.println(message);

            buffer.clear();
            socketChannel.read(buffer, buffer, this);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer buffer) {
            System.err.println("Error reading message");
            System.exit(1);
        }

    };
    static private CompletionHandler<Integer, Void> writeHandler = new CompletionHandler<Integer, Void>() {
        @Override
        public void completed(Integer bytesWritten, Void attachment) {

        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            System.err.println("Something went wrong");
            System.exit(-1);
        }
    };

    public static void main(String[] args) {

        try {
            socketChannel = AsynchronousSocketChannel.open();


            //try to connect to the server side
            socketChannel.connect(new InetSocketAddress("localhost", 3002), null
                    , new CompletionHandler<Void, Void>() {
                        @Override
                        public void completed(Void result, Void attachment) {

                            ByteBuffer receivedBuffer = ByteBuffer.allocate(1024);
                            socketChannel.read(receivedBuffer, receivedBuffer, readHandler);

                            Scanner scanner = new Scanner(System.in);
                            System.out.println("Write messages to send to server");

                            String line = "";
                            do {
                                line = scanner.nextLine();
                                byte[] bytesToWrite = line.getBytes();

                                ByteBuffer bufferToSend = ByteBuffer.allocate(bytesToWrite.length);
                                bufferToSend.put(bytesToWrite);
                                // System.out.println(bufferToSend.limit());
                                bufferToSend.flip();
                                // System.out.println(bufferToSend.limit());

                                socketChannel.write(bufferToSend, null, writeHandler);
                            }
                            while (!line.equals("exit"));
                        }

                        @Override
                        public void failed(Throwable exc, Void nothing) {
                            System.out.println("Error connection to host");
                        }

                    });

            while (true) {
                try {
                    Thread.sleep(60 * 1000);
                    // Sleep 1 min ... who cares ?
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
