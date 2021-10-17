/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class for assignments with time budget.
 *
 * This version is used for grading students' submissions.
 */
public class Assignment {

    private static final long TIME_BUDGET = 20;

    public static void main(String[] args) {
        runWithTimeout(() -> run(args), TIME_BUDGET);
    }

    private static void run(String[] args) {
        if (args.length > 0) {
            List<String> argList = new ArrayList<>();
            Collections.addAll(argList, "-pp", "-p", "plan.yml");
            Collections.addAll(argList, args);
            Main.main(argList.toArray(new String[0]));
        } else {
            System.out.println("Usage: -cp <CLASS_PATH> -m <CLASS_NAME>");
        }
    }

    private static void runWithTimeout(Runnable task, long seconds) {
        Duration timeout = Duration.ofSeconds(seconds);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        @SuppressWarnings("unchecked")
        Future<Void> handler = (Future<Void>) executor.submit(task);
        try {
            handler.get(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException | ExecutionException e) {
            e.getCause().printStackTrace();
            System.exit(1);
        } finally {
            executor.shutdown();
        }
    }
}
