package com.techup.travel_explorer_server.repository;

import com.techup.travel_explorer_server.entity.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    @EntityGraph(attributePaths = {"author"})
    @Override
    List<Trip> findAll();
}

