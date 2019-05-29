package com.melardev.networking.sockets.udp.sync.basic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class EchoClient {
    public static void main(String[] args) {

        DatagramSocket clientSocket = null;
        try {

            Scanner scanner = new Scanner(System.in);
            clientSocket = new DatagramSocket();

            byte[] buffer = new byte[1024];
            while (true) {
                System.out.print("> ");

                String data = scanner.nextLine();
                if ("exit".equalsIgnoreCase(data) || "quit".equalsIgnoreCase(data))
                    break;

                DatagramPacket packet = new DatagramPacket(data.getBytes(),
                        data.getBytes().length,
                        InetAddress.getLocalHost(),
                        3002);

                clientSocket.send(packet);
                DatagramPacket receivePacket = new DatagramPacket(buffer, 0, buffer.length);

                clientSocket.receive(receivePacket);
                System.out.println("[Client] Received from server Echo back: "
                        + new String(receivePacket.getData(), 0, receivePacket.getLength()));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Notice this does not throw an exception unlike Tcp Sockets
            // Udp are connectionless, any close() ing is really related to
            // our side and not really closing a connecting, because there is no such connection

            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }
}
