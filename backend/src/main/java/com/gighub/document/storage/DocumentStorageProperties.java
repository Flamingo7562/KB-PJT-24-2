package com.gighub.document.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * 계약 문서 비공개 저장소 경로를 외부 설정에서 읽습니다(DEC-DOCUMENT-STORAGE).
 *
 * <p>값이 없으면 저장소를 찾을 수 없으므로 기동 시점에 실패시킵니다. 저장 경로는 Web Root나
 * 공개 정적 자원 경로가 아닌 별도 디렉터리여야 합니다.</p>
 */
@Component
public class DocumentStorageProperties {

    private static final String BASE_PATH_KEY = "document.storage.base-path";

    private final Path basePath;

    @Autowired
    public DocumentStorageProperties(Environment environment) {
        this(environment.getRequiredProperty(BASE_PATH_KEY));
    }

    DocumentStorageProperties(String basePath) {
        this.basePath = normalizeBasePath(basePath);
    }

    /** 계약 문서 Storage Key를 해석하는 기준 디렉터리입니다. */
    public Path getBasePath() {
        return basePath;
    }

    private static Path normalizeBasePath(String value) {
        String trimmed = Objects.requireNonNull(value, BASE_PATH_KEY).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException(BASE_PATH_KEY + "는 빈 값일 수 없습니다.");
        }
        return Paths.get(trimmed).toAbsolutePath().normalize();
    }
}
