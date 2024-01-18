package no.nav.dagpenger.oppdrag.grensesnittavstemming

import com.ibm.mq.jakarta.jms.MQConnectionFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import io.mockk.spyk
import io.mockk.verify
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestOppdragMedAvstemmingsdato
import no.nav.dagpenger.oppdrag.util.somOppdragLager
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.junit.jupiter.api.Test
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

private const val TESTKØ = "DEV.QUEUE.2"
private val FAGSYSTEM = Fagsystem.Dagpenger
private val IDAG = LocalDateTime.now()

@Testcontainers
@ContextConfiguration(initializers = [Containers.MQInitializer::class])
class GrensesnittavstemmingSenderTest {
    companion object {
        @Container
        var ibmMQContainer = Containers.ibmMQContainer
    }

    private val mqConn =
        MQConnectionFactory().apply {
            hostName = "localhost"
            port = ibmMQContainer.getMappedPort(1414)
            channel = "DEV.ADMIN.SVRCONN"
            queueManager = "QM1"
            transportType = WMQConstants.WMQ_CM_CLIENT
        }

    private val cf =
        UserCredentialsConnectionFactoryAdapter().apply {
            setUsername("admin")
            setPassword("passw0rd")
            setTargetConnectionFactory(mqConn)
        }

    private val jmsTemplate = spyk(JmsTemplate(cf).apply { defaultDestinationName = TESTKØ })

    @Test
    fun skal_sende_grensesnittavstemming_når_påskrudd() {
        val avstemmingSender = GrensesnittavstemmingSender(jmsTemplate, "true")

        avstemmingSender.sendGrensesnittAvstemming(lagTestGrensesnittavstemming()[0])

        verify(exactly = 1) { jmsTemplate.convertAndSend(any<String>(), any<String>()) }
    }

    private fun lagTestGrensesnittavstemming(): List<Avstemmingsdata> {
        val utbetalingsoppdrag = TestOppdragMedAvstemmingsdato.lagTestUtbetalingsoppdrag(IDAG, FAGSYSTEM)
        val mapper =
            GrensesnittavstemmingMapper(listOf(utbetalingsoppdrag.somOppdragLager), FAGSYSTEM, IDAG.minusDays(1), IDAG)
        return mapper.lagAvstemmingsmeldinger()
    }
}