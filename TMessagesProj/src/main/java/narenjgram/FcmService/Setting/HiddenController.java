package powergram.FcmService.Setting;

import android.content.Context;


/**
 * Created by Saman on 6/13/2016.
 */
public class HiddenController {
    private static HiddenController fav;
    private static Context thiscontext;

    public static void addToHidden(Long id){
        String m= Setting.getHiddenList();
        m=m+"-"+String.valueOf(id);
        Setting.setHiddenList(m);
    }
    public static void addToHidden(String user){
        String m= Setting.getHiddenList();
        m=m+"-"+String.valueOf(user);
        Setting.setHiddenList(m);
    }
    public static Boolean isHidden(String user){
        boolean m= Setting.getHiddenList().toLowerCase().contains(String.valueOf(user));
        return m;
    }
    public static Boolean isHidden(Long id){
        boolean m= Setting.getHiddenList().toLowerCase().contains(String.valueOf(id));
        return m;
    }

    public static boolean IsHidden(Long aLong) {
        return  isHidden(aLong);
    }

    public static void RemoveFromHidden(long selectedDialog) {
            String m= Setting.getHiddenList();
            m=m.replace(String.valueOf(selectedDialog),"");
            Setting.setHiddenList(m);

    }
}
