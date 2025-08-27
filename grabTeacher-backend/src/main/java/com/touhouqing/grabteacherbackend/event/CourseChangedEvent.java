package com.touhouqing.grabteacherbackend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CourseChangedEvent extends ApplicationEvent {
    public enum ChangeType { CREATE, UPDATE, DELETE, STATUS, FEATURED }
    private final ChangeType changeType;

    public CourseChangedEvent(Object source, ChangeType changeType) {
        super(source);
        this.changeType = changeType;
    }

}

