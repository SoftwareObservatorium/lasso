package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import de.uni_mannheim.swt.lasso.ssn.SheetResolver;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A sheet based on {@link Table}.
 *
 * @author Marcus Kessel
 */
// FIXME different imlementations (e.g., backed by SQL table etc.)
public class Sheet<R extends Comparable, C extends Comparable, V> {

    private Table<R, C, V> table = TreeBasedTable.create();

    public Sheet() {
    }

    public Sheet(Table<R, C, V> table) {
        this.table = table;
    }

    public Sheet(Sheet<R, C, V> sheet) {
        this(TreeBasedTable.create((TreeBasedTable) sheet.table));
    }

//    public Table<R, C, V> getTable() {
//        return table;
//    }

    public void put(R r, C c, V v) {
        table.put(r, c, v);
    }

    public V get(R r, C c) {
        return table.get(r, c);
    }

    public Set<C> getColumns() {
        return table.columnKeySet();
    }

    public Set<R> getRows() {
        return table.rowKeySet();
    }

    public Set<Table.Cell<R, C, V>> getCells() {
        return table.cellSet();
    }

    public Sheet<R, C, V> getColumnAsSheet(C from, C to) {
        Map<R, V> column = table.column(from);
        Table<R, C, V> sub = TreeBasedTable.create();
        for(Map.Entry<R,V> entry: column.entrySet()) {
            sub.put(entry.getKey(), to, entry.getValue());
        }

        return new Sheet<>(sub);
    }

    public boolean isEquivalentColumn(Sheet<R, C, V> otherSheet, C c) {
        Map<R, V> column = table.column(c);
        Map<R, V> otherColumn = otherSheet.table.column(c);

        return Objects.equals(column, otherColumn);
    }

    public void addColumn(Sheet<R, C, V> otherSheet, C from, C to) {
        Map<R, V> column = otherSheet.table.column(from);
        for(Map.Entry<R,V> entry: column.entrySet()) {
            table.put(entry.getKey(), to, entry.getValue());
        }
    }

    public int getNumberOfRows() {
        return table.rowKeySet().size();
    }

    public int getNumberOfColumns() {
        return table.columnKeySet().size();
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
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object other) {
        Sheet otherSheet = (Sheet) other;

        return table.equals(otherSheet.table);
    }

    public String toJsonl() throws IOException {
        Gson gson = new Gson();

        // {"sheet": "Sheet 1", "header": "Row 1", "cells": {"A1": {}, "B1": "create", "C1": "Stack"}}
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for(R row : table.rowKeySet()) {
            StringBuilderWriter sbWriter = new StringBuilderWriter();
            JsonWriter writer = gson.newJsonWriter(sbWriter);
            writer.beginObject()//.name("sheet").value("FIXME").name("header").value("row " + c)
                    .name("cells").beginObject();

            for(C col : table.columnKeySet()) {
                V value = table.get(row, col);

                String cLbl = SheetResolver.toColumnLabel((Integer) col);
                String rLbl =SheetResolver.toRowLabel((Integer) row);

                if(value instanceof String || value == null) {
                    writer.name(cLbl + rLbl).value((String) value);
                }

                if(value instanceof Double) {
                    Double numVal = (Double) value;
                    if(numVal.isNaN()) {
                        writer.name(cLbl + rLbl).value(-1d);
                    } else {
                        writer.name(cLbl + rLbl).value((double) value);
                    }
                }

                // FIXME other types?
                if(value instanceof Boolean) {
                    writer.name(cLbl + rLbl).value((boolean) value);
                }
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
