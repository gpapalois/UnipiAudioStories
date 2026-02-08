package gr.unipi.unipiaudiostories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        tvGoLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (pass.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!pass.equals(confirm)) {
            etConfirm.setError("Passwords do not match");
            etConfirm.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                    goMain();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Register failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
