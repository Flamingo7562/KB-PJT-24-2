package com.gighub.config;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tomcat Root Context와 같은 설정으로 로컬 Docker MySQL을 조회하는 수동 통합 테스트입니다.
 */
@Tag("database")
class DatabaseConnectionIntegrationTest {

    /**
     * 외부 properties로 연결한 DB 이름과 users 테이블 행 수를 조회합니다.
     */
    @Test
    void readsConfiguredDatabaseAndUsersTable() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);

            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                DatabaseProbeMapper mapper = sqlSession.getMapper(DatabaseProbeMapper.class);
                String databaseName = mapper.selectDatabaseName();
                long userCount = mapper.countUsers();

                assertFalse(databaseName.isBlank());
                assertTrue(userCount >= 0);
                System.out.printf(
                        "Connected database: %s, users table rows: %d%n",
                        databaseName,
                        userCount
                );
            }
        }
    }
}
