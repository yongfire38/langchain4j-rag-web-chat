package egovframework.ragchat.service.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import egovframework.ragchat.dto.ChatRequest;
import egovframework.ragchat.service.ChatService;
import egovframework.ragchat.util.MarkdownConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("ChatService")
@RequiredArgsConstructor
public class ChatServiceImpl extends EgovAbstractServiceImpl implements ChatService {

	private final ChatLanguageModel chatLanguageModel;
	private final StreamingChatLanguageModel streamingChatLanguageModel;
	private final ContentRetriever contentRetriever;
	private final MarkdownConverter markdownConverter;

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

	/**
	 * 스트리밍 RAG 채팅봇 클래스
	 */
	private class StreamingRagChatbot {
		private final StreamingChatLanguageModel model;
		private final ContentRetriever retriever;
		private final SseEmitter emitter;
		private final MarkdownConverter converter;

		public StreamingRagChatbot(StreamingChatLanguageModel model, ContentRetriever retriever, SseEmitter emitter,
				MarkdownConverter converter) {
			this.model = model;
			this.retriever = retriever;
			this.emitter = emitter;
			this.converter = converter;
		}

		public void chat(String query) {
			try {
				// 사용자 메시지 생성;
				Query userQuery = Query.from(query);

				// 관련 문서 검색
				var relevantDocuments = retriever.retrieve(userQuery);
				StringBuilder contextBuilder = new StringBuilder();

				// 검색된 문서 내용을 컨텍스트로 구성
				relevantDocuments.forEach(document -> {
					contextBuilder.append(document.textSegment().toString()).append("\n\n");
				});

				// 프롬프트 구성
				String systemPrompt = """
						    당신은 지식 기반 질의응답 시스템입니다.
						    사용자의 질문에 대해 제공된 문서 내용을 기반으로 정확하고 도움이 되는 답변을 제공하세요.
						    제공된 문서에 관련 정보가 없는 경우, 솔직하게 모른다고 답변하세요.
						    답변은 한국어로 제공하세요.
						""";

				String fullPrompt = """
										    시스템: %s

										    컨텍스트:
						%s

										    사용자: %s
										""".formatted(systemPrompt, contextBuilder.toString(), query);

				// 스트리밍 응답 처리
				CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
				StringBuilder responseBuilder = new StringBuilder();

				// 토큰 버퍼링을 위한 StringBuilder
				StringBuilder tokenBuffer = new StringBuilder();

				// 스트리밍 처리 콜백
				model.generate(fullPrompt, new StreamingResponseHandler<AiMessage>() {
					@Override
					public void onNext(String token) {
						try {
							tokenBuffer.append(token);
							responseBuilder.append(token);
							String bufferContent = tokenBuffer.toString();

							// 토큰이 문장 끝, 줄바꾸기, 공백으로 끝나는 경우에만 전송
							if (token.endsWith(".") || token.endsWith("\n") || token.endsWith(" ") ||
									token.endsWith("!") || token.endsWith("?") || token.endsWith("\t") ||
									token.endsWith("\r") || token.endsWith(";") || token.endsWith(":") ||
									token.endsWith(",") || token.endsWith(")") || token.endsWith("]") ||
									token.endsWith("}")) {

								// 마크다운을 HTML로 변환
								String htmlContent = converter.convertToHtml(bufferContent);

								// 디버그용 로그 추가
								log.debug("스트리밍 토큰 전송: {}", htmlContent);

								// SSE 이벤트 생성 - 이벤트 이름을 지정하지 않고 데이터만 전송
								SseEmitter.SseEventBuilder event = SseEmitter.event()
										.data(htmlContent)
										.id(String.valueOf(System.currentTimeMillis()));

								// 이벤트 전송
								emitter.send(event);
								tokenBuffer.setLength(0);
							}
						} catch (IOException e) {
							log.error("스트리밍 응답 전송 중 오류", e);
						}
					}

					@Override
					public void onComplete(Response<AiMessage> response) {
						// 버퍼에 남아있는 내용이 있다면 마지막으로 전송
						try {
							if (tokenBuffer.length() > 0) {
								String remainingContent = tokenBuffer.toString();
								String htmlContent = converter.convertToHtml(remainingContent);

								SseEmitter.SseEventBuilder event = SseEmitter.event()
										.data(htmlContent)
										.name("message")
										.id(String.valueOf(System.currentTimeMillis()));

								emitter.send(event);
								log.debug("버퍼에 남은 내용 전송: {}", remainingContent);
							}
						} catch (IOException e) {
							log.error("마지막 버퍼 내용 전송 중 오류", e);
						}

						futureResponse.complete(response);
						log.debug("AI 응답 완료: {}", responseBuilder.toString());
						emitter.complete();
					}

					@Override
					public void onError(Throwable error) {
						futureResponse.completeExceptionally(error);
						log.error("AI 응답 생성 중 오류 발생", error);
						try {
							String errorMessage = handleException(new Exception(error));
							String htmlError = converter.convertToHtml(errorMessage);
							emitter.send(SseEmitter.event().data(htmlError));
							emitter.complete();
						} catch (IOException e) {
							emitter.completeWithError(e);
						}
					}
				});
			} catch (Exception e) {
				log.error("스트리밍 RAG 응답 생성 중 오류 발생", e);
				try {
					String errorMessage = handleException(e);
					String htmlError = converter.convertToHtml(errorMessage);
					emitter.send(SseEmitter.event().data(htmlError));
					emitter.complete();
				} catch (IOException ioe) {
					emitter.completeWithError(ioe);
				}
			}
		}
	}

