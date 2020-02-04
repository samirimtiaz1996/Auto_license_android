package com.samirimtiaz.auto_license;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {

    private CountryCodePicker ccp;
    private EditText phoneText;
    private EditText codeText;
    private Button continueAndNextBtn;
    private String checker="",phoneNumber="",mVerificationId;
    private RelativeLayout relativeLayout;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_otp);

        mAuth=FirebaseAuth.getInstance ();
        progressDialog=new ProgressDialog (this);
        phoneText=findViewById (R.id.phoneText);
        codeText=findViewById (R.id.codeText);
        continueAndNextBtn=findViewById (R.id.continueNextButton);
        relativeLayout=findViewById (R.id.phoneAuth);
        ccp=(CountryCodePicker) findViewById (R.id.ccp);
        ccp.registerCarrierNumberEditText (phoneText);
        continueAndNextBtn.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                if (continueAndNextBtn.getText ().equals ("Submit") || checker.equals ("Code Sent")){
                    String verificationCode=codeText.getText ().toString ();
                    if (verificationCode.equals ("")){
                        Toast.makeText (OTPActivity.this,"Please write verification code",Toast.LENGTH_LONG);
                    }
                    else {
                        progressDialog.setTitle ("Code Verification");
                        progressDialog.setMessage ("Please wait until code is received");
                        progressDialog.setCanceledOnTouchOutside (false);
                        progressDialog.show ();

                        PhoneAuthCredential phoneAuthCredential=PhoneAuthProvider.getCredential (mVerificationId,verificationCode);
                        signInWithPhoneAuthCredential (phoneAuthCredential);
                    }
                }
                else{
                    phoneNumber=ccp.getFullNumberWithPlus ();
                    if (!phoneNumber.equals ("")){
                        progressDialog.setTitle ("Phone Number Verification");
                        progressDialog.setMessage ("Please wait until number is verified");
                        progressDialog.setCanceledOnTouchOutside (false);
                        progressDialog.show ();
                        PhoneAuthProvider.getInstance().verifyPhoneNumber (
                                phoneNumber,        // Phone number to verify
                                60,                 // Timeout duration
                                TimeUnit.SECONDS,   // Unit of timeout
                                OTPActivity.this,               // Activity (for callback binding)
                                mCallbacks);        // OnVerificationStateChangedCallbacks

                    }
                    else{
                        Toast.makeText (OTPActivity.this,"Please Give Us A Valid Phone Number",Toast.LENGTH_SHORT);
                    }
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks () {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential (phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText (OTPActivity.this,"Invalid Phone Number . Error no :"+e.getMessage (),Toast.LENGTH_LONG);
                relativeLayout.setVisibility (View.VISIBLE);

                continueAndNextBtn.setText ("Continue");
                codeText.setVisibility (View.GONE);
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent (s, forceResendingToken);

                mVerificationId=s;
                resendingToken=forceResendingToken;
                relativeLayout.setVisibility (View.GONE);
                checker="Code Sent";
                continueAndNextBtn.setText ("Submit");
                codeText.setVisibility (View.VISIBLE);
                progressDialog.dismiss ();
                Toast.makeText (OTPActivity.this,"Verification Code Has Been Sent",Toast.LENGTH_LONG);
            }
        };
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult> () {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialog.dismiss ();
                            Toast.makeText (OTPActivity.this,"You Are Logged in",Toast.LENGTH_LONG);
                            sendUserToProfile ();
                        } else {
                            progressDialog.dismiss ();
                            String error=task.getException ().toString ();
                            Toast.makeText (OTPActivity.this,"Error: "+error,Toast.LENGTH_LONG);
                        }
                    }
                });
    }
    private void sendUserToProfile(){
        startActivity (new Intent (OTPActivity.this,ProfileViewActivity.class));
        finish ();
    }
}
