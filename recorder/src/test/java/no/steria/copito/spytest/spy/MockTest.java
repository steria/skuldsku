package no.steria.copito.spytest.spy;

import org.junit.After;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MockTest {
    private final DummyReportCallback reportCallback = new DummyReportCallback();



    @Test
    public void shouldUseMock() throws Exception {
        System.setProperty("no.steria.copito.doMock","true");
        MockRegistration.registerMock(ServiceInterface.class,MockFromServiceImpl.create(ServiceInterface.class,new ServiceMock()));
        ServiceInterface serviceClass = SpyWrapper.newInstance(new ServiceClass(), ServiceInterface.class, reportCallback, SpyConfig.factory().withAsyncMode(AsyncMode.ALL_SYNC).create());

        assertThat(serviceClass.doSimpleService("hoi")).isEqualTo("I am the mock hoi");

    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("no.steria.copito.doMock","false");


    }
}