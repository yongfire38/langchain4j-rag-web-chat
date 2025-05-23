# Langchain4j와 Qdrant를 사용한 RAG(Retrieval-Augmented Generation) 샘플

## 환경 설정

| 프로그램 명 | 버전 명 |
| :--------- | :------ |
| java | 15 이상 |
| maven | 3.8.4 |
| spring boot | 2.7.18 |
| Langchain4j | 0.35.0 |

## 사용 기술

1. Java
2. Spring Boot (Maven)
3. Langchain4j
4. Qdrant DB
5. Ollama

## 사전 준비

1. [Ollama](https://ollama.com/download) 설치
2. [Huggingface](https://huggingface.co/) 에서 PC 스펙에 적합한 GGUF 모델을 준비. 완료된 모델 명은 `application.properties` 파일의 `ollama.model-name`에 설정한다.
3. `\src\main\resources\data` 경로에 디폴트 문서가 존재하나, 다른 md 파일로 대체도 가능
4. `docker-compose.yml` 을 사용해 `docker compose up -d`로 docker container 기반의 Qdrant DB 설정을 해 둔다.

## 실행

1. 애플리케이션 실행 후, 메인 화면의 `지식 데이터 설정` 버튼을 클릭하면 `/api/chat/setup` 요청이 이루어지고 컬렉션 생성 및 임베딩, 적재가 실행된다. 컬렉션 이름은 `application.properties` 파일의 `rag.collection.name`이다.
2. `http://localhost:6333/dashboard#/collections` 에서 컬렉션 확인이 가능하다.
3. 메인 화면의 `RAG 채팅 모드`, `일반 채팅 모드` 버튼으로 RAG가 적용된 질의 답변, 일반적인 질의 답변을 받을 수 있다.


