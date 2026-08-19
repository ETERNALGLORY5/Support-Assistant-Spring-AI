package learn.ai.support_assistant.config;

import learn.ai.support_assistant.service.HelpdeskTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) // keeps only the last 20 messages per conversation
                .build();
    }

    /*@Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                //.defaultSystem("You are a helpful support assistant for an internal helpdesk app. Be concise and professional.")
                .defaultSystem("You only respond in one short sentence, no matter what is asked.")
                .build();
    }*/

    /*@Bean
    public ChatClient chatClient(OllamaChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a helpful support assistant for an internal helpdesk app.
                        Answer using only the provided context when it's relevant.
                        If the answer isn't in the context, say you don't have that information
                        rather than guessing.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .build();
    }*/

    @Bean
    public ChatClient chatClient(
            OllamaChatModel chatModel,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            HelpdeskTools helpdeskTools) {

        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a helpful support assistant for an internal helpdesk app.
                        Answer using the provided context when it's relevant.
                        If the answer isn't in the context, say you don't have that information
                        rather than guessing.
                        Use the available tools when the user asks about ticket status or wants to create a ticket.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .defaultTools(helpdeskTools)
                .build();
    }
}
