package de.ids_mannheim.korap.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes resources having free licenses. Primarily for
 * accommodating clients in providing data without login such as
 * KorapSRU.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@Entity
@Table(name = "resource")
public class Resource {

	// required
    @Id
    private String id;
    
    private String pid;

    // one title is required
    @Column(name = "de_title")
    private String germanTitle;

    @Column(name = "en_title")
    private String englishTitle;

    @Column(name = "en_description")
    private String englishDescription;
    
    // required
    @Column(name = "corpus_query")
    private String corpusQuery;
    
    private String institution;
    
    // required
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "resource_layer", joinColumns = @JoinColumn(name = "resource_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "layer_id", referencedColumnName = "id"))
    private Set<AnnotationLayer> layers;

    public Resource () {}

	public Resource (String id, String pid, String germanTitle,
					 String englishTitle, String englishDescription,
					 Set<AnnotationLayer> layers, String institution,
					 String corpusQuery) {
        this.id = id;
        this.pid = pid;
        this.germanTitle = germanTitle;
        this.englishTitle = englishTitle;
        this.englishDescription = englishDescription;
        this.layers = layers;
        this.corpusQuery = corpusQuery;
        this.institution=institution;
    }

	@Override
	public String toString () {
		return "id=" + id + "pid=" + pid + ", germanTitle=" + germanTitle
				+ ", englishTitle=" + englishTitle + ", description="
				+ englishDescription + ", layers= " + layers + ", institution="
				+ institution + ", corpusQuery=" + corpusQuery;
	}

}
