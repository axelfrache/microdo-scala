package squirrelvault.validation

import squirrelvault.api.dto.CreateBackupJobRequest

object BackupJobValidator:
  def validate(req: CreateBackupJobRequest): List[String] =
    List(
      Option.when(req.name.isBlank)("name must not be empty"),
      Option.when(req.source.isBlank)("source must not be empty"),
      Option.when(req.targetBucket.isBlank)("targetBucket must not be empty"),
      Option.when(req.targetPrefix.isBlank)("targetPrefix must not be empty"),
      validateSchedule(req.schedule),
      Option.when(req.retentionDays <= 0)("retentionDays must be greater than 0"),
      Option.when(req.expectedFrequencyHours <= 0)("expectedFrequencyHours must be greater than 0")
    ).flatten

  private val monthNames = Map(
    "JAN" -> 1,
    "FEB" -> 2,
    "MAR" -> 3,
    "APR" -> 4,
    "MAY" -> 5,
    "JUN" -> 6,
    "JUL" -> 7,
    "AUG" -> 8,
    "SEP" -> 9,
    "OCT" -> 10,
    "NOV" -> 11,
    "DEC" -> 12
  )

  private val dayNames = Map(
    "SUN" -> 0,
    "MON" -> 1,
    "TUE" -> 2,
    "WED" -> 3,
    "THU" -> 4,
    "FRI" -> 5,
    "SAT" -> 6
  )

  private def validateSchedule(schedule: String): Option[String] =
    if schedule.isBlank then Some("schedule must not be empty")
    else if !isValidCron(schedule) then Some("schedule must be a valid 5-field cron expression")
    else None

  private def isValidCron(schedule: String): Boolean =
    val fields = schedule.trim.split("\\s+")
    fields.length == 5 &&
      validateField(fields(0), 0, 59, allowNames = false) &&
      validateField(fields(1), 0, 23, allowNames = false) &&
      validateField(fields(2), 1, 31, allowNames = false) &&
      validateField(fields(3), 1, 12, allowNames = true) &&
      validateField(fields(4), 0, 7, allowNames = true, dayOfWeek = true)

  private def validateField(
      field: String,
      min: Int,
      max: Int,
      allowNames: Boolean,
      dayOfWeek: Boolean = false
  ): Boolean =
    field.nonEmpty && field.split(",", -1).forall(validatePart(_, min, max, allowNames, dayOfWeek))

  private def validatePart(
      part: String,
      min: Int,
      max: Int,
      allowNames: Boolean,
      dayOfWeek: Boolean
  ): Boolean =
    val pieces = part.split("/", -1)
    pieces.length == 1 && validateBase(pieces(0), min, max, allowNames, dayOfWeek) ||
      pieces.length == 2 &&
        isPositiveInt(pieces(1)) &&
        validateBase(pieces(0), min, max, allowNames, dayOfWeek)

  private def validateBase(
      base: String,
      min: Int,
      max: Int,
      allowNames: Boolean,
      dayOfWeek: Boolean
  ): Boolean =
    if base == "*" then true
    else if base.contains("-") then
      val range = base.split("-", -1)
      range.length == 2 &&
        validateAtom(range(0), min, max, allowNames, dayOfWeek) &&
        validateAtom(range(1), min, max, allowNames, dayOfWeek)
    else validateAtom(base, min, max, allowNames, dayOfWeek)

  private def validateAtom(
      atom: String,
      min: Int,
      max: Int,
      allowNames: Boolean,
      dayOfWeek: Boolean
  ): Boolean =
    if isInt(atom) then
      val value = atom.toInt
      value >= min && value <= max && (!dayOfWeek || value <= 7)
    else if allowNames then
      val names = if dayOfWeek then dayNames else monthNames
      names.contains(atom.toUpperCase)
    else false

  private def isInt(value: String): Boolean =
    value.nonEmpty && value.forall(_.isDigit)

  private def isPositiveInt(value: String): Boolean =
    isInt(value) && value.toInt > 0
