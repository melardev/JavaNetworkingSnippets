package com.melardev.networking.sockets.tcp.async.completion;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.ArrayDeque;
import java.util.Queue;

public class EchoServer {

    /**
     * You always have to keep in mind the position, limit concepts, otherwise you will end up with bugs, or crashes
     * The .read() calls, put data into the specified buffer, from its position up to its limit.
     * Each .write() call, takes data from the buffer, from its position up to its limit, and sends it.
     * With that in mind, we have to make sure:
     * - Before each read, the buffer position is set to 0, limit is set to the capacity of the buffer
     * Why? Because we want to use the whole buffer as a placeholder, the way we say to the framework this is the space
     * you can use to store data is through an interval defined by [position, limit]
     * - Before a write, we have to make sure the previous write sent everything, how? through hasRemaining() which translated to
     * position < limit ?, Let's say we had a buffer of capacity 1024, but we have 6 characters there which we wanted to send
     * write() call will try to send from position to limit, so we make sure, position is set to 0, and limit is set to 6.
     * what may happen, is that write() didn't finish sending the whole thing, let's say it only sent 3 bytes, in that
     * case position will be 3, and limit still 6. This is why we check if position < limit, if it is, then we issue
     * another write() until we sent everything.
     * At the end, if we want to reuse the same buffer, well, don't forget that read and write() move the position each
     * time they run, so to reuse them, make sure position=0, limit=capacity, this is achieved through clear()
     */
    public static void main(String[] args) {
        try {
            AsynchronousServerSocketChannel asynchronousServerSocketChannel =
                    AsynchronousServerSocketChannel.open();
            asynchronousServerSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            asynchronousServerSocketChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    3002));

            AttachmentSocket attachment = new AttachmentSocket(asynchronousServerSocketChannel,
                    new ReadHandler(), new WriteHandler(),
                    ByteBuffer.allocate(1024), ByteBuffer.allocate(1024),
                    Thread.currentThread());


            asynchronousServerSocketChannel.accept(attachment,
                    new ConnectionHandler());

            attachment.listeningThread.join();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void onMaySendMessage(AttachmentSocket attachment) {
        synchronized (attachment.messages) {
            if (attachment.isSendingMessage || attachment.messages.isEmpty())
                return;

            attachment.outputBuffer.clear();
            attachment.outputBuffer.put(attachment.messages.remove().getBytes());
            attachment.isSendingMessage = true;
            // pos is now the length of bytes you wrote so far, limit is capacity(1024)
            // we want to set pos to 0(to send from beginning of the buffer which is where you began writing bytes)
            // up to where you finished writing(your current position), this is done through flip(), explore its source
            // code to learn more
            attachment.outputBuffer.flip();
            attachment.socketChannel.write(attachment.outputBuffer, attachment, attachment.writeHandler);
        }
    }

    public static class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AttachmentSocket> {

        @Override
        public void completed(AsynchronousSocketChannel socketClient, AttachmentSocket attachment) {
            System.out.println("[Client] Got a connection ");

            // The previous .accept() is now consumed, if we want to be able to accept another client
            // We must schedule it again, let's make another async accept then
            attachment.server.accept(new AttachmentSocket(attachment.server,
                    new ReadHandler(), new WriteHandler(),
                    ByteBuffer.allocate(1024), ByteBuffer.allocate(1024),
                    attachment.listeningThread), this);

            attachment.socketChannel = socketClient;

            socketClient.read(attachment.inputBuffer, attachment, attachment.readHandler);
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            System.out.println("[Client] ConnectionHandler::failed");
        }
    }

    static class ReadHandler implements CompletionHandler<Integer, AttachmentSocket> {


        @Override
        public void completed(Integer bytesRead, AttachmentSocket attachment) {
            // at this moment in time inputBuffer.position will be set to bytesRead
            // and limit is = capacity(1024)

            ByteBuffer bufferIn = attachment.inputBuffer;
            ByteBuffer bufferOut = attachment.outputBuffer;
            // flip will set the position to 0, and the limit to the current pos(bytesRead)
            // so we read only interesting
            bufferIn.flip();
            byte[] inComingData = new byte[bytesRead]; // bytesRead == inputBuffer.position(before flip)

            // Move from position(0) to limit(bytesRead) bytes to our byte[]
            bufferIn.get(inComingData);

            // We want to reuse this ByteBuffer, so make sure pos=0; limit=capacity=1024
            // To be able to read a message up to 1024 bytes(from pos to limit)
            bufferIn.clear();

            String message = new String(inComingData);

            System.out.println("[Client] Received from client: " + message);
            if (message.equals("exit")) {
                attachment.listeningThread.interrupt();
                try {
                    attachment.socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            synchronized (attachment.messages) {
                attachment.messages.add(message);
            }


            onMaySendMessage(attachment);

            attachment.socketChannel.read(attachment.inputBuffer, attachment, attachment.readHandler);
        }


        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            System.err.println("[Client] Something went wrong ReadHandler::failed()");
            exc.printStackTrace();
        }
    }

    static class WriteHandler implements CompletionHandler<Integer, AttachmentSocket> {

        @Override
        public void completed(Integer bytesWritten, AttachmentSocket attachment) {
            // outputBuffer.position is now set to bytesWritten ( the first time )


            if (bytesWritten < 0) { // if bytesWritten, there was an error
                failed(new ClosedChannelException(), attachment);
            } else if (bytesWritten > 0) {
                // We have to make sure we sent everything
                // from position to limit, because that was the intent, if we hadn't, make another .write() call

                AsynchronousSocketChannel channel = attachment.socketChannel;

                if (attachment.outputBuffer.hasRemaining()) {
                    // very important, do not reset the position to 0 yet, we have to still send
                    // the remaining data, remaining is from position to limit
                    channel.write(attachment.outputBuffer, attachment, this);
                } else {
                    // We have sent everything from position to limit(here position is == limit)
                    // to be able to reuse this buffer set the position to 0, and the limit to the capacity(1024)
                    // this is done easily through clear
                    // clear is the same as position = 0; limit = capacity; mark = -1; // mark is not interesting here.
                    attachment.outputBuffer.clear();
                    attachment.isSendingMessage = false;
                    onMaySendMessage(attachment);
                }
            }
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
        }
    }

    static class AttachmentSocket {
        final Queue<String> messages;
        public boolean isSendingMessage;
        ByteBuffer inputBuffer;
        ByteBuffer outputBuffer;
        AsynchronousSocketChannel socketChannel;
        Thread listeningThread;
        ReadHandler readHandler;
        WriteHandler writeHandler;
        private AsynchronousServerSocketChannel server;

        public AttachmentSocket(AsynchronousServerSocketChannel server,
                                ReadHandler readHandler, WriteHandler writeHandler,
                                ByteBuffer inputBuffer,
                                ByteBuffer outputBuffer, Thread thread) {
            this.isSendingMessage = false;
            this.server = server;
            this.readHandler = readHandler;
            this.writeHandler = writeHandler;
            this.inputBuffer = inputBuffer;
            this.outputBuffer = outputBuffer;
            this.listeningThread = thread;
            this.messages = new ArrayDeque<>();
        }
    }
}

