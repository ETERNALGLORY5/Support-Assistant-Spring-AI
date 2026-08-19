package learn.ai.support_assistant.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class RagDebugController {

    private final VectorStore vectorStore;

    public RagDebugController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/search")
    public List<Object> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        return results.stream()
                .map(doc -> new java.util.LinkedHashMap<String, Object>() {{
                    put("score", doc.getMetadata().get("distance"));
                    put("filename", doc.getMetadata().get("filename"));
                    put("content", doc.getText());
                }})
                .collect(Collectors.toList());
    }
}
