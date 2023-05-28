package no.nav.dagpenger.oppdrag.rest

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.Opphør
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.dagpenger.oppdrag.common.Ressurs
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.OppdragMapper
import no.nav.dagpenger.oppdrag.iverksetting.OppdragSender
import no.nav.dagpenger.oppdrag.repository.OppdragLager
import no.nav.dagpenger.oppdrag.repository.OppdragLagerRepository
import no.nav.dagpenger.oppdrag.service.OppdragServiceImpl
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

internal class OppdragControllerTest {

    val localDateTimeNow = LocalDateTime.now()
    val localDateNow = LocalDate.now()

    val utbetalingsoppdrag = Utbetalingsoppdrag(
        Utbetalingsoppdrag.KodeEndring.NY,
        Fagsystem.Dagpenger,
        UUID.randomUUID(),
        "PERSONID",
        "SAKSBEHANDLERID",
        localDateTimeNow,
        listOf(
            Utbetalingsperiode(
                true,
                Opphør(localDateNow),
                2,
                1,
                localDateNow,
                "BATR",
                localDateNow,
                localDateNow,
                BigDecimal.ONE,
                Utbetalingsperiode.SatsType.MND,
                "UTEBETALES_TIL",
                UUID.randomUUID()
            )
        )
    )

    @Test
    fun `Skal lagre oppdrag for utbetalingoppdrag`() {

        val (oppdragLagerRepository, oppdragController) = mockkOppdragController(false)

        oppdragController.sendOppdrag(utbetalingsoppdrag)

        verify {
            oppdragLagerRepository.opprettOppdrag(
                match<OppdragLager> {
                    it.utgåendeOppdrag.contains("BA") &&
                        it.status == OppdragStatus.LAGT_PAA_KOE &&
                        it.opprettetTidspunkt > localDateTimeNow
                }
            )
        }
    }

    @Test
    fun `Skal kaste feil om oppdrag er lagret fra før`() {
        val (oppdragLagerRepository, oppdragController) = mockkOppdragController(true)

        val response = oppdragController.sendOppdrag(utbetalingsoppdrag)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(Ressurs.Status.FEILET, response.body?.status)

        verify(exactly = 1) { oppdragLagerRepository.opprettOppdrag(any()) }
    }

    private fun mockkOppdragController(alleredeOpprettet: Boolean = false): Pair<OppdragLagerRepository, OppdragController> {
        val mapper = OppdragMapper()
        val oppdragSender = mockk<OppdragSender>(relaxed = true)

        val oppdragLagerRepository = mockk<OppdragLagerRepository>()
        if (alleredeOpprettet) {
            every { oppdragLagerRepository.opprettOppdrag(any()) } throws org.springframework.dao.DuplicateKeyException("Duplicate key exception")
        } else {
            every { oppdragLagerRepository.opprettOppdrag(any()) } just Runs
        }

        val oppdragService = OppdragServiceImpl(oppdragSender, oppdragLagerRepository)

        val oppdragController = OppdragController(oppdragService, mapper)
        return Pair(oppdragLagerRepository, oppdragController)
    }
}
