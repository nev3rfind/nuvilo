package com.example.nuvilo.domain.model

enum class MedicationForm(val displayName: String, val isEstimatedConsumption: Boolean = false) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    LIQUID_ML("Liquid (ml)"),
    DROPS("Eye / Ear Drops", isEstimatedConsumption = true),
    INHALER_PUFFS("Inhaler (Puffs)", isEstimatedConsumption = true),
    TOPICAL_CREAM("Cream / Ointment", isEstimatedConsumption = true),
    INJECTION("Injection"),
    PATCH("Transdermal Patch"),
    OTHER("Other")
}

enum class StockUnit(val displayName: String, val abbreviation: String) {
    TABLETS("Tablets", "tab"),
    CAPSULES("Capsules", "cap"),
    ML("Millilitres", "ml"),
    DROPS("Drops", "drops"),
    PUFFS("Puffs", "puffs"),
    APPLICATIONS("Applications", "app"),
    UNITS("Units", "IU"),
    PATCHES("Patches", "patch"),
    BOTTLES("Bottles", "btl"),
    OTHER("Units", "units")
}

enum class RecordingMode(val displayName: String) {
    CONFIRM("Explicit Confirmation"),
    AUTO_ASSUME("Automatic Assumed Recording")
}

enum class DoseStatus(val displayName: String) {
    PENDING("Due"),
    TAKEN("Confirmed Taken"),
    ASSUMED("Assumed Taken"),
    SKIPPED("Skipped"),
    MISSED("Missed")
}

enum class MovementType(val displayName: String, val isInflow: Boolean) {
    OPENING_BALANCE("Opening Balance", true),
    RESTOCK("Restock Refill", true),
    ADJUSTMENT_IN("Adjustment (Count Increase)", true),
    DOSE_REVERSAL("Dose Reversal / Correction", true),
    DOSE_TAKEN("Confirmed Dose", false),
    DOSE_ASSUMED("Assumed Dose", false),
    ADJUSTMENT_OUT("Adjustment (Count Decrease)", false),
    WASTAGE("Spillage / Wastage", false)
}

enum class ScheduleType(val displayName: String) {
    DAILY("Daily"),
    SPECIFIC_WEEKDAYS("Specific Days of Week"),
    CYCLICAL("Cyclical (On / Off)"),
    PRN("As Needed (PRN)")
}

enum class PrescriptionStatus(val displayName: String) {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class PrescriptionEventType(val displayName: String) {
    CREATED("Prescription Added"),
    UPDATED("Prescription Updated"),
    REPEAT_REQUESTED("Repeat Requested"),
    REPEAT_COLLECTED("Repeat Collected"),
    STATUS_CHANGED("Status Changed"),
    ARCHIVED("Prescription Archived")
}
