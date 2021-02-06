package in.dev.android.demoauth.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import in.dev.android.demoauth.ApplicationConfig;
import in.dev.android.demoauth.R;
import in.dev.android.demoauth.adapter.UserAdapter;
import in.dev.android.demoauth.databinding.ActivityShowUserBinding;
import in.dev.android.demoauth.helper.Provider;
import in.dev.android.demoauth.helper.RecyclerItemTouchHelper;
import in.dev.android.demoauth.helper.Utils;
import in.dev.android.demoauth.model.User;

public class ShowUserActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    public static final String TAG = ShowUserActivity.class.getSimpleName();

    ActivityShowUserBinding binding;
    private DatabaseReference mFirebaseDatabase = null;

    List<User> userList;
    List<String> userKey;

    UserAdapter adapter;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShowUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference(Provider.USER_ACCESS_KEY);
        userList = new ArrayList<>();
        userKey = new ArrayList<>();

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle(ApplicationConfig.preference.getPreference(Provider.USER_EMAIL));

        binding.rvUser.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUser.setItemAnimator(new DefaultItemAnimator());
        binding.rvUser.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvUser);

        getUserList();
    }

    private void getUserList() {
        userList.clear();
        userKey.clear();
        mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                userKey.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String key = dataSnapshot.getKey();
                    User user = dataSnapshot.getValue(User.class);
                    userList.add(user);
                    userKey.add(key);
                }

                adapter = new UserAdapter(ShowUserActivity.this, userList, userKey);
                binding.rvUser.setAdapter(adapter);
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
        getUserList();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof UserAdapter.MyViewHolder) {
            removeUser(userList.get(viewHolder.getAdapterPosition()), userKey.get(viewHolder.getAdapterPosition()));
            adapter.removeItem(viewHolder.getAdapterPosition());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void removeUser(User user, String key) {
        Utils.showProgressDialog(ShowUserActivity.this, "Please Wait...");
        Query query = mFirebaseDatabase.child(key);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    dataSnapshot.getRef().removeValue();
                }

                Toast.makeText(ShowUserActivity.this, user.getEmail() + " has been Removed ..", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Utils.printLog(error.toException());
            }
        });
        Utils.hideProgressDialog();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logoutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.label_logout));
        builder.setMessage(getString(R.string.label_logout_message));
        builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ApplicationConfig.preference.clearPreference();
                mGoogleSignInClient.signOut();
                LoginManager.getInstance().logOut();
                startActivity(new Intent(ShowUserActivity.this, SigninActivity.class));
                finish();
            }
        });

        builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialog.cancel();
            }
        });

        builder.show();

    }
}