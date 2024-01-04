package de.ids_mannheim.korap.config;

/**
 * @author hanl
 * @date 26/02/2016
 */
public interface BeanInjectable {

    <T extends ContextHolder> void insertBeans (T beans);
}
