package com.senademirci.futbolyoklama.data.model

/** Bir öğrencinin tek bir antrenmandaki durumu. */
enum class AttendanceStatus(val label: String, val shortLabel: String) {
    PRESENT("Var", "V"),
    ABSENT("Yok", "Y"),
    EXCUSED("İzinli", "İ"),
    LATE("Geç geldi", "G");

    /** Devamsızlık sayılır mı? İzinli devamsızlıktan sayılmaz, geç gelen katılmış sayılır. */
    val countsAsAbsence: Boolean get() = this == ABSENT
}
