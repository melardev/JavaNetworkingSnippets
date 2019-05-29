package com.melardev.networking.sockets.tcp.async.select_serialize;

import com.melardev.networking.sockets.tcp.async.models.Packet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

    static ByteBuffer headerBuffer = ByteBuffer.allocate(4);
    static ByteBuffer payloadBuffer;
    private static boolean headerOk = false;

    public static void main(String[] args) {

        try {
            // Create new selector
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().setReuseAddress(true);
            // By default this is true, so set it to false for nio sockets
            serverSocketChannel.configureBlocking(false);
            InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
            // Bind to localhost and specified port
            serverSocketChannel.socket().bind(new InetSocketAddress(loopbackAddress, 3002));


            // ServerSocketChannel only supports OP_ACCEPT (see ServerSocketChannel::validOps())
            // it makes sense, server can only accept sockets
            int operations = SelectionKey.OP_ACCEPT;

            serverSocketChannel.register(selector, operations);
            while (true) {
                if (selector.select() <= 0) {
                    continue;
                }
                try {
                    processReadySet(selector.selectedKeys());
                } catch (IOException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void processReadySet(Set readySet) throws IOException {
        Iterator iterator = readySet.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            // After processing a key, it still persists in the Set, we wanna remove it
            // otherwise we will get it back the next time processReadySet is called
            // We would end up processing the same "event" as many times this method is called
            iterator.remove();
            // System.out.printf("isAcceptable %b isConnectable %b isReadable %b isWritable %b\n" , key.isAcceptable(), key.isConnectable(), key.isReadable(), key.isWritable());

            if (key.isAcceptable()) {
                ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();

                // Get the client socket channel
                SocketChannel clientSocketChannel = ssChannel.accept();
                // Configure it as non-blocking socket
                clientSocketChannel.configureBlocking(false);
                // Register the socket with the key selector, we want to get notified when we have
                // something to read from socket(OP_READ)
                clientSocketChannel.register(key.selector(), SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                // A Remote client has send us a message
                String message = "nothing";
                // Get the socket who sent the message
                SocketChannel sender = (SocketChannel) key.channel();

                int bytesCount = 0;
                try {
                    if (!headerOk) {
                        bytesCount = sender.read(headerBuffer);
                        if (headerBuffer.position() != headerBuffer.limit())
                            continue;

                        headerBuffer.flip();
                        int packetLength = headerBuffer.getInt();
                        payloadBuffer = ByteBuffer.allocate(packetLength);

                        // If you are reusing the same payloadBuffer make sure you set .limit like:
                        // now it does not make sense, because when we create a new ByteBuffer the limit is already
                        // set to capacity() which in this case is packetLength(argument to .allocate())
                        payloadBuffer.limit(packetLength);

                        headerOk = true;
                        headerBuffer.clear();
                    }

                    bytesCount = sender.read(payloadBuffer);
                    if (payloadBuffer.position() != payloadBuffer.limit())
                        continue;

                    if (bytesCount > 0) {


                        payloadBuffer.flip();
                        // For a demo of reusing the same buffer + serialization look the handlers_attachment demo
                        byte[] receivedMessageBytes = new byte[payloadBuffer.limit()];
                        payloadBuffer.get(receivedMessageBytes);

                        /* This also works, but I prefer one liner Apache Commons SerializationUtils
                        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(receivedMessageBytes));
                        Packet packet = (Packet) objectInputStream.readObject();
                        */
                        Packet packet = deserialize(receivedMessageBytes);
                        System.out.println(packet.message);

                        // Convert the ImageIcon into BufferedImage
                        BufferedImage bi = new BufferedImage(
                                packet.image.getIconWidth(),
                                packet.image.getIconHeight(),
                                BufferedImage.TYPE_INT_RGB);
                        Graphics g = bi.createGraphics();

                        packet.image.paintIcon(null, g, 0, 0);
                        g.dispose();

                        ImageIO.write(bi, "png", new File("D:/temp/output.png"));
                        System.out.println("[Server] Image saved!");
                        System.out.println("[Server] Received: " + packet.message);
                        payloadBuffer.clear();
                        headerOk = false;


                    } else {
                        SocketChannel ssChannel = (SocketChannel) key.channel();
                        ssChannel.close();
                        System.out.println("Server disconnected");
                        break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        sender.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    private static Packet deserialize(byte[] payload) {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Packet packet = (Packet) objectInputStream.readObject();
            return packet;
        } catch (IOException ex) {
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                byteArrayInputStream.close();
                if (objectInputStream != null)
                    objectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
