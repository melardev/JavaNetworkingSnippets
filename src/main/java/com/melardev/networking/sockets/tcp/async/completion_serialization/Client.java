package com.melardev.networking.sockets.tcp.async.completion_serialization;


import com.melardev.networking.sockets.tcp.async.models.Packet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;


public class Client {

    public static void main(String[] args) {
        AsynchronousSocketChannel channel = null;
        try {
            channel = AsynchronousSocketChannel.open();

            SocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 3002);
            AttachmentSocket attachment = new AttachmentSocket();

            attachment.channel = channel;
            attachment.writeHandler = new WriteHandler();
            attachment.listeningThread = Thread.currentThread();
            channel.connect(serverAddr, attachment, new ConnectionHandler());

            try {
                attachment.listeningThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class AttachmentSocket {
        ByteBuffer bufferOut;
        Thread listeningThread;
        WriteHandler writeHandler;
        AsynchronousSocketChannel channel;
        int totalBytesToSend;
    }

    private static class ConnectionHandler implements CompletionHandler<Void, AttachmentSocket> {
        @Override
        public void completed(Void result, AttachmentSocket attachment) {
            System.out.print("Please enter a  message  (exit to quit)");
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            while (!line.equals("exit")) {
                try {
                    line = consoleReader.readLine();

                    Packet packet = new Packet();
                    packet.message = line;
                    packet.X = 43;
                    packet.Y = 21;
                    packet.image = new ImageIcon(ImageIO.read(new File("D:/temp/image.png")));

                    byte[] objectBytes = serialize(packet);
                    System.out.println("Payload is " + objectBytes.length + " bytes long");
                    attachment.totalBytesToSend = 4 + objectBytes.length;

                    // Create buffer when null or when the buffer we have does not fit the whole data we wanna send
                    if (attachment.bufferOut == null || attachment.bufferOut.capacity() < 4 + objectBytes.length)
                        attachment.bufferOut = ByteBuffer.allocate(4 + objectBytes.length);

                    attachment.bufferOut.putInt(objectBytes.length);
                    attachment.bufferOut.put(objectBytes);

                    attachment.bufferOut.flip();
                    attachment.channel.write(attachment.bufferOut, attachment, attachment.writeHandler);

                    // Wait until the write has completed, the bufferOut.position is set to 0 in the WriteHandler
                    while (attachment.bufferOut.position() != 0) {
                        Thread.sleep(500);
                        continue;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private byte[] serialize(Packet packet) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = null;
            try {
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

                objectOutputStream.writeObject(packet);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (objectOutputStream != null)
                        objectOutputStream.close();
                    if (byteArrayOutputStream != null)
                        byteArrayOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return byteArrayOutputStream.toByteArray();
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            exc.printStackTrace();
        }

    }


    static class WriteHandler implements CompletionHandler<Integer, AttachmentSocket> {
        @Override
        public void completed(Integer result, AttachmentSocket attachment) {
            if (result < 0)
                return;

            if (attachment.bufferOut.position() != attachment.totalBytesToSend) {
                // Not all buffer was sent, reissue a write operation
                attachment.channel.write(attachment.bufferOut);
            } else {
                attachment.bufferOut.clear();
            }
        }

        @Override
        public void failed(Throwable e, AttachmentSocket attach) {
            e.printStackTrace();
        }
    }
}
