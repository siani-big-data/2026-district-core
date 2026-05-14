package siani.districting.architecture.engine.environment.actionfiltering.constraints;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.State;

import java.util.List;

public interface ConstraintCommand {
    List<Action> filter(State state, AdjacencySolver solver, List<Action> actionList);
}
