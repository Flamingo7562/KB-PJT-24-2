package com.gighub.document.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** {@code document_versions} 삽입 Parameter입니다. */
@Getter
@Builder
public class DocumentVersionInsertParam {

    /** MyBatis가 생성 Key를 되돌려 쓰기 위해 이 필드만 가변입니다. */
    @Setter
    private Long id;

    private final Long documentId;
    private final Integer versionNo;
    private final String versionType;
    private final String storageKey;
    private final String mimeType;
    private final Long sizeBytes;
    private final byte[] checksum;
}
