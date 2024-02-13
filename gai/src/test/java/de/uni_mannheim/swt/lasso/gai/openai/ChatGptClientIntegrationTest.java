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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 *
 *
 * @author Marcus Kessel
 */
public class ChatGptClientIntegrationTest {

    @Disabled
    @Test
    public void test_encodeBase64() {
        OpenAiClient client = new OpenAiClient("https://api.openai.com/v1/chat/completions",
                "FIXME");

        OpenAiCompletionRequest request = new OpenAiCompletionRequest();
        Message message = new Message();
        message.setRole("user");
        message.setContent("write a java method that encodes a string to base64 without padding and returns a byte array");
        request.setN(1);
        request.setTemperature(0.7);
        request.setTop_p(1.0);
        request.setPresence_penalty(0);
        request.setFrequency_penalty(0);
        request.setMax_tokens(2048);
        request.setStream(false); // return complete response

        request.setMessages(Collections.singletonList(message));

        request.setModel("gpt-3.5-turbo");

        CompletionResponse response = client.complete(request);

        System.out.println(ToStringBuilder.reflectionToString(response));

        //Choice choice = response.getChoices().get(0);
        System.out.println("---- Generated code ---- \n");
        for(Choice choice : response.getChoices()) {
            System.out.println("Next choice");
            System.out.println(choice.getMessage().getContent());
        }
    }
}
