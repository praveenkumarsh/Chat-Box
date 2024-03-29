package com.example.chatterbox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatSingleActivity extends AppCompatActivity {
    private String messageReceiverID,messageReceiverName,messageReceiverImage, messageSenderId;
    private TextView userName, userLastSeen;
    private CircleImageView userImage;
    private ImageButton SendmessageButton, SendFilesButton;
    private EditText MessageInputText;

    private DatabaseReference RootRef;
    private FirebaseAuth mAuth;

    private Toolbar ChatToolbar;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private  MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, saveCurrentDate;
    private String checker = "", myUrl = "";
    private StorageTask uploadTask;
    private Uri fileUri;

    private ProgressDialog Loadingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_single);

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverID = getIntent().getExtras().get("visit_user_id").toString();

        messageReceiverName = getIntent().getExtras().get("visit_user_name").toString();

        messageReceiverImage = getIntent().getExtras().get("visit_user_image").toString();

//        Toast.makeText(ChatSingleActivity.this,messageReceiverID,Toast.LENGTH_SHORT).show();
//        Toast.makeText(ChatSingleActivity.this,messageReceiverName,Toast.LENGTH_SHORT).show();

        InitializeControllers();

        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);

        SendmessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage();

            }
        });
        DisplayLastSeen();

        SendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence options[] = new CharSequence[]
                        {
                                "Images",
                                "PDF Files",
                                "Ms Word Files"
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatSingleActivity.this);
                builder.setTitle("Select the File");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0){
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent,"Select Image"),438);
                        }
                        if (i == 1){
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent,"Select PDF File"),438);
                        }
                        if (i == 2){
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent,"Select MS Word File"),438);
                        }
                    }
                });
                builder.show();
            }
        });
    }




    private void InitializeControllers() {


        ChatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(ChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled( true);



        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View actionBarView =  layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);


        userImage = (CircleImageView) findViewById(R.id.custom_profile_image);
        userName = (TextView)findViewById(R.id.custom_profile_name);
        userLastSeen =(TextView) findViewById(R.id.custom_user_last_seen);

        SendmessageButton = (ImageButton) findViewById(R.id.send_message_btn);
        SendFilesButton = (ImageButton) findViewById(R.id.send_files_btn);
        MessageInputText = (EditText) findViewById(R.id.input_message);

        messageAdapter = new MessageAdapter(messagesList);

        userMessagesList = (RecyclerView) findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);


        Loadingbar = new ProgressDialog(this);

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==438 && resultCode==RESULT_OK&&data!=null&&data.getData()!=null){

            Loadingbar.setTitle("Sending File");
            Loadingbar.setMessage("Please Wait While ARe Sending That File");
            Loadingbar.setCanceledOnTouchOutside(false);

            Loadingbar.show();


            fileUri = data.getData();
            if (!checker.equals("image")){

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");
                final String messageSenderRef = "Message/"+messageSenderId+"/"+messageReceiverID;
                final String messageReceiverRef = "Message/"+messageReceiverID+"/"+messageSenderId;
                DatabaseReference userMessageKeyRef =RootRef.child("Message")
                        .child(messageSenderId).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID).child(messagePushID+"."+checker);


                filePath.putFile(fileUri).continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()){
                            throw  task.getException();
                        }

                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            final Uri downloadUri = task.getResult();
                            String downloadUrll=String.valueOf(downloadUri);

                            Map<String,String> messageImageBody = new HashMap();
                            messageImageBody.put("message",downloadUrll);
                            messageImageBody.put("name",fileUri.getLastPathSegment());

                            messageImageBody.put("type",checker);
                            messageImageBody.put("from",messageSenderId);
                            messageImageBody.put("to",messageReceiverID);
                            messageImageBody.put("messageID",messagePushID);
                            messageImageBody.put("time",saveCurrentTime);
                            messageImageBody.put("date",saveCurrentTime);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageImageBody);
                            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageImageBody);

                            RootRef.updateChildren(messageBodyDetails);
                            Loadingbar.dismiss();

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Loadingbar.dismiss();
                        Toast.makeText(ChatSingleActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Loadingbar.dismiss();
                        Toast.makeText(ChatSingleActivity.this,"Uploaded Successfully",Toast.LENGTH_SHORT).show();
                    }
                });


            }else if (checker.equals("image")){

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                final String messageSenderRef = "Message/"+messageSenderId+"/"+messageReceiverID;
                final String messageReceiverRef = "Message/"+messageReceiverID+"/"+messageSenderId;
                DatabaseReference userMessageKeyRef =RootRef.child("Message")
                        .child(messageSenderId).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID).child(messagePushID+"."+"jpg");
                uploadTask = filePath.putFile(fileUri);


                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()){
                            throw  task.getException();
                        }

                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            Uri downloadUrl = task.getResult();
                            myUrl = downloadUrl.toString();


                            Map messageImageBody = new HashMap();
                            messageImageBody.put("message",myUrl);
                            messageImageBody.put("name",fileUri.getLastPathSegment());

                            messageImageBody.put("type",checker);
                            messageImageBody.put("from",messageSenderId);
                            messageImageBody.put("to",messageReceiverID);
                            messageImageBody.put("messageID",messagePushID);
                            messageImageBody.put("time",saveCurrentTime);
                            messageImageBody.put("date",saveCurrentTime);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageImageBody);
                            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageImageBody);

                            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if (task.isSuccessful()){
                                        Loadingbar.dismiss();
                                        Toast.makeText(ChatSingleActivity.this,"Message Sent Successfully...",Toast.LENGTH_SHORT).show();
                                    }else{
                                        Loadingbar.dismiss();
                                        Toast.makeText(ChatSingleActivity.this,"Something Went Wrong",Toast.LENGTH_SHORT).show();
                                    }
                                    MessageInputText.setText("");
                                }
                            });


                        }
                    }
                });


            }else{
                Loadingbar.dismiss();
                Toast.makeText(this,"Select Any one",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void DisplayLastSeen(){
        RootRef.child("Users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("userState").hasChild("state")){
                            String state = dataSnapshot.child("userState").child("state").getValue().toString();
                            String date = dataSnapshot.child("userState").child("date").getValue().toString();
                            String time = dataSnapshot.child("userState").child("time").getValue().toString();

                            if(state.equals("online")){
                                userLastSeen.setText("online");
                            }else if (state.equals("offline")){
                                userLastSeen.setText("Last Seen: "+date+" "+time);
                            }

                        }else{
                            userLastSeen.setText("offline");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }



    @Override
    protected void onStart() {
        super.onStart();

//        DisplayLastSeen();

        RootRef.child("Message").child(messageSenderId).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);

                        messagesList.add(messages);

                        messageAdapter.notifyDataSetChanged();

                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void SendMessage(){
        String messageText = MessageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)){
            Toast.makeText(this,"First write your Message...",Toast.LENGTH_SHORT);
        }else{
            String messageSenderRef = "Message/"+messageSenderId+"/"+messageReceiverID;
            String messageReceiverRef = "Message/"+messageReceiverID+"/"+messageSenderId;
            DatabaseReference userMessageKeyRef =RootRef.child("Message")
                    .child(messageSenderId).child(messageReceiverID).push();
            String messagePushID = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",messageText);
            messageTextBody.put("type","text");
            messageTextBody.put("from",messageSenderId);
            messageTextBody.put("to",messageReceiverID);
            messageTextBody.put("messageID",messagePushID);
            messageTextBody.put("time",saveCurrentTime);
            messageTextBody.put("date",saveCurrentTime);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageTextBody);
            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Toast.makeText(ChatSingleActivity.this,"Message Sent Successfully...",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ChatSingleActivity.this,"Something Went Wrong",Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText("");
                }
            });


        }
    }
}
