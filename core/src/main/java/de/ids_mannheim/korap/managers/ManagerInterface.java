package de.ids_mannheim.korap.managers;

import java.util.Set;

/**
 * @author hanl
 * @date 17/02/2016
 */
public interface ManagerInterface {

    String name ();


    Set<Function> getFunctions ();


    Result process ();

    class Result {

    }

    class Function {

        private String name;
        private String[] args;


        @Override
        public boolean equals (Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Function function = (Function) o;

            return name != null ? name.equals(function.name)
                    : function.name == null;

        }


        @Override
        public int hashCode () {
            return name != null ? name.hashCode() : 0;
        }


        public void setName (String name) {
            this.name = name;
        }


        public void setArgs (String ... args) {
            this.args = args;
        }

    }

}
