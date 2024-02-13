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
package de.uni_mannheim.swt.lasso.arena.task.load;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public interface SheetProvider {

    /**
     * Resolve CUTs and Sheets.
     *
     * @return
     * @throws IOException
     */
    List<ResolvedSheets> resolve() throws IOException;

    /**
     * Resolve CUTs only. No sheets.
     *
     * @throws IOException
     */
    void resolveCuts() throws IOException;

    /**
     * Get implementations managed by this provider.
     *
     * @return
     */
    List<String> getImplementations();

    /**
     * Get {@link CandidatePool} managed by this provider.
     *
     * @return
     */
    CandidatePool getPool();
}
