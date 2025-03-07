![logo](https://github.com/chocoteam/choco-solver/blob/master/solver/src/resources/png/ChocoLogo-160x135.png)

[![DOI](https://joss.theoj.org/papers/10.21105/joss.04708/status.svg)](https://doi.org/10.21105/joss.04708)

[![Discord](https://img.shields.io/discord/976015799619842078?color=7289DA&logo=discord&style=plastic)](https://discord.gg/aH6zxa7e64)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.choco-solver/choco-solver/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.choco-solver/choco-solver)
[![javadoc.io](https://javadoc.io/badge2/org.choco-solver/choco-solver/javadoc.io.svg)](https://javadoc.io/doc/org.choco-solver/choco-solver)

![Build](https://github.com/chocoteam/choco-solver/actions/workflows/maven-test.yml/badge.svg)
[![codecov.io](https://codecov.io/github/chocoteam/choco-solver/coverage.svg?branch=master)](https://codecov.io/github/chocoteam/choco-solver?branch=master)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/f1bafa113f94486b96343d63782c0f7a)](https://www.codacy.com/gh/chocoteam/choco-solver/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=chocoteam/choco-solver&amp;utm_campaign=Badge_Grade)


* [Documentation, Support and Issues](#doc)
* [Contributing](#con)
* [Download and installation](#dow)

Choco-solver is an open-source Java library for Constraint Programming.

Current stable version is 5.0.0-beta.1 (17 Feb 2025).

Choco-solver comes with:
- various type of variables (integer, boolean, set, graph and real),
- various state-of-the-art constraints (alldifferent, count, nvalues, etc.),
- various search strategies, from basic ones (first_fail, smallest, etc.) to most complex (impact-based and activity-based search),
- explanation-based engine, that enables conflict-based back jumping, dynamic backtracking and path repair,

But also, facilities to interact with the search loop, factories to help modelling, many samples, etc.

Choco-solver is distributed under BSD 4-Clause License (Copyright (c) 1999-2025, IMT Atlantique).

Contact:
- [Choco-solver on Discord](https://discord.gg/aH6zxa7e64)

### Overview

```java
// 1. Create a Model
Model model = new Model("my first problem");
// 2. Create variables
IntVar x = model.intVar("X", 0, 5);
IntVar y = model.intVar("Y", 0, 5);
// 3. Create and post constraints thanks to the model
model.element(x, new int[]{5,0,4,1,3,2}, y).post();
// 3b. Or directly through variables
x.add(y).lt(5).post();
// 4. Get the solver
Solver solver = model.getSolver();
// 5. Define the search strategy
solver.setSearch(Search.inputOrderLBSearch(x, y));
// 6. Launch the resolution process
solver.solve();
// 7. Print search statistics
solver.printStatistics();
```

<a name="doc"></a>
## Documentation, Support and Issues

The [latest release](https://github.com/chocoteam/choco-solver/releases/latest) points to binaries and source code.

You can get help on our [google group](https://groups.google.com/forum/#!forum/choco-solver).
Most support requests are answered very fast.

Use the [issue tracker](https://github.com/chocoteam/choco-solver/issues) here on GitHub to report issues.
As far as possible, provide a [Minimal Working Example](https://en.wikipedia.org/wiki/Minimal_Working_Example).

<a name="con"></a>
## Contributing

Anyone can contribute to the project, from the **source code** to the **documentation**.
In order to ease the process, we established a [contribution guide](CONTRIBUTING.md)
that should be reviewed before starting any contribution as
it lists the requirements and good practices to ease the contribution process.


And thank you for giving back to choco-solver.
Please meet our team of cho-coders : 

- [@cprudhom](https://github.com/cprudhom) (Charles Prud'homme)
- [@ArthurGodet](https://github.com/ArthurGodet) (Arthur Godet)
- [@jgFages](https://github.com/jgFages) (Jean-Guillaume Fages)

Supporting Choco with financial aid favors long-term support and development.
Our expenses are varied: fees (GitHub organization, Domain name, etc), funding PhD students or internships, conferences, hardware renewal, ...

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=2GHMNLTP4MCL8)


<a name="dow"></a>
## Download and installation ##

Requirements:
* JDK 8+
* maven 3+

Choco-solver is available on [Maven Central Repository](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.choco-solver%22%20AND%20a%3A%22choco-solver%22),
or directly from the [latest release](https://github.com/chocoteam/choco-solver/releases/latest).

[Snapshot releases](https://oss.sonatype.org/content/repositories/snapshots/org/choco-solver/choco-solver/) are also available for curious.

In the following, we distinguish two usages of Choco:

- as a standalone library: the jar file includes all required dependencies,
- as a library: the jar file excludes all dependencies.

The name of the jar file terms the packaging:
- `choco-solver-4.XX.Y-jar-with-dependencies.jar` or 
- `choco-solver-4.XX.Y.jar`.
- `choco-parsers-4.XX.Y-jar-with-dependencies.jar` or
- `choco-parsers-4.XX.Y-light.jar` or
- `choco-parsers-4.XX.Y.jar`.

The `light` tagged jar file is a version of the `jar-with-dependencies` one with dependencies from this archive.

A [Changelog file](./CHANGES.md) is maintained for each release.

### Inside a maven project ###

Choco is available on Maven Central Repository.
So you only have to edit your `pom.xml` to declare the following library dependency:

```xml
<dependency>
   <groupId>org.choco-solver</groupId>
   <artifactId>choco-solver</artifactId>
   <version>5.0.0-beta.1</version>
</dependency>
```

Note that if you want to test snapshot release, you should update your `pom.xml` with :

```xml
<repository>
    <id>sonatype</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

### As a stand-alone library ###

The jar file contains all required dependencies.
The next step is simply to add the jar file to your classpath of your application.
Note that if your program depends on dependencies declared in the jar file,
you should consider using choco as a library.

### As a library ###

The jar file does not contains any dependencies,
as of being used as a dependency of another application.
The next step is to add the jar file to your classpath of your application and also add the required dependencies.


### Dependencies ###

To declare continuous constraints, [Ibex-2.8.7](http://www.ibex-lib.org/download) needs to be installed
(instructions are given on Ibex website).


### Building from sources ###

The source of the released versions are directly available in the `Tag` section.
You can also download them using github features.
Once downloaded, move to the source directory then execute the following command
to make the jar:

    $ mvn clean package -DskipTests

If the build succeeded, the resulting jar will be automatically
installed in your local maven repository and available in the `target` sub-folders.



_Choco-solver dev team_
