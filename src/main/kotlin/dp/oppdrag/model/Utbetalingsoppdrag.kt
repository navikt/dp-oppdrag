package dp.oppdrag.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Utbetalingsoppdrag(
    val kodeEndring: KodeEndring,
    val fagSystem: String,
    val saksnummer: String,
    val aktoer: String,
    val saksbehandlerId: String,
    val avstemmingTidspunkt: LocalDateTime = LocalDateTime.now(),
    val utbetalingsperiode: List<Utbetalingsperiode>,
    val gOmregning: Boolean = false
) {
    enum class KodeEndring {
        NY,
        ENDR,
        UEND
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetalingsperiode(
    val erEndringPaaEksisterendePeriode: Boolean,
    val opphoer: Opphoer? = null,
    val periodeId: Long,
    val forrigePeriodeId: Long? = null,
    val datoForVedtak: LocalDate,
    val klassifisering: String,
    val vedtakdatoFom: LocalDate,
    val vedtakdatoTom: LocalDate,
    val sats: BigDecimal,
    val satsType: SatsType,
    val utbetalesTil: String,
    val behandlingId: Long,
    val utbetalingsgrad: Int? = null
) {
    enum class SatsType {
        DAG,
        MND,
        ENG
    }
}

data class Opphoer(
    val opphoerDatoFom: LocalDate
)

fun Utbetalingsoppdrag.behandlingsIdForFoersteUtbetalingsperiode(): String {

    return utbetalingsperiode[0].behandlingId.toString()
}

val Utbetalingsoppdrag.oppdragId
    get() = OppdragId(fagSystem, aktoer, behandlingsIdForFoersteUtbetalingsperiode())