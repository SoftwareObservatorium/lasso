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
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.engine.collect.RecordCollector;
import de.uni_mannheim.swt.lasso.engine.collect.Result;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.collections4.CollectionUtils;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.codehaus.groovy.runtime.GStringImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages actions and their instances.
 *
 * @author Marcus Kessel
 *
 */
public class ActionManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(ActionManager.class);

    private Map<String, Class<? extends DefaultAction>> registry = Collections.synchronizedMap(new HashMap<>());

    // ordered
    private Map<String, List<DefaultAction>> instances = Collections.synchronizedMap(new LinkedHashMap<>());

    public ActionManager() {
        //
        initRegistry(registry);
    }

    public void findAnnotatedClasses(String scanPackage) {
        ClassPathScanningCandidateComponentProvider provider = createComponentScanner();
        for (BeanDefinition beanDef : provider.findCandidateComponents(scanPackage)) {
            register(beanDef);
        }
    }

    private ClassPathScanningCandidateComponentProvider createComponentScanner() {
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(LassoAction.class));
        return provider;
    }

    private void register(BeanDefinition beanDef) {
        try {
            Class<? extends DefaultAction> cl = (Class<? extends DefaultAction>) Class.forName(beanDef.getBeanClassName());
            LassoAction lassoAction = cl.getAnnotation(LassoAction.class);

            Stable stable = cl.getAnnotation(Stable.class);

            if(LOG.isInfoEnabled()) {
                LOG.info(String.format("[%s] Found Action %s (%s)", stable != null ? "Stable" : "Unstable", cl.getName(), lassoAction.desc()));
            }

            // register
            registry.put(cl.getSimpleName(), cl);
        } catch (Throwable e) {
            LOG.warn(String.format("Failed to register Action %s", beanDef.getBeanClassName()), e);
        }
    }

    protected void initRegistry(Map<String, Class<? extends DefaultAction>> actionsRegistry) {
        // use scanning
        findAnnotatedClasses("de.uni_mannheim.swt.lasso.engine.action");
    }

    protected String createActionInstanceId(DefaultAction action) {
        return String.format("%s_%s", action.getName(), UUID.randomUUID().toString());
    }

    public boolean isLocalAction(String type) {
        Class<? extends DefaultAction> clazz = registry.get(type);
        Validate.notNull(clazz, "Given type unknown '%s'", clazz);

        return clazz.getAnnotation(Local.class) != null;
    }

    public boolean isDisablePartitioning(String type) {
        Class<? extends DefaultAction> clazz = registry.get(type);
        Validate.notNull(clazz, "Given type unknown '%s'", clazz);

        return clazz.getAnnotation(DisablePartitioning.class) != null;
    }

    public int getPartitioningMax(String type) {
        Class<? extends DefaultAction> clazz = registry.get(type);
        Validate.notNull(clazz, "Given type unknown '%s'", clazz);

        Partitioning partitioning = clazz.getAnnotation(Partitioning.class);

        return partitioning != null ? partitioning.max() : -1;
    }

    public boolean isTester(String type) {
        Class<? extends DefaultAction> clazz = registry.get(type);
        Validate.notNull(clazz, "Given type unknown '%s'", clazz);

        return clazz.getAnnotation(Tester.class) != null;
    }

    public List<Field> getInputFields(Class<? extends DefaultAction> actionClass) {
        return getAllFieldsWithAnnotation(actionClass, LassoInput.class);
    }

    /**
     * Keeps precedence of child action's fields (preferred over super class' fields)
     *
     * @param actionClass
     * @param annotationClass
     * @return
     */
    public List<Field> getAllFieldsWithAnnotation(Class<? extends DefaultAction> actionClass, Class <? extends Annotation> annotationClass) {
        // ordered by sub class first, then parent, then parent parent etc.
        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(actionClass, annotationClass);

        //
        Set<String> seen = new HashSet<>();

        // only add if not already seen
        return fields.stream().filter(f -> seen.add(f.getName())).collect(Collectors.toList());
    }

    public DefaultAction createLocalAction(String name, String actionType, ActionConfiguration actionConfiguration) throws IllegalAccessException, InstantiationException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Creating local action '{}' with type '{}'", name, actionType);
        }

        Class<? extends DefaultAction> actionClass = registry.get(actionType);
        DefaultAction action = actionClass.newInstance();
        action.setName(name);

        // set configuration
        for(Field field : getInputFields(actionClass)) {
            //
            if(actionConfiguration.getConfiguration().containsKey(field.getName())) {
                // make accessible
                field.setAccessible(true);

                // GStringImpl not compatible to String
                Object value = actionConfiguration.getConfiguration().get(field.getName());
                if(value != null) {
                    if(value instanceof GStringImpl && field.getType().equals(String.class)) {
                        value = value.toString();
                    }
                }

                field.set(action, value);
            }
        }

        String instanceId = createActionInstanceId(action);
        action.setInstanceId(instanceId);

        // add to instances
        if(!instances.containsKey(name)) {
            instances.put(name, new LinkedList<>());
        }

        instances.get(name).add(action);

        return action;
    }

    public boolean containsLocalAction(String name) {
        return instances.containsKey(name);
    }

    /**
     * For internal use only!
     *
     * @param name
     * @return
     */
    public DefaultAction getLocalAction(String name) {
        if(LOG.isDebugEnabled()) {
            instances.entrySet().stream().forEach(e -> {
                LOG.debug(String.format("Action '%s' has instances '%s' (execs '{}')", e.getKey(), e.getValue().stream().map(a -> a.getName() + "_" + a.getExecutables()).collect(Collectors.joining(","))));
            });
        }

        return instances.get(name).get(0);
    }

    public void execute(DefaultAction action, LSLExecutionContext lslExecutionContext) {
        //
        //action.execute(lslExecutionContext);
    }

    public Result collect(DefaultAction action, LSLExecutionContext lslExecutionContext,
                          String executableId) {
        System executable = action.getExecutables().getExecutable(executableId);

        List<RecordCollector> collectorList = action.createCollectors();
        if(CollectionUtils.isNotEmpty(collectorList)) {
            for(RecordCollector dataCollector : collectorList) {
                Result result;
                try {
                    result = dataCollector.collectData(lslExecutionContext, action, executable);
                } catch (Throwable e) {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Collector {} for {} failed with {}", dataCollector.getClass(), executableId, e.getMessage());
                    }

                    LOG.warn("Trace: ", e);

                    result = Result.ERROR;
                }

                // simply return result of first "failing" call (covers NULL as well)
                if(result != Result.SUCCESS) {
                    return result;
                }
            }
        }

        return Result.SUCCESS;
    }

    public Map<String, Class<? extends DefaultAction>> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    public Map<String, List<DefaultAction>> getInstances() {
        return instances;
    }
}
