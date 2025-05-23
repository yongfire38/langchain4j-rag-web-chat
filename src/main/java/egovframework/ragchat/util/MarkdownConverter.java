package egovframework.ragchat.util;

import org.springframework.stereotype.Component;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Component
public class MarkdownConverter {
	
	private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownConverter() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }
    
    /**
     * 마크다운 텍스트를 HTML로 변환.
     * 
     * @param markdown 마크다운 형식의 텍스트
     * @return HTML 형식으로 변환된 텍스트
     */
    public String convertToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

}
