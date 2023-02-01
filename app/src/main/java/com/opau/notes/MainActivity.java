package com.opau.notes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    DbHelper helper;
    SQLiteDatabase db;
    int current_folder = -1;
    CollapsingToolbarLayout title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helper = new DbHelper(this);
        db = helper.getWritableDatabase();

        title = findViewById(R.id.title);

        FloatingActionButton fb = findViewById(R.id.fab);
        fb.setOnClickListener((view)-> {

            LayoutInflater inf = getLayoutInflater();

            View composer = inf.inflate(R.layout.compose_sheet, null);

            BottomSheetDialog bsd = new BottomSheetDialog(this, R.style.BottomSheet);
            bsd.setContentView(composer);

            CardView createFolder = composer.findViewById(R.id.create_folder);
            createFolder.setOnClickListener((view2)-> {

                bsd.dismiss();

                View dialogLayout = getLayoutInflater().inflate(R.layout.create_folder_dialog, null);
                EditText folderName = dialogLayout.findViewById(R.id.folderName);

                AlertDialog d = new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.create_folder).
                        setPositiveButton(R.string.create, (dialog, which) -> {
                            String name = folderName.getText().toString();
                            createFolderIn(current_folder, name);
                            dialog.dismiss();
                        }).
                        setNegativeButton(R.string.cancel, (dialog, which)-> {
                            dialog.dismiss();
                        }).
                        setView(dialogLayout).create();

                d.show();

                folderName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().isEmpty()) {
                            d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        } else {
                            d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable editable) {}
                });

                d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

            });

            CardView create_checklist = composer.findViewById(R.id.create_checklist);
            create_checklist.setOnClickListener((view2)-> {
                bsd.dismiss();
                Intent intent = new Intent(this, noteEditorActivity.class);
                intent.putExtra("parent", current_folder);
                intent.putExtra("isCheckList", true);
                startActivity(intent);
            });

            CardView create_note = composer.findViewById(R.id.create_note);
            create_note.setOnClickListener((view2)-> {
                bsd.dismiss();
                Intent intent = new Intent(this, noteEditorActivity.class);
                intent.putExtra("parent", current_folder);
                startActivity(intent);
            });

            bsd.show();
        });

        loadFolder();

        folderLists fl = getFoldersIn(-1);
        ArrayList<Integer> folder_ids = fl.ids;

        Spinner spin = findViewById(R.id.item_sep_spinner);
        refreshFolderSpinner(spin, fl);

        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                current_folder = folder_ids.get(i);
                loadFolder();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

    }

    void refreshFolderSpinner(Spinner spin, folderLists fl) {

        ArrayList<String> folder_names = fl.names;

        ArrayAdapter spinner_adapter = new ArrayAdapter(this, R.layout.spinner_item, R.id.text1, folder_names);
        spin.setAdapter(spinner_adapter);
    }

    public class GridAdapter extends ArrayAdapter<item> {
        public GridAdapter(Context c, ArrayList<item> items) {
            super(c, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inf = LayoutInflater.from(getContext());
            View v;

            item i = getItem(position);

            if (i.isFolder) {
                v = inf.inflate(R.layout.folder_entry, null);
            } else {
                v = inf.inflate(R.layout.note_entry, null);
            }

            v.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView title = v.findViewById(R.id.entry_name);
            title.setText(i.name);

            TextView subtitle = v.findViewById(R.id.entry_sub);

            TextView folderName = v.findViewById(R.id.entry_folder);

            CardView itemCard = v.findViewById(R.id.item_card);

            LinearLayout card_layout = v.findViewById(R.id.card_layout);

            if (i.type.equals("note")) {
                subtitle.setText(i.content);

                itemCard.setOnClickListener((view)-> {
                    Intent intent = new Intent(getContext(), noteEditorActivity.class);
                    intent.putExtra("id", i.id);
                    intent.putExtra("name", i.name);
                    intent.putExtra("parent", i.parent);
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(MainActivity.this, card_layout, "note");
                    getContext().startActivity(intent);
                });

            } else if (i.type.equals("check")) {

                ArrayList<noteEditorActivity.item> items = new ArrayList<>();
                Gson gs = new Gson();
                Type listType = new TypeToken<ArrayList<noteEditorActivity.item>>() {
                }.getType();
                items = gs.fromJson(i.content, listType);

                String preview = "";

                for (int j=0; j<items.size();j++) {
                    preview += items.get(j).title;
                    if (j != items.size()-1) {
                        preview += ", ";
                    }
                }

                if (items.size() != 0) {
                    subtitle.setText(preview);
                } else {
                    subtitle.setText(R.string.empty_checklist);
                }


                itemCard.setOnClickListener((view)-> {
                    Intent intent = new Intent(getContext(), noteEditorActivity.class);
                    intent.putExtra("id", i.id);
                    intent.putExtra("name", i.name);
                    intent.putExtra("parent", i.parent);
                    intent.putExtra("isCheckList", true);
                    getContext().startActivity(intent);
                });

            }

            folderName.setText(i.folder_name);

            ImageView icon = v.findViewById(R.id.entry_folder_icon);
            icon.setColorFilter(ContextCompat.getColor(getContext(), i.color));

            return v;
        }
    }

    void loadFolder() {
        GridView gv = findViewById(R.id.grid);

        ArrayList<item> items = getNotesIn(current_folder);

        items.sort(new Comparator<item>() {
            @Override
            public int compare(item t1, item t2) {

                return t1.name.compareTo(t2.name);
            }
        });

        gv.setAdapter(new GridAdapter(this, items));
    }

    void createFolderIn(int folder, String name) {

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("folder_id", folder);

        db.insert("folders", null, cv);

        if (current_folder == folder) {
            loadFolder();
        }

    }

    folderLists getFoldersIn(int folder) {

        ArrayList<String> name = new ArrayList<>();
        ArrayList<Integer> id = new ArrayList<>();

        name.add(getString(R.string.notes_all));
        id.add(-1);

        Cursor c = db.query("folders", new String[]{"id", "name"}, null, null,null,null,null);
        c.moveToPosition(-1);

        while (c.moveToNext()) {

            if (c.getInt(0) == -1) {
                continue;
            }

            name.add(c.getString(1));
            id.add(c.getInt(0));
        }

        return new folderLists(name, id);
    }

    class folderLists {
        public ArrayList<String> names;
        public ArrayList<Integer> ids;
        public folderLists(ArrayList<String>n,ArrayList<Integer>i) {
            names = n;
            ids = i;
        }
    }

    ArrayList<item> getNotesIn(int folder) {

        ArrayList<item> al = new ArrayList<>();

        //Cursor c = db.query("notes, folders", new String[]{"notes.id", "notes.name", "notes.folder_id", "content", "type", "folders.name AS folder_name", "folders.color"}, "notes.folder_id = ? AND notes.folder_id = ?", new String[]{String.valueOf(folder), "folders.id"},null,null,null);

        Cursor c;

        if (folder == -1) {
            c = db.rawQuery("SELECT notes.id, notes.name, notes.folder_id, content, type, folders.name, folders.color FROM notes, folders WHERE folders.id = notes.folder_id", null);
        } else {
            c = db.rawQuery("SELECT notes.id, notes.name, notes.folder_id, content, type, folders.name, folders.color FROM notes, folders WHERE folders.id = notes.folder_id AND notes.folder_id = " + folder, null);
        }

        c.moveToPosition(-1);

        while (c.moveToNext()) {
            item f = new item();
            f.isFolder = false;
            f.name = c.getString(1);
            f.id = c.getInt(0);
            f.parent = c.getInt(2);
            f.content = c.getString(3);
            f.type = c.getString(4);

            Log.i("COLUMNS", f.name);

            if (f.parent != -1) {
                f.folder_name = c.getString(5);
                f.color = c.getInt(6);
            } else {
                f.folder_name = getString(R.string.uncategorised);
                f.color = R.color.folderColorUnknown;
            }


            al.add(f);
        }

        return al;
    }

    public class item {
        public String name;
        public String content;
        public boolean isFolder;
        public int id;
        public int parent;
        public String type;
        public String folder_name;
        public Integer color;
    }


    @Override
    public void onBackPressed() {

        if (current_folder == -1) {
            super.onBackPressed();
        } else {
            Cursor c = db.query("folders", new String[]{"id", "folder_id"}, "id = ?", new String[]{String.valueOf(current_folder)}, null, null, null);
            c.moveToFirst();

            current_folder = c.getInt(1);
            loadFolder();

        }

    }

    @Override
    protected void onResume() {

        GridView gv = findViewById(R.id.grid);
        gv.setEnabled(false);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gv.setEnabled(true);
                folderLists fl = getFoldersIn(-1);
                Spinner spin = findViewById(R.id.item_sep_spinner);
                refreshFolderSpinner(spin,fl);
                loadFolder();
            }
        }, 250);


        super.onResume();
    }
}