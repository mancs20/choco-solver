/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 15/10/2019
 */
module org.chocosolver.solver {
    // exports in alphabetical order
    exports org.chocosolver.memory;
    exports org.chocosolver.memory.trailing;
    exports org.chocosolver.memory.trailing.trail;
    exports org.chocosolver.memory.trailing.trail.flatten;
    exports org.chocosolver.memory.trailing.trail.chunck;
    exports org.chocosolver.memory.structure;
    
    exports org.chocosolver.sat;
    exports org.chocosolver.solver;
    exports org.chocosolver.solver.constraints;
    exports org.chocosolver.solver.constraints.binary;
    exports org.chocosolver.solver.constraints.binary.element;
    exports org.chocosolver.solver.constraints.extension;
    exports org.chocosolver.solver.constraints.extension.binary;
    exports org.chocosolver.solver.constraints.extension.hybrid;
    exports org.chocosolver.solver.constraints.extension.nary;
    exports org.chocosolver.solver.constraints.nary;
    exports org.chocosolver.solver.constraints.nary.alldifferent;
    exports org.chocosolver.solver.constraints.nary.alldifferent.algo;
    exports org.chocosolver.solver.constraints.nary.alldifferent.conditions;
    exports org.chocosolver.solver.constraints.nary.among;
    exports org.chocosolver.solver.constraints.nary.automata;
    exports org.chocosolver.solver.constraints.nary.automata.FA;
    //exports org.chocosolver.solver.constraints.nary.automata.FA.utils;
    //exports org.chocosolver.solver.constraints.nary.automata.structure;
    //exports org.chocosolver.solver.constraints.nary.automata.structure.multicostregular;
    //exports org.chocosolver.solver.constraints.nary.automata.structure.costregular;
    //exports org.chocosolver.solver.constraints.nary.automata.structure.regular;
    exports org.chocosolver.solver.constraints.nary.binPacking;
    exports org.chocosolver.solver.constraints.nary.channeling;
    exports org.chocosolver.solver.constraints.nary.circuit;
    exports org.chocosolver.solver.constraints.nary.cnf;
    exports org.chocosolver.solver.constraints.nary.count;
    exports org.chocosolver.solver.constraints.nary.cumulative;
    exports org.chocosolver.solver.constraints.nary.element;
    exports org.chocosolver.solver.constraints.nary.globalcardinality;
    exports org.chocosolver.solver.constraints.nary.lex;
    exports org.chocosolver.solver.constraints.nary.min_max;
    exports org.chocosolver.solver.constraints.nary.nvalue;
    //exports org.chocosolver.solver.constraints.nary.nvalue.amnv.graph;
    //exports org.chocosolver.solver.constraints.nary.nvalue.amnv.rules;
    //exports org.chocosolver.solver.constraints.nary.nvalue.amnv.differences;
    //exports org.chocosolver.solver.constraints.nary.nvalue.amnv.mis;
    exports org.chocosolver.solver.constraints.nary.tree;
    exports org.chocosolver.solver.constraints.nary.sat;
    exports org.chocosolver.solver.constraints.nary.sort;
    exports org.chocosolver.solver.constraints.nary.sum;
    exports org.chocosolver.solver.constraints.real;
    exports org.chocosolver.solver.constraints.reification;
    exports org.chocosolver.solver.constraints.set;
    exports org.chocosolver.solver.constraints.ternary;
    exports org.chocosolver.solver.constraints.unary;
    exports org.chocosolver.solver.exception;
    exports org.chocosolver.solver.expression.continuous.arithmetic;
    exports org.chocosolver.solver.expression.continuous.relational;
    exports org.chocosolver.solver.expression.discrete.arithmetic;
    exports org.chocosolver.solver.expression.discrete.logical;
    exports org.chocosolver.solver.expression.discrete.relational;
    exports org.chocosolver.solver.objective;
    exports org.chocosolver.solver.propagation;
    exports org.chocosolver.solver.search;
    exports org.chocosolver.solver.search.limits;
    exports org.chocosolver.solver.search.loop;
    exports org.chocosolver.solver.search.loop.learn;
    exports org.chocosolver.solver.search.loop.lns;
    exports org.chocosolver.solver.search.loop.lns.neighbors;
    exports org.chocosolver.solver.search.loop.move;
    exports org.chocosolver.solver.search.loop.monitors;
    exports org.chocosolver.solver.search.loop.propagate;
    exports org.chocosolver.solver.search.measure;
    exports org.chocosolver.solver.search.restart;
    exports org.chocosolver.solver.search.strategy;
    exports org.chocosolver.solver.search.strategy.assignments;
    exports org.chocosolver.solver.search.strategy.decision;
    exports org.chocosolver.solver.search.strategy.selectors.values;
    exports org.chocosolver.solver.search.strategy.selectors.variables;
    exports org.chocosolver.solver.search.strategy.selectors.values.graph.edge;
    exports org.chocosolver.solver.search.strategy.selectors.values.graph.node;
    exports org.chocosolver.solver.search.strategy.selectors.values.graph.priority;
    exports org.chocosolver.solver.search.strategy.strategy;
    exports org.chocosolver.solver.variables;
    exports org.chocosolver.solver.variables.delta;
    //exports org.chocosolver.solver.variables.delta.monitor;
    exports org.chocosolver.solver.variables.events;
    exports org.chocosolver.solver.variables.impl;
    //exports org.chocosolver.solver.variables.impl.scheduler;
    exports org.chocosolver.solver.variables.view;
    exports org.chocosolver.solver.trace;

