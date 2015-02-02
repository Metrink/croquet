package com.metrink.croquet.examples.crm.pages;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import com.metrink.croquet.CroquetTester;
import com.metrink.croquet.examples.crm.CrmModule;
import com.metrink.croquet.examples.crm.CrmSettings;
import com.metrink.croquet.examples.crm.EmailModule;
import com.metrink.croquet.examples.crm.Main;

public class CompanyPageTest {
    //private static final Logger LOG = LoggerFactory.getLogger(CompanyPageTest.class);

    private WicketTester tester;

    @Before
    public void setUp() throws Exception {
        // create the croquet tester object through the builder
        final CroquetTester<CrmSettings> croquetTester =
                Main.configureBuilder(CrmSettings.class, new String[] { "application.yml" }).buildTester();

        // get the custom settings for the application
        // if custom settings aren't needed for Guice modules, then you
        // don't need this method as settings are bound by Croquet
        final CrmSettings settings = croquetTester.getSettings();

        // add in our Guice module
        croquetTester.addGuiceModule(new CrmModule(settings));

        // add in a managed module
        croquetTester.addManagedModule(EmailModule.class);

        // get the tester
        tester = croquetTester.getTester();
    }

    @Test
    public void test() {
        tester.startPage(CompanyPage.class);

        tester.assertRenderedPage(CompanyPage.class);
    }
}
