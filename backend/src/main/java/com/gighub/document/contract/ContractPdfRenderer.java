package com.gighub.document.contract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link ContractSnapshot}로 근로계약서 ORIGINAL PDF(Version 1)를 렌더링합니다
 * (DEC-CONTRACT-AUTO-GENERATION, DEC-DOCUMENT-STORAGE).
 *
 * <p>배포물에 포함된 Pretendard(SIL OFL 1.1) 한글 Font를 Embed해 서버 환경과 관계없이
 * 같은 글꼴로 렌더링한다. 문서 정보의 생성·수정 시각은 렌더링을 수행한 시각이 아니라
 * {@link ContractSnapshot#acceptedAt()}로 고정해, 같은 Snapshot을 다시 렌더링해도 문서
 * Metadata가 매번 달라지지 않게 한다.</p>
 *
 * <p>ORIGINAL과 SIGNED는 같은 계약 조건을 렌더링하고, SIGNED에만 WORKER의 이름 직접 입력
 * 증거와 서명 시각을 덧붙입니다.</p>
 */
@Component
public class ContractPdfRenderer {

    private static final String FONT_REGULAR = "/fonts/Pretendard-Regular.ttf";
    private static final String FONT_BOLD = "/fonts/Pretendard-Bold.ttf";
    private static final float MARGIN = 50f;
    private static final float TITLE_SIZE = 18f;
    private static final float BODY_SIZE = 11f;
    private static final float LEADING = 18f;
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA);

    /** ORIGINAL Version(서명 Section 없음)을 렌더링한다. */
    public byte[] render(ContractSnapshot snapshot) {
        return render(snapshot, null);
    }

    /**
     * SIGNED Version을 렌더링한다.
     *
     * <p>{@code signature}가 있으면 본문 뒤에 서명 Section을 덧붙인다. ORIGINAL과 SIGNED는
     * 이 Section 유무로만 갈리고 나머지 계약 내용은 같은 {@link ContractSnapshot}에서 온다.</p>
     */
    public byte[] render(ContractSnapshot snapshot, ContractSnapshot.Signature signature) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont regular = loadFont(document, FONT_REGULAR);
            PDFont bold = loadFont(document, FONT_BOLD);
            applyDeterministicMetadata(document, snapshot);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float maxWidth = page.getMediaBox().getWidth() - (MARGIN * 2);
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = writeLine(stream, bold, TITLE_SIZE, MARGIN, y, "근로계약서");
                y -= LEADING;

                for (String line : bodyLines(snapshot)) {
                    for (String wrapped : wrap(regular, BODY_SIZE, line, maxWidth)) {
                        y = writeLine(stream, regular, BODY_SIZE, MARGIN, y, wrapped);
                    }
                }

                if (signature != null) {
                    y -= LEADING;
                    y = writeLine(stream, bold, BODY_SIZE, MARGIN, y, "서명");
                    for (String line : signatureLines(signature)) {
                        for (String wrapped : wrap(regular, BODY_SIZE, line, maxWidth)) {
                            y = writeLine(stream, regular, BODY_SIZE, MARGIN, y, wrapped);
                        }
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ContractDocumentGenerationException("근로계약서 PDF 생성에 실패했습니다.", e);
        }
    }

    private List<String> bodyLines(ContractSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("제목: " + snapshot.title());
        lines.add("사업장: " + snapshot.workplaceName() + " (" + snapshot.workplaceAddress() + ")");
        lines.add("근무 시간: " + formatDateTime(snapshot.startsAt())
                + " ~ " + formatDateTime(snapshot.endsAt()));
        lines.add("휴게 시간: " + snapshot.breakMinutes() + "분 ("
                + (snapshot.breakPaid() ? "유급" : "무급") + ")");
        lines.add("합의 일급: " + formatWon(snapshot.agreedWage()));
        lines.add("사장: " + snapshot.employerName());
        lines.add("근로자: " + snapshot.workerName());
        lines.add("약관 버전: v" + snapshot.sourceTermsVersion());
        lines.add("수락 일시: " + formatDateTime(snapshot.acceptedAt()));
        return lines;
    }

    private List<String> signatureLines(ContractSnapshot.Signature signature) {
        List<String> lines = new ArrayList<>();
        lines.add("근로자(서명): " + signature.typedName());
        lines.add("서명 방식: 이름 직접 입력(TYPED_NAME)");
        lines.add("서명 일시: " + formatDateTime(signature.signedAt()));
        return lines;
    }

    private void applyDeterministicMetadata(PDDocument document, ContractSnapshot snapshot) {
        Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
        fixed.clear();
        LocalDateTime acceptedAt = snapshot.acceptedAt();
        fixed.set(
                acceptedAt.getYear(), acceptedAt.getMonthValue() - 1, acceptedAt.getDayOfMonth(),
                acceptedAt.getHour(), acceptedAt.getMinute(), acceptedAt.getSecond());

        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle("근로계약서 - work_case " + snapshot.workCaseId());
        info.setCreationDate(fixed);
        info.setModificationDate(fixed);
    }

    private PDFont loadFont(PDDocument document, String resourcePath) throws IOException {
        try (InputStream fontStream = getClass().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                throw new IOException("Font 자원을 찾을 수 없습니다: " + resourcePath);
            }
            return PDType0Font.load(document, fontStream);
        }
    }

    private float writeLine(
            PDPageContentStream stream, PDFont font, float fontSize,
            float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
        return y - LEADING;
    }

    /** 페이지 폭을 넘는 줄을 Font 실측 폭 기준으로 나눈다(단어 단위 언어가 아니므로 글자 단위로 자른다). */
    private List<String> wrap(PDFont font, float fontSize, String text, float maxWidth)
            throws IOException {
        List<String> wrapped = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String candidate = current.toString() + c;
            if (widthOf(font, fontSize, candidate) > maxWidth && current.length() > 0) {
                wrapped.add(current.toString());
                current = new StringBuilder();
            }
            current.append(c);
        }
        if (current.length() > 0) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    private float widthOf(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(DATE_TIME_FORMAT);
    }

    private String formatWon(long amount) {
        return String.format(Locale.KOREA, "%,d원", amount);
    }
}
