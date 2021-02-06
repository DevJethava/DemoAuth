package in.dev.android.demoauth;

import androidx.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import in.dev.android.demoauth.helper.Preference;
import in.dev.android.demoauth.helper.Provider;

public class ApplicationConfig extends MultiDexApplication {

    public static Preference preference;

    @Override
    public void onCreate() {
        super.onCreate();
        preference = new Preference(this);
        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!Provider.ISFROMTEST);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }
}
