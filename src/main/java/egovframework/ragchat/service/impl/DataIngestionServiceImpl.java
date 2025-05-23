package egovframework.ragchat.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import egovframework.ragchat.config.RagConfig;
import egovframework.ragchat.service.DataIngestionService;
import io.qdrant.client.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("DataIngestionService")
@RequiredArgsConstructor
public class DataIngestionServiceImpl extends EgovAbstractServiceImpl implements DataIngestionService {
	
	private final QdrantClient qdrantClient;
	private final EmbeddingModel embeddingModel;
	private final EmbeddingStore<TextSegment> embeddingStore;
	private final RagConfig ragConfig;
	
	@Override
	public int setupRagChatbot() {
		// 컬렉션 생성
        ragConfig.createCollection(qdrantClient);

        // 문서 로드 및 임베딩
        return insertDocuments();
	}
	
	/**
     * 문서를 로드하고 임베딩하여 저장.
     * 
     * @return 처리된 문서 수
     */
	private int insertDocuments() {
		// 문서를 Chunk로 분할
		// 각 Chunk는 최대 문자수 1000, Chunk 간 겹치는 문자 수는 150으로 설정 (문맥을 잃지 않게 하기 위함)
		DocumentSplitter documentSplitter = DocumentSplitters.recursive(1000, 150);
		
		List<Document> documents = loadMarkdownDocuments();
		
		int processedCount = 0;

        log.info("총 {}개의 문서를 처리합니다.", documents.size());
        
        for (Document doc : documents) {
            try {
                List<TextSegment> segments = documentSplitter.split(doc);
                Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(segments);
                List<Embedding> embeddings = embeddingResponse.content();
                embeddingStore.addAll(embeddings, segments);
                processedCount++;
                String source = doc.metadata().getString("source");
                log.info("문서 처리 완료: {} (세그먼트 {}개)", (source != null ? source : "unknown"), segments.size());
            } catch (Exception e) {
                String errorSource = doc.metadata().getString("source");
                log.error("문서 처리 중 오류 발생: {}", (errorSource != null ? errorSource : "unknown"), e);
            }
        }

        log.info("총 {}개 문서 중 {}개 처리 완료", documents.size(), processedCount);
        
        return processedCount;
	}
	
	/**
     * 리소스 디렉토리에서 모든 마크다운 파일을 로드.
     * 
     * @return 로드된 문서 목록
     */
	private List<Document> loadMarkdownDocuments() {
		List<Document> documents = new ArrayList<>();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		
		try {
			Resource[] resources = resolver.getResources("classpath:data/*.md");
            log.info("{}개의 마크다운 파일을 찾았습니다.", resources.length);

            for (Resource resource : resources) {
                try {
                    String content = readResourceContent(resource);
                    String filename = resource.getFilename();

                    // Metadata 객체 생성
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("source", filename);
                    metadataMap.put("type", "markdown");
                    Metadata metadata = Metadata.from(metadataMap);

                    Document doc = Document.from(content, metadata);

                    documents.add(doc);
                    log.info("문서 로드 완료: {}, 크기: {}바이트", filename, content.length());
                } catch (IOException e) {
                    log.error("파일 읽기 오류: {}", resource.getFilename(), e);
                }
            }
		} catch (IOException e) {
            log.error("리소스 검색 중 오류 발생", e);
        }

        return documents;
	}
	
	/**
     * 리소스 파일의 내용을 문자열로 읽기.
     * 
     * @param resource 읽을 리소스
     * @return 파일 내용
     * @throws IOException 파일 읽기 오류 발생 시
     */
	private String readResourceContent(Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
	
}
