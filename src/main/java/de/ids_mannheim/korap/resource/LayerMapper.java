package de.ids_mannheim.korap.resource;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.UserSettings;

/**
 * @author hanl
 * @date 14/10/2014
 */
public class LayerMapper {

    private UserSettings settings;
    private KustvaktConfiguration config;

    public LayerMapper(KustvaktConfiguration config, UserSettings settings) {
        this.settings = settings;
        this.config = config;
    }

    public LayerMapper(KustvaktConfiguration config) {
        this.config = config;
    }

    /**
     * find foundry entry in settings specific settings. Includes a call to #translateLayer to get the
     * correct mapping for the layer denomination!
     *
     * @param layer
     * @return
     */

    //todo: make mapping configurable!
    public String findFoundry(String layer) {
        if (settings != null) {
            switch (translateLayer(layer.toLowerCase().trim())) {
                case "d":
                    return settings.getDefaultRelfoundry();
                case "c":
                    return settings.getDefaultConstfoundry();
                case "pos":
                    return settings.getDefaultPOSfoundry();
                case "lemma":
                    return settings.getDefaultLemmafoundry();
                case "surface":
                    return "opennlp";
                default:
                    // if the layer is not in this specific listing, assume a default layer
                    // like orth or other tokenization layers
                    return "opennlp";
            }
        }else {
            switch (translateLayer(layer.toLowerCase().trim())) {
                case "d":
                    return config.getDefault_dep();
                case "c":
                    return config.getDefault_const();
                case "pos":
                    return config.getDefault_pos();
                case "lemma":
                    return config.getDefault_lemma();
                case "surface":
                    return config.getDefault_token();
                // refers to "structure" and is used for paragraphs or sentence boundaries
                case "s":
                    return "base";
                default:
                    // if the layer is not in this specific listing, assume a default layer
                    // like orth or other tokenization layers
                    return "opennlp";
            }
        }
    }

    // relevance: map to access control id references. p is usually mapped to pos, l to lemma, etc.
    public String translateLayer(String layer) {
        switch (layer.toLowerCase().trim()) {
            //            case "pos":
            //                return "p";
            //            case "lemma":
            //                return "l";
            case "m":
                return "msd";
            //todo the orth layer does not need a foundry entry
            case "orth":
                return "surface";
            case "t":
                return "surface";
            case "const":
                return "c";
            case "p":
                return "pos";
            case "l":
                return "lemma";
            default:
                return layer;
        }
    }

}
