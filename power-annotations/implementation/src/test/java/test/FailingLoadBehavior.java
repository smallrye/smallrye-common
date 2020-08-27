package test;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.io.FileInputStream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.impl.PowerAnnotationsLoader;
import io.smallrye.common.powerannotations.index.Index;

class FailingLoadBehavior {
    @Test
    void shouldSilentlySkipUnknownIndexResource() {
        PowerAnnotationsLoader loader = new PowerAnnotationsLoader();

        then(loader).isNotNull();
    }

    @Test
    void shouldFailToLoadInvalidIndexInputStream() throws Exception {
        FileInputStream inputStream = new FileInputStream("pom.xml");

        Throwable throwable = catchThrowable(() -> Index.from(inputStream));

        then(throwable)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Not a jandex index");
    }
}
