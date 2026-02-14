/*
 * This file is part of examples, http://choco-solver.org/
 *
 * Copyright (c) 2026, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
/**
 * @author Jean-Guillaume Fages
 * @since 21/03/14
 * Created by IntelliJ IDEA.
 */
package org.chocosolver.examples.integer;

import org.chocosolver.examples.integer.experiments.ParetoGenerationExperiments;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Trivial multi-objective optimization computing pareto solutions
 *
 * @author Jimmy Liang, Jean-Guillaume Fages
 */
@Test(singleThreaded = true)
public class ParetoFront {

	private static final Map<String, RunResult> RUN_CACHE = new ConcurrentHashMap<>();

	private static String runKey(String benchmark, String problem, String instanceFile,
								 int timeoutSec, String method) {
		return String.join("|", benchmark, problem, instanceFile, String.valueOf(timeoutSec), method);
	}

	@AfterClass
	public void clearRunCache() {
		RUN_CACHE.clear();
	}

	@DataProvider(name = "methods")
	public Object[][] methods() {
		return new Object[][]{
//				{"SimpleOptGlobalConstraintTest"}, {"SimpleOptGlobalConstraint"}
//				{"SimpleOptGlobalConstraint"}, {"SaugmeconGlobalIntermediate"}, {"Saugmecon"}, {"Gavanelli"}, {"SaugmeconNoRTest"}, {"SimpleOptGlobalConstraintTest"}, {"SimpleOptGlobalConstraint"}
				{"SaugmeconNoRTestReal"}, {"ParetoDisjunctiveProgrammingTest"}, {"SaugmeconGlobal"},
				{"SaugmeconGlobalIntermediate"}, {"Saugmecon"}, {"Gavanelli"}, {"SaugmeconNoRTest"}, {"ParetoDisjunctiveProgrammingNoLabel"}, {"SimpleOptGlobalConstraintTest"}
//				{"SaugmeconNoRTest"}, {"ParetoGavanelliGlobalConstraintNoEvolutionInfoTest"}//, {"SimpleOptGlobalConstraint"},{"Saugmecon"},
//				{"Gavanelli"}, {"Saugmecon"}, {"ParetoGavanelliGlobalConstraintNoEvolutionInfoTest"}//, {"SimpleOptGlobalConstraint"},{"Saugmecon"},
//				{"ParetoDisjunctiveProgrammingTest"}, {"GIA"}, {"GIA_bounded"}, {"GIA_boundedLazy"}
		};
	}

	@DataProvider(name = "methodsMaximize")
	public Object[][] methodsMaximize() {
		return new Object[][]{
			{"SimpleOptGlobalConstraintTest"}, {"SimpleOptGlobalConstraintTest"}, {"SaugmeconGlobal"}, {"SaugmeconGlobalIntermediate"}, {"SimpleOptGlobalConstraintTest"}, {"Saugmecon"},
			{"ParetoDisjunctiveProgrammingNoLabel"}, {"SaugmeconNoRTestReal"}
		};
	}

	@DataProvider(name = "methodsMinimize")
	public Object[][] methodsMinimize() {
		return new Object[][]{
				{"ParetoDisjunctiveProgrammingTest"}
		};
	}

	@DataProvider(name = "methodsOptimizeObjectivesIndividually")
	public Object[][] methodsOptimizeObjectivesIndividually() {
		return new Object[][]{
				{"SaugmeconGlobal"}, {"SaugmeconGlobalIntermediate"},{"Saugmecon"},{"SaugmeconNoRTest"},
				{"ParetoDisjunctiveProgrammingTest"}, {"SaugmeconNoRTestReal"}, {"ParetoDisjunctiveProgrammingNoLabel"}
		};
	}

	@Test(groups = "1s", timeOut = 5000)
	public void testPareto() {
		// simple model
		Model model = new Model();
		IntVar a = model.intVar("a", 0, 2, false);
		IntVar b = model.intVar("b", 0, 2, false);
		IntVar c = model.intVar("c", 0, 2, false);
		model.arithm(a, "+", b, "=", c).post();

		// retrieve the pareto front
		List<Solution> paretoFront = model.getSolver().findParetoFront(new IntVar[]{a, b}, true);
		System.out.println("The pareto front has " + paretoFront.size() + " solutions : ");
		Assert.assertEquals(3, paretoFront.size());
		for (Solution s : paretoFront) {
			System.out.println("a = " + s.getIntVal(a) + " and b = " + s.getIntVal(b));
			Assert.assertEquals(2, s.getIntVal(c));
		}
	}

