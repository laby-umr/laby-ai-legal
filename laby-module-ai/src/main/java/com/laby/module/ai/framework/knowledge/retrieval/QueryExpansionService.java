package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import com.laby.module.ai.framework.knowledge.retrieval.bo.QueryExpansionDictPair;
import com.laby.module.ai.framework.knowledge.retrieval.bo.QueryExpansionDictYaml;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi-Query 规则扩展（中英互译 + 表格后缀）
 */
@Slf4j
public class QueryExpansionService {

    private final KnowledgeRetrievalProperties properties;

    private List<QueryExpansionDictPair> dictPairs = List.of();

    public QueryExpansionService(KnowledgeRetrievalProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadDictionary() {
        try (InputStream in = new ClassPathResource("knowledge/query-expansion-dict.yml").getInputStream()) {
            QueryExpansionDictYaml yaml = new Yaml().loadAs(in, QueryExpansionDictYaml.class);
            if (yaml != null && CollUtil.isNotEmpty(yaml.getPairs())) {
                dictPairs = yaml.getPairs();
            }
        } catch (Exception ex) {
            log.warn("[QueryExpansionService][loadDictionary 加载 query-expansion-dict.yml 失败]", ex);
            dictPairs = List.of();
        }
    }

    public List<String> expand(String query, AiQueryIntentEnum intent) {
        if (StrUtil.isBlank(query)) {
            return List.of();
        }
        KnowledgeRetrievalProperties.MultiQueryConfig config = properties.getMultiQuery();
        if (!config.isEnabled()) {
            return List.of(StrUtil.trim(query));
        }
        Set<String> variants = new LinkedHashSet<>();
        String original = StrUtil.trim(query);
        variants.add(original);

        if (intent == AiQueryIntentEnum.TABLE_CELL) {
            variants.add(original + " 表格");
            variants.add(original + " table");
        }

        for (QueryExpansionDictPair pair : dictPairs) {
            if (pair == null) {
                continue;
            }
            if (StrUtil.containsIgnoreCase(original, pair.getZh())) {
                variants.add(replaceIgnoreCase(original, pair.getZh(), pair.getEn()));
                variants.add(original + " " + pair.getEn());
            }
            if (StrUtil.containsIgnoreCase(original, pair.getEn())) {
                variants.add(replaceIgnoreCase(original, pair.getEn(), pair.getZh()));
            }
        }

        if (ReUtil.contains("[A-Za-z]", original) && ReUtil.contains("[\u4e00-\u9fff]", original)) {
            variants.add(original.replaceAll("\\s+", " "));
        }

        return limitVariants(variants, config.getMaxVariants());
    }

    private static List<String> limitVariants(Set<String> variants, int maxVariants) {
        List<String> result = new ArrayList<>();
        for (String variant : variants) {
            if (StrUtil.isBlank(variant)) {
                continue;
            }
            result.add(StrUtil.trim(variant));
            if (result.size() >= Math.max(1, maxVariants)) {
                break;
            }
        }
        return result;
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        return ReUtil.replaceAll(text, "(?i)" + ReUtil.escape(target), replacement);
    }

}
