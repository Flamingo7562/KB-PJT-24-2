package com.gighub.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageMetaTest {

    @Test
    void roundsTotalPagesUp() {
        PageMeta meta = PageMeta.of(0, 20, 41);

        assertEquals(0, meta.getNumber());
        assertEquals(20, meta.getSize());
        assertEquals(41, meta.getTotalElements());
        assertEquals(3, meta.getTotalPages());
    }

    @Test
    void reportsZeroPagesWhenEmpty() {
        assertEquals(0, PageMeta.of(0, 20, 0).getTotalPages());
    }

    /**
     * 검증을 건너뛴 호출이 Infinity나 NaN에서 나온 totalPages를 응답에 싣지 않고
     * 프로그래밍 오류로 끊기는지 확인합니다.
     */
    @Test
    void rejectsSizeBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> PageMeta.of(0, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> PageMeta.of(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PageMeta.of(0, -1, 5));
    }

    @Test
    void rejectsNegativePageNumber() {
        assertThrows(IllegalArgumentException.class, () -> PageMeta.of(-1, 20, 5));
    }
}
