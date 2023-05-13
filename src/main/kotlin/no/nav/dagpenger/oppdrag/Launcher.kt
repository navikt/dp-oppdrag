package no.nav.dagpenger.oppdrag

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.jms.annotation.EnableJms

@SpringBootApplication
@EnableJms
class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(no.nav.dagpenger.oppdrag.Launcher::class.java, *args)
}