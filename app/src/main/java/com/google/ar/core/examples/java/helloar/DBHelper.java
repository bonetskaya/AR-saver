package com.google.ar.core.examples.java.helloar;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.ar.core.Pose;

import java.util.ArrayList;

import static android.support.constraint.Constraints.TAG;
import static com.google.ar.core.examples.java.helloar.BytesConverter.byteToFloat;
import static com.google.ar.core.examples.java.helloar.BytesConverter.floatToByte;

class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table objects ("
                + "id integer primary key autoincrement,"
                + "t blob,"
                + "q blob,"
                + "model integer,"
                + "roomID integer" + ");");

        db.execSQL("create table rooms ("
                + "id integer primary key autoincrement,"
                + "roomName text,"
                + "cameraT blob,"
                + "cameraQ blob,"
                + "width integer,"
                + "height integer,"
                + "photoPath text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*db.execSQL("drop table objects;");
        db.execSQL("drop table rooms;");
        db.execSQL("create table objects ("
                + "id integer primary key autoincrement,"
                + "t blob,"
                + "q blob,"
                + "model integer,"
                + "roomID integer" + ");");

        db.execSQL("create table rooms ("
                + "id integer primary key autoincrement,"
                + "roomName text,"
                + "cameraT blob,"
                + "cameraQ blob,"
                + "width integer,"
                + "height integer,"
                + "photoPath text" + ");");*/
    }

    public long addRoom(String roomName, Pose cameraPose, String photoPath, int width, int height) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("photoPath", photoPath);
        cv.put("roomName", roomName);
        cv.put("cameraT", floatToByte(cameraPose.getTranslation()));
        cv.put("cameraQ", floatToByte(cameraPose.getRotationQuaternion()));
        cv.put("width", width);
        cv.put("height", height);
        Log.d(TAG, "HEIGHT" + height);
        long ID = getWritableDatabase().insert("rooms", null, cv);
        if (roomName.replaceAll("\\s+", "").equals("")) {
            cv = new ContentValues();
            cv.put("roomName", "Room " + ID);
            db.update("rooms", cv, "id = ?", new String[]{"" + ID});
        }
        close();
        return ID;
    }

    public void addObjects(long roomID, ArrayList<AnchorModel> anchors) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        while (anchors.size() > 0) {
            Pose pose = anchors.get(0).anchor.getPose();
            cv.put("t", floatToByte(pose.getTranslation()));
            cv.put("q", floatToByte(pose.getRotationQuaternion()));
            cv.put("roomID", roomID);
            cv.put("model", anchors.get(0).model);
            db.insert("objects", null, cv);
            anchors.remove(0);
        }
        close();
    }

    public ArrayList<RawAnchorModel> getObjects(long roomID) {
        Log.d(TAG, "getObjects: " + roomID);
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<RawAnchorModel> anchorModels = new ArrayList<>();
        Cursor c = db.rawQuery("select * from objects where roomID = " + roomID + ";", null);
        if (c.moveToFirst()) {

            int iT = c.getColumnIndex("t");
            int iQ = c.getColumnIndex("q");
            int iM = c.getColumnIndex("model");

            do {
                float[] t = byteToFloat(c.getBlob(iT));
                float[] q = byteToFloat(c.getBlob(iQ));
                int model = c.getInt(iM);
                Pose pose = new Pose(t, q);
                anchorModels.add(new RawAnchorModel(pose, model));
            } while (c.moveToNext());
        }
        c.close();
        close();
        return anchorModels;
    }


    public RoomData getRoom(long roomID) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from rooms where id = " + roomID + ";", null);
        RoomData data = new RoomData();
        if (c.moveToFirst()) {
            int iT = c.getColumnIndex("cameraT");
            int iQ = c.getColumnIndex("cameraQ");
            int iW = c.getColumnIndex("width");
            int iH = c.getColumnIndex("height");
            int iP = c.getColumnIndex("photoPath");

            data.cameraPose = new Pose(byteToFloat(c.getBlob(iT)), byteToFloat(c.getBlob(iQ)));
            data.width = c.getInt(iW);
            data.height = c.getInt(iH);
            data.photoPath = c.getString(iP);
        }
        c.close();
        close();
        return data;
    }

    public long getNextRoomID() {
        SQLiteDatabase db = getWritableDatabase();

        long result = -1;
        String query = "SELECT * FROM SQLITE_SEQUENCE";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(cursor.getColumnIndex("name")).equals("rooms")) {
                    result = cursor.getInt(cursor.getColumnIndex("seq"));
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        close();
        return result;
    }


    public ArrayList<RoomData> getRooms() {
        ArrayList<RoomData> rooms = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from rooms;", null);
        if (c.moveToFirst()) {
            int iRN = c.getColumnIndex("roomName");
            int iID = c.getColumnIndex("id");
            do {
                RoomData room = new RoomData();
                room.id = c.getLong(iID);
                room.name = c.getString(iRN);
                rooms.add(room);
            } while (c.moveToNext());
        }
        c.close();
        close();
        return rooms;
    }

    public String deleteRoom(long roomID) {
        Log.d(TAG, "deleteRoom: " + roomID);
        String path = new String();
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from objects where roomID = " + roomID + ";");
        Cursor c = db.rawQuery("select photoPath from rooms where id = " + roomID + ";", null);
        if (c.moveToFirst()) {
            int iP = c.getColumnIndex("photoPath");
            path = c.getString(iP);
        }
        c.close();
        db.execSQL("delete from rooms where id = " + roomID + ";");
        close();
        return path;
    }
}