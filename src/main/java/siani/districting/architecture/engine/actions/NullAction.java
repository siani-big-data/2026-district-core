package siani.districting.architecture.engine.actions;

import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.Precinct;

public record NullAction(Precinct precinct) implements Action {

    @Override
    public int districtId() {
        return 0;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String type() {
        return "null";
    }

    @Override
    public void execute() {

    }
}
