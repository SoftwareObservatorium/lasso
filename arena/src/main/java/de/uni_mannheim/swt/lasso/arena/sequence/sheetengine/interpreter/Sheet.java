package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * A sheet based on {@link Table}.
 *
 * @author Marcus Kessel
 */
public class Sheet<R extends Comparable, C extends Comparable, V> {

    private Table<R, C, V> table = TreeBasedTable.create();

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
}
