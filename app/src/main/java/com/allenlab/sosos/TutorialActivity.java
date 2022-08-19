package com.allenlab.sosos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TutorialActivity extends AppCompatActivity {
    //배경회색 #b3b3b3
    int index = 0;
    ImageView tutorial;
    TextView page;
    int[] pages = {R.drawable.slide_one,R.drawable.slide_two,R.drawable.slide_three,
            R.drawable.slide_four,R.drawable.slide_five,R.drawable.slide_six};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        tutorial = (ImageView)findViewById(R.id.tuto);
        page = (TextView)findViewById(R.id.pageNumber);
        Button pButton = (Button)findViewById(R.id.pastButton);
        Button nButton = (Button)findViewById(R.id.nextButton);

        pButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if ( 1 <= index && index <= 5){
                    index--;
                    tutorial.setImageResource(pages[index]);
                    page.setText(Integer.toString(index+1)+"/6");
                }else if(index == 0){
                    Toast.makeText(getApplicationContext(), "첫번째 페이지 입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        nButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if ( 0 <= index && index <= 4){
                    index++;
                    tutorial.setImageResource(pages[index]);
                    page.setText(Integer.toString(index+1)+"/6");
                }else if(index == 5){
                    Intent intent = new Intent(getApplicationContext(), PermissionActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}