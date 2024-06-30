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
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        Result<String> chat(String userMessage);
    }

    public Assistant create(List<CodeUnit> codeUnits) {
        ContentRetriever codeUnitRetriever = createCodeUnitRetriever(fromQueryResult(codeUnits));

        ContentInjector contentInjector = DefaultContentInjector.builder()
        // .promptTemplate(...) // Formatting can also be changed
        .metadataKeysToInclude(asList("id", "fullyQualifiedName", "mavenCoordinates", "type", "score",
                "measureTotalBranches", "measureTotalCLOC", "measureTotalLOC", "measureTotalCyclomaticComplexity"))
        .build();

        // observations
        ContentRetriever observationsRetriever = null;

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
        meta.put("id", codeUnit.getId());
        meta.put("fullyQualifiedName", codeUnit.toFQName());
        meta.put("mavenCoordinates", codeUnit.toUri());
        meta.put("type", codeUnit.getDocType());
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

        return Document.from(SignatureUtils.create(codeUnit).toLQL(true), Metadata.from(meta));
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

    private static ContentRetriever createObservationsRetriever(List<Document> documents) {
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

    protected Document fromObservation() {
        // FIXME either we need a router with two retrievers https://github.com/langchain4j/langchain4j-examples/blob/main/rag-examples/src/main/java/_3_advanced/_07_Advanced_RAG_Multiple_Retrievers_Example.java
        // or merge into one document

        return null;
    }
}
