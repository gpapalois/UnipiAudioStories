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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ✅ Explicit URL (region RTDB)
    private static final String RTDB_URL =
            "https://unipiaudiostories-f7609-default-rtdb.europe-west1.firebasedatabase.app";

    private RecyclerView recyclerStories;
    private MaterialButton btnLanguage, btnStats, btnLogout;

    private final List<Story> storyList = new ArrayList<>();
    private StoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Guard: αν δεν υπάρχει logged-in user -> Login
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }

        // 2) Bind views
        recyclerStories = findViewById(R.id.recyclerStories);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnStats = findViewById(R.id.btnStats);
        btnLogout = findViewById(R.id.btnLogout); // ⬅️ βάλε αυτό το id στο XML

        // 3) Recycler setup (Grid 2 columns)
        recyclerStories.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new StoryAdapter(this, storyList, story -> {
            Intent i = new Intent(MainActivity.this, StoryActivity.class);
            i.putExtra("storyId", story.id);
            startActivity(i);
        });
        recyclerStories.setAdapter(adapter);

        // 4) Buttons
        btnLanguage.setOnClickListener(v ->
                Toast.makeText(this, "Language picker (next step)", Toast.LENGTH_SHORT).show()
        );

        btnStats.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);

            finish();
        });

        // 5) Load stories
        loadStories();
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

                            story.id = s.getKey(); // key ως id
                            storyList.add(story);
                        }

                        adapter.notifyDataSetChanged();
                        setUiEnabled(true);

                        if (storyList.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "No stories found in RTDB (/stories).",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setUiEnabled(true);
                        Toast.makeText(MainActivity.this,
                                "Failed to load stories: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
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
