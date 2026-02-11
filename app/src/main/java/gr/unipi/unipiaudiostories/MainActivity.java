package gr.unipi.unipiaudiostories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String RTDB_URL =
            "https://unipiaudiostories-f7609-default-rtdb.europe-west1.firebasedatabase.app";

    private RecyclerView recyclerStories;
    private MaterialButton btnLanguage, btnStats, btnLogout;

    private final List<Story> storyList = new ArrayList<>();
    private StoryAdapter adapter;

    // Μεταβλητή για το ID του τρέχοντος χρήστη
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Guard: Έλεγχος αν υπάρχει συνδεδεμένος χρήστης
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }

        // Ορίζουμε το currentUserId χρησιμοποιώντας το UID της Firebase (μοναδικό για κάθε χρήστη)
        currentUserId = user.getUid();

        // 2) Bind views
        recyclerStories = findViewById(R.id.recyclerStories);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnStats = findViewById(R.id.btnStats);
        btnLogout = findViewById(R.id.btnLogout);

        // 3) Recycler setup (Grid 2 columns)
        recyclerStories.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new StoryAdapter(this, storyList, story -> {
            // Όταν επιλέγεται μια ιστορία, στέλνουμε το storyId ΚΑΙ το userId
            Intent i = new Intent(MainActivity.this, StoryActivity.class);
            i.putExtra("storyId", story.id);
            i.putExtra("userId", currentUserId);
            startActivity(i);
        });
        recyclerStories.setAdapter(adapter);

        // 4) Button Listeners

        // Επιλογή Γλώσσας
        btnLanguage.setOnClickListener(v -> {
            String[] languages = {"English", "Ελληνικά", "Français"};

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.app_name)); // Χρήση string resource για τον τίτλο
            builder.setItems(languages, (dialog, which) -> {
                if (which == 0) setAppLocale("en");
                else if (which == 1) setAppLocale("el");
                else if (which == 2) setAppLocale("fr");
            });
            builder.show();
        });

        // Προβολή Στατιστικών
        btnStats.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, StatsActivity.class);
            // Στέλνουμε το userId για να δει ο χρήστης ΜΟΝΟ τα δικά του στατιστικά
            i.putExtra("userId", currentUserId);
            startActivity(i);
        });

        // Αποσύνδεση (Logout)
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // 5) Φόρτωση ιστοριών από Firebase
        loadStories();
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
        // Το UI θα ανανεωθεί αυτόματα με τα κείμενα από τα strings.xml
    }

    private void loadStories() {
        setUiEnabled(false);
        FirebaseDatabase db = FirebaseDatabase.getInstance(RTDB_URL);

        db.getReference("stories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        storyList.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            Story story = s.getValue(Story.class);
                            if (story == null) continue;

                            story.id = s.getKey();
                            storyList.add(story);
                        }
                        adapter.notifyDataSetChanged();
                        setUiEnabled(true);

                        if (storyList.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No stories found.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setUiEnabled(true);
                        Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setUiEnabled(boolean enabled) {
        recyclerStories.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        btnLanguage.setEnabled(enabled);
        btnStats.setEnabled(enabled);
        btnLogout.setEnabled(enabled);
    }

    private void goLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}