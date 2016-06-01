package xyz.darkphoenix.pointer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {

    private PermissionChecker mPermissionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionChecker = new PermissionChecker(this);
        if (!mPermissionChecker.isRequiredPermissionGranted()) {
            Intent intent = mPermissionChecker.createRequiredPermissionIntent();
            startActivityForResult(intent, PermissionChecker.REQUIRED_PERMISSION_REQUEST_CODE);
        }

        finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PermissionChecker.REQUIRED_PERMISSION_REQUEST_CODE) {
            if (!mPermissionChecker.isRequiredPermissionGranted()) {
                Toast.makeText(this, "Required permission is not granted. Please restart the app and grant required permission.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
