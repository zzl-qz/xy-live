package org.xy.live.framework.datasource.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;


/**
 * 用来手动初始化Hikari数据源（因为这个数据源有bug，不能通过配置文件进行初始化）
 */
@Configuration
public class ShardingJdbcDatasourceAutoInitConnectionConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardingJdbcDatasourceAutoInitConnectionConfig.class);

    @Bean
    public ApplicationRunner runner(DataSource dataSource) {
        return args -> {
            LOGGER.info("dataSource: {}", dataSource);
            Connection connection = dataSource.getConnection();
        };
    }
}