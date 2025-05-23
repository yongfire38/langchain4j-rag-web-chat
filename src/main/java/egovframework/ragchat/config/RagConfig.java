package egovframework.ragchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RagConfig {

	@Value("${rag.collection.name}")
	private String collectionName;

	@Value("${rag.qdrant.host}")
	private String qdrantHost;

	@Value("${rag.qdrant.port}")
	private Integer qdrantPort;

	@Value("${rag.qdrant.use-tls}")
	private Boolean useTls;

	@Value("${rag.embedding.size}")
	private Integer embeddingSize;

	/**
	 * Qdrant 클라이언트 빈 생성
	 */
	@Bean
	public QdrantClient qdrantClient() {
		log.info("Qdrant 클라이언트 초기화 - 호스트: {}, 포트: {}, TLS 사용: {}", qdrantHost, qdrantPort, useTls);
		return new QdrantClient(QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls).build());
	}

	/**
	 * 임베딩 모델 빈 생성
	 */
	@Bean
	public EmbeddingModel embeddingModel() {
		log.info("BgeSmallEnV15QuantizedEmbeddingModel 초기화");
		return new BgeSmallEnV15QuantizedEmbeddingModel();
	}

	/**
	 * 임베딩 저장소 빈 생성
	 */
	@Bean
	public EmbeddingStore<TextSegment> embeddingStore() {
		log.info("Qdrant 임베딩 저장소 초기화 - 컬렉션: {}", collectionName);
		return QdrantEmbeddingStore.builder().collectionName(collectionName).host(qdrantHost).port(qdrantPort)
				.useTls(useTls).build();
	}

	/**
	 * 컨텐츠 검색기 빈 생성
	 */
	@Bean
	public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel) {
		log.info("컨텐츠 검색기 초기화");
		return EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore).embeddingModel(embeddingModel)
				.maxResults(3).minScore(0.6).build();
	}

	/**
	 * 컬렉션 생성 처리
	 */
	public void createCollection(QdrantClient client) {
		log.info("Qdrant 컬렉션 생성 시도: {}", collectionName);
		try {
			client.createCollectionAsync(collectionName,
					VectorParams.newBuilder().setDistance(Distance.Dot).setSize(embeddingSize).build()).get();
			log.info("컬렉션 생성 완료: {}", collectionName);
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().contains("already exists")) {
				log.info("컬렉션이 이미 존재합니다: {}", collectionName);
			} else {
				log.error("컬렉션 생성 중 오류 발생", e);
				throw new RuntimeException("컬렉션 생성 실패: " + e.getMessage(), e);
			}
		}
	}

}
