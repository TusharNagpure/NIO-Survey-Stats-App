package de.kai_morich.simple_usb_terminal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Set the Toolbar as the ActionBar
        setTitle("NIO Survey Stats");
        toolbar.setTitleTextColor(Color.WHITE);

        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new HomeFragment())
                            .commit();
                } else if (id == R.id.nav_gps) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new GpsFragment())
                            .commit();
                } else if (id == R.id.nav_terminal) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new TerminalFragment())
                            .commit();
                } else if (id == R.id.nav_about) { // New item for the About section
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // Set the initial fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new HomeFragment())
                    .commit();
        }
    }
}
