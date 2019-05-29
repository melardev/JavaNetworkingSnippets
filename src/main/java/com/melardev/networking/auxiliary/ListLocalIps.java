package com.melardev.networking.auxiliary;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ListLocalIps {
    public static void main(String[] args) {
        try {
            InetAddress localhost = InetAddress.getLocalHost();

            System.out.println(localhost.getHostName());
            InetAddress[] ifaces = InetAddress.getAllByName(localhost.getHostName());
            for (InetAddress iface : ifaces) {
                System.out.println(iface.getHostName() + " - " + iface.getHostAddress());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
