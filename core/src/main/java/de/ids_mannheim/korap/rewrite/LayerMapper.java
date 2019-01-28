package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.UserSettingProcessor;

/** EM:
 *  <ul>
 *  <li> Added default morphology foundry </li>
 *  <li> Made this class as a spring component</li>
 *  </ul>
 * @author hanl, margaretha
 * @date 14/10/2014
 */
@Component
public class LayerMapper {

    @Autowired
    private KustvaktConfiguration config;

    public String findFoundry (String layer) {
        return findFoundry(layer, null);
    }

    /**
     * find foundry entry in settings specific settings. Includes a
     * call to #translateLayer to get the
     * correct mapping for the layer denomination!
     * 
     * @param layer
     * @return
     */

    //todo: make mapping configurable!
    public String findFoundry (String layer, UserSettingProcessor settings) {
        if (settings != null) {
            switch (translateLayer(layer.toLowerCase().trim())) {
                case "d":
                    return (String) settings
                            .get(Attributes.DEFAULT_FOUNDRY_RELATION);
                case "c":
                    return (String) settings
                            .get(Attributes.DEFAULT_FOUNDRY_CONSTITUENT);
                case "pos":
                    return (String) settings
                            .get(Attributes.DEFAULT_FOUNDRY_POS);
                case "lemma":
                    return (String) settings
                            .get(Attributes.DEFAULT_FOUNDRY_LEMMA);
                case "surface":
                    return "opennlp";
                // EM: added
                case "morphology":
                    return (String) settings
                            .get(Attributes.DEFAULT_FOUNDRY_MORPHOLOGY);    
                default:
                    // if the layer is not in this specific listing, assume a default layer
                    // like orth or other tokenization layers
                    return null;
            }
        }
        else {
            switch (translateLayer(layer.toLowerCase().trim())) {
                case "d":
                    return config.getDefault_dep();
                case "c":
                    return config.getDefault_const();
                case "pos":
                    return config.getDefault_pos();
                case "lemma":
                    return config.getDefault_lemma();
                case "morphology":
                    return config.getDefault_morphology();
                case "surface":
                    return config.getDefault_token();
                    // refers to "structure" and is used for paragraphs or sentence boundaries
                case "s":
                    return "base";
                default:
                    // if the layer is not in this specific listing, assume a default layer
                    // like orth or other tokenization layers
                    return null;
            }
        }
    }


    // relevance: map to access control id references. p is usually mapped to pos, l to lemma, etc.
    public String translateLayer (String layer) {
        switch (layer.toLowerCase().trim()) {
        //            case "pos":
        //                return "p";
        //            case "lemma":
        //                return "l";
            case "m":
                return "morphology"; // EM: changed msd to morphology
//                return "msd";
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
