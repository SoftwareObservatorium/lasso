package de.uni_mannheim.swt.lasso.intelligence;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import joinery.DataFrame;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Retrieval Augmented Generation.
 *
 * Ask questions based on LASSO's code searches.
 *
 * Based on <a href="https://docs.langchain4j.dev/tutorials/rag">RAG Tutorial</a>
 *
 * @author Marcus Kessel
 *
 */
public class RagService {

    private static final Logger LOG = LoggerFactory
            .getLogger(RagService.class);

    private final ChatLanguageModel chatLanguageModel;

    public RagService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public interface Assistant {

        // FIXME system message
        @SystemMessage("You are a professional software engineer. Your task is to analyze a list of software code candidates based on their differences, each having a unique identifier (id).")
        Result<String> chat(String userMessage);
    }

    public Assistant create(List<CodeUnit> codeUnits) {
        return create(codeUnits, null);
    }

    public Assistant create(List<CodeUnit> codeUnits, DataFrame observations) {
        ContentRetriever codeUnitRetriever = createCodeUnitRetriever(fromQueryResult(codeUnits));

        ContentInjector contentInjector = DefaultContentInjector.builder()
        // .promptTemplate(...) // Formatting can also be changed
        .metadataKeysToInclude(asList("codeCandidateId", "fullyQualifiedName", "mavenCoordinates", "codeUnit", "score",
                "measureTotalBranches", "measureTotalCLOC", "measureTotalLOC", "measureTotalCyclomaticComplexity", "codeAdapterId", "isOracle"))
        .build();

        // observations
        ContentRetriever observationsRetriever = null;
        if(observations != null && !observations.isEmpty()) {
            observationsRetriever = createCodeUnitRetriever(fromDataFrameResult("output", observations));
        }

        RetrievalAugmentor retrievalAugmentor;
        if(observationsRetriever != null) {
            QueryRouter queryRouter = new DefaultQueryRouter(codeUnitRetriever, observationsRetriever);

            retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                    .queryRouter(queryRouter)
                    .contentInjector(contentInjector)
                    .build();
        } else {
            retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                    .contentRetriever(codeUnitRetriever)
                    .contentInjector(contentInjector)
                    .build();
        }

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                //.contentRetriever(contentRetriever)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        return assistant;
    }

    public Assistant create(QueryResult queryResult) {
        return create(queryResult.getImplementations());
    }

    protected List<Document> fromQueryResult(List<CodeUnit> codeUnits) {
        List<Document> documents = codeUnits.stream().map(this::fromCodeUnit).collect(Collectors.toList());

        return documents;
    }

    protected Document fromCodeUnit(CodeUnit codeUnit) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("codeCandidateId", codeUnit.getId());
        meta.put("fullyQualifiedName", codeUnit.toFQName());
        meta.put("mavenCoordinates", codeUnit.toUri());
        meta.put("codeUnit", codeUnit.getDocType());
        meta.put("score", codeUnit.getScore());

        if(MapUtils.isNotEmpty(codeUnit.getMeasures())) {
            String prefix = "m_static_";
//            codeUnit.getMetaData().keySet().stream()
//                    .filter(s -> StringUtils.startsWith(s, prefix))
//                    .forEach(s -> {
//                        meta.put(StringUtils.substringBetween(s, prefix, "_td"), codeUnit.getMetaData().get(s));
//                    });
            // XXX manual for better naming
            putMeasureSafely("m_static_branch_td", "measureTotalBranches", codeUnit.getMeasures(), meta);
            putMeasureSafely("m_static_cloc_td", "measureTotalCLOC", codeUnit.getMeasures(), meta);
            putMeasureSafely("m_static_loc_td", "measureTotalLOC", codeUnit.getMeasures(), meta);
            putMeasureSafely("m_static_complexity_td", "measureTotalCyclomaticComplexity", codeUnit.getMeasures(), meta);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Interface signature specification (similar to python notation) using format: [ClassName] { [method name]([list of input parameter types]:[output parameter types] }\n");
        sb.append(SignatureUtils.create(codeUnit).toLQL(true));
        sb.append("\n");

        return Document.from(sb.toString(), Metadata.from(meta));
    }

    private static void putMeasureSafely(String key, String newKey, Map<String, Double> from, Map<String, Object> to) {
        try {
            to.put(newKey, from.get(key));
        } catch (Throwable e) {
            //
            LOG.warn("Put measure failed", e);
        }
    }

    private static ContentRetriever createCodeUnitRetriever(List<Document> documents) {
        // Here, we create and empty in-memory store for our documents and their embeddings.
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Here, we are ingesting our documents into the store.
        // Under the hood, a lot of "magic" is happening, but we can ignore it for now.
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // Lastly, let's create a content retriever from an embedding store.
        return EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore)
                .maxResults(documents.size())
                //.minScore(someVal)
                .build();
    }

    protected List<Document> fromDataFrameResult(String type, DataFrame dataFrame) {
        List<Document> documents = new LinkedList<>();
        for(String column : (Set<String>) dataFrame.columns()) {
            if(StringUtils.equalsAnyIgnoreCase(column, "statement")) {
                continue;
            }

            String id = StringUtils.substringBeforeLast(column, "_");
            String adapter = StringUtils.substringAfterLast(column, "_");

            Map<String, Object> meta = new HashMap<>();
            meta.put("codeCandidateId", id);
            meta.put("codeAdapterId", adapter);
            // FIXME is oracle
            meta.put("isOracle", BooleanUtils.toStringYesNo(StringUtils.equalsIgnoreCase(id, "oracle")));

            StringBuilder sb = new StringBuilder();
            sb.append("Observations of test outputs for each test case statement using the format: [test case name] = [output]\n");
            int c = 0;
            List implCol = dataFrame.col(column);
            for(Object stmt : dataFrame.col("STATEMENT")) {
                sb.append(stmt);
                sb.append(" = ");
                sb.append(implCol.get(c++));
                sb.append("\n");
            }

            LOG.info("Document for {} is = {}, meta = {}", column, sb, meta);

            documents.add(Document.from(sb.toString(), Metadata.from(meta)));
        }

//        ByteArrayOutputStream out = new ByteArrayOutputStream();
////        try {
////            dataFrame.writeCsv(out);
////        } catch (IOException e) {
////            LOG.warn("failed to write out dataframe", e);
////        }
//
////        try (JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(
////                out, JsonEncoding.UTF8)) {
////            writeDataFrameToJson(dataFrame, jg);
////            jg.flush();
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
//
//        LOG.info("Written: {}", out.toString());

        return documents;
    }
}
