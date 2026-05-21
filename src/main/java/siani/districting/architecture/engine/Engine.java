package siani.districting.architecture.engine;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.actions.BuyAction;
import siani.districting.architecture.engine.environment.IslandDetector;
import siani.districting.architecture.engine.environment.MatrixMultiplicationBoundaryCalculator;
import siani.districting.architecture.engine.environment.StateFactory;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.stores.SerializerManager;
import siani.districting.architecture.stores.StateDelta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Engine {

    private State currentState;
    private AdjacencySolver adjacencySolver;
    private List<Agent> agents;
    private ActionFilter actionFilter;
    private MatrixMultiplicationBoundaryCalculator boundariesCalculator;
    private Map<Integer, Map<Integer, Set<Precinct>>> borders;
    private SerializerManager serializerManager;
    private StepListener stepListener;
    private int currentStep;
    private boolean chooseActionsInParallel;

    public Engine() {
        this.agents = Collections.emptyList();
        this.boundariesCalculator = new MatrixMultiplicationBoundaryCalculator();
        this.chooseActionsInParallel = true;
    }

    private Engine(Builder builder) {
        this.currentState = Objects.requireNonNull(builder.initialState, "initialState is required");
        this.adjacencySolver = Objects.requireNonNull(builder.adjacencySolver, "adjacencySolver is required");
        this.agents = List.copyOf(Objects.requireNonNull(builder.agents, "agents are required"));
        this.actionFilter = builder.actionFilter;
        this.boundariesCalculator = builder.boundariesCalculator;
        this.serializerManager = builder.serializerManager;
        this.stepListener = builder.stepListener;
        this.chooseActionsInParallel = builder.chooseActionsInParallel;
        this.currentStep = builder.initialStep != null
                ? builder.initialStep
                : serializerStepOrZero(builder.serializerManager);
    }

    public static Builder builder() {
        return new Builder();
    }

    public State currentState() {
        return currentState;
    }

    public int currentStep() {
        return currentStep;
    }

    public Map<Integer, Map<Integer, Set<Precinct>>> borders() {
        ensureBoundariesCalculated();
        return borders;
    }

    public RunResult runUntil(int maxStepExclusive) throws IOException {
        List<StepResult> results = new ArrayList<>();
        long start = System.currentTimeMillis();

        while (currentStep < maxStepExclusive) {
            results.add(step());
        }

        return new RunResult(
                currentStep,
                currentState,
                results,
                System.currentTimeMillis() - start
        );
    }

    public RunResult runSteps(int numberOfSteps) throws IOException {
        if (numberOfSteps < 0) {
            throw new IllegalArgumentException("numberOfSteps must be >= 0");
        }

        return runUntil(currentStep + numberOfSteps);
    }

    public StepResult step() throws IOException {
        ensureReadyToRun();
        ensureBoundariesCalculated();

        int stepNumber = currentStep + 1;
        long start = System.currentTimeMillis();
        List<Action> selectedActions = chooseActions(stepNumber);
        List<Action> finalActions = IslandDetector.findIslands(currentState, adjacencySolver, selectedActions);

        StateDelta delta = new StateDelta(finalActions);
        State previousState = currentState;
        State newState = StateFactory.applyDelta(previousState, delta);

        serializerManager.serialize(previousState, delta, newState);

        Set<Precinct> changedPrecincts = delta.differencies().keySet();
        borders = boundariesCalculator.updateMatrix(newState, changedPrecincts);
        currentState = newState;
        currentStep = stepNumber;

        ActionFilter.EpochName epochName = actionFilter != null
                ? actionFilter.getEpochNameFromStep(stepNumber)
                : null;

        StepResult result = new StepResult(
                stepNumber,
                selectedActions.size(),
                finalActions.size(),
                changedPrecincts.size(),
                epochName,
                System.currentTimeMillis() - start,
                currentState,
                delta
        );

        if (stepListener != null) {
            stepListener.onStep(result);
        }

        return result;
    }

    private List<Action> chooseActions(int stepNumber) {
        Stream<Agent> stream = chooseActionsInParallel ? agents.parallelStream() : agents.stream();
        State loopState = currentState;
        Map<Integer, Map<Integer, Set<Precinct>>> currentBorders = borders;

        return stream
                .map(agent -> chooseAction(agent, loopState, currentBorders, stepNumber))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Action::precinct))
                .values()
                .stream()
                .filter(actionsForPrecinct -> actionsForPrecinct.size() == 1)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private Action chooseAction(Agent agent,
                                State state,
                                Map<Integer, Map<Integer, Set<Precinct>>> currentBorders,
                                int stepNumber) {
        List<Action> possibleActions = currentBorders.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != agent.id())
                .map(entry -> entry.getValue().get(agent.id()))
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(precinct -> new BuyAction(agent.id(), precinct))
                .collect(Collectors.toList());

        if (actionFilter != null) {
            possibleActions = actionFilter.filterActions(
                    state,
                    adjacencySolver,
                    possibleActions,
                    stepNumber
            );
        }

        return agent.choose(new HashSet<>(possibleActions));
    }

    private void ensureReadyToRun() {
        Objects.requireNonNull(currentState, "Engine has no current state. Use Engine.builder().initialState(...).");
        Objects.requireNonNull(adjacencySolver, "Engine has no adjacency solver. Use Engine.builder().adjacencySolver(...).");

        if (agents == null || agents.isEmpty()) {
            throw new IllegalStateException("Engine has no agents. Use Engine.builder().agents(...).");
        }
    }

    private void ensureBoundariesCalculated() {
        if (borders == null) {
            borders = boundariesCalculator.calculateBoundariesForFirstTime(currentState, adjacencySolver);
        }
    }

    private static int serializerStepOrZero(SerializerManager serializerManager) {
        return serializerManager == null ? 0 : serializerManager.getStepCount();
    }

    public static class Builder {
        private State initialState;
        private AdjacencySolver adjacencySolver;
        private List<Agent> agents = Collections.emptyList();
        private ActionFilter actionFilter;
        private MatrixMultiplicationBoundaryCalculator boundariesCalculator = new MatrixMultiplicationBoundaryCalculator();
        private SerializerManager serializerManager;
        private StepListener stepListener;
        private Integer initialStep;
        private boolean chooseActionsInParallel = true;

        private Builder() {
        }

        public Builder initialState(State initialState) {
            this.initialState = initialState;
            return this;
        }

        public Builder adjacencySolver(AdjacencySolver adjacencySolver) {
            this.adjacencySolver = adjacencySolver;
            return this;
        }

        public Builder agents(List<Agent> agents) {
            this.agents = agents;
            return this;
        }

        public Builder actionFilter(ActionFilter actionFilter) {
            this.actionFilter = actionFilter;
            return this;
        }

        public Builder boundariesCalculator(MatrixMultiplicationBoundaryCalculator boundariesCalculator) {
            this.boundariesCalculator = Objects.requireNonNull(boundariesCalculator);
            return this;
        }

        public Builder serializerManager(SerializerManager serializerManager) {
            this.serializerManager = serializerManager;
            return this;
        }

        public Builder stepListener(StepListener stepListener) {
            this.stepListener = stepListener;
            return this;
        }

        public Builder initialStep(int initialStep) {
            if (initialStep < 0) {
                throw new IllegalArgumentException("initialStep must be >= 0");
            }
            this.initialStep = initialStep;
            return this;
        }

        public Builder chooseActionsInParallel(boolean chooseActionsInParallel) {
            this.chooseActionsInParallel = chooseActionsInParallel;
            return this;
        }

        public Engine build() {
            return new Engine(this);
        }
    }

    @FunctionalInterface
    public interface StepListener {
        void onStep(StepResult result) throws IOException;
    }

    public record StepResult(int step,
                             int selectedActions,
                             int appliedActions,
                             int changedPrecincts,
                             ActionFilter.EpochName epochName,
                             long elapsedMillis,
                             State state,
                             StateDelta delta) {
    }

    public record RunResult(int finalStep,
                            State finalState,
                            List<StepResult> steps,
                            long elapsedMillis) {
    }
}
