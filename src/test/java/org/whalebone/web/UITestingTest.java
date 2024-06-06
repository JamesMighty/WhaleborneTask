package org.whalebone.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.java.Log;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.whalebone.web.pom.UITestingPlaygroundPage;

import java.util.logging.Level;

import static org.whalebone.web.pom.UITestingPlaygroundPage.Apps.*;
import static org.whalebone.web.pom.UITestingPlaygroundPage.SampleApp.LoginState.*;

@Log
public class UITestingTest {
    private static final String WRONG_USR = "";
    private static final String CORRECT_USR = "abcde";
    private static final String WRONG_PWD = "123";
    private static final String CORRECT_PWD = "pwd";

    private static final double LOAD_TIMEOUT = 10 * 1000;

    private static final String PROGRESS_BAR_EXPECTED_VALUE = "75%";
    public static final int PROGRESS_BAR_TIMEOUT = 60 * 60 * 1000;
    public static final int POLLING = 100;

    private UITestingPlaygroundPage playgroundPage;

    private Playwright playwright;

    @BeforeClass
    public void Setup() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            this.playwright = playwright;
            final Page page = browser.newPage();
            playgroundPage = new UITestingPlaygroundPage(page);
        }
    }

    @AfterClass
    public void CleanUpClass(){
        playwright.close();
    }

    @BeforeMethod
    public void CleanUp() {
        playgroundPage.Navigate();
    }

    @Test
    public void VerifySampleApp() {
        final UITestingPlaygroundPage.SampleApp sampleApp =
                (UITestingPlaygroundPage.SampleApp) playgroundPage.SelectInMenu(SAMPLE_APP);

        sampleApp.AwaitLoaded(LOAD_TIMEOUT);

        Assert.assertEquals(sampleApp.GetLoginStatus(),
                LOGGED_OUT.getState());

        sampleApp.FillCredentials(WRONG_USR, WRONG_PWD);
        sampleApp.Login();

        Assert.assertEquals(sampleApp.GetLoginStatus(),
                INVALID_CREDENTIALS.getState());

        sampleApp.FillCredentials(CORRECT_USR, WRONG_PWD);
        sampleApp.Login();

        Assert.assertEquals(sampleApp.GetLoginStatus(),
                INVALID_CREDENTIALS.getState());

        sampleApp.FillCredentials(WRONG_USR, CORRECT_PWD);
        sampleApp.Login();

        Assert.assertEquals(sampleApp.GetLoginStatus(),
                INVALID_CREDENTIALS.getState());

        sampleApp.FillCredentials(CORRECT_USR, CORRECT_PWD);
        sampleApp.Login();

        Assert.assertEquals(sampleApp.GetLoginStatus(),
                String.format(LOGGED_IN.getState(), CORRECT_USR)
        );
    }

    @Test
    public void VerifyLoadDelayApp() {
        final UITestingPlaygroundPage.LoadDelay loadDelayApp =
                (UITestingPlaygroundPage.LoadDelay) playgroundPage.SelectInMenu(LOAD_DELAY);

        loadDelayApp.AwaitLoaded(LOAD_TIMEOUT);

        Assert.assertTrue(loadDelayApp.IsButtonLoaded());
    }

    @Test void VerifyProgressBarApp() {
        final UITestingPlaygroundPage.ProgressBar progressBarApp =
                (UITestingPlaygroundPage.ProgressBar) playgroundPage.SelectInMenu(PROGRESS_BAR);

        progressBarApp.AwaitLoaded(LOAD_TIMEOUT);

        progressBarApp.Start();

        progressBarApp.AwaitProgress(PROGRESS_BAR_EXPECTED_VALUE, PROGRESS_BAR_TIMEOUT, POLLING);
        progressBarApp.Stop();

        log.log(Level.INFO, progressBarApp.GetResult());

        // assert?
    }



}
