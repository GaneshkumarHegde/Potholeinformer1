package com.project.hackathon.Potholeinformer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.net.InetAddress;

import static com.google.android.gms.plus.PlusOneDummyView.TAG;

public class Signin extends Activity {

    TextView t;
    Button loginbtn;
    ProgressDialog progressDialog1;
    FirebaseAuth mAuth;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.signin);



            mAuth = FirebaseAuth.getInstance();
            progressDialog1 = new ProgressDialog(this);

            t =(TextView)findViewById(R.id.signuptxt);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Signin.this,Signup.class);
                    startActivity(intent);
                }
            });


                FirebaseUser currentUser = mAuth.getCurrentUser();
                checkLogin(currentUser);


        }

    private void checkLogin(FirebaseUser currentUser) {
        if(currentUser!=null){

            Toast.makeText(this,"Logged in",Toast.LENGTH_LONG).show();

            Intent intent =new Intent(Signin.this,MainActivity.class);
            startActivity(intent);
        }

    }


    public void signIn(View view){

        EditText email = (EditText)findViewById(R.id.emailid);

        EditText password = (EditText)findViewById(R.id.password);


        if(TextUtils.isEmpty(email.getText().toString()))  email.setError("Required");
        else if(TextUtils.isEmpty(password.getText().toString())) password.setError("Required");
        else {
            String emailid = email.getText().toString();
            String pswd = password.getText().toString();
            progressDialog1.setMessage("Loading...");
            progressDialog1.show();

            mAuth.signInWithEmailAndPassword(emailid.toString(), pswd.toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateLogin(user);
                                progressDialog1.dismiss();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                progressDialog1.dismiss();
                                updateLogin(null);
                            }

                            // ...
                        }
                    });

        }
    }
    private void updateLogin(FirebaseUser user)
    {
        if(user!=null){

            Toast.makeText(this,"Logged in",Toast.LENGTH_LONG).show();

            Intent intent =new Intent(Signin.this,MainActivity.class);
            startActivity(intent);
        }
        else
        {

            Toast.makeText(this,"Failed", Toast.LENGTH_LONG).show();

        }
    }

    public void adminSignin(View view){
        Intent intent =new Intent(Signin.this,AdminActivity.class);
        startActivity(intent);
    }

}
