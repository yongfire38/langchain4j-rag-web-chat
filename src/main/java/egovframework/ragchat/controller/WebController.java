package egovframework.ragchat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 웹 페이지를 제공하는 컨트롤러
 */
@Controller
public class WebController {

	/**
     * 메인 페이지.
     * 
     * @return 메인 페이지 템플릿 이름
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
