package com.example.tictactoe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tictactoe.MainActivity;

public class PlayerName extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_name);

        final EditText playerName=findViewById(R.id.playerName);
        final AppCompatButton StartBtn=findViewById(R.id.StartBtn);

        StartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String getPlayerName = playerName.getText().toString();

                if (getPlayerName.isEmpty()) {
                    Toast.makeText(PlayerName.this, "Please Enter Player Name!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = new Intent(PlayerName.this, MainActivity.class);
                    intent.putExtra("playerName",getPlayerName); //storing player name in variable playerName to be used later (pass your values and retrieve them in the other Activity)
                    startActivity(intent);
                    finish();//destroying this activity
                }
            }
        });
    }
}