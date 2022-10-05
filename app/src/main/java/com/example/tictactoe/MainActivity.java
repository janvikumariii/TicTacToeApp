package com.example.tictactoe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LinearLayout player1Layout,player2Layout;
    private ImageView image1,image2,image3,image4,image5,image6,image7,image8,image9;
    private TextView player1TV,player2TV;

    //make a list of winning combinations
    private final List<int[]> combinationsList=new ArrayList<>();
    private final List<String> doneBoxes=new ArrayList<>(); //position of boxes occupied by user so no two user clashes
    private String playerUniqueId = "0";

    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://tictactoe-7929c-default-rtdb.firebaseio.com/");
    private boolean opponentFound= false;
    private String opponentUniqueId= "0";
    private String status = "matching";
    private String playerTurn = "";
    private String connectionId = "";
    ValueEventListener turnsEventListener, wonEventListener; //generates value event listener for database

    //box selected by players
    private final String[] boxesSelectedBy = {"", "","","","","","","",""};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        player1Layout=findViewById(R.id.player1Layout);
        player2Layout=findViewById(R.id.player2Layout);

        image1=findViewById(R.id.image1);
        image2=findViewById(R.id.image2);
        image3=findViewById(R.id.image3);
        image4=findViewById(R.id.image4);
        image5=findViewById(R.id.image5);
        image6=findViewById(R.id.image6);
        image7=findViewById(R.id.image7);
        image8=findViewById(R.id.image8);
        image9=findViewById(R.id.image9);

        player1TV=findViewById(R.id.player1TV);
        player2TV=findViewById(R.id.player2TV);

        final String getPlayerName = getIntent().getStringExtra("playerName"); //getting player name which we stored earlier

        combinationsList.add(new int[]{0,1,2});
        combinationsList.add(new int[]{0,3,6});
        combinationsList.add(new int[]{0,4,8});
        combinationsList.add(new int[]{3,4,5});
        combinationsList.add(new int[]{6,7,8});
        combinationsList.add(new int[]{1,4,7});
        combinationsList.add(new int[]{2,5,8});
        combinationsList.add(new int[]{2,4,6});

        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Waiting for Opponent to join");
        progressDialog.show();

        playerUniqueId=String.valueOf(System.currentTimeMillis()); //player has a unique id which is milli second of current time

        player1TV.setText(getPlayerName);

        databaseReference.child("connections").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!opponentFound)//check if opponent found or not, if not
                {
                    if (snapshot.hasChildren())//checks if other players are are there
                    {
                        for (DataSnapshot connections : snapshot.getChildren())//checks each connection if someone is waiting to play
                        {
                            String conId = connections.getKey(); //connection unique id
                            int getPlayersCount = (int) connections.getChildrenCount();//if 1, player is waiting and if 2, connection completed

                            if (status.equals("Waiting")) {
                                if (getPlayersCount == 2) {
                                    playerTurn = playerUniqueId;
                                    applyPlayerTurn(playerTurn);

                                    boolean playerFound = false;

                                    for (DataSnapshot players : connections.getChildren()) { //getting all the players in connection
                                        String getPlayerUniqueId = players.getKey();
                                        if (getPlayerUniqueId.equals(playerUniqueId)) {
                                            playerFound = true;
                                        } else if (playerFound) {
                                            String getOpponentPlayerName = players.child("Player_Name").getValue(String.class);
                                            opponentUniqueId = players.getKey();

                                            player2TV.setText(getOpponentPlayerName);
                                            connectionId = conId;
                                            opponentFound = true;
                                            //adding event listener to database
                                            databaseReference.child("Turns").child(connectionId).addValueEventListener(turnsEventListener);
                                            databaseReference.child("Won").child(connectionId).addValueEventListener(wonEventListener);

                                            if (progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }

                                            //remove listener after connection is established
                                            databaseReference.child("connections").removeEventListener(this);
                                        }
                                    }
                                }
                            } else {
                                //if 2 players are waiting, join each other
                                if (getPlayersCount == 1) {
                                    connections.child(playerUniqueId).child("Player_Name").getRef().setValue(getPlayerName);
                                    for (DataSnapshot players : connections.getChildren()) {
                                        String getOpponentName = players.child("Player_Name").getValue(String.class);
                                        opponentUniqueId = players.getKey();

                                        //first turn who created the room
                                        playerTurn = opponentUniqueId;
                                        applyPlayerTurn(playerTurn);

                                        player2TV.setText(getOpponentName);
                                        connectionId = conId;
                                        opponentFound = true;
                                        databaseReference.child("Turns").child(connectionId).addValueEventListener(turnsEventListener);
                                        databaseReference.child("Won").child(connectionId).addValueEventListener(wonEventListener);

                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }

                                        //remove listener after connection is established
                                        databaseReference.child("connections").removeEventListener(this);

                                        break;
                                    }
                                }
                            }
                        }
                        //if no opponent, create a new connection
                        if (!opponentFound && !status.equals("Waiting")) {
                            String connectionUniqueId = String.valueOf(System.currentTimeMillis()); //unique id for connection
                            snapshot.child(connectionUniqueId).child(playerUniqueId).child("Player_Name").getRef().setValue(getPlayerName);
                            status = "Waiting";
                        }

                    } else //if no connection, creates a new connection
                    {
                        String connectionUniqueId = String.valueOf(System.currentTimeMillis()); //unique id for connection
                        snapshot.child(connectionUniqueId).child(playerUniqueId).child("Player_Name").getRef().setValue(getPlayerName);
                        status = "Waiting";
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        turnsEventListener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //all the turns of connection
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    if(dataSnapshot.getChildrenCount()==2){
                        //which box user clicked
                        final int getBoxPosition=Integer.parseInt(dataSnapshot.child("Box_Position").getValue(String.class));
                        //player id of who selected the box
                        final String getPlayerId=dataSnapshot.child("Player_Id").getValue(String.class);
                        if(!doneBoxes.contains(String.valueOf(getBoxPosition))){ //if user did not select the box before
                            doneBoxes.add(String.valueOf(getBoxPosition));
                            if(getBoxPosition==1){
                                selectBox(image1,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==2){
                                selectBox(image2,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==3){
                                selectBox(image3,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==4){
                                selectBox(image4,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==5){
                                selectBox(image5,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==6){
                                selectBox(image6,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==7){
                                selectBox(image7,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==8){
                                selectBox(image8,getBoxPosition,getPlayerId);
                            }
                            else if(getBoxPosition==9){
                                selectBox(image9,getBoxPosition,getPlayerId);
                            }
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        wonEventListener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //if user won
                if(snapshot.hasChild("Player_Id")){
                    String getWinPlayerId=snapshot.child("Player_Id").getValue(String.class);
                    final WinDialog winDialog;

                    if(getWinPlayerId.equals(playerUniqueId)){
                        winDialog=new WinDialog(MainActivity.this,"You won the game!");
                    }
                    else{
                        winDialog=new WinDialog(MainActivity.this,"Opponent won the game!");
                    }
                    winDialog.setCancelable(false);
                    winDialog.show();

                    databaseReference.child("Turns").child(connectionId).removeEventListener(turnsEventListener);
                    databaseReference.child("Won").child(connectionId).removeEventListener(wonEventListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("1")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("1");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("2")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("2");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("3")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("3");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("4")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("4");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("5")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("5");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("6")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("6");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("7")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("7");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("8")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("8");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });
        image9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!doneBoxes.contains("9")&&playerTurn.equals(playerUniqueId)){ //if box not selected befpre and current user turn
                    ((ImageView)view).setImageResource(R.drawable.crosss);
                    //send selected box position and player unique id to firebase
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Box_Position").setValue("9");
                    databaseReference.child("Turns").child(connectionId).child(String.valueOf(doneBoxes.size()+1)).child("Player_Id").setValue(playerUniqueId);

                    //change player turn
                    playerTurn=opponentUniqueId;
                }
            }
        });

    }

    private void applyPlayerTurn(String playerUniqueId2) {
        if(playerUniqueId2.equals(playerUniqueId)){
            player1Layout.setBackgroundResource(R.drawable.back);
            player2Layout.setBackgroundResource(R.drawable.back2);
        }
        else{
            player2Layout.setBackgroundResource(R.drawable.back);
            player1Layout.setBackgroundResource(R.drawable.back2);
        }
    }
    private void selectBox(ImageView imageView, int selectedBoxPosition,String selectedByPlayer){
        boxesSelectedBy[selectedBoxPosition-1]=selectedByPlayer;

        if(selectedByPlayer.equals(playerUniqueId)){
            imageView.setImageResource(R.drawable.crosss);
            playerTurn=opponentUniqueId;
        }
        else{
            imageView.setImageResource(R.drawable.donut);
            playerTurn=playerUniqueId;
        }
        applyPlayerTurn(playerTurn);

        if(checkPlayerWin(selectedByPlayer)){
            databaseReference.child("Won").child(connectionId).child("Player_Id").setValue(selectedByPlayer); //to notify opponent, send player id to database
        }
        if(doneBoxes.size()==9){
            //game over if no box left
            final WinDialog winDialog=new WinDialog(MainActivity.this,"It's a Draw!!");
            winDialog.setCancelable(false);
            winDialog.show();
        }

    }

    private boolean checkPlayerWin(String playerId){
        boolean isPlayerWon=false;

        //compare player turn with combination of array
        for(int i=0;i<combinationsList.size();i++){
            final int[] combination=combinationsList.get(i);

            //last three combination
            if(boxesSelectedBy[combination[0]].equals(playerId) &&
                    boxesSelectedBy[combination[1]].equals(playerId)&&
                            boxesSelectedBy[combination[2]].equals(playerId)){
                isPlayerWon=true;

            }
        }
        return isPlayerWon;
    }
}