	@Test(dataProvider = "methods", groups = "10s", timeOut = 10_000)
	public void testParetoWhenTimeoutHappens(String method) throws Exception {
		String instanceFile = "n_queens_p-2_q-14_ins-4.dat";
		int timeoutSec = 2;
		long t0 = System.nanoTime();
		RunResult rr = getOrRun(method, instanceFile, timeoutSec, "powa", "nqueens");
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		testGeneralAspectsWhenTimeout(rr, elapsedMs, method, timeoutSec);
	}

	private void testGeneralAspectsWhenTimeout(RunResult rr, long elapsedMs, String method, int timeoutSec) {
		JSONArray pfJson = rr.solutionsDetails.getJSONArray("pareto_front");
		System.out.println("Method " + method + " found " + pfJson.length() + " non-dominated solutions in "
				+ elapsedMs + " ms. Exhaustive: " + rr.wasExhaustive() + ". Expected timeout: " + timeoutSec * 1000 + " ms.");

		long upperBoundMs = timeoutSec * 1000L + 1000;
		assertTrue(elapsedMs <= upperBoundMs,
				"Expected <= " + upperBoundMs + " ms, got " + elapsedMs + " ms");
		assertFalse(rr.wasExhaustive(),
				"Expected non-exhaustive search when timeout happens" );
		String messageToCheck = rr.solverMessages.get(0);
		if (messageToCheck != null && messageToCheck.toLowerCase().contains("solutions")) {
			assertTrue(rr.solutionsDetails.getJSONArray("pareto_front").length() > 0,
					"Expected some points in the pareto front when timeout happens");
			assertTrue(rr.solutionsDetails.getJSONArray("solutions_pareto_front").length() > 0,
					"Expected some solutions in the Pareto set when timeout happens");
		}
		assertTrue(rr.stdout.contains("approximation"));
		assertFalse(rr.stdout.contains("There are 0 points"));
		pfJsonToSet(pfJson, method);
	}

	@Test(dataProvider = "methods", groups = "100s", timeOut = 100_000)
	public void testParetoMethodsForKnapsack(String method) throws Exception {
		String[] ukpInstanceFiles = new String[] {
				"KP_p-5_n-10_ins-10.dat"
				,"KP_p-5_n-10_ins-4.dat"
				,"KP_p-5_n-10_ins-5.dat"
				,"KP_p-5_n-10_ins-6.dat"
				,"KP_p-5_n-10_ins-8.dat"
				,"KP_p-5_n-10_ins-9.dat"
				,"KP_p-5_n-20_ins-2.dat"
				,"KP_p-5_n-20_ins-7.dat"
		};
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfoTest";
		int timeoutSec = 100;
		for (String instance: ukpInstanceFiles) {
			compareMethodsWithKnapsack(baseMethodInComparisson, method, instance, timeoutSec);
		}
	}

	@Test(dataProvider = "methods", groups = "100s", timeOut = 100_000)
	public void testParetoMethods(String method) throws Exception {
		String instanceFile = "n_queens_p-5_q-8_ins-1.dat";
		int timeoutSec = 10;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfoTest";
		long t0 = System.nanoTime();
		compareMethodsWithNqueen(baseMethodInComparisson, method, instanceFile, timeoutSec);
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		System.out.println("Method " + method + " compared to " + baseMethodInComparisson + " in "
				+ elapsedMs + " ms.");
	}

	@Test(groups = "10s", timeOut = 10_000)
	public void testSaugmeconRealFloatingPoint() throws Exception {
		String instanceFile = "paris_30_cost_clouds_angle.fzn";
		int timeoutSec = 5;
		String method = "SaugmeconNoRTestReal";
		String refMethod = "SaugmeconNoRTest";
		RunResult rrReal = getOrRun(method, instanceFile, timeoutSec, "powa", "sims");
		RunResult rrInt = getOrRun(refMethod, instanceFile, timeoutSec, "powa", "sims");
		JSONArray pfJsonReal = rrReal.solutionsDetails.getJSONArray("pareto_front");
		JSONArray pfJsonInt = rrInt.solutionsDetails.getJSONArray("pareto_front");
		compareParetoFrontJson(pfJsonInt, pfJsonReal, refMethod, method, instanceFile);
	}

