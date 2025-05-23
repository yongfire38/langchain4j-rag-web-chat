package egovframework.ragchat.service.impl;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import egovframework.ragchat.dto.ChatRequest;
import egovframework.ragchat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("ChatService")
@RequiredArgsConstructor
public class ChatServiceImpl extends EgovAbstractServiceImpl implements ChatService {

	private final ChatLanguageModel chatLanguageModel;
	private final ContentRetriever contentRetriever;

	@Override
	public String generateRagResponse(ChatRequest chatRequest) {
		String query = chatRequest.getQuery();
		log.info("사용자 질의 수신: {}", query);

		try {
			// RAG 챗봇 인터페이스 생성
			RagChatbot ragChatbot = AiServices.builder(RagChatbot.class).chatLanguageModel(chatLanguageModel)
					.contentRetriever(contentRetriever).build();

			// 질의 처리 및 응답 생성
			String response = ragChatbot.chat(query);
			log.debug("AI 응답: {}", response);
			return response;

		} catch (Exception e) {
			log.error("AI 응답 생성 중 오류 발생", e);
			return handleException(e);
		}
	}

	@Override
	public String generateSimpleResponse(ChatRequest chatRequest) {
		String query = chatRequest.getQuery();
		log.info("일반 채팅 질의 수신: {}", query);

		try {
			// 사용자 메시지 생성
			UserMessage userMessage = UserMessage.from(query);

			// AI 모델에 질의 전송 및 응답 수신
			AiMessage aiMessage = chatLanguageModel.generate(userMessage).content();

			log.debug("AI 응답: {}", aiMessage.text());
			return aiMessage.text();
		} catch (Exception e) {
			log.error("AI 응답 생성 중 오류 발생", e);
			return handleException(e);
		}
	}

	/**
	 * RAG 기반 챗봇 인터페이스
	 */
	@SystemMessage("""
			    당신은 지식 기반 질의응답 시스템입니다.
			    사용자의 질문에 대해 제공된 문서 내용을 기반으로 정확하고 도움이 되는 답변을 제공하세요.
			    제공된 문서에 관련 정보가 없는 경우, 솔직하게 모른다고 답변하세요.
			    답변은 한국어로 제공하세요.
			""")
	interface RagChatbot {
		String chat(String query);
	}

	/**
	 * 예외 처리를 위한 공통 메서드
	 * 
	 * @param e 발생한 예외
	 * @return 사용자에게 표시할 오류 메시지
	 */
	private String handleException(Exception e) {
		String errorMessage = e.getMessage();

		// 타임아웃 오류 처리
		if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("timed out")
				|| errorMessage.contains("connection") || e instanceof java.net.SocketTimeoutException
				|| e instanceof java.util.concurrent.TimeoutException)) {
			return "죄송합니다. 서버 응답 시간이 초과되었습니다.\n\n" + "지금 서버가 바쁨 상태이거나 질의가 너무 복잡한 것 같습니다.\n" + "잠시 후 다시 시도해주세요.";
		}

		// 기본 오류 메시지
		return "죄송합니다. 응답을 생성하는 중에 오류가 발생했습니다: " + errorMessage;
	}

}
