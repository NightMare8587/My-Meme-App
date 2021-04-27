package com.example.mymemeapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.developer.kalert.KAlertDialog;
import com.example.mymemeapp.Home.HomeScreen;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import karpuzoglu.enes.com.fastdialog.Animations;
import karpuzoglu.enes.com.fastdialog.FastDialog;
import karpuzoglu.enes.com.fastdialog.FastDialogBuilder;
import karpuzoglu.enes.com.fastdialog.NegativeClick;
import karpuzoglu.enes.com.fastdialog.PositiveClick;
import karpuzoglu.enes.com.fastdialog.Type;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    DatabaseReference reference;
    String verId;
    PhoneAuthCredential credential;
    FastDialog enterOtp;
    String phoneNumber;
    GoogleSignInAccount account;
    SignInButton signInButton;
    FastDialog loding;
    Button loginUsingNum;
    GoogleSignInOptions gso;
    FirebaseUser currentUser;
    GoogleSignInClient client;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        sharedPreferences = getSharedPreferences("AccountInfo",MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if(currentUser != null){
            startActivity(new Intent(this,HomeScreen.class));
            finish();
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        initialise();
        loginUsingNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastDialog fastDialog = new FastDialogBuilder(MainActivity.this, Type.DIALOG)
                        .setTitleText("Phone Authentication")
                        .setText("Enter Your Phone Number Below")
                        .setHint("Add Your Country Code i.e +91")
                        .setAnimation(Animations.SLIDE_TOP)
                        .setTextMaxLength(13)
                        .positiveText("Send OTP")
                        .negativeText("Cancel")
                        .create();
                fastDialog.show();
                fastDialog.positiveClickListener(new PositiveClick() {
                    @Override
                    public void onClick(View view) {
                        if(fastDialog.getInputText().length() <= 12){
                            Toast.makeText(MainActivity.this, "Invalid Number. Check Again", Toast.LENGTH_SHORT).show();
                            return;
                        }else{
                            loding = new FastDialogBuilder(MainActivity.this,Type.PROGRESS)
                                    .setAnimation(Animations.SLIDE_TOP)
                                    .progressText("Sending OTP.... Please Wait :)")
                                    .create();

                            loding.show();
                            phoneNumber = fastDialog.getInputText();
                            startPhoneNumberVerification(fastDialog.getInputText());
                            fastDialog.dismiss();
                        }
                    }
                });
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signIn = client.getSignInIntent();
                startActivityForResult(signIn,3);
            }
        });
    }

    private void initialise() {
        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference().getRoot();
        signInButton = findViewById(R.id.signInWithGoogle);
        loginUsingNum = findViewById(R.id.loginUsingPhoneNumber);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("651996831534-uo5o27749t9b568l9pfr0k7531q96gtt.apps.googleusercontent.com")
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(MainActivity.this,gso);
    }
    private void startPhoneNumberVerification(String number) {

        PhoneAuthOptions options =  PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setActivity(this)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        loding.dismiss();
                        credential = phoneAuthCredential;

                        signInWithPhoneAuthCredentials(phoneAuthCredential,number);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        Toast.makeText(MainActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
                        verId = s;
                        loding.dismiss();
                        enterOtp = new FastDialogBuilder(MainActivity.this,Type.DIALOG)
                                .setTitleText("Verify Code")
                                .setText("Enter OTP below")
                                .setHint("6 digit OTP")
                                .setTextMaxLength(6)
                                .setAnimation(Animations.SLIDE_TOP)
                                .positiveText("Verify")
                                .negativeText("Cancel")
                                .create();

                        enterOtp.show();
                        enterOtp.positiveClickListener(new PositiveClick() {
                            @Override
                            public void onClick(View view) {
                                if(enterOtp.getInputText().length() <= 5){
                                    Toast.makeText(MainActivity.this, "Enter 6 digit OTP", Toast.LENGTH_SHORT).show();
                                    return;
                                }else{
                                    credential = PhoneAuthProvider.getCredential(verId,enterOtp.getInputText());
                                    auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(MainActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                                                reference.child("Users").child(Objects.requireNonNull(auth.getUid())).child("number").setValue(phoneNumber);
                                                startActivity(new Intent(getApplicationContext(),HomeScreen.class));
                                                finish();
                                            }
                                        }
                                    });
                                }
                            }
                        });

                        enterOtp.negativeClickListener(new NegativeClick() {
                            @Override
                            public void onClick(View view) {
                                enterOtp.dismiss();
                                Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }



    private void signInWithPhoneAuthCredentials(PhoneAuthCredential phoneAuthCredential,String number) {
        auth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                Toast.makeText(MainActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();

                reference.child("Users").child(Objects.requireNonNull(auth.getUid())).child("number").setValue(number);
                startActivity(new Intent(getApplicationContext(),HomeScreen.class));
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 3){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            Toast.makeText(this, "Sign In", Toast.LENGTH_SHORT).show();
            createFirebaseAuthID(account.getIdToken());

            getSignInInformation();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, e.getLocalizedMessage()+"", Toast.LENGTH_SHORT).show();
        }
    }

    private void createFirebaseAuthID(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            auth= FirebaseAuth.getInstance();
                            FirebaseUser user = auth.getCurrentUser();
                            assert user != null;
                            auth.updateCurrentUser(user);
                            reference = FirebaseDatabase.getInstance().getReference().getRoot();
                            GoogleSignInDB googleSignInDB = new GoogleSignInDB(account.getDisplayName(),account.getEmail());
                            reference.child("Users").child(user.getUid()).setValue(googleSignInDB);
                            startActivity(new Intent(MainActivity.this, HomeScreen.class));
                            finish();
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
//                            updateUI(null);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            else{
                new KAlertDialog(MainActivity.this,KAlertDialog.ERROR_TYPE)
                        .setTitleText("Permission Required")
                        .setContentText("Without Storage Permission You can't share memes\nWanna provide permission??")
                        .setConfirmText("Yes")
                        .setCancelText("No")
                        .setConfirmClickListener(new KAlertDialog.KAlertClickListener() {
                            @Override
                            public void onClick(KAlertDialog kAlertDialog) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                                kAlertDialog.dismissWithAnimation();
                            }
                        }).setCancelClickListener(new KAlertDialog.KAlertClickListener() {
                    @Override
                    public void onClick(KAlertDialog kAlertDialog) {
                        kAlertDialog.dismissWithAnimation();
                    }
                }).show();
            }
        }
    }

    private void getSignInInformation() {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();
            editor.putString("email",personEmail);
            editor.putString("name",personName);
            editor.apply();
            Log.i("info",personName+ " " + personEmail + " " + personFamilyName);
        }
    }



}