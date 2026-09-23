package com.chatlocal.backend.service;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utilidades de red para resolver direcciones IP y validar puertos.
 */
public final class NetworkUtils {

    private NetworkUtils() {}

    /**
     * Obtiene una lista con todas las IPs IPv4 locales no loopback de las interfaces activas.
     */
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

    /**
     * Retorna la IP local principal recomendada.
     */
    public static String getPrimaryLocalIp() {
        List<String> ips = getAvailableLocalIps();
        return ips.isEmpty() ? "127.0.0.1" : ips.get(0);
    }

    /**
     * Valida si un puerto está dentro del rango válido de red TCP (1 - 65535).
     */
    public static boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }

    /**
     * Valida sintácticamente una dirección IP o nombre de host.
     */
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
