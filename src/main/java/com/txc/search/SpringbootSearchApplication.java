package com.txc.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author tianxiaochen
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Slf4j
public class SpringbootSearchApplication {

    @Value("${server.port}")
    private Long port;


    public static void main(String[] args) {
        SpringApplication.run(SpringbootSearchApplication.class, args);
        String port = null;
        try {
            port = getParam("server.port");
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("项目启动成功，访问端口:" + port);
    }

    public static String getParam(String key) throws IOException {
        Properties properties = new Properties();
        // 使用ClassLoader加载properties配置文件生成对应的输入流
        InputStream in = SpringbootSearchApplication.class.getClassLoader().getResourceAsStream("application.properties");
        // 使用properties对象加载输入流
        properties.load(in);
        return properties.getProperty(key);
    }

}
