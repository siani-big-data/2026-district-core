<div align="center">

# District Core

### Agent-based Redistricting Model

**A Final Degree Project by Carlos Mathias Osorio Rojas**

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-managed-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![GeoTools 30.2](https://img.shields.io/badge/GeoTools-30.2-2E8B57?style=for-the-badge)](https://geotools.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Status](https://img.shields.io/badge/status-academic%20project-3B82F6?style=for-the-badge)](https://github.com/siani-big-data/2026-district-core)

District Core is a simulation engine for exploring how electoral district configurations evolve when autonomous district agents exchange adjacent precincts under geographic and population constraints.

<img src="assets/tennessee-simulation.gif" alt="Tennessee district simulation" width="720">

</div>

## Overview

This repository provides the core domain model and simulation engine of an agent-based districting system. It transforms precinct-level geospatial and electoral data into a state model, discovers adjacency relationships between precincts, and runs iterative simulations in which districts act as independent agents.

Each simulation step identifies the precincts located along district boundaries, generates the valid transfers available to every agent, resolves competing actions, maintains geographic cohesion, applies the resulting state changes, and stores the outcome for later recovery and analysis.

The included scenario uses Tennessee as the main demonstration case. The engine itself is designed around reusable abstractions for states, districts, precincts, agents, constraints, persistence, and result export.

## Highlights

- **Agent-based Simulation**: every district is represented by an agent that selects from the precinct transfers available at its boundaries.
- **Population-aware actions** — configurable constraints keep proposed transfers within a permitted deviation from the average district population.
- **Adaptive simulation phases** — explorative, transition, and exploitative phases can apply different constraint levels as the simulation advances.
- **Contiguity handling** — transfers are checked against precinct adjacency and disconnected components are detected during state transitions.
- **Incremental boundary updates** — only boundaries affected by a state change are recalculated after each step.
- **Recoverable execution** — periodic snapshots and accumulated deltas allow a simulation to continue from its latest stored state.
- **Analysis-ready output** — every state can be exported to CSV with different columns such as: projected election winner, simulation phase or district population.
- **Extensible agents and constraints** — the `Agent` and `ConstraintCommand` interfaces provide clear extension points for new strategies and rules.

## Simulation workflow

The engine performs the following cycle for every simulation step:

1. Calculate the precincts that form each inter-district boundary.
2. Generate candidate transfers between adjacent districts.
3. Filter candidates through the constraints configured for the current phase.
4. Ask every district agent to select an action, optionally in parallel.
5. Discard conflicting actions that target the same precinct.
6. Validate geographic adjacency and handle disconnected precinct groups.
7. Apply the resulting changes to produce a new state instance.
8. Persist the state and update only the affected boundaries.
9. Export step-level data for subsequent visualization and analysis.

## Input data

District Core works with external geospatial and tabular datasets. These files are intentionally kept outside the repository and are supplied when configuring a simulation.

### Precinct shapefile

The shapefile defines precinct geometry and the initial district assignment. The current reader expects these attributes:

| Attribute | Description |
| --- | --- |
| `UNIQUE_ID` | Unique identifier for each precinct |
| `CONG_DIST` | Initial congressional district identifier |
| Numeric election columns | Precinct-level results retained for electoral calculations |

Election columns whose names begin with `GCON` are aggregated by district when calculating the projected winner.

### District population CSV

A two-column CSV associates every district identifier with its total population:

```csv
district_id,total_population
1,VALUE
2,VALUE
```

### Candidate-to-party CSV

A second two-column CSV maps each election-result column to its corresponding party for result exports:

```csv
candidate_column,party
GCON01CANDIDATE,PARTY
```

## Getting started

### Requirements

- JDK 21 or later
- Apache Maven
- A compatible precinct shapefile and the two CSV mapping files described above

### Build

Clone the repository and compile the project with Maven:

```bash
git clone https://github.com/siani-big-data/2026-district-core.git
cd 2026-district-core
mvn clean compile
```

### Configure the Tennessee example

The executable example is located at:

```text
src/main/java/siani/districting/architecture/engine/Simulator.java
```

Before running it, configure the constants at the beginning of the class with local paths for:

- the state snapshot directory
- the per-step CSV output directory
- the candidate-to-party mapping
- the precinct shapefile
- the district population file

The same class also exposes the snapshot interval and the number of steps to execute. The example applies progressively stricter population deviations across the three simulation phases.

Run the configured scenario with:

```bash
mvn exec:java -Dexec.mainClass="siani.districting.architecture.engine.Simulator"
```

When a stored state is available, the simulation restores it together with its adjacency and precinct information. Otherwise, it builds the initial state from the configured input files.

## Using the engine

`Engine` provides a builder-based API so that a simulation can be assembled with alternative datasets, agents, constraints, persistence policies, and step listeners:

```java
Engine engine = Engine.builder()
        .initialState(state)
        .adjacencySolver(adjacencySolver)
        .agents(agents)
        .actionFilter(actionFilter)
        .serializerManager(serializerManager)
        .stepListener(result -> {
            // Export, visualize, or inspect each completed step.
        })
        .build();

Engine.RunResult result = engine.runSteps(100);
State finalState = result.finalState();
```

Custom decision strategies implement the `Agent` interface, while additional domain rules implement `ConstraintCommand`. This keeps simulation policy separate from the geographic model and execution lifecycle.

## Project structure

```text
src/main/java/siani/districting/
├── architecture/
│   ├── adjacency/       # Precinct adjacency calculation
│   ├── engine/          # Simulation lifecycle, agents and actions
│   ├── geometry/        # Geospatial abstractions and GeoTools adapters
│   ├── model/           # State, district and precinct domain model
│   ├── precinctinfo/    # Precinct-level electoral information
│   └── stores/          # Snapshots, deltas, recovery and CSV export
└── readers/             # Shapefile, population and mapping readers
```

## Simulation output

The Tennessee example produces one CSV file per completed step. Each row represents a precinct and includes:

| Column | Description |
| --- | --- |
| `precinct_id` | Precinct identifier |
| `district_id` | District assigned at the current step |
| `election_winner` | Party associated with the leading congressional candidate in the district |
| `phase` | Current simulation phase |
| `district_population` | Total population of the assigned district |

The persisted snapshots and deltas complement these exports by preserving the complete simulation state and supporting interrupted or multi-session runs.

## Academic context

District Core was developed by **Carlos Mathias Osorio Rojas** as a Final Degree Project (*Trabajo de Fin de Grado*, TFG), with the academic guidance of professors **José Évora Gómez** and **José Juan Hernández Gálvez**. The project brings together geospatial modelling, multi-agent simulation, incremental state management, and electoral data analysis in a single extensible Java engine.

## Authors and academic supervisors

- **Carlos Mathias Osorio Rojas** — Main author
- **José Évora Gómez** — Co-author and academic supervisor
- **José Juan Hernández Gálvez** — Co-author and academic supervisor

## Copyright

Copyright © 2026 Carlos Mathias Osorio Rojas, José Évora Gómez, and José Juan Hernández Gálvez.
