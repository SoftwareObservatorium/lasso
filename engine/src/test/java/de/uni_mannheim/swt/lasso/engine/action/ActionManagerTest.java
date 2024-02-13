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
package de.uni_mannheim.swt.lasso.engine.action;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import org.apache.commons.collections.map.SingletonMap;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class ActionManagerTest {


    @LassoAction
    class ParentAction extends DefaultAction {
        @LassoInput
        public int input = 0; // should be overridden by child

        @LassoInput
        public int parentUnique = 0;
    }

    @LassoAction
    class ChildAction extends ParentAction {
        @LassoInput
        public int input = 0;

        @LassoInput
        public int childUnique = 0;
    }

    @LassoAction
    static class StringAction extends DefaultAction {
        @LassoInput
        public String input = "";

        StringAction() {

        }
    }

    @Test
    public void test_gstringimpl() throws IllegalAccessException, InstantiationException {
        ActionManager actionManager = new ActionManager() {

            @Override
            protected void initRegistry(Map<String, Class<? extends DefaultAction>> actionsRegistry) {
                actionsRegistry.put(StringAction.class.getSimpleName(), StringAction.class);
            }
        };

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.addConfiguration(new SingletonMap("input",
                new GStringImpl(new Object[]{}, new String[]{"blub"})));

        StringAction action = (StringAction) actionManager.createLocalAction("blub", StringAction.class.getSimpleName(), actionConfiguration);
        assertEquals("blub", action.input);
    }

    @Test
    public void test_input_fields_inheritance() {
        ActionManager actionManager = new ActionManager();

        List<Field> fields = actionManager.getInputFields(ChildAction.class);

        assertEquals(3, fields.size());

        fields.forEach(System.out::println);

        assertThat(fields.stream().map(Field::getName).collect(Collectors.toList()), is(Arrays.asList("input", "childUnique", "parentUnique")));
    }

    @Test
    public void test_registry() {
        ActionManager actionManager = new ActionManager();

        Map<String, Class<? extends DefaultAction>> registry = actionManager.getRegistry();

        registry.forEach((k,v) -> {
            System.out.println(String.format("%s => %s", k, v));
        });
    }

    // TODO update
    @Disabled
    @Test
    public void test_configuration() throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        ActionManager actionManager = new ActionManager();

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        Map<String, Serializable> configuration = new LinkedHashMap<>();
        configuration.put("agentArgs", "some args");
        configuration.put("packageBlackList", new ArrayList<>(Arrays.asList("some.pkg.*", "de.uni_mannheim.*")));

        actionConfiguration.resetConfiguration(configuration);

        DefaultAction defaultAction = actionManager.createLocalAction("test", "JavaAgent", actionConfiguration);

        for(String fieldName : configuration.keySet()) {
            Field field = defaultAction.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            assertEquals(configuration.get(fieldName), field.get(defaultAction));
        }
    }
}
