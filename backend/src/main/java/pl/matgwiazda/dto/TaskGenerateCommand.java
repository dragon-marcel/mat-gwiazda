package pl.matgwiazda.dto;

/**
 * Command to request generation of a task for a user.
 * Maps to Task.level preferences; `source` indicates origin (e.g. "ai" or "template").
 * Validation: level between 1 and 8 (service layer); source optional.
 */
public class TaskGenerateCommand {
    private Integer level;
    private String source;

    public TaskGenerateCommand() {}

    public TaskGenerateCommand(Integer level, String source) {
        this.level = level;
        this.source = source;
    }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

