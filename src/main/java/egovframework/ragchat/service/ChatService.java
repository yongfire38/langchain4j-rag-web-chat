package egovframework.ragchat.service;

import egovframework.ragchat.dto.ChatRequest;

public interface ChatService {

	/**
     * RAG를 사용하여 사용자 질의에 대한 응답을 생성.
     * 
     * @param chatRequest 사용자 질의 요청
     * @return AI 모델의 응답
     */
	public String generateRagResponse(ChatRequest chatRequest);
	
	/**
     * RAG 없이 일반 채팅 응답을 생성.
     * 
     * @param chatRequest 사용자 질의 요청
     * @return AI 모델의 응답
     */
	public String generateSimpleResponse(ChatRequest chatRequest);
}
