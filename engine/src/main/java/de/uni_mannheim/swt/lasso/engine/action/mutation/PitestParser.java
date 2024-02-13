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
package de.uni_mannheim.swt.lasso.engine.action.mutation;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;

import java.util.LinkedList;
import java.util.List;

/**
 * XML pitest report parser based on Stax event pulling.
 *
 * @author Marcus Kessel
 */
public class PitestParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(PitestParser.class);

    private static final String MUTATIONS_ELEMENT = "mutations";

    private InputStream inputStream;

    public PitestParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private XMLEventReader initReader(Reader reader) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader eventReader = factory.createXMLEventReader(reader);
        return eventReader;
    }

    public List<PitMutation> parseMutations() throws XMLStreamException {
        XMLEventReader eventReader = initReader(
                new InputStreamReader(inputStream));

        List<PitMutation> mutations = new LinkedList<>();
        PitMutation mutation = new PitMutation();

        try {
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            MUTATIONS_ELEMENT)) {
                        // empty
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutation")) {
                        //
                        mutation = new PitMutation();
                        mutations.add(mutation);

                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();

                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "detected")) {
                                //
                                boolean detectedBool = Boolean.parseBoolean(attr.getValue());
                                mutation.setDetected(detectedBool);
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "status")) {
                                //
                                mutation.setStatus(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "numberOfTestsRun")) {
                                //
                                mutation.setNumberOfTestRuns(Integer.parseInt(attr.getValue()));
                            }
                        }
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "sourceFile")) {
                        mutation.setSourceFile(eventReader.getElementText());
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutatedClass")) {
                        mutation.setMutatedClass(eventReader.getElementText());
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutatedMethod")) {
                        mutation.setMutatedMethod(eventReader.getElementText());
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "methodDescription")) {
                        mutation.setMethodDescription(eventReader.getElementText());
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "lineNumber")) {
                        mutation.setLineNumber(NumberUtils.createInteger(eventReader.getElementText()));
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutator")) {
                        mutation.setMutator(eventReader.getElementText());
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "index")) {
                        mutation.setIndex(NumberUtils.createInteger(eventReader.getElementText()));
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "block")) {
                        mutation.setBlock(NumberUtils.createInteger(eventReader.getElementText()));
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "killingTests")) {
                        String text = eventReader.getElementText();
                        mutation.setKillingTests(text);

                        if(StringUtils.isNotBlank(text)) {
                            mutation.setNoOfKillingTests(StringUtils.countMatches(text, '|') + 1);
                        }
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "succeedingTests")) {
                        String text = eventReader.getElementText();
                        mutation.setSucceedingTests(text);

                        if(StringUtils.isNotBlank(text)) {
                            mutation.setNoOfSucceedingTests(StringUtils.countMatches(text, '|') + 1);
                        }
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "description")) {
                        mutation.setDescription(eventReader.getElementText());
                    }
                }
            }

            return mutations;
        } finally {
            //
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (Throwable e) {
                    //
                }
            }

            //
            IOUtils.closeQuietly(inputStream);
        }
    }

    public PitestReport parse() throws XMLStreamException {
        XMLEventReader eventReader = initReader(
                new InputStreamReader(inputStream));

        try {
            int total = 0;
            int noCoverage = 0;
            int killed = 0;
            int detected = 0;
            int survived = 0;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            MUTATIONS_ELEMENT)) {
                        // empty
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutation")) {
                        //
                        total++;

                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();

                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "detected")) {
                                //
                                boolean detectedBool = Boolean.parseBoolean(attr.getValue());
                                if (detectedBool) {
                                    detected++;
                                }
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "status")) {
                                //
                                if (StringUtils.equalsIgnoreCase(attr.getValue(), "KILLED")) {
                                    killed++;
                                } else if (StringUtils.equalsIgnoreCase(attr.getValue(), "SURVIVED")) {
                                    survived++;
                                } else if (StringUtils.equalsIgnoreCase(attr.getValue(), "NO_COVERAGE")) {
                                    noCoverage++;
                                } else {
                                    if (LOG.isWarnEnabled()) {
                                        LOG.warn("Found unknown status value '{}'", attr.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PitestReport pitestReport = new PitestReport();
            pitestReport.setTotal(total);
            pitestReport.setKilled(killed); // seems to be misleading
            pitestReport.setSurvived(survived);
            pitestReport.setNoCoverage(noCoverage);
            pitestReport.setDetected(detected);
            pitestReport.setCoverage((double) detected / total);

            return pitestReport;
        } finally {
            //
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (Throwable e) {
                    //
                }
            }

            //
            IOUtils.closeQuietly(inputStream);
        }
    }

    public MethodPitestReport parse(List<String> methodSignatures) throws XMLStreamException {
        XMLEventReader eventReader = initReader(
                new InputStreamReader(inputStream));

        try {
            int total = 0;
            int noCoverage = 0;
            int killed = 0;
            int detected = 0;
            int survived = 0;

            String mutatedClass = null;
            String mutatedMethod = null;
            String methodDescription = null;

            int localTotal = 0;
            int localNoCoverage = 0;
            int localKilled = 0;
            int localDetected = 0;
            int localSurvived = 0;

            String line = null;
            String index = null;
            String block = null;
            // e.g, org.apache.commons.codec.binary.Base64Test.test_3(org.apache.commons.codec.binary.Base64Test)
            String killingTest = null;
            String mutator = null;

            MultiValuedMap<String, String> killersMap = new ArrayListValuedHashMap<>();

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            MUTATIONS_ELEMENT)) {
                        // empty
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutatedClass")) {
                        mutatedClass = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutatedMethod")) {
                        mutatedMethod = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "methodDescription")) {
                        methodDescription = eventReader.getElementText();
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutation")) {
                        //
                        localTotal++;

                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();

                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "detected")) {
                                //
                                boolean detectedBool = Boolean.parseBoolean(attr.getValue());
                                if (detectedBool) {
                                    localDetected++;
                                }
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "status")) {
                                //
                                if (StringUtils.equalsIgnoreCase(attr.getValue(), "KILLED")) {
                                    localKilled++;
                                } else if (StringUtils.equalsIgnoreCase(attr.getValue(), "SURVIVED")) {
                                    localSurvived++;
                                } else if (StringUtils.equalsIgnoreCase(attr.getValue(), "NO_COVERAGE")) {
                                    localNoCoverage++;
                                } else {
                                    if (LOG.isWarnEnabled()) {
                                        LOG.warn("Found unknown status value '{}'", attr.getValue());
                                    }
                                }
                            }
                        }
                    }

                    // which test is killing what mutant
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "lineNumber")) {
                        line = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "index")) {
                        index = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "block")) {
                        block = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "killingTest")) {
                        killingTest = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "killingTests")) {
                        killingTest = eventReader.getElementText();
                    }
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "mutator")) {
                        mutator = eventReader.getElementText();
                    }
                }

                if(event.isEndElement() && StringUtils.equalsIgnoreCase(event.asEndElement().getName().getLocalPart(), "mutation")) {
                    String signature1 = StringUtils.replace(mutatedClass, ".", "/") + "." + mutatedMethod + methodDescription;
                    String signature2 = mutatedClass + "." + mutatedMethod + methodDescription;

                    if(methodSignatures.contains(signature1) || methodSignatures.contains(signature2)) {
                        LOG.debug("Signature found {}", signature1);

                        total += localTotal;
                        killed += localKilled;
                        survived += localSurvived;
                        noCoverage += localNoCoverage;
                        detected += localDetected;

                        // keep statistic of who killed what
                        if(StringUtils.isNotBlank(killingTest)) {
                            String mutantId = String.format("%s::%s_%s_%s_%s", total, mutator, line, index, block);

                            // multiple tests?
                            String[] tests;
                            if(StringUtils.contains(killingTest, '|')) {
                                tests = StringUtils.split(killingTest, '|');
                            } else {
                                tests = new String[]{killingTest};
                            }

                            // add tests
                            Arrays.stream(tests).map(s -> StringUtils.substringBefore(s, "(")).forEach(s -> killersMap.put(mutantId, s));
                        }
                    } else {
                        LOG.debug("Signature not found {} in {}", signature1, Arrays.toString(methodSignatures.toArray()));
                    }

                    mutatedClass = null;
                    mutatedMethod = null;
                    methodDescription = null;

                    localTotal = 0;
                    localNoCoverage = 0;
                    localKilled = 0;
                    localDetected = 0;
                    localSurvived = 0;

                    line = null;
                    index = null;
                    block = null;
                    killingTest = null;
                    mutator = null;
                }
            }

            MethodPitestReport pitestReport = new MethodPitestReport();
            pitestReport.setTotal(total);
            pitestReport.setKilled(killed); // seems to be misleading. detected == killed
            pitestReport.setSurvived(survived);
            pitestReport.setNoCoverage(noCoverage);
            pitestReport.setDetected(detected);
            if(total == 0) { // NaN
                pitestReport.setCoverage(0);
            } else {
                pitestReport.setCoverage((double) detected / total);
            }
            pitestReport.setKillers(killersMap);

            return pitestReport;
        } finally {
            //
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (Throwable e) {
                    //
                }
            }

            //
            IOUtils.closeQuietly(inputStream);
        }
    }


}
