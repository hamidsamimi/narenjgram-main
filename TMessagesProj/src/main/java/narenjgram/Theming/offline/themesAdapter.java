package powergram.Theming.offline;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ir.powergram.messenger.R;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.List;

public class themesAdapter extends RecyclerView.Adapter<themesAdapter.ViewHolder> {

    private final TypedValue mTypedValue = new TypedValue();
    private final int mBackground;
    private final List<Integer> themes;
    Context context;
    BaseFragment baseFragment;

    public themesAdapter(Context context, List<Integer> themes, BaseFragment baseFragment) {
        //context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        this.themes = themes;
        this.context = context;

        this.baseFragment = baseFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_theme, parent, false);
        //view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.themeColor.setBackgroundColor(themes.get(position));


        holder.themeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                commitInt(themes.get(position));
                if(baseFragment instanceof offlineThemes){
                    ((offlineThemes)baseFragment).updateTheme();
                }
            }
        });

    }

    private void commitInt(int i){
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("themeColor", i);
        AndroidUtilities.themeColor = i;
        editor.commit();
        //Reset Theme Colors
        int darkColor = AndroidUtilities.setDarkColor(i, 0x15);
        editor.putInt("chatsHeaderColor", i);
        editor.putInt("chatsCountBGColor", i);
        editor.putInt("chatsChecksColor", i);
        editor.putInt("chatsMemberColor", darkColor);
        editor.putInt("chatsMediaColor", preferences.getInt("chatsMemberColor", darkColor));
        editor.putInt("chatsFloatingBGColor", i);

        editor.putInt("chatHeaderColor", i);
        editor.putInt("chatEmojiViewTabColor", AndroidUtilities.setDarkColor(i, -0x15));
        editor.putInt("chatChecksColor", i);
        editor.putInt("chatSendIconColor", i);
        editor.putInt("chatMemberColor", darkColor);
        editor.putInt("chatForwardColor", darkColor);

        editor.putInt("contactsHeaderColor", i);
        editor.putInt("contactsOnlineColor", darkColor);

        editor.putInt("prefHeaderColor", i);

        editor.putInt("dialogColor", i);

        editor.commit();
        AndroidUtilities.themeColor = i;
    }


    @Override
    public int getItemCount() {
        return themes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final View themeColor;
        final View mView;
        ViewHolder(View view) {
            super(view);
            mView = view;
            themeColor =  view.findViewById(R.id.themeColor);


        }


    }

}