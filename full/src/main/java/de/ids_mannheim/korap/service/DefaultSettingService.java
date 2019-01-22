package de.ids_mannheim.korap.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.DefaultSettingDao;
import de.ids_mannheim.korap.entity.DefaultSetting;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.UserSettings;
import de.ids_mannheim.korap.user.Userdata;

@Service
public class DefaultSettingService {

    @Autowired
    private DefaultSettingDao settingDao;

    public void handlePutRequest (String username,
            Map<String, Object> form, String authenticatedUser)
            throws KustvaktException {
        if (!username.equals(authenticatedUser)) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Username verification failed. Path parameter username "
                            + "must be the same as the authenticated username.");
        }
        else if (form == null || form.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Entity body is empty. No settings are given.");
        }

        Userdata userdata = new UserSettings(username);
        userdata.readQuietly(form, false);

        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        if (defaultSetting == null) {
            createDefaultSetting(username, userdata);
        }
        else {
            updateDefaultSetting(defaultSetting, userdata);
        }
    }

    public void createDefaultSetting (String username, Userdata userdata)
            throws KustvaktException {
        String jsonSettings = userdata.serialize();
        settingDao.createDefaultSetting(username, jsonSettings);
    }

    public void updateDefaultSetting (DefaultSetting setting, Userdata newData)
            throws KustvaktException {
        Userdata existingData = new UserSettings(setting.getUsername());
        existingData.setData(setting.getSettings());
        existingData.update(newData);

        String newSettings = existingData.serialize();
        setting.setSettings(newSettings);
        settingDao.updateDefaultSetting(setting);
    }

    public String retrieveDefaultSettings (String username)
            throws KustvaktException {
        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        return defaultSetting.getSettings();
    }

}
