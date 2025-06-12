package egovframework.ragchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

@SpringBootApplication
public class EgovBootApplication {

	public static void main(String[] args) {
		// UTF-8을 기본 인코딩으로 설정
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("sun.jnu.encoding", "UTF-8");
		System.setProperty("sun.stdout.encoding", "UTF-8");
		System.setProperty("sun.stderr.encoding", "UTF-8");

		SpringApplication.run(EgovBootApplication.class, args);
	}

	/**
	 * UTF-8 인코딩 필터 설정
	 * 모든 요청과 응답에 대해 UTF-8 인코딩을 강제 적용
	 */
	@Bean
	public CharacterEncodingFilter characterEncodingFilter() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true); // 강제 인코딩 적용
		return filter;
	}
}
