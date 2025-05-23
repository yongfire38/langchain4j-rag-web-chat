package egovframework.ragchat.service;

public interface DataIngestionService {
	
	/**
     * 컬렉션을 생성하고 문서를 임베딩하여 저장.
     * 
     * @return 처리된 문서 수
     */
	public int setupRagChatbot();
	
}
