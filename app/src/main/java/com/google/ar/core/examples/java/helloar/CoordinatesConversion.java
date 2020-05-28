package com.google.ar.core.examples.java.helloar;

import android.util.Log;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import static android.support.constraint.Constraints.TAG;

public class CoordinatesConversion {

    public static Pose changeCoordinates(Pose pose, Pose oldCameraPose, Pose newCameraPose) {
        pose = shiftBackward(pose, oldCameraPose.getTranslation());
        pose = rotateQuarternionLeft(pose, oldCameraPose.getRotationQuaternion());
        pose = shiftForward(pose, newCameraPose.getTranslation());
        pose = rotateQuarternionRight(pose, newCameraPose.getRotationQuaternion());
        return pose;
    }

    public static Pose rotateQuarternionRight(Pose pose, float[]q) {
        float[] newT = Pose.makeRotation(q).rotateVector(pose.getTranslation());
        float[] newQ = getRotation(pose.getRotationQuaternion(), q);
        return new Pose(newT, newQ);
    }

    public static Pose rotateQuarternionLeft(Pose pose, float[] q) {
        q[0] = - q[0];
        q[1] = - q[1];
        q[2] = - q[2];
        return rotateQuarternionRight(pose, q);

    }

    public static Pose shiftForward(Pose pose, float[] t) {
        Vector3 v1 = toVector3(pose.getTranslation());
        Vector3 v2 = toVector3(t);
        return new Pose(vector3ToFloat(Vector3.add(v1, v2)), pose.getRotationQuaternion());
    }

    public static Pose shiftBackward(Pose pose, float[] t) {
        Vector3 v1 = toVector3(pose.getTranslation());
        Vector3 v2 = toVector3(t);
        return new Pose(vector3ToFloat(Vector3.subtract(v1, v2)), pose.getRotationQuaternion());
    }

    public static Pose shiftCameraForward(Pose pose) {
        Vector3 v = toVector3(pose.getZAxis()).normalized().scaled(0.05f);
        return shiftBackward(pose, vector3ToFloat(v));
    }

    public static Pose shiftCameraBackward(Pose pose) {
        Vector3 v = toVector3(pose.getZAxis()).normalized().scaled(-0.05f);
        return shiftBackward(pose, vector3ToFloat(v));
    }

    public static Pose shiftCameraRight(Pose pose) {
        Vector3 v = toVector3(pose.getXAxis()).normalized().scaled(0.05f);
        return shiftForward(pose, vector3ToFloat(v));
    }

    public static Pose shiftCameraLeft(Pose pose) {
        Vector3 v = toVector3(pose.getXAxis()).normalized().scaled(-0.05f);
        return shiftForward(pose, vector3ToFloat(v));
    }

    public static Pose rotate(Pose pose) {
        Quaternion current = toQuaternion(pose.getRotationQuaternion());
        Quaternion rotation = new Quaternion(toVector3(pose.getYAxis()), 30f);
        Quaternion result = Quaternion.multiply(current, rotation);
        Log.d(TAG, "rotate: " + current + " " + rotation + " " + result);
        return new Pose(pose.getTranslation(), quaternionToFloat(result));
    }

    private static Quaternion toQuaternion(float[] q) {
        return new Quaternion(q[0], q[1], q[2], q[3]);
    }

    private static Vector3 toVector3(float[] t) {
        return new Vector3(t[0], t[1], t[2]);
    }

    private static float[] quaternionToFloat(Quaternion q) {
        return new float[]{q.x, q.y, q.z, q.w};
    }

    private static float[] vector3ToFloat(Vector3 v) {
        return new float[]{v.x, v.y, v.z};
    }

    private static float[] getRotation(float[] q1, float[] q2) {
        Quaternion Q1 = toQuaternion(q1);
        Quaternion Q2 = toQuaternion(q2);
        return quaternionToFloat(Quaternion.multiply(Q1, Q2));
    }

}
