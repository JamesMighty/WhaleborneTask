package org.whalebone.teams;

import lombok.extern.java.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
public class WhaleboneTeamsTestBase {
    public record Division(String name, int id) {}
    public record Team(Division division, int firstYearOfPlay, String name, int founded, String officialSiteUrl, String location) {}


    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public JSONObject fetchJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            log.log(Level.INFO, String.format("Fetched data from %s:\n%s", url, json.toString(4)));
            return json;
        } finally {
            is.close();
        }
    }

    public List<Team> jsonToTeams(JSONObject obj) {
        JSONArray teamsRaw = obj.getJSONArray("teams");
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamsRaw.length(); i++){
            JSONObject team = teamsRaw.getJSONObject(i);
            JSONObject div = team.getJSONObject("division");
            teams.add(
                    new Team(
                            new Division(
                                    div.getString("name"),
                                    div.getInt("id")
                            ),
                            team.getInt("firstYearOfPlay"),
                            team.getString("name"),
                            team.getInt("founded"),
                            team.getString("officialSiteUrl"),
                            team.getString("location")
                    )
            );
        }
        return teams;
    }

}
