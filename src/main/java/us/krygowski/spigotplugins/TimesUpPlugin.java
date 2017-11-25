/*
 * Copyright (c) 2017- , Jim Krygowski, USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package us.krygowski.spigotplugins;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * TimesUpPlugin is a console plugin intended to help parents to enforce a limit on the amount of time
 * their kids spend in a Minecraft session. This plugin admittedly is a very focused solution to a specific
 * problem I needed to address as a dad. It takes me out of the negotiation loop and puts the server in charge
 * of "deciding" when its time to stop playing Minecraft. This approach is working for me but YMMV.
 * <br/>
 * The commands to setting and cancelling the timer must be issued from the server console. In game, players can
 * pause, resume and check for remaining time.
 * <br/>
 * The TimesUpPlugin uses the {@link BukkitScheduler} to schedule a {@link BukkitTask} to shut the server down.
 * <br/>
 * The management of the state held by the plugin makes no use of locking or synchronizing because in the Bukkit model
 * commands are run sequentially by the main thread so there is no chance of multiple threads updating the
 * member variables to inconsistent states.
 * <br/>
 * Note that both {@link BukkitScheduler#runTaskLater(Plugin, Runnable, long)} and {@link BukkitScheduler#runTaskTimer(Plugin, Runnable, long, long)}
 * run on the main Bukkit Thread so again, there are no concurrency issues to contend with for either.
 */
public class TimesUpPlugin extends JavaPlugin {

    private static final String CONSOLE = "CONSOLE";
    private static final String SET_COMMAND = "set";
    private static final String PAUSE_COMMAND = "pause";
    private static final String RESUME_COMMAND = "resume";
    private static final String CANCEL_COMMAND = "cancel";
    private static final String TIMELEFT_COMMAND = "timeleft";

    /**
     * Period used by the {@link BukkitScheduler#runTaskTimer(Plugin, Runnable, long, long)} to schedule a
     * periodic update on the time remaining for play.
     */
    private static final long TIME_CHECK_PERIOD = 5 * 60 * 20;

    /**
     * timerStart is set to the current time in milliseconds by the {@link TimesUpPlugin#startTimer(CommandSender, Long)}
     * method. It is used by the {@link TimesUpPlugin#pauseTimer(CommandSender)} method to calculate how much of the
     * timeLimit has been consumed.
     * <p>
     * timerStart is only > 0 when the timer is running. Because of this, timerStart is used as a flag to signal
     * that there is an active timer.
     */
    private long timerStart = 0L;

    /**
     * timeUsed is calculated as current time in (milliseconds - timerStart) / 1000 / 60 when the {@link TimesUpPlugin#pauseTimer(CommandSender)}
     * is invoked. It is used by the {@link TimesUpPlugin#resumeTimer(CommandSender)} method to start a new timer that
     * reflects how much of the timer was consumed prior to the invocation of the  {@link TimesUpPlugin#pauseTimer(CommandSender)} method.
     * <p>
     * timeUsed is only > 0 when the timer is paused.
     */

    private long timeUsed = 0L;

    /**
     * timeLimit is set from the arguments passed in to {@link TimesUpPlugin#onCommand(CommandSender, Command, String, String[])} with
     * an invocation of the {@link TimesUpPlugin#SET_COMMAND} command AND by the {@link TimesUpPlugin#resumeTimer(CommandSender)}. The
     * timelimit is expressed in minutes and trasformed by the {@link TimesUpPlugin#startTimer(CommandSender, Long)} to Ticks for use
     * in setting the delay on the invocation that schedules the {@link BukkitTask}.
     * <p>
     * timeLimit is only > 0 when a timer is operating
     */
    private long timeLimit = 0L;

    /**
     * Holder for the Task ID returned by the {@link BukkitScheduler} when a shutdown {@link BukkitTask} is scheduled. This
     * value is used by the {@link TimesUpPlugin#cancelTimer(CommandSender)} method to abort the scheduled shutdown task.
     */
    private int currentTaskId = 0;

