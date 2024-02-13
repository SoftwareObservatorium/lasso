/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.engine.action.test.support.surefire;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Surefire record.
 * 
 * @author Marcus Kessel
 *
 */
public class SurefireReports extends LassoReport {

    private List<SurefireReport> surefireReports = new LinkedList<>();

    public SurefireReport getSurefireReport(String testClassName) {
        Optional<SurefireReport> surefireReportOp = surefireReports.stream()
                .filter(sr -> StringUtils.equals(sr.getName(), testClassName)).findFirst();

        return surefireReportOp.orElse(null);
    }

    public List<SurefireTestCase> getTestCases(String testClassName, int permutationId) {
        SurefireReport surefireReport = getSurefireReport(testClassName);
        if(surefireReport == null) {
            return Collections.emptyList();
        }

        // GET ALL test cases, may include also those from inner classes etc.
        List<SurefireTestCase> allTestCases = surefireReport.getTestCases().values().stream()
                .flatMap(List::stream).collect(Collectors.toList());

        List<SurefireTestCase> permTestCases = allTestCases.stream()
                .filter(tc -> StringUtils.startsWith(tc.getName(), String.format("%s_", permutationId)))
                .collect(Collectors.toList());

        return permTestCases;
    }

    public long getTestCasesWithPermutationId() {
        return surefireReports.stream().flatMap(sr -> sr.getTestCases().values().stream().flatMap(List::stream))
                .filter(SurefireTestCase::hasPermId).count();
    }

    public List<SurefireReport> getSurefireReports() {
        return surefireReports;
    }

    public void setSurefireReports(List<SurefireReport> surefireReports) {
        this.surefireReports = surefireReports;
    }
}
