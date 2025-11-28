package com.techup.travel_explorer_server.repository;

import com.techup.travel_explorer_server.entity.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    @EntityGraph(attributePaths = {"author"})
    @Override
    List<Trip> findAll();
    
    @EntityGraph(attributePaths = {"author"})
    @Override
    Optional<Trip> findById(Long id);
    
    @EntityGraph(attributePaths = {"author"})
    List<Trip> findByAuthorId(Long authorId);
    
    @Query(value = "SELECT DISTINCT t.* FROM trips t " +
           "LEFT JOIN users u ON t.author_id = u.id " +
           "WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "EXISTS (SELECT 1 FROM unnest(t.tags) AS tag WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :query, '%')))",
           nativeQuery = true)
    List<Trip> searchTrips(@Param("query") String query);
}

