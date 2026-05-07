package com.cms.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Value("${llm.provider:openai}")
    private String provider;

    @Value("${llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${llm.openai.temperature:0.1}")
    private double temperature;

    @Value("${llm.openai.max-tokens:1024}")
    private int maxTokens;

    @Value("${llm.embedding.model:all-MiniLM-L6-v2}")
    private String embeddingModel;

    @Value("${llm.embedding.dimension:384}")
    private int embeddingDimension;

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if ("openai".equalsIgnoreCase(provider) && !openaiApiKey.isBlank()) {
            return OpenAiChatModel.builder()
                    .apiKey(openaiApiKey)
                    .modelName(openaiModel)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
        }
        return null;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        if ("openai".equalsIgnoreCase(provider) && !openaiApiKey.isBlank()) {
            return OpenAiEmbeddingModel.builder()
                    .apiKey(openaiApiKey)
                    .modelName("text-embedding-3-small")
                    .dimensions(embeddingDimension)
                    .build();
        }
        return null;
    }

    public String getEmbeddingModelName() {
        return embeddingModel;
    }
}