    /**
     * Holder for the task ID returned by tye {@link BukkitScheduler} when a time check {@link BukkitTask} is scheduled.
     * This value is used by the the {@link TimesUpPlugin#cancelTimer(CommandSender)} method to abort the time check
     * task;
     */
    private int notifyTaskId = 0;


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("timesup")) {

            if (SET_COMMAND.equalsIgnoreCase(args[0])) {

                if (!CONSOLE.equals(sender.getName())) {
                    sender.sendMessage("timesup only runnable from console!");
                    return false;
                }

                if (args.length != 2) {
                    sender.sendMessage("Wrong number of arguments!");
                    return false;
                }

                if (!args[1].matches("^[0-9]+$")) {
                    sender.sendMessage("Argument must be numeric!!");
                    return false;
                }

                return startTimer(sender, Long.parseLong(args[1]));

            } else if (PAUSE_COMMAND.equalsIgnoreCase(args[0])) {
                return pauseTimer(sender);

            } else if (RESUME_COMMAND.equalsIgnoreCase(args[0])) {
                return resumeTimer(sender);

            } else if (TIMELEFT_COMMAND.equalsIgnoreCase(args[0])) {
                return timeLeft(sender);

            } else if (CANCEL_COMMAND.equalsIgnoreCase(args[0])) {
                if (!CONSOLE.equals(sender.getName())) {
                    sender.sendMessage("timesup only runnable from console!");
                    return false;
                }
                return cancelTimer(sender);

            } else {
                sender.sendMessage("Unrecognized argument.");
            }
        }

        return false;
    }

    /**
     * Initiates the two {@link BukkitTask}s responsible for a) shutting down the server and
     * b) keeping players notified of how much time is left.
     *
     * @param sender  Source of the command
     * @param minutes long value indicating how many minutes must pass before the server shuts down.
     * @return true if the command was successfully processed; false otherwise
     */
    private boolean startTimer(CommandSender sender, Long minutes) {

        timeLimit = minutes;
        Long delay = minutes * 20L * 60;

        BukkitScheduler scheduler = getServer().getScheduler();

        sender.sendMessage("Setting shutdown to " + delay + " ticks.");

        BukkitTask shutdownTask = scheduler.runTaskLater(this, new Runnable() {
            public void run() {
                getServer().broadcastMessage("SHUTTING DOWN NOW!!!");
                getServer().shutdown();

            }
        }, delay);

        timerStart = System.currentTimeMillis();
        currentTaskId = shutdownTask.getTaskId();

        BukkitTask notifyTask = scheduler.runTaskTimer(this, new Runnable() {
            public void run() {
                long minutesLeft = timeLimit - ((System.currentTimeMillis() - timerStart) / 1000 / 60);
                getServer().broadcastMessage("There are " + minutesLeft + " minutes left!!!");
                getLogger().info("There are " + minutesLeft + "minutes left!!!");
            }
        }, 1L, TIME_CHECK_PERIOD);

        notifyTaskId = notifyTask.getTaskId();

        getServer().broadcastMessage("Timer started " + minutes + " minutes left!!!");

        return true;
    }


    /**
     * Cancels running timers and records time used against the timeUsed variable.
     *
     * @param sender Source of the command
     * @return true if the command was successfully processed; false otherwise
     */
    private boolean pauseTimer(CommandSender sender) {

        if (timerStart == 0) {
            getLogger().info("No timer set. Nothing to do!");
            return false;
        }
        timeUsed = ((System.currentTimeMillis() - timerStart) / 1000 / 60);
        timerStart = 0;
        getServer().broadcastMessage("Timer paused with  " + (timeLimit - timeUsed) + " remaining!!!");
        return cancelTimer(sender);
    }

    /**
     * Calculates remaining time as timeLimit - timeUsed and uses the result to
     * start new timers. If there is no value for timeUsed we'll assume that we're not
     * in a paused state and report back that there is no timer to resume.
     *
     * @param sender  Source of the command
     * @return true if the command was successfully processed; false otherwise
     */
    private boolean resumeTimer(CommandSender sender) {


        if (timeUsed == 0) {
            getLogger().info("No timer to resume.");
            return false;
        }

        long minutes = timeLimit - timeUsed;
        timeUsed = 0;

        return startTimer(sender, minutes);

    }

    /**
     * Reports the time left to play. If the sender is a {@link Player} the
     * message is only shown to the player; otherwise the message is broadcasts
     * the message to the entire server.
     *
     * @return true if the command was successfully processed; false otherwise
     */
    private boolean timeLeft(CommandSender sender) {
        if (timerStart == 0) {
            getLogger().info("No timer set. Nothing to do!");
            return false;
        }

        long minutesLeft = timeLimit - ((System.currentTimeMillis() - timerStart) / 1000 / 60);
        String msg = "There are " + minutesLeft + " minutes left!!!";

        if (sender instanceof Player) {
            ((Player)sender).chat(msg);
        } else {
            getServer().broadcastMessage(msg);
        }

        return true;
    }

    /**
     * Cancels running timer.
     *
     * @param sender Source of the command
     * @return true if the command was successfully processed; false otherwise
     */
    private boolean cancelTimer(CommandSender sender) {

        BukkitScheduler scheduler = sender.getServer().getScheduler();

        if (currentTaskId == 0 || !scheduler.isQueued(currentTaskId)) {
            getLogger().info("No cancellable task found.");
            return false;
        }

        scheduler.cancelTask(notifyTaskId);
        scheduler.cancelTask(currentTaskId);
        getLogger().info("Cancelled timesup timer.");
        currentTaskId = 0;
        notifyTaskId = 0;
        timerStart = 0;
        return true;
    }

}