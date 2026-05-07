package com.example.shopsite.support;

import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CategoryLabelService {

    private final CategoryRepository categoryRepository;

    public CategoryLabelService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** 根据行为日志中的 categoryId 批量解析分类名称（用于表格展示）。 */
    public Map<Long, String> labelsForBehaviorLogs(List<UserBehaviorLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = logs.stream()
                .map(UserBehaviorLog::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> out = new HashMap<>();
        for (Long id : ids) {
            categoryRepository.findById(id).ifPresent(c -> out.put(id, c.getName()));
        }
        return out;
    }
}
