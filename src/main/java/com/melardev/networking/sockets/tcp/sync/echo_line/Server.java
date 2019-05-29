package com.melardev.networking.sockets.tcp.sync.echo_line;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        ServerSocket server = null;
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            server = new ServerSocket(3002);
            server.setReuseAddress(true);
            Socket client = server.accept();

            System.out.println("[Server] New client connected " + client.getInetAddress().getHostAddress());
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                System.out.printf("[Server] Received from the client: %s\n", line);
                out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (server != null)
                    server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
