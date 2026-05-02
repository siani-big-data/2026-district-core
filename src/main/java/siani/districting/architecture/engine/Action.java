package siani.districting.architecture.engine;

import siani.districting.architecture.model.Precinct;

public interface Action {

    Precinct precinct();
    int districtId();
    String name();
    String type();
    void execute();
}
