package com.example.shopsite.support;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;

/**
 * 根据登录 IP 推断用户画像「地域标签」：优先内网/本机分类；若存在 {@code classpath:geo/ip2region_v4.xdb} 则解析公网 IPv4 省市。
 */
@Service
public class RegionInferenceService {

    private volatile Ip2Region ip2Region;

    @PostConstruct
    public void init() {
        ClassPathResource res = new ClassPathResource("geo/ip2region_v4.xdb");
        if (!res.exists()) {
            return;
        }
        try {
            byte[] buf = res.getContentAsByteArray();
            Config v4 = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(new ByteArrayInputStream(buf))
                    .asV4();
            this.ip2Region = Ip2Region.create(v4, null);
        } catch (Exception ignored) {
            this.ip2Region = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (ip2Region != null) {
            try {
                ip2Region.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * @param ip 最近一次成功登录等场景下的 IP，可为 null
     */
    public String inferRegionTag(String ip) {
        if (ip == null || ip.isBlank()) {
            return "无登录IP记录";
        }
        String raw = ip.trim();
        String zoneStripped = stripIpv6Zone(raw);
        try {
            InetAddress addr = InetAddress.getByName(zoneStripped);
            if (addr.isLoopbackAddress()) {
                return "本机回环";
            }
            if (addr.isLinkLocalAddress()) {
                return "链路本地";
            }
            if (addr.isAnyLocalAddress()) {
                return "任意地址";
            }
            if (addr.isMulticastAddress()) {
                return "组播地址";
            }
            byte[] ab = addr.getAddress();
            if (ab != null && ab.length == 4 && (isPrivateIpv4(addr) || isCgnatIpv4(addr))) {
                return "局域网/私网 IPv4";
            }
            if (isUniqueLocalIpv6(addr)) {
                return "IPv6 ULA(内网)";
            }
        } catch (Exception ignored) {
            return "IP 格式异常";
        }

        if (ip2Region != null && isProbablyPublicIpv4(zoneStripped)) {
            try {
                String region = ip2Region.search(zoneStripped);
                if (region != null && !region.isBlank()) {
                    return shortenIp2Region(region);
                }
            } catch (Exception ignored) {
            }
            return "公网 IPv4(地域库未命中)";
        }
        if (zoneStripped.contains(":")) {
            return "公网 IPv6(未配置 v6 地域库)";
        }
        return "公网 IPv4(未配置地域库)";
    }

    private static String stripIpv6Zone(String ip) {
        int p = ip.indexOf('%');
        return p > 0 ? ip.substring(0, p) : ip;
    }

    private static boolean isPrivateIpv4(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b == null || b.length != 4) {
            return false;
        }
        int a0 = b[0] & 0xff;
        int a1 = b[1] & 0xff;
        if (a0 == 10) {
            return true;
        }
        if (a0 == 172 && a1 >= 16 && a1 <= 31) {
            return true;
        }
        if (a0 == 192 && a1 == 168) {
            return true;
        }
        return false;
    }

    private static boolean isCgnatIpv4(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b == null || b.length != 4) {
            return false;
        }
        int a0 = b[0] & 0xff;
        int a1 = b[1] & 0xff;
        return a0 == 100 && a1 >= 64 && a1 <= 127;
    }

    private static boolean isUniqueLocalIpv6(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b == null || b.length != 16) {
            return false;
        }
        int high = b[0] & 0xff;
        return (high & 0xfe) == 0xfc;
    }

    private static boolean isProbablyPublicIpv4(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            return a.getAddress().length == 4 && !a.isLoopbackAddress() && !a.isAnyLocalAddress()
                    && !isPrivateIpv4(a) && !isCgnatIpv4(a);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 典型格式：国家|区域|省份|城市|ISP，将 0 段视为缺省。
     */
    private static String shortenIp2Region(String region) {
        String[] p = region.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String s : p) {
            if (s == null || s.isBlank() || "0".equals(s)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('·');
            }
            sb.append(s);
            if (sb.length() >= 24) {
                break;
            }
        }
        String out = sb.toString();
        return out.isEmpty() ? region : out;
    }
}
