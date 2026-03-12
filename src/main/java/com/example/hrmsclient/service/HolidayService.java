package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.HolidayDTO;
import com.example.hrmsclient.entity.Holiday;
import com.example.hrmsclient.repository.HolidayRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayRepository holidayRepo;

    public HolidayService(HolidayRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    @Transactional
    public HolidayDTO addHoliday(HolidayDTO dto) {
        if (holidayRepo.existsByHolidayDate(dto.getHolidayDate()))
            throw new IllegalArgumentException(
                "A holiday already exists on " + dto.getHolidayDate());

        Holiday h = new Holiday();
        h.setName(dto.getName());
        h.setHolidayDate(dto.getHolidayDate());
        h.setDescription(dto.getDescription());
        h.setHolidayType(dto.getHolidayType());
        h.setCreatedBy(currentUser());

        return toDto(holidayRepo.save(h));
    }

    @Transactional
    public HolidayDTO updateHoliday(Long id, HolidayDTO dto) {
        Holiday h = holidayRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Holiday not found"));

        // Allow date change only if new date isn't already taken by another holiday
        if (!h.getHolidayDate().equals(dto.getHolidayDate())
                && holidayRepo.existsByHolidayDate(dto.getHolidayDate()))
            throw new IllegalArgumentException(
                "Another holiday already exists on " + dto.getHolidayDate());

        h.setName(dto.getName());
        h.setHolidayDate(dto.getHolidayDate());
        h.setDescription(dto.getDescription());
        h.setHolidayType(dto.getHolidayType());

        return toDto(holidayRepo.save(h));
    }

    @Transactional
    public void deleteHoliday(Long id) {
        if (!holidayRepo.existsById(id))
            throw new RuntimeException("Holiday not found");
        holidayRepo.deleteById(id);
    }

    public List<HolidayDTO> getHolidaysByYear(int year) {
        return holidayRepo.findByYearOrderByHolidayDate(year)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<HolidayDTO> getHolidaysByDateRange(LocalDate start, LocalDate end) {
        return holidayRepo.findByDateRange(start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Used internally by CalendarService
    public boolean isHoliday(LocalDate date) {
        return holidayRepo.existsByHolidayDate(date);
    }

    public java.util.Map<LocalDate, Holiday> getHolidayMapForRange(
            LocalDate start, LocalDate end) {
        return holidayRepo.findByDateRange(start, end)
                .stream()
                .collect(Collectors.toMap(Holiday::getHolidayDate, h -> h));
    }

    private HolidayDTO toDto(Holiday h) {
        return new HolidayDTO(h.getId(), h.getName(),
                h.getHolidayDate(), h.getDescription(), h.getHolidayType());
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) { return "admin"; }
    }
}