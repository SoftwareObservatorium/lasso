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
package de.uni_mannheim.swt.lasso.gai.openai

import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec

/**
 * A ChatBot completion prompt
 *
 * @author Marcus Kessel
 */
class Prompt extends LassoSpec {

    String model = "gpt-3.5-turbo"
    String role = "user"

    // model parameters
    int n = 1
    double temperature = 0.7
    int max_tokens = 2048
    double top_p = 1.0

    // OpenAI parameters
    double presence_penalty = 0
    double frequency_penalty = 0

    // Gpt4All parameters
    double top_k = 40
    double repeat_penalty = 1.18
    boolean echo = false

    String promptContent = ''
    String promptType = 'class'

    void prompt(String queryString, String type = 'class') {
        this.promptContent = queryString
        this.promptType = type
    }
}
