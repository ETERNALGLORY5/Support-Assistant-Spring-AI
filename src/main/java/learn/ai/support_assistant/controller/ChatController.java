package learn.ai.support_assistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
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
    public String chat(
            @RequestParam String conversationId,
            @RequestBody String message) {

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
