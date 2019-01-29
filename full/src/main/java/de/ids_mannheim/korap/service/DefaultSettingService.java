package de.ids_mannheim.korap.service;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.DefaultSettingDao;
import de.ids_mannheim.korap.entity.DefaultSetting;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.UserSettingProcessor;
import de.ids_mannheim.korap.validator.ApacheValidator;

/**
 * DefaultSettingService handles all business logic related to user
 * default setting.
 * 
 * @author margaretha
 *
 */
@Service
public class DefaultSettingService {

    @Autowired
    private DefaultSettingDao settingDao;
    @Autowired
    private ApacheValidator validator;

    private String verifiyUsername (String username, String contextUsername)
            throws KustvaktException {
        username = username.substring(1);
        if (!username.equals(contextUsername)) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Username verification failed. Path parameter username "
                            + "without prefix must be the same as the "
                            + "authenticated username.",
                    username);
        }
        return username;
    }

    private void validateSettingMap (Map<String, Object> map)
            throws KustvaktException {
        if (map == null || map.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Entity body is empty. No settings are given.");
        }
        for (String k : map.keySet()) {
            validator.validateEntry(k, "setting");
        }
    }

    public int handlePutRequest (String username, Map<String, Object> map,
            String contextUsername) throws KustvaktException {
        username = verifiyUsername(username, contextUsername);
        validateSettingMap(map);

        UserSettingProcessor processor = new UserSettingProcessor();
        processor.readQuietly(map, false);

        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        if (defaultSetting == null) {
            createDefaultSetting(username, processor);
            return HttpStatus.SC_CREATED;
        }
        else {
            updateDefaultSetting(defaultSetting, processor);
            return HttpStatus.SC_OK;
        }
    }

    public void createDefaultSetting (String username,
            UserSettingProcessor processor) throws KustvaktException {
        String jsonSettings = processor.serialize();
        settingDao.createDefaultSetting(username, jsonSettings);
    }

    public void updateDefaultSetting (DefaultSetting setting,
            UserSettingProcessor newProcessor) throws KustvaktException {
        UserSettingProcessor processor =
                new UserSettingProcessor(setting.getSettings());
        processor.update(newProcessor);

        String jsonSettings = processor.serialize();
        setting.setSettings(jsonSettings);
        settingDao.updateDefaultSetting(setting);
    }

    public String retrieveDefaultSettings (String username,
            String contextUsername) throws KustvaktException {

        username = verifiyUsername(username, contextUsername);
        return retrieveDefaultSettings(username);
    }

    public String retrieveDefaultSettings (String username)
            throws KustvaktException {
        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        if (defaultSetting == null) {
            return null;
        }
        return defaultSetting.getSettings();
    }

    public void deleteKey (String username, String contextUsername, String key)
            throws KustvaktException {
        username = verifiyUsername(username, contextUsername);
        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);

        String jsonSettings = defaultSetting.getSettings();
        UserSettingProcessor processor = new UserSettingProcessor(jsonSettings);
        processor.removeField(key);
        String json = processor.serialize();

        defaultSetting.setSettings(json);
        settingDao.updateDefaultSetting(defaultSetting);
    }

    public void deleteSetting (String username, String contextUsername)
            throws KustvaktException {
        username = verifiyUsername(username, contextUsername);
        settingDao.deleteDefaultSetting(username);
    }

}
