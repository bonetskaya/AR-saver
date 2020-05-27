package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.Pose;

public class RawAnchorModel {
    private Pose pose;
    public int model;

    public RawAnchorModel(Pose p, int m) {
        this.pose = p;
        this.model = m;
    }

    public Pose getPose() {
        return pose;
    }

    public void setPose(Pose pose) {
        this.pose = pose;
    }
}
