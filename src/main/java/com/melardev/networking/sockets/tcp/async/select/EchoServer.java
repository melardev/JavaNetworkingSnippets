package com.melardev.networking.sockets.tcp.async.select;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EchoServer {

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

            System.out.println("[Server] Listening ...");

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
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                int bytesCount = 0;

                try {
                    bytesCount = sender.read(buffer);

                    if (bytesCount > 0) {

                        // 1. Get manually
                        message = new String(buffer.array(), 0, bytesCount);

                        // 2. Or, use flip
                        // set buffer.position =0 and buffer.limit = bytesCount
                        buffer.flip();
                        byte[] receivedMessageBytes = new byte[bytesCount];
                        buffer.get(receivedMessageBytes);
                        message = new String(receivedMessageBytes);
                        System.out.println("[Server] Received: " + message);

                        // Writing
                        // 1. Easy approach, create a new ByteBuffer and send it
                        // ByteBuffer outputBuffer = ByteBuffer.wrap(message.getBytes());
                        // sender.write(outputBuffer);

                        // 2. Or to reuse the same buffer we could
                        // buffer.limit(buffer.position());
                        // buffer.position(0);

                        // 3. Or the same as point 2, but one line
                        buffer.flip();

                        // For big messages, write() will not write all at once,
                        // always make sure you wrap it in a while loop to make
                        // sure you send everything
                        int numberOfBytesWritten = 0;
                        while (numberOfBytesWritten < buffer.remaining())
                            numberOfBytesWritten += sender.write(buffer);


                    } else {
                        SocketChannel ssChannel = (SocketChannel) key.channel();
                        ssChannel.close();
                        System.out.println("Client disconnected");
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

}
