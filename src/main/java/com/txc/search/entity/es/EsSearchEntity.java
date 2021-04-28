package com.txc.search.entity.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * @author tianxiaochen
 */

/**
 * useServerConfiguration = true 使用线上的配置,createIndex 在项目启动的时候不要创建索引，通常在 kibana 中已经配置过了
 */

@Data
@Document(indexName = "blog", type = "doc", useServerConfiguration = true, createIndex = false)
public class EsSearchEntity {

    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String author;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
    private Date updateTime;

    public EsSearchEntity(String title, String author, String content) {
        this.title = title;
        this.author = author;
        this.content = content;
        Date date = new Date();
        this.createTime = date;
        this.updateTime = date;
    }

    public EsSearchEntity() {
    }
}
