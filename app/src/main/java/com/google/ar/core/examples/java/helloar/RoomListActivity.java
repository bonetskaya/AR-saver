package com.google.ar.core.examples.java.helloar;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import static android.support.constraint.Constraints.TAG;

public class RoomListActivity extends ListActivity {

    ArrayList<RoomData> rooms;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        dbHelper = new DBHelper(this);

        rooms = dbHelper.getRooms();

        ArrayList<String> data = new ArrayList<>();

        for (int i = 0; i < rooms.size(); i++) {
            data.add(rooms.get(rooms.size() - i - 1).name);
        }

        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<String>(this,
                R.layout.conversation_item,
                R.id.text1,
                data);
        setListAdapter(adapter);

        ListView lv = findViewById(android.R.id.list);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> l, View v, final int position, long id) {
                LayoutInflater li = LayoutInflater.from(RoomListActivity.this);
                View promptsView = li.inflate(R.layout.dialog_delete, null);

                //Создаем AlertDialog
                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(RoomListActivity.this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);

                //Настраиваем prompt.xml для нашего AlertDialog:
                mDialogBuilder.setView(promptsView);

                //Настраиваем сообщение в диалоговом окне:
                mDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        String path = dbHelper.deleteRoom(rooms.get(rooms.size() - position - 1).id);
                                        File file = new File(path);
                                        file.delete();
                                        data.remove(position);
                                        rooms.remove(rooms.size() - position - 1);
                                        adapter.notifyDataSetChanged();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                //Создаем AlertDialog:
                AlertDialog alertDialog = mDialogBuilder.create();

                //и отображаем его:
                alertDialog.show();
                return true;
            }
        });

    }


    @Override
    protected void onListItemClick(ListView l, View view, int position, long id) {
        Intent intent = new Intent(this, ResolveActivity.class);
        intent.putExtra("roomID", rooms.get(rooms.size() - position - 1).id);
        startActivity(intent);
    }


}
