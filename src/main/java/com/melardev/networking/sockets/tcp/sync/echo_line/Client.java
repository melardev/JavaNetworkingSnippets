package com.melardev.networking.sockets.tcp.sync.echo_line;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        String host = "127.0.0.1";
        int port = 3002;
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);
            String line = null;

            while (!"exit".equalsIgnoreCase(line)) {
                System.out.print("Enter your Message> ");
                line = scanner.nextLine();
                out.println(line);
                out.flush();
                System.out.println("[Client] Server replied: " + in.readLine());
                System.out.flush();
            }

            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
