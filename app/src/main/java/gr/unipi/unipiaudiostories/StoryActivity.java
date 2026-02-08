package gr.unipi.unipiaudiostories;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoryActivity extends AppCompatActivity {

    private static final String RTDB_URL =
            "https://unipiaudiostories-f7609-default-rtdb.europe-west1.firebasedatabase.app";

    private ImageView imgStory;
    private TextView tvTitle, tvAuthor, tvText;
    private MaterialButton btnPrev, btnPlayPause, btnNext, btnStop;

    private Story story;

    // TTS
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // Chunking state
    private final List<String> chunks = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isSpeaking = false;

    private boolean isPaused = false;

    private boolean userPaused = false;
    private int resumeIndex = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        imgStory = findViewById(R.id.imgStory);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvText = findViewById(R.id.tvText);

        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnStop = findViewById(R.id.btnStop);

        disableControls();

        initTTS();

        String storyId = getIntent().getStringExtra("storyId");
        if (storyId == null || storyId.trim().isEmpty()) {
            Toast.makeText(this, "Missing storyId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadStory(storyId);

        btnPlayPause.setOnClickListener(v -> {
            if (!ttsReady || chunks.isEmpty()) {
                Toast.makeText(this, "TTS not ready / no text", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isSpeaking) {
                // ▶ start / resume
                if (userPaused) {
                    currentIndex = resumeIndex;
                }
                userPaused = false;
                speakFromCurrentIndex();
            } else {
                // ⏸ pause
                pauseSpeaking();
            }


        });

        btnNext.setOnClickListener(v -> {
            if (chunks.isEmpty()) return;
            currentIndex = Math.min(currentIndex + 1, chunks.size() - 1);
            restartFromCurrentIndex();
        });

        btnPrev.setOnClickListener(v -> {
            if (chunks.isEmpty()) return;
            currentIndex = Math.max(currentIndex - 1, 0);
            restartFromCurrentIndex();
        });

        btnStop.setOnClickListener(v -> stopSpeaking());
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.US); // ιστορίες στα αγγλικά
                ttsReady = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);

                if (!ttsReady) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_LONG).show();
                    disableControls();
                    return;
                }

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {

                            // ❌ Αν ο χρήστης πάτησε pause, ΜΗΝ προχωράς index
                            if (userPaused) return;

                            currentIndex++;

                            if (currentIndex < chunks.size()) {
                                speakChunk(chunks.get(currentIndex));
                            } else {
                                isSpeaking = false;
                                btnPlayPause.setText("▶");
                            }
                        });
                    }



                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> Toast.makeText(StoryActivity.this, "TTS error", Toast.LENGTH_SHORT).show());
                    }
                });

                // αν έχει φορτωθεί ήδη story, μπορούμε να ενεργοποιήσουμε controls
                if (!chunks.isEmpty()) enableControls();
            } else {
                Toast.makeText(this, "TTS init failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadStory(String storyId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(RTDB_URL);

        db.getReference("stories")
                .child(storyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        story = snapshot.getValue(Story.class);
                        if (story == null) {
                            Toast.makeText(StoryActivity.this, "Story not found", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        tvTitle.setText(story.title);
                        tvAuthor.setText(story.author + (story.year > 0 ? " • " + story.year : ""));
                        tvText.setText(story.text);

                        Glide.with(StoryActivity.this)
                                .load(story.imageUrl)
                                .centerCrop()
                                .into(imgStory);

                        buildChunks(story.text);
                        currentIndex = 0;
                        isSpeaking = false;
                        isPaused = false;
                        btnPlayPause.setText("▶");

                        if (ttsReady && !chunks.isEmpty()) enableControls();
                        else disableControls();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StoryActivity.this,
                                "Failed to load story: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- Chunking ---
    private void buildChunks(String text) {
        chunks.clear();
        if (text == null) return;

        // Split σε προτάσεις (απλό και αποτελεσματικό για demo)
        // Κρατάμε το σημείο στίξης.
        String cleaned = text.trim().replace("\n", " ").replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) return;

        String[] parts = cleaned.split("(?<=[.!?])\\s+");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) chunks.add(s);
        }

        // fallback: αν δεν έσπασε (π.χ. χωρίς τελείες)
        if (chunks.isEmpty()) chunks.add(cleaned);
    }

    // --- Speaking controls ---
    private void speakFromCurrentIndex() {
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= chunks.size()) currentIndex = chunks.size() - 1;

        isSpeaking = true;
        btnPlayPause.setText("⏸");
        speakChunk(chunks.get(currentIndex));
    }

    private void speakChunk(String chunk) {
        if (!ttsReady || tts == null) return;
        if (chunk == null || chunk.trim().isEmpty()) return;

        // QUEUE_FLUSH για να κόβει οτιδήποτε έπαιζε και να παίζει αυτό
        String utteranceId = "chunk_" + currentIndex + "_" + System.currentTimeMillis();
        tts.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void pauseSpeaking() {
        if (tts == null) return;

        userPaused = true;
        resumeIndex = currentIndex;

        isSpeaking = false;
        btnPlayPause.setText("▶");

        tts.stop();
    }



    private void restartFromCurrentIndex() {
        if (!ttsReady || chunks.isEmpty()) return;

        userPaused = false;

        if (tts != null) tts.stop();

        isSpeaking = true;
        btnPlayPause.setText("⏸");

        speakFromCurrentIndex();
    }



    private void stopSpeaking() {
        if (tts != null) tts.stop();

        userPaused = false;
        isSpeaking = false;
        currentIndex = 0;
        resumeIndex = 0;

        btnPlayPause.setText("▶");
    }



    private void disableControls() {
        btnPrev.setEnabled(false);
        btnPlayPause.setEnabled(false);
        btnNext.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void enableControls() {
        btnPrev.setEnabled(true);
        btnPlayPause.setEnabled(true);
        btnNext.setEnabled(true);
        btnStop.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // για να μην μιλάει στο background
        if (tts != null) tts.stop();
        isSpeaking = false;
        isPaused = false;
        btnPlayPause.setText("▶");
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
