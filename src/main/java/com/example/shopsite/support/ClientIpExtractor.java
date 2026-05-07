package com.example.shopsite.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从请求中解析真实客户端 IP（配合反向代理、CDN 的转发头；并规范 IPv6/端口形式）。
 */
public final class ClientIpExtractor {

    private static final int MAX_LEN = 64;

    private ClientIpExtractor() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = parseForwarded(request.getHeader("Forwarded"));
        if (present(ip)) {
            return truncate(normalize(ip));
        }

        ip = parseXForwardedFor(request.getHeader("X-Forwarded-For"));
        if (present(ip)) {
            return truncate(normalize(ip));
        }

        String[] headers = {"CF-Connecting-IP", "True-Client-IP", "X-Real-IP", "X-Client-IP"};
        for (String h : headers) {
            String raw = request.getHeader(h);
            if (!present(raw)) {
                continue;
            }
            ip = stripPort(unwrapBracketIpv6(raw.trim()));
            if (present(ip)) {
                return truncate(normalize(ip));
            }
        }

        return truncate(normalize(request.getRemoteAddr()));
    }

    private static String parseForwarded(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        for (String element : forwarded.split(",")) {
            for (String token : element.split(";")) {
                String t = token.trim();
                if (t.length() >= 4 && t.regionMatches(true, 0, "for=", 0, 4)) {
                    String v = t.substring(4).trim();
                    v = normalizeQuoted(v);
                    v = unwrapBracketIpv6(v);
                    return stripPort(v);
                }
            }
        }
        return null;
    }

    private static String parseXForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) {
            return null;
        }
        for (String part : xff.split(",")) {
            String p = part.trim();
            if (!present(p) || "unknown".equalsIgnoreCase(p)) {
                continue;
            }
            p = normalizeQuoted(p);
            p = unwrapBracketIpv6(p);
            return normalize(stripPort(p));
        }
        return null;
    }

    private static String normalizeQuoted(String v) {
        if (v == null || v.length() < 2) {
            return v;
        }
        if (v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    /**
     * RFC 7239 / 常见写法：{@code [2001:db8::1]} 或 {@code [2001:db8::1]:443}
     */
    private static String unwrapBracketIpv6(String hostPort) {
        String s = hostPort.trim();
        if (s.startsWith("[") && s.contains("]")) {
            int end = s.indexOf(']');
            return s.substring(1, end);
        }
        return s;
    }

    /**
     * {@code host:port} 仅对 IPv4 或已解括号形式尝试去端口（IPv6 含多冒号，依赖方括号区分）。
     */
    private static String stripPort(String hostPort) {
        if (hostPort == null || hostPort.isEmpty()) {
            return hostPort;
        }
        String s = hostPort.trim();
        if (s.startsWith("[")) {
            return s;
        }
        long colons = s.chars().filter(c -> c == ':').count();
        if (colons == 1) {
            int i = s.lastIndexOf(':');
            String tail = s.substring(i + 1);
            if (tail.chars().allMatch(Character::isDigit)) {
                return s.substring(0, i);
            }
        }
        return s;
    }

    private static String normalize(String ip) {
        if (ip == null) {
            return null;
        }
        String s = ip.trim();
        int zone = s.indexOf('%');
        if (zone > 0) {
            s = s.substring(0, zone);
        }
        return s;
    }

    private static boolean present(String s) {
        return s != null && !s.isBlank() && !"unknown".equalsIgnoreCase(s.trim());
    }

    private static String truncate(String v) {
        if (v == null) {
            return null;
        }
        return v.length() <= MAX_LEN ? v : v.substring(0, MAX_LEN);
    }
}
