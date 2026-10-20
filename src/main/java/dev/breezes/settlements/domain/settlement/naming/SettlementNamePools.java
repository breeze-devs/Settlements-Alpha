package dev.breezes.settlements.domain.settlement.naming;

import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The prefix and suffix words settlement names are made from.
 * <p>
 * Each pool holds its words trimmed, none blank, and distinct ignoring case. Where words repeat, the first spelling is
 * kept at its first position.
 *
 * @param prefixes the words a name starts with
 * @param suffixes the words a name ends with
 */
public record SettlementNamePools(List<String> prefixes, List<String> suffixes) {

    public static final SettlementNamePools EMPTY = new SettlementNamePools(List.of(), List.of());

    public SettlementNamePools {
        Validate.notNull(prefixes, "Settlement name prefixes must not be null");
        Validate.notNull(suffixes, "Settlement name suffixes must not be null");
        prefixes = distinctWords(prefixes);
        suffixes = distinctWords(suffixes);
    }

    private static List<String> distinctWords(List<String> words) {
        Map<String, String> byLowercase = new LinkedHashMap<>();
        for (String word : words) {
            String trimmed = word.strip();
            if (!trimmed.isEmpty()) {
                byLowercase.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        return List.copyOf(byLowercase.values());
    }

}
