package de.uni_mannheim.swt.lasso.sheets.service.cut;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.DynamicClassLoader;
import de.uni_mannheim.swt.lasso.gai.openai.*;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.sheets.service.dto.LQLGenerationRequest;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetSpec;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class GAITest {

    @Test
    public void ollama_cut() {
        String model = "llama3.1:latest";
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName(model)
                //.temperature(0.7)
                .build();

        String chatResponse = ollamaChatModel.generate("""
implement a java class with the following interface specification, but do not inherit a java interface: ```Base64{
    encode(byte[])->byte[]
    decode(java.lang.String)->byte[]
}```. Only output the java class and nothing else.
                """);

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = contentParser.extractCode(chatResponse, "java");

        List<ClassUnderTest> classesUnderTest = new LinkedList<>();
        for(String sourceCode : generatedCode) {
            System.out.println(sourceCode);

            try {
                ClassUnderTest classUnderTest = DynamicClassLoader.loadBySource(sourceCode, "llm");
                classesUnderTest.add(classUnderTest);

                // FIXME add to cache, so that these can be used later on in the arena
                classUnderTest.getId();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void openaiendpoint_cut() {
        OpenAiClient client = new OpenAiClient("http://localhost:3000/api/chat/completions",
                "sk-78faef900ae84ce7a28f3f1d6607fd8d");
        String model = "llama3.1:latest";
        OllamaCompletionRequest request = new OllamaCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("""
implement a java class with the following interface specification, but do not inherit a java interface: ```Base64{
    encode(byte[])->byte[]
    decode(java.lang.String)->byte[]
}```. Only output the java class and nothing else.
                """);

        //request.setTemperature(prompt.getTemperature());

        request.setMessages(Collections.singletonList(message));

        // must contain file suffix
        request.setModel(model);

        CompletionResponse response = client.complete(request);

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = new LinkedList<>();
        for(Choice choice : response.getChoices()) {
            List<String> codeMatches = contentParser.extractCode(choice.getMessage().getContent(), "java");
            generatedCode.addAll(codeMatches);
        }

        List<ClassUnderTest> classesUnderTest = new LinkedList<>();
        for(String sourceCode : generatedCode) {
            System.out.println(sourceCode);

            try {
                ClassUnderTest classUnderTest = DynamicClassLoader.loadBySource(sourceCode, "llm");
                classesUnderTest.add(classUnderTest);

                // FIXME add to cache, so that these can be used later on in the arena
                classUnderTest.getId();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void ollama_lql() {
        String ollamaBaseUrl = "http://bagdana.informatik.uni-mannheim.de:11434";
        String model = "llama3.1:latest";
        InterfaceGeneration interfaceGeneration = new InterfaceGeneration(ollamaBaseUrl);

        LQLGenerationRequest request = new LQLGenerationRequest();
        request.setModel(model);
        request.setPrompt("""
generate an interface specification for the functionality of base64 encoding. return the interface specification in the format used by the following example:
```lql
MyBoundedQueue {
    MyBoundedQueue(int)
    enQueue(java.lang.Object)->void
    deQueue()->java.lang.Object
    isEmpty()->boolean
    isFull()->boolean
}
```
                """);

        String lql = interfaceGeneration.promptOllama(request);

        System.out.println(lql);
    }

    @Test
    public void ollama_sheet() {
        String ollamaBaseUrl = "http://bagdana.informatik.uni-mannheim.de:11434";
        String model = "llama3.1:latest";

        String prompt = """
generate a unit test for the functionality described by the following interface specification:
```Base64{     encode(byte[])->byte[]     decode(java.lang.String)->byte[] }```. return the test in the jsonl format used by the following example:
```
{"cells": {"A1": {}, "B1": "create", "C1": "Base64"}}
{"cells": {"A2": {}, "B2": "encode", "C2": "A1", "D2": "\\"Hello World!\\".getBytes()"}}
```
The example represents a unit test as a spreadsheet. Each line is represents a method call. the first column of the method invocation is the output column, the second column the method name column, the third column the class that offers the method, and finally the remaining columns are the input parameter values to the operation. Delimit each line by a new line operator. Do not generate comments.
""";

        SheetGeneration sheetGeneration = new SheetGeneration(ollamaBaseUrl);
        SheetSpec sheetSpec = sheetGeneration.promptOllama(model, prompt);

        System.out.println(sheetSpec.getBody());
    }
}
