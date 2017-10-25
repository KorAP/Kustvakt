package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.dto.FoundryDto;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.dto.converter.AnnotationConverter;
import de.ids_mannheim.korap.entity.AnnotationPair;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

@Service
public class AnnotationService {

    private static Logger jlog =
            LoggerFactory.getLogger(AnnotationService.class);

    @Autowired
    private AnnotationDao annotationDao;

    @Autowired
    private AnnotationConverter annotationConverter;

    public List<LayerDto> getLayerDtos () {
        List<AnnotationPair> layers = annotationDao.getAllFoundryLayerPairs();
        jlog.debug("/layers " + layers.toString());
        List<LayerDto> layerDto = annotationConverter.convertToLayerDto(layers);
        return layerDto;
    }

    public List<FoundryDto> getFoundryDtos (List<String> codes, String language)
            throws KustvaktException {
        List<AnnotationPair> annotationPairs = null;
        String foundry = "", layer = "";
        if (codes.contains("*")) {
            annotationPairs =
                    annotationDao.getAnnotationDescriptions(foundry, layer);
        }
        else {
            String[] annotationCode;
            annotationPairs = new ArrayList<AnnotationPair>();
            for (String code : codes) {
                jlog.debug("code " + code);
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
                    throw KustvaktResponseHandler.throwit(
                            new KustvaktException(StatusCodes.INVALID_ATTRIBUTE,
                                    "Bad attribute:", code));
                }

                annotationPairs.addAll(annotationDao
                        .getAnnotationDescriptions(foundry, layer));
            }
        }

        if (annotationPairs != null && !annotationPairs.isEmpty()) {
            List<FoundryDto> foundryDtos = annotationConverter
                    .convertToFoundryDto(annotationPairs, language);
            jlog.debug("/description " + annotationPairs.toString());
            return foundryDtos;
        }
        else {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found.", "");
        }

    }
}
