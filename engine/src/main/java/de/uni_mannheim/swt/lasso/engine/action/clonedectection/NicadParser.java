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
package de.uni_mannheim.swt.lasso.engine.action.clonedectection;

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
import java.util.*;

/**
 * Nicad XML parser.
 *
 * @author Marcus Kessel
 */
public class NicadParser {

    private InputStream inputStream;

    public NicadParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private XMLEventReader initReader(Reader reader) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader eventReader = factory.createXMLEventReader(reader);
        return eventReader;
    }

    public Map<Integer, CloneClass> parse() throws XMLStreamException {
        XMLEventReader eventReader = initReader(
                new InputStreamReader(inputStream));

        try {
            Map<Integer, CloneClass> map = new HashMap<>();

            int currentClassId = -1;
            int similarity = -1;
            int nlines = -1;
            int nclones = -1;

            CloneClass currentCloneClass = null;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();

                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "class")) {
                        //
                        currentCloneClass = new CloneClass();

                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "classid")) {
                                currentClassId = Integer.parseInt(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "nlines")) {
                                nlines = Integer.parseInt(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "nclones")) {
                                nclones = Integer.parseInt(attr.getValue());
                            }
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "similarity")) {
                                similarity = Integer.parseInt(attr.getValue());
                            }
                        }

                        currentCloneClass.setClassid(currentClassId);
                        currentCloneClass.setNlines(nlines);
                        currentCloneClass.setNclones(nclones);
                        currentCloneClass.setSimilarity(similarity);
                        currentCloneClass.setImplementations(new LinkedList<>());

                        map.put(currentClassId, currentCloneClass);
                    }

                    // source element
                    if (StringUtils.equalsIgnoreCase(
                            startElement.getName().getLocalPart(),
                            "source")) {
                        //
                        Iterator it = startElement.getAttributes();
                        while (it.hasNext()) {
                            Attribute attr = (Attribute) it.next();
                            if (StringUtils.equalsIgnoreCase(
                                    attr.getName().getLocalPart(), "file")) {
                                // add to map
                                map.get(currentClassId).getImplementations().add(StringUtils.substringBetween(attr.getValue(), "modules/", "/src"));
                            }
                        }
                    }
                }
            }

            return map;
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

    //    public static class Clones {
//        private int npcs;
//        private int npairs;
//        private int ncompares;
//        private long cputime;
//
//        private List<CloneClass> classes = new LinkedList<>();
//
//        public int getNpcs() {
//            return npcs;
//        }
//
//        public void setNpcs(int npcs) {
//            this.npcs = npcs;
//        }
//
//        public int getNpairs() {
//            return npairs;
//        }
//
//        public void setNpairs(int npairs) {
//            this.npairs = npairs;
//        }
//
//        public int getNcompares() {
//            return ncompares;
//        }
//
//        public void setNcompares(int ncompares) {
//            this.ncompares = ncompares;
//        }
//
//        public long getCputime() {
//            return cputime;
//        }
//
//        public void setCputime(long cputime) {
//            this.cputime = cputime;
//        }
//
//        public List<CloneClass> getClasses() {
//            return classes;
//        }
//
//        public void setClasses(List<CloneClass> classes) {
//            this.classes = classes;
//        }
//    }
//
    public static class CloneClass {

        private int classid;
        private int nclones;
        private int nlines;
        private int similarity;
        private List<String> implementations;

        public int getClassid() {
            return classid;
        }

        public void setClassid(int classid) {
            this.classid = classid;
        }

        public int getNclones() {
            return nclones;
        }

        public void setNclones(int nclones) {
            this.nclones = nclones;
        }

        public int getNlines() {
            return nlines;
        }

        public void setNlines(int nlines) {
            this.nlines = nlines;
        }

        public int getSimilarity() {
            return similarity;
        }

        public void setSimilarity(int similarity) {
            this.similarity = similarity;
        }

        public List<String> getImplementations() {
            return implementations;
        }

        public void setImplementations(List<String> implementations) {
            this.implementations = implementations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CloneClass that = (CloneClass) o;
            return classid == that.classid;
        }

        @Override
        public int hashCode() {
            return Objects.hash(classid);
        }
    }
}