	@Override
	public SseEmitter generateStreamingRagResponse(ChatRequest chatRequest) {
		String query = chatRequest.getQuery();
		log.info("스트리밍 RAG 질의 수신: {}", query);

		// SSE 이미터 생성 (타임아웃 설정: 2분)
		SseEmitter emitter = new SseEmitter(120000L);

		// UTF-8 인코딩 관련 핸들러 추가
		emitter.onCompletion(() -> log.debug("SSE 완료"));
		emitter.onTimeout(() -> log.debug("SSE 타임아웃"));
		emitter.onError((ex) -> log.error("SSE 오류", ex));

		try {
			// 스트리밍 RAG 채팅봇 인터페이스 생성
			StreamingRagChatbot streamingRagChatbot = new StreamingRagChatbot(streamingChatLanguageModel,
					contentRetriever, emitter, markdownConverter);

			// 질의 처리 및 응답 생성 (비동기적으로 스트리밍 처리)
			streamingRagChatbot.chat(query);

		} catch (Exception e) {
			log.error("스트리밍 RAG 응답 생성 중 오류 발생", e);
			try {
				String errorMessage = handleException(e);
				String htmlError = markdownConverter.convertToHtml(errorMessage);
				emitter.send(SseEmitter.event().data(htmlError));
				emitter.complete();
			} catch (IOException ioe) {
				emitter.completeWithError(ioe);
			}
		}

		return emitter;
	}

