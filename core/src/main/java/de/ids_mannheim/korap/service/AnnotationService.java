package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.dto.FoundryDto;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.dto.converter.AnnotationConverter;
import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.web.controller.AnnotationController;

/** AnnotationService defines the logic behind {@link AnnotationController}.
 * 
 * @author margaretha
 *
 */
@Service
public class AnnotationService {

    private static final boolean DEBUG = false;

    private static Logger jlog =
            LogManager.getLogger(AnnotationService.class);

    @Autowired
    private AnnotationDao annotationDao;

    @Autowired
    private AnnotationConverter annotationConverter;

    public List<LayerDto> getLayerDtos () {
        List<AnnotationLayer> layers = annotationDao.getAllFoundryLayerPairs();
        if (DEBUG){
            jlog.debug("/layers " + layers.toString());
        }
        List<LayerDto> layerDto = annotationConverter.convertToLayerDto(layers);
        return layerDto;
    }

    public List<FoundryDto> getFoundryDtos (List<String> codes, String language)
            throws KustvaktException {
        List<AnnotationLayer> annotationPairs = null;
        String foundry = "", layer = "";
        if (codes.contains("*")) {
            annotationPairs =
                    annotationDao.getAnnotationDescriptions(foundry, layer);
        }
        else {
            String[] annotationCode;
            annotationPairs = new ArrayList<AnnotationLayer>();
            for (String code : codes) {
                if (DEBUG){
                    jlog.debug("code " + code);
                }
                annotationCode = code.split("/");
                if (annotationCode.length == 1) {
                    foundry = annotationCode[0];
                }
                else if (annotationCode.length == 2) {
                    foundry = annotationCode[0];
                    layer = annotationCode[1];
                }
                else {
                    jlog.error("Annotation code is wrong: " + annotationCode);
                    throw new KustvaktException(StatusCodes.INVALID_ATTRIBUTE,
                            "Bad attribute:", code);
                }

                annotationPairs.addAll(annotationDao
                        .getAnnotationDescriptions(foundry, layer));
            }
        }

        if (annotationPairs != null && !annotationPairs.isEmpty()) {
            List<FoundryDto> foundryDtos = annotationConverter
                    .convertToFoundryDto(annotationPairs, language);
            if (DEBUG){
                jlog.debug("/description " + annotationPairs.toString());
            }
            return foundryDtos;
        }
        else {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found.", "");
        }

    }
}
