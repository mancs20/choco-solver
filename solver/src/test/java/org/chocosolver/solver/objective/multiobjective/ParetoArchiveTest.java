/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests the contents and structural invariants of {@link ParetoArchive}.
 * Test candidates respect the archive contract: they are not dominated by an
 * objective vector already in the archive.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public class ParetoArchiveTest {

    @Test(groups = "1s", timeOut = 60000)
    public void testEmptyArchiveIsConsistent() {
        TestArchive testArchive = createTestArchive();

        assertConsistent(testArchive);
        Assert.assertTrue(testArchive.archive.getParetoFrontSolutions().isEmpty());
        Assert.assertTrue(testArchive.archive.getParetoFrontValues().isEmpty());
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 0);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testDominatingCandidateRemovesOnlyDominatedSolutions() {
        TestArchive testArchive = createTestArchive();
        Solution dominated = solution(testArchive, 2, 2);
        Solution firstIncomparable = solution(testArchive, 5, 1);
        Solution secondIncomparable = solution(testArchive, 1, 5);

        Assert.assertTrue(testArchive.archive.addSolution(dominated));
        Assert.assertTrue(testArchive.archive.addSolution(firstIncomparable));
        Assert.assertTrue(testArchive.archive.addSolution(secondIncomparable));
        assertConsistent(testArchive);

        Solution dominating = solution(testArchive, 3, 3);
        Assert.assertTrue(testArchive.archive.addSolution(dominating));

        assertConsistent(testArchive);
        assertPoints(testArchive.archive, Set.of("5,1", "1,5", "3,3"));
        Assert.assertFalse(testArchive.archive.getParetoFrontSolutions().contains(dominated));
        Assert.assertTrue(testArchive.archive.getParetoFrontSolutions().contains(firstIncomparable));
        Assert.assertTrue(testArchive.archive.getParetoFrontSolutions().contains(secondIncomparable));
        Assert.assertTrue(testArchive.archive.getParetoFrontSolutions().contains(dominating));
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testEquivalentObjectiveVectorIsNotAdded() {
        TestArchive testArchive = createTestArchive();
        Solution first = solution(testArchive, 2, 3);
        Solution equivalent = solution(testArchive, 2, 3);

        Assert.assertTrue(testArchive.archive.addSolution(first));
        Assert.assertFalse(testArchive.archive.addSolution(equivalent));

        assertConsistent(testArchive);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(), List.of(first));
        assertPoints(testArchive.archive, Set.of("2,3"));
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testNonCertifiedSolutionIsAppended() {
        TestArchive testArchive = createTestArchive();
        Solution certified = solution(testArchive, 5, 1);
        Solution firstIntermediate = solution(testArchive, 3, 3);
        Solution added = solution(testArchive, 1, 5);
        testArchive.archive.addCertified(certified);
        testArchive.archive.addSolution(firstIntermediate);

        testArchive.archive.addSolution(added);

        Assert.assertSame(lastSolution(testArchive.archive), added);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 1);
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testCertifiedSolutionIsInsertedAtEndOfCertifiedPrefix() {
        TestArchive testArchive = createTestArchive();
        Solution firstIntermediate = solution(testArchive, 5, 1);
        Solution secondIntermediate = solution(testArchive, 1, 5);
        Solution firstCertified = solution(testArchive, 3, 3);
        testArchive.archive.addSolution(firstIntermediate);
        testArchive.archive.addSolution(secondIntermediate);

        testArchive.archive.addCertified(firstCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 1);
        Assert.assertSame(testArchive.archive.getParetoFrontSolutions().get(0), firstCertified);
        Assert.assertSame(lastSolution(testArchive.archive), firstIntermediate,
                "The intermediate displaced from the certified boundary must become the last entry");
        assertConsistent(testArchive);

        Solution secondCertified = solution(testArchive, 4, 2);
        testArchive.archive.addCertified(secondCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);
        Assert.assertSame(testArchive.archive.getParetoFrontSolutions().get(1), secondCertified);
        Assert.assertSame(lastSolution(testArchive.archive), secondIntermediate,
                "The intermediate displaced from the certified boundary must become the last entry");
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testCertifiedSolutionRemovesDominatedIntermediateSolutions() {
        TestArchive testArchive = createTestArchive();
        Solution existingCertified = solution(testArchive, 6, 0);
        Solution dominatedIntermediate = solution(testArchive, 2, 2);
        Solution incomparableIntermediate = solution(testArchive, 1, 5);
        Solution newCertified = solution(testArchive, 3, 3);

        testArchive.archive.addCertified(existingCertified);
        testArchive.archive.addSolution(dominatedIntermediate);
        testArchive.archive.addSolution(incomparableIntermediate);

        testArchive.archive.addCertified(newCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(),
                List.of(existingCertified, newCertified, incomparableIntermediate));
        Assert.assertFalse(testArchive.archive.getParetoFrontSolutions().contains(dominatedIntermediate));
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testAddingSeveralCertifiedSolutionsInARow() {
        TestArchive testArchive = createTestArchive();
        Solution firstCertified = solution(testArchive, 5, 1);
        Solution secondCertified = solution(testArchive, 3, 3);
        Solution thirdCertified = solution(testArchive, 1, 5);

        testArchive.archive.addCertified(firstCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 1);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(), List.of(firstCertified));
        Assert.assertSame(lastSolution(testArchive.archive), firstCertified);
        assertConsistent(testArchive);

        testArchive.archive.addCertified(secondCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(),
                List.of(firstCertified, secondCertified));
        Assert.assertSame(lastSolution(testArchive.archive), secondCertified);
        assertConsistent(testArchive);

        testArchive.archive.addCertified(thirdCertified);

        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 3);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(),
                List.of(firstCertified, secondCertified, thirdCertified));
        Assert.assertSame(lastSolution(testArchive.archive), thirdCertified);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(),
                testArchive.archive.getParetoFrontSolutions().size());
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testCertifiedSolutionsAreAddedWithoutDominanceChecks() {
        TestArchive testArchive = createTestArchive();
        Solution firstCertified = solution(testArchive, 2, 4);
        Solution secondCertified = solution(testArchive, 4, 2);
        Solution dominatingCertified = solution(testArchive, 5, 5);
        testArchive.archive.addCertified(firstCertified);
        testArchive.archive.addCertified(secondCertified);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);

        testArchive.archive.addCertified(dominatingCertified);

        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(),
                List.of(firstCertified, secondCertified, dominatingCertified),
                "Certified solutions must be inserted without dominance comparisons");
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 3);
        Assert.assertSame(lastSolution(testArchive.archive), dominatingCertified);
        assertPoints(testArchive.archive, Set.of("2,4", "4,2", "5,5"));
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testNonCertifiedSolutionDoesNotRemoveCertifiedSolutions() {
        TestArchive testArchive = createTestArchive();
        Solution firstCertified = solution(testArchive, 2, 4);
        Solution secondCertified = solution(testArchive, 4, 2);
        Solution nonCertified = solution(testArchive, 5, 5);
        testArchive.archive.addCertified(firstCertified);
        testArchive.archive.addCertified(secondCertified);

        testArchive.archive.addSolution(nonCertified);

        assertConsistent(testArchive);
        Assert.assertEquals(testArchive.archive.getParetoFrontSolutions(),
                List.of(firstCertified, secondCertified, nonCertified),
                "Dominance checks must not inspect or remove the certified prefix");
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testPromotionMovesSolutionToEndOfCertifiedPrefix() {
        TestArchive testArchive = createTestArchive();
        Solution certified = solution(testArchive, 5, 1);
        Solution displacedIntermediate = solution(testArchive, 4, 2);
        Solution promoted = solution(testArchive, 1, 5);
        testArchive.archive.addCertified(certified);
        testArchive.archive.addSolution(displacedIntermediate);
        testArchive.archive.addSolution(promoted);

        int[] movedIntoOldPosition = testArchive.archive.promoteToCertified(2);

        Assert.assertEquals(movedIntoOldPosition, new int[]{4, 2});
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2);
        Assert.assertSame(testArchive.archive.getParetoFrontSolutions().get(1), promoted);
        Assert.assertSame(lastSolution(testArchive.archive), displacedIntermediate);
        assertConsistent(testArchive);

        Assert.assertNull(testArchive.archive.promoteToCertified(1));
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 2,
                "Promoting an already certified solution must have no effect");
        assertConsistent(testArchive);

        testArchive.archive.promoteToCertified(2);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(),
                testArchive.archive.getParetoFrontSolutions().size(),
                "All solutions may validly be certified");
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testRemoveAndSwapKeepsListsAligned() {
        TestArchive testArchive = createTestArchive();
        Solution certified = solution(testArchive, 5, 1);
        Solution removed = solution(testArchive, 2, 2);
        Solution swapped = solution(testArchive, 1, 5);
        testArchive.archive.addCertified(certified);
        testArchive.archive.addSolution(removed);
        testArchive.archive.addSolution(swapped);

        int[] swappedValues = testArchive.archive.removeAndSwapWithLast(1);

        Assert.assertEquals(swappedValues, new int[]{1, 5});
        Assert.assertFalse(testArchive.archive.getParetoFrontSolutions().contains(removed));
        Assert.assertSame(testArchive.archive.getParetoFrontSolutions().get(0), certified);
        Assert.assertSame(testArchive.archive.getParetoFrontSolutions().get(1), swapped);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 1);
        assertConsistent(testArchive);

        Assert.assertNull(testArchive.archive.removeAndSwapWithLast(1));
        Assert.assertEquals(testArchive.archive.getCertifiedSize(),
                testArchive.archive.getParetoFrontSolutions().size());
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000,
            expectedExceptions = IndexOutOfBoundsException.class)
    public void testCertifiedSolutionCannotBeRemoved() {
        TestArchive testArchive = createTestArchive();
        testArchive.archive.addCertified(solution(testArchive, 5, 1));

        testArchive.archive.removeAndSwapWithLast(0);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testCertifiedObjectiveValuesAreCopied() {
        TestArchive testArchive = createTestArchive();
        int[] values = {2, 3};

        testArchive.archive.addCertified(null, values);
        values[0] = 99;

        Assert.assertEquals(testArchive.archive.getParetoFrontValues().get(0), new int[]{2, 3});
        assertConsistent(testArchive);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testAddIntermediateSolutionRecordsCurrentValues() {
        TestArchive testArchive = createTestArchive();
        testArchive.model.arithm(testArchive.firstObjective, "=", 2).post();
        testArchive.model.arithm(testArchive.secondObjective, "=", 3).post();
        Assert.assertTrue(testArchive.model.getSolver().solve());

        Solution recorded = testArchive.archive.addIntermediateSolution();

        Assert.assertNotNull(recorded);
        Assert.assertEquals(recorded.getIntVal(testArchive.firstObjective), 2);
        Assert.assertEquals(recorded.getIntVal(testArchive.secondObjective), 3);
        Assert.assertEquals(testArchive.archive.getCertifiedSize(), 0);
        assertPoints(testArchive.archive, Set.of("2,3"));
        assertConsistent(testArchive);
    }

    private TestArchive createTestArchive() {
        Model model = new Model();
        IntVar firstObjective = model.intVar("first", 0, 10);
        IntVar secondObjective = model.intVar("second", 0, 10);
        IntVar[] objectives = {firstObjective, secondObjective};
        return new TestArchive(
                model, firstObjective, secondObjective, objectives, new ParetoArchive(objectives)
        );
    }

    private Solution solution(TestArchive testArchive, int firstValue, int secondValue) {
        Solution solution = new Solution(testArchive.model);
        solution.setIntVal(testArchive.firstObjective, firstValue);
        solution.setIntVal(testArchive.secondObjective, secondValue);
        return solution;
    }

    private void assertConsistent(TestArchive testArchive) {
        List<Solution> solutions = testArchive.archive.getParetoFrontSolutions();
        List<int[]> values = testArchive.archive.getParetoFrontValues();
        Assert.assertEquals(solutions.size(), values.size(),
                "The solution and objective-value lists must remain aligned");
        Assert.assertTrue(testArchive.archive.getCertifiedSize() >= 0);
        Assert.assertTrue(testArchive.archive.getCertifiedSize() <= solutions.size(),
                "The certified prefix cannot be larger than the archive");

        for (int i = 0; i < solutions.size(); i++) {
            Solution solution = solutions.get(i);
            if (solution != null) {
                for (int objective = 0; objective < testArchive.objectives.length; objective++) {
                    Assert.assertEquals(solution.getIntVal(testArchive.objectives[objective]),
                            values.get(i)[objective],
                            "A solution and its objective vector differ at archive index " + i);
                }
            }
        }
    }

    private void assertPoints(ParetoArchive archive, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        for (int[] values : archive.getParetoFrontValues()) {
            actual.add(values[0] + "," + values[1]);
        }
        Assert.assertEquals(actual, expected);
    }

    private Solution lastSolution(ParetoArchive archive) {
        List<Solution> solutions = archive.getParetoFrontSolutions();
        return solutions.get(solutions.size() - 1);
    }

    private record TestArchive(Model model, IntVar firstObjective, IntVar secondObjective, IntVar[] objectives,
                               ParetoArchive archive) {

    }
}
