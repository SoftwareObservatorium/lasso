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
public class Gpt4AllClientIntegrationTest {

    // 1. enable REST API in gpt4all GUI https://docs.gpt4all.io/gpt4all_chat.html#server-mode
    // 2. mitmproxy --set block_global=false --listen-host 134.155.89.225 --listen-port 5891 --mode reverse:http://localhost:4891

    @Disabled
    @Test
    public void test_starcoderbase_7b() {
        OpenAiClient client = new OpenAiClient("http://lassohp10.informatik.uni-mannheim.de:5891/v1/chat/completions",
                "not_needed_for_this_model");

        Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("// this java method encodes text to a byte array using Base64 with padding\npublic static byte[] encodeBase64(String str) {");
        request.setN(1);
        request.setTemperature(0.2);
        request.setTop_p(1.0);
        request.setTop_k(40);
        request.setRepeat_penalty(1.18);
        request.setMax_tokens(4096);
        request.setStream(false); // return complete response
        request.setEcho(true); // include prompt

        request.setMessages(Collections.singletonList(message));

        // must contain file suffix
        request.setModel("starcoderbase-7b-ggml.bin");

        CompletionResponse response = client.complete(request);

        System.out.println(ToStringBuilder.reflectionToString(response));

        //Choice choice = response.getChoices().get(0);
        System.out.println("---- Generated code ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }

    @Disabled
    @Test
    public void test_wizard_1_1() {
        OpenAiClient client = new OpenAiClient("http://lassohp10.informatik.uni-mannheim.de:5891/v1/chat/completions",
                "not_needed_for_this_model");

        Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("write a java method that encodes a string to base64 without padding and returns a byte array");
        request.setN(1);
        request.setTemperature(0.7);
        request.setTop_p(1.0);
        request.setTop_k(40);
        request.setRepeat_penalty(1.18);
        request.setMax_tokens(1000);
        request.setStream(false); // return complete response
        request.setEcho(true); // include prompt

        request.setMessages(Collections.singletonList(message));

        // must contain file suffix
        request.setModel("wizardlm-13b-v1.1-superhot-8k.ggmlv3.q4_0.bin");

        CompletionResponse response = client.complete(request);

        System.out.println(new Gson().toJson(response));

        System.out.println("---- Generated code ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }
}
