package de.uni_mannheim.swt.lasso.intelligence;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieval Augmented Generation.
 *
 * Ask questions based on LASSO's code searches.
 *
 * @author Marcus Kessel
 *
 */
public class RagService {

    private final ChatLanguageModel chatLanguageModel;

    public RagService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public interface Assistant {

        Result<String> chat(String userMessage);
    }

    public Assistant create(List<CodeUnit> codeUnits) {
        ContentRetriever contentRetriever = createContentRetriever(fromQueryResult(codeUnits));

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
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
        return Document.from(SignatureUtils.create(codeUnit).toLQL(true));
    }

    private static ContentRetriever createContentRetriever(List<Document> documents) {
        // Here, we create and empty in-memory store for our documents and their embeddings.
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Here, we are ingesting our documents into the store.
        // Under the hood, a lot of "magic" is happening, but we can ignore it for now.
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // Lastly, let's create a content retriever from an embedding store.
        return EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore).maxResults(documents.size()).build();
    }
}
