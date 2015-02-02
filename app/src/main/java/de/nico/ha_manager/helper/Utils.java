package de.nico.ha_manager.helper;

/* 
 * @author Nico Alt
 * @author Devin
 * See the file "LICENSE.txt" for the full license governing this code.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.nico.ha_manager.HWManager;
import de.nico.ha_manager.R;
import de.nico.ha_manager.database.Source;

public class Utils {

    private static boolean isActionBarAvailable = false;

    public static void makeShortToast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void makeLongToast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }

    public static ArrayList<HashMap<String, String>> tempArray(
            ArrayList<HashMap<String, String>> ArHa, int pos) {

        // Temporary ArrayList containing a HashMap
        ArrayList<HashMap<String, String>> tempArHa = new ArrayList<>();

        // Temporary HashMap
        HashMap<String, String> tempHashMap = new HashMap<>();

        // Fill temporary HashMap with one row of original HashMap
        for (int i = 0; i < 5; i++)
            tempHashMap.put(Source.allColumns[i],
                    ArHa.get(pos).get(Source.allColumns[i]));

        // Add temporary HashMap to temporary ArrayList containing a HashMap
        tempArHa.add(tempHashMap);
        return tempArHa;

    }

    public static void setTheme(Activity c, boolean isAddTheme) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        final boolean dark = prefs.getBoolean("theme", false);
        final boolean black = prefs.getBoolean("black", false);

        if (dark) {
            if (isAddTheme)
                c.setTheme(R.style.DarkAddTheme);
            else
                c.setTheme(R.style.DarkAppTheme);
            if (black)
                c.getWindow().setBackgroundDrawableResource(android.R.color.black);
        } else {
            if (isAddTheme)
                c.setTheme(R.style.AddTheme);
            else
                c.setTheme(R.style.AppTheme);
        }

    }

    /**
     * A fix for a VerifyError crash on old versions
     * of Android
     */

    static {
        try {
            ActionBarWrapper.isAvailable();
            isActionBarAvailable = true;
        } catch (Throwable t) {
            isActionBarAvailable = false;
        }
    }

    public static void setupActionBar(Context context, boolean isPreferenceActivity) {

        if (Build.VERSION.SDK_INT >= 11 && isActionBarAvailable) {
            ActionBarWrapper actionBarWrapper = new ActionBarWrapper(context, isPreferenceActivity);
            actionBarWrapper.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static SimpleAdapter entryAdapter(Context c,
                                             ArrayList<HashMap<String, String>> a) {

        // All TextViews in Layout "listview_entry"
        int[] i = {R.id.textView_urgent, R.id.textView_subject,
                R.id.textView_homework, R.id.textView_until};

        // Make a SimpleAdapter which is like a row in the homework list
        return new SimpleAdapter(c, a, R.layout.listview_entry,
                Source.mostColumns, i);
    }

    @SuppressWarnings("deprecation")
    public static boolean shareApp(Context c) {
        String share_title = c.getString(R.string.intent_share_title);
        String app_name = c.getString(R.string.app_name);

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_TEXT,
                c.getString(R.string.intent_share_text));
        try {
            c.startActivity(Intent.createChooser(intent, share_title + " "
                    + app_name));
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e("ActivityNotFoundException", e.toString());
            return false;
        }
    }

    public static String getBuildInfo(Context c) {
        String buildInfo = "Built with love.";
        try {
            // Get Version Name
            PackageInfo pInfo = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), 0);
            String versionName = pInfo.versionName;

            // Get build time
            ApplicationInfo aInfo = c.getPackageManager().getApplicationInfo(
                    c.getPackageName(), 0);
            ZipFile zf = new ZipFile(aInfo.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            zf.close();
            long time = ze.getTime();
            DateFormat f = DateFormat.getDateInstance(DateFormat.SHORT,
                    Locale.getDefault());
            String buildDate = f.format(time);

            buildInfo = versionName + " (" + buildDate + ")";

        } catch (Exception e) {
            Log.e("Get Build Info:", e.toString());
        }

        return buildInfo;
    }

    public static void langSpinner(final Context c) {
        AlertDialog.Builder b = new Builder(c);
        // Current translations of HW-Manager
        final String[] langs = {"cs", "de", "en", "es", "fr", "hu", "ar", "fa"};
        // Items with translation's language
        String[] items = new String[langs.length + 1];
        items[0] = c.getString(R.string.pref_language_default);
        for (int i = 1; i < langs.length + 1; i++) {
            Locale appLoc = new Locale(langs[i - 1]);
            items[i] = appLoc.getDisplayLanguage(appLoc);
        }
        b.setTitle(c.getString(R.string.pref_language));
        b.setItems(items, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(c);
                SharedPreferences.Editor editor = prefs.edit();

                if (which == 0) {
                    editor.putString("locale_override", "");
                    editor.commit();
                } else {
                    editor.putString("locale_override", langs[which - 1]);
                    editor.commit();
                }
                HWManager.updateLanguage(c);
            }

        });

        b.show();
    }

    public static boolean transfer(final File src, final File dst) {
        try {
            // Import Database
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
            return true;

        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException", e.toString());
            return false;

        } catch (IOException e) {
            Log.e("IOException", e.toString());
            return false;

        }
    }

}



