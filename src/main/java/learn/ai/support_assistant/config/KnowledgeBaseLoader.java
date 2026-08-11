package learn.ai.support_assistant.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeBaseLoader implements CommandLineRunner {

    private final VectorStore vectorStore;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        // Avoid re-loading every restart — simple check via similarity search
        var existing = vectorStore.similaritySearch("password");
        if (existing != null && !existing.isEmpty()) {
            System.out.println("Knowledge base already loaded, skipping.");
            return;
        }

        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:kb/*.txt");

        for (Resource resource : resources) {
            TextReader textReader = new TextReader(resource);
            textReader.getCustomMetadata().put("filename", resource.getFilename());
            List<Document> docs = textReader.get();
            vectorStore.add(docs);
            System.out.println("Loaded into vector store: " + resource.getFilename());
        }
    }
}
