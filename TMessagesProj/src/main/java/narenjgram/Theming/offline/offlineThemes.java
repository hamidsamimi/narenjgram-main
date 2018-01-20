package powergram.Theming.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import ir.powergram.messenger.R;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import powergram.constant;


public class offlineThemes extends BaseFragment {

    private RecyclerView listView;
    List<Integer> themes = new ArrayList<>();
    themesAdapter listAdapter;
    private final int MENU_RESET = 1;

    boolean reseting = false;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();


        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("OfflineThemes", R.string.OfflineThemes));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_RESET) {
                    ResetTheme();
                }
            }
        });


        fragmentView = new FrameLayout(context);
        fragmentView.setLayoutParams(new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(MENU_RESET, R.drawable.ic_refresh, AndroidUtilities.dp(56));


        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerView(context);

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        listView.setBackgroundColor(preferences.getInt("prefBGColor", 0xffffffff));
        listView.setVerticalScrollBarEnabled(false);
        int def = preferences.getInt("themeColor", AndroidUtilities.defColor);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
        layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.TOP;
        listView.setLayoutParams(layoutParams);


        themes = new ArrayList<Integer>();

        themes.add(AndroidUtilities.defColor);
        themes.add( 0xffF7D7B0);
        themes.add(0xff008080);
        themes.add(0xff039969);
        themes.add(0xffc64c03);
        themes.add(0xffaa97ad);
        themes.add(0xffb32a00);
        themes.add(0xffAD1457);
        themes.add(0xffFF1744);
        themes.add(0xff1976D2);
        themes.add(0xff00BFA5);
        themes.add(0xff37474F);


        refreshDisplay(context);


        return fragmentView;
    }


    private void ResetTheme() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
        builder.setTitle(LocaleController.getString("ResetThemeSettings", R.string.ResetThemeSettings));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (reseting) {
                    return;
                }
                reseting = true;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reseting = false;
                        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
                        SharedPreferences.Editor editor = themePrefs.edit();
                        editor.clear();
                        editor.commit();
                        //Stock Background
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        editor = preferences.edit();
                        editor.putInt("selectedBackground", 1000001);
                        editor.putInt("selectedColor", 0);
                        editor.commit();
                        File toFile = new File(ApplicationLoader.applicationContext.getFilesDir(), "wallpaper.jpg");
                        if (toFile.exists()) {
                            toFile.delete();
                        }
                        if (getParentActivity() != null) {
                            Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("ResetThemeToastText", R.string.ResetThemeToastText), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
                AndroidUtilities.needRestart = true;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getParentActivity() != null) {
                            Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("AppWillRestart", R.string.AppWillRestart), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());

    }

    private void refreshDisplay(Context context) {


        if (themes != null) {
            listView.setLayoutManager(new LinearLayoutManager(listView.getContext()));
            listAdapter = new themesAdapter(context, themes, offlineThemes.this);
            listView.setAdapter(listAdapter);
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        updateTheme();
        if (constant.AnalyticInitialized)
            ApplicationLoader.getInstance().trackScreenView("offline Themes ");
    }


    public void updateTheme() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
        actionBar.setBackgroundColor(themePrefs.getInt("prefHeaderColor", def));
        actionBar.setTitleColor(themePrefs.getInt("prefHeaderTitleColor", 0xffffffff));

        Drawable back = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_back);
        back.setColorFilter(themePrefs.getInt("prefHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);
        actionBar.setBackButtonDrawable(back);
    }


}