    exports org.chocosolver.util;
    exports org.chocosolver.util.criteria;
    exports org.chocosolver.util.graphOperations.connectivity;
    exports org.chocosolver.util.graphOperations.dominance;
    exports org.chocosolver.util.iterators;
    exports org.chocosolver.util.logger;
    exports org.chocosolver.util.objects;
    exports org.chocosolver.util.objects.graphs;
    exports org.chocosolver.util.objects.queues;
    exports org.chocosolver.util.objects.setDataStructures;
    exports org.chocosolver.util.objects.setDataStructures.bitset;
    exports org.chocosolver.util.objects.setDataStructures.iterable;
    exports org.chocosolver.util.objects.setDataStructures.linkedlist;
    exports org.chocosolver.util.objects.setDataStructures.swapList;
    exports org.chocosolver.util.objects.setDataStructures.constant;
    exports org.chocosolver.util.objects.tree;
    exports org.chocosolver.util.procedure;
    exports org.chocosolver.util.sort;
    exports org.chocosolver.util.tools;
    exports org.chocosolver.util.bandit;

    requires trove4j;
    requires org.jgrapht.core;
    requires automaton;
    requires sizeof;

    opens org.chocosolver.memory to org.testng, sizeof;
    opens org.chocosolver.solver.constraints.unary to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.set to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.binary to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.binary.element to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.extension to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.extension.binary to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.extension.hybrid to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.extension.nary to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.reification to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.among to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.binPacking to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.circuit to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.count to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.element to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.lex to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.channeling to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.real to org.chocosolver.parsers, org.testng, sizeof;
    opens org.chocosolver.solver.constraints.nary.sum to sizeof;
    exports org.chocosolver.solver.constraints.nary.flow;
    opens org.chocosolver.solver.constraints.nary.flow to org.chocosolver.parsers, sizeof;
    exports org.chocosolver.solver.constraints.nary.knapsack;
    opens org.chocosolver.solver.constraints.nary.knapsack to org.chocosolver.parsers, sizeof;
    exports org.chocosolver.solver.constraints.nary.knapsack.structure;
    opens org.chocosolver.solver.constraints.nary.knapsack.structure to org.chocosolver.parsers, sizeof;
    opens org.chocosolver.solver.constraints.nary.alldifferent to sizeof;
    opens org.chocosolver.solver.constraints.nary.alldifferent.algo to sizeof;

    // to org.ehcache
    opens org.chocosolver.solver to sizeof;
    opens org.chocosolver.solver.variables.impl to sizeof;
    opens org.chocosolver.solver.variables.delta to sizeof;
    opens org.chocosolver.solver.variables.delta.monitor to sizeof;
    opens org.chocosolver.solver.objective to sizeof;
    opens org.chocosolver.memory.trailing to sizeof;
    opens org.chocosolver.memory.trailing.trail.flatten to sizeof;
    opens org.chocosolver.solver.search.restart to sizeof;
    opens org.chocosolver.solver.propagation to sizeof;
    opens org.chocosolver.util to sizeof;
    opens org.chocosolver.util.graphOperations.connectivity to sizeof;
    opens org.chocosolver.util.graphOperations.dominance to sizeof;
    opens org.chocosolver.util.objects.graphs to sizeof;
    opens org.chocosolver.util.iterators to sizeof;
    opens org.chocosolver.util.logger to sizeof;
    opens org.chocosolver.util.objects to sizeof;
    opens org.chocosolver.util.objects.queues to sizeof;
    opens org.chocosolver.util.objects.setDataStructures to sizeof;
    opens org.chocosolver.util.objects.setDataStructures.bitset to sizeof;
    opens org.chocosolver.util.objects.setDataStructures.constant to sizeof;
    opens org.chocosolver.util.objects.setDataStructures.linkedlist to sizeof;
    opens org.chocosolver.solver.search.loop.monitors to sizeof;
    opens org.chocosolver.solver.search.strategy.decision to sizeof;
    opens org.chocosolver.solver.search.strategy.strategy to sizeof;
    opens org.chocosolver.solver.search.strategy.selectors.variables to sizeof;
    opens org.chocosolver.solver.search.measure to sizeof;
    opens org.chocosolver.solver.search.loop.move to sizeof;
    opens org.chocosolver.memory.structure to sizeof;
    opens org.chocosolver.solver.search.loop to sizeof;
}