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
package de.uni_mannheim.swt.lasso.arena.adaptation;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.filter.CompositeSpecFilter;
import de.uni_mannheim.swt.lasso.arena.adaptation.filter.SpecFilter;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.ClassPermutation;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.ClassPermutationCompositeValidator;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.ClassPermutatorDistanceRanking;
import de.uni_mannheim.swt.lasso.arena.adaptation.permutator.PermutatorAdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.runner.permutator.*;
import de.uni_mannheim.swt.lasso.runner.permutator.combination.PermutationStategy;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.FactoryMethodStrategy;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.ProducerStrategy;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.StaticFieldInstance;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Default adaptation strategy
 *
 * @author Marcus Kessel
 */
public class DefaultAdaptationStrategy implements AdaptationStrategy {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(DefaultAdaptationStrategy.class);

    /**
     * Permutation validation
     */
    private ClassPermutationCompositeValidator validator = new ClassPermutationCompositeValidator(
            //new DistinctMethodsValidator(),

            permutation -> true
    );

    private List<ProducerStrategy> producerStrategies = Arrays.asList(
            new FactoryMethodStrategy(null),
            new StaticFieldInstance(null)//,
            //new NullInitializer()
    );

    /**
     * Adaptation strategies
     *
     * Attention: Order matters! (impacts {@link PermutationStategy})
     */
    public List<de.uni_mannheim.swt.lasso.runner.permutator.strategy.method.AdaptationStrategy> adaptationStrategies = Arrays.asList(
//            new MutabilityStrategyByReference(),
//            new MutabilityStrategyByValue(),
            new de.uni_mannheim.swt.lasso.runner.permutator.strategy.method.JavaConverterStrategy()//,
            //new DefaultParameterValueStrategy(SCAN_PKG_NAMES)
            // FIXME add ObjectGraphStrategy again
            //new ObjectGraphStrategy()
    );

    /**
     * Ranking for permutations
     */
    private Comparator<ClassPermutation> sorting = new ClassPermutatorDistanceRanking();

    /**
     * Method filter.
     */
    public CompositeSpecFilter specFilter = new CompositeSpecFilter(
            //new ObjectIdentityFilter(),/* new FrequentMethodFilter()*/ new SyntheticMethodFilter(), new GetterAndSetterFilter()
            (classUnderTest, method) -> true
    );
    // TODO add or make optional new GetterAndSetterFilter() ?

    /**
     * We limit maximum input params because of combinatorial explosion
     */
    private int maxParamsLength = 5;

    public void addSpecFilter(SpecFilter specFilter) {
        this.specFilter.addFilter(specFilter);
    }

    @Override
    public List<AdaptedImplementation> adapt(InterfaceSpecification specification, ClassUnderTest classUnderTest, int limit) {
        List<AdaptedImplementation> adaptedImplementations = new LinkedList<>();

        List<ClassPermutation> permutationList = null;
        try {
            permutationList = create(specification, classUnderTest, limit);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create permutations", e);
        }

        LOG.info("{} has {} permutations", classUnderTest, permutationList.size());

        if(limit > 0 && limit < permutationList.size()) {
            permutationList = permutationList.subList(0, limit);

            LOG.info("{} has {} FILTERED permutations", classUnderTest, permutationList.size());
        }

        int pid = 0;
        for(ClassPermutation permutation : permutationList) {
//            // FIXME do not allow permutations with duplicate methods .. for now.
//            // FIXME does it really happen?
//            Set<Member> members = new HashSet<>();
//            for (Candidate candidate : permutation.getMethods()) {
//                members.add(candidate.getMethod());
//            }
//
//            if (members.size() < permutation.getMethods().size()) {
//                continue;
//            }

            permutation.setId(pid++); // FIXME potentially problematic

            PermutatorAdaptedImplementation adaptedImplementation = new PermutatorAdaptedImplementation(classUnderTest, permutation);
            adaptedImplementations.add(adaptedImplementation);
        }

        return adaptedImplementations;
    }

