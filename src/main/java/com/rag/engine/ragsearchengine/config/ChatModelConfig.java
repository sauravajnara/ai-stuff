package com.rag.engine.ragsearchengine.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Groq is OpenAI-compatible
        return OpenAiChatModel.builder()
                .httpClientBuilder(new SpringRestClientBuilder())
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName("llama-3.3-70b-versatile")
                .build();
    }
}
