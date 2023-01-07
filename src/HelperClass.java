public class HelperClass {
    public static String formatTime(long secs) {
        return String.format("%2d:%02d", (secs % 3600) / 60, secs % 60);
    }
}
