package egovframework.ragchat.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OllamaConfig {

    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${ollama.model-name}")
    private String ollamaModelName;

    @Value("${ollama.temperature}")
    private Double temperature;

    /**
     * Ollama 채팅 모델 빈 생성
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Ollama 채팅 모델 초기화 - URL: {}, 모델: {}, 온도: {}",
                ollamaBaseUrl, ollamaModelName, temperature);
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }
    
    /**
     * Ollama 스트리밍 채팅 모델 빈 생성
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("Ollama 스트리밍 채팅 모델 초기화 - URL: {}, 모델: {}, 온도: {}",
                ollamaBaseUrl, ollamaModelName, temperature);
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

}
