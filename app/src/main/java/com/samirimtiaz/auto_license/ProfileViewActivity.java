package com.samirimtiaz.auto_license;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.picasso.transformations.CropSquareTransformation;

public class ProfileViewActivity extends AppCompatActivity {

    ImageView profile_image,nidfront,nidback;
    Button savebtn;
    EditText fullname,nid;
    private TextView birthdate,testText;
    Calendar calendar;
    private DatePickerDialog.OnDateSetListener dateSetListener;
    private static int GalleryPickProfile=1,GalleryPickNidFront=2,GalleryPickNidBack=3;
    Uri imageUriProfile,imageUriNidFront,imageUriNidBack;
    StorageReference firebaseUserImageRef,firebaseNidFrontRef,firebaseNidBackRef;
    String downloadUrl,getFullName,getNID,getBirthdate,getProfileImageLink,getNidFrontLink,getNidBackLink;
    private DatabaseReference customerRef;
    final HashMap<String,Object> profileMap = new HashMap<> ();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_profile_view);
        init ();

        profile_image.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                Intent galleryIntent= new Intent ();
                galleryIntent.setAction (Intent.ACTION_GET_CONTENT);
                galleryIntent.setType ("image/*");
                startActivityForResult (galleryIntent,GalleryPickProfile);

            }
        });

        nidfront.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                Intent galleryIntent= new Intent ();
                galleryIntent.setAction (Intent.ACTION_GET_CONTENT);
                galleryIntent.setType ("image/*");
                startActivityForResult (galleryIntent,GalleryPickNidFront);
            }
        });

        nidback.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                Intent galleryIntent= new Intent ();
                galleryIntent.setAction (Intent.ACTION_GET_CONTENT);
                galleryIntent.setType ("image/*");
                startActivityForResult (galleryIntent,GalleryPickNidBack);
            }
        });

        savebtn.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });

        birthdate.setOnClickListener (new View.OnClickListener () {
            int year=calendar.get (Calendar.YEAR);
            int month=calendar.get (Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog=new DatePickerDialog (ProfileViewActivity.this,android.R.style.Theme_Holo_Light_Dialog_MinWidth,dateSetListener,
                        year,month,day);
                dialog.getWindow ().setBackgroundDrawable (new ColorDrawable (Color.TRANSPARENT));
                dialog.show ();
            }
        });
        dateSetListener= new DatePickerDialog.OnDateSetListener () {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                birthdate.setText (dayOfMonth+"-"+(month+1)+"-"+year);

            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult (requestCode, resultCode, data);

        if(requestCode==GalleryPickProfile && resultCode==RESULT_OK &&data!=null){
            imageUriProfile=data.getData ();
            profile_image.setImageURI (imageUriProfile);
            uploadProfileImage ();
        }
        else if(requestCode==GalleryPickNidFront && resultCode==RESULT_OK &&data!=null){
            imageUriNidFront=data.getData ();
            nidfront.setImageURI (imageUriNidFront);
            uploadNidFront ();
        }
        else if(requestCode==GalleryPickNidBack && resultCode==RESULT_OK &&data!=null){
            imageUriNidBack=data.getData ();;
            nidback.setImageURI (imageUriNidBack);
            uploadNidBack ();
        }

    }

    private void saveUserData(){
         getFullName = fullname.getText ().toString ();
         getNID=nid.getText ().toString ();
         getBirthdate=birthdate.getText ().toString ();

        if( ((imageUriProfile!=null && imageUriNidFront!=null && imageUriNidBack!=null)||
                (getProfileImageLink!=null && getNidFrontLink!=null && getNidBackLink!=null))

                && !getFullName.equals ("") && !getNID.equals ("") && !getBirthdate.equals ("")){
            uploadDataOfStrings ();
            verifyNidandFace();
        }

        else{
            Toast.makeText (ProfileViewActivity.this,"Please Fill Up Everything to Continue",Toast.LENGTH_LONG).show ();
        }
    }

    private void verifyNidandFace(){
        Bitmap bm=((BitmapDrawable)nidfront.getDrawable()).getBitmap();
        FirebaseVisionImage image=FirebaseVisionImage.fromBitmap (bm);
        FirebaseVisionTextRecognizer detector= FirebaseVision.getInstance ().getOnDeviceTextRecognizer ();
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText> () {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                displayTextFromImage(firebaseVisionText);
                                // Task completed successfully
                                // ...
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener () {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText (ProfileViewActivity.this,"Error text recognition :"+e.getMessage (),Toast.LENGTH_LONG).show ();
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

    }

    private void uploadProfileImage(){
        final StorageReference filepathUserImage = firebaseUserImageRef.
                child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ());
        final UploadTask uploadTaskImage=filepathUserImage.putFile (imageUriProfile);

        uploadTaskImage.continueWithTask (new Continuation<UploadTask.TaskSnapshot, Task<Uri>> () {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                if (!task.isSuccessful ()){
                    throw task.getException ();
                }
                downloadUrl=filepathUserImage.getDownloadUrl ().toString ();
                return filepathUserImage.getDownloadUrl ();
            }
        }).addOnCompleteListener (new OnCompleteListener<Uri> () {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful ()){
                    downloadUrl = task.getResult ().toString ();
                    profileMap.put ("uid",FirebaseAuth.getInstance ().getCurrentUser ().getUid ());
                    profileMap.put ("ImageOfFace",downloadUrl);

                    customerRef.child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ()).updateChildren (profileMap);

                }
            }
        });
    }

    private void uploadNidFront(){
        final StorageReference filepathNidFront = firebaseNidFrontRef.
                child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ());
        final UploadTask uploadTaskNidFront=filepathNidFront.putFile (imageUriNidFront);
        uploadTaskNidFront.continueWithTask (new Continuation<UploadTask.TaskSnapshot, Task<Uri>> () {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                if (!task.isSuccessful ()){
                    throw task.getException ();
                }
                downloadUrl=filepathNidFront.getDownloadUrl ().toString ();
                return filepathNidFront.getDownloadUrl ();
            }
        }).addOnCompleteListener (new OnCompleteListener<Uri> () {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful ()){
                    downloadUrl = task.getResult ().toString ();
                    profileMap.put ("ImageOfNidFront",downloadUrl);

                    customerRef.child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ()).updateChildren (profileMap);

                }
            }
        });

    }

    private void uploadNidBack(){
        final StorageReference filepathNidBack = firebaseNidBackRef.
                child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ());
        final UploadTask uploadTaskNidBack=filepathNidBack.putFile (imageUriNidBack);
        uploadTaskNidBack.continueWithTask (new Continuation<UploadTask.TaskSnapshot, Task<Uri>> () {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                if (!task.isSuccessful ()){
                    throw task.getException ();
                }
                downloadUrl=filepathNidBack.getDownloadUrl ().toString ();
                return filepathNidBack.getDownloadUrl ();
            }
        }).addOnCompleteListener (new OnCompleteListener<Uri> () {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful ()){
                    downloadUrl = task.getResult ().toString ();
                    customerRef.child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ()).updateChildren (profileMap);
                }
            }
        });
    }

    private void uploadDataOfStrings(){
        profileMap.put ("Fullname",getFullName);
        profileMap.put ("NID_NO",getNID);
        profileMap.put ("Birthdate",getBirthdate);
        customerRef.child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ()).updateChildren (profileMap);

    }

    private  void  retrieveUserInfo(){
        customerRef.child (FirebaseAuth.getInstance ().getCurrentUser ().getUid ()).addValueEventListener (new ValueEventListener () {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists ()){

                    fullname.setText ((CharSequence) dataSnapshot.child ("Fullname").getValue ());
                    nid.setText ((CharSequence) dataSnapshot.child ("NID_NO").getValue ());
                    birthdate.setText ((CharSequence) dataSnapshot.child ("Birthdate").getValue ());

                    Picasso.get ().load (dataSnapshot.child ("ImageOfFace").getValue ().toString ()).placeholder (R.drawable.profile_image).into (profile_image);
                    Picasso.get ().load (dataSnapshot.child ("ImageOfNidFront").getValue ().toString ()).placeholder (R.drawable.nidfront).into (nidfront);
                    Picasso.get ().load (dataSnapshot.child ("ImageOfNidBack").getValue ().toString ()).placeholder (R.drawable.nidback).into (nidback);

                    getProfileImageLink=  dataSnapshot.child ("ImageOfFace").getValue ().toString ();
                    getNidFrontLink=  dataSnapshot.child ("ImageOfNidFront").getValue ().toString ();
                    getNidBackLink=dataSnapshot.child ("ImageOfNidFront").getValue ().toString ();


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText){
        List<FirebaseVisionText.TextBlock> blockList=firebaseVisionText.getTextBlocks ();
        if (blockList.size ()>0){
            for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks ()){
                String text=block.getText ();
                testText.setText (text);
            }
        }
        else {
            Toast.makeText (ProfileViewActivity.this,"No text detected",Toast.LENGTH_LONG).show ();
        }
    }

    private void init(){
        profile_image=findViewById (R.id.profile_image);
        nidfront=findViewById (R.id.nidfront);
        nidback=findViewById (R.id.nidback);
        savebtn=findViewById (R.id.savebtn);
        fullname=findViewById (R.id.fullname);
        nid=findViewById (R.id.nid);
        birthdate= findViewById (R.id.birthdate);
        testText=findViewById (R.id.testText);
        calendar=Calendar.getInstance ();
        firebaseUserImageRef= FirebaseStorage.getInstance ().getReference ().child ("User_Images");
        firebaseNidFrontRef= FirebaseStorage.getInstance ().getReference ().child ("NID_Front_Images");
        firebaseNidBackRef= FirebaseStorage.getInstance ().getReference ().child ("NID_Back_Images");
        customerRef= FirebaseDatabase.getInstance ().getReference ().child("Customers");
        retrieveUserInfo ();

    }


}