	@Override
	public SseEmitter generateStreamingSimpleResponse(ChatRequest chatRequest) {
		String query = chatRequest.getQuery();
		log.info("스트리밍 일반 채팅 질의 수신: {}", query);

		// SSE 이미터 생성 (타임아웃 설정: 2분)
		SseEmitter emitter = new SseEmitter(120000L);

		// UTF-8 인코딩 관련 핸들러 추가
		emitter.onCompletion(() -> log.debug("SSE 완료"));
		emitter.onTimeout(() -> log.debug("SSE 타임아웃"));
		emitter.onError((ex) -> log.error("SSE 오류", ex));

		try {
			// 사용자 메시지 생성
			UserMessage userMessage = UserMessage.from(query);

			// 스트리밍 응답 처리
			CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
			StringBuilder responseBuilder = new StringBuilder();

			// 한글 처리를 위한 버퍼 관리
			StringBuilder tokenBuffer = new StringBuilder();

			streamingChatLanguageModel.generate(userMessage, new StreamingResponseHandler<AiMessage>() {
				@Override
				public void onNext(String token) {
					try {
						// 토큰 누적
						responseBuilder.append(token);
						tokenBuffer.append(token);

						// 완성된 문장이나 단어만 전송 (한글 깨짐 방지)
						String bufferContent = tokenBuffer.toString();

						// 문장 끝나는 경우에만 전송 (강제 줄바꿈 방지)
						// 문장 종결 부호나 단락 구분자가 있을 때만 전송
						if (token.endsWith(".") || token.endsWith("\n") ||
								token.endsWith("\r") || token.endsWith("\t") || token.endsWith("，") || // 콤마
								token.endsWith("。") || token.endsWith("、") || // 중국어 문장 부호
								token.endsWith("…") || token.endsWith("‥") || // 점 부호
								token.endsWith("？") || token.endsWith("！") || // 물음표, 느낌표
								token.endsWith(".") || token.endsWith("!") || token.endsWith("?") || // 영문 종결 부호
								bufferContent.length() >= 30) { // 버퍼 길이 기준 증가 (더 긴 문장 단위로 전송)

							// HTML로 변환하여 전송
							String htmlContent = markdownConverter.convertToHtml(bufferContent);

							// 이벤트 이름 추가
							SseEmitter.SseEventBuilder event = SseEmitter.event()
									.data(htmlContent)
									.name("message")
									.id(String.valueOf(System.currentTimeMillis()));

							// 이벤트 전송
							emitter.send(event);
							tokenBuffer.setLength(0); // 버퍼 초기화
						}
					} catch (IOException e) {
						log.error("스트리밍 전송 중 오류 발생", e);
						emitter.completeWithError(e);
					}
				}

				@Override
				public void onComplete(Response<AiMessage> response) {
					// 버퍼에 남아있는 내용이 있다면 마지막으로 전송
					try {
						if (tokenBuffer.length() > 0) {
							String remainingContent = tokenBuffer.toString();

							// UTF-8 문자열 유효성 검사 및 인코딩 보장
							String validUtf8Content;
							try {
								byte[] bytes = remainingContent.getBytes("UTF-8");
								validUtf8Content = new String(bytes, "UTF-8");
							} catch (Exception e) {
								log.warn("마지막 버퍼 내용 UTF-8 변환 중 오류, 원본 내용 사용", e);
								validUtf8Content = remainingContent;
							}

							String htmlContent = markdownConverter.convertToHtml(validUtf8Content);

							SseEmitter.SseEventBuilder event = SseEmitter.event()
									.data(htmlContent)
									.name("message")
									.id(String.valueOf(System.currentTimeMillis()));

							emitter.send(event);
							log.debug("버퍼에 남은 내용 전송: {}", validUtf8Content);
						}
					} catch (IOException e) {
						log.error("마지막 버퍼 내용 전송 중 오류", e);
					}

					futureResponse.complete(response);
					log.debug("AI 응답 완료: {}", responseBuilder.toString());
					emitter.complete();
				}

				@Override
				public void onError(Throwable error) {
					futureResponse.completeExceptionally(error);
					log.error("AI 응답 생성 중 오류 발생", error);
					try {
						String errorMessage = handleException(new Exception(error));
						String htmlError = markdownConverter.convertToHtml(errorMessage);
						emitter.send(SseEmitter.event().data(htmlError));
						emitter.complete();
					} catch (IOException e) {
						emitter.completeWithError(e);
					}
				}
			});

		} catch (Exception e) {
			log.error("스트리밍 일반 응답 생성 중 오류 발생", e);
			try {
				String errorMessage = handleException(e);
				String htmlError = markdownConverter.convertToHtml(errorMessage);
				emitter.send(SseEmitter.event().data(htmlError));
				emitter.complete();
			} catch (IOException ioe) {
				emitter.completeWithError(ioe);
			}
		}

		return emitter;
	}

}
