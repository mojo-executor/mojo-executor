File buildLog = new File(basedir, "build.log")
return buildLog.readLines().contains("     [echo] Mojo Executor ran successfully.");
