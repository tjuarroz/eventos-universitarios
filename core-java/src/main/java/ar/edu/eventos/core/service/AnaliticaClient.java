package ar.edu.eventos.core.service;

import ar.edu.eventos.core.dto.AnaliticaDtos.EntrenarRequest;
import ar.edu.eventos.core.dto.AnaliticaDtos.PrediccionLoteRequest;
import ar.edu.eventos.core.dto.AnaliticaDtos.PrediccionRequest;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/** Cliente REST del microservicio Python (FastAPI) de analítica predictiva. */
@Component
public class AnaliticaClient {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaClient.class);

    private final RestClient http;
    private final JwtService jwt;

    public AnaliticaClient(@Value("${app.analitica.url}") String baseUrl, RestClient.Builder builder, JwtService jwt) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(60));
        // Se usa el builder de Spring Boot para heredar la configuración de Jackson (fechas ISO, etc.)
        this.http = builder.baseUrl(baseUrl).requestFactory(f).build();
        this.jwt = jwt;
    }

    public JsonNode entrenar(EntrenarRequest req) {
        return post("/api/v1/modelo/entrenar", req);
    }

    public JsonNode predecir(PrediccionRequest req) {
        return post("/api/v1/predicciones", req);
    }

    public JsonNode predecirLote(PrediccionLoteRequest req) {
        return post("/api/v1/predicciones/lote", req);
    }

    private JsonNode post(String uri, Object body) {
        try {
            return http.post().uri(uri)
                    .header("Authorization", "Bearer " + jwt.generarServicio())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.warn("Analítica respondió {} en {}: {}", e.getStatusCode(), uri, e.getResponseBodyAsString());
            String msg = e.getStatusCode().value() == 409
                    ? "El modelo predictivo todavía no fue entrenado"
                    : "El servicio de analítica rechazó la solicitud";
            throw new ApiException(HttpStatus.BAD_GATEWAY, msg);
        } catch (RestClientException e) {
            log.warn("No se pudo contactar al servicio de analítica ({}): {}", uri, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de analítica no está disponible");
        }
    }
}
