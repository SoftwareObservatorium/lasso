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

import net.sf.extjwnl.JWNLException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class ThesaurusTest {

    @Test
    public void test_verb_camelCase() throws JWNLException {
        List<String> synonyms = Thesaurus.getVerbSynonymsAsCamelCase("splitByCharacterTypeCamelCase");

        System.out.println(synonyms);

        assertTrue(synonyms.size() > 0);
    }

    @Test
    public void test_verb_non_camelCase() throws JWNLException {
        List<String> synonyms = Thesaurus.getVerbSynonymsAsCamelCase("decode");

        System.out.println(synonyms);

        assertTrue(synonyms.size() > 0);
    }

    @Test
    public void test_verb_MEANING() throws JWNLException {
        String verb = "set";
        Word wordMeaning = Thesaurus.getVerbMeaningAsCamelCase(verb);

        System.out.println(wordMeaning.getSynonyms());
        System.out.println(wordMeaning.getAntonyms());

        assertEquals(verb, wordMeaning.getText());

        assertTrue(wordMeaning.getSynonyms().size() > 0);
        assertTrue(wordMeaning.getAntonyms().size() > 0);
    }

    @Test
    public void test_getFirstWordFromName() {
        assertThat(Thesaurus.getFirstWordFromName("Base64UtilsManager666IchHabe_Fertig"), is("Base64"));
        assertThat(Thesaurus.getFirstWordFromName("Base64"), is("Base64"));
        assertThat(Thesaurus.getFirstWordFromName("base64"), is("base64"));
        assertThat(Thesaurus.getFirstWordFromName(null), isEmptyOrNullString());
        assertThat(Thesaurus.getFirstWordFromName(""), isEmptyOrNullString());
        assertThat(Thesaurus.getFirstWordFromName(" "), isEmptyOrNullString());
        assertThat(Thesaurus.getFirstWordFromName("HTTPManager"), is("HTTP"));
    }
}
