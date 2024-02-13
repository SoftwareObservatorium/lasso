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
package de.uni_mannheim.swt.lasso.arena.sequence.minimize;

import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.sequence.Sequence;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * (Simple) Sequence minimizer (drops duplicates).
 *
 * @author Marcus Kessel
 */
public class SequenceSpecificationMinimizer {

    private static final Logger LOG = LoggerFactory
            .getLogger(SequenceSpecificationMinimizer.class);

    /**
     * remove duplicate {@link randoop.sequence.Sequence}s.
     *
     * @param records
     * @return
     */
    public List<SequenceExecutionRecord> minimize(List<SequenceExecutionRecord> records) {
        List<SequenceExecutionRecord> remove = new LinkedList<>();
        for (int i = 0; i < records.size(); i++) {
            for (int j = 0; j < records.size(); j++) {
                if(i != j && !remove.contains(records.get(i))) {
                    // if A sequence == B sequence
                    Sequence a = records.get(i).getSequence();
                    Sequence b = records.get(j).getSequence();

                    if(a == null || b == null) {
                        continue;
                    }

                    if(Objects.equals(a, b)) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Dropping duplicate sequence '{}' =>\n{}",
                                    records.get(j).getSequenceSpecification().getName(),
                                    records.get(j).getSequence());

                            remove.add(records.get(j));
                        }
                    }
                }
            }
        }

        List<SequenceExecutionRecord> keep = new LinkedList<>(records);
        keep.removeAll(remove);

        return keep;
    }
}
