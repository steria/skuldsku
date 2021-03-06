package no.steria.skuldsku.testrunner.interfacerunner;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;

import no.steria.skuldsku.common.result.Results;
import no.steria.skuldsku.recorder.java.JavaCall;
import no.steria.skuldsku.recorder.java.serializer.ClassSerializer;
import no.steria.skuldsku.testrunner.interfacerunner.result.JavaCallMissingFromActualResult;
import no.steria.skuldsku.testrunner.interfacerunner.result.JavaCallNotEqualsResult;
import no.steria.skuldsku.testrunner.interfacerunner.verifiers.StrictJavaCallVerifier;

import org.junit.Test;

public class InterfaceRecordingCheckerTest {

    private final JavaCallVerifier verifier = new StrictJavaCallVerifier();

    private static class Simple {
        private String value;
    }

    @SuppressWarnings("unused")
    private static class Service {
        public Simple doIt(String val) {
            Simple simple = new Simple();
            simple.value = val;
            return simple;
        }

        public String reverseIt(Simple simple) {
            return simple.value;
        }
    }

    @Test
    public void shouldFindEquals()throws Exception {
        Simple simple = new Simple();
        simple.value = "result";
        ClassSerializer classSerializer = new ClassSerializer();
        String para = classSerializer.asString("para");
        JavaCall a = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);
        JavaCall b = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);

        final Results verifierResult = verifier.assertEquals(Arrays.asList(a), Arrays.asList(b), new JavaCallVerifierOptions());
        assertThat(verifierResult.hasErrors()).isEqualTo(false);
    }

    @Test
    public void shouldFindNotEquals()throws Exception {
        Simple simple = new Simple();
        simple.value = "result";
        ClassSerializer classSerializer = new ClassSerializer();
        JavaCall a = new JavaCall("", "Service", "doIt", classSerializer.asString("parax"), classSerializer.asString(simple), null, 0, 0);
        JavaCall b = new JavaCall("", "Service", "doIt", classSerializer.asString("para"), classSerializer.asString(simple), null, 0, 0);

        final Results verifierResult = verifier.assertEquals(Arrays.asList(a), Arrays.asList(b), new JavaCallVerifierOptions());

        assertThat(verifierResult.hasErrors()).isEqualTo(true);
    }



    @Test
    public void shouldRegisterMissingCall() throws Exception {
        Simple simple = new Simple();
        simple.value = "result";
        ClassSerializer classSerializer = new ClassSerializer();
        String para = classSerializer.asString("para");
        JavaCall a = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);
        JavaCall b = new JavaCall("", "Service", "reverseIt", classSerializer.asString(simple),para, null, 0, 0);
        JavaCall c = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);

        JavaCall d = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);
        JavaCall e = new JavaCall("", "Service", "doIt", para, classSerializer.asString(simple), null, 0, 0);

        final Results verifierResult = verifier.assertEquals(Arrays.asList(a, b, c), Arrays.asList(d, e), new JavaCallVerifierOptions());

        assertThat(verifierResult.getByType(JavaCallNotEqualsResult.class)).isEmpty();
        assertThat(verifierResult.getByType(JavaCallMissingFromActualResult.class)).hasSize(1);

    }
}
