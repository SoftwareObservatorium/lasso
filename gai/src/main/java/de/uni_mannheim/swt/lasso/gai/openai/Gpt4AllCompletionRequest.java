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
package de.uni_mannheim.swt.lasso.gai.openai;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class Gpt4AllCompletionRequest extends CompletionRequest {

    double top_k;

    double repeat_penalty;

    boolean echo;

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public double getRepeat_penalty() {
        return repeat_penalty;
    }

    public void setRepeat_penalty(double repeat_penalty) {
        this.repeat_penalty = repeat_penalty;
    }

    public double getTop_k() {
        return top_k;
    }

    public void setTop_k(double top_k) {
        this.top_k = top_k;
    }
}
