package com.touhouqing.grabteacherbackend.common.result;

import lombok.Data;

@Data
public class RescheduleTimeCheckResult {
    private boolean hasConflict;
    private String conflictType; // "teacher_unavailable" 或 "time_conflict"
    private String message;

    public RescheduleTimeCheckResult(boolean hasConflict, String conflictType, String message) {
        this.hasConflict = hasConflict;
        this.conflictType = conflictType;
        this.message = message;
    }
}
