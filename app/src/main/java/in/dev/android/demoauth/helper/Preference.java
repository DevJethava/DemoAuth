package in.dev.android.demoauth.helper;

import android.content.Context;
import android.content.SharedPreferences;

import in.dev.android.demoauth.R;

public class Preference {

    public static final String TAG = Preference.class.getSimpleName();

    private SharedPreferences preferences = null;

    public Preference(Context context) {
        this.preferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
    }

    public void removePreference(String key) {
        if (contains(key)) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.remove(key).apply();
        }
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    public String getPreference(String key) {
        Utils.printLog(TAG, "key:" + key + " - " + "value:" + preferences.getString(key, ""));
        return preferences.getString(key, "");
    }

    public void putPreference(String key, String value) {
        Utils.printLog(TAG, "key:" + key + " - " + "value:" + value);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(key, value);
        edit.apply();
    }

    public void clearPreference() {
        SharedPreferences.Editor edit = preferences.edit();
        edit.clear();
        edit.apply();
    }
}
