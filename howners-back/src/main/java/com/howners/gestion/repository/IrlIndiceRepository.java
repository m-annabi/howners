package com.howners.gestion.repository;

import com.howners.gestion.domain.rental.IrlIndice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IrlIndiceRepository extends JpaRepository<IrlIndice, UUID> {

    Optional<IrlIndice> findByAnneeAndTrimestre(Integer annee, Integer trimestre);

    List<IrlIndice> findAllByOrderByAnneeDescTrimestreDesc();

    Optional<IrlIndice> findTopByTrimestreOrderByAnneeDesc(Integer trimestre);
}
