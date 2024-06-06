package org.whalebone.teams;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.java.Log;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.whalebone.teams.pom.MCPage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class WhaleboneTeamsTest extends WhaleboneTeamsTestBase {

    private static final String ENDPOINT_URL = "https://qa-assignment.dev1.whalebone.io/api/teams";
    private static final String CAN = "CAN";
    private static final String USA = "USA";
    private static final int EXPECTED_TEAM_COUNT = 32;
    private static final int METROPOLITAN_DIVISION_ID = 2;
    private static final String EXPECTED_OLDEST_TEAM = "Montreal Canadiens";
    private static final String EXPECTED_OLDEST_TEAM_WEBSITE_URL = "https://www.nhl.com/fr/canadiens/";
    private static final List<String> CITIES_WITH_MULTIPLE_TEAMS = Collections.singletonList("New York");
    private static final List<String> METROPOLITAN_DIVISION_TEAMS = Arrays.asList(
            "Carolina Hurricanes",
            "Columbus Blue Jackets",
            "New Jersey Devils",
            "New York Islanders",
            "New York Rangers",
            "Philadelphia Flyers",
            "Pittsburgh Penguins",
            "Washington Capitals"
    );


    private List<Team> teamList;

    @BeforeClass
    public void Setup() throws IOException {
        teamList = jsonToTeams(fetchJsonFromUrl(ENDPOINT_URL));
    }

    /**
     * verify the response returned expected count of teams (32 in total)
     */
    @Test
    public void VerifyResponseTeamsCount() {
        final int teamCount = teamList.size();

        Assert.assertEquals(teamCount, EXPECTED_TEAM_COUNT);
    }

    /**
     * verify the oldest team is Montreal Canadiens
     */
    @Test
    public void VerifyOldestTeam() {
        final Comparator<Team> comp = Comparator.comparing(Team::founded);
        final List<Team> sorted = teamList.stream().sorted(comp).toList();

        final String teamName = sorted.get(0).name();

        Assert.assertEquals(teamName, EXPECTED_OLDEST_TEAM);
    }

    /**
     * verify there's a city with more than 1 team and verify names of those teams
     */
    @Test
    public void VerifyCityTeamCount() {
        final Map<String, List<Team>> groupedByLocation = teamList.stream().collect(Collectors.groupingBy(Team::location));
        final List<Map.Entry<String, List<Team>>> filtered = groupedByLocation.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1).toList();

        assert filtered.size() > 0 : "Expected at least one city with more than one team, but found none";

        final List<String> teamsNames = filtered.stream().map(Map.Entry::getKey).collect(Collectors.toList());

        Assert.assertEquals(teamsNames, CITIES_WITH_MULTIPLE_TEAMS);
    }

    /**
     * verify there are 8 teams in the Metropolitan division and verify them by their names
     */
    @Test
    public void VerifyTeamsInMetropolitanDivision() {
        final Map<Division, List<Team>> groupedByDivision = teamList.stream()
                .collect(Collectors.groupingBy(Team::division));
        final Optional<List<Team>> metropolitanDivisionTeams = groupedByDivision.entrySet().stream()
                .filter(entry -> entry.getKey().id() == METROPOLITAN_DIVISION_ID)
                .map(Map.Entry::getValue).findFirst();

        assert metropolitanDivisionTeams.isPresent();

        final List<String> teamNames = metropolitanDivisionTeams.get().stream()
                .map(Team::name).collect(Collectors.toList());

        Assert.assertEquals(teamNames, METROPOLITAN_DIVISION_TEAMS);
    }

    /**
     * open web browser and scrape roster of the oldest team and verify there are more Canadian players than players from USA
     */
    @Test(dependsOnMethods = "VerifyOldestTeam")
    public void VerifyPlayerCountsViaBrowser() {
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        Page page = browser.newPage();

        final String oldestTeamUrl = teamList.stream()
                .filter(team -> team.name().equals(EXPECTED_OLDEST_TEAM))
                .toList().get(0).officialSiteUrl();

        Assert.assertEquals(oldestTeamUrl, EXPECTED_OLDEST_TEAM_WEBSITE_URL);

        final MCPage mcPage = new MCPage(page, oldestTeamUrl);

        mcPage.navigate();
        mcPage.changeLang(MCPage.Language.ENGLISH);
        mcPage.selectInNav("Team");
        mcPage.selectInMenu("Roster");

        final List<String> states = mcPage.scrapeRoster();

        final long canPlayers = states.stream().filter(state -> state.equals(CAN)).count();
        final long usaPlayers = states.stream().filter(state -> state.equals(USA)).count();

        log.log(Level.INFO, String.format(
                "Found %d players from CAN vs %d players from USA",
                canPlayers, usaPlayers)
        );

        Assert.assertTrue(canPlayers > usaPlayers);

        playwright.close();
    }

}
