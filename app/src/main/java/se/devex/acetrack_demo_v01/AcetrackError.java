package se.devex.acetrack_demo_v01;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class AcetrackError extends AppCompatActivity {
    @Override
    protected void onCreate (Bundle saveInstanceState){
        setTheme(R.style.AppTheme);
        super.onCreate(saveInstanceState);
        setContentView(R.layout.acetrack_error);

        Button mButtonError = (Button)findViewById(R.id.errorButton);
        //Listener for the button mButtonError click
        mButtonError.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                //start new activity
                Intent intent= new Intent(AcetrackError.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}

