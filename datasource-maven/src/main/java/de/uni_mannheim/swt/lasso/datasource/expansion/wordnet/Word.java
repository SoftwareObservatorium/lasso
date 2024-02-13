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
package de.uni_mannheim.swt.lasso.datasource.expansion.wordnet;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class Word {

    private String text;

    private List<String> synonyms = new LinkedList<>();
    private List<String> antonyms = new LinkedList<>();

    public void addSynonyms(List<String> other) {
        for(String s : other) {
            if(!synonyms.contains(s)) {
                synonyms.add(s);
            }
        }
    }

    public void addAntonyms(List<String> other) {
        for(String s : other) {
            if(!antonyms.contains(s)) {
                antonyms.add(s);
            }
        }
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getAntonyms() {
        return antonyms;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