	@Test(groups = "200s", timeOut = 200_000)
	public void testFrameworkSaugmeconVsNonFramework() throws Exception {
		String instanceFile = "n_queens_p-5_q-8_ins-1.dat";
		int timeoutSec = 20;
		String baseMethodInComparisson = "SaugmeconNoRTest";
		long t0 = System.nanoTime();
		String method = "Saugmecon";
		compareFrontSameOrderMethodsWithNqueen(baseMethodInComparisson, method, instanceFile, timeoutSec);
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		System.out.println("Method " + method + " compared to " + baseMethodInComparisson + " in "
				+ elapsedMs + " ms.");
	}

	@Test(groups = "200s", timeOut = 200_000)
	public void testFrameworkGavanelliVsNonFramework() throws Exception {
		String instanceFile = "n_queens_p-5_q-8_ins-1.dat";
		int timeoutSec = 20;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfoTest";
		long t0 = System.nanoTime();
		String method = "Gavanelli";
		compareFrontSameOrderMethodsWithNqueen(baseMethodInComparisson, method, instanceFile, timeoutSec);
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		System.out.println("Method " + method + " compared to " + baseMethodInComparisson + " in "
				+ elapsedMs + " ms.");
	}

	@Test(dataProvider = "methodsMaximize", groups = "20s", timeOut = 20000_000)
	public void testObjFunctionDiffSignParetoInMinProblemsForMaxStrategies(String method) throws Exception{
		String instanceFile = "paris_30_cost_clouds.fzn";
		int timeoutSec = 2000;
		RunResult rr = getOrRun(method, instanceFile, timeoutSec, "powa", "fzn_instance");
		// check if the objective is MAximized
		String messageToCheck = rr.solverMessages.get(0);
		assertTrue(messageToCheck.contains("MAXIMIZE"), "MAXIMIZE should be present in the solver messages indicating maximization and not MINIMIZATION " + messageToCheck);
		String startMax = messageToCheck.substring(messageToCheck.indexOf("MAXIMIZE"));
		int optFunctionValue = Integer.parseInt(startMax.substring(startMax.indexOf("= ")+2, startMax.indexOf(",")));
		JSONArray pf = rr.solutionsDetails.getJSONArray("pareto_front");
		int objValFront = (Integer) pf.getJSONArray(0).get(0);
		assertTrue((objValFront ^ optFunctionValue) < 0);
	}

	@Test(dataProvider = "methodsMinimize", groups = "100s", timeOut = 100_000)
	public void testObjFunctionDiffSignParetoInMaxProblemsForMinStrategies(String method) throws Exception{
		String instanceFile = "n_queens_p-3_q-8_ins-1.dat";
		int timeoutSec = 5;
		RunResult rr = getOrRun(method, instanceFile, timeoutSec, "powa", "nqueens");
		// check if the objective is MAximized
		String messageToCheck = rr.solverMessages.get(0);
		assertTrue(messageToCheck.contains("MINIMIZE"), "MINIMIZE should be present in the solver messages indicating maximization and not MINIMIZATION " + messageToCheck);
		String startMax = messageToCheck.substring(messageToCheck.indexOf("MINIMIZE"));
		int optFunctionValue = Integer.parseInt(startMax.substring(startMax.indexOf("= ")+2, startMax.indexOf(",")));
		JSONArray pf = rr.solutionsDetails.getJSONArray("pareto_front");
		int objValFront = (Integer) pf.getJSONArray(0).get(0);
		assertTrue((objValFront ^ optFunctionValue) < 0);
	}

	private void compareMethodsWithNqueen(String baseMethod, String methodToTest, String instanceFile, int timeoutSec) throws Exception {
		JSONArray toTestPF = runAndCollectPFStringsNqueens(methodToTest, instanceFile, timeoutSec);
		JSONArray basePF = runAndCollectPFStringsNqueens(baseMethod, instanceFile, timeoutSec);

		compareParetoFrontJson(basePF, toTestPF, baseMethod, methodToTest, instanceFile);
	}

