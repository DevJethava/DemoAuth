package in.dev.android.demoauth.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.ProfileTracker;
import com.facebook.login.Login;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import in.dev.android.demoauth.ApplicationConfig;
import in.dev.android.demoauth.R;
import in.dev.android.demoauth.databinding.ActivitySigninBinding;
import in.dev.android.demoauth.helper.Provider;
import in.dev.android.demoauth.helper.Utils;
import in.dev.android.demoauth.model.User;

public class SigninActivity extends AppCompatActivity {

    public static final String TAG = SigninActivity.class.getSimpleName();

    ActivitySigninBinding binding;
    private FirebaseAuth auth;
    private static final int RC_SIGN_IN = 234;
    GoogleSignInClient mGoogleSignInClient;

    private DatabaseReference mFirebaseDatabase = null;
    private List<User> userList = null;

    private CallbackManager facebookCallbackManager;

    String loginUser = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference(Provider.USER_ACCESS_KEY);
        userList = new ArrayList<>();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        facebookCallbackManager = CallbackManager.Factory.create();

        if (ApplicationConfig.preference.contains(Provider.USER_LOGIN) && ApplicationConfig.preference.contains(Provider.USER_EMAIL)) {
            loginUser = ApplicationConfig.preference.getPreference(Provider.USER_LOGIN);

            if (loginUser.equals("true")) {
                startActivity(new Intent(SigninActivity.this, ShowUserActivity.class));
                finish();
            }
        }

        binding.tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SigninActivity.this, SignupActivity.class));
            }
        });

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                String email = binding.etEmail.getText().toString();
                String password = binding.etPassword.getText().toString();

                binding.tilEmail.setError("");
                binding.tilPassword.setError("");

                if (TextUtils.isEmpty(email)) {
                    binding.tilEmail.setError("Invalid Email.");
                    return;
                } else if (TextUtils.isEmpty(password)) {
                    binding.tilPassword.setError("Invalid Password.");
                    return;
                } else {
                    handleSigninWithEmailPassword(email, password);
                }

            }
        });

        binding.ivFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isNetworkAvailable(SigninActivity.this)) {
                    handleFacebookLogin();
                } else {
                    Toast.makeText(SigninActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.ivGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isNetworkAvailable(SigninActivity.this)) {
                    handleGoogleSignin();
                } else {
                    Toast.makeText(SigninActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleFacebookLogin() {
        if (facebookCallbackManager != null) {
            LoginManager.getInstance().registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            try {
                                if (response.getJSONObject() == null || !response.getJSONObject().has("email") || response.getJSONObject().optString("email").isEmpty()) {
                                    Toast.makeText(SigninActivity.this, "Facebook Email Not Found !", Toast.LENGTH_LONG).show();
                                }

                                String email = "";
                                if (response.getJSONObject() != null) {
                                    JSONObject userJson = response.getJSONObject();
                                    if (userJson.has("email")) {
                                        email = userJson.optString("email");
                                    }

                                    if (Utils.isNetworkAvailable(SigninActivity.this) && !TextUtils.isEmpty(email)) {
                                        loginAndGo(email);
                                    } else {
                                        Toast.makeText(SigninActivity.this, "Network Error !", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            } catch (Exception e) {
                                Utils.printLog(e);
                            }
                        }

                    });

                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id, email, first_name, last_name, gender");
                    request.setParameters(parameters);
                    request.executeAsync();
                }

                @Override
                public void onCancel() {
                    Utils.printLog(TAG + " => Facebook", "onCancel");
                }

                @Override
                public void onError(FacebookException error) {
                    Utils.printLog(error);
                }
            });

            LoginManager.getInstance().logInWithReadPermissions(SigninActivity.this, Arrays.asList("email", "public_profile"));
        }
    }

    private void handleGoogleSignin() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    assert user != null;
                    Utils.printLog(TAG, "Email : " + user.getEmail() + " Name :" + user.getDisplayName());

                    loginAndGo(user.getEmail());

                } else {
                    Utils.printLog(task.getException());
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handleSigninWithEmailPassword(String email, String password) {

        Utils.showProgressDialog(this, "Please Wait...");
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(SigninActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Utils.hideProgressDialog();

                        if (task.isSuccessful()) {
                            ApplicationConfig.preference.putPreference(Provider.USER_LOGIN, "true");
                            ApplicationConfig.preference.putPreference(Provider.USER_EMAIL, email);
                            Toast.makeText(SigninActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SigninActivity.this, ShowUserActivity.class));
                            finish();
                        } else {
                            Toast.makeText(SigninActivity.this, "Sign in Failed, Try again ....", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(SigninActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utils.printLog(e);
                    }
                })
                .addOnCanceledListener(SigninActivity.this, new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        Utils.printLog(TAG, "Signin Canceled !");
                    }
                });
        Utils.hideProgressDialog();
    }

    public void checkUserExistence(String email) {
        int i = 0;
        for (User user : userList) {
            if (user.getEmail().equals(email)) {
                i++;
                break;
            }
        }

        if (i > 0) {
            ApplicationConfig.preference.putPreference(Provider.USER_LOGIN, "true");
            ApplicationConfig.preference.putPreference(Provider.USER_EMAIL, email);

            Intent intent = new Intent(SigninActivity.this, ShowUserActivity.class);
            startActivity(intent);
            finish();
        } else {
            mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            ApplicationConfig.preference.clearPreference();
            Toast.makeText(SigninActivity.this, "This User is Not Register, Please Register First.", Toast.LENGTH_LONG).show();
        }

    }

    private void loginAndGo(String email) {
        getAllUserData(email);
    }

    public void getAllUserData(String email) {
        userList.clear();
        mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    userList.add(user);
                }

                checkUserExistence(email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Utils.printLog(TAG, error.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideProgressDialog();
    }
}