package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SheetResolver;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;

/**
 * A sheet based on {@link Table}.
 *
 * @author Marcus Kessel
 */
public class Sheet<R extends Comparable, C extends Comparable, V> {

    private Table<R, C, V> table = TreeBasedTable.create();

    // FIXME add name?
    public Sheet() {
    }

    public Sheet(Sheet<R, C, V> sheet) {
        this.table = TreeBasedTable.create((TreeBasedTable) sheet.getTable());
    }

    public Table<R, C, V> getTable() {
        return table;
    }

    public void put(R r, C c, V v) {
        table.put(r, c, v);
    }

    public V get(R r, C c) {
        return table.get(r, c);
    }

    public void debug() {
        for (Table.Cell<R, C, V> cell: table.cellSet()){
            System.out.println(cell.getRowKey()+" "+cell.getColumnKey()+" "+cell.getValue());
        }

        System.out.println();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Table.Cell<R, C, V> cell: table.cellSet()){
            sb.append(cell.getRowKey()+" "+cell.getColumnKey()+" "+cell.getValue());
        }

        return sb.toString();
    }

    public String toJsonl() throws IOException {
        Gson gson = new Gson();

        // {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for(R row : table.rowKeySet()) {
            StringBuilderWriter sbWriter = new StringBuilderWriter();
            JsonWriter writer = gson.newJsonWriter(sbWriter);
            writer.beginObject().name("sheet").value("FIXME").name("header").value("row " + c)
                    .name("cells").beginObject();

            for(C col : table.columnKeySet()) {
                V value = table.get(row, col);

                String cLbl = SheetResolver.toColumnLabel((Integer) col);
                String rLbl =SheetResolver.toRowLabel((Integer) row);

                writer.name(cLbl + rLbl).value((String) value);
            }

            writer.endObject().endObject();
            writer.close();

            sb.append(sbWriter);
            sb.append("\n");

            c++;
        }

        return sb.toString();
    }
}
