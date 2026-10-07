package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.Sala;
import ar.edu.eventos.core.dto.SalaDto;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salas;

    public SalaService(SalaRepository salas) {
        this.salas = salas;
    }

    @Transactional(readOnly = true)
    public List<SalaDto> listar(boolean soloActivas) {
        var lista = soloActivas ? salas.findByActivaTrueOrderByCapacidadAsc() : salas.findAllByOrderByCapacidadAsc();
        return lista.stream().map(SalaDto::de).toList();
    }

    @Transactional
    public SalaDto crear(SalaDto dto) {
        if (salas.existsByNombreIgnoreCase(dto.nombre().trim())) {
            throw ApiException.conflicto("Ya existe una sala con ese nombre");
        }
        Sala s = new Sala();
        copiar(dto, s);
        return SalaDto.de(salas.save(s));
    }

    @Transactional
    public SalaDto actualizar(Integer id, SalaDto dto) {
        Sala s = salas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Sala"));
        copiar(dto, s);
        return SalaDto.de(s);
    }

    @Transactional
    public void eliminar(Integer id) {
        Sala s = salas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Sala"));
        salas.delete(s);
        salas.flush(); // fuerza la verificación de FK: si tiene eventos, se responde 409
    }

    private void copiar(SalaDto dto, Sala s) {
        s.setNombre(dto.nombre().trim());
        s.setEdificio(dto.edificio().trim());
        s.setFacultad(dto.facultad() == null || dto.facultad().isBlank() ? null : dto.facultad());
        s.setCapacidad(dto.capacidad());
        s.setEquipamiento(dto.equipamiento());
        s.setActiva(dto.activa() == null || dto.activa());
    }
}
