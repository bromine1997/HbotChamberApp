package com.mcsl.hbotchamberapp.repository;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcsl.hbotchamberapp.model.ProfileSection;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileRepository {

    private Context context;

    public ProfileRepository(Context context) {
        this.context = context;
    }

    public List<ProfileSection> loadProfileDataFromFile() {
        List<String[]> stringData = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput("profile_data.json");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            Type type = new TypeToken<List<String[]>>() {}.getType();
            stringData = gson.fromJson(sb.toString(), type);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
        }

        return convertToProfileSection(stringData);
    }

    private List<ProfileSection> convertToProfileSection(List<String[]> stringData) {
        List<ProfileSection> profileSections = new ArrayList<>();
        for (String[] section : stringData) {
            if (section.length >= 3) {
                float startPressure = Float.parseFloat(section[1]);
                float endPressure = Float.parseFloat(section[2]);
                float duration = Float.parseFloat(section[3]);

                profileSections.add(new ProfileSection("SectionName", startPressure, endPressure, duration));
            }
        }
        return profileSections;
    }
}
