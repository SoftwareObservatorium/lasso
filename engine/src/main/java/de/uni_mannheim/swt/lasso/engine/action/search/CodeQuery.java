package de.uni_mannheim.swt.lasso.engine.action.search;

import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class CodeQuery extends LassoSpec {

    private String queryContent = "*:*";
    private int rows = 10;

    private String dataSource;

    private List<String> filters = Collections.emptyList();

    public String getQueryContent() {
        return queryContent;
    }

    public void setQueryContent(String queryContent) {
        this.queryContent = queryContent;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }
}
