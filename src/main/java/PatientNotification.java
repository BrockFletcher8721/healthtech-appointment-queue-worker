public final class PatientNotification {
    public static String forJob(HealthJob job) {
        return switch (job.status()) {
            case "CONFIRMED" -> "Appointment " + job.appointmentId() + " is confirmed.";
            case "CANCELLED" -> "Appointment " + job.appointmentId() + " was cancelled; contact support.";
            default -> "Appointment " + job.appointmentId() + " needs staff review.";
        };
    }
}
