package de.ids_mannheim.korap.config;

/**
 * @author hanl
 * @date 09/03/2016
 */
public abstract class BeanConfigTest extends BeanConfigBaseTest{

    @Override
    protected ContextHolder getContext () {
        return helper().getContext();
    }

    protected TestHelper helper () {
        try {
            return TestHelper.newInstance(this.context);
        }
        catch (Exception e) {
            return null;
        }
    }

}
