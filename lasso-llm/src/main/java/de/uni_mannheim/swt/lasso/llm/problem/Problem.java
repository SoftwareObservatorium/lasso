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
package de.uni_mannheim.swt.lasso.llm.problem;

public class Problem {
//        "name": "HumanEval_23_strlen",
//                "language": "java",
//                "prompt": "import java.util.*;\nimport java.lang.reflect.*;\nimport org.javatuples.*;\nimport java.security.*;\nimport java.math.*;\nimport java.io.*;\nimport java.util.stream.*;\nclass Problem {\n    // Return length of given string\n    // >>> stringLength((\"\"))\n    // (0l)\n    // >>> stringLength((\"abc\"))\n    // (3l)\n    public static long strlen(String string) {\n",
//                "doctests": "transform",
//                "original": "/home/arjun/repos/nuprl/MultiPL-E/datasets/../datasets/originals-with-cleaned-doctests/HumanEval_23_strlen.py",
//                "prompt_terminology": "reworded",
//                "tests": "    }\n    public static void main(String[] args) {\n    assert(strlen((\"\")) == (0l));\n    assert(strlen((\"x\")) == (1l));\n    assert(strlen((\"asdasnakj\")) == (9l));\n    }\n\n}\n",
//                "stop_tokens": [
//                "\n    }\n"
//                ]

    private String name;
    private String language;
    private String prompt;
    private String doctests;
    private String original;
    private String prompt_terminology;
    private String tests;
    private String[] stop_tokens;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getDoctests() {
        return doctests;
    }

    public void setDoctests(String doctests) {
        this.doctests = doctests;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getPrompt_terminology() {
        return prompt_terminology;
    }

    public void setPrompt_terminology(String prompt_terminology) {
        this.prompt_terminology = prompt_terminology;
    }

    public String getTests() {
        return tests;
    }

    public void setTests(String tests) {
        this.tests = tests;
    }

    public String[] getStop_tokens() {
        return stop_tokens;
    }

    public void setStop_tokens(String[] stop_tokens) {
        this.stop_tokens = stop_tokens;
    }
}
