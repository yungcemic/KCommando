package me.koply.kcommando;

import me.koply.kcommando.internal.CargoTruck;
import me.koply.kcommando.internal.CommandType;
import me.koply.kcommando.internal.Commando;
import org.reflections8.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;

@SuppressWarnings("rawtypes") // raw use of parameterized class 'Command'
public class KInitializer {

    private final Parameters params;
    public final Parameters getParams() { return params; }
    
    private final  Class<? extends CommandHandler>commandHandler;

    public KInitializer(Parameters params) {
        this.params = params;
        this.commandHandler = CommandHandler.class;
    }

    /**
     * this constructor for advanced usage
     *
     * @param params parameters for the runs bot
     * @param commandHandler custom commandHandler *class* for the kcommando
     */
    public KInitializer(Parameters params, Class<? extends CommandHandler> commandHandler) {
        this.params = params;
        this.commandHandler = commandHandler;
    }

    /**
     * @return command classes
     */
    public Set<Class<? extends Command>> getCommands() {
        final Reflections reflections = new Reflections(params.getPackagePath());
        return reflections.getSubTypesOf(Command.class);
    }

    private int classCounter = 0;
    /**
     * Classic build pattern. Register's CommandHandler and uses reflections from getCommands method.
     */
    public void build() {
        KCommando.logger.info("KCommando launching!");
        if (params.getIntegration() == null || params.getPackagePath() == null) {
            throw new IllegalArgumentException("We couldn't found integration or commands package path :(");
        }

        params.getDataManager().ifPresent(x -> x.initDataFile(params));

        final HashMap<String, CommandToRun> commandMethods = new HashMap<>();
        final Set<Class<? extends Command>> classes = getCommands();

        for (Class<? extends Command> clazz : classes) {
            registerCommand(clazz, commandMethods);
        }
        CargoTruck.setCargo(null);
        params.setCommandMethods(commandMethods);
        try {
            params.getIntegration().register(commandHandler.getDeclaredConstructor(Parameters.class).newInstance(params));
        } catch (Exception ex) {
            ex.printStackTrace();
            KCommando.logger.info("An unexpected error caught at initialize the command handler.");
        }

        KCommando.logger.info(classCounter + " commands are initialized.");
        KCommando.logger.info("KCommando system is ready o7");
    }

    /**
     * @return if returns true skips class
     */
    private boolean preCheck(Class<? extends Command> clazz) {
        if (clazz.getPackage().getName().contains("me.koply.kcommando.integration.impl")) return true;
        if ((clazz.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
            KCommando.logger.warning(clazz.getName() + " is not public class. Skipping...");
            return true;
        }
        return false;
    }

    /**
     * @return if returns true skips class
     */
    private boolean annotationCheck(Commando ant, String clazzName) {
        if (ant == null) {
            KCommando.logger.warning(clazzName + " is couldn't have Commando annotation. Skipping...");
            return true;
        } if (ant.guildOnly() && ant.privateOnly()) {
            KCommando.logger.warning(clazzName + " has GuildOnly and PrivateOnly at the same time. Skipping...");
            return true;
        }
        return false;
    }

    /**
     * checks handle methods for type
     *
     * a bit hardcoded object type checker
     * org.javacord.api.event.message
     * net.dv8tion.jda.api.events.message
     *
     * @return if returns null, skips the current class
     */
    private CommandType methodCheck(Class<? extends Command> clazz) {
        int methodCounter = 0;
        CommandType type = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getReturnType() != boolean.class) continue;
            final Class<?>[] parameters = method.getParameterTypes();
            if (!parameters[0].getPackage().getName().contains("message")) continue;
            if (parameters.length <= 3 && method.getName().equals("handle")) {
                if (parameters.length == 1) {
                    methodCounter++;
                    type = CommandType.EVENT;
                } else if (parameters.length == 2 && parameters[1].isArray()) { // ??
                    methodCounter++;
                    type = CommandType.ARGNEVENT;
                } else if (parameters.length == 3 && parameters[2] == String.class) {
                    methodCounter++;
                    type = CommandType.PREFIXED;
                }
            }
        }

        if (methodCounter != 1) {
            KCommando.logger.warning(clazz.getName() + " is have multiple command method. Skipping...");
            return null;
        }
        return type;
    }

    /**
     * creates info and pushes to cargo
     *
     * @param ant the command annotation
     */
    public void infoGenerator(Commando ant) {
        CommandInfo tempinfo = new CommandInfo();
        tempinfo.initialize(ant);
        CargoTruck.setCargo(tempinfo);
    }

    public void registerCommand(final Class<? extends Command> clazz, final HashMap<String, CommandToRun> commandMethods) {
        // for package and class public modifier
        if (preCheck(clazz)) return;

        final Commando commandAnnotation = clazz.getAnnotation(Commando.class);
        if (annotationCheck(commandAnnotation, clazz.getName())) return;

        final CommandType type = methodCheck(clazz);
        if (type == null) return;

        final String[] packageSplitted = clazz.getPackage().getName().split("\\.");
        final String groupName = packageSplitted[packageSplitted.length-1];

        try {
            infoGenerator(commandAnnotation);

            final Command commandInstance = clazz.getDeclaredConstructor().newInstance();
            final CommandToRun ctr = new CommandToRun(commandInstance, groupName, type);

            for (final String s : commandAnnotation.aliases()) {
                final String name = params.getCaseSensitivity().map(s::toLowerCase).orElse(s);
                commandMethods.put(name, ctr);
            }
            classCounter++;

        } catch (Throwable t) {
            KCommando.logger.warning("Something went wrong.");
        } finally {
            KCommando.logger.info(clazz.getName() + " is have command method");
        }
    }
}