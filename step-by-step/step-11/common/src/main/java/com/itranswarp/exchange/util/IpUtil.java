package com.itranswarp.exchange.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IpUtil to get IP address.
 */
public class IpUtil {

    static final Logger logger = LoggerFactory.getLogger(IpUtil.class);

    static final String IP;

    static final String HOST_ID;

    static {
        IP = doGetIpAddress();
        HOST_ID = doGetHostId();
        logger.info("get ip address: {}, host id: {}", IP, HOST_ID);
    }

    /**
     * Get localhost's IP address as string.
     *
     * @return IP address like "10.0.1.127"
     */
    public static String getIpAddress() {
        return IP;
    }

    public static String getHostId() {
        return HOST_ID;
    }

    private static String doGetHostId() {
        String id = getIpAddress();
        return id.replace(".", "_").replace("-", "_");
    }

    private static String doGetIpAddress() {
        List<InetAddress> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> iterInterface = NetworkInterface.getNetworkInterfaces();
            while (iterInterface.hasMoreElements()) {
                NetworkInterface network = iterInterface.nextElement();
                if (network.isLoopback() || !network.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> itAddr = network.getInetAddresses();
                while (itAddr.hasMoreElements()) {
                    InetAddress addr = itAddr.nextElement();
                    if (!addr.isLoopbackAddress() && (addr instanceof Inet4Address)) {
                        ipList.add(addr);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Get IP address by NetworkInterface.getNetworkInterfaces() failed: {}", e.getMessage());
        }
        ipList.forEach((ip) -> {
            logger.debug("Found ip address: {}", ip.getHostAddress());
        });
        if (ipList.isEmpty()) {
            return "127.0.0.1";
        }
        return ipList.get(0).getHostAddress();
    }
}
