package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.Anchor;

public class AnchorModel {
    public final Anchor anchor;
    public int model;

    public AnchorModel(Anchor a, int model) {
        this.anchor = a;
        this.model = model;
    }
}
