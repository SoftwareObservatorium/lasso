package de.uni_mannheim.swt.lasso.core.dto;

/**
 *
 * @author Marcus Kessel
 */
public class ChatRequest {

    private String message;
    private SearchQueryRequest searchQueryRequest;

    private String modelName;
    private double temperature;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SearchQueryRequest getSearchQueryRequest() {
        return searchQueryRequest;
    }

    public void setSearchQueryRequest(SearchQueryRequest searchQueryRequest) {
        this.searchQueryRequest = searchQueryRequest;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
