package com.opau.notes;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Image;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class noteEditorActivity extends AppCompatActivity {

    int id;
    int parent;
    DbHelper helper;
    SQLiteDatabase db;
    Boolean deleted;
    Boolean isCheckList;
    Gson gs;
    checkListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        deleted = false;

        //region initialize toolbar

        Toolbar tb = findViewById(R.id.toolbar2);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener((view)-> {
            finish();
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.delete_note) {
                    db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
                    deleted = true;
                    finish();
                } else if (item.getItemId() == R.id.share_note) {
                    share_note();
                }

                return false;
            }
        });

        //endregion

        gs = new Gson();

        helper = new DbHelper(this);
        db = helper.getWritableDatabase();

        String name = getIntent().getStringExtra("name");
        id = getIntent().getIntExtra("id", -1);
        parent = getIntent().getIntExtra("parent", -1);
        isCheckList = getIntent().getBooleanExtra("isCheckList", false);

        //region load details

        EditText titleEditor = findViewById(R.id.titleEditor);
        EditText textEditor = findViewById(R.id.noteEditorView);

        if (id != -1) {
            titleEditor.setText(name);
        }

        refreshFolderInfo();
        CardView category_card_view = findViewById(R.id.category_card_view);
        category_card_view.setOnClickListener((view) -> {showFolderChooserDialog();});

        if (isCheckList) {
            textEditor.setVisibility(View.GONE);
            View checklistEditorView = findViewById(R.id.checklistEditorView);
            checklistEditorView.setVisibility(View.VISIBLE);
        }

        if (id != -1 && !isCheckList) {
            Cursor c = db.query("notes", new String[]{"id, content"}, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
            c.moveToFirst();
            String content = c.getString(1);
            textEditor.setText(content);
        } else if (isCheckList) {

            ArrayList<item> items = new ArrayList<>();

            if (id != -1) {
                Cursor c = db.query("notes", new String[]{"id, content"}, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
                c.moveToFirst();
                String content = c.getString(1);

                Type listType = new TypeToken<ArrayList<item>>() {
                }.getType();
                items = gs.fromJson(content, listType);
            }

            ListView list = findViewById(R.id.list);
            adapter = new checkListAdapter(this, items);
            list.setAdapter(adapter);

            View v = findViewById(R.id.add_item);
            v.setOnClickListener((view)-> {
                adapter.add(new item());
            });
        }

        //endregion
    }

    void share_note() {
        EditText textEditor = findViewById(R.id.noteEditorView);

        String share_content = "";

        if (!isCheckList) {
            share_content = textEditor.getText().toString();
        } else {

            //getting checklist items

            for (int i = 0; i < adapter.getCount(); i++) {

                String t = adapter.getItem(i).title;

                if (i != 0) {
                    share_content += ", "+t;
                } else {
                    share_content += t;
                }
            }

        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, share_content);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    //region Basic functionality

    void refreshFolderInfo() {

        ImageView folder_icon = findViewById(R.id.entry_folder_icon);
        TextView folder_name = findViewById(R.id.folder_name);

        if (parent == -1) {
            folder_name.setText(R.string.uncategorised);
            folder_icon.setColorFilter(ContextCompat.getColor(this, R.color.folderColorUnknown));

            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(R.attr.textColor, typedValue, true);
            @ColorInt int color = typedValue.data;

            folder_name.setTextColor(color);

            return;
        }

        Cursor c = db.query("folders", new String[]{"id", "name", "color"}, "id = ?", new String[]{String.valueOf(parent)}, null,null,null);
        c.moveToFirst();

        folder_name.setText(c.getString(1));
        int col = c.getInt(2);

        folder_icon.setColorFilter(ContextCompat.getColor(this, col));
        folder_name.setTextColor(getColor(col));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (id == -1) {
            return false;
        }

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.note_actions, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        //Save data

        if (deleted) {
            super.onDestroy();
            return;
        }

        EditText titleEditor = findViewById(R.id.titleEditor);

        ContentValues cv = new ContentValues();

        String title = titleEditor.getText().toString();

        if (isCheckList) {
            cv.put("type", "check");

            ArrayList<item> items = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
                items.add(adapter.getItem(i));
            }
            String s = gs.toJson(items);
            cv.put("content", s);
        } else {
            cv.put("type", "note");
            EditText textEditor = findViewById(R.id.noteEditorView);
            String s = textEditor.getText().toString();
            cv.put("content", s);
        }

        if (title.isEmpty()) {
            cv.put("name", "Untitled");
        } else {
            cv.put("name", title);
        }

        if (id == -1) {
            cv.put("folder_id", parent);
            db.insert("notes", null, cv);
        } else {
            db.update("notes", cv, "id = ?", new String[]{String.valueOf(id)});
        }

        super.onBackPressed();

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

                parent = f.id;
                refreshFolderInfo();

                ContentValues cv = new ContentValues();
                cv.put("folder_id", parent);

                db.update("notes", cv, "id = ?", new String[]{String.valueOf(id)});

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

    //region CheckList

    public class checkListAdapter extends ArrayAdapter<item> {
        public checkListAdapter(@NonNull Context context, ArrayList<item> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            item item = getItem(position);

            LayoutInflater inf = LayoutInflater.from(getContext());

            View v = inf.inflate(R.layout.check_item, null);

            EditText et = v.findViewById(R.id.check_text);

            et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    item.title = charSequence.toString();
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });

            et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (et.getText().toString().isEmpty() && !b) {
                        adapter.remove(item);
                        Snackbar.make(findViewById(R.id.coordinator), getString(R.string.item_removed), 2000).show();
                    }
                }
            });

            et.setText(item.title);

            CheckBox check = v.findViewById(R.id.check_box);
            check.setChecked(item.checked);

            check.setOnCheckedChangeListener((view, checked)->{
                item.checked = checked;
            });


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

    //endregion
}