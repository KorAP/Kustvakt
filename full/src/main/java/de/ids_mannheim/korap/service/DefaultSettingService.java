package de.ids_mannheim.korap.service;

import java.util.Map;

import org.apache.http.HttpStatus;
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

    private void verifiyUsername (String username, String authenticatedUser)
            throws KustvaktException {
        if (!username.equals(authenticatedUser)) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Username verification failed. Path parameter username "
                            + "must be the same as the authenticated username.",
                    username);
        }
    }

    public int handlePutRequest (String username, Map<String, Object> map,
            String authenticatedUser) throws KustvaktException {
        verifiyUsername(username, authenticatedUser);

        if (map == null || map.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Entity body is empty. No settings are given.");
        }

        Userdata userdata = new UserSettings(username);
        userdata.readQuietly(map, false);

        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        if (defaultSetting == null) {
            createDefaultSetting(username, userdata);
            return HttpStatus.SC_CREATED;
        }
        else {
            updateDefaultSetting(defaultSetting, userdata);
            return HttpStatus.SC_OK;
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

    public String retrieveDefaultSettings (String username,
            String authenticatedUser) throws KustvaktException {

        verifiyUsername(username, authenticatedUser);

        DefaultSetting defaultSetting =
                settingDao.retrieveDefautlSetting(username);
        return defaultSetting.getSettings();
    }

}
