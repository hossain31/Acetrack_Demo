package se.devex.acetrack_demo_v01;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    Button mButtonWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, String.format("*** onCreate()"));
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonWrite = (Button)findViewById(R.id.searchAcetrack);
        //Listener for the button mButtonWrite click
        mButtonWrite.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                //start new activity
                Intent intent= new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