    protected List<ClassPermutation> create(InterfaceSpecification specification, ClassUnderTest classUnderTest, int limit) throws ClassNotFoundException {
        Class<?> cutClass = classUnderTest.loadClass();
        // check if cut class is concrete (i.e non-abstract)
        checkCutClass(cutClass);

        List<List<Candidate>> candidates = new ArrayList<>();

//        if(specification.isEmpty()) {
//
//        }

        try {
            for(MethodSignature methodSignature : specification.getMethods()) {
                List<Candidate> methodCandidates = resolveMethods(classUnderTest, cutClass,
                        methodSignature.getParameterTypes(cutClass),
                        methodSignature.getReturnType(cutClass), false);
                // add
                candidates.add(methodCandidates);
            }

            boolean hasMethods = !candidates.isEmpty();
            if(!hasMethods) {
                if(Logger.isInfoEnabled()) {
                    Logger.info("No methods to adapt");
                }
            }

            if(hasMethods) {
                // check if we couldn't find a candidate for some method sig
                int methodCount = 0;
                // failed to find certain methods
                List<Integer> fails = new LinkedList<>();
                for (List<Candidate> cList : candidates) {
                    if (CollectionUtils.isEmpty(cList)) {
                        // fail
                        fails.add(methodCount);
                    }
                    methodCount++;
                }

                // worst case, no suitable methods found at all
                if (fails.size() == methodCount) {
                    throw new PermutatorException(
                            "Couldn't find suitable method candidate(s) for methods: "
                                    + Arrays.toString(
                                    fails.toArray(new Integer[0])));
                }
            }

            // handle multiple constructors
            List<List<Candidate>> constructorCandidates = resolveInitializers(
                    classUnderTest, cutClass, specification.getConstructors(), candidates);

            // set additional ranking options if available
            String[] methodNames = specification.getMethodNames();
            Comparator<ClassPermutation> ranking = new ClassPermutatorDistanceRanking(methodNames);

            EfficientCombination permutationStrategy = new EfficientCombination(5, limit);

            // create method permutations for all combinations of methods
            return createPermutations(constructorCandidates,
                    candidates, permutationStrategy, ranking, specification, limit);
        } catch (Throwable e) {
            throw new PermutatorException(
                    "Cannot instantiate Permutator for " + cutClass.getName(),
                    e);
        }
    }

    /**
     * create permutations over all constructors and methods
     *
     * @param constructors
     * @param candidates
     * @param ranking
     * @return
     */
    protected List<ClassPermutation> createPermutations(
            List<List<Candidate>> constructors, List<List<Candidate>> candidates,
            EfficientCombination permutationStrategy,
            Comparator<ClassPermutation> ranking, InterfaceSpecification specification, int limit) {
        List<ClassPermutation> permutations = new ArrayList<>();

        // no methods to adapt?
        if(candidates.isEmpty()) {
//            for (List<Candidate> constructor : constructors) {
//                ClassPermutation permutation = new ClassPermutation(constructor, new LinkedList<>());
//                permutations.add(permutation);
//            }

            ClassPermutation permutation = new ClassPermutation(constructors, new LinkedList<>());
            permutations.add(permutation);

            return permutations;
        }

        // create permutations over all constructors and methods
//        if(CollectionUtils.isEmpty(constructors)) {
//            // FIXME no constructor given
//            List<ClassPermutation> perms = createMethodPermutations(new ArrayList<>(),
//                    candidates, permutationStrategy, ranking, specification, limit);
//            //
//            if (CollectionUtils.isNotEmpty(perms)) {
//                //
//                permutations.addAll(perms);
//            }
//        } else {
//            for (List<Candidate> constructor : constructors) {
//                List<ClassPermutation> perms = createMethodPermutations(constructor,
//                        candidates, permutationStrategy, ranking, specification, limit);
//                //
//                if (CollectionUtils.isNotEmpty(perms)) {
//                    //
//                    permutations.addAll(perms);
//                }
//            }
//        }

        permutations = createMethodPermutations(constructors,
                        candidates, permutationStrategy, ranking, specification, limit);

        return permutations;
    }

    /**
     * Create permutations for all combinations of method calls
     *
     * @return
     */
    protected List<ClassPermutation> createMethodPermutations(
            List<List<Candidate>> constructorCandidates, List<List<Candidate>> candidates,
            EfficientCombination permutationStrategy,
            Comparator<ClassPermutation> ranking, InterfaceSpecification specification, int limit) {
        // perms
        List<ClassPermutation> permutations = permutationStrategy.createPermutations(
                constructorCandidates, candidates, specification);

        // filter + rank
        permutations = permutations.stream().filter(validator::isValid)
                .sorted(ranking).collect(Collectors.toList());

        // cap if limit is set
        if (CollectionUtils.isNotEmpty(permutations)
                && permutations.size() > limit) {
            permutations = permutations.subList(0, limit);
        }

        return permutations;
    }

