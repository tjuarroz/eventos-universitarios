package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final PrediccionService prediccionService;
    private final EventoRepository eventos;
    private final boolean entrenarAlIniciar;

    public TareasProgramadas(PrediccionService prediccionService, EventoRepository eventos,
                             @Value("${app.analitica.entrenar-al-iniciar}") boolean entrenarAlIniciar) {
        this.prediccionService = prediccionService;
        this.eventos = eventos;
        this.entrenarAlIniciar = entrenarAlIniciar;
    }

    /** Al arrancar, entrena el modelo con el histórico (reintenta mientras el servicio Python levanta). */
    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        if (!entrenarAlIniciar) return;
        CompletableFuture.runAsync(() -> {
            for (int intento = 1; intento <= 20; intento++) {
                try {
                    var r = prediccionService.entrenar();
                    log.info("Modelo predictivo entrenado al iniciar: {}", r.path("metricas"));
                    return;
                } catch (Exception e) {
                    log.info("Entrenamiento inicial pendiente (intento {}): {}", intento, e.getMessage());
                    dormir(6000);
                }
            }
            log.warn("No se pudo entrenar el modelo al iniciar; se puede reentrenar desde el dashboard");
        });
    }

    /** Todas las noches: cierra eventos pasados y reentrena con los datos nuevos. */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Argentina/Buenos_Aires")
    public void nocturna() {
        finalizarVencidos();
        try {
            prediccionService.entrenar();
        } catch (Exception e) {
            log.warn("Reentrenamiento nocturno falló: {}", e.getMessage());
        }
    }

    @Transactional
    public void finalizarVencidos() {
        var vencidos = eventos.findByEstadoAndFechaBefore(EstadoEvento.PUBLICADO, LocalDate.now());
        vencidos.forEach(e -> e.setEstado(EstadoEvento.FINALIZADO));
        eventos.saveAll(vencidos);
        if (!vencidos.isEmpty()) log.info("{} eventos pasados marcados como FINALIZADOS", vencidos.size());
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
