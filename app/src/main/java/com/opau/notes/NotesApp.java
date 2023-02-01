package com.opau.notes;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class NotesApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //DynamicColors.applyToActivitiesIfAvailable(this);
    }

    public static class folderColor {
        public int resource;
        public int displayNameResource;

        public folderColor(int r, int d) {
            resource = r;
            displayNameResource = d;
        }
    }

    public static final ArrayList<folderColor> colorList = new ArrayList<folderColor>(
            Arrays.asList(
                    new folderColor(R.color.folderColorRed, R.string.folderColorRed),
                    new folderColor(R.color.folderColorPink, R.string.folderColorPink),
                    new folderColor(R.color.folderColorPurple, R.string.folderColorPurple),
                    new folderColor(R.color.folderColorDeepPurple, R.string.folderColorDeepPurple),
                    new folderColor(R.color.folderColorIndigo, R.string.folderColorIndigo),
                    new folderColor(R.color.folderColorBlue, R.string.folderColorBlue),
                    new folderColor(R.color.folderColorLightBlue, R.string.folderColorLightBlue),
                    new folderColor(R.color.folderColorCyan, R.string.folderColorCyan),
                    new folderColor(R.color.folderColorTeal, R.string.folderColorTeal),
                    new folderColor(R.color.folderColorGreen, R.string.folderColorGreen),
                    new folderColor(R.color.folderColorLightGreen, R.string.folderColorLightGreen),
                    new folderColor(R.color.folderColorLime, R.string.folderColorLime),
                    new folderColor(R.color.folderColorYellow, R.string.folderColorYellow),
                    new folderColor(R.color.folderColorAmber, R.string.folderColorAmber),
                    new folderColor(R.color.folderColorOrange, R.string.folderColorOrange),
                    new folderColor(R.color.folderColorDeepOrange, R.string.folderColorDeepOrange),
                    new folderColor(R.color.folderColorBrown, R.string.folderColorBrown)
            )
    );
}
