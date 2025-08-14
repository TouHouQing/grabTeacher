package com.touhouqing.grabteacherbackend.util;

import com.touhouqing.grabteacherbackend.entity.dto.TimeSlotDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@DisplayName("时间段工具类测试")
public class TimeSlotUtilTest {

    @Test
    @DisplayName("测试有效的时间段格式")
    public void testValidTimeSlot() {
        assertTrue(TimeSlotUtil.isValidTimeSlot("08:00-09:00"));
        assertTrue(TimeSlotUtil.isValidTimeSlot("14:30-16:00"));
        assertTrue(TimeSlotUtil.isValidTimeSlot("09:00-10:30"));
    }

    @Test
    @DisplayName("测试无效的时间段格式")
    public void testInvalidTimeSlot() {
        assertFalse(TimeSlotUtil.isValidTimeSlot(null));
        assertFalse(TimeSlotUtil.isValidTimeSlot(""));
        assertFalse(TimeSlotUtil.isValidTimeSlot("08:00"));
        assertFalse(TimeSlotUtil.isValidTimeSlot("08:00-25:00"));
        assertFalse(TimeSlotUtil.isValidTimeSlot("10:00-08:00")); // 结束时间早于开始时间
        assertFalse(TimeSlotUtil.isValidTimeSlot("invalid"));
    }

    @Test
    @DisplayName("测试有效的时间段列表")
    public void testValidTimeSlots() {
        List<TimeSlotDTO> timeSlots = Arrays.asList(
            TimeSlotDTO.builder()
                .weekday(1)
                .timeSlots(Arrays.asList("08:00-09:00", "10:00-11:00"))
                .build(),
            TimeSlotDTO.builder()
                .weekday(2)
                .timeSlots(Arrays.asList("14:00-15:00"))
                .build()
        );
        
        assertTrue(TimeSlotUtil.isValidTimeSlots(timeSlots));
    }

    @Test
    @DisplayName("测试空时间段列表")
    public void testEmptyTimeSlots() {
        assertTrue(TimeSlotUtil.isValidTimeSlots(null));
        assertTrue(TimeSlotUtil.isValidTimeSlots(new ArrayList<>()));
    }

    @Test
    @DisplayName("测试sanitizeTimeSlots方法")
    public void testSanitizeTimeSlots() {
        List<TimeSlotDTO> timeSlots = Arrays.asList(
            TimeSlotDTO.builder()
                .weekday(1)
                .timeSlots(Arrays.asList("08:00-09:00", "invalid-time", "10:00-11:00"))
                .build(),
            TimeSlotDTO.builder()
                .weekday(8) // 无效的星期几
                .timeSlots(Arrays.asList("14:00-15:00"))
                .build(),
            TimeSlotDTO.builder()
                .weekday(2)
                .timeSlots(Arrays.asList("14:00-15:00"))
                .build()
        );
        
        List<TimeSlotDTO> sanitized = TimeSlotUtil.sanitizeTimeSlots(timeSlots);
        
        assertNotNull(sanitized);
        assertEquals(2, sanitized.size()); // 应该只有2个有效的时间段
        
        // 检查第一个时间段
        assertEquals(1, sanitized.get(0).getWeekday());
        assertEquals(2, sanitized.get(0).getTimeSlots().size()); // 应该过滤掉无效的时间
        
        // 检查第二个时间段
        assertEquals(2, sanitized.get(1).getWeekday());
        assertEquals(1, sanitized.get(1).getTimeSlots().size());
    }

    @Test
    @DisplayName("测试完全无效的数据返回默认值")
    public void testSanitizeInvalidDataReturnsDefault() {
        List<TimeSlotDTO> invalidTimeSlots = Arrays.asList(
            TimeSlotDTO.builder()
                .weekday(8) // 无效的星期几
                .timeSlots(Arrays.asList("invalid-time"))
                .build()
        );
        
        List<TimeSlotDTO> sanitized = TimeSlotUtil.sanitizeTimeSlots(invalidTimeSlots);
        
        assertNotNull(sanitized);
        assertFalse(sanitized.isEmpty()); // 应该返回默认值，不为空
    }

    @Test
    @DisplayName("测试JSON转换")
    public void testJsonConversion() {
        List<TimeSlotDTO> timeSlots = Arrays.asList(
            TimeSlotDTO.builder()
                .weekday(1)
                .timeSlots(Arrays.asList("08:00-09:00", "10:00-11:00"))
                .build()
        );
        
        String json = TimeSlotUtil.toJsonString(timeSlots);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        List<TimeSlotDTO> parsed = TimeSlotUtil.fromJsonString(json);
        assertNotNull(parsed);
        assertEquals(1, parsed.size());
        assertEquals(1, parsed.get(0).getWeekday());
        assertEquals(2, parsed.get(0).getTimeSlots().size());
    }
}
