package ru.pancomanco.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import ru.pancomanco.common.i18n.CommonI18nAutoConfiguration;
import ru.pancomanco.common.i18n.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

class CommonI18nAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    CommonI18nAutoConfiguration.class
                            )
                    )
                    .withBean(
                            MessageSource.class,
                            StaticMessageSource::new
                    );

    @Test
    void shouldCreateMessageService() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(MessageService.class);
        });
    }

    @Test
    void shouldNotReplaceCustomMessageService() {
        contextRunner
                .withBean(
                        MessageService.class,
                        () -> new MessageService(
                                new StaticMessageSource()
                        )
                )
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(MessageService.class);
                });
    }
}