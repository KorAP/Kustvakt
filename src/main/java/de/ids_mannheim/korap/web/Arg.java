package de.ids_mannheim.korap.web;

import de.ids_mannheim.korap.config.KustvaktClassLoader;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author hanl
 * @date 11/02/2016
 */
public abstract class Arg<T> {

    @Getter
    protected T value;


    protected Arg () {}


    public abstract void setValue (String value);


    public String toString () {
        return "--" + getName();
    }


    public abstract String getName ();


    public abstract void run ();


    public static Set<Arg> loadArgs (String[] args) {
        Set<Arg> argSet = new HashSet<>();
        Set<Class<? extends Arg>> set = KustvaktClassLoader
                .loadSubTypes(Arg.class);

        for (int idx = 0; idx < args.length; idx++) {
            for (Class aClass : new HashSet<>(set)) {
                if (!argSet.contains(aClass)) {
                    Arg arg;
                    try {
                        arg = (Arg) aClass.newInstance();
                    }
                    catch (InstantiationException | IllegalAccessException e) {
                        continue;
                    }
                    if (arg.toString().equals(args[idx])) {
                        int i = args.length - 1;
                        if (i > idx + 1)
                            i = idx + 1;
                        arg.setValue(args[i]);
                        arg.run();
                        argSet.add(arg);
                        set.remove(aClass);
                    }
                }
            }
        }
        return argSet;
    }

    public static class ConfigArg extends Arg<String> {

        @Override
        public void setValue (String value) {
            this.value = value;
        }


        @Override
        public String getName () {
            return "config";
        }


        @Override
        public void run () {

        }
    }

    public static class InitArg extends Arg<Boolean> {

        @Override
        public void setValue (String value) {
            this.value = true;
        }


        @Override
        public String getName () {
            return "init";
        }


        @Override
        public void run () {

        }
    }

    public static class PortArg extends Arg<Integer> {

        @Override
        public void setValue (String value) {
            this.value = Integer.valueOf(value);
        }


        @Override
        public String getName () {
            return "port";
        }


        @Override
        public void run () {}
    }

}