	private void compareFrontSameOrderMethodsWithNqueen(String baseMethod, String methodToTest, String instanceFile, int timeoutSec) throws Exception {
		JSONArray toTestPF = runAndCollectPFStringsNqueens(methodToTest, instanceFile, timeoutSec);
		JSONArray basePF = runAndCollectPFStringsNqueens(baseMethod, instanceFile, timeoutSec);

		for (int i = 0; i < basePF.length(); i++) {
			String pointBase = basePF.getJSONArray(i).toString();
			String pointToTest = toTestPF.getJSONArray(i).toString();
			if (!pointBase.equals(pointToTest)) {
				fail("Different Pareto front order for methods " + baseMethod + " and " + methodToTest
						+ " at position " + i + ": " + pointBase + " vs " + pointToTest);
			}
		}

		System.out.println("Pareto fronts obtained in the same order for methods " + baseMethod + " and " + methodToTest
				+ " with " + basePF.length() + " solutions.");
	}

	private void compareMethodsWithKnapsack(String baseMethod, String methodToTest, String instanceFile, int timeoutSec) throws Exception {
		JSONArray toTestPF = runAndCollectPFStringsMOOLibraryKP(methodToTest, instanceFile, timeoutSec);
		JSONArray basePF = runAndCollectPFStringsMOOLibraryKP(baseMethod, instanceFile, timeoutSec);

		compareParetoFrontJson(basePF, toTestPF, baseMethod, methodToTest, instanceFile);
	}

	private void compareParetoFrontJson(JSONArray basePF, JSONArray toTestPF, String baseMethod, String methodToTest, String instanceFile) {
		Set<String> basePFSet = pfJsonToSet(basePF, baseMethod);
		Set<String> toTestPFSet = pfJsonToSet(toTestPF, methodToTest);

		assertEquals(toTestPFSet.size(), basePFSet.size(), "Different Pareto front sizes, Gavanelli= "
				+ basePFSet.size() + " and " + methodToTest + "= " + toTestPFSet.size() + " for instance: " + instanceFile);

		if (!toTestPFSet.equals(basePFSet)) {
			Set<String> missingInMethod = new HashSet<>(basePFSet);
			missingInMethod.removeAll(toTestPFSet);
			Set<String> extraInMethod = new HashSet<>(toTestPFSet);
			extraInMethod.removeAll(basePFSet);
			fail("Fronts differ for " + methodToTest
					+ "\nMissing in " + methodToTest + ": " + missingInMethod
					+ "\nExtra in " + methodToTest + ": " + extraInMethod);
		}

		System.out.println("Pareto fronts equal for methods " + baseMethod + " and " + methodToTest
				+ " with " + basePFSet.size() + " solutions for instance: " + instanceFile);
	}

	@Test(dataProvider = "methods", groups = "35s", timeOut = 40000)
	public void testParetoMethodsOutput(String method) throws Exception {
		String instanceFile = "n_queens_p-3_q-8_ins-1.dat";
		int timeoutSec = 5;
		RunResult rr = runAndCollectAllNoCache(method, instanceFile, timeoutSec, "powa", "nqueens");

		JSONArray pf = rr.solutionsDetails.optJSONArray("pareto_front");
		assertNotNull(pf, String.format("[%s] pareto_front missing.\n%s", method, rr.stdout));
		assertTrue(pf.length() >= 0, String.format("[%s] pareto_front empty?\n%s", method, rr.stdout));
		assertAggregatesConsistent(rr.solutionsDetails, rr.solverMessages, rr.stdout);
		assertTrue(rr.wasExhaustive());
	}

	@Test(groups = "100s", timeOut = 100_000)
	public void testParetoMethodsBiObjectiveProblems() throws Exception {
		String instanceFile = "n_queens_p-2_q-8_ins-1.dat";
		int timeoutSec = 100;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfoTest";
		String[] methodsToCompare = new String[]{"Saugmecon"};

		JSONArray basePF = runAndCollectPFStringsNqueens(baseMethodInComparisson, instanceFile, timeoutSec);
		Set<String> basePFSet = pfJsonToSet(basePF, baseMethodInComparisson);

		for (String method : methodsToCompare) {
			JSONArray pfJson = runAndCollectPFStringsNqueens(method, instanceFile, timeoutSec);
			Set<String> pf = pfJsonToSet(pfJson, method);

			assertEquals(pf.size(), basePFSet.size(), "Different Pareto front sizes, Gavanelli= "
					+ basePFSet.size() + " and " + method + "= " + pf.size());

			if (!pf.equals(basePFSet)) {
				Set<String> missingInMethod = new HashSet<>(basePFSet);
				missingInMethod.removeAll(pf);
				Set<String> extraInMethod = new HashSet<>(pf);
				extraInMethod.removeAll(basePFSet);
				fail("Fronts differ for " + method
						+ "\nMissing in " + method + ": " + missingInMethod
						+ "\nExtra in " + method + ": " + extraInMethod);
			}
		}
	}

