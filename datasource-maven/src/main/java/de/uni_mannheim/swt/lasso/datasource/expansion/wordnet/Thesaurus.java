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
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple Thesaurus based on Wordnet (i.e., {@link Dictionary}).
 *
 * @author Marcus Kessel
 */
public class Thesaurus {

    /**
     * Keep numbers as part of split word
     */
    private static final String CC_REGEX = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    private static Dictionary dictionary;
    static {
        try {
            dictionary = Dictionary.getDefaultResourceInstance();

            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        try {
                            dictionary.close();
                        } catch (JWNLException e) {
                            throw new RuntimeException("Could not close dictionary", e);
                        }
                    }));
        } catch (JWNLException e) {
            throw new RuntimeException("Could not open dictionary", e);
        }
    }

    public static String getFirstWordFromName(String name) {
        if(StringUtils.isBlank(name)) {
            return null;
        }

        String[] parts = name.split(CC_REGEX);

        return parts[0];
    }

    public static Word getVerbMeaningAsCamelCase(String rawWord) throws JWNLException {
        String[] words = StringUtils.splitByCharacterTypeCamelCase(rawWord);

        if(ArrayUtils.isEmpty(words)) {
            return null;
        }

        Word word = new Word();

        // maybe best to look at first word only (i.e., starting verb)
        String verb = words[0];

        word.setText(verb);

        IndexWord iWord = dictionary.lookupIndexWord(POS.VERB, verb);

        if(iWord != null) {
            if(CollectionUtils.isNotEmpty(iWord.getSenses())) {
                for(Synset synset : iWord.getSenses()) {
//                    PointerTargetNodeList synonymsList = PointerUtils.getSynonyms(synset);
//
//                    word.getSynonyms().addAll(synonymsList.stream()
//                            // only letters and digits allowed
//                            .filter(p -> StringUtils.isAlphanumeric(p.getWord().getLemma()))
//                            .map(p -> p.getWord().getLemma()).collect(Collectors.toSet()));

                    // synonyms
                    if(CollectionUtils.isNotEmpty(synset.getWords())) {
                        List<String> synonyms = synset.getWords().stream()
                                .filter(w -> !StringUtils.equals(verb, w.getLemma()))
                                // only letters and digits allowed
                                .filter(w -> StringUtils.isAlphanumeric(w.getLemma()))
                                //.peek(w -> System.out.println(w))
                                .map(w -> CaseUtils.toCamelCase(w.getLemma(), false, ' '))
                                .distinct()
                                .collect(Collectors.toList());

                        word.addSynonyms(synonyms);
                    }

                    PointerTargetNodeList antonymsList = PointerUtils.getAntonyms(synset);

                    word.addAntonyms(antonymsList.stream()
                            // only letters and digits allowed
                            .filter(p -> StringUtils.isAlphanumeric(p.getWord().getLemma()))
                            .map(p -> p.getWord().getLemma()).distinct().collect(Collectors.toList()));
                }
            }
        }

        return word;
    }

    public static List<String> getVerbSynonymsAsCamelCase(String rawWord) throws JWNLException {
        String[] words = StringUtils.splitByCharacterTypeCamelCase(rawWord);

        if(ArrayUtils.isEmpty(words)) {
            return Collections.emptyList();
        }

        // maybe best to look at first word only (i.e., starting verb)
        String verb = words[0];

        System.out.println(verb);

        IndexWord iWord = dictionary.lookupIndexWord(POS.VERB, verb);

        if(iWord != null) {
            if(CollectionUtils.isNotEmpty(iWord.getSenses())) {
                for(Synset synset : iWord.getSenses()) {
                    System.out.println(synset.getPointers());

                    PointerUtils.getAntonyms(synset).print();
                }

                if(CollectionUtils.isNotEmpty(iWord.getSenses().get(0).getWords())) {
                    return iWord.getSenses().get(0).getWords().stream()
                            .filter(w -> !StringUtils.equals(verb, w.getLemma()))
                            .map(w -> CaseUtils.toCamelCase(w.getLemma(), false, ' '))
                            .collect(Collectors.toList());
                }
            }
        }

        // antonym? if antonym, ignore method? we look for decode, but get encode --> ignore?

        return null;
    }
}
