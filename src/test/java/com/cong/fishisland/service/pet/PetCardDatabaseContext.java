package com.cong.fishisland.service.pet;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.cong.fishisland.mapper.PetCardCatalogMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackageClasses = PetCardCatalogMapper.class)
@ImportAutoConfiguration({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
class PetCardDatabaseContext {
    static AnnotationConfigApplicationContext open(String url, String... scripts) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", "root");
        properties.put("spring.datasource.password", "");
        properties.put("spring.sql.init.mode", "always");
        properties.put("spring.sql.init.encoding", "UTF-8");
        properties.put("spring.sql.init.schema-locations", String.join(",", scripts));
        properties.put("mybatis-plus.configuration.map-underscore-to-camel-case", false);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("petCardDatabaseTest", properties));
        context.register(PetCardDatabaseContext.class);
        context.refresh();
        return context;
    }
}
