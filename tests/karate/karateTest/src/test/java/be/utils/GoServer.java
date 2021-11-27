package be.utils;

public class GoServer extends Server implements Configurable {

    @Override
    public boolean start() {
        return super.start(getCmd(), getDir(), getLogPath());
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public String[] getCmd() {
        return new String[]{"bash", "-c", "./pop organizer --pk J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM= serve"};
    }

    @Override
    public String getDir() {
        return "C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\be1-go";
    }

    @Override
    public String getLogPath() {
        return "C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\tests\\karate\\karateTest\\go_create.log";
    }
}
