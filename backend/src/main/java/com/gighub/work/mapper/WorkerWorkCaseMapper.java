package com.gighub.work.mapper;

import com.gighub.work.mapper.result.WorkerWorkCaseRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkerWorkCaseMapper {

    WorkerWorkCaseRow findToday(
            @Param("workerId") Long workerId,
            @Param("previousDayStart") LocalDateTime previousDayStart,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("tomorrowStart") LocalDateTime tomorrowStart);

    List<WorkerWorkCaseRow> findPage(
            @Param("workerId") Long workerId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long count(@Param("workerId") Long workerId);
}
