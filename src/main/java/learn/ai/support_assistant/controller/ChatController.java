package learn.ai.support_assistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient ollamaChatClient;
    private final ChatClient claudeChatClient;
    private final VectorStore vectorStore;

    public ChatController(
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            VectorStore vectorStore) {
        this.ollamaChatClient = ollamaChatClient;
        this.claudeChatClient = claudeChatClient;
        this.vectorStore = vectorStore;
    }


    /*@PostMapping
    public String chat(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }*/
    /*@PostMapping
    public String chat(@RequestBody String message) {
        var response = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        System.out.println("Usage: " + response.getMetadata().getUsage());
        return response.getResult().getOutput().getText();
    }*/

    @PostMapping
    public Flux<String> chat(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "ollama") String provider,
            @RequestBody String message) {

        ChatClient client = provider.equalsIgnoreCase("claude") ? claudeChatClient : ollamaChatClient;


        if (message == null || message.isBlank()) {
            return Flux.just("Please enter a question.");
        }
        if (message.length() > 2000) {
            return Flux.just("Your message is too long, please shorten it.");
        }

        return client.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                //.call()
                .content();
    }

   /* @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String conversationId,
            @RequestBody String message) {

        if (message == null || message.isBlank()) {
            return Flux.just("Please enter a question.");
        }
        if (message.length() > 2000) {
            return Flux.just("Your message is too long, please shorten it.");
        }

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }*/

    @GetMapping("/debug/search")
    public List<Document> debugSearch(@RequestParam String query) {
        return vectorStore.similaritySearch(query);
    }
}