	@Test(groups = "100", timeOut = 100_000)
	public void testSaugmeconObjectiveFunction() throws Exception {
		String instanceFile = "n_queens_p-3_q-8_ins-1.dat";
		int timeoutSec = 100;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfoTest";
		String[] methodsToCompare = new String[]{"Saugmecon"};

		JSONArray basePF = runAndCollectPFStringsNqueens(baseMethodInComparisson, instanceFile, timeoutSec);
		Set<String> basePFSet = pfJsonToSet(basePF, baseMethodInComparisson);

		for (String method : methodsToCompare) {
			JSONArray pfJson = runAndCollectPFStringsNqueens(method, instanceFile, timeoutSec);
			Set<String> pf = pfJsonToSet(pfJson, method);

			assertEquals(pf.size(), basePFSet.size(), "Different Pareto front sizes, Gavanelli= "
					+ basePFSet.size() + " and " + method + "= " + pf.size());

			if (!pf.equals(basePFSet)) {
				Set<String> missingInMethod = new HashSet<>(basePFSet);
				missingInMethod.removeAll(pf);
				Set<String> extraInMethod = new HashSet<>(pf);
				extraInMethod.removeAll(basePFSet);
				fail("Fronts differ for " + method
						+ "\nMissing in " + method + ": " + missingInMethod
						+ "\nExtra in " + method + ": " + extraInMethod);
			}
		}
	}

	@Test(dataProvider = "methodsOptimizeObjectivesIndividually", groups = "100s", timeOut = 100_000)
	public void testParetoMethodsWhenTimeoutReachedWhileFindingIndividualOptimalValues(String method) throws Exception {
		String instanceFile = "J30_21_6.fzn";
		int timeoutSec = 10;
		long t0 = System.nanoTime();
		RunResult rr= getOrRun(method, instanceFile, timeoutSec, "minizinc-rcpsp", "RCPSP");
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		testGeneralAspectsWhenTimeout(rr, elapsedMs, method, timeoutSec);
	}

	@Test(dataProvider = "methodsOptimizeObjectivesIndividually", groups = "15s", timeOut = 15_0000000)
	public void testParetoMethodsWhenTimeoutReachedAfterIndividualOptimalValues(String method) throws Exception {
		String instanceFile = "lagos_nigeria_100_cost_clouds_angle.fzn";
		int timeoutSec = 1;
		RunResult rr = getOrRun(method, instanceFile, timeoutSec, "powa", "fzn_instance");
		assertFalse(rr.wasExhaustive());
		assertEquals(rr.solutionsDetails.getJSONArray("pareto_front").length(), 1);
		System.out.println(rr.stdout);
	}

	private JSONArray runAndCollectPFStringsNqueens(String paretoMethod, String instanceFile, int timeoutSec) throws Exception {
		return runAndCollectPFStrings(paretoMethod, instanceFile, timeoutSec, "powa", "nqueens");
	}

	private JSONArray runAndCollectPFStringsMOOLibraryKP(String paretoMethod, String instanceFile, int timeoutSec) throws Exception {
		return runAndCollectPFStrings(paretoMethod, instanceFile, timeoutSec, "MOOLibrary", "UKP");
	}

	private JSONArray runAndCollectPFStrings(String paretoMethod, String instanceFile, int timeoutSec,
											 String benchmark, String problem) throws Exception {
		RunResult rr = getOrRun(paretoMethod, instanceFile, timeoutSec, benchmark, problem);
		return rr.solutionsDetails.getJSONArray("pareto_front");
	}

	private RunResult runAndCollectAllNoCache(String paretoMethod, String instanceFile, int timeoutSec,
											  String benchmark, String problem) throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("moInstances/" + instanceFile);
		Path instancePath = Paths.get(Objects.requireNonNull(url, "instance not found: " + instanceFile).toURI());
		String instanceName = instanceFile.split("\\.")[0];

