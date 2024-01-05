package no.nav.dagpenger.oppdrag.service

import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.domene.id
import no.nav.dagpenger.oppdrag.iverksetting.OppdragSender
import no.nav.dagpenger.oppdrag.repository.OppdragLager
import no.nav.dagpenger.oppdrag.repository.OppdragLagerRepository
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("!e2e")
class OppdragServiceImpl(
    @Autowired private val oppdragSender: OppdragSender,
    @Autowired private val oppdragLagerRepository: OppdragLagerRepository
) : OppdragService {

    @Transactional(rollbackFor = [Throwable::class])
    override fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int) {

        LOG.debug("Lagrer oppdrag i databasen " + oppdrag.id)
        try {
            oppdragLagerRepository.opprettOppdrag(OppdragLager.lagFraOppdrag(utbetalingsoppdrag, oppdrag), versjon)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            LOG.info("Oppdrag ${oppdrag.id} er allerede sendt.")
            throw OppdragAlleredeSendtException()
        }

        LOG.debug("Legger oppdrag på kø " + oppdrag.id)
        oppdragSender.sendOppdrag(oppdrag)
    }

    override fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager {
        return oppdragLagerRepository.hentOppdrag(oppdragId)
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OppdragServiceImpl::class.java)
    }
}

class OppdragAlleredeSendtException : RuntimeException()