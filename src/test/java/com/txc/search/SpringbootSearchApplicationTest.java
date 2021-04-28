package com.txc.search;

import com.txc.search.entity.es.EsSearchEntity;
import com.txc.search.repository.es.EsSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Iterator;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class SpringbootSearchApplicationTest {
    @Autowired
    EsSearchRepository esSearchRepository;

    @Test
    public void testEs() {
        Iterable<EsSearchEntity> all = esSearchRepository.findAll();
        Iterator<EsSearchEntity> esSearchIterator = all.iterator();
        for (EsSearchEntity esSearchEntity : all) {
            System.out.println(esSearchEntity.toString());
        }
        EsSearchEntity esSearchEntity = esSearchIterator.next();

        log.info("【es集成springboot】esSearchEntity={}", esSearchEntity);
    }

}
