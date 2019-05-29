package com.melardev.networking.sockets.tcp.async.welcome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {

    public static void main(String[] args) {

        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            ServerSocket ss = serverSocketChannel.socket();
            ss.bind(new InetSocketAddress(3002));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                int n = selector.select();
                if (n == 0)
                    continue;
                Iterator keyIterator = selector.selectedKeys().iterator();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    if (key.isAcceptable()) {

                        SocketChannel socketChannel;
                        socketChannel = ((ServerSocketChannel) key.channel()).accept();

                        if (socketChannel == null)
                            continue;

                        System.out.println("[Server] client accepted");

                        byteBuffer.clear();
                        System.out.println("[Server] Sending message to client");
                        byteBuffer.put("Welcome to this Welcome-r Select Server v.0.0".getBytes());
                        byteBuffer.flip();

                        while (byteBuffer.hasRemaining())
                            socketChannel.write(byteBuffer);

                        System.out.println("[Server] Closing current client connection");
                        socketChannel.close();
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
