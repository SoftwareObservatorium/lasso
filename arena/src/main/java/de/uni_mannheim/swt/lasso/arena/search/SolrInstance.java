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
package de.uni_mannheim.swt.lasso.arena.search;

/**
 * Model describing a Solr instance.
 *
 * @author Marcus Kessel
 */
// FIXME SECURITY load passwords from somewhere else
public class SolrInstance {

    private String user;
    private String pass;
    private String url;

    private String name;

    public SolrInstance(String name, String user, String pass, String url) {
        this.name = name;
        this.user = user;
        this.pass = pass;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getUrl() {
        return url;
    }

    public static SolrInstance mavenCentral2020() {
        return new SolrInstance("mavenCentral2020", "", "", "http://swt105.informatik.uni-mannheim.de:8986/solr/mavencentral2020/");
    }

    public static SolrInstance secorpora2021() {
        return new SolrInstance("secorpora2021", "", "", "http://swt105.informatik.uni-mannheim.de:8986/solr/mtp-datasources-test/");
    }

    public static SolrInstance mavenCentral2017() {
        return new SolrInstance("mavenCentral2017", "", "", "http://swt100.informatik.uni-mannheim.de:8983/solr/candidates/");
    }

    public static SolrInstance mavenCentral2023() {
        return new SolrInstance("mavencentral2023", "", "", "http://lassohp10.informatik.uni-mannheim.de:8983/solr/mavencentral2023/");
    }

    public static SolrInstance secorpora2022() {
        return new SolrInstance("secorpora2022", "", "", "http://lassohp10.informatik.uni-mannheim.de:8983/solr/secorpora2022/");
    }

    public static SolrInstance multipleBenchmark23() {
        return new SolrInstance("multipleBenchmark23", "", "", "http://lassohp10.informatik.uni-mannheim.de:8983/solr/multiple-benchmark-23/");
    }

    public static SolrInstance gitimport23() {
        return new SolrInstance("gitimport23", "", "", "http://lassohp10.informatik.uni-mannheim.de:8983/solr/gitimport-23/");
    }
}
