package org.whalebone.teams.pom;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class MCPage {
    private static final String SPAN = "span";
    private static final int TIMEOUT = 2000;

    private final Page page;
    private final String url;

    private final Locator openLangDropdownButton;
    private final Locator langDropdownNav;
    private final Locator teamMenuDropdownButton;
    private final Locator teamMenuDropdownNav;
    private final Locator rosterBirthplaceState;

    @Getter
    @AllArgsConstructor
    public enum Language {
        ENGLISH("English"),
        FRENCH("Français");

        private final String value;
    }

    public MCPage(Page page, String url) {
        this.page = page;
        this.url = url;

        openLangDropdownButton = page.locator("#hamburger-menu button[aria-controls='language-switch']");
        langDropdownNav = page.locator("nav#language-switch");
        teamMenuDropdownButton = page.locator("#hamburger-menu li > button");
        teamMenuDropdownNav = page.locator("nav[aria-label='Team Menu']");
        rosterBirthplaceState = page.locator("table > tbody > tr > td > div > span:last-child");
    }

    public void navigate() {
        page.navigate(url);
    }

    public void changeLang(Language lang) {
        openLangDropdownButton.click();
        final Locator locator = langDropdownNav.locator(SPAN)
                .filter(new Locator.FilterOptions().setHasText(lang.getValue()));
        locator.waitFor();
        locator.click();
    }

    public void selectInNav(String name) {
        final Locator locator = teamMenuDropdownButton.filter(new Locator.FilterOptions().setHasText(name));
        locator.waitFor();
        locator.click(new Locator.ClickOptions().setTimeout(TIMEOUT));
    }

    public void selectInMenu(String name) {
        final Locator locator = teamMenuDropdownNav.locator(SPAN)
                .filter(new Locator.FilterOptions().setHasText(name));
        locator.waitFor();
        locator.click();
    }

    public List<String> scrapeRoster() {
        rosterBirthplaceState.first().waitFor();
        return rosterBirthplaceState.allInnerTexts();
    }

}
