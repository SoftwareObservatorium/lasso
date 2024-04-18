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
package de.uni_mannheim.swt.lasso.testing.generate.typeaware;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class TypeAwareMutatorTest {

    @Test
    public void testRandom() {
        MutatorSettings settings = new MutatorSettings();
        TypeAwareMutator mutator = new TypeAwareMutator(settings);

        System.out.println(mutator.mutateValue(true));
        System.out.println(mutator.mutateValue((byte) 7));
        System.out.println(mutator.mutateValue((short) 10));
        System.out.println(mutator.mutateValue(10));
        System.out.println(mutator.mutateValue(1000000000L));
        System.out.println(mutator.mutateValue(3.14f));
        System.out.println(mutator.mutateValue(3.14d));

        System.out.println(mutator.mutateValue("hello world"));
        System.out.println(mutator.mutateValue(Arrays.asList(1,2,3,4)));
        System.out.println(Arrays.toString(mutator.mutateValue(new Integer[]{1,2,3,4})));
        System.out.println(Arrays.toString(mutator.mutateValue(new int[]{1,2,3,4})));

        System.out.println(mutator.mutateValue(new HashSet<>(Arrays.asList(1,2,3,4))));

        Map map = new HashMap();
        map.put("bla", 5);
        map.put("blub", 50);
        map.put("lala", 500);
        System.out.println(mutator.mutateValue(map));

        Person person = new Person();
        person.setName("Thor");
        person.setAge(1500);
        System.out.println(ToStringBuilder.reflectionToString(mutator.mutateValue(person)));

        System.out.println(mutator.mutateValue((Object) "bla"));

        System.out.println(mutator.mutateValue(Long.valueOf(10L)));
    }

    public static class Person {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
