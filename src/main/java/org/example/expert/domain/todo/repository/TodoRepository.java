package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 1. 할일 리스트 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    // 2. 날씨만으로 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u " +
            "WHERE t.weather = :weather ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByWeather(@Param("weather") String weather, Pageable pageable);

    // 3. 수정일 기간으로만 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u " +
            "WHERE t.modifiedAt BETWEEN :startDate AND :endDate ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByModifiedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // 4. 날씨와 기간 모두 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u " +
            "WHERE t.weather = :weather " +
            "AND t.modifiedAt BETWEEN :startDate AND :endDate ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByWeatherAndModifiedAtBetween(
            @Param("weather") String weather,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT t FROM Todo t " +
            "LEFT JOIN FETCH t.user " + // 단건 조회라도 해도 유저 정보를 함게 가지고 오는게 성능상 이득일 듯
            "WHERE t.id = :todoId")
    Optional<Todo> findByIdWithUser(@Param("todoId") Long todoId);
}
