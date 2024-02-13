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
package de.uni_mannheim.swt.lasso.engine.action.test.support.surefire;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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
import java.util.Iterator;

/**
 * XML surefire test result parser based on Stax event pulling.
 * 
 * @author Marcus Kessel
 *
 */
public class SurefireParser {

    private static final String TESTSUITE_ELEMENT = "testsuite";

    private InputStream inputStream;
    private boolean skipFailureMsg;

    public SurefireParser(InputStream inputStream, boolean skipFailureMsg) {
        this.inputStream = inputStream;
        this.skipFailureMsg = skipFailureMsg;
    }

    private XMLEventReader initReader(Reader reader) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader eventReader = factory.createXMLEventReader(reader);
        return eventReader;
    }

    public SurefireReport parse() throws XMLStreamException {
        XMLEventReader eventReader = initReader(
                new InputStreamReader(inputStream));

        try {
            SurefireReport surefire = null;
            // current test case
            SurefireTestCase testCase = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            TESTSUITE_ELEMENT)) {
                        // init
                        surefire = new SurefireReport();
                        //
                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "name")) {
                                surefire.setName(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "tests")) {
                                surefire.setTests(
                                        Integer.parseInt(attr.getValue()));
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "failures")) {
                                surefire.setFailures(
                                        Integer.parseInt(attr.getValue()));
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "skipped")) {
                                surefire.setSkipped(
                                        Integer.parseInt(attr.getValue()));
                            }

                            // errors
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "errors")) {
                                // handle errors
                                surefire.setErrors(
                                        Integer.parseInt(attr.getValue()));
                            }

                            // time
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "time")) {
                                // handle time
                                try {
                                    surefire.setTime(Double
                                            .parseDouble(attr.getValue()));
                                } catch (NumberFormatException e) {
                                    //
                                }
                            }
                        }
                    }

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "testcase")) {
                        //
                        testCase = new SurefireTestCase();

                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();

                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(),
                                    "classname")) {
                                testCase.setClassName(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "name")) {
                                testCase.setName(attr.getValue());
                            }
                            
                            // time
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "time")) {
                                // handle time
                                try {
                                    testCase.setTime(Double
                                            .parseDouble(attr.getValue()));
                                } catch (NumberFormatException e) {
                                    //
                                }
                            }
                        }

                        // add once basic attributes have been set
                        surefire.addTestCase(testCase);
                    }

                    // FAILURE or ERROR (treated similarly)
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(), "failure")
                            || StringUtils.equalsIgnoreCase(
                                    startElement.getName().getLocalPart(),
                                    "error")) {
                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();

                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "type")) {
                                testCase.setType(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "message")) {
                                if (!skipFailureMsg) {
                                    // exception message (HINT: trace is
                                    // Characters within failure element)
                                    testCase.setMessage(attr.getValue());
                                }
                            }
                        }

                        if(StringUtils.equalsIgnoreCase(
                                startElement.getName().getLocalPart(),
                                "error")) {
                            testCase.setResult(2); // error
                        }
                        if(StringUtils.equalsIgnoreCase(
                                startElement.getName().getLocalPart(),
                                "failure")) {
                            testCase.setResult(1); // failure
                        }

                        // in case we stop it due to
                        // 'stopOnSuccessfulPermutation', @see
                        // SearchManager#STOP_ON_SUCCESSFUL_PERMUTATION
                        if (StringUtils.equals(
                                testCase.getType(),
                                "org.junit.runner.notification.StoppedByUserException")) {
                            //
                            surefire.setStoppedByUserException(true);
                        }
                    }
                }
            }

            return surefire;
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