    /**
     * Check if cut class is neither null nor interface
     *
     * @param cutClass
     */
    protected void checkCutClass(Class<?> cutClass) {
        //
        if (cutClass == null) {
            throw new PermutatorException("CUT class was null");
        }

        // ALLOW for abstract classes
//        if (Modifier.isAbstract(cutClass.getModifiers())) {
//            throw new PermutatorException(
//                    "CUT class is abstract " + cutClass.getName());
//        }

        if (cutClass.isInterface()) {
            throw new PermutatorException(
                    "CUT class is interface " + cutClass.getName());
        }
    }

    protected List<List<Candidate>> resolveInitializers(ClassUnderTest classUnderTest, Class<?> cutClass, List<MethodSignature> constructors, List<List<Candidate>> candidates) {
        if(CollectionUtils.isEmpty(constructors)) {
            // FIXME what to return? NoOp?

            return new LinkedList<>();
        }

        List<List<Candidate>> allConstructorCandidates = new LinkedList<>();

        // 1st: resolve constructors
        for(MethodSignature constructorSignature : constructors) {
            // 1st attempt: try to resolve suitable constructors
            List<Candidate> constructorCandidates = new LinkedList<>();
            try {
                List<Candidate> initializers = resolveMethods(classUnderTest, cutClass, constructorSignature.getParameterTypes(),
                        null, true);

                if(initializers != null) {
                    constructorCandidates.addAll(initializers);
                }
            } catch (Throwable e) {
                LOG.warn("Constructor resolve failed for {}", constructorSignature);
                LOG.warn("Stack trace", e);
            }

//            // 2nd attempt: try to find methods instead (FIXME if we do that, we need a constructor anyways if method non-static)
//            // this becomes then: initializer of instance + initializing method
//            try {
//                resolveMethods(classUnderTest, cutClass, constructorSignature.getParameterTypes(),
//                        null, false);
//            } catch (Throwable e) {
//                LOG.warn("Constructor resolve failed for {}", constructorSignature);
//                LOG.warn("Stack trace", e);
//            }

            // 2nd: use default constructor instead
            if(ArrayUtils.isNotEmpty(constructorSignature.getParameterTypes())) { // if it were empty, we already have the default constructor
                try {
                    List<Candidate> initializers = resolveMethods(classUnderTest, cutClass, new Class[0],
                            null, true);
                    if(initializers != null) {
                        constructorCandidates.addAll(initializers);
                    }
                } catch (Throwable e) {
                    LOG.warn("Resolving default constructor failed for {}", cutClass.getName());
                    LOG.warn("Stack trace", e);
                }

                // FIXME what if no default constructor exists? e.g. FastStack(int) .. use defaults?
                // if original tests exist, use this knowledge how to initialize?
            }

            // 3rd: check for static
            // add a static init in case no init params required
            if (/*ArrayUtils.isEmpty(paramTypes) &&*/ hasStaticMethods(candidates)) {
                // use ghost
                Candidate ghostInit = new Candidate(null,
                        new int[0]);
                ghostInit.setGhost(true);
                ghostInit.setGhostDescription("_STATIC_INIT_()");

                constructorCandidates.add(ghostInit);
            }

            allConstructorCandidates.add(constructorCandidates);
        }



        if(CollectionUtils.isEmpty(allConstructorCandidates)) {
            throw new PermutatorException(
                    "Couldn't find any suitable constructor (i.e initializer)");
        }

        return allConstructorCandidates;
    }

