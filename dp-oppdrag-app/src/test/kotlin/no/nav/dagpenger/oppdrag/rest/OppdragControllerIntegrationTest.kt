package no.nav.dagpenger.oppdrag.rest

import no.nav.dagpenger.oppdrag.common.Ressurs
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.domene.oppdragId
import no.nav.dagpenger.oppdrag.iverksetting.OppdragMapper
import no.nav.dagpenger.oppdrag.repository.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.service.OppdragService
import no.nav.dagpenger.oppdrag.util.Containers
import no.nav.dagpenger.oppdrag.util.TestConfig
import no.nav.dagpenger.oppdrag.util.TestUtbetalingsoppdrag.utbetalingsoppdragMedTilfeldigAktoer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jms.annotation.EnableJms
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@ActiveProfiles("local")
@ContextConfiguration(initializers = [Containers.PostgresSQLInitializer::class, Containers.MQInitializer::class])
@SpringBootTest(classes = [TestConfig::class])
@EnableJms
@Testcontainers
internal class OppdragControllerIntegrationTest {

    @Autowired lateinit var oppdragService: OppdragService
    @Autowired lateinit var oppdragLagerRepository: OppdragLagerRepository

    companion object {
        @Container var postgreSQLContainer = Containers.postgreSQLContainer
        @Container var ibmMQContainer = Containers.ibmMQContainer
    }

    @Test
    fun `Test skal lagre oppdrag for utbetalingoppdrag`() {

        val mapper = OppdragMapper()
        val oppdragController = OppdragController(oppdragService, mapper)

        val utbetalingsoppdrag = utbetalingsoppdragMedTilfeldigAktoer()
        oppdragController.sendOppdrag(utbetalingsoppdrag)

        var oppdragStatus: OppdragStatus

        do {
            val oppdrag = oppdragLagerRepository.hentOppdrag(utbetalingsoppdrag.oppdragId)

            oppdragStatus = oppdrag.status
        } while (oppdragStatus == OppdragStatus.LAGT_PAA_KOE)

        assertEquals(OppdragStatus.KVITTERT_OK, oppdragStatus)
    }

    @Test
    fun `Test skal returnere https statuscode 409 ved dobbel sending`() {

        val mapper = OppdragMapper()
        val oppdragController = OppdragController(oppdragService, mapper)

        val utbetalingsoppdrag = utbetalingsoppdragMedTilfeldigAktoer()

        val responseFørsteSending = oppdragController.sendOppdrag(utbetalingsoppdrag)
        assertEquals(HttpStatus.OK, responseFørsteSending.statusCode)
        assertEquals(Ressurs.Status.SUKSESS, responseFørsteSending.body?.status)

        val responseAndreSending = oppdragController.sendOppdrag(utbetalingsoppdrag)

        assertEquals(HttpStatus.CONFLICT, responseAndreSending.statusCode)
        assertEquals(Ressurs.Status.FEILET, responseAndreSending.body?.status)

        var oppdragStatus: OppdragStatus

        do {
            val oppdrag = oppdragLagerRepository.hentOppdrag(utbetalingsoppdrag.oppdragId)

            oppdragStatus = oppdrag.status
        } while (oppdragStatus == OppdragStatus.LAGT_PAA_KOE)

        assertEquals(OppdragStatus.KVITTERT_OK, oppdragStatus)
    }
}