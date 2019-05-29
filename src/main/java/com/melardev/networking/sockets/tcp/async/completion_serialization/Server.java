package com.melardev.networking.sockets.tcp.async.completion_serialization;


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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class Server {

    public static void main(String[] args) {
        try {
            AsynchronousServerSocketChannel asynchronousServerSocketChannel =
                    AsynchronousServerSocketChannel.open();
            asynchronousServerSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            asynchronousServerSocketChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    3002));

            AttachmentSocket attachment = new AttachmentSocket(asynchronousServerSocketChannel,
                    new ReadHandler(), Thread.currentThread());


            asynchronousServerSocketChannel.accept(attachment,
                    new ConnectionHandler());

            attachment.listeningThread.join();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Demo using Completion Handlers + Attachment + Serialization
     * The server will be receiving packets(un-serializing them)
     * So no CompletionHandler for write(), to keep it simple
     * If you wanna know how to handle write with CompletionHandler well... see the Server code.
     */
    static class AttachmentSocket {
        public int payloadLen;
        ByteBuffer headerBuffer;
        ByteBuffer payloadBuffer;
        AsynchronousSocketChannel socketChannel;
        Thread listeningThread;
        ReadHandler readHandler;
        private AsynchronousServerSocketChannel server;

        public AttachmentSocket(AsynchronousServerSocketChannel server,
                                ReadHandler readHandler, Thread thread) {
            this.server = server;
            this.readHandler = readHandler;
            this.headerBuffer = ByteBuffer.allocate(4);
            this.listeningThread = thread;
        }
    }

    public static class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AttachmentSocket> {

        @Override
        public void completed(AsynchronousSocketChannel socketClient, AttachmentSocket attachment) {
            System.out.println("Got a connection ");

            attachment.socketChannel = socketClient;

            // The previous .accept() is now consumed, if we want to be able to accept another client
            // We must schedule it again, let's make another async accept then
            attachment.server.accept(attachment, this);
            socketClient.read(attachment.headerBuffer, attachment, attachment.readHandler);
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {
            System.out.println("failed");
        }
    }

    static class ReadHandler implements CompletionHandler<Integer, AttachmentSocket> {


        @Override
        public void completed(Integer bytesRead, AttachmentSocket attachment) {

            if (attachment.headerBuffer.position() != attachment.headerBuffer.limit()) {
                // Keep reading, until we have the full header
                attachment.socketChannel.read(attachment.headerBuffer, attachment, this);
                return;
            }

            if (attachment.payloadBuffer == null) {
                // We have read the header but not the the payload, let's begin initializing the payloadBuffer
                attachment.headerBuffer.flip();
                int payloadLength = attachment.headerBuffer.getInt();
                attachment.payloadLen = payloadLength;
                attachment.payloadBuffer = ByteBuffer.allocate(payloadLength);
                attachment.socketChannel.read(attachment.payloadBuffer, attachment, this);
                return;
            }

            if (attachment.payloadBuffer.position() == 0) {
                // This is triggered at least for the second incoming packet, when
                // we have used payloadBuffer to fill one packet, and now it is time to decide if the payloadBuffer
                // we allocated before may be reused for new packet or otherwise we have to create another one.
                // The check is to see if the old payloadBuffer can hold the incoming packet
                attachment.headerBuffer.flip();
                int payloadLength = attachment.headerBuffer.getInt();
                attachment.payloadLen = payloadLength;
                if (attachment.payloadBuffer.capacity() < payloadLength)
                    attachment.payloadBuffer = ByteBuffer.allocate(payloadLength);
            }

            if (attachment.payloadBuffer.position() != attachment.payloadLen) {
                // We haven't read the whole packet yet
                attachment.socketChannel.read(attachment.payloadBuffer, attachment, this);
                return;
            }


            // We have the whole packet into payloadBuffer, let's process it

            byte[] payload = new byte[attachment.payloadLen];

            attachment.payloadBuffer.flip();
            System.out.println("Payload is " + attachment.payloadBuffer.limit() + " bytes long");
            attachment.payloadBuffer.get(payload);

            Packet packet = deserialize(payload);
            BufferedImage bi = new BufferedImage(
                    packet.image.getIconWidth(),
                    packet.image.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = bi.createGraphics();

            packet.image.paintIcon(null, g, 0, 0);
            g.dispose();

            try {
                ImageIO.write(bi, "png", new File("D:/temp/output.png"));
                System.out.println("[Client] Received image, written under D:/temp/output.png");
                System.out.println("[Client] Received: " + packet.message);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // set pos to 0, limit to capacity
            attachment.payloadBuffer.clear();
            attachment.headerBuffer.clear();

            // Read another packet, start by reading the header obviously
            attachment.socketChannel.read(attachment.headerBuffer, attachment, this);
        }

        private Packet deserialize(byte[] payload) {

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
                    if (byteArrayInputStream != null)
                        byteArrayInputStream.close();
                    if (objectInputStream != null)
                        objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public void failed(Throwable exc, AttachmentSocket attachment) {

        }
    }

}
