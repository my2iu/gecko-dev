/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.push.autopush.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.background.testhelpers.TestRunner;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.push.RegisterUserAgentResponse;
import org.mozilla.gecko.push.SubscribeChannelResponse;
import org.mozilla.gecko.push.autopush.AutopushClient;
import org.mozilla.gecko.push.autopush.AutopushClient.RequestDelegate;
import org.mozilla.gecko.push.autopush.AutopushClientException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.BaseResource;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * This test straddles an awkward line: it uses Mockito, but doesn't actually mock the service
 * endpoint.  That's why it's a <b>live</b> test: most of its value is checking that the client
 * implementation and the upstream server implementation are corresponding correctly.
 */
@RunWith(TestRunner.class)
@Ignore("Live test that requires network connection -- remove this line to run this test.")
public class TestLiveAutopushClient {
    final String serverURL = "https://updates-autopush-dev.stage.mozaws.net/v1/gcm/829133274407";

    protected AutopushClient client;

    @Before
    public void setUp() throws Exception {
        BaseResource.rewriteLocalhost = false;
        client = new AutopushClient(serverURL, WaitHelper.newSynchronousExecutor());
    }

    protected <T> T assertSuccess(RequestDelegate<T> delegate, Class<T> klass) {
        verify(delegate, never()).handleError(any(Exception.class));
        verify(delegate, never()).handleFailure(any(AutopushClientException.class));

        final ArgumentCaptor<T> register = ArgumentCaptor.forClass(klass);
        verify(delegate).handleSuccess(register.capture());

        return register.getValue();
    }

    protected <T> AutopushClientException assertFailure(RequestDelegate<T> delegate, Class<T> klass) {
        verify(delegate, never()).handleError(any(Exception.class));
        verify(delegate, never()).handleSuccess(any(klass));

        final ArgumentCaptor<AutopushClientException> failure = ArgumentCaptor.forClass(AutopushClientException.class);
        verify(delegate).handleFailure(failure.capture());

        return failure.getValue();
    }

    @Test
    public void testUserAgent() throws Exception {
        final RequestDelegate<RegisterUserAgentResponse> registerDelegate = mock(RequestDelegate.class);
        client.registerUserAgent(Utils.generateGuid(), registerDelegate);

        final RegisterUserAgentResponse registerResponse = assertSuccess(registerDelegate, RegisterUserAgentResponse.class);
        Assert.assertNotNull(registerResponse);
        Assert.assertNotNull(registerResponse.uaid);
        Assert.assertNotNull(registerResponse.secret);

        // Reregistering with a new GUID should succeed.
        final RequestDelegate<Void> reregisterDelegate = mock(RequestDelegate.class);
        client.reregisterUserAgent(registerResponse.uaid, registerResponse.secret, Utils.generateGuid(), reregisterDelegate);

        Assert.assertNull(assertSuccess(reregisterDelegate, Void.class));

        // Unregistering should succeed.
        final RequestDelegate<Void> unregisterDelegate = mock(RequestDelegate.class);
        client.unregisterUserAgent(registerResponse.uaid, registerResponse.secret, unregisterDelegate);

        Assert.assertNull(assertSuccess(unregisterDelegate, Void.class));

        // Trying to unregister a second time should give a 404.
        final RequestDelegate<Void> reunregisterDelegate = mock(RequestDelegate.class);
        client.unregisterUserAgent(registerResponse.uaid, registerResponse.secret, reunregisterDelegate);

        final AutopushClientException failureException = assertFailure(reunregisterDelegate, Void.class);
        Assert.assertThat(failureException, instanceOf(AutopushClientException.AutopushClientRemoteException.class));
        Assert.assertTrue(((AutopushClientException.AutopushClientRemoteException) failureException).isNotFound());
    }

    @Test
    public void testChannel() throws Exception {
        final RequestDelegate<RegisterUserAgentResponse> registerDelegate = mock(RequestDelegate.class);
        client.registerUserAgent(Utils.generateGuid(), registerDelegate);

        final RegisterUserAgentResponse registerResponse = assertSuccess(registerDelegate, RegisterUserAgentResponse.class);
        Assert.assertNotNull(registerResponse);
        Assert.assertNotNull(registerResponse.uaid);
        Assert.assertNotNull(registerResponse.secret);

        // We should be able to subscribe to a channel.
        final RequestDelegate<SubscribeChannelResponse> subscribeDelegate = mock(RequestDelegate.class);
        client.subscribeChannel(registerResponse.uaid, registerResponse.secret, subscribeDelegate);

        final SubscribeChannelResponse subscribeResponse = assertSuccess(subscribeDelegate, SubscribeChannelResponse.class);
        Assert.assertNotNull(subscribeResponse);
        Assert.assertNotNull(subscribeResponse.channelID);
        Assert.assertNotNull(subscribeResponse.endpoint);
        Assert.assertThat(subscribeResponse.endpoint, startsWith(FxAccountUtils.getAudienceForURL(serverURL)));

        // And we should be able to unsubscribe.
        final RequestDelegate<Void> unsubscribeDelegate = mock(RequestDelegate.class);
        client.unsubscribeChannel(registerResponse.uaid, registerResponse.secret, subscribeResponse.channelID, unsubscribeDelegate);

        Assert.assertNull(assertSuccess(unsubscribeDelegate, Void.class));

        // Trying to unsubscribe a second time should give a 404.
        final RequestDelegate<Void> reunsubscribeDelegate = mock(RequestDelegate.class);
        client.unsubscribeChannel(registerResponse.uaid, registerResponse.secret, subscribeResponse.channelID, reunsubscribeDelegate);

        final AutopushClientException reunsubscribeFailureException = assertFailure(reunsubscribeDelegate, Void.class);
        Assert.assertThat(reunsubscribeFailureException, instanceOf(AutopushClientException.AutopushClientRemoteException.class));
        Assert.assertTrue(((AutopushClientException.AutopushClientRemoteException) reunsubscribeFailureException).isNotFound());

        // Trying to unsubscribe from a non-existent channel should give a 404.  Right now it gives a 401!
        final RequestDelegate<Void> badUnsubscribeDelegate = mock(RequestDelegate.class);
        client.unsubscribeChannel(registerResponse.uaid + "BAD", registerResponse.secret, subscribeResponse.channelID, badUnsubscribeDelegate);

        final AutopushClientException badUnsubscribeFailureException = assertFailure(badUnsubscribeDelegate, Void.class);
        Assert.assertThat(badUnsubscribeFailureException, instanceOf(AutopushClientException.AutopushClientRemoteException.class));
        Assert.assertTrue(((AutopushClientException.AutopushClientRemoteException) badUnsubscribeFailureException).isInvalidAuthentication());
    }
}
