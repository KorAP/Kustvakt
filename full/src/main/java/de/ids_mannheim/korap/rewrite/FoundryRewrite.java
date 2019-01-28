package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.DefaultSettingService;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserSettingProcessor;

/**
 * @author margaretha
 *
 */
public class FoundryRewrite extends FoundryInject {

    @Autowired
    private DefaultSettingService settingService;

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        String username = user.getUsername();
        String jsonSettings =
                settingService.retrieveDefaultSettings(username);
        if (jsonSettings != null) {
            UserSettingProcessor processor =
                    new UserSettingProcessor(jsonSettings);
            user.setUserSettingProcessor(processor);
        }
        return super.rewriteQuery(node, config, user);
    }
}
