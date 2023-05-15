package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;

/**
 * @author hanl
 * @date 25/06/2015
 */
public class QueryBuilderUtil {

    public static MetaQueryBuilder defaultMetaBuilder (Integer pageIndex,
            Integer pageInteger, Integer pageLength, String ctx, Boolean cutoff) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex)
                .addEntry("startPage", pageInteger)
                .addEntry("count", pageLength).setSpanContext(ctx)
                .addEntry("cutOff", cutoff);
        return meta;

    }
}
