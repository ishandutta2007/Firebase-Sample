package io.pristine.firebasesample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends Activity {
    private static final String TAG = "[MAIN]";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FirebaseAuth authentication = FirebaseAuth.getInstance();

        String result = null;
        int availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        switch (availability) {
            case ConnectionResult.SUCCESS:
                result = "SUCCESS";
                break;
            case ConnectionResult.SERVICE_MISSING:
                result = "SERVICE_MISSING";
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                result = "SERVICE_VERSION_UPDATE_REQUIRED";
                break;
            case ConnectionResult.SERVICE_DISABLED:
                result = "SERVICE_DISABLED";
                break;
            case ConnectionResult.SIGN_IN_REQUIRED:
                result = "SIGN_IN_REQUIRED";
                break;
            case ConnectionResult.INVALID_ACCOUNT:
                result = "INVALID_ACCOUNT";
                break;
            case ConnectionResult.RESOLUTION_REQUIRED:
                result = "RESOLUTION_REQUIRED";
                break;
            case ConnectionResult.NETWORK_ERROR:
                result = "NETWORK_ERROR";
                break;
            case ConnectionResult.INTERNAL_ERROR:
                result = "INTERNAL_ERROR";
                break;
            case ConnectionResult.SERVICE_INVALID:
                result = "SERVICE_INVALID";
                break;
            case ConnectionResult.DEVELOPER_ERROR:
                result = "DEVELOPER_ERROR";
                break;
            case ConnectionResult.LICENSE_CHECK_FAILED:
                result = "LICENSE_CHECK_FAILED";
                break;
            case ConnectionResult.CANCELED:
                result = "CANCELED";
                break;
            case ConnectionResult.TIMEOUT:
                result = "TIMEOUT";
                break;
            case ConnectionResult.INTERRUPTED:
                result = "INTERRUPTED";
                break;
            case ConnectionResult.API_UNAVAILABLE:
                result = "API_UNAVAILABLE";
                break;
            case ConnectionResult.SIGN_IN_FAILED:
                result = "SIGN_IN_FAILED";
                break;
            case ConnectionResult.SERVICE_UPDATING:
                result = "SERVICE_UPDATING";
                break;
            case ConnectionResult.SERVICE_MISSING_PERMISSION:
                result = "SERVICE_MISSING_PERMISSION";
                break;
            case ConnectionResult.RESTRICTED_PROFILE:
                result = "RESTRICTED_PROFILE";
                break;
        }

        if (result != null) {
            Log.i(TAG, "Google Play Availability: " + result);
        } else {
            Log.wtf(TAG, "No google play availability returned?");
        }

        authentication.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.i(TAG, "Auth state changed");
                FirebaseUser user = authentication.getCurrentUser();
                if (user == null) {
                    Log.i(TAG, "Not logged in");
                } else {
                    Log.i(TAG, "Logged in as : " + user.getUid());
                }
            }
        });

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference authenticatedReference = database.getReference("/authenticated/");

        DatabaseReference unauthenticatedReference = database.getReference("/unauthenticated/");
        unauthenticatedReference.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                String result = dataSnapshot.getValue()  == null ? null : dataSnapshot.getValue().toString();
                Log.i(TAG, result + " changed at " + dataSnapshot.getRef().toString());
            }
            @Override public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Cancelled unauthenticated", databaseError.toException());
            }
        });

        Log.i(TAG, "Write value to unauthenticated reference");
        unauthenticatedReference.setValue("yolo swag lyfestyle " + System.currentTimeMillis());




        onSignedIn(authenticatedReference);

        authentication.signInWithEmailAndPassword("nobody@nowhere.com", "nobody").addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override public void onComplete(@NonNull Task<AuthResult> task) {
                Log.i(TAG, "Sign in complete");
                onSignedIn(authenticatedReference);

            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "Sign in failure", e);
            }
        });



        authenticatedReference.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                String result = dataSnapshot.getValue()  == null ? null : dataSnapshot.getValue().toString();
                Log.i(TAG, result + " changed at " + dataSnapshot.getRef().toString());
            }
            @Override public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Cancelled authenticated", databaseError.toException());
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                onSignedIn(authenticatedReference);
            }
        }, 3000);


    }

    public void onSignedIn(DatabaseReference authenticatedReference) {

        Log.i(TAG, "Write value to authenticated reference");
        authenticatedReference.setValue("yolo swag lyfestyle ".toUpperCase() + System.currentTimeMillis());
    }
}
