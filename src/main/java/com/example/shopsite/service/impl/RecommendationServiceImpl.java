package com.example.shopsite.service.impl;

import com.example.shopsite.model.*;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.RecommendationResultRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationResultRepository recommendationResultRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public RecommendationServiceImpl(RecommendationResultRepository recommendationResultRepository,
                                     UserRepository userRepository,
                                     OrderRepository orderRepository) {
        this.recommendationResultRepository = recommendationResultRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void recomputeAll(int topN) {
        int n = clampTopN(topN);

        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .collect(Collectors.toList());
        List<Order> orders = orderRepository.findAll().stream()
                .filter(this::isPaidPlus)
                .collect(Collectors.toList());

        List<Long> popular = computePopularProductIds(orders, n);

        // item->(otherItem->count) 同购共现
        Map<Long, Map<Long, Long>> coMatrix = computeCoPurchaseMatrix(orders);

        // ItemCF：这里用共现count做简单相似度（可扩展为余弦），先保证可用与稳定
        Map<Long, List<Long>> itemCfTop = buildTopNeighbors(coMatrix, n);

        // 为每个用户生成推荐
        for (User u : users) {
            Set<Long> history = userPurchaseHistory(u, orders);

            List<Long> coRec = recommendFromMatrix(history, coMatrix, n);
            List<Long> cfRec = recommendFromNeighbors(history, itemCfTop, n);

            List<Long> popRec = popular.stream()
                    .filter(pid -> !history.contains(pid))
                    .limit(n)
                    .collect(Collectors.toList());

            saveResult(u, RecommendationAlgorithm.CO_PURCHASE, coRec);
            saveResult(u, RecommendationAlgorithm.ITEM_CF, cfRec);
            saveResult(u, RecommendationAlgorithm.POPULAR, popRec);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getRecommendations(User user, RecommendationAlgorithm algorithm, int topN) {
        int n = clampTopN(topN);
        if (user == null) {
            return List.of();
        }
        return recommendationResultRepository
                .findFirstByUserAndAlgorithmOrderByComputedAtDesc(user, algorithm)
                .map(r -> parseIds(r.getProductIds()).stream().limit(n).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics(int topN) {
        int n = clampTopN(topN);
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .collect(Collectors.toList());

        long totalUsers = users.size();
        long coveredUsers = users.stream().filter(u ->
                recommendationResultRepository.findFirstByUserAndAlgorithmOrderByComputedAtDesc(u, RecommendationAlgorithm.ITEM_CF)
                        .map(r -> !parseIds(r.getProductIds()).isEmpty())
                        .orElse(false)).count();

        double coverage = totalUsers == 0 ? 0D : (coveredUsers * 100.0 / totalUsers);

        // 命中率（简化离线评估）：对每个用户取最近一单中的任一商品作为 target，
        // 看是否出现在 ITEM_CF 的 TopN 中（没有历史或没有最近单则跳过）
        long evalUsers = 0;
        long hitUsers = 0;
        List<Order> paidOrders = orderRepository.findAll().stream().filter(this::isPaidPlus).collect(Collectors.toList());
        Map<Long, List<Order>> byUser = paidOrders.stream()
                .filter(o -> o.getUser() != null && o.getUser().getId() != null)
                .collect(Collectors.groupingBy(o -> o.getUser().getId()));

        for (User u : users) {
            List<Order> uOrders = byUser.getOrDefault(u.getId(), List.of());
            if (uOrders.isEmpty()) continue;
            uOrders = uOrders.stream()
                    .filter(o -> o.getOrderDate() != null)
                    .sorted(Comparator.comparing(Order::getOrderDate))
                    .collect(Collectors.toList());
            Order last = uOrders.get(uOrders.size() - 1);
            Set<Long> targets = last.getItems() == null ? Set.of() : last.getItems().stream()
                    .map(oi -> oi.getProduct() == null ? null : oi.getProduct().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (targets.isEmpty()) continue;

            List<Long> rec = getRecommendations(u, RecommendationAlgorithm.ITEM_CF, n);
            evalUsers++;
            if (rec.stream().anyMatch(targets::contains)) {
                hitUsers++;
            }
        }

        double hitRate = evalUsers == 0 ? 0D : (hitUsers * 100.0 / evalUsers);

        // 召回稳定性（简化）：同一算法两次计算结果的平均 overlap@N（若不足两次则为 100）
        double stability = computeStability(users, RecommendationAlgorithm.ITEM_CF, n);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("topN", n);
        m.put("coverageRate", String.format("%.2f", coverage));
        m.put("hitRate", String.format("%.2f", hitRate));
        m.put("stability", String.format("%.2f", stability));
        m.put("totalUsers", totalUsers);
        m.put("evalUsers", evalUsers);
        return m;
    }

    private double computeStability(List<User> users, RecommendationAlgorithm algorithm, int n) {
        double sum = 0D;
        long cnt = 0;
        for (User u : users) {
            List<RecommendationResult> all = recommendationResultRepository.findByUser(u).stream()
                    .filter(r -> r.getAlgorithm() == algorithm)
                    .sorted(Comparator.comparing(RecommendationResult::getComputedAt).reversed())
                    .limit(2)
                    .collect(Collectors.toList());
            if (all.size() < 2) continue;
            List<Long> a = parseIds(all.get(0).getProductIds()).stream().limit(n).collect(Collectors.toList());
            List<Long> b = parseIds(all.get(1).getProductIds()).stream().limit(n).collect(Collectors.toList());
            if (a.isEmpty() || b.isEmpty()) continue;
            Set<Long> sa = new HashSet<>(a);
            long inter = b.stream().filter(sa::contains).count();
            sum += (inter * 100.0 / n);
            cnt++;
        }
        return cnt == 0 ? 100D : (sum / cnt);
    }

    private void saveResult(User user, RecommendationAlgorithm algorithm, List<Long> ids) {
        String s = ids == null ? "" : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        RecommendationResult r = RecommendationResult.builder()
                .user(user)
                .algorithm(algorithm)
                .productIds(s)
                .computedAt(LocalDateTime.now())
                .build();
        recommendationResultRepository.save(r);
    }

    private static List<Long> parseIds(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] parts = s.split(",");
        List<Long> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try { out.add(Long.valueOf(t)); } catch (Exception ignored) {}
        }
        return out;
    }

    private boolean isPaidPlus(Order o) {
        if (o == null || o.getStatus() == null) return false;
        return o.getStatus() == OrderStatus.PROCESSING
                || o.getStatus() == OrderStatus.SHIPPED
                || o.getStatus() == OrderStatus.DELIVERED
                || o.getStatus() == OrderStatus.COMPLETED;
    }

    private static int clampTopN(int topN) {
        if (topN <= 0) return 10;
        return Math.min(topN, 30);
    }

    private static List<Long> computePopularProductIds(List<Order> orders, int n) {
        Map<Long, Long> cnt = new HashMap<>();
        for (Order o : orders) {
            if (o.getItems() == null) continue;
            for (OrderItem oi : o.getItems()) {
                if (oi == null || oi.getProduct() == null || oi.getProduct().getId() == null) continue;
                long q = oi.getQuantity() == null ? 0 : oi.getQuantity();
                cnt.put(oi.getProduct().getId(), cnt.getOrDefault(oi.getProduct().getId(), 0L) + q);
            }
        }
        return cnt.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(n)
                .collect(Collectors.toList());
    }

    private static Map<Long, Map<Long, Long>> computeCoPurchaseMatrix(List<Order> orders) {
        Map<Long, Map<Long, Long>> m = new HashMap<>();
        for (Order o : orders) {
            if (o.getItems() == null || o.getItems().isEmpty()) continue;
            List<Long> ids = o.getItems().stream()
                    .map(oi -> oi.getProduct() == null ? null : oi.getProduct().getId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            for (int i = 0; i < ids.size(); i++) {
                for (int j = 0; j < ids.size(); j++) {
                    if (i == j) continue;
                    Long a = ids.get(i);
                    Long b = ids.get(j);
                    m.computeIfAbsent(a, k -> new HashMap<>())
                            .put(b, m.getOrDefault(a, Map.of()).getOrDefault(b, 0L) + 1);
                }
            }
        }
        return m;
    }

    private static Map<Long, List<Long>> buildTopNeighbors(Map<Long, Map<Long, Long>> matrix, int n) {
        Map<Long, List<Long>> out = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Long>> e : matrix.entrySet()) {
            List<Long> top = e.getValue().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .limit(n)
                    .collect(Collectors.toList());
            out.put(e.getKey(), top);
        }
        return out;
    }

    private static Set<Long> userPurchaseHistory(User u, List<Order> orders) {
        if (u == null || u.getId() == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        for (Order o : orders) {
            if (o.getUser() == null || o.getUser().getId() == null) continue;
            if (!o.getUser().getId().equals(u.getId())) continue;
            if (o.getItems() == null) continue;
            for (OrderItem oi : o.getItems()) {
                if (oi.getProduct() != null && oi.getProduct().getId() != null) {
                    ids.add(oi.getProduct().getId());
                }
            }
        }
        return ids;
    }

    private static List<Long> recommendFromMatrix(Set<Long> history, Map<Long, Map<Long, Long>> matrix, int n) {
        if (history == null || history.isEmpty()) return List.of();
        Map<Long, Long> score = new HashMap<>();
        for (Long h : history) {
            Map<Long, Long> neigh = matrix.getOrDefault(h, Map.of());
            for (Map.Entry<Long, Long> e : neigh.entrySet()) {
                Long pid = e.getKey();
                if (history.contains(pid)) continue;
                score.put(pid, score.getOrDefault(pid, 0L) + e.getValue());
            }
        }
        return score.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(n)
                .collect(Collectors.toList());
    }

    private static List<Long> recommendFromNeighbors(Set<Long> history, Map<Long, List<Long>> neighbors, int n) {
        if (history == null || history.isEmpty()) return List.of();
        Map<Long, Long> score = new HashMap<>();
        for (Long h : history) {
            for (Long pid : neighbors.getOrDefault(h, List.of())) {
                if (history.contains(pid)) continue;
                score.put(pid, score.getOrDefault(pid, 0L) + 1);
            }
        }
        return score.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(n)
                .collect(Collectors.toList());
    }
}

