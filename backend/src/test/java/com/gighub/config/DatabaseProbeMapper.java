package com.gighub.config;

/**
 * 로컬 데이터베이스 연결 확인에만 사용하는 Test 전용 MyBatis Mapper입니다.
 */
public interface DatabaseProbeMapper {

    /**
     * 현재 연결된 MySQL Database 이름을 조회합니다.
     *
     * @return 현재 Database 이름
     */
    String selectDatabaseName();

    /**
     * 기준 테이블에 실제 조회가 가능한지 확인합니다.
     *
     * @return users 테이블 행 수
     */
    long countUsers();
}
