package com.melardev.networking.sockets.udp.sync.basic;

import java.io.IOException;
import java.net.*;

public class EchoServer {
    public static void main(String[] args) {
        try {

            DatagramSocket serverSocket = new DatagramSocket(3002, InetAddress.getLocalHost());
            byte[] buffer = new byte[1024]; // 1KB, use a maximum of 8192 bytes

            DatagramPacket packetToReceive = new DatagramPacket(buffer, buffer.length);
            DatagramPacket packetToSend = new DatagramPacket(buffer, buffer.length);

            while (true) {
                serverSocket.receive(packetToReceive);
                String dataStr = new String(packetToReceive.getData(), 0, packetToReceive.getLength());
                System.out.printf("[Server] Packet received from %s:%d, content: %s\n",
                        packetToReceive.getAddress().getHostAddress(), packetToReceive.getPort(), dataStr);

                packetToSend.setAddress(packetToReceive.getAddress());
                packetToSend.setPort(packetToReceive.getPort());
                packetToSend.setData(packetToReceive.getData(), 0, packetToReceive.getLength());

                serverSocket.send(packetToSend);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
