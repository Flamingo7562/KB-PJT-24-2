package com.gighub.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 외부 DB properties와 영속성 Bean 연결을 검증합니다.
 */
class DatabaseConfigTest {

    private static final String CONFIG_LOCATION_PROPERTY = "gighub.database.config";

    @TempDir
    private Path tempDirectory;

    /**
     * JVM 속성으로 지정한 외부 파일의 값이 HikariCP, MyBatis와 Transaction에 연결되는지 확인합니다.
     *
     * @throws IOException 임시 properties 파일 생성 실패 시
     */
    @Test
    void loadsPersistenceBeansFromExternalProperties() throws IOException {
        Path configFile = tempDirectory.resolve("database-local.properties");
        Files.writeString(
                configFile,
                String.join(
                        System.lineSeparator(),
                        "database.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "database.jdbc-url=jdbc:mysql://localhost:3307/test_schema",
                        "database.username=test_user",
                        "database.password=test_password",
                        "database.pool.maximum-pool-size=7",
                        "database.pool.minimum-idle=1",
                        "database.pool.connection-timeout-ms=25000",
                        "database.pool.validation-timeout-ms=4000"
                ),
                StandardCharsets.UTF_8
        );

        String previousLocation = System.getProperty(CONFIG_LOCATION_PROPERTY);
        System.setProperty(
                CONFIG_LOCATION_PROPERTY,
                configFile.toAbsolutePath().toString().replace('\\', '/')
        );

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(DatabaseConfig.class)) {
            HikariDataSource dataSource = context.getBean(HikariDataSource.class);
            SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);
            PlatformTransactionManager transactionManager =
                    context.getBean(PlatformTransactionManager.class);

            assertEquals("jdbc:mysql://localhost:3307/test_schema", dataSource.getJdbcUrl());
            assertEquals("test_user", dataSource.getUsername());
            assertEquals(7, dataSource.getMaximumPoolSize());
            assertEquals(1, dataSource.getMinimumIdle());
            assertSame(
                    dataSource,
                    sqlSessionFactory.getConfiguration().getEnvironment().getDataSource()
            );
            assertNotNull(transactionManager);
            assertSame(
                    dataSource,
                    ((DataSourceTransactionManager) transactionManager).getDataSource()
            );
        } finally {
            restoreSystemProperty(previousLocation);
        }
    }

    private void restoreSystemProperty(String previousLocation) {
        if (previousLocation == null) {
            System.clearProperty(CONFIG_LOCATION_PROPERTY);
            return;
        }
        System.setProperty(CONFIG_LOCATION_PROPERTY, previousLocation);
    }
}
