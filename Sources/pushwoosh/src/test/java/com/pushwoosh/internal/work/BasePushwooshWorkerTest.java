package com.pushwoosh.internal.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.work.WorkInfo;

import org.junit.Test;

public class BasePushwooshWorkerTest {

    @Test
    public void mapsKnownStopReasons() {
        assertEquals("NOT_STOPPED", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_NOT_STOPPED));
        assertEquals("UNKNOWN", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_UNKNOWN));
        assertEquals("CANCELLED_BY_APP", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CANCELLED_BY_APP));
        assertEquals("PREEMPT", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_PREEMPT));
        assertEquals("TIMEOUT", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_TIMEOUT));
        assertEquals("DEVICE_STATE", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_DEVICE_STATE));
        assertEquals(
                "CONSTRAINT_BATTERY_NOT_LOW",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW));
        assertEquals(
                "CONSTRAINT_CHARGING",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CONSTRAINT_CHARGING));
        assertEquals(
                "CONSTRAINT_CONNECTIVITY",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY));
        assertEquals(
                "CONSTRAINT_DEVICE_IDLE",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE));
        assertEquals(
                "CONSTRAINT_STORAGE_NOT_LOW",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW));
        assertEquals("QUOTA", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_QUOTA));
        assertEquals(
                "BACKGROUND_RESTRICTION",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION));
        assertEquals("APP_STANDBY", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_APP_STANDBY));
        assertEquals("USER", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_USER));
        assertEquals(
                "SYSTEM_PROCESSING", BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_SYSTEM_PROCESSING));
        assertEquals(
                "ESTIMATED_APP_LAUNCH_TIME_CHANGED",
                BasePushwooshWorker.stopReasonToString(WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED));
    }

    @Test
    public void mapsUnknownReasonToCode() {
        String result = BasePushwooshWorker.stopReasonToString(9999);
        assertTrue("Unknown reason should contain code: " + result, result.startsWith("CODE_"));
        assertEquals("CODE_9999", result);
    }
}
