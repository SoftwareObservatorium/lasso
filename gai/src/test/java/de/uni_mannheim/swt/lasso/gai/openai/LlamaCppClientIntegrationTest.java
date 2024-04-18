/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.gai.openai;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 *
 *
 * @author Marcus Kessel
 */
public class LlamaCppClientIntegrationTest {

    // 1. http://bagdana.informatik.uni-mannheim.de:8080/ or http://dybbuk.informatik.uni-mannheim.de:8080/
    OpenAiClient client = new OpenAiClient("http://bagdana.informatik.uni-mannheim.de:8080/v1/chat/completions",
            "swt4321");

    //@Disabled
    @Test
    public void test_DeepSeekCoder33BInstruct_complete() {
        Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("// this java method encodes text to a byte array using Base64 with padding\npublic static byte[] encodeBase64(String str) {");
        //request.setN(1);
        request.setTemperature(0.7);
        request.setTop_p(0.95);
        request.setTop_k(40);
        request.setRepeat_penalty(1.18);
        request.setMax_tokens(2048);
        request.setStream(false); // return complete response
        request.setEcho(true); // include prompt

        request.setMessages(Collections.singletonList(message));

        // not really necessary
        request.setModel("deepseek-coder-33b-instruct.Q5_K_M.gguf");

        CompletionResponse response = client.complete(request);

        System.out.println(ToStringBuilder.reflectionToString(response));

        //Choice choice = response.getChoices().get(0);
        System.out.println("---- Generated code ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }

    @Test
    public void test_DeepSeekCoder33BInstruct_generate() {
        Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("generate a java class that contains a method that encodes a string to base64");
        //request.setN(1);
        request.setTemperature(0.7);
        request.setTop_p(0.95);
        request.setTop_k(40);
        request.setRepeat_penalty(1.18);
        request.setMax_tokens(2048);
        request.setStream(false); // return complete response
        request.setEcho(true); // include prompt

        request.setMessages(Collections.singletonList(message));

        // not really necessary
        request.setModel("deepseek-coder-33b-instruct.Q5_K_M.gguf");

        CompletionResponse response = client.complete(request);

        System.out.println(ToStringBuilder.reflectionToString(response));

        //Choice choice = response.getChoices().get(0);
        System.out.println("---- Generated code ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }

    @Test
    public void test_DeepSeekCoder33BInstruct_tests() {
        Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("write 10 tests to verify the functionality of determining the length of a string (method signature is \"strlen(String):long\"). Use a tabular representation with two columns. the first column contains the test input, whereas the second column the expected output.");
        //request.setN(1);
        request.setTemperature(0.7);
        request.setTop_p(0.95);
        request.setTop_k(40);
        request.setRepeat_penalty(1.18);
        request.setMax_tokens(2048);
        request.setStream(false); // return complete response
        request.setEcho(true); // include prompt

        request.setMessages(Collections.singletonList(message));

        // not really necessary
        request.setModel("deepseek-coder-33b-instruct.Q5_K_M.gguf");

        CompletionResponse response = client.complete(request);

        System.out.println(ToStringBuilder.reflectionToString(response));

        //Choice choice = response.getChoices().get(0);
        System.out.println("---- Generated tests ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }
}
