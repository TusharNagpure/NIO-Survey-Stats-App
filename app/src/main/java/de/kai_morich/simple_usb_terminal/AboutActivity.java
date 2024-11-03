package de.kai_morich.simple_usb_terminal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button (Back button) in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("About");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the Up button click
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity and return to the previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Method to open the project source code link
    public void openSourceCodeLink(android.view.View view) {
        String url = "https://github.com/TusharNagpure/NIO-Survey-Stats-App"; // Replace with your actual URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    // Method to open the documentation link
    public void openDocumentationLink(android.view.View view) {
        String url = "https://drive.google.com/file/d/1CNUzxTM7EQfK299h_iXMmiRnMkRl4JGR/view?usp=sharing"; // Replace with your actual URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    // Method to open the Kai-Morich package link
    public void openKaiMorichLink(android.view.View view) {
        String url = "https://github.com/kai-morich/SimpleUsbTerminal"; // Replace with the actual Kai-Morich package URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
