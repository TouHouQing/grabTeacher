package com.touhouqing.grabteacherbackend.service.events;

import java.time.LocalDate;
import java.util.List;

public class MatchedTeachersWarmupEvent {
    private final List<Long> teacherIds;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<String> preferredTimeSlots;

    public MatchedTeachersWarmupEvent(List<Long> teacherIds, LocalDate startDate, LocalDate endDate, List<String> preferredTimeSlots) {
        this.teacherIds = teacherIds;
        this.startDate = startDate;
        this.endDate = endDate;
        this.preferredTimeSlots = preferredTimeSlots;
    }

    public List<Long> getTeacherIds() {
        return teacherIds;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<String> getPreferredTimeSlots() {
        return preferredTimeSlots;
    }
}

