package gr.unipi.unipiaudiostories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private TextView txtFavoriteStory, txtFullStats;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        txtFavoriteStory = findViewById(R.id.txtFavoriteStory);
        txtFullStats = findViewById(R.id.txtFullStats);
        btnBack = findViewById(R.id.btnBack);

        loadStatistics();

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStatistics() {
        String userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            userId = "default_user";
        }

        SharedPreferences prefs = getSharedPreferences("Stats_" + userId, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) {
            txtFavoriteStory.setText("-");

            txtFullStats.setText(getString(R.string.no_data_available_yet));
            return;
        }

        StringBuilder detailsBuilder = new StringBuilder();
        String favoriteStory = "";
        int maxListens = -1;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String storyTitle = entry.getKey();
            int count = (Integer) entry.getValue();


            detailsBuilder.append("📖 ")
                    .append(storyTitle)
                    .append(": ")
                    .append(count)
                    .append(" ")
                    .append(getString(R.string.times_label))
                    .append("\n");

            if (count > maxListens) {
                maxListens = count;
                favoriteStory = storyTitle;
            }
        }

        txtFavoriteStory.setText(favoriteStory + " (" + maxListens + ")");
        txtFullStats.setText(detailsBuilder.toString());
    }
}