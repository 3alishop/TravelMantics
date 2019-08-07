package com.apptitia.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    ImageView imageView;
    TravelDeal deal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        //FirebaseUtil.openFbReference("traveldeals", this);


        //mFirebaseDatabase = FirebaseDatabase.getInstance();
        //remplacé par

        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;

        //mDatabaseReference = mFirebaseDatabase.getReference().child("traveldeals");
        //remplacé par

        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);

        imageView = findViewById(R.id.image);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());

        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                // EXTRA_LOCAL_ONLY ercieve only the data on the device
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true );
                startActivityForResult(intent.createChooser(intent,"Insert Picture"),PICTURE_RESULT);
            }
        });
    }

    //write on the database
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal Saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted",Toast.LENGTH_LONG).show();
                backToList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        if(FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditText(true);
            findViewById(R.id.btnImage).setEnabled(true);
        }
        else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditText(false);
            findViewById(R.id.btnImage).setEnabled(false);

        }
        return true;
    }

    //use firebase storage folder
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            //locate the file you want to uplad, put the location on the Uri object
            Uri imageUri = data.getData();
            //we need the reference of the storage where you want to upload the data

            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());

            //calla the putFile method on the reference, it will return an asyncronous task calles Upload Task
            //ref.putFile(imageUri);
            //UploadTask uploadTask = ref.putFile(imageUri);
            //we can listen to succes or failure, we need to take action on that
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    //String url = taskSnapshot.getUploadUri().toString();

                    //String url = taskSnapshot.getStorage().getDownloadUrl().getResult().toString();
                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    Uri url = uri.getResult();

                    String pictureName = taskSnapshot.getStorage().getPath();
                    deal.setImageUrl(url.toString());
                    deal.setImageName(pictureName);
                    Log.d("Url: ", url.toString());
                    Log.d("Name: ", pictureName);

                    showImage(url.toString());
                }
            });
        }
    }

    //creating the saveDeal() Method
    private  void  saveDeal(){
        //we will read the content of the 3 fields
/*
        String title = txtTitle.getText().toString();
        String description = txtDescription.getText().toString();
        String price = txtPrice.getText().toString();

*/

        //new way to do it
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription( txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());

        //logic
        if(deal.getId() == null){
            //inset a new object to the database, we need to use the push method
            mDatabaseReference.push().setValue(deal);
        }
        else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }


        //TravelDeal deal = new TravelDeal(title, description, price,"" );



    }


    //methode permettant de supprimer  un deal
    private void deleteDeal(){
        //will check if the deal exists
        if(deal == null){
            //s'il n'existe pas, on affiche un message
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_SHORT).show();
            return;
        }
        //on recupere la reference du deal courant
        mDatabaseReference.child(deal.getId()).removeValue();
        if(deal.getImageName() != null && deal.getImageName().isEmpty() == false){
            //get ref of the file we want to delete passing his name
            //how to get the name file of the image?
            //we only have the link of it
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
            //then call the delete method over the reference
            //it will return an asyncronic task
            //add onSucces listner
            //and  onFail listener
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image","Image Succesfully Deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image",e.getMessage());
                }
            });

        }
    }

    // back to list method pour revenir à list Activity after saving
    private void backToList(){
        Intent intent = new Intent(this ,ListActivity.class);
        startActivity(intent);
    }

    //creating the clean() Method
    private  void  clean(){
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void enableEditText(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
    }

    //methode qui  permet d'affichier une image
    public void showImage(String url){
         if(url != null && url.isEmpty() == false){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
             Picasso.with(this).load(url).resize(width,width*2/3).centerCrop().into(imageView);
         }
    }
}
