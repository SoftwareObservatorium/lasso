package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.srh;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.ArenaUtils;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.srm.CellId;
import de.uni_mannheim.swt.lasso.srm.CellValue;
import org.apache.commons.collections4.MapUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public class SRHWriter {

    private final LassoClusterClient lassoClusterClient;

    public SRHWriter(LassoClusterClient lassoClusterClient) {
        this.lassoClusterClient = lassoClusterClient;
    }

    // FIXME store stimulus sheets
    public void storeActuationSheet(ArenaJob arenaJob, String arenaId, AdaptedImplementation adaptedImplementation, Test test, TestInvocation testInvocation, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> sheet) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        // sheetId
        String sheetId = test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

        try {
            for (Table.Cell<Integer, Integer, String> cell : sheet.getCells()) {
                // column
                String col = "input_value";
                if(cell.getColumnKey() == 0) {
                    col = "value";
                } else if(cell.getColumnKey() == 1) {
                    col = "op";
                } else if(cell.getColumnKey() == 2) {
                    col = "service";
                }

                CellId cellId = ArenaUtils.cellIdOf(sheetId, cell.getColumnKey(), cell.getRowKey(), col, adaptedImplementation);
                CellValue cellValue = new CellValue();
                cellValue.setValue(cell.getValue());
                cellValue.setRawValue(cell.getValue());
                //cellValue.setValueType();
                //cellValue.setExecutionTime();
                cellValue.setLastModified(new Date());

                cells.put(cellId, cellValue);
            }

            // store
            store(cells, arenaJob, arenaId);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeOracleActuationSheet(ArenaJob arenaJob, String arenaId, Test test, TestInvocation testInvocation, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> sheet) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        // sheetId
        String sheetId = test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

        try {
            for (Table.Cell<Integer, Integer, String> cell : sheet.getCells()) {
                // column
                String col = "input_value";
                if(cell.getColumnKey() == 0) {
                    col = "value";
                } else if(cell.getColumnKey() == 1) {
                    col = "op";
                } else if(cell.getColumnKey() == 2) {
                    col = "service";
                }

                CellId cellId = ArenaUtils.cellIdOfOracle(sheetId, cell.getColumnKey(), cell.getRowKey(), col);
                CellValue cellValue = new CellValue();
                cellValue.setValue(cell.getValue());
                cellValue.setRawValue(cell.getValue());
                //cellValue.setValueType();
                //cellValue.setExecutionTime();
                cellValue.setLastModified(new Date());

                cells.put(cellId, cellValue);
            }

            // store
            store(cells, arenaJob, arenaId);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeStimulusSheet(ArenaJob arenaJob, String arenaId, Test test, TestInvocation testInvocation) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        // sheetId
        String sheetId = test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";

        try {
            CellId cellId = new CellId();
            cellId.setSheetId(sheetId);
            cellId.setX(-1);
            cellId.setY(-1);
            cellId.setType("stimulussheet");
            cellId.setSystemId("abstraction");
            cellId.setVariantId("abstraction");
            cellId.setAdapterId("abstraction");

            CellValue cellValue = new CellValue();
            cellValue.setValue(test.getParsedSheet().getSheet().getBody());
            //cellValue.setRawValue(cell.getValue());
            //cellValue.setValueType();
            //cellValue.setExecutionTime();
            cellValue.setLastModified(new Date());

            cells.put(cellId, cellValue);

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        try {
            CellId cellId = new CellId();
            cellId.setSheetId(sheetId);
            cellId.setX(-1);
            cellId.setY(-1);
            cellId.setType("interface");
            cellId.setSystemId("abstraction");
            cellId.setVariantId("abstraction");
            cellId.setAdapterId("abstraction");

            CellValue cellValue = new CellValue();
            cellValue.setValue(test.getParsedSheet().getSheet().getInterfaceSpecification());
            //cellValue.setRawValue(cell.getValue());
            //cellValue.setValueType();
            //cellValue.setExecutionTime();
            cellValue.setLastModified(new Date());

            cells.put(cellId, cellValue);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        try {
            // store
            store(cells, arenaJob, arenaId);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeMetricActuationSheet(ArenaJob arenaJob, String arenaId, AdaptedImplementation adaptedImplementation, String reportId, de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, Object> sheet) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        try {
            for (Integer row : sheet.getRows()) {
                String col = (String) sheet.get(row, 1);

                CellId cellId = ArenaUtils.cellIdOf(reportId, -1, -1, col, adaptedImplementation);
                CellValue cellValue = new CellValue();
                cellValue.setValue(Objects.toString(sheet.get(row, 0)));
                //cellValue.setRawValue(cell.getValue());
                //cellValue.setValueType();
                //cellValue.setExecutionTime();
                cellValue.setLastModified(new Date());

                cells.put(cellId, cellValue);
            }

            // store
            store(cells, arenaJob, arenaId);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeMutationActuationSheet(ArenaJob arenaJob, String arenaId, AdaptedImplementation adaptedImplementation, String reportId) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        ClassUnderTest classUnderTest = adaptedImplementation.getAdaptee();
        if(classUnderTest.getContext().containsKey(reportId)) {
            try {
                String mdJson = (String) classUnderTest.getContext().get(reportId);

                CellId cellId = new CellId();
                cellId.setSheetId(reportId);
                cellId.setX(-1);
                cellId.setY(-1);
                cellId.setType("mutant");
                cellId.setSystemId(adaptedImplementation.getAdaptee().getId());
                cellId.setVariantId(adaptedImplementation.getAdaptee().getVariantId());
                cellId.setAdapterId(adaptedImplementation.getAdapterId());

                CellValue cellValue = new CellValue();
                cellValue.setValue(mdJson);
                //cellValue.setRawValue(cell.getValue());
                //cellValue.setValueType();
                //cellValue.setExecutionTime();
                cellValue.setLastModified(new Date());

                cells.put(cellId, cellValue);

                // store
                store(cells, arenaJob, arenaId);
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Store static code measures from code index in SRM
     *
     * @param arenaJob
     * @param arenaId
     * @param adaptedImplementation
     * @param reportId
     */
    public void storeStaticMetricsSheet(ArenaJob arenaJob, String arenaId, AdaptedImplementation adaptedImplementation, String reportId) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        ClassUnderTest classUnderTest = adaptedImplementation.getAdaptee();
        if(MapUtils.isNotEmpty(classUnderTest.getImplementation().getCode().getMeasures())) {
            for(String metricId : classUnderTest.getImplementation().getCode().getMeasures().keySet()) {
                Double measure = classUnderTest.getImplementation().getCode().getMeasures().get(metricId);

                if(measure != null) {
                    try {
                        CellId cellId = new CellId();
                        cellId.setSheetId(reportId);
                        cellId.setX(-1);
                        cellId.setY(-1);
                        cellId.setType(metricId);
                        cellId.setSystemId(adaptedImplementation.getAdaptee().getId());
                        cellId.setVariantId(adaptedImplementation.getAdaptee().getVariantId());
                        cellId.setAdapterId(adaptedImplementation.getAdapterId());

                        CellValue cellValue = new CellValue();
                        cellValue.setValue(String.valueOf(measure));
                        //cellValue.setRawValue(cell.getValue());
                        //cellValue.setValueType();
                        //cellValue.setExecutionTime();
                        cellValue.setLastModified(new Date());

                        cells.put(cellId, cellValue);

                        // store
                        store(cells, arenaJob, arenaId);
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
    }

    public void storeRuntimeMetric(ArenaJob arenaJob, String arenaId, AdaptedImplementation adaptedImplementation, String metricId, Number measure) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

//        // sheetId
//        String sheetId = test.getSignature().getName() + "(" + testInvocation.getInvocationExpression() + ")";
//
//        try {
//            long executionTimeNanos = executedInvocations.getExecutionTime();
//
//            CellId cellId = ArenaUtils.cellIdOf(sheetId, -1, -1, "executionTimeNanos", adaptedImplementation);
//            CellValue cellValue = new CellValue();
//            cellValue.setValue(String.valueOf(executionTimeNanos));
//            cellValue.setRawValue(String.valueOf(executionTimeNanos));
//            //cellValue.setValueType();
//            //cellValue.setExecutionTime();
//            cellValue.setLastModified(new Date());
//
//            cells.put(cellId, cellValue);
//
//            // store
//            store(cells, arenaJob, arenaId);
//        } catch (RuntimeException e) {
//            throw new RuntimeException(e);
//        }

        // make compatible with other metrics
        try {
            CellId cellId = new CellId();
            cellId.setSheetId("runtimeMetrics");
            cellId.setX(-1);
            cellId.setY(-1);
            cellId.setType(metricId);
            cellId.setSystemId(adaptedImplementation.getAdaptee().getId());
            cellId.setVariantId(adaptedImplementation.getAdaptee().getVariantId());
            cellId.setAdapterId(adaptedImplementation.getAdapterId());

            CellValue cellValue = new CellValue();
            cellValue.setValue(String.valueOf(measure));
            //cellValue.setRawValue(cell.getValue());
            //cellValue.setValueType();
            //cellValue.setExecutionTime();
            cellValue.setLastModified(new Date());

            cells.put(cellId, cellValue);

            // store
            store(cells, arenaJob, arenaId);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void store(Map<CellId, CellValue> cells, ArenaJob arenaJob, String arenaId) {
        if (MapUtils.isNotEmpty(cells)) {
            cells.keySet().forEach(id -> {
                id.setExecutionId(arenaJob.getExecutionId());
                id.setAbstractionId(arenaJob.getAbstractionId());
                id.setArenaId(arenaId);
                id.setActionId(arenaJob.getActionId());
            });

            // store all cells
            lassoClusterClient.getSrmRepository().putAll(cells);
        }
    }
}