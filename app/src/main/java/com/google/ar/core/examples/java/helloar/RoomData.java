package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.Pose;

public class RoomData {
    public Pose cameraPose;
    public String photoPath;
    public int height;
    public int width;
    public long id;
    public String name;

    public RoomData(Pose pose, int height, int width, String path) {
        this.cameraPose = pose;
        this.height = height;
        this.width = width;
        this.photoPath = path;
    }

    public RoomData() {

    }
}
