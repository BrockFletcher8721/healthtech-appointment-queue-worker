public final class PatientNotificationTest {
    public static void main(String[] args) {
        HealthJob job = new HealthJob("apt-42", "p-9", "CANCELLED");
        String actual = PatientNotification.forJob(job);
        if (!actual.equals("Appointment apt-42 was cancelled; contact support.")) throw new AssertionError(actual);
        System.out.println("patient notification decision: passed");
    }
}
