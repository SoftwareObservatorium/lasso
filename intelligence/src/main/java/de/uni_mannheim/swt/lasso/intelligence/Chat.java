package de.uni_mannheim.swt.lasso.intelligence;

import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.Map;

public class Chat {

    public static void main(String[] args) {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName("gemma2:latest")
                .temperature(0.7)
                .build();

        System.out.println(ollamaChatModel.generate("Tell me a joke"));
    }
}
