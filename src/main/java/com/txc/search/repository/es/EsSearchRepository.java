package com.txc.search.repository.es;

import com.txc.search.entity.es.EsSearchEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author tianxiaochen
 */
public interface EsSearchRepository extends ElasticsearchRepository<EsSearchEntity, Integer> {
}
