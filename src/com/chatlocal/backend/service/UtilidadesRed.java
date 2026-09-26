package com.chatlocal.backend.service;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// utilidades para sacar las ips de la maquina y validar puertos
public final class UtilidadesRed {

    private UtilidadesRed() {}

    // saca las ips de la pc para mostrarlas en la pantalla de conectar
    public static List<String> getAvailableLocalIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {}

        if (ips.isEmpty()) {
            ips.add("127.0.0.1");
        }
        return ips;
    }

    // regresa la primera ip encontrada
    public static String getPrimaryLocalIp() {
        List<String> ips = getAvailableLocalIps();
        return ips.isEmpty() ? "127.0.0.1" : ips.get(0);
    }

    // revisa que el puerto este entre 1024 y 65535
    public static boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }

    // revisa si la ip es valida
    public static boolean isValidHost(String host) {
        if (host == null || host.trim().isEmpty()) return false;
        try {
            InetAddress.getByName(host.trim());
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
