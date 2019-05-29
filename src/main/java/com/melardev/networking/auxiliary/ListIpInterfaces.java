package com.melardev.networking.auxiliary;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ListIpInterfaces {

    public static void main(String[] args) {
        System.out.println("Network Interface");
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                System.out.println(iface.getDisplayName());
                //iface.getInterfaceAddresses().forEach(n -> System.out.println("\t" + n.getAddress().getHostAddress()));
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements())
                    System.out.println("\t" + addresses.nextElement().getHostAddress());
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
