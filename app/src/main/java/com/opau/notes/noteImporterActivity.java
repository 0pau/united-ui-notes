package com.opau.notes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class noteImporterActivity extends AppCompatActivity {

    DbHelper helper;
    SQLiteDatabase db;

    String content;
    int targetFolder;
    Boolean checkList;
    separator sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_importer);

        targetFolder = -1;
        checkList = false;

        helper = new DbHelper(this);
        db = helper.getWritableDatabase();

        Intent i = getIntent();

        Uri uri = i.getData();

        Toolbar tb = findViewById(R.id.toolbar3);
        tb.setSubtitle(uri.getLastPathSegment());

        try {
            InputStream in = getContentResolver().openInputStream(uri);


            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();

            ArrayList<String> lines = new ArrayList<>();

            for (String line; (line = r.readLine()) != null; ) {
                lines.add(line);
            }

            for (int j = 0; j < lines.size(); j++) {
                total.append(lines.get(j));
                if (j != lines.size()-1) {
                    total.append("\n");
                }
            }

            content = total.toString();

            EditText tv = findViewById(R.id.textPreview);
            tv.setText(content);
            tv.setEnabled(false);

        }catch (Exception e) {

        }

        Spinner note_type_spinner = findViewById(R.id.note_type_spinner);
        note_type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout itemSepOptions = findViewById(R.id.item_separate_setting);
                NestedScrollView plainPreview = findViewById(R.id.plainNotePreview);
                ListView checkPreview = findViewById(R.id.checkListPreview);
                if (i == 0) {
                    itemSepOptions.setVisibility(View.GONE);
                    plainPreview.setVisibility(View.VISIBLE);
                    checkPreview.setVisibility(View.GONE);
                    checkList = false;
                } else {
                    itemSepOptions.setVisibility(View.VISIBLE);
                    plainPreview.setVisibility(View.GONE);
                    checkPreview.setVisibility(View.VISIBLE);

                    if (checkPreview.getAdapter() == null) {
                        createCheckList(separator.NEWLINE);
                    }

                    checkList = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner item_sep_spinner = findViewById(R.id.item_sep_spinner);
        item_sep_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        createCheckList(separator.NEWLINE);
                        break;
                    case 1:
                        createCheckList(separator.COMMA);
                        break;
                    case 2:
                        createCheckList(separator.SEMICOLON);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    public void createCheckList(separator s) {

        sp = s;

        String[] items = content.split(s.toString());

        checkListAdapter a = new checkListAdapter(this, items);
        ListView checkPreview = findViewById(R.id.checkListPreview);
        checkPreview.setAdapter(a);

    }

    public String[] getList(separator s) {

        String[] items = content.split(s.toString());

        return items;

    }

    public void save(View v) {

        ContentValues cv = new ContentValues();

        EditText title = findViewById(R.id.title);
        String t = title.getText().toString();

        if (t.isEmpty()) {
            t = "Untitled";
        }
        cv.put("name", t);

        if (!checkList) {
            cv.put("type", "note");
            cv.put("content", content);
        } else {
            cv.put("type", "check");

            ListView checkPreview = findViewById(R.id.checkListPreview);
            checkListAdapter a = (checkListAdapter) checkPreview.getAdapter();

            ArrayList<item> items = new ArrayList<>();

            String[] itms = getList(sp);

            for (int i = 0; i < itms.length; i++) {
                item it = new item();
                it.title = itms[i];
                it.checked = false;
                items.add(it);
            }

            Gson gs = new Gson();
            String s = gs.toJson(items);
            cv.put("content", s);

        }

        cv.put("folder_id", targetFolder);
        db.insert("notes", null, cv);

        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    //region CheckList

    public class checkListAdapter extends ArrayAdapter<String> {
        public checkListAdapter(@NonNull Context context, String[] list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inf = LayoutInflater.from(getContext());

            View v = inf.inflate(R.layout.check_preview_item, null);

            TextView et = v.findViewById(R.id.check_text);

            et.setText(getItem(position));


            return v;
        }
    }

    public class item {
        public String title;
        public Boolean checked;

        public item() {
            title = "";
            checked = false;
        }
    }

    enum separator {NEWLINE,COMMA,SEMICOLON;

        @NonNull
        @Override
        public String toString() {

            switch (this) {
                case NEWLINE:
                    return "\n";
                case COMMA:
                    return ",";
                case SEMICOLON:
                    return ";";
            }

            return super.toString();
        }
    }

    public void folderChooser(View v) {
        showFolderChooserDialog();
    }

    public void cancel(View v) {
        finish();
    }
    void showFolderChooserDialog() {
        LayoutInflater inf = getLayoutInflater();
        View chooser = inf.inflate(R.layout.folder_selector_sheet, null);

        BottomSheetDialog bsd = new BottomSheetDialog(this, R.style.BottomSheet);
        bsd.setContentView(chooser);

        ListView lv = chooser.findViewById(R.id.folder_list);

        ArrayList<folder> folders = getFolderList();

        lv.setAdapter(new folderAdapter(this, folders));

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                folder f = folders.get(i);
                targetFolder = f.id;
                TextView folderName = findViewById(R.id.folderName);
                folderName.setText(f.name);

                ImageView folderBadge = findViewById(R.id.folderBadge);
                folderBadge.setColorFilter(ContextCompat.getColor(bsd.getContext(), f.color));

                bsd.dismiss();
            }
        });

        LinearLayout create_folder_item = chooser.findViewById(R.id.create_folder_item);
        create_folder_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFolderCreatorSheet();
                bsd.dismiss();
            }
        });

        bsd.show();
    }

    public class folderAdapter extends ArrayAdapter<folder> {

        public folderAdapter(@NonNull Context context, ArrayList<folder> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            folder f = getItem(position);

            LayoutInflater inf = LayoutInflater.from(getContext());
            View v = inf.inflate(R.layout.category_chooser_entry, null);

            TextView folder_name = v.findViewById(R.id.folder_name);
            ImageView folder_color = v.findViewById(R.id.folder_color);

            folder_name.setText(f.name);
            folder_color.setColorFilter(ContextCompat.getColor(getContext(), f.color));

            return v;
        }
    }

    ArrayList<folder> getFolderList() {
        ArrayList<folder> al = new ArrayList<>();

        folder root = new folder();
        root.id = -1;
        root.color = R.color.folderColorUnknown;
        root.name = getString(R.string.uncategorised);

        al.add(root);

        Cursor c = db.query("folders", new String[]{"id", "name", "color"}, null, null, null, null, "name ASC");

        c.moveToPosition(-1);

        while (c.moveToNext()) {
            folder f = new folder();
            f.id = c.getInt(0);

            if (f.id == -1) {
                continue;
            }

            f.name = c.getString(1);
            f.color = c.getInt(2);
            al.add(f);
        }

        return al;
    };

    public class folder {
        public int id;
        public String name;
        public int color;
    }

    public class folderColorAdapter extends ArrayAdapter<NotesApp.folderColor> {

        public folderColorAdapter(@NonNull Context context, ArrayList<NotesApp.folderColor> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            NotesApp.folderColor col = getItem(position);

            LayoutInflater inf = LayoutInflater.from(getContext());
            View v = inf.inflate(R.layout.folder_color_entry, null);

            ImageView iv = v.findViewById(R.id.folder_color);
            iv.setColorFilter(ContextCompat.getColor(getContext(), col.resource));

            return v;
        }
    }

    public void showFolderCreatorSheet() {

        final int[] sel = {-1};

        LayoutInflater inf = getLayoutInflater();
        View chooser = inf.inflate(R.layout.folder_creator_sheet, null);

        BottomSheetDialog bsd = new BottomSheetDialog(this, R.style.BottomSheet);
        bsd.setContentView(chooser);

        GridView lv = chooser.findViewById(R.id.color_list);

        folderColorAdapter fcadapter = new folderColorAdapter(this, NotesApp.colorList);

        lv.setAdapter(fcadapter);

        chooser.findViewById(R.id.folder_create_cancel).setOnClickListener((view)-> {bsd.dismiss();});

        Button save = chooser.findViewById(R.id.folder_saver_button);
        save.setEnabled(false);

        EditText et = chooser.findViewById(R.id.folder_name_editor);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (sel[0] != -1 && !charSequence.toString().isEmpty()) {
                    save.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                sel[0] = i;
                if (!et.getText().toString().isEmpty()) {
                    save.setEnabled(true);
                }
            }
        });

        save.setOnClickListener((view)-> {

            NotesApp.folderColor col = fcadapter.getItem(sel[0]);

            ContentValues cv = new ContentValues();
            cv.put("name", et.getText().toString());
            cv.put("color", col.resource);

            db.insert("folders", null, cv);

            bsd.dismiss();
            showFolderChooserDialog();

        });


        bsd.show();
    }

    //endregion
}