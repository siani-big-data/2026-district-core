package siani.districting.architecture.engine.actions;

import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.Precinct;

public record BuyAction(int districtId, Precinct precinct) implements Action {

    public Precinct precinct() {
        return precinct;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String type() {
        return "buy";
    }

    @Override
    public void execute() {

    }
}
