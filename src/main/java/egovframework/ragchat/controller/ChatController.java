package egovframework.ragchat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.ragchat.dto.ChatRequest;
import egovframework.ragchat.dto.ChatResponse;
import egovframework.ragchat.service.ChatService;
import egovframework.ragchat.service.DataIngestionService;
import egovframework.ragchat.util.MarkdownConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
	
	private final ChatService chatService;
    private final DataIngestionService dataIngestionService;
    private final MarkdownConverter markdownConverter;
    
    /**
     * 사용자 질의에 대한 RAG 기반 응답을 제공하는 엔드포인트.
     * 
     * @param chatRequest 사용자 질의가 포함된 요청 객체
     * @return AI 모델의 응답
     */
    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest chatRequest) {
    	String query = chatRequest.getQuery();
        log.info("사용자 질의 수신: {}", query);

        if (query == null || query.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "질의가 비어있습니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String response = chatService.generateRagResponse(chatRequest);

        // 마크다운을 HTML로 변환
        String htmlResponse = markdownConverter.convertToHtml(response);

        ChatResponse chatResponse = new ChatResponse(htmlResponse);
        return ResponseEntity.ok(chatResponse);
    }
    
    /**
     * RAG 없이 일반 채팅 응답을 제공하는 엔드포인트
     * 
     * @param chatRequest 사용자 질의가 포함된 요청 객체
     * @return AI 모델의 응답
     */
    @PostMapping("/simple")
    public ResponseEntity<?> simpleChat(@RequestBody ChatRequest chatRequest) {
    	String query = chatRequest.getQuery();
        log.info("일반 채팅 질의 수신: {}", query);
        
        if (query == null || query.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "질의가 비어있습니다");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        String response = chatService.generateSimpleResponse(chatRequest);
        
        // 마크다운을 HTML로 변환
        String htmlResponse = markdownConverter.convertToHtml(response);

        ChatResponse chatResponse = new ChatResponse(htmlResponse);
        return ResponseEntity.ok(chatResponse);
    }
    
    /**
     * 데이터 임베딩 커렉션을 생성하고 문서를 임베딩하는 엔드포인트
     * 
     * @return 처리 결과
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setupCollection() {
    	log.info("컬렉션 생성 및 문서 임베딩 시작");
    	
    	try {
            int processedCount = dataIngestionService.setupRagChatbot();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "컬렉션 생성 및 문서 임베딩 완료");
            result.put("processedDocuments", processedCount);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("컬렉션 생성 중 오류 발생", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "컬렉션 생성 중 오류 발생: " + e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 서버 상태를 확인하는 엔드포인트
     * 
     * @return 서버 상태 정보
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "online");
        status.put("service", "RAG Chat Service");

        return ResponseEntity.ok(status);
    }

}
