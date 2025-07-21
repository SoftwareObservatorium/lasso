package de.uni_mannheim.swt.lasso.engine.action.gai;

import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.gai.openai.Prompt;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
//import dev.langchain4j.model.openai.OpenAiChatRequestParameters;

/**
 * Generate Tests with OpenAI
 *
 * Based on langchain4j.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Generate Tests with OpenAI")
@Stable
@Local(numberOfParallelStimulusMatrices = 1) // limit to one stimulus matrix at a time
public class GenerateTestsOpenAI extends GenerateTestsOllama {

    private static final Logger LOG = LoggerFactory
            .getLogger(GenerateTestsOpenAI.class);

    @LassoInput(desc = "OpenAI API key", optional = true)
    public String apiKey = "demo"; // see https://docs.langchain4j.dev/integrations/language-models/open-ai/

    @LassoInput(desc = "OpenAI Base URL", optional = true)
    public String baseUrl = "https://api.openai.com/v1";

    // XXX for "thinking" models, we may run into timeouts
    @LassoInput(desc = "Timeout (in seconds)", optional = true)
    public long timeout = 60L;

    @Override
    protected String generate(Prompt prompt, String endpoint) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
//                .defaultRequestParameters(ChatRequestParameters.builder()
//                        .modelName("gpt-4o-mini")
//                        .temperature(0.7)
//                        .build())
                .modelName(prompt.getModel())
                .temperature(prompt.getTemperature())
                .timeout(Duration.ofSeconds(timeout))

                .build();

        LOG.info("Prompting '{}', '{}'", endpoint, prompt.getModel());

        String chatResponse = chatModel.generate(prompt.getPromptContent());

        return chatResponse;
    }
}
