package de.uni_mannheim.swt.lasso.sheets.service.cut;

import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.sheets.service.dto.SheetSpec;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class SheetGeneration {

    private static final Logger LOG = LoggerFactory
            .getLogger(SheetGeneration.class);

    private final String ollamaBaseUrl;

    public SheetGeneration(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public SheetSpec promptOllama(String model, String prompt) {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(model)
//                .temperature(0.8)
//                .numCtx(2048)
//                .numPredict(128)
                .build();

        String chatResponse = ollamaChatModel.generate(prompt);

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = contentParser.extractCode(chatResponse, "java");

        String jsonl = StringUtils.trim(generatedCode.get(0));

        if(StringUtils.indexOf(jsonl, '{') > 0) {
            jsonl = "{" + StringUtils.substringAfter(jsonl, "{");
        }

        LOG.debug("sheet body generated\n{}", jsonl);

        // FIXME validate syntax with parser
        SheetSpec sheetSpec = new SheetSpec();
        sheetSpec.setSignature("testLlm" + System.currentTimeMillis() + "()");
        sheetSpec.setInvocations(Arrays.asList(""));
        sheetSpec.setBody(jsonl);

        return sheetSpec;
    }
}
