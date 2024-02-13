///
/// LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
/// Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
///
/// This file is part of LASSO.
///
/// LASSO is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// LASSO is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
///

export enum Templates {
    themeDefault = 'vs',
    default = `dataSource 'mavenCentral2020'
// interface of a Stack in LQL notation
def interfaceSpec = """Stack {
    push(java.lang.Object)->java.lang.Object
    pop()->java.lang.Object
    peek()->java.lang.Object
    size()->int}"""
study(name: 'Stack-TestDrivenSelection') {
    action(name: 'select', type: 'Select') {
        abstraction('Stack') { // interface-driven code search
            queryForClasses interfaceSpec
            rows = 10
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
            // optionally, we do not want it to be a collection
            //excludeSuperClass("java.util.Collection")
            // non empty classes, i.e having complexity > 1
            filter 'm_static_complexity_td:[2 TO *]' // FIXME m_static_complexity_td instead of complexity
        }
    }

    action(name: 'filter', type: 'ArenaExecute') { // filter by tests
        specification = interfaceSpec // FIXME was missing
        sequences = [
                // parameterised sheet (SSN) with default input parameter values
                // expected values are given in first row (oracle)
                'pushPop': sheet(p1: 'Stack', p2: "hello world") {
                    row '', 'create', '?p1'
                    row '?p2', 'push', 'A1', '?p2'
                    row '?p2', 'peek', 'A1'
                    row 1, 'size', 'A1'
                    row '?p2', 'pop', 'A1'
                    row 0, 'size', 'A1'
                }
        ]
        maxAdaptations = 1 // how many adaptations to try

        dependsOn 'select'
        includeAbstractions 'Stack'
        profile('myTdsProfile') {
            scope('class') { type = 'class' }
            environment('java8') {
                image = 'maven:3.5.4-jdk-8-alpine'
            }
        }

        whenAbstractionsReady() {
            def stack = abstractions['Stack']
            def stackSrm = srm(abstraction: stack)
            // define oracle based on expected responses in sequences
            def expectedBehaviour = toOracle(srm(abstraction: stack).sequences)
            // alternatively, use any system as a (pseudo) oracle
            def referenceImpl = toOracle(srm(abstraction: stack).systems.first())
            // returns a filtered SRM
            def matchesSrm = srm(abstraction: stack)
                    .systems // select all systems
                    .equalTo(expectedBehaviour) // functionally equivalent

            // iterate over sub-SRM
            matchesSrm.systems.each { s ->
                log("Matched class \${s.id}, \${s.packageName}.\${s.name}")
            }
            // export to individual CSV file (if desired)
            export(matchesSrm, 'stacks.csv')
            // continue pipeline with matched systems only
            stack.systems = matchesSrm.systems
        }
    }
}
    `,
    lql = `Stack{
    Stack()
    push(java.lang.Object)->java.lang.Object
    pop()->java.lang.Object
    peek()->java.lang.Object
    size()->int
}`
     ,
  }
  