package com.bookstore.service;

import com.bookstore.dto.AiChatRequest;
import com.bookstore.dto.AiChatResponse;
import com.bookstore.dto.AiPreviewRequest;
import com.bookstore.dto.AiSummaryDto;
import com.bookstore.model.Book;
import com.bookstore.model.BookAiSummary;
import com.bookstore.model.BookVariant;
import com.bookstore.repository.BookAiSummaryRepository;
import com.bookstore.repository.BookRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dịch vụ Trợ Lý AI Tư Vấn & Tóm Tắt Sách (Phong cách Conversational AI như Tiki).
 *
 * Bao gồm 2 nghiệp vụ cốt lõi:
 * 1. Tóm tắt sách theo yêu cầu khách hàng trong cửa sổ chat (kết hợp Database Cache ngầm để tăng tốc và tiết kiệm chi phí token).
 * 2. Hội thoại hỏi - đáp trực tiếp (Multi-turn Q&A) dựa trên nội dung PDF đọc thử, giá các phiên bản bìa,
 *    tồn kho thực tế và danh mục sách tương tự trong Oracle Database.
 */
@Service
@Transactional
public class AiBookSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiBookSummaryService.class);

    private final BookRepository bookRepository;
    private final BookAiSummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    public AiBookSummaryService(BookRepository bookRepository,
                                BookAiSummaryRepository summaryRepository,
                                ObjectMapper objectMapper) {
        this.bookRepository = bookRepository;
        this.summaryRepository = summaryRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    private String getCleanApiKey() {
        if (openAiApiKey == null) {
            return "";
        }
        return openAiApiKey.trim().replaceAll("^['\"]+|['\"]+$", "").trim();
    }

    public boolean isAiApiConfigured() {
        return !getCleanApiKey().isEmpty();
    }

    // ================= 1. TÓM TẮT NỘI DUNG SÁCH TRONG KHUNG CHAT (TIKI STYLE) =================

    public AiSummaryDto getOrGenerateSummary(Long bookId, boolean forceRefresh) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đầu sách với ID: " + bookId));

        Optional<BookAiSummary> existingOpt = summaryRepository.findByBookId(bookId);
        if (existingOpt.isPresent() && !forceRefresh) {
            BookAiSummary cachedEntity = existingOpt.get();
            // Nếu bản cũ chưa có cấu trúc sections kiểu Tiki và đã gắn OpenAI Key thì nâng cấp tự động 1 lần
            boolean hasRichSections = cachedEntity.getSectionsJson() != null && !cachedEntity.getSectionsJson().isBlank();
            if (hasRichSections || !isAiApiConfigured()) {
                return toSummaryDto(book, cachedEntity, true);
            }
        }

        AiPreviewRequest req = new AiPreviewRequest();
        req.setTitle(book.getTitle());
        req.setAuthor(book.getAuthor());
        req.setCategoryName(book.getCategory() != null ? book.getCategory().getName() : null);
        req.setPublisher(book.getPublisher());
        req.setPublishYear(book.getPublishYear());
        req.setDescription(book.getDescription());
        req.setSampleExcerpt(book.getSampleExcerpt());

        GeneratedSummary generated = generateStructuredSummary(req);

        BookAiSummary entity = existingOpt.orElseGet(() -> {
            BookAiSummary fresh = new BookAiSummary();
            fresh.setBook(book);
            fresh.setGeneratedCount(0);
            return fresh;
        });

        entity.setHookQuestion(generated.hookQuestion);
        entity.setShortSummary(generated.shortSummary);
        try {
            entity.setSectionsJson(objectMapper.writeValueAsString(generated.sections));
        } catch (Exception ignored) {
            entity.setSectionsJson("[]");
        }
        entity.setKeyTakeaways(String.join("\n", generated.takeaways));
        entity.setTargetAudience(generated.targetAudience);
        entity.setAuthorBio(generated.authorBio);
        entity.setReadingTimeMinutes(generated.readingTimeMinutes);
        entity.setAiModel(generated.modelUsed);
        entity.setGeneratedCount((entity.getGeneratedCount() == null ? 0 : entity.getGeneratedCount()) + 1);
        entity.setUpdatedAt(LocalDateTime.now());

        BookAiSummary saved = summaryRepository.save(entity);
        book.setAiSummary(saved);

        return toSummaryDto(book, saved, false);
    }

    @Transactional(readOnly = true)
    public AiSummaryDto generatePreview(AiPreviewRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Tên sách trước khi tạo tóm tắt bằng AI.");
        }
        GeneratedSummary generated = generateStructuredSummary(request);
        AiSummaryDto dto = new AiSummaryDto();
        dto.setBookTitle(request.getTitle());
        dto.setBookAuthor(request.getAuthor());
        dto.setHookQuestion(generated.hookQuestion);
        dto.setShortSummary(generated.shortSummary);
        dto.setSections(generated.sections);
        dto.setTakeaways(generated.takeaways);
        dto.setTargetAudience(generated.targetAudience);
        dto.setAuthorBio(generated.authorBio);
        dto.setReadingTimeMinutes(generated.readingTimeMinutes);
        dto.setAiModel(generated.modelUsed);
        dto.setCached(false);
        return dto;
    }

    // ================= 2. HỘI THOẠI HỎI ĐÁP TRỰC TIẾP VỚI TRỢ LÝ AI =================

    @Transactional(readOnly = true)
    public AiChatResponse chatAboutBook(Long bookId, AiChatRequest chatRequest) {
        Book currentBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đầu sách: " + bookId));

        String userMessage = chatRequest.getMessage() != null ? chatRequest.getMessage().trim() : "";
        if (userMessage.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung câu hỏi.");
        }

        List<Book> allBooks = bookRepository.findAll();

        if (isAiApiConfigured()) {
            try {
                return callOpenAiChat(currentBook, allBooks, chatRequest);
            } catch (Exception ex) {
                log.warn("Không thể gọi OpenAI Chat ({}), chuyển sang bộ trả lời ngữ cảnh dự phòng.", ex.getMessage());
            }
        }
        return buildFallbackChatResponse(currentBook, allBooks, userMessage);
    }

    // ================= KẾT NỐI OPENAI API (SUMMARY & MULTI-TURN CHAT) =================

    private GeneratedSummary generateStructuredSummary(AiPreviewRequest req) {
        if (isAiApiConfigured()) {
            try {
                return callOpenAiSummaryApi(req);
            } catch (Exception ex) {
                log.warn("Không thể gọi OpenAI Summary API ({}), chuyển sang bộ phân tích dự phòng.", ex.getMessage());
            }
        }
        return buildContextualSummary(req);
    }

    private GeneratedSummary callOpenAiSummaryApi(AiPreviewRequest req) throws Exception {
        String prompt = buildTikiEditorialPrompt(req);
        String modelName = getModelName();

        Map<String, Object> systemMsg = Map.of(
                "role", "system",
                "content", "Bạn là Trợ lý AI tư vấn sách thông minh trên nền tảng thương mại điện tử sách (tương tự Trợ lý AI của Tiki). Luôn phản hồi bằng một đối tượng JSON hợp lệ bằng tiếng Việt, văn phong cuốn hút, chuẩn xác, dễ đọc lướt."
        );
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);

        JsonNode parsed = executeOpenAiJsonRequest(modelName, List.of(systemMsg, userMsg), 0.45);

        String hookQuestion = parsed.path("hookQuestion").asText("").trim();
        String shortSummary = parsed.path("shortSummary").asText("").trim();
        String targetAudience = parsed.path("targetAudience").asText("").trim();
        String authorBio = parsed.path("authorBio").asText("").trim();
        int readingTimeMinutes = parsed.path("readingTimeMinutes").asInt(240);

        List<AiSummaryDto.SectionItem> sections = new ArrayList<>();
        JsonNode secArr = parsed.path("sections");
        if (secArr.isArray()) {
            for (JsonNode s : secArr) {
                String heading = s.path("heading").asText("").trim();
                String subtitle = s.path("subtitle").asText("").trim();
                String content = s.path("content").asText("").trim();
                if (!heading.isEmpty() && !content.isEmpty()) {
                    sections.add(new AiSummaryDto.SectionItem(heading, subtitle, content));
                }
            }
        }

        List<String> takeaways = new ArrayList<>();
        JsonNode arr = parsed.path("keyTakeaways");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                String line = item.asText("").trim();
                if (!line.isEmpty()) {
                    takeaways.add(line);
                }
            }
        }

        if (shortSummary.isEmpty() && sections.isEmpty()) {
            throw new IllegalStateException("Phản hồi JSON từ OpenAI thiếu nội dung tóm tắt.");
        }

        return new GeneratedSummary(
                hookQuestion.isEmpty() ? ("Bạn đã từng tự hỏi điều gì làm nên sức hút đặc biệt của \"" + nullSafe(req.getTitle()) + "\" chưa?") : hookQuestion,
                shortSummary,
                sections,
                takeaways,
                targetAudience,
                authorBio.isEmpty() ? (notBlank(req.getAuthor()) ? req.getAuthor() + " là tác giả có phong cách viết sâu sắc và giàu tính thực tiễn." : "") : authorBio,
                Math.max(45, Math.min(readingTimeMinutes, 1200)),
                modelName
        );
    }

    private AiChatResponse callOpenAiChat(Book currentBook, List<Book> allBooks, AiChatRequest chatRequest) throws Exception {
        String modelName = getModelName();
        String systemPrompt = buildChatSystemPrompt(currentBook, allBooks);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (chatRequest.getHistory() != null) {
            int size = chatRequest.getHistory().size();
            int start = Math.max(0, size - 6);
            for (int i = start; i < size; i++) {
                AiChatRequest.ChatTurn turn = chatRequest.getHistory().get(i);
                if (turn != null && notBlank(turn.getContent())) {
                    String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "assistant" : "user";
                    messages.add(Map.of("role", role, "content", trimToLength(turn.getContent(), 1200)));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", chatRequest.getMessage().trim()));

        JsonNode parsed = executeOpenAiJsonRequest(modelName, messages, 0.5);

        String answer = parsed.path("answer").asText("").trim();
        if (answer.isEmpty()) {
            throw new IllegalStateException("OpenAI không trả về trường answer.");
        }

        List<String> suggestions = new ArrayList<>();
        JsonNode sugNode = parsed.path("suggestedQuestions");
        if (sugNode.isArray()) {
            for (JsonNode n : sugNode) {
                String q = n.asText("").trim();
                if (!q.isEmpty() && suggestions.size() < 3) {
                    suggestions.add(q);
                }
            }
        }

        Set<Long> recIds = new LinkedHashSet<>();
        JsonNode recNode = parsed.path("recommendedBookIds");
        if (recNode.isArray()) {
            for (JsonNode idNode : recNode) {
                if (idNode.canConvertToLong()) {
                    recIds.add(idNode.asLong());
                }
            }
        }

        Map<Long, Book> bookMap = new HashMap<>();
        for (Book b : allBooks) {
            bookMap.put(b.getId(), b);
        }

        List<AiChatResponse.MiniBookCard> cards = new ArrayList<>();
        for (Long rid : recIds) {
            Book b = bookMap.get(rid);
            if (b != null && cards.size() < 3) {
                cards.add(toMiniCard(b));
            }
        }

        AiChatResponse res = new AiChatResponse();
        res.setAnswer(answer);
        res.setSuggestedQuestions(suggestions);
        res.setRecommendedBooks(cards);
        res.setAiModel(modelName);
        return res;
    }

    private JsonNode executeOpenAiJsonRequest(String modelName, List<Map<String, Object>> messages, double temperature) throws Exception {
        String baseUrl = (openAiBaseUrl != null && !openAiBaseUrl.isBlank())
                ? openAiBaseUrl.trim().replaceAll("/+$", "")
                : "https://api.openai.com/v1";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);
        payload.put("temperature", temperature);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", messages);

        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + getCleanApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("OpenAI phản hồi rỗng.");
        }

        return objectMapper.readTree(stripMarkdownFences(contentNode.asText()));
    }

    private String buildTikiEditorialPrompt(AiPreviewRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hãy đóng vai Trợ lý AI của nhà sách trực tuyến, tóm tắt cuốn sách dưới đây theo đúng bố cục sinh động giống Trợ lý AI của Tiki.\n");
        sb.append("Không bịa đặt thông tin sai lệch; ưu tiên bám sát nội dung đọc thử nếu được cung cấp.\n\n");
        sb.append("- Tên sách: ").append(nullSafe(req.getTitle())).append("\n");
        sb.append("- Tác giả: ").append(nullSafe(req.getAuthor())).append("\n");
        if (notBlank(req.getCategoryName())) {
            sb.append("- Thể loại: ").append(req.getCategoryName().trim()).append("\n");
        }
        if (notBlank(req.getPublisher())) {
            sb.append("- Nhà xuất bản: ").append(req.getPublisher().trim()).append("\n");
        }
        if (req.getPublishYear() != null) {
            sb.append("- Năm xuất bản: ").append(req.getPublishYear()).append("\n");
        }
        if (notBlank(req.getDescription())) {
            sb.append("- Mô tả giới thiệu: ").append(req.getDescription().trim()).append("\n");
        }
        if (notBlank(req.getSampleExcerpt())) {
            String excerpt = req.getSampleExcerpt().trim();
            if (excerpt.length() > 12000) {
                excerpt = excerpt.substring(0, 12000) + "...";
            }
            sb.append("- Nội dung trích xuất từ bản đọc thử PDF / Mục lục:\n\"\"\"\n").append(excerpt).append("\n\"\"\"\n");
        }

        sb.append("\nTrả về DUY NHẤT một đối tượng JSON hợp lệ theo cấu trúc sau:\n");
        sb.append("{\n");
        sb.append("  \"hookQuestion\": \"Một câu hỏi mở đầu khơi gợi sự đồng cảm hoặc tò mò của người đọc (1 câu ngắn gọn)?\",\n");
        sb.append("  \"shortSummary\": \"2-3 câu khái quát bức tranh tổng thể và tinh thần xuyên suốt của cuốn sách.\",\n");
        sb.append("  \"sections\": [\n");
        sb.append("    {\n");
        sb.append("      \"heading\": \"1. [Tên phần / Chủ đề cốt lõi thứ nhất]\",\n");
        sb.append("      \"subtitle\": \"[Thông điệp điểm nhấn ngắn gọn của phần này]\",\n");
        sb.append("      \"content\": \"2-3 câu phân tích nội dung trọng tâm của phần này.\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"heading\": \"2. [Tên phần / Chủ đề cốt lõi thứ hai]\",\n");
        sb.append("      \"subtitle\": \"[Thông điệp điểm nhấn ngắn gọn của phần này]\",\n");
        sb.append("      \"content\": \"2-3 câu phân tích nội dung trọng tâm của phần này.\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"heading\": \"3. [Tên phần / Chủ đề cốt lõi thứ ba]\",\n");
        sb.append("      \"subtitle\": \"[Thông điệp điểm nhấn ngắn gọn của phần này]\",\n");
        sb.append("      \"content\": \"2-3 câu phân tích nội dung trọng tâm của phần này.\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"keyTakeaways\": [\n");
        sb.append("    \"Lý do nên đọc / Giá trị nhận được số 1\",\n");
        sb.append("    \"Lý do nên đọc / Giá trị nhận được số 2\",\n");
        sb.append("    \"Lý do nên đọc / Giá trị nhận được số 3\"\n");
        sb.append("  ],\n");
        sb.append("  \"targetAudience\": \"1-2 câu chỉ rõ ai nên đọc cuốn sách này.\",\n");
        sb.append("  \"authorBio\": \"2 câu giới thiệu ngắn gọn về uy tín và phong cách của tác giả.\",\n");
        sb.append("  \"readingTimeMinutes\": 240\n");
        sb.append("}");
        return sb.toString();
    }

    private String buildChatSystemPrompt(Book currentBook, List<Book> allBooks) {
        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là 'Trợ lý AI' thông minh của nhà sách trực tuyến BookStore (giống Trợ lý AI trên Tiki).\n");
        sb.append("Khách hàng đang đứng ở trang chi tiết của cuốn sách sau:\n");
        sb.append("- ID sách hiện tại: ").append(currentBook.getId()).append("\n");
        sb.append("- Tên sách: ").append(currentBook.getTitle()).append("\n");
        sb.append("- Tác giả: ").append(currentBook.getAuthor()).append("\n");
        sb.append("- Thể loại: ").append(currentBook.getCategory() != null ? currentBook.getCategory().getName() : "Chưa phân loại").append("\n");
        if (notBlank(currentBook.getPublisher())) {
            sb.append("- NXB: ").append(currentBook.getPublisher()).append(currentBook.getPublishYear() != null ? (" (" + currentBook.getPublishYear() + ")") : "").append("\n");
        }
        sb.append("- Các phiên bản bìa & tồn kho thực tế của cuốn sách này tại shop:\n");
        for (BookVariant v : currentBook.getVariants()) {
            sb.append("  + ").append(v.getVariantName())
              .append(" | Giá bán: ").append(vnFormat.format(v.getPrice())).append(" đ")
              .append(" | Tồn kho: ").append(v.getStockQuantity() != null && v.getStockQuantity() > 0 ? ("Còn " + v.getStockQuantity() + " cuốn") : "Tạm hết hàng")
              .append("\n");
        }
        if (notBlank(currentBook.getDescription())) {
            sb.append("- Mô tả sách: ").append(currentBook.getDescription().trim()).append("\n");
        }
        if (notBlank(currentBook.getSampleExcerpt())) {
            String excerpt = currentBook.getSampleExcerpt().trim();
            if (excerpt.length() > 8000) {
                excerpt = excerpt.substring(0, 8000) + "...";
            }
            sb.append("- Nội dung bản đọc thử (PDF / trích đoạn):\n\"\"\"\n").append(excerpt).append("\n\"\"\"\n");
        }

        sb.append("\nDanh sách các đầu sách KHÁC hiện đang có trong kho cửa hàng (chỉ gợi ý sách có trong danh sách này nếu khách hỏi tìm thêm sách):\n");
        for (Book b : allBooks) {
            if (!Objects.equals(b.getId(), currentBook.getId())) {
                sb.append("  * [ID=").append(b.getId()).append("] ")
                  .append(b.getTitle()).append(" - Tác giả: ").append(b.getAuthor())
                  .append(" | Thể loại: ").append(b.getCategory() != null ? b.getCategory().getName() : "")
                  .append(" | Giá từ: ").append(vnFormat.format(b.getMinPrice())).append(" đ")
                  .append(" | ").append(b.isInStock() ? "Còn hàng" : "Hết hàng")
                  .append("\n");
            }
        }

        sb.append("\nQuy tắc trả lời:\n");
        sb.append("1. Trả lời tự nhiên, thân thiện, ngắn gọn súc tích bằng tiếng Việt; có thể dùng **in đậm** và gạch đầu dòng '-' để dễ đọc.\n");
        sb.append("2. Báo đúng giá bán và tồn kho thật trong DB khi khách hỏi về phiên bản bìa hoặc giá.\n");
        sb.append("3. Nếu khuyên mua cuốn hiện tại hoặc gợi ý sách khác trong shop, đưa ID sách vào mảng recommendedBookIds (tối đa 2 ID) để giao diện hiển thị thẻ mua hàng nhanh.\n");
        sb.append("4. Bắt buộc trả về DUY NHẤT một đối tượng JSON hợp lệ theo mẫu:\n");
        sb.append("{\n");
        sb.append("  \"answer\": \"Câu trả lời dành cho khách hàng (hỗ trợ xuống dòng, **in đậm** và danh sách gạch đầu dòng - )\",\n");
        sb.append("  \"recommendedBookIds\": [").append(currentBook.getId()).append("],\n");
        sb.append("  \"suggestedQuestions\": [\"Câu hỏi gợi ý tiếp theo 1?\", \"Câu hỏi gợi ý tiếp theo 2?\"]\n");
        sb.append("}");
        return sb.toString();
    }

    // ================= CHUYỂN ĐỔI ENTITY <-> DTO & DỰ PHÒNG OFFLINE =================

    private AiSummaryDto toSummaryDto(Book book, BookAiSummary entity, boolean cached) {
        AiSummaryDto dto = new AiSummaryDto();
        dto.setBookId(book.getId());
        dto.setBookTitle(book.getTitle());
        dto.setBookAuthor(book.getAuthor());
        dto.setBookImage(book.getImageUrl());

        BookVariant def = book.getDefaultVariant();
        if (def != null) {
            dto.setDefaultVariantId(def.getId());
            dto.setDefaultVariantName(def.getVariantName());
            dto.setDefaultPrice(def.getPrice());
            dto.setInStock(def.isInStock());
        }

        dto.setHookQuestion(notBlank(entity.getHookQuestion())
                ? entity.getHookQuestion()
                : ("Bạn đã từng tự hỏi điều gì tạo nên giá trị vượt thời gian của \"" + book.getTitle() + "\" chưa?"));
        dto.setShortSummary(entity.getShortSummary());

        List<AiSummaryDto.SectionItem> sections = new ArrayList<>();
        if (notBlank(entity.getSectionsJson())) {
            try {
                sections = objectMapper.readValue(entity.getSectionsJson(), new TypeReference<List<AiSummaryDto.SectionItem>>() {});
            } catch (Exception ignored) {
            }
        }
        List<String> takeaways = entity.getTakeawayList();
        if (sections.isEmpty() && !takeaways.isEmpty()) {
            int count = Math.min(3, takeaways.size());
            for (int i = 0; i < count; i++) {
                sections.add(new AiSummaryDto.SectionItem(
                        (i + 1) + ". Luận điểm trọng tâm thứ " + (i + 1),
                        "Góc nhìn cốt lõi từ tác phẩm",
                        takeaways.get(i)
                ));
            }
        }
        dto.setSections(sections);
        dto.setTakeaways(takeaways);
        dto.setTargetAudience(entity.getTargetAudience());
        dto.setAuthorBio(notBlank(entity.getAuthorBio())
                ? entity.getAuthorBio()
                : (book.getAuthor() + " là tác giả đứng sau \"" + book.getTitle() + "\", nổi bật với lối viết mạch lạc, truyền cảm hứng và giàu giá trị thực tiễn."));
        dto.setReadingTimeMinutes(entity.getReadingTimeMinutes());
        dto.setFormattedReadingTime(entity.getFormattedReadingTime());
        dto.setAiModel(entity.getAiModel());
        dto.setUpdatedAt(entity.getFormattedUpdatedAt());
        dto.setCached(cached);
        return dto;
    }

    private AiChatResponse.MiniBookCard toMiniCard(Book b) {
        BookVariant def = b.getDefaultVariant();
        return new AiChatResponse.MiniBookCard(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                b.getImageUrl(),
                def != null ? def.getId() : null,
                def != null ? def.getVariantName() : "Bản tiêu chuẩn",
                def != null ? def.getPrice() : b.getMinPrice(),
                b.isInStock()
        );
    }

    private AiChatResponse buildFallbackChatResponse(Book currentBook, List<Book> allBooks, String userMessage) {
        String lower = userMessage.toLowerCase(Locale.ROOT);
        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        AiChatResponse res = new AiChatResponse();
        res.setAiModel("Editorial-AI-Assistant");

        if (lower.contains("bìa") || lower.contains("phiên bản") || lower.contains("ấn bản") || lower.contains("giá") || lower.contains("kho")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Hiện tại cuốn **").append(currentBook.getTitle()).append("** đang có các phiên bản sau tại BookStore:\n");
            for (BookVariant v : currentBook.getVariants()) {
                sb.append("- **").append(v.getVariantName()).append("**: ")
                  .append(vnFormat.format(v.getPrice())).append(" đ (")
                  .append(v.isInStock() ? ("Còn " + v.getStockQuantity() + " cuốn") : "Tạm hết hàng").append(")\n");
            }
            sb.append("\nNếu bạn mua để đọc hằng ngày tiện mang theo thì bản **Bìa mềm** rất kinh tế; còn nếu muốn sưu tầm lâu dài hoặc làm quà tặng thì bản **Bìa cứng / Đặc biệt** rất đáng cân nhắc!");
            res.setAnswer(sb.toString());
            res.setRecommendedBooks(List.of(toMiniCard(currentBook)));
            res.setSuggestedQuestions(List.of("Sách này có hợp với người mới bắt đầu không?", "Gợi ý thêm sách tương tự tại shop"));
            return res;
        }

        if (lower.contains("tương tự") || lower.contains("cùng thể loại") || lower.contains("gợi ý") || lower.contains("cuốn khác")) {
            List<AiChatResponse.MiniBookCard> cards = new ArrayList<>();
            StringBuilder sb = new StringBuilder("Dựa trên chủ đề của **").append(currentBook.getTitle()).append("**, bạn có thể tham khảo thêm các tựa sách đang có sẵn tại cửa hàng:\n");
            for (Book b : allBooks) {
                if (!Objects.equals(b.getId(), currentBook.getId()) && b.isInStock()) {
                    boolean sameCat = currentBook.getCategory() != null && b.getCategory() != null
                            && Objects.equals(currentBook.getCategory().getId(), b.getCategory().getId());
                    if (sameCat || cards.size() < 2) {
                        cards.add(toMiniCard(b));
                        sb.append("- **").append(b.getTitle()).append("** (").append(b.getAuthor()).append(") — giá từ ")
                          .append(vnFormat.format(b.getMinPrice())).append(" đ\n");
                        if (cards.size() >= 2) break;
                    }
                }
            }
            res.setAnswer(sb.toString());
            res.setRecommendedBooks(cards);
            res.setSuggestedQuestions(List.of("So sánh các phiên bản bìa của sách này", "Tóm tắt nội dung sách"));
            return res;
        }

        String desc = notBlank(currentBook.getDescription()) ? currentBook.getDescription().trim() : "";
        String excerpt = notBlank(currentBook.getSampleExcerpt()) ? ("\n\nĐiểm nhấn từ bản đọc thử: " + trimToLength(currentBook.getSampleExcerpt().trim(), 280)) : "";
        res.setAnswer("Về cuốn **" + currentBook.getTitle() + "** của tác giả **" + currentBook.getAuthor() + "**:\n"
                + desc + excerpt
                + "\n\nCuốn sách được trình bày mạch lạc, rất phù hợp cho cả người mới tìm hiểu lẫn bạn đọc muốn đào sâu ứng dụng thực tế.");
        res.setRecommendedBooks(List.of(toMiniCard(currentBook)));
        res.setSuggestedQuestions(List.of("Tư vấn chọn phiên bản bìa phù hợp", "Shop còn cuốn nào cùng chủ đề không?"));
        return res;
    }

    private GeneratedSummary buildContextualSummary(AiPreviewRequest req) {
        String title = nullSafe(req.getTitle());
        String author = notBlank(req.getAuthor()) ? req.getAuthor().trim() : "Tác giả";
        String category = notBlank(req.getCategoryName()) ? req.getCategoryName().trim() : "Sách tuyển chọn";
        String desc = notBlank(req.getDescription())
                ? req.getDescription().trim()
                : ("Tác phẩm tiêu biểu thuộc lĩnh vực " + category + ".");

        List<AiSummaryDto.SectionItem> sections = List.of(
                new AiSummaryDto.SectionItem(
                        "1. Bức tranh tư duy nền tảng",
                        "Khởi nguồn từ những vấn đề cốt lõi nhất",
                        trimToLength(desc, 220)
                ),
                new AiSummaryDto.SectionItem(
                        "2. Phương pháp & Góc nhìn thực tiễn",
                        "Chuyển hóa lý thuyết thành hành động rõ ràng",
                        notBlank(req.getSampleExcerpt())
                                ? trimToLength(req.getSampleExcerpt().trim(), 240)
                                : ("Tác giả " + author + " dẫn dắt người đọc đi qua từng luận điểm chặt chẽ kèm ví dụ gần gũi, dễ liên hệ thực tế.")
                ),
                new AiSummaryDto.SectionItem(
                        "3. Giá trị đọng lại dài lâu",
                        "Định hình thói quen và chiều sâu nhận thức",
                        "Không dừng lại ở kiến thức bề nổi, cuốn sách giúp người đọc xây dựng hệ quy chiếu bền vững để áp dụng lâu dài trong học tập, công việc và cuộc sống."
                )
        );

        List<String> takeaways = List.of(
                "Nắm bắt trọn vẹn hệ thống tư duy cốt lõi thuộc chủ đề " + category + " một cách mạch lạc, dễ hiểu.",
                "Rút ngắn thời gian tự mày mò nhờ những đúc kết thực tiễn sắc bén từ " + author + ".",
                "Phù hợp để đọc nghiền ngẫm nhiều lần và ứng dụng trực tiếp vào thực tế."
        );

        return new GeneratedSummary(
                "Bạn đã từng tự hỏi điều gì khiến \"" + title + "\" trở thành cuốn sách được nhiều độc giả tìm đọc đến vậy chưa?",
                "\"" + title + "\" của " + author + " là ấn phẩm nổi bật trong dòng sách " + category + ". " + trimToLength(desc, 220),
                sections,
                takeaways,
                "Phù hợp với sinh viên, người đi làm và độc giả yêu thích chủ đề " + category + ".",
                author + " là tác giả được đánh giá cao nhờ lối truyền đạt súc tích, giàu chiều sâu và truyền cảm hứng mạnh mẽ cho người đọc.",
                240,
                "Editorial-AI-Engine"
        );
    }

    private String getModelName() {
        return (openAiModel != null && !openAiModel.isBlank()) ? openAiModel.trim() : "gpt-4o-mini";
    }

    private String stripMarkdownFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    private String trimToLength(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        int cut = text.lastIndexOf(' ', maxLen);
        return (cut > 50 ? text.substring(0, cut) : text.substring(0, maxLen)) + "...";
    }

    private static class GeneratedSummary {
        final String hookQuestion;
        final String shortSummary;
        final List<AiSummaryDto.SectionItem> sections;
        final List<String> takeaways;
        final String targetAudience;
        final String authorBio;
        final Integer readingTimeMinutes;
        final String modelUsed;

        GeneratedSummary(String hookQuestion, String shortSummary, List<AiSummaryDto.SectionItem> sections,
                         List<String> takeaways, String targetAudience, String authorBio,
                         Integer readingTimeMinutes, String modelUsed) {
            this.hookQuestion = hookQuestion;
            this.shortSummary = shortSummary;
            this.sections = sections;
            this.takeaways = takeaways;
            this.targetAudience = targetAudience;
            this.authorBio = authorBio;
            this.readingTimeMinutes = readingTimeMinutes;
            this.modelUsed = modelUsed;
        }
    }
}
