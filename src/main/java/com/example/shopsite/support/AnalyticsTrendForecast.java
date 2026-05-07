package com.example.shopsite.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于时间序列的简单预测与文案评估（管理分析展示用，非严格预测模型）。
 */
public final class AnalyticsTrendForecast {

    private AnalyticsTrendForecast() {
    }

    public static Map<String, Object> buildForecast(List<Double> seriesValues, String metricLabel) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (seriesValues == null || seriesValues.size() < 3) {
            out.put("usable", false);
            out.put("summary", "数据点不足，暂不输出趋势预测（建议至少选择近 7 天按日粒度）。");
            return out;
        }
        List<Double> vals = new ArrayList<>(seriesValues);
        int n = vals.size();
        double last = vals.get(n - 1);
        double prev = vals.get(n - 2);

        // 简单线性回归斜率（x=0..n-1）
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = vals.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denom = n * sumX2 - sumX * sumX;
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        double next = slope * n + intercept;

        out.put("usable", true);
        out.put("metricLabel", metricLabel);
        out.put("lastValue", last);
        out.put("slopePerStep", slope);
        out.put("nextStepEstimate", next);
        out.put("dayOverDayDelta", last - prev);

        String trendWord;
        if (Math.abs(slope) < 1e-9) {
            trendWord = "基本持平";
        } else if (slope > 0) {
            trendWord = "整体上行";
        } else {
            trendWord = "整体下行";
        }
        String assessment;
        if ("转化率（%）".equals(metricLabel)) {
            assessment = trendWord + "；转化率受浏览/购买双因素影响，建议结合商品详情与活动排查。";
        } else if (last > prev * 1.15) {
            assessment = "近期走强，可评估库存与履约能力是否匹配。";
        } else if (last < prev * 0.85 && prev > 1e-6) {
            assessment = "近期走弱，可关注促销、流量渠道与竞品动态。";
        } else {
            assessment = "波动温和，建议持续观察 " + trendWord + " 是否延续。";
        }
        out.put("assessment", assessment);
        out.put("summary", String.format(
                "基于所选区间：%s 最新值约为 %.4f，线性外推下一期约 %.4f（仅供参考）。%s",
                metricLabel, last, next, assessment));
        return out;
    }
}
