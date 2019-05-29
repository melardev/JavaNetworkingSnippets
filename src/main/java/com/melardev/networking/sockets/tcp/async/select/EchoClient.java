package com.melardev.networking.sockets.tcp.async.select;

import java.io.IOException;
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

public class EchoClient {
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
            int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;

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
            } else if (key.isReadable())
                onRead(key);
            if (key.isWritable())
                onWrite(key);
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
                System.out.println("[Client] Write messages to send to server");
                String line = "";
                while (!line.equals("exit")) {
                    line = scanner.nextLine();
                    ByteBuffer bufferToSend = ByteBuffer.wrap(line.trim().getBytes());
                    int numberOfBytesWritten = 0;

                    // For big messages, write() will not write all at once,
                    // always make sure you wrap it in a while loop to make
                    // sure you send everything
                    while (numberOfBytesWritten < bufferToSend.remaining()) {
                        numberOfBytesWritten += channel.write(bufferToSend);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
        System.out.println("[Client] Received: " + receivedMessage);
    }

    // Not used here
    private static void onWrite(SelectionKey key) throws IOException {
        System.out.println("You just send a message");
    }

}
