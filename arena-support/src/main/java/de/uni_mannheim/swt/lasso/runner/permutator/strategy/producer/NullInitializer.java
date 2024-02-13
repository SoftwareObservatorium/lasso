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
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import de.uni_mannheim.swt.lasso.runner.permutator.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Initialize a constructor with default values.
 *
 * @author Marcus Kessel
 */
public class NullInitializer implements ProducerStrategy {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(NullInitializer.class);

    @Override
    public List<Candidate> match(Class<?> t, Class<?>[] paramTypes) throws Throwable {
        Constructor[] constructors = t.getDeclaredConstructors();

        if(ArrayUtils.isNotEmpty(constructors)) {
            // only non-default constructors
            Optional<Constructor> optional = Arrays.stream(constructors).filter(c -> c.getParameterCount() > 0).findFirst();

            if(!optional.isPresent()) {
                return null;
            }

            Constructor constructor = optional.get();

            List<Candidate> candidates = new ArrayList<>(1);
            // TODO positions
            Candidate candidate = new Candidate(constructor, new int[0]);
            candidate.setProducerStrategy(this);

            candidates.add(candidate);

            return candidates;
        }

        return null;
    }

    @Override
    public Object createInstance(Candidate candidate, Object[] inputs) throws Throwable {
        if(candidate.getMethod() instanceof Constructor) {
            try {
                Constructor constructor = (Constructor) candidate.getMethod();
                // make accessible
                constructor.setAccessible(true);

                Type[] types = constructor.getGenericParameterTypes();

                Object[] values = new Object[types.length];
                for(int i = 0; i < values.length; i++) {
                    values[i] = getValue(types[i].getTypeName());
                }

                Logger.info("Invoking "+constructor+" with " + Arrays.toString(values));

                Object instance = constructor.newInstance(values);

                return instance;
            } catch (Throwable e) {
                e.printStackTrace();

                // signal error
                return null;
            }
        } else {
            throw new IllegalArgumentException("Candidate member must be Constructor");
        }
    }

    private static Object getValue(String type) {

        switch (type) {

            //TODO add others
            case "java.lang.Byte":
                return (byte) 1;
            case "java.lang.Short":
                return (short) 1;
            case "java.lang.Character":
                return (char) 1;
            case "java.lang.Integer":
                return 1;
            case "java.lang.Long":
                return 1L;
            case "java.lang.Float":
                return 1f;
            case "java.lang.Double":
                return 1d;
            case "java.lang.Boolean":
                return true;
            case "java.lang.String":
                return "";
            case "java.lang.CharSequence":
                return "";

            default:
                return null;
        }
    }
}
