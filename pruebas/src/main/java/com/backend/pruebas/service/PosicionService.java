package com.backend.pruebas.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.pruebas.exceptions.BadRequestException;
import com.backend.pruebas.model.dto.ConfiguracionDTO;
import com.backend.pruebas.model.dto.PosicionDTO;
import com.backend.pruebas.model.entity.Posicion;
import com.backend.pruebas.model.entity.Prueba;
import com.backend.pruebas.model.entity.Vehiculo;
import com.backend.pruebas.repository.InteresadoRepository;
import com.backend.pruebas.repository.PosicionRepository;
import com.backend.pruebas.repository.PruebaRepository;
import com.backend.pruebas.repository.VehiculoRepository;
import com.backend.pruebas.utils.MathUtils;


@Service
public class PosicionService {

  private final PosicionRepository posicionRepository;
  private final VehiculoRepository vehiculoRepository;
  private final InteresadoRepository interesadoRepository;
  private final PruebaRepository pruebaRepository;

  @Autowired
  public PosicionService(
      PosicionRepository posicionRepository,
      VehiculoRepository vehiculoRepository,
      InteresadoRepository interesadoRepository,
      PruebaRepository pruebaRepository
  ) {
    this.posicionRepository = posicionRepository;
    this.vehiculoRepository = vehiculoRepository;
    this.interesadoRepository = interesadoRepository;
    this.pruebaRepository = pruebaRepository;
  }

  @Transactional
  public Posicion savePosicion(PosicionDTO posicionDto, ConfiguracionDTO configuracionDto) {
      vehiculoRepository.findById(posicionDto.getIdVehiculo()).orElseThrow(() -> new BadRequestException("El vehiculo no existe"));
      Posicion posicion = posicionRepository.save(posicionDto.toEntity());
      if (!validatePosicion(posicionDto, configuracionDto)) {
        // TODO: NOTIFICAR AL EMPLEADO
        Optional<Prueba> prueba = pruebaRepository.findFirstByVehiculoIdOrderByFechaHoraInicioDesc(posicionDto.getIdVehiculo());
        if (prueba.isPresent() && prueba.get().getFechaHoraFin() == null) {
          restrigirInteresado(prueba.get().getIdInteresado());
        }
      }
      return posicion;
  }


  private void restrigirInteresado(Long idInteresado) {
    interesadoRepository.findById(idInteresado).ifPresent(interesado -> {
      if (interesado.isValid()) {
        interesado.setRestringido(true);
        interesadoRepository.save(interesado);
      }
    });
  }

  private boolean validatePosicion(PosicionDTO posicion, ConfiguracionDTO configuracion) {
    double radioAdmitidoKm = configuracion.getRadioAdmitidoKm();
    double latitud = posicion.getLatitud();
    double longitud = posicion.getLongitud();
    double latitudAgencia = configuracion.getCoordenadasAgencia().getLat();
    double longitudAgencia = configuracion.getCoordenadasAgencia().getLon();
  
    if (MathUtils.distance(latitud, longitud, latitudAgencia, longitudAgencia) > radioAdmitidoKm) {
      // throw new BadRequestException("La posición está fuera del rango permitido");
      return false;
    }
    for (ConfiguracionDTO.Zona zona : configuracion.getZonasRestringidas()) {
      if (MathUtils.isInsideZone(latitud, longitud, zona.getNoroeste(), zona.getSureste())) {
        // throw new BadRequestException("La posición está dentro de una zona restringida");
        return false;
      }
    }

    return true;
  }
}
