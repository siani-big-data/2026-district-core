package siani.districting.architecture.engine.actions;

import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.Precinct;

public record SellAction(int districtId, Precinct precinct) implements Action {
    @Override
    public int districtId() {
        return districtId;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String type() {
        return "sell";
    }

    @Override
    public void execute() {

    }
}
