package io.pristine.firebasesample;

import android.app.Activity;
import android.content.res.AssetManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import io.pristine.firebasesample.databinding.ActivityMainBinding;

public class MainActivity extends Activity implements OnProgressListener<UploadTask
    .TaskSnapshot>, OnPausedListener<UploadTask.TaskSnapshot>, OnSuccessListener<UploadTask.TaskSnapshot> {
    private static final String TAG = "[MAIN]";

    private final Progress progress = new Progress();
    private Handler backgroundHandler;

    private StorageReference storageReference;
    private Uri videoFileUri;
    private StorageMetadata metadata;
    private UploadTask uploadTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      HandlerThread backgroundHandlerThread = new HandlerThread("background", Process
          .THREAD_PRIORITY_BACKGROUND);
      backgroundHandlerThread.start();

      backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
      final ActivityMainBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
      binding.setProgress(progress);
      printGooglePlayAvailability();

      binding.pause.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
              uploadTask.pause();
            }
          });
          binding.resume.requestFocus();
        }
      });
      binding.pause.requestFocus();

      binding.resume.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
              uploadTask.resume();
            }
          });
          binding.pause.requestFocus();
        }
      });
      FirebaseAuth.getInstance().signInWithEmailAndPassword("nobody@nowhere.com", "nobody").addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
        @Override public void onComplete(@NonNull Task<AuthResult> task) {
          Log.i(TAG, "Sign in complete");
          testStorageUpload();
        }
      }).addOnFailureListener(this, new OnFailureListener() {
        @Override public void onFailure(@NonNull Exception e) {
          Log.i(TAG, "Sign in failure", e);
        }
      });

    }


    private void testStorageUpload() {
      backgroundHandler.post(new Runnable() {
        @Override public void run() {
          File videoFile = new File(getExternalFilesDir(null), "niknak.mp4");
          if (!videoFile.exists()) {
            progress.status.set("copying file to private storage");
            try {
              videoFile.createNewFile();
              copyAssets();
              Log.i(TAG, "Copied file to private app storage");
            } catch (IOException e) {
              Log.e(TAG, "Failed to copy niknak.mp4 to private storage", e);
            }
          }

          progress.total.set(Long.toString(videoFile.length()));
          storageReference = FirebaseStorage.getInstance().getReference().child(Long.toString(System.currentTimeMillis()));
          videoFileUri = Uri.fromFile(videoFile);
          startUploadTask();
        }
      });
    }

    private void startUploadTask() {
      StorageMetadata.Builder builder = new StorageMetadata.Builder();

      builder.setCustomMetadata("mediaId", "random field")
          .setCustomMetadata("owner", FirebaseAuth.getInstance().getCurrentUser().getUid())
          .setContentLanguage(Locale.getDefault().getDisplayLanguage())
          .setContentEncoding(Charset.defaultCharset().displayName())
          .setContentType("video/mp4");

      metadata = builder.build();


      progress.status.set("starting upload task");
      uploadTask = storageReference.putFile(videoFileUri, metadata);
      uploadTask.addOnPausedListener(MainActivity.this).addOnSuccessListener(MainActivity.this)
          .addOnProgressListener(MainActivity.this).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          Log.e(TAG, "Exception ", e);
        }
      });

    }



    @Override
    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
      progress.status.set("Successfully uploaded");
    }

    @Override
    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
      progress.status.set( "Paused upload");
    }

    @Override
    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
      long totalBytesTransferred = taskSnapshot.getBytesTransferred();
      progress.status.set("Progress " + totalBytesTransferred);
      progress.transferred.set(Long.toString(totalBytesTransferred));
    }


  private void copyAssets() {
    AssetManager assetManager = getAssets();
    String[] files = null;
    try {
      files = assetManager.list("");
    } catch (IOException e) {
      Log.e("tag", "Failed to get asset file list.", e);
    }
    if (files != null) for (String filename : files) {
      InputStream in = null;
      OutputStream out = null;
      try {
        in = assetManager.open(filename);
        File outFile = new File(getExternalFilesDir(null), filename);
        out = new FileOutputStream(outFile);
        copyFile(in, out);
      } catch(IOException e) {
      }
      finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            // NOOP
          }
        }
        if (out != null) {
          try {
            out.close();
          } catch (IOException e) {
            // NOOP
          }
        }
      }
    }
  }
  private void copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while((read = in.read(buffer)) != -1){
      out.write(buffer, 0, read);
    }
  }

    private void printGooglePlayAvailability() {
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
    }


}
