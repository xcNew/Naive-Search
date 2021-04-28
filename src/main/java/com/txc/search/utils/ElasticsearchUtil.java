package com.txc.search.utils;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;

/**
 * 创 建 人 ： wangsheng 创建日期：2020年11月
 */
@Component
public class ElasticsearchUtil {
    @Autowired
    private ElasticsearchTemplate template;

    /**
     * 更新
     *
     * @return
     */
    public <T> Boolean update(String index, String type, T t) throws IOException {
        try {
            Class<? extends Object> tClass = t.getClass();
            //得到所有属性
            Field[] field = tClass.getDeclaredFields();
            //设置ID可以访问私有变量
            field[0].setAccessible(true);
            //整合出 getId() 属性这个方法
            Method m = tClass.getMethod("get" + "Id");
            //调用这个整合出来的get方法，强转成自己需要的类型
            Integer id = (Integer) m.invoke(t);
            XContentBuilder xContentBuilder = XContentFactory.jsonBuilder().startObject();
            for (int i = 0; i < field.length; i++) {
                String updateField1 = field[i].getName();
                //将属性名字的首字母大写
                updateField1 = updateField1.replaceFirst(updateField1.substring(0, 1), updateField1.substring(0, 1).toUpperCase());
                if (!updateField1.equals("Id")) {
                    Object updateValue = getValue(t, field[i]);
                    if (updateValue != null) {
                        xContentBuilder.field(field[i].getName(), updateValue);
                    }
                }
            }
            xContentBuilder.endObject();
            UpdateRequest updateRequest = new UpdateRequest().index(index)
                    .type(type).id(id.toString()).doc(xContentBuilder);
            ;
            UpdateQuery updateQuery = new UpdateQueryBuilder()
                    .withId(id.toString())
                    .withClass(tClass)
                    .withUpdateRequest(updateRequest)
                    .build();
            template.update(updateQuery);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //批量添加
    public <T> boolean addList(List<T> list) {
        try {
            List queries = new ArrayList();
            for (T t : list) {
                IndexQuery indexQuery = new IndexQueryBuilder()
                        .withId(getId(t).toString())
                        .withObject(t)
                        .build();
                queries.add(indexQuery);
            }
            if (queries.size() > 0) {
                template.bulkIndex(queries);

            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 删除
     *
     * @return
     */
    public <T> boolean delete(T t) throws IOException {
        try {
            Class<? extends Object> tClass = t.getClass();
            //得到所有属性
            Field[] field = tClass.getDeclaredFields();
            //设置ID可以访问私有变量
            field[0].setAccessible(true);
            //整合出 getId() 属性这个方法
            Method m = tClass.getMethod("get" + "Id");
            //调用这个整合出来的get方法，强转成自己需要的类型
            Object id = getValue(t, field[0]);
            template.delete(tClass, id.toString());
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }




    //分词匹配查询,比如有两条数据：1、我今天非常高兴 2、他摔倒很高兴输入：今天高兴. 这两条数据都能匹配上
    public <T> List<Class<T>> queryByfenci(Integer startpage, Integer pageSize, Class<T> tClass, String keyword, String value) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery(keyword, value))
                .withPageable(new PageRequest(startpage, pageSize))
                .build();
        List<T> list = template.queryForList(searchQuery, tClass);
        return (List<Class<T>>) list;
    }

    //分页单字符串全文查询
    public <T> List<Class<T>> queryByKeyWord(Integer startpage, Integer pageSize, String value, Class<T> tClass) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.queryStringQuery(value))
                .withPageable(new PageRequest(startpage, pageSize))
                .build();
        List<T> list = template.queryForList(searchQuery, tClass);
        return (List<Class<T>>) list;
    }


    //分页模糊查询,类似mysql中like "%word%"的模糊匹配
    public <T> List<T> queryLike(Integer startpage, Integer pageSize, String keyword, String value, Class<T> tClass) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchPhraseQuery(keyword, value))
                .withPageable(new PageRequest(startpage, pageSize))
                .build();
        List<T> list = template.queryForList(searchQuery, tClass);
        return list;
    }


    //分页多条件查，字段和值必须全等
    public <T> List<T> queryByManyKey(Integer startpage, Integer pageSize, T t) throws Exception {
        BoolQueryBuilder mustQuery = QueryBuilders.boolQuery();
        Pageable pageable = new PageRequest(startpage, pageSize);
        Class<? extends Object> tClass = t.getClass();
        //得到所有属性
        Field[] field = tClass.getDeclaredFields();
        for (int i = 0; i < field.length; i++) {
            String updateField = field[i].getName();
            //将属性名字的首字母大写
            updateField = updateField.replaceFirst(updateField.substring(0, 1), updateField.substring(0, 1).toUpperCase());
            if (!updateField.equals("Id")) {
                Object updateValue = getValue(t, field[i]);
                if (updateValue != null) {
                    mustQuery.must(QueryBuilders.matchPhraseQuery(field[i].getName(), updateValue));
                }
            }
        }
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(mustQuery).withPageable(pageable).build();
        return (List<T>) template.queryForList(searchQuery, tClass);

    }


    /**
     * 分页多个字段匹配某字符串,value是字符串，column是匹配的列，只要任何一个字段包括该字符串即可
     */
    public <T> List<Class<T>> manyFiledMach(Integer startpage, Integer pageSize, Class<T> tClass, String value, String... column) {
        Pageable pageable = new PageRequest(startpage, pageSize);
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(multiMatchQuery(value, column)).withPageable(pageable).build();
        List<T> list = template.queryForList(searchQuery, tClass);
        return (List<Class<T>>) list;
    }


    public <T> Object getValue(T t, Field field) {
        try {
            Class<? extends Object> tClass = t.getClass();
            field.setAccessible(true);
            String updateField1 = field.getName();
            //将属性名字的首字母大写
            updateField1 = updateField1.replaceFirst(updateField1.substring(0, 1), updateField1.substring(0, 1).toUpperCase());
            Method m = tClass.getMethod("get" + updateField1);
            // 获取属性类型
            String type = field.getGenericType().toString();
            if (type.equals("class java.lang.String")) {
                String value = (String) m.invoke(t);
                return value;
            }

            if (type.equals("class java.lang.Integer")) {
                Integer value = (Integer) m.invoke(t);
                return value;
            }

            if (type.equals("class java.util.Date")) {
                Date value = (Date) m.invoke(t);
                return value;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public <T> Object getId(T t) {
        try {
            Class<? extends Object> tClass = t.getClass();
            //得到所有属性
            Field[] field = tClass.getDeclaredFields();
            //设置ID可以访问私有变量
            field[0].setAccessible(true);
            //整合出 getId() 属性这个方法
            Method m = tClass.getMethod("get" + "Id");
            //调用这个整合出来的get方法，强转成自己需要的类型
            return getValue(t, field[0]);
        } catch (Exception e) {
            return null;
        }
    }


}