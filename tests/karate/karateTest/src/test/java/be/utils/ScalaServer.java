package be.utils;

public class ScalaServer extends Server implements Configurable {

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
        String configPath = getDir() + "\\src\\main\\scala\\ch\\epfl\\pop\\config";
        return new String[]{"sbt.bat", "-Dscala.config="+configPath, "run"};
    }

    @Override
    public String getDir() {
        return "C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\be2-scala";
    }

    @Override
    public String getLogPath() {
        return "C:\\Users\\Mohamed\\GolandProjects\\student_21_pop\\tests\\karate\\karateTest\\scala_create.log";
    }
}
