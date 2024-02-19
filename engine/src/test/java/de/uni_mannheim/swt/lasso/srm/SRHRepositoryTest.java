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
package de.uni_mannheim.swt.lasso.srm;

import de.uni_mannheim.swt.lasso.srm.operators.FunctionalCorrectness;
import de.uni_mannheim.swt.lasso.srm.operators.HeuristicsBasedCorrectness;
import joinery.DataFrame;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcus Kessel
 */
public class SRHRepositoryTest {

    static FunctionalCorrectness correctness = new HeuristicsBasedCorrectness();

    @Test
    public void testOracleFilters() throws IOException {
        DataFrame srm = new DataFrame<>("STATEMENT", "SYSTEMID", "VALUE");
        srm.append(Arrays.asList("sheet1@0,1", "system1", "[117,115,101,114,58,112,97,115,115]"));
        srm.append(Arrays.asList("sheet1@0,0", "system1", "_INSTANCE_"));
        srm.append(Arrays.asList("sheet1@0,1", "system2", "[1,2,3]"));
        srm.append(Arrays.asList("sheet1@0,0", "system2", "_INSTANCE_"));
        srm.append(Arrays.asList("sheet1@0,1", "system3", "[117,115,101,114,58,112,97,115,115]"));
        srm.append(Arrays.asList("sheet1@0,0", "system3", "_INSTANCE_"));

        System.out.println(srm.toString());

        Map<String, String> oracleFilters = new HashMap<>();
        oracleFilters.put("sheet1@0,1", "[117,115,101,114,58,112,97,115,115]");
        oracleFilters.put("sheet1@0,0", "_INSTANCE_");

        DataFrame wide = srm.pivot("STATEMENT", "SYSTEMID", "VALUE").sortBy("STATEMENT");
        System.out.println(wide.toString());

        SRHRepository srhRepository = new SRHRepository(null, correctness);
        wide = srhRepository.filterByOracleValues(wide, oracleFilters);

        assertEquals(new HashSet(Arrays.asList("STATEMENT", "system1", "system3")), wide.columns());
    }
}
