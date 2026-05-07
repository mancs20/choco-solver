# PAUGMECON experiments

This repository is a fork of the Choco solver used to implement the multi-objective algorithm PAUGMECON, which computes the complete Pareto front within a constraint solver.

It also includes the benchmark instances used in the experiments of the following paper:

**M. Combarro Simón, P. Talbot, and P. Bouvry. _Combining an ϵ-Constraint Method with the Pareto Global Constraint_. CP 2026.**

The implementation is based on `choco-solver-5.0.0`.

## Running the experiments

The main class used to run the experiments is:

```text
examples/src/main/java/org/chocosolver/examples/integer/experiments/ParetoGenerationExperiments.java
```

This class generates Pareto fronts for a given benchmark instance. The parameters are passed as command-line arguments.

The expected command-line format is:

```bash
java ParetoGenerationExperiments <benchmark> <problem> <instanceName> <instancePath> <solverSearchStrategy> <solverTimeoutSec> <frontGenerator> <threads>
```

where:

- `<benchmark>` is the benchmark name, e.g. `MOOLibrary` for `ukp` or `powa` for `NQUEENS` and when the instance files are `.fzn` files (`RCPSP` and `sims`).
- `<problem>` is the problem name. The problems used for the experiments:
  - `ukp` (for multi-objective unidimensional knapsack)
  - `NQUEENS`
  - `RCPSP`
  - `sims_cost_clouds` or `sims_cost_clouds_angle`.
- `<instanceName>` is the name of the instance.
- `<instancePath>` is the path to the instance file.
- `<solverSearchStrategy>` is the solver search strategy. In the experiments, `default` corresponds to `domOverWDegSearch`.
- `<solverTimeoutSec>` is the maximum solving time in seconds.
- `<frontGenerator>` is the Pareto-front generation algorithm. The supported values are:
    - `MOBAB-CP`
    - `SAUGMECON`
    - `PAUGMECON`

## Example

Assuming the command is run from the repository root, an example call is:

```bash
java ParetoGenerationExperiments MOOLibrary ukp KP_p-4_n-30_ins-2 benchmarks/ukp/KP_p-4_n-30_ins-2.dat default 3600 MOBAB-CP
```

The benchmark instances are stored in the top-level `benchmarks/` directory.