    /**
     * Generally, the whole type hierarchy is assessed for compatible param
     * types (e.g. java.io.ByteArrayInputStream matches with java.io.InputStream
     * etc.). Primitives are handled as well (wrapper converting and vice
     * versa).
     *
     * <pre>
     * 1. step: Determine direct parameter matches. Note: Direct parameter matches are always ranked first
     * 2. step:
     * 3. step:
     * </pre>
     *
     *
     * @param classUnderTest
     * @param cutClass
     * @param paramTypes
     * @param returnTypeClass
     * @param constructor
     *            find Constructor instead of Method
     * @return
     */
    protected List<Candidate> resolveMethods(ClassUnderTest classUnderTest, Class<?> cutClass, Class<?>[] paramTypes,
                                             Class<?> returnTypeClass, boolean constructor) throws Exception {
//        if(ArrayUtils.isNotEmpty(paramTypes) && paramTypes.length > getMaxParamsLength()) {
//            throw new UnsupportedOperationException("We do not support param types > 5");
//        }

        // find methods
        MethodResult methodResult = findMethods(classUnderTest, cutClass, returnTypeClass, paramTypes, constructor);

        // apply "heavy" adaptation strategies
        // FIXME maybe it's wise to only execute this part if no candidates have been found
        if(!constructor) {
            for(de.uni_mannheim.swt.lasso.runner.permutator.strategy.method.AdaptationStrategy adaptationStrategy : adaptationStrategies) {
                for(Member method : methodResult.getMethods()) {
                    try {
                        List<Candidate> adaptedCandidates = adaptationStrategy.matchMethod(cutClass, returnTypeClass, paramTypes, (Method) method);
                        if(CollectionUtils.isNotEmpty(adaptedCandidates)) {
                            for(Candidate adaptedCandidate : adaptedCandidates) {
                                if(Logger.isDebugEnabled()) {
                                    Logger.debug(
                                            "Found METHOD ADAPTED match with " + adaptationStrategy.getClass().getName()
                                                    + ", " + method.toString() + ", Pos "
                                                    + Arrays.toString(
                                                    adaptedCandidate.getPositions()));
                                }

                                // DO NOT FILTER
                                // add
                                methodResult.getCandidates().add(adaptedCandidate);
                            }
                        }
                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // try with producer strategies
        if(constructor) {
            Logger.info("Trying to find producer");

            for(ProducerStrategy producerStrategy : producerStrategies) {
                try {
                    List<Candidate> adaptedCandidates = producerStrategy.match(cutClass, paramTypes);
                    if(CollectionUtils.isNotEmpty(adaptedCandidates)) {
                        for(Candidate adaptedCandidate : adaptedCandidates) {
                            if(Logger.isDebugEnabled()) {
                                Logger.debug("Found PRODUCER match with " + producerStrategy.getClass().getName()
                                        + ", " + adaptedCandidate.getMethod() + ", Pos "
                                        + Arrays.toString(
                                        adaptedCandidate.getPositions()));
                            }

                            Match newMatch = new Match(adaptedCandidate);
                            if (!methodResult.getMatches().contains(newMatch)) {
                                // add
                                methodResult.getMatches().add(newMatch);

                                // add
                                methodResult.getCandidates().add(adaptedCandidate);
                            } else {
                                if(Logger.isDebugEnabled()) {
                                    Logger.debug("Ignoring duplicate PRODUCER permutation match: "
                                            + adaptedCandidate.getMethod());
                                }
                            }
                        }
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        return methodResult.getCandidates();
    }

    private Class[] getParameterTypes(Member member) {
        if(member instanceof Method) {
            return ((Method) member).getParameterTypes();
        }

        if(member instanceof Constructor) {
            return ((Constructor<?>) member).getParameterTypes();
        }

        throw new IllegalArgumentException("member type unsupported " + member.getClass().getName());
    }

    public MethodResult findMethods(ClassUnderTest classUnderTest, Class<?> cutClass, Class<?> returnTypeClass, Class<?>[] paramTypes, boolean constructor) throws Exception {
        // method matches
        List<Candidate> candidates = new ArrayList<>();
        List<Match> matches = new ArrayList<>();

        // 1. step: find direct method matches
        List<Member> methods = null;
        //
        if (constructor) {
            // must be declared to include non-public constructors (i.e. in case
            // of static final classes with static methods)
            methods = new ArrayList<>(
                    Arrays.<Member> asList(cutClass.getDeclaredConstructors()));
        } else {
            // get all declared (included private)
            methods = new ArrayList<>(
                    Arrays.<Member> asList(cutClass.getDeclaredMethods()));

            // find all protected/public methods from super hierarchy NOT
            // overridden
            List<Member> superMethods = new LinkedList<>();

            // is cutClass included?
            Iterator<Class<?>> mIt = ClassUtils.hierarchy(cutClass).iterator();
            while (mIt.hasNext()) {
                Class<?> superClass = mIt.next();

                // we are only interested in protected, public members
                for (Method method : superClass.getDeclaredMethods()) {
                    // only add if NOT private, so we can actually access it in
                    // subclass
                    if (!Modifier.isPrivate(method.getModifiers())) {
                        // if(methods.contains(method))
                        superMethods.add(method);
                    }
                }
            }

            if (!superMethods.isEmpty()) {
                // remove all methods overridden in cut class
                for (Member superMethod : superMethods) {
                    List<Class<?>> superMethodParams = Arrays
                            .asList(((Method) superMethod).getParameterTypes());

                    boolean overridden = false;
                    // check if super methods are overridden in cut class
                    for (Member method : methods) {
                        List<Class<?>> methodParams = Arrays
                                .asList(((Method) method).getParameterTypes());

                        // same signature?
                        if (method.getName().equals(superMethod.getName())
                                && CollectionUtils.isEqualCollection(
                                methodParams, superMethodParams)) {
                            //
                            overridden = true;

                            break;
                        }
                    }

                    if (!overridden) {
                        methods.add(superMethod);
                    }
                }
            }
        }

//        methods.stream().forEach(m -> {
//            System.out.println(m.getDeclaringClass().getName() + " = > " + m);
//        });

        // pre-filter
        if (specFilter != null && CollectionUtils.isNotEmpty(methods)) {
            //
            methods = methods.stream()
                    .filter(m -> m instanceof Constructor
                            || specFilter.accept(classUnderTest, (Method) m))
                    .collect(Collectors.toList());
        }

////        // TODO remove
//        System.out.println("CUT " + cutClass);
//        methods.stream().forEach(m -> {
//            System.out.println(m.getDeclaringClass().getName() + " = > " + m);
//        });

        // find candidates (exact order!)
        for (Member method : methods) {
            // method input params
            Class<?>[] methodInputParams = getParameterTypes(method);

            // check if we have compatible parameter types (including
            // supertypes)
            boolean isAssignable = isAssignable(paramTypes, methodInputParams);

//            System.out.println("NIX da " + method);
//            System.out.println(Arrays.toString(paramTypes));
//            System.out.println(Arrays.toString(methodInputParams));

            if (isAssignable) {

                // check if compatible return type (method return type to
                // desired return type)
                boolean isReturnTypeAssignable = constructor ? true
                        : (isAssignable(returnTypeClass,
                        ((Method) method).getReturnType(), true));

                if (isReturnTypeAssignable) {
                    // determine param positions (simple increments)
                    int[] positions = new int[paramTypes.length];
                    for (int k = 0; k < positions.length; k++) {
                        positions[k] = k;
                    }

                    if(Logger.isDebugEnabled()) {
                        Logger.debug("Found direct match (param types in right order): "
                                + method.toString() + ", Pos "
                                + Arrays.toString(positions));
                    }

                    // candidate
                    Candidate candidate = new Candidate(method, positions);

                    // add
                    candidates.add(candidate);

                    // add
                    matches.add(new Match(candidate));
                }
            }
        }

//        if(methods.size() == 0) {
//            System.out.println("METHODS ARE NULL");
//        } else {
//            System.out.println("METHODS ARE " + methods.size());
//
//            methods.stream().forEach(m -> {
//                System.out.println(m.getName());
//            });
//        }

        // XXX Extension: to find best match see
        // MemberUtils.compareParameterTypes

        // 2. step: create permutations and check
        List<Integer> paramPositions = IntStream.range(0, paramTypes.length)
                .mapToObj(index -> new Integer(index))
                .collect(Collectors.toList());

        Iterator<List<Integer>> paramPermutations;
        if(paramPositions.size() > getMaxParamsLength()) {
            // too many combinations, simply use exact match in this case
            paramPermutations = new ArrayList<>(Collections.singletonList(paramPositions)).iterator();
        } else {
            paramPermutations = createPermutations(
                    paramPositions);
        }

        while (paramPermutations.hasNext()) {
            // List<Class<?>> permutatedParamClasses = permutations.next();

            List<Integer> permutatedParamPositions = paramPermutations.next();

            for (Member method : methods) {
                // method input params
                Class<?>[] methodInputParams = getParameterTypes(method);
                // check if we have compatible parameter types (including
                // supertypes)
                Class<?>[] permutatedParamClasses = (Class<?>[]) permutatedParamPositions
                        .stream().map(index -> (Class<?>) paramTypes[index])
                        .collect(Collectors.toList()).toArray(new Class<?>[0]);

                boolean isAssignable = isAssignable(permutatedParamClasses,
                        methodInputParams);

                int[] positions = new int[paramTypes.length];
                int j = 0;

                // determine position
                for (Integer paramTypePos : permutatedParamPositions) {
                    int position = paramPositions.indexOf(paramTypePos);

                    positions[j++] = position;
                }

                if (isAssignable) {
                    // check if compatible return type
                    boolean isReturnTypeAssignable = constructor || isAssignable(returnTypeClass,
                            ((Method) method).getReturnType(), true);

                    if (isReturnTypeAssignable) {
                        if(Logger.isDebugEnabled()) {
                            Logger.debug("Found indirect match (param types in different order): "
                                    + method.toString() + ", Pos "
                                    + Arrays.toString(
                                    positions)/*
                             * +
                             * ". Assignable types "
                             * + Arrays.stream(
                             * permutatedParamClasses
                             * ).map (c ->
                             * c.getName()).
                             * collect(
                             * Collectors.joining(
                             * ","))
                             */);
                        }

                        // candidate
                        Candidate candidate = new Candidate(method, positions);

                        Match newMatch = new Match(candidate);
                        if (!matches.contains(newMatch)) {
                            // add
                            matches.add(newMatch);

                            // add
                            candidates.add(candidate);
                        } else {
                            if(Logger.isDebugEnabled()) {
                                Logger.debug("Ignoring duplicate permutation match: "
                                        + method.toString());
                            }
                        }
                    }
                }

                // remaining adaptation strategies
                // try with adaptation strategies
                if(method instanceof Method) {
                    for(de.uni_mannheim.swt.lasso.runner.permutator.strategy.method.AdaptationStrategy adaptationStrategy : adaptationStrategies) {
                        try {
                            List<Candidate> adaptedCandidates = adaptationStrategy.match(cutClass, returnTypeClass, paramTypes, (Method) method, positions);
                            if(CollectionUtils.isNotEmpty(adaptedCandidates)) {
                                for(Candidate adaptedCandidate : adaptedCandidates) {
                                    if(Logger.isDebugEnabled()) {
                                        Logger.debug("Found ADAPTED match with " + adaptationStrategy.getClass().getName()
                                                + ", " + method.toString() + ", Pos "
                                                + Arrays.toString(
                                                positions));
                                    }

                                    Match newMatch = new Match(adaptedCandidate);
                                    if (!matches.contains(newMatch)) {
                                        // add
                                        matches.add(newMatch);

                                        // add
                                        candidates.add(adaptedCandidate);
                                    } else {
                                        if(Logger.isDebugEnabled()) {
                                            Logger.debug("Ignoring duplicate permutation match: "
                                                    + method.toString());
                                        }
                                    }
                                }
                            }
                        } catch(Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        MethodResult methodResult = new MethodResult();
        methodResult.setCandidates(candidates);
        methodResult.setMatches(matches);
        methodResult.setMethods(methods);

        return methodResult;
    }

    /**
     * Create permutations for given list (e.g. permutations for all param
     * combinations)
     *
     * @param list
     * @return iterator over all possible permutations
     *
     */
    public static <T> Iterator<List<T>> createPermutations(List<T> list) {
        Iterator<List<T>> permutations = new PermutationIterator<T>(list);

        return permutations;
    }

    /**
     *
     * @param candidates
     * @return true if at least one permutation exists that has static methods
     */
    public static boolean hasStaticMethods(List<List<Candidate>> candidates) {
        //
        for (List<Candidate> cList : candidates) {
            Iterator<Candidate> it = cList.iterator();
            boolean isStatic = true;
            while (it.hasNext()) {
                if (!Modifier.isStatic(it.next().getMethod().getModifiers())) {
                    isStatic = false;
                    break;
                }
            }

            if(isStatic) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convert all primitive wrappers to primitives in order to allow casting
     *
     * @param params
     * @return
     */
    protected static Class<?>[] wrappersToPrimitives(Class<?>[] params) {
        return TypeUtils.wrappersToPrimitives(params);
    }

    /**
     * Check if the params arrays are assignable to each other (including
     * casting of primitives)
     *
     * @param classArray
     * @param toClassArray
     * @return
     */
    public static boolean isAssignable(Class<?>[] classArray,
                                       Class<?>[] toClassArray) {
        return TypeUtils.isAssignable(classArray, toClassArray);
    }

    /**
     * Check if first class is assignable to the second (including casting of
     * primitives)
     *
     * @param clazz
     * @param toClazz
     * @param isReturn
     * @return
     */
    public static boolean isAssignable(Class<?> clazz, Class<?> toClazz, boolean isReturn) {
        // FIXME decide if void is useful or not
        return TypeUtils.isAssignable(clazz, toClazz, isReturn);
    }

    public int getMaxParamsLength() {
        return maxParamsLength;
    }

    public void setMaxParamsLength(int maxParamsLength) {
        this.maxParamsLength = maxParamsLength;
    }
}
