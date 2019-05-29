package com.melardev.networking.sockets.tcp.async.select_serialize;


import com.melardev.networking.sockets.tcp.async.models.Packet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Client {
    public static void main(String[] args) {

        SocketChannel channel = null;
        try {
            InetAddress serverIPAddress = InetAddress.getByName("localhost");
            InetSocketAddress serverAddress = new InetSocketAddress(
                    serverIPAddress, 3002);
            Selector selector = Selector.open();
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(serverAddress);

            // SocketChannel supports OP_CONNECT , and Read
            // OP_WRITE will always be enabled after we connected, this will flood our loop, so skip it
            // I dont care about reading neither, in this demo
            int operations = SelectionKey.OP_CONNECT;

            channel.register(selector, operations);
            while (true) {
                try {
                    if (selector.select() > 0) {
                        processSelectedKeys(selector.selectedKeys());
                    }
                } catch (IOException e) {
                    // If server closed connection then exit, if our user does not have connection retry later
                    e.printStackTrace();
                    break;
                }
            }
        } catch (
                UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void processSelectedKeys(Set selectedKeySet) throws
            IOException {
        Iterator iterator = selectedKeySet.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            iterator.remove();
            // System.out.printf("isAcceptable %b isConnectable %b isReadable %b isWritable %b\n" , key.isAcceptable(), key.isConnectable(), key.isReadable(), key.isWritable());

            if (key.isConnectable()) {
                onConnect(key);
            }
        }

    }

    public static void onConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        while (channel.isConnectionPending()) {
            channel.finishConnect();
        }

        new Thread(() -> {

            try {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Write messages to send to server");
                String line = "";
                while (!line.equals("exit")) {
                    line = scanner.nextLine();
                    Packet packet = new Packet();
                    packet.message = line;
                    packet.X = 100;
                    packet.Y = 20;
                    packet.image = new ImageIcon(ImageIO.read(new File("D:/temp/image.png")));

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ObjectOutput objectOutput = new ObjectOutputStream(outputStream);
                    objectOutput.writeObject(packet);
                    objectOutput.flush();

                    byte[] resultt = outputStream.toByteArray();

                    byte[] result = serialize(packet);
                    ByteBuffer bufferToSend = ByteBuffer.allocate(4 + result.length);
                    bufferToSend.putInt(result.length);
                    bufferToSend.put(result);
                    bufferToSend.flip();
                    int written = 0;

                    while (written < bufferToSend.limit())
                        written += channel.write(bufferToSend);

                    outputStream.close();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static byte[] serialize(Packet packet) {
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

    public static void onRead(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        clientSocketChannel.read(buffer);
        buffer.flip();
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = decoder.decode(buffer);
        String receivedMessage = charBuffer.toString();
        System.out.println(receivedMessage);
    }


}
