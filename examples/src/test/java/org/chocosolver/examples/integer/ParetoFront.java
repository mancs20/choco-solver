/*
 * This file is part of examples, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
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
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.testng.Assert.*;

/**
 * Trivial multi-objective optimization computing pareto solutions
 *
 * @author Jimmy Liang, Jean-Guillaume Fages
 */
public class ParetoFront {

	@Test(groups = "1s", timeOut = 60000)
	public void testPareto(){
		// simple model
		Model model = new Model();
		IntVar a = model.intVar("a", 0, 2, false);
		IntVar b = model.intVar("b", 0, 2, false);
		IntVar c = model.intVar("c", 0, 2, false);
		model.arithm(a, "+", b, "=", c).post();

		// retrieve the pareto front
		List<Solution> paretoFront = model.getSolver().findParetoFront(new IntVar[]{a,b},true );
		System.out.println("The pareto front has "+paretoFront.size()+" solutions : ");
		Assert.assertEquals(3, paretoFront.size());
		for(Solution s:paretoFront){
			System.out.println("a = "+s.getIntVal(a)+" and b = "+s.getIntVal(b));
			Assert.assertEquals(2, s.getIntVal(c));
		}
	}

	@Test(groups = "5s", timeOut = 300_000)
	public void testParetoWhenTimeoutHappens() throws Exception {
		String instanceFile = "n_queens_p-5_q-8_ins-1.dat";
		int timeoutSec = 1;
		String[] methodsToCompare = new String[]{"Saugmecon"};

		for (String method : methodsToCompare) {
			JSONArray pfJson = runAndCollectPFStringsNqueens(method, instanceFile, timeoutSec);
			System.out.println("Method " + method + " found " + pfJson.length() + " non-dominated solutions.");
			pfJsonToSet(pfJson, method);
		}
	}

	@Test(groups = "1000s", timeOut = 60_000_000)
	public void testParetoMethods() throws Exception {
		String instanceFile = "n_queens_p-5_q-8_ins-1.dat";
		int timeoutSec = 1000;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfo";
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

	@Test(groups = "1000s", timeOut = 60_000_000)
	public void testParetoMethodsBiObjectiveProblems() throws Exception {
		String instanceFile = "n_queens_p-2_q-8_ins-1.dat";
		int timeoutSec = 1000;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfo";
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

	@Test(groups = "1000s", timeOut = 60_000_000)
	public void testSaugmeconObjectiveFunction() throws Exception {
		String instanceFile = "n_queens_p-3_q-8_ins-1.dat";
		int timeoutSec = 1000;
		String baseMethodInComparisson = "ParetoGavanelliGlobalConstraintNoEvolutionInfo";
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

	@Test(groups = "5s", timeOut = 300_000)
	public void testSaugmeconWhenTimeoutReachedAfterIndividualOptimalValues() throws Exception {
		String instanceFile = "KP_p-2_n-50_ins-14.dat";
		int timeoutSec = 5;
		String[] methodsToCompare = new String[]{"Saugmecon"};

		for (String method : methodsToCompare) {
			JSONArray pfJson = runAndCollectPFStringsMOOLibraryKP(method, instanceFile, timeoutSec);
			System.out.println("Method " + method + " found " + pfJson.length() + " non-dominated solutions.");
			pfJsonToSet(pfJson, method);
		}
	}

	private JSONArray runAndCollectPFStringsNqueens(String paretoMethod, String instanceFile, int timeoutSec) throws Exception {
		String benchmark = "powa";
		String problem = "nqueens";
		return runAndCollectPFStrings(paretoMethod, instanceFile, timeoutSec, benchmark, problem);
	}

	private JSONArray runAndCollectPFStringsMOOLibraryKP(String paretoMethod, String instanceFile, int timeoutSec) throws Exception {
		String benchmark = "MOOLibrary";
		String problem = "UKP";
		return runAndCollectPFStrings(paretoMethod, instanceFile, timeoutSec, benchmark, problem);
	}

	private JSONArray runAndCollectPFStrings(String paretoMethod, String instanceFile, int timeoutSec,
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

		// capture stdout
		PrintStream originalOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		try {
			ParetoGenerationExperiments.main(args);
		} finally {
			System.setOut(originalOut);
		}
		String out = new String(baos.toByteArray(), StandardCharsets.UTF_8);

		String json = extractSolutionsDetailsJson(out);
		assertNotNull(json, "Could not find solutions-details JSON in output.\nOutput was:\n" + out);

		return new JSONObject(json)
				.getJSONObject("solutions-details")
				.getJSONArray("pareto_front");
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

	// Find the JSON object starting at {"solutions-details"...} using simple brace counting.
	private String extractSolutionsDetailsJson(String out) {
		String marker = "{\"solutions-details\"";
		int start = out.indexOf(marker);
		if (start < 0) return null;
		int braces = 0;
		boolean started = false;
		for (int i = start; i < out.length(); i++) {
			char c = out.charAt(i);
			if (c == '{') { braces++; started = true; }
			else if (c == '}') { braces--; }
			if (started && braces == 0) {
				return out.substring(start, i + 1);
			}
		}
		return null;
	}
}
