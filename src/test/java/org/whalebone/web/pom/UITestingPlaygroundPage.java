package org.whalebone.web.pom;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class UITestingPlaygroundPage {
    private static final String url = "http://uitestingplayground.com/";
    private final Locator menuLocator;

    private final Page page;

    @Getter
    @AllArgsConstructor
    public enum Apps {
        SAMPLE_APP("Sample App"),
        LOAD_DELAY("Load Delay"),
        PROGRESS_BAR("Progress Bar");

        private final String name;
    }

    public UITestingPlaygroundPage(Page page) {
        this.page = page;

        menuLocator = page.locator("#overview a");
    }

    public void Navigate() {
        page.navigate(url);
        menuLocator.first().waitFor();
    }

    public UITestingApp SelectInMenu(Apps app) {
        final Locator locator = menuLocator.filter(new Locator.FilterOptions().setHasText(app.getName()));
        locator.waitFor();
        locator.click();

        switch (app){
            case SAMPLE_APP -> {
                return new SampleApp();
            }
            case LOAD_DELAY -> {
                return new LoadDelay();
            }
            case PROGRESS_BAR -> {
                return new ProgressBar();
            }
        }
        throw new IllegalArgumentException(String.format("Unknown app %s", app.getName()));
    }

    private interface UITestingApp {
        void AwaitLoaded(double timeout);
    }

    public class SampleApp implements UITestingApp {
        private final Locator sampleAppStatusSelector;
        private final Locator userInput;
        private final Locator passwordInput;
        private final Locator loginButton;

        private SampleApp() {
            sampleAppStatusSelector = page.locator("#loginstatus");
            userInput = page.locator("input[name='UserName']");
            passwordInput = page.locator("input[name='Password']");
            loginButton = page.locator("button#login");
        }

        @Getter
        @AllArgsConstructor
        public enum LoginState {
            LOGGED_OUT("User logged out."),
            INVALID_CREDENTIALS("Invalid username/password"),
            LOGGED_IN("Welcome, %s!");

            private final String state;
        }

        @Override
        public void AwaitLoaded(double timeout) {
            loginButton.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        }

        public void FillCredentials(String name, String pass) {
            userInput.fill(name);
            passwordInput.fill(pass);
        }

        public String GetLoginStatus() {
            return sampleAppStatusSelector.innerText();
        }

        public void Login() {
            loginButton.click();
        }
    }

    public class LoadDelay implements UITestingApp {

        private final Locator buttonAfterLoaded;

        private LoadDelay() {
            buttonAfterLoaded = page.locator("button")
                    .filter(new Locator.FilterOptions().setHasText("Button Appearing After Delay"));
        }

        @Override
        public void AwaitLoaded(double timeout) {
            buttonAfterLoaded.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(timeout)
                            .setState(WaitForSelectorState.VISIBLE)
            );
        }

        public boolean IsButtonLoaded() {
            return buttonAfterLoaded.isVisible();
        }
    }

    public class ProgressBar implements UITestingApp {
        private final Locator startButton;
        private final Locator stopButton;
        private final Locator progress;
        private final Locator result;

        private ProgressBar() {
            startButton = page.locator("button#startButton");
            stopButton = page.locator("button#stopButton");
            progress = page.locator("#progressBar");
            result = page.locator("#result");
        }

        @Override
        public void AwaitLoaded(double timeout) {
            startButton.waitFor();
            stopButton.waitFor();
            progress.waitFor();
        }

        public void Start() {
            startButton.click();
        }

        public void Stop() {
            stopButton.click();
        }

        public int GetProgress() {
            return Integer.parseInt(progress.getAttribute("aria-valuenow"));
        }

        public String GetResult() {
            return result.innerText();
        }

        public void AwaitProgress(int expectedProgress, long timeoutSeconds, double polling) {
            Instant start = Instant.now();
            while (Duration.between(start, Instant.now()).getSeconds() < timeoutSeconds) {
                if (GetProgress() >= expectedProgress) return;
                page.waitForTimeout(polling);
            }
        }
    }
}
