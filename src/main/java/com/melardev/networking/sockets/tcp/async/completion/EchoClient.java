package com.melardev.networking.sockets.tcp.async.completion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

public class EchoClient {


    private static final Queue<String> messages = new ArrayDeque<>();
    private static boolean isSendingPacket = false;

    public static void main(String[] args) {
        try {

            AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();

            SocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 3002);
            AttachmentSocket attachment = new AttachmentSocket();

            attachment.channel = channel;
            attachment.bufferIn = ByteBuffer.allocate(1024);
            attachment.readHandler = new ReadHandler();
            attachment.writeHandler = new WriteHandler();
            attachment.bufferOut = ByteBuffer.allocate(1024);
            attachment.listeningThread = Thread.currentThread();
            channel.connect(serverAddr, attachment, new ConnectionHandler());

            attachment.listeningThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void onMaySendMessage(AttachmentSocket attachment) {
        synchronized (messages) {
            // If already sending a packet or the queue is empty, then return
            if (isSendingPacket || messages.isEmpty())
                return;

            byte[] message = messages.remove().getBytes(StandardCharsets.UTF_8);
            attachment.bufferOut.put(message);
            attachment.bufferOut.flip();
            isSendingPacket = true;
            attachment.channel.write(attachment.bufferOut, attachment, attachment.writeHandler);
        }
    }

    private static class AttachmentSocket {
        public AsynchronousSocketChannel channel;
        ByteBuffer bufferIn;
        ByteBuffer bufferOut;
        Thread listeningThread;
        ReadHandler readHandler;
        WriteHandler writeHandler;
    }

    private static class ConnectionHandler implements CompletionHandler<Void, AttachmentSocket> {
        @Override
        public void completed(Void result, AttachmentSocket attachment) {
            System.out.print("[Server] Please enter a  message  (exit to quit)");
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            attachment.channel.read(attachment.bufferIn, attachment, attachment.readHandler);
            while (!line.equals("exit")) {
                try {
                    line = consoleReader.readLine();
                    synchronized (messages) {
                        messages.add(line);
                    }
                    onMaySendMessage(attachment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            exc.printStackTrace();
        }
    }

    private static class ReadHandler implements CompletionHandler<Integer, AttachmentSocket> {
        @Override
        public void completed(Integer result, AttachmentSocket attachment) {

            if (result < 0) {
                failed(new ClosedChannelException(), attachment);
                return;
            }

            // Never use this code, array() returns always the whole underlying buffer
            // which in this demo is 1024, but we don't want that, we want only the contained data
            // which may be less than 1024 bytes. Let's think about it:
            // before this method being triggered, we called .read() with a bufferIn with position 0, and limit 1024
            // so the received data is placed from position 0 filling up the array up to a maximum of 1024.
            // If for example we received 10 bytes then at this point we had: position 10, limit 1024, capacity 1024
            // we want to read those 10 bytes only, so we have to set position to 0, limit to 10, and this is
            // done easily with flip() command which does limit=position;position=0; (see the internal source code)
            // If you call .array() you will get the whole array and artifacts would happen, here is how you could trigger
            // an artifact: send to the server a messages 10 bytes long, the server will echo 10 bytes long, it will look
            // that it works fine, but now, send less bytes, for example 5 bytes, the server will echo back 5 bytes.
            // if you try to print the .array() you will see that you have the new received data appended with the
            // remaining of the old data, why?? because you are reading the whole buffer, and the buffer still contains
            // the first received data(their first bytes were overridden, ok, but the second 5 bytes they are still there
            // and you are reading them)

            //String message = new String(attachment.buffer.array());
            //System.out.printf("Received From server: %s", message);


            // flip will set the position to 0, and the limit to the current pos, so we read only interesting
            attachment.bufferIn.flip();

            byte[] bytes = new byte[result];

            attachment.bufferIn.get(bytes);

            System.out.println("[Server] Received from Client: " + new String(bytes, StandardCharsets.UTF_8));

            // set pos to 0, limit to capacity(1024)
            attachment.bufferIn.clear();

            attachment.channel.read(attachment.bufferIn, attachment, attachment.readHandler);
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            System.err.println("Something went wrong " + exc.getMessage());
        }
    }

    static class WriteHandler implements CompletionHandler<Integer, AttachmentSocket> {
        @Override
        public void completed(Integer result, AttachmentSocket attachment) {
            if (result < 0)
                return;

            if (attachment.bufferOut.limit() != attachment.bufferOut.position()) {
                // Not all buffer was sent, reissue a write operation
                attachment.channel.write(attachment.bufferOut);
            } else {
                attachment.bufferOut.clear();
                isSendingPacket = false;
                onMaySendMessage(attachment);
            }
        }

        @Override
        public void failed(Throwable e, AttachmentSocket attach) {
            e.printStackTrace();
        }
    }
}


