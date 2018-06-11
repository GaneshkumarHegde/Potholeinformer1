package com.project.hackathon.Potholeinformer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Signup extends Activity {

    private ProgressDialog progressDialog;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        progressDialog = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
    }
    public void signUp(View view) {
        EditText email = (EditText) findViewById(R.id.signupemailid);
        EditText pswd = (EditText) findViewById(R.id.signuppassword);
        EditText pswd1 = (EditText) findViewById(R.id.signuprepassword);

        if(TextUtils.isEmpty(email.getText().toString()))  email.setError("Required");
        else if(TextUtils.isEmpty(pswd.getText().toString()) ) pswd.setError("Required");
        else if(TextUtils.isEmpty(pswd1.getText().toString()) ) pswd1.setError("Required");
        else {
            String emailVal = email.getText().toString();
            String pswdVal = pswd.getText().toString();
            String pswdVal1 = pswd1.getText().toString();
            progressDialog.setMessage("Loading");
            progressDialog.show();

            if (!pswdVal.equals(pswdVal1)) {
                progressDialog.dismiss();
                pswd1.setError("Passwords don't match");
            }

            else if(pswdVal.length()<6){
                pswd.setError("Need min of 7 characters");

            }
            else {
                mAuth.createUserWithEmailAndPassword(emailVal.toString().trim(), pswdVal.toString().trim())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information


                                    FirebaseUser user = mAuth.getCurrentUser();
                                    update(user);
                                } else {
                                    // If sign in fails, display a message to the user.

                                    update(null);
                                }
                            }
                        });
            }
        }
    }

    public void update(FirebaseUser user)
    {
        if(user!=null){
            progressDialog.dismiss();
            Toast.makeText(this,"Successful. Logged in", Toast.LENGTH_LONG).show();
            Intent intent =new Intent(Signup.this,Signin.class);
            startActivity(intent);
        }
        else{
            progressDialog.dismiss();
            Toast.makeText(this,"Account already exists",Toast.LENGTH_LONG).show();
        }
    }
}
