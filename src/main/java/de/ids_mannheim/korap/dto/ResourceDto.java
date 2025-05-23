package de.ids_mannheim.korap.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for resource / corpus description (e.g. for
 * KorapSRU).
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
public class ResourceDto {

	private String resourceId;
	private Map<String, String> titles;
	private String description;
	private String[] languages;
	private Map<Integer, String> layers;
	private String institution;
	private String landingPage;
	private String requiredAccess;

	@Override
	public String toString () {
		return "resourceId= " + resourceId + ", description= " + description
				+ ", titles= " + titles + ", languages= " + languages
				+ ", layers= " + layers + ", institution=" + institution
				+ ", landingPage=" + landingPage + ",requiredAccess="
				+ requiredAccess;
	}
}
