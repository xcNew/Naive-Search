package com.txc.search.controller;

import cn.hutool.core.collection.CollUtil;
import com.txc.search.entity.es.EsSearchEntity;
import com.txc.search.repository.es.EsSearchRepository;
import com.txc.search.utils.ElasticsearchUtil;
import com.txc.search.utils.UIDUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author tianxiaochen
 */
@Controller
@Slf4j
public class DataController {

    private static Logger logger = LoggerFactory.getLogger(DataController.class);

    private static final String INDEX = "blog";

    public static final String TYPE = "doc";

    public static final Long SCROLL_TIME_OUT = 3000L;

//    @Autowired
//    MysqlSearchRepository mysqlSearchRepository;

    @Autowired
    EsSearchRepository esSearchRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    ElasticsearchUtil elasticsearchUtill;


    @ResponseBody
    @GetMapping("/blogs")
    public Object blogList() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withIndices(INDEX)//索引名
                .withTypes(TYPE)//类型名
                //  .withQuery(QueryBuilders.termQuery("userId", "123"))//查询条件，这里简单使用term查询
                .withPageable(PageRequest.of(0, 10))//从0页开始查，每页10个结果
//                .withFields("userId")//ES里该index内存的文档，可能存了很多我们不关心的字段，所以指定有用的字段
                .build();

        ScrolledPage<EsSearchEntity> scroll = (ScrolledPage<EsSearchEntity>) elasticsearchTemplate
                .startScroll(SCROLL_TIME_OUT, searchQuery, EsSearchEntity.class);
        logger.info("查询总命中数：" + scroll.getTotalElements());
        List<EsSearchEntity> esSearchEntities = new ArrayList<>();
        while (scroll.hasContent()) {
            for (EsSearchEntity esSearchEntity : scroll.getContent()) {
                esSearchEntities.add(esSearchEntity);
            }
            //取下一页，scrollId在es服务器上可能会发生变化，需要用最新的。发起continueScroll请求会重新刷新快照保留时间
            scroll = (ScrolledPage<EsSearchEntity>) elasticsearchTemplate
                    .continueScroll(scroll.getScrollId(), 3000, EsSearchEntity.class);
        }
        //及时释放es服务器资源
        elasticsearchTemplate.clearScroll(scroll.getScrollId());
        return esSearchEntities;

    }

    @ResponseBody
    @PostMapping("/search")
    public Object search(@RequestBody Param param) {
        Map<String, Object> map = new HashMap<>();
        StopWatch watch = new StopWatch();
        watch.start();
        // 空查询返回全量数据
        if (param.getKeyword().trim().equals("")) {
            map.put("list", blogList());
        } else {
            BoolQueryBuilder builder = QueryBuilders.boolQuery();
            builder.should(QueryBuilders.matchQuery("title", param.getKeyword()));
            builder.should(QueryBuilders.matchQuery("content", param.getKeyword()));
            log.info("s={}", builder.toString());
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(builder)
                    .withHighlightFields(
                            new HighlightBuilder.Field("title").preTags("<span style=\"color:red\">").postTags("</span>"),
                            new HighlightBuilder.Field("content").preTags("<span style=\"color:red\">").postTags("</span>")
                    )
                    .build();
            List<EsSearchEntity> content = queryEntities(searchQuery);
            map.put("list", content);
        }
        watch.stop();
        long millis = watch.getTotalTimeMillis();
        map.put("duration", millis);
        return map;
    }

    @ResponseBody
    @GetMapping("/blog/{id}")
    public Object blog(@PathVariable String id) {
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        builder.must(QueryBuilders.matchQuery("id", id));
        log.info("s={}", builder.toString());
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(builder)
                .build();
        List<EsSearchEntity> content = queryEntities(searchQuery);
        if (!CollUtil.isEmpty(content)) {
            return content.get(0);
        }
        return null;
    }

    @Data
    private static class Param {
        private String type;
        private String keyword;
    }

    @PostMapping("/put")
    public String blog(EsSearchEntity esSearchEntity) {
        if (esSearchEntity == null) {
            return "redirect:/index.html";
        }
        try {
            esSearchEntity.setId(UIDUtil.getUUID());
            Date date = new Date();
            esSearchEntity.setUpdateTime(date);
            esSearchEntity.setCreateTime(date);
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(esSearchEntity.getId())
                    .withObject(esSearchEntity)
                    .build();
            String flag = elasticsearchTemplate.index(indexQuery);
            System.out.println(flag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/index.html";
    }

    public List<EsSearchEntity> queryEntities(NativeSearchQuery searchQuery) {
        Page<EsSearchEntity> books = elasticsearchTemplate.queryForPage(searchQuery, EsSearchEntity.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits searchHits = response.getHits();
                SearchHit[] hits = searchHits.getHits();
                ArrayList<EsSearchEntity> esSearchEntities = new ArrayList<EsSearchEntity>();
                for (SearchHit hit : hits) {
                    EsSearchEntity esSearchEntity = new EsSearchEntity();
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    esSearchEntity.setId((sourceAsMap.get("id").toString()));
                    esSearchEntity.setAuthor(sourceAsMap.get("author").toString());
                    esSearchEntity.setContent(sourceAsMap.get("content").toString());
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//注意月份是MM
                    try {
                        esSearchEntity.setCreateTime(simpleDateFormat.parse(sourceAsMap.get("createTime").toString()));
                        esSearchEntity.setUpdateTime(simpleDateFormat.parse(sourceAsMap.get("updateTime").toString()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    esSearchEntity.setTitle(sourceAsMap.get("title").toString());
                    //TODO 搜索关键字高亮，以下代码似乎不生效
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    System.out.println(highlightFields);
                    if (highlightFields.get("title") != null) {
//                            String nameHighlight = highlightFields.get("title").getFragments()[0].toString();
                        esSearchEntity.setTitle(sourceAsMap.get("title").toString());
                    }
                    if (highlightFields.get("content") != null) {
//                            String contentHighlight = highlightFields.get("content").getFragments()[0].toString();
                        esSearchEntity.setContent(sourceAsMap.get("content").toString());
                    }
                    esSearchEntities.add(esSearchEntity);
                }
                return new AggregatedPageImpl<T>((List<T>) esSearchEntities);

            }
        });
        List<EsSearchEntity> esSearchEntities = books.getContent();
        return esSearchEntities;
    }

}
