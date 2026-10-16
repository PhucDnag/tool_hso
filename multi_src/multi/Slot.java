package multi;

public interface Slot {
    void shutdown();
    void clearInput();
    String status();
}