		String[] args = {
				benchmark, problem,
				instanceName,
				instancePath.toString(),
				"default",
				String.valueOf(timeoutSec),
				paretoMethod
		};

		// capture stdout of the experiment
		PrintStream originalOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos, true));
		try {
			ParetoGenerationExperiments.main(args);
		} finally {
			System.setOut(originalOut);
		}
		String out = baos.toString();

		// parse solutions-details JSON (output starts from "Starting experiment...")
		JSONObject lastSolutionsDetails = null;
		Pattern pattern = Pattern.compile("\\{(?:[^{}]|\\{[^{}]*\\})*\\}");
		Matcher matcher = pattern.matcher(out);

		while (matcher.find()) {
			String candidate = matcher.group();
			try {
				JSONObject obj = new JSONObject(candidate);
				if ("solutions-details".equals(obj.optString("type"))) {
					lastSolutionsDetails = obj.getJSONObject("solutions-details");
					break;
				}
			} catch (Exception ignore) {
				// Skip non-JSON or mismatches
			}
		}


		assertNotNull(lastSolutionsDetails, "No solutions-details JSON found.\nOutput was:\n" + out);

		// pull solver_messages if present
		List<String> solverMessages = new ArrayList<>();
		if (lastSolutionsDetails.has("solver_messages")) {
			JSONArray msgs = lastSolutionsDetails.getJSONArray("solver_messages");
			for (int i = 0; i < msgs.length(); i++) solverMessages.add(msgs.getString(i));
		}

		return new RunResult(lastSolutionsDetails, solverMessages, out);
	}

	private RunResult getOrRun(String paretoMethod, String instanceFile, int timeoutSec,
							   String benchmark, String problem) throws Exception {
		String key = runKey(benchmark, problem, instanceFile, timeoutSec, paretoMethod);
		RunResult cached = RUN_CACHE.get(key);
		if (cached != null) return cached;

		RunResult rr = runAndCollectAllNoCache(paretoMethod, instanceFile, timeoutSec, benchmark, problem);
		RUN_CACHE.put(key, rr);
		return rr;
	}

	private static ChocoMsgStats parseChocoMessage(String msg) {
		ChocoMsgStats s = new ChocoMsgStats();
		s.buildingTime = extractDouble(msg, "Building time\\s*:\\s*([\\d,.]+)s");
		s.resolutionTime = extractDouble(msg, "Resolution time\\s*:\\s*([\\d,.]+)s");

		Matcher m = Pattern.compile("Nodes:\\s*([\\d,]+)\\s*\\(([\\d,.]+)\\s*n/s\\)").matcher(msg);
		if (m.find()) {
			s.nodes = Long.parseLong(m.group(1).replace(",", ""));
			s.avgNodesPerSec = Double.parseDouble(m.group(2).replace(",", ""));
		}

		s.fails = (long) extractDouble(msg, "Fails\\s*:\\s*([\\d,]+)");
		s.backtracks = (long) extractDouble(msg, "Backtracks\\s*:\\s*([\\d,]+)");
		s.backjumps = (long) extractDouble(msg, "Backjumps\\s*:\\s*([\\d,]+)");
		s.restarts = (long) extractDouble(msg, "Restarts\\s*:\\s*([\\d,]+)");
		s.solutions = (long) extractDouble(msg, "Solutions\\s*:\\s*([\\d,]+)");
		return s;
	}

	private static double extractDouble(String s, String regex) {
		Matcher m = Pattern.compile(regex).matcher(s);
		if (m.find()) return Double.parseDouble(m.group(1).replace(",", ""));
		return 0.0;
	}

	private static void assertAggregatesConsistent(JSONObject details, List<String> msgs, String stdout) {
		if (msgs.isEmpty()) return;

		Matcher fgMatcher = Pattern.compile("frontGenerator:([A-Za-z0-9_]+)").matcher(stdout);
		String frontGenerator = fgMatcher.find() ? fgMatcher.group(1) : "";
		final boolean isGavanelli = frontGenerator.toLowerCase().contains("gavanelli");

		List<ChocoMsgStats> parsed = msgs.stream()
				.map(ParetoFront::parseChocoMessage)
				.collect(Collectors.toList());

		double tol = 1e-6; // float tolerance

		double expectedBuilding;
		double expectedResolutionSum = 0;
		long expectedFails = 0, expectedBacktracks = 0, expectedBackjumps = 0, expectedRestarts = 0, expectedSolutions = 0;
		long expectedNodes = 0;
		double avgNodesPerSec;

		ChocoMsgStats last = parsed.get(parsed.size() - 1);
		expectedBuilding = last.buildingTime;
		if (isGavanelli) {
			expectedResolutionSum = last.resolutionTime;
			expectedFails = last.fails;
			expectedBacktracks = last.backtracks;
			expectedBackjumps = last.backjumps;
			expectedRestarts = last.restarts;
			expectedSolutions = last.solutions;
			expectedNodes = last.nodes;
			avgNodesPerSec = last.avgNodesPerSec;
		} else {
			for (ChocoMsgStats s : parsed) {
				expectedResolutionSum += s.resolutionTime;
				expectedFails += s.fails;
				expectedBacktracks += s.backtracks;
				expectedBackjumps += s.backjumps;
				expectedRestarts += s.restarts;
				expectedSolutions += s.solutions;
				expectedNodes += s.nodes;
			}
			avgNodesPerSec = parsed.stream().mapToDouble(p -> p.avgNodesPerSec).average().orElse(0.0);
		}

		assertAlmostEquals(details.optDouble("sum_solutions_building_time(s)"), expectedBuilding, tol,
				"sum_solutions_building_time(s)");
		assertAlmostEquals(details.optDouble("sum_solutions_resolution_time(s)"), expectedResolutionSum, tol,
				"sum_solutions_resolution_time(s)");
		assertEquals(details.optLong("sum_solutions_fails"), expectedFails,
				"sum_solutions_fails mismatch");
		assertEquals(details.optLong("sum_solutions_backtracks"), expectedBacktracks,
				"sum_solutions_backtracks mismatch");
		assertEquals(details.optLong("sum_solutions_backjumps"), expectedBackjumps,
				"sum_solutions_backjumps mismatch");
		assertEquals(details.optLong("sum_solutions_restarts"), expectedRestarts,
				"sum_solutions_restarts mismatch");
		assertEquals(details.optLong("sum_number_solutions"), expectedSolutions,
				"sum_number_solutions mismatch");
		assertEquals(details.optLong("sum_solutions_nodes"), expectedNodes,
				"sum_solutions_nodes mismatch");

		double actualAvg = details.optDouble("average_node_per_second");
		if (Math.abs(Math.floor(actualAvg) - Math.floor(avgNodesPerSec)) > 0) {
			assertAlmostEquals(actualAvg, avgNodesPerSec, 1e-3, "average_node_per_second");
		}
	}

	private static void assertAlmostEquals(double actual, double expected, double tol, String field) {
		if (Double.isNaN(actual) && Double.isNaN(expected)) return;
		if (Math.abs(actual - expected) > tol) {
			fail(field + " mismatch. expected=" + expected + " actual=" + actual);
		}
	}

	private Set<String> pfJsonToSet(JSONArray pf, String methodName) {
		Set<String> set = new HashSet<>();
		List<String> dups = new ArrayList<>();
		for (int i = 0; i < pf.length(); i++) {
			String point = pf.getJSONArray(i).toString();
			if (!set.add(point)) {
				dups.add(point);
			}
		}
		assertTrue(dups.isEmpty(), "Error in " + methodName +". Duplicate solutions: " + dups);
		assertFalse(set.isEmpty(), "Error in " + methodName +". Empty pareto_front parsed");
		return set;
	}
}

class RunResult {
	final JSONObject solutionsDetails;
	final List<String> solverMessages;
	final String stdout;

	RunResult(JSONObject solutionsDetails, List<String> solverMessages, String stdout) {
		this.solutionsDetails = solutionsDetails;
		this.solverMessages = solverMessages;
		this.stdout = stdout;
	}

	public boolean wasExhaustive() {
		return solutionsDetails.optBoolean("exhaustive");
	}
}

class ChocoMsgStats {
	double buildingTime;
	double resolutionTime;
	long nodes;
	double avgNodesPerSec;
	long fails;
	long backtracks;
	long backjumps;
	long restarts;
	long solutions;
}
