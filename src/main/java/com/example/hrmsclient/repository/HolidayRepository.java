package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByYearOrderByHolidayDate(int year);

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :start AND :end " +
           "ORDER BY h.holidayDate")
    List<Holiday> findByDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    Optional<Holiday> findByHolidayDate(LocalDate date);

    boolean existsByHolidayDate(LocalDate date);
}