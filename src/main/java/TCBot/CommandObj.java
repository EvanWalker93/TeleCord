package main.java.TCBot;

public class CommandObj {
    private String command;
    private String parameter;

    public CommandObj(String command, String parameter) {
        this.command = command;
        this.parameter = parameter;
    }

    CommandObj(String message) {
        message = message.replaceAll("\\s+", " ");

        if (!message.isEmpty() && message.substring(0, 1).equals("/")) {
            String[] split = message.split(" ");
            this.command = split[0];
            if (split.length > 1) {
                this.parameter = split[1];
            }
        }
    }

    boolean hasParameter() {
        return this.parameter != null;
    }

    String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
