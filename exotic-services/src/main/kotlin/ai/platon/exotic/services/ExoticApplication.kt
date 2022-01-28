package ai.platon.exotic.services

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity"
)
class ExoticApplication(
    val applicationContext: ApplicationContext,
    val env: Environment
) {
    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }

    @Bean
    fun exoticCrawler(): ExoticCrawler {
        return ExoticCrawler(env)
    }
}

fun main(args: Array<String>) {
    runApplication<ExoticApplication>(*args)
}